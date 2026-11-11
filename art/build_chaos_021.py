#!/usr/bin/env python3
"""Refine only the three Chaos head silhouettes on the established native rig.

Run after build_chaos_020.py. The older builder provides the body, protected
eyes, animation anchors and material union algorithm. This finalizer changes
head appendages only; inherited coats and raw supplied assets remain read-only.
"""
from pathlib import Path
import hashlib
import json
import build_chaos_020 as base

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / 'src/main/resources/assets/chao_garden/models/entity/gameplay'


def replace_feature(data, glass, name, boxes, axis, cut, direction=1):
    opaque, ice = base.surface_union(boxes, name, base.MAIN, axis, cut,
                                     direction, head=True)
    by_name = {b['name']: b for b in data['bones']}
    glass_names = {b['name']: b for b in glass['bones']}
    assert name in by_name and name in glass_names
    by_name[name]['meshes'] = opaque
    glass_names[name]['meshes'] = ice


def refine(form):
    data, glass = base.build(form)
    if form == 'light':
        # Central side locks attach above the cheek, then descend beside the
        # chin. Their lower sections stay behind the resting shoulder/hand
        # plane, rather than flaring sideways from the back of the head.
        for side in (-1, 1):
            locks = base.mirrored([
                ([2.65, -4.25, -.55], [3.65, -.30, 1.55]),
                ([3.10, -1.60, .80], [3.95, .55, 1.95]),
                ([3.25, .25, 1.10], [3.80, 1.20, 2.10]),
                ([3.40, .95, 1.35], [3.70, 1.55, 2.20]),
            ], side)
            replace_feature(data, glass, 'chaos_crown_lobe_' + str(side),
                            locks, 1, -.75)
        rear = [
            ([-1.65, -4.70, 2.40], [1.65, -1.30, 3.30]),
            ([-1.10, -2.15, 3.05], [1.10, .35, 3.80]),
            ([-.60, -.10, 3.50], [.60, 1.20, 4.05]),
            ([-.30, .95, 3.75], [.30, 1.85, 4.15]),
        ]
        replace_feature(data, glass, 'chaos_crown_rear', rear, 1, -.10)
    elif form == 'angel':
        # Broader wavy feelers, still built from connected rectangular tiers.
        # The tips descend to the cheek instead of ending beside the forehead.
        for side in (-1, 1):
            feeler = base.mirrored([
                ([1.25, -6.35, -.55], [2.80, -4.80, 1.90]),
                ([1.80, -7.35, -.05], [3.10, -5.80, 2.10]),
                ([2.60, -7.75, .40], [3.70, -6.10, 2.30]),
                ([3.35, -6.80, .75], [4.45, -4.65, 2.60]),
                ([3.95, -5.45, 1.05], [4.95, -2.65, 3.00]),
                ([4.45, -3.30, 1.55], [5.35, -.25, 3.25]),
            ], side)
            replace_feature(data, glass, 'chaos_crown_horn_' + str(side),
                            feeler, 0, side * 3.45, side)
    elif form == 'devil':
        for side in (-1, 1):
            # Raised horns bow out before their narrow hooked ends turn inward.
            upper = base.mirrored([
                ([2.00, -6.10, -.70], [3.40, -4.80, 1.60]),
                ([2.80, -7.15, -.35], [4.15, -5.60, 1.75]),
                ([3.65, -8.35, .05], [4.65, -6.50, 1.85]),
                ([3.60, -9.40, .50], [4.30, -7.70, 1.90]),
                ([3.55, -9.70, .90], [3.95, -8.85, 1.95]),
            ], side)
            replace_feature(data, glass, 'chaos_crown_upper_' + str(side),
                            upper, 1, -6.50, -1)
            lower = base.mirrored([
                ([2.50, -2.30, -.10], [3.70, -.20, 2.10]),
                ([3.25, -1.25, .80], [4.45, .75, 2.80]),
                ([3.95, .35, 1.00], [4.85, 1.65, 3.20]),
                ([4.15, 1.35, 1.50], [4.60, 2.25, 3.25]),
            ], side)
            replace_feature(data, glass, 'chaos_crown_lower_' + str(side),
                            lower, 1, -.35)
    else:
        raise ValueError(form)
    data['authored_metadata']['version'] = '0.21.0'
    core_vertices = [v for b in data['bones'] if b['name'] == 'skull__cube_0'
                     for m in b['meshes'] for v in m['vertices']]
    data['authored_metadata']['head_core_bounds'] = [
        [min(v[i] for v in core_vertices) for i in range(3)],
        [max(v[i] for v in core_vertices) for i in range(3)],
    ]
    data['authored_metadata']['head_refinement'] = {
        'light': 'central descending side locks and downturned rear lock',
        'angel': 'broader wavy feelers descending to cheek level',
        'devil': 'four pronounced hooked upper and descending lower horns',
    }[form]
    data['authored_metadata']['glass_vertex_count'] = sum(
        len(m['vertices']) for b in glass['bones'] for m in b['meshes'])
    glass['authored_metadata'] = dict(data['authored_metadata'])
    base.write(MODELS / f'chao_chaos_{form}.json', data)
    base.write(MODELS / f'chao_chaos_{form}_glass.json', glass)
    return data, glass


def fingerprints(path, pattern='*'):
    return {p.relative_to(path).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in path.rglob(pattern) if p.is_file()}


def main():
    raw = ROOT / 'art/imported_chao'
    coats = ROOT / 'src/main/resources/assets/chao_garden/textures/entity/chao/gameplay/chaos'
    protected, colors = fingerprints(raw), fingerprints(coats, '*.png')
    assert len(protected) == 41 and len(colors) == 351
    report = {'version': '0.21.0', 'families': {}}
    for form in base.FAMILIES:
        data, glass = refine(form)
        report['families'][form] = base.validate(data, glass)
    assert protected == fingerprints(raw)
    assert colors == fingerprints(coats, '*.png')
    report['raw_source_files_preserved'] = len(protected)
    report['inherited_coat_files_preserved'] = len(colors)
    base.write(ROOT / 'docs/research/CHAOS_021_CHECKS.json', report)
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
