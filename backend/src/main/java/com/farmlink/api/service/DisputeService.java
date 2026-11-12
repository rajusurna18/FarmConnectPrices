package com.farmlink.api.service;

import com.farmlink.api.common.event.MarketplaceDomainEvent;
import com.farmlink.api.dto.*;
import com.farmlink.api.model.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing the lifecycle, audit trail, and counterparty communications
 * for marketplace transaction disputes in Module 24.
 */
@Service
public class DisputeService {

    private static final Logger logger = LoggerFactory.getLogger(DisputeService.class);

    public static final String COLLECTION_NAME = "disputes";
    public static final String SUB_RESPONSES = "responses";
    public static final String SUB_EVIDENCE = "evidence";
    public static final String SUB_HISTORY = "history";
    public static final String COLLECTION_LOCKS = "disputeLocks";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Firestore firestore;
    private final OrderService orderService;
    private final FirestoreQuotaGuard quotaGuard;
    private final ApplicationEventPublisher eventPublisher;
    private final SecretKey cursorSigningKey;

    // In-memory fallbacks for testing environments where Firestore is unavailable
    private final Map<String, Dispute> inMemoryDisputes = new ConcurrentHashMap<>();
    private final Map<String, List<DisputeResponseRecord>> inMemoryResponses = new ConcurrentHashMap<>();
    private final Map<String, List<DisputeEvidence>> inMemoryEvidence = new ConcurrentHashMap<>();
    private final Map<String, List<DisputeHistoryEntry>> inMemoryHistory = new ConcurrentHashMap<>();
    private final Map<String, String> inMemoryLocks = new ConcurrentHashMap<>();

    public static final int MINIMUM_SECRET_BYTE_LENGTH = 32; // 256 bits for HMAC-SHA256

    @Autowired
    public DisputeService(Firestore firestore,
                          OrderService orderService,
                          FirestoreQuotaGuard quotaGuard,
                          ApplicationEventPublisher eventPublisher,
                          @Value("${app.disputes.cursor-signing-secret:${DISPUTE_CURSOR_SECRET:}}") String cursorSigningSecret,
                          @Autowired(required = false) Environment environment) {
        this.firestore = firestore;
        this.orderService = orderService;
        this.quotaGuard = quotaGuard;
        this.eventPublisher = eventPublisher;
        this.cursorSigningKey = initializeCursorSigningKey(cursorSigningSecret, environment);
    }

    public DisputeService(Firestore firestore,
                          OrderService orderService,
                          FirestoreQuotaGuard quotaGuard,
                          ApplicationEventPublisher eventPublisher,
                          String cursorSigningSecret) {
        this(firestore, orderService, quotaGuard, eventPublisher, cursorSigningSecret, null);
    }

    public DisputeService(Firestore firestore,
                          OrderService orderService,
                          FirestoreQuotaGuard quotaGuard,
                          ApplicationEventPublisher eventPublisher) {
        this(firestore, orderService, quotaGuard, eventPublisher, null, null);
    }

    private SecretKey initializeCursorSigningKey(String secret, Environment env) {
        boolean isProd = isProductionEnvironment(env);

        if (secret != null && !secret.trim().isEmpty()) {
            byte[] secretBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < MINIMUM_SECRET_BYTE_LENGTH) {
                String errorMsg = String.format(
                        "CRITICAL CONFIGURATION ERROR: Configured dispute cursor secret is insufficiently strong. " +
                        "Minimum required length is %d bytes (256 bits) for HMAC-SHA256, but found %d bytes.",
                        MINIMUM_SECRET_BYTE_LENGTH, secretBytes.length
                );
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            if (isProd) {
                logger.info("Dispute cursor HMAC-SHA256 signing key successfully initialized from production environment configuration.");
            }
            return new SecretKeySpec(secretBytes, "HmacSHA256");
        }

        // Secret is missing or blank
        if (isProd) {
            String errorMsg = "CRITICAL CONFIGURATION ERROR: DISPUTE_CURSOR_SECRET is missing or blank in production environment. " +
                    "A secure signing secret of at least 32 bytes (256 bits) must be configured via environment variable DISPUTE_CURSOR_SECRET. " +
                    "Ephemeral signing keys are strictly forbidden in production. Application startup aborted.";
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Controlled non-production fallback (local development / test harness)
        logger.warn("DISPUTE_CURSOR_SECRET is not configured. Running in non-production mode; generating an ephemeral in-memory 256-bit signing key for this runtime instance.");
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
            String appEnv = env.getProperty("app.environment");
            if ("production".equalsIgnoreCase(appEnv) || "prod".equalsIgnoreCase(appEnv)) {
                return true;
            }
            String payEnv = env.getProperty("payment.environment");
            if ("production".equalsIgnoreCase(payEnv) || "prod".equalsIgnoreCase(payEnv)) {
                return true;
            }
        }
        String envVar = System.getenv("SPRING_PROFILES_ACTIVE");
        if (envVar != null && (envVar.toLowerCase().contains("prod") || envVar.equalsIgnoreCase("production"))) {
            return true;
        }
        String genericEnv = System.getenv("ENVIRONMENT");
        if (genericEnv != null && (genericEnv.equalsIgnoreCase("production") || genericEnv.equalsIgnoreCase("prod"))) {
            return true;
        }
        return false;
    }

    /**
     * Create a new dispute from an eligible order.
     */
    public synchronized DisputeResponse createDispute(String callerUid, CreateDisputeRequest request) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Dispute request cannot be null.");
        }
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required.");
        }
        if (request.getIssueCategory() == null || !DisputeCategory.isValid(request.getIssueCategory())) {
            throw new IllegalArgumentException("Invalid issue category: " + request.getIssueCategory());
        }
        if (request.getSubject() == null || request.getSubject().trim().length() < 3 || request.getSubject().trim().length() > 200) {
            throw new IllegalArgumentException("Subject must be between 3 and 200 characters.");
        }
        if (request.getDescription() == null || request.getDescription().trim().length() < 10 || request.getDescription().trim().length() > 2000) {
            throw new IllegalArgumentException("Description must be between 10 and 2000 characters.");
        }

        String orderId = request.getOrderId().trim();
        DisputeCategory category = DisputeCategory.fromString(request.getIssueCategory());

        // 1. Authoritative Order verification via OrderService
        OrderResponse order = orderService.getOrderById(orderId, callerUid);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }

        // 2. Validate Order eligibility: Must not be PENDING
        if (order.getStatus() != null && "PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Disputes cannot be reported for PENDING orders. Order must be confirmed or in progress.");
        }

        // 3. Derive participant UIDs and roles strictly from Order
        String farmerUid = order.getFarmerId();
        String buyerUid = order.getBuyerUid();
        String buyerRole = order.getBuyerRole() != null ? order.getBuyerRole() : "CUSTOMER";

        String createdByRole;
        String counterpartyUid;
        String counterpartyRole;

        if (callerUid.equals(farmerUid)) {
            createdByRole = "FARMER";
            counterpartyUid = buyerUid;
            counterpartyRole = buyerRole;
        } else if (callerUid.equals(buyerUid)) {
            createdByRole = buyerRole;
            counterpartyUid = farmerUid;
            counterpartyRole = "FARMER";
        } else {
            throw new AccessDeniedException("Unauthorized: user is not a participant in order " + orderId);
        }

        String lockKey = orderId + "_" + category.name();
        String disputeId = "disp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String historyId = "hist_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String now = Instant.now().toString();

        Dispute dispute = new Dispute(
                disputeId,
                orderId,
                order.getListingId(),
                order.getOfferId(),
                order.getCropId(),
                order.getCropName(),
                callerUid,
                createdByRole,
                farmerUid,
                buyerUid,
                buyerRole,
                category.name(),
                request.getSubject().trim(),
                request.getDescription().trim(),
                DisputeStatus.OPEN.name(),
                null, null, null, null, null,
                now, now, now
        );

        DisputeHistoryEntry historyEntry = new DisputeHistoryEntry(
                historyId,
                disputeId,
                callerUid,
                createdByRole,
                DisputeHistoryAction.DISPUTE_CREATED.name(),
                null,
                DisputeStatus.OPEN.name(),
                "Dispute opened by " + createdByRole + " for " + category.name(),
                now
        );

        // 4. Concurrency-safe atomic duplicate prevention via Firestore transaction
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference lockRef = firestore.collection(COLLECTION_LOCKS).document(lockKey);
                DocumentReference disputeRef = firestore.collection(COLLECTION_NAME).document(disputeId);
                DocumentReference histRef = disputeRef.collection(SUB_HISTORY).document(historyId);

                firestore.runTransaction(transaction -> {
                    DocumentSnapshot lockSnap = transaction.get(lockRef).get();
                    if (lockSnap.exists()) {
                        String activeDisputeId = lockSnap.getString("activeDisputeId");
                        if (activeDisputeId != null && !activeDisputeId.trim().isEmpty()) {
                            DocumentSnapshot existingDisputeSnap = transaction.get(firestore.collection(COLLECTION_NAME).document(activeDisputeId)).get();
                            if (existingDisputeSnap.exists()) {
                                String existingStatus = existingDisputeSnap.getString("status");
                                DisputeStatus ds = DisputeStatus.fromString(existingStatus);
                                if (ds != null && ds.isActive()) {
                                    throw new IllegalStateException("An active dispute (" + activeDisputeId + ") already exists for order " + orderId + " and category " + category.name());
                                }
                            }
                        }
                    }

                    Map<String, Object> lockData = new HashMap<>();
                    lockData.put("lockKey", lockKey);
                    lockData.put("orderId", orderId);
                    lockData.put("issueCategory", category.name());
                    lockData.put("activeDisputeId", disputeId);
                    lockData.put("status", DisputeStatus.OPEN.name());
                    lockData.put("updatedAt", now);

                    transaction.set(lockRef, lockData);
                    transaction.set(disputeRef, dispute);
                    transaction.set(histRef, historyEntry);
                    return null;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof IllegalStateException ise) {
                    throw ise;
                }
                if (e instanceof IllegalStateException ise) {
                    throw ise;
                }
                logger.error("Error creating dispute in Firestore for order {}: {}", orderId, e.getMessage(), e);
                throw new RuntimeException("Failed to persist dispute: " + e.getMessage(), e);
            }
        } else {
            // In-memory fallback
            String existingActiveId = inMemoryLocks.get(lockKey);
            if (existingActiveId != null) {
                Dispute existing = inMemoryDisputes.get(existingActiveId);
                if (existing != null && DisputeStatus.fromString(existing.getStatus()).isActive()) {
                    throw new IllegalStateException("An active dispute (" + existingActiveId + ") already exists for order " + orderId + " and category " + category.name());
                }
            }
            inMemoryLocks.put(lockKey, disputeId);
            inMemoryDisputes.put(disputeId, dispute);
            inMemoryHistory.computeIfAbsent(disputeId, k -> new ArrayList<>()).add(historyEntry);
        }

        logger.info("dispute.created disputeId: {}, orderId: {}, category: {}, callerUid: {}",
                disputeId, orderId, category.name(), callerUid);

        // 5. Decoupled real notification event dispatching (isolated from transaction)
        publishDisputeNotification(
                counterpartyUid,
                counterpartyRole,
                NotificationType.DISPUTE_CREATED,
                "Issue Reported on Order #" + orderId,
                "An issue has been reported for order #" + orderId + ". Please review dispute details.",
                disputeId,
                orderId
        );

        return new DisputeResponse(dispute);
    }

    /**
     * Retrieve single dispute details including responses, evidence, and audit history.
     */
    public DisputeDetailResponse getDisputeById(String callerUid, String disputeId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (disputeId == null || disputeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Dispute ID cannot be null or empty.");
        }

        Dispute dispute = fetchDispute(disputeId);
        if (dispute == null) {
            throw new NoSuchElementException("Dispute not found with ID: " + disputeId);
        }

        if (!callerUid.equals(dispute.getFarmerUid()) && !callerUid.equals(dispute.getBuyerUid())) {
            throw new AccessDeniedException("Unauthorized access to dispute " + disputeId);
        }

        List<DisputeResponseRecord> responses = fetchResponses(disputeId);
        List<DisputeEvidence> evidence = fetchEvidence(disputeId);
        List<DisputeHistoryEntry> history = fetchHistory(disputeId);

        return new DisputeDetailResponse(new DisputeResponse(dispute), responses, evidence, history);
    }

    /**
     * Get disputes created by the authenticated user with page number (backward-compatible overload).
     */
    public DisputePageResponse getMyDisputes(String callerUid, String statusFilter, int page, int size) {
        return getMyDisputes(callerUid, statusFilter, page, size, null);
    }

    /**
     * Get disputes created by the authenticated user with real Firestore cursor-based pagination.
     */
    public DisputePageResponse getMyDisputes(String callerUid, String statusFilter, int page, int size, String cursor) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        String normalizedStatus = (statusFilter != null && !statusFilter.trim().isEmpty()) ? statusFilter.trim().toUpperCase() : "ALL";

        CursorData cursorData = decodeAndValidateCursor(cursor, "MINE", callerUid, normalizedStatus);

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("createdByUid", callerUid);

                if (!"ALL".equalsIgnoreCase(normalizedStatus)) {
                    query = query.whereEqualTo("status", normalizedStatus);
                }

                // Deterministic ordering: createdAt DESC with documentId tie-breaker
                query = query.orderBy("createdAt", Query.Direction.DESCENDING)
                             .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING);

                if (cursorData != null) {
                    DocumentSnapshot cursorDoc = firestore.collection(COLLECTION_NAME).document(cursorData.disputeId).get().get();
                    if (cursorDoc.exists()) {
                        query = query.startAfter(cursorDoc);
                    } else {
                        query = query.startAfter(cursorData.createdAt, cursorData.disputeId);
                    }
                }

                // Fetch only safeSize + 1 records (genuine bounded cursor fetch, NEVER 1000)
                query = query.limit(safeSize + 1);

                QuerySnapshot snapshot = query.get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                List<QueryDocumentSnapshot> docs = snapshot.getDocuments();
                boolean hasNext = docs.size() > safeSize;
                int resultCount = Math.min(docs.size(), safeSize);

                List<DisputeResponse> items = new ArrayList<>();
                for (int i = 0; i < resultCount; i++) {
                    Dispute d = mapDocToDispute(docs.get(i));
                    if (d != null) items.add(new DisputeResponse(d));
                }

                String nextCursor = (hasNext && !items.isEmpty())
                        ? encodeCursor("MINE", callerUid, normalizedStatus, items.get(items.size() - 1).getCreatedAt(), items.get(items.size() - 1).getDisputeId())
                        : null;

                return new DisputePageResponse(items, safePage, safeSize, -1, -1, hasNext, nextCursor);
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e instanceof IllegalArgumentException || e instanceof AccessDeniedException) {
                    throw (RuntimeException) e;
                }
                logger.error("Error querying my disputes with cursor from Firestore: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to query disputes: " + e.getMessage(), e);
            }
        } else {
            // In-memory fallback
            List<Dispute> matching = new ArrayList<>();
            for (Dispute d : inMemoryDisputes.values()) {
                if (callerUid.equals(d.getCreatedByUid())) {
                    if ("ALL".equalsIgnoreCase(normalizedStatus) || normalizedStatus.equalsIgnoreCase(d.getStatus())) {
                        matching.add(d);
                    }
                }
            }

            Comparator<Dispute> deterministicOrder = Comparator
                    .comparing(Dispute::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Dispute::getDisputeId, Comparator.nullsLast(Comparator.reverseOrder()));
            matching.sort(deterministicOrder);

            return paginateInMemoryWithCursor(matching, "MINE", callerUid, normalizedStatus, safePage, safeSize, cursorData);
        }
    }

    /**
     * Get disputes concerning orders where the authenticated user is the counterparty (backward-compatible overload).
     */
    public DisputePageResponse getRelatedDisputes(String callerUid, String statusFilter, int page, int size) {
        return getRelatedDisputes(callerUid, statusFilter, page, size, null);
    }

    /**
     * Get disputes concerning orders where the authenticated user is the counterparty with real Firestore cursor-based pagination.
     */
    public DisputePageResponse getRelatedDisputes(String callerUid, String statusFilter, int page, int size, String cursor) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        String normalizedStatus = (statusFilter != null && !statusFilter.trim().isEmpty()) ? statusFilter.trim().toUpperCase() : "ALL";

        CursorData cursorData = decodeAndValidateCursor(cursor, "RELATED", callerUid, normalizedStatus);

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("counterpartyUid", callerUid);

                if (!"ALL".equalsIgnoreCase(normalizedStatus)) {
                    query = query.whereEqualTo("status", normalizedStatus);
                }

                // Deterministic ordering: createdAt DESC with documentId tie-breaker
                query = query.orderBy("createdAt", Query.Direction.DESCENDING)
                             .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING);

                if (cursorData != null) {
                    DocumentSnapshot cursorDoc = firestore.collection(COLLECTION_NAME).document(cursorData.disputeId).get().get();
                    if (cursorDoc.exists()) {
                        query = query.startAfter(cursorDoc);
                    } else {
                        query = query.startAfter(cursorData.createdAt, cursorData.disputeId);
                    }
                }

                // Fetch only safeSize + 1 records (genuine bounded cursor fetch, NEVER 1000)
                query = query.limit(safeSize + 1);

                QuerySnapshot snapshot = query.get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                List<QueryDocumentSnapshot> docs = snapshot.getDocuments();
                boolean hasNext = docs.size() > safeSize;
                int resultCount = Math.min(docs.size(), safeSize);

                List<DisputeResponse> items = new ArrayList<>();
                for (int i = 0; i < resultCount; i++) {
                    Dispute d = mapDocToDispute(docs.get(i));
                    if (d != null) items.add(new DisputeResponse(d));
                }

                String nextCursor = (hasNext && !items.isEmpty())
                        ? encodeCursor("RELATED", callerUid, normalizedStatus, items.get(items.size() - 1).getCreatedAt(), items.get(items.size() - 1).getDisputeId())
                        : null;

                return new DisputePageResponse(items, safePage, safeSize, -1, -1, hasNext, nextCursor);
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e instanceof IllegalArgumentException || e instanceof AccessDeniedException) {
                    throw (RuntimeException) e;
                }
                logger.error("Error querying related disputes with cursor from Firestore: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to query related disputes: " + e.getMessage(), e);
            }
        } else {
            // In-memory fallback
            List<Dispute> matching = new ArrayList<>();
            for (Dispute d : inMemoryDisputes.values()) {
                if (!callerUid.equals(d.getCreatedByUid()) && (callerUid.equals(d.getCounterpartyUid()) || callerUid.equals(d.getFarmerUid()) || callerUid.equals(d.getBuyerUid()))) {
                    if ("ALL".equalsIgnoreCase(normalizedStatus) || normalizedStatus.equalsIgnoreCase(d.getStatus())) {
                        matching.add(d);
                    }
                }
            }

            Comparator<Dispute> deterministicOrder = Comparator
                    .comparing(Dispute::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Dispute::getDisputeId, Comparator.nullsLast(Comparator.reverseOrder()));
            matching.sort(deterministicOrder);

            return paginateInMemoryWithCursor(matching, "RELATED", callerUid, normalizedStatus, safePage, safeSize, cursorData);
        }
    }

    /**
     * Get disputes associated with a specific order (caller must be an order participant).
     */
    public List<DisputeResponse> getDisputesByOrderId(String callerUid, String orderId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty.");
        }

        // Validate participant authorization through authoritative order
        OrderResponse order = orderService.getOrderById(orderId, callerUid);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }

        List<DisputeResponse> result = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                QuerySnapshot snapshot = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("orderId", orderId)
                        .limit(20)
                        .get().get();

                if (quotaGuard != null) quotaGuard.recordSuccess();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Dispute d = mapDocToDispute(doc);
                    if (d != null) {
                        result.add(new DisputeResponse(d));
                    }
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.error("Error querying disputes for order {}: {}", orderId, e.getMessage(), e);
            }
        } else {
            for (Dispute d : inMemoryDisputes.values()) {
                if (orderId.equals(d.getOrderId())) {
                    result.add(new DisputeResponse(d));
                }
            }
        }

        result.sort(Comparator.comparing(DisputeResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    /**
     * Add a participant response to an active dispute.
     */
    public synchronized DisputeResponseRecord addResponse(String callerUid, String disputeId, AddDisputeResponseRequest request) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (disputeId == null || disputeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Dispute ID cannot be null or empty.");
        }
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Response message cannot be blank.");
        }
        if (request.getMessage().trim().length() > 2000) {
            throw new IllegalArgumentException("Response message cannot exceed 2000 characters.");
        }

        Dispute dispute = fetchDispute(disputeId);
        if (dispute == null) {
            throw new NoSuchElementException("Dispute not found with ID: " + disputeId);
        }

        // Verify participant authorization
        if (!callerUid.equals(dispute.getFarmerUid()) && !callerUid.equals(dispute.getBuyerUid())) {
            throw new AccessDeniedException("Unauthorized to respond to dispute " + disputeId);
        }

        // Reject responses for terminal / non-active states
        DisputeStatus currentStatus = DisputeStatus.fromString(dispute.getStatus());
        if (currentStatus == null || !currentStatus.isActive()) {
            throw new IllegalStateException("Cannot respond to dispute in status: " + dispute.getStatus());
        }

        String authorRole = callerUid.equals(dispute.getFarmerUid()) ? "FARMER" : dispute.getBuyerRole();
        String counterpartyUid = callerUid.equals(dispute.getFarmerUid()) ? dispute.getBuyerUid() : dispute.getFarmerUid();
        String counterpartyRole = callerUid.equals(dispute.getFarmerUid()) ? dispute.getBuyerRole() : "FARMER";

        String responseId = "resp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String historyId = "hist_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String now = Instant.now().toString();

        DisputeResponseRecord record = new DisputeResponseRecord(
                responseId,
                disputeId,
                callerUid,
                authorRole,
                request.getMessage().trim(),
                now
        );

        // Status transition: If WAITING_FOR_INFORMATION, transition to UNDER_REVIEW server-side
        boolean statusTransitioned = currentStatus == DisputeStatus.WAITING_FOR_INFORMATION;
        String newStatus = statusTransitioned ? DisputeStatus.UNDER_REVIEW.name() : dispute.getStatus();
        String actionType = statusTransitioned ? DisputeHistoryAction.INFORMATION_SUBMITTED.name() : DisputeHistoryAction.RESPONSE_ADDED.name();

        DisputeHistoryEntry historyEntry = new DisputeHistoryEntry(
                historyId,
                disputeId,
                callerUid,
                authorRole,
                actionType,
                dispute.getStatus(),
                newStatus,
                "Response submitted by " + authorRole + (statusTransitioned ? " (Information provided)" : ""),
                now
        );

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference disputeRef = firestore.collection(COLLECTION_NAME).document(disputeId);
                DocumentReference respRef = disputeRef.collection(SUB_RESPONSES).document(responseId);
                DocumentReference histRef = disputeRef.collection(SUB_HISTORY).document(historyId);

                firestore.runTransaction(transaction -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("updatedAt", now);
                    updates.put("lastActivityAt", now);
                    if (statusTransitioned) {
                        updates.put("status", newStatus);
                    }
                    transaction.update(disputeRef, updates);
                    transaction.set(respRef, record);
                    transaction.set(histRef, historyEntry);
                    return null;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.error("Error persisting response for dispute {}: {}", disputeId, e.getMessage(), e);
                throw new RuntimeException("Failed to persist response: " + e.getMessage(), e);
            }
        } else {
            if (statusTransitioned) {
                dispute.setStatus(newStatus);
            }
            dispute.setUpdatedAt(now);
            dispute.setLastActivityAt(now);
            inMemoryResponses.computeIfAbsent(disputeId, k -> new ArrayList<>()).add(record);
            inMemoryHistory.computeIfAbsent(disputeId, k -> new ArrayList<>()).add(historyEntry);
        }

        logger.info("dispute.response_added disputeId: {}, authorUid: {}, role: {}", disputeId, callerUid, authorRole);

        // Notify counterparty (never author)
        publishDisputeNotification(
                counterpartyUid,
                counterpartyRole,
                NotificationType.DISPUTE_RESPONSE_RECEIVED,
                "New Response on Dispute #" + disputeId,
                "A new response was added to dispute #" + disputeId + ".",
                disputeId,
                dispute.getOrderId()
        );

        return record;
    }

    /**
     * Add supporting evidence metadata to an active dispute.
     */
    public synchronized DisputeEvidence addEvidence(String callerUid, String disputeId, AddDisputeEvidenceRequest request) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (disputeId == null || disputeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Dispute ID cannot be null or empty.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Evidence request cannot be null.");
        }
        if (request.getFileName() == null || request.getFileName().trim().isEmpty() || request.getFileName().length() > 255) {
            throw new IllegalArgumentException("File name must be between 1 and 255 characters.");
        }
        if (request.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(request.getContentType().trim().toLowerCase())) {
            throw new IllegalArgumentException("Invalid or unsupported content type. Allowed: " + ALLOWED_CONTENT_TYPES);
        }
        if (request.getFileSize() == null || request.getFileSize() <= 0 || request.getFileSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be positive and not exceed 10 MB.");
        }

        Dispute dispute = fetchDispute(disputeId);
        if (dispute == null) {
            throw new NoSuchElementException("Dispute not found with ID: " + disputeId);
        }

        if (!callerUid.equals(dispute.getFarmerUid()) && !callerUid.equals(dispute.getBuyerUid())) {
            throw new AccessDeniedException("Unauthorized to attach evidence to dispute " + disputeId);
        }

        DisputeStatus status = DisputeStatus.fromString(dispute.getStatus());
        if (status == null || !status.isActive()) {
            throw new IllegalStateException("Cannot attach evidence: dispute is in status " + dispute.getStatus());
        }

        String registrantRole = callerUid.equals(dispute.getFarmerUid()) ? "FARMER" : dispute.getBuyerRole();
        String evidenceId = "evi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String historyId = "hist_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String now = Instant.now().toString();

        // Evidence is registered strictly as metadata. No client storagePath or URL accepted.
        DisputeEvidence evidence = new DisputeEvidence(
                evidenceId,
                disputeId,
                callerUid,
                registrantRole,
                request.getFileName().trim(),
                null,
                request.getContentType().trim().toLowerCase(),
                request.getFileSize(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                now
        );

        DisputeHistoryEntry historyEntry = new DisputeHistoryEntry(
                historyId,
                disputeId,
                callerUid,
                registrantRole,
                DisputeHistoryAction.EVIDENCE_ADDED.name(),
                dispute.getStatus(),
                dispute.getStatus(),
                "Evidence metadata registered: " + request.getFileName().trim(),
                now
        );

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference disputeRef = firestore.collection(COLLECTION_NAME).document(disputeId);
                DocumentReference eviRef = disputeRef.collection(SUB_EVIDENCE).document(evidenceId);
                DocumentReference histRef = disputeRef.collection(SUB_HISTORY).document(historyId);

                firestore.runTransaction(transaction -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("updatedAt", now);
                    updates.put("lastActivityAt", now);
                    transaction.update(disputeRef, updates);
                    transaction.set(eviRef, evidence);
                    transaction.set(histRef, historyEntry);
                    return null;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.error("Error adding evidence metadata to Firestore: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to attach evidence: " + e.getMessage(), e);
            }
        } else {
            dispute.setUpdatedAt(now);
            dispute.setLastActivityAt(now);
            inMemoryEvidence.computeIfAbsent(disputeId, k -> new ArrayList<>()).add(evidence);
            inMemoryHistory.computeIfAbsent(disputeId, k -> new ArrayList<>()).add(historyEntry);
        }

        logger.info("dispute.evidence_registered disputeId: {}, evidenceId: {}, registeredByUid: {}", disputeId, evidenceId, callerUid);
        return evidence;
    }

    /**
     * Creator-only voluntary closure of an open dispute.
     */
    public synchronized DisputeResponse closeDispute(String callerUid, String disputeId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (disputeId == null || disputeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Dispute ID cannot be null or empty.");
        }

        Dispute dispute = fetchDispute(disputeId);
        if (dispute == null) {
            throw new NoSuchElementException("Dispute not found with ID: " + disputeId);
        }

        // Creator-only check
        if (!callerUid.equals(dispute.getCreatedByUid())) {
            throw new AccessDeniedException("Only the original dispute creator may close this dispute.");
        }

        DisputeStatus status = DisputeStatus.fromString(dispute.getStatus());
        if (status == DisputeStatus.CLOSED) {
            throw new IllegalStateException("Dispute is already closed.");
        }
        if (status == DisputeStatus.RESOLVED || status == DisputeStatus.REJECTED) {
            throw new IllegalStateException("Cannot close an adjudicated dispute with status " + dispute.getStatus() + ".");
        }
        if (!status.canTransitionTo(DisputeStatus.CLOSED)) {
            throw new IllegalStateException("Invalid status transition from " + dispute.getStatus() + " to CLOSED.");
        }

        String now = Instant.now().toString();
        String historyId = "hist_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String lockKey = dispute.getOrderId() + "_" + dispute.getIssueCategory();

        DisputeHistoryEntry historyEntry = new DisputeHistoryEntry(
                historyId,
                disputeId,
                callerUid,
                dispute.getCreatedByRole(),
                DisputeHistoryAction.DISPUTE_CLOSED.name(),
                dispute.getStatus(),
                DisputeStatus.CLOSED.name(),
                "Dispute closed voluntarily by creator.",
                now
        );

        String counterpartyUid = callerUid.equals(dispute.getFarmerUid()) ? dispute.getBuyerUid() : dispute.getFarmerUid();
        String counterpartyRole = callerUid.equals(dispute.getFarmerUid()) ? dispute.getBuyerRole() : "FARMER";

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference disputeRef = firestore.collection(COLLECTION_NAME).document(disputeId);
                DocumentReference histRef = disputeRef.collection(SUB_HISTORY).document(historyId);
                DocumentReference lockRef = firestore.collection(COLLECTION_LOCKS).document(lockKey);

                firestore.runTransaction(transaction -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", DisputeStatus.CLOSED.name());
                    updates.put("closedAt", now);
                    updates.put("updatedAt", now);
                    updates.put("lastActivityAt", now);

                    transaction.update(disputeRef, updates);
                    transaction.set(histRef, historyEntry);

                    // Clear active dispute pointer from lock
                    Map<String, Object> lockUpdates = new HashMap<>();
                    lockUpdates.put("activeDisputeId", null);
                    lockUpdates.put("status", DisputeStatus.CLOSED.name());
                    lockUpdates.put("updatedAt", now);
                    transaction.set(lockRef, lockUpdates, SetOptions.merge());

                    return null;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.error("Error closing dispute in Firestore: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to close dispute: " + e.getMessage(), e);
            }
        } else {
            dispute.setStatus(DisputeStatus.CLOSED.name());
            dispute.setClosedAt(now);
            dispute.setUpdatedAt(now);
            dispute.setLastActivityAt(now);
            inMemoryHistory.computeIfAbsent(disputeId, k -> new ArrayList<>()).add(historyEntry);
            inMemoryLocks.remove(lockKey);
        }

        logger.info("dispute.closed disputeId: {}, closedByUid: {}", disputeId, callerUid);

        // Notify counterparty
        publishDisputeNotification(
                counterpartyUid,
                counterpartyRole,
                NotificationType.DISPUTE_CLOSED,
                "Dispute #" + disputeId + " Closed",
                "Dispute #" + disputeId + " for order #" + dispute.getOrderId() + " has been closed.",
                disputeId,
                dispute.getOrderId()
        );

        dispute.setStatus(DisputeStatus.CLOSED.name());
        dispute.setClosedAt(now);
        dispute.setUpdatedAt(now);
        dispute.setLastActivityAt(now);
        return new DisputeResponse(dispute);
    }

    // --- Private Helper Methods ---

    private Dispute fetchDispute(String disputeId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot doc = firestore.collection(COLLECTION_NAME).document(disputeId).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                if (doc.exists()) {
                    return mapDocToDispute(doc);
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.error("Error fetching dispute {}: {}", disputeId, e.getMessage());
            }
        }
        return inMemoryDisputes.get(disputeId);
    }

    private List<DisputeResponseRecord> fetchResponses(String disputeId) {
        List<DisputeResponseRecord> list = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot snapshot = firestore.collection(COLLECTION_NAME).document(disputeId)
                        .collection(SUB_RESPONSES)
                        .limit(100)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    list.add(new DisputeResponseRecord(
                            doc.getString("responseId"),
                            doc.getString("disputeId"),
                            doc.getString("authorUid"),
                            doc.getString("authorRole"),
                            doc.getString("message"),
                            doc.getString("createdAt")
                    ));
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Could not fetch responses for dispute {}: {}", disputeId, e.getMessage());
            }
        } else {
            List<DisputeResponseRecord> mem = inMemoryResponses.get(disputeId);
            if (mem != null) list.addAll(mem);
        }
        list.sort(Comparator.comparing(DisputeResponseRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    private List<DisputeEvidence> fetchEvidence(String disputeId) {
        List<DisputeEvidence> list = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot snapshot = firestore.collection(COLLECTION_NAME).document(disputeId)
                        .collection(SUB_EVIDENCE)
                        .limit(50)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    String regUid = doc.getString("registeredByUid") != null
                            ? doc.getString("registeredByUid")
                            : doc.getString("uploadedByUid");
                    String regRole = doc.getString("registeredByRole") != null
                            ? doc.getString("registeredByRole")
                            : doc.getString("uploadedByRole");

                    list.add(new DisputeEvidence(
                            doc.getString("evidenceId"),
                            doc.getString("disputeId"),
                            regUid,
                            regRole,
                            doc.getString("fileName"),
                            doc.getString("storagePath"),
                            doc.getString("contentType"),
                            doc.getLong("fileSize"),
                            doc.getString("description"),
                            doc.getString("createdAt")
                    ));
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Could not fetch evidence for dispute {}: {}", disputeId, e.getMessage());
            }
        } else {
            List<DisputeEvidence> mem = inMemoryEvidence.get(disputeId);
            if (mem != null) list.addAll(mem);
        }
        list.sort(Comparator.comparing(DisputeEvidence::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    private List<DisputeHistoryEntry> fetchHistory(String disputeId) {
        List<DisputeHistoryEntry> list = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot snapshot = firestore.collection(COLLECTION_NAME).document(disputeId)
                        .collection(SUB_HISTORY)
                        .limit(100)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    list.add(new DisputeHistoryEntry(
                            doc.getString("historyId"),
                            doc.getString("disputeId"),
                            doc.getString("actorUid"),
                            doc.getString("actorRole"),
                            doc.getString("actionType"),
                            doc.getString("previousStatus"),
                            doc.getString("newStatus"),
                            doc.getString("description"),
                            doc.getString("createdAt")
                    ));
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Could not fetch history for dispute {}: {}", disputeId, e.getMessage());
            }
        } else {
            List<DisputeHistoryEntry> mem = inMemoryHistory.get(disputeId);
            if (mem != null) list.addAll(mem);
        }
        list.sort(Comparator.comparing(DisputeHistoryEntry::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    private Dispute mapDocToDispute(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Dispute d = new Dispute(
                doc.getString("disputeId"),
                doc.getString("orderId"),
                doc.getString("listingId"),
                doc.getString("offerId"),
                doc.getString("cropId"),
                doc.getString("cropName"),
                doc.getString("createdByUid"),
                doc.getString("createdByRole"),
                doc.getString("farmerUid"),
                doc.getString("buyerUid"),
                doc.getString("buyerRole"),
                doc.getString("issueCategory"),
                doc.getString("subject"),
                doc.getString("description"),
                doc.getString("status"),
                doc.getString("resolutionSummary"),
                doc.getString("resolutionOutcome"),
                doc.getString("resolvedBy"),
                doc.getString("resolvedAt"),
                doc.getString("closedAt"),
                doc.getString("createdAt"),
                doc.getString("updatedAt"),
                doc.getString("lastActivityAt")
        );
        if (doc.contains("counterpartyUid") && doc.getString("counterpartyUid") != null) {
            d.setCounterpartyUid(doc.getString("counterpartyUid"));
            d.setCounterpartyRole(doc.getString("counterpartyRole"));
        }
        return d;
    }

    public static class CursorData {
        public final String queryType;
        public final String ownerUid;
        public final String statusFilter;
        public final String createdAt;
        public final String disputeId;

        public CursorData(String queryType, String ownerUid, String statusFilter, String createdAt, String disputeId) {
            this.queryType = queryType;
            this.ownerUid = ownerUid;
            this.statusFilter = statusFilter;
            this.createdAt = createdAt;
            this.disputeId = disputeId;
        }
    }

    private byte[] computeHmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(this.cursorSigningKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 signature for cursor", e);
        }
    }

    private String encodeCursor(String queryType, String ownerUid, String statusFilter, String createdAt, String disputeId) {
        if (createdAt == null || disputeId == null) return null;
        String sFilter = (statusFilter != null && !statusFilter.trim().isEmpty()) ? statusFilter.trim().toUpperCase() : "ALL";
        String raw = queryType + "|" + ownerUid + "|" + sFilter + "|" + createdAt + "|" + disputeId;
        byte[] payloadBytes = raw.getBytes(StandardCharsets.UTF_8);
        byte[] signatureBytes = computeHmac(payloadBytes);

        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);
        String sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        return payloadB64 + "." + sigB64;
    }

    private CursorData decodeAndValidateCursor(String cursor, String expectedQueryType, String callerUid, String expectedStatusFilter) {
        if (cursor == null || cursor.trim().isEmpty()) return null;

        String trimmed = cursor.trim();
        int dotIndex = trimmed.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex >= trimmed.length() - 1) {
            throw new IllegalArgumentException("Malformed or invalid pagination cursor: missing cryptographic signature.");
        }

        String payloadB64 = trimmed.substring(0, dotIndex);
        String sigB64 = trimmed.substring(dotIndex + 1);

        byte[] payloadBytes;
        byte[] receivedSig;
        try {
            payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
            receivedSig = Base64.getUrlDecoder().decode(sigB64);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed or invalid pagination cursor encoding.");
        }

        // Cryptographic signature verification using constant-time equality check
        byte[] expectedSig = computeHmac(payloadBytes);
        if (!MessageDigest.isEqual(expectedSig, receivedSig)) {
            throw new IllegalArgumentException("Invalid, tampered, or forged pagination cursor signature.");
        }

        // Only after cryptographic verification passes, parse and inspect payload contents
        String raw = new String(payloadBytes, StandardCharsets.UTF_8);
        String[] parts = raw.split("\\|", 5);
        if (parts.length != 5 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty() || parts[3].isEmpty() || parts[4].isEmpty()) {
            throw new IllegalArgumentException("Malformed or invalid pagination cursor structure.");
        }

        String queryType = parts[0];
        String ownerUid = parts[1];
        String statusFilter = parts[2];
        String createdAt = parts[3];
        String disputeId = parts[4];

        if (!expectedQueryType.equalsIgnoreCase(queryType)) {
            throw new IllegalArgumentException("Cursor query scope mismatch: cursor was not issued for " + expectedQueryType + " queries.");
        }
        if (!callerUid.equals(ownerUid)) {
            throw new AccessDeniedException("Cursor owner mismatch: cursor cannot be used across different authenticated users.");
        }
        String normalizedExpectedStatus = (expectedStatusFilter != null && !expectedStatusFilter.trim().isEmpty()) ? expectedStatusFilter.trim().toUpperCase() : "ALL";
        if (!normalizedExpectedStatus.equalsIgnoreCase(statusFilter)) {
            throw new IllegalArgumentException("Cursor filter mismatch: cursor was issued for status filter " + statusFilter + ", but query requested " + normalizedExpectedStatus + ".");
        }

        return new CursorData(queryType, ownerUid, statusFilter, createdAt, disputeId);
    }

    private DisputePageResponse paginateInMemoryWithCursor(List<Dispute> sortedList,
                                                           String queryType,
                                                           String ownerUid,
                                                           String statusFilter,
                                                           int page,
                                                           int size,
                                                           CursorData cursorData) {
        int totalElements = sortedList.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int startIndex = 0;
        if (cursorData != null) {
            int anchorIndex = -1;
            for (int i = 0; i < sortedList.size(); i++) {
                Dispute d = sortedList.get(i);
                if (d.getCreatedAt().equals(cursorData.createdAt) && d.getDisputeId().equals(cursorData.disputeId)) {
                    anchorIndex = i;
                    break;
                }
            }
            if (anchorIndex >= 0) {
                startIndex = anchorIndex + 1;
            } else {
                throw new IllegalArgumentException("Invalid pagination cursor: anchor record not found in query scope.");
            }
        } else if (page > 0) {
            startIndex = page * size;
        }

        List<DisputeResponse> items = new ArrayList<>();
        if (startIndex < totalElements) {
            int toIndex = Math.min(startIndex + size, totalElements);
            for (Dispute d : sortedList.subList(startIndex, toIndex)) {
                items.add(new DisputeResponse(d));
            }
        }

        boolean hasNext = (startIndex + items.size()) < totalElements;
        String nextCursor = (hasNext && !items.isEmpty())
                ? encodeCursor(queryType, ownerUid, statusFilter, items.get(items.size() - 1).getCreatedAt(), items.get(items.size() - 1).getDisputeId())
                : null;

        return new DisputePageResponse(items, page, size, totalElements, totalPages, hasNext, nextCursor);
    }

    private void publishDisputeNotification(String recipientUid,
                                            String recipientRole,
                                            NotificationType type,
                                            String title,
                                            String message,
                                            String disputeId,
                                            String orderId) {
        if (eventPublisher == null || recipientUid == null || recipientUid.trim().isEmpty()) {
            return;
        }
        try {
            MarketplaceDomainEvent event = new MarketplaceDomainEvent(
                    this,
                    recipientUid.trim(),
                    recipientRole != null ? recipientRole : "",
                    type,
                    title,
                    message,
                    "DISPUTE",
                    disputeId,
                    "/marketplace/disputes/" + disputeId,
                    Map.of("orderId", orderId != null ? orderId : ""),
                    "event_dispute_" + type.name().toLowerCase() + "_" + disputeId + "_" + System.currentTimeMillis()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            logger.warn("dispute.notification_failed for disputeId: {}, type: {}, error: {}",
                    disputeId, type, e.getMessage());
            // Notification failure does NOT interrupt the dispute workflow
        }
    }
}

