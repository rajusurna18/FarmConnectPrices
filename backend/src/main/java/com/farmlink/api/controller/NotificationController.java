package com.farmlink.api.controller;

import com.farmlink.api.dto.NotificationListResponse;
import com.farmlink.api.dto.NotificationResponse;
import com.farmlink.api.dto.UnreadCountResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Get authenticated user's notifications with pagination and optional unread filter.
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            NotificationListResponse response = notificationService.getUserNotifications(token.getUid(), unreadOnly, page, pageSize);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch notifications: " + e.getMessage());
        }
    }

    /**
     * Get authenticated user's unread notifications.
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            NotificationListResponse response = notificationService.getUserNotifications(token.getUid(), true, page, pageSize);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch unread notifications: " + e.getMessage());
        }
    }

    /**
     * Get authenticated user's unread notification count.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            UnreadCountResponse response = notificationService.getUnreadCount(token.getUid());
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch unread count: " + e.getMessage());
        }
    }

    /**
     * Get single notification by ID for authenticated user.
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<?> getNotificationById(@PathVariable String notificationId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            NotificationResponse response = notificationService.getNotificationById(notificationId, token.getUid());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch notification: " + e.getMessage());
        }
    }

    /**
     * Mark a single notification as read for authenticated user.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String notificationId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            NotificationResponse response = notificationService.markAsRead(notificationId, token.getUid());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mark notification as read: " + e.getMessage());
        }
    }

    /**
     * Mark all notifications as read for authenticated user.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            UnreadCountResponse response = notificationService.markAllAsRead(token.getUid());
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mark all as read: " + e.getMessage());
        }
    }

    /**
     * Delete a single notification for authenticated user.
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable String notificationId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            notificationService.deleteNotification(notificationId, token.getUid());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete notification: " + e.getMessage());
        }
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof FirebaseAuthenticationToken token) {
            return token;
        }
        return null;
    }
}
