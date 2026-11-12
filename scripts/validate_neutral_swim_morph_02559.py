#!/usr/bin/env python3
"""Independent source checks for the Neutral/Swim first-type morphology.

Reads the delivered JSON, never imports the authoring generator, and evaluates
positive box parameters plus separating-axis connectivity over dense blends.
These are geometry diagnostics, not a Minecraft screenshot or motion claim.
"""
from pathlib import Path
import copy
import hashlib
import json
import math
from validate_blocky_run_02555 import vertices, connected, volume, overlaps

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODEL = ASSETS / 'models/entity/gameplay/chao_neutral_swim_proto059.json'
SOURCE = ASSETS / 'models/entity/gameplay/chao_baby.json'
FIELD = 'neutral_swim_box_059'
TARGETS = ['neutral_swim_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_guard', 'balanced_guard', 'power_guard']


def weights(age, rp, sf):
    sf = min(0, max(-1, sf))
    rp = min(1, max(-1, rp))
    return [1 - age] + [age * max(0, 1 - abs(rp - x)) * max(0, 1 - abs(sf - y))
                        for y in (-1, 0, 1) for x in (-1, 0, 1)]


def bounds(points):
    return [[min(p[a] for p in points), max(p[a] for p in points)] for a in range(3)]


def closed(mesh):
    assert len(mesh['vertices']) == 8 and len(mesh['faces']) == 6, mesh['name']
    edges = {}
    for face in mesh['faces']:
        ids = face['v']
        assert len(ids) == 4 and len(face['uv']) == 4
        assert len(set(ids)) == 4 and all(0 <= i < 8 for i in ids)
        for a, b in zip(ids, ids[1:] + ids[:1]):
            edges[(a, b)] = edges.get((a, b), 0) + 1
    assert len(edges) == 24 and all(n == 1 and edges.get((b, a)) == 1
                                    for (a, b), n in edges.items()), mesh['name']


def head_plane_gap(mesh, points, head_bounds):
    """Reject overlapping, same-facing head/feature coplanar face pairs.

    The head is the exact axis-aligned original cuboid. Project a parallel
    feature face onto its matching head plane, clip that convex quadrilateral
    against the head rectangle and require positive plane separation whenever
    the projected intersection has positive area. This catches the former
    pure-Swim roof rearZ=3 overlap without calling a narrow lit ledge a bug.
    """
    gap = math.inf
    for face in mesh['faces']:
        p = [points[i] for i in face['v']]
        a = [p[1][i] - p[0][i] for i in range(3)]
        b = [p[2][i] - p[0][i] for i in range(3)]
        normal = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]
        axis = max(range(3), key=lambda i: abs(normal[i]))
        if any(abs(normal[i]) > abs(normal[axis])*1e-7 for i in range(3) if i != axis):
            continue
        others = [i for i in range(3) if i != axis]
        polygon = [[q[i] for i in others] for q in p]
        for dim, coordinate in enumerate(others):
            for value, sign in ((head_bounds[coordinate][0], 1), (head_bounds[coordinate][1], -1)):
                clipped = []
                for u, v in zip(polygon, polygon[1:] + polygon[:1]):
                    inside_u = sign*(u[dim]-value) >= 0
                    inside_v = sign*(v[dim]-value) >= 0
                    if inside_u:
                        clipped.append(u)
                    if inside_u != inside_v:
                        t = (value-u[dim])/(v[dim]-u[dim])
                        clipped.append([u[j]+t*(v[j]-u[j]) for j in range(2)])
                polygon = clipped
                if not polygon:
                    break
            if not polygon:
                break
        if len(polygon) < 3:
            continue
        area = abs(sum(u[0]*v[1]-v[0]*u[1]
                       for u, v in zip(polygon, polygon[1:] + polygon[:1])))/2
        if area <= 1e-8:
            continue
        head_plane = head_bounds[axis][1 if normal[axis] > 0 else 0]
        separation = abs(sum(q[axis] for q in p)/4-head_plane)
        assert separation > 1e-7, (mesh['name'], axis, head_plane, area, 'Overlapping same-facing head/feature coplanar faces')
        gap = min(gap, separation)
    return gap


def endpoint(mesh, target):
    w = [0] * 10
    w[target] = 1
    return vertices(mesh, w)


def semantic_endpoints(features):
    named = {m['name']: m for m in features}
    rear = named['swim_cap_rear']
    # Inspect evaluated posterior edge (local template +Z), rather than the
    # generator's names/comments. Run points rear/up; Swim hood drops down.
    def rear_center(target):
        p = endpoint(rear, target)
        return [sum(p[i][a] for i in (4, 5, 6, 7)) / 4 for a in range(3)]
    young, balanced, run, power, swim, rs, ps = [rear_center(i) for i in (0, 5, 4, 6, 2, 1, 3)]
    assert young[2] > 4, 'Fresh Swim form lost its already-present posterior cap'
    assert run[2] > balanced[2] > power[2], 'Run/balanced/Power posterior silhouettes are indistinct'
    assert run[1] < balanced[1] - 1, 'Run posterior cap did not rise toward horizontal'
    assert swim[1] > balanced[1] + 1, 'Pure Swim hood did not hang lower'
    assert rs[1] < swim[1] and ps[1] < swim[1], 'Mixed hoods incorrectly stack maximum Swim drop'
    fin = named['swim_side_fin_right_tip']
    edges = {i: bounds(endpoint(fin, i)) for i in (0, 1, 2, 3, 4, 5, 6)}
    assert edges[4][1][0] < edges[5][1][0] - .7, 'Run temple fin did not turn upward'
    assert edges[2][1][1] > edges[5][1][1] + 2.0, 'Pure Swim flipper did not hang down'
    assert edges[2][1][1] > edges[1][1][1] + .5 and edges[2][1][1] > edges[3][1][1] + .5, 'Combined flippers should be shorter than pure Swim'
    channel = fin[FIELD]
    assert channel['sizes'][6][1] > channel['sizes'][4][1] * 2, 'Power fin should be stockier than Run'
    signatures = []
    for target in (1, 2, 3, 4, 5, 6):
        signatures.append(tuple(round(x, 7) for m in features for p in endpoint(m, target) for x in p))
    assert len(set(signatures)) == 6, 'Two requested mature forms are identical'


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
            assert actual == original, ('Protected mesh changed', name)
    assert data['head_decorations'] == source['head_decorations']
    assert data['materials'] == source['materials']
    head = old['skull__cube_0']['meshes'][0]['vertices']
    assert bounds(head) == [[-3.5, 3.5], [-6, 0], [-3, 3]]
    assert data['neutral_swim_path_059']['targets'] == TARGETS
    boxes, features, wings = [], [], []
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
            closed(mesh)
            for field in ('centers', 'sizes', 'rotations'):
                values = mesh[FIELD][field]
                assert len(values) == 10 and all(len(v) == 3 for v in values)
                assert all(math.isfinite(x) for v in values for x in v)
                for guard, center in ((7, 4), (8, 5), (9, 6)):
                    assert values[guard] == values[center], (mesh['name'], field, 'Fly guard differs')
            assert all(x >= .01 for v in mesh[FIELD]['sizes'] for x in v)
            actual = endpoint(mesh, 0)
            assert max(abs(a - b) for va, vb in zip(actual, mesh['vertices'])
                       for a, b in zip(va, vb)) < 1e-7
    assert len(features) == 7 and len(boxes) == 14 and len(wings) == 2
    semantic_endpoints(features)
    assert body is not None
    old_body = old['body__cube_0']['meshes'][0]['vertices']
    # The scope is precisely an existing torso's top-only extension. Its
    # fresh reference and every target keep the lower plane, X/Z and joints.
    assert bounds(body['vertices'])[0] == bounds(old_body)[0]
    assert bounds(body['vertices'])[2] == bounds(old_body)[2]
    assert abs(bounds(body['vertices'])[1][0] - (bounds(old_body)[1][0] - .45)) < 1e-8
    assert abs(bounds(body['vertices'])[1][1] - bounds(old_body)[1][1]) < 1e-8
    original_heights = [4.0, 4.06, 4.06, 4.16, 4.06, 4.06, 4.16, 4.06, 4.06, 4.16]
    for target, height in enumerate(original_heights):
        assert abs(body[FIELD]['centers'][target][1] - (-1.5 - .225)) < 1e-8
        assert abs(body[FIELD]['sizes'][target][1] - (height + .45)) < 1e-8
        assert abs(bounds(endpoint(body, target))[1][1] - (-1.5 + height / 2)) < 1e-8
    minimum_head_plane_gap = math.inf
    for target in range(10):
        for mesh in features:
            minimum_head_plane_gap = min(minimum_head_plane_gap, head_plane_gap(mesh, endpoint(mesh, target), bounds(head)))
    # Face panels have unchanged geometry in the skull frame. Keep all new
    # fins/hoods behind/above them rather than testing eye values alone.
    eyes = [m['vertices'] for n in ('skull__cube_2', 'skull__cube_3') for m in old[n]['meshes']]
    orb_bottom = -11 + math.cos(math.radians(22.5)) + math.sin(math.radians(22.5))
    frames = 0
    minimum_marker_gap = 999
    all_bounds = [[999, -999] for _ in range(3)]
    for ai in range(21):
        age = ai / 20
        for yi in range(11):
            sf = yi / 10 - 1
            for xi in range(21):
                rp = xi / 10 - 1
                w = weights(age, rp, sf)
                assert abs(sum(w) - 1) < 1e-9 and min(w) >= 0
                p = [vertices(m, w) for m in features]
                assert connected(p, head), (age, rp, sf, 'Disconnected cap or fin')
                for mesh, points in zip(features, p):
                    minimum_head_plane_gap = min(minimum_head_plane_gap, head_plane_gap(mesh, points, bounds(head)))
                for name, points in zip([m['name'] for m in features], p):
                    assert not any(overlaps(points, eye) for eye in eyes), (age, rp, sf, name, 'Eye collision')
                    for axis, (lo, hi) in enumerate(bounds(points)):
                        all_bounds[axis][0] = min(all_bounds[axis][0], lo)
                        all_bounds[axis][1] = max(all_bounds[axis][1], hi)
                gap = min(v[1] for part in p for v in part) - orb_bottom
                minimum_marker_gap = min(minimum_marker_gap, gap)
                assert gap > .3, (age, rp, sf, 'Default tilted orb collision')
                for mesh in boxes:
                    assert volume(vertices(mesh, w), mesh['faces']) > 1e-8, (mesh['name'], age, rp, sf)
                expected_body_lower = sum(weight * (-1.5 + height / 2)
                                          for weight, height in zip(w, original_heights))
                assert abs(bounds(vertices(body, w))[1][1] - expected_body_lower) < 1e-7
                for name, wing in wings:
                    values = vertices(wing, w)
                    xb = bounds(values)[0]
                    edge = xb[1] if 'left' in name else xb[0]
                    assert abs(edge) < 1e-7, (age, rp, sf, name, 'Wing hinge drift')
                    near = [q for q in values if abs(q[0]) < 1e-7]
                    assert len(near) == 4
                    assert abs(sum(q[1] for q in near) / 4 - .25) < 1e-7
                    assert abs(sum(q[2] for q in near) / 4) < 1e-7
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
    manifest = json.loads((ASSETS / 'neutral_swim059.json').read_text())
    assert manifest['sha256'] == hashlib.sha256(MODEL.read_bytes()).hexdigest()
    assert manifest['source_model_sha256'] == hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    for path, expected in manifest['construction_reference_sha256'].items():
        assert hashlib.sha256((ROOT / path).read_bytes()).hexdigest() == expected
    assert minimum_head_plane_gap >= .024999, minimum_head_plane_gap
    print(f'NeutralSwim059: 14 positive closed boxes, {frames} connected age/axis blends; exact original 7x6x6 head, face, emotion and59-joint rig; six distinct mature silhouettes, duplicate Fly guard row, eye/orb clearance, anchored soles/heels and wings passed. No overlapping same-facing head/feature coplanarity at all10targets andall4851blends; minimum positive matching-plane gap={minimum_head_plane_gap:.9f}. Minimum tilted-orb gap={minimum_marker_gap:.9f}; added-head bounds={all_bounds}.')


if __name__ == '__main__':
    main()
