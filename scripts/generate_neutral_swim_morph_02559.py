#!/usr/bin/env python3
"""Author the blocky Neutral/Swim first-type family on the original baby rig.

The live viewer supplies silhouettes, not geometry. Every added surface is a
closed authored cuboid; the original 7x6x6 head, eyes and joints remain exact.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_neutral_swim_proto059.json'
TARGETS = ['neutral_swim_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_guard', 'balanced_guard', 'power_guard']
BODY_UV = [63.75, 31.75]
TIP_UV = [63.25, 31.75]
FIN_UV = [62.75, 31.75]
UNDER_UV = [62.25, 31.75]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rounded(values):
    return [round(float(x), 9) for x in values]


def targets(mesh, poses):
    assert len(poses) == 10
    mesh['neutral_swim_box_059'] = {
        field: [rounded(p[key]) for p in poses]
        for field, key in [('centers', 'center'), ('sizes', 'size'), ('rotations', 'rotation')]}
    return mesh


def lattice(profiles):
    # Positive Fly is deliberately a duplicate of the balanced-Swim row. The
    # policy clamps net Swim/Fly to [-1,0], and this authored guard is safe if
    # a future caller accidentally supplies the unused positive coordinate.
    return [copy.deepcopy(profiles[name]) for name in
            ('young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
             'power', 'run', 'balanced', 'power')]


def feature(template, name, poses, region, uv=BODY_UV, underside=False):
    mesh = copy.deepcopy(template)
    for key in ('morph_vertices', 'hero_run_vertices_052', 'hero_run_legacy_vertices_052'):
        mesh.pop(key, None)
    mesh['name'] = name
    mesh['vertices'] = vertices(template, poses[0])
    mesh['neutral_swim_region_059'] = region
    for index, face in enumerate(mesh['faces']):
        # Rear and lower faces of the sloped hood form its pale trailing hem.
        # This is painted onto existing closed faces, not an extra floating
        # decorative sheet or a coincident second shell.
        selected = UNDER_UV if underside and index in (1, 5) else uv
        face['uv'] = [selected[:] for _ in face['v']]
    return targets(mesh, poses)


def cap_features(template):
    # Personally compared the live viewer's six first-Swim mature forms and
    # its shared fresh form. The fresh cap ALREADY has a rear/down overhang.
    # Three deliberately plain blocks interpret its broad top/shoulder and
    # posterior hood. None is a replacement skull or a Normal-first tuft.
    # width,height,depth,centerY,centerZ,pitchX for each closed cap section.
    profiles = {
        'young': ((6.4, 1.0, 5.3, -6.05, .15, 0),
                  (4.6, .85, 3.7, -6.65, .10, 0),
                  (5.5, 1.25, 3.3, -4.3, 3.25, -35)),
        'balanced': ((6.6, 1.05, 5.45, -6.05, .20, 0),
                     (4.8, .90, 3.8, -6.70, .15, 0),
                     (5.8, 1.35, 3.7, -3.9, 3.5, -40)),
        'run': ((6.3, .90, 5.35, -6.0, .25, 0),
                (4.6, .70, 3.7, -6.55, .20, 0),
                (5.4, 1.25, 4.6, -5.1, 3.9, 15)),
        'power': ((6.7, 1.15, 5.25, -6.1, .10, 0),
                  (5.0, 1.0, 3.8, -6.8, .0, 0),
                  (5.75, 1.45, 2.65, -4.6, 3.35, -20)),
        'swim': ((6.7, 1.25, 5.3, -6.10, .25, 0),
                 (4.9, 1.15, 3.9, -6.85, .15, 0),
                 (6.05, 1.4, 5.6, -2.5, 3.6, -56)),
        'run_swim': ((6.4, 1.05, 5.45, -6.05, .25, 0),
                     (4.7, .85, 3.8, -6.7, .15, 0),
                     (5.65, 1.25, 4.45, -3.5, 3.65, -34)),
        'power_swim': ((6.7, 1.2, 5.45, -6.10, .20, 0),
                       (5.0, 1.1, 3.9, -6.8, .10, 0),
                       (5.95, 1.45, 4.4, -3.3, 3.45, -45)),
    }
    result = []
    for index, name in enumerate(('swim_cap_roof', 'swim_cap_upper', 'swim_cap_rear')):
        values = {}
        for key, sections in profiles.items():
            width, height, depth, y, z, pitch = sections[index]
            values[key] = pose([0, y, z], [width, height, depth], [pitch, 0, 0])
        result.append(feature(template, name, lattice(values),
                              'crown_root' if index < 2 else 'crown_underside',
                              underside=index == 2))
    return result


def side_fins(template):
    # Two positive-volume cuboids per ear/flipper, on the upper OUTSIDE head
    # sides. Their roots stay behind the untouched face panels. Run rises,
    # Power remains short/thick, Swim hangs down/back, and combined corners
    # are shorter mixed forms rather than stacking maximum endpoint parts.
    # root length,height,depth,yaw,downward lean; same five for the distal part.
    profiles = {
        'young': (1.1, .70, 1.25, -45, 0, 1.1, .50, 1.0, -65, 0),
        'balanced': (1.25, .85, 1.35, -45, 5, 1.5, .65, 1.05, -65, 10),
        'run': (1.55, .65, 1.10, -40, -35, 1.65, .45, .85, -60, -55),
        'power': (1.25, 1.5, 1.7, -35, -8, 1.05, 1.15, 1.4, -60, -5),
        'swim': (2.5, 1.15, 1.55, -30, 55, 3.4, .90, 1.4, -35, 75),
        'run_swim': (1.9, .95, 1.3, -35, 45, 2.6, .70, 1.15, -45, 65),
        'power_swim': (2.0, 1.25, 1.6, -30, 45, 2.8, .90, 1.35, -45, 65),
    }
    roots, tips = {}, {}
    for key, values in profiles.items():
        length, height, depth, yaw, down, upper, uh, ud, uy, uz = values
        base = [3.20, -3.80, -.30]
        root_rotation = [0, yaw, down]
        root_offset = rotate([length / 2, 0, 0], root_rotation)
        root = pose([a + b for a, b in zip(base, root_offset)],
                    [length, height, depth], root_rotation)
        neck = [a + b for a, b in zip(base, rotate([length, 0, 0], root_rotation))]
        tip_rotation = [0, uy, uz]
        # A substantial shared volume, rather than touching edges, keeps the
        # two rigid sections connected while their Euler targets interpolate.
        tip_offset = rotate([upper / 2 - .34, 0, 0], tip_rotation)
        tip = pose([a + b for a, b in zip(neck, tip_offset)],
                   [upper, uh, ud], tip_rotation)
        roots[key] = root
        tips[key] = tip
    result = []
    for sign, side in ((-1, 'left'), (1, 'right')):
        for section, profiles in [('root', roots), ('tip', tips)]:
            mirrored = {}
            for key, value in profiles.items():
                p = copy.deepcopy(value)
                p['center'][0] *= sign
                p['rotation'][1] *= sign
                p['rotation'][2] *= sign
                mirrored[key] = p
            mesh = feature(template, 'swim_side_fin_' + side + '_' + section,
                           lattice(mirrored), 'side_fin_' + section, FIN_UV)
            # A restrained pale edge and green distal end suggest the viewer's
            # colored leaf without adding zigzag bands or sculpted segments.
            for face_index in (4, 5):
                mesh['faces'][face_index]['uv'] = [UNDER_UV[:] for _ in range(4)]
            if section == 'tip':
                # The sign mirror changes geometry, not the template's local
                # positive-X terminal vertex correspondence.
                mesh['faces'][3 if sign == 1 else 2]['uv'] = [BODY_UV[:] for _ in range(4)]
            result.append(mesh)
    return result


def limbs(data):
    selected = {'body__cube_0', 'arm_left__cube_1', 'arm_right__cube_1',
                'foot_left__cube_0', 'foot_right__cube_0'}
    for bone in data['bones']:
        name = bone['name']
        if name not in selected:
            continue
        for mesh in bone.get('meshes', []):
            base = original_box(mesh)
            profiles = {}
            for key, rp, sf in [('young', 0, 0), ('balanced', 0, 0), ('run', -1, 0),
                                ('power', 1, 0), ('swim', 0, -1),
                                ('run_swim', -1, -1), ('power_swim', 1, -1)]:
                run = max(0, -rp)
                power = max(0, rp)
                swim = max(0, -sf)
                p = copy.deepcopy(base)
                if key == 'young':
                    scale = [1, 1, 1]
                elif name.startswith('body'):
                    scale = [1.025 + .095 * power, 1.015 + .025 * power,
                             1.025 + .035 * swim + .035 * power]
                elif name.startswith('arm'):
                    scale = [1.035 + .145 * power, 1.025 + .12 * power,
                             1.035 + .10 * power + .02 * swim]
                else:
                    scale = [1.035 + .06 * swim + .035 * power,
                             1 + .035 * power, 1.04 + .09 * run + .04 * power + .035 * swim]
                p['size'] = [a * b for a, b in zip(base['size'], scale)]
                if name.startswith('body'):
                    # Follow the scoped adult walking head lift with the
                    # existing torso top; keep its lower support plane exact.
                    p['center'][1] -= .225
                    p['size'][1] += .45
                if name.startswith('foot'):
                    # Sole and heel remain in the original animation rig.
                    p['center'][1] = max(v[1] for v in mesh['vertices']) - p['size'][1] / 2
                    p['center'][2] = max(v[2] for v in mesh['vertices']) - p['size'][2] / 2
                profiles[key] = p
            values = lattice(profiles)
            if name.startswith('body'):
                # The rigid-box parser derives corner correspondence from
                # target0. Update only its four upper reference vertices to
                # match that same torso extension; lower vertices stay exact.
                mesh['vertices'] = vertices(mesh, values[0])
            targets(mesh, values)


def wings(data):
    # Exact .58 construction fix: both sides sample the visible original
    # front artwork; thin closing edges follow its per-vertex alpha cutout.
    profiles = {'young': (3, 2), 'balanced': (3.3, 2.05), 'run': (3.65, 1.75),
                'power': (2.8, 2.1), 'swim': (3.2, 1.8),
                'run_swim': (3.6, 1.8), 'power_swim': (2.9, 2.0)}
    for bone in data['bones']:
        if bone['name'] not in ('wing_left__cube_0', 'wing_right__cube_0'):
            continue
        sign = -1 if 'left' in bone['name'] else 1
        for mesh in bone.get('meshes', []):
            for key in ('morph_vertices', 'hero_run_vertices_052', 'hero_run_legacy_vertices_052'):
                mesh.pop(key, None)
            original = copy.deepcopy(mesh)
            for i, vertex in enumerate(mesh['vertices']):
                vertex[2] = -.08 if i < 4 else .08
            visible = original['faces'][0]
            uv = {i: visible['uv'][j] for j, i in enumerate(visible['v'])}
            mesh['faces'][1]['uv'] = [copy.deepcopy(uv[i % 4]) for i in mesh['faces'][1]['v']]
            front = visible['v']
            for a, b in zip(front, front[1:] + front[:1]):
                ids = [b, a, a + 4, b + 4]
                edge_uv = [[max(16.125, min(18.875, uv[i % 4][0])),
                            max(17.125, min(18.875, uv[i % 4][1]))] for i in ids]
                mesh['faces'].append({'v': ids, 'uv': edge_uv})
            p = {key: pose([sign * span / 2, .25, 0], [span, height, .16])
                 for key, (span, height) in profiles.items()}
            values = lattice(p)
            mesh['vertices'] = vertices(mesh, values[0])
            mesh['neutral_swim_region_059'] = 'wing'
            targets(mesh, values)


def author():
    source = MODELS / 'chao_baby.json'
    data = json.loads(source.read_text())
    original = copy.deepcopy(data)
    bones = {b['name']: b for b in data['bones']}
    template = bones['skull__cube_0']['meshes'][0]
    bones['gradual_head_021']['meshes'] = cap_features(template) + side_fins(template)
    limbs(data)
    wings(data)
    data['authored_gameplay_form'] = 'neutral_swim_proto059'
    data['neutral_swim_path_059'] = {
        'targets': TARGETS,
        'canonical_uv_layout': 'baby',
        'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(clamp(swimFly,-1,0),y); positive Fly guard row duplicates center',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv': BODY_UV, 'feature_tip_uv': TIP_UV,
        'feature_fin_uv': FIN_UV, 'feature_underside_uv': UNDER_UV,
        'reserved_pixels': {'feature_body': [127, 63], 'feature_tip': [126, 63],
                            'feature_fin': [125, 63], 'feature_underside': [124, 63]},
        'scope': 'Ordinary living Neutral adult with locked Swim FIRST evolution. Six balanced/Run/Power/Swim/Run+Swim/Power+Swim specialization targets. No positive Fly specialization.',
        'reference': 'Independently operated Chao Island viewer first-Swim checkbox: all six mature front, both sides and back, above and below, shared fresh form and intermediate age comparisons. Original official Swim cuboids/fins supply block construction vocabulary only.',
        'source_geometry_copied': False,
        'approximation': 'Unchanged square head with a broad three-box stepped cap/rear hood and two closed cuboids per temple fin. The fresh posterior cap is already substantial. Run raises its fins and rear cap, Power has short thick fins and a blunt hood, Swim has a long dropped hood and hanging flippers; mixed endpoints are intermediate forms. Curved hems, tapered leaves and zigzag bands are simplified into closed angled boxes and flat per-face color regions.',
        'wing_policy': 'Thin closed original UV wings, visible front mirrored onto back, alpha-consistent closing edges, exact local root-edge midpoint [0,.25,0], original parent yaw/pivots/animations.',
        'provisional_rates': '960 current level-bar progress per opposing axis; independent adult age. No original-game timing claim.'}
    actual = {b['name']: b for b in data['bones']}
    assert len(data['bones']) == len(original['bones'])
    for bone in original['bones']:
        name = bone['name']
        assert actual[name]['pivot'] == bone['pivot']
        assert actual[name]['rotation'] == bone['rotation']
        assert actual[name].get('parent') == bone.get('parent')
        if name.startswith(('skull', 'expression_', 'emotion_')):
            assert actual[name] == bone, name
    OUT.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    refs = ['art/imported_chao/models/chao_swim.geo.json',
            'art/imported_chao/textures/swim.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_swim.json']
    manifest = {
        'version': '0.25.59', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_emotion_and_joint_pivots_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': ['balanced', 'run', 'power', 'swim', 'run_swim', 'power_swim'],
        'guard_row': 'positive Fly targets exactly duplicate center; runtime net Swim/Fly clamps [-1,0]'}
    (ASSETS / 'neutral_swim059.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('NeutralSwim059 generated: exact original 7x6x6 head/face/59 joints; 7 head features, 5 compact body/limb boxes, 2 closed anchored wings; 10 targets and 6 legal mature forms.')


if __name__ == '__main__':
    author()
