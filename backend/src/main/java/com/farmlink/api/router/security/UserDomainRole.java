package com.farmlink.api.router.security;

/**
 * Domain roles recognized by the Agricultural Intent Router.
 */
public enum UserDomainRole {
    FARMER,
    MEDIATOR_BUYER,
    CUSTOMER;

    public static UserDomainRole fromString(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return CUSTOMER;
        }
        String normalized = rawRole.trim().toUpperCase();
        return switch (normalized) {
            case "FARMER" -> FARMER;
            case "MEDIATOR_BUYER", "MEDIATOR", "BUYER" -> MEDIATOR_BUYER;
            default -> CUSTOMER;
        };
    }
}

