# Hexagonal Architecture Validation (review-fix delta, after T18-T19)

## Validation: hexagonal-architecture - PASS ✅

**Verdict**: PASS

**Date**: 2026-09-24
**Spec**: `.specs/features/hexagonal-architecture/spec.md` (ARCH-01..ARCH-13)
**Diff range**: `9796beb..7068810`
- feature `9796beb..4cfd2ff` (T1-T5)
- fix iteration 1 `4cfd2ff..87626bf` (T6-T9)
- fix iteration 2 `87626bf..f2d371c` (T10-T12)
- fix iteration 3 `f2d371c..a11b2d3` (T13-T15)
- user-approved follow-up `a11b2d3..26b1b65` (`291cf1e` docs, `26b1b65` T16)
- round 5 `26b1b65..697af6c` (`697af6c` T17), recorded in `b5b76e4` (docs)
- **this delta `b5b76e4..7068810`**: `9009575` T18 (fix), `7068810` T19 (refactor), both from the PR #3 code review

**Verifier**: independent sub-agent (author ≠ verifier; did not write the code or the tests). Detached worktree `.worktrees/verify-hex7` at `7068810`, removed at the end.
**Round**: review-fix delta verification. Sensor round 7.

**Why PASS**:
- 13/13 ACs keep `file:line` evidence with matching outcomes. The gate is green: **304/304**, `./mvnw -B clean verify`, exit 0.
- **No behaviour change beyond the two fixes.** In `b5b76e4..7068810`, `src/main/resources`, every HTTP IT, the ArchUnit rules, runner and fixtures, `CustomerUpdateIntegrationTest` (CUST-24 `:282`) and `TransactionBoundaryIntegrationTest` (`:46`, `:58`) are byte-identical. They are all green. The only test change is +43/-0 in the adapter IT: two new tests, with no line removed.
- **T18 fixes a real regression against `feat/customer-api`.** At `9796beb`, `ApiExceptionHandler.java:85-86` mapped a commit-time `ObjectOptimisticLockingFailureException` to 409. The refactor replaced that handler with `ConcurrentCustomerUpdateException` (`ApiExceptionHandler.java:70-71` at HEAD). As a result, a concurrent DELETE failing at commit became a 500 at `b5b76e4`. Reverting `delete` to its `b5b76e4` form (D13) fails the new test with `ObjectOptimisticLockingFailureException: Unexpected row count … [delete from customer where id=? and version=?]`.
- **Question 4 is answered by a probe and by mutation: no update path issues an INSERT at HEAD.** The flag is not the only safeguard, and it matters only on the detached path (details below).
- **Sensor**: 20 counted mutations, 16 killed, 4 survived: 3 equivalent, 1 adversarial that predates this delta. There is **no plausible survivor**. Y7, Y8 and Z7c are still killed, on the same lines as in round 6.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1-T17 | ✅ Done | Verified in rounds 1-6; still green at HEAD |
| T18 | ✅ Done, verified | `CustomerPersistenceAdapter.java:47-49,63,66-74,114-128`; `CustomerJpaEntity.java:28,62-63,71,97-111`; tests `CustomerPersistenceAdapterIntegrationTest.java:130-138` and `:141-157`. "New tests failed first" is reproduced: D13 (delete reverted) fails `:155`, and D4/D7 (merge on insert) fail `:137` with `expected: 1L but was: 2L`. The gate passes with 304 |
| T19 | ✅ Done, verified | `LocationGroupingResponse.java:10-12`, `StateGroup.java:9-11`, `CityGroup.java:7-9`, `CustomerLocationController.java:22-24`. The location HTTP tests (STRICT JSON) are unchanged and green. D8, D9, D10b and D15 are killed by them |

---

## Gate Check

- **Command**: `./mvnw -B clean verify` in the clean worktree at `7068810`.
- **Result**: **304 passed, 0 failed, 0 errors, 0 skipped**. BUILD SUCCESS, exit 0, 51.8 s. Log: `scratchpad/hex7-gate.log`.
- **Test count history**: 240 at `9796beb` → 289 at `4cfd2ff` → 295 at `87626bf` → 297 at `f2d371c` → 300 at `a11b2d3` → 302 at `26b1b65` → 302 at `697af6c` → **304** at `7068810` (+2 adapter IT tests).
- **Deleted tests in `b5b76e4..7068810`**: none (`git diff --numstat -- src/test` = `43 0`, adapter IT only). Across the full range, only `customer/CustomerServiceTest` was deleted, and it was rewritten as `application/service/CustomerServiceTest` (17 tests, verified in round 1).

| Class | Tests |
| ----- | ----: |
| OperabilityIntegrationTest | 11 |
| CustomerApiSddApplicationTests | 1 |
| ClockConfigTest | 3 |
| HexagonalRulesDiscriminationTest | 17 |
| CustomerDeleteIntegrationTest | 4 |
| CustomerLocationIntegrationTest (HTTP) | 11 |
| ErrorContractIntegrationTest | 8 |
| CustomerLocationQueryIntegrationTest | 6 |
| CustomerPersistenceAdapterIntegrationTest | **11** (was 9) |
| SpringDataCustomerRepositoryIntegrationTest | 6 |
| CustomerSpecificationsIntegrationTest | 7 |
| CustomerRequestTest | 65 |
| CpfValidatorTest | 10 |
| CustomerUpdateIntegrationTest | 18 |
| CustomerListIntegrationTest | 16 |
| CustomerCreateAndGetIntegrationTest | 32 |
| CustomerServiceTest | 17 |
| CustomerSortTest | 21 |
| CustomerLocationServiceTest | 7 |
| TransactionBoundaryIntegrationTest | 4 |
| CpfTest | 12 |
| CustomerTest | 4 |
| HexagonalArchitectureTest | 13 |
| **Total** | **304** |

---

## Spec-Anchored Acceptance Criteria

**Abbreviations**:
- **HR**: rules file `src/test/java/com/example/customerapi/architecture/HexagonalRules.java`. The CPF rule is `:167-176`, the transactional rule `:158-164`, the layering rule `:179-183`.
- **HAT**: runner `HexagonalArchitectureTest.java:12`, `@AnalyzeClasses(packages = "com.example.customerapi", importOptions = DoNotIncludeTests)`.
- **HRD**: fixture proof `HexagonalRulesDiscriminationTest.java`. `:26-46` is the rule/fixture table (17 rows). `:52-53` asserts that `rule.check(FIXTURES)` throws.
- There is no `archunit.properties`, so `failOnEmptyShould=true`.

**Line stability (`b5b76e4..7068810`)**:
- `src/main` changed in 7 files:
  - `CityGroup` +7
  - `StateGroup` +7
  - `LocationGroupingResponse` +7
  - `CustomerLocationController` +1/-5
  - `CustomerJpaEntity` +26/-3
  - `CustomerPersistenceAdapter` +17/-12
- `src/test` changed only in the adapter IT (+43/-0).
- HR, HAT, HRD, every HTTP IT and `src/main/resources` are byte-identical.
- Shifted citations:
  - `CustomerJpaEntity.java` `@Entity` moved `:20` → **`:26`**.
  - In the adapter IT, lines after the imports shift by +7, and lines after the fields by +13. The round trip moved `:55-58` → **`:68-71`**. The stale-version update moved `:101-103` → **`:116-118`**.
  - `CustomerPersistenceAdapter.java:30` (class) and `CustomerLocationController.java:12` (`@RestController`) are unchanged.

| AC | Spec-defined outcome | `file:line` + assertion / rule | Result |
| -- | -------------------- | ------------------------------ | ------ |
| ARCH-01 | `..customer.domain..` depends on no `org.springframework..`, `jakarta..`, `org.hibernate..` or `..adapter..` | HR:77-82; HAT:16; HRD:26 (`SpringAwareDomainObject`). Round 1: M7a/M7b killed. The delta adds `Persistable` only in `adapter.out.persistence`; the domain is untouched | ✅ PASS |
| ARCH-02 | The domain customer has no JPA annotations and is persisted through a separate JPA entity | `domain/Customer.java:11` plain `public final class`; `adapter/out/persistence/CustomerJpaEntity.java:26` `@Entity`; HR:134-138; HAT:40; HRD:35; round trip `CustomerPersistenceAdapterIntegrationTest.java:68-71` (details, createdAt, updatedAt, version) | ✅ PASS |
| ARCH-03 | The CPF rule is implemented once, in the domain; the web validator delegates | `domain/Cpf.java:11-25`; `CpfValidator.java:14`; `CpfTest.java:14,22`; HR:167-176; HAT:52; HRD:40-45. R-A1 killed in round 6; files unchanged | ✅ PASS |
| ARCH-04 | `..customer.application..` is free of `..adapter..`, `jakarta.persistence..`, `org.hibernate..`, `org.springframework.data.jpa..`, `org.springframework.web..` | HR:85-91; HAT:19-20; HRD:27. The application layer is unchanged in the delta | ✅ PASS |
| ARCH-05 | Each use case is an interface in `..application.port.in..` | `port/in/CreateCustomerUseCase.java:6`, `GetCustomerUseCase.java:7`, `UpdateCustomerUseCase.java:8`, `DeleteCustomerUseCase.java:5`, `ListCustomersUseCase.java:8`, `GroupCustomersByLocationUseCase.java:7`; HR:94-100; HAT:23; HRD:28; consumer `CustomerLocationController.java:15-18,23` | ✅ PASS (completeness rests on compilation) |
| ARCH-06 | Persistence operations are `port.out` interfaces; services depend on them | `port/out/CustomerPersistencePort.java:16`, `LocationCountPort.java:7`; `CustomerService.java:35,39`; HR:103-108; HAT:26; HRD:29; `CustomerServiceTest.java:58-59` | ✅ PASS |
| ARCH-07 | `..adapter.in.web..` depends on no `..adapter.out..` and no `..application.port.out..` | HR:111-116; HAT:29; HRD:30-31. The new DTO factories import only `domain.StateLocation` / `domain.CityLocation` (`LocationGroupingResponse.java:5`, `StateGroup.java:5`, `CityGroup.java:3`), which is allowed; HAT is green | ✅ PASS |
| ARCH-08 | `..adapter.out.persistence..` depends on no `..adapter.in..` | HR:119-124; HAT:32-33; HRD:32 | ✅ PASS |
| ARCH-09 | Every output port is implemented by a class in `..adapter.out..` | `CustomerPersistenceAdapter.java:30`; HR:127-131; HAT:36-37; HRD:33-34; `CustomerApiSddApplicationTests.java:12` | ✅ PASS |
| ARCH-10 | `@RestController` in `..adapter.in.web..`; `@Entity` and Spring Data repositories in `..adapter.out.persistence..` | HR:148-152, 134-138, 141-145; HAT:40-46; HRD:35-37; `CustomerController.java:34`, `CustomerLocationController.java:12`, `SpringDataCustomerRepository.java:10`, `CustomerJpaEntity.java:26` | ✅ PASS |
| ARCH-11 | Every HTTP IT of customer-management and geo-grouping passes, with assertions unchanged | Gate: CreateAndGet 32, Update 18, List 16, Delete 4, ErrorContract 8, Location 11, Operability 11, all green. HTTP ITs have **zero diff** in `b5b76e4..7068810`. CUST-12 PUT `CustomerUpdateIntegrationTest.java:214/216/218/219`, cpf `:227/229/231/232`; POST `CustomerCreateAndGetIntegrationTest.java:173,185`; CUST-24 `:279` 409, `:282` optimistic failures == 1, `:284-288` stored values. **The delta also restores the `feat/customer-api` 409 for a concurrent DELETE** (adapter IT `:141-157`; HTTP mapping `ApiExceptionHandler.java:70-73`) | ✅ PASS |
| ARCH-12 | Grouping runs exactly one SQL statement and loads zero JPA entities | `CustomerLocationIntegrationTest.java:178` `getPrepareStatementCount()).isEqualTo(1)`, `:179` `getEntityLoadCount()).isZero()`. The T19 factories map records in memory; no query is added | ✅ PASS |
| ARCH-13 | Flyway migrations unchanged; Hibernate schema validation passes | `git diff --stat 9796beb..7068810 -- src/main/resources` is empty; `application.properties:9` `ddl-auto=validate`; context load passes. The `@Transient newRow` flag (`CustomerJpaEntity.java:62-63`) is not a column, and validation passes | ✅ PASS |

**Status**: 13/13 ACs have `file:line` evidence, and the spec outcomes match.

---

## Delta Findings (`b5b76e4..7068810`)

### D-1. Behaviour audit

| Change | Behaviour at `b5b76e4` | Behaviour at `7068810` | Judgement |
| ------ | ---------------------- | ---------------------- | --------- |
| `write(id, supplier)` now also catches `ObjectOptimisticLockingFailureException` (`:118-120`) and is generic (`<T>`) | Update had its own inner catch; insert and delete had none | One translation for insert, update and delete | Same for update. Insert cannot raise an optimistic failure (new row). Delete is the fix |
| `delete` = `write(id, deleteById + flush)` (`:68-74`) | `deleteById` only; a stale version failed at commit as `ObjectOptimisticLockingFailureException` → no handler → **500** | Flushed inside `write` → `ConcurrentCustomerUpdateException` → **409** (`ApiExceptionHandler.java:70-73`) | **Fix.** It restores `9796beb` (`ApiExceptionHandler.java:85-86` mapped the ORM exception to 409). The normal delete still returns 204 (CUST-25..28 green) |
| `CustomerJpaEntity implements Persistable<UUID>`, `newRow` set in `from()`, cleared by `@PostPersist`/`@PostLoad` | `saveAndFlush` on a new entity → `merge` → SELECT + INSERT | `persist` → one INSERT (`:137` `== 1`) | **Fix** (performance only). CUST-12 duplicate translation is unchanged (`:76-93` green, HTTP `:214/:227` green) |
| `getId()` became `public` (`Persistable` contract) | package-private | public on a package-private class | No reach outside the package; ArchUnit green |
| T19 DTO factories | Inline lambdas in the controller | `LocationGroupingResponse.from` → `StateGroup.from` → `CityGroup.from` | Identical mapping: same fields, same order, no sort, no filter. STRICT JSON tests are unchanged and green |

- **ARCH rules**: HAT 13/13 and HRD 17/17 are green at HEAD, and the rule files are byte-identical.
- **CUST-24 invariant**: `CustomerUpdateIntegrationTest.java:282` (optimistic failure count 1) is green at HEAD and still kills R-Y7, R-Y8 and R-Z7c (below).
- **Transaction count**: `TransactionBoundaryIntegrationTest.java:46,58` are green at HEAD, and `:58` still kills R-Y7 and R-Y8.
- **Docs**:
  - The `CustomerPersistencePort.java` javadoc ("a stale version becomes `ConcurrentCustomerUpdateException`") is now true for delete as well.
  - `design.md:78-80` still holds.
  - Minor: `DeleteCustomerUseCase.java:7` mentions only `CustomerNotFoundException`, not the 409 route (cosmetic).

### D-2. Question 4: can an update issue an INSERT?

**Answer: no, not at HEAD, on any update path.** A verifier-only probe test (`scratchpad/sensor7/VerifierT18ProbeIntegrationTest.java`) was copied into the worktree for each run and discarded afterwards. It uses Hibernate statistics (`getEntityInsertCount`, `getEntityUpdateCount`):

| Probe shape | HEAD (P0) | D3 `isNew()` always true (P1) | D5+D6 both callbacks removed (P2) | D6 no `@PostPersist` (P3) | D5 no `@PostLoad` (P4) |
| ----------- | --------- | ---------------------------- | --------------------------------- | ------------------------- | ---------------------- |
| Request-shaped update (one tx: `findById` then `update`, as `CustomerService` does) | inserts 0, updates 1, 2 stmts | **inserts 0**, updates 1 | inserts 0 | inserts 0 | inserts 0 |
| Standalone update (no outer tx; the adapter's entity is detached) | inserts 0, updates 1, 3 stmts (select, merge-select, update) | **INSERT → `duplicate key … "customer_pkey"`** (`DataIntegrityViolationException`) | inserts 0 | inserts 0 | inserts 0 |
| Insert + update in one tx | inserts 1, updates 1 | inserts 1, updates 1 | inserts 1, updates 1 | inserts 1, updates 1 | inserts 1, updates 1 |
| Insert + delete in one tx | row removed | **row kept** | **row kept** | **row kept** | row removed |
| `repository.findById(..).isNew()` | false | **true** | false | false | false |

Why this holds:
1. On the only production update route, `CustomerService.update` runs in `@Transactional`, and `adapter.update` gets the managed entity from `repository.findById`. Spring Data calls `persist` or `merge` based on `isNew()`. Even if `isNew()` were wrongly true, `persist` on a managed entity is a JPA no-op, and flush issues the versioned UPDATE (P1 request-shaped: 0 inserts).
2. A loaded entity is never "new". Hibernate instantiates it through the protected no-arg constructor, so `newRow` defaults to `false`. `@PostLoad` is therefore redundant (D5 is equivalent; P4 is identical to HEAD).
3. The flag matters only for a detached entity. When `isNew()` is true, `persist` issues an INSERT and hits the primary key. This is the round-6 D3 question answered: **yes, on the detached path**. At HEAD it cannot happen, because only `from()` sets the flag and `update` never writes a `from()` entity. D12 (update writes `CustomerJpaEntity.from(customer)`, the round-6 W9 that used to be *equivalent*) is now **killed** by 15 failures and 2 errors: every PUT returns 500, and the adapter IT fails with duplicate-key / entity-exists.
4. The existing suite guards the flag: D3 is killed by 8 failures and 2 errors (adapter IT `:100` and `:114` duplicate `customer_pkey`; DELETE HTTP tests `:30/:42/:53`, because `SimpleJpaRepository.delete` skips "new" entities; location ITs). D4 and D7 are killed by `:137` (`2L`).

### D-3. Latent adapter-contract nuance (not reachable)

Without `@PostPersist` (D6), the managed instance returned by `insert` stays "new" for the rest of its persistence context. A `delete` in the same transaction then silently does nothing (P3: the row is kept). No production path inserts and deletes in one transaction: `create` only inserts, and each request is one transaction. The full suite passes under D6, so it is classified as **equivalent**. `@PostPersist` is the load-bearing callback here, and `@PostLoad` is defensive.

---

## ARCH-11: HTTP Integration-Test Assertion Diff (`9796beb..7068810`)

**Carried over (rounds 1-6, `9796beb..697af6c`)**:
- HTTP ITs with zero diff: `CustomerDeleteIntegrationTest`, `CustomerListIntegrationTest`, `ErrorContractIntegrationTest`, `customer/location/CustomerLocationIntegrationTest`, `CustomerApiSddApplicationTests`.
- Sanctioned changes:
  - the `HttpIntegrationTestSupport` spy type, the `stored(UUID)` and `preCheckMisses` helpers;
  - the logger fragment `service.CustomerService` (`OperabilityIntegrationTest.java:132`);
  - `stored(id)` extractors (`CustomerCreateAndGetIntegrationTest.java:54,84,210`, `CustomerUpdateIntegrationTest.java:132`);
  - the CUST-24 arrangement through the port (`:259-289`);
  - T16 transaction tests;
  - T17's +12/-0 count assertion (`:276-282`).
- None weakens an expected value.

**This delta (`b5b76e4..7068810`)**: **no HTTP IT changed** (`git diff --stat` over `src/test/java/com/example/customerapi/*.java`, `customer/*.java`, `customer/location`, `architecture` is empty). The only test diff is the adapter IT, +43/-0: imports, two autowired fields and two new tests.

**Conclusion**: no asserted expected value is weakened across `9796beb..7068810`.

---

## Discrimination Sensor

### Isolation

- All runs used the detached worktree `.worktrees/verify-hex7` at `7068810`, with `./mvnw -B clean test` (full suite, except the probe runs, which used `-Dtest=VerifierT18ProbeIntegrationTest`).
- Mutations were applied by exact-match replacement (`sensor7/mut.py`, which asserts exactly one occurrence) or by `git apply` of the round-6 patches (`sensor7/patches/`; each passed `git apply --check` at `7068810`).
- `sensor7/run.sh` refuses to run when nothing changed or a mutation step failed. After each run it executes `git reset -q --hard HEAD && git clean -fdq src`. **`porcelain-after: []` was logged after all 25 runs** (`batch1.out` 5, `batch2.out` 18, `batch3.out` 2). The worktree porcelain was empty at the end, before removal.
- The main tree was clean on `refactor/hexagonal` at `7068810` before and after. There was no stash, commit, push or branch change.
- Artifacts: `scratchpad/sensor7/<id>.diff|.log`, `batch1.sh`..`batch3.sh`, `batch*.out`.

### Rounds 1-6 (carried over)

| Round | At | Mutations | Killed | Survived |
| ----- | -- | --------: | -----: | -------- |
| 1 | `4cfd2ff` | 20 | 16 | 4: M3b, M10, M11b (P); M7c (E) |
| 2 | `87626bf` | 14 + 1 control | 12 | 2: N3, N4b (P), plus the vacuous N1b control |
| 3 | `f2d371c` | 22 | 17 | 5: X5 (P); A1 (A); X14, X15 (C); X16 (E) |
| 4 | `a11b2d3` | 19 + 2 controls | 12 | 7: Y7, Y8 (P); R-X14b, R-X15, Y3+X5, Y5b (C); Y9 (E) |
| 5 | `26b1b65` | 22 + 2 controls + 5 experiment runs | 18 | 4: Z7b, Z7c (P); Z5b (C); Z6 (E) |
| 6 | `697af6c` | 25 (26 runs) | 22 | 3: Z7c+W9+M1 (C); W6, W9 (E) |

### Round 7 (this delta, `7068810`)

**Regression sample** (full suite each):

| # | Mutation | Result |
| - | -------- | ------ |
| R7-Y7 | `CustomerService.update` writes in a `REQUIRES_NEW` `TransactionTemplate` | ✅ Killed (302/304): `CustomerUpdateIntegrationTest…:282` `1L` vs **`0L`**; `TransactionBoundaryIntegrationTest.updateCompletesExactlyOneTransaction:58` |
| R7-Y8 | `@Transactional(REQUIRES_NEW)` on `CustomerPersistenceAdapter.update` | ✅ Killed (302/304): `:282` (`0L`); TB `:58` `1L` vs `2L` |
| R7-Z7c | `entityManager.clear()` before the reload in `update` | ✅ Killed (303/304): `:282` `1L` vs **`0L`** |

**New mutations on the delta**. `P` = plausible, `A` = adversarial, `C` = compound, `E` = equivalent.

| # | Class | Site (HEAD) | Mutation | Result |
| - | ----- | ----------- | -------- | ------ |
| D1 | P | `CustomerPersistenceAdapter.java:71` | Drop `repository.flush()` in `delete` | ✅ Killed (303/304): adapter IT `:155`, `ObjectOptimisticLockingFailureException` thrown at commit instead of `ConcurrentCustomerUpdateException` |
| D2 | P | `:118-120` | Drop the `ObjectOptimisticLockingFailureException` catch from `write` | ✅ Killed (302/304): CUST-24 HTTP `:279` 409 → **500**; adapter IT `:155` |
| D3 | P | `CustomerJpaEntity.java:104` | `isNew()` always `true` | ✅ Killed (294/304, 8F + 2E): adapter IT `:100`, `:114` (**duplicate key `customer_pkey` on INSERT**: standalone update of a detached entity), `:148`, `:165`; `CustomerDeleteIntegrationTest:30,42,53`; location ITs `:105,:143,:159` (delete skipped) |
| D4 | P | `:104` | `isNew()` always `false` | ✅ Killed (303/304): `insertIssuesOnlyTheInsertStatement:137` `1L` vs `2L` |
| D5 | E | `:108` | Remove `@PostLoad` | ⚪ Survived (304/304). Equivalent: Hibernate builds loaded entities through the no-arg constructor, so `newRow` is already `false`. Probe P4 is identical to HEAD |
| D6 | E | `:107` | Remove `@PostPersist` | ⚪ Survived (304/304). The only divergence is insert+delete in one persistence context (the delete is skipped, probe P3), which no production path does. Update is unaffected (P3: 0 inserts) |
| D7 | P | `:71` | `from()` no longer sets `newRow = true` | ✅ Killed (303/304): `:137` `1L` vs `2L` |
| D8 | P | `StateGroup.java:10` | State total ← first city's total | ✅ Killed (300/304): `CustomerLocationIntegrationTest:48,82,105,123` `totalCustomers` |
| D9 | P | `StateGroup.java:10` | Drop a city (`.skip(1)`) | ✅ Killed (295/304): location IT `:48,82,105,159,201` … `Expected N values but got N-1` |
| D10 | E | `LocationGroupingResponse.java:11` | Re-sort states by UF | ⚪ Survived (304/304). Equivalent: the service already orders states by UF (GEO-005), and UFs are unique, so the sort is the identity |
| D10b | P | `:11` | Re-sort states by total, descending | ✅ Killed (302/304): location IT `:48`, `:123` `states[0].state` |
| D11 | P | `CustomerPersistenceAdapter.java:68-74` | `deleteById` + `flush` outside `write` | ✅ Killed (303/304): `:155` |
| D12 | P | `:63` | Update writes `CustomerJpaEntity.from(customer)` (round-6 W9, then equivalent) | ✅ **Killed now** (287/304, 15F + 2E): every PUT → 500 (`CustomerUpdateIntegrationTest:95,128,145,214,227,237,279`, TB `:56,:84`, Operability `:74`, location moves `:240`); a `from()` entity is `isNew` → `persist` of a second instance with an existing id |
| D13 | P (control) | `:68-74` | Revert `delete` to `b5b76e4` (`deleteById` only) | ✅ Killed (303/304): `:155`, `ObjectOptimisticLockingFailureException: Unexpected row count … [delete from customer where id=? and version=?]`. This confirms "new test failed first" |
| D14 | P | `:70-71` | `flush()` before `deleteById` (the delete runs at commit) | ✅ Killed (303/304): `:155` |
| D15 | P | `CityGroup.java:8` | City total → constant `1` | ✅ Killed (300/304): location IT `:48,82,159,201` `cities[0].totalCustomers` |
| D16 | A (predates this delta) | `StateGroup.java:10` | Re-sort cities by plain `String` order in the web DTO | ⚪ Survived (304/304). HTTP location data never separates plain order from pt-BR collation order. GEO-006 is pinned only at the service level (`CustomerLocationServiceTest:59-63`). The geo-grouping validation recorded this as accepted informational gap 4 ("GEO-006 is discriminated only at the service unit level"). The identical injection into the `b5b76e4` controller lambda survives the same byte-identical HTTP tests, so T19 did not introduce it |

**Probe experiment runs (not counted as mutations)**: P0 (HEAD), P1 (D3), P2 (D5+D6), P3 (D6) and P4 (D5) with `VerifierT18ProbeIntegrationTest`. See D-2.

**Round 7 result**:

| Group | Mutations | Killed | Survived |
| ----- | --------: | -----: | -------- |
| Regression sample (R7-Y7, R7-Y8, R7-Z7c) | 3 | 3 | 0 |
| Persistence delta, plausible (D1, D2, D3, D4, D7, D11, D12, D13, D14) | 9 | 9 | 0 |
| Persistence delta, equivalent (D5, D6) | 2 | 0 | 2 E |
| Location DTO delta, plausible (D8, D9, D10b, D15) | 4 | 4 | 0 |
| Location DTO delta, equivalent / adversarial (D10, D16) | 2 | 0 | 1 E, 1 A |
| **Total** | **20** | **16** | **4** (0 P, 1 A, 0 C, 3 E) |

**Classification rationale**:
- **D5 (E)**: `@PostLoad` sets a value the constructor already gives. No observable difference at any level (P4).
- **D6 (E)**: the divergence exists only for a caller that inserts and deletes the same customer in one persistence context, and no use case does that. It is recorded as a residual risk, because the adapter's port contract would silently skip the delete in that case.
- **D10 (E)**: sorting by the key the input is already sorted by, with unique keys, is the identity.
- **D16 (A, pre-existing)**: it injects new logic (a re-sort) rather than altering existing tokens, and a pure mapping factory gives no reason for it. It also survives on pre-delta code with the same tests, and it matches a gap already accepted in the geo-grouping verification. It does not affect the verdict. It is listed as ranked gap 1 because it is cheap to close.
- **D12 turned from equivalent (round 6 W9) to killed**: `Persistable` makes a `from()`-built entity "new", so the merge-based write route is no longer available. This tightens the design statement `design.md:78` ("`update` loads the managed entity").

**Sensor depth**: expanded (data integrity and concurrency). **Verdict impact**: none. There is no plausible survivor.

---

## Code Quality (T18-T19)

| Check | Status |
| ----- | ------ |
| Minimum code / no scope creep | ✅ One translation helper, one flush, one `Persistable` flag, three factories. The skipped review findings (#3, #4, #7) are documented with rationale in `tasks.md` Phase 6 |
| Surgical changes | ✅ 7 main files, 1 test file (+43/-0). HTTP tests, rules and resources are untouched |
| Matches patterns | ✅ Reuses the `write()` translation and the statistics / `TransactionTemplate` + `REQUIRES_NEW` race pattern from the CUST-24 tests |
| Tests map to findings | ✅ `:130-138` → finding #2 (one INSERT); `:141-157` → finding #1 (DELETE 409) |
| Doc accuracy | ✅ The `CustomerJpaEntity` javadoc (`:20-24`) and the adapter javadoc (`:66`, `:109-113`) match the behaviour. Minor: `DeleteCustomerUseCase.java:7` does not mention the conflict |

---

## Spec-Precision Gaps

1. **ARCH-11 wording** (carried over): "assertions unchanged" should read "expected values unchanged".
2. **ARCH-03 enforcement strength** (carried over).
3. **ARCH-05 / ARCH-09 completeness** (carried over): rests on compilation and the context load.
4. **CUST-24 guard** (carried over): enforced as an invariant (`CustomerUpdateIntegrationTest.java:282`), not as an ARCH AC.
5. **Concurrent DELETE (new)**: the 409 for a DELETE racing an UPDATE is a `feat/customer-api` behaviour that the ARCH-11 HTTP suite never pinned. That is why the regression passed rounds 1-6. It is now guarded only at the adapter level (`:141-157`). The HTTP mapping of `ConcurrentCustomerUpdateException` → 409 is proven by CUST-24 `:279`.

---

## Residual Risks (not verdict-failing)

| Risk | Classification | One-line mitigation |
| ---- | -------------- | ------------------- |
| D16: a re-sort in the web DTO changing city order survives the HTTP suite | Adversarial, predates this delta (geo-grouping gap 4) | Add one HTTP location case with `Águas de Lindóia`, `Bauru`, `campos do Jordão` |
| No HTTP-level test for a concurrent DELETE → 409 | Low | The adapter IT pins the translation, and CUST-24 pins the HTTP mapping. Optionally add an HTTP race test |
| D6: without `@PostPersist`, insert + delete in one persistence context skips the delete | Equivalent (unreachable today) | Keep `@PostPersist`; optionally add an adapter IT for insert-then-delete in one transaction |
| D5: `@PostLoad` is redundant | Equivalent | Keep as a defensive marker |
| `isNew()` true on a detached entity → INSERT with duplicate PK | Guarded (D3, D12 killed) | None |
| W6 / W9 / Z7c+W9+M1 (round 6) | E / C | W9 is now killed (D12). Otherwise unchanged |
| Hibernate statistics are SessionFactory-global (review finding #7, skipped) | Low (flakiness) | Tests run sequentially in one context. The new `:137` also relies on this |
| Z5b, Z6, R-X14b, R-X15, Y3+X5, Y5b; reflection routes around the CPF rule; X3, X16 | Compound / equivalent / adversarial | Unchanged from round 6 |

---

## Ranked Gaps

None that fail the bar.

1. **[Low, test-only, predates this delta]** D16: pin GEO-006 city ordering end to end with one STRICT HTTP case whose plain and pt-BR orders differ.
2. **[Low, test-only]** Add an HTTP-level concurrent DELETE → 409 test (spec gap 5), or an adapter IT for insert-then-delete in one transaction (D6).
3. **[Cosmetic]** `DeleteCustomerUseCase.java:7` javadoc: mention `ConcurrentCustomerUpdateException`.
4. **[Cosmetic, carried over]** `design.md:127` wording (round 6). It matters less now that D12 is killed.

---

## Requirement Traceability Update (proposed)

| Requirement | Previous (round 6) | New Status |
| ----------- | ------------------ | ---------- |
| ARCH-01..10, 12, 13 | ✅ Verified | ✅ Verified (rules and fixtures byte-identical; HAT/HRD green) |
| ARCH-11 | ✅ Verified | ✅ Verified. No HTTP IT changed in the delta; the concurrent-DELETE 409 from `feat/customer-api` is restored |
| Design guard: CUST-24 stale write reaches Hibernate's version check | ✅ Enforced | ✅ Enforced (R7-Y7, R7-Y8, R7-Z7c killed at `:282`; D2 killed at `:279`) |
| Review findings #1, #2, #5, #6 (T18-T19) | n/a | ✅ Verified (D1, D11, D13, D14 → #1; D4, D7 → #2; D8, D9, D10b, D15 → #5; D2 → #6) |

---

## Summary

- **Overall**: ✅ Ready. T18 fixes a real 409 → 500 regression for a concurrent DELETE and removes the merge SELECT on insert. T19 is a behaviour-preserving mapping refactor. No HTTP test, ARCH rule or CUST-24 / transaction-count assertion changed, and all are green.
- **Spec-anchored check**: 13/13.
- **Gate**: 304 passed, 0 failed, 0 skipped (`./mvnw -B clean verify`, 51.8 s).
- **Question 4**: no update path issues an INSERT at HEAD. The request-shaped update gives 0 inserts even with `isNew()` forced true, because `persist` on a managed entity is a no-op. The detached path would INSERT and hit `customer_pkey`, but only a `from()` entity is new, `update` never writes one, and D3 and D12 are both killed.
- **Sensor, all seven rounds**:

| Round | At | Mutations | Killed | Survived |
| ----- | -- | --------: | -----: | -------- |
| 1 | `4cfd2ff` | 20 | 16 | 4: 3 P, 1 E |
| 2 | `87626bf` | 14 + 1 control | 12 | 2 P, plus the N1b control |
| 3 | `f2d371c` | 22 | 17 | 5: 1 P, 1 A, 2 C, 1 E |
| 4 | `a11b2d3` | 19 + 2 controls | 12 | 7: 2 P, 4 C, 1 E |
| 5 | `26b1b65` | 22 + 2 controls + 5 experiment runs | 18 | 4: 2 P, 1 C, 1 E |
| 6 | `697af6c` | 25 (26 runs) | 22 | 3: 1 C, 2 E |
| **7** | **`7068810`** | **20** (+5 probe runs) | **16** | **4: 0 P, 1 A (D16, pre-existing), 3 E (D5, D6, D10)** |

Note: per the dispatch, this report lives in the session scratchpad, not in `.specs/features/hexagonal-architecture/validation.md`. `validate_state.py` and `lessons.py` were not run.
