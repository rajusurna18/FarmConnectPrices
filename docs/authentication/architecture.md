# FarmConnectPrices — Authentication Architecture

This document describes the Firebase Authentication and Spring Security integration architecture for FarmConnectPrices.

## Architecture Overview

```text
React Frontend                     Spring Boot API               Firebase Platform
  │                                     │                              │
  ├── Login / Register ─────────────────┼─────────────────────────────►│ Firebase Auth
  │   (Email/Password)                  │                              │
  │                                     │                              │
  │◄── ID Token (JWT) ──────────────────┼──────────────────────────────┘
  │
  ├── API Request ─────────────────────►│
  │   Authorization: Bearer <ID_TOKEN>  │
  │                                     ├── Verify ID Token ──────────► Firebase Admin SDK
  │                                     │   (FirebaseAuthFilter)
  │                                     ├── Establish SecurityContext
  │                                     │
  │◄── Protected Response ──────────────┤ (GET /api/v1/users/me)
```

## Key Components

1. **Frontend Auth Layer (`frontend/src/features/auth/`):**
   - **`AuthContext.tsx`:** Manages reactive authentication state across React components using Firebase `onAuthStateChanged`.
   - **`services/firebase/auth.ts`:** Encapsulates Web SDK auth calls (`loginWithEmail`, `registerWithEmail`, `sendPasswordReset`, etc.).
   - **`services/api.ts`:** Axios interceptor automatically appending `Authorization: Bearer <ID_TOKEN>`.
   - **`ProtectedRoute.tsx`:** Guards client-side routes (`/dashboard`, `/profile`).

2. **Backend Security Layer (`backend/src/main/java/com/farmlink/api/security/`):**
   - **`FirebaseAuthFilter.java`:** Spring Security filter extracting Bearer tokens and verifying them via Firebase Admin SDK.
   - **`FirebaseAuthenticationToken.java`:** Spring Security principal representing the verified user.
   - **`SecurityConfig.java`:** Configures stateless session policy and authorization rules.

3. **Firestore Security (`firestore.rules`):**
   - Implements user-level document security on `users/{userId}` enforcing read/write access strictly for `request.auth.uid == userId`.
   - Enforces field immutability on `uid`, `role`, `status`, and `createdAt`.
