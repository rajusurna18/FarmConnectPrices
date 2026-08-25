# REST API SPECIFICATION — MARKET MASTER DATA

## Endpoints Summary

All endpoints require HTTP Bearer authentication token in header (`Authorization: Bearer <Firebase_ID_Token>`).

---

### 1. `GET /api/v1/markets`
Retrieves a list of market master summaries. Supports server-side query filters.

#### Query Parameters:
- `state` (optional, string): Filter by state name (e.g. `Andhra Pradesh`).
- `district` (optional, string): Filter by district (e.g. `Guntur`).
- `mandal` (optional, string): Filter by mandal.
- `type` (optional, string): Filter by market type (`MANDI`, `RYTHU_BAZAAR`, `WHOLESALE_MARKET`, `LOCAL_MARKET`, `OTHER`).
- `status` (optional, string): Filter by status (`ACTIVE`, `INACTIVE`).
- `cropId` (optional, string): Filter by supported crop ID (e.g. `crop-chilli`).
- `limit` (optional, integer): Max results limit (default: 50, max: 100).

#### Response (200 OK):
```json
[
  {
    "id": "mkt-guntur-mandi",
    "name": "Guntur Agricultural Market",
    "code": "GNT-MND-001",
    "type": "MANDI",
    "state": "Andhra Pradesh",
    "district": "Guntur",
    "mandal": "Guntur West",
    "status": "ACTIVE",
    "supportedCropCount": 4
  }
]
```

---

### 2. `GET /api/v1/markets/{marketId}`
Retrieves detailed metadata for a single market by ID.

#### Response (200 OK):
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

---

### 3. `GET /api/v1/markets/{marketId}/crops`
Retrieves list of agricultural crops supported/traded at the specified market.

#### Response (200 OK):
```json
[
  {
    "id": "mkt-guntur-mandi_crop-chilli",
    "marketId": "mkt-guntur-mandi",
    "cropId": "crop-chilli",
    "cropName": "Red Chilli",
    "cropCategory": "SPICE",
    "cropScientificName": "Capsicum annuum",
    "status": "ACTIVE",
    "createdAt": "2026-08-26T00:00:00Z",
    "updatedAt": "2026-08-26T00:00:00Z"
  }
]
```
