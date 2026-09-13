package com.farmlink.api.provider;

import com.farmlink.api.model.PaymentProviderType;
import com.farmlink.api.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class MockPaymentProviderTest {

    private MockPaymentProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockPaymentProvider();
    }

    @Test
    void testGetType() {
        assertEquals(PaymentProviderType.MOCK, provider.getType());
    }

    @Test
    void testCreateIntentSuccess() {
        PaymentIntentProviderRequest request = new PaymentIntentProviderRequest(
                "order_123",
                "buyer_456",
                new BigDecimal("12500.00"),
                "INR"
        );

        PaymentIntentProviderResponse response = provider.createIntent(request);

        assertNotNull(response);
        assertNotNull(response.getProviderPaymentId());
        assertTrue(response.getProviderPaymentId().startsWith("mock_pi_"));
        assertNotNull(response.getProviderOrderId());
        assertEquals(new BigDecimal("12500.00"), response.getAmount());
        assertEquals("INR", response.getCurrency());
        assertEquals(PaymentStatus.PENDING.name(), response.getStatus());
        assertNotNull(response.getExpiresAt());
    }

    @Test
    void testVerifyPaymentSuccess() {
        PaymentIntentProviderRequest req = new PaymentIntentProviderRequest("order_123", "buyer_456", new BigDecimal("5000.00"), "INR");
        PaymentIntentProviderResponse intentResp = provider.createIntent(req);

        PaymentVerificationProviderRequest verifyReq = new PaymentVerificationProviderRequest(
                intentResp.getProviderPaymentId(),
                "mock_sig_123",
                "SUCCESS",
                "UPI",
                null
        );

        PaymentVerificationProviderResponse verifyResp = provider.verifyPayment(verifyReq);

        assertTrue(verifyResp.isVerified());
        assertEquals(PaymentStatus.SUCCESS.name(), verifyResp.getStatus());
        assertNotNull(verifyResp.getTransactionReference());
        assertTrue(verifyResp.getTransactionReference().startsWith("tx_mock_"));
        assertEquals("UPI", verifyResp.getPaymentMethod());
        assertEquals(new BigDecimal("5000.00"), verifyResp.getAmount());
        assertNull(verifyResp.getFailureCode());
    }

    @Test
    void testVerifyPaymentSimulatedFailure() {
        PaymentIntentProviderRequest req = new PaymentIntentProviderRequest("order_123", "buyer_456", new BigDecimal("5000.00"), "INR");
        PaymentIntentProviderResponse intentResp = provider.createIntent(req);

        PaymentVerificationProviderRequest verifyReq = new PaymentVerificationProviderRequest(
                intentResp.getProviderPaymentId(),
                "mock_sig_123",
                "FAILED",
                "MOCK_CARD",
                "Insufficient test balance"
        );

        PaymentVerificationProviderResponse verifyResp = provider.verifyPayment(verifyReq);

        assertFalse(verifyResp.isVerified());
        assertEquals(PaymentStatus.FAILED.name(), verifyResp.getStatus());
        assertEquals("SIMULATED_MOCK_FAILURE", verifyResp.getFailureCode());
        assertEquals("Insufficient test balance", verifyResp.getFailureMessage());
    }
}
