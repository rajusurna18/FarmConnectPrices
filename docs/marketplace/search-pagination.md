# Search & Pagination — Module 15

## Bounded Queries & Pagination Design
To prevent Firestore `RESOURCE_EXHAUSTED` quota failures, marketplace browsing utilizes bounded server-side queries.

### Search & Filtering Parameters
- `cropId`: Filter by Crop Master ID.
- `state`: Filter by State name.
- `district`: Filter by District name.
- `minPrice` / `maxPrice`: Numerical filter on seller asking price.
- `sortBy`: `newest` (default), `price_asc`, `price_desc`, `quantity_desc`.

### Pagination Parameters
- `page`: 0-indexed page number.
- `size`: Bounded page size (default 20, max 50).

### Caching Strategy
- **Frontend**: TanStack Query caches browse responses for 60 seconds (`staleTime: 60000ms`), avoiding duplicate requests when switching tabs or re-rendering components.
- **Backend**: Protected by `FirestoreQuotaGuard` circuit breaker.
