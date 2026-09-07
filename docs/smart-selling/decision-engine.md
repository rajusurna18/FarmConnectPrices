# Deterministic Decision Engine & Scoring

## Decision Workflow

1. **Request Validation**: Verifies caller role (`FARMER`), validates crop, quantity, and candidate market IDs (max 10).
2. **Intelligence Gathering**:
   - Queries `MarketPriceService` for verified modal prices.
   - Queries `FarmEconomicsService` for saved production costs if `economicRecordId` provided.
   - Evaluates selling costs (custom or saved).
   - Queries `MarketTrendService` for 30-day trend direction and volatility.
   - Queries `ForecastService` for 1D, 3D, 7D, 14D price forecast scenarios.
3. **Calculation**: Computes gross revenue, total selling cost, net realization, and net profit per candidate market.
4. **Ranking & Trade-Offs**: Orders candidate market cards deterministically and identifies consequence-verified trade-offs.
5. **AI Narrative**: Passes structured result to Module 11 for narrative generation and risk/next-step formatting.
