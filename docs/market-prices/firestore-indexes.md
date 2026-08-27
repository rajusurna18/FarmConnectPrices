# Firestore Indexing & Read Optimization Specification

## Overview
This document specifies the composite and single-field indexes required by FarmConnectPrices to optimize telemetry discovery and price filtering while maintaining strict quota bounds and preventing read amplification.

---

## Collections & Required Indexes

### 1. `locations` (Lightweight Discovery Master)
- **Document Schema**: `{ state: string, district: string, mandal?: string, village?: string, pincode?: string }`
- **Composite Indexes**:
  - `state` ASC, `district` ASC

### 2. `crops` (Lightweight Discovery Master)
- **Document Schema**: `{ name: string, category: string, scientificName?: string, status: string }`
- **Composite Indexes**:
  - `status` ASC, `name` ASC

### 3. `markets` (Lightweight Discovery Master)
- **Document Schema**: `{ name: string, code: string, type: string, status: string, location: { state: string, district: string, mandal: string } }`
- **Composite Indexes**:
  - `location.state` ASC, `location.district` ASC
  - `status` ASC, `location.state` ASC

### 4. `marketCrops` (Lightweight Relationship Master)
- **Document Schema**: `{ marketId: string, cropId: string, status: string }`
- **Composite Indexes**:
  - `marketId` ASC, `status` ASC

### 5. `marketPrices` (Price Observation Data)
- **Document Schema**: `{ marketId: string, cropId: string, observedState: string, observedDistrict: string, priceDate: string, qualityStatus: string }`
- **Composite Indexes**:
  - `observedState` ASC, `observedDistrict` ASC, `priceDate` DESC
  - `marketId` ASC, `cropId` ASC, `priceDate` DESC
  - `qualityStatus` ASC, `priceDate` DESC

---

## Read Amplification Elimination Strategy
- **Master Data Separation**: State, district, market, and crop dropdown queries query lightweight collections (`locations`, `markets`, `crops`) rather than scanning `marketPrices`.
- **Spring Server-Side Caching**: Enabled via `@Cacheable` (`states`, `districts`, `markets`, `crops`, `marketCrops`) with 60-minute TTL.
- **Client React Query Caching**: Hooks set `staleTime: 30 minutes` and `retry: 1`.
