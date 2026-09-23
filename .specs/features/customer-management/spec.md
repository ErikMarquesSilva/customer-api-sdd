# Customer Management API Specification

## Problem Statement

The business has no system of record for customers. Client applications need a single HTTP API to create, read, update, delete and search customer records backed by PostgreSQL, with strict validation so the data stays clean (unique email and CPF, valid CPF check digits) and a predictable error contract that clients can handle programmatically.

## Goals

- [ ] Clients can perform full CRUD on customers over REST under `/api/v1/customers`, with every behavior below covered by an automated test.
- [ ] No two customers can share an email (case-insensitive) or a CPF, including under concurrent requests.
- [ ] Every error response is a machine-readable RFC 9457 Problem Details document.
- [ ] The service starts against an empty PostgreSQL database and creates its schema without manual steps.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
| ------- | ------ |
| Authentication / authorization | Assumed to be enforced by an API gateway in front of the service (see Assumptions). Separate feature. |
| Soft delete, restore, audit history | Decided: hard delete. |
| Partial update (PATCH) | Decided: PUT only. |
| Full address (street, number, zip code), multiple emails/phones | Decided: core fields + CPF, plus city and state only. |
| Rate limiting | Gateway concern; separate feature. |
| HTTP caching / ETag / If-Match | Not requested; concurrency handled by optimistic locking inside the service. |
| Bulk create/update/delete, import/export | Not requested. |
| OpenAPI/Swagger documentation UI | Not requested; candidate follow-up feature. |
| Metrics and distributed tracing | Only health and logging are in scope. |

---

## Assumptions & Open Questions

Every ambiguity is resolved or recorded here - nothing is left silently unclear.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Customer fields | name, email, cpf (required); phone, birthDate (optional); id, createdAt, updatedAt (server-managed) | User decision | y |
| Location fields (amendment for customer-geo-grouping) | `city` and `state` required on create and update | Every customer must be groupable by location; no legacy data exists, so no migration cost | y |
| City format | Trimmed, internal whitespace runs collapsed to one space, 2 to 100 characters; case and accents kept as sent | Stable grouping key without altering how the name is written | y |
| State format | One of the 27 Brazilian UFs (AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO), accepted in any case, stored uppercase | Two-letter UF as requested; rejects non-existent states | y |
| Delete semantics | Hard delete; email and CPF become reusable | User decision | y |
| Update operations | PUT full replacement only; omitted optional fields become null | User decision | y |
| Listing | Paginated with sort, optional `name` (contains, case-insensitive) and `email` (exact, case-insensitive) filters | User decision | y |
| Base path and versioning | `/api/v1/customers` | URI versioning is the simplest contract to evolve | y |
| Identifier type | Server-generated UUID | Not enumerable; no coupling to DB sequences | y |
| Authentication | None in the service; a gateway enforces it | Not requested; keeps the feature bounded | y |
| Error format | RFC 9457 `application/problem+json`; validation and conflict errors add an `errors` array of `{field, message}` | Standard, supported natively by Spring | y |
| Email normalization | Trimmed and lowercased before validation, storage and uniqueness checks | Emails are case-insensitive in practice; prevents duplicates by casing | y |
| CPF input format | Accept `52998224725` or `529.982.247-25`; store and return 11 digits | Clients send both forms; one canonical stored form | y |
| CPF mutability | CPF may be changed via PUT, still subject to validation and uniqueness | No requirement to lock it; simplest consistent rule | y |
| Name rules | Trimmed; 2 to 120 characters | Rejects blanks and junk while allowing full names | y |
| Phone format | Optional; when present must match `^\+?\d{10,13}$` (no spaces or punctuation) | Covers BR landline/mobile with or without country code; unambiguous | y |
| birthDate rule | Optional ISO date; must be strictly before the current UTC date | A future or today birth date is a data error | y |
| Page size bounds | Default 20; `size` 1-100; `page` >= 0; out of range returns 400 | Explicit errors over silent clamping | y |
| Default and allowed sort | Default `name,asc`; allowed properties: name, email, createdAt, updatedAt | Stable, user-meaningful ordering; blocks sorting on arbitrary columns | y |
| Concurrent updates | Optimistic locking; the losing concurrent write returns 409; no client-visible version field | Prevents silent lost updates without adding ETag scope | y |
| Unknown JSON properties | Ignored; client-supplied id, createdAt, updatedAt are ignored | Tolerant reader; server owns identity and timestamps | y |
| Timestamps | `createdAt`/`updatedAt` as ISO-8601 UTC instants | Unambiguous across time zones | y |
| Observability | Actuator `health` only; INFO log per create/update/delete with id; no PII in logs | "Production-ready" without scope creep into metrics | y |
| Schema management | Versioned migrations applied on startup; no Hibernate auto-DDL | Repeatable, reviewable schema changes | y |

**Open questions:** none - all resolved or logged above (required before the spec is confirmed).

---

## User Stories

### P1: Create a customer ⭐ MVP

**User Story**: As a client application, I want to register a customer so that it becomes part of the system of record.

**Why P1**: Nothing else works without customers.

**Acceptance Criteria** (each line is one EARS pattern):

1. WHEN a client sends `POST /api/v1/customers` with a valid body THEN the system SHALL persist the customer and respond 201 with the customer (id as UUID, name, email, cpf, phone, birthDate, city, state, createdAt, updatedAt). `CUST-01`
2. WHEN a customer is created THEN the system SHALL return a `Location` header equal to `/api/v1/customers/{id}`. `CUST-02`
3. WHEN a customer is created with email ` Ana@Example.COM ` THEN the system SHALL store and return `ana@example.com`. `CUST-03`
4. WHEN a customer is created with cpf `529.982.247-25` THEN the system SHALL store and return `52998224725`. `CUST-04`
5. IF `name` is missing, blank, or its trimmed length is outside 2-120 THEN the system SHALL respond 400 with an `errors` entry for field `name`. `CUST-05`
6. IF `email` is missing, not a valid address, or longer than 254 characters THEN the system SHALL respond 400 with an `errors` entry for field `email`. `CUST-06`
7. IF `cpf` is missing, is not 11 digits after removing `.` and `-`, has all digits equal, or fails the check-digit calculation THEN the system SHALL respond 400 with an `errors` entry for field `cpf`. `CUST-07`
8. IF `phone` is present and does not match `^\+?\d{10,13}$` THEN the system SHALL respond 400 with an `errors` entry for field `phone`. `CUST-08`
9. IF `birthDate` is present and is not strictly before the current UTC date THEN the system SHALL respond 400 with an `errors` entry for field `birthDate`. `CUST-09`
10. IF the normalized email already belongs to another customer THEN the system SHALL respond 409 with an `errors` entry for field `email` and SHALL NOT create a customer. `CUST-10`
11. IF the normalized cpf already belongs to another customer THEN the system SHALL respond 409 with an `errors` entry for field `cpf` and SHALL NOT create a customer. `CUST-11`
12. IF the database rejects a write because of the email or cpf unique constraint (concurrent requests) THEN the system SHALL respond 409 instead of 500. `CUST-12`
13. The system SHALL ignore `id`, `createdAt` and `updatedAt` supplied in any request body. `CUST-13`
14. IF `city` is missing, blank, or its normalized length is outside 2-100 THEN the system SHALL respond 400 with an `errors` entry for field `city`. `CUST-48`
15. IF `state` is missing or is not one of the 27 Brazilian UFs (compared ignoring case) THEN the system SHALL respond 400 with an `errors` entry for field `state`. `CUST-49`
16. WHEN a customer is created with state `sp` THEN the system SHALL store and return `SP`. `CUST-50`
17. WHEN a customer is created with city `  São   Paulo ` THEN the system SHALL store and return `São Paulo`. `CUST-51`

**Independent Test**: POST a valid customer, receive 201 with a UUID and Location header; POST the same email again, receive 409.

---

### P1: Retrieve a customer ⭐ MVP

**User Story**: As a client application, I want to fetch a customer by id so that I can display or use its data.

**Why P1**: Read-back is required to use any stored customer.

**Acceptance Criteria**:

1. WHEN a client sends `GET /api/v1/customers/{id}` for an existing customer THEN the system SHALL respond 200 with that customer. `CUST-14`
2. IF no customer exists with `{id}` THEN the system SHALL respond 404. `CUST-15`
3. IF `{id}` is not a valid UUID THEN the system SHALL respond 400. `CUST-16`

**Independent Test**: Create a customer, GET it by id and compare fields; GET a random UUID and receive 404.

---

### P1: Update a customer ⭐ MVP

**User Story**: As a client application, I want to replace a customer's data so that the record stays correct.

**Why P1**: Customer data changes; CRUD is incomplete without it.

**Acceptance Criteria**:

1. WHEN a client sends `PUT /api/v1/customers/{id}` with a valid body for an existing customer THEN the system SHALL replace name, email, cpf, phone, birthDate, city and state and respond 200 with the updated customer. `CUST-17`
2. WHEN the PUT body omits `phone` or `birthDate` THEN the system SHALL store that field as null. `CUST-18`
3. WHEN a customer is updated THEN the system SHALL keep `id` and `createdAt` unchanged and SHALL set `updatedAt` to the update time. `CUST-19`
4. IF the PUT body violates any rule in CUST-05 to CUST-09, CUST-48 or CUST-49 THEN the system SHALL respond 400 with the matching `errors` entries and SHALL leave the customer unchanged. `CUST-20`
5. IF the new email or cpf belongs to a different customer THEN the system SHALL respond 409 with the matching `errors` entry and SHALL leave the customer unchanged. `CUST-21`
6. WHEN the PUT body keeps the customer's own email and cpf THEN the system SHALL NOT report a conflict. `CUST-22`
7. IF no customer exists with `{id}` THEN the system SHALL respond 404 and SHALL NOT create a customer. `CUST-23`
8. IF a concurrent transaction modified the same customer before this update commits THEN the system SHALL respond 409 and SHALL keep the data from the transaction that committed first. `CUST-24`

**Independent Test**: Create a customer, PUT new values, GET shows the new values with the original createdAt.

---

### P1: Delete a customer ⭐ MVP

**User Story**: As a client application, I want to delete a customer so that records that must not be kept are removed.

**Why P1**: Completes CRUD.

**Acceptance Criteria**:

1. WHEN a client sends `DELETE /api/v1/customers/{id}` for an existing customer THEN the system SHALL remove it and respond 204 with an empty body. `CUST-25`
2. WHEN a customer has been deleted THEN the system SHALL respond 404 to `GET /api/v1/customers/{id}`. `CUST-26`
3. WHEN a customer has been deleted THEN the system SHALL accept its email and cpf for a new customer. `CUST-27`
4. IF no customer exists with `{id}` THEN the system SHALL respond 404. `CUST-28`

**Independent Test**: Create, DELETE (204), GET (404), re-create with the same email and CPF (201).

---

### P1: List customers ⭐ MVP

**User Story**: As a client application, I want to page through customers so that I can show them without loading everything.

**Why P1**: Required to discover customers without knowing ids.

**Acceptance Criteria**:

1. WHEN a client sends `GET /api/v1/customers` without parameters THEN the system SHALL respond 200 with the first 20 customers sorted by name ascending in the envelope `{content: [...], page: {number, size, totalElements, totalPages}}`. `CUST-29`
2. WHEN `page` and `size` are given THEN the system SHALL return the customers of that zero-based page with that page size. `CUST-30`
3. WHEN `sort=<property>,<asc|desc>` is given with property name, email, createdAt or updatedAt THEN the system SHALL order results by that property and direction. `CUST-31`
4. IF `page` is below 0 or `size` is below 1 or above 100 THEN the system SHALL respond 400. `CUST-32`
5. IF `sort` names any property other than name, email, createdAt or updatedAt THEN the system SHALL respond 400. `CUST-33`
6. WHEN no customers match THEN the system SHALL respond 200 with empty `content` and `totalElements` 0. `CUST-34`

**Independent Test**: Create 25 customers; default GET returns 20 sorted by name with totalElements 25; `page=1` returns 5.

---

### P1: Consistent error contract ⭐ MVP

**User Story**: As a client developer, I want every error in the same format so that I can handle failures programmatically.

**Why P1**: Every story above relies on it.

**Acceptance Criteria**:

1. The system SHALL return every 4xx and 5xx response with content type `application/problem+json` and the fields `type`, `title`, `status`, `detail` and `instance`. `CUST-35`
2. WHEN request validation fails THEN the system SHALL include an `errors` array with one `{field, message}` entry per invalid field. `CUST-36`
3. IF the request body is not well-formed JSON THEN the system SHALL respond 400. `CUST-37`
4. IF a POST or PUT request has a content type other than `application/json` THEN the system SHALL respond 415. `CUST-38`
5. IF an unexpected exception occurs THEN the system SHALL respond 500 with `detail` equal to `An unexpected error occurred.` and SHALL NOT include the exception message or stack trace. `CUST-39`

**Independent Test**: Send malformed JSON, a text/plain body and an invalid customer; every response is problem+json with the stated status.

---

### P2: Search customers

**User Story**: As a client application, I want to filter the customer list by name or email so that I can find a customer quickly.

**Why P2**: Listing works without it; search improves usability.

**Acceptance Criteria**:

1. WHEN the `name` parameter is given THEN the system SHALL return only customers whose name contains the value, ignoring case. `CUST-40`
2. WHEN the `email` parameter is given THEN the system SHALL return only the customer whose email equals the trimmed value, ignoring case. `CUST-41`
3. WHEN both `name` and `email` are given THEN the system SHALL return only customers matching both. `CUST-42`

**Independent Test**: Create "Ana Souza", "Mariana Lima", "Bruno Reis"; `name=ana` returns the first two.

---

### P2: Operability

**User Story**: As an operator, I want health checks, safe logs and automatic schema setup so that I can run the service in production.

**Why P2**: The API works without it, but it cannot be operated safely.

**Acceptance Criteria**:

1. WHEN a client sends `GET /actuator/health` THEN the system SHALL respond 200 with `status` `UP` while the database is reachable. `CUST-43`
2. The system SHALL expose no actuator endpoint other than `health` over HTTP. `CUST-44`
3. WHEN a customer is created, updated or deleted THEN the system SHALL write one INFO log line containing the operation and the customer id. `CUST-45`
4. The system SHALL never write a customer's email, cpf or phone to the logs. `CUST-46`
5. WHEN the application starts against an empty PostgreSQL database THEN the system SHALL create the customer schema through versioned migrations without manual steps. `CUST-47`

**Independent Test**: Start against an empty PostgreSQL database, GET /actuator/health returns UP, create a customer, and the log shows its id but not its email or CPF.

---

## Edge Cases

- IF the email differs from an existing one only by case or surrounding spaces THEN the system SHALL treat it as a duplicate (covered by CUST-03 and CUST-10).
- IF the cpf is `111.111.111-11` (valid length, all digits equal) THEN the system SHALL reject it with 400 (CUST-07).
- WHEN `name` has exactly 2 or exactly 120 characters after trimming THEN the system SHALL accept it (CUST-05).
- WHEN `birthDate` is yesterday in UTC THEN the system SHALL accept it (CUST-09).
- WHEN `size=100` THEN the system SHALL accept it (CUST-32).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| CUST-01 | P1: Create | Execute | Implementing |
| CUST-02 | P1: Create | Design | Pending |
| CUST-03 | P1: Create | Execute | Implementing |
| CUST-04 | P1: Create | Execute | Implementing |
| CUST-05 | P1: Create | Execute | Implementing |
| CUST-06 | P1: Create | Execute | Implementing |
| CUST-07 | P1: Create | Execute | Implementing |
| CUST-08 | P1: Create | Execute | Implementing |
| CUST-09 | P1: Create | Execute | Implementing |
| CUST-10 | P1: Create | Execute | Implementing |
| CUST-11 | P1: Create | Execute | Implementing |
| CUST-12 | P1: Create | Design | Pending |
| CUST-13 | P1: Create | Design | Pending |
| CUST-14 | P1: Retrieve | Execute | Implementing |
| CUST-15 | P1: Retrieve | Execute | Implementing |
| CUST-16 | P1: Retrieve | Design | Pending |
| CUST-17 | P1: Update | Execute | Implementing |
| CUST-18 | P1: Update | Execute | Implementing |
| CUST-19 | P1: Update | Execute | Implementing |
| CUST-20 | P1: Update | Design | Pending |
| CUST-21 | P1: Update | Execute | Implementing |
| CUST-22 | P1: Update | Execute | Implementing |
| CUST-23 | P1: Update | Execute | Implementing |
| CUST-24 | P1: Update | Design | Pending |
| CUST-25 | P1: Delete | Execute | Implementing |
| CUST-26 | P1: Delete | Design | Pending |
| CUST-27 | P1: Delete | Design | Pending |
| CUST-28 | P1: Delete | Execute | Implementing |
| CUST-29 | P1: List | Design | Pending |
| CUST-30 | P1: List | Design | Pending |
| CUST-31 | P1: List | Design | Pending |
| CUST-32 | P1: List | Design | Pending |
| CUST-33 | P1: List | Design | Pending |
| CUST-34 | P1: List | Design | Pending |
| CUST-35 | P1: Error contract | Design | Pending |
| CUST-36 | P1: Error contract | Design | Pending |
| CUST-37 | P1: Error contract | Design | Pending |
| CUST-38 | P1: Error contract | Design | Pending |
| CUST-39 | P1: Error contract | Design | Pending |
| CUST-40 | P2: Search | Design | Pending |
| CUST-41 | P2: Search | Design | Pending |
| CUST-42 | P2: Search | Design | Pending |
| CUST-43 | P2: Operability | Design | Pending |
| CUST-44 | P2: Operability | Execute | Implementing |
| CUST-45 | P2: Operability | Execute | Implementing |
| CUST-46 | P2: Operability | Execute | Implementing |
| CUST-47 | P2: Operability | Execute | Implementing |
| CUST-48 | P1: Create | Execute | Implementing |
| CUST-49 | P1: Create | Execute | Implementing |
| CUST-50 | P1: Create | Execute | Implementing |
| CUST-51 | P1: Create | Execute | Implementing |

**Coverage:** 51 total, 0 mapped to tasks, 51 unmapped ⚠️ (mapped during Tasks)

---

## Success Criteria

- [ ] All 51 acceptance criteria have at least one passing automated test that asserts the spec-defined outcome.
- [ ] The full test suite passes against a real PostgreSQL instance.
- [ ] Zero 500 responses across the error scenarios in this spec (CUST-05 to CUST-12, CUST-48, CUST-49, CUST-15, CUST-16, CUST-20 to CUST-24, CUST-28, CUST-32, CUST-33, CUST-37, CUST-38).
