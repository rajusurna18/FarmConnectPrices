package com.farmlink.api.controller;

import com.farmlink.api.dto.ProfileResponse;
import com.farmlink.api.dto.RoleSelectionRequest;
import com.farmlink.api.dto.UpdateProfileRequest;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<?> getProfile() {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        ProfileResponse profile = profileService.getProfile(
                token.getUid(),
                token.getEmail(),
                token.isEmailVerified(),
                token.getDisplayName()
        );

        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            ProfileResponse profile = profileService.updateProfile(
                    token.getUid(),
                    token.getEmail(),
                    token.isEmailVerified(),
                    token.getDisplayName(),
                    request
            );
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/role")
    public ResponseEntity<?> updateRole(@RequestBody RoleSelectionRequest request) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            ProfileResponse profile = profileService.updateRole(
                    token.getUid(),
                    token.getEmail(),
                    token.isEmailVerified(),
                    token.getDisplayName(),
                    request
            );
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof FirebaseAuthenticationToken token) {
            return token;
        }
        return null;
    }
}
