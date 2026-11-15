# Hero Fly-first morphology — 0.25.65

This update implements **Hero / Fly-first / ~**: balanced later development, Run, Power, Fly, Swim, Run+Fly, Run+Swim, Power+Fly and Power+Swim. Each compatible pair has one target regardless of order. The balanced form is still Hero-aligned and Fly-first; “balanced” describes later training, not a change to Neutral alignment.

## Independent viewer inspection

A fresh Chao Island viewer session rendered a Chao in Chromium 156 with WebGL2/ANGLE SwiftShader. The actual magenta Fly first-type checkbox, Hero alignment, age and both later-training sliders were operated, and mouse drags plus R-reset rotated the model. The Fly checkbox remained selected when later training crossed through balance to Swim; that opposite endpoint has a distinct flattened rearward crown. No compatibility warning or Continue prompt appeared. The sandbox browser initially failed before page load with `net::ERR_CERT_AUTHORITY_INVALID`; the same TLS-verifying browser worked through the inherited proxy/CA settings in the outer executor. Shared-desktop control tools were not offered, so installed Chromium/Playwright operated the real page controls.

The reference contains 186 unchanged original canvases: 150 family views and 36 control/context views, with 548 recorded mouse/keyboard actions. All nine directions were checked at ages 0/25/50/75/100%; front, face-right side and back at 0/50/100%; six mature angles; and 25/50/75% training at fixed mature age. All 21 review sheets and 36 full originals were personally opened by the reference reviewer. There were no JavaScript page errors and the WebGL context remained healthy. An independent pixel audit checked all 600 family handle readings and 150 selected-Fly checkbox patches. Positions are approximate normalized control fractions, not recovered game levels, timing or calibrated camera angles.

| Later influence | Observed reference and blocky interpretation |
| --- | --- |
| Fresh | Short paired arches, pink halo, white base with lavender/pink, cyan and white regions. |
| Balanced | Low outward/down crown arches; blue halo; magenta limbs, cyan wing edges and two-color chest blush. |
| Run | Tall outward V with a forward/up hooked side profile; yellow halo and pale pink/lavender accents. |
| Power | Tall branched V with upright prongs and small side spurs; gold halo and pale pink accents. |
| Fly | Long outward/down arches sweeping behind the head, separate upper forks and broad cyan/white fan wings; yellow halo. |
| Swim | Low nearly horizontal rearward tips; pale lemon/green halo, periwinkle and lime accents. |
| Run+Fly | Shorter outward/down arches and medium-large cyan/white wings. |
| Run+Swim | Moderate narrow raised V; periwinkle/lime accents and blue-gray/tan chest patch. |
| Power+Fly | Rearward/down main arches plus upper forks; shorter than pure Fly, with large wings and gold halo. |
| Power+Swim | Medium raised outward V and small spur; purple/periwinkle, lime and gray/tan accents. |

The reference’s age and training progression is nonlinear. For example, mature Run at half training can remain low while half-age Run at full training already has a raised V; compatible pairs can appear near their endpoint at half training on both controls. The mod deliberately uses authored age blending and bilinear training targets. Its common fresh target simplifies source forms that look similar but are not pixel-identical. Source geometry, textures and morph formulas were not imported.

## Construction and behavior

The latest published .64 project is the baseline. The original **7×6×6 head box**, rectangular face and all 59 canonical joint definitions remain unchanged. Its morph targets drive 21 closed cuboids within the inherited mesh: six crown sections, six wing rays, four halo bars and five compact torso/limb targets. Three mesh-only halo children bring the rig to 62 bones. Separate crown roots, main tips and small auxiliary forks move/rotate/scale through ten targets: one fresh target and the nine mature corners. Unused forks stay positively sized inside their connected crown root and emerge gradually. The halo retains a rigid four-bar construction and a coordinated small local translation for clearance. Original tail geometry and UVs remain intact.

Existing Lionfish rigid-box evaluation, animation clocks and shared attachment sampling remain in use. No new library, general-purpose API, save field, migration or network payload was introduced. The imported official Hero Fly, Hero and related cuboid assets were inspected as construction references. The viewer supplied the age-specific anatomy and colors.

`HeroFlyMorphology065` admits living ordinary mortal adults with locked Hero alignment at least +0.5 and locked Fly first type (`FLY_TYPE=2`). It reads current training bars (`100×level+progress`, bounded to 9900), with independent `(Power−Run)/960` and `(Fly−Swim)/960` coordinates clamped to −1…+1. The 960-point scale is the existing provisional mod pacing, not a formula recovered from the viewer. Both opposite directions are supported. Gains, ordinary and combined drives, reductions, Analyzer edits and absolute commands use those same current bars. Existing saved legacy influence fields are preserved. Biological maturity gates development independently; display-only thirty-tick easing smooths large edits without changing age or training.

First evolution still chooses the highest earned Swim/Fly/Run/Power points, with the most recently actually raised tied ability winning. Stamina, Intelligence and Luck do not compete. Adult alignment and first evolution type remain locked. Later Swim can cross beyond balance while first type remains Fly; later Run and Power cancel/cross on their other axis. Training changes never invoke a second first evolution or make an adult biologically younger. Deliberate Creative maturity edits still change visible maturity within the existing life-stage boundaries. The existing earlier Neutral Fly/Swim restrictions remain untouched.

## Regions, variants and attachments

The isolated `dynamic/hero_fly065/` route paints Fly-specific white/inherited head and body skin, lavender/purple limbs, cyan/lime edges, a small blocky two-color chest patch, crown regions, wing interiors and the independent halo palette. Five otherwise transparent native atlas texels are scoped to the new crown and wing faces. Original eye/reaction pixels, other transparency and source shading remain protected. Fine gradients and zigzag borders become clean pixel regions rather than extra band geometry.

Named, mono, jewel and shiny coats use the same morphology coordinates and region boundaries while retaining inherited hue and finish. The viewer does not expose those genetic/finish variants; its natural-color references do not verify the mod’s variants. Actual variant/finish inspection and numerical coat checks are reported separately below. Hats, animal parts, toy grips and equipped-player attachments continue through the canonical rig and baby-UV attachment alias.

All previous family models and policies, authority/network/command sources, original texture atlases and dependencies are guarded against the delivered .64 baseline. `ChaoState` changes only its read-only secondary-form labels. Analyzer additions display the new current-bar meters and explanatory text; the actual edit handlers and adult-lock rules are retained.

## Approximations

This remains a blocky first pass. Crown curves and thin filaments become blunt two-section arches and short auxiliary forks. The wings use three solid rays per side instead of smooth feather joins; feet and halo remain square, and the original compact tail replaces the source’s longer curved/forked tail geometry. Paint boundaries are flat pixels with an authored chest patch, rather than source gradients and zigzags. The fixed skull does not reproduce the source’s wedge/rounding. Planar Minecraft eyes can disappear in an exact side view, while the source’s curved face still exposes an eye. Lighting, idle pose and blinking also differ between captures.

The first actual game preview exposed insufficient rearward projection in Fly and Power+Fly. Their existing crown rotations and lengths were corrected; a second actual four-scene, six-angle preview checked half/full maturity after the correction. No head-box or material change was needed. These diagnostic previews are separate from the final frozen comparison cohort.

## Runtime clearance correction

Review of the first complete diagnostic capture exposed wing faces piercing the unchanged head during water motion. That cohort and its accepted static-review records were preserved as historical evidence and excluded from the delivery. A focused failing regression reproduced the collision before the correction. This was a new defect in this family, not an inherited failure.

The final rig lowers and folds this family's wing poses before the existing eight-tick posture transition. The normal pose uses a small lower hinge/fold; Crawl, Swim, Paddle and Glide use authored posture-specific folds. The canonical bone definitions and static surface remain unchanged, and replacement animal wings inherit the same posed joints. Other families bypass this helper. The lower hinge and compact folded fan are deliberate blocky clearance approximations, not behavior inferred from the static viewer.

The focused production test passed 132,131,331 checks, including 48,843 movement/gaze/transition frames, 293,058 complete wing/head separating-axis checks, the same number of ray-root/torso contacts and 2,160 replacement-wing joint matrices. Half-tick posture transitions reached a maximum 0.48216593-pixel step under the unchanged 0.8-pixel limit. A separate actual Minecraft preflight completed all 13 selected scenes with zero skips and 120 original captures. Root and independent reviewers opened all 120 through six labelled sheets; native full water frames were also reopened. No wing faces pierced the head in those reviewed frames, including the largest Fly fan and Phoenix replacement. These stationary water animation checks do not establish autonomous swimming navigation.

## Verification boundaries and test notes

The ordinary Gradle `test` task is disabled in the existing project configuration. The build uses the existing executable `coreTest` harness and the retained Lionfish rig/material/timing tasks; a successful build should not be read as a JUnit run. Counts in those harnesses are assertions and sampled geometry/pixels, not millions of independent gameplay scenarios.

Native tests exercise the real command dispatcher, permission checks, Creative payload handlers, ordinary/combined drives, stat reductions, first-type ties, locked alignment/type, maturity boundaries, NBT round trips and tracked observer data. An entity-NBT reload and in-process observer transfer do not establish a complete dedicated-server restart or a connection from an independent remote player. Earlier-world compatibility is guarded by unchanged serialization and synthetic old-state tests; no authentic historical Hero Fly world was supplied.

The new profile correctly reads current bars rather than legacy `swimFly`/`runPower` fields. Existing tests whose synthetic inputs relied on Hero Fly remaining unscoped were updated to supply matching current bars or to exercise an as-yet-unscoped Hero Power fixture. The actual held-Swim-drive regression now seeds current Swim progress 470/960 before the real 24-point drive crosses its threshold; all original assertions and the original legacy-axis condition remain. A material-inventory check now includes the added 35th gameplay model and still scans every previous UV footprint. These are resolved fixture/oracle updates, not weakened tolerances or removed checks.

The inherited guide-journal checker still reports 8/9: the “Your gardens” paragraph-format expectation fails. The checker and guide item match .64 exactly; this is separate from the successful build/native suites and from the corrected new wing/head collision. Existing glasses edge aliasing and some frontal player-hair/halo overlap remain visible inherited limitations.

Useful existing operator commands are `/chao testtoy list`, `/chao testtoy broom once`, `/chao testtoy picture_book loop`, `/chao testtoy stop`, `/chao testcarry loop` and `/chao testcarry stop`. They retain production clocks, cleanup and no-reward behavior. The actual client evidence distinguishes commands exercised from those merely available. The client uses Linux software OpenGL; it does not verify phone GPU rendering. Online account/skin service fetches were unavailable in this sandbox, so a local fixture player was used. No audio listening pass, physical touchscreen interaction, every toy/accessory combination, continuous shiny-glint playback, independent remote multiplayer, full server restart or complete natural lifetime is claimed.

For tomorrow's in-game test, use matching client/server JARs and external Lionfish API 3.1. Check the nine signed-axis forms at fresh/half/full adult maturity, reverse training through balance while watching the locked Hero/Fly identity, and try your usual hats, animal parts and player equipment. Check normal walking and actual water travel on your phone, Creative maturity dragging, and a real world restart. The captured water tests evaluate animation and geometry while the actor remains in a controlled area.

For an already evolved Hero/Fly Chao locked at **+100** alignment, `/chao ~ 100 Fly 10` selects the nearest and sets current Fly level 10. With Fly still at 10, `/chao "Kite" 100 Swim 20` selects a uniquely named Chao and crosses the later axis to Swim. First Fly identity and biological maturity remain unchanged. Use the Creative maturity bar separately to compare fresh/half/full development. These examples retain the existing command units (main-stat levels 0–99); they do not set or replace an adult's first evolution type. GLIDE clearance is numerically checked; no actual gliding client scene is claimed.

Comparison “age” percentages mean normalized adult development, not calendar years or remaining lifespan. The existing adult Analyzer bar maps 0–100% onto raw evolution strength 0–1.2; morphology uses that value divided by 1.2. Thus 50% is raw 0.6, and 100% is raw 1.2. Child editing uses its existing 0–0.99 cap. These edits update development and its normal checkpoint, leave calendar age/lifespan and life-stage flags intact, and retain ordinary aging afterward. A child already at its calendar evolution boundary may therefore still evolve normally after an edit; dragging does not promise to suspend its lifecycle.

## Completed build and in-game checks

| Check | Actual result |
| --- | --- |
| NeoForge 1.21.1 / Java 21 full build | Successful in 15m 30s; 57 tasks, 51 executed and 6 up-to-date. |
| Core Hero Fly policy | 7,970 assertions passed in the same full-build invocation. |
| Hero Fly rig | 132,131,331 sampled assertions passed, including the new full-volume posed wing-clearance regression. |
| Hero Fly material | 141,910,059 pixel/material assertions across 172 canonical coats passed. |
| Existing rig/material/toy regression tasks | Passed; piano and organ each checked 275,076 hand samples, retaining old configurations and adding 27 Hero Fly configurations. |
| Dedicated native server | 632/632 required tests passed: 54 new and 578 retained, 14 namespaces and 84 actual batches; 57s Gradle invocation, 27.26s test clock. |
| Final normal JAR and real Accessories run preparation | Successful in 10s; final production entries exactly match the broad build. |
| Complete current Minecraft capture invocation | 97/97 scenes, 0 skips, 848 original PNGs saved; successful in 11m 4s. |
| Separate inherited guide-journal checker | 8/9; unchanged “Your gardens” formatting expectation remains a known failure. |

The final cohort contains 480 static six-angle images, 16 actual UI images, 9 post-edit world views and 343 live scene images. It covers all nine Hero/Fly corners at newly evolved/half/mature development, independent training fractions, opposite-axis crossing, mixed age/training, five representative coats/finishes, pumpkin/rabbit/Phoenix/glasses, previous-family regressions, actual walking, water animation, broom and picture-book grips, and equipped carrying with the real optional Accessories/owo runtime. Reviewers personally opened all 480 static panels, with selected native-size reopens; live/UI and comparison review scopes are recorded separately in the bundle. These are real Minecraft captures, not authoring projections.

Actual UI records confirm adult Hero alignment remained read-only, Creative maturity dragging updated through the tested positions, Survival edits were rejected, the child stayed a child during the tested edit sequence, and all three UI scenarios preserved exact entity-NBT reload and life-stage checks. The native rule suite also checked commands, permissions, selection, ordinary/combined drives, reductions, first-evolution ties and tracked-data transfer.

The actual first-evolution scene begins from a deliberately prepared child near its existing evolution boundary. It ran 300 normal game ticks, displayed the real cocoon and newly evolved adult, and saved 25 frames. The result remained locked Hero/Fly with normalized adult maturity `8.487654E-5`, Run/Power 0 and carried-forward Fly/Swim +0.5. This establishes the sampled boundary transition, not an entire naturally elapsed childhood or lifetime. Live opposition separately logged Fly/Swim +0.5, 0, −0.5, −1 while first type remained Fly and adult maturity stayed 1.0.

The installable JAR is `chao_garden-0.25.65.jar`, 31,342,871 bytes, SHA256 `0872eee8eef81807ef0f4e2df7e542fde76835bbdcbcd5e0c96b25ae0d548448`. The development run used declared Gradle output roots, frozen before the complete cohort. All 8,369 production Chao class/asset entries match the final JAR byte-for-byte and still match the live output roots after the run. The immutable post-cohort proof SHA256 is `84432762e5a7f693cfdd74e521c2fc5998a049ba26cf544233ecba20a5084f85`. This is a development-classpath/JAR comparison; `runClient` is not claimed to have loaded the packaged archive.

## Viewer and game comparisons

- [Newly evolved: front, side and back](comparisons/final-matched-age000.png)
- [Intermediate: front, side and back](comparisons/final-matched-age050.png)
- [Mature: front, side and back](comparisons/final-matched-age100.png)
- [Additional Minecraft left sides](comparisons/final-opposite-side-audit.png)
- [Mature above views](comparisons/final-mature-crown-caps.png)

The main sheets contain 81 matched viewer/game pairs and 27 additional Minecraft left-side views. Root personally opened all four completed sheets; the main accepted manifest SHA256 is `31f4253d2c2ab1d49e87787ebcf26b7d12e5fc9fa6c859a0bd502920f44e7a05`. Source images are unchanged; derivatives use fixed crops, padding and uniform nearest scaling, with no flips, recoloring or per-image warping. The reference and game have independent lighting, camera pitch, idle poses and blinking. Root also personally opened all nine mature above pairs; the separate accepted above manifest SHA256 is `830d8beda0f95f8c5f41ad6399fec83fb77d9b593366f2c8193760362b6921be`. Both manifests bind the same final 97-scene cohort, JAR, post-cohort proof and unchanged viewer reference. The above views make the compact blunt treatment of the source’s thin rear Swim extensions particularly clear.

The [103.6-second silent motion recording](hero-fly-0.25.65-motion.mp4) contains the actual gain/loss/mixed/opposition scenes 78–82: 2,072 frames, 20 fps, 1440×900 H.264, with no time rescaling. Root personally opened all 18 decoded six-second samples from 0 through 102s and native full frames at 0s, approximately 57.090671s in the held-pair interval, and 102s. The held decoder time is approximate and separate from the actual rendered `PAIR_SETTLED` tick 280 marker. This is sampled temporal review, not continuous playback or audio listening. Scene/camera cuts are intentional. Movie SHA256 is `138c8b2bd5b50c9cc32f92a8601439c0ec3de6c89aacd1afe4f209b4799c0057`, 57,426,826 bytes.

The final independent material review opened 610 distinct capture paths on 55 sheets and 63 native-size originals, including all 60 water frames and every one of the 21 water back frame. Two common-fresh above images are pixel-identical; both authentic capture records remain. The final live reviewer opened 109 distinct captures, including all 60 water samples, while the UI/evolution reviewer covered 50 distinct images. Current reviews found no blocking skull intrusion or duplicate Phoenix fan. Those sampled reviews do not establish every unsampled animation interval or every possible attachment. See the supporting audits for exact personal-open lists and hashes.
