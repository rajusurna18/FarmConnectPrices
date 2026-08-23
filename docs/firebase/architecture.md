# FarmConnectPrices — Firebase & Firestore Architecture

This document describes the foundational Firebase & Cloud Firestore architecture for FarmConnectPrices.

## Architecture Overview

```text
                    FarmConnectPrices
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
       React Frontend             Spring Boot API
              │                         │
       Firebase Web SDK          Firebase Admin SDK
              │                         │
              └────────────┬────────────┘
                           ▼
                     Cloud Firestore
```

## Client vs Backend Access

- **React Frontend (Firebase Web SDK):**
  - Used for real-time Firestore listeners, frontend UI state syncing, and direct read/write operations guarded by Firestore Security Rules.
  - Centralized initialization in `frontend/src/config/firebase.ts`.
  - Typed generic helpers provided in `frontend/src/services/firebase/firestore.ts`.

- **Spring Boot Backend (Firebase Admin SDK):**
  - Used for administrative tasks, server-side data processing, batch updates, and secure backend operations with full Firestore privileges.
  - Configuration managed in `com.farmlink.api.config.firebase.FirebaseConfig`.
  - Properties bound securely via `app.firebase` prefix in `application.yml`.

## Security Boundaries & Rules Policy

- **Default Policy:** Default-Deny (`allow read, write: if false;` in `firestore.rules`).
- **Authorization:** Future feature modules will introduce collection-specific rules enforcing user authentication and role-based access.
- **Environment Isolation:** Local development supports connecting to the Cloud Firestore Emulator (port `8081`) to avoid accidental production data mutation.
