# Smart Selling System Architecture

## Overview

Module 14 orchestrates trusted services from Modules 07–13 without altering their internal implementations or duplicating business logic.

```
                    VERIFIED MARKET DATA (Module 07)
                                 │
                    MARKET INTELLIGENCE (Module 08)
                                 │
                 ┌───────────────┴───────────────┐
                 ▼                               ▼
          HISTORICAL TREND                FARM ECONOMICS
            (Module 12)                    (Module 10)
                 │                               │
                 └───────────────┬───────────────┘
                                 ▼
                           PRICE FORECAST
                             (Module 13)
                                 │
                                 ▼
                     SMART SELLING DECISION
                           ENGINE (14)
                                 │
                                 ▼
                           RANKED OPTIONS
                                 │
                                 ▼
                        TRADE-OFF ANALYSIS
                                 │
                                 ▼
                           AI EXPLANATION
                             (Module 11)
                                 │
                                 ▼
                              FARMER
```

## Source-of-Truth Hierarchy

1. **LEVEL 1**: Verified Market Data (`MarketPriceService`)
2. **LEVEL 2**: Deterministic Profitability & Net Realization (`ProfitabilityCalculationService`, `FarmEconomicsCalculationService`)
3. **LEVEL 3**: Forecast Models (`ForecastService`)
4. **LEVEL 4**: Deterministic Lexicographic Ranking & Trade-Off Engine
5. **LEVEL 5**: AI Explanation Layer (`AiDecisionService`)
