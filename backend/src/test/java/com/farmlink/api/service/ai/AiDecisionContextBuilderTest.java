package com.farmlink.api.service.ai;

import com.farmlink.api.dto.*;
import com.farmlink.api.dto.ai.*;
import com.farmlink.api.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiDecisionContextBuilderTest {

    @Mock
    private FarmService farmService;
    @Mock
    private CropMasterService cropMasterService;
    @Mock
    private MarketService marketService;
    @Mock
    private MarketPriceService marketPriceService;
    @Mock
    private DecisionSupportService decisionSupportService;
    @Mock
    private FarmEconomicsService farmEconomicsService;
    @Mock
    private MarketIntelligenceService marketIntelligenceService;

    private AiDecisionContextBuilder contextBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        contextBuilder = new AiDecisionContextBuilder(
                farmService, cropMasterService, marketService,
                marketPriceService, decisionSupportService, farmEconomicsService,
                marketIntelligenceService
        );
    }

    @Test
    void testBuildContext_MarketSelection() {
        String farmerUid = "farmer-uid-1";
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_SELECTION);
        request.setCropId("crop-1");
        request.setMarketIds(List.of("mkt-1", "mkt-2"));
        request.setQuantity(new BigDecimal("10.0"));

        CropResponse crop = new CropResponse();
        crop.setId("crop-1");
        crop.setName("Rice (Paddy)");
        when(cropMasterService.getCropById("crop-1")).thenReturn(crop);

        MarketResponse mkt1 = new MarketResponse();
        mkt1.setId("mkt-1");
        mkt1.setName("Warangal Mandi");
        when(marketService.getMarketById("mkt-1")).thenReturn(mkt1);

        MarketResponse mkt2 = new MarketResponse();
        mkt2.setId("mkt-2");
        mkt2.setName("Khammam Mandi");
        when(marketService.getMarketById("mkt-2")).thenReturn(mkt2);

        MarketSummaryResponse mktSummary1 = new MarketSummaryResponse();
        mktSummary1.setId("mkt-1");
        mktSummary1.setName("Warangal Mandi");
        mktSummary1.setType("APMC");
        mktSummary1.setState("Telangana");
        mktSummary1.setDistrict("Warangal");
        mktSummary1.setMandal("Warangal");

        MarketEvaluationResponse eval1 = new MarketEvaluationResponse();
        eval1.setStatus("SUCCESS");
        eval1.setSelectedPrice(new BigDecimal("2200.00"));
        eval1.setEstimatedNetRealization(new BigDecimal("21500.00"));
        eval1.setPriceDate("2026-09-02");
        eval1.setMarket(mktSummary1);
        when(decisionSupportService.evaluateMarket(any())).thenReturn(eval1);

        AiDecisionContext context = contextBuilder.buildContext(farmerUid, request);

        assertNotNull(context);
        assertEquals(farmerUid, context.getFarmerUid());
        assertEquals("Rice (Paddy)", context.getCrop().getName());
        assertEquals(2, context.getCandidateMarkets().size());
        assertTrue(context.isHasVerifiedPrice());
        verify(farmService).verifyFarmerRole(farmerUid);
    }
}
