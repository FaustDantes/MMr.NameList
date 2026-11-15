# Architecture

Status: approved by the owner on 2026-11-15.
Requirements: [requirements.md](requirements.md). Decisions log: [decisions.md](decisions.md).

## 1. Goals

- Build the simplest Android app that meets the requirements.
- Make it look clean, calm and modern.
- Build in small milestones. See [milestones.md](milestones.md).
- Do not require Android Studio. The app builds from the command line and in GitHub Actions.

## 2. Technology decision

| Option | For | Against |
|---|---|---|
| **Kotlin + Jetpack Compose (chosen)** | Native access to camera, location, notifications and local storage. One language. Strong screenshot-test tools. | Android only. |
| Flutter | One codebase for a later iOS version. | Extra layer for native features. Not necessary while Android is the only target. |
| Installable web app (PWA) | Smallest code. No APK. | Scheduled notifications are not reliable. The browser can delete local data. Fails F8 and F12. |

Decision: Kotlin + Jetpack Compose, one Gradle module (`:app`). Open this decision again only if the owner asks for iOS. See [ADR-001](decisions.md#adr-001-kotlin-and-jetpack-compose).

## 3. Stack

Use the latest stable version of each library at build time. Do not use versions from memory. Check the official release page and record the version in `gradle/libs.versions.toml`.

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose, type-safe routes |
| Storage | Room (SQLite). Schema export ON. Each schema change has a tested migration. |
| Images | Coil |
| Settings | DataStore (theme only) |
| Dependency injection | Manual: one `AppContainer` class. No Hilt. See [ADR-002](decisions.md#adr-002-manual-dependency-injection). |
| Tests | JUnit, Room in-memory tests, Compose UI tests on Robolectric, Roborazzi screenshots |
| SDK levels | Min SDK 26. Target SDK: latest stable. |

## 4. Layers

Use three layers. Do not add a domain layer unless two or more screens share a use case.

1. **UI**: Compose screens. One ViewModel per screen. State flows in one direction: the ViewModel exposes `StateFlow<UiState>`, and the screen sends events back.
2. **Data**: Room entities, DAOs, and one repository per aggregate (People, Tabs, Relationships, Reminders, Backup).
3. **Platform**: thin wrappers for camera, photo picker, location, alarms, notifications and file export. Each wrapper has an interface, so tests can replace it.

## 5. Package layout

```
app/src/main/java/<package>/
  App.kt, AppContainer.kt, MainActivity.kt
  ui/theme/        colors, typography, shapes
  ui/components/   avatar, empty state, search bar
  ui/people/       People list screen + ViewModel
  ui/person/       detail and edit screens + ViewModels
  ui/settings/     Settings screen + ViewModel
  data/db/         AppDatabase, entities, DAOs, migrations
  data/repo/       repositories
  platform/        location, alarms, notifications, photos, backup
```

## 6. Data model (Room)

| Entity | Fields |
|---|---|
| Person | id, name, photoFile (nullable), tabId (nullable), originCountry (ISO code, nullable), metCountry (ISO code, nullable), metCity (nullable), metLat, metLon (nullable), notes, isSelf, sortIndex, createdAt |
| Tab | id, name, sortIndex |
| Relationship | id, fromPersonId, toPersonId, type. Foreign keys with ON DELETE CASCADE. Unique index on (fromPersonId, toPersonId, type). |
| Reminder | id, personId (nullable), message, triggerAt (epoch millis), done |

Rules:

- Store enums by name, not by ordinal. Ordinals break when the enum order changes.
- "Me" is a Person row with `isSelf = true`. The app creates it at first launch. The user cannot delete it. Thus a link to "Me" is the same as any other link.
- The country list comes from `Locale.getISOCountries()`. No network, no dependency.

## 7. Bidirectional relationships

- Store one row per link. Do not store a reverse row. Two rows can drift apart. See [ADR-003](decisions.md#adr-003-one-row-per-relationship).
- Each `RelationType` defines its inverse in code:
  - Symmetric: FRIEND, COLLEAGUE, PARTNER, SIBLING, NEIGHBOUR, FAMILY, OTHER.
  - Paired: PARENT and CHILD.
- To show the links of person X, read the rows where X is `from` (use the type) and the rows where X is `to` (use the inverse).
- Version 1 uses gender-neutral labels. Gendered labels (sister, brother) need a gender field. Add it only if the owner approves.
- Unit-test each type and its inverse.

## 8. Platform decisions

| Area | Decision | Reason |
|---|---|---|
| Gallery photos | System Photo Picker | No storage permission. |
| Camera photos | Camera intent with a FileProvider URI. Do not declare the CAMERA permission. | No camera permission. |
| Photo storage | Copy each photo to app-private storage, max 1024 px, JPEG. Store only the file name. | Gallery URIs can become invalid. |
| Location | `LocationManagerCompat.getCurrentLocation`, coarse only, only on user request | No Google Play Services dependency. |
| City from coordinates | Not in default. Save coordinates; the user types the city. | Android `Geocoder` sends coordinates to a network service. This conflicts with N3. |
| Reminders | AlarmManager with a 10-minute window. Reschedule after reboot. Ask for notification permission at the first reminder. | No exact-alarm permission. |
| Android cloud backup | `allowBackup=false` | N3. |
| Export and import | One `.zip` (people.json + photos/) through the system file picker. Import replaces all data after a confirmation. | User-controlled backup. |

## 9. Screens and navigation

```
People (start)
 |-- Person detail
 |    |-- Person edit
 |    `-- Add relationship (sheet)
 `-- Settings
```

1. **People**: search field, tab row ("All" first, then user tabs), people list, add button. Long-press to drag and reorder. Sort menu: manual or A to Z.
2. **Person detail**: photo or initials avatar, name, origin, place of meeting, relationships, reminders, notes. Edit button.
3. **Person edit**: all fields. Photo buttons: camera and gallery.
4. **Settings**: theme, manage tabs, export, import, about.
5. **Add relationship sheet**: type dropdown, then person picker.

Visual rules are in [design.md](design.md).

## 10. Build and delivery

- Gradle wrapper. Local build is optional: JDK 17 or later and Android command-line tools.
- **Primary path: GitHub Actions** (`.github/workflows/`). On each push: build, unit tests, screenshot tests.
- On a review tag on `dev` (`m1-rc1`, ...): publish a GitHub pre-release with the signed APK and the screenshots. The owner downloads the APK on the phone from the release page.
- On an approved tag on `master` (`m1`, ...): publish a GitHub release. See [ADR-007](decisions.md#adr-007-branches).

## 11. Signing

- Create one keystore at M0. Store it in GitHub Secrets (`SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`).
- Sign each APK with it. If each CI run uses a new debug key, Android refuses to update the app, and the owner loses test data. See [ADR-005](decisions.md#adr-005-github-actions-and-one-signing-key).
- Never commit the keystore.

## 12. Open decisions for the owner

The agent must get an answer before the milestone in brackets.

1. **[M4] City from current location.** A: coordinates only, the user types the city (default, fully offline). B: use Geocoder with one-time consent. Coordinates go to a network service.
2. **[M5] Gendered relationship labels.** A: neutral labels only (default). B: optional gender field, show sister or brother.
3. **[M6] Reminder precision.** A: within 10 minutes, no special permission (default). B: exact time, needs a permission that the user grants in system settings.
4. **[M7] Android cloud backup.** A: off (default). If the phone is lost, data is lost unless the user exported a zip. B: on, Google backs up the data.
