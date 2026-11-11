#!/usr/bin/env python3
"""Independent final-resource checks against the frozen verified 0.21 control.

This validator never rewrites model resources. It checks the permitted numeric
changes, restores only those fields in an isolated copy, then compares the
entire remaining model with its original control.
"""
from pathlib import Path
import argparse
import copy
import gzip
import hashlib
import json
import math
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
CONTROL = ROOT / 'art/controls/proportions_022_control.json.gz'
KEYS = ('baby', 'swim', 'fly', 'run', 'power', 'hero', 'hero_swim',
        'hero_fly', 'hero_run', 'hero_power', 'dark', 'dark_swim',
        'dark_fly', 'dark_run', 'dark_power')
SHORTEN = frozenset(('hero', 'hero_fly', 'hero_power', 'dark', 'dark_swim', 'dark_run'))
IRIS_FIXES = frozenset(('hero', 'hero_fly', 'hero_power', 'dark_run', 'dark_swim'))


class Checks:
    def __init__(self):
        self.count = 0

    def __call__(self, condition, why):
        self.count += 1
        if not bool(condition):
            raise AssertionError(why)


def bounds(vertices):
    value = np.asarray(vertices, float)
    return np.r_[value.min(axis=0), value.max(axis=0)]


def head(data):
    values = []
    for bi, bone in enumerate(data['bones']):
        if not bone['name'].startswith('skull__cube_'):
            continue
        for mi, mesh in enumerate(bone.get('meshes', [])):
            if mesh.get('material') == 'lens':
                continue
            box = bounds(mesh['vertices'])
            values.append((float(np.prod(box[3:] - box[:3])), bi, mi))
    _, bi, mi = max(values)
    return bi, mi, data['bones'][bi]['meshes'][mi]


def mesh_at(mesh, endpoint=None, age=1):
    young = np.asarray(mesh['vertices'], float)
    if endpoint is None or 'morph_vertices' not in mesh:
        return young
    target = np.asarray(mesh['morph_vertices'][endpoint], float)
    return young + (target - young) * age


def overlaps(a, b, epsilon=1e-5):
    return all(min(a[i + 3], b[i + 3]) + epsilon >= max(a[i], b[i]) for i in range(3))


def local_matrix(bone):
    # Column vectors and the exact PoseStack/JOML order used by the renderer.
    x, y, z = np.radians(bone.get('rotation', [0, 0, 0]))
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    matrix = np.identity(4)
    matrix[:3, :3] = rz @ ry @ rx
    matrix[:3, 3] = bone['pivot']
    return matrix


def cuboids_touch(a, b):
    va, ra = a
    vb, rb = b
    axes = [ra[:, i] for i in range(3)] + [rb[:, i] for i in range(3)]
    axes += [np.cross(ra[:, i], rb[:, j]) for i in range(3) for j in range(3)]
    for axis in axes:
        if np.linalg.norm(axis) < 1e-9:
            continue
        axis = axis / np.linalg.norm(axis)
        pa, pb = va @ axis, vb @ axis
        if min(pa.max(), pb.max()) + 1e-5 < max(pa.min(), pb.min()):
            return False
    return True


def check_limb_assemblies(key, data, check):
    names = {b['name']: b for b in data['bones']}
    anchors = ['arm_left', 'arm_right', 'foot_left', 'foot_right' if 'foot_right' in names else 'feet_bone_right']
    for endpoint in [None, *range(9)]:
        def matrix(name):
            bone = names[name]
            if name == 'body':
                return np.identity(4)
            return matrix(bone['parent']) @ local_matrix(bone)

        def boxes(bones):
            output = []
            for bone in bones:
                transform = matrix(bone['name'])
                for mesh in bone.get('meshes', []):
                    vertices = mesh_at(mesh, endpoint)
                    points = (transform @ np.c_[vertices, np.ones(len(vertices))].T).T[:, :3]
                    output.append((points, transform[:3, :3]))
            return output

        for anchor in anchors:
            check(names[anchor]['parent'] == 'body', key + ': limb semantic parent changed ' + anchor)
            limb = boxes([b for b in data['bones'] if b['name'].startswith(anchor + '__cube_')])
            check(bool(limb), key + ': missing limb ' + anchor)
            # Check internal upper-arm/hand and multi-piece-foot connections.
            # Torso contact also depends on the actual animated shoulder pose;
            # its joints/geometry are separately required to match the control.
            nodes = limb
            reached = {0}
            changed = True
            while changed:
                changed = False
                for index, candidate in enumerate(nodes):
                    if index not in reached and any(cuboids_touch(candidate, nodes[other]) for other in reached):
                        reached.add(index)
                        changed = True
            check(len(reached) == len(nodes), key + ': detached limb/hand ' + anchor + ' endpoint ' + str(endpoint))
    return len(anchors) * 10


def check_iris(key, data, baseline, assets, check):
    donor = data['source_uv_layout']
    with Image.open(assets / f'textures/entity/chao/official/{donor}.png') as source_image:
        image = source_image.convert('RGBA')
    with Image.open(assets / f'textures/entity/chao/gameplay/masks/{donor}.png') as source_mask:
        mask = source_mask.convert('RGBA')
    original_eyes = []
    for bone in baseline['bones']:
        if not bone['name'].startswith('skull__'):
            continue
        for mesh in bone['meshes']:
            if mesh.get('material') == 'lens':
                continue
            box = bounds(mesh['vertices'])
            size = box[3:] - box[:3]
            if 1.8 <= size[0] <= 2.5 and 1.8 <= size[1] <= 2.5 and .8 <= size[2] <= 1.5:
                original_eyes.extend(mesh['faces'])
    for bone in data['bones']:
        for mesh in bone['meshes']:
            if mesh.get('name') != 'colored_iris' or mesh.get('material') != 'lens':
                continue
            uv = mesh['faces'][0]['uv'][0]
            check(all(point == uv for face in mesh['faces'] for point in face['uv']), key + ': iris no longer samples one white source texel')
            u, v = uv
            inside = any(min(p[0] for p in face['uv']) <= u < max(p[0] for p in face['uv'])
                         and min(p[1] for p in face['uv']) <= v < max(p[1] for p in face['uv']) for face in original_eyes)
            check(inside, key + ': genetic iris sample lies outside original eye UVs')
            x, y = int(u * image.width / data['texture_width']), int(v * image.height / data['texture_height'])
            pixel = image.getpixel((x, y))
            check(pixel[3] == 255 and min(pixel[:3]) > 205 and max(pixel[:3]) - min(pixel[:3]) < 22,
                  key + ': genetic iris sample is not opaque neutral eye white')
            x, y = int(u * mask.width / data['texture_width']), int(v * mask.height / data['texture_height'])
            check(mask.getpixel((x, y))[3] == 0, key + ': genetic iris sample is recolored by the skin mask')


def validate_model(key, data, baseline, assets, check):
    copy_for_control = copy.deepcopy(data)
    bi, mi, original_head = head(baseline)
    actual_head = data['bones'][bi]['meshes'][mi]
    original_box = bounds(original_head['vertices'])
    target_height = min(6., original_box[4] - original_box[1])
    check(data['bones'][bi]['name'] == baseline['bones'][bi]['name'], key + ': core anchor changed')
    metadata = data.get('proportion_adjustment_022')
    if key in SHORTEN:
        check(isinstance(metadata, dict), key + ': missing proportion metadata')
        check(set(metadata) == {'release', 'core_bone', 'source_height', 'target_height', 'source_bounds', 'target_bounds', 'marker_young_delta', 'marker_target_deltas'}, key + ': unrecognized proportion metadata')
        check(metadata['release'] == '0.22.0' and metadata['core_bone'] == baseline['bones'][bi]['name'], key + ': wrong metadata domain')
        check(metadata['source_height'] == 7 and metadata['target_height'] == 6, key + ': incorrect target height')
        source_arrays = [original_head['vertices'], *original_head['morph_vertices']]
        actual_arrays = [actual_head['vertices'], *actual_head['morph_vertices']]
        drops = []
        for original_vertices, actual_vertices in zip(source_arrays, actual_arrays):
            before = np.asarray(original_vertices, float)
            after = np.asarray(actual_vertices, float)
            box = bounds(before)
            expected = before.copy()
            expected[:, 1] = box[4] - (box[4] - before[:, 1]) * 6 / 7
            check(np.allclose(after, expected, atol=5.01e-7, rtol=0), key + ': head correction is not an anchored 7-to-6 normalization')
            check(np.array_equal(before[:, [0, 2]], after[:, [0, 2]]), key + ': head width/front/depth changed')
            drops.append(round(float(bounds(after)[1] - box[1]), 6))
        check(np.allclose(metadata['source_bounds'], original_box, atol=1e-6), key + ': source head control incorrect')
        check(np.allclose(metadata['target_bounds'], bounds(actual_head['vertices']), atol=1e-6), key + ': target head metadata incorrect')
        check(abs(metadata['marker_young_delta'] - drops[0]) < 1e-6 and np.allclose(metadata['marker_target_deltas'], drops[1:], atol=1e-6), key + ': marker target offset incorrect')
        copy_for_control['bones'][bi]['meshes'][mi]['vertices'] = copy.deepcopy(original_head['vertices'])
        copy_for_control['bones'][bi]['meshes'][mi]['morph_vertices'] = copy.deepcopy(original_head['morph_vertices'])
        old_crest = next(b for b in baseline['bones'] if b['name'] == 'gradual_head_021')
        new_crest = next(b for b in data['bones'] if b['name'] == 'gradual_head_021')
        restored_crest = next(b for b in copy_for_control['bones'] if b['name'] == 'gradual_head_021')
        for old_mesh, new_mesh, restored_mesh in zip(old_crest['meshes'], new_crest['meshes'], restored_crest['meshes']):
            for before, after, drop in zip([old_mesh['vertices'], *old_mesh['morph_vertices']],
                                           [new_mesh['vertices'], *new_mesh['morph_vertices']], drops):
                expected = np.asarray(before, float) + np.array([0, drop, 0])
                check(np.allclose(after, expected, atol=5.01e-7, rtol=0), key + ': crest was scaled or moved independently from its crown')
                check(np.allclose(bounds(after)[3:] - bounds(after)[:3], bounds(before)[3:] - bounds(before)[:3], atol=1e-6), key + ': crest block length changed')
            restored_mesh['vertices'] = copy.deepcopy(old_mesh['vertices'])
            restored_mesh['morph_vertices'] = copy.deepcopy(old_mesh['morph_vertices'])
        old_monster = next(b for b in baseline['bones'] if b['name'] == 'monster_crown')
        new_monster = next(b for b in data['bones'] if b['name'] == 'monster_crown')
        check(np.allclose(new_monster['pivot'], np.asarray(old_monster['pivot']) + [0, drops[0], 0], atol=1e-6), key + ': optional crown not reanchored')
        next(b for b in copy_for_control['bones'] if b['name'] == 'monster_crown')['pivot'] = copy.deepcopy(old_monster['pivot'])
        del copy_for_control['proportion_adjustment_022']
    else:
        check(metadata is None, key + ': untouched family received a correction')
    check_iris(key, data, baseline, assets, check)
    if key in IRIS_FIXES:
        for old_bone, restored_bone in zip(baseline['bones'], copy_for_control['bones']):
            for old_mesh, restored_mesh in zip(old_bone['meshes'], restored_bone['meshes']):
                if old_mesh.get('name') == 'colored_iris' and old_mesh.get('material') == 'lens':
                    for old_face, restored_face in zip(old_mesh['faces'], restored_mesh['faces']):
                        restored_face['uv'] = copy.deepcopy(old_face['uv'])
    check(copy_for_control == baseline, key + ': changed a field outside approved core/crest/marker/iris UV corrections')
    crest = next(b for b in data['bones'] if b['name'] == 'gradual_head_021')
    for endpoint in range(9):
        for age in (0, .5, 1):
            box = bounds(mesh_at(actual_head, endpoint, age))
            scale = (box[4] - box[1]) / target_height
            check(.9199 <= scale <= 1.1101, key + ': excessive final age proportions')
            check(abs(box[2] - original_box[2]) < 1e-6 and abs(box[4] - original_box[4]) < 1e-6, key + ': front/lower head plane moved')
            skull_box = box + np.r_[data['bones'][bi]['pivot'], data['bones'][bi]['pivot']]
            for slot in range(6):
                pieces = [bounds(mesh_at(m, endpoint, age)) for m in crest['meshes'][slot * 6:slot * 6 + 6]]
                check(overlaps(pieces[0], skull_box), key + ': detached crest root')
                check(all(overlaps(a, b) for a, b in zip(pieces, pieces[1:])), key + ': detached stepped crest')
                check(all(np.all(piece[3:] > piece[:3]) for piece in pieces), key + ': degenerate crest block')
    return check_limb_assemblies(key, data, check)


def self_test(models, controls, assets, check):
    rejected = []
    def mutation(label, change):
        broken = copy.deepcopy(models['hero'])
        change(broken)
        try:
            validate_model('hero', broken, controls['hero'], assets, Checks())
        except (AssertionError, KeyError, ValueError):
            rejected.append(label)
        else:
            raise AssertionError('Validator accepted corruption: ' + label)

    def core_mesh(d):
        return head(d)[2]
    mutation('seven-pixel head restored', lambda d: core_mesh(d)['vertices'][0].__setitem__(1, -7))
    mutation('front plane moved', lambda d: core_mesh(d)['vertices'][0].__setitem__(2, -4))
    mutation('marker age mismatch', lambda d: d['proportion_adjustment_022']['marker_target_deltas'].__setitem__(0, 0))
    mutation('crest length changed', lambda d: next(b for b in d['bones'] if b['name'] == 'gradual_head_021')['meshes'][0]['vertices'][0].__setitem__(1, -30))
    mutation('hand detached', lambda d: next(b for b in d['bones'] if b['name'] == 'arm_left__cube_1')['meshes'][0]['vertices'][0].__setitem__(0, 40))
    mutation('native eye displaced', lambda d: next(m for b in d['bones'] if b['name'].startswith('skull__') for m in b['meshes'] if m.get('name') == 'colored_iris')['vertices'][0].__setitem__(1, -9))
    mutation('head UV changed', lambda d: core_mesh(d)['faces'][0]['uv'][0].__setitem__(0, 0))
    check(len(rejected) == 7, 'Missing rejected corruption fixture')
    # Independent two-axis quarter turn: Rx first sends +Y to +Z, then Rz
    # leaves +Z fixed. The formerly reversed composition sends it to -X.
    rotation = local_matrix({'pivot': [0, 0, 0], 'rotation': [90, 0, 90]})
    check(np.allclose(rotation @ [0, 1, 0, 1], [0, 0, 1, 1], atol=1e-8), 'Cuboid SAT rotation order differs from rendering')
    return rejected


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test', action='store_true')
    parser.add_argument('--output', type=Path, default=ROOT / 'docs/research/PROPORTIONS_022_INDEPENDENT_CHECKS.json')
    args = parser.parse_args()
    frozen = json.loads(gzip.decompress(CONTROL.read_bytes()))
    check = Checks()
    check(frozen['baseline_release'] == '0.21.0' and len(frozen['models']) == 15, 'Incorrect baseline controls')
    models = {key: json.loads((MODELS / f'chao_{key}.json').read_text()) for key in KEYS}
    limb_cases = sum(validate_model(key, models[key], frozen['models'][key], ASSETS, check) for key in KEYS)
    for category in ('raw_inputs', 'special_files', 'chaos_coats', 'chaos_masks'):
        for relative, digest in frozen[category].items():
            check(hashlib.sha256((ROOT / relative).read_bytes()).hexdigest() == digest, category + ' changed: ' + relative)
    rejected = self_test(models, frozen['models'], ASSETS, check) if args.self_test else []
    report = {'release': '0.22.0', 'result': 'PASS', 'checks': check.count, 'age_endpoint_cases': 15 * 9 * 3,
              'limb_assembly_cases': limb_cases, 'normalized_families': sorted(SHORTEN),
              'preserved_special_files': len(frozen['special_files']), 'preserved_chaos_coats': len(frozen['chaos_coats']),
              'preserved_raw_inputs': len(frozen['raw_inputs']), 'rejected_corruptions': rejected,
              'control_sha256': hashlib.sha256(CONTROL.read_bytes()).hexdigest(),
              'scope': 'Independent final JSON geometry, rigid limb contacts, fixed native anchors and original source hashes; actual renderer and material behavior is checked separately in Minecraft.'}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    print(f'Proportions022: {check.count} checks PASS; 405 ordinary age cases, {limb_cases} limb assemblies, 10 preserved special files, 351 coats and 41 raw inputs.')


if __name__ == '__main__':
    main()
