Current focused update: [0.25.84 development report](docs/FIXES_02584_DEV.md). Use Java 21 and disposable worlds; the report records actual results and separates fixture checks from client inspection.

```sh
python3 -B scripts/test_duck_tambourine_assets_02584.py
./gradlew --no-configuration-cache coreTest babyPowerRigTest084 blockyRunMorphRigTest055 heroRunMaterialTest055 rattleAnimationTest
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_fixes84,chao_run84,chao_bonus80,chao_maintenance83 -PchaoGameTestDir=/path/to/private/fixes84-world -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/actual/optional-mods
```

The two tambourine rig checks are part of `rattleAnimationTest`, not standalone Gradle tasks. The complete catalogue also retains prior morphology, toys, care, garden and companion namespaces. Ordinary client studies must use the normal dimension datapacks. Operator examples: `/chao refresh ~`, `/chao refresh "My Chao"`, `/chao refresh held`, and existing `/chao testtoy tambourine once|loop|stop`. Refresh never fills visual maturity or triggers evolution.

Previous maintenance pass: [0.25.83 report](docs/MAINTENANCE_02583_DEV.md). Use Java 21 and disposable worlds. The report records actual outcomes; commands alone are not evidence of a pass.

```sh
python3 -B scripts/audit_resource_links_083.py --json /path/to/resource-audit.json
python3 -B scripts/test_resource_audit_083.py
python3 -B scripts/test_journal_content_02518.py
python3 -B scripts/test_validator_output_083.py
./gradlew --no-configuration-cache --continue clean build
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_garden,chao_maintenance83 -PchaoGameTestDir=/path/to/private/maintenance-test-world
```

The complete regression uses the retained prior toy, morphology, equipment, garden and baby-alignment namespaces as well. Actual Accessories checks require the optional test runtime (`-PchaoAccessoriesTest=true` with a directory containing Accessories/owo). Test fixtures and visual-study helpers stay outside the release JAR. Ordinary clients use normal dimension datapacks; the isolated GameTest preset must never be installed in a user world.

No gameplay preview commands changed. Existing `/chao testidle ...` and `/chao testtoy ...` commands reuse production poses; use their once/loop/stop controls for manual visual, audio and interruption checks. Custom shader packs, resource packs and third-party modpacks need their own checks.

Previous focused update: [0.25.78 idle variety](docs/IDLE_ANIMATIONS_02578.md). Use Java 21 and private test worlds.

```sh
./gradlew --no-configuration-cache coreTest idleRigTest078 assemble
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_idle78,chao_faces76 -PchaoGameTestDir=/path/to/private/idle-test-world
```

Operator previews: `/chao testidle list`, `/chao testidle <look_around|look_up|think|bashful|chuckle|tired|ground_play> once|loop|stop`, and `/chao testidle stop`. The same short production action plays inside the existing 160-tick preview session; the remainder is calm. A forced tired preview changes no real fatigue. Check babies/adults, entry/exit, natural selection, high-priority interruption and a second player's view. The report distinguishes authored cases, numerical checks and actual visual inspection.

Previous focused update: [0.25.77 prone head correction](docs/PRONE_HEAD_02577.md).

```sh
./gradlew --no-configuration-cache activeToyRigTest049 quietToyRigTest049 proneToyRigTest051 assemble
```

Use `/chao testtoy toy_car once|loop|stop` and `/chao testtoy crayons once|loop|stop` with operator permissions. Check side views, entry, interruption and return to idle on babies and adults. These are the existing production previews and retain their normal cleanup and save protection.

Previous focused update: [0.25.76 facial-expression verification](docs/FACES_02576.md). Use Java 21 and private test worlds. Read the report for actual outcomes and visual limits.

```sh
./gradlew --no-configuration-cache coreTest faceGlyphRigTest076 assemble
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_faces76 -PchaoGameTestDir=/path/to/private/face-test-world
```

Operators can use `/chao testface list` and `/chao testface happy once`, `/chao testface sleep loop`, `/chao testface stop`. Facial previews reuse the production resolver and existing 160-tick preview lifecycle, without changing actual mood, illness, personality, genes or care. Test real illness onset/recovery and natural toy interactions separately. Native Chaos and character artwork is preserved.

Previous focused update: [0.25.75 radio toy verification](docs/RADIO_TOY_02575.md). Use Java 21 and private test worlds.

```sh
./gradlew --no-configuration-cache coreTest radioToyRigTest075 assemble
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_radio75 -PchaoGameTestDir=/path/to/private/radio-test-world
```

Radio previews require operator permission level 2, a real placed radio and a Chao already within 0.4 blocks of its clear front listening seat. Use `/chao testtoy radio once`, `/chao testtoy radio loop` and `/chao testtoy radio stop`. They share the natural interaction's pose, clock and cues; they grant no care rewards and restore temporary AI state. Natural approaches must be tested separately, including from behind.

The focused radio catalogue passed 71/71. The broad catalogue passed 4,461/4,490, with the same 29 inherited failures as 0.25.74. Radio notes use the Blocks sound category, so enable Master and Blocks volume when listening. See the report for actual client views, sound limits and remaining multiplayer checks; automated assertions are not a physical listening pass.

Previous focused update: [0.25.71 Dark Power and Dark Run verification](docs/DARK_POWER_02571.md). Use Java 21 and private test worlds.

Previous focused update: [0.25.69 Dark/Fly-first verification](docs/DARK_FLY_02569.md). Use Java 21 and private test worlds.

Previous focused update: [0.25.68 Dark/Swim-first verification](docs/DARK_SWIM_02568.md). Use Java 21 and isolated test worlds; the current-family commands and coverage are in the 0.25.68 section below.

Previous focused update: [0.25.67 Hero/Power-first verification](docs/HERO_POWER_02567.md).

Previous focused update: [0.25.62 Neutral/Run-first verification](docs/NEUTRAL_RUN_02562.md).

```sh
python3 scripts/validate_neutral_run_morph_02562.py
./gradlew --no-configuration-cache --continue build
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56,chao_heronormal57,chao_swim59,chao_darknormal60,chao_fly61,chao_run62 -PchaoGameTestDir=/path/to/private/test-world
```

Read the family report for actual outcomes and bounded visual coverage. The complete build includes new core, production-emitter rig/material and retained checks; it is one invocation when it also supplies focused results.

Previous focused update: [0.25.59 Neutral/Swim-first verification](docs/NEUTRAL_SWIM_02559.md). Use Java 21 and a private test world:

```sh
python3 scripts/validate_neutral_swim_morph_02559.py
./gradlew --no-configuration-cache coreTest neutralSwimMorphRigTest059 neutralSwimMaterialTest059 heroNormalMorphRigTest057 heroNormalMaterialTest057 neutralMorphRigTest056 neutralMaterialTest056 blockyRunMorphRigTest055 heroRunMaterialTest055 pianoRigTest organRigTest
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56,chao_heronormal57,chao_swim59 -PchaoGameTestDir=/path/to/private/test-world
```

Previous focused update: [0.25.58 Neutral/Hero refinement verification](docs/MORPH_REFINEMENT_02558.md). Existing task/resource identifiers `056`/`057` are retained for these families. Use Java 21 and `--no-configuration-cache` for the existing resource-fixture task:

```sh
./gradlew --no-configuration-cache coreTest heroNormalMorphRigTest057 heroNormalMaterialTest057 neutralMorphRigTest056 neutralMaterialTest056 blockyRunMorphRigTest055 heroRunMaterialTest055 pianoRigTest organRigTest
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56,chao_heronormal57 -PchaoGameTestDir=/path/to/private/test-world
```

Native tests and visual-study helpers remain excluded from the installable JAR. See the family report for actual results and limits; raw generic toy-model loops do not establish all mature family/contact combinations.

Previous focused update: see [the 0.25.56 verification report](docs/NEUTRAL_FOUNDATION_02556.md) for final outcomes, inherited failures and client limits. Coordinated commands use `--no-configuration-cache`, because the existing resource-fixture task accesses `Task.project` during execution.

```sh
./gradlew --no-configuration-cache coreTest neutralMorphRigTest056 neutralMaterialTest056 blockyRunMorphRigTest055 heroRunMaterialTest055 pianoRigTest organRigTest
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56 -PchaoGameTestDir=/path/to/private/test-world
```

The installable JAR excludes native test classes and opt-in visual-study helpers. Use matching client/server versions and external Lionfish API3.1. Do not point a development fixture run at a real user world.

# Chao animation and sound previews

Requires Chao Garden 0.25.28 or later, cheats/operator permission level 2, and a nearby Chao on dry solid ground. The normal renderer and external Lionfish API 3.1 are used. No toy item or daycare lesson is required for these temporary previews.

Stand within 12 blocks of the Chao you want to preview. The nearest ordinary Chao is selected; other Chao are untouched.

```mcfunction
/chao testtoy list
/chao testtoy rattle once
/chao testtoy bubble_wand once
/chao testtoy maracas once
/chao testtoy bell once
/chao testtoy tambourine once
/chao testtoy castanets once
/chao testtoy tambourine loop
/chao testtoy stop
```

Omitting `once` also plays one complete 160-tick (8-second) cycle. `loop` repeats complete cycles, including raising and lowering the hands. Run another toy command to switch. `/chao testtoy <toy> stop` also works when that toy is the nearest active preview. Tab completion lists supported toys and modes.

The preview pauses autonomous AI and the Chao's age/care simulation, grants no rewards, does not teach lessons, and creates no real held/dropped toy or placed block. It preserves the original NoAI and Stay settings. Actual damage or petting still has its normal gameplay effects after interrupting the preview.

A preview stops on right-click interaction, damage, unloading or reloading the Chao, the initiating player disconnecting or changing dimensions, or that player moving more than 64 blocks away. Explicit stop searches for the nearest active preview within 12 blocks. Other players can see the animation and hear its normal sound cues.

Busy Chao, held items, cocoons, sleeping/eating Chao, school residents/enrolled students, companions and activities are rejected with an explanation. Retrieve a daycare student first. The source-reference comparison mode must be turned off with `/chao appearance garden` before previewing gameplay animations.

For listening: set Master and Friendly Creatures volume above zero. Enable subtitles if helpful. Listen for tone, volume, clipping and tap/shake synchronization; test multiple nearby Chao for overlapping sound. These commands play the whole performance; `/playsound` plays just an individual audio cue. The bubble wand uses its existing bubble-pop sound rather than a new instrument sound.

These commands preview presentation. They do not substitute for testing natural toy selection, approach/pathfinding, placed-block restoration or daycare learning/rewards. New or revised toys and animations must include corresponding preview support; see AGENTS.md.


## 0.25.29 instrument previews

With operator permission, stand within 12 blocks of an ordinary idle Chao:

```text
/chao testtoy list
/chao testtoy tambourine once
/chao testtoy castanets once
/chao testtoy cymbals once
/chao testtoy cymbals loop
/chao testtoy stop
```

The previews use the production poses and cues. Repeat with a baby and an adult, then check side views, interruption, and a second player's view. The tambourine's decorated face should face upward; castanet open lids should remain below and ahead of the head. Listen for one cymbal clash at each paired rim contact. No items or lessons are required for previews, and previews grant no rewards. A busy, enrolled, wet, airborne, or otherwise incompatible Chao is rejected.


## Organ 0.25.44

See [organ geometry, preview commands and verification](docs/ORGAN_02544.md). Native client captures verify presentation separately from automated assertions; this cloud run uses a silent audio backend, so listening and independent multiplayer checks remain manual.


## Guitar 0.25.45

See [guitar interaction, commands and validation](docs/GUITAR_02545.md). Native client captures are separate from rig assertions; cloud audio used a silent device. Listening, separate-client multiplayer and full-modpack testing remain manual.

## Focused fixes 0.25.46

See [causes, verification and manual review](docs/FIXES_02546.md) for organ/guitar contact, equipped input/back attachment, bounded running cadence and lobby translucency. Real Solas V3.7b and Accessories follow-up results are recorded in [0.25.47 verification](docs/REAL_MOD_CHECKS_02547.md).


## Piano and accordion 0.25.48

See [interaction changes, preview commands and verification](docs/INSTRUMENTS_02548.md). Use `/chao testtoy piano once|loop|stop` with a real piano nearby, or `/chao testtoy accordion once|loop|stop`. `/chao testtoy stop` stops the current preview. These are operator commands; previews use the production clock, visible pose and sound while preserving care, lessons, real items and AI flags.

Check baby/adult and replacement hands from the front and both sides, all four piano facings, accordion keys facing outward, bellows opening and closing, entry/exit, damage, stop, movement and removal of the placed toy. Listen to the harp/accordion timing and volume with multiple Chao; independent multiplayer and full-modpack checks remain manual.

The full 0.25.48 `build` still hits an unchanged triangle transition assertion that was reproduced against the delivered 0.25.47 JAR. The installable JAR assembles successfully; see the report for baseline proof and the passing focused checks.


## Garden toys 0.25.49

See [per-toy changes and verification](docs/TOYS_02549.md), [source references](docs/TOY_REFERENCES_02549.md) and [coffin retirement compatibility](docs/COFFIN_RETIREMENT_02549.md). All new toys expose `/chao testtoy <id> once|loop|stop`; horse and pogo require a nearby matching placed toy. The opt-in `gardenToy049Study` client creates a disposable world and supports `gardenToy049Start` / `gardenToy049Limit` for individual quality passes.


For the 0.25.50 hat, complete-DNA, stock-rotation and gem-egg update, see [results and compatibility](docs/ITEM_DNA_EGGS_02550.md), [hat migration](docs/HATS_02550.md) and [Lootr chest behavior](docs/GEM_EGGS_02550.md). Existing toy preview commands are unchanged.


For 0.25.51, see [toy polish verification](docs/TOY_POLISH_02551.md) and [Shadow/icon asset references](docs/TOY_ASSETS_02551.md). The existing broom, book, dolls, crayons and car previews use their revised production interactions. Car/crayon previews require clear supported floor space.


## Hero Run morphology prototype (0.25.52)

See [the implementation and results report](docs/HERO_RUN_02552.md) and [reference audit](docs/HERO_RUN_REFERENCES_02552.md). Use Java 21 and the existing external Lionfish API 3.1 dependency.

```sh
./gradlew coreTest heroRunMorphRigTest052
./gradlew runGameTestServer -PchaoGameTestNamespaces=chao_morph52 -PchaoGameTestDir=run-morph052-tests
./gradlew runClient -PheroRun052Study=true -PheroRun052Start=0 -PheroRun052Limit=27 -PheroRun052Dir=run-morph052-visual
```

Run the server and client in separate invocations. The development client helper creates a fresh isolated flat world and intentionally stages training/maturity values; it is excluded from the production JAR. Scenes 18–20 require the real Accessories/owo test runtime supplied via the existing `chaoAccessoriesTest`/`chaoAccessoriesDir` properties. The helper's frames and normal-time recording complement, rather than replace, manual checks in a real save and multiplayer.

Production evaluation uses Hero Garden Fruit, Run drives, Creative Analyzer edits and `/chao inspect`; `/chao mature` is the existing first-evolution shortcut. Existing toy once/loop/stop previews remain available. Stage percentages in the galleries are controlled inputs, not a claim of having observed an entire natural lifetime.


## Baby Run revision and alignment control (0.25.53)

See [the implementation and results report](docs/BABY_RUN_02553.md) and [reference notes](docs/BABY_RUN_REFERENCES_02553.md). In Creative, hold an Analyzer and open **Mind → Profile** to drag the bottom-right Dark/Neutral/Hero bar for an available baby. Survival is read-only; adult alignment remains locked even in Creative. Verify save/reload and the same baby's world appearance after each edit.

```sh
./gradlew coreTest babyRunMorphRigTest053
./gradlew runGameTestServer -PchaoGameTestNamespaces=chao_baby53 -PchaoGameTestDir=run-baby053-tests
./gradlew runClient -PbabyRun053Study=true -PbabyRun053Start=0 -PbabyRun053Limit=26 -PbabyRun053Dir=run-baby053-visual
```

Serialize server and client runs. The opt-in helper creates an isolated flat world, uses actual gameplay rendering and stages controlled training values. Its last two equipped scenes need real Accessories/owo supplied through `chaoAccessoriesTest` / `chaoAccessoriesDir`. Development helpers and native fixtures are excluded from the production JAR. The report separates actual client observations from mathematical and dedicated-server checks and identifies inherited build failures.

Compare fixed-alignment normal/max babies and 25/50/75% Run stages from front, side and back. Inspect walking, expressions, compact hands/feet, hats, animal parts, toys and equipped poses. Use the existing `/chao testtoy broom once`, `/chao testtoy picture_book loop` and `/chao testtoy stop` controls to recheck production attachment poses without rewards. Shaders, full modpacks, remote-player views and listening remain manual checks.


## Archived viewer-guided experiment (unpublished 0.25.54)

The .54 rounded-model draft was paused and superseded by .55. Its [retained draft report](docs/HERO_RUN_VIEWER_02554.md), research and explicit diagnostic commands are historical material. They do not establish a completed .54 release or current .55 client/native test results. Use the .55 workflow below for production validation.

```sh
./gradlew coreTest heroRunMorphRigTest054 heroRunMaterialTest054
./gradlew runGameTestServer -PchaoGameTestNamespaces=chao_morph54 -PchaoGameTestDir=run-hero054-tests
./gradlew runClient -PheroRun054Study=true -PheroRun054Baseline=false -PheroRun054Start=0 -PheroRun054Limit=45 -PheroRun054Dir=run-hero054-visual
```

Serialize server and client runs. The final equipped scenes require real Accessories/owo supplied through `chaoAccessoriesTest` / `chaoAccessoriesDir`. The development helper and native fixtures are excluded from the production JAR. The final report lists the additional mesh/material checks and actual outcomes; a completed fixture design alone is not a visual or test pass.

## Blocky Hero Run path (0.25.55)

See [implementation and actual verification](docs/HERO_RUN_BLOCKY_02555.md) and [reference evidence](docs/HERO_RUN_REFERENCES_02555.md). Use Java 21, Minecraft 1.21.1, NeoForge 21.1.219 and external Lionfish API 3.1. Run the server and client in separate invocations so the isolated GameTest dimension fixture cannot enter an ordinary client run.

```sh
python3 scripts/validate_blocky_run_02555.py
./gradlew coreTest blockyRunMorphRigTest055 heroRunMaterialTest055
./gradlew runGameTestServer -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55 -PchaoGameTestDir=run-morph055-tests
CHAO_RUN055_SMOKE=true CHAO_RUN055_START=0 CHAO_RUN055_LIMIT=30 CHAO_RUN055_OUTPUT=/tmp/chao-run055-visual ./gradlew runClient
```

The client helper is development-only, creates an isolated world and uses the production renderer. `START` is inclusive and `LIMIT` is an absolute exclusive case index; split the 30-case cohort across runs if needed. Its controlled photos compare Neutral/Hero babies at 0/25/50/75/100% Run, young/fresh adults and independent adult training/maturity, from front, both sides, back, above and below. Frozen pre-evolution babies and suspended photo fixtures expose the intended endpoints/undersides; they do not introduce normal hovering or a permanent baby-at-evolution gameplay state. Motion, native toys, the genuine Analyzer and equipped cases are recorded separately.

The final equipped cases need real Accessories/owo supplied through `-PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/accessories-mods`. An unavailable optional runtime is an explicit skip, not a pass. Test helpers, native classes and fixture data remain excluded from the production JAR. The current renderer check is `blockyRunMorphRigTest055`; the unpublished .54 rig task remains an explicit historical diagnostic outside the default `check` dependencies.

In your save, compare the fixed head box, front nubs, stepped rear branches and Hero stem/fan at normal, intermediate and maximum development. Raise and lower Run, add opposing Power, try combined drives, then save/reload. In Creative, drag Analyzer **Mind → Profile** through Dark/Neutral/Hero and confirm the baby updates; Survival stays read-only and an adult stays locked. Compare the same form across natural, named two-tone, monocolor, jewel and shiny coats, and check eyes, animal parts, hats, glasses, hands/feet and player attachments in motion. Existing `/chao testtoy broom once`, `/chao testtoy picture_book loop` and `/chao testtoy stop` previews use production animations without rewards.

Use the final report to distinguish actual build/native/client passes, skips and inherited failures. Remote-player views, latency, arbitrary resource packs/shaders, a full modpack, an actual user's existing world and sound listening require separate verification when absent from that report.

## Dark / Neutral-first 0.25.60

The family and its current verification are documented in [DARK_NORMAL_02560.md](docs/DARK_NORMAL_02560.md). New checks are `coreTest`, `darkNormalMorphRigTest060`, `darkNormalMaterialTest060`, the `chao_darknormal60` dedicated-server namespace and the opt-in excluded-from-JAR `DarkNormalVisualSmoke060` helper. Existing family, toy/transition and keyboard assertions remain enabled. Original full canvas/PNG hashes, actual case settings and development-classpath/JAR binding accompany delivery. A pixel/geometry assertion is not a visual inspection; physical touch, listening and independent remote-client latency need separate checks.

## Neutral / Fly-first 0.25.61

See [NEUTRAL_FLY_02561.md](docs/NEUTRAL_FLY_02561.md) for final verification and limits. New checks are `coreTest`, `neutralFlyMorphRigTest061`, `neutralFlyMaterialTest061`, dedicated namespace `chao_fly61` and excluded-from-JAR `NeutralFlyVisualSmoke061`. Previous family and toy/keyboard limits stay enabled. Screenshot/motion, actual live viewer settings and development-classpath/JAR byte bindings accompany delivery; geometric assertions do not constitute visual inspection or listening.


## Neutral / Power-first 0.25.63

See [NEUTRAL_POWER_02563.md](docs/NEUTRAL_POWER_02563.md) for actual outcomes and visual limits. Run with Java 21 and the existing external Lionfish API 3.1:

```sh
python3 scripts/validate_neutral_power_morph_02563.py
./gradlew coreTest neutralPowerMorphRigTest063 neutralPowerMaterialTest063 pianoRigTest organRigTest
./gradlew runGameTestServer -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56,chao_heronormal57,chao_swim59,chao_darknormal60,chao_fly61,chao_run62,chao_power63 -PchaoGameTestDir=run-power063-tests
CHAO_POWER063_SMOKE=true CHAO_POWER063_OUTPUT=/tmp/chao-power063-visual ./gradlew runClient -PneutralPower063Dir=/tmp/chao-power063-world
```

Serialize server/client invocations. The equipped native scene needs real optional Accessories/owo through `chaoAccessoriesTest` and `chaoAccessoriesDir`; absence is a skip rather than a visual pass. The smoke helper remains excluded from the production JAR. Its authored case plan is not an executed test result. Compare actual viewer/client sources at fresh, intermediate and mature ages, both signed axes, stat gain/loss, pair hold/reversal and Power-through-balance-to-Run; check the fixed head, caps, wings, tail, eyes, coat/shine, animal parts, hats, toy grips and player attachments. Entity-NBT and tracked-state checks do not establish a complete world restart or independent remote multiplayer; physical touch and listening require their own evidence.


## Hero / Swim-first 0.25.64

See [HERO_SWIM_02564.md](docs/HERO_SWIM_02564.md) for actual results, side-by-side viewer/client comparisons and limits. Run with Java 21 and the existing external Lionfish API 3.1:

```sh
python3 scripts/validate_hero_swim_morph_02564.py
./gradlew coreTest heroSwimMorphRigTest064 heroSwimMaterialTest064 pianoRigTest organRigTest
./gradlew runGameTestServer -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56,chao_heronormal57,chao_swim59,chao_darknormal60,chao_fly61,chao_run62,chao_power63,chao_hero_swim64 -PchaoGameTestDir=run-hero-swim064-tests
CHAO_HEROSWIM064_SMOKE=true CHAO_HEROSWIM064_OUTPUT=/tmp/chao-hero-swim064-visual ./gradlew runClient -PheroSwim064Dir=/tmp/chao-hero-swim064-world
```

Serialize server/client runs. The equipped scene requires real optional Accessories/owo through `chaoAccessoriesTest` and `chaoAccessoriesDir`; an unavailable runtime is a skip, not a visual pass. The dev-only smoke helper stays excluded from the production JAR. Authored cases are not execution results. Compare newly evolved, intermediate and mature shapes and colors, both signed axes, opposite Fly beyond Swim balance, gain/loss and held-pair reversal. Check the fixed head, fins, halo, wings, tail, eyes, coat/shine, animal parts, hats, toy grips and player attachments. Existing `/chao testtoy list`, `/chao testtoy broom once`, `/chao testtoy picture_book loop` and `/chao testtoy stop` use production previews and cleanup; distinguish actually exercised modes from those only available. Entity NBT/tracked observer checks do not establish a full world restart or independent remote multiplayer. Physical touch and audio listening require separate evidence.


## Hero / Fly-first 0.25.65

Use Java 21 and a private test world. New focused tasks are `coreTest`, `heroFlyMorphRigTest065` and `heroFlyMaterialTest065`; `build` retains all completed family and toy checks. The dedicated namespace `chao_hero_fly65` adds real command, Creative-payload, ordinary/combined drive, locked-identity, reduction, NBT and tracked-data tests. Run it together with the retained morphology/edit namespaces for regression coverage. The rig suite also checks complete posed wing/head separation and wing-root/torso contact through movement, gaze and half-tick posture transitions; its tolerances remain fixed.

The current opt-in development helper `CHAO_HEROFLY065V2_SMOKE=true` uses `-PheroFly065Dir=<private directory>` and saves literal client captures to `CHAO_HEROFLY065V2_OUTPUT`. Use `CHAO_HEROFLY065V2_START=0` and `CHAO_HEROFLY065V2_LIMIT=97` for the complete reviewed plan; the older helper must remain disabled. Both helpers are excluded from the installable JAR. The current 97-case plan includes the largest Fly fan and Phoenix replacement wings in actual water animation, in addition to nine directions at fresh/intermediate/mature ages, independent training quarters, opposite Fly-to-Swim crossing, mixed age/training, color/shine, hats/animal parts/glasses, UI and Survival rejection, production toy grips, equipped carrying, natural first evolution and earlier-family regressions. Actual Accessories checks require `-PchaoAccessoriesTest=true -PchaoAccessoriesDir=<local Accessories/owo directory>`.

Available operator previews remain `/chao testtoy list`, `/chao testtoy broom once`, `/chao testtoy picture_book loop` and `/chao testtoy stop`. They share production clocks and cleanup and do not grant gameplay rewards. Available commands alone do not prove actual playback, listening or multiplayer behavior. See [the Hero Fly report](docs/HERO_FLY_02565.md) for completed results, visual comparisons, inherited failures and remaining verification limits.

## Complete Hero / Run-first 0.25.66

Use Java 21 and a private test world. `coreTest`, `heroRunMorphRigTest066` and `heroRunMaterialTest066` cover the signed Hero/Run policy and actual rigid-box/atlas implementation; `build` retains previous family and toy checks. The new native namespace `chao_hero_run66` must run alongside the earlier morphology/edit namespaces. Test both independent axis crossings, including later Power while first type remains Run, plus mixed targets, maturity, tie recency, ordinary/combined drives, reductions, commands, permissions, NBT and tracked-data transfer.

The opt-in development helper `CHAO_HERORUN066_SMOKE=true` uses `-PheroRun066Dir=<private directory>` and `CHAO_HERORUN066_OUTPUT` for actual captures. Review its complete authored plan and helper hash before running the final cohort. Keep static comparisons, runtime animation views and numerical tests distinct. Test largest wings and replacement wings in water, the retained normal/Run babies, colors/finish, face, halo, hats, animal parts, toy grips, equipment and the completed Hero/Fly family. Real optional Accessories requires `-PchaoAccessoriesTest=true -PchaoAccessoriesDir=<local Accessories/owo directory>`.

Historical055 adult geometry tests explicitly pin the old asset through the existing test loader after the production adult route advances to066; every old geometry/UV/attachment assertion is retained. Current baby selection must still use055, and new066 tests must independently verify the actual production adult route. No production asset override is supplied. Existing operator previews and cleanup remain available through `/chao testtoy list`, `/chao testtoy broom once`, `/chao testtoy picture_book loop`, `/chao testtoy stop` and `/chao testcarry loop|stop`. Actual client review, audio listening, physical touch and independent remote multiplayer each require separate evidence.


## Hero / Power-first 0.25.67

Use Java 21 and a private test world. Focused tasks are `coreTest`, `heroPowerMorphRigTest067` and `heroPowerMaterialTest067`; the full `build` retains completed family and shared toy/animation checks. Native namespace `chao_hero_power67` runs alongside all prior morphology/edit namespaces. Check signed Power→Run and Fly→Swim crossings, actual mixed hold/reversal, independent maturity, first-evolution ties, ordinary/combined/held drives, reductions, command/Creative permissions, NBT and tracked-data transfer.

The development-only client helper uses `CHAO_HEROPOWER067_SMOKE=true`, `-PheroPower067Dir=<private directory>` and `CHAO_HEROPOWER067_OUTPUT=<capture directory>`. Review its complete authored fixture definitions, regression first types and helper hash before the final whole run. Keep preflights separate from the final cohort. Compare all nine forms at fresh/intermediate/mature ages, independent training blends, biggest wings/replacement wings in water, five coats/finishes, eyes/halo, hats/animal parts, toy grips, equipped companions, retained babies and completed Hero/Run066. Real optional equipment checks use `-PchaoAccessoriesTest=true -PchaoAccessoriesDir=<Accessories/owo directory>`.

Existing `/chao testtoy list`, one-shot/loop/stop toy previews and `/chao testcarry loop|stop` retain production clocks and cleanup. They do not grant gameplay rewards. Actual visual review, audio listening, physical touch and independent remote multiplayer remain distinct checks. Report video metadata from actual ffprobe output, never duration multiplied by nominal frame rate. See [the Hero Power report](docs/HERO_POWER_02567.md) for completed results and limitations.


## Dark / Swim-first 0.25.68

Use Java 21 and private test worlds. Focused tasks are `coreTest`, `darkSwimMorphRigTest068` and `darkSwimMaterialTest068`; `build` retains completed family, shared animation, toy and attachment checks. New namespace `chao_dark_swim68` runs with all earlier morphology/edit namespaces. Verify both signed-axis crossings, including later Fly with first Swim preserved, compatible pairs, held/reversed blends, independent maturity, first-evolution ties, ordinary/combined/held drives, reductions, command/Creative permissions, NBT and tracked-data transfer.

Include `chao_morph54` in the enabled GameTest namespaces: NeoForge filters generated `TestFunction` entries by the structure template namespace, and these factories share `chao_morph54:activity_test`. Selecting only `chao_dark_swim68` executes no tests. Require the actual registered test count and `All … required tests passed` line as well as a successful process exit; a server startup failure can still leave Gradle reporting success.

The development-only client helper uses `CHAO_DARKSWIM068_SMOKE=true`, `-PdarkSwim068Dir=<private directory>` and `CHAO_DARKSWIM068_OUTPUT=<capture directory>`. Review the actual full authored plan, every regression first type and helper hash before the final run. Keep preflights and old captures separate. Compare fresh/intermediate/mature states, independent training quarters, both opposite crossings, source-appropriate Dark marker and wings, inherited colors/finishes, eyes, hats/animal parts, toy grips, equipped rendering, babies and completed Hero/Power067. Real optional equipment requires `-PchaoAccessoriesTest=true -PchaoAccessoriesDir=<Accessories/owo directory>`.

Operator `/chao testpose list` includes `crawl`, `paddle`, `swim`, `glide` and `flail`, plus the retained `sit` pose. Each supports `once`, `loop` and `stop`; `/chao testpose stop` ends the nearest active preview. These dry-ground testing adapters reuse production rig poses and the existing synced 160-tick session clock. They freeze care/age during playback, create no props or rewards, restore the original AI flag and stop on interaction, damage, unload or keeper disconnect. Native cases exercise both original AI states, exact state/items, real collision support, permissions, suggestions and save/reload; actual client scenes must separately prove once, loop wrap, stop and rendered poses from three angles.

Existing `/chao testtoy list`, toy one-shot/loop/stop previews and `/chao testcarry loop|stop` retain their production clocks and cleanup. Numerical tests, actual views, audio, touch and independent remote clients are distinct checks. Report video metadata from actual ffprobe output; never infer frame count from duration times nominal FPS. See [the Dark Swim report](docs/DARK_SWIM_02568.md) for completed outcomes and limitations.

## Dark / Fly-first 0.25.69

Focused tasks are `coreTest`, `darkFlyMorphRigTest069` and `darkFlyMaterialTest069`; `build` retains the completed family, shared animation, toy and attachment checks. Add native namespace `chao_dark_fly69` to the previous morphology/edit namespaces, including the shared generated-fixture namespace `chao_morph54`. Require an actual nonzero required-test count and final pass marker, not just Gradle exit status. Verify both signed-axis crossings, later Swim with first Fly preserved, mixed targets, independent maturity, first-evolution ties, ordinary/combined/held drives, reductions, command/Creative permissions, entity NBT and tracked-data transfer.

The rig task includes 116 proper Dark/Fly car/crayon profiles across the nine targets, three maturity levels, natural completion, cancellation and actual idle returns. It checks the late production pose order, connected closed geometry, unchanged non-wing poses, held palm targets and the existing 0.8-pixel half-tick movement limit.

The development-only capture helper uses `CHAO_DARKFLY069_SMOKE=true`, `-PdarkFly069Dir=<private directory>` and `CHAO_DARKFLY069_OUTPUT=<capture directory>`. Before the full cohort, review its complete source fixtures, regression first types, action schedules and source hashes. Keep preflight/failed attempts separate. Compare fresh/intermediate/mature and independent training stages, both opposing axes, colors/finishes, face/spiked marker, hats/animal parts, toy grips, equipped companions, retained babies and completed Dark/Swim068. The two floor scenes use the real 160-tick toy preview with a 180-tick observation window and verify active playback, fitted props, per-angle held views and exact cleanup. Real optional equipment uses `-PchaoAccessoriesTest=true -PchaoAccessoriesDir=<existing Accessories/owo directory>`.

Existing `/chao testpose <crawl|paddle|swim|glide|flail|sit> once|loop|stop`, `/chao testpose stop`, `/chao testtoy list`, toy once/loop/stop and `/chao testcarry loop|stop` reuse production clocks and cleanup. No new preview API is needed for this family. Keep actual visual inspection, numeric collision tests, audio listening, physical touch, independent remote multiplayer and full old-world restart checks distinct. Record actual video frame counts from the encoded streams. Read [the family report](docs/DARK_FLY_02569.md) for final outcomes, approximations and unverified aspects.


## Dark / Run-first 0.25.70

```sh
python3 scripts/validate_dark_run_morph_02570.py
./gradlew --no-configuration-cache coreTest darkRunMorphRigTest070 darkRunMaterialTest070 pianoRigTest organRigTest
./gradlew --no-configuration-cache build
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestDir=/path/to/private/test-world -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56,chao_heronormal57,chao_swim59,chao_darknormal60,chao_fly61,chao_run62,chao_power63,chao_hero_swim64,chao_hero_fly65,chao_hero_run66,chao_hero_power67,chao_dark_swim68,chao_dark_fly69,chao_dark_run70
```

The new tests cover the actual production emitter, all nine signed corners, independent ages, fixed head/face/joints, both opposite-axis crossings, direct ordinary and combined stat paths, current-bar edits, retained family regression, material/attachment behavior and the late car/crayon floor-pose hook. Keep the original geometric and continuity limits intact. Add 27 real Dark/Run age/axis configurations to each keyboard-contact test while retaining all prior fixtures. Existing operator previews use the production clock: `/chao testpose crawl once|loop|stop`, `/chao testtoy toy_car once|loop|stop`, `/chao testtoy crayons once|loop|stop` and `/chao testcarry loop|stop`. See the report for exact executed outcomes and bounded visual coverage; prepared commands are not claimed as completed results.


## Dark / Power-first 0.25.71 and retained Dark / Run

```sh
./gradlew --no-configuration-cache coreTest darkPowerMorphRigTest071 darkPowerMaterialTest071 darkRunMorphRigTest070 darkRunMaterialTest070 pianoRigTest organRigTest
./gradlew --no-configuration-cache build
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestDir=/path/to/private/test-world -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56,chao_heronormal57,chao_swim59,chao_darknormal60,chao_fly61,chao_run62,chao_power63,chao_hero_swim64,chao_hero_fly65,chao_hero_run66,chao_hero_power67,chao_dark_swim68,chao_dark_fly69,chao_dark_run70,chao_dark_power71
```

Run the dedicated GameTest server separately from the ordinary client. If exercising the existing optional Accessories integration, use the established test flags and your own existing optional jars; those libraries are not bundled. Restore ordinary runtime resources with a normal `prepareClientRun jar` before freezing the actual client runtime. The installable JAR must exclude native fixtures, capture helpers and nested optional jars.

Exercise young, half-developed and mature forms, fixed-age partial training, both opposite-axis crossings, all compatible mixed corners, gains/reductions/absolute Creative edits, first-evolution ties, immutable adult alignment/type, entity/tag reload and tracked updates. Capture matched viewer/client settings and multiple angles for Dark Power and retained Dark Run. Include representative coats, shiny finishes, eyes/marker, hats/animal parts, quiet toys, equipped-player carriage and production pose/floor previews. Numeric clearance checks do not replace scoped personal visual review.

Existing operator previews retain production clocks and cleanup: `/chao testpose crawl loop`, `/chao testpose swim once`, `/chao testpose stop`, `/chao testtoy broom once`, `/chao testtoy picture_book loop`, `/chao testtoy toy_car once`, `/chao testtoy crayons loop`, `/chao testtoy stop`, and `/chao testcarry loop|stop`. Test once/loop/stop through the native dispatcher and actual client where possible; preserve saved care, genetics, age and real items and report listening, phone/touch, independent multiplayer and old-world restart limitations explicitly. The separate inherited journal-format result is not a morphology pass.


## Headwear and accessories 0.25.79

```sh
python3 scripts/validate_headwear079.py
./gradlew --no-configuration-cache coreTest wearableRigTest079 pumpkinPortsUvTest079 faceGlyphRigTest076 companionCarryRigTest idleRigTest078 proneToyRigTest051 quietToyRigTest049 activeToyRigTest049 assemble
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestDir=/path/to/private/test-world -PchaoGameTestNamespaces=chao_wear79,chao_hats50,chao_garden
```

The wearable geometry tests exercise actual skin landmarks and emitted accessory meshes; the dedicated-server lifecycle cases cover all 25 canonical wearable items as babies and adults through consent, exact item components, entity/carrier reload, keeper authorization and removal. Use existing `/chao testidle <id> once|loop|stop`, `/chao testtoy <id> once|loop|stop`, `/chao testpose <pose> once|loop|stop` and `/chao testcarry loop|stop` previews for actual moving attachments. Keep emitted geometry, client screenshots, item models, sound listening and independent remote multiplayer checks distinct. See [the headwear report](docs/HEADWEAR_ACCESSORIES_02579.md) for the exact executed results and supported combination limits.
