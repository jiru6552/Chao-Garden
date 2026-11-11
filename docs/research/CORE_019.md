# 0.19 core: source coverage and simulation boundaries

This file covers the Minecraft-independent state, heredity, needs, illness, and reincarnation code. Runtime movement, animations, classroom UI, gifts, activities, and stock have separate hosts and tests. A stored source field is not presented as a fully understood original-game mechanic.

## Primary references checked

- [Chao World Extended](https://chao-island.com/wiki/Chao_World_Extended): the complete December 2024 page (9.5.3.4), plus current search-cached primary-page sections including 9.6.2 changes. The current page/printable/API requests returned 403 during research; this is not evidence that every current subsection was available. This release selects applicable garden additions rather than claiming to port the whole external mod.
- [Personality & Emotion](https://chao-island.com/wiki/Personality_%26_Emotion): complete archived page, snapshot 2024-12-17T19:33:21Z, revision 5115. Used for constraints, Doctor threshold, emotion purposes, and six illness meters. Current primary-page cached text was checked for updates.
- [Genetics](https://chao-island.com/wiki/Genetics): current cached primary-page body supplies personality alleles 0–4, initial `(allele−2)*40`, equally dominant inheritance, and hatch-treatment DNA changes. Archived revision 5154, snapshot 2025-01-13T11:24:09Z, still calls most personality heredity unknown. The current information supersedes that old statement; we do not attribute the newer formula to 5154.
- [Stats](https://chao-island.com/wiki/Stats): complete snapshot 2024-12-17T00:07:57Z, revision 5159, and current primary cached text. Its specific hidden-stat section gives Luck/Intelligence alleles 0–255, level-gain formula, bar gains, and4000 point cap. This takes precedence over the Genetics page's generic E–S description of stat genes.
- [Death & Reincarnation](https://chao-island.com/wiki/Death_%26_Reincarnation): complete snapshot 2024-12-17T10:36:19Z, revision 5509. Modern SA2B happiness threshold, retained lessons/prizes/hats, DNA reset, and random part removal were checked. The explicit 30% per-slot probability appears in its SADX technical section.
- [Small Animals](https://chao-island.com/wiki/Small_Animals): current primary table supplies modern SADX animal training and part slots. Original21 animal indices remain stable. Ten additional samples are genes, not new mobs.
- [Egg](https://chao-island.com/wiki/Egg): gentle/thrown hatch treatment affects descendant personality genotype rather than the newborn's already selected personality phenotype. The egg host owns one-time application and rough-hatch bond/dizzy effects.

## Personality

All 13 sliders persist in −100..100, with two 0..4 alleles each. Fresh nonbred eggs have matching alleles. Each inherited allele comes from its corresponding parent, and one inherited allele determines the initial slider. Phenotypes never reroll on load. Gentle hatch increments both Kindness alleles and decrements both Energy/Aggressiveness alleles; thrown hatch reverses this. Values clamp0..4. Neither treatment changes current sliders, current Doctor labels, happiness, or favourite fruit.

Doctor projections are active when magnitude exceeds 54, cycle through at most three labels, and retain all raw sliders even when a label is not among those three. Selection of the strongest three and several label-to-slider projections are explicit Minecraft interpretations where the source does not give a complete projection algorithm. They are not represented as a recovered original-game function.

| Stored slider | Implemented consumer | Accuracy boundary |
|---|---|---|
| Curious | Boredom growth, food acceptance, item inspection, voluntary play | CWE direction; Minecraft rates and label projection |
| Cry baby / Energetic | Cry pressure, play frequency, animation speed | Source direction; Minecraft weights |
| Naive / Normal | Earlier sleepiness and longer naps | CWE direction; Minecraft clock |
| Big eater | Fullness exception and stomach-ache resistance | Source/CWE behavior; Minecraft selection |
| Carefree | Slower boredom, lower abuse dislike, slower clips | CWE direction; bounded Minecraft weights |
| Kindness | Social/bullying choices in entity host | CWE direction; Minecraft selection |
| Solitude | Loneliness growth and attention choices | CWE direction; Minecraft rates |
| Vitality | Roaming and desire-to-mate growth | CWE direction; Minecraft rates |
| Regain | Surprise, tiredness, nap, and behavior recovery | CWE direction; Minecraft rates |
| Skillful | Drive/gene absorption time | CWE direction; Minecraft clip durations |
| Charm | Persisted and visible to Doctor | Purpose unverified; no fabricated consumer |
| Chatty | Social choice frequency | Source identity; authored behavior weights |
| Fickle | Relax duration | CWE direction; Minecraft decay |

The v4 save stored 15 independent labels and only four tendency values; its random label cannot be uniquely reconstructed from those tendencies. Migration therefore preserves all four actual values, including zero and fruit-induced nonmultiples of 40, and preserves its old label as a saved compatibility overlay. Intentional future personality fruit or slider edits end that overlay and derive the profile from the expanded sliders. Missing old alleles are reconstructed to the nearest permitted allele; this cannot recover history that was never saved. Tests cover all 15 old labels with both zero and nonzero tendencies.

## Emotion and illness

The seven short emotion slots (Joy, Anger, Cry, Fear, Surprise, Relax, Total) use 0..200. Sleepiness, Tiredness, Hungry, Mate, Boredom, Lonely, Tire, Stress, Nourish, Condition, Energy use 0..10,000. The legacy `hunger` HUD/API means fullness, so source Hungry is `(100−fullness)*100`; it is not accidentally inverted twice. Fractional meter values survive integer HUD mirrors and serialization, while direct old API writes are synchronized.

Active meters drive sleep/grogginess, item and toy interest, social attention, emotion cues, resting, roaming and mating in their entity hosts. Feeding and petting calm needs; hurt, refusal, and interrupted sleep produce corresponding reactions. Source Energy is also the mod's existing stamina/roaming budget, an explicit shared-meter adaptation. Condition has the observed stationary/Nourish relationship via `onGardenMotion(boolean moving)`; exact rate and symptom-suppression threshold are authored because the source describes observed behavior, not a complete formula.

Total is unused in the final original game. Stress also has no natural original-game source; neither accumulates naturally here, and Stress is not given an invented happiness override or death rule. Nourish decays from its birth value; its ultimate purpose remains unknown and is not invented as healing or damage. These fields still persist and are exposed honestly.

Cough, Cold, Rash, Runny Nose, Hiccups and Stomach Ache each persist−100..100. Negative values are active; Doctor treatment sets100. Runny Nose was unfinished/unused in the base game and is an expanded feature here. Immunity duration24,000 loaded ticks, onset checks once per 1,200 ticks, and odds 1/480 (1/48 under poor fullness/energy) are Minecraft balance values. Swim forms resist Cold/Runny Nose; Power forms resist Hiccups/Stomach Ache, and Big Eater resists Stomach Ache under the chosen CWE rules. Rare immune forms do not acquire natural illnesses. Each illness and immunity timer roundtrips independently.

## Hidden stats and grades

Luck and Intelligence now have separate diploid byte-grade genes, fixed expressed grades, level 0..99, fractional100-point training bars, and points 0..4,000. A child starts with zero trained points, rather than receiving an average of parental points. Each animal adds 20 Luck/20 Intelligence bar units; each drive adds 40/20. A level gives `13+3*grade+uniform(−2..2)`, including inherited grades above5. Higher-grade expression uses the general visible-stat70% bias as an inference; the hidden-specific dominance algorithm was not independently confirmed. Generated eggs use pure grades 0..9 as a Minecraft shop-distribution choice; the full inherited 0..255 range remains supported.

Doctor grade upgrades require level 99, allow E→D→C→B→A only, at most three purchases per life, and cost 10,000×(old rank+1) source rings. The host maps/charges currency. Only the expressed allele upgrades; the other allele remains. That stat's level/bar/points reset so the stronger grade can grow fully. Source Hyper fruit eligibility allows one S→X rank across the four movement/Power stats per life; X resets to S at rebirth. X is a temporary phenotype boost; keeping its DNA allele at S is a declared nonheritable interpretation because the source does not establish X heredity. The current X gain 29..33 for all four is an explicit Minecraft formula. CWE reported caps suggest a different exact Fly/Swim/Power formula, which was not verified; no exact-CWE-X claim is made.

## Reincarnation and rare forms

Modern happiness strictly greater than 50 reincarnates; exactly 50 does not. The host's natural-death toggle may choose a gentler outcome. Immortal adults still advance a saved, saturating adult-calendar counter for birthday scheduling; this never triggers lifespan transitions and never reuses their prior child age. Visible and hidden levels become 1, bars 0, and points become floor(previous/10), at most 500. Stat grade genes, appearance, happiness, hats/unlocks, special flame/ghost traits, learned lessons and rewards survive. Personality DNA/sliders reset. The current-life animal checklist resets; a deterministic saved-entropy roll removes 30% of each acquired part slot. Random retention is source modern behavior; borrowing the exact SADX 30% probability for SA2-style Chao is declared adaptation. Reloading cannot reroll it. Unfinished lessons and per-life Doctor purchases clear.

Chaos qualification still requires the original 21 animals in this life; additional CWE sample bits do not break the criterion. Default rare-form breeding/animal parts follow original restrictions. `canBreed(boolean)`, `breed(...,boolean)` and `giveAnimal(...,boolean)` permit an explicitly enabled extension. A persisted rare-parts permission keeps opted-in appearance on reload. CWE Egg Chao has one1% breeding-time roll, saved as `character=egg`; it is a cosmetic ordinary lifecycle because the source lists no infertility/immortality restriction. Other special character creation belongs to the content host.

## Optional monster development and birth irises

`gardenBehavior.monsterEvolution` defaults false. With it enabled, an ordinary adult can continue accepting useful drives/genes past the normal ±1 influence cap after the corresponding stat reaches 2,000 points. Influence is saved, bounded at ±3, and stage2 requires 3,000 points. Switching the code off stops the additional rendering/acceptance. A balanced primary Normal adult uses high Stamina as the source specifies; the exact Normal magnitude equation was unavailable, so its 2,000/3,000 gates and continuous projection are identified as authored. Existing special character and Chaos forms are excluded. Rebirth clears adult development and reduces points, so it cannot preserve monster status in a child.

Only ordinary-colour, non-Jewel, typed monster forms become translucent. Stage opacities0.78/0.58 are Minecraft visual choices; the source does not give those values. `fixMonsterTransparency` defaults false and makes such forms opaque when enabled. Both effective options persist in dedicated state fields so connected clients display the same shape. Forms use additional authored silhouettes beyond the existing primary/secondary morphs; raw official comparison mode keeps its reference geometry.

`newbornEyeChancePercent` defaults0 (off), accepts0..100, and controls a single random ordinary iris choice0..8 at new-state birth. The source supplies no probability, so the configured chance is an adaptation. Special/alignment irises9..12 remain lens-only. Generated/natural Chao, newly prepared eggs and bred children call the birth overloads; existing eggs and reincarnated state do not. The chosen iris persists through reload/capture and does not overwrite a lens on rebirth. Constructed Omochao does not receive a biological newborn-eye roll.

## Additional genes

Original indices 0..20 remain unchanged; appended indices 21..30 are Bee, Ladybug, Scorpion, Deer, Elephant, Kangaroo, Koala, Lion, Mole and Swallow. The first three use CWE's 2.5-bar Fly/Fly/Power gains; their recognizable part geometry/masks are authored. The seven SADX modern sample gains and anatomy masks come from the primary table. One sample progressively expresses one available part; that progression is an authored Minecraft rule that preserves mixed parts.

| Sample | Swim / Fly / Run / Power bar units | Source part slots |
|---|---|---|
| Deer | −4 / −12 /32 /16 | Arms, ears, feet, horns, tail |
| Elephant | −8 / −16 /20 /36 | Arms, ears, feet, tail |
| Kangaroo | −8 / −8 /40 /8 | Arms, ears, feet, tail |
| Koala |20 /16 /4 /8 | Arms, ears, feet, tail |
| Lion |8 / −16 /4 /36 | Arms, ears, forehead, feet, tail |
| Mole |8 /8 /16 /16 | Arms, feet, tail |
| Swallow |20 /60 / −24 /16 | Feet, tail, wings |

## Save/API safety and verification

Schema 5 appends personality genes/sliders, emotion/illness arrays, hidden genes/training, explicit cosmetic fields and a bounded extension-trait map. Existing numeric sections, known genes, theme identity and legacy APIs remain supported. Versions 2/3/4 upgrade without rerolling saved appearance or training. Theme, character, lens, bright/negative/wing/brush appearance fields are separate. Lens/gift appearance does not silently become hereditary; the optional CWE random-newborn-eye code has no verified inheritance rule.

Family snapshots store two stable parent IDs/DNA summaries and up to four grandparent IDs/DNA summaries. Unknown relatives stay unknown. The ledger copies only one further generation and includes coat, visible/hidden grade and personality genotype; acquired training and animal parts do not become hereditary. IDs are assigned once from birth/migration entropy and saved in a dedicated64-bit field; subsequent PRNG entropy changes never alter them. This does not consume feature-trait capacity. IDs are stable save-local references, not globally unique world entity UUIDs.

The extension map allows 256 entries, keys 48 printable ASCII identifier characters, values 128 printable ASCII characters, and a 20,000-character encoded budget. Setters return false on refusal and never evict another feature's data. This leaves room below Minecraft's32,767-character synchronized string limit. Learned lessons, medals, active sessions and cosmetic identity use stable named keys. Serialization validates counts/ranges and rejects oversized malformed payloads. It does not execute payload content.

`ChaoLifeSystem019Test` adds source-boundary, all-label migration, fixed-phenotype hatch DNA, hidden inheritance/training, signed bond, illness immunity, fractional needs, Naive/Carefree contrasts, random part retention, source animal anatomy, Egg Chao birth, trait capacity, and per-life grade checks. The complete standalone mechanics suite passes; release reports include exact current counts. Standalone availability tests use an isolated ModList stub; real NeoForge compilation and server integration are separate release checks.
