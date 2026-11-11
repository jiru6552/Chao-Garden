# Chao Garden 0.25.0 verification

The current release passed the complete core suite, two fresh NeoForge GameTest server profiles, an actual Minecraft client study, visual review, an isolated full asset rebuild and exact production artifact checks. Machine-readable reports and their bound input hashes are in [docs/validation-0.25](docs/validation-0.25). Earlier results in [TESTING-0.24.0.md](TESTING-0.24.0.md) are historical.

## Mechanics and actual server profiles

The eleven core suites passed **1,406,531 checks**:

| Suite | Checks | Result |
| --- | ---: | --- |
| ChaoState | 168,905 | PASS |
| Egg distribution policy | 243 | PASS |
| Training supplies | 248 | PASS |
| Wardrobe choices | 6,644 | PASS |
| Baby animation timing | 1,489 | PASS |
| Black Market stock | 899,355 | PASS |
| Kindergarten buyback | 220,052 | PASS |
| Activity rules | 101,311 | PASS |
| Companion rules | 2,091 | PASS |
| SA2 fruit effects | 4,984 | PASS |
| Locomotion rules | 1,209 | PASS |

Each profile used a new disposable world and actual NeoForge 21.1.219 / Minecraft Java 1.21.1 game APIs. Successful server runs tested production class/resource payloads equal to the final JAR; the two profile artifact SHA256 values match exactly.

| Profile | Required GameTests | GameTest time | Complete Gradle invocation |
| --- | ---: | ---: | --- |
| default | 175/175 | 32.69 s | 1m 33s |
| accessories | 175/175 | 32.46 s | 1m 28s |

The default profile has no Accessories registration. The second confirms registration with the real Accessories API and runs its slot-specific cases. It also uses 17 identity-only mod fixtures to exercise optional integration gates; these do not establish compatibility with the full third-party mods.

Actual optional dependencies:

- `accessories-neoforge-1.1.0-beta.54+1.21.1.jar` — SHA256 `cbecac21acbaf323349afa263b22cda67c3e836f928f6cf3f951ba460c196d15`.
- `owo-lib-neoforge-0.12.15.6-beta.1+1.21.jar` — SHA256 `5fc306ad3d42e1dc26812f2df856e7c232376405caab6af4f5aaffe89cd2b256`.

The server coverage includes real player travel and game-mode restoration; all six school walking routes through vanilla collision resolution; staff authorization, selection and resident identity; classroom admission, seats, collection, unload/reentry and saved merchant stock; medical/naming actions; whole-lot buyback atomicity and rejection paths; garden egg/carrier/toy interactions; persistent landscaping; and eight natural TV front-approach/play cases across all four facings with capped and uncapped ticks. Existing acquisition, care, lifecycle, genetics, evolution, competition and companion cases remain included.

Asynchronous school fixtures were corrected to await their required published actors and actual ticket-release or completion boundaries. Their identity, stock, lesson and transaction assertions were retained. These are fixture changes; production payloads were unchanged.

## Actual client and visual review

A real integrated Minecraft client produced **42 native 1280×720 world/service captures**, **108 timed baby PNGs** and **20,677 pose checks**. Every saved callback has a bound file digest. Captures wait for the full scene's chunks and 40 consecutive stable render frames rather than accepting partially loaded scenery.

The study covers transparent lobby views and its underside; the first Neutral Garden, actual Chao/egg release and voluntary toy use; the school exterior, foyer and every room; four adult staff from front and oblique angles with a baby comparison; and eleven actual service views, including teacher admission from zero to one student.

All 42 final world captures were individually opened and compared with selected native frames from the two supplied recordings. All nine actual 30 fps action clips were independently inspected using consecutive decoded-frame samples and full-timeline contact sheets, alongside 108 timed PNGs. This is frame-based visual review, not uninterrupted real-time playback or all-frame inspection. The offline gallery embeds all required stills and nine H.264 action clips, with no network dependency.

The full native recording is 3,006 frames / 100.2 seconds at 30 fps. It is not interpolated or retimed and contains no captured audio. The supplied reference does not expose a complete shell-break or clean complete crawl cycle; those remain authored adaptations, as documented in [baby motion](docs/BABY_ANIMATIONS_025.md).

The renderer was Linux Xvfb with Mesa 25.2.8 llvmpipe OpenGL 4.5, using controlled integrated-client fixtures. These checks do not claim external multiplayer, full modpack compatibility, subjective audio approval or representative hardware performance. The recovered baby-client log lacks a wrapper completion footer; saved callbacks, native recording, runtime input domains and exact production payload parity provide its recorded evidence. The report states that limitation explicitly.

## Resources and production artifact

A complete asset CLI ran in a fresh disposable source copy in 328.339 seconds. All **5,863 main resource files** rebuilt byte-for-byte with no missing, extra or changed files. The physically separate **2 GameTest resources**, **41 raw Chao inputs**, archived prototype and **105 frozen pipeline source files** remained unchanged. The main-resource tree SHA256 is `119e3f228026d109ae1e7098890dfa6e476264cd817819bd5f8ad30fd263f18c`.

The environment validator passed 61,533 assertions, including 13 malformed negative fixtures. It checks 79 reusable school decoration IDs, eight internal scene blocks, composed rug/cloud tile closures and transparent-scene resources. The adult staff validator passed 12,870 asset assertions. Existing egg, item, distribution, animal-part and heritage validators run in the same full pipeline.

The normal runtime retains the vanilla three-dimension flat preset and loads the three custom realms through ordinary dimension datapacks. The six-dimension GameTest-only preset is physically separate and excluded from normal resources and the installable JAR. Visual helpers, pose probes, test classes, identity fixtures and third-party dependency binaries are excluded from the release artifact.

Final artifact: `chao_garden-0.25.0.jar`, **13,181,053 bytes**, 6,248 unique ZIP entries, full CRC PASS. Exact comparison found 255 production class payloads and 5,858 current runtime resource payloads; the 5,856 assets/data payload domain has no missing or extra entries. Both successful server profiles and both native client input snapshots match these production payloads.

SHA256: `0df25ffe953b50369e304ee422e001a72121b82f8ec380c5aa73f6dea8934e1c`.

## Reproduction and export

Use Java 21, the included Gradle 9.2.1 wrapper and NeoForge 21.1.219:

```bash
./gradlew build coreTest
./gradlew runGameTestServer -PchaoGameTestDir=/tmp/chao-default-check
./gradlew runGameTestServer -PchaoGameTestDir=/tmp/chao-accessories-check -PchaoVariantFixtures=true -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/actual/dependencies
python3 scripts/generate_all_assets.py
./gradlew runClient
```

Use distinct fresh game directories. The dependency directory must contain the exact Accessories and owo JARs listed above. The pinned validation environment and adapter provenance are recorded in `docs/validation-0.25/toolchain.json`; build tooling binaries are not bundled. On Windows use `gradlew.bat` and suitable local paths.

The complete export includes all editable source/native assets, the Gradle wrapper, current machine-readable proof, selected original-resolution reference frames, current captures, raw native action recording and self-contained gallery. Earlier generated preview media remain in earlier saved releases. The two large supplied recordings are represented by provenance hashes and selected frames rather than duplicated. Release packaging verifies every included source payload, archive CRCs, unique safe paths, the tested JAR, current image hashes, proof inputs and executable launcher permissions. `scripts/package_chao025.py` and `scripts/checkpoint025.py --verified` require all seven current PASS reports.
