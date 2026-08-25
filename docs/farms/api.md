# Module 05 REST APIs

All farm management endpoints require a valid Firebase ID Token in the `Authorization: Bearer <token>` header.

## Farm APIs
- `GET /api/v1/farms`: List all farms owned by authenticated UID (Role: `FARMER`).
- `POST /api/v1/farms`: Create a new farm record.
- `GET /api/v1/farms/{farmId}`: Get farm by ID (Enforces ownerUid == auth UID).
- `PUT /api/v1/farms/{farmId}`: Update farm record.
- `DELETE /api/v1/farms/{farmId}`: Delete farm record & associated farm crops.

## Farm Crop APIs
- `GET /api/v1/farms/{farmId}/crops`: Get crops for a farm.
- `POST /api/v1/farms/{farmId}/crops`: Add crop relationship to a farm.
- `PUT /api/v1/farms/{farmId}/crops/{farmCropId}`: Update season/status.
- `DELETE /api/v1/farms/{farmId}/crops/{farmCropId}`: Remove crop relationship.

## Reference APIs
- `GET /api/v1/crops`: Read-only Crop Master reference list.
- `GET /api/v1/locations`: Read-only Location Master taxonomy reference list.
