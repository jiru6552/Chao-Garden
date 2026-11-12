# Hero tip depth separation — 0.25.58

The current-cohort book front view exposed striped white/blue pixels inside a
Hero Power+Swim cap near the root join. The two cuboids share their Z center
and rotate only about Z, so equal depths gave overlapping coplanar front/back
faces. The visible diagnostic was personally inspected before this change.

The generator now chooses `cap depth = max(authored cap depth, root depth +
0.08)` at every one of the ten targets. Nonnegative partition weights preserve
that ordering through every age/opposed-axis blend. Both cap planes therefore
remain at least **0.04 model pixels** beyond the corresponding root planes;
they no longer cross through coplanarity while development changes.

Six targets per mirrored cap required an increase: twelve depth components in
total, at most **0.23 model pixels**, with only **0.03** needed at the fresh
corner. Other existing larger caps keep their authored depths. No head box,
local X/Y vertex, root/cap center or rotation, UV, wing, rig, material policy or
gameplay field changed. The source-change audit proves those records exact.
The fresh cap's stored vertex Z values and explanatory metadata necessarily
update along with the target depths and model hash. Slightly thicker caps can
change perspective side views; “unchanged silhouette” refers to local X/Y
construction, not pixel-identical screenshots.

The independent validator passed **15 positive closed boxes** and **2,205
connected age/axis blends**, preserving the original head/face/pivots and soles,
actual tilted-halo separation (minimum vertical gap **0.354107px**) and exact
mirrored wing hinges. New target and actual blended-vertex checks establish
minimum front/back root/cap clearance **0.040000000px**.

This pass ran the generator, independent Python validator and exact source/model
diff audit only. It ran no Gradle, native-server or Minecraft client. The root
coordinator must inspect new client captures before treating the striped-pixel
artifact as visually resolved. Earlier captures describe the pre-fix model.
