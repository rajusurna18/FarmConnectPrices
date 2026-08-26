package com.farmlink.api.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class PriceUnitConversionService {

    public static final String UNIT_KG = "KG";
    public static final String UNIT_QUINTAL = "QUINTAL";
    public static final String UNIT_TONNE = "TONNE";

    public static class ConvertedPriceResult {
        private final double minPrice;
        private final double modalPrice;
        private final double maxPrice;
        private final String sourceUnit;
        private final String displayUnit;
        private final boolean conversionApplied;
        private final double conversionFactor;

        public ConvertedPriceResult(double minPrice, double modalPrice, double maxPrice,
                                    String sourceUnit, String displayUnit,
                                    boolean conversionApplied, double conversionFactor) {
            this.minPrice = minPrice;
            this.modalPrice = modalPrice;
            this.maxPrice = maxPrice;
            this.sourceUnit = sourceUnit;
            this.displayUnit = displayUnit;
            this.conversionApplied = conversionApplied;
            this.conversionFactor = conversionFactor;
        }

        public double getMinPrice() { return minPrice; }
        public double getModalPrice() { return modalPrice; }
        public double getMaxPrice() { return maxPrice; }
        public String getSourceUnit() { return sourceUnit; }
        public String getDisplayUnit() { return displayUnit; }
        public boolean isConversionApplied() { return conversionApplied; }
        public double getConversionFactor() { return conversionFactor; }
    }

    public boolean isSupportedUnit(String unit) {
        if (unit == null) return false;
        String u = unit.trim().toUpperCase(Locale.ROOT);
        return u.equals(UNIT_KG) || u.equals(UNIT_QUINTAL) || u.equals(UNIT_TONNE);
    }

    public ConvertedPriceResult convert(double minPrice, double modalPrice, double maxPrice,
                                         String sourceUnit, String targetUnit) {
        String sUnit = (sourceUnit != null) ? sourceUnit.trim().toUpperCase(Locale.ROOT) : "UNKNOWN";
        String tUnit = (targetUnit != null && !targetUnit.trim().isEmpty()) ? targetUnit.trim().toUpperCase(Locale.ROOT) : sUnit;

        if (!isSupportedUnit(sUnit) || !isSupportedUnit(tUnit) || sUnit.equals(tUnit)) {
            return new ConvertedPriceResult(minPrice, modalPrice, maxPrice, sUnit, sUnit, false, 1.0);
        }

        BigDecimal factor = calculateConversionFactor(sUnit, tUnit);
        double convertedMin = convertValue(minPrice, factor);
        double convertedModal = convertValue(modalPrice, factor);
        double convertedMax = convertValue(maxPrice, factor);

        return new ConvertedPriceResult(
                convertedMin, convertedModal, convertedMax,
                sUnit, tUnit, true, factor.doubleValue()
        );
    }

    public BigDecimal calculateConversionFactor(String sourceUnit, String targetUnit) {
        String src = sourceUnit.trim().toUpperCase(Locale.ROOT);
        String tgt = targetUnit.trim().toUpperCase(Locale.ROOT);

        if (src.equals(tgt)) return BigDecimal.ONE;

        if (src.equals(UNIT_QUINTAL) && tgt.equals(UNIT_KG)) {
            return new BigDecimal("0.01"); // price / 100
        } else if (src.equals(UNIT_QUINTAL) && tgt.equals(UNIT_TONNE)) {
            return new BigDecimal("10"); // price * 10
        } else if (src.equals(UNIT_KG) && tgt.equals(UNIT_QUINTAL)) {
            return new BigDecimal("100"); // price * 100
        } else if (src.equals(UNIT_KG) && tgt.equals(UNIT_TONNE)) {
            return new BigDecimal("1000"); // price * 1000
        } else if (src.equals(UNIT_TONNE) && tgt.equals(UNIT_QUINTAL)) {
            return new BigDecimal("0.1"); // price / 10
        } else if (src.equals(UNIT_TONNE) && tgt.equals(UNIT_KG)) {
            return new BigDecimal("0.001"); // price / 1000
        }

        return BigDecimal.ONE;
    }

    private double convertValue(double val, BigDecimal factor) {
        BigDecimal bVal = BigDecimal.valueOf(val);
        BigDecimal converted = bVal.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        return converted.doubleValue();
    }
}
