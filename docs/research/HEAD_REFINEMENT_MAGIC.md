# Magic theme head refinement — 1.21.1 verification

Checked 2026-10-29. The scope here is downloadable Minecraft **1.21.1 NeoForge** builds, including clearly identified publisher alphas and community ports. A source branch, plan to port, or broad project-level loader tag alone is insufficient. These checks establish available builds and matching identity gates; they do not claim a Chao Garden launch alongside every full third-party mod.

The authored silhouettes live in `art/head_profiles_magic.py`. They replace the upper cranium with a continuous connected outline and broad roots. The Chao face remains recognizable, and the original Sonic-like Chao evolution approach informs how the head grows into a type. The designs are original interpretations, with no imported mod meshes or textures.

## Confirmed themes and intended silhouette

| Theme / catalog ID | Confirmed 1.21.1 NeoForge build | Head refinement |
| --- | --- | --- |
| Astral Sorcery / `astral_sorcery` | [AstralSorcery 2.0.3, alpha, Oct 21 2026](https://www.curseforge.com/minecraft/mc-mods/astral-sorcery/files/9281073) | A broad crescent cranium, with two high lunar temple tips and a low center. |
| Botania / `botania` | [Botania Revived 1.0.3, Oct 23 2026](https://www.curseforge.com/minecraft/mc-mods/botania-revived/files/all), **unofficial altered port** | Three broad mana-flower petals become the crown and temples, with one taller central petal. |
| Blood Magic / `blood_magic` | [Blood Magic unofficial port v1.1, Sep 11 2026](https://www.curseforge.com/minecraft/mc-mods/blood-magic-unofficial-port/files/8855778), **community alpha/WIP** | The whole upper head tapers into a plump ruby droplet above broad ritual-slate shoulders. |
| Ars Nouveau / `ars_nouveau` | [5.13.3, Oct 3 2026](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau/files/9052154) | Broad Starbuncle-like ear lobes and one small source-flame tuft, all continuous with the skull. |
| Occultism / `occultism` | [1.224.6, Oct 27 2026](https://www.curseforge.com/minecraft/mc-mods/occultism/files/all?page=1&pageSize=20&version=1.21.1) | An asymmetric flowing Otherworld wisp with an elongated taper and pale broad roots. |
| Hexerei / `hexerei` | [0.5.0.3, beta, Apr 2 2025](https://www.curseforge.com/minecraft/mc-mods/hexerei/files/all?page=1&version=1.21.1) | The crown flows into a soft drooping willow curl, with no separate witch hat or brim. |
| Nature's Aura / `natures_aura` | [41.10, Sep 4 2026](https://www.curseforge.com/minecraft/mc-mods/natures-aura/files/all) | One broad offset golden leaf blade unfurls from a low seed-leaf shoulder. |
| Roots / `roots` | [4.0.0.31-alpha, Sep 1 2026](https://www.curseforge.com/minecraft/mc-mods/roots/files/8788962) | Three unequal, low sprout lobes share a broad bark-and-moss crown. |
| Iron's Spells / `iron_spells` | [3.16.3, Aug 18 2026](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks/files/all) | A swept central spell-wave fin rises between two lower curled temple lobes. |
| ProjectE / `projecte` | [PE1.1.0, Jun 3 2025](https://www.curseforge.com/minecraft/mc-mods/projecte/files/all) | Continuous dark-matter shoulders taper to one broad Philosopher's Stone diamond point. |
| Psi / `psi` | [110, Sep 1 2026](https://www.curseforge.com/minecraft/mc-mods/psi/files/all) | Two blunt psimetal crown lobes split around a clean central notch, echoing programmed flow. |
| Malum / `malum` | [1.8.2, Dec 8 2025](https://www.curseforge.com/minecraft/mc-mods/malum/files/all?page=1&version=1.21.1) | Three unequal flowing spirit lobes, with an offset tallest flame and broad soulwood roots. |
| EvilCraft / `evilcraft` | [1.2.99 release, Oct 16 2026](https://www.curseforge.com/minecraft/mc-mods/evilcraft/files/all) | Two low swept temple lobes around a shallow dark-gem brow, rather than mounted horns. |
| Mystical Agriculture / `mystical_agriculture` | [8.0.29, Oct 22 2026](https://www.curseforge.com/minecraft/mc-mods/mystical-agriculture/files/all?page=1&pageSize=20&version=1.21.1) | Two broad open seed leaves form the upper skull around a low central bud. |
| Hex Casting / `hexcasting` | [Official publisher-linked CI, successful build 64](https://ci.blamejared.com/job/petrakat/job/Hexcasting/job/1.21/64/), **development alpha**, `hexcasting-neoforge-1.21.1-0.12.0-devel-pre-64.jar` | One broad six-sided amethyst peak between two low facets, a connected chevron inspired by hex-grid patterns. |

## Port and development-build evidence

**Botania:** The [official changelog](https://botaniamod.net/changelog.html) still describes the official 1.21+ port as ongoing. The separate [Botania Revived publisher](https://www.curseforge.com/minecraft/mc-mods/botania-revived) explicitly identifies its release as an unofficial NeoForge 1.21.1 port, preserving both mod ID and resource namespace `botania`. It requires **NeoForge 21.1.240 or newer**, Java 21, and Patchouli 1.21.1-93 or newer. This is available through a community port, not a newly released official Botania line. The Chao gate already accepts `botania` and needs no new alias.

**Blood Magic:** The official project's current line remains [Forge 1.20.1](https://www.curseforge.com/minecraft/mc-mods/blood-magic/files/all). The community port's published v1.1 JAR was fetched into memory and its `META-INF/neoforge.mods.toml` inspected without executing it or retaining its assets. It declares `modId="bloodmagic"`, version `3.4.0-46-alpha`, NeoForge `[21.1.0,)`, Minecraft `[1.21.1,1.22)`, and a required Modopedia dependency. The project's release channel label is “Release”, but its description explicitly calls the port alpha/work in progress; the latter is the accurate stability description. The existing `bloodmagic` gate matches.

**Hex Casting:** The [publisher's CurseForge description](https://www.curseforge.com/minecraft/mc-mods/hexcasting) says the 1.21.1 port is in development and links its alpha builds at [official CI](https://ci.blamejared.com/job/petrakat/job/Hexcasting/job/1.21/). The CI JSON endpoint for `lastSuccessfulBuild` returned build **64** with an actual `Neoforge/build/libs/hexcasting-neoforge-1.21.1-0.12.0-devel-pre-64.jar` artifact. That artifact was fetched to memory and its NeoForge manifest inspected: mod ID `hexcasting`, Minecraft `[1.21.1,1.21.10)`, NeoForge `[21,)`, KotlinForForge loader, required PAUCAL/Patchouli/Caelus/Inline entries. This confirms a downloadable NeoForge 1.21.1 development build, while the stable CurseForge line remains 1.20.1. The existing gate matches. Development CI availability is not a stable-release recommendation.

No downloaded third-party binary, model, texture, or sound is included in the deliverable. JAR manifest checks above were read-only and in memory.

## Deferred within this pass

**Thaumcraft:** [The official project](https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/all) still lists its latest game generation as 1.12.2. Community source-only port work exists, but no official or verified published compatible build was established in this check. Its historical theme remains as before rather than being included in this confirmed-build refinement pass.

Roots 4 and Roots Classic remain distinct projects. The catalog's existing `roots` / `rootsclassic` OR gate is retained; the available 1.21.1 build verified here is **Roots 4**, not a claim of a new Roots Classic release.
