package com.farmlink.api.service;

import com.farmlink.api.exception.FirestoreQuotaExhaustedException;
import com.farmlink.api.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FirestoreQuotaGuardTest {

    private FirestoreQuotaGuard quotaGuard;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    public void setUp() {
        quotaGuard = new FirestoreQuotaGuard(1000L); // 1-second cooldown for fast unit tests
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should be CLOSED initially and allow operations")
    public void testInitialClosedState() {
        assertFalse(quotaGuard.isCircuitOpen());
        assertDoesNotThrow(() -> quotaGuard.checkQuotaAvailability());
    }

    @Test
    @DisplayName("Should trip to OPEN on RESOURCE_EXHAUSTED and throw FirestoreQuotaExhaustedException during cooldown")
    public void testCircuitTrippingOnQuotaExhaustion() {
        Exception quotaError = new RuntimeException("io.grpc.StatusRuntimeException: RESOURCE_EXHAUSTED: Quota exceeded.");
        quotaGuard.recordQuotaExhaustion(quotaError);

        assertTrue(quotaGuard.isCircuitOpen());
        assertThrows(FirestoreQuotaExhaustedException.class, () -> quotaGuard.checkQuotaAvailability());
    }

    @Test
    @DisplayName("Should transition from OPEN to CLOSED when cooldown window expires")
    public void testCircuitCooldownExpiry() throws InterruptedException {
        quotaGuard.recordQuotaExhaustion(new RuntimeException("RESOURCE_EXHAUSTED"));
        assertTrue(quotaGuard.isCircuitOpen());

        // Wait for 1.1s cooldown expiry
        Thread.sleep(1100L);

        assertFalse(quotaGuard.isCircuitOpen());
        assertDoesNotThrow(() -> quotaGuard.checkQuotaAvailability());
    }

    @Test
    @DisplayName("GlobalExceptionHandler should convert FirestoreQuotaExhaustedException to HTTP 503 SERVICE_UNAVAILABLE")
    public void testExceptionHandlerMapping() {
        FirestoreQuotaExhaustedException ex = new FirestoreQuotaExhaustedException("Firestore quota circuit breaker is OPEN.");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleFirestoreQuotaExhaustedException(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").contains("database quota limits"));
    }
}
