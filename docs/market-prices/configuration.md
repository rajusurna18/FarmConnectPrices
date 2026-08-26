# Configuration & Environment Guide

## Backend Environment Variables

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `DATA_GOV_IN_API_KEY` | Secret API key from data.gov.in (Backend only) | `YOUR_SECRET_KEY` |
| `DATA_GOV_IN_BASE_URL` | Base endpoint URL for data.gov.in resource API | `https://api.data.gov.in/resource` |
| `DATA_GOV_IN_RESOURCE_ID` | AGMARKNET dataset resource identifier | `9ef84268-d588-465a-a308-a864a43d0070` |
| `MARKET_PRICE_INGEST_ENABLED` | Toggle automatic scheduled ingestion | `false` |
| `MARKET_PRICE_INGEST_CRON` | Cron schedule for ingestion job | `0 30 18 * * *` |

## Rotation Procedure
If the API key is ever exposed or compromised:
1. Generate a new API key on your data.gov.in account dashboard.
2. Update `DATA_GOV_IN_API_KEY` in `backend/.env` or deployment environment variables.
3. Restart the Spring Boot application server.
