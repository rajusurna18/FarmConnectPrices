package com.farmlink.api.router.security;

import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.exception.UnauthorizedEconomicsAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EconomicDataPrivacyGuardTest {

    private EconomicDataPrivacyGuard privacyGuard;

    @BeforeEach
    void setUp() {
        privacyGuard = new EconomicDataPrivacyGuard();
    }

    @Test
    @DisplayName("Farmer querying own farm economics is authorized")
    void testFarmerOwnEconomicsAuthorized() {
        assertDoesNotThrow(() ->
                privacyGuard.enforcePrivacyBoundaries(
                        AgriIntent.FARM_ECONOMICS_INQUIRY,
                        UserDomainRole.FARMER,
                        "farmer123",
                        "farmer123"
                )
        );

        assertDoesNotThrow(() ->
                privacyGuard.enforcePrivacyBoundaries(
                        AgriIntent.FARM_ECONOMICS_INQUIRY,
                        UserDomainRole.FARMER,
                        "farmer123",
                        null
                )
        );
    }

    @Test
    @DisplayName("Farmer querying another user's farm economics throws UnauthorizedEconomicsAccessException")
    void testFarmerOtherUserEconomicsDenied() {
        assertThrows(UnauthorizedEconomicsAccessException.class, () ->
                privacyGuard.enforcePrivacyBoundaries(
                        AgriIntent.FARM_ECONOMICS_INQUIRY,
                        UserDomainRole.FARMER,
                        "farmer123",
                        "farmer999"
                )
        );
    }

    @Test
    @DisplayName("Mediator/Buyer querying farm economics is denied")
    void testMediatorBuyerEconomicsDenied() {
        assertThrows(UnauthorizedEconomicsAccessException.class, () ->
                privacyGuard.enforcePrivacyBoundaries(
                        AgriIntent.FARM_ECONOMICS_INQUIRY,
                        UserDomainRole.MEDIATOR_BUYER,
                        "mediator1",
                        "mediator1"
                )
        );
    }

    @Test
    @DisplayName("Customer querying farm economics is denied")
    void testCustomerEconomicsDenied() {
        assertThrows(UnauthorizedEconomicsAccessException.class, () ->
                privacyGuard.enforcePrivacyBoundaries(
                        AgriIntent.FARM_ECONOMICS_INQUIRY,
                        UserDomainRole.CUSTOMER,
                        "cust1",
                        "cust1"
                )
        );
    }

    @Test
    @DisplayName("Public agricultural intents are authorized for all roles")
    void testPublicIntentsAuthorizedForAllRoles() {
        for (UserDomainRole role : UserDomainRole.values()) {
            assertDoesNotThrow(() ->
                    privacyGuard.enforcePrivacyBoundaries(
                            AgriIntent.MARKET_PRICE_INQUIRY,
                            role,
                            "user123",
                            null
                    )
            );
            assertDoesNotThrow(() ->
                    privacyGuard.enforcePrivacyBoundaries(
                            AgriIntent.PEST_MANAGEMENT,
                            role,
                            "user123",
                            null
                    )
            );
            assertDoesNotThrow(() ->
                    privacyGuard.enforcePrivacyBoundaries(
                            AgriIntent.WEATHER_INQUIRY,
                            role,
                            "user123",
                            null
                    )
            );
        }
    }
}

