package com.farmlink.api.service;

import com.farmlink.api.dto.LocationMasterResponse;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LocationMasterService {

    private static final Logger logger = LoggerFactory.getLogger(LocationMasterService.class);
    private final Firestore firestore;
    private final FirestoreQuotaGuard quotaGuard;

    public LocationMasterService(Firestore firestore) {
        this(firestore, null);
    }

    @Autowired
    public LocationMasterService(Firestore firestore, @Autowired(required = false) FirestoreQuotaGuard quotaGuard) {
        this.firestore = firestore;
        this.quotaGuard = quotaGuard;
    }

    @Cacheable(value = "locations", sync = true)
    public List<LocationMasterResponse> getAllLocations() {
        if (quotaGuard != null) {
            quotaGuard.checkQuotaAvailability();
        }

        if (firestore == null) {
            return Collections.emptyList();
        }

        List<LocationMasterResponse> locations = new ArrayList<>();
        try {
            var colRef = firestore.collection("locations");
            if (colRef != null) {
                var future = colRef.get();
                if (future != null) {
                    QuerySnapshot snapshot = future.get();
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
                }
            }
            if (quotaGuard != null) {
                quotaGuard.recordSuccess();
            }
            return locations;
        } catch (Exception e) {
            if (quotaGuard != null && isQuotaExhaustedError(e)) {
                quotaGuard.recordQuotaExhaustion(e);
            }
            logger.error("Firestore read error in getAllLocations: {}", e.getMessage());
            throw new RuntimeException("Could not fetch locations from Firestore: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "states", sync = true)
    public List<String> getCanonicalStates() {
        if (quotaGuard != null) {
            quotaGuard.checkQuotaAvailability();
        }

        if (firestore == null) {
            return Collections.emptyList();
        }

        Set<String> states = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try {
            // Read lightweight locations master collection
            var locRef = firestore.collection("locations");
            if (locRef != null) {
                var future = locRef.get();
                if (future != null) {
                    QuerySnapshot locSnapshot = future.get();
                    if (locSnapshot != null && !locSnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : locSnapshot.getDocuments()) {
                            String st = doc.getString("state");
                            if (st != null && !st.trim().isEmpty()) {
                                states.add(st.trim());
                            }
                        }
                    }
                }
            }

            // Merge states from lightweight markets master collection
            var mktRef = firestore.collection("markets");
            if (mktRef != null) {
                var future = mktRef.get();
                if (future != null) {
                    QuerySnapshot mktSnapshot = future.get();
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
            }

            if (quotaGuard != null) {
                quotaGuard.recordSuccess();
            }
            return new ArrayList<>(states);
        } catch (Exception e) {
            if (quotaGuard != null && isQuotaExhaustedError(e)) {
                quotaGuard.recordQuotaExhaustion(e);
            }
            logger.error("Firestore read error in getCanonicalStates: {}", e.getMessage());
            throw new RuntimeException("Could not fetch states from Firestore: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "districts", key = "#state", sync = true)
    public List<String> getCanonicalDistricts(String state) {
        if (state == null || state.trim().isEmpty()) {
            return Collections.emptyList();
        }

        if (quotaGuard != null) {
            quotaGuard.checkQuotaAvailability();
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
                var future = locRef.whereEqualTo("state", targetState).get();
                if (future != null) {
                    QuerySnapshot locSnapshot = future.get();
                    if (locSnapshot != null && !locSnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : locSnapshot.getDocuments()) {
                            String dist = doc.getString("district");
                            if (dist != null && !dist.trim().isEmpty()) {
                                districts.add(dist.trim());
                            }
                        }
                    }
                }
            }

            // Query lightweight markets collection filtered by location.state
            var mktRef = firestore.collection("markets");
            if (mktRef != null) {
                var future = mktRef.whereEqualTo("location.state", targetState).get();
                if (future != null) {
                    QuerySnapshot mktSnapshot = future.get();
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
            }

            if (quotaGuard != null) {
                quotaGuard.recordSuccess();
            }
            return new ArrayList<>(districts);
        } catch (Exception e) {
            if (quotaGuard != null && isQuotaExhaustedError(e)) {
                quotaGuard.recordQuotaExhaustion(e);
            }
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

    private boolean isQuotaExhaustedError(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        if (msg != null && (msg.contains("RESOURCE_EXHAUSTED") || msg.contains("Quota exceeded"))) {
            return true;
        }
        return isQuotaExhaustedError(t.getCause());
    }
}
