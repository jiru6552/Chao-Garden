# Classroom play city 0.25.8

The painted houses, towers and trees now have compact, separate silhouettes.
Their x/z geometry stays inside their own block footprint, and structural
cuboids meet at shared faces. The house fronts contain fitted coloured door
and window inlays rather than intersecting a solid front-wall box.

The seven scenic pieces occupy the rear and side strips of the classroom.
The old concrete placeholder and second decorative ball were removed. The
south classroom floor is reserved for the four ambient pupils, their actual
ball and rattle, and the existing owned-student seats. The lesson desk and
back-wall artwork remain clear of all city props.

`ClassroomToyGeometry0258` provides the actual roof/canopy bounds to the native
outline and collision callback. The resource guard verifies every structural
cuboid, connected assembly, world placement, wall/desk/play-area clearance and
the native bounds. Malformed regressions include overlapping house/tower
placements, embedded inlays, oversized crowns, detached caps and the old
short collision box. A native layout case also checks all six models against
the actual collision/outline callbacks in every horizontal facing.

The changes reuse the existing painted palette textures and finite block IDs.
No extra furnishing mod or new item acquisition is required.
