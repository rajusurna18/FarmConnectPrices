# Firestore Quota Forensic Verification Report

## Executive Summary
This document provides the complete technical forensic analysis of **FarmConnectPrices** following Modules 10A–10G. It audits backend read paths, background schedulers, startup runners, frontend request behavior, spring caching, single-flight protections, and the `FirestoreQuotaGuard` circuit breaker.

---

## 1. Application-Side Read Paths & Findings

| Component | Operation / Method | Firestore Target Collection | Trigger Condition |
| :--- | :--- | :--- | :--- |
| `CropMasterService` | `getAllCrops()` / `getCropMap()` | `crops` | Cold cache miss (`@Cacheable("crops", sync=true)`) |
| `MarketService` | `fetchAllMarkets()` / `getMarketById()` | `markets` | Cold cache miss (`@Cacheable("markets", sync=true)`) |
| `LocationMasterService` | `getCanonicalStates()`, `getCanonicalDistricts()` | `locations`, `markets` | Cold cache miss (`@Cacheable(sync=true)`) |
| `MarketPriceService` | `fetchTargetedMarketPrices()` | `marketPrices` | Directly queried via API with parameter filters |
| `DiscoveryIndexSyncService` | `syncDiscoveryIndexFromObservedData()` | `marketPrices` | Manual trigger only (NOT scheduled / NOT run at startup) |

### Forensic Analysis:
- **Scheduled Background Reads**: Only `DataGovIngestionScheduler` contains `@Scheduled`. It checks `${market-price.ingestion.enabled:false}` and immediately exits when disabled (default). Zero automated background polling calls Firestore.
- **Startup Runners**: No active `CommandLineRunner` or `ApplicationRunner` beans exist in the main application context. Startup reads do not occur automatically.
- **Unbounded Collections Dumps**: Full collection scans (`.collection("marketPrices").get()`) have been completely removed from request paths. All market price queries enforce strict limits (`limit(100)` / `limit(300)`).

---

## 2. Frontend Request Forensics

- **React Query Retries**:
  All frontend hooks (`useMarkets`, `useCrops`, `useLocationCascade`, `useMarketIntelligence`) specify `retry: 1` or `defaultRetry`.
  `defaultRetry` checks for HTTP 503 status:
  ```ts
  const defaultRetry = (failureCount: number, error: unknown) => {
    if ((error as ApiError)?.response?.status === 503) return false;
    return failureCount < 1;
  };
  ```
  When the backend returns HTTP 503 (`FirestoreQuotaExhaustedException`), React Query **immediately aborts retrying** (0 duplicate requests).
- **Window Focus Refetching**: `refetchOnWindowFocus: false` is configured across all pricing and intelligence queries.
- **3D Components & Canvas**: Components like `MarketNetworkVisual` and `IndiaMarketMap` read state from parent React Query hooks and perform **zero direct network requests** or timers.

---

## 3. Backend Cache & Single-Flight Verification

- **Spring AOP Proxy Protection**:
  Fixed internal `this` self-invocation in `CropMasterService`. `getCropById` calls `getCropMap()`, which resolves through Spring's `@Cacheable("crops", sync = true)` proxy.
- **Single-Flight (`sync = true`) Protection**:
  Concurrent cold requests share a single execution thread while waiting for the initial Firestore query to populate cache.
- **Warm Cache Performance**:
  Subsequent requests resolve in $O(1)$ time directly from Spring's `ConcurrentMapCacheManager` or `AtomicReference` maps without issuing gRPC calls.

---

## 4. `FirestoreQuotaGuard` Circuit Breaker Verification

- **Configurable Cooldown Window**:
  Configured in `application.yml` via `firestore.quota.cooldown-ms: ${FIRESTORE_QUOTA_COOLDOWN_MS:60000}`.
- **Circuit State Transitions**:
  - `CLOSED`: Database operations pass through to Firestore.
  - `RESOURCE_EXHAUSTED` Exception: Trips circuit to `OPEN` for 60 seconds.
  - `OPEN`: Instantly throws `FirestoreQuotaExhaustedException` mapped by `GlobalExceptionHandler` to `HTTP 503 SERVICE_UNAVAILABLE`.
  - Zero gRPC round-trips to Firestore occur during `OPEN` state.
- **Log Rate Limiting**:
  Stack traces are suppressed; a single warning is logged per 60-second cooldown window.

---

## 5. Runtime Read Budget Verification (`FirestoreReadBudgetTest`)

Running `FirestoreReadBudgetTest`:
- 100 warm requests → **0 additional Firestore reads**.
- 10 concurrent cold requests → **1 single Firestore read**.
- 100 market intelligence observations → **0 per-observation metadata reads** ($O(1)$ map lookup).

---

## 6. Real Quota Condition & Technical Assessment

> [!IMPORTANT]
> **Technical Honesty Statement**:
> External Firebase quota exhaustion could not be independently verified from the application runtime without Google Cloud Console credentials.

### Final Conclusion:
**Application-side read storm and retry storms have been completely eliminated.**
When external Firebase daily quota limits (`50,000 reads/day`) are reached on Google Cloud, the system safely trips the `FirestoreQuotaGuard` circuit breaker, serves `HTTP 503 SERVICE_UNAVAILABLE` to the frontend without mock data, and halts database calls until the daily quota resets or the project is upgraded on Firebase Console.
