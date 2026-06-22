# Dog Levels

A Minecraft Forge mod that adds a **leveling system for tamed wolves**. Dogs gain XP from combat, level up to grow stronger and larger, and unlock unique abilities at milestone levels so they stay viable through endgame.

**Minecraft 1.21.1 — Forge 52.1.2 — v1.4.0**

---

## Features

### Leveling & Stats

- Dogs gain XP by killing mobs or assisting their owner (configurable range: 32 blocks)
- Level cap: 30 (configurable up to 1000)
- Each level grants stat bonuses:
  - **Health** (+1.0 per level)
  - **Attack Damage** (+0.5 per level)
  - **Movement Speed** (+0.002 per level)
  - **Armor** (+0.5 per level)
  - **Knockback Resistance** (+0.02 per level)
- Dogs visually grow in size as they level up (up to +60%)

### Abilities

All unlock levels and tuning values are configurable in `doglevels-common.toml`.

| Level (default) | Ability | Description |
|------------------|---------|-------------|
| 5 | **Splash Bite** | Attacks deal 50% splash damage to nearby hostile mobs |
| 10 | **Fire Resistance** | Dog gains permanent fire resistance |
| 15 | **Pack Hunter** | Howl grants Strength to nearby tamed wolves and the owner |
| 20 | **Howl of Vitality** | 20% chance to heal the dog (and partially the owner) on attack |
| 25 | **Lifesteal** | 25% of damage dealt is healed to the dog |
| 30 | **Bonded Endurance** | Permanent Regeneration II effect |

All abilities can be individually toggled in the config.

### Behavior Modes

Right-click a dog with an empty hand (sneaking) to open the **Dog Stats** GUI, where you can change its behavior:

- **DEFAULT** — Only attacks hostile mobs that attack you or the dog
- **AGGRESSIVE** — Automatically attacks hostile mobs within 16 blocks (configurable)
- **PASSIVE** — Never attacks any entity

### Items

- **Treat** — Feed to a tamed wolf for instant XP (craftable, configurable XP amount)

### Commands

- `/doglevels info [entityId]` — View dog's level, XP, and behavior. If no entityId is given, raytraces the wolf the player is looking at.
- `/doglevels behavior <entityId> <mode>` — Set behavior mode remotely
- `/doglevels setlevel <targets> <level>` — (Admin, OP 2) Set a dog's level. Accepts `@e` selectors.
- `/doglevels addxp <targets> <amount>` — (Admin, OP 2) Add XP to a dog. Accepts `@e` selectors.
- `/doglevels reset <targets>` — (Admin, OP 2) Reset a dog to level 1, 0 XP, DEFAULT behavior. Accepts `@e` selectors.

---

## Configuration

A `doglevels-common.toml` file is generated after first run. Server owners can adjust XP rates, stat growth, size scaling, ability unlock levels, ability tuning, behavior range, and enable/disable individual abilities.

Key config options (defaults shown):

| Option | Default | Description |
|--------|---------|-------------|
| `max_level` | 30 | Maximum level (1–1000) |
| `xp_per_level_base` | 20 | Base XP for level 1→2 |
| `xp_per_level_growth` | 10 | Extra XP per level beyond base |
| `xp_from_kill_health_mult` | 1.0 | XP = victim max health × this |
| `xp_from_player_kill_mult` | 0.5 | Assist XP multiplier (0 = disabled) |
| `xp_from_treat` | 50 | XP from feeding a Treat |
| `health_per_level` | 1.0 | Bonus health per level |
| `damage_per_level` | 0.5 | Bonus damage per level |
| `size_per_level` | 0.015 | Visual size increase per level |
| `splash_bite_level` | 5 | Level at which Splash Bite unlocks |
| `splash_bite_radius` | 2.5 | Splash Bite radius (blocks) |
| `splash_bite_damage_mult` | 0.5 | Splash Bite damage multiplier |
| `pack_hunter_period_ticks` | 100 | How often Pack Hunter re-buffs (ticks) |
| `howl_of_vitality_chance` | 0.2 | Howl of Vitality trigger chance (0–1) |
| `howl_of_vitality_heal` | 1.0 | Howl of Vitality heal amount |
| `lifesteal_percent` | 0.25 | Lifesteal fraction of damage dealt |
| `aggressive_range` | 16.0 | AGGRESSIVE auto-target range (blocks) |

(Full list in `doglevels-common.toml` after first run.)

---

## Installation

1. Install Minecraft Forge 52.1.2 for Minecraft 1.21.1
2. Download the `doglevels-1.4.0.jar` from [Releases](https://github.com/ollezetterstrom/Doglevels/releases)
3. Place the JAR in your `mods` folder
4. Launch the game

### Building from source

```bash
git clone https://github.com/ollezetterstrom/Doglevels.git
cd Doglevels
./gradlew build
```

The built JAR will be in `build/libs/`.

### Running tests

```bash
./gradlew test
```

Tests are written with JUnit 5 and cover the leveling math, ability thresholds, behavior persistence, and the 1.4.0 fixes (max-level liveness, cache isolation, NBT round-trip).

---

## Development

This is a standard Forge MDK project. Import into IntelliJ IDEA or Eclipse:

```bash
./gradlew genIntellijRuns   # IntelliJ
./gradlew genEclipseRuns    # Eclipse
```

Run the client with `./gradlew runClient`. Tests are under `src/test/`.

---

## Networking

Starting in 1.4.0, the mod uses a proper `SimpleChannel` (`doglevels:main`) to sync each dog's level/XP/behavior from server to client. The packet is sent on:

- Level-up
- XP gain (from kills, assists, or treats)
- Behavior change (from the GUI or command)
- Player start-tracking (so freshly logged-in players see the correct state immediately)

The render scale handler falls back to inferring the level from the synced `MAX_HEALTH` attribute modifier when no sync packet has arrived yet (e.g. on a vanilla server without the mod).

---

## License

MIT — see [LICENSE.txt](LICENSE.txt).

## Credits

Author: **SuperZ** (ZMod)
