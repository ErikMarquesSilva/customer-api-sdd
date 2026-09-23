# Hexagonal Architecture Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/hexagonal-architecture/design.md`
**Status**: Done (awaiting focused verification 5)
**Branch**: `refactor/hexagonal` (stacked on `feat/customer-api`)

---

## Test Coverage Matrix

> Carried over from customer-management (no project guidelines; strong defaults). New layer: architecture rules.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Architecture rules | unit (ArchUnit) | Every ARCH rule passes on production classes and fails on its violation fixture | `src/test/java/**/architecture/*Test.java` | `./mvnw -q -B test -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` |
| Domain | unit | Every rule of `Customer`, `Cpf` | `src/test/java/**/domain/*Test.java` | same as above |
| Application services | unit | Same scenarios as the old service tests, on output-port mocks | `src/test/java/**/application/**/*Test.java` | same as above |
| Persistence adapter | integration | Queries, mapping, exception translation, version check | `src/test/java/**/adapter/out/**/*IntegrationTest.java` | `./mvnw -q -B test` |
| Web adapter (HTTP) | integration | Existing HTTP tests, assertions unchanged (ARCH-11) | `src/test/java/**/*IntegrationTest.java` | `./mvnw -q -B test` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Unit-only tasks | `./mvnw -q -B test -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` |
| Full | Tasks with integration tests | `./mvnw -q -B test` |
| Build | Build changes and the last task | `./mvnw -B clean verify` |

---

## Execution Plan

### Phase 1: Migration

```
T1 → T2 → T3 → T4 → T5
```

### Phase 2: Verifier fix iteration 1

The verifier returned FAIL (`9796beb..4cfd2ff`): 13/13 ACs, but surviving mutants M3b (CUST-12 race untested on PUT), M10 (`@Transactional` unenforced), M11b (ARCH-03 copy undetected).

```
T6 → T7 → T8 → T9
```

### Phase 3: Verifier fix iteration 2

Re-verification returned FAIL (`9796beb..87626bf`): M3b, M10, M11b are killed, but the new guards could be evaded: N3 (`propagation = SUPPORTS`), N4b (delegate call ignored), and N1b (a race-test stub is never verified).

```
T10 → T11 → T12
```

### Phase 4: Verifier fix iteration 3

Re-verification 2 returned FAIL (`9796beb..f2d371c`). N3, N4b and N1b are killed. X5 survives (plausible): a method-level `@Transactional(propagation = SUPPORTS)` on `update` bypasses the class-level rule. Also cosmetic: A1 (algorithm moved into a constructor) and X14/X15 (race arrangement split across two lines).

```
T13 → T14 → T15
```

### Phase 5: User-approved follow-up

Re-verification 3 was FAIL (Y7/Y8: the versioned write could move into its own `REQUIRES_NEW` transaction without any test failing). The fix bound was reached and the issue was escalated. The user chose option (b): one more task, then a 4th verification.

```
T16 → T17
```

Round 4 was FAIL. T16 killed Y7 and Y8, but Z7b/Z7c (the adapter discards the entity the service read, via `clear()` or `refresh()`, inside one transaction) reach the same weak state. The verifier's suggestion is to assert the invariant itself: the stale write is rejected by Hibernate's versioned UPDATE (optimistic failure count 1). Limit set by the orchestrator: any new plausible survivor after T17 is escalated, with no further fix round.

---

## Task Breakdown

### Phase 1: Migration

#### T1: ArchUnit dependency and rule skeleton

**What**: Add `archunit-junit5` 1.4.1 (test scope) and `HexagonalArchitectureTest` with the layer-dependency rules from design.md (ARCH-01, ARCH-04 to ARCH-08). Rules allow empty packages, so they pass before the packages exist. Location rules (ARCH-09, ARCH-10) would fail on the current code and land in T5.
**Where**: `pom.xml` (+ `src/test/java/com/example/customerapi/architecture/HexagonalArchitectureTest.java`)
**Depends on**: None
**Reuses**: design.md ArchUnit table
**Requirement**: ARCH-01, ARCH-04, ARCH-05, ARCH-06, ARCH-07, ARCH-08 (rules written; strict from T5)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Rules compile and pass on the current code (empty target packages allowed)
- [x] Gate check passes: `./mvnw -B clean verify`, 246 tests (240 + 6 rules)

**Tests**: unit
**Gate**: build

**Status**: ✅ Complete

**Commit**: `build: add archunit and hexagonal architecture rules`

---

#### T2: Pure domain model

**What**: Create `customer.domain`: plain `Customer` (with `version`), `CustomerDetails`, `CustomerFilter` (moved), `LocationCount`, `StateLocation`, `CityLocation`, `Cpf` rule, domain exceptions (moved, plus `ConcurrentCustomerUpdateException`). Make `CpfValidator` delegate to `Cpf.isValid`.
**Where**: `src/main/java/com/example/customerapi/customer/domain/Customer.java` (+ the other domain files, `CustomerTest.java`, `CpfTest.java`, import updates)
**Depends on**: T1
**Reuses**: check-digit code from `CpfValidator`, rules from the old entity's `replaceWith`
**Requirement**: ARCH-01, ARCH-02, ARCH-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `Customer.register` assigns an id, sets both timestamps to the clock instant and sets version 0; `update` keeps `id`/`createdAt`, replaces the details and sets `updatedAt`
- [x] `Cpf.isValid` has the same cases as `CpfValidatorTest`, including both remainder-10 CPFs; `CpfValidator` delegates to it
- [x] Domain imports no framework (ARCH-01 rule green)
- [x] Gate check passes: `./mvnw -q -B test`

**Tests**: unit
**Gate**: full

**Status**: ✅ Complete (262 tests)

**Commit**: `refactor(customer): extract framework-free domain model`

---

#### T3: Ports, application services and persistence adapter (switchover)

**What**: Add the `port.in` and `port.out` interfaces. Move the use cases into `application.service` on the output ports. Move the entity, repository and specifications into `adapter.out.persistence` (renamed) behind `CustomerPersistenceAdapter`, which does the mapping, the version check and exception translation. Controllers call the use cases. Delete the old service, entity and repository.
**Where**: `src/main/java/com/example/customerapi/customer/application/service/CustomerService.java` (+ ports, `CustomerLocationService`, `CustomerSort`, persistence adapter files, controller wiring, moved and rewritten tests)
**Depends on**: T2
**Reuses**: bodies of the old `CustomerService`, `CustomerLocationService`, `CustomerRepository`, `CustomerSpecifications`, entity
**Requirement**: ARCH-02, ARCH-04, ARCH-05, ARCH-06, ARCH-09, ARCH-11, ARCH-12, ARCH-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Service unit tests cover the old scenarios, on `CustomerPersistencePort` / `LocationCountPort` mocks
- [x] Adapter integration tests: queries, specifications, native query, mapping round trip, `DataIntegrityViolationException` → `DuplicateFieldException(email|cpf)`, version mismatch → `ConcurrentCustomerUpdateException`
- [x] Every HTTP integration test passes with assertions unchanged (only arrangement code and the spy type change)
- [x] `ApiExceptionHandler` no longer references `DataIntegrityViolationException` or `ObjectOptimisticLockingFailureException`
- [x] Gate check passes: `./mvnw -q -B test`

**Tests**: unit, integration
**Gate**: full

**Status**: ✅ Complete (272 tests). HTTP assertions unchanged: only the stored-state accessors (`Customer::getName` to `CustomerDetails::name`, same values) and the logger-name fragment (`customer.CustomerService` to `service.CustomerService`) changed. `SpringDataCustomerRepository` is public so HTTP race tests can spy it.

**Commit**: `refactor(customer): route use cases through ports and a persistence adapter`

---

#### T4: Web adapter package

**What**: Move controllers, request/response DTOs, location DTOs and the `@Cpf` validation into `adapter.in.web`, with their unit tests.
**Where**: `src/main/java/com/example/customerapi/customer/adapter/in/web/CustomerController.java` (+ the other web classes and moved tests)
**Depends on**: T3
**Reuses**: existing web classes (moved, bodies unchanged except imports)
**Requirement**: ARCH-07, ARCH-10, ARCH-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] No class remains directly in `com.example.customerapi.customer` or `customer.location`
- [x] HTTP tests pass with assertions unchanged
- [x] Gate check passes: `./mvnw -q -B test`

**Tests**: integration
**Gate**: full

**Status**: ✅ Complete (272 tests). Black-box HTTP tests stay in their packages; only web-layer unit tests moved.

**Commit**: `refactor(customer): move web adapter into adapter.in.web`

---

#### T5: Enforce the architecture rules

**What**: Add the location rules (ARCH-09, ARCH-10), make every ArchUnit rule strict (no empty-package allowance), and add violation fixtures, showing that each rule fails when broken.
**Where**: `src/test/java/com/example/customerapi/architecture/HexagonalArchitectureTest.java` (+ fixture classes under `src/test/java/archfixtures/`)
**Depends on**: T4
**Reuses**: rules from T1
**Requirement**: ARCH-01, ARCH-04, ARCH-07, ARCH-08, ARCH-09, ARCH-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Each rule passes on production classes and fails on its fixture
- [x] `grep` finds `@Entity` only in `adapter/out/persistence`
- [x] Gate check passes: `./mvnw -B clean verify`

**Tests**: unit
**Gate**: build

**Status**: ✅ Complete (289 tests, clean build). 11 strict rules, 12 discrimination cases. Two rules added beyond the T1 set: inbound adapters must not depend on `application.service` (design: controllers use `port.in` only), and every feature class belongs to domain/application/adapter. Fixtures moved to the top-level `archfixtures` package after the first clean build showed Spring scanning the fixture `@Entity` (`missing table [jpa_entity_in_domain]`).

**Commit**: `test(architecture): enforce hexagonal dependency rules`

---

### Phase 2: Verifier fix iteration 1

#### T6: Test the unique-constraint race on PUT

**What**: HTTP test: a PUT whose uniqueness pre-check misses (spied repository) hits the database constraint and returns 409 with the field, for email and cpf.
**Where**: `src/test/java/com/example/customerapi/customer/CustomerUpdateIntegrationTest.java`
**Depends on**: T5
**Reuses**: existing tests, `HexagonalRules`
**Requirement**: CUST-12 (update path), ARCH-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] PUT with a taken email and `existsByEmailAndIdNot` stubbed false → 409, `errors[].field` = email, customer unchanged
- [x] Same for cpf
- [x] Mutant M3b (update skips `write()`) fails the new tests
- [x] Gate check passes: `./mvnw -q -B test`

**Tests**: integration
**Gate**: full

**Status**: ✅ Complete. M3b now fails both new tests (500 instead of 409).

**Commit**: `test(customer): cover the unique-constraint race on update`

---

#### T7: Enforce transactional use cases and correct the concurrency note

**What**: ArchUnit rule: every `@Service` in `application.service` is `@Transactional`. Correct design.md: in production CUST-24 is caught by Hibernate's versioned UPDATE; the explicit version check covers callers outside a transaction.
**Where**: `src/test/java/com/example/customerapi/architecture/HexagonalRules.java` (+ test registration, fixture, design.md)
**Depends on**: T6
**Reuses**: existing tests, `HexagonalRules`
**Requirement**: ARCH-11 (CUST-24 guarantee), design

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Rule passes on production and reports a non-transactional service fixture
- [x] Mutant M10 (drop `@Transactional`) fails the rule
- [x] design.md Risks/Tech Decisions state which defense is live
- [x] Gate check passes: quick

**Tests**: unit
**Gate**: quick

**Status**: ✅ Complete. M10 fails the new rule. `CustomerLocationService` moved `@Transactional(readOnly = true)` from its only method to the class (equivalent).

**Commit**: `test(architecture): require transactional use cases`

---

#### T8: Enforce single CPF implementation

**What**: ArchUnit rule: the web `CpfValidator` calls `domain.Cpf.isValid`, so a copied algorithm without delegation fails the build.
**Where**: `src/test/java/com/example/customerapi/architecture/HexagonalRules.java` (+ test registration, fixture)
**Depends on**: T7
**Reuses**: existing tests, `HexagonalRules`
**Requirement**: ARCH-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Rule passes on production and reports a validator fixture that does not delegate
- [x] Mutant M11b (algorithm copied back into `CpfValidator`) fails the rule
- [x] Gate check passes: quick

**Tests**: unit
**Gate**: quick

**Status**: ✅ Complete. M11b passes the behavioural tests but fails the new rule.

**Commit**: `test(architecture): require cpf validation to delegate to the domain`

---

#### T9: Tidy moved files

**What**: Remove leftover blank lines in `ApiExceptionHandler`, restore import grouping in moved files, wrap the long javadoc in `CustomerLocationController`.
**Where**: `src/main/java/com/example/customerapi/common/web/ApiExceptionHandler.java` (+ moved files' imports)
**Depends on**: T8
**Reuses**: existing tests, `HexagonalRules`
**Requirement**: ARCH-11 (no behaviour change)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] No line over 120 characters in `src/main`
- [x] Gate check passes: `./mvnw -B clean verify`

**Tests**: none
**Gate**: build

**Status**: ✅ Complete (295 tests, clean build).

**Commit**: `style: tidy imports and blank lines after the hexagonal move`

---

### Phase 3: Verifier fix iteration 2

#### T10: CPF validator may call only the domain rule

**What**: Strengthen the ARCH-03 rule: `CpfValidator` must call `domain.Cpf.isValid` and no other method. Add a fixture that calls the domain rule and also computes its own result.
**Where**: `src/test/java/com/example/customerapi/architecture/HexagonalRules.java` (+ fixture, discrimination case)
**Depends on**: T9
**Reuses**: `HexagonalRules`, existing race tests
**Requirement**: ARCH-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Rule passes on production; reports both the non-delegating and the delegate-but-ignore fixtures
- [x] N4b and M11b fail the rule
- [x] Gate check passes: quick

**Tests**: unit
**Gate**: quick

**Status**: ✅ Complete. N4b (7 violations) and M11b (5) fail the rule. Calls to the validator's own methods are allowed because javac adds a bridge `isValid(Object, ...)`; a copied algorithm in private helpers still calls `String` methods and fails.

**Commit**: `test(architecture): allow the cpf validator to call only the domain rule`

---

#### T11: Transactional rule checks propagation

**What**: The use-case rule requires `@Transactional` with a propagation that starts a transaction (REQUIRED, REQUIRES_NEW, NESTED). Add a SUPPORTS fixture and make design.md state what the rule checks.
**Where**: `src/test/java/com/example/customerapi/architecture/HexagonalRules.java` (+ fixture, discrimination case, design.md)
**Depends on**: T10
**Reuses**: `HexagonalRules`, existing race tests
**Requirement**: ARCH-11 (CUST-24 guarantee)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Rule passes on production; reports the SUPPORTS fixture
- [x] N3 fails the rule
- [x] Gate check passes: quick

**Tests**: unit
**Gate**: quick

**Status**: ✅ Complete. N3 (`propagation = SUPPORTS`) fails the rule.

**Commit**: `test(architecture): require use-case transactions to start a transaction`

---

#### T12: Race tests verify their stub was used

**What**: Each race test that disables a pre-check by stubbing the spied repository also proves the request reached the database write (`assertWriteReachedTheDatabase`, after clearing the arrangement's invocations). Verifying that the stubbed method was called is not enough: with a stub on the wrong method, the real pre-check still runs and answers 409.
**Where**: `src/test/java/com/example/customerapi/customer/CustomerUpdateIntegrationTest.java` (+ `CustomerCreateAndGetIntegrationTest.java`)
**Depends on**: T11
**Reuses**: `HexagonalRules`, existing race tests
**Requirement**: CUST-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] The 4 race tests (create and update, email and cpf) verify their stubbed method
- [x] A stub on the wrong method (N1b) fails the test that owns it
- [x] Gate check passes: `./mvnw -B clean verify`

**Tests**: integration
**Gate**: build

**Status**: ✅ Complete (297 tests, clean build). N1b (stub on the wrong method) fails in both the create and the update race tests.

**Commit**: `test(customer): verify race-test stubs are exercised`

---

### Phase 4: Verifier fix iteration 3

#### T13: Behavioural probe of the write transaction

**What**: HTTP tests that record, from inside the persistence call of a real POST and PUT, that a transaction is active and not read-only. This covers every way of losing the use-case transaction (class or method level, any propagation, readOnly) instead of adding one static rule per annotation level.
**Where**: `src/test/java/com/example/customerapi/customer/TransactionBoundaryIntegrationTest.java`
**Depends on**: T12
**Reuses**: `HttpIntegrationTestSupport`, `HexagonalRules`
**Requirement**: ARCH-11 (CUST-24 guarantee)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] POST and PUT observe an active, read-write transaction at the persistence call
- [x] X5 (method-level SUPPORTS on update), N3 and a readOnly update each fail the probe
- [x] Gate check passes: `./mvnw -q -B test`

**Tests**: integration
**Gate**: full

**Status**: ✅ Complete. Killed: X5 (method-level SUPPORTS on update), N3 (class-level SUPPORTS), `readOnly` on update, M10 (no `@Transactional`).

**Commit**: `test(customer): probe the write transaction of create and update`

---

#### T14: CPF rule also checks constructor calls

**What**: Use `onlyCallCodeUnitsThat` so the validator can call neither foreign methods nor foreign constructors (allowing `Object` for `super()`). Add a fixture that hides the algorithm in another class's constructor.
**Where**: `src/test/java/com/example/customerapi/architecture/HexagonalRules.java` (+ fixture, discrimination case)
**Depends on**: T13
**Reuses**: `HttpIntegrationTestSupport`, `HexagonalRules`
**Requirement**: ARCH-03

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Rule passes on production; reports the constructor-hiding fixture (A1)
- [x] Gate check passes: quick

**Tests**: unit
**Gate**: quick

**Status**: ✅ Complete (30 architecture tests).

**Commit**: `test(architecture): stop the cpf validator calling foreign constructors`

---

#### T15: One helper for race-test arrangement

**What**: `HttpIntegrationTestSupport.preCheckMisses(stub)` applies the stub and clears the arrangement's invocations in one call; the four race tests use it.
**Where**: `src/test/java/com/example/customerapi/HttpIntegrationTestSupport.java` (+ the two race test classes)
**Depends on**: T14
**Reuses**: `HttpIntegrationTestSupport`, `HexagonalRules`
**Requirement**: CUST-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] The four race tests use the helper; no separate `clearInvocations` remains
- [x] Gate check passes: `./mvnw -B clean verify`

**Tests**: integration
**Gate**: build

**Status**: ✅ Complete (300 tests, clean build). N1b is still killed through the helper.

**Commit**: `test(customer): arrange race tests through one helper`

---

### Phase 5: User-approved follow-up

#### T16: Each write request completes exactly one transaction

**What**: HTTP tests assert, using Hibernate statistics (already enabled for tests), that one POST and one PUT each complete exactly one transaction. Reading and versioned writing therefore cannot be split.
**Where**: `src/test/java/com/example/customerapi/customer/TransactionBoundaryIntegrationTest.java`
**Depends on**: T15
**Reuses**: `TestcontainersConfiguration` statistics bean, T13 probe
**Requirement**: ARCH-11 (CUST-24 guarantee)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] POST and PUT each complete exactly 1 successful transaction
- [x] Y8 (`REQUIRES_NEW` on `CustomerPersistenceAdapter.update`) and Y7 (`TransactionTemplate` REQUIRES_NEW write in `CustomerService.update`) fail the test
- [x] Gate check passes: `./mvnw -B clean verify`

**Tests**: integration
**Gate**: build

**Status**: ✅ Complete (302 tests, clean build). Checked empirically first: the counter reports 1 transaction per POST and PUT, and 2 under Y8. Y7 and Y8 fail `updateCompletesExactlyOneTransaction`.

**Commit**: `test(customer): assert one transaction per write request`

---

#### T17: CUST-24 test asserts the versioned UPDATE rejected the stale write

**What**: In the CUST-24 HTTP test, clear Hibernate statistics before the request and assert `getOptimisticFailureCount() == 1`. This is the invariant itself: the stale write reached the versioned UPDATE and was rejected there.
**Where**: `src/test/java/com/example/customerapi/customer/CustomerUpdateIntegrationTest.java`
**Depends on**: T16
**Reuses**: statistics bean (T19 of customer-management), CUST-24 test
**Requirement**: ARCH-11 (CUST-24 guarantee)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] At HEAD the count is 1 and the test passes
- [x] Y7, Y8, Z7b and Z7c fail the test
- [x] design.md wording: "not split across transactions"; the probe javadoc names create and update only
- [x] Gate check passes: `./mvnw -B clean verify`

**Tests**: integration
**Gate**: build

**Status**: ✅ Complete (302 tests, clean build). With the verifier's saved diffs, Y7, Y8, Z7b and Z7c each fail with `expected: 1L but was: 0L`.

**Commit**: `test(customer): assert the versioned update rejects the stale write`

---

## Phase Execution Map

```
Phase 1:  T1 ------→ T2 ------→ T3 ------→ T4 ------→ T5
Phase 2:  T6 ------→ T7 ------→ T8 ------→ T9
Phase 3:  T10 ------→ T11 ------→ T12
Phase 4:  T13 ------→ T14 ------→ T15
Phase 5:  T16 ------→ T17
```

## Requirement Coverage

| Requirement | Tasks |
| ----------- | ----- |
| ARCH-01 | T1, T2, T5 |
| ARCH-02, ARCH-03 | T2, T3 |
| ARCH-04 to ARCH-06 | T3, T5 |
| ARCH-07, ARCH-08 | T4, T5 |
| ARCH-09, ARCH-10 | T3, T4, T5 |
| ARCH-11 to ARCH-13 | T3, T4 (gates), final clean verify |

All 13 requirements are mapped.
