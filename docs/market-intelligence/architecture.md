# Market Intelligence Architecture

## Overview
Module 08 introduces the Market Intelligence layer for FarmConnectPrices. It transforms verified market price observations from Module 07 (`/marketPrices`) into transparent, deterministic intelligence.

## System Architecture
```
Cloud Firestore (/marketPrices)
        ↓ (qualityStatus = VERIFIED)
MarketIntelligenceService.java
        ↓ (Math formulas, unit filtering, ranking)
MarketIntelligenceController.java (/api/v1/market-intelligence/*)
        ↓ (Authenticated REST APIs)
Frontend TanStack Query Hooks (useMarketIntelligence.ts)
        ↓
MarketIntelligencePage.tsx (/market-intelligence)
```

## Security & Profile Access
- Access requires Firebase ID token (`Authorization: Bearer <token>`).
- All three primary user profiles (`FARMER`, `MEDIATOR_BUYER`, `CUSTOMER`) have full read access to market intelligence.
- Read-only calculations. No data mutation endpoints are provided.
