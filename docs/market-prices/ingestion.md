# Ingestion Pipeline & Location Cascading Architecture

## Ingestion Workflow
1. **Fetch**: `DataGovMandiClient` fetches records from AGMARKNET resource `9ef84268-d588-465a-a308-a864a43d0070` (`INCREMENTAL` mode for daily bulletin) or `35985678-0d79-46b4-9ed6-6f13308a1d24` (`BACKFILL` mode for historical ranges) using configurable page limits (`pageSize`, default 1,000) and request timeouts.
2. **Entity Discovery & Mapping**:
   - `MandiMappingService` attempts deterministic mapping of `(state, district, market)` to canonical Module 06 markets and `commodity` to Module 05 crops.
   - **Observed Entity Discovery**: If an external market or commodity is missing from canonical reference master lists, it is **NOT** discarded. It is preserved as an observed source entity (`obs-mkt-<hash>`, `obs-crop-<hash>`) with `mappingStatus = "UNMAPPED"` and rendered via the REST API.
3. **Raw Provenance Preservation**:
   Unmodified external strings (`rawState`, `rawDistrict`, `rawMarket`, `rawCommodity`, `rawVariety`, `rawGrade`, `rawArrivalDate`, `rawMinPrice`, `rawMaxPrice`, `rawModalPrice`) are preserved under `source`.
4. **Validation**: Price values are checked for numerical validity:
   - `minPrice >= 0`
   - `maxPrice >= 0`
   - `modalPrice >= 0`
   - `minPrice <= modalPrice <= maxPrice`
   Invalid records are marked `qualityStatus = "REJECTED"` and tracked in metrics.
5. **Deduplication & Idempotent Upsert**: Records generate a deterministic Firestore document ID (`prc_gov_{slugMarket}_{slugCommodity}_{date}_{hash}`). Repeated ingestion executes idempotent upsert via `.set(docMap)`.
6. **Operational Metrics**: Ingestion returns detailed operational metrics in `IngestionResultDto`: `recordsFetched`, `recordsProcessed`, `recordsAccepted`, `recordsRejected`, `recordsSkipped`, `recordsUnmappedMarkets`, `recordsUnmappedCrops`, `recordsUpserted`, `recordsUnchanged`, `pagesFetched`, `lastOffset`, `durationMs`, and `errors`.

## Location Cascading Rules
- `GET /api/v1/locations/states`: List available States (combining canonical master & observed AGMARKNET states).
- `GET /api/v1/locations/districts?state=...`: List available Districts for selected State (combining canonical master & observed AGMARKNET districts).
- `GET /api/v1/locations/areas?state=...&district=...`: List optional sub-areas for District (returns `[]` if no sub-area data exists).
- **District Fallback**: If a selected sub-area has no specific market, the UI gracefully falls back to district markets with notice.

