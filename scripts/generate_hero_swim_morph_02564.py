#!/usr/bin/env python3
"""Author the blocky Hero/Swim-first family on the unchanged original rig.

Fresh actual viewer observations guide broad anatomy and progression only.
Four authored head cuboids form white roots and two-region distal flaps; no
source geometry, sculpted curves or replacement base head are introduced.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_hero_swim_proto064.json'
TARGETS = ['hero_swim_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
FIELD = 'hero_swim_box_064'
REGION = 'hero_swim_region_064'
BODY_UV = [63.75, 31.75]
CAP_PROX_UV = [62.75, 31.75]
CAP_DISTAL_UV = [63.25, 31.75]
WING_ROOT_UV = [62.25, 31.75]
WING_DISTAL_UV = [61.75, 31.75]
HALO_UV = [58.5, 1.5]


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


def head_features(template):
    # Actual Hero Swim has paired crown flaps even immediately after evolution.
    # A white/inherited lower section grows out from the unchanged crown; the
    # colored upper section arches out/down, drops as a ribbon, or rises as a
    # broad paddle. Two closed boxes per side retain a clean Minecraft shape.
    # root length,width,depth,pitch,roll; cap length,width,depth,pitch,roll.
    profiles = {
        'young': (2.40, 1.35, 1.55, -8, 63, 3.20, 1.20, 1.95, -12, 155),
        'balanced': (2.00, 1.25, 1.45, -5, 65, 2.35, .95, 1.85, -8, 112),
        'run': (3.00, 1.20, 1.40, 5, 65, 6.10, .90, 1.70, 22, 170),
        'power': (2.60, 1.60, 2.10, -15, 48, 2.85, 2.05, 2.65, -25, 82),
        'fly': (2.25, 1.45, 1.70, -12, 70, 3.25, 1.20, 2.05, -24, 30),
        'swim': (3.10, 1.15, 1.40, -8, 65, 7.20, 1.05, 1.80, -17, 170),
        'run_swim': (3.05, 1.15, 1.45, -2, 65, 6.80, .90, 1.80, 0, 174),
        'run_fly': (2.00, 1.25, 1.50, -8, 62, 2.70, 1.00, 1.90, -7, 112),
        'power_swim': (2.20, 1.50, 1.90, -12, 57, 3.60, 1.45, 2.30, -22, 133),
        'power_fly': (2.45, 1.70, 2.00, -15, 55, 3.10, 1.90, 2.50, -22, 75),
    }
    result = []
    for sign, side in ((-1, 'left'), (1, 'right')):
        roots, caps = {}, {}
        for key, (rl, rw, rd, rx, rz, cl, cw, cd, cx, cz) in profiles.items():
            anchor = [sign * .65, -5.72, .75]
            root_rotation = [rx, 0, sign * rz]
            cap_rotation = [cx, 0, sign * cz]
            root_delta = rotate([0, -rl / 2, 0], root_rotation)
            roots[key] = pose([a + b for a, b in zip(anchor, root_delta)],
                              [rw, rl, rd], root_rotation)
            neck = [a + b for a, b in zip(anchor, rotate([0, -rl, 0], root_rotation))]
            cap_delta = rotate([0, -(cl / 2 - .65), 0], cap_rotation)
            caps[key] = pose([a + b for a, b in zip(neck, cap_delta)],
                             [cw, cl, cd], cap_rotation)
        root = feature(template, 'hero_swim_crown_' + side + '_root', lattice(roots), 'crown_root')
        cap = feature(template, 'hero_swim_crown_' + side + '_tip', lattice(caps), 'crown_tip')
        # Two actual colored cap regions live on its existing closed faces.
        # Local negative Y is the terminal direction regardless of world lean.
        for face in cap['faces']:
            face['uv'] = [(CAP_DISTAL_UV if i in (2, 3, 6, 7) else CAP_PROX_UV)[:]
                          for i in face['v']]
        result += [root, cap]
    return result


def limbs(data):
    scales = {
        'young': ((1.025, 1.00, 1.025), (1.035, 1.035, 1.035), (1.035, 1.00, 1.045)),
        'balanced': ((1.055, 1.025, 1.055), (1.085, 1.08, 1.08), (1.095, 1.015, 1.105)),
        'run': ((1.035, 1.04, 1.035), (1.055, 1.06, 1.055), (1.075, 1.01, 1.14)),
        'power': ((1.10, 1.045, 1.09), (1.17, 1.16, 1.14), (1.12, 1.045, 1.115)),
        'fly': ((1.035, 1.045, 1.035), (1.07, 1.065, 1.07), (1.085, 1.01, 1.095)),
        'swim': ((1.065, 1.015, 1.075), (1.085, 1.075, 1.085), (1.155, 1.02, 1.145)),
        'run_swim': ((1.045, 1.025, 1.055), (1.07, 1.065, 1.065), (1.125, 1.015, 1.16)),
        'run_fly': ((1.035, 1.04, 1.035), (1.06, 1.06, 1.06), (1.085, 1.01, 1.115)),
        'power_swim': ((1.085, 1.035, 1.085), (1.125, 1.12, 1.11), (1.14, 1.035, 1.13)),
        'power_fly': ((1.065, 1.045, 1.06), (1.12, 1.105, 1.11), (1.105, 1.03, 1.105)),
    }
    for bone in data['bones']:
        name = bone['name']
        if name not in ('body__cube_0', 'arm_left__cube_1', 'arm_right__cube_1',
                        'foot_left__cube_0', 'foot_right__cube_0'):
            continue
        for mesh in bone.get('meshes', []):
            base = original_box(mesh)
            values = {}
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
                values[key] = p
            poses = lattice(values)
            mesh['vertices'] = vertices(mesh, poses[0])
            targets(mesh, poses, 'body' if name.startswith('body') else 'hand' if name.startswith('arm') else 'foot')


def wings(data, template):
    # Actual Hero Swim wings remain small lobes. Fly changes them into small
    # raised gold winglets, not the wide span of a Fly-first adult. Constant
    # lobe directions retain an exact proximal hinge throughout interpolation.
    profiles = {
        'young': (1.70, 1.15, .85), 'balanced': (1.85, 1.20, .85),
        'run': (1.90, 1.15, .75), 'power': (1.75, 1.20, .95),
        'fly': (2.25, .95, .95), 'swim': (1.90, 1.30, .52),
        'run_swim': (1.90, 1.20, .65), 'run_fly': (2.05, 1.05, .85),
        'power_swim': (1.80, 1.20, .75), 'power_fly': (2.10, 1.00, 1.00),
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
                rotation = [0, 0, sign * (-30 if index == 0 else 22)]
                offset = rotate([sign * span / 2, 0, 0], rotation)
                values[key] = pose([offset[0], .25 + offset[1], 0],
                                   [span, height * (1 - .14 * index), thickness], rotation)
            name = 'hero_swim_wing_' + side + '_' + part
            mesh = feature(template, name, lattice(values), 'wing_' + part, WING_ROOT_UV)
            distal = {0, 3, 4, 7} if sign > 0 else {1, 2, 5, 6}
            for face in mesh['faces']:
                face['uv'] = [(WING_DISTAL_UV if i in distal else WING_ROOT_UV)[:] for i in face['v']]
            parts.append(mesh)
            names.append(name)
        bone['meshes'] = parts
    return names


def halo(data, template):
    bones = {b['name']: b for b in data['bones']}
    original = bones['emotion_dot__cube_0']['meshes'][0]
    heights = [.65, .60, .60, .55, .65, .70, .55, .65, .50, .55]
    specs = [([0, 0, -2.1], [4.6, .4, .4]), ([0, 0, 2.1], [4.6, .4, .4]),
             ([-2.1, 0, 0], [.4, .4, 4.6]), ([2.1, 0, 0], [.4, .4, 4.6])]
    for index, (center, size) in enumerate(specs):
        poses = [pose([center[0], h, center[2]], size) for h in heights]
        if index == 0:
            original['vertices'] = vertices(original, poses[0])
            for key in ('morph_vertices', 'hero_run_vertices_052', 'hero_run_legacy_vertices_052'):
                original.pop(key, None)
            targets(original, poses, 'halo')
        else:
            mesh = feature(template, 'hero_swim_halo_bar_' + str(index), poses, 'halo', HALO_UV)
            data['bones'].append({'name': 'hero_swim_halo_064_' + str(index), 'parent': 'emotion_dot',
                                  'pivot': [0, 0, 0], 'rotation': [0, 0, 0], 'meshes': [mesh]})


def author():
    source = MODELS / 'chao_baby.json'
    data = json.loads(source.read_text())
    original = copy.deepcopy(data)
    bones = {b['name']: b for b in data['bones']}
    template = bones['skull__cube_0']['meshes'][0]
    bones['gradual_head_021']['meshes'] = head_features(template)
    limbs(data)
    wing_names = wings(data, template)
    halo(data, template)
    data['authored_gameplay_form'] = 'hero_swim_proto064'
    data['evolution_revision'] = '0.25.64'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh + nine fully signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'hero/swim',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['hero_swim_path_064'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(swimFly,y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv': BODY_UV, 'feature_cap_proximal_uv': CAP_PROX_UV,
        'feature_cap_distal_uv': CAP_DISTAL_UV, 'wing_root_uv': WING_ROOT_UV,
        'wing_distal_uv': WING_DISTAL_UV, 'halo_uv': HALO_UV,
        'reserved_pixels': {'feature_body': [127, 63], 'cap_proximal': [125, 63],
                            'cap_distal': [126, 63], 'wing_root': [124, 63], 'wing_distal': [123, 63]},
        'scope': 'Ordinary living Hero adult with locked Swim FIRST evolution. Full signed later axes retain nine unique corners without changing first type or biological age.',
        'reference': 'Fresh independently operated Chao Island viewer with Swim first checkbox selected and Hero alignment. Fresh and all nine mature forms personally inspected front/side; balanced/Swim/Fly backs also inspected. Imported Hero and Swim cuboids supply block construction vocabulary only; no official imported Hero Swim model exists.',
        'source_geometry_copied': False,
        'approximation': 'Four closed cuboids interpret paired white-root and two-color distal flaps around the original square skull. Source smooth arches, webbed feet and small rounded wing lobes are simplified. Run turns down/forward, Swim down/back, Power forms thick upward/back paddles, Fly raises the outer point, and mixed corners moderate these. Bilinear combinations and maturity pacing are authored approximations.',
        'head_caps_policy': 'White/inherited root127; one upper cap per side spans proximal125 and distal126 along its actual local length, including terminal faces. No head shell, forehead stripes or band geometry.',
        'wing_policy': 'Two small closed lobes per unchanged wing joint; fixed directions -30/+22 degrees retain exact proximal hinge [0,.25,0]. Four longitudinal faces carry root124/distal123 pigments.',
        'body_contact_policy': 'Top-only torso extension +.45 supports scoped .35 pixel walking head lift. Original joints and shoulder anchors remain exact; compact hands and feet grow moderately.',
        'tail_policy': 'Original small tail geometry, native UVs and attachment retained unchanged.',
        'marker_policy': 'Square four-bar Hero halo uses original emotion_dots/emotion_dot anchors and tilt; first bar retains original atlas artwork, three mesh-only children add the remaining bars.',
        'provisional_rates': '960 current level-bar progress per opposing axis; independent adult maturity. Viewer settings do not establish original game levels or timing.'}
    actual = {b['name']: b for b in data['bones']}
    assert len(actual) == len(original['bones']) + 3 == 62
    for bone in original['bones']:
        name = bone['name']
        for field in ('pivot', 'rotation', 'parent'):
            assert actual[name].get(field) == bone.get(field), (name, field)
        if name.startswith(('skull', 'expression_')) or name.startswith('emotion_') and name != 'emotion_dot__cube_0':
            assert actual[name] == bone, name
    OUT.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    refs = ['art/imported_chao/models/chao_hero.geo.json', 'art/imported_chao/models/chao_hero_fly.geo.json',
            'art/imported_chao/models/chao_hero_power.geo.json', 'art/imported_chao/models/chao_swim.geo.json']
    head_names = [m['name'] for m in bones['gradual_head_021']['meshes']]
    manifest = {'version': '0.25.64', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_and_joint_pivots_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': ['balanced', 'run', 'power', 'fly', 'swim', 'run_fly', 'run_swim', 'power_fly', 'power_swim'],
        'closed_box_count': 17, 'head_feature_count': 4, 'wing_ray_count': 4, 'halo_bar_count': 4,
        'head_feature_names': head_names, 'wing_mesh_names': wing_names,
        'tail_geometry_unchanged': True,
        'effective_viewer_controls': 'Actual first-Swim selection permits signed Fly counter-training beyond balance; opposing axes remain independent and reverse-order pairs share one corner.'}
    (ASSETS / 'hero_swim064.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('HeroSwim064 generated: exact original head/face and 59 original joints; four paired head sections, '
          'five compact body/limb boxes, four small wing lobes, four halo bars; 17 closed boxes and ten targets.')


if __name__ == '__main__':
    author()
