# Chao Garden 0.23.0 — distribution and dimension companions

This release develops survival acquisition after the 0.22 anatomy, lifecycle and statistics checkpoint. It adds chest egg pools, persistent base-color merchant stock, dimension companion traits and a visible back-grip during supported flight.

## Egg discovery

Eggs cannot be crafted. The Normal recipe, all nineteen themed conversions and their unlock advancements are removed. The old six egg loot modifiers and six bonus pools are also removed, so they cannot bypass the new distribution rules.

| Egg category | Eligible chest source | Default chance per eligible generation |
| --- | --- | --- |
| Base colors, including Normal | Chest loot in every dimension and namespace | 0.25% for one egg from the base pool |
| Shiny colors | Every non-Overworld dimension; Overworld buried treasure, ancient city, stronghold, trial chamber, desert pyramid and jungle temple tables | 0.10% for one shiny egg |
| Gem coats, including Moon without space integration | Ancient city, desert pyramid, stronghold, bastion and End city treasure tables | 0.02% for one gem egg |
| Moon with configured space integration | Chest loot in an exact configured surface Moon dimension | 0.02% for one Moon egg |
| Nether | Any chest loot generated in `minecraft:the_nether` | 1% for one Nether egg |
| End | Any chest loot generated in `minecraft:the_end` | 1% for one End egg |

Category rolls are independent and occur per generation, rather than per chest slot. The base pool has six tickets for Normal and five for each of the fifteen other colors. That gives Normal a 1-in-5,400 chance per qualifying chest, versus 1-in-6,480 for each individual other base color. Shinies are evenly selected from sixteen colors; gems are evenly selected from thirteen coats, or twelve when Moon is reserved for a Moon dimension.

The additive modifier recognizes the actual `minecraft:chest` loot-table parameter set in any namespace, including unusual mod table names. A narrow, scoped witness also covers an actual physical loot chest or chest minecart filling from a generic root table. That witness is tied to the exact root table and context and is cleared even if filling fails. An unproven generic table, nearby chest block, fishing table, entity drop or block drop does not establish chest provenance.

The global modifier `data/chao_garden/loot_modifiers/chest_eggs.json` exposes the four category probabilities for data packs. Other mods’ global modifier entries are retained. Existing chest contents that have already generated are not rerolled by installing the update.

## Moon and space mods

The synchronized server config provides `distribution.spaceModIds` and `distribution.moonDimensionIds`. The default pairs are `ad_astra` / `ad_astra:moon`, `northstar` / `northstar:moon`, and `stellaris` / `stellaris:moon`. These are identifier mappings, not bundled mods or a claim that all three have been tested as complete NeoForge 1.21.1 modpacks.

For an unlisted space mod, add its actual loaded mod ID and exact surface Moon dimension ID to those lists. Orbit, stations and substring matches do not qualify. Once a configured space mod is loaded, Moon leaves ordinary gem loot and merchant stock. If that mod has no mapped Moon chest source, Moon remains unavailable until the mapping or world generation is supplied. The other twelve gems retain their structure sources.

## Black Market stock

Every visiting merchant offers all sixteen non-shiny base-color eggs at once, with unlimited purchases throughout the visit. Normal costs two emeralds; every other base color costs more. Permanent egg counters are reset after an actual trade and restored correctly after loading an exhausted saved offer.

Each rotating stock has exactly one shiny color. One in 1,000 restocks also has one gem offer. All thirteen gem coats are eligible unless space policy excludes Moon. Saved stock is migrated and checked against the current policy, including removing a previously saved Moon offer when a configured space mod is added.

The usual stock has 31 offers, or 32 with the optional gem. Existing category tabs and pagination retain vanilla offer indices and transaction behavior. Other supplies remain finite and rotate. Genes and drives remain loot supplies, and neither addon nor Nether/End theme eggs enter merchant stock. [LOOT.md](../LOOT.md) records the detailed item catalog and prices.

## Paused mod-addon acquisition

The seventeen mod-addon appearances remain registered, with their art and existing saved heritage. New egg placement, hatching, crafting, custom command supply, creative listings, breeding heritage and robot-construction heritage are paused. Existing living Chao retain their appearance data; placed addon eggs remain recoverable and retain their incubation. Nether and End are the two currently obtainable themes.

[The theme catalog](MOD_VARIANTS.md) preserves the old material list as disabled design history. The pause does not retire the nineteen current theme asset sets or erase saved creatures.

## Accessories companions

These added traits require a living adult Chao in the actual Accessories companion slot. Holding a carrier or keeping a Chao nearby provides none of them. Defeat, recovery, removal or a stale projection revokes the trait.

- A Nether companion prevents fire, burning, lava and hot-floor damage and extinguishes the keeper. Its authenticated temporary body receives the same protection, so entering lava does not immediately disable the companion.
- An End companion prevents and clears levitation. Endermen ignore eye contact, while attacking one still permits ordinary retaliation.
- The End companion’s rebindable key defaults to **V**. It opens the keeper’s real vanilla Ender Chest, with its normal saved inventory. The server checks the live companion and slot, limits repeated opening requests, and closes the menu when the companion becomes unavailable. It cannot open another player’s inventory.

Innate Nether/End traits remain available to a tired living companion. The existing skill-scaled combat and movement abilities retain their energy rules.

## Flight presentation

A Chao actually supporting a finite glide or elytra flight holds onto the keeper’s back. Its real tracked body follows the player’s torso and retains its coat, shine, adult/Chaos anatomy, animal parts and wardrobe. The ordinary world draw is suppressed during attachment, avoiding a duplicate follower. Other players receive the server-authored attachment state. Landing, ending support or unequipping restores ordinary following or recall.

Pinning remains active even while the Chao holds a real pending gift. Flight-only collision, gravity, velocity and fall debt are cleared from managed snapshots before a later ordinary release. An ordinary garden Chao’s deliberate saved physics is preserved.

The Keeper’s Journal explains these changes. Its Chaos evolution page also describes the already-established happiness-above-80 requirement and Stamina-favored Normal first evolution.

## Validation

Fresh executed checks and their limits belong in [TESTING.md](../TESTING.md) and `docs/validation-0.23/`. Earlier 0.22 galleries and test records remain historical references; their old crafting and stock descriptions do not define this release’s acquisition rules. No previous, lost working-copy test counts are asserted by this document.
