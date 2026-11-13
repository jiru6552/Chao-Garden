# Neutral Run-first .62 authored construction audit

The source model has 19 positive closed cuboids: eight head features, five compact body/hand/foot targets, four small wing lobes and two tail sections. It retains all 59 canonical joint names, order, parents, pivots and rest rotations. Original skull, face, expression and orb bone/mesh records are exact; the original 7×6×6 box is not a morph target. The original head-decoration list remains intact so existing hats can hide crown features through the established renderer.

The older official Run JSON was inspected alongside its texture and converted model. Its construction vocabulary is a two-piece central swept crest, two stepped rear points and separate front-side details. The new geometry uses that small cuboid vocabulary on the canonical baby topology, with authored centers, sizes and rotations. No viewer mesh, source-game morph logic or donor vertex arrays are imported.

I personally opened the fresh viewer front/side; balanced back; all nine mature side originals; and the mature Fly front/back. The independent viewer agent also researched the full family. The observed identities guide these targets:

| Later influence | Authored direction and proportion |
| --- | --- |
| balanced | Short up/back central crest, rear-facing side points, larger upright front nubs than fresh. |
| Run | Long central and side points hook back/down; tiny wings remain compact. |
| Power | Taller central crest and side blades rise up/back, with different proportions from Fly. The locked first type stays Run. |
| Fly | Three tall nearly vertical prongs with narrow upright front nubs; wings remain small. |
| Swim | Rear points compress into short broad lobes; the tail becomes a longer broad upturned paddle. |
| Run+Fly | Shorter back/down central and rear blades, upright front nubs, moderated compact wings/tail. |
| Run+Swim | Moderate back/down points and a larger tail, between Run hooks and Swim's compressed lobes. |
| Power+Fly | Upward three-prong form with moderately different central/rear proportions. |
| Power+Swim | Moderate up/back points and a broad long tail. |

The generator emits one common fresh adult target plus a fully signed 3×3 mature lattice. The baseline Run-first generic system already supports Power exceeding Run; the asset has genuine positive-Power targets rather than treating that side as impossible. Fly and Swim remain opposed. Pair order shares one target. The viewer agent measured original knob pixels and confirmed full endpoint controls on both compatible axes; the brief suspicion of half-range coupling was discarded. Mixed endpoint shapes are moderated rather than simply stacking both pure endpoints. Bilinear interior timing remains a stated approximation.

Front nubs sample the original opaque hand texel (28,32), independently of blue/darker head accents. Head roots use reserved body texel127 and positive-Z tip faces use126. Wings span125→124 across all four long faces per lobe, so distal pigment occupies broad surfaces rather than only a thin end cap. Tail tips use the independent original tail texel (50,32). No fifth semantic texel or extra decorative shell is introduced.

All wing proximal-face midpoints stay exactly at the canonical local [0,.25,0]. Initial variable-angle lobes drifted under independent linear center/rotation blending. Fixed -28/+18-degree lobe directions, with variable relative spans/heights, solve that within authored geometry; no new API or relaxed hinge assertion is needed. The small wing direction is therefore simplified. Tail roots overlap the retained original tail base, and the two sections overlap each other. Equal initial root/tip widths created coincident outer planes at some endpoints; narrower tips remove those faces without removing the shared volume.

The independent source validator passes 9,773 dense/random fully signed age/axis samples. It checks 12,665,808 arbitrary-rotation head/feature face pairs with zero positive-area coplanar overlap; eyes and the original tilted orb remain clear. Head features, wings and tail sections stay connected; tail roots intersect the retained original base; soles and heel planes remain anchored. All 19 volumes are positive (minimum 0.14364 model-pixel³). The maximum sampled five-percent age step is 0.255352842 model pixels. It separately checks 3,024 physical wing-face UV samples, including 1,368 substantial root and 1,368 distal samples. All seven delivered earlier gameplay assets retain their expected hashes.

This is source geometry validation, not an actual Minecraft visual, full motion, multiplayer or packaged-JAR claim. The coordinator and production rig/material tests provide those separate outcomes. Two-piece crest steps, cuboid tips, fixed wing directions, original box proportions and bilinear interiors are deliberate first-pass approximations.


## Current nub and stripe revision

Native preflight exposed the mature front nubs as mostly buried by the unchanged square head. The approved correction raises only their mature root anchors from Y−4.05 to−4.80; fresh anchors, UVs, dimensions, rotations and every other feature remain unchanged. Lower nub sections still positively intersect the skull through the full 9,773-blend sweep, while balanced/Fly/Run/Power tops visibly clear the original crown plane. The initial 84-static preflight review is diagnostic evidence for that earlier geometry, not this correction.

The user then explicitly requested moving forehead stripes onto the headTOP and up the central spine/tip. This is an authored placement choice, not an exact viewer feature location. Material062 clears the old forehead/side strokes and paints the original top-head UV pixels. The existing central crest root and tip map only their local-negative Y broad face 4 to a verified-unused 7×2 native atlas tile 112..118×32..33. Four body columns alternate with three marking columns. Padded UV coordinates map physical localX width and localZ proximal-to-distal length, preserving body background and three parallel lanes along both pieces. Other five faces stay unchanged, including the permitted darker tip endpoint. No new geometry, rig changes or head/face/orb UV edits were made. A byte-structural comparison against nub-adjusted modelCCD2 proves only these two face UV arrays and descriptive metadata differ.

The final strict source validator again passes 9,773 dense/random signed blends and 12,665,808 face pairs with zero positive-area overlaps. It adds 4,500 physical central top-face UV samples over all ten targets (2,880 body and 1,620 marking samples), plus seven-lane continuity probes, while retaining all previous eye/orb/connection/positive-volume/sole/heel/wing-UV bounds. Model SHA is `cfed60309de43bc854272a4869d455d9ec76281a37a1787ff223f69b7ed85881`; metadata SHA is `20d1602340666c4f87fdf479277cb019308642d3ebad76ef3aae43926c18ced5`. These source results do not substitute for the coordinator's fresh actual client capture or packaged-JAR verification.
