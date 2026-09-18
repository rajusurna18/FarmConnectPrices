package com.farmlink.api.controller;

import com.farmlink.api.dto.NotificationPreferenceResponse;
import com.farmlink.api.dto.UpdateNotificationPreferenceRequest;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.NotificationPreferenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class NotificationPreferenceControllerTest {

    private NotificationPreferenceService preferenceService;
    private NotificationPreferenceController controller;

    private static final String USER_UID = "user_pref_ctl_100";

    @BeforeEach
    public void setUp() {
        preferenceService = Mockito.mock(NotificationPreferenceService.class);
        controller = new NotificationPreferenceController(preferenceService);
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
    public void testGetPreferences_Unauthenticated_Returns401() {
        ResponseEntity<?> response = controller.getPreferences();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testGetPreferences_Authenticated_ReturnsPreferences() {
        authenticateUser();
        NotificationPreferenceResponse expected = new NotificationPreferenceResponse(true, true, true, true, true);
        when(preferenceService.getPreferences(eq(USER_UID))).thenReturn(expected);

        ResponseEntity<?> response = controller.getPreferences();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    public void testUpdatePreferences_Unauthenticated_Returns401() {
        UpdateNotificationPreferenceRequest req = new UpdateNotificationPreferenceRequest(false, true, true, false, true);
        ResponseEntity<?> response = controller.updatePreferences(req);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testUpdatePreferences_Authenticated_ReturnsUpdated() {
        authenticateUser();
        UpdateNotificationPreferenceRequest req = new UpdateNotificationPreferenceRequest(false, true, true, false, true);
        NotificationPreferenceResponse expected = new NotificationPreferenceResponse(false, true, true, false, true);

        when(preferenceService.updatePreferences(eq(USER_UID), eq(req))).thenReturn(expected);

        ResponseEntity<?> response = controller.updatePreferences(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }
}
