# Requirements

Status: approved by the owner on 2026-11-15.
Source: [Name List App - Requirements](https://claude.ai/artifact/K4nibaTK4TagnTbbccBogw) (Claude Docs). This file is the copy that the build agent uses.

## 1. Purpose

A mobile app to remember people that the user meets. The user records the name, photo, origin, place of meeting, and the relationships of each person. All data stays on the user's own device.

## 2. Platforms and constraints

- Primary platform: Android. iOS is a later option.
- Storage: local on the device only. No account, no cloud sync, no server in version 1.
- Theme: light and dark mode.
- Interface language: English first. Localization later.

## 3. Core features

| ID | Feature | Description |
|---|---|---|
| F1 | Name lists | The user creates person entries quickly. Each entry has at minimum a name. |
| F2 | Photo capture | The user takes a photo with the camera and attaches it to a person. |
| F3 | Photo library | The user selects an existing photo from the device gallery. |
| F4 | Unlimited tabs | The user creates any number of tabs to categorize people. A person belongs to a tab. |
| F5 | Rearranging | Drag and drop ordering, and alphabetical sorting. |
| F6 | All items view | One screen shows all people across all tabs. |
| F7 | Search | Fast global search across all people and their fields. |
| ~~F8~~ | ~~Notifications~~ | Removed by the owner on 2026-11-15 (ADR-019). |
| F9 | Dark mode | Light, dark, and system theme. |

## 4. Extended features

### F10 Place of meeting (changed by the owner on 2026-11-15, ADR-019)

- Optional. One text field: "City, state" (free text).
- One GPS button: stores the current device location (coordinates). No city lookup; coordinates stay on the device.
- The app asks for location permission only when the user taps the GPS button. The app works fully without it.

### ~~F11 Country of origin~~

Removed by the owner on 2026-11-15 (ADR-019).

### F12 Local storage

- All data, including photos, stays on the device.
- The user can export and import a backup file.

### F13 Relationships

- A person can have a link to the user ("Me") or to another person in the app.
- The user selects a relationship type from a dropdown: friend, colleague, family, partner, sibling, parent, child, neighbour, other.
- After the type, the user selects the person that the link points to.
- Relationships are **bidirectional**. When the user sets "Anna is Peter's sibling", Peter shows Anna as his sibling automatically.
- Each type has an inverse. Symmetric types: friend, colleague, partner, sibling, neighbour, family, other. Paired types: parent and child.
- When the user deletes a person, the app deletes all links of that person. When the user edits a link, both sides change.

### F14 Multi-select (added by the owner on 2026-11-15)

- The user selects several people (long press, then tap more).
- The user deletes all selected people at once, after a confirmation.
- The user moves all selected people to one tab, or removes their tab.
- Actions apply only to people that are shown at that moment.

### F15 World map (added by the owner on 2026-11-15)

- A simple world map with country borders, not detailed, like the Instagram map.
- Each person with GPS coordinates (F10) shows as a circle with initials.
- The map works offline and sends nothing (N3). A typed "City, state" has no coordinates and does not show.
- The detail screen shows a small map at the person's place.

### F16 Date of first meeting and quick add (added by the owner on 2026-11-15)

- Each person has a date of the first meeting. The user can pick it, or leave it empty.
- An empty date is saved as the current date (local time zone). The picker allows no future dates.
- The name is the only required field.
- The add button opens the new-person form with the cursor in the Name field and the keyboard open.
- Backups carry the date (optional field; backups of version 1.0.0 still import).

### F17 Backup password and encryption (added by the owner on 2026-11-15)

- Settings has a backup password. The user sets it, changes it by giving the old and the new password, or removes it with the old password. A forgotten password can be reset; old encrypted backups then stay locked.
- Export asks "Encrypt this backup?" with a checkbox "Do not show this again". The checkbox makes the answer the default. Settings changes the default: ask, always encrypt, never encrypt.
- To encrypt, the user types the password again (it must match the saved one). With no password set, the app asks for one first.
- An encrypted backup uses AES-256-GCM with a key from the password (PBKDF2-HMAC-SHA256). Import asks for the password of an encrypted file. A wrong password or a changed file changes nothing.
- The password is at least 8 characters. The app never stores the password, only a check value (ADR-028).

### F18 Archive (added by the owner on 2026-11-15)

- The user selects people (F14) and moves them to the archive.
- Archived people leave the lists, the search, the map and the relationship picker. Their data and links stay.
- Each archived person has the time of archiving. The archive screen shows the date and how long they are in the archive. The user can restore people or delete them.
- "Me" cannot be archived.

### F19 More details and last update (added by the owner on 2026-11-15)

- New optional fields: native language, date of birth, education, current work, ask about.
- The form shows only the icon (photo), name and tab on top. Place of meeting, first meeting, the new fields and notes are in a collapsed "More details" section.
- Each person has "last updated": the time of the last save from the edit screen. The detail screen shows it.
- Search also looks in the new text fields.

## 5. Non-functional requirements

| ID | Requirement |
|---|---|
| N1 | The app works offline. |
| N2 | Search results show while the user types. |
| N3 | Photos and location data never leave the device. |
| N4 | Data survives app updates. |
| N5 | All icons have content descriptions. Touch targets are 48 dp or larger. |

## 6. Open decisions

See [architecture.md, section 12](architecture.md#12-open-decisions-for-the-owner).

### F20 Home-screen widget (added by the owner on 2026-11-15)
A widget for the phone's home screen with one big add button. The button opens the quick-add form with the cursor in the Name field; saving or closing the form returns to the home screen. One widget resizes between three layouts: 1 x 1 (add button), 4 x 1 (add button and the name of the newest person), 4 x 2 (add button, hint and the initials of the three newest people). Archived people and "Me" are not shown. Dark navy add button (ADR-031).

### F21 Big centered add button (added by the owner on 2026-11-15)
The add button on the people list is dark navy, twice the usual size (112 dp) and at the bottom center, so it is easy to hit with the thumb (ADR-032).
