package com.farmlink.api.service;

import com.farmlink.api.exception.FirestoreQuotaExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FirestoreQuotaGuard {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreQuotaGuard.class);

    private static final long DEFAULT_COOLDOWN_MS = 60_000L; // 60 seconds

    private final long cooldownMs;
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final AtomicLong cooldownUntilMillis = new AtomicLong(0L);
    private final AtomicLong lastLoggedTimestamp = new AtomicLong(0L);

    public FirestoreQuotaGuard() {
        this(DEFAULT_COOLDOWN_MS);
    }

    @Autowired
    public FirestoreQuotaGuard(@Value("${firestore.quota.cooldown-ms:60000}") long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public void checkQuotaAvailability() {
        long now = System.currentTimeMillis();
        if (circuitOpen.get()) {
            if (now < cooldownUntilMillis.get()) {
                throw new FirestoreQuotaExhaustedException("Firestore quota circuit breaker is OPEN. Cooldown active until " + cooldownUntilMillis.get());
            } else {
                // Cooldown expired - transition circuit to HALF_OPEN probe state
                logger.info("Firestore quota cooldown window expired. Transitioning circuit to probe state.");
                circuitOpen.set(false);
            }
        }
    }

    public void recordQuotaExhaustion(Throwable cause) {
        long now = System.currentTimeMillis();
        cooldownUntilMillis.set(now + cooldownMs);
        boolean wasClosed = circuitOpen.compareAndSet(false, true);

        // Rate-limited log: log warning at most once per cooldown window
        if (wasClosed || (now - lastLoggedTimestamp.get() > cooldownMs)) {
            lastLoggedTimestamp.set(now);
            logger.warn("Firestore quota exhaustion detected ({})! Circuit breaker OPEN for {} ms cooldown.",
                    cause != null ? cause.getMessage() : "Quota exceeded", cooldownMs);
        }
    }

    public void recordSuccess() {
        if (circuitOpen.get()) {
            logger.info("Firestore operation succeeded. Closing circuit breaker.");
            circuitOpen.set(false);
        }
    }

    public boolean isCircuitOpen() {
        return circuitOpen.get() && System.currentTimeMillis() < cooldownUntilMillis.get();
    }

    public void reset() {
        circuitOpen.set(false);
        cooldownUntilMillis.set(0L);
    }
}
