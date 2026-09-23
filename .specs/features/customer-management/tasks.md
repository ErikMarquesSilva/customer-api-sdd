# Customer Management Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/customer-management/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: none - strong defaults applied. Test types from the user's request ("testes unitários e/ou de integração"); commands from `pom.xml` (Maven Wrapper, Surefire). No linter or formatter is configured.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Validation (constraint validators, request DTO normalization) | unit | Every rule and boundary in CUST-03 to CUST-09, CUST-48 to CUST-51 | `src/test/java/**/*Test.java` | `./mvnw -q -B test -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` |
| Service | unit | All branches; 1:1 to spec ACs; every listed edge case | `src/test/java/**/*Test.java` | same as above |
| Repository / queries | integration | Key query paths + constraint violations | `src/test/java/**/*IntegrationTest.java` | `./mvnw -q -B test` |
| Controller + exception handler | integration (MockMvc + Testcontainers) | Every route: happy path + edge + error paths | `src/test/java/**/*IntegrationTest.java` | `./mvnw -q -B test` |
| Entity / config / migration | none | - (build gate; exercised by integration tests) | - | build gate only |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After tasks with unit tests only | `./mvnw -q -B test -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` |
| Full | After tasks with integration tests | `./mvnw -q -B test` (Docker must be running) |
| Build | After phase completion or config-only tasks | `./mvnw -B verify` |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Foundation

```
T1 → T2
```

### Phase 2: Domain

```
T3 → T4 → T5 → T6
```

### Phase 3: HTTP API

```
T7 → T8 → T9 → T10 → T11
```

### Phase 4: Fixes and amendments

Added after batch 2: the user's sort amendment (CUST-52, CUST-53) and two defects the batch 2 worker found.

```
T12 → T13 → T14
```

### Phase 5: Verifier fix iteration 1

The verifier returned FAIL (range `1810ee5..e01ecd8`): one surviving mutant on CUST-44 plus weak evidence and spec-precision gaps.

```
T15 → T16 → T17 → T18
```

---

## Task Breakdown

### Phase 1: Foundation

#### T1: Build, configuration and PostgreSQL test infrastructure

**What**: Add Flyway, Flyway PostgreSQL, Actuator and Testcontainers dependencies, remove H2, configure datasource/JPA/actuator properties, and boot the context test against a Testcontainers PostgreSQL.
**Where**: `pom.xml`, `src/main/resources/application.properties`, `src/test/java/com/example/customerapi/TestcontainersConfiguration.java`, `src/test/java/com/example/customerapi/CustomerApiSddApplicationTests.java`
**Depends on**: None
**Reuses**: Initializr pom; artifact names confirmed from start.spring.io for Boot 4.1.1
**Requirement**: CUST-47 (infrastructure), CUST-44 (exposure config)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `pom.xml` has `spring-boot-starter-flyway`, `flyway-database-postgresql`, `spring-boot-starter-actuator`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`; no `h2`
- [x] `application.properties`: datasource from `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` with local defaults, `ddl-auto=validate`, `open-in-view=false`, actuator exposure `health` only
- [x] Context test boots against PostgreSQL via `@Import(TestcontainersConfiguration.class)`
- [x] Gate check passes: `./mvnw -B verify`
- [x] Test count: 1 test passes

**Status**: ✅ Complete

**Tests**: integration
**Gate**: build

**Commit**: `build: add flyway, actuator and testcontainers postgres setup`

---

#### T2: Customer table, entity and repository

**What**: Flyway `V1__create_customer.sql`, the `Customer` entity (UUID, timestamps, `@Version`) and `CustomerRepository` with the `exists*` look-ups, with integration tests.
**Where**: `src/main/resources/db/migration/V1__create_customer.sql`, `src/main/java/com/example/customerapi/customer/Customer.java`, `src/main/java/com/example/customerapi/customer/CustomerRepository.java`, `src/test/java/com/example/customerapi/customer/CustomerRepositoryIntegrationTest.java`
**Depends on**: T1
**Reuses**: `TestcontainersConfiguration` (T1)
**Requirement**: CUST-47, CUST-10, CUST-11 (DB constraints backing them)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Application starts on an empty PostgreSQL and Flyway creates `customer`; Hibernate `validate` passes (CUST-47)
- [x] Saving a second row with the same email or cpf fails with `DataIntegrityViolationException` naming `uk_customer_email` / `uk_customer_cpf`
- [x] `existsByEmailAndIdNot` / `existsByCpfAndIdNot` return false for the row itself and true for another row
- [x] Gate check passes: `./mvnw -q -B test`
- [x] Test count: ≥ 5 tests pass (no silent deletions)

**Status**: ✅ Complete

**Tests**: integration
**Gate**: full

**Commit**: `feat(customer): add customer table, entity and repository`

---

### Phase 2: Domain

#### T3: CPF constraint validator

**What**: `@Cpf` annotation and `CpfValidator` (11 digits, not all equal, valid check digits; null valid).
**Where**: `src/main/java/com/example/customerapi/customer/validation/CpfValidator.java` (+ `Cpf.java` annotation, `CpfValidatorTest.java`)
**Depends on**: None
**Reuses**: Jakarta Bean Validation `ConstraintValidator`
**Requirement**: CUST-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Accepts `52998224725`; rejects wrong check digits, 10 or 12 digits, letters, `11111111111`
- [x] Gate check passes: quick
- [x] Test count: ≥ 6 tests pass

**Status**: ✅ Complete

**Tests**: unit
**Gate**: quick

**Commit**: `feat(customer): add cpf constraint validator`

---

#### T4: CustomerRequest with normalization, constraints and UTC clock

**What**: `CustomerRequest` record that normalizes in its compact constructor and declares all field constraints, plus `ClockConfig` (UTC `Clock` bean wired into Bean Validation's `ClockProvider`).
**Where**: `src/main/java/com/example/customerapi/customer/CustomerRequest.java` (+ `common/config/ClockConfig.java`, `CustomerRequestTest.java`)
**Depends on**: T3
**Reuses**: `@Cpf` (T3), `ValidationConfigurationCustomizer`
**Requirement**: CUST-03, CUST-04, CUST-05, CUST-06, CUST-07, CUST-08, CUST-09, CUST-48, CUST-49, CUST-50, CUST-51

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Normalization: ` Ana@Example.COM ` → `ana@example.com`; `529.982.247-25` → `52998224725`; `sp` → `SP`; `  São   Paulo ` → `São Paulo`
- [x] Each invalid field yields a violation on exactly that property; boundaries: name 2/120 ok, 1/121 fail; city 2/100 ok, 1/101 fail; email 254 ok, 255 fail; phone 10/13 digits ok, 9/14 fail; birthDate yesterday ok, today fail (fixed clock); state `XX` fail
- [x] Gate check passes: quick
- [x] Test count: ≥ 20 tests pass

**Status**: ✅ Complete

**Tests**: unit
**Gate**: quick

**Commit**: `feat(customer): add customer request normalization and validation`

---

#### T5: CustomerService create, get, update, delete

**What**: `CustomerService` CRUD operations with uniqueness pre-checks, not-found handling, `Clock` timestamps, PII-free INFO logs, plus `CustomerResponse`, `CustomerNotFoundException`, `DuplicateFieldException`.
**Where**: `src/main/java/com/example/customerapi/customer/CustomerService.java` (+ `CustomerResponse.java`, exceptions, `CustomerServiceTest.java`)
**Depends on**: T4
**Reuses**: `CustomerRepository` (T2), `CustomerRequest` (T4)
**Requirement**: CUST-01, CUST-10, CUST-11, CUST-14, CUST-15, CUST-17, CUST-18, CUST-19, CUST-21, CUST-22, CUST-23, CUST-25, CUST-28, CUST-45, CUST-46

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] create/update throw `DuplicateFieldException("email"|"cpf")` on conflicts; update ignores the customer's own email/cpf
- [x] get/update/delete throw `CustomerNotFoundException` for unknown ids; update never saves a new row
- [x] update keeps `id`/`createdAt`, sets `updatedAt` from the clock, nulls omitted phone/birthDate
- [x] create/update/delete log one INFO line with operation and id and without email, cpf, phone
- [x] Gate check passes: quick
- [x] Test count: ≥ 15 tests pass

**Status**: ✅ Complete

**Tests**: unit
**Gate**: quick

**Commit**: `feat(customer): add customer service crud operations`

---

#### T6: Listing with sort whitelist and filters

**What**: `CustomerSort` parser, `CustomerSpecifications` (name contains ignoring case with escaped wildcards, email exact ignoring case, AND), `PageResponse`, and `CustomerService.list`.
**Where**: `src/main/java/com/example/customerapi/customer/CustomerSpecifications.java` (+ `CustomerSort.java`, `PageResponse.java`, `CustomerService.java` modify, `CustomerSortTest.java`, `CustomerSpecificationsIntegrationTest.java`)
**Depends on**: T5
**Reuses**: `JpaSpecificationExecutor`, `CustomerService` (T5)
**Requirement**: CUST-29, CUST-30, CUST-31, CUST-33, CUST-34, CUST-40, CUST-41, CUST-42

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Sort: default `name,asc`; accepts name/email/createdAt/updatedAt with asc/desc; any other property or direction throws `InvalidSortException`; `id` appended as tiebreaker
- [x] Filters on PostgreSQL: `name=ana` matches `Ana Souza` and `Mariana Lima`, not `Bruno Reis`; `name=a%` does not act as a wildcard; `email= ANA@X.COM ` matches `ana@x.com`; both filters AND
- [x] Gate check passes: `./mvnw -q -B test`
- [x] Test count: ≥ 10 tests pass

**Status**: ✅ Complete

**Tests**: unit, integration
**Gate**: full

**Commit**: `feat(customer): add paginated listing with sort and filters`

---

### Phase 3: HTTP API

#### T7: Error contract and create/retrieve endpoints

**What**: `ApiExceptionHandler` (AD-003) and `CustomerController` with `POST` and `GET /{id}`, with MockMvc integration tests.
**Where**: `src/main/java/com/example/customerapi/customer/CustomerController.java` (+ `common/web/ApiExceptionHandler.java`, `CustomerCreateAndGetIntegrationTest.java`, `ErrorContractIntegrationTest.java`)
**Depends on**: T6
**Reuses**: `CustomerService` (T5), Spring `ResponseEntityExceptionHandler`
**Requirement**: CUST-01 to CUST-16, CUST-35 to CUST-39, CUST-48 to CUST-51

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] POST returns 201, body and `Location`; every invalid field → 400 with the right `errors` entry; duplicates → 409 with field; DB-constraint race (repository pre-check spied to false) → 409 (CUST-12); body `id`/`createdAt` ignored
- [x] GET returns 200 / 404 / 400 for existing, unknown and non-UUID ids
- [x] All errors are `application/problem+json` with `type`, `title`, `status`, `detail`, `instance`; malformed JSON 400; `text/plain` 415; forced exception → 500 with fixed detail and no exception message
- [x] Gate check passes: `./mvnw -q -B test`
- [x] Test count: ≥ 25 tests pass

**Status**: ✅ Complete

**Tests**: integration
**Gate**: full

**Commit**: `feat(customer): add create and get endpoints with problem details errors`

---

#### T8: Update endpoint

**What**: `PUT /{id}` plus optimistic-lock → 409 mapping, with integration tests.
**Where**: `src/main/java/com/example/customerapi/customer/CustomerController.java` (modify) (+ `CustomerUpdateIntegrationTest.java`)
**Depends on**: T7
**Reuses**: `CustomerService.update` (T5), `ApiExceptionHandler` (T7)
**Requirement**: CUST-17 to CUST-24

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] PUT replaces all fields (200), omitted optionals become null, `createdAt` unchanged and `updatedAt` advanced
- [x] Invalid body → 400 and the stored customer unchanged; conflicting email/cpf → 409 and unchanged; own email/cpf → 200; unknown id → 404 and no row created
- [x] A concurrent committed update between read and write → 409 and the first commit's data kept (CUST-24)
- [x] Gate check passes: `./mvnw -q -B test`
- [x] Test count: ≥ 9 tests pass

**Status**: ✅ Complete

**Tests**: integration
**Gate**: full

**Commit**: `feat(customer): add update endpoint with optimistic locking`

---

#### T9: Delete endpoint

**What**: `DELETE /{id}` with integration tests.
**Where**: `src/main/java/com/example/customerapi/customer/CustomerController.java` (modify) (+ `CustomerDeleteIntegrationTest.java`)
**Depends on**: T8
**Reuses**: `CustomerService.delete` (T5)
**Requirement**: CUST-25 to CUST-28

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] DELETE → 204 with empty body; later GET → 404; same email and cpf can be re-created (201); unknown id → 404
- [x] Gate check passes: `./mvnw -q -B test`
- [x] Test count: ≥ 4 tests pass

**Status**: ✅ Complete

**Tests**: integration
**Gate**: full

**Commit**: `feat(customer): add delete endpoint`

---

#### T10: List endpoint

**What**: `GET /api/v1/customers` with `page`, `size`, `sort`, `name`, `email`, with integration tests.
**Where**: `src/main/java/com/example/customerapi/customer/CustomerController.java` (modify) (+ `CustomerListIntegrationTest.java`)
**Depends on**: T9
**Reuses**: `CustomerService.list` (T6)
**Requirement**: CUST-29 to CUST-34, CUST-40 to CUST-42

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] 25 customers: default returns 20 sorted by name with `page` metadata (totalElements 25, totalPages 2); `page=1` returns 5; `size=100` accepted
- [x] `sort=email,desc` orders by email descending; `page=-1`, `size=0`, `size=101`, `sort=cpf,asc` → 400; no match → empty content, totalElements 0
- [x] `name` / `email` / both filters return the expected customers over HTTP
- [x] Gate check passes: `./mvnw -q -B test`
- [x] Test count: ≥ 10 tests pass

**Status**: ✅ Complete

**Tests**: integration
**Gate**: full

**Commit**: `feat(customer): add paginated list endpoint`

---

#### T11: Operability checks

**What**: Integration tests for health, actuator exposure and PII-free logging (config from T1, logging from T5).
**Where**: `src/test/java/com/example/customerapi/OperabilityIntegrationTest.java`
**Depends on**: T10
**Reuses**: `OutputCaptureExtension`, `TestcontainersConfiguration`
**Requirement**: CUST-43, CUST-44, CUST-45, CUST-46

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `/actuator/health` → 200 `status` `UP`; `/actuator/env` and `/actuator/beans` → 404
- [x] Create, update and delete over HTTP each log one INFO line with the id; captured output contains no email, cpf or phone of that customer
- [x] Gate check passes: `./mvnw -B verify`
- [x] Test count: ≥ 4 tests pass

**Status**: ✅ Complete

**Tests**: integration
**Gate**: build

**Commit**: `test(customer): verify health exposure and pii-free logging`

---

### Phase 4: Fixes and amendments

#### T12: Sort direction defaults to ascending

**What**: Accept `sort=<property>` without a direction as ascending; keep 400 for unknown properties and directions.
**Where**: `src/main/java/com/example/customerapi/customer/CustomerSort.java` (+ `CustomerSortTest.java`, `CustomerListIntegrationTest.java`)
**Depends on**: T11
**Reuses**: `CustomerSort` (T6), list endpoint (T10)
**Requirement**: CUST-52, CUST-53

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `sort=name` → `name` ascending with `id` tiebreaker (unit) and ascending order over HTTP
- [x] `sort=name,up` → 400 over HTTP; `sort=cpf` → 400
- [x] Gate check passes: `./mvnw -q -B test`
- [x] Test count: 206 tests pass (197 + 9)

**Tests**: unit, integration
**Gate**: full

**Status**: ✅ Complete

**Commit**: `feat(customer): default sort direction to ascending`

---

#### T13: Keep PostgreSQL error detail out of logs

**What**: Stop the PostgreSQL JDBC driver from putting server error detail (the duplicated key value) into exception messages that Hibernate logs, and test the race path logs.
**Where**: `src/main/resources/application.properties` (+ `OperabilityIntegrationTest.java`)
**Depends on**: T12
**Reuses**: `OperabilityIntegrationTest` (T11), race-path stubbing from T7
**Requirement**: CUST-46

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] A create that passes the pre-check and hits `uk_customer_email` / `uk_customer_cpf` returns 409 and the captured output contains neither the email nor the cpf
- [x] Gate check passes: `./mvnw -q -B test`
- [x] Test count: 208 tests pass (206 + 2)

**Tests**: integration
**Gate**: full

**Status**: ✅ Complete

**Commit**: `fix(customer): keep database error detail out of logs`

---

#### T14: Microsecond clock precision

**What**: Make the UTC clock tick in microseconds so timestamps returned at create/update equal what PostgreSQL stores (`timestamptz` has microsecond precision) on every OS.
**Where**: `src/main/java/com/example/customerapi/common/config/ClockConfig.java` (+ `ClockConfigTest.java`)
**Depends on**: T13
**Reuses**: `ClockConfig` (T4)
**Requirement**: CUST-01, CUST-14, CUST-19

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] The clock bean is UTC and every instant it returns has zero sub-microsecond nanos
- [x] Gate check passes: `./mvnw -B verify`
- [x] Test count: 211 tests pass (208 + 3)

**Tests**: unit
**Gate**: build

**Status**: ✅ Complete

**Commit**: `fix(customer): align clock precision with postgresql timestamps`

---

### Phase 5: Verifier fix iteration 1

#### T15: Assert the full actuator exposure set

**What**: Assert that the actuator discovery lists exactly `self`, `health` and `health-path`, killing the surviving mutant M19 (`include=health,info`).
**Where**: `src/test/java/com/example/customerapi/OperabilityIntegrationTest.java`
**Depends on**: T14
**Reuses**: existing test classes and `HttpIntegrationTestSupport`
**Requirement**: CUST-44

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `GET /actuator` `_links` keys are exactly {self, health, health-path}
- [x] Mutant `include=health,info` fails the test
- [x] Gate check passes: `./mvnw -q -B test`

**Tests**: integration
**Gate**: full

**Status**: ✅ Complete

**Commit**: `test(customer): assert the full actuator exposure set`

---

#### T16: Cover the CPF remainder-10 branch directly

**What**: Unit cases where each check digit comes from the remainder-10 → 0 rule.
**Where**: `src/test/java/com/example/customerapi/customer/validation/CpfValidatorTest.java`
**Depends on**: T15
**Reuses**: existing test classes and `HttpIntegrationTestSupport`
**Requirement**: CUST-07

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `10000000108` (first digit via remainder 10) and `10000002810` (second digit via remainder 10) are accepted
- [x] Mutating `remainder == 10 ? 0 : remainder` fails `CpfValidatorTest`
- [x] Gate check passes: quick

**Tests**: unit
**Gate**: quick

**Status**: ✅ Complete

**Commit**: `test(customer): cover cpf check-digit remainder-10 branch`

---

#### T17: Error contract and PII checks on the remaining error paths

**What**: Share the RFC 9457 assertion helper and apply it to the list 400s and the optimistic-lock 409; assert no PII in logs on the 400 validation and malformed-JSON paths.
**Where**: `src/test/java/com/example/customerapi/HttpIntegrationTestSupport.java` (+ `ErrorContractIntegrationTest.java`, `CustomerListIntegrationTest.java`, `CustomerUpdateIntegrationTest.java`, `OperabilityIntegrationTest.java`)
**Depends on**: T16
**Reuses**: existing test classes and `HttpIntegrationTestSupport`
**Requirement**: CUST-35, CUST-46

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `page=-1`, `size=101`, `sort=cpf`, `sort=name,up` and the CUST-24 409 carry `type`, `title`, `status`, `detail`, `instance`
- [ ] Invalid-body 400 and malformed-JSON 400 leave no email/cpf/phone of the request in the logs
- [ ] Gate check passes: `./mvnw -q -B test`

**Tests**: integration
**Gate**: full

**Commit**: `test(customer): check error contract and log pii on remaining error paths`

---

#### T18: Clarify CUST-06, CUST-36 and CUST-44 in the spec

**What**: Record the verifier's spec-precision gaps as assumptions: `errors` array scope, email validity rule, actuator discovery root.
**Where**: `.specs/features/customer-management/spec.md`
**Depends on**: T17
**Reuses**: existing test classes and `HttpIntegrationTestSupport`
**Requirement**: CUST-06, CUST-36, CUST-44

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Assumptions table has one row per clarification, each with default and rationale
- [ ] `validate_spec.py` passes

**Tests**: none
**Gate**: build

**Commit**: `docs(spec): clarify email validity, errors array scope and actuator root`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 ------→ T2
Phase 2:  T3 ------→ T4 ------→ T5 ------→ T6
Phase 3:  T7 ------→ T8 ------→ T9 ------→ T10 ------→ T11
Phase 4:  T12 ------→ T13 ------→ T14
Phase 5:  T15 ------→ T16 ------→ T17 ------→ T18
```

Execution is strictly sequential.

---

## Requirement Coverage

| Requirement | Tasks |
| ----------- | ----- |
| CUST-01 to CUST-02 | T5, T7 |
| CUST-03 to CUST-09, CUST-48 to CUST-51 | T4, T7 |
| CUST-10 to CUST-11 | T2, T5, T7 |
| CUST-12 to CUST-16 | T5, T7 |
| CUST-17 to CUST-24 | T5, T8 |
| CUST-25 to CUST-28 | T5, T9 |
| CUST-29 to CUST-34 | T6, T10 |
| CUST-35 to CUST-39 | T7 |
| CUST-40 to CUST-42 | T6, T10 |
| CUST-43 to CUST-46 | T1, T5, T11 |
| CUST-47 | T1, T2 |
| CUST-52 to CUST-53 | T12 |

All 53 requirements are mapped. CUST-46 is reinforced by T13; CUST-01, CUST-14, CUST-19 by T14.
