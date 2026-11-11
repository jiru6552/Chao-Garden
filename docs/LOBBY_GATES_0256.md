# Hero and Dark lobby gates, 0.25.6

The two user-supplied game images (`trgewd.png` and `tyrhegfw.png`) are the
visual references for this revision. The meshes are original geometry; no
extracted game models or textures are bundled.

The Hero gate has long round gold shafts, ivory square capitals with recessed
Chao badges, a shallow open lintel, white top orbs with tilted gold halo rings,
and two winged Hero Chao on forward plinths. The Dark gate has a broad purple
brick crown with flared shoulders, pale base and pier bands, blue-violet fire
sconces, and two horned, bat-winged Chao with tridents on forward plinths.

`GardenGateGeometry0256` emits the complete local meshes. The lobby renderer
positions them using the current `HubRooms` anchor and facing, so stairway
relocation does not leave the sculpture behind. Plain stone, gold, and fire
colors render separately from Dark masonry. The latter uses Minecraft's
stone-brick texture with purple tints and world-unit UVs; the arch slices do
not each repeat a stretched whole brick tile.

Both real portal apertures remain 3 blocks wide and 4 high. Their crown
chords match the native 24-segment portal outline, with 0.04 blocks of visual
clearance. The meshes contain front, back, and reveal faces but no opaque
portal backing. The sculpture is decorative and leaves the central walking
route open.

`GardenGateGeometry0256Test` checks the actual emitted vertices for finite,
grounded bounds, reference-specific ornaments and material separation,
sweeps the native portal openings against front and rear faces, and rejects
an injected opaque backing. A focused internal oblique mesh view was also
reviewed against both supplied pictures. The native client smoke check is
recorded separately by the release validation workflow.
