# ChangeLog

## [2.0.0] — Pipe hop-by-hop redesign + machine IO lifecycle (化繁为简)
- **Pipe**: removed the upgrade system (Pull/Push/Speed/Page/Ender items and levels) and the network-level root matching + active/passive complement roles. Pipes now use a **hop-by-hop transfer model**: every pipe ticks independently, pulls from adjacent containers into an internal buffer (64 items/tick) and pushes the buffer to adjacent containers (arrival) or adjacent pipes (relay); items move one hop per tick. All pipes share a unified **64 items/tick** rate. Buffer is persisted in tile data (items in pipes survive saves).
- **Pipe pull points**: use the Spanner (扳手) on a pipe's connection face to toggle it as an active **pull point**; unconfigured faces are passive IO (the pipe does not actively pull from them).
- **Machine IO lifecycle**: face modes now have explicit semantics — 无(OFF)=disabled, 被动输入(BE_IN)=passive input only (external may insert, machine does not pull), 被动输出(BE_OUT)=passive output only (external may extract), 被动双向(BOTH)=passive in+out, 主动输入(IN)=machine actively pulls from adjacent container (64/tick), 主动输出(OUT)=machine actively pushes output slots to adjacent container (64/tick). Item face gating now only allows external insert on BE_IN/BOTH and external extract on BE_OUT/BOTH.
- **Face config ecosystem**: faceData client mirror now syncs fully (initial @DescSynced + syncAllFacesToClients on change, all 6 faces x 3 types), fixing IO icons not updating until GUI reopen.
- **Breaking**: old pipe networks with upgrades rely on the new pull-point config; machine faces set to IN/OUT now require the machine's own active IO (previously external-facing).

## [1.1.0] — Pipe active/passive complementary IO
- **Pipe network IO redesigned to active/passive complement model**: previously a network needed a Pull-upgraded source pipe (sticky piston) AND a Push-upgraded target pipe (piston) paired to transfer items. Now active endpoints (Pull or Push upgraded) define network direction, and passive endpoints (no Pull/Push upgrade) automatically adopt the complementary role — upgrade one end, the other complements automatically. See `docs/pipe-transport-redesign.md` §3.
  - Active Pull (no Push) → passive endpoints become Push
  - Active Push (no Pull) → passive endpoints become Pull
  - Active Pull+Push both present → passive neutral
  - No active endpoint → network does not transfer
- **Breaking**: networks that relied on auto/passive transfer without upgrades now stay idle. Add a Pull (sticky piston) or Push (piston) upgrade to one pipe to reactivate.

## [1.0.0] — P5-T1 old save compatibility
- **readTileData unconditional progress reset**: loading old/pre-refactor NBT now resets `progress`, `maxProgress`, `lockedB`, `lockedMaxProgress` to 0 regardless of value. Previously only values exceeding 72000 ticks were reset, missing low-value old-format saves (e.g. furnace ~3000 FE). Without a schema version field to distinguish formats, unconditional reset is the only safe strategy. Inventory, energy storage, upgrades, and face configuration are preserved on load. Next `conditionStart()` rebuilds tick-based progress from the current recipe/effect.
