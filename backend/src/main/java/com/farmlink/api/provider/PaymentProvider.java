package com.farmlink.api.provider;

import com.farmlink.api.model.PaymentProviderType;

/**
 * Interface isolating payment gateway provider operations from domain/service logic.
 */
public interface PaymentProvider {

    PaymentProviderType getType();

    PaymentIntentProviderResponse createIntent(PaymentIntentProviderRequest request);

    PaymentVerificationProviderResponse verifyPayment(PaymentVerificationProviderRequest request);

    PaymentStatusProviderResponse getStatus(String providerPaymentId);
}
