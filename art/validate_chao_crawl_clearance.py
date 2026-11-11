"""Bounded crawl hand/head cuboid diagnostic for the 0.17 gameplay controller.

Forty-nine phases at full crawl movement are measured across eleven ordinary
source anatomies and eighteen authored animal-arm sets (fourteen are equip-compatible). This is a targeted
geometry diagnostic, not a guarantee for other motions, look angles, breathing,
reaction/transition blends, noncuboid features or every mixed animal part.
"""
from pathlib import Path
import argparse
from collections import Counter
import json
import math
import numpy as np

from validate_chao_overlay_clearance import model, matrices, points, poses, sat

from runtime_resource_contract_022 import COAT_LAYOUTS
RIGS = COAT_LAYOUTS


def crawl_step(cycle):
    phase = cycle / math.tau % 1
    if phase < .6:
        return (-1.05 + 2.10 * phase / .6, 0)
    recovery = (phase - .6) / .4
    ease = recovery * recovery * (3 - 2 * recovery)
    return (1.05 - 2.10 * ease, math.sin(recovery * math.pi) * .85)


def crawl_pose(phase, movement=1, head_lower=None, key="baby"):
    """Equivalent semantic deltas to crawl(), on the imported t=0 idle base.

    Breathing is fixed at its midpoint and look/reaction/transition deltas are
    absent so the diagnostic isolates the actual crawling limb arrangement.
    """
    if head_lower is None:
        head_lower = .3 if key == 'hero' else .7
    result = poses('animation.chao.idle', 0)
    body = result['body']
    pitch = 1.10
    rock = math.sin(phase) * movement
    bob = (1 - math.cos(phase * 2)) * .045 * movement
    body['position'][1] += .5 + bob
    body['position'][0] += rock * .12
    body['rotation'][0] = pitch
    body['rotation'][1] += rock * .025
    body['rotation'][2] += rock * .045
    head = result['skull']
    head['position'][1] += head_lower - bob * .35
    head['position'][0] += rock * .025
    head['position'][2] -= 3.4
    head['rotation'][0] += .025
    for name in ('arm_left', 'arm_right'):
        side = 1 if name.endswith('left') else -1
        reach, lift = crawl_step(phase + (math.pi if side < 0 else 0))
        reach *= movement
        lift *= movement
        vertical = -lift - bob
        result[name]['position'] = np.array([side * .9,
            math.cos(pitch) * vertical + math.sin(pitch) * reach,
            .7 - math.sin(pitch) * vertical + math.cos(pitch) * reach])
        result[name]['rotation'] = np.array([0, side * pitch,
                                            side * (1.4 - lift * .09)])
    return result


def core_head(official):
    candidates = [b for b in official['bones'] if b['name'].startswith('skull__cube_')]
    def volume(bone):
        vertices = bone['meshes'][0]['vertices']
        return math.prod(max(v[i] for v in vertices) - min(v[i] for v in vertices)
                         for i in range(3))
    return max(candidates, key=volume)


def measure(head_lower=None):
    results = {name: {'checks': 0, 'contacts': []} for name in ('native', 'overlay')}
    for key in RIGS:
        official = json.loads((model/'official'/f'chao_{key}.json').read_text())
        overlay = json.loads((model/'overlays'/f'chao_{key}.json').read_text())
        head = core_head(official)
        hm = head['meshes'][0]
        for phase in np.linspace(0, math.tau, 49):
            pose = crawl_pose(float(phase), head_lower=head_lower, key=key)
            native_matrices = matrices(official, pose)
            overlay_matrices = matrices(overlay, pose)
            hv = points(hm, native_matrices[head['name']])
            for kind, rig, transforms in (('native', official, native_matrices),
                                         ('overlay', overlay, overlay_matrices)):
                result = results[kind]
                for bone in rig['bones']:
                    if kind == 'native':
                        if not bone['name'].startswith(('arm_left__cube_', 'arm_right__cube_')):
                            continue
                    elif not bone['name'].startswith('animal_0_'):
                        continue
                    for mesh in bone['meshes']:
                        if len(mesh['vertices']) != 8:
                            continue
                        depth = sat(hm, hv, mesh, points(mesh, transforms[bone['name']]))
                        result['checks'] += 1
                        if depth > .05:
                            result['contacts'].append({'depth': depth, 'rig': key,
                                'phase_radians': round(float(phase), 6),
                                'bone': bone['name'], 'mesh': mesh['name']})
    for result in results.values():
        contacts = result.pop('contacts')
        result['contacts_over_0_05'] = len(contacts)
        result['max_depth'] = max((value['depth'] for value in contacts), default=0)
        result['mesh_contacts'] = dict(Counter(value['mesh'] for value in contacts))
        result['native_cube_contacts'] = {}
        if any('__cube_' in value['bone'] for value in contacts):
            result['native_cube_contacts'] = dict(Counter(value['bone'].rsplit('__', 1)[-1] for value in contacts))
        result['top_contacts'] = sorted(contacts, key=lambda value: value['depth'], reverse=True)[:12]
    return {'motion': 'crawl', 'head_lower_model_pixels': head_lower if head_lower is not None else {'hero': .3, 'other_rigs': .7}, 'movement_weight': 1, 'rig_count': len(RIGS),
            'samples_per_rig': 49, 'authored_arm_sets': 18, 'equip_compatible_arm_species': 14,
            'depth_units': 'model pixels; 16 per block',
            'scope': 'Full-movement crawl cuboids only; midpoint breathing, zero look/reaction/transition deltas. Not universal animation clearance.',
            **results}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--head-lower', type=float, help='Diagnostic override; default matches the adaptive runtime fit')
    args = parser.parse_args()
    evidence = measure(args.head_lower)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(evidence, indent=2) + '\n')
    print(json.dumps(evidence, indent=2))


if __name__ == '__main__':
    main()
