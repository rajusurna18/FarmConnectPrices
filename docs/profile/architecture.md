# User Profiles & Role Architecture — Three Primary Profiles

## Overview
FarmConnectPrices separates identity, common account metadata, and role-specific profile documents.

```
                  Firebase Authentication (UID)
                                │
                                ▼
                           users/{uid}
             (displayName, email, role, status, timestamps)
                                │
         ┌──────────────────────┼──────────────────────┐
         ▼                      ▼                      ▼
farmerProfiles/{uid}  mediatorBuyerProfiles/{uid}  customerProfiles/{uid}
```

## Primary Application Roles
1. **`FARMER`** (Display: **Farmer**): Produce sellers and agricultural growers.
2. **`MEDIATOR_BUYER`** (Display: **Mediator / Buyer**): Commercial buyers, aggregators, traders, and agricultural market mediators.
3. **`CUSTOMER`** (Display: **Customer**): End consumers purchasing agricultural goods.

## Decommissioned Profiles
- The legacy `BUYER` role and `buyerProfiles/{uid}` collection have been decommissioned and replaced by `mediatorBuyerProfiles/{uid}` and `customerProfiles/{uid}`.
