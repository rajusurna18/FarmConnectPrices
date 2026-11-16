package com.farmlink.api.router.security;

import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.exception.UnauthorizedEconomicsAccessException;
import org.springframework.stereotype.Component;

/**
 * Enforces role-based privacy and ownership boundaries for agricultural queries.
 * Farm economics inquiries are strictly restricted to authenticated farmers querying their own records.
 */
@Component
public class EconomicDataPrivacyGuard {

    public void enforcePrivacyBoundaries(AgriIntent intent, UserDomainRole callerRole, String authenticatedUid, String targetOwnerUid) {
        if (intent == AgriIntent.FARM_ECONOMICS_INQUIRY) {
            // Must be a FARMER
            if (callerRole != UserDomainRole.FARMER) {
                throw new UnauthorizedEconomicsAccessException(
                        "Access denied: Farm economics inquiry is restricted to farmers. Current role: " + callerRole
                );
            }

            // Target owner validation (if provided, must match caller UID)
            if (targetOwnerUid != null && !targetOwnerUid.isBlank()) {
                if (!targetOwnerUid.trim().equals(authenticatedUid)) {
                    throw new UnauthorizedEconomicsAccessException(
                            "Access denied: Cannot query economics data for another user. Authenticated: "
                                    + authenticatedUid + ", Requested: " + targetOwnerUid
                    );
                }
            }
        }
    }
}

