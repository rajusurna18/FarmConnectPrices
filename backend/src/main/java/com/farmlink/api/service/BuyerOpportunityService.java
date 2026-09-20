package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuyerOpportunityService {

    private static final Logger logger = LoggerFactory.getLogger(BuyerOpportunityService.class);

    private final Firestore firestore;
    private final ProductListingService listingService;
    private final ProfileService profileService;
    private final BuyerOpportunityMatchingEngine matchingEngine;
    private final FirestoreQuotaGuard quotaGuard;

    @Autowired
    public BuyerOpportunityService(
            @Autowired(required = false) Firestore firestore,
            ProductListingService listingService,
            ProfileService profileService,
            BuyerOpportunityMatchingEngine matchingEngine,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard
    ) {
        this.firestore = firestore;
        this.listingService = listingService;
        this.profileService = profileService;
        this.matchingEngine = matchingEngine;
        this.quotaGuard = quotaGuard;
    }

    /**
     * Get deterministic buyer opportunities for an ACTIVE product listing owned by the authenticated farmer.
     */
    public BuyerOpportunityPageResponse getBuyerOpportunities(String callerUid, String listingId, int page, int size) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        listingService.verifyFarmerRole(callerUid);
        if (listingId == null || listingId.trim().isEmpty()) {
            throw new IllegalArgumentException("Listing ID is required.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        // 1. Fetch & validate authoritative listing
        ProductListingResponse listing = listingService.getListingById(listingId, callerUid);
        if (listing == null) {
            throw new NoSuchElementException("Marketplace listing not found with ID: " + listingId);
        }

        // 2. Validate listing ownership
        if (!callerUid.equals(listing.getOwnerUid())) {
            throw new AccessDeniedException("Unauthorized access to listing opportunities. Caller must be the listing owner.");
        }

        // 3. Validate ACTIVE status
        if (!ProductListingStatus.ACTIVE.name().equalsIgnoreCase(listing.getStatus())) {
            throw new IllegalArgumentException("Buyer opportunities are only available for ACTIVE listings. Current listing status: " + listing.getStatus());
        }

        // 4. Fetch verified buyer-side offer records from Firestore / in-memory
        List<Offer> verifiedBuyerOffers = fetchVerifiedBuyerOffers(listing.getCropId(), listing.getCropName());

        if (verifiedBuyerOffers == null || verifiedBuyerOffers.isEmpty()) {
            return new BuyerOpportunityPageResponse(
                    Collections.emptyList(),
                    safePage,
                    safeSize,
                    0L,
                    0,
                    false,
                    "NO_MATCHES"
            );
        }

        // Group offers by buyerUid to produce deduplicated buyer demand records
        Map<String, List<Offer>> buyerOfferMap = verifiedBuyerOffers.stream()
                .filter(o -> o.getBuyerUid() != null && !o.getBuyerUid().equals(callerUid))
                .filter(o -> {
                    String status = o.getStatus() != null ? o.getStatus().trim().toUpperCase(Locale.ROOT) : "";
                    return !"REJECTED".equals(status) && !"CANCELLED".equals(status) && !"EXPIRED".equals(status);
                })
                .collect(Collectors.groupingBy(Offer::getBuyerUid));

        if (buyerOfferMap.isEmpty()) {
            return new BuyerOpportunityPageResponse(
                    Collections.emptyList(),
                    safePage,
                    safeSize,
                    0L,
                    0,
                    false,
                    "NO_MATCHES"
            );
        }

        List<BuyerOpportunityDto> opportunities = new ArrayList<>();

        for (Map.Entry<String, List<Offer>> entry : buyerOfferMap.entrySet()) {
            String buyerUid = entry.getKey();
            List<Offer> buyerOffers = entry.getValue();

            // Most recent offer representing buyer demand
            Offer latestOffer = buyerOffers.stream()
                    .max(Comparator.comparing(Offer::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(buyerOffers.get(0));

            // Resolve buyer location from profile
            LocationDto buyerLocation = null;
            try {
                ProfileResponse buyerProfile = profileService.getProfile(buyerUid, null, false, null);
                if (buyerProfile != null && buyerProfile.getLocation() != null) {
                    buyerLocation = buyerProfile.getLocation();
                }
            } catch (Exception e) {
                logger.debug("Could not resolve profile location for buyer {}: {}", buyerUid, e.getMessage());
            }

            BuyerOpportunityDto opp = matchingEngine.evaluateOpportunity(
                    listing,
                    latestOffer,
                    buyerLocation
            );

            if (opp != null) {
                opportunities.add(opp);
            }
        }

        // 5. Deterministic Ranking
        // 1. relevanceScore DESC
        // 2. quantity compatibility rank DESC
        // 3. location compatibility rank DESC
        // 4. quality compatibility rank DESC
        // 5. opportunityId ASC (stable tie-breaker)
        opportunities.sort((o1, o2) -> {
            int scoreCompare = o2.getRelevanceScore().compareTo(o1.getRelevanceScore());
            if (scoreCompare != 0) return scoreCompare;

            int qtyCompare = Integer.compare(getQuantityRank(o2), getQuantityRank(o1));
            if (qtyCompare != 0) return qtyCompare;

            int locCompare = Integer.compare(getLocationRank(o2), getLocationRank(o1));
            if (locCompare != 0) return locCompare;

            int qualCompare = Integer.compare(getQualityRank(o2), getQualityRank(o1));
            if (qualCompare != 0) return qualCompare;

            return o1.getOpportunityId().compareTo(o2.getOpportunityId());
        });

        int totalElements = opportunities.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<BuyerOpportunityDto> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = opportunities.subList(fromIndex, toIndex);
        }

        boolean hasNext = (safePage + 1) < totalPages;
        String statusState = totalElements > 0 ? "SUCCESS" : "NO_MATCHES";

        return new BuyerOpportunityPageResponse(
                pageItems,
                safePage,
                safeSize,
                totalElements,
                totalPages,
                hasNext,
                statusState
        );
    }

    private List<Offer> fetchVerifiedBuyerOffers(String cropId, String cropName) {
        List<Offer> results = new ArrayList<>();
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                Query query = firestore.collection(OfferService.COLLECTION_NAME);
                if (cropId != null && !cropId.trim().isEmpty()) {
                    query = query.whereEqualTo("cropId", cropId);
                }

                QuerySnapshot snaps = query.get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();

                for (DocumentSnapshot doc : snaps.getDocuments()) {
                    Offer offer = mapDocToOffer(doc);
                    if (offer != null) {
                        results.add(offer);
                    }
                }
                return results;

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error querying buyer offers from Firestore for buyer opportunities: {}", e.getMessage());
            }
        }

        return results;
    }

    private Offer mapDocToOffer(DocumentSnapshot doc) {
        if (!doc.exists()) return null;
        Offer offer = new Offer();
        offer.setOfferId(doc.getString("offerId"));
        offer.setListingId(doc.getString("listingId"));
        offer.setFarmerId(doc.getString("farmerId"));
        offer.setBuyerUid(doc.getString("buyerUid"));
        offer.setBuyerRole(doc.getString("buyerRole"));
        offer.setCropId(doc.getString("cropId"));
        offer.setCropName(doc.getString("cropName"));
        if (doc.get("offeredQuantity") != null) {
            offer.setOfferedQuantity(((Number) doc.get("offeredQuantity")).doubleValue());
        }
        offer.setQuantityUnit(doc.getString("quantityUnit"));
        offer.setStatus(doc.getString("status"));
        offer.setCreatedAt(doc.getString("createdAt"));
        offer.setUpdatedAt(doc.getString("updatedAt"));
        return offer;
    }

    private int getQuantityRank(BuyerOpportunityDto dto) {
        if (dto.getMatchReasons() == null) return 0;
        if (dto.getMatchReasons().contains(MatchReasonCode.QUANTITY_MATCH)) return 3;
        if (dto.getMatchReasons().contains(MatchReasonCode.PARTIAL_QUANTITY)) return 2;
        return 1;
    }

    private int getLocationRank(BuyerOpportunityDto dto) {
        if (dto.getMatchReasons() == null) return 0;
        if (dto.getMatchReasons().contains(MatchReasonCode.SAME_MARKET)) return 4;
        if (dto.getMatchReasons().contains(MatchReasonCode.SAME_DISTRICT)) return 3;
        if (dto.getMatchReasons().contains(MatchReasonCode.SAME_STATE)) return 2;
        return 1;
    }

    private int getQualityRank(BuyerOpportunityDto dto) {
        if (dto.getMatchReasons() == null) return 0;
        if (dto.getMatchReasons().contains(MatchReasonCode.QUALITY_MATCH)) return 2;
        return 1;
    }
}
