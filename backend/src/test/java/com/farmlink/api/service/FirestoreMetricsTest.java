package com.farmlink.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FirestoreMetricsTest {

    private FirestoreMetrics metrics;

    @BeforeEach
    public void setUp() {
        metrics = new FirestoreMetrics();
    }

    @Test
    @DisplayName("Should accurately record reads, writes, and cache hits/misses")
    public void testRecordMetrics() {
        metrics.recordRead("crops", 10);
        metrics.recordRead("markets", 5);
        metrics.recordWrite(2);
        metrics.recordCacheHit();
        metrics.recordCacheHit();
        metrics.recordCacheMiss();

        Map<String, Object> summary = metrics.getMetricsSummary();
        assertEquals(15L, summary.get("totalReads"));
        assertEquals(2L, summary.get("totalWrites"));
        assertEquals(2L, summary.get("cacheHits"));
        assertEquals(1L, summary.get("cacheMisses"));
        assertEquals("66.67%", summary.get("cacheHitRatio"));
    }

    @Test
    @DisplayName("Should reset metrics cleanly")
    public void testResetMetrics() {
        metrics.recordRead("crops", 10);
        metrics.recordQuotaExhaustion();
        metrics.resetMetrics();

        Map<String, Object> summary = metrics.getMetricsSummary();
        assertEquals(0L, summary.get("totalReads"));
        assertEquals(0L, summary.get("quotaExhaustionEvents"));
    }
}
