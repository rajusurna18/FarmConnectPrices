package com.farmlink.api.service;

import com.farmlink.api.dto.LocationMasterResponse;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocationMasterService {

    private static final Logger logger = LoggerFactory.getLogger(LocationMasterService.class);
    private final Firestore firestore;

    public static final List<LocationMasterResponse> DEFAULT_LOCATIONS = List.of(
            new LocationMasterResponse("loc-hyderabad", "Telangana", "Hyderabad", "Bahadurpura", "Bahadurpura Village", "500064"),
            new LocationMasterResponse("loc-warangal", "Telangana", "Warangal", "Warangal Urban", "Enumamula", "506002"),
            new LocationMasterResponse("loc-nizamabad", "Telangana", "Nizamabad", "Nizamabad North", "Bardipur", "503001"),
            new LocationMasterResponse("loc-vijayawada", "Telangana", "Khammam", "Khammam Urban", "Khanapuram", "507002"),
            new LocationMasterResponse("loc-guntur", "Andhra Pradesh", "Guntur", "Guntur West", "Pattabhipuram", "522006"),
            new LocationMasterResponse("loc-kurnool", "Andhra Pradesh", "Kurnool", "Kurnool Urban", "Kallur", "518003"),
            new LocationMasterResponse("loc-bengaluru", "Karnataka", "Bengaluru Rural", "Devanahalli", "Devanahalli Village", "562110"),
            new LocationMasterResponse("loc-nagpur", "Maharashtra", "Nagpur", "Nagpur Urban", "Kalamna", "440008")
    );

    public LocationMasterService(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<LocationMasterResponse> getAllLocations() {
        List<LocationMasterResponse> locations = new ArrayList<>();
        if (firestore != null) {
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
            } catch (Exception e) {
                logger.warn("Could not fetch locations from Firestore: {}. Returning default reference locations.", e.getMessage());
            }
        }

        if (locations.isEmpty()) {
            return DEFAULT_LOCATIONS;
        }

        return locations;
    }
}
