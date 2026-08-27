# Module 10F — Authenticated Runtime Verification Report

## Overview
This document records the end-to-end authentication flow, token verification, protected API security contracts, and runtime read budget for **FarmConnectPrices**.

---

## 1. Authentication Pipeline & Flow Architecture

```text
React User Login (Firebase Auth)
        │
        ▼
getCurrentIdToken() (Firebase Client SDK)
        │
        ▼
apiClient Axios Interceptor (frontend/src/services/api.ts)
        │ Attach header: Authorization: Bearer <Firebase ID token>
        ▼
Spring Boot FirebaseAuthFilter (backend/src/main/java/com/farmlink/api/security/FirebaseAuthFilter.java)
        │ FirebaseAuth.verifyIdToken(idToken)
        ▼
SecurityContextHolder (Populated with FirebaseAuthenticationToken)
        │
        ▼
REST Controller (CropMasterController, MarketController, MarketPriceController, MarketIntelligenceController)
        │ getAuthenticatedToken() -> Valid token
        ▼
Cached Service Layer (CropMasterService, MarketService, LocationMasterService)
        │ AtomicReference / @Cacheable(sync = true) $O(1)$ memory lookup
        ▼
Firestore Database -> REST API Response -> React UI
```

---

## 2. Security Verification & 401 Behavior

- **Unauthenticated Direct Request**:
  - Request: `GET /api/v1/crops` (No Authorization header)
  - Result: **HTTP 401 Unauthorized** (`"Unauthorized"`)
  - Status: **VERIFIED & SECURE**

- **Authenticated Application Request**:
  - Request: `GET /api/v1/crops` (With `Authorization: Bearer <token>` attached by `apiClient.ts`)
  - Security check passes, executing cached service lookup to return real Firestore data / `[]` / 503 depending on database telemetry state.

---

## 3. Read Budget Matrix for Authenticated Requests

| Endpoint | Security Check | First Request (Cold) | Warm Request | Concurrency Protection |
| :--- | :--- | :--- | :--- | :--- |
| **`GET /api/v1/crops`** | Firebase Token Required | 1 collection scan | 0 reads | `sync = true` single-flight |
| **`GET /api/v1/markets`** | Firebase Token Required | 1 collection scan | 0 reads | `sync = true` single-flight |
| **`GET /api/v1/market-prices`** | Firebase Token Required | Bounded query (max 100) | 0 master reads | Map lookup metadata |
| **`GET /api/v1/market-intelligence/trends`** | Firebase Token Required | Bounded price query | 0 master reads | Map lookup metadata |

---

## 4. Verification Suite Results

- **Backend Automated Unit Tests**: `.\mvnw.cmd clean test` → **BUILD SUCCESS (115 passed)**
- **Read-Budget Concurrency Suite**: `FirestoreReadBudgetTest.java` → **PASSED**
- **Frontend ESLint**: `npm run lint` → **0 warnings**
- **Frontend Production Build**: `npm run build` → **SUCCESS** (`dist/` generated in 14.68s)
- **Actuator Health**: `GET http://localhost:8080/actuator/health` → `HTTP 200 {"status":"UP"}`
