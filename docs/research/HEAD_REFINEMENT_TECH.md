# Tech head refinement — verified 1.21.1 scope

Checked October 29, 2026. Each included row has an actual downloadable file whose
publisher file page explicitly lists **Minecraft 1.21.1 and NeoForge**. An alpha or
beta download counts as available, not as stable or as a tested integration. No
third-party jars were executed for this research. Presence gates and these source
checks do not prove all mods can run together.

## Included head evolutions

All profiles are original authored skull outlines in `art/head_profiles_tech.py`.
They replace the crown contour with broad-rooted, rear-swept growth. They do not
add hats, goggles, loose rods or detached crystals. Their face and cheek region
remains a Chao. These shapes are design interpretations of the mods, not claims
that those mods include Chao.

| Theme ID | Verified file | Required identity | Publication status | Integrated head direction |
| --- | --- | --- | --- | --- |
| `create` | [Create 6.0.10](https://www.curseforge.com/minecraft/mc-mods/create/files/7963363) | `create` | Publisher release | Three broad, swept brass folds grow from the crown like soft cog teeth; the off-centre leading tuft suggests rotation. |
| `ae2` | [AE2 19.2.18](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2/files/8992605) | `ae2` | Publisher release | A faceted central fluix peak and two smaller rooted facets turn the whole upper skull into a crystal-like Chao evolution. |
| `refined_storage` | [Refined Storage 2.0.9](https://www.curseforge.com/minecraft/mc-mods/refined-storage/files/8211701) | `refinedstorage` | Publisher release | A broad squared teardrop rises naturally from a low quartz dome, with a restrained flat cyan tip echoing a storage disk. |
| `mekanism` | [Mekanism 10.7.19.85](https://www.curseforge.com/minecraft/mc-mods/mekanism/files/7904058) | `mekanism` | Publisher release | Two thick rear-swept lobes merge into a low aerodynamic crown, recalling osmium turbine vanes instead of a worn respirator. |
| `immersive_engineering` | [Immersive Engineering 12.4.2-194](https://www.curseforge.com/minecraft/mc-mods/immersive-engineering/files/6733669) | `immersiveengineering` | Publisher release | Paired rounded copper lobes have broad cheek roots and a soft central saddle, echoing wound transformer coils in the head anatomy. |
| `pneumaticcraft` | [PneumaticCraft Repressurized 8.2.23](https://www.curseforge.com/minecraft/mc-mods/pneumaticcraft-repressurized/files/8519226) | `pneumaticcraft` | Publisher release | An inflated rounded skull gathers into a single swept pressure-drop tuft; the silhouette remains soft and recognizably Chao. |
| `computercraft` | [CC: Tweaked 1.113.1](https://www.curseforge.com/minecraft/mc-mods/cc-tweaked/files/5714512) | `computercraft` | Publisher alpha; CurseForge says future releases are on Modrinth | A low, broad shell dome with a tiny centre crest recalls ComputerCraft turtles through the skull contour rather than a computer helmet. |
| `enderio` | [Ender IO 8.2.12-beta](https://www.curseforge.com/minecraft/mc-mods/ender-io/files/8832691) | `enderio` | Publisher beta | Two ender-dark horns flow backward from full temple roots, with a clean shallow central valley like branching conduits. |
| `actually_additions` | [Actually Additions 1.3.26](https://www.curseforge.com/minecraft/mc-mods/actually-additions/files/8281896) | `actuallyadditions` | Publisher beta; binary file, not sources/Javadoc | A single broad diamond-shaped crest grows smoothly out of the head, with empowered-crystal facets replacing the old loose crystal spikes. |
| `railcraft` | [Railcraft Reborn 1.2.16](https://www.curseforge.com/minecraft/mc-mods/railcraft-reborn/files/9356165) | `railcraft` | Maintained continuation release, October 28, 2026 | A broad locomotive-prow crest tapers backward between low shoulder folds, giving the whole head a streamlined rail-engine profile. |
| `industrialcraft` | [IC2 Nostalgia 21.1.0-local.35](https://www.curseforge.com/minecraft/mc-mods/industrial-craft-2-nostalgia/files/9316825) | `ic2` | Unofficial community port; not IC2 Classic or an official IC2 release | Rubber-like scalloped temple folds feed into a chunky central battery-drop tuft; yellow and dark insulation follow the head surface. |
| `buildcraft` | [BuildCraft Community Edition 8.0.23](https://www.curseforge.com/minecraft/mc-mods/buildcraft-community-edition/files/9035819) | `buildcraftcore` | Unofficial community continuation | Three wide triangular folds share one continuous yellow crown, translating quarry cutting teeth into Chao head growth. |
| `draconic_evolution` | [Draconic Evolution 3.1.4.633](https://www.curseforge.com/minecraft/mc-mods/draconic-evolution/files/8797042) | `draconicevolution` | Publisher beta | Broad-rooted wyvern horns sweep backward around a low central draconic crest, changing the skull silhouette without a helmet or attached antlers. |
| `galacticraft` | [Galacticraft Boobcat Edition 5.4.17-prealpha](https://www.curseforge.com/minecraft/mc-mods/galacticraft-boobcat-edition/files/9169477) | `galacticraft` | Unofficial fork; downloadable NeoForge prealpha, despite its CF Release flag | A long clean rocket-nose droplet rises from broad moon-grey cheeks, with a red tapered tip and no astronaut helmet. |

## Scope exclusions

- **Thermal**: [official Thermal Foundation file list](https://www.curseforge.com/minecraft/mc-mods/thermal-foundation/files/all) still identifies 1.20.1 as its newest line; no confirmed 1.21.1 NeoForge binary was found. Leave this historical theme alone.
- **Tinkers Construct**: [official project/files](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct/files/all) has recent September 2026 updates, but those are 1.20.1. A NeoForge loader label on a 1.20.1 file is not evidence of a 1.21.1 port. Leave this theme alone.

## Galacticraft identity correction

The earlier survey only established the Fabric implementation. A closer file
search found the separate community fork with a NeoForge 1.21.1 build. Its
[publisher file](https://www.curseforge.com/minecraft/mc-mods/galacticraft-boobcat-edition/files/9169477)
links to [ColinVaughn/Galacticraft-1.21.1](https://github.com/ColinVaughn/Galacticraft-1.21.1).
The actual source metadata was fetched and inspected:

- [`gradle.properties`](https://github.com/ColinVaughn/Galacticraft-1.21.1/blob/main/gradle.properties): `mod.id=galacticraft`, `minecraft.version=1.21.1`, `neoforge.version=21.1.238`, both Fabric and NeoForge modules.
- [`neoforge.mods.toml`](https://github.com/ColinVaughn/Galacticraft-1.21.1/blob/main/neoforge/src/main/resources/META-INF/neoforge.mods.toml): `modId = "galacticraft"`, display name identifies the community fork.

The existing `galacticraftcore` historical alias will not recognize this file.
Accept verified `galacticraft` as another alias when refining that theme. Do not
require the user to load the incompatible old `galacticraftcore` jar. The fork's
README still describes its multi-loader migration as work in progress; describe
this as a published prealpha, not a stable official Galacticraft release.

## Source grounding for recognizable forms

The design cues follow the established mod identities: Create brass cog teeth;
AE2 fluix crystal facets ([official guide](https://guide.appliedenergistics.org/1.21.1/items-blocks-machines/fluix_crystal/));
Refined Storage quartz and cyan storage geometry ([official docs](https://refinedmods.com/refined-storage/));
Mekanism osmium and turbine vanes; Immersive Engineering copper transformers;
PneumaticCraft pressure and air; [CC: Tweaked turtles](https://tweaked.cc/);
Ender IO branching conduits; Actually Additions empowered crystals; Railcraft
streamlined locomotives; IC2 rubber insulation and stored power; BuildCraft quarry
cutters; Draconic Evolution wyvern horns; Galacticraft rocket nose cones. The
publisher pages linked in the table establish each mod's identity and download
status. Palette details and silhouettes are authored interpretations.
