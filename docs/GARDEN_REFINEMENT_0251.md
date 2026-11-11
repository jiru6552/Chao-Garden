# First Neutral Garden — 0.25.1 refinement

The supplied SA2/Battle recording is the layout reference. In particular, first-video 00:22 shows the waterfall to the right when arriving from the exit cave; 03:24 shows the pond facing the cave wall; 05:09 and 05:48 show the transporter ridge looking across the lawn toward the fall; and 06:30 shows the compact entrance in a continuous crag wall. See [the exact-frame spatial brief](references-0.25.1/GARDEN_SPATIAL_BRIEF.md).

## Spatial corrections

The earlier 0.25 garden reversed the cave-arrival relationship and used an almost uniform ellipse with excessive grass stair bands. The new authored layout puts the cascade and pond on the western side, the cave in the back crag and the transporter on the eastern grassy ridge beside the open horizon. This is an interpretation in Minecraft block units, not a measured extraction of the SA2 level mesh.

The main pond is a 13-by-19-radius oval centred at X−28/Z−2, truncated against the waterfall's foot. Its water surface is Y79 and its pale bed is Y76 or Y77. All 719 declared pond columns are filled to the same surface using source water. The four broad receiving shelves stand at Y83, 87, 91 and 95, with six-block depth each; each has a complete stone floor and side/rear containment. The dark waterfall cave is replaced by solid rock. No tall palm or its six-block fronds is placed within the waterfall clearance.

The cave's actual mouth is three blocks wide and four high. It is carved into the existing tall rock mass, with a four-block throat and a solid wall behind the portal. There is no separate flat awning, hollow cutout above it or wooden sign. The shared animated End-style portal has its own complete quartz frame and visible threshold. Arrival is near the mouth facing the lawn. The garden remains in Adventure mode with the project's dedicated care-item permissions.

The front and western sky edge remain visibly open. Invisible barrier blocks follow its finite footprint through the dimension's build height, including a high invisible cap, so walking creatures, dropped items and high throws/flight cannot escape over a short fence. This does not add a visible retaining wall. The normal Chao World out-of-bounds rescue also remains in place.

## Foliage and materials

Eight palms stand on actual dry turf. The compact crown/support/fruit blocks still use the real Garden Fruit ripening and harvest implementation. Flower clusters are placed only after checking the actual final plan's turf support and empty space, instead of recomputing a height that ignores the pond. No authored flowers float over water or air.

A separate deterministic material pass revises the three existing garden materials: cooler grey fractured limestone, mixed green turf with less lime saturation, and a quiet pale shoreline. One derived turf-edge sprite gives vertical grass-block sides a thin grass lip over rock, avoiding metre-thick green crest bands. The native model and five-sprite atlas catalogue are validated together. There is no new block registry or furnishing dependency. The asset CLI runs this pass after the original lobby/garden generator so a full rebuild retains the new materials.

## Existing-world update

The scene revision remains bounded to the garden. It never deletes entities. Keeper-placed care blocks are retained through the existing type-based preservation rule. The 44 known old authored fruit positions and original fixed transporter are explicitly excluded from that rule, so old floating fruit does not survive as a supposed user placement after its old tree is removed.

An already loaded Chao, egg, dropped item or keeper that becomes obstructed or newly immersed by the moved terrain is moved to a nearby clear turf position using the same entity, UUID, owner, state and item contents. An existing swimmer is not moved merely because it is already in water during an unchanged rebuild. Entity sections that publish after the terrain rewrite receive the same narrow rescue on ordinary server ticks, only when their body actually intersects authored limestone, turf or sand. Ordinary swimmers, user props, managed companions and active contests are excluded. Terrain generation does not grant eggs, Chao, animals or other acquisition rewards.

A registered old transporter migrates its exact keeper, name and member UUID ledger before the old physical block is removed. The destination is preflighted and physically staged first. If the new ridge provider is already claimed, a separate free provider is installed nearby without merging keepers; a lack of a safe destination retains the old provider and registration. Old linked bells resolve a bounded, validated, owner-consistent relocation record even after SavedData reload. Removal callbacks always use raw block positions so tearing down the old provider cannot delete its migrated registration. Physical last-observed body positions remain unchanged until an actual body move records its new location; race/recovery coordinates are not blindly redirected.

## Validation contracts

The new actual-server geometry tests check every pond column and receiving shelf after 100 ticks in genuinely ticking chunks; physical source-fluid and solid-bed state; supported dry flowers; eight dry palms with a fall clearance; sealed waterfall rock; the compact real portal and continuous mountain; and absence of the old wooden sign. Physical Minecraft collision moves a real keeper, baby Chao and dropped item against the open edge at both ground and throw/flight heights. A bounded-update fixture verifies same-UUID/state preservation, safe body positions, retained placed toys and removal of the known old authored pod/transporter positions.

Final executed results and native client views belong to the release validation report. This document describes intended contracts and does not substitute static source inspection for those runtime results.
