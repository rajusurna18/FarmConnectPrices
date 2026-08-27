package com.farmlink.api.service;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;

import static org.mockito.Mockito.*;

class DiscoveryIndexSyncServiceTest {

    private Firestore firestore;
    private DiscoveryIndexSyncService syncService;

    @BeforeEach
    void setUp() {
        firestore = mock(Firestore.class);
        syncService = new DiscoveryIndexSyncService(firestore);
    }

    @Test
    void syncDiscoveryIndexFromObservedData_handlesNullFirestore() {
        DiscoveryIndexSyncService nullService = new DiscoveryIndexSyncService(null);
        Map<String, Integer> metrics = nullService.syncDiscoveryIndexFromObservedData();

        assertNotNull(metrics);
        assertEquals(0, metrics.get("statesIndexed"));
        assertEquals(0, metrics.get("districtsIndexed"));
    }

    @Test
    void syncDiscoveryIndexFromObservedData_executesBoundedScan() throws Exception {
        CollectionReference marketPricesRef = mock(CollectionReference.class);
        Query queryMock = mock(Query.class);

        when(firestore.collection("marketPrices")).thenReturn(marketPricesRef);
        when(marketPricesRef.limit(anyInt())).thenReturn(queryMock);

        Map<String, Integer> metrics = syncService.syncDiscoveryIndexFromObservedData();

        assertNotNull(metrics);
        verify(firestore, atLeastOnce()).collection("marketPrices");
    }
}
