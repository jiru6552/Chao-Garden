# Retained themes, retirement and Neo Vitae - 0.14.0

The active collection has 19 themes: Create, Thaumcraft, Astral Sorcery, Neo Vitae, Refined Storage, Cataclysm, Ice and Fire, Aether, Twilight Forest, Nether, End, Ars Nouveau, Occultism, Iron's Spells, ProjectE, Malum, Cobblemon, Draconic Evolution and Born in Chaos. Seventeen require their matching mod; Nether and End do not.

The 29 requested integrations are retired: Botania, AE2, Mekanism, Immersive Engineering, Thermal, PneumaticCraft, CC: Tweaked, Ender IO, Actually Additions, Railcraft, IndustrialCraft, BuildCraft, Hexerei, Nature's Aura, Roots, Psi, EvilCraft, Mystical Agriculture, Hex Casting, Alex's Caves, Alex's Mobs, Undergarden, Deeper and Darker, Blue Skies, Betweenlands, Tinkers, Galacticraft, Quark and Vampirism.

Retired themes have no active catalog entries, recipes, recipe-book advancements, creative entries, theme commands, runtime geometry, body/egg atlases or themed egg artwork. Loading their former source mod does not enable them. Historical source authoring profiles remain available for future work, but the shipped rig and appearance metadata contain only current themes.

Existing Chao are kept as ordinary Chao, preserving their UUID, owner, custom name, genetic coat, skills, acquired animal parts, happiness, age and random history. Entity and carrier loading normalizes only the explicitly retired appearance IDs. Unavailable retained themes keep the previous dormant-heritage behavior; unknown future IDs are preserved.

Former egg registry IDs remain as hidden, ordinary egg compatibility shells so old inventories do not silently lose their eggs. They have normal-egg models/names, no recipe or creative entry, and can only place ordinary Chao. Placement retains saved genes, name and incubation. Recovering the egg produces the normal registered egg, completing the migration. These placeholders cannot produce a removed themed Chao.

Neo Vitae uses the real `neovitae` mod gate and the public name **Neo Vitae**. The stable `blood_magic` saved identity and `blood_magic_chao_egg` registry ID preserve existing eggs/Chao. `/chao variantegg neovitae` is the current suggested command; the older argument also resolves the same identity. The old `bloodmagic` mod ID alone no longer unlocks this theme. Its artwork is a blood drop.

Primary reference: [Neo Vitae source](https://github.com/breakinblocks/NeoVitae), [1.21.1 build identity](https://github.com/breakinblocks/NeoVitae/blob/1.21.1/gradle.properties). The mod is a derivative/rewrite of Blood Magic rather than a simple upstream project rename.

Regression coverage checks the exact retained catalog, all retired IDs against an all-mods-loaded predicate, the Neo Vitae gate and command alias, state-only appearance normalization, and unchanged future-ID behavior. Two server integration tests exercise all 29 old egg IDs through actual placement/recovery and all 29 old Chao saves/carriers through actual load/release, checking genetic data, name, owner, UUID and incubation retention. Asset validators reject retired recipes, artwork, atlases or rig groups and require ordinary compatibility models.
