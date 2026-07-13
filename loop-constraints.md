# Loop Constraints -- Maven Resolver PR Review

## Push & Merge

- NEVER merge or close any PR
- NEVER label or assign any PR or issue
- NEVER push code (review-only loop)
- NEVER force-push to any branch
- NEVER approve PRs automatically — post comments only

## Scope

- Max 3 PRs per run
- No size limit on PRs to review
- Skip PRs authored by bots (dependabot, renovate) unless they touch Java source

## Review Quality

- Always verify findings with a second agent (maker/checker pattern)
- Always check git history for context before commenting
- Review against project rules in `.oss-ai-helper-rules/`
- Focus on: correctness, thread safety, API compatibility, performance
- Do NOT comment on style-only issues if checkstyle passes
- Do NOT request changes for trivial matters

## Paths

- Ignore generated code styling
- Ignore `.github/`, `.asf.yaml` changes (infra files)
- Pay extra attention to API module (`maven-resolver-api`) changes

## Communication

- AI attribution required in all review comments
- Be respectful and constructive (Apache community standards)
- Acknowledge good patterns, not just problems

## Budget

- 80% budget threshold -> report-only mode
- Kill switch via `loop-pause-all` in STATE.md
- Exit early if no PRs need review (< 5k tokens)
