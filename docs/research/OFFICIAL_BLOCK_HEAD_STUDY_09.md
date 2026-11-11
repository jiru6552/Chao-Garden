# Official Sonic add-on block construction — 0.9 refinement

Revisited October 29, 2026 for the user's correction to the 0.8 themed heads.
The goal is the construction language of Gamemode One's official Minecraft
Sonic add-on. The 0.8 silhouettes can guide the theme, while their continuous
faceted skull surfaces should give way to a rectangular main head and a few
substantial cuboid growths.

## Primary references actually inspected

- [Gamemode One: The ultimate guide to Chao](https://support.gamemodeone.com/hc/en-us/articles/4402606440347-The-ultimate-guide-to-Chao), reopened during this pass. This is the publisher's guide, linking the variant showcase and garden image.
- [Official variant GIF](https://support.gamemodeone.com/hc/article_attachments/4402923261595/Sonic_ChaoCharacter.gif): the retained native file is **720 × 720 pixels, 21 frames**. I inspected the complete contact sheet again and extracted native frames 0, 5, 8, 10, 13, 14, 17, 18 and 20 into a scratch study folder. Frames 0, 10, 14, 17 and 18 were inspected individually at native resolution. Frame numbers identify this GIF, not guaranteed official type IDs or unique-form counts.
- [Official garden image](https://support.gamemodeone.com/hc/article_attachments/5709130352155/ChaoGarden.png), reopened and visually inspected at **1600 × 1600**. Emerald and Knuckles provide particularly clear examples of chunky crest/lock construction from another pose.

No add-on model source, texture source or animation source was extracted. Exact
dimensions, cube counts and rotation angles cannot be proved from these images.
No new video timing claim is made.

## What the images show

| Reference | Visible construction | Useful rule for the new models |
| --- | --- | --- |
| Baby, GIF frame 0 | A plain rectangular head remains readable beneath a low, broad hooked tuft. The tuft has a flat cut end and rectangular color areas. | Keep the main head cube; add a compact crest with broad roots and square ends. |
| Pink flying form, frame 10 | Large lateral lobes rise on a slant. Their faces are rectangular, their terminal blue faces are flat, and broad pink/lilac bands follow the pieces. | A slope can come from a **rotated cuboid**; it need not be a stair made from tiny voxels or a triangular wedge. |
| White forked flying form, frame 14 | Two thin rectangular branches angle outward above the forehead, ending in cyan bands. | Forks should be a small number of deliberate rectangular bars with clear roots and flat tips. |
| Orange power form, frame 17 | Substantial upward/backward blocks change the head outline, with pale terminal areas. The ordinary broad front face is retained. | Use chunky block lobes attached to the box, instead of replacing the upper skull with one sculpted surface. |
| Green running form, frame 18 | Broad rear and temple quills resemble rectangular bars, with directional slopes and color bands. | Convey motion through the placement and rotation of a few thick quills, maintaining obvious cuboid faces. |
| Emerald, garden image | Broad green/cyan crest pieces create an explicit stepped contour over the rectangular face. | Preserve a stepped silhouette where that is the intended shape; do not bevel the whole head into a dome. |
| Knuckles, garden image | Long straight-sided locks flank the large rectangular head, each with a flat lower cut. Their pose introduces slanted edges without smoothing the underlying pieces. | Rectangular growths may rotate with the form or pose; square cross sections and flat ends remain the visual grammar. |

The official aesthetic therefore permits **both stepped axis-aligned blocks and
rotated cuboids**. It is not restricted to an axis-aligned staircase. The key
difference from 0.8 is the visible rectangular construction: intact main head,
deliberate added blocks, flat ends, broad color regions and no triangular
faceted crown skin.

## Concrete authoring direction

1. Restore the existing **8 × 6.5 × 6.5** main head cube for the themed forms.
   Those dimensions are this mod's previously approved anatomy, not claimed DLC
   measurements. Keep the face planes, expression positions and low arm rig.
2. Translate each theme's dominant silhouette into a restrained authored group
   of chunky cuboids. A useful starting budget is **two to six primary pieces**
   plus color caps; this is an art guideline, not an observed source count.
3. Use broad rectangular roots that penetrate the crown slightly so they read
   as head growth. Avoid detached rods, thin antennae and stacks of tiny cubes.
   Forks, lobes and quills should remain different arrangements in three views.
4. Use a few deliberate rotations for swept, wing-like or hooked forms. Keep
   the cross sections square/rectangular and the terminal surfaces flat.
   Approximate angles are authored choices to judge in game, not source data.
5. Keep the palette's broad body/crest/tip regions. A terminal color cap should
   follow a block face or the end of a quill, rather than an arbitrary triangle
   on a continuous mesh. Use the existing one-texel-per-model-unit cube UVs.
6. Avoid visible coplanar overlays: put joints inside the base/head growth,
   and put caps at the end of a block instead of overlapping its outer face.
7. Keep Create's and Thaumcraft's goggles as separately authored accessories
   across the **upper forehead**, below the crown and above the Chao eyes.
   Their detailed construction should follow the respective mods' actual
   goggles, rather than the generic old square-lens prop. This placement is
   the user's explicit direction, not an inference from the Sonic images.

## Visual review checklist

At a common front, three-quarter and side camera, check the main head is still
an unbroken rectangular structure; crests have readable block faces and flat
ends; each theme keeps its intended silhouette; goggles sit against the upper
forehead; pupils and expressions remain clear; resting and animated hands do
not enter the head; and no caps, straps or roots produce flicker or gaps.

The original-game Chao evolution study still guides which way a form grows.
These official images govern **how that growth is built in Minecraft**.
