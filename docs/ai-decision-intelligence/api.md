# Module 11 — REST API Specification

Base Path: `/api/v1/ai/decisions`

Requires Authorization header: `Authorization: Bearer <idToken>` and `FARMER` profile role.

## Endpoint: `POST /api/v1/ai/decisions`

### Request Payload (`AiDecisionRequest`)
```json
{
  "decisionType": "MARKET_SELECTION",
  "cropId": "crop-uuid",
  "farmId": "farm-uuid",
  "economicRecordId": "record-uuid",
  "marketIds": ["market-1", "market-2"],
  "quantity": 10.0,
  "quantityUnit": "QUINTAL",
  "priceBasis": "MODAL",
  "priceMode": "LATEST_AVAILABLE",
  "date": "2026-09-02",
  "transportationCost": 300.0,
  "otherSellingCosts": 100.0
}
```

### Response Payload (`AiDecisionResponse`)
```json
{
  "decisionType": "MARKET_SELECTION",
  "summary": "Based on current verified prices and estimated net realization, Warangal Mandi currently appears most favorable for Rice (Paddy).",
  "recommendation": "Warangal Mandi",
  "confidence": "HIGH",
  "verifiedFacts": [
    "Verified modal price at Warangal Mandi: ₹2200.00 per QUINTAL",
    "Transportation cost: ₹300.00, Other selling costs: ₹100.00",
    "Evaluated quantity: 10.0 QUINTAL"
  ],
  "calculatedMetrics": {
    "selectedPrice": 2200.00,
    "expectedYield": 10.0,
    "yieldUnit": "QUINTAL",
    "grossRevenue": 22000.00,
    "totalSellingCost": 400.00,
    "estimatedNetRealization": 21600.00
  },
  "reasoning": [
    "Warangal Mandi offers an estimated net realization of ₹21600.00 for 10.0 QUINTAL.",
    "Outperforms Khammam Mandi by an estimated net realization advantage of ₹800.00."
  ],
  "risks": [
    "Actual transportation costs can fluctuate depending on vehicle availability and fuel rates.",
    "Market prices fluctuate throughout the day and can change before sale execution."
  ],
  "nextSteps": [
    "Contact mandi representative or local trader at Warangal Mandi to confirm today's arrival prices.",
    "Confirm transportation availability and fixed freight charges.",
    "Verify crop moisture and grading standards before loading."
  ],
  "engine": "RULE_BASED",
  "aiGenerated": false,
  "dataFreshness": {
    "marketPriceDate": "2026-09-02",
    "status": "CURRENT",
    "stalePrice": false,
    "staleMessage": "Based on current verified market price date (2026-09-02)."
  },
  "limitations": [
    "Estimates are based on latest verified market data and farmer-entered costs. Actual market prices and logistics costs may vary at the time of transaction."
  ]
}
```
