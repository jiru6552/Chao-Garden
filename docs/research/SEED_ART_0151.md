# Seed art refinement — 0.15.1

The sixteen seed item IDs retain their existing planting and recipe behavior. Fourteen belong to active SA2 fruit families; Smart and Stamina remain legacy-save IDs. This change replaces the old shared brown bean and color patch with authored native 16px seed clusters. Two separated husked kernels form the common crop motif; the mushroom crops use three smaller grains. Each has a pale split seam, a restrained fruit-related coat and a silhouette derived from its parent fruit: round blue, squared green, triangular gold, pear-shaped cyan, notched Heart, curved Dark and the garden crop profiles. There are no fruit stalks or leaves on the seed items. Mushroom propagation remains the mod's existing orchard adaptation.

The following primary mod textures were downloaded and visually inspected on 2026-10-30. They are style references only; no source pixels, downloaded sprites or third-party textures are embedded in the mod.

| Primary source | Observed design |
| --- | --- |
| [Farmer's Delight tomato seeds, 1.21](https://raw.githubusercontent.com/vectorwing/FarmersDelight/1.21/src/main/resources/assets/farmersdelight/textures/item/tomato_seeds.png) | Several small orange kernels, bright sides and gaps between grains. |
| [Farmer's Delight cabbage seeds, 1.21](https://raw.githubusercontent.com/vectorwing/FarmersDelight/1.21/src/main/resources/assets/farmersdelight/textures/item/cabbage_seeds.png) | Small neutral kernels with clear negative space and minimal shading. |
| [Croptopia blueberry seed, v4](https://raw.githubusercontent.com/ExcessiveAmountsOfZombies/Croptopia/v4/common/src/main/resources/assets/croptopia/textures/item/blueberry_seed.png) | Loose navy/blue kernels carry crop identity through color. |
| [Croptopia bellpepper seed, v4](https://raw.githubusercontent.com/ExcessiveAmountsOfZombies/Croptopia/v4/common/src/main/resources/assets/croptopia/textures/item/bellpepper_seed.png) | The same compact grain vocabulary with olive/yellow-green color. |
| [Thermal Cultivation coffee seeds, 1.20.x](https://raw.githubusercontent.com/CoFH/ThermalCultivation/1.20.x/src/main/resources/assets/thermal/textures/item/coffee_seeds.png) | Two larger beans with dark husks and a pale seam. |
| [Thermal Cultivation peanut seeds, 1.20.x](https://raw.githubusercontent.com/CoFH/ThermalCultivation/1.20.x/src/main/resources/assets/thermal/textures/item/peanut_seeds.png) | Scattered brown grains with pale/olive upper surfaces. |

The result uses Thermal's readable larger-kernel idea and the separated-cluster convention shared with Farmer's Delight and Croptopia. The crop-shaped husks are this project's own visual choice. This is a visual reference comparison, not a claim that all reference mod versions are compatible with this mod's NeoForge 1.21.1 target.

Rebuild through `scripts/generate_fruit_assets.py`; the seed drawing lives in `art/sa2_seed_art.py`. `scripts/validate_seed_sprites.py` checks all sixteen native images, binary alpha, small palettes, distinct outputs and detached kernels with no isolated stray pixels. It does not establish that an artistic silhouette is recognizable; native and enlarged review plus actual Minecraft inventory captures provide that check.
