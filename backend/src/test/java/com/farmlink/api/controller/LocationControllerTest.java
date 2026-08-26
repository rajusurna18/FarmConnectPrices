package com.farmlink.api.controller;

import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.service.MarketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationControllerTest {

    private MarketService marketService;
    private LocationController locationController;

    @BeforeEach
    void setUp() {
        marketService = mock(MarketService.class);
        locationController = new LocationController(marketService);
    }

    @Test
    void getStates_returnsDistinctSortedStates() {
        MarketSummaryResponse m1 = new MarketSummaryResponse("m1", "M1", "C1", "MANDI", "Telangana", "Warangal", "Enumamula", "ACTIVE", 2);
        MarketSummaryResponse m2 = new MarketSummaryResponse("m2", "M2", "C2", "MANDI", "Andhra Pradesh", "Guntur", "Guntur West", "ACTIVE", 3);
        when(marketService.getMarkets(null, null, null, null, null, null, 100))
                .thenReturn(List.of(m1, m2));

        ResponseEntity<List<String>> response = locationController.getStates();

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Andhra Pradesh", response.getBody().get(0));
        assertEquals("Telangana", response.getBody().get(1));
    }

    @Test
    void getDistricts_returnsDistrictsForState() {
        MarketSummaryResponse m1 = new MarketSummaryResponse("m1", "M1", "C1", "MANDI", "Telangana", "Warangal", "Enumamula", "ACTIVE", 2);
        when(marketService.getMarkets("Telangana", null, null, null, null, null, 100))
                .thenReturn(List.of(m1));

        ResponseEntity<List<String>> response = locationController.getDistricts("Telangana");

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Warangal", response.getBody().get(0));
    }

    @Test
    void getAreas_returnsEmpty_whenNoSubAreaExists() {
        MarketSummaryResponse m1 = new MarketSummaryResponse("m1", "M1", "C1", "MANDI", "Telangana", "Khammam", null, "ACTIVE", 2);
        when(marketService.getMarkets("Telangana", "Khammam", null, null, null, null, 100))
                .thenReturn(List.of(m1));

        ResponseEntity<List<String>> response = locationController.getAreas("Telangana", "Khammam");

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}
