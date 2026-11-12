# Chao Island viewer — Neutral foundation .56

Fresh live research on 2026-11-12. The page returned HTTP200 and the 3D Chao actually rendered in Chromium156. Initial sandbox Chromium failed `ERR_CERT_AUTHORITY_INVALID`; the same verified-TLS browser in the outer executor used the existing CA trust and succeeded. TLS verification was not disabled. No desktop CUA tool was available, so Playwright operated the real Unity canvas using mouse clicks/drags and the viewer camera-reset key. No compatibility warning appeared in this successful run.

## Controls and scope

The **Child** checkbox was unchecked and **Normal** first evolution selected for these nine Neutral/Normal specializations. Neutral alignment was centered. Run/Power and Swim/Fly are opposed sliders; compatible pairs are the two independent endpoints together. Age0,25,50,75,100% positions were set on the visible age track. Coordinates are approximate visible-control positions, not an extracted authoritative game value. Each saved original PNG includes the complete canvas and controls, with action timestamps/hashes in captures.json/actions.json. Orbit drags supply front, approximate side/rear, above and below views. No viewer geometry or morph implementation was copied.

## Actually observed mature endpoints

| Neutral / Normal specialization | Main silhouette observation |
|---|---|
| Neutral | One central upright/slightly swept crown; modest body/limb growth. |
| Run | One longer rear-swept upper crown; not the multiple rear spikes/front nubs of a Run first-type. |
| Run+Fly | Crown lower, blunter and swept back, close to a low hump. |
| Run+Swim | Crown longer and more sharply rear swept with a large green cap region. |
| Power | Taller/narrower upright curling crown, less rear-reaching than Run; no broad lateral horns. |
| Power+Fly | Crown becomes a short rolled/hooked stub over the upper/front region. |
| Power+Swim | Tall simple upright/slightly rearward pointed crown. |
| Fly | A single central green forward/down-curled hook over the top/front; not paired horns. |
| Swim | A long thin rear-swept curling crown. |

Above/below views support a **single central crown** for these Normal-first-type forms. No separate front nubs or multiple large rear head spikes were observed here. Their eyes, pink zigzag wings, turquoise/cyan body and green hand/foot/crown accents remain broadly consistent. Mature orb is yellow. Differences in hand/foot bulk are modest and should not justify exaggerated stretched limbs. The head itself changes in the smooth reference, but the mod deliberately keeps the approved original cuboid head unchanged and translates silhouette differences into added blocky crown pieces.

Young Normal-first-type adults at age0 look essentially identical across all nine visible stat endpoint settings: light cyan body, yellow/lime hand/foot/tip accents, pink wings, pale peach orb and a short upright tuft. This supports one shared fresh adult corner. The nine mature forms should emerge through age/training rather than appear fully formed immediately.

## Hero baby orb check

Fresh Hero child, centered training: blue orb. Old Hero child, centered Run: blue orb. Young Hero child, maximum Run: blue orb. Old Hero child, maximum Run: **pink orb**, uniformly shaded. Thus the .55 pink endpoint is consistent with the viewer; pink alone is not evidence of an unintended color. A fix should distinguish deliberate age/Run marker transition from texture/shine/coat leakage. Additional live quarter steps show old full-Run alignment changing green → mint/gray → warm gray → pink, and full-Hero old Run changing blue → blue/purple → purple-gray → dusty pink → pink. Transitional desaturated colors alone are also present in the reference. Hero children retain the paired buds/short stem behavior already studied. The viewer does not verify named coat or shiny variants.

## Retained assets

The old addon assets `art/imported_chao/models/chao_{run,power,fly,swim}.geo.json` were inspected as construction records: small collections of cuboids/rotated cuboids, not geometry to copy blindly. `old-official-asset-inspection.json` records source hashes and rotated-cuboid parameters. These are first-type models and are not direct references for all Normal-first-type specializations.

## Limits

Viewer colors/shininess are not controls here. Its stat/age sliders do not establish absolute Minecraft training thresholds or elapsed biological timing. Matching these proportions is an authored blocky interpretation. The observations above concern the actual visible canvas; they do not assert polygon correspondence or a copied morph algorithm.

## Intermediate observations

The age25/50/75 side views of all nine directions were actually reviewed. Their crowns are recognizable shortened/intermediate versions of the mature shapes, while the very young corners remain close to one shared fresh form. Fixed mature-age training25% views were reviewed separately from the age sweep: Fly influence can flatten/curl the crown early. The viewer therefore guides qualitative transitions; it does not establish a linear weight formula. The implementation may use a documented blocky interpolation approximation.

## Completed capture and review record

Both live browser invocations completed successfully with WebGL2/SwiftShader and `contextLost:false`. No compatibility warning or continue dialog appeared. The primary run saved159 original full-canvas PNGs; the second saved92, for **251 unchanged original screenshots**. The second independently varied training at fixed mature age25/50/75% along each nonzero direction and captured all nine mature opposite-side views. `research-manifest.json` and `/workspace/.setup/neutral056-reference-manifest.json` bind every original hash, actual action notes and session verification.

Actually visually reviewed: all nine mature forms from front/side/back/above/below/opposite side; all nine young front/side views; all nine age25/50/75 side views; all eight nonzero training25/50/75 side views; and the Hero child endpoint/alignment/Run quarter-step views. Extra intermediate front/back originals are available, but not every one was separately opened. The `inspection-*.png` sheets are navigation derivatives (uniform crop/resize/labels), not new renders and not included in the original count.

The fixed-age training50/75 views continue the same qualitative crest progression. Opposite-side mature views are consistent with the shared central crown interpretation. No new paired horns or Run-first-type nubs were discovered on the Normal-first-type adult path.
