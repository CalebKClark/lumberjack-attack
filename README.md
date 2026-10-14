# Lumberjack Attack

A 2D top-down homestead game. Chop trees, fish, craft, manage energy, and sleep through the day.

**Version 0.11** is a Godot 4 / C# port of the first playtest. It is past 0.1 and not 0.2 yet. Pixel art is original.

The Java 17 / LibGDX playtest is still available as [v0.1.0](https://github.com/CalebKClark/lumberjack-attack/releases/tag/v0.1.0) and on the `v0.1-libgdx` branch, including the Windows zip from that release.

## This version

- Homestead loop: chop, gather, craft, place, fish, eat, and sleep
- Fishing minigame
- Energy, skills, collections, inventory, chests, and a wood chipper
- Named saves from the title screen, plus settings for display and input
- Data-driven items, recipes, fish, and maps under `data/`
- Maps: homestead, cabin interior, aspen forest, a cave, and a dungeon entrance
- Menu music: "Lumberjack Attack Overture"
- A world editor on the title screen in this source tree (`ENABLE_WORLD_EDIT`)

Combat is not in yet. Extra maps are further along than the 0.1 homestead, and still short of a finished 0.2 area.

## Run 0.11

1. Install [Godot 4.7 .NET](https://godotengine.org/download) and the .NET 8 SDK
2. Open this folder in Godot
3. Let it import assets and build C#
4. Press **F5**. The title screen has New Game and Load

## Version history

| Version | Engine | Where |
| --- | --- | --- |
| 0.1.0 | Java 17, LibGDX | Tag `v0.1.0`, branch `v0.1-libgdx`. First playtest, August 2026. |
| 0.11 | Godot 4.7, C# | `main`. Engine port of the homestead loop. |

Notes for each version are in [CHANGELOG.md](CHANGELOG.md). How the systems fit together is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Layout

| Path | Role |
| --- | --- |
| `data/` | CSV catalogs and `.map` files |
| `scripts/game/` | Composition root (`Main`) and game events |
| `scripts/world/` | Maps, tiles, trees, loot, placeables |
| `scripts/ui/` | Title, HUD, pause menu, settings |
| `scripts/` | Inventory, crafting, fishing, skills, save, and the other systems |
| `assets/` | Sprites, font, and music |
