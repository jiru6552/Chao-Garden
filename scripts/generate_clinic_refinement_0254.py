#!/usr/bin/env python3
"""Deterministic clinic postpass; run after generate_kindergarten_assets_025.py.

Beds are a real union of nonoverlapping frame/bedding solids. Only exposed
surface patches are emitted, so corner posts cannot share a coplanar skin with
green rails. The east curtain tie has an authored reverse-side variant, keeping
its tassel on the room-facing side when the right panel is turned around.
"""
from copy import deepcopy
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
AS = ROOT / 'src/main/resources/assets/chao_garden'
MODEL = AS / 'models/block'
TEX = AS / 'textures/block/school025'
FACES = {'west': (0, -1, 2, 1), 'east': (0, 1, 2, 1),
         'down': (1, -1, 0, 2), 'up': (1, 1, 0, 2),
         'north': (2, -1, 0, 1), 'south': (2, 1, 0, 1)}


def write(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n')


def exterior(parts):
    """Cut each cuboid face around touching solids; retain physical UV scale."""
    result = []
    for index, (lo, hi, material) in enumerate(parts):
        for face, (axis, sign, uaxis, vaxis) in FACES.items():
            plane = hi[axis] if sign > 0 else lo[axis]
            blockers = []
            for other, (a, b, _) in enumerate(parts):
                if other == index:
                    continue
                covers = a[axis] <= plane < b[axis] if sign > 0 else a[axis] < plane <= b[axis]
                if covers and min(hi[uaxis], b[uaxis]) > max(lo[uaxis], a[uaxis]) and min(hi[vaxis], b[vaxis]) > max(lo[vaxis], a[vaxis]):
                    blockers.append((a, b))
            us = sorted({lo[uaxis], hi[uaxis]} | {max(lo[uaxis], min(hi[uaxis], p[uaxis])) for pair in blockers for p in pair})
            vs = sorted({lo[vaxis], hi[vaxis]} | {max(lo[vaxis], min(hi[vaxis], p[vaxis])) for pair in blockers for p in pair})
            for u0, u1 in zip(us, us[1:]):
                for v0, v1 in zip(vs, vs[1:]):
                    um, vm = (u0 + u1) / 2, (v0 + v1) / 2
                    if any(a[uaxis] < um < b[uaxis] and a[vaxis] < vm < b[vaxis] for a, b in blockers):
                        continue
                    a, b = list(lo), list(hi)
                    a[uaxis], b[uaxis], a[vaxis], b[vaxis] = u0, u1, v0, v1
                    uv = [(u0 - lo[uaxis]) / (hi[uaxis] - lo[uaxis]) * 16,
                          (hi[vaxis] - v1) / (hi[vaxis] - lo[vaxis]) * 16,
                          (u1 - lo[uaxis]) / (hi[uaxis] - lo[uaxis]) * 16,
                          (hi[vaxis] - v0) / (hi[vaxis] - lo[vaxis]) * 16]
                    # A quilt is patterned on its top only, using the full14x23
                    # cloth exactly once. All exposed vertical edges are pink.
                    tex = '#quilt' if material == 'quilt' and face == 'up' else '#pink' if material == 'quilt' else '#' + material
                    result.append({'from': a, 'to': b, 'faces': {face: {'texture': tex, 'uv': uv}}})
    return result


def bed():
    path = MODEL / 'school_white_exam_bed.json'
    doc = json.loads(path.read_text())
    # Posts, rails, pillow and quilt meet at exact boundaries rather than
    # occupying the same corner volumes. Bounds retain the native collision.
    parts = [([2, 4, 0], [14, 8, 32], 'green'),
             ([0, 4, 2], [2, 8, 30], 'green'),
             ([14, 4, 2], [16, 8, 30], 'green'),
             ([1, 8, 7], [15, 10, 30], 'quilt'),
             ([2, 8, 2], [14, 11, 7], 'white')]
    for x in (0, 14):
        for z in (0, 30):
            parts.append(([x, 0, z], [x + 2, 15, z + 2], 'white'))
    for z in (0, 30):
        parts.append(([2, 13, z], [14, 15, z + 2], 'white'))
    doc['elements'] = exterior(parts)
    write(path, doc)
    # The shortened quilt no longer intersects the raised pillow. Its physical
    #14x23 aspect keeps the embroidery square instead of stretching the grid.
    im = Image.new('RGBA', (224, 368), '#ed5a9b')
    draw = ImageDraw.Draw(im)
    for y in range(0, 368, 16):
        draw.line((0, y, 223, y), fill='#ce3d7f', width=2)
    for x in range(0, 224, 16):
        draw.line((x, 0, x, 367), fill='#f481b1', width=2)
    im.save(TEX / 'pink_quilt_top.png', optimize=True)


def mirrored_tie():
    source = 'school_green_room_curtain_tie'
    doc = json.loads((MODEL / (source + '.json')).read_text())
    mirrored = deepcopy(doc)
    for element in mirrored['elements']:
        lo, hi = element['from'][2], element['to'][2]
        element['from'][2], element['to'][2] = 16 - hi, 16 - lo
        faces = element['faces']
        # Reflect both physical surfaces and their normals. A plain270-degree
        # turn alone would bury the dangling gold end behind the curtain cloth.
        faces['north'], faces['south'] = faces['south'], faces['north']
    variant = source + '_east'
    write(MODEL / (variant + '.json'), mirrored)
    path = AS / 'blockstates' / (source + '.json')
    states = json.loads(path.read_text())
    states['variants']['facing=east'] = {'model': 'chao_garden:block/' + variant, 'y': 270}
    write(path, states)


def main():
    bed()
    mirrored_tie()
    print('Clinic0254: exposed bed union, quilt aspect, room-facing mirrored right curtain tie generated.')


if __name__ == '__main__':
    main()
