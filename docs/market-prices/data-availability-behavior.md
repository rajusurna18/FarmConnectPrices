# Data Availability & Error State Specifications (Module 10B)

## 1. Overview
FarmConnectPrices enforces strict data integrity rules across all agricultural price, market, location, and crop discovery interfaces. Hardcoded or default sample agricultural data (`DEFAULT_LOCATIONS`, `DEFAULT_CROPS`, `DEFAULT_MARKETS`, `DEFAULT_PRICES`) has been completely removed to prevent misleading users when live telemetry is unavailable.

---

## 2. 3-State Data Availability Contract

Every UI page and component that consumes agricultural telemetry adheres to a strict 3-state contract:

| Status | HTTP Response | Data Payload | Frontend UI Rendering |
| :--- | :--- | :--- | :--- |
| **1. REAL DATA AVAILABLE** | `HTTP 200 OK` | Populated array (`len > 0`) | Render live AGMARKNET price cards, metrics, and location options. Display `DataAvailabilityBadge` as `REAL_DATA`. |
| **2. NO DATA AVAILABLE** | `HTTP 200 OK` | Empty array (`[]`) | Render clean empty state banner: *"No market data available for the selected filters."* Display `DataAvailabilityBadge` as `NO_DATA`. |
| **3. DATA TEMPORARILY UNAVAILABLE** | `HTTP 503 Service Unavailable` | `{"error": "Live agricultural market data is temporarily unavailable. Please retry."}` | Render error state card: *"Market data temporarily unavailable - Live AGMARKNET telemetry has not reached FarmConnectPrices right now. Please try again shortly."* with a **Retry** button. Display `DataAvailabilityBadge` as `TEMPORARILY_UNAVAILABLE`. |

---

## 3. Backend Error Mapping Rules (`GlobalExceptionHandler.java`)

1. **Firestore Resource Exhaustion / Quota Exceeded**:
   `StatusRuntimeException` with status code `RESOURCE_EXHAUSTED` or `UNAVAILABLE` → Mapped directly to `HTTP 503 Service Unavailable`.
2. **Database Failure**:
   General database read failures in `LocationMasterService`, `MarketService`, `CropMasterService`, or `MarketPriceService` → Throws `RuntimeException` / `ServiceUnavailableException` → Mapped to `HTTP 503`.
3. **Not Found**:
   `NoSuchElementException` for missing entity IDs → Mapped to `HTTP 404 Not Found`.
4. **Validation Failure**:
   `IllegalArgumentException` / invalid parameters → Mapped to `HTTP 400 Bad Request`.

---

## 4. Frontend Component Behaviors

### 4.1 Filter Selectors (`MarketPriceFilterBar.tsx`)
- If states/districts/markets/crops are loading: Display `Loading...` option.
- If zero items exist in Firestore: Display `No states currently available`, `No markets currently available`, or `No crops currently available`.
- **Zero Fallbacks**: Selectors will never fall back to the old 8 default states (`Telangana`, `Andhra Pradesh`, `Karnataka`, `Maharashtra`, `Tamil Nadu`, `Gujarat`, `Punjab`, `Haryana`).

### 4.2 Telemetry Status Badge (`DataAvailabilityBadge.tsx`)
Exposes live telemetry health on `MarketPriceListPage` and `MarketIntelligencePage`:
- 🟢 `AGMARKNET Live Telemetry (N Records)`
- ⚪ `No AGMARKNET Observations Found`
- 🔴 `Telemetry Temporarily Unavailable` (with inline Retry trigger)

---

## 5. Verification Checklist

- [x] All default agricultural reference data constants removed.
- [x] Spring `@Cacheable` caches discovery lists (`states`, `districts`, `markets`, `crops`).
- [x] Firestore read failures map to HTTP 503 (not silent fallback).
- [x] All 111 backend unit and integration tests pass.
- [x] Frontend ESLint passes with 0 warnings.
- [x] Production build passes cleanly.
