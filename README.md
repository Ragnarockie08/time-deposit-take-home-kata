# Time Deposit Refactoring Kata: Java solution

The solution lives in [`java/`](java). The other language folders are the untouched starters.
The original task is kept unchanged [at the end of this file](#the-task).

## Run it

Requirements: Java 17 or newer and Docker. Maven comes with the wrapper.

```bash
cd java
docker compose up -d          # PostgreSQL 16 on localhost:5432
./mvnw spring-boot:run        # API on http://localhost:8080
```

Flyway creates the schema and loads 8 demo deposits covering every plan and the boundary days
(30/31, 45/46, 365/366). If port 5432 or 8080 is taken, copy `.env.example` to `.env` and change `DB_PORT`
and `DB_URL`, and/or start the app with `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`.

## Trigger the endpoints with Swagger

Open **http://localhost:8080/swagger-ui.html**. It shows the hand-written contract
[`time-deposits.yaml`](java/src/main/resources/static/openapi/time-deposits.yaml), from which the
API interface and DTOs are generated.

1. **GET /time-deposits** → *Try it out* → *Execute*: all deposits with `id`, `planType`, `balance`, `days`
   and `withdrawals`.
2. **POST /time-deposits/balance-updates** → *Try it out* → *Execute*: `204 No Content`. One month of interest
   has been applied to every deposit.
3. Run the GET again to see the new balances. Deposits at 30 days (all plans), 45 days (premium) and 366 days
   (student) are unchanged.

The same with curl:

```bash
curl -s localhost:8080/time-deposits
curl -s -X POST -o /dev/null -w "%{http_code}\n" localhost:8080/time-deposits/balance-updates
```

The POST returns `409 Conflict` (`application/problem+json`) if another balance update is running at that
moment, on any instance.

## Tests

```bash
cd java
./mvnw test     # unit and slice tests, no Docker needed
./mvnw verify   # plus integration tests against PostgreSQL in Testcontainers
```

| Level | What it proves |
|---|---|
| `TimeDepositCalculatorTest` | Pinning tests for the calculator, written against the original code before any refactoring |
| `InterestPolicyTest` | Each plan's rate and day boundaries; every plan type has a policy |
| `TimeDepositServiceTest` | Use cases against hand-written in-memory ports |
| `TimeDepositControllerTest` | JSON shape of both endpoints, 204 and 409 |
| `TimeDepositPersistenceAdapterIT` | Schema, mapping, rounding to cents, row locks, advisory lock held until commit |
| `TimeDepositApiIT` | Both endpoints end to end on PostgreSQL, including a rejected overlapping run |

## Design

Hexagonal architecture in one Maven module, Spring Boot 4.1, Java 17, PostgreSQL, Flyway.

```
org.ikigaidigital
├── TimeDeposit, TimeDepositCalculator    shared classes, same package and API as before
├── domain/           PlanType, InterestPolicy + one policy per plan, Withdrawal
├── application/
│   ├── port/in       UpdateBalancesUseCase, GetTimeDepositsUseCase
│   ├── port/out      TimeDepositRepository, WithdrawalRepository, InterestRunLock
│   └── service/      TimeDepositService (implements both use cases)
├── adapter/rest/     controller implementing the interface generated from the OpenAPI contract
└── adapter/db/       JPA entities, Spring Data repositories, port implementations
```

- **Calculator:** the `if/else` chain became a Strategy per plan (`InterestPolicy`), chosen through an
  `EnumMap<PlanType, InterestPolicy>` registry, with a Null Object for unknown plans. A new plan is one policy
  class and one registry entry; the calculator does not change. `TimeDeposit` is untouched, and
  `updateBalance(List<TimeDeposit>)` keeps its signature.
- **Behaviour first:** 21 pinning tests were written against the original calculator, then the refactor
  landed without changing them. A comparison over 10 million random inputs gave bit-identical results.
- **Money fix:** the original `double` arithmetic under-pays at exact half cents (18.00 at 1% gives 0.01
  instead of 0.02). It is fixed in its own commit, after the behaviour-preserving refactor, and can be reverted
  alone. Details and numbers in [DESIGN_NOTES 1.1](docs/DESIGN_NOTES.md#11-interest-computed-in-double-fixed-as-a-separate-commit).
- **Concurrency:** the update runs in one transaction. A PostgreSQL advisory lock rejects an overlapping run
  from any instance with 409, and rows are locked in id order so no update is lost and runs cannot deadlock.
- **Security hygiene:** schema owned by Flyway (`ddl-auto=validate`), parameterised queries only, credentials
  from environment variables, `ProblemDetail` errors without stack traces, actuator limited to `health`.

## Assumptions

- One POST applies one month of interest. It does not advance `days`, as the original calculator never did,
  and it is not idempotent: every call applies another month.
- "No interest after 1 year" for students means `days < 366`, exactly as the original code.
- Plan types are the lowercase codes the original code used; unknown codes earn nothing.
- Withdrawals are returned but do not affect interest; the task does not say how they would.
- Tables and columns use snake_case (`time_deposits`, `plan_type`, `time_deposit_id`); PostgreSQL folds
  unquoted camelCase identifiers.
- Balances are `numeric(19,2)` in the database and decimals in the API. The shared class keeps its `Double`.
- No authentication and no input validation, as the task does not require them.

Every trade-off and known limitation, with what could be improved next, is in
[docs/DESIGN_NOTES.md](docs/DESIGN_NOTES.md).

## AI-assisted development

Built with Claude Code and a small agent team (lead, researcher, implementers in parallel git worktrees,
reviewer). Setup, rules and what was AI-assisted: [docs/AI_WORKFLOW.md](docs/AI_WORKFLOW.md),
[CLAUDE.md](CLAUDE.md), [.claude/agents/](.claude/agents).

## Commit history

One branch and pull request per step, merged in order:

| Branch | Step |
|---|---|
| `chore/ai-setup` | AI agent setup and design notes |
| `refactor/interest-policies` | Pinning tests, then the calculator refactor |
| `feature/spring-boot-setup` | Spring Boot, OpenAPI contract, application ports |
| `feature/persistence` | Schema, demo data, JPA adapters, locks |
| `feature/rest-api` | Service, REST adapter, end-to-end tests |
| `fix/decimal-interest` | Decimal interest arithmetic (deliberate behaviour change) |
| `docs/solution-readme` | This README and the AI workflow notes |

---

# The task

## Time Deposit Refactoring Kata - Take-Home Assignment

### XA Bank Time Deposit

#### Context
A junior developer implemented domain logic for a time deposit system but did not complete the API functionality. Your task is to refactor the existing codebase to implement all required functionalities based on the provided business requirements, ensuring no breaking changes occur.

#### Requirements

1. **API Endpoints**:
    - Create a RESTful API endpoint to update the balances of all time deposits in the database.
    - Create a RESTful API endpoint to retrieve all time deposits.
        - The GET endpoint should return a list of all time deposits with the following schema:
            - `id`
            - `planType`
            - `balance`
            - `days`
            - `withdrawals`

2. **Database Setup**:
    - Store all time deposit plans in a database.
    - Define the following tables:
        - `timeDeposits`:
            - `id`: Integer (primary key)
            - `planType`: String (required)
            - `days`: Integer (required)
            - `balance`: Decimal (required)
        - `withdrawals`:
            - `id`: Integer (primary key)
            - `timeDepositId`: Integer (foreign key, required)
            - `amount`: Decimal (required)
            - `date`: Date (required)

3. **Interest Calculation**:
    - Implement logic to calculate monthly interest based on the plan type:
        - **Basic Plan**: 1% interest
        - **Student Plan**: 3% interest (no interest after 1 year)
        - **Premium Plan**: 5% interest (interest starts after 45 days)
    - No interest is applied for the first 30 days for any existing plans.

4. **Refactoring Constraints**:
    - Do not introduce breaking changes to the shared `TimeDeposit` class or modify the `updateBalance` method signature.
    - Ensure the design is extensible to accommodate future complexities in interest calculations.

5. **Code Quality**:
    - Adhere to SOLID principles, design patterns, and clean code practices where applicable.

6. **AI-Assisted Development**:
    - Set up an AI harness or agent workflow and use it throughout the development for this take-home exercise.
    - Briefly document the tools and setup used (e.g., LLMs, coding assistants, agentic frameworks, configuration).
    - Ensure your AI setup is practical and reproducible.
    - Include any custom rules, system prompts, or agent configurations used.
    - Include a brief summary of which parts of the solution were AI-assisted and why.

#### Important Guidelines
- The existing `TimeDepositCalculator.updateBalance` method is functioning correctly. Ensure its behavior remains unchanged after refactoring.
- The final solution must include **exactly two API endpoints**. Do not develop additional endpoints.
- **Do not** create a pull request or a new branch in the ikigai-digital repository. Instead, fork the repository into your own GitHub repository and develop the solution there.
- Handling invalid input or exceptions is not required.
- Use any tools, frameworks, or libraries you find suitable.
- In case of ambiguity, make logical assumptions and justify them in code comments.

#### Preferred Stack
- Use an OpenAPI Swagger contract.
- Embrace Hexagonal Architecture.
- Follow atomic commit practices.
- Utilize testcontainers.
- Leverage AI-assisted development tools for code generation, testing, and refactoring.

#### Submission Instructions
- Provide clear instructions on how to trigger the endpoints using the Swagger contract.
- Email the link to your public GitHub repository.