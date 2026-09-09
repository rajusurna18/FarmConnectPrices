# Module 16 — Offers & Negotiation Foundation

## 1. Overview & Objective

Module 16 introduces a controlled marketplace commercial negotiation workflow into **FarmConnectPrices** (FarmLink AI). It enables structured price and quantity proposals between:
- **FARMER** (Marketplace Listing Owner)
- **MEDIATOR / BUYER** (Offer Initiator)
- **CUSTOMER** (Offer Initiator)

The negotiation lifecycle supports multiple rounds of counter-offers, formal acceptances, rejections, cancellations, and lazy expiration while maintaining strict separation between:
1. **Farmer Asking Price** (defined on marketplace listing)
2. **Verified Market Price** (from Agmarknet / Market Intelligence modules)
3. **Buyer Offer Price** (entered by buyer/customer)
4. **Counter Offer Price** (entered by responding party)
5. **Final Agreed Price** (created upon proposal ACCEPTANCE)

---

## 2. Architecture & Security Controls

- **Authoritative Identity**: Uses authenticated Firebase UID exclusively from `SecurityContextHolder` / `FirebaseAuthenticationToken`. Client-supplied user UIDs are ignored.
- **Role Enforcement**:
  - `MEDIATOR_BUYER` and `CUSTOMER` can initiate offers against active farmer marketplace listings.
  - `FARMER` can review, accept, reject, or counter offers received for listings they own.
  - Farmers cannot make offers on their own listings.
- **Firestore Quota Guard & Performance**:
  - All negotiation rounds are embedded directly within the root `Offer` document (`rounds: List<OfferRound>`).
  - Fetches offer history in a **single Firestore read**, completely eliminating N+1 read amplification.
  - Integrates `FirestoreQuotaGuard` circuit breaker for quota protection.

---

## 3. Offer Lifecycle State Machine

```
              ┌───────────┐
              │  PENDING  │
              └─────┬─────┘
                    │
   ┌────────────────┼────────────────┬────────────────┐
   ▼                ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌─────────────┐
│  COUNTERED   │ │   ACCEPTED   │ │   REJECTED   │ │  CANCELLED  │
└──────┬───────┘ └──────────────┘ └──────────────┘ └─────────────┘
       │
       ├────────────────┬────────────────┐
       ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  COUNTERED   │ │   ACCEPTED   │ │   REJECTED   │
└──────────────┘ └──────────────┘ └──────────────┘
```

### Valid State Transitions
- `PENDING` → `COUNTERED`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `EXPIRED`
- `COUNTERED` → `COUNTERED`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `EXPIRED`
- Terminal States (`ACCEPTED`, `REJECTED`, `CANCELLED`, `EXPIRED`) are immutable.

### Server-Enforced Limits
- `MAX_NEGOTIATION_ROUNDS = 10`
- Reaching round 10 blocks further counter-offers but allows `ACCEPT` or `REJECT` on the active proposal.
- Default expiration period: 7 days (`DEFAULT_EXPIRATION_DAYS = 7`). Lazy expiration transitions status to `EXPIRED` upon access.

---

## 4. REST API Endpoints (`/api/v1/marketplace/offers`)

| Method | Endpoint | Description | Auth / Role |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/marketplace/offers` | Create offer against active listing | Buyer / Customer |
| `GET` | `/api/v1/marketplace/offers/mine` | Query offers initiated by caller | Buyer / Customer |
| `GET` | `/api/v1/marketplace/offers/received` | Query offers received on owned listings | Farmer |
| `GET` | `/api/v1/marketplace/offers/{offerId}` | Fetch single offer details | Offer Participants |
| `POST` | `/api/v1/marketplace/offers/{offerId}/counter` | Submit counter-offer proposal | Responding Participant |
| `POST` | `/api/v1/marketplace/offers/{offerId}/accept` | Accept active proposal | Responding Participant |
| `POST` | `/api/v1/marketplace/offers/{offerId}/reject` | Reject active proposal | Responding Participant |
| `POST` | `/api/v1/marketplace/offers/{offerId}/cancel` | Cancel pending offer | Initiator Buyer |
| `GET` | `/api/v1/marketplace/offers/{offerId}/history` | Fetch complete negotiation timeline | Offer Participants |

---

## 5. Explicit Out-of-Scope Boundaries (Module 16)

Module 16 strictly terminates at commercial proposal agreement (`ACCEPTED`). The following future features are **NOT** implemented in Module 16:
- ❌ Order Creation
- ❌ Payment Processing / Escrow / Wallet
- ❌ Inventory Stock Reservation / Auto-Deduction
- ❌ Delivery & Logistics
- ❌ Invoicing & Ratings
- ❌ Autonomous AI Negotiation / Pricing
