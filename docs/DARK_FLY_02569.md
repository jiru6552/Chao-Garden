# Dark / Fly-first family — 0.25.69

Implemented the complete first pass: a common newly evolved Dark/Fly form and nine mature signed-axis targets, including later Swim and all four compatible mixed pairs. The final build, dedicated tests and 112-scene real Minecraft capture passed. The original head box remains unchanged. Compact wing membranes and curved tip shapes are intentional blocky approximations described below.

This update builds on completed 0.25.68 and adds the full ordinary Dark-aligned, Fly-first adult family. It uses the established shared renderer and separate closed blocky features around the unchanged original 7×6×6 head box. Adult alignment and first type remain locked; biological maturity and later current-stat training remain independent.

[NeoForge 1.21.1 JAR](chao_garden-0.25.69.jar) · [Source changes](chao-garden-0.25.69-source-changes.zip) · [Verification results](verification-outcomes.json) · [Viewer findings](VIEWER_REFERENCES_02569.md)

## Family and progression

The original head box, facial geometry/UVs and 59 animation-joint definitions are unchanged. Two separate cuboids per side form the head roots and colored tips; six wing rays, two tail sections, compact body/palm/foot surfaces and six Dark-marker points bring the new target geometry to 23 closed cuboids. No library, saved field or network payload is added.

| Later specialization | Blocky interpretation and natural-coat accents |
| --- | --- |
| Newly evolved | Paired outward roots and downward colored tips; plum body, dark muzzle/chest, magenta cuffs, lavender hand/toe faces, green/cyan wings and cyan spiked marker. |
| Balanced | Longer down-hanging tips; plum/magenta/lavender accents, teal/blue bat wings and blue marker. |
| Run | Compact inward/broad upper caps; indigo/violet accents, peach wings and peach-pink marker. |
| Power | Taller paired roots with inward pincer ends; warm brown/plum, magenta/tan accents and yellow/orange wings/marker. |
| Fly | Taller outward roots with short upturned ends and the broadest fan; violet/lavender accents, pink/red wings and magenta marker. |
| Swim | Low/wide roots with the lowest descending outer tips; slate-blue/violet/cyan accents, compact lime wings and pale-lime marker. |
| Run + Fly | Raised roots and compact caps; indigo/violet accents and salmon/pink expanded wings. |
| Run + Swim | Low roots and shorter down-tips; indigo/violet/cyan accents and compact golden wings. |
| Power + Fly | Taller upright/outward pair and short colored tips; warm plum/magenta/lavender accents and red/orange wings. |
| Power + Swim | Low-to-medium roots with small downward ends; charcoal/plum/magenta/mint accents and golden wings. |

Straight cuboid ends approximate the source's curved/tapered points and rounded Run lobes. Later targets share one model and rig. Reverse-order compatible pairs share the same target.

One common newly evolved appearance develops toward balanced, Run, Power, Fly, Swim and the four compatible cross-axis pairs. Current training uses `100 × level + progress`, net Run/Power and Swim/Fly axes each saturating at 960 progress. Those scales and interpolation are mod authoring choices, not viewer formulas. Thirty-tick display easing smooths edits without writing back to age or training.

## Independent reference and comparisons

Fresh Chromium 156 rendered the viewer through WebGL2/SwiftShader. Actual mouse input selected the magenta **Fly first-type square**, left Child and Normal unchecked, changed full Dark alignment, age and both later training axes, and rotated front, both sides, back, above and below. No compatibility warning appeared. An initial sandbox launch failed before page load with `ERR_CERT_AUTHORITY_INVALID`; the installed browser worked through the approved outer executor and inherited proxy/CA trust with certificate verification enabled. Shared desktop-control tools were unavailable; the browser runner supplied actual interaction.

The frozen reference contains **205 unchanged selected source canvases**: 186 family views and 19 context/control views. All nine forms have five requested ages from front/side/back, mature forms have six angles, and eight nonbalanced tendencies have training-quarter comparisons at fixed mature age. Nineteen formal source sheets and ten full originals were personally opened. An independent pixel audit checked all 186 Fly-checkbox patches and 744 slider positions. Maximum normalized handle error is approximately **1.06 percentage points**, with calibrated training positions within **0.283 percentage points**; source settings and angles are approximate, not calibrated game units or camera degrees.

The audit caught inaccurate initial training-quarter clicks. Those 24 original canvases/session records were preserved as excluded history and recaptured in a second fresh TLS-verifying session. The selected set uses 181 unchanged first-session originals and 24 unchanged calibrated recaptures. Both successful sessions closed normally with zero JavaScript errors; no source pixels were edited.

Front/back/above views establish two lateral horns, with no separate axial crest or third branch. Young model crops are identical across later-axis settings; first identity and adult specialization are separate. Wing color also has a distinct intermediate stage: half-age Fly has muted olive/blue/lavender before mature pink/red. At fixed mature age, lower training develops from the mature balanced appearance. Some source mixed tendencies saturate early; the mod retains authored smooth, reversible blends. No viewer geometry, state variables, textures or morph formulas were imported. No dedicated old Dark Fly import exists; existing Fly/Hero Fly/Dark assets supply construction context only.

The final side-by-side sheets pair **81 front/side/back stage comparisons**, plus nine mature above comparisons and 24 fixed-maturity training comparisons. An additional sheet shows all 27 opposite game-side views. These use unchanged original viewer and Minecraft images, fixed crops and uniform nearest-neighbor scaling; no mirroring, recoloring or shape warping was applied.

I personally opened all six final sheets after the successful complete capture. The common young shape stays distinct from the mature targets; raised Fly/Power roots, squat Run caps and low Swim ends are readable in the front and side silhouettes. Half-age Fly retains its muted transitional wing palette. At fixed mature age, tips and pigments vary with training while the box head stays fixed. The viewer's mixed tendencies often saturate before the handle endpoint; the mod retains continuous authored blends. Idle leans and arm poses, lighting and camera angles differ between the two renderers. The opposite side and above views retain connected features; no new obvious detached or exposed part appeared in the inspected views. Wing fans remain more compact than the reference.

- [Newly evolved front, side and back](comparisons/final-matched-age000.png)
- [Half-developed front, side and back](comparisons/final-matched-age050.png)
- [Mature front, side and back](comparisons/final-matched-age100.png)
- [Opposite game side](comparisons/final-opposite-side-audit.png)
- [Mature head details from above](comparisons/final-mature-head-details.png)
- [Independent training at fixed maturity](comparisons/final-independent-training.png)

## Actual verification

- **Final build:** `build` completed successfully in 21m46s, including all 58 custom check dependencies and retained morphology/toy/attachment regressions. The ordinary Gradle `test` task is intentionally skipped; the actual custom tasks executed. Broad and focused results in the verification JSON refer to this one final invocation.
- **Dedicated server:** all **939 required tests** passed across 18 namespaces: 880 retained and 59 new. These cover first-evolution selection/ties, locked adult identity, signed/opposing axes, ordinary and combined drives, decreases, Creative/server permissions and command selection/bounds, maturity edits, entity tag roundtrips and tracked-observer updates. All 367 nonclient Java sources still match the before-run pin after the later client-only floor correction.
- **Core/rig/material:** 9,013 new core checks passed. The actual family rig test passed 1,588,908,385 assertions, including 3,452 age/axis/edit frames, production Lionfish poses, attachment matrices, fixed head/face/joints, feature continuity and closed geometry. It includes 116 proper car/crayon profiles and 74,356 floor frames; maximum half-tick wing movement was 0.7933355 model pixels overall and 0.74810326 in floor tests, below the unchanged 0.8 limit. Material checks passed across all **172 canonical coats**, preserving facial pixels, expected alpha and finish/color behavior. Piano and organ checks add 27 new family configurations each while retaining prior cases.
- **Actual Minecraft:** one successful final client invocation completed **112/112 scenes**, **1,158 original PNGs**, zero skips: 86 six-angle static scenes, three UI scenes with 16 actual UI images, motion/evolution/opposition scenes, six production-pose scenes and two real floor-toy lifecycles. Coverage includes all nine targets at new/half/mature development, independent training quarters, signed interior blends and crossings, earlier-family and baby regressions, five natural/coat/shiny pairs, pumpkin/rabbit/Phoenix parts with glasses, water, broom/book, equipped-player carriage and mouse-handler Creative controls. Not every PNG was personally inspected: all six final paired sheets were opened, and a supplementary personal review covered 286 canonical originals, including above/below and late floor views, with their hashes revalidated against the completed run.
- **Three actual recordings:** root personally reviewed **46 decoded silent samples** in three sheets (18 + 10 + 18). They show gain/loss, pair reversal, both opposite axes and representative production poses. No new detached geometry was visible in those samples. This is bounded sampling, not continuous playback or listening. Encoded frame totals below come from actual `ffprobe`, not nominal FPS multiplied by duration.
- **Artifact/runtime:** all **8,414 production entries** match the delivered JAR byte for byte, from 8,857 frozen declared development-runtime files. All 7,800 prior assets remain exact. The real graphical run used the development classpath; it is not claimed to have loaded the archive itself. No new dependency, saved field or network payload was introduced.

[103.60-second gain/pair/Run–Power recording](dark-fly-0.25.69-motion.mp4): 2,072 frames at nominal 20 fps; 18 sampled times.

[27.55-second Fly–Swim recording](dark-fly-0.25.69-second-opposition-motion.mp4): 551 frames at nominal 20 fps; 10 sampled times.

[122.15-second production-pose recording](dark-fly-0.25.69-production-poses-motion.mp4): 2,442 frames at nominal 20 fps; actual average `48840/2443` fps; 18 sampled times. The two floor-toy scenes have separate actual PNG/held-phase/contact/cleanup witnesses, not their own continuous movie.

## Approximations, failures and limits

The wings use three closed cuboid rays per side, rather than the viewer's broad, curved bat membrane. They remain visibly more compact and shallower, especially for balanced, Swim and Run/Swim targets; the fixed square head and arms obscure some of their frontal span. A separate source-width study found source fans roughly 1.52 times central-head width for fresh/balanced/Swim, and about 1.72 for Fly. These are source-image estimates, not proportions reproduced exactly in the mod. Balanced and Swim descending ends also stop higher along the cheek/neck than the smooth reference. Rounded Run caps, tapered horns, tail curves and the spiked marker use simple cuboid approximations.

Keeping the fans higher and broader exposed genuine head, palm and foot contacts during authoring. The final construction retains the full upper-ray span, shortens the middle/lower rays by 15%, and turns the middle ray rearward by half a degree around its unchanged hinge. This family's crawl transitions and exits from belly-down toy play use world-orientation interpolation and fourteen-tick settling on the existing production clock; the body, head, limbs and tail still settle in eight ticks. During floor play the fans tuck toward the spine after the complete body and hand pose. Their remembered bases return during ticks 4–8 of the existing body turn, while the wing rotation and opening finish on the existing fourteen-tick path. This uses temporary render context and transition history, without adding a saved timer. Wings also compact during dash, glide and rest. Crawl/glide and belly-down play can hide much of the fan behind the torso. These are animation interpretations, not viewer-derived motion formulas. Existing JOML supplies the rotation math; no dependency or biological-age field was added.

Earlier enlarged-wing diagnostics failed collision or strict movement-step checks and were retained separately. An additional direct car/crayon check exposed a separate late-pose boundary: those toys select WALK, then apply their belly-down pose after the ordinary transition. The older generic prone-model scan did not exercise this family's developed targets. The scoped correction now fits the fans after that whole pose, preserves the hand/body calculations and tests all nine targets at three maturity levels, both natural completion and cancellation, and the actual idle sitting return. The selected construction passed the actual complete family rig test with the original limits intact: 116 floor profiles and 74,356 floor frames, alongside the fully settled transitions and direct steady RUN regression. A missing import in the supplemental test initially prevented test compilation; it was corrected without changing production bytes. Neither earlier failures nor the first smaller-wing diagnostic captures are promoted as final evidence.

The separate journal-content checker remains **8/9** because of the inherited `Your gardens` paragraph-separator failure. Its checker and content are byte-identical to the completed 0.25.68 baseline. This is separate from the new morphology checks.

Floor-toy palm bounding envelopes can extend through the floor in the existing pose. All 1,379 observations from eight matched diagnostic profiles reproduced the same counts and depths in real 0.25.68 Dark/Swim fixtures. The wing correction leaves every non-wing pose unchanged and actual held-grip targets pass; it does not revise that shared palm fitting. Floor and prop surfaces can obscure palm edges in the actual car/crayon samples. Treat those inherited envelope observations separately from the corrected wing contacts, and check their appearance on the phone.

Screenshot samples establish the observed poses, not continuous temporal playback. Movie reports must state their actual decoded sampling scope; no listening claim is made. The viewer does not verify genetic colors or shininess: canonical texture checks and representative actual game finishes cover those separately. Pumpkin hats intentionally hide natural horns; rabbit and Phoenix parts replace their corresponding natural parts. A carried Chao is obscured from the front by its player, so side/back views provide the useful equipment evidence. The evolution cocoon also obscures the emergence shape.

The graphical checks use a real development-client classpath, rather than claiming that `runClient` loaded the delivered archive. No independent remote-player session, physical phone/touch interaction, shader-pack sweep, exhaustive attachment/toy combination, full pre-update-world restart or complete natural lifetime was tested. Save/tag roundtrips and tracked-observer checks have narrower scope than those scenarios. Existing Creative controls are exercised through their mouse handlers; this does not establish phone-control usability.

## Delivery and suggested checks

Use matching client/server JARs with Minecraft 1.21.1, NeoForge 21.1.219, Java 21 and the existing external Lionfish API 3.1. The source ZIP is a cumulative overlay onto complete 0.25.43 source, retaining completed local 0.25.68; no published 0.25.68 Git parent is assumed. Inherited guarded source deletions do not touch saves.

For an already evolved fully Dark (−100), Fly-first adult, existing testing commands can develop Run+Swim while retaining first Fly:

```mcfunction
/chao ~ -100 Power 0
/chao ~ -100 Run 10
/chao ~ -100 Fly 0
/chao ~ -100 Swim 10
```

Quoted names also work, for example `/chao "Gale" -100 Swim 10`. A partly Dark adult requires its existing locked alignment value; a request to change adult alignment is rejected. The Analyzer maturity bar changes current adult development without resetting lifespan or first evolution. Existing operator previews include `/chao testpose crawl loop`, `/chao testpose swim once`, `/chao testpose stop`, `/chao testtoy broom once`, `/chao testtoy picture_book loop`, `/chao testtoy toy_car once`, `/chao testtoy crayons loop`, `/chao testtoy stop` and `/chao testcarry loop|stop`. They reuse production clocks and cleanup, preserve care/age and real items, grant no rewards, restore temporary AI flags and stop on interaction, damage, unload, disconnect or explicit stop.
