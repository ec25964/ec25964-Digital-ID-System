# Digital ID System

A console-based backend system for managing digital identities used by multiple organisations. The system follows a federated model: a single Central Authority issues and manages Digital IDs, while consuming organisations (e.g., tax services, banks) verify them and hold their own domain-specific data.

**Repository:** <https://github.com/ec25964/ec25964-Digital-ID-System>

## Running the system

### Prerequisites

- Java 21
- Maven 3.6+

### Run

From the project root:

```bash
mvn exec:java
```

On first run, the system seeds `data/` with a starter set of Digital IDs and audit log entries copied from `sample-data/`, so verification and audit-log queries work immediately. Subsequent runs preserve the contents of `data/`.

### Run the tests

```bash
mvn test
```

Test count is reported at the end. JaCoCo coverage report is generated at `target/site/jacoco/index.html`.

## System structure

The codebase follows a layered architecture. Each layer depends only on the layer below it.

```
CLI Layer       cli/CliController,
                cli/MenuRenderer
                          ↓
Service Layer   service/IdentityManagementService,
                service/IdentityVerificationService,
                service/AuditService,
                service/OrganisationRegistry
                          ↓
Persistence     persistence/DigitalIdRepository,
                persistence/AuditRepository,
                persistence/CsvParser,
                persistence/CsvWriter
                          ↓
Data Files      data/ids.csv,
                data/audit.csv
```

### Main components

| Component | Role |
|---|---|
| `Main` | Wires the dependency graph and starts the CLI. Seeds `data/` from `sample-data/` on first run. |
| `CliController` | Drives the interactive session: organisation selection, command loop, dispatch to services. Holds no business logic. |
| `MenuRenderer` | Renders text menus appropriate to the selected organisation's role. |
| `IdentityManagementService` | Identity *management* capabilities — create, update attribute, change status. Central Authority only. |
| `IdentityVerificationService` | Identity *consumption* capabilities — verify, period-aware verify. Read-only with respect to Digital ID state. |
| `AuditService` | Records audit events and returns audit history. Authorisation-checked queries (Central Authority only). |
| `OrganisationRegistry` | Holds the configured organisations and their per-organisation accessible-attribute sets. |
| `DigitalIdRepository` / `AuditRepository` | CSV-backed persistence. Append-only for audit. |
| `CsvParser` / `CsvWriter` | Quote-aware CSV utilities supporting fields containing commas and double quotes (RFC 4180 style escaping). |
| `exception/*` | Domain-specific exception hierarchy: `DigitalIdException` base, with `AuthorisationException`, `NotFoundException`, `ValidationException`, `IllegalTransitionException`. |

### Key data model

- **`DigitalId`** — id (UUID, immutable), first name, last name, date of birth (immutable), address, nationality, email, status.
- **`IdStatus`** — `ACTIVE`, `SUSPENDED`, `REVOKED`. Transitions encoded explicitly; `REVOKED` is terminal.
- **`Organisation`** — name, type (Central Authority or Consuming Organisation), accessible-attribute set, period-verification capability flag.
- **`AuditEntry`** — event type, Digital ID, organisation, timestamp, details.
- **`VerificationResult`** — Digital ID, current status, filtered attribute map.
- **`PeriodVerificationResult`** — Digital ID, date range, continuously-active boolean, status events that fell in the period.

## Continuous integration

GitHub Actions runs `mvn clean test` on every pull request and on every commit reaching `main`. The workflow is defined in `.github/workflows/ci.yml`. The repository has branch protection rules: direct pushes to `main` are blocked, and a change can only be merged after CI passes on its pull request.

## Design notes

- **Layered architecture** with no upward dependencies. Each layer is testable in isolation.
- **Identity management and consumption are separated by class.** `IdentityManagementService` handles create/update/status writes; `IdentityVerificationService` handles reads. The brief specifies these capabilities should be handled separately, and the class boundary makes that explicit.
- **Federated identity model**: the central system holds only the identity baseline. Domain-specific data (e.g., licence points, tax codes) is assumed to live with the organisation that uses it, keyed by Digital ID.
- **Per-organisation attribute permissions** rather than role-based access: each organisation declares the specific attributes it can see during verification. Demonstrates data minimisation at the architectural level.
- **Privileged period verification.** Only Central Authority and Tax Authority can call `verifyContinuousActivity`, because revealing historical status could enable discriminatory uses by orgs that don't have a regulated need for it.
- **Date format separation**: dates are stored and passed between layers in ISO 8601 (yyyy-MM-dd); the CLI converts to and from `dd/MM/yyyy` at the boundary so the user sees UK format without leaking presentation concerns into the service layer.
- **Quote-aware CSV** so legitimate user input containing commas (e.g., a multi-part address) survives being written to CSV and read back unchanged.
- **REVOKED is terminal** for both status transitions and attribute updates. SUSPENDED is reversible and still allows attribute updates.
- **Domain-specific exception hierarchy** so rejections are not just consistent in message but in type. The CLI catches the base `DigitalIdException` for uniform error display while preserving the specific subclass for tests and any future caller that wants to distinguish kinds of failure.
- **Fail-fast preflight in the CLI** for operations that take an ID followed by further input — if the ID is missing or in a non-alterable state, the CLI bails before collecting more input.
- **Sample data seeding**: `sample-data/` is committed; runtime data lives in `data/` (gitignored). On first run `Main` copies missing files from one to the other, so the system has realistic content for demo without polluting version control with session data.

## Project layout

```
.
├── README.md
├── pom.xml
├── .github/workflows/ci.yml
├── sample-data/                          ← committed seed data
│   ├── ids.csv
│   └── audit.csv
├── data/                                 ← runtime data, gitignored
│   ├── ids.csv
│   └── audit.csv
├── src/main/java/com/github/ec25964/
│   ├── Main.java
│   ├── cli/
│   ├── exception/
│   ├── model/
│   ├── persistence/
│   └── service/
└── src/test/java/com/github/ec25964/
    ├── cli/
    ├── model/
    ├── persistence/
    └── service/
```
