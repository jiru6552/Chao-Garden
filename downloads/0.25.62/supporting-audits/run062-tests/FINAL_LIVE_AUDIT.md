# Final live audit — Neutral / Run 0.25.62

PASS within the stated scope. This audit changed only private evidence files. It reran no game, Gradle task or tests and changed no production, test or Git files.

The actual final cohort completed **92/92 cases**, zero skips, 789 saved original PNGs and `BUILD SUCCESSFUL in 10m 21s`. Every literal capture hash was independently checked: 773 world PNGs (462 static, 9 post-UI world, 302 motion/evolution) plus 16 UI PNGs, with zero mismatches, missing files or duplicate names. Both native and render completions exist for all 12 motion/evolution cases 72–83.

All **8,336 declared production entries** independently match across the frozen snapshot, current declared development outputs and delivered JAR. The snapshot predates the full cohort; the runtime proof was rebound after completion and this audit did not rewrite it. The client uses the development classpath; this comparison does not claim the client loaded the JAR.

- JAR SHA256: `f6d975a540c765f4e89383af9070c748ebb549ff5d00194d3dfda2b09e4a1a15`
- Model SHA256: `cfed60309de43bc854272a4869d455d9ec76281a37a1787ff223f69b7ed85881`
- Metadata SHA256: `20d1602340666c4f87fdf479277cb019308642d3ebad76ef3aae43926c18ced5`
- Final log SHA256: `027b6c62cf0b630232f3b1a1e63f3057412874a7b15e29aa394cb35eec9c703c`
- Runtime proof SHA256: `d2e8f96b661ead1e5deed1c645d4e2c2e40bd50c11e6058e0b423ecea690aa5a`
- Authored helper SHA256: `49d6f47c7bcd2bdc6bb7fa55b4882d9f904fdc5e51aa4d4a209871d9c880eb75`

Independently read actual results: broad build **13m 27s**, current focused **54s**, dedicated native **476/476 required tests in 48s**. Native batch counts independently sum to 476, including 48 new 062 and 428 retained tests. Focused core 7,453, rig 106,370,521 and material 316,516,118 recur in the broad invocation; these are separate invocations of the same checks, not additional unique coverage. Reviewed test sources still match their focused-review hashes. The 12 expression/marker emitter frames are numerical geometry coverage.

| Case | Actual action | Ticks / measured wall | Walked blocks | Captures | Age / adult age delta |
| --- | --- | --- | --- | --- | --- |
| 72 | balanced_walk | 240 / 11.991s | 8.3575 | 20 | 240 / 240 |
| 73 | run_walk | 240 / 11.981s | 18.6424 | 20 | 240 / 240 |
| 74 | water_swim | 240 / 11.987s | 0.0000 | 20 | 240 / 240 |
| 75 | broom | 180 / 8.963s | 0.0000 | 15 | 20 / 20 |
| 76 | picture_book | 180 / 8.954s | 0.0000 | 14 | 20 / 20 |
| 77 | carry_flying | 240 / 11.983s | 0.0000 | 20 | 0 / 0 |
| 78 | gain_walk | 280 / 13.965s | 10.5015 | 23 | 280 / 280 |
| 79 | gain_walk | 280 / 13.990s | 9.7693 | 23 | 280 / 280 |
| 80 | loss_walk | 280 / 13.984s | 9.7693 | 23 | 280 / 280 |
| 81 | pair_walk | 640 / 31.983s | 23.9259 | 53 | 640 / 640 |
| 82 | opposite_walk | 560 / 27.991s | 22.1754 | 46 | 560 / 560 |
| 83 | evolution | 300 / 14.986s | 0.0000 | 25 | 100 / 88 |

All scenes retain the configured 20 TPS animation clock; measured tick/wall rates are 20.006–20.103 TPS. Training/Analyzer input schedules 78–82 are deliberately accelerated. Cases 72–82 keep biological strength exactly 1.2, locked Neutral alignment 0, first Run type 3 and the same encoded lineage. Walk/training scenes advance both chronological ages exactly their elapsed ticks. Toys pause most care aging and carried states pause it completely, as the table makes explicit.

Case 81 reaches raw (-1,+1) at tick 240, holds the raw endpoint for 80 ticks through 320, then reverses the actual Analyzer schedule at 322–560. A real Render-thread `RUN062_PAIR_SETTLED` marker at 280 records displayed (-1,+1), maturity 1 and first Run 3. Full originals at 288/300/312 were personally opened and show the held shape with changing gait. Displayed sample 600 is balanced; full original 624 visibly confirms the returned low crest and cyan body.

Case 82 logs raw RP -.5/0/.5/1 at ticks 120/240/360/480, SF 0, maturity 1 and first Run 3 throughout. Its display follows 30-tick easing during continuous edits: raw 0 at 240 differs from displayed RP about -.518. Displayed sample 540 is +1; original 551 visibly shows Power features and the authored magenta/orange palette. Encoded strength remains 1.2; lineage stays [1279803727,1195463236]; both ages advance 560 ticks. Opposite Power crosses the Run-first axis without changing first identity.

Personal review opened `motion72-77-review.png` and `motion78-83-review.png`, containing **45 unique hash-bound actual originals** (18+27). It also reopened **21 full 1440×900 originals**, including at least one from every case 72–83. Exact paths/hashes are recorded in `final-live-review.json`. Sheets use uniform crops/resizing; no model or texture is substituted.

- **72:** Balanced walking keeps the head, orb, hands, feet and crest readable; native navigation walked 8.3575 blocks.
- **73:** Run gait has an extended stride with the Run head features attached; native navigation walked 18.6424 blocks and the running-frame assertion passed.
- **74:** The submerged, blue-tinted original shows the Run/Swim body and swimming limb pose. The actual SWIM-in-water frame assertion passed; displacement is exactly zero.
- **75:** The broom shaft and bristles are visible with the hands meeting the tool. Real native-dispatcher one-shot cleanup and 380 measured palm contacts passed.
- **76:** The open picture book, both hands and delighted face are visible. The broad Power/Swim silhouette stays coherent; native-dispatcher cleanup and 376 measured palm contacts passed.
- **77:** An upright hovering player carries the actual Accessories body. The front view fully hides the Chao; left/back views expose its body and appendages. Face/orb overlap the player hair/head from some angles, a retained visibility limitation.
- **78:** The selected moving sequence progresses toward Run features and a deep-blue palette while maintaining readable head, orb and limbs. Final raw axes are (-1,0).
- **79:** Both Power and Fly increase; taller three-prong head features and the lavender body are visible in the selected back view. Final raw axes are (+1,+1).
- **80:** The selected sequence changes from Fly toward low balanced features and a cyan body. Final raw axes are (0,0); strength and identity remain unchanged.
- **81:** Full originals at ticks 288/300/312 show the held Run+Fly shape with gait changes. Full original 624 shows the balanced shape after reversal. The actual render-thread marker at 280 records (-1,+1) before reverse edits start at 322.
- **82:** Selected originals show deep-blue Run, intermediate blue/cyan, then magenta/orange Power. Magenta matches authored Power BODY(233,99,245)/HAND(207,98,32). Raw 0 at 240 differs from eased displayed RP about -.518; display reaches +1 by 540. First type remains Run 3.
- **83:** Full originals show baby at 11, real cocoon at 155 and fresh lime Neutral/Run adult at 300. The fixture seeds childhood 12 ticks before the age threshold; native lifecycle ticking completes the transition. This is not a whole-childhood observation.

The observed head-top/central-crest marks leave forehead/sides plain. The original cube head, eyes/orb and 19 closed model parts with 59 joints retain the established construction. Only the new profile has 30-tick/1.5s edit easing. Static whole-family geometry/material coverage belongs to the separate final reviewers.

Limits: stationary water does not establish autonomous swim travel, and no autonomous glide is established. Carry is upright Creative flight with actual Accessories, not horizontal elytra/swim carry or takeoff/landing; player hair/head occludes the Chao face/orb at some angles. No actual three-visible-hurt-face pass, listening, full movie playback or independent remote multiplayer/latency/reconnect pass is claimed. Evolution seeds a baby 12 ticks before the age threshold and observes the genuine lifecycle through cocoon to fresh locked Run adult; it does not cover a whole childhood.

Artifacts: `final-live-numeric-audit.json` carries literal case/native/render lines and decoded tracked states; `final-capture-hashes.json` carries all 789 capture bindings; `final-production-hash-audit.json` carries all 8,336 entry comparisons; `final-live-review.json` binds personal visual review, source hashes, native batch counts and reopened originals. Movie metadata/review are owned separately by the coordinator.
