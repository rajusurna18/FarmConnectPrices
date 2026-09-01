# Module 10 — Farm Economics & Profitability Architecture

## System Architecture

Module 10 introduces crop production economics for **FarmConnectPrices**, linking farmer-entered production costs with verified market prices (Module 07/09).

```
                      VERIFIED MARKET PRICE (Module 07/09)
                                       │
                                       ▼
                                 GROSS REVENUE
                                       │
                     ┌─────────────────┴─────────────────┐
                     │                                   │
              PRODUCTION COST                      SELLING COST
              (Module 10)                          (Module 09)
                     │                                   │
                     └─────────────────┬─────────────────┘
                                       ▼
                                  TOTAL COST
                                       │
                                       ▼
                                ESTIMATED PROFIT
                                       │
                      ┌────────────────┼────────────────┐
                      ▼                ▼                ▼
                 Profit/Unit          ROI          Break-Even Price
```

## Architectural Principles
1. **Deterministic Core**: All monetary calculations use Java `BigDecimal` (`HALF_UP` rounding, scale=2).
2. **Reused Architecture**: Leverages `FirestoreQuotaGuard`, Spring Cache, trusted `MarketPriceService`, and Firebase Auth ID tokens.
3. **Role Security**: Access is strictly limited to the `FARMER` primary user profile. `MEDIATOR_BUYER` and `CUSTOMER` profiles receive HTTP `403 FORBIDDEN`.
