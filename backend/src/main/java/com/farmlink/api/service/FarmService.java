package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class FarmService {

    private static final Logger logger = LoggerFactory.getLogger(FarmService.class);

    public static final String ROLE_FARMER = "FARMER";
    public static final String UNIT_ACRE = "ACRE";
    public static final String UNIT_HECTARE = "HECTARE";

    public static final Set<String> VALID_UNITS = Set.of(UNIT_ACRE, UNIT_HECTARE);
    public static final Set<String> VALID_SEASONS = Set.of("KHARIF", "RABI", "ZAID");
    public static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final Firestore firestore;
    private final CropMasterService cropMasterService;
    private final Map<String, FarmResponse> inMemoryFarms = new HashMap<>();
    private final Map<String, FarmCropResponse> inMemoryFarmCrops = new HashMap<>();

    public FarmService(Firestore firestore, CropMasterService cropMasterService) {
        this.firestore = firestore;
        this.cropMasterService = cropMasterService;
    }

    public void verifyFarmerRole(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (firestore != null) {
            try {
                DocumentSnapshot userDoc = firestore.collection("users").document(uid).get().get();
                if (userDoc.exists()) {
                    String role = userDoc.getString("role");
                    if (!ROLE_FARMER.equalsIgnoreCase(role)) {
                        throw new AccessDeniedException("Farm management access is strictly restricted to Farmers.");
                    }
                    return;
                }
            } catch (AccessDeniedException ade) {
                throw ade;
            } catch (Exception e) {
                logger.warn("Could not verify user role from Firestore for uid {}: {}", uid, e.getMessage());
            }
        }
    }

    // ==========================================
    // FARM CRUD OPERATIONS
    // ==========================================

    public List<FarmResponse> getFarmsForOwner(String ownerUid) {
        verifyFarmerRole(ownerUid);
        List<FarmResponse> farms = new ArrayList<>();

        if (firestore != null) {
            try {
                QuerySnapshot snapshot = firestore.collection("farms")
                        .whereEqualTo("ownerUid", ownerUid)
                        .get().get();

                if (snapshot != null && !snapshot.isEmpty()) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        farms.add(mapDocToFarmResponse(doc));
                    }
                    return farms;
                }
            } catch (Exception e) {
                logger.warn("Error fetching farms for ownerUid {}: {}", ownerUid, e.getMessage());
            }
        }

        // Fallback for offline / in-memory test environment
        for (FarmResponse f : inMemoryFarms.values()) {
            if (ownerUid.equals(f.getOwnerUid())) {
                farms.add(f);
            }
        }
        return farms;
    }

    public FarmResponse getFarmById(String farmId, String ownerUid) {
        verifyFarmerRole(ownerUid);
        if (farmId == null || farmId.trim().isEmpty()) {
            throw new IllegalArgumentException("Farm ID cannot be empty.");
        }

        if (firestore != null) {
            try {
                DocumentSnapshot doc = firestore.collection("farms").document(farmId).get().get();
                if (doc.exists()) {
                    String farmOwner = doc.getString("ownerUid");
                    if (!ownerUid.equals(farmOwner)) {
                        throw new AccessDeniedException("Access denied. You do not own this farm.");
                    }
                    return mapDocToFarmResponse(doc);
                } else {
                    throw new NoSuchElementException("Farm not found with ID: " + farmId);
                }
            } catch (AccessDeniedException | NoSuchElementException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("Error fetching farm ID {}: {}", farmId, e.getMessage());
            }
        }

        FarmResponse f = inMemoryFarms.get(farmId);
        if (f == null) {
            throw new NoSuchElementException("Farm not found with ID: " + farmId);
        }
        if (!ownerUid.equals(f.getOwnerUid())) {
            throw new AccessDeniedException("Access denied. You do not own this farm.");
        }
        return f;
    }

    public FarmResponse createFarm(String ownerUid, FarmRequest request) {
        verifyFarmerRole(ownerUid);
        validateFarmRequest(request);

        String farmId = UUID.randomUUID().toString();
        String nowIso = Instant.now().toString();
        String status = (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                ? request.getStatus().trim().toUpperCase()
                : "ACTIVE";

        LocationDto loc = request.getLocation();

        Map<String, Object> farmData = new HashMap<>();
        farmData.put("id", farmId);
        farmData.put("ownerUid", ownerUid);
        farmData.put("name", request.getName().trim());
        farmData.put("location", mapLocationToMap(loc));
        farmData.put("landArea", request.getLandArea());
        farmData.put("landAreaUnit", request.getLandAreaUnit().trim().toUpperCase());
        farmData.put("status", status);
        farmData.put("createdAt", FieldValue.serverTimestamp());
        farmData.put("updatedAt", FieldValue.serverTimestamp());

        if (firestore != null) {
            try {
                firestore.collection("farms").document(farmId).set(farmData).get();
            } catch (Exception e) {
                logger.warn("Error saving farm to Firestore: {}", e.getMessage());
            }
        }

        FarmResponse response = new FarmResponse(farmId, ownerUid, request.getName().trim(), loc,
                request.getLandArea(), request.getLandAreaUnit().trim().toUpperCase(), status, nowIso, nowIso);
        inMemoryFarms.put(farmId, response);
        return response;
    }

    public FarmResponse updateFarm(String farmId, String ownerUid, FarmRequest request) {
        verifyFarmerRole(ownerUid);
        FarmResponse existing = getFarmById(farmId, ownerUid);
        validateFarmRequest(request);

        String nowIso = Instant.now().toString();
        String status = (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                ? request.getStatus().trim().toUpperCase()
                : existing.getStatus();

        LocationDto loc = request.getLocation();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", request.getName().trim());
        updates.put("location", mapLocationToMap(loc));
        updates.put("landArea", request.getLandArea());
        updates.put("landAreaUnit", request.getLandAreaUnit().trim().toUpperCase());
        updates.put("status", status);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        if (firestore != null) {
            try {
                firestore.collection("farms").document(farmId).set(updates, SetOptions.merge()).get();
            } catch (Exception e) {
                logger.warn("Error updating farm in Firestore: {}", e.getMessage());
            }
        }

        existing.setName(request.getName().trim());
        existing.setLocation(loc);
        existing.setLandArea(request.getLandArea());
        existing.setLandAreaUnit(request.getLandAreaUnit().trim().toUpperCase());
        existing.setStatus(status);
        existing.setUpdatedAt(nowIso);

        inMemoryFarms.put(farmId, existing);
        return existing;
    }

    public void deleteFarm(String farmId, String ownerUid) {
        verifyFarmerRole(ownerUid);
        getFarmById(farmId, ownerUid); // verifies ownership and existence

        if (firestore != null) {
            try {
                firestore.collection("farms").document(farmId).delete().get();

                // Delete associated farm crops
                QuerySnapshot cropsSnap = firestore.collection("farmCrops")
                        .whereEqualTo("farmId", farmId)
                        .get().get();
                if (cropsSnap != null && !cropsSnap.isEmpty()) {
                    for (DocumentSnapshot doc : cropsSnap.getDocuments()) {
                        doc.getReference().delete();
                    }
                }
            } catch (Exception e) {
                logger.warn("Error deleting farm from Firestore: {}", e.getMessage());
            }
        }

        inMemoryFarms.remove(farmId);
        inMemoryFarmCrops.entrySet().removeIf(entry -> entry.getValue().getFarmId().equals(farmId));
    }

    // ==========================================
    // FARM CROP RELATIONSHIP OPERATIONS
    // ==========================================

    public List<FarmCropResponse> getFarmCrops(String farmId, String ownerUid) {
        getFarmById(farmId, ownerUid); // verifies ownership & role
        List<FarmCropResponse> result = new ArrayList<>();

        if (firestore != null) {
            try {
                QuerySnapshot snapshot = firestore.collection("farmCrops")
                        .whereEqualTo("farmId", farmId)
                        .get().get();

                if (snapshot != null && !snapshot.isEmpty()) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        result.add(mapDocToFarmCropResponse(doc));
                    }
                    return result;
                }
            } catch (Exception e) {
                logger.warn("Error fetching farmCrops for farmId {}: {}", farmId, e.getMessage());
            }
        }

        for (FarmCropResponse fc : inMemoryFarmCrops.values()) {
            if (farmId.equals(fc.getFarmId()) && ownerUid.equals(fc.getOwnerUid())) {
                result.add(fc);
            }
        }
        return result;
    }

    public FarmCropResponse addFarmCrop(String farmId, String ownerUid, FarmCropRequest request) {
        getFarmById(farmId, ownerUid); // verifies ownership & role
        validateFarmCropRequest(request);

        CropResponse crop = cropMasterService.getCropById(request.getCropId());
        if (crop == null) {
            throw new IllegalArgumentException("Invalid crop ID. Selected crop does not exist in Crop Master.");
        }

        String season = request.getSeason().trim().toUpperCase();
        String deterministicId = farmId + "_" + crop.getId() + "_" + season;
        String nowIso = Instant.now().toString();
        String status = (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                ? request.getStatus().trim().toUpperCase()
                : "ACTIVE";

        if (firestore != null) {
            try {
                DocumentSnapshot existingDoc = firestore.collection("farmCrops").document(deterministicId).get().get();
                if (existingDoc.exists()) {
                    throw new IllegalArgumentException("Crop relationship already exists for this farm and season (" + season + ").");
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("Error checking duplicate farmCrop: {}", e.getMessage());
            }
        }

        if (inMemoryFarmCrops.containsKey(deterministicId)) {
            throw new IllegalArgumentException("Crop relationship already exists for this farm and season (" + season + ").");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", deterministicId);
        data.put("farmId", farmId);
        data.put("ownerUid", ownerUid);
        data.put("cropId", crop.getId());
        data.put("season", season);
        data.put("status", status);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        if (firestore != null) {
            try {
                firestore.collection("farmCrops").document(deterministicId).set(data).get();
            } catch (Exception e) {
                logger.warn("Error saving farmCrop to Firestore: {}", e.getMessage());
            }
        }

        FarmCropResponse response = new FarmCropResponse(deterministicId, farmId, ownerUid, crop.getId(), crop, season, status, nowIso, nowIso);
        inMemoryFarmCrops.put(deterministicId, response);
        return response;
    }

    public FarmCropResponse updateFarmCrop(String farmId, String farmCropId, String ownerUid, FarmCropRequest request) {
        getFarmById(farmId, ownerUid); // verifies ownership & role
        if (farmCropId == null || farmCropId.trim().isEmpty()) {
            throw new IllegalArgumentException("FarmCrop ID cannot be empty.");
        }

        FarmCropResponse existing = getFarmCropById(farmCropId, ownerUid);
        if (!farmId.equals(existing.getFarmId())) {
            throw new IllegalArgumentException("FarmCrop relationship does not belong to the specified farm.");
        }

        String nowIso = Instant.now().toString();
        String season = (request.getSeason() != null && !request.getSeason().trim().isEmpty())
                ? request.getSeason().trim().toUpperCase()
                : existing.getSeason();
        if (!VALID_SEASONS.contains(season)) {
            throw new IllegalArgumentException("Invalid season. Season must be KHARIF, RABI, or ZAID.");
        }

        String status = (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                ? request.getStatus().trim().toUpperCase()
                : existing.getStatus();
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid status. Status must be ACTIVE or INACTIVE.");
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("season", season);
        updates.put("status", status);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        if (firestore != null) {
            try {
                firestore.collection("farmCrops").document(farmCropId).set(updates, SetOptions.merge()).get();
            } catch (Exception e) {
                logger.warn("Error updating farmCrop in Firestore: {}", e.getMessage());
            }
        }

        existing.setSeason(season);
        existing.setStatus(status);
        existing.setUpdatedAt(nowIso);

        inMemoryFarmCrops.put(farmCropId, existing);
        return existing;
    }

    public void deleteFarmCrop(String farmId, String farmCropId, String ownerUid) {
        getFarmById(farmId, ownerUid); // verifies ownership & role
        FarmCropResponse existing = getFarmCropById(farmCropId, ownerUid);
        if (!farmId.equals(existing.getFarmId())) {
            throw new IllegalArgumentException("FarmCrop relationship does not belong to the specified farm.");
        }

        if (firestore != null) {
            try {
                firestore.collection("farmCrops").document(farmCropId).delete().get();
            } catch (Exception e) {
                logger.warn("Error deleting farmCrop from Firestore: {}", e.getMessage());
            }
        }

        inMemoryFarmCrops.remove(farmCropId);
    }

    // ==========================================
    // HELPERS & VALIDATION
    // ==========================================

    private FarmCropResponse getFarmCropById(String farmCropId, String ownerUid) {
        if (firestore != null) {
            try {
                DocumentSnapshot doc = firestore.collection("farmCrops").document(farmCropId).get().get();
                if (doc.exists()) {
                    String owner = doc.getString("ownerUid");
                    if (!ownerUid.equals(owner)) {
                        throw new AccessDeniedException("Access denied. You do not own this farm-crop relationship.");
                    }
                    return mapDocToFarmCropResponse(doc);
                }
            } catch (AccessDeniedException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("Error fetching farmCrop by ID {}: {}", farmCropId, e.getMessage());
            }
        }

        FarmCropResponse fc = inMemoryFarmCrops.get(farmCropId);
        if (fc == null) {
            throw new NoSuchElementException("FarmCrop relationship not found.");
        }
        if (!ownerUid.equals(fc.getOwnerUid())) {
            throw new AccessDeniedException("Access denied. You do not own this farm-crop relationship.");
        }
        return fc;
    }

    private void validateFarmRequest(FarmRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Farm request cannot be null.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Farm name is required.");
        }
        if (request.getName().trim().length() > 100) {
            throw new IllegalArgumentException("Farm name cannot exceed 100 characters.");
        }
        if (request.getLandArea() == null || request.getLandArea() <= 0) {
            throw new IllegalArgumentException("Land area must be a positive number greater than zero.");
        }
        if (request.getLandAreaUnit() == null || !VALID_UNITS.contains(request.getLandAreaUnit().trim().toUpperCase())) {
            throw new IllegalArgumentException("Land area unit must be ACRE or HECTARE.");
        }

        LocationDto loc = request.getLocation();
        if (loc == null || !loc.isComplete()) {
            throw new IllegalArgumentException("Farm location requires state, district, mandal, and village.");
        }
        if (loc.getPincode() != null && !loc.getPincode().trim().isEmpty()) {
            String pin = loc.getPincode().trim();
            if (!pin.matches("^[1-9][0-9]{5}$")) {
                throw new IllegalArgumentException("Pincode must be a valid 6-digit Indian postal code.");
            }
        }
    }

    private void validateFarmCropRequest(FarmCropRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("FarmCrop request cannot be null.");
        }
        if (request.getCropId() == null || request.getCropId().trim().isEmpty()) {
            throw new IllegalArgumentException("Crop ID is required.");
        }
        if (request.getSeason() == null || !VALID_SEASONS.contains(request.getSeason().trim().toUpperCase())) {
            throw new IllegalArgumentException("Season must be KHARIF, RABI, or ZAID.");
        }
    }

    private FarmResponse mapDocToFarmResponse(DocumentSnapshot doc) {
        String id = doc.getId();
        String ownerUid = doc.getString("ownerUid");
        String name = doc.getString("name");
        Double landArea = doc.getDouble("landArea");
        String landAreaUnit = doc.getString("landAreaUnit");
        String status = doc.getString("status");

        Map<String, Object> locMap = (Map<String, Object>) doc.get("location");
        LocationDto loc = locMap != null ? new LocationDto(
                (String) locMap.get("state"),
                (String) locMap.get("district"),
                (String) locMap.get("mandal"),
                (String) locMap.get("village"),
                (String) locMap.get("pincode")
        ) : new LocationDto();

        String createdAt = doc.get("createdAt") != null ? doc.get("createdAt").toString() : Instant.now().toString();
        String updatedAt = doc.get("updatedAt") != null ? doc.get("updatedAt").toString() : Instant.now().toString();

        return new FarmResponse(id, ownerUid, name, loc, landArea, landAreaUnit, status, createdAt, updatedAt);
    }

    private FarmCropResponse mapDocToFarmCropResponse(DocumentSnapshot doc) {
        String id = doc.getId();
        String farmId = doc.getString("farmId");
        String ownerUid = doc.getString("ownerUid");
        String cropId = doc.getString("cropId");
        String season = doc.getString("season");
        String status = doc.getString("status");

        CropResponse crop = cropMasterService.getCropById(cropId);
        String createdAt = doc.get("createdAt") != null ? doc.get("createdAt").toString() : Instant.now().toString();
        String updatedAt = doc.get("updatedAt") != null ? doc.get("updatedAt").toString() : Instant.now().toString();

        return new FarmCropResponse(id, farmId, ownerUid, cropId, crop, season, status, createdAt, updatedAt);
    }

    private Map<String, Object> mapLocationToMap(LocationDto dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("state", dto.getState().trim());
        map.put("district", dto.getDistrict().trim());
        map.put("mandal", dto.getMandal().trim());
        map.put("village", dto.getVillage().trim());
        map.put("pincode", dto.getPincode() != null ? dto.getPincode().trim() : null);
        return map;
    }
}
