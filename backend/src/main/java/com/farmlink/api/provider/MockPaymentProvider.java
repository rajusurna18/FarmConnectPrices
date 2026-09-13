package com.farmlink.api.provider;

import com.farmlink.api.model.PaymentProviderType;
import com.farmlink.api.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe Development / Mock Payment Provider.
 * Strictly used in development and test environments for Module 18.
 * Never connects to live financial rails or production credentials.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockPaymentProvider.class);

    private final Map<String, PaymentIntentProviderResponse> mockIntents = new ConcurrentHashMap<>();

    @Override
    public PaymentProviderType getType() {
        return PaymentProviderType.MOCK;
    }

    @Override
    public PaymentIntentProviderResponse createIntent(PaymentIntentProviderRequest request) {
        logger.info("[MOCK PROVIDER] Creating mock payment intent for order ID: {}, buyer: {}, amount: {} {}",
                request.getOrderId(), request.getBuyerUid(), request.getAmount(), request.getCurrency());

        String providerPaymentId = "mock_pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String providerOrderId = "mock_order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // 15-minute mock expiration TTL
        String expiresAt = Instant.now().plusSeconds(15 * 60).toString();

        PaymentIntentProviderResponse response = new PaymentIntentProviderResponse(
                providerPaymentId,
                providerOrderId,
                request.getAmount(),
                request.getCurrency(),
                PaymentStatus.PENDING.name(),
                expiresAt
        );

        mockIntents.put(providerPaymentId, response);
        return response;
    }

    @Override
    public PaymentVerificationProviderResponse verifyPayment(PaymentVerificationProviderRequest request) {
        logger.info("[MOCK PROVIDER] Verifying mock payment: providerPaymentId={}, simulatedStatus={}",
                request.getProviderPaymentId(), request.getSimulatedStatus());

        PaymentIntentProviderResponse intent = mockIntents.get(request.getProviderPaymentId());

        boolean isSimulatedFailure = "FAILED".equalsIgnoreCase(request.getSimulatedStatus());

        String status = isSimulatedFailure ? PaymentStatus.FAILED.name() : PaymentStatus.SUCCESS.name();
        String txRef = "tx_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String method = request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()
                ? request.getPaymentMethod().trim()
                : "MOCK_CARD";

        String failureCode = isSimulatedFailure ? "SIMULATED_MOCK_FAILURE" : null;
        String failureMessage = isSimulatedFailure
                ? (request.getFailureReason() != null ? request.getFailureReason() : "Mock payment processing failed")
                : null;

        return new PaymentVerificationProviderResponse(
                !isSimulatedFailure,
                status,
                request.getProviderPaymentId(),
                txRef,
                method,
                intent != null ? intent.getAmount() : null,
                intent != null ? intent.getCurrency() : "INR",
                failureCode,
                failureMessage
        );
    }

    @Override
    public PaymentStatusProviderResponse getStatus(String providerPaymentId) {
        PaymentIntentProviderResponse intent = mockIntents.get(providerPaymentId);
        if (intent == null) {
            return new PaymentStatusProviderResponse(providerPaymentId, PaymentStatus.FAILED.name(), null, "INR");
        }
        return new PaymentStatusProviderResponse(providerPaymentId, intent.getStatus(), intent.getAmount(), intent.getCurrency());
    }
}
