package com.farmlink.api.controller;

import com.farmlink.api.service.LocationMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationControllerTest {

    private LocationMasterService locationMasterService;
    private LocationController locationController;

    @BeforeEach
    void setUp() {
        locationMasterService = mock(LocationMasterService.class);
        locationController = new LocationController(locationMasterService);
    }

    @Test
    void getStates_returnsDistinctSortedStates() {
        when(locationMasterService.getCanonicalStates())
                .thenReturn(List.of("Andhra Pradesh", "Telangana"));

        ResponseEntity<List<String>> response = locationController.getStates();

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Andhra Pradesh", response.getBody().get(0));
        assertEquals("Telangana", response.getBody().get(1));
    }

    @Test
    void getDistricts_returnsDistrictsForState() {
        when(locationMasterService.getCanonicalDistricts("Telangana"))
                .thenReturn(List.of("Hyderabad", "Warangal"));

        ResponseEntity<List<String>> response = locationController.getDistricts("Telangana");

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Hyderabad", response.getBody().get(0));
    }

    @Test
    void getAreas_returnsEmpty_whenNoSubAreaExists() {
        when(locationMasterService.getCanonicalAreas("Telangana", "Khammam"))
                .thenReturn(List.of());

        ResponseEntity<List<String>> response = locationController.getAreas("Telangana", "Khammam");

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}
