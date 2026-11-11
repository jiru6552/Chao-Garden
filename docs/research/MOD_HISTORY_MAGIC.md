# Magic mod Chao inspiration research — 0.7

Research date: 2026-10-29. This is internal research, not a claim of tested full-mod compatibility. Designs below are original interpretations of each mod's visual language, not proposed extraction of models or textures.

The user explicitly requested inspiration beyond the named mods. Recommended core twelve are the four requested magic mods plus Ars Nouveau, Occultism, Hexerei, Nature's Aura, Roots, Iron's Spells, ProjectE and Psi. Malum, EvilCraft and Mystical Agriculture are additional strong modern choices.

## Design and gating catalog

| Variant key / loaded mod ID | Name | Original Chao design cues | Status for this NeoForge 1.21.1 mod |
| --- | --- | --- | --- |
| `thaumcraft` | Thaumic Chao | Aubergine body, bronze goggle rims kept clear of pupils, parchment chest clasp, tiny violet wisp above head | Historic official project ends at 1.12.2. Gate is dormant unless a compatible port really exposes this ID; do not imply official current support. |
| `astralsorcery` | Astral Chao | Midnight blue / marble white, cyan crystal crest, gold star marker and fine constellation marks | Official CF lists 1.21.1 NeoForge alpha 2.0.3, Oct 2026. |
| `botania` | Manabloom Chao | Leaf green body, white/pink petal crest, manasteel-blue cuffs, small vine collar | Official CF current project files inspected were 1.20.1. Keep condition dormant where absent; no claim of a verified 1.21.1 build. |
| `bloodmagic` | Sanguine Chao | Black/red ritual coat, gold slate runes, red gem/orb and dark cuffs; cute arcane styling rather than gore | Official CF current main line inspected is Forge 1.20.1. Treat 1.21.1 as an unverified/dormant gate. |
| `ars_nouveau` | Sourceweaver Chao | Lavender archwood coat, small gold-bound spellbook on back, purple Source Gem crest and gold accents | Official CF 1.21.1 NeoForge 5.13.3. |
| `occultism` | Spiritkeeper Chao | Charcoal coat, pale chalk-circle collar, violet spirit flame and silver summoning runes | Official project has 1.21.1 NeoForge branch and listings. |
| `hexerei` | Willow Witch Chao | Muted lavender / willow green, low brim and pointed witch hat, candle marker, small broom-like tail | Official CF 1.21.1 NeoForge 0.5.0.3 beta. |
| `naturesaura` | Aurabloom Chao | Pale green body, golden laurel/leaf crest, warm gold aura marker, leaf cuffs | Official CF 1.21.1 NeoForge 41.10. |
| `roots` | Grove Chao | Bark brown and moss green, two-leaf sprout crest, amber seed marker, simple carved rune chest | Roots 4 official CF has 1.21.1 NeoForge alpha 4.0.0.31. Separate from Roots Classic. |
| `irons_spellbooks` | Spellbound Chao | Deep blue cloth hood, gold trim, small rolled scroll case, sapphire marker | Official CF 1.21.1 NeoForge 3.16.3. |
| `projecte` | Alchemist Chao | Obsidian and dark matter red, gold transmutation ring, red gem crest and bright gold palms | Official CF 1.21.1 NeoForge PE1.1.0. |
| `psi` | Psionic Chao | White psimetal body with cyan circuit seams, small temple antenna and blue formula-marker ring | Official CF 1.21.1 NeoForge 110. |
| `malum` | Spirit Arcana Chao | Charred umber and muted violet robes, orange/teal spirit wisps, soul-rune chest and small soulwood crest | Official CF 1.21.1 NeoForge 1.8.2. |
| `evilcraft` | Dark Gem Chao | Black-plum body, blood-red dark gem crest, silver bottle clasp and crimson cuffs | Official CF files include 1.21.1 NeoForge 1.2.99. |
| `mysticalagriculture` | Essence Sprout Chao | Inferium green body, seedling crown, purple prosperity-crystal accents, seed-pod marker | Official CF 1.21.1 NeoForge 8.0.28. |

Preserve the 8×6.5×6.5 face and lowered arms. These themes should add small crest, collar, back or marker details. Large hats must be checked against the marker, and back props should not sweep into hands when the head turns. Do not reintroduce animal gift variants to implement these thematic forms.

`rootsclassic` is a real separate mod ID/project. It may optionally unlock the same broadly druidic tribute by explicit OR condition, but must not be represented as the exact Roots 4 mod. Avoid guessed aliases such as `blood_magic`, `astral_sorcery`, `ironspells` or `natureaura`.

## Exact mod ID evidence from official source

These source bytes were fetched directly; the relevant constants/properties were inspected:

- `astralsorcery`: https://raw.githubusercontent.com/HellFirePvP/AstralSorcery/1.21.1/gradle.properties — `mod_id=astralsorcery`.
- `botania`: https://raw.githubusercontent.com/VazkiiMods/Botania/1.20.x/Xplat/src/main/java/vazkii/botania/api/BotaniaAPI.java — API namespace constant `botania`.
- `bloodmagic`: https://raw.githubusercontent.com/WayofTime/BloodMagic/1.20.1/src/main/java/wayoftime/bloodmagic/BloodMagic.java — `MODID` constant.
- `ars_nouveau`: https://github.com/baileyholl/Ars-Nouveau/blob/main/gradle.properties — `mod_id=ars_nouveau`; also declares Minecraft 1.21.1.
- `occultism`: https://raw.githubusercontent.com/klikli-dev/occultism/version/1.21.1/gradle.properties — `mod_id=occultism`.
- `hexerei`: https://raw.githubusercontent.com/JoeFoxe/Hexerei-1.19/1.20.1/src/main/java/net/joefoxe/hexerei/Hexerei.java — `MOD_ID` constant. The official current CF release separately confirms 1.21.1.
- `naturesaura`: https://raw.githubusercontent.com/Ellpeck/NaturesAura/main/src/main/java/de/ellpeck/naturesaura/api/NaturesAuraAPI.java — `MOD_ID` constant.
- `roots`: https://raw.githubusercontent.com/MysticMods/Roots/1.21.1/gradle.properties — `mod_id=roots`.
- `irons_spellbooks`: https://github.com/iron431/irons-spells-n-spellbooks/blob/1.21/gradle.properties — exact `mod_id` and Minecraft release line.
- `projecte`: https://raw.githubusercontent.com/sinkillerj/ProjectE/mc1.21.1/src/main/java/moze_intel/projecte/PECore.java — uses API's `PROJECTE_MODID`; API constant separately checked below.
- `psi`: https://raw.githubusercontent.com/VazkiiMods/Psi/master/gradle.properties — `mod_id=psi`.
- `malum`: https://raw.githubusercontent.com/SammySemicolon/Malum/1.21.1/src/main/java/com/sammy/malum/MalumMod.java — `MALUM` constant.
- `evilcraft`: https://raw.githubusercontent.com/CyclopsMC/EvilCraft/master-1.21/src/main/java/org/cyclops/evilcraft/Reference.java — `MOD_ID` constant.
- `mysticalagriculture`: https://raw.githubusercontent.com/BlakeBr0/MysticalAgriculture/1.21/src/main/java/com/blakebr0/mysticalagriculture/MysticalAgriculture.java — `MOD_ID` constant.

## Survival acquisition suggestions

The safest broad compatibility implementation is a conditional own-item recipe or customization mechanism gated by actual loaded mod IDs; it should not fail datapack loading when an external item has been renamed in a different release. External catalyst suggestions below are meaningful choices, but only IDs explicitly verified in source should be hardcoded without a registry-exists condition.

| Mod | Proposed external catalyst | Source verification status |
| --- | --- | --- |
| Thaumcraft | Amber or thaumometer | Historic concept verified by project/API; exact current port registry not verified. |
| Astral Sorcery | Aquamarine or rock crystal | Theme verified; 1.21.1 exact registry names not yet verified. |
| Botania | `botania:manasteel_ingot` | Registry source checked below. |
| Blood Magic | Blank slate (`bloodmagic:blankslate` in older releases) | Exact current registry not verified; use fallback/exists gate. |
| Ars Nouveau | Source Gem (`ars_nouveau:source_gem`) | Registry source checked below. |
| Occultism | Otherworld ashes | Exact current ID not verified. |
| Hexerei | Mandrake root | Exact current ID not verified. |
| Nature's Aura | Infused iron | Exact current ID not verified. |
| Roots | Wildroot | Exact current ID not verified. |
| Iron's Spells | Arcane essence | Exact current ID not verified. |
| ProjectE | Dark matter | Exact current ID not verified. |
| Psi | Psidust | Exact current ID not verified. |
| Malum | Arcane spirit | Exact current item registry path was not resolved; do not hardcode without an exists gate. |
| EvilCraft | Dark gem | Exact current ID not verified. |
| Mystical Agriculture | Inferium essence (`mysticalagriculture:inferium_essence`) | Registry source checked below. |

## CurseForge primary project references

- https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/all
- https://www.curseforge.com/minecraft/mc-mods/astral-sorcery
- https://www.curseforge.com/minecraft/mc-mods/botania
- https://www.curseforge.com/minecraft/mc-mods/blood-magic
- https://www.curseforge.com/minecraft/mc-mods/ars-nouveau
- https://www.curseforge.com/minecraft/mc-mods/occultism
- https://www.curseforge.com/minecraft/mc-mods/hexerei
- https://www.curseforge.com/minecraft/mc-mods/natures-aura
- https://www.curseforge.com/minecraft/mc-mods/roots
- https://www.curseforge.com/minecraft/mc-mods/roots-classic/files/all
- https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks
- https://www.curseforge.com/minecraft/mc-mods/projecte
- https://www.curseforge.com/minecraft/mc-mods/psi
- https://www.curseforge.com/minecraft/mc-mods/malum/files/all?page=1&version=1.21.1
- https://www.curseforge.com/minecraft/mc-mods/evilcraft/files/all
- https://www.curseforge.com/minecraft/mc-mods/mystical-agriculture

The current project descriptions and file listings, not old memory of mod versions, were checked. No mod files were installed or compatibility launch performed by this research task.

## Additional checked API and item literals

- ProjectE API confirms its ID literal `projecte`: https://raw.githubusercontent.com/sinkillerj/ProjectE/mc1.21.1/src/api/java/moze_intel/projecte/api/ProjectEAPI.java .
- Ars Nouveau's `SOURCE_GEM` is registered in `setup/registry/ItemsRegistry.java`; its `common/lib/LibItemNames.java` literal is `source_gem`. Both are under https://github.com/baileyholl/Ars-Nouveau/tree/main/src/main/java/com/hollingsworth/arsnouveau . This verifies `ars_nouveau:source_gem` on the current main branch.
- Botania's `manaSteel` item uses `MANASTEEL_INGOT`; the literal is `manasteel_ingot`. Checked https://raw.githubusercontent.com/VazkiiMods/Botania/1.20.x/Xplat/src/main/java/vazkii/botania/common/item/BotaniaItems.java and https://raw.githubusercontent.com/VazkiiMods/Botania/1.20.x/Xplat/src/main/java/vazkii/botania/common/lib/LibItemNames.java . This verifies `botania:manasteel_ingot` for that branch, not an unknown port's renaming.
- Mystical Agriculture registers `inferium_essence` in https://raw.githubusercontent.com/BlakeBr0/MysticalAgriculture/1.21/src/main/java/com/blakebr0/mysticalagriculture/init/ModItems.java .

## Hex Casting cross-check

The publisher's CurseForge project is https://www.curseforge.com/minecraft/mc-mods/hexcasting . Its examined stable files target1.20.1, and its description points to work on1.21.1. The official Forge manifest at https://raw.githubusercontent.com/FallingColors/HexMod/main/Forge/src/main/resources/META-INF/mods.toml confirms the ID `hexcasting`. This is an ID/inspiration reference, not a claim that a compatible NeoForge release was tested.
