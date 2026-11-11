#!/usr/bin/env python3
"""Check actual stationary-toy meshes against the production collision helpers.

Run: python scripts/test_stationary_toy_geometry_02518.py
Requires Python 3 and Java 21 with jdk.compiler; no Minecraft, native renderer,
asset regeneration, or third-party Python packages are needed. This is geometry
and isolated Java verification, not a native Minecraft collision/visual test.

OBJ coordinates are blocks and are converted to model units (16 per block).
JSON element corners include their axis rotation, origin, and rescale. Blockstate
rotations are applied independently to the mesh and to the production Java box.
Every idle/play model and all four facing variants are checked. Bounds remain
constant across animation frames so starting a session cannot grow a collider.
"""
import itertools
import json
import math
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
RULES = ROOT / 'src/main/java/com/chaogarden/toys/ToyInteractionRules.java'
TOYS = ('radio', 'guitar', 'triangle', 'rubber_duck', 'jack_in_box', 'rattle', 'drum')
FACING = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
STAGES = ('idle', 'play_0', 'play_1', 'play_2', 'play_3')

# Explicit reviewed envelopes, not derived from current production constants.
# The floor extension is intentional; every other face is tight to the union
# of all five shipped animation frames, allowing at most .01 model units.
ENVELOPES = {
    'radio': (2, 0, 5.75, 14, 5.63, 10.25),
    'guitar': (3, 0, 5.84, 13, 4.89, 10.16),
    'triangle': (4, 0, 6, 12, 14, 10.6),
    'rubber_duck': (1, 0, 3, 16, 11, 13),
    'jack_in_box': (3.8, 0, 3.8, 14, 16, 12.5),
    'rattle': (6.25, 0, 4.74, 9.61, 3.96, 11.26),
    'drum': (2.72, 0, 3, 13.28, 6.08, 13),
}
UNIONS = {
    'radio': (2, .08, 5.756056, 14, 5.6236688, 10.243944),
    'guitar': (3, 0, 5.845896, 13, 4.8887632, 10.154104),
    'triangle': (4, 1, 6, 12, 14, 10.6),
    'rubber_duck': (1, 1, 3, 16, 11, 13),
    'jack_in_box': (3.8, 0, 3.8, 14, 16, 12.5),
    'rattle': (6.2529744, .08, 4.741616, 9.6089536, 3.9577792, 11.258384),
    'drum': (2.725344, .08, 3.0004848, 13.274656, 6.0727632, 12.9995152),
}


def rotate_element(point, rotation):
    """Minecraft JSON positive element rotation, including optional rescale."""
    origin = rotation['origin']
    angle = float(rotation['angle'])
    if angle not in (-45, -22.5, 0, 22.5, 45):
        raise ValueError(f'Unsupported Minecraft element rotation: {angle}')
    axis = 'xyz'.index(rotation['axis'])
    i, j = ((1, 2), (2, 0), (0, 1))[axis]
    q = [float(v) - float(o) for v, o in zip(point, origin)]
    c, s = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    q[i], q[j] = q[i]*c-q[j]*s, q[i]*s+q[j]*c
    if rotation.get('rescale', False):
        q[i] /= c
        q[j] /= c
    return tuple(v+float(o) for v, o in zip(q, origin))


def element_vertices(element):
    vertices = list(itertools.product(*zip(element['from'], element['to'])))
    if 'rotation' in element:
        vertices = [rotate_element(p, element['rotation']) for p in vertices]
    return vertices


def obj_vertices(path):
    """Read actual face-referenced vertices; reject malformed and unused data."""
    vertices, used = [], set()
    for line in path.read_text().splitlines():
        fields = line.split()
        if not fields:
            continue
        if fields[0] == 'v':
            if len(fields) != 4:
                raise ValueError(f'Unsupported OBJ vertex: {line}')
            vertices.append(tuple(16*float(v) for v in fields[1:]))
        elif fields[0] == 'f':
            if len(fields) < 4:
                raise ValueError(f'Invalid OBJ face: {line}')
            for entry in fields[1:]:
                index = int(entry.split('/')[0])
                if index == 0:
                    raise ValueError('OBJ vertex indices cannot be zero')
                index = index-1 if index > 0 else len(vertices)+index
                if not 0 <= index < len(vertices):
                    raise ValueError(f'Unresolved OBJ vertex index: {entry}')
                used.add(index)
    if not vertices or used != set(range(len(vertices))):
        raise ValueError(f'Empty or unreferenced OBJ geometry: {path}')
    return vertices


def model_vertices(resource):
    namespace, name = resource.split(':', 1)
    if namespace != 'chao_garden':
        raise ValueError(f'Unexpected model namespace: {resource}')
    path = ASSETS / 'models' / (name + '.json')
    model = json.loads(path.read_text())
    if 'transform' in model or 'transforms' in model:
        raise ValueError(f'Model-level transforms require explicit support: {path}')
    if model.get('loader') == 'neoforge:obj':
        namespace, obj = model['model'].split(':', 1)
        if namespace != 'chao_garden':
            raise ValueError(f'Unexpected OBJ namespace: {model["model"]}')
        return obj_vertices(ASSETS / obj)
    if 'loader' in model:
        raise ValueError(f'Unsupported model loader: {path}')
    if 'elements' in model:
        return [v for element in model['elements'] for v in element_vertices(element)]
    if 'parent' in model:
        return model_vertices(model['parent'])
    raise ValueError(f'No geometry in model: {path}')


def blockstate_rotate(point, degrees):
    """Blockstate Y rotation is clockwise looking down, about (8, y, 8)."""
    x, y, z = point
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    return (8+(x-8)*c-(z-8)*s, y, 8+(x-8)*s+(z-8)*c)


def bounds(vertices):
    return tuple(min(p[i] for p in vertices) for i in range(3)) + tuple(
        max(p[i] for p in vertices) for i in range(3))


def expected_rotation(box, quarter_turns):
    """Independent exact AABB transform, separate from mesh trigonometry."""
    x0, y0, z0, x1, y1, z1 = box
    return (
        (x0, y0, z0, x1, y1, z1),
        (16-z1, y0, x0, 16-z0, y1, x1),
        (16-x1, y0, 16-z1, 16-x0, y1, 16-z0),
        (z0, y0, 16-x1, z1, y1, 16-x0),
    )[quarter_turns]


def production_boxes():
    """Compile real dependency-free production code, not a copied algorithm."""
    java = shutil.which('java')
    if not java:
        raise RuntimeError('Java with jdk.compiler is required for production envelope checks')
    harness = '''
import com.chaogarden.toys.ToyInteractionRules;
public final class StationaryGeometryProbe {
  public static void main(String[] names) {
    for (String name : names) {
      for (int q=0; q<4; q++) {
        double[] box=ToyInteractionRules.collisionEnvelope("chao_"+name);
        if (box==null || box.length!=6) throw new AssertionError(name);
        double[] rotated=ToyInteractionRules.rotateEnvelope(box,q);
        System.out.print(name+","+q);
        for (double value : rotated) System.out.print(","+value);
        System.out.println();
      }
    }
  }
}
'''
    with tempfile.TemporaryDirectory(prefix='stationary-toy-geometry-') as temp:
        source = Path(temp) / 'StationaryGeometryProbe.java'
        source.write_text(harness)
        commands = (
            [java, '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-d', temp,
             str(RULES), str(source)],
            [java, '-ea', '-cp', temp, 'StationaryGeometryProbe', *TOYS],
        )
        for command in commands:
            result = subprocess.run(command, capture_output=True, text=True, timeout=60)
            if result.returncode:
                raise RuntimeError('Production Java probe failed:\n' + result.stdout + result.stderr)
    result_boxes = {}
    for line in result.stdout.splitlines():
        name, turns, *values = line.split(',')
        if len(values) != 6:
            raise ValueError(f'Malformed production envelope: {line}')
        result_boxes[name, int(turns)] = tuple(map(float, values))
    return result_boxes


class GeometryParserTest(unittest.TestCase):
    def assertPointAlmostEqual(self, actual, expected):
        for a, e in zip(actual, expected):
            self.assertAlmostEqual(a, e, places=10)

    def test_element_rotation_uses_axis_origin_sign_and_rescale(self):
        t = math.sqrt(.5)
        for axis, point, expected in (
                ('x', (2, 4, 4), (2, 3+t, 4+t)),
                ('y', (2, 3, 5), (2+t, 3, 4+t)),
                ('z', (3, 3, 4), (2+t, 3+t, 4))):
            rotation = {'axis': axis, 'angle': 45, 'origin': (2, 3, 4)}
            self.assertPointAlmostEqual(rotate_element(point, rotation), expected)
            rotation['angle'] = -45
            self.assertPointAlmostEqual(rotate_element(expected, rotation), point)
            rotation.update(angle=45, rescale=True)
            scaled = tuple(o+(e-o)/t for o, e in zip((2, 3, 4), expected))
            self.assertPointAlmostEqual(rotate_element(point, rotation), scaled)

    def test_real_triangle_rotated_corners_are_not_unrotated_boxes(self):
        model = json.loads((ASSETS / 'models/block/chao_triangle_idle.json').read_text())
        arm = model['elements'][1]
        actual = bounds(element_vertices(arm))
        unrotated = tuple(arm['from']+arm['to'])
        self.assertLess(actual[0], unrotated[0]-3)
        self.assertGreater(actual[4], unrotated[4])
        self.assertAlmostEqual(actual[0], 8-.35*math.cos(math.pi/8)-9.5*math.sin(math.pi/8))

    def test_blockstate_facing_rotation_has_independent_cardinal_landmarks(self):
        for degrees, expected in ((0, (8, 5, 0)), (90, (16, 5, 8)),
                                  (180, (8, 5, 16)), (270, (0, 5, 8))):
            self.assertPointAlmostEqual(blockstate_rotate((8, 5, 0), degrees), expected)


class StationaryToyGeometryTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.boxes = production_boxes()
        cls.meshes = {(name, stage): model_vertices(f'chao_garden:block/chao_{name}_{stage}')
                      for name in TOYS for stage in STAGES}

    def assertBoundsAlmostEqual(self, actual, expected):
        self.assertEqual(len(actual), 6)
        for a, e in zip(actual, expected):
            self.assertAlmostEqual(a, e, places=7)

    def assertContains(self, box, vertices):
        actual = bounds(vertices)
        for axis in range(3):
            self.assertGreaterEqual(actual[axis]+1e-8, box[axis], (actual, box))
            self.assertLessEqual(actual[axis+3]-1e-8, box[axis+3], (actual, box))

    def test_shipped_geometry_unions_match_reviewed_dimensions(self):
        for name in TOYS:
            with self.subTest(toy=name):
                vertices = [p for stage in STAGES for p in self.meshes[name, stage]]
                self.assertBoundsAlmostEqual(bounds(vertices), UNIONS[name])

    def test_production_envelopes_match_tight_reviewed_boxes_and_all_facings(self):
        self.assertEqual(len(self.boxes), len(TOYS)*4)
        for name in TOYS:
            for turns in range(4):
                with self.subTest(toy=name, quarter_turns=turns):
                    self.assertBoundsAlmostEqual(self.boxes[name, turns],
                                                 expected_rotation(ENVELOPES[name], turns))

    def test_every_shipped_variant_is_inside_actual_java_collision_box(self):
        for name in TOYS:
            variants = json.loads((ASSETS / f'blockstates/chao_{name}.json').read_text())['variants']
            expected_keys = {f'facing={facing},frame={frame},playing={playing}'
                             for facing in FACING for frame in range(4)
                             for playing in ('false', 'true')}
            self.assertEqual(set(variants), expected_keys)
            for key, variant in variants.items():
                with self.subTest(toy=name, variant=key):
                    state = dict(item.split('=') for item in key.split(','))
                    stage = 'play_'+state['frame'] if state['playing']=='true' else 'idle'
                    self.assertEqual(variant['model'], f'chao_garden:block/chao_{name}_{stage}')
                    angle = FACING[state['facing']]
                    self.assertEqual(variant.get('y', 0), angle)
                    self.assertEqual(variant.get('x', 0), 0)
                    vertices = [blockstate_rotate(p, angle) for p in self.meshes[name, stage]]
                    self.assertContains(self.boxes[name, angle//90], vertices)

    def test_collision_has_no_excess_padding_except_floor_and_animation_union(self):
        for name in TOYS:
            for turns in range(4):
                with self.subTest(toy=name, quarter_turns=turns):
                    mesh = bounds([blockstate_rotate(p, turns*90) for stage in STAGES
                                   for p in self.meshes[name, stage]])
                    box = self.boxes[name, turns]
                    for axis in (0, 2):
                        self.assertLessEqual(mesh[axis]-box[axis], .010001)
                        self.assertLessEqual(box[axis+3]-mesh[axis+3], .010001)
                    self.assertLessEqual(box[4]-mesh[4], .010001)
                    self.assertEqual(box[1], 0)
                    self.assertLessEqual(mesh[1]-box[1], 1.000001)
                    idle = bounds([blockstate_rotate(p, turns*90) for p in self.meshes[name, 'idle']])
                    top_slack = {'rubber_duck': 1, 'jack_in_box': 7,
                                 'rattle': .04, 'drum': 1.27}.get(name, .01)
                    self.assertLessEqual(box[4]-idle[4], top_slack+1e-6)
                    # The jack's lid and rigid rattle/drum motion enlarge the
                    # shared horizontal envelope beyond their individual idle poses.
                    horizontal_slack = {'jack_in_box': .3, 'rattle': .17,
                                        'drum': .28}.get(name, .01)
                    for axis in (0, 2):
                        self.assertLessEqual(idle[axis]-box[axis], horizontal_slack+1e-6)
                        self.assertLessEqual(box[axis+3]-idle[axis+3], horizontal_slack+1e-6)

    def test_asymmetric_models_detect_the_old_swap_without_mirroring(self):
        # These are deliberate negative controls: the old implementation passes
        # symmetric furniture but misses asymmetric geometry after turns.
        for name in ('triangle', 'rubber_duck', 'jack_in_box', 'rattle'):
            for turns in (1, 2, 3):
                box = ENVELOPES[name]
                if turns in (1, 3):
                    box = (box[2], box[1], box[0], box[5], box[4], box[3])
                actual = bounds([blockstate_rotate(p, turns*90) for stage in STAGES
                                 for p in self.meshes[name, stage]])
                differs = any(actual[i] < box[i]-1e-8 or actual[i+3] > box[i+3]+1e-8
                              for i in (0, 2))
                # Duck/rattle Z is symmetric, so east happens to fit the old swap.
                # Triangle's X is symmetric, so west alone also happens to fit it.
                expected = (name, turns) not in {
                    ('rubber_duck', 1), ('rattle', 1), ('triangle', 3)}
                self.assertEqual(differs, expected, (name, turns, actual, box))


if __name__ == '__main__':
    unittest.main(verbosity=2)
