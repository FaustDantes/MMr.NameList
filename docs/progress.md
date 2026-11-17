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
| M8 Multi-select and colors | approved | v1.0.0 | 2026-11-15 |
| M9 World map | approved | v1.0.0 | 2026-11-15 |
| M10 to M13 First meeting, backup password, archive, more details | approved | v1.1.0 | 2026-11-15 |
| M14 Widget, M15 Big add button | approved | v1.2.0 | 2026-11-15 |
| M16 to M19 Same name, tags, Line design, welcome | approved | v1.3.0 | 2026-11-17 |
| M20 Learn names | review | v1.4.0-rc1 | 2026-11-17 |

## Log

### 2026-11-17 (M20, F35 learn names, owner request)

- Owner asked for learning with flash cards and approved the plan with these answers: people without spark, place and notes are left out; results are saved and shown with a time and score chart; the front shows tags, spark, place, and notes at the bottom; 10 cards in random order.
- ADR-042: book button bottom left; rounds of up to 10 random people; tap turns the card to the name; "I knew it" or "I did not know" (off until the turn); table `learn_round`, schema 8, auto migration 7 to 8; result with score, change against the last round, best score, Canvas line chart in percent, people to learn again.
- Decisions I made without asking: no photo on the front; the chart's left axis is percent, because a round can have fewer than 10 cards; a round left before the end is not saved; learning results are not in the backup.
- CI problem found and fixed: after the two learning screenshots were added, the test process sometimes stopped at the first screenshot ("JniConstants: Class not found: java/nio/..."), 4 of 6 runs. A per-test log showed `ScreenshotsTest > personDetail` each time. Fix: the screenshot test decodes one image on the test thread before the image loader starts its own threads. After the fix, 3 of 3 runs passed. A 2 GB heap and one graphics mode for all tests did not help, so they were removed. Root cause is not proven; the fix is based on the evidence above.
- Tests: 266 pass in CI. Build and lint pass. README screenshots 07 and 08 added.
- Check on the phone: the book button beside the add button; the turn animation; a round with fewer than 10 people; the chart after a few rounds on different days; the empty message when nobody has details.

### 2026-11-17 (release 1.3.0)

- Owner approved 1.3.0 ("it works well, let's release it"). Release date set in CHANGELOG.md. `master` is fast-forwarded to `dev`, then the workflow builds and publishes `v1.3.0`.
- AGENTS.md rule change (rule 12): the line about screenshots now says that README screenshots are allowed (ADR-040). Per-milestone screenshots stay stopped (ADR-019).
- Owner asked what an iOS version needs. Answer given in chat. No decision made. ADR-001 stays open only if the owner asks for iOS.

### 2026-11-17 (1.3.0, F34, owner request)

- ADR-041: Archive first in Settings, Lists; one-time welcome dialog with four points, closed only by "Get started". Example people were asked for and then withdrawn by the owner; nothing of them is in the code.
- Check on the phone: the welcome on the first start (also after an update), that Back and tapping outside do not close it, that it does not come back after a restart.

### 2026-11-17 (1.3.0, F33, owner request)

- ADR-039: only Settings in the people list top bar; "N people · order" line under the title; archive in Settings, Lists; map only from a person's place (owner's canvas comment); tags edited only in Settings.
- New "Line list" app icon (option 4); widgets show the icon and app name, white or black with the phone's mode.
- Check on the phone: the new icon on the home screen and in the app drawer (also with themed icons on); the three widget sizes in light and dark mode; sort from the new line; open the archive from Settings; open the map from a person.

### 2026-11-17 (1.3.0, F32, owner request)

- ADR-038: Line design from the design canvas, light (default) and dark. Photos only, no initials anywhere. Space Grotesk and Inter bundled (SIL OFL, licenses/).
- Decisions I made without asking: the add button uses the accent color (the canvas showed it so); the people list keeps its top-bar icons (map, archive, tags, sort, settings); the map shows people as small squares without initials.
- Check on the phone: light and dark theme on each screen; a person with and without a photo; the widget in all three sizes.

### 2026-11-16 (1.3.0, F29 to F31, owner request)

- ADR-037: tabs are tags with unique names (case ignored); relationship types are a list in Settings; tag drop-down with a plus button in the edit form. Database 6 to 7, hand-written migration, tested.
- Decisions I made without asking: "Other", "Parent" and "Child" cannot be deleted; deleting a type turns its links into "Other"; types you make read the same from both sides; code and tables keep the name "tab".
- Check on the phone: update from rc2 and see that relationships keep their type; add a type, rename "Friend", delete a type in use; try a tag name in other capitals; pick two tags in the drop-down.

### 2026-11-16 (1.3.0, F28, owner request)

- ADR-036: add a tab from the edit form; German and Polish removed. I read "the tabs should be also localised" as: the new add-tab control must be translated; it reuses existing strings, so it is in Slovak and Czech. Tell me if you meant something else, such as tab names.
- F28 and ADR-035: many tabs per person. Database 5 to 6 with a data-copying auto migration.
- Decision I made without asking: "Move to tab" in multi-select replaces all tabs with the chosen one. Say if you want "add to tab" as a second action.
- Check on the phone: update from 1.2.0 and see that people keep their tab; give one person two tabs; open each tab; export and import a backup.

### 2026-11-15 (1.2.0, F22 to F27, owner request)

- F22 to F27 in ADR-033 and ADR-034. Review builds v1.2.0-rc2 and rc3.
- Decisions I made without asking: search treats "any field" as all text fields plus all dates; the widget follows the system language; the translations are mine and need a native check; the add button is 88 dp.

### 2026-11-15 (1.2.0, F20 and F21, owner request)

- F20 (ADR-031): home-screen widget with RemoteViews, resizable between 1 x 1, 4 x 1 and 4 x 2, as designed (navy add button). The add button opens the quick-add form; saving or closing returns to the home screen. The widget refreshes when the app closes.
- F21 (ADR-032): the add button on the people list is navy, 112 dp, bottom center.
- Decisions I made without asking (change them if wrong): navy applies to the add buttons only, not all buttons; the widget shows names on 4 x 1 and initials on 4 x 2; the widget is not scrollable (the owner kept the original design).
- Design: https://claude.ai/artifact/6LkuXr71Z35rbfT1Jf55Az (light blue in the board; the app uses navy as asked).
- AGENTS.md rule 12: only the feature range changed (F1 to F21).
- Check on the phone: add the widget, resize it, tap the button, save, widget refresh, the new button position.

### 2026-11-15 (version 1.0.0)

- Owner approved M8 and M9 and asked for version 1, merged into master.
- Tests added for the most used features: PersonEditViewModel, PersonDetailViewModel, and AppFlowTest (the real app: add, search, open, edit, delete; long press and bulk delete). 102 tests pass.
- Released as v1.0.0 from master (ADR-026). Changes: CHANGELOG.md.

### 2026-11-15 (M8, M9)

- M8: white, black and light blue palette, Light by default (ADR-023); multi-select: long press, bulk delete, bulk move to tab; drag by handle (ADR-024).
- M9: offline world map from Natural Earth 1:110m borders (80 KB asset, tools/map/); Mercator; pan, pinch, double tap; initials circles, clusters; mini map on detail (ADR-025).
- Check on the phone: long press and selection, drag handle, map gestures and marker taps.

### 2026-11-15 (1.1.0, F17 to F19, owner request)

- F17 (ADR-028): backup password in settings (check value only; set, change with old, remove, reset); export asks to encrypt with "do not show again"; default ask, always or never in settings; AES-256-GCM streaming with Tink 1.24.0 and PBKDF2 (600,000 iterations); import asks for the password. Format in `docs/backup-format.md`.
- F18 (ADR-029): archive selected people; archive screen with date and time in the archive; restore all or one; hidden from list, search, map and relationship picker.
- F19 (ADR-030): native language, date of birth, education, current work, ask about, last updated; collapsed "More details" section; schema 5.
- Tests: 202 pass in CI (new: crypto, password store, settings ViewModel, archive repository and screen, more details, migration 4 to 5). Lint passes.
- Decisions I made without asking (change them if wrong): the password is typed again for each encrypted export; "note how long" means the computed time in the archive; the details section is always collapsed when the form opens.
- Not done, waiting for the owner: the signing key (finding 1 of the security scan) and a cleaned repo history.
- Check on the phone: the encrypt question and password dialogs, export to a file and import it back with the password, archive and restore, the collapsed section and date pickers.

### 2026-11-15 (1.1.0, F16, owner request)

- Date of first meeting (`firstMetOn`, schema 4, ADR-027); empty means today; picker without future dates; shown on the detail screen; in the backup as an optional field.
- New person: the Name field has the focus and the keyboard opens. Name stays the only required field.
- Tests added for the repository (zone and defaults), migration 3 to 4, backup (with and without the field), edit ViewModel, screens (focus, date field, detail) and date helpers.
- Schema `4.json` committed. CI: 121 tests and lint pass. Check the date picker and the keyboard on the phone.

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
