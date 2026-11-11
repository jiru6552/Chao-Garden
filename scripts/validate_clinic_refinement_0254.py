#!/usr/bin/env python3
"""Independent native mesh/UV checks for the reported Clinic defects."""
from copy import deepcopy
from pathlib import Path
import hashlib
import json
import subprocess
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
AS = ROOT / 'src/main/resources/assets/chao_garden'
AXES = {'west': (0, 0, 2, 1), 'east': (0, 1, 2, 1),
        'down': (1, 0, 0, 2), 'up': (1, 1, 0, 2),
        'north': (2, 0, 0, 1), 'south': (2, 1, 0, 1)}


def read(name):
    return json.loads((AS / name).read_text())


def check_bed(doc):
    faces = []
    for e in doc['elements']:
        a, b = e['from'], e['to']
        assert all(-16 <= x <= 32 for x in a + b), 'Illegal native element bounds'
        assert all(a[i] < b[i] for i in range(3)), 'Degenerate bed solid'
        for face, data in e['faces'].items():
            axis, high, u, v = AXES[face]
            assert 'cullface' not in data, 'Exposed bed surface acquired world-block culling'
            faces.append((axis, b[axis] if high else a[axis], a[u], b[u], a[v], b[v]))
    for i, face in enumerate(faces):
        axis, plane, u0, u1, v0, v1 = face
        for other in faces[i+1:]:
            if axis != other[0] or abs(plane-other[1]) > 1e-8:
                continue
            assert min(u1, other[3])-max(u0, other[2]) <= 1e-8 or min(v1, other[5])-max(v0, other[4]) <= 1e-8, 'Bed has overlapping coplanar surfaces (post/rail or pillow/quilt z-fighting)'
    bounds = [[min(e['from'][i] for e in doc['elements']), max(e['to'][i] for e in doc['elements'])] for i in range(3)]
    assert bounds == [[0, 16], [0, 15], [0, 32]], 'Bed native bounds changed independently of its real collision'
    quilts = [e for e in doc['elements'] if e['faces'].get('up', {}).get('texture') == '#quilt']
    assert len(quilts) == 1 and quilts[0]['from'] == [1, 8, 7] and quilts[0]['to'] == [15, 10, 30], 'Raised pillow and quilt overlap or the top cloth was broken into repeated tiles'
    assert quilts[0]['faces']['up']['uv'] == [0, 0, 16, 16], 'Physical quilt no longer spans its artwork exactly once'
    assert all(data['texture'] != '#quilt' for e in doc['elements'] for f, data in e['faces'].items() if f != 'up'), 'Pink quilt side edges repeat the full top grid'
    with Image.open(AS / 'textures/block/school025/pink_quilt_top.png') as im:
        assert im.size == (224, 368) and im.width/im.height == 14/23, 'Quilt cells stretch because texture and actual bed cloth disagree'
    return len(faces)


def check_curtain():
    states = read('blockstates/school_green_room_curtain_tie.json')['variants']
    assert states['facing=west'] == {'model': 'chao_garden:block/school_green_room_curtain_tie', 'y': 90}, 'Original left tie was changed'
    state = states['facing=east']
    assert state.get('y') == 270 and state['model'].endswith('_east'), 'Right curtain uses only the old backward-facing model'
    doc = read('models/' + state['model'].replace('chao_garden:', '') + '.json')
    gold = [e for e in doc['elements'] if e['from'][1] == 4 and e['to'][1] == 7]
    assert len(gold) == 1 and all(f['texture'] == '#gold' for f in gold[0]['faces'].values())
    a, b = gold[0]['from'], gold[0]['to']
    # Minecraft's270-degree turn is x'=z,z'=16-x. This independently checks
    # room side (west) and left side (north) after the actual blockstate turn.
    assert b[2] <= 5.5 and 16-a[0] < 8, 'Right tassel is buried behind the cloth or hangs on the right'
    original = read('models/block/school_green_room_curtain_tie.json')
    assert len(doc['elements']) == len(original['elements']) and doc['textures'] == original['textures'], 'Mirroring erased material or geometry'
    for source, mirror in zip(original['elements'], doc['elements']):
        assert source['from'][:2] == mirror['from'][:2] and source['to'][:2] == mirror['to'][:2]
        assert mirror['from'][2] == 16-source['to'][2] and mirror['to'][2] == 16-source['from'][2]
        assert mirror['faces']['north'] == source['faces']['south'] and mirror['faces']['south'] == source['faces']['north'], 'Reflection failed to preserve front/back normal ownership'


def main():
    bed = read('models/block/school_white_exam_bed.json')
    count = check_bed(bed)
    check_curtain()
    mutants = []
    duplicate = deepcopy(bed)
    duplicate['elements'].append(deepcopy(duplicate['elements'][0]))
    mutants.append(duplicate)
    old_corner = deepcopy(bed)
    old_corner['elements'].append({'from': [0, 4, 0], 'to': [16, 8, 32], 'faces': {'west': {'texture': '#green', 'uv': [0, 0, 16, 16]}}})
    mutants.append(old_corner)
    for mutant in mutants:
        try:
            check_bed(mutant)
        except AssertionError:
            pass
        else:
            raise AssertionError('Duplicate or original green/post corner skin mutant was accepted')
    files = [AS / 'models/block/school_white_exam_bed.json', AS / 'models/block/school_green_room_curtain_tie_east.json', AS / 'blockstates/school_green_room_curtain_tie.json', AS / 'textures/block/school025/pink_quilt_top.png']
    digest = lambda: [hashlib.sha256(p.read_bytes()).hexdigest() for p in files]
    before = digest()
    subprocess.run([sys.executable, str(ROOT / 'scripts/generate_clinic_refinement_0254.py')], check=True)
    assert before == digest(), 'Clinic postpass does not reproduce byte-identical native assets'
    print(f'Clinic0254: {count} exposed bed faces; zero overlapping coplanar surfaces; physical quilt UVs; left-facing visible tassel; {len(mutants)} overlapping-skin mutants rejected; deterministic postpass. PASS')


if __name__ == '__main__':
    main()
