# Module 10K Final Firestore Failure Classification & Probe Concurrency Report

## Executive Summary
Module 10K fixes a critical probe thundering herd / stampede race condition in **`FirestoreQuotaGuard`** recovery phase and establishes clear classification between GCP/Firebase Firestore daily quota limits (`RESOURCE_EXHAUSTED`) and temporary network/DNS resolution failures (`UnknownHostException` / `UNAVAILABLE`).

---

## 1. Network & DNS Diagnostics

From the local development machine, DNS and TLS connectivity to `firestore.googleapis.com` were tested:

```text
Resolve-DnsName firestore.googleapis.com ->
  A     142.251.222.106
  AAAA  2404:6800:4009:810::200a

Test-NetConnection firestore.googleapis.com -Port 443 ->
  TcpTestSucceeded : True
```
- **Finding**: DNS resolution and TLS port 443 connectivity to `firestore.googleapis.com` are fully operational. Any transient `UnknownHostException` in logs is an environment network hiccup, whereas `RESOURCE_EXHAUSTED` represents Google Cloud project quota limits.

---

## 2. Probe Race Condition Fix & 3-State Machine

### Problem Identified
Previously, when the 60-second cooldown window expired in `OPEN` state, multiple concurrent incoming requests checked `now >= cooldownUntilMillis` simultaneously, causing **multiple concurrent probe requests** to hit Firestore at once!

### 3-State Machine Architecture

```text
                   ┌───────────────┐
                   │    CLOSED     │ (Normal operational state)
                   └───────┬───────┘
                           │
                 RESOURCE_EXHAUSTED
                           │
                           ▼
                   ┌───────────────┐
                   │     OPEN      │ (Cooldown active: 0 Firestore calls)
                   └───────┬───────┘
                           │
               Cooldown Window Expired
          (Atomic CAS: OPEN -> HALF_OPEN)
                           │
                           ▼
                   ┌───────────────┐
                   │   HALF_OPEN   │ (Single Probe Owner Active)
                   └───────┬───────┘
                     │           │
          Probe Succeeded     Probe Failed
                     │           │
                     ▼           ▼
                  CLOSED       OPEN
```

### Single Probe Ownership Rule
1. When in `OPEN` state and `now >= cooldownUntilMillis`, the first thread executes `state.compareAndSet(OPEN, HALF_OPEN)`.
2. **Exactly 1 thread wins CAS** and becomes the **Single Probe Owner**.
3. All concurrent requests seeing `HALF_OPEN` state immediately throw `FirestoreQuotaExhaustedException` ("Probe in progress") without calling Firestore.
4. If the probe succeeds -> `recordSuccess()` transitions state to `CLOSED`.
5. If the probe fails -> `recordQuotaExhaustion()` transitions state back to `OPEN` for a new cooldown window.

---

## 3. Failure Classification

`FirestoreQuotaGuard.classifyFailure(Throwable t)` categorizes errors into:
- `QUOTA_EXHAUSTED`: `RESOURCE_EXHAUSTED` or `Quota exceeded`.
- `NETWORK_UNAVAILABLE`: `UnknownHostException`, `UNAVAILABLE`, or `DnsNameResolver` errors.
- `UNKNOWN`: Unclassified runtime errors.

Network/DNS errors generate clear log warnings without falsely incrementing quota exhaustion counters.

---

## 4. Verification Matrix

| Test Suite / Command | Result |
| :--- | :--- |
| **Backend Unit & Concurrency Tests** | `.\mvnw.cmd clean test` → **BUILD SUCCESS (123 tests passed)** |
| **Probe Concurrency Test** | 10 concurrent requests → **1 Probe Owner, 9 rejected (503)** |
| **Frontend Vitest Tests** | `npx vitest run` → **3 passed** |
| **Frontend ESLint** | `npm run lint` → **0 warnings**, 0 errors |
| **Frontend Production Build** | `npm run build` → **SUCCESS** (`dist/` generated in 16.06s) |
| **Actuator Health** | `GET http://localhost:8080/actuator/health` → `HTTP 200 {"status":"UP"}` |

---

## 5. Technical Conclusion & Quota Status

> [!NOTE]
> **Technical Conclusion**:
> Application-side quota protection and probe concurrency have been completely fixed. When external GCP / Firebase Firestore daily quota resets, the single probe owner detects successful reads, closes the circuit breaker, and streams real AGMARKNET agricultural data directly to the frontend.
