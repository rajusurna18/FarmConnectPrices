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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
                : "farmconnectprices";

        // 1. Emulator Mode
        if (properties.isUseEmulator() || System.getenv("FIRESTORE_EMULATOR_HOST") != null || System.getenv("FIREBASE_AUTH_EMULATOR_HOST") != null) {
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

        // 2. Real Production / Development Credentials Mode
        GoogleCredentials credentials = resolveCredentials(projectId);

        if (credentials == null) {
            String errorMsg = String.format(
                    "Firebase Admin SDK credentials not configured for project '%s'. " +
                    "To fix this for local development: " +
                    "1) Place your Firebase service account JSON file in the project root or backend directory as 'service-account.json', OR " +
                    "2) Set GOOGLE_APPLICATION_CREDENTIALS environment variable pointing to your service account JSON file, OR " +
                    "3) Set FIREBASE_CLIENT_EMAIL and FIREBASE_PRIVATE_KEY environment variables, OR " +
                    "4) Set FIREBASE_USE_EMULATOR=true to use Firebase Emulators.",
                    projectId
            );
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

        logger.info("FirebaseApp initialized successfully for projectId: {}", projectId);
        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials resolveCredentials(String projectId) {
        // A. Check explicit credentials path from properties
        if (properties.getCredentialsPath() != null && !properties.getCredentialsPath().trim().isEmpty()) {
            File credFile = new File(properties.getCredentialsPath());
            if (credFile.exists()) {
                logger.info("Loading Firebase credentials from app.firebase.credentials-path: {}", credFile.getAbsolutePath());
                try (InputStream is = new FileInputStream(credFile)) {
                    return GoogleCredentials.fromStream(is);
                } catch (IOException e) {
                    logger.error("Failed to read credentials file from path {}: {}", credFile.getAbsolutePath(), e.getMessage());
                }
            }
        }

        // B. Check GOOGLE_APPLICATION_CREDENTIALS environment variable
        String envCredPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (envCredPath != null && !envCredPath.trim().isEmpty()) {
            File envFile = new File(envCredPath);
            if (envFile.exists()) {
                logger.info("Loading Firebase credentials from GOOGLE_APPLICATION_CREDENTIALS: {}", envFile.getAbsolutePath());
                try (InputStream is = new FileInputStream(envFile)) {
                    return GoogleCredentials.fromStream(is);
                } catch (IOException e) {
                    logger.error("Failed to read GOOGLE_APPLICATION_CREDENTIALS file {}: {}", envFile.getAbsolutePath(), e.getMessage());
                }
            }
        }

        // C. Check standard local credential file names in working directory, backend, or parent root
        String[] candidateFileNames = {
                "service-account.json",
                "firebase-adminsdk.json",
                "serviceAccountKey.json",
                "backend/service-account.json",
                "backend/firebase-adminsdk.json",
                "../service-account.json"
        };
        for (String candidate : candidateFileNames) {
            File file = new File(candidate);
            if (file.exists() && file.isFile()) {
                logger.info("Auto-detected local Firebase service account key file: {}", file.getAbsolutePath());
                try (InputStream is = new FileInputStream(file)) {
                    return GoogleCredentials.fromStream(is);
                } catch (IOException e) {
                    logger.error("Failed to read auto-detected credentials file {}: {}", file.getAbsolutePath(), e.getMessage());
                }
            }
        }

        // D. Check inline private key and client email
        String privateKey = properties.getFormattedPrivateKey();
        String clientEmail = properties.getClientEmail();
        if (privateKey != null && !privateKey.trim().isEmpty() && clientEmail != null && !clientEmail.trim().isEmpty()) {
            logger.info("Loading Firebase credentials from inline clientEmail and privateKey for project: {}", projectId);
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
            try {
                return GoogleCredentials.fromStream(new ByteArrayInputStream(jsonCredentials.getBytes(StandardCharsets.UTF_8)));
            } catch (IOException e) {
                logger.error("Failed to parse inline service account credentials: {}", e.getMessage());
            }
        }

        // E. Check Application Default Credentials (ADC)
        try {
            logger.debug("Attempting Google Application Default Credentials (ADC)...");
            return GoogleCredentials.getApplicationDefault();
        } catch (Exception e) {
            logger.debug("Google Application Default Credentials (ADC) not available: {}", e.getMessage());
        }

        return null;
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
