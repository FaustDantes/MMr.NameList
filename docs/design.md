# Design

Status: approved by the owner on 2026-11-15.

## 1. Principles

- Simple first. Each screen has one main task.
- Calm and clean. Much white space, few colors, no decoration that has no purpose.
- Fast. The user adds a person in less than 10 seconds.

## 2. Theme

| Item | Rule |
|---|---|
| Design system | Material 3 |
| Color | Dynamic color on Android 12 and later. Palette (owner, 2026-11-15, ADR-023). Wallpaper colors are off. Default theme: Light.

| Role | Light | Dark |
|---|---|---|
| Background, surfaces | White `#FFFFFF` | Black `#000000` |
| Text, icons | Black `#000000` | White `#FFFFFF` |
| Buttons, selected chips | Light blue `#CFE6FF`, black text | Light blue `#CFE6FF`, black text |
| Add button (people list and widget) | Dark navy `#0B2545`, white icon | Dark navy `#0B2545`, white icon |
| Text buttons, accents | Blue `#0B5CAD` | Light blue `#9CCBFF` |
| Cards | Near white `#F5F7F9` | `#121417` |

## 3. Shapes (square design)

The owner chose a square look on 2026-11-15 (ADR-010) and asked for "everything squared" later the same day (ADR-018). No circles, no pills.

| Element | Shape |
|---|---|
| Avatars, icon badges | Square with corners at 12 % of the size (`IconSquare`) |
| Cards, list rows | 6 dp corners |
| Search field, floating action button, all buttons | 8 dp corners. Material 3 buttons are round by default: always pass `shape = MaterialTheme.shapes.large`. |
| Chips | 4 dp corners |
| Dialogs, sheets | 10 dp corners |
| Text fields, menus | 2 dp corners |

The launcher icon shape comes from the phone's launcher. The app cannot force it to be square.

## 4. Components

- **Person card**: card with 10 dp corners, 48 dp square avatar, name in `titleMedium`, one muted line below (city, or relationship to "Me").
- **Avatar fallback**: initials in a square on a color from a hash of the name. The same name always gets the same color. Text contrast 4.5:1 or more.
- **Empty states**: one short friendly sentence and one action button.
- **Search field**: always visible at the top of the People screen.
- **Add buttons**: icon only with a content description for screen readers. The people list has one 112 dp dark navy person-add button at the bottom center (ADR-032). The "add tag" button on the tags screen and the "add relationship type" button stay small, light blue, bottom right.

## 5. Accessibility

- Touch targets 48 dp or larger.
- Content descriptions on all icons and images.
- Text scales with the system font size up to 200 % without cut text.
- Do not use color as the only carrier of information.

## 6. Out of scope

No splash animation, no onboarding, no ads, no analytics, no account screen.
