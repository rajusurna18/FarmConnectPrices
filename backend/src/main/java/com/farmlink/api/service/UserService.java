package com.farmlink.api.service;

import com.farmlink.api.dto.UserResponse;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final Firestore firestore;

    public UserService(Firestore firestore) {
        this.firestore = firestore;
    }

    public UserResponse getUserByUid(String uid, String email, String displayName) {
        String resolvedDisplayName = (displayName != null && !displayName.trim().isEmpty())
                ? displayName
                : (email != null ? email.split("@")[0] : "User");
        String role = "USER";
        String status = "ACTIVE";

        if (firestore != null && uid != null) {
            try {
                DocumentSnapshot doc = firestore.collection("users").document(uid).get().get();
                if (doc.exists()) {
                    if (doc.getString("displayName") != null) {
                        resolvedDisplayName = doc.getString("displayName");
                    }
                    if (doc.getString("role") != null) {
                        role = doc.getString("role");
                    }
                    if (doc.getString("status") != null) {
                        status = doc.getString("status");
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.warn("Could not read users/{} document from Firestore: {}", uid, e.getMessage());
            } catch (Exception e) {
                logger.warn("Unexpected error reading Firestore user document: {}", e.getMessage());
            }
        }

        return new UserResponse(uid, email, resolvedDisplayName, role, status);
    }
}
