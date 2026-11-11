# Gameplay forms and gestures, 0.19

The 20 supplied Marketplace models, PNGs and eight animation clips remain the raw comparison set. Gameplay uses separate authored mesh files on the same semantic skeletons. New cuboid shapes adapt the inspected game references to the addon's stepped Minecraft construction; they are not newly extracted SA2 meshes.

## Reference review

The primary gallery pages for Normal, Swim, Fly, Run and Power were inspected for all three alignments. The advanced evolution pages for Types, Neutral, Hero and Dark were inspected with all 135 pictured adult combinations: 45 for each alignment. Four composite review sheets cover all 15 primary and 135 advanced reference pictures, plus the gradual-evolution diagram (151 downloaded reference pictures). `forms019-reference-manifest.json` records the source URLs and inspection inputs without embedding their pixels. This focused review supplements the earlier full 35-gallery-tab audit.

Sources:

- https://chao-island.com/gallery/chao-types/normal.html
- https://chao-island.com/gallery/chao-types/swim.html
- https://chao-island.com/gallery/chao-types/fly.html
- https://chao-island.com/gallery/chao-types/run.html
- https://chao-island.com/gallery/chao-types/power.html
- https://chao-island.com/gallery/advanced-evolution/types.html
- https://chao-island.com/gallery/advanced-evolution/neutral.html
- https://chao-island.com/gallery/advanced-evolution/hero.html
- https://chao-island.com/gallery/advanced-evolution/dark.html
- https://chao-island.com/gallery/special-chao/character.html
- https://chao-island.com/gallery/hacked-chao/omochao.html
- https://chao-island.com/gallery/hacked-chao/egg.html
- https://chao-island.com/info-center/advanced/animal-parts.html
- https://chao-island.com/wiki/Chao_World_Extended
- Spartoi's original Chao Island picture, recovered and visually inspected: https://web.archive.org/web/20241212181034im_/https://chao-island.com/w/images/thumb/1/1f/Cwe-spartoi.png/100px-Cwe-spartoi.png

## Complete primary geometry matrix

Every row below has nine continuous development endpoints. Native skeleton/UV aliases select anchors and source coat layout; gameplay file names select distinct authored shapes.

| Alignment | Primary | Gameplay key | Skeleton / UV alias | Main silhouette |
|---|---|---|---|---|
| Neutral | Normal | baby | baby | Small central crest |
| Neutral | Swim | swim | swim | Flattened broad aquatic head |
| Neutral | Fly | fly | fly | Paired upright forked feelers |
| Neutral | Run | run | run | Swept central head and quills |
| Neutral | Power | power | power | Broad heavy central crown |
| Hero | Normal | hero | hero | Short upturned crest and halo |
| Hero | Swim | hero_swim | swim | Paired stepped back-drooping feelers and halo |
| Hero | Fly | hero_fly | hero_fly | High forked feelers and halo |
| Hero | Run | hero_run | run | White rounded head, short single swept crest and halo |
| Hero | Power | hero_power | hero_power | Broad layered crown and halo |
| Dark | Normal | dark | dark | Low angled crown and star |
| Dark | Swim | dark_swim | dark_swim | Broad swept aquatic head and star |
| Dark | Fly | dark_fly | fly | Outward forked side horns and star |
| Dark | Run | dark_run | dark_run | Swept Shadow-like quills and star |
| Dark | Power | dark_power | power | Heavy stepped central cone and star |

The four previously absent forms replace the donor's head ornaments with their own connected cuboids. They no longer display the unmodified closest donor rig.

## Nine endpoints for every primary

| Swim/Fly axis | Run/Power axis | Development endpoint |
|---|---|---|
| -1 | -1 | Swim + Run |
| -1 | 0 | Swim |
| -1 | +1 | Swim + Power |
| 0 | -1 | Run |
| 0 | 0 | Normal |
| 0 | +1 | Power |
| +1 | -1 | Fly + Run |
| +1 | 0 | Fly |
| +1 | +1 | Fly + Power |

Thus 15 primaries × 9 endpoints = **135 distinct adult geometric endpoints**. This includes the five normal/single-ability endpoints, four mixed endpoints, and same-primary double evolutions. The actual training axes blend vertices bilinearly between adjacent endpoints with the same topology. There is no threshold mesh replacement; feeding the opposite axis reverses the geometry. Head width/depth, feet, torso, arms, wings and tail participate. Five connected stepped head ridges grow from the head; running sweeps them backward, flying separates the high forks, swimming broadens the profile and power builds the crown. Sonic and Shadow cues inform the stronger double-run sweep. Semantic bone pivots and parent relationships remain fixed, keeping hands, wings, animal parts and accessories attached during the morph.

## Characters and appearance

Amy has pink hair, stepped fringe, red band and dress. Spartoi follows the inspected purple/magenta Chao picture: dark stepped brow, paired blue horns, blue feet, pink hands, jagged white chest marking and gold eyes. Omochao has blue robotic panels, gold eye rings and an orange looped windup key; its key turns during gameplay. Full-Egg has a closed stepped shell with yellow cap, blue spots and black eyes and rolls while moving. Supplied Tails and Knuckles geometry is reused through separate gameplay entries.

The new colored lens pieces support all 13 lens settings, preserving original unmodified eyes at setting zero. Negative, bright and body-colored-wing cosmetics use cached native-resolution copies with source alpha and eye texels protected. Hero brushes recolor accent pixels only (Swim yellow, Fly purple, Run green, Power red); they preserve the primary type, geometry and training axes. Chaos wing accents also accept the body-color cosmetic. Texture allocations retire after the current frame and clear on resource reload.

## New gene geometry

All source SADX animal slots are preserved. Bee, ladybug and scorpion visual sets are authored additions. Each advertised slot has actual geometry and every set is fitted to all 20 source skeleton layouts, using the same animated anchors. Existing 21 animals retain their earlier assets.

| Animal | Slots | Recognizable shape |
|---|---|---|
| Bee | Feet, horns, wings | Striped feet, antennae and pale paired wings |
| Ladybug | Forehead, horns, wings | Spotted cap, antennae and spotted wing plates |
| Scorpion | Arms, feet, tail | Foreclaws, toes and segmented curved stinger |
| Deer | Arms, ears, feet, horns, tail | Hooves, inner ears, branched antlers and short tail |
| Elephant | Arms, ears, feet, tail | Broad inner ears, round paws and thin tail |
| Kangaroo | Arms, ears, feet, tail | Tall detailed ears, long forward feet and long tail |
| Koala | Arms, ears, feet, tail | Round inner ears, paws and small tail |
| Lion | Arms, ears, forehead, feet, tail | Layered mane, ears, paws and tufted tail |
| Mole | Arms, feet, tail | Broad digging claws, feet and small tail |
| Swallow | Feet, tail, wings | Long toes, forked tail and swept wing feathers |

There are 37 new animal slot sets across 20 layouts. The renderer observes `allowsAnimalParts()`, so optional rare-form permission works without suppressing every immortal/character form unconditionally.

## Synced behaviors

The gameplay pose layer covers every `ChaoEntity.Behavior`: greeting, nuzzling, cheering, crying, cowering, tantrums, dizziness, grogginess, all six illness symptoms, talking, gossip, hugs, holding hands, shaking trees, building sandcastles, four learned dances/exercise, lessons, fleeing, bullying/intervening, break dancing, drawing, singing and all 11 learned instruments. Symptom gestures differ: cough covers the mouth and bends forward; sneeze has a windup and forward pulse; rash scratches; hiccups pulse the torso; stomach ache hunches with hands close; runny nose wipes the face. Six authored mouth expressions reinforce the poses.

Actual item actions take priority over behavior gestures. Learned music shows a transient client-only item prop while the real hand is empty; it creates no dropped item or inventory copy. Mounted toys use straddled horse, reclining coffin or floating lifesaver poses. New personality idle cues use independent sliders. Voluntary RELAX uses a quiet seated posture with a small head tilt. Omochao construction visibly assembles its body, head, arms/key and then feet in four stages. Entry/exit blending includes the complete authored gesture so petting and social poses settle smoothly.

Birthday hosts and party guests have separate gentle swaying/clapping poses. A birthday host with free head slots wears a temporary stepped paper party hat fitted to its skull; actual purchased headwear takes priority and is never removed or changed. The cake is the real held item supplied by the behavior/item pipeline. The systems-study flag also runs the additive pose probes.

## Opt-in monster development

The server's optional monster flag enables the core's 2,000/3,000-point stages. The existing primary and secondary silhouettes remain recognizable, while authored ridge/head/torso amplification grows beyond ordinary development. Balanced Normal monsters gain an explicit stepped crown. Accessories scale with their semantic anchors. Regular colored non-Normal monsters use the core's restrained stage opacity (0.78/0.58); the optional transparency fix restores opacity. These opacity values and cubic amplification are Minecraft adaptations, not claims of exact game mesh/opacity reproduction. Raw comparison ignores monster state. The runtime probe covers both stages of all five abilities.

## Verification

`art/validate_forms_019.py` independently checks all endpoint signatures, fixed semantic anchors, topology, finite vertices, bilinear continuity, positive cuboids, UV bounds, connected ridge segments, character geometry and all animal slot fits. Every new mesh directory also receives duplicate-name, parent-existence, cycle and three-frame independent world-transform checks; malformed regression fixtures verify those checks actually reject invalid graphs. The first runtime pass caught the Dark donor right-foot alias on Spartoi; the generator now resolves `feet_bone_right` explicitly, and legacy overlay aliases inherit its motion. `LocomotionVisualSmokePoseProbe.inspectForms019()` adds runtime checks for all 135 forms, exact reversible morphs, every synced gesture on all 15 primary rigs and six character rig selections. Its log is separate from the unchanged prior motion probe.

The earlier independent native validator still passes **6,486 checks** with maximum corner error 3.55e-15, and heritage validation still passes **6,379 checks**. The earlier idle contact diagnostic also passes its skeleton contract; its intentionally documented inherited source and connector contacts remain a limited diagnostic, not a claim of universal clipping freedom. New authoring checks complement actual Minecraft capture review; they do not establish perfect clearance under every possible mod accessory combination.
