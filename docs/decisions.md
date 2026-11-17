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

## ADR-020: Location without Google Play Services, settings in SharedPreferences

Date: 2026-11-15
Status: accepted
Decision: The GPS button uses `LocationManagerCompat.getCurrentLocation` (fused provider on Android 12+, else GPS or network), asks for location only on tap, and accepts "approximate". Coordinates are shown with 4 decimals; "Open in maps" hands them to the user's map app. The theme choice (M7) is kept in SharedPreferences.
Reason: No new dependency (AGENTS.md rule 6). DataStore is not needed for one value.
Alternatives: Play Services FusedLocationProviderClient (new dependency, not on all phones), DataStore.

## ADR-021: Relationships

Date: 2026-11-15
Status: accepted
Decision: Table `relationship` (schema v3, auto migration). One row (from = A, to = B, type = T) means "B is A's T"; from B's side it reads T.inverse (PARENT and CHILD swap, all other types are symmetric). Gender-neutral types (open decision 2, default A). Foreign keys CASCADE delete links with a person. A fact that already exists from either side is rejected. Added on the detail screen through a bottom sheet: pick a type, then tap a person ("Me" first).
Reason: Requirements F13 and ADR-003.
Alternatives: Two rows per link.

## ADR-022: Backup as zip with JSON

Date: 2026-11-15
Status: accepted
Decision: One `.zip` with `backup.json` (org.json, built into Android) and `photos/`. Saved and opened with the system file picker. Import checks the whole file first, then replaces all data in one transaction. See docs/backup-format.md.
Reason: F12 and ADR-006 (no cloud). No new dependency. One file is easy to move to a new phone.
Alternatives: Copy the SQLite file (breaks across schema versions), kotlinx-serialization-json (new dependency).

## ADR-023: White, black and light blue

Date: 2026-11-15
Status: accepted (replaces the teal and sand palette and dynamic color)
Decision: White background, black text, light blue (`#CFE6FF`) buttons, FAB and selected chips with black text. Text buttons use a darker blue (`#0B5CAD`) because light blue text on white fails contrast. Dynamic (wallpaper) color is off. The default theme is Light, also when the phone is in dark mode; Settings can still switch to Dark or System. The launcher icon uses the same blues.
Reason: Owner request.
Alternatives: Keep System as the default (a phone in dark mode would show a black background).

## ADR-024: Long press selects, a handle drags

Date: 2026-11-15
Status: accepted (changes the gesture in ADR-015)
Decision: Long press on a person starts multi-select (F14); tap adds or removes people while selecting; Back or the X ends it. Drag and drop moves to a handle on the right of each row, shown only in manual order and outside selection. The selection is kept in the ViewModel's SavedStateHandle and is always cut to the people on screen, so a bulk action never touches hidden people.
Reason: Long press to select is the Android convention; one gesture cannot both select and drag.
Alternatives: A "Select" button in the top bar and keep long press for drag (selection would be harder to find).

## ADR-025: Offline world map from Natural Earth

Date: 2026-11-15
Status: accepted
Decision: Country borders from Natural Earth Admin 0, 1:110m (public domain), taken from npm `world-atlas` 2.0.2 and converted by `tools/map/convert.py` into an 80 KB binary asset. Drawn with Compose Canvas in Web Mercator (latitude clamped to 85°, Antarctica left out). Pinch, drag and double tap; markers are composables (circles with initials, as the owner asked), grouped when they would overlap. Only people with GPS coordinates show; there is no geocoding (ADR-019).
Reason: Free, no API key, no network, no new dependency; the map never sends locations anywhere (N3). The 1:110m detail matches "not so detailed".
Alternatives: Google Maps SDK (API key, network, Play Services), MapLibre or osmdroid with online tiles (network, larger app), 1:50m data (5 times larger).

## ADR-026: Semantic versions from 1.0.0

Date: 2026-11-15
Status: accepted (extends ADR-007)
Decision: From version 1.0.0, releases are tagged `vX.Y.Z` on `master` and review builds `vX.Y.Z-rcN` on `dev`. The app's versionName is the tag without "v"; versionCode is the CI run number, so it always goes up. Each release gets an entry in CHANGELOG.md.
Reason: The owner asked for "version 1". Milestone tags no longer fit once the planned milestones are done.
Alternatives: Keep milestone tags (unclear to users of the app).

## ADR-027: Date of first meeting, stored as an epoch day

Date: 2026-11-15
Status: accepted
Decision: `person.firstMetOn` is a nullable `Long`: days since 1970-01-01 of the local calendar date. Room schema 4, auto migration from 3. An empty date is replaced by today when the person is saved; a person from before 1.1 counts from the day they were added (`createdAt`), without writing it to the database. The backup gets an optional `firstMetOn` field; the backup `version` stays 1 because older files import unchanged. The Name field takes focus when a new person opens. The date picker is the Material 3 `DatePickerDialog`; no new dependency.
Reason: A calendar date has no time or time zone, so an epoch day cannot shift when the user travels (a timestamp would). Nullable avoids inventing a date for old rows during migration.
Alternatives: Timestamp in milliseconds (shifts with the time zone); text "yyyy-MM-dd" (needs parsing; sorts the same but is easier to corrupt); non-null column with a default of 0 (shows 1970 for old people).

## ADR-028: Backup encryption with a password in settings

Date: 2026-11-15
Status: accepted
Decision: An encrypted backup is the zip stream inside Tink's AES-GCM-HKDF streaming AEAD (`AesGcmHkdfStreaming`, 256-bit key, 64 KiB segments). The key is PBKDF2-HMAC-SHA256 of the password, 600,000 iterations, a random 16-byte salt per file. The file starts with a plain header: `MMRNLENC`, version 1, iteration count, salt. The header is the associated data, so a changed header fails. Import accepts both plain and encrypted files and finds the type from the first 8 bytes. The format is public; only the password is secret. Details: [backup-format.md](backup-format.md).
The settings password is saved only as a check value: PBKDF2 hash with its own salt, in SharedPreferences. The app does not keep the password. To encrypt an export, the user types the password (it must match the check value). The check lets the user change or remove the password with the old one, and stops a typing error from locking a backup. Changing the password does not re-encrypt old backups. A forgotten password can be reset in settings; this removes only the check value.
Export asks "Encrypt this backup?" with "Do not show this again". The checkbox saves the answer as the default (`EncryptChoice`: ASK, ALWAYS, NEVER). Settings changes it.
New dependency: `com.google.crypto.tink:tink-android` 1.24.0 (newest stable on Maven Central, checked with the versions probe on 2026-11-15). Reason: a reviewed streaming AEAD. The platform AES-GCM buffers a whole file in memory when it decrypts, which can run out of memory with many photos, and a self-made chunk format is easy to get wrong.
Reason: The owner asked for AES, a password in settings, and a question on export.
Alternatives: Platform `Cipher` AES-GCM only (memory limit); password-protected zip (older format, weaker integrity, needs a library); storing the password in the Android Keystore so export needs no typing (the password would be at rest; the owner can ask for it later).

## ADR-029: Archive

Date: 2026-11-15
Status: accepted
Decision: `person.archivedAt` (milliseconds, nullable). A person with a value is archived. The list, search, map and relationship picker read only people without it. The archive screen reads the others, newest first, and shows the date (device time zone) and the time in the archive: under a day, days up to 59, months up to 23, then years. Archiving a person who is already archived keeps the first time. "Me" cannot be archived. Restoring clears the value; tab, order and links are untouched. Archiving and restoring do not change "last updated". Relationships of archived people stay and show on the other person.
Reason: The owner asked to move people to an archive with a timestamp and the time they stay there. "Note how long" is read as the time in the archive, computed from the timestamp, not as a text note. Say so if a text note is wanted.
Alternatives: A separate table (moves rows and links); a tab called "Archive" (people would still show in "All" and on the map).

## ADR-030: More details, collapsed form, last updated

Date: 2026-11-15
Status: accepted
Decision: New nullable columns `nativeLanguage`, `birthDate` (epoch day, like ADR-027), `education`, `currentWork`, `askAbout` and `lastUpdatedAt` (milliseconds). Room schema 5, automatic migration from 4. The edit form shows the photo, name and tab; everything else is in a "More details" section that starts collapsed on every visit (place of meeting, first meeting, birth date, the four text fields, notes). Every save from the edit screen sets `lastUpdatedAt`; moving to a tab, archiving and reordering do not. People saved before 1.1 show the day they were added. Blank text is stored as empty. Search covers the new text fields. The backup carries all new fields as optional (format version stays 1).
Reason: The owner asked for these fields, a short top of the form, and a "last updated" time.
Alternatives: Expand the section when a person already has values (one tap less when editing; easy to add later); a custom-fields table (more than the owner asked for).

## ADR-031: Home-screen widget with RemoteViews

Date: 2026-11-15
Status: accepted
Decision: The widget is an `AppWidgetProvider` with plain `RemoteViews` (XML layouts `widget_icon`, `widget_strip`, `widget_card`). One widget is resizable; the layout follows the size the launcher reports (`WidgetSize.forDp`: height 110 dp or more = card, width 110 dp or more = strip, else icon). `updatePeriodMillis` is 0; the data is read on each update, and `MainActivity.onStop` asks for an update, so a new person shows up as soon as the app closes. The add button starts `MainActivity` with the action `ADD_PERSON`; the activity opens the edit form and, when the form closes or saves, finishes, so the user is back on the home screen. The widget shows the name of the newest person on the 4 x 1 layout and initials only on the 4 x 2 layout, as designed. No new dependency.
Reason: The owner approved the design and asked for it as designed. RemoteViews needs no library; Jetpack Glance would add a dependency for three simple layouts without a list.
Alternatives: Jetpack Glance (new dependency, useful once the widget has a scrolling list); a scrolling list in the 4 x 2 layout (the owner kept the design without it, so the question of names or initials stays as designed).

## ADR-032: Add button, dark navy, bottom center, 112 dp

Date: 2026-11-15
Status: accepted
Decision: The add button on the people list is 112 dp (twice the Material default of 56 dp), color `#0B2545` (dark navy) with a white 56 dp icon, and sits at the bottom center (`FabPosition.Center`). The list has 144 dp of bottom padding so the last person is not hidden. The widget add button uses the same navy. All other buttons keep the light blue palette (ADR-023); the small "add tab" button stays as it was.
Reason: The owner asked for a dark navy color, a bottom center position and twice the size, so people can tap it easily. The owner's color request is applied to the add buttons only, because the palette of all other buttons was chosen in ADR-023; say if all buttons should change.
Alternatives: Navy for every button (changes ADR-023 across the app); navy only on the widget.

## ADR-033: Conversation Spark, duplicate names, search by any field, version and contact

Date: 2026-11-15
Status: accepted
Decision: F22: saving a new name that matches another person's name, ignoring case and extra spaces, first asks: open the existing person, save anyway, or change the name. Archived people count. Checked in the repository (`findSameName`), not in the Composable.
F23: the field "Ask about" is renamed "Conversation Spark" and sits directly under the name on the edit form (not collapsed). On the person's page it is a highlighted block under the name. In the people list the first line of the spark is shown under the name, cut to five words with "…".
F26: search matches every text field (including origin and meeting country) and every date (first met, birth, last updated, archived), written as ISO, dotted, slashed, month-name and the localized medium style. The add button is a 112 dp navy circle at the bottom right (ADR-032 is superseded for its position).
F24: Settings shows the version and build number, the contact address info@michalmarcin.com (opens the mail app), and "Report a problem", which opens the mail app with the version, build, Android version, phone model and language. Nothing from the list is sent.
F25: languages English (default), German, Slovak, Czech, Polish, via `values-<lang>/strings.xml` and `locales_config.xml`. Plural forms use the CLDR categories of each language.
Reason: The owner asked for these. The mail app is used so the app needs no internet permission.
Alternatives: A built-in form that sends the report (needs internet permission, which the app avoids); machine translation (the texts were written by hand and need a native check).

## ADR-034: Language drop-down, 88 dp add button

Date: 2026-11-15
Status: accepted
Decision: The language is saved in `SettingsStore` (`AppLanguage`: system, en, de, sk, cs, pl). `MainActivity.attachBaseContext` wraps the context with the saved locale and sets the default locale, so texts and dates follow it. A change recreates the activity. Flags are emoji; each entry also shows the language name in its own language. No new dependency: `AppCompatDelegate` would add androidx.appcompat. The widget follows the system language, because it runs outside the activity. The add button is 88 dp (was 112 dp), still a navy circle at the bottom right (supersedes the size in ADR-032 and ADR-033).
Reason: The owner asked for a visible language list with flags and for a smaller add button. A system option stays first so the app can follow the phone again.
Alternatives: `AppCompatDelegate.setApplicationLocales` (new dependency); only the Android 13 system language screen (hidden from most users; the owner could not find it).

## ADR-035: Many tabs per person

Date: 2026-11-16
Status: accepted
Decision: New table `person_tab(personId, tabId)` with a composite key and cascade delete on both sides. Database version 6, auto migration with `PersonTabMigration`, which copies each `person.tabId` into the new table. The column `person.tabId` stays in the schema, unused and set to null on every save, so the migration cannot lose data and a later version can drop it. Backup format version stays 1: people get an added `tabIds` array and keep `tabId` (the lowest tab) for older versions of the app; import reads `tabIds`, else `tabId`, and drops links to tabs that are not in the file. Bulk "Move to tab" keeps its meaning: it replaces all tabs of the people with the one chosen tab.
Reason: The owner chose "more than one tab per person" from the market review (tags in Clay and Contacts+, groups in NameRoll).
Alternatives: Store a list in one column (cannot be filtered in SQL, breaks foreign keys); drop `person.tabId` at once (needs a table copy and risks data loss); add a separate "add to tab" bulk action (not asked).

## ADR-036: Languages reduced to English, Slovak, Czech; add a tab from the edit form

Date: 2026-11-16
Status: accepted
Decision: German and Polish are removed (`values-de`, `values-pl`, their entries in `locales_config.xml` and in `AppLanguage`). Supersedes the language list in ADR-033 (F25) and ADR-034. A phone that is set to German or Polish now shows English. A saved choice of "de" or "pl" is read as "System language" (`AppLanguage.fromTag`). The edit form always shows the tab chips, with an "Add tab" chip that opens the tab name form, creates the tab and puts the person in it (F28). The texts reuse existing strings, so the new control is in all three languages.
Reason: The owner asked for this on 2026-11-16. Adding a tab needed a trip to the tabs screen, and with no tabs the form showed nothing.
Alternatives: Keep the two languages hidden (dead translations still cost upkeep).

## ADR-037: Tags with unique names; relationship types as an editable list

Date: 2026-11-16
Status: accepted
Decision:
- "Tab" is called "tag" in the app (en: tag, sk: štítok, cs: štítek). Code, table (`tab`, `person_tab`) and backup keys keep the old name, so no data moves and older backups still import.
- A tag name exists only once. Two names are the same when they match without case and outer spaces (`labelKey`). The name form shows "This name exists already." and cannot be saved. The repository refuses the name too. In the edit form, a taken name picks the existing tag.
- Migration 6 to 7 and the backup import merge tags whose names differ only in case: links go to the first tag (by order), the rest are deleted.
- Relationship types are rows in a new table `relation_type` (id, name, builtIn, sortIndex). The nine built-in types have fixed ids 1 to 9 (`RelationType.seedId`). `name` is null for a built-in type the user did not rename, so it shows in the app language. `relationship.type` (a name) becomes `relationship.typeId`. Room cannot change a column with an auto migration and keep the data, so `Migration6To7` is written by hand and tested.
- A type name is unique in the same way. A built-in type that is not renamed blocks its label in all three languages.
- Deleting a type turns its relationships into "Other"; a link that would then be a copy of an existing one is dropped. "Other" (the target of that move), "Parent" and "Child" (a pair: one is the other seen from the other side) can be renamed but not deleted. A type the user makes reads the same from both sides.
- Settings has a "Tags and relationships" section that opens the tags screen and the new relationship types screen. The people list keeps its tags button.
- The edit form shows one drop-down for tags with a check box per tag (the menu stays open for more choices) and a plus button next to it to make a new tag.
- The backup format stays version 1: `relationTypes` and `typeId` are new optional fields; `type` stays for older app versions (a type the user made is written as OTHER there).
Reason: The owner asked for this on 2026-11-16.
Alternatives: Rename the code and tables to "tag" (large change and a migration for no user value). Store the type name in each relationship (a rename would have to touch every link). Let the user delete every type (deleting "Other" leaves no safe target; deleting one of parent and child breaks the reverse reading). Let custom types have a reverse type (more UI; can come later).

## ADR-038: "Line" design, light and dark; photos only

Date: 2026-11-17
Status: accepted
Decision:
- The owner chose "Option 2, Line" from the design canvas (MMr.NameList Futuristic Redesign). It replaces the palette of ADR-023 and the add button of ADR-032 to ADR-034.
- Light: white `#FFFFFF`, black text, muted text `#6B6B6B`, hairlines `#EAEAEA`, accent `#0B5CAD` (white text on it). Dark: black `#000000`, white text, muted `#8C8C8C`, hairlines `#1F1F1F`, accent `#5AA9FF` (black text on it), dialogs `#111111`. Light stays the default theme.
- No cards: rows sit under 1 px hairlines (`topHairline`). Tags on the people list are plain text; the selected one is underlined in the accent. Text fields are a line, not a box (`lineFieldColors`).
- A person shows a photo only when one is set. There are no initials anywhere: not in lists, not on the detail page, not on the map (a small accent square), not on the widget (names instead).
- Add button: a 64 dp square with 8 dp corners in the accent color, bottom right, a plus icon.
- Save is a filled accent button; the other buttons are text or outlined.
- Type: Space Grotesk for titles and names, Inter for body text. The six TTF files (regular, medium, semibold) are bundled in `res/font`, about 1.3 MB, so no network or Google Play services are needed. Both are under the SIL Open Font License 1.1; the license texts are in `licenses/`. Both cover all Czech and Slovak letters (checked with fontTools).
- Widgets are black in light and dark mode, with the blue add button and the names of the newest people.
Reason: The owner asked for a new, futuristic look on 2026-11-17, chose Line, asked for a light mode and for photos only on profiles that have one.
Alternatives: Downloadable Google Fonts (needs Google Play services and a provider certificate; fails offline on first start). Keep initials for people without a photo (the owner did not want them).
