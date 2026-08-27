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
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CropMasterService {

    private static final Logger logger = LoggerFactory.getLogger(CropMasterService.class);
    private final Firestore firestore;
    private final AtomicReference<Map<String, CropResponse>> cropMapCache = new AtomicReference<>();

    public CropMasterService(Firestore firestore) {
        this.firestore = firestore;
    }

    @Cacheable(value = "crops", sync = true)
    public List<CropResponse> getAllCrops() {
        Map<String, CropResponse> cachedMap = cropMapCache.get();
        if (cachedMap != null && !cachedMap.isEmpty()) {
            return new ArrayList<>(cachedMap.values());
        }
        if (firestore == null) {
            return Collections.emptyList();
        }

        List<CropResponse> crops = new ArrayList<>();
        try {
            var colRef = firestore.collection("crops");
            if (colRef != null) {
                var future = colRef.get();
                if (future != null) {
                    QuerySnapshot snapshot = future.get();
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
            }
            Map<String, CropResponse> map = new HashMap<>();
            for (CropResponse c : crops) {
                if (c != null && c.getId() != null) {
                    map.put(c.getId().toLowerCase(Locale.ROOT), c);
                }
            }
            cropMapCache.compareAndSet(null, Collections.unmodifiableMap(map));
            return crops;
        } catch (Exception e) {
            logger.error("Firestore read error in getAllCrops: {}", e.getMessage());
            throw new RuntimeException("Could not fetch crops from Firestore: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "cropMap", sync = true)
    public Map<String, CropResponse> getCropMap() {
        Map<String, CropResponse> cached = cropMapCache.get();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<CropResponse> list = getAllCrops();
        Map<String, CropResponse> map = new HashMap<>();
        for (CropResponse c : list) {
            if (c != null && c.getId() != null) {
                map.put(c.getId().toLowerCase(Locale.ROOT), c);
            }
        }
        Map<String, CropResponse> unmodifiable = Collections.unmodifiableMap(map);
        cropMapCache.compareAndSet(null, unmodifiable);
        return cropMapCache.get() != null ? cropMapCache.get() : unmodifiable;
    }

    public CropResponse getCropById(String cropId) {
        if (cropId == null || cropId.trim().isEmpty()) {
            return null;
        }
        Map<String, CropResponse> map = getCropMap();
        CropResponse direct = map.get(cropId.trim().toLowerCase(Locale.ROOT));
        if (direct != null) {
            return direct;
        }
        for (CropResponse c : map.values()) {
            if (c.getId().equalsIgnoreCase(cropId.trim())) {
                return c;
            }
        }
        return null;
    }
}
