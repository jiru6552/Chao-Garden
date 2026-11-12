# Hero Run .54: viewer and supplied-file evidence

This reference pass supports one path: ordinary baby development toward Hero Run, the newly evolved Hero Run adult, and mature Hero/Run/Run specialization. The viewer supplies inspectable shapes and color-region placement. The supplied original game files provide independent anatomical and correspondence evidence. Neither establishes that the viewer's geometry or hidden morph logic can be copied directly into the mod.

## Actual live viewer access

Source: [Chao Island 3D Chao Viewer](https://chao-island.com/viewer/), a Unity program credited by the site to Justin113D. The live 3D Chao loaded in headless Chromium 156 with WebGL 2.0 through ANGLE/SwiftShader software rendering. Actual mouse input changed the Run/Power, age and alignment sliders, and click-drag rotated the model; `R` restored the front view. This was more than a successful HTML request.

The default browser's proxy-certificate trust initially failed. The supplied environment CA was trusted and the browser ran in the execution context where that trust store applied; TLS certificate verification remained enabled. Optional analytics/fonts requests were blocked without preventing the viewer from rendering or responding. Shared-desktop controls were unavailable, so this was an automated browser inspection, not a claim of using that panel.

The completed pass saved **121 actual viewer PNGs**: normal and developing Hero/Neutral children, a new Hero Run adult, mature Hero Run at intermediate Run settings, independent intermediate visual ages, and maximum Hero/Run/Run. Front, left, right, back, above and below views were used. The [retained viewer audit](research/hero-run-02554-viewer-audit.json) records original screenshot hashes, displayed controls and capture metadata; the delivery also includes the original reference PNGs and provenance manifest. The PNGs are copied unchanged as reference evidence; resized galleries are separately identified.

Basic sliders expose positions rather than verified numeric values. Quarter positions are approximate visual settings. Mouse rotations similarly provide approximate directions, not calibrated angles or the Minecraft camera's exact projection. The viewer's `young`/`old` control was used to inspect a visual progression; it does not measure biological age, drive counts or wall-clock growth. Its current color/shiny controls were not used to validate inherited mod coats. The project requirement is that inherited colors and finish share the same morphology and detail placement.

## What was visibly useful

- A Hero child at balanced Run has two rounded buds, a blue orb, yellow belly and blue hand/foot caps. Toward Run, its buds recede into a low central-stem/tiny-bud form; the orb becomes pink, belly pale blue and distal caps mint green. Proximal limbs and the body remain white. It does not acquire adult stripes or the Neutral child's long swept quills.
- The maximum Neutral child shown by Child=true retains a cyan body and belly, green orb/quill/limb accents and gold wings. The earlier supplied green Neutral Run photograph shows a first-evolution form; it is a different reference stage.
- The newly evolved Hero Run adult has a faceted, rounded/tapered skull, narrow central stem, compact palms, broad shaped feet and a gold halo. Its belly is a small violet region within blue and white torso surroundings.
- With adult development and Run influence, the skull gains an integrated rear point. The crown becomes **three thick connected petals** that fan backward and bend down. Above and back views are especially useful for distinguishing those petals from thin stepped bars.
- Hands, feet and the crown carry wrapped blue/yellow/white bands in the young form; the fully developed form shifts the band family toward blue/cyan/teal. The mature belly has a blue center and yellow rim, and the wings shift toward teal/dark blue. These are relative region/color observations, not a verified original RGB palette.
- Below views expose broad solid foot pads and wrapped markings. In the old mod baseline, the main problem was flat slab-like jaw/sole shape and missing wrapped detail; no literal open mesh hole was identified in the reviewed adult stills.

The [before-edit comparison](research/hero-run-02554-before-edits.md) records how these observations differed from actual 0.25.53 Minecraft views before production target editing. The baseline used the delivered production classes/assets on a development classpath and saved all 240 original six-angle PNGs. This is separate from the new model's later client acceptance.

## Fresh audit of the supplied game files

The original members were available and freshly read without modification:

| File | SHA-256 |
| --- | --- |
| `art/sa2_02514/original/ChaoMain.prs` | `d296ebbb14a8386e86eba3035b9fb35c6e1add0e563ef233ef67c8830be17dc8` |
| `art/sa2_02515/original/al_body.prs` | `ab4932311f7600654b91dbacba5bc8320048af469c44fb120722c42c387d380c` |

The [fresh source audit](research/hero-run-02554-source-audit.json) matches the previous checked audit. The relocated table contains 144 entries, six shapes per type. Relevant entries are Neutral child 0, Hero child 6, Hero child Run 10, newly evolved Hero Run 78, aged Hero Run/Normal 79 and aged Hero Run/Run 82. The type/part interpretation is corroborated by the community CWE sources cited in the [0.25.52 research](HERO_RUN_REFERENCES_02552.md); numerical positions come from the supplied original bytes.

[Anatomical landmarks](research/hero-run-02554-source-landmarks.json) support these new-adult → maximum Hero/Run/Run trends:

| Measure | Endpoint ratio |
| --- | ---: |
| Main head width | 1.000 |
| Head depth including rear profile | 1.302 |
| Head height including central stem | 1.194 |
| One foot width | 1.397 |
| One foot depth | 1.178 |
| Body width | 1.000 |
| Body height/depth | 0.944 / 0.947 |

The source has a central curved crown stem and two broad lateral feelers bending outward, rearward and down. Child Hero targets instead alter head/feeler, tail and wing presentation while their body, arm and leg local positions remain unchanged. Small whole-body changes during childhood are therefore an authored Minecraft adaptation to the requested prototype, not a measured original-game child scaling rule.

The [indexed correspondence audit](research/hero-run-02554-source-correspondence.json) checks point indices, triangle/edge graphs and UV/material bindings instead of assuming equal totals imply compatibility. Hero child → Hero child Run and Hero Run young → aged Normal/Run have verified correspondence within each source family, despite restripped face order. Child → first adult does **not**: body vertices change 32→30, legs 22→25, head 79→81 and wings 15→12, with other graph and UV differences. The common 40-node/720-triangle totals hide those changes. No source-to-mod-cuboid index mapping follows from this evidence.

Ninja coordinates use Y up and front +Z; they need deliberate conversion and semantic fitting to Minecraft joints. The old source sheets also render multiple conditional facial layers together and intentionally use neutral body shading. Their overlapping expressions and gray bodies are not game screenshots. The supplied CI4 body archive does not establish a complete verified original palette; viewer views and the earlier gallery photographs guide color placement, while final RGB values remain authored.

## Rules and timing boundaries

The source constants support a 1.2 growth cap, an adult growth increment one quarter of the child's, and the previously documented 20/40 ordinary-drive influence relationship. They do not establish exact callback frequency, universal visible-stat formulas or original wall-clock hours. Reproduce the bounded audit with `python3 scripts/audit_hero_run_sources_02552.py`; it reads the original files and writes audit data, without changing a world or runtime model.

The new common .54 topology is authored from semantic rings on the existing Minecraft rig; it imports neither the viewer's mesh nor the decoded original vertex arrays. The source ratios and observed contours guide that adaptation. Existing facial inset and joint semantics take precedence where a game mesh cannot fit the mod's eyes, expressions or attachments directly.

Named, mono, jewel and shiny details were checked against the mod's own source atlases and material rules, not extrapolated as observations of the viewer. Their common UV masks retain readable rings/insets: named coats use inherited base/accent, while mono/equal-hue jewel coats use value differences without changing hue. The family orb/halo remains a development marker. Exact RGB values and inherited-color adaptation are authored policy.

The prototype retains its existing provisional **480/960 net Run-minus-Power bar-progress thresholds**, independent biological maturity, four-ability earned-point/latest-increase first evolution and locked adult alignment. Those are project rules. The viewer's type buttons and visual sliders are comparison tools, not a substitute for the user's rules or a test of server authority, persistence, inheritance, multiplayer or attachments. Final implementation and actual Minecraft verification are recorded in [the update report](HERO_RUN_VIEWER_02554.md).
