# Customer Management Context

**Gathered:** 2026-09-22
**Spec:** `.specs/features/customer-management/spec.md`
**Status:** Ready for design

---

## Feature Boundary

A CRUD REST API for customers under `/api/v1/customers`, backed by PostgreSQL, with validation, uniqueness on email and CPF, paginated listing with name/email search, RFC 9457 errors, a health endpoint and PII-safe logging.

---

## Implementation Decisions

### Data model

- Fields: name, email, cpf (required); phone, birthDate (optional); id (UUID), createdAt, updatedAt (server-managed).
- CPF is validated with check digits and stored as 11 digits.

### Delete

- Hard delete. Email and CPF become reusable after deletion.

### Update

- PUT only, full replacement. Omitted optional fields become null. No PATCH.

### Listing

- Paginated (default size 20, max 100) with sort, plus optional `name` (contains, case-insensitive) and `email` (exact, case-insensitive) filters.

### Agent's Discretion

None. The user accepted the recommended option for every question asked.

### Declined / Undiscussed Gray Areas → Assumptions

Base path/versioning, UUID ids, authentication (gateway), error format, email/CPF normalization, CPF mutability, field bounds, page bounds, sort whitelist, optimistic locking, unknown-field handling, timestamps, observability and schema migrations were not discussed. Each is logged with a default and rationale in the spec's Assumptions & Open Questions table (Confirmed? = n) and is confirmed or overridden during spec review.

---

## Specific References

No specific requirements - open to standard approaches.

---

## Deferred Ideas

- OpenAPI/Swagger documentation.
- ETag / If-Match conditional updates.
