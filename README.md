# Ethos

Ethos is a Paper plugin that adds a persistent karma system, unlockable titles, and gameplay effects for the Ethos
server. Player progress is stored in SQLite and can be exposed to other plugins through PlaceholderAPI.

## Features

- Configurable positive and negative karma for combat, trading, animal interaction, and villager curing
- Daily karma decay and anti-farm protection for repeated kills and repeated interactions
- Karmic and achievement-based titles with flat, alternating, and gradient colors
- Configurable buffs, drawbacks, mob reactions, and player restrictions at different karma tiers
- English and Russian player messages
- Optional PlaceholderAPI integration for chat, tab lists, and other plugins

The complete gameplay reference, including karma values, title conditions, and effect tiers, is available in
[WIKI.md](WIKI.md).

## Requirements

- Java 21
- Paper 1.21.11
- PlaceholderAPI 2.11.6 or newer (optional)

## Installation

1. Download `ethos-1.0.0.jar`.
2. Place it in the server's `plugins/` directory.
3. Restart the server.
4. Edit the generated files in `plugins/ethos/` and run `/ethos reload` when needed.

Install PlaceholderAPI before Ethos if another plugin needs the Ethos placeholders. Ethos still works when
PlaceholderAPI is absent.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/karma` | `ethos.karma.view` | Show your karma and active effects |
| `/karma <player>` | `ethos.karma.view.others` | Show another player's karma |
| `/karma reset` | `ethos.karma.view` | Reset your karma, subject to the configured cooldown |
| `/karma effects` | `ethos.karma.view` | Toggle personal karma effects, subject to the configured cooldown |
| `/karma set <player> <value>` | `ethos.karma.admin` | Set another player's karma |
| `/etitle` | `ethos.title.use` | List and select unlocked titles |
| `/etitle all [page]` | `ethos.title.use` | Browse all title slots |
| `/etitle info <id>` | `ethos.title.use` | Show title details |
| `/etitle reset` | `ethos.title.use` | Remove the active title |
| `/etitle give <player> <id>` | `ethos.title.admin` | Grant a title |
| `/ethos reload` | `ethos.admin` | Reload configuration and title definitions |

The `/etitle` command also has the aliases `/et` and `/dt`.

## PlaceholderAPI

When PlaceholderAPI is installed, Ethos provides:

- `%ethos_karma%`
- `%ethos_title%`
- `%ethos_title_colored%`
- `%ethos_title_id%`
- `%ethos_karma_title%`

## Configuration and data

- `config.yml` controls karma actions, decay, cooldowns, anti-farm rules, title ranges, and karma effects.
- `titles.yml` defines title names, colors, descriptions, and unlock conditions.
- `endermen.yml` controls the plugin's Enderman behavior.
- `messages_en.yml` and `messages_ru.yml` contain localized messages.
- `ethos.db` stores karma, statistics, unlocked titles, and active titles by player UUID.

Back up `ethos.db` before replacing or restoring a production installation.

## Building and testing

```bash
./gradlew check
./gradlew build
```

The release JAR is written to `build/libs/ethos-1.0.0.jar`. Tests use JUnit 4 and MockBukkit; JaCoCo reports are
generated in `build/reports/jacoco/test/html/`.

## License

Copyright (c) 2026 Nyansus. Released under the [MIT License](LICENSE).
