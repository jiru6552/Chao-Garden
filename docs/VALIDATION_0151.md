# Release validation - 0.15.1

Target: Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21. This patch refines Nether/End egg surfaces and seed item sprites.

## Production scope and build

An independent comparison with the 0.15.0 production JAR found no added or removed entries. Exactly twenty PNG resources changed: two dimension-egg items, two egg shell atlases and sixteen seed icons. The only changed production Java class is `ChaoEggRenderer`, which chooses the native End portal render pass for the active End variant. Release metadata changes to 0.15.1. Geometry, source Chao assets, care systems, planting, recipes, loot and conditional themes are byte-identical in production. The comparison is recorded in `docs/validation-0.15.1/production-diff.json`.

The Gradle build passed, including 82,275 deterministic state, market, activity, companion and fruit-effect checks. The resulting JAR passed ZIP integrity checks. Gameplay is unchanged; the 36 server integration tests from 0.15.0 were not rerun for this rendering/art patch. Their results remain in [VALIDATION_015.md](docs/VALIDATION_015.md).

## Art and geometry checks

Nether now uses finer native-grain netherrack-inspired clusters and cracks, repeating across the shell instead of magnifying one 16px tile. The End item and static fallback have no purple rim. Placed End eggs use vanilla `RenderType.endPortal()` and its own end-sky/end-portal textures with the animated 15-layer shader. The closed rounded mesh is unchanged, so its full-surface projection does not depend on shell UVs. The inventory icon remains a static matching portal motif. See [dimension art](docs/DIMENSION_EGG_ART_0151.md) and [portal renderer](docs/END_PORTAL_EGG_0151.md).

The existing egg validator passed 96 narrow longitude panels, six closing panels, 187 matching wrap edges, an exact Blockbench roundtrip and two deliberate seam corruptions. Variant checks passed nineteen themes, seventeen optional gates, twenty-nine retired IDs, 397 recipe-presence cases and four malformed catalogs.

All sixteen seed PNGs are distinct native 16x16 RGBA images with binary alpha, compact palettes and separated kernels without stray isolated pixels. Fourteen belong to active fruit families; two remain legacy-save appearances. Generator replay produced no additional resource changes. Primary mod images and the design comparison are documented in [seed research](docs/research/SEED_ART_0151.md). These artwork checks verify resource constraints; actual recognizability was checked visually at native/enlarged scale and in Minecraft inventory views.

## Actual Minecraft review

Thirteen fresh 1280x720 client captures completed with synchronized egg state and successful screenshot callbacks: front/rear views of Nether, End and ordinary eggs; fourteen fruit/seed pairs; both dimension item tooltips; and eight frames of the same placed End egg from a fixed camera. Both sides of the End egg render without the old purple border. The Nether shell shows fine mottled texture instead of broad colored patches. Seeds preserve the fruit-linked silhouettes and read as clusters rather than the old common brown bean.

For the motion subset, incubation was held near zero by the development fixture to suppress ordinary hatch rocking. Every consecutive pair changed 11,081-11,247 pixels in a 12,000-pixel shell-interior region. Sky and ground control regions changed zero pixels, and the complete changed region remained within the same 212x287-pixel egg bounds. This confirms visible shader motion independently of camera/background movement. See `docs/validation-0.15.1/portal-motion-checks.json` and the capture manifest's actual game-tick intervals.

The portal GIF uses those eight full source frames sampled roughly 2.5 seconds apart and plays at four review frames per second, an accelerated inspection sequence. It does not represent real-time playback timing. The current gallery includes five feature views and the motion sequence; the earlier 0.15 gallery and twenty-three source frames remain historical.

The new development harness is excluded from the installable JAR, alongside earlier fixtures. This study uses the base mod; it does not establish compatibility with arbitrary shader packs or full third-party modpacks.

The three-page PDF was rendered and every page checked for legibility, clipping and whole-frame layout. The gallery script passed a syntax check; packaging verifies every current offline image link and each original frame hash.

## Reproduce

```bash
python3 scripts/generate_all_assets.py
python3 scripts/validate_variants.py
python3 scripts/validate_egg_shell.py
python3 scripts/validate_seed_sprites.py
./gradlew build
./gradlew runClient -PchaoVisualTest=true -PchaoAssetStudy=true
python3 scripts/package_chao0151.py --previews-only
python3 scripts/build_preview_pdf_0151.py
python3 scripts/package_chao0151.py
```

The opt-in visual study requires the development `ChaoVisual` world. Runtime classes/resources and every original capture are checked against the packaged outputs. The source archive retains the exact 41 asset-only imported inputs and excludes private inspection files, worlds, caches and third-party archives.
