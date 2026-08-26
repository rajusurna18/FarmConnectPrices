# Module 07 — Data Quality & Validation Policy

## Quality Status States
1. **`VERIFIED`**: Market data validated by verified ingestion pipelines. Standard default in UI views.
2. **`UNVERIFIED`**: Market observation recorded but awaiting verification. Exposed only with explicit visual tag.
3. **`REJECTED`**: Record failed price relationship checks (`minPrice <= modalPrice <= maxPrice` or `minPrice >= 0`). Excluded from standard user price queries and views.

## Validation Rules
- `minPrice >= 0 && maxPrice >= 0 && modalPrice >= 0`
- `minPrice <= modalPrice && modalPrice <= maxPrice`
- `marketId` and `cropId` must exist in master reference collections.
- Invalid records are rejected rather than silently modified.
