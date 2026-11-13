#!/usr/bin/env python3
"""Independent JSON and geometry diagnostics for Dark/Normal .60.

No generator import, Minecraft renderer or browser dependency. Dense blended
parameter samples test real rigid-box corner correspondence, separating-axis
connections and facial/marker clearance. These are not visual proof.
"""
from pathlib import Path
import copy
import hashlib
import json
import math
from validate_blocky_run_02555 import vertices, connected, volume, overlaps, rotate, dot, sub, cross
from validate_neutral_swim_morph_02559 import bounds, closed, head_plane_gap

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODEL = ASSETS / 'models/entity/gameplay/chao_dark_normal_proto060.json'
SOURCE = ASSETS / 'models/entity/gameplay/chao_baby.json'
FIELD = 'dark_normal_box_060'
TARGETS = ['dark_normal_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']


def weights(age, rp, sf):
    return [1 - age] + [age * max(0, 1 - abs(rp - x)) * max(0, 1 - abs(sf - y))
                        for y in (-1, 0, 1) for x in (-1, 0, 1)]


def endpoint(mesh, target):
    w = [0] * 10
    w[target] = 1
    return vertices(mesh, w)


def axis_end(mesh, target):
    points = endpoint(mesh, target)
    return [sum(points[i][a] for i in (4, 5, 6, 7)) / 4 for a in range(3)]


def semantic_endpoints(features):
    named = {m['name']: m for m in features}
    tip = named['dark_crest_tip']
    root = named['dark_crest_root']
    young, balanced, run, power, fly, swim, rs, ps, rf, pf = [axis_end(tip, i) for i in (0, 5, 4, 6, 8, 2, 1, 3, 7, 9)]
    assert balanced[1] > young[1] + .8, 'Balanced hood must develop downward independently of age0 stats'
    assert run[1] < balanced[1] - 3, 'Run posterior crest must rise substantially'
    assert power[1] < balanced[1] - 2, 'Power posterior crest must rise'
    assert root[FIELD]['sizes'][6][0] > root[FIELD]['sizes'][4][0] + .5, 'Power root must be broader than Run'
    assert swim[1] > balanced[1] + .4, 'Pure Swim rear hood must hang farther down'
    assert rs[2] < swim[2] - .5 and rs[1] < swim[1] - 1.5, 'Run+Swim should remain a short moderate rear point'
    assert ps[2] < swim[2] - .4 and ps[1] < swim[1] - 1.5, 'Power+Swim should remain a short moderate rear point'
    assert fly[1] < balanced[1] - 2, 'Fly posterior hook must rise'
    # Fly's actual final local Z axis points upward/back; the Run tip stays
    # almost horizontal. Inspect emitted edge differences, not profile names.
    front = [sum(endpoint(tip, 8)[i][a] for i in (0, 1, 2, 3)) / 4 for a in range(3)]
    assert fly[1] < front[1] - .5 and fly[2] > front[2], 'Fly terminal segment must turn up/back'
    assert abs(run[1] - (sum(endpoint(tip, 4)[i][1] for i in (0, 1, 2, 3)) / 4)) < .1
    assert rf[1] < balanced[1] - 3 and pf[1] < balanced[1] - 2, 'Compatible Fly mixes lost raised silhouettes'
    signatures = [tuple(round(x, 7) for m in features for p in endpoint(m, target) for x in p)
                  for target in range(1, 10)]
    assert len(set(signatures)) == 9, 'Requested mature forms must all be distinct'


def marker_planes(boxes):
    """All seven dot-local boxes share one existing bone transform.

    Independent same-facing plane/rectangle intersection rejects z-fighting
    between core and spurs, or between spurs. Here all box axes are aligned in
    dot-local coordinates; rotation/scale of their common bone preserves the
    absence of coincident surfaces. Perpendicular faces create real shaded
    raised edges, which must not be mistaken for texture holes.
    """
    pairs = parallel = coincident = 0
    for ai, (a, ap) in enumerate(boxes):
        for bi, (b, bp) in enumerate(boxes):
            if bi <= ai:
                continue
            for fa in a['faces']:
                aa = [ap[i] for i in fa['v']]
                an = cross(sub(aa[1], aa[0]), sub(aa[2], aa[0]))
                alen = math.sqrt(dot(an, an))
                an = [n / alen for n in an]
                for fb in b['faces']:
                    pairs += 1
                    bb = [bp[i] for i in fb['v']]
                    bn = cross(sub(bb[1], bb[0]), sub(bb[2], bb[0]))
                    blen = math.sqrt(dot(bn, bn))
                    bn = [n / blen for n in bn]
                    if dot(an, bn) < 1 - 1e-8:
                        continue
                    parallel += 1
                    if max(abs(dot(sub(v, aa[0]), an)) for v in bb) > 1e-8:
                        continue
                    coincident += 1
                    axis = max(range(3), key=lambda i: abs(an[i]))
                    other = [i for i in range(3) if i != axis]
                    assert abs(an[axis]) > 1 - 1e-8, 'Marker local axes changed; update independent polygon clip test'
                    area = 1
                    for k in other:
                        area *= max(0, min(max(q[k] for q in aa), max(q[k] for q in bb)) - max(min(q[k] for q in aa), min(q[k] for q in bb)))
                    assert area <= 1e-8, (a['name'], b['name'], axis, area, 'Marker same-facing coplanar overlap')
    return pairs, parallel, coincident


def main():
    data = json.loads(MODEL.read_text())
    source = json.loads(SOURCE.read_text())
    bones = {b['name']: b for b in data['bones']}
    old = {b['name']: b for b in source['bones']}
    assert list(bones) == list(old) and len(bones) == 59
    for name, original in old.items():
        actual = bones[name]
        for field in ('pivot', 'rotation', 'parent'):
            assert actual.get(field) == original.get(field), (name, field)
        if name.startswith(('skull', 'expression_', 'emotion_')):
            if name == 'emotion_dot__cube_0':
                assert actual['meshes'][0] == original['meshes'][0]
                assert len(actual['meshes']) == len(original['meshes']) + 6
            else:
                assert actual == original, ('Protected mesh changed', name)
    assert data['head_decorations'] == source['head_decorations']
    assert data['materials'] == source['materials']
    head = old['skull__cube_0']['meshes'][0]['vertices']
    assert bounds(head) == [[-3.5, 3.5], [-6, 0], [-3, 3]]
    assert data['dark_normal_path_060']['targets'] == TARGETS
    boxes, features, wings, markers, tails = [], [], [], [], []
    body = None
    for bone in data['bones']:
        for original in bone.get('meshes', []):
            if FIELD not in original:
                continue
            mesh = copy.deepcopy(original)
            mesh['blocky_box_055'] = mesh[FIELD]
            boxes.append(mesh)
            if bone['name'] == 'body__cube_0':
                body = mesh
            if bone['name'] == 'gradual_head_021':
                features.append(mesh)
            if bone['name'].startswith('wing_'):
                wings.append((bone['name'], mesh))
            if bone['name'] == 'emotion_dot__cube_0':
                markers.append(mesh)
            if bone['name'] == 'tail__cube_0':
                tails.append(mesh)
            closed(mesh)
            for field in ('centers', 'sizes', 'rotations'):
                values = mesh[FIELD][field]
                assert len(values) == 10 and all(len(v) == 3 for v in values)
                assert all(math.isfinite(x) for v in values for x in v)
            assert all(x >= .01 for v in mesh[FIELD]['sizes'] for x in v)
            actual = endpoint(mesh, 0)
            assert max(abs(a - b) for va, vb in zip(actual, mesh['vertices'])
                       for a, b in zip(va, vb)) < 1e-7
    assert len(features) == 3 and len(boxes) == 18 and len(wings) == 2 and len(markers) == 6 and len(tails) == 2
    semantic_endpoints(features)
    tail_root, tail_tip = tails
    for target in range(10):
        for mesh, expected_pitch in ((tail_root, 10), (tail_tip, 35)):
            points = [rotate(v, [45, 0, 0]) for v in endpoint(mesh, target)]
            ends = [[sum(points[i][a] for i in ids) / 4 for a in range(3)]
                    for ids in ((0, 1, 2, 3), (4, 5, 6, 7))]
            direction = [b - a for a, b in zip(*ends)]
            actual_pitch = math.degrees(math.atan2(-direction[1], direction[2]))
            assert abs(actual_pitch - expected_pitch) < 1e-6, ('Tail direction changed', target, mesh['name'], actual_pitch)
    assert tail_root[FIELD]['sizes'][8][2] > tail_root[FIELD]['sizes'][5][2] + 1, 'Fly tail must lengthen distinctly'
    for mesh in markers:
        for field in ('centers', 'sizes', 'rotations'):
            assert all(p == mesh[FIELD][field][0] for p in mesh[FIELD][field]), 'Marker geometry must be independent of later training'
    assert body is not None
    old_body = old['body__cube_0']['meshes'][0]['vertices']
    assert bounds(body['vertices'])[0] == bounds(old_body)[0]
    assert bounds(body['vertices'])[2] == bounds(old_body)[2]
    assert abs(bounds(body['vertices'])[1][0] - (bounds(old_body)[1][0] - .45)) < 1e-8
    assert abs(bounds(body['vertices'])[1][1] - bounds(old_body)[1][1]) < 1e-8
    for target in range(10):
        assert abs(body[FIELD]['centers'][target][1] - (-1.5 - .225)) < 1e-8
    eye_vertices = [m['vertices'] for n in ('skull__cube_2', 'skull__cube_3') for m in old[n]['meshes']]
    core_mesh = old['emotion_dot__cube_0']['meshes'][0]
    core = core_mesh['vertices']
    marker_plane_counts = marker_planes([(core_mesh, core)] + [(m, endpoint(m, 0)) for m in markers])
    marker_local = [core] + [endpoint(m, 0) for m in markers]
    # Exact static original chain: emotion_dots[-10,+.5Z], dot[-1,-.5Z]
    # followed by its original -22.5deg pitch. Native dynamic motion is checked
    # separately against the production emitted bones, not claimed here.
    marker_skull = [[[p[0], p[1] - 11, p[2]] for p in [rotate(v, [-22.5, 0, 0]) for v in part]]
                    for part in marker_local]
    assert all(overlaps(m, core) for m in marker_local[1:]), 'Marker protrusion detached from original core'
    minimum_head_plane_gap = math.inf
    maximum_step = 0
    frames = 0
    all_bounds = [[999, -999] for _ in range(3)]
    for ai in range(21):
        age = ai / 20
        for yi in range(21):
            sf = yi / 10 - 1
            for xi in range(21):
                rp = xi / 10 - 1
                w = weights(age, rp, sf)
                assert abs(sum(w) - 1) < 1e-9 and min(w) >= -1e-9
                points = [vertices(m, w) for m in features]
                assert connected(points, head), (age, rp, sf, 'Disconnected head feature')
                for mesh, part in zip(features, points):
                    minimum_head_plane_gap = min(minimum_head_plane_gap, head_plane_gap(mesh, part, bounds(head)))
                    assert not any(overlaps(part, eye) for eye in eye_vertices), (age, rp, sf, mesh['name'], 'Eye collision')
                    assert not any(overlaps(part, marker) for marker in marker_skull), (age, rp, sf, mesh['name'], 'Original tilted spikeball collision')
                    for axis, (lo, hi) in enumerate(bounds(part)):
                        all_bounds[axis][0] = min(all_bounds[axis][0], lo)
                        all_bounds[axis][1] = max(all_bounds[axis][1], hi)
                tail_parts = [vertices(m, w) for m in tails]
                assert overlaps(tail_parts[0], tail_parts[1]), (age, rp, sf, 'Tail tip detached')
                for mesh in boxes:
                    assert volume(vertices(mesh, w), mesh['faces']) > 1e-8, (mesh['name'], age, rp, sf)
                for name, wing in wings:
                    values = vertices(wing, w)
                    xb = bounds(values)[0]
                    edge = xb[1] if 'left' in name else xb[0]
                    assert abs(edge) < 1e-7, (age, rp, sf, name, 'Wing hinge drift')
                    near = [q for q in values if abs(q[0]) < 1e-7]
                    assert len(near) == 4
                    assert abs(sum(q[1] for q in near) / 4 - .25) < 1e-7
                    assert abs(sum(q[2] for q in near) / 4) < 1e-7
                if ai < 20:
                    ahead = [vertices(m, weights(age + .05, rp, sf)) for m in features]
                    maximum_step = max(maximum_step, max(abs(a - b) for p, q in zip(points, ahead) for u, v in zip(p, q) for a, b in zip(u, v)))
                frames += 1
    for name in ('foot_left__cube_0', 'foot_right__cube_0'):
        mesh = next(m for b in data['bones'] if b['name'] == name for m in b['meshes'])
        mesh = copy.deepcopy(mesh)
        mesh['blocky_box_055'] = mesh[FIELD]
        for target in range(10):
            assert abs(max(p[1] for p in endpoint(mesh, target)) - 2) < 1e-7
            assert abs(max(p[2] for p in endpoint(mesh, target))) < 1e-7
    for name, wing in wings:
        original = old[name]['meshes'][0]
        assert wing['faces'][0] == original['faces'][0], 'Original wing front UV changed'
        front = original['faces'][0]
        uv = {i: front['uv'][j] for j, i in enumerate(front['v'])}
        assert wing['faces'][1]['uv'] == [uv[i % 4] for i in wing['faces'][1]['v']]
        for face in wing['faces'][2:]:
            expected = [[max(16.125, min(18.875, uv[i % 4][0])),
                         max(17.125, min(18.875, uv[i % 4][1]))] for i in face['v']]
            assert face['uv'] == expected, 'Closing edge no longer follows front alpha'
    manifest = json.loads((ASSETS / 'dark_normal060.json').read_text())
    assert manifest['sha256'] == hashlib.sha256(MODEL.read_bytes()).hexdigest()
    assert manifest['source_model_sha256'] == hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    for path, expected in manifest['construction_reference_sha256'].items():
        assert hashlib.sha256((ROOT / path).read_bytes()).hexdigest() == expected
    print(f'DarkNormal060:18positive closed boxes,{frames}connected fullsigned age/axis blends; exact original7x6x6 head/face/59joints; nine distinct mature silhouettes,6connected same-bone marker protrusions, eye/marker SATclearance,2connected tailparts,anchored soles/heels andwings passed. Marker same-facing plane counts(face pairs,parallel,disjoint coplanar)={marker_plane_counts};0positiveareaoverlaps. Head/feature samefacingplane checks minimumpositivegap={minimum_head_plane_gap:.9f}px; maximum5%ageaxisvertexstep={maximum_step:.9f}px; added-headbounds={all_bounds}.')


if __name__ == '__main__':
    main()
