# Live Chao Island viewer research — Dark / Normal first type / ~ (0.25.60)

This is visual reference research from a fresh live viewer session, not an import of its geometry or proof of Minecraft behavior. In the viewer, **Normal** is selected as the first type, **Child is unselected**, and alignment is full Dark. This is distinct from Dark/Run, Dark/Power, Dark/Fly or Dark/Swim first types.

## Browser and actual interaction

The environment reports the selected managed executor running with network policy enforced and Chao Island allowed. Shared-desktop capability is enabled, state unknown, but no desktop JavaScript/CUA tool is offered to this agent. The existing installed Chromium/Playwright workflow was therefore used. A fresh sandbox attempt failed before page load with `net::ERR_CERT_AUTHORITY_INVALID`. The same script succeeded outside the sandbox with the environment's existing CA trust; **TLS verification remains enabled** (no ignoreHTTPS flag). Installed Chromium reports version156.0.8078.79. HTTPS returned200, and one actual Unity/WebGL canvas renders a visible Chao.

Actual mouse actions select the Normal checkbox, change Dark/Neutral/Hero alignment, change age, set both opposing-stat sliders, and orbit the Chao. R resets the orbit. The requested family is captured at full Dark. No compatibility dialog or Continue prompt has appeared in the successful session. External font/advertising/telemetry failures and shader warnings, if present in the raw logs, do not substitute for render verification. `verification.json` records actual WebGL context status after the session completes.

Control percentages are approximate normalized positions on viewer tracks, not exact original-game stat levels or timing. Viewer Run/Power is positive Run on the left and negative Power on the right. Viewer Swim/Fly is negative Swim on the left and positive Fly on the right. The mod uses the opposite sign for its Run/Power numeric axis. First-type Normal remains selected as later influences change. A viewer alignment slider does not establish how the mod locks adult identity or authorizes edits.

## Views and forms

All nine requested mature forms have been directly inspected from front, side facing right, approximately square back, opposite side, above and below. Mouse delta+100 produces the side facing right and+180 produces an approximately square back, visually checked. These are mouse-driven approximate orbits, not calibrated angular degrees. Original970x485 canvas screenshots are preserved. Contact sheets apply one fixed crop, uniform scaling and padding only; they do not flip images, change colors or invent geometry.

A supplementary early session inspected fresh balanced, Run, Power, Fly and Swim forms. At age0 they visibly converge: dark maroon body/head, vivid red extremities/chest crescent/rear head tip, lavender/periwinkle spiked marker, purple bat wings. The fresh head already has a **short rearward and downward hood/crest, ending in a small upturned rear tip**. It is not the Normal/Neutral adult's ordinary upright stem. The primary session also captures all nine forms at age0,50,100 with front/side/back, age25/75 side views, and training25/50/75 at fixed mature age. The completed primary session contains153 original canvases: 150 requested family views and 3 context/control proofs. All 15 contact sheets were actually opened and inspected, covering all 153 originals; selected full originals were also opened. The session recorded468 mouse/key actions, zero JavaScript page errors, WebGL 2 through ANGLE/Vulkan SwiftShader with its context not lost, and no compatibility prompt.

## Mature forms actually observed

The major changing feature is **one central rear head extension**, with its root on the upper rear of the head. These Dark/Normal forms do not have the Neutral/Run first form's three stacked rear spikes or the Neutral/Swim first form's paired long temple fins. A few coherent closed cuboids can describe the changing hook/hood direction around the unchanged original Minecraft head box.

| Later specialization | Head silhouette, side facing right | Natural viewer colors, visually estimated |
| --- | --- | --- |
| Balanced | Rear extension slopes backward/down toward upper-back level; small terminal tip points further rearward. | Dark plum body/head; magenta hands/feet/chest crescent and crest tip; purple wings and spiked marker. |
| Run | Long rear extension rises above upper skull; short terminal tip turns rearward almost horizontally. | Dark purple/plum body; saturated violet/magenta extremities and crest; cyan/blue bat wings; yellow spiked marker. |
| Power | Broader upright crest rises from upper rear skull, ending in a short backward bent tip; front shows a tall central cone-like peak. | Warm dark brown body; orange extremities, chest crescent and crest tip; yellow/orange wings with olive inner webs; golden marker. |
| Fly | Rear crest rises with an angular hook: backward section then upturned terminal tip. Tail is visibly longer. | Dark gray/slate body; blue cuffs grading toward pale pink extremities, lavender crescent; pink crest tip/marker; orange/yellow wings with olive webs. |
| Swim | Longer rear hood droops down strongly, ending in a short backward point at around mid-head height. | Warm dark brown body; gray cuffs grading to peach/orange extremities and crest tip; peach crescent; vivid purple/magenta wings and purple marker. |
| Run+Fly | Single raised rear crest with a backward/upward terminal point; intermediate between Run's horizontal bend and Fly's hooked tip. | Dark plum/slate body; violet/pink extremities/crescent; purple-blue crest with pink tip; olive-green wing webs/purple rims; peach/tan marker. |
| Run+Swim | Short near-horizontal rear extension at upper rear head, tiny terminal point; upward and downward tendencies moderate each other. | Dark plum body; pink extremities/crescent/tip, purple-blue crest root/wings; dusty rose marker. |
| Power+Fly | Single straight-ish tapered crest sweeps upward/backward, without the full pure Fly hook. | Dark desaturated brown/slate body; blue cuffs into salmon/pink extremities, crescent and tip; orange/yellow wings with olive webs; peach/salmon marker. |
| Power+Swim | Short rearward crest with modest downward sweep, much less droopy than pure Swim. | Warm dark brown body; olive cuffs into orange extremities/crescent/tip; pink/violet wings; rose/magenta marker. |

Hands and feet remain compact; Power is somewhat stockier rather than dramatically oversized. All forms have triangular bat wings with a notched/scalloped lower silhouette. Back views expose the wing web color clearly. Wings change accent palette more strongly than their basic anatomy in this family. A thin rear tail has a small pointed tip, generally matching the extremity accent, with Fly extending its length. The marker is a **spiked ball**, not a Hero halo or smooth neutral orb, and its intended color changes independently of genetic body color.

The viewer's small dark brow contours and slanted-eye expression are reference anatomy only. Preserve the mod's original working face/eyes, pixel texture layout and attachment rig, as requested. Do not resize or round its original head to reproduce the viewer's skull.

At fixed mature age, the individual Run/Power/Fly/Swim 25/50/75% controls show gradual head direction and palette changes. The **compatible paired controls reach a nearly unchanged combined endpoint by about50% on each slider**: the actual full canvas UI confirms the thumbs are at their half-axis positions, while50/75/100% look very similar. The mod's bilinear mature grid can provide a clear smooth first pass, but its interior mixture/rate must be labeled an approximation rather than an exact reproduction of the viewer's blending. This observation does not justify copying the viewer's code or changing game evolution rules.

## Project construction references inspected read-only

- `art/imported_chao/models/chao_dark.geo.json` — SHA256 `15b73a6aa1ff798ead9edb47fa863ce553ebe22d9af8ea726493d77d4365391a`
- `art/imported_chao/textures/dark.png` — SHA256 `c98cbb17c956eb3d40f269d3934fdaff1241d1aeafe6a5a830f4340a46597d2a`
- `src/main/resources/assets/chao_garden/models/entity/official/chao_dark.json` — SHA256 `0fbd76dd136f98780af031f033440cd4a734bf3d2610880fca00d5669082ae5b`

The imported official Dark model uses a larger7x7x6 skull, extra brow cubes, simple closed limb cubes, flat wings and a pointed tail. Its skull dimensions and face/brow replacement must not be copied into the fixed7x6x6 gameplay head. Its small angled cuboid features can guide construction vocabulary alongside the already approved blocky models. The viewer was not used to copy vertices, morph logic, textures or code.

## Limits

Viewer colors are visual estimates from rendered images, not extracted original-game pigment values. Genetic colors and shininess are not available in this viewer. Viewer age and slider positions do not establish mod biological timing, bar units, evolution selection/tie recency, save compatibility or permissions; those require project tests. Actual in-game comparison and motion verification are performed by the coordinator from the production model, separately from these reference captures.
