# Progress

The agent updates this file at the end of each session. Newest entry first.

## Status

| Milestone | Status | Tag | Date |
|---|---|---|---|
| M0 Skeleton and CI | approved | m0 | 2026-11-15 |
| M1 People basics | approved | m1-rc1 | 2026-11-15 |
| M2 Photos | approved | m2-rc1 | 2026-11-15 |
| M3 Tabs and ordering | approved | m3-rc1 | 2026-11-15 |
| M4 Place of meeting | approved | m7 | 2026-11-15 |
| M5 Relationships | approved | m7 | 2026-11-15 |
| ~~M6 Reminders~~ | removed | - | - |
| M7 Backup and polish | approved | m7 | 2026-11-15 |
| M8 Multi-select and colors | ready for review | m8-rc1 | 2026-11-15 |

## Log

### 2026-11-15 (M4, M5, M7, one review at the end, by owner request)

- Owner: everything square, icon-only add buttons, M6 and F11 removed, M4 simplified, no screenshots (ADR-018, ADR-019).
- M4: "City, state" field and GPS button; coordinates only, no lookup; "Open in maps". ADR-020.
- M5: two-way relationships, schema v3 (auto migration, tested from v1 and v2). ADR-021.
- M7: settings (theme, backup, about), zip backup with full check before import and safe photo names. ADR-022.
- Tests: 71 pass in CI. Lint passes.
- Check on the phone: GPS button (permission dialog, approximate location), add-relationship sheet, export to a file and import it back, tab add and rename dialogs.

### 2026-11-15 (M1 to M3, one review at the end, by owner request)

- Owner approved M0 and asked for M1, M2 and M3 without stops, with one report at the end, and for a square design (ADR-010).
- M1: Room (schema v1), "Me" row, add, edit, delete, search without case and diacritics, manual or language-aware A-Z sort, detail and edit screens, type-safe navigation. ADR-011 to ADR-013.
- M2: photos from camera (intent + FileProvider, no CAMERA permission) and gallery (Photo Picker), copied upright (EXIF) and scaled to 1024 px; unused photo files deleted. ADR-014.
- M3: tabs (schema v2, auto migration tested from v1), tab chips, tab manager with drag handles, tab in edit and detail, long-press drag and drop in manual order. ADR-015 to ADR-017.
- Tests: 55 pass in CI (repository, migration, photo store, UI, 17 screenshots).
- Bugs found by tests: photo import always failed (bounds decode returned null by design); M3 buttons were round (Material 3 default ignores theme shapes).
- Open risk: the add and rename tab dialogs are tested only as a form; Robolectric cannot test a text field inside a dialog window. Check on the phone.

### 2026-11-15 (M0)

- Android project: Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, compile and target SDK 37, min SDK 26.
- People screen with large title, search field, empty state and add button (shows a "next update" message).
- Material 3 theme: dynamic color on Android 12+, teal and sand fallback. Adaptive and themed launcher icon.
- Tests: 3 UI tests and 2 screenshot tests pass in CI. Lint passes.
- CI: build, tests, screenshots, lint on each push. Screenshots go to the `ci-screenshots` branch. Signed release on tags.
- Signing: encrypted keystore in `signing/` (ADR-008). Release needs the `SIGNING_PASSWORD` secret.
- Release: [m0-rc1](https://github.com/FaustDantes/MMr.NameList/releases/tag/m0-rc1), pre-release, APK 8.3 MB, signed with the release key (v2 scheme, certificate checked in CI).
- Publishing: tags cannot be pushed from the agent sandbox; the workflow creates them (`release_tag` input, AGENTS.md).
- Findings: current Compose needs compile SDK 37. The FAB label is not merged into the button semantics; tests click it by test tag.

### 2026-11-15

- Repository set up with requirements, architecture, design, milestones and decisions.
- Next step: start M0 on the `dev` branch.
