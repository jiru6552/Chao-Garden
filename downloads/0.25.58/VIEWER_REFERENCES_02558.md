# Live Chao Island viewer reference — morphology refinement 0.25.58

The current page actually rendered a Chao in Chromium 156.0.8078.79 using WebGL2/ANGLE SwiftShader. Actual mouse clicks changed Child/adult, Normal first type, Neutral/Hero alignment, both Run/Power and Swim/Fly sliders, and young/old age. Reset plus actual orbit drags produced front, both profiles, back, rear-oblique, above and below views. No compatibility warning appeared; the final context was healthy (`contextLost:false`). This is operational verification, not merely a successful page load.

The shared desktop JavaScript/CUA tool was absent. The first sandbox Chromium attempt failed at navigation with `ERR_CERT_AUTHORITY_INVALID`. The same installed browser then worked outside the sandbox using its existing outer-executor trust, with TLS verification enabled. No `ignoreHTTPSErrors` or certificate bypass was used. `capture-first.log` retains the failed attempt, while `capture.log`, `actions.json`, `browser-events.json` and `verification.json` describe the accepted session.

`reference-manifest.json` binds **205 unchanged original canvas PNGs**, their exact hashes, approximate mouse-controlled settings and session provenance. These are one initial Neutral child plus twelve adult settings: three requested Neutral/Normal specializations and all nine Hero/Normal combinations. Every adult setting has mature seven-angle coverage, fresh/half front-side-back-rear-oblique coverage, and quarter/three-quarter side views. Captures are not automatically claimed personally inspected: `reviewed-originals.json` records **142 selected original views actually inspected**, directly or through uniform crop/resize contact sheets. The other captured angles remain available as evidence without a claim that every view was manually studied. No viewer geometry was imported.

## Requested Neutral / Normal forms

The side-view face points right. Directions below refer to the Chao, not the viewer's screen coordinates.

- **Swim:** The smooth mature silhouette has a broad sloping upper-head shoulder extending rearward; its rear upper end curls up. The rear apex is visibly higher than its forward shoulder. It is not a rear end sagging down from a flat horizontal bar. The current fixed-box interpretation can suggest this with an angled lower addition and a short rising upper piece, rather than moving or reshaping the skull. A simple sign reversal of a long rectangular tip is not a full reconstruction of the smooth curve.
- **Power + Swim:** From the front, the entire upper shoulder rises and narrows steadily to one tall central apex. The side profile is somewhat rear-biased, and the front shoulder slopes toward it. A broad lower step, narrower rising step and small apex can communicate the cone while leaving the original head untouched. A narrow isolated stick does not communicate the broad shoulder.
- **Power:** The lower crest bulges forward/up before its upper end sweeps back/up like a wave or hook. The upper apex is roughly 35–40 screenshot pixels behind the lower neck in the 970×486 mature side image. This rough visual measurement is not a game/model formula. The front view alone hides much of that hook. The mod should use a clearly angled lower rise and a shorter back-swept upper section, following the later user direction/length guidance as an authored blocky interpretation.

The root coordinator received additional user redline guidance after these captures. Final Neutral cuboid directions/lengths are therefore also user-guided choices; they should not be described as a polygon-for-polygon or exact mathematical copy of the smooth viewer curves.

## Hero / Normal head structure

All nine remain white-bodied and retain two separate main top features, rather than the three-branch Hero/Run-first-type crest. Each feature has a generally white lower portion and a variant-colored upper portion. The lower part itself changes proportions, angle and spacing; identical recolored sticks do not reproduce the visible differences.

| Specialization | Actually observed mature two-part feature |
| --- | --- |
| Balanced / Normal | Broad white lower halves; compact blue rounded leaf/cup-like upper buds. |
| Run | White lower bases widen from the head; long slender blue/cyan upper points diverge outward and up. |
| Run + Fly | Shorter and broader leaf/spike upper shapes than pure Run, on raised white bases. |
| Run + Swim | Longer broad pointed leaves, wider than Run/Fly; substantial white roots and cyan/blue upper regions. |
| Power | Low outward-bending white necks; broad blunt purple-blue upper knobs wider than their necks. |
| Power + Fly | More raised/outward white necks than pure Power, with broad angled violet knobs. |
| Power + Swim | Broad joined-looking white roots and broad outward/upward blue horn caps; less blunt than pure Power. |
| Fly | Very thin white stalks ending in substantial lavender bulbs. Upper bulb width is roughly comparable to centered buds; the whole feature must not be reduced to tiny dots. |
| Swim | Broad white roots, joining visually as a low V, with wide cyan/green upper leaves tapering to long outward points. |

In the matched mature front canvases, the total visible feature heights are approximately centered 55, Run 80, Fly 50 and Swim 85 pixels; the Swim fan is about head-width or slightly wider. These numbers are approximate visual comparisons at the captured pose, not imported dimensions or prescribed mod measurements. They support making Run/Swim taller and more spread, Power blunter/wider and Fly's neck thinner with a real upper bulb. The mod still uses simple closed cuboids and the fixed original 7×6×6 head.

An early coarse retained-sheet description called Fly a uniformly small bud. Direct inspection of the fresh Fly front and comparison with centered corrected that interpretation: Fly's **thin stalk / substantial bulb ratio** is the defining distinction, while total height is only moderately smaller. This correction was sent to the model and rig-test owners before finalization.

## Wings, colors and placement

Fresh side, back and rear-oblique views show that wings have a **white proximal/root region and variant-dependent distal pigment**. They are not uniformly colored identical plates. The shapes also vary: Run is a longer thin horizontal blade, Power shorter/chunkier, Fly broader and flatter, Swim broad and swept. Smooth curvature is simplified into thin closed blocky wings on the mod's existing wing joints; that construction is authored, not taken from the viewer mesh.

| Specialization | Visible distal wing color family | Other visible natural accents |
| --- | --- | --- |
| Normal | Yellow; broad white inner portion. | Blue caps/extremities, yellow belly, blue halo. |
| Run | Cool mint over most long blade, small white root. | Blue tips with green/cyan bands, blue belly, cyan halo. |
| Run + Fly | Muted gray-olive/mint. | Lavender/blue tips, gray-lavender belly, gray-mint halo. |
| Run + Swim | Pale yellow-green. | Blue/cyan tips, cyan belly, mint halo. |
| Power | Saturated golden yellow over most short chunky blade. | Strong deep blue/violet tips, pink-violet middle accent, golden belly, fuchsia halo. |
| Power + Fly | Golden ochre. | Violet tips, ochre/tan belly, pink halo. |
| Power + Swim | Golden yellow. | Blue tips, pale-green belly, pink halo. |
| Fly | White broad blade with ochre distal tip/edge. | Lavender tips, peach belly, salmon halo. |
| Swim | White broad inner wing with lemon-yellow distal tip. | Cyan/mint tips, mint belly, pale-yellow halo. |

These are lighting-dependent visible color families and placements. Exact RGBs, thresholds, band widths and colored-span percentages in the mod are authored approximations. The viewer does not show inherited coats or shiny finishes, so it supplies no verification of those mod variants. Preserve their base coat/finish while applying the same morphology and consistent accent regions.

## Development and limitations

The newly evolved Normal-first Hero forms are very similar across all nine opposed settings: small blue buds, white body, blue extremities, yellow belly/wings and a pale-cyan halo. Half maturity already separates Run/Swim points, Power knobs, Fly stems and halo/accent colors; quarter and three-quarter views show intermediate silhouettes rather than a different first identity. The Neutral fresh forms likewise start similarly, then diverge in the requested crest curves. The visual age track is not evidence for an exact absolute training amount or elapsed Minecraft time.

All actual selected settings and original views are available alongside the evidence. The viewer research alone does not establish the resulting Minecraft geometry, animation contact, named/shiny material behavior, accessory fit, save/reload, server rules or multiplayer behavior. Those require the root coordinator's separate implementation and game tests. User-directed cuboid silhouettes, fixed skull constraints and simplified wings necessarily remain approximations.
