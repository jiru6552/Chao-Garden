# Chao asset editing - 0.16.0

The live Chao body now uses imported official geometry, textures and animation definitions from the owner's supplied Sonic Marketplace world. The mod's own theme crowns, goggles, badges, animal parts and Chaos adaptations are retargeted onto those skeletons. [Asset provenance](../../docs/ASSET_CREDITS_015.md) distinguishes the supplied assets from the project additions.

There are two authoring sources: the imported form JSON/PNG files, and the eight retained **Blockbench Generic Model (`free`) projects** for custom additions and the egg. Editing an old master body does not replace the imported in-game forms.

## Imported forms and animation

`art/imported_chao/` contains the minimal reproducible inputs: twenty model JSON files, twenty texture PNGs and one animation JSON. This includes supplemental source rigs; keeping a source rig does not add it as an obtainable new theme.

- `models/`: supplied Bedrock form geometry, with original cuboids, parent pivots, cube rotations, inflation, mirror flags and zero-thickness planes.
- `textures/`: supplied form texture maps.
- `animations/chao.animation.json`: eight supplied clips. The runtime samples idle, walk and emotion curves; source special-rig clips are also retained. Resting, swimming, feeding and combat receive Minecraft-specific pose additions.

To apply edits to these files, run from the source project root:

```bash
python3 art/import_official_chao.py
python3 art/retarget_chao_overlays.py
```

The importer converts coordinates and box UVs into `models/entity/official/`, copies animation definitions, and generates inherited/shiny coats matching each form's UVs. The retargeter writes fitted overlays and theme/Chaos coats. `OfficialChaoRig.java` selects the available form for saved alignment and ability state and samples the imported animation curves. It does not import the entire Bedrock entity/controller system or provide every source-game primary/secondary morph combination.

The imported source JSON can be opened in an appropriate Bedrock-model editor. Preserve the named source bones when changing geometry; retargeted additions depend on those anchors. No external model or animation mod is required.

## Custom additions in Blockbench

The retained projects have embedded textures:

| Project | Authoring purpose |
| --- | --- |
| `chao_adventure_master.bbmodel` | Shared library of custom animal, theme, evolution and Chaos groups |
| `chao_hero.bbmodel`, `chao_dark.bbmodel` | Alignment previews of the same custom authoring rig |
| `chao_chaos_light.bbmodel`, `chao_chaos_angel.bbmodel`, `chao_chaos_devil.bbmodel` | Custom Chaos-form previews |
| `chao_mod_variants.bbmodel` | Retained theme groups, with Create shown by default |
| `chao_egg.bbmodel` | Authored rounded egg with the repaired shell UVs |

Open through **File -> Open Model**. Custom shapes remain editable cubes or meshes, with named rig groups. Theme groups use `variant_<id>_head`, `_body` and `_marker`; animal groups use `animal_<slot>_<index>[_left/right]`. Keep these names so runtime visibility and retargeting can identify them. Nineteen active theme sets remain; the 29 retired sets are absent from the runtime collection.

To apply a custom addition edited in the master:

```bash
python3 art/blockbench_to_runtime.py art/blockbench/chao_adventure_master.bbmodel src/main/resources/assets/chao_garden/models/entity/chao_mesh.json
python3 art/retarget_chao_overlays.py
```

The first command exports the authoring library; the second fits its custom groups to each imported skeleton. An inherited coat, active theme or Chaos state chooses the material at runtime. Animal ears/horns/crests can replace head decorations while the source head and face remain. Replacement limbs follow the imported limb anchors. Chaos takes visual precedence over theme heritage and animal appearance.

For a theme preview in the older authoring project, enable its three groups and load the matching `textures/entity/chao/variant_<id>.png` atlas. This previews the authoring shapes before fitting; inspect the live fitted result in Minecraft as well.

The bridge supports cubes and meshes, per-face and Box UVs, UV rotation/mirroring, cube inflation, pivot rotations, rest rotations and Blockbench 4.x/5.x groups. A zero-depth cube exports as one visible quad. The bridge's first texture page is its runtime material source; extra editor pages do not create runtime material passes.

## Egg editing

The supplied Marketplace pack has no Chao egg geometry or hatching animation. The rounded egg and incubation wobble remain project work. Export egg edits to `models/entity/egg_mesh.json`, then run:

```bash
python3 art/refine_egg_shell.py
python3 scripts/validate_egg_shell.py
```

The shell postprocessor keeps closing longitude panels adjacent in UV space, matches the wrap-edge texels, and synchronizes the editable egg UVs and embedded preview. See [the shell repair](../../docs/EGG_SHELL_015.md).

## Regeneration and checks

```bash
python3 -m pip install pillow
python3 scripts/generate_all_assets.py
./gradlew build coreTest
./gradlew runGameTestServer
```

The full generator rebuilds custom art, recipes and Blockbench projects before importing the official inputs, fitting overlays, merging 0.15 resources and repairing egg shells. It overwrites generated files: keep lasting design changes in the source generator or imported authoring input. Use the targeted export/retarget workflow for a hand-edited custom model rather than regenerating the master afterward.

`art/variant_catalog.py` supplies theme IDs, palettes and ingredients. `art/variant_icons.py` supplies native badge symbols; `art/item_sprite_art.py` supplies eggs and drives. `art/build_adventure_assets.py` authors custom animal/theme/Chaos geometry. `art/import_official_chao.py` and `art/retarget_chao_overlays.py` integrate the supplied forms. The complete pipeline and confirmed tests are documented in [TESTING.md](../../TESTING.md).

Blockbench Animate-workspace clips in the older Generic projects remain editor previews. Live animation now comes from the imported animation JSON plus the gameplay additions in `ChaoModel.java`; editing an old preview clip alone does not alter it.

## Movement controller in 0.16

The imported eight-clip source file remains the reference input. Ordinary gameplay now uses a quiet anchor with occasional idle gestures and point-selected crawling, trained ground gaits, swimming and short ledge glides. These changes live in the Java gameplay pose controller; editing the raw idle clip alone does not replace all gameplay states. Custom overlays continue to inherit semantic bone poses. See [movement mechanics](../../docs/research/MOVEMENT_016.md) and the current release validation.
