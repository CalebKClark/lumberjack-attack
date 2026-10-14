# Architecture notes

## Composition
`Main` (`scripts/game/Main.cs`) is the **composition root**: it constructs services, routes input, and ticks the world. Domain logic lives in focused services (`ChopService`, `PlaceService`, `FishingService`, …), not in `Main`.

## Data-first content
Prefer CSV / map files under `data/` over new C# branches. Catalogs load through `CsvUtil` where possible.

## Placeables
`ItemDef.PlaceableKind` (from `placeable_kind`) drives place / open / break behavior. Adding a new furniture type:
1. Add the item row + kind in `items.csv`
2. Add art under `assets/sprites/items/`
3. If it needs new UI or rules, extend `PlaceableKind` + a small handler — avoid scattering `if (itemId == "...")` checks

## Player vitals
`PlayerHealthState.SetMaxHealth` is synced from `EffectiveCombatStats` on HUD refresh so held gear and live HP stay aligned for future combat.

## Extension hooks
`GameEvents` is the seam for Steam achievements, analytics, or quests. Raise from gameplay; implement subscribers in a future `scripts/platform/` layer.
