# Chao Garden 0.25.18


Current update: the Chao Analyzer is a blue and yellow tablet, with a larger mouse-tracking preview, letter-colored skill grades, clearer Genes pages, and a body-part diagram with a clickable Chaos DNA checklist. Lessons are now daycare-only. See [release notes](docs/release-0.25.18.md) and [verification](TESTING.md).

The previous [garden, journal, tooltip and artwork updates](docs/release-0.25.17.md) remain included.

Fruit, seed and watering-can first-person views now fit the screen more comfortably. Chao keep their walking stride while carrying seeds, and watering raises, pours and lowers the can in time with the real effect. Gardening tools remain permanent Black Market stock. See [release notes](docs/release-0.25.14.md) and [verification](TESTING.md).


A standalone **Minecraft Java 1.21.1 / NeoForge** fan mod about raising Chao: eggs, care, inherited coats, training, evolution, breeding and reincarnation. Build gardens, plant orchards, and enter the Chao you raise in player-built races and karate.

Version **0.25.8** raises the Dark Garden fruit trees by one block, keeps the ambient babies inside the classroom, rebuilds the classroom play-city props, and reverses the ornate school doors while closing their frame gaps. The doors keep their requested positions at the exterior or lobby-facing edge of each frame. See [release notes](docs/release-0.25.8.md) and [current verification](TESTING.md).

Regional music now uses the supplied SA2 ADX recordings for the Battle lobby, Neutral/Hero/Dark gardens and kindergarten. Six full classroom singing recordings are included; background music fades out while the classroom chorus is selected and returns when you leave. No extra resource pack is required. The existing jukebox disc keeps its separate recording and normal controls. See [audio details](docs/SA2-AUDIO-IMPORT.md).

The arranged short Chao-vocal phrases remain as fallback assets; the bundled full classroom songs take precedence. Individual learned solo singing is outside this update.

The earlier flipped Hero stairway, animated Dark gate fire and fresh-arrival lobby rendering fix remain included.

The school's architecture, furniture, cabinet and Hero masonry are included in the base mod. No furnishing mod is required. Hero and Dark Garden are first playable passes, with ordinary Chao care and return routes to their respective lobby landings.

The previous egg-distribution rules remain: ordinary eggs cannot be crafted; all base colors are always available from Black Market Traders; shinies rotate one at a time and gem offers are extremely rare. Chest discovery follows [the 0.23 distribution rules](docs/release-0.23.0.md). Mod-themed Chao acquisition remains paused, with their registered assets and saved heritage retained.

The official comparison uses the supplied original model, original PNG and source animation curves. The customized garden appearance retains inherited coats, animal parts, Chaos families, held-item interactions and other Minecraft adaptations. A comparison changes rendering; it does not erase the saved creature's care or genes. Use `/chao appearance official [rig]` on the nearest Chao and `/chao appearance garden` to return. Both commands require operator level 2, accept a target within 12 blocks, and preserve their selection through saving or carrying. The [reference research](docs/research/) and [surface/attachment notes](docs/research/SURFACES_AND_TRADER_018.md) record what was inspected and changed.

Point-based movement from 0.16 remains: a newborn crawls until it earns **50 Run points**, with further milestones for running, dashing, swimming and short ledge glides. Held Chao keep a supported carried pose. See [movement mechanics and sources](docs/research/MOVEMENT_016.md).

The earlier netherrack egg, live animated End-portal shell and fruit-shaped seed refinements remain included. The End inventory icon is static.

The native renderer uses the supplied official Sonic addon **Chao models, textures and animation definitions**. The current **19 retained themes**, **31 animal-gene sources** and **Light, Angel and Devil Chaos** additions fit those skeletons; the original source import began in 0.15. The egg shell retains its authored rounded model with repaired texture wrapping. See [asset provenance](docs/ASSET_CREDITS_015.md) and [egg repair](docs/EGG_SHELL_015.md).

The food collection includes **35 active fruit/food families**: the twelve consolidated SA2 garden foods, ten Chao Adventure 2 fruits and thirteen CWE-inspired additions. The collection has **33 active seed families**. Wild Garden Fruit grows in the Overworld, Dark Garden Fruit in the Nether, and Hero Garden Fruit in the optional Aether; other fruit and seeds appear in rotating Black Market stock. New CWE food, accessory and special-gift categories rotate alongside the existing stock. **Mushroom and Nutritious Mushroom have no seeds to craft, buy or plant**. The Garden Block resembles the PC Chao transporter, pickup creates the filled Chao item directly, and the new **Chao Garden CD** plays about **115.81 seconds** in a vanilla jukebox.

## Install

1. Use **Minecraft Java 1.21.1**, **NeoForge 21.1.219**, and **64-bit Java 21**.
2. Place `chao_garden-0.25.18.jar` in `mods/`, replacing the previous Chao Garden jar.
3. Install the same version on the server and every connecting client.

The base mod requires no additional model or animation library. Adult equipped companions optionally require **Accessories for NeoForge 1.21.1 and its oωo dependency** on both sides. See [companions](docs/COMPANIONS.md). This build targets NeoForge.

Existing Chao, eggs and filled carriers retain their saved identities and care data. Legacy empty carriers are removed when their inventory tick runs. Smart Fruit is active again as a Chao Adventure 2 food, including its existing orchard IDs. Old Stamina trees remain usable, while Stamina Fruit and Mint Candy have no new acquisition paths. The 29 retired themes continue to load as ordinary forms with saved data preserved. See [retirement handling](docs/THEME_RETIREMENT_014.md).

In Chao World, all five authored realms use Adventure mode. Eggs, held Chao, seeds and garden props still use their normal interactions. Sneak-right-click a placed garden prop with empty hands to recover it; authored scenery remains protected. School doors open and close by right-clicking. The central exit returns you beside your original teleporter and restores your original game mode.

## Start a garden

1. Very rarely find a **Normal or colored Chao Egg** in chest loot, or buy any ordinary base color from a Black Market Trader. Normal eggs cost less and are slightly more likely than each colored egg. Eggs cannot be crafted. Use an egg on a block. It hatches after **five active minutes** by default; recovering a placed egg retains incubation. Hold use with the egg in the air for eight seconds, then place it to hatch gently and quickly.
2. Craft the **Chao Care Guide** with a book and wheat seeds. Pet with an empty hand, feed fruit, and use a **Chao Analyzer** to inspect care, stats and genes. The player placing the egg becomes its keeper.
3. Enemies drop **one random Chaos Drive per kill**. Find DNA-vial **Animal Genes** in any chest loot table, from their existing mob sources or in Black Market stock. Mythical DNA is moderately less common than ordinary DNA. Hover over a gene for its benefits. Ordinary samples add one supported part at a time; repeated samples complete a set, and different animals can mix.
4. Find your dimension’s wild fruit tree or buy seeds from a farmer. Find the other products in rotating Black Market stock. Non-mushroom fruit crafts into its matching seed; the trader also offers the seven seeds listed in the reference catalog. See [fruit trees](docs/FRUIT_TREES.md).
5. Craft a **Chao Teleporter**, place its two-block tube and walk inside. Visit the **Kindergarten** through the hub's blue-column arch, enter the **Neutral Garden** through the natural stone arch, follow the ascending celestial staircase along the lobby wall to the **Hero Garden**, or take the descending wall-side staircase into the **Dark Garden**. All three gardens permit normal Chao care and placement while protecting their scenery. Carry a Chao to the Teacher for class, the Doctor for charts and free illness treatment, or the Fortune-Teller for naming. The Principal and bulletin board explain the services. Legacy Lesson Desks and Doctor Stations still work in existing saves; new acquisition is retired. The Analyzer retains remote inspection and wardrobe management.
6. Place a **Chao Garden Transporter** and right-click it to enroll up to eight nearby Chao. A **Garden Bell** assigns particular Chao. Build a race course or a complete **7x7 karate mat**, then choose entrants from your gardens.

Unloaded Chao, held Chao and equipped companions do not age. Awake Chao can pick up a nearby dropped item, hold it and consider it. Only one real stack is held at a time. Gifts use the same saved held-item state as objects picked up from the ground; explicit tools remain tool interactions. Food acceptance depends on care needs; unfamiliar objects are inspected and returned to the world. Accepted food applies its benefit after the eating sequence. Returning an item does not create another copy. Garden enrollment, deliberate gifts, pickup and contests enforce ownership. See [item decisions and persistence](docs/ITEM_HANDLING_018.md) for acceptance, rejection, timing and transfer details.

## Controls and crafting

| Action | Control |
| --- | --- |
| Pet or claim an unowned Chao | Empty-hand right-click |
| Offer fruit, a drive or a gene | Right-click with the item; the Chao holds and considers it |
| Let Chao discover an object | Drop the item nearby; they may pick it up, inspect it and return it |
| Add a garden toy | Place the toy item on a block; nearby Chao choose when to play |
| Inspect care and genetics | Right-click with the Analyzer |
| Pick up your Chao | Sneak + empty-hand right-click |
| Release your held Chao | Use its filled item on a block with clear space |
| See held animations | Select the filled Chao item in either hand |
| Equip an adult companion | Put its filled item in the Chao Companion Accessories slot |
| Start companion flight | Double jump while airborne; finite below Fly 99, unlimited at Fly 99 or with an all-S robot |
| Enter Chao World | Walk into a placed Chao Teleporter |
| Leave Chao World | Use the central hub transporter; `/chao return` is an emergency exit |
| Boost an all-S Omochao flight | Rebindable boost key (default R), every five seconds |
| Open your Ender Chest with an active End companion | Rebindable key (default V) |
| Manage a garden | Right-click the Garden Transporter |
| Assign an individual Chao | Use a Garden Bell on the transporter, then on your Chao |
| Unassign or toggle stay/follow | Sneak-use the Garden Bell on your Chao |
| Link a race | Select the start with a Race Marker, place flags within 32 blocks per link, then select the finish |
| Open a contest lobby | Right-click the starting line or a complete 7x7 mat |
| Harvest fruit | Right-click a ripe hanging pod |
| Name a Chao | Use a named name tag |
| Offer a Chao hat | Right-click your Chao with the hat; it considers the gift before equipping it |
| Remove a Chao hat | Right-click your Chao with shears |
| Check personality, emotions or illness | Analyzer → MIND → PROFILE, FEELINGS or HEALTH |
| Diagnose and treat | Visit the Kindergarten Doctor; free illness treatment and paid grade care |
| Enroll in a timed class | Visit the Kindergarten Teacher with your Chao; select LEAVE CHAO |
| Offer, remove or dye Chao accessories | CARE → WARDROBE using actual inventory items; equip offers wait for the Chao's decision |
| Select an earned medal | CARE → WARDROBE → MEDAL |
| Rock an egg gently | Hold use in the air for eight seconds, then place it |
| Throw an egg | Sneak, hold use, release; a wall impact hatches it |

Vanilla **carved pumpkins, melon blocks and skeleton skulls** work as Chao hats. Melon slices remain food. These three hats are not sold by the Black Market; old custom watermelon hats convert to vanilla melon through supported load/use paths. See [migration details](docs/HATS_02550.md).

An **Analyzer** uses a glass pane above iron/copper/iron, with redstone below the copper. A **Garden Transporter** uses planks at the top corners and middle sides, copper in the middle, and an emerald below it. The **Garden CD** uses eight iron nuggets around an emerald. The **Chao Teleporter** recipe is glass/copper/glass, glass/emerald/glass, then three copper ingots. [LOOT.md](LOOT.md) lists the remaining recipes.

There is no craftable or creative empty carrier. Pickup directly stores the same creature's UUID, name, keeper and complete state. A successful release consumes the filled item in survival and creative; blocked or rejected placement preserves it. See [pickup and music](docs/PICKUP_AND_MUSIC_015.md).

## Learning to move

Fresh Chao start with zero ability points and crawl. Train with drives, animal genes, fruit and activities to earn points. The Analyzer's stat rows and Care Guide show movement guidance.

| Movement | Ability-point requirement |
| --- | ---: |
| Walk | 50 Run |
| Run | 667 Run |
| Dash | 1334 Run |
| Swim | 100 Swim |
| Improved swim stroke | 667 Swim |
| Short garden ledge glide | 135 Fly |

A novice in water flails and tries to escape safely. Trained swimmers move more effectively; a trained flyer can glide briefly while descending from a ledge, rather than hover or take off from level ground. Staying, resting, eating and evolution suppress the garden glide. Reincarnation keeps ten percent of points, so a reborn child can retain an ability if its remaining points still qualify.

The point gates apply to ordinary Chao moving around the world. Omochao starts with adult walking mechanics without receiving free Run points. Accessories companion benefits use trained levels instead of these garden movement point gates.

## Orchards, contests and companions

Fruit trees stand **seven blocks tall**, with real wood branches, a **five-by-five leaf canopy** and four hanging pods. Leave clear growth space, plant on dirt or farmland, and use bone meal to accelerate growth. Ripe pods regrow while their branch and leaf support remains and a planted tree is alive. Buy a shovel or watering can from permanent Black Market stock and give it to a Chao to teach gardening. The two mushroom foods are bought as consumables; new mushroom seed recipes, planting and creative entries are removed. Existing placed mushroom trees remain for saved-world compatibility. Old small trees can expand safely with bone meal. See [fruit sources and growing](docs/FRUIT_TREES.md).

The **Black Market Trader** uses emeralds and emerald blocks. All **16 ordinary base-color eggs** remain available without a purchase limit throughout a visit. Exactly one shiny color rotates with the other stock; gem eggs appear on one in 1,000 restocks. Normal eggs cost **2 emeralds**, and every other ordinary color costs **4–20 emeralds**. Stock refreshes every three to seven minutes after trading closes. The Shovel (20 emeralds) and Watering Can (25 emeralds) are also always available. Other fruit, seeds, hats and toys retain their existing discovery rules and prices. See [LOOT.md](LOOT.md) for the complete current catalog.

The full thirteen-entry gem catalog includes **Silver, Gold, Ruby, Sapphire, Amethyst, Emerald and Moon**. With configured space integration, Moon eggs are restricted to exact surface Moon dimensions and leave ordinary gem loot and shop stock. Moon uses a finely mottled, faceted blue-purple coat based on the gallery references. The retained five merchant toys are **Trumpet, Drum, Jack-in-a-box, Radio and TV**. The expanded garden has **24 physical toy types**, including eleven instrument props, water toys, mounts and a movable ball; classroom-only instruments are earned from lessons rather than sold at invented N/A prices. An awake, unoccupied Chao with enough food and energy can approach a reachable toy when its personality and boredom make play appealing. A completed play session improves happiness and reduces boredom; interrupted sessions do not award the benefit. Their sounds and visible activity belong to the placed prop. Hats are considered as gifts before being worn and can be removed with shears. Ball, Rattle and Maracas can also be placed, while remaining absent from new merchant stock.

A race has straight rows of **2-8 Starting/Ending Line sections** and linked red triangular flags at most **32 blocks** apart. Karate needs a connected **7x7 square** of mats. Shared lobbies allow multiple Chao per keeper and other nearby keepers to fill free slots. Races use actual navigation; karate uses virtual bout health. Both train skills and activity experience. See [gardens and activities](docs/mechanics/GARDENS_AND_ACTIVITIES.md).

Every active, energized adult Accessories companion can defend you and assist movement. Benefits scale with the relevant trained level: Run reaches **+100% land speed**, Swim reaches **+200% water speed**, and Power attack damage grows from **1 to 11** (a punch to a fully charged Sharpness V netherite sword). Fly grants **vanilla elytra flight** with the normal airborne double jump: about **2 seconds** initially, **10 seconds at level 70**, **12 seconds at level 98**, and unlimited duration at **level 99** while energy lasts. Swim 70 grants underwater night vision, and Swim 99 adds underwater breathing; those owned effects end on land or unequip. A **Nether companion** provides fire/lava immunity to its keeper and temporary body. An **End companion** prevents levitation and gaze-triggered Enderman hostility; the rebindable **V** key opens the keeper's real Ender Chest. These innate traits remain available at low energy and end on unequip or defeat. During supported gliding or elytra flight, the Chao holds onto the keeper's back. An **all-S Omochao** permits elytra flight even before leveling Fly and adds a rebindable **boost key (default R)** with a **five-second cooldown**. Abilities consume energy. Defeat still disables the companion for 60 seconds, costs 10 happiness and one random trained skill level, then restores it while equipped. See [companion behavior and persistence](docs/COMPANIONS.md).

See [placed toy behavior](docs/PLACED_TOYS_018.md) for selection, cooldown, interruption and save details. Breaking a toy recovers one item; there is no internal toy inventory.

## Personality, health and lessons

MIND exposes all thirteen personality sliders, all eighteen emotion meters and all six illness meters. Strong profile traits produce up to three cycling Doctor descriptions. Inherited personality alleles remain saved; older saves keep their original label and four existing tendency values until deliberate personality changes replace that migration overlay. Some source emotion fields have unresolved behavior; they are preserved and shown without inventing unsupported effects.

The **Kindergarten Doctor** offers three charts, free treatment of all six illnesses, and paid grade care. Grade care requires level 99, a grade below A and fewer than three upgrades this life. It costs 100, 200, 300 or 400 emeralds according to the old grade, raises the expressed grade by one and resets that ability's training; its unexpressed allele stays intact. Payments use actual emeralds/blocks and change.

The **Kindergarten classroom** has four shared places. Each lesson takes ten server-running minutes; the offered class rotates every thirty minutes. Students remain in class until completion, even while their keeper is elsewhere or logged out. Their care and life cycle pause during boarding. Rejoining after a restart preserves their identities and countdown. Collect a finished student from the Teacher with a free inventory slot. Song and drawing improve to five, four dances unlock breakdancing, and all eleven instruments support spontaneous performance. A first instrument lesson awards one placeable prop. Lessons grant knowledge rather than Intelligence XP. See [Kindergarten services, timing and adaptations](docs/KINDERGARTEN_024.md).

The hub, Kindergarten, Neutral Garden, Hero Garden and first playable Dark Garden are included and require no decoration dependencies. Visitors enter Adventure mode; the central hub transporter returns them to their original dimension, location and game mode. The blue-column arch leads to the school and the stone arch leads to the Neutral Garden. Celestial stairs ascend east to the Hero Garden gateway and descend west to the Dark Garden gateway. `/chao return` provides a fallback exit during a visit.

Family records keep lineage IDs internally and optional name snapshots separately. Newly born Chao record their parents' display names and inherit available grandparent names. Older records can resolve a loaded named relative; an unavailable unnamed relative displays as unknown rather than exposing its numeric ID.

CARE / WARDROBE offers actual inventory stacks for five Chao equipment categories. Hats, clothes, lenses and cosmetic wings are held and considered: mood gives an awake Chao a 15–90% chance to accept. Rejected gifts are thrown back into the world. Accepted gifts replace the old item without duplicating it. Removal returns equipment and dyeing consumes actual dye for supported clothing. It preserves item components, including custom names and colours. Earned contest medals are selectable; the UI cannot grant awards. Eggshell hats retain the appearance of the egg that produced them, including shiny and rare coats. They cover the whole head with eye openings.

## Growth, inheritance and themes

Default active durations are **three hours as a child** and **twelve hours as an adult**, plus short cocoons. At end-of-life, happiness **above 50** gives reincarnation. Reincarnation retains coat/stat genes, learned skills, equipment and friendship. Each valid visible or hidden point total becomes `floor(old points / 10)`, levels return to 1 and progress returns to 0. The earlier 500-point carry cap is removed. It rerolls the personality profile and removes a random subset of animal parts, while clearing the current-life checklist. Configuration can disable unhappy natural death.

Hero/Dark Garden Fruit moves a child's alignment. Old separate fruit, seed and orchard IDs load as their garden counterparts. At first evolution, alignment and the first adult type lock until reincarnation. This mod follows the requested highest-stat rule: the greatest trained ability-point total wins, with the most recently increased tied stat breaking ties; Stamina gives Normal. That first-choice rule is a deliberate adaptation of the game's hidden-influence selection.

A separate **evolution strength** starts at 0 in each life phase. Loaded age and successfully eaten whole fruit increase it. Adult strength grows from **0 to 1.2**, blending the young-adult form toward a fully aged target; it is not remaining lifespan. Training moves the signed **Swim/Fly** and **Run/Power** axes within **-1 to +1**. A secondary can be Normal, one of four pure types, or one of **Swim+Run, Swim+Power, Fly+Run, Fly+Power**. Swim+Fly and Run+Power cannot coexist within one secondary stage. A fixed first Swim type can later have Fly as its secondary, and training can reverse that secondary without another cocoon. The original twenty reference models remain unchanged. The new age targets are authored stepped Minecraft adaptations of the young/fully aged gallery references; historical 0.20 exaggerated/Monster demonstrations are labeled as historical. See [gradual evolution](docs/research/GRADUAL_EVOLUTION_021.md).

Two eligible adults given **Heart Fruit** can breed, with a five-minute cooldown. Offspring inherit coat/stat alleles and one parent's theme heritage; acquired animal parts and trained levels do not pass to offspring. On its **third life or later**, a Chao with **all 31 registered current-life DNA species**, happiness **above 80** and a prospective **Normal first type** under the requested highest-stat rule can evolve into **Light, Angel or Devil Chaos**, selected by alignment. Stamina maps to Normal, so training it above the other abilities favors that choice; a tied total still uses the most recently increased stat. See [precise mechanics](MECHANICS.md).

Omochao is built by a wrench-trained Chao using four accepted parts. A Parts Box or recovered **Robot Component** fills one stage; one calibrated **Robo Heart** can replace one part. It starts as a **young adult with all C grades and fresh levels**, copies its creator's coat, walks immediately, ignores animal-gene training/parts, and cannot get sick. Drives, fruit, activities and lessons still train it. At the end of a loaded garden lifespan, an unhappy robot leaves one component; happiness above 50 leaves one heart. Rebuilding with that heart raises every grade **C → B → A → S**, capped at S. The new builder becomes its parent and the old creator becomes its grandparent. Equipped/held aging still pauses. Its mechanical rig retains one spiral-eye pair, cable arms, a rotor and winding key; it uses the ordinary care/garden/companion systems.

The collection retains **19 themes: 17 mod tributes plus Nether and End**. Their anatomy and native-layout atlases remain intact. New creation and acquisition of the seventeen mod-themed Chao is paused, including crafting, creative listings, custom commands, new egg hatching, breeding inheritance and robot heritage. Existing living creatures retain their saved appearance. Nether and End eggs now come from rare chest loot in their respective dimensions, and have no active conversion recipe. [The theme catalog](docs/MOD_VARIANTS.md) keeps the previous materials as disabled design history.

## Configuration and commands

Edit the world's generated `chao_garden-server.toml` with the server stopped. Defaults are `hatchTicks=6000`, `childTicks=216000`, `adultTicks=864000`, and `naturalDeath=true`. Tick-based timing slows with server lag. Classroom defaults are `lessonTicks=12000`, `lessonOfferTicks=36000` and `classroomCapacity=4`.

Optional server rules are disabled unless chosen: `expandedRareActions` allows rare-form breeding/parts, `bullyChao` allows nonlethal bullying, `cweFruitRebalance` selects CWE's stronger food table, and `monsterEvolution` enables extended adult development. `fixMonsterTransparency` changes that optional appearance. `newbornEyeChancePercent=0` leaves random newborn iris colours disabled; a positive percentage enables an explicit Minecraft birth chance. `birthdayYearTicks=24000` makes one loaded adult Minecraft day a birthday year; zero disables parties. The birthday calendar, appearance effects and optional chances are adaptations rather than claimed original timing equations. `distribution.spaceModIds` and `distribution.moonDimensionIds` configure exact optional Moon integrations; add unlisted mods and their surface Moon dimension IDs to both lists. Orbit dimensions do not qualify.

| Command | Permission | Effect |
| --- | --- | --- |
| `/chao return` | Any visiting player | Return from the hub, Garden or Kindergarten to the saved origin |
| `/chao inspect` | Any player | Inspect the nearest Chao within 12 blocks |
| `/chao variants` | Any player | List currently obtainable themes |
| `/chao variantegg <id>` | Operator level 2 | Give an obtainable Nether/End egg; paused addons are rejected |
| `/chao appearance official [rig]` | Operator level 2 | Show the nearest Chao within 12 blocks using an original source model, PNG and animation clips; omit the rig for state-derived selection |
| `/chao appearance garden` | Operator level 2 | Restore the nearest Chao's customized appearance |
| `/chao testkit` | Operator level 2 | Give tools, blocks and care supplies |
| `/chao mature` | Operator level 2 | Advance a child to evolution |
| `/chao rebirth` | Operator level 2 | Advance a happy end-of-life transition |
| `/chao clearparts` | Operator level 2 | Apply Skeleton Dog's part-clearing effect |

## Editable source

The complete download contains the production JAR, editable project, original reference inputs and current verification logs. Separate screenshot and video previews are omitted as requested. See [TESTING.md](TESTING.md) for the verification scope and limits.

Open `source/` as a Gradle project with Java 21:

```bash
./gradlew build coreTest
./gradlew runGameTestServer
./gradlew runClient
```

Use `gradlew.bat` on Windows. The output is `build/libs/chao_garden-0.25.8.jar`. [TESTING.md](TESTING.md) records completed checks.

The editable official model/texture/animation inputs are in `art/imported_chao`. Eight retained Blockbench projects author custom additions and the egg; they are not replacements for the imported form JSON. See [asset editing](art/blockbench/README.md).

This is an unofficial fan project, unaffiliated with SEGA, Sonic Team or Gamemode One. The supplied Chao geometry, textures, animation definitions and garden music belong to their rights holders. Custom themes, animal parts, Chaos adaptations and Minecraft mechanics are project work. The complete Marketplace world and third-party mod jars/textures are not bundled. See [asset provenance](docs/ASSET_CREDITS_015.md), [audio credits](docs/AUDIO_CREDITS.md), [LICENSE](LICENSE) and [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).
