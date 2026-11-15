#!/usr/bin/env python3
"""Author the blocky Hero/Power-first family on the unchanged original rig.

Fresh actual viewer observations guide broad anatomy and progression only.
Six authored head cuboids form three white roots and patch-painted caps; no
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
OUT = MODELS / 'chao_hero_power_proto067.json'
TARGETS = ['hero_power_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
FIELD = 'hero_power_box_067'
REGION = 'hero_power_region_067'
BODY_UV = [63.75, 31.75]
CAP_PROX_UV = [62.75, 31.75]
CAP_DISTAL_UV = [63.25, 31.75]
WING_ROOT_UV = [62.25, 31.75]
WING_DISTAL_UV = [61.75, 31.75]
PATCH_MIN_UV = [58.1, 30.1]
PATCH_MAX_UV = [59.9, 31.9]
HALO_UV = [58.5, 1.5]
HALO_HEIGHTS = [.65, .55, .65, .45, -.25, .65, .05, .45, .50, .25]


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


def axial(anchor, length, width, depth, rotation, overlap=0):
    """Positive closed box following its negative localY principal axis."""
    offset = rotate([0, -(length / 2 - overlap), 0], rotation)
    return pose([a + b for a, b in zip(anchor, offset)], [width, length, depth], rotation)


def end(anchor, length, rotation):
    return [a + b for a, b in zip(anchor, rotate([0, -length, 0], rotation))]


def head_features(template):
    # Actual independently operated first-Power Hero viewer: three short blunt
    # fresh lobes, orange upright balanced knobs, a taller central Power bulb,
    # long opposite-Run rear points, and paired Fly/Swim lobes around a LONG
    # narrow central rear blade (confirmed above, not guessed from its tiny
    # front projection). Original square skull/face stay exact.
    # Root length,width,depth,pitch,roll; cap length,width,depth,pitch,roll.
    centers = {
        'young': (.75, .62, .65, -8, 0, 1.15, .98, 1.00, -12, 0),
        'balanced': (1.05, .68, .75, -6, 0, 1.55, 1.12, 1.15, -8, 0),
        'run': (3.20, 1.25, 1.35, -15, 0, 3.80, 1.10, 1.20, -100, 0),
        'power': (2.50, .75, .85, -6, 0, 2.20, 2.15, 2.00, -8, 0),
        'fly': (4.75, .62, .70, -77, 0, 1.20, .50, .60, -98, 0),
        'swim': (4.30, .65, .70, -72, 0, 1.65, .65, .75, -98, 0),
        'run_fly': (2.10, .88, .95, -14, 0, 2.20, .85, .90, -101, 0),
        'run_swim': (2.50, 1.00, 1.05, -15, 0, 3.20, 1.10, 1.15, -105, 0),
        'power_fly': (2.15, .90, .90, -8, 0, 1.60, 1.00, 1.15, -25, 0),
        'power_swim': (1.65, .75, .85, -10, 0, 1.65, 1.55, 1.55, -15, 0),
    }
    sides = {
        'young': (.85, .58, .68, -7, 35, 1.15, 1.05, 1.05, -10, 40),
        'balanced': (1.05, .65, .80, -8, 36, 1.55, 1.15, 1.20, -12, 40),
        'run': (1.85, .95, 1.00, -25, 50, 3.65, 1.00, 1.20, -80, 80),
        'power': (1.45, .70, .85, -8, 32, 1.90, 1.65, 1.75, -18, 38),
        'fly': (2.05, .85, 1.05, -10, 40, 2.20, 1.75, 1.65, -55, 90),
        'swim': (2.30, .80, 1.00, -18, 35, 2.60, 1.45, 1.45, -65, 90),
        'run_fly': (1.85, .75, .85, -18, 45, 2.60, .85, .90, -78, 82),
        'run_swim': (2.20, .90, 1.00, -20, 40, 3.20, 1.45, 1.20, -72, 86),
        'power_fly': (1.75, .80, .95, -5, 42, 2.45, .90, 1.10, -35, 58),
        'power_swim': (1.40, .80, .95, -10, 42, 1.90, 1.55, 1.55, -30, 55),
    }
    result = []
    for side, sign, profiles in [('center', 0, centers), ('left', -1, sides), ('right', 1, sides)]:
        roots, caps = {}, {}
        for key, (rl, rw, rd, rx, rz, cl, cw, cd, cx, cz) in profiles.items():
            anchor = [sign * .75, -5.72, 1.20 if sign == 0 else .45]
            root_rotation = [rx, 0, sign * rz]
            # A real small twist prevents same-facing cap/root side planes from
            # becoming coincident when their widths cross during a blend.
            cap_rotation = [cx, 2.5 if sign == 0 else sign * 2.5, sign * cz]
            roots[key] = axial(anchor, rl, rw, rd, root_rotation)
            neck = end(anchor, rl, root_rotation)
            overlap = .25 if key == 'young' else .55
            caps[key] = axial(neck, cl, cw, cd, cap_rotation, overlap)
        root = feature(template, 'hero_power_crown_' + side + '_root', lattice(roots), 'crown_root')
        cap = feature(template, 'hero_power_crown_' + side + '_tip', lattice(caps), 'crown_tip')
        for face in cap['faces']:
            indices = face['v']; distal = [i in (2, 3, 6, 7) for i in indices]
            if all(distal):
                face['uv'] = [CAP_DISTAL_UV[:] for _ in indices]
            elif not any(distal):
                face['uv'] = [CAP_PROX_UV[:] for _ in indices]
            else:
                # A stable2D spot tile wraps four existing closed faces. V runs
                # proximal-to-distal; U spans the face's localX or localZ axis.
                cross = 0 if len({template['vertices'][i][0] for i in indices}) > 1 else 2
                low = min(template['vertices'][i][cross] for i in indices)
                face['uv'] = [[PATCH_MIN_UV[0] if template['vertices'][i][cross] == low else PATCH_MAX_UV[0],
                               PATCH_MAX_UV[1] if d else PATCH_MIN_UV[1]]
                              for i, d in zip(indices, distal)]
        result += [root, cap]
    return result


def limbs(data):
    scales = {
        'young': ((1.04, 1.015, 1.045), (1.16, 1.10, 1.15), (1.12, 1.025, 1.10)),
        'balanced': ((1.075, 1.035, 1.075), (1.22, 1.14, 1.20), (1.18, 1.04, 1.14)),
        'run': ((1.055, 1.04, 1.055), (1.18, 1.115, 1.17), (1.23, 1.035, 1.18)),
        'power': ((1.12, 1.05, 1.11), (1.32, 1.18, 1.28), (1.23, 1.055, 1.18)),
        'fly': ((1.055, 1.045, 1.055), (1.20, 1.12, 1.18), (1.15, 1.025, 1.14)),
        'swim': ((1.075, 1.025, 1.085), (1.23, 1.14, 1.21), (1.24, 1.04, 1.19)),
        'run_swim': ((1.065, 1.035, 1.075), (1.21, 1.13, 1.19), (1.23, 1.035, 1.19)),
        'run_fly': ((1.055, 1.04, 1.055), (1.19, 1.115, 1.17), (1.20, 1.03, 1.16)),
        'power_swim': ((1.10, 1.045, 1.10), (1.29, 1.17, 1.25), (1.23, 1.045, 1.18)),
        'power_fly': ((1.085, 1.05, 1.08), (1.26, 1.155, 1.23), (1.19, 1.04, 1.17)),
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
                if name.startswith('arm'):
                    # The fist grows below its original wrist/top plane.
                    # Symmetric height growth pushed its upper corners into
                    # the fixed skull during extreme downward walking gaze.
                    p['center'][1] = min(v[1] for v in mesh['vertices']) + p['size'][1] / 2
                if name.startswith('foot'):
                    p['center'][1] = max(v[1] for v in mesh['vertices']) - p['size'][1] / 2
                    p['center'][2] = max(v[2] for v in mesh['vertices']) - p['size'][2] / 2
                values[key] = p
            poses = lattice(values)
            mesh['vertices'] = vertices(mesh, poses[0])
            targets(mesh, poses, 'body' if name.startswith('body') else 'hand' if name.startswith('arm') else 'foot')


def wings(data, template):
    # Actual Power-first wings begin as compact green leaves. Fly broadens
    # them into large blue/teal fans; Run keeps small lateral pads. Three
    # opaque rays retain the original hinge and avoid zero-area source planes.
    profiles = {
        'young': (2.10, 1.40, .85, 1.00),
        'balanced': (2.25, 1.50, .90, 1.10),
        'run': (2.20, 1.45, .85, .95),
        'power': (2.55, 1.80, 1.05, 1.15),
        'fly': (4.15, 3.05, 1.95, 1.35),
        'swim': (2.45, 1.65, 1.00, 1.00),
        'run_swim': (2.25, 1.50, .85, .95),
        'run_fly': (2.50, 1.70, 1.05, 1.05),
        'power_swim': (2.55, 1.80, 1.05, 1.15),
        'power_fly': (3.35, 2.45, 1.50, 1.25),
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
            name = 'hero_power_wing_' + side + '_' + part
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
    heights = HALO_HEIGHTS
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
            mesh = feature(template, 'hero_power_halo_bar_' + str(index), poses, 'halo', HALO_UV)
            data['bones'].append({'name': 'hero_power_halo_067_' + str(index), 'parent': 'emotion_dot',
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
    data['authored_gameplay_form'] = 'hero_power_proto067'
    data['evolution_revision'] = '0.25.67'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh + nine fully signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'hero/power',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['hero_power_path_067'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(swimFly,y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv': BODY_UV, 'feature_cap_proximal_uv': CAP_PROX_UV,
        'feature_cap_distal_uv': CAP_DISTAL_UV, 'wing_root_uv': WING_ROOT_UV,
        'wing_distal_uv': WING_DISTAL_UV, 'halo_uv': HALO_UV,
        'feature_patch_min_uv': PATCH_MIN_UV, 'feature_patch_max_uv': PATCH_MAX_UV,
        'reserved_pixels': {'feature_body': [127, 63], 'cap_proximal': [125, 63],
                            'cap_distal': [126, 63], 'wing_root': [124, 63], 'wing_distal': [123, 63],
                            **{'patch_' + str(x) + '_' + str(y): [116 + x, 60 + y]
                               for y in range(4) for x in range(4)}},
        'scope': 'Ordinary living Hero adult with locked Power FIRST evolution. Full signed later axes include opposite Run training without changing first type or biological age.',
        'reference': 'Fresh independently operated Chao Island viewer with actual red Power checkbox selected, Hero alignment, age and both later axes. Fresh front/side and all nine mature front/side/back plus Run/Power/Fly/Swim above originals personally inspected before authoring. Imported official HeroPower cuboids supply construction context only.',
        'source_geometry_copied': False,
        'approximation': 'Three two-section closed cuboid lobes interpret compact fresh/balanced knobs, a large central Power bulb, long rear-swept Run points, broad Fly/Swim outer fins and long thin central rear blades. Mixed states have separately authored dimensions. Fixed original skull does not reproduce the source rear wedge. Smooth curves, taper, round bulbs, source wing/tail forks and repeated tiny coat spots are simplified. Linear maturity and bilinear current-axis blends approximate nonlinear source timing.',
        'head_caps_policy': 'White/inherited roots use body127. Four existing cap side faces wrap a two-dimensional4x4 patch tile116..119/60..63, V proximal-to-distal along negative localY, U across localX orZ. PositiveY terminal125; negativeY terminal126. No stripe meshes, forehead paint, hidden auxiliary forks or replacement head shell.',
        'head_part_names': [m['name'] for m in bones['gradual_head_021']['meshes']],
        'halo_height_targets': HALO_HEIGHTS, 'halo_local_z_offset': 0.0,
        'wing_policy': 'Three opaque closed cuboid rays per unchanged wing joint; fixed directions -12/+18/+48 degrees retain exact proximal hinge [0,.25,0]. Four longitudinal faces carry root124/distal123 pigments, and differing thicknesses avoid parallel coplanar overlap.',
        'body_contact_policy': 'Top-only torso extension +.45 supports scoped walking head lift. Original joints and shoulder anchors remain exact; first-Power fists/shoes begin modestly bulky and develop compact broader dimensions.',
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
    assert data['head_decorations'] == original['head_decorations']
    for name in ('tail', 'tail__cube_0', 'tail__cube_1'):
        assert actual[name] == next(b for b in original['bones'] if b['name'] == name)
    OUT.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    refs = ['art/imported_chao/models/chao_hero_power.geo.json',
            'art/imported_chao/textures/hero_power.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_hero_power.json']
    head_names = [m['name'] for m in bones['gradual_head_021']['meshes']]
    manifest = {'version': '0.25.67', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'original_joint_count': 59, 'bone_count': len(data['bones']),
        'original_head_box': [7, 6, 6],
        'feature_count': 6, 'positive_closed_cuboid_target_count': 21,
        'reserved_pixels': data['hero_power_path_067']['reserved_pixels'],
        'canonical_head_face_and_joint_pivots_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': ['balanced', 'run', 'power', 'fly', 'swim', 'run_fly', 'run_swim', 'power_fly', 'power_swim'],
        'closed_box_count': 21, 'head_feature_count': 6, 'wing_ray_count': 6, 'halo_bar_count': 4,
        'head_feature_names': head_names, 'wing_mesh_names': wing_names,
        'tail_geometry_unchanged': True,
        'effective_viewer_controls': 'Actual first-Power selection permits signed Run counter-training beyond balance; opposing axes remain independent and reverse-order pairs share one corner.'}
    (ASSETS / 'hero_power067.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('HeroPower067 generated: exact original head/face and 59 original joints; six paired head sections, '
          'five compact body/limb boxes, six substantial wing rays, four halo bars; 21 closed boxes and ten targets.')


if __name__ == '__main__':
    author()
