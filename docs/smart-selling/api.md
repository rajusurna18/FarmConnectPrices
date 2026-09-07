# Smart Selling Decision API Specification

## Endpoint

`POST /api/v1/smart-selling/evaluate`

## Security
- Spring Security + Firebase Authentication
- Restricted to role: `FARMER` (`@PreAuthorize("hasRole('FARMER')")`)

## Request Body

```json
{
  "cropId": "crop-1",
  "quantity": 10.0,
  "quantityUnit": "QUINTAL",
  "candidateMarketIds": ["market-1", "market-2"],
  "farmId": "farm-1",
  "economicRecordId": "econ-1",
  "customSellingCosts": {
    "transportationCost": 200,
    "otherSellingCosts": 50
  },
  "priceBasis": "MODAL",
  "forecastHorizon": "7_DAYS"
}
```

## Response Body

```json
{
  "status": "SUCCESS",
  "recommendedMarketId": "market-1",
  "recommendationType": "BEST_ESTIMATED_NET_REALIZATION",
  "decisionBasis": "COMPLETE_ECONOMIC_NET_REALIZATION",
  "overallDecisionConfidence": "HIGH",
  "primaryRecommendation": { ... },
  "rankedMarkets": [ ... ],
  "tradeOffs": [ ... ],
  "forecastScenarios": [ ... ],
  "aiExplanation": { ... }
}
```
