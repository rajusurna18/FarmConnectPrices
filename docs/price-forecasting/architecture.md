# Forecasting Architecture

## 🏗️ End-to-End Data & Execution Pipeline

```text
                  VERIFIED MARKET DATA (Modules 07–12)
                                   │
                                   ▼
                  HISTORICAL PRICE DATA PREPARATION
                  - Filter valid & non-rejected records
                  - Deduplicate by priceDate
                  - Sort chronologically (oldest to newest)
                  - Preserve missing dates without interpolation
                                   │
                                   ▼
                   FORECAST MODEL EXECUTION (`WMA_V1`)
                  - Linear decay weights: w_i = i
                  - Pure recursive multi-step forecasting
                                   │
                                   ▼
               WALK-FORWARD OUT-OF-SAMPLE BACKTESTING
                  - Origin-relative historical cutoffs (T_cut)
                  - Horizon-specific evaluation (1D, 3D, 7D, 14D)
                  - Residual collection: e_{m,h} = y_actual - y_pred
                                   │
                                   ▼
                  ERROR METRICS & CONFIDENCE EVALUATION
                  - MAE & MAPE calculation per horizon
                  - Empirical residual standard error: S_h
                  - Lower/Upper bounds: forecast +/- 1.96 * S_h
                  - Order-of-precedence confidence classification
                                   │
             ┌─────────────────────┴─────────────────────┐
             ▼                                           ▼
      FORECAST RESPONSE API                      MODULE 11 AI EXPLANATION
  (GET /market-intelligence/forecast)        (Guardrailed human summary)
             │                                           │
             └─────────────────────┬─────────────────────┘
                                   ▼
                     FARMER DECISION SUPPORT UI
                    (/price-forecast route)
```

## 🔐 Security & Quota Strategy

- **Spring Cache (`@Cacheable`)**: Caches forecasts by `cropId_marketId_horizon_unit` to eliminate duplicate reads.
- **Firestore Quota Guard**: Utilizes `FirestoreQuotaGuard` circuit breaker to prevent quota exhaustion.
- **Data Isolation**: Private farmer economics scenarios (`POST /farm-economics/{id}/forecast-scenario`) are strictly enforced for the owning farmer (`FARMER` role).
