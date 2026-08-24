package com.farmlink.api.service;

import com.farmlink.api.dto.LocationDto;
import com.farmlink.api.dto.RoleSelectionRequest;
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
    void calculateProfileCompleted_farmer_returnsTrue_whenLocationAndPhonePresent() {
        LocationDto loc = new LocationDto("Telangana", "Ranga Reddy", "Rajendranagar", "Budvel");
        assertTrue(profileService.calculateProfileCompleted("FARMER", "+919876543210", loc, null, null));
    }

    @Test
    void calculateProfileCompleted_mediatorBuyer_returnsTrue_whenOrgPresent() {
        LocationDto loc = new LocationDto("Telangana", "Ranga Reddy", "Rajendranagar", "Budvel");
        assertTrue(profileService.calculateProfileCompleted("MEDIATOR_BUYER", "+919876543210", loc, "AgriTraders LLC", null));
        assertFalse(profileService.calculateProfileCompleted("MEDIATOR_BUYER", "+919876543210", loc, null, null));
    }

    @Test
    void calculateProfileCompleted_customer_returnsTrue_whenAddressPresent() {
        LocationDto loc = new LocationDto("Telangana", "Ranga Reddy", "Rajendranagar", "Budvel");
        assertTrue(profileService.calculateProfileCompleted("CUSTOMER", "+919876543210", loc, null, "123 Main St, Hyderabad"));
        assertFalse(profileService.calculateProfileCompleted("CUSTOMER", "+919876543210", loc, null, null));
    }

    @Test
    void updateRole_acceptsValidPrimaryRoles() {
        RoleSelectionRequest farmerReq = new RoleSelectionRequest("FARMER");
        assertEquals("FARMER", profileService.updateRole("uid-1", "user@example.com", true, "User", farmerReq).getRole());

        RoleSelectionRequest mediatorReq = new RoleSelectionRequest("MEDIATOR_BUYER");
        assertEquals("MEDIATOR_BUYER", profileService.updateRole("uid-1", "user@example.com", true, "User", mediatorReq).getRole());

        RoleSelectionRequest customerReq = new RoleSelectionRequest("CUSTOMER");
        assertEquals("CUSTOMER", profileService.updateRole("uid-1", "user@example.com", true, "User", customerReq).getRole());
    }

    @Test
    void updateRole_throwsException_forForbiddenAndOldRoles() {
        RoleSelectionRequest adminReq = new RoleSelectionRequest("ADMIN");
        assertThrows(IllegalArgumentException.class, () ->
                profileService.updateRole("uid-1", "user@example.com", true, "User", adminReq)
        );

        RoleSelectionRequest deliveryReq = new RoleSelectionRequest("DELIVERY_PARTNER");
        assertThrows(IllegalArgumentException.class, () ->
                profileService.updateRole("uid-1", "user@example.com", true, "User", deliveryReq)
        );

        RoleSelectionRequest middlemanReq = new RoleSelectionRequest("MIDDLEMAN");
        assertThrows(IllegalArgumentException.class, () ->
                profileService.updateRole("uid-1", "user@example.com", true, "User", middlemanReq)
        );

        RoleSelectionRequest oldBuyerReq = new RoleSelectionRequest("BUYER");
        assertThrows(IllegalArgumentException.class, () ->
                profileService.updateRole("uid-1", "user@example.com", true, "User", oldBuyerReq)
        );

        RoleSelectionRequest nullReq = new RoleSelectionRequest(null);
        assertThrows(IllegalArgumentException.class, () ->
                profileService.updateRole("uid-1", "user@example.com", true, "User", nullReq)
        );
    }
}
