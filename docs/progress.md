# Progress

The agent updates this file at the end of each session. Newest entry first.

## Status

| Milestone | Status | Tag | Date |
|---|---|---|---|
| M0 Skeleton and CI | ready for review | m0-rc1 | 2026-11-15 |
| M1 People basics | not started | - | - |
| M2 Photos | not started | - | - |
| M3 Tabs and ordering | not started | - | - |
| M4 Origin and place | not started | - | - |
| M5 Relationships | not started | - | - |
| M6 Reminders | not started | - | - |
| M7 Backup and polish | not started | - | - |

## Log

### 2026-11-15 (M0)

- Android project: Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, compile and target SDK 37, min SDK 26.
- People screen with large title, search field, empty state and add button (shows a "next update" message).
- Material 3 theme: dynamic color on Android 12+, teal and sand fallback. Adaptive and themed launcher icon.
- Tests: 3 UI tests and 2 screenshot tests pass in CI. Lint passes.
- CI: build, tests, screenshots, lint on each push. Screenshots go to the `ci-screenshots` branch. Signed release on tags.
- Signing: encrypted keystore in `signing/` (ADR-008). Release needs the `SIGNING_PASSWORD` secret.
- Findings: current Compose needs compile SDK 37. The FAB label is not merged into the button semantics; tests click it by test tag.

### 2026-11-15

- Repository set up with requirements, architecture, design, milestones and decisions.
- Next step: start M0 on the `dev` branch.
