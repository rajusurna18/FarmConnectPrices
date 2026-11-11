package com.farmlink.api.model;

/**
 * Controlled enum representing supported marketplace dispute issue categories.
 */
public enum DisputeCategory {
    PRODUCT_QUALITY,
    DAMAGED_PRODUCT,
    QUANTITY_MISMATCH,
    WRONG_PRODUCT,
    DELIVERY_DELAY,
    DELIVERY_NOT_RECEIVED,
    PAYMENT_ISSUE,
    ORDER_CANCELLATION,
    OTHER;

    /**
     * Checks if a string matches a valid DisputeCategory.
     */
    public static boolean isValid(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return false;
        }
        for (DisputeCategory c : values()) {
            if (c.name().equalsIgnoreCase(categoryStr.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safely parse a DisputeCategory from string, returning null if invalid.
     */
    public static DisputeCategory fromString(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return null;
        }
        for (DisputeCategory c : values()) {
            if (c.name().equalsIgnoreCase(categoryStr.trim())) {
                return c;
            }
        }
        return null;
    }
}

