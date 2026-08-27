# Real Mandi Data Source Architecture

## Overview
FarmConnectPrices Version 1 integrates exclusively with official Government of India Open Government Data Platform (`data.gov.in`) datasets. No third-party connectors (Farmer.in, MandiAPI) are used.

- **Historical / Controlled Backfill Resource ID**: `35985678-0d79-46b4-9ed6-6f13308a1d24` ("Variety-wise Daily Market Prices Data of Commodity", 81.4M historical records).
- **Daily Bulletin / Incremental Sync Resource ID**: `9ef84268-d588-465a-a308-a864a43d0070` ("Current Daily Price of Various Commodities AGMARKNET", ~12.8K daily active snapshot records).
- **Source Agency**: AGMARKNET (Directorate of Marketing & Inspection, Ministry of Agriculture & Farmers Welfare).

## Real API Record Model Fields
Inspection of AGMARKNET resources returns the following standard fields:
- `state` / `State`
- `district` / `District`
- `market` / `Market`
- `commodity` / `Commodity`
- `commodity_code` / `Commodity_Code`
- `variety` / `Variety`
- `grade` / `Grade`
- `arrival_date` / `Arrival_Date`
- `min_price` / `Min_Price`
- `max_price` / `Max_Price`
- `modal_price` / `Modal_Price`

## Conditional Mandal/Area Rule
The external dataset returns `State`, `District`, and `Market`, but **no** native Mandal / Sub-District field.
- Dynamic Hierarchy: **State $\rightarrow$ District $\rightarrow$ [Optional Mandal/Area] $\rightarrow$ Market $\rightarrow$ Crop**.
- Mandal/Area options are rendered **only** when sub-district/area metadata exists for selected markets (e.g. from market master properties) or when supported by future LGD administrative mappings.
- **No mandals or markets are fabricated or inferred from string guessing.**

## Security Architecture
1. **Secret Storage**: The `DATA_GOV_IN_API_KEY` exists exclusively in the Spring Boot backend environment (`backend/.env` or system environment variables).
2. **Zero Client Exposure**: The API key is **never** embedded in React frontend code, frontend `.env`, browser localStorage/sessionStorage, Git tracking, or application logs.
3. **Mediated Access**: The frontend queries Spring Boot (`/api/v1/market-prices`), which reads from Firestore. The external API is an ingestion pipeline source, not a client proxy.

## Data Flow
```
AGMARKNET (Resource 9ef84268... Incremental / 35985678... Backfill)
       │
       ▼ (HTTPS / Secure RestClient with Timeouts & Retries)
Spring Boot DataGovIngestionService (Paginated Engine)
       │
       ▼ (Raw Source Provenance, Normalization, Discovery, Validation & Idempotent Upsert)
Cloud Firestore (/marketPrices/{priceId})
       │
       ▼ (REST API / Spring Boot Controllers)
Spring Boot MarketPriceController & LocationController
       │
       ▼ (Axios apiClient / React)
React Frontend (/market-prices & /markets)
```

