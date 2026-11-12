# FarmConnectPrices — Production Deployment Guide
## Multi-Tier Architecture Deployment Runbook

---

## 1. Overview & Architecture

**FarmConnectPrices (FarmLink AI)** uses a decoupled cloud architecture:
- **Frontend SPA**: React 19 + TypeScript + Vite, deployed on a static CDN (Vercel, Firebase Hosting, or Cloudflare Pages).
- **Backend API**: Java 21 LTS + Spring Boot 3.3.4, packaged as a container image and hosted on a managed container PaaS (Render, Google Cloud Run, or AWS App Runner).
- **Identity & Data**: Firebase Authentication and Google Cloud Firestore.

```
[Browser Client] ---> [Frontend CDN (Vercel / Firebase Hosting)]
                            │
                            │ HTTPS + Bearer JWT
                            ▼
                      [Backend PaaS (Render / Cloud Run)]
                            │
                            │ Google Cloud Admin SDK / gRPC
                            ▼
                      [Google Cloud Firestore & Firebase Auth]
```

---

## 2. Prerequisites & Cloud Accounts

1. **Google Cloud / Firebase Project**:
   - Access to [Firebase Console](https://console.firebase.google.com/) for project `farmconnectprices`.
   - Upgrade project to **Blaze Plan (Pay-as-you-go)** to ensure high-concurrency read availability beyond the 50k reads/day free tier.
   - Configure a budget alert (e.g. \$10.00/month) in Google Cloud Billing.
2. **Backend PaaS Account**:
   - Render (Web Service), Google Cloud Run, or AWS App Runner.
3. **Frontend CDN Account**:
   - Vercel, Firebase Hosting, or Cloudflare Pages.

---

## 3. Step-by-Step Backend Deployment (Render / Cloud Run)

### A. Environment Variables Required

Configure these environment variables in your container hosting dashboard:

| Variable Name | Required | Example / Description |
| :--- | :---: | :--- |
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` (activates `application-prod.yml`) |
| `PORT` | Yes | `8080` (or injected automatically by PaaS) |
| `FIREBASE_PROJECT_ID` | Yes | `farmconnectprices` |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Yes | Complete raw JSON content of your Google Cloud service account key |
| `DISPUTE_CURSOR_SECRET` | Yes | Random 256-bit secret string (min 32 characters) for HMAC pagination signing |
| `APP_CORS_ALLOWED_ORIGINS` | Yes | Comma-separated allowed frontend domains, e.g. `https://farmconnectprices.vercel.app,https://farmconnectprices.com` |
| `FIRESTORE_QUOTA_COOLDOWN_MS` | No | `60000` (circuit breaker cooldown in ms, default 60s) |

### B. Deployment via Dockerfile
1. Link your Git repository to your container PaaS.
2. Select **Dockerfile** as the build runtime (`backend/Dockerfile` as the Dockerfile path, or set Root Directory to `backend`).
3. Set Health Check path to `/api/v1/health` or `/actuator/health`.
4. Deploy the service. Verify response:
   ```bash
   curl -i https://<your-backend-domain>/api/v1/health
   # Response: HTTP 200 OK
   # {"status":"UP","service":"farmlink-api"}
   ```

---

## 4. Step-by-Step Frontend Deployment (Vercel / Firebase Hosting)

### Option A — Deploying to Vercel (Recommended)
1. Import repository into Vercel Dashboard.
2. Set **Root Directory** to `frontend`.
3. Configure **Build Settings**:
   - Framework Preset: `Vite`
   - Build Command: `npm run build`
   - Output Directory: `dist`
4. Configure **Environment Variables** in Vercel Project Settings:
   - `VITE_API_BASE_URL`: `https://<your-backend-domain>`
   - `VITE_FIREBASE_API_KEY`: Production Firebase Web API Key
   - `VITE_FIREBASE_AUTH_DOMAIN`: `farmconnectprices.firebaseapp.com`
   - `VITE_FIREBASE_PROJECT_ID`: `farmconnectprices`
   - `VITE_FIREBASE_STORAGE_BUCKET`: `farmconnectprices.firebasestorage.app`
   - `VITE_FIREBASE_MESSAGING_SENDER_ID`: Production Sender ID
   - `VITE_FIREBASE_APP_ID`: Production Firebase App ID
   - `VITE_USE_FIREBASE_EMULATOR`: `false`
5. Deploy. The committed `frontend/vercel.json` automatically configures SPA routing rewrites and production security headers.

### Option B — Deploying to Firebase Hosting
1. Build frontend bundle:
   ```bash
   cd frontend && npm run build
   ```
2. Deploy hosting using Firebase CLI:
   ```bash
   firebase deploy --only hosting
   ```
   (Uses configuration in `firebase.json` with public folder `frontend/dist`).

---

## 5. Firebase Production Security Configuration

1. **Deploy Firestore Rules**:
   ```bash
   firebase deploy --only firestore:rules
   ```
2. **Deploy Firestore Composite Indexes**:
   ```bash
   firebase deploy --only firestore:indexes
   ```
3. **Register Authorized Domains**:
   - Open [Firebase Console](https://console.firebase.google.com/) -> **Authentication** -> **Settings** -> **Authorized Domains**.
   - Add your production frontend domain (e.g., `farmconnectprices.vercel.app` or `farmconnectprices.com`).

---

## 6. Post-Deployment Smoke Verification

1. **Backend Liveness**:
   - Access `GET /api/v1/health` -> verify HTTP 200 `{"status":"UP"}`.
   - Access `GET /api/v1/health/firebase` -> verify HTTP 200 with initialized Firebase status.
2. **Frontend SPA Navigation**:
   - Visit landing page -> verify 3D elements render and hero displays cleanly without layout shift.
   - Sign in as a `FARMER`, `MEDIATOR_BUYER`, and `CUSTOMER`.
   - Refresh a deep route (e.g. `/marketplace/disputes`) -> verify route renders directly without HTTP 404.
3. **End-to-End Marketplace Loop**:
   - Create an offer -> accept offer to create order -> submit payment intent -> complete delivery -> submit review -> test dispute reporting.
