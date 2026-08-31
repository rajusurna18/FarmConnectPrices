package com.farmlink.api.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProfitabilityCalculationService {

    public static class ProfitabilityResult {
        private final BigDecimal price;
        private final BigDecimal quantity;
        private final BigDecimal grossRevenue;
        private final BigDecimal transportationCost;
        private final BigDecimal otherSellingCosts;
        private final BigDecimal totalSellingCosts;
        private final BigDecimal estimatedNetRealization;
        private final BigDecimal netRealizationPerUnit;
        private final BigDecimal sellingCostBreakEvenPrice;
        private final boolean loss;

        public ProfitabilityResult(
                BigDecimal price,
                BigDecimal quantity,
                BigDecimal grossRevenue,
                BigDecimal transportationCost,
                BigDecimal otherSellingCosts,
                BigDecimal totalSellingCosts,
                BigDecimal estimatedNetRealization,
                BigDecimal netRealizationPerUnit,
                BigDecimal sellingCostBreakEvenPrice,
                boolean loss
        ) {
            this.price = price;
            this.quantity = quantity;
            this.grossRevenue = grossRevenue;
            this.transportationCost = transportationCost;
            this.otherSellingCosts = otherSellingCosts;
            this.totalSellingCosts = totalSellingCosts;
            this.estimatedNetRealization = estimatedNetRealization;
            this.netRealizationPerUnit = netRealizationPerUnit;
            this.sellingCostBreakEvenPrice = sellingCostBreakEvenPrice;
            this.loss = loss;
        }

        public BigDecimal getPrice() { return price; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getGrossRevenue() { return grossRevenue; }
        public BigDecimal getTransportationCost() { return transportationCost; }
        public BigDecimal getOtherSellingCosts() { return otherSellingCosts; }
        public BigDecimal getTotalSellingCosts() { return totalSellingCosts; }
        public BigDecimal getEstimatedNetRealization() { return estimatedNetRealization; }
        public BigDecimal getNetRealizationPerUnit() { return netRealizationPerUnit; }
        public BigDecimal getSellingCostBreakEvenPrice() { return sellingCostBreakEvenPrice; }
        public boolean isLoss() { return loss; }
    }

    public ProfitabilityResult calculate(
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal transportationCost,
            BigDecimal otherSellingCosts
    ) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }

        BigDecimal transport = transportationCost != null ? transportationCost : BigDecimal.ZERO;
        BigDecimal other = otherSellingCosts != null ? otherSellingCosts : BigDecimal.ZERO;

        if (transport.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transportation cost cannot be negative.");
        }
        if (other.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Other selling costs cannot be negative.");
        }

        // Scale monetary inputs to 2 decimal places
        BigDecimal scaledPrice = price.setScale(2, RoundingMode.HALF_UP);
        BigDecimal scaledQuantity = quantity.setScale(2, RoundingMode.HALF_UP);
        BigDecimal scaledTransport = transport.setScale(2, RoundingMode.HALF_UP);
        BigDecimal scaledOther = other.setScale(2, RoundingMode.HALF_UP);

        // grossRevenue = price * quantity
        BigDecimal grossRevenue = scaledPrice.multiply(scaledQuantity).setScale(2, RoundingMode.HALF_UP);

        // totalSellingCosts = transport + other
        BigDecimal totalSellingCosts = scaledTransport.add(scaledOther).setScale(2, RoundingMode.HALF_UP);

        // estimatedNetRealization = grossRevenue - totalSellingCosts
        BigDecimal estimatedNetRealization = grossRevenue.subtract(totalSellingCosts).setScale(2, RoundingMode.HALF_UP);

        // netRealizationPerUnit = estimatedNetRealization / quantity
        BigDecimal netRealizationPerUnit = estimatedNetRealization.divide(scaledQuantity, 2, RoundingMode.HALF_UP);

        // sellingCostBreakEvenPrice = totalSellingCosts / quantity
        BigDecimal sellingCostBreakEvenPrice = totalSellingCosts.divide(scaledQuantity, 2, RoundingMode.HALF_UP);

        boolean isLoss = estimatedNetRealization.compareTo(BigDecimal.ZERO) < 0;

        return new ProfitabilityResult(
                scaledPrice,
                scaledQuantity,
                grossRevenue,
                scaledTransport,
                scaledOther,
                totalSellingCosts,
                estimatedNetRealization,
                netRealizationPerUnit,
                sellingCostBreakEvenPrice,
                isLoss
        );
    }
}
