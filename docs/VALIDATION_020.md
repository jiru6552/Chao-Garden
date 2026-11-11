# Release validation - 0.20.0

This document records completed verification for 0.20. Historical results remain in [VALIDATION_019.md](docs/VALIDATION_019.md) and [VALIDATION_018.md](docs/VALIDATION_018.md).

## Integration

The final production gameplay classes passed **101/101 required NeoForge server GameTests** in both the default profile and the profile with real compatible Accessories/oωo dependencies plus synthetic theme identities. The final integrated build also passed all deterministic suites. Explicit game-server completion, rather than a Gradle exit alone, is recorded in [default-server evidence](docs/validation-0.20/server-default.json) and [optional-profile evidence](docs/validation-0.20/server-accessories.json). Subsequent cap metadata corrections changed resources only; the final resource rebuild, build and fresh actual-client study verify those corrections against the same production class payloads.

| Deterministic suite | Passed checks |
| --- | ---: |
| State, genetics, needs and lifecycle | 77,943 |
| Black Market selection and payment | 147,010 |
| Activity rules and rewards | 1,271 |
| Companion rules | 1,260 |
| Fruit effects | 84 |
| Locomotion rules | 1,205 |
| Total | 228,773 |

The complete asset pipeline passed, and a whole repeat left all **6,743 resources byte-identical**, with no added, removed or changed resources ([rebuild evidence](docs/validation-0.20/asset-rebuild.json)). Evolution validation passed **36,084 checks** across fifteen rigs and 135 unique ordinary endpoints, including connected steps, fixed joints/face planes, both Monster-crown scale stages, UVs and closed-cap native/authored crest hiding while preserving the actual core and eyes. Each ordinary axis maximum changes projected head silhouette by at least ten percent in front or profile ([evolution evidence](docs/validation-0.20/evolution.json)).

Independent character geometry/UV checks passed 3,908 assertions. Accessory fitting passed 7,050 independent contact checks over 2,115 compiled production-fit cases, plus 5,379 closed-cap crest/core/eye assertions. The actual client verified 10,575 animated fits over fifteen ordinary families, child, Amy, Omochao and all three Chaos rigs. Source parity, Chaos material/visibility, retirement, UI background, gene, egg, seed, theme and overlay-clearance validators also passed. All forty-one supplied asset-only inputs, twenty original rigs/textures and eight source clips remain present.

## Actual client and visual review

The final study completed **247 whole-frame PNGs and 247 asynchronous save callbacks**: 151 feature views and four motion sequences containing 96 source frames. It passed 6,854 form/behavior samples, 16,795 gameplay-pose samples and 2,000 raw imported pose samples across twenty rigs. Eleven actual renderer material samples verified inherited Omochao pixels, distinct coats, protected hardware, shine and cache identity. [Runtime evidence](docs/validation-0.20/runtime-client.json) and the [capture manifest](docs/release-capture-manifest-0.20.0.json) bind these results to the final log and capture hashes.

Every feature view was reviewed: 45 ordinary-form/limit views, twelve inspection tabs, 63 accessory views, fifteen robot/Amy views, fifteen Chaos views and the retained-character view. Owners also inspected actual moving frames. All four motion reviews retain source-frame game ticks without interpolation. Ordinary +/-1 limits and opt-in magnitude-three Monster progression are labeled separately; Normal's balanced ordinary duplicate is explicit. The compact grids and close triads keep all models and markers in view. The final four aliased-cap cases were checked again after donor-decoration metadata was corrected.

The **55-page PDF** was rendered with Poppler at 110 dpi and every page was individually inspected at full size, with an independent review of all seven page montages and critical full-size pages. Whole frames, headers, captions and numbering fit without clipping or overlap. [PDF source provenance](docs/pdf-source-manifest-0.20.0.json) and [page review](docs/validation-0.20/pdf-review.json) preserve each page's actual source and render hashes. The offline HTML gallery contains 155 cards (151 features plus four motion reviews); all image links resolve and its inline JavaScript passes Node syntax checking ([gallery checks](docs/validation-0.20/gallery.json)).

## Production and scope

The final installable JAR is **14,135,735 bytes**, with 7,022 entries, 169 production class payloads and 6,740 assets/data resource payloads plus metadata. CRC, unique names, exact compiled production-class set and byte parity with current resources passed. Development harnesses, test identities and dependency archives are absent. [JAR evidence](docs/validation-0.20/production-jar.json) records SHA256 `6f46aeec1a6369114007093313103314c5573c509fe3a8655c9672d8dcdc0c09`.

The release packager requires current JAR/resource parity, all 41 source inputs, eight retained editable projects, every capture hash, current PDF provenance, offline image links and privacy/dependency exclusions before committing the complete ZIP. It imposes no file-size ceiling and verifies the archive CRC and resource payloads after writing.

Synthetic theme identities verify gates and registries; they do not establish complete third-party modpack compatibility. Repeated live resource reloads were not stress-tested. Test clocks may be accelerated; ordinary durations remain configurable. This is a Minecraft implementation of the documented Chao systems, with the adaptations recorded in the research notes.

## Reproduce

Use Java 21, Minecraft Java 1.21.1 and NeoForge 21.1.219:

```bash
python3 scripts/generate_all_assets.py
./gradlew build coreTest
./gradlew runGameTestServer
./gradlew runClient -PchaoVisualTest=true -PchaoRefinement020Study=true
python3 scripts/package_chao020.py --previews-only --capture-log ../chao020-client.log
python3 scripts/build_preview_pdf_020.py
```

The release owner also runs the required server suite with compatible real Accessories/oωo dependencies. The complete download retains precise asset-only reference inputs while excluding private account/key material, whole Marketplace content, third-party mod archives, runtime worlds, caches and dependency downloads.
