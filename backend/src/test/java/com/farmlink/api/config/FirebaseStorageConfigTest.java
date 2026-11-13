package com.farmlink.api.config;

import com.farmlink.api.config.firebase.FirebaseConfig;
import com.farmlink.api.config.firebase.FirebaseProperties;
import com.google.cloud.storage.Storage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

public class FirebaseStorageConfigTest {

    @Test
    @DisplayName("FirebaseProperties should correctly store and retrieve storageBucket")
    void firebaseProperties_StorageBucketProperty() {
        FirebaseProperties properties = new FirebaseProperties();
        assertNull(properties.getStorageBucket());

        properties.setStorageBucket("farmconnectprices.appspot.com");
        assertEquals("farmconnectprices.appspot.com", properties.getStorageBucket());
    }

    @Test
    @DisplayName("Storage client in emulator mode connects using NoCredentials and default host localhost:9199")
    void googleCloudStorage_EmulatorMode_InitializesWithEmulatorHost() {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setProjectId("farmconnectprices");
        properties.setUseEmulator(true);
        properties.setStorageBucket("farmconnectprices.appspot.com");

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        FirebaseConfig config = new FirebaseConfig(properties, env);
        FirebaseApp mockApp = Mockito.mock(FirebaseApp.class);

        Storage storage = config.googleCloudStorage(mockApp);
        assertNotNull(storage, "Storage bean must be initialized in emulator mode");
        assertEquals("farmconnectprices", storage.getOptions().getProjectId());
        assertEquals("http://localhost:9199", storage.getOptions().getHost());
    }

    @Test
    @DisplayName("Storage client throws IllegalStateException if emulator is enabled in production profile")
    void googleCloudStorage_FailsClosed_WhenEmulatorInProduction() {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setProjectId("farmconnectprices");
        properties.setUseEmulator(true);
        properties.setStorageBucket("farmconnectprices.appspot.com");

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        FirebaseConfig config = new FirebaseConfig(properties, env);
        FirebaseApp mockApp = Mockito.mock(FirebaseApp.class);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            config.googleCloudStorage(mockApp);
        });

        assertTrue(ex.getMessage().contains("Storage emulator is configured in production profile") ||
                ex.getMessage().contains("CRITICAL SECURITY ERROR"));
    }

    @Test
    @DisplayName("Storage client throws IllegalStateException in production profile if storage-bucket is missing")
    void googleCloudStorage_RequiresStorageBucketInProduction() {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setProjectId("farmconnectprices");
        properties.setUseEmulator(false);
        properties.setStorageBucket(null);

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        FirebaseConfig config = new FirebaseConfig(properties, env);
        FirebaseApp mockApp = Mockito.mock(FirebaseApp.class);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            config.googleCloudStorage(mockApp);
        });

        assertTrue(ex.getMessage().contains("app.firebase.storage-bucket") ||
                ex.getMessage().contains("CRITICAL CONFIGURATION ERROR"));
    }

    @Test
    @DisplayName("Storage client throws IllegalStateException in production profile if storage-bucket is blank")
    void googleCloudStorage_RequiresNonBlankStorageBucketInProduction() {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setProjectId("farmconnectprices");
        properties.setUseEmulator(false);
        properties.setStorageBucket("   ");

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        FirebaseConfig config = new FirebaseConfig(properties, env);
        FirebaseApp mockApp = Mockito.mock(FirebaseApp.class);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            config.googleCloudStorage(mockApp);
        });

        assertTrue(ex.getMessage().contains("app.firebase.storage-bucket") ||
                ex.getMessage().contains("CRITICAL CONFIGURATION ERROR"));
    }

    @Test
    @DisplayName("Explicit storageBucket selection stores and retrieves correctly without fallback")
    void firebaseProperties_ExplicitBucketSelection() {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setStorageBucket("custom-crops-bucket-prod.appspot.com");
        assertEquals("custom-crops-bucket-prod.appspot.com", properties.getStorageBucket());
    }

    @Test
    @DisplayName("Storage client in production initializes successfully when valid bucket is provided")
    void googleCloudStorage_InitializesSuccessfully_WhenValidBucketInProduction() {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setProjectId("farmconnectprices");
        properties.setUseEmulator(false);
        properties.setStorageBucket("valid-production-bucket.appspot.com");

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        FirebaseConfig config = new FirebaseConfig(properties, env);
        FirebaseApp mockApp = Mockito.mock(FirebaseApp.class);

        Storage storage = config.googleCloudStorage(mockApp);
        assertNotNull(storage);
        assertEquals("farmconnectprices", storage.getOptions().getProjectId());
    }
}
