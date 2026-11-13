#!/usr/bin/env python3
"""Authored blocky Dark/Normal adult family on the original shared baby rig.

Fresh/mature silhouettes were personally inspected in a fresh live viewer
session. These clean closed cuboids interpret anatomy; no viewer mesh is used.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_dark_normal_proto060.json'
TARGETS = ['dark_normal_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
BODY_UV = [63.75, 31.75]
TIP_UV = [63.25, 31.75]
FIN_UV = [62.75, 31.75]
UNDER_UV = [62.25, 31.75]
MARKER_UV = [58.5, 1.5]
FIELD = 'dark_normal_box_060'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rounded(values):
    return [round(float(x), 9) for x in values]


def targets(mesh, poses):
    assert len(poses) == 10
    mesh[FIELD] = {field: [rounded(p[key]) for p in poses]
                   for field, key in [('centers', 'center'), ('sizes', 'size'), ('rotations', 'rotation')]}
    return mesh


def lattice(profiles):
    return [copy.deepcopy(profiles[name]) for name in
            ('young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
             'power', 'run_fly', 'fly', 'power_fly')]


def feature(template, name, poses, region, uv=BODY_UV):
    mesh = copy.deepcopy(template)
    for key in ('morph_vertices', 'hero_run_vertices_052', 'hero_run_legacy_vertices_052'):
        mesh.pop(key, None)
    mesh['name'] = name
    mesh['vertices'] = vertices(template, poses[0])
    mesh['dark_normal_region_060'] = region
    for face in mesh['faces']:
        face['uv'] = [uv[:] for _ in face['v']]
    return targets(mesh, poses)


def crests(template):
    # One posterior head extension, NOT Run-first spikes, Hero antlers or
    # Swim-first side flippers. Three tapered steps retain a single coherent
    # angular contour. Local +Z is the rear; positive X pitch points up/back.
    # rootLength,width,height,pitch; middleLength,width,height,pitch;
    # tipLength,width,height,pitch. All roots share an embedded head anchor.
    profiles = {
        'young': (2.05, 2.80, 1.75, -12, 1.05, 1.65, 1.05, 28, .80, .70, .60, 42),
        'balanced': (2.45, 2.95, 1.85, -35, 1.10, 1.70, 1.10, -55, .80, .75, .62, 8),
        'run': (3.55, 2.55, 1.70, 62, 1.15, 1.30, .90, 15, .85, .60, .55, 4),
        'power': (3.20, 3.25, 2.15, 68, 1.05, 1.90, 1.10, -12, .80, .85, .65, 20),
        'fly': (2.70, 2.55, 1.75, 60, 1.45, 1.35, .90, 0, 1.05, .65, .55, 68),
        'swim': (2.85, 3.05, 1.95, -40, 1.35, 1.65, 1.00, -62, .90, .70, .60, 18),
        'run_swim': (1.40, 2.75, 1.75, 12, .80, 1.35, .90, 5, .65, .60, .52, 0),
        'power_swim': (1.45, 3.30, 2.05, -2, .80, 1.65, 1.00, -12, .70, .75, .60, 10),
        'run_fly': (3.20, 2.55, 1.70, 55, 1.05, 1.35, .90, 35, .85, .65, .55, 30),
        'power_fly': (2.80, 3.05, 1.95, 42, 1.20, 1.65, 1.05, 30, .90, .75, .60, 25),
    }
    parts = [{}, {}, {}]
    for key, values in profiles.items():
        anchor = [0, -3.75, 2.30]
        for index in range(3):
            length, width, height, pitch = values[index * 4:index * 4 + 4]
            overlap = (0, .35, .25)[index]
            rotation = [pitch, 0, 0]
            delta = rotate([0, 0, length / 2 - overlap], rotation)
            parts[index][key] = pose([a + b for a, b in zip(anchor, delta)],
                                     [width, height, length], rotation)
            anchor = [a + b for a, b in zip(anchor, rotate([0, 0, length - overlap], rotation))]
    result = [feature(template, 'dark_crest_' + name, lattice(values), region, uv)
              for name, values, region, uv in
              [('root', parts[0], 'crown_root', BODY_UV),
               ('middle', parts[1], 'crown_fin', FIN_UV),
               ('tip', parts[2], 'crown_tip', TIP_UV)]]
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
            poses = [copy.deepcopy(base)]
            for y in (-1, 0, 1):
                for x in (-1, 0, 1):
                    run, power, swim, fly = max(0, -x), max(0, x), max(0, -y), max(0, y)
                    p = copy.deepcopy(base)
                    if name.startswith('body'):
                        scale = [1.025 + .065 * power, 1.015 + .025 * fly,
                                 1.025 + .035 * swim + .02 * power]
                    elif name.startswith('arm'):
                        scale = [1.025 + .12 * power, 1.025 + .10 * power,
                                 1.025 + .075 * power]
                    else:
                        scale = [1.025 + .045 * swim + .025 * power,
                                 1 + .025 * power, 1.035 + .095 * run + .03 * swim]
                    p['size'] = [a * b for a, b in zip(base['size'], scale)]
                    if name.startswith('foot'):
                        p['center'][1] = max(v[1] for v in mesh['vertices']) - p['size'][1] / 2
                        p['center'][2] = max(v[2] for v in mesh['vertices']) - p['size'][2] / 2
                    poses.append(p)
            if name.startswith('body'):
                # Top only: follow the scoped .35px WALK head clearance lift
                # while preserving the original torso bottom and every pivot.
                for p in poses:
                    p['center'][1] -= .225
                    p['size'][1] += .45
                mesh['vertices'] = vertices(mesh, poses[0])
            targets(mesh, poses)


def wings(data):
    # Exact original hinge; both sides use the original visible front artwork
    # and the thin closing edges track its existing alpha cutout per vertex.
    profiles = {'young': (3.10, 2.15), 'balanced': (3.20, 2.25),
                'run': (3.30, 1.95), 'power': (2.90, 2.20),
                'fly': (3.90, 2.65), 'swim': (3.10, 2.25),
                'run_fly': (3.60, 2.30), 'run_swim': (3.30, 2.10),
                'power_fly': (3.30, 2.50), 'power_swim': (2.95, 2.25)}
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
            mesh['dark_normal_region_060'] = 'wing'
            targets(mesh, values)


def marker(data, template):
    # Six small closed cuboids on the SAME existing dot cube bone. Original
    # dot mesh/UV, marker tilt, expression visibility and all59bones stay exact.
    # This is a blocky spikeball interpretation, not a faceted sphere copy.
    dot = next(b for b in data['bones'] if b['name'] == 'emotion_dot__cube_0')
    for axis in range(3):
        for sign in (-1, 1):
            center, size = [0, 0, 0], [.46, .46, .46]
            center[axis] = sign * 1.075
            size[axis] = .65
            p = pose(center, size)
            dot['meshes'].append(feature(template, 'dark_marker_spike_' + str(axis) + ('_n' if sign < 0 else '_p'),
                                         [copy.deepcopy(p) for _ in TARGETS], 'marker', MARKER_UV))


def tail(data, template):
    # A slim two-block tail with a bent colored tip; original tail joint and
    # animal-tail visibility stay authoritative. Fly lengthens it markedly.
    bone = next(b for b in data['bones'] if b['name'] == 'tail__cube_0')
    mesh = bone['meshes'][0]
    profiles = {'young': 2.10, 'balanced': 2.35, 'run': 2.25, 'power': 2.20,
                'fly': 3.80, 'swim': 2.45, 'run_fly': 2.80, 'run_swim': 2.30,
                'power_fly': 2.65, 'power_swim': 2.30}
    roots, tips = {}, {}
    for key, length in profiles.items():
        # Counter the unchanged parent tail's45deg pitch. The long stem is
        # about10deg above horizontal in the body frame; its small terminal
        # block turns up~35deg, as the independently viewed DarkNormal tail.
        base, rotation, tip_rotation = [0, .5, 1.95], [-35, 0, 0], [-10, 0, 0]
        roots[key] = pose([a + b for a, b in zip(base, rotate([0, 0, length / 2], rotation))],
                          [.65, .65, length], rotation)
        end = [a + b for a, b in zip(base, rotate([0, 0, length], rotation))]
        tips[key] = pose([a + b for a, b in zip(end, rotate([0, 0, .95 / 2 - .25], tip_rotation))],
                         [.80, .55, .95], tip_rotation)
    values = lattice(roots)
    mesh['vertices'] = vertices(mesh, values[0])
    mesh['dark_normal_region_060'] = 'tail_root'
    targets(mesh, values)
    bone['meshes'].append(feature(template, 'dark_tail_tip', lattice(tips), 'tail_tip', TIP_UV))


def author():
    source = MODELS / 'chao_baby.json'
    data = json.loads(source.read_text())
    original = copy.deepcopy(data)
    bones = {b['name']: b for b in data['bones']}
    template = bones['skull__cube_0']['meshes'][0]
    bones['gradual_head_021']['meshes'] = crests(template)
    limbs(data)
    wings(data)
    marker(data, template)
    tail(data, template)
    data['authored_gameplay_form'] = 'dark_normal_proto060'
    data['evolution_revision'] = '0.25.60'
    data['gradual_evolution'] = {
        'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh +9 mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'dark/normal',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['dark_normal_path_060'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(swimFly,y), x/y=-1,0,1; full signed axes',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv': BODY_UV, 'feature_tip_uv': TIP_UV,
        'feature_fin_uv': FIN_UV, 'feature_underside_uv': UNDER_UV, 'marker_uv': MARKER_UV,
        'reserved_pixels': {'feature_body': [127, 63], 'feature_tip': [126, 63],
                            'feature_fin': [125, 63], 'feature_underside': [124, 63]},
        'scope': 'Ordinary living Dark adult with locked NORMAL first evolution only. All9 compatible signed-axis specializations; no Run+Power or Swim+Fly stacking.',
        'reference': 'Fresh live Chao Island viewer independently operated: actual fresh balanced/Run, all9 mature front/side/back, additional top/bottom views; intermediate research independently recorded. Original official Dark, Dark Run and Dark Swim assets inspected as cuboid construction vocabulary only.',
        'source_geometry_copied': False,
        'approximation': 'Original square skull retained exactly. One three-section closed blocky posterior head extension: fresh small upturned hook; balanced downward hood; Run long raised crest; Power broader shorter raised hook; Fly rising then upturned hook; Swim long dropped hood; mixed corners moderated. Original pixel eyes and expressions are retained without new permanent brows. Six blunt cube protrusions interpret spikeball; two-block slim tail and thin closed original-UV wings simplify smooth curves, taper and bat-wing shapes.',
        'wing_policy': 'Original visible front UV mirrored onto closed back, per-vertex alpha-consistent thin closing edges, fixed local root-edge midpoint [0,.25,0], original parent yaw/pivots/animations.',
        'body_contact_policy': 'New family torso top extends .45 model pixels on every target; bottom, original59bone transforms, originalhead and attachment rig unchanged. Supports scoped .35pixel WALK head-clearance lift.',
        'marker_policy': 'Original dot cuboid and all expression/marker joints exact. Six new fixed-positive boxes share that existing cube bone and its visibility; no spikes on heart/question/exclamation replacements.',
        'provisional_rates': '960 current level-bar progress per opposing axis; independent adult maturity. No original-game timing claim.'}
    actual = {b['name']: b for b in data['bones']}
    assert len(data['bones']) == len(original['bones']) == 59
    for bone in original['bones']:
        name = bone['name']
        for field in ('pivot', 'rotation', 'parent'):
            assert actual[name].get(field) == bone.get(field), (name, field)
        if name.startswith(('skull', 'expression_', 'emotion_')):
            if name == 'emotion_dot__cube_0':
                assert actual[name]['meshes'][0] == bone['meshes'][0]
            else:
                assert actual[name] == bone, name
    OUT.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    refs = ['art/imported_chao/models/chao_dark.geo.json',
            'art/imported_chao/models/chao_dark_run.geo.json',
            'art/imported_chao/models/chao_dark_swim.geo.json',
            'art/imported_chao/textures/dark.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_dark.json']
    manifest = {
        'version': '0.25.60', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_and_joint_pivots_unchanged': True,
        'original_marker_core_and_expression_visibility_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': ['balanced', 'run', 'power', 'fly', 'swim',
                                  'run_fly', 'run_swim', 'power_fly', 'power_swim']}
    (ASSETS / 'dark_normal060.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('DarkNormal060 generated: exact original7x6x6 head/face/59joints;3head features,6marker protrusions,5compact body/limb boxes,2tail boxes,2closed anchored wings;10targets/all9matureforms.')


if __name__ == '__main__':
    author()
