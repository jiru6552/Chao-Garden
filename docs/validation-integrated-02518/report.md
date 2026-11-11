# Integrated source verification

Reviewed current source snapshot. Production files were not changed; report-producing final validators ran against a private copy. No Minecraft/NeoForge compilation, server GameTests, client rendering or packaging was run.

## Latest cross-chunk rerun after breeding publication fix

Rechecked current working source on 2026-11-04 after the breeding publication/reentry fix. Every command below passed:

| Command | Result |
|---|---|
| `bash scripts/test_toy_interactions_02518.sh` | 3,089 Java rule assertions; 22 Python geometry tests |
| `bash scripts/test_journal_02518.sh` | 9 content checks; 19 executable core assertions |
| `python3 -B scripts/tests/check_tatami_isolated.py` | 205 extracted-production-method assertions |
| `python3 -B scripts/tests/check_daycare_isolated.py` | 135 extracted-production-method assertions |
| `python3 -B scripts/tests/check_breeding_isolated.py` | 63 extracted-production-method assertions |
| `java docs/validation-integrated-02518/ParseToyJava.java $(find src/main/java src/test/java -name '*.java')` | 335 Java files parse without syntax errors; no type checking |
| `bash -n scripts/test_toy_interactions_02518.sh scripts/test_journal_02518.sh docs/validation-integrated-02518/commands.sh` | Shell syntax passes |

Python `ast.parse` also passed on all four `scripts/tests/*.py` files and the five focused journal/toy geometry Python harnesses, without writing bytecode or editing production files. Exact output logs and SHA-256 hashes of the source/test/script inputs for this rerun are preserved in `latest-rerun.json` and `latest-*.log` beside this report.

The three extracted-method harnesses use lightweight Minecraft API stubs. They prove the stated isolated logic contracts, not actual event ordering, NeoForge API compatibility, server persistence, packet delivery or client rendering. No high-confidence cross-chunk regression appeared in this rerun.

**Native status remains unchanged:** full Gradle/NeoForge build, native GameTests (including the new breeding publication tests), and actual client rendering are unrun. The previously observed blocker is the unavailable Gradle distribution/dependency cache and failed distribution download. No new installable JAR or native pass is claimed. The earlier final-asset sweep below was not rerun in full for this Java-only breeding follow-up; its result and historical-stage distinction remain explicitly scoped.

## Focused checks
- PASS: `bash scripts/test_toy_interactions_02518.sh`: 3,089 Java assertions and 22 geometry tests (4 coffin, 5 mounted, 5 rigid, 8 stationary).
- PASS: `bash scripts/test_journal_02518.sh`: 9 source-content tests and 19 real core-mechanics assertions.
- PASS: `java docs/validation-integrated-02518/ParseToyJava.java $(find src/main/java src/test/java -name '*.java')`: 335 Java source files parsed; not type checked.

## Resource audit
- 7,990 source resource files, compared with 7,980 in the historical source verification.
- 11,974 internal model/parent/texture references resolved with no missing targets.
- Compared to the original supplied JAR: exactly 10 new production resources (five floating lifesaver JSON definitions and five OBJ poses), zero removed resources, and 14 changed resources. Five additional source-only GameTest resources are correctly excluded by build.gradle.
- Existing JAR and historical PASS records do not represent current source.

## Final asset validators
Results: 49 commands passed after the documented retries; one historical intermediate-stage validator failed against final output. The lifesaver state-contract mismatch has been corrected and rerun successfully (7,489 checks). The school-door test initially failed because no javac executable was on PATH. Retried unchanged using a temporary javac wrapper around the installed JDK compiler module, and all 48 dependency-free door geometry states passed.

| Command | Result |
|---|---|
| `python scripts/validate_sa2_assets_02515.py --rigid-toy-poses` | PASS |
| `python scripts/validate_sa2_assets_02515.py` | Historical intermediate stage: fails garment assertion against later final assets |
| `python scripts/validate_cwe_assets_02516.py` | PASS |
| `python scripts/validate_fruit_models_02512.py` | PASS |
| `python art/validate_ca2_fruit_018.py` | PASS |
| `python art/validate_forms_019.py` | PASS |
| `python art/validate_cwe_content_019.py` | PASS after exact-state contract correction: 7,489 checks |
| `python art/validate_gradual_evolution_021.py` | PASS |
| `python art/validate_characters_020.py` | PASS |
| `python art/validate_chaos_020.py` | PASS |
| `python art/validate_chaos_021.py` | PASS |
| `python art/validate_overlay_head_fit_021.py` | PASS |
| `python art/validate_retired_characters_020.py` | PASS |
| `python art/validate_inspector_ui_020.py` | PASS |
| `python art/validate_native_rig_018.py` | PASS |
| `python art/validate_heritage_018.py` | PASS |
| `python art/validate_surface_018.py` | PASS |
| `python art/validate_chao_overlay_clearance.py` | PASS |
| `python art/validate_chao_crawl_clearance.py` | PASS |
| `python scripts/validate_official_chao.py` | PASS |
| `python scripts/validate_gene_assets.py` | PASS |
| `python scripts/validate_seed_sprites.py` | PASS |
| `python scripts/validate_egg_shell.py` | PASS |
| `python scripts/validate_variants.py` | PASS |
| `python scripts/validate_distribution_023.py` | PASS |
| `python scripts/validate_training_supplies_024.py` | PASS |
| `python art/validate_staff_025.py` | PASS |
| `python art/validate_school_emblem_0251.py` | PASS |
| `python art/validate_portals_0251.py` | PASS |
| `python art/validate_lobby_0252.py` | PASS |
| `python art/validate_environments_025.py` | PASS |
| `python scripts/test_lobby_material_manifest0255.py` | PASS |
| `python scripts/validate_area_music_0257.py` | PASS |
| `python scripts/validate_supplied_audio_02511.py` | PASS |
| `python scripts/validate_school_singing_0258.py` | PASS |
| `python scripts/validate_classroom_toys_0258.py` | PASS |
| `python art/validate_hero_garden_0253.py --self-test` | PASS |
| `python scripts/validate_clinic_refinement_0254.py` | PASS |
| `python scripts/validate_school_doors_0255.py` | PASS |
| `python scripts/test_school_door_geometry_0255.py` | PASS on temporary JDK compiler-wrapper retry; initial environment failure |
| `python scripts/validate_kindergarten_layout_0255.py --self-test` | PASS |
| `python scripts/validate_lobby_approaches_0255.py` | PASS |
| `python art/validate_dark_garden_0255.py --self-test` | PASS |
| `python scripts/validate_garden_device_0254.py --self-test` | PASS |
| `python art/validate_classical_models_0254.py --self-test` | PASS |
| `python scripts/validate_kindergarten_halls_0252.py --self-test` | PASS |
| `python art/validate_proportions_022.py --self-test` | PASS |
| `python art/validate_variants_022.py --self-test` | PASS |
| `python art/validate_items_eggs_022.py --self-test` | PASS |
| `python art/validate_resource_hygiene_022.py --self-test` | PASS |

## Historical stage and corrected integration contract
- Corrected and independently rerun: `art/validate_cwe_content_019.py` previously assumed 32 states for every toy. It now checks the exact Cartesian state domain and model/rotation bindings: 64 for the dry/wet lifesaver, 32 for the other toys. It also checks all five floating models. All 7,489 assertions pass.
- `scripts/validate_sa2_assets_02515.py` is an intermediate-stage validator in `generate_all_assets.py`: it runs immediately after the 0.25.15 generator, **before** `generate_cwe_assets_02516.py` replaces garments with later source-art geometry. Running that historical full validator against final assets fails its old 16-vertex `Missing garment bevel` assertion. This is not evidence of a new toy or final-garment regression.
- The final-stage `scripts/validate_cwe_assets_02516.py` passes its actual wearable topology, finite geometry, UV, face-index, source ownership and attachment checks. The 0.25.15 focused rigid-toy validator also passes 4,378 checks against the current toys.
- A complete staged regeneration was **not** run. Historical-stage failure is retained explicitly, not silently counted as a pass. Final-output checks and syntax parsing do not establish a full build/release pass.

## Cross-chunk source review
Dedicated keyboard, coffin, horse/ring, ball and stationary branches remain separated; shared session timelines and synchronized kick metadata are present. Existing deferred mounted recovery and its suffocation/rescue fixes are retained. No additional high-confidence controller regression found in this pass.

The 63 newly authored native toy tests (18 interaction, 15 mobile, 25 stationary, 5 presentation) remain unrun. Child movement, collision/controller runtime behavior, networking and rendered anatomical fit still need native verification.

## Reproduction
Run from the source root. `docs/validation-integrated-02518/commands.sh` preserves every executed focused/final validation command and continues after failures. It runs source-only checks, not Minecraft. Validators may refresh their generated report JSON files; run in a disposable source copy to preserve the working tree. The syntax-only helper and resource-reference audit are included alongside the command list. The optional original-JAR audit argument expects the supplied JAR next to the source directory; omit it for a references/count-only audit. `results.json` preserves initial outcomes and explicit retries; `resource-audit.json` preserves reference/count findings.

For the school-door test on this machine, the installed Java includes `jdk.compiler` but no `javac` launcher on PATH. A temporary launcher invoking `java -m jdk.compiler/com.sun.tools.javac.Main` was used with a sibling `java` symlink. This is a dependency-free Java geometry check, not a native server/client test.
