package com.farmlink.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Production Safety Guard for Module 18 Payments.
 * Prevents accidental execution of MOCK payment provider in production environment.
 */
@Component
public class PaymentEnvironmentGuard {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEnvironmentGuard.class);

    private final String provider;
    private final String environment;

    public PaymentEnvironmentGuard(
            @Value("${payment.provider:MOCK}") String provider,
            @Value("${payment.environment:development}") String environment
    ) {
        this.provider = provider;
        this.environment = environment;
    }

    @PostConstruct
    public void validateEnvironment() {
        logger.info("Initializing Payment Environment Guard. Provider: {}, Environment: {}", provider, environment);

        if ("production".equalsIgnoreCase(environment) && "MOCK".equalsIgnoreCase(provider)) {
            String errorMsg = "CRITICAL SECURITY ERROR: Production environment ('production') MUST NOT use MOCK payment provider! Deployment aborted.";
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }

    public String getProvider() { return provider; }
    public String getEnvironment() { return environment; }
}
