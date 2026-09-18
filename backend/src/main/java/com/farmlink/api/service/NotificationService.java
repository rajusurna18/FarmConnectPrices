package com.farmlink.api.service;

import com.farmlink.api.dto.CreateNotificationRequest;
import com.farmlink.api.dto.NotificationListResponse;
import com.farmlink.api.dto.NotificationResponse;
import com.farmlink.api.dto.UnreadCountResponse;
import com.farmlink.api.model.Notification;
import com.farmlink.api.model.NotificationType;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    public static final String COLLECTION_NAME = "notifications";

    private final Firestore firestore;
    private final FirestoreQuotaGuard quotaGuard;
    private final NotificationPreferenceService preferenceService;

    // In-memory fallback for isolated unit testing when Firestore bean is absent
    private final Map<String, Notification> inMemoryNotifications = new ConcurrentHashMap<>();

    @Autowired
    public NotificationService(
            @Autowired(required = false) Firestore firestore,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard,
            @Autowired(required = false) NotificationPreferenceService preferenceService
    ) {
        this.firestore = firestore;
        this.quotaGuard = quotaGuard;
        this.preferenceService = preferenceService;
    }

    /**
     * Creates and persists a new in-app notification.
     * Enforces recipient identity validation and idempotency via sourceEventId.
     */
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateNotificationRequest cannot be null");
        }
        if (request.getRecipientUid() == null || request.getRecipientUid().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient UID is required");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Notification title is required");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message is required");
        }

        String recipientUid = request.getRecipientUid().trim();

        // Check recipient notification preference
        if (preferenceService != null && !preferenceService.isCategoryEnabled(recipientUid, request.getType())) {
            logger.info("Notification creation suppressed for recipient {} and type {} according to user preferences", recipientUid, request.getType());
            return null;
        }

        // Idempotency check using sourceEventId if present
        if (request.getSourceEventId() != null && !request.getSourceEventId().trim().isEmpty()) {
            String sourceEventId = request.getSourceEventId().trim();
            Optional<Notification> existing = findBySourceEventId(recipientUid, sourceEventId);
            if (existing.isPresent()) {
                logger.info("Notification with sourceEventId {} already exists for recipient {}. Returning existing record.",
                        sourceEventId, recipientUid);
                return new NotificationResponse(existing.get());
            }
        }

        String notificationId = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();

        Notification notification = new Notification(
                notificationId,
                recipientUid,
                request.getRecipientRole() != null ? request.getRecipientRole() : "",
                request.getType(),
                request.getTitle().trim(),
                request.getMessage().trim(),
                false,
                request.getRelatedEntityType() != null ? request.getRelatedEntityType() : "",
                request.getRelatedEntityId() != null ? request.getRelatedEntityId() : "",
                request.getRelatedRoute() != null ? request.getRelatedRoute() : "",
                request.getMetadata() != null ? request.getMetadata() : new HashMap<>(),
                request.getSourceEventId() != null ? request.getSourceEventId().trim() : "",
                createdAt,
                null
        );

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                firestore.collection(COLLECTION_NAME).document(notificationId).set(notification).get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.error("Error persisting notification {} to Firestore: {}", notificationId, e.getMessage(), e);
                // Fallback to in-memory store to prevent crashing callers
                inMemoryNotifications.put(notificationId, notification);
            }
        } else {
            inMemoryNotifications.put(notificationId, notification);
        }

        logger.info("Created notification {} of type {} for user {}", notificationId, notification.getType(), recipientUid);
        return new NotificationResponse(notification);
    }

    /**
     * Retrieves paginated notifications for the authenticated recipient user.
     */
    public NotificationListResponse getUserNotifications(String recipientUid, Boolean unreadOnly, Integer page, Integer pageSize) {
        validateUid(recipientUid);
        int p = (page != null && page >= 0) ? page : 0;
        int size = (pageSize != null && pageSize > 0) ? Math.min(pageSize, 50) : 20;
        boolean onlyUnread = Boolean.TRUE.equals(unreadOnly);

        List<Notification> userNotifications = new ArrayList<>();

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("recipientUid", recipientUid);

                if (onlyUnread) {
                    query = query.whereEqualTo("read", false);
                }

                ApiFuture<QuerySnapshot> querySnapshot = query.get();
                QuerySnapshot snapshot = querySnapshot.get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Notification n = doc.toObject(Notification.class);
                    if (n != null) {
                        userNotifications.add(n);
                    }
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error fetching notifications from Firestore for user {}: {}. Falling back to in-memory.", recipientUid, e.getMessage());
                userNotifications = getInMemoryNotificationsForUser(recipientUid, onlyUnread);
            }
        } else {
            userNotifications = getInMemoryNotificationsForUser(recipientUid, onlyUnread);
        }

        // Sort descending by createdAt
        userNotifications.sort((a, b) -> {
            String t1 = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String t2 = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return t2.compareTo(t1);
        });

        long totalCount = userNotifications.size();
        long unreadCount = userNotifications.stream().filter(n -> !n.isRead()).count();

        int fromIndex = Math.min(p * size, userNotifications.size());
        int toIndex = Math.min(fromIndex + size, userNotifications.size());
        List<NotificationResponse> pageContent = userNotifications.subList(fromIndex, toIndex)
                .stream()
                .map(NotificationResponse::new)
                .collect(Collectors.toList());

        boolean hasMore = toIndex < userNotifications.size();

        return new NotificationListResponse(pageContent, totalCount, unreadCount, p, size, hasMore);
    }

    /**
     * Gets unread notifications count for a recipient.
     */
    public UnreadCountResponse getUnreadCount(String recipientUid) {
        validateUid(recipientUid);
        long count = 0;

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("recipientUid", recipientUid)
                        .whereEqualTo("read", false);

                ApiFuture<QuerySnapshot> snapshotFuture = query.get();
                QuerySnapshot snapshot = snapshotFuture.get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                count = snapshot.size();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error fetching unread count from Firestore for {}: {}", recipientUid, e.getMessage());
                count = inMemoryNotifications.values().stream()
                        .filter(n -> recipientUid.equals(n.getRecipientUid()) && !n.isRead())
                        .count();
            }
        } else {
            count = inMemoryNotifications.values().stream()
                    .filter(n -> recipientUid.equals(n.getRecipientUid()) && !n.isRead())
                    .count();
        }

        return new UnreadCountResponse(count);
    }

    /**
     * Fetches a single notification by ID and verifies recipient ownership.
     */
    public NotificationResponse getNotificationById(String notificationId, String recipientUid) {
        validateUid(recipientUid);
        Notification notification = findNotification(notificationId);

        if (notification == null) {
            throw new NoSuchElementException("Notification not found with ID: " + notificationId);
        }
        if (!recipientUid.equals(notification.getRecipientUid())) {
            throw new AccessDeniedException("Access denied. You do not own this notification.");
        }

        return new NotificationResponse(notification);
    }

    /**
     * Marks a notification as read. Enforces recipient ownership.
     */
    public NotificationResponse markAsRead(String notificationId, String recipientUid) {
        validateUid(recipientUid);
        Notification notification = findNotification(notificationId);

        if (notification == null) {
            throw new NoSuchElementException("Notification not found with ID: " + notificationId);
        }
        if (!recipientUid.equals(notification.getRecipientUid())) {
            throw new AccessDeniedException("Access denied. You cannot modify notifications belonging to another user.");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now().toString());

            if (firestore != null) {
                try {
                    if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                    firestore.collection(COLLECTION_NAME).document(notificationId).set(notification).get();
                    if (quotaGuard != null) quotaGuard.recordSuccess();
                } catch (Exception e) {
                    if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                    logger.warn("Error updating notification read status in Firestore: {}", e.getMessage());
                    inMemoryNotifications.put(notificationId, notification);
                }
            } else {
                inMemoryNotifications.put(notificationId, notification);
            }
        }

        return new NotificationResponse(notification);
    }

    /**
     * Marks all notifications as read for a recipient.
     */
    public UnreadCountResponse markAllAsRead(String recipientUid) {
        validateUid(recipientUid);
        List<Notification> unreadList = new ArrayList<>();

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("recipientUid", recipientUid)
                        .whereEqualTo("read", false);

                QuerySnapshot snapshot = query.get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                WriteBatch batch = firestore.batch();
                String now = Instant.now().toString();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Notification n = doc.toObject(Notification.class);
                    if (n != null) {
                        n.setRead(true);
                        n.setReadAt(now);
                        batch.set(doc.getReference(), n);
                        unreadList.add(n);
                    }
                }

                if (!unreadList.isEmpty()) {
                    batch.commit().get();
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error marking all read in Firestore for user {}: {}. Updating in-memory.", recipientUid, e.getMessage());
                markAllInMemoryRead(recipientUid);
            }
        } else {
            markAllInMemoryRead(recipientUid);
        }

        return new UnreadCountResponse(0);
    }

    /**
     * Deletes a notification by ID. Enforces recipient ownership.
     */
    public void deleteNotification(String notificationId, String recipientUid) {
        validateUid(recipientUid);
        Notification notification = findNotification(notificationId);

        if (notification == null) {
            return; // Idempotent deletion
        }
        if (!recipientUid.equals(notification.getRecipientUid())) {
            throw new AccessDeniedException("Access denied. You cannot delete notifications belonging to another user.");
        }

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                firestore.collection(COLLECTION_NAME).document(notificationId).delete().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error deleting notification from Firestore: {}", e.getMessage());
            }
        }
        inMemoryNotifications.remove(notificationId);
    }

    // Helper methods
    private void validateUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new AccessDeniedException("Authenticated user UID is required");
        }
    }

    private Optional<Notification> findBySourceEventId(String recipientUid, String sourceEventId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("recipientUid", recipientUid)
                        .whereEqualTo("sourceEventId", sourceEventId);

                QuerySnapshot snapshot = query.get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                if (!snapshot.isEmpty()) {
                    return Optional.ofNullable(snapshot.getDocuments().get(0).toObject(Notification.class));
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
            }
        }
        return inMemoryNotifications.values().stream()
                .filter(n -> recipientUid.equals(n.getRecipientUid()) && sourceEventId.equals(n.getSourceEventId()))
                .findFirst();
    }

    private Notification findNotification(String notificationId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot doc = firestore.collection(COLLECTION_NAME).document(notificationId).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                if (doc.exists()) {
                    return doc.toObject(Notification.class);
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
            }
        }
        return inMemoryNotifications.get(notificationId);
    }

    private List<Notification> getInMemoryNotificationsForUser(String recipientUid, boolean unreadOnly) {
        return inMemoryNotifications.values().stream()
                .filter(n -> recipientUid.equals(n.getRecipientUid()))
                .filter(n -> !unreadOnly || !n.isRead())
                .collect(Collectors.toList());
    }

    private void markAllInMemoryRead(String recipientUid) {
        String now = Instant.now().toString();
        for (Notification n : inMemoryNotifications.values()) {
            if (recipientUid.equals(n.getRecipientUid()) && !n.isRead()) {
                n.setRead(true);
                n.setReadAt(now);
            }
        }
    }
}
