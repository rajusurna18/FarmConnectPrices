package com.farmlink.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe metrics tracker for monitoring Firestore query RPC invocations,
 * document-level read counts, write budgets, and cache hit/miss ratios.
 */
@Component
public class FirestoreMetrics {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreMetrics.class);

    private final AtomicLong queryRpcInvocations = new AtomicLong(0L);
    private final AtomicLong documentsFetched = new AtomicLong(0L);
    private final AtomicLong totalWrites = new AtomicLong(0L);
    private final AtomicLong cacheHits = new AtomicLong(0L);
    private final AtomicLong cacheMisses = new AtomicLong(0L);
    private final AtomicLong quotaExhaustionEvents = new AtomicLong(0L);
    private final AtomicLong networkFailureEvents = new AtomicLong(0L);

    private final Map<String, AtomicLong> documentsByCollection = new ConcurrentHashMap<>();

    public void recordQuery(String collection, long docsCount) {
        queryRpcInvocations.incrementAndGet();
        documentsFetched.addAndGet(docsCount);
        documentsByCollection.computeIfAbsent(collection, k -> new AtomicLong(0L)).addAndGet(docsCount);
    }

    public void recordWrite(long count) {
        totalWrites.addAndGet(count);
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void recordQuotaExhaustion() {
        quotaExhaustionEvents.incrementAndGet();
    }

    public void recordNetworkFailure() {
        networkFailureEvents.incrementAndGet();
    }

    public Map<String, Object> getMetricsSummary() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        double hitRatio = (hits + misses) > 0 ? (double) hits / (hits + misses) : 1.0;

        return Map.of(
            "queryRpcInvocations", queryRpcInvocations.get(),
            "documentsFetched", documentsFetched.get(),
            "totalWrites", totalWrites.get(),
            "cacheHits", hits,
            "cacheMisses", misses,
            "cacheHitRatio", String.format("%.2f%%", hitRatio * 100),
            "quotaExhaustionEvents", quotaExhaustionEvents.get(),
            "networkFailureEvents", networkFailureEvents.get(),
            "documentsByCollection", documentsByCollection
        );
    }

    public void resetMetrics() {
        queryRpcInvocations.set(0L);
        documentsFetched.set(0L);
        totalWrites.set(0L);
        cacheHits.set(0L);
        cacheMisses.set(0L);
        quotaExhaustionEvents.set(0L);
        networkFailureEvents.set(0L);
        documentsByCollection.clear();
    }
}
