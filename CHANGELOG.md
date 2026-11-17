# Changelog

Versions follow [semantic versioning](https://semver.org/) (ADR-026). Earlier builds were tagged by milestone (`m0` to `m9-rc1`).

## 1.4.0 (2026-11-17)

- Learn names: a new book button at the bottom left of the people list. A round shows 10 random people as flash cards: tags, Conversation Spark, where you met and notes on the front, the name on the back. Tap to turn the card, then say if you knew the name.
- After each round you see your score, the change against the last round, a chart of your progress and the people to learn again. Results are saved on the phone.
- People with no spark, place or notes are not on cards.

## 1.3.0 (2026-11-17)

- New look, "Line": white (or black in dark mode), thin lines instead of cards, one blue accent, new fonts (Space Grotesk and Inter).
- People without a photo show only their name; the coloured initials are gone, also on the map and the widget.
- The add button is a blue square at the bottom right.
- New app icon: three lines on black, the middle one blue.
- The people list has only the Settings button at the top. Under the title you see how many people are listed and the sort order; tap the order to change it.
- The archive is in Settings, under Lists, above Tags and Relationship types.
- A short welcome on the first start says what the app can do. It shows once. The map opens from a person's place.
- Widgets show the app icon and the name Name List, and follow the phone's light or dark mode. The small widget is the app icon with a plus.

- Tabs are now called tags. A person can have several tags, and a tag shows everyone who has it.
- A tag name exists only once; capitals do not count. Tags with the same name from earlier versions are merged.
- In the edit form, pick tags in one drop-down with check boxes. The plus button next to it makes a new tag.
- Settings, "Tags and relationships": edit the list of tags and the list of relationship types. Add your own types, rename any type, and delete types you do not use (their relationships become "Other"). "Other", "Parent" and "Child" stay.
- German and Polish are removed. The app speaks English, Slovak and Czech. A phone set to German or Polish shows English.
- Existing people keep their tags and relationships. Backups carry tags and relationship types, and older backups still import.

## 1.2.0 (2026-11-15)

- Home-screen widget: add a person with one tap. Long press the home screen, choose Widgets, then Name List. Resize it to 1 x 1 (button only), 4 x 1 (button and the newest person) or 4 x 2 (button and the initials of the three newest people).
- The widget's button opens the form with the cursor in the Name field. Saving or closing the form returns to the home screen.
- The add button in the app is dark navy, round and at the bottom right.
- A warning when you save a name that already exists (capitals do not matter), with a button to open that person.
- "Ask about" is now "Conversation Spark". It sits under the name, and the first words show under the name in the list.
- Search finds text in every field and every date.
- Settings shows the version and build, the contact address and "Report a problem" (opens your mail app with the version and phone model, nothing from your list).
- Five languages: English, German, Slovak, Czech, Polish. Choose one in Settings with the flag drop-down, or follow the phone.

## 1.1.0 (2026-11-15)

- Backup password in Settings: set it, change it with the old password, remove it, or reset it when forgotten.
- Export asks "Encrypt this backup?" with "Do not show this again". The default (ask, always, never) can be changed in Settings. Encrypted backups use AES-256 with a key from your password. Import asks for the password of an encrypted file.
- Archive: select people and move them to the archive. They leave the lists, the search and the map. The archive screen shows the date and how long each person has been there, and can restore them.
- More details for each person: native language, date of birth, education, current work and "ask about". The form shows only the photo, name and tab on top; everything else is in a collapsed "More details" section.
- "Last updated" on the person's page: the time of the last save.
- Search also looks in the new text fields.
- Database schema 5 (automatic update, data kept). Backups from 1.0.0 still import.
- Date of first meeting for each person. Leave it empty and the app uses today. It shows on the person's page and in backups; backups from 1.0.0 still import.
- The add button opens the form with the cursor in the Name field. Name is the only required field.
- Database schema 4 (automatic update, data kept).

## 1.0.0 (2026-11-15)

First release. All data stays on the phone.

### People
- Add, edit and delete people with name, notes and photo (camera or gallery).
- Search while typing; ignores case and accents ("simon" finds "Šimon").
- Manual order (drag by the handle) or A to Z sorted by language rules.
- "Me" as a person, for relationships with you.

### Organization
- Unlimited tabs, shown as chips; manage, rename, reorder and delete tabs.
- Multi-select: long press, then tap more; delete or move to a tab at once.

### Places and map
- Where you met: "City, state" text, or the current GPS position.
- Offline world map with country borders; people with GPS shown as initials, grouped when close.
- Small map on each person's page; "Open in maps" hands the point to your map app.

### Relationships
- Two-way links (friend, family, partner, sibling, parent and child, colleague, neighbour, other).

### Backup and settings
- Export and import one backup file (.zip) with people, tabs, links and photos.
- Light (default), dark or system theme; white, black and light blue design with square shapes.

### Quality
- 102 automated tests run on every change, including full-app flows.
- Database upgrades from every earlier build keep your data.
