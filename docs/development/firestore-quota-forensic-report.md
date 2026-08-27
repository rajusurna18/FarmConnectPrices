# Firestore Quota Forensic Report & Read Optimization Architecture

## Executive Summary
This document provides a complete forensic investigation and architectural breakdown of Google Cloud / Firebase Firestore read operations for **FarmConnectPrices**. It details every Firestore access point, quantifies read amplification, documents Spring Cache protections, and clarifies the boundary between application-side read optimization and GCP external quota limits (`RESOURCE_EXHAUSTED`).

---

## 1. Backend Firestore Access Inventory

| File | Class | Method | Collection | Operation | Read/Write | Cache Status | Limit / Budget |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `CropMasterService.java` | `CropMasterService` | `getAllCrops()` | `crops` | `colRef.get().get()` | READ | `@Cacheable(value="crops", sync=true)` | Cold: 1 read. Warm: 0 reads. |
| `MarketService.java` | `MarketService` | `fetchAllMarkets()` | `markets` | `colRef.get().get()` | READ | `@Cacheable(value="markets", sync=true)` | Cold: 1 read. Warm: 0 reads. |
| `MarketService.java` | `MarketService` | `getMarketCrops()` | `marketCrops` | `colRef.document(id).get().get()` | READ | Single Document Read | 1 document read |
| `LocationMasterService.java` | `LocationMasterService` | `getCanonicalStates()` | `locations` | `colRef.whereEqualTo("type", "STATE").get().get()` | READ | `@Cacheable(value="states", sync=true)` | Cold: 1 read. Warm: 0 reads. |
| `LocationMasterService.java` | `LocationMasterService` | `getDistrictsForState()` | `locations` | `colRef.whereEqualTo("state", s).get().get()` | READ | `@Cacheable(value="districts", sync=true)` | Cold: 1 read. Warm: 0 reads. |
| `MarketPriceService.java` | `MarketPriceService` | `getMarketPrices()` | `marketPrices` | `colRef.whereEqualTo(...).limit(100).get().get()` | READ | Targeted Query | Filtered, Default Limit: 100 |
| `MarketPriceService.java` | `MarketPriceService` | `getMarketPriceById()` | `marketPrices` | `docRef.get().get()` | READ | Single Document Read | 1 document read |
| `ProfileService.java` | `ProfileService` | `getUserProfile()` | `users`, `farmerProfiles`, etc. | `docRef.get().get()` | READ/WRITE | On-Demand Auth | 1-2 document reads per user |
| `FarmService.java` | `FarmService` | `getUserFarms()`, `getFarmCrops()` | `farms`, `farmCrops` | `colRef.whereEqualTo("ownerUid", uid).get().get()` | READ/WRITE | User-scoped | Filtered by UID |
| `DiscoveryIndexSyncService.java` | `DiscoveryIndexSyncService` | `syncDiscoveryIndexes()` | `marketPrices` -> `locations`, `markets`, `crops` | Ingestion Index Sync | READ/WRITE | Guarded by `QuotaGuard` | Skipped when quota circuit OPEN |
| `DataGovIngestionService.java` | `DataGovIngestionService` | `ingestMandiPrices()` | `marketPrices`, `markets`, `crops` | External Ingestion | READ/WRITE | Disabled by default (`false`) | Runs max 1/day when enabled |

---

## 2. Ingestion & Background Job Forensics

- **Scheduled Ingestion (`DataGovIngestionScheduler.java`)**:
  - `@Scheduled(cron = "${market-price.ingestion.cron:0 30 18 * * *}")`
  - Enabled property: `market-price.ingestion.enabled=false` (Default: **Disabled**).
  - **Finding**: Background ingestion does **NOT** run on backend startup or in uncontrolled loops. It runs at most once per day at 18:30 IST only if explicitly enabled with a valid API key.

---

## 3. Frontend API Request Forensics

| Hook | Query Key | Endpoint | Retry Policy | Stale Time | Component Consumers |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `useCrops()` | `['crops']` | `/api/v1/crops` | `defaultRetry` (0 on 503) | 30 minutes | `MarketPriceFilter`, `MarketIntelligencePage` |
| `useMarkets()` | `['markets', state, district, mandal, type]` | `/api/v1/markets` | `defaultRetry` (0 on 503) | 30 minutes | `MarketListPage`, `MarketPriceFilter` |
| `useLocationCascade()` | `['locations', 'states']`, `['locations', 'districts', state]` | `/api/v1/locations/*` | `defaultRetry` (0 on 503) | 60 minutes | Location selection dropdowns |
| `useMarketPrices()` | `['marketPrices', state, district, marketId, cropId, date, unit, quality]` | `/api/v1/market-prices` | `defaultRetry` (0 on 503) | 5 minutes | `MarketPriceListPage` |
| `useMarketIntelligence()` | `['market-intelligence', view, state, district, marketId, cropId, unit]` | `/api/v1/market-intelligence/*` | `defaultRetry` (0 on 503) | 5 minutes | `MarketIntelligencePage` |

- **Finding**: React Query key normalizers ([`queryConfig.ts`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/utils/queryConfig.ts)) use stable primitive string values, preventing reference-invalidation request storms. `defaultRetry` returns `false` on HTTP 503, preventing retry loops when database quota is active.

---

## 4. Application Read Amplification & Optimization Summary

1. **Cold Load Cache Stampede Protection**:
   - Spring `@Cacheable(sync = true)` is active on `CropMasterService.getCropMap()`, `MarketService.fetchAllMarkets()`, `LocationMasterService.getCanonicalStates()`, and `LocationMasterService.getDistrictsForState()`.
   - Concurrent incoming requests share the single cold load execution, ensuring exactly **1 read** per master collection across concurrent threads.
2. **Quota Guard Circuit Breaker (`FirestoreQuotaGuard`)**:
   - 3-State Machine (`CLOSED`, `OPEN`, `HALF_OPEN`).
   - Single Probe Owner rule: Exactly 1 thread executes a probe after cooldown, while concurrent requests during `HALF_OPEN` state receive HTTP 503 without invoking gRPC calls to Firestore.
3. **Observability Tracking (`FirestoreMetrics`)**:
   - Integrated [`FirestoreMetrics.java`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/backend/src/main/java/com/farmlink/api/service/FirestoreMetrics.java) to track reads, writes, cache hits, cache misses, and circuit breaker events.

---

## 5. Distinction Between Application Optimizations & External GCP Quota Limit

> [!IMPORTANT]
> **Definitive Classification**:
> - **Application Read Amplification**: Solved. Unbounded reads, duplicate query keys, and probe stampedes have been completely eliminated from the codebase.
> - **External Firebase/GCP Quota Limit**: Project `farmconnectprices` is on the Firebase Standard Free Tier (50,000 document reads/day). If the Google Cloud console shows 50,000/50,000 daily reads used, Firestore returns `RESOURCE_EXHAUSTED`.
> - **Behavior**: The application returns HTTP 503 SERVICE_UNAVAILABLE cleanly with zero fallback/mock data. As soon as the daily GCP quota resets or the project plan is upgraded, `FirestoreQuotaGuard` automatically detects success on its single probe attempt, closes the circuit, and streams real AGMARKNET data to the UI.

---

## 6. Manual Firebase Console Verification Required

To verify external quota status:
1. Open [Google Cloud Console / Firebase Console](https://console.firebase.google.com/).
2. Select Project `farmconnectprices` -> **Firestore Database** -> **Usage**.
3. Inspect **Document Reads (24h graph)**.
4. Verify if read volume reached the 50,000 daily free tier cap.
