# Module 24 — Dispute End-to-End Integration, Security Validation & Quality Assurance

## 1. Overview & Scope

**Stage 4.3** delivers end-to-end integration, security validation, and comprehensive quality assurance for **Module 24: Order Issues, Disputes & Resolution Foundation** across **FarmConnectPrices / FarmLink AI**.

This document certifies that the complete dispute workflow is operational, secure, accessible, and tested across:
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS, TanStack Query v5, React Router v7.
- **Backend**: Spring Boot 3.3.4, Java 21, Spring Security, Maven.
- **Authentication**: Firebase Authentication ID tokens with principal-derived UIDs.
- **Data & Security**: Google Cloud Firestore with `FirestoreQuotaGuard`, `firestore.rules`, and HMAC-SHA256 cursor signing.
- **Domain Integrations**: Authoritative Orders (Module 17), Deliveries (Module 19), Notifications (Module 21), and Preferences (Module 22).

---

## 2. API Contract Verification (All 8 Endpoints)

Every dispute endpoint was audited and verified against its Spring Boot backend implementation:

| # | Operation | Method | Backend Endpoint | Request DTO | Response DTO | Auth / Status |
|---|---|---|---|---|---|---|
| 1 | Create Dispute | `POST` | `/api/v1/marketplace/disputes` | `CreateDisputeRequest` | `DisputeResponse` | Authenticated; returns `201 CREATED` |
| 2 | My Disputes | `GET` | `/api/v1/marketplace/disputes/mine` | `@RequestParam(status, page, size, cursor)` | `DisputePageResponse` | Authenticated caller; returns `200 OK` |
| 3 | Related Disputes | `GET` | `/api/v1/marketplace/disputes/related` | `@RequestParam(status, page, size, cursor)` | `DisputePageResponse` | Authenticated counterparty; returns `200 OK` |
| 4 | Disputes by Order | `GET` | `/api/v1/marketplace/disputes/order/{orderId}` | Path: `orderId` | `List<DisputeResponse>` | Order participant only; returns `200 OK` |
| 5 | Dispute Detail | `GET` | `/api/v1/marketplace/disputes/{disputeId}` | Path: `disputeId` | `DisputeDetailResponse` | Participant only; returns `200 OK` |
| 6 | Add Response | `POST` | `/api/v1/marketplace/disputes/{disputeId}/responses` | `AddDisputeResponseRequest` | `DisputeResponseRecord` | Active participant; returns `201 CREATED` |
| 7 | Add Evidence | `POST` | `/api/v1/marketplace/disputes/{disputeId}/evidence` | `AddDisputeEvidenceRequest` | `DisputeEvidence` | Active participant; returns `201 CREATED` |
| 8 | Voluntary Closure | `POST` | `/api/v1/marketplace/disputes/{disputeId}/close` | Path: `disputeId` | `DisputeResponse` | Original creator only; returns `200 OK` |

---

## 3. All 9 Authoritative Dispute Categories

Frontend models (`dispute.ts`) and create forms (`CreateDisputePage.tsx`) were aligned with the authoritative backend `DisputeCategory.java` enum:

| Enum Constant | Human-Readable UI Label | Description |
|---|---|---|
| `PRODUCT_QUALITY` | Product Quality Issue | Substandard quality, discoloration, grade discrepancy |
| `DAMAGED_PRODUCT` | Damaged Product | Crushed crates, physical transit or handling damage |
| `QUANTITY_MISMATCH` | Quantity Mismatch | Weight or count received differs from agreed order |
| `WRONG_PRODUCT` | Wrong Product Delivered | Incorrect variety, cultivar, or commodity delivered |
| `DELIVERY_DELAY` | Delivery Delay | Produce arrived significantly after estimated delivery date |
| `DELIVERY_NOT_RECEIVED` | Delivery Not Received | Order marked dispatched or completed but not delivered |
| `PAYMENT_ISSUE` | Payment Issue | Billing discrepancy or payment settlement issue |
| `ORDER_CANCELLATION` | Order Cancellation Issue | Disputed cancellation after processing or payment |
| `OTHER` | Other Commercial Issue | Unspecified commercial disagreements between participants |

---

## 4. Authentication & Authorization Security

### A. Authentication Enforcement
- Configured in `SecurityConfig.java`:
  ```java
  .requestMatchers("/api/v1/marketplace/disputes/**").authenticated()
  ```
- Unauthenticated requests are immediately rejected by `FirebaseAuthFilter` / `DisputeController.getAuthenticatedToken()` with HTTP `401 UNAUTHORIZED`.
- Client-supplied UIDs in request bodies are ignored; the authenticated Firebase token UID (`token.getUid()`) is authoritative.

### B. Participant Scoping & Authorization Matrix
- **Dispute Creation**: Callers must be participants in the authoritative order (`callerUid == order.getFarmerId() || callerUid == order.getBuyerUid()`). All other callers receive HTTP `403 FORBIDDEN`.
- **Dispute Inspection**: Callers must be either `farmerUid` or `buyerUid`. Unrelated authenticated users receive HTTP `403 FORBIDDEN`.
- **Response & Evidence Submission**: Caller must be a participant in an **active** dispute (`OPEN`, `UNDER_REVIEW`, `WAITING_FOR_INFORMATION`). Non-participants receive `403 FORBIDDEN`; submissions to closed/terminal disputes receive `409 CONFLICT`.
- **Voluntary Closure**: Caller must be the **original creator** (`callerUid == dispute.getCreatedByUid()`). Counterparties receive `403 FORBIDDEN`.

### C. Three Primary User Profiles Invariant
The platform strictly preserves the three primary user profiles:
1. `FARMER`
2. `MEDIATOR_BUYER`
3. `CUSTOMER`

No fourth primary profile (`BUYER`, `SELLER`, `DELIVERY_PARTNER`, or ordinary `ADMIN`) exists. Participant eligibility is derived directly from the underlying commercial order.

---

## 5. Dispute State Machine & Data Integrity

```
               +---------------------------+
               |           OPEN            |
               +-------------+-------------+
                             |
              +--------------+--------------+
              |                             |
              v                             v
+---------------------------+ +---------------------------+
|       UNDER_REVIEW        | | WAITING_FOR_INFORMATION   |
+-------------+-------------+ +-------------+-------------+
              ^                             |
              |   (Participant response)    |
              +-----------------------------+
              |                             |
              v                             v
+---------------------------+ +---------------------------+
|         RESOLVED*         | |         REJECTED*         |
+-------------+-------------+ +-------------+-------------+
              |                             |
              +--------------+--------------+
                             |
                             v
               +---------------------------+
               |          CLOSED           | (Terminal state)
               +---------------------------+
```
*\*Privileged support adjudication (deferred). Ordinary participants cannot directly transition disputes to `RESOLVED` or `REJECTED`.*

### Key State Machine Rules:
1. **Voluntary Closure**: Original creator can transition active states (`OPEN`, `UNDER_REVIEW`, `WAITING_FOR_INFORMATION`) to `CLOSED`.
2. **Reopening Prohibited**: `CLOSED` is terminal (`isTerminal() == true`). Closed disputes reject transitions, responses, and evidence.
3. **Automatic Progression**: When a counterparty submits a response to a dispute in `WAITING_FOR_INFORMATION`, it automatically transitions back to `UNDER_REVIEW`, recording `INFORMATION_SUBMITTED` in history.
4. **Duplicate Locks**: Concurrency-safe atomic duplicate prevention via Firestore transaction on `disputeLocks/{orderId}_{category}`. When a dispute is closed, the lock is cleared (`activeDisputeId: null`), permitting future dispute filings if new issues arise.

---

## 6. Evidence Metadata Security

Evidence registration is **strictly metadata-only**:
- **Allowed MIME types**: `image/jpeg`, `image/png`, `image/webp`, `application/pdf`.
- **File size cap**: Maximum 10 MB (`MAX_FILE_SIZE = 10 * 1024 * 1024`).
- **No Client Storage Paths**: `storagePath` is never accepted from clients; it is persisted as `null` or server-managed.
- **Prominent User Notice Banner**: The frontend displays:
  > *"Secure evidence file upload will be available in a future update. Evidence registration is currently limited to metadata. No raw files are uploaded to public storage."*

---

## 7. Cryptographic Cursor Pagination Security

- **Tamper Resistance**: Cursors are signed with HMAC-SHA256 (`HmacSHA256`) using a production secret key (`DISPUTE_CURSOR_SECRET` >= 256 bits).
- **Constant-Time Verification**: Uses `MessageDigest.isEqual(...)` to eliminate timing attack vectors.
- **Security Binding**: Cursors bind `queryType|ownerUid|statusFilter|createdAt|disputeId`. Attempting to steal a cursor across different users or query types is rejected with `AccessDeniedException` / `IllegalArgumentException`.
- **Frontend Stack Invariant**: The UI maintains an opaque cursor history stack (`[undefined, cursor1, cursor2]`), forwarding tokens verbatim without client construction or decoding.

---

## 8. Notification & Quota Infrastructure

### A. Notifications (Module 21 & 22)
- Asynchronous event publication via `MarketplaceDomainEvent` dispatched through `ApplicationEventPublisher`.
- Category: Mapped to `NotificationCategory.ORDERS`.
- Supported Types: `DISPUTE_CREATED`, `DISPUTE_RESPONSE_RECEIVED`, `DISPUTE_CLOSED`.
- User Preference Enforcement: Respects user-configured notifications via `NotificationPreferenceService`.
- Transaction Isolation: Notification failures are caught and logged; they do not corrupt the underlying dispute transaction.

### B. Firestore Security Rules & Quota Guard
- `firestore.rules`:
  ```
  match /disputes/{disputeId} {
    allow read: if request.auth != null && (resource.data.buyerUid == request.auth.uid || resource.data.farmerUid == request.auth.uid);
    allow write: if false;
    ...
  }
  ```
  Direct client writes to `/disputes/**` are completely blocked (`allow write: if false;`).
- `FirestoreQuotaGuard`: Wraps all read, query, and transaction paths. If quota is exhausted (`RESOURCE_EXHAUSTED`), transitions to `OPEN` circuit breaker state with cooldown.

---

## 9. Test Execution & Verification Summary

### Frontend Test Results
```
 Test Files  12 passed (12)
      Tests  93 passed (93)
   Duration  2.78s
```
- `src/features/marketplace/__tests__/marketplaceDisputes.test.ts`: **19 passed**
- `src/features/marketplace/__tests__/marketplaceDisputesIntegration.test.ts`: **23 passed**
- `src/features/marketplace/__tests__/marketplaceDisputesUI.test.tsx`: **23 passed**
- All 9 other module test suites: **28 passed**
- **Total**: **93 tests passed (100%)**.

### Frontend Code Quality
- **ESLint**: `eslint src --max-warnings 0` — **0 errors, 0 warnings**.
- **Build**: `tsc -b && vite build` — **Built in 10.75s, 0 errors**.

### Backend Test Results
```
[INFO] Running com.farmlink.api.controller.DisputeControllerTest (14 tests) - PASS
[INFO] Running com.farmlink.api.model.DisputeModelTest (16 tests) - PASS
[INFO] Running com.farmlink.api.service.DisputeNotificationAndSecurityTest (26 tests) - PASS
[INFO] Running com.farmlink.api.service.DisputeServiceTest (19 tests) - PASS
[INFO] Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (12.418 s)
```
- **Total Backend Dispute Tests**: **75 tests passed (100%)**.

---

## 10. Known Limitations & Deferred Capabilities

1. **Privileged Adjudication Deferred**: Ordinary users can create disputes, submit responses, attach evidence metadata, and voluntarily close disputes. Formal third-party administrative resolution (`RESOLVED`, `REJECTED` with specific monetary remedy outcomes) is safely deferred until an authorized administrative portal is established.
2. **Binary Evidence Uploads Deferred**: Evidence remains metadata-only; raw binary uploads to Firebase Storage or cloud buckets remain deferred until verified storage upload hooks are provisioned.
3. **No Automatic Refunds**: Financial transactions remain governed strictly by Module 18 (Payments). Dispute records do not execute automated payment reversals or deductions.

---

## 11. Manual Verification Walkthrough

To manually verify the dispute workflows:
1. **Report Issue on Order**:
   - Navigate to `/marketplace/orders/:orderId`.
   - Click `⚠️ Report Issue` in the action bar or order disputes section.
   - Fill in subject, category, and description; submit.
   - Verify redirection to `/marketplace/disputes/:disputeId`.
2. **Add Responses**:
   - On the dispute detail page, enter a message in the response form.
   - Submit and confirm the message appears in the conversation thread and timeline.
3. **Register Evidence Metadata**:
   - Click `+ Register Evidence` in the supporting evidence section.
   - Enter file name (e.g. `proof.jpg`), MIME type (`image/jpeg`), file size, and description.
   - Submit and verify metadata card rendering with disclaimer banner.
4. **Voluntary Closure**:
   - As creator, click `Close Dispute` in the top voluntary resolution banner.
   - Confirm closure; verify status updates to `CLOSED` and forms disable.
