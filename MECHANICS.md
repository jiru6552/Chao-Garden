# Chao mechanics in 0.22.0

This file describes the implemented mod. Sonic Adventure 2 Battle provides the main inspiration, with jewel coats and Chaos forms drawn from the wider Adventure-era system. Timing, consumable amounts, movement benefits, and some inheritance details are adapted for Minecraft.

Version 0.22 audits the existing raising system, model consistency, retained themes, item art, lifecycle boundaries and arithmetic. The project labels this phase its user-defined 50% readiness checkpoint; it is not a numerical claim that every original-game or CWE mechanic is present. Gradual maturity, fixed first-adult identity, finite Omochao lives and level-scaled companions remain separate systems. The rules below take precedence over historical notes.

## Time and the lifecycle

Chao age through server ticks while their entities actively tick. There is no offline catch-up. Carrier storage pauses care and aging. Equipped adults accumulate care time and visual maturity while their lifespan stays paused; recovering an egg preserves incubation. Companion defeat recovery uses absolute server world time. At 20 ticks per second:

| Stage | Default duration |
| --- | --- |
| Placed egg | 6000 ticks / five minutes |
| Child | 216000 ticks / three hours |
| Evolution cocoon | 200 ticks / ten seconds |
| Adult | 864000 ticks / twelve hours |

Configuration changes egg, child, and adult durations. Runtime duration arithmetic uses reachable bounds of 20 through `Integer.MAX_VALUE - 1`; the configuration schema retains its existing minimums. Age counters pause during cocoons. Cocoon transitions add a little time beyond the named durations. A completed whole-fruit action advances evolution strength once; rejected or interrupted food does not. Adult strength starts at 0 and caps at 1.2 independently of lifespan. Its baseline age clock grows over four configured child durations, with the documented fruit increment. Omochao has a separate component/heart terminal path.

At first evolution, a child becomes an adult and its primary type locks. At the end of adulthood:

- Happiness **greater than 50** produces reincarnation.
- Lower happiness produces natural death when `naturalDeath = true`.
- `naturalDeath = false` makes unhappy Chao reincarnate instead of dying of old age.

Reincarnation creates a placed egg and preserves the original body UUID through egg items and reload. The same garden entry remains reserved; hatching rejects an existing live actor with that UUID in any server dimension. Actual death removes the garden/recovery entry, while safe capture and rebirth retain it. It retains genes, expressed coat traits, themed heritage, grades, keeper, name, happiness, and bond. Each visible ability keeps `floor(old points / 10)` points across the complete supported 0..9999 point range; its level becomes 1 and progress becomes 0. Hidden Luck/Intelligence use the same rule across 0..4000. The source-game 500 carry cap is intentionally removed for the requested literal ten-percent rule. Grades are retained; no saved-state format is widened and no already-completed old reincarnation is retroactively recomputed. Hero/Dark alignment returns at +1/−1 and Neutral at 0. Primary/secondary types, the current-life animal checklist and per-life gift counters reset. Each animal-part slot has a 30% removal chance; the retained subset survives into the egg. The SADX probability is adapted to both alignment families. Hats, ghost/flame traits, accessories, learned skills, awards and friendship records remain saved. Personality and its tendencies are assigned anew; boredom resets to 0, fullness to 80 and energy to 100.

The ordinary sequence is egg → baby → first-evolution cocoon → young adult → fully adult → terminal cocoon → death or an individual rebirth egg. Young adult and fully adult share the same adult lifespan; there is no second maturity cocoon. Evolution uses yellow/aqua/blue, reincarnation peach/pink and death ivory/gray shell palettes. Pending terminal item returns stop further gift processing. A denied world drop retains the actor and its real held/worn stacks until retry; ordinary rebirth transfers its saved equipment into the egg.

The natural-death option governs old age. Ordinary Chao can still be hurt or killed by Minecraft hazards and combat. Chaos Chao reject ordinary damage, but deliberate creative removal and bypass-invulnerability damage, such as administrative killing, remain possible. Evolution cocoons also reject ordinary damage.

## Care and behavior

Happiness ranges from −100 to 100. Bond, fullness, and energy range from 0 to 100. Fresh Chao begin with happiness 20, bond 0, fullness 80, and energy 100.

| Action or condition | Effect |
| --- | --- |
| Pet, with a three-second cooldown | +2 happiness, +2 bond |
| Eat fruit | +25 fullness, +3 happiness, +1 bond, plus the item's training/effect |
| Eat its favorite shape fruit | Another +5 happiness |
| Receive a drive | +1 happiness, +40 Luck progress, +20 Intelligence progress and visible-stat training |
| Receive an animal gene | +2 happiness, +20 Intelligence and Luck progress, training and current-life checklist progress |
| Complete voluntary toy play | +4 happiness, −35 boredom, −2 energy; child rattle gives +7 happiness instead; no keeper bond |
| An ordinary Chao takes damage | −5 happiness |
| Fifteen active seconds | −1 fullness |
| Five active minutes with fullness below 10 | −2 happiness |
| Thirty seconds awake | −1 energy |
| Rest for one second | +1 energy |

Chao rest when energy reaches 5 or lower and wake after recovery. Water wakes them. Petting can wake them too. Fullness is a care value, not the player's food bar; hunger does not directly apply Minecraft starvation damage. Boredom rises by one per ten awake seconds, up to 100; petting lowers it by eight and eating by five.

Offering a fruit, drive, gene or hat transfers one actual item into the Chao's hands. It thinks first, then eats/uses it or throws it back. Benefits and consumption commit once at the end of the accepted action. Unknown items are inspected with confusion and returned unchanged. Pending items and their components survive saving and carrying; interruption returns an unconsumed item exactly once. See [item handling](docs/ITEM_HANDLING_018.md) for durations and interruption rules.

Personality now has thirteen diploid tendencies with expressed sliders in −100..100. A magnitude above 54 supplies eligible Doctor labels; up to three labels cycle in the live profile. The Analyzer MIND / PROFILE tab shows all tendencies, FEELINGS shows all eighteen emotion meters, and HEALTH shows six illness meters. Exact original behavior weights are not asserted where sources leave them unresolved. Old v4 labels and four actual tendency values migrate intact until an intentional personality change replaces their compatibility overlay. New births and breeding use stored alleles, while reincarnation randomizes personality again.

Timed joy, crying, fear, anger, surprise, tiredness and illness symptoms affect visible voluntary actions. Six illness meters range −100..100, with negative values active. Medicine restores all to 100 and grants 24000 loaded ticks of treatment protection; this protection duration is a Minecraft adaptation. Coughing or sneezing does not cure illness. A Doctor Station and real medicine inventory permit direct treatment, or the medicine may be offered through the saved gift decision sequence. Dormant TOTAL/STRESS and unresolved Charm/Nourish purposes are preserved without invented mortality or health rules.

Lonely Chao can approach familiar partners and talk. Five completed conversations unlock gossip; ten unlock hugs and handholding. Up to 32 stable friend identities survive saving, carrying and rebirth. These familiarity thresholds, radii and action lengths adapt the source's social behavior to Minecraft. A hungry Chao can approach an orchard, shake it and drop real fruit into the same held decision process.

Bond of at least 20 lets an ordinary, unassigned Chao follow its nearby keeper through local pathfinding. Garden assignment confines roaming to its garden area; the Garden Bell can unassign it or toggle stay/follow. Equipped companions have their own follow/recall behavior. Staying stops voluntary movement. A player holding suitable fruit can attract Chao. Idle Chao can approach available dropped items within four blocks, take one unit, and consider it through the same action system. Pickup delay and a specific item owner are respected; returned items have a ten-second Chao pickup grace period.

The keeper is the player who places an egg or claims an unowned Chao. Other survival players cannot feed, command, or capture an owned Chao. The Analyzer is read-only. Name tags work normally.

## Abilities, grades, and levels

**Swim, Fly, Run, Power, and Stamina** each have a grade, level, progress bar, and ability-point total. Grade controls points per level; it is separate from level.

| Grade | Internal value | Ability points per level |
| --- | ---: | ---: |
| E | 0 | 11–15 |
| D | 1 | 14–18 |
| C | 2 | 17–21 |
| B | 3 | 20–24 |
| A | 4 | 23–27 |
| S | 5 | 26–30 |

Every 100 progress gains a level and `13 + 3 × grade + random integer from −2 to +2` ability points. Levels stop at 99. Negative animal effects subtract current progress, down to 0; they do not remove earned levels or points. Newly generated shop/loot eggs randomize a pure pair for each stat grade. Bred eggs inherit one allele per parent; a different higher grade expresses with 70% probability. The stored phenotype never rerolls on load, and old hidden alleles remain intact.

The five possible E–S level gains are equally likely. The first-evolution grade improvement affects future level gains; it does not multiply already-earned points. A perfect-S ordinary first life reaches 2970 points after 99 level-ups. Repeated ten-percent carry with level-1 restarts permits 98 further level-ups, giving the independent maximum sequence 2970 → 3237 → 3263 → 3266 → 3266. Activity XP, care history and reincarnation counters saturate at their persisted bounds instead of wrapping. Price conversion uses wide arithmetic before applying payment limits.

| Ability | Minecraft benefit |
| --- | --- |
| Run below 50 points | Slow crawling |
| Run 50-666 points | Walking |
| Run 667-1333 points | Running |
| Run 1334+ points | Dashing |
| Swim below 100 points | Novice flailing and emergency surface/escape behavior |
| Swim 100-666 points | Competent swimming |
| Swim 667+ points | Improved arm-and-leg swimming stroke |
| Fly 135+ points | Short descending garden ledge glides |
| Stamina points | Increase maximum health |
| Power points | Increase armor, capped at 12 armor points |

These milestones use earned `points[]`, not displayed levels or progress bars. They apply at every age: adulthood does not automatically grant walking, and a reincarnated child can retain a learned ability if its retained points still meet the threshold. The ground gait changes as soon as synchronized training or loading changes the point total.

Navigation chooses the destination and pace. The base ground speed attribute varies within each gait: crawl starts at 0.075; walk at 0.17; run at 0.25; dash at 0.34, capped at 0.44. Water propulsion is independent of Run: novice speed 0.055, swimming starts at 0.16 and the improved stroke starts at 0.24, capped at 0.36. These speeds are Minecraft balance choices, not original-game units. All Chao remain air-safe in water; novice flailing does not introduce drowning deaths. Trained swimmers paddle upward only when submerged rather than repeatedly panic-hopping at the surface.

A garden glide requires at least 135 Fly points, energy above 5, horizontal motion and a descent. Staying, resting, eating, cocoons and water prevent it. Each fall permits up to 60 active glide ticks; landing or entering water replenishes that allowance. It limits downward speed and resets fall distance only while active, adds no upward lift and cannot sustain hovering. The transient glide state is not inherited or saved. Accessories player benefits remain separate: all energized adults get a level-scaled finite glide, and Fly99 or all-S Omochao permits elytra flight.

The modern wiki's GC/HD ledge-flight threshold is above 134 points, implemented as 135. The older Info Center lists 100 Fly points; this release follows the newer wiki for that garden ability. See [movement research](docs/research/MOVEMENT_016.md) for the source distinction.

The crawling cycle alternates an extended, planted forelimb with a lifted recovering forelimb and an opposite rear-knee push. A small torso rock and head nod communicate the transfer of support. It advances with travelled stride and freezes its locomotion cycle while stopped. See [the crawl study](docs/research/CRAWL_017.md) for the reference and fitting limits.

Ground motion uses a quiet baseline rather than continuously repeating the full imported idle sway. One 27.5-tick sway gesture occurs per independently phased 260-399-tick interval, with a five-tick ease at each end; it does not play while moving, reacting, resting or swimming. Actual locomotion selects crawl, walk, run, dash, water or air poses, with an eight-tick pose transition. Held Chao use a supported carried pose. Source clips remain available for source-parity inspection, and the gameplay controller adds these states while custom parts inherit the same semantic bone transforms.

Movement, swimming and being airborne still supply the existing small passive training gains. Those gains change ability progress, not evolution influence.

Luck and Intelligence each retain two grade alleles in 0..255, a fixed expressed grade, a level/progress bar and points capped at 4000. Their level gain uses the same `13 + 3 × grade ± 2` equation, capped by the hidden-stat point limit. Animal samples add 20 progress to both hidden stats; drives add 40 Luck and 20 Intelligence progress. Bred offspring start untrained. Rebirth keeps `floor(old hidden points / 10)` and returns their levels to 1 with zero progress; the valid 0..4000 source range therefore carries at most 400. The broad byte-grade range is source data rather than E–S labels. Intelligence affects karate zeal; unsupported random luck powers are not invented.

CWE Hyper fruit can upgrade one eligible S-grade Swim/Fly/Run/Power stat to X, and only one X is allowed. X returns to S at rebirth. Its current 29–33 ability-point gain uses an explicit Minecraft extension of the grade equation because the exact CWE X gain was not established by the source.

## Fruits and drives

One item represents a complete consumable, applied once through the 100-point progress bar for each level after the Chao finishes eating. The active fruit totals adapt the SA2 four-bite benefits into a held-item action; tree sources, prices and care timing are Minecraft rules. Hero Garden Fruit uses the intended benefit rather than the PC port's stamina bug.

| Item | Training or special effect |
| --- | --- |
| Chao Garden Fruit | +40 Stamina progress |
| Round, Square, Triangle Fruit | +40 Stamina; favorite shape adds happiness |
| Hero Garden Fruit | +40 Stamina; child's alignment +0.15 |
| Dark Garden Fruit | +40 Stamina; child's alignment -0.15 |
| Heart Fruit | +40 Stamina; puts an eligible adult into breeding readiness |
| Chao Fruit | +50 progress in each of the five abilities |
| Mushroom | +50 Stamina progress; +50 Luck progress |
| Nutritious Mushroom | +80 Stamina progress; +100 Luck progress |
| Strong Fruit | +50 Stamina |
| Tasty Fruit | +30 Stamina |
| Stamina Fruit (saved legacy item) | +60 Stamina |
| Smart Fruit | +40 Stamina; +10 Intelligence |
| Energy Fruit | +40 Stamina; Curiosity/Energetic +10, toward Naive 10, Carefree −10 |
| Peace Fruit | +40 Stamina; reverses Energy's four tendency changes |
| Tall, Vitality Fruit | +40 Stamina, no extra ability effect |
| Rare, Wonder Fruit | +40 Stamina; disliked by Dark Chao |
| Creepy, Rough, Forbidden Fruit | +40 Stamina; disliked by Hero Chao |
| Mint Candy (saved legacy item) | +15 Stamina; +40 energy |
| Yellow/Swim Drive | +24 Swim progress |
| Purple/Fly Drive | +24 Fly progress |
| Green/Run Drive | +24 Run progress |
| Red/Power Drive | +24 Power progress |

The round, square, or triangle favorite is selected at birth. Bred offspring retain one preference allele from each parent and express either equally. Favorite shape increases willingness to eat and adds happiness. Adult Hero Chao refuse Dark Garden/Creepy/Rough/Forbidden Fruit; adult Dark Chao refuse Hero Garden/Rare/Wonder Fruit. Children can eat either alignment fruit and change alignment. Modern Strong Fruit keeps its modern neutral profile. Alignment fruit never changes adult alignment.

The consolidated SA2/CA2 core has 22 active foods and 20 non-mushroom seed families; the expanded collection adds 13 CWE-inspired families, for 35 foods and 33 seeds total. Wild trees and bonus supply rolls yield Garden Fruit in the Overworld, Dark Garden Fruit in the Nether and Hero Garden Fruit in the optional Aether. Round, Triangle, Square, Heart, Chao, Mushroom and Nutritious Mushroom begin as rotating Black Market **fruit** purchases; Hero Garden and Dark Garden Fruit can also be bought there. One additional rotating offer introduces Energy, Peace, Smart, Tall, Vitality, Rare, Wonder, Creepy, Rough or Forbidden Fruit. Craft obtained non-mushroom fruit into its seed. The trader can also sell Tasty, Round, Square, Triangle, Dark Garden, Hero Garden and Strong seeds, following the supplied reference list. Adventure seeds require their fruit first. Farmers sell Garden, Hero Garden, Dark Garden, Strong and Tasty seeds. Market-only foods remain absent from bonus loot and natural tree generation. Wild placement attempts occur on average once per eight eligible new chunks and require suitable clear space. Naturally placed trees have ripe fruit.

Plant on dirt/farmland with seven blocks of height and a clear five-by-five canopy area from the fourth block upward. Trees have real wood branches, three layers of biome-tinted leaves and four hanging pods. Right-click ripe pods to harvest; supported pods regrow. Bone meal advances seedlings and fruit and can expand saved small trees without copying their crop. Mushroom and Nutritious Mushroom seeds no longer craft, appear in creative stock or plant; their old registry IDs and existing placed trees remain for saved-world compatibility. Smart is active again with its saved IDs; Stamina Fruit and Mint Candy remain legacy foods with no new acquisition, while saved Stamina orchards/seeds still work. See [fruit trees](docs/FRUIT_TREES.md) and the [0.18 source review](docs/research/FRUIT_CORE_018.md).

## Alignment and evolution

Child alignment runs from −1 to +1. At least +0.5 is **Hero**; at most −0.5 is **Dark**; the middle is **Neutral**. Hero Garden and Dark Garden Fruit explicitly control it. Their +40 Stamina and ±0.15 child-affinity effects apply once per completed meal in both food profiles. Old separate Hero/Dark Fruit, seeds and orchard blocks resolve to the corresponding garden variant when loaded. Empty-hand petting is alignment-neutral. Alignment locks at adulthood. Reincarnation retains its category at Hero +1, Dark −1 or Neutral 0, after which child care can affect it again.

Shape uses two separate axes, clamped to −1…+1:

- **Swim ↔ Fly:** Swim is negative, Fly positive.
- **Run ↔ Power:** Run is negative, Power positive.

A child's drive moves its axis by 0.05; a corresponding animal moves it by 0.1. Adult changes are half as large. Opposite gifts cancel one another. Ordinary animal groups favor their corresponding ability; random, mythical, and ghost animals influence a randomly chosen ability.

Ordinary first adult selection compares the greatest earned point total among **Swim, Fly, Run and Power only**. Stamina is excluded. The latest actual increase among tied abilities wins; when one combined drive raises several abilities simultaneously, their prior relative recency is preserved, with previously unrecorded members ordered Swim/Fly/Run/Power. Positive ties without history use that same order; an untouched all-zero/no-ability-history Chao retains Normal. The saved five-stat recency ledger remains backward compatible because the separate Chaos admission still needs its historical Stamina entry. This differs from the source game's hidden-influence first-type chooser.

The primary type is fixed for that life. Swim, Fly, Run, or Power evolution raises that ability's expressed grade one rank, up to S. Normal and Chaos evolution raise Stamina's expressed grade instead. Only the expressed allele changes; its hidden counterpart remains intact. The improvement persists through reincarnation and can be inherited. After first evolution, the axes reset for adult training.

Outside the focused Hero Run prototype, adult secondary shape keeps the existing continuous, reversible nine-target lattice: Normal, Swim, Fly, Run, Power, Swim+Run, Swim+Power, Fly+Run and Fly+Power. Opposites cannot coexist within one secondary stage, though a fixed first Swim can later have a Fly secondary. Biological evolution strength remains independent: 0 begins at the young-adult base, 0.6 is half mature, and 1.2 reaches full maturity. The center Normal target also ages. These other targets remain authored stepped Minecraft interpretations of the galleries; their first identity is not replaced when training reverses. Original-asset comparison remains separate.

The [blocky Hero Run prototype](docs/HERO_RUN_BLOCKY_02555.md) reads its reversible training target from `max(0, RunTotal - PowerTotal)`, where each total is `level × 100 + progress`, clamped to 0–9900. Raw baby Run influence saturates at 480 net progress; its **visible** development is that target multiplied by child evolution strength, clamped to 0–1. Hero development remains a separate alignment influence, reaching its endpoint at +0.5. A young, highly trained baby can therefore look close to its normal young form. Adult Hero Run uses a 960-progress target; visible specialization is that target multiplied by biological maturity (`strength / 1.2`). These provisional 20/40 ordinary-drive equivalents count from zero, so baby training contributes to the adult target. They are authored Minecraft rates, not measured original-game drive counts.

All mutation routes read the same current state: ordinary and combined drives, animals, fruit, activity training, decreases, Doctor resets, companion defeat and Creative Analyzer edits. Reducing training or adding opposing Power can reverse specialized appearance without reducing biological counters, changing the locked first type or making an adult younger. Compatible Swim/Fly detail remains on the Hero Run body's limbs/wings and small accent regions through bounded existing-axis contributions. This focused path does not author every mixed specialization; other adult first forms and special/themed variants retain their established morphology paths.

The original blocky baby head box, eyes and facial UVs stay fixed through this path. Added closed cuboids change position, size and angle continuously; inactive parts remain inside opaque head or stem geometry rather than being visibly swapped. Neutral Run front nubs are separate from the larger rear branches. Hero children retain their distinct buds/stem, and adult Hero Run progresses from a short stem to an upper arch/fan. The same geometry and pixel-region placement apply to inherited colors, monocolor, jewel and shiny finishes. Existing hats, animal parts and toy/equipped attachments use the shared rig. See [implementation and verification limits](docs/HERO_RUN_BLOCKY_02555.md).

The existing adult clock adds one strength unit per four configured child durations. With the default three-hour childhood, full strength 1.2 would take 14.4 active adult hours; ordinary adult life ends after twelve hours. Whole adult fruit adds 0.005 each. Forty completed meals supply the 0.20 gap at the terminal boundary; forty-one or more allow the exact full endpoint before that tick. The live equipped companion also advances its existing adult maturity-care clock while lifespan/birthday counters remain paused. Feeding, equipped care and time retain their existing rules; training introduces no biological-age acceleration or clock reset.

Themed Chao follow the same 15 alignment/first-type families and gradual age/secondary shapes as ordinary Chao. Their canonical palette, emblems, goggles, crowns and floating marker remain recognizable through 209 native-layout atlases. Crown/marker placement follows the head; chest emblems and packs fit the animated torso. Caps hide covered crown parts, and acquired animal head parts can replace a crown as described below. Underlying alignment, types, grades and benefits remain calculated and saved. Chaos evolution replaces the visible theme with the appropriate Light, Angel or Devil form while retaining its saved heritage.

## Color and coat inheritance

Each Chao carries two alleles for base color, tone, shiny, jewel coat, each visible stat grade and its shape-fruit preference. An offspring gets one randomly selected allele from each parent for each trait. Trained levels, ability points, alignment, evolution type, saved animal-part data, trained emotion state and the animal checklist do not transfer through breeding. Personality, Luck and Intelligence each have their own diploid alleles; these inherit independently of acquired training.

| Trait | Expression rule |
| --- | --- |
| Color | Normal is recessive; two different non-normal alleles select one at birth |
| Mono/two-tone | Select one inherited allele at birth |
| Shiny | Either shiny allele makes the offspring shiny |
| Jewel | No-jewel is recessive; two different jewel alleles select one at birth |
| Stat grade | When different, the higher inherited grade expresses 70% of the time |
| Favorite shape fruit | Select either inherited preference equally |

Choices are stored rather than rerolled after loading. Hidden alleles remain available to later generations. Jewel coats visually take precedence over base color. Shiny jewel offspring remain opaque. Shiny and jewel Chao and placed eggs receive a moving silver reflection using the mod's included reflection shader and Minecraft foil-style blending; it follows the player's glint-speed/strength settings and needs no separate shader mod. This is an animated highlight, not a mirror or a physically based material. The live End-portal egg keeps its own animated portal material. The unwanted white base piece is removed from placed eggs.

The 16 base entries are Normal, White, Red, Yellow, Blue, Aqua, Pink, Orange, Purple, Brown, Green, Grey, Lime, Black, Sky Blue, and Cyan. The 13 jewel coats are Silver, Gold, Ruby, Sapphire, Emerald, Amethyst, Aquamarine, Garnet, Onyx, Diamond, Peridot, Topaz and Moon. Moon has a textured blue/purple finish rather than a literal crescent silhouette. Aqua, Cyan, and Diamond extend the source palette for Minecraft.

## Breeding

Any two living, non-Chaos adults can breed when neither is in a cocoon, both are in love, and both have finished their breeding cooldown. Feed both Heart Fruit and let them meet. The result is a placed egg carrying inherited genes, with the initiating parent's keeper.

Both parents leave love mode and receive a **6000-tick / five-minute** cooldown. The mod uses deliberate Heart Fruit feeding rather than naturally timed flower seasons. It does not recreate the original dance sequence. Breeding does not inherit acquired training or drain parental lifespan.

## Themed heritage and mod availability

The 19 active themes consist of **17 mod tributes plus Nether and End**. The 29 retired appearances are unavailable even if their source mod is loaded; legacy Chao and eggs safely use ordinary forms. Neo Vitae uses the neovitae mod requirement. A themed egg uses any native plain, coloured, shiny or jewel Chao Egg plus three vanilla materials in a shapeless recipe. Crafting changes the heritage while preserving the input egg's genes, name, incubation and other saved data. The source mod must be loaded for a mod theme's recipe, creative-tab entry, operator acquisition and egg placement to be enabled. Nether and End have no mod dependency. Recipes and accepted mod IDs are listed in [MOD_VARIANTS.md](docs/MOD_VARIANTS.md).

Heritage is stored separately from the ordinary coat genes. A child of matching parents keeps their shared heritage. Different parents contribute one heritage chosen with equal probability, including a normal parent's ordinary heritage. Reincarnation preserves the choice. Heritage does not grant another mod's energy, mana, storage, spell or combat functions.

While its dependency is available, a themed Chao uses a canonical theme material, accessories and floating marker. Color, tone, shiny and jewel genes remain saved and inheritable. Chaos Chao use the Light, Angel or Devil visual instead, retaining their saved heritage underneath.

If a source mod is removed, an existing Chao renders as an ordinary Chao while its heritage stays saved. Restoring the matching mod reactivates that appearance. Existing placed eggs can hatch with dormant heritage; recovered or inventory eggs carrying unavailable heritage cannot be placed until its mod is restored. A filled Carrier still releases the existing Chao safely. An unknown future heritage ID is also preserved with an ordinary fallback.

The egg item registry remains stable even when integrations are absent, preventing item IDs from disappearing from saves. Unavailable shells are hidden from the Chao creative tab and have no active recipe or enabled egg command. Administrative `/give` can expose an inert registered shell, but placement still rejects it. Placing a normal shell with embedded unavailable heritage is rejected too.

Availability uses actual loaded mod IDs, with explicit OR aliases where listed. No optional mod classes are linked. Recognizing a historic mod ID does not make an older Forge or Fabric jar compatible with NeoForge 1.21.1; dormant themes require a genuine compatible build.

## Animal genes and the Chaos checklist

There are **31 consumable Animal Genes**, shown as colored DNA vials. Hover over a sample to see its animal, exact Swim/Fly/Run/Power training percentages, supplied appearance slots and special effects. Right-click a Chao you keep to offer one. Keeper and busy/resting rules apply; the Chao considers the gift before using it. The first completed sample of each species marks the **current-life checklist** in the Analyzer, contributing to Chaos evolution; acquiring every visible part is not required for that checklist.

The training profiles follow the requested Chao Island Info Center's modern SA2/B table. Percentages use the mod's 100-point progress bar; negative changes stop at zero progress without removing earned levels. See the [complete profile table and references](docs/research/ANIMAL_GENES_010.md).

An ordinary sample chooses **one random supported slot not already expressed by that animal** and adds or replaces that part. The eight appearance slots are arms, ears, forehead, face, feet, horns, tail and wings. Repeated samples complete that animal's set without repeatedly selecting an already acquired part; a completed set stays intact and further samples still train stats and grant care benefits. Other occupied slots remain for mixed animals.

The one-part guarantee is an authored Minecraft rule. Chao Island documents supported slots, replacement and larger adult parts; the additional modding reference describes partial acquisition. The inspected sources do not establish an exact vanilla acquisition probability. There is no hidden strength tier or additional item. Four Rabbit samples, for example, complete its arms, ears, feet and tail. Four Peacock samples can then replace the feet and tail and add its crest and wings, retaining the Rabbit arms and ears.

Ears and horns from the **same animal** can coexist, including Sheep or Dragon. When another animal supplies ears, incompatible horns are removed; when it supplies horns, incompatible ears are removed. Unrelated slots remain. Child animal details are smaller, with limb connections and soles retained; adult parts use their full authored proportions. Existing saved partial or complete sets remain as saved. See [progression, mixing and source limits](docs/research/ANIMAL_PARTS_011.md).

The imported main head and face remain; animal crests, ears, snouts and horns are fitted cuboid additions. Acquired limbs follow the imported skeleton and gameplay pose additions. Head growth replaces an ordinary crest or themed crown; a muzzle-only change keeps the crown. Theme coat, body and floating marker remain unless a ghost effect overrides the marker.

Half Fish changes the floating marker to a green flame without removing acquired parts. Happy hearts temporarily take priority. Bat hides feet, tail and wings; later genes may replace those slots. Skeleton Dog clears animal parts and restores hidden feet. Its saved `hatsUnlocked` flag remains. The new merchant hats can be equipped independently of that old flag as a direct Minecraft care interaction. Chaos forms receive training and checklist progress while keeping their canonical bodies and marker.

The checklist covers Penguin, Seal, Otter, Peacock, Parrot, Condor, Rabbit, Cheetah, Boar, Bear, Tiger, Gorilla, Skunk, Sheep, Raccoon, Dragon, Unicorn, Phoenix, Half Fish, Skeleton Dog and Bat. Boar retains the established registry spelling `warthog_animal_gene`. New loot uses `<animal>_animal_gene`; old `<animal>_animal` items remain functional aliases. Acquired appearance is saved but is not inherited. Samples introduce no new animal mobs. Bee, Ladybug, Scorpion, Deer, Elephant, Kangaroo, Koala, Lion, Mole and Swallow also count: Chaos evolution requires all 31 registered DNA species. Learned actions use explicit skill and animal checks; their authored animation does not reproduce every original clip. All 31 retain chest routes; direct sources remain in [LOOT.md](LOOT.md).

## Black Market and toys

A Chao-shaped wandering trader opens the vanilla emerald merchant interface. It uses the supplied Chao rig and walking cycle, with dark sunglasses and a lower-face mask. Vanilla dusk potion goals are removed, and an older saved invisibility effect is cleared; the merchant stays visible at night.

Current stock contains 42 offers, with a 1-in-1,000 chance of one additional gem egg. All sixteen base eggs, four drives, both gardening tools and the key are permanent. Three of 27 ordinary DNA samples and one of Dragon, Unicorn, Phoenix or Half-fish rotate; Half-fish is not duplicated in ordinary stock and keeps its one-emerald price and four-use limit. Half-fish keeps ordinary chest weighting, independent of the shop category. Other fruit, seed, hat, toy and CWE offers rotate with their existing prices. Vanilla carved pumpkin, melon and skeleton skull hats are not sold. Stock refreshes after three to seven minutes, after any open trade ends. Valid current-version purchase counts and refresh deadlines survive save/load; old or invalid catalogs are restocked once. See [current stock and costs](LOOT.md).

Ring prices are converted at 100 rings per emerald, rounded upward. Values above one emerald stack use emerald blocks plus a remainder. Radio and TV exceed one stack of blocks and round upward to a block multiple (603 and 1008 emeralds, respectively), paid with two block stacks. The regular egg costs 2 emeralds. Dreamcast Silver/Gold/gem/Moon eggs use explicit Minecraft rarity prices of 50/75/100/150 emeralds, because their source requirements were Emblems. The ordinary Black egg already supplies Black. These are balance adaptations, not source-game ring prices. [LOOT.md](LOOT.md) records every product and cost.

Natural visits are attempted at most once per 24000 ticks of world play, with a 60% roll near a living overworld player. Trader- and mob-spawning game rules apply. The saved attempt/occupied budget prevents restart rerolls; visits last up to 24000 ticks and expire by world time, including unloaded time.

Trumpet, Drum, Jack-in-a-box, Radio and TV are placeable toys, along with Ball, Rattle and Maracas. Chao independently choose reachable toys within seven horizontal blocks when their needs and personality permit it. A completed session lasts eight seconds, then play rests for roughly 30–40 seconds. The actual toy animates in place; musical toys use short instrument rhythms, and the TV shows an authored animated scene. There is no copied display item or forced click-to-reward action. Resting, hungry, busy, cocooned, staying, competing and summoned-companion Chao do not begin play. Interrupted play grants no completion reward. See [placed toys](docs/PLACED_TOYS_018.md).

Twelve merchant hats use the Chao's synchronized head equipment. Offer a hat to your Chao; it considers whether to equip it, retaining the previous hat until the swap completes. The previous hat returns once. Shears remove equipped headwear without consuming the shears while the Chao is idle. Care ownership, resting, cocoon and contest restrictions apply. Hats follow the imported head pose and save with the entity, held Chao and companion. The new hat does not replace inherited coat genes. See [item handling](docs/ITEM_HANDLING_018.md) and [the market source notes](docs/research/BLACK_MARKET_017.md).

## Optional CWE rules and birthdays

Original rare-form restrictions remain the default. Server `expandedRareActions` optionally permits breeding and animal parts on rare forms. `bullyChao` optionally enables nonlethal bullying and intervention. `cweFruitRebalance` chooses the stronger CWE food table; ordinary SA2/CA2 effects remain the default. `newbornEyeChancePercent` defaults to zero; enabling it rolls an ordinary iris colour once at birth at the configured percentage, without giving special alignment/Spartoi lenses at random.

`monsterEvolution` is disabled by default. Its compatibility data and optional2000/3000-point stages use separate influence fields; they do not stretch the ordinary signed axes past-1/+1 or replace the new gradual age clock. Current ordinary previews exclude those exaggerated stages. `fixMonsterTransparency` keeps ordinary-colour monster forms opaque. These forms/opacity rules fit the project's native geometry and are documented Minecraft adaptations. Hyper X remains a temporary phenotype; its inherited DNA allele remains S.

Birthdays use a configurable loaded-adult calendar (`birthdayYearTicks=24000`, zero disables). An eligible year credit is committed before the timed party starts, so interruption or reload cannot repeat a serving. Nearby eligible guests can join a host once per party; synchronized celebration poses and temporary paper hats accompany the activity. A completed host party produces one marked real cake serving for the normal held-eating transaction. This calendar and physical serving are Minecraft adaptations; the source's exact year clock is not claimed.

## Doctor, classroom and customization

A physical Doctor Station must be within eight blocks of the keeper and Chao for direct clinic actions. A cure consumes one actual medicine unless the keeper has creative abilities. Paid grade care requires level 99 and a grade below A, and allows at most three upgrades this life. The price is `10000 × (old grade + 1)` source rings, converted at 100 rings per emerald. Only the expressed allele increases; the chosen stat's level, progress and points reset to zero. Currency is checked and consumed on the server, with emerald-block change. No charge occurs for rejected care.

A physical Lesson Desk admits four simultaneous students by default. Current offers follow the original 21-step cycle with Triangle, Guitar and Accordion appended. Each offer lasts 36000 ticks and each lesson requires 12000 loaded ticks by default. The newer Wiki timing takes precedence over the older Info Center's conflicting 60/30 minutes. A student retains its original chosen lesson when the offer changes. Unloaded/carried time does not progress. Missing desks, distance, illness, sleep, insufficient fullness, contests and equipped companion status pause learning; restoring conditions resumes the same pending lesson.

Song and drawing have five levels; four dances, exercise and eleven instruments are binary skills. All four dances unlock breakdancing. Knowledge is saved and retained through rebirth, while unfinished classes are cancelled on rebirth. Completion clears the pending transaction before granting knowledge, preventing a completed save from replaying. No Intelligence XP comes from lessons. The first successful instrument class gives one physical prop through a persisted once-only claim. A nearby online keeper gets it immediately; otherwise the classroom's claim button retrieves it later. Physical classroom objects, cancellation and the prop acquisition rule are Minecraft adaptations.

The Analyzer CARE / WARDROBE tab uses actual inventory stacks. Hat, additional head, face, neck and body slots save their equipment and components through all creature transfers. Replacements return their previous item once. Supported accessories consume an actual selected/available dye; identical tint is a no-op. This transfer still consumes the source equipment/dye in creative. The chosen medal must belong to the Chao's earned award flags. [Kindergarten implementation notes](docs/research/KINDERGARTEN_019.md) describe validation boundaries.

## Gardens, races and karate

The PC-inspired Chao Garden Transporter detects owned Chao inside its saved active area and offers six registration slots. Automatic entries follow live placement and pickup. Manual carrier reservations and existing registrations persist across upgrades. The existing containment and ownership rules still govern residents. See [garden registration and boundaries](docs/mechanics/GARDENS_AND_ACTIVITIES.md).

Race courses use straight rows of 2–8 start/finish sections and linked red triangular pennants, with at most 32 blocks per link and 64 links per course. A route must total at least eight blocks and cannot loop. Chao actually navigate it. Run governs land speed, Swim water speed, Power climbing and Fly descent; Stamina supplies virtual race endurance. A five-minute limit returns blocked entrants. Completion and active timed-out participation grant training; winners gain a bonus.

A karate arena is a complete connected 7×7 mat. Up to eight entrants progress through two-Chao bouts, each limited to 90 seconds. Stamina supplies virtual health, Power damage, Swim defence, Fly evasion, Run attack frequency and Intelligence zeal retention. Bouts animate approach, attacks and pushback; virtual damage does not hurt the pet's real health. Every competitor receives training and Karate Experience, with a champion bonus. Lifetime victory and award progress survives reincarnation; the next victory advances the saved total even when the new body has no legacy entity win counter.

Right-click the start or mat to open a shared lobby. Keepers may enter multiple Chao from their registered gardens, including their own Chao in another keeper's free slots; only the host starts/cancels. The server validates ownership, capacity, proximity and construction. Carried/equipped/unavailable Chao cannot become duplicate entrants. Contests save return locations and original control flags; cancellation, block removal, disconnection or restart returns existing bodies without manufacturing missing ones. See [construction and persistence details](docs/mechanics/GARDENS_AND_ACTIVITIES.md).

## Holding and optional companions

Sneak-right-click your Chao with an empty hand to place it in that hand as a filled Chao item, even with no spare inventory slots. No empty carrier is craftable or visible in creative. The held item renders the saved Chao with imported animation and fitted appearance. Use it on clear block space to release it; a successful release consumes the filled item in survival and creative. Blocked or rejected release leaves it intact. Old empty carriers disappear on their inventory tick; existing filled carriers keep working. Capture and release retain the **original entity UUID**, name, keeper and saved state. A copied Carrier cannot release another living entity with that UUID. Cocooned, contest-controlled and already equipped Chao cannot be captured.

With Accessories for NeoForge 1.21.1 and its oωo dependency installed on both sides, a healthy baby or adult can enter the **Chao Companion** slot. One companion follows as a temporary, unsaved world body backed by the item. Unequipping, logout and dimension changes recall it and snapshot the state. Equipped care and adult visual maturity advance. Lifespan and birthday time pause.

Babies provide no gameplay benefits and use head/back carrying. Adults without earned Run/Swim unlocks are carried for that movement; unlocked adults retain level-scaled assistance. All active, energized free-following adults assist combat. Power damage scales from1 to11; Run speed bonus reaches100%, and water speed bonus reaches200%. Fly gives a per-fall budget from40ticks at0 to200ticks at70 and240ticks at98; Fly99 or an all-S robot permits vanilla fall-flying. Double jump to begin elytra flight. Swim70 grants underwater night vision; Swim99 also grants water breathing. Those owned effects end outside water. An all-S robot can boost with defaultR once per100server ticks; replay/slot cycling does not reset the deadline. Hold sneak to descend normally. Abilities consume energy and pause when it is low; grounded rest beside a stationary keeper restores low energy, or full energy while the keeper crouches. Defeat recalls the body for 1200 server ticks. Babies keep their stats; an adult loses ten happiness and one random positive skill level, then returns while equipped. Skills and points cannot fall below zero. The cooldown persists through unequipping and cannot be skipped by releasing the item. See [companion thresholds and save behavior](docs/COMPANIONS.md).

## Garden music

The Chao Garden CD is crafted with eight iron nuggets around an emerald. It uses vanilla jukebox playback and comparator strength 7. The supplied Neutral Garden intro followed by one loop lasts 115.809592 seconds; the song finishes normally and its disc can be ejected and reused. See [pickup and music](docs/PICKUP_AND_MUSIC_015.md).

## Chaos evolution

The check happens when a child first evolves. All conditions must hold:

1. At least two previous reincarnations: third life or later.
2. Happiness **above 80**. This is separate from the above-50 terminal reincarnation threshold.
3. All 31 registered DNA species given during the current life, including the ten additional samples.
4. The retained **Stamina/Normal Chaos balance**: Stamina wins the old five-stat point comparison, including recency ties. This explicit compatibility predicate runs separately from ordinary four-ability selection, preserving the existing rare Chaos path rather than making it unreachable when Stamina was excluded from ordinary first types.

Recorded DNA bits in existing saves are preserved; missing additional species must be received before a future child-to-adult Chaos check. Existing Chaos Chao remain immortal and are not re-evaluated on load. Reincarnation still resets this current-life checklist. The separate Tails/Amy/Knuckles character-form route retains its original 21-species gate.

Repeated animals are allowed. Later training can change the ordinary prospective first type and the separate Chaos balance before evolution; the Analyzer shows the ordinary choice and the Chaos Stamina check separately. Alignment selects **Light** for Neutral, **Angel** for Hero, or **Devil** for Dark. Each has distinct family geometry and eye colors; saved inherited color, tone, shiny and jewel genes now determine its skin finish. Chaos evolution raises Stamina's expressed grade once, clears saved animal-part data, stops old age, protects from ordinary damage, and disables breeding. There are no further reincarnation-based grade improvements.

## Persistence and pack integration

State is server-authoritative and saved with the entity. Egg items and carriers store the corresponding state in item data components. The Analyzer reads synchronized state, including hidden appearance, ability and personality alleles, emotions, illness, learned knowledge and ancestry. The v5 format bounds generic traits and migrates previous state versions without replacing valid identities. Garden ledgers and interrupted-activity return journals are server saved data. Companion items retain the creature identity and recovery deadline; their temporary world bodies are not separately saved. Historical version 4 introduced the first personality/boredom fields; version 5 adds the complete bounded profile and heredity records while retaining previous field order and the final heritage ID. Existing version 2/3 states preserve their genes, phenotype, care and history; missing traits receive safe defaults. Pending gifts are separately persisted with their real item components.

`/chao appearance official [source]` selects an original supplied rig/PNG for the nearest Chao; `/chao appearance garden` restores the customized view. Comparison settings survive saving/carrying without changing the creature's mechanics. Original rendering omits added animal/theme/headwear/held-item/personality deformations. See [native rig research](docs/research/NATIVE_RIG_018.md).

Loot additions use additive NeoForge global loot modifiers, preserving vanilla pools. Pack makers can adjust bonus JSON or remove modifier entries in a data pack; see [LOOT.md](LOOT.md). Main bodies and animation curves come from the supplied official addon assets, converted by the native renderer; custom overlays and gameplay poses remain project work. The source includes twenty model inputs, twenty textures and eight clips in one animation file. See [asset provenance](docs/ASSET_CREDITS_015.md). No third-party animation library is required. Accessories and oωo are optional dependencies for equipped Chao companions.

## Research references

These describe the original games; the sections above describe this mod's balance and limits.

- [Sega: Sonic Adventure instruction manual](https://info.sonicretro.org/images/8/85/SonicAdventure_DC_US_manual.pdf)
- [Sega: Sonic Adventure 2 manual, mirrored transcription](https://manualzz.com/doc/1393870/sega-sonic-adventure-2-video-game-instruction-manual)
- [Chao Island: Genetics](https://chao-island.com/wiki/Genetics)
- [Chao Island: Stats](https://chao-island.com/wiki/Stats)
- [Chao Island: Evolution](https://chao-island.com/wiki/Evolution)
- [Chao Island: Small Animals](https://chao-island.com/wiki/Small_Animals)
- [Chao Island: Animal Parts](https://chao-island.com/info-center/advanced/animal-parts.html)
- [Chao Island: Fruit](https://chao-island.com/wiki/Fruit)
- [Chao Island: Breeding](https://chao-island.com/info-center/advanced/breeding.html)
- [Chao Island: Death and reincarnation](https://chao-island.com/info-center/life-cycle/death.html)
- [Chao Island: Chaos Chao](https://chao-island.com/info-center/advanced/chaos-chao.html)
- [NeoForge 1.21.1: development setup](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [NeoForge 1.21.1: loot tables](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/)


## Omochao component and heart lifecycle

A completed Omochao starts young-adult with uniform C grades and zero trained levels. It walks without free Run points, cannot assimilate animal gifts or parts, cannot become ill, and cannot use biological grade-up care/Hyper Fruit. Other training remains available. Four real accepted parts build one owned UUID; one calibrated heart can replace one part.

At the end of its loaded garden lifespan, happiness above 50 yields one Robo Heart; otherwise it yields one Robot Component. Combat death yields a component. The old garden slot is released and the ended body retires once the physical output and any held/worn item returns have committed. Failed insertion retains the actor and exact stacks for retry; a previously committed component or heart cannot be produced again. A heart carries the next uniform grade tier (C to B to A to S, capped S) and former creator snapshot. The rebuilt robot has fresh levels and the new creator as parent; the old creator is grandparent. Carrier/equipped lifespan time remains paused. Companion defeat still recalls temporarily instead of manufacturing a recovery item.

Version 0.25.41 adds damage-only garden/inventory/storage recovery, durable pickup-retained garden marks, baby equipment and earned-unlock carrying. See docs/COMPANIONS.md for destinations, lifecycle separation, preview commands and failure handling.
