#!/usr/bin/env python3
"""Authored blocky Neutral/Fly first family on the untouched original rig.

Live viewer silhouettes guide four simple head boxes, six solid wing rays and
two tail boxes. Viewer geometry is never imported. All 59 joints, the complete
original square head, pixel face, expressions and smooth orb core stay exact.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_neutral_fly_proto061.json'
TARGETS = ['neutral_fly_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
BODY_UV = [63.75, 31.75]
TIP_UV = [63.25, 31.75]
FIN_UV = [62.75, 31.75]
UNDER_UV = [62.25, 31.75]
FIELD = 'neutral_fly_box_061'
REGION = 'neutral_fly_region_061'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rounded(values):
    return [round(float(x), 9) for x in values]


def targets(mesh, poses, region):
    assert len(poses) == 10
    mesh[FIELD] = {field: [rounded(p[key]) for p in poses]
                   for field, key in [('centers', 'center'), ('sizes', 'size'), ('rotations', 'rotation')]}
    mesh[REGION] = region
    return mesh


def lattice(profiles):
    # First type stays Fly while later training traverses both signed axes.
    return [copy.deepcopy(profiles[name]) for name in
            ('young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced', 'power',
             'run_fly', 'fly', 'power_fly')]


def feature(template, name, poses, region, uv=BODY_UV):
    mesh = copy.deepcopy(template)
    for key in ('morph_vertices', 'hero_run_vertices_052', 'hero_run_legacy_vertices_052'):
        mesh.pop(key, None)
    mesh['name'] = name
    mesh['vertices'] = vertices(template, poses[0])
    for face in mesh['faces']:
        face['uv'] = [uv[:] for _ in face['v']]
    return targets(mesh, poses, region)


def crests(template):
    # Paired upper-lateral crests: two closed boxes per side, no central
    # replacement head or stem. Each terminal overlaps its root by .55px.
    # root length,height,depth,yaw,down; tip length,height,depth,yaw,down.
    # Positive local X is outward. Negative yaw sweeps a right crest rearward.
    profiles = {
        'young': (2.2, 1.35, 1.65, -35, 5, 1.35, .90, 1.10, 5, 30),
        # Reciprocal Swim row: broad rear/up ears, a compact Run curl, and
        # narrower upright Power peaks. Two closed sections approximate the
        # source's rounded/rolled terminals without altering the fixed skull.
        'swim': (2.9, 1.90, 2.10, -55, -28, 1.40, 1.25, 1.25, -60, -65),
        'run_swim': (2.5, 1.90, 2.00, -50, -15, 1.55, 1.30, 1.50, -45, 110),
        'power_swim': (3.4, 1.70, 1.70, -45, -65, 1.20, 1.05, 1.20, -40, -50),
        'balanced': (2.8, 1.30, 1.55, -50, -28, 2.0, .80, 1.05, -65, -25),
        'run': (3.0, 1.85, 1.95, -40, 35, 1.85, 1.10, 1.30, 45, 40),
        'power': (3.6, 1.60, 1.80, -30, -70, 1.5, .85, 1.15, 45, -20),
        'fly': (2.5, 1.65, 1.80, -45, 25, 2.05, .85, 1.20, 20, 65),
        'run_fly': (2.05, 1.65, 1.75, -45, 15, 1.25, .85, 1.05, 25, 55),
        'power_fly': (2.35, 1.75, 1.85, -45, -45, 1.05, .85, 1.10, 45, 18),
    }
    roots, tips = {}, {}
    for key, values in profiles.items():
        length, height, depth, yaw, down, tl, th, td, ty, tz = values
        anchor = [2.25, -5.15, .40]
        rotation = [0, yaw, down]
        roots[key] = pose([a + b for a, b in zip(anchor, rotate([length / 2, 0, 0], rotation))],
                          [length, height, depth], rotation)
        end = [a + b for a, b in zip(anchor, rotate([length, 0, 0], rotation))]
        tr = [0, ty, tz]
        tips[key] = pose([a + b for a, b in zip(end, rotate([tl / 2 - .55, 0, 0], tr))],
                         [tl, th, td], tr)
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
            mesh = feature(template, 'fly_crest_' + side + '_' + section,
                           lattice(mirrored), 'crown_' + section)
            if section == 'tip':
                # Blue outer end on the closed cuboid itself, not a second
                # coincident cap. Body color remains on the other five faces.
                face = mesh['faces'][3 if sign > 0 else 2]
                face['uv'] = [TIP_UV[:] for _ in face['v']]
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
                                ('power', 1, 0), ('fly', 0, 1),
                                ('run_fly', -1, 1), ('power_fly', 1, 1),
                                ('run_swim', -1, -1), ('swim', 0, -1),
                                ('power_swim', 1, -1)]:
                run, power, fly = max(0, -rp), max(0, rp), max(0, sf)
                p = copy.deepcopy(base)
                if key == 'young':
                    scale = [1, 1, 1]
                elif name.startswith('body'):
                    scale = [1.025 + .075 * power, 1.015 + .02 * fly,
                             1.025 + .025 * power]
                elif name.startswith('arm'):
                    scale = [1.025 + .10 * power, 1.025 + .09 * power,
                             1.025 + .075 * power]
                else:
                    scale = [1.025 + .025 * power, 1 + .025 * power,
                             1.035 + .095 * run + .025 * power]
                p['size'] = [a * b for a, b in zip(base['size'], scale)]
                if name.startswith('body'):
                    # Top-only .45px support for the scoped full-head .35px
                    # walking lift. Original lower plane and all joints stay.
                    p['center'][1] -= .225
                    p['size'][1] += .45
                if name.startswith('foot'):
                    p['center'][1] = max(v[1] for v in mesh['vertices']) - p['size'][1] / 2
                    p['center'][2] = max(v[2] for v in mesh['vertices']) - p['size'][2] / 2
                profiles[key] = p
            poses = lattice(profiles)
            # Parser derives rigid corner correspondence from target0.
            mesh['vertices'] = vertices(mesh, poses[0])
            targets(mesh, poses, 'body' if name.startswith('body') else
                    'hand' if name.startswith('arm') else 'foot')


def wings(data, template):
    # Three opaque closed rays per ORIGINAL wing bone; their common embedded
    # root-face midpoint is exactly [0,.25,0]. Differing positive thicknesses
    # separate overlapping front/back planes and prevent coplanar z-fighting.
    # The fresh green fan is broad; mature balanced shrinks, Power is compact,
    # Fly opens a long fan, and the two compatible pairs are moderated.
    profiles = {
        'young': (4.70, 3.55, 2.75, 1.10),
        'swim': (4.90, 3.60, 2.40, 1.45),
        'run_swim': (5.20, 3.80, 2.60, 1.35),
        'power_swim': (3.50, 2.60, 1.85, 1.25),
        'balanced': (3.80, 2.85, 2.15, 1.00),
        'run': (4.95, 3.65, 2.70, 1.00),
        'power': (2.70, 2.10, 1.65, 1.05),
        'fly': (6.20, 4.95, 3.80, 1.18),
        'run_fly': (5.35, 4.00, 3.00, 1.10),
        'power_fly': (4.60, 3.45, 2.60, 1.12),
    }
    for bone in data['bones']:
        if bone['name'] not in ('wing_left__cube_0', 'wing_right__cube_0'):
            continue
        sign = -1 if 'left' in bone['name'] else 1
        side = 'left' if sign < 0 else 'right'
        rays = []
        for index, (part, angle, thickness) in enumerate(
                [('upper', -8, .30), ('middle', 23, .25), ('lower', 53, .20)]):
            values = {}
            for key, (upper, middle, lower, height) in profiles.items():
                span = (upper, middle, lower)[index]
                r = [0, 0, sign * angle]
                delta = rotate([sign * span / 2, 0, 0], r)
                values[key] = pose([delta[0], .25 + delta[1], 0],
                                   [span, height * (1 - index * .1), thickness], r)
            mesh = feature(template, 'fly_wing_' + side + '_' + part,
                           lattice(values), 'wing_' + part, FIN_UV)
            # Adjacent opaque swatches create a clean half-length band on
            # every long face, rather than hiding distal pigment on a thin
            # end cap. Per-vertex UVs preserve each face's actual corner
            # correspondence. Point sampling switches 125 -> 124 at U62.5.
            distal = {0, 3, 4, 7} if sign > 0 else {1, 2, 5, 6}
            for face in mesh['faces']:
                face['uv'] = [(UNDER_UV if index in distal else FIN_UV)[:]
                              for index in face['v']]
            rays.append(mesh)
        bone['meshes'] = rays


def tail(data, template):
    bone = next(b for b in data['bones'] if b['name'] == 'tail__cube_0')
    lengths = {'young': 2.65, 'balanced': 2.65, 'run': 1.95, 'power': 1.85,
               'fly': 3.85, 'run_fly': 2.50, 'power_fly': 2.35,
               'swim': 2.80, 'run_swim': 1.90, 'power_swim': 2.10}
    roots, tips = {}, {}
    for key, length in lengths.items():
        anchor, r, tr = [0, .5, 1.85], [-35, 0, 0], [-2, 0, 0]
        if key in ('swim', 'run_swim', 'power_swim'):
            # The opposite row has a flatter tail than Fly's rising curl.
            r = [-10 if key == 'swim' else -5, 0, 0]
            tr = [-5, 0, 0]
        roots[key] = pose([a + b for a, b in zip(anchor, rotate([0, 0, length / 2], r))],
                          [.60, .60, length], r)
        end = [a + b for a, b in zip(anchor, rotate([0, 0, length], r))]
        tips[key] = pose([a + b for a, b in zip(end, rotate([0, 0, .90 / 2 - .30], tr))],
                         [.65, .55, .90], tr)
    bone['meshes'] = [feature(template, 'fly_tail_root', lattice(roots), 'tail_root'),
                      feature(template, 'fly_tail_tip', lattice(tips), 'tail_tip', TIP_UV)]


def author():
    source = MODELS / 'chao_baby.json'
    data = json.loads(source.read_text())
    original = copy.deepcopy(data)
    bones = {b['name']: b for b in data['bones']}
    template = bones['skull__cube_0']['meshes'][0]
    bones['gradual_head_021']['meshes'] = crests(template)
    limbs(data)
    wings(data, template)
    tail(data, template)
    data['authored_gameplay_form'] = 'neutral_fly_proto061'
    data['evolution_revision'] = '0.25.72'
    data['gradual_evolution'] = {
        'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh +9 distinct mature signed-axis closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'neutral/fly',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['neutral_fly_path_061'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(clamp(runPower,-1,1),x)*triangle(clamp(swimFly,-1,1),y), x/y=-1,0,1; first evolution remains Fly across balance',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv': BODY_UV, 'feature_tip_uv': TIP_UV,
        'feature_fin_uv': FIN_UV, 'feature_underside_uv': UNDER_UV,
        'reserved_pixels': {'feature_body': [127, 63], 'feature_tip': [126, 63],
                            'feature_fin': [125, 63], 'feature_underside': [124, 63]},
        'scope': 'Ordinary living Neutral adult with locked Fly FIRST evolution only. Full nine-corner Run/Power and Swim/Fly adult lattice: balanced, Run, Power, Swim, Fly and four compatible pairs.',
        'reference': 'Live Chao Island first-Fly checkbox, Neutral alignment: fresh and existing six mature references plus independently captured reciprocal Swim/Run+Swim/Power+Swim front/side/back and intermediate development. Original official Fly geometry and texture are construction vocabulary only.',
        'source_geometry_copied': False,
        'approximation': 'Exact original square head and pixel face. Existing young and six mature targets are preserved. Two closed boxes per upper-lateral crest approximate reciprocal Swim broad rear/up ears, Run+Swim compact inward/down curls, and Power+Swim narrower upright peaks; smooth rolled ends and small source side-fin lobes are simplified into the existing paired crests. Six opaque closed wing rays simplify smooth three-lobe fans; two-color half-length bands replace smooth gradients. Reciprocal Swim tails flatten relative to the original rising Fly curl. No new joints, central stem, halo or spikeball.',
        'wing_policy': 'Three opaque closed cuboid rays per original wing bone, all root-face midpoints fixed local [0,.25,0]; distinct .30/.25/.20 thicknesses avoid coincident parallel planes. Per-vertex FIN125 at each root-X edge and UNDER124 at each distal-X edge produce a clean half-length root/distal band on all four long faces; end faces use their matching constant swatch. All original wing parents/pivots/animations unchanged; no alpha closing-edge frame.',
        'body_contact_policy': 'New family torso top extends .45 model pixels on every target; bottom, original59bone transforms, originalhead and attachment rig unchanged. Supports scoped .35pixel WALK head-clearance lift.',
        'marker_policy': 'Entire original orb, marker and expression bones/meshes byte-exact; live reference uses a smooth sphere, so no added marker geometry.',
        'provisional_rates': '960 current level-bar progress per opposing axis; independent adult maturity. No original-game timing claim.'}
    actual = {b['name']: b for b in data['bones']}
    assert len(actual) == len(original['bones']) == 59
    for bone in original['bones']:
        name = bone['name']
        for field in ('pivot', 'rotation', 'parent'):
            assert actual[name].get(field) == bone.get(field), (name, field)
        if name.startswith(('skull', 'expression_', 'emotion_')):
            assert actual[name] == bone, name
    OUT.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    refs = ['art/imported_chao/models/chao_fly.geo.json',
            'art/imported_chao/textures/fly.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_fly.json']
    manifest = {
        'version': '0.25.72', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_orb_and_joint_pivots_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': ['balanced', 'run', 'power', 'swim', 'fly', 'run_swim', 'power_swim', 'run_fly', 'power_fly'],
        'reciprocal_row': 'Distinct negative-Swim targets; full signed Swim/Fly axis with locked first Fly identity',
        'closed_box_count': 17, 'head_feature_count': 4, 'wing_ray_count': 6,
        'tail_part_count': 2}
    (ASSETS / 'neutral_fly061.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('NeutralFly061 generated: exact original7x6x6 head/face/orb/59joints;4headfeatures,5compactbodylimbs,6opaqueanchoredwingrays,2tailparts;17closedboxes,10targets/9matureforms.')


if __name__ == '__main__':
    author()
