# Hero / Power-first family — 0.25.67

This update completes a first pass for ordinary Hero-aligned adults whose first evolution is **Power**. It starts from the corrected working 0.25.66 project and retains the completed Neutral, Hero and Dark families.

[NeoForge 1.21.1 JAR](chao_garden-0.25.67.jar) · [Source changes](chao-garden-0.25.67-source-changes.zip) · [Verification results](verification-outcomes.json) · [Viewer findings](VIEWER_REFERENCES_02567.md)

The original **7×6×6 head box, facial layout and UVs remain unchanged**. Three separate crown branches each use a white/inherited root and a colored tip. Together with the wings, four-bar halo and compact body/hand/foot targets, they use the existing closed-cuboid morph system and shared animation rig. The original 59 joints retain their parents, pivots and rotations. No dependency, save field or network message was added.

## Forms and progression

All forms retain Hero alignment and the locked first Power type. Later tendencies use both signed axes, including Run after Power-first evolution.

| Later specialization | Blocky interpretation and natural-coat accents |
| --- | --- |
| Newly evolved | Three short blunt pink/red buds, pink halo, white body and pink/red bib, glove and shoe patches. |
| Balanced | Compact upright orange/gold buds, yellow halo and compact orange/yellow wings. |
| Run | Raised central branch with a long rear-swept cap and lower outward branches; pink/cream/tan patches and pale-yellow halo. |
| Power | Tallest blunt central bulb and broader outer bulbs; red/orange/yellow patches, lime halo and compact green wings. |
| Fly | Raised broad outer lobes and a long thin white rear center blade; lavender/purple patches, blue halo and larger blue/teal wings. |
| Swim | Flared outer fins and a narrow rear center blade; magenta/pink patches, blue halo and smaller teal/gray wings. |
| Run + Fly | Smaller swept branches, muted pink/lavender/gray patches, gray-green halo and olive-tipped wings. |
| Run + Swim | Broader swept branches, stronger pink patches, gray-green halo and compact ochre/green wings. |
| Power + Fly | Slender outward/upward branches, pink/rose patches, green halo and broader green wings. |
| Power + Swim | Broad blunt branches shorter than pure Power, red/pink patches, green halo and compact green/yellow wings. |

The reverse order of a compatible pair shares the same target. Run/Power cancel on one axis; Swim/Fly cancel on the other. Current training totals are `100 × level + progress`; the existing provisional full-influence scale is 960 net progress. Coordinates are `(Power − Run) / 960` and `(Fly − Swim) / 960`, clamped independently. This scale is a mod authoring choice.

Biological maturity independently blends the common fresh form toward the mature targets. The established 30-tick display easing smooths edits without writing to age or stats. Reductions can return a mature Chao to a balanced specialization while its biological maturity remains unchanged. Existing first-evolution selection still uses the highest earned Run/Fly/Power/Swim points, with the most recently increased tied stat winning. Adult alignment and first type stay locked.

## What the viewer actually showed

The Chao Island viewer rendered successfully in Chromium 156 with WebGL2/SwiftShader. Actual browser mouse and keyboard actions selected **Power**, changed Hero alignment, age and both training sliders, and rotated the Chao through front, both sides, back, above and below views. No compatibility warning appeared in the successful session.

A sandbox browser attempt failed before page load with `ERR_CERT_AUTHORITY_INVALID`. A TLS-verifying session through the inherited proxy/CA setup worked without a certificate bypass. An early click on the Power text left Child selected; that attempt was caught visually and excluded. The admitted reference set contains **186 original canvases**, with selected-type and slider checks and 21 personally reviewed sheets.

The research covers all nine mature targets, fresh and intermediate ages, independent training quarters at mature age, and alignment context. The long narrow center blade in Fly/Swim is especially clear from above. The older imported `chao_hero_power.geo.json`, `hero_power.png` and converted official model supplied construction context; the existing gameplay baby supplied the fixed head and rig. Viewer geometry and morph formulas were not imported.

## Viewer and game comparisons

Each pair uses unchanged originals with fixed crops and uniform scaling. The camera angles and normalized age/training settings are practical matches; lighting, idle poses and projection differ. Measured intermediate viewer slider error reaches about 3.03 percentage points, so the independent-training comparisons are approximate.

- [Newly evolved: front, side and back](comparisons/final-matched-age000.png)
- [Half developed: front, side and back](comparisons/final-matched-age050.png)
- [Mature: front, side and back](comparisons/final-matched-age100.png)
- [Opposite game side at all three ages](comparisons/final-opposite-side-audit.png)
- [Mature crown and rear-blade views from above](comparisons/final-mature-crown-caps.png)
- [Independent training at full maturity](comparisons/final-independent-training.png)

The first preview showed overly flush outer crown lobes. The final revision raises the Fly/Swim lobes, strengthens Power bulbs and swept Run branches, and modestly increases fresh/balanced tip exposure. It changes only the six new crown sections; the original head, UVs, corrected wrist anchoring, wings and halo targets stay fixed.

[103.65-second gain, loss, mixed and Power-to-Run recording](hero-power-0.25.67-motion.mp4) · [27.55-second Fly-to-Swim recording](hero-power-0.25.67-second-opposition-motion.mp4)

Both recordings are silent, at nominal 20 fps, with the native 20-TPS game clock and no time rescaling. Training schedules are accelerated test edits. Root personally reviewed 18 primary and seven secondary decoded samples plus two full-size decoded frames. Additional source screenshots cover walking, running, controlled swimming, toys, carrying and evolution. This is sampled visual inspection; full continuous playback and audio listening were not verified.

## Verification

| Check | Actual result |
| --- | --- |
| Full build | **Passed**, 17m 9s; 61 tasks, 55 executed and six up-to-date. Includes current and retained core, rig, material, animation, posture, toy and instrument checks. |
| New core policy | **8,745 checks passed**, including signed axes, ties, current stat-change routes, immutable identity and serialization. |
| Geometry/material | Passed production-emitter tests and 172 canonical coats; original head/face, compact limbs, crown/wing contacts, attachment matrices, halo clearance and regional pixels checked. Static validator passed 9,773 blends and 7,388,388 rotated face-pair checks. |
| Dedicated native server | **745/745 required tests passed**, comprising 57 new and 688 retained cases across 16 namespaces. Includes ordinary/combined/held drives, reductions, command selection/permissions, Analyzer payloads, NBT and tracked-observer data. |
| Actual client | **102/102 scenes**, zero skipped; **918 original PNGs**: 504 static, 16 UI, nine post-UI world and 389 motion/evolution captures. All logged capture hashes checked. |
| Runtime/JAR binding | All **8,391 production classes/assets** match the pre-run frozen development outputs and final JAR. All 7,796 prior asset entries remain byte-identical. |
| Earlier families | Retained policies, 168 prior native test sources and 12 protected family models preserved; completed-family and baby regression scenes included. |

The broad build also supplies the focused checks; these are one invocation in the final outcomes. The project's ordinary Gradle `test` task remains disabled; its executable `coreTest` and Lionfish tasks ran. Game tests run on a dedicated server. NBT round trips and tracked observers establish serialization/data behavior, while independent remote-client transport and full world shutdown/restart remain unverified.

The team visually inspected all 504 static game originals across the core stages, independent training, finishes, attachments, earlier families and babies. Motion, evolution and UI were reviewed through selected frames; hash checks cover all 918 originals.

The actual client reported `BUILD SUCCESSFUL in 11m 50s`, completed all capture IO, and its launcher, daemon and client processes terminated. A platform readiness update discarded the old execution-session receipt; no tool exit code is inferred. The immutable runtime proof was bound afterward. The client used development outputs checked against the JAR; this does not assert that it loaded the packaged archive.

The actual first-evolution scene progressed from a near-boundary child through its cocoon into a compact fresh Hero/Power adult. The mixed Run/Fly scene reached its full displayed endpoint, held it, then returned to balance. Both opposing-axis scenes crossed through +50%, zero, −50% and −100% while first type stayed Power and maturity stayed full.

The live Analyzer checks retained adult alignment locking, responsive Creative maturity edits, Survival denial and baby alignment/maturity controls. Actual toy grips and real Accessories carrying were exercised. The carried Chao is obscured by the player from the front; side and rear views establish its visible attachment. Controlled water poses were inspected; autonomous swim travel was not established.

## Resolved issues, approximations and remaining limits

A strict walking/downward-gaze check found a small fist/head intersection. Keeping fist growth below the original wrist plane removed it while preserving the original head and shoulder/wrist contacts. A new UV test oracle used the wrong canonical vertex IDs and was corrected without changing the good UV data. Four old fallback witnesses were moved to still-legacy Dark/Power because Hero/Power is now implemented; their assertions and stat operations were retained. The final build and native run have no remaining failure.

The separate inherited journal checker remains **8/9**, failing the existing `Your gardens` paragraph separator. Its checker and content are unchanged. A prior 0.25.66 report-only video frame-count discrepancy is documented separately; the old source and artifacts remain untouched.

The smooth reference's wedge skull, bulb ends, tapered fins and wing contours remain blocky approximations. The tail retains its original geometry with regional paint. Wings use a lower, closed posture to clear the fixed head. Source interpolation is nonlinear; the mod's intermediate timing and blends approximate its progression. Source and game idle gestures can differ in otherwise matching views.

Color/shiny/jewel inputs share the same morphology. The viewer cannot validate those finishes; mod pixel tests and representative game views cover them. Existing jewel reflections can lighten a pupil, and glasses rims retain their stippled appearance. Exhaustive coat/animation/accessory combinations, phone hardware/touch, shaders, audio, full natural lifetime, multiplayer latency/reconnect and a real old Hero/Power world restart still need in-game checking.

## Delivery and suggested checks

Use matching client/server JARs with Minecraft 1.21.1, NeoForge 21.1.219, Java 21 and external Lionfish API 3.1. The source ZIP is a cumulative overlay onto the complete 0.25.43 source, retaining the corrected local 0.25.66 work. Its guarded deletion instructions preserve modified source and do not touch saves. There is no assumed published 0.25.66 Git parent.

For an already evolved Hero/Power Chao, these existing Creative/admin commands make later Run/Fly visible while keeping its first identity:

```mcfunction
/chao ~ 100 Power 0
/chao ~ 100 Run 10
/chao ~ 100 Swim 0
/chao ~ 100 Fly 10
```

Quoted names also work, for example `/chao "Tempo" 100 Run 10`. Adult commands that request a conflicting alignment are rejected. Use the Analyzer maturity control to compare fresh and mature appearance; it does not reset the adult's lifespan or first evolution.

Please check the forms on your phone, reload an existing world, cross both axes and reverse them, then try your preferred shiny/jewel, hat, animal-part and equipped combinations with another player. Existing operator previews remain available:

```mcfunction
/chao testtoy list
/chao testtoy broom once
/chao testtoy picture_book loop
/chao testtoy stop
/chao testcarry loop
/chao testcarry stop
```

These use the production preview/cleanup paths and grant no rewards. Listening, actual flight travel and independent multiplayer remain manual checks.

JAR SHA256: `12dbed906198e29d47febcf9a7f6a03a97e5594b55ce9cd8336d646a473755a7`.
