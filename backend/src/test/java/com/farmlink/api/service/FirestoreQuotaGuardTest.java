package com.farmlink.api.service;

import com.farmlink.api.exception.FirestoreQuotaExhaustedException;
import com.farmlink.api.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
        assertEquals(FirestoreQuotaGuard.State.CLOSED, quotaGuard.getState());
        assertDoesNotThrow(() -> quotaGuard.checkQuotaAvailability());
    }

    @Test
    @DisplayName("Should trip to OPEN on RESOURCE_EXHAUSTED and throw FirestoreQuotaExhaustedException during cooldown")
    public void testCircuitTrippingOnQuotaExhaustion() {
        Exception quotaError = new RuntimeException("io.grpc.StatusRuntimeException: RESOURCE_EXHAUSTED: Quota exceeded.");
        quotaGuard.recordQuotaExhaustion(quotaError);

        assertEquals(FirestoreQuotaGuard.State.OPEN, quotaGuard.getState());
        assertThrows(FirestoreQuotaExhaustedException.class, () -> quotaGuard.checkQuotaAvailability());
    }

    @Test
    @DisplayName("Should transition from OPEN to HALF_OPEN probe state when cooldown window expires, granting single probe ownership")
    public void testSingleProbeOwnershipAfterCooldown() throws InterruptedException {
        quotaGuard.recordQuotaExhaustion(new RuntimeException("RESOURCE_EXHAUSTED"));
        assertEquals(FirestoreQuotaGuard.State.OPEN, quotaGuard.getState());

        // Wait for 1.1s cooldown expiry
        Thread.sleep(1100L);

        // First caller wins probe ownership -> state transitions OPEN -> HALF_OPEN
        assertDoesNotThrow(() -> quotaGuard.checkQuotaAvailability());
        assertEquals(FirestoreQuotaGuard.State.HALF_OPEN, quotaGuard.getState());

        // Second caller during HALF_OPEN probe state is rejected immediately with 503
        assertThrows(FirestoreQuotaExhaustedException.class, () -> quotaGuard.checkQuotaAvailability());
    }

    @Test
    @DisplayName("10 concurrent requests after cooldown window yield exactly 1 probe owner and 9 rejected requests")
    public void testProbeStampedePrevention() throws InterruptedException {
        quotaGuard.recordQuotaExhaustion(new RuntimeException("RESOURCE_EXHAUSTED"));
        Thread.sleep(1100L); // Cooldown expires

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        AtomicInteger probeOwnerCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    quotaGuard.checkQuotaAvailability();
                    probeOwnerCount.incrementAndGet();
                } catch (FirestoreQuotaExhaustedException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release threads simultaneously
        finishLatch.await();
        executor.shutdown();

        assertEquals(1, probeOwnerCount.get(), "Exactly 1 thread must win probe ownership");
        assertEquals(9, rejectedCount.get(), "Remaining 9 concurrent threads must be rejected");
        assertEquals(FirestoreQuotaGuard.State.HALF_OPEN, quotaGuard.getState());
    }

    @Test
    @DisplayName("Successful probe transitions HALF_OPEN to CLOSED")
    public void testSuccessfulProbeClosesCircuit() throws InterruptedException {
        quotaGuard.recordQuotaExhaustion(new RuntimeException("RESOURCE_EXHAUSTED"));
        Thread.sleep(1100L);

        // Probe owner checks availability -> HALF_OPEN
        quotaGuard.checkQuotaAvailability();
        assertEquals(FirestoreQuotaGuard.State.HALF_OPEN, quotaGuard.getState());

        // Probe succeeds
        quotaGuard.recordSuccess();
        assertEquals(FirestoreQuotaGuard.State.CLOSED, quotaGuard.getState());
    }

    @Test
    @DisplayName("Failed probe transitions HALF_OPEN back to OPEN with new cooldown")
    public void testFailedProbeReopensCircuit() throws InterruptedException {
        quotaGuard.recordQuotaExhaustion(new RuntimeException("RESOURCE_EXHAUSTED"));
        Thread.sleep(1100L);

        // Probe owner checks availability -> HALF_OPEN
        quotaGuard.checkQuotaAvailability();
        assertEquals(FirestoreQuotaGuard.State.HALF_OPEN, quotaGuard.getState());

        // Probe fails with RESOURCE_EXHAUSTED
        quotaGuard.recordQuotaExhaustion(new RuntimeException("RESOURCE_EXHAUSTED"));
        assertEquals(FirestoreQuotaGuard.State.OPEN, quotaGuard.getState());
        assertThrows(FirestoreQuotaExhaustedException.class, () -> quotaGuard.checkQuotaAvailability());
    }

    @Test
    @DisplayName("classifyFailure correctly distinguishes QUOTA_EXHAUSTED from NETWORK_UNAVAILABLE")
    public void testFailureClassification() {
        Throwable quotaEx = new RuntimeException("io.grpc.StatusRuntimeException: RESOURCE_EXHAUSTED: Quota exceeded.");
        assertEquals(FirestoreQuotaGuard.FailureType.QUOTA_EXHAUSTED, FirestoreQuotaGuard.classifyFailure(quotaEx));

        Throwable netEx = new RuntimeException("UnknownHostException: firestore.googleapis.com");
        assertEquals(FirestoreQuotaGuard.FailureType.NETWORK_UNAVAILABLE, FirestoreQuotaGuard.classifyFailure(netEx));

        Throwable unavailEx = new RuntimeException("io.grpc.StatusRuntimeException: UNAVAILABLE: DnsNameResolver failed");
        assertEquals(FirestoreQuotaGuard.FailureType.NETWORK_UNAVAILABLE, FirestoreQuotaGuard.classifyFailure(unavailEx));
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
