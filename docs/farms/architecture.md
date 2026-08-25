# Module 05 — Farm Foundation Architecture

## Overview
FarmConnectPrices allows authenticated users with the **`FARMER`** profile to manage agricultural land records (farms) and associate crops across cultivation seasons (`KHARIF`, `RABI`, `ZAID`).

```
                    Firebase Authentication (UID)
                                  │
                                  ▼
                             users/{uid}
                                  │
                          (role == 'FARMER')
                                  │
                                  ▼
                           farms/{farmId}
                         (ownerUid == uid)
                                  │
                                  ▼
                       farmCrops/{farmCropId}
                (farmId, ownerUid, cropId, season)
```

## Primary Domain Model
- A **Farmer** can own multiple farms.
- A **Farm** has an owner UID, name, land area, land unit (`ACRE` / `HECTARE`), operational status (`ACTIVE` / `INACTIVE`), and location.
- A **FarmCrop** represents the relationship between a Farm, a Crop Master record, and a cultivation season (`KHARIF` | `RABI` | `ZAID`).

## Role Security Policy
Farm-management functionality belongs exclusively to **`FARMER`** users. Requests from `MEDIATOR_BUYER` or `CUSTOMER` users receive `HTTP 403 Forbidden` from Spring Boot backend services and are redirected to `/dashboard` by frontend route guards.
