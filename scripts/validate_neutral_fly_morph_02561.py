#!/usr/bin/env python3
"""Independent JSON/rig/rigid geometry checks for Neutral/Fly first .61.

Reads delivered assets without importing the generator. Dense emitted rigid
box blends, SAT connections and projected face clipping are diagnostics,
not a claim of Minecraft visual or motion acceptance.
"""
from pathlib import Path
import copy
import hashlib
import json
import math
import random
from validate_blocky_run_02555 import vertices, connected, volume, overlaps, rotate, dot, sub, cross
from validate_neutral_swim_morph_02559 import bounds, closed

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
MODEL = MODELS / 'chao_neutral_fly_proto061.json'
SOURCE = MODELS / 'chao_baby.json'
FIELD = 'neutral_fly_box_061'
REGION = 'neutral_fly_region_061'
TARGETS = ['neutral_fly_young', 'run_guard', 'balanced_guard', 'power_guard',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
PROTECTED = {
    'chao_baby.json': 'ae3d4da88b84905f8726b863bdb072e3a26eb404f38e565d0781b92b01e2cf9b',
    'chao_hero_run_proto055.json': '77b41d4501cba585b83be91b4052e8c56e03bff02e47db2b209f146d36390f08',
    'chao_neutral_proto056.json': 'a354458cae5605b873464614b57f9f9bd04d4a4e01d1ed793cf949194c4d14db',
    'chao_hero_normal_proto057.json': 'd0052db7f401ecc545e7a3db2992933b5671387520c82429d4a8aa336f28232d',
    'chao_neutral_swim_proto059.json': '06c6c2372669adb2fcf0fb4d529cbf9d0866b20e4b0d23b002e84c9fc8a9a1a8',
    'chao_dark_normal_proto060.json': '36ae27aed51674ea31310ab4af602e3e7127e1507c736ee9b6f75f3728acb514',
}


def weights(age, rp, sf):
    age = min(1, max(0, age))
    rp = min(1, max(-1, rp))
    sf = min(1, max(0, sf))
    return [1 - age] + [age * max(0, 1 - abs(rp - x)) * max(0, 1 - abs(sf - y))
                        for y in (-1, 0, 1) for x in (-1, 0, 1)]


def endpoint(mesh, target):
    w = [0] * 10
    w[target] = 1
    return vertices(mesh, w)


def signed_area(polygon):
    return sum(a[0] * b[1] - b[0] * a[1]
               for a, b in zip(polygon, polygon[1:] + polygon[:1])) / 2


def intersection_area(subject, clip):
    """Convex polygon clipping with either winding, independent of box axes."""
    winding = 1 if signed_area(clip) > 0 else -1
    for a, b in zip(clip, clip[1:] + clip[:1]):
        def side(point):
            return winding * ((b[0] - a[0]) * (point[1] - a[1]) -
                              (b[1] - a[1]) * (point[0] - a[0]))
        result = []
        for u, v in zip(subject, subject[1:] + subject[:1]):
            su, sv = side(u), side(v)
            if su >= -1e-10:
                result.append(u)
            if (su >= -1e-10) != (sv >= -1e-10):
                t = su / (su - sv)
                result.append([u[i] + t * (v[i] - u[i]) for i in range(2)])
        subject = result
        if len(subject) < 3:
            return 0
    return abs(signed_area(subject))


def face_planes(mesh, points):
    result = []
    for face in mesh['faces']:
        p = [points[i] for i in face['v']]
        n = cross(sub(p[1], p[0]), sub(p[2], p[0]))
        length = math.sqrt(dot(n, n))
        assert length > 1e-8, ('Degenerate face', mesh['name'])
        result.append((p, [x / length for x in n]))
    return result


def no_same_facing_coplanar(boxes):
    """Reject positive-area coincident faces for arbitrary rotated cuboids.

    Dropping the dominant normal coordinate keeps a stable 2D projection.
    The clipping test covers feature/feature pairs as well as skull/features.
    Opposite-facing internal intersection surfaces are intentionally allowed.
    """
    planes = [(mesh['name'], face_planes(mesh, points)) for mesh, points in boxes]
    examined = coincident = 0
    for i, (name_a, faces_a) in enumerate(planes):
        for name_b, faces_b in planes[i + 1:]:
            for pa, na in faces_a:
                for pb, nb in faces_b:
                    examined += 1
                    if dot(na, nb) < 1 - 1e-8:
                        continue
                    if max(abs(dot(sub(p, pa[0]), na)) for p in pb) > 1e-7:
                        continue
                    coincident += 1
                    axis = max(range(3), key=lambda a: abs(na[a]))
                    keep = [a for a in range(3) if a != axis]
                    area = intersection_area([[p[a] for a in keep] for p in pa],
                                             [[p[a] for a in keep] for p in pb])
                    assert area < 1e-8, (name_a, name_b, area,
                                         'Same-facing positive-area coplanar overlap')
    return examined, coincident


def semantic_endpoints(features, wings):
    named = {m['name']: m for m in features}
    root = named['fly_crest_right_root']
    tip = named['fly_crest_right_tip']
    def terminal(mesh, target):
        points = endpoint(mesh, target)
        return [sum(points[i][a] for i in (0, 3, 4, 7)) / 4 for a in range(3)]
    fresh, balanced, run, power, fly, run_fly, power_fly = [terminal(tip, i)
        for i in (0, 5, 4, 6, 8, 7, 9)]
    assert balanced[1] < fresh[1] - 1 and balanced[2] > fresh[2] + 1.5, 'Balanced crests must develop up/back from fresh ears'
    assert run[1] > balanced[1] + 2 and run[2] < terminal(root, 4)[2], 'Run must form down/forward hooks'
    assert power[1] < balanced[1] - 1.5 and power[2] < terminal(root, 6)[2], 'Power must form tall upright roots and forward tips'
    assert fly[1] > balanced[1] + 2, 'Fly must form downward terminal points'
    assert run_fly[1] < run[1] - .5, 'Run+Fly curled points must remain more compact than Run'
    assert power_fly[1] > power[1] + 1.5, 'Power+Fly crests must stay shorter than pure Power'
    signatures = [tuple(round(x, 7) for m in features for p in endpoint(m, t) for x in p)
                  for t in (4, 5, 6, 7, 8, 9)]
    assert len(set(signatures)) == 6, 'Six mature silhouettes must be distinct'
    for _, wing in wings:
        span = wing[FIELD]['sizes']
        assert span[8][0] > span[5][0] + .5, 'Fly specialization must enlarge wings'
        assert span[4][0] > span[6][0], 'Run wing must be longer than compact Power wing'


def wing_uv_coverage(name, mesh):
    """Sample actual long-face positions/UVs and require both opaque texels.

    Geometry projection determines the true root-to-distal fraction without
    assuming face order or trusting the generator's corner-ID selection.
    Each broad and narrow long face must show substantial 125/124 coverage.
    The material test independently reads alpha/pigment in painted textures.
    """
    sign = -1 if 'left' in name else 1
    channel = mesh[FIELD]
    center = channel['centers'][0]
    axis = rotate([1, 0, 0], channel['rotations'][0])
    span = channel['sizes'][0][0]
    count = root_count = distal_count = long_faces = 0
    for face in mesh['faces']:
        points = [mesh['vertices'][i] for i in face['v']]
        fractions = [.5 + sign * dot(sub(p, center), axis) / span for p in points]
        for fraction, uv in zip(fractions, face['uv']):
            assert abs(uv[0] - (62.75 - .5 * fraction)) < 1e-7
            assert uv[1] == 31.75
        if max(fractions) - min(fractions) < .99:
            pixel = 124 if min(fractions) > .99 else 125
            assert all(math.floor(uv[0] * 2) == pixel for uv in face['uv'])
            continue
        long_faces += 1
        root_hits = distal_hits = 0
        for si in range(21):
            for ti in range(9):
                s, t = si / 20, ti / 8
                w = [(1-s)*(1-t), s*(1-t), s*t, (1-s)*t]
                point = [sum(a * p[k] for a, p in zip(w, points)) for k in range(3)]
                uv = [sum(a * p[k] for a, p in zip(w, face['uv'])) for k in range(2)]
                fraction = .5 + sign * dot(sub(point, center), axis) / span
                pixel = (math.floor(uv[0] * 2), math.floor(uv[1] * 2))
                assert pixel in ((124, 63), (125, 63)), ('Wing UV escaped opaque semantic texels', name, pixel)
                if fraction < .49:
                    assert pixel == (125, 63), ('Wrong pigment in actual root half', name, fraction, pixel)
                    root_hits += 1
                if fraction > .51:
                    assert pixel == (124, 63), ('Wrong pigment in actual distal half', name, fraction, pixel)
                    distal_hits += 1
                count += 1
        assert root_hits >= 80 and distal_hits >= 80, ('Missing visible root/distal band', name, root_hits, distal_hits)
        root_count += root_hits
        distal_count += distal_hits
    assert long_faces == 4, ('Expected four banded long faces', name, long_faces)
    return count, root_count, distal_count


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
            assert actual == original, ('Protected head/face/orb changed', name)
    assert data['head_decorations'] == source['head_decorations']
    assert data['materials'] == source['materials']
    head_mesh = old['skull__cube_0']['meshes'][0]
    head = head_mesh['vertices']
    assert bounds(head) == [[-3.5, 3.5], [-6, 0], [-3, 3]]
    meta = data['neutral_fly_path_061']
    assert meta['targets'] == TARGETS
    assert meta['reserved_pixels'] == {'feature_body': [127, 63], 'feature_tip': [126, 63],
                                       'feature_fin': [125, 63], 'feature_underside': [124, 63]}
    old_uv = [uv for b in source['bones'] for m in b.get('meshes', [])
              for f in m['faces'] for uv in f['uv']]
    assert max(v for u, v in old_uv) < 31.75, 'Reserved swatches intersect original atlas use'
    boxes, features, wings, tails = [], [], [], []
    body = None
    for bone in data['bones']:
        for original in bone.get('meshes', []):
            if FIELD not in original:
                continue
            mesh = copy.deepcopy(original)
            mesh['blocky_box_055'] = mesh[FIELD]
            assert REGION in mesh, ('Missing semantic region', bone['name'])
            boxes.append(mesh)
            if bone['name'] == 'body__cube_0':
                body = mesh
            if bone['name'] == 'gradual_head_021':
                features.append(mesh)
            if bone['name'].startswith('wing_'):
                wings.append((bone['name'], mesh))
            if bone['name'] == 'tail__cube_0':
                tails.append(mesh)
            closed(mesh)
            for field in ('centers', 'sizes', 'rotations'):
                values = mesh[FIELD][field]
                assert len(values) == 10 and all(len(v) == 3 for v in values)
                assert all(math.isfinite(x) for v in values for x in v)
                for guard, center in ((1, 4), (2, 5), (3, 6)):
                    assert values[guard] == values[center], (mesh['name'], field, 'Negative Swim guard differs')
            assert all(x >= .01 for v in mesh[FIELD]['sizes'] for x in v)
            actual = endpoint(mesh, 0)
            assert max(abs(a - b) for va, vb in zip(actual, mesh['vertices'])
                       for a, b in zip(va, vb)) < 1e-7
    assert len(features) == 4 and len(wings) == 6 and len(tails) == 2
    assert len(boxes) == len(features) + 5 + len(wings) + len(tails)
    semantic_endpoints(features, wings)
    assert tails[0][FIELD]['sizes'][8][2] > tails[0][FIELD]['sizes'][5][2] + 1
    assert tails[0][FIELD]['sizes'][6][2] < tails[0][FIELD]['sizes'][5][2] - .5
    assert body is not None
    old_body = old['body__cube_0']['meshes'][0]['vertices']
    assert bounds(body['vertices'])[0] == bounds(old_body)[0]
    assert bounds(body['vertices'])[2] == bounds(old_body)[2]
    assert abs(bounds(body['vertices'])[1][0] - (bounds(old_body)[1][0] - .45)) < 1e-8
    assert abs(bounds(body['vertices'])[1][1] - bounds(old_body)[1][1]) < 1e-8
    for target in range(10):
        assert abs(body[FIELD]['centers'][target][1] - (-1.5 - .225)) < 1e-8
    eyes = [m['vertices'] for n in ('skull__cube_2', 'skull__cube_3') for m in old[n]['meshes']]
    # Exact original static orb chain in skull coordinates. Dynamic production
    # animation and expression replacement checks belong to the native suite.
    orb = [[p[0], p[1] - 11, p[2]] for p in
           [rotate(v, [-22.5, 0, 0]) for v in old['emotion_dot__cube_0']['meshes'][0]['vertices']]]
    samples = [(a / 20, x / 10 - 1, y / 10)
               for a in range(21) for x in range(21) for y in range(11)]
    rng = random.Random(25061)
    samples += [(rng.random(), rng.uniform(-1, 1), rng.random()) for _ in range(512)]
    maximum_step = 0
    minimum_volume = math.inf
    all_bounds = [[math.inf, -math.inf] for _ in range(3)]
    face_pairs = same_plane_pairs = 0
    for age, rp, sf in samples:
        w = weights(age, rp, sf)
        assert abs(sum(w) - 1) < 1e-9 and min(w) >= 0
        assert weights(age, rp, -1) == weights(age, rp, 0)
        points = [vertices(m, w) for m in features]
        assert connected(points, head), (age, rp, sf, 'Disconnected head feature')
        examined, coincident = no_same_facing_coplanar([(head_mesh, head)] + list(zip(features, points)))
        face_pairs += examined
        same_plane_pairs += coincident
        for mesh, part in zip(features, points):
            assert not any(overlaps(part, eye) for eye in eyes), (age, rp, sf, mesh['name'], 'Eye collision')
            assert not overlaps(part, orb), (age, rp, sf, mesh['name'], 'Tilted original orb collision')
            for axis, (lo, hi) in enumerate(bounds(part)):
                all_bounds[axis][0] = min(all_bounds[axis][0], lo)
                all_bounds[axis][1] = max(all_bounds[axis][1], hi)
        if tails:
            tail_parts = [vertices(m, w) for m in tails]
            assert connected(tail_parts[1:], tail_parts[0]), (age, rp, sf, 'Detached tail part')
            no_same_facing_coplanar(list(zip(tails, tail_parts)))
        for mesh in boxes:
            minimum_volume = min(minimum_volume, volume(vertices(mesh, w), mesh['faces']))
        assert minimum_volume > 1e-8
        for name, wing in wings:
            values = vertices(wing, w)
            root_ids = (0, 3, 4, 7) if 'left' in name else (1, 2, 5, 6)
            root = [sum(values[i][a] for i in root_ids) / 4 for a in range(3)]
            assert max(abs(a - b) for a, b in zip(root, (0, .25, 0))) < 1e-7, (
                age, rp, sf, name, wing['name'], root, 'Wing hinge drift')
        for side in ('left', 'right'):
            ray_parts = [(m, vertices(m, w)) for name, m in wings if side in name]
            assert connected([p for m, p in ray_parts[1:]], ray_parts[0][1]), (
                age, rp, sf, side, 'Disconnected wing fan')
            no_same_facing_coplanar(ray_parts)
        if age <= .95:
            ahead = [vertices(m, weights(age + .05, rp, sf)) for m in features]
            maximum_step = max(maximum_step, max(abs(a - b) for p, q in zip(points, ahead)
                                                 for u, v in zip(p, q) for a, b in zip(u, v)))
    for name in ('foot_left__cube_0', 'foot_right__cube_0'):
        mesh = next(m for b in data['bones'] if b['name'] == name for m in b['meshes'])
        mesh = copy.deepcopy(mesh)
        mesh['blocky_box_055'] = mesh[FIELD]
        for target in range(10):
            assert abs(max(p[1] for p in endpoint(mesh, target)) - 2) < 1e-7
            assert abs(max(p[2] for p in endpoint(mesh, target))) < 1e-7
    uv_samples = root_samples = distal_samples = 0
    for name, wing in wings:
        count, root_count, distal_count = wing_uv_coverage(name, wing)
        uv_samples += count
        root_samples += root_count
        distal_samples += distal_count
    for name, expected in PROTECTED.items():
        assert hashlib.sha256((MODELS / name).read_bytes()).hexdigest() == expected, ('Prior model changed', name)
    manifest = json.loads((ASSETS / 'neutral_fly061.json').read_text())
    assert manifest['sha256'] == hashlib.sha256(MODEL.read_bytes()).hexdigest()
    assert manifest['source_model_sha256'] == hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    for path, expected in manifest['construction_reference_sha256'].items():
        assert hashlib.sha256((ROOT / path).read_bytes()).hexdigest() == expected
    print(f'NeutralFly061: {len(boxes)} positive closed boxes; {len(samples)} dense/random age/axis blends; '
          f'{len(features)} SAT-connected head features; exact original 7x6x6 head/face/orb/59joints; '
          f'6 distinct mature silhouettes; negative-SF duplicate guard; eye/tilted-orb clearance; '
          f'anchored soles/heels and connected3raywing fans; opaque semantic wingUVs; {face_pairs} arbitrary-rotation face pairs, '
          f'{same_plane_pairs} disjoint same-facing coplanar pairs and ZERO positive-area overlaps. '
          f'Wing actual-face UV samples={uv_samples} (root125={root_samples},distal124={distal_samples}), '
          f'all four long faces per ray visibly cover both opaque semantic texels. '
          f'Minimum positive closed volume={minimum_volume:.9f}; maximum 5%age vertex step={maximum_step:.9f}; '
          f'added-head bounds={all_bounds}; 6 protected older gameplay assets byte-exact.')


if __name__ == '__main__':
    main()
