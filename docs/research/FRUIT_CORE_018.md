# Fruit and core mechanics research — 0.18.0

This is a bounded correction of the existing simulation, with SA2 Battle/HD taking precedence over Dreamcast behavior wherever both provide an implementation. It is not a claim to reproduce every original internal variable. Research was reviewed on 30 October 2026.

## Source coverage

The live wiki endpoints returned HTTP 403 during this session. Exact copies of all nine requested pages were recovered from the public Internet Archive; the current Info Center pages were used as a cross-check. The wiki itself marks several articles as drafts, and explicitly calls some personality research speculative. The dates below identify the recovered copies, rather than claiming that they are current wiki revisions.

| Requested page | Recovered snapshot | Review and implementation |
| --- | --- | --- |
| [Fruit (DC)](https://chao-island.com/wiki/Fruit_(DC)) | 17 December 2024, revision 5161 | Reviewed the entire **Sonic Adventure 2** section and all ten newly authored fruit reference renders. Chao Adventure 1 fruit are excluded. |
| [Egg](https://chao-island.com/wiki/Egg) | 14 February 2025, revision 4149 | Reviewed modern egg acquisition, hatching and feature assignment. Existing timed hatching and stored egg identity remain; shaking and throw-to-hatch are not implemented. |
| [Breeding](https://chao-island.com/wiki/Breeding) | 17 December 2024, revision 4934 | Adult, non-Chaos breeding and diploid inheritance retained; whole Heart Fruit is committed after the new eating animation. |
| [Genetics](https://chao-island.com/wiki/Genetics) | 13 January 2025, revision 5154 | Corrected higher-grade expression to 70%; retained both grade alleles; added saved favorite-fruit alleles. |
| [Stats](https://chao-island.com/wiki/Stats) | 17 December 2024 | Reviewed modern grades, level-up formula, movement milestones, hidden stats and reincarnation. Existing visible-stat formula and movement thresholds are retained. Hidden luck/intelligence remain a simpler Minecraft model. |
| [Alignment](https://chao-island.com/wiki/Alignment) | 17 December 2024, revision 3503 | Hero/Dark thresholds and whole-fruit influence retained. Reincarnation now preserves the previous alignment category. |
| [Personality & Emotion](https://chao-island.com/wiki/Personality_%26_Emotion) | 17 December 2024, revision 5115 | Reviewed profiles, sliders, inheritance uncertainty and emotions. Added saved personality, boredom, considered feeding and four CA2 fruit tendencies. |
| [Evolution](https://chao-island.com/wiki/Evolution) | 17 December 2024 | Reviewed first-evolution influence, tie priority and grade upgrade. Adult secondary appearance now follows the two axes without the old arbitrary delay. |
| [Death & Reincarnation](https://chao-island.com/wiki/Death_%26_Reincarnation) | 17 December 2024, revision 5509 | Corrected SA2 reincarnation to happiness **greater than 50**, resets personality, retains alignment category and preserves existing stat/grade retention. |

The recovered pages can be reached with the public archive URL prefix `https://web.archive.org/web/20250000000000id_/` followed by the original URL. Raw downloaded page bodies and reference images are research inputs outside the shipped project; they are not included as authored assets.

Live cross-checks included [personality](https://chao-island.com/info-center/advanced/personality.html), [feeding](https://chao-island.com/info-center/basics/feeding.html), the [CA2 fruit guide](https://chao-island.com/guides/sonic-adventure-2/chao-adventure-2/fruit-seeds.html), and the gallery's [gradual evolution types](https://chao-island.com/gallery/advanced-evolution/types.html). The CA2 guide has an incomplete table and some entries that appear copied from other versions; the exact Fruit (DC) wiki's SA2 table supplies the additional identities/effects. Gallery research separately inspected all 35 navigation tabs and the two-axis evolution diagram.

## Chao Adventure 2 fruit

The source's VMU table contains nineteen fruit profiles. Nine already exist in the mod: Chao, Heart, Round, Triangle, Square, Hero, Dark, Tasty and Strong. Their modern SA2 Battle effects take priority; in particular modern Strong Fruit is not made unpalatable to Dark Chao just because its Dreamcast counterpart was.

Ten additional profiles are active in 0.18. Smart Fruit reuses its old saved ID. Every new fruit has an authored 16-pixel item, a matching seed, a seedling, a leafy orchard family and four hanging-pod stages. Their silhouettes and palettes were compared with all ten actual reference renders. No reference image pixels are embedded in the new art.

| Item ID | Source-specific additional effect | Authored visual cues |
| --- | --- | --- |
| `energy_fruit` | Curiosity +10, Energetic +10, toward Naive 10, Carefree −10 | Orange/red body, yellow shoulder, large green leaves |
| `peace_fruit` | The opposite four changes | Pale green pear, white horizontal bands |
| `smart_fruit` | Intelligence base +10 | Purple joined bulbs and a green stem |
| `tall_fruit` | No additional effect | Ribbed cyan/blue onion |
| `vitality_fruit` | No additional effect | Green top, orange middle, brown base |
| `rare_fruit` | Disliked by Dark Chao | Mustard pointed upper half, orange lower half |
| `wonder_fruit` | Disliked by Dark Chao | White upper half grading to dark blue, vertical ribs |
| `creepy_fruit` | Disliked by Hero Chao | Teal body with red vertical stripes |
| `rough_fruit` | Disliked by Hero Chao | Elongated, faceted purple body |
| `forbidden_fruit` | Disliked by Hero Chao | Mottled mustard/brown body, dark purple tip |

Dreamcast uses a different stamina system: base nourishment per bite and additional effects on the last bite. This mod uses its established modern whole-fruit scale: each new profile contributes **40% of a stamina level bar**, fills 25 fullness points and applies its extra effect once, after eating finishes. Smart's +10 is mapped directly to the existing simplified intelligence units. Energy changes a personality tendency; it does not invent an immediate energy-battery effect.

The Black Market includes one rotating Adventure fruit offer in each stock, costing eight emeralds. This price and the rotating offer are Minecraft adaptations, not a claim about a canonical CA2 shop price. Acquire a fruit first, craft one seed, then grow it. Adventure seeds do not appear as direct merchant offers or natural Overworld trees. The five existing natural families retain their world generation and farmer seeds. The two mushroom foods have no new seed acquisition. There are **24 active foods and 22 active seed families**; old Stamina Fruit orchards and mushroom block IDs remain registered for saves.

`art/build_ca2_fruit_018.py` is the narrow final asset pass, run after the earlier fruit and 0.17 market generators. It creates only the ten additional profiles and merges the language entries. It also removes any accidentally regenerated mushroom seed recipes and keeps their old tree loot from introducing new mushroom seeds.

## Choice and personality

The fifteen saved Doctor labels are No personality, Gentle, Naughty, Energetic, Quiet, Big eater, Chatty, Easily bored, Curious, Carefree, Careless, Smart, Cry baby, Lonely and Naive. New egg identity assigns a label independently of parents. Source research does not establish a reliable personality inheritance rule, so no invented inheritance is added.

The original Doctor may cycle among several labels determined by thirteen sliders. This implementation stores one label and the four signed tendencies specifically affected by Energy/Peace Fruit. It does not pretend to implement that complete profile system. The Naive slider runs toward negative values; Energy decreases it and Peace increases it.

`ChaoPreferences` provides pure rules shared by held-item decisions, toys and animation. Numeric thresholds and timers below are explicit Minecraft balance choices:

- Existing `hunger` is actually **fullness**, from 0 hungry to 100 full. An ordinary Chao accepts below 85, or below 95 for its favorite shape. Quiet and Cry baby Chao normally use 80. Big eaters accept even at 100, reflecting the source's documented eating-while-full exception.
- Adult Hero/Dark forms can refuse alignment fruit and the documented CA2 dislikes regardless of appetite. Children can eat either alignment fruit, allowing training away from a retained Hero/Dark category after rebirth. Modern Strong Fruit keeps its modern neutral profile.
- A loaded Chao loses one fullness point per 300 ticks: fifteen seconds. From full, an ordinary Chao wants another ordinary meal after about four minutes. This replaces the old 1200-tick clock, which made the new refusal behavior overly restrictive.
- Smart and Big eater Chao ponder more quickly; Curious, Quiet and Cry baby Chao take longer. The bounded decision duration is 18–70 ticks. Curiosity can lengthen inspection.
- Boredom increases while awake, and care, feeding and completed voluntary play reduce it. Boredom and activity tendencies influence the chance to choose a placed toy. Exhausted, sleeping, dead or cocooned Chao do not begin training/play.

Effects and item consumption remain transactional: the item-handling system applies these effects only after completing the eating/use action. Merely offering an item does not award stats. Unknown dropped items can be inspected and thrown away.

## Inheritance, evolution and saves

Stat-grade offspring retain one allele from each parent; different grades express the higher allele with 70% probability. Evolution upgrades only the expressed allele, leaving its hidden counterpart intact. Color's regular allele stays recessive; nonregular colors and tone are equally dominant; shiny and jewel remain dominant over their absence. Favorite-fruit phenotype is chosen from the two inherited, equally dominant shape-preference alleles.

The sources conflict on initial garden stat genes: the Genetics page says all starter/shop genes are pure, while the Stats page distinguishes heterozygous garden starters from pure Black Market Chao. This mod has no automatic starter-garden population. Newly generated shop/loot/creative eggs use pure grade pairs; a future starter-garden feature should choose a separate initialization policy. Existing saved heterozygotes are never normalized.

First evolution raises the expressed grade for its ability type; Normal and Chaos raise Stamina. Only the expressed allele is upgraded and S remains the cap. The two adult axes support Normal, Swim, Fly, Run, Power and the four mixed forms Swim+Run, Swim+Power, Fly+Run and Fly+Power. Their current influence can be reversed by training, with no second cocoon. Rendering interpolates the axes rather than treating them as another permanent evolution. First-evolution type remains locked.

Happy rebirth requires happiness >50, resets levels to 1 and retains one tenth of stat points rounded down, grades and genetic identity. Hero/Dark rebirth preserves its category at +1/−1; Neutral returns to 0. Personality/tendencies reset for the new life. The new label can coincidentally match the previous label.

Version 4 keeps the existing semicolon field order and theme as the last field. Its care array grows from 29 to 37 bounded integers. Versions 2 and 3 still decode without altering existing phenotype, genes, acquired parts or theme. Older saves receive No personality, zero boredom/tendencies and a pure favorite-fruit pair reconstructed from their saved preference. This is migration of missing data, not a claim that the original Chao had no hidden preference allele.

Remaining deliberate limits include a simplified luck/intelligence system, an adult alignment value locked with its visual category, no shake/throw egg hatching, no illness simulation, and no full thirteen-slider personality system. The source describes some animal parts and hats surviving reincarnation; the existing Minecraft lifecycle still clears parts and handles hats as items. Those behaviors were not silently rewritten without a complete persistence/lifecycle implementation.

## Validation

Pure executable checks cover v4 round trips and v2/v3 migration, inheritance/70% grade expression, hidden favorite-fruit alleles, appetite/favorite/dislike/Big eater decisions, boredom/timing, strict reincarnation threshold and alignment, reversible mixed secondary forms and the fullness clock. Fruit rules cover all ten new profiles, modern overlap precedence and acquisition classification. Existing `FruitGameTests` loops now exercise all active/registered families, including growth, recipe, obstruction, harvest, regrowth, leaf behavior and exclusion of market-only families from wild generation.

Standalone checks completed during implementation: **70,466 ChaoState checks** (including 16,052 preference checks, migration, variants, animal genetics and focused Normal/Chaos Stamina-grade cases), **55 fruit rule checks**, and **122 native asset structure checks**. The asset validator checks both singular/current and plural/legacy mushroom recipe paths and runs as the full pipeline's final guard. Full Gradle/server/client validation is recorded separately in the release testing document; these standalone results do not assert that a Minecraft client was already exercised.
