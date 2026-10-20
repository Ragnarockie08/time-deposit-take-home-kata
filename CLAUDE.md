# Time deposit kata: rules for AI agents

The solution lives in `java/`. The other language folders are the untouched starters and must not change.

## Hard constraints from the task
- `org.ikigaidigital.TimeDeposit` must not change in any way.
- `TimeDepositCalculator` keeps its package, its public no-arg constructor and
  `void updateBalance(List<TimeDeposit>)`. Its results must stay identical, except for the documented money
  fix (see `docs/DESIGN_NOTES.md`, 1.1), which lives in its own commit.
- Exactly two REST endpoints: `GET /time-deposits` and `POST /time-deposits/balance-updates`. Never add a third.
- Handling invalid input is not required. Do not add validation layers.

## Architecture
Hexagonal, one Maven module, Spring Boot 4.1, Java 17, PostgreSQL + Flyway.

```
org.ikigaidigital
├── TimeDeposit, TimeDepositCalculator   shared classes, stay in place
├── domain/        PlanType, InterestPolicy + one policy per plan, Withdrawal
├── application/   port/in (use cases), port/out (repositories), service (implements the use cases)
├── adapter/rest/  controller implementing the interface generated from the OpenAPI contract
└── adapter/db/    JPA entities, Spring Data repositories, port implementations
```
- Dependencies point inward: adapters → application → domain. The domain imports no Spring, JPA or HTTP types.
- The OpenAPI file `java/src/main/resources/static/openapi/time-deposits.yaml` is the source of truth for the API.
  Change the contract first, then the code.
- JPA entities are separate from `TimeDeposit`. Never annotate the shared class.
- Money is `BigDecimal` / `numeric(19,2)` everywhere outside the shared class.
- Keep it lean: no extra layers, decorators or abstractions without a concrete need from the task.

## Working rules
- Behaviour first: the calculator pinning tests (`TimeDepositCalculatorTest`) are the safety net. A refactor
  must keep them green without editing them.
- Tests explain themselves through their names: no comments or Javadoc in test code.
- Production comments are one short line, only where the reason is not obvious; longer explanations go into
  `docs/DESIGN_NOTES.md`.
- Every design trade-off or known limitation goes into `docs/DESIGN_NOTES.md` with a status.
- Assumptions are stated in code comments where they apply, and summarised in the README.
- Code, comments, file names and docs in English.
- Git (GitHub flow): `main` only receives merged pull requests. Each step gets a short-lived branch
  (`chore/`, `refactor/`, `feature/`, `fix/`, `docs/`) off `main`, or stacked on the previous step's branch
  until that one is merged. One logical change per commit, descriptive imperative messages, no
  Co-Authored-By trailer. Never commit on `main`, push, open PRs or change remotes; the owner does that.

## Commands
```
cd java
./mvnw test      # unit tests, no Docker needed
./mvnw verify    # plus integration tests (*IT) with Testcontainers, needs Docker
docker compose up -d && ./mvnw spring-boot:run   # local run, Swagger UI at /swagger-ui.html
```

## Agent team
Defined in `.claude/agents/`. The main session is the lead: it plans, writes tickets with file ownership,
defines shared interfaces and merges. `implementer` builds one ticket in its own worktree, `reviewer`
checks each diff against its ticket before it lands, `researcher` verifies library versions and docs.
See `docs/AI_WORKFLOW.md`.
