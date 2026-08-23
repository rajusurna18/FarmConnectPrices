package com.farmlink.api.service;

import com.farmlink.api.config.firebase.FirebaseProperties;
import com.farmlink.api.dto.HealthResponse;
import com.google.firebase.FirebaseApp;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final FirebaseProperties firebaseProperties;

    public HealthService(FirebaseProperties firebaseProperties) {
        this.firebaseProperties = firebaseProperties;
    }

    public HealthResponse getHealthStatus() {
        return new HealthResponse("UP", "farmlink-api");
    }

    public HealthResponse getFirebaseHealthStatus() {
        String firebaseStatus = "UNINITIALIZED";
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                if (firebaseProperties.isUseEmulator() || System.getenv("FIRESTORE_EMULATOR_HOST") != null) {
                    firebaseStatus = "EMULATOR_CONNECTED";
                } else if (firebaseProperties.isConfigured()) {
                    firebaseStatus = "CONFIGURED";
                } else {
                    firebaseStatus = "INITIALIZED";
                }
            }
        } catch (Exception e) {
            firebaseStatus = "ERROR";
        }
        return new HealthResponse("UP", "farmlink-api", firebaseStatus);
    }
}
