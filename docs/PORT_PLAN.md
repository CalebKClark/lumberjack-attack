# Lumberjack Attack — Godot port plan

The Java/LibGDX playtest remains at git tag `v0.1.0` and branch `v0.1-libgdx`.
This tree is the C# / Godot 4 line, version 0.11.

## Principles
- Copy art/audio; do not move or delete Java assets.
- Port systems in slices that stay playable.
- Prefer Godot TileMap for maps (replaces custom Java world editor).
- Keep data-driven content (CSV → Godot resources or stay CSV under `data/`).

## Milestone order
1. **Boot** — project opens, C# runs (done)
2. **Tiles + homestead map** — TileSet from PNG + load `data/homestead.map` (done)
3. **Player move + camera** — WASD, collision with solid tiles (done)
4. **Cabin ↔ homestead** — door transitions + camera snap (done)
5. **Chop trees** — click pine, axe swing, stump, log rain, pickup, 30s regrowth (done)
6. **Inventory UI + time/sleep** — hotbar, energy, day clock (done)
7. **Fishing (basic)** — rod cast → wait → catch (minigame later) (done)
8. **Crafting** — recipe list, consume logs, craft chipper/etc. (done)
9. **Saves** — F5/F9 JSON slot1, map cache, sleep+autosave (done)
10. **Polish + export** — then later content (Aspen, dungeon) as 0.2
11. **Tile rotation import** — apply `id:rot` from `.map` files (noted)
12. **Overlay tiles** — stump/bridge draw over grass/water base, not empty behind (noted)
13. **Daylight cycle polish** — full day/night presentation later; clock already ticks (noted)
14. **Fishing minigame** — flash bite + hook minigame (noted)
15. **Recipe unlocks** — discovery-gated recipes (noted; all unlocked for now)
16. **Save UI / multi-slot / title** — pause menu + named saves (noted; slot1 only for now)

## Folder map
| Path | Role |
|------|------|
| `assets/` | Your sprites, music, SFX |
| `scenes/` | Godot scenes (UI, maps, main) |
| `scripts/` | C# (core, game, world, entities) |
| `data/` | Items, recipes, fish tables, etc. |
