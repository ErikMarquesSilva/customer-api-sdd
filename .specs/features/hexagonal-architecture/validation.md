# Hexagonal Architecture Validation (focused re-verification 5, after T17)

## Validation: hexagonal-architecture - PASS ✅

**Verdict**: PASS

**Date**: 2026-09-23
**Spec**: `.specs/features/hexagonal-architecture/spec.md` (ARCH-01..ARCH-13)
**Diff range**: `9796beb..697af6c`
- feature `9796beb..4cfd2ff` (T1-T5)
- fix iteration 1 `4cfd2ff..87626bf` (T6-T9)
- fix iteration 2 `87626bf..f2d371c` (T10-T12)
- fix iteration 3 `f2d371c..a11b2d3` (T13-T15)
- user-approved follow-up `a11b2d3..26b1b65` (`291cf1e` docs, `26b1b65` T16)
- this round `26b1b65..697af6c` (`697af6c` T17)

**Verifier**: independent sub-agent (author ≠ verifier; did not write the code or the tests). Detached worktree `.worktrees/verify-hex6` at `697af6c`, removed at the end.
**Round**: focused re-verification 5 (final round before escalation). Sensor round 6.

**Why PASS**:
- 13/13 ACs have `file:line` evidence with matching outcomes. The gate is green (**302/302**, `./mvnw -B clean verify`, exit 0).
- **T17 kills the round-5 survivors.** Reproduced independently, full suite each: Z7b and Z7c now fail `CustomerUpdateIntegrationTest.concurrentUpdateCommittedFirstMakesThisUpdateReturn409AndKeepsTheFirstCommit:282` with `expected: 1L but was: 0L`. Y7 and Y8 fail the same line (`1L` vs `0L`) **and** `TransactionBoundaryIntegrationTest.updateCompletesExactlyOneTransaction:58` (`1L` vs `2L`).
- **Every earlier plausible and adversarial survivor is killed**, including M1, X5, N3, M3b, A1, Z1 and Z2 in the regression sample (11/11 re-runs killed).
- **11 new mutations plus 3 compound follow-ups attacked the new assertion.** 11 of the 14 are killed. The 3 survivors (W6, W9, Z7c+W9+M1) are **equivalent or compound, and none weakens CUST-24**: each still rejects the stale write in Hibernate against the version the service read, or only removes a defensive `clear()`. There is **no surviving plausible mutant**.
- `src/main` is unchanged in `26b1b65..697af6c`. The only removed test line is a javadoc sentence. No assertion is weakened. The design.md and javadoc wording matches the code.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1-T15 | ✅ Done | Verified in rounds 1-5; still green at HEAD |
| T16 | ✅ Done | `TransactionBoundaryIntegrationTest.java:46-47,58-59`. It still kills Y7, Y8, Z1 and Z2 this round |
| T17 | ✅ Done, verified | `CustomerUpdateIntegrationTest.java:276-277` clears the statistics right before the request; `:282` asserts `getOptimisticFailureCount() == 1`. Every "Done when" box is confirmed: 1 at HEAD (green); Y7, Y8, Z7b and Z7c fail with `1L` vs `0L`; the design.md and javadoc wording is corrected; the gate passes |

---

## Gate Check

- **Command**: `./mvnw -B clean verify` in the clean worktree at `697af6c`.
- **Result**: **302 passed, 0 failed, 0 errors, 0 skipped**. BUILD SUCCESS, exit 0, 35.8 s. Log: `scratchpad/sensor6/gate.log`.
- **Test count history**: 240 at `9796beb` → 289 at `4cfd2ff` → 295 at `87626bf` → 297 at `f2d371c` → 300 at `a11b2d3` → 302 at `26b1b65` → **302** at `697af6c`. T17 adds an assertion, not a test.
- **Deleted tests in `26b1b65..697af6c`**: none. Across the full range, only `customer/CustomerServiceTest` was deleted, and it was rewritten as `application/service/CustomerServiceTest` (17 tests, verified in round 1).

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
| CustomerPersistenceAdapterIntegrationTest | 9 |
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
| **Total** | **302** |

---

## Spec-Anchored Acceptance Criteria

**Abbreviations**:
- **HR**: rules file `src/test/java/com/example/customerapi/architecture/HexagonalRules.java`. The CPF rule is `:167-176`, the transactional rule `:158-164`, the layering rule `:179-183`.
- **HAT**: runner `HexagonalArchitectureTest.java:12`, `@AnalyzeClasses(packages = "com.example.customerapi", importOptions = DoNotIncludeTests)`.
- **HRD**: fixture proof `HexagonalRulesDiscriminationTest.java`. `:26-46` is the rule/fixture table (17 rows). `:52-53` asserts that `rule.check(FIXTURES)` throws an `AssertionError` whose message contains the fixture.
- There is no `archunit.properties`, so `failOnEmptyShould=true`.

**Line stability**: `git diff --numstat 26b1b65..697af6c -- src` touches only `CustomerUpdateIntegrationTest.java` (+12/-0) and `TransactionBoundaryIntegrationTest.java` (+1/-1, javadoc). Production code, HR, HAT, HRD and every other test file are byte-identical to `26b1b65`. In `CustomerUpdateIntegrationTest.java`, lines after `:20` shift by +4 (imports), and lines after `:46` shift by +7 (imports plus the `EntityManagerFactory` field). The citations below use HEAD line numbers.

**Spot-checked this round at `697af6c`** (15 citations, read via `git show 697af6c:<path>`; all match):
- `domain/Customer.java:11` `public final class Customer {`
- `adapter/in/web/validation/CpfValidator.java:14` `return value == null || com.example.customerapi.customer.domain.Cpf.isValid(value);`
- `adapter/out/persistence/CustomerPersistenceAdapter.java:30` `class CustomerPersistenceAdapter implements CustomerPersistencePort, LocationCountPort {`
- `adapter/out/persistence/SpringDataCustomerRepository.java:10` `public interface SpringDataCustomerRepository`
- `adapter/in/web/CustomerController.java:34` `@RestController`
- HR:77-82 (ARCH-01 package list, incl. `"org.springframework..", "jakarta..", "org.hibernate..", ADAPTER, APPLICATION`); HR:85-91 (ARCH-04 list); HR:158-164; HR:167-176 (`callMethod(Cpf.class,"isValid",String.class)` + `onlyCallCodeUnitsThat(...)`); HR:179-183
- HAT:12
- HRD:26-46 (17 rows) and `:52-53`
- `customer/location/CustomerLocationIntegrationTest.java:178` `getPrepareStatementCount()).isEqualTo(1)`, `:179` `getEntityLoadCount()).isZero()`
- `application.properties:9` `spring.jpa.hibernate.ddl-auto=validate`
- `OperabilityIntegrationTest.java:132` `.contains(" INFO ").contains("service.CustomerService")`
- the six `port/in/*UseCase.java` files, each `public interface` (`:6`, `:7`, `:8`, `:5`, `:8`, `:7`)
- `CustomerPersistenceAdapterIntegrationTest.java:101-103` (stale-version update throws `ConcurrentCustomerUpdateException`; the first write is kept)
- `CustomerUpdateIntegrationTest.java:214/216/218/219` (CUST-12 PUT email) and `:227/229/231/232` (cpf)
- `git diff --stat 9796beb..697af6c -- src/main/resources` is empty

| AC | Spec-defined outcome | `file:line` + assertion / rule | Result |
| -- | -------------------- | ------------------------------ | ------ |
| ARCH-01 | `..customer.domain..` depends on no `org.springframework..`, `jakarta..`, `org.hibernate..` or `..adapter..` | HR:77-82 `noClasses().that().resideInAPackage(DOMAIN).should().dependOnClassesThat().resideInAnyPackage("org.springframework..","jakarta..","org.hibernate..",ADAPTER,APPLICATION)`; HAT:16; HRD:26 (`SpringAwareDomainObject`). Round 1: M7a/M7b killed | ✅ PASS |
| ARCH-02 | The domain customer has no JPA annotations and is persisted through a separate JPA entity | `domain/Customer.java:11` plain `public final class`; `adapter/out/persistence/CustomerJpaEntity.java:20` `@Entity`; HR:134-138; HAT:40; HRD:35 (`JpaEntityInDomain`); round trip `CustomerPersistenceAdapterIntegrationTest.java:55-58` (details, createdAt, updatedAt, version). Round 1: M4a/b/c killed | ✅ PASS |
| ARCH-03 | The CPF rule is implemented once, in the domain; the web validator delegates | `domain/Cpf.java:11-25`; `CpfValidator.java:14` delegates; `CpfTest.java:14,22`; HR:167-176; HAT:52; HRD:40-45. **Re-run this round**: R-A1 is killed by `cpfValidationDelegatesToDomain` ("calls constructor `CpfDigits.<init>(String)` in (CpfValidator.java:15)") | ✅ PASS |
| ARCH-04 | `..customer.application..` is free of `..adapter..`, `jakarta.persistence..`, `org.hibernate..`, `org.springframework.data.jpa..`, `org.springframework.web..` | HR:85-91 (the exact list); HAT:19-20; HRD:27 (`JpaAwareService`) | ✅ PASS |
| ARCH-05 | Each use case (create, get, update, delete, list, group) is an interface in `..application.port.in..` | `port/in/CreateCustomerUseCase.java:6`, `GetCustomerUseCase.java:7`, `UpdateCustomerUseCase.java:8`, `DeleteCustomerUseCase.java:5`, `ListCustomersUseCase.java:8`, `GroupCustomersByLocationUseCase.java:7`; HR:94-100; HAT:23; HRD:28 (`ConcretePort`); consumers `CustomerController.java:40-52`, `CustomerLocationController.java:15-18` | ✅ PASS (completeness rests on compilation) |
| ARCH-06 | Persistence operations are `port.out` interfaces; services depend on them, never on Spring Data repositories | `port/out/CustomerPersistencePort.java:16`, `LocationCountPort.java:7`; `CustomerService.java:35,39`; `CustomerLocationService.java:36,38`; HR:103-108; HAT:26; HRD:29; `CustomerServiceTest.java:58-59` `@Mock CustomerPersistencePort`; `CustomerLocationServiceTest.java:22-23` | ✅ PASS |
| ARCH-07 | `..adapter.in.web..` depends on no `..adapter.out..` and no `..application.port.out..` | HR:111-116; HAT:29; HRD:30-31. Round 1: M6/M8 killed | ✅ PASS |
| ARCH-08 | `..adapter.out.persistence..` depends on no `..adapter.in..` | HR:119-124; HAT:32-33; HRD:32 | ✅ PASS |
| ARCH-09 | Every output port is implemented by a class in `..adapter.out..` | `CustomerPersistenceAdapter.java:30`; HR:127-131; HAT:36-37; HRD:33-34; context load `CustomerApiSddApplicationTests.java:12` | ✅ PASS |
| ARCH-10 | `@RestController` lives in `..adapter.in.web..`; `@Entity` and Spring Data repositories live in `..adapter.out.persistence..` | HR:148-152, HR:134-138, HR:141-145; HAT:40-46; HRD:35-37; `CustomerController.java:34`, `CustomerLocationController.java:12`, `SpringDataCustomerRepository.java:10`, `CustomerJpaEntity.java:20` | ✅ PASS |
| ARCH-11 | Every HTTP IT of customer-management and geo-grouping passes, with assertions unchanged | Gate: every HTTP IT is green (CreateAndGet 32, Update 18, List 16, Delete 4, ErrorContract 8, Location 11, Operability 11). No expected value weakened across `9796beb..697af6c` (see the ARCH-11 section). CUST-12 PUT: `CustomerUpdateIntegrationTest.java:214` `isConflict()`, `:216` field `email`, `:218` `assertWriteReachedTheDatabase()`, `:219` `assertAnaUnchanged()`; cpf `:227/:229/:231/:232`. POST: `CustomerCreateAndGetIntegrationTest.java:173` and `:185`. CUST-24: `CustomerUpdateIntegrationTest.java:279` 409, **`:282` optimistic failures == 1 (new, T17)**, `:284-288` stored values | ✅ PASS |
| ARCH-12 | Grouping runs exactly one SQL statement and loads zero JPA entities | `CustomerLocationIntegrationTest.java:178` `getPrepareStatementCount()).isEqualTo(1)`, `:179` `getEntityLoadCount()).isZero()`. Round 1: M12 killed. This round, W7 (statistics disabled) fails `:178`, so the assertion is not vacuous | ✅ PASS |
| ARCH-13 | Flyway migrations are unchanged; Hibernate schema validation passes | `git diff --stat 9796beb..697af6c -- src/main/resources` empty; `application.properties:9` `ddl-auto=validate`; `CustomerApiSddApplicationTests.java:12` passes. Round 1: M13 killed | ✅ PASS |

**Status**: 13/13 ACs have `file:line` evidence, and the spec outcomes match.

**Edge cases**:
- IF a domain class imports Spring or JPA THEN the build fails: HRD:26/35; round-1 M7a/M7b. ✅
- IF a controller injects a repository or an output port THEN the build fails: HRD:30; round-1 M8. ✅

---

## ARCH-11: HTTP Integration-Test Assertion Diff (`9796beb..697af6c`)

**Carried over (rounds 1-5, `9796beb..26b1b65`)**. HTTP ITs with zero diff: `CustomerDeleteIntegrationTest`, `CustomerListIntegrationTest`, `ErrorContractIntegrationTest`, `customer/location/CustomerLocationIntegrationTest`, `CustomerApiSddApplicationTests`.

| File (HEAD line) | Change | Kind | Expected value changed? | Judgement |
| ---------------- | ------ | ---- | ----------------------- | --------- |
| `HttpIntegrationTestSupport.java:66-71,93-95` | Spy type `CustomerRepository` → `SpringDataCustomerRepository`; adds `@Autowired CustomerPersistencePort persistence` and `stored(UUID)` | arrangement | n/a | OK (sanctioned by the design) |
| `HttpIntegrationTestSupport.java:78-81` | `preCheckMisses(Consumer)` helper | arrangement | n/a | OK (T15) |
| `OperabilityIntegrationTest.java:132` | `customer.CustomerService` → `service.CustomerService` | assertion | logger fragment only | ✅ Known intended change |
| `CustomerCreateAndGetIntegrationTest.java:54, 84, 210` | reads stored state via `stored(id)` | extractor | No | ✅ Known intended change |
| `CustomerUpdateIntegrationTest.java:132` (was `:125`) | `stored(anaId).getDetails()`, still `isNull()` | extractor | No | ✅ Known intended change |
| `CustomerUpdateIntegrationTest.java:259-289` (CUST-24) | the concurrent first writer uses `persistence.findById` + `persistence.update(...)` in `REQUIRES_NEW` | arrangement | No; `:279` 409, `:284-288` values identical | ✅ Not a weakening; discriminates |
| `TransactionBoundaryIntegrationTest.java` | T16 tests (`:40-48`, `:51-60`) and the `putEmail` helper | added tests | No | ✅ (round 5) |

**This round (`26b1b65..697af6c`)**:
- `CustomerUpdateIntegrationTest.java`: **+12/-0**. It adds imports (`:21-24`), the `EntityManagerFactory` field (`:47-48`), `Statistics statistics = …; statistics.clear();` (`:276-277`), and a comment plus `assertThat(statistics.getOptimisticFailureCount()).isEqualTo(1);` (`:281-282`). No line is removed; the 409 at `:279` and the stored values at `:284-288` are unchanged.
- `TransactionBoundaryIntegrationTest.java`: +1/-1. The only removed line is the javadoc's "The write use cases run inside one read-write transaction", now "Create and update run inside one read-write transaction" (`:26`). No code changed.

**Conclusion**: no asserted expected value is weakened across `9796beb..697af6c`. T17 strictly adds an assertion.

---

## Diff Audit `26b1b65..697af6c`

- `git diff --stat 26b1b65..697af6c -- src/main` is **empty**. `src/main/resources` is unchanged across the full range.
- `git diff --numstat`: `.specs/LESSONS.md` +6, `.specs/STATE.md` +3/-3, `design.md` +2/-2, `tasks.md` +34/-3, `.specs/lessons.json` +19/-1, `CustomerUpdateIntegrationTest.java` +12/-0, `TransactionBoundaryIntegrationTest.java` +1/-1.
- **design.md vs code**:
  - `design.md:106` now says "not split across transactions". That is accurate: Y7, Y8, Z1 and Z2 are killed by `TransactionBoundaryIntegrationTest.java:46,58`. ✅
  - `design.md:106` "The CUST-24 HTTP test also asserts the invariant itself: Hibernate records exactly one optimistic failure … also covers an adapter that discards the entity it already read (Z7b/Z7c, T17)". This matches `CustomerUpdateIntegrationTest.java:276-282`, and Z7b, Z7c and W10 (detach) are killed. ✅
  - `design.md:127` "the adapter gets back the entity the service already read … The CUST-24 HTTP test covers this, including the assertion that exactly one optimistic failure occurred". This is now enforced for every route tried (split transaction, clear, refresh, detach). One nuance: the compound Z7c+W9+M1 does not hand back the same entity but still survives. It remains correct, because Hibernate's merge rejects the stale version the service read (see the sensor). The guarantee design.md cares about (no lost update, conflict detected against the service's version) holds. ✅
  - `design.md:127` "The explicit version comparison only fires for callers outside a transaction". This is accurate at HEAD: M1 alone is killed only by the adapter IT (`:103`), and CUST-24 at HEAD records exactly one Hibernate optimistic failure. ✅
- **TransactionBoundaryIntegrationTest javadoc `:26`** "Create and update …" matches the tests (POST `:40-48,:62-72`, PUT `:51-60,:75-86`). This closes round 5's cosmetic gap 2 (DELETE is not covered, and the javadoc no longer claims it). ✅
- **tasks.md T17**: every box is accurate (reproduced above). **STATE.md**: the handoff (T1-T17, "focused verification 5", escalation rule) is accurate.

---

## Discrimination Sensor

### Isolation

- All runs used the detached worktree `.worktrees/verify-hex6` at `697af6c`, with `./mvnw -B clean test [-Dtest=...]` (the gate used `clean verify`).
- Mutations were applied by `git apply` of the round-5 saved diffs (`scratchpad/sensor6/patches/*.patch`, the round-5 `.diff` files stripped of their porcelain trailer; each passed `git apply --check` at HEAD; R-Y7's patch is byte-identical to round 5's `Y7-…patch`) or by exact-match replacement (`sensor6/mut.py`, which asserts exactly one occurrence).
- `sensor6/run.sh` refuses to run when nothing changed or a mutation step failed. After each run it executes `git reset -q --hard HEAD && git clean -fdq src`. **`porcelain-after: []` was logged after all 26 runs** (`batch1.out` 11, `batch2.out` 12, `batch3.out` 3). The worktree porcelain was empty at the end, before removal.
- The main tree's porcelain was empty, on `refactor/hexagonal` at `697af6c`, before and after. There was no stash, commit, push or branch change. The worktree was removed with `git worktree remove --force`.
- Artifacts: `scratchpad/sensor6/<id>.diff|.log`, `batch1.sh`..`batch3.sh`, `batch*.out`.

### Rounds 1-5 (carried over)

| Round | At | Mutations | Killed | Survived |
| ----- | -- | --------: | -----: | -------- |
| 1 | `4cfd2ff` | 20 | 16 | 4: M3b, M10, M11b (P); M7c (E) |
| 2 | `87626bf` | 14 + 1 control | 12 | 2: N3, N4b (P), plus the vacuous N1b control |
| 3 | `f2d371c` | 22 | 17 | 5: X5 (P); A1 (A); X14, X15 (C); X16 (E) |
| 4 | `a11b2d3` | 19 + 2 controls | 12 | 7: Y7, Y8 (P); R-X14b, R-X15, Y3+X5, Y5b (C); Y9 (E) |
| 5 | `26b1b65` | 22 + 2 controls + 5 experiment runs | 18 | 4: Z7b, Z7c (P); Z5b (C); Z6 (E) |

### Round 6 (this round, `697af6c`)

**Step 2: re-runs of earlier survivors and the regression sample** (full suite each):

| # | Mutation | Result |
| - | -------- | ------ |
| R-Y7 | `CustomerService.update` runs `persistence.update(...)` in a `TransactionTemplate` with `PROPAGATION_REQUIRES_NEW` | ✅ Killed (300/302): `CustomerUpdateIntegrationTest…:282` `1L` vs **`0L`**; `TransactionBoundaryIntegrationTest.updateCompletesExactlyOneTransaction:58` `1L` vs `2L` |
| R-Y8 | `@Transactional(REQUIRES_NEW)` on `CustomerPersistenceAdapter.update` | ✅ Killed (300/302): `:282` `1L` vs **`0L`**; TB `:58` `1L` vs `2L` |
| R-Z7b | `@Transactional` (REQUIRED) + `@PersistenceContext EntityManager` + `entityManager.refresh(entity)` in the adapter's `update` | ✅ **Killed (301/302)**: only `:282`, `expected: 1L but was: 0L` |
| R-Z7c | `@PersistenceContext EntityManager` + `entityManager.clear()` before the reload in `update` | ✅ **Killed (301/302)**: only `:282`, `expected: 1L but was: 0L` |
| R-M1 | Explicit version check (`CustomerPersistenceAdapter.java:59-61`) removed | ✅ Killed (301/302): `CustomerPersistenceAdapterIntegrationTest.updateFromAStaleVersionThrowsConcurrentUpdateAndKeepsTheFirstWrite:103` |
| R-X5 | Method-level `@Transactional(SUPPORTS)` on `update` | ✅ Killed (299/302): `:282` (`0L`), TB `:58` (`3L`), TB `:86` (`Observed[active=false…]`) |
| R-N3 | Class-level `@Transactional(SUPPORTS)` | ✅ Killed (297/302): `HexagonalArchitectureTest.useCaseServicesAreTransactional`, `:282`, TB `:58`, `:72`, `:86` |
| R-M3b | `update`'s `saveAndFlush` no longer wrapped in `write(...)` | ✅ Killed (300/302): `CustomerUpdateIntegrationTest:214` and `:227`, 409 → 500 |
| R-A1 | `validation/CpfDigits` constructor holds a copied algorithm; decoy `Cpf.isValid` kept | ✅ Killed (301/302): `cpfValidationDelegatesToDomain`, "calls constructor `CpfDigits.<init>(String)` in (CpfValidator.java:15)" |
| R-Z1 | `REQUIRES_NEW` on `insert` (round-5 Z1) | ✅ Killed (301/302): TB `createCompletesExactlyOneTransaction:46` `1L` vs `2L` |
| R-Z2 | `REQUIRES_NEW` on `findById` (round-5 Z2) | ✅ Killed (300/302): `:282` `0L`; TB `:58` `2L` |

The orchestrator's claim ("each failed the test with `expected: 1L but was: 0L`") is **confirmed** for Y7, Y8, Z7b and Z7c.

**Step 3: new mutations attacking the T17 assertion**. `P` = plausible, `A` = adversarial, `C` = compound, `E` = equivalent.

| # | Class | Site (HEAD) | Mutation | Tests | Result |
| - | ----- | ----------- | -------- | ----- | ------ |
| W1 | P | `CustomerPersistenceAdapter.java:67-69` | On `ObjectOptimisticLockingFailureException`, retry the write once in a `REQUIRES_NEW` `TransactionTemplate` against the latest committed row (reload, apply, `saveAndFlush`) | full | ✅ Killed (301/302): `:279` 409 → **500**. The outer transaction was already marked rollback-only by the failed flush, so the commit throws `UnexpectedRollbackException`. The 409 assertion carries this one |
| W2 | P | `CustomerService.java:78` | The service catches `ConcurrentCustomerUpdateException` and retries once with `find(id)` in the same transaction | full | ✅ Killed (301/302): `:282` `1L` vs **`2L`**. The retry re-reads the same managed stale entity and fails again, so the response is still 409. This is a stricter-than-behaviour kill (see the classification notes) |
| W3 | P | `CustomerPersistenceAdapter.java:59` | Version check against a fresh DB re-read (`select c.version from CustomerJpaEntity c where c.id = :id`) instead of the managed entity | full | ✅ Killed (301/302): `:282` `1L` vs **`0L`**. The explicit check now fires first, so the stale write never reaches Hibernate. This is a stricter-than-behaviour kill: the managed entity is kept and Hibernate's guard still exists |
| W4a | P | `CustomerService.java:78` | `persistence.update(changed)` called twice with the same domain object | full | ✅ Killed (290/302): every normal PUT turns into 409 (the explicit check sees version+1), plus `CustomerServiceTest`, `OperabilityIntegrationTest` and the location ITs |
| W4b | P | `:78` | `persistence.update(persistence.update(...))` (second call with the first result) | full | ✅ Killed (301/302): `CustomerServiceTest.updateWithoutPhoneAndBirthDateStoresThemAsNull:175`, Mockito `TooManyActualInvocations` (wanted 1, was 2) |
| W5 | P (test-side) | `CustomerUpdateIntegrationTest.java:277` | `statistics.clear()` moved after the request | class | ✅ Self-detecting at HEAD: the count assertion (`:281` in the mutated file) fails, `1L` vs `0L` |
| W6 | E (test-side) | `:277` | `statistics.clear()` removed | class, full | ⚪ Survived (18/18, 302/302). No earlier test in the shared context produces a Hibernate optimistic failure, so the cumulative count is still 1. **The clear() is defensive, not load-bearing**: W6+Z7c is still killed (below) |
| W7 | P (config) | `TestcontainersConfiguration.java:22` | `hibernate.generate_statistics` → `false` | Update, TB, Location ITs | ✅ **Fails loudly, not vacuously** (29/33): `:282` `1L` vs `0L`, TB `:46` and `:58` (`0L`), ARCH-12 `CustomerLocationIntegrationTest:178` |
| W8 | P | `CustomerPersistenceAdapter.java:65` | `saveAndFlush(entity)` → `save(entity)` (flush deferred to commit) | full | ✅ Killed (299/302): `:279` 409 → 500, plus CUST-12 PUT `:214`, `:227` 409 → 500 |
| W9 | E | `:65` | Write by merging the domain state: `saveAndFlush(CustomerJpaEntity.from(customer))`. The detached entity carries the version the service read | full | ⚪ Survived (302/302). Behaviour and invariant are preserved: at HEAD the merge targets the managed stale entity, and the versioned UPDATE fails as before (count 1, 409, first write kept) |
| W10 | P | `:58` | Z7c variant: `entityManager.detach(cached)` and a second `findById` | full | ✅ Killed (301/302): `:282` `1L` vs `0L` |
| W6+Z7c | C (test + P) | `:277` + Z7c | Does the assertion still discriminate without `clear()`? | full | ✅ Killed (301/302): `:280` (mutated file) `1L` vs `0L`. It confirms W6 is equivalent |
| Z7c+W9 | C | Z7c + W9 | Fresh entity, merge write | full | ✅ Killed (301/302): `:282` `0L`. The explicit check (fresh v+1 vs read v) fires before Hibernate. Stricter than behaviour (the response is correct: 409) |
| Z7c+W9+M1 | C (three edits) | Z7c + W9 + explicit check removed | Fresh entity, no explicit check, merge the domain state carrying the read version | full | ⚪ Survived (302/302). **Correct behaviour**: Hibernate's merge compares the detached version (the one the service read) with the loaded row, records an optimistic failure (count 1), and throws `StaleObjectStateException` → `ObjectOptimisticLockingFailureException` → 409. The first write is kept (`:284-288` pass), and the adapter IT `:103` passes through the same route. This is not a lost update and not a weakening. It is an alternative, standard JPA optimistic-locking implementation |

**Round 6 result**: 25 counted mutations in 26 runs (11 re-runs, 11 new, 3 compound follow-ups; W6 ran twice, class-only and full suite).

| Group | Mutations | Killed | Survived |
| ----- | --------: | -----: | -------- |
| Re-runs (R-Y7, R-Y8, R-Z7b, R-Z7c, R-M1, R-X5, R-N3, R-M3b, R-A1, R-Z1, R-Z2) | 11 | 11 | 0 |
| New plausible, production (W1, W2, W3, W4a, W4b, W8, W10) | 7 | 7 | 0 |
| New plausible, test-side and config (W5, W7) | 2 | 2 | 0 |
| New equivalent (W6 test-side, W9 production) | 2 | 0 | 2 E |
| Compound follow-ups (W6+Z7c, Z7c+W9, Z7c+W9+M1) | 3 | 2 | 1 C |
| **Total** | **25** | **22** | **3** (0 P, 0 A, 1 C, 2 E) |

W5 (self-detecting) and W7 (fails loudly) count as killed. Controls: none this round; the gate run is not counted.

**Classification rationale**:
- **W6 is equivalent**. Removing a defensive `clear()` changes no outcome today. W6+Z7c proves the assertion still discriminates without it. Keeping the `clear()` protects against a future test in the shared context that causes an optimistic failure (see Residual Risks).
- **W9 is equivalent**. Merging the domain state is the canonical JPA pattern for detached optimistic locking. Hibernate checks the version the service read in both paths. HTTP behaviour, stored data and the "stale write rejected by Hibernate's version check" invariant are all unchanged.
- **Z7c+W9+M1 is compound and behaviour-correct**. It takes three independent edits and still produces no lost update. It departs from the design.md wording "the adapter gets back the entity the service already read", but not from its goal. The T17 assertion's message ("the stale write reached Hibernate's versioned UPDATE") is honoured in spirit, because merge's version check is Hibernate's optimistic check. It is not a gap.
- **W2, W3 and Z7c+W9 are killed but stricter than behaviour**: their HTTP outcome is correct (409, first write kept), yet `:282` fails. W3 keeps both guards; W2 is a pointless same-transaction retry. This is over-specification: the test pins the detection mechanism named in design.md. Over-strict kills do not affect the verdict. They are listed as a maintenance note.
- **W1 and W8 are killed by the pre-existing 409 assertion, not by T17**. Both keep the optimistic count at 1 (the failure happens), but they break the response. This shows the status assertion and the count assertion complement each other.
- **W7 fails loudly**. With statistics disabled, every count reads 0, and 4 assertions fail, including ARCH-12. The new assertion cannot pass vacuously.

**All earlier plausible and adversarial survivors are killed**: round 1 M3b, M10, M11b; round 2 N3, N4b, N1b; round 3 X5, A1; round 4 Y7, Y8; **round 5 Z7b, Z7c**. The re-runs this round cover Y7, Y8, Z7b, Z7c, M1, X5, N3, M3b, A1, Z1 and Z2.

**Sensor depth**: expanded (P0-style: data integrity and concurrency). **Verdict impact**: none. There are no plausible or adversarial survivors.

---

## Code Quality (T17)

| Check | Status |
| ----- | ------ |
| Minimum code / no scope creep | ✅ One field, two statement lines, one comment and one assertion in one test method. No production change |
| Surgical changes | ✅ The `src/main` diff is empty; the test diff is +12/-0 |
| Matches patterns | ✅ Reuses the statistics bean (`TestcontainersConfiguration.java:19-22`) and the clear-before-request pattern from T16 |
| Tests map to ACs | ✅ T17 → the CUST-24 design guard (ARCH-11 context) |
| Doc accuracy | ✅ `design.md:106,127` and the javadoc `:26` match the code. One nuance for the merge-based variant is noted under the diff audit |

---

## Spec-Precision Gaps

1. **ARCH-11 wording** (carried over): "assertions unchanged" conflicts with the sanctioned extractor and logger-fragment changes, which are judged non-weakening. The spec should say "expected values unchanged".
2. **ARCH-03 enforcement strength** (carried over): the spec does not say how strongly "delegates" is enforced; HR:167-176 enforces "calls `Cpf.isValid` and nothing else foreign".
3. **ARCH-05 / ARCH-09 completeness** (carried over): the rules prove shape and placement; completeness rests on compilation and the context load.
4. **CUST-24 guard** (updated): it is still not an ARCH AC. It is now enforced as the invariant (`CustomerUpdateIntegrationTest.java:282`), not as one route. The design.md wording "the adapter gets back the entity the service already read" is stricter than what is tested (see Z7c+W9+M1). The tested property (the stale version is rejected by Hibernate's optimistic check) is the one that prevents a lost update.

---

## Residual Risks (not verdict-failing)

| Risk | Classification | One-line mitigation |
| ---- | -------------- | ------------------- |
| W6: `statistics.clear()` (`:277`) is not load-bearing today | Equivalent (test-side) | Keep it. It protects against a future earlier test in the shared context that causes an optimistic failure |
| W9, Z7c+W9+M1: a merge-based write passes and is correct | Equivalent / compound, behaviour-correct | None needed. Optionally reword `design.md:127` to "Hibernate checks the version the service read" instead of "gets back the entity the service already read" |
| W2, W3, Z7c+W9: correct HTTP behaviour, yet `:282` fails | Over-strict (stricter than behaviour) | Accept. The test pins the mechanism documented in design.md, and a deliberate change of mechanism should update the test and the design together |
| Hibernate statistics are SessionFactory-global | Low (flakiness) | Tests run sequentially in one context. Only a background transaction or optimistic failure during the request could skew `:46`, `:58` or `:282` |
| Z5b (round 5): the update test keeps only `getSuccessfulTransactionCount` alongside a contrived failing inner transaction | Compound | Keep TB `:58` (`getTransactionCount`); it is the load-bearing line |
| Z6 (round 5): `delete` split across two transactions | Equivalent against the spec | The javadoc is now scoped to create and update (`:26`); nothing further |
| R-X14b, R-X15, Y3+X5, Y5b (round 4) | Compound test-code faults | Unchanged |
| Reflection, `MethodHandle` or bytecode routes around the CPF rule | Adversarial | `Method.invoke` is itself a foreign call and fails the rule; otherwise code review |
| X3 (JTA annotation rejected), X16 (unreachable fallback) | Over-strict / equivalent | None needed |

---

## Ranked Gaps

None that fail the bar.

1. **[Cosmetic, optional]** `design.md:127`: "the adapter gets back the entity the service already read" is one sufficient implementation, not the tested invariant. A merge-based write that re-reads (Z7c+W9+M1) is also correct and passes. A suggested rewording is "Hibernate's optimistic check runs against the version the service read". There is no code or test change.
2. **[Cosmetic, optional]** Compound test-fault vacuity (Z5b; round-4 R-X14b, R-X15, Y3+X5, Y5b). See Residual Risks.

---

## Requirement Traceability Update (proposed)

| Requirement | Previous (round 5) | New Status |
| ----------- | ------------------ | ---------- |
| ARCH-01, 02, 04-10, 12, 13 | ✅ Verified | ✅ Verified |
| ARCH-03 | ✅ Verified | ✅ Verified (R-A1 killed again) |
| ARCH-11 | ✅ Verified | ✅ Verified (no expected value weakened through `697af6c`; T17 is purely additive) |
| Design guard: CUST-24 stale write reaches Hibernate's version check (not an AC) | ⚠️ Z7b, Z7c survived | ✅ Enforced: split transactions (Y7, Y8, Z2), a split persistence context (Z7b, Z7c, W10) and a re-read check (W3) are all killed by `:282`; retries and a deferred flush (W1, W8) are killed by `:279` |

---

## Summary

- **Overall**: ✅ Ready. T17 closes the round-5 gap with a direct assertion of the CUST-24 invariant, verified independently. There is no surviving plausible or adversarial mutant.
- **Spec-anchored check**: 13/13.
- **Gate**: 302 passed, 0 failed, 0 skipped (`./mvnw -B clean verify`, 35.8 s).
- **Sensor, all six rounds**:

| Round | At | Mutations | Killed | Survived |
| ----- | -- | --------: | -----: | -------- |
| 1 | `4cfd2ff` | 20 | 16 | 4: 3 P (M3b, M10, M11b), 1 E (M7c) |
| 2 | `87626bf` | 14 + 1 control | 12 | 2 P (N3, N4b), plus the N1b control |
| 3 | `f2d371c` | 22 | 17 | 5: 1 P (X5), 1 A (A1), 2 C (X14, X15), 1 E (X16) |
| 4 | `a11b2d3` | 19 + 2 controls | 12 | 7: 2 P (Y7, Y8), 4 C, 1 E (Y9) |
| 5 | `26b1b65` | 22 + 2 controls + 5 experiment runs | 18 | 4: 2 P (Z7b, Z7c), 1 C (Z5b), 1 E (Z6) |
| **6** | **`697af6c`** | **25** (26 runs) | **22** | **3: 0 P, 0 A, 1 C (Z7c+W9+M1, behaviour-correct), 2 E (W6, W9)** |

- **What works**:
  - Every plausible and adversarial survivor from rounds 1-5 is killed.
  - The invariant assertion catches every route to a fresh-state write tried so far: split transactions, `clear`, `refresh`, `detach`, a re-read check.
  - The transaction count and probe still catch lost, read-only and split transactions.
  - Disabling statistics fails loudly.
  - T17 touched no production code and weakened no assertion.
- **Next steps**: push `refactor/hexagonal` and open the PR, per STATE.md. The two optional cosmetic items can go in with it or be skipped.

Note: per the dispatch, this report lives in the session scratchpad, not in `.specs/features/hexagonal-architecture/validation.md`. `validate_state.py` and `lessons.py` were therefore not run, since they read or modify the repo. Candidate lessons are in the chat reply.
