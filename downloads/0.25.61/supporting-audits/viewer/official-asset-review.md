# Official Neutral Fly project references inspected read-only

These project assets provide a block construction vocabulary only. They are **not fresh live viewer evidence**, and no geometry, texture, dependencies or source code was copied into production by this inspection. The original gameplay head, face, UV layout and attachment rig remain the constraint.

The imported Bedrock reference `art/imported_chao/models/chao_fly.geo.json` identifies `geometry.chao_fly` with logical64×32 UVs. Its skull is7×6×6, with two small2×2×1 inflated face/brow cubes added. Matching skull dimensions do not authorize replacing the gameplay skull or its eyes/brows.

The main distinctive head construction is a **paired rear/side crest**, one on each side of the skull. Each has a3×3×5 root cuboid and a2×2×4 terminal cuboid, with opposite rotations producing symmetric outward/rearward sweep. This supports paired horn/ear forms made from a few coherent closed cuboids around the unchanged gameplay head. It does not establish the mature viewer silhouettes or their age interpolation.

The body is a compact4×4×4 cube. Hands are2×2×2 cubes with short2×1×1 upper arms, and feet are2×2×4. Each wing is an8×5 **zero-thickness card**, rotated slightly backward; the tail combines a2×2×2 angled base with a zero-thickness4×5 side-plane tip. Those flat imported parts are unsuitable as watertight production examples; author closed production wings/tail with actual thickness and continuous attachment. The reference has one2×2×2 emotion-ball cube above the head and optional expression variants.

The source texture `art/imported_chao/textures/fly.png` is128×64RGBA although the model declares logical64×32 UVs. I opened the actual PNG and a nearest-neighbor enlargement saved as `official-fly-texture-inspection.png`. Visible palette: pale pink/lilac head/body, purple and dark blue limb/crest accent areas, green/cyan wing pixels and a pale yellow emotion-ball patch. The ball patch containsRGBA(255,251,124,255). These are asset-specific colors, not extracted viewer pigment values or a mature-form palette. Viewer captures must decide the correct evolution family colors.

The converted official model `src/main/resources/assets/chao_garden/models/entity/official/chao_fly.json` names `crest_right` and `crest_right2` as `head_decorations`. Its coordinate conversion is `x=-x,y=24-y,z=z; Blockbench-equivalent rotations=x,-y,-z; native Bedrock box-UV`. The deployed official texture has the same SHA256 as the imported texture. The older hat/reference `art/sa2_02515/previous-models/hats/chao_fly.json` was located and hashed; it was not treated as evolution evidence.

Exact file hashes and the enlargement hash are in `official-asset-review.json`. All referenced production/source assets were read only.
