package com.farmlink.api.router.idempotency;

import com.farmlink.api.router.dto.RespTuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RedisSessionCoordinatorIntegrationTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisSessionCoordinator coordinator;

    @Test
    @DisplayName("Integration: Init session, acquire lock, and complete turn lifecycle")
    void testSessionLifecycle() {
        String sessionId = "integ-sess-001";
        String userId = "farmer-integ";
        String fp = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String token = "abcdef0123456789abcdef0123456789";

        // 1. Init Session
        RespTuple initResp = coordinator.initSession(sessionId, userId);
        assertTrue(initResp.isCommitted());

        // 2. Acquire Lock
        RespTuple acqResp = coordinator.acquireLock(sessionId, 1L, userId, fp, token);
        assertTrue(acqResp.isLockAcquired());

        // 3. Complete Turn
        String payload = "{\"status\":\"OK\"}";
        RespTuple compResp = coordinator.completeTurn(sessionId, 1L, userId, fp, token, payload);
        assertTrue(compResp.isCommitted() || compResp.isCompletedWarning());

        // 4. Retry Same Turn (Idempotency) -> returns cached result
        RespTuple retryResp = coordinator.acquireLock(sessionId, 1L, userId, fp, token);
        assertTrue(retryResp.isResultCached());
        assertEquals(payload, retryResp.payload());
    }
}

