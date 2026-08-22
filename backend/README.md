# FarmConnectPrices Backend API (`farmlink-api`)

The backend RESTful API service for **FarmConnectPrices**.

---

## Technical Stack & Requirements

- **Java:** JDK 21 LTS
- **Framework:** Spring Boot 3.3.4
- **Build Tool:** Apache Maven (Maven Wrapper included)
- **Key Modules:**
  - Spring Web
  - Spring Security (Prepared foundation)
  - Spring Boot Actuator
  - Spring Validation

---

## Package Architecture

```text
com.farmlink.api
├── config         # Security, CORS, Web configurations
├── controller     # REST API controllers
├── service        # Core business service logic
├── dto            # Data Transfer Objects
├── exception      # Global exception handlers
└── common         # Application constants and utility classes
```

---

## Build & Run Commands

### Windows (PowerShell)

- **Clean & Run Tests:**
  ```powershell
  .\mvnw.cmd clean verify
  ```

- **Run Server:**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

### Linux / macOS

- **Clean & Run Tests:**
  ```bash
  ./mvnw clean verify
  ```

- **Run Server:**
  ```bash
  ./mvnw spring-boot:run
  ```

---

## Health API Endpoints

- **Custom Application Health API:**
  ```http
  GET /api/v1/health
  ```
  Returns `HTTP 200 OK`:
  ```json
  {
    "status": "UP",
    "service": "farmlink-api"
  }
  ```

- **Actuator Operational Health Endpoint:**
  ```http
  GET /actuator/health
  ```
