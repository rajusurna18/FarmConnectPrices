package com.farmlink.api.service;

import com.farmlink.api.dto.LocationMasterResponse;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LocationMasterService {

    private static final Logger logger = LoggerFactory.getLogger(LocationMasterService.class);
    private final Firestore firestore;

    // Canonical Government of India Administrative Location Master (States & Districts)
    public static final List<LocationMasterResponse> DEFAULT_LOCATIONS = List.of(
            // Telangana
            new LocationMasterResponse("loc-tg-hyd", "Telangana", "Hyderabad", "Bahadurpura", "Malakpet", "500036"),
            new LocationMasterResponse("loc-tg-wgl", "Telangana", "Warangal", "Warangal Urban", "Enumamula", "506002"),
            new LocationMasterResponse("loc-tg-nzb", "Telangana", "Nizamabad", "Nizamabad North", "Bardipur", "503001"),
            new LocationMasterResponse("loc-tg-kmm", "Telangana", "Khammam", "Khammam Urban", "Khanapuram", "507002"),
            new LocationMasterResponse("loc-tg-krm", "Telangana", "Karimnagar", "Karimnagar", "Kothapalli", "505001"),
            new LocationMasterResponse("loc-tg-mbb", "Telangana", "Mahbubnagar", "Mahbubnagar", "Palamoor", "509001"),
            new LocationMasterResponse("loc-tg-[#nlg]", "Telangana", "Nalgonda", "Nalgonda", "Clock Tower", "508001"),
            new LocationMasterResponse("loc-tg-adlb", "Telangana", "Adilabad", "Adilabad", "Mavala", "504001"),
            new LocationMasterResponse("loc-tg-medak", "Telangana", "Medak", "Medak", "Ramayampet", "502110"),
            new LocationMasterResponse("loc-tg-siddipet", "Telangana", "Siddipet", "Siddipet", "Ensanpally", "502103"),

            // Andhra Pradesh
            new LocationMasterResponse("loc-ap-gnt", "Andhra Pradesh", "Guntur", "Guntur West", "Pattabhipuram", "522006"),
            new LocationMasterResponse("loc-ap-krn", "Andhra Pradesh", "Kurnool", "Kurnool Urban", "Kallur", "518003"),
            new LocationMasterResponse("loc-ap-krishna", "Andhra Pradesh", "Krishna", "Vijayawada", "Gollapudi", "520012"),
            new LocationMasterResponse("loc-ap-vsp", "Andhra Pradesh", "Visakhapatnam", "Visakhapatnam", "Anakapalle", "530001"),
            new LocationMasterResponse("loc-ap-eg", "Andhra Pradesh", "East Godavari", "Kakinada", "Rajahmundry", "533001"),
            new LocationMasterResponse("loc-ap-wg", "Andhra Pradesh", "West Godavari", "Eluru", "Tadepalligudem", "534001"),
            new LocationMasterResponse("loc-ap-atp", "Andhra Pradesh", "Anantapur", "Anantapur", "Dharmavaram", "515001"),
            new LocationMasterResponse("loc-ap-ctr", "Andhra Pradesh", "Chittoor", "Tirupati", "Madanapalle", "517501"),

            // Karnataka
            new LocationMasterResponse("loc-ka-blr-r", "Karnataka", "Bengaluru Rural", "Devanahalli", "Devanahalli Village", "562110"),
            new LocationMasterResponse("loc-ka-blr-u", "Karnataka", "Bengaluru Urban", "Yeshwanthpur", "APMC Yard", "560022"),
            new LocationMasterResponse("loc-ka-kolar", "Karnataka", "Kolar", "Kolar", "Malur", "563101"),
            new LocationMasterResponse("loc-ka-mysore", "Karnataka", "Mysuru", "Mysuru", "Bandipalya", "570001"),
            new LocationMasterResponse("loc-ka-belagavi", "Karnataka", "Belagavi", "Belagavi", "Bailhongal", "590001"),

            // Maharashtra
            new LocationMasterResponse("loc-mh-nagpur", "Maharashtra", "Nagpur", "Nagpur Urban", "Kalamna", "440008"),
            new LocationMasterResponse("loc-mh-pune", "Maharashtra", "Pune", "Haveli", "Gultekdi", "411037"),
            new LocationMasterResponse("loc-mh-nashik", "Maharashtra", "Nashik", "Lasalgaon", "Vinchur", "422306"),
            new LocationMasterResponse("loc-mh-mumbai", "Maharashtra", "Mumbai City", "Vashi", "APMC Market", "400703"),

            // Tamil Nadu
            new LocationMasterResponse("loc-tn-chennai", "Tamil Nadu", "Chennai", "Koyambedu", "Wholesale Market", "600107"),
            new LocationMasterResponse("loc-tn-coimbatore", "Tamil Nadu", "Coimbatore", "Mettupalayam", "Kiramani", "641001"),

            // Gujarat
            new LocationMasterResponse("loc-gj-ahmedabad", "Gujarat", "Ahmedabad", "Jamalpur", "APMC Market", "380001"),
            new LocationMasterResponse("loc-gj-rajkot", "Gujarat", "Rajkot", "Bedi", "Yard Market", "360001"),

            // Punjab
            new LocationMasterResponse("loc-pb-ludhiana", "Punjab", "Ludhiana", "Gill Road", "Mandi Yard", "141001"),

            // Uttar Pradesh
            new LocationMasterResponse("loc-up-lucknow", "Uttar Pradesh", "Lucknow", "Dubagga", "Mandi Samiti", "226001")
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

    public List<String> getCanonicalStates() {
        Set<String> states = new TreeSet<>();
        for (LocationMasterResponse loc : getAllLocations()) {
            if (loc.getState() != null && !loc.getState().trim().isEmpty()) {
                states.add(loc.getState().trim());
            }
        }
        return new ArrayList<>(states);
    }

    public List<String> getCanonicalDistricts(String state) {
        if (state == null || state.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> districts = new TreeSet<>();
        for (LocationMasterResponse loc : getAllLocations()) {
            if (loc.getState() != null && loc.getState().equalsIgnoreCase(state.trim())) {
                if (loc.getDistrict() != null && !loc.getDistrict().trim().isEmpty()) {
                    districts.add(loc.getDistrict().trim());
                }
            }
        }
        return new ArrayList<>(districts);
    }

    public List<String> getCanonicalAreas(String state, String district) {
        if (state == null || state.trim().isEmpty() || district == null || district.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> areas = new TreeSet<>();
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
