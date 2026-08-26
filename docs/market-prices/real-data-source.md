# Real Mandi Data Source Architecture

## Overview
FarmConnectPrices integrates with the official Government of India Open Government Data Platform (`data.gov.in`) dataset:
- **Title**: Current Daily Price of Various Commodities from Various Markets (Mandi)
- **Resource ID**: `9ef84268-d588-465a-a308-a864a43d0070`
- **Source Agency**: AGMARKNET (Directorate of Marketing & Inspection)

## Security Architecture
1. **Secret Storage**: The `DATA_GOV_IN_API_KEY` exists exclusively in the Spring Boot backend environment (`backend/.env` or system environment variables).
2. **Zero Client Exposure**: The API key is **never** embedded in React frontend code, frontend `.env`, browser localStorage/sessionStorage, Git tracking, or application logs.
3. **Mediated Access**: The frontend queries Spring Boot (`/api/v1/market-prices`), which reads from Firestore. The external API is an ingestion pipeline source, not a client proxy.

## Data Flow
```
data.gov.in (AGMARKNET API)
       │
       ▼ (HTTPS / Secure Backend Client)
Spring Boot DataGovIngestionService
       │
       ▼ (Validation & Deduplication)
Cloud Firestore (/marketPrices/{priceId})
       │
       ▼ (REST API / Firebase Admin SDK)
Spring Boot MarketPriceController
       │
       ▼ (Axios apiClient)
React Frontend (/market-prices)
```
