#!/usr/bin/env python3
"""Actual shipped duck-eye and ground tambourine contracts; no Java/game launch.

Uses source generators read-only for reproducibility. The numerical geometry
checks are not an in-game visual verdict. Held contact stays covered by the
existing tambourineRigTest and tambourineReplacementRigTest tasks.
"""
from pathlib import Path
import importlib.util
import itertools
import json
import math
import unittest
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
STAGES=('idle','play_0','play_1','play_2','play_3')


def load_generator(name,path):
    spec=importlib.util.spec_from_file_location(name,ROOT/path)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def read_obj(path):
    vertices=[];uv=[];normals=[];faces=[];material=None
    for line in path.read_text().splitlines():
        f=line.split()
        if not f:continue
        if f[0]=='v':vertices.append(tuple(float(x)*16 for x in f[1:]))
        elif f[0]=='vt':uv.append(tuple(float(x) for x in f[1:]))
        elif f[0]=='vn':normals.append(tuple(float(x) for x in f[1:]))
        elif f[0]=='usemtl':material=f[1]
        elif f[0]=='f':faces.append((material,[tuple(int(i)-1 for i in p.split('/')) for p in f[1:]]))
    return vertices,uv,normals,faces


class DuckTambourineAssets084(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.duck=load_generator('duck_assets084','art/build_cwe_content_019.py')
        cls.sa2=load_generator('sa2_assets084','scripts/generate_sa2_assets_02515.py')

    def test_duck_two_exposed_mirrored_eye_cuboids_all_stages(self):
        with Image.open(ASSETS/'textures/block/toy_palette.png') as palette:
            self.assertTrue(all(c<80 for c in palette.getpixel((palette.width*2//16,palette.height*10//16))[:3]))
        for frame,stage in enumerate(STAGES,-1):
            with self.subTest(stage=stage):
                model=json.loads((ASSETS/'models/block'/f'chao_rubber_duck_{stage}.json').read_text())
                eyes=[e for e in model['elements'] if all(f.get('uv')==[0,8,4,12] for f in e['faces'].values())]
                self.assertEqual(len(eyes),2,'Both opposite head sides need their own black surface')
                left,right=sorted(eyes,key=lambda e:e['from'][2])
                self.assertEqual(left['from'][:2],right['from'][:2]);self.assertEqual(left['to'][:2],right['to'][:2])
                self.assertAlmostEqual(left['from'][2],16-right['to'][2]);self.assertAlmostEqual(left['to'][2],16-right['from'][2])
                self.assertLess(left['from'][2],5);self.assertGreater(right['to'][2],11)
                for e,outward in ((left,'north'),(right,'south')):
                    self.assertIn(outward,e['faces']);self.assertNotIn('cullface',e['faces'][outward])
                    self.assertTrue(all(a<b for a,b in zip(e['from'],e['to'])))
                corners=[p for e in model['elements'] for p in itertools.product(*zip(e['from'],e['to']))]
                self.assertEqual([min(p[a] for p in corners) for a in range(3)],[1,1+(0,.5,1,.5)[max(0,frame)] if frame>=0 else 1,3])
                self.assertEqual([max(p[a] for p in corners) for a in range(3)],[16,10+(0,.5,1,.5)[max(0,frame)] if frame>=0 else 10,13])
                self.assertEqual(model,self.duck.toy_model('chao_rubber_duck',frame>=0,max(0,frame)))

    def test_tambourine_idle_decorated_face_up_and_floor_contact(self):
        vertices,uv,normals,faces=read_obj(ASSETS/'models/sa2_02515/chao_tambourine_idle.obj')
        self.assertEqual(len(vertices),408)
        self.assertTrue(all(math.isfinite(c) for p in vertices+uv+normals for c in p))
        skin=[face for mat,face in faces if mat=='sa2_star01_52']
        self.assertEqual(len(skin),4)  # Two source triangles retain their reverse-wound back faces.
        ys=[vertices[v][1] for f in skin for v,_,_ in f]
        self.assertLess(max(ys)-min(ys),.00001,'Decorated drumhead must lie horizontally')
        self.assertEqual(sum(normals[face[0][2]][1]>.99999 for face in skin),2,'Both decorated source triangles need an upward-facing side')
        for face in skin:
            for _,_,n in face:self.assertGreater(abs(normals[n][1]),.99999,'Drumhead normal must remain vertical')
        self.assertAlmostEqual(min(p[1] for p in vertices),.08,places=5)
        self.assertLess(max(p[1] for p in vertices)-min(p[1] for p in vertices),2.31)
        self.assertTrue(all(3<=p[0]<=13 and 3<=p[2]<=13 for p in vertices))

    def test_tambourine_all_block_poses_rigid_grounded_and_generator_replay(self):
        rest=read_obj(ASSETS/'models/sa2_02515/chao_tambourine_idle.obj')
        for frame,stage in enumerate(STAGES,-1):
            with self.subTest(stage=stage):
                model=json.loads((ASSETS/'models/block'/f'chao_tambourine_{stage}.json').read_text())
                self.assertEqual(model['loader'],'neoforge:obj')
                self.assertNotIn('transform',model);self.assertNotIn('transforms',model)
                vertices,uv,normals,faces=read_obj(ASSETS/model['model'].split(':',1)[1])
                self.assertEqual(faces,rest[3]);self.assertEqual(uv,rest[1])
                self.assertAlmostEqual(min(p[1] for p in vertices),.08,places=5)
                self.assertTrue(all(3<=p[0]<=13 and 0<=p[1]<=8 and 3<=p[2]<=13 for p in vertices),'Existing collision envelope still contains the complete ground pose')
                for _,face in faces:
                    for i in range(len(face)):
                        a=face[i][0];b=face[(i+1)%len(face)][0]
                        self.assertAlmostEqual(math.dist(vertices[a],vertices[b]),math.dist(rest[0][a],rest[0][b]),places=4,msg='Ground rocking must rotate, not resize individual frames')
                expected=[p for face,_,_ in self.sa2.tambourine_ground_pose(frame).faces for p in face]
                self.assertEqual(len(vertices),len(expected))
                self.assertTrue(all(abs(a-b)<.000002 for p,q in zip(vertices,expected) for a,b in zip(p,q)),'Targeted/full generator and actual OBJ differ')
        states=json.loads((ASSETS/'blockstates/chao_tambourine.json').read_text())['variants']
        self.assertEqual(len(states),32)
        for key,value in states.items():
            props=dict(x.split('=') for x in key.split(','));stage='play_'+props['frame'] if props['playing']=='true' else 'idle'
            self.assertEqual(value['model'],'chao_garden:block/chao_tambourine_'+stage)
            self.assertIn(value.get('y',0),(0,90,180,270))

    def test_held_tambourine_path_is_separate_and_originally_tilted(self):
        root=json.loads((ASSETS/'models/item/chao_tambourine.json').read_text())
        self.assertEqual(root['base']['parent'],'chao_garden:item/sa2_02515/chao_tambourine')
        held=json.loads((ASSETS/'models/item/sa2_02515/chao_tambourine.json').read_text())
        self.assertEqual(held['model'],'chao_garden:models/sa2_02515/chao_tambourine.obj')
        _,_,normals,faces=read_obj(ASSETS/'models/sa2_02515/chao_tambourine.obj')
        for mat,face in faces:
            if mat=='sa2_star01_52':
                for _,_,n in face:self.assertAlmostEqual(abs(normals[n][1]),.6693578,places=5,msg='Ground adaptation must not rotate the existing hand geometry')
        animation=(ROOT/'src/main/java/com/chaogarden/client/ChaoTambourineAnimation.java').read_text()
        self.assertIn('ItemDisplayContext.NONE',animation)
        self.assertIn('TambourinePlayRules.KIND',animation)


if __name__=='__main__':unittest.main(verbosity=2)
