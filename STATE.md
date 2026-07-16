# Loop State

## Last Run

- **Timestamp:** 2026-07-17T09:25:00Z
- **Status:** action-taken (re-reviewed #1965 commits 6-8, moved #1968 and #1947 to Resolved)
- **PRs reviewed:** 1
- **Reviews posted:** 1
- **False positives caught by verifier:** 0

## Reviewed PRs

| # | Title | Author | Verdict | Reviewed At |
|---|-------|--------|---------|-------------|
| #1965 | Use custom doclet to extract configuration metadata | kwin | COMMENT (commits 6-8: 1 important finding — wrong tag name in error messages, 1 minor stale javadoc) | 2026-07-17T09:25:00Z |
| #1815 | Advised and Enforced Management | cstamas | COMMENT (approve, 1 minor observation) | 2026-07-14T20:49:20Z |
| #1912 | CI: Use Java 26 | cstamas | COMMENT (2 confirmed blockers) | 2026-07-14T20:49:30Z |
| #1683 | Fix version comparison with case insensitive lexical order | sultan | COMMENT (2 blocking + 3 minor findings) | 2026-07-14T20:49:40Z |
| #1714 | Edit version comparison | elharo | COMMENT (2 confirmed findings) | 2026-07-14T20:26:21Z |
| #1758 | Add CycleAwareDependencyGraphDumper | Jimin15 | COMMENT (6 confirmed findings) | 2026-07-14T20:26:34Z |
| #1955 | Expose body for non RFC 9457 error responses | kwin | COMMENT (issues found) | 2026-07-13T11:59:36Z |

## Skipped PRs

| # | Title | Author | Reason |
|---|-------|--------|--------|
| #1956 | Bump bouncycastleVersion from 1.84 to 1.85 | dependabot | Bot + dependencies label |

## Review Queue

| # | Title | Author | Priority |
|---|-------|--------|----------|
| #63 | [MRESOLVER-126] support conversion from String to Map | kwin | Deferred (very old, 2020) |
| #17 | [MRESOLVER-43] Enhancements to the public API | ChristianSchulte | Deferred (very old, 2018) |

## Escalated (human required)

_(none)_

## Resolved (7-day rolling window)

| # | Title | Author | Resolution |
|---|-------|--------|------------|
| #1968 | fix: Some since javadoc tags were off; fixed | cstamas | Merged 2026-07-16T17:18:04Z by cstamas (our approve was correct, despite kwin's 2.1.0 suggestion) |
| #1947 | Use try-with-resources in AbstractTransporter | Aayush10016 | Merged 2026-07-16T17:07:10Z by cstamas (our approve was correct) |
| #1966 | Feat: new (limited) transport | cstamas | Merged 2026-07-16T16:56:39Z by cstamas (our approve across 7 review rounds was correct) |
| #1967 | Fix reporting of HTTP/3 in JdkTransporter | kwin | Merged 2026-07-16T15:57:27Z by kwin (our approve was correct) |
| #1762 | Expose additional transport details to TransportListener | kwin | Merged 2026-07-16T15:34:42Z by kwin (our approve was correct, cstamas also approved) |
| #1949 | Support HTTP/3 in Jetty and JRE HTTP Client | kwin | Merged 2026-07-16T15:30:22Z by kwin (our approve was correct, cstamas also approved) |
| #1957 | Skip validation and decoration on re-entrant RepositorySystem calls | gnodet | Merged 2026-07-14T20:04:12Z (our approve was correct) |
