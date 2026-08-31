# Decision Support Data Flow

```
Farmer Form Input (Crop, Market, Quantity, Transport & Other Costs)
                               │
                               ▼
            POST /api/v1/decision-support/evaluate
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
Firebase Token Verification            Role Authorization (FARMER)
            │                                     │
            └──────────────────┬──────────────────┘
                               ▼
                 MarketPriceService Lookup
           (Spring Cache / QuotaGuard Protected)
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
   Unit Compatibility Check                Freshness / Stale Check
 (quantityUnit == priceUnit)              (priceDate vs LocalDate.now)
            │                                     │
            └──────────────────┬──────────────────┘
                               ▼
            ProfitabilityCalculationService
             - Gross Revenue = Price * Quantity
             - Total Costs = Transport + Other
             - Net Realization = Gross - Total Costs
             - Net / Unit = Net / Quantity
             - Break-Even = Total Costs / Quantity
                               │
                               ▼
                 Structured API Response
                               │
                               ▼
                  ProfitabilitySummaryCard
```
