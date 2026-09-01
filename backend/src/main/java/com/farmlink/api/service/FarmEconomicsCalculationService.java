package com.farmlink.api.service;

import com.farmlink.api.dto.ProductionCostCategory;
import com.farmlink.api.dto.ProductionCostItemDto;
import com.farmlink.api.dto.SellingCostsDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class FarmEconomicsCalculationService {

    public static final String STATUS_PROFITABLE = "PROFITABLE";
    public static final String STATUS_BREAK_EVEN = "BREAK_EVEN";
    public static final String STATUS_LOSS = "LOSS";
    public static final String STATUS_INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

    public static final String COMPLETENESS_COMPLETE = "COMPLETE";
    public static final String COMPLETENESS_PARTIAL = "PARTIAL";

    public static class CalculationResult {
        private final BigDecimal totalProductionCost;
        private final BigDecimal totalSellingCost;
        private final BigDecimal totalCost;
        private final BigDecimal grossRevenue;
        private final BigDecimal estimatedNetRealization;
        private final BigDecimal estimatedProfit;
        private final BigDecimal profitPerUnit;
        private final BigDecimal productionCostPerUnit;
        private final BigDecimal totalCostPerUnit;
        private final BigDecimal breakEvenSellingPrice;
        private final BigDecimal roi;
        private final String profitabilityStatus;
        private final String costCompleteness;

        public CalculationResult(
                BigDecimal totalProductionCost,
                BigDecimal totalSellingCost,
                BigDecimal totalCost,
                BigDecimal grossRevenue,
                BigDecimal estimatedNetRealization,
                BigDecimal estimatedProfit,
                BigDecimal profitPerUnit,
                BigDecimal productionCostPerUnit,
                BigDecimal totalCostPerUnit,
                BigDecimal breakEvenSellingPrice,
                BigDecimal roi,
                String profitabilityStatus,
                String costCompleteness
        ) {
            this.totalProductionCost = totalProductionCost;
            this.totalSellingCost = totalSellingCost;
            this.totalCost = totalCost;
            this.grossRevenue = grossRevenue;
            this.estimatedNetRealization = estimatedNetRealization;
            this.estimatedProfit = estimatedProfit;
            this.profitPerUnit = profitPerUnit;
            this.productionCostPerUnit = productionCostPerUnit;
            this.totalCostPerUnit = totalCostPerUnit;
            this.breakEvenSellingPrice = breakEvenSellingPrice;
            this.roi = roi;
            this.profitabilityStatus = profitabilityStatus;
            this.costCompleteness = costCompleteness;
        }

        public BigDecimal getTotalProductionCost() { return totalProductionCost; }
        public BigDecimal getTotalSellingCost() { return totalSellingCost; }
        public BigDecimal getTotalCost() { return totalCost; }
        public BigDecimal getGrossRevenue() { return grossRevenue; }
        public BigDecimal getEstimatedNetRealization() { return estimatedNetRealization; }
        public BigDecimal getEstimatedProfit() { return estimatedProfit; }
        public BigDecimal getProfitPerUnit() { return profitPerUnit; }
        public BigDecimal getProductionCostPerUnit() { return productionCostPerUnit; }
        public BigDecimal getTotalCostPerUnit() { return totalCostPerUnit; }
        public BigDecimal getBreakEvenSellingPrice() { return breakEvenSellingPrice; }
        public BigDecimal getRoi() { return roi; }
        public String getProfitabilityStatus() { return profitabilityStatus; }
        public String getCostCompleteness() { return costCompleteness; }
    }

    public BigDecimal calculateTotalProductionCost(List<ProductionCostItemDto> productionCosts) {
        BigDecimal sum = BigDecimal.ZERO;
        if (productionCosts != null) {
            for (ProductionCostItemDto item : productionCosts) {
                if (item == null) continue;
                if (item.getCategory() != null) {
                    ProductionCostCategory.fromString(item.getCategory()); // validates category
                }
                if (item.getAmount() != null) {
                    if (item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Production cost amount cannot be negative for category: " + item.getCategory());
                    }
                    sum = sum.add(item.getAmount());
                }
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalSellingCost(SellingCostsDto sellingCosts) {
        if (sellingCosts == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal transport = sellingCosts.getTransportationCost() != null ? sellingCosts.getTransportationCost() : BigDecimal.ZERO;
        BigDecimal other = sellingCosts.getOtherSellingCosts() != null ? sellingCosts.getOtherSellingCosts() : BigDecimal.ZERO;

        if (transport.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transportation cost cannot be negative.");
        }
        if (other.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Other selling costs cannot be negative.");
        }

        return transport.add(other).setScale(2, RoundingMode.HALF_UP);
    }

    public String determineCostCompleteness(List<ProductionCostItemDto> productionCosts) {
        if (productionCosts == null || productionCosts.isEmpty()) {
            return COMPLETENESS_PARTIAL;
        }
        long nonZeroCount = productionCosts.stream()
                .filter(item -> item != null && item.getAmount() != null && item.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();
        // If farmer has entered at least 3 distinct non-zero categories, consider complete
        return nonZeroCount >= 3 ? COMPLETENESS_COMPLETE : COMPLETENESS_PARTIAL;
    }

    public CalculationResult calculateProfitability(
            BigDecimal price,
            BigDecimal expectedYield,
            List<ProductionCostItemDto> productionCosts,
            SellingCostsDto sellingCosts
    ) {
        if (expectedYield == null || expectedYield.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expected yield must be greater than zero.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Market price cannot be negative.");
        }

        BigDecimal scaledPrice = price.setScale(2, RoundingMode.HALF_UP);
        BigDecimal scaledYield = expectedYield.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalProductionCost = calculateTotalProductionCost(productionCosts);
        BigDecimal totalSellingCost = calculateTotalSellingCost(sellingCosts);
        BigDecimal totalCost = totalProductionCost.add(totalSellingCost).setScale(2, RoundingMode.HALF_UP);

        // grossRevenue = price * yield
        BigDecimal grossRevenue = scaledPrice.multiply(scaledYield).setScale(2, RoundingMode.HALF_UP);

        // estimatedNetRealization = grossRevenue - totalSellingCost
        BigDecimal estimatedNetRealization = grossRevenue.subtract(totalSellingCost).setScale(2, RoundingMode.HALF_UP);

        // estimatedProfit = estimatedNetRealization - totalProductionCost
        BigDecimal estimatedProfit = estimatedNetRealization.subtract(totalProductionCost).setScale(2, RoundingMode.HALF_UP);

        // Per unit calculations
        BigDecimal profitPerUnit = estimatedProfit.divide(scaledYield, 2, RoundingMode.HALF_UP);
        BigDecimal productionCostPerUnit = totalProductionCost.divide(scaledYield, 2, RoundingMode.HALF_UP);
        BigDecimal totalCostPerUnit = totalCost.divide(scaledYield, 2, RoundingMode.HALF_UP);
        BigDecimal breakEvenSellingPrice = totalCost.divide(scaledYield, 2, RoundingMode.HALF_UP);

        // ROI calculation: (estimatedProfit / totalCost) * 100
        BigDecimal roi = null;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            roi = estimatedProfit
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalCost, 2, RoundingMode.HALF_UP);
        }

        // Profitability status
        String status;
        int profitComparison = estimatedProfit.compareTo(BigDecimal.ZERO);
        if (profitComparison > 0) {
            status = STATUS_PROFITABLE;
        } else if (profitComparison == 0) {
            status = STATUS_BREAK_EVEN;
        } else {
            status = STATUS_LOSS;
        }

        String completeness = determineCostCompleteness(productionCosts);

        return new CalculationResult(
                totalProductionCost,
                totalSellingCost,
                totalCost,
                grossRevenue,
                estimatedNetRealization,
                estimatedProfit,
                profitPerUnit,
                productionCostPerUnit,
                totalCostPerUnit,
                breakEvenSellingPrice,
                roi,
                status,
                completeness
        );
    }
}
