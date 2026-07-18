# Changelog

All notable changes to **Rat Cheese Helper** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versions follow [Semantic Versioning](https://semver.org/).

## [1.0.1] - 2026-07-18

### Fixed
- False-positive highlighting of unrelated "cheese" props — Garden vermin traps
  baited with Tasty Cheese, Blue Cheese Goblin Omelettes, etc. Detection now
  relies on the exact cheese head texture by default; name markers are empty
  and opt-in via `/ratcheese addname`. Old configs containing only the legacy
  `"cheese"` marker are migrated automatically.

## [1.0.0] - 2026-07-18

Initial release.

### Added
- **Cheese highlight**: glow through walls with a configurable color and up to
  5× visual size. Scaling works for any entity type (armor stands, dropped
  items, item displays). The Hypixel cheese head texture is built in, with
  `/ratcheese scan` / `addtexture` / `addname` as a fallback if it ever changes.
- **Buff HUD**: outgoing and incoming Magic Find buffs with live countdowns,
  player names colored by rank, optional shrinking timer bars (ScathaPro
  style), rat loot (RAT BLESSING) counter with a flash line for fresh drops,
  and a "You smell CHEESE nearby!" indicator. Movable and scalable via a
  drag-and-drop editor (`/ratcheese hud`): mouse drag, scroll to scale,
  arrow keys to nudge, right click to reset.
- **Alerts**: full-screen title and sound when cheese spawns nearby (both
  toggleable).
- **Achievements**: 24 cheese-themed achievements, 7 of them secret (some with
  cryptic hints), unlock dates, lifetime stats and a card-style list UI
  (`/ratcheese achievements`). Unlock effects with titles, chat messages and
  sounds.
- **Mod Menu** integration and a built-in settings screen (`/ratcheese`).
- English and Russian localization.
