Current focused update: [0.25.62 Neutral/Run-first verification](docs/NEUTRAL_RUN_02562.md). Use Java 21 and isolated test worlds.

```sh
python3 scripts/validate_neutral_run_morph_02562.py
./gradlew --no-configuration-cache --continue build
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56,chao_heronormal57,chao_swim59,chao_darknormal60,chao_fly61,chao_run62 -PchaoGameTestDir=/path/to/private/test-world
```

Read the family report for actual outcomes and bounded visual coverage. The complete build includes new core, production-emitter rig/material and retained checks; it is one invocation when it also supplies focused results.

Current focused update: [0.25.59 Neutral/Swim-first verification](docs/NEUTRAL_SWIM_02559.md). Use Java 21 and a private test world:

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
