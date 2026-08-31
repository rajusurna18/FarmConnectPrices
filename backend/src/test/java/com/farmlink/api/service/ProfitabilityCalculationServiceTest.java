package com.farmlink.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProfitabilityCalculationServiceTest {

    private ProfitabilityCalculationService service;

    @BeforeEach
    void setUp() {
        service = new ProfitabilityCalculationService();
    }

    @Test
    @DisplayName("1. Normal gross revenue: 2800 price * 10 quantity = 28000")
    void testNormalGrossRevenue() {
        var res = service.calculate(new BigDecimal("2800"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("28000.00"), res.getGrossRevenue());
    }

    @Test
    @DisplayName("2. Decimal price: 2850.50 price * 10 quantity = 28505.00")
    void testDecimalPrice() {
        var res = service.calculate(new BigDecimal("2850.50"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("28505.00"), res.getGrossRevenue());
    }

    @Test
    @DisplayName("3. Decimal quantity: 2800 price * 10.5 quantity = 29400.00")
    void testDecimalQuantity() {
        var res = service.calculate(new BigDecimal("2800"), new BigDecimal("10.5"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("29400.00"), res.getGrossRevenue());
    }

    @Test
    @DisplayName("4. Total selling costs: 300 transport + 100 other = 400")
    void testTotalSellingCosts() {
        var res = service.calculate(new BigDecimal("2800"), new BigDecimal("10"), new BigDecimal("300"), new BigDecimal("100"));
        assertEquals(new BigDecimal("400.00"), res.getTotalSellingCosts());
    }

    @Test
    @DisplayName("5. Estimated net realization: 28000 gross - 400 costs = 27600")
    void testEstimatedNetRealization() {
        var res = service.calculate(new BigDecimal("2800"), new BigDecimal("10"), new BigDecimal("300"), new BigDecimal("100"));
        assertEquals(new BigDecimal("27600.00"), res.getEstimatedNetRealization());
        assertFalse(res.isLoss());
    }

    @Test
    @DisplayName("6. Net realization per unit: 27600 / 10 = 2760.00")
    void testNetRealizationPerUnit() {
        var res = service.calculate(new BigDecimal("2800"), new BigDecimal("10"), new BigDecimal("300"), new BigDecimal("100"));
        assertEquals(new BigDecimal("2760.00"), res.getNetRealizationPerUnit());
    }

    @Test
    @DisplayName("7. Break-even selling cost price: 400 costs / 10 quantity = 40.00")
    void testSellingCostBreakEvenPrice() {
        var res = service.calculate(new BigDecimal("2800"), new BigDecimal("10"), new BigDecimal("300"), new BigDecimal("100"));
        assertEquals(new BigDecimal("40.00"), res.getSellingCostBreakEvenPrice());
    }

    @Test
    @DisplayName("8. Negative realization (loss): Gross 1000, costs 1500 => net -500, isLoss true")
    void testNegativeRealization() {
        var res = service.calculate(new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("1000"), new BigDecimal("500"));
        assertEquals(new BigDecimal("-500.00"), res.getEstimatedNetRealization());
        assertTrue(res.isLoss());
    }

    @Test
    @DisplayName("9. Zero quantity throws IllegalArgumentException")
    void testZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate(new BigDecimal("2800"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("10. Negative quantity throws IllegalArgumentException")
    void testNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate(new BigDecimal("2800"), new BigDecimal("-5"), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("11. Negative transportation cost throws IllegalArgumentException")
    void testNegativeTransportationCost() {
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate(new BigDecimal("2800"), new BigDecimal("10"), new BigDecimal("-100"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("12. Negative other cost throws IllegalArgumentException")
    void testNegativeOtherCost() {
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate(new BigDecimal("2800"), new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("-50")));
    }

    @Test
    @DisplayName("13. Zero costs: 2800 * 10 = 28000 net, 0 break-even")
    void testZeroCosts() {
        var res = service.calculate(new BigDecimal("2800"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("28000.00"), res.getEstimatedNetRealization());
        assertEquals(new BigDecimal("0.00"), res.getSellingCostBreakEvenPrice());
    }

    @Test
    @DisplayName("14. Zero price: Gross 0, Net -400, isLoss true")
    void testZeroPrice() {
        var res = service.calculate(BigDecimal.ZERO, new BigDecimal("10"), new BigDecimal("300"), new BigDecimal("100"));
        assertEquals(new BigDecimal("0.00"), res.getGrossRevenue());
        assertEquals(new BigDecimal("-400.00"), res.getEstimatedNetRealization());
        assertTrue(res.isLoss());
    }

    @Test
    @DisplayName("15. Large quantity: 2500 price * 100000 quantity = 250000000.00")
    void testLargeQuantity() {
        var res = service.calculate(new BigDecimal("2500"), new BigDecimal("100000"), new BigDecimal("5000"), new BigDecimal("2000"));
        assertEquals(new BigDecimal("250000000.00"), res.getGrossRevenue());
        assertEquals(new BigDecimal("249993000.00"), res.getEstimatedNetRealization());
    }

    @Test
    @DisplayName("16. BigDecimal precision: 3333.33 price * 3 quantity")
    void testBigDecimalPrecision() {
        var res = service.calculate(new BigDecimal("3333.33"), new BigDecimal("3"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("9999.99"), res.getGrossRevenue());
    }

    @Test
    @DisplayName("17. Rounding: 100 total costs / 3 quantity = 33.33 break-even")
    void testRounding() {
        var res = service.calculate(new BigDecimal("1000"), new BigDecimal("3"), new BigDecimal("100"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("33.33"), res.getSellingCostBreakEvenPrice());
    }

    @Test
    @DisplayName("18. Negative price throws IllegalArgumentException")
    void testNegativePrice() {
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate(new BigDecimal("-100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
