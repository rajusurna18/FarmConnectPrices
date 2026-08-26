package com.farmlink.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceUnitConversionServiceTest {

    private PriceUnitConversionService conversionService;

    @BeforeEach
    void setUp() {
        conversionService = new PriceUnitConversionService();
    }

    @Test
    void quintalToKg_convertsCorrectly() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                18500.0, 20500.0, 22500.0, "QUINTAL", "KG"
        );
        assertTrue(result.isConversionApplied());
        assertEquals(185.0, result.getMinPrice());
        assertEquals(205.0, result.getModalPrice());
        assertEquals(225.0, result.getMaxPrice());
        assertEquals("QUINTAL", result.getSourceUnit());
        assertEquals("KG", result.getDisplayUnit());
        assertEquals(0.01, result.getConversionFactor(), 0.0001);
    }

    @Test
    void quintalToTonne_convertsCorrectly() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                18500.0, 20500.0, 22500.0, "QUINTAL", "TONNE"
        );
        assertTrue(result.isConversionApplied());
        assertEquals(185000.0, result.getMinPrice());
        assertEquals(205000.0, result.getModalPrice());
        assertEquals(225000.0, result.getMaxPrice());
    }

    @Test
    void kgToQuintal_convertsCorrectly() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                185.0, 205.0, 225.0, "KG", "QUINTAL"
        );
        assertTrue(result.isConversionApplied());
        assertEquals(18500.0, result.getMinPrice());
        assertEquals(20500.0, result.getModalPrice());
        assertEquals(22500.0, result.getMaxPrice());
    }

    @Test
    void kgToTonne_convertsCorrectly() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                205.0, 205.0, 205.0, "KG", "TONNE"
        );
        assertTrue(result.isConversionApplied());
        assertEquals(205000.0, result.getModalPrice());
    }

    @Test
    void tonneToQuintal_convertsCorrectly() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                205000.0, 20500.0, 205000.0, "TONNE", "QUINTAL"
        );
        assertTrue(result.isConversionApplied());
        assertEquals(2050.0, result.getModalPrice());
    }

    @Test
    void tonneToKg_convertsCorrectly() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                205000.0, 205000.0, 205000.0, "TONNE", "KG"
        );
        assertTrue(result.isConversionApplied());
        assertEquals(205.0, result.getModalPrice());
    }

    @Test
    void sameUnit_returnsUnchanged() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                20500.0, 20500.0, 20500.0, "QUINTAL", "QUINTAL"
        );
        assertFalse(result.isConversionApplied());
        assertEquals(20500.0, result.getModalPrice());
    }

    @Test
    void decimalValues_roundsHalfUp() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                1234.56, 2345.67, 3456.78, "QUINTAL", "KG"
        );
        assertTrue(result.isConversionApplied());
        assertEquals(12.35, result.getMinPrice());
        assertEquals(23.46, result.getModalPrice());
        assertEquals(34.57, result.getMaxPrice());
    }

    @Test
    void zeroValues_handlesSafely() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                0.0, 0.0, 0.0, "QUINTAL", "KG"
        );
        assertTrue(result.isConversionApplied());
        assertEquals(0.0, result.getModalPrice());
    }

    @Test
    void unsupportedUnit_returnsOriginalUnchanged() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                100.0, 200.0, 300.0, "BUNCH", "KG"
        );
        assertFalse(result.isConversionApplied());
        assertEquals(200.0, result.getModalPrice());
        assertEquals("BUNCH", result.getSourceUnit());
    }

    @Test
    void minModalMaxRelationship_preservedAfterConversion() {
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                15000.0, 20000.0, 25000.0, "QUINTAL", "KG"
        );
        assertTrue(result.getMinPrice() <= result.getModalPrice());
        assertTrue(result.getModalPrice() <= result.getMaxPrice());
    }
}
