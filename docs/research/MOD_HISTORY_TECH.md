# Technical mod Chao research — 2026-10-29

This is reference research, not an implementation or a compatibility test. All
models/accessories suggested below should be newly authored, keeping the existing
8×6.5×6.5 Chao head and relaxed hanging arms. Publisher CurseForge projects and
official source metadata were inspected. No third-party meshes/textures are needed.

## Suggested collection

The first three were named by the user. The other ten expand the collection across
the history of technical Minecraft mods. Item IDs in the optional catalyst column
are suggestions to resolve against the installed registry, not compile-time imports.
The vanilla fallback is for an original Chao Garden recipe **with the same mod-loaded
condition**; it must never bypass the dependency gate.

| Variant key / name | Required mod ID(s) | Original visual signature | Optional catalyst; mod-gated vanilla fallback | Publisher-listed target status |
| --- | --- | --- | --- | --- |
| `create` / Clockwork Chao | `create` | Warm brass/copper coat, cyan-lens brow goggles, toothed cog crest and turning cog marker | `create:brass_ingot`; copper + gold + redstone | NeoForge 1.21.1 Create 6.0.10 listed |
| `ae2` / Fluix Chao | `ae2`; old alias `appliedenergistics2` | Violet fluix crystal crown, grey circuit collar, luminous violet storage-cell marker | `ae2:fluix_crystal`; amethyst + quartz + redstone | NeoForge 1.21.1 AE2 19.2.18 listed |
| `refined_storage` / Quartz Network Chao | `refinedstorage` | White quartz chassis, cyan disk inset, paired square antennas and disk marker | `refinedstorage:quartz_enriched_iron`; quartz + iron + redstone | NeoForge 1.21.1 RS 2.0.9 listed |
| `mekanism` / Osmium Chao | `mekanism` | Teal/steel coat, small respirator brow band, paired rear tanks and green energy core | `mekanism:alloy_infused`; iron + redstone + slime ball | NeoForge 1.21.1 10.7.19.85 listed |
| `immersive_engineering` / Engineer Chao | `immersiveengineering` | Leather goggles, copper coil backpack, steel rivets and orange signal-light marker | `immersiveengineering:ingot_steel`; iron + copper + leather | NeoForge 1.21.1 12.4.2-194 listed |
| `pneumaticcraft` / Pressure Chao | `pneumaticcraft` | Vented grey helmet, red air tanks, pressure gauge marker and yellow warning accents | `pneumaticcraft:ingot_iron_compressed`; iron + redstone + glass | NeoForge 1.21.1 8.2.23 listed |
| `computercraft` / Turtle Chao | `computercraft` | Grey terminal belly with green pixel cursor, compact turtle-shell backpack, floppy-disk marker | `computercraft:computer_normal`; stone + redstone + glass pane | NeoForge 1.21.1 files listed; CC:Tweaked retains the `computercraft` ID |
| `enderio` / Conduit Chao | `enderio` | Darksteel coat, coloured conduit spine, ender-pearl chest core and antenna rods | `enderio:vibrant_alloy_ingot`; iron + ender pearl + redstone | NeoForge 1.21.1 8.2.12-beta listed |
| `actually_additions` / Laser Crystal Chao | `actuallyadditions` | Charcoal/white lens collar, cyan crystal crest, small cubic laser-emitter marker | `actuallyadditions:restonia_crystal`; redstone + quartz + diamond | NeoForge 1.21.1 1.3.26 beta listed |
| `railcraft` / Conductor Chao | `railcraft` | Blue conductor cap with red band, steel/copper bogie feet, signal-lantern marker | Optional installed steel ingot tag; rail + iron + redstone torch | Railcraft Reborn NeoForge 1.21.1 1.2.10 listed |
| `thermal` / Flux Chao | `thermal`, `thermal_foundation`, or `thermal_expansion` | Electrum coat, orange/blue split tips, redstone coil backpack and gear marker | Optional installed Thermal RF coil; gold + redstone + blue dye | Official Thermal Foundation/Expansion currently list 1.20.1, so the 1.21.1 gate is dormant until a compatible implementation exists |
| `industrialcraft` / Rubber Circuit Chao | `ic2` (verified in the Nostalgia NeoForge manifest; older 1.7 releases used `IC2`) | Yellow rubber suit accents, black cable tail, battery backpack and circuit inset | Resolve installed circuit item; iron + dried kelp + redstone | IC2 Classic publisher currently lists 1.19.2. A separate unofficial IC2 Nostalgia project lists 1.21.1 NeoForge; do not describe it as an official IC2 release |
| `buildcraft` / Quarry Chao | `buildcraftcore` | Yellow hardhat, black safety stripe, bronze gears and a glass-pipe marker | Resolve installed gear; gold + glass + iron pickaxe | Original BuildCraft is historical; BuildCraft Community Edition explicitly lists 1.21.1 NeoForge and preserves modular IDs including `buildcraftcore` |

## Gate design implications

- These should be presence gates only: no optional mod class must be imported or
  loaded. Resolve the installed mod list on the server.
- CC:Tweaked is **not** named `cctweaked` in the mod list.
- Railcraft Reborn is **not** named `railcraft_reborn` in its manifest.
- Ender IO's main mod is `enderio`; `endercore` alone is only its support library
  and must not unlock the themed Chao.
- AE2 1.21.1 uses `ae2`; `appliedenergistics2` is a legacy alias, not its present ID.
- Thermal Foundation declares `thermal_foundation`, and depends on common
  `thermal`. The theme can accept either its actual core or one of the named
  Thermal modules; never require CoFH Core alone.
- BuildCraft CE retains `buildcraftcore`, `buildcraftlib`, `buildcraftfactory`, etc.
  Gate the Chao on the actual content/core mod, not only `buildcraftlib`.
- Presence detection does not make older Forge/Fabric jars compatible with
  NeoForge 1.21.1. Document historical gates honestly; do not claim tested binary
  compatibility with all these mods simply because their IDs are recognized.

## Sources

Publisher/project pages (both identity/theme and listed Minecraft/loader support):

- https://www.curseforge.com/minecraft/mc-mods/create
- https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2
- https://www.curseforge.com/minecraft/mc-mods/refined-storage
- https://www.curseforge.com/minecraft/mc-mods/mekanism
- https://www.curseforge.com/minecraft/mc-mods/immersive-engineering
- https://www.curseforge.com/minecraft/mc-mods/pneumaticcraft-repressurized
- https://www.curseforge.com/minecraft/mc-mods/cc-tweaked
- https://www.curseforge.com/minecraft/mc-mods/ender-io/files/all
- https://www.curseforge.com/minecraft/mc-mods/actually-additions/files/all
- https://www.curseforge.com/minecraft/mc-mods/railcraft-reborn
- https://www.curseforge.com/minecraft/mc-mods/thermal-foundation
- https://teamcofh.com/downloads/
- https://www.curseforge.com/minecraft/mc-mods/ic2-classic
- https://www.curseforge.com/minecraft/mc-mods/industrial-craft-2-nostalgia
- https://www.curseforge.com/minecraft/mc-mods/buildcraft-community-edition
- https://www.curseforge.com/minecraft/mc-mods/buildcraft-community-edition/files/8709018

Actual mod IDs verified from first-party source metadata/API constants:

- https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/gradle.properties
- https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/src/main/java/appeng/api/ids/AEConstants.java
- https://github.com/refinedmods/refinedstorage/blob/develop/src/main/resources/META-INF/mods.toml
- https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/resources/META-INF/neoforge.mods.toml
- https://github.com/BluSunrize/ImmersiveEngineering/blob/1.21.1/src/main/templates/META-INF/neoforge.mods.toml
- https://github.com/TeamPneumatic/pnc-repressurized/blob/1.21/src/main/resources/META-INF/neoforge.mods.toml
- https://github.com/cc-tweaked/CC-Tweaked/blob/mc-1.20.x/projects/forge/src/main/resources/META-INF/mods.toml
- https://github.com/Team-EnderIO/EnderIO/blob/1.21.1/enderio/src/main/templates/META-INF/neoforge.mods.toml
- https://github.com/Ellpeck/ActuallyAdditions/blob/main/src/main/java/de/ellpeck/actuallyadditions/api/ActuallyAdditionsAPI.java
- https://github.com/railcraft-reborn/railcraft/blob/1.21.x/src/main/templates/META-INF/neoforge.mods.toml
- https://github.com/CoFH/ThermalFoundation/blob/1.20.x/src/main/resources/META-INF/mods.toml
- https://github.com/shipovskijkorp/BuildCraft/blob/main/source-platforms/neoforge/src/main/resources/META-INF/neoforge.mods.toml
- https://www.curseforge.com/minecraft/mc-mods/industrial-craft-2-nostalgia/files/9217244 — inspected the published jar's `META-INF/neoforge.mods.toml`, confirming `modId="ic2"`; no code was executed.

Additional official visual/resource cross-checks:

- https://guide.appliedenergistics.org/1.21.1/items-blocks-machines/fluix_crystal/
- https://refinedmods.com/refined-storage/utility/quartz-enriched-iron.html
- https://tweaked.cc/
- https://github.com/Team-EnderIO/EnderIO/blob/1.21.1/docs/README.md

No game was launched with these third-party jars during this research. Optional
catalyst IDs should be registry-checked if used; the original mod-gated vanilla
fallback avoids unresolved-item recipes when historical mods return under a
compatible loader/version.
