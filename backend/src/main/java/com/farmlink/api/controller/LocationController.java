package com.farmlink.api.controller;

import com.farmlink.api.service.LocationMasterService;
import com.farmlink.api.service.MarketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationMasterService locationMasterService;

    public LocationController(LocationMasterService locationMasterService) {
        this.locationMasterService = locationMasterService;
    }

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        List<String> states = locationMasterService.getCanonicalStates();
        return ResponseEntity.ok(states);
    }

    @GetMapping("/districts")
    public ResponseEntity<List<String>> getDistricts(@RequestParam(value = "state", required = false) String state) {
        if (state == null || state.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<String> districts = locationMasterService.getCanonicalDistricts(state);
        return ResponseEntity.ok(districts);
    }

    @GetMapping("/areas")
    public ResponseEntity<List<String>> getAreas(
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "district", required = false) String district
    ) {
        if (state == null || state.trim().isEmpty() || district == null || district.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<String> areas = locationMasterService.getCanonicalAreas(state, district);
        // Returns empty list [] if no reliable sub-district/mandal mapping exists
        return ResponseEntity.ok(areas);
    }
}
