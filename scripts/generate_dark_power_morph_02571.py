#!/usr/bin/env python3
"""Author the independently observed Dark/Power-first family as closed cuboids.

The retained 7x6x6 skull, face and59 joints stay exact. Five small head boxes
interpret one banded bent horn and the distal fork seen in Run/Fly tendencies;
this is a new family, not the Run family's three rear points/front nubs.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_dark_power_proto071.json'
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
TARGETS = ['dark_power_young'] + NAMES[1:]
FIELD, REGION = 'dark_power_box_071', 'dark_power_region_071'
BODY_UV, MID_UV, TIP_UV, UNDER_UV = ([36.75, 31.75], [36.25, 31.75],
                                     [35.75, 31.75], [35.25, 31.75])
WING_UV, WING_TIP_UV, MARKER_UV = [34.75, 31.75], [34.25, 31.75], [58.5, 1.5]


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
    for face in mesh['faces']:
        face['uv'] = [(distal if i in (4, 5, 6, 7) else proximal)[:] for i in face['v']]
    return mesh


def bands(mesh, proximal, distal):
    # A single source-transparent14-texel footprint includes a broad alternating
    # eight-texel strip. UVs follow the physical local-Z span of each closed box,
    # including both caps, rather than adding separate floating stripe geometry.
    return longitudinal(mesh, [(proximal + .5) / 2, 31.75], [(distal + .5) / 2, 31.75])


def head_features(template):
    # All nine actual mature front/side/back views and all nine half-age sheets
    # were personally opened. Length,width,thickness,pitch,roll per section.
    profiles = {
        'young': ((.85, 1.60, .85, 70, 0), (.60, 1.05, .60, 95, 0), (.45, .55, .40, 120, 0)),
        'balanced': ((1.30, 1.70, 1.05, 65, 0), (1.15, 1.20, .75, 100, 0), (.85, .60, .45, 160, 0)),
        'run': ((1.75, 1.90, 1.10, 28, 0), (1.90, 1.20, .75, 25, 0), (1.15, .65, .48, 25, 0)),
        'power': ((1.45, 2.25, 1.20, 70, 12), (1.25, 1.60, .95, 110, -18), (1.00, .70, .50, 82, 8)),
        'fly': ((1.65, 1.65, .90, 15, 0), (1.75, 1.05, .65, 5, 0), (.70, .65, .50, -8, 0)),
        'swim': ((1.50, 2.05, 1.00, 30, 0), (1.35, 1.50, .90, -35, 0), (1.05, .85, .65, -95, 0)),
        'run_fly': ((1.60, 1.80, 1.00, 20, 0), (1.70, 1.10, .65, 8, 0), (.80, .65, .45, 0, 0)),
        'run_swim': ((1.15, 1.80, .95, 22, 0), (.90, 1.15, .65, 12, 0), (.55, .60, .45, 5, 0)),
        'power_fly': ((1.20, 1.95, 1.00, 30, 0), (1.00, 1.35, .80, 55, 0), (.65, .65, .45, 10, 0)),
        'power_swim': ((.60, 1.80, .60, 5, 0), (.45, 1.20, .45, 10, 0), (.35, .50, .35, 30, 0)),
    }
    values = [{}, {}, {}]
    for key, parts in profiles.items():
        anchor = [0, -5.75, 1.10]
        for index, (length, width, height, pitch, roll) in enumerate(parts):
            overlap = 0 if index == 0 else min(.35 if index == 1 else .30, length * .38)
            rotation = [pitch, 0, roll]
            p = axial(anchor, length, width, height, rotation, overlap)
            values[index][key] = p
            anchor = add(anchor, rotate([0, 0, length - overlap], rotation))
    result = []
    for index, (name, start, end) in enumerate((('root', 60, 62), ('middle', 62, 65), ('tip', 65, 67))):
        result.append(bands(feature(template, 'dark_power_horn_' + name, lattice(values[index]), 'horn_' + name), start, end))
    # Distal forks are real Run/Fly branch anatomy; no front-side nubs or rear
    # side-of-skull quills are invented. At the other targets both tiny positive
    # boxes lie separately inside the terminal horn, so emergence is continuous.
    forks = {'young': (.10, .10, .10, 0, 60), 'balanced': (.10, .10, .10, 0, 60),
             'power': (.10, .10, .10, 0, 60), 'swim': (.10, .10, .10, 0, 60),
             'power_swim': (.10, .10, .10, 0, 60),
             'run': (1.45, .55, .45, 35, 65),
             'fly': (2.10, .70, .55, -30, 70),
             'run_fly': (1.45, .65, .45, -2, 75),
             'run_swim': (.75, .45, .35, 28, 65),
             'power_fly': (1.00, .55, .45, -25, 65)}
    for sign, side in ((-1, 'left'), (1, 'right')):
        branch = {}
        for key, (length, width, height, pitch, yaw) in forks.items():
            center = values[2][key]['center']
            anchor = add(center, [sign * .10, 0, 0])
            branch[key] = axial(anchor, length, width, height, [pitch, sign * yaw, 0], min(.25, length * .2))
        result.append(longitudinal(feature(template, 'dark_power_fork_' + side, lattice(branch), 'fork_tip'), BODY_UV, TIP_UV))
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
                    scale = [1.06 + .04 * power, 1.02 + .02 * fly, 1.04 + .03 * swim + .02 * power]
                elif name.startswith('arm'):
                    scale = [1.08 + .12 * power, 1.06 + .08 * power, 1.07 + .10 * power]
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
    # Closed rays approximate the source's compact bat membrane and colored rim;
    # Fly broadens the fan while all original hinges/animation joints stay exact.
    span = {'young': 2.85, 'balanced': 3.00, 'run': 3.05, 'power': 3.05,
            'fly': 3.40, 'swim': 3.05, 'run_swim': 3.05, 'power_swim': 3.15,
            'run_fly': 3.25, 'power_fly': 3.25}
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
                length = span[key] * ratio * (1 if index == 0 else .85)
                rotation = [0, -sign * .5 if index == 1 else 0, sign * angle]
                delta = rotate([sign * length / 2, 0, 0], rotation)
                values[key] = pose([delta[0], .25 + delta[1], delta[2]], [length, .65 - .08 * index, thickness], rotation)
            mesh = feature(template, 'dark_power_wing_' + side + '_' + name,
                           lattice(values), 'wing_' + name, WING_UV)
            distal = (0, 3, 4, 7) if sign > 0 else (1, 2, 5, 6)
            for face in mesh['faces']:
                face['uv'] = [(WING_TIP_UV if i in distal else WING_UV)[:] for i in face['v']]
            parts.append(mesh)
        bone['meshes'] = parts


def marker(data, template):
    dot = next(b for b in data['bones'] if b['name'] == 'emotion_dot__cube_0')
    for axis in range(3):
        for sign in (-1, 1):
            center, size = [0, 0, 0], [.46, .46, .46]
            center[axis], size[axis] = sign * 1.075, .65
            p = pose(center, size)
            dot['meshes'].append(feature(template, 'dark_power_marker_spike_' + str(axis) + ('_n' if sign < 0 else '_p'),
                                         [copy.deepcopy(p) for _ in TARGETS], 'marker', MARKER_UV))


def tail(data, template):
    lengths = {'young': 2.10, 'balanced': 2.35, 'run': 2.45, 'power': 2.20,
               'fly': 2.60, 'swim': 2.65, 'run_swim': 2.35, 'power_swim': 2.30,
               'run_fly': 2.50, 'power_fly': 2.45}
    roots, tips = {}, {}
    for key, length in lengths.items():
        pitch = -35 if key not in ('run', 'run_fly', 'power_fly') else -45
        anchor, rotation = [0, -.15, 1.75], [pitch, 0, 0]
        roots[key] = axial(anchor, length, .80, .65, rotation, .10)
        end = add(anchor, rotate([0, 0, length - .10], rotation))
        tips[key] = axial(end, 1.10, .75, .55, [-10, 2, 0], .30)
    bone = next(b for b in data['bones'] if b['name'] == 'tail__cube_0')
    mesh = bone['meshes'][0]
    mesh['vertices'] = vertices(mesh, lattice(roots)[0])
    targets(mesh, lattice(roots), 'tail_root')
    bands(mesh, 60, 64)
    bone['meshes'].append(bands(feature(template, 'dark_power_tail_tip', lattice(tips), 'tail_tip'), 64, 67))


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
    data['authored_gameplay_form'] = 'dark_power_proto071'
    data['evolution_revision'] = '0.25.71'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh + nine full signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'dark/power',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['dark_power_path_071'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(swimFly,y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'reserved_pixels': {'band_strip': [[x, 63] for x in range(60, 68)],
                            'feature_body': [73, 63], 'feature_mid': [72, 63], 'feature_tip': [71, 63],
                            'feature_underside': [70, 63], 'wing_root': [69, 63], 'wing_tip': [68, 63]},
        'scope': 'Ordinary living Dark adult with locked POWER FIRST evolution; full signed later axes including opposite Run.',
        'reference': 'Fresh actual Chao Island red Power FIRST checkbox, full Dark alignment, age/axis/orbit controls. Model author personally opened young front/side, all nine mature front/side/back and all nine half-age front/side/back sheets. Actual source has one striped central horn, Run/Fly distal forks, no Run-first rear quills or separate front-side nubs.',
        'source_geometry_copied': False,
        'approximation': 'Original square skull retained. Three closed central boxes provide a stepped/bent striped horn; two small closed distal branches develop as Run/Fly lobes and remain positively embedded for single-horn targets. Run lengthens backward and raises its fork; Power thickens and raises a zigzag center; Fly extends rearward and droops its fork; Swim curls down/back; PowerSwim compresses into a low cap. Mixed corners keep distinct parameters. Curved/tapered spirals, source changing skull mass and conical fists are simplified. Linear age/bilinear training is authored timing, not extracted source formulas.',
        'head_part_names': [m['name'] for m in bones['gradual_head_021']['meshes']],
        'feature_count': 5, 'wing_ray_count': 6, 'positive_closed_cuboid_target_count': 24,
        'paint_policy': 'Eight unused strip texels alternate broad body/mid pigments along the actual central horn and tail box spans; original skull and forehead art remain intact. Fork lobes use body-to-tip pigments. Existing compact hand islands receive broad source-informed bands without UV/geometry stretching.',
        'wing_policy': 'Three closed rays per wing approximate the source compact bat membrane and colored rim on original hinges; Fly broadens the fan. Existing Dark world-orientation and late-floor fit are reused through the scoped sibling predicate on production clocks.',
        'body_contact_policy': 'Torso top extends .45modelpixels for existing scoped walking lift; wrists keep original upper plane and soles/heels fixed. Limbs remain compact rectangular boxes rather than source conical hands.',
        'fork_policy': 'Distal lobes share the central tip region. Run raises a gold fork, Fly droops wider blue lobes, RunFly flattens a fork, RunSwim makes tiny up/out lobes and PowerFly keeps shorter drooping lobes. For young/balanced/Power/Swim/PowerSwim two tiny separate positive cuboids remain embedded in the terminal horn; no opacity swap or zero-size geometry.',
        'marker_policy': 'Original dot core/expression joints/visibility exact; six same-bone closed protrusions interpret the source Dark spiked ball.',
        'tail_policy': 'Two connected closed striped boxes retain original tail base/joint and approximate the source pointed conical tail. Distal width .75 and thickness .55 modelpixels stay inside the established shared prone-transition envelope; original length, centers, rotations and existing scoped folding pivot remain unchanged.',
        'provisional_rates': '960current level-bar progress per opposing axis, independent adult maturity. Viewer establishes no original-game level or morph formula.'}
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
    refs = ['art/imported_chao/models/chao_power.geo.json',
            'art/imported_chao/textures/power.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_power.json',
            'art/imported_chao/models/chao_dark.geo.json',
            'art/imported_chao/textures/dark.png']
    manifest = {'version': '0.25.71', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_emotion_and_59_joint_transforms_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'head_parts': data['dark_power_path_071']['head_part_names'],
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': NAMES[1:]}
    (ASSETS / 'dark_power071.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('DarkPower071: fixed original7x6x6head/face/59joints; fiveheadboxes, sixwingrays, sixmarkerpoints, two stripedtailsections, fivecompactlimbboxes; fresh+nine signedmaturetargets.')


if __name__ == '__main__':
    author()
