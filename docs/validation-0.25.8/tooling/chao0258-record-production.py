#!/usr/bin/env python3
"""Verify the final release JAR against current compilation, resources and both server-tested payloads."""
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

root = Path(sys.argv[1]).resolve()
original = Path(sys.argv[2]).resolve()
proof = root/'docs/validation-0.25.8'
log = proof/'production-build.txt'
copy_atomic(original,log)
content = log.read_text(errors='replace')
assert 'BUILD SUCCESSFUL' in content
def sha(path):
    with path.open('rb') as handle:return hashlib.file_digest(handle,'sha256').hexdigest()
spec=importlib.util.spec_from_file_location('release_helpers',root/'scripts/package_chao017.py')
helpers=importlib.util.module_from_spec(spec);spec.loader.exec_module(helpers);helpers.VERSION='0.25.8'
jar=root/'build/libs/chao_garden-0.25.8.jar';helpers.validate_production_jar(jar,root)
patterns=re.findall(r"(?m)^\s*exclude '([^']+)'",(root/'build.gradle').read_text())
with zipfile.ZipFile(jar) as z:
    names=z.namelist();assert len(names)==len(set(names)) and z.testzip() is None
    payload={i.filename:hashlib.sha256(z.read(i)).hexdigest() for i in z.infolist() if not i.is_dir()}
classes={n for n in payload if n.startswith('com/chaogarden/') and n.endswith('.class')}
compiled=root/'build/classes/java/main'
expected={p.relative_to(compiled).as_posix() for p in (compiled/'com/chaogarden').rglob('*.class')
          if not any(fnmatch.fnmatchcase(p.relative_to(compiled).as_posix(),pat) for pat in patterns)}
assert classes==expected
class_mismatches=[n for n in classes if sha(compiled/n)!=payload[n]];assert not class_mismatches
resources=root/'src/main/resources';excluded=[];resource_mismatches=[];resource_count=0;expected_asset_data=set()
for p in resources.rglob('*'):
    if not p.is_file():continue
    n=p.relative_to(resources).as_posix()
    if p.suffix=='.bbmodel' or any(fnmatch.fnmatchcase(n,pat) for pat in patterns):excluded.append(n);continue
    resource_count+=1
    if n.startswith(('assets/','data/')):expected_asset_data.add(n)
    if payload.get(n)!=sha(p):resource_mismatches.append(n)
assert not resource_mismatches,resource_mismatches
actual_asset_data={n for n in payload if n.startswith(('assets/','data/'))}
extra_asset_data=sorted(actual_asset_data-expected_asset_data);missing_asset_data=sorted(expected_asset_data-actual_asset_data)
assert not extra_asset_data and not missing_asset_data,(extra_asset_data,missing_asset_data)
comparisons={}
sources=[log]
client_smoke=proof/'client-smoke.json'
if client_smoke.is_file():
    client=json.loads(client_smoke.read_text())
    assert client['result']=='PASS' and client['production_jar_sha256']==sha(jar) and client['production_payloads_byte_identical']
    sources.append(client_smoke)
assemble = None
if len(sys.argv) > 3:
    assemble = proof/'production-assemble.txt'
    copy_atomic(Path(sys.argv[3]).resolve(),assemble)
    assert 'BUILD SUCCESSFUL' in assemble.read_text(errors='replace')
    sources.append(assemble)
for profile in ['default','accessories']:
    manifest=proof/f'server-{profile}-payloads.json';tested=json.loads(manifest.read_text());sources.append(manifest)
    old=tested['payloads']
    missing=sorted(old.keys()-payload.keys());extra=sorted(payload.keys()-old.keys())
    changed=sorted(n for n in old.keys()&payload.keys() if old[n]!=payload[n])
    comparisons[profile]=dict(server_tested_jar_sha256=tested['jar_sha256'],
        identical_jar=sha(jar)==tested['jar_sha256'],missing=missing,extra=extra,changed=changed)
    assert not missing and not extra and not changed and comparisons[profile]['identical_jar'],comparisons[profile]
core=dict((name,int(n)) for name,n in re.findall(r'^(.+?): (\d+) checks passed$',content,re.MULTILINE))
baseline={'ChaoState','Egg distribution policy','Training supplies','Wardrobe choices','Baby animation timing','Kindergarten buyback','Black Market stock','Activity rules','Companion rules','SA2 fruit effects','Locomotion rules'}
assert baseline<=core.keys() and all(n>0 for n in core.values())
report=dict(release='0.25.8',result='PASS',sources=[dict(file=p.relative_to(root).as_posix(),sha256=sha(p)) for p in sources],
    build_exit=0,build_duration=re.search(r'BUILD SUCCESSFUL in (.+)',content)[1],
    artifact=dict(file=jar.relative_to(root).as_posix(),size_bytes=jar.stat().st_size,sha256=sha(jar),entries=len(names)),
    all_server_tested_payloads_identical=True,server_profile_comparisons=comparisons,
    production_class_payloads=len(classes),compiled_class_mismatches=class_mismatches,
    source_runtime_resource_payloads=resource_count,source_resource_mismatches=resource_mismatches,
    source_asset_data_payloads=len(expected_asset_data),jar_asset_data_payloads=len(actual_asset_data),extra_asset_data_payloads=extra_asset_data,missing_asset_data_payloads=missing_asset_data,exact_current_asset_data_domain=True,
    excluded_fixture_source_files=sorted(excluded),forbidden_development_entries=[],
    crc='PASS',unique_entry_names=True,metadata_version='0.25.8',core_checks=sum(core.values()),
    scope='Final production build/coreTest plus current class/resource and both fresh isolated-world server profiles. Every final payload and the complete JAR hash equal both successful server-tested artifacts. Study helpers, test-only fixture refinements and the dedicated GameTest preset are excluded.')
if assemble is not None:
    report['post_study_assemble_exit']=0
    report['post_study_assemble_duration']=re.search(r'BUILD SUCCESSFUL in (.+)',assemble.read_text(errors='replace'))[1]
write_json_atomic(proof/'production-jar.json',report)
write_json_atomic(proof/'core.json',dict(release='0.25.8',result='PASS',
    sources=[dict(file=p.relative_to(root).as_posix(),sha256=sha(p)) for p in [log,root/'build.gradle',root/'gradle.properties',*sorted((root/'src/test/java').rglob('*.java'))]],
    total_checks=sum(core.values()),core_total=sum(core.values()),
    suites=[dict(name=k,checks=v,result='PASS') for k,v in core.items()]))
print(json.dumps({k:v for k,v in report.items() if k not in ['excluded_fixture_source_files','server_profile_comparisons']},indent=2))
