# Module 10I Runtime Stability & WebGL Verification Report

## Executive Summary
This report provides the complete forensic diagnosis and runtime stability audit of **FarmConnectPrices** following Module 10I. It separates genuine application runtime health from external Google Cloud / Firebase Firestore daily quota limits (`RESOURCE_EXHAUSTED`), verifies React Query retry behavior, and optimizes Three.js 3D canvas rendering to eliminate WebGL context loss.

---

## 1. Diagnostics & Root Cause Analysis

### A. HTTP 503 Service Unavailable Responses
- **Root Cause**: Daily Google Cloud / Firebase Firestore free quota limit (50,000 reads/day) reached on the target Firebase project (`farmconnectprices`).
- **Expected Behavior**: When Firestore throws `RESOURCE_EXHAUSTED`, `FirestoreQuotaGuard` transitions the circuit breaker from `CLOSED` to `OPEN` for a 60-second cooldown window.
- **Verification**: While `OPEN`, subsequent backend requests immediately throw `FirestoreQuotaExhaustedException` mapped to `HTTP 503 SERVICE_UNAVAILABLE` by `GlobalExceptionHandler`. Zero round-trips to Firestore occur during `OPEN` state. Stack trace logging is rate-limited.

### B. Three.js `THREE.Clock` Deprecation Warning
- **Root Cause**: `@react-three/fiber` (R3F internal frame loop) instantiates `THREE.Clock` internally in R3F v8.x when setting up default animation frame state for `useFrame(({ clock }) => ...)`.
- **Finding**: Our application code does not directly instantiate `new THREE.Clock()`. It reads `clock.getElapsedTime()` provided by R3F. The warning is third-party dependency-originated and non-breaking.

### C. Three.js `WebGLRenderer: Context Lost` Handling
- **Root Cause**: Browsers limit concurrent WebGL contexts per domain (typically 8–16 max). When multiple 3D components (`Hero3DElements`, `MarketNetworkVisual`, `IndiaMarketMap`, `EcosystemCanvas`, `AIDataSphere`) render or re-render during loading/error transitions, older contexts can be forcibly closed by the browser.
- **Fix Implemented**:
  1. Updated `WebGLBoundary.tsx` with a `MutationObserver` and direct canvas listeners to reliably catch `webglcontextlost` events on any child canvas and gracefully degrade to a high-performance 2D ambient fallback.
  2. Configured all `<Canvas>` components with `gl={{ alpha: true, powerPreference: 'low-power' }}` to reduce GPU resource allocation.
  3. Preserved `useInView3D` viewport intersection observing so offscreen 3D canvases are unmounted automatically.

---

## 2. Frontend Request Path & 503 Handling

- **Request Path**: `React View` → `React Query Hook` → `Axios Client` → `Firebase Auth Interceptor` → `Spring Security` → `REST Controller` → `FirestoreQuotaGuard` → `Firestore`.
- **No Automatic Retry Storms**:
  All React Query hooks enforce `defaultRetry`:
  ```ts
  const defaultRetry = (failureCount: number, error: unknown) => {
    if ((error as ApiError)?.response?.status === 503) return false;
    return failureCount < 1;
  };
  ```
  When the backend returns HTTP 503, React Query immediately aborts retries. Duplicate requests are completely eliminated.

---

## 3. Data Availability 3-State Contract Compliance

1. **State 1 (HTTP 200 + Real Data)**: Renders live AGMARKNET telemetry.
2. **State 2 (HTTP 200 + Empty List `[]`)**: Renders *"No market data available for the selected filters."*
3. **State 3 (HTTP 503 Service Unavailable)**: Renders *"Market data temporarily unavailable"* with a user-initiated **Retry** button (`onClick={() => refetch()}`).

> [!IMPORTANT]
> **No Fake Data Policy Preserved**: The system never substitutes mock, hardcoded, or default agricultural data (`DEFAULT_*`) during 503 states.

---

## 4. Verification Suite Matrix

| Verification Check | Target / Command | Result |
| :--- | :--- | :--- |
| **Backend Unit & Circuit Tests** | `.\mvnw.cmd clean test` | **BUILD SUCCESS (119 tests passed)** |
| **Frontend ESLint** | `npm run lint` | **0 warnings**, 0 errors |
| **Frontend Production Build** | `npm run build` | **SUCCESS** (`dist/` generated in 16.06s) |
| **Actuator Health** | `GET http://localhost:8080/actuator/health` | `HTTP 200 {"status":"UP"}` |
| **WebGL Fallback Boundary** | `WebGLBoundary.tsx` | Enhanced with canvas `webglcontextlost` capture |
| **Port 8080 Safety** | Port check via `Get-NetTCPConnection` | Clean single-instance backend running |

---

## 5. Technical Conclusion & Firestore Status Statement

> [!NOTE]
> **Technical Conclusion**:
> Application-side protection is functioning correctly, and all WebGL context loss handlers have been stabilized. Live Firestore market data remains temporarily unavailable via HTTP 503 because the external Firebase daily quota limit on Google Cloud is exhausted.
