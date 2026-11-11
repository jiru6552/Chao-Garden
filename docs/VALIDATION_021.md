# Release validation - 0.21.0

This document records completed code, asset, server, actual-client and PDF verification for 0.21. The release packager enforces the archive checks below before committing its output. Historical results remain in [VALIDATION_020.md](docs/VALIDATION_020.md), [VALIDATION_019.md](docs/VALIDATION_019.md) and [VALIDATION_018.md](docs/VALIDATION_018.md).

## Build and server integration

The clean integrated build passed **229,779 deterministic checks** and **117/117 required NeoForge server GameTests** in the default profile (6.116 seconds). The second profile passed **117/117** with real **Accessories 1.1.0-beta.54+1.21.1**, **oωo 0.12.15.6-beta.1+1.21** and identity-only theme fixtures (7.272 seconds). Both results include explicit game-server completion and a successful Gradle build, preserved with source-log hashes in [default evidence](docs/validation-0.21/server-default.json) and [Accessories evidence](docs/validation-0.21/server-accessories.json).

| Deterministic suite | Passed checks |
| --- | ---: |
| State, genetics, needs and lifecycle | 78,230 |
| Black Market selection and payment | 147,010 |
| Activity rules and rewards | 1,271 |
| Companion rules | 1,975 |
| Fruit effects | 84 |
| Locomotion rules | 1,209 |
| Total | 229,779 |

The new real-entity cases cover the first cocoon's highest-stat/history choice and identity lock; held opposite-type drive training; a multi-stat fruit's single maturity commit across an eating reload; and pickup/item-NBT/release persistence. Four robot cases exercise actual C→B→A→S heart reconstruction and creator ancestry, considered heart gifts and second-heart rejection, blocked physical output and reload recovery, and stale creator claims versus a still-living robot.

Eight companion integration cases cover native elytra fallback, actual-slot flight and chest-armor preservation, underwater effects and external-potion ownership, inbound boost replay/cooldown and serialization, two-robot cooldown cycling, finite-glide exhaustion and landing, inbound falling-position packets with zero server velocity, and the actual minimum/maximum attack damage. Accessories-dependent cases explicitly skip when that optional mod is absent; the second profile executes them through its real API.

## Models, assets and repeatability

The complete asset pipeline passed. A full repeat left all **6,791 resources and supplied-input files byte-identical**, with no additions, removals or changes ([repeatability evidence](docs/research/ASSET_REBUILD_021_CHECKS.json)).

Gradual-model validation passed **70,725 checks** across **15 young-adult forms and 135 fully aged targets**, with independent age samples 0, 0.6 and 1.2, signed ordinary axes bounded to −1…+1, connected stepped shapes, protected eyes/joints and natural palette roles ([model evidence](docs/research/GRADUAL_MODELS_021_CHECKS.json)). The retained form/animal-fit suite passed 123,811 checks. Separate overlay checks cover 405 age/endpoint fits and 750 skull-local attachment roots; the restrained core scale is at most 1.08 times the source ([overlay evidence](docs/research/OVERLAY_HEAD_FIT_021_CHECKS.json)).

Character, Chaos geometry/material, retirement, source-parity, seed, egg-wrap, animal-gene, theme-gate and additive-generation validators passed. All **41 supplied asset-only inputs**, **20 original rigs/textures**, and **eight source clips** remain present. Current Chaos head dimensions and connectivity are recorded in [independent anatomy checks](docs/research/CHAOS_021_INDEPENDENT_CHECKS.json).

## Production JAR

The final installable JAR contains **7,044 entries**, including **183 production class payloads** and **6,748 source-resource payloads**. CRC, unique names, exact class set, resource byte parity, development-code exclusions and registered flight Mixins passed. All 183 production class payloads are byte-identical to those that passed both server profiles; the later correction changed only four model JSONs and their hash manifest. [Production evidence](docs/research/PRODUCTION_JAR_021_CHECKS.json) records the **14,207,027-byte** JAR and SHA256 `d516ec4918b80cef85d3cb0e871ad16f2cf06b8f632f7a90d6e02c3a35c9ffbc`.

## Actual client and release artifacts

The final study completed **340 ordered whole-frame PNGs and 340 asynchronous save callbacks**: **220 feature views** and **120 raw frames across five motion sequences**. The captures include all alignment/first-type endpoint grids, maturity triads, opposing and valid mixed secondaries, reversible secondary training, child tendencies, all 63 worn views, actual robot assembly/lifecycle/reconstruction, real Accessories water/flight/boost scenes, and the refined Chaos families. The final ordinary silhouettes were approved across all 15 families. The child tooltip visibly uses its 1.00 clock and prospective first type; adults use their separate 1.20 clock.

The actual client passed **7,259 form/behavior samples**, **31,725 animated accessory fits**, **16,795 gameplay poses**, **2,000 raw imported poses across 20 rigs**, **421 coat-material cases**, and **14 inherited Omochao renderer-material samples**. All 334 observer captures confirmed a standing player and actual camera-eye clearance after companion flight. Real-slot scenes confirm Swim 0 has neither water effect, Swim 70 has night vision, Swim 99 has both effects, and land exit removes the owned effects. Fly 99 and an all-S robot with fresh Fly 0 both enter actual vanilla flight; the robot's boost succeeds, rejects immediate replay and asserts its 100-tick cooldown. [Runtime evidence](docs/validation-0.21/runtime-client.json) and the [capture manifest](docs/release-capture-manifest-0.21.0.json) bind these facts to the final log and PNG hashes.

All **120 raw motion frames** remain available. The five review GIFs use **118 original frames** without synthesized or interpolated images. Walking uses its first **22 of 24** captured frames: the final two reach or cross the static camera boundary and are omitted from the review while remaining preserved in the raw capture set. The other four reviews use all 24 frames. The metadata records the ordered review subset, omission reasons, raw hashes and recorded game-clock intervals. [Motion review](docs/validation-0.21/motion-review.json) records the visual scope.

The **78-page PDF** was rendered with Poppler at **110 dpi**, and every final page was individually inspected at full size by its assigned reviewer. The combined reviews cover all 78 pages exactly once; root also approved representative contact sheets and full-size pages. Whole frames, headings, captions, footers and numbering fit without overlap or clipping. Every page's source PNG, final render and PDF hash is bound by [PDF provenance](docs/pdf-source-manifest-0.21.0.json) and the [aggregate review](docs/validation-0.21/pdf-review.json). Its selected static-page metadata remains unchanged by the GIF-only walking selection.

The relative-path offline gallery and the standalone embedded-image gallery each contain **225 cards** (220 feature views and five motion reviews) and passed **759 bounded interaction/link checks**. The shipped inline JavaScript was executed in Node with a DOM/event adapter: category/search combinations, empty results, enlargement, visible-result next/previous/wrapping, arrow navigation, caption changes and close/backdrop controls all passed. All 225 local images resolve; the standalone has 225 embedded images. No graphical browser executable was available, so these checks do not claim browser-layout testing. [Offline results](docs/validation-0.21/gallery.json) and [standalone results](docs/validation-0.21/gallery-standalone.json) preserve the exact HTML/script hashes.

The packager requires current production-JAR/resource parity, all 41 supplied source inputs, eight editable projects, all 340 raw capture hashes, current selected PDF provenance, offline image links and private-file/dependency exclusions before committing the complete ZIP. It imposes no file-size ceiling and checks CRC, unique archive names and written resource payloads. The release owner's independent final verification is distributed separately. Initial candidate captures are not release evidence.

## Scope and reproduction

Identity-only fixtures verify gates and registries; they do not establish complete third-party modpack compatibility. Repeated live resource reloads were not stress-tested. Test clocks and displayed evolution strength can be accelerated in fixtures; ordinary durations remain configurable. The requested highest-stat/MRU first-type rule, fruit aging, controls and robot lifecycle are documented Minecraft adaptations.

Use Java 21, Minecraft Java 1.21.1 and NeoForge 21.1.219. Before the automated client command, create a disposable creative superflat development world named **ChaoVisual**; quick play expects that name. Runtime worlds are deliberately excluded from the download.

```bash
python3 scripts/generate_all_assets.py
./gradlew clean build coreTest runGameTestServer
./gradlew runGameTestServer -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/compatible/dependencies -PchaoVariantFixtures=true
./gradlew runClient -PchaoVisualTest=true -PchaoGradual021Study=true -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/compatible/dependencies
python3 scripts/package_chao021.py --previews-only --capture-log ../chao021-client.log
python3 scripts/build_preview_pdf_021.py
node scripts/check_gallery021.mjs
```

The release owner renders and inspects every PDF page before running the final packager. The complete source download retains the precise asset-only inputs and editable projects while excluding private account/key material, whole Marketplace content, third-party mod archives, runtime worlds and dependency caches.
