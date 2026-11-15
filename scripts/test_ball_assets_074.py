#!/usr/bin/env python3
"""Ball mesh, texture, item-fit and renderer binding checks; no client claim.

Run: python3 scripts/test_ball_assets_074.py
"""
from io import BytesIO
from itertools import combinations, permutations, product
from pathlib import Path
import importlib.util
import json
import math
import tempfile
import unittest

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/chao_garden"
spec = importlib.util.spec_from_file_location("ball_assets074", ROOT / "art/build_ball_074.py")
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)


def shipped(name):
    return json.loads((ASSETS / name).read_text())


def bounds(model):
    return [min(e["from"][a] for e in model["elements"]) for a in range(3)] + [
        max(e["to"][a] for e in model["elements"]) for a in range(3)]


class BallAssets074Test(unittest.TestCase):
    def setUp(self):
        self.world = shipped("models/block/chao_ball_idle.json")
        self.item = shipped("models/item/chao_ball.json")

    def test_closed_disjoint_cuboids_have_fixed_six_pixel_grounded_bounds(self):
        self.assertEqual(bounds(self.world), [5, 0, 5, 11, 6, 11])
        self.assertGreater(len(self.world["elements"]), 6)
        self.assertLessEqual(len(self.world["elements"]), 32)
        for element in self.world["elements"]:
            self.assertNotIn("rotation", element)
            self.assertEqual(set(element["faces"]), {"north", "south", "east", "west", "up", "down"})
            for a in range(3):
                self.assertGreater(element["to"][a], element["from"][a])
        for a, b in combinations(self.world["elements"], 2):
            # Touching faces are intentional; overlapping volumes would cause
            # wasted hidden slabs or exposed z-fighting when globally rotated.
            self.assertFalse(all(min(a["to"][i], b["to"][i]) -
                                 max(a["from"][i], b["from"][i]) > 1e-9 for i in range(3)))

    def test_union_is_solid_connected_and_isotropic_in_all_three_axes(self):
        occupied = set()
        for x, y, z in product(range(8), repeat=3):
            point = (5 + (x + .5) * .75, (y + .5) * .75, 5 + (z + .5) * .75)
            hits = sum(all(e["from"][i] < point[i] < e["to"][i] for i in range(3))
                       for e in self.world["elements"])
            self.assertLessEqual(hits, 1)
            if hits:
                occupied.add((x, y, z))
        self.assertEqual(len(occupied), 208)
        for order in permutations(range(3)):
            self.assertEqual({tuple(p[a] for a in order) for p in occupied}, occupied)
        for axis in range(3):
            self.assertEqual({tuple(7 - v if a == axis else v for a, v in enumerate(p))
                              for p in occupied}, occupied)
        reached = {next(iter(occupied))}
        todo = list(reached)
        while todo:
            p = todo.pop()
            for a, delta in product(range(3), (-1, 1)):
                neighbor = tuple(v + delta if i == a else v for i, v in enumerate(p))
                if neighbor in occupied and neighbor not in reached:
                    reached.add(neighbor)
                    todo.append(neighbor)
        self.assertEqual(reached, occupied)
        # Every occupied lattice column/row is contiguous: there is no shell
        # with an exposed cavity when this rigid mesh rolls.
        for axis in range(3):
            for fixed in product(range(8), repeat=2):
                line = sorted(p[axis] for p in occupied
                              if tuple(p[i] for i in range(3) if i != axis) == fixed)
                if line:
                    self.assertEqual(line, list(range(line[0], line[-1] + 1)))

    def test_item_is_the_same_rigid_mesh_with_only_a_centering_origin_shift(self):
        self.assertEqual(len(self.world["elements"]), len(self.item["elements"]))
        self.assertEqual(bounds(self.item), [5, 5, 5, 11, 11, 11])
        self.assertEqual(self.world["textures"], self.item["textures"])
        for world, item in zip(self.world["elements"], self.item["elements"]):
            self.assertEqual(world["faces"], item["faces"])
            for key in ("from", "to"):
                self.assertEqual(item[key], [world[key][0], world[key][1] + 5, world[key][2]])
        display = self.item["display"]
        self.assertEqual(set(display), {"gui", "thirdperson_righthand", "thirdperson_lefthand",
                                       "firstperson_righthand", "firstperson_lefthand", "ground", "fixed"})
        self.assertEqual(display["gui"]["translation"], [0, 0, 0])
        for name, transform in display.items():
            self.assertEqual(len(set(transform["scale"])), 1, name)
            self.assertGreater(transform["scale"][0], 0)
        # A radius bound, independent of Euler order, guarantees the complete
        # GUI sphere fits the sixteen-pixel icon at every authored rotation.
        corners = [p for e in self.item["elements"]
                   for p in product(*[(e["from"][a], e["to"][a]) for a in range(3)])]
        radius = max(math.dist(p, (8, 8, 8)) for p in corners)
        self.assertLess(radius * display["gui"]["scale"][0], 8)
        self.assertGreater(6 * display["gui"]["scale"][0], 10)
        self.assertAlmostEqual((5 - 8) * display["ground"]["scale"][1] +
                               display["ground"]["translation"][1], 0)

    def test_panel_texture_is_opaque_palette_art_with_valid_separate_face_uvs(self):
        texture = Image.open(ASSETS / "textures/block/ball074/chao_ball_panels.png").convert("RGBA")
        self.assertEqual(texture.size, (256, 256))
        self.assertEqual(texture.getchannel("A").getextrema(), (255, 255))
        colors = {color for _, color in texture.getcolors(texture.width * texture.height)}
        expected = set(builder.COLORS.values())
        self.assertLessEqual(colors, expected)
        self.assertTrue(expected <= colors)
        uv_boxes = set()
        patch_colors = set()
        for element in self.world["elements"]:
            for face in element["faces"].values():
                self.assertEqual(face["texture"], "#panels")
                uv = tuple(face["uv"])
                self.assertNotIn(uv, uv_boxes)
                uv_boxes.add(uv)
                self.assertTrue(all(math.isfinite(v) and 0 <= v <= 16 for v in uv))
                self.assertLess(uv[0], uv[2])
                self.assertLess(uv[1], uv[3])
                crop = texture.crop(tuple(round(v * texture.size[i % 2] / 16) for i, v in enumerate(uv)))
                patch_colors |= {color for _, color in crop.getcolors(crop.width * crop.height)}
        self.assertTrue(expected <= patch_colors)

    def test_static_and_active_bindings_draw_exactly_one_ball_without_bobbing(self):
        variants = shipped("blockstates/chao_ball.json")["variants"]
        expected = {f"facing={f},frame={n},playing={str(p).lower()},rolling={str(r).lower()}"
                    for f, n, p, r in product(("north", "east", "south", "west"), range(4),
                                             (False, True), (False, True))}
        self.assertEqual(set(variants), expected)
        for key, value in variants.items():
            fields = dict(x.split("=") for x in key.split(","))
            dynamic = fields["playing"] == "true" or fields["rolling"] == "true"
            self.assertEqual(value["model"], builder.EMPTY if dynamic else builder.IDLE)
            self.assertEqual(value["y"], {"north": 0, "east": 90, "south": 180, "west": 270}[fields["facing"]])
        empty = shipped("models/block/chao_ball_active_empty.json")
        self.assertEqual(empty["elements"], [])
        self.assertEqual(empty["textures"]["particle"], builder.TEXTURE)
        for frame in range(4):
            self.assertEqual(shipped(f"models/block/chao_ball_play_{frame}.json"), {"parent": builder.IDLE})

    def test_targeted_regeneration_matches_shipped_and_writes_only_ball_assets(self):
        expected = {"models/block/chao_ball_idle.json", "models/block/chao_ball_active_empty.json",
                    "models/item/chao_ball.json", "blockstates/chao_ball.json",
                    "textures/block/ball074/chao_ball_panels.png"} | {
                        f"models/block/chao_ball_play_{n}.json" for n in range(4)}
        output = builder.generated()
        self.assertEqual(set(output), expected)
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            for name, data in output.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
                self.assertEqual(path.read_bytes(), (ASSETS / name).read_bytes(), name)
            self.assertEqual({p.relative_to(root).as_posix() for p in root.rglob("*") if p.is_file()}, expected)
        self.assertEqual(Image.open(BytesIO(output["textures/block/ball074/chao_ball_panels.png"])).size,
                         (256, 256))


if __name__ == "__main__":
    unittest.main(verbosity=2)
