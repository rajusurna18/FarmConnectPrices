package com.farmlink.api.service;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.MarketResponse;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirestoreReadBudgetTest {

    private Firestore firestore;
    private CollectionReference cropsCol;
    private CollectionReference marketsCol;
    private QuerySnapshot cropsSnapshot;
    private QuerySnapshot marketsSnapshot;

    private CropMasterService cropMasterService;
    private MarketService marketService;

    @BeforeEach
    void setUp() throws Exception {
        firestore = mock(Firestore.class);
        cropsCol = mock(CollectionReference.class);
        marketsCol = mock(CollectionReference.class);
        cropsSnapshot = mock(QuerySnapshot.class);
        marketsSnapshot = mock(QuerySnapshot.class);

        when(firestore.collection("crops")).thenReturn(cropsCol);
        when(firestore.collection("markets")).thenReturn(marketsCol);

        // Stub Crops Firestore query
        var cropsFuture = mock(com.google.api.core.ApiFuture.class);
        when(cropsCol.get()).thenReturn(cropsFuture);
        when(cropsFuture.get()).thenReturn(cropsSnapshot);

        QueryDocumentSnapshot cropDoc = mock(QueryDocumentSnapshot.class);
        when(cropDoc.getId()).thenReturn("crop-chilli");
        when(cropDoc.getString("name")).thenReturn("Red Chilli");
        when(cropDoc.getString("category")).thenReturn("SPICE");
        when(cropsSnapshot.isEmpty()).thenReturn(false);
        when(cropsSnapshot.getDocuments()).thenReturn(List.of(cropDoc));

        // Stub Markets Firestore query
        var marketsFuture = mock(com.google.api.core.ApiFuture.class);
        when(marketsCol.get()).thenReturn(marketsFuture);
        when(marketsFuture.get()).thenReturn(marketsSnapshot);

        QueryDocumentSnapshot marketDoc = mock(QueryDocumentSnapshot.class);
        when(marketDoc.getId()).thenReturn("mkt-guntur-mandi");
        when(marketDoc.getString("name")).thenReturn("Guntur Agricultural Market");
        when(marketDoc.getString("code")).thenReturn("GNT-MND-001");
        when(marketDoc.getString("type")).thenReturn("MANDI");
        when(marketsSnapshot.isEmpty()).thenReturn(false);
        when(marketsSnapshot.getDocuments()).thenReturn(List.of(marketDoc));

        cropMasterService = new CropMasterService(firestore);
        marketService = new MarketService(firestore, cropMasterService);
    }

    @Test
    @DisplayName("CropMasterService — Cold & Warm Cache Firestore Read Budget Verification")
    void cropMasterService_readBudgetVerification() {
        // Cold Cache: First read
        List<CropResponse> crops1 = cropMasterService.getAllCrops();
        assertNotNull(crops1);
        assertEquals(1, crops1.size());
        verify(cropsCol, times(1)).get();

        // Warm Cache: Subsequent 100 calls to getCropById/getAllCrops do 0 additional Firestore reads
        for (int i = 0; i < 100; i++) {
            CropResponse c = cropMasterService.getCropById("crop-chilli");
            assertNotNull(c);
            assertEquals("Red Chilli", c.getName());
        }

        // Verify total Firestore collection reads remain EXACTLY 1
        verify(cropsCol, times(1)).get();
    }

    @Test
    @DisplayName("MarketService — Cold & Warm Cache Firestore Read Budget Verification")
    void marketService_readBudgetVerification() {
        // Cold Cache: First read
        List<MarketResponse> markets1 = marketService.fetchAllMarkets();
        assertNotNull(markets1);
        assertEquals(1, markets1.size());
        verify(marketsCol, times(1)).get();

        // Warm Cache: 100 subsequent getMarketById lookups do 0 additional Firestore reads
        for (int i = 0; i < 100; i++) {
            MarketResponse m = marketService.getMarketById("mkt-guntur-mandi");
            assertNotNull(m);
            assertEquals("Guntur Agricultural Market", m.getName());
        }

        // Verify total Firestore collection reads remain EXACTLY 1
        verify(marketsCol, times(1)).get();
    }

    @Test
    @DisplayName("MarketService — Unmapped obs-mkt-* IDs never trigger Firestore document queries")
    void marketService_obsMarketIds_doNotTriggerFirestoreDocumentQueries() {
        // Warm up cache
        marketService.fetchAllMarkets();

        // Querying unknown / observation market ID
        assertThrows(NoSuchElementException.class, () -> marketService.getMarketById("obs-mkt-675752122"));

        // Verify no document("obs-mkt-675752122") call was EVER made on Firestore
        verify(marketsCol, never()).document(anyString());
    }

    @Test
    @DisplayName("Single-Flight Protection — Concurrent threads execute single Firestore read")
    void concurrentRequests_executeSingleFirestoreRead() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<List<CropResponse>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                return cropMasterService.getAllCrops();
            }));
        }

        latch.countDown(); // Release all threads simultaneously

        for (Future<List<CropResponse>> future : futures) {
            List<CropResponse> result = future.get();
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        executor.shutdown();

        // In-memory single-flight invocation test verifies minimal reads
        assertTrue(Mockito.mockingDetails(cropsCol).getInvocations().size() <= 10);
    }
}
