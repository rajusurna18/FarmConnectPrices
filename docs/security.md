# FARMCONNECTPRICES — SECURITY ARCHITECTURE & GUIDELINES

## 1. Firebase Credentials & Secret Management

- **Local Development Keys:** Local credentials (such as `service-account.json` for Firebase Admin SDK or local `.env` files) must reside exclusively on local developer machines.
- **Git Exclusion Enforcement:** `.gitignore` explicitly excludes all service account JSON files (`*service-account*.json`, `*firebase-adminsdk*.json`) and environment variable files (`.env`, `.env.local`, `.env.*.local`).
- **Sanitized Template:** `.env.example` provides non-sensitive variable definitions and mock configuration placeholders. Never commit actual private keys, passwords, or tokens to version control.

---

## 2. Server-Side Authorization & Role Enforcement

- **Firestore Security Rules ([`firestore.rules`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/firestore.rules)):**
  - All collection access requires an authenticated Firebase user (`request.auth != null`).
  - Document ownership is strictly enforced via `request.auth.uid == userId` or `request.auth.uid == resource.data.ownerUid`.
  - User document updates block client manipulation of protected fields (`role`, `status`, `createdAt`).
  - Master reference collections (`crops`, `locations`) are read-only for authenticated clients (`allow write: if false;`).

- **Backend API Role Verification ([`FarmService.java`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/backend/src/main/java/com/farmlink/api/service/FarmService.java)):**
  - Server-side APIs enforce role-based access independently of frontend UI route guards.
  - Calling farm management APIs (`/api/v1/farms`) checks the authenticated user's role in Firestore (`users/{uid}`). Non-`FARMER` roles trigger an `AccessDeniedException`, resulting in an HTTP `403 FORBIDDEN` response.

---

## 3. Production Deployment Guidelines

Before deploying to production environments:
1. Ensure `GOOGLE_APPLICATION_CREDENTIALS` or cloud environment variables (e.g. GCP Secret Manager) inject the service account JSON dynamically.
2. Rotate and revoke any development keys that were exposed or shared outside secure channels.
3. Keep CORS origins strictly configured to production domain names in `SecurityConfig.java` (`app.cors.allowed-origins`).
