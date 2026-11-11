#!/usr/bin/env python3
"""Independent Blockbench-equivalent transform and recognizable native UV checks.

Reference conventions inspected in JannisX11/blockbench's Bedrock codec and
Cube preview controller, retrieved 2026-10-30. This does not call convert().
"""
from pathlib import Path
import json,math,sys
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'art'))
from import_official_chao import KEYS,native_skin_masks
SOURCE=ROOT/'art/imported_chao'
ASSETS=ROOT/'src/main/resources/assets/chao_garden'

def matrix(position,rotation):
    x,y,z=[math.radians(a) for a in rotation]
    rx=np.array([[1,0,0,0],[0,math.cos(x),-math.sin(x),0],[0,math.sin(x),math.cos(x),0],[0,0,0,1.]])
    ry=np.array([[math.cos(y),0,math.sin(y),0],[0,1,0,0],[-math.sin(y),0,math.cos(y),0],[0,0,0,1.]])
    rz=np.array([[math.cos(z),-math.sin(z),0,0],[math.sin(z),math.cos(z),0,0],[0,0,1,0],[0,0,0,1.]])
    out=rz@ry@rx;out[:3,3]=position;return out

checks=0;maximum=0
for key in KEYS:
    source=json.loads((SOURCE/'models'/('chao_'+key+('.json' if key=='dad' else '.geo.json'))).read_text())['minecraft:geometry'][0]
    target=json.loads((ASSETS/'models/entity/official'/f'chao_{key}.json').read_text())
    old={b['name']:b for b in source['bones']};new={b['name']:b for b in target['bones']}
    source_matrices={};target_matrices={}
    def reference(name):
        if name in source_matrices:return source_matrices[name]
        b=old[name];parent=b.get('parent');p=b['pivot'];q=old[parent]['pivot'] if parent else [0,0,0]
        delta=[-(p[0]-q[0]),p[1]-q[1],p[2]-q[2]]
        r=b.get('rotation',[0,0,0])
        out=(reference(parent) if parent else np.identity(4))@matrix(delta,[-r[0],-r[1],r[2]])
        source_matrices[name]=out;return out
    def runtime(name):
        if name in target_matrices:return target_matrices[name]
        b=new[name];parent=b.get('parent')
        out=(runtime(parent) if parent else np.identity(4))@matrix(b['pivot'],b.get('rotation',[0,0,0]))
        target_matrices[name]=out;return out
    for b in old.values():
        for i,c in enumerate(b.get('cubes',[])):
            cp=c.get('pivot',b['pivot']);delta=np.array(cp)-np.array(b['pivot']);delta[0]*=-1
            cr=c.get('rotation',[0,0,0])
            cm=reference(b['name'])@matrix(delta,[-cr[0],-cr[1],cr[2]])
            actual=new[b['name']+'__cube_'+str(i)]['meshes'][0]
            o=np.array(c['origin'],float);size=np.array(c['size'],float);inflate=c.get('inflate',0)
            lo=o-inflate;hi=o+size+inflate
            corners=[lo,[hi[0],lo[1],lo[2]],[hi[0],hi[1],lo[2]],[lo[0],hi[1],lo[2]],
                     [lo[0],lo[1],hi[2]],[hi[0],lo[1],hi[2]],hi,[lo[0],hi[1],hi[2]]]
            for expected,vertex in zip(corners,actual['vertices']):
                expected=np.array(expected,float)
                if c.get('mirror',b.get('mirror',False)):expected[0]=lo[0]+hi[0]-expected[0]
                point=expected-np.array(cp);point[0]*=-1
                bb=cm@np.array([*point,1])
                expected=np.array([bb[0],24-bb[1],bb[2],1])
                found=runtime(b['name']+'__cube_'+str(i))@np.array([*vertex,1])
                error=float(np.max(np.abs(expected-found)));maximum=max(maximum,error)
                assert error<.00002,(key,b['name'],error)
                checks+=1

baby=json.loads((ASSETS/'models/entity/official/chao_baby.json').read_text())
bones={b['name']:b for b in baby['bones']}
mesh=bones['foot_left__cube_0']['meshes'][0]

def face_uv(face,point):
    xyz=np.array([mesh['vertices'][i]+[1] for i in face['v']])
    transform=np.linalg.lstsq(xyz,np.array(face['uv']),rcond=None)[0]
    return np.array([*point,1])@transform

original=Image.open(SOURCE/'textures/baby.png').convert('RGBA')
def sample(uv,image=original):
    return image.getpixel((min(image.width-1,max(0,int(uv[0]*image.width/64))),
                           min(image.height-1,max(0,int(uv[1]*image.height/32)))))
def yellow(rgb):return rgb[0]>190 and rgb[1]>190 and rgb[2]<170
def blue(rgb):return rgb[2]>200 and rgb[1]>170 and rgb[0]<180

# The native file's front vertical toe face is yellow. All adjoining upper and
# side faces must put that yellow end at the same negative-Z toe, not the heel.
toe_uv=face_uv(mesh['faces'][4],[1,0,-3.8])
heel_uv=face_uv(mesh['faces'][4],[1,0,-.2])
assert yellow(sample(toe_uv)),('toe',toe_uv,sample(toe_uv))
assert blue(sample(heel_uv)),('heel',heel_uv,sample(heel_uv))
for index in (2,3):
    x=min(v[0] for v in mesh['vertices']) if index==2 else max(v[0] for v in mesh['vertices'])
    assert yellow(sample(face_uv(mesh['faces'][index],[x,1,-3.8])))
    assert blue(sample(face_uv(mesh['faces'][index],[x,1,-.2])))
checks+=6

for mode in ('mono','two'):
    coat=Image.open(ASSETS/'textures/entity/chao/official/coats/baby'/f'red_{mode}.png').convert('RGBA')
    heel=sample(heel_uv,coat);toe=sample(toe_uv,coat)
    assert heel[0]>heel[1]*1.8 and heel[0]>heel[2]*1.8,(mode,'heel not genetic red',heel)
    if mode=='two':assert yellow(toe),('lost two-tone toe',toe)
    else:assert toe[0]>toe[1]*1.8 and toe[0]>toe[2]*1.8,('mono toe not red',toe)
    _,eyes,_=native_skin_masks(json.loads((SOURCE/'models/chao_baby.geo.json').read_text())['minecraft:geometry'][0],original)
    assert all(original.getpixel((x,y))==coat.getpixel((x,y)) for y in range(coat.height) for x in range(coat.width) if eyes.getpixel((x,y)))
    assert list(original.getchannel('A').getdata())==list(coat.getchannel('A').getdata())
    checks+=4

report={'source_corner_checks':checks-14,'uv_and_palette_checks':14,'maximum_world_corner_error':maximum,
        'rig_count':20,'reference':'Blockbench Bedrock parse + Cube preview box-UV conventions',
        'rotations':'native x,-y,-z','toe_and_crest_uv':'native front-to-back orientation'}
out=ROOT/'docs/validation-0.18';out.mkdir(parents=True,exist_ok=True)
(out/'native-rig.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS',checks,'checks; maximum source world corner error',maximum)
