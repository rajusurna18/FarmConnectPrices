package com.farmlink.api.controller;

import com.farmlink.api.dto.HealthResponse;
import com.farmlink.api.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(healthService.getHealthStatus());
    }

    @GetMapping("/firebase")
    public ResponseEntity<HealthResponse> getFirebaseHealth() {
        return ResponseEntity.ok(healthService.getFirebaseHealthStatus());
    }
}
