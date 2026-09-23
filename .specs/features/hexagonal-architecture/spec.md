# Hexagonal Architecture Specification

## Problem Statement

The Customer API mixes business rules with framework code: `Customer` is both the domain model and a JPA entity, `CustomerService` builds JPA `Specification`s, and nothing stops a controller from calling the repository directly. Replacing the database or the web layer, or testing rules without Spring, means touching business code. Restructuring into ports and adapters isolates the rules and makes the dependency direction explicit and enforced.

## Goals

- [ ] The domain has no framework dependency; the application core talks to the outside only through ports.
- [ ] Dependency rules are enforced by ArchUnit tests that fail the build on violation.
- [ ] Every existing behavior (CUST-01 to CUST-53, GEO-001 to GEO-019) is unchanged, proven by the existing integration tests.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Any API contract change (paths, status codes, JSON fields, validation rules) | Pure refactor; behavior must not change. |
| Maven multi-module split | Package boundaries plus ArchUnit enforce the rules without build complexity. |
| Fully framework-free application layer | Decided: pragmatic core (see Assumptions). |
| CQRS, domain events, new use cases | Not requested. |
| Changes to the database schema | The Flyway schema stays as is. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Core purity | Pragmatic: `domain` imports no framework; `application` may use Spring stereotypes, `@Transactional` and Spring Data Commons types (`Page`, `Pageable`, `Sort`), but not JPA, Hibernate, Spring Data JPA or Spring Web | User decision on 2026-09-22 | y |
| Enforcement | ArchUnit tests (`com.tngtech.archunit:archunit-junit5` 1.4.1, test scope) | User decision on 2026-09-22 | y |
| Branch | `refactor/hexagonal`, stacked on `feat/customer-api`, with its own PR | User decision on 2026-09-22 | y |
| Package layout | `customer.domain`, `customer.application.port.in`, `customer.application.port.out`, `customer.application.service`, `customer.adapter.in.web`, `customer.adapter.out.persistence` | Standard ports-and-adapters naming; one bounded context | y |
| Input validation | Bean Validation stays on the web request DTO (an adapter concern, needed for the `errors` array); the CPF check-digit algorithm moves into the domain and the web validator delegates to it | One source for the rule; the domain stays annotation-free | y |
| Existing tests | Tests may move to new packages and switch mocks from `CustomerRepository` to output ports; no assertion on HTTP behavior may change | Behavior preservation is proven by unchanged assertions | y |
| Requirement ID format | `ARCH-01` style | Matches the validator's ID rule | y |

**Open questions:** none - all resolved or logged above (required before the spec is confirmed).

---

## User Stories

### P1: Framework-free domain ⭐ MVP

**User Story**: As a developer, I want the domain model free of framework code so that business rules can be read and tested without Spring or JPA.

**Why P1**: It is the center of the hexagon.

**Acceptance Criteria** (each line is one EARS pattern):

1. The system SHALL keep every class in `..customer.domain..` free of dependencies on `org.springframework..`, `jakarta..`, `org.hibernate..` and `..adapter..`. `ARCH-01`
2. The system SHALL represent the customer in the domain by a class without JPA annotations, persisted through a separate JPA entity in the persistence adapter. `ARCH-02`
3. The system SHALL implement the CPF check-digit rule once, in the domain, and the web validator SHALL delegate to it. `ARCH-03`

**Independent Test**: ArchUnit rule over `..customer.domain..` passes; `grep` finds `@Entity` only under `adapter.out.persistence`.

---

### P1: Application core behind ports ⭐ MVP

**User Story**: As a developer, I want use cases to reach the outside world only through port interfaces so that adapters can be replaced without touching business rules.

**Why P1**: Defines the hexagon's boundary.

**Acceptance Criteria**:

1. The system SHALL keep every class in `..customer.application..` free of dependencies on `..adapter..`, `jakarta.persistence..`, `org.hibernate..`, `org.springframework.data.jpa..` and `org.springframework.web..`. `ARCH-04`
2. The system SHALL expose each use case (create, get, update, delete, list, group by location) as an interface in `..application.port.in..`. `ARCH-05`
3. The system SHALL declare the persistence operations the use cases need as interfaces in `..application.port.out..`, and application services SHALL depend on those interfaces, never on Spring Data repositories. `ARCH-06`

**Independent Test**: ArchUnit rules over `..customer.application..` pass; the service unit tests mock only output ports.

---

### P1: Adapters at the edges ⭐ MVP

**User Story**: As a developer, I want adapters to depend inward only so that the web and persistence layers stay independent of each other.

**Why P1**: Completes the dependency rule.

**Acceptance Criteria**:

1. The system SHALL keep web adapter classes (`..adapter.in.web..`) from depending on `..adapter.out..` and on `..application.port.out..`. `ARCH-07`
2. The system SHALL keep persistence adapter classes (`..adapter.out.persistence..`) from depending on `..adapter.in..`. `ARCH-08`
3. The system SHALL implement every output port with a class in `..adapter.out..`. `ARCH-09`
4. The system SHALL place every `@RestController` in `..adapter.in.web..` and every `@Entity` and Spring Data repository in `..adapter.out.persistence..`. `ARCH-10`

**Independent Test**: ArchUnit layered-architecture rule passes.

---

### P1: Behavior preserved ⭐ MVP

**User Story**: As a client of the API, I want the refactor to be invisible so that nothing I depend on changes.

**Why P1**: A refactor that changes behavior is a regression.

**Acceptance Criteria**:

1. WHEN the refactor is complete THEN the system SHALL pass every HTTP integration test of customer-management and customer-geo-grouping with its assertions unchanged. `ARCH-11`
2. WHEN the grouping endpoint is called THEN the system SHALL still execute exactly one SQL statement and load zero JPA entities. `ARCH-12`
3. WHEN the refactor is complete THEN the system SHALL keep the Flyway migrations unchanged and pass Hibernate schema validation at startup. `ARCH-13`

**Independent Test**: `./mvnw -B clean verify` passes with the same HTTP test assertions as `feat/customer-api` (checked by diffing the integration test assertions).

---

## Edge Cases

- IF a future class in the domain imports Spring or JPA THEN the build SHALL fail through the ARCH-01 rule.
- IF a controller injects a repository or output port THEN the build SHALL fail through the ARCH-07 rule.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| ARCH-01 | P1: Framework-free domain | Design | Pending |
| ARCH-02 | P1: Framework-free domain | Design | Pending |
| ARCH-03 | P1: Framework-free domain | Design | Pending |
| ARCH-04 | P1: Application core behind ports | Design | Pending |
| ARCH-05 | P1: Application core behind ports | Design | Pending |
| ARCH-06 | P1: Application core behind ports | Design | Pending |
| ARCH-07 | P1: Adapters at the edges | Design | Pending |
| ARCH-08 | P1: Adapters at the edges | Design | Pending |
| ARCH-09 | P1: Adapters at the edges | Design | Pending |
| ARCH-10 | P1: Adapters at the edges | Design | Pending |
| ARCH-11 | P1: Behavior preserved | Design | Pending |
| ARCH-12 | P1: Behavior preserved | Design | Pending |
| ARCH-13 | P1: Behavior preserved | Design | Pending |

**Coverage:** 13 total, 0 mapped to tasks, 13 unmapped ⚠️ (mapped during Tasks)

---

## Success Criteria

- [ ] ArchUnit rules for ARCH-01, ARCH-04, ARCH-07 to ARCH-10 pass, and each fails when a violating class is introduced.
- [ ] `./mvnw -B clean verify` passes with no HTTP integration test assertion changed.
