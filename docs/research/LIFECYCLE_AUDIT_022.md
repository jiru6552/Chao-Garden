# Lifecycle audit — 0.22.0

This audit reconstructs the changes from the retained review notes after workspace maintenance removed the previous unsaved working tree. Results below refer to the reconstructed source and fresh executions; previous run counts are not reused as current evidence.

## Reference rules and adaptations

The primary reference is Sonic Adventure 2 Battle / HD. [Chao Island's stats guide](https://chao-island.com/info-center/basics/stats.html), [stats wiki](https://chao-island.com/wiki/Stats) and [death guide](https://chao-island.com/info-center/life-cycle/death.html) support the implemented rules:

| Rule | Implemented behavior |
| --- | --- |
| E–S level gains | Uniform integer outcomes: E11–15, D14–18, C17–21, B20–24, A23–27, S26–30. |
| First evolution | Raise the selected ability's expressed hereditary allele once, capped at S; Normal and Chaos select Stamina. |
| Adult development | Young and full adult share one adult lifespan. Later morphology is gradual and does not create another evolution cocoon. |
| Terminal care | Ordinary reincarnation requires happiness strictly greater than50. |
| Reincarnation | Retain grades, identity, lineage, name, awards and worn items; visible/hidden levels become1 and progress0. |
| Point carry | Exactly floor(old points/10), with valid input ranges0..9999 visible and0..4000 hidden. |

The user explicitly requested literal ten-percent carry. The source game's500-point rebirth ceiling therefore no longer truncates high valid modified visible stats. Ordinary perfect-S training converges2970→3237→3263→3266→3266:99 gains in the first life,98 after the level-one reset. Existing saved lives receive no invented retroactive refund.

Chaos care now uses **happiness greater than80**, independently of ordinary reincarnation's50 boundary. The [game-specific Chaos page](https://chao-island.com/wiki/Chaos_Chao) and [SA2B field-offset documentation](https://chao-island.com/guides/chao-hacking/hacking-pc/chao-offsets/chao-general.html) both specify over80; an older mixed-game guide uses a50 threshold while another general summary says80 or higher. This audit follows the game-specific strict boundary and tests79/80/81 in all three alignments. The medical checklist uses the same threshold. The separate CWE character rule and mechanical heart care remain unchanged.

## Boundary and transaction repairs

All lifecycle duration comparisons now use the same reachable20..Integer.MAX_VALUE−1 core range as the saturating age counters, including robot lives. Public config minima remain unchanged. Adult quarter-rate maturity keeps wide arithmetic. Reincarnation history saturates at the existing9999 persisted limit; care counters stop at Integer.MAX_VALUE.

Pending terminal outcomes run before gift processing. A denied held-item or equipment insertion preserves its exact stack, actor and pending flags for retry and reload. Natural death retires only after all real stacks commit. Rebirth returns a held object before creating its individual egg, while worn objects transfer to that egg. Recursive cleanup or damage during a transaction cannot release the same equipment twice. The existing UUID/egg serialization guards remain in effect.

Omochao keep their adult-only C→B→A→S reconstruction design, illness/animal restrictions and creator ancestry. A committed component/heart is never emitted again while equipment delivery retries. Their new chassis begins fresh skills as documented; the ordinary egg/ten-percent loop is a separate biological lifecycle.

## Current verification

The reconstructed `LifecycleRules022Test` is wired into `ChaoStateTest`, and thus the Gradle `coreTest`/`check` path. A fresh standalone Java21 execution passed **90,618 assertions**:50,000 visible carry cases,10,000 visible level/progress groups,30,000 hidden range/reset checks, grade/allele evolution, perfect-S convergence,50/51 terminal care, maximum reachable durations, counter/identity persistence 43 dedicated Chaos boundary/animal/type checks and7 assertions proving a natural two-reincarnation,21-animal,Stamina-fruit path to Chaos.

Four real-server GameTests are present for: the complete physical egg→both cocoons→serialized egg→same-UUID next life; denied gray-death held/worn returns with reload and recursive callbacks; an already-committed robot component with denied gear; and maximum egg incubation across repeated denied hatches, reload, pickup, item serialization and placement. Both fresh centralized server profiles now pass all122 required tests: default NeoForge and actual Accessories with identity fixtures. The closed source namespace contains exactly122 methods, including these four unconditional lifecycle tests; the compiled class exists. `LIFECYCLE_022_SERVER_PROOF.json` records source/log hashes, exact method IDs and aggregate result lines without inventing per-ID log messages.

A fresh focused Java21 compilation passes for current ChaoState/GradualEvolutionRules/OmochaoRules, ChaoEntity, OmochaoLifecycle, the medical screen, all four new GameTests and the helper against the restored NeoForge21.1.219 APIs. The complete standalone ChaoStateTest also passes168,848 assertions, using current core/test sources and the unchanged baseline variant-definition dependency; this includes the90,618 focused assertions and is not another additive count.

The opt-in `LifecycleVisualSmokeSupport022` uses actual entity transitions. It accelerates incubation, elapsed age and whole-fruit maturity, while NoAI/NoGravity/NoPhysics freeze preview motion and prevent an artificial airborne training tick. The real-server tests instead retain gravity and stand on actual stone floors. Its grade cards are explicit display fixtures: seed E–S and perform one real100-progress training step. Before/after charts seed1255/2509/1699/21/1009 and verify125/250/169/2/100 with retainedS/S/B/S/B. The capture driver removes loose setup shells and clears accumulated client fixture particles without modifying the underlying stage or statistics. Helpers are excluded from the installable JAR.

Full Gradle/server and final-client/PDF evidence is centralized in the release validation. That evidence supersedes the focused compilation and pure-rule status above.

Final visual review passed all 41 assigned actual raw frames and all 25 individually opened PDF pages (19–22,32–36,111–126). The frozen 135-page gallery SHA-256 is `f65667e8ac1c022befb6dc91822091cc663797a6ca621cd3cd90ff561d400183`. `../validation-0.22/review-lifecycle.json` binds every reviewed raw frame and rendered PDF page to its capture/source hashes. Numeric before/after charts and all six seeded grade rows match their oracles; robot ancestry, coats, adult restrictions, construction and physical outputs are clear. No final visual blockers were found. This inspection does not substitute for the separate real-server lifecycle tests.
