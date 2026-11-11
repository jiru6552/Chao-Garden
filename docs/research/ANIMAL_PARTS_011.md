# Progressive animal parts — 0.11

Animal Gene samples now build a Chao's appearance one part at a time. The 0.10 implementation granted the animal's complete set on its first use; it already preserved unrelated slots, but did not provide progressive acquisition.

## Source rules

The requested [Chao Island animal-parts guide](https://chao-island.com/info-center/advanced/animal-parts.html) defines eight SA2/B slots: arms, ears, forehead, face, feet, horns, tail and wings. Each animal supplies only its listed slots. New animal parts can replace older ones, so a Chao can combine features from multiple animals. Adult parts are more pronounced.

Its ear/horn restriction concerns **different animals**. Sheep and Dragon each supply both ears and horns, and their matching pair can coexist. An ear acquired from another animal replaces the conflicting horns, and a horn acquired from another animal replaces the conflicting ears. Unrelated head, body and limb slots remain.

[Nostalgia Ninja's authored animal-modding guide](https://nostalgianinja.github.io/ChaoModding_Docs/AnimalModding/) also describes random partial acquisition on absorption and separate adult appearance models. Neither source inspected for this release establishes an exact vanilla acquisition probability or numbered strength tiers. This mod does not claim either.

## Minecraft acquisition rule

An ordinary sample selects **one random slot** from the animal's supplied slots that is not already expressed by that animal. It adds or replaces that one part. Repeated samples therefore complete the set without repeatedly selecting a part already present. A complete same-animal set stays intact; further samples still grant its existing training and care benefits.

This guaranteed one-part rule is an authored Minecraft adaptation. There is no additional inventory item or hidden intensity meter. One sample also records that animal for Chaos evolution immediately; collecting every possible visible part is not required for the checklist.

For example, four Rabbit samples acquire its arms, ears, feet and tail in a varied order. Four Peacock samples can then replace the feet and tail and add its crest and wings, leaving the Rabbit arms and ears. The resulting Chao has six occupied appearance slots from two animals. Any later sample changes only its selected supported slot, plus an incompatible opposite ear/horn slot when necessary.

The existing special cases remain: Half Fish changes the floating marker; Bat hides feet, tail and wings until replacement parts arrive; Skeleton Dog clears acquired parts and restores hidden feet. Chaos Chao keep their own forms while receiving training and checklist entries. Drives never add or remove animal parts.

## Appearance and persistence

Adult animal geometry retains its full authored proportions. Child ears, crests, horns, muzzles, tails and wings are smaller; replacement arm and foot bulk is reduced while keeping shoulder connections and the soles in place. These are proportional Minecraft versions, rather than claims of exact console meshes or colors. The broad Chao head, block-built additions and inherited coats remain.

No save-schema change is required. Existing complete or mixed sets remain as saved; future samples use the new incremental rule. Partial sets and the saved random history survive entity saves, carriers and the existing companion snapshot path. Acquired parts are not inherited by breeding or carried through reincarnation, following the existing lifecycle rules.

## Verification

`AnimalGeneStateTest` exercises progressive acquisition for all eighteen ordinary animals, both child and adult states and 32 independent random histories; mixed slots, compatible Sheep/Dragon head pairs, incompatible head replacement, exact persistence, completion stability, ghost cases and Chaos protection. `GeneGameTests.repeatedGeneSamplesAccumulateMixedPartsAndPersistInCarrier` uses actual care interactions at normal cooldown intervals, then verifies the mixed creature's carrier state and identity.

The production-model pose validator completes each set through repeated samples, tests both child and adult individual animals, and includes Rabbit/Peacock, Sheep/Peacock and Dragon/Peacock mixed sets. It asserts the exact expected visible parts, reduced child proportions, inherited themed coats and Chaos precedence, then checks actual animation transforms for head/limb clearance.

Completed affected-feature checks: **32,386 core animal assertions** passed using the production state source. The full affected-model sweep passed **176,400 actual `setupAnim` poses** and **3,109,050 conservative head/limb SAT checks** across 48 child/adult/combined animal cases, seven activities (including punches), head yaw ±90° and input pitch ±60°. Minimum conservative clearance was **0.0505 model units**. These numerical checks cover the changed animal cases; unchanged ordinary and themed geometry retains its separately recorded validation. Actual care-interaction GameTests and final Minecraft captures are recorded by the release owner after integration.
