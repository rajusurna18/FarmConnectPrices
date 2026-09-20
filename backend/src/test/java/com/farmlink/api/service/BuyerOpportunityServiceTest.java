package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BuyerOpportunityServiceTest {

    @Mock
    private ProductListingService listingService;

    @Mock
    private ProfileService profileService;

    private BuyerOpportunityMatchingEngine matchingEngine;
    private BuyerOpportunityService opportunityService;

    private static final String FARMER_UID = "farmer_100";
    private static final String BUYER_UID = "buyer_200";
    private static final String OTHER_FARMER_UID = "farmer_999";
    private static final String LISTING_ID = "listing_tomato_1";

    private ProductListingResponse activeListing;
    private ProductListingResponse inactiveListing;

    @BeforeEach
    void setUp() {
        matchingEngine = new BuyerOpportunityMatchingEngine();
        opportunityService = new BuyerOpportunityService(null, listingService, profileService, matchingEngine, null);

        activeListing = new ProductListingResponse(
                LISTING_ID,
                FARMER_UID,
                "crop_tomato",
                "Tomato",
                1000.0,
                1000.0,
                "KG",
                30.0,
                "KG",
                new LocationDto("Telangana", "Rangareddy", "Rajendranagar", "Village Hub", "500030"),
                "Fresh organic tomatoes",
                "GRADE_A",
                "2026-09-10",
                "2026-09-11",
                ProductListingStatus.ACTIVE.name(),
                "2026-09-15T00:00:00Z",
                "2026-09-15T00:00:00Z",
                null
        );

        inactiveListing = new ProductListingResponse(
                "listing_inactive",
                FARMER_UID,
                "crop_tomato",
                "Tomato",
                500.0,
                0.0,
                "KG",
                30.0,
                "KG",
                new LocationDto("Telangana", "Rangareddy", "Rajendranagar", "Village Hub", "500030"),
                "Sold out tomatoes",
                "GRADE_A",
                "2026-09-10",
                "2026-09-11",
                ProductListingStatus.SOLD_OUT.name(),
                "2026-09-15T00:00:00Z",
                "2026-09-15T00:00:00Z",
                null
        );
    }

    @Test
    void getBuyerOpportunities_unauthenticated_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () ->
                opportunityService.getBuyerOpportunities(null, LISTING_ID, 0, 10)
        );
    }

    @Test
    void getBuyerOpportunities_nonOwner_throwsAccessDeniedException() {
        when(listingService.getListingById(eq(LISTING_ID), eq(OTHER_FARMER_UID))).thenReturn(activeListing);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                opportunityService.getBuyerOpportunities(OTHER_FARMER_UID, LISTING_ID, 0, 10)
        );
        assertTrue(ex.getMessage().contains("Caller must be the listing owner"));
    }

    @Test
    void getBuyerOpportunities_inactiveListing_throwsIllegalArgumentException() {
        when(listingService.getListingById(eq("listing_inactive"), eq(FARMER_UID))).thenReturn(inactiveListing);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                opportunityService.getBuyerOpportunities(FARMER_UID, "listing_inactive", 0, 10)
        );
        assertTrue(ex.getMessage().contains("only available for ACTIVE listings"));
    }

    @Test
    void getBuyerOpportunities_listingNotFound_throwsNoSuchElementException() {
        when(listingService.getListingById(eq("missing"), eq(FARMER_UID))).thenReturn(null);

        assertThrows(NoSuchElementException.class, () ->
                opportunityService.getBuyerOpportunities(FARMER_UID, "missing", 0, 10)
        );
    }

    @Test
    void getBuyerOpportunities_noBuyerOffers_returnsNoMatchesState() {
        when(listingService.getListingById(eq(LISTING_ID), eq(FARMER_UID))).thenReturn(activeListing);

        BuyerOpportunityPageResponse response = opportunityService.getBuyerOpportunities(FARMER_UID, LISTING_ID, 0, 10);

        assertNotNull(response);
        assertEquals(0, response.getItems().size());
        assertEquals("NO_MATCHES", response.getStatusState());
        assertFalse(response.isHasNext());
    }

    @Test
    void matchingEngine_evaluatesExactCropAndQuantityAndLocation_correctScore() {
        Offer buyerOffer = new Offer(
                "offer_123",
                "listing_other",
                "farmer_222",
                BUYER_UID,
                "MEDIATOR_BUYER",
                "crop_tomato",
                "Tomato",
                900.0,
                "KG",
                new BigDecimal("28.00"),
                "KG",
                new BigDecimal("30.00"),
                "Bulk purchase",
                "PENDING",
                1,
                FARMER_UID,
                null,
                "2026-09-18T10:00:00Z",
                "2026-09-18T10:00:00Z",
                null
        );

        LocationDto buyerLocation = new LocationDto("Telangana", "Rangareddy", "Kondapur", "Hub", "500084");

        BuyerOpportunityDto dto = matchingEngine.evaluateOpportunity(activeListing, buyerOffer, buyerLocation);

        assertNotNull(dto);
        assertEquals("Mediator / Buyer", dto.getBuyerRoleDisplay());
        assertEquals("Rangareddy, Telangana", dto.getAnonymizedLocation());
        assertEquals("Tomato", dto.getCropName());
        // Crop: 40%, Qty (900/1000 = 0.9 => 80-150%): 25%, Location (District match Rangareddy): 15%, Quality (GRADE_A present): 15% => 95.00
        assertEquals(new BigDecimal("95.00"), dto.getRelevanceScore());
        assertEquals(ConfidenceLevel.HIGH, dto.getConfidenceLevel());
        assertTrue(dto.getMatchReasons().contains(MatchReasonCode.CROP_MATCH));
        assertTrue(dto.getMatchReasons().contains(MatchReasonCode.QUANTITY_MATCH));
        assertTrue(dto.getMatchReasons().contains(MatchReasonCode.SAME_DISTRICT));
        assertTrue(dto.getMatchReasons().contains(MatchReasonCode.QUALITY_MATCH));
    }

    @Test
    void matchingEngine_incompatibleUnits_returnsUnknownQuantityReason() {
        Offer buyerOffer = new Offer(
                "offer_124", "listing_other", "farmer_222", BUYER_UID, "CUSTOMER",
                "crop_tomato", "Tomato", 10.0, "CRATES",
                new BigDecimal("28.00"), "CRATES", new BigDecimal("30.00"), "Message",
                "PENDING", 1, FARMER_UID, null, "2026-09-18T10:00:00Z", "2026-09-18T10:00:00Z", null
        );

        BuyerOpportunityDto dto = matchingEngine.evaluateOpportunity(activeListing, buyerOffer, null);

        assertNotNull(dto);
        assertTrue(dto.getMatchReasons().contains(MatchReasonCode.UNKNOWN_QUANTITY));
        // Crop: 40, Qty: 0, Location: 0, Quality: 15 (grade present) => 55.00
        assertEquals(new BigDecimal("55.00"), dto.getRelevanceScore());
        assertEquals(ConfidenceLevel.MEDIUM, dto.getConfidenceLevel());
    }

    @Test
    void matchingEngine_convertibleUnitsQuintalToKg_calculatesCorrectQuantityMatch() {
        // activeListing quantity is 1000.0 KG (which is 10 QUINTALS)
        // Buyer offer asks for 10.0 QUINTAL => 10.0 * 100 = 1000.0 KG => ratio 1.0 => 25.0 points!
        Offer buyerOffer = new Offer(
                "offer_125", "listing_other", "farmer_222", BUYER_UID, "CUSTOMER",
                "crop_tomato", "Tomato", 10.0, "QUINTAL",
                new BigDecimal("2800.00"), "QUINTAL", new BigDecimal("3000.00"), "Message",
                "PENDING", 1, FARMER_UID, null, "2026-09-18T10:00:00Z", "2026-09-18T10:00:00Z", null
        );

        LocationDto location = new LocationDto("Telangana", "Rangareddy", "Rajendranagar", "Village Hub", "500030");
        BuyerOpportunityDto dto = matchingEngine.evaluateOpportunity(activeListing, buyerOffer, location);

        assertNotNull(dto);
        assertTrue(dto.getMatchReasons().contains(MatchReasonCode.QUANTITY_MATCH));
        assertTrue(dto.getMatchReasons().contains(MatchReasonCode.SAME_DISTRICT));
        // Crop: 40 + Qty: 25 + Loc: 15 (District match) + Qual: 15 = 95.00
        assertEquals(new BigDecimal("95.00"), dto.getRelevanceScore());
    }

    @Test
    void matchingEngine_scoreFormulaCases_returnsExactNumericWeights() {
        // Product listing without quality grade for baseline tests
        ProductListingResponse baseListing = new ProductListingResponse(
                LISTING_ID, FARMER_UID, "crop_tomato", "Tomato", 1000.0, 1000.0, "KG",
                30.0, "KG", null, "Desc", null, "2026-09-10", "2026-09-11",
                ProductListingStatus.ACTIVE.name(), "2026-09-15T00:00:00Z", "2026-09-15T00:00:00Z", null
        );

        LocationDto locA = new LocationDto("Telangana", "Rangareddy", "Rajendranagar", "Hub", "500030");
        MarketPriceReferenceDto refMarket = new MarketPriceReferenceDto(30.0, 25.0, 35.0, "KG", "Rajendranagar APMC", "2026-09-18");
        ProductListingResponse listingA = new ProductListingResponse(LISTING_ID, FARMER_UID, "crop_tomato", "Tomato", 1000.0, 1000.0, "KG", 30.0, "KG", locA, "Desc", "GRADE_A", "2026-09-10", "2026-09-11", ProductListingStatus.ACTIVE.name(), "2026-09-15T00:00:00Z", "2026-09-15T00:00:00Z", refMarket);

        // Case A: 100% Crop (40) + Qty (25) + Loc (20) + Qual (15) => 100.00
        Offer offerA = new Offer("o1", "l1", "f1", BUYER_UID, "CUSTOMER", "crop_tomato", "Tomato", 1000.0, "KG", new BigDecimal("30"), "KG", new BigDecimal("30"), "msg", "PENDING", 1, FARMER_UID, null, "2026-09-18", "2026-09-18", null);
        assertEquals(new BigDecimal("100.00"), matchingEngine.evaluateOpportunity(listingA, offerA, locA).getRelevanceScore());

        // Case B: Crop match only (40) + unknown qty (0) + unknown loc (0) + unknown qual (0) => 40.00
        Offer offerB = new Offer("o2", "l1", "f1", BUYER_UID, "CUSTOMER", "crop_tomato", "Tomato", 1.0, "KG", new BigDecimal("30"), "KG", new BigDecimal("30"), "msg", "PENDING", 1, FARMER_UID, null, "2026-09-18", "2026-09-18", null);
        assertEquals(new BigDecimal("40.00"), matchingEngine.evaluateOpportunity(baseListing, offerB, null).getRelevanceScore());

        // Case C: Crop match (40) + Qty match (25) + unknown loc (0) + unknown qual (0) => 65.00
        Offer offerC = new Offer("o3", "l1", "f1", BUYER_UID, "CUSTOMER", "crop_tomato", "Tomato", 1000.0, "KG", new BigDecimal("30"), "KG", new BigDecimal("30"), "msg", "PENDING", 1, FARMER_UID, null, "2026-09-18", "2026-09-18", null);
        assertEquals(new BigDecimal("65.00"), matchingEngine.evaluateOpportunity(baseListing, offerC, null).getRelevanceScore());

        // Case D: Crop match (40) + unknown qty (0) + Loc match (20) + unknown qual (0) => 60.00
        ProductListingResponse listingLoc = new ProductListingResponse(LISTING_ID, FARMER_UID, "crop_tomato", "Tomato", 1000.0, 1000.0, "KG", 30.0, "KG", locA, "Desc", null, "2026-09-10", "2026-09-11", ProductListingStatus.ACTIVE.name(), "2026-09-15T00:00:00Z", "2026-09-15T00:00:00Z", refMarket);
        Offer offerD = new Offer("o4", "l1", "f1", BUYER_UID, "CUSTOMER", "crop_tomato", "Tomato", 1.0, "KG", new BigDecimal("30"), "KG", new BigDecimal("30"), "msg", "PENDING", 1, FARMER_UID, null, "2026-09-18", "2026-09-18", null);
        assertEquals(new BigDecimal("60.00"), matchingEngine.evaluateOpportunity(listingLoc, offerD, locA).getRelevanceScore());

        // Case E: Crop match (40) + unknown qty (0) + unknown loc (0) + Qual match (15) => 55.00
        assertEquals(new BigDecimal("55.00"), matchingEngine.evaluateOpportunity(listingA, offerB, null).getRelevanceScore());
    }

    @Test
    void privacyAudit_dtoDoesNotExposeSensitiveBuyerIdentity() {
        Offer buyerOffer = new Offer(
                "offer_privacy", "listing_other", "farmer_222", BUYER_UID, "CUSTOMER",
                "crop_tomato", "Tomato", 1000.0, "KG",
                new BigDecimal("30.00"), "KG", new BigDecimal("30.00"), "Private message",
                "PENDING", 1, FARMER_UID, null, "2026-09-18T10:00:00Z", "2026-09-18T10:00:00Z", null
        );

        BuyerOpportunityDto dto = matchingEngine.evaluateOpportunity(activeListing, buyerOffer, new LocationDto("Telangana", "Rangareddy", "Hub", "V", "500030"));

        assertNotNull(dto);
        assertFalse(dto.getOpportunityId().contains(BUYER_UID));
        assertFalse(dto.getAnonymizedLocation().contains(BUYER_UID));
        assertEquals("Customer", dto.getBuyerRoleDisplay());
        assertEquals("Rangareddy, Telangana", dto.getAnonymizedLocation());
    }
}
