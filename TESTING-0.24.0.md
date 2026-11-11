# Chao Garden 0.24.0 verification

Fresh mechanics, actual server, normal-client, visual, production JAR and resource checks pass for Minecraft Java 1.21.1 / NeoForge 21.1.219 / Java 21. Earlier reports and galleries retain their release labels and are historical: [0.23](TESTING-0.23.0.md) and [0.22](TESTING-0.22.0.md).

## Mechanics and actual server integration

The nine-suite runner passes **1,184,990 checks**. Repeated profile runs are not added to this total. [Core evidence](docs/validation-0.24/core.json) binds the full successful log.

| Suite | Passed checks |
| --- | ---: |
| State, genetics, life cycle and theme availability | 168,905 |
| Egg distribution policy | 243 |
| Training supply weights | 248 |
| Wardrobe choices | 6,644 |
| Black Market stock and payment | 899,355 |
| Activities | 101,311 |
| Companion rules | 2,091 |
| SA2 fruit effects | 4,984 |
| Locomotion | 1,209 |

Both actual server profiles pass **154/154 required GameTests** in fresh, separate disposable worlds:

| Profile | GameTest duration | Gradle duration | Evidence |
| --- | ---: | ---: | --- |
| Base mod, without Accessories | 14.48 seconds | 41 seconds | [Default](docs/validation-0.24/server-default.json) |
| Real Accessories plus 17 identity fixtures | 15.12 seconds | 47 seconds | [Accessories](docs/validation-0.24/server-accessories.json) |

The optional profile uses Accessories `1.1.0-beta.54+1.21.1` and oωo `0.12.15.6-beta.1+1.21`; the report records their exact hashes and actual API registration. Slot-dependent cases execute only with Accessories installed. Identity fixtures check mod-list gates and do not implement those third-party mods.

New coverage exercises real enemy deaths with Looting and held drives, passive/canceled/gamerule negatives, the Ender Dragon's special death sequence and saved one-time flag, arbitrary chest contexts, weighted DNA resources, real unlimited drive purchases and finite DNA stock, persistent merchant uses, and resident collision protection.

Wardrobe integration covers accepted and rejected gifts, saved one-time mood choices, item components, denied/reentrant equipment returns, foreign stacked equipment, physical hatch shells, carrying and reincarnation. The six native shell coats include Moon; separate client frames cover ordinary, shiny, jewel, Nether and End masks.

Hub integration covers physical tube placement and entry, portal latches, two-player return journals, saved/reloaded travel, actual respawn, canceled travel and game-mode changes, occupied cursors, safe missing-dimension/invalid-coordinate fallback, and room protection without changing global gamerules.

Kindergarten integration covers actual staff payloads, server nonces, ownership and range, immediate mutation after a read-only refresh, same-item treatment/naming, long Unicode display-name codec bounds, four shared students, persistent same-UUID projections, lesson completion while keepers are elsewhere, inventory-full pickup, reentrant/canceled instrument rewards, staff/student interaction protection, duplicate-resident migration, saved finite DNA uses and visitor-ticket release/reentry.

Flight cases use the actual Accessories slot and vanilla fall-flying hooks at Fly 0, 98 and 99, the independent chest elytra fallback, and repeated midair stop/start/slot changes that cannot renew a finite budget. Existing life-cycle, evolution, fruit, contest and companion cases remain part of both profiles.

## Rebuilt resources and development isolation

The entire asset CLI passes in a fresh source copy in **181.94 seconds**. All **5,311 main resource files** reproduce byte-for-byte, with zero missing, extra or changed paths. The one physically separate GameTest resource also reproduces exactly. All **41 imported authoring inputs**, the archived prototype and the live source resource tree remain unchanged. [Rebuild evidence](docs/validation-0.24/asset-rebuild.json) binds the complete pipeline log.

The main resource-tree SHA-256 is `2ce72906d07e9c168cc0505d19bbbc850717c96c8f1e1f214d4e42c6bbe88789`. The pipeline includes the final weighted DNA, 315-item domain, tube geometry/translucency, resource-hygiene and previous form/egg validators. The gene validator independently passes 17,703 assertions and rejects deliberately broken rarity, registration and chest-guard cases.

Vanilla GameTestServer bakes an empty dimension registry into its flat preset. Its five-dimension fixture therefore lives in **`src/gameTestDimensions/resources`**, physically outside ordinary resources, and is attached only for `runGameTestServer`. The generator removes any legacy main-source copy. Normal client and GameTest runs use separate Gradle invocations. [Fixture design](docs/validation-0.24/gametest-dimensions.json) records the exact preserved vanilla definitions. The production JAR never contains this preset override.

## Actual client review

The final coherent study passes **28 actual 1280×720 captures and 28 SHA-bound save callbacks**. All 28 images were individually opened and inspected after the final run. The [runtime report](docs/validation-0.24/runtime-client.json), [visual review](docs/validation-0.24/visual-review.json) and capture manifest bind the exact source, logs and images.

The normal client resolves the **vanilla three-dimension flat preset**, while both custom Chao dimensions load through the ordinary datapack route. It actually enters the tube, arrives in Adventure mode, crosses the Kindergarten doorway and returns through the saved travel path. The received merchant's synchronized resident flag makes it unpushable on the client too.

Room captures wait for every bounded room chunk to arrive, empty section build/upload queues and forty consecutive stable rendered frames. The final hub and lobby meshes are complete. The views include furnished rooms, the visible principal, legible service screens and selected Chao portraits, the vinyl disc, accepted/rejected gear, five original-shell coats, and active/expired finite flight. The wardrobe geometry and eye-opening probe passes **8,108 runtime cases**.

The Teacher's real LEAVE CHAO action changes occupancy from **0/4 to 1/4**, with the exact selected participant UUID and actual lesson countdown. Fly level 0 uses the normal fall-flying start path with a forty-tick budget: its keeper is prone and the Chao grips the back at elapsed tick 13; by tick 46, both flight and the back attachment have ended.

The [current gallery](docs/release-gallery-0.24.0.html) embeds all 28 pictures in one offline HTML file. It has no external image or network dependencies. The complete ZIP's `previews/index.html` links to it and retains clearly labeled earlier galleries. The 0.22 PDF is historical; no new PDF or audio listening test is claimed.

## Production and packaging

The final production `build coreTest` passes in **12 seconds**. After the final client study, `assemble` passes in **6 seconds** without repeating unchanged tests; the complete payload comparison still matches both server profiles. The JAR is **byte-identical to both successful server-tested artifacts**: **12,177,575 bytes**, SHA-256 `f5531a22bc616b2e66fb97b6e0c5cd0d087ff831a5addbc97812d6dbcf9aaed4`.

All **223 production class payloads** match current compiled output and all **5,306 runtime resource payloads** match source. Missing, extra and changed payload counts are zero; ZIP CRCs and unique entries pass. Development helpers, five main test resources and the isolated GameTest preset are absent. [Production evidence](docs/validation-0.24/production-jar.json) binds the final build, payload manifests and exclusion checks.

The package gate requires seven fresh PASS reports, checks their evidence hashes, compares the exact server-tested JAR with the installable artifact, checks complete class/resource coverage and the physically separated preset, validates safe unique archive paths and CRCs, compares every source payload, and resolves the current and retained galleries' image links. Final archive sizes and hashes are in the separate release verification file.

```bash
./gradlew build coreTest
./gradlew runGameTestServer -PchaoGameTestDir=../chao-tests/base-fresh
./gradlew runGameTestServer -PchaoGameTestDir=../chao-tests/accessories-fresh -PchaoVariantFixtures=true -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/dependencies
python3 scripts/generate_all_assets.py
./gradlew runClient -PchaoVisualTest=true -PchaoHub024Study=true -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/dependencies
python3 scripts/package_chao024.py
```

This verifies the pinned Java 21 / Minecraft 1.21.1 / NeoForge 21.1.219 configuration, including a real software-rendered client and simulated multiple connected players. Full third-party modpacks, external internet multiplayer, every GPU/OS and long-duration performance benchmarks are outside this validation. Already-generated chest contents are not rerolled. Other hub gardens remain decorative, as requested; [release notes](docs/release-0.24.0.md) and [Kindergarten adaptations](docs/KINDERGARTEN_024.md) describe the gameplay scope.
