package com.farmlink.api.dto;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum ProductionCostCategory {
    SEEDS,
    FERTILIZER,
    PESTICIDES,
    LABOR,
    IRRIGATION,
    MACHINERY,
    LAND,
    OTHER;

    private static final Set<String> VALID_NAMES = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    public static boolean isValid(String name) {
        if (name == null) return false;
        return VALID_NAMES.contains(name.trim().toUpperCase());
    }

    public static ProductionCostCategory fromString(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Cost category cannot be null.");
        }
        try {
            return ProductionCostCategory.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid production cost category: " + name + ". Allowed categories are: " + VALID_NAMES);
        }
    }
}
