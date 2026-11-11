# Release validation — 0.15.0

Target: Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21.

## Build and mechanics

The integrated Gradle build passed with 82,275 deterministic checks: 54,378 state/genetic checks, 25,506 market checks, 1,099 activity checks, 1,260 companion checks and 32 SA2 fruit-effect checks. All 36 server GameTests passed. The new integration cases exercise the actual configured fruit-tree feature and all five allowed natural crops, market-crop rejection, fourteen fruit-to-seed recipes, successful/blocked/creative Chao release, empty legacy-carrier cleanup and vanilla jukebox insertion, playback, comparator output and ejection.

The filled held-Chao item retains its legacy registry ID and saved entity/companion data. Successful release consumes it, including in creative; failed release retains it. Empty carriers are hidden, have no recipe and are removed from old inventories. Existing fruit/tree IDs remain compatible, while new acquisition follows the five-natural/nine-market-first catalog.

## Geometry and resources

The imported-source validator passed 19,746 comparisons over twenty rigs, 366 source bones, 809 cuboids, 63 mirrored cubes, 72 inflated cubes, 69 zero-thickness cubes and 4,576 faces. The twenty PNGs and eight-clip animation input match the bundled asset-only authoring sources. A Minecraft client probe sampled 2,000 finite poses across all twenty rigs and four reactions, including both retained special clips, and checked lowered idle arms and loop endpoints. See [IMPORTED_RIG_015.md](docs/IMPORTED_RIG_015.md).

Variant validation passed nineteen active themes, seventeen optional-mod gates, twenty-nine retired IDs, 397 recipe-presence cases and four deliberate malformed catalogs. Eight earlier editable Blockbench projects passed 64 export/edit roundtrips, 28 cube/plane checks, 553 art contracts and sixteen malformed-project rejections. Those projects supply our custom overlays and cocoon; ordinary base anatomy now comes from the imported Bedrock sources.

Overlay validation passed 15,484 structural checks. Its bounded idle diagnostic sampled eleven ordinary rigs at thirteen times: fitted hand/finger cuboids had no head contact deeper than 0.05 model pixels. Measured contacts were shoulder connectors, also present on native source arms. This is a limited cuboid diagnostic, not a guarantee for every animation and mixed-part combination. See [OVERLAY_RETARGET_015.md](docs/OVERLAY_RETARGET_015.md).

The egg validator passed 96 narrow longitude panels, six corrected closing panels, 187 texture edge joins and an exact editable-model roundtrip; deliberate UV and edge corruptions were rejected. No Chao egg/hatch or fruit asset was present in the supplied world. The repaired egg and SA2-inspired fruit art are authored separately. The transporter model fits one block and retains four-way placement and existing garden behavior.

## Actual client review

The release study completed twenty-three fresh 1280×720 Minecraft captures with synchronized entity states and successful screenshot callbacks. Fifteen feature views cover ordinary/ability Chao, all nineteen themes, rabbit child/adult parts, three Chaos forms, transporter/trader, front/rear egg mapping, both orchards, the fourteen active fruit/seed pairs, actual merchant UI, held Chao and CD tooltip. Eight further captures form an idle review sequence using distinct frames from the same live Chao. The review GIF runs at four frames per second for inspection; this does not represent the game's capture timing.

Facial UVs are upright, resting hands point down/out, goggles and theme badges follow the imported heads/bodies, and the placed End egg no longer has the stretched closing wedge. Source shoulder contacts remain as documented. Orchard screenshots demonstrate grown trees; natural generation itself is covered by server tests. New trees appear in newly generated suitable Overworld chunks.

Optional mod identity fixtures enable theme rendering only. They do not prove full third-party-modpack compatibility. Accessories behavior retains the existing optional integration and prior validation; this release's new client study does not exercise a complete Accessories modpack. Development visual harnesses and fixture classes are excluded from the installable JAR.

The offline gallery references all twenty-three preserved source frames through fifteen feature views and the eight-frame review GIF. The eight-page PDF was rendered and every page was visually checked for legibility, clipping and whole-frame placement. Capture hashes and screenshot callbacks are retained with the release. Packaging checks the current production classes/resources against the build and rejects fixture/private inputs.

## Reproduce

```bash
python3 scripts/generate_all_assets.py
python3 scripts/validate_variants.py
python3 scripts/validate_models.py
python3 scripts/validate_official_chao.py
python3 scripts/validate_egg_shell.py
python3 art/validate_chao_overlay_clearance.py --output docs/validation-0.15/overlay-retarget-checks.json
./gradlew build runGameTestServer
./gradlew runClient -PchaoVisualTest=true -PchaoReleaseStudy=true -PchaoVariantFixtures=true
python3 scripts/package_chao015.py --previews-only
python3 scripts/build_preview_pdf_015.py
python3 scripts/package_chao015.py
```

The development capture world is required for the automatic visual study. Asset reproduction uses the included minimal imported art inputs and existing finite Ogg record; rebuilding its audio from the original two recordings is optional. Earlier validation remains in the versioned documents under `docs/`.
