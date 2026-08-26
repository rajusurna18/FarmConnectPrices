# Administrative Location Master Guide

## Overview
FarmConnectPrices maintains an administrative Location Master independent of daily price observation records.

## Architecture
- **Service**: `LocationMasterService.java`
- **Data Model**: `LocationMasterResponse.java`
- **Firestore Collection**: `/locations/{locationId}`
- **Fallback**: Official Government of India administrative States and Districts reference master.

## REST Endpoints
- `GET /api/v1/locations/states`: List all canonical States available in the Location Master.
- `GET /api/v1/locations/districts?state=...`: List canonical Districts for a selected State.
- `GET /api/v1/locations/areas?state=...&district=...`: List optional sub-areas / mandals for a District (returns `[]` if unavailable).

## Separation Principle
Administrative locations exist independent of whether an AGMARKNET price observation was logged for a specific district today.
