package com.farmlink.api.service;

import com.farmlink.api.client.DataGovMandiClient;
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
import static org.mockito.Mockito.*;

class DataGovIngestionServiceTest {

    private DataGovMandiClient client;
    private MandiMappingService mappingService;
    private Firestore firestore;
    private DataGovIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        client = mock(DataGovMandiClient.class);
        mappingService = mock(MandiMappingService.class);
        firestore = mock(Firestore.class);

        ingestionService = new DataGovIngestionService(client, mappingService, firestore);
    }

    @Test
    void ingestMandiPrices_returnsFailure_whenClientReturnsEmpty() {
        when(client.fetchMandiPrices(100, 0)).thenReturn(Optional.empty());

        IngestionResultDto result = ingestionService.ingestMandiPrices(100);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(0, result.getRecordsFetched());
    }

    @Test
    void ingestMandiPrices_ingestsAndValidatesRecordsCorrectly() {
        DataGovMandiResponseDto responseDto = new DataGovMandiResponseDto();
        DataGovMandiRecordDto validRecord = new DataGovMandiRecordDto(
                "Andhra Pradesh", "Guntur", "Guntur Mandi", "Chilli Red", "Sannam",
                "26/08/2026", "18000", "22000", "20000"
        );
        responseDto.setRecords(List.of(validRecord));

        when(client.fetchMandiPrices(100, 0)).thenReturn(Optional.of(responseDto));

        MarketSummaryResponse m = new MarketSummaryResponse(
                "mkt-guntur-mandi", "Guntur Mandi", "GNT-001", "MANDI", "Andhra Pradesh", "Guntur", "Guntur West", "ACTIVE", 4
        );
        CropResponse c = new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE");

        when(mappingService.mapMarket("Andhra Pradesh", "Guntur", "Guntur Mandi")).thenReturn(Optional.of(m));
        when(mappingService.mapCrop("Chilli Red")).thenReturn(Optional.of(c));

        IngestionResultDto result = ingestionService.ingestMandiPrices(100);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordsFetched());
        assertEquals(1, result.getRecordsImported());
        assertEquals(0, result.getRecordsRejected());
    }
}
