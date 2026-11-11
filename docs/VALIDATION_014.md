# Release validation - 0.14.0

Target: Minecraft Java1.21.1, NeoForge21.1.219, Java21. This release narrows the active catalog to19 themes, retires29 appearances with saved-data migration, updates Neo Vitae identity/gating, and refines the requested egg/chest artwork.

## Build and migration

`./gradlew clean build coreTest runGameTestServer` passed. The deterministic runner completed **80,242 assertions**: state/genetics/appearance54,378; Black Market23,505; activities1,099; companions1,260. The reduction from previous totals reflects the smaller availability catalog; retained and retired ID regressions were added.

All **31 required GameTests passed**. The two new tests exercise all29 former theme egg IDs through ordinary placement/recovery, and all29 retired creature/carrier states through loading and release. They verify preservation of genes, training, UUID, owner, name, incubation and other state as applicable. Existing hatching, care, orchard, merchant, activities and companion safety tests remain. A retired theme remains unavailable even if every source mod is reported present. Legacy compatibility shells have ordinary models, no themed art or recipes, and are hidden from creative entries.

Neo Vitae uses the actual `neovitae` ID verified in the publisher's1.21.1 gradle.properties. Core checks distinguish it from the old `bloodmagic` gate and verify the new command alias. The stable serialized ID is retained for compatibility.

## Resources and models

Variant validation passes19 active themes,17 mod gates,29 retired IDs,397 recipe-presence cases and4 malformed-catalog cases. It rejects stale removed recipes, advancements, item PNGs and body/egg atlases, allowing only ordinary compatibility item models. All **478 JSON /599 PNG** production resources decode.

All eight Blockbench projects pass64 export/edit roundtrips,28 cube/plane semantic checks,553 release art contracts and16 malformed-project rejections. Each Chao rig has222 bones,727 cuboids and52 meshes, with4,362 unit-density cuboid faces. All222 retained bone records are structurally identical to0.13; exactly87 retired theme bones were removed. Placed-egg geometry is byte-identical. The production animation implementation is unchanged, so the previous animal pose sweep remains applicable to retained geometry without rerunning it.

Head validation passes16 active crowns,73 native crown blocks,486 expression rays and two fitted goggle designs. Archived crown authoring recipes remain in source for possible future development; inactive recipes are absent from runtime models and metadata.

## Icon and client review

The active19 IDs have native8px marks and16px badges with matching motifs, binary alpha and no duplicates. All generated egg PNGs and body-atlas patches match the current artwork. Ars marks are centered by their painted bounds; Cataclysm's16px and Create's16px artwork remain unchanged. Nether/End item eggs use complete surface textures, and their64px placed-egg atlases use corresponding netherrack or purple-rimmed starfield designs.

Six actual Minecraft captures show all19 eggs, the Neo Vitae tooltip, the thirteen requested appearances on Chao, and placed dimension eggs. Captures wait for matching server/client theme state, rendered frames and both dimension eggs. Identity-only development fixtures open the seventeen mod gates for rendering; these are not full implementations of those mods and do not prove all modpack combinations compatible.

The eight-page PDF preserves six whole game frames plus two enlarged authored-art pages containing every retained theme once. All pages are rendered and visually inspected. The HTML and PDF distinguish authored artwork from actual game frames. Historical galleries retain their original release labels.

## Reproduce

```bash
./gradlew clean build coreTest runGameTestServer
python3 art/build_adventure_assets.py --entity-only
python3 art/generate_variant_resources.py
python3 scripts/preview_theme_icons.py
python3 scripts/validate_variants.py
python3 scripts/validate_head_morphs.py
python3 scripts/validate_models.py
./gradlew runClient -PchaoVisualTest=true -PchaoCollectionStudy=true -PchaoVariantFixtures=true
python3 scripts/package_chao014.py --previews-only
python3 scripts/build_preview_pdf_014.py
python3 scripts/package_chao014.py
```

The development capture world is required. Packaging checks exact source-resource and compiled-class equality with the JAR, rejects retired art/recipes, tests, fixtures and visual harness classes, verifies capture hashes/CRCs and editor projects, and validates offline image links. The current PDF is included. Full modpack combinations and long-term balance still benefit from playtesting.
