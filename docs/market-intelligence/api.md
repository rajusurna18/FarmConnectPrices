# Market Intelligence REST API Documentation

## Endpoints

### 1. Market Comparison
- `GET /api/v1/market-intelligence/compare`
- **Query Parameters**: `cropId`, `date`, `fromDate`, `toDate`, `state`, `district`, `unit`
- **Response**: `MarketComparisonResponse` (contains market list, highestMarket, lowestMarket, priceDifference, percentageDifference).

### 2. Market Intelligence Summary
- `GET /api/v1/market-intelligence/summary`
- **Query Parameters**: `cropId`, `marketId`, `fromDate`, `toDate`, `state`, `district`, `unit`
- **Response**: `MarketIntelligenceSummaryResponse` (observation count, min/max/average modal prices, dates, trend direction).

### 3. Historical Price Trends
- `GET /api/v1/market-intelligence/trends`
- **Query Parameters**: `cropId`, `marketId`, `fromDate`, `toDate`, `unit`
- **Response**: `PriceTrendResponse` (chronological points, trend direction, absolute/percentage change).
