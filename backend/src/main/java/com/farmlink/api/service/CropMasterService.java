package com.farmlink.api.service;

import com.farmlink.api.dto.CropResponse;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CropMasterService {

    private static final Logger logger = LoggerFactory.getLogger(CropMasterService.class);
    private final Firestore firestore;

    public static final List<CropResponse> DEFAULT_CROPS = List.of(
            new CropResponse("crop-paddy", "Rice / Paddy", "CEREAL", "Oryza sativa", "ACTIVE"),
            new CropResponse("crop-wheat", "Wheat", "CEREAL", "Triticum aestivum", "ACTIVE"),
            new CropResponse("crop-maize", "Maize", "CEREAL", "Zea mays", "ACTIVE"),
            new CropResponse("crop-cotton", "Cotton", "FIBER", "Gossypium hirsutum", "ACTIVE"),
            new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"),
            new CropResponse("crop-tomato", "Tomato", "VEGETABLE", "Solanum lycopersicum", "ACTIVE"),
            new CropResponse("crop-onion", "Onion", "VEGETABLE", "Allium cepa", "ACTIVE"),
            new CropResponse("crop-groundnut", "Groundnut", "OILSEED", "Arachis hypogaea", "ACTIVE"),
            new CropResponse("crop-turmeric", "Turmeric", "SPICE", "Curcuma longa", "ACTIVE"),
            new CropResponse("crop-sugarcane", "Sugarcane", "CASH_CROP", "Saccharum officinarum", "ACTIVE"),
            new CropResponse("crop-pulses", "Red Gram (Tur)", "PULSE", "Cajanus cajan", "ACTIVE"),
            new CropResponse("crop-mango", "Mango", "FRUIT", "Mangifera indica", "ACTIVE")
    );

    public CropMasterService(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<CropResponse> getAllCrops() {
        List<CropResponse> crops = new ArrayList<>();
        if (firestore != null) {
            try {
                QuerySnapshot snapshot = firestore.collection("crops").get().get();
                if (snapshot != null && !snapshot.isEmpty()) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String category = doc.getString("category");
                        String scientificName = doc.getString("scientificName");
                        String status = doc.getString("status") != null ? doc.getString("status") : "ACTIVE";
                        if (name != null) {
                            crops.add(new CropResponse(id, name, category != null ? category : "OTHER", scientificName, status));
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not fetch crops from Firestore: {}. Returning default reference crops.", e.getMessage());
            }
        }

        if (crops.isEmpty()) {
            return DEFAULT_CROPS;
        }

        return crops;
    }

    public CropResponse getCropById(String cropId) {
        if (cropId == null || cropId.trim().isEmpty()) {
            return null;
        }
        for (CropResponse crop : getAllCrops()) {
            if (crop.getId().equalsIgnoreCase(cropId.trim())) {
                return crop;
            }
        }
        return null;
    }
}
