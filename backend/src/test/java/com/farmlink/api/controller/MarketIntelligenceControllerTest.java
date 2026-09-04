package com.farmlink.api.controller;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.MarketComparisonResponse;
import com.farmlink.api.dto.MarketIntelligenceSummaryResponse;
import com.farmlink.api.dto.MarketTrendResponse;
import com.farmlink.api.dto.PriceTrendResponse;
import com.farmlink.api.service.MarketIntelligenceService;
import com.farmlink.api.service.MarketTrendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketIntelligenceControllerTest {

    private MarketIntelligenceService intelligenceService;
    private MarketTrendService trendService;
    private MarketIntelligenceController controller;

    @BeforeEach
    void setUp() {
        intelligenceService = mock(MarketIntelligenceService.class);
        trendService = mock(MarketTrendService.class);
        controller = new MarketIntelligenceController(intelligenceService, trendService, null);
    }

    @Test
    void compareMarkets_returnsOkResponse_withCropId() {
        MarketComparisonResponse mockResponse = new MarketComparisonResponse(
                new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"),
                "2026-08-26", "INR", "QUINTAL", List.of(), null, null, 0.0, null
        );
        when(intelligenceService.compareMarkets(eq("crop-chilli"), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        ResponseEntity<MarketComparisonResponse> response = controller.compareMarkets("crop-chilli", null, "2026-08-26", null, null, null, null, "QUINTAL");

        assertNotNull(response.getBody());
        assertEquals("INR", response.getBody().getCurrency());
        assertEquals("QUINTAL", response.getBody().getUnit());
    }

    @Test
    void compareMarkets_supportsCropAlias() {
        MarketComparisonResponse mockResponse = new MarketComparisonResponse(
                new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"),
                "2026-08-26", "INR", "KG", List.of(), null, null, 0.0, null
        );
        when(intelligenceService.compareMarkets(eq("crop-chilli"), any(), any(), any(), eq("Andhra Pradesh"), eq("Chittoor"), eq("KG")))
                .thenReturn(mockResponse);

        ResponseEntity<MarketComparisonResponse> response = controller.compareMarkets(null, "crop-chilli", null, null, null, "Andhra Pradesh", "Chittoor", "KG");

        assertNotNull(response.getBody());
        assertEquals("KG", response.getBody().getUnit());
    }

    @Test
    void getSummary_returnsOkResponse() {
        MarketIntelligenceSummaryResponse mockSummary = new MarketIntelligenceSummaryResponse(
                2, 20000.0, 18000.0, 22000.0, 20000.0, "2026-08-01", "2026-08-26", 2000.0, 11.11, "INCREASING", "INR", "QUINTAL"
        );
        when(intelligenceService.getSummary(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockSummary);

        ResponseEntity<MarketIntelligenceSummaryResponse> response = controller.getSummary("crop-chilli", null, "mkt-guntur", null, null, null, null, "QUINTAL");

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getObservationCount());
        assertEquals("INCREASING", response.getBody().getTrendDirection());
    }

    @Test
    void getTrends_returnsOkResponse() {
        MarketTrendResponse mockTrend = new MarketTrendResponse();
        mockTrend.setCropId("crop-cotton");
        mockTrend.setCropName("Cotton");
        mockTrend.setTrendDirection("INSUFFICIENT_DATA");
        mockTrend.setUnit("KG");

        when(trendService.calculateTrend(eq("crop-cotton"), any(), any(), any(), any(), eq("KG")))
                .thenReturn(mockTrend);

        ResponseEntity<MarketTrendResponse> response = controller.getTrends(
                "crop-cotton", null, null, null, null, null, null, null, "KG", false, null
        );

        assertNotNull(response.getBody());
        assertEquals("INSUFFICIENT_DATA", response.getBody().getTrendDirection());
        assertEquals("KG", response.getBody().getUnit());
    }
}
