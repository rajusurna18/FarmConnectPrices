package com.farmlink.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmlink.api.dto.LocationDto;
import com.farmlink.api.dto.RoleSelectionRequest;
import com.farmlink.api.dto.UpdateProfileRequest;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"app.firebase.use-emulator=true"})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void optionsPreflightOnProfileShouldReturnCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/profile")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("authorization")));
    }

    @Test
    void getProfileWithoutTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileWithAuthenticatedTokenShouldReturnProfile() throws Exception {
        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken("test-uid-456", "test@example.com", "Test User");
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("test-uid-456"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"));
    }

    @Test
    void updateRoleWithAdminRoleShouldReturnBadRequest() throws Exception {
        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken("test-uid-456", "test@example.com", "Test User");
        SecurityContextHolder.getContext().setAuthentication(auth);

        RoleSelectionRequest req = new RoleSelectionRequest("ADMIN");

        mockMvc.perform(put("/api/v1/profile/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRoleWithOldBuyerRoleShouldReturnBadRequest() throws Exception {
        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken("test-uid-456", "test@example.com", "Test User");
        SecurityContextHolder.getContext().setAuthentication(auth);

        RoleSelectionRequest req = new RoleSelectionRequest("BUYER");

        mockMvc.perform(put("/api/v1/profile/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRoleWithFarmerRoleShouldSucceed() throws Exception {
        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken("test-uid-456", "test@example.com", "Test User");
        SecurityContextHolder.getContext().setAuthentication(auth);

        RoleSelectionRequest req = new RoleSelectionRequest("FARMER");

        mockMvc.perform(put("/api/v1/profile/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("FARMER"))
                .andExpect(jsonPath("$.roleDisplayName").value("Farmer"));
    }

    @Test
    void updateRoleWithMediatorBuyerRoleShouldSucceed() throws Exception {
        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken("test-uid-456", "test@example.com", "Test User");
        SecurityContextHolder.getContext().setAuthentication(auth);

        RoleSelectionRequest req = new RoleSelectionRequest("MEDIATOR_BUYER");

        mockMvc.perform(put("/api/v1/profile/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEDIATOR_BUYER"))
                .andExpect(jsonPath("$.roleDisplayName").value("Mediator / Buyer"));
    }

    @Test
    void updateRoleWithCustomerRoleShouldSucceed() throws Exception {
        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken("test-uid-456", "test@example.com", "Test User");
        SecurityContextHolder.getContext().setAuthentication(auth);

        RoleSelectionRequest req = new RoleSelectionRequest("CUSTOMER");

        mockMvc.perform(put("/api/v1/profile/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.roleDisplayName").value("Customer"));
    }

    @Test
    void updateProfileWithoutTokenShouldReturnUnauthorized() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest("Updated Name", "+919876543210", new LocationDto("TS", "RR", "RNR", "BDV"), null, null);
        mockMvc.perform(put("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
