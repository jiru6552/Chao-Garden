# Dark / Swim-first family — 0.25.68

This update builds on completed 0.25.67 and adds an initial full family for ordinary Dark-aligned adults whose first evolution is **Swim**. It preserves the completed Neutral, Hero and Dark families.

[NeoForge 1.21.1 JAR](chao_garden-0.25.68.jar) · [Source changes](chao-garden-0.25.68-source-changes.zip) · [Verification results](verification-outcomes.json) · [Viewer findings](VIEWER_REFERENCES_02568.md)

The original **7×6×6 head box, facial art and UV layout remain unchanged**. The existing 59 animation joints retain their parents, pivots and authored rotations. Separate closed cuboids form an upper rear hook, a broader lower hood and two lateral spurs. Compact body, hand and foot targets, paired three-ray wings, a two-section tail and a six-point Dark marker use the existing renderer. No library, save field or packet was added.

## Family and progression

All nine later targets keep the locked first Swim identity and Dark alignment. Each compatible pair is one target regardless of order.

| Later specialization | Blocky interpretation and natural-coat accents |
| --- | --- |
| Newly evolved | Two short rear structures, olive body, bright green lower muzzle/chest, turquoise-green cuffs and tail, lime tips and cyan spiked marker. |
| Balanced | Two rearward hooks, dark teal body, blue/teal feature bands, yellow marker and small pale wings. |
| Run | Both rear hooks rise steeply; blue accents, wings and marker. |
| Power | Thicker upright kinked upper hook and taller side spurs; green accents and turquoise marker. |
| Fly | Flatter rear hooks, lateral upturned spurs and the largest wing fan; blue body accents with mature pink/lavender wings and marker. |
| Swim | Longest descending lower hood, shallower than the smooth reference to clear the rig; olive/teal body, mint/lime bands and yellow marker/wings. |
| Run + Fly | Rising hooks and smaller side spurs; blue/lavender accents. |
| Run + Swim | Shorter hood and compact upper hook; teal accents and muted marker/wings. |
| Power + Fly | Tall kink and paired spurs; green/teal accents and muted teal marker/wings. |
| Power + Swim | Descending hood and paired spurs; green/mint accents and green marker/wings. |

Current training totals are `100 × level + progress`. The existing provisional full-influence threshold is 960 net progress: `(Power − Run) / 960` and `(Fly − Swim) / 960`, clamped independently to −1…+1. Swim-first adults can therefore cross balance toward Fly, and either Run or Power can develop on the other axis. These scales and interpolation rules are mod authoring choices, not extracted viewer formulas.

Biological maturity independently blends the common newly evolved form toward the mature targets. Thirty-tick display easing smooths stat and Creative edits. Training reductions change specialization without changing chronological age, adult maturity or first identity. The highest earned Run/Fly/Power/Swim points still choose the first evolution, with the most recently increased tied stat winning. Adult alignment and first type remain locked.

## Independent viewer inspection

The viewer rendered in Chromium 156 with WebGL2/SwiftShader. Actual input selected the yellow **Swim** first-type square, full Dark alignment, age and both training axes, then rotated the model through front, both sides, back, above and below. The successful session showed no compatibility warning. An earlier sandbox launch failed before page load with `ERR_CERT_AUTHORITY_INVALID`; the inherited proxy/CA configuration worked without bypassing certificate verification.

The frozen reference set contains **205 original canvases**, including 186 family settings and 19 context/proof views. All nine forms were inspected at five ages; mature forms also have six-angle coverage and independent training-quarter views. Nineteen contact sheets were personally reviewed. Pixel measurements verified the selected Swim square and 744 slider handles; the largest measured deviation from the requested normalized setting was approximately **0.493 percentage points**.

The source has a wedge-shaped skull with two rear extensions, target-dependent lateral spurs, a colored lower muzzle, batlike wings, a thin tail and a spiked floating ball. Our unchanged square skull means that the surrounding cuboids approximate the hood silhouette. The viewer's training response can saturate early: some Run/Swim images were identical at 50%, 75% and full training despite different control positions. The mod retains smooth, reversible training blends instead of inferring the source's internal logic. No viewer geometry or morph formulas were imported.

The existing gameplay baby supplied the fixed head and shared rig. I also inspected the older imported `chao_dark_swim.geo.json`, its `dark_swim.png` atlas and the converted official JSON: its two cuboid crest roots and short end pieces provide construction context. That was a source/texture inspection, not an in-game visual check of the old asset; its geometry was left unchanged.

## Viewer and in-game comparisons

All nine targets were compared with the independently operated viewer at newly evolved, 50% and mature development from front, side and back: **81 paired views**. Additional sheets show 27 opposite-side game views, nine mature top-view pairs and 24 independent training-quarter pairs at fixed maturity. Both reviewers personally opened all six final sheets. Matching settings and camera directions are approximate; the source and Minecraft keep their own lighting and proportions. Some static game fixtures retain an idle rigid lean or crossed arms.

The team also inspected every static fixture from six angles: **85 cases / 510 body crops**, including five coats/finishes and retained families, hats and animal parts. Four full Analyzer originals and 66 selected motion/attachment stills were reviewed separately. The player hides the carried Chao from the front; left/back show the visible attachment. Cocoon images hide most of first-evolution emergence. These are actual Minecraft captures, not authoring projections.

- [Newly evolved: front, side and back](comparisons/final-matched-age000.png)
- [Half developed: front, side and back](comparisons/final-matched-age050.png)
- [Mature: front, side and back](comparisons/final-matched-age100.png)
- [Opposite game side at all three ages](comparisons/final-opposite-side-audit.png)
- [Mature head details from above](comparisons/final-mature-head-details.png)
- [Independent training at full maturity](comparisons/final-independent-training.png)

## Motion recordings

These are silent recordings of the actual Minecraft window at normal 20 TPS, with no time rescaling. The review used decoded samples and separate saved frames; it does not claim continuous playback or audio listening.

- [103.10-second training and Run/Power crossing recording](dark-swim-0.25.68-motion.mp4), 2,062 frames at nominal 20 fps; includes gains, a held mixed endpoint, reversal and a fixed-age Power-to-Run crossing.
- [28.05-second Swim/Fly crossing recording](dark-swim-0.25.68-second-opposition-motion.mp4), 561 frames at nominal 20 fps; first Swim and biological maturity remain fixed.
- [122.20-second production-pose recording](dark-swim-0.25.68-production-poses-motion.mp4), 2,443 frames at nominal 20 fps; crawl, paddle, swim, glide and flail one-shot/loop/stop sessions, including the widest Fly fan and longest Swim hood.

Root personally reviewed 18, seven and 31 regular decoded samples from the respective recordings, plus 36 dense samples across four crawl entry/exit windows. The latter use approximate timing from actual ARMED epochs at 20 TPS; exact switch events are not present in the video log. No new eye, texture or detached-feature issue was observed in those samples.

Frame counts come from the actual encoded streams. Each recorder ended with intentional SIGINT (`ffmpeg` exit 255) after its selected scenes; all three files decoded successfully.

## Verification

| Check | Actual result |
| --- | --- |
| Full build | **Passed**, 19m 9s; 63 actionable tasks, 57 executed and six up-to-date. Current and retained morphology, posture, toy and instrument checks ran. |
| New core policy | **8,878 checks passed**, including both signed axes, current-stat routes, evolution selection/ties, immutable identity and serialization. |
| Geometry/material | Strengthened production-emitter suite passed **1,070,755,362 numerical checks**, including active gait phases, posture entry/exit, full head-feature/wing/limb/tail clearance, attachment matrices and retained families. Material checks passed for **172 canonical coats**, including exact face pixels, independent marker colors, preserved alpha and opaque new cuboids. |
| Dedicated native server | **880/880 required tests passed**: 58 new morphology cases, 77 new pose-preview cases and 745 retained cases. The run enabled 17 namespaces; generated cases use their shared fixture namespace. |
| Actual client | **109/109 scenes passed**, zero skips; **1,122 unchanged original PNGs**. Includes 85 six-angle static cases, three Analyzer scenes and 21 motion/transition scenes, including six operator-pose scenarios. |
| Runtime/JAR binding | All **8,402 production entries** match the outputs frozen before the successful client run, the live development outputs and the final JAR. The JAR has 9,100 unique CRC-valid entries. `runClient` used the development classpath; it is not claimed to have loaded the packaged archive. |
| Earlier assets | All **7,798 prior asset entries** remain byte-identical. The original head/face, 59 joint definitions and 13 prior gameplay models are unchanged. |

The full build includes the focused checks; preliminary focused runs remain separate history. The project’s standard Gradle `test` task is disabled, while its executable core/Lionfish checks ran. The static validator passed 9,517 age/axis blends and 7,194,852 rotated face-pair checks.

Native tests run on a dedicated server. Entity NBT round trips and tracked-observer assignment verify serialization/data behavior; a server-constructed observer is not an independent remote multiplayer client or a whole-world shutdown/restart.

## Resolved issues and limits

Strict production-emitter tests found a wing/skull intersection in a downward-looking swimming pose and several long-hood collisions with the new tail, body, feet and wings. The correction is confined to this family: a lower folded prone wing posture, shallower Swim/Power+Swim hood targets and a prone fold of the new tail sections around their existing root. The original head, tail base and joints remain fixed. Initial failures and refinement evidence are retained separately from final results.

Actual side/back comparisons exposed overly small Fly wings, so the Fly, Run+Fly and Power+Fly fans were widened. Stronger moving-gait tests then found crawl and dash contacts. A compact crawl fold and a short tuck through the existing eight-tick posture transition clear those paths; the new tail also counters forward dash lean. The upright fans retain their full authored span. These are scoped pose corrections around the fixed head and existing attachment joints.

Five new native preview fixtures initially failed when restarting immediately after one-shot playback: the stationary Chao’s vanilla ground flag was false despite unchanged position on a solid floor. The fixtures now settle through a real downward block-collision move and repeat their exact state/items/AI assertions. The production ground guard was left intact. An isolated namespace invocation ran no tests and was excluded; the final complete suite reports the actual 880 passes.

The first full visual-helper launch stopped before any catalog or PNG because its evolution fixture read server config at the title screen. Catalog creation now waits for player join/server readiness. A targeted actual run then completed natural first evolution and saved 25 images. The failed attempt and its snapshot/log/contract were archived; this development helper is excluded from the unchanged production JAR.

A strict evidence check initially rejected one pose-capture timestamp: the helper snapshotted tick 203 for a PNG, then logged tick 204 after the shared server clock advanced. All 198 pose events were matched one-to-one to their unchanged original PNG callbacks by order, angle, path, hash and time; only that literal pair differs. The private parser correction passed 16 rejection checks. No image, log, fixture or production byte was changed to resolve this accounting issue.

The inherited standalone journal checker still passes **8/9**, failing the existing `Your gardens` paragraph separator. Its checker and content remain unchanged.

The smooth source skull wedge, tapering hood, curved fins, thin tail, spiked sphere and wing membranes are simplified into closed cuboids and flat pixel regions. Color/shiny/jewel variants share the same geometry; the viewer cannot validate those finishes. Phone hardware/touch, shader packs, audio, every accessory/toy combination, full natural lifetime, independent remote multiplayer/reconnect and a real old-world restart require further in-game checking.

## Delivery and suggested checks

Use matching client/server JARs with Minecraft 1.21.1, NeoForge 21.1.219, Java 21 and external Lionfish API 3.1. The source ZIP is a cumulative overlay onto complete 0.25.43 source, retaining completed local 0.25.67. Its guarded deletion instructions preserve modified source and do not touch saves. No published 0.25.67 Git parent is assumed.

For an already evolved, fully Dark (−100) / Swim Chao, these existing Creative/admin commands develop later Run+Fly without changing its first type:

```mcfunction
/chao ~ -100 Power 0
/chao ~ -100 Run 10
/chao ~ -100 Swim 0
/chao ~ -100 Fly 10
```

Quoted names also work, for example `/chao "Ripple" -100 Fly 10`. Use the existing locked alignment value for a partly Dark adult; requests for a different adult alignment are rejected. Use the Analyzer maturity control to compare newly evolved and mature appearances; it does not reset lifespan or first evolution.

Please reload an existing world, reverse both training axes, and try your preferred shiny/jewel, hat, animal-part and carried combinations on your phone and with another player. Existing operator previews remain available:

```mcfunction
/chao testtoy list
/chao testtoy broom once
/chao testtoy picture_book loop
/chao testtoy stop
/chao testpose list
/chao testpose swim once
/chao testpose crawl loop
/chao testpose crawl stop
/chao testpose stop
/chao testcarry loop
/chao testcarry stop
```

The new operator pose adapters also include paddle, glide and flail. They reuse the production rig poses and the existing synced 160-tick preview clock, with standing entry/exit. Care and age pause only during the temporary session; no real props or rewards are created. The existing cleanup restores the original AI flag and ends playback on interaction, damage, unload or keeper disconnect. These are dry-ground pose previews; water particles and autonomous flight/swimming require their actual environments. Ordinary feeding, care, age, rewards and toy cleanup retain their established rules.
