"""Source-only JSON reference/count audit; optional original JAR comparison."""
from pathlib import Path
import json
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
RESOURCES = ROOT / 'src/main/resources'
ASSETS = RESOURCES / 'assets/chao_garden'
errors = []
references = 0

def walk(value, source, key=None):
    global references
    if isinstance(value, dict):
        for name, child in value.items():
            if name == 'textures' and isinstance(child, dict):
                for texture in child.values():
                    if isinstance(texture, str) and texture.startswith('chao_garden:'):
                        references += 1
                        if not (ASSETS / 'textures' / (texture.split(':', 1)[1] + '.png')).exists():
                            errors.append([source, 'texture ' + texture])
            else:
                walk(child, source, name)
    elif isinstance(value, list):
        for child in value:
            walk(child, source, key)
    elif isinstance(value, str) and key in ('model', 'parent') and value.startswith('chao_garden:'):
        references += 1
        path = value.split(':', 1)[1]
        target = ASSETS / path if path.endswith('.obj') else ASSETS / 'models' / (path + '.json')
        if not target.exists():
            errors.append([source, key + ' ' + value])

for path in ASSETS.rglob('*.json'):
    walk(json.loads(path.read_text()), str(path.relative_to(ASSETS)))
files = {p.relative_to(RESOURCES).as_posix(): p for p in RESOURCES.rglob('*') if p.is_file()}
report = {'resources': len(files), 'checked_json_refs': references, 'reference_errors': errors}
if len(sys.argv) > 1:
    with zipfile.ZipFile(sys.argv[1]) as jar:
        prior = {n for n in jar.namelist() if n.startswith(('assets/', 'data/')) and not n.endswith('/')}
        current = {n for n in files if n.startswith(('assets/', 'data/'))}
        report.update(added_vs_original_jar=sorted(current-prior), removed_vs_original_jar=sorted(prior-current),
                      changed_vs_original_jar=sorted(n for n in current & prior if files[n].read_bytes() != jar.read(n)))
print(json.dumps(report, indent=2))
raise SystemExit(bool(errors))
