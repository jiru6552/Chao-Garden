# Chaos anatomy and ice surfaces — 0.20.0

Reviewed the three regular two-tone game screenshots and the full Chaos appearance descriptions. The gameplay meshes are newly authored with connected cuboid tiers; the raw Marketplace comparison assets remain unchanged. This is a block interpretation of game shapes, rather than a smooth mesh transplant.

## References reviewed

- [Requested Sonic Wiki Zone page](https://sonic.fandom.com/wiki/Chaos_Chao): Light, Angel, Devil family distinction. Direct full-page access is restricted, so appearance details were cross-checked with the dedicated Chao Island page and gallery.
- [Chao Island Chaos appearance](https://chao-island.com/wiki/Chaos_Chao): no mouth, large solid eyes; two-tone head/hands are partially transparent. Angel has wavy feelers with pink translucent highlights and a long downturned rear spike. Devil has two upward/two downward horns, arm spikes, a long forked tail, yellow chest crescent, purple flame marker. Wings are tiny and normally almost hidden.
- [Chaos gallery](https://chao-island.com/gallery/chao-types/chaos.html): regular, colored, shiny, mono-tone and two-tone forms.
- Viewed [Light regular two-tone](https://chao-island.com/assets/images/gallery/twotone/chaos/neutralchaostwotoneregular.jpg), [Angel regular two-tone](https://chao-island.com/assets/images/gallery/twotone/chaos/herochaostwotoneregular.jpg), [Devil regular two-tone](https://chao-island.com/assets/images/gallery/twotone/chaos/darkchaostwotoneregular.jpg). Local reference-only JPGs are under `chaos020-reference`; none of their pixels are used in gameplay assets.

## Result

- Light: broad tapered head, single stepped droplet crest, blue side/rear lobes, bright green eyes and teal light marker.
- Angel: two connected wavy descending feelers, pink icy terminal portions, long rear point, rose chest marking, blue eyes and yellow block halo.
- Devil: two rising/two falling horns, short arm spikes, forked tail, yellow crescent/eyes, orange-red tips and purple flame marker.
- Each mesh has one pair of solid eyes and no donor eye panel or expression mouth.
- All three keep source baby animation pivots. `skull__cube_0` is the common head core; `body__cube_0` is the torso; `arm_*__cube_0` is upper arm. `attachment_landmarks.eyes` provides `[0,-2.25,-3.045,1.75]` in skull space.
- All351 existing inherited coat keys (117 per family) have a dedicated native-UV gameplay texture. Colors, jewel patterns and shiny eligibility remain inherited. Canonical Light/Angel bodies are pale, Devil is dark, and family chest/tip colors are distinct. Negative/bright palettes and cosmetic wing masks include new UV swatches. Protected eye texels remain unchanged.

## Material boundary

The glass layer contains the actual distal geometry. It is not a transparent overlay on an opaque hand or foot. A cuboid surface union removes interior faces before dividing opaque and translucent external shell faces. The foot cut is exactly `z=-1.2`; head feeler cuts follow the family silhouette. At the join, glass alpha is1, smoothly reducing to0.38 at the far tip. Head surfaces also blend the inherited core hue into the tip hue, with a pale edge. Mono-tone coats use the same terminal mesh with alpha1. The policy is the expressed two-tone flag for ordinary, colored and jewel coats alike; shininess does not change opacity. Two-tone jewel coats therefore receive icy tips, while mono-tone jewel coats remain opaque.

The vanilla culled entity translucent buffer sorts the exposed shell quads. Main body and eyes continue to use the opaque entity pass. Glass poses copy the complete gameplay hierarchy, including visibility, so hats, replacement animal limbs and gameplay motion also affect terminal geometry.

Legacy Chaos overlays are disabled when the authored Chaos rig is active. Chaos family selection requires both immortality and `character == normal`; special character Chao no longer receive a Chaos crest, wings or Chaos texture by virtue of being immortal.

## Checks

Run `python art/build_chaos_020.py` after the older asset builders. It emits `CHAOS_020_CHECKS.json` and checks matching opaque/glass hierarchies and pivots, finite vertices, native/gradient UV bounds, one eye pair, no donor face or mouth, no opaque geometry under the glass feet, all351 inherited textures and the complete hash set of41 raw source files. Coplanar face merging keeps each family to325–462 visible anatomy/marker quads; the retained, usually hidden source reaction glyphs add120quads per rig.

These static checks do not substitute for the release's actual-client front/three-quarter/rear and checker-background glass captures. The runtime layer deliberately uses ordinary Minecraft rendering so it also works without an optional shader pack.

`python art/validate_chaos_020.py` independently checks all translucent head branches are in the opaque rig's head-decoration hiding subtrees, complete skull hiding covers every icy head branch, hats leave limb ice visible, every tip reaches both ends of its gradient UVs, no cut-plane cap is present, all117coat keys per family exist, and donor eye panels remain empty. Its result is `CHAOS_020_VISIBILITY_CHECKS.json`.

## Overlay crown attachment audit

The stronger development meshes also exposed the old overlay's fixed source-height assumptions. `ChaoOverlayLayer` now fits animal ears, forehead and horns (including extra CWE animal parts), theme head roots and floating overlay markers to the current main skull bounds relative to the raw donor skull. It changes only added attachment roots; sampled skull and limb poses remain untouched. Juvenile parts retain their existing size reduction but shrink about the actual current crown. Nonzero floating marker pivots are included in the affine composition. Face and beak parts keep their existing eye-relative plane because development preserves face placement while extending the crown/rear.

`python art/validate_overlay_head_fit_020.py` checks135development endpoints, including120changed skull bounds,750skull-local roots and all juvenile growth scales; results are in `OVERLAY_HEAD_FIT_020_CHECKS.json`.
