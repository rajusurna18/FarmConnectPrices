# Firestore Quota Protection & Circuit Breaker Architecture

## Overview
This document details the **`FirestoreQuotaGuard`** circuit breaker architecture designed to handle Google Cloud / Firebase Firestore daily read quota limits (`RESOURCE_EXHAUSTED: Quota exceeded`) in **FarmConnectPrices**.

---

## State Machine Architecture

```text
       Request
          │
          ▼
FirestoreQuotaGuard.checkQuotaAvailability()
          │
    Is Circuit OPEN?
    ├── YES ─────────────────────────────► Immediately throw FirestoreQuotaExhaustedException
    │                                                    │
    │                                                    ▼
    │                                          HTTP 503 SERVICE_UNAVAILABLE
    │                                          (0 Firestore gRPC calls)
    └── NO
         │
         ▼
     Firestore
         │
    ┌────┴──────────────────────────┐
    │                               │
 Success                     RESOURCE_EXHAUSTED
    │                               │
    ▼                               ▼
Continue                  Trip Circuit to OPEN
                         (60-second Cooldown Window)
```

---

## Technical Specifications

1. **State Machine & Cooldown**:
   - `CLOSED` (Healthy): Allows database operations through to Firestore.
   - `OPEN` (Cooldown Active): When `RESOURCE_EXHAUSTED` occurs, the guard trips `OPEN` for 60 seconds (`DEFAULT_COOLDOWN_MS = 60,000L`).
   - During `OPEN` state, all database requests immediately throw `FirestoreQuotaExhaustedException` without executing gRPC round-trips to Firestore.

2. **Log Rate Limiting**:
   - Suppresses stack trace dumps per request.
   - Logs quota exhaustion warning at most once per cooldown window.

3. **HTTP 503 Contract**:
   - `GlobalExceptionHandler` converts `FirestoreQuotaExhaustedException` to `HTTP 503 SERVICE_UNAVAILABLE` with payload:
     `{"error": "Live market discovery is temporarily unavailable due to database quota limits. Please retry shortly."}`

4. **Zero Fallback Data Guarantee**:
   - No mock, fake, or fallback agricultural data (`DEFAULT_*`) is injected when in 503 state.
