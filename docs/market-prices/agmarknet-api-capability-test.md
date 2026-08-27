# AGMARKNET / data.gov.in Live API Capability Test Report

## Executive Summary

A live API capability test was conducted on **2026-08-26** using the official Government of India `data.gov.in` platform with the configured `DATA_GOV_IN_API_KEY`. 

The test empirically evaluated pagination, record counts, field schemas, max page sizes, and filter parameters across the 3 selected AGMARKNET dataset resources.

---

## Live Capability Summary Table

| Resource ID | Dataset Name | Page Size Tested | Pagination Works? | Total/Estimated Records | Supported Filters | Date Fields | Technical Feasibility & Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `35985678-0d79-46b4-9ed6-6f13308a1d24` | Variety-wise Daily Market Prices Data of Commodity (Historical & Current) | 10, 100, 500, 1000, 5000, 10000 | **YES** | **81,410,937** (81.4M records) | `filters[state]`, `filters[district]`, `filters[market]`, `filters[commodity]` | `Arrival_Date` (dd/MM/yyyy) | **FEASIBLE**. Primary historical & current dataset. Supports up to 10,000 records per page. |
| `9ef84268-d588-465a-a308-a864a43d0070` | Current Daily Price of Various Commodities (AGMARKNET Daily Bulletin) | 10, 100, 500, 1000, 5000, 10000 | **YES** | **12,840** (Daily Snapshot) | `filters[state]`, `filters[district]`, `filters[market]`, `filters[commodity]` | `arrival_date` (dd/MM/yyyy) | **FEASIBLE**. Ideal for lightweight daily `INCREMENTAL` mode sync (fetches all daily mandi prices in ~2 pages). |
| `61761d66-1dbf-4101-a7e3-0f91ab7c20e6` | Monthly Wholesale Price Data (AGMARKNET Archive) | 10, 100, 500, 1000 | **NO** | **0** (Inactive/Empty) | N/A | None | **INACTIVE**. Resource returned zero records. Will not be used. |

---

## Detailed Test Results & Findings

### 1. Pagination Mechanics & Proof of Traversal
- **Offset Traversal**: Passing `offset=0`, `offset=10`, `offset=100`, `offset=200` verified that the API returns distinct, ordered records for each page.
  - *Offset 0 Sample*: State: West Bengal | Market: Sheoraphuly | Commodity: Potato | Date: 11/11/2009
  - *Offset 10 Sample*: State: West Bengal | Market: Sheoraphuly | Commodity: Potato | Date: 20/01/2009
  - *Offset 100 Sample*: State: Uttar Pradesh | Market: Gopiganj | Commodity: Green Peas | Date: 14/02/2023
  - *Offset 200 Sample*: State: Uttrakhand | Market: Khateema | Commodity: Potato | Date: 17/06/2009

### 2. Maximum Practical Page Size
- The API was tested with `limit` values of 100, 500, 1,000, 5,000, and 10,000.
- **Result**: `data.gov.in` successfully served **10,000 records in a single HTTP JSON response** with low latency (< 1.5 seconds).
- **Recommendation**: Use a default page size of `1000` to `5000` records per request for optimal memory usage and network efficiency.

### 3. Server-Side Filtering Capabilities
- **State Filtering**: `filters[state]=Punjab` returned `227,201` total records.
- **Commodity Filtering**: `filters[commodity]=Green Peas` returned `546,966` total records.
- Filtering allows FarmConnectPrices to perform targeted backfills by state or commodity if desired.

### 4. Response Metadata & Next-Page Calculation
- The response JSON structure provides explicit metadata:
  ```json
  {
    "total": 81410937,
    "count": 10000,
    "limit": 10000,
    "offset": 0,
    "records": [...]
  }
  ```
- **Next-Page Calculation**: Next offset is calculated as `offset + count`. Ingestion terminates safely when `records.length == 0` or `offset + count >= total`.

### 5. Architectural Feasibility Assessment
- **Proposed Paginated Ingestion**: **100% Technically Feasible**.
- **Mode Suitability**:
  - `INCREMENTAL` Mode: Uses Resource `9ef84268-d588-465a-a308-a864a43d0070` (12,840 records daily) or filtered Resource `35985678-0d79-46b4-9ed6-6f13308a1d24` with recent `arrival_date` lookback.
  - `BACKFILL` Mode: Uses Resource `35985678-0d79-46b4-9ed6-6f13308a1d24` with page traversal (`pageSize=5000`, `maxPages=N`).

---

## Security & Verification Constraints Compliance
1. `DATA_GOV_IN_API_KEY` was loaded securely from `backend/.env` and **never** exposed in logs, git history, or client responses.
2. Zero production code files were altered during this capability test.
3. Zero Firestore database records were modified or written.
4. No unlimited historical downloads were executed.
