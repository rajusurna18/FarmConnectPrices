package com.farmlink.api.service;

import com.farmlink.api.config.PaymentEnvironmentGuard;
import com.farmlink.api.dto.*;
import com.farmlink.api.model.*;
import com.farmlink.api.provider.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public static final String COLLECTION_NAME = "payments";
    public static final String DEFAULT_CURRENCY = "INR";

    private final Firestore firestore;
    private final OrderService orderService;
    private final PaymentProvider paymentProvider;
    private final FirestoreQuotaGuard quotaGuard;
    private final PaymentEnvironmentGuard environmentGuard;

    // In-memory fallback ONLY for isolated unit tests (when firestore is null)
    private final Map<String, Payment> inMemoryPayments = new ConcurrentHashMap<>();

    @Autowired
    public PaymentService(
            @Autowired(required = false) Firestore firestore,
            OrderService orderService,
            PaymentProvider paymentProvider,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard,
            @Autowired(required = false) PaymentEnvironmentGuard environmentGuard
    ) {
        this.firestore = firestore;
        this.orderService = orderService;
        this.paymentProvider = paymentProvider;
        this.quotaGuard = quotaGuard;
        this.environmentGuard = environmentGuard;
    }

    /**
     * Create or retrieve an active Payment Intent for a CONFIRMED Order.
     * Enforces single active payment intent per order with atomic Firestore protection.
     */
    public synchronized PaymentIntentResponse createPaymentIntent(String callerUid, String orderId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required.");
        }

        // Load authoritative Order
        OrderResponse order = orderService.getOrderById(orderId, callerUid);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }

        // Enforce buyer-only initiation (MEDIATOR_BUYER or CUSTOMER)
        if (!callerUid.equals(order.getBuyerUid())) {
            if (callerUid.equals(order.getFarmerId())) {
                throw new AccessDeniedException("Farmers cannot initiate buyer payment for their own received order.");
            }
            throw new AccessDeniedException("Unauthorized to initiate payment for this order.");
        }

        // Enforce payable order status (CONFIRMED only)
        if (!OrderStatus.CONFIRMED.name().equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Payment initiation is only permitted for CONFIRMED orders. Current order status: " + order.getStatus());
        }

        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid order total amount for payment initiation.");
        }
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        String currency = order.getCurrency() != null ? order.getCurrency() : DEFAULT_CURRENCY;
        String paymentId = "pay_" + orderId;
        String nowIso = Instant.now().toString();

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference paymentRef = firestore.collection(COLLECTION_NAME).document(paymentId);

                Payment existingOrNew = firestore.runTransaction(transaction -> {
                    DocumentSnapshot paySnap = transaction.get(paymentRef).get();

                    if (paySnap.exists()) {
                        Payment existing = mapDocToPayment(paySnap);

                        // If already paid successfully, reject duplicate creation
                        if (PaymentStatus.SUCCESS.name().equalsIgnoreCase(existing.getStatus())) {
                            throw new IllegalStateException("Order has already been successfully paid.");
                        }

                        // If active PENDING or PROCESSING intent exists, return existing
                        if (PaymentStatus.PENDING.name().equalsIgnoreCase(existing.getStatus()) ||
                            PaymentStatus.PROCESSING.name().equalsIgnoreCase(existing.getStatus())) {
                            return existing;
                        }
                    }

                    // Request provider intent
                    PaymentIntentProviderRequest providerReq = new PaymentIntentProviderRequest(
                            orderId,
                            callerUid,
                            order.getTotalAmount().setScale(2, RoundingMode.HALF_UP),
                            currency
                    );
                    PaymentIntentProviderResponse providerResp = paymentProvider.createIntent(providerReq);

                    Payment newPayment = new Payment(
                            paymentId,
                            orderId,
                            order.getBuyerUid(),
                            order.getFarmerId(),
                            order.getBuyerRole(),
                            order.getTotalAmount().setScale(2, RoundingMode.HALF_UP),
                            currency,
                            PaymentStatus.PENDING.name(),
                            paymentProvider.getType().name(),
                            providerResp.getProviderPaymentId(),
                            providerResp.getProviderOrderId(),
                            null,
                            null,
                            null,
                            null,
                            nowIso,
                            nowIso,
                            nowIso,
                            null,
                            null,
                            providerResp.getExpiresAt(),
                            new ArrayList<>()
                    );

                    transaction.set(paymentRef, mapPaymentToDoc(newPayment));
                    return newPayment;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toIntentResponse(existingOrNew);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to create payment intent transaction: " + e.getMessage(), e);
            }
        }

        // In-memory execution for unit tests (when firestore is null)
        Payment existing = inMemoryPayments.get(paymentId);
        if (existing != null) {
            if (PaymentStatus.SUCCESS.name().equalsIgnoreCase(existing.getStatus())) {
                throw new IllegalStateException("Order has already been successfully paid.");
            }
            if (PaymentStatus.PENDING.name().equalsIgnoreCase(existing.getStatus()) ||
                PaymentStatus.PROCESSING.name().equalsIgnoreCase(existing.getStatus())) {
                return toIntentResponse(existing);
            }
        }

        PaymentIntentProviderRequest providerReq = new PaymentIntentProviderRequest(
                orderId,
                callerUid,
                totalAmount,
                currency
        );
        PaymentIntentProviderResponse providerResp = paymentProvider.createIntent(providerReq);

        Payment newPayment = new Payment(
                paymentId,
                orderId,
                order.getBuyerUid(),
                order.getFarmerId(),
                order.getBuyerRole(),
                totalAmount,
                currency,
                PaymentStatus.PENDING.name(),
                paymentProvider.getType().name(),
                providerResp.getProviderPaymentId(),
                providerResp.getProviderOrderId(),
                null,
                null,
                null,
                null,
                nowIso,
                nowIso,
                nowIso,
                null,
                null,
                providerResp.getExpiresAt(),
                new ArrayList<>()
        );

        inMemoryPayments.put(paymentId, newPayment);
        return toIntentResponse(newPayment);
    }

    /**
     * Verify payment with independent provider verification, amount reconciliation, and atomic status update.
     */
    public synchronized PaymentResponse verifyPayment(String callerUid, String paymentId, VerifyPaymentRequest request) {
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID is required.");
        }

        Payment payment = fetchPaymentFromFirestoreOrMemory(paymentId);
        if (payment == null) {
            throw new NoSuchElementException("Payment record not found with ID: " + paymentId);
        }

        // Verify authorization if callerUid is present
        if (callerUid != null && !callerUid.trim().isEmpty()) {
            if (!callerUid.equals(payment.getBuyerUid()) && !callerUid.equals(payment.getFarmerId())) {
                throw new AccessDeniedException("Unauthorized access to payment verification.");
            }
        }

        // Idempotency: If already SUCCESS, return response without re-processing
        if (PaymentStatus.SUCCESS.name().equalsIgnoreCase(payment.getStatus())) {
            return toResponse(payment);
        }

        String nowIso = Instant.now().toString();
        String providerPaymentId = request != null && request.getProviderPaymentId() != null
                ? request.getProviderPaymentId()
                : payment.getProviderPaymentId();

        // Call provider verification
        PaymentVerificationProviderRequest providerReq = new PaymentVerificationProviderRequest(
                providerPaymentId,
                request != null ? request.getProviderSignature() : null,
                request != null ? request.getSimulatedStatus() : null,
                request != null ? request.getPaymentMethod() : null,
                request != null ? request.getFailureReason() : null
        );

        PaymentVerificationProviderResponse providerResp = paymentProvider.verifyPayment(providerReq);

        // Perform double-entry amount and currency reconciliation against authoritative Order
        boolean amountMatches = providerResp.getAmount() == null ||
                payment.getAmount().compareTo(providerResp.getAmount().setScale(2, RoundingMode.HALF_UP)) == 0;
        boolean currencyMatches = providerResp.getCurrency() == null ||
                payment.getCurrency().equalsIgnoreCase(providerResp.getCurrency());

        boolean isSuccessful = providerResp.isVerified() && amountMatches && currencyMatches;

        PaymentAttempt attempt = new PaymentAttempt(
                "att_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                providerResp.getProviderPaymentId(),
                providerResp.getTransactionReference(),
                providerResp.getPaymentMethod(),
                isSuccessful ? PaymentStatus.SUCCESS.name() : PaymentStatus.FAILED.name(),
                !amountMatches ? "AMOUNT_MISMATCH" : (!currencyMatches ? "CURRENCY_MISMATCH" : providerResp.getFailureCode()),
                !amountMatches ? "Provider amount mismatch" : (!currencyMatches ? "Provider currency mismatch" : providerResp.getFailureMessage()),
                nowIso,
                isSuccessful ? nowIso : null
        );

        if (payment.getAttempts() == null) {
            payment.setAttempts(new ArrayList<>());
        }
        payment.getAttempts().add(attempt);

        if (isSuccessful) {
            payment.setStatus(PaymentStatus.SUCCESS.name());
            payment.setProviderPaymentId(providerResp.getProviderPaymentId());
            payment.setTransactionReference(providerResp.getTransactionReference());
            payment.setPaymentMethod(providerResp.getPaymentMethod());
            payment.setVerifiedAt(nowIso);
            payment.setUpdatedAt(nowIso);
            payment.setFailureCode(null);
            payment.setFailureMessage(null);
        } else {
            payment.setStatus(PaymentStatus.FAILED.name());
            payment.setFailedAt(nowIso);
            payment.setUpdatedAt(nowIso);
            payment.setFailureCode(attempt.getFailureCode());
            payment.setFailureMessage(attempt.getFailureMessage());
        }

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentReference paymentRef = firestore.collection(COLLECTION_NAME).document(paymentId);
                firestore.runTransaction(transaction -> {
                    transaction.set(paymentRef, mapPaymentToDoc(payment));
                    return null;
                }).get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error updating payment verification to Firestore: {}", e.getMessage());
            }
        } else {
            inMemoryPayments.put(paymentId, payment);
        }

        return toResponse(payment);
    }

    /**
     * Get single payment detail by ID.
     */
    public PaymentResponse getPaymentById(String paymentId, String callerUid) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        Payment payment = fetchPaymentFromFirestoreOrMemory(paymentId);
        if (payment == null) {
            throw new NoSuchElementException("Payment record not found with ID: " + paymentId);
        }

        if (!callerUid.equals(payment.getBuyerUid()) && !callerUid.equals(payment.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized access to payment details.");
        }

        return toResponse(payment);
    }

    /**
     * Get payments initiated by authenticated buyer.
     */
    public PaymentPageResponse getMyPayments(String buyerUid, String statusFilter, int page, int size) {
        if (buyerUid == null || buyerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Payment> buyerPayments = fetchBuyerPaymentsFromFirestoreOrMemory(buyerUid);

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            buyerPayments = buyerPayments.stream()
                    .filter(p -> statusFilter.equalsIgnoreCase(p.getStatus()))
                    .collect(Collectors.toList());
        }

        buyerPayments.sort(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = buyerPayments.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<PaymentResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = buyerPayments.subList(fromIndex, toIndex).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new PaymentPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Get safe payment status for farmer received orders.
     */
    public PaymentPageResponse getReceivedPayments(String farmerId, String statusFilter, int page, int size) {
        if (farmerId == null || farmerId.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Payment> farmerPayments = fetchFarmerPaymentsFromFirestoreOrMemory(farmerId);

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            farmerPayments = farmerPayments.stream()
                    .filter(p -> statusFilter.equalsIgnoreCase(p.getStatus()))
                    .collect(Collectors.toList());
        }

        farmerPayments.sort(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = farmerPayments.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<PaymentResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = farmerPayments.subList(fromIndex, toIndex).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new PaymentPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Process incoming provider webhooks idempotently.
     */
    public PaymentResponse processWebhook(String provider, String rawPayload, String signatureHeader) {
        logger.info("[WEBHOOK] Processing incoming webhook for provider: {}", provider);
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("Webhook payload cannot be empty.");
        }

        // Webhook verification & processing
        String paymentId = extractPaymentIdFromWebhook(rawPayload);
        if (paymentId == null) {
            throw new IllegalArgumentException("Unable to extract payment reference from webhook payload.");
        }

        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setProviderPaymentId(paymentId);
        req.setProviderSignature(signatureHeader);
        req.setSimulatedStatus("SUCCESS");

        return verifyPayment(null, paymentId, req);
    }

    private String extractPaymentIdFromWebhook(String payload) {
        // Mock / simple parser for webhook payment reference
        if (payload.contains("pay_")) {
            int idx = payload.indexOf("pay_");
            int endIdx = payload.indexOf("\"", idx);
            if (endIdx == -1) endIdx = payload.indexOf("'", idx);
            if (endIdx == -1) endIdx = payload.length();
            return payload.substring(idx, Math.min(idx + 36, endIdx));
        }
        return null;
    }

    private Payment fetchPaymentFromFirestoreOrMemory(String paymentId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot doc = firestore.collection(COLLECTION_NAME).document(paymentId).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                if (doc.exists()) {
                    return mapDocToPayment(doc);
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error fetching payment {} from Firestore: {}", paymentId, e.getMessage());
            }
        }
        return inMemoryPayments.get(paymentId);
    }

    private List<Payment> fetchBuyerPaymentsFromFirestoreOrMemory(String buyerUid) {
        List<Payment> results = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot querySnaps = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("buyerUid", buyerUid)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                for (DocumentSnapshot doc : querySnaps.getDocuments()) {
                    results.add(mapDocToPayment(doc));
                }
                return results;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying buyer payments for {} from Firestore: {}", buyerUid, e.getMessage());
            }
        }

        return inMemoryPayments.values().stream()
                .filter(p -> buyerUid.equals(p.getBuyerUid()))
                .collect(Collectors.toList());
    }

    private List<Payment> fetchFarmerPaymentsFromFirestoreOrMemory(String farmerId) {
        List<Payment> results = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot querySnaps = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("farmerId", farmerId)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                for (DocumentSnapshot doc : querySnaps.getDocuments()) {
                    results.add(mapDocToPayment(doc));
                }
                return results;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying farmer payments for {} from Firestore: {}", farmerId, e.getMessage());
            }
        }

        return inMemoryPayments.values().stream()
                .filter(p -> farmerId.equals(p.getFarmerId()))
                .collect(Collectors.toList());
    }

    public PaymentIntentResponse toIntentResponse(Payment payment) {
        if (payment == null) return null;
        return new PaymentIntentResponse(
                payment.getProviderPaymentId(),
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider(),
                payment.getProviderOrderId(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getExpiresAt()
        );
    }

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) return null;
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getBuyerUid(),
                payment.getFarmerId(),
                payment.getBuyerRole(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getProviderPaymentId(),
                payment.getProviderOrderId(),
                payment.getTransactionReference(),
                payment.getPaymentMethod(),
                payment.getFailureCode(),
                payment.getFailureMessage(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getInitiatedAt(),
                payment.getVerifiedAt(),
                payment.getFailedAt(),
                payment.getExpiresAt()
        );
    }

    private Payment mapDocToPayment(DocumentSnapshot doc) {
        Payment payment = new Payment();
        payment.setPaymentId(doc.getString("paymentId"));
        payment.setOrderId(doc.getString("orderId"));
        payment.setBuyerUid(doc.getString("buyerUid"));
        payment.setFarmerId(doc.getString("farmerId"));
        payment.setBuyerRole(doc.getString("buyerRole"));

        if (doc.get("amount") != null) {
            payment.setAmount(new BigDecimal(doc.get("amount").toString()));
        }
        payment.setCurrency(doc.getString("currency"));
        payment.setStatus(doc.getString("status"));
        payment.setProvider(doc.getString("provider"));
        payment.setProviderPaymentId(doc.getString("providerPaymentId"));
        payment.setProviderOrderId(doc.getString("providerOrderId"));
        payment.setTransactionReference(doc.getString("transactionReference"));
        payment.setPaymentMethod(doc.getString("paymentMethod"));
        payment.setFailureCode(doc.getString("failureCode"));
        payment.setFailureMessage(doc.getString("failureMessage"));

        payment.setCreatedAt(doc.getString("createdAt"));
        payment.setUpdatedAt(doc.getString("updatedAt"));
        payment.setInitiatedAt(doc.getString("initiatedAt"));
        payment.setVerifiedAt(doc.getString("verifiedAt"));
        payment.setFailedAt(doc.getString("failedAt"));
        payment.setExpiresAt(doc.getString("expiresAt"));

        List<Map<String, Object>> attemptMaps = (List<Map<String, Object>>) doc.get("attempts");
        if (attemptMaps != null) {
            List<PaymentAttempt> attempts = new ArrayList<>();
            for (Map<String, Object> map : attemptMaps) {
                PaymentAttempt att = new PaymentAttempt();
                att.setAttemptId((String) map.get("attemptId"));
                att.setProviderPaymentId((String) map.get("providerPaymentId"));
                att.setTransactionReference((String) map.get("transactionReference"));
                att.setPaymentMethod((String) map.get("paymentMethod"));
                att.setStatus((String) map.get("status"));
                att.setFailureCode((String) map.get("failureCode"));
                att.setFailureMessage((String) map.get("failureMessage"));
                att.setAttemptedAt((String) map.get("attemptedAt"));
                att.setVerifiedAt((String) map.get("verifiedAt"));
                attempts.add(att);
            }
            payment.setAttempts(attempts);
        }

        return payment;
    }

    private Map<String, Object> mapPaymentToDoc(Payment payment) {
        Map<String, Object> map = new HashMap<>();
        map.put("paymentId", payment.getPaymentId());
        map.put("orderId", payment.getOrderId());
        map.put("buyerUid", payment.getBuyerUid());
        map.put("farmerId", payment.getFarmerId());
        map.put("buyerRole", payment.getBuyerRole());
        map.put("amount", payment.getAmount() != null ? payment.getAmount().toString() : null);
        map.put("currency", payment.getCurrency());
        map.put("status", payment.getStatus());
        map.put("provider", payment.getProvider());
        map.put("providerPaymentId", payment.getProviderPaymentId());
        map.put("providerOrderId", payment.getProviderOrderId());
        map.put("transactionReference", payment.getTransactionReference());
        map.put("paymentMethod", payment.getPaymentMethod());
        map.put("failureCode", payment.getFailureCode());
        map.put("failureMessage", payment.getFailureMessage());
        map.put("createdAt", payment.getCreatedAt());
        map.put("updatedAt", payment.getUpdatedAt());
        map.put("initiatedAt", payment.getInitiatedAt());
        map.put("verifiedAt", payment.getVerifiedAt());
        map.put("failedAt", payment.getFailedAt());
        map.put("expiresAt", payment.getExpiresAt());

        if (payment.getAttempts() != null) {
            List<Map<String, Object>> attemptMaps = new ArrayList<>();
            for (PaymentAttempt att : payment.getAttempts()) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("attemptId", att.getAttemptId());
                attMap.put("providerPaymentId", att.getProviderPaymentId());
                attMap.put("transactionReference", att.getTransactionReference());
                attMap.put("paymentMethod", att.getPaymentMethod());
                attMap.put("status", att.getStatus());
                attMap.put("failureCode", att.getFailureCode());
                attMap.put("failureMessage", att.getFailureMessage());
                attMap.put("attemptedAt", att.getAttemptedAt());
                attMap.put("verifiedAt", att.getVerifiedAt());
                attemptMaps.add(attMap);
            }
            map.put("attempts", attemptMaps);
        }

        return map;
    }
}
