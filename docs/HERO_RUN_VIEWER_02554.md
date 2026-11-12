# Viewer-guided Hero Run path — 0.25.54

This focused update continues the ordinary baby → Hero Run adult → Hero/Run/Run prototype. It uses the working Chao Island viewer and supplied game files to refine the recognizable shapes and their progression within the existing Minecraft renderer. Run development, alignment and biological maturity remain separate inputs. Use matching 0.25.54 client/server JARs with Java 21, Minecraft 1.21.1, NeoForge 21.1.219 and external Lionfish API 3.1.

## What the comparison established first

The live [Chao Island viewer](https://chao-island.com/viewer/) rendered a Chao in Chromium using software WebGL 2.0. Actual mouse controls changed Run, age and alignment, rotated the model, and reset it to the front. The completed reference pass saved **121 actual viewer PNGs**, covering normal and developing Neutral/Hero children, new Hero Run adults, intermediate Run/age settings and fully developed Hero/Run/Run from the front, both sides, back, above and below. The [viewer audit](research/hero-run-02554-viewer-audit.json) records those controls, image hashes and limits. [Reference notes](HERO_RUN_REFERENCES_02554.md) separate those observations from original-file measurements and project policy.

Before production morphology edits, an actual Minecraft client completed **20 controlled baseline scenes and 240 original PNGs**, covering six views per state. The development classpath's **8,249 prior production class and asset entries** were compared byte for byte with the delivered 0.25.53 JAR and frozen. That JAR is a comparison reference; the client ran a development classpath, not a claimed loaded copy of the JAR. The [before-edit audit](research/hero-run-02554-before-edits.md) and [dated proof](research/hero-run-02554-before-edits-proof.json) record the findings before target authoring began.

| Feature | Largest difference in 0.25.53 | Reference direction for this update |
| --- | --- | --- |
| Hero baby head | Five long Neutral-style quills on a mostly rectangular head | Hero child central stem and small lateral features, with a distinct developing cranial outline |
| Adult head | Square shell with a separate rear wedge | Faceted, tapered shell with an integrated rear point |
| Mature crown | Thin, stepped strips | Three thick connected petals spreading backward and down |
| Hands and feet | Angular layers, plank-like feet and flat dark soles | Compact closed palms and broader tapered pads with coherent underside surfaces |
| Color regions | Entire torso front violet, sparse limb markings | Inset belly region, white surround, wrapped crown/hand/foot bands and developing wing colors |
| Attachments and finish | Some inherited coats bypassed region UVs; hidden decorations could affect hat fitting | One sampled shape and consistent region placement across coats, shine, parts and attachments |

The earlier five-quill Hero baby was a deliberate interpretation of the user's Neutral Run photograph. This request to follow the viewer's Hero path revises that interpretation. It does not change the intended alignment to Neutral: alignment-specific baby anatomy can differ while Run and alignment remain independent continuous influences.

## Implementation scope

The .54 path uses one deliberately authored canonical mesh with seven compatible endpoints: Neutral baby, Hero baby, Neutral Run baby, Hero Run baby, new Hero Run adult, mature Hero Run/Normal and mature Hero/Run/Run. Four independent Hero × Run baby corners blend continuously; adult weights combine the new-adult, mature-Normal and mature-Run targets through the existing maturity and specialization sample. This prevents unrelated donor vertex indices from becoming an accidental morph map.

The [generator](../scripts/generate_hero_run_morph_02554.py) writes `chao_hero_run_proto054.json` and its `hero_run_morphology_054.json` manifest. The moderate closed facets form a tapered cranial shell with an integrated rear point, compact palms, broad shaped feet and three thick curved adult crown branches. Existing eye/expression panels keep their registered facial inset. Face normals are recomputed from the blended surface, correcting the old cube-normal lighting on sloped targets. The old .52/.53 source assets remain protected and their historical asset tests are separate from the new production-path tests.

All skin targets, sampled bind joints, palms and wardrobe bounds use the same rig. Animal overlays and attachments copy those sampled transforms. Closed hats hide the added head decorations; hidden extra branches are excluded from fit bounds. A faceted child orb and adult ring halo now have stage-appropriate geometry, while heart/question/dizzy/emphasis symbols retain their existing transforms. The ordinary baby/adult size and identity change stays inside the existing first cocoon; shared topology does not claim to remove that gameplay transition.

The existing indexed-face loader and named attachment frames support this work without a new library. No viewer runtime mesh or original source mesh was imported into the new asset. Verified source landmarks guide its authored contours instead.

### Regions, inherited colors and shine

`HeroRunCoatMaterial054` and `HeroRunRegionPaint054` give every coat the same belly, hand, foot, crown and sole UV regions, with shared body/limb/head/marker/tail swatches. Original eye/wing UVs and alpha outside the explicitly added atlas regions are protected; animal/flame overlay atlases stay separate. The existing shine reflection pass uses the same geometry and is unchanged. Dynamic textures retain a bounded 512-entry cache and end-frame retirement.

For natural Hero children, zero Run has two rounded head buds, a blue orb, yellow belly and blue distal hand/foot caps. Toward maximum Run, the buds recede into the low central-stem/tiny-bud form; the orb becomes pink, the belly pale blue and the caps mint green. The body and proximal limbs remain white, and babies do not acquire adult stripes. The viewer's maximum Neutral **child** instead has a cyan body/belly, green quill and limb accents, green orb and gold wings. The earlier green Neutral Run photograph is a first-evolution form, not that Child=true endpoint.

New natural Hero Run adults have blue/gold/white wrapped bands and a blue belly arch with a violet center inside the white torso. Visible adult Run development moves the bands toward blue/teal, the belly toward a gold rim and blue center, and the wings from gold toward teal. These RGB values and simplified patterns are authored approximations of observed regions.

Named two-tone coats keep their inherited base/accent colors in the same masks. Monocolor and jewel coats keep their hue while light/dark value differences preserve readable bands and insets; they do not become natural white/blue or lose their region detail. Family markers still follow development rather than inheriting body hue. This is an explicit material policy, not a claim that those variants were shown by the viewer. Eyes, expressions and animal textures retain their existing meaning.

## Preserved rules and saves

First evolution still selects the highest displayed **earned ability points** among Swim, Fly, Run and Power, with the most recently actually raised tied ability winning. Stamina does not compete. This is distinct from comparing level numbers. Combined Drives remain simultaneous transactions: increased stats retain their prior relative recency, rather than the loop's last member becoming an invented winner. The existing qualified Chaos route and already saved first-evolution identities are preserved.

Appearance continues to use the current level/progress bars:

```text
bar total = clamp(level × 100 + current progress, 0, 9900)
net Run = Run bar total − Power bar total
baby Run target = clamp(net Run / 480, 0, 1)
adult Run target = clamp(net Run / 960, 0, 1)
visible adult Run development = adult Run target × biological maturity
```

The 480/960 thresholds remain **provisional Minecraft policy**, not values measured from the viewer's sliders. They correspond to twenty/forty net ordinary 24-progress drive units from balanced Run/Power before caps. Stat decreases, opposing Power training and Creative stat edits can reverse specialization without making the Chao biologically younger. Swim/Fly opposition and the other adult paths are not overhauled.

Adult alignment stays locked at first evolution. The existing Analyzer **Mind → Profile** bar remains editable for an available living ordinary baby in Creative, read-only in Survival, and `LOCKED` for adults. Server authority, held-Analyzer and care/range checks remain in effect. A Hero adult cannot be turned Dark by the control or a forged request.

No new persistent fields or save-format version are added. Appearance derives from existing saved identity, training, alignment and maturity. Latest .53 regression inputs are accepted client states with NBT round-trip evidence; they are not described as a copied user's disk world. No user's world was modified for this work.

## Comparisons and verification

The controlled state matrix separates Run from age. Baby series use fixed alignment and age with 0/25/50/75/100% Run. Adult series vary Run at fixed full maturity, then vary maturity at fixed maximum Run. A real freshly evolved Hero Run with Run total 480 has training target 50% but maturity zero; it is not the same input as a fully mature adult with no net Run training.

Static six-angle Minecraft fixtures are suspended temporarily so the camera can see actual undersides without floor occlusion. They do not demonstrate a new hovering behavior. Motion, toys and carrying use the ordinary supported world positions. Viewer rotations and slider positions are approximate visual controls; they are not exact camera/FOV matches or an original-game time simulation.

The completed focused checks currently establish:

| Check | Observed result |
| --- | --- |
| Core mechanics and alignment | Passed, including 171,316 ChaoState checks, 964 morphology-policy checks, 785 alignment-rule checks and 1,835 bar input/layout checks. |
| Production .54 mesh/rig | 17,363,420 checks passed across 1,990 emitted geometry/motion frames, 4,040 closed compact limb samples and 464 coat/shape cases. Maximum 1% vertex step: 0.058279872 model pixels. |
| Final material source | 30,076,664 checks passed across 172 source coats, 11,878,320 original pixels, 10,685,520 exact protected pixels and 2,211,920 semantic pattern samples, including inherited hue/detail guards. |
| Retained .52/.53 asset contracts | Both historical rig suites passed; these protect retained assets rather than proving the new production model's appearance. |

**FINAL_RESULTS_PENDING**

The final client cohort being evaluated contains 45 scenes: the matched 20-stage comparison, actual Creative/Survival/adult Analyzer controls, gains and decreases while walking, expressions, independent adult maturity, a first cocoon, baby/adult broom and book grips, equipped ground/flight poses, and representative named two-tone, jewel, shiny and monocolor/hat cases. Those planned checks are not counted as passed until their actual run is recorded.

The prior release's broad-build baseline contains triangle and trumpet transition failures: 25 and 37 failed samples respectively out of 267,696. The standalone journal suite previously passed 8/9 with the `Your gardens` paragraph-format assertion. Current .54 results must be compared with their saved outputs before any failure is classified as inherited. The final report and download evidence must retain failed checks as well as passes.

## Recheck in your game

1. Compare normal, intermediate and maximum Run babies from the front, both sides and back. Check the Hero child's stem/buds, the newly evolved adult and the mature three-part crown; inspect the rear point and soles from above/below too.
2. In Creative, drag Mind → Profile alignment through Neutral and Hero on the same Run-trained baby, then save/reload. Run progress should remain intact. Confirm Survival is read-only and an adult's bar remains locked.
3. Compare natural, named two-tone, monocolor, jewel and shiny Chao at the same state. Look for consistent head/limb shapes and region placement, intact eyes, and no lost patches or distorted accessory textures.
4. Walk, turn and use your preferred hats, animal parts, glasses, toys and equipped poses. Existing operator previews include `/chao testtoy broom once`, `/chao testtoy picture_book loop` and `/chao testtoy stop`; they use production animations without rewards. `/chao inspect` reports the prototype inputs, while `/chao mature` is a first-evolution shortcut, not an adult-specialization setter.

The cloud setup uses a silent audio backend. Shaders, resource packs, a full modpack, remote-player views, network latency and an actual user's existing world require separate verification. The source changes ZIP is a cumulative overlay onto the full 0.25.43 project with prior updates and explicit source-deletion instructions; the delivery branch contains the complete updated source. Its deletion helper edits source files only, never Minecraft saves.
