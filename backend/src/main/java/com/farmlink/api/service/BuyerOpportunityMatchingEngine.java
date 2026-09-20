package com.farmlink.api.service;

import com.farmlink.api.dto.BuyerOpportunityDto;
import com.farmlink.api.dto.LocationDto;
import com.farmlink.api.dto.ProductListingResponse;
import com.farmlink.api.model.ConfidenceLevel;
import com.farmlink.api.model.MatchReasonCode;
import com.farmlink.api.model.Offer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class BuyerOpportunityMatchingEngine {

    public static final String ROLE_MEDIATOR = "MEDIATOR_BUYER";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private final PriceUnitConversionService unitConversionService;

    public BuyerOpportunityMatchingEngine() {
        this.unitConversionService = new PriceUnitConversionService();
    }

    public BuyerOpportunityMatchingEngine(PriceUnitConversionService unitConversionService) {
        this.unitConversionService = unitConversionService != null ? unitConversionService : new PriceUnitConversionService();
    }

    /**
     * Compute a deterministic BuyerOpportunityDto from a farmer's active listing and a buyer's historical demand offer.
     */
    public BuyerOpportunityDto evaluateOpportunity(
            ProductListingResponse listing,
            Offer buyerOffer,
            LocationDto buyerLocation
    ) {
        if (listing == null || buyerOffer == null) {
            return null;
        }

        List<MatchReasonCode> reasons = new ArrayList<>();
        double cropScore = 0.0;
        double quantityScore = 0.0;
        double locationScore = 0.0;
        double qualityScore = 0.0;

        // 1. Crop Compatibility (40%)
        String listingCropId = listing.getCropId();
        String listingCropName = listing.getCropName() != null ? listing.getCropName().trim() : "";

        String buyerCropId = buyerOffer.getCropId();
        String buyerCropName = buyerOffer.getCropName() != null ? buyerOffer.getCropName().trim() : "";

        boolean cropMatch = (listingCropId != null && listingCropId.equalsIgnoreCase(buyerCropId)) ||
                (!listingCropName.isEmpty() && listingCropName.equalsIgnoreCase(buyerCropName));

        if (cropMatch) {
            cropScore = 40.0;
            reasons.add(MatchReasonCode.CROP_MATCH);
        }

        // 2. Quantity Compatibility (25%)
        Double listingQty = listing.getAvailableQuantity() != null ? listing.getAvailableQuantity() : listing.getQuantity();
        Double buyerQty = buyerOffer.getOfferedQuantity() != null ? buyerOffer.getOfferedQuantity() : buyerOffer.getAgreedQuantity();
        String listingUnit = listing.getUnit();
        String buyerUnit = buyerOffer.getQuantityUnit();

        Double listingQtyKg = normalizeToKg(listingQty, listingUnit);
        Double buyerQtyKg = normalizeToKg(buyerQty, buyerUnit);

        if (listingQtyKg != null && listingQtyKg > 0 && buyerQtyKg != null && buyerQtyKg > 0) {
            double ratio = buyerQtyKg / listingQtyKg;
            if (ratio >= 0.8 && ratio <= 1.5) {
                quantityScore = 25.0;
                reasons.add(MatchReasonCode.QUANTITY_MATCH);
            } else if (ratio >= 0.3 && ratio < 0.8) {
                quantityScore = 15.0;
                reasons.add(MatchReasonCode.PARTIAL_QUANTITY);
            } else {
                quantityScore = 0.0;
                reasons.add(MatchReasonCode.UNKNOWN_QUANTITY);
            }
        } else {
            quantityScore = 0.0;
            reasons.add(MatchReasonCode.UNKNOWN_QUANTITY);
        }

        // 3. Location Compatibility (20%)
        LocationDto listingLoc = listing.getLocation();
        String listingMarket = listing.getReferenceMarketPrice() != null ? listing.getReferenceMarketPrice().getMarketName() : null;

        if (listingLoc != null && buyerLocation != null) {
            String lDistrict = listingLoc.getDistrict() != null ? listingLoc.getDistrict().trim() : "";
            String bDistrict = buyerLocation.getDistrict() != null ? buyerLocation.getDistrict().trim() : "";

            String lState = listingLoc.getState() != null ? listingLoc.getState().trim() : "";
            String bState = buyerLocation.getState() != null ? buyerLocation.getState().trim() : "";

            boolean canonicalMarketMatch = listingMarket != null && !listingMarket.trim().isEmpty() &&
                    listingLoc.getMandal() != null && listingLoc.getMandal().equalsIgnoreCase(buyerLocation.getMandal());

            if (canonicalMarketMatch) {
                locationScore = 20.0;
                reasons.add(MatchReasonCode.SAME_MARKET);
            } else if (!lDistrict.isEmpty() && lDistrict.equalsIgnoreCase(bDistrict)) {
                locationScore = 15.0;
                reasons.add(MatchReasonCode.SAME_DISTRICT);
            } else if (!lState.isEmpty() && lState.equalsIgnoreCase(bState)) {
                locationScore = 10.0;
                reasons.add(MatchReasonCode.SAME_STATE);
            } else {
                locationScore = 0.0;
                reasons.add(MatchReasonCode.UNKNOWN_LOCATION);
            }
        } else {
            locationScore = 0.0;
            reasons.add(MatchReasonCode.UNKNOWN_LOCATION);
        }

        // 4. Quality Grade Compatibility (15%)
        String listingGrade = listing.getQualityGrade();
        if (listingGrade != null && !listingGrade.trim().isEmpty()) {
            qualityScore = 15.0;
            reasons.add(MatchReasonCode.QUALITY_MATCH);
        } else {
            qualityScore = 0.0;
            reasons.add(MatchReasonCode.UNKNOWN_QUALITY);
        }

        double totalScoreDouble = cropScore + quantityScore + locationScore + qualityScore;
        BigDecimal relevanceScore = BigDecimal.valueOf(totalScoreDouble).setScale(2, RoundingMode.HALF_UP);

        // Confidence Classification (Deterministic based strictly on current verified inputs)
        ConfidenceLevel confidenceLevel;
        if (!cropMatch) {
            confidenceLevel = ConfidenceLevel.INSUFFICIENT_DATA;
        } else if (totalScoreDouble >= 75.0 && reasons.contains(MatchReasonCode.QUANTITY_MATCH) && (reasons.contains(MatchReasonCode.SAME_MARKET) || reasons.contains(MatchReasonCode.SAME_DISTRICT)) && reasons.contains(MatchReasonCode.QUALITY_MATCH)) {
            confidenceLevel = ConfidenceLevel.HIGH;
        } else if (totalScoreDouble >= 50.0) {
            confidenceLevel = ConfidenceLevel.MEDIUM;
        } else if (totalScoreDouble >= 40.0) {
            confidenceLevel = ConfidenceLevel.LOW;
        } else {
            confidenceLevel = ConfidenceLevel.INSUFFICIENT_DATA;
        }

        // Buyer Role Display
        String buyerRole = buyerOffer.getBuyerRole() != null ? buyerOffer.getBuyerRole() : ROLE_CUSTOMER;
        String buyerRoleDisplay = ROLE_MEDIATOR.equalsIgnoreCase(buyerRole) ? "Mediator / Buyer" : "Customer";

        // Anonymized Location string
        String anonymizedLocation = buildAnonymizedLocation(buyerLocation);

        // Opportunity ID (Deterministic hash based on buyerUid and cropId)
        String rawOpp = buyerOffer.getBuyerUid() + "_" + (buyerOffer.getCropId() != null ? buyerOffer.getCropId() : "crop");
        String opportunityId = "opp_" + Math.abs(rawOpp.hashCode());

        String lastActiveAt = buyerOffer.getUpdatedAt() != null ? buyerOffer.getUpdatedAt() : buyerOffer.getCreatedAt();

        return new BuyerOpportunityDto(
                opportunityId,
                buyerRole,
                buyerRoleDisplay,
                anonymizedLocation,
                listing.getCropId(),
                listing.getCropName(),
                buyerOffer.getOfferedQuantity(),
                buyerOffer.getQuantityUnit(),
                relevanceScore,
                confidenceLevel,
                reasons,
                lastActiveAt
        );
    }

    private String buildAnonymizedLocation(LocationDto location) {
        if (location == null) {
            return "Regional Market";
        }

        String district = location.getDistrict() != null ? location.getDistrict().trim() : "";
        String state = location.getState() != null ? location.getState().trim() : "";

        if (!district.isEmpty() && !state.isEmpty()) {
            return district + ", " + state;
        } else if (!district.isEmpty()) {
            return district + " District";
        } else if (!state.isEmpty()) {
            return state;
        }

        return "Regional Market";
    }

    private Double normalizeToKg(Double quantity, String unit) {
        if (quantity == null || quantity <= 0 || unit == null) {
            return null;
        }
        String u = unit.trim().toUpperCase(Locale.ROOT);
        if (unitConversionService != null && !unitConversionService.isSupportedUnit(u)) {
            return null;
        }
        if (u.equals(PriceUnitConversionService.UNIT_KG) || u.equals("KGS") || u.equals("KILOGRAM") || u.equals("KILOGRAMS")) {
            return quantity;
        } else if (u.equals(PriceUnitConversionService.UNIT_QUINTAL) || u.equals("QUINTALS") || u.equals("QTL")) {
            return quantity * 100.0;
        } else if (u.equals(PriceUnitConversionService.UNIT_TONNE) || u.equals("TONNES") || u.equals("TON") || u.equals("TONS") || u.equals("MT")) {
            return quantity * 1000.0;
        }
        return null;
    }
}
