# Module 07 Location & Market Discovery Audit

## Executive Summary
This audit documents the current discovery architecture in FarmConnectPrices and identifies why location, market, and crop discovery were previously incomplete or constrained by daily price observation records.

## Audit Findings

### 1. Current Source of States
- **Previous Implementation**: States were fetched dynamically from `LocationController.getStates()`, which queried `MarketSummaryResponse` list derived from currently registered markets or observations.
- **Limitation**: States without an active market observation on a given day were absent from the state selector.

### 2. Current Source of Districts
- **Previous Implementation**: Districts were fetched via `LocationController.getDistricts?state=...`, which filtered `MarketSummaryResponse` list by state.
- **Limitation**: Districts without a registered market or price record were omitted, preventing users from exploring administrative location structures.

### 3. Current Source of Markets
- **Previous Implementation**: Markets were retrieved from `MarketService.getMarkets(...)` which searched `markets` in Firestore or seed list.
- **Limitation**: Tightly coupled to whether price observation records had mapped the market.

### 4. Current Source of Crops
- **Previous Implementation**: Crops were fetched from `CropMasterService.getAllCrops()` or filtered via market price observations.
- **Limitation**: When price observations were absent for a specific crop on a given day, UI filters risked hiding valid canonical crops from the Crop Master.

### 5. Role of AGMARKNET Daily Prices
- **Dataset Resource**: `35985678-0d79-46b4-9ed6-6f13308a1d24` ("Variety-wise Daily Market Prices Data of Commodity").
- **Intended Purpose**: Primary source for price observations (`minPrice`, `maxPrice`, `modalPrice`), price dates, arrival dates, and observation source metadata.
- **Misuse Identified**: Treating daily price observation records as the sole administrative location master.

## Corrected Master vs. Price Architecture

```
                    FARMCONNECTPRICES
                           |
             +-------------+-------------+
             |                           |
       MASTER DATA                  PRICE DATA
             |                           |
       Location Master             AGMARKNET
       Market Master               Daily Prices
       Crop Master
             |
             v
 State -> District -> Optional Area -> Market -> Crop
                                              |
                                              v
                                        Real Price Data
```

## Refactoring Plan & Files to Modify
1. `LocationMasterService.java`: Expand to serve as the canonical Indian administrative Location Master (States, Districts, optional Mandals).
2. `LocationController.java`: Serve States and Districts directly from `LocationMasterService` independent of daily price observations.
3. `MarketService.java`: Retain independent Market Master discovery (`markets` collection).
4. `CropMasterService.java`: Retain independent Crop Master discovery (`crops` collection).
5. `useLocationCascade.ts`: Update frontend location cascading hooks to consume the Location Master endpoints.
6. `MarketListPage.tsx` & `MarketFilterDrawer.tsx`: Ensure location cascading uses Location Master and displays district fallback notices when sub-area markets are unavailable.
7. Documentation: Create `location-master.md`, `market-discovery.md`, and `crop-discovery.md`.
