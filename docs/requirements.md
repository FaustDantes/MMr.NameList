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
