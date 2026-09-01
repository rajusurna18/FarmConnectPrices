package com.farmlink.api.service;

import com.farmlink.api.dto.ProductionCostItemDto;
import com.farmlink.api.dto.SellingCostsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FarmEconomicsCalculationServiceTest {

    private FarmEconomicsCalculationService service;

    @BeforeEach
    void setUp() {
        service = new FarmEconomicsCalculationService();
    }

    @Test
    void testCalculateTotalProductionCost() {
        List<ProductionCostItemDto> costs = List.of(
                new ProductionCostItemDto("SEEDS", "Rice seed", 2500.0),
                new ProductionCostItemDto("FERTILIZER", "Urea & NPK", 4000.0),
                new ProductionCostItemDto("LABOR", "Harvest labor", 5000.0)
        );

        BigDecimal total = service.calculateTotalProductionCost(costs);
        assertEquals(new BigDecimal("11500.00"), total);
    }

    @Test
    void testCalculateTotalSellingCost() {
        SellingCostsDto sellingCosts = new SellingCostsDto(300.0, 100.0);
        BigDecimal total = service.calculateTotalSellingCost(sellingCosts);
        assertEquals(new BigDecimal("400.00"), total);
    }

    @Test
    void testCalculateProfitabilityProfitable() {
        BigDecimal price = new BigDecimal("2800.00"); // ₹2800/quintal
        BigDecimal yield = new BigDecimal("10.00"); // 10 quintals
        List<ProductionCostItemDto> prodCosts = List.of(
                new ProductionCostItemDto("SEEDS", "Seed", 2500.0),
                new ProductionCostItemDto("FERTILIZER", "Fertilizer", 4000.0),
                new ProductionCostItemDto("LABOR", "Labor", 5000.0)
        ); // Total production cost = 11,500
        SellingCostsDto sellCosts = new SellingCostsDto(300.0, 100.0); // Total selling cost = 400

        FarmEconomicsCalculationService.CalculationResult res = service.calculateProfitability(price, yield, prodCosts, sellCosts);

        // Gross Revenue = 2800 * 10 = 28000
        assertEquals(new BigDecimal("28000.00"), res.getGrossRevenue());
        // Total Production Cost = 11500
        assertEquals(new BigDecimal("11500.00"), res.getTotalProductionCost());
        // Total Selling Cost = 400
        assertEquals(new BigDecimal("400.00"), res.getTotalSellingCost());
        // Total Cost = 11900
        assertEquals(new BigDecimal("11900.00"), res.getTotalCost());
        // Net Realization = 28000 - 400 = 27600
        assertEquals(new BigDecimal("27600.00"), res.getEstimatedNetRealization());
        // Estimated Profit = 27600 - 11500 = 16100
        assertEquals(new BigDecimal("16100.00"), res.getEstimatedProfit());
        // Profit / Unit = 16100 / 10 = 1610.00
        assertEquals(new BigDecimal("1610.00"), res.getProfitPerUnit());
        // Production Cost / Unit = 11500 / 10 = 1150.00
        assertEquals(new BigDecimal("1150.00"), res.getProductionCostPerUnit());
        // Total Cost / Unit = 11900 / 10 = 1190.00
        assertEquals(new BigDecimal("1190.00"), res.getTotalCostPerUnit());
        // Break Even Price = 1190.00
        assertEquals(new BigDecimal("1190.00"), res.getBreakEvenSellingPrice());
        // ROI % = (16100 / 11900) * 100 = 135.29%
        assertEquals(new BigDecimal("135.29"), res.getRoi());
        assertEquals("PROFITABLE", res.getProfitabilityStatus());
    }

    @Test
    void testCalculateProfitabilityLoss() {
        BigDecimal price = new BigDecimal("1000.00"); // ₹1000/quintal
        BigDecimal yield = new BigDecimal("10.00"); // Gross Revenue = 10,000
        List<ProductionCostItemDto> prodCosts = List.of(
                new ProductionCostItemDto("SEEDS", "Seed", 8000.0),
                new ProductionCostItemDto("LABOR", "Labor", 5000.0)
        ); // Production Cost = 13,000
        SellingCostsDto sellCosts = new SellingCostsDto(500.0, 500.0); // Selling Cost = 1,000 (Total Cost = 14,000)

        FarmEconomicsCalculationService.CalculationResult res = service.calculateProfitability(price, yield, prodCosts, sellCosts);

        assertEquals(new BigDecimal("10000.00"), res.getGrossRevenue());
        assertEquals(new BigDecimal("14000.00"), res.getTotalCost());
        assertEquals(new BigDecimal("-4000.00"), res.getEstimatedProfit());
        assertEquals("LOSS", res.getProfitabilityStatus());
    }

    @Test
    void testCalculateProfitabilityBreakEven() {
        BigDecimal price = new BigDecimal("1400.00"); // ₹1400/quintal
        BigDecimal yield = new BigDecimal("10.00"); // Gross Revenue = 14,000
        List<ProductionCostItemDto> prodCosts = List.of(
                new ProductionCostItemDto("SEEDS", "Seed", 8000.0),
                new ProductionCostItemDto("LABOR", "Labor", 5000.0)
        ); // Production Cost = 13,000
        SellingCostsDto sellCosts = new SellingCostsDto(500.0, 500.0); // Selling Cost = 1,000 (Total Cost = 14,000)

        FarmEconomicsCalculationService.CalculationResult res = service.calculateProfitability(price, yield, prodCosts, sellCosts);

        assertEquals(new BigDecimal("14000.00"), res.getGrossRevenue());
        assertEquals(new BigDecimal("14000.00"), res.getTotalCost());
        assertEquals(new BigDecimal("0.00"), res.getEstimatedProfit());
        assertEquals("BREAK_EVEN", res.getProfitabilityStatus());
    }

    @Test
    void testZeroTotalCostRoiNull() {
        BigDecimal price = new BigDecimal("1000.00");
        BigDecimal yield = new BigDecimal("10.00");
        List<ProductionCostItemDto> prodCosts = new ArrayList<>();
        SellingCostsDto sellCosts = new SellingCostsDto(0.0, 0.0);

        FarmEconomicsCalculationService.CalculationResult res = service.calculateProfitability(price, yield, prodCosts, sellCosts);

        assertEquals(new BigDecimal("0.00"), res.getTotalCost());
        assertNull(res.getRoi()); // division by zero safe check
    }

    @Test
    void testZeroYieldThrows() {
        BigDecimal price = new BigDecimal("1000.00");
        BigDecimal yield = BigDecimal.ZERO;
        List<ProductionCostItemDto> prodCosts = new ArrayList<>();
        SellingCostsDto sellCosts = new SellingCostsDto(100.0, 0.0);

        assertThrows(IllegalArgumentException.class, () -> service.calculateProfitability(price, yield, prodCosts, sellCosts));
    }

    @Test
    void testNegativeProductionCostThrows() {
        List<ProductionCostItemDto> prodCosts = List.of(
                new ProductionCostItemDto("SEEDS", "Seed", -500.0)
        );
        assertThrows(IllegalArgumentException.class, () -> service.calculateTotalProductionCost(prodCosts));
    }

    @Test
    void testInvalidCategoryThrows() {
        List<ProductionCostItemDto> prodCosts = List.of(
                new ProductionCostItemDto("INVALID_CATEGORY", "Seed", 500.0)
        );
        assertThrows(IllegalArgumentException.class, () -> service.calculateTotalProductionCost(prodCosts));
    }
}
