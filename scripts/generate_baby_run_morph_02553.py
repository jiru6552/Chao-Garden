#!/usr/bin/env python3
"""Author the focused baby Run revision without regenerating the adult prototype.

Every baby target retains the canonical vertex index and face winding.  In
particular, cuboid donors with an equal vertex count are not interchangeable.
The new swept shell is a Minecraft interpretation of the supplied Run photo.
"""
from pathlib import Path
import copy
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
SOURCE = MODELS / 'chao_hero_run_proto052.json'
OUT = MODELS / 'chao_baby_run_proto053.json'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rounded(vertices):
    return [[round(float(value), 6) for value in point] for point in vertices]


def run_vertices(name, vertices):
    """Analytic index-preserving changes; no donor cube/face correspondence."""
    if name == 'skull__cube_0':
        # Facial plane and eye-panel registration remain exact.  The rear of
        # the cranial shell tapers into the swept point instead of remaining a
        # second square face.
        return [[x * .88 if z > 0 else x * (.80 if y < -3 else 1),
                 (y + 3) * .65 - 3 if z > 0 else y,
                 4.1 if z > 0 else z] for x, y, z in vertices]
    if name.startswith('body__'):
        return [[x * (1.08 if y < -2 else .94), y * 1.02, z * .96]
                for x, y, z in vertices]
    if name.startswith('arm_') and name.endswith('__cube_0'):
        sign = 1 if 'left' in name else -1
        return [[x * 1.025, (y - .5) * 1.15 + .5, z * 1.15]
                for x, y, z in vertices]
    if name.startswith('arm_') and name.endswith('__cube_1'):
        sign = 1 if 'left' in name else -1
        return [[sign * 3 + (x - sign * 3) * 1.05,
                 1 + (y - 1) * 1.05, -.5 + (z + .5) * 1.05]
                for x, y, z in vertices]
    if name.startswith(('foot_', 'feet_')) and '__' in name:
        center = (min(v[0] for v in vertices) + max(v[0] for v in vertices)) / 2
        # Compact single solids; sole plane Y=2 stays fixed at every target.
        return [[center + (x - center) * 1.075, 2 + (y - 2) * 1.05, z * .90]
                for x, y, z in vertices]
    if name == 'hero_run_rear_052':
        # Close the tapered cranial shell with a connected rear wedge.
        return [[x * .88, (y + 3) * .65 - 3, 4.08 if i < 4 else 5.9]
                for i, (x, y, z) in enumerate(vertices)]
    return copy.deepcopy(vertices)


def closed_quill(root, middle, tip, width, height, mid_width, mid_height):
    def ring(center, w, h):
        x, y, z = center
        return [[x-w/2, y-h/2, z], [x+w/2, y-h/2, z],
                [x+w/2, y+h/2, z], [x-w/2, y+h/2, z]]
    points = ring(root, width, height) + ring(middle, mid_width, mid_height) + [list(tip)]
    # Closed and consistently wound, with a broad rooted surface and a real
    # single-point tip. Degenerate zero-Run points remain inside the skull.
    faces = [[0, 3, 2, 1]]
    faces += [[i, (i+1) % 4, (i+1) % 4 + 4, i+4] for i in range(4)]
    faces += [[i+4, (i+1) % 4+4, 8] for i in range(4)]
    return rounded(points), [
        {'v': face, 'uv': [[63.75 if len(face) == 4 else 63.25, 31.75] for _ in face]}
        for face in faces]


def author():
    protected = {str(path.relative_to(ASSETS)): digest(path) for path in (
        SOURCE, ASSETS/'hero_run_morphology_052.json', MODELS/'chao_baby.json',
        MODELS/'chao_hero.json', MODELS/'chao_run.json', MODELS/'chao_hero_run.json')}
    data = json.loads(SOURCE.read_text())
    original = copy.deepcopy(data)
    for bone in data['bones']:
        name = bone['name']
        old_pivots = copy.deepcopy(bone.get('hero_run_pivots_052', [bone['pivot']] * 5))
        old_rotations = copy.deepcopy(bone.get('hero_run_rotations_052', [bone.get('rotation', [0,0,0])] * 5))
        base_pivot = copy.deepcopy(bone['pivot'])
        base_rotation = copy.deepcopy(bone.get('rotation', [0,0,0]))
        hero_pivot, hero_rotation = copy.deepcopy(base_pivot), copy.deepcopy(base_rotation)
        # Preserve alignment marker articulation, rather than copying anatomy
        # from differently indexed donor meshes.
        if name.startswith('emotion_dot'):
            hero_pivot, hero_rotation = old_pivots[0], old_rotations[0]
        run_pivot, run_rotation = copy.deepcopy(base_pivot), copy.deepcopy(base_rotation)
        if name in ('arm_left', 'arm_right'):
            run_pivot[0] *= .95
        if name in ('foot_left', 'foot_right'):
            run_pivot[2] += .1
        hero_run_pivot = [run_pivot[i] + hero_pivot[i] - base_pivot[i] for i in range(3)]
        hero_run_rotation = [run_rotation[i] + hero_rotation[i] - base_rotation[i] for i in range(3)]
        bone['hero_run_pivots_052'] = [hero_pivot, run_pivot, hero_run_pivot, old_pivots[3], old_pivots[4]]
        bone['hero_run_rotations_052'] = [hero_rotation, run_rotation, hero_run_rotation, old_rotations[3], old_rotations[4]]
        for mesh in bone.get('meshes', []):
            base = copy.deepcopy(mesh['vertices'])
            old_targets = copy.deepcopy(mesh.get('hero_run_vertices_052', [base] * 5))
            hero = copy.deepcopy(base)
            if name.startswith('emotion_dot') or name == 'gradual_head_021':
                hero = old_targets[0]
            run = run_vertices(name, base)
            if name == 'gradual_head_021':
                # New solid swept quills replace the old narrow stroke pieces.
                run = [[0, -4, 2] for _ in base]
                hero_run = copy.deepcopy(run)
            else:
                hero_run = [[run[i][a] + hero[i][a] - base[i][a] for a in range(3)]
                            for i in range(len(base))]
            mesh['hero_run_vertices_052'] = [rounded(hero), rounded(run), rounded(hero_run), old_targets[3], old_targets[4]]
            for face in mesh['faces']:
                if name.startswith(('arm_', 'foot_', 'feet_')):
                    face.pop('hero_run_uv_052', None)
    # Five separate pointed swept-back prongs: a central upper prong and two
    # bilateral pairs. Their roots overlap the cranial shell, not one another.
    specs = [((0,-5.0,1.5), (0,-6.1,4.5), (0,-7.4,7.0), 2.6,1.7,1.8,1.3)]
    for sign in (-1,1):
        specs += [((sign*2.1,-3.8,2.1), (sign*3.7,-3.8,4.8), (sign*5.2,-3.8,7.8), 2.5,1.9,1.7,1.3),
                  ((sign*2.2,-1.4,2.0), (sign*3.7,-.8,4.7), (sign*4.8,.3,6.8), 2.3,1.8,1.4,1.0)]
    quills = []
    for index, spec in enumerate(specs):
        points, faces = closed_quill(*spec)
        # The dormant solid is a point at its real root inside the skull. It
        # unfolds from that attachment instead of travelling out from centre.
        collapsed = [list(spec[0]) for _ in points]
        name = f'baby_run_quill_053_{index}'
        quills.append(name)
        data['bones'].append({'name':name, 'parent':'skull', 'pivot':[0,0,0], 'rotation':[0,0,0],
            'hero_run_pivots_052':[[0,0,0] for _ in range(5)],
            'hero_run_rotations_052':[[0,0,0] for _ in range(5)],
            'meshes':[{'name':'closed_swept_quill', 'material':'official', 'shading':'flat',
                'vertices':collapsed,
                'hero_run_vertices_052':[collapsed, points, points, collapsed, collapsed],
                'faces':faces}]})
    data.setdefault('head_decorations', []).extend(quills + ['hero_run_rear_052'])
    data.setdefault('attachment_extra_bones', {}).setdefault('skull', []).extend(quills)
    data['baby_run_prototype_053'] = {
        'corners':['neutral_baby','hero_baby','neutral_run','hero_run'],
        'independent_axes':True, 'vertex_correspondence':'canonical index-preserving analytic targets',
        'shape_reference':'user-supplied Neutral Run silhouette; Hero remains an independent alignment influence',
        'quills':quills, 'limbs':'connected compact canonical cuboids; no baby stripe UV substitution',
        'natural_two_tone_patches':{'belly_uv':[52.25,28.25]},
        'adult_source_model_unchanged':'gameplay/chao_hero_run_proto052.json'}
    data['authored_gameplay_form'] = 'baby_run_proto053'
    OUT.write_text(json.dumps(data, separators=(',', ':'))+'\n')
    for relative, expected in protected.items():
        assert digest(ASSETS/relative) == expected, 'Protected existing model changed: '+relative
    manifest = {'version':'0.25.53', 'model':str(OUT.relative_to(ASSETS)), 'sha256':digest(OUT),
        'protected_source_sha256':protected, 'adult_model_sha256':digest(SOURCE),
        'corner_order':['neutral_baby','hero_baby','neutral_run','hero_run'],
        'source_mesh_imported':False, 'scope':'baby Run anatomy, independent alignment'}
    (ASSETS/'baby_run_morphology_053.json').write_text(json.dumps(manifest, indent=2)+'\n')
    print('Baby Run 053 asset generated; canonical limb correspondence and adult 052 bytes preserved.')


if __name__ == '__main__':
    author()
