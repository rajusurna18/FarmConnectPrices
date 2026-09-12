# Module 17 — Inventory & Availability Control Semantics

## Core Invariants
1. **Quantity Boundary**: `0.0 <= availableQuantity <= quantity`
2. **Revalidation at Order Creation**: An order can only be created if the listing's current `availableQuantity >= offer.agreedQuantity`.
3. **Atomic Decrement**:
   `newAvailableQuantity = currentAvailableQuantity - agreedQuantity`
4. **Automatic SOLD_OUT Transition**:
   When `newAvailableQuantity == 0.0` and listing status is `ACTIVE`, the server automatically transitions listing status to `SOLD_OUT`.
5. **No Overselling**: Concurrent order creation requests execute within isolated Firestore write transactions (`firestore.runTransaction`), preventing negative inventory or overselling under high concurrency.

---

## Cancellation & Stock Restoration Semantics
1. **Eligible States**: Order cancellation is permitted only when status is `PENDING` or `CONFIRMED`.
2. **Atomic Restoration**:
   `restoredAvailableQuantity = currentAvailableQuantity + order.totalQuantity`
3. **Automatic ACTIVE Transition**:
   If the listing was previously `SOLD_OUT` and `restoredAvailableQuantity > 0.0`, status transitions back to `ACTIVE`.
4. **Idempotent Restoration**:
   Once an order is marked `CANCELLED`, any subsequent cancellation attempt throws `IllegalStateException`. Stock is restored **exactly once**, avoiding double-restoration defects.

---

## Explicit Scope Boundaries
- Module 17 models **Marketplace Listing Available Quantity**.
- Full warehouse inventory, multi-warehouse batch tracking, logistics dispatch, payment processing, and autonomous AI ordering are out of scope.
