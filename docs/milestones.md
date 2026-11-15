# Milestones

Each milestone ends with:

1. Green CI: build, unit tests, screenshot tests.
2. Screenshots in light and dark mode in `docs/screenshots/m<N>/`.
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
| M4 | Origin and place | F10, F11 | Country of origin. Place of meeting: country, city, or current location. The app works when location permission is denied. |
| M5 | Relationships | F13 | Add, edit and delete links. The reverse link shows on the other person. Delete a person removes the links. Inverse tests pass for all types. |
| M6 | Reminders | F8 | Create a reminder with a message and a time. The notification shows. Alarms survive a reboot. |
| M7 | Backup and polish | F9 (switch), F12 | Export and import a zip. Settings screen. Theme switch. Accessibility check. Final screenshot set. |

The app is useful after M1. The owner can stop after any milestone.

## Screenshots

- Use Roborazzi with Robolectric. Screenshots run on the JVM in CI. No emulator.
- One test class per screen. Fixed seed data with fictional names. Fixed clock.
- Device size: standard phone, 411 x 891 dp. Light and dark.
- Save to `docs/screenshots/m<N>/` and attach to the pre-release.
- From M4: also ask the owner for one screenshot from the real phone. JVM screenshots do not show permission and camera problems.

## Progress

The agent records the status in [progress.md](progress.md).
