# Neutral Garden water feature, 0.25.3

The cascade now occupies the western wall and flows east. The cave return portal
stays on the northern wall, facing south. This changes the actual terrain,
terraces, vertical water faces, receiving pool and banks by a quarter turn.

The independently checkable mapping from each old water-feature coordinate is
`(oldX, oldY, oldZ) → (oldZ - 4, oldY, -oldX - 21)`. The compact playable mask is
unchanged: 1,745 ground columns within the existing 49 × 41 coordinate box. Every
authored water source stays inside that mask and its invisible safety ring.

| Landmark | Current coordinates or direction |
| --- | --- |
| Cave return portal | `(5, 80, -18)`, facing south |
| Transporter | `(17, 81, 9)` |
| Arrival lawn | The existing dry walk at `X=5`, `Z=-12..-6` |
| Pond center | `(-5, 79, -8)` |
| Pond surface | 179 source columns, `X=-10..4`, `Z=-14..-2` |
| Cascade direction | East, `+X` |
| Cascade fronts | `X=-11,-14,-17,-20`, at heights `82,85,88,91` |
| Cascade channels | `Z=-7..-4`; solid banks at `Z=-8` and `Z=-3` |
| Highest source shelf | `X=-22..-20`, `Y=91`, `Z=-7..-4` |
| Solid western backdrop | `X=-23`, `Z=-8..-3`, through `Y=92` |

Each falling face joins a source-filled receiving shelf immediately east of it.
The lowest face joins the rotated pond at water level 79, so there is no dry gap
between the cascade and pool. The rear wall remains continuous around the cave;
the foreground horizon remains open above the existing invisible safety wall.
The seven existing arrival/care tiles at `X=5`, `Y=79`, `Z=-12..-6` keep their
level turf surface where the rotated dry sand rim reaches the approach. This
does not alter any of the 179 pond source columns. A real-player collision walk
checks seven metres of this route with level turf support at every step.

The former native palm at `(-1,-8)` now stands on dry shore at `(-8,5)`. All eight
palms remain grounded. Upgrade cleanup recognizes the old ripe pods only when
they still have their native palm trunk, crown and support blocks; ordinary
owned fruit trees at those coordinates remain preserved care assemblies.

The existing migration first fits and reserves every owned care assembly and
body, then writes terrain. Registered providers retain their owner/name, member
entries and saved Garden Bell aliases. Connected purchases retain relative
positions and exact states. Waterlogged toys move only into actual source
water. A formerly dry standing body is relocated when the new pool would place
water at its feet or immediately below, while an existing unblocked swimmer
stays in place. An unsafe fit still aborts before terrain or registry writes.

`GardenGeometryGameTests0251.allPoolCellsAndReceivingBasinsStayFullWithSupportedPlants`
checks the exact independent transform of every old pond and terrace cell,
including supported beds, eastward receiving faces and actual source stability
after 100 ticking server ticks. It also checks grounded flowers and palms, the
closed waterfall cave, and the unchanged framed cave portal.

`GardenWaterfallGameTests0253` adds two focused tests:

- `garden0253_water_turn` checks actual western source coordinates, every planned
  source inside the compact safety wall, all 179 pond columns, perpendicular
  cave/cascade directions, and the dry arrival walk.
- `garden0253_owned_turn` installs genuine revision-27 support cells, native palm
  geometry and ripe pods, a registered provider with an adjacent oriented TV, an
  offline saved Bell, a waterlogged duck, an owned Chao and dropped diamonds. It
  loads a saved revision-27 marker and invokes the real `HubContent.ensure`
  upgrade. It checks identity/payload preservation, supported dry landings for
  both a newly buried body and a body standing above the new pool, exact toy
  state, old native-pod cleanup, and repeated-ensure idempotence.

These descriptions are test scope, not execution results. Execution results and
native captures belong to the 0.25.3 release validation record. The 0.25.2 scale
study and its old north-facing water diagram remain historical artifacts.
