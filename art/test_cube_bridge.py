#!/usr/bin/env python3
"""Semantic regression checks for editable Blockbench cube export.

These use known world-space bounds, volumes and the official Blockbench
CubeFace.UVToLocal orientation, not a duplicate implementation of the exporter.
Run from the source root: python3 art/test_cube_bridge.py
"""
from __future__ import annotations

import copy
import math
import unittest

from blockbench_to_runtime import cube_to_runtime, export_model


DIRECTIONS = ("north", "east", "south", "west", "up", "down")
MATERIALS = {"body": [0, 0, 32, 32]}


def cube(**updates):
    value = {
        "uuid": "test-cube",
        "type": "cube",
        "name": "editable block",
        "from": [-1, 0, -1],
        "to": [1, 2, 1],
        "origin": [0, 0, 0],
        "rotation": [0, 0, 0],
        "faces": {name: {"uv": [5, 7, 7, 10], "texture": 0} for name in DIRECTIONS},
        "chao_material": "body",
    }
    value.update(updates)
    return value


def bounds(mesh):
    return [(min(v[axis] for v in mesh["vertices"]), max(v[axis] for v in mesh["vertices"]))
            for axis in range(3)]


def volume(mesh):
    result = 0
    for face in mesh["faces"]:
        vertices = [mesh["vertices"][i] for i in face["v"]]
        a = vertices[0]
        for i in range(1, len(vertices) - 1):
            b, c = vertices[i], vertices[i + 1]
            result += (a[0] * (b[1] * c[2] - b[2] * c[1])
                       + a[1] * (b[2] * c[0] - b[0] * c[2])
                       + a[2] * (b[0] * c[1] - b[1] * c[0])) / 6
    return result


def project(element, flat_groups=False, box_uv=False):
    group = {"name": "root", "uuid": "test-root", "origin": [0, 0, 0],
             "rotation": [0, 0, 0], "children": [element["uuid"]]}
    return {
        "meta": {"model_format": "free", "format_version": "5.0" if flat_groups else "4.0", "box_uv": box_uv},
        "resolution": {"width": 256, "height": 256},
        "chao_runtime": {"ground_y": 24, "materials": MATERIALS},
        "elements": [element],
        "groups": [group] if flat_groups else [],
        "outliner": [{"uuid": group["uuid"], "children": group["children"]}] if flat_groups else [group],
    }


class CubeBridgeTests(unittest.TestCase):
    def export(self, source):
        return cube_to_runtime(source, [0, 0, 0], MATERIALS)

    def test_closed_cube_has_shared_corners_outward_faces_and_expected_volume(self):
        mesh = self.export(cube())
        self.assertEqual(bounds(mesh), [(-1, 1), (-2, 0), (-1, 1)])
        self.assertEqual(len(mesh["vertices"]), 8)
        self.assertEqual(len(mesh["faces"]), 6)
        self.assertAlmostEqual(volume(mesh), 8)
        self.assertEqual(mesh["primitive"], "cube")
        edges = {}
        for face in mesh["faces"]:
            ids = face["v"]
            for i, a in enumerate(ids):
                b = ids[(i + 1) % 4]
                edges.setdefault(tuple(sorted((a, b))), []).append((a, b))
        self.assertEqual(len(edges), 12)
        for uses in edges.values():
            self.assertEqual(len(uses), 2)
            self.assertEqual(uses[0], uses[1][::-1])

    def test_cube_rotation_is_about_its_own_absolute_pivot(self):
        source = cube(**{"from": [10, 20, 30], "to": [12, 24, 36],
                         "origin": [11, 22, 33], "rotation": [0, 90, 0]})
        mesh = cube_to_runtime(source, [10, 20, 30], MATERIALS)
        self.assertEqual(bounds(mesh), [(-2, 4), (-4, 0), (2, 4)])
        self.assertAlmostEqual(volume(mesh), 48)
        first = mesh["vertices"][mesh["faces"][0]["v"][0]]
        self.assertEqual(first, [-2, -4, 2])

    def test_inflate_and_stretch_preserve_cube_center(self):
        mesh = self.export(cube(**{"from": [0, 0, 0], "to": [2, 2, 2],
                                  "inflate": .5, "stretch": [2, 1, .5]}))
        self.assertEqual(bounds(mesh), [(-2, 4), (-2.5, .5), (.25, 1.75)])
        self.assertAlmostEqual(volume(mesh), 27)
        self.assertIsNone(self.export(cube(inflate=-1)))

    def test_uv_top_left_matches_blockbench_north_view(self):
        mesh = self.export(cube(**{"from": [0, 0, 0], "to": [2, 3, 4]}))
        face = mesh["faces"][0]
        self.assertEqual([mesh["vertices"][i] for i in face["v"]],
                         [[2, -3, 0], [0, -3, 0], [0, 0, 0], [2, 0, 0]])
        self.assertEqual(face["uv"], [[5, 7], [7, 7], [7, 10], [5, 10]])

    def test_uv_rotation_and_reversed_uv_bounds_change_texture_not_shape(self):
        source = cube(**{"from": [0, 0, 0], "to": [2, 3, 4]})
        source["faces"]["north"]["rotation"] = 90
        mesh = self.export(source)
        face = mesh["faces"][0]
        self.assertEqual(mesh["vertices"][face["v"][0]], [0, -3, 0])
        self.assertEqual(face["uv"][0], [5, 7])
        source["faces"]["north"]["uv"] = [7, 7, 5, 10]
        flipped = self.export(source)
        self.assertEqual(bounds(mesh), bounds(flipped))
        self.assertEqual(flipped["faces"][0]["uv"][0], [7, 7])

    def test_box_uv_mirror_flips_north_and_swaps_east_west_islands(self):
        source = cube(**{"from": [0, 0, 0], "to": [2, 3, 4],
                         "box_uv": True, "uv_offset": [10, 20]})
        original = self.export(source)
        source["mirror_uv"] = True
        mirrored = self.export(source)
        self.assertEqual(bounds(original), bounds(mirrored))
        self.assertEqual(original["faces"][0]["uv"], [[14, 24], [16, 24], [16, 27], [14, 27]])
        self.assertEqual(mirrored["faces"][0]["uv"], [[16, 24], [14, 24], [14, 27], [16, 27]])
        self.assertEqual(mirrored["faces"][1]["uv"], [[20, 24], [16, 24], [16, 27], [20, 27]])

    def test_face_planes_emit_one_double_sided_quad_without_degenerate_edges(self):
        source = cube(**{"from": [-2, 0, 0], "to": [2, 3, 0]})
        mesh = self.export(source)
        self.assertEqual(len(mesh["faces"]), 1)
        self.assertEqual(len(mesh["vertices"]), 4)
        self.assertEqual(bounds(mesh), [(-2, 2), (-3, 0), (0, 0)])
        self.assertEqual(volume(mesh), 0)
        source["faces"]["north"]["texture"] = None
        mesh = self.export(source)
        self.assertEqual(len(mesh["faces"]), 1)

    def test_disabled_and_null_textured_faces_do_not_emit(self):
        source = cube()
        source["faces"]["north"]["texture"] = None
        source["faces"]["east"]["enabled"] = False
        self.assertEqual(len(self.export(source)["faces"]), 4)

    def test_v4_v5_and_project_box_uv_inheritance(self):
        source = cube(uv_offset=[10, 20])
        a = export_model(project(source, box_uv=True))
        b = export_model(project(source, flat_groups=True, box_uv=True))
        self.assertEqual(a, b)
        mesh = a["bones"][0]["meshes"][0]
        self.assertEqual(mesh["faces"][0]["uv"], [[12, 22], [14, 22], [14, 24], [12, 24]])

    def test_existing_mesh_elements_keep_local_origin_and_uv_pairing(self):
        mesh = {
            "uuid": "test-mesh", "type": "mesh", "name": "legacy face",
            "origin": [4, 5, 6], "rotation": [0, 0, 0], "shading": "flat",
            "vertices": {"a": [0, 0, 0], "b": [2, 0, 0], "c": [0, 3, 0]},
            "faces": {"front": {"vertices": ["a", "b", "c"],
                                  "uv": {"a": [0, 0], "b": [2, 0], "c": [0, 3]}, "texture": 0}},
        }
        data = project(mesh)
        data["outliner"][0]["origin"] = [1, 2, 3]
        result = export_model(data)["bones"][0]["meshes"][0]
        self.assertEqual(result["vertices"], [[3, -3, 3], [5, -3, 3], [3, -6, 3]])
        self.assertEqual(result["faces"], [{"v": [2, 1, 0], "uv": [[0, 3], [2, 0], [0, 0]]}])

    def test_expression_materials_survive_removed_editor_metadata(self):
        for material, u in (("happy_eye", 96), ("lid", 104), ("mouth_open", 112)):
            mesh = {
                "uuid": "expression", "type": "mesh", "name": "expression plane",
                "vertices": {"a": [0, 0, 0], "b": [1, 0, 0], "c": [0, 1, 0]},
                "faces": {"face": {"vertices": ["a", "b", "c"], "texture": 0,
                                     "uv": {"a": [u, 64], "b": [u + 1, 64], "c": [u, 65]}}},
            }
            data = project(mesh)
            data.pop("chao_runtime")
            exported = export_model(data)["bones"][0]["meshes"][0]
            self.assertEqual(exported["material"], material)

    def test_invalid_cube_coordinates_and_uv_rotations_are_rejected(self):
        source = cube()
        source["from"][0] = math.inf
        with self.assertRaises(ValueError):
            self.export(source)
        source = cube()
        source["faces"]["north"]["rotation"] = 45
        with self.assertRaises(ValueError):
            self.export(source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
