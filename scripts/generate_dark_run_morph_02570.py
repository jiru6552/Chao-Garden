#!/usr/bin/env python3
"""Author the observed Dark/Run-first family as closed boxes on the baby rig.

Fresh actual viewer controls and nine mature side views establish the central
crest, paired rear quills and separate front-side nubs. The original square skull, face and 59 joints stay exact.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_dark_run_proto070.json'
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
TARGETS = ['dark_run_young'] + NAMES[1:]
FIELD, REGION = 'dark_run_box_070', 'dark_run_region_070'
BODY_UV, MID_UV, TIP_UV, UNDER_UV = ([39.75, 31.75], [39.25, 31.75],
                                     [38.75, 31.75], [38.25, 31.75])
WING_UV, WING_TIP_UV, MARKER_UV = [37.75, 31.75], [37.25, 31.75], [58.5, 1.5]


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


def chain(anchor, length, width, height, rotation, tip_length, tip_width,
          tip_height, tip_rotation, overlap=.66):
    root = axial(anchor, length, width, height, rotation)
    neck = add(anchor, rotate([0, 0, length], rotation))
    tip = axial(neck, tip_length, tip_width, tip_height, tip_rotation, overlap)
    return root, tip


def marked_face(mesh, index):
    # Keep pigment on the physical top/spine or outer quill face, not on
    # invented bands or forehead geometry. Local +Z is the rear endpoint.
    face = mesh['faces'][index]
    face['uv'] = [(TIP_UV if i >= 4 else MID_UV)[:] for i in face['v']]
    return mesh


def head_features(template):
    # Personally inspected actual Dark/Run first-type young and nine mature
    # side views, plus Run/Power fronts and balanced front/back controls.
    # The source has THREE large rear points and TWO independent front nubs.
    # Two clean sections per large point retain the imported Run vocabulary;
    # the square skull and its 59 attachment joints are never deformed.
    # central root:length,width,height,pitch; tip:length,width,height,pitch.
    central = {
        'young': (3.15, 2.00, 1.25, 7, 1.50, 1.10, .70, 28),
        'balanced': (3.50, 2.10, 1.35, 13, 1.90, 1.15, .75, 48),
        'run': (4.80, 2.65, 1.65, 4, 3.00, 1.50, .95, 55),
        'power': (3.35, 2.10, 1.45, 67, 1.85, 1.10, .70, 35),
        'fly': (4.00, 2.10, 1.35, 16, 2.20, 1.20, .75, 3),
        'swim': (3.40, 2.20, 1.20, 3, 1.90, 1.20, .70, -6),
        'run_fly': (4.40, 2.50, 1.55, 8, 2.80, 1.40, .85, 8),
        'run_swim': (3.90, 2.50, 1.50, 18, 2.40, 1.40, .85, 35),
        'power_fly': (3.10, 2.00, 1.30, 38, 1.65, 1.05, .70, 12),
        'power_swim': (2.95, 2.10, 1.30, 36, 1.65, 1.10, .70, -5),
    }
    roots, tips = {}, {}
    for key, (rl, rw, rh, rx, tl, tw, th, tx) in central.items():
        roots[key], tips[key] = chain([0, -5.73, .60], rl, rw, rh, [rx, 0, 0],
                                      tl, tw, th, [tx, 0, 0], .70)
    result = [marked_face(feature(template, 'dark_run_crest_root', lattice(roots), 'crest_root'), 4),
              marked_face(feature(template, 'dark_run_crest_tip', lattice(tips), 'crest_tip'), 4)]
    # Actual Run grows wide, long rising rear quills. Power instead develops
    # narrower down-turned hanging ends while keeping its first Run identity.
    # root:length,width,height,pitch,yaw; tip:length,width,height,pitch,yaw.
    rear = {
        'young': (2.15, 1.75, 1.55, -2, 20, 1.35, .95, .80, 13, 14),
        'balanced': (2.45, 1.85, 1.65, -4, 20, 1.55, 1.00, .85, 22, 14),
        'run': (3.55, 2.20, 2.10, -12, 22, 2.65, 1.20, 1.10, 43, 14),
        'power': (1.75, 1.35, 1.30, -27, 63, 2.75, .65, .70, -79, 12),
        'fly': (2.50, 1.65, 1.50, -1, 19, 1.65, .95, .80, 29, 13),
        'swim': (2.30, 1.85, 1.55, -13, 20, 1.60, 1.00, .85, -8, 13),
        'run_fly': (2.85, 2.05, 1.85, -1, 22, 1.70, 1.15, 1.05, 7, 15),
        'run_swim': (3.05, 2.00, 1.90, -10, 22, 2.10, 1.10, .95, 33, 14),
        'power_fly': (1.80, 1.45, 1.30, -10, 33, 1.35, .70, .70, -36, 16),
        'power_swim': (1.95, 1.50, 1.35, -22, 40, 1.95, .75, .75, -60, 15),
    }
    for sign, side in ((-1, 'left'), (1, 'right')):
        roots, tips = {}, {}
        for key, (rl, rw, rh, rx, ry, tl, tw, th, tx, ty) in rear.items():
            roots[key], tips[key] = chain([sign * 2.30, -3.90, 1.60], rl, rw, rh,
                                          [rx, sign * ry, 0], tl, tw, th,
                                          [tx, sign * ty, 0], .72)
        outer = 2 if sign < 0 else 3
        result += [marked_face(feature(template, 'dark_run_rear_' + side + '_root', lattice(roots), 'rear_root'), outer),
                   marked_face(feature(template, 'dark_run_rear_' + side + '_tip', lattice(tips), 'rear_tip'), outer)]
    nubs = {
        'young': (.65, 1.35, .80, -6, 16),
        'balanced': (.65, 1.55, .80, -10, 17),
        'run': (.80, 1.90, 1.00, -4, 17),
        'power': (.65, 1.60, .80, -2, 15),
        'fly': (.65, 1.20, .80, -48, 14),
        'swim': (.75, .85, 1.00, -32, 20),
        'run_fly': (.70, 1.45, .85, -37, 17),
        'run_swim': (.75, 1.35, .90, -23, 19),
        'power_fly': (.65, 1.15, .80, -41, 15),
        'power_swim': (.70, 1.05, .85, -28, 18),
    }
    for sign, side in ((-1, 'left'), (1, 'right')):
        values = {}
        for key, (width, length, depth, pitch, outward) in nubs.items():
            rotation = [pitch, 0, sign * outward]
            anchor = [sign * 2.70, -5.43, -1.25]
            values[key] = pose(add(anchor, rotate([0, -length / 2, 0], rotation)),
                               [width, length, depth], rotation)
        mesh = feature(template, 'dark_run_front_nub_' + side, lattice(values), 'front_nub')
        mesh['faces'][0]['uv'] = [(MID_UV if i in (2, 3, 6, 7) else BODY_UV)[:] for i in mesh['faces'][0]['v']]
        mesh['faces'][4]['uv'] = [MID_UV[:] for _ in range(4)]
        result.append(mesh)
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
    span = {'young': 2.70, 'balanced': 2.90, 'run': 2.80, 'power': 2.95,
            'fly': 3.30, 'swim': 3.10, 'run_swim': 2.85, 'power_swim': 3.00,
            'run_fly': 3.15, 'power_fly': 3.05}
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
            mesh = feature(template, 'dark_run_wing_' + side + '_' + name,
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
            dot['meshes'].append(feature(template, 'dark_run_marker_spike_' + str(axis) + ('_n' if sign < 0 else '_p'),
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
    bone['meshes'].append(longitudinal(feature(template, 'dark_run_tail_tip', lattice(tips), 'tail_tip')))


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
    data['authored_gameplay_form'] = 'dark_run_proto070'
    data['evolution_revision'] = '0.25.70'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh + nine full signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'dark/run',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['dark_run_path_070'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(swimFly,y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'reserved_pixels': {'feature_body': [79, 63], 'feature_mid': [78, 63], 'feature_tip': [77, 63],
                            'feature_underside': [76, 63], 'wing_root': [75, 63], 'wing_tip': [74, 63]},
        'scope': 'Ordinary living Dark adult with locked RUN FIRST evolution; full signed later axes including opposite Power.',
        'reference': 'Fresh actual Chao Island green Run FIRST checkbox, full Dark alignment and actual age/axis/orbit controls verified. Model author personally inspected young/balanced front/side/back, mature Run/Power front/side and all nine mature side sources before authoring. Source confirms three large rear points plus two separate front-side nubs.',
        'source_geometry_copied': False,
        'approximation': 'Exact original square skull retained. Eight closed cuboids provide one two-section central swept crest, a two-section rear quill on each side and two independent front-side nubs. Balanced raises the central end; Run lengthens and broadens back/up quills; Power raises the central stalk and lowers thin side ends; Fly flattens its swept crest and nubs; Swim lowers broad quills and small nubs. Four compatible mixed corners retain distinct shapes. Smooth rising skull mass, tapered horns, hooked tips and rounded nub lobes are simplified. Linear age/bilinear training approximates source timing.',
        'head_part_names': [m['name'] for m in bones['gradual_head_021']['meshes']],
        'feature_count': 8, 'wing_ray_count': 6, 'positive_closed_cuboid_target_count': 27,
        'paint_policy': 'Only central root/tip face4 local-negativeY receives two-region longitudinal top-spine pigment. Rear quills receive pigment on their physical outer lateral faces; nubs have a small front inset and top cap. No forehead geometry or decorative bands are added. Original face UVs remain exact.',
        'wing_policy': 'Three opaque closed cuboid rays approximate each observed compact two-lobed bat membrane on the original joint. Fixed ray directions preserve exact proximal hinges. Fly broadens the fan, Power stays compact. The current Dark Fly world-orientation and late-floor posture fit is reused through a scoped sibling predicate, on the existing production clock.',
        'numeric_visibility_refinement': 'Actual emitted upright sampling across all27 age/axis states found Power and PowerSwim fans too close to their broader torsos. Their authored ray spans increase2.50to2.95 and2.65to3.00 respectively; other seven mature targets, fresh geometry, original hinges and poses remain unchanged. The existing .7modelpixel visibility guard and every collision/continuity limit are retained. Source back views establish compact Power fans relative to the broad Fly fan; obscured torso outlines do not establish an exact balanced-versus-Power ratio.',
        'body_contact_policy': 'Torso top extends .45modelpixels for existing scoped walking lift; wrists keep original upper plane, soles/heels fixed. Limbs retain compact boxes; no intended head-feature contact with torso, hands, wings, feet or tail.',
        'front_nub_policy': 'Two separately authored closed blocks remain above and outside the eyes, positively embedded in the original upper head. They develop taller in Run, flatter/backward in Fly and shorter/rounder in Swim. They are never merged with rear quills.',
        'marker_policy': 'Original dot core and expression joints/visibility exact; six same-bone closed protrusions interpret the observed Dark spiked ball.',
        'tail_policy': 'Two slim connected closed boxes retain the original tail base and joint; scoped prone folding pivots about an interior point of the unchanged base.',
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
    refs = ['art/imported_chao/models/chao_run.geo.json',
            'art/imported_chao/textures/run.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_run.json',
            'art/imported_chao/models/chao_dark.geo.json',
            'art/imported_chao/textures/dark.png',
            'art/imported_chao/models/chao_dark_run.geo.json',
            'art/imported_chao/textures/dark_run.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_dark_run.json']
    manifest = {'version': '0.25.70', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_emotion_and_59_joint_transforms_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'head_parts': data['dark_run_path_070']['head_part_names'],
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': NAMES[1:]}
    (ASSETS / 'dark_run070.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('DarkRun070: exact original7x6x6head/face/59joints; eightheadboxes, sixwingrays, sixsameboneorbprotrusions, two tailsections, fivecompactlimbboxes; fresh+nine signedmaturetargets.')


if __name__ == '__main__':
    author()
