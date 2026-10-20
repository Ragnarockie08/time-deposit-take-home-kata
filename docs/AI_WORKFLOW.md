# AI-assisted workflow

## Tools and setup

- **Claude Code** (CLI) as the agent harness. The main session runs Claude Opus 5.5 and acts as the lead.
- **Project rules** for every agent: [`CLAUDE.md`](../CLAUDE.md) at the repository root (hard constraints from
  the task, architecture, git and testing rules, commands).
- **Sub-agents** defined in [`.claude/agents/`](../.claude/agents):

| Agent | Model | Role | Tools |
|---|---|---|---|
| lead (main session) | Opus 5.5 | diagnosis, plan, tickets with file ownership, shared interfaces, merging | all |
| [`researcher`](../.claude/agents/researcher.md) | Sonnet 5.5 | checks library versions and compatibility against official sources | web, read-only files |
| [`implementer`](../.claude/agents/implementer.md) | Sonnet 5.5 | builds one ticket in its own git worktree, only the files it owns | all |
| [`reviewer`](../.claude/agents/reviewer.md) | Opus 5.5 | read-only review of one diff against its ticket before it lands | read-only |

The owner (the developer submitting this) made the design decisions and corrected the agents along the way;
the agents did the research, wrote the code and reviewed each other's diffs.

### Reproducing it

1. Install Claude Code and start `claude` in the repository root. `CLAUDE.md` and `.claude/agents/` are picked
   up automatically.
2. Ask the lead to plan a change. It writes tickets that name the files each implementer owns and the
   interfaces it must keep, starts implementers with worktree isolation, and sends each diff to the reviewer
   before it lands on the step's branch.

## How the work went

1. **Read and report.** The lead read the task and the starter code and reported the state before planning.
2. **Plan with the owner.** The first plan (decorators, ArchUnit, four parallel implementers) was rejected as
   too heavy for the task. The second, lean hexagonal plan was approved. The owner then walked through the
   business meaning of the plans, the hexagonal layout, testing and concurrency before any code was written.
3. **Findings before code.** A sweep over every balance from 0.01 to 1,000,000.00 showed the original `double`
   arithmetic under-pays at half cents. The owner chose to keep the refactor behaviour-identical and fix
   money in a separate, revertible commit. Every trade-off went into [DESIGN_NOTES.md](DESIGN_NOTES.md).
4. **Behaviour first (lead).** Pinning tests against the original calculator, then the Strategy refactor.
   A scratch comparison over 10 million random inputs confirmed bit-identical results; the reviewer
   approved and one finding (an empty registry map broke the constructor) was fixed.
5. **Stack check (researcher).** Verified current versions: Spring Boot 3.5 had left OSS support, so the
   owner chose Boot 4.1. The researcher mapped the Boot 4 changes (renamed starters, Testcontainers 2
   artifacts, Jackson 3 options for the OpenAPI generator).
6. **Parallel implementation.** The lead wrote the build, contract and ports; two implementers then built
   the persistence adapter and the REST/service layer in separate worktrees at the same time.
7. **Review.** Both diffs were approved by the reviewer. Findings that were applied: a test proving the
   advisory lock is held until commit, one consistent snapshot for the listing, clearer errors, and design
   notes for the rest.
8. **Money fix and docs (lead).** Decimal arithmetic in its own commit, checked against exact decimal
   arithmetic over 300 million cases; then this README and these notes.

### Corrections from the owner during the work

- Plans must be sized to the task: the first plan was cut down.
- Commits went on `main` at first; they were moved to one branch per step (GitHub flow), and the rule was
  added to `CLAUDE.md`.
- Tests must explain themselves through their names, with no comments; the pinning tests were rewritten that
  way and the rule was added to `CLAUDE.md`.
- The application service moved to `application/service` to match the common hexagonal layout.

## What was AI-assisted, and why

| Part | AI role | Why |
|---|---|---|
| Analysis of the starter and the money bug | lead found and measured it; owner decided how to handle it | fast, exhaustive checking (a 100-million-balance sweep) that is impractical by hand |
| Plan and design | lead proposed; owner reshaped (scope, branches, enum plan types, Boot 4) | quick iterations on options, with the owner keeping the decisions |
| Calculator refactor and pinning tests | lead wrote; reviewer verified | the most critical part, so it stayed with the strongest model plus an independent review |
| Library versions | researcher | facts that change over time had to come from current official sources |
| Persistence and REST layers | two implementers in parallel; reviewer before merge | independent pieces with clear contracts, so parallel work was safe |
| Tests | written with each piece; integration tests run against real PostgreSQL | every claim of "done" is backed by a test run, not by the agent's word |
| Documentation | lead drafted from the decisions made in the session | keeps assumptions and trade-offs written down as they happen |

The code was written by agents. The reviewer agent checked the calculator refactor, both implementer
branches and the money fix before they landed. Agents
only committed locally; every push, pull request and merge was done by the owner.
