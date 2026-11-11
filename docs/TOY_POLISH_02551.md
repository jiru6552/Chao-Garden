# Toy polish — Chao Garden 0.25.51

This update builds on the delivered 0.25.50 project and revises the existing toys. It adds no duplicate items or new acquisition routes. Java 21, Minecraft 1.21.1, NeoForge 21.1.219 and external Lionfish API 3.1 remain required. Use matching Chao Garden versions on client and server.

## Changes

| Toy | Focused change |
| --- | --- |
| Broom | Both visible hands grip separate points on the real shaft throughout sweeping. The broom is closer, arm extension is capped at 1.2 model pixels rather than the previous 3.2, and a small toe splay clears long feet. Brush contact remains on the floor; its existing sweep sound remains. |
| Shadow doll | Correct the official face texture's mirrored repetition and the four nose UV samples. Features are upright and symmetric, and the nose samples opaque black instead of a red pupil. Source geometry and grip landmarks are unchanged. |
| Picture-book, crayon-box and broom icons | Separate GUI views face toward the viewer and show the cover, box and brush clearly. Their non-GUI base model definitions remain exact copies of 0.25.50, preserving placed and held appearance. |
| Crayons | The Chao lowers onto its stomach and draws on a supported floor sheet, with the box alongside. One hand steadies the sheet and the other grips the real crayon barrel; its tip follows the strokes. Arm extension is capped at 1.8 pixels. Drawing marks remain transient presentation, without changing blocks or saved world art. No interaction sound. |
| Picture book | Raise the book and both hands, adapting the clearance to actual posed feet, including genetic and glass feet. Covers and page turns retain their original source geometry and production clock. No interaction sound. |
| Sonic and Shadow dolls | Raise both hands and the held doll above the visible feet, including entry and exit. Retain the existing soothing/lifting motion and real two-hand grips. No interaction sound. |
| Toy car | Resting car and collision bounds shrink to 70% on each axis. The Chao's presentation uses the matching reduction, lies prone and uses its right hand to move the car left/right along the floor; the other actual palm supports the body. The car faces along its movement, keeps its wheels grounded and remains full size while the Chao prepares/exits. Contact is handed over as the hand reaches it. Arm extension is capped at 2 pixels instead of 3.2. The original four rolling cues remain. |

The closed-hat, complete-DNA, Half-fish stock and gem-egg changes from 0.25.50 are retained. Other instrument animations, rocking horse, pogo stick, companion movement, garden and lobby behavior are outside this update.

## Face and inventory references

[Asset details](TOY_ASSETS_02551.md) and [quiet-toy fit notes](QUIET_TOY_POLISH_02551.md) record the exact supplied SA2 Battle `sh_doll01` source texture and hashes. The Sonic donor mesh uses a 64-pixel mirrored face atlas; the old Shadow mapping stretched one 32-pixel texture across that entire atlas. The corrected atlas reconstructs the intended mirrored repeat. Four nose triangles also needed their constant UV moved from Shadow's red pupil to an existing opaque black texel.

Shadow still uses the previously disclosed Sonic-donor shape with its Shadow texture/palette adaptation. This update does not claim a newly extracted independent Shadow mesh. The primary texture and labeled before/after software UV reference are supplied with the delivery. They are explicitly software references, separate from actual Minecraft captures.

GUI presentation is selected only for the GUI display context. The three original base definitions and 63 protected quiet/active held, floor and material resources are checked by hash. Shadow face UV/material changes and the smaller resting car are the intentional exceptions. Car hand vertices remain unchanged; its runtime presentation supplies the smaller scale.

## Grounding, transitions and interruption

Car and crayon play share the same measured prone helper. It places the belly just above the floor, reconnects the skull using the actual source hierarchy, raises the face, and turns the visible feet behind the body. Tails/Knuckles use their body-parented skulls correctly; other supplied heads use their root parent. Actual genetic/glass soles and reached hand volumes are used rather than assuming a palm center or a generic foot box. Closed vanilla hat padding is included where applicable.

The real presence clock lowers and raises the body. A renderer-only, per-UUID outgoing transition correction prevents hands/feet passing through the floor during the existing eight-tick blend after an abrupt interruption. It runs before a new activity fits its own hands and props, clears on the normal stale/offscreen/age-reset guards, and skips air/water/companion carrying. It checks the actual collision top immediately below the feet rather than the transient client `onGround` flag; an actual Power-Chao car-removal session exposed that flag being false while stationary on smooth stone. It changes no entity position, hitbox, care data or persistent flags.

The server checks sufficient clear, supported space for these two floor activities, including preparation, active sessions and source-free previews. This uses a padded envelope derived from the measured source head/body/foot union plus prop/hand room, without changing entity hitboxes. A wall, missing floor, liquid or another living actor in that space prevents play; a newly blocked space cancels it cleanly. Only the matching reserved source's collision can be ignored. Other toys keep their prior space policies.

The car is also classified consistently as a path obstacle, including Minecraft's cache query without a supplied mob. This makes a front-side approach route around the occupied cell instead of climbing onto its roof. Actual collision and player interaction remain the smaller measured shape.

Existing toy IDs, placed states, recipes, stock, ownership, reservations, rewards and cooldowns remain. These visual updates require no save conversion. Existing placed cars use the smaller mesh/collision when loaded normally. Previews use production poses and clocks, grant no rewards or lessons, preserve exact state and real items, and restore temporary AI flags. Damage, care, removal, movement, unloading, logout and stop retain cleanup.

## Sound policy

Picture-book, crayon, Sonic-doll and Shadow-doll sessions are silent in both natural play and previews. Both scheduling and the shared dispatch function enforce this; even a stale/direct cue call cannot emit their old interaction events. Old sound registrations/files remain as compatibility resources, without an active interaction call site. Broom, car and pogo timing/volume are retained. Ordinary ambient Chao behavior is not globally muted.

## Operator previews

Use a nearby ordinary Chao on clear supported ground, with operator permission level 2:

```text
/chao testtoy list
/chao testtoy broom once
/chao testtoy picture_book once
/chao testtoy sonic_doll once
/chao testtoy shadow_doll loop
/chao testtoy crayons once
/chao testtoy toy_car loop
/chao testtoy stop
```

Every named toy accepts `once`, `loop` and `stop`. Car/crayon previews require room for the whole floor pose. Existing mounted and instrument previews are unchanged.

## Verification

The core suite and 20 focused asset checks passed. The final dedicated-server run passed **all 357 required tests**, including the new space fixtures and car path-cache assertions. This is 238 polish/space/preview/sound/route checks plus 119 prior hat, DNA, trader, item, egg and real-Lootr regressions. Real Accessories/owo and Lootr were installed in the dedicated run. Coverage includes both ages/four source orientations, native once/loop/stop commands, no-reward state preservation, actual unload/reload cleanup, forced silence, retained cue times, wall/ledge refusal and active cancellation, and matching-owned-source handling. Sixteen added car-navigation cases cover Run-0 babies/adults, front/rear starts and all four orientations, requiring grounded real navigation, rear contact and exactly one completion reward.

**All 49 distinct actual-client scenarios passed**, using the production commands and normal 20-TPS playback with real Accessories/owo installed. Each toy covers natural baby/adult approach, rabbit replacement hands, Chaos loop/stop, Power source removal, Dragon damage, and complete Run/Power variant phrases. The final case switches directly from prone car play to broom play. Per-frame checks cover real prop/palm contact, book/doll foot clearance, crayon tip/paper placement, prone floor contact, transitions and cleanup; emitted sound events confirm silence for book/crayons/dolls and retained broom/car cues. Five additional unspawned-clone probes verify that the outgoing correction uses real floor support even when the client ground flag is false, and rejects unsupported air and an embedded actor.

Representative front, side and rear frames were visually inspected for babies, adults, replacement hands and the long-footed forms. Book/doll shoes clear the feet; broom hands meet separate shaft grips; prone crayons show supported forearms, paper and box placement. An additional zero-pitch, near-floor car camera confirms wheel contact; the higher side camera had made its greater distance look like a gap. Shadow close-ups show an upright symmetric face and black nose, compared with the supplied primary texture. Actual normal inventory slots and a sharp enlarged GUI view show the three recognizable upward-facing icons. An extra adult car run passed with a zero-pitch floor camera and these final GUI views. These checks are representative frame reviews, not a claim of watching every rendered frame or listening to the game.

Earlier runs exposed and resolved the slow-baby car roof route and Power car-removal floor blend described above. Two helper-only issues were also corrected: waiting for matching source packets before arming a case, and using the exact old car bounds times 0.70 for the baked-model guard. Strict contact/floor assertions were preserved. Only frames from cases with both native and render completion records are accepted for delivery; failed/incomplete case captures are excluded. Final focused rig results are recorded below.

Final focused Gradle results passed: **59,346 quiet-toy poses / 96,084 emitted hands**, **101,736 active-toy poses / 131,880 emitted hands**, and **31,804,748 prone-support assertions** over 792 source form/age/morph/head cases, 526,600 quarter/contact frames, 1,352 actual replacement-foot cases and 2,816 outgoing transition frames. Both timing suites passed 641 quarter-tick samples. There were zero failures in these checks. The maximum prone quarter-step was 0.498 model pixels; outgoing recovery was 0.551 pixels, with a maximum temporary root correction of 3.558 pixels. These mathematical checks complement the representative client observations; they do not establish audio quality.

The final `build --continue` produced the production JAR, but the full gate is **not green**: only the two previously documented `triangleTransitionRigTest` and `trumpetTransitionRigTest` tasks failed. Their 267,696-sample result files are byte-for-byte identical to the delivered 0.25.50 baselines: triangle 25 failures / minimum -0.54432297 pixels, trumpet 37 / -0.58569264. SHA-256 values are recorded in the delivered comparison JSON. Those unrelated tests and animations were not relaxed or changed. The other completed build checks passed, including the 264 visible-transition checks and the existing accordion, carry, drum, flute, guitar, organ, piano, posture, replacement-hand instruments, rocking horse, running gait and sitting checks. Compilation, JAR integrity and all focused 0.25.51 checks passed.

The delivery includes original native PNG frames, per-toy archives, compact navigation galleries, console excerpts, source changes, archive integrity checks and checksums. Software UV/reference renders are separately labeled.

## Remaining verification

No new sounds were added; audible broom/car/pogo samples are unchanged. This environment has no audio hardware, so listening is not claimed. Independent remote-player views, network latency, full modpacks, shader/resource-pack combinations and a real user world still require in-game checking. Native server tests use actual server APIs and simulated players; they are not a physical multi-client multiplayer session.

The source ZIP is a cumulative overlay on the full 0.25.43 project, including previous updates and explicit source-file deletions. Follow its deletion list/helper or use the complete delivery branch. The helper edits source files only and never scans Minecraft saves.
