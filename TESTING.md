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
