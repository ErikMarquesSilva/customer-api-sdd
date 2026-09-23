# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Specify/Design)

Corroborated across multiple features. Safe to apply as guidance.

_none_

## Candidates (under observation - do NOT load as guidance yet)

Seen once or not yet corroborated. Tracked, not trusted.

### L-001 - When a grouping or uniqueness key uses database case folding (lower/upper), test a non-ASCII case pair (SÃO/São) and state the required UTF-8 LC_CTYPE in the spec, because folding depends on the database locale.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `persistence/sql` · harmful: 0
- features: customer-geo-grouping
- evidence: GEO-009 (persistence/sql)
- last seen: 2026-09-23T01:39:56Z

### L-002 - When the spec keeps variants distinct but orders them with a comparison that ignores the difference (accent-insensitive collator), name the tie-break in the spec so tests do not enforce an unstated choice.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `ordering` · harmful: 0
- features: customer-geo-grouping
- evidence: GEO-006 (ordering)
- last seen: 2026-09-23T01:39:56Z

### L-003 - Set the PgJDBC property logServerErrorDetail=false when logs must be PII-free; unique-violation detail echoes the key value into Hibernate's WARN log.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `logging/postgresql` · harmful: 0
- features: customer-management
- evidence: CUST-46 (logging/postgresql)
- last seen: 2026-09-23T01:39:56Z

### L-004 - Tick application clocks in microseconds when timestamps are stored in PostgreSQL timestamptz, or returned values differ from stored ones on Linux JDKs with nanosecond clocks.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `time/postgresql` · harmful: 0
- features: customer-management
- evidence: CUST-14 (time/postgresql)
- last seen: 2026-09-23T01:39:56Z

### L-005 - A test for 'nothing except X is exposed' must assert the whole exposed set (e.g. the discovery listing), not probe a few sample names.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `actuator/exposure` · harmful: 0
- features: customer-management
- evidence: M19 (actuator/exposure)
- last seen: 2026-09-23T01:47:12Z

### L-006 - Give every check-digit branch (e.g. remainder 10 -> 0) its own unit case; a data generator that hits it by chance is not evidence.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `validation/check-digits` · harmful: 0
- features: customer-management
- evidence: M02 (validation/check-digits)
- last seen: 2026-09-23T01:47:13Z

### L-007 - When a spec promises a per-field errors array, state whether query parameters and unparseable values are included.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `api/errors` · harmful: 0
- features: customer-management
- evidence: CUST-36 (api/errors)
- last seen: 2026-09-23T01:47:13Z

### L-008 - When the author narrows an AC to match current behaviour after verification, log it as an unconfirmed assumption for the user, never as settled spec.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `process` · harmful: 0
- features: customer-management
- evidence: CUST-36 (process)
- last seen: 2026-09-23T02:03:40Z

### L-009 - Never put test-only settings in a production config location (config/application.properties); it shadows production config in every test. Use a test bean or profile, and run mvn clean after deleting resources.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `test-config` · harmful: 0
- features: customer-management
- evidence: P1 (test-config)
- last seen: 2026-09-23T02:03:40Z

### L-010 - Assert every contract field of error responses (type, title, detail, instance), not only status and content type; mutants that drop a field survive status-only checks.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `api/errors` · harmful: 0
- features: customer-management
- evidence: CUST-35 (api/errors)
- last seen: 2026-09-23T02:03:40Z

### L-011 - Put ArchUnit violation fixtures outside the application's base package; Spring component and entity scanning picks up fixture @Entity/@Component classes and breaks every integration test.
- signal: `gate_fail` · recurrence: 1 feature(s) · scope: `architecture/tests` · harmful: 0
- features: hexagonal-architecture
- evidence: T5 (architecture/tests)
- last seen: 2026-09-23T04:38:32Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
