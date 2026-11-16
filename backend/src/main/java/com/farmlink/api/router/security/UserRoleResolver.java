package com.farmlink.api.router.security;

import com.farmlink.api.service.FirestoreQuotaGuard;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * Resolves the authenticated user's current domain role directly from Firestore on every turn.
 * Role caching is strictly forbidden.
 */
@Component
public class UserRoleResolver {

    private static final Logger logger = LoggerFactory.getLogger(UserRoleResolver.class);

    private final Firestore firestore;
    private final FirestoreQuotaGuard firestoreQuotaGuard;

    public UserRoleResolver(@Autowired(required = false) Firestore firestore,
                            FirestoreQuotaGuard firestoreQuotaGuard) {
        this.firestore = firestore;
        this.firestoreQuotaGuard = firestoreQuotaGuard;
    }

    public UserDomainRole resolveUserRole(String uid) {
        if (uid == null || uid.isBlank()) {
            return UserDomainRole.CUSTOMER;
        }

        if (firestore == null) {
            logger.warn("Firestore client is null, falling back to default role CUSTOMER for uid: {}", uid);
            return UserDomainRole.CUSTOMER;
        }

        firestoreQuotaGuard.checkQuotaAvailability();

        try {
            DocumentSnapshot doc = firestore.collection("users").document(uid).get().get();
            firestoreQuotaGuard.recordSuccess();

            if (doc.exists()) {
                String roleStr = doc.getString("role");
                return UserDomainRole.fromString(roleStr);
            } else {
                logger.info("User document not found for uid: {}, defaulting to CUSTOMER", uid);
                return UserDomainRole.CUSTOMER;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (firestoreQuotaGuard != null) {
                firestoreQuotaGuard.recordQuotaExhaustion(e);
            }
            throw new RuntimeException("Interrupted while resolving user role for uid: " + uid, e);
        } catch (ExecutionException e) {
            if (firestoreQuotaGuard != null) {
                firestoreQuotaGuard.recordQuotaExhaustion(e);
            }
            logger.error("Failed to fetch user role from Firestore for uid: {}", uid, e);
            throw new RuntimeException("Error resolving user role from Firestore", e);
        } catch (Exception e) {
            if (firestoreQuotaGuard != null) {
                firestoreQuotaGuard.recordQuotaExhaustion(e);
            }
            throw e;
        }
    }
}
