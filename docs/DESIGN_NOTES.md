# Design notes: known issues and future improvements

This file lists the design issues found while working on the kata, what the submission does about each
one, and how it could be improved later. Status values:

- **Fixed**: handled in this submission.
- **Mitigated**: partly handled, the rest is described.
- **Documented**: intentionally left as is, with the reason and the proposed improvement.

## 1. Money correctness

### 1.1 Interest computed in `double` (Fixed, as a separate commit)
The legacy `TimeDepositCalculator` computes interest as `balance * rate / 12` in `double` and rounds
`new BigDecimal(double)` with `HALF_UP`. Binary floating point cannot represent most decimal values, so
at exact half-cent results the value lands just below `.xx5` and is rounded **down**.

A sweep over every balance from 0.01 to 1,000,000.00 found the legacy result differs from exact decimal
arithmetic in:

| Plan rate | Balances with wrong interest | Example |
|---|---|---|
| 1% (basic) | 30,349 (0.03%) | 18.00 → legacy 0.01, correct 0.02 |
| 3% (student) | 170,436 (0.17%) | 6.00 → legacy 0.01, correct 0.02 |
| 5% (premium) | 95,631 (0.10%) | 18.00 → legacy 0.07, correct 0.08 |

The error always under-pays the customer. The README states that `updateBalance` works correctly and
its behaviour must not change, so the work is split:

1. The refactor commit keeps the legacy `double` arithmetic exactly; the pinning tests prove it.
2. A separate commit (`Calculate interest in decimal arithmetic`) switches the calculator and the policies to
   `BigDecimal`. `TimeDeposit` and the `updateBalance` signature are unchanged: the balance is read with
   `BigDecimal.valueOf`, interest is rounded `HALF_UP` to cents, and only the result is written back as a
   `Double`. It can be reverted on its own if the legacy behaviour is required.

Of the 21 pinning tests, exactly the 5 that recorded `double` artefacts changed: the three half-cent cases
(now rounded up) and two balances that were not whole cents (`1.3800000000000001` is now `1.38`,
`1001.6600000000001` is now `1001.66`). The same sweep against exact decimal arithmetic, for all three
plans, now finds **0** differences in 300,000,000 cases.

Other effects of the change, outside the money range the application uses:

- `InterestPolicy.monthlyInterest` now returns `BigDecimal` instead of `double`. This changes the public
  interface behind `TimeDepositCalculator(Map<PlanType, InterestPolicy>)`; custom policies must be updated.
- A `NaN` or infinite balance now fails with `NumberFormatException` on every path; before, it passed through
  unchanged when no interest was due. Balances come from `numeric(19,2)`, so the application cannot produce one.

### 1.2 Balance is a `Double` in the shared `TimeDeposit` class (Documented)
`TimeDeposit` must not change, so its `Double balance` stays. The database (`numeric(19,2)`) and the API
use decimals and convert at the edges with `BigDecimal.valueOf`. For whole-cent balances (all balances that
come from the database), the decimal calculator returns the `double` closest to a whole-cent value, so the
conversion is exact; the adapters still round to cents defensively.
**Improvement:** a versioned `TimeDeposit` (or a new domain type) with a `BigDecimal` or `Money` balance,
migrated with consumers.

### 1.3 Rounding policy (Documented)
`HALF_UP` is kept as the business rule. Banks often use `HALF_EVEN` (banker's rounding) to remove
systematic bias. **Improvement:** make the rounding mode a business decision in configuration.

### 1.4 Simple monthly interest (Documented)
Monthly interest is `annual rate / 12`, regardless of days in the month or a day-count convention
(Actual/365, 30/360). **Improvement:** a day-count convention per product, as a policy.

## 2. Concurrency and idempotency

### 2.1 Concurrent updates across instances (Fixed)
The update runs in one transaction and loads rows with a pessimistic write lock, ordered by id. Hibernate
renders it on PostgreSQL as `SELECT ... ORDER BY id FOR NO KEY UPDATE`: it blocks other updates, deletes and
lockers of those rows, and only lets foreign-key checks (a new withdrawal) through. Row locks live in
PostgreSQL, so they work across any number of application instances:

- all-or-nothing: a crash mid-run rolls back;
- no lost update: a second run waits and then reads the committed balances;
- no deadlock: rows are always locked in `id` order.

### 2.2 Overlapping runs (Fixed)
At the start of the transaction the update takes `pg_try_advisory_xact_lock` on a fixed key, on the same
connection as the JPA work (an integration test proves the lock is held until commit). If another
instance is already running the update, the call returns **409 Conflict** at once instead of queueing behind
the row locks and applying interest a second time. The lock is released automatically on commit or rollback,
so a crashed instance cannot leave it held.

### 2.3 Repeated runs apply interest again (Mitigated)
2.2 only rejects runs that **overlap**. A second POST sent after the first one finished (or a client retry
after a timeout) still applies another month of interest. The task fixes the schema and the domain has no
notion of an interest period (`days` is never advanced), so this is not solved here. **Improvement:**

- an `interest_runs(period PRIMARY KEY)` table inserted in the same transaction, so a second run for the
  same period fails on the unique constraint (exactly once, atomic with the update);
- or per-deposit `last_interest_date`, which also supports resuming a partial batch.

### 2.4 Whole-table lock in one transaction (Documented)
Fine for this data size; at bank scale it means one long transaction holding every row lock, and balances
are written with one `UPDATE` per row (no JDBC batching). **Improvement:** process in chunks (keyset
pagination, one transaction per chunk) together with 2.3, and enable Hibernate JDBC batching.

### 2.5 Consistent reads for the listing (Fixed)
`GET /time-deposits` reads deposits and withdrawals with two queries. It runs in a read-only
`REPEATABLE_READ` transaction, so both queries see the same snapshot.

## 3. Domain model

### 3.1 Time does not advance (Documented)
`days` is treated as input owned by another service, the one that creates deposits and keeps their age up to
date. This service only applies one month of interest to the deposits as they are stored, and writes nothing
but `balance`; its row locks keep it from overwriting changes made by that other service. This matches the task:
the shared `TimeDeposit` has no setter for `days`, the original calculator never changed it, and the two required
endpoints include no way to create or age a deposit. In this repository the demo data has no such service, so
repeated updates see the same `days`. **Improvement**, if this service owned the data: store the date interest
starts (the value date) and compute age from it and a business clock, instead of keeping a `days` counter.

### 3.2 "No interest after 1 year" means `days < 366` (Documented)
Kept exactly as the legacy code does. Leap years and the exact cut-off day are business questions.

### 3.3 Plan types are free-form strings (Fixed)
The shared `TimeDeposit` exposes the plan as a `String`, so a misspelled plan would silently earn nothing.
The text is now parsed once into a `PlanType` enum, and policies are registered in an `EnumMap`; a test fails
the build if a plan has no policy. The OpenAPI contract declares `planType` as an enum, and the database has a
`CHECK` constraint on `plan_type` (column type stays `varchar`, as the task defines). The three lists are kept in step
by tests: the API enum must equal the domain enum, and the database must accept every domain plan. For compatibility, the
shared calculator still pays no interest for an unknown code passed by other callers.

### 3.4 Grace period is shared by all plans (Documented)
The 30-day rule lives in the calculator because every current plan has it. The task says "for any
**existing** plans", so a future plan may not. **Improvement:** move it into the policy as a default method
`gracePeriodDays()` returning 30, which a new plan can override; existing plans need no change.

### 3.5 Withdrawals do not affect interest (Documented)
The task returns withdrawals but does not say how they affect interest. **Improvement:** a
withdrawal-aware `InterestPolicy` (for example a penalty for early withdrawal), which the policy design
supports without changing the calculator.

### 3.6 Balance is overwritten instead of booked (Documented)
Interest is applied by overwriting `balance`, so there is no audit trail. **Improvement:** a ledger of
interest entries, with the balance derived from it or reconciled against it.

## 4. API and security

### 4.1 No authentication or authorization (Documented)
Both endpoints are open, including a bulk operation that moves money. **Improvement:** OAuth2 / JWT at
the gateway or as a resource server, an operator role for the update, and audit logging of who ran it.

### 4.2 No input validation (Documented)
Not required by the task. The endpoints take no input today.

### 4.3 GET returns everything (Documented)
No pagination. **Improvement:** cursor pagination once the deposit count grows.

### 4.4 Interest run as an HTTP endpoint (Documented)
Required by the task. In production this is a scheduled batch job with a single leader (for example
ShedLock), with metrics and alerts on each run, not a public endpoint.

## 5. Database

### 5.1 Schema naming (Documented)
The task names tables and columns in camelCase (`timeDeposits`, `planType`). PostgreSQL folds unquoted
identifiers to lowercase, so the schema uses snake_case (`time_deposits`, `plan_type`) with a 1:1 mapping.

### 5.2 Demo data is a regular migration (Documented)
`V2__seed_demo_data.sql` lives next to the schema, so every environment gets the demo deposits. That suits
this exercise (there is no endpoint to create deposits). **Improvement:** move it to a separate Flyway
location enabled only for local and demo profiles.

## 6. Architecture

### 6.1 Spring annotations in the application core (Documented)
The domain is framework-free, but the application service uses `@Service` and `@Transactional`. A strict
hexagonal core would be a plain class wired in a `@Configuration`, with transactions applied outside it.
For a service this size that adds wiring without changing behaviour, so the pragmatic form is kept.

### 6.2 Policy registry (Documented)
The calculator falls back to no interest when a known plan has no registered policy, the same as for an
unknown plan. The default registry is complete (a test fails the build otherwise), but a custom map passed
to the second constructor could miss a plan silently. **Improvement:** fail fast on an incomplete registry
once custom registries are used in production code.

### 6.3 Shared classes outside the domain package (Documented)
`TimeDeposit` and `TimeDepositCalculator` must stay in `org.ikigaidigital`, while the policies live in
`org.ikigaidigital.domain`, so the two packages reference each other. **Improvement:** move both shared
classes into `domain` once consumers can migrate.

### 6.4 Two persistence adapters (Documented)
`TimeDepositRepository` and `WithdrawalRepository` both declare `findAll()` with different return types, so
one class cannot implement both. Each port has its own small adapter.
