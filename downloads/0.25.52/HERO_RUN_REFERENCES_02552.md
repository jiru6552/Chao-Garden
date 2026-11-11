# Hero Run prototype: references and supplied-source audit

This research supports one path: a Hero-aligned, Run-trained child, its first Hero Run adult form, and later Hero/Run/Run specialization. It distinguishes original-file evidence, earlier project research, and Minecraft policy. The prototype keeps the current block-built Chao style and semantic skeleton; the source measurements guide that adaptation rather than replacing the whole rig with a different game skeleton.

## Requested community references

The user supplied these Chao Island pages:

- [Run first-evolution forms](https://chao-island.com/gallery/chao-types/run.html)
- [Hero adult specializations](https://chao-island.com/gallery/advanced-evolution/hero.html)
- [Age and gradual appearance](https://chao-island.com/info-center/advanced/age)
- [Evolution](https://chao-island.com/wiki/Evolution)
- [Alignment](https://chao-island.com/wiki/Alignment)

Fresh HTTPS retrieval succeeded for the exact Run gallery, Hero advanced gallery and age pages, and the three relevant gallery photographs were opened and visually inspected. Their bytes match the earlier photo hashes below. [The retrieval audit](research/hero-run-02552-web-audit.json) records URLs, statuses and hashes.

The two exact wiki URLs still returned HTTP 403 with Cloudflare's `cf-mitigated: challenge` header and “Just a moment” HTML. A normal Chromium attempt did not retrieve their text; the environment's Chromium NSS trust setup also reported a certificate-authority error. No challenge bypass or certificate-ignore option was used. The site's linked public [Evolution Info Center](https://chao-island.com/info-center/life-cycle/evolution.html) and [Alignment Info Center](https://chao-island.com/info-center/life-cycle/alignment.html) pages were both successfully retrieved and read as the accessible counterparts. No further environment-network change was needed for those pages.

The existing [0.19 reference manifest](https://github.com/jiru6552/Chao-Garden/blob/codex/download-0.25.52/docs/research/forms019-reference-manifest.json) records a previous gallery study, including these specific photo inputs:

| Subject | Original photo URL | Recorded SHA-256 |
| --- | --- | --- |
| First Hero Run | `https://chao-island.com/assets/images/gallery/twotone/run/heroruntwotoneregular.jpg` | `ce3903ab49f9385fc02664b103e56307c807cbf134e8fce5f036e055e15b1114` |
| Fully aged Hero Run/Normal | `https://chao-island.com/assets/images/gallery/advanced-evolution/hero/runnormal.jpg` | `406a4d4f98c299fd64bcec7379706b131504e58a0a274ccdd4d7fc94247b48e4` |
| Fully aged Hero Run/Run | `https://chao-island.com/assets/images/gallery/advanced-evolution/hero/runrun.jpg` | `b44a55c1b45fd09fdc41183582db24dabe701d9265a3bdb0bba0cfece78bf8ef` |

All three photographs were freshly downloaded and viewed in this session; their exact matches with the earlier manifest connect this review to the prior gallery study. The earlier [gradual-evolution notes](https://github.com/jiru6552/Chao-Garden/blob/codex/download-0.25.52/docs/research/GRADUAL_EVOLUTION_021.md) and [model notes](https://github.com/jiru6552/Chao-Garden/blob/codex/download-0.25.52/docs/research/GRADUAL_MODELS_021.md) remain useful project history, but their assertions are not a substitute for this session's direct source audit.

## Fresh page and photograph findings

The age page explicitly describes a child's body and regular colour gradually shifting toward its favoured first evolution. It also describes adult change as slower, mutable with new animals/drives, and not a second cocoon at a fixed age. Its statement that adult appearance “has nothing to do with stats” matters: using the current visible bar difference for this prototype is an intentional **user-requested Minecraft override**, not an original-game claim.

The linked Evolution page likewise explicitly rejects “highest visible stat” and “Stamina means Normal” as explanations of the original game's evolution selection. It instead describes recent animal/drive influence. This prototype follows the user's four-stat highest-point/latest-raised rule and excludes Stamina from that contest; the update report documents the change from 0.25.51's five-stat comparison separately.

The Hero gallery describes Hero/Run/Run as having light blue markings with a head split into **three petal-like parts**. The photograph shows a white main body, blue hands/foot markings, and broad blue-tipped petals with visible gaps, corroborating the decoded source crown rather than a generic five-quill Run silhouette. The first Hero Run has a small straight spike; the linked Evolution page specifies blue/yellow hands, blue/purple stomach and a yellow halo. The aged Normal photo/text has a longer central spike and striped blue hands. These establish relative shapes and colour placement; exact Minecraft RGB choices remain authored rather than copied photo pixels.

The Alignment page describes a gradual Hero–Neutral–Dark spectrum during childhood. It identifies a Hero child's white body, sky-blue bud/hand/foot markings and yellow body markings, and notes that age changes that appearance. Adult identity locking in this prototype is the user's explicit rule; it is not inferred from a blocked wiki page.

## Original supplied files are available

Two original members of the user's previously supplied SA2B archive are present and were read without modification:

| Member | SHA-256 |
| --- | --- |
| `art/sa2_02514/original/ChaoMain.prs` | `d296ebbb14a8386e86eba3035b9fb35c6e1add0e563ef233ef67c8830be17dc8` |
| `art/sa2_02515/original/al_body.prs` | `ab4932311f7600654b91dbacba5bc8320048af469c44fb120722c42c387d380c` |

`ChaoMain.prs` expands to 4,402,824 bytes, SHA-256 `ebbef2b6473f2ec32abc92ce5ded0640f975abb882b6b2f008fc3b86c8977be4`. Its self-relocations resolve a 144-pointer root-object table at decompressed offset `0x3a4cd0`. The table contains six source shapes per type. Source type 15 is Hero Run, making table entries 78–83 its young, aged Normal, Swim, Fly, Run and Power shapes. Type 3 supplies the Hero-child branch.

The type/group interpretation is corroborated by the public [CWE type enumeration](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/SA2Enums.h), [six-model registration](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/api/api_customchao.cpp), and [40-part enumeration](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/al_draw.h). CWE is a community reverse-engineering/modding source, not a claim that its optional game changes are vanilla rules. The actual positions and vertices below come from the supplied original PRS bytes, not CWE replacement models.

| Source shape | Table entry | Relocated root offset |
| --- | ---: | --- |
| Neutral child base | 0 | `0x73d20` |
| Hero child base | 6 | `0x906e8` |
| Hero child Run endpoint | 10 | `0xa3858` |
| Newly evolved Hero Run | 78 | `0x1ac1a4` |
| Fully aged Hero Run/Normal | 79 | `0x1b0d60` |
| Fully aged Hero Run/Run | 82 | `0x1bf124` |

Each inspected source shape contains 40 semantic nodes and 720 decoded triangles. [The audit JSON](research/hero-run-02552-source-audit.json) records each part's parent, pivot and bind-pose bounds. Matching node/triangle counts are useful evidence of the original deformation system; they do not make the game's rounded mesh a drop-in replacement for the mod's different cuboid mesh, UVs, genetic overlays or wardrobe anchors.

The offline audit uses the Ninja transform order corroborated by [SA Tools `ProcessTransforms`](https://github.com/X-Hax/sa_tools/blob/58e6cd1ee3f1f275ccbe288f46944fec28d3883b/Libraries/SAModel.Direct3D/Extensions.cs) and [matrix functions](https://github.com/X-Hax/sa_tools/blob/58e6cd1ee3f1f275ccbe288f46944fec28d3883b/Libraries/SAModel.Direct3D/MatrixFunctions.cs). This audit supplies its own transform rather than altering or re-running the historical toy exporters. Bilateral source parts then agree to the small precision differences in the original mesh.

## Verified Hero Run shape findings

Offline front and side projections of the decoded source triangles show these differences:

- The first Hero Run adult has a narrow central crown/stem leaning toward the rear. The paired feeler meshes are still tiny.
- The fully aged Hero Run/Run has a three-part crown: a tall central curved stem and two broad side feelers that spread outward and bend rearward/downward. A pointed rear head profile also extends farther back. It is not the Neutral Run family's five swept Sonic-like quills.
- The aged Normal secondary develops a longer, rearward central curl while its side feelers remain small. This distinguishes biological/adult maturation from the later Run-specific crown.
- Hero child alignment changes its head/feeler, tail and wing presentation. In the inspected source child endpoints, the body and leg meshes keep the same bind-pose extents. A Minecraft prototype that visibly develops the body toward its first-adult proportions during childhood intentionally follows the user's rule beyond that particular source-child geometry.

Selected source bind-pose measurements, in original model units:

| Part measure | Newly evolved Hero Run | Aged Hero Run/Run | Ratio |
| --- | ---: | ---: | ---: |
| Main head width | 2.5598 | 2.5598 | 1.000 |
| Head depth, including rear crest/profile | 2.9309 | 3.8163 | 1.302 |
| Head height, including central crest | 2.9107 | 3.4759 | 1.194 |
| One foot's width | 0.8837 | 1.2343 | 1.397 |
| One foot's depth | 1.6493 | 1.9432 | 1.178 |
| Body width | 1.4773 | 1.4773 | 1.000 |
| Body height | 1.6212 | 1.5312 | 0.944 |
| Body depth | 1.7078 | 1.6178 | 0.947 |

The source leg height remains about 1.0543 and the neck attachment rises. These are shape/pose measurements, not growth rates, collision-box dimensions or a requirement to scale every Minecraft part by the exact same ratio. The adaptation can retain its source eyes and meaningful attachment frames while matching the recognizable crown, rear silhouette and foot development.

The body textures in `al_body.prs` use CI4 indexed colour. The selected supplied archive members do not include a complete verified original palette set. The offline source sheets therefore use clearly labeled neutral body shading plus decodable facial textures; they are geometry references, not original-game screenshots or proof of final in-game RGB colours. The freshly viewed gallery photos separately establish white/blue/yellow/purple colour placement, while the mod's precise natural-palette RGB values remain authored. Genetic, jewel and accessory appearance remains a separate integration concern.

## Verified constants and timing limits

A contiguous original constant table at decompressed offset `0x310700` matches the corroborated [CWE `CHAO_GLOBAL` layout](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/al_behavior/al_intention.h), including movement values and neighbouring limits. The audit reads the following values directly from the supplied bytes:

| Field | Value (approximately) | Decompressed offset |
| --- | ---: | --- |
| Ordinary deformation limit | 1.0 | `0x310740` |
| Growth limit | 1.2 | `0x310744` |
| First-cocoon growth point | 1.0 | `0x310748` |
| Normal/Neutral boundary values | 0.5 / 0.5 | `0x31074c` / `0x310750` |
| Child time-growth increment | 1/60 | `0x310758` |
| Adult time-growth increment | 1/240 | `0x31075c` |
| Child fruit-growth increment | 0.005 | `0x3107ac` |
| Adult fruit-growth increment | 0.00125 | `0x3107b0` |
| Child animal-form increment | 0.1 | `0x3107d0` |
| Adult animal-form increment | 0.05 | `0x3107d4` |

The original values support an adult time-growth increment one quarter of the child's, independent signed form influences, and a 1.2 growth cap. The [CWE capture implementation](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/al_behavior/albhv_capture.cpp) applies half the form increment to drives and the full increment to animal acquisition, with Run and Power on opposite signs of the same horizontal influence. Combined with the supplied constants this corroborates the existing mod's ordinary Run-drive steps of 0.05 for a child and 0.025 for an adult: 20 or 40 pure accepted drives from a balanced influence respectively. Retained earlier training changes the remaining number; multi-stat training and explicit stat reversals need the prototype's documented Minecraft policy.

The exact original callback frequency, the complete game clock control flow and a universal visible-stat-points-to-specialization formula were **not** reverse-engineered in this audit. Constant field names alone do not establish wall-clock hours. No newly chosen points/rate should be described as the original game's exact rule.

For comparison, 0.25.51 already advances adult visual maturity by `loadedTicks / (4 * configuredChildDuration)`, with a 1.2 cap and +0.005 per successfully eaten whole fruit. At the default three-hour childhood this is 14.4 loaded hours from adult strength 0 to 1.2 with no fruit; offline and cocoon time do not count. Its whole-fruit increments, 0.02 child and 0.005 adult, equal four of the original table's per-increment fruit values. This relationship supports the adaptation, but the mod's absolute Minecraft duration and any new Hero Run specialization mapping remain explicitly provisional policy. The main prototype report describes the final implemented mapping separately from biological age.

## Explicit provisional 0.25.52 mapping

The implemented `HeroRunMorphology052` keeps the biological clock above and samples a separate training coordinate from the **current** visible bar totals. For each ability, `barTotal = clamp(level * 100 + progress, 0, 9900)`. Hero Run influence is based on the signed difference `RunBarTotal - PowerBarTotal`; it does not erase or reuse biological age.

- Child Run development reaches its endpoint at a net 480 progress units. Hero appearance reaches its endpoint at alignment +0.5; both coordinates vary continuously.
- Adult Hero Run specialization reaches its training endpoint at a net 960 progress units. The ordinary drive's 24 accepted progress units make that 40 pure Run drives from equal Run/Power away from caps; existing net training changes how many are still needed. The child threshold similarly corresponds to 20 drives.
- Adult visible Run development combines this reversible training coordinate with normalized biological maturity (`evolutionStrength / 1.2`). A decrease or opposing Power increase can reduce specialization while leaving the Chao's maturity and calendar untouched. Equal additions on both sides of the opposing pair cancel in the difference; Combined Drives and Analyzer edits therefore have the same final-state interpretation as other accepted stat changes.

At the default twelve-hour adult lifespan, time alone contributes strength 1.0, or about 83.3% of the 1.2 visual-maturity cap. Forty successfully eaten whole adult fruits add the remaining 0.2; they can be spread through adulthood. This is a condition for the complete endpoint under the existing default clock, not a hidden claim that forty drives alone instantly produce a fully mature body. Configured durations and the existing equipped-adult visual clock can change the available maturation time.

The thresholds **480/960 progress units are provisional Minecraft policy**, calibrated to the original-file-supported 20/40 drive influence relationship. The original source audit does not establish a universal formula based on `level * 100 + progress`. In particular, this prototype intentionally makes explicit stat decreases/editing affect appearance, rather than preserving an independent hidden direction indefinitely. Its scope is ordinary Chao on this Hero Run path; the other adult specialization paths are not redefined by this helper.

## Existing assets and reuse

The pre-prototype Hero Run gameplay asset was an authored adaptation of the owner-supplied `run` cuboid rig, with generated crown targets and palette. It was not an independently imported original Hero Run model. The inspected authoring paths are `art/build_forms_019.py` and `art/build_gradual_evolution_021.py`; raw comparison rigs remain separate. The newly decoded original Hero Run source provides better endpoint guidance for this focused prototype without adding an animation/morph dependency.

Reproduce the bounded original-file audit with:

```sh
python3 scripts/audit_hero_run_sources_02552.py
```

It checks both original member hashes, decompresses/relocates only in memory, reads six relevant source shapes and the constant table, and writes only the audit JSON. It changes no Minecraft save, toy export, gameplay model or runtime dependency. Actual prototype client captures and motion, save/evolution tests, integration observations and remaining visual limitations belong to the main update report.
