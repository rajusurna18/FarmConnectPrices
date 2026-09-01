# Module 10 — Data Model Specification

## Firestore Collection: `farmEconomics`

Document ID: `{economicRecordId}` (UUID)

```json
{
  "id": "uuid-string",
  "ownerUid": "firebase-uid",
  "farmId": "farm-id",
  "cropId": "crop-id",
  "farmName": "Green Acres",
  "cropName": "Rice (Paddy)",
  "season": "KHARIF",
  "cultivatedArea": 5.0,
  "cultivatedAreaUnit": "ACRE",
  "expectedYield": 10.0,
  "yieldUnit": "QUINTAL",
  "productionCosts": [
    {
      "id": "item-uuid-1",
      "category": "SEEDS",
      "description": "Paddy seed 20kg",
      "amount": 2500.0,
      "currency": "INR"
    },
    {
      "id": "item-uuid-2",
      "category": "FERTILIZER",
      "description": "Urea & NPK",
      "amount": 4000.0,
      "currency": "INR"
    }
  ],
  "sellingCosts": {
    "transportationCost": 300.0,
    "otherSellingCosts": 100.0
  },
  "totalProductionCost": 6500.0,
  "totalSellingCost": 400.0,
  "totalCost": 6900.0,
  "productionCostPerUnit": 650.0,
  "totalCostPerUnit": 690.0,
  "breakEvenSellingPrice": 690.0,
  "createdAt": "2026-09-01T23:00:00Z",
  "updatedAt": "2026-09-01T23:00:00Z"
}
```

## Production Cost Categories
- `SEEDS`
- `FERTILIZER`
- `PESTICIDES`
- `LABOR`
- `IRRIGATION`
- `MACHINERY`
- `LAND`
- `OTHER`
