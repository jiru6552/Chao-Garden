# Curved school doors — 0.25.5

The user sketch replaces the former seven broad stair steps with a circular crown. Each existing school arch remains three blocks wide and three blocks high. Its outer radius is 24 model pixels, its inner radius 22.5, and both spring at 24 pixels above the sill. The radial colored trim is therefore 1.5 pixels (0.09375 blocks), slimmer than the previous 2-pixel trim. The corner infill uses the actual `school_cream_wall` texture and wall depth.

Ninety-six angular samples keep both horizontal and vertical increments below one model pixel. Native block models express this fine curve as narrow cuboid strips. The leaves follow the conservative lower envelope of the inner circular opening, with 0.25 pixels of clearance. The two leaves meet at the center, keeping an exact-center crosshair usable. Their complete round badges retain the original pixels and physical aspect; decorative borders now follow the crown. Rigid UV coordinates keep handles and fronts/backs attached to the same physical leaf after it swings.

No door or frame registry ID, blockstate property or model name is replaced. Existing saved fifteen-part pairs resolve the new model and native shape immediately when the new resource pack loads; revision 30 also refreshes the room plan. The old six-root compatibility path and native fifteen-cell prediction/rollback remain intact. The separate steel tool cabinet deliberately retains all 43 of its original rectangular assets byte for byte.

`generate_school_doors_0255.py` is the final postpass after the previous school and door writers. It emits both native geometry and the corresponding models. Native shapes are built once for every handed/open/section/height/facing combination using balanced `Shapes.joinUnoptimized` unions and one final optimization. Picking never reconstructs the detailed shape per ray.

`validate_school_doors_0255.py` checks the independent circle equation, fine contour error, fitted slender frame, wall material, owning-cell clips, rigid front/back UVs, original badge patches and unchanged cabinet. Its malformed fixtures restore rectangles, reverse art, thicken trim, remove fine arc members or change the cream material; each must fail. `test_school_door_geometry_0255.py` compiles the real dependency-free Java class and compares all 36 canonical shapes with independently derived clips.

The retained native door matrix covers 288 valid panel uses and 32 empty outer cuts across every facing and swing state. `SchoolDoorCurveGameTests0255` additionally compares all 216 actual packaged leaf meshes with native picking/collision and casts 320 rays immediately below/above the mathematical curve, so both old coarse tops and rectangular hitboxes are rejected. The central native runner owns execution of those cases.

## 0.25.7 frame-edge depth

Ornate leaves now occupy canonical depth 14–16 model pixels, with a hinge at 15, instead of the former 7–9 / hinge 8. Each authored facing points toward the requested edge: classroom East, clinic/Fortune West, Principal South, inner exit North, and external entrance South. Therefore every authored ornate pair moves 7/16 block toward the foyer or exterior without moving its frame, signage or centre anchors.

Leaves swing inward around that facing-edge hinge. Their existing TIP state is reserved on the opposite side of the centre, keeping all 15 state-owning cells bounded. Selection, collision, opening fronts/backs, badge UVs and native prediction use the same geometry. The steel tool cupboard keeps its projecting case and forward swing unchanged.

A complete old player-built 15-part pair migrates its six forward TIP slices on first use only when its new rear cells are air. It preserves the roots, centre and open state before the requested toggle, and never replaces masonry. Six-root expansion remains conservative. Breaking any slice of a complete old or current pair removes the whole pair and drops one item. The room refresh rebuilds authored pairs in the new arrangement.

Current native cases additionally assert actual authored frame-edge depth, exercise all four facings and two states of old 15-part first-use migration with blocked-space refusal, and test both old/current 15-part teardown. All tests keep exact native ray, collision, ownership and item-count assertions.
