# Firestore Schema — User Profiles & Roles

## Collections

### 1. `users/{uid}`
Houses application-level account information.
```json
{
  "uid": "string",
  "displayName": "string",
  "email": "string",
  "emailVerified": boolean,
  "role": "FARMER" | "BUYER" | "USER" | "MIDDLEMAN" | "DELIVERY_PARTNER" | "ADMIN",
  "status": "ACTIVE" | "SUSPENDED",
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

### 2. `farmerProfiles/{uid}`
Houses farmer-specific profile foundation.
```json
{
  "uid": "string",
  "profileCompleted": boolean,
  "phoneNumber": "string",
  "location": {
    "state": "string",
    "district": "string",
    "mandal": "string",
    "village": "string"
  },
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

### 3. `buyerProfiles/{uid}`
Houses buyer-specific profile foundation.
```json
{
  "uid": "string",
  "profileCompleted": boolean,
  "phoneNumber": "string",
  "location": {
    "state": "string",
    "district": "string",
    "mandal": "string",
    "village": "string"
  },
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```
