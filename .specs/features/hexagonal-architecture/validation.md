# Hexagonal Architecture Validation (re-verification 3, after fix iteration 3)

## Validation: hexagonal-architecture - FAIL ❌

**Verdict**: FAIL

**Date**: 2026-09-23
**Spec**: `.specs/features/hexagonal-architecture/spec.md` (ARCH-01..ARCH-13)
**Diff range**: `9796beb..a11b2d3`
- feature `9796beb..4cfd2ff` (T1-T5)
- fix iteration 1 `4cfd2ff..87626bf` (T6-T9)
- fix iteration 2 `87626bf..f2d371c` (T10-T12)
- fix iteration 3 `f2d371c..a11b2d3`: `7ffd0b1` T13, `d5817e1` T14, `a11b2d3` T15

**Verifier**: independent sub-agent (author ≠ verifier). Detached worktree `.worktrees/verify-hex4` at `a11b2d3`, removed at the end.
**Round**: re-verification 3 (the last allowed round, after fix iteration 3 of 3)

**Why FAIL**:
- 13/13 ACs have evidence with matching outcomes. The gate is green (300/300).
- Every earlier survivor the fix loop targeted is killed. X5 and A1 are killed, and so are N3 and M10. X14 in its natural form at HEAD (through the new helper) is also killed.
- **Two plausible mutants survive, Y8 and Y7.** Both split `update` into two transactions: the read runs in one and the versioned write in another.
  - **Y8** is one annotation, `@Transactional(propagation = REQUIRES_NEW)`, on `CustomerPersistenceAdapter.update`. It survives with 300/300 green.
  - **Y7** makes `CustomerService.update` run its persistence call in a `TransactionTemplate` with `PROPAGATION_REQUIRES_NEW`. It survives with 300/300 green.
  - Both have the same signature as X5 in round 2:
    - they are HTTP-invisible on their own, because the adapter's explicit version check becomes the live guard;
    - adding M1 (dropping that check) turns CUST-24 from 409 into **200, a lost update**: Y8+M1 and Y7+M1 fail at `CustomerUpdateIntegrationTest.java:269`;
    - M1 alone leaves CUST-24 at 409 (the M1 control).
  - So each mutant removes Hibernate's same-transaction guard. That guard is exactly what design.md:127 and the T13 javadoc promise: "Hibernate's versioned UPDATE detects a concurrent change only against the version read in the same transaction".
- The new probe checks that a transaction is active and read-write **at the pre-check**. It does not check that the write runs in **the same** transaction. Round 2 called X5 plausible, and Y8 is the same kind of fault (one ordinary annotation, no trick). Under the stated bar ("no surviving plausible mutant") the verdict is FAIL.
- **Severity: Minor.** CUST-24 stays correct at HEAD, because the explicit version check is live and M1 is killed by adapter IT:103. What is not enforced is the second, redundant guard, and design.md claims that guard exists. This was the last allowed fix round, so the realistic ways out are a user decision or one more small test (see Ranked Gaps).
- **Deviation from the dispatch**: the dispatch said the X14 and X15 re-runs "must be killed".
  - X15 and the literal round-2 form of X14 (which bypasses the helper) still survive.
  - Both need two independent test-code faults plus a production fault. The pass bar puts compound test-code mutations under residual risk, so they are not counted toward the FAIL. They are reported here openly.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1-T12 | ✅ Done | Verified in rounds 1-3; still green at HEAD |
| T13 | ⚠️ Done, with a gap | `TransactionBoundaryIntegrationTest.java:31-41` (POST) and `:44-58` (PUT) record `isActualTransactionActive()` and `isCurrentTransactionReadOnly()` in a `doAnswer` on the pre-check (`:36`, `:50`). They assert `Observed(true, false)` at `:40` and `:57`. X5, N3, M10, Y1 and Y2 are killed. **Gap**: the probe runs at the pre-check, so a write in a *separate* transaction (Y7, Y8) is not seen. DELETE is not probed (Y9, equivalent). The T13 claim "covers every way of losing the use-case transaction" is too strong |
| T14 | ✅ Done | `HexagonalRules.java:174-175` uses `onlyCallCodeUnitsThat(declaredIn(Cpf) or declaredIn(simpleName "CpfValidator") or declaredIn(Object))`. Fixture `archfixtures/customer/adapter/in/web/constructor/{CpfValidator,CopiedCpfCheck}.java`; HRD row `:44-45`. A1 is killed. Y6 (loosening the rule to all of `java.lang`) is killed by HRD row 15 (`ignoring.CpfValidator`) |
| T15 | ✅ Done (X15 not addressed) | `HttpIntegrationTestSupport.java:78-81` `preCheckMisses(Consumer)` stubs and then clears invocations in one call. The four race tests use it (`CustomerCreateAndGetIntegrationTest.java:171,183`; `CustomerUpdateIntegrationTest.java:205,218`). X14 through the helper is killed. The literal X14 (which bypasses the helper) and X15 still survive (compound) |

---

## Gate Check

- **Command**: `./mvnw -B clean verify`, run in the clean worktree at `a11b2d3`.
- **Result**: **300 passed, 0 failed, 0 errors, 0 skipped**. BUILD SUCCESS, exit 0, 1:04 min. Log: `scratchpad/sensor4/gate.log`.
- **Test count history**: 240 at `9796beb` → 289 at `4cfd2ff` → 295 at `87626bf` → 297 at `f2d371c` → **300** at `a11b2d3`.
- **Delta 297 → 300 (+3)**: `TransactionBoundaryIntegrationTest` adds 2 tests, and `HexagonalRulesDiscriminationTest` grows from 16 to 17 cases (the `constructor.CpfValidator` row).
- **Deleted tests in the fix range**: none. Across the full range, only `customer/CustomerServiceTest` was deleted. It was rewritten as `application/service/CustomerServiceTest` (17 tests), as verified in round 1.
- **Tests per class**:

| Class | Tests |
| ----- | ----: |
| Operability | 11 |
| AppContext | 1 |
| ClockConfig | 3 |
| HexDiscrimination | 17 |
| Delete | 4 |
| Location (HTTP) | 11 |
| ErrorContract | 8 |
| LocationQuery | 6 |
| PersistenceAdapter | 9 |
| SpringDataRepo | 6 |
| Specifications | 7 |
| CustomerRequest | 65 |
| CpfValidator | 10 |
| Update | 18 |
| List | 16 |
| CreateAndGet | 32 |
| CustomerService | 17 |
| CustomerSort | 21 |
| CustomerLocationService | 7 |
| **TransactionBoundary (new)** | **2** |
| CpfTest | 12 |
| CustomerTest | 4 |
| HexArch | 13 |

---

## Spec-Anchored Acceptance Criteria

**Abbreviations**:
- **HR**: rules file `src/test/java/com/example/customerapi/architecture/HexagonalRules.java`. Lines up to `:164` are unchanged from round 3. The CPF rule is now `:167-176` and the layering rule `:179-183`.
- **HAT**: runner `HexagonalArchitectureTest.java:12`, `@AnalyzeClasses(packages = "com.example.customerapi", importOptions = DoNotIncludeTests)`. Its lines are unchanged.
- **HRD**: fixture proof `HexagonalRulesDiscriminationTest.java`. `:26-46` is the rule/fixture table. `:52-53` asserts that `rule.check(FIXTURES)` throws an `AssertionError` whose message contains the fixture.
- There is no `archunit.properties`, so `failOnEmptyShould=true`.

**Spot-checked this round**: 25+ citations were read at `a11b2d3` with `git show a11b2d3:<path>`, and all match:
- `domain/Customer.java:11` `public final class Customer`
- `CustomerJpaEntity.java:20` `@Entity`
- `CpfValidator.java:14` `return value == null || ...domain.Cpf.isValid(value)`
- `Cpf.java:11-13`
- all six `port/in` interfaces at `:6/:7/:8/:5/:8/:7`, each a `public interface`
- `CustomerPersistencePort.java:16`, `LocationCountPort.java:7`
- `CustomerService.java:29,35,39`, `CustomerLocationService.java:24,36,38`
- `CustomerController.java:34` `@RestController`, `:40-52` (only the five use-case fields and constructor parameters)
- `CustomerLocationController.java:12,15-18`
- `SpringDataCustomerRepository.java:10`
- `CustomerPersistenceAdapter.java:30`
- `CustomerLocationIntegrationTest.java:178-179`
- `CustomerPersistenceAdapterIntegrationTest.java:55-58,103`
- `CustomerServiceTest.java:58-59`, `CustomerLocationServiceTest.java:22-23`
- `OperabilityIntegrationTest.java:132`
- `CpfTest.java:14,22`
- `CustomerApiSddApplicationTests.java:12`
- `application.properties:9`
- HR and HAT lines as cited below

Production code is byte-identical to `87626bf` (see Diff Audit). The test-line shifts caused by T15 have been re-located below.

| AC | Spec-defined outcome | `file:line` + assertion / rule | Result |
| -- | -------------------- | ------------------------------ | ------ |
| ARCH-01 | `..customer.domain..` depends on no `org.springframework..`, `jakarta..`, `org.hibernate..`, `..adapter..` | HR:77-82 `noClasses().that().resideInAPackage(DOMAIN).should().dependOnClassesThat().resideInAnyPackage("org.springframework..","jakarta..","org.hibernate..",ADAPTER,APPLICATION)`; HAT:16; HRD:26 (`SpringAwareDomainObject`). Round 1: M7a and M7b killed | ✅ PASS |
| ARCH-02 | The domain customer has no JPA annotations and is persisted through a separate JPA entity | `domain/Customer.java:11` plain `public final class`; `adapter/out/persistence/CustomerJpaEntity.java:20` `@Entity`; HR:134-138; HAT:40; HRD:35 (`JpaEntityInDomain`); round trip `CustomerPersistenceAdapterIntegrationTest.java:55` `assertThat(stored.getDetails()).isEqualTo(ANA)`, `:56-58` createdAt/updatedAt/version. Round 1: M4a/b/c killed | ✅ PASS |
| ARCH-03 | The CPF rule is implemented once, in the domain; the web validator delegates | `domain/Cpf.java:11-25` (the algorithm); `adapter/in/web/validation/CpfValidator.java:14` delegates; `CpfTest.java:14,22`. HR:167-176 requires `callMethod(Cpf.class,"isValid",String.class)` **and** `onlyCallCodeUnitsThat(declaredIn(Cpf) or declaredIn(simpleName "CpfValidator") or declaredIn(Object))`; HAT:52; HRD:40-45 (copy, ignore and constructor fixtures). Killed by the rule: M11b, N4b, N5, X7, **A1** (this round: "`CpfValidator.isValid` calls constructor `CpfDigits.<init>(String)`"). Killed by behaviour tests: N4, X8. The rule itself is guarded by HRD (Y6 is killed) | ✅ PASS |
| ARCH-04 | `..customer.application..` is free of `..adapter..`, `jakarta.persistence..`, `org.hibernate..`, `org.springframework.data.jpa..`, `org.springframework.web..` | HR:85-91 (the exact package list); HAT:19-20; HRD:27 (`JpaAwareService`) | ✅ PASS |
| ARCH-05 | Each use case (create, get, update, delete, list, group) is an interface in `..application.port.in..` | `port/in/CreateCustomerUseCase.java:6`, `GetCustomerUseCase.java:7`, `UpdateCustomerUseCase.java:8`, `DeleteCustomerUseCase.java:5`, `ListCustomersUseCase.java:8`, `GroupCustomersByLocationUseCase.java:7` (all `public interface`); HR:94-100; HAT:23; HRD:28 (`ConcretePort`); consumers `CustomerController.java:40-52`, `CustomerLocationController.java:15-18` | ✅ PASS (that all six exist rests on compilation) |
| ARCH-06 | Persistence operations are `port.out` interfaces; services depend on them, never on Spring Data repositories | `port/out/CustomerPersistencePort.java:16`, `LocationCountPort.java:7`; `CustomerService.java:35,39`; `CustomerLocationService.java:36,38`; HR:103-108; HAT:26; HRD:29 (`RepositoryAwareService`); `CustomerServiceTest.java:58-59` `@Mock CustomerPersistencePort`; `CustomerLocationServiceTest.java:22-23` `@Mock LocationCountPort` | ✅ PASS |
| ARCH-07 | `..adapter.in.web..` depends on no `..adapter.out..` and no `..application.port.out..` | HR:111-116; HAT:29; HRD:30-31. Round 1: M6 and M8 killed | ✅ PASS |
| ARCH-08 | `..adapter.out.persistence..` depends on no `..adapter.in..` | HR:119-124; HAT:32-33; HRD:32 (`PersistenceUsingController`) | ✅ PASS |
| ARCH-09 | Every output port is implemented by a class in `..adapter.out..` | `CustomerPersistenceAdapter.java:30` `implements CustomerPersistencePort, LocationCountPort`; HR:127-131; HAT:36-37; HRD:33-34; the context load in `CustomerApiSddApplicationTests.java:12` | ✅ PASS |
| ARCH-10 | `@RestController` lives in `..adapter.in.web..`; `@Entity` and Spring Data repositories live in `..adapter.out.persistence..` | HR:148-152, HR:134-138, HR:141-145; HAT:40-46; HRD:35-37; production `CustomerController.java:34`, `CustomerLocationController.java:12`, `SpringDataCustomerRepository.java:10`, `CustomerJpaEntity.java:20` | ✅ PASS |
| ARCH-11 | Every HTTP IT of customer-management and geo-grouping passes, with assertions unchanged | Gate: every HTTP IT is green (CreateAndGet 32, Update 18, List 16, Delete 4, ErrorContract 8, Location 11, Operability 11). No expected value is weakened (see the ARCH-11 section). CUST-12 on PUT: `CustomerUpdateIntegrationTest.java:207` `isConflict()`, `:209` field `email`, `:211` `assertWriteReachedTheDatabase()`, `:212` `assertAnaUnchanged()`; for cpf `:220/:222/:224/:225`. On POST: `CustomerCreateAndGetIntegrationTest.java:173/175/176` and `:185/187/188`. CUST-24: `CustomerUpdateIntegrationTest.java:269` `assertProblem(...,409,...)`, `:272-276` stored values | ✅ PASS |
| ARCH-12 | Grouping runs exactly one SQL statement and loads zero JPA entities | `customer/location/CustomerLocationIntegrationTest.java:178` `getPrepareStatementCount()).isEqualTo(1)`, `:179` `getEntityLoadCount()).isZero()`. Round 1: M12 killed | ✅ PASS |
| ARCH-13 | Flyway migrations are unchanged; Hibernate schema validation passes | `git diff --stat 9796beb..a11b2d3 -- src/main/resources` is empty; `application.properties:9` `spring.jpa.hibernate.ddl-auto=validate`; `CustomerApiSddApplicationTests.java:12` `contextLoads` passes. Round 1: M13 killed | ✅ PASS |

**Status**: 13/13 ACs have `file:line` evidence, and the spec outcomes match. The FAIL comes from the non-AC design guard (the CUST-24 same-transaction guarantee) that fix iterations 1-3 added. It does not come from an AC.

**Edge cases**:
- IF a domain class imports Spring or JPA THEN the build fails: HRD:26/35, round-1 M7a and M7b. ✅
- IF a controller injects a repository or an output port THEN the build fails: HRD:30, round-1 M8. ✅

---

## ARCH-11: HTTP Integration-Test Assertion Diff (`9796beb..a11b2d3`)

**Round-1 findings (`9796beb..4cfd2ff`)**, carried over with lines re-located at HEAD:
- HTTP ITs with zero diff: `CustomerDeleteIntegrationTest`, `CustomerListIntegrationTest`, `ErrorContractIntegrationTest`, `customer/location/CustomerLocationIntegrationTest`, `CustomerApiSddApplicationTests`.

| File (HEAD line) | Change | Kind | Expected value changed? | Judgement |
| ---------------- | ------ | ---- | ----------------------- | --------- |
| `HttpIntegrationTestSupport.java:66-71,93-95` | The spy type changes from `CustomerRepository` to `SpringDataCustomerRepository`; adds `@Autowired CustomerPersistencePort persistence` and `stored(UUID)` | arrangement | n/a | OK (sanctioned by the design) |
| `OperabilityIntegrationTest.java:132` | `.contains("customer.CustomerService")` becomes `.contains("service.CustomerService")` | assertion | logger fragment only | ✅ Known intended change |
| `CustomerCreateAndGetIntegrationTest.java:54, 84, 210` | reads stored state via `stored(id).getDetails()` | extractor | No; the expected tuples are identical | ✅ Known intended change |
| `CustomerUpdateIntegrationTest.java:125` | `stored(anaId).getDetails().phone()/birthDate()`, still `isNull()` | extractor | No | ✅ Known intended change |
| `CustomerUpdateIntegrationTest.java:252-277` (CUST-24) | the concurrent first writer uses `persistence.findById` + `persistence.update(...)` in `REQUIRES_NEW` | arrangement | No; `:269` 409 and `:272-276` values are identical | ✅ Not a weakening; it still discriminates (R-M2 → 500; N3+M1, X5+M1, Y7+M1 and Y8+M1 → 200) |

- **Fix iterations 1-2**: they only added race tests, `clearInvocations` and `assertWriteReachedTheDatabase()`. This was verified in rounds 2 and 3.
- **Fix iteration 3 (`f2d371c..a11b2d3`)**: `git diff f2d371c..a11b2d3 -- src`. Every `-` line is listed here:
  1. `HexagonalRules.java`: the comment line and `.onlyCallMethodsThat(...)` are replaced by `.onlyCallCodeUnitsThat(... .or(declaredIn(Object.class)))`.
     - This is strictly stronger: methods are a subset of code units, and the method predicate is the same.
     - The only new allowance is `Object`'s code units (for `super()`).
  2. `CustomerCreateAndGetIntegrationTest.java` and `CustomerUpdateIntegrationTest.java`:
     - the now-unused `clearInvocations` and `doReturn` static imports are removed (2 + 2 lines);
     - in each of the four race tests, `doReturn(false).when(repository).<preCheck>(...)` plus `clearInvocations(repository)` becomes `preCheckMisses(r -> r.<preCheck>(...))`, with the same pre-check method and matchers. The helper (`HttpIntegrationTestSupport.java:78-81`) performs the same two calls, in the same order.
- **Assertions**: every `isConflict()`, `contains(field)`, `assertWriteReachedTheDatabase()` and `assertAnaUnchanged()` line is untouched. Added: `TransactionBoundaryIntegrationTest` (new, 2 tests) and one HRD row.

**Conclusion**: no asserted expected value is weakened across `9796beb..a11b2d3`.

---

## Diff Audit `f2d371c..a11b2d3`

- `git diff --stat f2d371c..a11b2d3 -- src/main` is **empty**. Production code is untouched, as required. `src/main/resources` is also unchanged across the full range.
- Changed files:
  - `design.md` (+3/-1)
  - `tasks.md` (+94/-1)
  - two fixtures (`constructor/CopiedCpfCheck.java` +12, `constructor/CpfValidator.java` +13)
  - `HttpIntegrationTestSupport.java` (+13)
  - `HexagonalRules.java` (+4/-2)
  - HRD (+2)
  - Create IT (+2/-6)
  - Update IT (+2/-6)
  - `TransactionBoundaryIntegrationTest.java` (+65, new)
- **design.md vs code**:
  - The CPF row ("no method or constructor other than the domain rule, its own and `Object`'s") matches HR:174-175. ✅
  - `design.md:103-104` inserts a paragraph **inside** the rules table. The CPF row and the layering row after it (`:105-106`) are then cut off from the table and render as stray pipe text. This is a cosmetic doc defect.
  - `design.md:104` says the probe "observes an active read-write transaction inside POST and PUT, and catches method-level overrides (X5)". That is accurate, but it does not prove "one transaction" (Y7, Y8).
  - `design.md:127` (Risks) says: "In production the use case runs in one transaction … the adapter gets back the entity the service already read, and Hibernate's versioned UPDATE … catch the conflict". This is **not enforced**. Y7 and Y8 keep 300/300 green while the adapter reloads a fresh entity in a new transaction. Y7+M1 and Y8+M1 give a lost update (see the Sensor).
- **T13 status text vs reality**: T13 says it "covers every way of losing the use-case transaction (class or method level, any propagation, readOnly)". That is correct for losing the transaction. It is not correct for splitting it (REQUIRES_NEW at the write). DELETE is not probed.

---

## Discrimination Sensor

### Isolation

- All runs used the detached worktree `.worktrees/verify-hex4` at `a11b2d3`, with `./mvnw -B clean test [-Dtest=...]`.
- Mutations were applied by exact-match string replacement. `scratchpad/sensor4/mut.py` asserts exactly one occurrence, and this round it also supports empty replacements (the round-3 tooling slip).
- `run.sh` refuses to run when nothing changed or when a mutation step failed.
- After each run, `git reset -q --hard HEAD && git clean -fdq src` was executed. **`porcelain-after: []` was logged after every one of the 21 runs** (`batch1.out`, `batch2.out`, `batch3.out`).
- The worktree was removed with `git worktree remove --force`. `git worktree list` now shows only the main tree.
- The main tree's porcelain was ` M .specs/LESSONS.md`, ` M .specs/lessons.json` before and after. These changes are pre-existing and not the verifier's. The branch stayed `refactor/hexagonal` at `a11b2d3`.
- No stash, commit, push or branch change.
- Artifacts are in `scratchpad/sensor4/<id>.diff|.log` and the scripts in `batch.sh`, `batch2.sh`, `batch3.sh`, `m1.sh`, `m3b.sh` and `run.sh`.

### Rounds 1-3 (carried over)

| Round | At | Mutations | Killed | Survived |
| ----- | -- | --------: | -----: | -------- |
| 1 | `4cfd2ff` | 20 | 16 | 4: M3b, M10 and M11b (plausible); M7c (equivalent) |
| 2 | `87626bf` | 14 production + 1 control | 12 | 2: N3 and N4b (plausible), plus the vacuous N1b control |
| 3 | `f2d371c` | 22 | 17 | 5: X5 (plausible); A1 (adversarial); X14 and X15 (compound); X16 (equivalent) |

Details are in `validation-hexagonal-architecture-final2.md`. Round 3 included:
- M1: killed by adapter IT:103 only
- M2: 409 → 500
- M3b: killed since round 2
- X1 (MANDATORY), X2 (NEVER), X3 (JTA annotation), X4 (location SUPPORTS)
- X6 (`readOnly` update): 409 → 200
- X7-X13: all killed

### Round 4 (this round, `a11b2d3`)

**Re-runs of earlier survivors**:

| # | Mutation (diff file) | Tests run | Result |
| - | -------------------- | --------- | ------ |
| R-X5 | Method-level `@Transactional(propagation = SUPPORTS)` on `CustomerService.update`; the class-level annotation is kept (identical to `sensor3/X5-update-method-SUPPORTS.diff`) | full | ✅ **Killed** (299/300). `TransactionBoundaryIntegrationTest.updateRunsInsideAReadWriteTransaction:57`: expected `Observed[active=true, readOnly=false]` but was `Observed[active=false, readOnly=false]` |
| R-N3 | Class-level `@Transactional(propagation = SUPPORTS)` | full | ✅ **Killed** (297/300): `useCaseServicesAreTransactional` (HAT:49 → HR:158-164), plus the probe at `:40` and `:57` |
| R-M10 | Class-level `@Transactional` removed | full | ✅ **Killed** (297/300): `useCaseServicesAreTransactional`, plus the probe at `:40` and `:57` |
| R-A1 | New `validation/CpfDigits` whose constructor holds the copied algorithm; the validator keeps the decoy `Cpf.isValid(value)` and returns `new CpfDigits(value).valid` (identical to the round-3 A1) | full | ✅ **Killed** (299/300): `cpfValidationDelegatesToDomain` (HAT:52 → HR:167-176), "Method `CpfValidator.isValid(String, ConstraintValidatorContext)` calls constructor `CpfDigits.<init>(String)` in (CpfValidator.java:15)" |
| R-X14a | X14 as it can now happen: the email PUT race test passes the wrong pre-check to `preCheckMisses` (so `clearInvocations` can no longer be forgotten), plus M3b | that test | ✅ **Killed**: `CustomerUpdateIntegrationTest:211` → `HttpIntegrationTestSupport.assertWriteReachedTheDatabase:90` |
| R-X14b | The literal round-2 end state: the test **bypasses the helper** with a raw `Mockito.doReturn(false)...existsByCpfAndIdNot(...)` and no `clearInvocations`, plus M3b | that test | ⚫ **Survived** (1/1). **Compound**: it needs two test faults (bypassing the helper and stubbing the wrong method) plus the production fault. The arrangement's own `saveAndFlush` satisfies the helper |
| R-X15 | The helper matches `"findById"` **and** both PUT race tests swap stubs through `preCheckMisses`, plus M3b | Update IT | ⚫ **Survived** (18/18). **Compound**: two independent test faults. `findById` is always invoked by `update`. T15 did not target this route |

**New mutations**: `P` = plausible, `A` = adversarial, `C` = compound (two or more independent faults), `E` = equivalent.

| # | Class | Site (HEAD) | Mutation | Tests run | Result |
| - | ----- | ----------- | -------- | --------- | ------ |
| Y1 | P | `CustomerService.java:44-45` | Method-level `@Transactional(readOnly = true)` on `create` | full | ✅ Killed (67 failures). The probe `createRuns…:40`, plus every POST-backed test: 201 → 500 with "cannot execute INSERT in a read-only transaction" |
| Y2 | P | `CustomerService.java:69-70` | Method-level `@Transactional(propagation = NOT_SUPPORTED)` on `update` | full | ✅ Killed (1): `updateRunsInsideAReadWriteTransaction:57` (`active=false`) |
| Y3+X5 | C (test fault + production fault) | `TransactionBoundaryIntegrationTest.java:61` + X5 | The probe records `isSynchronizationActive()` instead of `isActualTransactionActive()`, together with X5 | full | ⚫ Survived (300/300). Under SUPPORTS, Spring keeps synchronization active without a real transaction, so the probe goes blind. Control run: Y3 alone is green at HEAD (2/2), a silent weakening that nothing guards |
| Y4 | P (test-side) | `TransactionBoundaryIntegrationTest.java:50` | The update probe copy-pastes the create stub (`existsByEmail(anyString())`) | probe | ✅ Killed at HEAD: `:57` expected `Observed[true,false]` but was `null`. A probe on an uncalled method detects itself |
| Y5 | P (test-side) + M3b | `HttpIntegrationTestSupport.java:80` | `preCheckMisses` forgets `clearInvocations`, plus M3b | Create + Update IT | ✅ Killed (2): `CustomerUpdateIntegrationTest:207` and `:220`, 409 → 500. The stubs are still right, so M3b surfaces |
| Y5b | C | `:80` + `CustomerUpdateIntegrationTest.java:205` | The helper forgets `clearInvocations` **and** the email PUT race test stubs the wrong pre-check, plus M3b | that test | ⚫ Survived (1/1). This is the X14 shape moved into the helper; it needs two test faults |
| Y6 | P (rule-side) | `HexagonalRules.java:175` | The CPF rule allows all of `java.lang` (`declaredIn(resideInAPackage("java.lang"))` instead of `declaredIn(Object.class)`) | HAT + HRD | ✅ Killed (1): `HexagonalRulesDiscriminationTest.ruleReportsItsViolationFixture[15]:53`. The `ignoring.CpfValidator` fixture (decoy plus `String.matches`) is no longer reported |
| **Y7** | **P** | `CustomerService.java:39-42,78` | `update` runs `persistence.update(...)` inside a `TransactionTemplate` with `PROPAGATION_REQUIRES_NEW`. The `@Autowired` constructor takes a `PlatformTransactionManager`, and the 2-arg constructor uses `TransactionOperations.withoutTransaction()`, so the unit tests compile | full | ❌ **Survived** (300/300). The read and the pre-checks run in the outer transaction, where the probe sees `active=true, readOnly=false`. The versioned write runs in a new transaction, where the adapter reloads a fresh entity |
| Y7+M1 | P+P (consequence probe) | Y7 + `CustomerPersistenceAdapter.java:59-61` explicit check removed | | Update IT + adapter IT + probe | ✅ Killed (2): `CustomerUpdateIntegrationTest.concurrentUpdate…:269` **409 → 200 (lost update)**, plus adapter IT `:103` |
| **Y8** | **P** | `CustomerPersistenceAdapter.java:55-56` | One annotation, `@Transactional(propagation = Propagation.REQUIRES_NEW)`, on the adapter's `update` | full | ❌ **Survived** (300/300). Same mechanism as Y7 |
| Y8+M1 | P+P (consequence probe) | Y8 + explicit check removed | | Update IT + adapter IT + probe | ✅ Killed (2): `:269` **409 → 200 (lost update)**, plus adapter IT `:103` |
| M1 (control) | — | explicit check removed alone | | same 29 tests | Only adapter IT `:103` fails; CUST-24 stays **409**. This proves the 200 in Y7+M1 and Y8+M1 comes from the split transaction |
| Y9 | E (behaviour) | `CustomerService.java:83-84` | Method-level `@Transactional(propagation = SUPPORTS)` on `delete` | full | ⚪ Survived (300/300). `delete` is `find` + `deleteById`. `deleteById` is itself transactional, and no spec outcome or guard depends on a shared transaction, so no consequence can be constructed. It does contradict the probe's javadoc wording ("the write use cases") |

**Round 4 result**: 19 counted mutations. The two consequence probes are counted, and the 2 control runs (Y3 alone, M1 alone) are not.

| Group | Mutations | Killed | Survived |
| ----- | --------: | -----: | -------- |
| Re-runs of earlier survivors | 7 (R-X5, R-N3, R-M10, R-A1, R-X14a, R-X14b, R-X15) | 5 | 2 compound (R-X14b, R-X15) |
| New plausible (incl. test/rule-side) | 9 (Y1, Y2, Y4, Y5, Y6, Y7, Y7+M1, Y8, Y8+M1) | 7 | **2 plausible: Y7, Y8** |
| New compound | 2 (Y3+X5, Y5b) | 0 | 2 |
| Equivalent | 1 (Y9) | 0 | 1 |
| **Total** | **19** | **12** | **7** (2 P, 0 A, 4 C, 1 E) |

**Classification rationale**:
- **Y8 is plausible.** It is a single ordinary annotation on an adapter method, the layer where hexagonal codebases commonly put `@Transactional`. `REQUIRES_NEW` is the propagation the test suite itself uses (CUST-24 arrangement). It has no reflection, no naming trick and no new class, which are exactly the grounds on which round 3 called X5 plausible. Its consequence signature (+M1 → 200) is identical to X5's.
- **Y7 is plausible, but less likely.** It is a deliberate multi-line change (programmatic transaction management in the service). It is not written to evade a rule, and the dispatch listed it as a plausible attack. It is not adversarial under the bar's definition.
- **Y3+X5, Y5b, R-X14b and R-X15 are compound.** Each needs at least two independent faults, and at least one of them is in test code. By the bar's definition, compound test-code mutations are residual risks.
  - Round 3 labelled X12 (one test fault plus M3b) "P (test-side)". The same shape here (Y5) is killed, so the label has no effect on the verdict.
- **Y9 is equivalent in behaviour.** It is listed because it shows that DELETE is outside both the rule's reach (method level) and the probe's.

**All earlier plausible and adversarial survivors are killed**:
- from round 1: M3b, M10 and M11b;
- from round 2: N3, N4b and N1b;
- from round 3: X5 and A1.

**Sensor depth**: expanded (P0-style: data integrity and concurrency). **Verdict impact**: FAIL ❌ (Y7 and Y8, plausible survivors).

---

## Code Quality (fix iteration 3)

| Check | Status |
| ----- | ------ |
| Minimum code / no scope creep | ✅ One new IT class (2 tests), one rule tightening, one fixture pair, one HRD row, one helper and four call-site simplifications. No production change |
| Surgical changes | ✅ The `src/main` diff is empty |
| Matches patterns | ✅ The helper and the probe are javadoc'd. The probe's `null`-on-miss design detects a wrong stub by itself (Y4) |
| Tests map to ACs | ✅ T13 → the CUST-24 design guard (ARCH-11 context); T14 → ARCH-03; T15 → CUST-12 |
| Doc accuracy | ⚠️ `design.md:103-104` breaks the rules table. The claim that the use case "runs in one transaction" (`design.md:127`) and T13's "every way of losing the transaction" overstate what is enforced (Y7, Y8) |

---

## Spec-Precision Gaps

1. **ARCH-11 wording** (carried over): "assertions unchanged" conflicts with the sanctioned extractor and logger-fragment changes. They are judged non-weakening. The spec should say "expected values unchanged".
2. **ARCH-03 enforcement strength**: now enforced as "calls `Cpf.isValid` and no other method or constructor except its own and `Object`'s". The spec's Success Criteria still do not say how strongly it must be enforced.
3. **ARCH-05 / ARCH-09 completeness** (carried over): the rules prove shape and placement. Completeness rests on compilation and the context load.
4. **CUST-24 transaction boundary**: not an ARCH AC. design.md promises "one transaction" and "the adapter gets back the entity the service already read". Neither the rule nor the probe enforces the "same transaction" half (Y7, Y8).

---

## Residual Risks (not verdict-failing)

| Risk | Classification | One-line mitigation |
| ---- | -------------- | ------------------- |
| R-X14b: a race test bypasses `preCheckMisses` with a raw wrong stub | Compound (2 test faults + production) | Optional: an ArchUnit/grep check that only `HttpIntegrationTestSupport` calls `doReturn` on `repository` |
| R-X15: the helper's hard-coded `"saveAndFlush"` is changed to an always-invoked method, and the stubs are wrong | Compound (2 test faults + production) | Optional: assert that `saveAndFlush` is **absent** before the request in each race test (self-check), or accept |
| Y3+X5: the probe reads the wrong flag (`isSynchronizationActive`) and X5 is present | Compound (test + production) | Optional: a sanity assertion inside the probe that it sees `active=false` when called outside any transaction |
| Y5b: the helper forgets `clearInvocations` and one stub is wrong | Compound | Same as R-X15 |
| Y9: DELETE is not probed, and a method-level SUPPORTS on it is invisible | Equivalent (no consequence) | Reword the probe's javadoc to "create and update", or probe DELETE too |
| Reflection, `MethodHandle` or bytecode routes around the CPF rule | Adversarial | `Method.invoke` is itself a foreign call and fails the rule; otherwise code review |
| X3: the rule rejects `jakarta.transaction.Transactional` | Over-strict, harmless | None needed |
| X16: the `.orElse("REQUIRED")` fallback is unreachable | Equivalent | None needed |
| The CUST-24 design guards are HTTP-invisible while the adapter's explicit check is live | Defense in depth | M1 is killed by adapter IT:103, so today at least one CUST-24 guard is always test-enforced |

---

## Ranked Gaps

1. **[Minor] Y8 and Y7 survived: the versioned write can run in a separate `REQUIRES_NEW` transaction, and nothing notices.**
   - **Where**: `TransactionBoundaryIntegrationTest.java:44-58` probes only the pre-check. `HexagonalRules.java:158-164` looks only at service classes. `CustomerPersistenceAdapter.update` (Y8) and programmatic templates in the service (Y7) are unconstrained.
   - **Consequence**: one of CUST-24's two guards (Hibernate's same-transaction versioned UPDATE) is lost, and a single further fault (M1) becomes a lost update (409 → 200, proven).
   - **Fix options**, cheapest first:
     - (a) **Accept by user decision.** Record in STATE.md that CUST-24 is enforced by the adapter's explicit version check (M1 is killed by adapter IT:103), and that "one transaction" is defense in depth enforced only against losing the transaction. Correct `design.md:127` and the T13 wording to match.
     - (b) **Probe the transaction count.** With Hibernate statistics (already enabled for ARCH-12), assert that a PUT (and a POST, and optionally a DELETE) completes exactly **one** transaction. For example, `statistics.clear()` before the request, then `getTransactionCount()` (or `getSuccessfulTransactionCount()`) equals 1 afterwards. This should kill X5, N3, Y7, Y8 and Y9 at once. **Verify the counter semantics** under `JpaTransactionManager` first.
     - (c) **Probe at the write.** A Hibernate `StatementInspector` (test-only property) records `TransactionSynchronizationManager.getCurrentTransactionName()` when the `update customer` statement runs. Assert that it equals `…CustomerService.update`. Y7 gives `null`, and Y8 gives `…CustomerPersistenceAdapter.update`.
   - **Done when**: Y7 and Y8 fail, X5, N3 and M10 still fail, and HEAD stays green.
2. **[Cosmetic] `design.md:103-104` splits the ArchUnit rules table.** Move the paragraph below the table.
3. **[Cosmetic, optional] Compound test-fault vacuity (R-X14b, R-X15, Y3+X5, Y5b).** See Residual Risks.

---

## Requirement Traceability Update (proposed)

| Requirement | Previous (round 3) | New Status |
| ----------- | ------------------ | ---------- |
| ARCH-01, 02, 04-10, 12, 13 | ✅ Verified | ✅ Verified |
| ARCH-03 | ✅ Verified (A1 residual) | ✅ Verified (A1 killed; rule guarded by HRD, Y6 killed) |
| ARCH-11 | ✅ Verified | ✅ Verified (no expected value weakened; race tests through one helper) |
| Design guard: CUST-24 transaction boundary (not an AC) | ⚠️ X5 survived | ⚠️ X5, N3, M10, Y1 and Y2 killed; **a split transaction (Y7, Y8) survives** |

---

## Summary

- **Overall**: ❌ Not Ready. The blocker is two Minor, HTTP-invisible, plausible survivors (Y7, Y8) in the non-AC "use case runs in one transaction" guard. CUST-24 behaviour itself is correct and test-enforced through the adapter's explicit version check.
- **Spec-anchored check**: 13/13.
- **Gate**: 300 passed, 0 failed, 0 skipped (`./mvnw -B clean verify`, 1:04 min).
- **Sensor, all four rounds**:

| Round | Mutations | Killed | Survived |
| ----- | --------: | -----: | -------- |
| 1 | 20 | 16 | 4 (M3b, M10, M11b; M7c equivalent) |
| 2 | 14 + 1 control | 12 | 2 (N3, N4b), plus the N1b control |
| 3 | 22 | 17 | 5: 1 P (X5), 1 A (A1), 2 C (X14, X15), 1 E (X16) |
| 4 | 19 + 2 controls | 12 | 7: 2 P (Y7, Y8), 0 A, 4 C (R-X14b, R-X15, Y3+X5, Y5b), 1 E (Y9) |

- **What works**:
  - Every earlier plausible and adversarial survivor is killed.
  - The probe catches the transaction being lost in any way: class or method level, SUPPORTS, NOT_SUPPORTED, NEVER, missing annotation, or `readOnly`.
  - The CPF rule now also blocks constructor routes, and its own loosening is caught by HRD.
  - The race-test arrangement can no longer forget `clearInvocations`.
  - Fix iteration 3 touched no production code and weakened no assertion.
- **Next steps**: this was the last allowed fix round. Recommended: user decision (Gap 1 option (a)), or one more small test (option (b)) with a targeted re-check of Y7, Y8, X5 and N3.

Note: per the dispatch instructions this report lives in the session scratchpad, not in `.specs/features/hexagonal-architecture/validation.md`. For that reason `validate_state.py` and `lessons.py` were not run (they read or modify the repo). Candidate lessons are in the chat reply.
