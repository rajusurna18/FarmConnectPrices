package com.farmlink.api.router.extractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocationEntityExtractorTest {

    private LocationEntityExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new LocationEntityExtractor(null);
    }

    @Test
    @DisplayName("Extracts states and agricultural districts")
    void testExtractLocations() {
        List<String> locs = extractor.extract("What is the weather in Guntur, Andhra Pradesh?");
        assertTrue(locs.contains("guntur"));
        assertTrue(locs.contains("andhra pradesh"));
    }

    @Test
    @DisplayName("Case-insensitive matching")
    void testCaseInsensitivity() {
        List<String> locs = extractor.extract("paddy rates in WARANGAL, TELANGANA");
        assertTrue(locs.contains("warangal"));
        assertTrue(locs.contains("telangana"));
    }
}

