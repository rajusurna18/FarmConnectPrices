package com.farmlink.api.router.extractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuantityEntityExtractorTest {

    private QuantityEntityExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new QuantityEntityExtractor();
    }

    @Test
    @DisplayName("Extracts various agricultural quantities and units")
    void testExtractQuantities() {
        List<String> q1 = extractor.extract("I want to sell 50 quintals of paddy");
        assertEquals(1, q1.size());
        assertEquals("50 quintals", q1.get(0));

        List<String> q2 = extractor.extract("I have 2.5 tons of maize and 100 kg seeds");
        assertEquals(2, q2.size());
        assertTrue(q2.contains("2.5 tons"));
        assertTrue(q2.contains("100 kg"));

        List<String> q3 = extractor.extract("Dosage for 5 acres of cotton");
        assertEquals(1, q3.size());
        assertEquals("5 acres", q3.get(0));
    }

    @Test
    @DisplayName("Empty query returns empty list")
    void testEmpty() {
        assertTrue(extractor.extract("").isEmpty());
        assertTrue(extractor.extract(null).isEmpty());
    }
}

