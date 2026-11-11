# Portal and lobby refinements — 0.25.1

The attached SA2 garden arch is the reference for the lobby's garden entrance. Its replacement uses nine irregular radial masonry pieces, pale carved spiral columns, plant urns, flowers and two small white Chao statues. The old moss-topped rectangular lintel is removed. The Kindergarten gate's roof and arched infill are solid prisms with both caps, every underside and depth reveal; looking up no longer exposes an empty shell.

## Real vertical portal surfaces

`PortalScenes0251` reuses the existing internal `lobby_scene_anchor`; it adds no obtainable scenery item or new block ID. Its native state has `portal`, horizontal `facing` and `portal_style` properties. The ordinary lobby anchor remains `portal=false`, and palm rendering retains its separate branch. Portal anchors return from their own render branch before any sky or whole-lobby geometry is emitted.

Each aperture uses Minecraft's actual `RenderType.endPortal()` material and its animated projected star layers. The renderer emits position-only vertices for both opposite face windings at Z ±0.001, then flushes that material. There is no opaque black backing. The frame is a closed smooth-quartz band with front, back, inner reveal and outer reveal faces; a small quartz threshold remains visible above the standing floor. The server uses the same convex aperture polygon for its travel checks.

The helper's anchor X/Z are the center of the block; Y is the aperture's standing-floor height. `PortalScenes0251.state(facing,style)` also supports the school's private plan type.

| Scene | Anchor | Facing | Aperture |
| --- | --- | --- | --- |
| Hub Kindergarten | `(0,80,-23)` | South | 5.6 wide × 6.15 high |
| Hub first Garden | `(0,80,23)` | North | 4.9 wide × 5.4 high |
| School return | `(0,80,49)` | North | 2.4 wide × 3 high |
| Garden return | Garden owner's `EXIT_PORTAL` constant | South | 2.4 wide × 3 high |

The compact return's outer rim reaches 1.48 blocks either side and 3.28 blocks above the base, fitting the actual three-block-wide rock mouth. The mountain wall behind it stays solid. No wooden sign or excavated oversized tunnel is part of the return design. The two freestanding hub gates can be viewed from behind; the cave's solid mountain is deliberately not hollowed out for a backside screenshot.

## Gazebo and celestial dome

The upper gazebo disk now has an opaque closed core, and its visible energy face writes depth. EXIT labels sit 0.05 blocks in front of the crest's front face and 0.05 behind its back face. Each points outward and uses a culled, depth-writing text material. The old second label floating at the opposite side of the gazebo is removed. These changes prevent crest z-fighting and mirrored text showing through the energy disk.

The sky generator replaces abrupt cloud-envelope changes at latitude 0.38/0.58 with smooth fades, blends cloud light continuously, copies the equirectangular longitude endpoints exactly and blends each polar row toward its mean cloud tone across a smooth cap. This removes cloud cutoff rings, the longitude seam and pole pinwheels while preserving the lower celestial view through the lobby floor.

## Validation

`PortalGeometry0251Test` contributes 1,210 pure-Java checks to `MechanicsTestSuite`. It verifies closed rim edge ownership; rejects each individually missing face, duplicate sheets and a cracked seam; checks winding, full standing-player clearance, bounds and nonfinite inputs. `art/validate_portals_0251.py` passes 238 source/material/image assertions and rejects 12 malformed shader, one-sided surface, opaque backing, uncapped roof, text, ceiling and sky fixtures. These tests do not replace native Minecraft rendering; current front/back, underside, portal motion and travel checks belong to the final 0.25.1 client/server reports.
