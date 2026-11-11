# Release validation - 0.22.0

The current source passed the integrated build, mechanics suite, both server profiles, clean-copy asset rebuild, production-JAR parity, actual-client study and complete visual/export reviews below. This is the user-defined 50% readiness checkpoint; the percentage names this development milestone rather than measuring complete original-game/CWE coverage. Historical releases remain in [VALIDATION_021.md](docs/VALIDATION_021.md) and earlier validation documents.

## Completed checks

| Area | Result | Evidence |
| --- | --- | --- |
| Mechanics and integrated build | PASS: 431,616 deterministic checks | [Default build/server](docs/validation-0.22/server-default.json) |
| Default NeoForge server | PASS: 122/122 required tests, 6.464 s | [Default evidence](docs/validation-0.22/server-default.json) |
| Real Accessories + identity-only theme fixtures | PASS: 122/122 required tests, 8.124 s | [Accessories evidence](docs/validation-0.22/server-accessories.json) |
| Complete asset rebuild | PASS: all 5,349 resource hashes equal | [Clean-copy comparison](docs/research/CLEAN_FULL_REBUILD_022_CHECKS.json), [successful log](docs/validation-0.22/asset-rebuild.txt) |
| Production JAR | PASS: 189 classes and 5,347 runtime resources byte-identical | [JAR evidence](docs/validation-0.22/production-jar.json) |
| Actual client | PASS: 465 ordered captures and 465 save callbacks | [Runtime](docs/validation-0.22/runtime-client.json), [capture manifest](docs/release-capture-manifest-0.22.0.json) |
| Static review | PASS: all 345 final views | [Static review](docs/validation-0.22/static-review.json) |
| Motion review | PASS: all 120 raw frames, all 24 per sequence retained | [Motion review](docs/validation-0.22/motion-review.json) |
| PDF | PASS: all 135 pages individually inspected | [PDF review](docs/validation-0.22/pdf-review.json), [source provenance](docs/pdf-source-manifest-0.22.0.json) |
| Relative gallery | PASS: 350 cards, 1,134 checks | [Relative results](docs/validation-0.22/gallery.json) |
| Standalone gallery | PASS: 350 embedded images, 1,134 checks | [Standalone results](docs/validation-0.22/gallery-standalone.json) |
| Complete ZIP | Atomic publication gate | CRC, unique paths, current payload parity and all review gates are required before commit; the separate release-owner verification records the final archive result |

## Mechanics and server integration

| Deterministic suite | Passed checks |
| --- | ---: |
| State, genetics, needs and lifecycle | 168,848 |
| Black Market selection and payment | 153,173 |
| Activity rules and rewards | 101,311 |
| Companion rules | 2,091 |
| Fruit effects | 4,984 |
| Locomotion rules | 1,209 |
| Total | 431,616 |

The default integrated run completed successfully in 29 seconds and passed all 122 required GameTests in 6.464 seconds. The real Accessories run completed successfully in 28 seconds and passed all 122 in 8.124 seconds. Its log confirms real Accessories API registration. Exact source-log hashes and result lines are preserved in the two server evidence reports.

Optional coverage uses actual Accessories `1.1.0-beta.54+1.21.1` and oωo `0.12.15.6-beta.1+1.21`, with their exact retained SHA-256 values. Accessories-dependent cases skip in the default profile and execute through the real API in the second profile. The identity-only mods exercise 17 theme gates and registries; they do not implement the named third-party mods.

The focused lifecycle suite contributes 90,611 assertions to the aggregate state total and is not counted twice. It covers every valid visible/hidden carry input, E–S gains and one expressed-allele first-evolution upgrade, the S cap, repeated-life maxima, duration/counter boundaries and terminal care. Chaos unlock requires happiness strictly above 80; ordinary reincarnation and the Omochao care-heart outcome separately use above 50. Normal eligibility follows the requested highest-points/MRU chooser, with Stamina mapping to Normal.

Four new actual-entity server cases exercise physical egg → first cocoon → young/full adult → rebirth cocoon → same-UUID egg/item/reload/placement/hatch; gray death with denied held/worn insertion and recursive callbacks; an Omochao with an already-committed component and blocked equipment return; and maximum-incubation denied hatches through serialization. The karate regression starts through its production begin transaction and actual return journals before testing knockout behavior. Earlier companion, care, breeding, garden, activity, item-decision, construction and recovery cases remain in the required suite.

## Models, resources and repeatability

The authoritative authoring control is the verified 0.21 complete archive, SHA-256 `1ddc03d937194604053ebc5934124041866fdd1ebf7fb6956134ff869e5b34e5`. The full asset CLI passed in an independent clean source copy. Every one of 5,349 rebuilt resources matches the frozen live tree: zero changed, missing or extra files. All 41 supplied asset-only inputs and the archived prototype are unchanged. The rebuild claim is scoped to that clean-copy execution and byte comparison.

The final source proportion guard passed 14,955 checks across 405 age/endpoint cases and 600 rigid limb assemblies. It preserves native eye/front planes, parents, joints, crest lengths, special forms, 351 Chaos coats and all imported inputs; deliberately broken models are rejected. The six corrected ordinary head cores and original shorter Swim profiles are distinguished in the [proportion table](docs/research/PROPORTIONS_022.md). All 19 themes use the actual adult type and gradual age geometry with 209 native-layout atlases.

The item/egg guard covers all 250 retained sprites after nine obsolete sprites are removed, all 314 item models, 389 own texture bindings, 191 placed atlases, 86 equal shiny/base albedo pairs, 683 canonical glyph pixels and separate cocoon surfaces. Four inventory pixels are repaired; unrelated artwork remains unchanged. Resource hygiene passed 2,429 checks and its unknown-selector mutation tests. Exactly 1,615 unreachable derivatives are absent. Eleven coat layouts, thirteen attachment layouts, 22 opaque gameplay rigs and three Chaos tip layers remain. Twenty exact source rigs/textures, eight source clips, historical galleries and editable projects remain available.

The installable `chao_garden-0.22.0.jar` is 12,070,207 bytes, SHA-256 `4b343083d67e1b144cc01fc2d980178c187adb8da150ce94110e3f1c10e54925`. Its 5,660 entries have unique names and clean CRC. The exact 189-class production set and all 5,347 runtime source-resource payloads match the current compiled/tested bytes. Two test structures and all development studies/probes are excluded. The temporary validation environment's JDK/Gradle/download checks are recorded separately in [toolchain.json](docs/validation-0.22/toolchain.json).

## Actual client and visual review

The final Minecraft Java 1.21.1 / NeoForge 21.1.219 run completed successfully in 9 minutes 45 seconds. It produced 345 static views and five complete 24-frame motion sequences, for 465 whole-frame PNGs with 465 asynchronous save callbacks. All 459 non-companion observer views confirm a standing player, no inherited fall-flying/swimming state and at least 1.5 blocks of camera-eye clearance.

The client confirmed 314 registered namespace items across nine actual inventory pages: eight full pages and a 26-item last page. Compatibility aliases are included in this exhaustive registry audit and are not thereby re-enabled for ordinary acquisition. The legacy carrier entry uses a genuine filled Chao snapshot because bare empty carriers correctly remove themselves. Sixteen early lifecycle/grade views verify actual stages, exact before/after values, retained grades, level 1 and zero progress.

Actual in-client probes passed 14,215 proportion assertions; 4,275 theme fitting-helper poses, 855 cap cases and 209 native material cases; 31,725 animated wearable fits; 7,259 gradual/behavior samples; 18,469 gameplay poses plus 2,000 imported poses; 421 coat and 14 inherited robot material cases; and 4,102 surface-cache plus 764 held-cache checks. The exact source-rig probe separately passed 2,000 samples. These numerical/helper/material domains run inside the client; they are distinct from the 465 whole-frame visual captures and are not added into one aggregate assertion total.

All 345 static views were inspected by their assigned owners, including all ordinary form families, all 19 themes and targeted Power/Fly attachments, eleven egg scenes, family/robot lineage, fitted accessories and special/Chaos anatomy. Six actual-slot companion scenes confirm water effects at Swim 0/70/99, effect cleanup on land, ordinary max-Fly flight and all-S Omochao boost with replay denial and its 100-tick cooldown.

All 120 motion sources were reviewed in order, including every walking/robot/orbit boundary frame. The observer follows the real walking route; every actor remains fully on screen. Every GIF retains all 24 source frames and the recorded game-clock intervals. Decoded GIF pixels match independently resized/quantized source frames, with no generated or interpolated motion. Fixture clocks and development coordinates can be accelerated. Test-only lifecycle/orbit views clear setup particles, and lifecycle views remove loose setup shells; production state transitions and mechanics are unchanged.

## PDF, galleries and publication

The 135-page PDF was rendered with Poppler at 110 dpi. Each final page was opened individually at full size by its assigned reviewer: 39 proportion pages, 45 theme pages, 18 item/egg pages, 25 lifecycle/robot/grade pages and eight UI/companion pages. Whole source frames, headings, captions, footers and numbering fit without overlap or clipping. The aggregate [PDF review](docs/validation-0.22/pdf-review.json) binds every source PNG, rendered page and final PDF hash. Its one nonblocking caption-clarity note leaves the correct Dark-head comparison and precise research dimensions intact.

Both relative-path and standalone galleries have 350 cards (345 statics plus five motion reviews) and passed 1,134 checks each. All 350 relative images resolve; the standalone embeds all 350. The exact shipped inline JavaScript was executed through a bounded Node DOM/event adapter for filtering, combined case-insensitive search, empty states, enlargement, visible-result navigation/wrapping, arrows, captions and close/backdrop controls. This checks interactions and links; no graphical browser-layout run is claimed.

Before committing the complete ZIP, the packager requires current production source/class/resource parity, every supplied input, retained editable projects, current capture provenance, complete PDF/static/motion approvals and exact gallery hashes. It verifies CRC, unique paths and archive payload parity without a size ceiling. Runtime worlds, caches, whole Marketplace content, private account/key inputs and third-party mod archives are excluded. The independent release-owner verification supplies final archive sizes, hashes and audit results.

## Reproduction and limits

Use Java 21, Minecraft Java 1.21.1 and NeoForge 21.1.219. Before an automated client study, create a disposable creative superflat development world named **ChaoVisual**. Runtime worlds are deliberately absent from the source download. The restricted validation host additionally uses a small temporary NeoForm adapter when ProcessHandle cannot report its Java command; it falls back to `java.home/bin/java` and changes no application behavior. Its source is preserved under [validation tooling](docs/validation-0.22/tooling/).

```bash
python3 scripts/generate_all_assets.py
./gradlew clean build coreTest runGameTestServer
./gradlew runGameTestServer -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/compatible/dependencies -PchaoVariantFixtures=true
./gradlew runClient -PchaoVisualTest=true -PchaoAudit022Study=true -PchaoVariantFixtures=true -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/compatible/dependencies > ../chao022-client-final.log 2>&1
python3 scripts/package_chao022.py --previews-only --capture-log ../chao022-client-final.log
python3 scripts/build_preview_pdf_022.py
pdftoppm -png -r 110 ../output/pdf/chao-garden-0.22.0-preview-gallery.pdf tmp/pdf022/pages/page
node scripts/check_gallery022.mjs
node scripts/check_gallery022.mjs ../deliverables/chao-garden-0.22.0-gallery.html docs/validation-0.22/gallery-standalone.json
```

Review the new images/pages and bind the reports before the final packager. Full third-party modpacks, internet multiplayer, repeated live reload stress, long-duration load/memory benchmarks and every original-game/CWE feature remain outside this audit. The highest-stat first-type chooser, timing, controls and mechanical reconstruction are documented Minecraft adaptations.
