# First Neutral Garden — compact 0.25.2 reconstruction

The new garden uses the supplied SA2/Battle recording as its visual reference. Ten original native frames were reopened: `first-022`, `043`, `061`, `204`, `309`, `315`, `321`, `348`, `390` and `410` under `references-0.25.1/native-keyframes/`. They show a continuous low lawn, a nearby cluster of palms, a broad stepped cascade dominating one end of the crag wall, the narrow main cave and the transporter beside the opposite tapering cliff. The other horizon is open sky/ocean. This is an authored reconstruction; the source recording supplies no measured map or exact original mesh coordinates.

## Scale and layout

The old footprint contained 8,279 ground-mask columns. The new one contains 1,745, a **78.9% reduction in footprint area**. The occupied voxel bounding box changes from 101×91 to 49×41; the spans between outermost column centers change from 100×90 to 48×40. Those masks include crags and water. They are not measurements of walkable lawn or of the original SA2 level.

![Same-scale footprint comparison](references-0.25.2/garden-footprint-comparison.png)

| Feature | New authored position | Reference relationship |
|---|---|---|
| Arrival | `(5.5,80.1,-12.5)`, facing southwest (yaw 45°) | Arrival remains beside the cave mouth, with the cliff on the right and open lawn on the left. The waterfall is revealed after walking out, rather than visible from this exact arrival pose. |
| Cave return | `(5,80,-18)` | Three-block mouth, four-high throat in a continuous crag, short recess and a framed animated portal. |
| Main pond | Center `(-13,-1)`, radii 7×10; water Y79 | 179 filled source-water columns immediately beneath the fall. |
| Cascade | Channel X−17..−14, fronts Z−7/−10/−13/−16 | Four broad receiving shelves at Y82/85/88/91; solid rock closes the former waterfall cave. |
| Transporter | `(17,81,9)` | A one-block grassy hummock near the tapering eastern wall and open edge. |
| Palms | Eight trees, trunks four to five blocks high | Shorter, closer trees retain genuine fruit support and harvesting. |
| Cliff limit | A compact, two-cell invisible outer ring to Y255 | Protects walking babies, players, dropped items and flight while leaving the horizon visually open. |

The central walk from `(5.5,80,-11.5)` to `(5.5,80,-6.5)` stays on actual level turf with player-height clearance. Native appearance evidence must also include the arrival view, the transporter looking across toward the cascade, the pond looking toward the cave, a close cave view and an outward cliff view. The footprint diagram alone does not verify the player's experience.

## Lossless saved-world preflight

The broad old `BOUNDS` remains a cleanup and migration sweep. The new playable field is the compact `insideField` mask, with its separate `ACTIVE_BOUNDS`. Old terrain beyond the new field is cleared rather than left as a giant hidden lawn.

Before any block, provider or registry write, the upgrade prepares the entire terrain and care plan. It snapshots actual placed care blocks; joins connected trees and physically connected props; joins complete linked course components so all their nodes retain one translation; and preserves supported placements that already fit. Displaced assemblies move as intact block-state payloads to free locations. Rooted toys, saplings, tree trunks and every independent activity node require actual supporting turf. Waterlogged toys keep their exact state in actual source-water pond cells, rather than creating a new water pocket on dry lawn. Every affected route edge retains valid length, and provider claims and alias capacity are checked in advance.

Future authored coordinates do not establish ownership. A fruit pod is ignored as old scenery only if its actual block, facing and native palm crown/trunk/support identify an existing authored garden-fruit assembly. An owned ordinary fruit tree occupying a future palm position is preserved and relocated, including its fruit family and ripeness. A different care block occupying the future transporter cell is also preserved.

Every loaded player, Chao, egg and dropped item is checked against a read-only collision view of the proposed plan. Surviving body spaces are reserved first; displaced bodies receive separate dry supported landings. If care or body capacity is insufficient, an unknown inventory/block entity would be changed, or an affected active contest/managed companion cannot safely stay, the upgrade aborts before writes. It leaves that existing scene and its data intact. Known stateless native scene/palm render entities are the explicit block-entity exception.

After all preflight succeeds, the commit physically stages registered providers and moves their existing owner/name/member records through the validated Garden Registry alias API. Completed course owners and links move together before old physical removal callbacks run. All old moved fruit pods are removed first, then all crowns, then other old care blocks; only afterward does terrain placement install their new positions. An old crown cannot drop duplicate fruit or destroy a newly moved pod. Existing Garden Bells continue to resolve the old provider link after saving/reloading. The same creature/entity UUIDs and item payloads are retained. A real body move updates its physical last-seen record; a metadata alias does not pretend that a body moved.

Chao published after the scenery rewrite receive the narrow ordinary-tick rescue if they emerge in old rock or the removed outer field. Active contest and managed companion state is not reset. A previously unfinished raw Race Marker draft whose physical source no longer exists must be selected again; its item use clears the invalid draft and explains the action. Completed course graphs retain their data.

## Verification scope

The source scale report is labelled `SOURCE_SCALE_STUDY`, not runtime PASS. `garden_scale_study0252.py` binds the exact old/new source masks and the ten primary frames, and can additionally bind the actual current server geometry receipt. Current native server tests cover emitted footprint, all filled pond/shelf cells, physical edge collision, source-supported palms and flowers, saved identities and Garden Bells, completed course state, exact oriented toy state, waterlogged toys, whole fruit trees and prevention of duplicate fruit. Separate refusal cases cover an oversized assembly, an unknown chest inventory and exhausted physical body-landing capacity.

Final executed outcomes and limitations belong to `TESTING.md` and the current validation reports. Prior 0.25.1 captures and its larger 719-column pond are historical evidence.
