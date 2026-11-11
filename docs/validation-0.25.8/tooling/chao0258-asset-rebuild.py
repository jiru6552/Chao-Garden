#!/usr/bin/env python3
"""Run the complete resource pipeline in an isolated source copy and compare bytes."""
import hashlib
import ast
import fnmatch
import re
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time
from chao_artifact_io import write_json_atomic,copy_atomic

root = Path(sys.argv[1]).resolve()
proof = root / 'docs/validation-0.25.8'
proof.mkdir(parents=True, exist_ok=True)
log = proof / 'asset-rebuild.txt'
live=Path('/workspace/tools/chao0258-logs');live.mkdir(parents=True,exist_ok=True)
log_prefix=sys.argv[2] if len(sys.argv)>2 else 'asset-rebuild'
assert re.fullmatch(r'[a-z0-9-]+',log_prefix)
stage=live/(log_prefix+'.txt.working')
assert not stage.exists()
closed=live/(log_prefix+'.txt')
assert not closed.exists()

def sha(path):
    with path.open('rb') as handle:
        return hashlib.file_digest(handle, 'sha256').hexdigest()

def snapshot(folder):
    return {p.relative_to(folder).as_posix(): sha(p)
            for p in sorted(folder.rglob('*')) if p.is_file()}

def tree(values):
    payload = ''.join(f'{key}\0{values[key]}\n' for key in sorted(values))
    return hashlib.sha256(payload.encode()).hexdigest()

spec=importlib.util.spec_from_file_location('asset_pipeline_definition',root/'scripts/generate_all_assets.py')
pipeline=importlib.util.module_from_spec(spec);spec.loader.exec_module(pipeline)
pipeline_sources={root/'scripts/generate_all_assets.py'} | {root/(step if isinstance(step,str) else step[0]) for step in pipeline.STEPS} | set((root/'art').glob('*.py')) | {root/'src/main/java/com/chaogarden'/n for n in ('hub/LobbyDecor025.java','kindergarten/KindergartenDecor025.java','client/LobbySceneRenderer025.java')}
# Writers import local helper modules (including the eight-part transporter
# writer). Bind those transitive inputs without freezing unrelated packaging
# scripts that do not participate in the resource pipeline.
pending=[p for p in pipeline_sources if p.suffix=='.py'];seen=set()
while pending:
    source=pending.pop()
    if source in seen:continue
    seen.add(source)
    for node in ast.walk(ast.parse(source.read_text())):
        modules=([a.name for a in node.names] if isinstance(node,ast.Import)
                 else [node.module] if isinstance(node,ast.ImportFrom) and node.module else [])
        for module in modules:
            relative=Path(*module.split('.')).with_suffix('.py')
            for parent in (source.parent,root/'scripts',root/'art',root):
                dependency=parent/relative
                if dependency.is_file() and dependency not in pipeline_sources:
                    pipeline_sources.add(dependency);pending.append(dependency)
# Validators now read portal placement/trigger/door Java declarations as well
# as the two original block registries. Bind a conservative complete production
# Java input set; excluded test/capture helpers cannot invalidate this resource gate.
patterns=re.findall(r"(?m)^\s*exclude '([^']+)'",(root/'build.gradle').read_text())
main_java=root/'src/main/java'
migration_only=set()
pipeline_sources |= {p for p in main_java.rglob('*.java') if p.relative_to(main_java).as_posix() not in migration_only and not any(fnmatch.fnmatchcase(p.relative_to(main_java).with_suffix('.class').as_posix(),pat) for pat in patterns)}
def gameplay_callers(folder):
 result={}
 for source in sorted((folder/'src/main/java').rglob('*.java')):
  args=re.findall(r'\.animateGameplay\(\s*([^,]+),',source.read_text())
  if args:result[source.relative_to(folder).as_posix()]=args
 return result
callers_before=gameplay_callers(root)
pipeline_sources |= {root/'build.gradle',root/'gradle.properties'}
pipeline_source_before={p.relative_to(root).as_posix():sha(p) for p in sorted(pipeline_sources)}
baseline = snapshot(root / 'src/main/resources')
test_fixtures = snapshot(root / 'src/gameTestDimensions/resources')
raw = snapshot(root / 'art/imported_chao')
prototype = sha(root / 'art/legacy_runtime/chao_mesh.json')
started = time.time_ns()
temp = Path(tempfile.mkdtemp(prefix='chao0258-clean-assets-'))
copy = temp / 'source'
shutil.copytree(root, copy, ignore=shutil.ignore_patterns(
    'build', 'run', 'run-*', '.gradle', '__pycache__', '*.pyc'))
with stage.open('wb') as output:
    result = subprocess.run([sys.executable, 'scripts/generate_all_assets.py'],
                            cwd=copy, stdout=output, stderr=subprocess.STDOUT)
    output.flush()
    os.fsync(output.fileno())
os.replace(stage,closed)
copy_atomic(closed,log)
rebuilt = snapshot(copy / 'src/main/resources')
rebuilt_fixtures = snapshot(copy / 'src/gameTestDimensions/resources')
missing = sorted(baseline.keys() - rebuilt.keys())
extra = sorted(rebuilt.keys() - baseline.keys())
changed = sorted(k for k in baseline.keys() & rebuilt.keys() if baseline[k] != rebuilt[k])
raw_ok = raw == snapshot(copy / 'art/imported_chao') == snapshot(root / 'art/imported_chao')
proto_ok = prototype == sha(copy / 'art/legacy_runtime/chao_mesh.json') == sha(root / 'art/legacy_runtime/chao_mesh.json')
live_ok = baseline == snapshot(root / 'src/main/resources')
fixture_ok = test_fixtures == rebuilt_fixtures == snapshot(root / 'src/gameTestDimensions/resources')
separate = len(test_fixtures) >= 1 and 'data/minecraft/worldgen/world_preset/flat.json' in test_fixtures \
    and 'data/minecraft/worldgen/world_preset/flat.json' not in baseline
pipeline_source_ok=pipeline_source_before=={p.relative_to(root).as_posix():sha(p) for p in sorted(pipeline_sources)}
generated_java_names={p.relative_to(root).as_posix() for p in pipeline_sources if p.suffix=='.java'}
generated_java_changed=sorted(n for n in generated_java_names if not (copy/n).is_file() or sha(copy/n)!=pipeline_source_before[n])
generated_java_ok=not generated_java_changed
callers_ok=callers_before==gameplay_callers(copy)==gameplay_callers(root)
passed = callers_ok and pipeline_source_ok and generated_java_ok and result.returncode == 0 and not missing and not extra and not changed and raw_ok and proto_ok and live_ok and fixture_ok and separate
finished = time.time_ns()
report = dict(release='0.25.8', result='PASS' if passed else 'FAIL',
    sources=[dict(file=log.relative_to(root).as_posix(), sha256=sha(log))]+[dict(file=n,sha256=h) for n,h in sorted(pipeline_source_before.items())],
    frozen_pipeline_source_files=len(pipeline_source_before), frozen_pipeline_source_tree_sha256=tree(pipeline_source_before), frozen_pipeline_source_unchanged=pipeline_source_ok,
    generated_production_java_matches_live=generated_java_ok,generated_production_java_changes=generated_java_changed,
    pipeline_exit=result.returncode, started_ns=started, finished_ns=finished,
    elapsed_seconds=round((finished-started)/1e9, 3), clean_source_root=str(copy),
    baseline_resources=len(baseline), rebuilt_resources=len(rebuilt),
    resource_tree_sha256_before=tree(baseline), resource_tree_sha256_rebuilt=tree(rebuilt),
    missing_resources=missing, extra_resources=extra, changed_resources=changed,
    separated_gametest_resources=len(test_fixtures), separated_gametest_resources_unchanged=fixture_ok,
    gametest_fixture_tree_sha256=tree(test_fixtures), dev_fixture_absent_from_main=separate,
    raw_input_count=len(raw), raw_input_sha256_unchanged=raw_ok, raw_input_tree_sha256=tree(raw),
    archived_prototype_sha256_unchanged=proto_ok, archived_prototype_sha256=prototype,
    live_resource_tree_unchanged=live_ok, python=sys.version.split()[0],
    source_binding_policy='All pipeline/art Python, all production Java and build/version inputs frozen, including current compact-garden geometry and migration changes. Excluded test/native capture helper sources stay outside the runtime resource domain; gameplay caller projection is separately frozen.', migration_only_source_exceptions=sorted(migration_only), gameplay_caller_projection_unchanged=callers_ok, gameplay_caller_projection=callers_before,
    scope='One full asset CLI in a fresh disposable source copy; all main resource bytes and the physically separate GameTest fixture compared to frozen live source. No live resource write or Gradle execution.')
write_json_atomic(proof/'asset-rebuild.json',report)
print(json.dumps(report, indent=2), flush=True)
raise SystemExit(0 if passed else 1)
