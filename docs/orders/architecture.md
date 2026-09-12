# Module 17 — Order Domain Architecture & Data Model

## Overview
Module 17 introduces the Order domain layer to the FarmConnectPrices marketplace. An order is created exclusively from an `ACCEPTED` negotiation proposal (Module 16), creating a binding commercial record, allocating available quantity from the target listing (Module 15), and enforcing a server-side state machine.

---

## Order Lifecycle State Machine

```
              ┌───────────────┐
              │    PENDING    │
              └───────┬───────┘
                      │
            (Farmer confirms order)
                      ▼
              ┌───────────────┐
              │   CONFIRMED   │
              └───────┬───────┘
                      │
            (Farmer processes order)
                      ▼
              ┌───────────────┐
              │  PROCESSING   │
              └───────┬───────┘
                      │
            (Farmer completes order)
                      ▼
              ┌───────────────┐
              │   COMPLETED   │ [Terminal State]
              └───────────────┘
```

### Cancellation Matrix

| Current State | Initiator | Target State | Inventory Restored? |
| :--- | :--- | :--- | :--- |
| `PENDING` | Buyer / Farmer | `CANCELLED` | Yes (Atomically restores `order.totalQuantity`) |
| `CONFIRMED` | Buyer / Farmer | `CANCELLED` | Yes (Atomically restores `order.totalQuantity`) |
| `PROCESSING` | N/A | Forbidden | No |
| `COMPLETED` | N/A | Forbidden | No |
| `CANCELLED` | N/A | Immutable | No (Prevented double restoration) |

---

## Data Model & Firestore Persistence

### Order Document (`orders/{orderId}`)
- `orderId`: String (`order_<uuid>`)
- `offerId`: String (reference to accepted offer)
- `listingId`: String (reference to product listing)
- `farmerId`: String (seller farmer UID)
- `buyerUid`: String (buyer UID)
- `buyerRole`: String (`MEDIATOR_BUYER` or `CUSTOMER`)
- `cropId`: String
- `cropName`: String
- `status`: String (`PENDING`, `CONFIRMED`, `PROCESSING`, `COMPLETED`, `CANCELLED`)
- `totalQuantity`: Double
- `quantityUnit`: String
- `agreedPrice`: BigDecimal
- `priceUnit`: String
- `subtotal`: BigDecimal (`agreedPrice * totalQuantity`)
- `shippingCost`: BigDecimal (`0.00`)
- `otherCost`: BigDecimal (`0.00`)
- `totalAmount`: BigDecimal
- `currency`: String (`"INR"`)
- `item`: Embedded `OrderItem` object
- `createdAt`, `updatedAt`, `confirmedAt`, `processedAt`, `completedAt`, `cancelledAt`

### Embedded OrderItem Object
- `orderItemId`: String
- `orderId`: String
- `listingId`: String
- `cropId`: String
- `cropName`: String
- `quantity`: Double
- `quantityUnit`: String
- `agreedUnitPrice`: BigDecimal
- `priceUnit`: String
- `lineTotal`: BigDecimal

---

## Duplicate Order Protection Strategy
1. **Firestore Transaction Check**: Order creation performs a transactional query `orders.whereEqualTo("offerId", offerId)`. If an order document already exists, creation is aborted with HTTP 409 Conflict (`ORDER_ALREADY_EXISTS`).
2. **UI Double-Click Prevention**: The frontend button disables upon mutation invocation.

---

## REST APIs
- `POST /api/v1/marketplace/orders/from-offer/{offerId}` (Create order from accepted offer)
- `GET /api/v1/marketplace/orders/mine` (Buyer order history)
- `GET /api/v1/marketplace/orders/received` (Farmer received orders)
- `GET /api/v1/marketplace/orders/{orderId}` (Order detail)
- `POST /api/v1/marketplace/orders/{orderId}/confirm` (Farmer confirm)
- `POST /api/v1/marketplace/orders/{orderId}/process` (Farmer process)
- `POST /api/v1/marketplace/orders/{orderId}/complete` (Farmer complete)
- `POST /api/v1/marketplace/orders/{orderId}/cancel` (Participant cancel & restore stock)

---

## Security & Quota Safety
- **Firestore Security Rules**: Direct client writes are blocked (`allow write: if false;`). Reads restricted to order participants (`buyerUid` or `farmerId`).
- **Quota Guard**: Uses `FirestoreQuotaGuard` circuit breaker state (`CLOSED`, `OPEN`, `HALF_OPEN`) returning HTTP 503 on quota exhaustion.
