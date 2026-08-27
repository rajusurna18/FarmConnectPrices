package com.farmlink.api.service;

import com.farmlink.api.client.DataGovMandiClient;
import com.farmlink.api.config.DataGovMandiProperties;
import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.IngestionResultDto;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.dto.external.DataGovMandiRecordDto;
import com.farmlink.api.dto.external.DataGovMandiResponseDto;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataGovIngestionServiceTest {

    private DataGovMandiClient client;
    private MandiMappingService mappingService;
    private DataGovMandiProperties properties;
    private Firestore firestore;
    private DataGovIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        client = mock(DataGovMandiClient.class);
        mappingService = new MandiMappingService(mock(MarketService.class), mock(CropMasterService.class));
        properties = new DataGovMandiProperties();
        properties.setApiKey("test-api-key");
        properties.setPageSize(2);
        properties.setMaxPages(5);
        properties.setMode("INCREMENTAL");
        firestore = mock(Firestore.class);

        ingestionService = new DataGovIngestionService(client, mappingService, properties, firestore);
    }

    @Test
    void ingestMandiPrices_returnsFailure_whenClientReturnsEmpty() {
        when(client.fetchMandiPrices(anyString(), anyInt(), anyInt())).thenReturn(Optional.empty());

        IngestionResultDto result = ingestionService.ingestMandiPrices(100);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(0, result.getRecordsFetched());
    }

    @Test
    void ingestMandiPrices_ingestsSinglePageSuccessfully() {
        DataGovMandiResponseDto responseDto = new DataGovMandiResponseDto();
        DataGovMandiRecordDto validRecord = new DataGovMandiRecordDto(
                "Andhra Pradesh", "Guntur", "Guntur Mandi", "Chilli Red", "Sannam",
                "26/08/2026", "18000", "22000", "20000"
        );
        responseDto.setRecords(List.of(validRecord));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(responseDto));
        when(client.fetchMandiPrices(anyString(), anyInt(), eq(1))).thenReturn(Optional.of(new DataGovMandiResponseDto()));

        IngestionResultDto result = ingestionService.ingestMandiPrices(100);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordsFetched());
        assertEquals(1, result.getRecordsAccepted());
        assertEquals(0, result.getRecordsRejected());
    }

    @Test
    void ingestMandiPrices_handlesMultiplePagesAndEmptyPageTermination() {
        DataGovMandiResponseDto page1 = new DataGovMandiResponseDto();
        page1.setRecords(List.of(
                new DataGovMandiRecordDto("Punjab", "Sangrur", "Sangrur Mandi", "Wheat", "Kalyan", "26/08/2026", "2000", "2400", "2200"),
                new DataGovMandiRecordDto("Punjab", "Ludhiana", "Ludhiana Mandi", "Rice", "Basmati", "26/08/2026", "3000", "3600", "3300")
        ));

        DataGovMandiResponseDto page2 = new DataGovMandiResponseDto();
        page2.setRecords(List.of(
                new DataGovMandiRecordDto("Haryana", "Karnal", "Karnal Mandi", "Paddy", "Common", "26/08/2026", "1900", "2100", "2000")
        ));

        when(client.fetchMandiPrices(anyString(), eq(2), eq(0))).thenReturn(Optional.of(page1));
        when(client.fetchMandiPrices(anyString(), eq(2), eq(2))).thenReturn(Optional.of(page2));
        when(client.fetchMandiPrices(anyString(), eq(2), eq(3))).thenReturn(Optional.of(new DataGovMandiResponseDto()));

        IngestionResultDto result = ingestionService.ingestMandiPrices("INCREMENTAL", null, 2, 5, 30, 0);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getRecordsFetched());
        assertEquals(2, result.getPagesFetched());
    }

    @Test
    void ingestMandiPrices_handlesMaxPagesTermination() {
        DataGovMandiResponseDto page = new DataGovMandiResponseDto();
        page.setRecords(List.of(
                new DataGovMandiRecordDto("Gujarat", "Rajkot", "Rajkot APMC", "Cotton", "Medium", "26/08/2026", "6000", "7000", "6500"),
                new DataGovMandiRecordDto("Gujarat", "Amreli", "Amreli APMC", "Groundnut", "Bold", "26/08/2026", "5500", "6500", "6000")
        ));

        when(client.fetchMandiPrices(anyString(), anyInt(), anyInt())).thenReturn(Optional.of(page));

        IngestionResultDto result = ingestionService.ingestMandiPrices("INCREMENTAL", null, 2, 2, 30, 0);

        assertNotNull(result);
        assertEquals(4, result.getRecordsFetched());
        assertEquals(2, result.getPagesFetched());
    }

    @Test
    void ingestMandiPrices_preservesObservedEntities_whenUnmappedInCanonicalMaster() {
        DataGovMandiResponseDto responseDto = new DataGovMandiResponseDto();
        DataGovMandiRecordDto unmappedRecord = new DataGovMandiRecordDto(
                "Odisha", "Kalahandi", "Bhawanipatna Mandi", "Dragon Fruit", "Red Flesh",
                "26/08/2026", "8000", "12000", "10000"
        );
        responseDto.setRecords(List.of(unmappedRecord));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(responseDto));

        IngestionResultDto result = ingestionService.ingestMandiPrices(100);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordsFetched());
        assertEquals(1, result.getRecordsUnmappedMarkets());
        assertEquals(1, result.getRecordsUnmappedCrops());
        assertEquals(1, result.getRecordsAccepted());
    }

    @Test
    void ingestMandiPrices_rejectsInvalidPriceRecords() {
        DataGovMandiResponseDto responseDto = new DataGovMandiResponseDto();
        // Invalid: min > modal or negative price
        DataGovMandiRecordDto invalidRecord = new DataGovMandiRecordDto(
                "Karnataka", "Kolar", "Kolar APMC", "Tomato", "Local",
                "26/08/2026", "5000", "3000", "4000"
        );
        responseDto.setRecords(List.of(invalidRecord));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(responseDto));

        IngestionResultDto result = ingestionService.ingestMandiPrices(100);

        assertNotNull(result);
        assertEquals(1, result.getRecordsFetched());
        assertEquals(1, result.getRecordsRejected());
    }
}

