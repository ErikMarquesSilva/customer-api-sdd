# Customer Geographic Grouping Validation

**Verdict**: PASS
**Date**: 2026-09-22
**Spec**: `.specs/features/customer-geo-grouping/spec.md` (GEO-001..GEO-019)
**Diff range**: `e01ecd8..3f95c98` (fe42b2c T1, 9d78ec7 T2, 6627480 T3, 3f95c98 T4)
**Verifier**: independent sub-agent (author ≠ verifier). This is round 2: round 1 verified `e01ecd8..6627480` (PASS, 17/17, 11/11 mutants). Round 2 re-verified at `3f95c98` in the detached worktree `.worktrees/verify-geo2`, which was removed after the run.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 Aggregate query | ✅ Done | `CustomerRepository.countByLocation()` + `LocationCount`. `CustomerLocationQueryIntegrationTest` has 5 T1 tests plus 1 T4 test |
| T2 Grouping service + DTOs | ✅ Done | `CustomerLocationService`, `LocationGroupingResponse` / `StateGroup` / `CityGroup`; 7 unit tests |
| T3 Grouping endpoint | ✅ Done | `CustomerLocationController`; 11 integration tests; `hibernate.generate_statistics` is enabled in the test config only |
| T4 Verifier follow-ups | ✅ Done | Adds the GEO-018 PostgreSQL regression test and relabels the existing tie-break test as GEO-019 |

**Follow-up commit `6627480..3f95c98`**: `git diff --name-only 6627480..3f95c98 -- src/main` returns **0 files**. It changes only `spec.md` (2 assumption rows, GEO-018/019, traceability rows, coverage line), `tasks.md` (T4), `CustomerLocationQueryIntegrationTest.java` (+12 lines, 1 new test) and `CustomerLocationServiceTest.java` (1 comment line relabelled, line count unchanged). No existing assertion was changed or weakened. `CustomerLocationIntegrationTest.java` is byte-identical to the round-1 version.

---

## Gate Check

- **Gate command**: `./mvnw -B verify` (the Build gate from tasks.md)
- **Result**: **235 passed, 0 failed, 0 errors, 0 skipped**. BUILD SUCCESS, exit 0
- **Feature tests**: `CustomerLocationQueryIntegrationTest` 6, `CustomerLocationServiceTest` 7, `CustomerLocationIntegrationTest` 11 (24 in total)
- **Test count before feature** (`e01ecd8`): 211. **After**: 235. **Delta**: +24 (round 1 +23, T4 +1). No skips. No tests deleted. No assertions weakened.

---

## Spec-Anchored Acceptance Criteria

Paths are abbreviated. `IT` = `src/test/java/com/example/customerapi/customer/location/CustomerLocationIntegrationTest.java`, `QIT` = `.../CustomerLocationQueryIntegrationTest.java`, `ST` = `.../CustomerLocationServiceTest.java`. Line numbers are re-checked at `3f95c98`. The round-1 references that moved are marked (moved). All other references were checked and still match.

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| -- | -------------------- | ----------------------- | ------ |
| GEO-001 | 200, JSON `{"states":[{"state","totalCustomers","cities":[{"city","totalCustomers"}]}]}` | `IT:46-55` - `status().isOk()`, `contentTypeCompatibleWith(APPLICATION_JSON)`, `content().json({"states":[{"state":"MG","totalCustomers":1,"cities":[{"city":"Uberlândia","totalCustomers":1}]},{"state":"SP",...}]}, JsonCompareMode.STRICT)` | ✅ PASS |
| GEO-002 | One entry per UF with customers; total = customers in the UF | `IT:48-55` STRICT: exactly MG (1) and SP (3); `ST:35-37` - `isEqualTo(new LocationGroupingResponse(List.of(new StateGroup("MG",30,...), new StateGroup("SP",150,...))))` | ✅ PASS |
| GEO-003 | One city entry per city with customers; total = customers in the city and state | `IT:52-53` - Campinas 2, Limeira 1 (STRICT); `QIT:57-59` - `containsExactlyInAnyOrder(tuple("SP","campinas",2L), tuple("MG","Belo Horizonte",3L))` | ✅ PASS |
| GEO-004 | State total = sum of the city totals | `ST:35-37` - `StateGroup("SP", 150, [Campinas 100, Limeira 50])`; `IT:51` SP 3 = 2+1; `IT:84` MG 3 = 1+1+1 | ✅ PASS |
| GEO-005 | States in alphabetical order by UF | `ST:47-48` - `extracting(StateGroup::state).containsExactly("AC","BA","MG","RJ","SP")`; `IT:48-55` MG before SP (STRICT array order) | ✅ PASS |
| GEO-006 | pt-BR collation ignoring case and accents: `Águas de Lindóia`, `Bauru`, `campos do Jordão` | `ST:57-59` - `extracting(CityGroup::city).containsExactly("Águas de Lindóia","Bauru","campos do Jordão")` (input order reversed at `ST:55`) | ✅ PASS |
| GEO-007 | No customers gives 200 `{"states": []}` | `IT:68` - `status().isOk()` + `content().json("{\"states\": []}", STRICT)`; `ST:82`; `QIT:118` (moved from 106) - `isEmpty()` | ✅ PASS |
| GEO-008 | No id/name/email/cpf/phone/birthDate/createdAt/updatedAt | `IT:60-61` - `assertThat(body).doesNotContain("\"id\"","\"name\"","\"email\"","\"cpf\"","\"phone\"","\"birthDate\"","\"createdAt\"","\"updatedAt\"")`; the STRICT JSON at `IT:48-55` also rejects extra fields | ✅ PASS |
| GEO-009 | Same-state cities that differ only by case form one entry | `QIT:57-59` - `tuple("SP","campinas",2L)`, `tuple("MG","Belo Horizonte",3L)` (3 case variants); `IT:89` - `{"city":"Campinas","totalCustomers":2}` STRICT | ✅ PASS |
| GEO-010 | The same city name in different states gives a separate entry per state | `QIT:109-111` (moved from 97-99) - `containsExactlyInAnyOrder(tuple("SP","Santa Rita",1L), tuple("MG","Santa Rita",2L))`; `IT:85,90` STRICT | ✅ PASS |
| GEO-011 | Show the spelling of the earliest-created customer (ties broken by id) | `QIT:57-59` - rows saved out of creation order; expects `"campinas"` (T1) and `"Belo Horizonte"` (T1, the middle spelling, so it rules out both min and max); `QIT:71-73` - same instant, lowest id wins: `"campinas"`, `"Belo Horizonte"`; also `QIT:98` (non-ASCII, earliest `"São Carlos"` saved second) | ✅ PASS |
| GEO-012 | `Uberlândia` / `Uberlandia` are separate entries | `QIT:84-86` - `tuple("MG","Uberlândia",2L), tuple("MG","Uberlandia",1L)`; `IT:86-87` STRICT | ✅ PASS |
| GEO-013 | A deleted customer is excluded from the next response | `IT:103-109` - after DELETE 204: SP 2 = Campinas 1, Limeira 1 (STRICT) | ✅ PASS |
| GEO-014 | After a PUT that changes city/state, the customer counts only under the new location | `IT:120-130` - one customer moved to Curitiba/PR, one to Limeira/SP: PR 1 (Curitiba 1), SP 2 (Campinas 1, Limeira 1) STRICT | ✅ PASS |
| GEO-015 | A city or state with no customers left is omitted | `IT:140-145` - only `{"state":"PR",...,"Curitiba",1}`; `IT:156-161` - Limeira emptied by a delete, MG emptied by a move: only SP/Campinas 2 | ✅ PASS |
| GEO-016 | Exactly 1 SQL statement per call | `IT:178` - `assertThat(statistics.getPrepareStatementCount()).isEqualTo(1)` with 3 states / 5 cities, statistics cleared at `IT:174` | ✅ PASS |
| GEO-017 | 0 customer entities loaded | `IT:179` - `assertThat(statistics.getEntityLoadCount()).isZero()` | ✅ PASS |
| GEO-018 | `São Carlos` + `SÃO CARLOS` in one state give **one** city entry (UTF-8 LC_CTYPE database) | `QIT:92-98` - `save("SÃO CARLOS","SP",T2); save("São Carlos","SP",T1);` then `repository.countByLocation()...containsExactly(tuple("SP","São Carlos",2L))`. That is exactly one row with total 2, and the earliest spelling is saved second, so insertion order does not pick it. The run uses PostgreSQL `postgres:17-alpine` via Testcontainers (default `en_US.utf8` ctype). | ✅ PASS |
| GEO-019 | Cities equal ignoring case and accents are in plain character order: `Uberlandia` before `Uberlândia` | `ST:66-73` - rows fed in both orders; `assertThat(first).containsExactly(new CityGroup("Uberlandia", 2), new CityGroup("Uberlândia", 1))` and `assertThat(second).isEqualTo(first)`; end to end `IT:86-87` - `{"city":"Uberlandia"...}` then `{"city":"Uberlândia"...}` in STRICT array order | ✅ PASS |

**Status**: ✅ 19/19 ACs covered with evidence. Every asserted value matches the spec outcome. The two round-1 spec-precision gaps are resolved by GEO-018 and GEO-019.

---

## Edge Cases

- [x] Exactly one customer gives one state and one city, both with total 1: `IT:188-190` STRICT; `ST:91-92`
- [x] All customers in one city give state total = city total: `IT:201-203` SP 3 / Campinas 3 STRICT; `ST:101-102` RJ 7 / Niterói 7
- [x] Query parameters are ignored: `IT:213-220` - `?state=SP&city=Campinas` still returns MG and SP (STRICT)
- [x] Route precedence over `/api/v1/customers/{id}`: every `IT` test reaches the literal path and gets 200 (no 400 from UUID conversion)

---

## Discrimination Sensor

**Sensor depth**: expanded (14 manual behavior-level mutations in two rounds, covering every branch of the new query and service)
**Method**: each mutation was an in-place edit in a throwaway detached worktree, never the real tree. The covering tests ran with `./mvnw -B test -Dtest=...`, and the file was restored with `git checkout -- src`. `git status --porcelain` showed 0 lines before the sensor and after every mutation. `git stash` was not used.

### Round 1 (at `6627480`, 11 mutations, `-Dtest='CustomerLocation*'`)

| # | Mutation | Location | Killing tests | Result |
| - | -------- | -------- | ------------- | ------ |
| M1 | `GROUP BY c.state, lower(c.city)` changed to `GROUP BY c.state, c.city` | `CustomerRepository.java:31` | QIT `casesOfOneCity…`, QIT `sameCreationInstant…`, IT `citiesGroupIgnoringCase…` | ✅ Killed |
| M2 | Latest spelling instead of earliest: `ORDER BY c.created_at DESC, c.id DESC` | `CustomerRepository.java:28` | QIT `casesOfOneCity…`, QIT `sameCreationInstant…`, IT `citiesGroupIgnoringCase…` | ✅ Killed |
| M3 | `min(c.city)` instead of the earliest-created spelling | `CustomerRepository.java:28` | QIT `casesOfOneCity…`, QIT `sameCreationInstant…` | ✅ Killed |
| M4 | State dropped from the group key: `SELECT min(c.state)`, `GROUP BY lower(c.city)` | `CustomerRepository.java:27,31` | QIT `sameCityNameInDifferentStates…`, IT `citiesGroupIgnoringCase…` | ✅ Killed |
| M5 | Plain `String` city order instead of the pt-BR Collator | `CustomerLocationService.java:28` | ST `citiesAreSortedByPtBrCollation…` | ✅ Killed |
| M6 | `TreeMap` replaced with `HashMap` (states unsorted) | `CustomerLocationService.java:39` | ST `statesAreSortedAlphabeticallyByUf` | ✅ Killed |
| M7 | State total = number of cities instead of the sum | `CustomerLocationService.java:46` | 6 failures (2 ST, 4 IT) | ✅ Killed |
| M8 | `findAll()` loads entities and groups in Java | `CustomerLocationService.java:40` | IT `oneCallRunsOneStatementAndLoadsNoEntity` (`getEntityLoadCount` 5, expected 0) + ST | ✅ Killed |
| M9 | Id tie-break reversed: `ORDER BY c.created_at, c.id DESC` | `CustomerRepository.java:28` | QIT `sameCreationInstantPicksTheSpellingOfTheLowestId` | ✅ Killed |
| M10 | `.thenComparing(CityGroup::city)` tie-break removed | `CustomerLocationService.java:28` | ST `citiesEqualIgnoringAccentsKeepTheSameOrder…` | ✅ Killed |
| M11 | Redundant second `countByLocation()` call (2 statements) | `CustomerLocationService.java:40` | IT `oneCallRunsOneStatementAndLoadsNoEntity` (`getPrepareStatementCount` 2, expected 1) | ✅ Killed |

### Round 2 (at `3f95c98`, 3 mutations on the GEO-018 / GEO-019 paths)

| # | Mutation | Location | Tests run | Killing tests (observed failure) | Result |
| - | -------- | -------- | --------- | -------------------------------- | ------ |
| M12 (a) | `lower(c.city)` changed to `lower(c.city COLLATE "C")` (folds ASCII only) | `CustomerRepository.java:31` | `CustomerLocationQueryIntegrationTest` (6) | QIT `casesDifferingInNonAsciiLettersFormOneRow` at `QIT:98`: actual `[("SP","SÃO CARLOS",1L), ("SP","São Carlos",1L)]`, expected `[("SP","São Carlos",2L)]`. The other 5 QIT tests pass, so this test is the only guard for non-ASCII folding | ✅ Killed |
| M13 (b) | Tie-break reversed: `.thenComparing(CityGroup::city, Comparator.reverseOrder())` (`Uberlândia` first) | `CustomerLocationService.java:28` | `CustomerLocationServiceTest` (7); rerun with `CustomerLocation*` (24) | ST `citiesEqualIgnoringAccentsKeepTheSameOrderWhateverTheRowOrder` (actual `[Uberlândia 1, Uberlandia 2]`); in the 24-test rerun it was also killed end to end by IT `citiesGroupIgnoringCaseButNotAccentsAndPerState` (2 failures) | ✅ Killed |
| M14 (c) | Locale-independent ASCII-only folding: `GROUP BY c.state, translate(c.city, 'A…Z', 'a…z')`. This is a plausible rewrite that removes the ctype dependency but breaks GEO-018 | `CustomerRepository.java:31` | `CustomerLocation*` (24) | QIT `casesDifferingInNonAsciiLettersFormOneRow`. It was the only failure (1/24); ASCII case merging (GEO-009) still passed | ✅ Killed |

**Result**: **14/14 killed** (round 1: 11/11; round 2: 3/3). ✅ PASS

---

## Spec-Precision Gaps

Both round-1 gaps are **resolved**:

1. The GEO-006 tie-break for collation-equal names is now **GEO-019** plus an assumption row. It is pinned by `ST:72` and `IT:86-87`, and M10 and M13 are killed.
2. Non-ASCII case folding under GEO-009 is now **GEO-018** plus the "Database character locale" assumption (UTF-8 `LC_CTYPE`). It is pinned by `QIT:98`, and M12 and M14 are killed.

Remaining (non-blocking):

- **GEO-018 is an environment precondition that nothing in the repository enforces.** The spec logs it as an assumption. The repository has no compose/infra file that sets the production database locale and no startup check, so a production database created with `C`/`POSIX` ctype would silently split `SÃO CARLOS` / `São Carlos`. The test container covers only the default `postgres:17-alpine` locale.
- **GEO-019 is tested only with accent-only variants.** Pairs that differ in both case and accent (for example `uberlândia` vs `Uberlandia`) are also collation-equal and separate entries. The same comparator orders them (`U` < `u` by code point), but no test pins that. This is low risk because the code path is identical.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ one native query, one service method, 3 records, a delegating controller; T4 adds a test only |
| Surgical changes | ✅ only `CustomerRepository` modified outside `customer/location/`; T4 touches no `src/main` file |
| No scope creep | ✅ no filters, pagination, caching or indexes (matches Out of Scope) |
| Matches patterns | ✅ the new QIT test reuses the file's `save(...)` helper and extraction/tuple style |
| Spec-anchored outcome check | ✅ exact STRICT JSON and exact tuples/values, including GEO-018 `containsExactly` (it rejects extra rows) |
| Per-layer coverage | ✅ query rules (GEO-009..012, 018) at the query level; service 1:1 for GEO-002..007 and 019; route happy path, empty case, data-change cases and efficiency end to end |
| Every test maps to an AC / edge case / Done-when | ✅ every test is labelled with its GEO id or edge case (the relabelled ST test now names GEO-019) |
| Documented guidelines | none found; strong defaults applied |

---

## Ranked Gaps

None block the feature. Suggestions, in priority order:

1. (Minor, operational) GEO-018 depends on the production database having a UTF-8 `LC_CTYPE`, and the repository does not enforce it. Consider documenting it in deployment docs or checking `datctype` at startup.
2. (Informational) GEO-018 is proven only at the query level (`QIT:92-98`). The HTTP layer passes the query output through unchanged, so an end-to-end test would add little.
3. (Informational) GEO-019 has no test with a case-plus-accent pair (`uberlândia` / `Uberlandia`). The comparator path is the same as the tested one.
4. (Informational, carried over) GEO-006 is discriminated only at the service unit level (`ST:57-59`). M5 is killed only there, which is acceptable because the service owns the rule.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| GEO-001 … GEO-019 | Implementing | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 19/19 ACs matched the spec outcome | 0 open spec-precision gaps that block (2 informational notes)
**Sensor**: 14/14 mutations killed (11 in round 1 + 3 in round 2)
**Gate**: 235 passed, 0 failed, 0 skipped (`./mvnw -B verify`, BUILD SUCCESS)

**What works**: a single native aggregate query (proven by 1 prepared statement and 0 entity loads) groups by state and `lower(city)`. On a UTF-8 ctype database this folds non-ASCII capitals too (`SÃO CARLOS` = `São Carlos`), and each group shows the earliest-created spelling. States sort by UF. Cities sort by pt-BR Collator at PRIMARY strength, with plain character order breaking ties (`Uberlandia` before `Uberlândia`). State totals are summed, the empty result is `{"states": []}`, deletes and moves show up in the next call, and query parameters are ignored.

**Next steps**: the orchestrator commits this report as `.specs/features/customer-geo-grouping/validation.md`, runs `validate_state.py`, and updates spec traceability (GEO-001..019 to Verified).
