# Final .61 live-motion and numeric audit

Read-only audit of the completed final client cohort, recorded in `/workspace/.setup/fly061-client-final.log` and `/workspace/.setup/fly061-client-final/screenshots`. No source, production resource, Gradle, running client, or original image was changed.

## Literal provenance and completed results

- Final JAR SHA-256: `2618d3ed5326621d782e3a396fe68edb25b03c8f657206407641c4d840c02060`.
- Final model SHA-256: `2524c85c7c121531233b03eee36cdb3df6c8a39d568fbe9b0faea09ba887a885`.
- All **8,325** declared frozen production entries match the JAR and current declared development output bytes; zero mismatches. Runtime mode is **development classpath**, with a byte-for-byte JAR comparison, not direct JAR loading.
- All **590 original PNG files** match their literal logged SHA-256 values: 574 world images and 16 UI images. Zero mismatches. The final completion record says **67 requested / 67 rendered / 0 skipped**, captures590 / saved590.
- Cases 52–62 have all eleven native and render completions, with **247 world PNGs**. Native clocks range from **20.008 to 20.103 measured ticks/s** using literal armed/completion wall times; the small offset above20 is consistent with tick-boundary logging. Playback was not accelerated.
- Full build log literally reports **BUILD SUCCESSFUL in 12m 54s**, including 6,899 new core checks, 56,961,691 new rig checks and 288,539,007 material checks. Native log reports **428/428 required passed**, 18.67s game-test execution and 49s complete Gradle invocation. There was no separate focused build invocation.

## Motion and state findings

| Case | Actual activity | Native ticks / seconds | Walked blocks | Native final axes(RP,SF) |
|---|---|---:|---:|---|
|52|balanced walking|240 /11.957|8.3575|(0,0)|
|53|Run running|240 /11.972|18.6424|(-1,0)|
|54|stationary in-water SWIM animation|240 /11.981|0|(0,1)|
|55|broom once|180 /8.954|0|(-1,1)|
|56|picture book once|180 /8.989|0|(1,1)|
|57|actual Accessories upright flying carry|240 /11.962|0|(-1,1)|
|58|Run gain while walking|280 /13.978|10.5015|(-1,0)|
|59|Power+Fly gain while walking|280 /13.964|9.7693|(1,1)|
|60|Fly loss while walking|280 /13.966|9.7693|(0,0)|
|61|Run+Fly gain and reversal while walking|520 /25.989|19.3072|(0,0)|
|62|natural first evolution|300 /14.976|0|(0,.5), freshly evolved|

For every adult case 52–61, initial/final encoded strength remains exactly 1.2 and all logged display samples have maturity 1.0. The saved first identity stays locked, Neutral alignment 0, first type 2 (FLY), with identical lineage halves. Current level/progress bars independently reproduce final raw morphology axes. For live training cases 58–61, **chronological age advances by exactly 280/280/280/520 ticks**; biological visual maturity stays fixed. Toy previews retain their production care-preservation behavior. Accelerated gain/edit schedules and correction of incidental mobility/water training are explicitly fixture controls, not claims about natural growth pacing.

Case 62 starts as an ordinary near-threshold child fixture at age 215,988 (12 active ticks before the ordinary first-evolution threshold), visibly enters a cocoon, then ends adult with locked Neutral/Fly first identity, no cocoon, inherited Fly bars 480 and tiny fresh visual maturity≈0.00008487654. The final displayed trace is even younger because of appearance easing. This establishes the actual first-evolution transition, not full natural-lifetime pacing.

Broom and book native cleanup completed. Their render records report 382 and 380 actual grip samples, with maximum errors 0.0000002556103 and 0.0000003625609 in the production solver coordinates respectively; source checks require less than 0.025. These are the native production solver metrics, not invented image measurements. Carry uses actual Accessories and was visually attached from the side; the player blocks much of the front view.

## Live interpolation and cancellation limits

Raw server gains reach the intended endpoints, and loss/reversal return both axes to balanced while retaining locked Fly identity and mature biological age. The production client records clearly show gradual shape/paint response rather than an instantaneous target switch.

The raw compatible-pair peak is logged at tick 240 as (-1,+1), firstType 2. At that same time, the **displayed** sample is only about (-0.70996,+0.70996); the logged display at tick 300 is (-0.80152,+0.80152) while the server is already receding. The live pair sequence therefore **does not hold a fully settled visible full pair endpoint**. The static gallery provides that endpoint separately.

The single gains/loss and pair cancellation retain 40 ticks after their input schedules stop. Late originals visually show the gained Run / Power+Fly shapes and the receded balanced Fly-first form. There is no extra dedicated rendered-axis trace at ticks 280/520: the last 60-tick traces are 240/480. Final PNGs and successful render completion are the evidence for the late visible state; do not relabel the raw native-completion sample as a logged settled display sample.

## Actual visual review performed

Personally inspected **29 distinct unchanged originals** through two derived uniform contact sheets (fixed 640×600 crop, uniform 400×375 scaling, labels only). The exact source paths and hashes are saved in `motion52-57-sheet-sources.json` and `motion58-62-sheet-sources.json`. Reopened **six** of those originals at full 1440×900 resolution: walking52-left107, broom55-front35, book56-left107, carry57-left107, pair61-back517, and post-evolution62-back287.

Observed walking/running limbs, fixed square skull, shoulder/head continuity and wing fans remain coherent in the reviewed frames. Broad root/distal bands are visible on the wing faces. The broom grip stays on its shaft; the book stays with both hands, including the inspected side pose. The reviewed carry side shows the Chao attached to the player. The live gain/loss sheet visibly changes crest direction, wing span, palette and tail; the late cancellation view resembles the balanced mature form. The evolution sheet shows the cyan/yellow child, cocoon and short-eared fresh purple Fly-first adult.

This is sampled-frame review, not review of every moving frame or full motion-video playback/listening. Case 54 is **stationary water animation**, not autonomous swimming travel. No gliding travel was demonstrated. Carry only covers upright Creative flight, with player occlusion. Distinct hurt/pet/attack reaction faces are **not** established by this audit or saved `setEmotion` inputs; the separate 12 production-emitter frames are numerical visibility evidence only. No independent remote-player/latency/reconnect, shader/resource-pack or broad modpack claim follows from these captures.

Machine-readable evidence: `final-production-hash-audit.json`, `final-live-numeric-audit.json`, `final-capture-hashes.json`. The audit script is `audit_final_live.py`.
