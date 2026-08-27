# End-to-End Runtime Data Flow Architecture

## Pipeline Stages

```text
AGMARKNET Ingestion
        │
        ▼
Firestore (marketPrices collection)
        │
        ▼
Discovery Index Sync (Internal background job)
        │
        ▼
Lightweight Master Collections (locations, markets, crops, marketCrops)
        │
        ▼
Spring Boot Services (AtomicReference + Spring Cache @Cacheable(sync = true))
        │
        ▼
REST API (Public GET endpoints: /locations, /crops, /markets, /market-intelligence)
        │
        ▼
React Query (TanStack Query with staleTime & 503 retry protection)
        │
        ▼
React UI Components (3-State UX Data Availability Contract)
```

---

## 3-State Frontend Contract

- **State 1 — Real Data Available (HTTP 200)**: Displays `AGMARKNET Live Telemetry` badge and real records.
- **State 2 — No Data Available (HTTP 200 + `[]`)**: Displays `No market data available for the selected filters.`
- **State 3 — Temporarily Unavailable (HTTP 503 / Network Down)**: Displays `Market data temporarily unavailable — Live AGMARKNET telemetry has not reached FarmConnectPrices right now. Please try again shortly.` with **Retry** button.
