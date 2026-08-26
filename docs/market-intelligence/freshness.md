# Data Freshness & Stale Data Policy

## Freshness Policy
1. Every intelligence result explicitly returns `latestObservationDate` or `date`.
2. The UI explicitly labels data as "Latest Available Verified Price: YYYY-MM-DD".
3. Prices are never misleadingly labeled as "Today's Price" unless the observation date matches today's date.
