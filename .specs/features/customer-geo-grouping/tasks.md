# Customer Geographic Grouping Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/customer-geo-grouping/design.md`
**Status**: Done
**Prerequisite**: customer-management tasks T1 to T11 complete.

---

## Test Coverage Matrix

> Same matrix as customer-management (strong defaults; no guidelines found).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Repository / queries | integration | Every grouping rule the query owns (GEO-009 to GEO-012) | `src/test/java/**/*IntegrationTest.java` | `./mvnw -q -B test` |
| Service | unit | All branches; 1:1 to spec ACs (GEO-002 to GEO-007) | `src/test/java/**/*Test.java` | `./mvnw -q -B test -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` |
| Controller | integration (MockMvc + Testcontainers) | Route end to end + data-change and efficiency ACs | `src/test/java/**/*IntegrationTest.java` | `./mvnw -q -B test` |
| DTO records | none | - (covered through service and controller tests) | - | build gate only |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After tasks with unit tests only | `./mvnw -q -B test -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` |
| Full | After tasks with integration tests | `./mvnw -q -B test` (Docker must be running) |
| Build | After the last task | `./mvnw -B verify` |

---

## Execution Plan

### Phase 1: Grouping

```
T1 → T2 → T3
```

### Phase 2: Verifier follow-ups

Added after the verifier's PASS report: it flagged two spec-precision gaps, now GEO-018 and GEO-019.

```
T4
```

---

## Task Breakdown

### Phase 1: Grouping

#### T1: Aggregate query

**What**: `LocationCount` projection and `CustomerRepository.countByLocation()` native query, with integration tests on PostgreSQL.
**Where**: `src/main/java/com/example/customerapi/customer/CustomerRepository.java` (modify) (+ `customer/location/LocationCount.java`, `CustomerLocationQueryIntegrationTest.java`)
**Depends on**: None
**Reuses**: `CustomerRepository`, `customer` table, `TestcontainersConfiguration`
**Requirement**: GEO-003, GEO-009, GEO-010, GEO-011, GEO-012

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `Campinas` + `campinas` (SP) → one row, total 2, city = spelling of the earliest `created_at`
- [x] `Uberlândia` + `Uberlandia` (MG) → two rows
- [x] `Santa Rita` in SP and in MG → two rows, one per state
- [x] Empty table → empty list
- [x] Gate check passes: `./mvnw -q -B test`
- [x] Test count: ≥ 4 tests pass

**Status**: ✅ Complete

**Tests**: integration
**Gate**: full

**Commit**: `feat(customer): add location count aggregate query`

---

#### T2: Grouping service and response DTOs

**What**: `CustomerLocationService.groupByLocation()` building `LocationGroupingResponse` / `StateGroup` / `CityGroup` from rows, with unit tests.
**Where**: `src/main/java/com/example/customerapi/customer/location/CustomerLocationService.java` (+ the three DTO records, `CustomerLocationServiceTest.java`)
**Depends on**: T1
**Reuses**: `LocationCount` (T1)
**Requirement**: GEO-002, GEO-003, GEO-004, GEO-005, GEO-006, GEO-007, GEO-008

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Rows (SP Limeira 50, SP Campinas 100, MG Uberlândia 30) → MG(30: Uberlândia 30), SP(150: Campinas 100, Limeira 50)
- [x] Cities `campos do Jordão`, `Bauru`, `Águas de Lindóia` → `Águas de Lindóia`, `Bauru`, `campos do Jordão`
- [x] State total equals the sum of its cities; no rows → empty `states`
- [x] Gate check passes: quick
- [x] Test count: ≥ 5 tests pass

**Status**: ✅ Complete

**Tests**: unit
**Gate**: quick

**Commit**: `feat(customer): add customer location grouping service`

---

#### T3: Grouping endpoint

**What**: `CustomerLocationController` for `GET /api/v1/customers/grouped-by-location`, with end-to-end integration tests.
**Where**: `src/main/java/com/example/customerapi/customer/location/CustomerLocationController.java` (+ `CustomerLocationIntegrationTest.java`, test property `hibernate.generate_statistics`)
**Depends on**: T2
**Reuses**: `CustomerLocationService` (T2), CRUD endpoints to arrange data
**Requirement**: GEO-001 to GEO-017

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Spec's independent test data → exact JSON (MG then SP; Campinas before Limeira); no customer fields present anywhere in the body
- [x] No customers → 200 `{"states":[]}`
- [x] Delete and move scenarios (GEO-013 to GEO-015) reflected in the next call
- [x] One call with 3 states and 5 cities → Hibernate statistics: 1 prepared statement, 0 entity loads
- [x] Gate check passes: `./mvnw -B verify`
- [x] Test count: ≥ 7 tests pass

**Status**: ✅ Complete

**Tests**: integration
**Gate**: build

**Commit**: `feat(customer): add grouped-by-location endpoint`

---

### Phase 2: Verifier follow-ups

#### T4: Pin non-ASCII case folding and the accent tie-break

**What**: Regression test that non-ASCII case variants merge on PostgreSQL (GEO-018), and trace the existing accent tie-break test to GEO-019.
**Where**: `src/test/java/com/example/customerapi/customer/location/CustomerLocationQueryIntegrationTest.java` (+ comment in `CustomerLocationServiceTest.java`)
**Depends on**: T3
**Reuses**: `CustomerLocationQueryIntegrationTest` (T1), `CustomerLocationServiceTest` (T2)
**Requirement**: GEO-018, GEO-019

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `São Carlos` + `SÃO CARLOS` (SP) → one row, total 2, earliest spelling shown
- [x] `Uberlandia` before `Uberlândia` asserted and labelled GEO-019
- [x] Gate check passes: `./mvnw -B verify`
- [x] Test count: 235 tests pass (234 + 1)

**Tests**: integration
**Gate**: build

**Status**: ✅ Complete

**Commit**: `test(customer): pin non-ascii case folding and accent tie-break`

---

## Phase Execution Map

```
Phase 1:  T1 ------→ T2 ------→ T3
Phase 2:  T4
```

---

## Requirement Coverage

| Requirement | Tasks |
| ----------- | ----- |
| GEO-001 | T3 |
| GEO-002 to GEO-008 | T2, T3 |
| GEO-009 to GEO-012 | T1, T3 |
| GEO-013 to GEO-017 | T3 |
| GEO-018 to GEO-019 | T4 (GEO-019 also T2) |

All 19 requirements are mapped.
