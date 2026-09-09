package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.OfferStatus;
import com.farmlink.api.model.ProductListingQualityGrade;
import com.farmlink.api.model.ProductListingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OfferServiceTest {

    @Mock
    private ProductListingService listingService;

    @Mock
    private ProfileService profileService;

    private OfferService offerService;

    private static final String FARMER_UID = "farmer_uid_100";
    private static final String BUYER_UID = "buyer_uid_200";
    private static final String OTHER_UID = "other_uid_300";
    private static final String LISTING_ID = "listing_test_1";

    private ProductListingResponse activeListing;

    @BeforeEach
    void setUp() {
        offerService = new OfferService(null, listingService, profileService, null);

        activeListing = new ProductListingResponse(
                LISTING_ID,
                FARMER_UID,
                "crop_tomato",
                "Tomato",
                500.0,
                500.0,
                "KG",
                2500.0,
                "QUINTAL",
                new LocationDto("Telangana", "Warangal", "Enumamula", "Village"),
                "Fresh tomatoes",
                ProductListingQualityGrade.UNSPECIFIED.name(),
                "2026-09-01",
                "2026-09-02",
                ProductListingStatus.ACTIVE.name(),
                "2026-09-08T00:00:00Z",
                "2026-09-08T00:00:00Z",
                null
        );

        org.mockito.Mockito.lenient().when(profileService.getProfile(eq(BUYER_UID), any(), eq(false), any()))
                .thenReturn(new ProfileResponse(BUYER_UID, "Buyer User", "buyer@test.com", true, "CUSTOMER", "Customer", "ACTIVE", null, null, null, null, true));

        org.mockito.Mockito.lenient().when(profileService.getProfile(eq(FARMER_UID), any(), eq(false), any()))
                .thenReturn(new ProfileResponse(FARMER_UID, "Farmer User", "farmer@test.com", true, "FARMER", "Farmer", "ACTIVE", null, null, null, null, true));

        org.mockito.Mockito.lenient().when(listingService.getListingById(eq(LISTING_ID), any()))
                .thenReturn(activeListing);
    }

    @Test
    void createOffer_validRequest_success() {
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 200.0, "KG", new BigDecimal("2200.00"), "QUINTAL", "Can pick up today");

        OfferResponse response = offerService.createOffer(BUYER_UID, request);

        assertNotNull(response);
        assertNotNull(response.getOfferId());
        assertEquals(LISTING_ID, response.getListingId());
        assertEquals(FARMER_UID, response.getFarmerId());
        assertEquals(BUYER_UID, response.getBuyerUid());
        assertEquals(200.0, response.getOfferedQuantity());
        assertEquals(new BigDecimal("2200.00"), response.getOfferedPrice());
        assertEquals(new BigDecimal("2500.0"), response.getAskingPriceReference());
        assertEquals(OfferStatus.PENDING.name(), response.getStatus());
        assertEquals(1, response.getRoundNumber());
        assertEquals(FARMER_UID, response.getCurrentResponderUid());
        assertEquals(1, response.getRounds().size());
        assertEquals("OFFER", response.getRounds().get(0).getAction());
    }

    @Test
    void createOffer_unauthenticated_throwsAccessDenied() {
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 100.0, "KG", new BigDecimal("2000.00"), "QUINTAL", null);

        assertThrows(AccessDeniedException.class, () ->
                offerService.createOffer(null, request)
        );
    }

    @Test
    void createOffer_ownerOfferingOnOwnListing_throwsIllegalArgument() {
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 100.0, "KG", new BigDecimal("2000.00"), "QUINTAL", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                offerService.createOffer(FARMER_UID, request)
        );
        assertTrue(ex.getMessage().contains("owners cannot make offers against their own listings"));
    }

    @Test
    void createOffer_quantityExceedsAvailable_throwsIllegalArgument() {
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 600.0, "KG", new BigDecimal("2000.00"), "QUINTAL", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                offerService.createOffer(BUYER_UID, request)
        );
        assertTrue(ex.getMessage().contains("cannot exceed available quantity"));
    }

    @Test
    void createOffer_negativePrice_throwsIllegalArgument() {
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 100.0, "KG", new BigDecimal("-500.00"), "QUINTAL", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                offerService.createOffer(BUYER_UID, request)
        );
        assertTrue(ex.getMessage().contains("Offered price must be greater than zero"));
    }

    @Test
    void fullNegotiationLifecycle_counter_and_accept_success() {
        // Step 1: Buyer creates initial offer (Round 1)
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 300.0, "KG", new BigDecimal("2000.00"), "QUINTAL", "Initial offer");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);
        assertEquals(OfferStatus.PENDING.name(), offer.getStatus());
        assertEquals(1, offer.getRoundNumber());

        // Step 2: Farmer counters offer (Round 2)
        CounterOfferRequest farmerCounterReq = new CounterOfferRequest(300.0, "KG", new BigDecimal("2300.00"), "QUINTAL", "Counter 2300");
        OfferResponse round2 = offerService.counterOffer(FARMER_UID, offer.getOfferId(), farmerCounterReq);
        assertEquals(OfferStatus.COUNTERED.name(), round2.getStatus());
        assertEquals(2, round2.getRoundNumber());
        assertEquals(BUYER_UID, round2.getCurrentResponderUid());
        assertEquals(new BigDecimal("2300.00"), round2.getOfferedPrice());

        // Step 3: Buyer counters back (Round 3)
        CounterOfferRequest buyerCounterReq = new CounterOfferRequest(300.0, "KG", new BigDecimal("2200.00"), "QUINTAL", "Final try 2200");
        OfferResponse round3 = offerService.counterOffer(BUYER_UID, offer.getOfferId(), buyerCounterReq);
        assertEquals(OfferStatus.COUNTERED.name(), round3.getStatus());
        assertEquals(3, round3.getRoundNumber());
        assertEquals(FARMER_UID, round3.getCurrentResponderUid());

        // Step 4: Farmer accepts buyer's counter-offer (Round 4)
        OfferResponse accepted = offerService.acceptOffer(FARMER_UID, offer.getOfferId());
        assertEquals(OfferStatus.ACCEPTED.name(), accepted.getStatus());
        assertEquals(4, accepted.getRoundNumber());
        assertEquals(new BigDecimal("2200.00"), accepted.getAgreedPrice());
        assertEquals(300.0, accepted.getAgreedQuantity());
        assertEquals(4, accepted.getRounds().size());
    }

    @Test
    void wrongResponder_attemptingAction_throwsIllegalState() {
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 300.0, "KG", new BigDecimal("2000.00"), "QUINTAL", "Initial offer");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);

        // Buyer attempts to accept their own offer (when farmer is expected responder)
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                offerService.acceptOffer(BUYER_UID, offer.getOfferId())
        );
        assertTrue(ex.getMessage().contains("not your turn"));
    }

    @Test
    void cancelOffer_buyerCannotCancelWhenWaitingForFarmerResponseToCounter() {
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 200.0, "KG", new BigDecimal("2000.00"), "QUINTAL", "Initial");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);

        // Farmer counters back to Buyer
        OfferResponse r2 = offerService.counterOffer(FARMER_UID, offer.getOfferId(), new CounterOfferRequest(200.0, "KG", new BigDecimal("2300.00"), "QUINTAL", "Farmer Counter"));

        // Buyer counters back to Farmer
        OfferResponse r3 = offerService.counterOffer(BUYER_UID, offer.getOfferId(), new CounterOfferRequest(200.0, "KG", new BigDecimal("2200.00"), "QUINTAL", "Buyer Counter"));

        // Buyer now tries to cancel while waiting for farmer response -> Should fail!
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                offerService.cancelOffer(BUYER_UID, offer.getOfferId())
        );
        assertTrue(ex.getMessage().contains("Cannot cancel offer while waiting for the farmer"));
    }

    @Test
    void rejectOffer_success() {
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 200.0, "KG", new BigDecimal("2000.00"), "QUINTAL", "Low offer");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);

        OfferResponse rejected = offerService.rejectOffer(FARMER_UID, offer.getOfferId());
        assertEquals(OfferStatus.REJECTED.name(), rejected.getStatus());
        assertFalse(rejected.isCanAccept());
        assertFalse(rejected.isCanCounter());
    }

    @Test
    void counterOffer_exceedsMaxRounds_throwsIllegalArgument() {
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 200.0, "KG", new BigDecimal("2000.00"), "QUINTAL", "Round 1");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);

        String currentResponder = FARMER_UID;
        String offerId = offer.getOfferId();

        for (int r = 2; r <= 10; r++) {
            String nextUser = currentResponder;
            CounterOfferRequest counterReq = new CounterOfferRequest(200.0, "KG", new BigDecimal(2000 + r), "QUINTAL", "Round " + r);
            OfferResponse res = offerService.counterOffer(nextUser, offerId, counterReq);
            currentResponder = res.getCurrentResponderUid();
        }

        final String finalResponder = currentResponder;
        CounterOfferRequest counter11 = new CounterOfferRequest(200.0, "KG", new BigDecimal("2500.00"), "QUINTAL", "Round 11 attempt");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                offerService.counterOffer(finalResponder, offerId, counter11)
        );
        assertTrue(ex.getMessage().contains("Maximum negotiation rounds reached"));
    }
}
