# Module 12 — Market Trend Intelligence Foundation

Module 12 provides deterministic historical and current market trend intelligence for agricultural commodities based strictly on verified AGMARKNET price observations.

## Overview & Core Principles

- **Verified Market Data Only**: Operates strictly on verified price observations ingested via `MarketPriceService`. No synthetic prices, AI-generated rates, or external web scraping.
- **Single Source of Truth**: `MarketTrendService` serves as the authoritative engine for all trend, range, percentage change, volatility, quality, and current-vs-average calculations.
- **Deterministic Math**: Calculations use `BigDecimal` with 2 decimal places (`HALF_UP` rounding). Percentage change uses `((latest - earliest) / earliest) * 100` with zero-earliest protection.
- **Non-Predictive AI**: Module 11 AI integration summarizes past and present observations without forecasting future price movements or guaranteeing future sales outcomes.
- **Quota & Cache Protection**: Stateless design, reusing `FirestoreQuotaGuard`, Spring `@Cacheable`, and bounded Firestore queries.

## Documentation Structure

- [Architecture Overview](architecture.md)
- [Trend Calculations & Math](trend-calculations.md)
- [Data Quality & Volatility Rules](data-quality.md)
- [API Reference](api.md)
- [Module 11 AI Integration & Guardrails](ai-integration.md)
