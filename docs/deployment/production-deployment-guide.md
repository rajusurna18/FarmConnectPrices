# FarmConnectPrices — Production Deployment Guide
## Multi-Tier Architecture Deployment Runbook

---

## 1. Overview & Architecture

**FarmConnectPrices (FarmLink AI)** uses a decoupled cloud architecture with Firebase Hosting serving as the primary frontend delivery network and reverse-proxy gateway:

- **Frontend SPA**: React 19 + TypeScript + Vite, served globally via Firebase Hosting CDN.
- **Backend API**: Java 21 LTS + Spring Boot 3.3.4, packaged as a container image and hosted on Google Cloud Run (`farmlink-api` in `asia-south1`).
- **Unified Origin Routing**: Firebase Hosting rewrites `/api/**` requests directly to Cloud Run, providing same-origin API access without client-side CORS dependencies.
- **Identity & Data**: Firebase Authentication and Google Cloud Firestore.

```
[Browser Client]
       │
       │ HTTPS (Same-Origin)
       ▼
[Firebase Hosting CDN] ────────(/api/** Rewrite)───────> [Google Cloud Run (farmlink-api)]
  - Static Assets (/assets/**)                                  │
  - SPA Fallback (index.html)                                   │ Google Cloud Admin SDK / gRPC
                                                                ▼
                                                 [Firestore & Firebase Auth]
```

> [!IMPORTANT]
> **Production Deployment Authorization Notice**: Production deployment requires separate, explicit authorization following completion of the production readiness review. Production deployment is not currently authorized.

---

## 2. Prerequisites & Cloud Accounts

1. **Google Cloud / Firebase Production Project**:
   - Project ID: `farmconnectprices`
   - Access to [Firebase Console](https://console.firebase.google.com/) and [Google Cloud Console](https://console.cloud.google.com/).
   - Project on **Blaze Plan (Pay-as-you-go)**.
   - Cloud Billing budget alert configured.
2. **Backend Service Prerequisites**:
   - Google Cloud Run service `farmlink-api` provisioned in region `asia-south1`.
3. **Frontend Hosting Prerequisites**:
   - Firebase CLI (`firebase-tools`) installed.
   - Authenticated operator session with access to project `farmconnectprices`.

---

## 3. Backend Service & Cloud Run Prerequisites

Before deploying the frontend, the operator must separately verify the backend Cloud Run service readiness in the Google Cloud Console:

### A. Cloud Run Verification Checklist
1. **Service Identity**: Service `farmlink-api` exists in project `farmconnectprices` and region `asia-south1`.
2. **Service Readiness**: Latest revision is 100% healthy with 100% traffic allocation.
3. **Health Probes**: Liveness and readiness probes respond successfully:
   - `GET /actuator/health` or `GET /api/v1/health` returns HTTP 200 `{"status":"UP"}`.
4. **Invocation Configuration**:
   - Verify the correct supported Firebase Hosting-to-Cloud Run invocation configuration.
   - Do **NOT** grant invocation permissions to `allUsers` by default, as doing so unnecessarily exposes the raw Cloud Run URL directly to the public internet and bypasses Firebase Hosting perimeter controls.

### B. Backend Environment Variables (Configured in Cloud Run Dashboard)

| Variable Name | Required | Description |
| :--- | :---: | :--- |
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` (activates `application-prod.yml`) |
| `PORT` | Yes | `8080` (or platform default) |
| `FIREBASE_PROJECT_ID` | Yes | `farmconnectprices` |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Yes | Service account credentials for Firestore and Auth Admin SDK |
| `DISPUTE_CURSOR_SECRET` | Yes | Cryptographic HMAC secret (min 32 chars) for pagination signing |
| `APP_CORS_ALLOWED_ORIGINS` | Yes | Allowed origins for direct access if applicable |
| `FIRESTORE_QUOTA_COOLDOWN_MS` | No | `60000` (circuit breaker cooldown in ms, default 60s) |

---

## 4. Frontend Production Build & Hosting Deployment

### A. Production Frontend Build Configuration

The frontend production build uses Vite and requires specific environment configuration:

- **Same-Origin API Base URL**: In production, `VITE_API_BASE_URL` must be set to `/`. The frontend application's `resolveApiBaseUrl()` converts `/` into an empty Axios base URL (`""`), ensuring all API requests target same-origin `/api/v1/...` and route through the Firebase Hosting `/api/**` rewrite to Cloud Run.
- **Firebase Web SDK Configuration**: Public client configuration variables must point to the production Firebase project `farmconnectprices`:
  - `VITE_FIREBASE_API_KEY`: Production Web API Key.
  - `VITE_FIREBASE_AUTH_DOMAIN`: `farmconnectprices.firebaseapp.com`
  - `VITE_FIREBASE_PROJECT_ID`: `farmconnectprices`
  - `VITE_FIREBASE_STORAGE_BUCKET`: `farmconnectprices.firebasestorage.app`
  - `VITE_FIREBASE_MESSAGING_SENDER_ID`: Production Cloud Messaging Sender ID.
  - `VITE_FIREBASE_APP_ID`: Production Firebase Web App ID.
- **Client-Side Configuration Security**:
  - All `VITE_*` variables are embedded into public JavaScript chunks during `vite build` and are visible in browser developer tools.
  - **NEVER** place private keys, server secrets, or service account JSON into `VITE_*` variables.
  - Production environment values must be supplied securely through the approved build environment (e.g. CI/CD pipeline secrets or environment injection). Real credential values are never committed to Git or printed in documentation.

### B. Firebase Project Targeting

Production deployment commands must use explicit project targeting:

```bash
--project farmconnectprices
```

- Explicit project targeting makes deployments deterministic and avoids reliance on interactive CLI selection or implicit active project aliases.
- The `.firebaserc` file is optional when every deployment command explicitly specifies `--project farmconnectprices`. An operator is not required to create `.firebaserc`.

### C. Hosting-Only Deployment Procedure

Production deployments must deploy Firebase Hosting only. **Do not run a broad `firebase deploy` command**, which would inadvertently deploy Firestore rules, indexes, or functions.

```bash
# 1. Navigate to frontend and build production bundle
cd frontend
npm ci
npm run lint
npm test
npm run build
cd ..

# 2. Deploy Hosting only with explicit project targeting
firebase deploy --only hosting --project farmconnectprices
```

### D. Preview Environment Isolation

When testing pull requests or staging changes:

- **Preview Isolation Requirement**: Preview deployments must **NOT** use production Firebase Authentication, production Firestore, production backend credentials, or production user data.
- **Dedicated Preview Project**: A separate preview project (e.g. `farmconnectprices-preview`) and isolated backend environment must be established before preview channel deployments are enabled.
- **Strict Prohibition**: Operators and CI workflows must **never** deploy preview channels into the production `farmconnectprices` project.

---

## 5. Firebase Security Rules & Indexes Deployment

When database schema or rule updates are separately approved:

1. **Deploy Firestore Rules Only**:
   ```bash
   firebase deploy --only firestore:rules --project farmconnectprices
   ```
2. **Deploy Firestore Indexes Only**:
   ```bash
   firebase deploy --only firestore:indexes --project farmconnectprices
   ```
3. **Register Authorized Domains**:
   - In [Firebase Console](https://console.firebase.google.com/) -> **Authentication** -> **Settings** -> **Authorized Domains**, verify `farmconnectprices.web.app` and `farmconnectprices.firebaseapp.com` are authorized.

---

## 6. Post-Deployment Smoke Verification

Following deployment, execute these smoke checks:

1. **Backend Liveness via Hosting Rewrite**:
   - Request `GET https://farmconnectprices.web.app/api/v1/health`
   - Verify: HTTP 200 OK `{"status":"UP","service":"farmlink-api"}`.
2. **Frontend SPA Navigation**:
   - Access `https://farmconnectprices.web.app/`
   - Verify landing page renders cleanly without layout shifts or console errors.
3. **Deep Route Revalidation**:
   - Directly refresh `https://farmconnectprices.web.app/auth/login`
   - Verify: Serves `index.html` (HTTP 200) without 404.
4. **Unknown Route 404 Handling**:
   - Access `https://farmconnectprices.web.app/non-existent-page`
   - Verify: Renders FarmLink NotFoundPage (404) without redirecting to unrelated dashboards.

---

## 7. Production Rollback Procedure

If a production frontend deployment causes regressions, use the official Firebase Console release rollback:

### Primary Rollback Procedure (Firebase Console)
1. Open the [Firebase Console](https://console.firebase.google.com/) and navigate to project `farmconnectprices`.
2. Select **Hosting** from the Build menu.
3. In the **Release History** table, locate the previous known healthy release.
4. Click the three-dot overflow menu (`...`) next to the target release.
5. Select **Roll back**.
6. Firebase Hosting instantaneously switches CDN traffic back to the selected release without requiring a rebuild or code re-upload.

> [!WARNING]
> - Do **not** use obsolete or unsupported CLI commands such as `firebase hosting:releases:rollback` (unsupported by the Firebase CLI).
> - Do **not** use `git revert HEAD` as an automatic generic rollback procedure, as rebuilding and redeploying from source takes longer than instant CDN release rollback and risks introducing new build discrepancies during an incident.
