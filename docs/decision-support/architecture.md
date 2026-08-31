# Decision Support Architecture

## System Overview
The Decision Support & Profitability Foundation (Module 09) provides deterministic financial evaluation for farmers by combining verified market prices (Module 07/08) with explicit farmer-entered selling costs.

```
                      +-----------------------------------+
                      |   Farmer UI (/decision-support/*) |
                      +-----------------+-----------------+
                                        |
                            HTTP POST (Firebase Token)
                                        v
                      +-----------------------------------+
                      |     DecisionSupportController     |
                      |  - Authenticates Firebase Token   |
                      |  - Verifies FARMER Role Authority |
                      +-----------------+-----------------+
                                        |
                                        v
                      +-----------------------------------+
                      |       DecisionSupportService      |
                      |  - Orchestrates market price lookup|
                      |  - Enforces unit compatibility   |
                      |  - Checks freshness & stale dates |
                      +--------+----------------+---------+
                               |                |
         Fetch Verified Price  |                | Calculate Economics
                               v                v
                  +------------------+   +----------------------------------+
                  |MarketPriceService|   | ProfitabilityCalculationService  |
                  | - Spring Cache   |   | - Pure BigDecimal Math           |
                  | - QuotaGuard     |   | - Half-Up Monetary Rounding      |
                  +------------------+   +----------------------------------+
```

## Infrastructure Reuse
- **Price Store**: Sourced strictly from existing `MarketPriceService`. No secondary price data pipeline or independent Firestore read paths were created.
- **Quota Protection**: Leverages `FirestoreQuotaGuard` (CLOSED / OPEN / HALF_OPEN state machine with CAS probe ownership and HTTP 503 retry suppression).
- **Master Data Caching**: Master crops and markets are retrieved via cached `CropMasterService` and `MarketService` beans.
