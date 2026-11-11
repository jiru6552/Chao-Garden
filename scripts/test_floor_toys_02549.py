"""Independent source, grounding and inventory-preservation checks for floor layouts.

These are asset checks, not a Minecraft visual or collision acceptance pass.
Run from the project root: python3 scripts/test_floor_toys_02549.py
"""
from pathlib import Path
import hashlib
import json
import math
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT/'src/main/resources/assets/chao_garden'
DATA = json.loads((ASSETS/'models/floor02549/manifest.json').read_text())
TOYS = ('accordion','guitar','triangle','bubble_wand','castanets')
STAGES = ('idle','play_0','play_1','play_2','play_3')
ENVELOPES = {
    'accordion': (3.9,0,3.82,12.1,2.25,12.18),
    'guitar': (6.23,0,2.77,9.77,.73,13.23),
    'triangle': (2,0,2,12.9,1,14),
    'bubble_wand': (6.67,0,3.04,9.33,.51,12.96),
    'castanets': (4.05,0,4.56,11.95,3.1,11.44),
}
HEIGHTS = {'accordion':2.25,'guitar':.73,'triangle':1,'bubble_wand':.51,'castanets':3.1}


def read_obj(path):
    vertices, uv, normals, faces = [], [], [], []
    material = ''; library = ''
    for line in path.read_text().splitlines():
        p = line.split()
        if not p: continue
        if p[0] == 'v': vertices.append(tuple(float(x) for x in p[1:]))
        elif p[0] == 'vt': uv.append(tuple(float(x) for x in p[1:]))
        elif p[0] == 'vn': normals.append(tuple(float(x) for x in p[1:]))
        elif p[0] == 'usemtl': material = p[1]
        elif p[0] == 'mtllib': library = p[1]
        elif p[0] == 'f':
            indices = [tuple(int(n)-1 for n in item.split('/')) for item in p[1:]]
            faces.append((material,[vertices[i[0]] for i in indices],[uv[i[1]] for i in indices],[normals[i[2]] for i in indices]))
    return vertices,faces,library


def bounds(points):
    return tuple(min(p[a] for p in points) for a in range(3))+tuple(max(p[a] for p in points) for a in range(3))
def subtract(a,b): return tuple(x-y for x,y in zip(a,b))
def cross(a,b): return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def dot(a,b): return sum(x*y for x,y in zip(a,b))
def length(a): return math.sqrt(dot(a,a))
def rotate(point, turns):
    x,y,z = point
    return ((x,y,z),(1-z,y,x),(1-x,y,1-z),(z,y,1-x))[turns]
def rotate_box(b, turns):
    x0,y0,z0,x1,y1,z1=b
    return ((x0,y0,z0,x1,y1,z1),(16-z1,y0,x0,16-z0,y1,x1),
            (16-x1,y0,16-z1,16-x0,y1,16-z0),(z0,y0,16-x1,z1,y1,16-x0))[turns]


class FloorToys(unittest.TestCase):
    def test_all_world_frames_use_only_ground_layouts(self):
        for toy in TOYS:
            for stage in STAGES:
                with self.subTest(toy=toy,stage=stage):
                    model=json.loads((ASSETS/f'models/block/chao_{toy}_{stage}.json').read_text())
                    self.assertEqual(model['model'],f'chao_garden:models/floor02549/{toy}.obj')
                    self.assertEqual(model['loader'],'neoforge:obj')
                    self.assertEqual(model['render_type'],'minecraft:cutout')
                    self.assertFalse(model['automatic_culling'])

    def test_original_held_and_item_sources_are_byte_identical(self):
        self.assertGreaterEqual(len(DATA['protected_sha256']),46)
        for name, expected in DATA['protected_sha256'].items():
            with self.subTest(name=name):
                self.assertEqual(hashlib.sha256((ASSETS/name).read_bytes()).hexdigest(),expected)
        wrapper=json.loads((ASSETS/'models/item/chao_triangle.json').read_text())
        self.assertEqual(wrapper,{'parent':'chao_garden:item/chao_triangle_display'})
        display=json.loads((ASSETS/'models/item/chao_triangle_display.json').read_text())
        self.assertEqual(len(display['elements']),5)
        self.assertEqual(display['display']['gui']['rotation'],[30,225,0])
        self.assertEqual(display['elements'][4]['from'],[4,1,10])
        self.assertEqual(display['elements'][4]['to'],[12,1.6,10.6])

    def test_actual_meshes_contact_the_surface_and_stay_flat(self):
        for toy in TOYS:
            with self.subTest(toy=toy):
                points,faces,_=read_obj(ASSETS/f'models/floor02549/{toy}.obj')
                b=bounds(points)
                self.assertAlmostEqual(b[1],0,places=8)
                self.assertTrue(all(p[1]>=-1e-8 for p in points))
                self.assertGreater(sum(abs(p[1])<1e-8 for p in points),0)
                self.assertLessEqual(b[4]*16,HEIGHTS[toy]+.00001)
                self.assertLess(b[4]-b[1],max(b[3]-b[0],b[5]-b[2])*.42)
                self.assertEqual(len(faces),DATA['toys'][toy]['faces'])

    def test_four_facings_fit_reviewed_collision_envelopes(self):
        for toy in TOYS:
            points,_,_=read_obj(ASSETS/f'models/floor02549/{toy}.obj')
            state=json.loads((ASSETS/f'blockstates/chao_{toy}.json').read_text())['variants']
            for turns,facing in enumerate(('north','east','south','west')):
                with self.subTest(toy=toy,facing=facing):
                    mesh=bounds([tuple(c*16 for c in rotate(p,turns)) for p in points])
                    box=rotate_box(ENVELOPES[toy],turns)
                    for axis in range(3):
                        self.assertLessEqual(box[axis],mesh[axis]+1e-6)
                        self.assertGreaterEqual(box[axis+3],mesh[axis+3]-1e-6)
                    self.assertAlmostEqual(mesh[1],0,places=7)
                    matching=[v for k,v in state.items() if k.startswith('facing='+facing+',')]
                    self.assertTrue(matching)
                    for model in matching: self.assertEqual(model.get('y',0),turns*90)

    def test_source_topology_uvs_and_rigid_shapes_are_preserved(self):
        for toy in ('accordion','guitar','bubble_wand','castanets'):
            with self.subTest(toy=toy):
                record=DATA['toys'][toy]
                original=[f for path in record['sources'] for f in read_obj(ASSETS/path)[1]]
                _,current,_=read_obj(ASSETS/f'models/floor02549/{toy}.obj')
                self.assertEqual(len(original),len(current))
                scale=record['transforms'][0]['scale']
                for before,after in zip(original,current):
                    self.assertEqual(before[0],after[0])
                    self.assertEqual(len(before[1]),len(after[1]))
                    for a,b in zip(before[2],after[2]):
                        for old,new in zip(a,b):self.assertAlmostEqual(old,new,places=8)
                    for i in range(len(before[1])):
                        self.assertAlmostEqual(length(subtract(before[1][i],before[1][i-1]))*scale,
                                               length(subtract(after[1][i],after[1][i-1])),places=7)

    def test_uv_materials_normals_and_source_faces_are_valid(self):
        for toy in TOYS:
            _,faces,library=read_obj(ASSETS/f'models/floor02549/{toy}.obj')
            material_text=(ASSETS/library.split(':',1)[1]).read_text()
            model=json.loads((ASSETS/f'models/block/chao_{toy}_idle.json').read_text())
            for material,points,_,normals in faces:
                self.assertIn('newmtl '+material+'\n',material_text)
                resource=model['textures'][material]
                self.assertTrue((ASSETS/'textures'/(resource.split(':',1)[1]+'.png')).is_file())
                face_normal=cross(subtract(points[1],points[0]),subtract(points[2],points[0]))
                self.assertGreater(length(face_normal),1e-11)
                for n in normals:
                    self.assertAlmostEqual(length(n),1,places=5)
                    self.assertGreater(dot(n,face_normal)/length(face_normal),.9999)

    def test_guitar_strings_and_accordion_keys_face_up(self):
        # Independently recorded source-facing axes from measured source faces.
        fronts={'guitar':(-.0257916,-.7233867,.6899612),'accordion':(.8329784336,-.2746999560,.4802987230)}
        for toy,front in fronts.items():
            m=DATA['toys'][toy]['transforms'][0]['matrix']
            result=tuple(dot(row,front) for row in m)
            self.assertGreater(result[1],.99999)
            self.assertLess(abs(result[0]),.00001);self.assertLess(abs(result[2]),.00001)
            _,faces,_=read_obj(ASSETS/f'models/floor02549/{toy}.obj')
            self.assertTrue(any(n[1]>.999 for _,_,_,normals in faces for n in normals))

    def test_castanet_star_is_the_top_closed_shell(self):
        _,faces,_=read_obj(ASSETS/'models/floor02549/castanets.obj')
        star=[p for material,points,_,_ in faces if 'star_yel' in material for p in points]
        wood=[p for material,points,_,_ in faces if 'kasuta01' in material for p in points]
        self.assertTrue(star and wood)
        self.assertGreater(min(p[1] for p in star),max(p[1] for p in wood))

    def test_triangle_beater_is_flat_beside_the_triangle(self):
        _,faces,_=read_obj(ASSETS/'models/floor02549/triangle.obj')
        self.assertEqual(len(faces),30)
        body=bounds([p for _,points,_,_ in faces[:24] for p in points])
        beater=bounds([p for _,points,_,_ in faces[24:] for p in points])
        self.assertAlmostEqual(body[1],0,places=7);self.assertAlmostEqual(beater[1],0,places=7)
        self.assertGreater(beater[0],body[3]+.1)
        self.assertLess(beater[4],1/16)


if __name__ == '__main__': unittest.main(verbosity=2)
