#!/usr/bin/env bash
# Run from the source root; report-producing validators may update docs JSON.
# These are source-only checks. No native build/server/client run is implied.
# Continue after failures so historical-stage checks cannot hide final-output results.
set +e
bash scripts/test_toy_interactions_02518.sh
bash scripts/test_journal_02518.sh
python3 -B scripts/tests/check_tatami_isolated.py
python3 -B scripts/tests/check_daycare_isolated.py
python3 -B scripts/tests/check_breeding_isolated.py
python docs/validation-integrated-02518/audit_resources.py ../chao_garden-0.25.18.jar
java docs/validation-integrated-02518/ParseToyJava.java $(find src/main/java src/test/java -name '*.java')
python scripts/validate_sa2_assets_02515.py --rigid-toy-poses
# Historical intermediate stage: garment assertion is expected to fail on final CWE outputs.
python scripts/validate_sa2_assets_02515.py
python scripts/validate_cwe_assets_02516.py
python scripts/validate_fruit_models_02512.py
python art/validate_ca2_fruit_018.py
python art/validate_forms_019.py
python art/validate_cwe_content_019.py
python art/validate_gradual_evolution_021.py
python art/validate_characters_020.py
python art/validate_chaos_020.py
python art/validate_chaos_021.py
python art/validate_overlay_head_fit_021.py
python art/validate_retired_characters_020.py
python art/validate_inspector_ui_020.py
python art/validate_native_rig_018.py
python art/validate_heritage_018.py
python art/validate_surface_018.py
python art/validate_chao_overlay_clearance.py
python art/validate_chao_crawl_clearance.py
python scripts/validate_official_chao.py
python scripts/validate_gene_assets.py
python scripts/validate_seed_sprites.py
python scripts/validate_egg_shell.py
python scripts/validate_variants.py
python scripts/validate_distribution_023.py
python scripts/validate_training_supplies_024.py
python art/validate_staff_025.py
python art/validate_school_emblem_0251.py
python art/validate_portals_0251.py
python art/validate_lobby_0252.py
python art/validate_environments_025.py
python scripts/test_lobby_material_manifest0255.py
python scripts/validate_area_music_0257.py
python scripts/validate_supplied_audio_02511.py
python scripts/validate_school_singing_0258.py
python scripts/validate_classroom_toys_0258.py
python art/validate_hero_garden_0253.py --self-test
python scripts/validate_clinic_refinement_0254.py
python scripts/validate_school_doors_0255.py
python scripts/test_school_door_geometry_0255.py
python scripts/validate_kindergarten_layout_0255.py --self-test
python scripts/validate_lobby_approaches_0255.py
python art/validate_dark_garden_0255.py --self-test
python scripts/validate_garden_device_0254.py --self-test
python art/validate_classical_models_0254.py --self-test
python scripts/validate_kindergarten_halls_0252.py --self-test
python art/validate_proportions_022.py --self-test
python art/validate_variants_022.py --self-test
python art/validate_items_eggs_022.py --self-test
python art/validate_resource_hygiene_022.py --self-test

# School-door retry if the installed Java has jdk.compiler but javac is absent:
# tmp=$(mktemp -d); java_bin=$(command -v java)
# printf '#!/bin/sh\nexec "%s" -m jdk.compiler/com.sun.tools.javac.Main "$@"\n' "$java_bin" > "$tmp/javac"
# chmod +x "$tmp/javac"; ln -s "$java_bin" "$tmp/java"
# PATH="$tmp:$PATH" python scripts/test_school_door_geometry_0255.py
# rm -rf "$tmp"
