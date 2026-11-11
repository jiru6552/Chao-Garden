# Toy surface and inventory polish — 0.25.51

These changes refine existing assets. They do not add duplicate toys, change recipes or re-author source geometry.

## Shadow doll face reference and correction

The primary reference is the supplied **Sonic Adventure 2 Battle `al_toy.prs` texture `sh_doll01`**, decoded by the project's existing source extractor. It contains Shadow's black/red forehead, slanted white eyes with red pupils, tan muzzle and mouth. The exact decoded 32×32 tile is also the [clamped face material](../src/main/resources/assets/chao_garden/textures/block/active02549/chao_shadow_doll/sa2_soni03_52.png). Archive hashes and the decoded RGBA hash are recorded in `models/active02549/manifest.json` inside the assets.

The existing doll remains the disclosed Sonic-doll geometry adaptation. No independently extracted Shadow doll mesh is claimed. This update corrects the texture mapping rather than inventing a new silhouette:

- The original donor material `soni03_148` spans source U=0..2 with mirrored repeat. Its atlas must contain two 32px tiles, with the second reflected horizontally. The previous Shadow substitution stretched a single face across the whole 64px atlas. That misplaced the eye/muzzle samples and lost the mirrored pair. The corrected atlas contains the exact official tile followed by its U mirror.
- Four original donor nose triangles sample constant source UV (0.5, 0.5), borrowing black from Sonic's pupil. That same location is red in Shadow's pupil. Only those four triangles now sample the black corner pixel of the same official Shadow tile. The nose is black again, without painting over the original face texture.

All Shadow vertices, normals, source triangles, hand-contact landmarks and assembly orientation are unchanged. Only the repeated face atlas and the four constant nose UVs change. The same correction applies to held, placed and inventory presentations. Other Shadow cloth/palette textures retain the existing black/red/white adaptation.

The developer inspected before/after software renders against the decoded primary texture. Those previews establish the UV/material correction; actual Minecraft rendering and observations are recorded separately in the release report.

## Inventory views

Picture book, box of crayons and broom now use `neoforge:separate_transforms`. Only the GUI perspective selects a source-generated flat sprite. The top/cover face is turned toward the viewer before rendering; a small in-plane tilt preserves recognizable silhouettes. These are projections of the existing textured models, rather than replacement art.

The non-GUI `base` is the exact previous item-model payload, including first/third-person, ground and fixed display transforms. Block models, all quiet-toy hand meshes, materials, textures and temporary drawing geometry remain unchanged. The book's cover, the multicolored crayon box and the broom brush/shaft are visible across their inventory sprites instead of showing thin floor-model edges.

## Smaller toy car

The existing resting car is uniformly 70% of its old size, scaled around block centre X/Z and the floor plane Y=0. Its front orientation and floor contact remain unchanged. Its new source bounds, in model pixels, are:

```text
[5.824094833, 0, 4.634718862, 10.175905167, 3.36, 11.365281138]
```

The canonical held source mesh is byte-identical. The runtime interaction uses a corresponding 0.42 scale instead of 0.60; the new one-handed motion and collision envelope are handled by the production pose/rules code.

## Checks and regeneration

`scripts/test_toy_polish_assets_02551.py` independently checks the primary decoded texture, exact reflected atlas halves, the four black-nose triangle samples, unchanged donor geometry, uniform car scaling/grounding in all orientations, inventory silhouette coverage and exact preserved non-GUI item payloads. A manifest protects 63 unchanged held/floor/material assets by SHA-256. Existing source-surface tests continue to compare quiet hand meshes against the original decoded archive.

Bounded regeneration commands:

```sh
python3 scripts/generate_active_toys_02549.py --only=chao_toy_car,chao_shadow_doll
python3 scripts/generate_quiet_toys_02549.py --models-only
python3 scripts/test_toy_polish_assets_02551.py
```

The bounded active generator preserves unrelated materials and skips audio generation. The quiet command skips audio too. Runtime motion, actual client observations and remaining multiplayer/resource-pack limitations belong to the main release report; asset tests alone do not certify animation or visual acceptance.
