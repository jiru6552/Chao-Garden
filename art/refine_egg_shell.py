#!/usr/bin/env python3
"""Finish the placed egg's longitude UVs and matching wrap-edge texels.

Run after the legacy shell/coat generator.  Chao body textures can use an
independent renderer; the egg retains its own 64px shell atlases and editable
Blockbench project.  No Chao egg model/animation occurs in the supplied Sonic
world pack, so this fixes the existing authored egg instead.
"""
from __future__ import annotations

import base64
import copy
import io
import json
import math
import os
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'


def write_bytes(path: Path, payload: bytes) -> None:
    temporary = path.with_name(path.name + '.writing')
    temporary.write_bytes(payload)
    os.replace(temporary, path)


def wrap_longitude_edges(image: Image.Image) -> Image.Image:
    """Equal edge texels give the clamp-sampled longitude cut identical color.

    Only the final column is a wrap gutter.  The interior artwork remains
    unchanged, including the borderless End fallback starfield.
    """
    image = image.convert('RGBA')
    for y in range(image.height):
        image.putpixel((image.width - 1, y), image.getpixel((0, y)))
    return image


def unwrap_shell_uvs(data: dict) -> dict:
    data = copy.deepcopy(data)
    width, height = data['texture_width'], data['texture_height']
    for bone in data['bones']:
        for mesh in bone.get('meshes', []):
            vertices = mesh['vertices']
            top = min(v[1] for v in vertices)
            bottom = max(v[1] for v in vertices)
            span = bottom - top
            if span <= 0:
                raise ValueError('Egg shell has no height')
            for face in mesh['faces']:
                points = [vertices[i] for i in face['v']]
                vertical = [(bottom - p[1]) / span for p in points]
                # Flat polar caps are one texel color, avoiding the unrelated
                # fan diagonals through an entire longitude row.
                if max(vertical) - min(vertical) < 1e-7:
                    v = .5 + vertical[0] * (height - 1)
                    face['uv'] = [[width / 2, round(v, 5)] for _ in points]
                    continue
                longitude = [(math.atan2(p[2], p[0]) / math.tau) % 1 for p in points]
                # A face crossing zero must use the adjacent U=1 endpoint,
                # never interpolate back across almost the entire atlas.
                if max(longitude) - min(longitude) > .5:
                    longitude = [u + 1 if u < .5 else u for u in longitude]
                face['uv'] = [
                    [round(.5 + u * (width - 1), 5),
                     round(.5 + v * (height - 1), 5)]
                    for u, v in zip(longitude, vertical)
                ]
    return data


def main() -> None:
    runtime = ASSETS / 'models/entity/egg_mesh.json'
    old = json.loads(runtime.read_text())
    data = unwrap_shell_uvs(old)
    write_bytes(runtime, (json.dumps(data, separators=(',', ':')) + '\n').encode())
    atlases = sorted((ASSETS / 'textures/entity/eggs').glob('*.png'))
    changed = 0
    for path in atlases:
        with Image.open(path) as source:
            old_image = source.convert('RGBA')
            image = wrap_longitude_edges(old_image.copy())
        if image.tobytes() == old_image.tobytes():
            continue
        payload = io.BytesIO()
        image.save(payload, format='PNG', optimize=True)
        write_bytes(path, payload.getvalue())
        changed += 1

    project_path = ROOT / 'art/blockbench/chao_egg.bbmodel'
    project = json.loads(project_path.read_text())
    meshes = [element for element in project['elements'] if element.get('type') == 'mesh']
    source_meshes = [m for b in data['bones'] for m in b.get('meshes', [])]
    if len(meshes) != len(source_meshes):
        raise ValueError('Editable/runtime egg mesh counts differ')
    for editable, mesh in zip(meshes, source_meshes):
        if len(editable['faces']) != len(mesh['faces']):
            raise ValueError('Editable/runtime egg face counts differ')
        for face, fixed in zip(editable['faces'].values(), mesh['faces']):
            face['uv'] = {'v' + str(i): uv for i, uv in zip(fixed['v'], fixed['uv'])}
    reference = ASSETS / 'textures/entity/eggs/normal_neutral_0_two.png'
    project['textures'][0]['source'] = 'data:image/png;base64,' + base64.b64encode(reference.read_bytes()).decode()
    write_bytes(project_path, json.dumps(project, separators=(',', ':')).encode())
    print(f'Egg shell UVs unwrapped; {len(atlases)} independent atlases checked, {changed} wrap gutters completed; editable egg synchronized.')


if __name__ == '__main__':
    main()
