#!/usr/bin/env python3
"""Check the visible school seal, full UVs and every native facing transform."""
from copy import deepcopy
from pathlib import Path
import argparse
import json
import math
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSET = Path('src/main/resources/assets/chao_garden')
CYAN = {(121, 213, 223), (87, 180, 203)}
CHECKS = 0


def require(condition, message):
    global CHECKS
    CHECKS += 1
    if not condition:
        raise AssertionError(message)


def components(points):
    points = set(points)
    result = []
    while points:
        todo = [points.pop()]
        group = set(todo)
        while todo:
            x, y = todo.pop()
            for point in ((x-1, y), (x+1, y), (x, y-1), (x, y+1)):
                if point in points:
                    points.remove(point)
                    group.add(point)
                    todo.append(point)
        result.append(group)
    return sorted(result, key=len, reverse=True)


def bounds(points):
    return (min(x for x, _ in points), min(y for _, y in points),
            max(x for x, _ in points), max(y for _, y in points))


def visible_motif(image):
    require(image.mode == 'RGBA' and image.size == (128, 128),
            'School emblem must preserve its square native RGBA canvas')
    opaque = {(x, y) for y in range(128) for x in range(128)
              if image.getpixel((x, y))[3] == 255}
    require(opaque and bounds(opaque)[0] >= 4 and bounds(opaque)[1] >= 4
            and bounds(opaque)[2] <= 123 and bounds(opaque)[3] <= 123,
            'School emblem touches the image edge and can be cropped')
    require(all(image.getpixel(p)[3] == 0 for p in ((0, 0), (127, 0), (0, 127), (127, 127))),
            'School seal must retain transparent corners')
    # This measures the visible rim all the way around. It rejects the old C
    # artwork, including a right-side opening that an alpha-extrema test missed.
    for angle in range(360):
        theta = math.radians(angle)
        x, y = round(63.5 + 56*math.cos(theta)), round(63.5 + 56*math.sin(theta))
        red, green, blue, alpha = image.getpixel((x, y))
        require(alpha == 255 and red > 145 and green > 100 and blue < 180,
                'School seal has an incomplete gold rim at angle ' + str(angle))
    greens = {(x, y) for x, y in opaque
              if (lambda c: c[1] > c[0]*1.2 and c[1] > c[2]*1.15)(image.getpixel((x, y)))}
    for angle in range(0, 360, 18):
        theta = math.radians(angle)
        cx, cy = 63.5 + 45.5*math.cos(theta), 63.5 + 45.5*math.sin(theta)
        require(any((x-cx)**2+(y-cy)**2 <= 25 for x, y in greens),
                'School laurel has a missing sector at angle ' + str(angle))
    head = components({point for point in opaque if image.getpixel(point)[:3] in CYAN})
    require(len(head) == 2 and len(head[0]) > 1500 and len(head[1]) >= 20,
            'Chao head/orb must be complete, connected and visually separate')
    body_box, orb_box = bounds(head[0]), bounds(head[1])
    require(body_box[0] <= 40 and body_box[2] >= 88 and body_box[1] <= 43 and body_box[3] >= 95,
            'Chao head silhouette is cropped or missing its tuft/rounded base')
    require(58 <= orb_box[0] <= orb_box[2] <= 70 and 24 <= orb_box[1] <= orb_box[3] <= 35,
            'Chao orb must fit above the head inside the laurel')
    require(body_box[1] - orb_box[3] >= 5,
            'Chao orb merges into the head or wreath')


def native_projection(model, states):
    require(model.get('render_type') == 'minecraft:cutout', 'School seal loses its cutout rendering')
    require(model.get('textures', {}).get('base') == 'chao_garden:block/school025/school_emblem',
            'School seal binds another texture')
    elements = model.get('elements', [])
    require(len(elements) == 1 and set(elements[0].get('faces', {})) == {'south'},
            'School seal is no longer a single unobstructed facade face')
    element = elements[0]
    lower, upper = element['from'], element['to']
    require(upper[0]-lower[0] == upper[1]-lower[1] == 48,
            'School seal projection stretches or clips its square canvas')
    require(all(-16 <= value <= 32 for value in lower+upper), 'School seal leaves legal native model bounds')
    face = element['faces']['south']
    require(face.get('texture') == '#base' and face.get('uv') == [0, 0, 16, 16],
            'School seal UVs crop the complete emblem')
    require('cullface' not in face, 'School seal is culled by its facade support')
    variants = states.get('variants', {})
    require(set(variants) == {'facing='+name for name in ('south', 'west', 'north', 'east')},
            'School seal facing state domain is incomplete')
    normals = {'south': (0, 0, 1), 'west': (-1, 0, 0),
               'north': (0, 0, -1), 'east': (1, 0, 0)}
    for facing, expected in normals.items():
        variant = variants['facing='+facing]
        require(variant.get('model') == 'chao_garden:block/school_emblem',
                'School seal facing binds another mesh: ' + facing)
        angle = math.radians(variant.get('y', 0))
        normal = (round(-math.sin(angle)), 0, round(math.cos(angle)))
        require(normal == expected, 'School seal faces the wrong side: ' + facing)
        # The four texture corners survive the native rotation as one square;
        # no state substitutes a cropped model or changes its local UV domain.
        corners = []
        for x, y in ((lower[0], lower[1]), (upper[0], lower[1]),
                     (upper[0], upper[1]), (lower[0], upper[1])):
            dx, dz = x-8, upper[2]-8
            corners.append((8+dx*math.cos(angle)-dz*math.sin(angle), y,
                            8+dx*math.sin(angle)+dz*math.cos(angle)))
        lengths = [sum((a-b)**2 for a, b in zip(corners[i], corners[(i+1) % 4]))
                   for i in range(4)]
        require(all(abs(length-48*48) < 1e-7 for length in lengths),
                'School seal loses a full texture edge after facing rotation: ' + facing)


def rejects(action, label):
    try:
        action()
    except AssertionError:
        return
    raise AssertionError('Malformed school seal accepted: ' + label)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    args = parser.parse_args()
    asset = args.root / ASSET
    image = Image.open(asset / 'textures/block/school025/school_emblem.png').convert('RGBA')
    model = json.loads((asset / 'models/block/school_emblem.json').read_text())
    states = json.loads((asset / 'blockstates/school_emblem.json').read_text())
    visible_motif(image)
    native_projection(model, states)
    broken = image.copy()
    ImageDraw.Draw(broken).pieslice((0, 0, 127, 127), 342, 18, fill=(0, 0, 0, 0))
    rejects(lambda: visible_motif(broken), 'old right-side opening')
    broken = image.copy()
    for y in range(35, 101):
        for x in range(64, 96):
            if broken.getpixel((x, y))[:3] in CYAN:
                broken.putpixel((x, y), (239, 211, 92, 255))
    rejects(lambda: visible_motif(broken), 'cropped Chao head')
    broken = image.copy()
    ImageDraw.Draw(broken).rectangle((63, 30, 64, 44), fill=(121, 213, 223, 255))
    rejects(lambda: visible_motif(broken), 'orb/head merge')
    broken_model = deepcopy(model)
    broken_model['elements'][0]['faces']['south']['uv'][2] = 12
    rejects(lambda: native_projection(broken_model, states), 'UV crop')
    broken_model = deepcopy(model)
    broken_model['elements'][0]['to'][0] = 24
    rejects(lambda: native_projection(broken_model, states), 'stretched projection')
    broken_states = deepcopy(states)
    broken_states['variants']['facing=west']['y'] = 270
    rejects(lambda: native_projection(model, broken_states), 'reversed facing')
    broken_model = deepcopy(model)
    broken_model['elements'][0]['faces']['south']['cullface'] = 'south'
    rejects(lambda: native_projection(broken_model, states), 'support-face culling')
    print(f'School emblem0251: {CHECKS:,} assertions passed; complete gold/green seal, separated full head/orb, all four native facings; 7 malformed fixtures rejected.')


if __name__ == '__main__':
    main()
