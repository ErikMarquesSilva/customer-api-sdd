# STATE

## Decisions

### AD-001
- **Decision**: Every test that touches the database runs against real PostgreSQL started by Testcontainers (`@ServiceConnection`); the H2 dependency is removed.
- **Reason**: The grouping query uses PostgreSQL-specific SQL (`array_agg ... ORDER BY`), and the uniqueness race (CUST-12), optimistic locking (CUST-24) and migrations (CUST-47) only prove anything on the real engine. Chosen by the user on 2026-09-22.
- **Trade-off**: Tests need a running Docker daemon and are slower than an in-memory database.
- **Scope**: All features, all integration tests.
- **Date**: 2026-09-22
- **Status**: active

### AD-002
- **Decision**: The schema is owned by versioned Flyway migrations (`src/main/resources/db/migration`); Hibernate runs with `ddl-auto=validate`.
- **Reason**: Reviewable, repeatable schema changes; Hibernate auto-DDL cannot express constraints and indexes deliberately.
- **Trade-off**: Every entity change needs a hand-written migration.
- **Scope**: All features with persistence.
- **Date**: 2026-09-22
- **Status**: active

### AD-003
- **Decision**: One `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` produces every error as RFC 9457 `application/problem+json`, with an `errors` array of `{field, message}` for validation and uniqueness failures and a fixed detail for unexpected 500s.
- **Reason**: A single error contract for clients (CUST-35 to CUST-39); reuses Spring's built-in handling of framework exceptions.
- **Trade-off**: New endpoints must throw the shared domain exceptions instead of building their own responses.
- **Scope**: All HTTP endpoints.
- **Date**: 2026-09-22
- **Status**: active

### AD-004
- **Decision**: Code is packaged by feature (`com.example.customerapi.customer`); controllers accept and return DTO records only, JPA entities never leave the service layer, and controllers hold no business rules.
- **Reason**: Requested explicitly (no entity exposure, thin controllers); keeps the HTTP contract independent of the persistence model.
- **Trade-off**: Mapping code between entity and DTOs.
- **Scope**: All features.
- **Date**: 2026-09-22
- **Status**: superseded by AD-005

### AD-005
- **Decision**: Each feature package is hexagonal: `domain` (no framework imports), `application.port.in` (use-case interfaces), `application.port.out` (persistence interfaces), `application.service` (use cases, which may use Spring stereotypes, `@Transactional` and Spring Data Commons `Page`/`Pageable`/`Sort`), `adapter.in.web` and `adapter.out.persistence`. ArchUnit tests enforce the dependency rules. Controllers still use DTO records only and hold no business rules (kept from AD-004).
- **Reason**: The user asked for hexagonal architecture with a pragmatic core. It isolates business rules from JPA and the web layer, and ArchUnit makes violations fail the build instead of relying on review.
- **Trade-off**: More types (ports, a separate JPA entity, mappers). The application layer stays coupled to Spring transactions and Spring Data Commons paging types.
- **Scope**: All features, all packages under `com.example.customerapi`.
- **Date**: 2026-09-22
- **Status**: active

## Handoff

- **Feature**: hexagonal-architecture (`.specs/features/hexagonal-architecture/`)
- **Phase / Task**: Execute. T1-T17 are complete. T16 was added after the user chose option (b) on 2026-09-23. T17 applies round 4's suggestion under the user's delegated Staff authority.
- **Completed**: T1-T17; 302 tests pass on a clean build.
- **In-progress** (file:line): none
- **Next step**: focused verification 5. On PASS, push `refactor/hexagonal` and open a PR against `feat/customer-api`. On any new plausible survivor, escalate to the user with no further fix round.
- **Blockers**: none
- **Uncommitted files**: none after this commit
- **Branch**: `refactor/hexagonal` (local, not pushed)
