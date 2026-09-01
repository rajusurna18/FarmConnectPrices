# Module 10 — REST API Specification

Base Path: `/api/v1/farm-economics`

All endpoints require a valid Firebase Authorization header: `Authorization: Bearer <idToken>` and `FARMER` role.

## Endpoints Summary

| Method | Endpoint | Description | Role Required |
|---|---|---|---|
| `GET` | `/api/v1/farm-economics` | List authenticated farmer's economic records | `FARMER` |
| `POST` | `/api/v1/farm-economics` | Create new farm economic record | `FARMER` |
| `GET` | `/api/v1/farm-economics/{id}` | Get economic record detail | `FARMER` |
| `PUT` | `/api/v1/farm-economics/{id}` | Update existing economic record | `FARMER` |
| `DELETE` | `/api/v1/farm-economics/{id}` | Delete economic record | `FARMER` |
| `POST` | `/api/v1/farm-economics/evaluate` | Evaluate live market profitability | `FARMER` |
| `POST` | `/api/v1/farm-economics/compare-markets` | Compare profitability across multiple markets | `FARMER` |

## Response Status Codes
- `200 OK`: Success.
- `201 CREATED`: Record created successfully.
- `400 BAD REQUEST`: Invalid input payload or negative amounts.
- `401 UNAUTHORIZED`: Unauthenticated request / missing Firebase token.
- `403 FORBIDDEN`: Non-Farmer role (`MEDIATOR_BUYER`, `CUSTOMER`) or cross-farmer access attempt.
- `404 NOT FOUND`: Non-existent farm, crop, or economic record.
- `422 UNPROCESSABLE ENTITY` / Status `NO_VERIFIED_PRICE` / `UNIT_MISMATCH`: No verified price available or unit incompatibility.
- `503 SERVICE UNAVAILABLE`: Firestore quota exhaustion circuit breaker active.
