#!/usr/bin/env python3
"""Rattle/drum rigid geometry and shipped-asset checks, without a game runtime.

Run: python scripts/test_rigid_toy_geometry_02518.py
These checks do not constitute native build or visual verification.
"""
import contextlib
import hashlib
import io
import json
import math
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

import generate_sa2_assets_02515 as assets


FRAMES = (-1, 0, 1, 2, 3)
NAMES = ('chao_rattle', 'chao_drum')


def emitted(mesh):
    return [p for ps, _, _ in mesh.faces
            if sum(v*v for v in assets.normal(ps)) >= .9 for p in ps]


def stage(frame):
    return 'idle' if frame < 0 else 'play_' + str(frame)


def shipped(name, frame):
    path = assets.A / 'models/sa2_02515' / (name + '_' + stage(frame) + '.obj')
    lines = path.read_text().splitlines()
    return [tuple(map(float, line.split()[1:])) for line in lines if line.startswith('v ')]


def rotated(point, pivot, degrees, lift=0):
    x, y, z = point
    cx, cy, _ = pivot
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    return (cx + (x-cx)*c - (y-cy)*s,
            cy + (x-cx)*s + (y-cy)*c + lift, z)


class RigidToyGeometryTest(unittest.TestCase):
    def test_rest_uses_original_idle_size_and_one_uniform_scale(self):
        self.assertEqual(assets.RIGID_TOYS, NAMES)
        for name in NAMES:
            source = assets.toy_mesh(name)[0]
            source_bounds = source.bounds()
            limits = list(assets.LIMITS[name])
            limits[1] = .08
            limits[4] -= .08
            scale = min((limits[a+3]-limits[a]) /
                        (source_bounds[a+3]-source_bounds[a]) for a in range(3))
            rest_mesh = assets.rigid_toy_rest(name)
            rest = emitted(rest_mesh)
            source_points = emitted(source)
            self.assertEqual(len(source_points), len(rest))
            self.assertAlmostEqual(rest_mesh.bounds()[1], .08, places=12)
            for initial, actual in zip(source_points, rest):
                expected = ((initial[0]-(source_bounds[0]+source_bounds[3])/2)*scale+8,
                            (initial[1]-source_bounds[1])*scale+.08,
                            (initial[2]-(source_bounds[2]+source_bounds[5])/2)*scale+8)
                for a, b in zip(actual, expected):
                    self.assertAlmostEqual(a, b, places=12)
            # Preserve idle dimensions, not the former enlarged rattle play fit.
            bounds = rest_mesh.bounds()
            if name == 'chao_rattle':
                self.assertAlmostEqual(bounds[4]-bounds[1], 3.84, places=12)
            else:
                self.assertAlmostEqual(bounds[3]-bounds[0], 10, places=12)

    def test_every_frame_uses_fixed_rotation_and_vertical_floor_support(self):
        self.assertEqual(assets.RIGID_TOY_ANGLES, (0, 8, 0, -8))
        self.assertEqual(assets.RIGID_TOY_FLOOR, .08)
        for name in NAMES:
            rest_mesh = assets.rigid_toy_rest(name)
            rest = emitted(rest_mesh)
            bounds = rest_mesh.bounds()
            pivot = tuple((bounds[a]+bounds[a+3])/2 for a in range(3))
            # Compute each support translation independently of pose helpers.
            all_source_points = [p for ps, _, _ in rest_mesh.faces for p in ps]
            for frame, angle in zip(FRAMES, (0, 0, 8, 0, -8)):
                minimum = min(rotated(p, pivot, angle)[1] for p in all_source_points)
                lift = .08-minimum
                actual = emitted(assets.rigid_toy_pose(name, frame))
                self.assertEqual(len(rest), len(actual))
                for initial, current in zip(rest, actual):
                    expected = rotated(initial, pivot, angle, lift)
                    for a, b in zip(current, expected):
                        self.assertAlmostEqual(a, b, places=12)
                    self.assertEqual(initial[2], current[2])
                self.assertAlmostEqual(min(p[1] for p in actual), .08, places=12)
                # Only Y may receive support correction; X must keep its fixed rotation.
                for initial, current in zip(rest, actual):
                    self.assertAlmostEqual(current[0], rotated(initial, pivot, angle)[0], places=12)
            # The neutral frames retain the original idle fit, without hovering.
            for frame in (-1, 0, 2):
                self.assertEqual(emitted(assets.rigid_toy_pose(name, frame)), rest)

    def test_all_mesh_edges_and_long_distances_remain_rigid(self):
        for name in NAMES:
            rest = assets.rigid_toy_pose(name, -1)
            for frame in range(4):
                pose = assets.rigid_toy_pose(name, frame)
                self.assertEqual(len(rest.faces), len(pose.faces))
                for (original, uv, mat), (current, pose_uv, pose_mat) in zip(rest.faces, pose.faces):
                    self.assertEqual((uv, mat), (pose_uv, pose_mat))
                    for i in range(len(original)):
                        j = (i+1) % len(original)
                        self.assertAlmostEqual(math.dist(original[i], original[j]),
                                               math.dist(current[i], current[j]), places=11)
                points, initial = emitted(pose), emitted(rest)
                for i in range(len(initial)):
                    j = (i+len(initial)//2) % len(initial)
                    self.assertAlmostEqual(math.dist(points[i], points[j]),
                                           math.dist(initial[i], initial[j]), places=11)

    def test_shipped_geometry_matches_generator_and_has_no_scale_jump(self):
        for name in NAMES:
            rest = shipped(name, -1)
            self.assertEqual(rest, shipped(name, 0))
            self.assertEqual(rest, shipped(name, 2))
            for frame in FRAMES:
                actual = shipped(name, frame)
                expected = emitted(assets.rigid_toy_pose(name, frame))
                self.assertEqual(len(actual), len(expected))
                for got, wanted in zip(actual, expected):
                    for g, w in zip(got, wanted):
                        self.assertAlmostEqual(g, w/16, delta=5.01e-8)
                self.assertAlmostEqual(min(p[1] for p in actual), .005, delta=5.01e-8)
                for i in range(len(rest)):
                    for j in ((i+1) % len(rest), (i+len(rest)//2) % len(rest)):
                        self.assertAlmostEqual(math.dist(actual[i], actual[j]),
                                               math.dist(rest[i], rest[j]), delta=2e-7)
                model = json.loads((assets.A / 'models/block' /
                                    (name+'_'+stage(frame)+'.json')).read_text())
                self.assertEqual(model['model'], 'chao_garden:models/sa2_02515/'+name+'_'+stage(frame)+'.obj')

    def test_targeted_regeneration_touches_only_requested_block_poses(self):
        original = assets.A
        with tempfile.TemporaryDirectory() as folder:
            destination = Path(folder)
            for name in NAMES:
                idle = Path('models/block') / (name+'_idle.json')
                (destination / idle).parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(original / idle, destination / idle)
                textures = json.loads((original / idle).read_text())['textures']
                for texture in textures.values():
                    path = Path('textures') / (texture.split(':')[1]+'.png')
                    (destination / path).parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(original / path, destination / path)
            sentinel = destination / 'unrelated-coffin-horse-ring-item-and-manifest'
            sentinel.write_text('Do not touch unrelated assets.')
            before = {p.relative_to(destination): hashlib.sha256(p.read_bytes()).hexdigest()
                      for p in destination.rglob('*') if p.is_file()}
            with patch.object(assets, 'A', destination), contextlib.redirect_stdout(io.StringIO()):
                assets.rigid_toy_poses_only()
            expected_paths = {Path(directory) / (name+'_'+stage(frame)+suffix)
                              for name in NAMES for frame in FRAMES
                              for directory, suffix in (('models/block', '.json'),
                                                        ('models/sa2_02515', '.obj'))}
            after = {p.relative_to(destination): hashlib.sha256(p.read_bytes()).hexdigest()
                     for p in destination.rglob('*') if p.is_file()}
            self.assertEqual(set(after), set(before) | expected_paths)
            for path, digest in before.items():
                if path not in expected_paths:
                    self.assertEqual(after[path], digest)
            # Targeted regeneration is reproducible with the actual shipped data.
            for path in expected_paths:
                self.assertEqual((destination / path).read_bytes(), (original / path).read_bytes())


if __name__ == '__main__':
    unittest.main(verbosity=2)
