# Buyer Opportunity Matching Foundation — Module 23

## 1. Purpose & Objective

Module 23 introduces the **Buyer Opportunity Matching Foundation** for FarmConnectPrices. It provides deterministic, decision-support buyer matching for active farmer produce listings based strictly on verified historical marketplace activity (Offers, Listings, and Profiles).

> **Notice**: Buyer opportunities are deterministic decision-support results based on verified application data. They do NOT represent guaranteed buyer demand, purchase intent, or purchase probability.

---

## 2. Primary User Profiles

FarmConnectPrices maintains three primary profiles:
1. `FARMER`: Listing owner seeking compatible buyer demand opportunities.
2. `MEDIATOR_BUYER` (displayed as "Mediator / Buyer"): Registered mediator/commercial buyer.
3. `CUSTOMER` (displayed as "Customer"): Registered end buyer/consumer.

---

## 3. Architecture & Data Flow

```text
ProductListing (ACTIVE)
        │
        ├── Crop ID / Crop Name
        ├── Available Quantity & Unit
        ├── Quality Grade
        └── Location (District, State, Mandal)
        │
        ▼
BuyerOpportunityMatchingEngine
        ▲
        │
Verified Buyer Demand Records (Offers & Profiles)
        ├── Buyer Role (MEDIATOR_BUYER / CUSTOMER)
        ├── Requested / Offered Crop & Quantity
        └── Buyer Anonymized Location (District, State)
        │
        ▼
BuyerOpportunityDto
        ├── Relevance Score (0% – 100%)
        ├── Confidence Level (HIGH / MEDIUM / LOW / INSUFFICIENT_DATA)
        ├── Structured Match Reason Codes
        └── Anonymized Buyer Display
```

---

## 4. Scoring Formula

Relevance score calculation is 100% deterministic using a weighted multi-dimensional formula:

$$\text{Relevance Score} = \text{CropMatch} (40\%) + \text{QuantityMatch} (25\%) + \text{LocationMatch} (20\%) + \text{QualityMatch} (15\%)$$

### Weight Distribution
1. **Crop Compatibility (40%)**:
   - **40%**: Exact match on `cropId` or `cropName` (case-insensitive).
   - **0%**: Mismatch or unknown crop.

2. **Quantity Compatibility (25%)**:
   - **25%**: Demanded quantity is between 80% and 150% of listing `availableQuantity`.
   - **15%**: Partial quantity match (>= 30% and < 80% of listing `availableQuantity`).
   - **0%**: Out of optimal range (< 30% or > 150%) or unknown/incompatible quantity.

3. **Location Compatibility (20%)**:
   - **20%**: Same canonical market (`SAME_MARKET`).
   - **15%**: Same district (`SAME_DISTRICT`).
   - **10%**: Same state (`SAME_STATE`).
   - **0%**: Different state / unknown (`UNKNOWN_LOCATION`).

4. **Quality Grade Compatibility (15%)**:
   - **15%**: Quality grade specified (`QUALITY_MATCH`).
   - **0%**: Unspecified grade / unknown (`UNKNOWN_QUALITY`).

---

## 5. Confidence Classification

Confidence is classified deterministically based strictly on the completeness and reliability of current verified inputs:
- `HIGH`: Relevance Score $\ge 75\%$ WITH verified Crop Match, Quantity Match, Location Match (Market/District), AND Quality Match.
- `MEDIUM`: Relevance Score $\ge 50\%$ WITH verified Crop Match and at least one secondary dimension.
- `LOW`: Relevance Score $\ge 40\%$ WITH verified Crop Match.
- `INSUFFICIENT_DATA`: Missing crop match or zero verified matching inputs.

---

## 5.1 Deterministic Ranking Order

Matching opportunities are ordered using a strict 5-tier deterministic comparison sequence:
1. `relevanceScore` DESC
2. `quantity compatibility rank` (QUANTITY_MATCH > PARTIAL_QUANTITY > UNKNOWN_QUANTITY) DESC
3. `location compatibility rank` (SAME_MARKET > SAME_DISTRICT > SAME_STATE > UNKNOWN_LOCATION) DESC
4. `quality compatibility rank` (QUALITY_MATCH > UNKNOWN_QUALITY) DESC
5. `opportunityId` ASC (stable string tie-breaker)

---

## 6. Structured Reason Codes

- `CROP_MATCH`: Exact crop compatibility.
- `QUANTITY_MATCH`: Requested quantity aligns with available produce.
- `PARTIAL_QUANTITY`: Listing quantity satisfies a portion of buyer demand.
- `QUALITY_MATCH`: Produce quality grade matches buyer requirement.
- `SAME_MARKET`: Buyer operates in the local market center.
- `SAME_DISTRICT`: Buyer operates in the same district.
- `SAME_STATE`: Buyer operates in the same state.
- `UNKNOWN_QUALITY`: Standard/unspecified produce quality grade.

---

## 7. Privacy & Security Rules

- **No Data Leakage**: Private buyer information (phone numbers, email addresses, exact street addresses, authentication tokens, payment history) is **never exposed**.
- **Anonymized Display**: Opportunities show only buyer role ("Mediator / Buyer" or "Customer") and broad region ("Rangareddy, Telangana").
- **Strict Authorization**: Requests require authenticated `FARMER` role and listing ownership (`listing.ownerUid == callerUid`).
- **Listing Status Validation**: Opportunities are generated ONLY for `ACTIVE` listings. Inactive listings (`DRAFT`, `PAUSED`, `SOLD_OUT`, `EXPIRED`) return HTTP 400 Bad Request.

---

## 8. REST API Specifications

### Get Buyer Opportunities
`GET /api/v1/marketplace/buyer-opportunities`

#### Query Parameters
- `listingId` (string, required): Active listing ID.
- `page` (int, optional, default 0): Zero-based page index.
- `size` (int, optional, default 10, max 50): Page size limit.

#### Example Response (`200 OK`)
```json
{
  "items": [
    {
      "opportunityId": "opp_101",
      "buyerRole": "MEDIATOR_BUYER",
      "buyerRoleDisplay": "Mediator / Buyer",
      "anonymizedLocation": "Rangareddy, Telangana",
      "cropId": "crop_tomato",
      "cropName": "Tomato",
      "typicalQuantity": 500.0,
      "quantityUnit": "KG",
      "relevanceScore": 95.00,
      "confidenceLevel": "HIGH",
      "matchReasons": [
        "CROP_MATCH",
        "QUANTITY_MATCH",
        "SAME_DISTRICT",
        "QUALITY_MATCH"
      ],
      "lastActiveAt": "2026-09-18T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "statusState": "SUCCESS"
}
```

---

## 9. Quota Safety & Error Handling

- Integrated with `FirestoreQuotaGuard` for read operations.
- Infrastructure or quota failures return HTTP 503 (`SERVICE_UNAVAILABLE`) or explicit error states, **NEVER** false `NO_MATCHES`.
- `NO_MATCHES` state is returned only when zero verified buyer demand records match the listing.

---

## 10. Explicit Limitations

Out of scope for Module 23:
- Automatic buyer contact or direct messaging
- Purchase intent prediction or AI probability modeling
- Autonomous offer or order generation
- Payment, delivery, or escrow modifications
- External buyer web scraping or fake buyer data generation
