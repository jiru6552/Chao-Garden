# Chao ball toy — 0.25.74

The compact cyan/yellow stepped ball shares one production controller for
natural play and previews: approach, close contact, roll and pursuit. The original `chao_ball` block/item ID, acquisition and
break loot remain. The updated JAR, cumulative source changes and visual
verification bundle accompany this report. The broader server suite retains
its documented inherited failures.

## Model and reference

The grounded model uses **23 closed, disjoint cuboids** forming a connected
solid, **six model pixels (0.375 blocks)** across. Cyan/yellow panels meet pale
caps. Bounds are `[5,0,5]` to `[11,6,11]`. Item geometry/UVs match, shifted five
pixels upward with GUI/hand/ground transforms. Independent bobbing is removed.

The [Chao Island SA2 race guide](https://chao-island.com/info-center/training-sa2/races.html)
rendered in Chromium with TLS verification enabled. Its original **75×75 Chao
Garden Ball prize icon** was inspected for the cyan/yellow palette and pale
cap. Wiki Ball/Toys pages remained behind HTTP 403 verification. Exact Sonic
Adventure 1 geometry, relative size, motion, timing, expressions and audio were
not verified. Sizing and choreography are authored Minecraft adaptations.

Six asset tests passed: geometry/connectivity, bounds, item origin, texture/UVs,
model bindings and reproducible ball-only generation.

## Play, contact and compatibility

Natural sessions retain their existing **160-tick** duration, selection,
cooldown and reward rules. Each contact action lasts **20 ticks**, touching at
**tick 10**. A successful push commits the **one real ball block** to an adjacent
clear supported cell and synchronizes its target. A six-tick cosmetic roll,
plus four settling ticks, bridges the static positions. Actual cuboid vertices
supply rotational floor support. This remains level-cell play, not free ball
physics or slope rolling.

Approach and chase use navigation and movement controls. The controller checks
the contact anchor, swept obstacles/living actors, loaded space and sturdy
floor; blocked actions do not move the ball or emit successful-roll cues.
Babies and crawling adults give a small rigid head nudge; walking adults tip a
rigid toe. Trained babies retain their normal chase gait between actions. The
original head box, limb lengths and scales remain unchanged.

Only the owner's claimed ball grants clearance through its oversized collision
box. Same-ball clearance lasts until the Chao physically leaves that overlap,
including after cancellation/reload. Other actors and blocks remain solid.
The ball adds default `rolling=false` and a ball-only cosmetic block entity;
existing facing/frame/playing/waterlogged states remain. Disk reload retains
the committed cell, while live packet/update tags carry the roll. Legacy idle
states remain visible, including old blocks initially lacking the new entity.
School pupils retain their managed real-toy sessions.

Each successful physical transfer emits one vanilla slime-step cue at
**volume 0.08, pitch 1.0**. The old periodic ball jingle and session-start happy
shout are removed. Event/channel checks do not establish perceived loudness.

## Operator previews

```text
/chao testtoy ball once
/chao testtoy ball loop
/chao testtoy ball stop
/chao testtoy stop
/chao testtoy list
```

Operator permission 2 is required. These select the nearest eligible Chao within
12 blocks and need an available placed ball within three blocks, a reachable
side and dry supported floor. The 160-tick one-shot includes approach, so a
far untrained crawler can finish before contact. Bring its ball closer or use
a loop while staying within command range; the existing slow gait is preserved.
Loops preserve an action crossing their boundary. List/suggestions include ball.

Preview pauses care, age, genetics/lessons/training changes, grants no rewards,
creates no item and restores temporary AI/goal/cooldown state. Explicit stop,
interaction, damage, unload/disconnect and reload cleanup are tested. Legitimate
damage remains. **Moved Chao and ball positions remain after stop**; preview
does not rewind the world. A committed cosmetic roll may finish after cleanup.

## Verification and remaining scope

| Check | Actual result |
| --- | --- |
| Focused ball server | **44/44 passed**, closed `native-ball04`, 9.045s test clock |
| Broad dedicated server | **4,390/4,419 passed**, same 29 inherited failures; no new/changed failure |
| Ball rig | **5,567,749 checks passed**, 2,166 contact profiles, 84 repeated/loop/cancel episodes, 68,628 quarter frames |
| Animal-foot fallback | 90 profiles/1,890 frames passed; base attachment envelope only |
| Retained checks/build | Core, active/quiet toy rigs/timing, prone crayons/car, TV, sitting and `assemble` passed; closed 214.24s run |
| Preservation audit | 7,873 protected files and all 288 prior 0.25.73 delivery files unchanged |
| Actual full client cohort | **25/25 scenes passed**, closed exit 0 and strict proof; **192 original PNGs**, 13 natural routes |

Native tests exercise real dispatcher/server paths: four-sided baby/adult
routes, contact/facing/chase, blocking, once/loop preservation, permissions/
selection/completions, interruptions, school ownership, legacy state and
packet/disk roundtrips. Remote reconnect and old-world restart remain untested.

Rig bounds retained fixed head geometry, rigid limbs, floor clearance and
head-child transforms. Maximum standard-foot contact SAT gap was **0.00002006
blocks**, with maximum rigid head shift **0.30121 model pixels**. Hidden animal
feet retain their parent gesture using the ordinary attachment envelope;
different replacement-mesh lengths can leave a gap, so exact contact for every
animal part is approximate. The actual adult rabbit contact was inspected
without a noticeable remote gap; that does not generalize to all replacements.

Two preview bugs were fixed without loosening tests: transient ground flags
canceled supported Chao, and a stale movement target turned them away during
contact handoff. Dry-support verification and corrected target/facing handling
resolve these. Initial compile errors were corrected; failed diagnostics remain
separate from passing reruns.

The **29 inherited failures** exactly match prior names and assertion reasons:
16 stale quiet-toy cue expectations, four stale car sound/count assertions,
eight mounted-preview fixtures lacking placed toys, and the tired Nether
companion extinguish bug. They were previously reproduced against the frozen
older runtime. Current broad execution correctly exited 1. All 175 retained
native test sources remain unchanged; corrected TV 59 and new ball 44 passed.

The final client ran production AI, poses, movement and native commands at
normal 20 TPS. Its 25 completed scenes cover baby/adult approaches from four
sides, representative Neutral/Hero/Dark morphs, shiny/colored Chao, a fitted
pumpkin, rabbit parts, once/loop/stop, damage/removal/displacement, recovery,
TV/car/crayon regressions and actual placed/dropped/held/GUI ball rendering.
The 192 source-bound original PNGs are accompanied by **18 review sheets**.
There were **55 physical ball transfers and 55 corresponding ball cue channels**,
plus one TV and four car channels: **60 total**. Client contact, intermediate
roll/floor and idle-recovery checks passed; these do not replace personal review.

All **18 review sheets**, representative item/contact originals and **52 decoded
motion frames** were personally inspected. The deliberate six-block displacement
left that case's actor outside its recovery/idle photos: cleanup assertions
passed, but that particular idle pose was not visually verified. Other stop,
damage and removal idle views were inspected. Three silent, original-speed
20-FPS clips fully decoded:
**49.1s** adult/baby natural play, **20.6s** loop/stop and damage, and **11.6s**
rabbit-adult play. The first two contain completed scenes from later-failed
diagnostic runs, not full-cohort acceptance. Earlier capture fixtures needed
operator-range and item-camera/UI corrections. A rendering hitch also missed a
strict photo window; the final unchanged-runtime rerun bounded Mesa workers to
the allocated CPUs and completed. Diagnostic failures remain labeled separately.

The ordinary prepared runtime equals the NeoForge 1.21.1 JAR: **31,586,495 bytes**,
**9,149 entries**, 642 production classes and 8,254 resources. SHA256:
`1b0424ff0f5adbbc246be8ef038b4e47dfcc744d3f7d6991971f64e8c10af45b`.
Capture helpers, fixtures and isolated-world overrides are excluded.
The source ZIP overlays complete 0.25.43 source
(`fcde2f9d012499b2e2cb1ec1d50a7f58d4dc3ca2`), retaining completed 0.25.73
changes. Follow its README/guarded source-deletion instructions and use fresh
build output. The gallery includes originals, sheets, recordings and receipts;
worlds and caches are excluded.

No physical sound listening, phone/touch, independent remote multiplayer,
complete old-world restart, every equipment/shader combination or every-frame
collision review has been established. OpenAL used a silent backend; actual
cue-channel events and silent motion recordings are not a listening pass.
