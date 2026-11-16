package com.farmlink.api.router.extractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CropEntityExtractorTest {

    private CropEntityExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new CropEntityExtractor(null);
    }

    @Test
    @DisplayName("Extracts crops correctly from query")
    void testExtractCrops() {
        List<String> crops = extractor.extract("What is the market price of tomato and chilli today?");
        assertTrue(crops.contains("tomato"));
        assertTrue(crops.contains("chilli"));
    }

    @Test
    @DisplayName("Crucial: 'price' does not falsely match 'rice'")
    void testPriceDoesNotMatchRice() {
        List<String> crops = extractor.extract("What is the current market price?");
        assertFalse(crops.contains("rice"), "'price' must never trigger a match for 'rice'");
    }

    @Test
    @DisplayName("Extracts 'rice' when 'rice' is an independent word")
    void testIndependentRiceMatches() {
        List<String> crops = extractor.extract("What is the price of rice today?");
        assertTrue(crops.contains("rice"));
    }

    @Test
    @DisplayName("Extracts multi-word crops like pearl millet or red gram")
    void testMultiWordCrops() {
        List<String> crops = extractor.extract("How to grow pearl millet in rainfed conditions?");
        assertTrue(crops.contains("pearl millet"));
    }

    @Test
    @DisplayName("Empty or blank query returns empty list")
    void testEmptyQuery() {
        assertTrue(extractor.extract("").isEmpty());
        assertTrue(extractor.extract(null).isEmpty());
        assertTrue(extractor.extract("   ").isEmpty());
    }
}

