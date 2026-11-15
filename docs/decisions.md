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
