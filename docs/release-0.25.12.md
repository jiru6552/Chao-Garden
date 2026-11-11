# Chao Garden 0.25.12 — fruit, seeds and gardening

All 35 active foods and 33 active seeds now have authored low-poly 3D meshes and
32×32 inventory icons. Retained Stamina, Mint Candy and mushroom-seed resources
are also updated without adding new acquisition paths. Inventory icons are
rendered from the same geometry used for held and dropped items. Hanging crops
use matching shapes at four growth stages; planted bulbs have two growth stages.

The SA2 garden nuts, blue Round Fruit, green Square Fruit, yellow Triangle Fruit,
Heart Fruit, cyan Chao Fruit, curved Tasty Fruit, pointed Strong Fruit and green
mushrooms follow inspected game-render references. Seeds are sprouting bulbs,
including the pink/yellow Hero seed and brown Dark seed. Portable-game and
CWE-inspired foods retain their distinct established colors and use authored
interpretations. These are reconstructed Minecraft assets, not extracted game
models. No original reference-image pixels are embedded in the new textures.

## Using the gardening tools

Both **Shovel (20 emeralds)** and **Watering Can (25 emeralds)** are permanently
available in Black Market stock, without a race requirement. These prices are
Minecraft adaptations. Existing traders refresh their saved stock automatically.

Give a tool to a Chao you can care for. It considers the actual item, accepts one
to learn the ability, and retains that ability when saved or carried. A Chao that
already knows the ability returns a duplicate tool. The shovel remains placeable
for the existing sandcastle activity.

Give a seed to a Chao with a shovel and **at least 50 Run points**. It walks to a
reachable clear planting spot and plants after a short digging gesture. It keeps
the real seed until placement succeeds. Blocked planting, unsuitable ground,
full gardens and interrupted/reloaded actions cannot create an extra tree or
silently consume the seed. Players can also plant seeds directly.

Chao that know watering may tend nearby trees while idle. Players can use a
watering can on a seedling, trunk or crown. Watering advances a seedling's growth,
or ripens all actual fruit on one mature tree. Seedlings still require bright
soil and clear space for the existing seven-block-high, five-block-wide canopy.

## Planted-tree lifecycle

Each authored garden allows **four newly planted trees**, including pending
seedlings. This is not a global cap on Overworld orchards. Pending seedlings
reserve space for their future canopy.

New planted trees live for **60 active minutes after maturity**, or **120 minutes
for Strong Fruit trees**. Watering gives a small ten-second life extension, at
most once per ten seconds per tracked tree, without banking lifetime beyond a
fresh tree. A withered tree loses its fruit and foliage, then its remaining wood
disappears after one active minute, freeing its planting slot. Foreign blocks in
the tree's footprint are preserved. These timings are explicit Minecraft
adaptations; they are not claimed to reproduce original game timer values.

Tree aging runs only in loaded areas with a living player within 64 blocks.
There is no offline catch-up and no forced chunk loading. Authored garden trees,
natural Overworld trees and existing untracked orchards remain permanent. Only
trees planted through the new seed interactions enter the lifecycle tracker.

Hero/Dark fruit consolidation, prior garden scenery, supplied music, fruit
effects and save aliases remain intact. No official race system is added.

## References and scope

- [SEGA's original Sonic Adventure 2 Chao screenshots](https://sonic.sega.jp/sonicadv2/0601/shots_e.html): official environment reference.
- [Chao Island — Tree](https://chao-island.com/wiki/Tree): user-provided mechanics reference; permanent versus planted trees, planting ability, watering, capacity and lifespan behavior.
- [Chao Island — Seeds](https://chao-island.com/wiki/Seeds): planting prerequisites and seed identities.
- [Chao Island — Black Market](https://chao-island.com/info-center/kindergarten/black-market.html): inspected original-game fruit and seed renders. This is a community reference containing game imagery, not an official SEGA publication.

No additional game extraction was required for this pass. The reference URL/hash
index is retained in `docs/research/fruit-references-02512.json`. Validation and
remaining limits are recorded in `TESTING.md`.
