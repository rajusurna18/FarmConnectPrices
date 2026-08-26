# Real Mandi Data Source Architecture

## Overview
FarmConnectPrices integrates with the official Government of India Open Government Data Platform (`data.gov.in`) dataset:
- **Title**: Variety-wise Daily Market Prices Data of Commodity
- **Resource ID**: `35985678-0d79-46b4-9ed6-6f13308a1d24`
- **Source Agency**: AGMARKNET (Directorate of Marketing & Inspection)

## Real API Record Model Fields
Inspection of resource `35985678-0d79-46b4-9ed6-6f13308a1d24` returns the following fields:
- `State`
- `District`
- `Market`
- `Commodity` / `Commodity_Code`
- `Variety`
- `Grade`
- `Arrival_Date`
- `Min_Price`
- `Max_Price`
- `Modal_Price`

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
data.gov.in (AGMARKNET Resource 35985678-0d79-46b4-9ed6-6f13308a1d24)
       │
       ▼ (HTTPS / Secure Backend Client)
Spring Boot DataGovIngestionService
       │
       ▼ (Validation & Deduplication)
Cloud Firestore (/marketPrices/{priceId})
       │
       ▼ (REST API / Firebase Admin SDK)
Spring Boot MarketPriceController & LocationController
       │
       ▼ (Axios apiClient / TanStack Query)
React Frontend (/market-prices & /markets)
```
