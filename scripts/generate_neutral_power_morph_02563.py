#!/usr/bin/env python3
"""Authored blocky Neutral/Power-first family on the unchanged original rig.

The live Power-selected viewer supplies anatomy and development directions, never
geometry. A central upper crown, ONE axial lower rear point and two front-side
horns are separate closed boxes. The original skull, pixel eyes, orb and all
59 animation joints remain exact. Four simple point swatches color broad caps.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_neutral_power_proto063.json'
TARGETS = ['neutral_power_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
BODY_UV = [63.75, 31.75]
TIP_UV = [63.25, 31.75]
FIN_UV = [62.75, 31.75]
UNDER_UV = [62.25, 31.75]
TAIL_UV = [25.25, 16.25]
FIELD = 'neutral_power_box_063'
REGION = 'neutral_power_region_063'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rounded(values):
    return [round(float(x), 9) for x in values]


def lattice(profiles):
    return [copy.deepcopy(profiles[name]) for name in NAMES]


def targets(mesh, poses, region):
    assert len(poses) == 10
    mesh[FIELD] = {field: [rounded(p[key]) for p in poses]
                   for field, key in [('centers', 'center'), ('sizes', 'size'), ('rotations', 'rotation')]}
    mesh[REGION] = region
    return mesh


def feature(template, name, poses, region, uv=BODY_UV):
    mesh = copy.deepcopy(template)
    for key in ('morph_vertices', 'hero_run_vertices_052', 'hero_run_legacy_vertices_052'):
        mesh.pop(key, None)
    mesh['name'] = name
    mesh['vertices'] = vertices(template, poses[0])
    for face in mesh['faces']:
        face['uv'] = [uv[:] for _ in face['v']]
    return targets(mesh, poses, region)


def chain(anchor, length, width, height, rotation, tip_length, tip_width,
          tip_height, tip_rotation, overlap=.65):
    """Two positive closed boxes with substantial authored shared volume."""
    root = pose([a + b for a, b in zip(anchor, rotate([0, 0, length / 2], rotation))],
                [width, height, length], rotation)
    end = [a + b for a, b in zip(anchor, rotate([0, 0, length], rotation))]
    tip = pose([a + b for a, b in zip(end, rotate([0, 0, tip_length / 2 - overlap], tip_rotation))],
               [tip_width, tip_height, tip_length], tip_rotation)
    return root, tip


def head_features(template):
    # Actual first-Power anatomy is not first-Run's mirrored rear spikes/nubs.
    # These two central sections form an upper axial crown. Fly's terminal
    # pitch passes90deg, bending UP/FORWARD toward the face; Run bends DOWN/BACK.
    central = {
        'young': (3.00, 1.85, 2.65, 47, 1.55, 1.05, 1.25, 47),
        'balanced': (3.25, 1.95, 3.30, 49, 1.65, 1.10, 1.45, 58),
        'run': (3.20, 2.00, 4.10, 13, 1.90, 1.20, 2.05, -38),
        'power': (3.60, 2.10, 3.75, 48, 2.20, 1.35, 1.90, 48),
        'fly': (2.75, 1.65, 3.60, 66, 1.30, .85, 1.80, 112),
        'swim': (3.75, 1.85, 4.10, 2, 2.00, 1.15, 1.70, -7),
        'run_swim': (3.55, 1.80, 4.00, 3, 1.85, 1.10, 1.85, -20),
        'run_fly': (3.00, 1.80, 3.25, 48, 1.60, 1.00, 1.50, 36),
        'power_swim': (3.40, 1.90, 3.50, 28, 1.85, 1.15, 1.45, 52),
        'power_fly': (3.20, 1.90, 3.50, 58, 1.75, 1.10, 1.75, 78),
    }
    roots, tips = {}, {}
    for key, (w, h, length, pitch, tw, th, tl, tp) in central.items():
        roots[key], tips[key] = chain([0, -5.40, .90], length, w, h, [pitch, 0, 0],
                                      tl, tw, th, [tp, 0, 0], overlap=.70)
    result = [feature(template, 'power_crown_root', lattice(roots), 'crown_root'),
              feature(template, 'power_crown_tip', lattice(tips), 'crown_tip', TIP_UV)]

    # One midline LOWER rear point is visible as a diamond cap from behind.
    # Balanced lengthens it down/back; Fly raises its base/terminal direction;
    # fullPower shortens it. This never becomes two mirrored Run rear hooks.
    rear = {
        'young': (3.05, 2.10, 1.70, 10, 1.65, 1.30, 1.05, -5),
        'balanced': (3.35, 2.35, 2.50, -2, 1.90, 1.50, 1.70, -22),
        'run': (3.30, 2.40, 2.85, -10, 1.90, 1.55, 2.00, -42),
        'power': (2.85, 2.00, 1.50, 8, 1.50, 1.20, 1.10, 8),
        'fly': (3.10, 2.15, 3.00, 33, 1.70, 1.30, 1.70, 25),
        'swim': (3.45, 2.10, 2.10, -3, 1.80, 1.25, 1.20, -15),
        'run_swim': (3.20, 2.10, 2.25, -8, 1.70, 1.25, 1.45, -28),
        'run_fly': (3.15, 2.20, 2.40, 19, 1.75, 1.35, 1.50, 5),
        'power_swim': (3.05, 2.05, 2.00, 3, 1.65, 1.25, 1.20, 0),
        'power_fly': (3.00, 2.10, 2.50, 25, 1.60, 1.30, 1.50, 20),
    }
    roots, tips = {}, {}
    for key, (w, h, length, pitch, tw, th, tl, tp) in rear.items():
        roots[key], tips[key] = chain([0, -2.20, 2.10], length, w, h, [pitch, 0, 0],
                                      tl, tw, th, [tp, 0, 0], overlap=.70)
    result += [feature(template, 'power_rear_root', lattice(roots), 'rear_root'),
               feature(template, 'power_rear_tip', lattice(tips), 'rear_tip', TIP_UV)]

    # Two substantial front-side horns, not tiny decorative nubs. Swim rotates
    # them outward into small fins; Fly lengthens thin upright horns. Mature
    # fullPower instead thickens and sweeps them back along the crown.
    horns = {
        'young': (1.35, 1.10, 2.15, 55, 25, 25, .85, .75, 1.05, 60, 18, 20, -4.85),
        'balanced': (.85, .90, 1.90, 75, 10, 15, .55, .65, 1.05, 83, 8, 10, -4.75),
        'run': (.80, .85, 2.20, 76, 8, 12, .50, .60, 1.25, 90, 5, 8, -4.75),
        'power': (1.50, 1.30, 2.65, 55, 10, 15, .90, .85, 1.25, 62, 8, 18, -4.75),
        'fly': (.65, .70, 2.45, 85, 5, 8, .40, .50, 1.55, 97, 3, 5, -4.75),
        'swim': (.75, .65, 1.35, -8, 75, 0, .45, .45, .85, -12, 78, 0, -3.80),
        'run_swim': (.38, .40, .72, -3, 83, 0, .25, .28, .50, 2, 85, 0, -4.10),
        'run_fly': (.75, .80, 2.15, 78, 8, 11, .48, .55, 1.15, 88, 5, 7, -4.75),
        'power_swim': (.65, .60, 1.05, 0, 70, 0, .40, .40, .70, 8, 76, 0, -4.15),
        'power_fly': (1.15, 1.10, 2.45, 67, 8, 12, .70, .70, 1.20, 78, 6, 10, -4.75),
    }
    for sign, side in ((-1, 'left'), (1, 'right')):
        roots, tips = {}, {}
        for key, (w, h, length, pitch, yaw, roll, tw, th, tl, tp, ty, tr, y) in horns.items():
            roots[key], tips[key] = chain([sign * (3.0 if key == 'young' else 2.75), y, -1.15], length, w, h,
                                          [pitch, sign * yaw, sign * roll], tl, tw, th,
                                          [tp, sign * ty, sign * tr], overlap=.48)
        result += [feature(template, 'power_horn_' + side + '_root', lattice(roots), 'horn_root'),
                   feature(template, 'power_horn_' + side + '_tip', lattice(tips), 'horn_tip', TIP_UV)]
    return result


def limbs(data):
    # Compact fist/foot growth preserves cohesive cuboids and original anchors.
    scales = {
        'young': ((1.025, 1.00, 1.025), (1.035, 1.035, 1.035), (1.025, 1.00, 1.025)),
        'balanced': ((1.06, 1.025, 1.06), (1.10, 1.10, 1.10), (1.065, 1.025, 1.065)),
        'run': ((1.04, 1.04, 1.04), (1.065, 1.07, 1.065), (1.055, 1.01, 1.13)),
        'power': ((1.10, 1.045, 1.09), (1.18, 1.17, 1.15), (1.10, 1.05, 1.10)),
        'fly': ((1.035, 1.045, 1.035), (1.10, 1.08, 1.10), (1.06, 1.015, 1.07)),
        'swim': ((1.075, 1.015, 1.08), (1.11, 1.09, 1.10), (1.15, 1.025, 1.13)),
        'run_swim': ((1.055, 1.025, 1.06), (1.085, 1.075, 1.08), (1.12, 1.02, 1.15)),
        'run_fly': ((1.04, 1.04, 1.04), (1.08, 1.08, 1.08), (1.06, 1.015, 1.10)),
        'power_swim': ((1.085, 1.035, 1.085), (1.14, 1.13, 1.12), (1.14, 1.04, 1.12)),
        'power_fly': ((1.075, 1.045, 1.065), (1.14, 1.12, 1.12), (1.085, 1.035, 1.09)),
    }
    selected = {'body__cube_0', 'arm_left__cube_1', 'arm_right__cube_1',
                'foot_left__cube_0', 'foot_right__cube_0'}
    for bone in data['bones']:
        name = bone['name']
        if name not in selected:
            continue
        for mesh in bone.get('meshes', []):
            base = original_box(mesh)
            profiles = {}
            for key in NAMES:
                p = copy.deepcopy(base)
                index = 0 if name.startswith('body') else 1 if name.startswith('arm') else 2
                p['size'] = [a * b for a, b in zip(base['size'], scales[key][index])]
                if name.startswith('body'):
                    p['center'][1] -= .225
                    p['size'][1] += .45
                if name.startswith('foot'):
                    p['center'][1] = max(v[1] for v in mesh['vertices']) - p['size'][1] / 2
                    p['center'][2] = max(v[2] for v in mesh['vertices']) - p['size'][2] / 2
                profiles[key] = p
            poses = lattice(profiles)
            mesh['vertices'] = vertices(mesh, poses[0])
            targets(mesh, poses, 'body' if name.startswith('body') else
                    'hand' if name.startswith('arm') else 'foot')


def wings(data, template):
    # Actual Power-first wings stay compact. Two closed lobes on each retained
    # wing joint vary span/thickness but keep a fixed exact proximal hinge.
    profiles = {
        'young': (1.65, 1.25, .90), 'balanced': (1.80, 1.35, .95),
        'run': (2.20, 1.45, .95), 'power': (1.65, 1.20, 1.10),
        'fly': (2.25, 1.35, .70), 'swim': (1.75, 1.20, .85),
        'run_swim': (1.90, 1.30, .90), 'run_fly': (2.10, 1.35, .85),
        'power_swim': (1.70, 1.25, .95), 'power_fly': (2.05, 1.35, .95),
    }
    names = []
    for bone in data['bones']:
        if bone['name'] not in ('wing_left__cube_0', 'wing_right__cube_0'):
            continue
        sign = -1 if 'left' in bone['name'] else 1
        side = 'left' if sign < 0 else 'right'
        parts = []
        for index, (part, thickness) in enumerate((('upper', .26), ('lower', .19))):
            values = {}
            for key, (upper, lower, height) in profiles.items():
                span = upper if index == 0 else lower
                angle = -28 if index == 0 else 18
                rotation = [0, 0, sign * angle]
                offset = rotate([sign * span / 2, 0, 0], rotation)
                values[key] = pose([offset[0], .25 + offset[1], 0],
                                   [span, height * (1 - .16 * index), thickness], rotation)
            name = 'power_wing_' + side + '_' + part
            mesh = feature(template, name, lattice(values), 'wing_' + part, FIN_UV)
            distal = {0, 3, 4, 7} if sign > 0 else {1, 2, 5, 6}
            for face in mesh['faces']:
                face['uv'] = [(UNDER_UV if i in distal else FIN_UV)[:] for i in face['v']]
            parts.append(mesh)
            names.append(name)
        bone['meshes'] = parts
    return names


def tail(data, template):
    bone = next(b for b in data['bones'] if b['name'] == 'tail__cube_0')
    # A compact spade/diamond tail, with the orange/body root and independent
    # colored end. Swim moderately broadens it rather than adding a huge fin.
    profiles = {
        'young': (1.00, .95, 1.65, -17, .65, .70, 1.05, 5),
        'balanced': (1.05, 1.00, 1.85, -17, .70, .75, 1.15, 5),
        'run': (1.00, .95, 2.15, -23, .65, .70, 1.25, 10),
        'power': (1.15, 1.10, 1.70, -12, .75, .80, 1.05, 12),
        'fly': (1.05, .90, 1.95, -20, .65, .65, 1.25, 12),
        'swim': (1.35, 1.00, 2.25, -20, .90, .75, 1.25, 8),
        'run_swim': (1.20, .95, 2.15, -22, .80, .70, 1.25, 10),
        'run_fly': (1.05, .95, 2.05, -22, .70, .70, 1.20, 10),
        'power_swim': (1.25, 1.05, 2.05, -16, .85, .75, 1.20, 10),
        'power_fly': (1.10, 1.00, 1.90, -16, .75, .75, 1.15, 12),
    }
    roots, tips = {}, {}
    for key, (w, h, length, pitch, tw, th, tl, tp) in profiles.items():
        roots[key], tips[key] = chain([0, -.10, 1.65], length, w, h, [pitch, 0, 0],
                                      tl, tw, th, [tp, 0, 0], overlap=.50)
    bone['meshes'] = [feature(template, 'power_tail_root', lattice(roots), 'tail_root'),
                      feature(template, 'power_tail_tip', lattice(tips), 'tail_tip', TAIL_UV)]


def author():
    source = MODELS / 'chao_baby.json'
    data = json.loads(source.read_text())
    original = copy.deepcopy(data)
    bones = {b['name']: b for b in data['bones']}
    template = bones['skull__cube_0']['meshes'][0]
    bones['gradual_head_021']['meshes'] = head_features(template)
    limbs(data)
    wing_names = wings(data, template)
    tail(data, template)
    data['authored_gameplay_form'] = 'neutral_power_proto063'
    data['evolution_revision'] = '0.25.63'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh +9 fully signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'neutral/power',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['neutral_power_path_063'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(clamp(runPower,-1,1),x)*triangle(clamp(swimFly,-1,1),y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv': BODY_UV, 'feature_tip_uv': TIP_UV, 'feature_tail_uv': TAIL_UV,
        'feature_fin_uv': FIN_UV, 'feature_underside_uv': UNDER_UV,
        'reserved_pixels': {'feature_body': [127, 63], 'feature_tip': [126, 63],
                            'feature_fin': [125, 63], 'feature_underside': [124, 63]},
        'scope': 'Ordinary living Neutral adult with locked Power FIRST evolution. Full signed laterRun/Power andSwim/Fly axes retain balance, fourdirections and fourunique compatiblepairs, without changing firsttype.',
        'reference': 'Fresh independently operated Chao Island viewer with POWER first checkbox selected, Neutral alignment, allnine mature front/side/back and selected above controls, plus young/intermediate source views. Original official Power addon used for stepped cuboid construction vocabulary only.',
        'source_geometry_copied': False,
        'approximation': 'Original skull and pixel face exact. Eightclosedheadboxes interpret ONE upper axialcrown, ONE mid/lower axialrearpoint and TWO frontsidehorns. Flycrown bendsup/forward; Runextendsupper/lowerback-downhooks; Swimflattensupperbackspike/lowrearpoint and rotateshornsoutward; fullPowerthickensupperthreepoints and shortenslowerrear. These are authored broad two-section caps, not smooth sourcegeometry/zigzagtextures. Bilinear mixed interiors and age pacing are approximations.',
        'head_caps_policy': 'BODY127 roots plus TIP126 across every face of four visible distal tipboxes. No forehead strokes, no originalheadTOP stripes, no centralstripe tile. Run062 stripes and allprevious paint/models stay unchanged.',
        'wing_policy': 'Two small closedcuboidlobes per retainedwingjoint. Fixeddirections-28/+18deg and varying relativespans preserve exactproximalface local[0,.25,0] underblending. Positive thicknesses.26/.19; allfourlongfaces carry root125/distal124 broadpigments.',
        'body_contact_policy': 'Top-only torsoextension+.45 supports scoped.35pixel walkingheadlift. Original59joint/resttransforms and shoulderanchors exact; compact fists and feet grow moderately.',
        'tail_contact_policy': 'Two compactspadesections overlap.50; rootlocal[0,-.10,1.65] remains within retained originaltailbase. Independent tailend pigment uses original opaque[25.25,16.25]texel.',
        'marker_policy': 'Original unspiked orb, expression/marker bones and meshes exact; no added markergeometry.',
        'provisional_rates': '960currentlevelbarprogress per opposingaxis; independentadultvisualmaturity. Viewercontrols do not establish originalgame levels/timing.'}
    actual = {b['name']: b for b in data['bones']}
    assert len(actual) == len(original['bones']) == 59
    for bone in original['bones']:
        name = bone['name']
        for field in ('pivot', 'rotation', 'parent'):
            assert actual[name].get(field) == bone.get(field), (name, field)
        if name.startswith(('skull', 'expression_', 'emotion_')):
            assert actual[name] == bone, name
    OUT.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    refs = ['art/imported_chao/models/chao_power.geo.json', 'art/imported_chao/textures/power.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_power.json']
    head_names = [m['name'] for m in bones['gradual_head_021']['meshes']]
    manifest = {'version': '0.25.63', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_orb_and_joint_pivots_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': ['balanced', 'run', 'power', 'fly', 'swim', 'run_fly', 'run_swim', 'power_fly', 'power_swim'],
        'closed_box_count': 19, 'head_feature_count': 8, 'wing_ray_count': 4, 'tail_part_count': 2,
        'head_feature_names': head_names, 'wing_mesh_names': wing_names,
        'tail_mesh_names': ['power_tail_root', 'power_tail_tip'],
        'head_feature_roles': {'central': head_names[:2], 'rear': head_names[2:4], 'horns': head_names[4:]},
        'head_caps_policy': 'Allfourdistaltipboxes use CAP126 throughout; lowerrootsBODY127. No forehead/top/centralstripes. Fixedoriginalskull preserved.',
        'effective_viewer_pair_controls': 'Observed full signed controls remain valid with firstPowerselected; mixed silhouettes moderate rather than stack singles. Bilinear interiors and linear age development remain authored approximations.'}
    (ASSETS / 'neutral_power063.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('NeutralPower063 generated: exact original7x6x6head/face/orb/59joints; '
          '8Powerheadfeatures (central2,singlelowerrear2,sidehorn4),5compactbody/limbs,'
          '4smallclosedwinglobes,2tailparts;19closedboxes,10targets/9signedmatureforms.')


if __name__ == '__main__':
    author()
