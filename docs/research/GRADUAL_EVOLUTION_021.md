# Gradual evolution and fixed first types — 0.21

The Chao Island gradual-evolution reference separates **direction** from **strength**. Swim/Fly and Run/Power are two signed sliders, each bounded to −1…+1. A separate age/food magnitude makes those directions gradually visible. A newly evolved adult starts at magnitude zero; later care changes its secondary shape without another cocoon.

The first cocoon fixes alignment and the primary type for that life. A later Swim secondary on a Fly primary is valid: the fixed and mutable categories are separate. Only opposite directions *within the mutable category* exclude each other. The nine ordinary targets are Normal, Swim, Fly, Run, Power, Swim+Run, Swim+Power, Fly+Run and Fly+Power. Their continuous intermediate shapes remain available between endpoints.

## Explicit Minecraft adaptation

The user requested first-type selection from the greatest trained stat, with the most recently increased stat breaking ties. This differs from SA2's original hidden-influence selection and fixed influence tie precedence. This release intentionally follows the requested rule: compare all five trained point totals, mapping Stamina to Normal. A five-element most-recently-increased list resolves ties among the greatest stats, even if the latest overall increase belongs to a weaker stat. Positive partial-bar training counts; decreases and already capped gains do not. A wholly untouched child becomes Normal. The existing twenty-one-animal/two-reincarnation Chaos requirement remains attainable through a Stamina-leading Normal first evolution.

Primary type, exact alignment, mutation sliders and age magnitude persist separately. Old adults keep their saved primary/alignment, rather than having their best current stat retrospectively replace their original first evolution. The first-identity latch also prevents stale public-field edits from changing the next save. Reincarnation releases that latch for the next childhood.

## Clock and fruit

`ChaoState.evolutionStrength()` exposes 0…1 for children and 0…1.2 for adults. Childhood gains one unit over the configured loaded childhood duration. Adult time gains strength at one quarter of that rate, matching the source relationship. Loaded cocoon time and offline time do not count. Food and age contribute independently, and sufficient childhood food can bring the first cocoon forward.

`onFruitEaten()` is called once for a successfully consumed whole four-bite item: +0.02 childhood or +0.005 adulthood. It is separate from `feed()` and `train()` so a fruit that trains several stats cannot accidentally add several meals' worth of magnitude. The host calls this after actual item acceptance/eating, not when the item is offered. The renderer normalizes the ordinary adult range over 1.2 and does not extrapolate beyond the fully aged reference target.

`setEvolutionStrength(float)` is a bounded fixture/debug API; it also sets the loaded-age checkpoint. Capture fixtures assign their age first, then call it. The young-adult base mesh and fully aged center target are different: multiplying only the signed axes by age would incorrectly leave an untrained center unchanged throughout life.

## Persistence and optional extremes

Schema 6 keeps the existing twenty-five-section layout and adds bounded fields to the care extension: double-precision magnitude, loaded-age checkpoint, five-stat training recency, fixed first identity, optional overdrive directions, and robot lifecycle markers/outcomes. These do not consume the bounded feature/ancestry map. Versions 2–5 retain their genes, phenotype, animal history, needs, family records, stable lineage identity and earned rewards. Their old oversized ordinary axes are clamped, not silently restored as Monster development. Legacy magnitude derives from saved loaded age once, when the host supplies its clock duration; repeated reloads cannot add the same age twice.

The older optional Monster rule remains disabled by default and uses separate private direction fields. Ordinary APIs and persistence keep `swimFly` and `runPower` within −1…+1. New explicit enabled overdrive training can still progress the optional rule; it does not redefine or exaggerate ordinary endpoint targets.

## Mechanical Chao integration

Omochao bypasses biological first evolution and ordinary reincarnation. The pure state delegates its finite adult lifespan transition to `OmochaoRules`, blocks animal samples, illness, mating and Doctor/Hyper grade shortcuts, and retains pending component/heart outcomes outside feature-map capacity. Its family and rank reconstruction are implemented by the mechanical lifecycle owner. Ordinary genetic Chaos Chao remain immortal and retain their loaded adult calendar.

## Validation

The direct Java 21 state suite passes 78,230 assertions after these changes: the previous 77,943 state checks, 96 new gradual-evolution cases, and 191 mechanical lifecycle cases. New cases cover age/food independence, early food evolution, zero-strength adult reset, quarter-rate adult growth, every primary/alignment choice, partial-bar and non-max latest-stat ties, primary/alignment locking, all nine valid secondary corners, reversal from Fly to Swim, legacy magnitude/axis migration, saturated traits, and separate optional development. Four real-entity GameTests passed in both completed 117/117 required-test server profiles: first-cocoon identity locking, a held drive crossing into an opposite secondary, one multi-stat fruit maturity commit across an eating reload, and real sneak-pickup/inventory-NBT/release clock preservation. The default profile completed in 6.116 seconds; the real Accessories/oωo profile completed in 7.272 seconds. The final actual-client study completed 340 ordered captures and passed 7,259 form/behavior samples, 421 coat-material cases and the other runtime probes recorded in `TESTING.md`. Bounded completion evidence and source-log hashes are retained in `docs/validation-0.21/server-default.json` and `server-accessories.json`.

## References

- https://chao-island.com/wiki/Gradual_Evolution — cached search text was readable; direct wiki retrieval returned 403. The page labels itself a draft.
- https://chao-island.com/wiki/Evolution — first-cocoon locking and the original influence-based selection, explicitly distinguished from the requested highest-stat adaptation.
- https://chao-island.com/info-center/advanced/age.html — secondary forms remain mutable throughout adult aging and do not create another cocoon.
- https://chao-island.com/guides/chao-hacking/hacking-pc/chao-offsets/chao-evolution.html — independent signed direction and 0…1.2 transformation-magnitude fields.
- https://chao-island.com/gallery/advanced-evolution/types.html — alignment / first type / secondary type naming.
- The Neutral, Hero and Dark advanced galleries supply separate young-adult and fully aged references, including Normal-secondary targets that differ from their young-adult bases.
