package com.farmlink.api.config.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.firebase")
public class FirebaseProperties {

    private String projectId;
    private String clientEmail;
    private String privateKey;
    private boolean useEmulator = false;
    private String emulatorHost = "localhost:8081";

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public boolean isUseEmulator() {
        return useEmulator;
    }

    public void setUseEmulator(boolean useEmulator) {
        this.useEmulator = useEmulator;
    }

    public String getEmulatorHost() {
        return emulatorHost;
    }

    public void setEmulatorHost(String emulatorHost) {
        this.emulatorHost = emulatorHost;
    }

    /**
     * Helper to return private key with actual newlines resolved from escaped strings (\n).
     */
    public String getFormattedPrivateKey() {
        if (privateKey == null) {
            return null;
        }
        return privateKey.replace("\\n", "\n");
    }

    /**
     * Checks if credentials (projectId and privateKey or clientEmail) are configured.
     */
    public boolean isConfigured() {
        return (projectId != null && !projectId.trim().isEmpty()) &&
                ((privateKey != null && !privateKey.trim().isEmpty()) ||
                 (clientEmail != null && !clientEmail.trim().isEmpty()) ||
                 useEmulator);
    }
}
