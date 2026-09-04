# API Reference — Market Trend Intelligence

## Endpoint

`GET /api/v1/market-intelligence/trends`

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `cropId` | String | No | null | Target crop ID or commodity alias |
| `marketId` | String | No | null | Target mandi / market ID |
| `period` | String | No | `30D` | Time horizon: `7D`, `30D`, `90D`, `6M`, `1Y`, `CUSTOM` |
| `startDate` | String | No (Yes for CUSTOM) | null | ISO date `YYYY-MM-DD` |
| `endDate` | String | No (Yes for CUSTOM) | null | ISO date `YYYY-MM-DD` |
| `unit` | String | No | `QUINTAL` | Display unit: `QUINTAL`, `KG`, `TONNE` |
| `includeAiExplanation` | Boolean | No | `false` | Include Module 11 AI trend summary |

### Validation Rules for `CUSTOM` Period
- `startDate` and `endDate` must be valid ISO dates (`YYYY-MM-DD`).
- `startDate <= endDate`.
- `endDate <= today`.
- Date range span cannot exceed **365 days**.
- Violations return `HTTP 400 Bad Request`.

### Response Schema (`MarketTrendResponse`)

```json
{
  "cropId": "crop-chilli",
  "cropName": "Red Chilli",
  "marketId": "mkt-guntur",
  "marketName": "Guntur Mandi",
  "currency": "INR",
  "unit": "QUINTAL",
  "period": "30D",
  "startDate": "2026-08-05",
  "endDate": "2026-09-04",
  "observationCount": 10,
  "earliestPrice": 18000.0,
  "latestPrice": 21000.0,
  "minPrice": 17500.0,
  "maxPrice": 22000.0,
  "avgPrice": 19500.0,
  "priceRange": 4500.0,
  "absoluteChange": 3000.0,
  "percentageChange": 16.67,
  "trendDirection": "RISING",
  "volatility": "LOW",
  "volatilityCvPercent": 4.12,
  "dataQuality": "GOOD",
  "freshnessStatus": "FRESH",
  "latestObservationDate": "2026-09-02",
  "currentVsAverageStatement": "The latest verified price (₹21,000.00 / QUINTAL) is approximately 7.69% above the selected period average (₹19,500.00 / QUINTAL).",
  "currentVsAveragePctDiff": 7.69,
  "points": [
    {
      "priceDate": "2026-08-05",
      "modalPrice": 18000.0,
      "minPrice": 17000.0,
      "maxPrice": 19000.0
    }
  ],
  "aiExplanation": null
}
```
