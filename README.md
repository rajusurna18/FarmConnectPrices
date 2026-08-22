# FarmConnectPrices

**Tagline:**  
*Real Prices. Better Markets. Smarter Decisions.*

---

## Project Vision

FarmConnectPrices is an AI-powered agricultural market intelligence and marketplace platform designed to help farmers access reliable market prices, compare markets, make better selling decisions, and connect directly with buyers and agricultural services.

---

## Technology Stack

- **Frontend:** React, TypeScript, Vite, Tailwind CSS, React Router, Axios, TanStack Query, ESLint
- **Backend:** Java 21, Spring Boot (3.3.4), Maven, Spring Web, Spring Validation, Spring Security, Spring Boot Actuator
- **Platform Preparation:** Firebase Integration placeholders (Authentication, Firestore, Storage, Messaging)
- **AI Infrastructure:** Reserved for Python / FastAPI in future modules

---

## Repository Structure

```text
FarmConnectPrices/
├── frontend/             # React + TypeScript + Vite application
├── backend/              # Spring Boot Java 21 Maven application
├── ai/                   # Reserved for Python/FastAPI AI services
├── docs/                 # Project documentation
├── scripts/              # Helper scripts
│
├── .gitignore            # Git exclusion rules
├── README.md             # Project documentation
└── LICENSE               # License statement
```

---

## Prerequisites

- **Java Development Kit:** Java 21 LTS
- **Node.js Environment:** Node.js v18+ (v24.x recommended)
- **Package Manager:** npm (v10+ / v11+)
- **Git Version Control:** Git 2.x

---

## Environment Variables

Copy `frontend/.env.example` to `frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8080

VITE_FIREBASE_API_KEY=
VITE_FIREBASE_AUTH_DOMAIN=
VITE_FIREBASE_PROJECT_ID=
VITE_FIREBASE_STORAGE_BUCKET=
VITE_FIREBASE_MESSAGING_SENDER_ID=
VITE_FIREBASE_APP_ID=
```

---

## Firebase Prerequisites (Future Integration)

Firebase services will be activated in future modules:
- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- Firebase Cloud Messaging

*Note: Environment placeholders are configured. Do not commit secret keys or service account JSON files.*

---

## Development Setup & Commands

### Backend Setup (Spring Boot)

Navigate to the `backend/` directory:

```bash
cd backend
```

- **Run Maven Verify & Test:**
  ```powershell
  .\mvnw.cmd clean verify
  ```

- **Start Backend Development Server:**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

The backend server starts at `http://localhost:8080`.

---

### Frontend Setup (Vite + React)

Navigate to the `frontend/` directory:

```bash
cd frontend
```

- **Install Dependencies:**
  ```bash
  npm install
  ```

- **Lint Codebase:**
  ```bash
  npm run lint
  ```

- **Production Build:**
  ```bash
  npm run build
  ```

- **Start Frontend Development Server:**
  ```bash
  npm run dev
  ```

The frontend server starts at `http://localhost:5173`.

---

## Health API Verification

### Custom Application Health API

```http
GET http://localhost:8080/api/v1/health
```

**Expected Response (HTTP 200 OK):**

```json
{
  "status": "UP",
  "service": "farmlink-api"
}
```

### Spring Boot Actuator Health API

```http
GET http://localhost:8080/actuator/health
```

**Expected Response (HTTP 200 OK):**

```json
{
  "status": "UP"
}
```
