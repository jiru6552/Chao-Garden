# Technology theme icons - 0.13.0

These are original pixel-art interpretations of distinctive mod items and
devices, rather than extracted textures or official logos. Recognizable objects
replace the older generic gear, circuit, square, eye and energy marks. Their
canonical color cues belong to each motif instead of being recolored to match
the Chao's coat.

Each theme has a deliberately authored **8px egg mark** and a separate **16px
chest decal**. The small version is not a reduced large image: its outline,
negative space and defining colored areas are planned on its native grid.
The larger artwork adds edges and highlights without changing the object.
Both use binary transparency, with no smoothing or nearly transparent fringe.

| Theme | Identifying motif | Primary reference |
| --- | --- | --- |
| Create | Wooden cogwheel, andesite axle socket and shaped teeth | [Creators of Create cogwheel documentation](https://github.com/Creators-of-Create/Create/wiki/Cogwheel) |
| Applied Energistics 2 | ME Controller face, pale corners and segmented rainbow core | [AE2 1.21.1 controller guide](https://guide.appliedenergistics.org/1.21.1/items-blocks-machines/controller) |
| Refined Storage | Pale storage-disk housing and blue storage center | [Refined Mods storage disk](https://refinedmods.com/refined-storage/storage/1k-storage-disk.html) |
| Mekanism | Orange Atomic Disassembler head and green powered handle | [Official Mekanism Atomic Disassembler page](https://wiki.aidancbrady.com/wiki/Atomic_Disassembler) |
| Immersive Engineering | Broad steel Engineer's Hammer on a wooden handle | [Developer item registry](https://github.com/BluSunrize/ImmersiveEngineering/blob/1.21.1/src/main/java/blusunrize/immersiveengineering/common/register/IEItems.java) |
| Thermal Series | Redstone reception coil, with red windings and metal terminals | [Team CoFH materials catalog](https://teamcofh.com/docs/1.12/thermal-foundation/) |
| PneumaticCraft | Circular pressure dial and red needle | [Team Pneumatic developer repository](https://github.com/TeamPneumatic/pnc-repressurized) |
| CC: Tweaked | Stone computer bezel, dark terminal and green prompt | [Developer terminal documentation](https://tweaked.cc/module/term.html) |
| Ender IO | Ender pearl at the junction of four short conduit ports | [Developer conduit documentation](https://new.enderio.com/docs/technical/datapacks/custom_conduits/) |
| Actually Additions | Blue drill body and silver drill bit | [Developer drill changelog](https://github.com/Ellpeck/ActuallyAdditions/blob/main/CHANGELOG.md) |
| Railcraft | Steam locomotive front, chimney, lamp and red running board | [Railcraft Reborn developer repository](https://github.com/railcraft-reborn/railcraft) |
| IndustrialCraft | Yellow nuclear-reactor plaque and dark radiation trefoil | [IndustrialCraft nuclear reactor documentation](https://wiki.industrial-craft.net/index.php?title=Nuclear_Reactor) |
| BuildCraft | Orange open-ended pipe wrench | [BuildCraft developer repository](https://github.com/BuildCraft/BuildCraft) and [project site](https://mod-buildcraft.com/) |
| ProjectE | Red Philosopher's Stone with a golden frame | [ProjectE developer repository](https://github.com/sinkillerj/ProjectE) |
| Psi | Psimetal CAD silhouette, pale housing and cyan spell hardware | [Vazkii's official Psi site](https://psi.vazkii.net/) |
| Galacticraft | Tier 1 rocket, red nose, fins and blue window | [Official Galacticraft Tier 1 rocket documentation](https://galacticraft.mods.wiki/wiki/Tier_1_Rocket) |

The Ender IO pearl/conduit combination and IndustrialCraft reactor plaque are
compact emblem interpretations of the referenced systems. The marks identify
the visual heritage; they do not operate those mods or add compatibility with a
new loader/version. Existing mod-ID admission rules remain authoritative.

Artwork source: `art/theme_icons_tech.py`, with `ICON_IDS` and
`render_theme(ident, size=16)`. Supported native sizes are 8 and 16. Unknown
IDs raise `KeyError`; unsupported sizes raise `ValueError`; every call returns
a fresh RGBA image. All sixteen pairs were checked for native dimensions,
binary alpha, deterministic rendering and uniqueness, then viewed enlarged and
at their actual footprints on light and dark backgrounds.
