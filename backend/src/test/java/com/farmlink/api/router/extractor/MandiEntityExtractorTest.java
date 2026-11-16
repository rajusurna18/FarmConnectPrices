package com.farmlink.api.router.extractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MandiEntityExtractorTest {

    private MandiEntityExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new MandiEntityExtractor(null);
    }

    @Test
    @DisplayName("Extracts mandi names and general mandi references")
    void testExtractMandis() {
        List<String> mandis = extractor.extract("What is the rate at Guntur mandi today?");
        assertTrue(mandis.contains("guntur mandi") || mandis.contains("mandi"));
    }

    @Test
    @DisplayName("Extracts APMC or Rythu Bazaar")
    void testApmcRythuBazaar() {
        List<String> m1 = extractor.extract("Bowenpally market arrivals");
        assertTrue(m1.contains("bowenpally"));

        List<String> m2 = extractor.extract("Rythu Bazaar tomato price");
        assertTrue(m2.contains("rythu bazaar"));
    }
}

