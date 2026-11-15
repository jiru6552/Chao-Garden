#!/usr/bin/env python3
"""Author the blocky Hero/Fly-first family on the unchanged original rig.

Fresh actual viewer observations guide broad anatomy and progression only.
Six authored head cuboids form white roots, two-region caps and extra forks; no
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
OUT = MODELS / 'chao_hero_fly_proto065.json'
TARGETS = ['hero_fly_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
FIELD = 'hero_fly_box_065'
REGION = 'hero_fly_region_065'
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
    # Directly inspected live first-Fly Hero canvases: a paired low arch when
    # fresh/balanced, tall outward V for Run, branched V for Power, broad long
    # downward arches plus an upper filament for Fly, and low rear-streaming
    # zigzags for opposite Swim. These six closed cuboids deliberately retain
    # the original square skull instead of reproducing the source wedge head.
    # root length,width,depth,pitch,roll; cap length,width,depth,pitch,roll;
    # auxiliary length,width,depth,pitch,roll,root fraction.
    profiles = {
        'young': (2.60, 1.10, 1.35, -10, 60, 2.35, .75, 1.30, -10, 142, .12, .08, .08, -9, 64, .60),
        'balanced': (3.00, 1.10, 1.30, -15, 62, 3.50, .80, 1.45, -25, 158, .12, .08, .08, -14, 66, .60),
        'run': (3.80, 1.10, 1.35, 40, 45, 3.80, .85, 1.50, -40, 55, .12, .08, .08, 26, 49, .60),
        'power': (3.30, 1.35, 1.65, -5, 45, 2.80, 1.00, 1.65, 10, 20, 2.00, .65, 1.20, -6, 105, .78),
        'fly': (3.60, 1.10, 1.45, -15, 65, 5.20, .80, 1.50, -35, 155, 3.35, .45, .85, 8, 60, .45),
        'swim': (3.40, 1.05, 1.30, -35, 78, 3.10, .70, 1.35, -40, 98, .12, .08, .08, -29, 82, .60),
        'run_swim': (3.20, 1.10, 1.40, -5, 50, 3.20, .80, 1.45, -15, 75, .12, .08, .08, -4, 54, .60),
        'run_fly': (2.80, 1.10, 1.35, -8, 65, 3.70, .75, 1.45, -12, 135, .12, .08, .08, -7, 69, .60),
        'power_swim': (3.00, 1.25, 1.50, -15, 55, 3.00, .90, 1.60, -20, 70, 1.25, .50, 1.00, -14, 110, .78),
        'power_fly': (3.20, 1.25, 1.50, -20, 55, 4.20, .90, 1.55, -30, 145, 3.15, .55, 1.00, 8, 35, .50),
    }
    result = []
    for sign, side in ((-1, 'left'), (1, 'right')):
        roots, caps, spurs = {}, {}, {}
        for key, values in profiles.items():
            rl, rw, rd, rx, rz, cl, cw, cd, cx, cz, sl, sw, sd, sx, sz, fraction = values
            anchor = [sign * .65, -5.72, .75]
            root_rotation = [rx, 0, sign * rz]
            cap_rotation = [cx, 0, sign * cz]
            root_delta = rotate([0, -rl / 2, 0], root_rotation)
            roots[key] = pose([a + b for a, b in zip(anchor, root_delta)],
                              [rw, rl, rd], root_rotation)
            neck = [a + b for a, b in zip(anchor, rotate([0, -rl, 0], root_rotation))]
            cap_delta = rotate([0, -(cl / 2 - .80), 0], cap_rotation)
            caps[key] = pose([a + b for a, b in zip(neck, cap_delta)],
                             [cw, cl, cd], cap_rotation)
            # Unused filaments are small positive solids buried inside the
            # white root. Their anchored volumes grow smoothly into the two
            # source branches where Power/Fly call for them; no visibility swap.
            spur_anchor = [a + b for a, b in zip(anchor, rotate([0, -rl * fraction, 0], root_rotation))]
            spur_rotation = [sx, 0, sign * sz]
            spur_delta = rotate([0, -(sl / 2 - .08), 0], spur_rotation)
            spurs[key] = pose([a + b for a, b in zip(spur_anchor, spur_delta)],
                              [sw, sl, sd], spur_rotation)
        root = feature(template, 'hero_fly_crown_' + side + '_root', lattice(roots), 'crown_root')
        cap = feature(template, 'hero_fly_crown_' + side + '_tip', lattice(caps), 'crown_tip')
        spur = feature(template, 'hero_fly_crown_' + side + '_spur', lattice(spurs), 'crown_spur')
        for mesh in (cap, spur):
            for face in mesh['faces']:
                face['uv'] = [(CAP_DISTAL_UV if i in (2, 3, 6, 7) else CAP_PROX_UV)[:]
                              for i in face['v']]
        result += [root, cap, spur]
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
    # The actual Fly-first viewer has a substantial three-point fan from its
    # first evolution, growing very wide at pure Fly. Three opaque closed rays
    # per original wing joint simplify that outline without thin alpha planes.
    profiles = {
        'young': (3.80, 2.90, 2.10, 1.10),
        'balanced': (4.70, 3.40, 2.50, 1.10),
        'run': (4.30, 3.00, 2.25, 1.10),
        'power': (3.65, 2.70, 1.95, 1.20),
        'fly': (6.40, 4.80, 3.40, 1.25),
        'swim': (5.10, 3.60, 2.50, 1.05),
        'run_swim': (4.70, 3.30, 2.45, 1.05),
        'run_fly': (5.20, 3.80, 2.70, 1.15),
        'power_swim': (4.70, 3.40, 2.50, 1.15),
        'power_fly': (5.10, 3.70, 2.60, 1.20),
    }
    names = []
    for bone in data['bones']:
        if bone['name'] not in ('wing_left__cube_0', 'wing_right__cube_0'):
            continue
        sign = -1 if 'left' in bone['name'] else 1
        side = 'left' if sign < 0 else 'right'
        parts = []
        for index, (part, angle, thickness) in enumerate(
                (('upper', -12, .30), ('middle', 18, .25), ('lower', 48, .20))):
            values = {}
            for key, (upper, middle, lower, height) in profiles.items():
                span = (upper, middle, lower)[index]
                rotation = [0, 0, sign * angle]
                offset = rotate([sign * span / 2, 0, 0], rotation)
                values[key] = pose([offset[0], .25 + offset[1], 0],
                                   [span, height * (1 - .10 * index), thickness], rotation)
            name = 'hero_fly_wing_' + side + '_' + part
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
    heights = [.65, .55, .60, .50, .15, .65, .10, .55, .45, .20]
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
            mesh = feature(template, 'hero_fly_halo_bar_' + str(index), poses, 'halo', HALO_UV)
            data['bones'].append({'name': 'hero_fly_halo_065_' + str(index), 'parent': 'emotion_dot',
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
    data['authored_gameplay_form'] = 'hero_fly_proto065'
    data['evolution_revision'] = '0.25.65'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh + nine fully signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'hero/fly',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['hero_fly_path_065'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(swimFly,y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv': BODY_UV, 'feature_cap_proximal_uv': CAP_PROX_UV,
        'feature_cap_distal_uv': CAP_DISTAL_UV, 'wing_root_uv': WING_ROOT_UV,
        'wing_distal_uv': WING_DISTAL_UV, 'halo_uv': HALO_UV,
        'reserved_pixels': {'feature_body': [127, 63], 'cap_proximal': [125, 63],
                            'cap_distal': [126, 63], 'wing_root': [124, 63], 'wing_distal': [123, 63]},
        'scope': 'Ordinary living Hero adult with locked Fly FIRST evolution. Full signed later axes retain nine unique corners, including opposite Swim training, without changing first type or biological age.',
        'reference': 'Fresh independently operated Chao Island viewer with Fly first checkbox selected and Hero alignment. Fresh front/side and all nine mature forms front/side personally inspected before authoring; balanced mature back also inspected. Imported Hero Fly cuboids supply block construction vocabulary only.',
        'source_geometry_copied': False,
        'approximation': 'Six closed head cuboids interpret paired white-root and two-color caps with auxiliary Power/Fly forks around the original square skull. Source smooth curves, wave zigzags, four thin Fly filaments, webbed feet and feathered fans are simplified. Balanced curls down/out, Run rises in a dramatic outward V and sweeps forward, Power adds upright prongs and small outer spurs, Fly grows long rearward/downward outer caps with upper filaments, opposite Swim flattens rearward, and mixed corners use separately authored silhouettes. Bilinear combinations and maturity pacing are authored approximations.',
        'head_caps_policy': 'White/inherited root127; main caps and auxiliary spurs span proximal125/distal126 along actual local lengths including terminal faces. Unused auxiliary spurs remain positive closed solids buried inside roots and emerge smoothly. No head shell, forehead stripes or band geometry.',
        'wing_policy': 'Three opaque closed cuboid rays per unchanged wing joint; fixed directions -12/+18/+48 degrees retain exact proximal hinge [0,.25,0]. Four longitudinal faces carry root124/distal123 pigments, and differing thicknesses avoid parallel coplanar overlap.',
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
    manifest = {'version': '0.25.65', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_and_joint_pivots_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': ['balanced', 'run', 'power', 'fly', 'swim', 'run_fly', 'run_swim', 'power_fly', 'power_swim'],
        'closed_box_count': 21, 'head_feature_count': 6, 'wing_ray_count': 6, 'halo_bar_count': 4,
        'head_feature_names': head_names, 'wing_mesh_names': wing_names,
        'tail_geometry_unchanged': True,
        'effective_viewer_controls': 'Actual first-Fly selection permits signed Swim counter-training beyond balance; opposing axes remain independent and reverse-order pairs share one corner.'}
    (ASSETS / 'hero_fly065.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('HeroFly065 generated: exact original head/face and 59 original joints; six paired head sections, '
          'five compact body/limb boxes, six substantial wing rays, four halo bars; 21 closed boxes and ten targets.')


if __name__ == '__main__':
    author()
