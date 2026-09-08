# Lumberjack Attack

A 2D top-down homestead game in **Java 17** and **LibGDX**.

Chop trees, fish, craft better tools, manage energy, and sleep through the day.

**Version:** 0.1.0 (released 2026-08-15)

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://adoptium.net/)
[![LibGDX](https://img.shields.io/badge/LibGDX-1.12.1-red)](https://libgdx.com/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

---

## Features

- Homestead loop: chop trees → gather → craft → place → sleep
- Energy and food, through fish
- Fishing minigame, 3 different kinds of fish
- Cabin interior and outdoor homestead locations
- Save / load / settings
- Crafting, collections, chests and a wood chipper

---

## How to play
### Controls - can all be rebound if wanted

- **WASD** — move
- **Mouse** — aim / click to interact (chop, fish, place, attack)
- **1–9** / scroll — hotbar
- **E** — inventory / player menu
- **Esc** — pause / cancel (also reels in while fishing)
### Basic loop
1. Leave the cabin and explore the homestead.
2. Equip the starter axe, and chop pine trees for logs.
3. Fish the pond (cast, wait for the bite flash, then play the fishing minigame).
4. Eat fish to restore health and energy (actions cost energy).
5. Craft at the menu (chests, wood chipper, better axe, etc.).
6. Sleep in a bed to restore energy and advance the day.
7. Save from the menu so your world progress sticks.
Tip: keep an eye on the green energy bar — when it’s very low, you move slower.

---

## Vision
Lumberjack Attack is a solo passion project. All pixel art, sound, music, and gameplay are all my own creation and will continue to be what adds on to this project.
My goal for this project is to create a refined playable RPG title that I am happy with, and I can publish on steam. Aiming for a steam release sometime in 2027.
**Direction after 0.1 preparing for 0.2**
- New area: Aspen Forest
- First dungeon inside the Aspen Forest
- Combat system starts to fall into place
- Shovel item to dig up unwanted stumps
- tree seeds
- More polish on art, audio, and feel
The long-term goal is a cozy grind game with a bit of challenge, where chopping, exploring, and upgrading your lumberjack character is the game loop!

---

## Play

1. Download **`LumberJackAttack-0.1.zip`** from [Releases](../../releases) (when published)
2. Install **Java 17+** ([Adoptium](https://adoptium.net/))
3. Unzip → run `run.bat` → **New Game**

Windows playtest build.

---

## Build from source

```bat
gradlew run
```

Requires JDK 17+.

---

## Layout

```
src/lumberjack/   game code
resources/        maps, CSV data, sprites, audio
```

---

## License

MIT — see [LICENSE](LICENSE).
