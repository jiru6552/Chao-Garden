# Physical toy navigation

A crawling Chao behind a south-facing TV must walk around to the screen-side
standing cell before starting its voluntary play session. The TV occupies a
partial block collision box. Under Minecraft 1.21.1's default
`BlockBehaviour.isPathfindable(LAND)`, every non-full collision box is considered
pathfindable. `WalkNodeEvaluator` then classifies that TV cell as OPEN, allowing
the planned route to go directly through a solid screen cabinet.

The live room study reproduced the mismatch: TV `(1,80,7)` facing south, front
standing center `(1.5,80,8.5)`, and a zero-point newborn approaching from
`(1.5,80,5.5)` stopped at `(1.5305,80,6.9250)`, the north collision edge.
Needs and eligibility were valid; the physical route never reached the screen.

`ChaoToyBlock` now declares its occupied cell non-pathfindable, following the
same vanilla contract used by `ChestBlock` and `LecternBlock`. Toys taller than
the ordinary 0.6-block step also report a fence-like path type through NeoForge's
block path hook. This prevents the path planner selecting a jump onto an
uncapped TV: a crawling Chao cannot travel far enough forward during the high
part of that jump to reach the cabinet top. The actual
collision shape, toy selection, screen-side requirement, play chance, approach
deadline and reward transactions are unchanged. Water toys are still approached
from their clear adjacent standing cell and mounted only by an admitted play
session.

The pinned source is `neoforge-21.1.219-sources.jar`:

| Class | Source SHA-256 |
| --- | --- |
| `BlockBehaviour.java` | `410ae028d36585b6c210e853720ea3fb973154a4d7800a082d541293cf355014` |
| `WalkNodeEvaluator.java` | `bea8355cdfb9ed464857397117856c16979d672f68045448d3cd98a82b0b179a` |
| `ChestBlock.java` | `2c6332d6b26e89cbbd9f48b6b73dd2488e8eb7eae4d61960831279c7a03c45e0` |
| `IBlockExtension.java` | `2dea052b03b569b82ca728073ab4299282eb4bb9116637b9ee19ade36975e5a5` |

`ToyApproachGameTests025` covers north, south, east and west screen facings in
eight isolated test arenas: four open-floor uncapped cabinets matching the live
garden setup, and four alcoves blocking the top and one flank. Each zero-Run
newborn starts behind the cabinet. Only the ordinary
`ToyContent.PlayGoal`, real path planner, body movement and voluntary interest
roll can bring it around the remaining side. The checks reject any path node
through the cabinet or its failed jump-up node, require a physical detour and arrival at the screen, then
require completion of the real session before its happiness reward. Existing
toy tests cover water mounting, ball movement, removal and save cancellation.
