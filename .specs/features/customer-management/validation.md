# Customer Management Validation (re-verification, fix iteration 1)

**Verdict**: PASS

## Validation: customer-management - PASS ✅

**Date**: 2026-09-22
**Spec**: `.specs/features/customer-management/spec.md` at `d32d170` (53 ACs: CUST-01..CUST-53, including the three new assumption rows added in `d32d170`)
**Diff range**: `1810ee5..d32d170` (customer-management commits: the 14 task commits T1-T14 `1361ab7..e01ecd8`, and the fix commits T15-T18 `64ce512`, `4827972`, `34f5deb`, `d32d170`). The commits `fe42b2c`, `9d78ec7`, `6627480` and `3f95c98` in this range belong to customer-geo-grouping. That feature is verified separately. I checked them here only for side effects on CRUD (see "Cross-feature check").
**Previous round**: `1810ee5..e01ecd8`, FAIL (52/53; 31/32 mutants killed; M19 survived). Report: `validation-customer-management.md` in the same scratchpad.
**Verifier**: an independent sub-agent (the author is not the verifier). All work ran in a detached scratch worktree `.worktrees/verify-crud2` at `d32d170`. Nothing was committed. The implementer's worktree (`.worktrees/feat-customer-api`) and the main tree were not touched.

**Why PASS**: all 53 ACs have `file:line` evidence whose asserted outcome matches the spec as written at `d32d170`. M19 is now killed. M02 is now killed directly by `CpfValidatorTest`. All 10 round-2 mutations were killed. None of the tests were weakened.

**Condition**: the three new assumption rows are marked `Confirmed? n`, and `d32d170` also narrowed the wording of CUST-36 itself. This PASS is against that narrowed text. If the user rejects the narrowing, CUST-36 becomes an implementation gap: query-parameter and parse errors return no `errors` array (see SP-1).

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1-T14 | ✅ Done | Carried over from round 1 |
| T15 Assert full actuator exposure set | ✅ Done | `OPS:50-56`; kills M19, M36 and M37 |
| T16 CPF remainder-10 branch | ✅ Done | `CPF:17-24`; kills M02 directly |
| T17 Error contract + PII on remaining error paths | ✅ Done | `SUP:42-50` helper applied at `LS:138` and `UP:240`; `OPS:111-127` |
| T18 Spec clarifications | ✅ Done (pending user confirmation) | 3 assumption rows `Confirmed? n`; CUST-36 AC text narrowed |

---

## Gate Check

- **Gate command**: `./mvnw -B verify`
- **Result**: 240 tests run, 240 passed, 0 failures, 0 errors, 0 skipped. `BUILD SUCCESS`, exit 0.
- **Test count before feature** (`1810ee5`): 1
- **Round 1** (`e01ecd8`): 211
- **Now** (`d32d170`): 240. That is 211, plus 5 from the customer-management fixes (OPS +3: `actuatorDiscoveryListsOnlyHealth`, `validationErrorLogsNoPii`, `malformedJsonLogsNoPii`; CPF +2), plus 24 customer-geo-grouping tests (`CustomerLocationIntegrationTest` 11, `CustomerLocationServiceTest` 7, `CustomerLocationQueryIntegrationTest` 6).
- **Customer-management tests**: 216
- **Skipped**: none
- **Test integrity**: no test was deleted and no assertion was weakened. `ErrorContractIntegrationTest`'s private `assertProblem` moved unchanged into `HttpIntegrationTestSupport`. The list-400 and optimistic-lock-409 checks were replaced with `assertProblem`, which checks everything the old code checked plus `type`, `title`, `detail` and `instance`. The `LS` test still checks `$.content doesNotExist`.

Per class: CustomerRequestTest 65, CustomerCreateAndGetIntegrationTest 32, CustomerSortTest 21, CustomerServiceTest 16, CustomerUpdateIntegrationTest 16, CustomerListIntegrationTest 16, OperabilityIntegrationTest 11, CustomerLocationIntegrationTest 11, CpfValidatorTest 10, ErrorContractIntegrationTest 8, CustomerSpecificationsIntegrationTest 7, CustomerLocationServiceTest 7, CustomerRepositoryIntegrationTest 6, CustomerLocationQueryIntegrationTest 6, CustomerDeleteIntegrationTest 4, ClockConfigTest 3, CustomerApiSddApplicationTests 1.

---

## Cross-feature check (geo-grouping commits `fe42b2c..3f95c98`)

| Change | Effect on CRUD | Evidence |
| ------ | -------------- | -------- |
| `CustomerRepository.countByLocation()` native query added | Only additions to `src/main/java` (no deleted or modified line). The query is called only from `CustomerLocationService:40`. The CRUD derived queries are unchanged. | `git diff e01ecd8..d32d170 -- src/main/java` has no `-` lines; `grep countByLocation` |
| `GET /api/v1/customers/grouped-by-location` (literal path) | GET on that literal returns 200 `{"states":[]}` instead of the CUST-16 400 for a non-UUID id. PUT and DELETE on it still return 400 problem+json `Failed to convert 'id'`. Other non-UUID ids are unaffected (`EC:58`, `CG:240-243`). Intended by the geo spec. Recorded as SP-6. | Throwaway probe (discarded) |
| `src/test/resources/config/application.properties` (`hibernate.generate_statistics=true`) | Test-only. It does not change CRUD behaviour, and the whole suite passes. **It does shadow any future `src/main/resources/config/application.properties` during tests** (same classpath resource name, and `test-classes` comes first). See probe P1 and ranked gap 1. | P1 and its control, below |
| No geo commit touched a CRUD test class, `HttpIntegrationTestSupport`, `common/` or `application.properties` | None | `git log e01ecd8..d32d170 -- <those paths>` lists only `64ce512` and `34f5deb` |

---

## Spec-Anchored Acceptance Criteria

Test paths are relative to `src/test/java/com/example/customerapi/`. Abbreviations:
- `CG` = `customer/CustomerCreateAndGetIntegrationTest.java`
- `UP` = `customer/CustomerUpdateIntegrationTest.java`
- `DEL` = `customer/CustomerDeleteIntegrationTest.java`
- `LS` = `customer/CustomerListIntegrationTest.java`
- `EC` = `customer/ErrorContractIntegrationTest.java`
- `REQ` = `customer/CustomerRequestTest.java`
- `SVC` = `customer/CustomerServiceTest.java`
- `SORT` = `customer/CustomerSortTest.java`
- `SPEC` = `customer/CustomerSpecificationsIntegrationTest.java`
- `REPO` = `customer/CustomerRepositoryIntegrationTest.java`
- `CPF` = `customer/validation/CpfValidatorTest.java`
- `OPS` = `OperabilityIntegrationTest.java`
- `SUP` = `HttpIntegrationTestSupport.java`

`SUP:42-50` `assertProblem(result, status, path)` checks all of the following: `status().is(status)`, `contentType(APPLICATION_PROBLEM_JSON)`, `$.type`, `$.title` and `$.detail` are `not(emptyOrNullString())`, `$.status == status`, and `$.instance == path`.

Re-checked this round (evidence moved or changed): CUST-07, 24, 32, 33, 35, 36, 37, 38, 39, 43, 44, 45, 46, 53. Carried over after a spot-check: I checked 8 citations against the current source and all matched unchanged (`CG:36-57`, `CG:100-127`, `DEL:26-31`, `LS:64-69`, `SORT:23-45`, `SVC:141-142`, `UP:86-101`, `REQ:53,59`). `CG`, `DEL`, `REQ`, `SVC`, `SORT`, `SPEC` and `REPO` did not change after `e01ecd8`. In `LS` and `UP`, only the lines noted below moved.

| AC | Evidence `file:line` + assertion | Spec-defined outcome | Covered? |
| -- | -------------------------------- | -------------------- | -------- |
| CUST-01 | `CG:36` `status().isCreated()`; `CG:37-43` `$.name "Ana Souza"`, `$.email`, `$.cpf`, `$.phone`, `$.birthDate`, `$.city "São Paulo"`, `$.state "SP"`; `CG:47` `UUID.fromString($.id)`; `CG:48-51` `Instant.parse(createdAt/updatedAt)`, `updatedAt.isEqualTo(createdAt)`; `CG:53-57` stored row `containsExactly(...)`; `SVC:78-82` | 201; body with UUID id and all fields; persisted | ✅ PASS |
| CUST-02 | `CG:50` `getHeader("Location")).isEqualTo("/api/v1/customers/" + id)` | `Location` = `/api/v1/customers/{id}` | ✅ PASS |
| CUST-03 | `CG:75` `$.email "ana@example.com"` for `" Ana@Example.COM "`; `CG:84-85` stored; `REQ:53` | stored and returned `ana@example.com` | ✅ PASS |
| CUST-04 | `CG:76` `$.cpf "52998224725"` for `529.982.247-25`; `CG:85`; `REQ:59` | stored and returned `52998224725` | ✅ PASS |
| CUST-05 | `CG:94-97` cases, `CG:123-127` `isBadRequest()`, `errors[*].field contains(field)`, `count()).isZero()`; boundaries `CG:134-139`; `REQ:98-123` | 400 + `errors` entry `name`; 2/120 accepted | ✅ PASS |
| CUST-06 | `CG:98-101` (missing, `ana.example.com`, 255 chars), `CG:123-127`; `REQ:130-151` (254 ok, 255 invalid) | 400 + `errors` entry `email`; "valid" = Hibernate `@Email` (new assumption row, `n`) | ✅ PASS (see SP-1) |
| CUST-07 | `CG:102-105` (missing, 10 digits, `111.111.111-11`, wrong check digit), `CG:123-127`; `CPF:33,38` wrong 1st/2nd digit `isFalse()`; `CPF:43,48,53` 10/12 digits and letters; `CPF:58` all equal; **`CPF:18` `isValid("10000000108")).isTrue()` and `CPF:23` `isValid("10000002810")).isTrue()`** (remainder-10 → 0 for each check digit; I checked both by hand); `REQ:158-173` | 400 + `errors` entry `cpf`; check-digit calculation correct | ✅ PASS (moved +10 lines; M02 now killed directly) |
| CUST-08 | `CG:106-107`, `CG:123-127`; `REQ:180-200` | 400 + `errors` entry `phone` | ✅ PASS |
| CUST-09 | `CG:108-109` (today UTC, tomorrow), `CG:123-127`; yesterday accepted `CG:134-137`; `REQ:207-217` fixed UTC clock | 400 unless strictly before today's UTC date | ✅ PASS |
| CUST-10 | `CG:148-152` `isConflict()`, problem+json, `errors[*].field contains("email")`, `count() == 1` | 409 + `email`; nothing created | ✅ PASS |
| CUST-11 | `CG:159-163` `isConflict()`, `contains("cpf")`, `count() == 1` | 409 + `cpf`; nothing created | ✅ PASS |
| CUST-12 | `CG:169-174`, `CG:180-185` (pre-check stubbed false, so the DB constraint fires) `isConflict()`, field, `count() == 1`; `REPO:54-58,67-71` | 409, not 500 | ✅ PASS |
| CUST-13 | `CG:197-207` `$.id not(clientId)`, `$.createdAt/$.updatedAt not(clientInstant)`, `existsById(clientId)).isFalse()`; `UP:136-138` PUT keeps `$.id`, `$.createdAt` | client id/createdAt/updatedAt ignored | ✅ PASS (PUT `updatedAt` is not asserted; structurally impossible because `CustomerRequest` has no such field; confirmed by a round-1 probe) |
| CUST-14 | `CG:220-227` `isOk()`, `readTree(fetched)).isEqualTo(readTree(created))` | 200 with that customer | ✅ PASS |
| CUST-15 | `CG:232-235` `isNotFound()`, `$.status 404`; `EC:44` `assertProblem(404)` | 404 | ✅ PASS |
| CUST-16 | `CG:240-243` `isBadRequest()` for `not-a-uuid`; `EC:58` `assertProblem(400, "/api/v1/customers/123")` | 400 | ✅ PASS (see SP-6 for the geo literal path) |
| CUST-17 | `UP:86-95` `isOk()`, `$.id`, `$.name "Ana Lima"`, `$.email`, `$.cpf`, `$.phone`, `$.birthDate`, `$.city "Rio de Janeiro"`, `$.state "RJ"`; `UP:103-110` re-read | all 7 fields replaced; 200 | ✅ PASS |
| CUST-18 | `UP:119-125` `$.phone/$.birthDate doesNotExist`, stored `isNull()`; `SVC:154-157` | omitted optional fields stored null | ✅ PASS |
| CUST-19 | `UP:87` `$.id`; `UP:95,111` `$.createdAt == createdAt`; `UP:101` `updatedAt.isAfter(createdAt)`; `SVC:141-142` `updatedAt == NOW` | id/createdAt unchanged; updatedAt = update time | ✅ PASS |
| CUST-20 | `UP:145-163` one invalid case per rule, `isBadRequest()`, `errors[*].field contains(field)` (`UP:160`), `assertAnaUnchanged()` (`UP:77` full JSON equality) | 400 + entries; unchanged | ✅ PASS |
| CUST-21 | `UP:180-184`, `UP:191-195` `isConflict()`, field, `assertAnaUnchanged()`; `SVC:161-182` | 409 + entry; unchanged | ✅ PASS |
| CUST-22 | `UP:200-203` `isOk()`, own email/cpf; `SVC:188-195` | no conflict | ✅ PASS |
| CUST-23 | `UP:212-217` `isNotFound()`, `existsById(unknown)).isFalse()`, `count() == 1` | 404; nothing created | ✅ PASS |
| CUST-24 | `UP:226-238` REQUIRES_NEW commit between read and write; **`UP:240` `assertProblem(..., 409, "/api/v1/customers/" + anaId)`**; `UP:242-247` stored `name "First Writer"`, `city "Curitiba"`, `state "PR"` | 409; first committer's data kept | ✅ PASS (moved; now checks the full problem contract) |
| CUST-25 | `DEL:26-31` `isNoContent()`, `content().string("")`, `existsById(id)).isFalse()` | 204, empty, removed | ✅ PASS |
| CUST-26 | `DEL:41-43` `isNotFound()` | 404 | ✅ PASS |
| CUST-27 | `DEL:53-56` `isCreated()`, `count() == 1` | email and cpf reusable | ✅ PASS |
| CUST-28 | `DEL:65-70` `isNotFound()`, `$.status 404`, other row remains | 404 | ✅ PASS |
| CUST-29 | `LS:64-69` `isOk()`, `content[*].name contains(names(1,20))`, `page.number 0`, `size 20`, `totalElements 25`, `totalPages 2` | first 20 by name asc; envelope | ✅ PASS |
| CUST-30 | `LS:78-83`, `LS:90-94` | that zero-based page/size | ✅ PASS |
| CUST-31 | `LS:113-115` `sort=email,desc` against PostgreSQL; `SPEC:80-83`; `SORT:23-27` all 8 pairs `Sort.by(expected, property).and(id asc)` | ordered by property and direction | ✅ PASS (createdAt/updatedAt are asserted only on the `Sort` object; DB order confirmed by a round-1 probe) |
| CUST-32 | `LS:132` `page=-1`, `size=0`, `size=101` → **`LS:138` `assertProblem(result, 400, CUSTOMERS)`**, `LS:139` `$.content doesNotExist`; `LS:101-104` `size=100` accepted | 400 out of range; 100 accepted | ✅ PASS |
| CUST-33 | `LS:132` `sort=cpf,asc`, `sort=cpf` → `LS:138`; `SORT:38-40` → `InvalidSortException` | 400 | ✅ PASS |
| CUST-34 | `LS:148-150` `isOk()`, `$.content empty()`, `totalElements 0` | 200, empty, 0 | ✅ PASS |
| CUST-35 | `SUP:42-50` (all 5 fields + content type), applied to: 400 body validation `EC:28`; 404 `EC:44`; 409 duplicate `EC:51`; 400 non-UUID `EC:58`; 400 malformed JSON `EC:66`; 415 `EC:75`; 500 `EC:86`; **400 list page/size/sort (6 rows) `LS:138`**; **409 optimistic lock `UP:240`** | every 4xx/5xx is problem+json with type, title, status, detail, instance | ✅ PASS (representative set now includes every custom handler and the framework 400/415 paths; see SP-2) |
| CUST-36 | `EC:33-37` `$.errors hasSize(2)`, `errors[*].field containsInAnyOrder("name","city")`, `errors[0/1].message isNotEmpty()`; `CG:125` `errors[*].field contains(field)` (Hamcrest `contains` = exactly that one entry) for each body rule of CUST-05..09, 48, 49; `UP:160` same for PUT | (narrowed text) body validation → one `{field, message}` per invalid field | ✅ PASS against the narrowed AC (see SP-1) |
| CUST-37 | `EC:62-67` malformed body, `assertProblem(result, 400, CUSTOMERS)`, `count()).isZero()` | 400 | ✅ PASS |
| CUST-38 | `EC:71-76` POST `text/plain`, `assertProblem(415)`, `count()).isZero()`; `UP:166-171` PUT `text/plain`, `isUnsupportedMediaType()`, problem+json, unchanged | 415 for POST and PUT | ✅ PASS |
| CUST-39 | `EC:79-92` `assertProblem(500)`, `$.detail "An unexpected error occurred."`, `$.trace/$.exception/$.message doesNotExist`, body `doesNotContain("secret-internal-failure","IllegalStateException","at com.example")` | 500, fixed detail, no message/stack | ✅ PASS |
| CUST-40 | `LS:159-161` `name=ANA` → `contains("Ana Souza","Mariana Lima")`, `totalElements 2`; `SPEC:39,44,49-52` | contains, case-insensitive | ✅ PASS |
| CUST-41 | `LS:168-170` `email=" ANA@X.COM "` → `contains("Ana Souza")`, `totalElements 1`; `SPEC:57` | trimmed, case-insensitive equality | ✅ PASS |
| CUST-42 | `LS:177-181`; `SPEC:62-63` | AND | ✅ PASS |
| CUST-43 | `OPS:39` `isOk()`, `jsonPath("$.status").value("UP")` | 200, UP | ✅ PASS |
| CUST-44 | **`OPS:50-56` `GET /actuator` `isOk()`, `links.keySet()).containsExactlyInAnyOrder("self","health","health-path")`**; `OPS:44-47` `/actuator/env`, `/actuator/beans` → `isNotFound()` | no actuator endpoint other than `health` over HTTP; discovery root lists only self/health/health-path (new assumption row, `n`) | ✅ PASS (was ❌; kills M19, M36 and M37; see ranked gap 1 for a latent harness blind spot) |
| CUST-45 | `OPS:65,76,87` + `OPS:129-133` exactly 1 line containing `"Customer created/updated/deleted id=" + id`, `contains(" INFO ")`, `contains("customer.CustomerService")`; `SVC:270-275` | one INFO line per create/update/delete with operation and id | ✅ PASS (moved +11) |
| CUST-46 | `OPS:135-140` `output.getAll()).doesNotContain(email, cpf, cpfDigits, phone, phoneDigits)`, applied to: create `OPS:66`, update `OPS:77-78`, delete `OPS:88`, DB-race email `OPS:98`, DB-race cpf `OPS:108`, **invalid-body 400 `OPS:115`**, **malformed-JSON 400 `OPS:126`**; `SVC:280-281` | email, cpf and phone never in logs | ✅ PASS (see SP-3) |
| CUST-47 | `REPO:38-46` `flyway_schema_history` v1 success, `customer` table exists on a fresh Testcontainers DB; `ddl-auto=validate` (`src/main/resources/application.properties:9`) | versioned migrations, no manual step | ✅ PASS |
| CUST-48 | `CG:110-112` → 400 `city`; `REQ:228-254`; `UP:151` | 400 + `errors` entry `city` | ✅ PASS |
| CUST-49 | `CG:113-114` → 400 `state`; `REQ:260-275` all 27 UFs in any case | 400 + `errors` entry `state` | ✅ PASS |
| CUST-50 | `CG:77` `$.state "SP"` for `sp`; `CG:85`; `REQ:65` | `SP` | ✅ PASS |
| CUST-51 | `CG:78` `$.city "São Paulo"` for `"  São   Paulo "`; `CG:85`; `REQ:71` | `São Paulo` | ✅ PASS |
| CUST-52 | `LS:124-126` `sort=email` → `c01..c05` against PostgreSQL; `SORT:31-34` | ascending | ✅ PASS |
| CUST-53 | `LS:132` `sort=name,up` → `LS:138` `assertProblem(400)`; `SORT:44-45` `InvalidSortException` | 400 | ✅ PASS |

**Status**: ✅ 53/53 ACs have evidence matching the spec outcome at `d32d170`. 6 spec-precision gaps remain; none blocks.

### Edge cases (spec.md)

- [x] Email differing only by case/spaces is a duplicate: `CG:148-151`
- [x] `111.111.111-11` → 400: `CG:104`, `CPF:58`
- [x] Name exactly 2 / 120 accepted: `CG:134-139`, `REQ:107,117`
- [x] birthDate yesterday (UTC) accepted: `CG:134-137`, `REQ:207`
- [x] `size=100` accepted: `LS:101-104`

---

## Discrimination Sensor

**Method**: manual behavior-level mutations in the scratch worktree `.worktrees/verify-crud2` (no pitest). I ran each mutant against the relevant test class(es) with `./mvnw -B test -Dtest=...`, then discarded it with `git checkout -- . && git clean -fdq src`. `git status --porcelain` was empty after every one. I also checked that the compiler removed stale mutant `.class` files from `target/` (`common/config/` holds only `ClockConfig.class`). Logs and diffs are in `scratchpad/mut2/`.

### Round 1 (`e01ecd8`, previous verifier; carried over)

| # | Mutation | Location | Result |
| - | -------- | -------- | ------ |
| M01 | Email not lowercased | `CustomerRequest.java:32` | ✅ Killed |
| M02 | CPF remainder-10 → 0 rule dropped | `CpfValidator.java:25` | ✅ Killed, but only incidentally (seeded helper CPF) → **re-run in round 2** |
| M03 | Second check digit not verified | `CpfValidator.java:16` | ✅ Killed |
| M04 | All-equal-digits rule removed | `CpfValidator.java:13` | ✅ Killed |
| M05 | Update email check without self-exclusion | `CustomerService.java:56` | ✅ Killed |
| M06 | Update cpf check without self-exclusion | `CustomerService.java:59` | ✅ Killed |
| M07 | `Location` header dropped | `CustomerController.java:42` | ✅ Killed |
| M08 | `id` tiebreaker dropped (explicit sort) | `CustomerSort.java:34` | ✅ Killed |
| M09 | `id` tiebreaker dropped (default sort) | `CustomerSort.java:22` | ✅ Killed |
| M10 | `sort=<property>` defaults to desc | `CustomerSort.java:28` | ✅ Killed |
| M11 | Optimistic-lock handler removed (→ 500) | `ApiExceptionHandler.java:85` | ✅ Killed |
| M12 | `logServerErrorDetail=false` removed | `application.properties:7` | ✅ Killed |
| M13 | Create log line includes email | `CustomerService.java:38` | ✅ Killed |
| M14 | Name max 120 → 121 | `CustomerRequest.java:19` | ✅ Killed |
| M15 | Name min 2 → 3 | `CustomerRequest.java:19` | ✅ Killed |
| M16 | City min 2 → 1 | `CustomerRequest.java:24` | ✅ Killed |
| M17 | `size` max 100 → 99 | `CustomerController.java:48` | ✅ Killed |
| M18 | `size` min 1 → 0 | `CustomerController.java:48` | ✅ Killed |
| **M19** | **Actuator `include=health,info`** | `application.properties:12` | ❌ Survived → **re-run in round 2** |
| M20 | Unique-constraint → field mapping swapped | `ApiExceptionHandler.java:42` | ✅ Killed |
| M21 | Unique-constraint race → 500 | `ApiExceptionHandler.java:81` | ✅ Killed |
| M22 | `@Past` → `@PastOrPresent` | `CustomerRequest.java:23` | ✅ Killed |
| M23 | City inner whitespace not collapsed | `CustomerRequest.java:34` | ✅ Killed |
| M24 | Name filter case-sensitive | `CustomerSpecifications.java:27` | ✅ Killed |
| M25 | Email filter not trimmed | `CustomerSpecifications.java:30` | ✅ Killed |
| M26 | Filters combined with OR | `CustomerSpecifications.java:32` | ✅ Killed |
| M27 | Update does not advance `updatedAt` | `Customer.java:60` | ✅ Killed |
| M28 | 500 `detail` = exception message | `ApiExceptionHandler.java:99` | ✅ Killed |
| M29 | Delete does not remove the row | `CustomerService.java:69` | ✅ Killed |
| M30 | PUT keeps old phone/birthDate (PATCH semantics) | `CustomerService.java:62` | ✅ Killed |
| M31 | Duplicate/renamed `errors` entries | `ApiExceptionHandler.java:53` | ✅ Killed |
| M32 | State not uppercased | `CustomerRequest.java:35` | ✅ Killed |

Round 1 result: 31/32 killed. I did not re-run M01, M03-M18 or M20-M32. Their production code has not changed since `e01ecd8` (the geo commits only add lines, all outside those files). The tests that killed them were either unchanged or strengthened. Line numbers are unchanged: `ApiExceptionHandler`, `CustomerService`, `CustomerRequest`, `CpfValidator` and `application.properties` have no diff in `e01ecd8..d32d170`.

### Round 2 (`d32d170`, this verifier)

| # | Mutation | Location | Tests run | Result (killing test `file:line`) |
| - | -------- | -------- | --------- | --------------------------------- |
| M19 (re-run) | `include=health` → `include=health,info` | `src/main/resources/application.properties:12` | OperabilityIntegrationTest | ✅ Killed: `actuatorDiscoveryListsOnlyHealth` (`OPS:56`) |
| M02 (re-run) | `return remainder == 10 ? 0 : remainder` → `return remainder` | `customer/validation/CpfValidator.java:25` | **CpfValidatorTest only** | ✅ Killed directly: `acceptsCpfWhoseFirstCheckDigitComesFromRemainder10` (`CPF:18`), `acceptsCpfWhoseSecondCheckDigitComesFromRemainder10` (`CPF:23`) |
| M33 | Optimistic-lock 409 built as a hand-made problem+json map **without `instance`** | `common/web/ApiExceptionHandler.java:86-88` | CustomerUpdateIntegrationTest | ✅ Killed: `concurrentUpdate...` (`UP:240` → `SUP:49` "No value at JSON path $.instance"). This would have survived round 1's test. |
| M34 | Optimistic-lock handler returns **500** instead of 409 | `ApiExceptionHandler.java:87` | CustomerUpdateIntegrationTest | ✅ Killed: `UP:240` → `SUP:43` "expected:<409> but was:<500>" |
| M35 | `log.warn("Request validation failed for {}", bindingResult.getTarget())` (logs the request body on validation failure) | `ApiExceptionHandler.java:51` | OperabilityIntegrationTest | ✅ Killed: `validationErrorLogsNoPii` (`OPS:115` → `OPS:138`). This would have survived round 1's tests. |
| M36 | Different property path: `include=*` + `exclude=env,beans` (exposes info, metrics, loggers, mappings… but keeps the two probed paths 404) | `application.properties:12-13` | OperabilityIntegrationTest | ✅ Killed: `actuatorDiscoveryListsOnlyHealth` (`OPS:56`). The old `/env`,`/beans` probes passed. |
| M37 | `include=*` | `application.properties:12` | OperabilityIntegrationTest | ✅ Killed: `OPS:47` ×2, `OPS:56` |
| M38 | Invalid-sort 400 returned directly (`ResponseEntity.badRequest().body(ProblemDetail...)`), bypassing `createResponseEntity`, so `type` is lost | `ApiExceptionHandler.java:91-94` | CustomerListIntegrationTest | ✅ Killed: `outOfRangePaging...[sort=cpf,asc / cpf / name,up]` (`LS:138` → `SUP:45` "No value at JSON path $.type"). This would have survived round 1's list test. |
| M39 | New `CommonsRequestLoggingFilter` bean with `includePayload=true` + its logger at DEBUG | new `common/config/RequestLoggingConfig.java`, `application.properties` | OperabilityIntegrationTest | ✅ Killed: 7 PII tests incl. `malformedJsonLogsNoPii` (`OPS:126`) and `validationErrorLogsNoPii` (`OPS:115`) |
| M40 | New filter that logs the cached request body **only when the response is 400** | new `common/config/BadRequestLoggingFilter.java` | OperabilityIntegrationTest, then single methods | ✅ Killed. Run in isolation: `malformedJsonLogsNoPii` alone fails (`OPS:126`), and `validationErrorLogsNoPii` alone fails (`OPS:115`). The control `createLogsOneInfoLineWithIdAndNoPii` alone passes, because that path is not exercised. So each new CUST-46 test kills it independently. |

**Round 2 result**: 10/10 killed, 0 survived.
**Cumulative**: 40 distinct mutants (32 + 8 new). All 40 were killed at their latest run: M19 and M02 re-run in round 2, the other 30 carried over from round 1 as justified above. **0 survive.**
**Sensor depth**: expanded (critical-path data-integrity API, ≥5 manual mutations per risk area).

### Probe P1 (not counted as a mutant; latent harness blind spot)

| Probe | Result |
| ----- | ------ |
| P1: add `src/main/resources/config/application.properties` containing `management.endpoints.web.exposure.include=health,info` (a production-effective config location with higher precedence than `classpath:application.properties`) | ⚠️ **Undetected**: OperabilityIntegrationTest passes (exit 0). The test-only `src/test/resources/config/application.properties` (added by geo commit `6627480`) has the same classpath name. `target/test-classes` comes first on the test classpath, so Spring Boot loads the test file and never sees the main one. |
| P1 control: the same main file, with the test `config/application.properties` temporarily removed (including the stale copy in `target/test-classes`) | ✅ Detected: `actuatorDiscoveryListsOnlyHealth` (`OPS:56`) fails |

Why P1 is not counted as a surviving mutant: it does not mutate customer-management code or config. It adds a config source that does not exist in the product. The control shows that the CUST-44 assertion catches that exact exposure when the file is visible, so the tests discriminate correctly. The blind spot comes from the test harness, and the cause is the geo-grouping test resource. The file does not exist at `d32d170`, so production behaviour today is correct. It is ranked as gap 1 (non-blocking) so the orchestrator can decide. If you prefer the strict reading ("any undetected production-config change is a surviving mutant"), this becomes a FAIL until the test resource is renamed.

---

## Exploratory Probes (scratch-only, discarded; informational)

| Request | Observed |
| ------- | -------- |
| `GET /actuator` | 200 `_links`: self, health, health-path (matches the new assumption row) |
| `GET /actuator/info` | 404 problem+json, all 5 fields |
| `GET /api/v1/customers/grouped-by-location` | 200 `{"states":[]}` (geo feature literal route) |
| `PUT` / `DELETE /api/v1/customers/grouped-by-location` | 400 problem+json `Failed to convert 'id' with value: 'grouped-by-location'` (CUST-16 behaviour kept for non-GET methods) |
| `GET ...?size=abc` | 400 problem+json, all 5 fields, no `errors` (matches the new CUST-36 scope row) |
| `GET ...?page=-1` | 400 problem+json, `detail "Validation failure"`, no `errors` (matches the new CUST-36 scope row) |

---

## Spec-Precision Gaps (remaining)

- **SP-1 (unconfirmed clarifications; CUST-06, CUST-36, CUST-44)**: `d32d170` adds three assumption rows, all marked `Confirmed? n`, and **rewrites the CUST-36 AC** from "WHEN request validation fails" to "WHEN request body validation fails (CUST-05 to CUST-09, CUST-48, CUST-49)". The implementer narrowed the AC to match existing behaviour. The narrowing is reasonable and documented, but it needs the user's confirmation. If rejected, `page/size` and unparseable-input 400s must gain an `errors` array, and CUST-36 becomes an implementation gap. The `@Email` row (`user@host` accepted) and the discovery-root row need the same confirmation.
- **SP-2 (CUST-35 universality)**: "every 4xx and 5xx" is universal. Tests now cover every custom handler plus framework 400 (malformed, non-UUID, page/size), 415 and 500. They do not cover 405 (a round-1 probe saw a full problem+json) or a 404 for an unmapped route (a probe this round saw a full problem+json for `/actuator/info`). This is sampling, not a defect.
- **SP-3 (CUST-46 universality)**: "never" is universal. Tests cover create, update, delete, both DB-race paths, the invalid-body 400 and the malformed-JSON 400. Not covered: PUT validation 400 and PUT conflict paths. They share the handlers that M35, M39 and M40 exercise, so the risk is low. Also, `CapturedOutput.getAll()` includes the class-level capture from `@ExtendWith(OutputCaptureExtension)`, so one leak cascades into every later test in the class. Kills stay valid, but the failing test does not always point at the leaking path. M40 shows this: the class run failed 7 tests, and single-method runs isolate it.
- **SP-4 (CUST-13, CUST-31, carried)**: PUT ignoring a body `updatedAt` is not asserted, and it cannot occur because the record has no such field. DB ordering by `createdAt`/`updatedAt` is asserted only on the `Sort` object. Both were confirmed by a round-1 probe.
- **SP-5 (CUST-44 wording)**: the AC still says "no actuator endpoint other than `health`", while the new assumption row allows `GET /actuator`. That is consistent once confirmed, but the AC text itself could reference the assumption.
- **SP-6 (CUST-16 vs geo route)**: `GET /api/v1/customers/grouped-by-location` returns 200, although `grouped-by-location` is not a UUID. The geo spec intends this. CUST-16 could say "except reserved literal sub-resources" to keep the two specs consistent.

---

## Ranked Gaps

None blocks PASS.

1. **[Latent, harness, owner: customer-geo-grouping] Test resource `src/test/resources/config/application.properties` shadows `classpath:config/application.properties` from main (probe P1).** Any future production setting put in `src/main/resources/config/application.properties` is invisible to every test. That includes actuator exposure (CUST-44), `logServerErrorDetail` (CUST-46) and `ddl-auto` (CUST-47). **Fix**: move the statistics flag to a test-only mechanism that does not reuse a production resource name, for example `src/test/resources/application-test.properties` with `@ActiveProfiles("test")` on the geo tests, `@TestPropertySource` on the geo test classes, or `spring.config.import` from a uniquely named file. Done when P1 fails `OperabilityIntegrationTest`. Priority: Minor (no such main file exists today).
2. **[Spec decision] SP-1: confirm the three `n` assumption rows and the narrowed CUST-36 text with the user.** If CUST-36 is not accepted as narrowed, reopen as an implementation task (`errors` for query-parameter and parse failures). Priority: Major for contract clarity, but does not block this verdict.
3. **[Hardening] SP-3: make CUST-46 log assertions per-test**, for example by snapshotting `output.getAll().length()` before the request and asserting only on the tail, and add PUT-400/PUT-409 PII checks. Priority: Minor.
4. **[Hardening] SP-2/SP-4: add a 405 problem+json assertion; assert DB ordering for `createdAt`/`updatedAt`.** Priority: Minor.
5. **[Spec wording] SP-5/SP-6: align the CUST-44 AC text with the discovery-root assumption, and let CUST-16 acknowledge the geo literal route.** Priority: Cosmetic.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / no scope creep | ✅ Fix commits T15-T17 are test-only; T18 is spec-only |
| Surgical changes | ✅ `assertProblem` was moved, not duplicated |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ✅ 53/53 |
| Per-layer coverage | ✅ |
| Every test maps to an AC, edge case or Done-when | ✅ New tests map to CUST-07, CUST-35, CUST-44 and CUST-46 |
| Documented guidelines | none found; strong defaults applied |

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| CUST-01..CUST-43, CUST-45..CUST-53 | ✅ Verified (round 1) | ✅ Verified |
| CUST-44 | ❌ Needs Fix | ✅ Verified |
| CUST-06, CUST-36, CUST-44 assumptions | - | ⚠️ Awaiting user confirmation (`n`) |

---

## Isolation

- Scratch worktree: `.worktrees/verify-crud2` (detached at `d32d170`). `git status --porcelain` was empty before the sensor, after every mutation and after the probes. The worktree is removed at the end.
- `.worktrees/feat-customer-api` (HEAD `2f33d66`) and the main tree were never modified. Nothing was stashed, committed or pushed.

---

## Summary

**Overall**: ✅ Ready (pending user confirmation of the three `n` assumption rows)

**Spec-anchored check**: 53/53 ACs matched the spec outcome | 6 spec-precision gaps (non-blocking)
**Sensor**: round 2 10/10 killed; cumulative 40 distinct mutants, 40 killed, 0 survived (+1 uncounted harness probe P1)
**Gate**: 240 passed, 0 failed, 0 skipped (216 customer-management + 24 geo)

---

## Addendum: post-verification change (orchestrator, 2026-09-22)

Commit `81e6bca` (task T19) closes ranked gap 1 (probe P1). It deletes `src/test/resources/config/application.properties` and enables Hibernate statistics through a `HibernatePropertiesCustomizer` bean in `src/test/java/com/example/customerapi/TestcontainersConfiguration.java:22`. No file under `src/main` changed.

Evidence after the change:

- `./mvnw -B clean verify` gave 240 passed, 0 failed, 0 skipped. The GEO-016/017 statistics test still passes, so statistics are on.
- Probe P1 was re-run: `management.endpoints.web.exposure.include=health,info` placed in `src/main/resources/config/application.properties` fails `OperabilityIntegrationTest.java:56` after a clean build.
- A non-clean build still showed P1 surviving, because the deleted resource was still present in `target/test-classes/config/`. Maven does not remove stale resources, so run `clean` after deleting resources.

The verdict above stands at `81e6bca`.
