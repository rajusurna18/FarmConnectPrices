# FarmConnectPrices — Planned Firestore Collections & Data Design Principles

This document outlines the planned future collection structure and core data design principles for Cloud Firestore in FarmConnectPrices.

> [!NOTE]
> Module 02 establishes infrastructure only. The collections below are architectural blueprints for future modules. No business data or sample records are created in this module.

## Core Data Design Principles

1. **Stable Document IDs:** Use deterministic, predictable document IDs where appropriate (e.g., matching Firebase Auth `uid` for `users`, `farmerProfiles`, `buyerProfiles`).
2. **Server Timestamps:** Always use Firestore server timestamps (`FieldValue.serverTimestamp()`) for `createdAt` and `updatedAt` instead of client-generated ISO strings.
3. **Small Document Sizes:** Keep document size small (< 1 MB limit) by avoiding storing binary media or massive text blocks directly in Firestore.
4. **Bounded Arrays:** Avoid unbounded arrays inside documents. Use subcollections or reference arrays with fixed limits.
5. **Entity References:** Store reference IDs (e.g., `marketId`, `farmerId`) rather than deeply duplicating large mutable documents.
6. **Shallow Collections:** Keep collection nesting shallow (maximum 1 subcollection layer where needed).
7. **Query-First Design:** Structure collection schema based on expected access patterns to minimize complex composite index requirements.
8. **Decouple Dynamic Data:** Separate rapidly changing fields (e.g. daily `marketPrices`) from static metadata (e.g. `markets` profile).

---

## Planned Future Collections Blueprint

| Collection Name | Description | Key Reference / Relationship |
| :--- | :--- | :--- |
| `users` | Base user identity, account status, and role metadata | Auth UID |
| `farmerProfiles` | Farmer-specific details, location, and certification info | `userId` |
| `buyerProfiles` | Buyer business details, purchasing criteria | `userId` |
| `farms` | Farm land parcels, locations, and boundaries | `farmerId` |
| `crops` | Crop catalog and seasonal metadata | Standalone / Reference |
| `marketPrices` | Daily/real-time agricultural market commodity prices | `marketId`, `cropId` |
| `markets` | Physical & digital market locations, operating hours | Standalone |
| `listings` | Farmer crop listings available for purchase | `farmerId`, `cropId` |
| `inventory` | Farmer available stock levels | `farmerId`, `cropId` |
| `offers` | Buyer bids or purchase offers | `listingId`, `buyerId` |
| `orders` | Confirmed trade contracts and purchase orders | `listingId`, `buyerId`, `farmerId` |
| `payments` | Payment transaction records and statuses | `orderId` |
| `deliveries` | Logistics, dispatch, and delivery tracking | `orderId` |
| `notifications` | User alerts, order updates, and price drop notifications | `userId` |
| `aiPredictions` | Machine learning price trends and yield forecasts | `cropId`, `marketId` |
| `auditLogs` | System administrative and security activity logs | System-wide |
