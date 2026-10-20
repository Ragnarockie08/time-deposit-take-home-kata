# AI-assisted workflow

## How I work with agents

I use AI agents the way I would use a small team: I own the design decisions and the final review, and the
agents do focused pieces of work within rules I set.

1. **Architecture first.** I propose the architecture; the lead agent analyses it from different angles
   (edge cases, concurrency, money precision, testability) and points out risks before any code is written.
2. **Small, owned tickets.** Each piece of work has a clear goal, the files it may touch and the interfaces it must
   keep. Independent pieces can run in parallel.
3. **Proof, not claims.** A task is done only when its tests pass. A separate reviewer agent checks risky changes
   (here: the calculator refactor, the persistence and REST layers, the money fix) before they land.
4. **I review and merge.** Every step goes through its own branch and pull request.

## Setup

- **Tool:** Claude Code (CLI).
- **Project rules** for every agent: [`CLAUDE.md`](../CLAUDE.md). It holds the constraints from the task,
  the architecture, and the testing, commenting and git rules.
- **Agent definitions** in [`.claude/agents/`](../.claude/agents):

| Agent | Model | Role |
|---|---|---|
| lead (main session) | Claude Opus | analyses my design, splits it into tickets, integrates |
| [`researcher`](../.claude/agents/researcher.md) | Claude Sonnet | checks library versions and compatibility against official sources |
| [`implementer`](../.claude/agents/implementer.md) | Claude Sonnet | builds one ticket in its own git worktree |
| [`reviewer`](../.claude/agents/reviewer.md) | Claude Opus | read-only review of a diff against its ticket |

To reproduce: install Claude Code and run `claude` in the repository root; `CLAUDE.md` and `.claude/agents/`
are picked up automatically.

## What was AI-assisted, and why

| Part | How AI helped | Why |
|---|---|---|
| Analysing the starter | measured the effect of `double` arithmetic over a large range of balances | exhaustive checking is impractical by hand |
| Library versions | researcher verified current Spring Boot, Testcontainers and OpenAPI generator versions | versions change; they must come from current sources |
| Code and tests | agents drafted code and tests for each ticket, within the agreed design | speed, with tests as the safety net |
| Reviews | reviewer agent checked the critical diffs against their tickets | a second, independent look at the risky parts |
| Documentation | drafted from the decisions made during the work | keeps assumptions and trade-offs written down |
