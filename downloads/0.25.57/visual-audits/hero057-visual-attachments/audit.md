# Actual-client attachment and motion audit (.57)

**PASS WITH LIMITATIONS.** No blocking visual defect was observed in the reviewed frames. Only the successful final whole-56 cohort is accepted here.

Source: `/workspace/.setup/heronormal057-client-final.log`  
SHA256: `e1e5d7ffb832c33595739148935735a0ad756bc605f07394fb81c83ddb14f8c3`

`[18:02:42] [Render thread/INFO] [co.ch.cl.HeroNormalVisualSmoke057/]: HERONORMAL057_VISUAL_COMPLETE requestedCases=56 renderedCases=56 skippedCases=0 start=0 limit=56 baseline=false captures=435 saved=435 epochMillis=1794506562920 normal20TPS=true audioListening=false output=/workspace/.setup/heronormal057-client-final`

All 140 source PNG hashes match their literal capture-log SHA256s. I actually inspected 72 selected frames (first/middle/last at each of three camera angles for each of eight cases) in the labelled contact sheets, plus four of those same frames again as full-resolution originals. The original PNGs are unchanged. The sheets use the uniform crop `[350,100,1100,850]` and resize each crop to 400×400 for comparison.

The complete path/hash/capture-line index is `original-png-review-index.json`. Per-case literal native/render completion lines, selected original paths/hashes and sheet hashes are in `audit.json`; relevant original log lines are retained in `cohort-proof-lines.txt`.

## Case 48: Hero/Normal Run+Fly gain during real navigation

Selected frames show compact extremities, intact face and blocky head, and a coherent crest/halo as training increases. The early crawl-like and later walking poses belong to the existing movement system; no detached features, texture tears or conspicuous gaps were visible in these frames.

Evidence: `case-48-selected-original-frames.png`; three angles, nine selected frames. Native completion logged walked distance `6.792856146291008` blocks; render completion logged `386` moving render frames.

Boundary: Frames are sampled during development and sequential camera angles; this review does not prove absence of between-frame popping.

## Case 49: Hero/Normal Run+Fly reduction during real navigation

The selected reduction sequence keeps the original-looking head and face intact while the added details recede. The return to a low-Run crawl/seated gait is visible; this is a movement ability change, not a biological-age reversal. Hands, feet and halo remain coherent.

Evidence: `case-49-selected-original-frames.png`; three angles, nine selected frames. Native completion logged walked distance `6.009938348955438` blocks; render completion logged `407` moving render frames.

Boundary: Selected frames, not continuous playback; no age-reversal claim is inferred from the gait.

## Case 50: Hero/Normal Power+Swim gain during real navigation

Front, side and rear-oblique walking frames show coherent broad extremities and unbroken cuboid head/face surfaces as the Power+Swim target develops. No obvious elongated limbs or exposed interior geometry is visible. The halo remains a complete ring.

Evidence: `case-50-selected-original-frames.png`; three angles, nine selected frames. Native completion logged walked distance `9.739358196337234` blocks; render completion logged `409` moving render frames.

Boundary: The controlled fixture keeps balanced Run/Power baseline progress to unlock ordinary walking without adding a net Run influence.

## Case 51: Hero/Normal Power+Swim reduction during real navigation

The sampled reduction sequence keeps arms/feet joined to the shared rig and the face and head intact. Details reduce without an obvious abrupt model replacement in the sampled frames. The halo stays separate above the head.

Evidence: `case-51-selected-original-frames.png`; three angles, nine selected frames. Native completion logged walked distance `9.739358196337234` blocks; render completion logged `410` moving render frames.

Boundary: The 12-tick screenshot sampling cannot certify every intermediate rendered frame.

## Case 52: Hero/Normal Run+Fly broom once preview

Front views show both compact palms together around the broom shaft, and the side view keeps the shaft at the grip with the bristles at the floor. The smiling facial pose works. Later rear-oblique frames show the broom gone and the arms back down after normal once completion. A full-resolution side original was additionally inspected.

Evidence: `case-52-selected-original-frames.png`; three angles, nine selected frames. Native completion logged walked distance `0.0` blocks; render completion logged `0` moving render frames.
Production palm-contact checks logged `382` samples and maximum error `2.504825E-7` blocks (threshold `<0.025`). These numerical checks support, but do not replace, the visible grip review.

Boundary: The opposite hand is partly occluded in the side view. This scene exercises preview completion, not natural approach, damage interruption or toy removal.

## Case 53: Hero/Normal Power+Swim picture-book once preview

The book remains raised above the feet with the hands supporting its lower edges, including the sampled page-turn pose. The face remains legible and compact limbs do not stretch toward the prop. Later rear-oblique frames show the book removed and the arms down after normal completion. A full-resolution side original was additionally inspected.

Evidence: `case-53-selected-original-frames.png`; three angles, nine selected frames. Native completion logged walked distance `0.0` blocks; render completion logged `0` moving render frames.
Production palm-contact checks logged `382` samples and maximum error `3.8656955E-7` blocks (threshold `<0.025`). These numerical checks support, but do not replace, the visible grip review.

Boundary: The book is partly occluded from the rear. Natural approach and forced interruptions are not established by this preview.

## Case 54: Hero/Normal Fly real Accessories projection during upright Creative flight

Side and rear-oblique views show the projected adult against the player upper back rather than suspended behind/below it. The front player view naturally hides the Chao. Head features/halo and compact limbs remain visible and coherent where not occluded. A full-resolution side original was additionally inspected.

Evidence: `case-54-selected-original-frames.png`; three angles, nine selected frames. Native completion logged walked distance `0.0` blocks; render completion logged `16` moving render frames.

Boundary: Owner is upright in Creative flight with pitch 0 and fall-flying stopped. This is not elytra, swimming, takeoff/landing or horizontal-flight transition coverage.

## Case 55: Hero Run baby real Accessories projection during upright Creative flight

Side and rear-oblique samples keep the baby tucked against the upper back with feet near the waist. It does not visibly trail behind/below the player in this upright pose. The front naturally occludes it. A full-resolution rear-oblique original was additionally inspected.

Evidence: `case-55-selected-original-frames.png`; three angles, nine selected frames. Native completion logged walked distance `0.0` blocks; render completion logged `16` moving render frames.

Boundary: The player body obscures the baby face/contact area. Same upright-Creative-only flight boundary as case54.

## Evidence boundaries

- Visual review covers 72 selected actual-client PNG frames (nine per case), not all 140 captured originals and not continuous video playback. Four of the same selected frames were additionally opened as full-resolution originals.
- Each case has front, side and rear-oblique programmed camera positions, labelled front/left/back. The angles occur sequentially in time; these are not simultaneous matched-angle comparisons. No above/below inspection is claimed for this subset.
- No audio was listened to. No remote multiplayer latency, full modpack, shader, all-accessory, all-replacement-hand or all-toy visual coverage is implied.
- Broom/book evidence covers actual once previews and normal completion only, not autonomous approach or damage/removal interruption.
- Accessories evidence covers the actual projection in upright Creative flight only, not elytra/swimming/other flight transitions.
- Exact unchanged base-head dimensions and gameplay invariants require the separate rig/core/native tests; pixels alone do not establish those numerical facts.
- Initial failed/incomplete cohorts are excluded. This audit uses only the single whole-56 successful final cohort.

## Fixture history

The earlier cohort stopped on a motion-fixture navigation prerequisite. A controlled balanced Run/Power progress baseline was added to the development helper to unlock ordinary walking without adding net Run specialization. The final helper also holds that baseline through the real Analyzer progress API while passive movement training occurs. Navigation and endpoint assertions remain in place; production movement and morphology code were not changed for this correction. Failed/incomplete captures are not used as final evidence.
