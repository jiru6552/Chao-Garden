# Neutral Swim morphology .59: policy and renderer integration

## Scoped identity and six adult targets

Only an ordinary, living, mortal, normal-character, normal-variant adult with a locked Neutral alignment (-0.5 < alignment < 0.5) and locked first type `ChaoState.SWIM_TYPE` (1) enters this new path. Existing Neutral/Normal, Hero/Normal, Hero/Run, babies, Dark types, special characters and variants retain their existing selection branches.

The new first-type-specific family contains balanced, Run, Power, Swim, Swim+Run and Swim+Power. Opposite ordering is the same pair. The balanced secondary target remains recognizably Swim-first; it is not a return to a Normal first type.

Current visible main-stat bars use `100 * level + progress`, bounded to 0..9900, through the existing `HeroRunMorphology052.totalTraining` function:

- Run/Power = clamp((Power - Run) / 960, -1, 1).
- Swim/Fly = clamp((Fly - Swim) / 960, -1, 0).
- Biological visual maturity = existing `gradualVisualStrength()` = evolution strength / 1.20, capped at 1.

The 960-progress full-axis threshold matches previous adult prototype policies; it is provisional mod timing, not timing extracted from the Chao Island viewer. Earlier baby training carries forward. No post-evolution stat baseline or reset is introduced. At maturity zero, the first Swim surface has 100% weight even if prior training already reaches a mature endpoint.

Fly cancels later Swim back toward the balanced Swim-first target. Excess Fly is deliberately clamped at that target for this six-form scope rather than inventing a Neutral/Swim/Fly model. First-type identity remains Swim. Run and Power cancel normally, and either combines with net Swim.

## Shared rendering implementation

`NeutralSwimMorphology059` supplies read-only samples and ten weights. `NeutralSwimAppearance059` follows the existing 20-tick display-only smoothstep interpolation, with per-UUID state bounded to 128 entries and weak per-state frame samples. No biological or saved field is changed by interpolation.

The new `gameplay/chao_neutral_swim_proto059` asset uses metadata `neutral_swim_path_059` and per-mesh `neutral_swim_box_059`. It reuses the existing ten-target rigid-box evaluator and per-asset frame/normal caches. Target 0 is fresh Swim-first; targets 1..3 are the net Swim row at Run/center/Power; 4..6 are balanced Swim-first at Run/center/Power. Targets 7..9 duplicate 4..6 as an authoring guard and receive no production weight. Every enabled weighting remains a nonnegative partition of unity.

The original head box is protected by `fixedHeadBox055()` on the new asset, including the inherited monster amplification guard. Flat box face normals use the same sampled surface as the renderer and attachment envelope sampling. UV/attachment family explicitly aliases to canonical `baby`, while the logical geometry key remains `swim`. This prevents the logical first-type name from redirecting new geometry into old Swim UVs or attachment profiles.

The inherited developed-Swim gait adjustment only steadies head/body walking motion and spreads Power-Swim palms; it does not resize the head. For this new profile it reads the same current Run/Power sample multiplied by maturity. Every old path continues using its saved `runPower` input unchanged.

## Read-only user-facing labels

`ChaoState.secondaryTypes()` recognizes the new current-bar sample, preserving the same +/-0.5 threshold used by previous prototype labels and the same ordered compatible-pair keys. It does not rewrite legacy saved `swimFly`, `runPower`, `secondaryType`, first-type fields or alignment.

Analyzer Growth meters use the new current-bar visual axes only for this family. Their header explains that Fly counters later Swim while first type stays Swim. Maturity hover help explains the provisional 960-progress threshold, carried baby training, age gating, reversibility and locked Swim identity. No Creative control, authorization, hit area, network payload or edit behavior is modified in this pass.

## Existing gameplay and save systems inspected

- First evolution uses `GradualEvolutionRules.firstType(points, trainingRecency)`: greatest earned Swim/Fly/Run/Power points; Stamina excluded; newest actual increase among tied abilities wins. Untouched zero training remains Normal.
- `train` updates current bars and recency only on actual gains. Ordinary drives call it with +24 progress; animals and movement training also reach it.
- `DriveRules.apply` covers Solid, Super Solid, Chaotic, Super Chaotic, Unstable Solid, Unstable Chaotic and Perfect. Multi-stat actual increases are promoted as a group while retaining prior relative recency. Reductions use `setTotal` and lower current bars without inventing a recent raise. The new policy reads resulting bars without changing any drive arithmetic.
- `AnalyzerStatRules.apply` and `CreativeChaoEditRules056` already modify current bars and record actual raises; the new policy therefore follows absolute Creative edits and reductions automatically.
- Natural first evolution resets adult visual strength to zero, locks first alignment/type through `normalizeEvolutionState`, and retains training. Existing `lockedAlignment()` and `lockedPrimaryType()` are read-only views used by the new eligibility check.
- Save encoding already persists current bars, maturity clock, training history, first-evolution lock, locked alignment and first type. Decode normalizes old and current records. No new persistent fields or codec changes are required or introduced.
- Existing server command and Analyzer controls retain their server authorization, sync and locked-adult editing behavior. No changes to those implementations were made here.

## Agent verification boundary

This agent performed source inspection and implementation review. Build, core/native/rig tests, client screenshots, in-game motion, save/reload and dedicated-server verification are coordinated by the root and tests059 agents. Do not describe the source audit alone as runtime proof or viewer inspection.
