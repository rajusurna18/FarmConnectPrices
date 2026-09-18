package com.farmlink.api.controller;

import com.farmlink.api.dto.NotificationPreferenceResponse;
import com.farmlink.api.dto.UpdateNotificationPreferenceRequest;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.NotificationPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    /**
     * Get authenticated user's notification preferences.
     */
    @GetMapping
    public ResponseEntity<?> getPreferences() {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            NotificationPreferenceResponse response = preferenceService.getPreferences(token.getUid());
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch notification preferences: " + e.getMessage());
        }
    }

    /**
     * Update authenticated user's notification preferences.
     */
    @PutMapping
    public ResponseEntity<?> updatePreferences(@RequestBody UpdateNotificationPreferenceRequest request) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            NotificationPreferenceResponse response = preferenceService.updatePreferences(token.getUid(), request);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update notification preferences: " + e.getMessage());
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
