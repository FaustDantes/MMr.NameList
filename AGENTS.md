# AGENTS.md

Instructions for AI coding agents that work on this repository. Humans can read it too.

## Project

MMr.NameList is a simple Android app to remember people. Kotlin, Jetpack Compose, Room. All data stays on the device.

## Read first, every session

1. [docs/progress.md](docs/progress.md): where the work stands.
2. [docs/milestones.md](docs/milestones.md): the current milestone and its "done when" criteria.
3. [docs/architecture.md](docs/architecture.md): stack, layers, data model, platform decisions.
4. [docs/design.md](docs/design.md): visual rules.
5. [docs/decisions.md](docs/decisions.md): decisions already made. Do not reopen them without the owner.
6. [docs/requirements.md](docs/requirements.md): feature IDs (F1 to F13, N1 to N5).

If two documents disagree, stop and ask the owner.

## Commands

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Unit and UI tests | `./gradlew testDebugUnitTest` |
| Record screenshots | `./gradlew recordRoborazziDebug` |
| Verify screenshots | `./gradlew verifyRoborazziDebug` |
| Lint | `./gradlew lintDebug` |

## Reading CI results

- Logs: `gh api repos/FaustDantes/MMr.NameList/actions/jobs/<job-id>/logs`. Test failures print in full.
- Screenshots of the latest CI run: `git fetch origin ci-screenshots && git checkout origin/ci-screenshots -- .` in a scratch folder. CI force-pushes this branch on each run.
- Copy the approved screenshots to `docs/screenshots/m<N>/` before the review tag.

## Publishing a release

Tags cannot be pushed from the agent sandbox. Run the workflow instead; it creates the tag and the release:

`gh workflow run android.yml --ref dev -f release_tag=m<N>-rc<K>`

A tag pushed by a human (`git push origin m<N>-rc<K>`) also works.

## Rules

1. Work on one milestone at a time. Do not start the next milestone before the owner approves.
2. Work on the `dev` branch. Never push directly to `master` after M0. See [ADR-007](docs/decisions.md#adr-007-branches).
3. Write or update tests in the same commit as the code.
4. Before a report, run build, all tests and screenshot tests. Report only what these runs prove. Never say "done" without evidence.
5. Record each non-obvious decision in `docs/decisions.md` as a new ADR.
6. Do not add a dependency without an ADR line that gives the reason.
7. Use the latest stable library versions. Verify each version on the official release page. Do not use versions from memory.
8. Keep commits small. Use Conventional Commits: `feat:`, `fix:`, `test:`, `docs:`, `build:`, `ci:`, `refactor:`.
9. Never commit secrets, unencrypted keystores or real personal data. The only keystore in the repo is `signing/release.jks.enc` (ADR-008). Use fictional seed data in tests and screenshots.
10. If a requirement is not clear, ask the owner. Do not guess silently.
11. Update `docs/progress.md` at the end of each session.
12. If you change a rule in this file, tell the owner in the milestone report.

## Code style

- Kotlin official style. `ktlint` or the Android lint defaults.
- One ViewModel per screen. ViewModels expose `StateFlow<UiState>`.
- No business logic in Composables.
- Store enums by name in Room, never by ordinal.
- No `!!` in production code.
- All user-visible text in `strings.xml`.

## Milestone report

At the end of each milestone, send the owner a report. Use [docs/templates/milestone-report.md](docs/templates/milestone-report.md). Attach the screenshots and the APK link. Then stop and wait.
