# World editor

Title-menu map editor (`scenes/WorldEdit.tscn`). New maps start as **5×5** of cabin-wall gray `blank` tiles (id 27). Rotations and transfer markers round-trip in `.map` files the same way as Java / in-game loading.

## Controls

- **LMB** paint · **RMB** erase to blank · **Q/E** or Rot L/R brush rotation
- **Scroll** zoom · **WASD** pan · **Ctrl+LMB** drag pan · **Z** undo
- Edge **+/-** buttons grow/shrink map on that side (new cells = blank)
- Palette includes transfer marker **T** (editor-only `t` suffix)

## Disconnect for release

1. Remove the `ENABLE_WORLD_EDIT` define from `LumberjackAttack.csproj` (the PropertyGroup near the bottom).
2. Optionally exclude `scenes/WorldEdit.tscn` and `scripts/editor/` from the export preset.

With the define removed, `GameConfig.EnableWorldEdit` is false and the title **World Edit** button is hidden.
