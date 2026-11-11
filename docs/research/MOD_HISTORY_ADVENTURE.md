# Adventure, creature and dimension Chao inspiration — 0.7

Research snapshot: 2026-10-29. Proposed art is original authored Chao decoration, not copied mod models/textures. The Chao broad 8×6.5×6.5 head and lowered arm rig should remain intact. All accessories should attach above/on the back of the head or outside the body; no oversized shoulder pieces beneath the face.

## Recommended designs and runtime gates

| Stable key | Source mod; actual mod ID | Three recognizable authored cues | 1.21.1 NeoForge status / notes | Catalyst candidate |
| --- | --- | --- | --- | --- |
| cataclysm | L_Ender's Cataclysm; `cataclysm` | Basalt-black armor coat; narrow orange/gold Ignis crest; molten floating core | Official CurseForge file list confirms NeoForge1.21.1 | `cataclysm:ignitium_ingot` (registered source verified) |
| iceandfire | Ice and Fire; `iceandfire` | Red/ice-blue split coat; small swept dragon horns; dorsal scales | Original CurseForge line is older, but IceAndFire Community Edition has a1.21.1NeoForge file and retains `iceandfire` | Dragon bone; exact registry spelling should be checked on the target port rather than assumed |
| aether | The Aether; `aether` | White/cloud-blue body; gold valkyrie circlet; tiny gold-white winglets | Official1.21.1NeoForge confirmed | `aether:ambrosium_shard` (official item registry / wiki) |
| twilightforest | The Twilight Forest; `twilightforest` | Dusk-purple/forest-green coat; small branching antlers; firefly light marker | Official1.21.1NeoForge confirmed | `twilightforest:fiery_ingot` (modern recipe namespace; resolve registry before consuming) |
| cobblemon | Cobblemon; `cobblemon` | Trainer cap and brim; red/white capture-ball orb; green field vest | Official1.21.1NeoForge confirmed | `cobblemon:poke_ball` (candidate; verify loaded registry) |
| undergarden | The Undergarden; `undergarden` | Muted moss/charcoal coat; luminous cyan fungus crest; root/bark cuffs | Official1.21.1NeoForge confirmed | `undergarden:cloggrum_ingot` (candidate; verify loaded registry) |
| deeperdarker | Deeper and Darker; `deeperdarker` | Navy sculk coat; cyan antenna tips; pale rib motif / echo marker | Official1.21.1NeoForge confirmed | `deeperdarker:heart_of_the_deep` (candidate; verify loaded registry) |
| alexscaves | Alex's Caves; `alexscaves` | Toxic-lime/graphite coat; compact hazmat-mask trim; little uranium/magnet marker | Original remains1.20.1; explicitly unofficial1.21.1NeoForge port exists. Gate on actual `alexscaves`, not the port's project title. | `alexscaves:uranium` (candidate; verify registry) |
| blueskies | Blue Skies; `blue_skies` | Turquoise/sun-gold coat; small angular aquite crest; contrasting sun/moon marker | Official examined file listings are older1.20.x; do not promise native1.21.1. Dormant gate unless a compatible same-ID port is loaded. | `blue_skies:aquite` / gem spelling unverified; use an owned fallback token recipe if not resolving item IDs |
| betweenlands | The Betweenlands; `thebetweenlands` | Peat-green coat; fungus cap crest; root fibers / eerie swamp light | Official examined release line is1.12.2. Modern port work is not proof of a released1.21.1NeoForge build. Dormant gate. | `thebetweenlands:swamp_talisman` is a legacy candidate; avoid hard dependency |
| draconicevolution | Draconic Evolution; `draconicevolution` | Obsidian-purple shell; orange energy-core chest; angular wyvern crown | Official1.21.1NeoForge confirmed, beta file line | `draconicevolution:draconium_dust` (candidate; resolve registry) |
| tconstruct | Tinkers' Construct; `tconstruct` | Cobalt/slime-blue crest; smeltery apron; molten-metal orb | SlimeKnights official FAQ says1.21.1 port is planned after Mantle/Metalborn; official current line remains1.20.1. Dormant same-ID gate. | `tconstruct:cobalt_ingot` (candidate; resolve registry) |
| rats | Rats; `rats` | Warm-grey/cheese coat; tiny cheese-wedge crest; curled cable-like tail / cheese orb | Original is1.20.1; an explicitly unofficial1.21.1NeoForge port exists. | `rats:cheese` (candidate; resolve registry) |
| vampirism | Vampirism; `vampirism` | Charcoal/crimson coat; high cape collar behind head; ruby tear marker | Official1.21.1NeoForge confirmed | `vampirism:vampire_fang` (candidate; resolve registry) |
| borninchaos | Born in Chaos; `born_in_chaos_v1` | Charcoal/pumpkin-orange coat; small crooked jack-o-lantern crown; soul-flame marker | Official1.21.1NeoForge confirmed. Actual mod ID corroborated by mod's registry namespaces and issue modlists; source is MCreator-generated and not published on the project page. | `born_in_chaos_v1:dark_metal_ingot` (candidate; resolve registry) |
| alexsmobs | Alex's Mobs; `alexsmobs` | Khaki naturalist/pith helmet; single crow feather; compact field-guide satchel or binocular motif | Original1.20.1; several explicitly unofficial1.21.1NeoForge ports, including Alex's Mobs Neo | `alexsmobs:animal_dictionary` (candidate; resolve registry) |
| quark | Quark; `quark` | Stoneling stone cap; Toretoise-style compact ore shell; diamond Heart of Diamond marker | Official1.21.1NeoForge confirmed, including4.1-488 file line | `quark:diamond_heart` (candidate; verify registry) |
| galacticraft | Galacticraft; legacy `galacticraftcore`, modern rewrite `galacticraft` | White astronaut visor frame; small oxygen tanks on back; satellite marker | Legacy official builds1.12.2/1.7.10. A third-party1.21.1 port found is Fabric only, so no nativeNeoForge guarantee. Modern rewrite ID needs metadata verification before adding as an alias. | Prefer owned, mod-gated token recipe with glass/iron rather than guessing a legacy metadata item |
| atum | Atum2; `atum` | Sandstone/gold coat; striped pharaoh headpiece; blue scarab marker | Official Atum2 line remains1.16.5. Do not confuse it with unrelated modern Fabric speedrun reset mod named Atum. Dormant gate, but bare `atum` alone can collide with unrelated mod! | Legacy `atum:scarab`; avoid unsafe same-name detection without a second item namespace signature |
| erebus | The Erebus; `erebus` | Amber-green chitin coat; short beetle-like crest; paired shell plates on back | Official examined line remains1.12.2. Dormant gate. | Legacy `erebus:jade`; registry resolution needed |

The first12 rows provide a broad adventure subset, while Rats/Vampirism/Born in Chaos add current, popular creature/horror themes and the last3 are recognizable historical dimension references. The actual owner mod must be loaded before showing, obtaining or applying a themed token; a vanilla fallback recipe still needs a real mod-presence condition. A matching namespace alone is weaker than loading the proper mod ID. Avoid misleading aliases for entirely different mods (particularly Atum).

## Primary source links inspected

### Current availability and original themes (CurseForge project/file pages)
- Cataclysm: https://www.curseforge.com/minecraft/mc-mods/lendercataclysm/files/all
- Ice and Fire original: https://www.curseforge.com/minecraft/mc-mods/ice-and-fire-dragons ; community edition: https://www.curseforge.com/minecraft/mc-mods/iceandfire-ce/files/all
- Aether: https://www.curseforge.com/minecraft/mc-mods/aether/files/all?page=1&version=1.21.1
- Twilight: https://www.curseforge.com/minecraft/mc-mods/the-twilight-forest/files/all?page=1&version=1.21.1
- Undergarden: https://www.curseforge.com/minecraft/mc-mods/the-undergarden/files/all
- DeeperDarker: https://www.curseforge.com/minecraft/mc-mods/deeperdarker
- Alex's Caves unofficial port: https://www.curseforge.com/minecraft/mc-mods/alexs-caves-unofficial-port
- Cobblemon: https://www.curseforge.com/minecraft/mc-mods/cobblemon/files/all
- Blue Skies: https://www.curseforge.com/minecraft/mc-mods/blue-skies/files/all
- Betweenlands: https://www.curseforge.com/minecraft/mc-mods/angry-pixel-the-betweenlands-mod
- Draconic Evolution: https://www.curseforge.com/minecraft/mc-mods/draconic-evolution/files/all?page=1&version=1.21.1
- Tinkers: https://www.curseforge.com/minecraft/mc-mods/tinkers-construct ; official roadmap: https://slimeknights.github.io/faq/
- Rats unofficial port: https://www.curseforge.com/minecraft/mc-mods/rats-unofficial-port
- Vampirism: https://www.curseforge.com/minecraft/mc-mods/vampirism-become-a-vampire/files/all?page=1&version=1.21.1
- Born in Chaos: https://www.curseforge.com/minecraft/mc-mods/born-in-chaos/files/all
- Galacticraft legacy: https://www.curseforge.com/minecraft/mc-mods/galacticraft-legacy ; third-party Fabric port: https://www.curseforge.com/minecraft/mc-mods/galacticraft-legacy-fabric-1-21-1
- Atum2: https://www.curseforge.com/minecraft/mc-mods/atum
- Erebus: https://www.curseforge.com/minecraft/mc-mods/the-erebus/files/all

### IDs and registry source verification
- Cataclysm `MODID = "cataclysm"`: https://github.com/lender544/new1.20.1/blob/master/src/main/java/com/github/L_Ender/cataclysm/Cataclysm.java ; `ignitium_ingot`: https://github.com/lender544/new1.20.1/blob/master/src/main/java/com/github/L_Ender/cataclysm/init/ModItems.java
- IceAndFire CE `MOD_ID = "iceandfire"`: https://github.com/IAFEnvoy/IceAndFire-CE/blob/master/src/main/java/com/iafenvoy/iceandfire/IceAndFire.java
- Aether `mod_id=aether`, actualMinecraft1.21.1: https://github.com/The-Aether-Team/The-Aether/blob/1.21.1-develop/gradle.properties ; items: https://github.com/The-Aether-Team/The-Aether/blob/1.21.1-develop/src/main/java/com/aetherteam/aether/item/AetherItems.java
- Twilight `mod_id=twilightforest`, actualMinecraft1.21.1: https://raw.githubusercontent.com/TeamTwilight/twilightforest/1.21.1/gradle.properties (read successfully)
- Alex's Caves `modId="alexscaves"`: https://github.com/AlexModGuy/AlexsCaves/blob/main/src/main/resources/META-INF/mods.toml
- Rats `mod_id=rats`, Minecraft1.20.1 original: https://raw.githubusercontent.com/AlexModGuy/Rats/1.20/gradle.properties ; https://raw.githubusercontent.com/AlexModGuy/Rats/1.20/src/main/resources/META-INF/mods.toml (read successfully)
- Tinkers `modId="tconstruct"`: https://raw.githubusercontent.com/SlimeKnights/TinkersConstruct/1.20.1/src/main/resources/META-INF/mods.toml (read successfully)
- Draconic `modId="draconicevolution"`: https://raw.githubusercontent.com/brandon3055/Draconic-Evolution/1.21/src/main/resources/META-INF/neoforge.mods.toml (read successfully)
- Vampirism `vampirism_mod_id=vampirism`: https://raw.githubusercontent.com/TeamLapen/Vampirism/dev/gradle.properties (read successfully; dev branch is newer26.1, while CurseForge independently confirms1.21.1)

The table deliberately distinguishes candidates from verified catalyst registry IDs. Runtime recipe code should either check the loaded registry safely or use owned recipes gated by the source mod; it should never unconditionally deserialize missing foreign items.

## Additional confirmed metadata and project checks

- Undergarden `modId="undergarden"`: https://raw.githubusercontent.com/quek04/The-Undergarden/26.1/src/main/resources/META-INF/neoforge.mods.toml (read successfully).
- Betweenlands `public static final String ID = "thebetweenlands"`: https://raw.githubusercontent.com/Angry-Pixel/The-Betweenlands/1.12-fixes/src/main/java/thebetweenlands/common/lib/ModInfo.java (read successfully).
- Legacy Galacticraft `MOD_ID_CORE = "galacticraftcore"`, `MOD_ID_PLANETS = "galacticraftplanets"`: https://raw.githubusercontent.com/TeamGalacticraft/Galacticraft-Legacy/master-1.12/src/main/java/micdoodle8/mods/galacticraft/core/Constants.java (read successfully).
- Quark `modId="quark"`: https://raw.githubusercontent.com/VazkiiMods/Quark/master/src/main/resources/META-INF/neoforge.mods.toml (read successfully).1.21.1NeoForge confirmed: https://www.curseforge.com/minecraft/mc-mods/quark/files/all
- DeeperDarker `id: deeperdarker`, `MOD_ID = "deeperdarker"`: https://raw.githubusercontent.com/KyaniteMods/DeeperAndDarker/fabric-1.20.x-2.0.0s/src/main/resources/fabric.mod.json ; https://raw.githubusercontent.com/KyaniteMods/DeeperAndDarker/fabric-1.20.x-2.0.0s/src/main/java/com/kyanite/deeperdarker/DeeperDarker.java (read successfully; same stable namespace as the independently verified NeoForge release).
- Alex's Mobs `modId="alexsmobs"`: https://raw.githubusercontent.com/AlexModGuy/AlexsMobs/1.20/src/main/resources/META-INF/mods.toml (read successfully). Original: https://www.curseforge.com/minecraft/mc-mods/alexs-mobs ; unofficial1.21.1NeoForge port: https://www.curseforge.com/minecraft/mc-mods/alexs-mobs-neo

Quark and Alex's Mobs suggestions were sent directly to the geometry agent, as were Vampirism and Born in Chaos: short stone crest/ore shell/diamond marker; pith helmet/feather/satchel; crimson cape/collar/ruby marker; crooked black pumpkin crown/ragged cape/soul-flame marker respectively.
