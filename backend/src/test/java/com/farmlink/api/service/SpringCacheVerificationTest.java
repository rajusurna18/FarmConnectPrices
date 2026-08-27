package com.farmlink.api.service;

import com.farmlink.api.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {CacheConfig.class})
public class SpringCacheVerificationTest {

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        if (cacheManager.getCache("crops") != null) {
            cacheManager.getCache("crops").clear();
        }
    }

    @Test
    @DisplayName("Verify Spring CacheManager registers master data caches")
    public void testCacheRegistration() {
        assertNotNull(cacheManager.getCache("states"));
        assertNotNull(cacheManager.getCache("districts"));
        assertNotNull(cacheManager.getCache("markets"));
        assertNotNull(cacheManager.getCache("crops"));
        assertNotNull(cacheManager.getCache("marketCrops"));
    }

    @Test
    @DisplayName("Verify single invocation caches result and second call avoids underlying execution")
    public void testCacheHitBehavior() {
        AtomicInteger computeCount = new AtomicInteger(0);

        // Simulate a cached service call using Spring CacheManager
        var cache = cacheManager.getCache("crops");
        assertNotNull(cache);

        // Call #1: Cache Miss
        String key = "allCrops";
        String value = cache.get(key, () -> {
            computeCount.incrementAndGet();
            return "realCropData";
        });

        assertEquals("realCropData", value);
        assertEquals(1, computeCount.get());

        // Call #2: Cache Hit (Value fetched directly from cache, loader not called)
        String cachedValue = cache.get(key, () -> {
            computeCount.incrementAndGet();
            return "shouldNotBeCalled";
        });

        assertEquals("realCropData", cachedValue);
        assertEquals(1, computeCount.get(), "Underlying loader must NOT be invoked on cache hit");
    }

    @Test
    @DisplayName("Verify 10 concurrent requests to cold cache execute loader only once")
    public void testConcurrentColdLoadProtection() throws InterruptedException {
        var cache = cacheManager.getCache("markets");
        assertNotNull(cache);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        AtomicInteger loaderExecutions = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    cache.get("allMarkets", () -> {
                        loaderExecutions.incrementAndGet();
                        try {
                            Thread.sleep(50); // Simulate network latency
                        } catch (InterruptedException ignored) {}
                        return "realMarketList";
                    });
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        assertEquals(1, loaderExecutions.get(), "Cold cache load under concurrency must execute loader exactly once");
    }
}
