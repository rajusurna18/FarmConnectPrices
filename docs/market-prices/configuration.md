# Configuration & Environment Guide

## Backend Environment Variables

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `DATA_GOV_IN_API_KEY` | Secret API key from data.gov.in (Backend only) | `YOUR_SECRET_KEY` |
| `DATA_GOV_IN_BASE_URL` | Base endpoint URL for data.gov.in resource API | `https://api.data.gov.in/resource` |
| `DATA_GOV_IN_RESOURCE_ID` | Historical / Controlled Backfill resource identifier | `35985678-0d79-46b4-9ed6-6f13308a1d24` |
| `DATA_GOV_IN_DAILY_RESOURCE_ID` | Current Daily Bulletin resource identifier | `9ef84268-d588-465a-a308-a864a43d0070` |
| `MARKET_PRICE_INGEST_ENABLED` | Toggle automatic scheduled daily ingestion | `false` |
| `MARKET_PRICE_INGEST_CRON` | Cron schedule for daily ingestion job | `0 30 18 * * *` |
| `MARKET_PRICE_INGEST_MODE` | Ingestion mode (`INCREMENTAL` or `BACKFILL`) | `INCREMENTAL` |
| `MARKET_PRICE_INGEST_PAGE_SIZE` | Page size limit for API batch requests | `1000` |
| `MARKET_PRICE_INGEST_MAX_PAGES` | Maximum pages to fetch per run | `10` |
| `MARKET_PRICE_INGEST_LOOKBACK_DAYS` | Days lookback window for incremental sync | `30` |
| `MARKET_PRICE_INGEST_RETRY_ATTEMPTS` | Maximum retry attempts per HTTP request | `3` |
| `MARKET_PRICE_INGEST_REQUEST_TIMEOUT_MS` | HTTP Client connect/read timeout in milliseconds | `10000` |

## Rotation & Security Procedure
If the API key is ever exposed or compromised:
1. Generate a new API key on your `data.gov.in` account dashboard.
2. Update `DATA_GOV_IN_API_KEY` in `backend/.env` or deployment environment variables.
3. Restart the Spring Boot application server.
4. **Never** expose `DATA_GOV_IN_API_KEY` to the React frontend or log files.

