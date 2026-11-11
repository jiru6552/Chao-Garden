#!/usr/bin/env python3
"""Rigid horse/lifesaver generator and shipped OBJ regressions, no game required.

Run: python scripts/test_mounted_toy_geometry_02518.py
This is geometry validation, not native visual verification.
"""
import math
import json
import unittest

import generate_sa2_assets_02515 as assets


def emitted(mesh):
    return [p for ps, _, _ in mesh.faces
            if sum(v*v for v in assets.normal(ps)) >= .9 for p in ps]


def shipped(name, frame):
    stage = 'idle' if frame < 0 else 'play_' + str(frame)
    path = assets.A / 'models/sa2_02515' / (name + '_' + stage + '.obj')
    return [tuple(map(float, line.split()[1:])) for line in path.read_text().splitlines()
            if line.startswith('v ')]


class MountedToyGeometryTest(unittest.TestCase):
    def test_horse_axis_pivot_floor_and_expected_transforms(self):
        self.assertEqual(assets.HORSE_ROCK_ANGLES, (0, 6, 0, -6))
        self.assertEqual(assets.HORSE_ROCK_PIVOT, (8, .08, 8))
        rest_mesh = assets.mounted_toy_rest('chao_rocking_horse')
        rest = emitted(rest_mesh)
        # Current source already clears the floor with one unchanged rest offset.
        self.assertEqual(assets.horse_common_lift(rest_mesh), 0)
        for frame, degrees in enumerate((0, 6, 0, -6)):
            c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
            posed = emitted(assets.mounted_toy_pose('chao_rocking_horse', frame))
            self.assertEqual(len(rest), len(posed))
            for (x, y, z), actual in zip(rest, posed):
                expected = (x, .08 + (y-.08)*c - (z-8)*s,
                            8 + (y-.08)*s + (z-8)*c)
                for a, b in zip(actual, expected):
                    self.assertAlmostEqual(a, b, places=12)
                # Every point keeps its radius about the same ground pivot.
                self.assertAlmostEqual(math.dist(actual, (8, .08, 8)),
                                       math.dist((x, y, z), (8, .08, 8)), places=12)
            self.assertGreaterEqual(min(p[1] for p in posed), 0)
            self.assertTrue(all(1 <= p[0] <= 15 and 1 <= p[2] <= 15.1 for p in posed))
        # Floor position really changes; a per-frame recenter would violate this.
        self.assertNotEqual(assets.mounted_toy_pose('chao_rocking_horse', 1).bounds()[1],
                            assets.mounted_toy_pose('chao_rocking_horse', 3).bounds()[1])

    def test_horse_source_forward_and_saddle(self):
        rest = assets.mounted_toy_rest('chao_rocking_horse')
        muzzle = [p for ps, _, mat in rest.faces if mat == 'sa2_horse03_52' for p in ps]
        tail = [p for ps, _, mat in rest.faces if mat == 'sa2_horse01_52' for p in ps]
        self.assertGreater(min(p[2] for p in muzzle), max(p[2] for p in tail))
        # Source back endpoints support a saddle near (.5,.428,.4375) blocks.
        back = [p for p in emitted(rest) if 6.8 < p[1] < 6.9 and 5.7 < p[2] < 8.2]
        self.assertTrue(back)
        self.assertLess(min(p[2] for p in back), 7)
        self.assertGreater(max(p[2] for p in back), 7)

    def test_lifesaver_translates_only_and_keeps_center(self):
        self.assertEqual(assets.LIFESAVER_BOB_BLOCKS, (0, .025, 0, -.025))
        rest = emitted(assets.mounted_toy_rest('chao_lifesaver'))
        for frame, bob in enumerate((0, .025, 0, -.025)):
            posed = emitted(assets.mounted_toy_pose('chao_lifesaver', frame))
            for initial, current in zip(rest, posed):
                self.assertEqual(initial[0], current[0])
                self.assertEqual(initial[2], current[2])
                self.assertAlmostEqual(current[1]-initial[1], 16*bob, places=12)
            for axis in (0, 2):
                self.assertAlmostEqual(min(p[axis] for p in posed)+max(p[axis] for p in posed), 16)

    def test_waterlogged_variants_float_at_water_surface(self):
        self.assertEqual(assets.LIFESAVER_FLOAT_BLOCKS, .80)
        for frame in (-1, 0, 1, 2, 3):
            dry = shipped('chao_lifesaver', frame)
            wet = shipped('chao_lifesaver_float', frame)
            expected = emitted(assets.mounted_toy_pose('chao_lifesaver', frame, True))
            self.assertEqual(len(dry), len(wet))
            for d, w, e in zip(dry, wet, expected):
                self.assertEqual(d[0], w[0])
                self.assertEqual(d[2], w[2])
                self.assertAlmostEqual(w[1]-d[1], .80, delta=1.01e-7)
                for actual, wanted in zip(w, e):
                    self.assertAlmostEqual(actual, wanted/16, delta=5.01e-8)
            self.assertGreaterEqual(min(p[1] for p in wet), .78)
            self.assertLessEqual(max(p[1] for p in wet), 1.0075)
        variants = json.loads((assets.A / 'blockstates/chao_lifesaver.json').read_text())['variants']
        self.assertEqual(len(variants), 64)
        for facing, angle in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            for playing in ('true', 'false'):
                for frame in range(4):
                    for waterlogged in ('true', 'false'):
                        key = f'facing={facing},frame={frame},playing={playing},waterlogged={waterlogged}'
                        value = variants[key]
                        suffix = ('_float_' if waterlogged == 'true' else '_')
                        suffix += 'play_'+str(frame) if playing == 'true' else 'idle'
                        self.assertEqual(value['model'], 'chao_garden:block/chao_lifesaver'+suffix)
                        self.assertEqual(value['y'], angle)
                        model = json.loads((assets.A / 'models/block' / ('chao_lifesaver'+suffix+'.json')).read_text())
                        self.assertTrue((assets.A / model['model'].split(':')[1]).exists())

    def test_shipped_poses_match_generator_and_rigid_distances(self):
        for name in assets.MOUNTED_TOYS:
            rest = shipped(name, -1)
            self.assertEqual(rest, shipped(name, 0))
            self.assertEqual(rest, shipped(name, 2))
            for frame in (-1, 0, 1, 2, 3):
                actual = shipped(name, frame)
                expected = emitted(assets.mounted_toy_pose(name, frame))
                self.assertEqual(len(actual), len(expected))
                for got, wanted in zip(actual, expected):
                    for g, w in zip(got, wanted):
                        self.assertAlmostEqual(g, w/16, delta=5.01e-8)
                # Distant and adjacent pairs catch any frame-dependent scaling.
                for i in range(len(rest)):
                    for j in ((i+1) % len(rest), (i+len(rest)//2) % len(rest)):
                        self.assertAlmostEqual(math.dist(actual[i], actual[j]),
                                               math.dist(rest[i], rest[j]), delta=2e-7)


if __name__ == '__main__':
    unittest.main(verbosity=2)
