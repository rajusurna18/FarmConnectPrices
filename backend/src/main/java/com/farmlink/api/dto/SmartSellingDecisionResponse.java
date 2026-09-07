package com.farmlink.api.dto;

import com.farmlink.api.dto.ai.AiDecisionResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SmartSellingDecisionResponse {

    private String status; // SUCCESS, PARTIAL, ERROR
    private String message;
    
    private CropResponse crop;
    private double quantity;
    private String quantityUnit;
    private String priceBasis;

    private String recommendedMarketId;
    private String recommendationType; // BEST_ESTIMATED_NET_REALIZATION, BEST_NET_PROFIT, BEST_VERIFIED_CURRENT_PRICE, INSUFFICIENT_DATA
    private String decisionBasis; // COMPLETE_ECONOMIC_NET_REALIZATION, FARM_PROFITABILITY_NET_PROFIT, RAW_VERIFIED_PRICE_ONLY

    // Decoupled Confidence & Quality Breakdown
    private String overallDecisionConfidence; // HIGH, MEDIUM, LOW
    private String forecastConfidence; // HIGH, MEDIUM, LOW, INSUFFICIENT_DATA, UNAVAILABLE
    private String trendDataQuality; // GOOD, LIMITED, INSUFFICIENT
    private String economicsCompleteness; // COMPLETE_FARM_ECONOMICS, COMPLETE_SELLING_COSTS, PARTIAL_SELLING_COSTS, MISSING

    private MarketCard primaryRecommendation;
    private List<MarketCard> rankedMarkets = new ArrayList<>();
    private List<MarketEvaluationResponse> unavailableMarkets = new ArrayList<>();

    private List<TradeOffItem> tradeOffs = new ArrayList<>();
    private List<ForecastScenarioItem> forecastScenarios = new ArrayList<>();

    private AiDecisionResponse aiExplanation;
    private List<String> risks = new ArrayList<>();
    private List<String> nextSteps = new ArrayList<>();
    private List<String> limitations = new ArrayList<>();

    public SmartSellingDecisionResponse() {
    }

    public static SmartSellingDecisionResponse error(String status, String message) {
        SmartSellingDecisionResponse resp = new SmartSellingDecisionResponse();
        resp.setStatus(status);
        resp.setMessage(message);
        resp.setOverallDecisionConfidence("LOW");
        resp.setForecastConfidence("UNAVAILABLE");
        resp.setTrendDataQuality("INSUFFICIENT");
        resp.setEconomicsCompleteness("MISSING");
        return resp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CropResponse getCrop() {
        return crop;
    }

    public void setCrop(CropResponse crop) {
        this.crop = crop;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public String getPriceBasis() {
        return priceBasis;
    }

    public void setPriceBasis(String priceBasis) {
        this.priceBasis = priceBasis;
    }

    public String getRecommendedMarketId() {
        return recommendedMarketId;
    }

    public void setRecommendedMarketId(String recommendedMarketId) {
        this.recommendedMarketId = recommendedMarketId;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
    }

    public String getDecisionBasis() {
        return decisionBasis;
    }

    public void setDecisionBasis(String decisionBasis) {
        this.decisionBasis = decisionBasis;
    }

    public String getOverallDecisionConfidence() {
        return overallDecisionConfidence;
    }

    public void setOverallDecisionConfidence(String overallDecisionConfidence) {
        this.overallDecisionConfidence = overallDecisionConfidence;
    }

    public String getForecastConfidence() {
        return forecastConfidence;
    }

    public void setForecastConfidence(String forecastConfidence) {
        this.forecastConfidence = forecastConfidence;
    }

    public String getTrendDataQuality() {
        return trendDataQuality;
    }

    public void setTrendDataQuality(String trendDataQuality) {
        this.trendDataQuality = trendDataQuality;
    }

    public String getEconomicsCompleteness() {
        return economicsCompleteness;
    }

    public void setEconomicsCompleteness(String economicsCompleteness) {
        this.economicsCompleteness = economicsCompleteness;
    }

    public MarketCard getPrimaryRecommendation() {
        return primaryRecommendation;
    }

    public void setPrimaryRecommendation(MarketCard primaryRecommendation) {
        this.primaryRecommendation = primaryRecommendation;
    }

    public List<MarketCard> getRankedMarkets() {
        return rankedMarkets;
    }

    public void setRankedMarkets(List<MarketCard> rankedMarkets) {
        this.rankedMarkets = rankedMarkets;
    }

    public List<MarketEvaluationResponse> getUnavailableMarkets() {
        return unavailableMarkets;
    }

    public void setUnavailableMarkets(List<MarketEvaluationResponse> unavailableMarkets) {
        this.unavailableMarkets = unavailableMarkets;
    }

    public List<TradeOffItem> getTradeOffs() {
        return tradeOffs;
    }

    public void setTradeOffs(List<TradeOffItem> tradeOffs) {
        this.tradeOffs = tradeOffs;
    }

    public List<ForecastScenarioItem> getForecastScenarios() {
        return forecastScenarios;
    }

    public void setForecastScenarios(List<ForecastScenarioItem> forecastScenarios) {
        this.forecastScenarios = forecastScenarios;
    }

    public AiDecisionResponse getAiExplanation() {
        return aiExplanation;
    }

    public void setAiExplanation(AiDecisionResponse aiExplanation) {
        this.aiExplanation = aiExplanation;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public List<String> getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(List<String> nextSteps) {
        this.nextSteps = nextSteps;
    }

    public List<String> getLimitations() {
        return limitations;
    }

    public void setLimitations(List<String> limitations) {
        this.limitations = limitations;
    }

    // Inner DTO Class: MarketCard
    public static class MarketCard {
        private int rank;
        private MarketSummaryResponse market;
        private BigDecimal selectedPrice;
        private String priceUnit;
        private String priceDate;
        private boolean stalePrice;
        private String costSourceClassification; // VERIFIED_MARKET, SAVED_FARMER_RECORD, USER_ESTIMATE, NOT_VERIFIED

        private BigDecimal grossRevenue;
        private BigDecimal totalSellingCost;
        private BigDecimal estimatedNetRealization;
        private BigDecimal netRealizationPerUnit;

        private BigDecimal totalProductionCost; // null if no farm economics
        private BigDecimal estimatedNetProfit; // null if no farm economics
        private BigDecimal roi; // null if no farm economics
        private String profitabilityStatus; // PROFITABLE, LOSS, BREAK_EVEN, UNKNOWN

        private String trendDirection; // RISING, STABLE, FALLING, UNKNOWN
        private String trendDataQuality; // GOOD, LIMITED, INSUFFICIENT
        private String decisionBasis; // COMPLETE_ECONOMIC_NET_REALIZATION, FARM_PROFITABILITY_NET_PROFIT, RAW_VERIFIED_PRICE_ONLY

        public MarketCard() {
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public MarketSummaryResponse getMarket() {
            return market;
        }

        public void setMarket(MarketSummaryResponse market) {
            this.market = market;
        }

        public BigDecimal getSelectedPrice() {
            return selectedPrice;
        }

        public void setSelectedPrice(BigDecimal selectedPrice) {
            this.selectedPrice = selectedPrice;
        }

        public String getPriceUnit() {
            return priceUnit;
        }

        public void setPriceUnit(String priceUnit) {
            this.priceUnit = priceUnit;
        }

        public String getPriceDate() {
            return priceDate;
        }

        public void setPriceDate(String priceDate) {
            this.priceDate = priceDate;
        }

        public boolean isStalePrice() {
            return stalePrice;
        }

        public void setStalePrice(boolean stalePrice) {
            this.stalePrice = stalePrice;
        }

        public String getCostSourceClassification() {
            return costSourceClassification;
        }

        public void setCostSourceClassification(String costSourceClassification) {
            this.costSourceClassification = costSourceClassification;
        }

        public BigDecimal getGrossRevenue() {
            return grossRevenue;
        }

        public void setGrossRevenue(BigDecimal grossRevenue) {
            this.grossRevenue = grossRevenue;
        }

        public BigDecimal getTotalSellingCost() {
            return totalSellingCost;
        }

        public void setTotalSellingCost(BigDecimal totalSellingCost) {
            this.totalSellingCost = totalSellingCost;
        }

        public BigDecimal getEstimatedNetRealization() {
            return estimatedNetRealization;
        }

        public void setEstimatedNetRealization(BigDecimal estimatedNetRealization) {
            this.estimatedNetRealization = estimatedNetRealization;
        }

        public BigDecimal getNetRealizationPerUnit() {
            return netRealizationPerUnit;
        }

        public void setNetRealizationPerUnit(BigDecimal netRealizationPerUnit) {
            this.netRealizationPerUnit = netRealizationPerUnit;
        }

        public BigDecimal getTotalProductionCost() {
            return totalProductionCost;
        }

        public void setTotalProductionCost(BigDecimal totalProductionCost) {
            this.totalProductionCost = totalProductionCost;
        }

        public BigDecimal getEstimatedNetProfit() {
            return estimatedNetProfit;
        }

        public void setEstimatedNetProfit(BigDecimal estimatedNetProfit) {
            this.estimatedNetProfit = estimatedNetProfit;
        }

        public BigDecimal getRoi() {
            return roi;
        }

        public void setRoi(BigDecimal roi) {
            this.roi = roi;
        }

        public String getProfitabilityStatus() {
            return profitabilityStatus;
        }

        public void setProfitabilityStatus(String profitabilityStatus) {
            this.profitabilityStatus = profitabilityStatus;
        }

        public String getTrendDirection() {
            return trendDirection;
        }

        public void setTrendDirection(String trendDirection) {
            this.trendDirection = trendDirection;
        }

        public String getTrendDataQuality() {
            return trendDataQuality;
        }

        public void setTrendDataQuality(String trendDataQuality) {
            this.trendDataQuality = trendDataQuality;
        }

        public String getDecisionBasis() {
            return decisionBasis;
        }

        public void setDecisionBasis(String decisionBasis) {
            this.decisionBasis = decisionBasis;
        }
    }

    // Inner DTO Class: TradeOffItem
    public static class TradeOffItem {
        private String marketIdA;
        private String marketNameA;
        private String marketIdB;
        private String marketNameB;
        private String tradeOffType; // HIGHER_PRICE_HIGHER_TRANSPORT, LOWER_PRICE_LOWER_TRANSPORT, STALE_PRICE_HIGHER_MARGIN, TREND_DIVERGENCE, FORECAST_UPSIDE_RISK
        private String title;
        private String description;
        private BigDecimal calculatedConsequence; // Actual net realization / profit difference

        public TradeOffItem() {
        }

        public TradeOffItem(String marketIdA, String marketNameA, String marketIdB, String marketNameB, String tradeOffType, String title, String description, BigDecimal calculatedConsequence) {
            this.marketIdA = marketIdA;
            this.marketNameA = marketNameA;
            this.marketIdB = marketIdB;
            this.marketNameB = marketNameB;
            this.tradeOffType = tradeOffType;
            this.title = title;
            this.description = description;
            this.calculatedConsequence = calculatedConsequence;
        }

        public String getMarketIdA() {
            return marketIdA;
        }

        public void setMarketIdA(String marketIdA) {
            this.marketIdA = marketIdA;
        }

        public String getMarketNameA() {
            return marketNameA;
        }

        public void setMarketNameA(String marketNameA) {
            this.marketNameA = marketNameA;
        }

        public String getMarketIdB() {
            return marketIdB;
        }

        public void setMarketIdB(String marketIdB) {
            this.marketIdB = marketIdB;
        }

        public String getMarketNameB() {
            return marketNameB;
        }

        public void setMarketNameB(String marketNameB) {
            this.marketNameB = marketNameB;
        }

        public String getTradeOffType() {
            return tradeOffType;
        }

        public void setTradeOffType(String tradeOffType) {
            this.tradeOffType = tradeOffType;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getCalculatedConsequence() {
            return calculatedConsequence;
        }

        public void setCalculatedConsequence(BigDecimal calculatedConsequence) {
            this.calculatedConsequence = calculatedConsequence;
        }
    }

    // Inner DTO Class: ForecastScenarioItem
    public static class ForecastScenarioItem {
        private String marketId;
        private String marketName;
        private String horizon; // 1_DAY, 3_DAYS, 7_DAYS, 14_DAYS
        private String direction; // RISING, FALLING, STABLE, UNCERTAIN
        private String confidence; // HIGH, MEDIUM, LOW, INSUFFICIENT_DATA
        private boolean isScenario = true;

        private Double currentVerifiedPrice;
        private Double expectedPrice;
        private Double bearPrice; // lower bound
        private Double bullPrice; // upper bound

        private BigDecimal expectedNetRealization;
        private BigDecimal bearNetRealization;
        private BigDecimal bullNetRealization;

        public ForecastScenarioItem() {
        }

        public String getMarketId() {
            return marketId;
        }

        public void setMarketId(String marketId) {
            this.marketId = marketId;
        }

        public String getMarketName() {
            return marketName;
        }

        public void setMarketName(String marketName) {
            this.marketName = marketName;
        }

        public String getHorizon() {
            return horizon;
        }

        public void setHorizon(String horizon) {
            this.horizon = horizon;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public boolean isScenario() {
            return isScenario;
        }

        public void setScenario(boolean scenario) {
            isScenario = scenario;
        }

        public Double getCurrentVerifiedPrice() {
            return currentVerifiedPrice;
        }

        public void setCurrentVerifiedPrice(Double currentVerifiedPrice) {
            this.currentVerifiedPrice = currentVerifiedPrice;
        }

        public Double getExpectedPrice() {
            return expectedPrice;
        }

        public void setExpectedPrice(Double expectedPrice) {
            this.expectedPrice = expectedPrice;
        }

        public Double getBearPrice() {
            return bearPrice;
        }

        public void setBearPrice(Double bearPrice) {
            this.bearPrice = bearPrice;
        }

        public Double getBullPrice() {
            return bullPrice;
        }

        public void setBullPrice(Double bullPrice) {
            this.bullPrice = bullPrice;
        }

        public BigDecimal getExpectedNetRealization() {
            return expectedNetRealization;
        }

        public void setExpectedNetRealization(BigDecimal expectedNetRealization) {
            this.expectedNetRealization = expectedNetRealization;
        }

        public BigDecimal getBearNetRealization() {
            return bearNetRealization;
        }

        public void setBearNetRealization(BigDecimal bearNetRealization) {
            this.bearNetRealization = bearNetRealization;
        }

        public BigDecimal getBullNetRealization() {
            return bullNetRealization;
        }

        public void setBullNetRealization(BigDecimal bullNetRealization) {
            this.bullNetRealization = bullNetRealization;
        }
    }
}
