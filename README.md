# Rat Cheese Helper 🧀🐀

**English** · [Русский](README.ru.md)

A client-side Fabric mod for Hypixel Skyblock (Minecraft **26.1.2**) built around the **Rat pet** mechanic.

![Build](https://github.com/Racapio/RatCheese/actions/workflows/build.yml/badge.svg)

## Features

1. **Cheese highlight.** The cheese your rat spawns only lives for ~10 seconds. The mod makes it glow through walls (configurable color) and renders it **up to 5× bigger** so you can reach it in time.
2. **Buff HUD:**
   - who you buffed: `You → Zellion63: +7✯ MF, 43s` — the name is colored by the player's rank, the countdown can be swapped for a shrinking timer bar;
   - buffs other rats put on you;
   - rat loot counter (RAT BLESSING) plus a flash line for every fresh drop;
   - a "You smell CHEESE nearby!" line until the cheese is collected or despawns.
3. **Alerts** — full-screen title + sound when cheese appears (both optional).
4. **24 achievements** — from "First Sniff" to "Rat King", 7 of them secret (some with cryptic hints), unlock dates and lifetime stats.

## Installation

1. [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.1.2 (loader ≥ 0.19.3).
2. Into `mods`:
   - [Fabric API](https://modrinth.com/mod/fabric-api) for 26.1.2;
   - `ratcheese-<version>.jar` (see [Releases](https://github.com/Racapio/RatCheese/releases) or build it yourself);
   - optionally [Mod Menu](https://modrinth.com/mod/modmenu) — settings right from the mod list.

## Commands

| Command | Description |
|---|---|
| `/ratcheese` | Settings (highlight, glow color, cheese size, HUD, alerts) |
| `/ratcheese hud` | HUD editor: drag with mouse, scroll to scale, arrow keys to nudge, right click to reset |
| `/ratcheese achievements` | Achievement list with progress |
| `/ratcheese test` | Inject sample data to preview the HUD |
| `/ratcheese reset` | Reset session buffs and the loot counter |
| `/ratcheese scan` | Debug: list prop entities within 8 blocks |
| `/ratcheese addname <text>` / `addtexture <hash>` | Extra cheese-detection markers |

Config: `.minecraft/config/ratcheese.json`, achievement data: `ratcheese_achievements.json`.

## Cheese detection

The texture hash of Hypixel's cheese head is built into the mod — the highlight works out of the box. If Hypixel ever changes it: stand next to a cheese, run `/ratcheese scan` and click the green **[+]** next to the right head — the texture is added to your config.

## Building from source

Requires JDK 25:

```
gradlew build
```

The jar ends up in `build/libs/`.

## Disclaimer

Client-side only: the mod automates nothing and reads only your own chat. As with any Hypixel mod — use at your own risk. Not affiliated with Hypixel.
