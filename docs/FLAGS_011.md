# Triangular race markers — 0.11

Race Markers now use a red triangular pennant on a slim silver pole. The
cloth is actual thin, stepped cuboid geometry: seven one-unit rows taper to
a single point, rather than painting a triangle onto the old thick rectangular
block. Subtle red folds and a small stitched edge remain readable at Minecraft
inventory scale. The inventory view faces the pennant directly, while held and
placed markers retain their three-dimensional model.

The marker follows the same two placement orientations as before. Its selection
outline rotates with the model and follows the pole and pennant. It remains
non-colliding, so race participants can pass through it.

The item and block ID remain `chao_garden:race_marker`; existing markers adopt
the new appearance without losing their stored course links. The recipe now
uses red wool and two sticks, yielding four markers, to match the pennant.

Use a marker on the starting line or an existing flag, then place the next flag.
Each route leg still has a **32-block maximum**. Use the linked marker on the
ending line to finish the course. The placement preview, course ownership,
loop prevention, shared lobby and race rules are unchanged by this visual pass.

The supplied red pennant image guided the silhouette and pole treatment. The
Minecraft geometry and pixel textures are newly authored. Rebuild them with
`python3 scripts/generate_activity_assets.py`, or use the ordered complete asset
rebuild script. See [Gardens and activities](mechanics/GARDENS_AND_ACTIVITIES.md)
for the full course-building controls.
