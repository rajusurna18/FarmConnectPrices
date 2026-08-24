# Firestore Collections Schema — Three Primary Profiles

## 1. `users/{uid}`
```json
{
  "uid": "string",
  "displayName": "string",
  "email": "string",
  "emailVerified": boolean,
  "role": "FARMER" | "MEDIATOR_BUYER" | "CUSTOMER" | "USER",
  "status": "ACTIVE",
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

## 2. `farmerProfiles/{uid}`
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

## 3. `mediatorBuyerProfiles/{uid}`
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
  "businessOrganizationName": "string",
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

## 4. `customerProfiles/{uid}`
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
  "address": "string",
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```
