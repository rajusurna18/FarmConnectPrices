package com.farmlink.api.service;

import com.farmlink.api.dto.LocationDto;
import com.farmlink.api.dto.ProfileResponse;
import com.farmlink.api.dto.RoleSelectionRequest;
import com.farmlink.api.dto.UpdateProfileRequest;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class ProfileServiceTest {

    private Firestore firestore;
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        firestore = Mockito.mock(Firestore.class);
        profileService = new ProfileService(firestore);
    }

    @Test
    void calculateProfileCompleted_returnsTrue_whenAllFieldsPresent() {
        LocationDto loc = new LocationDto("Telangana", "Ranga Reddy", "Rajendranagar", "Budvel");
        assertTrue(profileService.calculateProfileCompleted("+919876543210", loc));
    }

    @Test
    void calculateProfileCompleted_returnsFalse_whenLocationIncomplete() {
        LocationDto loc = new LocationDto("Telangana", "Ranga Reddy", "", "Budvel");
        assertFalse(profileService.calculateProfileCompleted("+919876543210", loc));
    }

    @Test
    void calculateProfileCompleted_returnsFalse_whenPhoneMissing() {
        LocationDto loc = new LocationDto("Telangana", "Ranga Reddy", "Rajendranagar", "Budvel");
        assertFalse(profileService.calculateProfileCompleted(null, loc));
        assertFalse(profileService.calculateProfileCompleted("", loc));
    }

    @Test
    void updateRole_throwsException_forForbiddenRoles() {
        RoleSelectionRequest adminReq = new RoleSelectionRequest("ADMIN");
        IllegalArgumentException exAdmin = assertThrows(IllegalArgumentException.class, () ->
                profileService.updateRole("uid-1", "user@example.com", true, "User", adminReq)
        );
        assertTrue(exAdmin.getMessage().contains("Invalid role selected"));

        RoleSelectionRequest middlemanReq = new RoleSelectionRequest("MIDDLEMAN");
        assertThrows(IllegalArgumentException.class, () ->
                profileService.updateRole("uid-1", "user@example.com", true, "User", middlemanReq)
        );

        RoleSelectionRequest deliveryReq = new RoleSelectionRequest("DELIVERY_PARTNER");
        assertThrows(IllegalArgumentException.class, () ->
                profileService.updateRole("uid-1", "user@example.com", true, "User", deliveryReq)
        );

        RoleSelectionRequest nullReq = new RoleSelectionRequest(null);
        assertThrows(IllegalArgumentException.class, () ->
                profileService.updateRole("uid-1", "user@example.com", true, "User", nullReq)
        );
    }
}
