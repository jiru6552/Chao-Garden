"""Bounded developer check for fitted Chao additions (requires numpy).

Checks all overlay skeletons, then measures cuboid/head overlap during 13 samples
of the native idle clip across 11 ordinary source rigs. These measurements are a
regression diagnostic, not proof of unrestricted animation clearance. The native
source shoulders themselves overlap the head during some idle samples. Runtime
movement/reaction blending, noncuboid features, and arbitrary mixed poses require
actual-client visual review.
"""
from pathlib import Path
import json,math,ast
import numpy as np
from runtime_resource_contract_022 import ATTACHMENT_LAYOUTS, COAT_LAYOUTS
r=Path(__file__).resolve().parents[1]
model=r/'src/main/resources/assets/chao_garden/models/entity'
a=json.loads((r/'art/imported_chao/animations/chao.animation.json').read_text())['animations']
def expr(s,t):
 s=s.lower().replace('query.anim_time','t').replace('variable.carry','0').replace('math.sin','sin')
 node=ast.parse(s,mode='eval')
 def e(n):
  if isinstance(n,ast.Expression):return e(n.body)
  if isinstance(n,ast.Constant):return n.value
  if isinstance(n,ast.Name) and n.id=='t':return t
  if isinstance(n,ast.UnaryOp):return -e(n.operand) if isinstance(n.op,ast.USub) else e(n.operand)
  if isinstance(n,ast.BinOp):
   x,y=e(n.left),e(n.right)
   if isinstance(n.op,ast.Add):return x+y
   if isinstance(n.op,ast.Sub):return x-y
   if isinstance(n.op,ast.Mult):return x*y
   if isinstance(n.op,ast.Div):return x/y
  if isinstance(n,ast.Call) and n.func.id=='sin':return math.sin(math.radians(e(n.args[0])))
  raise ValueError(s)
 return e(node)
def val(v,t):return np.array([expr(x,t) if isinstance(x,str) else x for x in v],dtype=float)
def sample(v,t):
 if isinstance(v,(int,float,str)):return np.repeat(expr(v,t) if isinstance(v,str) else v,3)
 if isinstance(v,list):return val(v,t)
 pts=sorted((float(k),vv) for k,vv in v.items());left=next((p for p in reversed(pts) if p[0]<=t),pts[0]);right=next((p for p in pts if p[0]>=t),pts[-1])
 if right[0]==left[0]:return val(left[1],t)
 q=(t-left[0])/(right[0]-left[0]);return val(left[1],t)*(1-q)+val(right[1],t)*q
def poses(clip,t):
 out={}
 for name,channels in a[clip]['bones'].items():
  p={'position':np.zeros(3),'rotation':np.zeros(3),'scale':np.ones(3)}
  for prop,v in channels.items():
   values=sample(v,t)
   if prop!='scale':values*=np.array([-1,-1,1])
   if prop=='rotation':values=-np.radians(values)
   p[prop]=values
  out[name]=p
 return out
def rot(d):
 x,y,z=d;cx,sx=math.cos(x),math.sin(x);cy,sy=math.cos(y),math.sin(y);cz,sz=math.cos(z),math.sin(z)
 return np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])@np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]])@np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]])
def matrices(rig,ps):
 out={};bones={b['name']:b for b in rig['bones']}
 def visit(name):
  if name in out:return out[name]
  b=bones[name];p=ps.get(name,{});m=np.eye(4);m[:3,:3]=rot(np.radians(b.get('rotation',[0,0,0]))+p.get('rotation',np.zeros(3)))@np.diag(p.get('scale',np.ones(3)));m[:3,3]=np.array(b['pivot'])+p.get('position',np.zeros(3))
  if b.get('parent'):m=visit(b['parent'])@m
  out[name]=m;return m
 for name in bones:visit(name)
 return out
def points(mesh,m):
 v=np.array(mesh['vertices']);return (m[:3,:3]@v.T).T+m[:3,3]
def axes(mesh,v):
 directions=[];normals=[]
 def add(values,target):
  n=np.linalg.norm(values)
  if n<1e-7:return
  values=values/n
  if not any(abs(np.dot(x,values))>.99999 for x in target):target.append(values)
 for f in mesh['faces']:
  ids=f['v'];add(np.cross(v[ids[1]]-v[ids[0]],v[ids[2]]-v[ids[0]]),normals)
  for i,n in enumerate(ids):add(v[ids[(i+1)%len(ids)]]-v[n],directions)
 return directions,normals
def sat(ma,va,mb,vb):
 ea,na=axes(ma,va);eb,nb=axes(mb,vb);ax=na+nb+[np.cross(x,y) for x in ea for y in eb];depth=1e99
 for x in ax:
  n=np.linalg.norm(x)
  if n<1e-7:continue
  x=x/n;pa=va@x;pb=vb@x;overlap=min(max(pa),max(pb))-max(min(pa),min(pb))
  if overlap<.0001:return 0
  depth=min(depth,overlap)
 return depth


def inspect_skeletons():
 checks=0
 actual={p.stem.removeprefix('chao_') for p in (model/'overlays').glob('chao_*.json')}
 assert actual==set(ATTACHMENT_LAYOUTS), 'Runtime overlay selector domain differs'
 for key in ATTACHMENT_LAYOUTS:
  path=model/'official'/f'chao_{key}.json'
  official=json.loads(path.read_text())
  overlay=json.loads((model/'overlays'/path.name).read_text())
  ob={b['name']:b for b in overlay['bones']}
  for bone in official['bones']:
   if '__cube_' in bone['name']:continue
   fitted=ob[bone['name']]
   assert fitted['parent']==bone['parent'],bone['name']
   assert fitted['pivot']==bone['pivot'],bone['name']
   assert fitted.get('rotation',[0,0,0])==bone.get('rotation',[0,0,0]),bone['name']
   assert not fitted['meshes'],bone['name']
   checks+=4
  visible_animals={b['name'].split('_')[2] for b in overlay['bones'] if b['name'].startswith('animal_')}
  assert visible_animals=={str(i) for i in range(18)}
  assert all('chaos_'+form+'_crest' in ob for form in ('light','angel','devil'))
  for b in overlay['bones']:
   assert b.get('parent') is None or b['parent'] in ob
   for mesh in b['meshes']:
    assert all(math.isfinite(c) for vertex in mesh['vertices'] for c in vertex)
    checks+=1
  checks+=2
 return checks


def measure_idle():
 results={kind:{'checks':0,'contacts':[]} for kind in ('native','overlay')}
 rigs=COAT_LAYOUTS
 clip='animation.chao.idle'
 for key in rigs:
  official=json.loads((model/'official'/('chao_'+key+'.json')).read_text())
  overlay=json.loads((model/'overlays'/('chao_'+key+'.json')).read_text())
  # Largest skull cube is the core; on Swim the core is cube 1, not cube 0.
  candidates=[b for b in official['bones'] if b['name'].startswith('skull__cube_')]
  def volume(b):
   vs=b['meshes'][0]['vertices']
   return math.prod(max(v[i] for v in vs)-min(v[i] for v in vs) for i in range(3))
  head=max(candidates,key=volume);hm=head['meshes'][0]
  for t in np.linspace(0,a[clip]['animation_length'],13):
   ps=poses(clip,t);om=matrices(official,ps);cm=matrices(overlay,ps)
   hv=points(hm,om[head['name']])
   for kind,rig,mats in [('native',official,om),('overlay',overlay,cm)]:
    result=results[kind]
    for b in rig['bones']:
     if kind=='native':
      if not b['name'].startswith(('arm_left__cube_','arm_right__cube_')):continue
     elif not b['name'].startswith('animal_0_'):continue
     for mesh in b['meshes']:
      if len(mesh['vertices'])!=8:continue
      depth=sat(hm,hv,mesh,points(mesh,mats[b['name']]))
      result['checks']+=1
      if depth>.05:
       result['contacts'].append({'depth':depth,'rig':key,'time':round(float(t),3),
                                  'bone':b['name'],'mesh':mesh['name']})
 from collections import Counter
 for result in results.values():
  contacts=result.pop('contacts')
  result['contacts_over_0_05']=len(contacts)
  result['max_depth']=max((v['depth'] for v in contacts),default=0)
  result['mesh_contacts']=dict(Counter(v['mesh'] for v in contacts))
  result['top_contacts']=sorted(contacts,key=lambda v:v['depth'],reverse=True)[:12]
 return {'clip':clip,'rig_count':len(rigs),'samples_per_rig':13,
         'depth_units':'model pixels; 16 per block',
         'scope':'Idle cuboid diagnostic only; does not establish universal clearance.',
         **results}


if __name__=='__main__':
 import argparse
 parser=argparse.ArgumentParser(description=__doc__)
 parser.add_argument('--output',type=Path,help='Write JSON evidence')
 args=parser.parse_args()
 evidence={'skeleton_assertions':inspect_skeletons(),**measure_idle()}
 if args.output:
  args.output.parent.mkdir(parents=True,exist_ok=True)
  args.output.write_text(json.dumps(evidence,indent=2)+'\n')
 print(json.dumps(evidence,indent=2))
