package com.farmlink.api.controller;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.MarketComparisonResponse;
import com.farmlink.api.dto.MarketIntelligenceSummaryResponse;
import com.farmlink.api.dto.PriceTrendResponse;
import com.farmlink.api.service.MarketIntelligenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketIntelligenceControllerTest {

    private MarketIntelligenceService intelligenceService;
    private MarketIntelligenceController controller;

    @BeforeEach
    void setUp() {
        intelligenceService = mock(MarketIntelligenceService.class);
        controller = new MarketIntelligenceController(intelligenceService);
    }

    @Test
    void compareMarkets_returnsOkResponse() {
        MarketComparisonResponse mockResponse = new MarketComparisonResponse(
                new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"),
                "2026-08-26", "INR", "QUINTAL", List.of(), null, null, 0.0, null
        );
        when(intelligenceService.compareMarkets(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        ResponseEntity<MarketComparisonResponse> response = controller.compareMarkets("crop-chilli", "2026-08-26", null, null, null, null, "QUINTAL");

        assertNotNull(response.getBody());
        assertEquals("INR", response.getBody().getCurrency());
        assertEquals("QUINTAL", response.getBody().getUnit());
    }

    @Test
    void getSummary_returnsOkResponse() {
        MarketIntelligenceSummaryResponse mockSummary = new MarketIntelligenceSummaryResponse(
                2, 20000.0, 18000.0, 22000.0, 20000.0, "2026-08-01", "2026-08-26", 2000.0, 11.11, "INCREASING", "INR", "QUINTAL"
        );
        when(intelligenceService.getSummary(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockSummary);

        ResponseEntity<MarketIntelligenceSummaryResponse> response = controller.getSummary("crop-chilli", "mkt-guntur", null, null, null, null, "QUINTAL");

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getObservationCount());
        assertEquals("INCREASING", response.getBody().getTrendDirection());
    }
}
