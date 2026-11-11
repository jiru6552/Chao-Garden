#!/usr/bin/env python3
"""Validate egg longitude continuity and reject the original seam regression."""
from pathlib import Path
import copy
import json
import sys
from collections import Counter

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'art'))
from blockbench_to_runtime import export_model


def require(condition, message):
    if not condition:
        raise ValueError(message)


def validate_mesh(data):
    width, height = data['texture_width'], data['texture_height']
    sides = seams = 0
    for bone in data['bones']:
        for mesh in bone.get('meshes', []):
            vertices = mesh['vertices']
            edges = Counter()
            for face in mesh['faces']:
                points = [vertices[i] for i in face['v']]
                for i, a in enumerate(face['v']):
                    edges[tuple(sorted((a, face['v'][(i+1) % len(face['v'])])))] += 1
                for u, v in face['uv']:
                    require(.5-1e-4 <= u <= width-.5+1e-4 and
                            .5-1e-4 <= v <= height-.5+1e-4,
                            'Egg UV reaches outside center-sampled atlas bounds')
                if max(p[1] for p in points) - min(p[1] for p in points) < 1e-6:
                    continue
                sides += 1
                span = max(uv[0] for uv in face['uv']) - min(uv[0] for uv in face['uv'])
                require(span <= width / 8,
                        'Egg longitude panel stretches across the texture seam')
                if any(abs(uv[0] - (width-.5)) < 1e-4 for uv in face['uv']):
                    seams += 1
            require(all(n == 2 for n in edges.values()), 'Egg shell has an open or doubled edge')
    require(sides == 96 and seams == 6, 'Expected six closed sixteen-panel longitude rings')
    return sides, seams


def validate_texture(image):
    require(image.size == (64, 64), 'Unexpected egg atlas resolution')
    require(all(image.getpixel((0, y)) == image.getpixel((63, y)) for y in range(64)),
            'Egg atlas longitude edge texels do not meet')


def main():
    assets = ROOT / 'src/main/resources/assets/chao_garden'
    data = json.loads((assets / 'models/entity/egg_mesh.json').read_text())
    sides, seams = validate_mesh(data)
    atlases = list((assets / 'textures/entity/eggs').glob('*.png'))
    require(bool(atlases), 'Missing egg atlases')
    for path in atlases:
        with Image.open(path) as picture:
            validate_texture(picture.convert('RGBA'))
    project = json.loads((ROOT / 'art/blockbench/chao_egg.bbmodel').read_text())
    exported = export_model(project)
    require(exported == data, 'Editable egg no longer roundtrips to runtime geometry/UVs')

    corrupted = copy.deepcopy(data)
    for face in corrupted['bones'][0]['meshes'][0]['faces']:
        if any(abs(uv[0] - 63.5) < 1e-4 for uv in face['uv']) and len(set(uv[1] for uv in face['uv'])) > 1:
            for uv in face['uv']:
                if abs(uv[0] - 63.5) < 1e-4:
                    uv[0] = .5
            break
    try:
        validate_mesh(corrupted)
    except ValueError:
        pass
    else:
        raise ValueError('Original closing-panel UV regression was not rejected')
    with Image.open(atlases[0]) as picture:
        bad_texture = picture.convert('RGBA')
    pixel = bad_texture.getpixel((0, 0))
    bad_texture.putpixel((63, 0), ((pixel[0]+1) % 256, *pixel[1:]))
    try:
        validate_texture(bad_texture)
    except ValueError:
        pass
    else:
        raise ValueError('Mismatching texture-edge regression was not rejected')
    print(f'Egg shell passed: {sides} narrow longitude panels, {seams} closing panels, '
          f'{len(atlases)} matching wrap edges, exact Blockbench roundtrip; '
          'UV stretch and texture-edge corruptions rejected.')


if __name__ == '__main__':
    main()
