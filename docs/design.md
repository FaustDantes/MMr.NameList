# Design

Status: approved by the owner on 2026-11-15; Line design approved on 2026-11-17 (ADR-038).

## 1. Principles

- Simple first. Each screen has one main task.
- Calm and clean. Much white space, few colors, no decoration that has no purpose.
- Fast. The user adds a person in less than 10 seconds.

## 2. Theme: "Line" (ADR-038, owner, 2026-11-17)

Design canvas: "MMr.NameList Futuristic Redesign", Option 2 · Line, dark and light rows.

| Role | Light (default) | Dark |
|---|---|---|
| Background, surfaces | White `#FFFFFF` | Black `#000000` |
| Text | Black `#000000` | White `#FFFFFF` |
| Muted text | `#6B6B6B` | `#8C8C8C` |
| Hairlines between rows | `#EAEAEA` | `#1F1F1F` |
| Field lines, outlined buttons | `#8F8F8F` | `#6E6E6E` |
| Accent: add button, Save, selected tag, links | `#0B5CAD`, white on it | `#5AA9FF`, black on it |
| Dialogs and sheets | White | `#111111` |
| Error | `#C62828` | `#FF8A80` |

Wallpaper colors stay off. Type: Space Grotesk (titles, names), Inter (body). Both bundled (ADR-038).

## 3. Shapes

| Element | Shape |
|---|---|
| Photos | Square, corners at 8 % of the size |
| Buttons, add button | 8 dp corners |
| Small check boxes, chips | 4 dp corners |
| Dialogs, sheets | 12 dp corners |
| Rows | No shape: a 1 px hairline above each row |
| Text fields | No box: a line under the text |

## 4. Components

- **Person row**: hairline above; the photo (44 dp) only when one is set; name in Space Grotesk 18 sp; the Conversation Spark preview in muted text. No initials.
- **Selecting**: a 22 dp check box in front of each row.
- **Tags on the people list**: plain text; the selected tag is underlined in the accent.
- **Person detail**: photo 104 dp (only when set) beside the name; the Conversation Spark under an accent line; the other details as titled blocks under hairlines.
- **Edit form**: line fields; tags in one drop-down with check boxes and a plus button next to it; Save filled.
- **Add button**: 64 dp square, accent color, plus icon, bottom right. The add buttons on the tags and relationship type screens use the same color at the standard size.
- **Map**: people as small accent squares; groups as accent squares with the number.
- **Widgets**: black, blue add button, names of the newest people.
- **Empty states**: one short friendly sentence and one action button.

## 5. Accessibility

- Touch targets 48 dp or larger.
- Content descriptions on all icons and images.
- Text scales with the system font size up to 200 % without cut text.
- Do not use color as the only carrier of information.

## 6. Out of scope

No splash animation, no onboarding, no ads, no analytics, no account screen.
