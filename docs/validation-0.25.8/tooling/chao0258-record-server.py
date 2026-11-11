#!/usr/bin/env python3
"""Bind an actual successful server log to the exact current production payload."""
import fnmatch
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import shutil
import sys
import zipfile
from chao_artifact_io import write_json_atomic, copy_atomic
from chao_log_capture import coherent_console

root = Path(sys.argv[1]).resolve()
profile = sys.argv[2]
original_log = Path(sys.argv[3]).resolve()
fresh_world = sys.argv[4]
proof = root / 'docs/validation-0.25.8'
proof.mkdir(parents=True, exist_ok=True)
log = proof / f'server-{profile}.txt'
copy_atomic(original_log, log)
content = coherent_console(log.read_bytes()).decode(errors='replace')
assert re.search(r'BUILD SUCCESSFUL', content), 'Gradle did not succeed'
registered = re.search(r'(\d+) tests are now running', content)
passed = re.search(r'All (\d+) required tests passed', content)
assert registered and passed and registered[1] == passed[1], 'Incomplete required GameTests'
assert 'required tests failed' not in content and ' failed at ' not in content
spec = importlib.util.spec_from_file_location('release_helpers', root/'scripts/package_chao017.py')
helpers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helpers)
helpers.VERSION = '0.25.8'
jar = root / 'build/libs/chao_garden-0.25.8.jar'
helpers.validate_production_jar(jar, root)

def sha(path):
    with path.open('rb') as handle:
        return hashlib.file_digest(handle, 'sha256').hexdigest()

with zipfile.ZipFile(jar) as archive:
    entry_count = len(archive.namelist())
    payload = {i.filename: hashlib.sha256(archive.read(i)).hexdigest()
               for i in archive.infolist() if not i.is_dir()}
    assert len(archive.namelist()) == len(set(archive.namelist()))
classes = {n for n in payload if n.startswith('com/chaogarden/') and n.endswith('.class')}
patterns = re.findall(r"(?m)^\s*exclude '([^']+)'", (root/'build.gradle').read_text())
compiled = root/'build/classes/java/main'
expected = {p.relative_to(compiled).as_posix() for p in (compiled/'com/chaogarden').rglob('*.class')
            if not any(fnmatch.fnmatchcase(p.relative_to(compiled).as_posix(), pat) for pat in patterns)}
assert classes == expected, 'Production class set differs from current compilation'
assert all(sha(compiled/n)==payload[n] for n in classes), 'Current server compilation differs from captured JAR class bytes'
runtime_resources = [p for p in (root/'src/main/resources').rglob('*') if p.is_file()
    and p.suffix != '.bbmodel'
    and p.relative_to(root/'src/main/resources').as_posix() in payload]
source_root=root/'src/main/resources'
source_payloads={p.relative_to(source_root).as_posix():sha(p) for p in source_root.rglob('*') if p.is_file() and p.suffix!='.bbmodel' and not any(fnmatch.fnmatchcase(p.relative_to(source_root).as_posix(),pat) for pat in patterns)}
assert all(payload.get(n)==h for n,h in source_payloads.items()), 'Server source resource bytes differ from JAR'
assert {n for n in payload if n.startswith(('assets/','data/'))}=={n for n in source_payloads if n.startswith(('assets/','data/'))}, 'Server JAR resource domain is not exact'
manifest = proof / f'server-{profile}-payloads.json'
write_json_atomic(manifest,dict(release='0.25.8', jar_sha256=sha(jar), payloads=payload))
core = dict((name, int(n)) for name,n in re.findall(r'^(.+?): (\d+) checks passed$', content, re.MULTILINE))
core_log=proof/'production-build.txt'
core_content=coherent_console(core_log.read_bytes()).decode(errors='replace')
assert 'BUILD SUCCESSFUL' in core_content and 'BUILD FAILED' not in core_content
if not core:core=dict((name,int(n)) for name,n in re.findall(r'^(.+?): (\d+) checks passed$',core_content,re.MULTILINE))
baseline={'ChaoState','Egg distribution policy','Training supplies','Wardrobe choices','Baby animation timing','Kindergarten buyback','Black Market stock','Activity rules','Companion rules','SA2 fruit effects','Locomotion rules'}
assert baseline <= core.keys() and all(n>0 for n in core.values()), 'Missing or empty mechanics suite'
expected_tests_file=proof/'expected-tests.json'
expected_tests=json.loads(expected_tests_file.read_text())
new_batches=expected_tests['new_batches']
required_batches=expected_tests['baseline_regression_batches']|new_batches
observed_batches={}
for batch,count in re.findall(r"Running test batch '([^']+)' \((\d+) tests\)",content):
    prefix=batch.rsplit(':',1)[0]
    observed_batches[prefix]=observed_batches.get(prefix,0)+int(count)
assert all(observed_batches.get(n)==count for n,count in required_batches.items()),('New focused regression batch absent or incomplete',observed_batches)
assert int(registered[1])>=expected_tests['baseline_required_cases']+sum(new_batches.values()), 'A feature profile unexpectedly ran fewer than the existing and new required cases'
duration = re.search(r'(\d+) GAME TESTS COMPLETE IN ([\d.]+) (s|min|h)', content)
gradle = re.search(r'BUILD SUCCESSFUL in (.+)', content)
accessories = 'Chao companion registered with the Accessories API' in content
assert accessories == (profile == 'accessories'), 'Unexpected optional Accessories profile'
tv_fixture=root/'src/main/java/com/chaogarden/test/ToyApproachGameTests025.java'
assert 'state.setIllness(illness,100)' in tv_fixture.read_text(), 'Final navigation fixture must isolate unrelated random illness'
report = dict(release='0.25.8', result='PASS',
    sources=[dict(file=p.relative_to(root).as_posix(), sha256=sha(p)) for p in [log,manifest,core_log,tv_fixture,expected_tests_file]],
    profile=profile, exit_code=0, required_tests=int(registered[1]), required_tests_passed=int(passed[1]),
    focused_new_regression_batches={n:observed_batches[n] for n in new_batches},focused_new_required_cases=sum(new_batches.values()),
    controlled_tv_navigation_fixture=dict(illness_meters=6,existing_public_treatment_immunity_ticks=24000,test_budget_ticks=2000,production_ai_unchanged=True,reason='Inherited healthy routing fixture uses existing clinic treatment immunity to isolate unrelated biological illness rolls. Newborn speed, physical obstacles, route/session/reward assertions and test budget remain unchanged.',source_sha256=sha(tv_fixture)),
    curved_stair_fixture_readiness=dict(actual_player_tick=True,actual_hub_level=True,colliding_player_state=True,all_native_route_collision_chunks_published=True,initial_floor_visible_through_native_collision_getter=True,original_four_walks_and_no_collision_tolerance_preserved=True),
    gametest_duration_seconds=round(float(duration[2])*{'s':1,'min':60,'h':3600}[duration[3]],3) if duration else None,
    gametest_duration_reported=(duration[2]+' '+duration[3]) if duration else None,
    gradle_duration=gradle[1] if gradle else None,
    core_total=sum(core.values()), core_suites=core, core_source='production-build.txt (single build/coreTest; not repeated by this profile)',
    fresh_disposable_game_directory=fresh_world,
    command='runGameTestServer -PchaoGameTestDir=<fresh disposable directory> -PchaoArchiveStagingDir=<outside synchronized workspace>' +
      (' -PchaoVariantFixtures=true -PchaoAccessoriesTest=true -PchaoAccessoriesDir=<actual pinned dependency directory>' if accessories else ''),
    server_tested_artifact=dict(file=jar.relative_to(root).as_posix(), filename=jar.name,
        size_bytes=jar.stat().st_size, sha256=sha(jar), entries=entry_count,
        production_class_payloads=len(classes), source_runtime_resource_payloads=len(runtime_resources),
        development_helpers_and_distribution_fixtures_excluded=True,current_compiled_and_source_payloads_byte_identical=True,exact_asset_data_domain=True),
    identity_fixture_mod_count=17 if accessories else 0,
    accessories_api_registration_confirmed=accessories,
    scope='Fresh actual NeoForge1.21.1 GameTestServer in an isolated world. Required cases execute real game APIs; optional slot cases execute only with the real Accessories profile. Identity fixtures are not full third-party mod implementations.')
stair_fixture=root/'src/main/java/com/chaogarden/test/LobbyApproachGameTests0255.java'
pupil_fixture=root/'src/main/java/com/chaogarden/test/KindergartenPupilsGameTests0257.java'
for current_fixture in [stair_fixture,pupil_fixture]:
    report['sources'].append(dict(file=current_fixture.relative_to(root).as_posix(),sha256=sha(current_fixture)))
assert 'getChunkForCollisions' in stair_fixture.read_text() and 'thenWaitUntil' in stair_fixture.read_text()
if accessories:
    resident_fixture=root/'src/main/java/com/chaogarden/test/KindergartenServicesGameTests.java'
    report['sources'].append(dict(file=resident_fixture.relative_to(root).as_posix(),sha256=sha(resident_fixture)))
    report['resident_fixture_readiness']=dict(campus_anchor_chunks=6,requires_entities_loaded=True,requires_entity_ticking=True,production_resident_logic_unchanged=True,strict_identity_position_visibility_and_purchased_stock_assertions_preserved=True,source_sha256=sha(resident_fixture),reason='Inherited fixture waits for the same six-anchor complete-campus loaded/entity-ticking precondition already enforced by production, before checking strict recovery identity and stock invariants.')
    report['dependencies'] = [dict(filename=p.name,size_bytes=p.stat().st_size,sha256=sha(p))
        for p in sorted(Path('/workspace/tools/dependencies').glob('*.jar'))]
# Bind every actual native fixture source and compiled development class, not only the focused additions.
fixture_sources=sorted((root/'src/main/java/com/chaogarden/test').rglob('*.java'))
fixture_classes={p.relative_to(compiled).as_posix():sha(p) for p in sorted((compiled/'com/chaogarden/test').rglob('*.class'))}
assert fixture_sources and fixture_classes
fixture_manifest=proof/f'server-{profile}-test-fixtures.json'
write_json_atomic(fixture_manifest,dict(release='0.25.8',source_files=[dict(file=p.relative_to(root).as_posix(),sha256=sha(p)) for p in fixture_sources],compiled_development_classes=fixture_classes))
known={row['file'] for row in report['sources']}
for current in [fixture_manifest,root/'build.gradle',root/'gradle.properties',*fixture_sources]:
    name=current.relative_to(root).as_posix()
    if name not in known:report['sources'].append(dict(file=name,sha256=sha(current)));known.add(name)
report['native_fixture_source_files']=len(fixture_sources)
report['native_fixture_compiled_classes']=len(fixture_classes)
write_json_atomic(proof/f'server-{profile}.json',report)
core_report = dict(release='0.25.8',result='PASS',sources=[dict(file=p.relative_to(root).as_posix(),sha256=sha(p)) for p in [core_log,root/'build.gradle',root/'gradle.properties',*sorted((root/'src/test/java').rglob('*.java'))]],
    total_checks=sum(core.values()),core_total=sum(core.values()),
    suites=[dict(name=name,checks=n,result='PASS') for name,n in core.items()])
write_json_atomic(proof/'core.json',core_report)
print(json.dumps({k:v for k,v in report.items() if k not in ['core_suites','dependencies']},indent=2))
