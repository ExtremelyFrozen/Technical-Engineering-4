# ChangeLog

## [1.1.0] — Pipe active/passive complementary IO
- **Pipe network IO redesigned to active/passive complement model**: previously a network needed a Pull-upgraded source pipe (sticky piston) AND a Push-upgraded target pipe (piston) paired to transfer items. Now active endpoints (Pull or Push upgraded) define network direction, and passive endpoints (no Pull/Push upgrade) automatically adopt the complementary role — upgrade one end, the other complements automatically. See `docs/pipe-transport-redesign.md` §3.
  - Active Pull (no Push) → passive endpoints become Push
  - Active Push (no Pull) → passive endpoints become Pull
  - Active Pull+Push both present → passive neutral
  - No active endpoint → network does not transfer
- **Breaking**: networks that relied on auto/passive transfer without upgrades now stay idle. Add a Pull (sticky piston) or Push (piston) upgrade to one pipe to reactivate.

## [1.0.0] — P5-T1 old save compatibility
- **readTileData unconditional progress reset**: loading old/pre-refactor NBT now resets `progress`, `maxProgress`, `lockedB`, `lockedMaxProgress` to 0 regardless of value. Previously only values exceeding 72000 ticks were reset, missing low-value old-format saves (e.g. furnace ~3000 FE). Without a schema version field to distinguish formats, unconditional reset is the only safe strategy. Inventory, energy storage, upgrades, and face configuration are preserved on load. Next `conditionStart()` rebuilds tick-based progress from the current recipe/effect.
