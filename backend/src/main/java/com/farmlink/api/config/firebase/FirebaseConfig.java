package com.farmlink.api.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    private final FirebaseProperties properties;

    public FirebaseConfig(FirebaseProperties properties) {
        this.properties = properties;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            logger.info("FirebaseApp already initialized.");
            return FirebaseApp.getInstance();
        }

        String projectId = (properties.getProjectId() != null && !properties.getProjectId().trim().isEmpty())
                ? properties.getProjectId()
                : "farmconnectprices-dev";

        // Handle emulator mode
        if (properties.isUseEmulator() || System.getenv("FIRESTORE_EMULATOR_HOST") != null) {
            String emulatorHost = properties.getEmulatorHost();
            if (System.getenv("FIRESTORE_EMULATOR_HOST") != null) {
                emulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
            }
            logger.info("Initializing FirebaseApp in EMULATOR mode for projectId: {} at host: {}", projectId, emulatorHost);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setProjectId(projectId)
                    .setCredentials(GoogleCredentials.create(null))
                    .build();
            return FirebaseApp.initializeApp(options);
        }

        // Standard / Production Credentials mode
        GoogleCredentials credentials = null;
        String privateKey = properties.getFormattedPrivateKey();
        String clientEmail = properties.getClientEmail();

        if (privateKey != null && !privateKey.trim().isEmpty() && clientEmail != null && !clientEmail.trim().isEmpty()) {
            logger.info("Initializing FirebaseApp with service account credentials for projectId: {}", projectId);
            String jsonCredentials = String.format(
                    "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"%s\",\n" +
                    "  \"client_email\": \"%s\",\n" +
                    "  \"private_key\": \"%s\"\n" +
                    "}",
                    projectId,
                    clientEmail,
                    privateKey.replace("\n", "\\n")
            );
            credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(jsonCredentials.getBytes(StandardCharsets.UTF_8)));
        } else {
            logger.info("Attempting Application Default Credentials for projectId: {}", projectId);
            try {
                credentials = GoogleCredentials.getApplicationDefault();
            } catch (Exception e) {
                logger.warn("Application Default Credentials unavailable. Initializing with development/test credentials.");
                credentials = GoogleCredentials.create(null);
            }
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
