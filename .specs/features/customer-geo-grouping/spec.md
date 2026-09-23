# Customer Geographic Grouping Specification

## Problem Statement

The business needs to know where its customers are. Today the only way to answer "how many customers per state and city" is to page through every customer and count on the client side, which is slow and wasteful. A single read endpoint that returns counts grouped by state and then by city, computed by the database, answers the question in one call.

## Goals

- [ ] One request returns customer counts per state and per city, with no customer-level data.
- [ ] The grouping runs as a single aggregate database query; no customer entities are loaded into memory.
- [ ] Every acceptance criterion below has a passing automated test.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Filters (by state, by date, by other attributes) | Not requested. |
| Pagination of the grouping | The response is bounded by 27 UFs and the cities in use; see Assumptions. |
| Validating city names against the IBGE municipality list | Not requested; needs an external dataset. |
| Grouping without accents (`Uberlandia` = `Uberlândia`) | Decided: accents are significant. Would need `unaccent` or a normalized column. |
| Caching the result | Premature; no load data yet. |
| Soft-delete handling | Customer deletion is a hard delete (customer-management spec), so every stored customer is active. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Dependency on the CRUD | Location (`city`, `state`, both required) is added by amending the customer-management spec (CUST-48 to CUST-51). This feature is implemented after the CRUD. | The CRUD is specified but not implemented; one model avoids a second migration and a second contract change | y |
| Endpoint path | `GET /api/v1/customers/grouped-by-location` | Consistent with the `/api/v1/customers` base of the CRUD | y |
| `document` field in the requested model | Kept as `cpf` from the customer-management spec | Same concept already specified with CPF validation | y |
| City grouping key | Cities are grouped per state ignoring case; accents are significant; whitespace is already normalized on write (CUST-51) | Merges `Campinas` / `campinas` without changing stored data | y |
| City display name | The spelling of the earliest-created customer in the group (ties broken by id) | Deterministic and independent of database collation | y |
| City ordering | Alphabetical by pt-BR collation, ignoring case and accents for ordering only (`Águas de Lindóia` before `Bauru`) | Matches how Portuguese speakers expect names sorted; the database collation is environment-dependent | y |
| State ordering | Alphabetical by the two-letter UF | Requested | y |
| Active customers | Every stored customer counts | Hard delete means there is no inactive state | y |
| Pagination | None | At most 27 states; cities are bounded by the municipalities actually used | y |
| Indexes | None added for this feature | A full-table aggregate with no filter reads every row either way; see design.md | y |
| Requirement ID format | `GEO-001` style | The requested `REQ-GEO-001` has two hyphens, which the skill's spec validator rejects; the ids stay unique and traceable | y |

**Open questions:** none - all resolved or logged above (required before the spec is confirmed).

---

## User Stories

### P1: Customer counts by state and city ⭐ MVP

**User Story**: As a business analyst, I want the number of customers per state and per city so that I can see where our customers are without exporting the customer list.

**Why P1**: It is the whole feature.

**Acceptance Criteria** (each line is one EARS pattern):

1. WHEN a client sends `GET /api/v1/customers/grouped-by-location` THEN the system SHALL respond 200 with a JSON body `{"states": [{"state", "totalCustomers", "cities": [{"city", "totalCustomers"}]}]}`. `GEO-001`
2. The system SHALL return exactly one state entry per UF that has at least one customer, with `totalCustomers` equal to the number of customers in that UF. `GEO-002`
3. The system SHALL return, inside each state entry, exactly one city entry per city of that state that has at least one customer, with `totalCustomers` equal to the number of customers in that city and state. `GEO-003`
4. The system SHALL make each state's `totalCustomers` equal to the sum of its cities' `totalCustomers`. `GEO-004`
5. The system SHALL order state entries alphabetically by UF. `GEO-005`
6. The system SHALL order city entries within each state alphabetically by pt-BR collation, ignoring case and accents, so that `Águas de Lindóia`, `Bauru`, `campos do Jordão` appear in that order. `GEO-006`
7. WHEN no customers exist THEN the system SHALL respond 200 with the body `{"states": []}`. `GEO-007`
8. The system SHALL NOT include any customer-level field (id, name, email, cpf, phone, birthDate, createdAt, updatedAt) in the response. `GEO-008`
9. WHEN two customers in the same state have cities that differ only by letter case THEN the system SHALL count them in one city entry. `GEO-009`
10. WHEN cities with the same name exist in different states THEN the system SHALL count them in separate city entries under each state. `GEO-010`
11. WHEN a city group contains different spellings by case THEN the system SHALL display the spelling of the earliest-created customer in that group. `GEO-011`
12. WHEN cities differ by accents (`Uberlândia`, `Uberlandia`) THEN the system SHALL count them in separate city entries. `GEO-012`

**Independent Test**: Create customers in Limeira/SP (1), Campinas/SP (2) and Uberlândia/MG (1); the endpoint returns MG (1: Uberlândia 1) then SP (3: Campinas 2, Limeira 1).

---

### P1: Counts reflect current data ⭐ MVP

**User Story**: As a business analyst, I want the counts to reflect deletions and moves so that the report matches the customer base.

**Why P1**: Stale or wrong counts make the report useless.

**Acceptance Criteria**:

1. WHEN a customer is deleted THEN the system SHALL exclude it from the next grouping response. `GEO-013`
2. WHEN a customer's city or state is changed through PUT THEN the system SHALL count it only under the new location in the next grouping response. `GEO-014`
3. WHEN the last customer of a city or state is deleted or moved THEN the system SHALL omit that city or state entry from the response. `GEO-015`

**Independent Test**: Create two customers in Campinas/SP, move one to Curitiba/PR and delete the other; the response has only PR with Curitiba (1).

---

### P1: Efficient aggregation ⭐ MVP

**User Story**: As an operator, I want the grouping computed by the database so that the endpoint stays fast and memory-safe as the customer base grows.

**Why P1**: Explicit technical requirement; loading every customer does not scale.

**Acceptance Criteria**:

1. WHEN the grouping endpoint is called THEN the system SHALL execute exactly one SQL statement against the customer table, regardless of the number of states and cities. `GEO-016`
2. WHEN the grouping endpoint is called THEN the system SHALL load zero customer entities into the persistence context. `GEO-017`

**Independent Test**: With customers in 3 states and 5 cities, Hibernate statistics after one call show 1 prepared statement and 0 entity loads.

---

## Edge Cases

- WHEN exactly one customer exists THEN the system SHALL return one state with one city, both with `totalCustomers` 1 (GEO-002, GEO-003).
- WHEN all customers are in one city THEN the state total SHALL equal that city total (GEO-004).
- WHEN a client sends a query parameter to the endpoint THEN the system SHALL ignore it and return the full grouping (no filters are in scope).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| GEO-001 | P1: Counts by state and city | Design | Pending |
| GEO-002 | P1: Counts by state and city | Design | Pending |
| GEO-003 | P1: Counts by state and city | Design | Pending |
| GEO-004 | P1: Counts by state and city | Design | Pending |
| GEO-005 | P1: Counts by state and city | Design | Pending |
| GEO-006 | P1: Counts by state and city | Design | Pending |
| GEO-007 | P1: Counts by state and city | Design | Pending |
| GEO-008 | P1: Counts by state and city | Design | Pending |
| GEO-009 | P1: Counts by state and city | Design | Pending |
| GEO-010 | P1: Counts by state and city | Design | Pending |
| GEO-011 | P1: Counts by state and city | Design | Pending |
| GEO-012 | P1: Counts by state and city | Design | Pending |
| GEO-013 | P1: Counts reflect current data | Design | Pending |
| GEO-014 | P1: Counts reflect current data | Design | Pending |
| GEO-015 | P1: Counts reflect current data | Design | Pending |
| GEO-016 | P1: Efficient aggregation | Design | Pending |
| GEO-017 | P1: Efficient aggregation | Design | Pending |

**Coverage:** 17 total, 0 mapped to tasks, 17 unmapped ⚠️ (mapped during Tasks)

---

## Success Criteria

- [ ] All 17 acceptance criteria have at least one passing automated test asserting the spec-defined outcome.
- [ ] The endpoint issues one SQL statement per call and loads no customer entities (GEO-016, GEO-017).
