# Local Development & Firestore Emulator Guide

This guide describes how to run FarmConnectPrices locally with Firebase and Cloud Firestore.

## Environment Setup

### 1. Frontend Configuration (`frontend/.env`)

Copy `frontend/.env.example` to `frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8080

VITE_FIREBASE_API_KEY=your-api-key
VITE_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=farmconnectprices-dev
VITE_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=123456789
VITE_FIREBASE_APP_ID=1:123456789:web:abcdef

# Enable Firestore Emulator for local frontend dev (Optional)
VITE_USE_FIREBASE_EMULATOR=true
VITE_FIREBASE_EMULATOR_HOST=localhost
VITE_FIREBASE_EMULATOR_PORT=8081
```

### 2. Backend Configuration (`application.yml` or Environment Variables)

The Spring Boot backend uses `app.firebase` configuration. For local development or testing with the emulator:

```bash
# Optional Environment Variables for Backend
FIREBASE_PROJECT_ID=farmconnectprices-dev
FIREBASE_USE_EMULATOR=true
FIRESTORE_EMULATOR_HOST=localhost:8081
```

---

## Running Firebase Local Emulator (Optional)

If Java & Firebase CLI are installed on your machine:

1. Install Firebase CLI (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. Start the Firestore Emulator:
   ```bash
   firebase emulators:start --only firestore
   ```

   - **Firestore Emulator Port:** `8081` (chosen specifically to avoid conflict with Spring Boot on port `8080`)
   - **Emulator UI:** `http://localhost:4000`

---

## Verification Endpoints & Commands

- **Backend Health Check:**
  ```http
  GET http://localhost:8080/api/v1/health
  ```
  Response: `{"status": "UP", "service": "farmlink-api"}`

- **Backend Firebase Health Diagnostic:**
  ```http
  GET http://localhost:8080/api/v1/health/firebase
  ```
  Response: `{"status": "UP", "service": "farmlink-api", "firebase": "EMULATOR_CONNECTED"}` (or `"CONFIGURED"`)
