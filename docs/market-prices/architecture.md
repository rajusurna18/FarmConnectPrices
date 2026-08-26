# Module 07 — Real Market Prices & Price Data Foundation Architecture

## Overview
Module 07 establishes the independent market price data foundation for FarmConnectPrices. Dynamic price observations are completely decoupled from static `markets/{marketId}` and `crops/{cropId}` documents into a dedicated collection: `marketPrices/{priceId}`.

```
React UI  ──(Axios apiClient)──>  Spring Boot REST API  ──(Firebase Admin SDK)──>  Cloud Firestore
```

## Architectural Principles
1. **Decoupled Data Layer**:
   - `Market` defines physical mandi metadata and location.
   - `Crop` defines commodity classification and taxonomy.
   - `MarketPrice` captures price observations at a specific business date (`priceDate`), observation timestamp (`observedAt`), unit (`QUINTAL`), currency (`INR`), source, and quality status.
2. **Backend-Mediated Access Only**:
   - Direct client-side Firestore reads and writes are forbidden (`allow read, write: if false;`).
   - The React frontend fetches price data exclusively via Spring Boot REST controllers authenticated using `Authorization: Bearer <Firebase ID Token>`.
3. **Data Quality & Verification Rules**:
   - Enforces `minPrice <= modalPrice <= maxPrice` and non-negative pricing.
   - Quality states: `VERIFIED`, `UNVERIFIED`, `REJECTED`. `REJECTED` records are strictly excluded from standard user views.
4. **Deterministic Ordering**:
   - Price listings and historical queries sort deterministically by `priceDate` (descending) and `observedAt` (descending).
5. **Bounded Pagination**:
   - Result windows are bounded (`limit` default 50, max 100).
