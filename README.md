# ACME Employee Salary Management System

A full-stack compensation management and analytics application built with **Spring Boot 3 (Java 21)** and **Angular 19 (Angular Material)** to replace ACME’s spreadsheet-based tracking for **10,000 employees across 5 countries**.

---

## 🔗 Live Application & Demo Walkthrough

- **🌐 Live Cloud Application (Railway):** [https://salary-frontend-production.up.railway.app](https://salary-frontend-production.up.railway.app)
- **🎥 Video Demo Walkthrough (8 min):** [Watch on YouTube](https://youtu.be/shFp-NOKtfU)

---

## 📚 Project Documentation (`docs/`)

- 📖 **[`docs/PROCESS_LOG.md`](docs/PROCESS_LOG.md)** — Engineering Journey & Real Issues Solved (PostgreSQL casting, CORS, UI UX)
- 📐 **[`docs/data_modelling.md`](docs/data_modelling.md)** — Entity Relationships, Schemas & Design Trade-offs
- 🏗️ **[`docs/design-notes.md`](docs/design-notes.md)** — Architecture, Tech Stack Decisions & REST API Contracts

---

## Features & Capabilities

### 1. Employee & Compensation Management
- **Employee Directory (`/`)**: Server-side paginated, sortable, and filterable directory across 10,000 employees with search by name or employee ID (`ACME-00001`), country (`US`, `IN`, `GB`, `DE`, `SG`), department (`Engineering`, `Finance`, `Sales`, `People`, `Operations`, `Product`), and status (`ACTIVE` / `INACTIVE`).
- **Employee Onboarding (`POST /api/employees`)**: Creates a new employee profile together with their initial salary record in a single database transaction and returns `409 Conflict` on duplicate employee numbers.
- **Single Employee Profile (`/employees/:id`)**: Displays employment details, the salary record active as of today (`currentSalary`), the full auditable history of past/current/scheduled salary periods (`salaryHistory`), and an inline form to record a new versioned salary change.
- **Salary History & Search (`/salary-records`)**: Paginated search across all salary periods with filters for employee ID, country, department, currency, effective-date range, and a `currentOnly` toggle. Adding a new salary record automatically closes the preceding period on the day before the new effective date (`effectiveTo = newEffectiveDate - 1 day`).

### 2. Compensation Analytics & Reporting (`/dashboard`)
- **Country Salary Report (`GET /api/reports/countries`)**: Headcount, average salary, median salary, and total payroll per country in both local currency (`USD`, `INR`, `GBP`, `EUR`, `SGD`) and normalized reporting currency (`USD`).
- **Department Salary Report (`GET /api/reports/departments`)**: Headcount, average salary, median salary, and total payroll per department normalized to `USD`.
- **Salary Distribution Report (`GET /api/reports/distribution`)**: Organization-wide and per-country salary band breakdown across configurable `USD` band sizes (`$15,000`, `$25,000`, `$40,000`, `$50,000`, or custom thresholds).
- **Departmental Pay Extremes (`GET /api/reports/department-extremes`)**: Highest- and lowest-paid employees in each department ranked by normalized `USD` annual compensation, with links directly to each employee’s profile.

---

## Reporting & Salary-History Rules

1. **Current-Pay Selection (`asOfDate`)**:
   - Every report evaluates salary periods using the inclusive interval rule:
     $$\text{effectiveDate} \le \text{asOfDate} \quad \text{AND} \quad (\text{effectiveTo IS NULL} \;\lor\; \text{effectiveTo} \ge \text{asOfDate})$$
   - Superseded historical records and future-dated scheduled records are never counted alongside current pay.
2. **Normalized Reporting Currency (`USD`) & Deterministic FX Basis**:
   - Live FX rates are out of scope (`requirements.md`). Multi-country amounts are never summed or averaged without conversion.
   - Reports normalize compensation into **`USD`** using explicit, deterministic fixed conversion rates disclosed in both API responses and the dashboard banner:
     | Currency | Country | Fixed Conversion Rate to `USD` |
     |---|---|---|
     | `USD` | United States (`US`) | `1.0000 USD` |
     | `INR` | India (`IN`) | `0.0120 USD` |
     | `GBP` | United Kingdom (`GB`) | `1.2700 USD` |
     | `EUR` | Germany (`DE`) | `1.0800 USD` |
     | `SGD` | Singapore (`SG`) | `0.7400 USD` |
3. **Active vs. Inactive Employees**:
   - By default (`includeInactive=false`), reports include **only `ACTIVE` employees** so payroll totals and compensation medians reflect the active workforce.
   - An optional `includeInactive=true` query parameter (and dashboard toggle) allows the HR Manager to include `INACTIVE` employees when needed.

---

## Architecture & Data Model

- **Backend (`backend/`)**: Java 21, Spring Boot 3.5, Spring Data JPA, Jakarta Bean Validation, H2 (local dev & fast tests), PostgreSQL (Docker / Railway production deployment).
- **Frontend (`frontend/`)**: Angular 19 standalone components, Angular Material, RxJS.
- **Reverse Proxy**: Nginx container handling SPA routing, static asset serving, and `/api` reverse-proxying.

---

## Quick Start

### Option 1: One-Command Docker Setup (PostgreSQL + Backend + Frontend)

```bash
docker compose up --build
```

- **Frontend UI**: [http://localhost:4200](http://localhost:4200)
- **Backend REST API**: [http://localhost:8080/api/employees](http://localhost:8080/api/employees)
- **Database**: Persistent PostgreSQL 16 (`postgres-salary-data` volume). On first boot, `EmployeeDataSeeder` and `SalaryRecordDataSeeder` automatically seed 10,000 employees and 20,000 salary records.

### Option 2: Local Development (H2 In-Memory Database)

1. **Start the Spring Boot backend**:
   ```bash
   cd backend
   ./gradlew bootRun
   # Or in PowerShell: .\gradlew.bat bootRun
   ```
   The backend starts on `http://localhost:8080` with an in-memory H2 database and seeds 10,000 synthetic employees.

2. **Start the Angular frontend**:
   ```bash
   cd frontend
   npm install
   npm start
   ```
   Open `http://localhost:4200` (requests to `/api/*` are proxied to `http://localhost:8080` via `proxy.conf.json`).

---

## Running Tests & Production Build

### Backend Unit, Repository, and Integration Tests
```bash
cd backend
./gradlew test
# Or in PowerShell: .\gradlew.bat test
```

### Frontend Production Build
```bash
cd frontend
npm run build
```

---

## REST API Summary

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/employees` | Paginated, sortable, filterable employee directory (`q`, `countryCode`, `department`, `status`, `page`, `size`, `sortBy`, `direction`) |
| `POST` | `/api/employees` | Create a new employee with initial salary transactionally (`201 Created`, `409 Conflict` on duplicate ID) |
| `GET` | `/api/employees/{id}` | Single employee profile with `currentSalary` and `salaryHistory` |
| `PUT` | `/api/employees/{id}` | Update employee profile fields (name, department, country, job title, job level, date of joining, status) |
| `GET` | `/api/employees/{employeeNumber}/salary-records` | Ordered salary history for an employee |
| `POST` | `/api/employees/{employeeNumber}/salary-records` | Record a new salary period and close the adjacent prior period |
| `GET` | `/api/salary-records` | Paginated salary-record search (`currentOnly`, `countryCode`, `department`, `currencyCode`, `effectiveFrom`, `effectiveTo`) |
| `GET` | `/api/reports/countries` | Average/median salary, headcount, and payroll total by country (local + `USD`) |
| `GET` | `/api/reports/departments` | Average/median salary, headcount, and payroll total by department (`USD`) |
| `GET` | `/api/reports/distribution` | Organization-wide and per-country salary band distribution (`bandSize`, `countryCode`, `bands`) |
| `GET` | `/api/reports/department-extremes` | Highest- and lowest-paid employees per department (`department`, `limit`) |

---

## Cloud Deployment & Demo Walkthrough

### Production Deployment (Railway)
The live production application is deployed on **Railway** as a fully containerized multi-service architecture:
- **🌐 Live URL:** [https://salary-frontend-production.up.railway.app](https://salary-frontend-production.up.railway.app)
- **Architecture on Railway:**
  - **`salary-frontend`**: Angular SPA served through **Nginx** reverse proxy, handling client-side SPA routing (`try_files $uri $uri/ /index.html`) and proxying API traffic.
  - **`salary-backend`**: Spring Boot 3 Java 21 container running with JVM flags (`-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC`) to ensure memory stability on cloud tiers.
  - **`salary-postgres`**: Managed PostgreSQL database with persistent volume storage (`postgres-volume`) so employee records, salary history, and newly created profiles persist across redeploys.
  - **Profiles**: Production runs with `SPRING_PROFILES_ACTIVE=prod`, automatically applying PostgreSQL-compatible migrations and entity mapping.

### Local & Alternative Deployments
- **Local Dev**: In-memory **H2** database (`ddl-auto=create-drop`) for zero-configuration startup and fast JUnit tests.
- **Docker Compose**: Run `docker compose up --build -d` to spin up PostgreSQL, backend, and frontend locally.
- **Render**: Blueprint included in [`render.yaml`](render.yaml).

### 🎥 Demo Video Walkthrough
- **Full Walkthrough (8 min):** [Watch on YouTube](https://youtu.be/shFp-NOKtfU)
- Covers end-to-end user workflows, atomic transactions, date clamping for salary revisions, and high-performance dashboard analytics.

