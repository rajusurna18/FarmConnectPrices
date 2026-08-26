# Module 07 — REST API Specification (`/api/v1/market-prices`)

All endpoints require `Authorization: Bearer <Firebase ID Token>`.

## Endpoints

### 1. List Market Prices
`GET /api/v1/market-prices`
- **Query Parameters**: `marketId`, `cropId`, `priceDate`, `fromDate`, `toDate`, `qualityStatus`, `unit`, `state`, `district`, `limit` (max 100).
- **Response**: Array of `MarketPriceSummaryResponse`.

### 2. Get Latest Market Price
`GET /api/v1/market-prices/latest?marketId=...&cropId=...`
- **Query Parameters**: `marketId` (required), `cropId` (required).
- **Response**: Most recent verified/unverified `MarketPriceResponse` (sorted by `priceDate` desc, `observedAt` desc).

### 3. Get Price History
`GET /api/v1/market-prices/history?marketId=...&cropId=...`
- **Query Parameters**: `marketId` (required), `cropId` (required), `fromDate`, `toDate`.
- **Response**: Deterministic history array of `MarketPriceResponse` ordered newest to oldest.

### 4. Get Price Detail by ID
`GET /api/v1/market-prices/{priceId}`
- **Response**: Detailed `MarketPriceResponse` with populated embedded Market and Crop summary data.

### 5. Client Write Prevention
- `POST /api/v1/market-prices` $\rightarrow$ `HTTP 405 Method Not Allowed`
- `PUT /api/v1/market-prices/{priceId}` $\rightarrow$ `HTTP 405 Method Not Allowed`
- `DELETE /api/v1/market-prices/{priceId}` $\rightarrow$ `HTTP 405 Method Not Allowed`
