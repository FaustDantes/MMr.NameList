# MMr.NameList

A simple Android app to remember the people you meet: name, photo, where they come from, where you met, and how they relate to you and to each other. All data stays on your phone.

**Status:** M0 released. M1 to M3 ready for review. See [progress](docs/progress.md) and [releases](../../releases).

## Features

| Area | Features |
|---|---|
| People | Fast name lists, search while typing, all-people view, A to Z or manual order |
| Photos | Take a photo or pick one from the gallery |
| Organization | Unlimited tabs, drag and drop |
| Origin and place | Country of origin, place of meeting (country, city, or current location) |
| Relationships | Two-way links between people and to you, with relationship types |
| Reminders | A notification with your message at a set time |
| Privacy | Local storage only, export and import a backup file |
| Look | Material 3, light and dark mode |

Full list with IDs: [docs/requirements.md](docs/requirements.md).

## Technology

Kotlin, Jetpack Compose, Material 3, Room. Built and tested in GitHub Actions. No Android Studio necessary. Details: [docs/architecture.md](docs/architecture.md).

## Documentation

| Document | Content |
|---|---|
| [Requirements](docs/requirements.md) | What the app must do (F1 to F13, N1 to N5) |
| [Architecture](docs/architecture.md) | Stack, layers, data model, platform decisions, open decisions |
| [Design](docs/design.md) | Theme, colors, components, accessibility |
| [Milestones](docs/milestones.md) | Build plan M0 to M7, "done when" criteria, screenshot rules |
| [Decisions](docs/decisions.md) | Architecture Decision Records (ADR) |
| [Progress](docs/progress.md) | Current status and session log |
| [Milestone report template](docs/templates/milestone-report.md) | Format of each report to the owner |
| [Screenshots](docs/screenshots/README.md) | Screenshots per milestone |

## For AI agents

Start with [AGENTS.md](AGENTS.md). [CLAUDE.md](CLAUDE.md) points to the same file.

## Repository structure

```
.
|-- AGENTS.md              instructions for AI coding agents
|-- CLAUDE.md              points to AGENTS.md
|-- README.md              this file
|-- LICENSE                MIT
|-- docs/
|   |-- requirements.md
|   |-- architecture.md
|   |-- design.md
|   |-- milestones.md
|   |-- decisions.md
|   |-- progress.md
|   |-- templates/         report templates
|   `-- screenshots/       one folder per milestone
|-- .github/               CI workflows, PR and issue templates
|-- signing/               encrypted release keystore (ADR-008)
`-- app/                   Android app; app/schemas/ holds Room schemas
```

## Install on your phone

From milestone M0 on, each milestone has a GitHub release with a signed APK.

1. Open the [releases page](../../releases) on your Android phone.
2. Download the APK of the latest release.
3. Open it. If Android asks, allow installs from this source.

## Branches

- `master`: approved milestones only.
- `dev`: work in progress.

See [ADR-007](docs/decisions.md#adr-007-branches).

## License

[MIT](LICENSE)
