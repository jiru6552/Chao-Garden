# Neutral morphology and Creative editing — 0.25.56

This is the first usable foundation for **Neutral alignment / Normal first evolution** and its nine later training combinations: centered, Run, Run+Fly, Run+Swim, Power, Power+Fly, Power+Swim, Fly and Swim. It builds on the approved .55 blocky style and Hero Run path. The original head box remains the construction anchor; new anatomy is expressed through surrounding blocky features and coherent shared-rig body adjustments rather than a replacement sculpted skull.

These are compatible-axis combinations, not nine different first-evolution identities. Run+Fly and Fly+Run describe the same state. A baby trained strongly in Run before first evolution can instead select **Run as its first type**, which is a different path. The new Neutral/Normal adult lattice does not silently overwrite that saved identity or claim to finish every other Neutral first type. An untouched ordinary baby with no recorded four-stat preference can select Normal; older saved Normal adults retain their identity.

## Reference and first-pass construction

The Chao Island viewer actually rendered in Chromium156 with WebGL2/SwiftShader. Child/adult selection, Normal first type, alignment, both opposed training sliders, age and orbit controls were operated. The successful sessions had healthy contexts and no compatibility warning. The sandbox browser initially rejected the proxy certificate; the same verified-TLS browser succeeded with the existing outer-executor trust. TLS verification was not disabled. Research retained **251 original canvas screenshots**, with hashes, control actions and settings notes. All nine mature endpoints were inspected from front, both sides, back, above and below; all nine also received young/intermediate age views, plus fixed-age training intermediates. Slider positions are visual approximations and do not establish absolute game training or elapsed-time rules. No viewer geometry was copied.

The original **0.25.51 blocky baby**, retained in `gameplay/chao_baby.json`, remains the construction anchor. Its **7×6×6 head**, eye/expression records, orb and joint pivots remain exact in the new model. Existing official-addon `chao_run`, `chao_power`, `chao_fly` and `chao_swim` geometry/textures were inspected for their cuboid construction and feature vocabulary. They are different first-type models and were not substituted for these Normal-first-type specializations.

| Neutral / Normal / later specialization | Viewer-guided blocky foundation | Current approximation |
| --- | --- | --- |
| Normal | Short central upright/slightly swept crown. | Two clean cuboids; unchanged square skull. |
| Run | Taller, rear-swept single crown. | A stepped root and cap, without Run-first-type rear spikes/front nubs. |
| Run + Fly | Lower, blunter rearward crest. | A low blocky hump; pair differences are intentionally modest. |
| Run + Swim | Longer pointed rearward crest. | Two overlapping angled boxes rather than a sculpted curve. |
| Power | Narrower upright curling crown. | A taller stepped upright feature, without broad horns. |
| Power + Fly | Short rolled/hooked upper feature. | A compact stepped hook; simplified curl. |
| Power + Swim | Tall upright/slightly rearward point. | A simple tall stepped cap; modest limb bulk. |
| Fly | Central forward/down-curled hook. | Three closed cuboids form an open angled arch above the fixed head. Its visible air gap is deliberate, not a torn mesh; the viewer's filled smooth skull is not reproduced. |
| Swim | Thin, longer rear-swept hook. | Two blocky segments in place of a continuous curved extension. |

All nine are implemented as usable foundation corners, rather than left as duplicate placeholders. Their differences are smaller than the distinct Run/Fly/Power/Swim first types. Every young corner shares the same fresh form, then develops through age and training. The original planar pink wings, pixel face and compact hands/feet remain; body and limb adjustments are modest. Fly's curl and the subtle paired forms are candidates for tomorrow's visual refinement.

One separate Neutral asset uses **ten shape targets**: one fresh adult plus a mature 3×3 opposed-axis lattice. It extends the existing rigid-box sampler and shared Lionfish rig without a new dependency or general API. Positive center/size/rotation targets are blended before the boxes are evaluated; there are no visible stage swaps, arbitrary donor vertex correspondences or shrinking head boxes. Surface queries, palms, rendering and attachment bounds share the same evaluated geometry. The approved Hero Run asset and its seven-target shape calculations remain unchanged.

## Identity, training and age

First evolution continues to compare **earned ability points** for Swim, Fly, Run and Power, excluding Stamina. The most recently actually raised tied ability wins. These points differ from the displayed level numbers. A combined training operation retains the established relative history of its increased stats rather than arbitrarily treating the last loop member as most recent. The special Chaos admission route retains its separate existing requirements.

Adult alignment and first type remain locked at first evolution. Later specialization reads current level/progress training, so increases, decreases, opposing training and Creative edits can change the adult's appearance without making it biologically younger or changing its first identity. For the new enabled Neutral/Normal path:

```text
training total = clamp(level × 100 + progress, 0, 9900)
Run/Power axis = clamp((Power total − Run total) / 960, −1, +1)
Swim/Fly axis = clamp((Fly total − Swim total) / 960, −1, +1)
maturity = clamp(adult evolution strength / 1.2, 0, 1)
```

Run is the negative end of the first axis; Power is positive. Swim is negative on the other axis; Fly is positive. The renderer blends the fresh first form into the mature two-axis targets as biological maturity rises. The **960 net-progress threshold is provisional mod policy**, consistent with the preceding Hero Run prototype; it is not a training rate established from the viewer or original game. Baby training still contributes to the current bars used after evolution.

The Analyzer's existing **Genes → Growth** meters use these same current axes for the new enabled path. Its maturity tooltip reports the signed targets, independent maturity and provisional threshold. Other existing paths keep their previous meter policy. Form labels use the corresponding Neutral policy without adding saved fields or rewriting historical training-direction mirrors.

The inherited natural growth clock is unchanged: with default three-hour childhood, passive full adult growth takes 14.4 active adult hours, while ordinary adult lifespan is twelve hours. Existing whole adult meals add 0.005 strength and equipped care can advance maturity while lifespan pauses. Full endpoints therefore need existing care boosts or the new Creative maturity control during testing. Training never reduces biological age or extends lifetime.

## Hero baby orb

The fresh viewer check confirmed **blue** for young Hero children and old Hero children at centered Run, and a gradual **blue → gray/purple → pink** transition for an older, strongly Run-trained Hero baby. Those intermediate hues and the pink endpoint are intentional reference behavior. The viewer does not show all inherited colors or shininess.

The reported original patchwork/strange-color symptom was **not reproduced with the current 172 atlases**: all 96 original orb texels were uniformly yellow before semantic painting. This update hardens the paint path so all six orb faces use one canonical alignment/age/Run pigment instead of inheriting source-atlas hue/shade differences, and protects that pigment from cosmetic palette recoloring. Normal Minecraft face lighting and the existing shiny reflection pass remain. This is a targeted material safeguard and a clarification of intended hues, rather than a claim that a confirmed phone-specific root cause was found.

Neutral/Normal adult markers separately develop peach → yellow with biological maturity, independent of their nine specialization combinations. Color and finish do not change morphology or marker placement.

## Analyzer alignment control

Open **Mind → Profile**. The bar shows Dark, Neutral and Hero, with a displayed testing scale of **−100 to +100**; internally alignment remains −1 to +1. Creative ordinary babies can be tapped or dragged toward either side while the player holds a Chao Analyzer. The thumb and requested number respond locally; saved care and the developing body update from the server's authoritative state. Requests are limited during dragging and the final position is always sent on release. A pending request is displayed briefly until its synchronized value arrives or the preview expires.

For an adult, the header explicitly says **ADULT ALIGNMENT: LOCKED**. Tapping it displays the reason: adult identity is locked at first evolution. This was already the intended underlying rule; the earlier inability to move an adult from Dark to Hero was not evidence of faulty phone input. This update clarifies the interface instead of silently removing that lock. Survival remains read-only. Special fixed forms and unavailable Chao also remain read-only.

The alignment hit area includes the caption and end labels, enlarging the original narrow target while keeping it clear of personality rows and navigation. This supports ordinary mouse clicks, dragging and taps delivered as mouse input; it does not by itself establish physical touchscreen compatibility on every launcher.

## Analyzer maturity control

In Creative, open **Stats** and tap or drag the **MATURITY** bar. The bar shows a responsive percentage to one decimal place, a larger thumb and an expanded hit area. This changes **current-life-stage biological growth**, not the active-life clock printed in the left profile:

| Current stage | Allowed control | Saved biological strength | Deliberate boundary |
| --- | --- | --- | --- |
| Ordinary baby | 0–99% | 0.00–0.99 | Stops below first evolution. The slider cannot turn a baby into an adult. |
| Ordinary adult | 0–100% | 0.00–1.20 | Remains an adult, including at 0%; locked first type and alignment stay unchanged. |
| Dead, immortal/special fixed form, Omochao or unavailable cocoon/activity | Read-only | Unchanged | No forced revival or rewrite of a fixed form. |

The wire represents the actual normalized stage fraction in thousandths: babies accept 0–990, adults 0–1000. Invalid values are rejected on the server; the normal baby UI never emits an evolution-threshold request. Each accepted edit uses the existing growth setter and resets its synchronization checkpoint safely. It does **not** change `ageTicks`, `adultAgeTicks`, birthdays, reincarnation count, a cocoon, or the adult identity lock. It does not itself invoke death, reincarnation or duplicate evolution.

Natural aging resumes afterward. A baby edited to 99% can still evolve later through the existing clock, and reducing an adult's maturity does not extend its remaining lifespan. A Chao already near the end of life is not protected from its ordinary next lifecycle update by this control. The separate existing operator command `/chao mature` deliberately advances a nearby baby's first-evolution age; that behavior is not hidden inside the slider.

Both Analyzer controls require actual Creative mode, a held Analyzer, an eligible Chao in the player's dimension, existing care/management authorization and the existing **eight-block** care range. Cocoon/activity restrictions are checked on the server for each request. A modified Survival client cannot gain authority from the screen's local flags. Successful edits synchronize the entity and equipped item state through the existing mechanisms and persist in the existing save schema. Omochao's finite mechanical lifespan does not make its fixed form maturity-editable.

## Creative/admin editing command

```text
/chao <name or ~> <alignment -100..100> <Power|Fly|Run|Swim|Intelligence|Luck> <level 0..99>
```

The command requires **Creative mode or operator permission level 2**. The Analyzer's Creative-only UI restriction is separate: an operator in Survival may run the admin command, but cannot drag the Creative bar. The command executes and authorizes selection/mutation on the server and does not require holding an Analyzer.

Names are case-insensitive exact matches among loaded, real Chao in the source dimension; quote supplied names, especially spaces or an existing subcommand such as `"rebirth"`. Autocomplete quotes names automatically. Named selection is not restricted to the nearby radius. `~` selects the nearest eligible Chao within **12 blocks**. No nearby Chao, an unknown/unloaded name, duplicate names, or an equal-distance nearest tie produces an explanatory failure rather than a silently selected target. Kindergarten pupils are excluded. Invalid alignment, stat or level arguments are rejected, and unavailable cocoon/activity/construction states are not edited.

| Argument or stored value | Actual units and bounds |
| --- | --- |
| Alignment argument | Integer −100…+100, mapped to internal −1…+1; 0 is Neutral. |
| Power, Fly, Run, Swim argument | Absolute displayed level, 0…99. The selected level's progress is reset to zero. Their earned-point totals remain separate values capped at 9,999. |
| Intelligence, Luck argument | Absolute **hidden level**, also 0…99, with that hidden progress reset to zero. These are not raw earned points and not inherited potential. |
| Intelligence/Luck earned points | Separate 0…4,000 totals. A hidden level is trained through 100 progress; earned gains depend on its saved potential grade. |
| Intelligence/Luck potential | Saved 0…255 grade/potential, used for training gains; this command does not edit it. |

Absolute level editing follows the existing Analyzer convention for points: it applies the level difference using the grade-dependent mean gain `13 + 3 × grade`, bounded to the appropriate earned-point cap. It does not invent random gains. Setting level zero resets the corresponding earned total to zero. Existing historical earned totals are otherwise adjusted rather than replaced with an assumed universal level-to-points conversion. A genuine four-stat increase records training recency; a decrease or same-level edit does not invent a more recent tie winner. Intelligence and Luck do not participate in ordinary first-type selection or the two morphology axes.

Baby alignment and the selected stat are one validated operation. An adult's requested alignment must equal its **rounded current locked alignment** on the −100…+100 scale. Otherwise the whole command is rejected and **neither value changes**; feedback gives the number to repeat when editing only the stat. An accepted adult command preserves the precise saved alignment and first type. For example, an internally locked −0.802 adult accepts the displayed command value −80 without changing its precise identity.

Examples, assuming `Milo`/`Blue Star` already have the intended alignment and first form:

```text
/chao "Milo" 0 Run 10
/chao "Blue Star" 0 Fly 10
/chao ~ 100 Run 10
/chao ~ 0 Intelligence 25
/chao ~ 0 Luck 50
/chao "Milo" 0 Run 0
```

The nearest `+100` example changes an ordinary baby's Hero alignment and Run level, or edits a Hero adult whose locked value is already +100. It does not convert a Neutral/Dark adult into Hero. For a Neutral/Normal adult, the first two commands demonstrate compatible Run+Fly development while preserving the Normal first type. Raise or lower the other compatible/opposing levels to compare combinations; same training settings in reverse command order produce the same later morphology.

## Verification

The coordinated focused Gradle invocation completed **BUILD SUCCESSFUL** with `--no-configuration-cache`. The current dedicated morphology/editing GameTest run completed **all 259 required tests passed**: 163 retained morphology/alignment cases, **54 new editing cases** and **42 new Neutral cases**. The initial command run caught a real Brigadier parsing defect with bare `~`; adding an explicit vanilla literal branch fixed it, and the final native run verifies the exact requested syntax.

| Check | Final recorded result |
| --- | --- |
| New core policy | **1,742** checks: all nine combinations, real ordinary/combined/Analyzer gains and decreases, current labels, opposing axes, compatible-pair order, independent maturity, immutable identity and persistence. |
| Creative editing policy | **361** checks, plus **2,824** maturity and **1,835** alignment input/geometry checks. Invalid bounds, scaled hit areas and stage limits are covered. |
| New production geometry/emitter | **7,443,569** checks; 1,534 age/axis/edit frames, 4,235 closed rectangular-box samples, 567 actual Lionfish motion frames, 6,804 separate attachment matrices and 1,044 coat/shape cases. Maximum sampled axis step **0.074799635** model pixels; maximum 20-tick edit step **0.17027597**. |
| Geometry connectivity | **8** positive closed boxes through **2,205** connected age/axis blends; exact original head/face/rig, anchored soles and Fly forehead clearance. |
| Materials | **68,841,869** checks across **172** original coats; 38,034,360 preserved-alpha pixels, 297,216 exact face/expression pixels and 445,824 Neutral orb samples. Maximum sampled 1% color step **4**. |
| Existing Hero Run | Retained core **1,128**, production rig **22,226,455** and material **23,615,146** checks passed. The original `.55` model asset is preserved byte-for-byte. |
| Keyboard contacts | Piano and organ each passed **171,558** hand samples, including 54 existing `.55` and **27 new production `.56`** age/axis configurations. Existing clearance/contact/reach limits are unchanged. |
| Dedicated server | **259/259** required cases passed. Includes exact native named/nearest dispatcher syntax, selection ambiguity/bounds, Creative/non-op and operator authority, forged Survival requests, adult atomic rejection, maturity limits, saved historical Neutral fixtures, current training changes, real NBT and synchronization. |

The initial compile attempt encountered the existing Gradle configuration-cache restriction on the resource-fixture task's execution-time `Task.project` access. Coordinated invocations explicitly disable that cache; no runtime or lifecycle rule was changed to work around it.

### Actual Minecraft client

The full private-world client study completed **58/58 scenes, zero skips, 413 original PNG saves**. Accepted final evidence replaces the three initial UI scenes with the last clear-view rerun: again **3/3 scenes, zero skips, 25 original PNGs**. The final accepted selection therefore contains **413 unchanged original PNGs** from the latest successful scenes. The first full run and both UI reruns completed BUILD SUCCESSFUL. Earlier UI captures had hover tooltips covering settled values; the final dev-only helper records the actual rendered pointer position away from the controls. Production tooltips were not suppressed.

The client used Minecraft1.21.1/NeoForge21.1.219, Java21, Lionfish3.1 and real Accessories/owo, software OpenGL and shaders disabled. It rendered the production models/materials and existing animation clocks. All49 static scenes were reviewed in all six angles (**294 original images**): nine Neutral forms at fresh/half/full maturity, representative 25/50/75% training mixtures, Hero orb age/alignment states, named/mono/jewel/shiny examples, animal parts, glasses, a skull hat and fresh/mature Hero Run regressions. Every static form remained coherent, without a blocking detached crown, torn surface, distorted limb or eye intrusion. One half-age Fly fixture naturally blinked in two views; its other ages show the native eyes intact.

Static photography deliberately raises and freezes the fixture to expose its underside. It is not a new hovering behavior, a natural-lifetime observation, or evidence of actual world-floor contact. Motion and toy fixtures run at the real floor. Matched cameras do not imply identical idle joint poses or per-image skull scaling; fixed head geometry is established separately by the emitted-vertex tests.

All **25 final UI images** were reviewed. The real Analyzer screen's native mouse handlers and server payload path show a baby changing alignment to Hero and scrubbing maturity through25% to99%; a Dark adult remains alignment-locked while scrubbing25% to100%; Survival stays read-only after actual forged requests. All three preserve their life stage and reload entity NBT exactly. Ongoing ordinary aging can slightly advance a requested value afterward. Physical phone touch and other live GUI scales were not exercised; the enlarged tap targets and smaller-scale mapping checks are not a mobile hardware claim.

Representative broom and book previews used the production command adapters/animations without rewards. Selected front/side/back frames show both broom grips and floor contact, book clearance above the feet, page motion and cleanup. Actual Accessories baby/adult projections stay against the player's back in **upright Creative flight**; front views naturally occlude part of the Chao. Horizontal elytra, swimming and takeoff/landing transitions were not visually retested in this pass.

A **28.55-second,1440×900,20fps silent recording** shows actual Neutral Run+Fly training gain and Run reduction at the normal20TPS production clock. Fixture training is accelerated; it is not natural training-rate footage. Twelve individually decoded frames at0,2.5,…27.5seconds and representative native motion PNGs were inspected. The actor naturally changes crawl/walk/seated poses as its stats change; this does not reduce biological age. Full interactive movie playback and sound listening were not performed.

The client ran on the declared Gradle **development classpath**, not by installing the packaged JAR. A frozen byte comparison covers every production Chao class and Chao asset in the final JAR; the matching class/resource counts, exact JAR hash, CRC and unique-entry checks are included with the delivery. The dev-only study/fixture classes and test resources are excluded from the installable JAR.

### Broad checks and inherited failures

The broad `build --continue` invocation completed in11m49s and exited nonzero **only** for the two inherited transition suites: triangle **25/267,696** samples (minimum separation−0.54432297 model pixels), trumpet **37/267,696** (−0.58569264). Their complete result-file SHA-256 hashes match the delivered `.55` results exactly. Their unchanged harnesses cover four older profiles, not the new Neutral/Normal lattice. Those unrelated failures were not weakened or hidden; a wholly green full build is not claimed. Other executed checks passed, and the updated JAR was built.

The standalone journal content test passed **8/9**, retaining the existing `Your gardens` paragraph-format assertion. Its test and content source are byte-identical to delivered `.55`. No journal mechanic or text was changed here. The initial new core test used the wrong lattice corner index for Power+Swim; that assertion was corrected to the actual grid corner, and the final core suite passed.

Older raw toy-model loops enumerate the new asset but generally sample only its fresh corner; they do not establish mature contact for all nine combinations. The new production motion/attachment suite, actual representative broom/book work and new piano/organ grid establish a foundation-level attachment pass. Other toys, every replacement hand and every adult combination still need broader visual checking. The known `.55` generic native toy-preview fixture failures were not rerun as a full native suite here; the dedicated run above is the focused259-case scope, not a claim that all server/modpack tests are green.

## Tomorrow's in-game check

1. For these exact Neutral/Normal forms, start with a **truly untrained fresh baby**, preserve Neutral alignment and let first evolution select Normal. An operator can use existing `/chao mature` before training the four main abilities, then wait for the cocoon. Resetting a previously trained baby's levels does not erase its tie history. Existing saved Normal adults are also suitable.
2. In Creative, set adult maturity to100% in Stats. On a Neutral adult whose locked displayed alignment is0, `/chao ~ 0 Run 10` and `/chao ~ 0 Fly 10` give the full Run+Fly target. Set opposing levels back to0 and compare the other listed combinations; test intermediate maturity as well. Wait about one second after a large edit for the display easing.
3. Try `/chao "Milo" 0 Power 10` for a uniquely named Neutral adult, and `/chao ~ 0 Intelligence 25` or `/chao ~ 0 Luck 50` for the hidden-level controls. For a Dark adult locked at−100, `/chao ~ -100 Run 20` changes its stat; requesting100 rejects the whole edit and reports its lock.
4. Inspect Fly's blocky curl, Run/Swim silhouette differences and the smaller paired-form details. Compare color/shiny variants, hats, animal replacements and your usual toys. Recheck the orb's intentional transitional hues versus shiny reflection.
5. Check phone readability/taps, your full modpack/shaders and a remote multiplayer client. Recheck swimming/horizontal flight carrying and natural progression in a copy of an existing world.

No real user world was modified by the fixtures. Arbitrary older worlds, independent remote-player synchronization/latency, full modpacks, arbitrary resource packs/shaders, physical phone touch, all toys/all combinations and full natural-lifetime pacing remain unverified. Use matching **0.25.56** client/server JARs and external **Lionfish API3.1**.
