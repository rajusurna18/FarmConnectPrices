package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.ProductListingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductListingServiceTest {

    @Mock
    private CropMasterService cropMasterService;

    @Mock
    private MarketPriceService marketPriceService;

    private ProductListingService listingService;

    private static final String FARMER_UID = "farmer_uid_123";
    private static final String OTHER_UID = "other_uid_456";

    @BeforeEach
    void setUp() {
        listingService = new ProductListingService(null, cropMasterService, marketPriceService, null);
        org.mockito.Mockito.lenient().when(cropMasterService.getCropById("crop_tomato"))
                .thenReturn(new CropResponse("crop_tomato", "Tomato", "VEGETABLE", "Solanum lycopersicum", "ACTIVE"));
    }



    @Test
    void createListing_validRequest_success() {
        CreateListingRequest request = new CreateListingRequest();
        request.setCropId("crop_tomato");
        request.setQuantity(500.0);
        request.setUnit("KG");
        request.setAskingPrice(2500.0);
        request.setPriceUnit("QUINTAL");
        request.setDescription("Fresh farm tomatoes");

        ProductListingResponse response = listingService.createListing(FARMER_UID, request);

        assertNotNull(response);
        assertNotNull(response.getListingId());
        assertEquals("Tomato", response.getCropName());
        assertEquals(500.0, response.getQuantity());
        assertEquals(500.0, response.getAvailableQuantity());
        assertEquals(2500.0, response.getAskingPrice());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(FARMER_UID, response.getOwnerUid());
    }

    @Test
    void createListing_invalidCrop_throwsException() {
        when(cropMasterService.getCropById("invalid_crop")).thenReturn(null);

        CreateListingRequest request = new CreateListingRequest();
        request.setCropId("invalid_crop");
        request.setQuantity(100.0);
        request.setAskingPrice(1000.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                listingService.createListing(FARMER_UID, request)
        );
        assertTrue(ex.getMessage().contains("Invalid crop ID"));
    }

    @Test
    void createListing_zeroQuantity_throwsException() {
        CreateListingRequest request = new CreateListingRequest();
        request.setCropId("crop_tomato");
        request.setQuantity(0.0);
        request.setAskingPrice(1000.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                listingService.createListing(FARMER_UID, request)
        );
        assertTrue(ex.getMessage().contains("Quantity must be greater than zero"));
    }

    @Test
    void createListing_negativePrice_throwsException() {
        CreateListingRequest request = new CreateListingRequest();
        request.setCropId("crop_tomato");
        request.setQuantity(100.0);
        request.setAskingPrice(-500.0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                listingService.createListing(FARMER_UID, request)
        );
        assertTrue(ex.getMessage().contains("Asking price must be greater than zero"));
    }

    @Test
    void updateListing_ownedListing_success() {
        CreateListingRequest createReq = new CreateListingRequest();
        createReq.setCropId("crop_tomato");
        createReq.setQuantity(500.0);
        createReq.setAskingPrice(2500.0);

        ProductListingResponse created = listingService.createListing(FARMER_UID, createReq);

        UpdateListingRequest updateReq = new UpdateListingRequest();
        updateReq.setAskingPrice(2800.0);
        updateReq.setAvailableQuantity(400.0);

        ProductListingResponse updated = listingService.updateListing(FARMER_UID, created.getListingId(), updateReq);

        assertEquals(2800.0, updated.getAskingPrice());
        assertEquals(400.0, updated.getAvailableQuantity());
    }

    @Test
    void updateListing_otherUser_throwsAccessDenied() {
        CreateListingRequest createReq = new CreateListingRequest();
        createReq.setCropId("crop_tomato");
        createReq.setQuantity(500.0);
        createReq.setAskingPrice(2500.0);

        ProductListingResponse created = listingService.createListing(FARMER_UID, createReq);

        UpdateListingRequest updateReq = new UpdateListingRequest();
        updateReq.setAskingPrice(3000.0);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                listingService.updateListing(OTHER_UID, created.getListingId(), updateReq)
        );
        assertTrue(ex.getMessage().contains("only modify your own"));
    }

    @Test
    void updateListingStatus_lifecycleTransitions_success() {
        CreateListingRequest createReq = new CreateListingRequest();
        createReq.setCropId("crop_tomato");
        createReq.setQuantity(500.0);
        createReq.setAskingPrice(2500.0);

        ProductListingResponse created = listingService.createListing(FARMER_UID, createReq);

        // Pause listing
        ProductListingResponse paused = listingService.updateListingStatus(
                FARMER_UID, created.getListingId(), new UpdateListingStatusRequest("PAUSED")
        );
        assertEquals("PAUSED", paused.getStatus());

        // Reactivate listing
        ProductListingResponse active = listingService.updateListingStatus(
                FARMER_UID, created.getListingId(), new UpdateListingStatusRequest("ACTIVE")
        );
        assertEquals("ACTIVE", active.getStatus());
    }

    @Test
    void browsePublicListings_filtersActiveListingsOnly() {
        CreateListingRequest activeReq = new CreateListingRequest();
        activeReq.setCropId("crop_tomato");
        activeReq.setQuantity(500.0);
        activeReq.setAskingPrice(2500.0);
        activeReq.setStatus("ACTIVE");
        listingService.createListing(FARMER_UID, activeReq);

        CreateListingRequest draftReq = new CreateListingRequest();
        draftReq.setCropId("crop_tomato");
        draftReq.setQuantity(100.0);
        draftReq.setAskingPrice(1200.0);
        draftReq.setStatus("DRAFT");
        listingService.createListing(FARMER_UID, draftReq);

        ListingPageResponse page = listingService.browsePublicListings("crop_tomato", null, null, null, null, "newest", 0, 10);

        assertEquals(1, page.getItems().size());
        assertEquals("ACTIVE", page.getItems().get(0).getStatus());
    }
}
