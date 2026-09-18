package com.farmlink.api.service;

import com.farmlink.api.dto.NotificationPreferenceResponse;
import com.farmlink.api.dto.UpdateNotificationPreferenceRequest;
import com.farmlink.api.model.NotificationCategory;
import com.farmlink.api.model.NotificationPreference;
import com.farmlink.api.model.NotificationType;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceService.class);
    public static final String COLLECTION_NAME = "notificationPreferences";

    private final Firestore firestore;
    private final FirestoreQuotaGuard quotaGuard;

    // In-memory cache to prevent excessive Firestore reads for every notification event
    private final Map<String, NotificationPreference> preferenceCache = new ConcurrentHashMap<>();

    @Autowired
    public NotificationPreferenceService(
            @Autowired(required = false) Firestore firestore,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard
    ) {
        this.firestore = firestore;
        this.quotaGuard = quotaGuard;
    }

    /**
     * Gets user notification preferences for the given UID. Returns default if not yet saved in Firestore.
     */
    public NotificationPreferenceResponse getPreferences(String uid) {
        validateUid(uid);
        NotificationPreference preference = getOrCreatePreference(uid);
        logger.debug("notification_preferences.loaded for user {}", uid);
        return new NotificationPreferenceResponse(preference);
    }

    /**
     * Updates user notification preferences and refreshes in-memory cache.
     */
    public NotificationPreferenceResponse updatePreferences(String uid, UpdateNotificationPreferenceRequest request) {
        validateUid(uid);
        if (request == null) {
            throw new IllegalArgumentException("Update request cannot be null");
        }

        NotificationPreference current = getOrCreatePreference(uid);

        if (request.getOffers() != null) {
            current.setOffers(request.getOffers());
        }
        if (request.getOrders() != null) {
            current.setOrders(request.getOrders());
        }
        if (request.getPayments() != null) {
            current.setPayments(request.getPayments());
        }
        if (request.getDelivery() != null) {
            current.setDelivery(request.getDelivery());
        }
        if (request.getReviews() != null) {
            current.setReviews(request.getReviews());
        }

        current.setUpdatedAt(Instant.now().toString());

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                firestore.collection(COLLECTION_NAME).document(uid).set(current).get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.error("notification_preferences.update_failed for user {}: {}", uid, e.getMessage(), e);
            }
        }

        // Update in-memory cache
        preferenceCache.put(uid, current);
        logger.info("notification_preferences.updated for user {}", uid);

        return new NotificationPreferenceResponse(current);
    }

    /**
     * Checks if notification delivery is enabled for the specified user and notification type.
     * Guaranteed transaction safety: defaults to true if preference lookup fails.
     */
    public boolean isCategoryEnabled(String recipientUid, NotificationType type) {
        if (recipientUid == null || recipientUid.trim().isEmpty() || type == null) {
            return true;
        }

        NotificationCategory category = NotificationCategory.fromNotificationType(type);
        if (category == null) {
            return true;
        }

        try {
            NotificationPreference pref = getOrCreatePreference(recipientUid);
            boolean enabled = switch (category) {
                case OFFERS -> pref.isOffers();
                case ORDERS -> pref.isOrders();
                case PAYMENTS -> pref.isPayments();
                case DELIVERY -> pref.isDelivery();
                case REVIEWS -> pref.isReviews();
            };

            if (!enabled) {
                logger.info("notification.suppressed_by_preference: Notification type {} for recipient {} suppressed by {} preference",
                        type, recipientUid, category);
            }

            return enabled;
        } catch (Exception e) {
            logger.warn("Error checking notification preference for user {}: {}. Defaulting to enabled.", recipientUid, e.getMessage());
            return true;
        }
    }

    /**
     * Helper to fetch preference from cache, Firestore, or fallback to default.
     */
    private NotificationPreference getOrCreatePreference(String uid) {
        if (preferenceCache.containsKey(uid)) {
            return preferenceCache.get(uid);
        }

        NotificationPreference preference = null;

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot doc = firestore.collection(COLLECTION_NAME).document(uid).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                if (doc.exists()) {
                    preference = doc.toObject(NotificationPreference.class);
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error reading notification preferences from Firestore for user {}: {}", uid, e.getMessage());
            }
        }

        if (preference == null) {
            preference = NotificationPreference.createDefault(uid);
        }

        preferenceCache.put(uid, preference);
        return preference;
    }

    private void validateUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("User UID is required for notification preference operations");
        }
    }
}
