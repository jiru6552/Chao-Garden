# Release validation - 0.12.0

Target: Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21. This release refines animal geometry, inventory sprites and journal rendering. Item IDs, recipes, training, progression and save formats are retained.

## Build and gameplay regression

`./gradlew build coreTest runGameTestServer` passed. The deterministic suites retain **82,418 assertions**:

| Suite | Assertions |
| --- | ---: |
| Chao state, genetics, lifecycle, availability and animal parts | 56,554 |
| Black Market stock | 23,505 |
| Race and karate rules | 1,099 |
| Companion rules | 1,260 |

All **29 standalone GameTests passed**, exercising persistence, hatching, actual care, partial/mixed genes, trees, loot, market transactions, races, karate and companion safety paths. The real visual client loaded Accessories `1.1.0-beta.54+1.21.1` and oωo `0.12.15.6-beta.1+1.21`. Those external jars are not distributed. The previous 0.11 release also passed the 29-test actual Accessories integration suite; it was not repeated for these presentation changes.

## Animal models and animation

All eight editable Blockbench projects passed 64 export/edit roundtrips, 28 cube/plane semantic checks, 1,162 art contracts and 16 malformed-project rejections. Each Chao rig has 309 bones, 950 cuboids and 91 meshes; its 5,700 cube faces retain unit texel density. Animal slots contain 461 cuboids, up from 232. The separate egg project is retained.

All 200 non-animal bones retain their prior geometry, UVs and hierarchy. The changes affect supplied animal slots and their atlas detail swatches. Insets use opaque colors on offset surfaces. Child parts remain less pronounced than adults. Child muzzles retain side placement while shortening vertically, preventing Otter whiskers from drifting across eye columns.

The final-source full probe passed **176,400 actual production `setupAnim` poses**, **8,584,800 conservative head-to-limb SAT checks** and **110,250 muzzle-to-expression checks**. Minimum separating clearance was **0.0502 model units**. A prior quick pass covered 49,392 poses, 2,403,744 head-to-limb checks and 30,870 expression checks. It covers 48 adult/child/mixed cases and seven activities, including punches, with head yaw to +/-90 degrees and pitch to +/-60 degrees. A temporary fixture restoring only the previous horizontal child-muzzle scaling fails the new Otter expression check. These are selected pose and mixture checks rather than every possible phenotype, environment or terrain interaction.

## Sprites, heads and resources

All 535 JSON and 686 PNG resources decode. Updated art includes all 92 egg variants plus the spawn egg, four upright drives and 42 canonical/legacy gene icons (139 item PNGs). All expected sprites match their generators, have binary alpha and preserve distinct animal palettes. Legacy item aliases match canonical samples; 33 unrelated item PNGs and all five audio files remain byte-identical to 0.11. Isolated gene regeneration preserves 91 unrelated resources.

Variant validation passed 48 asset sets and 2,643 recipe-presence cases. Head validation passed 41 block-built crowns, 1,161 expression rays and two fitted goggle designs. Actual optional-mod gating is unchanged; identity fixtures do not prove compatibility with every full modpack.

## Journal and actual client review

The panel and symbol blits now use alpha blending. Decorative rings/gloss are composited through the rounded inner-shell mask rather than written across its edges. All four panel interiors are opaque, outermost corners are transparent, and the five generated UI PNGs exclude nearly invisible alpha1/2 resampling noise. All ten circular symbols retain antialiased blended edges.

A real Minecraft client completed 16 synchronized full-frame captures. Reviewed views show child/adult Rabbit parts from front and side, Penguin/Sheep/Tiger/Dragon sets, mixed Rabbit/Peacock features, held rendering, upright drives, rounder eggs, simpler DNA vials, and the repaired journal at GUI scales 3 and 1. The study also retains orchard and race-flag views. Capture barriers verify server/client animal slots and rendered frames; the held view requires actual first-person renderer calls. Source-generated sprite sheets are labelled separately from Minecraft captures. The later child-muzzle placement adjustment is covered by the production-model pose check; captured species do not use that muzzle slot.

The 17-page PDF gallery was rendered and visually checked across all pages, with individual checks of animal, journal and sprite pages. The offline HTML gallery uses included PNGs in the complete ZIP; the standalone HTML embeds its images. Historical studies retain their original release labels.

## Reproduce and package

```bash
./gradlew build coreTest runGameTestServer
python3 scripts/validate_models.py
python3 scripts/validate_head_morphs.py
python3 scripts/validate_variants.py
python3 scripts/validate_gene_assets.py
JAVA_HOME=/path/to/jdk-21 python3 scripts/validate_poses.py --forms animals
./gradlew runClient -PchaoVisualTest=true -PchaoRefinementStudy=true
python3 scripts/package_chao012.py
python3 scripts/build_preview_pdf_012.py
```

The development visual world must be available for capture. The installed JAR excludes test classes/structures, visual harnesses, synthetic mod fixtures and third-party jars. Packaging checks exact source-resource and compiled-class equality with the JAR, CRCs, screenshot hashes, eight editor projects, unique archive entries and offline gallery image links. Full modpacks and long-term balance still benefit from playtesting.
