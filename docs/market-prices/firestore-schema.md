# Module 07 — Firestore Price Collection Schema (`marketPrices/{priceId}`)

## Document Structure

```json
{
  "id": "prc-gnt-chilli-20260826",
  "marketId": "mkt-guntur-mandi",
  "cropId": "crop-chilli",
  "priceDate": "2026-08-26",
  "observedAt": "2026-08-26T06:00:00Z",
  "minPrice": 18500.0,
  "maxPrice": 22500.0,
  "modalPrice": 20500.0,
  "currency": "INR",
  "unit": "QUINTAL",
  "source": {
    "type": "IMPORTED_DATA",
    "name": "Development Reference Seed Data",
    "reference": "ref-dev-seed-2026"
  },
  "qualityStatus": "VERIFIED",
  "status": "ACTIVE",
  "createdAt": "2026-08-26T06:00:00Z",
  "updatedAt": "2026-08-26T06:00:00Z"
}
```

## Field Definitions
- `id` (string): Unique document identifier.
- `marketId` (string): Foreign reference to `markets/{marketId}`.
- `cropId` (string): Foreign reference to `crops/{cropId}`.
- `priceDate` (string): ISO business date (`YYYY-MM-DD`).
- `observedAt` (timestamp/ISO): Timestamp of price recording.
- `minPrice` (number): Minimum traded rate.
- `maxPrice` (number): Maximum traded rate.
- `modalPrice` (number): Most representative/modal traded rate.
- `currency` (string): ISO currency code (default `INR`).
- `unit` (string): Standard price unit (default `QUINTAL`).
- `source` (object): Metadata containing `type`, `name`, `reference`.
- `qualityStatus` (string): `VERIFIED` | `UNVERIFIED` | `REJECTED`.
- `status` (string): `ACTIVE` | `INACTIVE`.
