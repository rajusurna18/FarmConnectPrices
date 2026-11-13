package com.farmlink.api.service;

import com.farmlink.api.config.firebase.FirebaseProperties;
import com.farmlink.api.dto.*;
import com.farmlink.api.exception.DuplicateRecordException;
import com.farmlink.api.exception.FirestoreQuotaExhaustedException;
import com.farmlink.api.exception.InvalidImageSignatureException;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.ReadChannel;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CropDiagnosticServiceTest {

    private Firestore firestore;
    private Storage storage;
    private CropMasterService cropMasterService;
    private FarmService farmService;
    private FirestoreQuotaGuard quotaGuard;
    private CollectionReference collectionRef;
    private DocumentReference docRef;
    private WriteResult writeResult;
    private Blob blobOriginal;
    private Blob blobDiagnostic;
    private Blob blobDisplay;

    private FirebaseProperties firebaseProperties;
    private MockEnvironment environment;
    private CropDiagnosticService service;

    private static final String TEST_UID = "farmer-uid-123";
    private static final String DIAGNOSTIC_ID = "diag-crop-001";
    private static final String CROP_ID = "crop-tomato-01";
    private static final String FARM_ID = "farm-warangal-01";
    private static final String BUCKET_NAME = "farmconnectprices.appspot.com";
    private static final String TEST_SECRET = "abcdef0123456789abcdef0123456789abcdef0123456789";

    // Magic Byte Signatures
    private static final byte[] VALID_JPEG_BYTES = new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x60
    };

    private static final byte[] VALID_PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    private static final byte[] VALID_WEBP_BYTES = new byte[] {
            0x52, 0x49, 0x46, 0x46, // "RIFF"
            0x24, 0x00, 0x00, 0x00, // file length
            0x57, 0x45, 0x42, 0x50, // "WEBP"
            0x56, 0x50, 0x38, 0x20  // "VP8 "
    };

    @BeforeEach
    void setUp() {
        firestore = mock(Firestore.class);
        storage = mock(Storage.class);
        cropMasterService = mock(CropMasterService.class);
        farmService = mock(FarmService.class);
        quotaGuard = mock(FirestoreQuotaGuard.class);
        collectionRef = mock(CollectionReference.class);
        docRef = mock(DocumentReference.class);
        writeResult = mock(WriteResult.class);
        blobOriginal = mock(Blob.class);
        blobDiagnostic = mock(Blob.class);
        blobDisplay = mock(Blob.class);

        firebaseProperties = new FirebaseProperties();
        firebaseProperties.setProjectId("farmconnectprices");
        firebaseProperties.setStorageBucket(BUCKET_NAME);

        environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        service = new CropDiagnosticService(
                firestore,
                storage,
                firebaseProperties,
                cropMasterService,
                farmService,
                quotaGuard,
                TEST_SECRET,
                environment
        );
    }

    private CreateDiagnosticRequest createSampleRequest() {
        DiagnosticImagesDto images = new DiagnosticImagesDto(
                new DiagnosticImagesDto.ImageMetadataDto("users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg", "original.jpg", "image/jpeg", 1024L, 800, 600),
                new DiagnosticImagesDto.ImageMetadataDto("users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/diagnostic.webp", "diagnostic.webp", "image/webp", 512L, 512, 512),
                new DiagnosticImagesDto.ImageMetadataDto("users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/display.webp", "display.webp", "image/webp", 256L, 256, 256)
        );

        CreateDiagnosticRequest request = new CreateDiagnosticRequest();
        request.setDiagnosticId(DIAGNOSTIC_ID);
        request.setFarmId(null);
        request.setCropId(CROP_ID);
        request.setSymptoms("Leaves show severe chlorosis and brown spots");
        request.setAffectedPart("LEAF");
        request.setImages(images);
        return request;
    }

    private void mockSuccessfulBlobs(byte[] origMagic, byte[] diagMagic, byte[] dispMagic) throws IOException {
        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg")))
                .thenReturn(blobOriginal);
        when(blobOriginal.exists()).thenReturn(true);
        when(blobOriginal.getSize()).thenReturn(1024L);
        when(blobOriginal.getContentType()).thenReturn("image/jpeg");
        when(blobOriginal.reader()).thenAnswer(inv -> new MockReadChannel(origMagic));

        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/diagnostic.webp")))
                .thenReturn(blobDiagnostic);
        when(blobDiagnostic.exists()).thenReturn(true);
        when(blobDiagnostic.getSize()).thenReturn(512L);
        when(blobDiagnostic.getContentType()).thenReturn("image/webp");
        when(blobDiagnostic.reader()).thenAnswer(inv -> new MockReadChannel(diagMagic));

        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/display.webp")))
                .thenReturn(blobDisplay);
        when(blobDisplay.exists()).thenReturn(true);
        when(blobDisplay.getSize()).thenReturn(256L);
        when(blobDisplay.getContentType()).thenReturn("image/webp");
        when(blobDisplay.reader()).thenAnswer(inv -> new MockReadChannel(dispMagic));
    }

    // =========================================================================
    // 0. BUCKET CONFIGURATION & SELECTION TESTS
    // =========================================================================

    @Test
    @DisplayName("CropDiagnosticService throws IllegalStateException when storageBucket is null")
    void initializeStorageBucket_ThrowsIllegalStateException_WhenBucketNull() {
        FirebaseProperties props = new FirebaseProperties();
        props.setProjectId("farmconnectprices");
        props.setStorageBucket(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new CropDiagnosticService(
                    firestore, storage, props, cropMasterService, farmService, quotaGuard, TEST_SECRET, environment
            );
        });
        assertTrue(ex.getMessage().contains("app.firebase.storage-bucket") || ex.getMessage().contains("FIREBASE_STORAGE_BUCKET"));
    }

    @Test
    @DisplayName("CropDiagnosticService throws IllegalStateException when storageBucket is blank")
    void initializeStorageBucket_ThrowsIllegalStateException_WhenBucketBlank() {
        FirebaseProperties props = new FirebaseProperties();
        props.setProjectId("farmconnectprices");
        props.setStorageBucket("   ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new CropDiagnosticService(
                    firestore, storage, props, cropMasterService, farmService, quotaGuard, TEST_SECRET, environment
            );
        });
        assertTrue(ex.getMessage().contains("app.firebase.storage-bucket") || ex.getMessage().contains("FIREBASE_STORAGE_BUCKET"));
    }

    @Test
    @DisplayName("resolveStorageBucket returns explicitly configured bucket without hardcoded fallback")
    void resolveStorageBucket_ReturnsExplicitlyConfiguredBucket() {
        FirebaseProperties props = new FirebaseProperties();
        props.setProjectId("farmconnectprices");
        props.setStorageBucket("custom-crops-diagnostic-bucket.appspot.com");

        CropDiagnosticService customService = new CropDiagnosticService(
                firestore, storage, props, cropMasterService, farmService, quotaGuard, TEST_SECRET, environment
        );
        assertEquals("custom-crops-diagnostic-bucket.appspot.com", customService.resolveStorageBucket());
    }

    @Test
    @DisplayName("CropDiagnosticService fails startup in production profile when storageBucket is missing")
    void initializeStorageBucket_ThrowsIllegalStateException_WhenBucketMissingInProduction() {
        FirebaseProperties props = new FirebaseProperties();
        props.setProjectId("farmconnectprices");
        props.setStorageBucket(null);

        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new CropDiagnosticService(
                    firestore, storage, props, cropMasterService, farmService, quotaGuard, "01234567890123456789012345678901", prodEnv
            );
        });
        assertTrue(ex.getMessage().contains("app.firebase.storage-bucket") || ex.getMessage().contains("FIREBASE_STORAGE_BUCKET"));
    }

    // =========================================================================
    // 1. STORAGE PATH & VALIDATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Path validation rejects path belonging to different farmer")
    void validateStoragePath_MismatchedFarmerUid_ThrowsAccessDenied() {
        CreateDiagnosticRequest request = createSampleRequest();
        request.getImages().getOriginal().setStoragePath("users/other-farmer-999/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg");

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("Storage path ownership violation") || ex.getMessage().contains("caller does not own path"));
    }

    @Test
    @DisplayName("Path validation rejects path with mismatched diagnosticId")
    void validateStoragePath_MismatchedDiagnosticId_ThrowsIllegalArgument() {
        CreateDiagnosticRequest request = createSampleRequest();
        request.getImages().getOriginal().setStoragePath("users/" + TEST_UID + "/diagnostics/diff-diag-id/original.jpg");

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("Storage path diagnosticId mismatch"));
    }

    @Test
    @DisplayName("Path validation rejects unauthorized filenames")
    void validateStoragePath_UnauthorizedFilename_ThrowsIllegalArgument() {
        CreateDiagnosticRequest request = createSampleRequest();
        request.getImages().getOriginal().setStoragePath("users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/malicious.sh");

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("Unapproved filename"));
    }

    @Test
    @DisplayName("Storage validation throws when object does not exist in bucket")
    void validateStorageObject_ObjectMissing_ThrowsIllegalArgument() {
        CreateDiagnosticRequest request = createSampleRequest();

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);
        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg")))
                .thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("does not exist in storage"));
    }

    @Test
    @DisplayName("Storage validation rejects object exceeding size cap")
    void validateStorageObject_Oversized_ThrowsIllegalArgument() {
        CreateDiagnosticRequest request = createSampleRequest();

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg")))
                .thenReturn(blobOriginal);
        when(blobOriginal.exists()).thenReturn(true);
        when(blobOriginal.getSize()).thenReturn(11L * 1024 * 1024); // 11 MiB > 10 MiB

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("exceeds maximum permitted limit"));
    }

    @Test
    @DisplayName("Storage validation rejects invalid stored MIME type")
    void validateStorageObject_InvalidMime_ThrowsIllegalArgument() {
        CreateDiagnosticRequest request = createSampleRequest();

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg")))
                .thenReturn(blobOriginal);
        when(blobOriginal.exists()).thenReturn(true);
        when(blobOriginal.getSize()).thenReturn(1024L);
        when(blobOriginal.getContentType()).thenReturn("application/pdf");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("Invalid stored Content-Type"));
    }

    // =========================================================================
    // 2. BYTE-SIGNATURE VERIFICATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Magic bytes verification throws InvalidImageSignatureException on corrupted/spoofed bytes")
    void validateImageBytes_CorruptedBytes_ThrowsInvalidSignature() {
        CreateDiagnosticRequest request = createSampleRequest();

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        byte[] fakeBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };

        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg")))
                .thenReturn(blobOriginal);
        when(blobOriginal.exists()).thenReturn(true);
        when(blobOriginal.getSize()).thenReturn(1024L);
        when(blobOriginal.getContentType()).thenReturn("image/jpeg");
        when(blobOriginal.reader()).thenReturn(new MockReadChannel(fakeBytes));

        InvalidImageSignatureException ex = assertThrows(InvalidImageSignatureException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("image signature verification failed"));
    }

    @Test
    @DisplayName("Magic bytes verification throws InvalidImageSignatureException when file is shorter than 16 bytes")
    void validateImageBytes_TruncatedFile_ThrowsInvalidSignature() {
        CreateDiagnosticRequest request = createSampleRequest();

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        byte[] truncatedBytes = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg")))
                .thenReturn(blobOriginal);
        when(blobOriginal.exists()).thenReturn(true);
        when(blobOriginal.getSize()).thenReturn(1024L);
        when(blobOriginal.getContentType()).thenReturn("image/jpeg");
        when(blobOriginal.reader()).thenReturn(new MockReadChannel(truncatedBytes));

        InvalidImageSignatureException ex = assertThrows(InvalidImageSignatureException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("Premature EOF encountered"));
    }

    @Test
    @DisplayName("Magic bytes verification correctly handles partial reads across chunks")
    void validateImageBytes_PartialReads_SuccessfullyAggregates() throws Exception {
        CreateDiagnosticRequest request = createSampleRequest();

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        // ReadChannel returning 4 bytes, then 6 bytes, then 6 bytes (total 16 bytes)
        MockChunkedReadChannel chunkedChannel = new MockChunkedReadChannel(VALID_JPEG_BYTES, new int[] { 4, 6, 6 });

        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg")))
                .thenReturn(blobOriginal);
        when(blobOriginal.exists()).thenReturn(true);
        when(blobOriginal.getSize()).thenReturn(1024L);
        when(blobOriginal.getContentType()).thenReturn("image/jpeg");
        when(blobOriginal.reader()).thenReturn(chunkedChannel);

        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/diagnostic.webp")))
                .thenReturn(blobDiagnostic);
        when(blobDiagnostic.exists()).thenReturn(true);
        when(blobDiagnostic.getSize()).thenReturn(512L);
        when(blobDiagnostic.getContentType()).thenReturn("image/webp");
        when(blobDiagnostic.reader()).thenReturn(new MockReadChannel(VALID_WEBP_BYTES));

        when(storage.get(BlobId.of(BUCKET_NAME, "users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/display.webp")))
                .thenReturn(blobDisplay);
        when(blobDisplay.exists()).thenReturn(true);
        when(blobDisplay.getSize()).thenReturn(256L);
        when(blobDisplay.getContentType()).thenReturn("image/webp");
        when(blobDisplay.reader()).thenReturn(new MockReadChannel(VALID_WEBP_BYTES));
        when(blobDisplay.getContentType()).thenReturn("image/webp");
        when(blobDisplay.reader()).thenReturn(new MockReadChannel(VALID_WEBP_BYTES));

        when(firestore.collection("cropDiagnostics")).thenReturn(collectionRef);
        when(collectionRef.document(DIAGNOSTIC_ID)).thenReturn(docRef);
        when(docRef.create(anyMap())).thenReturn(ApiFutures.immediateFuture(writeResult));
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.ofTimeSecondsAndNanos(1700000000, 123456789));

        DiagnosticRecordResponse response = service.createDiagnostic(TEST_UID, request);
        assertNotNull(response);
        assertEquals(DIAGNOSTIC_ID, response.getId());
        assertEquals("PENDING_ANALYSIS", response.getStatus());
    }

    // =========================================================================
    // 3. ATOMIC CREATION & ERROR MAPPINGS
    // =========================================================================

    @Test
    @DisplayName("Create diagnostic maps AlreadyExistsException to DuplicateRecordException (409)")
    void createDiagnostic_AlreadyExists_ThrowsDuplicateRecordException() throws Exception {
        CreateDiagnosticRequest request = createSampleRequest();

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        mockSuccessfulBlobs(VALID_JPEG_BYTES, VALID_WEBP_BYTES, VALID_WEBP_BYTES);

        when(firestore.collection("cropDiagnostics")).thenReturn(collectionRef);
        when(collectionRef.document(DIAGNOSTIC_ID)).thenReturn(docRef);

        StatusRuntimeException statusEx = new StatusRuntimeException(Status.ALREADY_EXISTS);
        ExecutionException execEx = new ExecutionException("Already exists", statusEx);
        when(docRef.create(anyMap())).thenReturn(ApiFutures.immediateFailedFuture(execEx));

        DuplicateRecordException ex = assertThrows(DuplicateRecordException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Create diagnostic maps ResourceExhaustedException to FirestoreQuotaExhaustedException (503)")
    void createDiagnostic_ResourceExhausted_ThrowsFirestoreQuotaExhaustedException() throws Exception {
        CreateDiagnosticRequest request = createSampleRequest();

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        mockSuccessfulBlobs(VALID_JPEG_BYTES, VALID_WEBP_BYTES, VALID_WEBP_BYTES);

        when(firestore.collection("cropDiagnostics")).thenReturn(collectionRef);
        when(collectionRef.document(DIAGNOSTIC_ID)).thenReturn(docRef);

        StatusRuntimeException statusEx = new StatusRuntimeException(Status.RESOURCE_EXHAUSTED);
        ExecutionException execEx = new ExecutionException("Quota exceeded", statusEx);
        when(docRef.create(anyMap())).thenReturn(ApiFutures.immediateFailedFuture(execEx));

        FirestoreQuotaExhaustedException ex = assertThrows(FirestoreQuotaExhaustedException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("quota exceeded"));
        verify(quotaGuard).recordQuotaExhaustion(any());
    }

    // =========================================================================
    // 4. GET RECORD BY ID
    // =========================================================================

    @Test
    @DisplayName("Get diagnostic by ID throws AccessDeniedException when caller is not record owner")
    void getDiagnosticById_NotOwner_ThrowsAccessDenied() throws Exception {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(firestore.collection("cropDiagnostics")).thenReturn(collectionRef);
        when(collectionRef.document(DIAGNOSTIC_ID)).thenReturn(docRef);
        when(docRef.get()).thenReturn(ApiFutures.immediateFuture(doc));
        when(doc.exists()).thenReturn(true);
        when(doc.getString("farmerUid")).thenReturn("other-farmer-456");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> {
            service.getDiagnosticById(TEST_UID, DIAGNOSTIC_ID);
        });
        assertTrue(ex.getMessage().contains("You do not own this diagnostic record"));
    }

    @Test
    @DisplayName("Get diagnostic by ID throws NoSuchElementException when document does not exist")
    void getDiagnosticById_NotFound_ThrowsNoSuchElement() throws Exception {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(firestore.collection("cropDiagnostics")).thenReturn(collectionRef);
        when(collectionRef.document(DIAGNOSTIC_ID)).thenReturn(docRef);
        when(docRef.get()).thenReturn(ApiFutures.immediateFuture(doc));
        when(doc.exists()).thenReturn(false);

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> {
            service.getDiagnosticById(TEST_UID, DIAGNOSTIC_ID);
        });
        assertTrue(ex.getMessage().contains("Diagnostic record not found"));
    }

    @Test
    @DisplayName("Get diagnostic by ID returns DiagnosticRecordResponse when owner matches")
    void getDiagnosticById_Success() throws Exception {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(firestore.collection("cropDiagnostics")).thenReturn(collectionRef);
        when(collectionRef.document(DIAGNOSTIC_ID)).thenReturn(docRef);
        when(docRef.get()).thenReturn(ApiFutures.immediateFuture(doc));
        when(doc.exists()).thenReturn(true);
        when(doc.getString("farmerUid")).thenReturn(TEST_UID);
        when(doc.getId()).thenReturn(DIAGNOSTIC_ID);
        when(doc.getString("cropId")).thenReturn(CROP_ID);
        when(doc.getString("cropName")).thenReturn("Tomato");
        when(doc.getString("symptoms")).thenReturn("Yellow leaves");
        when(doc.getString("affectedPart")).thenReturn("LEAF");
        when(doc.getString("status")).thenReturn("PENDING_ANALYSIS");
        Timestamp ts = Timestamp.ofTimeSecondsAndNanos(1700000000, 0);
        when(doc.getTimestamp("createdAt")).thenReturn(ts);
        when(doc.getTimestamp("updatedAt")).thenReturn(ts);

        DiagnosticRecordResponse res = service.getDiagnosticById(TEST_UID, DIAGNOSTIC_ID);
        assertNotNull(res);
        assertEquals(DIAGNOSTIC_ID, res.getId());
        assertEquals(TEST_UID, res.getFarmerUid());
        assertEquals("Tomato", res.getCropName());
    }

    @Test
    @DisplayName("Create diagnostic attaches farmCropId when farm and crop relationship exists")
    void createDiagnostic_WithFarmAndFarmCropsLinkage() throws Exception {
        CreateDiagnosticRequest request = createSampleRequest();
        request.setFarmId("farm-warangal-01");

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        mockSuccessfulBlobs(VALID_JPEG_BYTES, VALID_WEBP_BYTES, VALID_WEBP_BYTES);

        Query farmCropsQuery = mock(Query.class);
        CollectionReference farmCropsCol = mock(CollectionReference.class);
        QuerySnapshot fcSnap = mock(QuerySnapshot.class);
        QueryDocumentSnapshot fcDoc = mock(QueryDocumentSnapshot.class);

        when(firestore.collection("farmCrops")).thenReturn(farmCropsCol);
        when(farmCropsCol.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);
        when(farmCropsQuery.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);
        when(farmCropsQuery.get()).thenReturn(ApiFutures.immediateFuture(fcSnap));
        when(fcSnap.isEmpty()).thenReturn(false);
        when(fcSnap.getDocuments()).thenReturn(List.of(fcDoc));
        when(fcDoc.getId()).thenReturn("farmcrop-provenance-123");

        when(firestore.collection("cropDiagnostics")).thenReturn(collectionRef);
        when(collectionRef.document(DIAGNOSTIC_ID)).thenReturn(docRef);
        when(docRef.create(anyMap())).thenReturn(ApiFutures.immediateFuture(writeResult));
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.ofTimeSecondsAndNanos(1700000000, 0));

        DiagnosticRecordResponse response = service.createDiagnostic(TEST_UID, request);
        assertNotNull(response);
        assertEquals("farmcrop-provenance-123", response.getFarmCropId());
        assertEquals("farm-warangal-01", response.getFarmId());
    }

    @Test
    @DisplayName("Create diagnostic with farm succeeds with null farmCropId when no farmCrops document exists")
    void createDiagnostic_WithFarmAndNoMatchingFarmCrops_SucceedsWithNullFarmCropId() throws Exception {
        CreateDiagnosticRequest request = createSampleRequest();
        request.setFarmId("farm-warangal-01");

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        mockSuccessfulBlobs(VALID_JPEG_BYTES, VALID_WEBP_BYTES, VALID_WEBP_BYTES);

        Query farmCropsQuery = mock(Query.class);
        CollectionReference farmCropsCol = mock(CollectionReference.class);
        QuerySnapshot fcSnap = mock(QuerySnapshot.class);

        when(firestore.collection("farmCrops")).thenReturn(farmCropsCol);
        when(farmCropsCol.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);
        when(farmCropsQuery.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);
        when(farmCropsQuery.get()).thenReturn(ApiFutures.immediateFuture(fcSnap));
        when(fcSnap.isEmpty()).thenReturn(true);

        when(firestore.collection("cropDiagnostics")).thenReturn(collectionRef);
        when(collectionRef.document(DIAGNOSTIC_ID)).thenReturn(docRef);
        when(docRef.create(anyMap())).thenReturn(ApiFutures.immediateFuture(writeResult));
        when(writeResult.getUpdateTime()).thenReturn(Timestamp.ofTimeSecondsAndNanos(1700000000, 0));

        DiagnosticRecordResponse response = service.createDiagnostic(TEST_UID, request);
        assertNotNull(response);
        assertNull(response.getFarmCropId());
        assertEquals("farm-warangal-01", response.getFarmId());
        verify(quotaGuard, atLeastOnce()).recordSuccess();
    }

    @Test
    @DisplayName("Create diagnostic with farm throws FirestoreQuotaExhaustedException when farmCrops query exhausts quota")
    void createDiagnostic_WithFarm_ThrowsQuotaExhausted_WhenFarmCropsExceedsQuota() throws Exception {
        CreateDiagnosticRequest request = createSampleRequest();
        request.setFarmId("farm-warangal-01");

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        mockSuccessfulBlobs(VALID_JPEG_BYTES, VALID_WEBP_BYTES, VALID_WEBP_BYTES);

        Query farmCropsQuery = mock(Query.class);
        CollectionReference farmCropsCol = mock(CollectionReference.class);

        when(firestore.collection("farmCrops")).thenReturn(farmCropsCol);
        when(farmCropsCol.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);
        when(farmCropsQuery.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);

        StatusRuntimeException statusEx = new StatusRuntimeException(Status.RESOURCE_EXHAUSTED);
        ExecutionException execEx = new ExecutionException("Quota exceeded", statusEx);
        when(farmCropsQuery.get()).thenReturn(ApiFutures.immediateFailedFuture(execEx));

        FirestoreQuotaExhaustedException ex = assertThrows(FirestoreQuotaExhaustedException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("quota exceeded during farmCrops linkage lookup"));
        verify(quotaGuard).recordQuotaExhaustion(any());
    }

    @Test
    @DisplayName("Create diagnostic with farm throws AccessDeniedException when farmCrops query is permission denied")
    void createDiagnostic_WithFarm_ThrowsAccessDenied_WhenFarmCropsPermissionDenied() throws Exception {
        CreateDiagnosticRequest request = createSampleRequest();
        request.setFarmId("farm-warangal-01");

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        mockSuccessfulBlobs(VALID_JPEG_BYTES, VALID_WEBP_BYTES, VALID_WEBP_BYTES);

        Query farmCropsQuery = mock(Query.class);
        CollectionReference farmCropsCol = mock(CollectionReference.class);

        when(firestore.collection("farmCrops")).thenReturn(farmCropsCol);
        when(farmCropsCol.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);
        when(farmCropsQuery.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);

        StatusRuntimeException statusEx = new StatusRuntimeException(Status.PERMISSION_DENIED);
        ExecutionException execEx = new ExecutionException("Permission denied", statusEx);
        when(farmCropsQuery.get()).thenReturn(ApiFutures.immediateFailedFuture(execEx));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("Access denied querying farmCrops"));
    }

    @Test
    @DisplayName("Create diagnostic with farm throws RuntimeException when farmCrops query suffers transient infrastructure failure")
    void createDiagnostic_WithFarm_ThrowsRuntimeException_WhenFarmCropsInfrastructureFailure() throws Exception {
        CreateDiagnosticRequest request = createSampleRequest();
        request.setFarmId("farm-warangal-01");

        CropResponse crop = new CropResponse(CROP_ID, "Tomato", "SOLANACEAE", "Solanum lycopersicum", "ACTIVE");
        when(cropMasterService.getCropById(CROP_ID)).thenReturn(crop);

        mockSuccessfulBlobs(VALID_JPEG_BYTES, VALID_WEBP_BYTES, VALID_WEBP_BYTES);

        Query farmCropsQuery = mock(Query.class);
        CollectionReference farmCropsCol = mock(CollectionReference.class);

        when(firestore.collection("farmCrops")).thenReturn(farmCropsCol);
        when(farmCropsCol.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);
        when(farmCropsQuery.whereEqualTo(anyString(), any())).thenReturn(farmCropsQuery);

        StatusRuntimeException statusEx = new StatusRuntimeException(Status.UNAVAILABLE);
        ExecutionException execEx = new ExecutionException("Service unavailable", statusEx);
        when(farmCropsQuery.get()).thenReturn(ApiFutures.immediateFailedFuture(execEx));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.createDiagnostic(TEST_UID, request);
        });
        assertTrue(ex.getMessage().contains("Firestore infrastructure failure during farmCrops linkage query"));
    }

    // =========================================================================
    // 5. CURSOR PAGINATION & SIGNATURE VALIDATION
    // =========================================================================

    @Test
    @DisplayName("Tampered cursor throws IllegalArgumentException")
    void listDiagnostics_TamperedCursor_ThrowsIllegalArgument() {
        String tamperedCursor = "eyJ0aW1lc3RhbXAiOnsic2Vjb25kcyI6MTcwMDAwMDAwMCwibmFub3MiOjEyMzQ1Njc4OX0sImRpYWdub3N0aWNJZCI6ImRpYWctMDAxIn0.invalidSignature";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.listFarmerDiagnostics(TEST_UID, "ALL", null, 20, tamperedCursor);
        });
        assertTrue(ex.getMessage().contains("Invalid or tampered"));
    }

    @Test
    @DisplayName("Cursor issued to different farmer throws AccessDeniedException")
    void listDiagnostics_CrossUserCursorReplay_ThrowsAccessDenied() {
        // Create valid cursor for farmer-A
        CropDiagnosticService serviceOther = new CropDiagnosticService(
                firestore, storage, firebaseProperties, cropMasterService, farmService, quotaGuard, TEST_SECRET, environment
        );

        Timestamp ts = Timestamp.ofTimeSecondsAndNanos(1700000000, 500000000);
        String cursorForOtherUser = serviceOther.encodeCursor("farmer-A", "ALL", "NONE", ts, "diag-1");

        // Farmer B attempts to query using Farmer A's cursor
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> {
            service.listFarmerDiagnostics(TEST_UID, "ALL", null, 20, cursorForOtherUser);
        });
        assertTrue(ex.getMessage().contains("Cursor owner mismatch") || ex.getMessage().contains("cannot be transferred across users"));
    }

    @Test
    @DisplayName("Cursor with mismatched filter throws IllegalArgumentException")
    void listDiagnostics_MismatchedFilterCursor_ThrowsIllegalArgument() {
        Timestamp ts = Timestamp.ofTimeSecondsAndNanos(1700000000, 500000000);
        String cursorWithStatus = service.encodeCursor(TEST_UID, "PENDING_ANALYSIS", "NONE", ts, "diag-1");

        // Request with status=ALL but cursor was for PENDING_ANALYSIS
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            service.listFarmerDiagnostics(TEST_UID, "ALL", null, 20, cursorWithStatus);
        });
        assertTrue(ex.getMessage().contains("Cursor filter mismatch"));
    }

    @Test
    @DisplayName("List diagnostics successfully executes query and returns page response")
    void listFarmerDiagnostics_Success() throws Exception {
        Query query = mock(Query.class);
        QuerySnapshot snap = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);

        when(firestore.collection("cropDiagnostics")).thenReturn(collectionRef);
        when(collectionRef.whereEqualTo("farmerUid", TEST_UID)).thenReturn(query);
        when(query.orderBy(anyString(), any(Query.Direction.class))).thenReturn(query);
        when(query.orderBy(any(FieldPath.class), any(Query.Direction.class))).thenReturn(query);
        when(query.limit(anyInt())).thenReturn(query);
        when(query.get()).thenReturn(ApiFutures.immediateFuture(snap));
        when(snap.getDocuments()).thenReturn(List.of(doc));

        when(doc.getId()).thenReturn(DIAGNOSTIC_ID);
        when(doc.getString("farmerUid")).thenReturn(TEST_UID);
        when(doc.getString("cropId")).thenReturn(CROP_ID);
        when(doc.getString("cropName")).thenReturn("Tomato");
        when(doc.getString("symptoms")).thenReturn("Yellow leaves");
        when(doc.getString("affectedPart")).thenReturn("LEAF");
        when(doc.getString("status")).thenReturn("PENDING_ANALYSIS");
        Timestamp ts = Timestamp.ofTimeSecondsAndNanos(1700000000, 0);
        when(doc.getTimestamp("createdAt")).thenReturn(ts);
        when(doc.getTimestamp("updatedAt")).thenReturn(ts);

        DiagnosticPageResponse page = service.listFarmerDiagnostics(TEST_UID, "ALL", null, 10, null);
        assertNotNull(page);
        assertEquals(1, page.getItems().size());
        assertEquals(10, page.getLimit());
        assertFalse(page.isHasNext());
    }

    @Test
    @DisplayName("Cursor secret missing in production profile throws IllegalStateException")
    void cursorSecret_ThrowsIllegalStateException_WhenMissingInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new CropDiagnosticService(
                    firestore, storage, firebaseProperties, cropMasterService, farmService, quotaGuard, null, prodEnv
            );
        });
        assertTrue(ex.getMessage().contains("DIAGNOSTIC_CURSOR_SECRET is missing or blank in production"));
    }

    @Test
    @DisplayName("Cursor secret blank in production profile throws IllegalStateException")
    void cursorSecret_ThrowsIllegalStateException_WhenBlankInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new CropDiagnosticService(
                    firestore, storage, firebaseProperties, cropMasterService, farmService, quotaGuard, "   ", prodEnv
            );
        });
        assertTrue(ex.getMessage().contains("DIAGNOSTIC_CURSOR_SECRET is missing or blank in production"));
    }

    @Test
    @DisplayName("Cursor secret shorter than 32 bytes throws IllegalStateException")
    void cursorSecret_ThrowsIllegalStateException_WhenShorterThan32Bytes() {
        String shortSecret = "too-short-secret-only-24b";

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new CropDiagnosticService(
                    firestore, storage, firebaseProperties, cropMasterService, farmService, quotaGuard, shortSecret, environment
            );
        });
        assertTrue(ex.getMessage().contains("DIAGNOSTIC_CURSOR_SECRET must be at least 32 bytes"));
    }

    @Test
    @DisplayName("Valid production cursor secret of 32 bytes initializes successfully")
    void cursorSecret_InitializesSuccessfully_WhenValidInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");
        String validSecret = "01234567890123456789012345678901"; // 32 bytes

        CropDiagnosticService prodService = new CropDiagnosticService(
                firestore, storage, firebaseProperties, cropMasterService, farmService, quotaGuard, validSecret, prodEnv
        );
        assertNotNull(prodService);
        String cursor = prodService.encodeCursor(TEST_UID, "ALL", "NONE", Timestamp.now(), DIAGNOSTIC_ID);
        assertNotNull(cursor);
        CropDiagnosticService.CursorData data = prodService.decodeAndValidateCursor(cursor, TEST_UID, "ALL", "NONE");
        assertNotNull(data);
        assertEquals(TEST_UID, data.ownerUid);
    }

    @Test
    @DisplayName("Missing cursor secret in non-production profile generates ephemeral key and initializes successfully")
    void cursorSecret_InitializesEphemeralKey_WhenMissingInNonProduction() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        CropDiagnosticService devService = new CropDiagnosticService(
                firestore, storage, firebaseProperties, cropMasterService, farmService, quotaGuard, null, devEnv
        );
        assertNotNull(devService);
        String cursor = devService.encodeCursor(TEST_UID, "ALL", "NONE", Timestamp.now(), DIAGNOSTIC_ID);
        assertNotNull(cursor);
        CropDiagnosticService.CursorData data = devService.decodeAndValidateCursor(cursor, TEST_UID, "ALL", "NONE");
        assertNotNull(data);
        assertEquals(TEST_UID, data.ownerUid);
    }

    // =========================================================================
    // HELPER MOCK CLASSES FOR READ CHANNEL
    // =========================================================================

    private static class MockReadChannel implements ReadChannel {
        private final ByteBuffer buffer;
        private boolean open = true;

        public MockReadChannel(byte[] data) {
            this.buffer = ByteBuffer.wrap(data);
        }

        @Override
        public int read(ByteBuffer dst) {
            if (!open || !buffer.hasRemaining()) {
                return -1;
            }
            int bytesToRead = Math.min(dst.remaining(), buffer.remaining());
            for (int i = 0; i < bytesToRead; i++) {
                dst.put(buffer.get());
            }
            return bytesToRead;
        }

        @Override
        public void close() {
            this.open = false;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override public void seek(long position) {}
        @Override public void setChunkSize(int chunkSize) {}
        @Override public com.google.cloud.RestorableState<ReadChannel> capture() { return null; }
    }

    private static class MockChunkedReadChannel implements ReadChannel {
        private final byte[] data;
        private final int[] chunkSizes;
        private int currentChunkIndex = 0;
        private int totalOffset = 0;
        private boolean open = true;

        public MockChunkedReadChannel(byte[] data, int[] chunkSizes) {
            this.data = data;
            this.chunkSizes = chunkSizes;
        }

        @Override
        public int read(ByteBuffer dst) {
            if (!open || totalOffset >= data.length || currentChunkIndex >= chunkSizes.length) {
                return -1;
            }
            int chunkSize = chunkSizes[currentChunkIndex++];
            int bytesToRead = Math.min(chunkSize, Math.min(dst.remaining(), data.length - totalOffset));
            dst.put(data, totalOffset, bytesToRead);
            totalOffset += bytesToRead;
            return bytesToRead;
        }

        @Override
        public void close() {
            this.open = false;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override public void seek(long position) {}
        @Override public void setChunkSize(int chunkSize) {}
        @Override public com.google.cloud.RestorableState<ReadChannel> capture() { return null; }
    }
}
