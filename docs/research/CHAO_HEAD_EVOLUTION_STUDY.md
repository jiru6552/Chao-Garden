# Chao head evolution study for 0.8

Reviewed 29 October 2026. This is visual design research, not an asset import.

## Sources examined

- [SEGA Sonic Channel: Chao character profile](https://sonic.sega.jp/SonicChannel/character/chao.html). Primary description presents Chao as adaptable creatures whose form responds to the characteristics of other life.
- [SEGA Sonic Channel: Act 023](https://sonic.sega.jp/SonicChannel/special/sidestory/comic/20161202_001245/). Primary explanation describes changes in Chao color and shape through environment and contact with other creatures.
- [Chao Island evolution comparison](https://chao-island.com/info-center/life-cycle/evolution.html). The neutral Fly, Run and Power and Hero Swim image links were retrieved as visual reference. The neutral Run explanation identifies its three large rear spikes and Sonic resemblance; Fly has curved side lobes, and Power has upward/outward spikes.
- Direct images: [Neutral Fly](https://chao-island.com/assets/images/info-center/evolution-neutralfly.jpg), [Neutral Run](https://chao-island.com/assets/images/info-center/evolution-neutralrun.jpg), [Neutral Power](https://chao-island.com/assets/images/info-center/evolution-neutralpower.jpg), [Hero Swim](https://chao-island.com/assets/images/info-center/evolution-heroswim.jpg).
- [Chao Island 3D viewer](https://chao-island.com/viewer/), credited to Justin113D by its tool page. The already prepared local front/side silhouette renders for all fifteen evolved heads and the neutral child four views were actually opened and inspected. The viewer is a supplemental reconstruction with its own color system, not a claim of byte-identical original game data.
- The current 0.7.0 Minecraft theme preview was opened for a direct comparison with the above silhouettes.

No video inspection is claimed. Public Chao Island HTML downloading returned 403; its page and image links were accessible through web retrieval. The existing viewer-derived reference renders provided the reliable multi-angle geometry comparison.

## Observations

| Form | Front silhouette | Side silhouette | Design implication |
| --- | --- | --- | --- |
| Neutral child | Broad cheek volume narrows into one soft point | Point grows from a rounded cranium and leans behind the face | The crest is part of the head, with no mounting plate or hat brim |
| Neutral Run / Sonic direction | Dominant central quill and low side points | Main quill and secondary points sweep toward the rear | Direction and mass matter more than simply adding three spikes |
| Neutral Fly | Wide outward crown lobes with dipping tips | Roots occupy substantial upper/back head volume | A lobe needs a broad root, a bend, and a tip that tapers in depth |
| Neutral Power | Strong outward temple fins with central crown mass | Low fins sweep outward and back | Strength can read through width and swept wedges; tall antlers are not required |
| Hero Swim | Low split leaf-like crown | Long, flattened rear sweep | Two broad flowing folds can replace numerous small projections |
| Dark Run | Several radial rear points | Jagged rearward silhouette rather than vertical rods | Use a few intentional directional forms that join the skull |

The current 0.7.0 themes mostly preserve an identical flat cranial box and place small objects on top. Their floating marker and palette carry more identity than their head silhouettes. The refinement should move identity into the cranial outline itself.

## Design rules for the themed heads

1. Preserve the broad face, cheek height, eye plane and small torso relationship. Change the crown and temples without enlarging the head into shoulder space.
2. Give each theme one main structural idea, such as a kinetic wave, growing fluix facets, a split leaf crest, a cloud fold or a sculk sensor lobe. Secondary points should reinforce that idea.
3. Join every lobe to a substantial root. A small rod, peg, crown mount, hat brim or thin antenna reads as an accessory rather than an evolved head.
4. Taper in three dimensions. A full head-depth extrusion of a pointed front outline becomes a slab; upper tips should lose front-to-back thickness and move rearward where appropriate.
5. Vary the side view as well as the front view. Broad low sweeps, paired lobes, upright buds and a single hooked point should not share one generic rear profile.
6. Carry the base head color through roots, then place the accent near the tip. A continuous color transition supports the impression of growth from the head.
7. Use restrained geometry. The silhouette should remain legible at normal Minecraft viewing distance and in a flat, single-color view without its marker.
8. Keep eyebrows and eye expression readable. Crown details should not cross the face or cover the existing eye planes.
9. Recheck resting, swimming, eating, petting and sleeping with the actual animation transforms, including arm clearance and floating-marker spacing.

## Feedback on the integrated loft approach

An integrated, closed head shell is a sound fit for this request. Replacing the shared head shell for selected themes prevents the old cranial box from showing through and makes the crown genuinely part of the surface. The replacement should use a stable face region and a lofted upper crown, with a continuous perimeter and tapered front/rear tip thickness. It should not use a uniform-depth extrusion for all upper points.

A restrained bevel on the upper outline can bridge the Minecraft model vocabulary and the original games' flowing head changes. The result should still read as a Chao first, with the mod suggested by the direction, number and shape of its lobes rather than by a miniature item attached to it.
