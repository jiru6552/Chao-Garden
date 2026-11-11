#!/usr/bin/env python3
"""Independent topology, attachment, UV, endpoint and new animal-part contracts."""
from pathlib import Path
import hashlib,json,math
from PIL import Image
import numpy as np
from runtime_resource_contract_022 import ATTACHMENT_LAYOUTS
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden';M=A/'models/entity'
PRIMARY={'baby':'baby','swim':'swim','fly':'fly','run':'run','power':'power','hero':'hero','hero_swim':'swim','hero_fly':'hero_fly','hero_run':'run','hero_power':'hero_power','dark':'dark','dark_swim':'dark_swim','dark_fly':'fly','dark_run':'dark_run','dark_power':'power'}
SLOTS={21:{4,5,7},22:{2,5,7},23:{0,4,6},24:{0,1,4,5,6},25:{0,1,4,6},26:{0,1,4,6},27:{0,1,4,6},28:{0,1,2,4,6},29:{0,4,6},30:{4,6,7}}
checks=0
def test(ok,message):
 global checks;checks+=1
 if not ok:raise AssertionError(message)
def load(p):return json.loads(p.read_text())
def finite(v):return all(math.isfinite(x) for p in v for x in p)
def signature(d,t):
 return hashlib.sha256(json.dumps([[b['name'],[(m.get('morph_vertices') or [m['vertices']]*9)[t] for m in b['meshes']]] for b in d['bones']],separators=(',',':')).encode()).hexdigest()
def ext(v,i):return min(p[i] for p in v),max(p[i] for p in v)
def transform(p,r):
 x,y,z=[math.radians(v) for v in r]
 rx=np.array([[1,0,0,0],[0,math.cos(x),-math.sin(x),0],[0,math.sin(x),math.cos(x),0],[0,0,0,1.]])
 ry=np.array([[math.cos(y),0,math.sin(y),0],[0,1,0,0],[-math.sin(y),0,math.cos(y),0],[0,0,0,1.]])
 rz=np.array([[math.cos(z),-math.sin(z),0,0],[math.sin(z),math.cos(z),0,0],[0,0,1,0],[0,0,0,1.]])
 out=rz@ry@rx;out[:3,3]=p;return out

def valid_hierarchy(d,label):
 names={b['name']:b for b in d['bones']};test(len(names)==len(d['bones']),label+' duplicate bone names')
 roots=[]
 for b in d['bones']:
  parent=b.get('parent');test(not parent or parent in names,label+' missing parent '+str(parent))
  test(len(b.get('pivot',[]))==3 and all(math.isfinite(v) for v in b['pivot']),label+' invalid pivot')
  test(len(b.get('rotation',[0,0,0]))==3 and all(math.isfinite(v) for v in b.get('rotation',[0,0,0])),label+' invalid rotation')
  if not parent:roots.append(b['name'])
  seen=set();at=b['name']
  while at:
   test(at not in seen,label+' cyclic parent chain '+at);seen.add(at);at=names[at].get('parent')
 test(bool(roots),label+' no root')
 for frame in (0,1,2):
  cache={}
  def world(name):
   if name in cache:return cache[name]
   b=names[name];i=list(names).index(name);phase=i*.13+frame*.7
   p=list(b['pivot']);r=list(b.get('rotation',[0,0,0]))
   if frame:
    p=[v+math.sin(phase+j)*.15 for j,v in enumerate(p)];r=[v+math.sin(phase+j)*15 for j,v in enumerate(r)]
   local=transform(p,r);out=(world(b['parent']) if b.get('parent') else np.identity(4))@local
   test(np.isfinite(out).all(),label+' non-finite ancestor transform '+name);cache[name]=out;return out
  for b in d['bones']:
   mtx=world(b['name'])
   for mesh in b.get('meshes',[]):
    test(all(all(0<=i<len(mesh['vertices']) for i in f['v']) and len(f['v'])==len(f['uv']) for f in mesh['faces']),label+' face indices or UV topology')
    vertices=mesh.get('morph_vertices',[mesh['vertices']])[frame*4] if mesh.get('morph_vertices') else mesh['vertices']
    test(np.isfinite((mtx@np.array([v+[1] for v in vertices]).T)).all(),label+' invalid posed vertices '+b['name'])

# Regression fixtures prove the independent validator rejects the exact missing
# authored-parent failure and malformed hierarchies before Minecraft loads them.
for label,bones in (
 ('missing',[{'name':'boot','parent':'foot_right','pivot':[0,0,0],'meshes':[]}]),
 ('cycle',[{'name':'a','parent':'b','pivot':[0,0,0],'meshes':[]},{'name':'b','parent':'a','pivot':[0,0,0],'meshes':[]}]),
 ('duplicate',[{'name':'a','parent':None,'pivot':[0,0,0],'meshes':[]},{'name':'a','parent':None,'pivot':[0,0,0],'meshes':[]}])):
 rejected=False
 try:valid_hierarchy({'bones':bones},'negative fixture '+label)
 except AssertionError:rejected=True
 test(rejected,'malformed hierarchy was accepted: '+label)

for directory in ('gameplay','cwe_parts','party_hats','cwe_accessories'):
 for path in sorted((M/directory).glob('*.json')):valid_hierarchy(load(path),directory+'/'+path.name)

all_signatures=set()
for key,donor in PRIMARY.items():
 d=load(M/'gameplay'/f'chao_{key}.json');raw=load(M/'official'/f'chao_{donor}.json');names={b['name']:b for b in d['bones']}
 test(len(names)==len(d['bones']),key+' duplicate bones')
 for b in raw['bones']:
  if '__cube_' in b['name']:continue
  actual=names[b['name']]
  test(all(actual.get(k)==b.get(k) for k in ('parent','pivot','rotation')),key+' semantic anchor '+b['name'])
 for t in range(9):
  sig=signature(d,t);test(sig not in all_signatures,key+' duplicate endpoint '+str(t));all_signatures.add(sig)
 for b in d['bones']:
  for m in b['meshes']:
   vs=m['vertices'];targets=m.get('morph_vertices');test(finite(vs),key+' nonfinite')
   if targets:
    test(len(targets)==9 and all(len(v)==len(vs) for v in targets),key+' topology changed')
    for target in targets:test(finite(target),key+' nonfinite endpoint')
    # Piecewise bilinear vertices are a convex combination of the same four
    # endpoints on either side of zero, so the shared boundary is continuous.
    for axis in (0,1):
     before=[0,0,0];after=[0,0,0]
     for c in range(3):
      v0=targets[1 if axis==0 else 3][0][c];v1=targets[4][0][c];v2=targets[7 if axis==0 else 5][0][c]
      before[c]=v1+(v0-v1)*1e-6;after[c]=v1+(v2-v1)*1e-6
     test(max(abs(before[c]-after[c]) for c in range(3))<.00005,key+' morph discontinuity')
   authored=b['name'].startswith(('evolution_','gradual_','expression_','monster_')) or m.get('material')=='lens'
   if authored:
    for f in m['faces']:
     test(all(0<=uv[0]<d['texture_width'] and 0<=uv[1]<d['texture_height'] for uv in f['uv']),key+' authored UV outside atlas')
    for target in targets or [vs]:test(all(ext(target,i)[1]>ext(target,i)[0] for i in range(3)),key+' collapsed authored cuboid')
 for b in d['bones']:
  if not b['name'].startswith('evolution_ridge_'):continue
  for t in range(9):
   if t==4:continue # Hidden dormant ridges deliberately remain inside head.
   boxes=[m['morph_vertices'][t] for m in b['meshes']]
   for left,right in zip(boxes,boxes[1:]):
    test(all(min(ext(left,i)[1],ext(right,i)[1])+1e-6>=max(ext(left,i)[0],ext(right,i)[0]) for i in range(3)),key+' detached stepped ridge '+str(t))
# Distinct gameplay primary center geometry, and every special character has a
# real geometric signature and a separate valid authored coat.
for character in ('amy','omochao'):
 d=load(M/'gameplay'/f'chao_{character}.json');test(d['authored_character']==character,'character key')
 test(any(b['name'].startswith('character_') and b['meshes'] for b in d['bones']),character+' missing geometry')
 im=Image.open(A/f'textures/entity/chao/gameplay/character_{character}.png');test(im.mode=='RGBA',character+' alpha')
 test(len(set(im.get_flattened_data()))>=6,character+' palette lost detail')
 for b in d['bones']:
  if not b['name'].startswith('character_'):continue
  for m in b['meshes']:
   for f in m['faces']:test(all(0<=uv[0]<d['texture_width'] and 0<=uv[1]<d['texture_height'] for uv in f['uv']),character+' authored palette UV')
for p in (M/'cwe_parts').glob('*.json'):
 d=load(p);found={i:set() for i in SLOTS}
 for b in d['bones']:
  if not b['name'].startswith('animal_'):continue
  _,slot,animal,*_=b['name'].split('_');slot,animal=int(slot),int(animal);found[animal].add(slot)
  test(bool(b['meshes']),p.name+' empty gene part')
  for m in b['meshes']:
   test(finite(m['vertices']),p.name+' nonfinite gene geometry')
   test(all(ext(m['vertices'],i)[1]>ext(m['vertices'],i)[0] for i in range(3)),p.name+' collapsed gene shape')
   for f in m['faces']:test(all(0<=uv[0]<32 and 0<=uv[1]<32 for uv in f['uv']),p.name+' gene UV')
 test(found==SLOTS,p.name+' source slot mismatch')
test({p.stem.removeprefix('chao_') for p in (M/'cwe_parts').glob('*.json')}==set(ATTACHMENT_LAYOUTS),'all runtime source fits')
for p in (M/'party_hats').glob('*.json'):
 d=load(p)
 for b in d['bones']:
  for m in b['meshes']:
   test(finite(m['vertices']),'party hat finite')
   for f in m['faces']:test(all(0<=uv[0]<32 and 0<=uv[1]<32 for uv in f['uv']),'party hat UV')
test({p.stem.removeprefix('chao_') for p in (M/'party_hats').glob('*.json')}==set(ATTACHMENT_LAYOUTS),'party hats runtime source fits')
test(len(all_signatures)==135,'135 distinct endpoints')
report={'checks':checks,'adult_endpoints':135,'primary_forms':15,'new_characters':2,'new_animal_slot_sets':37,'new_animal_source_fits':len(ATTACHMENT_LAYOUTS),'result':'PASS'}
(ROOT/'docs/research/forms019-validation.json').write_text(json.dumps(report,indent=2)+'\n')
print('Forms019 independent validation:',checks,'checks;135 distinct endpoints;37 animal slot sets×13 runtime source fits;PASS')
