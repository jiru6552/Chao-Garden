# Animal genes and Chao drives, 0.10

Historical 0.10 implementation notes. The training table remains current; progressive acquisition, ear/horn compatibility and child proportions are updated in [Animal Parts 0.11](ANIMAL_PARTS_011.md).

The requested reference is the Chao Island Info Center. The modern SA2/B training profile and its published body-part assignments determine this implementation:

- [SA2/B animals](https://chao-island.com/info-center/training-sa2/animals.html)
- [Chaos Drives](https://chao-island.com/info-center/training-sa2/chaos-drives.html)
- [Animal parts and ghost animals](https://chao-island.com/info-center/advanced/animal-parts.html)
- [NeoForge 1.21.1 global loot modifiers](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/glm/)

## Acquisition and use

Twenty-one original 16-pixel DNA-vial items replace the need for new animal mobs. Each sample has colored liquid, a visible DNA helix, and a tooltip naming its animal, exact stat changes, supplied appearance slots, and special effects. Right-click a Chao you keep to consume one sample. Existing care ownership and feeding cooldown rules apply. The Black Market also rotates these samples through its stock.

Every loot table whose path starts with `chests/`, in any namespace, independently has a 40% chance to add 1–2 uniformly selected Chao drives and a 12% chance to add one uniformly selected animal gene. Existing rewards remain, including the earlier regional care-supply pools, which can add further supplies. Loot with other paths is untouched by this generic chest modifier. This includes conventional modded chest tables; tables using an unrelated custom naming convention need their own datapack entry.

Drives grant 24% of one Swim/Fly/Run/Power progress bar. They influence evolution tendency, add one happiness and two hidden Luck points, and preserve animal appearance. Gene samples add two happiness, one hidden Intelligence point and one Luck point; the exact hidden increments are Minecraft balancing choices, preserving the Info Center's stated relative drive/animal Luck relationship. Stat progress never goes below zero and negative percentages do not remove levels already earned.

## Training percentages

These values are percentages of the next level's progress bar, not direct stat points. The modern Info Center formats 36% as `3.6` segments of a ten-segment bar. Its Skunk, Sheep, and Raccoon entries conflict with the separate Chao Island Wiki's table; this release deliberately follows the **Info Center** requested by the user.

| Animal | Swim | Fly | Run | Power |
|---|---:|---:|---:|---:|
| Penguin | +36 | +12 | +8 | −24 |
| Seal | +40 | −16 | 0 | +8 |
| Otter | +44 | −4 | +8 | −16 |
| Peacock | +12 | +48 | −16 | −12 |
| Parrot | 0 | +48 | −20 | +4 |
| Condor | +20 | +60 | −24 | +16 |
| Rabbit | 0 | +8 | +40 | −16 |
| Cheetah | −8 | −8 | +40 | +8 |
| Boar | −4 | −12 | +32 | +16 |
| Bear | +8 | −16 | +4 | +36 |
| Tiger | −8 | −16 | +20 | +36 |
| Gorilla | −4 | −8 | +4 | +40 |
| Skunk | +8 | +12 | +12 | +8 |
| Sheep | +8 | +12 | +20 | +20 |
| Raccoon | +20 | +12 | +4 | +8 |
| Dragon | +20 | +4 | +8 | +32 |
| Unicorn | +16 | +12 | +36 | 0 |
| Phoenix | +12 | +32 | +4 | +16 |
| Half Fish | +32 | 0 | +8 | +24 |
| Skeleton Dog | +8 | +8 | +32 | +16 |
| Bat | +8 | +40 | +8 | +8 |

## Appearance rules

Each gene only replaces the body slots supplied by that animal. Other acquired slots remain, allowing mixed animals. Ears and horns exclude one another. The original broad rectangular head stays in place; animal ears, low crest blocks, snouts and horns are cuboid additions. Limbs inherit their corresponding walking, swimming, eating and sleeping animations. No new animal mob is registered.

Animal head growth replaces the ordinary crest or a themed head crown, preventing overlapping sets; the themed coat, body and floating marker remain. A gene with only a muzzle keeps the existing crown. All pieces use the existing animal texture atlas and preserve the face plane. Replacement limbs suppress the base limb mesh while keeping its animation transform.

Half Fish changes the floating marker to a green flame and does **not** remove acquired parts. The happy heart temporarily takes priority during petting and eating. Bat hides feet, tail and wings; subsequent genes can replace those slots. Skeleton Dog removes animal parts and restores hidden feet. Its existing saved `hatsUnlocked` flag remains, but this release does not claim a new hat system. These ghost samples use no fabricated physical animal parts.

Chaos forms still receive training and the all-animals checklist, but never acquire animal parts, ghost feet or a replacement flame. Acquired appearance is saved per Chao and is not inherited by offspring. Animal-specific learned actions such as singing and fire breathing are outside this release's implementation and are not advertised by the tooltips.

The canonical registry pattern is `chao_garden:<animal>_animal_gene`. The Boar uses the established registry spelling `warthog_animal_gene`, matching the project's existing item catalog. New mob/rescue/chest loot uses these canonical samples. The old `<animal>_animal` IDs remain functioning aliases for saved inventories and datapacks and receive the new vial appearance.

## Verification

`AnimalGeneStateTest` has independent fixtures for all 21 stat profiles, save roundtrips, hidden-stat gains, mixed slots, ghost effects, drive appearance preservation and Chaos protection. `GeneGameTests` exercises actual entity interactions for every sample, ownership refusal, the loot codec, a foreign-namespace chest, and registered vanilla chest loot. `validate_gene_assets.py` checks the 21 distinct DNA icons, compatibility items, additive generator behavior, exact authored slot coverage and Chaos-safe geometry. `validate_poses.py --forms animals` compiles the production animation source and sweeps all 21 individual sets through head movement, six care/movement activities and synchronized karate/companion punches, with extra themed/Chaos visibility assertions.
