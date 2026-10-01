# Process Log — salary-management-system

Running notes on how this project actually came together, added as I go.

## 1. Problem statement
ACME's HR team manages salary data for 10,000 employees across multiple countries via
spreadsheets. Goal: a web-based tool for the HR Manager to manage that data and answer questions
about how the org pays people.

## 2. Requirement gathering
While going through the brief and figuring out exactly what needs to be built, ran into a few
things that weren't clear and would materially change the scope depending on how they're read:
- No time budget mentioned anywhere — a few hours, a weekend, or longer?
- "Fully functional deployed software" — does that mean a public URL, or is a Docker setup plus
  a demo video enough?
- Persona is just "HR Manager" (singular) — does that mean no login/authentication is needed?

Got it clarified from the hiring team.

## 3. Domain research
Started digging into how salary/compensation data is usually
modeled in HR systems, and what kind of pay-related questions and reports HR teams actually
care about — to make sure the requirements and data model are grounded in something real, not
just guessed.

## 4. Planning
With scope settled, moved on to deciding architecture, data model, tech stack, and the order of
implementation — covered in `design-notes.md`.(Planned it using AI, tradeoffs and thought process are explained in appropriate places)

## 5. Data modelling
Checkout `data_modelling.md`

## 6. Feature Building
Started implementing backend and frontend features step by step:

- **Core Backend & Data Seeding:**
  - Set up `Employee` and `SalaryRecord` entities with Spring Data JPA. Used H2 for local development to keep things fast and zero-config.
  - Wrote a seeder (`SalaryRecordDataSeeder`) to populate 10,000 employees across 5 countries (US, IN, GB, DE, SG) and departments, generating around 20,000 salary records with initial pay and annual merit reviews.
  - Implemented `EmployeeService` and `SalaryRecordService` with transactional methods so creating an employee and their initial salary happens in one atomic transaction.
  - Added input validation rules for positive salary amounts and matching local currency to employee country.

- **Frontend Pages (Angular):**
  - Built standalone Angular components with Material components (`mat-table`, `mat-paginator`, `mat-sort`, `mat-form-field`).
  - **Employee Directory:** Server-side search, filtering by country/department/status, and pagination so 10,000 rows don't choke the browser.
  - **Employee Profile:** Shows employee details, current active pay, and full salary timeline. Added the form to add new salary records.
  - **Salary Records:** Organization-wide ledger of all 20,000+ salary events with date filters and current-only toggle.
  - **Compensation Dashboard:** High-level analytics cards, country/department tables, outlier detection (highest/lowest paid per department), and a salary distribution histogram with USD bands. Used `forkJoin` in Angular to fetch all 5 report endpoints in parallel instead of one after another.

## 7. Testing
- Added unit tests for business validation rules and currency checks.
- Wrote `@DataJpaTest` repository tests for interval filtering and active-salary queries.
- Added `@SpringBootTest` integration tests with `MockMvc` to test API endpoints, pagination, and date range search parameters.

## 8. Dockerization & Deployment
- Set up `Dockerfile` for backend and frontend, and a `docker-compose.yml` linking backend, frontend, and a PostgreSQL database.
- Used Nginx in the frontend container to serve the compiled Angular app, handle SPA client-side routing rewrites (`try_files $uri $uri/ /index.html`), and reverse-proxy `/api/` calls to the backend.
- Deployed the project to Railway (backend + frontend + PostgreSQL).
- Tuned JVM memory flags (`-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC`) in the backend Dockerfile so the container doesn't hit memory limits on free cloud tiers.

## 9. Real Issues Faced & How I Fixed Them
Ran into a few real-world problems during and after deployment:

1. **CORS 403 on POST requests:**
   After deploying to Railway, GET requests worked fine, but POST requests (like adding an employee or adding a salary record) failed with 403 Forbidden. The backend `WebCorsConfig` only had localhost whitelisted. Fixed it by allowing all origin patterns (`.allowedOriginPatterns("*")`), which unblocked all POST forms.

2. **PostgreSQL Type Inference and Null Parameter Casting:**
   Queries that worked fine on H2 locally started throwing errors on PostgreSQL in production.
   - For string search parameters, PostgreSQL threw errors when trying to run `lower()` or `concat()` on null parameters. Fixed by casting parameters with `cast(:param as string)`.
   - For date filters on salary records (`effectiveFrom`, `effectiveTo`), passing null dates caused PostgreSQL to complain with `could not determine data type of parameter` or `cannot cast type bytea to date`. Fixed by safely casting via string: `(cast(:param as string) is null or sr.effectiveDate >= cast(cast(:param as string) as date))`. This made the date queries work cleanly on both local H2 and deployed PostgreSQL.

3. **Salary Records Filter UI and Linking:**
   - In Salary Records, clicking an employee's name originally didn't navigate to their profile. Added router links to open their profile directly, preserving the navigation state so clicking back returns to the exact search state.
   - Reordered the filter controls to make logical sense: `Effective from` -> `Effective to` -> `Current salary only` checkbox, and renamed the label from "Effective start through" to "Effective to".
   - Contextual filtering on the dashboard histogram: made it clear with explicit salary bands (e.g. $0-$15k, $15k-$30k) and added country-level inspection.

4. **Relocated Page-Level Filters on Dashboard:**
   - Moved the `As-of date` and `Include inactive` filters from inside the histogram card up to the top header (`.controls-card`) to clearly establish that they control all reports across the entire dashboard, while keeping histogram-specific controls (`Bands` and `Inspect country`) inside the distribution card.

## 10. Links & Demo Video
- **Live Cloud Application (Railway):** https://salary-frontend-production.up.railway.app
- **Demo Video Walkthrough (8 min):** https://youtu.be/shFp-NOKtfU


