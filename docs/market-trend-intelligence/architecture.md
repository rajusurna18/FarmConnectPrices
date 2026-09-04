# Architecture — Market Trend Intelligence Foundation

## High-Level Architecture Flow

```
[ Verified AGMARKNET Data ] ──> MarketPriceService (Quota Guard & Spring Cache)
                                      │
                                      ▼
                             MarketTrendService (Authoritative Deterministic Engine)
                                      │
                   ┌──────────────────┴──────────────────┐
                   ▼                                     ▼
        MarketIntelligenceController           AiDecisionContextBuilder
        GET /api/v1/market-intelligence/trends           │
                   │                                     ▼
                   │                               AiDecisionService
                   │                                     │
                   │                                     ▼
                   │                           RuleBasedAiDecisionEngine / Provider
                   │                                     │
                   │                                     ▼
                   │                            AiDecisionValidator
                   │                                     │
                   └──────────────────┬──────────────────┘
                                      ▼
                        [ React / Recharts Frontend ]
                               /market-trends
```

## Core Infrastructure Principles

1. **Stateless Operations**: No secondary Firestore collection is created for trend history.
2. **Quota Protection**: All price queries pass through `FirestoreQuotaGuard` circuit breaker.
3. **Caching**: Leverages `@Cacheable` Spring Cache annotations.
4. **Single Source of Truth**: Both Module 08 and Module 12 query `MarketTrendService` for statistical calculations.
