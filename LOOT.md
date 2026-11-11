# Survival loot and crafting - 0.24.0

All loot is additive and preserves the original tables. Regional care rewards use NeoForge's `neoforge:add_table`; the generic training modifier uses `chao_garden:generic_chest_training`, and eggs use the unified `chao_garden:chest_eggs` policy. The modifier registry sets `replace: false`. Data packs can adjust JSON under `data/chao_garden/loot_table/bonus` and `data/chao_garden/loot_modifiers`, or disable entries in `data/neoforge/loot_modifiers/global_loot_modifiers.json`.

## Getting started

- **Normal and colored Chao Eggs:** very rare chest loot in every dimension, or buy any of the sixteen ordinary base colors from a Black Market Trader. Normal is slightly more likely than each individual color and always cheaper. Eggs are not craftable.
- **Chao Care Guide:** book + wheat seeds.
- **Chao Analyzer:** glass pane above iron/copper/iron, with redstone below the copper.
- **Held Chao:** sneak-right-click your own Chao with an empty hand. No empty carrier is craftable or obtainable in creative; successful release consumes the filled item. Existing filled carriers remain valid.
- **Chao Garden CD:** eight iron nuggets around an emerald; about 115.81 seconds of vanilla jukebox playback.

| New tool/block | Shaped recipe | Output |
| --- | --- | ---: |
| Chao Garden Transporter (Garden Block) | Planks in top corners and middle sides; copper in middle; emerald below it | 1 |
| Garden Bell | Iron nugget above a row of three copper ingots; another copper below the middle | 1 |
| Starting Line | Three white wool above stick/green dye/stick | 3 |
| Ending Line | Three white wool above stick/red dye/stick | 3 |
| Race Marker | Red wool above two vertical sticks | 4 |
| Karate Mat | Three blue wool above one centered leather | 7 |
| Chao Medicine | Shapeless sugar + honey bottle + paper | 2 |
| Lesson Desk | Book above three planks, with planks at the lower corners | 1 |
| Doctor Station | Glass above iron + book + iron, with three planks below | 1 |

See [gardens and activities](docs/mechanics/GARDENS_AND_ACTIVITIES.md) for the straight line, linked flag and 7×7 mat requirements.

## Generic chest training supplies

Every declared chest table, in any namespace or path, receives two independent rolls. A proven physical chest/minecart fill also qualifies when its table omits the chest type. The same exact-context witness used for egg distribution excludes arbitrary generic, fishing, entity and block rewards.

| Bonus | Chance | Amount and selection |
| --- | ---: | --- |
| Chaos drive | 40% | 1–2 of one uniformly selected Swim, Fly, Run or Power Drive |
| Animal DNA | 12% | One of all 31 samples; ordinary animals have weight 3, Dragon/Unicorn/Phoenix have weight 2 |

Each mythical sample is two-thirds as likely as each ordinary sample rather than being exceptionally rare. There are ninety total selection tickets: each ordinary sample appears on about 0.4% of chest rolls and each mythical sample on about 0.267%. The old extra DNA roll in a short list of vanilla chests is removed, so each qualifying chest gets one DNA category roll. Other original loot is preserved. Settings remain `drive_chance` and `gene_chance` in `data/chao_garden/loot_modifiers/generic_chest_training.json`. Already-generated contents are not refilled.

## Fruit, drives and regional care supplies

Every enemy death with `doMobLoot=true` includes **exactly one** uniformly selected Chaos Drive, regardless of Looting. Hostile `Enemy` entities and the `MONSTER` spawn category qualify automatically; modpacks can extend `chao_garden:chaos_drive_enemies` for custom hostile entity types. The real captured death-drop list is normalized to one drive so old pools or a carried stack cannot create additional drive rewards. The Ender Dragon uses a persistent one-time reward during its actual death experience sequence because vanilla dragons bypass ordinary item-death loot; repeated bursts and reloading cannot duplicate that drive. Cancelled drop events remain cancelled, and ordinary animals do not receive this reward.

The old optional enemy supply roll uses the same combined fruit odds for the dimension’s wild fruit. Its previous drive choices become empty outcomes; guaranteed drives are handled separately. Regional chest supply pools may still add one to three fruit/drives; their extra old eighteen-percent DNA pool is removed. These are additive care supplies and do not make shop-only fruit naturally discoverable.

The supplies are usable care items for Chao rather than player food.

## Animal genes

Thirty-one coloured DNA-vial samples train abilities and supply mixed parts. The original 21 fill the current-life Chaos checklist; ten additional species are bonus samples and never expand that requirement. Each ordinary sample also selects **one supported part not already expressed by that animal**, progressively completing its appearance over repeated samples. It retains unrelated parts for mixing; same-animal ears/horns coexist while incompatible ears/horns from another animal are replaced. Child parts are less pronounced than adult parts. Hover over a sample for its exact benefits, then right-click your Chao to offer it. Training and parts apply only after the Chao considers and uses the sample; a tired Chao can return it. Half Fish supplies a green flame marker, Bat hides selected parts, and Skeleton Dog clears parts. Chaos bodies retain their family anatomy while still receiving training benefits. No new animal mobs are created. See [progressive appearance rules](docs/research/ANIMAL_PARTS_011.md), [item handling](docs/ITEM_HANDLING_018.md) and the [retained training profiles](docs/research/ANIMAL_GENES_010.md).

Most direct source mobs have a **20%** chance to drop the corresponding sample. Unicorn and Phoenix drops are **8%**; Dragon drops from bosses are **50%**. New rewards use `<animal>_animal_gene`; Boar retains the established registry name `warthog_animal_gene`. Old `<animal>_animal` IDs remain usable aliases for existing inventories and datapacks.

| Animal gene | Direct source |
| --- | --- |
| Penguin | Chicken, snow golem |
| Seal | Dolphin |
| Otter | Cod, salmon |
| Rabbit | Rabbit |
| Cheetah | Ocelot |
| Boar / Warthog | Pig, hoglin |
| Bear | Polar bear |
| Tiger | Wolf |
| Gorilla | Ravager |
| Peacock | Chicken |
| Parrot | Parrot |
| Condor | Phantom |
| Skunk | Fox |
| Sheep | Sheep |
| Raccoon | Panda |
| Half Fish | Guardian, elder guardian |
| Skeleton Dog | Skeleton, wither skeleton |
| Bat | Bat |
| Dragon | Ender dragon, wither |
| Unicorn | Horse, skeleton horse |
| Phoenix | Blaze, ghast |

Most hostile mobs also have an independent **6%** chance to drop a rescued gene of any type. Mythical animals have lower selection weights in that pool. All samples also have chest routes, so passive-mob hunting is optional. The Black Market now sells all four drives and rotates ordinary/mythical DNA, as described below.

## Renewable fruit and seed trades

The 35 active foods include 33 non-mushroom fruit families that craft shapelessly into one matching seed. Wild trees and bonus fruit rolls give Garden Fruit in the Overworld, Dark Garden Fruit in the Nether and Hero Garden Fruit in the Aether when installed. Other dimensions give no wild fruit. Other fruits appear in rotating Black Market stock. The seven seeds with listed SA2 purchase costs can also be bought directly: Tasty, Round, Square, Triangle, Dark Garden, Hero Garden and Strong. Chao, Heart and all ten Adventure seeds require obtaining their fruit first. Market-only fruit/seeds are absent from bonus loot and natural tree generation.

The new Chao Adventure 2 pool contains the following products. Each costs **eight emeralds**, has four purchases per offer and crafts into one matching seed. One of the ten appears per stock; these prices and discovery rules are Minecraft adaptations.

| Adventure fruit | Additional effect after eating |
| --- | --- |
| Energy | Shifts curiosity/activity/naivety/carefree tendencies |
| Peace | Reverses Energy's four tendency shifts |
| Smart | +10 Intelligence |
| Tall | Ordinary nourishment |
| Vitality | Ordinary nourishment |
| Rare | Dark Chao dislike it |
| Wonder | Dark Chao dislike it |
| Creepy | Hero Chao dislike it |
| Rough | Hero Chao dislike it |
| Forbidden | Hero Chao dislike it |

All ten also add 40 Stamina progress. Modern SA2 Battle effects retain priority for the nine CA2 table entries already present. The Chao Adventure 1 fruit section is excluded. See the [source review](docs/research/FRUIT_CORE_018.md).

Plant seeds on dirt or farmland with seven blocks of clear height and room for a five-by-five leafy canopy. Four hanging pods harvest and regrow while their branch and leaf support remains. Mushroom seeds are retired: no recipes, shop offers or creative entries, and old seed stacks cannot be planted. Saved mushroom orchards keep producing fruit; breaking their legacy crown or seedling yields a mushroom instead of a seed. Old small trees can expand safely with bone meal. Smart Fruit is active again and retains its existing IDs. Stamina Fruit and Mint Candy remain saved-item compatibility foods; old Stamina orchards/seeds still work, with no new recipe or supply route. See [fruit trees](docs/FRUIT_TREES.md).

Farmers offer these five seeds:

| Farmer level | Seed families | Emeralds per seed |
| --- | --- | ---: |
| 1 | Chao Garden, Hero Garden, Dark Garden | 1 for Chao Garden; 2 for the others |
| 2 | Strong, Tasty | 4 |

Each added offer has 12 uses. Vanilla chooses the farmer's available offers normally; a villager is not guaranteed every listed seed.

## Black Market stock and toys

The naturally visiting Black Market Chao stays visible at night. Stock refreshes every three to seven minutes. Each stock contains **41 offers**, with an independent **1-in-1,000** chance of a 42nd offer containing one gem egg. Both gardening tools, all sixteen base eggs and all four Chaos Drives are present simultaneously and never sell out; purchases reset their stock counter immediately, including repeated purchases in one open menu. Other stock remains finite and preserves purchase counts on reload. The Kindergarten resident uses the same catalog and finite counts but has no wandering-visit expiry; normal stock refresh still applies. Old saved catalogs migrate to the current rules. Current-version saved Moon offers are also removed when a configured space mod is added. Three ordinary DNA offers and one mythical DNA offer rotate with finite stock. Star Fruit, tree and cake are not sold. SA2 menu voice themes have no Minecraft counterpart and are omitted.

Prices adapt **100 source rings to one emerald, rounded up**, with no random price changes. Prices over 64 emeralds use emerald blocks and a possible emerald remainder in vanilla's two payment slots. Radio and TV exceed 64 blocks, so their costs round upward to a whole number of blocks: 603 and 1008 emeralds respectively. The actual merchant payment and toy tooltip show that payable cost.

| Product | Offers per stock | Emeralds | Uses per offer |
| --- | ---: | ---: | ---: |
| Shovel | Always | 20 (Minecraft adaptation) | Unlimited |
| Watering Can | Always | 25 (Minecraft adaptation) | Unlimited |
| Chaos Drive | All 4 | 1 | Unlimited |
| Ordinary animal DNA | 3 of 28 | 1 | 4 |
| Mythical animal DNA | 1 of Dragon, Unicorn, Phoenix | 2 | 2 |
| Normal egg | 1, always | 2 (Minecraft adaptation; source N/A exception) | Unlimited |
| Colored egg | All 15, always | 4–20; Aqua 6 and Cyan 8 | Unlimited |
| Shiny egg | Exactly 1 of 16 | Shiny Normal 10; colors 40–200 | 1 |
| Gem egg | 1-in-1,000 restocks; 1 of 13 (12 with Moon restricted) | Silver 50; Gold 75; Diamond/Moon 150; others 100 | 1 |
| Shaped fruit | 2 of Round/Square/Triangle | 1 | 8 |
| Other market fruit | 1 of 6 | Hero Garden/Dark Garden/Chao 2; Heart/Mushrooms 3 | 8 Hero Garden/Dark Garden; 6 Chao/Mushroom; 4 Heart; 2 alternate Mushroom |
| Adventure fruit | 1 of 10 | 8 (Minecraft adaptation) | 4 |
| Strong seed | 1, always | 5 | 8 |
| Other listed seed | 2 of 6 | Tasty 2; shapes 3; Hero Garden/Dark Garden 4 | 8 |
| Chao hat | 2 of 12 | 20–200 | 2 |
| Toy | 2 from the expanded eligible pool | Published prices where present; otherwise explicit Minecraft prices | 1 |
| CWE fruit | 1 of 13 | Orange 5; GBA fruits 1; Shiny 30; Hyper 200 | 4; Hyper 1 |
| CWE accessory | 1 of 10 | 25 | 2 |
| CWE special gift | 1 | See `CweCatalog`: lens/brush 10, wings 5, mirror/parts box 20, potion 500 | Source-dependent |

The sixteen base eggs cover Normal and every registered ordinary color, including Aqua and Cyan. All sixteen shiny counterparts are eligible for the single rotating shiny slot. The thirteen gems are Silver, Gold, Ruby, Sapphire, Emerald, Amethyst, Aquamarine, Garnet, Onyx, Diamond, Peridot, Topaz and Moon. Dreamcast Emblem costs have explicit Minecraft emerald rarity prices rather than a direct conversion. Normal and Shiny Normal prices, plus the additional Aqua/Cyan colors, are explicit Minecraft adaptations. Moon uses its authored mottled blue-purple coat. Its ordinary loot and market routes are disabled when configured space integration is active.

The twelve hats are Apple, Cardboard Box, Pan, Paper Bag, Empty Can, Stump, Flower Pot, Bucket, Watermelon and three wool beanies. Offer a hat to a Chao you keep; it thinks before equipping or refusing it. An accepted swap returns the old hat exactly once. Right-click an idle Chao with shears to remove equipped headwear. Hats use saved, synchronized head equipment and stay with held or companion Chao.

The expanded collection has 24 placeable toy types; previous Ball, Rattle and Maracas IDs remain outside trader stock. Bell, Castanets, Cymbals, Flute and Tambourine have no invented N/A shop price: their first successful instrument lesson grants a one-time physical prop. Other learned instruments also provide this acquisition path. Chao choose reachable toys voluntarily according to needs, personality and boredom. Eight seconds of completed play grants happiness, reduces boredom and costs energy; it grants no keeper bond. Toys animate in place and return one actual item when broken. Clicking a toy at a Chao gives placement guidance; the shovel is also a gardening tool that can be offered to teach planting. See [placed toys](docs/PLACED_TOYS_018.md). The full source-price mapping is in `MarketStock.catalog()` and [the market study](docs/research/BLACK_MARKET_017.md).

## CWE additions and kindergarten rewards

Thirteen renewable crop families extend the existing foods: Orange, seven GBA colours, Shiny and four Hyper fruits. They begin as rotating market fruit; each can then craft into its seed. They do not bypass discovery through new natural trees or chest pools. Dropped Orange/GBA fruit can produce a real DNA sample after its source-inspired attraction timer; this replaces temporary animal mobs with the established gene system. Ten added samples include seven SADX species and three CWE samples. See [the source inventory](docs/research/CWE_019.md) for exact matrices, weighted pools and gift eligibility.

Thirteen lenses, ten accessories and nine other gifts use real inventory items and saved considered gift transactions. The Chao's own wardrobe requires no Accessories mod; the player Companion slot still does. Dye consumes one actual dye. A first instrument lesson grants one physical instrument and records the claim on the Chao, so reload or repeat lessons cannot duplicate it. An offline keeper can claim a pending prop later beside a Lesson Desk.

Doctor grade care converts 100 rings to one emerald: an E/D/C/B ability at level 99 costs 100/200/300/400 emeralds. It supports actual emeralds plus blocks and returns change. Only eligible nearby owned Chao can receive it, at most three times per life. A failed request consumes nothing. Grade care is a service rather than a merchant recipe.

## Egg loot distribution

There are no built-in egg recipes. Each category below rolls independently once per eligible chest loot generation, rather than once per chest slot. The unified modifier recognizes declared chest tables in any namespace, including unconventional names. A scoped physical loot-container fill also recognizes generic mod chest and minecart tables that omit a chest type. An unproven generic table, ordinary entity drop, fishing reward or block drop does not receive chest eggs.

| Category | Eligible chest loot | Chance per eligible roll |
| --- | --- | ---: |
| Base color, including Normal | Any chest table in any dimension | 0.25% for one egg from the base pool |
| Shiny color | Any non-Overworld dimension; Overworld buried treasure, ancient city, stronghold, trial chamber, desert pyramid and jungle temple tables | 0.10% for one shiny egg |
| Gem, including Moon without space integration | Ancient city, desert pyramid, stronghold, bastion and End city treasure tables | 0.02% for one gem egg |
| Moon with configured space integration | Any chest on an exact configured surface Moon dimension | 0.02% for one Moon egg |
| Nether Chao | Any chest table generated in `minecraft:the_nether` | 1% |
| End Chao | Any chest table generated in `minecraft:the_end` | 1% |

Normal receives six selection tickets and each of the fifteen colors receives five. Thus Normal appears about once per 5,400 chest rolls, versus once per 6,480 for each individual color. The sixteen shiny colors are uniformly selected. The gem pool contains thirteen uniformly selected gems, or twelve when Moon is restricted. Rolls can independently succeed together. Previously opened/generated chests are not refilled.

The four category chances are configurable through `data/chao_garden/loot_modifiers/chest_eggs.json`. Its ledger uses `replace: false`, preserving other mods' loot modifiers. The six old egg modifiers and their bonus tables are removed to avoid additional acquisition routes.

### Moon and space mods

Without a configured space mod loaded, Moon belongs to the thirteen-gem structure and rare-market pools. When a configured space mod is loaded, Moon leaves both routes. It instead gets an extremely rare roll in any chest on the exact configured surface Moon dimension. Orbit, stations and other planets do not count. The other twelve gems retain their existing structure routes.

The synchronized server config has `distribution.spaceModIds` and `distribution.moonDimensionIds`. Defaults are the verified Ad Astra (`ad_astra` / `ad_astra:moon`), Northstar (`northstar` / `northstar:moon`) and Stellaris (`stellaris` / `stellaris:moon`) identifiers. These mappings are optional identifier detection, not bundled mods or a claim that every named project has a compatible NeoForge 1.21.1 build. Add an unlisted space mod's actual mod ID and exact surface Moon dimension to the corresponding lists. A recognized space mod without an available mapped Moon source intentionally leaves new Moon discovery unavailable until its mapping or generation is supplied.

## Themed Chao eggs

New acquisition and creation of the seventeen mod-addon Chao is paused. Their recipes, creative entries, custom egg command, egg placement/hatching and new breeding/robot-building heritage cannot create those forms. Their registry IDs, models, textures, saved eggs and living Chao are retained for future work. Existing living Chao keep their appearance data; paused eggs stay recoverable without hatching. The twenty-nine previously retired themes remain retired.

Nether and End are the two obtainable themes and enter gameplay through their respective dimension chest pools. Neither has an active crafting conversion or merchant offer. The legacy themed-egg recipe serializer remains registered for compatibility but cannot match or assemble an egg. The old conversion materials in [the catalog](docs/MOD_VARIANTS.md) are disabled design history. `/chao variants` lists obtainable themes, and `/chao variantegg <id>` rejects paused addon themes. Ordinary administrative commands remain operator tools rather than survival acquisition paths.

## Resource inventory

`scripts/generate_all_assets.py` rebuilds the shared care/theme/gene/fruit/market/activity/companion resources, music metadata, official model import, fitted overlays and final egg shell correction in dependency order. `scripts/resource_items.json` is the generated obtainable-art ledger, not a complete runtime registry count: compatibility IDs remain registered separately.

The active theme catalog is `assets/chao_garden/chao_variants.json`, authored by `art/variant_catalog.py`, with nineteen retained theme egg IDs; seventeen are paused for new acquisition. Thirty-one canonical DNA-vial genes are active; only the original 21 retain legacy aliases. Four upright drives and the native/active-theme egg sprites retain their pixel-art refinements; placed egg UV wrapping is corrected in 0.15. Fruit and transporter art are project work. Main Chao model, texture and animation inputs come from the supplied official addon; see [asset provenance](docs/ASSET_CREDITS_015.md). Third-party mod assets and complete mod jars are not bundled.
