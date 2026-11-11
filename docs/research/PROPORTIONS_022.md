# Proportion audit — 0.22.0

The recovered authoring baseline is the complete verified 0.21.0 source archive,
SHA-256 `1ddc03d937194604053ebc5934124041866fdd1ebf7fb6956134ff869e5b34e5`.
`art/controls/proportions_022_control.json.gz` freezes all fifteen ordinary model
payloads before modification, plus hashes of all forty-one asset-only imported
inputs, ten special meshes, 351 Chaos coats and three Chaos material masks. The
control is authoring evidence and is not a runtime texture or model.

## Corrected families

| Family | Core before | Core after |
| --- | --- | --- |
| Hero Normal, Fly, Power | 7×7×6 | 7×6×6 |
| Dark Normal, Swim, Run | 7×7×6 | 7×6×6 |

The six changes shorten the core upward from its unchanged lower edge. Width,
depth, front face, eye planes and semantic joints stay fixed. The established
five-pixel Swim/Hero Swim head profiles remain shorter; the other ordinary cores
already have the six-pixel height. Amy, Omochao, Tails and Knuckles are preserved.
All three true Chaos skull cores already share the same 7×6.2×5.65 dimensions;
their crowns and translucent tips remain distinct.

Every young crest block is translated down by one model pixel. Fully aged
crest targets are translated by the matching difference between their original
and normalized core crowns. This preserves each individual block's dimensions
and the connections between steps. The optional Monster crown is reanchored at
the corrected young crown. Ordinary age and signed training coordinates retain
their existing independent ranges; the finalizer does not amplify them.

The runtime reads the same young/nine-target crown offsets as the geometry.
`emotion_dots` receives the age/axis blended offset after animation sampling.
The applied value is tracked on that resettable pose, so repeated development
calls and reversible age changes cannot accumulate translation. Imported exact
comparison rigs carry no correction metadata and remain unchanged.

## Independent verification

`validate_proportions_022.py` checks the correction numerically without calling
the builder. It compares the complete remaining model to the frozen control,
including all native parents, pivots, rotations, face topology and texture UVs.
Only the five independently inspected genetic-iris source-texel corrections are
permitted in Hero, Hero Fly, Hero Power, Dark Run and Dark Swim; they may change
the authored `colored_iris` lens face UVs and no eye vertices. Each sample must be
opaque neutral eye white inside the original eye UVs and outside the skin mask.

The static audit covers fifteen families, nine secondary endpoints and three
ages (405 cases), connected crest roots/steps and 600 internal arm/hand/foot
assemblies. The cuboid separating-axis test composes rotations in the renderer's
`Rz·Ry·Rx` order and includes an independent two-axis quarter-turn regression.
The limb check concerns internal rigid pieces; torso contact also depends on
the animated shoulder pose. All torso/limb geometry and joints must separately
match the original control exactly. Seven deliberately broken models verify
rejection of stretched heads, shifted front faces, wrong marker offsets,
stretched crests, displaced hands/eyes and altered head UVs.

`ProportionsVisualSmokePoseProbe` is an opt-in, production-excluded actual-client
probe. It checks the fifteen families at nine endpoints, three ages and five
moving poses, including fixed eye/front planes, bounds, marker reversibility and
all twenty exact source comparison rigs. Its expected normal completion is
14,215 assertions. That number is a test expectation, not a claim of execution.

The source preparation runs only isolated in-memory transformations; live
asset generation, the final independent CLI, Java compilation and the actual
Minecraft probe are centralized by the release owner. Final results in
`TESTING.md` supersede preparation-only status.

The earlier 0.21 determinism guard still rebuilds its own age targets, then
reapplies this finalizer whenever the current models declare the correction.
It therefore preserves the final 0.22 output while retaining standalone 0.21
behavior on unmarked resources.

Visual reference pages retained by the authoring work:

- https://chao-island.com/gallery/advanced-evolution/neutral.html
- https://chao-island.com/gallery/advanced-evolution/hero.html
- https://chao-island.com/gallery/advanced-evolution/dark.html
- https://chao-island.com/gallery/chao-types/chaos.html
