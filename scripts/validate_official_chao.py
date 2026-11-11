#!/usr/bin/env python3
"""Check imported rig fidelity against its asset-only Bedrock authoring inputs."""
from pathlib import Path
import json,math,hashlib,sys
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'art'))
from import_official_chao import KEYS
ART=ROOT/'art/imported_chao';RES=ROOT/'src/main/resources/assets/chao_garden'

def require(v,msg):
 if not v:raise AssertionError(msg)
def close(a,b,msg):require(len(a)==len(b) and all(abs(x-y)<1e-5 for x,y in zip(a,b)),msg)
checks=0;counts={'bones':0,'cuboids':0,'mirrored_cuboids':0,'inflated_cuboids':0,'zero_thickness_cuboids':0,'faces':0}
for key in KEYS:
 original=ART/'models'/('chao_'+key+('.json' if key=='dad' else '.geo.json'))
 source=json.loads(original.read_text())['minecraft:geometry'][0]
 target=json.loads((RES/'models/entity/official'/f'chao_{key}.json').read_text());bones={b['name']:b for b in target['bones']};old={b['name']:b for b in source['bones']}
 require([target['texture_width'],target['texture_height']]==[source['description']['texture_width'],source['description']['texture_height']],key+' logical UV')
 for b in source['bones']:
  name=b['name'];actual=bones[name];parent=b.get('parent');p=b['pivot'];r=b.get('rotation',[0,0,0]);q=old[parent]['pivot'] if parent else [0,0,0]
  close(actual['pivot'],[-(p[0]-q[0]),-(p[1]-q[1])+(24 if parent is None else 0),p[2]-q[2]],key+':'+name+' parent pivot')
  close(actual['rotation'],[r[0],-r[1],-r[2]],key+':'+name+' Blockbench-equivalent rotation')
  require(actual['parent']==parent,key+':'+name+' parent')
  checks+=3;counts['bones']+=1
  for i,c in enumerate(b.get('cubes',[])):
   part=bones[name+'__cube_'+str(i)];cp=c.get('pivot',p);cr=c.get('rotation',[0,0,0]);size=c['size'];infl=c.get('inflate',0);mirror=c.get('mirror',b.get('mirror',False));mesh=part['meshes'][0]
   close(part['pivot'],[-(cp[0]-p[0]),-(cp[1]-p[1]),cp[2]-p[2]],key+' cube pivot')
   close(part['rotation'],[cr[0],-cr[1],-cr[2]],key+' cube rotation')
   # Independently compare the pre-rotation cuboid extrema, including inflation.
   origin=c['origin'];lo=[origin[j]-infl for j in range(3)];hi=[origin[j]+size[j]+infl for j in range(3)]
   expected_min=[-hi[0]+cp[0],-hi[1]+cp[1],lo[2]-cp[2]]
   expected_max=[-lo[0]+cp[0],-lo[1]+cp[1],hi[2]-cp[2]]
   close([min(v[j] for v in mesh['vertices']) for j in range(3)],expected_min,key+' cube minimum')
   close([max(v[j] for v in mesh['vertices']) for j in range(3)],expected_max,key+' cube maximum')
   close(mesh['vertices'][0],[-(hi[0] if mirror else lo[0])+cp[0],-lo[1]+cp[1],lo[2]-cp[2]],key+' mirrored winding vertex')
   dims=[x+2*infl for x in size];expected_faces=2*sum(dims[(j+1)%3]*dims[(j+2)%3]>1e-8 for j in range(3))
   require(len(mesh['faces'])==expected_faces,key+' nondegenerate source faces')
   u,v=c['uv'];w,h,d=size
   for face in mesh['faces']:
    require(len(face['v'])==len(face['uv'])==4,key+' quad shape')
    require(all(u-1e-5<=uv[0]<=u+2*(w+d)+1e-5 and v-1e-5<=uv[1]<=v+d+h+1e-5 for uv in face['uv']),key+' source box-UV extents')
    a,bv,z=[mesh['vertices'][n] for n in face['v'][:3]]
    ab=[bv[j]-a[j] for j in range(3)];ac=[z[j]-a[j] for j in range(3)]
    norm=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
    require(sum(n*n for n in norm)>1e-10,key+' nondegenerate normal')
    checks+=3;counts['faces']+=1
   checks+=6;counts['cuboids']+=1;counts['mirrored_cuboids']+=bool(mirror);counts['inflated_cuboids']+=infl!=0;counts['zero_thickness_cuboids']+=0 in size
 source_tex=ART/'textures'/f'{key}.png';output_tex=RES/'textures/entity/chao/official'/f'{key}.png'
 require(source_tex.read_bytes()==output_tex.read_bytes(),key+' exact source texture bytes')
 with Image.open(output_tex) as image:image.verify()
 require(len(target['bones'])==len(source['bones'])+sum(len(b.get('cubes',[])) for b in source['bones']),key+' no source bone/cube lost')
 checks+=3
animation=ART/'animations/chao.animation.json';output=RES/'animations/official/chao.animation.json'
require(animation.read_bytes()==output.read_bytes(),'Exact source animation bytes')
clips=json.loads(output.read_text())['animations'];require(len(clips)==8,'Eight animation clips')
for name,length in [('idle',1.375),('walk',1),('happy',1.58333),('sad',1.5),('alert',.75),('no_emotion',.52)]:
 require(abs(clips['animation.chao.'+name]['animation_length']-length)<1e-5,'Declared clip timing '+name);checks+=1
require(not any(p.suffix in ('.ent','.key') or p.name in ('contents.json','options.txt') for p in ART.rglob('*')),'Only model/texture/animation authoring assets')
print(f'PASS: {len(KEYS)} imported rigs; {checks} source-parity checks; '+', '.join(str(v)+' '+k for k,v in counts.items())+'; exact 20 texture files and eight-clip animation source.')
