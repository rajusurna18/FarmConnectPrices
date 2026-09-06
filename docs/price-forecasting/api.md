# Price Forecasting REST API Reference

## 🌐 Endpoints Overview

| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/v1/market-intelligence/forecast` | `GET` | All / Public | Fetch price forecast for crop, market & horizon |
| `/api/v1/farm-economics/{id}/forecast-scenario` | `POST` | `FARMER` | Calculate scenario profitability for farmer's economic record |

---

## 1. Fetch Price Forecast

```http
GET /api/v1/market-intelligence/forecast?cropId=crop-1&marketId=mkt-1&horizon=7_DAYS&lookbackDays=30&unit=QUINTAL
```

### Response Example (`200 OK`)
```json
{
  "cropId": "crop-1",
  "cropName": "Tomato",
  "marketId": "mkt-1",
  "marketName": "Kolar Mandi",
  "model": "WEIGHTED_MOVING_AVERAGE_V1",
  "horizon": "7_DAYS",
  "currentVerifiedPrice": 2400.0,
  "forecastPrice": 2475.0,
  "forecastLowerBound": 2280.0,
  "forecastUpperBound": 2670.0,
  "direction": "UP",
  "confidence": "MEDIUM",
  "dataQuality": "GOOD",
  "observationsUsed": 28,
  "latestObservationDate": "2026-09-05",
  "unit": "QUINTAL",
  "currency": "INR",
  "generatedAt": "2026-09-06T12:00:00Z",
  "backtest": {
    "sampleCount": 20,
    "mae": 82.5,
    "mape": 4.1,
    "horizonEvaluated": "7_DAYS"
  },
  "limitations": [
    "Forecast is based on historical market prices and pure recursive Weighted Moving Average projection.",
    "Future market conditions may differ from historical observations."
  ],
  "disclaimer": "Forecasts are empirical statistical estimates based on historical verified market-price data and may differ from actual future prices. Verify the latest market information before making a selling decision."
}
```

---

## 2. Farmer Scenario Profitability

```http
POST /api/v1/farm-economics/econ-101/forecast-scenario?horizon=7_DAYS
Authorization: Bearer <Firebase_ID_Token>
```

### Response Example (`200 OK`)
```json
{
  "farmEconomicRecordId": "econ-101",
  "currentPriceUsed": 2400.0,
  "forecastPriceUsed": 2475.0,
  "priceDifference": 75.0,
  "priceDifferencePercentage": 3.13,
  "isScenario": true,
  "scenarioTag": "SCENARIO",
  "scenarioRevenueDelta": 7500.0,
  "scenarioProfitDelta": 7500.0,
  "disclaimer": "This is a read-only scenario profitability projection using estimated forecast prices. It does not alter your farm's recorded economic figures."
}
```
