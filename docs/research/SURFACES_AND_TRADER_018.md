# Continuous shiny surfaces and connected trader costume — 0.18

The six supplied game screenshots were inspected. The egg bands ended abruptly
where the shell's atlas panels met; the Chao material could reset at a head or
limb texture island. The reflection shader had used the same atlas UV as the
painted coat. Making the reflection brighter could not repair that mismatch.

The reflection now samples a single animated field projected from geometry in
view space. Two faces at the same geometric point therefore receive the same
reflection coordinate regardless of their texture islands. A small depth
component gives the projection a three-dimensional slant. The source texture's
original UV is used separately to preserve the alpha silhouette of eyes and
wings. Equal-depth testing, the neutral reflection color, vanilla glint strength
and speed controls, and the End egg's own portal material are retained.

This remains a stylized moving material, rather than a mirror of the world.
Its field can change with the viewing angle and object movement, but it no
longer restarts because a polygon uses another portion of the texture atlas.

The trader's previous glasses had a physical gap: the round outer lens ended
near x=2.20 model pixels, while its temple began at x=3.40. The mask's front
cloth and side loop also differed by 0.22 model pixels in depth. New overlapping
frame corners join each lens to its temple and hook. The cloth turns around
both cheeks, and two narrow elastic bands connect the sides around the back
of the head. These pieces follow the same sampled skull transform. Mask pleats
stand slightly in front of the cloth and overlap it, avoiding coplanar flicker.

The official comparison mode selects the original model texture and omits the
added shine, animal/theme overlays, and hats, allowing an unmodified reference
to be viewed alongside the customized Chao.

`python3 art/validate_surface_018.py` checks the actual twenty imported meshes
and egg shell. At 6,872 shared geometric edges with differing atlas UVs, it
sampled five positions and five affine camera views: 171,800 comparisons. The
largest new reflection-coordinate difference was 5.55e-17. The former UV field
disagreed in 32,775 sampled positions. The same diagnostic uses full convex
polyhedron intersections to confirm one connected component for the nine
spectacle pieces and one for the twelve mask pieces. Its report is in
`docs/validation-0.18/surfaces-and-accessory-joins.json`.

The completed 0.18 client study loaded the shader successfully and retained
665 whole-frame views with their asynchronous save callbacks. Final merchant
front/left/right/back frames visibly connect the front frames to the temples
and the cloth to its cheek and rear bands. The Chao shine view and both sides
of the egg shell no longer show the old atlas-island reflection cutoffs in
the inspected views. A 48-source-frame shine review is retained alongside
the raw PNGs. These observations are bounded to the captured scenes; the
geometry diagnostic supplies independent coverage across all twenty rigs.
