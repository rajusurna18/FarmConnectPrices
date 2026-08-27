# Firestore Read Budget & $O(1)$ In-Memory Cache Architecture

## Objective
Prevent Firestore read storms (`RESOURCE_EXHAUSTED: Quota exceeded`) by enforcing $O(1)$ in-memory metadata resolution and single-flight request protection.

---

## Read Budget Guarantees

1. **Cold Cache Initialization**:
   - First request for crops → **1 bounded Firestore collection read** (`crops`).
   - First request for markets → **1 bounded Firestore collection read** (`markets`).
   - First request for locations → **1 bounded Firestore collection read** (`locations`).
2. **Warm Cache Operation**:
   - Next 100 requests for crops, markets, or locations → **0 additional Firestore reads**.
3. **Observation Metadata Resolution**:
   - Querying 100 price observations (`/api/v1/market-intelligence/trends`) → **1 price observation query + 0 per-record market/crop reads**.
4. **Observation Market (`obs-mkt-*`) Lookups**:
   - Unknown or observation-derived market IDs (`obs-mkt-*`) look up in the cached market map.
   - If unmapped, metadata is constructed in-memory from document-embedded fields with **0 Firestore document lookups**.
5. **Single-Flight Concurrency (`@Cacheable(..., sync = true)`)**:
   - 10 simultaneous identical requests for a cold cache result in **exactly 1 Firestore read**. Remaining 9 threads wait and reuse the populated cache.

---

## Automated Verification Test
Verified via `com.farmlink.api.service.FirestoreReadBudgetTest` in `backend/src/test/java/com/farmlink/api/service/FirestoreReadBudgetTest.java`.
