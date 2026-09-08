# 🌾 FarmConnectPrices — Module 15: Marketplace Product Catalog & Listing Foundation

## Overview
Module 15 establishes the foundational agricultural commerce catalog for **FarmConnectPrices / FarmLink AI**. It enables farmers to list harvested produce with asking prices, quantities, availability, and regional location details while allowing buyers and customers to discover and view produce offerings across markets.

---

## Key Principles & Architectural Concepts

### 1. Product/Crop Master vs. Marketplace Listing
- **Crop Master Data**: Reused directly from Modules 01–05 (`CropMasterService`, `crops` Firestore collection). No duplicate crop master entities are created.
- **Marketplace Listing**: Represents a specific farmer's produce offering (`productListings` Firestore collection) referencing the authoritative crop ID.

### 2. Farmer Asking Price vs. Verified Market Price
- `askingPrice`: Farmer-entered seller asking price.
- `verifiedMarketPrice`: Read-only market intelligence from Module 07 (`MarketPriceService`).
- Asking price remains strictly seller-controlled and is **never** auto-filled or overwritten by market price data.

### 3. Seller Ownership & Security
- Seller identity (`ownerUid`) is resolved server-side from `FirebaseAuthenticationToken`.
- Front-end payloads supplying arbitrary `farmerId` or `ownerUid` are ignored.
- Only authenticated users with the `FARMER` profile role can create, update, pause, or delete listings.

### 4. Visibility & Privacy Protection
- Public browse (`/api/v1/marketplace/listings`) returns ONLY `ACTIVE` listings.
- `DRAFT`, `PAUSED`, and `EXPIRED` listings remain strictly private to the owner.
- Private addresses, precise GPS coordinates, phone numbers, and emails are never exposed on public marketplace endpoints.

---

## Documentation Index
- [`architecture.md`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/docs/marketplace/architecture.md) — System architecture, data flow, component integration.
- [`listing-model.md`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/docs/marketplace/listing-model.md) — Data schema, fields, data types, quality grades.
- [`listing-lifecycle.md`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/docs/marketplace/listing-lifecycle.md) — Status transitions (`DRAFT`, `ACTIVE`, `PAUSED`, `SOLD_OUT`, `EXPIRED`).
- [`api.md`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/docs/marketplace/api.md) — REST API endpoint specification.
- [`security.md`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/docs/marketplace/security.md) — Role access matrix, Firestore security rules, server authorization.
- [`search-pagination.md`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/docs/marketplace/search-pagination.md) — Bounded search, query filters, pagination, index setup.
- [`limitations.md`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/docs/marketplace/limitations.md) — Scope boundary and out-of-scope capabilities.
