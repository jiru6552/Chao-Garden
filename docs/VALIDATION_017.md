# Release validation - 0.17.0

Target: Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21. This release changes crawling, shiny materials, the merchant appearance/catalog, wearable hats and mushroom seed acquisition.

## Rules and server integration

The integrated core run passed **172,984 deterministic checks**: 54,378 Chao state, 115,010 market, 1,099 activity, 1,260 companion, 32 fruit and 1,205 locomotion checks. The server completed **all 45 required GameTests** with a successful Gradle exit.

New server cases exercise every registered merchant product and actual large-price payments; inherited shiny egg state; an AI-enabled merchant remaining visible at night; hat equip/swap/removal, save/load and filled-item transport; and returning headwear once at actual reincarnation/death removal. Existing stock/purchase reload, reusable owned toy play/cooldowns, visit budgets, fruit/tree, care, activity, companion and point-based movement tests remain included.

Walking/running/dashing still use 50/667/1334 Run points, swimming/stroke use 100/667 Swim points and short descending ledge glides use 135 Fly points. These use earned points rather than displayed levels. The separate adult Accessories player-glide perk remains Fly level 10. Earlier completed validation is preserved in [VALIDATION_016.md](docs/VALIDATION_016.md).

## Crawl geometry diagnostic

The cuboid crawl diagnostic sampled eleven ordinary rigs at 49 phases each. It checked **2,156 native arm/head pairs and 44,198 fitted-overlay arm/head pairs**, finding no contacts deeper than 0.05 model pixels. The fitted set covers eighteen authored arm species, fourteen equip-compatible. This is a bounded diagnostic at full movement, midpoint breathing and zero head-look/reaction/transition deltas; it does not establish universal animation clearance. The report is preserved in [crawl-clearance.json](docs/validation-0.17/crawl-clearance.json).

## Actual client and previews

The final client run completed **35 fresh 1280x720 screenshots** with all asynchronous save callbacks and a successful Gradle exit. The actual Minecraft pose probe passed **11,044 gameplay samples and 2,000 raw imported poses** across twenty rigs. The new low-speed checks cover 49 moving and nine stopped phases per rig, requiring a visible hand stroke, lifted recovery, opposite contact and a settled stationary pose. Source-alpha masking was exercised by the loaded reflection shader; the final shine sequence contains no detached shiny-only white particles. The separate Chaos aura remains.

The eleven feature views cover crawl side/front, matched ordinary/shiny Chao, matched ordinary/shiny eggs plus Gold and the actual live End-portal egg, the day/night trader, eight rare coats, twelve hats in two views, the five toy inventory items and the merchant UI. All feature views were visually reviewed. The night trader stays visible; the toy tooltip and merchant offers show the rebalanced emerald costs.

The sixteen-frame crawl sequence follows one actual moving UUID over 30 game ticks, travelling approximately **0.16535 blocks** with two-tick sample intervals. The eight-frame shine sequence retains the same four Chao and two eggs, with sixteen ticks between samples. Whole-frame source PNGs and hashes are preserved. Review GIFs use the recorded game-clock intervals at nominal 20 ticks per second without interpolated frames; the last frame holds for the median interval. This is not wall-clock video. Internal contact-sheet inspection confirms the alternating limb phases and moving surface highlights.

The four-page PDF was rendered and every page visually reviewed for legibility, clipping, image placement and complete framing. The offline HTML gallery's JavaScript passed a syntax check, and all thirteen image references resolve locally. The capture manifest preserves the source clock, hashes, original filenames and GIF timing.

## Production and packaging

The final build passed. The production JAR passed its ZIP CRC check, exact byte comparison against **all 4,778 current source resources** intended for production, and comparison of every packaged project class with current compiled output. Development capture/probe classes, test fixtures and fixture-mod metadata are excluded. All twenty inherited-coat directories are present, with **172 textures per rig**, including the restored Run form. The included reflection shader resources are present.

The complete-archive packager verifies CRCs, current capture hashes, current production JAR bytes, offline image links and the current PDF. It requires every source resource and all twenty inherited-coat directories, retaining the legitimate nested `official/coats/run/` art path while excluding root runtime/build folders. The exact 41 imported asset-only source inputs and eight editable Blockbench projects are retained; private inspection inputs, worlds, caches and third-party jars are excluded. Final evidence is in [validation-0.17](docs/validation-0.17/).


## Reproduce

```bash
./gradlew build runGameTestServer
./gradlew runClient -PchaoVisualTest=true -PchaoMarketPolishStudy=true -PchaoVariantFixtures=true
python3 art/validate_chao_crawl_clearance.py
python3 scripts/package_chao017.py --previews-only
python3 scripts/build_preview_pdf_017.py
python3 scripts/package_chao017.py
```

The capture study requires the development ChaoVisual world. Earlier releases and historical galleries retain their original labels and validation limits.
