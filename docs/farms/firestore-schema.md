# Firestore Schema — Module 05

## `farms/{farmId}`
```json
{
  "id": "farm-uuid-123",
  "ownerUid": "firebase-auth-uid",
  "name": "Green Acres Farm",
  "location": {
    "state": "Telangana",
    "district": "Warangal",
    "mandal": "Enumamula",
    "village": "Desrajupalle",
    "pincode": "506002"
  },
  "landArea": 5.5,
  "landAreaUnit": "ACRE",
  "status": "ACTIVE",
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

## `farmCrops/{farmCropId}`
Document ID format: `${farmId}_${cropId}_${season}` (e.g. `farm-123_crop-paddy_KHARIF`)

```json
{
  "id": "farm-123_crop-paddy_KHARIF",
  "farmId": "farm-123",
  "ownerUid": "firebase-auth-uid",
  "cropId": "crop-paddy",
  "season": "KHARIF",
  "status": "ACTIVE",
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

## `crops/{cropId}` (Global Reference)
```json
{
  "id": "crop-paddy",
  "name": "Rice / Paddy",
  "category": "CEREAL",
  "scientificName": "Oryza sativa",
  "status": "ACTIVE"
}
```

## `locations/{locationId}` (Global Reference)
```json
{
  "id": "loc-warangal",
  "state": "Telangana",
  "district": "Warangal",
  "mandal": "Warangal Urban",
  "village": "Enumamula",
  "pincode": "506002"
}
```
