package com.farmlink.api.router.extractor;

import com.farmlink.api.dto.LocationMasterResponse;
import com.farmlink.api.service.LocationMasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LocationEntityExtractor implements AgriculturalEntityExtractor<String> {

    private static final Logger logger = LoggerFactory.getLogger(LocationEntityExtractor.class);

    private static final List<String> CANONICAL_LOCATIONS = List.of(
            // States
            "telangana", "andhra pradesh", "karnataka", "maharashtra", "punjab",
            "haryana", "uttar pradesh", "madhya pradesh", "gujarat", "rajasthan",
            "tamil nadu", "kerala", "odisha", "west bengal", "bihar",
            // Districts & Agricultural Centers
            "guntur", "warangal", "khammam", "nizamabad", "karimnagar", "hyderabad",
            "kurnool", "anantapur", "vijayawada", "visakhapatnam", "pune", "nagpur",
            "nashik", "ludhiana", "amritsar", "bhatinda", "karnal", "amritsar",
            "indore", "bhopal", "ahmedabad", "rajkot", "coimbatore", "madurai"
    );

    private final LocationMasterService locationMasterService;

    public LocationEntityExtractor(@Autowired(required = false) LocationMasterService locationMasterService) {
        this.locationMasterService = locationMasterService;
    }

    @Override
    public List<String> extract(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> knownLocations = new LinkedHashSet<>(CANONICAL_LOCATIONS);
        if (locationMasterService != null) {
            try {
                List<LocationMasterResponse> dbLocations = locationMasterService.getAllLocations();
                if (dbLocations != null) {
                    for (LocationMasterResponse loc : dbLocations) {
                        if (loc.getState() != null && !loc.getState().isBlank()) {
                            knownLocations.add(loc.getState().toLowerCase().trim());
                        }
                        if (loc.getDistrict() != null && !loc.getDistrict().isBlank()) {
                            knownLocations.add(loc.getDistrict().toLowerCase().trim());
                        }
                        if (loc.getMandal() != null && !loc.getMandal().isBlank()) {
                            knownLocations.add(loc.getMandal().toLowerCase().trim());
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not fetch locations from LocationMasterService: {}", e.getMessage());
            }
        }

        List<String> matched = new ArrayList<>();
        List<String> sortedLocations = new ArrayList<>(knownLocations);
        sortedLocations.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String loc : sortedLocations) {
            Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(loc) + "\\b");
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                if (!matched.contains(loc)) {
                    matched.add(loc);
                }
            }
        }

        return Collections.unmodifiableList(matched);
    }
}
