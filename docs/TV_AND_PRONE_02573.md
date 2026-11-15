# TV watching and stomach-lying toy poses — 0.25.73

The TV now uses a bounded watching performance: the Chao reaches its screen-side seat, faces the actual screen center, settles into the existing sitting pose, makes two small attention movements and briefly smiles, then returns to idle. Car and crayon play lower the head slightly to reduce the disconnected-neck appearance. The original head geometry, completed morphology assets, colors, shared attachment rig and existing toy placement are unchanged.

## TV interaction

The earlier behind-TV pathfinding fix is retained. The cabinet remains non-pathfindable and fence-like to the planner, so a Chao routes around it instead of planning a jump or path through its collision box. Natural use still uses the ordinary `PlayGoal`, the real front contact position and the existing selection, cooldown and reward rules. No cabinet movement or approach teleport was added.

Watching uses the existing synchronized **160-tick session**: one second settling, a quiet watching interval, and one second recovering at normal 20 TPS. The shipped four-frame screen artwork advances once through a slow sequence, rather than continually cycling and chiming. There is at most **one quiet startup bit cue per session**, no repeated musical-note particles and no automatic happy shout. Explicit preview loop mode intentionally begins another session; ordinary watching is bounded.

The gaze target comes from the actual imported OBJ screen faces, including their off-center position and the block's rotation. Natural play and NoAI previews share the same bounded orientation calculation. This avoids relying on an inactive LookControl during operator playback. The existing outgoing pose blend handles interruption.

The current imported TV cabinet, trim, antenna and screen images are retained. The screen program uses the existing red-clothed character frames; no new cartoon or sound recording was imported.

## Car and crayons

Only the extra head-clearance arguments changed: car **0.80 → 0.40 model pixels**, crayons **0.60 → 0.15**. At full presence this lowers the head by approximately **0.40 pixels for the car and 0.45 for crayons**. The shared neck/collision/floor solver, head dimensions, hand grips, props, clocks and animations remain intact.

Removing all extra clearance was tested and rejected because it produced real hand/head overlaps. The retained margin passes the original strict positive-clearance tests. The new connection test measures the actual visible head/torso faces rather than using a crown or hat bounding box; its measured maximum neck gap is about **0.441 pixels**, with separate limits of 0.50 for car and 0.25 for crayons. The ordinary original head remains 7×6×6; it is moved, not resized or reshaped.

## Operator previews

These reuse production poses, session timing, cues and rendering. They require operator permission and select the nearest Chao within 12 blocks.

```text
/chao testtoy tv once
/chao testtoy tv loop
/chao testtoy tv stop
/chao testtoy toy_car once
/chao testtoy crayons once
/chao testtoy stop
/chao testtoy list
```

`toy_car` and `crayons` also accept `loop` and `stop`. TV preview requires an existing available TV with a clear, supported screen-side seat already within **0.4 blocks** of the Chao. A Chao behind the cabinet is rejected with feedback instead of being relocated through it; natural approach is tested separately.

Previews grant no rewards, preserve ordinary care/genetics/lessons/age/skills/items, restore temporary AI flags, the Chao's pre-preview orientation and prior cooldown, and release the TV reservation on completion or cancellation. Deliberate changes to the placed TV persist. Explicit stop, damage, petting, toy removal/rotation/obstruction, displacement, unload, logout, range changes and reload are covered. Real damage and petting retain their legitimate effects. Ordinary care resumes after playback ends; it is not frozen indefinitely.

## Actual automated results

| Check | Actual result |
| --- | --- |
| Core mechanics and coverage | Passed, including 4,336 TV timing checks and the existing 27,586-check guard for all 135 mature morphology corners |
| Active/quiet toy rig and timing suites | Passed, with original hand/head, prop, reach and floor limits retained |
| Prone support | Passed: 60,733,276 checks over 1,516 source form/age/morph/head cases and 1,005,244 sampled frames |
| New head connection check | Passed: 1,839,294 checks over 588 state/transition configurations, 191,670 production frames, fitted hats and head-child attachment matrices |
| New TV shared-rig check | Passed: 158,380,660 checks, 416 anatomy/stage/axis cases, 316,992 frames and 13,312 normal/cancellation exits |
| Shared sitting rig | Passed: 2,379,636 checks over 882 form/age/morph cases |
| Focused native TV rerun | **All 59 passed**, including 32 approaches: newborn/adult × four screen facings × all four starting sides; four additional rear-newborn alcoves; preview, interruption, permissions, invalid-seat and natural cancellation cases |
| NeoForge 1.21.1 assembly | Passed; normal prepared production classes/resources are byte-matched to the named 0.25.73 JAR |

The eight existing rear-newborn approach tests were preserved, rather than replaced by the new matrix. Test assertions are not a visual or listening pass. No main entity/save/network schema or morphology asset was changed; the normal synchronized toy state is reused.

## Reproducing the automated checks

From the complete project with Java 21, run the same public Gradle tasks:

```bash
./gradlew --no-configuration-cache coreTest activeToyRigTest049 quietToyRigTest049 proneToyRigTest051 tvWatchRigTest073 activeToyTimingTest049 quietToyTimingTest049 sittingRigTest assemble
./gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_tv73 -PchaoGameTestDir=run-tv73-tests
```

Use a fresh test directory. The actual focused native run also enabled the optional Accessories runtime. To repeat that classpath, add `-PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/accessories-jars` to the second command, using Accessories `1.1.0-beta.54+1.21.1` and owo-lib `0.12.15.6-beta.1+1.21`. These are optional test dependencies, not newly bundled mod requirements. Run the GameTest server separately from an ordinary client invocation; its isolated test dimension preset is excluded from the installable JAR.

## Broad-run failures and inherited issues

The first broad native run **did not pass**: **4,328 of 4,375 passed; 47 required cases failed**. Eighteen failures were new TV test-observation mistakes, corrected without changing TV production code: a baby's prospective type may change during normal walking; care resumes after preview tick 160; external world changes are cleaned up on the next authoritative tick. Strict adult identity, exact active-preview state, item/AI/reservation cleanup and cue checks remain. The subsequent focused 59-case TV run passed. The entire 4,375-case catalogue was not rerun after those observation corrections, so no full-suite green result is claimed.

The other **29 failures were reproduced with the frozen accepted 0.25.72 runtime**, with identical names and assertions:

- **20 stale cue expectations:** 16 nonzero expectations for intentionally silent dolls/book/crayons, and four car cases expecting the old sound ID/count rather than the current `toy.car` four-cue sequence.
- **8 old mounted-preview fixtures** that omit the now-required placed rocking horse or pogo toy.
- **1 real inherited companion bug:** a tired living Nether companion's zero-energy early return prevents the expected extinguish step. This is outside the TV/head-position scope and remains a focused follow-up.

These inherited issues were not silently repaired or removed from the evidence.

## Reference and actual client review

The [Chao Island SA2 race guide](https://chao-island.com/info-center/training-sa2/races.html) rendered in Chromium with TLS verification enabled, and its Television prize icon was inspected. The wiki Toys/Television pages remained blocked by HTTP 403 security verification, with no Continue option. **Original-game watching motion, timing and audio were not verified.** The watching choreography and cue choices here are authored Minecraft adaptations, not a claim of exact Sonic Adventure behavior.

A fresh five-scene client preflight completed successfully with **32 original PNGs**: adult Dark Run TV loop/stop, baby car, Neutral Swim/Fly crayons, Hero Power crayons and Dark Fly car. Earlier diagnostic attempts exposed missing audio-device and capture-observer issues. The first full-client attempt then stopped at case 12 because its observer incorrectly demanded a TV sitting pose after deliberate real damage had already disabled watching. The corrected observer uses the actual production eligibility predicates, retains strict uninterrupted-pose checks, and adds bounded server/client cancellation checks. A separate four-scene interruption preflight passed with 29 PNGs before the clean full rerun. No production changes were needed for these observer corrections; the failed attempts remain in the evidence.

The clean final run completed **all 28 scenes at normal 20 TPS**, with **193 original PNGs**, nine natural TV approaches and 44 observed toy sound channels after resolution/playback started. It covered all four starting sides for a newborn and shiny Dark Run adult, another rear approach for Hero Swim, TV once/loop/stop, actual damage and cabinet removal, and both prone toys on ordinary/shiny babies, rabbit parts, reciprocal Neutral Swim/Fly adults, Hero Power and colored Dark Fly adults. Adult identity, preview state, equipment, AI restoration, cleanup and NBT round trips passed the explicit checks. The zero-Run newborn's rear detour took 1,058 ticks (about 53 seconds), walking 4.796 blocks to the front seat; the existing slow crawl speed was retained.

The delivery includes `GALLERY.md` and `index.html`, 17 review sheets, the 193 unchanged captures, a scene-by-scene CSV and two silent 20-FPS recordings: the complete rear approach (62.10 seconds) and TV loop/stop, damage/removal and baby car/crayon play (49.65 seconds). Both recordings fully decoded without errors. Active, recovery and approach images were personally inspected, along with spaced frames from the recordings: the sampled head/torso connections, eyes, orb/halo, wings, rabbit parts, hat and grips showed no obvious new clipping or detached parts. This is sampled visual review, not an every-frame collision guarantee. `REPORT.md`, the verification logs, source inventory and delivery checksums preserve the exact scope and results.

The client uses a process-local OpenAL **null output backend**. Sound-event cadence can be tested, but no listening pass, loudness judgement or physical audio output is claimed. Actual sound comfort still needs a normal audio device. Phone/touch, shader packs, independent remote multiplayer/reconnect, every equipment combination, a complete old-world restart and every-frame collision clearance also remain unverified. Development-client classes/resources equal the built JAR; a separate fresh launcher installation of that archive is not yet verified.
