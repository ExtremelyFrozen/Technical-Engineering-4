# Content Overview

Kenergy Engineering Retechnicalized (KER) is a remake branch of Technical Engineering 4 — a tech mod about processing and automation infrastructure.
Engines make power, cables carry it, machines process, pipes and channels move the output away, and batching scales it up. The whole chain lives in one mod.
This page lists the content; recipes, timings and numbers belong to the in-game guide and to your recipe viewer.

Platform: Minecraft 1.21.1 / NeoForge

## Processing machines

Eight machines, one shared recipe model, one batching rule, one stall behaviour — learn one, you know them all. A cycle runs several units at once and waits when input, output room or energy falls short.

| Machine | What it does |
|---|---|
| Smelter | Runs vanilla furnace recipes on power; upgrades switch it to blasting or smoking |
| Pulverizer | Grinds ores and metals into dust for more output, with extra byproducts |
| Compressor | Presses plates, rods, gears and wires with moulds; the mould is not consumed |
| Refiner | Refines back and forth between item and fluid form to push materials up the chain |
| Induction Furnace | Combines several inputs into alloys |
| Psionicant | Pairs two inputs in fixed combinations to synthesise hard-to-get materials |
| Enchantment Flusher | Moves enchantments from a tool onto a book or a blank tool and turns them into Liquid XP |
| Matter Condenser | Burns a catalyst to condense Bizarrerie fluid continuously |

## Environmental and field machines

Seven machines, no recipe slots — they work from their own position and facing. Progress survives low power, a redstone cut or a full output, and resets when the input disappears.

| Machine | What it does |
|---|---|
| Cooler | Sits against a machine's front face and cuts its processing time, burning coolant |
| Beacon Simulator | Keeps a wide area under a potion effect, using one potion item as a template |
| Mob Ripper | Clears the mobs behind it with the weapon in its weapon slot |
| Quarry | Digs down through the entire chunk it sits in, all the way to the dimension floor; standard or fast scan |
| Farm Manager | Takes over the farmland behind it, replanting and harvesting mature crops |
| Block Breaker | Breaks blocks in front of it at the cost of tool durability |
| Block Former | Places blocks in front of it, refilling from its candidate bar |

## Engines

All four default their energy face to output and push power into adjacent machines or cables.

| Machine | What it does |
|---|---|
| Extraction Engine | Burns ordinary fuel |
| Metal Engine | Burns metal ingots |
| Biomass Engine | Burns leaves, logs and other plant matter |
| Solar Engine | Runs on sky light, so it needs open sky and clear weather |

## Energy and transmission

| Device | What it does |
|---|---|
| Energy Cell | A power buffer that also charges and drains battery items |
| Energy Module | A portable battery that quietly recharges powered gear on you and in your curio slots |
| Energy Cables | Four tiers from thin to thick, auto-connecting to neighbours, placeable underwater |
| Item Pipes | Normal, whitelisting and blacklisting variants; only a side set to pull reaches into a machine |
| Spanner | Rotates a machine, and sneak-right-click picks it up with its contents and upgrades intact |

## Channels

Shared storage that crosses dimensions and machines, split into three types — two channels with the same name but different types never talk to each other. The channel connector attaches machines to a channel, and channel blocks work as ordinary containers too.

| Channel | What it does |
|---|---|
| Item Channel | Shared stock; the more machines attached, the more of one item it holds |
| Fluid Channel | One shared tank |
| Energy Channel | One shared buffer |

## Face config and upgrades

Every face of a machine is set on its own — in, out, passive or off, with both as the default. Redstone control lives in the same screen. When a machine stalls, check face permissions first, then room in the input and output, then redstone and power. Range machines can draw their working area, and mining and breaking can apply tool enchantments.

Upgrades go into the upgrade slots of the machine screen; anything a machine does not support is refused outright.

| Upgrade | What it does |
|---|---|
| Powered Kit | Faster and stronger, plus extra batch |
| Augmented Kit | Faster and stronger, more gently |
| Shulker Kit | The biggest speed, power and batch gains; found in end city chests |
| Photosynthetic Power | Feeds the machine a little power in open sky during dry daytime, at the cost of longer processing |
| Range Expansion | Widens the working radius of range machines |
| Blasting Professor | Moves the Smelter to blasting recipes; exclusive with smoking |
| Smoking Professor | Moves the Smelter to smoking recipes; exclusive with blasting |
| Knowledge Expansion | The Smelter yields Liquid XP alongside each smelt |
| Mineral Detection | The Quarry pulls only ores |
| Frozen Soil Drilling | The Quarry outputs ice |
| Mantle Drilling | The Quarry outputs lava |
| Potion Effect Extraction | Raises the Beacon Simulator effect by one amplifier |
| Starlight Energy Deliverance | Placeholder, currently does nothing |

## Materials

| Category | What is there |
|---|---|
| Metals and ores | Tin, nickel, powered tin, chlorium and mushrium alongside the usual iron, copper, gold and gem materials |
| Worked shapes | Dust, plate, rod, gear and wire, plus raw and storage forms |
| Moulds | Nine of them, deciding what the Compressor produces |
| Fluids | Liquid XP, honey, royal jelly, spicy jelly and bizarrerie, all bucket-carriable |
| Components | Redstone parts and AI chips, hydraulic widget, azure glass, indigo |

Every material is registered under common tags, so other mods can add members and our machines will take them.

## Mod compatibility

| Mod | What you get |
|---|---|
| JEI (recommended) | A recipe page for every machine, three for the Smelter, plus engine fuel and coolant pages |
| EMI | Machine and fuel pages are implemented and still being verified in-game |
| Jade | Shows channel attachment and whether a pipe side is pulling |
| GuideME (optional) | Required for the guide book; the mod runs fine without it |
| Curios API (optional) | Lets the Energy Module charge curio-slot gear |
| AE2 ecosystem | Not a requirement; machine interfaces follow the read and write rules logistics mods expect, so pattern providers and similar devices can attach. With AE2 or its addons (ExtendedAE, Data Energistics, AE2 Crystal Science) installed, their crystals can also be ground into that mod's own dust in the Pulverizer |
| Ender IO, Ender IO: Evolution, Mekanism, Immersive Engineering, All the Ores, Allthemodium, NEO ECO AE Extension | The Pulverizer, Compressor and Induction Furnace pick up their ore and material chains, and a recipe with missing input disappears instead of showing something you cannot run |

## In-game guide

The Kenergy Engineering Guide needs GuideME and covers everything from powering your first machine to face config and the order a batch lock is evaluated.
