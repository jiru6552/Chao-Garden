# Release validation - 0.13.0

Target: Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21. This release changes four Chaos Drive sprites, 48 themed egg symbols and 48 themed body-atlas badges. Source-mod textures are not redistributed; all new symbols are original code-authored pixel art.

## Build and retained rules

`./gradlew build coreTest` passes with the existing 82,418 deterministic assertions: Chao state 56,554; Black Market 23,505; activities 1,099; companions 1,260. No gameplay state, recipe, loot, availability, save-schema or production Java implementation changes are introduced. The new Java capture class is a development-only fixture excluded from the JAR.

Availability validation passes all 48 themed asset sets, 46 mod-gated entries, always-available Nether/End and 2,643 recipe-presence cases. The 0.12 standalone integration result remains 29/29 GameTests; no server suite was repeated for these texture-only changes. See [the previous validation](docs/VALIDATION_012.md) for animal animation, journal rendering and prior gameplay integration evidence.

## Asset integration

All 48 theme IDs have separate native 8px and 16px designs, with no missing IDs, overlapping module ownership, generic fallbacks or duplicates at either size. All 96 outputs are nonempty RGBA with binary transparency. The 8px egg marks are explicitly authored instead of downsampling larger symbols. Their canonical item/object colors are not replaced by the Chao coat accent.

Every generated themed egg matches its native 8px design and every themed body atlas contains the exact corresponding 16px badge at the existing material patch. All four drive PNGs match the upright center-line generator. Comparison with 0.12 finds exactly **100 changed production resources, all PNGs**: four drives, 48 themed eggs and 48 themed body atlases. All other resources, including ordinary eggs, genes, audio and all data JSON, are byte-identical.

Both production runtime geometry JSONs are byte-identical to 0.12, as is the production animation code. No additional animation sweep was needed. All eight editable Blockbench projects pass 64 exporter roundtrip/edit checks, 28 cube/plane semantic checks, 1,162 art contracts and 16 malformed-project rejection checks. The themed master embeds the refreshed atlas; other rigs retain their existing geometry. Head validation passes 41 crowns, 1,161 expression rays and two goggle designs.

All **535 JSON and 686 PNG** production resources decode. The authored theme sheet shows all 48 badges, native egg marks and final eggs, including actual-size samples. Source/motif notes link primary mod documentation or repositories in three [technology](docs/research/THEME_ICONS_TECH_013.md), [magic](docs/research/THEME_ICONS_MAGIC_013.md) and [adventure](docs/research/THEME_ICONS_ADVENTURE_013.md) tables.

## Real client review and previews

A real Minecraft client completed **six synchronized whole-frame captures**: the drives with sample eggs, all 48 themed eggs across two inventory views, and three four-Chao groups showing twelve example chest badges. Server/client theme state and rendered-frame barriers are checked before capture. All frames were visually reviewed. The first inventory frame contains an ordinary recipe-unlock toast outside the item grid.

Theme screenshots use identity-only development fixtures to activate the existing mod gates. These are not the actual referenced mods and do not establish compatibility with every full modpack. The fixtures are excluded from the release. The actual output is rendered through Minecraft's item/entity rendering paths. No artwork contact sheet is described as a game frame.

The PDF preview preserves each actual game frame and includes the item sheet plus three pages of sixteen authored theme tiles. It is rendered and visually reviewed for clipping and readability. The complete ZIP includes the current PDF, original PNGs, offline HTML gallery and eight editable Blockbench projects. Older preview studies retain their release labels.

## Reproduce

```bash
./gradlew build coreTest
python3 art/build_adventure_assets.py --entity-only
python3 art/generate_variant_resources.py
python3 scripts/preview_item_sprites.py
python3 scripts/preview_theme_icons.py
python3 scripts/validate_variants.py
python3 scripts/validate_models.py
./gradlew runClient -PchaoVisualTest=true -PchaoIconStudy=true -PchaoVariantFixtures=true
python3 scripts/package_chao013.py --previews-only
python3 scripts/build_preview_pdf_013.py
python3 scripts/package_chao013.py
```

The development capture world is required. The final package verifies source-resource and compiled-class equality with the production JAR, rejects test classes, visual harnesses, identity fixtures and third-party jars, checks capture hashes/CRC/editor projects, and validates current offline gallery image links. ZIP writes are atomic with checksums. Playtesting complete modpacks remains useful.
