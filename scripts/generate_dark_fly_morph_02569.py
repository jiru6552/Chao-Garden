#!/usr/bin/env python3
"""Author the observed Dark/Fly-first family as closed boxes on the baby rig.

Fresh actual viewer controls and nine mature side views establish the paired
lateral horn roots and colored end caps. The original square skull, face and 59 joints stay exact.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_dark_fly_proto069.json'
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
TARGETS = ['dark_fly_young'] + NAMES[1:]
FIELD, REGION = 'dark_fly_box_069', 'dark_fly_region_069'
BODY_UV, MID_UV, TIP_UV, UNDER_UV = ([52.75, 31.75], [52.25, 31.75],
                                     [51.75, 31.75], [51.25, 31.75])
WING_UV, WING_TIP_UV, MARKER_UV = [50.75, 31.75], [50.25, 31.75], [58.5, 1.5]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def add(a, b):
    return [x + y for x, y in zip(a, b)]


def lattice(profiles):
    return [copy.deepcopy(profiles[name]) for name in NAMES]


def targets(mesh, values, region):
    mesh[FIELD] = {field: [[round(float(x), 9) for x in p[key]] for p in values]
                   for field, key in [('centers', 'center'), ('sizes', 'size'), ('rotations', 'rotation')]}
    mesh[REGION] = region
    return mesh


def feature(template, name, values, region, uv=BODY_UV):
    mesh = copy.deepcopy(template)
    for key in ('morph_vertices', 'hero_run_vertices_052', 'hero_run_legacy_vertices_052'):
        mesh.pop(key, None)
    mesh['name'] = name
    mesh['vertices'] = vertices(template, values[0])
    for face in mesh['faces']:
        face['uv'] = [uv[:] for _ in face['v']]
    return targets(mesh, values, region)


def axial(anchor, length, width, height, rotation, overlap=0):
    return pose(add(anchor, rotate([0, 0, length / 2 - overlap], rotation)),
                [width, height, length], rotation)


def longitudinal(mesh, proximal=MID_UV, distal=TIP_UV):
    # Template positive-Z terminal is 4/5/6/7. Four long closed faces carry
    # the two pigments across their actual length, including their interiors.
    for face in mesh['faces']:
        face['uv'] = [(distal if i in (4, 5, 6, 7) else proximal)[:] for i in face['v']]
    return mesh


def head_features(template):
    # Actual independently operated Dark/Fly FIRST viewer: all nine mature
    # front/side/back views and fresh balanced views were personally opened.
    # Exactly two lateral horns: broad base-colored roots, colored end caps.
    # Keep our fixed skull; do not import its source's rising side/head mass.
    # root:length,width,height,pitch,yaw; cap:length,width,height,pitch,yaw.
    # +Y points down; +Z trails rear. Mirrored Y-axis yaw directs +/-X horns.
    profiles = {
        'young': (2.60, 1.40, 1.40, 20, 75, 2.60, .80, .85, -72, 76),
        'balanced': (3.20, 1.45, 1.45, 30, 76, 4.00, .85, .85, -72, 76),
        'run': (2.65, 1.45, 1.45, 35, 75, 1.90, 1.20, 1.35, -8, -80),
        'power': (3.75, 1.50, 1.55, 65, 75, 2.35, .80, .80, 6, -75),
        'fly': (3.20, 1.35, 1.40, 48, 76, 1.75, .75, .75, 28, 78),
        'swim': (2.75, 1.35, 1.35, -3, 80, 3.65, .80, .85, -72, 77),
        'run_swim': (2.55, 1.35, 1.35, 5, 78, 2.05, .85, .85, -58, 78),
        'power_swim': (2.80, 1.45, 1.45, 25, 77, 1.20, .80, .80, -45, 76),
        'run_fly': (3.00, 1.45, 1.45, 42, 76, 1.80, 1.10, 1.15, -7, 85),
        'power_fly': (3.60, 1.40, 1.50, 58, 76, 1.40, .75, .75, 62, 76),
    }
    result = []
    for sign, side in ((-1, 'left'), (1, 'right')):
        roots, caps = {}, {}
        for key, values in profiles.items():
            rl, rw, rh, rx, ry, cl, cw, ch, cx, cy = values
            anchor = [sign * 2.10, -5.60, .80]
            rr, cr = [rx, sign * ry, 0], [cx, sign * cy, 0]
            roots[key] = axial(anchor, rl, rw, rh, rr, .20)
            neck = add(anchor, rotate([0, 0, rl - .20], rr))
            caps[key] = axial(neck, cl, cw, ch, cr, .70)
        result.append(feature(template, 'dark_fly_' + side + '_root',
                              lattice(roots), 'horn_root'))
        result.append(longitudinal(feature(template, 'dark_fly_' + side + '_cap',
                                           lattice(caps), 'horn_cap')))
    return result


def limbs(data):
    for bone in data['bones']:
        name = bone['name']
        if name not in ('body__cube_0', 'arm_left__cube_1', 'arm_right__cube_1',
                        'foot_left__cube_0', 'foot_right__cube_0'):
            continue
        for mesh in bone.get('meshes', []):
            base, values = original_box(mesh), {}
            for key in NAMES:
                rp = -1 if key.startswith('run') else 1 if key.startswith('power') else 0
                swim, fly = int('swim' in key), int('fly' in key)
                power, run = max(0, rp), max(0, -rp)
                if key == 'young':
                    scale = [1.025, 1.015, 1.025]
                elif name.startswith('body'):
                    scale = [1.04 + .065 * power, 1.02 + .02 * fly, 1.04 + .03 * swim + .02 * power]
                elif name.startswith('arm'):
                    scale = [1.06 + .10 * power, 1.05 + .08 * power, 1.05 + .075 * power]
                else:
                    scale = [1.08 + .035 * swim + .02 * power, 1.015 + .02 * power,
                             1.065 + .08 * run + .025 * swim]
                p = copy.deepcopy(base)
                p['size'] = [a * b for a, b in zip(base['size'], scale)]
                if name.startswith('body'):
                    p['center'][1] -= .225
                    p['size'][1] += .45
                if name.startswith('arm'):
                    p['center'][1] = min(v[1] for v in mesh['vertices']) + p['size'][1] / 2
                if name.startswith('foot'):
                    p['center'][1] = max(v[1] for v in mesh['vertices']) - p['size'][1] / 2
                    p['center'][2] = max(v[2] for v in mesh['vertices']) - p['size'][2] / 2
                values[key] = p
            poses = lattice(values)
            mesh['vertices'] = vertices(mesh, poses[0])
            targets(mesh, poses, 'body' if name.startswith('body') else 'hand' if name.startswith('arm') else 'foot')


def wings(data, template):
    # Actual back views show compact three-point bat/arrow wings. Their opaque
    # cuboid rays simplify that contour; Fly develops a larger fan. Every ray
    # shares the original hinge, parent yaw and production animation joint.
    span = {'young': 3.60, 'balanced': 4.00, 'run': 3.50, 'power': 3.70,
            'fly': 4.60, 'swim': 3.00, 'run_swim': 3.10, 'power_swim': 3.40,
            'run_fly': 4.30, 'power_fly': 4.40}
    for bone in data['bones']:
        if bone['name'] not in ('wing_left__cube_0', 'wing_right__cube_0'):
            continue
        sign = -1 if 'left' in bone['name'] else 1
        side = 'left' if sign < 0 else 'right'
        parts = []
        for index, (name, angle, ratio, thickness) in enumerate((('upper', -15, 1, .28),
                                                               ('middle', 18, .80, .24),
                                                               ('lower', 52, .65, .20))):
            values = {}
            for key in NAMES:
                # Preserve the upper ray's full lateral breadth. Shallower
                # inner lobes and a tiny rearward middle tilt keep clean
                # closed fans clear of the fixed skull and compact palms.
                length = span[key] * ratio * (1 if index == 0 else .85)
                rotation = [0, -sign * .5 if index == 1 else 0, sign * angle]
                delta = rotate([sign * length / 2, 0, 0], rotation)
                values[key] = pose([delta[0], .25 + delta[1], delta[2]], [length, .65 - .08 * index, thickness], rotation)
            mesh = feature(template, 'dark_fly_wing_' + side + '_' + name,
                           lattice(values), 'wing_' + name, WING_UV)
            distal = (0, 3, 4, 7) if sign > 0 else (1, 2, 5, 6)
            for face in mesh['faces']:
                face['uv'] = [(WING_TIP_UV if i in distal else WING_UV)[:] for i in face['v']]
            parts.append(mesh)
        bone['meshes'] = parts


def marker(data, template):
    # Observed Dark spiked orb: retain exact original dot core, tilt and
    # expression visibility; six blunt closed protrusions share its cube bone.
    dot = next(b for b in data['bones'] if b['name'] == 'emotion_dot__cube_0')
    for axis in range(3):
        for sign in (-1, 1):
            center, size = [0, 0, 0], [.46, .46, .46]
            center[axis], size[axis] = sign * 1.075, .65
            p = pose(center, size)
            dot['meshes'].append(feature(template, 'dark_fly_marker_spike_' + str(axis) + ('_n' if sign < 0 else '_p'),
                                         [copy.deepcopy(p) for _ in TARGETS], 'marker', MARKER_UV))


def tail(data, template):
    lengths = {'young': 2.10, 'balanced': 2.35, 'run': 2.45, 'power': 2.20,
               'fly': 2.60, 'swim': 2.65, 'run_swim': 2.35, 'power_swim': 2.30,
               'run_fly': 2.50, 'power_fly': 2.45}
    roots, tips = {}, {}
    for key, length in lengths.items():
        # Counter the unchanged 45deg tail joint to give a slim rearward stem.
        pitch = -35 if key not in ('run', 'run_fly', 'power_fly') else -45
        anchor, rotation = [0, -.15, 1.75], [pitch, 0, 0]
        roots[key] = axial(anchor, length, .65, .60, rotation, .10)
        end = add(anchor, rotate([0, 0, length - .10], rotation))
        tips[key] = axial(end, 1.1, .75, .55, [-10, 2, 0], .30)
    bone = next(b for b in data['bones'] if b['name'] == 'tail__cube_0')
    mesh = bone['meshes'][0]
    mesh['vertices'] = vertices(mesh, lattice(roots)[0])
    targets(mesh, lattice(roots), 'tail_root')
    bone['meshes'].append(longitudinal(feature(template, 'dark_fly_tail_tip', lattice(tips), 'tail_tip')))


def author():
    source = MODELS / 'chao_baby.json'
    data = json.loads(source.read_text())
    original = copy.deepcopy(data)
    bones = {b['name']: b for b in data['bones']}
    template = bones['skull__cube_0']['meshes'][0]
    bones['gradual_head_021']['meshes'] = head_features(template)
    limbs(data)
    wings(data, template)
    marker(data, template)
    tail(data, template)
    data['authored_gameplay_form'] = 'dark_fly_proto069'
    data['evolution_revision'] = '0.25.69'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh + nine full signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'dark/fly',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['dark_fly_path_069'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(swimFly,y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'reserved_pixels': {'feature_body': [105, 63], 'feature_mid': [104, 63], 'feature_tip': [103, 63],
                            'feature_underside': [102, 63], 'wing_root': [101, 63], 'wing_tip': [100, 63]},
        'scope': 'Ordinary living Dark adult with locked FLY FIRST evolution; full signed later axes including opposite Swim.',
        'reference': 'Fresh actual Chao Island viewer magenta Fly checkbox and full Dark alignment verified. Personally opened fresh front/side, mature balanced front/side/back/above and all nine mature front/side/back views before authoring. Additional angles and age research retained separately.',
        'source_geometry_copied': False,
        'approximation': 'Original square skull retained exactly. Four closed cuboids create two base-colored lateral horn roots and their colored caps. Balanced has long down-pointing caps; Run compacts/inverts them inward; Power develops tall inward-facing pincers; Fly develops up/out caps; Swim drops long outer caps; mixed targets remain distinct. The source rising side skull mass, curved horns and pointed end bends are simplified to two clean sections per side, without an axial crest or invented fork. Linear age/bilinear training approximates nonlinear source timing.',
        'head_part_names': [m['name'] for m in bones['gradual_head_021']['meshes']],
        'feature_count': 4, 'wing_ray_count': 6, 'positive_closed_cuboid_target_count': 23,
        'visual_refinement': 'Actual initial Minecraft preflight showed short fresh/balanced hanging caps and tiny waist-level fans. Fresh roots lower slightly and caps lengthen, balanced caps lengthen and Power exposes a clearer inward pincer; fans broaden and upright poses raise/reduce folding of only this family. Original head/joints and old family methods remain exact.',
        'wing_policy': 'Three opaque closed cuboid rays approximate each observed two-lobed bat wing on the exact original joint. Fly has the widest/longest fan, RunFly/PowerFly are next and Swim/RunSwim stay compact. Fresh swatches101/100 follow physical ray halves. The upper ray retains full breadth; middle/lower rays are shortened15percent and the middle ray tilts rearward0.5degree around the same hinge for the fixed blocky head and production-pose clearance. This shallower opaque fan approximates the source membrane, including narrower Swim/mixed breadth. Original parent yaw and pivots unchanged.',
        'body_contact_policy': 'Torso top extends .45modelpixels for existing scoped walking lift; wrists keep original upperplane, soles/heels fixed. No intended headfeature contact with torso, hands, wings, feet or tail.',
        'marker_policy': 'Original dot core and all expression joints/visibility exact; six same-bone closed protrusions interpret observed Dark spikedball, never a Hero halo.',
        'tail_policy': 'Two slim connected closed rearward/upturned boxes on the existing tail__cube_0 and joint; original tail__cube_1 base unchanged. Scoped prone stem folding pivots about an interior point of that unchanged base, preserving its attachment.',
        'provisional_rates': '960current level-bar progress per opposing axis, independent adult maturity. Viewer controls establish no original-game level or timing.'}
    actual = {b['name']: b for b in data['bones']}
    assert list(actual) == [b['name'] for b in original['bones']] and len(actual) == 59
    for bone in original['bones']:
        name = bone['name']
        for field in ('pivot', 'rotation', 'parent'):
            assert actual[name].get(field) == bone.get(field), (name, field)
        if name.startswith(('skull', 'expression_', 'emotion_')):
            if name == 'emotion_dot__cube_0':
                assert actual[name]['meshes'][0] == bone['meshes'][0]
            else:
                assert actual[name] == bone, name
    assert data['head_decorations'] == original['head_decorations']
    OUT.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    refs = ['art/imported_chao/models/chao_dark.geo.json',
            'art/imported_chao/models/chao_fly.geo.json',
            'art/imported_chao/models/chao_hero_fly.geo.json',
            'art/imported_chao/textures/dark.png',
            'art/imported_chao/textures/fly.png',
            'art/imported_chao/textures/hero_fly.png']
    manifest = {'version': '0.25.69', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_emotion_and_59_joint_transforms_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'head_parts': data['dark_fly_path_069']['head_part_names'],
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': NAMES[1:]}
    (ASSETS / 'dark_fly069.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('DarkFly069: exact original7x6x6head/face/59joints; fourheadboxes, sixwingrays, sixsameboneorbprotrusions, two tailsections, fivecompactlimbboxes; fresh+nine signedmaturetargets.')


if __name__ == '__main__':
    author()
