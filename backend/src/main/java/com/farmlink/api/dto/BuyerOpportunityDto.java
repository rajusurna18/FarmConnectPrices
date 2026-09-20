package com.farmlink.api.dto;

import com.farmlink.api.model.ConfidenceLevel;
import com.farmlink.api.model.MatchReasonCode;
import java.math.BigDecimal;
import java.util.List;

public class BuyerOpportunityDto {
    private String opportunityId;
    private String buyerRole; // MEDIATOR_BUYER or CUSTOMER
    private String buyerRoleDisplay; // "Mediator / Buyer" or "Customer"
    private String anonymizedLocation; // e.g. "Hyderabad, Telangana" or "Rangareddy District"
    private String cropId;
    private String cropName;
    private Double typicalQuantity;
    private String quantityUnit;
    private BigDecimal relevanceScore; // 0.00 to 100.00
    private ConfidenceLevel confidenceLevel;
    private List<MatchReasonCode> matchReasons;
    private String lastActiveAt;

    public BuyerOpportunityDto() {}

    public BuyerOpportunityDto(String opportunityId, String buyerRole, String buyerRoleDisplay,
                               String anonymizedLocation, String cropId, String cropName,
                               Double typicalQuantity, String quantityUnit, BigDecimal relevanceScore,
                               ConfidenceLevel confidenceLevel, List<MatchReasonCode> matchReasons,
                               String lastActiveAt) {
        this.opportunityId = opportunityId;
        this.buyerRole = buyerRole;
        this.buyerRoleDisplay = buyerRoleDisplay;
        this.anonymizedLocation = anonymizedLocation;
        this.cropId = cropId;
        this.cropName = cropName;
        this.typicalQuantity = typicalQuantity;
        this.quantityUnit = quantityUnit;
        this.relevanceScore = relevanceScore;
        this.confidenceLevel = confidenceLevel;
        this.matchReasons = matchReasons;
        this.lastActiveAt = lastActiveAt;
    }

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }

    public String getBuyerRole() { return buyerRole; }
    public void setBuyerRole(String buyerRole) { this.buyerRole = buyerRole; }

    public String getBuyerRoleDisplay() { return buyerRoleDisplay; }
    public void setBuyerRoleDisplay(String buyerRoleDisplay) { this.buyerRoleDisplay = buyerRoleDisplay; }

    public String getAnonymizedLocation() { return anonymizedLocation; }
    public void setAnonymizedLocation(String anonymizedLocation) { this.anonymizedLocation = anonymizedLocation; }

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public Double getTypicalQuantity() { return typicalQuantity; }
    public void setTypicalQuantity(Double typicalQuantity) { this.typicalQuantity = typicalQuantity; }

    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }

    public BigDecimal getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(BigDecimal relevanceScore) { this.relevanceScore = relevanceScore; }

    public ConfidenceLevel getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(ConfidenceLevel confidenceLevel) { this.confidenceLevel = confidenceLevel; }

    public List<MatchReasonCode> getMatchReasons() { return matchReasons; }
    public void setMatchReasons(List<MatchReasonCode> matchReasons) { this.matchReasons = matchReasons; }

    public String getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(String lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}
