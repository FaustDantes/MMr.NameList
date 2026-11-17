# Milestones

Each milestone ends with:

1. Green CI: build, unit and UI tests, lint.
2. ~~Screenshots~~: stopped after M3 by the owner (ADR-019). Old ones stay in `docs/screenshots/m0` to `m3`.
3. A signed APK on a GitHub pre-release with tag `m<N>-rc<K>` on the `dev` branch.
4. A report to the owner. Use [templates/milestone-report.md](templates/milestone-report.md).
5. A stop. Do not start the next milestone before the owner approves.
6. After approval: merge `dev` into `master`, tag `m<N>`. See [ADR-007](decisions.md#adr-007-branches).

## Plan

| # | Milestone | Features | Done when |
|---|---|---|---|
| M0 | Skeleton and CI | - | The app starts and shows an empty People screen with the theme. CI builds, tests, makes screenshots and publishes a signed APK. |
| M1 | People basics | F1, F6, F7, F9 (system theme) | Add, edit and delete a person (name, notes). All-people list. Search while typing. Sort A to Z. "Me" exists. Data survives an app restart. |
| M2 | Photos | F2, F3 | Camera and gallery photos. Photos copied and scaled. Initials avatar when there is no photo. |
| M3 | Tabs and ordering | F4, F5 | Create, rename, delete and reorder tabs. Move a person to a tab. Drag and drop in the list. |
| M4 | Place of meeting | F10 | One "City, state" field and a GPS button that stores coordinates. The app works when location permission is denied. |
| M5 | Relationships | F13 | Add, edit and delete links. The reverse link shows on the other person. Delete a person removes the links. Inverse tests pass for all types. |
| ~~M6~~ | ~~Reminders~~ | ~~F8~~ | Removed by the owner (ADR-019). |
| M7 | Backup and polish | F9 (switch), F12 | Export and import a zip ([format](backup-format.md)). Settings screen. Theme switch. Accessibility check. |
| M8 | Multi-select and colors | F14 | Long press selects; tap adds; bulk delete with confirmation; bulk move to a tab; drag by handle. White, black and light blue palette (ADR-023). |
| M9 | World map | F15 | Map screen with country borders, pan and zoom; initials circles for people with GPS, clustered when close; tap opens the person; small map on the detail screen. |
| M10 | First meeting | F16 | Date of first meeting (empty = today), picker without future dates, shown on the detail screen and in the backup; the add button focuses the Name field. Release 1.1.0. |
| M10 | First meeting | F16 | Date of first meeting (empty = today), picker without future dates, shown on the detail screen and in the backup; the add button focuses the Name field. Part of release 1.1.0. |
| M11 | Backup password | F17 | Password in settings (set, change with old, remove, reset); export asks to encrypt with "do not show again"; default in settings; AES-256-GCM backup; import asks for the password. |
| M12 | Archive | F18 | Archive selected people with a time stamp; archive screen with time in the archive; restore and delete. |
| M13 | More details | F19 | Native language, date of birth, education, current work, ask about, last updated; collapsed "More details" section; only icon, name and tab on top. |
| M14 | Widget | F20 | Resizable home-screen widget (1 x 1, 4 x 1, 4 x 2) with a navy add button that opens the quick-add form; saving returns to the home screen. |
| M15 | Big add button | F21 | Navy 112 dp add button at the bottom center of the people list. Release 1.2.0. |
| M16 | Same name, spark, search, languages | F22 to F27 | Duplicate-name dialog, Conversation Spark, search by any field and date, version and report, five languages with a drop-down, 88 dp add button. Release 1.2.0. |
| M17 | Many tabs per person | F28 | A person can be in several tabs; migration 5 to 6; backups carry all tabs. Release 1.3.0. |
| M19 | Line design | F32, F33, F34 | Light and dark Line theme, photos only, no initials, bundled fonts, black widgets. Release 1.3.0. |
| M18 | Tags and relationship types | F29, F30, F31 | Tabs are tags with unique names; relationship types are edited in Settings; tag drop-down in the edit form; migration 6 to 7. Release 1.3.0. |

The app is useful after M1. The owner can stop after any milestone.

## Screenshots

Stopped after M3 by the owner (ADR-019). Do not add screenshot tests.

## Progress

The agent records the status in [progress.md](progress.md).
