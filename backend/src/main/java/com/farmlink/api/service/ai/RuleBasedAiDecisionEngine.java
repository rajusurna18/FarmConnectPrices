package com.farmlink.api.service.ai;

import com.farmlink.api.dto.*;
import com.farmlink.api.dto.ai.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RuleBasedAiDecisionEngine implements AiDecisionEngine {
    private static final Logger log = LoggerFactory.getLogger(RuleBasedAiDecisionEngine.class);

    @Override
    public AiDecisionResponse generateDecision(AiDecisionRequest request, AiDecisionContext context) {
        AiDecisionResponse response = new AiDecisionResponse();
        response.setDecisionType(request.getDecisionType());
        response.setEngine("RULE_BASED");
        response.setAiGenerated(false);

        // Data Freshness Metadata
        AiDataFreshnessDto freshness = new AiDataFreshnessDto();
        freshness.setMarketPriceDate(context.getPriceDate());
        freshness.setStalePrice(context.isHasStalePrice());
        if (context.isHasVerifiedPrice()) {
            freshness.setStatus(context.isHasStalePrice() ? "STALE" : "CURRENT");
            freshness.setStaleMessage(context.isHasStalePrice() ?
                    "Based on the latest available verified market price date (" + context.getPriceDate() + ")." :
                    "Based on current verified market price date (" + context.getPriceDate() + ").");
        } else {
            freshness.setStatus("MISSING");
            freshness.setStaleMessage("No verified market price available for the specified crop/markets.");
        }
        response.setDataFreshness(freshness);

        // Route by Decision Type
        switch (request.getDecisionType()) {
            case MARKET_SELECTION:
                handleMarketSelection(request, context, response);
                break;
            case PROFITABILITY_EXPLANATION:
                handleProfitabilityExplanation(request, context, response);
                break;
            case MARKET_COMPARISON_EXPLANATION:
                handleMarketComparisonExplanation(request, context, response);
                break;
            case SELLING_DECISION_SUPPORT:
                handleSellingDecisionSupport(request, context, response);
                break;
            default:
                handleMarketSelection(request, context, response);
                break;
        }

        return response;
    }

    private void handleMarketSelection(AiDecisionRequest request, AiDecisionContext context, AiDecisionResponse response) {
        List<MarketEvaluationResponse> evals = context.getMarketEvaluations();

        if (evals == null || evals.isEmpty()) {
            response.setSummary("Insufficient market price data to evaluate market selection.");
            response.setRecommendation("No recommendation available");
            response.setConfidence(AiConfidenceLevel.LOW);
            response.getVerifiedFacts().add("No verified market price observations were found for the selected markets.");
            response.getRisks().add("Decisions cannot be made without verified price data.");
            response.getNextSteps().add("Check back when market prices are updated or select alternate markets.");
            return;
        }

        // Sort by estimatedNetRealization descending using existing Module 09 calculations
        List<MarketEvaluationResponse> validEvals = evals.stream()
                .filter(e -> "SUCCESS".equalsIgnoreCase(e.getStatus()) && e.getEstimatedNetRealization() != null)
                .sorted(Comparator.comparing(MarketEvaluationResponse::getEstimatedNetRealization).reversed())
                .toList();

        if (validEvals.isEmpty()) {
            MarketEvaluationResponse failed = evals.get(0);
            response.setSummary("Market selection evaluation unprocessable: " + failed.getMessage());
            response.setRecommendation("Verify input parameters or market selections.");
            response.setConfidence(AiConfidenceLevel.LOW);
            response.getVerifiedFacts().add("Status: " + failed.getStatus() + " - " + failed.getMessage());
            return;
        }

        MarketEvaluationResponse best = validEvals.get(0);
        String cropName = context.getCrop() != null ? context.getCrop().getName() : "Crop";
        String bestMarketName = best.getMarket() != null ? best.getMarket().getName() : "Selected Market";

        response.setSummary("Based on current verified prices and estimated net realization, " + bestMarketName + " currently appears most favorable for " + cropName + ".");
        response.setRecommendation(bestMarketName);

        // Populate verified facts from Module 09 evaluation
        List<String> facts = new ArrayList<>();
        if (best.getSelectedPrice() != null) {
            facts.add("Verified modal price at " + bestMarketName + ": ₹" + best.getSelectedPrice() + " per " + (best.getPriceUnit() != null ? best.getPriceUnit() : "QUINTAL"));
        }
        BigDecimal transCost = best.getTransportationCost();
        BigDecimal otherCost = best.getOtherSellingCosts();
        facts.add("Transportation cost: ₹" + (transCost != null ? transCost : 0) + ", Other selling costs: ₹" + (otherCost != null ? otherCost : 0));
        facts.add("Evaluated quantity: " + best.getQuantity() + " " + best.getQuantityUnit());
        response.setVerifiedFacts(facts);

        // Attach calculated metrics directly from existing Module 09 evaluation
        AiCalculatedMetricsDto metrics = new AiCalculatedMetricsDto();
        metrics.setSelectedPrice(best.getSelectedPrice());
        metrics.setExpectedYield(best.getQuantity());
        metrics.setYieldUnit(best.getQuantityUnit());
        metrics.setGrossRevenue(best.getGrossRevenue());
        metrics.setTotalSellingCost(best.getTotalSellingCosts());
        metrics.setEstimatedNetRealization(best.getEstimatedNetRealization());

        // If Module 10 farm economics evaluation is present, attach production cost & profit metrics
        if (context.getProfitabilityEvaluation() != null && "SUCCESS".equalsIgnoreCase(context.getProfitabilityEvaluation().getStatus())) {
            FarmProfitabilityEvaluationResponse prof = context.getProfitabilityEvaluation();
            metrics.setTotalProductionCost(prof.getTotalProductionCost());
            metrics.setTotalCost(prof.getTotalCost());
            metrics.setEstimatedProfit(prof.getEstimatedProfit());
            metrics.setProfitPerUnit(prof.getProfitPerUnit());
            metrics.setProductionCostPerUnit(prof.getProductionCostPerUnit());
            metrics.setTotalCostPerUnit(prof.getTotalCostPerUnit());
            metrics.setBreakEvenSellingPrice(prof.getBreakEvenSellingPrice());
            metrics.setRoi(prof.getRoi());
            metrics.setProfitabilityStatus(prof.getProfitabilityStatus());
        }
        response.setCalculatedMetrics(metrics);

        // Reasoning
        List<String> reasoning = new ArrayList<>();
        reasoning.add(bestMarketName + " offers an estimated net realization of ₹" + best.getEstimatedNetRealization() + " for " + best.getQuantity() + " " + best.getQuantityUnit() + ".");

        if (validEvals.size() > 1) {
            MarketEvaluationResponse runnerUp = validEvals.get(1);
            String runnerUpName = runnerUp.getMarket() != null ? runnerUp.getMarket().getName() : "Second Market";
            BigDecimal diff = best.getEstimatedNetRealization().subtract(runnerUp.getEstimatedNetRealization());
            reasoning.add("Outperforms " + runnerUpName + " by an estimated net realization advantage of ₹" + diff + ".");
        }
        response.setReasoning(reasoning);

        // Risks
        List<String> risks = new ArrayList<>();
        if (best.isStalePrice()) {
            risks.add("The latest price is from " + best.getPriceDate() + ". Prices may have changed at the mandi.");
        }
        risks.add("Actual transportation costs can fluctuate depending on vehicle availability and fuel rates.");
        response.setRisks(risks);

        // Next Steps
        List<String> nextSteps = new ArrayList<>();
        nextSteps.add("Contact mandi representative or local trader at " + bestMarketName + " to confirm today's arrival prices.");
        nextSteps.add("Confirm transportation availability and fixed freight charges.");
        nextSteps.add("Verify crop moisture and grading standards before loading.");
        response.setNextSteps(nextSteps);
    }

    private void handleProfitabilityExplanation(AiDecisionRequest request, AiDecisionContext context, AiDecisionResponse response) {
        FarmProfitabilityEvaluationResponse prof = context.getProfitabilityEvaluation();

        if (prof == null || !"SUCCESS".equalsIgnoreCase(prof.getStatus())) {
            response.setSummary("Farm economics and profitability data unavailable for this record.");
            response.setRecommendation("Enter production costs to view profitability explanation.");
            response.setConfidence(AiConfidenceLevel.LOW);
            response.getVerifiedFacts().add("No farm economic record or verified market price available.");
            return;
        }

        String cropName = prof.getCrop() != null ? prof.getCrop().getName() : "Crop";
        String status = prof.getProfitabilityStatus() != null ? prof.getProfitabilityStatus() : "UNKNOWN";
        BigDecimal profit = prof.getEstimatedProfit() != null ? prof.getEstimatedProfit() : BigDecimal.ZERO;

        response.setSummary("Farm profitability analysis for " + cropName + ": Current operation is estimated to be " + status + " with an estimated profit of ₹" + profit + ".");
        response.setRecommendation("Maintain cost control and monitor mandi prices above break-even (₹" + prof.getBreakEvenSellingPrice() + " per " + prof.getYieldUnit() + ").");

        // Verified facts
        List<String> facts = new ArrayList<>();
        facts.add("Expected yield: " + prof.getExpectedYield() + " " + prof.getYieldUnit());
        facts.add("Total production cost: ₹" + prof.getTotalProductionCost() + ", Total selling cost: ₹" + prof.getTotalSellingCost());
        facts.add("Break-even selling price: ₹" + prof.getBreakEvenSellingPrice() + " per " + prof.getYieldUnit());
        response.setVerifiedFacts(facts);

        // Calculated metrics directly from Module 10 evaluation
        AiCalculatedMetricsDto metrics = new AiCalculatedMetricsDto();
        metrics.setSelectedPrice(prof.getSelectedPrice());
        metrics.setExpectedYield(prof.getExpectedYield());
        metrics.setYieldUnit(prof.getYieldUnit());
        metrics.setGrossRevenue(prof.getGrossRevenue());
        metrics.setTotalProductionCost(prof.getTotalProductionCost());
        metrics.setTotalSellingCost(prof.getTotalSellingCost());
        metrics.setTotalCost(prof.getTotalCost());
        metrics.setEstimatedNetRealization(prof.getEstimatedNetRealization());
        metrics.setEstimatedProfit(prof.getEstimatedProfit());
        metrics.setProfitPerUnit(prof.getProfitPerUnit());
        metrics.setProductionCostPerUnit(prof.getProductionCostPerUnit());
        metrics.setTotalCostPerUnit(prof.getTotalCostPerUnit());
        metrics.setBreakEvenSellingPrice(prof.getBreakEvenSellingPrice());
        metrics.setRoi(prof.getRoi());
        metrics.setProfitabilityStatus(prof.getProfitabilityStatus());
        response.setCalculatedMetrics(metrics);

        // Reasoning
        List<String> reasoning = new ArrayList<>();
        reasoning.add("Gross revenue of ₹" + prof.getGrossRevenue() + " covers total production cost (₹" + prof.getTotalProductionCost() + ") and selling cost (₹" + prof.getTotalSellingCost() + ").");
        reasoning.add("Break-even selling price is ₹" + prof.getBreakEvenSellingPrice() + " per " + prof.getYieldUnit() + ". Selling above this price yields positive profit.");
        if (prof.getRoi() != null) {
            reasoning.add("Estimated Return on Investment (ROI) is " + prof.getRoi() + "%.");
        }
        response.setReasoning(reasoning);

        // Risks
        List<String> risks = new ArrayList<>();
        risks.add("Actual harvested yield may differ from expected yield of " + prof.getExpectedYield() + " " + prof.getYieldUnit() + ".");
        risks.add("Unanticipated post-harvest labor or transport delays can increase selling costs.");
        response.setRisks(risks);

        // Next Steps
        List<String> nextSteps = new ArrayList<>();
        nextSteps.add("Track daily mandi price trends to target sales when market price is comfortably above break-even.");
        nextSteps.add("Explore bulk transport sharing with nearby farmers to reduce per-unit selling cost.");
        response.setNextSteps(nextSteps);
    }

    private void handleMarketComparisonExplanation(AiDecisionRequest request, AiDecisionContext context, AiDecisionResponse response) {
        List<MarketEvaluationResponse> evals = context.getMarketEvaluations();

        if (evals == null || evals.size() < 2) {
            response.setSummary("At least two valid candidate markets are required to perform market comparison explanation.");
            response.setRecommendation("Select two or more markets for comparison.");
            response.setConfidence(AiConfidenceLevel.LOW);
            return;
        }

        List<MarketEvaluationResponse> validEvals = evals.stream()
                .filter(e -> "SUCCESS".equalsIgnoreCase(e.getStatus()) && e.getEstimatedNetRealization() != null)
                .sorted(Comparator.comparing(MarketEvaluationResponse::getEstimatedNetRealization).reversed())
                .toList();

        if (validEvals.size() < 2) {
            response.setSummary("Verified market price data is unavailable for multiple selected markets.");
            response.setRecommendation("Select markets with verified price observations.");
            response.setConfidence(AiConfidenceLevel.LOW);
            return;
        }

        MarketEvaluationResponse m1 = validEvals.get(0);
        MarketEvaluationResponse m2 = validEvals.get(1);

        String name1 = m1.getMarket() != null ? m1.getMarket().getName() : "Market 1";
        String name2 = m2.getMarket() != null ? m2.getMarket().getName() : "Market 2";
        BigDecimal diff = m1.getEstimatedNetRealization().subtract(m2.getEstimatedNetRealization());

        response.setSummary(name1 + " provides a higher estimated net realization than " + name2 + " by ₹" + diff + " for " + m1.getQuantity() + " " + m1.getQuantityUnit() + ".");
        response.setRecommendation(name1);

        // Verified Facts
        List<String> facts = new ArrayList<>();
        facts.add(name1 + ": Modal Price = ₹" + m1.getSelectedPrice() + ", Selling Cost = ₹" + m1.getTotalSellingCosts() + ", Net Realization = ₹" + m1.getEstimatedNetRealization());
        facts.add(name2 + ": Modal Price = ₹" + m2.getSelectedPrice() + ", Selling Cost = ₹" + m2.getTotalSellingCosts() + ", Net Realization = ₹" + m2.getEstimatedNetRealization());
        response.setVerifiedFacts(facts);

        // Reasoning
        List<String> reasoning = new ArrayList<>();
        BigDecimal priceDiff = m1.getSelectedPrice().subtract(m2.getSelectedPrice());
        if (priceDiff.compareTo(BigDecimal.ZERO) > 0) {
            reasoning.add(name1 + " has a higher verified modal price (+₹" + priceDiff + " per " + m1.getPriceUnit() + ").");
        } else if (priceDiff.compareTo(BigDecimal.ZERO) < 0) {
            reasoning.add(name2 + " has a higher modal price (+₹" + priceDiff.abs() + "), but lower selling costs make " + name1 + " more profitable overall.");
        } else {
            reasoning.add("Both markets have identical modal prices (₹" + m1.getSelectedPrice() + "), but " + name1 + " has lower selling costs.");
        }

        BigDecimal costDiff = m1.getTotalSellingCosts().subtract(m2.getTotalSellingCosts());
        if (costDiff.compareTo(BigDecimal.ZERO) < 0) {
            reasoning.add(name1 + " incurs ₹" + costDiff.abs() + " less in total selling costs compared to " + name2 + ".");
        }
        response.setReasoning(reasoning);

        // Risks
        List<String> risks = new ArrayList<>();
        risks.add("Transport distance and road conditions to " + name1 + " vs " + name2 + " may affect transit time and crop freshness.");
        response.setRisks(risks);

        // Next Steps
        List<String> nextSteps = new ArrayList<>();
        nextSteps.add("Confirm actual transport quotes for both " + name1 + " and " + name2 + " before departure.");
        nextSteps.add("Check arrival volumes at both mandis to avoid heavy congestion.");
        response.setNextSteps(nextSteps);
    }

    private void handleSellingDecisionSupport(AiDecisionRequest request, AiDecisionContext context, AiDecisionResponse response) {
        String cropName = context.getCrop() != null ? context.getCrop().getName() : "your crop";

        response.setSummary("Selling decision support checklist for " + cropName + ": Evaluate price freshness, logistics cost, and break-even selling price before transaction.");
        response.setRecommendation("Verify today's live mandi arrival prices before finalizing harvest sale.");

        List<String> facts = new ArrayList<>();
        if (context.getPriceDate() != null) {
            facts.add("Latest verified price date on record: " + context.getPriceDate());
        }
        if (context.getQuantity() != null) {
            facts.add("Target sale quantity: " + context.getQuantity() + " " + (context.getQuantityUnit() != null ? context.getQuantityUnit() : "QUINTAL"));
        }
        response.setVerifiedFacts(facts);

        List<String> reasoning = new ArrayList<>();
        reasoning.add("Selling decisions should balance verified market prices against transportation and handling costs.");
        if (context.getProfitabilityEvaluation() != null && context.getProfitabilityEvaluation().getBreakEvenSellingPrice() != null) {
            reasoning.add("Your break-even price is ₹" + context.getProfitabilityEvaluation().getBreakEvenSellingPrice() + ". Any market price above this covers full production & selling expenses.");
        }
        response.setReasoning(reasoning);

        List<String> risks = new ArrayList<>();
        risks.add("Prices can fluctuate rapidly during peak harvest arrivals.");
        risks.add("Delaying sale for higher prices risks crop moisture loss or storage degradation.");
        response.setRisks(risks);

        List<String> nextSteps = new ArrayList<>();
        nextSteps.add("Compare prices across 2-3 nearby mandis.");
        nextSteps.add("Negotiate transport costs upfront with local logistics providers.");
        nextSteps.add("Ensure produce meets mandi quality and moisture standards.");
        response.setNextSteps(nextSteps);
    }
}
