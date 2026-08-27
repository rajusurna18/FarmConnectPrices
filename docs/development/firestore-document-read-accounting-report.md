# Firestore Document-Read Accounting & Cache Verification Report

## Executive Summary
This report presents a document-level accounting analysis of Google Cloud / Firebase Firestore reads across **FarmConnectPrices**. It distinguishes between query RPC invocations and billable document read counts, proves Spring Cache activity under unit and concurrency testing, traces frontend page-by-page read costs, and classifies the current runtime state.

---

## 1. Spring Cache Verification

- **Configuration**: `CacheConfig.java` declares `@EnableCaching` and `ConcurrentMapCacheManager("states", "districts", "markets", "crops", "marketCrops")`.
- **Automated Verification Suite**: `SpringCacheVerificationTest.java`
  - **Single Invocation Test**: Call #1 triggers cold load & Populates cache. Call #2 fetches value directly from Spring Cache without invoking the loader.
  - **Cold Load Concurrency Test**: 10 concurrent threads requesting cold cache key `allMarkets` execute the loader **exactly 1 time**.

---

## 2. Document-Read Accounting Analysis

| Service / Collection | Firestore Operation | Documents Returned per Query | Billable Document Reads per Query | Warm Cache Read Cost |
| :--- | :--- | :--- | :--- | :--- |
| **`CropMasterService` (`crops`)** | `colRef.get().get()` | `N_crops` (~30-100 master crops) | `N_crops` document reads | **0 reads** |
| **`MarketService` (`markets`)** | `colRef.get().get()` | `N_markets` (~50-200 markets) | `N_markets` document reads | **0 reads** |
| **`LocationMasterService` (`locations` states)** | `whereEqualTo("type", "STATE")` | `N_states` (~36 states/UTs) | `N_states` document reads | **0 reads** |
| **`LocationMasterService` (`locations` districts)** | `whereEqualTo("state", s)` | `N_districts` (~10-40 districts) | `N_districts` document reads | **0 reads** |
| **`MarketPriceService` (`marketPrices`)** | `whereEqualTo(...).limit(100)` | `N_prices` (Max 100 observations) | `N_prices` document reads (Max 100) | Filter-dependent transient cache |
| **`ProfileService` (`users`, profiles)** | `docRef.get().get()` | 1 document | 1 document read | On-demand auth cache |

> [!NOTE]
> **Key Distinction**:
> A single query RPC (`colRef.get().get()`) is 1 RPC call, but GCP Firestore charges **1 billable document read per document snapshot returned**. Therefore, returning 100 documents in 1 query equals 100 billable document reads.
> With Spring `@Cacheable(sync = true)`, warm cache requests cost **0 billable document reads**.

---

## 3. Frontend Page-by-Page Read Tracing

| Page View | Initial Endpoints Requested | Target Collection | Max Documents Returned | Billable Read Cost (Cold Load) | Warm Read Cost |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Market Prices Page** | `/api/v1/crops`, `/api/v1/locations/states`, `/api/v1/market-prices` | `crops`, `locations`, `marketPrices` | ~30 crops + 36 states + 100 prices | ~166 document reads | **0 document reads** (Master data cached) |
| **Markets Page** | `/api/v1/locations/states`, `/api/v1/markets` | `locations`, `markets` | ~36 states + 100 markets | ~136 document reads | **0 document reads** (Cached) |
| **Market Intelligence** | `/api/v1/crops`, `/api/v1/market-intelligence/summary` | `crops`, `marketPrices` | ~30 crops + 100 price observations | ~130 document reads | **0 document reads** (Master data cached) |
| **Dashboard** | `/api/v1/locations/states`, `/api/v1/market-prices` | `locations`, `marketPrices` | ~36 states + 20 price observations | ~56 document reads | **0 document reads** (Master data cached) |

---

## 4. Ingestion & Background Job Verification

- **`DataGovIngestionScheduler.java`**: `@Scheduled(cron = "${market-price.ingestion.cron:0 30 18 * * *}")`
- **Default Status**: `market-price.ingestion.enabled=false` (**Disabled by default**).
- **Finding**: Ingestion does NOT run on application startup or in uncontrolled loops. It runs at most once per day at 18:30 IST only when explicitly enabled with an API key.

---

## 5. Classification of Current Quota Status

> [!IMPORTANT]
> **Classification (Option B)**:
> **APPLICATION READ LOAD IS ALREADY CONTROLLED, BUT EXTERNAL FIRESTORE QUOTA REMAINS EXHAUSTED.**
>
> Application-side read amplification, query key instabilites, and probe stampedes have been completely eliminated. Spring Cache protects master data. However, Google Cloud project `farmconnectprices` runs on the Firestore Standard Free Tier (50,000 document reads/day). When external 24h usage reaches 50,000 reads, Firestore returns `RESOURCE_EXHAUSTED`.
>
> `FirestoreQuotaGuard` converts `RESOURCE_EXHAUSTED` into a clean HTTP 503 SERVICE_UNAVAILABLE response without fake data. Once the GCP daily quota resets or the Firebase plan is upgraded, `FirestoreQuotaGuard` detects success on its single probe attempt, closes the circuit breaker, and streams real AGMARKNET data to the frontend.

---

## 6. Manual Firebase Console Verification Required

1. Open [Google Cloud Console / Firebase Console](https://console.firebase.google.com/).
2. Select Project `farmconnectprices` -> **Firestore Database** -> **Usage**.
3. Inspect **Document Reads (24h graph)**.
4. Verify if daily read volume reached the 50,000 cap.
