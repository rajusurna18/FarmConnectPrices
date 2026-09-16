# Module 21 — Notifications & Communication Foundation

## Overview
Module 21 provides a robust, decoupled, and quota-conscious **in-app notification foundation** for **FarmConnectPrices**. It ensures that critical marketplace actions across **Offers (Module 16)**, **Orders (Module 17)**, **Payments (Module 18)**, **Delivery (Module 19)**, and **Reviews (Module 20)** generate persistent, authorized in-app notifications for the appropriate recipient users.

---

## Core Principles & Security
1. **Derived Communication Record**: Notifications serve strictly as derived communication records and are **never** the source of truth for marketplace state.
2. **Backend-Controlled Writes**: All notification creation and mutation is mediated through backend Spring Boot services via the Firebase Admin SDK. Direct client writes to Firestore are blocked via `firestore.rules`.
3. **Strict Identity & Ownership Authorization**: Recipient identity is derived from authenticated domain context on the backend (`recipientUid = request.auth.uid`). Users can only query, read, or delete notifications where `recipientUid == authenticatedUid`.
4. **Idempotency & Duplicate Protection**: Supports `sourceEventId` on notifications to prevent repeated events or retries from creating duplicate notification entries.
5. **Quota Safety**: Integrated with `FirestoreQuotaGuard` and bounded queries to protect against Firestore quota exhaustion.

---

## Data Model

### Collection Structure
`notifications/{notificationId}`

### Schema
```json
{
  "notificationId": "uuid-string",
  "recipientUid": "firebase-user-uid",
  "recipientRole": "FARMER | MEDIATOR_BUYER | CUSTOMER",
  "type": "NEW_OFFER",
  "title": "New Offer Received",
  "message": "You received a new offer of ₹2,500 for 100 kg Tomatoes.",
  "read": false,
  "relatedEntityType": "OFFER",
  "relatedEntityId": "offer123",
  "relatedRoute": "/marketplace/offers/offer123",
  "metadata": {
    "listingId": "listing123"
  },
  "sourceEventId": "event_offer_new_offer123",
  "createdAt": "2026-09-16T10:00:00Z",
  "readAt": null
}
```

---

## Notification Types & Triggers

| Type | Recipient Role | Triggering Marketplace Event | Navigation Route |
| :--- | :--- | :--- | :--- |
| `NEW_OFFER` | FARMER | Buyer/Customer places an offer on listing | `/marketplace/offers/{offerId}` |
| `COUNTER_OFFER_RECEIVED` | FARMER / BUYER | Counter-offer submitted in negotiation round | `/marketplace/offers/{offerId}` |
| `OFFER_ACCEPTED` | BUYER / CUSTOMER | Farmer accepts offer | `/marketplace/offers/{offerId}` |
| `OFFER_REJECTED` | BUYER / CUSTOMER | Farmer declines offer | `/marketplace/offers/{offerId}` |
| `OFFER_CANCELLED` | FARMER | Buyer cancels pending/countered offer | `/marketplace/offers/{offerId}` |
| `ORDER_CREATED` | FARMER | Order generated from accepted offer or direct purchase | `/marketplace/orders/{orderId}` |
| `ORDER_CONFIRMED` | BUYER / CUSTOMER | Farmer confirms order | `/marketplace/orders/{orderId}` |
| `ORDER_PROCESSING` | BUYER / CUSTOMER | Order moves to processing state | `/marketplace/orders/{orderId}` |
| `ORDER_COMPLETED` | BUYER / CUSTOMER | Order marked as completed | `/marketplace/orders/{orderId}` |
| `ORDER_CANCELLED` | FARMER / BUYER | Order cancelled by participant | `/marketplace/orders/{orderId}` |
| `PAYMENT_SUCCESS` | BUYER / CUSTOMER | Payment intent verified as successful | `/marketplace/payments/{paymentId}` |
| `PAYMENT_FAILED` | BUYER / CUSTOMER | Payment verification failed | `/marketplace/payments/{paymentId}` |
| `DELIVERY_ASSIGNED` | BUYER / CUSTOMER | Delivery record created for order | `/marketplace/deliveries/{deliveryId}` |
| `DELIVERY_READY_FOR_PICKUP` | BUYER / CUSTOMER | Seller/partner marks delivery ready for pickup | `/marketplace/deliveries/{deliveryId}` |
| `DELIVERY_PICKED_UP` | BUYER / CUSTOMER | Delivery picked up from location | `/marketplace/deliveries/{deliveryId}` |
| `DELIVERY_IN_TRANSIT` | BUYER / CUSTOMER | Delivery package in transit | `/marketplace/deliveries/{deliveryId}` |
| `DELIVERY_OUT_FOR_DELIVERY` | BUYER / CUSTOMER | Package out for final delivery | `/marketplace/deliveries/{deliveryId}` |
| `DELIVERY_DELIVERED` | BUYER / CUSTOMER | Package successfully delivered | `/marketplace/deliveries/{deliveryId}` |
| `NEW_REVIEW` | FARMER / BUYER | Review submitted by counterparty | `/profile` |

---

## API Endpoints (`/api/v1/notifications`)

- `GET /api/v1/notifications` — Paginated notifications for authenticated user (`unreadOnly`, `page`, `pageSize`).
- `GET /api/v1/notifications/unread` — Unread notifications for authenticated user.
- `GET /api/v1/notifications/unread-count` — Returns `{"count": N}` for authenticated user.
- `GET /api/v1/notifications/{notificationId}` — Retrieve single notification (recipient owner check enforced).
- `PATCH /api/v1/notifications/{notificationId}/read` — Mark single notification as read.
- `PATCH /api/v1/notifications/read-all` — Mark all notifications for authenticated user as read.
- `DELETE /api/v1/notifications/{notificationId}` — Delete single notification (recipient owner check enforced).

---

## Known Scope Limitations
As defined in Module 21 specifications, the following external communication channels and features are **explicitly out of scope**:
- WhatsApp / SMS integration
- Email notification delivery
- Push notifications / FCM
- WebSockets / Real-time messaging
- AI prioritization or notification scheduling
- Autonomous communication providers
