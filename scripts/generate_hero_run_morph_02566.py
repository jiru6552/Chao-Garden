#!/usr/bin/env python3
"""Finish the approved blocky Hero/Run adult construction on the original rig.

The055 baby asset and policy remain unchanged. Fresh actual viewer controls and
front/side/back/above views guide these adult shapes, not imported smooth data.
The original square skull and every canonical animation joint stay exact.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_hero_run_proto066.json'
TARGETS = ['hero_run_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
FIELD = 'hero_run_box_066'
REGION = 'hero_run_region_066'
BODY_UV = [63.75, 31.75]
CAP_PROX_UV = [62.75, 31.75]
CAP_DISTAL_UV = [63.25, 31.75]
WING_ROOT_UV = [62.25, 31.75]
WING_DISTAL_UV = [61.75, 31.75]
STRIPE_PROX_UV = [60.1, 30.25]
STRIPE_DISTAL_UV = [61.9, 30.25]
HALO_UV = [58.5, 1.5]
HALO_HEIGHTS = [.65, .25, .35, .45, .15, .45, .15, .10, -.35, -.05]


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


def add(a, b):
    return [x + y for x, y in zip(a, b)]


def axial(anchor, length, width, depth, rotation, overlap=0):
    """Closed box whose negative localY end follows the authored stem direction."""
    return pose(add(anchor, rotate([0, -(length / 2 - overlap), 0], rotation)),
                [width, length, depth], rotation)


def end(anchor, length, rotation):
    return add(anchor, rotate([0, -length, 0], rotation))


def head_features(template):
    # The approved055 vocabulary is retained: one central root/tip and two
    # root/tip fan branches. Actual adult fronts across all nine settings show
    # no separate above-eye nubs; those remain in the unchanged055 baby rig.
    # Root length,width,depth,pitch; cap length,width,depth,pitch.
    stems = {
        'young': (1.25, .70, .70, -10, .65, .45, .45, -12),
        'balanced': (2.00, .80, .85, -15, 2.65, .65, .70, -105),
        'run': (2.50, .85, 1.00, -20, 3.00, 1.05, .80, -100),
        'power': (2.20, .72, .78, -5, 1.45, 1.60, 1.50, -8),
        'fly': (2.05, .82, .90, -20, 1.20, 1.00, 1.00, -35),
        'swim': (2.35, .85, .95, -15, 2.55, 1.00, .82, -102),
        'run_swim': (2.55, .85, 1.00, -22, 3.15, 1.05, .82, -102),
        'run_fly': (1.85, .80, .88, -16, 1.05, 1.25, 1.00, -50),
        'power_swim': (1.95, .78, .85, -9, 1.55, .95, .85, -100),
        'power_fly': (2.00, .80, .88, -15, 1.05, .95, .95, -30),
    }
    # Branch root length,width,depth,pitch,roll; tip length,width,depth,pitch,roll.
    # All pieces retain localY length so painted bands follow the geometry.
    fans = {
        'young': (.12, .10, .10, -10, 0, .10, .08, .08, -10, 0),
        'balanced': (.12, .10, .10, -15, 0, .10, .08, .08, -15, 0),
        'run': (2.65, .95, 1.15, -27, 74, 1.50, .75, .90, -45, 125),
        'power': (.12, .10, .10, -5, 0, .10, .08, .08, -5, 0),
        # Actual first client preflight showed the narrower roots reading as
        # antlers. Broader, outward-tilted closed lobes fill the lower fishtail
        # body while retaining a small upper V notch and the same six parts.
        'fly': (3.40, 2.15, 1.65, -40, 55, 1.30, 1.55, 1.50, -50, 75),
        'swim': (2.50, 1.00, 1.15, -22, 78, 1.65, .80, .95, -40, 130),
        'run_swim': (2.80, .95, 1.15, -30, 73, 1.65, .75, .92, -47, 125),
        'run_fly': (1.80, 1.00, 1.10, -38, 52, .82, .55, .60, -45, 85),
        'power_swim': (1.25, .80, .85, -18, 80, .80, .60, .70, -30, 125),
        'power_fly': (2.60, 1.75, 1.45, -35, 52, 1.00, 1.25, 1.30, -45, 72),
    }
    roots, tips = {}, {}
    branch = {side: ({}, {}) for side in ('left', 'right')}
    for key in NAMES:
        rl, rw, rd, rx, cl, cw, cd, cx = stems[key]
        anchor = [0, -5.72, .72]
        rotation = [rx, 0, 0]
        roots[key] = axial(anchor, rl, rw, rd, rotation)
        neck = end(anchor, rl, rotation)
        overlap = .40 if key != 'young' else .16
        # A small authored twist keeps the root/cap side planes distinct even
        # as Power broadens the cap through the stem width during interpolation.
        # It is real closed geometry, not a validator exclusion or hidden band.
        tips[key] = axial(neck, cl, cw, cd, [cx, 2.5, 0], overlap)
        bl, bw, bd, bx, bz, tl, tw, td, tx, tz = fans[key]
        for sign, side in ((-1, 'left'), (1, 'right')):
            br, bt = branch[side]
            if key in ('young', 'balanced', 'power'):
                # Positive dormant solids remain entirely inside the stalk.
                br[key] = pose(add(roots[key]['center'], [sign * .18, .12, 0]), [bw, bl, bd], rotation)
                bt[key] = pose(add(roots[key]['center'], [sign * .18, -.12, 0]), [tw, tl, td], rotation)
                continue
            base = [0, neck[1] + .30, neck[2] + .25]
            if key in ('run', 'swim', 'run_swim', 'power_swim'):
                base[2] += .35
            brot = [bx, 0, sign * bz]
            br[key] = axial(base, bl, bw, bd, brot, .45)
            bend = end(base, bl - .45, brot)
            if key == 'run_fly':
                # Source Run+Fly has a tiny secondary fan below its upper V.
                # Its two short side prongs attach directly to the same stem.
                small_anchor = [sign * .20, neck[1] + .60, neck[2] + .05]
                bt[key] = axial(small_anchor, tl, tw, td, [tx, 0, sign * tz], .25)
            else:
                bt[key] = axial(bend, tl, tw, td, [tx, 0, sign * tz], .55)
    results = []
    for name, profiles, region in [('hero_run_stem_root', roots, 'stem_root'),
                                   ('hero_run_stem_tip', tips, 'stem_tip')]:
        results.append(feature(template, name, lattice(profiles), region))
    for side in ('left', 'right'):
        br, bt = branch[side]
        results += [feature(template, 'hero_run_fan_' + side + '_root', lattice(br), 'branch_root'),
                    feature(template, 'hero_run_fan_' + side + '_tip', lattice(bt), 'branch_tip')]
    for mesh in results:
        for face in mesh['faces']:
            distal = [i in (2, 3, 6, 7) for i in face['v']]
            if all(distal):
                face['uv'] = [CAP_DISTAL_UV[:] for _ in face['v']]
            elif not any(distal):
                face['uv'] = [CAP_PROX_UV[:] for _ in face['v']]
            else:
                face['uv'] = [(STRIPE_DISTAL_UV if d else STRIPE_PROX_UV)[:] for d in distal]
    return results


def limbs(data):
    # Modest positive box scaling follows approved055 construction and the
    # completed shared rig. Hands/toes retain broad flat faces and connections.
    scales = {
        'young': ((1.00, 1.00, 1.00), (1.00, 1.00, 1.00), (1.00, 1.00, 1.00)),
        'balanced': ((1.03, 1.03, 1.02), (1.04, 1.04, 1.04), (1.08, 1.02, 1.04)),
        'run': ((1.03, 1.03, 1.02), (1.08, 1.08, 1.08), (1.20, 1.05, 1.12)),
        'power': ((1.10, 1.045, 1.09), (1.15, 1.14, 1.12), (1.12, 1.04, 1.12)),
        'fly': ((1.035, 1.045, 1.035), (1.07, 1.065, 1.07), (1.085, 1.01, 1.095)),
        'swim': ((1.065, 1.015, 1.075), (1.085, 1.075, 1.085), (1.155, 1.02, 1.145)),
        'run_swim': ((1.045, 1.025, 1.055), (1.08, 1.075, 1.075), (1.16, 1.02, 1.16)),
        'run_fly': ((1.035, 1.04, 1.035), (1.065, 1.065, 1.065), (1.10, 1.015, 1.12)),
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
            profiles = {}
            for key in NAMES:
                p = copy.deepcopy(base)
                idx = 0 if name.startswith('body') else 1 if name.startswith('arm') else 2
                p['size'] = [a * b for a, b in zip(base['size'], scales[key][idx])]
                if name.startswith('body'):
                    p['center'][1] -= .225
                    p['size'][1] += .45
                if name.startswith('foot'):
                    p['center'][1] = max(v[1] for v in mesh['vertices']) - p['size'][1] / 2
                    p['center'][2] = max(v[2] for v in mesh['vertices']) - p['size'][2] / 2
                profiles[key] = p
            poses = lattice(profiles)
            mesh['vertices'] = vertices(mesh, poses[0])
            targets(mesh, poses, 'body' if name.startswith('body') else 'hand' if name.startswith('arm') else 'foot')


def wings(data, template):
    profiles = {
        'young': (2.10, 1.45, .85, 1.05),
        'balanced': (2.25, 1.55, .95, 1.10),
        'run': (2.05, 1.40, .85, 1.00),
        'power': (2.70, 2.00, 1.25, 1.20),
        'fly': (3.40, 2.60, 1.65, 1.25),
        'swim': (2.50, 1.70, 1.05, 1.00),
        'run_swim': (2.65, 1.80, 1.10, 1.10),
        'run_fly': (3.00, 2.10, 1.30, 1.10),
        'power_swim': (2.70, 1.85, 1.15, 1.10),
        'power_fly': (3.00, 2.35, 1.40, 1.20),
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
            profiles_by_key = {}
            for key, (upper, middle, lower, height) in profiles.items():
                span = (upper, middle, lower)[index]
                rotation = [0, 0, sign * angle]
                offset = rotate([sign * span / 2, 0, 0], rotation)
                profiles_by_key[key] = pose([offset[0], .25 + offset[1], 0],
                                            [span, height * (1 - .10 * index), thickness], rotation)
            name = 'hero_run_wing_' + side + '_' + part
            mesh = feature(template, name, lattice(profiles_by_key), 'wing_' + part, WING_ROOT_UV)
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
    specs = [([0, 0, -2.1], [4.6, .4, .4]), ([0, 0, 2.1], [4.6, .4, .4]),
             ([-2.1, 0, 0], [.4, .4, 4.6]), ([2.1, 0, 0], [.4, .4, 4.6])]
    for index, (center, size) in enumerate(specs):
        poses = [pose([center[0], h, center[2] - .20], size) for h in HALO_HEIGHTS]
        if index == 0:
            original['vertices'] = vertices(original, poses[0])
            for key in ('morph_vertices', 'hero_run_vertices_052', 'hero_run_legacy_vertices_052'):
                original.pop(key, None)
            targets(original, poses, 'halo')
        else:
            mesh = feature(template, 'hero_run_halo_bar_' + str(index), poses, 'halo', HALO_UV)
            data['bones'].append({'name': 'hero_run_halo_066_' + str(index), 'parent': 'emotion_dot',
                                  'pivot': [0, 0, 0], 'rotation': [0, 0, 0], 'meshes': [mesh]})


def author():
    source = MODELS / 'chao_baby.json'
    approved = MODELS / 'chao_hero_run_proto055.json'
    data = json.loads(source.read_text())
    original = copy.deepcopy(data)
    bones = {b['name']: b for b in data['bones']}
    template = bones['skull__cube_0']['meshes'][0]
    bones['gradual_head_021']['meshes'] = head_features(template)
    limbs(data)
    wing_names = wings(data, template)
    halo(data, template)
    data['authored_gameplay_form'] = 'hero_run_proto066'
    data['evolution_revision'] = '0.25.66'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh + nine fully signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'hero/run',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['hero_run_path_066'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(runPower,x)*triangle(swimFly,y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv': BODY_UV, 'feature_cap_proximal_uv': CAP_PROX_UV,
        'feature_cap_distal_uv': CAP_DISTAL_UV, 'feature_stripe_proximal_uv': STRIPE_PROX_UV,
        'feature_stripe_distal_uv': STRIPE_DISTAL_UV,
        'wing_root_uv': WING_ROOT_UV, 'wing_distal_uv': WING_DISTAL_UV, 'halo_uv': HALO_UV,
        'reserved_pixels': {'feature_body': [127, 63], 'cap_proximal': [125, 63],
                            'cap_distal': [126, 63], 'wing_root': [124, 63], 'wing_distal': [123, 63],
                            'stripe_white_a': [120, 60], 'stripe_primary': [121, 60],
                            'stripe_white_b': [122, 60], 'stripe_secondary': [123, 60]},
        'scope': 'Ordinary living Hero adult with locked Run FIRST evolution. Complete signed later axes include Power beyond balance without replacing Run identity or age.',
        'construction_reference': {'approved055_model_sha256': sha(approved),
            'retained': 'Original055 cuboid center-stalk and paired root/tip fan vocabulary; original head/face and canonical rig. The unchanged055 asset continues all babies including the approved two front-side Run nubs.',
            'adult_nubs': 'Fresh and all nine mature independently inspected Hero/Run-first front views do not show separate brow nubs; do not invent persistent baby nubs on the adult.'},
        'reference': 'Fresh independently operated Chao Island viewer, green Run first checkbox, Hero alignment, both opposing axes and age; actual fresh, nine mature front/side/back and mature above evidence personally inspected before authoring.',
        'source_geometry_copied': False,
        'approximation': 'Six closed cuboids reinterpret a short fresh stalk, single rear hook, three-prong Run/Swim fans, blunt opposite-Power bulb and two-lobed Fly/PowerFly fishtail. RunFly has a small secondary fan below the upper V. Fixed skull does not reproduce source wedge deformations. Thin curves, rounded bulbs, broad triangular lobes, gradients and repeated fine bands are simplified. Common fresh target, linear maturity and bilinear training are provisional.',
        'head_caps_policy': 'All six localY principal-axis parts paint four existing longitudinal faces with a four-texel white/primary/white/secondary tile. PositiveY terminal face125 and negativeY terminal face126. No stripe meshes, forehead bands or head texture deformation.',
        'head_part_names': [m['name'] for m in bones['gradual_head_021']['meshes']],
        'wing_policy': 'Three small closed cuboid rays per unchanged wing joint, growing at Fly; directions -12/+18/+48 preserve exact local proximal midpoint[0,.25,0]. Long faces interpolate root124 to distal123.',
        'body_contact_policy': 'Top-only torso extension +.45 supports scoped walking head lift; compact positive hand/foot scaling, exact original joints and floor/rear toe anchoring.',
        'tail_policy': 'Canonical tail geometry/UVs retained unchanged; no extra tail API.',
        'marker_policy': 'Four connected square bars at original emotion anchors and tilt; mesh-only height/forward offsets remain independent of the unchanged joint definitions.',
        'halo_height_targets': HALO_HEIGHTS, 'halo_local_z_offset': -.20,
        'provisional_rates': '960 current level-bar progress per opposing axis, independently normalized adult development. Viewer sliders are not original-game timing or levels.',
    }
    result = {b['name']: b for b in data['bones']}
    for name, bone in {b['name']: b for b in original['bones']}.items():
        for field in ('parent', 'pivot', 'rotation'):
            assert result[name].get(field) == bone.get(field), (name, field)
        if name.startswith(('skull', 'expression_')) or name.startswith('emotion_') and name != 'emotion_dot__cube_0':
            assert result[name] == bone, name
    for name in ('tail', 'tail__cube_0', 'tail__cube_1'):
        assert result[name] == next(b for b in original['bones'] if b['name'] == name)
    assert data['head_decorations'] == original['head_decorations']
    old_uv = [uv for b in original['bones'] for m in b.get('meshes', []) for f in m['faces'] for uv in f['uv']]
    assert max(v for u, v in old_uv) < 30
    OUT.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    manifest = {'version': '0.25.66', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'approved055_model_sha256': sha(approved),
        'target_names': TARGETS, 'canonical_original_head_unchanged': True,
        'original_joint_count': 59, 'bone_count': len(data['bones']),
        'feature_count': 6, 'wing_ray_count': len(wing_names), 'halo_bar_count': 4,
        'positive_closed_cuboid_target_count': 21, 'reserved_pixels': data['hero_run_path_066']['reserved_pixels'],
        'adult_nubs': data['hero_run_path_066']['construction_reference']['adult_nubs'],
        'geometry_source_imported': False, 'geometry_copied': False}
    (ASSETS / 'hero_run066.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('Authored HeroRun066:', len(data['bones']), 'bones, 21 closed cuboid targets, fixed original head; retained055 construction.')
    return manifest


if __name__ == '__main__':
    author()
