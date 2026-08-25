package com.farmlink.api.controller;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.CropMasterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crops")
public class CropMasterController {

    private final CropMasterService cropMasterService;

    public CropMasterController(CropMasterService cropMasterService) {
        this.cropMasterService = cropMasterService;
    }

    @GetMapping
    public ResponseEntity<?> getAllCrops() {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        List<CropResponse> crops = cropMasterService.getAllCrops();
        return ResponseEntity.ok(crops);
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof FirebaseAuthenticationToken token) {
            return token;
        }
        return null;
    }
}
