# Native themed forms — 0.22

The 0.21 runtime selected Baby geometry whenever a theme was active. This also disabled gradual mesh development, and its theme PNGs used only Baby's UV layout. The 0.22 selection keeps the ordinary first-type family and age/secondary geometry for every themed adult.

`build_variants_022.py` produces the full nineteen-theme by eleven-native-layout product: 209 PNGs with a manifest binding each selected path to its SHA256. Existing nineteen Baby theme images stay byte-identical, including the sources used by inherited Omochao coats. The twenty supplied reference PNGs/models and their original expression texels remain untouched. Native atlases retain each source's alpha and protected eyes while recoloring skin and wings to the selected theme. Ordinary natural gradual color changes are bypassed for themes; explicit cosmetic palette effects remain supported.

The original iris-swatch selection could choose white skin rather than protected expression pigment. Genetic colored irises therefore inherited skin recoloring in five donors. `build_forms_019.samples` now selects the brightest neutral opaque expression texel outside the skin mask. Independent comparison confirms that only Hero, HeroFly, HeroPower, DarkRun and DarkSwim change their added colored-iris UV sample; the other fifteen supplied donor choices remain equal. No eye geometry or raw UV panel changes.

Themed root attachments now follow the current visible skin rather than only the untouched imported anchors:

* Forehead crowns and actual Create/Thaumcraft goggles use the current skull affine.
* Chest symbols and back assemblies use the current torso affine, so Power bodies do not swallow their emblems or leave packs floating away.
* Floating theme markers translate with crown height and skull center while retaining their size and authored five-pixel gap.
* Closed headwear and birthday hats suppress themed crowns through the same `headCovered` rule used by the model. Typed native features remain; Normal/Baby's redundant curl is replaced by the themed crown. Immortal Chaos anatomy keeps precedence over dormant theme metadata.

Animal bone slot/ID descriptors are parsed once when an overlay is loaded, rather than split and converted during every rendered frame.

## Verification scope

The independent Python validator checks every generated pixel against native alpha/expression regions and the declared theme palette, exact Baby bytes, all nineteen by fifteen by nine by three static type/secondary/age fits, actual forehead lens meshes, crown marker gaps, torso contacts and exposed symbol samples. A separately derived dominant source-skull texel must equal the exact selected theme head color. Its optional corruption checks reject altered eye pigment, alpha and theme hue. It does not claim moving-pose or third-party-modpack validation.

The opt-in `VariantsVisualSmokePoseProbe` uses the real resource manager and selected material for all 209 native atlases. It samples actual emitted genetic-iris UVs, verifies source eyes and alpha, and checks 4,275 production-fitting-helper poses plus 855 real cap cases. Independent JSON ancestor matrices include optional static rotations defaulting to zero, exactly as the model loader does. Required pivots and parent paths remain strict. The installed JAR excludes this audit probe.

Integrated generation, Java compilation, actual-client execution, mod-gate fixtures and final visual reviews are centralized by the release owner. Their final logs and current verification document supersede this source-preparation status.
