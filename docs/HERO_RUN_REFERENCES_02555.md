# Blocky Hero Run .55: reference evidence and limits

This pass supports the approved blocky baby → Hero Run adult → Hero/Run/Run path. Original mod construction takes priority over smooth reference geometry. The Chao Island viewer guides silhouettes, proportions, feature placement, age/alignment differences and color regions; the older addon supplies clean cuboid construction. Neither supplies an automatic source-to-mod morph map.

## Original mod and older addon

The style reference is the active ordinary baby in **0.25.51, commit `e6d0797b68d40ba55c9b5a214feb715a02b42212`**, immediately before the prototype releases. Its `gameplay/chao_baby.json` is byte-identical to the earliest available .43 model and the retained original asset:

```text
SHA-256 ae3d4da88b84905f8726b863bdb072e3a26eb404f38e565d0781b92b01e2cf9b
head 7×6×6; body 4×4×4; hands 2×2×2; feet 2×2×4 model pixels
```

The supplied construction references were inspected directly:

- [`art/imported_chao/models/chao_run.geo.json`](../art/imported_chao/models/chao_run.geo.json)
- [`art/imported_chao/textures/run.png`](../art/imported_chao/textures/run.png)
- [`models/entity/official/chao_run.json`](../src/main/resources/assets/chao_garden/models/entity/official/chao_run.json)

The old Run crest comprises **six rotated rectangular cuboids in three stepped branches**, with larger roots and smaller angled tips. Its base head is also 7×6×6. Its reach is too long/tall to paste unchanged onto this baby endpoint, so the approved adaptation shortens those parts. The two old side elements are zero-thickness planes; the requested front nubs were authored as separate closed solid boxes. Original baby eye panels stay untouched because cube numbering differs between the two rigs. The old green Run texture is a construction/pixel reference, not a universal Hero palette or proof of a complete Hero Run lineage.

## Actual independent viewer operation

The [Chao Island 3D viewer](https://chao-island.com/viewer/) rendered a real Chao in Chromium **156.0.8078.79**, using WebGL2 through ANGLE/SwiftShader software rendering. No compatibility warning or Continue prompt appeared in the fresh .55 sessions, and the context was not lost. Shared-desktop CUA controls were unavailable, so the inspection used headless Chromium/Playwright with the supplied proxy and TLS verification retained.

Actual mouse input unchecked Child, selected the **green Run first-type** control, changed alignment, age, Run and Swim/Fly, rotated the model and used `R` to reset the view. The fresh adult pass saved **35 original PNGs: 28 main and 7 color-center captures**, including two initial child frames. The age-quarter series covers approximate 0/25/50/75/100% front/side/back views with selected above/below views. A separate session checked fresh centered/max Run and mature centered Run colors. Earlier independently operated baby views and the user's initial photographs support the approved baby endpoints and distinct front nubs.

Observed distinctions:

- Developed Neutral Run children have swept rear features and two small front-side nubs above/outside the eyes. Young high-Run children can remain similar to their normal young form; age and Run are separate inputs.
- Hero children use short paired buds at normal influence and a low central stem with Run development. They do not simply acquire the Neutral child's larger rear branches/nubs.
- Fresh Hero Run adults have one short rear-leaning stem and a yellow halo. Fresh centered and maximum Run were visually similar in these controls.
- Mature centered Run gives a longer single arch and lime halo. Mature Hero/Run/Run gives a three-way fan above/rear of the head: one central rearward branch and two lateral branches sweeping outward/back. Its halo is yellow, with cyan/mint accents and a yellow-rim/blue-center belly.
- Changing Swim/Fly while retaining the Run first type, maturity and high Run visibly changes compatible shape/color influence. The focused mod pass preserves independent contributions without claiming every cross-specialization has been fully modeled.

The mod deliberately keeps its fixed head box and expresses recognizable features through a small number of attached cuboids. The smooth skull, sculpted curves and dense source bands are not copied. Viewer track positions and camera directions are approximate; hidden numeric values, exact FOV, original drive counts and wall-clock timing were not exposed. The viewer does not verify the mod's named colors, monocolor/jewel/shiny finishes, attachment behavior, permissions, persistence or multiplayer.

## Pages and original game-file audit

These supplied pages returned HTTP 200 and were retained for this pass:

- [First-evolution Run forms](https://chao-island.com/gallery/chao-types/run.html)
- [Hero adult specializations](https://chao-island.com/gallery/advanced-evolution/hero.html)
- [Age and appearance](https://chao-island.com/info-center/advanced/age)

The [Evolution wiki](https://chao-island.com/wiki/Evolution) and [Alignment wiki](https://chao-island.com/wiki/Alignment) returned HTTP 403 to the fresh fetches. They are linked as user-supplied references, not claimed as successfully read in this pass. The working viewer and other evidence allowed work to continue without more user screenshots.

The supplied-file audit was rerun with both source hashes verified:

| Input | SHA-256 |
| --- | --- |
| `art/sa2_02514/original/ChaoMain.prs` | `d296ebbb14a8386e86eba3035b9fb35c6e1add0e563ef233ef67c8830be17dc8` |
| `art/sa2_02515/original/al_body.prs` | `ab4932311f7600654b91dbacba5bc8320048af469c44fb120722c42c387d380c` |

Relevant families include Hero child/child Run, newly evolved Hero Run and aged Hero Run/Normal/Run. The adult side feelers are near embedded in the young source and expand into the mature lateral petals. This corroborates developing parts around a shared head rather than importing different source heads. Child/adult source topology and UV correspondence cannot be assumed from equal overall model totals; the mod uses deliberately authored canonical cuboids and semantic part fitting instead.

Source growth constants verified by the bounded audit are approximately `TimeAddGrowthChild=0.0166666675` and `TimeAddGrowthAdult=0.00416666688`, an adult/child ratio of **0.25**. Absolute callback cadence and wall-clock hours are not established. Source fruit growth also differs from the mod's retained completed-meal rule, so the mod's adult `+0.005` must not be called an original-game constant. The source archive does not establish a complete verified RGB palette; final pixel colors are authored targets guided by visible regions.

The current **480/960 net Run-minus-Power progress thresholds** and linear age×training visibility remain explicitly provisional Minecraft policy. The user-requested highest four earned-stat/recency chooser and locked adult identity take priority over original-game hidden-influence rules. No new clock rebalance is inferred from the viewer. Reproduce the bounded original-file read with `python3 scripts/audit_hero_run_sources_02552.py`; it does not change a world or runtime model.

The release reference manifest retains original screenshot hashes, actions, verification and research records. This reference evidence is distinct from actual .55 Minecraft/client acceptance and final runtime/JAR comparisons, recorded in [the implementation report](HERO_RUN_BLOCKY_02555.md).
