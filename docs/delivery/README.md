# Module 19 — Delivery & Logistics Foundation Documentation

## Overview
Module 19 provides physical shipment tracking, address snapshot management, logistics partner allocation, and single-transaction order completion for the FarmConnectPrices / FarmLink AI platform.

---

## Key Architectural Guarantees

1. **Role Architecture Integrity:**
   - Enforces exactly three primary user roles: `FARMER`, `MEDIATOR_BUYER`, `CUSTOMER`.
   - Logistics drivers / partners are embedded as internal domain models (`DeliveryPartnerInfo`) and do not act as primary user login roles.

2. **Domain Separation:**
   - Commercial agreements & order lifecycle (`PENDING` → `CONFIRMED` → `PROCESSING` → `COMPLETED` / `CANCELLED`) belong to Module 17 (`OrderService`).
   - Payment intents and gateways belong to Module 18 (`PaymentService`).
   - Physical shipment transitions and tracking belong to Module 19 (`DeliveryService`).

3. **Deterministic Uniqueness:**
   - Delivery ID follows deterministic pattern `del_{orderId}` in collection `deliveries`.
   - Prevent duplicate delivery requests transactionally.

4. **Atomic Single-Transaction Order Completion:**
   - Transitioning a delivery to `DELIVERED` executes a single Firestore transaction mutating both `deliveries/{deliveryId}` and `orders/{orderId}`.
   - If the transaction fails, both document writes roll back completely, ensuring **zero silent inconsistent state**.

---

## Delivery State Machine

```
CREATED → ASSIGNED → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
   └──────────┴─────────────┴────────────► CANCELLED (Prior to pickup)
```

| Status | Trigger Action | Allowed Actor |
| :--- | :--- | :--- |
| `CREATED` | `POST /api/v1/marketplace/deliveries` | Buyer / Farmer (Paid Order) |
| `ASSIGNED` | `POST /api/v1/marketplace/deliveries/{id}/assign` | Farmer / Admin |
| `READY_FOR_PICKUP` | `POST /api/v1/marketplace/deliveries/{id}/ready` | Farmer / Admin |
| `PICKED_UP` | `POST /api/v1/marketplace/deliveries/{id}/pickup` | Farmer / Driver |
| `IN_TRANSIT` | `POST /api/v1/marketplace/deliveries/{id}/in-transit` | Farmer / Driver |
| `OUT_FOR_DELIVERY` | `POST /api/v1/marketplace/deliveries/{id}/out-for-delivery` | Farmer / Driver |
| `DELIVERED` | `POST /api/v1/marketplace/deliveries/{id}/delivered` | Farmer / Driver / Buyer |
| `CANCELLED` | `POST /api/v1/marketplace/deliveries/{id}/cancel` | Buyer / Farmer |

---

## Security & Address Privacy

- Direct client writes to `deliveries` in `firestore.rules` are disabled (`allow write: if false`). All updates are mediated via Spring Boot Admin SDK.
- Address details (`pickupAddress`, `deliveryAddress`) are protected and returned only to authorized participants (`buyerUid` or `farmerId`).
