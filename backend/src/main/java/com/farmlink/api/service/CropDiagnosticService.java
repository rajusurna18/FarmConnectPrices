package com.farmlink.api.service;

import com.farmlink.api.config.firebase.FirebaseProperties;
import com.farmlink.api.dto.*;
import com.farmlink.api.exception.DuplicateRecordException;
import com.farmlink.api.exception.FirestoreQuotaExhaustedException;
import com.farmlink.api.exception.InvalidImageSignatureException;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.cloud.ReadChannel;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CropDiagnosticService {

    private static final Logger logger = LoggerFactory.getLogger(CropDiagnosticService.class);

    public static final String COLLECTION_NAME = "cropDiagnostics";
    public static final String STATUS_PENDING = "PENDING_ANALYSIS";
    public static final int HEADER_BYTES_CAP = 32;
    public static final int REQUIRED_HEADER_BYTES = 16;
    public static final int MINIMUM_SECRET_BYTE_LENGTH = 32;

    private static final Pattern PATH_PATTERN = Pattern.compile("^users/([a-zA-Z0-9_-]{1,128})/diagnostics/([a-zA-Z0-9_-]{1,64})/([a-zA-Z0-9._-]{1,64})$");

    private static final Set<String> ALLOWED_ORIGINAL_FILES = Set.of("original.jpg", "original.jpeg", "original.png", "original.webp");
    private static final Set<String> ALLOWED_DIAGNOSTIC_FILES = Set.of("diagnostic.webp", "diagnostic.jpg", "diagnostic.jpeg");
    private static final Set<String> ALLOWED_DISPLAY_FILES = Set.of("display.webp", "display.jpg", "display.jpeg");

    private static final long MAX_ORIGINAL_SIZE_BYTES = 10 * 1024 * 1024; // 10 MiB
    private static final long MAX_DIAGNOSTIC_SIZE_BYTES = 10 * 1024 * 1024; // 10 MiB
    private static final long MAX_DISPLAY_SIZE_BYTES = 2 * 1024 * 1024; // 2 MiB

    private final Firestore firestore;
    private final Storage storage;
    private final FirebaseProperties firebaseProperties;
    private final CropMasterService cropMasterService;
    private final FarmService farmService;
    private final FirestoreQuotaGuard quotaGuard;
    private final String storageBucket;
    private final SecretKey cursorSigningKey;

    @Autowired
    public CropDiagnosticService(
            Firestore firestore,
            Storage storage,
            FirebaseProperties firebaseProperties,
            CropMasterService cropMasterService,
            FarmService farmService,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard,
            @Value("${app.diagnostics.cursor-signing-secret:${DIAGNOSTIC_CURSOR_SECRET:}}") String cursorSigningSecret,
            @Autowired(required = false) Environment environment
    ) {
        this.firestore = firestore;
        this.storage = storage;
        this.firebaseProperties = firebaseProperties;
        this.cropMasterService = cropMasterService;
        this.farmService = farmService;
        this.quotaGuard = quotaGuard;
        this.storageBucket = initializeStorageBucket(firebaseProperties);
        this.cursorSigningKey = initializeCursorSigningKey(cursorSigningSecret, environment);
    }

    private String initializeStorageBucket(FirebaseProperties properties) {
        String bucket = properties != null ? properties.getStorageBucket() : null;
        if (bucket == null || bucket.trim().isEmpty()) {
            String errorMsg = "CRITICAL CONFIGURATION ERROR: app.firebase.storage-bucket (or FIREBASE_STORAGE_BUCKET) " +
                    "is required. An explicit Cloud Storage bucket must be configured in every environment.";
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        return bucket.trim();
    }

    private SecretKey initializeCursorSigningKey(String secret, Environment env) {
        boolean isProd = isProductionEnvironment(env);
        if (secret != null && !secret.trim().isEmpty()) {
            byte[] keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < MINIMUM_SECRET_BYTE_LENGTH) {
                String msg = String.format(
                        "CRITICAL CONFIGURATION ERROR: DIAGNOSTIC_CURSOR_SECRET must be at least %d bytes (256 bits) for HMAC-SHA256, but found %d bytes.",
                        MINIMUM_SECRET_BYTE_LENGTH, keyBytes.length
                );
                logger.error(msg);
                throw new IllegalStateException(msg);
            }
            return new SecretKeySpec(keyBytes, "HmacSHA256");
        }

        if (isProd) {
            String msg = "CRITICAL CONFIGURATION ERROR: DIAGNOSTIC_CURSOR_SECRET is missing or blank in production environment.";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }

        logger.warn("DIAGNOSTIC_CURSOR_SECRET is not configured. Running in non-production mode; generating an ephemeral 256-bit key for this runtime instance.");
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            keyGen.init(256, new SecureRandom());
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 algorithm not available in JVM", e);
        }
    }

    private boolean isProductionEnvironment(Environment env) {
        if (env != null) {
            for (String profile : env.getActiveProfiles()) {
                if ("production".equalsIgnoreCase(profile) || "prod".equalsIgnoreCase(profile)) {
                    return true;
                }
            }
        }
        String envVar = System.getenv("SPRING_PROFILES_ACTIVE");
        return envVar != null && (envVar.toLowerCase().contains("prod") || envVar.equalsIgnoreCase("production"));
    }

    public String resolveStorageBucket() {
        return this.storageBucket;
    }

    // =========================================================================
    // CREATE DIAGNOSTIC RECORD
    // =========================================================================

    public DiagnosticRecordResponse createDiagnostic(String authenticatedUid, CreateDiagnosticRequest request) {
        if (authenticatedUid == null || authenticatedUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Diagnostic request cannot be null.");
        }

        farmService.verifyFarmerRole(authenticatedUid);

        String diagnosticId = request.getDiagnosticId().trim();

        // 1. Verify cropId against master catalog
        CropResponse crop = cropMasterService.getCropById(request.getCropId());
        if (crop == null || !"ACTIVE".equalsIgnoreCase(crop.getStatus())) {
            throw new IllegalArgumentException("Invalid crop ID: Crop does not exist in master catalog or is inactive (" + request.getCropId() + ").");
        }

        // 2. Verify farmId ownership if supplied
        String farmId = (request.getFarmId() != null && !request.getFarmId().trim().isEmpty())
                ? request.getFarmId().trim()
                : null;
        String farmCropId = null;

        if (farmId != null) {
            farmService.getFarmById(farmId, authenticatedUid); // Throws NoSuchElementException (404) or AccessDeniedException (403)

            // Optional linkage to farmCrops if exists
            if (firestore != null) {
                if (quotaGuard != null) {
                    quotaGuard.checkQuotaAvailability();
                }

                try {
                    QuerySnapshot fcSnap = firestore.collection("farmCrops")
                            .whereEqualTo("farmId", farmId)
                            .whereEqualTo("cropId", crop.getId())
                            .whereEqualTo("ownerUid", authenticatedUid)
                            .get().get();

                    if (quotaGuard != null) {
                        quotaGuard.recordSuccess();
                    }

                    if (fcSnap != null && !fcSnap.isEmpty()) {
                        farmCropId = fcSnap.getDocuments().get(0).getId();
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    while (cause instanceof ExecutionException && cause.getCause() != null) {
                        cause = cause.getCause();
                    }

                    if (cause instanceof ResourceExhaustedException ||
                            (cause instanceof StatusRuntimeException sre && sre.getStatus().getCode() == Status.Code.RESOURCE_EXHAUSTED)) {
                        if (quotaGuard != null) {
                            quotaGuard.recordQuotaExhaustion(cause);
                        }
                        throw new FirestoreQuotaExhaustedException("Firestore quota exceeded during farmCrops linkage lookup");
                    }

                    if (cause instanceof StatusRuntimeException sre && sre.getStatus().getCode() == Status.Code.PERMISSION_DENIED) {
                        throw new AccessDeniedException("Access denied querying farmCrops: " + sre.getMessage(), sre);
                    }

                    throw new RuntimeException("Firestore infrastructure failure during farmCrops linkage query: " + (cause != null ? cause.getMessage() : e.getMessage()), cause);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("farmCrops query thread interrupted: " + ie.getMessage(), ie);
                }
            }
        }

        // 3. Authoritative Storage validation
        DiagnosticImagesDto images = request.getImages();
        if (images == null || images.getOriginal() == null || images.getDiagnostic() == null || images.getDisplay() == null) {
            throw new IllegalArgumentException("Diagnostic request must include metadata for original, diagnostic, and display images.");
        }

        String bucketName = resolveStorageBucket();

        Blob blobOriginal = validateStorageObject(
                bucketName,
                images.getOriginal(),
                "original",
                authenticatedUid,
                diagnosticId,
                ALLOWED_ORIGINAL_FILES,
                MAX_ORIGINAL_SIZE_BYTES,
                Set.of("image/jpeg", "image/png", "image/webp")
        );

        Blob blobDiagnostic = validateStorageObject(
                bucketName,
                images.getDiagnostic(),
                "diagnostic",
                authenticatedUid,
                diagnosticId,
                ALLOWED_DIAGNOSTIC_FILES,
                MAX_DIAGNOSTIC_SIZE_BYTES,
                Set.of("image/webp", "image/jpeg")
        );

        Blob blobDisplay = validateStorageObject(
                bucketName,
                images.getDisplay(),
                "display",
                authenticatedUid,
                diagnosticId,
                ALLOWED_DISPLAY_FILES,
                MAX_DISPLAY_SIZE_BYTES,
                Set.of("image/webp", "image/jpeg")
        );

        // 4. Atomic Firestore persistence using docRef.create()
        if (quotaGuard != null) {
            quotaGuard.checkQuotaAvailability();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", diagnosticId);
        data.put("farmerUid", authenticatedUid);
        data.put("farmId", farmId);
        data.put("farmCropId", farmCropId);
        data.put("cropId", crop.getId());
        data.put("cropName", crop.getName());
        data.put("symptoms", request.getSymptoms().trim());
        data.put("affectedPart", request.getAffectedPart());
        data.put("images", mapImagesToFirestoreMap(images, blobOriginal, blobDiagnostic, blobDisplay));
        data.put("status", STATUS_PENDING);
        data.put("aiDiagnosis", null);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(diagnosticId);
        WriteResult writeResult;

        try {
            writeResult = docRef.create(data).get();
            if (quotaGuard != null) {
                quotaGuard.recordSuccess();
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof ExecutionException && cause.getCause() != null) {
                cause = cause.getCause();
            }

            if (cause instanceof AlreadyExistsException) {
                throw new DuplicateRecordException("Diagnostic record with ID '" + diagnosticId + "' already exists.");
            }
            if (cause instanceof StatusRuntimeException sre && sre.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
                throw new DuplicateRecordException("Diagnostic record with ID '" + diagnosticId + "' already exists.");
            }
            if (cause instanceof ResourceExhaustedException ||
                    (cause instanceof StatusRuntimeException sre && sre.getStatus().getCode() == Status.Code.RESOURCE_EXHAUSTED)) {
                if (quotaGuard != null) {
                    quotaGuard.recordQuotaExhaustion(cause);
                }
                throw new FirestoreQuotaExhaustedException("Firestore quota exceeded during diagnostic creation");
            }

            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Failed to persist diagnostic record: " + (cause != null ? cause.getMessage() : e.getMessage()), cause);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Diagnostic persistence thread interrupted: " + ie.getMessage(), ie);
        }

        String commitTimestampIso = writeResult.getUpdateTime().toDate().toInstant().toString();

        return new DiagnosticRecordResponse(
                diagnosticId,
                authenticatedUid,
                farmId,
                farmCropId,
                crop.getId(),
                crop.getName(),
                request.getSymptoms().trim(),
                request.getAffectedPart(),
                images,
                STATUS_PENDING,
                null,
                commitTimestampIso,
                commitTimestampIso
        );
    }

    // =========================================================================
    // GET DIAGNOSTIC RECORD BY ID
    // =========================================================================

    public DiagnosticRecordResponse getDiagnosticById(String authenticatedUid, String diagnosticId) {
        if (authenticatedUid == null || authenticatedUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (diagnosticId == null || diagnosticId.trim().isEmpty()) {
            throw new IllegalArgumentException("Diagnostic ID cannot be empty.");
        }

        farmService.verifyFarmerRole(authenticatedUid);

        if (quotaGuard != null) {
            quotaGuard.checkQuotaAvailability();
        }

        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION_NAME).document(diagnosticId.trim()).get().get();
            if (quotaGuard != null) {
                quotaGuard.recordSuccess();
            }
            if (!doc.exists()) {
                throw new NoSuchElementException("Diagnostic record not found with ID: " + diagnosticId);
            }

            String recordOwner = doc.getString("farmerUid");
            if (!authenticatedUid.equals(recordOwner)) {
                throw new AccessDeniedException("Access denied. You do not own this diagnostic record.");
            }

            return mapDocToResponse(doc);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof ExecutionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof ResourceExhaustedException ||
                    (cause instanceof StatusRuntimeException sre && sre.getStatus().getCode() == Status.Code.RESOURCE_EXHAUSTED)) {
                if (quotaGuard != null) {
                    quotaGuard.recordQuotaExhaustion(cause);
                }
                throw new FirestoreQuotaExhaustedException("Firestore quota exceeded during diagnostic fetch");
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Error fetching diagnostic record: " + (cause != null ? cause.getMessage() : e.getMessage()), cause);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Diagnostic fetch thread interrupted: " + ie.getMessage(), ie);
        }
    }

    // =========================================================================
    // LIST FARMER DIAGNOSTIC HISTORY WITH CURSOR PAGINATION
    // =========================================================================

    public DiagnosticPageResponse listFarmerDiagnostics(
            String authenticatedUid,
            String statusFilter,
            String cropIdFilter,
            int limit,
            String cursor
    ) {
        if (authenticatedUid == null || authenticatedUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        farmService.verifyFarmerRole(authenticatedUid);

        int safeLimit = Math.min(Math.max(1, limit), 50);

        String safeStatus = (statusFilter != null && !statusFilter.trim().isEmpty())
                ? statusFilter.trim().toUpperCase()
                : "ALL";

        if (!"ALL".equals(safeStatus) && !STATUS_PENDING.equals(safeStatus)) {
            throw new IllegalArgumentException("Unsupported status filter: '" + statusFilter + "'. Supported values in this stage: ALL, PENDING_ANALYSIS.");
        }

        String safeCropId = (cropIdFilter != null && !cropIdFilter.trim().isEmpty())
                ? cropIdFilter.trim()
                : "NONE";

        CursorData cursorData = decodeAndValidateCursor(cursor, authenticatedUid, safeStatus, safeCropId);

        if (quotaGuard != null) {
            quotaGuard.checkQuotaAvailability();
        }

        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("farmerUid", authenticatedUid);

            if (!"ALL".equals(safeStatus)) {
                query = query.whereEqualTo("status", safeStatus);
            }
            if (!"NONE".equals(safeCropId)) {
                query = query.whereEqualTo("cropId", safeCropId);
            }

            // Ordering: createdAt DESC with documentId tie-breaker
            query = query.orderBy("createdAt", Query.Direction.DESCENDING)
                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING);

            if (cursorData != null) {
                query = query.startAfter(cursorData.timestamp, cursorData.diagnosticId);
            }

            // Bounded read: safeLimit + 1
            query = query.limit(safeLimit + 1);

            QuerySnapshot snapshot = query.get().get();
            if (quotaGuard != null) {
                quotaGuard.recordSuccess();
            }

            List<QueryDocumentSnapshot> docs = snapshot.getDocuments();
            boolean hasNext = docs.size() > safeLimit;
            int resultCount = Math.min(docs.size(), safeLimit);

            List<DiagnosticRecordResponse> items = new ArrayList<>();
            for (int i = 0; i < resultCount; i++) {
                items.add(mapDocToResponse(docs.get(i)));
            }

            String nextCursor = null;
            if (hasNext && !items.isEmpty()) {
                QueryDocumentSnapshot lastDoc = docs.get(resultCount - 1);
                Timestamp lastTimestamp = lastDoc.getTimestamp("createdAt");
                String lastId = lastDoc.getId();
                nextCursor = encodeCursor(authenticatedUid, safeStatus, safeCropId, lastTimestamp, lastId);
            }

            return new DiagnosticPageResponse(items, safeLimit, hasNext, nextCursor);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof ExecutionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof ResourceExhaustedException ||
                    (cause instanceof StatusRuntimeException sre && sre.getStatus().getCode() == Status.Code.RESOURCE_EXHAUSTED)) {
                if (quotaGuard != null) {
                    quotaGuard.recordQuotaExhaustion(cause);
                }
                throw new FirestoreQuotaExhaustedException("Firestore quota exceeded during diagnostic history query");
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Error querying diagnostic history: " + (cause != null ? cause.getMessage() : e.getMessage()), cause);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Diagnostic query thread interrupted: " + ie.getMessage(), ie);
        }
    }

    // =========================================================================
    // STORAGE VERIFICATION & BOUNDED IMAGE SIGNATURE INSPECTION
    // =========================================================================

    private Blob validateStorageObject(
            String bucketName,
            DiagnosticImagesDto.ImageMetadataDto meta,
            String expectedRole,
            String callerUid,
            String diagnosticId,
            Set<String> allowedFileNames,
            long maxSizeAllowed,
            Set<String> allowedMimes
    ) {
        String path = meta.getStoragePath();
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage path is required for " + expectedRole + " image.");
        }
        path = path.trim();

        Matcher matcher = PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid canonical storage path structure for " + expectedRole + " image: " + path);
        }

        String pathUid = matcher.group(1);
        String pathDiagId = matcher.group(2);
        String fileName = matcher.group(3);

        if (!callerUid.equals(pathUid)) {
            throw new AccessDeniedException("Storage path ownership violation: caller does not own path " + path);
        }
        if (!diagnosticId.equals(pathDiagId)) {
            throw new IllegalArgumentException("Storage path diagnosticId mismatch: path references '" + pathDiagId + "' but request specified '" + diagnosticId + "'.");
        }
        if (!allowedFileNames.contains(fileName.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unapproved filename '" + fileName + "' for " + expectedRole + " image.");
        }

        if (!meta.getFileName().equalsIgnoreCase(fileName)) {
            throw new IllegalArgumentException("Metadata fileName '" + meta.getFileName() + "' does not match path filename '" + fileName + "'.");
        }

        // Object lookup in configured Storage
        Blob blob;
        try {
            blob = storage.get(BlobId.of(bucketName, path));
        } catch (Exception e) {
            logger.error("Failed to query Storage for blob {}: {}", path, e.getMessage());
            throw new RuntimeException("Cloud Storage lookup failure for " + path + ": " + e.getMessage(), e);
        }

        if (blob == null || !blob.exists()) {
            throw new IllegalArgumentException("Required diagnostic image file does not exist in storage: " + path);
        }

        long actualSize = blob.getSize();
        if (actualSize > maxSizeAllowed) {
            throw new IllegalArgumentException(expectedRole + " image size (" + actualSize + " bytes) exceeds maximum permitted limit (" + maxSizeAllowed + " bytes) at: " + path);
        }

        String storedContentType = blob.getContentType();
        if (storedContentType == null || !allowedMimes.contains(storedContentType.toLowerCase(Locale.ROOT).trim())) {
            throw new IllegalArgumentException("Invalid stored Content-Type '" + storedContentType + "' for " + expectedRole + " image at " + path + ". Allowed: " + allowedMimes);
        }

        // Bounded server-side image signature verification.
        // Inspects the initial 16-32 bytes for valid container magic numbers (JPEG, PNG, WebP).
        // Note: This verifies container header signature consistency with declared MIME type;
        // it does not perform full image decoding, pixel raster validation, or deep decompression.
        byte[] headerBytes;
        try {
            headerBytes = readHeaderBytes(blob, path);
        } catch (IOException ioe) {
            logger.error("I/O transport failure while reading header bytes from blob {}: {}", path, ioe.getMessage());
            throw new RuntimeException("I/O failure while inspecting image header for " + path + ": " + ioe.getMessage(), ioe);
        }

        verifyImageSignature(headerBytes, storedContentType, path);

        return blob;
    }

    /**
     * Reads up to 32 header bytes from the Storage blob for bounded image signature verification.
     * Does not perform full image decoding or pixel validation.
     */
    public byte[] readHeaderBytes(Blob blob, String storagePath) throws IOException {
        if (blob.getSize() < REQUIRED_HEADER_BYTES) {
            throw new InvalidImageSignatureException("Image file size (" + blob.getSize() + " bytes) is smaller than required header length (16 bytes) at: " + storagePath);
        }

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTES_CAP);
        int totalBytesRead = 0;

        try (ReadChannel channel = blob.reader()) {
            while (buffer.hasRemaining() && totalBytesRead < REQUIRED_HEADER_BYTES) {
                int read = channel.read(buffer);
                if (read == -1) {
                    break;
                }
                totalBytesRead += read;
            }

            if (totalBytesRead < REQUIRED_HEADER_BYTES) {
                throw new InvalidImageSignatureException("Premature EOF encountered while reading image header (read " + totalBytesRead + " of 16 bytes) at: " + storagePath);
            }

            buffer.flip();
            byte[] header = new byte[buffer.remaining()];
            buffer.get(header);
            return header;
        }
    }

    /**
     * Performs bounded server-side image signature verification against container magic numbers
     * for JPEG, PNG, and WebP formats.
     * Does not claim full image decoding or pixel validation.
     */
    public void verifyImageSignature(byte[] header, String contentType, String storagePath) {
        String normalized = contentType != null ? contentType.toLowerCase(Locale.ROOT).trim() : "";
        boolean valid = switch (normalized) {
            case "image/jpeg" ->
                    header.length >= 3 &&
                            (header[0] & 0xFF) == 0xFF &&
                            (header[1] & 0xFF) == 0xD8 &&
                            (header[2] & 0xFF) == 0xFF;
            case "image/png" ->
                    header.length >= 8 &&
                            (header[0] & 0xFF) == 0x89 &&
                            header[1] == 0x50 &&
                            header[2] == 0x4E &&
                            header[3] == 0x47 &&
                            header[4] == 0x0D &&
                            header[5] == 0x0A &&
                            header[6] == 0x1A &&
                            header[7] == 0x0A;
            case "image/webp" ->
                    header.length >= 12 &&
                            header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
                            header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };

        if (!valid) {
            throw new InvalidImageSignatureException("Server-side image signature verification failed for file " + storagePath + ": header bytes do not match declared format '" + contentType + "'.");
        }
    }

    // =========================================================================
    // CURSOR ENCODING & VERIFICATION
    // =========================================================================

    public String encodeCursor(String callerUid, String statusFilter, String cropIdFilter, Timestamp timestamp, String diagnosticId) {
        long seconds = timestamp != null ? timestamp.getSeconds() : 0L;
        int nanos = timestamp != null ? timestamp.getNanos() : 0;
        String payload = String.format("DIAG|%s|%s|%s|%d:%d|%s", callerUid, statusFilter, cropIdFilter, seconds, nanos, diagnosticId);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] signature = computeHmac(payloadBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes) + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    public CursorData decodeAndValidateCursor(String cursor, String callerUid, String expectedStatus, String expectedCropId) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }

        String[] parts = cursor.trim().split("\\.", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Malformed pagination cursor format.");
        }

        byte[] payloadBytes;
        byte[] receivedSig;
        try {
            payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            receivedSig = Base64.getUrlDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed base64 in pagination cursor.", e);
        }

        byte[] expectedSig = computeHmac(payloadBytes);
        if (!MessageDigest.isEqual(expectedSig, receivedSig)) {
            throw new IllegalArgumentException("Invalid or tampered pagination cursor signature.");
        }

        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        String[] fields = payload.split("\\|", 6);
        if (fields.length != 6 || !"DIAG".equals(fields[0])) {
            throw new IllegalArgumentException("Malformed payload structure in pagination cursor.");
        }

        String cursorOwner = fields[1];
        String cursorStatus = fields[2];
        String cursorCropId = fields[3];
        String cursorTsRaw = fields[4];
        String cursorDiagId = fields[5];

        if (!callerUid.equals(cursorOwner)) {
            throw new AccessDeniedException("Cursor owner mismatch: Pagination cursors cannot be transferred across users.");
        }
        if (!expectedStatus.equalsIgnoreCase(cursorStatus)) {
            throw new IllegalArgumentException("Cursor filter mismatch: Cursor was generated for status '" + cursorStatus + "', but query requested '" + expectedStatus + "'.");
        }
        if (!expectedCropId.equalsIgnoreCase(cursorCropId)) {
            throw new IllegalArgumentException("Cursor filter mismatch: Cursor was generated for cropId '" + cursorCropId + "', but query requested '" + expectedCropId + "'.");
        }

        String[] tsParts = cursorTsRaw.split(":", 2);
        if (tsParts.length != 2) {
            throw new IllegalArgumentException("Malformed timestamp in pagination cursor.");
        }

        long seconds;
        int nanos;
        try {
            seconds = Long.parseLong(tsParts[0]);
            nanos = Integer.parseInt(tsParts[1]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid numeric timestamp representation in pagination cursor.", nfe);
        }

        Timestamp timestamp = Timestamp.ofTimeSecondsAndNanos(seconds, nanos);
        return new CursorData(cursorOwner, cursorStatus, cursorCropId, timestamp, cursorDiagId);
    }

    private byte[] computeHmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(cursorSigningKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 signature for cursor", e);
        }
    }

    // =========================================================================
    // MAPPING HELPERS
    // =========================================================================

    private Map<String, Object> mapImagesToFirestoreMap(
            DiagnosticImagesDto images,
            Blob blobOriginal,
            Blob blobDiagnostic,
            Blob blobDisplay
    ) {
        Map<String, Object> map = new HashMap<>();
        map.put("original", Map.of(
                "storagePath", images.getOriginal().getStoragePath(),
                "fileName", images.getOriginal().getFileName(),
                "mimeType", blobOriginal.getContentType(),
                "sizeBytes", blobOriginal.getSize(),
                "width", images.getOriginal().getWidth() != null ? images.getOriginal().getWidth() : 0,
                "height", images.getOriginal().getHeight() != null ? images.getOriginal().getHeight() : 0
        ));
        map.put("diagnostic", Map.of(
                "storagePath", images.getDiagnostic().getStoragePath(),
                "fileName", images.getDiagnostic().getFileName(),
                "mimeType", blobDiagnostic.getContentType(),
                "sizeBytes", blobDiagnostic.getSize(),
                "width", images.getDiagnostic().getWidth() != null ? images.getDiagnostic().getWidth() : 0,
                "height", images.getDiagnostic().getHeight() != null ? images.getDiagnostic().getHeight() : 0
        ));
        map.put("display", Map.of(
                "storagePath", images.getDisplay().getStoragePath(),
                "fileName", images.getDisplay().getFileName(),
                "mimeType", blobDisplay.getContentType(),
                "sizeBytes", blobDisplay.getSize(),
                "width", images.getDisplay().getWidth() != null ? images.getDisplay().getWidth() : 0,
                "height", images.getDisplay().getHeight() != null ? images.getDisplay().getHeight() : 0
        ));
        return map;
    }

    @SuppressWarnings("unchecked")
    private DiagnosticRecordResponse mapDocToResponse(DocumentSnapshot doc) {
        String id = doc.getId();
        String farmerUid = doc.getString("farmerUid");
        String farmId = doc.getString("farmId");
        String farmCropId = doc.getString("farmCropId");
        String cropId = doc.getString("cropId");
        String cropName = doc.getString("cropName");
        String symptoms = doc.getString("symptoms");
        String affectedPart = doc.getString("affectedPart");
        String status = doc.getString("status");
        Object aiDiagnosis = doc.get("aiDiagnosis");

        Timestamp createdTs = doc.getTimestamp("createdAt");
        Timestamp updatedTs = doc.getTimestamp("updatedAt");
        String createdAtIso = createdTs != null ? createdTs.toDate().toInstant().toString() : Instant.now().toString();
        String updatedAtIso = updatedTs != null ? updatedTs.toDate().toInstant().toString() : Instant.now().toString();

        Map<String, Object> imagesMap = (Map<String, Object>) doc.get("images");
        DiagnosticImagesDto imagesDto = mapFirestoreImagesToDto(imagesMap);

        return new DiagnosticRecordResponse(
                id,
                farmerUid,
                farmId,
                farmCropId,
                cropId,
                cropName,
                symptoms,
                affectedPart,
                imagesDto,
                status,
                aiDiagnosis,
                createdAtIso,
                updatedAtIso
        );
    }

    @SuppressWarnings("unchecked")
    private DiagnosticImagesDto mapFirestoreImagesToDto(Map<String, Object> map) {
        if (map == null) return null;

        Map<String, Object> origMap = (Map<String, Object>) map.get("original");
        Map<String, Object> diagMap = (Map<String, Object>) map.get("diagnostic");
        Map<String, Object> dispMap = (Map<String, Object>) map.get("display");

        DiagnosticImagesDto.ImageMetadataDto orig = mapSingleImage(origMap);
        DiagnosticImagesDto.ImageMetadataDto diag = mapSingleImage(diagMap);
        DiagnosticImagesDto.ImageMetadataDto disp = mapSingleImage(dispMap);

        return new DiagnosticImagesDto(orig, diag, disp);
    }

    private DiagnosticImagesDto.ImageMetadataDto mapSingleImage(Map<String, Object> img) {
        if (img == null) return null;
        String path = (String) img.get("storagePath");
        String fileName = (String) img.get("fileName");
        String mime = (String) img.get("mimeType");
        Number size = (Number) img.get("sizeBytes");
        Number w = (Number) img.get("width");
        Number h = (Number) img.get("height");

        return new DiagnosticImagesDto.ImageMetadataDto(
                path,
                fileName,
                mime,
                size != null ? size.longValue() : 0L,
                w != null ? w.intValue() : 0,
                h != null ? h.intValue() : 0
        );
    }

    public static class CursorData {
        public final String ownerUid;
        public final String status;
        public final String cropId;
        public final Timestamp timestamp;
        public final String diagnosticId;

        public CursorData(String ownerUid, String status, String cropId, Timestamp timestamp, String diagnosticId) {
            this.ownerUid = ownerUid;
            this.status = status;
            this.cropId = cropId;
            this.timestamp = timestamp;
            this.diagnosticId = diagnosticId;
        }
    }
}
