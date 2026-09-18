# Module 22 — Notification Preferences & Communication Controls

## 1. Overview

Module 22 introduces user-controlled **In-App Notification Preferences** for FarmConnectPrices. It empowers authenticated users across all three primary ecosystem roles (`FARMER`, `MEDIATOR_BUYER`, `CUSTOMER`) to control which event categories generate in-app notifications in the future, without modifying past notification history or interrupting core marketplace business transactions.

---

## 2. Preference Categories

Notification preferences are organized into 5 controlled enum categories matching Module 21 notification events:

| Preference Category | Enum Value | Description |
| :--- | :--- | :--- |
| **Offers & Negotiation** | `OFFERS` | Notifications for new offers, counter-offers, acceptance, and rejections |
| **Orders & Fulfillment** | `ORDERS` | Notifications for order creation, confirmation, processing, and completion |
| **Payments & Transactions** | `PAYMENTS` | Notifications for payment creation, success, or transaction failure |
| **Delivery & Logistics** | `DELIVERY` | Notifications for produce pickup, transit status, and delivery completion |
| **Reviews & Trust Ratings** | `REVIEWS` | Notifications when commercial trading partners submit verified reviews |

---

## 3. Event Mapping

Module 21 notification types map deterministically to preference categories:

```
NotificationType                     Preference Category
─────────────                        ───────────────────
NEW_OFFER ─────────────────────────┐
COUNTER_OFFER_RECEIVED ────────────┼─► OFFERS
OFFER_ACCEPTED ────────────────────┤
OFFER_REJECTED ────────────────────┤
OFFER_CANCELLED ───────────────────┘

ORDER_CREATED ─────────────────────┐
ORDER_CONFIRMED ───────────────────┼─► ORDERS
ORDER_PROCESSING ──────────────────┤
ORDER_CANCELLED ───────────────────┤
ORDER_COMPLETED ───────────────────┘

PAYMENT_CREATED ───────────────────┐
PAYMENT_SUCCESS ───────────────────┼─► PAYMENTS
PAYMENT_FAILED ────────────────────┘

DELIVERY_ASSIGNED ─────────────────┐
DELIVERY_READY_FOR_PICKUP ─────────┤
DELIVERY_PICKED_UP ────────────────┼─► DELIVERY
DELIVERY_IN_TRANSIT ───────────────┤
DELIVERY_OUT_FOR_DELIVERY ─────────┤
DELIVERY_DELIVERED ────────────────┤
DELIVERY_CANCELLED ────────────────┘

NEW_REVIEW ──────────────────────────► REVIEWS
```

---

## 4. Default Preferences

All new users default to **all categories enabled (`true`)**:

```json
{
  "offers": true,
  "orders": true,
  "payments": true,
  "delivery": true,
  "reviews": true
}
```

- If no Firestore preference document exists for a user, default preferences are returned without triggering unnecessary database writes on GET operations.

---

## 5. Firestore Document Structure

Preferences are stored in the `notificationPreferences` Firestore collection:

- **Path**: `notificationPreferences/{uid}`
- **Fields**:
  - `uid` (String): Authenticated Firebase User ID
  - `offers` (Boolean): Enable/disable offer notifications
  - `orders` (Boolean): Enable/disable order notifications
  - `payments` (Boolean): Enable/disable payment notifications
  - `delivery` (Boolean): Enable/disable delivery notifications
  - `reviews` (Boolean): Enable/disable review notifications
  - `createdAt` (String ISO-8601): Document creation timestamp
  - `updatedAt` (String ISO-8601): Document update timestamp

---

## 6. Security & Firestore Rules

- Ownership is strictly enforced using `request.auth.uid == userId`.
- **Firestore Rule**:
```firestore-rules
match /notificationPreferences/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```
- Server-side REST endpoints derive ownership exclusively from `FirebaseAuthenticationToken.getUid()`.

---

## 7. Performance, Caching & Quota Safety

- **In-Memory Cache**: `NotificationPreferenceService` maintains a thread-safe `ConcurrentHashMap` cache to avoid executing Firestore document reads on every marketplace event.
- **Cache Invalidation**: When a user saves new preferences via `PUT /api/v1/notification-preferences`, the cache is immediately updated with new selections.
- **Quota Protection**: Integrates with `FirestoreQuotaGuard` to prevent quota exhaustion and stampedes.

---

## 8. Failure Isolation

- Preference lookup failure **NEVER** interrupts authoritative marketplace operations.
- If Firestore or preference lookup fails during notification creation, the system safely falls back to `enabled = true` and logs a warning.
- Core business operations (e.g., accepting an offer, completing an order, releasing payment) remain authoritative and uninterrupted.

---

## 9. API Contracts

### Get Notification Preferences
`GET /api/v1/notification-preferences`

- **Auth Required**: Yes
- **Response `200 OK`**:
```json
{
  "offers": true,
  "orders": true,
  "payments": true,
  "delivery": false,
  "reviews": true
}
```

### Update Notification Preferences
`PUT /api/v1/notification-preferences`

- **Auth Required**: Yes
- **Request Body**:
```json
{
  "offers": true,
  "orders": true,
  "payments": true,
  "delivery": false,
  "reviews": true
}
```
- **Response `200 OK`**: Updated preference object.

---

## 10. Frontend Experience

- Accessible at `/notification-preferences`.
- Integrated into `NotificationCenterPage` header actions and `ProfilePage`.
- Dynamic dirty state detection disables `Save Changes` button until options are changed.
- `Reset to Defaults` resets local switches to `true`.
- Works identically across `FARMER`, `MEDIATOR_BUYER`, and `CUSTOMER` profiles.

---

## 11. Limitations

Module 22 is explicitly focused on **in-app notification controls**. The following are out of scope:
- Email / SMS / WhatsApp / Push notification controls
- Marketing campaign preferences
- Digest scheduling or quiet hours
- Complex per-listing or per-market preferences
