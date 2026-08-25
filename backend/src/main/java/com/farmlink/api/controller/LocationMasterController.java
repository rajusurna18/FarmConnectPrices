package com.farmlink.api.controller;

import com.farmlink.api.dto.LocationMasterResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.LocationMasterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationMasterController {

    private final LocationMasterService locationMasterService;

    public LocationMasterController(LocationMasterService locationMasterService) {
        this.locationMasterService = locationMasterService;
    }

    @GetMapping
    public ResponseEntity<?> getAllLocations() {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        List<LocationMasterResponse> locations = locationMasterService.getAllLocations();
        return ResponseEntity.ok(locations);
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof FirebaseAuthenticationToken token) {
            return token;
        }
        return null;
    }
}
