#!/usr/bin/env python3
"""Verify complete current production payloads before fresh server profiles."""
from pathlib import Path
import fnmatch,hashlib,importlib.util,json,re,sys,zipfile
from chao_artifact_io import write_json_atomic

root=Path(sys.argv[1]).resolve();log=Path(sys.argv[2]).resolve()
proof=root/'docs/validation-0.25.8';jar=root/'build/libs/chao_garden-0.25.8.jar'
def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
def tree(rows):return hashlib.sha256(''.join(n+'\0'+rows[n]+'\n' for n in sorted(rows)).encode()).hexdigest()
assert 'BUILD SUCCESSFUL' in log.read_text(errors='replace')
patterns=re.findall(r"(?m)^\s*exclude '([^']+)'",(root/'build.gradle').read_text())
def included(name):return not any(fnmatch.fnmatchcase(name,p) for p in patterns)
compiled=root/'build/classes/java/main'
classes={p.relative_to(compiled).as_posix():sha(p) for p in (compiled/'com/chaogarden').rglob('*.class') if included(p.relative_to(compiled).as_posix())}
resources=root/'src/main/resources'
source={p.relative_to(resources).as_posix():sha(p) for p in resources.rglob('*') if p.is_file() and p.suffix!='.bbmodel' and included(p.relative_to(resources).as_posix())}
assert classes and source
expected=classes|source;metadata={'META-INF/MANIFEST.MF','META-INF/neoforge.mods.toml'}
with zipfile.ZipFile(jar) as bundle:
    names=bundle.namelist();assert len(names)==len(set(names)) and bundle.testzip() is None
    payload={i.filename:hashlib.sha256(bundle.read(i)).hexdigest() for i in bundle.infolist() if not i.is_dir()}
    assert set(payload)-metadata==set(expected),(sorted(set(payload)-metadata-set(expected)),sorted(set(expected)-set(payload)))
    changed=[n for n,h in expected.items() if payload[n]!=h]
    assert not changed,('Current compiled/source bytes differ from candidate',changed)
    assert 'data/minecraft/worldgen/world_preset/flat.json' not in payload
    assert not any('/test/' in n or 'VisualSmoke' in n or 'PoseProbe' in n for n in names)
    assert re.search(r'(?m)^version\s*=\s*"0\.25\.8"',bundle.read('META-INF/neoforge.mods.toml').decode())
manifest=proof/'current-production-payloads.json'
write_json_atomic(manifest,dict(release='0.25.8',result='PASS',payloads=expected,class_tree_sha256=tree(classes),resource_tree_sha256=tree(source)))
report=dict(release='0.25.8',result='PASS',sources=[dict(file=p.relative_to(root).as_posix(),sha256=sha(p)) for p in (log,manifest)],artifact=dict(file=jar.name,size_bytes=jar.stat().st_size,sha256=sha(jar),entries=len(names)),production_classes=len(classes),production_resources=len(source),asset_data_payloads=len([n for n in source if n.startswith(('assets/','data/'))]),extra_payloads=[],missing_payloads=[],crc='PASS',unique_names=True,metadata_only_exemptions=sorted(metadata),scope='Current compiled production classes and source runtime resources compared byte for byte against the complete release JAR. Development code and flat preset are excluded. This is artifact integrity, not a gameplay or visual claim.')
write_json_atomic(proof/'pre-server-jar.json',report)
print(json.dumps({k:v for k,v in report.items() if k!='sources'},indent=2))
