package com.farmlink.api.router.idempotency;

import com.farmlink.api.router.config.RouterConfigurationProperties;
import com.farmlink.api.router.dto.RespTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TurnLockHeartbeatManager {

    private static final Logger logger = LoggerFactory.getLogger(TurnLockHeartbeatManager.class);
    private static final String CANONICAL_LEASE_MS = "15000";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> heartbeatLockScript;
    private final RouterConfigurationProperties properties;

    public TurnLockHeartbeatManager(
            StringRedisTemplate redisTemplate,
            @Qualifier("heartbeatLockScript") RedisScript<List> heartbeatLockScript,
            RouterConfigurationProperties properties) {
        this.redisTemplate = redisTemplate;
        this.heartbeatLockScript = heartbeatLockScript;
        this.properties = properties;
    }

    public HeartbeatHandle startHeartbeat(String lockKey, String workerToken, String fingerprint) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "router-heartbeat-" + workerToken);
            t.setDaemon(true);
            return t;
        });

        AtomicBoolean active = new AtomicBoolean(true);
        long intervalMs = properties.getHeartbeatIntervalMs();

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (!active.get()) {
                return;
            }
            try {
                String timestampMs = String.valueOf(System.currentTimeMillis());
                List<?> result = redisTemplate.execute(
                        heartbeatLockScript,
                        Collections.singletonList(lockKey),
                        workerToken,
                        fingerprint,
                        CANONICAL_LEASE_MS,
                        timestampMs
                );
                RespTuple tuple = RespTuple.fromList(result);
                if (!tuple.isCommitted()) {
                    logger.warn("Heartbeat renewal failed for lockKey: {}, status: {}, error: {}",
                            lockKey, tuple.status(), tuple.error());
                }
            } catch (Exception e) {
                logger.warn("Error executing heartbeat script for lockKey: {}", lockKey, e);
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

        return new HeartbeatHandle(scheduler, future, active);
    }

    public static class HeartbeatHandle implements Closeable {
        private final ScheduledExecutorService scheduler;
        private final ScheduledFuture<?> future;
        private final AtomicBoolean active;

        public HeartbeatHandle(ScheduledExecutorService scheduler, ScheduledFuture<?> future, AtomicBoolean active) {
            this.scheduler = scheduler;
            this.future = future;
            this.active = active;
        }

        @Override
        public void close() {
            active.set(false);
            if (future != null) {
                future.cancel(true);
            }
            scheduler.shutdownNow();
        }
    }
}

