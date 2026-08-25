# FARMCONNECTPRICES — PERFORMANCE & BUNDLE OPTIMIZATION ARCHITECTURE

## 1. Executive Summary

To deliver a high-performance web experience across desktop and low-power mobile devices, **FarmConnectPrices** implements targeted React code-splitting, dynamic component loading, and WebGL rendering boundaries.

---

## 2. Bundle Optimization Metrics

### Pre-Optimization Baseline
- **Monolithic Main Chunk:** `dist/assets/index-DDcfzUwN.js` (**1,989.75 kB / ~1.99 MB**)
- **Issue:** Loaded Three.js, React Three Fiber, Drei, Recharts, Framer Motion, and all sub-routes upfront during initial page load.

### Post-Optimization Architecture
- **Route-Level Code-Splitting:** Dynamic imports (`React.lazy`) for non-landing routes (`DashboardPage`, `ProfilePage`, `EditProfilePage`, `RoleSelectionPage`, `FarmListPage`, `CreateFarmPage`, `FarmDetailPage`, `EditFarmPage`, `FarmCropsPage`).
- **Section-Level Code-Splitting:** Lazy-loading heavy 3D visual sections (`Hero3DElements`, `EcosystemSection`, `IndiaNetworkSection`, `AISection`).
- **Zero Layout-Shift Hero:** The primary landing hero shell (Title, Tagline, CTAs, Navbar, and Ambient CSS Gradient) renders synchronously, eliminating layout shifts while heavy 3D canvases load asynchronously.

---

## 3. 3D & Mobile Viewport Strategy

- **Device Pixel Ratio (DPR):**
  - Desktop: Clamped between `1` and `1.5` (`dpr={[1, 1.5]}`).
  - Mobile (< 768px): Strictly set to `1` (`dpr={1}`) to minimize GPU frame memory overhead.
- **Particle Reduction:**
  - Instanced mesh particle counts drop from 30 (desktop) to 4 (mobile) in `Hero3DElements`.
- **WebGL Boundaries ([`WebGLFallback.tsx`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/frontend/src/components/3d/WebGLFallback.tsx)):**
  - React Error Boundaries wrap Three.js canvases, ensuring WebGL context loss falls back cleanly without breaking application state.
- **Touch Targets:**
  - All interactive controls enforce a minimum touch height of **>= 48px** (`min-h-[50px]`) for mobile touch compliance.
