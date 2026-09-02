package com.farmlink.api.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class AiDecisionResponse {
    private AiDecisionType decisionType;
    private String summary;
    private String recommendation;
    private AiConfidenceLevel confidence;

    private List<String> verifiedFacts = new ArrayList<>();
    private AiCalculatedMetricsDto calculatedMetrics;
    private List<String> reasoning = new ArrayList<>();
    private List<String> risks = new ArrayList<>();
    private List<String> nextSteps = new ArrayList<>();

    private String engine = "RULE_BASED";
    private boolean aiGenerated = false;

    private AiDataFreshnessDto dataFreshness;
    private List<String> limitations = new ArrayList<>();

    public AiDecisionResponse() {}

    public AiDecisionType getDecisionType() {
        return decisionType;
    }

    public void setDecisionType(AiDecisionType decisionType) {
        this.decisionType = decisionType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public AiConfidenceLevel getConfidence() {
        return confidence;
    }

    public void setConfidence(AiConfidenceLevel confidence) {
        this.confidence = confidence;
    }

    public List<String> getVerifiedFacts() {
        return verifiedFacts;
    }

    public void setVerifiedFacts(List<String> verifiedFacts) {
        this.verifiedFacts = verifiedFacts != null ? verifiedFacts : new ArrayList<>();
    }

    public AiCalculatedMetricsDto getCalculatedMetrics() {
        return calculatedMetrics;
    }

    public void setCalculatedMetrics(AiCalculatedMetricsDto calculatedMetrics) {
        this.calculatedMetrics = calculatedMetrics;
    }

    public List<String> getReasoning() {
        return reasoning;
    }

    public void setReasoning(List<String> reasoning) {
        this.reasoning = reasoning != null ? reasoning : new ArrayList<>();
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks != null ? risks : new ArrayList<>();
    }

    public List<String> getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(List<String> nextSteps) {
        this.nextSteps = nextSteps != null ? nextSteps : new ArrayList<>();
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public boolean isAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public AiDataFreshnessDto getDataFreshness() {
        return dataFreshness;
    }

    public void setDataFreshness(AiDataFreshnessDto dataFreshness) {
        this.dataFreshness = dataFreshness;
    }

    public List<String> getLimitations() {
        return limitations;
    }

    public void setLimitations(List<String> limitations) {
        this.limitations = limitations != null ? limitations : new ArrayList<>();
    }
}
