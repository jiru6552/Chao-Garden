# Reciprocal Neutral Swim/Fly forms — 0.25.72

Implemented all six mature appearances missing from the 0.25.71 coverage check. Both families now use the full signed Run–Power and Swim–Fly axes, with gradual geometry and palette blends. A Swim-first adult can develop toward Fly, and a Fly-first adult toward Swim, while retaining its original first type, locked alignment and biological age.

| Locked Neutral first type | Newly implemented later specializations | Existing mature targets retained |
| --- | --- | --- |
| Swim | Fly; Run + Fly; Power + Fly | Balanced, Run, Power, Swim, Run + Swim, Power + Swim |
| Fly | Swim; Run + Swim; Power + Swim | Balanced, Run, Power, Fly, Run + Fly, Power + Fly |

The current implementation contains 15 newly evolved parameter targets and **135 distinct, reachable mature parameter targets**, with no duplicate mature target signatures within a family. This establishes coverage; it is not a visual certification of every previous form. Each family's newly evolved appearance intentionally remains common across later training tendencies.

The original **7×6×6 head box**, eye geometry/UVs, 59 shared joints, young target and six existing mature targets in each affected family are unchanged. The other 13 families' implementation/assets are byte-identical. The production JAR has the same entry set as 0.25.71: only four policy/paint classes, four affected model/manifest resources and version metadata differ. Save, network, commands, shared renderer, animation and interaction implementations are unchanged.

## Reference and appearance

The live [Chao Island viewer](https://chao-island.com/viewer/) rendered and was operated independently in Chromium 156 using WebGL2/ANGLE SwiftShader. First type, alignment, both training axes, age and camera rotation worked. No compatibility warning appeared in this session, so no Continue action was necessary. TLS verification remained enabled. The preserved reference includes 94 original canvas screenshots and 485 recorded actions; no source geometry or hidden progression formula was imported.

All six new corners were compared at nominal ages 0%, 50% and 100% from front, side and rear; at 25% and 75% from the side; and at mature age from above. Mature opposing-axis strengths 25%, 50% and 75% were also compared from the side. In mixed-form training comparisons, Run/Power stays at its endpoint while only Swim/Fly changes. These are separate from the equal-strength mixed gain/hold/reversal tests.

Swim-first additions develop a rising posterior blade and compact side fins. Pure Fly is taller, Run + Fly longer/lower, and Power + Fly stockier. Blue young wings develop violet or peach/lilac accents. Fly-first additions develop broad capped lobes for Swim, compact inward-rolled tips for Run + Swim, and upright magenta peaks for Power + Swim. Mature wings turn gold except the Run + Swim treatment, which retains green.

These are blocky first-pass interpretations. The Swim posterior blade is narrower and more rectangular than the smooth reference wedge. Fly lobes/curls and small side details are simplified into the existing closed cuboids. Fine stripes, gradients and wing rims use simpler swatches; authored color keys and linear geometric blends approximate the observed progression. The fixed head and shared rig were preserved rather than broadening the smooth-model silhouette.

The delivered `comparisons/final/` gallery contains **90 matched viewer/game pairs in 10 sheets**. Start with `swim-age100-front-side-back.png` and `fly-age100-front-side-back.png`, then the age and quarter-training sheets. Crops and resizing are uniform and originals remain unchanged. Physical viewer angles, lighting and idle poses are approximate matches. Age labels compare the viewer slider fraction with the mod's visual maturity fraction, not an assumed identical lifetime formula. Some captures include ordinary blinking or head tilting. Age-zero Swim side/rear source captures showed minor rendered differences with correct controls; their cause was not isolated, and no universal pixel-identity claim is made.

## Actual verification

| Check | Result |
| --- | --- |
| Core mechanics and new permanent all-family coverage guard | Passed; guard performs 27,586 checks across all 135 corners, five ages, interiors, variants, locks, codec and bidirectional training |
| Dedicated GameTest server, all 20 morphology/editing namespaces | **All 1,098 required tests passed**, including real entity NBT reload, synchronization data, permissions, evolution ties, commands, opposing drives/reductions and Creative edits |
| Affected Swim/Fly rig and material suites | Passed dense geometry, continuity, opaque UV, eye/orb, attachment, animation and wing checks; 172 coats; maximum tested palette step 5 against unchanged bound 8 |
| Expanded Piano and Organ grip suites | Both passed; both affected families now cover 27 configurations each |
| Exact old/new palette comparison | 900,936 color records and 50,724,864 atlas pixels match for the retained forms |
| NeoForge 1.21.1 assembly | Passed; normal prepared production classes/resources match the delivered JAR bytes |
| Canonical in-game captures | **109 scenes / 1,048 original PNGs**, zero skipped scenes: 48 per affected family plus 13 earlier-family regressions |
| Diagnostic preflight | 18 additional scenes / 108 original PNGs passed before the canonical runs |

Each affected family was captured at five ages, three fractional opposing-axis strengths, all six retained mature targets, five representative genetic/shiny/jewel coats, pumpkin/rabbit/Phoenix+glasses attachments, Creative and Survival Analyzer interactions, both directions through balance, a mixed gain/held-endpoint/reversal, individual gain/loss, broom/book interaction and an actual Accessories flying-player attachment. Existing production preview commands/clocks were reused.

The client checks verified actual encoded state and server/render completions. Creative pointer/drag handlers updated maturity and preview while retaining adult identity; Survival edits were denied. Entity/codec reload and synchronization tests passed. The two silent normal-speed recordings total **177.7 seconds / 3,554 decoded frames**. Their complete media decode passed; visual review covered 40 spaced frames, not every frame or continuous playback. Stat inputs were accelerated for these tests while the game continued at normal 20 TPS. Each mixed endpoint was held until an actual client-rendered sample settled before reversal.

The primary reviewer personally opened all 10 comparison sheets, 18 additional review sheets, six motion timelines and four earlier-family regression sheets. No obvious new eye/anchor, detached-part or abrupt-transition defect was found in those inspected views. Native originals, callback hashes, control measurements, logs, closed receipts, source inventory and independent byte audits are included in the delivery evidence. One private plan used generic `side` for dynamic views; an explicit erratum preserves the original plan and binds the actual helper's `left` label. Static matching-angle comparisons were unaffected.

## Limits and inherited failures

No new applicable build, test or capture failure remains. The inherited journal-formatting result is still **8/9** (`Your gardens` paragraph separator); that unrelated check was not rerun or repaired. The complete unrelated custom-test catalogue was not rerun: this pass ran the affected rigorous suites, the core guard and all 20 native namespaces. Inherited startup diagnostics are recorded in the logs and did not cause failing tests.

Physical phone/touch interaction, shader packs, independent remote multiplayer/reconnect, every accessory/toy, a complete existing-world restart, natural lifetime progression and all-frame collision clearance remain unverified. Native entity synchronization and the integrated client are narrower checks than remote multiplayer. Captures used the development client with production classes/resources proven equal to the JAR; a separate fresh launcher installation of the archive was not performed.

For testing, use an existing mature Neutral Swim Chao and train Fly past balance, then reverse; repeat with a Neutral Fly Chao toward Swim. Try a mixed Run or Power tendency and your preferred hat/color on the phone, then check save/restart and a second client. Existing saves need no new state format or re-evolution. The source ZIP is cumulative against complete 0.25.43 source, retaining all 0.25.71 changes; its included instructions and exact guarded source-deletion helper explain rebuilding. No Git publication is part of this delivery.
