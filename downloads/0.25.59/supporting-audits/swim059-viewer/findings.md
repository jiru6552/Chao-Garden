# Live Chao Island viewer research for Neutral / Swim / ~ (.59)

This is visual reference research, not an in-game test or an import of Chao Island geometry. The actual **Swim first-type checkbox** is selected, with Child unchecked and Normal unselected. This is different from the earlier Neutral / Normal / Swim specialization.

## Rendering and interaction actually verified

The site's HTTPS page and game canvas load in installed Chromium 156.0.8078.79 with TLS verification enabled. The initial sandbox attempt failed with `net::ERR_CERT_AUTHORITY_INVALID`; the same existing supported outer Chromium workflow succeeds without disabling certificate verification. The browser uses ANGLE / Vulkan SwiftShader with WebGL 2.0, and its context reports not lost. No compatibility warning or Continue prompt appeared in this session. External font, advertising/analytics and Unity telemetry requests are blocked; some nonessential Unity shader warnings appear, but the Chao visibly renders and responds.

Actual mouse actions selected Swim first type, changed age, changed both opposing-stat sliders, changed alignment to Hero for one control proof, restored Neutral, and rotated the rendered Chao. Front, both sides, rear, above and below were viewed. The inherited drag160 'back' filenames are **rear oblique**; a drag200 trial also overshot slightly and is retained as an opposite rear-oblique diagnostic. Supplemental drag180 'square-back' files provide the approximately square back views. These are mouse orbits, not a mathematically calibrated camera. Original full canvases are preserved; contact sheets use only a uniform crop and resize of those screenshots.

Control percentages are approximate normalized positions on the viewer tracks, not claims about original-game stat levels or exact age timing. Alignment is centered Neutral for the requested family. Run/Power is one axis (positive Run on the left, negative Power on the right), and Swim/Fly is one axis (negative Swim on the left, positive Fly on the right). The first-type checkbox remains Swim when those later axes change. The viewer is not evidence for the mod's evolution selection, tie history, persistence or adult alignment-lock implementation.

## Forms compared

The six requested forms are balanced, Run, Power, Swim, Run+Swim and Power+Swim, all with Neutral alignment and Swim as first evolution type. Every form was captured at viewer age0%, 25%, 50%, 75% and100%; newly evolved/half/mature have matched front/side/rear views. All six mature forms additionally have opposite-side, above, below and rear-oblique captures. Training25%,50%,75% was sampled separately at fixed mature age. Positive Fly samples are context only, not additional requested prototype forms.

At age0 all six later-training endpoints visibly converge to the **same fresh Swim form and palette**. A newly evolved Swim form already has a broad crown/cap with a rear/down overhang, plus small yellow paired temple fins; it does not start from an ordinary Normal-adult single stem. Its rear point is approximately level with the lower face in side view. A shared newly evolved target and palette, with age-gated later differences, matches this observation.

| Later influence | Mature silhouette actually observed | Natural viewer color regions, visually estimated |
| --- | --- | --- |
| Balanced | Broad cap, pointed rear extension sloping down; small/medium fins above outer eyes point backward. Modest growth of cap/fins/body from fresh. | Lime crown/body; pale cream lower face and belly; yellow hands, feet and fins; cyan forehead V; blue/cyan wings and tail; pale lime orb. |
| Run | Longer rear cap projection becomes near horizontal or slightly raised; narrow upward/rearward temple fins, appearing as upturned horns from front; comparatively lean arms/body. | Bright green crown/body; cyan lower face, belly, fin bands and wrist accents; lavender/purple feet ends, wings and tail; cyan orb. |
| Power | Shorter/blunter rear cap, high broad top; short thick lateral temple fins; broader arms, hands and torso. | Lime crown/body; saturated yellow lower face, belly, mitts, side fins and tail; orange feet; pink/orange/lavender wings; bright yellow-lime orb; greener forehead V. |
| Swim | Long rear cap drops behind head toward shoulders; long temple flippers hang down/outward/rearward almost to wrist height. These are separate paired features, not a central top stem. | Lime crown/body; pale white/cyan lower face, belly, rear cap hem, fin and hand bands; yellow-green fins/hands, yellow toes; blue/cyan wings and tail; pale icy orb. |
| Run+Swim | Moderated mixed cap: longer than Power, less dropped than pure Swim. Medium hanging/outward fins end around chin/neck rather than wrists; narrower than Power+Swim. | Green crown/body; cyan lower face, belly, fin bands and orb; yellow-green mitts, yellow feet; muted blue wings/tail with olive edge. |
| Power+Swim | Broad rounded cap with intermediate rear/down projection; medium thick hanging temple fins, and somewhat stockier body. Not pure Swim's full-length flippers plus full Power hands. | Lime crown/body; pale cream-green lower face, belly, fin bands and orb; yellow hands/feet; blue-gray/olive wings, pale green tail. |

The 25/50/75 age side samples show gradual cap direction/length, fin direction/length, body proportion and palette development. In particular, Run's fresh rear/down tip gradually rotates toward the raised mature point, while Swim's fins and rear hem lengthen downward. Power's outer fins thicken and the foot/wing colors warm. Compatible combinations are moderated mixed shapes in the viewer, not a simple pile of both full extrema.

From above, the cap is broad at the forward portion and narrows toward one central rear point. The paired fins root on its upper outer sides and sweep along the cap edges. Both sides are symmetric. Wings remain below and behind the head, with a small separate tail lower on the back. There is no need to copy a dense mesh to preserve these cues.

## Blocky construction reference and implementation guidance

Read-only official construction references in the checkout:

- `art/imported_chao/models/chao_swim.geo.json` — SHA256 `7bf8fb338a655234971c0315c6bf96f6658dba94e611ed3a468df6af47ac542e`
- `art/imported_chao/textures/swim.png` — SHA256 `611c48b612efa85d657d24e09f8863536307bcdef4c330e0b132d27e9627b43a`
- `src/main/resources/assets/chao_garden/models/entity/official/chao_swim.json` — SHA256 `eee91f1358d37a19d42ddcb15554dfc7624a7ff645f0d8b11ab7f3edaadefa62`

That official asset uses a broad closed cuboid crest/cap with an angled rear cuboid and symmetric thin temple fins. Its altered base head and raised eye cubes are not compatible with the user's fixed original head requirement and should not be copied. Use only the simple cap/rear-piece/paired-fin construction idea. Preserve the original head, eye planes, pivots and rig; author a few separate closed cuboids for the broad cap, tapered rear suggestion and each fin, with coherent attachment overlap. Translate color bands with pixel UV regions rather than decorative extra geometry. The long pure-Swim fins and cap should stop clear of eyes, arms and wing roots throughout interpolation.

No viewer geometry, meshes, morph code or external model library was imported. These are authored Minecraft approximations. The viewer's smooth silhouette and gradients cannot be reproduced literally while retaining the approved fixed head and simple cuboid style. Palette names above are visual estimates, not extracted canonical RGB or texture data. The viewer does not demonstrate the mod's color/shiny variants, hats, animal parts, toy grips, player attachments, permission checks, networking or save/reload; those need separate mod tests.

## Final coverage and boundaries

The main live run completed113 original canvas captures and336 actual control actions, with no JavaScript page errors and a healthy WebGL context at completion. A separate corrected back-angle run adds18 requested-family back captures plus one initial control-provenance canvas. The final primary reference manifest therefore contains132 original canvases:123 for the requested family, seven explicitly labeled context/control proofs, and two initial Child provenance frames. All123 requested-family sources and seven context/control sources were personally inspected directly and/or through the uniform contact sheets. The two initial Child provenance frames are not counted as reviewed family views. The drag200 diagnostic run and initial three-view probe are retained locally for audit but are superseded in the final comparisons.

At fixed mature age, the five nonbalanced training paths were inspected at25%,50% and75%, plus their balanced and full endpoints. The two compatible diagonal paths visually appear near their mixed endpoint by approximately50% on both tracks; this supports normalization of compatible mixtures but is not evidence for an exact hidden formula. Positive Fly at50% and100% visibly changes the first-Swim form (raised rear cap and larger wings), while its first-type checkbox remains Swim. Because the UI exposes only one Swim/Fly slider, it cannot demonstrate separate simultaneous Swim and Fly stats, exact cancellation arithmetic, or the mod's correct stat-history behavior. One Hero-alignment control proof visibly changes to a white Hero Swim with a halo; that is not a request to expand this Neutral prototype.

The final fresh/half/mature front-side-back sheets use the visually checked drag180 approximately square back. Both-side, above/below, rear-oblique, age-progression and fixed-age-training sheets remain separately available. Static samples support a smooth authored transition but do not certify every unsampled frame, Minecraft collisions, accessory clipping or in-motion behavior.
