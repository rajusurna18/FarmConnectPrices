# Ingestion Pipeline & Quality Architecture

## Ingestion Workflow
1. **Fetch**: `DataGovMandiClient` fetches records from `api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070`.
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
     "reference": "9ef84268-d588-465a-a308-a864a43d0070"
   }
   ```

## Scheduled & Manual Triggers
- **Scheduled Ingestion**: `DataGovIngestionScheduler` runs on cron (`${market-price.ingestion.cron:0 30 18 * * *}`) when `${market-price.ingestion.enabled}` is set to `true`.
- **Manual Trigger**: `POST /api/v1/internal/market-prices/ingest` (requires admin authentication header/token).
