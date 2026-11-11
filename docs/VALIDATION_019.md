# Release validation - 0.19.0

This document records completed verification for the 0.19 release. Historical 0.18 results are preserved separately in [VALIDATION_018.md](docs/VALIDATION_018.md); they are not evidence that newly added code passed.

## Integration status

The final integrated build and deterministic suite passed, followed by **all 95 required NeoForge server GameTests** in the default profile (`chao019-build-server-final4.log`). The same 95 tests also passed with the real compatible Accessories/oωo dependencies and synthetic theme-mod identity fixtures (`chao019-gates-accessories-final4.log`). Explicit game-server completion reports are required; a Gradle task exit alone is not accepted as evidence of a working game launch.

| Deterministic suite | Passed checks |
| --- | ---: |
| State, genetics, needs and lifecycle | 76,589 |
| Black Market selection and payment | 147,010 |
| Activity rules and rewards | 1,271 |
| Companion rules | 1,260 |
| Fruit effects | 84 |
| Locomotion rules | 1,205 |
| Total | 227,419 |

The complete final asset pipeline passed and a repeat rebuild left all 6,386 resources byte-identical ([rebuild record](docs/research/assets019-rebuild-check.json)). Independent checks include 109,742 new form/parent/pose checks, 1,050 CWE content checks, 26,221 canonical/legacy gene checks, 283 seed checks, 6,486 native-rig checks and 6,379 heritage checks, plus the retained surface/UV/crawl diagnostics. Synthetic theme identities verify gating and registry paths; they do not establish complete third-party modpack compatibility.

The final actual-client study completed with **372 whole-frame PNGs and 372 asynchronous save callbacks**, comprising 84 feature views and six motion sequences. It passed 6,721 new form/behavior probe samples (135 distinct adult endpoints across 15 skeletons), 16,795 gameplay-pose samples and 2,000 raw-source pose samples across 20 rigs. The final run contains no Chao runtime or shader failure. [Client evidence](docs/validation-0.19/runtime-client.json) and [capture manifest](docs/release-capture-manifest-0.19.0.json) preserve its log hash, capture hashes and recorded game ticks. The retained visual study covers unchanged client rendering and assets; the final server-only lifetime contest-counter, rebirth identity and garden-registry corrections are covered by the rerun regression suites. Failed developmental runs are not counted as completed release verification.

The **twenty-page PDF** was rendered with Poppler at 110 dpi and every page individually inspected. All 84 feature views were additionally reviewed in seven contact sheets, with critical UI, content and appearance frames inspected at full size. Whole frames, titles, captions and page numbers fit without clipping or overlap. [PDF provenance](docs/pdf-source-manifest-0.19.0.json) binds every page to this exact study; [render review](docs/validation-0.19/pdf-review.json) records the checks. Six GIF reviews preserve all 288 captured motion frames and their game-tick timing, without interpolation.

The current production JAR is 13,666,274 bytes, with 6,665 ZIP entries, 160 byte-exact production class payloads and 6,384 byte-exact resource payloads. Its CRC and unique-name checks passed; development harnesses, identity fixtures and third-party dependency archives are absent. [JAR evidence](docs/validation-0.19/production-jar.json) records SHA256 `c29537a63a946fa800a2fc43a43e55758294ce5c3a7eac694fa27e3ded01b1d1`.

Packaging enforces current-JAR/resource parity, all 41 supplied asset-only inputs, eight retained editable projects, every raw capture hash, current PDF provenance, offline gallery links and privacy/dependency exclusions before committing the final ZIP. The completed offline gallery has 90 cards (84 features plus six GIF reviews); all 90 image references exist and its inline JavaScript passes Node syntax checking.

## New verification coverage

`KindergartenGameTests` exercises real station blocks and inventories, ownership/range rejection, medicine consumption, loaded lesson completion across entity reload, broken/ill classroom pause and resume, grade-care currency/change and hidden-allele preservation, equipment component conservation, dye consumption, once-only instrument claims and earned-only medal selection. Other modules add their own core, egg, social/behavior, content and contest tests. The real-race regression also verifies that the first win after reincarnation advances the saved lifetime total when the legacy entity counter is absent. The full garden lifecycle regression verifies original UUID preservation through the cocoon, egg recovery, item/entity reload, cross-dimension collision guard and hatch; actual death frees the garden entry, while carrying and rebirth keep it reserved. Test fixtures may accelerate clocks; default gameplay durations remain configurable.

The preview harness captures the actual Minecraft renderer and UI. Whole frames, asynchronous save callbacks, source game ticks, final manifests and hashes are required before a completed study can be packaged. Review GIFs use captured frames only, without interpolation. The metadata-selected PDF must be rendered and inspected page by page.

## Reproduce

Use 64-bit Java 21, Minecraft Java 1.21.1 and NeoForge 21.1.219:

```bash
python3 scripts/generate_all_assets.py
./gradlew build coreTest
./gradlew runGameTestServer
./gradlew runClient
```

The coordinator also runs the same required server test suite with the compatible real Accessories and oωo dependencies. Development capture harnesses and test classes must be absent from the production JAR. The completed release retains the precise supplied asset-only reference inputs while excluding private account/key material, whole Marketplace content, third-party mod archives, runtime worlds, caches and dependency downloads.
