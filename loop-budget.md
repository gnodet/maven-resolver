# Loop Budget -- Maven Resolver PR Review

## Daily limits

| Loop | Max runs/day | Max tokens/day | Max sub-agent spawns/run |
|------|--------------|----------------|--------------------------|
| pr-review-loop | 24 | 10000000 | 6 |

## On budget exceed

1. Switch to report-only mode
2. Append event to `loop-run-log.md`
3. Log warning in STATE.md Escalated section

## Kill switch

- Set `loop-pause-all` flag in STATE.md to halt all loop activity
- Resume only after human clears the flag
