# Decision Support API Specification

## Authentication & Authorization
All endpoints require a valid Firebase ID token in the `Authorization: Bearer <token>` header and strictly authorize the `FARMER` role.

| Endpoint | Method | Role | Unauthenticated | Non-Farmer |
|---|---|---|---|---|
| `/api/v1/decision-support/evaluate` | POST | FARMER | 401 Unauthorized | 403 Forbidden |
| `/api/v1/decision-support/compare-markets` | POST | FARMER | 401 Unauthorized | 403 Forbidden |

## 1. Single Market Evaluation
`POST /api/v1/decision-support/evaluate`

### Request Payload
```json
{
  "cropId": "CROP_RICE_001",
  "marketId": "MKT_WGL_001",
  "quantity": 10.0,
  "quantityUnit": "QUINTAL",
  "priceBasis": "MODAL",
  "priceMode": "LATEST_AVAILABLE",
  "date": null,
  "transportationCost": 300.0,
  "otherSellingCosts": 100.0
}
```

### Response Payload (200 OK)
```json
{
  "status": "SUCCESS",
  "message": "Evaluation completed successfully.",
  "crop": { "id": "CROP_RICE_001", "name": "Rice" },
  "market": { "id": "MKT_WGL_001", "name": "Warangal Mandi", "state": "Telangana" },
  "selectedPrice": 2800.00,
  "priceBasis": "MODAL",
  "priceUnit": "QUINTAL",
  "quantity": 10.00,
  "quantityUnit": "QUINTAL",
  "grossRevenue": 28000.00,
  "transportationCost": 300.00,
  "otherSellingCosts": 100.00,
  "totalSellingCosts": 400.00,
  "estimatedNetRealization": 27600.00,
  "netRealizationPerUnit": 2760.00,
  "sellingCostBreakEvenPrice": 40.00,
  "currency": "INR",
  "priceDate": "2026-08-31",
  "observedAt": "2026-08-31T10:00:00Z",
  "source": { "type": "AGMARKNET", "name": "Agmarknet Gov" },
  "qualityStatus": "VERIFIED",
  "isStalePrice": false,
  "staleMessage": null,
  "isLoss": false
}
```

## 2. Multi-Market Comparison
`POST /api/v1/decision-support/compare-markets`

### Request Payload
```json
{
  "cropId": "CROP_RICE_001",
  "quantity": 10.0,
  "quantityUnit": "QUINTAL",
  "priceBasis": "MODAL",
  "priceMode": "LATEST_AVAILABLE",
  "markets": [
    { "marketId": "MKT_WGL_001", "transportationCost": 300.0, "otherSellingCosts": 100.0 },
    { "marketId": "MKT_KHM_001", "transportationCost": 100.0, "otherSellingCosts": 50.0 }
  ]
}
```
