#!/usr/bin/env python3
"""Build only the Chao ball's native cuboid models and panel texture.

The cyan/yellow panel palette is an adaptation of the Chao Island SA2 prize
icon. The six-pixel size, stepped geometry and texture mapping are authored
Minecraft choices, not measurements or a copy of an original game mesh.

Run from the complete project: python3 art/build_ball_074.py
Use --check to verify shipped bytes without changing them.
"""
from argparse import ArgumentParser
from io import BytesIO
from itertools import product
from pathlib import Path
import hashlib
import json
import math

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/chao_garden"
STEP = .75
CENTER = (8, 3, 8)
BOUNDS = (5, 0, 5, 11, 6, 11)
TEXTURE = "chao_garden:block/ball074/chao_ball_panels"
IDLE = "chao_garden:block/chao_ball_idle"
EMPTY = "chao_garden:block/chao_ball_active_empty"
SIDES = ("north", "south", "east", "west", "up", "down")
COLORS = {
    "cyan": (25, 201, 205, 255),
    "cyan_light": (47, 216, 217, 255),
    "yellow": (241, 213, 38, 255),
    "yellow_light": (255, 230, 66, 255),
    "seam": (211, 242, 218, 255),
    "cap": (240, 247, 222, 255),
}


def json_bytes(value):
    return (json.dumps(value, indent=2) + "\n").encode()


def voxels():
    # An isotropic lattice avoids a barrel silhouette and needs no rotated or
    # paper-thin elements. Greedy merging changes only the internal partition.
    return {(x, y, z) for x, y, z in product(range(8), repeat=3)
            if sum(((v - 3.5) * STEP) ** 2 for v in (x, y, z)) <= 2.85 ** 2}


def cuboids():
    left = voxels()
    boxes = []
    for y, z, x in product(range(8), repeat=3):
        if (x, y, z) not in left:
            continue
        xe = x + 1
        while (xe, y, z) in left:
            xe += 1
        ze = z + 1
        while ze < 8 and all((xx, y, ze) in left for xx in range(x, xe)):
            ze += 1
        ye = y + 1
        while ye < 8 and all((xx, ye, zz) in left
                             for xx in range(x, xe) for zz in range(z, ze)):
            ye += 1
        boxes.append([5 + x * STEP, y * STEP, 5 + z * STEP,
                      5 + xe * STEP, ye * STEP, 5 + ze * STEP])
        for xx, yy, zz in product(range(x, xe), range(y, ye), range(z, ze)):
            left.remove((xx, yy, zz))
    assert not left
    return boxes


def point_on_face(box, side, u, v):
    """Match vanilla FaceBakery UV orientation for a closed, unrotated cuboid."""
    x0, y0, z0, x1, y1, z1 = box
    if side == "north":
        return x1 + (x0 - x1) * u, y1 + (y0 - y1) * v, z0
    if side == "south":
        return x0 + (x1 - x0) * u, y1 + (y0 - y1) * v, z1
    if side == "west":
        return x0, y1 + (y0 - y1) * v, z0 + (z1 - z0) * u
    if side == "east":
        return x1, y1 + (y0 - y1) * v, z1 + (z0 - z1) * u
    if side == "up":
        return x0 + (x1 - x0) * u, y1, z0 + (z1 - z0) * v
    return x0 + (x1 - x0) * u, y0, z1 + (z0 - z1) * v


def panel_color(point):
    x, y, z = (point[a] - CENTER[a] for a in range(3))
    rho = math.hypot(x, z)
    if abs(y) >= 2.98 and rho <= 1.15:
        return COLORS["cap"]
    # Three cyan and three yellow panels curve toward the pale pole caps.
    phase = 3 * (math.atan2(z, x) + .32 * y / 3)
    if abs(y) > 2.1 and rho < 1.25 and abs(math.sin(phase)) < .12:
        return COLORS["seam"]
    color = "cyan" if math.sin(phase) >= 0 else "yellow"
    return COLORS[color + ("_light" if y > .2 else "")]


def geometry_and_texture():
    boxes = cuboids()
    tile = 16
    columns = 16
    rows = 1 << math.ceil(math.log2(math.ceil(len(boxes) * 6 / columns)))
    width, height = columns * tile, rows * tile
    atlas = Image.new("RGBA", (width, height), COLORS["cyan"])
    elements = []
    for index, box in enumerate(boxes):
        faces = {}
        for face, side in enumerate(SIDES):
            slot = index * 6 + face
            tx, ty = slot % columns * tile, slot // columns * tile
            # Twelve actual texels and a two-texel repeated border keep mipmaps
            # from sampling an unrelated face's panel patch.
            for py, px in product(range(tile), repeat=2):
                u = min(1, max(0, (px - 2 + .5) / 12))
                v = min(1, max(0, (py - 2 + .5) / 12))
                atlas.putpixel((tx + px, ty + py),
                               panel_color(point_on_face(box, side, u, v)))
            faces[side] = {"texture": "#panels",
                           "uv": [(tx + 2) * 16 / width, (ty + 2) * 16 / height,
                                  (tx + 14) * 16 / width, (ty + 14) * 16 / height]}
        elements.append({"from": box[:3], "to": box[3:], "faces": faces})
    return elements, atlas


def generated():
    elements, atlas = geometry_and_texture()
    base = {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"panels": TEXTURE, "particle": TEXTURE}, "elements": elements}
    output = {"models/block/chao_ball_idle.json": json_bytes(base)}
    # Kept for source/resource compatibility; animation is one rigid BER model,
    # rather than four unrelated bobbing shapes selected by block frames.
    for frame in range(4):
        output[f"models/block/chao_ball_play_{frame}.json"] = json_bytes({"parent": IDLE})
    output["models/block/chao_ball_active_empty.json"] = json_bytes({
        "parent": "minecraft:block/block", "textures": {"particle": TEXTURE}, "elements": []})
    variants = {}
    for facing, rotation in zip(("north", "east", "south", "west"), (0, 90, 180, 270)):
        for playing, rolling, frame in product((False, True), (False, True), range(4)):
            key = (f"facing={facing},frame={frame},playing={str(playing).lower()},"
                   f"rolling={str(rolling).lower()}")
            variants[key] = {"model": EMPTY if playing or rolling else IDLE, "y": rotation}
    output["blockstates/chao_ball.json"] = json_bytes({"variants": variants})
    # Center the same rigid geometry for item transforms. The world ball stays
    # on y=0; using that low origin in the GUI formerly made it tiny/off-center.
    item_elements = json.loads(json.dumps(elements))
    for element in item_elements:
        element["from"][1] += 5
        element["to"][1] += 5
    item = {"parent": "minecraft:block/block", "gui_light": "front", "ambientocclusion": False,
            "textures": base["textures"], "elements": item_elements, "display": {
                "gui": {"rotation": [27, 225, 0], "translation": [0, 0, 0], "scale": [1.85] * 3},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.9] * 3},
                "thirdperson_lefthand": {"rotation": [75, -45, 0], "translation": [0, 2.5, 0], "scale": [.9] * 3},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0], "scale": [1.05] * 3},
                "firstperson_lefthand": {"rotation": [0, -45, 0], "translation": [0, 1, 0], "scale": [1.05] * 3},
                "ground": {"translation": [0, 2.4, 0], "scale": [.8] * 3},
                "fixed": {"rotation": [0, 180, 0], "scale": [1.3] * 3}}}
    output["models/item/chao_ball.json"] = json_bytes(item)
    data = BytesIO()
    atlas.save(data, format="PNG", compress_level=9)
    output["textures/block/ball074/chao_ball_panels.png"] = data.getvalue()
    return output


def contract():
    boxes = cuboids()
    corners = sorted({
        corner for b in boxes for corner in product((b[0], b[3]), (b[1], b[4]), (b[2], b[5]))})
    return {"scope": "Authored closed cuboid ball geometry; model pixels, not world blocks. No game/render verification inferred.",
            "diameter_model_pixels": 6, "center_model_pixels": CENTER, "bounds_model_pixels": BOUNDS,
            "world_model": IDLE, "active_empty_model": EMPTY, "voxel_step": STEP,
            "cuboids": boxes, "unique_corners": corners,
            "maximum_corner_radius_model_pixels": max(math.dist(p, CENTER) for p in corners),
            "item_origin_shift_model_pixels": [0, 5, 0], "texture": TEXTURE}


def main():
    parser = ArgumentParser(description=__doc__)
    parser.add_argument("--output-root", type=Path, default=ASSETS)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--contract", type=Path)
    args = parser.parse_args()
    output = generated()
    for name, data in output.items():
        path = args.output_root / name
        if args.check:
            if not path.is_file() or path.read_bytes() != data:
                raise SystemExit("Ball asset differs from generator: " + str(path))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    if args.contract:
        args.contract.parent.mkdir(parents=True, exist_ok=True)
        args.contract.write_bytes(json_bytes(contract()))
    print(json.dumps({"mode": "checked" if args.check else "generated", "files": len(output),
                      "cuboids": len(cuboids()), "bounds": BOUNDS,
                      "sha256": {name: hashlib.sha256(data).hexdigest() for name, data in output.items()}}, indent=2))


if __name__ == "__main__":
    main()
