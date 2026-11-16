package com.farmlink.api.router.idempotency;

import com.farmlink.api.router.dto.RespTuple;
import com.farmlink.api.router.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class RedisSessionCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(RedisSessionCoordinator.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> initSessionScript;
    private final RedisScript<List> acquireLockScript;
    private final RedisScript<List> completeTurnScript;

    public RedisSessionCoordinator(
            StringRedisTemplate redisTemplate,
            @Qualifier("initSessionScript") RedisScript<List> initSessionScript,
            @Qualifier("acquireLockScript") RedisScript<List> acquireLockScript,
            @Qualifier("completeTurnScript") RedisScript<List> completeTurnScript) {
        this.redisTemplate = redisTemplate;
        this.initSessionScript = initSessionScript;
        this.acquireLockScript = acquireLockScript;
        this.completeTurnScript = completeTurnScript;
    }

    public static String getSessionKey(String sessionId) {
        return "{session:" + sessionId + "}";
    }

    public static String getLockKey(String sessionId, long turnIndex) {
        return "{session:" + sessionId + "}:lock:" + turnIndex;
    }

    public static String getResultKey(String sessionId, long turnIndex) {
        return "{session:" + sessionId + "}:result:" + turnIndex;
    }

    public RespTuple initSession(String sessionId, String ownerUserId) {
        String sessionKey = getSessionKey(sessionId);
        String timestampMs = String.valueOf(System.currentTimeMillis());

        try {
            List<?> result = redisTemplate.execute(
                    initSessionScript,
                    Collections.singletonList(sessionKey),
                    ownerUserId,
                    timestampMs
            );
            RespTuple tuple = RespTuple.fromList(result);
            if (tuple.isConflict()) {
                throw new IdempotencyConflictException("Session already exists: " + sessionId);
            }
            if (!tuple.isCommitted()) {
                throw new RedisCoordinationException("Failed to initialize session: " + tuple.error());
            }
            return tuple;
        } catch (IdempotencyConflictException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error executing init_session.lua for session: {}", sessionId, e);
            throw new RedisCoordinationException("Redis error during session initialization", e);
        }
    }

    public RespTuple acquireLock(String sessionId, long turnIndex, String ownerUserId, String fingerprint, String workerToken) {
        String sessionKey = getSessionKey(sessionId);
        String lockKey = getLockKey(sessionId, turnIndex);
        String resultKey = getResultKey(sessionId, turnIndex);
        String timestampMs = String.valueOf(System.currentTimeMillis());

        try {
            List<?> result = redisTemplate.execute(
                    acquireLockScript,
                    Arrays.asList(sessionKey, lockKey, resultKey),
                    ownerUserId,
                    String.valueOf(turnIndex),
                    fingerprint,
                    workerToken,
                    timestampMs
            );
            RespTuple tuple = RespTuple.fromList(result);
            handleAcquireErrors(tuple, sessionId, turnIndex);
            return tuple;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error executing acquire_lock.lua for session: {}, turn: {}", sessionId, turnIndex, e);
            throw new RedisCoordinationException("Redis error during lock acquisition", e);
        }
    }

    public RespTuple completeTurn(String sessionId, long turnIndex, String ownerUserId, String fingerprint, String workerToken, String resultJson) {
        String sessionKey = getSessionKey(sessionId);
        String lockKey = getLockKey(sessionId, turnIndex);
        String resultKey = getResultKey(sessionId, turnIndex);
        String timestampMs = String.valueOf(System.currentTimeMillis());

        try {
            List<?> result = redisTemplate.execute(
                    completeTurnScript,
                    Arrays.asList(sessionKey, lockKey, resultKey),
                    ownerUserId,
                    String.valueOf(turnIndex),
                    fingerprint,
                    workerToken,
                    resultJson,
                    timestampMs
            );
            RespTuple tuple = RespTuple.fromList(result);
            handleCompleteErrors(tuple, sessionId, turnIndex);
            return tuple;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error executing complete_turn.lua for session: {}, turn: {}", sessionId, turnIndex, e);
            throw new RedisCoordinationException("Redis error during turn completion", e);
        }
    }

    private void handleAcquireErrors(RespTuple tuple, String sessionId, long turnIndex) {
        if (tuple.isLockAcquired() || tuple.isResultCached()) {
            return;
        }

        String err = tuple.error();
        if (tuple.isExpired() || "TURN_RESULT_EXPIRED_410_GONE".equals(err)) {
            throw new TurnResultExpiredException("Turn " + turnIndex + " result has expired (410 GONE)");
        }
        if (tuple.isInProgress() || "TURN_EXECUTION_IN_PROGRESS".equals(err)) {
            throw new SessionLockedException("Turn " + turnIndex + " is currently being processed by another worker");
        }
        if (tuple.isConflict()) {
            if ("IDEMPOTENCY_FINGERPRINT_MISMATCH".equals(err)) {
                throw new IdempotencyConflictException("Turn " + turnIndex + " already completed with a different request fingerprint");
            }
            throw new IdempotencyConflictException("Conflict acquiring lock: " + err);
        }
        if (tuple.isRejected()) {
            if ("SESSION_NOT_FOUND".equals(err)) {
                throw new SessionNotFoundException("Session not found: " + sessionId);
            }
            if ("SESSION_OWNER_CONFLICT".equals(err)) {
                throw new UnauthorizedEconomicsAccessException("Authenticated user does not own session: " + sessionId);
            }
            if ("TURN_OUT_OF_SEQUENCE".equals(err)) {
                throw new TurnOutOfSequenceException("Turn " + turnIndex + " is out of sequence");
            }
            if ("SESSION_TERMINAL_CLOSED".equals(err)) {
                throw new TurnOutOfSequenceException("Session " + sessionId + " is closed in TERMINAL state");
            }
            if ("SESSION_CORRUPTED_TYPE".equals(err) || "SESSION_CORRUPTED_MISSING_FIELDS".equals(err)
                    || "SESSION_CORRUPTED_NON_NUMERIC".equals(err) || "SESSION_STATE_CORRUPTED".equals(err)) {
                throw new SessionCorruptedException("Session data corrupted: " + err);
            }
            throw new TurnOutOfSequenceException("Rejected: " + err);
        }
        if (tuple.isError()) {
            throw new RedisCoordinationException("Redis coordination error: " + err);
        }
    }

    private void handleCompleteErrors(RespTuple tuple, String sessionId, long turnIndex) {
        if (tuple.isCommitted() || tuple.isCompletedWarning() || tuple.isResultCached()) {
            return;
        }

        String err = tuple.error();
        if (tuple.isExpired() || "TURN_RESULT_EXPIRED_410_GONE".equals(err)) {
            throw new TurnResultExpiredException("Turn " + turnIndex + " result has expired (410 GONE)");
        }
        if (tuple.isConflict()) {
            if ("LOCK_OWNERSHIP_LOST".equals(err)) {
                throw new SessionLockedException("Lock ownership lost before turn completion: " + err);
            }
            if ("IDEMPOTENCY_FINGERPRINT_MISMATCH".equals(err)) {
                throw new IdempotencyConflictException("Turn " + turnIndex + " already completed with a different fingerprint");
            }
            throw new IdempotencyConflictException("Conflict completing turn: " + err);
        }
        if (tuple.isRejected()) {
            if ("SESSION_NOT_FOUND".equals(err)) {
                throw new SessionNotFoundException("Session not found: " + sessionId);
            }
            if ("SESSION_OWNER_CONFLICT".equals(err)) {
                throw new UnauthorizedEconomicsAccessException("Authenticated user does not own session: " + sessionId);
            }
            if ("TURN_OUT_OF_SEQUENCE".equals(err)) {
                throw new TurnOutOfSequenceException("Turn " + turnIndex + " is out of sequence");
            }
            if ("SESSION_TERMINAL_CLOSED".equals(err)) {
                throw new TurnOutOfSequenceException("Session " + sessionId + " is closed in TERMINAL state");
            }
            if ("LOCK_NOT_FOUND".equals(err)) {
                throw new SessionLockedException("Turn lock not found during completion");
            }
            throw new TurnOutOfSequenceException("Rejected: " + err);
        }
        if (tuple.isError()) {
            throw new RedisCoordinationException("Redis completion error: " + err);
        }
    }
}

