# ChangeLog

## [1.0.0] — P5-T1 old save compatibility
- **readTileData unconditional progress reset**: loading old/pre-refactor NBT now resets `progress`, `maxProgress`, `lockedB`, `lockedMaxProgress` to 0 regardless of value. Previously only values exceeding 72000 ticks were reset, missing low-value old-format saves (e.g. furnace ~3000 FE). Without a schema version field to distinguish formats, unconditional reset is the only safe strategy. Inventory, energy storage, upgrades, and face configuration are preserved on load. Next `conditionStart()` rebuilds tick-based progress from the current recipe/effect.
