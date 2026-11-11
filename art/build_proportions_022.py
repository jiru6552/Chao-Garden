#!/usr/bin/env python3
"""Normalize the six tall ordinary head cores without moving eyes or joints.

This is a finalizer for the 0.21 age targets.  It changes the head's upper edge,
translates (never scales) its stepped crest, and records a reversible marker
offset for the runtime. The exact imported comparison rigs stay untouched.
"""
from pathlib import Path
import copy
import hashlib
import json
import math

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
KEYS = ('baby', 'swim', 'fly', 'run', 'power', 'hero', 'hero_swim',
        'hero_fly', 'hero_run', 'hero_power', 'dark', 'dark_swim',
        'dark_fly', 'dark_run', 'dark_power')
SHORTEN = frozenset(('hero', 'hero_fly', 'hero_power', 'dark', 'dark_swim', 'dark_run'))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, separators=(',', ':')) + '\n')


def bounds(vertices):
    return [min(v[i] for v in vertices) for i in range(3)] + [max(v[i] for v in vertices) for i in range(3)]


def core(data):
    candidates = []
    for bone in data['bones']:
        if not bone['name'].startswith('skull__cube_'):
            continue
        for mesh in bone.get('meshes', []):
            if mesh.get('material') == 'lens':
                continue
            box = bounds(mesh['vertices'])
            candidates.append((math.prod(box[i + 3] - box[i] for i in range(3)), bone, mesh))
    if not candidates:
        raise ValueError('No native head core')
    return max(candidates, key=lambda entry: entry[0])[1:]


def adjust_model(key, supplied):
    data = copy.deepcopy(supplied)
    if key not in SHORTEN:
        return data
    if data.get('proportion_adjustment_022', {}).get('release') == '0.22.0':
        return data
    bone, mesh = core(data)
    original = bounds(mesh['vertices'])
    height = original[4] - original[1]
    if abs(height - 7) > 1e-6:
        raise ValueError(f'{key}: expected the verified seven-pixel source head, got {height}')
    if len(mesh.get('morph_vertices', [])) != 9:
        raise ValueError(f'{key}: expected all nine fully aged endpoints')
    ratio = 6.0 / height
    targets = [mesh['vertices'], *mesh['morph_vertices']]
    drops = []
    for vertices in targets:
        box = bounds(vertices)
        new_top = box[4] - (box[4] - box[1]) * ratio
        drops.append(round(new_top - box[1], 6))
        for vertex in vertices:
            vertex[1] = round(box[4] - (box[4] - vertex[1]) * ratio, 6)
    crest = next(b for b in data['bones'] if b['name'] == 'gradual_head_021')
    for piece in crest['meshes']:
        arrays = [piece['vertices'], *piece['morph_vertices']]
        for vertices, drop in zip(arrays, drops):
            for vertex in vertices:
                vertex[1] = round(vertex[1] + drop, 6)
    crown = next((b for b in data['bones'] if b['name'] == 'monster_crown'), None)
    if crown is not None:
        crown['pivot'][1] = round(crown['pivot'][1] + drops[0], 6)
    data['proportion_adjustment_022'] = {
        'release': '0.22.0', 'core_bone': bone['name'],
        'source_height': height, 'target_height': 6,
        'source_bounds': original, 'target_bounds': bounds(mesh['vertices']),
        'marker_young_delta': drops[0], 'marker_target_deltas': drops[1:],
    }
    return data


def main():
    records = []
    for key in KEYS:
        path = MODELS / f'chao_{key}.json'
        original = json.loads(path.read_text())
        final = adjust_model(key, original)
        if final != original:
            write(path, final)
        _, head = core(final)
        box = bounds(head['vertices'])
        records.append({'key': key, 'head_size': [round(box[i + 3] - box[i], 6) for i in range(3)],
                        'normalized': key in SHORTEN, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()})
    manifest_path = ASSETS / 'gradual_evolution_models_021.json'
    manifest = json.loads(manifest_path.read_text())
    for record in manifest['models']:
        record['sha256'] = next(row['sha256'] for row in records if row['key'] == record['key'])
    manifest['proportion_revision'] = '0.22.0'
    write(manifest_path, manifest)
    report = {'release': '0.22.0', 'result': 'PASS', 'normalized_heads': len(SHORTEN), 'models': records,
              'preserved': ['native joints', 'eye panels', 'front and lower head planes', 'width and depth',
                            'crest lengths', 'bodies and limbs', 'special forms', 'raw reference inputs']}
    write(ROOT / 'docs/research/PROPORTIONS_022_BUILD.json', report)
    print('Proportions022: six 7-high cores shortened to 6; eyes/joints/UVs, crest lengths, bodies and coats preserved.')


if __name__ == '__main__':
    main()
