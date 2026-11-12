# Hero Normal morphology foundation — 0.25.57

This update extends the approved blocky system to **Hero alignment / Normal first evolution** and the same nine later specialization combinations: Normal, Run, Run+Fly, Run+Swim, Power, Power+Fly, Power+Swim, Fly and Swim. “Hero / Neutral / …” refers to this family; the mod calls the balanced first type **Normal**. Run+Fly and Fly+Run are the same compatible pair.

These are later appearances of a saved Hero/Normal adult, not nine new first-evolution identities. The existing Hero/Run first-type prototype and Neutral/Normal foundation remain separate paths. An adult whose saved first type is Run does not silently become Normal, and a Dark or Neutral adult is not realigned to Hero to enter this renderer.

## Construction and reference

The original blocky baby model retained in `gameplay/chao_baby.json` remains the style and shared-rig anchor. The **7×6×6 head box**, pixel face, eyes and semantic joint pivots remain unchanged; additional cuboid features, small body/limb adjustments and the Hero halo provide the developing silhouette. The mature corners and reference-specific approximations are recorded with the completed viewer/visual evidence, not inferred from the old Run-first-type asset.

The existing Chao Island viewer workflow and old official-addon Hero assets provide the references. The viewer is a source for proportions, feature placement, silhouette and transitions; its smooth skull is not a replacement for the mod's box. Existing `art/imported_chao/models/chao_hero.geo.json` and `art/imported_chao/textures/hero.png` provide block construction/texture context. The retained reference evidence distinguishes actual rendered views/control actions from authored simplifications; merely loading the page is not treated as an inspection.

The fresh reference session actually rendered the Chao in Chromium 156/WebGL2. Child/adult, Normal first type, Hero alignment, Run/Power, Swim/Fly and age controls were changed, and orbit drags produced different front, side, back, above and below views. The sandbox certificate error was resolved by using the same verified-TLS browser with the existing outer-executor trust; TLS verification was not disabled. No compatibility warning appeared. The viewer's controls establish visible comparisons, not extracted exact game values.

All nine mature Hero/Normal endpoints retain **two top features beneath the halo**. Pure Run's long points and Swim's broader horns differ from Fly's small buds and Power's broad caps. A third central horn, the Hero/Run first type's three-branch crest, and separate Run-first-type front nubs are not part of these reference endpoints.

| Hero / Normal / specialization | Actually observed mature silhouette | Approximate halo / belly color families |
| --- | --- | --- |
| Normal | Two enlarged upright leaflike buds on short white bases. | Blue / yellow. |
| Run | Longer narrow points diverging outward and upward; they overlap in a side profile. | Cyan / blue. |
| Run + Fly | Shorter, broader leaf/spike stalks than pure Run. | Gray-mint / gray-lavender. |
| Run + Swim | Long broader triangular points with white roots. | Mint / cyan. |
| Power | Short outward-bending white necks with broad fistlike caps. | Fuchsia / yellow-gold. |
| Power + Fly | Slightly longer rising necks with broad angled caps. | Pink / ochre-tan. |
| Power + Swim | Broad outward/upward curved horns with joined-looking roots. | Pink / light green. |
| Fly | Small rounded caps on thin short stalks, the smallest mature head additions. | Salmon / pale peach. |
| Swim | Broad triangular outward/upward horns with joined-looking bases. | Pale yellow / mint. |

The natural reference family retains a dominant white head/body, colored hand/foot tips and caps, and yellow/pale wings. Feet have white side/back portions and colored toes; broad accent regions also appear on the back. The fresh centered form has short cyan buds and a pale-cyan halo. These are observed color families and placements, not exact sampled RGB values or measurements of band widths. The mod interprets the smooth shapes as a few cuboids and broad pixel rows while keeping the original head box. Named coats and shiny finishes require separate mod verification because the viewer does not expose them.

The new path uses a separate Hero/Normal asset with one fresh adult corner and a mature 3×3 opposed-axis lattice, preserving the shared animation and attachment rig. Center, positive size and rotation targets blend before rigid boxes are evaluated; this avoids unrelated donor-vertex correspondence and visible stage swaps. No new model library or general morphology API is needed. Surface queries, visible hands and attachment sampling use the same geometry as rendering.

## Identity, reversible training and maturity

Eligibility is an ordinary living adult with **locked Hero alignment (at least +0.5 internally)** and **locked Normal first type**. Character, immortal/fixed and separate mod-variant forms retain their existing paths. The new appearance reads current ability bars:

```text
training total = clamp(level × 100 + progress, 0, 9900)
Run/Power = clamp((Power total − Run total) / 960, −1, +1)
Swim/Fly = clamp((Fly total − Swim total) / 960, −1, +1)
maturity = clamp(adult evolution strength / 1.2, 0, 1)
```

Run is negative and Power positive on one axis; Swim is negative and Fly positive on the other. Both independent axes can develop together. Maturity blends the fresh adult into the mature targets, while later training selects the specialization. Large edits ease from the currently displayed shape over twenty client ticks. Gains, ordinary/combined drives, reductions and Creative editing therefore agree; decreasing Run or adding opposing Power can recede its specialization without lowering biological age or changing the adult's identity.

The **960 net-progress threshold is provisional mod policy**, retained from the prior prototype/foundation. The viewer's controls do not establish this absolute number or elapsed Minecraft training time. Existing biological clocks and food/carry boosts are unchanged: default passive full adult maturity takes 14.4 active adult hours against a twelve-hour ordinary adult lifespan, so existing care boosts or Creative maturity editing are needed to inspect the full endpoint within that lifespan.

First evolution still selects the highest **earned-point total** among Swim, Fly, Run and Power, with the most recently actually raised tied ability winning. Stamina does not compete. Levels are separate from those earned points. The special Chaos admission route and existing saved identities retain their established rules. An untouched ordinary baby with no recorded four-ability preference can select Normal; resetting a previously trained baby's stats does not erase its tie history.

The new path adds no save fields. Older Hero/Normal adults derive appearance from their saved current bars and maturity. Saved DNA, genetics, hats, lessons and first identity are retained. Colors and shiny finishes are underlying coat/finish choices, not separate shape systems. The original Hero baby orb policy is preserved.

## Analyzer integration

The existing **Genes → Growth** meters and displayed secondary-form labels use the same current axes for this Hero/Normal path, as they already do for the Neutral/Normal foundation. Other first types keep their previous rules, including the completed Hero/Run path. The maturity hover help identifies the Hero/Normal target, independent biological maturity and provisional threshold.

One focused usability refinement keeps the live **MATURITY** percentage and thumb visible while the player is actively scrubbing: the maturity hover tooltip is temporarily withheld during that drag. Normal maturity help returns on release; ordinary skill tooltips, server authorization, requested-value preview and pending synchronization remain unchanged. This does not add or widen editing authority.

Creative maturity remains **within the current life stage**: babies stop at 99% before first evolution, adults can move 0–100% without becoming babies. The control changes the existing biological growth strength/checkpoint, not `ageTicks`, `adultAgeTicks`, birthdays or remaining lifetime. Natural aging resumes afterward. It does not itself trigger death, reincarnation or duplicate evolution.

Adult alignment remains visibly read-only under **Mind → Profile**, including Hero/Normal adults. Changing maturity or training cannot convert a Hero adult into Dark. The Creative alignment bar is for eligible babies; Survival cannot edit either control. Existing server checks still require actual Creative authority, a held Analyzer, range/care eligibility and an available Chao.

## Commands and a quick Hero/Normal fixture

The existing Creative/admin command is unchanged:

```text
/chao <name or ~> <alignment -100..100> <Power|Fly|Run|Swim|Intelligence|Luck> <level 0..99>
```

All six selectable stats use absolute **levels 0–99**, resetting selected progress to 0. Intelligence/Luck also have separate earned totals 0–4000 and potential 0–255; the command's final number is neither of those. The command uses the existing grade-dependent mean earned-point adjustment. A conflicting adult alignment rejects the entire transaction and reports its locked value; matching the displayed rounded value permits stat editing while preserving the precise saved alignment.

Names are exact case-insensitive loaded matches in the source dimension; quoted names also avoid collisions with existing subcommands. Autocomplete quotes names. `~` selects the nearest eligible Chao within twelve blocks, rejecting equal-distance ties. Missing/unloaded/duplicate names and invalid inputs produce feedback rather than selecting an arbitrary Chao. Commands require Creative or operator permission level 2; the Analyzer controls remain specifically Creative-only.

To inspect these exact forms, start with a **truly untrained fresh ordinary baby**. While it is still a baby, `/chao ~ 100 Luck 0` sets full Hero alignment without training one of the four competing abilities. An operator can then use existing `/chao mature`, wait for its cocoon and confirm **Hero / Normal**. Natural first evolution also works. A previously trained baby may select its remembered four-stat preference even after its levels are reset.

For an adult whose locked displayed alignment is exactly +100, set maturity to 100% under Stats, then use examples such as:

```text
/chao ~ 100 Run 10
/chao "Milo" 100 Fly 10
/chao ~ 100 Power 0
/chao ~ 100 Swim 0
/chao ~ 100 Intelligence 25
/chao ~ 100 Luck 50
```

Run 10 and Fly 10 together reach the full compatible target when their opponents are 0. The named example assumes `Milo` is the intended unique Hero/Normal adult. Other Hero adults can be locked at values such as +80; repeat their own displayed alignment number instead of 100. Reverse command order selects the same later compatible pair. Wait about one second after a large edit for the display easing, then compare lower training and maturity settings.

## Implementation and references

The new family is one separately selected `gameplay/chao_hero_normal_proto057.json` asset: one fresh adult target plus a mature 3×3 opposed-axis lattice. It uses the existing ten-target rigid-box sampler and shared Lionfish rig. No new dependency or general morphology API was introduced. Four closed cuboids form two white roots and two colored caps; four more make a rectangular halo. Compact body, hand and foot changes use five additional rigid-box targets. The **original 7×6×6 head, facial UVs, eye/expression geometry and joint pivots remain fixed**. Halo geometry intentionally changes from the original baby marker into an adult ring while its attachment/reaction-symbol joints remain shared.

The generator and independent validator check thirteen positive closed boxes across 2,205 connected age/axis blends, original head/face/rig identity and floor-anchored soles. Halo clearance uses oriented boxes after the actual marker tilt, rather than assuming a negative vertical bound means a collision. Source HeroRun `.55` and Neutral `.56` model files remain byte-identical to delivered `.56`.

The actual viewer session retained **166 original screenshots** with hashes/actions: all nine mature forms in six angles; all nine young/half forms in front/side/back; quarter/three-quarter-age profiles; and representative fixed-age training intermediates. Chromium 156/WebGL2 rendered successfully, controls and orbit worked, and no compatibility warning appeared. The initial sandbox certificate failure was resolved using existing outer-executor browser trust with TLS verification enabled. No viewer geometry was imported. Original-addon Hero/HeroFly/HeroPower/baby records and converted models were construction references, not substitutes for this different first-type family.

These are deliberately simplified interpretations. Roots/caps use a few closed boxes instead of rounded or curved buds/horns. Run and Swim extend paired points; Power makes broader/blunter caps; Fly keeps shorter small buds. Some paired-form differences are subtle. The square halo retains the Minecraft interpretation; marker and tip RGBs and broad band widths are authored approximations, not extracted game formulas. The existing planar wings remain. Exact shape progression and original-game absolute timing were not inferred merely from the viewer opening.

The material keeps original faces/expressions and source alpha, except two formerly unused body/tip swatch pixels. Natural Hero colors use white skin, a small flat belly/back patch, colored caps, broad existing hand/toe pixel rows, recolored wings and an independent halo pigment. The source baby-atlas Hero wings are pink, so this family deliberately paints the reference's yellow young wings rather than claiming an unchanged full fresh atlas. Named, monocolor and jewel coats keep inherited body hues with corresponding bounded-contrast details. Shine uses the existing finish/reflection pass. The viewer does not verify inherited colors/shininess.

## Verification

Focused core/renderer/material/keyboard Gradle checks completed **BUILD SUCCESSFUL** with `--no-configuration-cache`. A subsequent current rig/cache and dedicated-server invocation also completed successfully with **all 305 required tests passed**. Initial renderer testing exposed a real UV/attachment alias omission for the new profile; it was corrected to the canonical baby family before client testing. The stable-cache test confirms current production already guards inactive profile updates; no new cache behavior fix is claimed.

| Check | Recorded result |
| --- | --- |
| New core policy | **1,957** checks: all nine combinations, actual ordinary/combined drives, Analyzer/command increases and reductions, current labels, compatible-pair order, maturity and immutable identity, and persistence. |
| Production rig/emitter | **9,590,990** checks; 1,534 age/axis/edit frames; 9,528 closed rectangular-box samples; 567 Lionfish motion frames; 6,804 separate attachment matrices; 1,044 coat cases; 23,440 actual tilted-halo separation tests. Maximum sampled axis step 0.024278762 model pixels, 20-tick edit step 0.11147869. |
| Cache behavior | Repeated unchanged production development calls keep geometry/cache revision stable for Hero/Normal and Neutral. A real opposing-stat change invalidates exactly once and changes the shape. |
| Materials | **73,779,511** checks over 172 source coats; 38,034,360 preserved-alpha samples; 297,216 exact face/expression samples; 445,824 canonical halo samples; 6,873,120 exact unaffected body/head samples. Maximum sampled 1% color step 4. |
| Preserved families | Existing `.55` HeroRun rig/material and `.56` Neutral rig/material suites passed; exact existing model files are retained. Prior core/evolution/editing tests remain active. |
| Piano/organ contacts | Each passed **175,932** hand samples across 54 existing `.55`, 27 `.56` and27 new production `.57` configurations. Existing contact, clearance and reach limits remain unchanged. |
| Dedicated server | **305/305** required tests: 259 retained cases plus 46 new Hero/Normal cases, including actual quoted-name/nearest command paths, authority, forged/adult rejection, real maturity handler, NBT/sync, first evolution, ties and opposing influences. |

No genuine older Hero/Normal entity fixture was located among the retained worlds inspected. Migration coverage uses an **explicitly synthetic supported older expanded-care21 Hero/Normal record** without newer lock/growth fields, plus unchanged genuine older Neutral fixtures. This does not claim a real old Hero world was loaded. No saved format version or persistent fields were added; arbitrary user-world compatibility still needs a copy-of-save check.

### Actual Minecraft client

The final actual Minecraft invocation completed **56/56 scenes, zero skips and 435 unchanged original PNGs**, with `BUILD SUCCESSFUL` in 6m 12s. It ran Minecraft 1.21.1 / NeoForge 21.1.219, Java 21 and Lionfish 3.1 with real Accessories/owo installed, software OpenGL and shaders disabled. The client used the development classpath; its frozen production classes/assets are byte-compared with the delivered JAR, rather than claiming that `runClient` loaded that archive directly.

The 45 static scenes include all nine Hero/Normal combinations at 0/50/100% maturity, representative 25/50/75% training at fixed mature age, retained HeroRun and Neutral forms, shiny/monocolor/jewel coats, a pumpkin hat, glasses/animal parts and Hero baby orb regressions. Each has front, left, right, back, above and below captures. These fixtures are raised/frozen two blocks for below views; they are not proof of natural hovering or floor contact. Matched camera positions do not freeze every idle joint or blink.

The three Analyzer scenes add 25 originals, including 16 actual screen captures. Adult alignment remains read-only; a forged conflicting adult alignment and a forged Survival maturity edit were rejected. Actual Creative maturity drags show a live unobstructed thumb/value (including held intermediate 59.4% for an adult and 66.4% for a baby), settle to requested safe endpoints, and preserve life stage/first identity through NBT. Baby alignment changes update its developing appearance. This exercises the real pointer callbacks at 1440×900 / GUI scale 2, not physical phone touch.

Eight additional scenes record actual Run/Fly and Power/Swim gain/reduction while navigating, a broom interaction, a picture-book interaction, and adult/baby real Accessories projection against the player's upper back during upright Creative flight. Selected first/middle/last front/side/back frames were visually inspected; no blocking head/face tear, elongated extremity, feature separation, toy grip or attachment defect was observed in those samples. Broom/book once playback completed and cleaned up. This is representative evidence, not every toy or every interruption.

The **56.55-second silent 1440×900 / 20fps movie** records the four gain/reversal scenes at normal recording speed, using deliberately accelerated fixture training at native 20TPS. Twelve decoded frames at 0, 5, …55s and 72 selected motion/toy/carry originals were reviewed; full interactive movie playback and listening were not performed. All final original PNG hashes and the movie hash are bound to this successful cohort. Two earlier private attempts were excluded: one stopped at a strict navigation assertion because its zero-Run fixture had not unlocked walking, and one was intentionally stopped while correcting passive walking gains in that fixture. The final fixture uses balanced baseline training and real Analyzer progress edits to hold its intended axis; production movement/evolution rules were unchanged. Both final Power/Swim navigation cases traveled about 9.74 blocks and met the original strict checks.

The visual audits found no blocking defect in the inspected views. Some compatible-pair differences are deliberately modest; a few captures contain ordinary blinks or idle tilts rather than missing eyes or a resized head. Independent remote multiplayer, shaders, physical mobile input, horizontal elytra/swimming carry transitions, natural autonomous toy approach/forced interruption, audio and a full natural lifetime were not tested in this cohort.

### Broad checks and inherited failures

The broad `--continue build` invocation completed with **two inherited failed tasks**, `triangleTransitionRigTest` and `trumpetTransitionRigTest`. Triangle retains 25 failing transition samples out of 267,696 (minimum hand/head clearance −0.54432297 model pixels); trumpet retains 37 out of 267,696 (−0.58569264). Both complete result files are **byte-identical to delivered 0.25.56**, not merely similar counts. All other executed build checks passed; no new unresolved failure was found. The installable JAR was built successfully, but the overall broad build is not green.

The separate journal check passed **8/9** and retains the existing `Your gardens` paragraph-format assertion. Both its test and journal-content source are byte-identical to delivered 0.25.56. These inherited failures were preserved and reported rather than removed or weakened. The initial new-profile UV alias test failure was fixed before the successful focused/native/client runs.

Older raw generic toy-model loops can enumerate the new asset while sampling only its fresh target; that is not all mature combinations. The new production pose/attachment grid, keyboard contacts and actual representative broom/book checks are separate evidence. No all-toys/all-forms claim is made. Previously known generic native toy-preview fixture failures were not rerun as a full native suite; the dedicated 305-case scope above is explicit.

## Remaining in-game review

All nine are usable first-pass forms, with the simplifications above exposed for refinement. Compare the paired root/cap proportions, smaller Fly buds, broader Power/Swim combinations, broad pixel bands, rectangular halo and inherited color/shiny finishes. The existing HeroRun three-branch path should remain distinct from Hero/Normal/Run's two-feature path.

Use the recipe and commands above on an untouched Hero baby or a saved Hero/Normal adult. Compare maturity 25/50/75/100% and current stat gains/losses, including compatible pairs and opposing training. Verify the permanent adult-alignment lock and unobstructed maturity thumb/value on your phone. Whole-clip toy testing, every accessory/replacement hand, horizontal elytra/swimming carry transitions, physical mobile input, remote-player latency, shaders/resource packs/full modpacks and natural-lifetime pacing remain separate checks unless explicitly recorded above.

Use matching **0.25.57** client/server JARs and external **Lionfish API 3.1**. All test worlds are private fixtures; no real user world was modified.
