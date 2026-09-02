package com.farmlink.api.controller;

import com.farmlink.api.dto.ai.AiDecisionRequest;
import com.farmlink.api.dto.ai.AiDecisionResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.FarmService;
import com.farmlink.api.service.ai.AiDecisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/decisions")
public class AiDecisionController {

    private final AiDecisionService aiDecisionService;
    private final FarmService farmService;

    public AiDecisionController(AiDecisionService aiDecisionService, FarmService farmService) {
        this.aiDecisionService = aiDecisionService;
        this.farmService = farmService;
    }

    @PostMapping
    public ResponseEntity<AiDecisionResponse> processDecision(
            FirebaseAuthenticationToken token,
            @RequestBody AiDecisionRequest request) {

        if (token == null || token.getUid() == null) {
            return ResponseEntity.status(401).build();
        }

        String farmerUid = token.getUid();
        // Enforce FARMER role requirement
        farmService.verifyFarmerRole(farmerUid);

        AiDecisionResponse response = aiDecisionService.processDecisionRequest(farmerUid, request);
        return ResponseEntity.ok(response);
    }
}
