# AI-assisted workflow

> Work in progress: completed at the end of the implementation with what was AI-assisted and why.

## Tools
- **Claude Code** (CLI) as the agent harness, main session on Claude Opus 5.5.
- Sub-agents defined in `.claude/agents/`:

| Agent | Model | Role |
|---|---|---|
| lead (main session) | Opus 5.5 | diagnosis, plan, tickets with file ownership, shared interfaces, merging |
| `researcher` | Sonnet 5.5 | verifies library versions and compatibility against official docs |
| `implementer` | Sonnet 5.5 | builds one ticket in its own git worktree, only the files it owns |
| `reviewer` | Opus 5.5 | read-only review of each diff against its ticket before it lands |

- Project rules for every agent: `CLAUDE.md` at the repository root.

## Flow
1. Read the task and the starter code; report status.
2. Plan with the owner (several rounds; the first plan was cut down as over-engineered).
3. Walk through the design before coding: business meaning of the plans, hexagonal layout, testing,
   concurrency. Every trade-off found is recorded in `docs/DESIGN_NOTES.md`.
4. Implement in atomic commits: pinning tests first, then the refactor, then the API around it.
5. Reviewer pass on every calculator change and every implementer diff.

## Reproducing the setup
Install Claude Code, open the repository root and start `claude`. `CLAUDE.md` and `.claude/agents/` are
picked up automatically.
