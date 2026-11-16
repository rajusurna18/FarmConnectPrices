package com.farmlink.api.router.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmlink.api.orchestrator.service.AgriOrchestrationService;
import com.farmlink.api.router.dto.*;
import com.farmlink.api.router.engine.AgriculturalIntentRouterEngine;
import com.farmlink.api.router.idempotency.CanonicalFingerprintCalculator;
import com.farmlink.api.router.idempotency.RedisSessionCoordinator;
import com.farmlink.api.router.idempotency.TurnLockHeartbeatManager;
import com.farmlink.api.router.security.UserDomainRole;
import com.farmlink.api.router.security.UserRoleResolver;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/router")
public class AgriculturalRouterController {

    private static final Logger logger = LoggerFactory.getLogger(AgriculturalRouterController.class);

    private final RedisSessionCoordinator coordinator;
    private final TurnLockHeartbeatManager heartbeatManager;
    private final CanonicalFingerprintCalculator fingerprintCalculator;
    private final UserRoleResolver userRoleResolver;
    private final AgriculturalIntentRouterEngine routerEngine;
    private final ObjectMapper objectMapper;
    private final AgriOrchestrationService orchestrationService;

    public AgriculturalRouterController(
            RedisSessionCoordinator coordinator,
            TurnLockHeartbeatManager heartbeatManager,
            CanonicalFingerprintCalculator fingerprintCalculator,
            UserRoleResolver userRoleResolver,
            AgriculturalIntentRouterEngine routerEngine,
            ObjectMapper objectMapper) {
        this(coordinator, heartbeatManager, fingerprintCalculator, userRoleResolver, routerEngine, objectMapper, null);
    }

    @Autowired
    public AgriculturalRouterController(
            RedisSessionCoordinator coordinator,
            TurnLockHeartbeatManager heartbeatManager,
            CanonicalFingerprintCalculator fingerprintCalculator,
            UserRoleResolver userRoleResolver,
            AgriculturalIntentRouterEngine routerEngine,
            ObjectMapper objectMapper,
            @Autowired(required = false) AgriOrchestrationService orchestrationService) {
        this.coordinator = coordinator;
        this.heartbeatManager = heartbeatManager;
        this.fingerprintCalculator = fingerprintCalculator;
        this.userRoleResolver = userRoleResolver;
        this.routerEngine = routerEngine;
        this.objectMapper = objectMapper;
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/session")
    public ResponseEntity<SessionInitResponse> initSession(
            @RequestBody(required = false) SessionInitRequest request,
            Authentication authentication) {

        String uid = extractUid(authentication);
        String serverGeneratedSessionId = UUID.randomUUID().toString();
        coordinator.initSession(serverGeneratedSessionId, uid);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new SessionInitResponse(serverGeneratedSessionId, "COMMITTED", 1800L)
        );
    }

    @PostMapping("/turn")
    public ResponseEntity<RouterTurnResponse> processTurn(
            @Valid @RequestBody RouterTurnRequest request,
            Authentication authentication) {

        String uid = extractUid(authentication);

        // Resolve current role fresh from Firestore on every turn (zero caching)
        UserDomainRole callerRole = userRoleResolver.resolveUserRole(uid);

        // Deterministic SHA-256 canonical fingerprint
        String fingerprint = fingerprintCalculator.calculateFingerprint(uid, request.turnIndex(), request.query());

        // 32-hex worker token
        String workerToken = UUID.randomUUID().toString().replace("-", "").toLowerCase();

        // Step 1: Attempt lock acquisition or cached retry retrieval
        RespTuple acqTuple = coordinator.acquireLock(
                request.sessionId(),
                request.turnIndex(),
                uid,
                fingerprint,
                workerToken
        );

        // Idempotent retry: cached result available
        if (acqTuple.isResultCached()) {
            try {
                RouterTurnResponse cached = objectMapper.readValue(acqTuple.payload(), RouterTurnResponse.class);
                return ResponseEntity.ok(new RouterTurnResponse(
                        cached.sessionId(),
                        cached.turnIndex(),
                        cached.intent(),
                        cached.confidence(),
                        cached.confidenceBand(),
                        cached.entities(),
                        cached.clarification(),
                        cached.authorized(),
                        true,
                        cached.orchestration()
                ));
            } catch (Exception e) {
                logger.error("Failed to deserialize cached turn response JSON", e);
                throw new IllegalStateException("Corrupted cached turn result JSON", e);
            }
        }

        // Lock acquired: run background heartbeat and process turn
        String lockKey = RedisSessionCoordinator.getLockKey(request.sessionId(), request.turnIndex());
        try (TurnLockHeartbeatManager.HeartbeatHandle ignored = heartbeatManager.startHeartbeat(lockKey, workerToken, fingerprint)) {

            RouterTurnResponse response = routerEngine.processTurn(
                    request.sessionId(),
                    request.turnIndex(),
                    request.query(),
                    callerRole,
                    uid,
                    request.targetOwnerUid()
            );

            RouterTurnResponse finalResponse = response;
            if (orchestrationService != null) {
                finalResponse = orchestrationService.orchestrate(
                        response,
                        request.query(),
                        callerRole,
                        uid
                );
            }

            String resultJson;
            try {
                resultJson = objectMapper.writeValueAsString(finalResponse);
            } catch (Exception e) {
                logger.error("Failed to serialize router turn response to JSON", e);
                throw new IllegalStateException("Failed to serialize turn response", e);
            }

            coordinator.completeTurn(
                    request.sessionId(),
                    request.turnIndex(),
                    uid,
                    fingerprint,
                    workerToken,
                    resultJson
            );

            return ResponseEntity.ok(finalResponse);
        }
    }

    private String extractUid(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        return authentication.getName();
    }
}

