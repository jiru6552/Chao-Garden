# Chaos head refinements — 0.21.0

The requested [Chao Island Chaos gallery](https://chao-island.com/gallery/chao-types/chaos.html) was checked again, including the regular two-tone [Light](https://chao-island.com/assets/images/gallery/twotone/chaos/neutralchaostwotoneregular.jpg), [Angel](https://chao-island.com/assets/images/gallery/twotone/chaos/herochaostwotoneregular.jpg) and [Devil](https://chao-island.com/assets/images/gallery/twotone/chaos/darkchaostwotoneregular.jpg) views. The colored, monotone and jewel examples help distinguish anatomy from the pale/pink/orange material colors. The earlier reference-only front/profile head study and the supplied Knuckles rig were also examined; Knuckles' hair descends beside the skull rather than flaring only from its rear.

All new slopes remain connected rectangular tiers. No reference-image pixels or reference-viewer meshes are copied into these additions.

## Narrow anatomical changes

- Light: side locks now begin in the central depth of the skull and descend past the chin. Their depth midpoint changes from2.30pixels to0.825; the old side endpoint above the chin at y=-0.55 now reaches y=1.55. Lower portions sit behind the resting arm/hand plane. The rear lock also turns downward, rather than extending mostly backward.
- Angel: each wavy feeler is broader, with the pair's overall width10.7pixels versus9.6previously (about11.5%). The tips descend to cheek level. This retains the two-peak outline rather than introducing more horns.
- Devil: the upper pair sweep outward and hook inward toward their narrower tips; their high point is y=-9.7 instead of-8.2. The lower pair descend to y=2.25 instead of0.9 and have a more defined taper. There are still exactly four head horns.

Coordinates are skull-local model pixels with positive y downward. The unchanged main skull is7wide,6.2high and5.65deep. Eye centers remain±1.75/-2.25/-3.045. The body, hands, feet, wings, markers, reaction glyphs and all native animation anchors are byte-identical to the approved0.20 bone data.

## Rendering and inheritance

`art/build_chaos_021.py` runs after `build_chaos_020.py`. It reuses that builder's native skeleton and exterior surface-union algorithm, then replaces only the named head appendages. All41 raw supplied inputs and351 inherited coat PNGs remain unchanged.

Opaque and translucent terminal surfaces share one exterior union without coplanar duplicate caps. Light's locks and Devil's lower horns fade along their downward direction; Angel's feelers and Devil's upper horns retain their outward/upward tip gradients. The distal half lies outside the solid skull. Existing two-tone alpha and monotone opacity policies are unchanged. The opaque and glass head branches retain matching names and hat-hiding metadata; a closed cap hides them together while leaving hand/foot ice visible.

## Validation

`python art/validate_chaos_021.py --self-test` performs2,511 independent checks. It reads final exported geometry instead of importing the generator: positive-X ray parity reconstructs filled cells, a six-neighbor walk proves each refined appendage is connected, and actual volume overlap proves attachment to the skull. It checks central/downward Light placement, restrained Angel width, Devil horn height/sweep, fixed approved non-head anatomy, native anchors, one eye pair, hat roots, distal glass backing, gradient UV coverage, finite coordinates and material-cap separation. Four isolated corruptions are deliberately rejected: a detached/rear-shifted lock, oversized feelers, a missing cap root, and changed eyes.

The older `validate_chaos_020.py` also passes its351-coat, limb-glass and headwear checks. Running the021 finalizer twice leaves all six final rigs byte-identical.

These checks establish geometry and material contracts, not universal animation clearance. Resting-limb separation is checked for the descending Light locks; actual Minecraft front/profile/rear, moving and capped views remain part of release review.
