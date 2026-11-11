# Chao fruit orchards - 0.25.13

Thirty-three non-mushroom fruit families have seeds for renewable Minecraft orchards. Mushroom and Nutritious Mushroom remain care foods without new seeds. Their first acquisition follows two routes:

| Initial source | Families |
| --- | --- |
| Wild trees | Garden Fruit in the Overworld, Dark Garden Fruit in the Nether and Hero Garden Fruit in the optional Aether |
| Rotating Black Market **fruit** offers | Hero Garden, Dark Garden, Round, Triangle, Square, Heart, Chao, Mushroom, Nutritious Mushroom |
| Rotating Black Market **Adventure fruit** offer | Energy, Peace, Smart, Tall, Vitality, Rare, Wonder, Creepy, Rough, Forbidden |
| Rotating Black Market **CWE fruit** offer | Orange, seven GBA colors, Shiny, four Hyper fruits |

Buy a market fruit or one of the seven listed seed products from rotating trader stock. Non-mushroom fruit crafts into one matching seed. The merchant seed list is **Tasty, Round, Square, Triangle, Dark Garden, Hero Garden and Strong**; Strong appears in every stock with two other distinct listed seeds. Four fruits appear per stock: two shape fruits, one other modern market food and one Adventure fruit. Adventure fruit costs eight emeralds; its seeds are obtained by crafting the fruit, rather than direct seed trades. Farmers sell only Garden, Hero Garden, Dark Garden, Strong and Tasty seeds. [LOOT.md](../LOOT.md) gives prices.

New chunks in eligible biomes get a fruit-tree placement roll with a one-in-eight chance. Each dimension has one wild fruit species; suitable soil and clear space are required, so an attempt may place nothing. Successful natural trees start with ripe fruit. Existing explored chunks are not retroactively planted.

## Growing and harvesting

Plant a seed on dirt or farmland in a bright place. Trees stand **seven blocks tall** with a **five-by-five canopy**. Leave seven clear blocks above the planting position and a clear five-by-five area from the fourth block upward. The two-stage seedling grows with random ticks or bone meal. Growth checks the whole space before placing wood or foliage.

A trunk and four real wood branches support three layers of biome-tinted `LeavesBlock` foliage. Leaves use normal distance, decay, shears, Silk Touch, waterlogging and player-placed persistence. Remove all supporting wood when felling a tree. Fruit-specific crown loot, ripe fruit and seed trades supply seeds; ordinary foliage does not randomly introduce other fruit families.

Four pods hang from the branches. Their four stages retain each fruit's authored low-poly 3D silhouette as size and color mature. Right-click a ripe pod for one fruit; its stem remains and grows another. Bone meal advances a pod one stage, or advances all four through the crown. Regeneration requires the crown, branch and actual leaf support. Removing that support removes the pod; a ripe broken pod can drop its fruit.

Mushroom seeds are removed from new seed recipes, creative entries and planting. Both mushroom foods remain available from the trader. Their old seed/block IDs and existing placed orchards remain solely for saved-world compatibility. Mushroom and Nutritious Mushroom share their green, white-spotted appearance but have different feeding benefits. Hover over the item for its effect; [MECHANICS.md](../MECHANICS.md) lists the totals.

## Gardening tools and planted-tree lifespan

The Black Market always sells a Shovel for **20 emeralds** and a Watering Can for **25 emeralds**. Give one to a Chao to teach the corresponding persistent ability. A Chao with a shovel and **50 Run points** can accept a seed, walk to clear nearby soil, and plant it. An interrupted or blocked attempt retains or returns the actual seed. A Chao that already knows a tool returns duplicate gifts. The shovel also retains its sandcastle use.

Idle Chao that know watering may water nearby seedlings or trees, including the permanent palms and Dark Garden trunks. Players can use the can directly. One watering advances a seedling stage or ripens all fruit on a mature tree.

Each authored garden permits **four newly planted trees**, including seedlings. Overworld orchards have no global four-tree cap. Newly planted ordinary trees live for **60 active minutes after maturity**, Strong trees for **120 minutes**, followed by a one-minute bare-tree phase. Watering adds ten seconds of life, at most once per ten seconds, without extending beyond a fresh tree's lifetime. Only loaded trees with a living player within 64 blocks age. These are Minecraft timings, not extracted SA2 timer values. Natural trees, authored scenery and pre-upgrade untracked orchards remain permanent.

All inventory icons are **32×32 renders of their corresponding native 3D meshes**. Seeds use single sprouting bulbs; hanging fruit and placed seedlings retain their family's colors and shape. See [the current reference notes](release-0.25.12.md).

## Saved orchards and foods

The separate Hero Fruit and Dark Fruit have been consolidated into Hero Garden Fruit and Dark Garden Fruit. Saved fruit stacks, seeds, pods, crowns and seedlings load as the matching garden variant; stack components and orchard growth properties are preserved. Both garden fruits retain +40 Stamina per meal and now shift child alignment by +0.15 / −0.15. Adult alignment remains locked.

Old small palm crowns still produce their saved crop. Bone meal expands one when sufficient space is clear, preserving its fruit family and each pod's ripeness before the usual one-stage increase. Obstructed upgrades leave the tree intact and do not duplicate its crop.

Smart Fruit is active again as a confirmed Chao Adventure 2 fruit, preserving its existing saved item/tree IDs. Stamina Fruit and Mint Candy remain saved-item compatibility foods, hidden from the creative collection and absent from new recipes, trades and loot. Existing Stamina trees and seeds remain usable. The registered tree system includes all 35 active foods plus the legacy Stamina family, while new seed acquisition covers the 33 non-mushroom active families.

## References and validation

The [SA2 fruit catalog](https://chao-island.com/wiki/Fruit), [Fruit (DC) SA2 section](https://chao-island.com/wiki/Fruit_(DC)) and [Black Market guide](https://chao-island.com/info-center/kindergarten/black-market.html) informed the fruit identities and benefits. [The 0.15 study](research/FRUIT_SA2_015.md) and [0.18 source review](research/FRUIT_CORE_018.md) record the adaptation and inspected renders. Only Chao Adventure **2** additions are included; modern SA2 Battle effects win for overlapping fruit identities. The supplied Marketplace world contains no matching Chao fruit assets; sprites, pod shapes and trees are authored here.

Minecraft adaptations include natural Overworld discovery, emerald prices, fruit-to-seed crafting, direct planting, large leafy trees, bone meal and right-click harvest. The mod commits one whole-fruit effect after the Chao holds, considers and completes its eating animation. A full Chao or one that dislikes the offered fruit can refuse it.

`FruitGameTests` exercises natural-family selection, fruit-to-seed crafting, tree growth/harvest/regrowth, obstruction protection, safe old-tree expansion and vanilla leaf behavior. See [TESTING.md](../TESTING.md) for completed run results.

Gardening polish in 0.25.13 uses an exact reachable standing spot. The watering can raises, pours at tick 40 and lowers through tick 55; moving the Chao beyond three blocks cancels an uncommitted pour.
