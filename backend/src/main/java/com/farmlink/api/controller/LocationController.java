package com.farmlink.api.controller;

import com.farmlink.api.dto.LocationDto;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.service.MarketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final MarketService marketService;

    public LocationController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        List<MarketSummaryResponse> markets = marketService.getMarkets(null, null, null, null, null, null, 100);
        Set<String> states = markets.stream()
                .map(MarketSummaryResponse::getState)
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));

        return ResponseEntity.ok(new ArrayList<>(states));
    }

    @GetMapping("/districts")
    public ResponseEntity<List<String>> getDistricts(@RequestParam(value = "state", required = false) String state) {
        List<MarketSummaryResponse> markets = marketService.getMarkets(state, null, null, null, null, null, 100);
        Set<String> districts = markets.stream()
                .map(MarketSummaryResponse::getDistrict)
                .filter(d -> d != null && !d.trim().isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));

        return ResponseEntity.ok(new ArrayList<>(districts));
    }

    @GetMapping("/areas")
    public ResponseEntity<List<String>> getAreas(
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "district", required = false) String district
    ) {
        List<MarketSummaryResponse> markets = marketService.getMarkets(state, district, null, null, null, null, 100);
        Set<String> areas = markets.stream()
                .map(MarketSummaryResponse::getMandal)
                .filter(a -> a != null && !a.trim().isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));

        // Returns empty array [] if no reliable mandal/area exists for the selected district
        return ResponseEntity.ok(new ArrayList<>(areas));
    }
}
