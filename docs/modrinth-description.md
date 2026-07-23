# Rat Cheese Helper 🧀🐀

A client-side Fabric mod for the **Rat pet** on Hypixel Skyblock.

The Rat is the only morph pet in the game: you turn into a rat, sniff out cheese that spawns nearby, and collecting it grants a random player Magic Find — while you get a chance to copy their drops (RAT BLESSING). The cheese only exists for ~10 seconds, and vanilla gives you nothing but a chat message. This mod fixes that.

## Features

### 🔦 Cheese highlight
- While the cheese is **in your line of sight**, it gets a glowing outline (configurable color) and can be **rendered up to 5× bigger** so you actually spot it in time. Nothing is revealed through opaque blocks.
- Detection works out of the box (the cheese head texture is built in), with a `/ratcheese scan` debug tool as a fallback.

### 📊 Buff HUD
- Who you buffed, how much Magic Find, and a live countdown: `You → Player: +7✯ MF, 43s`
- Buffs other rats put **on you**.
- Rat loot counter + a flash line whenever a RAT BLESSING drop arrives.
- Names are colored by the player's rank, numeric countdown can be swapped for a **shrinking timer bar**.
- Fully movable and scalable: drag it, scroll to resize, arrow keys to nudge (`/ratcheese hud`).

### 🔔 Alerts
- On-screen title + sound the moment you smell cheese (both optional).

### 🏆 Achievements
- 24 cheese-themed achievements, 7 of them secret (some with cryptic hints).
- Unlock effects with titles and sounds, lifetime stats, unlock dates.
- `/ratcheese achievements`

## Commands
`/ratcheese` — settings (also available via Mod Menu) · `/ratcheese hud` — move/scale the HUD · `/ratcheese achievements` — achievement list · `/ratcheese test` — preview the HUD with sample data · `/ratcheese scan` — debug nearby prop entities

## Notes
- Requires **Fabric API**. Optional: Mod Menu.
- Client-side only, parses only your own chat messages, automates nothing, and does not render anything through opaque blocks.
- Use at your own risk, like any Hypixel mod. Not affiliated with Hypixel.

---

## Русский

Клиентский мод для пета **Крыса** (Rat) на Hypixel Skyblock: подсветка (в прямой видимости) и увеличение сыра (живёт всего ~10 секунд!), HUD с баффами Magic Find и таймерами, счётчик крысиного лута, алерты о сыре и 24 ачивки с секретками. Настройки — `/ratcheese` или через Mod Menu, русская локализация в комплекте.
