# AGMARKNET Ingestion Architecture & Operational Guide

## Architecture Overview

FarmConnectPrices Version 1 implements an end-to-end Government of India AGMARKNET / `data.gov.in` agricultural price platform architecture:

```
AGMARKNET (data.gov.in API)
       │
       ▼ (Paginated Ingestion Engine / RestClient)
Spring Boot DataGovIngestionService
       │
       ▼ (Raw Source Provenance, Normalization, Observed Entity Discovery & Validation)
Cloud Firestore (/marketPrices/{priceId})
       │
       ▼ (REST API / Spring Boot Controllers)
Spring Boot MarketPriceController & LocationController
       │
       ▼ (Axios apiClient / React)
React Frontend Web Application
```

---

## Key Principles & Design Decisions

### 1. Exclusive External Data Provider
- **AGMARKNET / data.gov.in** is the **ONLY** external price provider for Version 1.
- No third-party API connectors (Farmer.in, MandiAPI) are configured or called.

### 2. Dual Ingestion Modes
- **`INCREMENTAL` Mode**: Used by `DataGovIngestionScheduler` for automated daily updates. Synchronizes recent mandi observations using Resource `9ef84268-d588-465a-a308-a864a43d0070` (~12,840 daily active records).
- **`BACKFILL` Mode**: Triggered deliberately for historical data population using Resource `35985678-0d79-46b4-9ed6-6f13308a1d24` with configurable pagination (`pageSize`, `maxPages`, `lookbackDays`, `offset`).

### 3. Broad Observed Entity Discovery (Zero Data Discard)
- Valid AGMARKNET observations across any state, district, market, or commodity in India are **NOT** discarded simply because they do not match canonical reference master lists (`DEFAULT_MARKETS`, `DEFAULT_CROPS`, `DEFAULT_LOCATIONS`).
- If an entity matches canonical master lists, `marketId` / `cropId` references canonical IDs (`mappingStatus = "MAPPED"`).
- If unmapped, the record is stored as an observed entity (`obs-mkt-<hash>`, `obs-crop-<hash>`) with `mappingStatus = "UNMAPPED"` and served seamlessly via public REST APIs.

### 4. Raw Source Provenance & Deterministic Normalization
- Unmodified external strings (`rawState`, `rawDistrict`, `rawMarket`, `rawCommodity`, `rawVariety`, `rawGrade`, `rawArrivalDate`, `rawMinPrice`, `rawMaxPrice`, `rawModalPrice`) are preserved under `source.*`.
- Strings are normalized deterministically for entity matching without corrupting raw source provenance.

### 5. Idempotent Deduplication & Price Validation
- Firestore document IDs are generated deterministically (`prc_gov_{slugMarket}_{slugCommodity}_{date}_{hash}`).
- Ingestion executes idempotent upsert via `.set(docMap)`.
- Enforces price validation: `minPrice >= 0 && maxPrice >= 0 && modalPrice >= 0 && minPrice <= modalPrice && modalPrice <= maxPrice`. Invalid records receive `qualityStatus = "REJECTED"`.

---

## Operational Controls

Ingestion can be manually triggered via `InternalIngestionController`:

```http
POST /api/v1/internal/market-prices/ingest?mode=INCREMENTAL&pageSize=1000&maxPages=10
Header: X-Internal-Secret: <configured-secret>
```

Parameters:
- `mode`: `INCREMENTAL` or `BACKFILL`
- `resourceId`: Overrides target AGMARKNET dataset resource ID
- `pageSize`: Records per page (default: 1000, max tested: 10000)
- `maxPages`: Page iteration limit
- `lookbackDays`: Maximum days window
- `offset`: Starting offset
