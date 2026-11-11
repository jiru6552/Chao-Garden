# Piano and accordion — Chao Garden 0.25.48

This update refines the existing piano and accordion in the full project, retaining Java 21, Minecraft 1.21.1, NeoForge 21.1.219 and external Lionfish API 3.1. Use matching Chao Garden client/server versions. No new duplicate toy or save conversion is required.

## Piano

The previous contact fit could extend arms by over seven model pixels. The piano now has its own measured stance: 0.500 blocks from the block center for babies and 0.555 for adults, facing the authored keyboard front. Hands meet the front portion of the actual keys and follow their depression; arm extension is capped at two model pixels. A bounded shoulder adjustment accommodates short replacement hands.

Chao collision leaves the measured space below the keyboard available while retaining the rear cabinet, full player collision and interaction outline. The final approach uses the same slow collision-checked handoff as the organ, preventing fast Chao from climbing onto the piano. Blocked fronts reject play and previews. The existing sixteen alternating harp notes, block key timing, reservation, reward and cooldown remain intact.

## Accordion

The existing source accordion is fitted to the Chao's torso with the textured keyboard face outward. Its three baked parts retain the original triangles, UVs and materials: rigid keyboard end, flexible bellows and rigid support end. The right hand meets the actual vertical keybed and the left supports and pumps the real outer side of the opposite end. The played register and support height leave room for larger heads and replacement hands. The rigid ends move as the bellows expand; the keys and cabinet are not stretched.

One 160-tick production phrase drives Lionfish pose, bellows and eight original reed cues at ticks 24, 40, 56, 72, 88, 104, 120 and 136. Placed play, learned performances, classroom practice and previews share that implementation. The reserved placed source is hidden during hand presentation and restored afterward; no inventory copy is created. Ordinary lesson gates, rewards, cooldowns and care behavior remain in place.

## Preview commands

With operator permission, stand near an idle, dry Chao:

```text
/chao testtoy list
/chao testtoy piano once
/chao testtoy piano loop
/chao testtoy piano stop
/chao testtoy accordion once
/chao testtoy accordion loop
/chao testtoy accordion stop
/chao testtoy stop
```

Piano previews reserve a real piano within three blocks with a clear supported front. Accordion previews use a temporary presentation without requiring an inventory toy or lesson. Previews grant no rewards and preserve saved care, genetics, age, lessons, real items, cooldown and temporary AI flags. Damage, interaction, distance, unload, disconnect and explicit stop clean up through the existing preview lifecycle.

## Verification

The full `build` reached the existing `triangleTransitionRigTest` and failed on 25 hand/head overlap assertions in 267,696 samples. This is an unchanged baseline failure: the exact parent-0.25.47 test and helper sources were compiled separately and run against the delivered 0.25.47 JAR, with all current production class directories removed. It produced the byte-identical failure report (SHA-256 `99bce5827d20d9d3aba51fef07728eb609dda4bc14e2c0534e903aacba50b6f8`). JVM class-load records and class hashes confirm the baseline dependencies. The triangle is outside this focused update and remains unchanged. The installable JAR assembled successfully and passed integrity checks; the full build is **not green**. Baseline evidence is included with the delivery.

- Core mechanics suite and standalone interaction/geometry checks passed (3,092 interaction assertions plus the existing geometry checks).
- Piano: 152,604 hand samples, 14,094 actual source-mesh body/skull/foot clearance checks and 1,282 real Lionfish timing samples passed. Arm extension stays at or below two model pixels. Maximum measured key-height/depth error is 0.00530 / 0.01050 blocks; minimum actor-surface clearance is 0.19824 model pixels.
- Native dedicated GameTest server with the real Accessories/owo runtime: all 71 new instrument checks passed, plus all 106 existing piano, organ, guitar, Analyzer/garden and focused regression checks. Tests cover two ages and four facings, maximum Run approach, blocked keys, collision, lesson gates, natural rewards/cooldowns, actual dispatcher once/loop/stop, idempotent cues, saved-body reload, pause/resume, classroom rewards and interruption.
- The first native run exposed three incorrect test assumptions. The corrected tests require movement/removal cleanup on the next simulation tick, and preserve the remaining classroom performance when a lesson pauses. They still check silence during pause, cleanup, unique cues and one reward per pupil. Production preview and lesson behavior did not change.
- Accordion: 38,622 poses and 64,056 actual emitted-hand checks passed. Maximum extension is 2.7283 pixels on the right and 2.4884 on the left, with a production cap of 2.8. Emitted hands clear head/torso bounds by at least 0.0845 / 0.3187 pixels; the prop clears head/torso/floor by at least 0.3791 / 1.2419 / 1.9929 pixels. The production phrase passed 641 quarter-tick timing samples.
- Original accordion reed OGG decodes; six source articulation, rigid shape, outward face, seam, registration and audio-envelope checks passed, including independent containment of the played register and support point in the actual source triangles.

The final native client study completed all 12 sessions across two invocations: four piano age/facing/form cases, four accordion cases, damage and toy-removal interruptions, and two uninterrupted normal-speed phrases. The study produced 429 unmodified screenshots and checked actual world/Chao rendering before capture. Live hand-contact bounds, source restoration, preview state, exit and hurt priority checks passed. The normal-speed phrases took approximately 8.98 / 9.00 seconds through their 180-tick observation windows and delivered exactly 16 harp / 8 accordion client sound events. Multiple contact, entry, exit and bellows frames were visually inspected, including both piano sides and baby/adult/rabbit/Chaos accordion hands.

After the first five successful final cases, a preview-only capture-helper error was corrected: an accordion preview without a placed source needed a free-standing anchor instead of the block-contact helper. The remaining seven cases were then resumed successfully. The helper is excluded from the mod JAR; no production interaction change was required. The evidence excerpts retain both invocations.

The actual client uses the current renderer and real Accessories 1.1.0-beta.54 / owo 0.12.15.6-beta.1 runtime with shaders disabled. A native frozen/stepped clock is used only to retain exact pose and transition views; it does not establish normal-speed motion or audio acceptance. Two additional uninterrupted phrases verify entry, play and exit at normal speed with actual client sound events. Native capture assertions measure live piano palms against the actual keyboard coordinates and arm bound, and live accordion palms against the animated rigid end sections. They check the final blended model, rather than merely checking that an interaction is active. Cue-peak frames include both alternating hands.

The cloud uses Mesa software rendering and a silent OpenAL backend. OGG decoding, sound dispatch and cue timing are verified; tone, volume and listening are not. Independent multiplayer, network latency, a full modpack and physical-GPU performance were not tested in this update. Existing world structures/saves require no migration; native NBT reload checks preserve care, genetics, age and lessons. The previous lobby, equipped-flight, carry, gait, organ and guitar runtime entries are checked against the 0.25.47 JAR during packaging.

## In-game checks

Check piano hand contact on babies, adults and replacement hands in all four orientations. View the accordion from the front and both sides while the bellows open and close, then damage/stop/move the Chao and remove its placed toy. Listen for natural harp/reed tone, volume and synchronization, including several nearby performers. Independent multiplayer and full-modpack checks remain manual.

## Repeatable checks

Use Java 21 and the actual Gradle settings, rather than the outdated README setup:

```sh
bash gradlew --no-configuration-cache build
python3 scripts/test_accordion_assets_02548.py
bash scripts/test_toy_interactions_02518.sh
bash gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_instruments48 -PchaoGameTestDir=run-instruments048-native
bash gradlew --no-configuration-cache runClient -Pinstrument048Study=true -Pinstrument048Dir=run-instruments048-client
```

Run native server and client Gradle invocations separately. The opt-in client study creates a disposable flat world and exits after its real sessions; its fixtures and captures are excluded from the installable JAR. The default study has 12 cases; `-Pinstrument048Limit=1` selects the first case for diagnosis, and `-Pinstrument048Start=5` resumes from case 5. Real Accessories/owo can be added as local runtime test dependencies using the existing `chaoAccessoriesTest` and `chaoAccessoriesDir` properties. No accessory or shader dependency is bundled into this update.
