# Module 18 — Payments Foundation Documentation

## 1. Overview & Objective
Module 18 establishes a secure, backend-authoritative, and extensible payment foundation for marketplace orders in **FarmConnectPrices**.

### Key Architectural Invariants
- **Backend Authoritative**: The frontend can request payment intent creation and initiation, but cannot declare payment success. Only independent backend verification (or trusted gateway webhooks) can transition a payment to `SUCCESS`.
- **Server-Side Amount Derivation**: Payment amount (`order.totalAmount`) and currency (`order.currency`) are loaded strictly from the trusted `Order` in Firestore. Client payloads supplying client-authoritative amounts or currency are ignored or rejected.
- **Single Aggregate Model**: Payment intent, status, provider transaction references, reconciliation values, and attempt history are stored in a single document in Firestore (`payments/pay_{orderId}`). This design avoids N+1 reads and preserves Firestore quota safety (`RESOURCE_EXHAUSTED`).
- **Idempotency**: Guarantees **One Order → At Most One Active Payment Intent** and **One Order → At Most One Successful Payment**. Duplicate create-intent requests return the existing active intent without creating duplicate documents.

---

## 2. Payment Lifecycle & State Machine

```
              ┌───────────┐
              │  CREATED  │
              └─────┬─────┘
                    │
                    v
              ┌───────────┐         (Intent Timeout)
              │  PENDING  ├────────────────────────────────┐
              └─────┬─────┘                                │
                    │                                      │
                    v                                      v
              ┌───────────┐       (User/System Cancel)   ┌───────────┐
              │ PROCESSING├─────────────────────────────>│ CANCELLED │
              └─┬───────┬─┘                              └───────────┘
                │       │
      (Verified │       │ (Failed verification /
       Success) │       │  Gateway failure)
                v       v
         ┌─────────┐ ┌────────┐
         │ SUCCESS │ │ FAILED │
         └─────────┘ └────────┘
          (TERMINAL) (TERMINAL for attempt)
```

| State | Description | Immutable / Terminal? |
| :--- | :--- | :--- |
| `CREATED` | Initial state during payment entity instantiation. | No |
| `PENDING` | Active payment intent initialized and waiting for payment submission. | No |
| `PROCESSING` | Payment checkout opened; verification in progress. | No |
| **`SUCCESS`** | Payment independently verified by backend and amount reconciled. | **YES (TERMINAL)** |
| `FAILED` | Payment attempt failed or amount/currency reconciliation mismatch. | Terminal for attempt |
| `CANCELLED` | Payment intent cancelled by user or system. | **YES (TERMINAL)** |
| `EXPIRED` | Payment intent TTL (15 minutes) exceeded without payment. | **YES (TERMINAL)** |

> [!CAUTION]
> The terminal state `SUCCESS` is immutable. No retry, cancellation, or error webhook can alter a payment once `SUCCESS` is achieved.

---

## 3. Order Eligibility & Domain Boundaries
- **Payable Order State**: Only **`CONFIRMED`** orders (where the seller farmer has confirmed readiness to fulfill) are eligible for payment initiation.
- **Ineligible Order States**: `PENDING` (unconfirmed), `CANCELLED`, or `COMPLETED` orders will reject payment intent creation with `409 CONFLICT` or `400 BAD_REQUEST`.
- **Inventory Boundary**: Payment success does **NOT** alter listing inventory (`availableQuantity`). Inventory deduction occurred when the order was created in Module 17.
- **Fulfillment Boundary**: Payment success does **NOT** automatically mark an order `COMPLETED` or assign delivery/logistics. Fulfillment remains a separate lifecycle phase in future modules.

---

## 4. Provider Abstraction & Development Mock

### Provider Interface (`PaymentProvider`)
Isolates gateway integrations (`MOCK`, `RAZORPAY`, `STRIPE`, `CASHFREE`, `PAYU`) from domain logic:
```java
public interface PaymentProvider {
    PaymentProviderType getType();
    PaymentIntentProviderResponse createIntent(PaymentIntentProviderRequest request);
    PaymentVerificationProviderResponse verifyPayment(PaymentVerificationProviderRequest request);
    PaymentStatusProviderResponse getStatus(String providerPaymentId);
}
```

### Mock Provider (`MockPaymentProvider`)
- Enabled when `payment.provider=MOCK`.
- Never connects to real financial rails or production credentials.
- Generates test-safe transaction references (`mock_pi_...`, `tx_mock_...`).

### Production Safety Guard (`PaymentEnvironmentGuard`)
If `payment.environment=production` and `payment.provider=MOCK`, Spring Boot initialization will fail fast with an `IllegalStateException`, preventing accidental mock usage in production.

---

## 5. API Reference

| Endpoint | Method | Role / Auth | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/marketplace/payments/create-intent` | `POST` | Buyer (`MEDIATOR_BUYER` / `CUSTOMER`) | Create or retrieve active payment intent for a `CONFIRMED` order. |
| `/api/v1/marketplace/payments/{paymentId}/verify` | `POST` | Buyer / Participant | Independently verify payment attempt & reconcile amounts. |
| `/api/v1/marketplace/payments/mine` | `GET` | Buyer | Get paginated payment history for authenticated buyer. |
| `/api/v1/marketplace/payments/received` | `GET` | Farmer | Get safe payment status view for orders received by seller farmer. |
| `/api/v1/marketplace/payments/{paymentId}` | `GET` | Participant | Get payment details by payment ID. |
| `/api/v1/marketplace/payments/webhook/{provider}` | `POST` | Webhook / Provider | Provider webhook endpoint with signature validation. |

---

## 6. Firestore Rules & Indexes

### Firestore Security Rules (`firestore.rules`)
```javascript
match /payments/{paymentId} {
  allow read: if request.auth != null && (resource.data.buyerUid == request.auth.uid || resource.data.farmerId == request.auth.uid);
  allow write: if false;
}
```

### Composite Indexes (`firestore.indexes.json`)
- `payments`: `buyerUid` (ASC), `createdAt` (DESC)
- `payments`: `farmerId` (ASC), `createdAt` (DESC)

---

## 7. Quality & Testing Verification
- **Backend Suite**: 287 tests run, 0 failures, 0 errors.
- **Frontend Suite**: 25 tests run, 0 failures.
- **TypeScript**: `npx tsc --noEmit` passed with 0 errors.
- **ESLint**: `npx eslint src --max-warnings 0` passed clean.
- **Production Build**: `vite build` completed successfully.
