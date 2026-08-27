# FarmConnectPrices — Final Runtime Cleanup & Root Cause Analysis Report

## Executive Summary
This document provides a consolidated forensic analysis, root-cause resolution, and runtime verification for **FarmConnectPrices**. It confirms that the backend and frontend data pipeline functions cleanly, respects Google Cloud / Firebase Firestore daily quota limits with circuit breaker protection, and maintains a strict zero-fake-data policy.

---

## 1. Root Cause Classification & Fixes

### A. Application Bugs (Fixed)
1. **Un-handled Static Resource Exceptions (`{"error":"No static resource ."}`)**:
   - **Root Cause**: In Spring Boot 3.3, requests to non-existent static paths (such as `/favicon.ico` or `/`) threw `NoResourceFoundException`. Because `@ExceptionHandler(Exception.class)` in `GlobalExceptionHandler.java` caught it without an explicit handler, it returned HTTP 500 `{"error":"No static resource ."}` with a stack trace.
   - **Fix**: Added `@ExceptionHandler(NoResourceFoundException.class)` to [`GlobalExceptionHandler.java`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/backend/src/main/java/com/farmlink/api/exception/GlobalExceptionHandler.java) mapping missing static paths to HTTP 404 NOT_FOUND (`{"error": "Requested resource not found."}`).

2. **Probe Thundering Herd / Stampede Race**:
   - **Root Cause**: Multiple concurrent requests checking `now >= cooldownUntilMillis` simultaneously entered `OPEN` -> `HALF_OPEN` state, flooding Firestore with duplicate probe reads.
   - **Fix**: Implemented atomic 3-state machine (`CLOSED`, `OPEN`, `HALF_OPEN`) in [`FirestoreQuotaGuard.java`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/backend/src/main/java/com/farmlink/api/service/FirestoreQuotaGuard.java) using `state.compareAndSet(OPEN, HALF_OPEN)`. Exactly 1 thread becomes the **Single Probe Owner**, while concurrent requests during `HALF_OPEN` state are immediately rejected with HTTP 503 without executing gRPC calls to Firestore.

3. **Frontend Duplicate Request Storm & Un-normalized Query Keys**:
   - **Root Cause**: Inline filter object literals passed directly to React Query keys (`queryKey: ['markets', filters]`) caused cache invalidation on every render. Hooks also lacked status-aware retry suppression.
   - **Fix**: Created [`queryConfig.ts`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/utils/queryConfig.ts) containing `defaultRetry` (immediately returns `false` on HTTP 503) and primitive canonical `queryKeys` normalizer functions.

### B. Third-Party Dependency Warnings (Resolved)
1. **Three.js `THREE.Clock` Deprecation Warning**:
   - **Source**: Direct calls to `clock.getElapsedTime()` in 3D canvas components triggered `THREE.Clock: This module has been deprecated` in Three.js r180+.
   - **Fix**: Updated [`AIDataSphere.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/AIDataSphere.tsx), [`Hero3DElements.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/Hero3DElements.tsx), [`IndiaMarketMap.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/IndiaMarketMap.tsx), [`EcosystemCanvas.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/EcosystemCanvas.tsx), and [`MarketNetworkVisual.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/MarketNetworkVisual.tsx) to read `clock.elapsedTime` directly, eliminating deprecation warnings.

2. **WebGL Context Lost**:
   - **Source**: GPU resource allocation and Canvas lifecycle transitions during navigation.
   - **Fix**: Implemented [`WebGLBoundary.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/WebGLFallback.tsx) to catch `webglcontextlost` events and gracefully degrade to 2D ambient fallback visuals.

### C. External Firebase / Firestore Limitations (Documented)
- **Firebase Firestore Quota**: Project `farmconnectprices` runs on Firebase Standard Free Tier (50,000 document reads/day). When daily GCP quota is exhausted, Firestore returns `RESOURCE_EXHAUSTED`.
- **Application Guarantee**: `FirestoreQuotaGuard` trips to `OPEN`, returning controlled HTTP 503 SERVICE_UNAVAILABLE responses. Zero fallback/mock agricultural data is rendered, preserving data integrity. Once daily quota resets, the single probe owner automatically closes the circuit and streams live AGMARKNET telemetry.

---

## 2. Verification Matrix

| Verification Step | Command / Endpoint | Expected | Status |
| :--- | :--- | :--- | :--- |
| **Backend Unit Tests** | `.\mvnw.cmd clean test` | 123 tests passed, 0 failures, 0 errors | **PASSED** |
| **Probe Stampede Concurrency** | `FirestoreQuotaGuardTest` | 1 Probe Owner, 9 rejected (503) | **PASSED** |
| **Frontend Unit Tests** | `npx vitest run` | 3 tests passed | **PASSED** |
| **Frontend ESLint** | `npm run lint` | 0 warnings, 0 errors | **PASSED** |
| **Frontend Production Build** | `npm run build` | `dist/` generated in 56.23s | **PASSED** |
| **Backend Health** | `GET http://localhost:8080/actuator/health` | `HTTP 200 {"status":"UP"}` | **PASSED** |
| **Authentication Protection** | `GET http://localhost:8080/api/v1/crops` | `HTTP 401 Unauthorized` | **PASSED** |

---

## 3. Data Integrity & Contract Summary

| API Condition | Response Status | UI Rendered State |
| :--- | :--- | :--- |
| **Real AGMARKNET Data Present** | `HTTP 200 + data` | Render live telemetry & charts |
| **No Data Found for Query** | `HTTP 200 + []` | "No market data available for selected filters." |
| **Database Quota Exhausted** | `HTTP 503` | "Live market telemetry temporarily unavailable due to database quota limits. Retry shortly." |

---

## 4. Files Modified

- [`GlobalExceptionHandler.java`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/backend/src/main/java/com/farmlink/api/exception/GlobalExceptionHandler.java)
- [`FirestoreQuotaGuard.java`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/backend/src/main/java/com/farmlink/api/service/FirestoreQuotaGuard.java)
- [`FirestoreQuotaGuardTest.java`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/backend/src/test/java/com/farmlink/api/service/FirestoreQuotaGuardTest.java)
- [`queryConfig.ts`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/utils/queryConfig.ts)
- [`queryConfig.test.ts`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/utils/queryConfig.test.ts)
- [`tsconfig.app.json`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/tsconfig.app.json)
- 3D components: [`AIDataSphere.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/AIDataSphere.tsx), [`Hero3DElements.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/Hero3DElements.tsx), [`IndiaMarketMap.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/IndiaMarketMap.tsx), [`EcosystemCanvas.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/EcosystemCanvas.tsx), [`MarketNetworkVisual.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/MarketNetworkVisual.tsx)
