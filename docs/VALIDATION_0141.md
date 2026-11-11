# Release validation - 0.14.1

Target: Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21. This patch changes Neo Vitae's blood-drop symbol and Iron's Spells' scroll at their separately authored native 8px and 16px sizes.

## Artwork and scope

An independent comparison with the 0.14.0 release finds exactly four changed production resources: the two item-egg PNGs and their corresponding Chao texture atlases. No production resources were added or removed. Both runtime mesh JSON files and all Java source remain unchanged. All 28 unrelated magic-icon outputs are byte-identical; the current19-theme catalog and availability rules remain intact.

The generated item sprites match the current egg renderer, and the chest-badge regions match the current16px symbols. Both sizes and their complete egg contexts were visually reviewed: the drop has a narrow pointed tip and a round lower bowl; the scroll has straight parchment, balanced rolled ends and simple ink. Both preserve binary-alpha edges.

## Build and resource checks

`./gradlew build` passed, including the existing core suite's **80,242 assertions**. The production JAR is `chao_garden-0.14.1.jar`.

Variant validation passed19 active themes,17 mod gates,29 retired IDs,397 recipe-presence cases and4 malformed-catalog cases. All eight editable Blockbench projects passed64 export/edit roundtrips,28 cube/plane semantic checks,553 release art contracts and16 malformed-project rejections. Each Chao rig retains222 bones,727 cuboids and52 meshes.

The runtime, save migration, animation and companion code are unchanged. The previous0.14.0 server integration results remain recorded in [VALIDATION_014.md](docs/VALIDATION_014.md); those server tests were not rerun for this pixel-art patch. Earlier animal-pose coverage remains in [VALIDATION_012.md](docs/VALIDATION_012.md).

## Client and previews

Six fresh actual Minecraft frames completed with synchronized theme state and screenshot-save callbacks. The Neo Vitae and Iron’s Spells chest badges were visually checked in those frames. Current captures, the native art sheet, offline gallery and three-page PDF are included. Every PDF page was rendered and reviewed for layout and clipping. The PDF focuses on these two revised symbols; the HTML gallery includes all19 retained themes.

## Reproduce

```bash
python3 art/build_adventure_assets.py --entity-only
python3 art/generate_variant_resources.py
python3 scripts/preview_theme_icons.py
python3 scripts/validate_variants.py
python3 scripts/validate_models.py
./gradlew build
./gradlew runClient -PchaoVisualTest=true -PchaoCollectionStudy=true -PchaoVariantFixtures=true
python3 scripts/package_chao0141.py --previews-only
python3 scripts/build_preview_pdf_0141.py
python3 scripts/package_chao0141.py
```

The development capture world is required for the automated client study. The installer excludes development fixtures and visual harnesses. Optional mod identity fixtures only enable themes for rendering and do not prove compatibility with complete third-party modpacks.
