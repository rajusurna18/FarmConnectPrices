package com.farmlink.api.service;

import com.farmlink.api.dto.CropResponse;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

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
            new CropResponse("crop-tomato", "Tomato", "VEGETABLE", "Solanum lycopersicum", "ACTIVE")
    );

    public CropMasterService(Firestore firestore) {
        this.firestore = firestore;
    }

    @Cacheable(value = "crops")
    public List<CropResponse> getAllCrops() {
        if (firestore == null) {
            return DEFAULT_CROPS;
        }

        List<CropResponse> crops = new ArrayList<>();
        try {
            var colRef = firestore.collection("crops");
            if (colRef != null) {
                QuerySnapshot snapshot = colRef.get().get();
                if (snapshot != null && !snapshot.isEmpty()) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String category = doc.getString("category");
                        String scientificName = doc.getString("scientificName");
                        String status = doc.getString("status") != null ? doc.getString("status") : "ACTIVE";
                        if (name != null) {
                            crops.add(new CropResponse(id, name, category != null ? category : "AGRICULTURAL_COMMODITY", scientificName, status));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Firestore read error in getAllCrops: {}", e.getMessage());
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

