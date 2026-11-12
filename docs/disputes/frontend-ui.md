# Module 24 — Dispute Management Frontend UI Documentation

## 1. Overview

**Module 24: Order Issues, Disputes & Resolution Foundation** provides a structured, multi-participant dispute resolution interface for the **FarmConnectPrices / FarmLink AI** platform.

Stage 4.2 delivers the complete responsive, accessible frontend user interface, integrating with the approved Stage 4.1 TypeScript models, API service, and TanStack Query foundation.

---

## 2. Architecture & Invariants

| Principle | Specification / Implementation |
| :--- | :--- |
| **Primary User Profiles** | Exactly three profiles: `FARMER`, `MEDIATOR_BUYER`, `CUSTOMER`. No ordinary administrative adjudication; disputes are participant-scoped. |
| **API Mediation** | 100% of dispute reads, mutations, responses, and closures are mediated through backend REST endpoints with Firebase authentication. Zero direct Firestore access from the browser. |
| **Opaque Cursor Forwarding** | Backend HMAC-SHA256 signed pagination cursors are treated as completely opaque tokens, maintained in a cursor history stack (`cursorHistory`), and forwarded verbatim. |
| **Evidence Registration** | Strictly metadata-only registration (`fileName`, `contentType`, `fileSize`, `description`). No binary uploads or fake storage paths. Display mandatory honest disclaimer banner. |
| **Voluntary Dispute Closure** | Original dispute creators may voluntarily close an active dispute (`OPEN`, `UNDER_REVIEW`, `WAITING_FOR_INFORMATION`) when mutual agreement is reached. |
| **Order Integration** | Direct integration into `OrderDetailPage` with contextual "⚠️ Report Issue" CTA and an embedded "Order Disputes & Issues" audit list. |

---

## 3. Route Inventory

All dispute routes are protected under `<Route element={<ProtectedRoute />}>` in `App.tsx` and accessible to all authenticated participant profiles:

| Route Path | Component | Purpose |
| :--- | :--- | :--- |
| `/marketplace/disputes` | `MyDisputesPage` | Displays disputes initiated by the authenticated caller with status metrics, filter bar, and cursor pagination. |
| `/marketplace/disputes/related` | `RelatedDisputesPage` | Displays disputes involving orders where the caller is the farmer or buyer counterparty. |
| `/marketplace/disputes/new/:orderId` | `CreateDisputePage` | Structured issue reporting form linked to an existing order, with active dispute conflict detection and input validation. |
| `/marketplace/disputes/:disputeId` | `DisputeDetailPage` | Complete dispute resolution view including overview, adjudication banner, counterparty message thread, evidence list, and audit timeline. |

---

## 4. Component Hierarchy

```
frontend/src/features/marketplace/
├── components/
│   └── disputes/
│       ├── DisputeStatusBadge.tsx       # Reusable status pill with distinct semantic colors for all 6 statuses
│       ├── DisputeFilters.tsx           # Status filter bar (ALL, OPEN, UNDER_REVIEW, WAITING_FOR_INFORMATION, etc.)
│       ├── DisputeCard.tsx              # Summary card for dispute lists with identifiers, roles, and navigation
│       ├── DisputeTimeline.tsx          # Vertical audit trail rendering DisputeHistoryEntry[] events with icons
│       ├── DisputeResponseForm.tsx      # 2000-char validated response submission form with spinner and error handling
│       └── DisputeEvidenceSection.tsx   # Evidence metadata list, honest upload disclaimer, and registration form
└── pages/
    ├── MyDisputesPage.tsx               # Primary "My Disputes" view with metrics breakdown and cursor pagination
    ├── RelatedDisputesPage.tsx          # Counterparty "Related Disputes" view with metrics and cursor pagination
    ├── CreateDisputePage.tsx            # Order issue reporting page with order summary card and validation
    └── DisputeDetailPage.tsx           # Full dispute detail view with voluntary close modal, responses, and evidence
```

---

## 5. Navigation & Order Page Integration

### Global Navigation (`Navbar.tsx`)
- Desktop and mobile navigation menus include a direct **Disputes** link (`/marketplace/disputes`) for all authenticated users.
- Sub-navigation links within dispute views allow seamless switching between **My Disputes**, **Related Disputes**, and **My Orders**.

### Order Details (`OrderDetailPage.tsx`)
- **Action Bar CTA**: Contextual `⚠️ Report Issue` button linking to `/marketplace/disputes/new/:orderId`.
- **Embedded Dispute Section**: `⚖️ Order Disputes & Issues` card rendering all disputes filed on the current order with status badges and deep-links.

---

## 6. Verification & Test Coverage

### Test Suites
1. **Frontend Foundation & API Tests**: `frontend/src/features/marketplace/__tests__/marketplaceDisputes.test.ts` (19 tests)
2. **Frontend UI Components & Invariant Tests**: `frontend/src/features/marketplace/__tests__/marketplaceDisputesUI.test.tsx` (23 tests)
3. **Total Frontend Test Suite**: 11 test suites, **70/70 passing tests**.

### Build & Lint Verification
- **TypeScript**: `tsc -b` compiled with zero errors.
- **ESLint**: `eslint src --max-warnings 0` passed with 0 errors and 0 warnings.
- **Vite Production Build**: Production bundle generated successfully in 12.28s.
