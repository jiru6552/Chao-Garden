# Hero Run morphology prototype — 0.25.52

This focused update extends the delivered 0.25.51 project. It follows ordinary Chao from neutral baby through Hero/Run development, first Hero Run evolution, and adult Hero/Run/Run specialization. Other primary adult forms, Chaos/character Chao and themed variants keep their existing model paths. Java 21, Minecraft 1.21.1, NeoForge 21.1.219 and external Lionfish API 3.1 remain required.

## Findings and implementation

The existing in-house mesh renderer already supports compatible vertex targets and Lionfish bone animation. Its old baby deformation was small, alignment textures switched at a threshold, and adult deformation used maturity as its overall magnitude. The new focused model path shares topology and samples visible vertices, joints and attachment landmarks together. No new library or model-renderer replacement is needed. Geometry and coat changes share a client-only 20-tick interpolation for large edits; the cache is bounded and resets for stale/replaced entities. Overlay, animal-part and legacy-hat pose transfer includes the changing base joints, not only animation offsets. Source-derived shape measurements are fitted to the mod's existing style and coat UV layout; this is not a wholesale import of the original game's rendering engine.

[Reference and source audit](HERO_RUN_REFERENCES_02552.md) distinguishes binary-verified values, source-model silhouettes, mod interpretation and unavailable evidence. The provided ChaoMain.prs contains the original Hero Run young, normal-adult and Run-specialized models. Their silhouettes inform the single crown at first evolution and the curved three-pronged crown, rear head point and larger feet of the Run/Run endpoint. Offline source-mesh sheets are labeled separately from Minecraft captures. Fresh gallery photos guide the natural white/light-blue family, blue/yellow hand markings and violet belly. Exact RGB values and their fit to the cuboid UV layout remain a Minecraft interpretation; inherited colors, jewels, shine and animal/wardrobe identity are preserved.

## First evolution and ties

Ordinary first evolution now chooses the highest **earned points** among Swim, Fly, Run and Power, preserving the existing meaning of the displayed numerical ability points. This is not the highest level number. This is the requested Minecraft rule: the accessible Chao Island evolution guide explicitly rejects highest-visible-stat selection as an explanation of the original games, whose outcomes follow their influence system. The old selector included Stamina and could choose Normal because Stamina was highest. Stamina no longer competes with the four abilities.

An exact earned-point tie uses the saved history of actual training increases among the tied abilities. A positive partial-bar increase counts; no-op or decreasing edits do not steal recency. If none of the four abilities has any recorded increase and all four point values are zero, Normal remains the compatibility fallback. Old nonzero tied records without history use Swim, Fly, Run, Power order deterministically; missing history is not invented.

One combined drive is a simultaneous transaction. Actually increased members are promoted together, preserving their previous relative recency; newly encountered members use Swim, Fly, Run, Power, Stamina order. The last iteration of a loop cannot arbitrarily make Power win a balanced drive. Existing animal and multi-stat fruit effects remain ordered sequences of their existing training calls; their order is preserved. This distinction is intentional and tested.

The rare Chaos route remains an explicit exception: the previous five-stat Normal-admission predicate is retained solely for an otherwise qualified Chaos candidate, along with the existing reincarnations, happiness and full registered-DNA requirements. This prevents the four-stat ordinary selector from making the established Chaos path unreachable. Existing Chaos Chao and saved first-evolution identities are not re-selected.

## Appearance policy and timing

| Quantity | Prototype policy |
| --- | --- |
| Hero baby development | Continuous Hero weight `clamp(alignment / 0.5, 0, 1)`, reaching the Hero endpoint at the actual Hero identity threshold. |
| Run baby development | `clamp((Run total - Power total) / 480, 0, 1)`. |
| Adult Run training target | `clamp((Run total - Power total) / 960, 0, 1)`. |
| Training total | Existing bounded `level * 100 + progress`; one ordinary drive adds 24 progress when capacity permits. |
| Biological maturity | Existing saved maturity, age and whole-fruit clock; no stat edit subtracts age or maturity. |
| Visible adult Run specialization | Training target multiplied by the existing normalized maturation gate; the underlying adult body proportions continue to mature independently. |

The 480/960 thresholds correspond to 20/40 net ordinary drives **from zero**, not 40 additional drives after evolution. Baby training carries into the adult target. Run and Power oppose one another; equal gains cancel in this target. Stat decreases, unstable outcomes, training resets and Creative Analyzer level/progress edits all affect the same current-value rule. Grade-only edits do not invent training. Existing Swim/Fly and unfocused adult influences retain their prior behavior around the focused model path.

The freshly read Chao Island age page explicitly distinguishes adult appearance from visible stats and describes reversible animal/drive influences without a fixed calendar age. Deriving this prototype from current training bars is the user-requested mod rule for decreases and Analyzer consistency, rather than a claim to reproduce every original hidden variable.

Original-file constants corroborate the ordinary child/adult influence steps and relative adult growth rate, but mapping every mod item or combined-drive tier to this total-progress rule is a documented prototype interpretation. It is not a claim that the original games used Minecraft levels, bars, combined drives or these absolute durations.

The existing clock adds adult strength at one quarter of the child rate and caps it at 1.2. With default `childTicks=216000`, first evolution is at three active hours without accelerators; adult strength 1.2 takes 14.4 active adult hours from zero by age alone. Default adult lifetime is twelve active hours, so age alone does not reach that endpoint during a normal life. Each whole four-bite adult fruit adds 0.005 strength. Roughly forty fruits cover the 0.20 gap at the twelve-hour boundary; at least forty-one fed earlier allow the exact endpoint before that boundary. Fruit timing/care and lifespan rules are preserved. The existing equipped-adult maturation clock can also provide additional development time while its lifespan counter is paused. Client staging uses explicit test fixtures rather than claiming to have waited through a natural lifetime.

Adult alignment and first type remain locked at first evolution. Specialization can recede without turning a Hero adult Dark or returning its biological body to a baby. The existing cocoon conceals the ordinary baby/adult global render-scale change. A lightly trained Run winner can still have remaining first-form development concealed by that cocoon; the fully developed prototype endpoint is the continuity comparison.

## Saves, synchronization and integration

There are no new persistent state fields or format version changes. The existing encoded state string is still the authoritative entity/NBT, client-sync and carrier payload. Morphology is derived from those saved values. Eleven actual prior-save records (babies and Neutral adults, with synthetic legacy Hero Run cases tested separately) are included in the native regression fixtures and are checked for byte-exact round trips; no user save files are edited by this update.

The Analyzer and journal now explain four-stat selection, locked identity, training target and biological maturity separately. `/chao inspect` reports the current prototype percentages without changing the Chao. Existing ordinary/combined drives, animal/fruit training, passive activity gains, decreases and Analyzer edits all feed the current-value appearance policy. No broad influence rewrite is applied to unrelated adult paths. Existing saved Hero Run adults receive the new presentation derived from their current saved training and maturity; their care, identity and genes are retained.

## In-game evaluation

Use a normal baby Chao and Hero Garden Fruit, then ordinary Run drives. The first twenty net Run drive units span the baby Run target; every five units is 25%. Hero Fruit raises alignment in its existing increments, so those care steps need not exactly match the Run percentages. `/chao inspect` shows both values. `/chao mature` is the existing operator first-evolution shortcut; it does not set adult specialization or maturity. Creative Analyzer edits can vary Run and Power level/progress bars; changing them should alter specialization while biological maturity stays unchanged.

```text
/give @s chao_garden:hero_garden_fruit 8
/give @s chao_garden:run_drive 40
/chao inspect
/chao mature
/chao testtoy broom once
/chao testtoy picture_book loop
/chao testtoy stop
```

Test previews use the existing production toy implementation and retain their once/loop/stop cleanup and no-reward rules. The source contains an opt-in development client helper for the staged galleries, not a separate demonstration mesh.

## Verification

The Java 21 build compiled successfully. Core verification passed **170,531 ChaoState checks**, including **964 focused Hero Run checks**, plus the existing drive arithmetic, all 24 allocation permutations and 50,000 persisted random rolls. The source audit reproduced all six original reference shapes (40 nodes / 720 triangles each), the input hashes, growth cap and relative adult rate.

The actual dedicated-server run passed **all 425 required tests**: 68 new morphology tests and 357 previous toy/item/DNA/shop/loot regressions. Real Accessories/owo and Lootr were installed. New coverage includes real first cocoons and ties across alignments, Stamina exclusion, the exact Chaos exception, held ordinary/combined-drive commits and pending-item reloads, every combined tier, increases/decreases, Creative Analyzer permission/sync/NBT behavior, adult identity, carrier release, reincarnation and eleven actual older-save records.

The final focused mesh/attachment check passed **4,180,524 assertions** over 1,572 geometry/pose frames and 1,032,336 emitted vertices. It covers quarter stages and intermediate samples, exact fully developed baby/fresh-adult local surfaces and joints, eye panels, retained axes, seven motion families, overlay/animal/hat/merchant joint transfer, palette effects and bounded interpolation. The largest small-progress vertex step was 0.024 model pixels. Optional negative/bright/Hero-brush swatches are checked mathematically; those optional flags are not claimed as an additional client visual pass.

Expanded prone verification passed **34,382,840 assertions** over 848 form/age/morph/head cases, 569,776 quarter/contact frames, 1,872 replacement-foot cases and 3,456 outgoing blend frames. Sitting passed **1,311,228 checks** over 486 form/age/morph cases, retaining its strict floor, head-clearance and transition limits.

The final actual-client cohort passed **all 27 scenes**, saving **708 original PNGs**. It includes baby/adult 0/25/50/75/100 targets from front, side and rear; a separately identified newly evolved adult; a real evolution cocoon; gradual training walks; an Analyzer-driven decrease; opposite-alignment care; skull/pumpkin/melon hats; rabbit parts, phoenix wings, red shiny and ruby-jewel coats and round glasses; broom/book contact; and actual Accessories slots in baby ground/flight and adult flight views. Representative frames were reviewed against the fresh gallery photographs. No visible blocker was found in that review. It is not a claim of manually watching every frame or testing every toy/variant.

The galleries use controlled, fixed biological maturity to isolate training, rather than claiming all adult stages are photographs of one elapsed natural lifetime. The separate 94-second silent MP4 contains **1,880 actual recorded frames at 20 FPS**. Its captions identify accelerated test-fixture training; client playback remains normal-time. Whole-source-frame selection, decoded timestamps and zero duplicate/drop checks are recorded in the movie provenance. No generated or interpolated Chao images replace the captured frames. Natural multi-hour aging was verified through the production rules/tests and controlled fixtures, not by waiting through an entire twelve-hour life.

The broad `build --continue` gate is **not green** because of two previously documented instrument-transition failures: triangle has 25 failures in 267,696 samples (minimum -0.54432297 pixels), and trumpet has 37 in 267,696 (minimum -0.58569264). Both result files have exactly the same SHA-256 as delivered 0.25.51. Their tests were not relaxed. The same broad run initially exposed two fixture null lookups in prone/sitting: the tests assumed the nominal asset cache key, while the renderer selected the new shared morph mesh. Those fixtures now use the actual sampled mesh and include all five prototype stages. The subsequent `assemble proneToyRigTest051 sittingRigTest heroRunMorphRigTest052` invocation passed with the expanded assertions above. No floor or clearance threshold was weakened, and no functional pose change was needed for those fixture corrections.

All other completed broad checks passed, including existing active/quiet toys, companion carry, accordion, drum, flute, guitar, organ, piano, posture, rocking horse, running gait and **276 visible transitions across 92 histories**. The standalone journal content suite passed 8 of 9 checks; its remaining pre-existing `Your gardens` paragraph-format assertion was reproduced from the 0.25.51 snapshot. The parser was corrected to retain the existing dynamic 31-DNA pages, without editing the unrelated garden page or removing its assertion. The journal and instrument baseline comparisons are included with the delivery.

Production-JAR packaging checks verify unique entries, CRCs, embedded version, current prototype asset/schema hashes, absence of development helpers/fixtures and preservation of previous assets and unrelated toy/loot/movement classes. All delivered client PNGs are selected from the one final 27-scene cohort, with original-byte hashes and literal native/render completion evidence.

## Limits

The Run gallery, Hero specialization gallery and age page were read after the domain setting was applied, and their reference photos were visually checked. The two requested wiki articles still returned HTTP 403; exact access evidence and timing limits are recorded in the reference audit. Original source-model reference sheets are offline geometry renders, not Minecraft screenshots. Real client captures and motion recording are labeled separately. This environment has no audio output hardware; there are no new sounds in this update. Independent remote-player views, latency, resource packs, shaders, full modpacks and a real user world require their own checks. Native server tests use actual server APIs with simulated players rather than two independent Minecraft installations.

The source ZIP is a cumulative overlay onto full 0.25.43 source, with previous updates and explicit deletions. Follow its source-deletion instructions or use the complete delivery branch. The deletion helper operates on source files only, never Minecraft saves.
