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

### L-012 - When a refactor moves exception translation from a global handler to each call site, add a test for every call site; a behaviour that was structurally global becomes per-path and can regress silently.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `refactoring/errors` · harmful: 0
- features: hexagonal-architecture
- evidence: M3b (refactoring/errors)
- last seen: 2026-09-23T05:10:22Z

### L-013 - When two defenses guard one requirement (explicit version check and transactional versioned UPDATE), mutate each alone and both together, and state in the design which defense is live in production.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `concurrency` · harmful: 0
- features: hexagonal-architecture
- evidence: M10 (concurrency)
- last seen: 2026-09-23T05:10:22Z

### L-014 - Requirements like 'implemented once' or 'delegates to X' need a structural rule (ArchUnit call or dependency assertion); behavioural tests cannot tell a copy from delegation.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `architecture/tests` · harmful: 0
- features: hexagonal-architecture
- evidence: M11b (architecture/tests)
- last seen: 2026-09-23T05:10:22Z

### L-015 - An ArchUnit rule that only checks an annotation's presence or a call's existence can be evaded (propagation=SUPPORTS, ignored result); constrain the attribute or 'only calls X', or pin it with a behaviour test.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `architecture/tests` · harmful: 0
- features: hexagonal-architecture
- evidence: N3 (architecture/tests)
- last seen: 2026-09-23T05:45:06Z

### L-016 - When a race test disables a pre-check by stubbing a spy, assert the downstream effect (the write was attempted, after clearing arrangement invocations); verifying the stubbed call is not enough because the real pre-check may still answer.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `tests/stubs` · harmful: 0
- features: hexagonal-architecture
- evidence: N1b (tests/stubs)
- last seen: 2026-09-23T05:45:06Z

### L-017 - Re-verification must attack the new guards themselves with evasion mutants, not only replay old survivors; replaying alone gives a false PASS.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `process` · harmful: 0
- features: hexagonal-architecture
- evidence: N4b (process)
- last seen: 2026-09-23T05:45:06Z

### L-018 - A class-level annotation rule does not bound effective behaviour when Spring resolves method-level annotations first; guard effective attributes (transaction, security, cache) with a behavioural probe instead of one static rule per annotation level.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `architecture/tests` · harmful: 0
- features: hexagonal-architecture
- evidence: X5 (architecture/tests)
- last seen: 2026-09-23T06:24:56Z

### L-019 - Use onlyCallCodeUnitsThat rather than onlyCallMethodsThat when a class must compute nothing itself; method-only rules leave constructor calls open.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `architecture/tests` · harmful: 0
- features: hexagonal-architecture
- evidence: A1 (architecture/tests)
- last seen: 2026-09-23T06:24:56Z

### L-020 - Put multi-step test arrangements that guard against vacuity (stub + clearInvocations) in one helper; separate lines can each be forgotten.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `tests/stubs` · harmful: 0
- features: hexagonal-architecture
- evidence: X14 (tests/stubs)
- last seen: 2026-09-23T06:24:56Z

### L-021 - A 'runs in a transaction' probe must prove the read and the write share the same transaction; a check at one call site misses REQUIRES_NEW splits, including on outbound adapters. Count completed transactions per request instead.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `concurrency` · harmful: 0
- features: hexagonal-architecture
- evidence: Y8 (concurrency)
- last seen: 2026-09-23T06:54:28Z

### L-022 - Pair every mutation that is invisible over HTTP with the mutation that removes the other guard, and run that guard's removal alone as a control, to expose the real consequence (e.g. a lost update).
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `process` · harmful: 0
- features: hexagonal-architecture
- evidence: Y7 (process)
- last seen: 2026-09-23T06:54:28Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
