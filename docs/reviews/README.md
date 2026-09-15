# Module 20 — Ratings, Reviews & Trust Foundation

## Overview
Module 20 implements the **Ratings, Reviews & Trust Foundation** for the FarmConnectPrices / FarmLink AI marketplace. 

The core invariant of Module 20 is:
**Every review must originate from a real, completed marketplace transaction (`OrderStatus.COMPLETED`).** Users cannot rate or review arbitrary users without a qualifying commercial order.

---

## Key Rules & Invariants

1. **Transaction Qualification**:
   - Reviews are permitted **only** when `order.status == "COMPLETED"`.
   - Orders in `PENDING`, `CONFIRMED`, `PROCESSING`, or `CANCELLED` states are ineligible.
2. **Authoritative Counterparty Derivation**:
   - Reviewer identity is always derived from the authenticated Firebase token (`callerUid`).
   - Reviewee identity is strictly derived by the backend from the authoritative `Order` document:
     - Buyer reviews Farmer (`revieweeUid = order.farmerId`, `revieweeRole = "FARMER"`).
     - Farmer reviews Buyer (`revieweeUid = order.buyerUid`, `revieweeRole = order.buyerRole`).
   - API callers cannot specify or override the `revieweeUid` or `revieweeRole`.
3. **Uniqueness**:
   - 1 Reviewer + 1 Order + 1 Reviewee = 1 Review (up to 2 independent reviews per order, 1 from each counterparty).
   - Deterministic Firestore Document ID: `review_{orderId}_{reviewerUid}`.
   - Creation uses an atomic Firestore transaction that fails with `ALREADY_REVIEWED` if a duplicate review attempt occurs.
4. **Rating Aggregation**:
   - Persisted in collection `ratingSummaries/{userId}`.
   - Updated atomically in the same Firestore transaction as review creation.
   - Fields: `totalReviews`, `totalRatingPoints`, `averageRating` (scale 2 `HALF_UP`), and star distribution counts (`oneStarCount` through `fiveStarCount`).
5. **Security & Privacy**:
   - Direct client writes to `reviews` and `ratingSummaries` are blocked in `firestore.rules` (`allow write: if false;`).
   - All writes are executed exclusively by the Spring Boot backend via the Admin SDK.
   - Only safe public display names and roles are exposed. Private contact details (email, phone, address) are never included.
6. **Data Store Policy**:
   - **Production**: Firestore is strictly authoritative. No in-memory persistence fallback is permitted in production runtime.
   - **Tests**: In-memory fallback is used ONLY where required by isolated unit tests when the `firestore` bean is null.

---

## Firestore Collections Schema

### 1. Collection `reviews`
Document ID format: `review_{orderId}_{reviewerUid}`

```json
{
  "reviewId": "review_order123_buyer456",
  "orderId": "order123",
  "listingId": "listing789",
  "cropId": "crop_paddy",
  "cropName": "Paddy / Rice",
  "reviewerUid": "buyer456",
  "reviewerRole": "CUSTOMER",
  "reviewerDisplayName": "Ramesh Kumar",
  "revieweeUid": "farmer100",
  "revieweeRole": "FARMER",
  "revieweeDisplayName": "Suresh Patel",
  "rating": 5,
  "title": "Excellent Paddy Quality",
  "comment": "The produce was fresh and delivered on time.",
  "status": "PUBLISHED",
  "createdAt": "2026-09-15T22:00:00Z",
  "updatedAt": "2026-09-15T22:00:00Z"
}
```

### 2. Collection `ratingSummaries`
Document ID format: `{userId}`

```json
{
  "userId": "farmer100",
  "userRole": "FARMER",
  "averageRating": "4.80",
  "totalReviews": 10,
  "totalRatingPoints": 48,
  "oneStarCount": 0,
  "twoStarCount": 0,
  "threeStarCount": 1,
  "fourStarCount": 1,
  "fiveStarCount": 8,
  "updatedAt": "2026-09-15T22:00:00Z"
}
```

---

## REST API Endpoints

- `POST /api/v1/marketplace/reviews` — Submit review for completed order.
- `GET /api/v1/marketplace/reviews/eligibility/{orderId}` — Check eligibility to review an order.
- `GET /api/v1/marketplace/reviews/mine` — Get reviews submitted by caller.
- `GET /api/v1/marketplace/users/{userId}/reviews` — Get public reviews received by a user.
- `GET /api/v1/marketplace/orders/{orderId}/reviews` — Get reviews for an order (order participants).
- `GET /api/v1/marketplace/users/{userId}/rating-summary` — Get public rating summary for a user.

---

## Verification & Testing
- Backend Unit Tests: `ReviewServiceTest` and `ReviewControllerTest`.
- Frontend Types & React Query Hooks: `src/types/review.ts`, `src/features/marketplace/api/marketplaceReviewsApi.ts`.
- UI Components: `ReviewForm`, `RatingSummaryCard`, `ReviewCard`, `ReviewList`.
- Integrated into `OrderDetailPage.tsx` and `ProfilePage.tsx`.
