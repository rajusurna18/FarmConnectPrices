package com.farmlink.api.router.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmlink.api.exception.GlobalExceptionHandler;
import com.farmlink.api.router.dto.*;
import com.farmlink.api.router.engine.AgriculturalIntentRouterEngine;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.entity.ConfidenceBand;
import com.farmlink.api.router.exception.*;
import com.farmlink.api.router.idempotency.CanonicalFingerprintCalculator;
import com.farmlink.api.router.idempotency.RedisSessionCoordinator;
import com.farmlink.api.router.idempotency.TurnLockHeartbeatManager;
import com.farmlink.api.router.security.UserDomainRole;
import com.farmlink.api.router.security.UserRoleResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AgriculturalRouterControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RedisSessionCoordinator coordinator;
    private TurnLockHeartbeatManager heartbeatManager;
    private CanonicalFingerprintCalculator fingerprintCalculator;
    private UserRoleResolver userRoleResolver;
    private AgriculturalIntentRouterEngine routerEngine;

    private final Principal principal = new TestingAuthenticationToken("farmer123", "password");

    @BeforeEach
    void setUp() {
        coordinator = mock(RedisSessionCoordinator.class);
        heartbeatManager = mock(TurnLockHeartbeatManager.class);
        fingerprintCalculator = mock(CanonicalFingerprintCalculator.class);
        userRoleResolver = mock(UserRoleResolver.class);
        routerEngine = mock(AgriculturalIntentRouterEngine.class);

        AgriculturalRouterController controller = new AgriculturalRouterController(
                coordinator,
                heartbeatManager,
                fingerprintCalculator,
                userRoleResolver,
                routerEngine,
                objectMapper
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /session initializes session with server-generated UUIDv4 and returns 201 CREATED")
    void testInitSessionSuccess() throws Exception {
        when(coordinator.initSession(anyString(), eq("farmer123")))
                .thenReturn(new RespTuple("COMMITTED", "NONE", "NONE"));

        mockMvc.perform(post("/api/v1/router/session")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("COMMITTED"))
                .andExpect(jsonPath("$.ttlSeconds").value(1800));
    }

    @Test
    @DisplayName("POST /turn processes new turn and returns 200 OK with cached=false")
    void testProcessTurnSuccess() throws Exception {
        RouterTurnRequest req = new RouterTurnRequest("sess-001", 1L, "What is the price of tomato in Guntur?", null);
        String fp = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        when(userRoleResolver.resolveUserRole("farmer123")).thenReturn(UserDomainRole.FARMER);
        when(fingerprintCalculator.calculateFingerprint("farmer123", 1L, req.query())).thenReturn(fp);
        when(coordinator.acquireLock(eq("sess-001"), eq(1L), eq("farmer123"), eq(fp), anyString()))
                .thenReturn(new RespTuple("LOCK_ACQUIRED", "NONE", "NONE"));
        when(heartbeatManager.startHeartbeat(anyString(), anyString(), anyString()))
                .thenReturn(mock(TurnLockHeartbeatManager.HeartbeatHandle.class));

        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("tomato"), List.of("guntur"), List.of(), List.of(), Collections.emptyMap()
        );
        RouterTurnResponse engineResp = new RouterTurnResponse(
                "sess-001", 1L, AgriIntent.MARKET_PRICE_INQUIRY, 0.85, ConfidenceBand.HIGH,
                entities, ClarificationDetails.notRequired(), true, false
        );
        when(routerEngine.processTurn(eq("sess-001"), eq(1L), eq(req.query()), eq(UserDomainRole.FARMER), eq("farmer123"), isNull()))
                .thenReturn(engineResp);
        when(coordinator.completeTurn(eq("sess-001"), eq(1L), eq("farmer123"), eq(fp), anyString(), anyString()))
                .thenReturn(new RespTuple("COMMITTED", "NONE", "NONE"));

        mockMvc.perform(post("/api/v1/router/turn")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-001"))
                .andExpect(jsonPath("$.turnIndex").value(1))
                .andExpect(jsonPath("$.intent").value("MARKET_PRICE_INQUIRY"))
                .andExpect(jsonPath("$.confidenceBand").value("HIGH"))
                .andExpect(jsonPath("$.cached").value(false));
    }

    @Test
    @DisplayName("POST /turn idempotent retry returns cached result with cached=true")
    void testProcessTurnCachedRetry() throws Exception {
        RouterTurnRequest req = new RouterTurnRequest("sess-001", 1L, "What is the price of tomato in Guntur?", null);
        String fp = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("tomato"), List.of("guntur"), List.of(), List.of(), Collections.emptyMap()
        );
        RouterTurnResponse cachedResp = new RouterTurnResponse(
                "sess-001", 1L, AgriIntent.MARKET_PRICE_INQUIRY, 0.85, ConfidenceBand.HIGH,
                entities, ClarificationDetails.notRequired(), true, false
        );
        String cachedJson = objectMapper.writeValueAsString(cachedResp);

        when(userRoleResolver.resolveUserRole("farmer123")).thenReturn(UserDomainRole.FARMER);
        when(fingerprintCalculator.calculateFingerprint("farmer123", 1L, req.query())).thenReturn(fp);
        when(coordinator.acquireLock(eq("sess-001"), eq(1L), eq("farmer123"), eq(fp), anyString()))
                .thenReturn(new RespTuple("RESULT_CACHED", cachedJson, "NONE"));

        mockMvc.perform(post("/api/v1/router/turn")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-001"))
                .andExpect(jsonPath("$.cached").value(true));

        verifyNoInteractions(routerEngine);
    }

    @Test
    @DisplayName("POST /turn with active concurrent execution returns 423 LOCKED")
    void testProcessTurnLocked() throws Exception {
        RouterTurnRequest req = new RouterTurnRequest("sess-001", 1L, "query", null);
        String fp = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        when(userRoleResolver.resolveUserRole("farmer123")).thenReturn(UserDomainRole.FARMER);
        when(fingerprintCalculator.calculateFingerprint(anyString(), anyLong(), anyString())).thenReturn(fp);
        when(coordinator.acquireLock(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new SessionLockedException("Turn is currently locked"));

        mockMvc.perform(post("/api/v1/router/turn")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isLocked());
    }

    @Test
    @DisplayName("POST /turn with fingerprint conflict returns 409 CONFLICT")
    void testProcessTurnConflict() throws Exception {
        RouterTurnRequest req = new RouterTurnRequest("sess-001", 1L, "different query", null);

        when(userRoleResolver.resolveUserRole("farmer123")).thenReturn(UserDomainRole.FARMER);
        when(fingerprintCalculator.calculateFingerprint(anyString(), anyLong(), anyString())).thenReturn("fp1");
        when(coordinator.acquireLock(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new IdempotencyConflictException("Fingerprint mismatch"));

        mockMvc.perform(post("/api/v1/router/turn")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /turn with expired committed result returns 410 GONE")
    void testProcessTurnExpired() throws Exception {
        RouterTurnRequest req = new RouterTurnRequest("sess-001", 1L, "query", null);

        when(userRoleResolver.resolveUserRole("farmer123")).thenReturn(UserDomainRole.FARMER);
        when(fingerprintCalculator.calculateFingerprint(anyString(), anyLong(), anyString())).thenReturn("fp1");
        when(coordinator.acquireLock(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new TurnResultExpiredException("Turn result expired"));

        mockMvc.perform(post("/api/v1/router/turn")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("POST /turn out of sequence returns 409 CONFLICT")
    void testProcessTurnOutOfSequence() throws Exception {
        RouterTurnRequest req = new RouterTurnRequest("sess-001", 5L, "query", null);

        when(userRoleResolver.resolveUserRole("farmer123")).thenReturn(UserDomainRole.FARMER);
        when(fingerprintCalculator.calculateFingerprint(anyString(), anyLong(), anyString())).thenReturn("fp1");
        when(coordinator.acquireLock(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new TurnOutOfSequenceException("Turn out of sequence"));

        mockMvc.perform(post("/api/v1/router/turn")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /turn with invalid turn index (< 1) returns 400 BAD REQUEST")
    void testInvalidTurnIndex() throws Exception {
        RouterTurnRequest req = new RouterTurnRequest("sess-001", 0L, "query", null);

        mockMvc.perform(post("/api/v1/router/turn")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
