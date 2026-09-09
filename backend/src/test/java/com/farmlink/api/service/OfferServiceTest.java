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
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 200.0, "KG", 2200.0, "QUINTAL", "Can pick up today");

        OfferResponse response = offerService.createOffer(BUYER_UID, request);

        assertNotNull(response);
        assertNotNull(response.getOfferId());
        assertEquals(LISTING_ID, response.getListingId());
        assertEquals(FARMER_UID, response.getFarmerId());
        assertEquals(BUYER_UID, response.getBuyerUid());
        assertEquals(200.0, response.getOfferedQuantity());
        assertEquals(2200.0, response.getOfferedPrice());
        assertEquals(2500.0, response.getAskingPriceReference());
        assertEquals(OfferStatus.PENDING.name(), response.getStatus());
        assertEquals(1, response.getRoundNumber());
        assertEquals(FARMER_UID, response.getCurrentResponderUid());
        assertEquals(1, response.getRounds().size());
        assertEquals("OFFER", response.getRounds().get(0).getAction());
    }

    @Test
    void createOffer_unauthenticated_throwsAccessDenied() {
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 100.0, "KG", 2000.0, "QUINTAL", null);

        assertThrows(AccessDeniedException.class, () ->
                offerService.createOffer(null, request)
        );
    }

    @Test
    void createOffer_ownerOfferingOnOwnListing_throwsIllegalArgument() {
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 100.0, "KG", 2000.0, "QUINTAL", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                offerService.createOffer(FARMER_UID, request)
        );
        assertTrue(ex.getMessage().contains("owners cannot make offers against their own listings"));
    }

    @Test
    void createOffer_quantityExceedsAvailable_throwsIllegalArgument() {
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 600.0, "KG", 2000.0, "QUINTAL", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                offerService.createOffer(BUYER_UID, request)
        );
        assertTrue(ex.getMessage().contains("cannot exceed available quantity"));
    }

    @Test
    void createOffer_negativePrice_throwsIllegalArgument() {
        CreateOfferRequest request = new CreateOfferRequest(LISTING_ID, 100.0, "KG", -500.0, "QUINTAL", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                offerService.createOffer(BUYER_UID, request)
        );
        assertTrue(ex.getMessage().contains("Offered price must be greater than zero"));
    }

    @Test
    void createOffer_inactiveListing_throwsIllegalArgument() {
        ProductListingResponse inactiveListing = new ProductListingResponse(
                "listing_inactive", FARMER_UID, "crop_tomato", "Tomato",
                500.0, 500.0, "KG", 2500.0, "QUINTAL",
                null, "Description", "GRADE_A", "2026-09-01", "2026-09-02", "PAUSED",
                "2026-09-08T00:00:00Z", "2026-09-08T00:00:00Z", null
        );
        when(listingService.getListingById(eq("listing_inactive"), any())).thenReturn(inactiveListing);

        CreateOfferRequest request = new CreateOfferRequest("listing_inactive", 100.0, "KG", 2000.0, "QUINTAL", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                offerService.createOffer(BUYER_UID, request)
        );
        assertTrue(ex.getMessage().contains("ACTIVE marketplace listings"));
    }

    @Test
    void fullNegotiationLifecycle_counter_and_accept_success() {
        // Step 1: Buyer creates initial offer (Round 1)
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 300.0, "KG", 2000.0, "QUINTAL", "Initial offer");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);
        assertEquals(OfferStatus.PENDING.name(), offer.getStatus());
        assertEquals(1, offer.getRoundNumber());

        // Step 2: Farmer counters offer (Round 2)
        CounterOfferRequest farmerCounterReq = new CounterOfferRequest(300.0, "KG", 2300.0, "QUINTAL", "Counter 2300");
        OfferResponse round2 = offerService.counterOffer(FARMER_UID, offer.getOfferId(), farmerCounterReq);
        assertEquals(OfferStatus.COUNTERED.name(), round2.getStatus());
        assertEquals(2, round2.getRoundNumber());
        assertEquals(BUYER_UID, round2.getCurrentResponderUid());
        assertEquals(2300.0, round2.getOfferedPrice());

        // Step 3: Buyer counters back (Round 3)
        CounterOfferRequest buyerCounterReq = new CounterOfferRequest(300.0, "KG", 2200.0, "QUINTAL", "Final try 2200");
        OfferResponse round3 = offerService.counterOffer(BUYER_UID, offer.getOfferId(), buyerCounterReq);
        assertEquals(OfferStatus.COUNTERED.name(), round3.getStatus());
        assertEquals(3, round3.getRoundNumber());
        assertEquals(FARMER_UID, round3.getCurrentResponderUid());
        assertEquals(2200.0, round3.getOfferedPrice());

        // Step 4: Farmer accepts buyer's counter-offer (Round 4)
        OfferResponse accepted = offerService.acceptOffer(FARMER_UID, offer.getOfferId());
        assertEquals(OfferStatus.ACCEPTED.name(), accepted.getStatus());
        assertEquals(4, accepted.getRoundNumber());
        assertEquals(2200.0, accepted.getAgreedPrice());
        assertEquals(300.0, accepted.getAgreedQuantity());
        assertEquals(4, accepted.getRounds().size());

        // Step 5: Verify history preservation
        List<OfferRoundResponse> history = offerService.getOfferHistory(offer.getOfferId(), FARMER_UID);
        assertEquals(4, history.size());
        assertEquals("OFFER", history.get(0).getAction());
        assertEquals("COUNTER", history.get(1).getAction());
        assertEquals("COUNTER", history.get(2).getAction());
        assertEquals("ACCEPT", history.get(3).getAction());
    }

    @Test
    void rejectOffer_success() {
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 200.0, "KG", 2000.0, "QUINTAL", "Low offer");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);

        OfferResponse rejected = offerService.rejectOffer(FARMER_UID, offer.getOfferId());
        assertEquals(OfferStatus.REJECTED.name(), rejected.getStatus());
        assertFalse(rejected.isCanAccept());
        assertFalse(rejected.isCanCounter());
    }

    @Test
    void cancelOffer_buyerCanCancelPendingOffer() {
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 200.0, "KG", 2000.0, "QUINTAL", "Quick offer");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);

        OfferResponse cancelled = offerService.cancelOffer(BUYER_UID, offer.getOfferId());
        assertEquals(OfferStatus.CANCELLED.name(), cancelled.getStatus());
    }

    @Test
    void cancelOffer_farmerCannotCancelOffer() {
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 200.0, "KG", 2000.0, "QUINTAL", "Quick offer");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                offerService.cancelOffer(FARMER_UID, offer.getOfferId())
        );
        assertTrue(ex.getMessage().contains("Only the offer initiator can cancel"));
    }

    @Test
    void counterOffer_exceedsMaxRounds_throwsIllegalArgument() {
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 200.0, "KG", 2000.0, "QUINTAL", "Round 1");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);

        String currentResponder = FARMER_UID;
        String offerId = offer.getOfferId();

        // Perform 9 counter-offers so roundNumber reaches 10
        for (int r = 2; r <= 10; r++) {
            String nextUser = currentResponder;
            CounterOfferRequest counterReq = new CounterOfferRequest(200.0, "KG", 2000.0 + r, "QUINTAL", "Round " + r);
            OfferResponse res = offerService.counterOffer(nextUser, offerId, counterReq);
            currentResponder = res.getCurrentResponderUid();
        }

        // At round 10, another counter attempt should fail with MAX rounds exception
        final String finalResponder = currentResponder;
        CounterOfferRequest counter11 = new CounterOfferRequest(200.0, "KG", 2500.0, "QUINTAL", "Round 11 attempt");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                offerService.counterOffer(finalResponder, offerId, counter11)
        );
        assertTrue(ex.getMessage().contains("Maximum negotiation rounds reached"));
    }

    @Test
    void unauthorizedUser_cannotAccessOfferDetail() {
        CreateOfferRequest createReq = new CreateOfferRequest(LISTING_ID, 200.0, "KG", 2000.0, "QUINTAL", "Private");
        OfferResponse offer = offerService.createOffer(BUYER_UID, createReq);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                offerService.getOfferById(offer.getOfferId(), OTHER_UID)
        );
        assertTrue(ex.getMessage().contains("Unauthorized access"));
    }
}
