# .58 refinement verification plan

Read-only baseline review and test-source work by tests058. The root coordinator alone runs Gradle, native tests, Minecraft and final packaging. No test result below is claimed for .58 before those actual invocations.

## Source invariants and focused additions

- Original baby `skull`/expression records, fixed7×6×6 head vertices/UVs, reaction joints and all original59 joint pivots/rotations must remain exact in both families. Existing emitted head/eye comparisons stay unchanged across the dense age/axis lattice.
- Neutral retains three positive closed crown cuboids. Protect the latest user red-line silhouette directions using actual emitted source-corner endpoints: Swim's lower section leans far rear/up and its short upper hook turns upright/slightly forward; Power's broad lower section leans forward/up and its short upper section curls rear/up. The earlier long-horizontal-cap assertions were superseded by that explicit steering. Power+Swim retains the wider lower top narrowing upward. Validate connected roots/caps, positive volume/winding, exact texture coordinates and unchanged other corner targets via the generator's independent Python validator.
- Hero retains two roots and two upper caps. Add emitted-geometry distinction checks for balanced, Run, Power, Fly and Swim tips, with separate root/cap identity and UV semantics. Keep the strict actual tilted-halo/head-feature SAT separation checks.
- Only Hero wings are intentionally changed from two-sided planes to two thin positive closed boxes. The canonical baby back-wing island is fully transparent, so this family deliberately mirrors its back faces into the existing opaque front island; source image alpha and every other family UV remain unchanged. Check actual emitted six-face winding/orthogonality/positive volume through the dense target grid, original visible front UV island, intentionally remapped formerly transparent back UVs within that same front island, original wing parents/pivots and left/right mirror symmetry. The authored9-wing targets now deliberately have distinct span/height pairs; check those actual emitted distinctions alongside the viewer-supported long/slender Run versus compact/broad Power, taller Fly and extended Swim families. A physical root test derives the old x=0 edge center from original vertices, transforms actual emitted proximal corners into the wing joint frame and bounds their mean drift below .001 model pixel. Actual wing/torso OBB overlap is also checked across the dense grid and real motions.
- Existing continuous100-step axis sweeps, twenty-tick large-edit/reversal, save/reload shape identity and color/shiny geometric identity remain active; do not widen their limits merely to accept a refit.
- Existing motion tests cover567 real Lionfish frames per family (all27 age/axis states × seven motion modes × three ticks), toy palm finiteness and6,804 separate overlay/animal/hat/accessory anchor matrices. Preserve those exact limits and include wing-anchor matrices.
- Existing Piano/Organ tests already enumerate all27 production056 plus27 production057 configurations, alongside prior055; no source change is needed to make them sample the refined assets. Keep contact/reach/clearance thresholds unchanged.
- Material058 owns `HeroNormalMaterialTest057.java`: face/expression/source alpha, named/mono/jewel/shiny colors and independent halo pigment remain mandatory; new root/cap/wing contrast checks should use actual regions and reference-observed hue families.

## Root-run command scope

Use the existing Java21 wrapper and `--no-configuration-cache` (inherited fixture task accesses Task.project):

```sh
python3 /workspace/.setup/gradle-cloud.py --no-configuration-cache coreTest neutralMorphRigTest056 neutralMaterialTest056 heroNormalMorphRigTest057 heroNormalMaterialTest057 blockyRunMorphRigTest055 heroRunMaterialTest055 pianoRigTest organRigTest
python3 /workspace/.setup/gradle-cloud.py --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_morph52,chao_baby53,chao_morph54,chao_morph55,chao_edit56,chao_neutral56,chao_heronormal57 -PchaoGameTestDir=/path/to/private058world
```

The retained305-case native suite already covers first-evolution highest earned-point/recency ties, no Stamina selection, ordinary/combined drives, current-axis reductions/commutative compatible pairs, independent maturity, locked adult alignment/first type, named/nearest command permission/selection, real Creative maturity handler and forged/Survival rejection, NBT and client sync. It also explicitly distinguishes the synthetic supported expanded-care21 Hero/Normal record from the genuine older Neutral fixture. Reusing these tests is preferable to adding copies of unchanged policy tests. No real user world should be altered.

Run the two family Python validators after their generators; the root should retain literal successful outcomes. Build/JAR checks and the broader `--continue build` follow only after focused checks. Any failure must be classified from the current result, not predeclared inherited by its task name.

## Required actual-client comparison

At fresh/half/mature: Neutral Swim, Power+Swim and Power; Hero all nine mature forms and at least balanced/Run/Power/Fly/Swim intermediate states. Capture front/left/right/back, side/back wing evidence and above views where needed to see top direction. Include representative full-color/shiny/accessory cases, head/face/halo clearance, walking and large-stat increase/decrease. Inspect actual production poses rather than a second pose implementation. Representative broom/book or keyboard use and real equipped back attachment should exercise changing wings/head additions where available. Keep phone touch, audio, remote multiplayer/shaders/fullmodpacks and unperformed activities explicit.

## Inherited failures, not .58 results

`morph058-tests/starting-baselines.json` binds actual starting result/source hashes against delivered `.57` commit6632fd1. Current inherited triangle result:25 failures /267,696, minimum hand/head pixels−0.54432297, SHA25699bce5827d20d9d3aba51fef07728eb609dda4bc14e2c0534e903aacba50b6f8. Current trumpet:37 /267,696, minimum−0.58569264, SHAb64266310e866db7a3628192f974bcd9abf1eabfd4b36b797de53dfc37fa0bfa. Only call the .58 failures inherited after rerunning and comparing the complete current result files. The separate journal is previously8/9 on `Your gardens` paragraph formatting; its test and GardenGuideItem content currently match delivered `.57` exactly. No broad native toy-suite success is inferred from this focused suite.

## Source freeze

Only `HeroNormalMorphRigTest057.java` and `NeutralMorphRigTest056.java` were edited by this agent. Existing keyboard tests already select the revised production assets, so they were left untouched. Hero material tests remain material058 ownership. Source files were frozen before the root first coordinated compile; `git diff --check` passed. This is not a compiler/runtime success claim.

## Diagnosed coordinate-frame correction

The first coordinated rig run compiled/core passed but the new world-space wing orthogonality assumption failed. A root-only diagnostic rerun identified actual fresh Run+Swim WALK tick0: body scales1,1.0022913,1 and wing world-basis Y/Z normalized dot−0.00019947. Existing nonuniform breathing plus flap rotations legitimately makes world parallelepipeds. Strict .00005 local box rigidity is now checked after the inverse actual wingjoint transform (normals transformed by the corresponding A-transpose); world positive volume, planarity, winding/normals, opposite edges, physical hinge and torso overlap remain checked. SAT cross axes now use actual edges instead of treating face normals as edge directions. No emitter or production geometry fix was needed. The exact matrix and diagnosis are retained in `morph058-tests/wing-affine-diagnostic.json`; rerun success remains the root coordinator's responsibility.

## Actual-client edge-alpha correction

The root preflight observed a hollow outline at a Run wing cutout: constant opaque side samples drew a border where the original visible front image is transparent. The generator now maps side UV vertices to the corresponding front UV corner with a small inside inset. The rig test enforces per-vertex front correspondence within .126 UVunit and original visible-island bounds, rather than accepting any opaque side texel. Material058 owns actual per-coat cutout tests. No wing positions, head, joint pivots or hinge tests changed. The preflight remains separate from the eventual final accepted capture cohort.
