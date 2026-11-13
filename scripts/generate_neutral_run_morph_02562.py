#!/usr/bin/env python3
"""Authored blocky Neutral/Run-first family on the untouched original baby rig.

The independent live viewer supplies directions/proportions, never geometry.
Eight head boxes retain the approved official-addon Run vocabulary: a swept
central crest, two stepped rear points and separate front-side nubs. The
original skull, pixel eyes, orb and all 59 animation joints remain exact.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box, rotate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
OUT = MODELS / 'chao_neutral_run_proto062.json'
TARGETS = ['neutral_run_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
NAMES = ['young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
         'power', 'run_fly', 'fly', 'power_fly']
BODY_UV = [63.75, 31.75]
TIP_UV = [63.25, 31.75]
FIN_UV = [62.75, 31.75]
UNDER_UV = [62.25, 31.75]
TAIL_UV = [25.25, 16.25]  # Existing opaque tail texel with independent tail pigment.
NUB_UV = [14.25, 16.25]  # Existing opaque hand texel, not a new atlas region.
STRIPE_UV_MIN = [56.025, 16.025]  # Verified-unused native tile x112..118,y32..33.
STRIPE_UV_MAX = [59.475, 16.975]  # Padded away from adjacent source texels.
FIELD = 'neutral_run_box_062'
REGION = 'neutral_run_region_062'


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
    return [copy.deepcopy(profiles[name]) for name in NAMES]


def feature(template, name, poses, region, uv=BODY_UV):
    mesh = copy.deepcopy(template)
    for key in ('morph_vertices', 'hero_run_vertices_052', 'hero_run_legacy_vertices_052'):
        mesh.pop(key, None)
    mesh['name'] = name
    mesh['vertices'] = vertices(template, poses[0])
    for face in mesh['faces']:
        face['uv'] = [uv[:] for _ in face['v']]
    return targets(mesh, poses, region)


def chain(anchor, length, width, height, rotation, tip_length, tip_width,
          tip_height, tip_rotation, overlap=.62):
    """Two closed boxes; substantial shared volume survives parameter blending."""
    root = pose([a + b for a, b in zip(anchor, rotate([0, 0, length / 2], rotation))],
                [width, height, length], rotation)
    end = [a + b for a, b in zip(anchor, rotate([0, 0, length], rotation))]
    tip = pose([a + b for a, b in zip(end, rotate([0, 0, tip_length / 2 - overlap], tip_rotation))],
               [tip_width, tip_height, tip_length], tip_rotation)
    return root, tip


def head_features(template):
    # width/height/length/pitch for each central section. +X pitch sends the
    # Z-directed crest up/back; negative pitch creates a down/back hook.
    central = {
        'young': (2.35, 1.40, 3.45, 30, 1.30, .80, 1.70, 10),
        'balanced': (2.45, 1.50, 3.75, 27, 1.40, .85, 1.85, 6),
        'run': (3.05, 1.85, 4.40, 14, 1.90, 1.15, 3.20, -44),
        'power': (2.90, 1.70, 3.70, 53, 1.65, 1.00, 1.85, 43),
        'fly': (2.45, 1.55, 3.70, 69, 1.35, .85, 1.55, 78),
        'swim': (2.55, 1.55, 3.20, 20, 1.35, .90, 1.45, -5),
        'run_swim': (2.90, 1.70, 4.00, 10, 1.75, 1.00, 2.25, -30),
        'run_fly': (2.80, 1.65, 3.50, 18, 1.60, 1.05, 2.15, -58),
        'power_swim': (2.65, 1.60, 3.25, 37, 1.45, .95, 1.70, 24),
        'power_fly': (2.70, 1.65, 3.70, 57, 1.45, .95, 1.75, 53),
    }
    roots, tips = {}, {}
    for key, (w, h, length, pitch, tw, th, tl, tp) in central.items():
        roots[key], tips[key] = chain([0, -5.65, .60], length, w, h, [pitch, 0, 0],
                                      tl, tw, th, [tp, 0, 0], overlap=.68)
    result = [feature(template, 'run_crest_root', lattice(roots), 'crown_root'),
              feature(template, 'run_crest_tip', lattice(tips), 'crown_tip')]
    # User-directed placement: three longitudinal lanes continue from the
    # original head TOP along the central crest, never across the forehead.
    # Only the existing local-negative-Y broad face receives this small tile.
    # U follows physical width (localX), V runs proximal->distal (localZ).
    # The tile has alternating body/mark/body/mark/body/mark/body columns.
    for central_mesh in result:
        top = central_mesh['faces'][4]
        top['uv'] = [[STRIPE_UV_MAX[0] if i in (0, 3, 4, 7) else STRIPE_UV_MIN[0],
                      STRIPE_UV_MAX[1] if i >= 4 else STRIPE_UV_MIN[1]]
                     for i in top['v']]
    # A dark endpoint belongs to the existing closed tip, not an extra cap.
    result[-1]['faces'][1]['uv'] = [TIP_UV[:] for _ in range(4)]

    # Rear pair keeps its root embedded at the back corners of the original
    # skull. Run droops into long hooks; Fly lifts three tall prongs; Swim
    # compresses these into short broad lobes with a larger paddle tail.
    rear = {
        'young': (2.45, 2.15, 2.65, 2, 18, 1.45, 1.35, 1.80, 8, 8),
        'balanced': (2.55, 2.30, 2.85, 4, 18, 1.55, 1.45, 2.00, 4, 8),
        'run': (2.90, 2.75, 3.65, -14, 20, 1.85, 1.85, 3.00, -43, 8),
        'power': (2.45, 2.40, 3.25, 48, 10, 1.40, 1.35, 1.85, 61, 5),
        'fly': (2.00, 2.15, 3.35, 70, 7, 1.20, 1.15, 2.15, 86, 3),
        'swim': (2.65, 2.20, 1.75, -4, 14, 1.65, 1.40, 1.20, 1, 4),
        'run_swim': (2.70, 2.50, 3.10, -6, 18, 1.70, 1.55, 2.35, -20, 6),
        'run_fly': (2.65, 2.45, 2.55, -15, 17, 1.70, 1.65, 2.55, -66, 4),
        'power_swim': (2.50, 2.30, 2.35, 22, 14, 1.50, 1.45, 1.60, 25, 6),
        'power_fly': (2.20, 2.25, 3.00, 56, 9, 1.30, 1.25, 1.85, 72, 4),
    }
    for sign, side in ((-1, 'left'), (1, 'right')):
        roots, tips = {}, {}
        for key, (w, h, length, pitch, yaw, tw, th, tl, tp, ty) in rear.items():
            roots[key], tips[key] = chain([sign * 2.20, -3.75, 1.80], length, w, h,
                                          [pitch, sign * yaw, 0], tl, tw, th,
                                          [tp, sign * ty, 0], overlap=.76)
        result.append(feature(template, 'run_rear_' + side + '_root', lattice(roots), 'rear_root'))
        mesh = feature(template, 'run_rear_' + side + '_tip', lattice(tips), 'rear_tip')
        mesh['faces'][1]['uv'] = [TIP_UV[:] for _ in range(4)]
        result.append(mesh)

    # The two front nubs are independent of the large rear points. Their
    # root ends stay inside the upper front sides; no mesh crosses the eyes.
    # They acquire their hand-colored pigment from a canonical opaque texel.
    nubs = {
        'young': (.70, .85, 1.00, 7, 48),
        'balanced': (.70, 1.35, 1.00, 12, 24),
        'run': (.85, 1.85, 1.10, 12, 18),
        'power': (.95, 2.20, 1.20, 24, 34),
        'fly': (.65, 2.00, .95, 13, 13),
        'swim': (.85, .95, 1.20, 8, 48),
        'run_swim': (.80, 1.35, 1.10, 11, 30),
        'run_fly': (.70, 1.70, 1.00, 13, 17),
        'power_swim': (.90, 1.35, 1.15, 15, 36),
        'power_fly': (.80, 1.85, 1.05, 19, 23),
    }
    for sign, side in ((-1, 'left'), (1, 'right')):
        profiles = {}
        for key, (w, length, depth, pitch, outward) in nubs.items():
            rotation = [pitch, 0, sign * outward]
            anchor = [sign * 3.0, -4.05 if key == 'young' else -4.80, -1.70]
            profiles[key] = pose([a + b for a, b in zip(anchor, rotate([0, -length / 2, 0], rotation))],
                                 [w, length, depth], rotation)
        result.append(feature(template, 'run_front_nub_' + side, lattice(profiles), 'front_nub', NUB_UV))
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
            for key in NAMES:
                run, power = int(key.startswith('run')), int(key.startswith('power'))
                swim, fly = int('swim' in key), int('fly' in key)
                p = copy.deepcopy(base)
                if key == 'young':
                    scale = [1, 1, 1]
                elif name.startswith('body'):
                    scale = [1.025 + .075 * power + .025 * swim, 1.015 + .02 * fly,
                             1.025 + .025 * power + .025 * swim]
                elif name.startswith('arm'):
                    scale = [1.025 + .10 * power, 1.025 + .09 * power,
                             1.025 + .075 * power]
                else:
                    scale = [1.025 + .045 * power + .025 * swim, 1 + .025 * power,
                             1.045 + .13 * run + .025 * power]
                p['size'] = [a * b for a, b in zip(base['size'], scale)]
                if name.startswith('body'):
                    p['center'][1] -= .225
                    p['size'][1] += .45
                if name.startswith('foot'):
                    p['center'][1] = max(v[1] for v in mesh['vertices']) - p['size'][1] / 2
                    p['center'][2] = max(v[2] for v in mesh['vertices']) - p['size'][2] / 2
                profiles[key] = p
            poses = lattice(profiles)
            mesh['vertices'] = vertices(mesh, poses[0])
            targets(mesh, poses, 'body' if name.startswith('body') else
                    'hand' if name.startswith('arm') else 'foot')


def wings(data, template):
    # First-Run wings remain small in the actual viewer, even at full Fly.
    # Two closed lobes per existing wing joint are sufficient. All four
    # proximal face midpoints remain exactly local [0,.25,0].
    profiles = {
        'young': (1.85, 1.45, .90),
        'balanced': (2.05, 1.50, 1.05),
        'run': (2.65, 2.00, 1.30),
        'power': (1.80, 1.35, 1.10),
        'fly': (2.45, .90, 1.00),
        'swim': (2.60, 1.95, .85),
        'run_swim': (2.60, 2.00, 1.10),
        'run_fly': (2.45, 1.85, 1.15),
        'power_swim': (2.15, 1.60, .95),
        'power_fly': (2.20, 1.65, 1.05),
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
                # Fixed lobe angles make the independently interpolated
                # centers/sizes preserve an EXACT proximal hinge. Relative
                # lobe spans provide upward-vs-flat emphasis across forms.
                angle = -28 if index == 0 else 18
                rotation = [0, 0, sign * angle]
                offset = rotate([sign * span / 2, 0, 0], rotation)
                values[key] = pose([offset[0], .25 + offset[1], 0],
                                   [span, height * (1 - .16 * index), thickness], rotation)
            name = 'run_wing_' + side + '_' + part
            mesh = feature(template, name, lattice(values), 'wing_' + part, FIN_UV)
            distal = {0, 3, 4, 7} if sign > 0 else {1, 2, 5, 6}
            for face in mesh['faces']:
                face['uv'] = [(UNDER_UV if i in distal else FIN_UV)[:] for i in face['v']]
            parts.append(mesh)
            names.append(name)
        bone['meshes'] = parts
    return names


def tail(data, template):
    bone = next(b for b in data['bones'] if b['name'] == 'tail__cube_0')
    # Tail root overlaps the retained original tail base instead of stopping
    # just beyond its edge. Swim gains a broad upturned paddle; other forms
    # retain a compact pointed two-section tail.
    profiles = {
        'young': (.70, .65, 1.65, -20, .50, .60, 1.00, -5),
        'balanced': (.75, .70, 1.75, -17, .55, .65, 1.05, 0),
        'run': (.85, .75, 2.10, -24, .60, .65, 1.35, 8),
        'power': (1.00, .85, 2.05, -10, .75, .70, 1.25, 9),
        'fly': (.85, .70, 2.10, -22, .60, .65, 1.15, 9),
        'swim': (1.65, 1.00, 3.65, -32, 1.20, .80, 1.60, 12),
        'run_swim': (1.25, .85, 2.95, -29, .90, .75, 1.45, 9),
        'run_fly': (.85, .75, 2.10, -22, .60, .65, 1.20, 9),
        'power_swim': (1.30, .90, 2.70, -25, .95, .75, 1.40, 11),
        'power_fly': (.95, .80, 2.10, -18, .70, .70, 1.25, 10),
    }
    roots, tips = {}, {}
    for key, (w, h, length, pitch, tw, th, tl, tp) in profiles.items():
        roots[key], tips[key] = chain([0, -.10, 1.65], length, w, h, [pitch, 0, 0],
                                      tl, tw, th, [tp, 0, 0], overlap=.50)
    bone['meshes'] = [feature(template, 'run_tail_root', lattice(roots), 'tail_root'),
                      feature(template, 'run_tail_tip', lattice(tips), 'tail_tip', TAIL_UV)]


def author():
    source = MODELS / 'chao_baby.json'
    data = json.loads(source.read_text())
    original = copy.deepcopy(data)
    bones = {b['name']: b for b in data['bones']}
    template = bones['skull__cube_0']['meshes'][0]
    bones['gradual_head_021']['meshes'] = head_features(template)
    limbs(data)
    wing_names = wings(data, template)
    tail(data, template)
    data['authored_gameplay_form'] = 'neutral_run_proto062'
    data['evolution_revision'] = '0.25.62'
    data['gradual_evolution'] = {'strength_range': [0, 1.2], 'vertices': 'fresh adult target0',
        'targets': 'fresh +9 fully signed mature closed cuboid parameter profiles',
        'axis_order': 'Swim/Fly then Run/Power', 'head_family': 'neutral/run',
        'reference_page': 'https://chao-island.com/viewer/'}
    data['neutral_run_path_062'] = {
        'targets': TARGETS, 'canonical_uv_layout': 'baby', 'original_head_box': [7, 6, 6],
        'weights': 'fresh=1-maturity; mature age*triangle(clamp(runPower,-1,1),x)*triangle(clamp(swimFly,-1,1),y), x/y=-1,0,1',
        'feature_parameter_order': 'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv': BODY_UV, 'feature_tip_uv': TIP_UV, 'feature_nub_uv': NUB_UV, 'feature_tail_uv': TAIL_UV,
        'feature_fin_uv': FIN_UV, 'feature_underside_uv': UNDER_UV,
        'central_stripe_tile_uv_min': STRIPE_UV_MIN, 'central_stripe_tile_uv_max': STRIPE_UV_MAX,
        'central_stripe_tile_native_inclusive': [112, 32, 118, 33],
        'central_stripe_policy': 'User-directed top-head/central-spine placement, not exact viewer forehead placement. Only central root and tip face4(local-negativeY) use the verified-unused7x2 tile; U follows localX width and V localZ length. Three marked columns alternate with four body columns; sides stay body127, positive-Z tip cap126. Original skull/face/eyes/orb UVs and all geometry are unchanged.',
        'reserved_pixels': {'feature_body': [127, 63], 'feature_tip': [126, 63],
                            'feature_fin': [125, 63], 'feature_underside': [124, 63]},
        'scope': 'Ordinary living Neutral adult with locked Run FIRST evolution. Balanced plus Run, Power, Swim, Fly and four unique compatible pairs; full signed opposing training may recede through balance and develop the opposite specialization without changing locked first type.',
        'reference': 'Fresh independent live Chao Island actual first-Run checkbox and Neutral alignment, all nine mature front/side/back silhouettes and young/intermediate comparisons. Official Run model/texture inspected only for blocky construction vocabulary.',
        'source_geometry_copied': False,
        'approximation': 'Original skull and pixel face exact. Two central crest cuboids, two per mirrored rear spike and two independent hand-colored front nubs replace smooth tapered surfaces. Run hooks back/down; Fly forms three tall nearly vertical prongs; Power rises up/back with different proportions; Swim compresses rear lobes and enlarges an upturned paddle tail. Four small closed wing lobes remain on original joints. Mixed full-axis viewer endpoints have moderated observed silhouettes; authored bilinear corners capture those endpoints, while the interior timing remains an approximation.',
        'wing_policy': 'Two closed cuboid lobes per original wing; fixed directions-28/+18deg and varying relative spans preserve exact proximal-face midpoints local[0,.25,0] during parameter interpolation. Distinct positive thicknesses .26/.19 avoid coincident planes. Four long faces per lobe interpolate UV125(root) to124(distal), providing a visible broad band without added faces.',
        'body_contact_policy': 'Torso top-only+.45 at target0 supports scoped .35pixel walking head lift. All59joint/rest transforms and original head are unchanged; hands stay compact.',
        'tail_contact_policy': 'Root anchor local[0,-.10,1.65] intersects the retained original tail base; two authored sections overlap .50 along their local axis.',
        'front_nub_policy': 'Fresh anchorY-4.05 and every mature anchorY-4.80, same X±3.0/Z-1.70. Original full square head would otherwise conceal mature fin tips. Lower nub sections remain positively embedded in the skull; only age-blended features move, never skull/face/attachments.',
        'marker_policy': 'Original unspiked orb, marker and expression bones/meshes exact; no added marker geometry.',
        'provisional_rates': '960 current level-bar progress per opposing axis and independent adult visual maturity; original-game pacing is not inferred from viewer controls.'}
    actual = {b['name']: b for b in data['bones']}
    assert len(actual) == len(original['bones']) == 59
    for bone in original['bones']:
        name = bone['name']
        for field in ('pivot', 'rotation', 'parent'):
            assert actual[name].get(field) == bone.get(field), (name, field)
        if name.startswith(('skull', 'expression_', 'emotion_')):
            assert actual[name] == bone, name
    OUT.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    refs = ['art/imported_chao/models/chao_run.geo.json', 'art/imported_chao/textures/run.png',
            'src/main/resources/assets/chao_garden/models/entity/official/chao_run.json']
    head_names = [m['name'] for m in bones['gradual_head_021']['meshes']]
    manifest = {'version': '0.25.62', 'model': str(OUT.relative_to(ASSETS)), 'sha256': sha(OUT),
        'source_model_sha256': sha(source), 'target_names': TARGETS,
        'canonical_head_face_orb_and_joint_pivots_unchanged': True,
        'shared_animation_attachment_rig': True, 'source_geometry_copied': False,
        'construction_reference_sha256': {name: sha(ROOT / name) for name in refs},
        'legal_specializations': ['balanced', 'run', 'power', 'fly', 'swim', 'run_fly', 'run_swim', 'power_fly', 'power_swim'],
        'closed_box_count': 19, 'head_feature_count': 8, 'wing_ray_count': 4, 'tail_part_count': 2,
        'head_feature_names': head_names, 'wing_mesh_names': wing_names,
        'tail_mesh_names': ['run_tail_root', 'run_tail_tip'],
        'head_feature_roles': {'central': head_names[:2], 'rear': head_names[2:6], 'front_nubs': head_names[6:]},
        'central_stripe_mesh_names': head_names[:2],
        'central_stripe_tile_native_inclusive': [112, 32, 118, 33],
        'central_stripe_top_face': 4, 'central_stripe_direction': 'localX width / localZ proximal-to-distal length',
        'stripe_placement_source': 'Explicit user preference: original headTOP and central spine/tip; viewer forehead/side markings deliberately relocated.',
        'effective_viewer_pair_controls': 'Measured original control-handle pixels confirm full endpoints on both compatible axes. Mixed silhouettes moderate rather than simply stacking both single-axis maximum forms. Bilinear interior is an authored approximation.'}
    (ASSETS / 'neutral_run062.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('NeutralRun062 generated: exact original 7x6x6 head/face/orb/59joints; '
          '8 head features,5 compactbody/limbs,4 small opaque anchored winglobes,2 tailparts; '
          '19 closedboxes,10 targets/9 mature forms with full signed opposing axes.')


if __name__ == '__main__':
    author()
