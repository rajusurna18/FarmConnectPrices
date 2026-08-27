package com.farmlink.api.service;

import com.farmlink.api.client.DataGovMandiClient;
import com.farmlink.api.config.DataGovMandiProperties;
import com.farmlink.api.dto.CoverageAuditResultDto;
import com.farmlink.api.dto.external.DataGovMandiRecordDto;
import com.farmlink.api.dto.external.DataGovMandiResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgmarknetCoverageAuditServiceTest {

    private DataGovMandiClient client;
    private DataGovMandiProperties properties;
    private AgmarknetCoverageAuditService auditService;

    @BeforeEach
    void setUp() {
        client = mock(DataGovMandiClient.class);
        properties = new DataGovMandiProperties();
        properties.setResourceId("35985678-0d79-46b4-9ed6-6f13308a1d24");
        properties.setPageSize(100);

        auditService = new AgmarknetCoverageAuditService(client, properties);
    }

    // 1. Pagination Sampling Test
    @Test
    void performCoverageAudit_executesPaginationSamplingCorrectly() {
        DataGovMandiResponseDto page1 = new DataGovMandiResponseDto();
        page1.setRecords(List.of(
                new DataGovMandiRecordDto("Telangana", "Warangal", "Enumamula", "Chilli Red", "Sannam", "26/08/2026", "18000", "22000", "20000"),
                new DataGovMandiRecordDto("Andhra Pradesh", "Guntur", "Guntur Mandi", "Chilli Red", "Sannam", "26/08/2026", "19000", "23000", "21000")
        ));

        when(client.fetchMandiPrices(anyString(), eq(2), eq(0))).thenReturn(Optional.of(page1));
        when(client.fetchMandiPrices(anyString(), eq(2), eq(2))).thenReturn(Optional.of(new DataGovMandiResponseDto()));

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 2, 5, 0);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getRecordsSampled());
        assertEquals(1, result.getPagesSampled());
    }

    // 2. Unique State Extraction Test
    @Test
    void performCoverageAudit_extractsUniqueStatesCorrectly() {
        DataGovMandiResponseDto page = new DataGovMandiResponseDto();
        page.setRecords(List.of(
                new DataGovMandiRecordDto("Punjab", "Sangrur", "Sangrur Mandi", "Wheat", "Kalyan", "26/08/2026", "2000", "2400", "2200"),
                new DataGovMandiRecordDto("Punjab", "Ludhiana", "Ludhiana Mandi", "Rice", "Basmati", "26/08/2026", "3000", "3600", "3300"),
                new DataGovMandiRecordDto("Gujarat", "Rajkot", "Bedi APMC", "Cotton", "Medium", "26/08/2026", "6000", "7000", "6500")
        ));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(page));

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 100, 1, 0);

        assertEquals(2, result.getUniqueStatesCount());
    }

    // 3. Unique District Extraction Test
    @Test
    void performCoverageAudit_extractsUniqueDistrictsCorrectly() {
        DataGovMandiResponseDto page = new DataGovMandiResponseDto();
        page.setRecords(List.of(
                new DataGovMandiRecordDto("Karnataka", "Bengaluru Urban", "Yeshwanthpur", "Potato", "Local", "26/08/2026", "1200", "1600", "1400"),
                new DataGovMandiRecordDto("Karnataka", "Kolar", "Kolar APMC", "Tomato", "Local", "26/08/2026", "2000", "2500", "2200")
        ));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(page));

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 100, 1, 0);

        assertEquals(2, result.getUniqueDistrictsCount());
    }

    // 4. Unique Market Extraction Test
    @Test
    void performCoverageAudit_extractsUniqueMarketsCorrectly() {
        DataGovMandiResponseDto page = new DataGovMandiResponseDto();
        page.setRecords(List.of(
                new DataGovMandiRecordDto("Maharashtra", "Nagpur", "Kalamna", "Orange", "Medium", "26/08/2026", "4000", "5000", "4500"),
                new DataGovMandiRecordDto("Maharashtra", "Pune", "Gultekdi", "Onion", "Red", "26/08/2026", "1500", "2000", "1800")
        ));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(page));

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 100, 1, 0);

        assertEquals(2, result.getUniqueMarketsCount());
    }

    // 5. Unique Commodity Extraction Test
    @Test
    void performCoverageAudit_extractsUniqueCommoditiesCorrectly() {
        DataGovMandiResponseDto page = new DataGovMandiResponseDto();
        page.setRecords(List.of(
                new DataGovMandiRecordDto("Kerala", "Wayanad", "Kalpetta", "Black Pepper", "Garbled", "26/08/2026", "50000", "55000", "52000"),
                new DataGovMandiRecordDto("Kerala", "Idukki", "Kumily", "Cardamom", "Small", "26/08/2026", "150000", "180000", "165000")
        ));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(page));

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 100, 1, 0);

        assertEquals(2, result.getUniqueCommoditiesCount());
    }

    // 6. State Filter Request Test
    @Test
    void performCoverageAudit_executesStateFilterRequests() {
        DataGovMandiResponseDto samplePage = new DataGovMandiResponseDto();
        samplePage.setRecords(List.of(
                new DataGovMandiRecordDto("Odisha", "Kuttack", "Chhatrabazar", "Brinjal", "Local", "26/08/2026", "1000", "1400", "1200")
        ));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(samplePage));

        DataGovMandiResponseDto filterPage = new DataGovMandiResponseDto();
        filterPage.setRecords(List.of(
                new DataGovMandiRecordDto("Odisha", "Puri", "Puri Mandi", "Pumpkin", "Local", "26/08/2026", "800", "1000", "900")
        ));
        when(client.fetchMandiPrices(anyString(), eq(100), eq(0), eq(Map.of("state", "Odisha"))))
                .thenReturn(Optional.of(filterPage));

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 100, 1, 0);

        assertNotNull(result);
        assertEquals(1, result.getStateSummaries().size());
        assertTrue(result.getStateSummaries().get(0).isFilterConfirmed());
        assertEquals("FILTER_CONFIRMED", result.getStateSummaries().get(0).getStatus());
    }

    // 7. Duplicate Elimination Test
    @Test
    void performCoverageAudit_eliminatesDuplicatesCorrectly() {
        DataGovMandiResponseDto page = new DataGovMandiResponseDto();
        page.setRecords(List.of(
                new DataGovMandiRecordDto("Haryana", "Karnal", "Karnal Mandi", "Paddy", "Common", "26/08/2026", "1900", "2100", "2000"),
                new DataGovMandiRecordDto("Haryana", "Karnal", "Karnal Mandi", "Paddy", "Common", "26/08/2026", "1900", "2100", "2000")
        ));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(page));

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 100, 1, 0);

        assertEquals(1, result.getUniqueStatesCount());
        assertEquals(1, result.getUniqueDistrictsCount());
        assertEquals(1, result.getUniqueMarketsCount());
        assertEquals(1, result.getUniqueCommoditiesCount());
    }

    // 8. Empty API Response Test
    @Test
    void performCoverageAudit_handlesEmptyApiResponseGracefully() {
        when(client.fetchMandiPrices(anyString(), anyInt(), anyInt())).thenReturn(Optional.of(new DataGovMandiResponseDto()));

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 100, 5, 0);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(0, result.getRecordsSampled());
        assertEquals(0, result.getPagesSampled());
    }

    // 9. API Error / Retry Handling Test
    @Test
    void performCoverageAudit_handlesApiErrorsGracefully() {
        when(client.fetchMandiPrices(anyString(), anyInt(), anyInt())).thenReturn(Optional.empty());

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 100, 5, 0);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(0, result.getRecordsSampled());
        assertFalse(((List<?>) result.getSamplingDetails().get("errors")).isEmpty());
    }

    // 10. Maximum Page Limit Enforcement Test
    @Test
    void performCoverageAudit_enforcesUpperBoundsOnPageSizeAndMaxPages() {
        when(client.fetchMandiPrices(anyString(), anyInt(), anyInt())).thenReturn(Optional.of(new DataGovMandiResponseDto()));

        // Request 50,000 pageSize and 500 maxPages -> should cap at 10,000 pageSize and 100 maxPages
        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 50000, 500, 0);

        assertEquals(10000, result.getSamplingDetails().get("pageSize"));
        assertEquals(100, result.getSamplingDetails().get("maxPages"));
    }

    // 11. No Firestore Price Writes Test
    @Test
    void performCoverageAudit_neverWritesToFirestore() {
        // Service doesn't inject Firestore, guaranteeing zero writes to Firestore
        DataGovMandiResponseDto page = new DataGovMandiResponseDto();
        page.setRecords(List.of(
                new DataGovMandiRecordDto("Tamil Nadu", "Chennai", "Koyambedu", "Banana", "Poovan", "26/08/2026", "3000", "4000", "3500")
        ));

        when(client.fetchMandiPrices(anyString(), anyInt(), eq(0))).thenReturn(Optional.of(page));

        CoverageAuditResultDto result = auditService.performCoverageAudit(null, 100, 1, 0);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        // Verify client was called for data discovery only
        verify(client, atLeastOnce()).fetchMandiPrices(anyString(), anyInt(), anyInt());
    }

    // 12. Authentication Protection Test
    @Test
    void triggerCoverageAudit_deniesUnauthenticatedRequests() {
        com.farmlink.api.controller.InternalIngestionController controller =
                new com.farmlink.api.controller.InternalIngestionController(mock(DataGovIngestionService.class), auditService, mock(DiscoveryIndexSyncService.class));

        org.springframework.http.ResponseEntity<?> unauthResp = controller.triggerCoverageAudit(null, null, 100, 1, 0, null);
        assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED, unauthResp.getStatusCode());
    }
}
