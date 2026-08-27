package com.farmlink.api.service;

import com.farmlink.api.dto.LocationMasterResponse;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LocationMasterService {

    private static final Logger logger = LoggerFactory.getLogger(LocationMasterService.class);
    private final Firestore firestore;

    public LocationMasterService(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<LocationMasterResponse> getAllLocations() {
        if (firestore == null) {
            return Collections.emptyList();
        }

        List<LocationMasterResponse> locations = new ArrayList<>();
        try {
            QuerySnapshot snapshot = firestore.collection("locations").get().get();
            if (snapshot != null && !snapshot.isEmpty()) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    String id = doc.getId();
                    String state = doc.getString("state");
                    String district = doc.getString("district");
                    String mandal = doc.getString("mandal");
                    String village = doc.getString("village");
                    String pincode = doc.getString("pincode");
                    if (state != null && district != null) {
                        locations.add(new LocationMasterResponse(id, state, district, mandal, village, pincode));
                    }
                }
            }
            return locations;
        } catch (Exception e) {
            logger.error("Firestore read error in getAllLocations: {}", e.getMessage());
            throw new RuntimeException("Could not fetch locations from Firestore: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "states")
    public List<String> getCanonicalStates() {
        if (firestore == null) {
            return Collections.emptyList();
        }

        Set<String> states = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try {
            // Read lightweight locations master collection
            var locRef = firestore.collection("locations");
            if (locRef != null) {
                QuerySnapshot locSnapshot = locRef.get().get();
                if (locSnapshot != null && !locSnapshot.isEmpty()) {
                    for (DocumentSnapshot doc : locSnapshot.getDocuments()) {
                        String st = doc.getString("state");
                        if (st != null && !st.trim().isEmpty()) {
                            states.add(st.trim());
                        }
                    }
                }
            }

            // Merge states from lightweight markets master collection
            var mktRef = firestore.collection("markets");
            if (mktRef != null) {
                QuerySnapshot mktSnapshot = mktRef.get().get();
                if (mktSnapshot != null && !mktSnapshot.isEmpty()) {
                    for (DocumentSnapshot doc : mktSnapshot.getDocuments()) {
                        Object locObj = doc.get("location");
                        if (locObj instanceof Map<?, ?> locMap) {
                            Object stObj = locMap.get("state");
                            if (stObj != null && !stObj.toString().trim().isEmpty()) {
                                states.add(stObj.toString().trim());
                            }
                        }
                    }
                }
            }

            return new ArrayList<>(states);
        } catch (Exception e) {
            logger.error("Firestore read error in getCanonicalStates: {}", e.getMessage());
            throw new RuntimeException("Could not fetch states from Firestore: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "districts", key = "#state")
    public List<String> getCanonicalDistricts(String state) {
        if (state == null || state.trim().isEmpty()) {
            return Collections.emptyList();
        }

        if (firestore == null) {
            return Collections.emptyList();
        }

        Set<String> districts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try {
            String targetState = state.trim();

            // Query lightweight locations collection filtered by state
            var locRef = firestore.collection("locations");
            if (locRef != null) {
                QuerySnapshot locSnapshot = locRef
                        .whereEqualTo("state", targetState)
                        .get().get();
                if (locSnapshot != null && !locSnapshot.isEmpty()) {
                    for (DocumentSnapshot doc : locSnapshot.getDocuments()) {
                        String dist = doc.getString("district");
                        if (dist != null && !dist.trim().isEmpty()) {
                            districts.add(dist.trim());
                        }
                    }
                }
            }

            // Query lightweight markets collection filtered by location.state
            var mktRef = firestore.collection("markets");
            if (mktRef != null) {
                QuerySnapshot mktSnapshot = mktRef
                        .whereEqualTo("location.state", targetState)
                        .get().get();
                if (mktSnapshot != null && !mktSnapshot.isEmpty()) {
                    for (DocumentSnapshot doc : mktSnapshot.getDocuments()) {
                        Object locObj = doc.get("location");
                        if (locObj instanceof Map<?, ?> locMap) {
                            Object distObj = locMap.get("district");
                            if (distObj != null && !distObj.toString().trim().isEmpty()) {
                                districts.add(distObj.toString().trim());
                            }
                        }
                    }
                }
            }

            return new ArrayList<>(districts);
        } catch (Exception e) {
            logger.error("Firestore read error in getCanonicalDistricts for state {}: {}", state, e.getMessage());
            throw new RuntimeException("Could not fetch districts from Firestore: " + e.getMessage(), e);
        }
    }

    public List<String> getCanonicalAreas(String state, String district) {
        if (state == null || state.trim().isEmpty() || district == null || district.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> areas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (LocationMasterResponse loc : getAllLocations()) {
            if (loc.getState() != null && loc.getState().equalsIgnoreCase(state.trim()) &&
                loc.getDistrict() != null && loc.getDistrict().equalsIgnoreCase(district.trim())) {
                if (loc.getMandal() != null && !loc.getMandal().trim().isEmpty()) {
                    areas.add(loc.getMandal().trim());
                }
            }
        }
        return new ArrayList<>(areas);
    }
}



