# FIRESTORE SCHEMA — MARKET MASTER DATA

## 1. `markets` Collection

Document Path: `markets/{marketId}`

```json
{
  "id": "mkt-guntur-mandi",
  "name": "Guntur Agricultural Market",
  "code": "GNT-MND-001",
  "type": "MANDI",
  "location": {
    "state": "Andhra Pradesh",
    "district": "Guntur",
    "mandal": "Guntur West",
    "village": "Pattabhipuram",
    "pincode": "522006"
  },
  "latitude": 16.2974,
  "longitude": 80.4398,
  "status": "ACTIVE",
  "createdAt": "2026-08-26T00:00:00Z",
  "updatedAt": "2026-08-26T00:00:00Z"
}
```

### Fields:
- `id` (string, required): Unique document identifier.
- `name` (string, required): Display name of the market.
- `code` (string, required): Stable unique market code (e.g. `HYD-WHL-003`).
- `type` (enum, required): `MANDI` | `RYTHU_BAZAAR` | `WHOLESALE_MARKET` | `LOCAL_MARKET` | `OTHER`.
- `location` (map, required): Reuses Module 05 location structure.
- `latitude` / `longitude` (number, optional): Coordinates for future mapping telemetry.
- `status` (enum, required): `ACTIVE` | `INACTIVE`.

---

## 2. `marketCrops` Collection

Document Path: `marketCrops/{marketCropId}` (Deterministic ID: `{marketId}_{cropId}`)

```json
{
  "id": "mkt-guntur-mandi_crop-chilli",
  "marketId": "mkt-guntur-mandi",
  "cropId": "crop-chilli",
  "status": "ACTIVE",
  "createdAt": "2026-08-26T00:00:00Z",
  "updatedAt": "2026-08-26T00:00:00Z"
}
```

### Fields:
- `id` (string, required): Deterministic ID matching `{marketId}_{cropId}` to prevent duplicate market-crop relationships.
- `marketId` (string, required): Foreign key referencing `markets/{marketId}`.
- `cropId` (string, required): Foreign key referencing `crops/{cropId}`.
- `status` (enum, required): `ACTIVE` | `INACTIVE`.
