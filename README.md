# LumberJack Attack

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
