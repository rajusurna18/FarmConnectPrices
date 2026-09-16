package com.farmlink.api.controller;

import com.farmlink.api.dto.NotificationListResponse;
import com.farmlink.api.dto.NotificationResponse;
import com.farmlink.api.dto.UnreadCountResponse;
import com.farmlink.api.model.Notification;
import com.farmlink.api.model.NotificationType;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class NotificationControllerTest {

    private NotificationService notificationService;
    private NotificationController controller;

    private static final String USER_UID = "user_uid_100";

    @BeforeEach
    public void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        controller = new NotificationController(notificationService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                USER_UID, "user@example.com", "User", true, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    public void testGetNotifications_Unauthenticated_Returns401() {
        ResponseEntity<?> response = controller.getNotifications(false, 0, 20);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testGetNotifications_Authenticated_ReturnsSuccess() {
        authenticateUser();
        NotificationListResponse expected = new NotificationListResponse(List.of(), 0, 0, 0, 20, false);
        when(notificationService.getUserNotifications(eq(USER_UID), eq(false), eq(0), eq(20))).thenReturn(expected);

        ResponseEntity<?> response = controller.getNotifications(false, 0, 20);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    public void testGetUnreadNotifications_Authenticated_ReturnsSuccess() {
        authenticateUser();
        NotificationListResponse expected = new NotificationListResponse(List.of(), 0, 0, 0, 20, false);
        when(notificationService.getUserNotifications(eq(USER_UID), eq(true), eq(0), eq(20))).thenReturn(expected);

        ResponseEntity<?> response = controller.getUnreadNotifications(0, 20);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    public void testGetUnreadCount_Authenticated_ReturnsCount() {
        authenticateUser();
        UnreadCountResponse expected = new UnreadCountResponse(5);
        when(notificationService.getUnreadCount(eq(USER_UID))).thenReturn(expected);

        ResponseEntity<?> response = controller.getUnreadCount();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    public void testGetNotificationById_NotFound_Returns404() {
        authenticateUser();
        when(notificationService.getNotificationById(eq("notif_999"), eq(USER_UID)))
                .thenThrow(new NoSuchElementException("Notification not found"));

        ResponseEntity<?> response = controller.getNotificationById("notif_999");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testGetNotificationById_Forbidden_Returns403() {
        authenticateUser();
        when(notificationService.getNotificationById(eq("notif_999"), eq(USER_UID)))
                .thenThrow(new AccessDeniedException("Access denied"));

        ResponseEntity<?> response = controller.getNotificationById("notif_999");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testMarkAsRead_Authenticated_ReturnsUpdatedNotification() {
        authenticateUser();
        Notification n = new Notification("notif_1", USER_UID, "FARMER", NotificationType.NEW_OFFER, "Title", "Message", true, "OFFER", "off_1", "/route", null, "evt_1", "2026-09-16T10:00:00Z", "2026-09-16T10:05:00Z");
        NotificationResponse expected = new NotificationResponse(n);

        when(notificationService.markAsRead(eq("notif_1"), eq(USER_UID))).thenReturn(expected);

        ResponseEntity<?> response = controller.markAsRead("notif_1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    public void testMarkAllAsRead_Authenticated_ReturnsZeroUnread() {
        authenticateUser();
        UnreadCountResponse expected = new UnreadCountResponse(0);
        when(notificationService.markAllAsRead(eq(USER_UID))).thenReturn(expected);

        ResponseEntity<?> response = controller.markAllAsRead();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    public void testDeleteNotification_Authenticated_Returns204NoContent() {
        authenticateUser();
        ResponseEntity<?> response = controller.deleteNotification("notif_1");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
