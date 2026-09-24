# Employee Salary Management System — Requirements Document

## Goal
Replace ACME's spreadsheet-based salary tracking with a web-based tool that lets the HR Manager
view, manage, and analyze salary data for 10,000 employees across multiple countries, and answer
questions about how the organization pays people.

## User
Single persona: HR Manager, assumed to be the sole actor using the system.

## In Scope

### Employee & Salary Management
- Paginated, sortable, searchable/filterable employee list (by name, country, department,
  employee ID).
- Single employee profile view with full salary detail.
- Add a new employee with salary information.
- Edit an existing employee's salary — creates a new versioned salary record rather than
  mutating history, keeping past pay auditable.
- Validation: required fields, positive salary values, valid country/currency, unique employee
  IDs.

### Reporting
- Average and median salary by country.
- Average and median salary by department.
- Salary distribution / band breakdown, org-wide and per country.
- Highest and lowest paid employees per department.
- Headcount and total payroll cost by country and by department.

### Data
- Seed script generating 10,000 synthetic employees across multiple countries, departments, and
  salary bands.

### Engineering Quality
- REST API (Spring Boot) with layered architecture (controller/service/repository).
- In-memory H2 database for local development and testing. The deployed demonstration will use a persistent relational database.
- Angular + Angular Material frontend consuming the API.
- Unit tests for salary calculation/aggregation logic and core services.
- Integration tests for key REST endpoints.
- Dockerized backend, frontend, and database for .one-command reproducible setup

## Explicitly Out of Scope

Can be included but, it is either unnecessary or too much for the scope .

| Excluded feature | Reason for exclusion                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Multi-role access (RBAC/SSO) | The assessment defines one persona: the HR Manager. Adding identity-provider integration and role-based authorization would introduce authentication configuration, authorization rules, and additional security tests without strengthening the core employee, salary-history, or reporting flows. The initial version therefore assumes a single trusted operator; access control can be added if the product gains more user types. |
| Live multi-currency FX conversion | Live conversion would add an external rate provider, network-failure handling, rate timestamping, caching, and decisions about which historical rate applies to each salary. Those moving inputs would also make reports harder to reproduce and test. Cross-country reporting will use a normalized reporting currency without live rates, so the conversion assumption must remain explicit and stable for a given report.                           |
| Payroll processing, tax, and statutory compliance | Payroll calculations depend on jurisdiction-specific tax, benefits, deductions, and regulatory rules that change over time. Implementing them correctly would require a separate rules model, regular updates, and extensive validation. This application is limited to recording salary amounts and analyzing them; it does not calculate or issue pay.                                                                                               |
| Bulk Excel import/export | Spreadsheet workflows require column mapping, format and locale handling, row-level validation, duplicate detection, partial-failure reporting, and safe retry behavior. That is a separate data-ingestion/export surface. It is excluded so implementation and testing can focus on the core REST operations, salary-history integrity, and reports.                                                                                                  |
| Dedicated audit-log UI | Salary changes are retained as versioned salary records, providing the underlying history needed for traceability. A separate audit interface would require additional history views, filtering, and access patterns; it is deferred while the employee and salary-record workflows are established.                                                                                                                                                   |
| Notifications and approval workflows | These require workflow states, approver identity and permissions, notification delivery, retries, and rules for effective dates and rejected changes. None is needed for the assessment's single-user flow, and adding them would expand the domain model and test matrix beyond salary management and reporting.                                                                                                                                      |


## Success Criteria
The HR Manager can load the app, browse and search 10,000 employees without noticeable lag, add
or update salary records, and use a dashboard to answer how the organization pays people across
countries and departments — without touching a spreadsheet.
