# Design & Architecture Notes

## Tech Stack

| Layer | Choice | Reasoning |
|---|---|---|
| Backend | Java 17 + Spring Boot 3 | Matches the role (Java Backend / Software Craftsperson). Spring Data JPA + Spring MVC give a fast, well-tested path to a layered REST API. |
| Build tool | Gradle (Kotlin DSL) | Listed as an accepted option in the job posting; modern default tooling for new Spring Boot projects. |
| Database | SQLite (dev), PostgreSQL (deployed) | SQLite is zero-ops for local seeding and tests; the deployed instance uses Postgres since SQLite on ephemeral hosting disk risks data loss on redeploy. Schema is DB-agnostic via JPA + Flyway. |
| Frontend | Angular (standalone components) + TypeScript | The assessment specifies Angular for Java candidates, and the target role's job posting lists Angular/TypeScript as a hard requirement. |
| Component library | Angular Material (`mat-table`, `mat-paginator`, `mat-sort`, `mat-dialog`, `mat-form-field`) | Official Angular library, integrates cleanly with CDK data sources for paginating/sorting 10,000 rows without hand-rolled logic. |
| State/HTTP | RxJS + `HttpClient` via typed Angular services | RxJS is called out as a good-to-have in the job posting; used for employee list filtering/search and parallel dashboard report loading. |
| Testing (BE) | JUnit 5 + Mockito, `@SpringBootTest` + H2 for integration tests | Matches job posting's JUnit requirement; H2 in-memory DB keeps integration tests fast and deterministic. |
| Testing (FE) | Jasmine/Karma (Angular CLI default) | Standard Angular testing stack, no extra setup. |
| Deployment | Docker Compose (backend + frontend + DB), deployed to a public host (Render/Railway) | A public URL is a confirmed requirement; Docker Compose keeps the setup reproducible and portable across hosts. |

## Data Model

Checkout `data_modelling.md`


## API Contract (v1)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/employees?page=&size=&sort=&search=&country=&department=` | Paginated, filterable, sortable employee list |
| GET | `/api/employees/{id}` | Single employee + current salary |
| POST | `/api/employees` | Create employee (with initial salary) |
| PUT | `/api/employees/{id}` | Update employee profile fields |
| POST | `/api/employees/{id}/salary` | Add new salary record (closes previous one) |
| GET | `/api/reports/salary/by-country` | Avg/median salary + headcount per country |
| GET | `/api/reports/salary/by-department` | Avg/median salary + headcount per department |
| GET | `/api/reports/salary/distribution?bucketSize=` | Histogram buckets org-wide or filtered |
| GET | `/api/reports/salary/outliers?department=` | Top/bottom N paid employees per department |
| GET | `/api/reports/summary` | Org-wide headcount, total payroll cost, overall avg/median |

All list/report endpoints return paginated or aggregated JSON, never the full 10,000-row set in
one payload.

## Angular Frontend Structure

```
src/app/
  core/
    services/
      employee.service.ts       // HttpClient + RxJS wrappers for /api/employees
      report.service.ts         // HttpClient + RxJS wrappers for /api/reports/*
    models/
      employee.model.ts
      salary-record.model.ts
      report.model.ts
  features/
    employee-list/
      employee-list.component.ts   // mat-table + mat-paginator + mat-sort, search/filter bar
    employee-detail/
      employee-detail.component.ts // profile + salary history + "add salary record" dialog
    employee-form/
      employee-form.component.ts   // create/edit reactive form with validation
    dashboard/
      dashboard.component.ts       // report cards: by-country, by-department, distribution, outliers
  app.routes.ts                    // /employees, /employees/:id, /employees/new, /dashboard
  app.config.ts                    // provideHttpClient, routing, Material theming
```

- `employee-list.component` combines filter state and paginator/sort events via RxJS
  (`combineLatest`, debounced search) to call the backend only when needed.
- `dashboard.component` fetches all report endpoints in parallel with `forkJoin`.
- Server-side pagination is used throughout — client-side paging of 10,000 rows in the browser
  is unnecessary overhead once a real API exists.

## Seeding Strategy
A Spring Boot `CommandLineRunner` (profile-gated, e.g. `--spring.profiles.active=seed`) inserts
10,000 employees in batches (e.g. batch size 500 via `saveAll`) across 6-8 countries and 5-6
departments, with salary drawn from a country/department-appropriate range plus random variance.

## Trade-offs Log
- **SQLite locally, Postgres in production:** fast local iteration without giving up a real
  deployed database.
- **Server-side pagination over client-side:** correct choice at 10k rows.
- **Versioned salary records over a mutable salary field:** more modeling work up front, but
  avoids a costly migration later if salary history becomes a real requirement.
- **No auth for v1:** confirmed out of scope by the hiring team, not an oversight.
