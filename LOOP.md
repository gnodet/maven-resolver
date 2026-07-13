# Maven Resolver PR Review Loop

## Configuration

| Setting | Value |
|---------|-------|
| Pattern | pr-review-loop |
| Cadence | 1 hour |
| Level | L2 (assisted) |
| Max PRs per run | 3 |
| State branch | pr-review-loop-state |

## Skills Used

- forgebot-loop-core (init, loop-guard, loop-budget, loop-constraints)
- forgebot-review-loop (review-loop, pr-review-triage, learnings)

## How to Run

```
/loop 1h /forgebot-review-loop
```

## Notes

- Reviews are posted as comments on the PR
- Uses maker/checker pattern: one agent reviews, another verifies findings
- Thread safety and API compatibility are critical review dimensions for this project
- Check backward compatibility of any public API changes
