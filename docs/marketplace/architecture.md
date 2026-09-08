# Marketplace Architecture — Module 15

## System Overview

```text
                      FARMER
                        │
                        ▼
                  CREATE LISTING
                        │
                        ▼
                 CROP MASTER DATA (Module 05)
                        │
                        ▼
                MARKETPLACE CATALOG (productListings)
                        │
            ┌───────────┴───────────┐
            ▼                       ▼
     MEDIATOR / BUYER            CUSTOMER
            │                       │
            └───────────┬───────────┘
                        ▼
                 DISCOVER & VIEW
```

## Persistence Architecture
- **Firestore Collection**: `productListings`
- **Quota Protection**: Integrated with `FirestoreQuotaGuard` to prevent circuit-breaker trip and quota exhaustion.
- **Client Caching**: React Query (`staleTime: 60000ms`) and Spring Cache layer reduce redundant reads.
- **No SQL/JPA**: Follows existing Firestore persistence architecture.

## Component Integration
- `CropMasterService`: Authoritative crop verification.
- `LocationMasterService` / `LocationDto`: Regional location masking (State, District, Mandal, Village).
- `MarketPriceService`: Optional read-only verified market modal price reference integration.
- `PriceUnitConversionService`: Reused for unit calculations.
- `ProfileService`: Enforces profile role checks (`FARMER`, `MEDIATOR_BUYER`, `CUSTOMER`).
