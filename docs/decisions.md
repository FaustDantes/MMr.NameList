# Decisions log

Each entry is an Architecture Decision Record (ADR). Add new entries at the end. Do not change an accepted entry. To change a decision, add a new entry that supersedes it.

Template:

```
## ADR-NNN: Title
Date: YYYY-MM-DD
Status: proposed | accepted | superseded by ADR-NNN
Decision: ...
Reason: ...
Alternatives: ...
```

## ADR-001: Kotlin and Jetpack Compose

Date: 2026-11-15
Status: accepted
Decision: Native Android app in Kotlin with Jetpack Compose and Material 3. One Gradle module.
Reason: Android is the only target. Native APIs for camera, location, alarms and local storage. The owner wants low cost and high speed.
Alternatives: Flutter (better for a later iOS version), PWA (fails reminders and reliable local storage).

## ADR-002: Manual dependency injection

Date: 2026-11-15
Status: accepted
Decision: One `AppContainer` class creates the database, repositories and platform wrappers.
Reason: The app is small. Hilt adds annotation processing, build time and complexity.
Alternatives: Hilt, Koin.

## ADR-003: One row per relationship

Date: 2026-11-15
Status: accepted
Decision: Store each link once. Compute the reverse direction from the `RelationType` inverse.
Reason: Two rows can drift apart after an edit or a failed write.
Alternatives: Two rows per link in one transaction.

## ADR-004: Roborazzi screenshots

Date: 2026-11-15
Status: accepted
Decision: Screenshot tests with Roborazzi on Robolectric, in CI, on each milestone.
Reason: The owner needs screenshots after each milestone. JVM screenshots need no emulator and are deterministic.
Alternatives: Emulator with `adb screencap` (slow, unstable in CI), Paparazzi.

## ADR-005: GitHub Actions and one signing key

Date: 2026-11-15
Status: accepted
Decision: GitHub Actions builds and tests each push. Milestone tags publish a pre-release with an APK signed by one keystore in GitHub Secrets.
Reason: No Android Studio. A fixed key lets each new APK update the installed app and keep data.
Alternatives: Local command-line build only.

## ADR-006: Local-only data

Date: 2026-11-15
Status: accepted
Decision: All data and photos in app-private storage. `allowBackup=false`. No network calls in version 1.
Reason: Requirements F12 and N3.
Alternatives: See open decision 4 in [architecture.md](architecture.md#12-open-decisions-for-the-owner).

## ADR-007: Branches

Date: 2026-11-15
Status: accepted
Decision: Milestone work goes to the `dev` branch. For review, tag `m<N>-rc<K>` on `dev`; CI publishes a pre-release with the APK. After the owner approves and all tests pass, merge `dev` into `master` and tag `m<N>`; CI publishes a release.
Reason: `master` always holds an approved, working version. The owner can test an APK before approval.
Alternatives: Direct commits to `master`.

## ADR-008: Encrypted keystore in the repository

Date: 2026-11-15
Status: accepted (replaces the keystore part of ADR-005)
Decision: The release keystore is in the repo as `signing/release.jks.enc`, encrypted with AES-256 (OpenSSL, PBKDF2, 600 000 iterations). CI decrypts it with one secret, `SIGNING_PASSWORD`. Store and key passwords are the same.
Reason: The agent cannot write GitHub secrets. One short secret is easy for the owner to add from a phone. The key is also backed up in the repo.
Alternatives: Four secrets with a base64 keystore (hard to paste on a phone, no backup).

## ADR-009: Application ID

Date: 2026-11-15
Status: accepted
Decision: `com.michalmarcin.namelist`, based on the owner's domain.
Reason: A reverse-domain ID that the owner controls. The ID cannot change later without a new install.
Alternatives: `com.mmr.namelist`.

## ADR-010: Square design

Date: 2026-11-15
Status: accepted
Decision: Square shapes instead of circles: avatars and icon badges are squares with 22 % corners, cards 10 dp, search field and buttons 12 dp. See docs/design.md section 3.
Reason: Owner request ("squared icons").
Alternatives: Sharp-cornered glyphs (Material Symbols Sharp). Not chosen: the request was about shapes, and the current glyphs already have square forms.

## ADR-011: Search and A-Z sort in Kotlin

Date: 2026-11-15
Status: accepted
Decision: The DAO returns all people. The repository filters (lower case, no diacritics) and sorts (`java.text.Collator` for the user's language).
Reason: SQLite `LIKE` and `NOCASE` ignore case only for ASCII. Czech names such as "Šimon" must match "simon", and "Č" must sort by Czech rules. The list is small (hundreds of people), so memory and speed are not a problem.
Alternatives: SQLite FTS with a custom tokenizer (complex), a normalized name column (needs migrations for each new searchable field).

## ADR-012: Room 2.8, not Room 3

Date: 2026-11-15
Status: accepted
Decision: Room 2.8.5 with KSP 2.3.14. Schemas in `app/schemas/` (Room Gradle plugin).
Reason: Room 3 is alpha (3.1.0-alpha03). AGENTS.md rule 7 requires stable versions.
Alternatives: Room 3 alpha.

## ADR-013: Navigation and ViewModel libraries

Date: 2026-11-15
Status: accepted
Decision: Navigation Compose 2.10.2 with type-safe routes (Kotlin serialization plugin), Lifecycle ViewModel Compose 2.11.0.
Reason: Planned in docs/architecture.md. Type-safe routes catch wrong arguments at compile time.
Alternatives: String routes.

## ADR-014: Coil and ExifInterface for photos

Date: 2026-11-15
Status: accepted
Decision: Coil 3.6.3 loads photos in the UI (memory cache, off the main thread). AndroidX ExifInterface 1.4.2 reads the camera orientation so photos are upright on Android 8 and 9, where `ImageDecoder` is not available.
Reason: Planned in docs/architecture.md. Without EXIF handling, many camera photos show turned by 90 degrees.
Alternatives: Own bitmap loading with an LRU cache (more code, more bugs).

## ADR-015: Reorderable for drag and drop

Date: 2026-11-15
Status: accepted
Decision: `sh.calvin.reorderable:reorderable` 3.1.0 for drag and drop in the People list (long press) and in the tab manager (drag handle).
Reason: Compose has no built-in reorder for lazy lists. The library handles auto-scroll and item animation.
Alternatives: Own gesture code with `detectDragGesturesAfterLongPress` (more code, scroll edge cases).

## ADR-016: Tabs as chips, global search, reorder by slots

Date: 2026-11-15
Status: accepted
Decision: Tabs show as square filter chips under the search field ("All" first), only when at least one tab exists. A search query ignores the selected tab (F7 is global search). Drag and drop works only in manual order without a search query. A reorder inside one tab reuses the sort positions of the moved people, so people in other tabs keep their places.
Reason: Chips fit the square design and many tabs (scroll). Reordering a filtered list must not move hidden people.
Alternatives: Material tab row (round indicator, less room), per-tab sort columns (more schema).

## ADR-017: Room auto migration 1 to 2

Date: 2026-11-15
Status: accepted
Decision: Schema v2 adds the `tab` table and `person.tabId` (foreign key, ON DELETE SET NULL). Room auto migration; `MigrationTest` checks that people survive.
Reason: N4 (data survives updates). The owner already has v1 data from `m1-rc1` and `m2-rc1`.
Alternatives: Manual migration SQL.

## ADR-018: Everything square, icon-only add buttons

Date: 2026-11-15
Status: accepted (makes ADR-010 stricter)
Decision: Smaller radii: text fields and menus 2 dp, chips 4 dp, cards 6 dp, buttons and search 8 dp, dialogs 10 dp, avatars 12 %. The add-person and add-tab buttons are icon-only floating action buttons with content descriptions.
Reason: Owner request after reviewing M1 to M3.
Alternatives: Zero radius (looks broken at small sizes on high-density screens).

## ADR-019: Scope change after M3

Date: 2026-11-15
Status: accepted
Decision: (1) M6 Reminders and F8 removed. (2) F11 Country of origin removed. (3) M4 is one "City, state" text field plus a GPS button that stores coordinates; no city lookup. (4) No more screenshots; Roborazzi and screenshot tests removed. (5) M4, M5 and M7 are built without stops and reviewed together.
Reason: Owner decisions.
Alternatives: None; owner direction.
