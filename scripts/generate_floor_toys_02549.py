"""Ground-only layouts for five existing toys; held and inventory meshes stay intact.

The supplied SA2/CWE surfaces are reused with rigid rotations and translations.
Guitar strings, accordion keys and the castanet star face upward. The triangle
and its beater are laid next to each other. No animation presenter uses these
new OBJ files: only the five ordinary block model families are redirected.
"""
from pathlib import Path
import copy
import hashlib
import json
import math

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
DEST = ASSETS / 'models/floor02549'
TOYS = ('accordion', 'guitar', 'triangle', 'bubble_wand', 'castanets')
STAGES = ('idle', 'play_0', 'play_1', 'play_2', 'play_3')


def dot(a, b): return sum(x*y for x, y in zip(a, b))
def cross(a, b): return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
def unit(a):
    length = math.sqrt(dot(a, a))
    return tuple(v/length for v in a)
def mul(matrix, point): return tuple(dot(row, point) for row in matrix)
def bounds(faces):
    points = [p for f in faces for p in f['points']]
    return [min(p[i] for p in points) for i in range(3)] + [max(p[i] for p in points) for i in range(3)]


def obj_faces(path):
    vertices, uvs, normals, faces = [], [], [], []
    material = ''
    for line in path.read_text().splitlines():
        words = line.split()
        if not words: continue
        if words[0] == 'v': vertices.append(tuple(map(float, words[1:])))
        elif words[0] == 'vt': uvs.append(tuple(map(float, words[1:])))
        elif words[0] == 'vn': normals.append(tuple(map(float, words[1:])))
        elif words[0] == 'usemtl': material = words[1]
        elif words[0] == 'f':
            ids = [tuple(int(i)-1 for i in w.split('/')) for w in words[1:]]
            faces.append({'material': material, 'points': [vertices[i[0]] for i in ids],
                          'uv': [uvs[i[1]] for i in ids], 'normals': [normals[i[2]] for i in ids]})
    if not faces: raise ValueError('No source surfaces: '+str(path))
    return faces


def element_faces(elements):
    result = []
    for element in elements:
        x0, y0, z0 = element['from']; x1, y1, z1 = element['to']
        sides = {
            'north': [(x1,y0,z0),(x0,y0,z0),(x0,y1,z0),(x1,y1,z0)],
            'south': [(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)],
            'west': [(x0,y0,z0),(x0,y0,z1),(x0,y1,z1),(x0,y1,z0)],
            'east': [(x1,y0,z1),(x1,y0,z0),(x1,y1,z0),(x1,y1,z1)],
            'down': [(x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1)],
            'up': [(x0,y1,z1),(x1,y1,z1),(x1,y1,z0),(x0,y1,z0)],
        }
        for side, data in element['faces'].items():
            points = sides[side]
            rotation = element.get('rotation')
            if rotation:
                axis = 'xyz'.index(rotation['axis']); i, j = ((1,2),(2,0),(0,1))[axis]
                c, s = math.cos(math.radians(rotation['angle'])), math.sin(math.radians(rotation['angle']))
                transformed = []
                for point in points:
                    q = [point[a]-rotation['origin'][a] for a in range(3)]
                    q[i], q[j] = q[i]*c-q[j]*s, q[i]*s+q[j]*c
                    if rotation.get('rescale'): q[i] /= c; q[j] /= c
                    transformed.append(tuple((q[a]+rotation['origin'][a])/16 for a in range(3)))
                points = transformed
            else: points = [tuple(v/16 for v in point) for point in points]
            a = tuple(points[1][i]-points[0][i] for i in range(3))
            b = tuple(points[2][i]-points[0][i] for i in range(3))
            normal = unit(cross(a, b))
            u0, v0, u1, v1 = data['uv']
            uv = [(u0/16,1-v1/16),(u1/16,1-v1/16),(u1/16,1-v0/16),(u0/16,1-v0/16)]
            result.append({'material': data['texture'].lstrip('#'), 'points': points,
                           'uv': uv, 'normals': [normal]*4})
    return result


def placed(faces, matrix, scale=1, center=(.5,.5)):
    """Rigidly orient, uniformly size, then put the lowest authored surface at y=0."""
    out = copy.deepcopy(faces)
    for face in out:
        face['points'] = [tuple(v*scale for v in mul(matrix, p)) for p in face['points']]
        face['normals'] = [unit(mul(matrix, n)) for n in face['normals']]
    b = bounds(out); offset = (center[0]-(b[0]+b[3])/2, -b[1], center[1]-(b[2]+b[5])/2)
    for face in out: face['points'] = [tuple(p[i]+offset[i] for i in range(3)) for p in face['points']]
    return out, {'matrix': matrix, 'scale': scale, 'offset': offset}


def write_obj(name, faces, material_library):
    lines = ['# Block-only flat floor layout 0.25.49; original source surfaces and UVs.',
             'mtllib chao_garden:models/'+material_library+'/materials.mtl']
    offset = 1
    for face in faces:
        lines.append('usemtl '+face['material'])
        lines.extend('v '+' '.join(f'{v:.9f}' for v in p) for p in face['points'])
        lines.extend('vt '+' '.join(f'{v:.9f}' for v in p) for p in face['uv'])
        lines.extend('vn '+' '.join(f'{v:.9f}' for v in p) for p in face['normals'])
        lines.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(offset,offset+len(face['points']))))
        offset += len(face['points'])
    path = DEST/(name+'.obj'); path.write_text('\n'.join(lines)+'\n')


def protected_paths():
    paths = set()
    for toy in TOYS:
        for folder in ('sa2_02515','cwe02516'):
            paths.update((ASSETS/'models'/folder).glob('chao_'+toy+'*.obj'))
            paths.update((ASSETS/'models/item'/folder).glob('chao_'+toy+'*.json'))
        paths.update((ASSETS/'models/item').glob('chao_'+toy+'_hand_*.json'))
        if toy != 'triangle': paths.add(ASSETS/'models/item'/('chao_'+toy+'.json'))
    paths.update((ASSETS/'models/accordion02548').glob('*'))
    return sorted(p for p in paths if p.is_file())


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    (DEST/'materials.mtl').write_text('newmtl palette\nKd 1.0 1.0 1.0\nmap_Kd #palette\n')
    manifest_path = DEST/'manifest.json'
    previous = json.loads(manifest_path.read_text()) if manifest_path.exists() else None
    if previous:
        for name, expected in previous['protected_sha256'].items():
            if hashlib.sha256((ASSETS/name).read_bytes()).hexdigest() != expected:
                raise ValueError('Held/item source changed since the floor-only pass: '+name)
    # Triangle's original item shared the world model. Preserve that exact model
    # before the world model is redirected; its held parts are already separate.
    triangle_display = ASSETS/'models/item/chao_triangle_display.json'
    if not triangle_display.exists():
        original = ASSETS/'models/block/chao_triangle_idle.json'
        if 'elements' not in json.loads(original.read_text()): raise ValueError('Missing original triangle display')
        triangle_display.write_bytes(original.read_bytes())
    triangle = json.loads(triangle_display.read_text())
    (ASSETS/'models/item/chao_triangle.json').write_text(json.dumps({'parent':'chao_garden:item/chao_triangle_display'},indent=2)+'\n')

    records = {}
    for name in TOYS:
        template_path = ASSETS/'models/block'/('chao_'+name+'_idle.json')
        template = json.loads(template_path.read_text())
        transforms = []
        if name == 'triangle':
            body, transform = placed(element_faces(triangle['elements'][:4]), ((1,0,0),(0,0,-1),(0,1,0)), center=(6/16,.5))
            stick, stick_transform = placed(element_faces(triangle['elements'][4:]), ((0,1,0),(0,0,-1),(-1,0,0)), center=(12.6/16,.5))
            faces = body+stick; transforms = [transform,stick_transform]
            sources = ['models/item/chao_triangle_display.json']
            textures = triangle['textures']; library = 'floor02549'
            note = 'Existing palette triangle and beater, laid side by side; original item geometry retained separately.'
        else:
            library = 'cwe02516' if name in ('accordion','guitar') else 'sa2_02515'
            source = ASSETS/'models'/library/('chao_'+name+'.obj')
            if name == 'guitar':
                neck = unit((.92050569,.25199345,.29861109))
                front = (-.0257916,-.7233867,.6899612)
                front = unit(tuple(front[i]-dot(front,neck)*neck[i] for i in range(3)))
                across = cross(front,neck)
                matrix = (tuple(-v for v in across),front,tuple(-v for v in neck)); scale = 5/7
                source_faces = obj_faces(source); sources = [str(source.relative_to(ASSETS))]
                note = 'Original CWE guitar; measured outward string face up and neck along the floor toward north.'
            elif name == 'accordion':
                expansion = unit((.5529550825,.3824004520,-.7402773609))
                front = (.8329784336,-.2746999560,.4802987230)
                front = unit(tuple(front[i]-dot(front,expansion)*expansion[i] for i in range(3)))
                matrix = (expansion,front,cross(expansion,front)); scale = .575
                source_faces = obj_faces(source); sources = [str(source.relative_to(ASSETS))]
                note = 'Original fan-open CWE accordion, lying on its back with the actual key fronts facing upward.'
            elif name == 'bubble_wand':
                matrix = ((1,0,0),(0,0,1),(0,-1,0)); scale = .70
                source_faces = obj_faces(source); sources = [str(source.relative_to(ASSETS))]
                note = 'Original SA2 wand, rotated exactly flat with the ring toward north.'
            else:
                matrix = ((1,0,0),(0,-1,0),(0,0,-1)); scale = .55
                sources = ['models/sa2_02515/chao_castanets_hand_base.obj','models/sa2_02515/chao_castanets_hand_lid.obj']
                source_faces = [f for path in sources for f in obj_faces(ASSETS/path)]
                note = 'Existing rigid SA2 shell adaptations, closed and resting with the star-decorated lid upward.'
            faces, transform = placed(source_faces,matrix,scale); transforms = [transform]
            textures = template['textures']
        write_obj(name,faces,library)
        model = {'loader':'neoforge:obj','model':'chao_garden:models/floor02549/'+name+'.obj',
                 'automatic_culling':False,'shade_quads':True,'flip_v':True,'textures':textures,'render_type':'minecraft:cutout'}
        if name == 'triangle': model['ambientocclusion'] = False
        for stage in STAGES:
            (ASSETS/'models/block'/('chao_'+name+'_'+stage+'.json')).write_text(json.dumps(model,indent=2)+'\n')
        b = bounds(faces)
        # Round outwards at .01 pixel so collision never clips the generated mesh.
        pixels = [v*16 for v in b]
        envelope = [math.floor(v*100+1e-6)/100 if i<3 else math.ceil(v*100-1e-6)/100 for i,v in enumerate(pixels)]
        records[name] = {'sources':sources,'faces':len(faces),'bounds_blocks':b,'collision_envelope_pixels':envelope,
                         'transforms':transforms,'reference':note}
    protected = previous['protected_sha256'] if previous else {
        str(p.relative_to(ASSETS)):hashlib.sha256(p.read_bytes()).hexdigest() for p in protected_paths()}
    protected['models/item/chao_triangle_display.json'] = hashlib.sha256(triangle_display.read_bytes()).hexdigest()
    manifest = {'version':'0.25.49','toys':records,'protected_sha256':protected,
                'scope':'Only block model families use these layouts. Item and actual held-animation source meshes/transforms are preserved.'}
    manifest_path.write_text(json.dumps(manifest,indent=2)+'\n')
    for name, record in records.items(): print(name,record['collision_envelope_pixels'])


if __name__ == '__main__': main()
