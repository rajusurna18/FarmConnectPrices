# Backend Development Startup & Process Management Guide

## Overview
FarmConnectPrices consists of a Spring Boot Java backend (`backend/`) and a React TypeScript frontend (`frontend/`).

To avoid port conflicts such as:
```text
Web server failed to start. Port 8080 was already in use.
```
developers must ensure **exactly one backend instance** is running on port 8080.

---

## Standard Development Launch Sequence

### Terminal 1 — Spring Boot Backend (Port 8080)
```bash
cd backend
.\mvnw.cmd spring-boot:run
```

### Terminal 2 — React Frontend (Port 5173)
```bash
cd frontend
npm run dev
```

---

## Port 8080 Conflict Resolution

If port 8080 is blocked by an orphaned Java process:

### Windows (PowerShell)
```powershell
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process -Force
```

### Verification Health Check
After starting Spring Boot, verify backend health:
```bash
curl http://localhost:8080/actuator/health
```
Response:
```json
{"status":"UP"}
```
