# Ingestion Pipeline & Location Cascading Architecture

## Ingestion Workflow
1. **Fetch**: `DataGovMandiClient` fetches records from `api.data.gov.in/resource/35985678-0d79-46b4-9ed6-6f13308a1d24`.
2. **Mapping**: `MandiMappingService` maps normalized `(state, district, market)` to existing Module 06 Markets and `commodity` to Module 05 Crops. Unmapped records are safely skipped without mutating market/crop masters.
3. **Validation**: Price values are checked for numerical validity:
   - `minPrice >= 0`
   - `maxPrice >= 0`
   - `modalPrice >= 0`
   - `minPrice <= modalPrice <= maxPrice`
   Invalid records are marked `REJECTED` and excluded from public price discovery.
4. **Deduplication**: Records generate a deterministic Firestore document ID (`prc_gov_{marketId}_{cropId}_{date}`). Subsequent runs update existing observations rather than duplicating records.
5. **Source Attribution**:
   ```json
   "source": {
     "type": "GOVERNMENT",
     "name": "data.gov.in / AGMARKNET",
     "reference": "35985678-0d79-46b4-9ed6-6f13308a1d24"
   }
   ```

## Location Cascading Rules
- `GET /api/v1/locations/states`: List available States.
- `GET /api/v1/locations/districts?state=...`: List available Districts for selected State.
- `GET /api/v1/locations/areas?state=...&district=...`: List optional sub-areas for District (returns `[]` if no sub-area data exists).
- **District Fallback**: If a selected sub-area has no specific market, the UI gracefully falls back to district markets with notice.
