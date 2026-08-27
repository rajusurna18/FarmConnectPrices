package com.farmlink.api.service;

import com.farmlink.api.exception.FirestoreQuotaExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FirestoreQuotaGuard {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreQuotaGuard.class);

    public enum State {
        CLOSED,      // Normal operational state
        OPEN,        // Circuit open during cooldown window
        HALF_OPEN    // Cooldown window expired - single probe owner active
    }

    public enum FailureType {
        QUOTA_EXHAUSTED,
        NETWORK_UNAVAILABLE,
        UNKNOWN
    }

    private static final long DEFAULT_COOLDOWN_MS = 60_000L; // 60 seconds

    private final long cooldownMs;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicLong cooldownUntilMillis = new AtomicLong(0L);
    private final AtomicLong lastLoggedTimestamp = new AtomicLong(0L);

    public FirestoreQuotaGuard() {
        this(DEFAULT_COOLDOWN_MS);
    }

    @Autowired
    public FirestoreQuotaGuard(@Value("${firestore.quota.cooldown-ms:60000}") long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    /**
     * Checks if a database request is allowed to proceed to Firestore.
     * Enforces single-probe ownership during HALF_OPEN state.
     */
    public void checkQuotaAvailability() {
        long now = System.currentTimeMillis();
        State currentState = state.get();

        if (currentState == State.OPEN) {
            if (now < cooldownUntilMillis.get()) {
                throw new FirestoreQuotaExhaustedException("Firestore quota circuit breaker is OPEN. Cooldown active until " + cooldownUntilMillis.get());
            } else {
                // Cooldown window expired -> attempt atomic transition to HALF_OPEN probe owner
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    logger.info("Firestore quota cooldown window expired. Transitioning circuit to HALF_OPEN probe state. Single probe owner active.");
                    return;
                } else {
                    // Another concurrent thread won probe ownership
                    throw new FirestoreQuotaExhaustedException("Firestore quota circuit breaker is HALF_OPEN (Probe in progress). Skipping concurrent request.");
                }
            }
        } else if (currentState == State.HALF_OPEN) {
            // A probe is currently in progress by another thread -> reject concurrent requests
            throw new FirestoreQuotaExhaustedException("Firestore quota circuit breaker is HALF_OPEN (Probe in progress). Skipping concurrent request.");
        }
    }

    /**
     * Records a database failure and classifies whether it is quota exhaustion or network/DNS failure.
     */
    public void recordQuotaExhaustion(Throwable cause) {
        long now = System.currentTimeMillis();
        cooldownUntilMillis.set(now + cooldownMs);
        State previousState = state.getAndSet(State.OPEN);

        FailureType failureType = classifyFailure(cause);

        // Rate-limited log: log warning at most once per cooldown window
        if (previousState != State.OPEN || (now - lastLoggedTimestamp.get() > cooldownMs)) {
            lastLoggedTimestamp.set(now);
            if (failureType == FailureType.QUOTA_EXHAUSTED) {
                logger.warn("Firestore quota exhaustion detected (RESOURCE_EXHAUSTED)! Circuit breaker OPEN for {} ms cooldown.", cooldownMs);
            } else if (failureType == FailureType.NETWORK_UNAVAILABLE) {
                logger.warn("Firestore network/DNS failure detected ({})! Circuit breaker OPEN for {} ms cooldown.",
                        cause != null ? cause.getMessage() : "Network failure", cooldownMs);
            } else {
                logger.warn("Firestore error detected ({})! Circuit breaker OPEN for {} ms cooldown.",
                        cause != null ? cause.getMessage() : "Unknown error", cooldownMs);
            }
        }
    }

    /**
     * Called when a database probe or operation succeeds.
     * Transitions circuit to CLOSED state.
     */
    public void recordSuccess() {
        State current = state.get();
        if (current != State.CLOSED) {
            if (state.compareAndSet(current, State.CLOSED)) {
                logger.info("Firestore operation succeeded. Closing circuit breaker.");
            }
        }
    }

    public State getState() {
        return state.get();
    }

    public boolean isCircuitOpen() {
        return state.get() == State.OPEN && System.currentTimeMillis() < cooldownUntilMillis.get();
    }

    public void reset() {
        state.set(State.CLOSED);
        cooldownUntilMillis.set(0L);
    }

    public static FailureType classifyFailure(Throwable t) {
        if (t == null) return FailureType.UNKNOWN;
        String msg = t.getMessage();
        if (msg != null) {
            if (msg.contains("RESOURCE_EXHAUSTED") || msg.contains("Quota exceeded")) {
                return FailureType.QUOTA_EXHAUSTED;
            }
            if (msg.contains("UnknownHostException") || msg.contains("UNAVAILABLE") || msg.contains("DnsNameResolver")) {
                return FailureType.NETWORK_UNAVAILABLE;
            }
        }
        if (t.getCause() != null && t.getCause() != t) {
            return classifyFailure(t.getCause());
        }
        return FailureType.UNKNOWN;
    }
}
