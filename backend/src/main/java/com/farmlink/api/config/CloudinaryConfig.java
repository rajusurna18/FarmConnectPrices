package com.farmlink.api.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Cloudinary image storage configuration.
 * Connects to the Cloudinary API using credentials provided via environment or properties.
 * Never logs or exposes the API secret.
 */
@Configuration
public class CloudinaryConfig {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryConfig.class);

    @Value("${cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:}}")
    private String cloudName;

    @Value("${cloudinary.api-key:${CLOUDINARY_API_KEY:}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:${CLOUDINARY_API_SECRET:}}")
    private String apiSecret;

    public String getCloudName() {
        return cloudName;
    }

    public String getApiKey() {
        return apiKey;
    }

    /**
     * API Secret getter - internal usage only.
     * Never log or display this value.
     */
    public String getApiSecret() {
        return apiSecret;
    }

    /**
     * Checks if all required Cloudinary credentials are present and non-blank.
     */
    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }

    /**
     * Creates a reusable Cloudinary client bean if credentials are valid.
     * If credentials are missing, logs a safe non-sensitive warning and returns null
     * so that application startup is never blocked.
     */
    @Bean
    public Cloudinary cloudinary() {
        if (!isConfigured()) {
            logger.warn("Cloudinary credentials are not fully configured. Cloudinary client will not be initialized.");
            return null;
        }

        logger.info("Initializing Cloudinary client for cloud-name: {}", cloudName);
        Map<?, ?> config = ObjectUtils.asMap(
                "cloud_name", cloudName.trim(),
                "api_key", apiKey.trim(),
                "api_secret", apiSecret.trim(),
                "secure", true
        );
        return new Cloudinary(config);
    }
}
