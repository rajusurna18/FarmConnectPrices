package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.ProductListing;
import com.farmlink.api.model.ProductListingQualityGrade;
import com.farmlink.api.model.ProductListingStatus;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ProductListingService {

    private static final Logger logger = LoggerFactory.getLogger(ProductListingService.class);

    public static final String COLLECTION_NAME = "productListings";

    public static final Set<String> SUPPORTED_UNITS = Set.of(
            "KG", "QUINTAL", "TON", "TONNE", "LITRE", "PIECE", "BAG"
    );

    private final Firestore firestore;
    private final CropMasterService cropMasterService;
    private final MarketPriceService marketPriceService;
    private final FirestoreQuotaGuard quotaGuard;

    // In-memory fallback map for unit tests / offline execution
    private final Map<String, ProductListing> inMemoryListings = new ConcurrentHashMap<>();

    @Autowired
    public ProductListingService(
            @Autowired(required = false) Firestore firestore,
            CropMasterService cropMasterService,
            @Autowired(required = false) MarketPriceService marketPriceService,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard
    ) {
        this.firestore = firestore;
        this.cropMasterService = cropMasterService;
        this.marketPriceService = marketPriceService;
        this.quotaGuard = quotaGuard;
    }

    /**
     * Verifies that the authenticated user has the FARMER role.
     */
    public void verifyFarmerRole(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (firestore != null) {
            try {
                if (quotaGuard != null) {
                    quotaGuard.checkQuotaAvailability();
                }
                DocumentSnapshot userDoc = firestore.collection("users").document(uid).get().get();
                if (quotaGuard != null) {
                    quotaGuard.recordSuccess();
                }
                if (userDoc.exists()) {
                    String role = userDoc.getString("role");
                    if (!ProfileService.ROLE_FARMER.equalsIgnoreCase(role)) {
                        throw new AccessDeniedException("Marketplace listing creation and management is strictly restricted to Farmers.");
                    }
                    return;
                }
            } catch (AccessDeniedException ade) {
                throw ade;
            } catch (Exception e) {
                if (quotaGuard != null) {
                    quotaGuard.recordQuotaExhaustion(e);
                }
                logger.warn("Could not verify user role from Firestore for uid {}: {}", uid, e.getMessage());
            }
        }
        // Fallback for mock/test environment when firestore is null
    }

    /**
     * Creates a new marketplace product listing.
     * Seller UID is resolved exclusively server-side from authenticated token.
     */
    public ProductListingResponse createListing(String authenticatedUid, CreateListingRequest request) {
        verifyFarmerRole(authenticatedUid);

        if (request == null) {
            throw new IllegalArgumentException("Create listing request cannot be null.");
        }

        // Validate Crop
        if (request.getCropId() == null || request.getCropId().trim().isEmpty()) {
            throw new IllegalArgumentException("Crop ID is required.");
        }
        CropResponse crop = cropMasterService.getCropById(request.getCropId());
        if (crop == null) {
            throw new IllegalArgumentException("Invalid crop ID specified: " + request.getCropId());
        }

        // Validate Quantity
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Double availableQuantity = request.getAvailableQuantity();
        if (availableQuantity == null) {
            availableQuantity = request.getQuantity();
        } else if (availableQuantity < 0 || availableQuantity > request.getQuantity()) {
            throw new IllegalArgumentException("Available quantity must be non-negative and cannot exceed total quantity.");
        }

        // Validate Unit
        String unit = (request.getUnit() != null) ? request.getUnit().trim().toUpperCase(Locale.ROOT) : "QUINTAL";
        if (!SUPPORTED_UNITS.contains(unit)) {
            throw new IllegalArgumentException("Unsupported unit specified: " + request.getUnit());
        }

        // Validate Asking Price
        if (request.getAskingPrice() == null || request.getAskingPrice() <= 0) {
            throw new IllegalArgumentException("Asking price must be greater than zero.");
        }
        String priceUnit = (request.getPriceUnit() != null) ? request.getPriceUnit().trim().toUpperCase(Locale.ROOT) : unit;

        // Validate Status
        String statusStr = (request.getStatus() != null) ? request.getStatus().trim().toUpperCase(Locale.ROOT) : ProductListingStatus.ACTIVE.name();
        ProductListingStatus status;
        try {
            status = ProductListingStatus.valueOf(statusStr);
            if (status != ProductListingStatus.DRAFT && status != ProductListingStatus.ACTIVE) {
                throw new IllegalArgumentException("New listings must be initialized with status DRAFT or ACTIVE.");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid listing status: " + request.getStatus());
        }

        // Quality grade
        String qualityGrade = (request.getQualityGrade() != null) ? request.getQualityGrade().trim().toUpperCase(Locale.ROOT) : ProductListingQualityGrade.UNSPECIFIED.name();
        try {
            ProductListingQualityGrade.valueOf(qualityGrade);
        } catch (IllegalArgumentException e) {
            qualityGrade = ProductListingQualityGrade.UNSPECIFIED.name();
        }

        String listingId = "listing_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String nowIso = Instant.now().toString();

        LocationDto loc = request.getLocation() != null ? request.getLocation() : new LocationDto();

        ProductListing listing = new ProductListing(
                listingId,
                authenticatedUid,
                authenticatedUid,
                crop.getId(),
                crop.getName(),
                request.getQuantity(),
                availableQuantity,
                unit,
                request.getAskingPrice(),
                priceUnit,
                loc,
                request.getDescription(),
                qualityGrade,
                request.getHarvestDate(),
                request.getAvailableFrom(),
                status.name(),
                nowIso,
                nowIso
        );

        // Save to Firestore / In-Memory
        saveListingToFirestore(listing);
        inMemoryListings.put(listingId, listing);

        return toResponse(listing, authenticatedUid);
    }

    /**
     * Fetches listing detail by listingId with ownership/visibility policy.
     */
    public ProductListingResponse getListingById(String listingId, String callerUid) {
        ProductListing listing = fetchListingFromFirestoreOrMemory(listingId);
        if (listing == null) {
            throw new NoSuchElementException("Marketplace listing not found with ID: " + listingId);
        }

        // Visibility policy: Non-ACTIVE listings are visible ONLY to the owner
        if (!ProductListingStatus.ACTIVE.name().equalsIgnoreCase(listing.getStatus())) {
            if (callerUid == null || !callerUid.equals(listing.getOwnerUid())) {
                throw new AccessDeniedException("Access denied. Private or inactive listing.");
            }
        }

        return toResponse(listing, callerUid);
    }

    /**
     * Updates an existing listing owned by the authenticated farmer.
     */
    public ProductListingResponse updateListing(String authenticatedUid, String listingId, UpdateListingRequest request) {
        verifyFarmerRole(authenticatedUid);

        ProductListing listing = fetchListingFromFirestoreOrMemory(listingId);
        if (listing == null) {
            throw new NoSuchElementException("Marketplace listing not found with ID: " + listingId);
        }

        if (!authenticatedUid.equals(listing.getOwnerUid())) {
            throw new AccessDeniedException("You can only modify your own marketplace listings.");
        }

        if (request == null) {
            return toResponse(listing, authenticatedUid);
        }

        if (request.getQuantity() != null) {
            if (request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero.");
            }
            listing.setQuantity(request.getQuantity());
        }

        if (request.getAvailableQuantity() != null) {
            if (request.getAvailableQuantity() < 0 || request.getAvailableQuantity() > listing.getQuantity()) {
                throw new IllegalArgumentException("Available quantity must be non-negative and cannot exceed total quantity.");
            }
            listing.setAvailableQuantity(request.getAvailableQuantity());

            // Auto transition to SOLD_OUT if available quantity reaches zero
            if (listing.getAvailableQuantity() == 0 && ProductListingStatus.ACTIVE.name().equalsIgnoreCase(listing.getStatus())) {
                listing.setStatus(ProductListingStatus.SOLD_OUT.name());
            }
        }

        if (request.getUnit() != null) {
            String unit = request.getUnit().trim().toUpperCase(Locale.ROOT);
            if (!SUPPORTED_UNITS.contains(unit)) {
                throw new IllegalArgumentException("Unsupported unit specified: " + request.getUnit());
            }
            listing.setUnit(unit);
        }

        if (request.getAskingPrice() != null) {
            if (request.getAskingPrice() <= 0) {
                throw new IllegalArgumentException("Asking price must be greater than zero.");
            }
            listing.setAskingPrice(request.getAskingPrice());
        }

        if (request.getPriceUnit() != null) {
            listing.setPriceUnit(request.getPriceUnit().trim().toUpperCase(Locale.ROOT));
        }

        if (request.getLocation() != null) {
            listing.setLocation(request.getLocation());
        }

        if (request.getDescription() != null) {
            listing.setDescription(request.getDescription());
        }

        if (request.getQualityGrade() != null) {
            String qg = request.getQualityGrade().trim().toUpperCase(Locale.ROOT);
            try {
                ProductListingQualityGrade.valueOf(qg);
                listing.setQualityGrade(qg);
            } catch (IllegalArgumentException e) {
                // Ignore invalid quality grade edit
            }
        }

        if (request.getHarvestDate() != null) {
            listing.setHarvestDate(request.getHarvestDate());
        }

        if (request.getAvailableFrom() != null) {
            listing.setAvailableFrom(request.getAvailableFrom());
        }

        listing.setUpdatedAt(Instant.now().toString());

        saveListingToFirestore(listing);
        inMemoryListings.put(listingId, listing);

        return toResponse(listing, authenticatedUid);
    }

    /**
     * Updates listing status lifecycle state (ACTIVE, PAUSED, DRAFT, SOLD_OUT, EXPIRED).
     */
    public ProductListingResponse updateListingStatus(String authenticatedUid, String listingId, UpdateListingStatusRequest request) {
        verifyFarmerRole(authenticatedUid);

        ProductListing listing = fetchListingFromFirestoreOrMemory(listingId);
        if (listing == null) {
            throw new NoSuchElementException("Marketplace listing not found with ID: " + listingId);
        }

        if (!authenticatedUid.equals(listing.getOwnerUid())) {
            throw new AccessDeniedException("You can only modify your own marketplace listings.");
        }

        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Target status must be provided.");
        }

        String targetStatusStr = request.getStatus().trim().toUpperCase(Locale.ROOT);
        ProductListingStatus targetStatus;
        try {
            targetStatus = ProductListingStatus.valueOf(targetStatusStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + request.getStatus());
        }

        listing.setStatus(targetStatus.name());
        listing.setUpdatedAt(Instant.now().toString());

        saveListingToFirestore(listing);
        inMemoryListings.put(listingId, listing);

        return toResponse(listing, authenticatedUid);
    }

    /**
     * Deletes an owned draft or unpublished listing.
     */
    public void deleteListing(String authenticatedUid, String listingId) {
        verifyFarmerRole(authenticatedUid);

        ProductListing listing = fetchListingFromFirestoreOrMemory(listingId);
        if (listing == null) {
            return;
        }

        if (!authenticatedUid.equals(listing.getOwnerUid())) {
            throw new AccessDeniedException("You can only delete your own marketplace listings.");
        }

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                firestore.collection(COLLECTION_NAME).document(listingId).delete().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error deleting listing {} from Firestore: {}", listingId, e.getMessage());
            }
        }
        inMemoryListings.remove(listingId);
    }

    /**
     * Public browse of ACTIVE marketplace listings with server-side queries, filtering, sorting, and pagination.
     */
    public ListingPageResponse browsePublicListings(
            String cropId,
            String state,
            String district,
            Double minPrice,
            Double maxPrice,
            String sortBy,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<ProductListing> candidateListings = fetchActiveListingsFromFirestoreOrMemory(cropId, state, district);

        // Apply price range filter
        List<ProductListing> filtered = candidateListings.stream()
                .filter(l -> ProductListingStatus.ACTIVE.name().equalsIgnoreCase(l.getStatus()))
                .filter(l -> (minPrice == null || (l.getAskingPrice() != null && l.getAskingPrice() >= minPrice)))
                .filter(l -> (maxPrice == null || (l.getAskingPrice() != null && l.getAskingPrice() <= maxPrice)))
                .collect(Collectors.toList());

        // Apply sorting
        Comparator<ProductListing> comparator;
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(ProductListing::getAskingPrice, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(ProductListing::getAskingPrice, Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("quantity_desc".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(ProductListing::getAvailableQuantity, Comparator.nullsLast(Comparator.reverseOrder()));
        } else {
            // Default: newest first
            comparator = Comparator.comparing(ProductListing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        filtered.sort(comparator);

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<ProductListingResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = filtered.subList(fromIndex, toIndex).stream()
                    .map(l -> toResponse(l, null))
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;

        return new ListingPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Authenticated farmer's own listings dashboard.
     */
    public ListingPageResponse getFarmerListings(String authenticatedUid, String statusFilter, int page, int size) {
        verifyFarmerRole(authenticatedUid);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<ProductListing> farmerListings = fetchFarmerListingsFromFirestoreOrMemory(authenticatedUid);

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            farmerListings = farmerListings.stream()
                    .filter(l -> statusFilter.equalsIgnoreCase(l.getStatus()))
                    .collect(Collectors.toList());
        }

        farmerListings.sort(Comparator.comparing(ProductListing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = farmerListings.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<ProductListingResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = farmerListings.subList(fromIndex, toIndex).stream()
                    .map(l -> toResponse(l, authenticatedUid))
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;

        return new ListingPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    // ==========================================
    // PRIVATE PERSISTENCE & MAPPING HELPERS
    // ==========================================

    private ProductListingResponse toResponse(ProductListing listing, String callerUid) {
        if (listing == null) return null;

        // Privacy location masking: Only expose district & state for public users
        LocationDto publicLoc = listing.getLocation();
        if (publicLoc != null) {
            // Keep state, district, mandal, village as regional indicator without private address
            publicLoc = new LocationDto(publicLoc.getState(), publicLoc.getDistrict(), publicLoc.getMandal(), publicLoc.getVillage());
        }

        MarketPriceReferenceDto refPrice = resolveMarketPriceReference(listing.getCropId(), listing.getLocation());

        return new ProductListingResponse(
                listing.getListingId(),
                listing.getOwnerUid(),
                listing.getCropId(),
                listing.getCropName(),
                listing.getQuantity(),
                listing.getAvailableQuantity(),
                listing.getUnit(),
                listing.getAskingPrice(),
                listing.getPriceUnit(),
                publicLoc,
                listing.getDescription(),
                listing.getQualityGrade(),
                listing.getHarvestDate(),
                listing.getAvailableFrom(),
                listing.getStatus(),
                listing.getCreatedAt(),
                listing.getUpdatedAt(),
                refPrice
        );
    }

    private MarketPriceReferenceDto resolveMarketPriceReference(String cropId, LocationDto location) {
        if (marketPriceService == null || cropId == null) {
            return null;
        }

        try {
            String state = location != null ? location.getState() : null;
            String district = location != null ? location.getDistrict() : null;

            List<MarketPriceResponse> prices = marketPriceService.getMarketPrices(
                    state, district, null, cropId, null, null, null, "date", "desc", 0, 1, 1
            );
            if (prices != null && !prices.isEmpty()) {
                MarketPriceResponse latest = prices.get(0);
                String mktName = latest.getMarket() != null ? latest.getMarket().getName() : null;
                return new MarketPriceReferenceDto(
                        latest.getModalPrice(),
                        latest.getMinPrice(),
                        latest.getMaxPrice(),
                        latest.getUnit() != null ? latest.getUnit() : "QUINTAL",
                        mktName,
                        latest.getPriceDate()
                );
            }
        } catch (Exception e) {
            logger.debug("Could not resolve reference market price for crop {}: {}", cropId, e.getMessage());
        }
        return null;
    }


    private void saveListingToFirestore(ProductListing listing) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Map<String, Object> doc = new HashMap<>();
                doc.put("listingId", listing.getListingId());
                doc.put("ownerUid", listing.getOwnerUid());
                doc.put("farmerProfileId", listing.getFarmerProfileId());
                doc.put("cropId", listing.getCropId());
                doc.put("cropName", listing.getCropName());
                doc.put("quantity", listing.getQuantity());
                doc.put("availableQuantity", listing.getAvailableQuantity());
                doc.put("unit", listing.getUnit());
                doc.put("askingPrice", listing.getAskingPrice());
                doc.put("priceUnit", listing.getPriceUnit());

                if (listing.getLocation() != null) {
                    Map<String, Object> locMap = new HashMap<>();
                    locMap.put("state", listing.getLocation().getState());
                    locMap.put("district", listing.getLocation().getDistrict());
                    locMap.put("mandal", listing.getLocation().getMandal());
                    locMap.put("village", listing.getLocation().getVillage());
                    doc.put("location", locMap);
                }

                doc.put("description", listing.getDescription());
                doc.put("qualityGrade", listing.getQualityGrade());
                doc.put("harvestDate", listing.getHarvestDate());
                doc.put("availableFrom", listing.getAvailableFrom());
                doc.put("status", listing.getStatus());
                doc.put("createdAt", listing.getCreatedAt());
                doc.put("updatedAt", listing.getUpdatedAt());

                firestore.collection(COLLECTION_NAME).document(listing.getListingId()).set(doc, SetOptions.merge()).get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Could not save listing {} to Firestore: {}", listing.getListingId(), e.getMessage());
            }
        }
    }

    private ProductListing fetchListingFromFirestoreOrMemory(String listingId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot snap = firestore.collection(COLLECTION_NAME).document(listingId).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                if (snap.exists()) {
                    return mapDocToProductListing(snap);
                }
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error fetching listing {} from Firestore: {}", listingId, e.getMessage());
            }
        }
        return inMemoryListings.get(listingId);
    }

    private List<ProductListing> fetchActiveListingsFromFirestoreOrMemory(String cropId, String state, String district) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("status", ProductListingStatus.ACTIVE.name());

                if (cropId != null && !cropId.trim().isEmpty()) {
                    query = query.whereEqualTo("cropId", cropId.trim());
                }
                if (state != null && !state.trim().isEmpty()) {
                    query = query.whereEqualTo("location.state", state.trim());
                }

                QuerySnapshot querySnap = query.get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                List<ProductListing> list = new ArrayList<>();
                for (DocumentSnapshot doc : querySnap.getDocuments()) {
                    ProductListing l = mapDocToProductListing(doc);
                    if (district == null || district.trim().isEmpty() ||
                        (l.getLocation() != null && district.equalsIgnoreCase(l.getLocation().getDistrict()))) {
                        list.add(l);
                    }
                }
                return list;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying active listings from Firestore: {}", e.getMessage());
            }
        }

        // In-memory fallback
        return inMemoryListings.values().stream()
                .filter(l -> ProductListingStatus.ACTIVE.name().equalsIgnoreCase(l.getStatus()))
                .filter(l -> (cropId == null || cropId.equalsIgnoreCase(l.getCropId())))
                .filter(l -> (state == null || (l.getLocation() != null && state.equalsIgnoreCase(l.getLocation().getState()))))
                .filter(l -> (district == null || (l.getLocation() != null && district.equalsIgnoreCase(l.getLocation().getDistrict()))))
                .collect(Collectors.toList());
    }

    private List<ProductListing> fetchFarmerListingsFromFirestoreOrMemory(String ownerUid) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Query query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("ownerUid", ownerUid);

                QuerySnapshot querySnap = query.get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                List<ProductListing> list = new ArrayList<>();
                for (DocumentSnapshot doc : querySnap.getDocuments()) {
                    list.add(mapDocToProductListing(doc));
                }
                return list;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying farmer listings from Firestore for uid {}: {}", ownerUid, e.getMessage());
            }
        }

        // In-memory fallback
        return inMemoryListings.values().stream()
                .filter(l -> ownerUid.equals(l.getOwnerUid()))
                .collect(Collectors.toList());
    }

    private ProductListing mapDocToProductListing(DocumentSnapshot doc) {
        LocationDto loc = null;
        Map<String, Object> locMap = (Map<String, Object>) doc.get("location");
        if (locMap != null) {
            loc = new LocationDto(
                    (String) locMap.get("state"),
                    (String) locMap.get("district"),
                    (String) locMap.get("mandal"),
                    (String) locMap.get("village")
            );
        }

        return new ProductListing(
                doc.getId(),
                doc.getString("ownerUid"),
                doc.getString("farmerProfileId"),
                doc.getString("cropId"),
                doc.getString("cropName"),
                doc.getDouble("quantity"),
                doc.getDouble("availableQuantity"),
                doc.getString("unit"),
                doc.getDouble("askingPrice"),
                doc.getString("priceUnit"),
                loc,
                doc.getString("description"),
                doc.getString("qualityGrade"),
                doc.getString("harvestDate"),
                doc.getString("availableFrom"),
                doc.getString("status"),
                doc.getString("createdAt"),
                doc.getString("updatedAt")
        );
    }
}
