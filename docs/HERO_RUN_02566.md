# Complete Hero Run-first morphology — 0.25.66

This update completes **Hero / Run-first / ~**: balanced later development, Run, Power, Fly, Swim, Run+Fly, Run+Swim, Power+Fly and Power+Swim. Each compatible pair has one target regardless of order. Later Power is valid even when the first evolution was Run. “Balanced” describes later training, not Neutral alignment or a new first type.

## Existing prototype and baseline

The starting point is the finished local 0.25.65 project and its accepted JAR/source bundle. A complete source snapshot was frozen before this work; the old artifacts and evidence remain unchanged. No published 0.25.65 Git branch is assumed.

The approved 052/055 prototype already has the original 7×6×6 head, clean cuboid stalk/fan construction, continuous baby Run/alignment development, separate Neutral baby front-side nubs, canonical animation joints and attachment behavior. Its adult training scalar clamps Run−Power to 0..1, and its compatible adult shape/color deltas are limited. It cannot represent a distinct later Power endpoint or the full current-bar signed nine-corner family.

This pass keeps that baby policy and model asset unchanged. It adds a scoped adult surface and reuses the existing rigid-box target evaluation and animation renderer, rather than introducing a new library or general-purpose morphology API. Historical 055 adult geometry remains a retained construction/test reference; production Hero/Run adults now use the complete 066 family.

## Independent viewer inspection

A fresh Chao Island session rendered in Chromium 156 with WebGL 2/ANGLE SwiftShader. The actual green first-Run checkbox, full Hero alignment, age and both later-training sliders were operated. Mouse orbit/R-reset established front, both sides, back, above and below. The first-Run checkbox stayed selected when later training crossed to Power. No compatibility warning or Continue prompt appeared. The initial sandbox browser failed before page load with `net::ERR_CERT_AUTHORITY_INVALID`; the same TLS-verifying setup worked through inherited proxy/CA settings in the outer executor. No TLS bypass was used. Desktop-control tools were unavailable, so installed Playwright operated the actual viewer canvas.

The immutable reference contains 186 original canvases: 150 family views and 36 control/context views, with 548 actual actions. All 21 review sheets and 36 primary full-size originals were personally opened by the reference reviewer. An independent pixel audit verified 600 slider-handle readings and 150 selected-Run patches, with maximum normalized control error 0.03028 under 0.04. No JavaScript page error occurred; the WebGL context stayed healthy. Manifest SHA256 is `b3ddf207bf7aa1cff29b712a23b858fd261beee0b025fbb7588ff6051518d7fa`.

The source was compared at adult development 0/25/50/75/100%, at training quarters with mature age fixed, at the nine signed endpoints, and in separate alignment context views. These are normalized control positions and approximate camera angles, not recovered source-game stat units or calibrated biological timing.

| Later influence | Observed reference and blocky interpretation |
| --- | --- |
| Fresh | One short striped central horn; yellow halo, yellow/orange wings, blue/purple belly and cyan/white/yellow limb bands. |
| Balanced | A long narrow rearward/downward hook; lime halo, compact yellow/orange wings and blue/purple belly. |
| Run | Three long rearward/down prongs share one raised stem; pale-yellow halo, blue/cyan/white bands, dark-teal wings and yellow/cyan belly. |
| Power | Thin upright stalk ending in a blunt bulb; orange halo, pale-gold/lavender bands, orange-gold wings and gold belly. |
| Fly | Broad two-lobed V/fishtail cup above a narrow stalk; blue halo, pink/lavender bands, larger teal/pink wings and pink belly. |
| Swim | Rearward three-prong fan with downward curl; lime halo, purple/white/green bands, green wings and yellow/mint belly. |
| Run+Fly | Smaller blue/periwinkle cup with a lower tier of points on the crown stem; gray halo, steel-blue wings and peach/gray belly. |
| Run+Swim | Strong rearward fan, blue/white/green bands, yellow-lime halo and green wings. |
| Power+Fly | Smaller pink flared cup; muted-pink halo, pink/lavender/pale-gold bands and salmon/gray-green wings. |
| Power+Swim | Compact mini-fan with a blunt center and short lateral points; gold halo, lilac/white/green bands and lime wings. |

The full-Hero adult source does not show the Neutral form’s separate outside-eye brow nubs. An actual first-Run/half-age alignment comparison shows nubs on Neutral and none on Hero. The small extra Run+Fly points belong on the crown stem. The approved baby nubs and other alignment families therefore remain separately scoped; they were not silently removed or attached permanently to every Hero adult.

Source progression is nonlinear: a full-Run fan can stay short at half adult age, while half training at mature age produces a different shape, and compatible half/half influences can already resemble their paired endpoint. The mod’s common fresh target, authored maturity blend and bilinear training are explicit approximations. Source geometry, textures and morph formulas were not imported.

## Construction and rules

The adult morph targets drive 21 closed cuboids within the inherited mesh: six stalk/fan/cap pieces, six wing plates, four rigid halo bars and five compact body/limb targets. The original head box and its face remain unchanged, as do all 59 canonical joint definitions. Three mesh-only halo children bring the asset to 62 bones. Unused branch pieces stay positively sized inside connected parts and emerge gradually. A small 2.5-degree cap twist avoids coincident painted side planes during intermediate blends. All eleven prior relevant gameplay model assets remain byte-exact.

The new profile admits ordinary living Hero adults with locked Run first type (`RUN_TYPE=3`). It reads current bars `100×level+progress`, bounded to 9900. The net `(Power−Run)/960` and `(Fly−Swim)/960` coordinates each clamp to−1..1. The 960-point scale is existing provisional mod pacing, not a formula recovered from the viewer. Each axis can cancel and cross balance; compatible axes combine, while opposing influences do not create duplicate pair forms.

Normalized adult development is independent of those current training coordinates. Thirty-tick display easing smooths large edits without rewriting biology, stats or saves. The existing adult Analyzer bar maps 0–100% onto raw evolution strength 0–1.2; a half-developed fixture uses raw 0.6. Calendar age, adult lifetime and first identity are separate. Deliberate Creative maturity scrubbing remains within the existing stage; ordinary aging resumes afterward. A child already at its calendar evolution boundary may still evolve normally after an edit.

First evolution still selects the highest earned Run/Fly/Power/Swim points, with the most recently actually increased tied ability winning. Stamina and hidden stats do not compete. Adult alignment and first type remain locked. Later training, decreases, ordinary/combined drives, commands and Creative edits use the same current-bar inputs. Existing legacy fields, serialization, authority, command permissions and network payloads are retained. No save field, migration or dependency was added.

## Texture regions, wings and attachments

The new isolated material route paints the Run bands, chest, tips, wings and independent halo palette without painting stripes onto the original forehead. Four formerly transparent native texels form a white/primary/white/secondary strip; the four longitudinal faces of each head cuboid map along it. Terminal faces use separate cap swatches. Together with five surface swatches, this is a scoped nine-texel alpha exception. The original atlases, face pixels, other transparency and source shading are protected. Bands remain painted pixels, not extra model geometry.

The canonical tail retains its geometry and receives the observed chest rim/core colors. Named, mono, jewel and shiny coats share identical morphology coordinates and regions while retaining inherited pigment and finish. The viewer cannot verify genetic colors or shininess; the mod’s own numerical and actual-client checks are reported separately.

Run-first source wings are generally small leaf/oval pads rather than the broad Hero/Fly-first fans. They use the shared joints with scoped posture offsets. Private strict wing candidates showed that 0.6 px/.15 rad and 0.8 px/.2 rad upright offsets could intersect the unchanged head at extreme running/gaze samples; the smallest tested passing candidate is 1.0 px/.25 rad. Prone poses use their own fold and the existing eight-tick transition. Old 065 poses remain unchanged, and animal replacement wings inherit the same posed anchors. The blocky lower/folded placement remains an approximation to the source’s higher soft leaf shapes.

Eyes, hats, animal parts, toy grips and equipped-player attachments continue through the existing canonical rig/UV aliases. Actual representative runtime views are necessary to verify the new visible placement; numerical checks alone do not establish visual quality.

## Corrected fixtures and validation

The first actual focused invocation passed the complete core harness/new 8,607 policy assertions and 145,924,513 material assertions, but a new baby parity test initially compared different marker-decoration visibility. Matching the ordinary visibility and using the unchanged child-growth setup restored exact geometry/UV equality for all 27 baby fixtures. A second new oracle had copied paired-crown mirror assumptions onto the intentionally twisted central stalk. It now uses an independently authored cap-center expectation at the same precision; the root, head and paired fans retain exact symmetry/placement checks. No old SAT/clearance/UV/transition tolerance was relaxed.

The retained 055 adult test explicitly pins its historical resource through the existing test-only loader after the live adult route advances to 066. Every old shape/UV/attachment assertion remains; current baby selection is still verified as 055 and the new suite independently verifies production 066. The old 062 UV-footprint inventory now counts 36 and includes 066 without a new exclusion. These are bounded fixture/oracle updates, not production asset overrides.

The corrected actual rig task passed 134,788,589 sampled assertions, including all 27 current baby fixtures, 48,843 wing/head clearance frames, 293,058 full-volume wing/head pairs and the same number of wing-root contacts, 2,160 animal-wing matrices and 9,963 half-tick posture transition frames. Maximum half-tick step is 0.30460492 px under the unchanged 0.8 px limit. The retained 055 and completed 065 suites passed, and piano/organ each passed 289,656 hand samples with all previous configurations plus 27 new 066 configurations. These counts represent sampled assertions, not millions of independent gameplay scenarios.

The first dedicated invocation completed 688 cases with three failed assertions. Two historical 055 compatible-pair fixtures set only legacy axis fields; the new read-only adult labels correctly use current training bars, where their Swim/Fly total was zero. The fixture now supplies the corresponding 720-point bar and restores its exact original raw axes, retaining every assertion, Run 960, age and biological strength. Of 167 prior native sources, 166 remain exact and this one has only the documented setup correction. The new baby-sync fixture included a byte-identical default baby; Minecraft legitimately omits that unchanged tracked-data default from nondefault updates. Adding an unrelated saved sentinel makes the existing exact payload/observer/NBT assertions meaningful without changing production synchronization. A copied new-test failure message was corrected from first Fly to first Run; its condition was unchanged. The failed native log remains preserved.

## Completed build and actual-client results

| Check | Actual result |
| --- | --- |
| NeoForge 1.21.1 / Java 21 full build | Successful in 16m 23s; 59 actionable tasks, 53 executed and 6 up-to-date; no failed task. |
| New Hero Run core policy |8,607 assertions passed in the full build. |
| New actual rig / material suites |134,788,589 /145,924,513 sampled assertions passed. |
| Existing family, animation and toy tasks | Passed; piano and organ each checked 289,656 hand samples with retained configurations plus 27 new 066 configurations. |
| Dedicated native server rerun |688/688 required passed:632 retained+56 new;15 namespaces,91 actual batches; 55s Gradle invocation and 26.89s test clock. |
| Normal JAR and real Accessories preparation | Successful in 10s; final archive and all 8,380 production entries exactly match the broad-build artifact. |
| Complete current Minecraft invocation |101/101 scenes,0 skips,911 original PNGs; successful in 11m 46s. |
| Separate inherited journal checker |8/9; unchanged “Your gardens” paragraph-format expectation fails. |

The native-only fixture corrections followed the broad build and were recompiled and exercised by the complete successful native rerun. Their classes are excluded from the installable JAR; the subsequent exact production/archive comparison confirms they changed no delivered production bytes. There is no claim that the original failed native invocation passed.

The final cohort contains 498 static six-angle images,16 actual Analyzer images,9 post-edit world views and 388 live images. The model reviewer personally opened all 498 static and all 59 water panels, plus 25 native originals (564 distinct final images). The material reviewer covered 690 distinct originals on 77 sheets and 15 native reopens. The live reviewer covered 124 distinct originals and 35 full reopens, including every water image. The UI/policy reviewer covered 65 distinct originals and 34 full reopens. These scopes overlap; their totals must not be added as unique combined coverage. Exact open lists and hashes are in the supporting audits.

All nine corners have fresh/half/mature comparisons, independent training quarters, mixed age/training, opposite-axis crossing and representative coats. Pumpkin/rabbit/Phoenix/glasses, broom/book grips and real Accessories-equipped Creative flying-player poses were inspected. The player completely obscures the equipped Chao in the front view; only side/back morphology is established there. Normal baby97 is captured during an ordinary closed-eye blink; Hero98 and Neutral100 have intact open eyes, and Neutral100 retains the separate two nubs. All three water scenes were reviewed: 20 RunSwim frames, 20 largest-Fly-wing frames and 19 Phoenix replacement-wing frames. They establish actual water animation in a controlled area, not autonomous swimming travel.

Actual UI callbacks show the adult alignment lock and explanatory tap feedback, responsive adult maturity 80→52.5→25→100%, Survival rejection, and editable baby alignment with maturity capped at 99%. Entity NBT/identity checks passed after those interactions. The actual first-evolution scene starts 12 ticks before the normal boundary and runs 300 native ticks; it shows the real cocoon and a short fresh adult stalk/halo. Completion is locked Hero/Run with normalized maturity 8.487654E−5, Run/Power−0.5 and Swim/Fly 0. The camera is at the back during emergence, so that scene does not establish an exact-emergence front view or a whole naturally elapsed childhood.

The paired Run+Fly sequence reached its raw peak at 240, displayed its settled endpoint at 280, held it before reversing and returned both axes to balance. Live Run→Power raw coordinates were−0.5,0,+0.5,+1; Fly→Swim coordinates were+0.5,0,−0.5,−1. Both retain first Run (3) and full maturity 1 while chronological age advances. Display easing intentionally lags rapidly changing server targets.

Strict admission found one expected-metadata typo after the run: the pre-run plan called the existing Neutral/Fly regression case 65 first Run (3), but the unchanged pre-run helper explicitly supplies first Fly (2), and its actual saved state and NeutralFly061 renderer correctly agree. The separately accepted **post-run v3** changes exactly that one oracle field; every other 101-case identity/action/angle/fixture field matches. The original pre-run v2 and all runtime outputs remain unchanged. V3 is not presented as a newly pre-authored plan. Its exact delta, hash bindings and post-run timeline are included in the delivery.

## Viewer and game comparisons

- [Newly evolved: front, side and back](comparisons/final-matched-age000.png)
- [Intermediate: front, side and back](comparisons/final-matched-age050.png)
- [Mature: front, side and back](comparisons/final-matched-age100.png)
- [Additional Minecraft left sides](comparisons/final-opposite-side-audit.png)
- [Mature above views](comparisons/final-mature-crown-caps.png)

Root personally opened all four main/left sheets: 81 matched viewer/game pairs and 27 additional Minecraft left views. The accepted main manifest SHA256 is `e08c0a7ab207377d9bdbffa0d836ca75f563756451d7942089f3227152fd03df`. Root also opened all nine mature above pairs; their accepted manifest SHA256 is `692031de142c1f50e1a4d061fffd8e2a63707c721bfbd363f1d452579798712b`. Originals are unchanged; derivatives use fixed crops, padding and uniform nearest scaling, without flips, recoloring or per-image warping. Source/game lighting, camera pitch, idle poses and blinking differ. These are actual Minecraft views, not authoring projections.

The [103.15-second primary motion recording](hero-run-0.25.66-motion.mp4) covers gain/loss, held pair/reversal and Run→Power scenes 78–82: 2,063 frames at 20 fps, 1440×900 H.264, silent and without time rescaling. Root opened all 18 decoded six-second samples 0..102s and four native frames, including the held pair at approximately 56.688s and final Power endpoint 102s. The 102.8s native sample is empty floor after intentional scene cleanup; the unchanged movie retains that closing cut. Its SHA256 is `4e73b435bd6f668cf41b9a954e4b20ab0c305911ae6cbc6d9b3e9bc64c03dd08`.

A separate [28.1-second Fly→Swim recording](hero-run-0.25.66-fly-to-swim.mp4) covers scene 99: 561 frames at nominal 20 fps. Root opened all five six-second samples 0..24s and native frames 0/12/27s. The 12s frame is an ongoing crossing, not a settled-balance hold. SHA256 is `5b487ec34ea726e0d9640cd38797bb477f8f85f02b87d8ee5274c23374dd34a3`. This is sampled temporal review, not continuous full playback or audio listening.

## Artifact, remaining limits and in-game follow-up

The updated JAR is `chao_garden-0.25.66.jar`, 31,373,369 bytes, SHA256 `a33c685b9c86daf5bbbf5409a6ac16a0503723b7a52c34fb0f36227a5bea19e4`. All 8,380 production Chao class/asset entries match the pre-client frozen development outputs and remain exact after the entire run. The immutable post-cohort proof SHA256 is `a729f4619eb17efe5c1061edfbcc4f49e212acb85e3fb1b94e21f7fea0ee1ddb`. This is development-classpath/JAR byte equivalence; `runClient` is not claimed to have loaded the archive itself. All 7,794 prior asset entries remain exact. Existing worlds need no migration. The source ZIP is a cumulative overlay onto complete 0.25.43 sources; current changes are separately measured against the accepted local 0.25.65 snapshot, without assuming a published 0.25.65 Git parent.

No remaining new blocking failure was found in the tested scope. The inherited guide checker and `GardenGuideItem` match local 0.25.65 exactly, so its 8/9 formatting result remains separate from successful build/native checks. Inherited glasses lens-edge stippling and frontal player/hair/halo/ear occlusion remain limitations. A hat can cover the crest rather than exposing every feature.

Actual rendering used Linux llvmpipe OpenGL 4.5, not a phone GPU. Online skin/account services were unavailable, so the carried player is a local fixture. Physical touch or mouse hardware, all accessories/toys/coats in motion, shader packs, complete continuous glint/motion playback, independent remote multiplayer/latency/reconnect, a full world/server process restart, authentic older HeroRun worlds and an entire natural lifetime were not verified. Native NBT round trips, synthetic old-layout records and tracked-data observers provide bounded save/sync evidence; they are not those missing checks. GLIDE clearance is numerical; no actual free-gliding client traversal is claimed.

Install matching client/server versions with Java 21, NeoForge 21.1.219 for Minecraft 1.21.1 and external Lionfish API 3.1. Real equipped adult companions also need the existing Accessories/owo dependencies. Tomorrow, try all nine forms at fresh/half/full development, reverse both training axes while watching locked Hero/Run identity, exercise your usual hats/animal parts/equipment, walk and swim normally on your phone, drag the Creative maturity bar and restart the world/server.

For an already evolved Hero/Run Chao locked at **+100** alignment, `/chao ~ 100 Run 10` selects the nearest and sets its Run level 10. With Run still 10, `/chao "Kite" 100 Power 20` selects a uniquely named Chao and crosses later training toward Power. These retain existing main-stat level units 0–99; they do not replace first type or biological age. Use Creative maturity separately. Existing operator previews remain `/chao testtoy list`, `/chao testtoy broom once`, `/chao testtoy picture_book loop`, `/chao testtoy stop` and `/chao testcarry loop|stop`; they share production clocks/cleanup and grant no gameplay rewards. Broom/book/carry were exercised; availability of other previews is not a listening or every-toy validation claim.
