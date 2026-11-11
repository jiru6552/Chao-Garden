#!/usr/bin/env python3
"""Independent geometry/visibility audit of the restrained Chaos head refinements."""
from collections import deque
from pathlib import Path
import argparse
import copy
import hashlib
import itertools
import json
import math
import tempfile

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / 'src/main/resources/assets/chao_garden/models/entity/gameplay'
SOURCE = ROOT / 'src/main/resources/assets/chao_garden/models/entity/official/chao_baby.json'
EPS = 1e-7
CHECKS = 0

# Unchanged 0.20 anatomy: eyes, torso, hands, feet, wings, markers, reaction
# glyphs and every animation anchor. Head-appendage geometry is excluded.
APPROVED_BODY = {
    'light': '20cceb4c70850cdab8b16b317af263b26748ac129a540ad3c53406bb56dc0730',
    'angel': '769fb9e698d0db8956128b1317fd60635ef15428d6fcc9d19742252c9b11bfc6',
    'devil': 'a7de20667088638d69786c8bae6e0599b5c1dd71b1b8b114b1cfa61902d94e09',
}


def require(condition, message):
    global CHECKS
    CHECKS += 1
    if not condition:
        raise AssertionError(message)


def vertices(bone):
    return [v for m in bone['meshes'] for v in m['vertices']]


def extents(bones):
    vs = [v for b in bones for v in vertices(b)]
    return [[min(v[i] for v in vs) for i in range(3)],
            [max(v[i] for v in vs) for i in range(3)]]


def panels(bones):
    result = []
    for b in bones:
        for m in b['meshes']:
            for face in m['faces']:
                points = [m['vertices'][i] for i in face['v']]
                lo = [min(v[i] for v in points) for i in range(3)]
                hi = [max(v[i] for v in points) for i in range(3)]
                flat = [i for i in range(3) if abs(hi[i] - lo[i]) < EPS]
                require(len(flat) == 1, 'A head surface is degenerate or nonrectangular')
                result.append((flat[0], lo, hi))
    return result


def inside(point, faces):
    # Independent positive-X ray parity on the exported exterior rectangles.
    crossings = sum(axis == 0 and lo[0] > point[0] + EPS
                    and lo[1] < point[1] < hi[1]
                    and lo[2] < point[2] < hi[2]
                    for axis, lo, hi in faces)
    return crossings % 2 == 1


def connected_volume(name, pair, core):
    faces, core_faces = panels(pair), panels([core])
    vs = [v for b in [*pair, core] for v in vertices(b)]
    axes = [sorted({round(v[i], 7) for v in vs}) for i in range(3)]
    volume, roots = set(), set()
    for cell in itertools.product(*(range(len(a) - 1) for a in axes)):
        point = [(axes[i][cell[i]] + axes[i][cell[i] + 1]) / 2 for i in range(3)]
        if inside(point, faces):
            volume.add(cell)
            if inside(point, core_faces):
                roots.add(cell)
    require(bool(volume), name + ' has no closed positive volume')
    require(bool(roots), name + ' is detached from the actual skull')
    seen = {next(iter(volume))}
    queue = deque(seen)
    while queue:
        cell = queue.popleft()
        for axis in range(3):
            for sign in (-1, 1):
                p = list(cell)
                p[axis] += sign
                adjacent = tuple(p)
                if adjacent in volume and adjacent not in seen:
                    seen.add(adjacent)
                    queue.append(adjacent)
    require(seen == volume, name + ' has a detached step')
    return len(volume)


def hidden_set(bones, roots):
    result = set(roots)
    while True:
        grown = result | {name for name, b in bones.items() if b.get('parent') in result}
        if grown == result:
            return result
        result = grown


def main(write_report=True):
    global CHECKS
    CHECKS = 0
    source = {b['name']: b for b in json.loads(SOURCE.read_text())['bones']}
    report = {'version': '0.21.0', 'families': {}}
    for family in ['light', 'angel', 'devil']:
        opaque = json.loads((MODELS / f'chao_chaos_{family}.json').read_text())
        glass = json.loads((MODELS / f'chao_chaos_{family}_glass.json').read_text())
        bones = {b['name']: b for b in opaque['bones']}
        ice = {b['name']: b for b in glass['bones']}
        require(bones.keys() == ice.keys(), family + ' mismatched glass skeleton')
        for name, b in source.items():
            for field in ['parent', 'pivot', 'rotation']:
                require(bones[name][field] == b[field], family + ' changed native ' + name + '/' + field)
        protected = [[b for b in data['bones'] if not b['name'].startswith('chaos_crown')]
                     for data in [opaque, glass]]
        digest = hashlib.sha256(json.dumps(protected, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
        require(digest == APPROVED_BODY[family], family + ' changed approved non-head anatomy')
        hidden = hidden_set(bones, opaque['head_decorations'])
        require(set(opaque['head_decorations']) == set(glass['head_decorations']), family + ' mismatched cap roots')
        require(not ({'skull__cube_0', 'chaos_eye_-1', 'chaos_eye_1'} & hidden), family + ' cap hides skull/eyes')
        active = [name for name in bones if name.startswith('chaos_crown')]
        for name in active:
            require(name in hidden, name + ' escapes a closed hat')
            if vertices(ice[name]):
                require(name in hidden, name + ' glass escapes a closed hat')
                uvs = [uv for m in ice[name]['meshes'] for f in m['faces'] for uv in f['uv']]
                require(min(uv[0] for uv in uvs) == .5 and max(uv[0] for uv in uvs) == 63.5,
                        name + ' loses its continuous opacity gradient')
                # The translucent distal half must extend beyond the solid
                # skull, rather than revealing an opaque duplicate behind it.
                core_faces = panels([bones['skull__cube_0']])
                for m in ice[name]['meshes']:
                    for face in m['faces']:
                        for index, uv in zip(face['v'], face['uv']):
                            if uv[0] > 32:
                                p = m['vertices'][index]
                                require(not inside([p[0] + 1e-6, p[1] + 1e-6, p[2] + 1e-6], core_faces),
                                        name + ' distal glass has opaque skull backing')
        changed = [name for name in active if name != 'chaos_crown'
                   and not (family == 'angel' and name == 'chaos_crown_rear')]
        volumes = {name: connected_volume(name, [bones[name], ice[name]], bones['skull__cube_0'])
                   for name in changed}
        if family == 'light':
            side_bounds = {}
            for side in (-1, 1):
                name = 'chaos_crown_lobe_' + str(side)
                low, high = extents([bones[name], ice[name]])
                require(high[1] >= 1.4, 'Light side lock does not descend past the chin')
                require((low[2] + high[2]) / 2 < 1, 'Light side lock is still behind the head')
                require(low[2] < 0 and high[2] < 2.65, 'Light lock is not central in skull depth')
                # Resting upper arms/hands reach z=.65. Descending sections are
                # behind that plane; this is a rest-clearance check, not a claim
                # about every possible waving/animal/equipment animation.
                require(all(v[2] > .70 for b in [bones[name], ice[name]]
                            for v in vertices(b) if v[1] > -.1), 'Light lower lock overlaps resting arm plane')
                side_bounds[str(side)] = [low, high]
            report['families'][family] = {'central_downward_locks': side_bounds, 'connected_volume_cells': volumes}
        elif family == 'angel':
            low, high = extents([bones[n] for n in active] + [ice[n] for n in active])
            width = high[0] - low[0]
            require(1.08 <= width / 9.6 <= 1.15, 'Angel feeler width change is not slight')
            for side in (-1, 1):
                name = 'chaos_crown_horn_' + str(side)
                a, b = extents([bones[name], ice[name]])
                require(b[1] >= -.3, 'Angel feeler ends too high on the forehead')
                require(b[0] - a[0] >= 3.8, 'Angel feeler is still too narrow')
            report['families'][family] = {'head_appendage_width': width, 'width_ratio_to_020': width / 9.6, 'connected_volume_cells': volumes}
        else:
            upper, lower = [], []
            for side in (-1, 1):
                up, down = 'chaos_crown_upper_' + str(side), 'chaos_crown_lower_' + str(side)
                a, b = extents([bones[up], ice[up]])
                require(a[1] <= -9.5, 'Devil upper horn is not pronounced')
                require(max(abs(a[0]), abs(b[0])) >= 4.5, 'Devil upper horn lacks outward sweep')
                top = [abs(v[0]) for part in [bones[up], ice[up]] for v in vertices(part) if v[1] <= -9.39]
                require(max(top) < 4.5, 'Devil upper horn does not hook inward')
                c, d = extents([bones[down], ice[down]])
                require(d[1] >= 2.1, 'Devil lower horn does not descend')
                require(max(abs(c[0]), abs(d[0])) >= 4.7, 'Devil lower horn is not pronounced')
                upper.append([a, b]); lower.append([c, d])
            report['families'][family] = {'upper_hooked_horns': upper, 'lower_descending_horns': lower, 'connected_volume_cells': volumes}
        require(len([name for name in bones if name.startswith('chaos_eye_')]) == 2, family + ' duplicate eyes')
        for name in ['chaos_hand_arm_left', 'chaos_hand_arm_right', 'chaos_foot_foot_left', 'chaos_foot_foot_right']:
            require(name not in hidden and bool(vertices(ice[name])), family + ' hat hides or removes limb glass')
        for name in changed:
            solid = {tuple(sorted(tuple(v) for v in [m['vertices'][i] for i in f['v']]))
                     for m in bones[name]['meshes'] for f in m['faces']}
            transparent = {tuple(sorted(tuple(v) for v in [m['vertices'][i] for i in f['v']]))
                           for m in ice[name]['meshes'] for f in m['faces']}
            require(not solid & transparent, name + ' has coincident opaque/glass caps')
        for data in [opaque, glass]:
            for b in data['bones']:
                for mesh in b['meshes']:
                    require(all(math.isfinite(x) for v in mesh['vertices'] for x in v), b['name'] + ' nonfinite geometry')
                    require(all(0 <= u <= 64 and 0 <= v <= 32 for f in mesh['faces'] for u, v in f['uv']), b['name'] + ' UV outside texture')
    report['checks'] = CHECKS
    report['result'] = 'PASS'
    if write_report:
        path = ROOT / 'docs/research/CHAOS_021_INDEPENDENT_CHECKS.json'
        path.write_text(json.dumps(report, indent=2) + '\n')
        print(json.dumps(report, indent=2))
    return report


def rejected_corruptions():
    """Prove the independent audit rejects plausible asset mistakes off-tree."""
    global MODELS
    original = MODELS
    fixtures = {p.name: json.loads(p.read_text()) for p in original.glob('chao_chaos_*.json')}

    def shift_light(data):
        for suffix in ['', '_glass']:
            for b in data['chao_chaos_light' + suffix + '.json']['bones']:
                if b['name'] == 'chaos_crown_lobe_1':
                    for m in b['meshes']:
                        for v in m['vertices']:
                            v[2] += 4

    def widen_angel(data):
        for suffix in ['', '_glass']:
            for b in data['chao_chaos_angel' + suffix + '.json']['bones']:
                if b['name'].startswith('chaos_crown_horn_'):
                    for m in b['meshes']:
                        for v in m['vertices']:
                            v[0] *= 1.7

    def lose_cap_root(data):
        for suffix in ['', '_glass']:
            data['chao_chaos_devil' + suffix + '.json']['head_decorations'].remove('chaos_crown_upper_1')

    def move_eye(data):
        for b in data['chao_chaos_light.json']['bones']:
            if b['name'] == 'chaos_eye_1':
                for m in b['meshes']:
                    for v in m['vertices']:
                        v[0] += .5

    mutations = [('rear-shifted detached Light lock', shift_light),
                 ('exaggerated Angel feeler width', widen_angel),
                 ('glass and opaque horn escaping cap', lose_cap_root),
                 ('unrequested eye alteration', move_eye)]
    rejected = []
    try:
        with tempfile.TemporaryDirectory(prefix='chao021-chaos-corruptions-') as temp:
            MODELS = Path(temp)
            for label, mutation in mutations:
                changed = copy.deepcopy(fixtures)
                mutation(changed)
                for name, data in changed.items():
                    (MODELS / name).write_text(json.dumps(data))
                try:
                    main(write_report=False)
                except AssertionError:
                    rejected.append(label)
                else:
                    raise AssertionError('Audit accepted corruption: ' + label)
    finally:
        MODELS = original
    print('Chaos021 audit rejected all four isolated corruptions: ' + '; '.join(rejected))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    main()
    if args.self_test:
        rejected_corruptions()
