#!/usr/bin/env python3
"""Author the observed Dark/Swim-first family as closed boxes on the baby rig.

Fresh actual viewer controls and nine mature side views establish the two
axial rear extensions. The original square skull, face and 59 joints stay exact.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_dark_swim_proto068.json'
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
TARGETS = ['dark_swim_young'] + NAMES[1:]
FIELD, REGION = 'dark_swim_box_068', 'dark_swim_region_068'
BODY_UV, MID_UV, TIP_UV, UNDER_UV = ([55.75, 31.75], [55.25, 31.75],
                                     [54.75, 31.75], [54.25, 31.75])
WING_UV, WING_TIP_UV, MARKER_UV = [53.75, 31.75], [53.25, 31.75], [58.5, 1.5]


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
    # Personally opened all nine actual mature side views before these tables:
    # upper: length,width,height,pitch; tip:length,width,height,pitch.
    upper = {
        'young': (2.8, 1.10, .95, 5, 1.0, .70, .65, 65),
        'balanced': (3.4, 1.10, 1.0, 0, 1.1, .70, .65, 45),
        'run': (3.2, 1.10, 1.15, 65, 1.25, .75, .70, 38),
        'power': (3.4, 1.60, 1.45, 75, 1.3, 1.0, .85, 25),
        'fly': (3.8, 1.20, 1.05, 15, 1.15, .70, .65, 70),
        'swim': (3.0, 1.25, 1.0, 5, 1.05, .80, .65, 15),
        'run_swim': (2.8, 1.10, 1.0, 25, .95, .70, .60, 30),
        'power_swim': (3.2, 1.40, 1.15, 20, 1.0, .85, .70, 25),
        'run_fly': (3.0, 1.10, 1.05, 55, 1.05, .70, .65, 40),
        'power_fly': (3.4, 1.35, 1.15, 60, 1.1, .80, .70, 50),
    }
    # Lower hood is a wide transverse root with a single narrow terminal.
    # Swim drops behind the torso; Run raises both axial extensions. Combined
    # corners use moderate independent profiles instead of stacked maxima.
    lower = {
        'young': (2.7, 5.65, 1.05, 5, 1.1, 1.20, .65, 25),
        'balanced': (3.3, 6.00, 1.15, 5, 1.2, 1.25, .70, 15),
        'run': (3.9, 5.50, 1.15, 58, 1.3, 1.20, .75, 42),
        'power': (3.5, 6.10, 1.40, -20, 1.9, 1.50, .85, -45),
        'fly': (4.2, 5.90, 1.05, 5, 1.3, 1.25, .70, 60),
        'swim': (4.5, 5.90, .95, -20, 2.2, 1.30, .65, -35),
        'run_swim': (2.8, 5.65, 1.05, -5, 1.0, 1.20, .65, 5),
        'power_swim': (4.0, 6.10, 1.10, -12, 1.7, 1.45, .75, -30),
        'run_fly': (3.6, 5.60, 1.10, 40, 1.2, 1.20, .70, 45),
        'power_fly': (3.2, 5.90, 1.15, 0, 1.1, 1.30, .75, 12),
    }
    result = []
    for name, table, anchor, regions in [('upper', upper, [0, -5.65, 1.8], ('crown_root', 'crown_tip')),
                                          ('lower', lower, [0, -3.6, 2.45], ('hood_root', 'hood_tip'))]:
        roots, tips = {}, {}
        for key, values in table.items():
            length, width, height, pitch, tl, tw, th, tp = values
            rotation = [pitch, 0, 0]
            roots[key] = axial(anchor, length, width, height, rotation, .20)
            neck = add(anchor, rotate([0, 0, length - .20], rotation))
            tips[key] = axial(neck, tl, tw, th, [tp, 1.5, 0], .60)
        root = feature(template, 'dark_swim_' + name + '_root', lattice(roots), regions[0])
        # A painted dorsal band interprets the broad blue/green edge; its
        # actual faces remain closed. Different heights avoid coplanar shells.
        for i in (1, 4):
            root['faces'][i]['uv'] = [MID_UV[:] for _ in root['faces'][i]['v']]
        root['faces'][5]['uv'] = [UNDER_UV[:] for _ in root['faces'][5]['v']]
        tip = longitudinal(feature(template, 'dark_swim_' + name + '_tip', lattice(tips), regions[1]))
        result += [root, tip]
    # Actual Power front/back/above shows two tall nearly upright side horns;
    # Swim has downward/outward side fins. Fly and mixes have smaller spurs.
    # Absent forms retain positive small boxes inside the original rear skull.
    spur = {'young': (.12, .10, .10, 0), 'balanced': (.12, .10, .10, 0),
            'run': (.12, .10, .10, 0), 'power': (2.90, .85, .85, 82),
            'swim': (2.10, .75, .65, -25), 'fly': (1.80, .65, .80, 50),
            'run_swim': (.30, .22, .22, 10), 'power_swim': (1.05, .60, .65, 45),
            'run_fly': (.70, .40, .45, 35), 'power_fly': (1.65, .65, .75, 65)}
    for sign, side in ((-1, 'left'), (1, 'right')):
        values = {}
        for key, (length, width, height, pitch) in spur.items():
            anchor = [sign * 2.25, -3.65, 2.65]
            yaw = 38
            if key == 'power':
                anchor, yaw = [sign * 2.55, -4.2, 1.75], 15
            elif key == 'swim':
                anchor, yaw = [sign * 2.70, -2.6, 2.0], 70
            values[key] = axial(anchor, length, width, height, [pitch, sign * yaw, 0], .12)
        result.append(longitudinal(feature(template, 'dark_swim_side_' + side + '_spur',
                                            lattice(values), 'side_spur')))
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
    span = {'young': 1.45, 'balanced': 1.55, 'run': 1.65, 'power': 1.55,
            'fly': 3.60, 'swim': 1.65, 'run_swim': 1.60, 'power_swim': 1.70,
            'run_fly': 2.60, 'power_fly': 3.00}
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
                length = span[key] * ratio
                rotation = [0, 0, sign * angle]
                delta = rotate([sign * length / 2, 0, 0], rotation)
                values[key] = pose([delta[0], .25 + delta[1], 0], [length, .65 - .08 * index, thickness], rotation)
            mesh = feature(template, 'dark_swim_wing_' + side + '_' + name,
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
            dot['meshes'].append(feature(template, 'dark_swim_marker_spike_' + str(axis) + ('_n' if sign < 0 else '_p'),
                                         [copy.deepcopy(p) for _ in TARGETS], 'marker', MARKER_UV))


def tail(data, template):
    lengths = {'young': 2.10, 'balanced': 2.35, 'run': 2.70, 'power': 2.35,
               'fly': 2.80, 'swim': 3.15, 'run_swim': 2.85, 'power_swim': 2.75,
               'run_fly': 2.65, 'power_fly': 2.70}
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
    bone['meshes'].append(longitudinal(feature(template, 'dark_swim_tail_tip', lattice(tips), 'tail_tip')))


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
    data['authored_gameplay_form'] = 'dark_swim_proto068'
    data['evolution_revision'] = '0.25.68'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh + nine full signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'dark/swim',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['dark_swim_path_068'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(swimFly,y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'reserved_pixels': {'feature_body': [111, 63], 'feature_mid': [110, 63], 'feature_tip': [109, 63],
                            'feature_underside': [108, 63], 'wing_root': [107, 63], 'wing_tip': [106, 63]},
        'scope': 'Ordinary living Dark adult with locked SWIM FIRST evolution; full signed later axes including opposite Fly.',
        'reference': 'Fresh actual Chao Island viewer yellow Swim checkbox, full Dark alignment, fresh front/side/back/above and mature balanced side/back/above plus all nine mature side views personally opened before authoring. Additional angles and age research retained separately.',
        'source_geometry_copied': False,
        'approximation': 'Original square skull retained exactly. Narrow upper root/hook plus broad lower transverse hood and narrow terminal, with two small positive lateral spurs. Run raises both rear extensions; Power thickens the upper hook and drops the hood; Swim lengthens the descending rear hood; Fly extends flatter rear hooks and lateral spurs. The source Swim hood reaches near the feet; this closed-box hood is shorter and shallower to clear the retained full gaze, upright skull and forward crawling head. Smooth taper, curved fins, pointed ends and source skull wedge are simplified into six closed cuboids and flat color regions. Linear age/bilinear training approximates nonlinear source timing.',
        'head_part_names': [m['name'] for m in bones['gradual_head_021']['meshes']],
        'feature_count': 6, 'wing_ray_count': 6, 'positive_closed_cuboid_target_count': 25,
        'wing_policy': 'Three opaque closed cuboid rays per exact original wing joint; all rays share proximal hinge[0,.25,0]. Actual Minecraft/viewer preflight showed mature Fly wings hidden behind the arms; Fly/RunFly/PowerFly mature spans are enlarged while other endpoints stay exact. Fresh swatches107/106 follow four longitudinal face interiors. Original parent yaw/pivots and older family animations unchanged.',
        'body_contact_policy': 'Torso top extends .45modelpixels for existing scoped walking lift; wrists keep original upperplane, soles/heels fixed. No intended headfeature contact with torso, hands, wings, feet or tail.',
        'marker_policy': 'Original dot core and all expression joints/visibility exact; six same-bone closed protrusions interpret observed Dark spikedball, never a Hero halo.',
        'tail_policy': 'Two slim connected closed rearward/upturned boxes on existing tail__cube_0 and joint; original tail__cube_1 base unchanged. Scoped prone stem folding pivots about an interior point of that unchanged base, avoiding the head hood while preserving the attachment.',
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
    refs = ['art/imported_chao/models/chao_dark_swim.geo.json',
            'art/imported_chao/textures/dark_swim.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_dark_swim.json']
    manifest = {'version': '0.25.68', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_emotion_and_59_joint_transforms_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'head_parts': data['dark_swim_path_068']['head_part_names'],
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': NAMES[1:]}
    (ASSETS / 'dark_swim068.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('DarkSwim068: exact original7x6x6head/face/59joints; sixheadboxes, sixwingrays, sixsameboneorbprotrusions, two tailsections, fivecompactlimbboxes; fresh+nine signedmaturetargets.')


if __name__ == '__main__':
    author()
