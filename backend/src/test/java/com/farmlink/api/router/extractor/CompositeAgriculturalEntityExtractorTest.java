package com.farmlink.api.router.extractor;

import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompositeAgriculturalEntityExtractorTest {

    private CompositeAgriculturalEntityExtractor compositeExtractor;

    @BeforeEach
    void setUp() {
        CropEntityExtractor crop = new CropEntityExtractor(null);
        LocationEntityExtractor loc = new LocationEntityExtractor(null);
        MandiEntityExtractor mandi = new MandiEntityExtractor(null);
        QuantityEntityExtractor qty = new QuantityEntityExtractor();
        compositeExtractor = new CompositeAgriculturalEntityExtractor(crop, loc, mandi, qty);
    }

    @Test
    @DisplayName("Extracts composite agricultural entities from complex query")
    void testCompositeExtraction() {
        String query = "Selling 50 quintals of cotton at Warangal mandi in Telangana";
        ExtractedEntitiesDto entities = compositeExtractor.extractEntities(query);

        assertNotNull(entities);
        assertTrue(entities.crops().contains("cotton"));
        assertTrue(entities.locations().contains("warangal") || entities.locations().contains("telangana"));
        assertTrue(entities.mandis().contains("warangal mandi") || entities.mandis().contains("mandi"));
        assertTrue(entities.quantities().contains("50 quintals"));
        assertEquals("cotton", entities.attributes().get("primaryCrop"));
    }
}

