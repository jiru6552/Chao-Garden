# Blocky Hero Run morphology — 0.25.55

This update expands the approved baby head features into the focused ordinary baby → Hero Run adult → Hero/Run/Run path. It retains the mod's original Minecraft construction: a fixed cuboid head, compact limbs, the existing pixel face and shared Lionfish animation/attachment rig. Use matching .55 client/server JARs with Java 21, Minecraft 1.21.1, NeoForge 21.1.219 and external Lionfish API 3.1. Accessories/owo remains optional for adult equipped companions.

The rounded/faceted .54 draft was paused before publication. Its retained research and diagnostic code do not define this release's model or establish current verification. The .55 shape follows the user-approved blocky endpoint review.

## Shape system

The style reference is the active original baby model from **0.25.51, commit `e6d0797`**. Its bytes match the earliest available .43 model and the retained `gameplay/chao_baby.json`. The head is **7×6×6**, body 4×4×4, hands 2×2×2 and feet 2×2×4 model pixels.

The entire original head/eye-panel records, facial UVs and shared pivots stay fixed across the new path. Base-head vertices bypass legacy and prototype deformation, including the optional old monster head-size boost. New anatomy is built around that head. Baby body/limb proportions remain exact; adult growth uses small semantic cuboid adjustments, with soles anchored to the support plane. The ordinary child/adult world scales remain 0.64/0.80, changing at first evolution inside the existing cocoon. No unrelated donor vertex indices are used as a morph map.

[The generator](https://github.com/jiru6552/Chao-Garden/blob/codex/download-0.25.55/scripts/generate_blocky_run_morph_02555.py) authors `gameplay/chao_hero_run_proto055.json` and its `hero_run055.json` hash manifest. Seven corners share one rig: normal Neutral baby, normal Hero baby, developed Neutral Run baby, developed Hero Run baby, newly evolved Hero Run, mature Hero Run at centered training, and mature Hero/Run/Run. Added parts share corresponding center, positive size and Euler-angle targets. The renderer blends those parameters and evaluates rigid boxes; it does not shear a cuboid by blending unrelated rotated vertices. The original normal tuft retracts inside the unchanged head while new features emerge from embedded roots. Parts keep nonzero volume throughout; there is no visible model/visibility swap.

Neutral Run uses the older addon's stepped cuboid construction, shortened for baby proportions: larger roots with smaller angled tips, plus **two separate front nubs above/outside the eyes**. These nubs are distinct from the rear branches. Hero children retain the viewer's paired buds/short central stem. Newly evolved Hero Run adults have a short stem; mature centered training lengthens a single arch, while developed Hero/Run/Run grows a three-part fan above/rear of the head. The smooth viewer's changing skull is deliberately translated into additions around the fixed Minecraft box.

The existing renderer and Lionfish integration support this without a new dependency or general morphology API. Rendering, surface queries and attachment samplers use the same evaluated geometry. Recomputed normals follow the actual cuboid faces. Closed hats and acquired head parts retain the existing decoration-hiding rule; eyes, expressions, palms and equipment joints keep their semantic anchors.

## Training, identity and timing

Ordinary first evolution still compares the greatest **earned ability-point total** among Swim, Fly, Run and Power. This is distinct from comparing level numbers. The most recently actually raised tied ability wins; Stamina is excluded. A simultaneous combined-drive transaction preserves the prior relative recency of its increased abilities rather than inventing a last member from loop order. The qualified Chaos route and already saved first-evolution identities remain intact.

Appearance reads current level/progress values through one read-only policy:

```text
bar total = clamp(level × 100 + progress, 0, 9900)
net Run = Run bar total − Power bar total
raw baby Run target = clamp(net Run / 480, 0, 1)
visible baby Run = raw baby Run target × child maturity
adult Run target = clamp(net Run / 960, 0, 1)
visible adult specialization = adult Run target × adult maturity
```

Child maturity is evolution strength clamped to 0–1; adult maturity is strength/1.2 clamped to 0–1. Baby Hero influence is independent and reaches its endpoint at alignment +0.5. Shapes and colors use the same age-conditioned visible baby value, so a highly trained young baby can remain close to its normal young form. Existing client interpolation smooths larger edits and reversals over 20 ticks from the currently displayed state.

The **480/960** thresholds and linear age×training visibility are provisional Minecraft policy, not hidden values measured from the viewer. They correspond to twenty/forty net ordinary 24-progress drive equivalents from balanced Run/Power, before caps. Baby training contributes toward the later adult target. Ordinary and combined drives, decreases, animals, activity/food training and Creative Analyzer edits all derive appearance from the same current state. Reducing Run or raising opposing Power can recede specialization without reducing age, resetting biological maturity or changing the adult's first identity.

Swim/Fly remains an independent opposing axis, compatible with Run/Power. The original baby lattice had no distinct body-axis targets, which a new renderer test exposed. The .55 asset restores meaningful adult hand/foot/wing contributions using **semantic cuboid center/size deltas** from the older Hero Run profiles, mapped onto the canonical part's own corners. Runtime deltas are bounded to 0.25 model pixels × maturity and exclude the head, face, marker and added head features. Compatible-axis accent/wing colors retain a small existing-profile contribution. This preserves independent influences; it does not author every mixed specialization or change other adult first-form paths.

Adult alignment and first type remain locked at first evolution. Analyzer **Mind → Profile** keeps the Dark/Neutral/Hero bar: available ordinary babies are editable in Creative with server-held-Analyzer, permission, ownership/range and activity checks; Survival is read-only and adults remain locked. Rendering adds no persistent fields or save-format version. Existing care, training, recency, genetics and locked identity continue to derive the appearance after reload.

The inherited maturity clock remains unchanged. At the default three-hour childhood, passive adult strength reaches 1.2 after **14.4 active adult hours**, while ordinary adult life ends after twelve hours. Whole adult meals add the mod's existing 0.005 strength; forty bridge the 0.20 gap at the terminal boundary, and forty-one or more permit the exact endpoint earlier. Equipped adult care also advances the existing maturity clock while lifespan/birthday counters pause. Training does not accelerate these clocks. These mod rates are separate from the verified source adult/child increment ratio and are documented rather than silently rebalanced.

## Color, finish and attachments

`HeroRunCoatMaterial055` uses the retained baby UV layout and protected facial/expression pixels. The only newly opaque atlas texels are two previously unused feature body/tip swatches at `(127,63)` and `(126,63)` in the 128×64 atlas. Existing mapped alpha stays intact. The small front belly region occupies the approved existing 4×4 pixel patch; adult limb/stem markings use a few broad pixel rows. No extra 3D rings, decorative band meshes or the .54 reserved-pattern sheet are added.

Natural Neutral/Hero baby corners retain their approved region placement. Fresh Hero Run has a yellow halo and blue/yellow accents; mature centered training has a lime halo, while mature Run specialization returns it toward yellow and shifts accents toward cyan/mint. Named, monocolor and jewel coats keep their inherited hue with the same detail placement and bounded value contrast. Shine uses the existing finish pass on the same geometry. These variant rules are authored mod policy: the viewer does not verify all inherited colors or shininess. Animal/flame overlays retain their existing atlas rather than inheriting the new body paint.

## Verification

The coordinated focused Gradle run (`coreTest blockyRunMorphRigTest055 heroRunMaterialTest055`) completed **BUILD SUCCESSFUL**. The dedicated morphology-only GameTest run also completed with **all 163 required cases passed**. Source/geometry assertions remain separate from a visual Minecraft or multiplayer pass:

| Check | Recorded result |
| --- | --- |
| Focused morphology policy | 1,128 checks passed: four-stat ties, simultaneous drives, decreases, independent maturity, age-conditioned baby growth, save/reload and locked identity. |
| New production rig/emitter | 22,226,455 checks passed; 2,488 development frames, 3,840 rectangular-box samples, 996 compact-limb samples, 350 Lionfish motion frames, 2,520 separate attachment matrices and 696 coat cases. Maximum 1% vertex step 0.07039931 model pixels; maximum sampled 20-tick-edit step 0.24693298 pixels. |
| Pixel policy | 23,615,146 checks passed across 172 source coats, including 12,678,120 preserved-alpha pixels and 99,072 exact eye/expression pixels. Maximum sampled 1% channel change was 4. |
| Dedicated morphology GameTests | All 163 required cases passed: retained .52/.53 evolution, permission, migration and persistence suites, .54 Journey rules and 26 new .55 age/training/compatibility cases. |
| Broad dedicated native run | 2,880 cases completed: 2,852 passed, 28 failed in unchanged legacy generic toy-preview assertions/fixtures; none of the failures belongs to a morphology suite. |

The final JAR and refreshed keyboard checks completed successfully (`run055-keyboard-final-build.log`): each piano/organ suite checked **167,184 actual hand samples across 54 production .55 age/axis/growth configurations**, preserving the existing contact, clearance and two-pixel reach limits. The earlier whole `build --continue` invocation exited nonzero: it still included an obsolete .53 production-selection assertion and keyboard checks against the unused rounded .54 asset, alongside the inherited triangle/trumpet failures. The current active profile uses the stronger .55 rig suite; the older surface diagnostics remain explicit historical tasks, and keyboard enumeration excludes only the archived .54 model. Those refreshed keyboard checks pass. The two unchanged transition failures remain unresolved; no completely green full-check claim is made.

The actual Minecraft client completed **all 30 scenes with zero skips**, using NeoForge1.21.1, Lionfish3.1 and the real Accessories/owo runtime. The original full cohort produced256 PNGs. Accepted latest successful evidence contains **252 unchanged original PNGs**, replacing motion scenes23–25 with the recording rerun and the Ruby/glasses scene22 with an open-eye recapture. Static morphology scenes cover six angles; motion/toy/equipped scenes cover three. Neutral/Hero baby quarters, young maximum-trained baby, actual first-evolution state, fresh/mature adult quarters, half-age adult and compatible axes were inspected. Natural idle/gait poses vary with mobility, so matched cameras do not imply identical joint poses or per-image head rescaling; fixed geometry is established by the exact emitted-head checks.

The real Analyzer Mind → Profile screen was opened, its Creative Dark/Neutral/Hero control dragged, the appearance/color updated and actual entity NBT reloaded exactly. Survival/forged/adult locking is covered by native server cases. Representative Red shiny/rabbit/skull and Ruby/phoenix/glasses views were inspected, including native open eyes in the final Ruby capture. Broom shaft contacts and book/page grips remained aligned in selected front/side/back frames. The real Accessories baby/adult attachment remained against the player's back in upright Creative flight; this is not horizontal elytra or swimming verification.

A **43.1-second,1440×900,20fps silent X11 recording** shows accelerated fixture training gains and reductions while production rendering/easing/locomotion run at the native20TPS clock. Fourteen exact decoded frames at1,4,…40 seconds and representative original motion screenshots were inspected for coherent parts and progression. The actor naturally changes seated/crawl/walk poses as its stats change; no alternate demonstration animation is injected. Full interactive movie playback and audio listening were not performed.

Runtime provenance freezes and compares **8,264 production Chao class/asset entries byte-for-byte** with the31,066,080-byte JAR, SHA-256 `58c3279f23ae31532803e93b6e4a84c39b3eb286d349267c1f6c416bed750aab`. CRCs and unique archive entries were verified. The client uses the declared Gradle development classpath; this byte comparison does not claim `runClient` loaded the JAR itself. All three accepted client invocations completed BUILD SUCCESSFUL. Native source and client logs, original image hashes, movie provenance and runtime/JAR coverage are retained in the delivery artifacts.

The 28 broad native failures are in the old generic `ToyPreviewGameTests02527`: 16 still expect sounds for book/crayon/doll activities made intentionally silent in .51; four filter for the wrong car sound ID; eight omit the placed horse/pogo toy their preview adapters require. The failing tests/fixtures, relevant production sources and ten current compiled production classes are byte-identical to the delivered .53 counterparts. This inheritance classification uses exact source/class identity and the obsolete expectations; no new .53 full-server rerun is claimed. Those unrelated legacy tests were not weakened in this focused update.

Recorded inherited .53 outcomes are separate: triangle and trumpet transition checks failed **25/267,696** and **37/267,696** samples respectively, with result hashes identical to the saved .52 comparison; the standalone journal suite passed **8/9**, retaining the `Your gardens` paragraph-format assertion. The current .55 triangle/trumpet result files match the prior known result hashes exactly; the current journal run again passed8/9 with the same unchanged assertion/source. No prior .54 native Journey pass is claimed.

Controlled photos freeze biological inputs and suspend a fixture only when needed to expose its underside. They are not a new hovering behavior or a natural lifetime observation. A fresh-evolution fixture completing its cocoon before photography does not visually verify the full live cocoon. Real client work runs on the development classpath; a separately recorded byte comparison with the final JAR establishes named class/asset coverage without claiming that JAR was loaded by `runClient`.

## In-game recheck

1. Compare normal and developed babies from front, both sides and back; watch the separate front nubs, crest roots, eyes and head box during gains and decreases.
2. Follow Hero alignment and Run training through the first cocoon. Compare the new adult's short stem with independent mature centered and specialized forms.
3. Try Creative alignment/stat edits, combined drives and opposing Power, then save/reload. Confirm Survival cannot edit alignment and the adult bar remains locked.
4. Compare natural, named two-tone, monocolor, jewel and shiny variants at the same state. Watch hats, animal replacements, glasses, hands/feet, toys and player attachments in motion. Operator previews include `/chao testtoy broom once`, `/chao testtoy picture_book loop` and `/chao testtoy stop`, using production animations without rewards.

Actual-user worlds, remote-player views, network latency, arbitrary shaders/resource packs, full modpacks and sound listening remain separate checks unless the final verification block explicitly records them. See [reference evidence](HERO_RUN_REFERENCES_02555.md) for what was observed, measured and provisionally authored.
