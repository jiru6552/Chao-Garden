#!/usr/bin/env python3
"""Regress source hinge, planted hull, rigid lid and the actual shipped OBJ poses.

Run: python scripts/test_coffin_geometry_02518.py
No Minecraft/JDK or regeneration required. The original-archive check is skipped
only in source distributions that omit the original supplied PRS archive.
"""
import math
import struct
import unittest

import generate_sa2_assets_02515 as assets


def emitted(mesh):
    return [p for ps, _, _ in mesh.faces
            if sum(v*v for v in assets.normal(ps)) >= .9 for p in ps]


def obj_vertices(stage):
    path = assets.A / 'models/sa2_02515' / ('chao_coffin_' + stage + '.obj')
    return [tuple(float(x) for x in line.split()[1:])
            for line in path.read_text().splitlines() if line.startswith('v ')]


class CoffinGeometryTest(unittest.TestCase):
    def test_source_hinge_matches_original_node(self):
        archive = assets.ROOT / 'art/sa2_02514/original/ChaoMain.prs'
        if not archive.exists():
            self.skipTest('Original PRS not included; extracted-geometry tests still run')
        from extract_sa2_02515 import prs, relocate, transform
        data = relocate(prs(archive.read_bytes()))
        root = transform(data, 0x30ad7c)
        lid = root @ transform(data, 0x30a8a8)
        self.assertEqual(struct.unpack_from('>I', data, 0x30ad7c + 44)[0],
                         int(assets.COFFIN_LID_NODE, 16))
        for actual, expected in zip(assets.COFFIN_HINGE, lid[:3, 3]):
            self.assertAlmostEqual(actual, expected, places=12)
        for actual, expected in zip(assets.COFFIN_AXIS, lid[:3, 2]):
            self.assertAlmostEqual(actual, expected, places=12)

    def test_hinge_is_on_rest_lid_long_edge(self):
        source = assets.TOYS['chao_coffin']['faces']
        ps = [p for f in source if f['node'] == assets.COFFIN_LID_NODE
              for p in f['points']]
        cx, cy, cz = assets.COFFIN_HINGE
        ax, _, az = assets.COFFIN_AXIS
        # Source hinge is inset <0.008 source units from the lower long edge.
        # Both edge endpoints, several units apart, must stay near this axis.
        hinge_points = [p for p in ps if abs(p[1]-cy) < 1e-4
                        and abs((p[0]-cx)*az-(p[2]-cz)*ax) < .008]
        self.assertGreater(max(p[0] for p in hinge_points) -
                           min(p[0] for p in hinge_points), 4)

    def test_source_lid_is_rigid_and_hull_unchanged(self):
        rest = assets.coffin_source_pose(-1)['faces']
        for frame in range(4):
            posed = assets.coffin_source_pose(frame)['faces']
            self.assertEqual(len(rest), len(posed))
            for initial, current in zip(rest, posed):
                if initial['node'] != assets.COFFIN_LID_NODE:
                    self.assertEqual(initial, current)
                    continue
                for a in range(3):
                    b = (a+1) % 3
                    self.assertAlmostEqual(math.dist(initial['points'][a], initial['points'][b]),
                                           math.dist(current['points'][a], current['points'][b]), places=10)

    def test_shipped_poses_are_planted_monotonic_and_match_generator(self):
        self.assertEqual(assets.COFFIN_OPEN_ANGLES, (0, 25, 50, 75))
        stages = ['idle', 'play_0', 'play_1', 'play_2', 'play_3']
        shipped = [obj_vertices(stage) for stage in stages]
        self.assertEqual(shipped[0], shipped[1])
        # Resolve output vertex ownership independently using isolated source faces.
        ownership = []
        for face in assets.TOYS['chao_coffin']['faces']:
            mesh, _ = assets.adapt({**assets.TOYS['chao_coffin'], 'faces': [face]})
            ownership.extend([face['node']] * len(emitted(mesh)))
        hull = [i for i, node in enumerate(ownership) if node != assets.COFFIN_LID_NODE]
        lid = [i for i, node in enumerate(ownership) if node == assets.COFFIN_LID_NODE]
        self.assertTrue(hull and lid)
        heights = []
        for frame, actual in zip((-1, 0, 1, 2, 3), shipped):
            expected = emitted(assets.coffin_pose(frame))
            self.assertEqual(len(actual), len(expected))
            self.assertEqual(len(actual), len(ownership))
            for got, wanted in zip(actual, expected):
                for g, w in zip(got, wanted):
                    self.assertAlmostEqual(g, w/16, delta=5.01e-8)
            self.assertEqual([actual[i] for i in hull], [shipped[0][i] for i in hull])
            heights.append(sum(actual[i][1] for i in lid)/len(lid))
        self.assertTrue(all(b-a > .015 for a, b in zip(heights[1:], heights[2:])), heights)
        self.assertGreater(max(math.dist(shipped[1][i], shipped[-1][i]) for i in lid), .4)
        # The lid must rise above the old fitting envelope, never shrink back into it.
        self.assertGreater(max(p[1] for p in shipped[-1]), max(p[1] for p in shipped[0]) + .2)


if __name__ == '__main__':
    unittest.main(verbosity=2)
