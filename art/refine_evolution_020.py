#!/usr/bin/env python3
"""Stronger authored ordinary evolution silhouettes on the unchanged source rig.

Ordinary development is already normalized to[-1,1]. Larger input values do not
select further ordinary shapes. These nine vertex endpoints replace the subtle
0.19 silhouettes with connected block steps, while keeping the face plane,
semantic joints, exact comparison rigs and the independent Monster rule intact.
"""
from pathlib import Path
import json, math
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
MODELS=ROOT/'src/main/resources/assets/chao_garden/models/entity'
PRIMARY=('baby','swim','fly','run','power','hero','hero_swim','hero_fly','hero_run','hero_power','dark','dark_swim','dark_fly','dark_run','dark_power')
DONORS={'hero_swim':'swim','hero_run':'run','dark_fly':'fly','dark_power':'power'}
AXES=[(sf,rp) for sf in (-1,0,1) for rp in (-1,0,1)]
SIDES=((0,3,2,1),(4,5,6,7),(0,1,5,4),(3,7,6,2),(0,4,7,3),(1,2,6,5))

def vertices(lo,hi):
 x,y,z=lo;X,Y,Z=hi
 return [[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]]

def bounds(vs):return [min(v[i] for v in vs) for i in range(3)],[max(v[i] for v in vs) for i in range(3)]

def core(data):
 choices=[]
 for b in data['bones']:
  if b['name'].startswith('skull__cube_'):
   for m in b['meshes']:
    if m.get('material')=='lens':continue
    lo,hi=bounds(m['vertices']);volume=math.prod(hi[i]-lo[i] for i in range(3))
    choices.append((volume,b,m,lo,hi))
 return max(choices,key=lambda item:item[0])

def source_uv(data,accent=False):
 # Existing fitted ridge faces point into skin/accent texels in the donor UV.
 if data.get('evolution020_skin_uv'):
  return data['evolution020_accent_uv' if accent else 'evolution020_skin_uv']
 candidates=[b for b in data['bones'] if b['name'].startswith('evolution_ridge_')]
 if candidates:
  meshes=candidates[0]['meshes'];return meshes[-1 if accent else 0]['faces'][0]['uv'][0]
 _,_,m,_,_=core(data);return m['faces'][0]['uv'][0]

def add(data,name,boxes,uv,top):
 meshes=[]
 for number,box_at in enumerate(boxes):
  targets=[]
  for sf,rp in AXES:
   lo,hi=box_at(sf,rp)
   targets.append(vertices([round(x,6) for x in lo],[round(x,6) for x in hi]))
  meshes.append({'name':name+'_'+str(number),'material':'official','shading':'flat',
    'vertices':targets[4],'morph_vertices':targets,
    'faces':[{'v':list(face),'uv':[list(uv) for _ in face]} for face in SIDES]})
 data['bones'].append({'name':name,'parent':'skull','pivot':[0,0,0],'rotation':[0,0,0],'meshes':meshes})
 data['head_decorations'].append(name)

def blend_box(lo,hi,weight,top):
 # Every dormant segment remains nondegenerate but fully inside the head.
 a=[-.055,top+2.5,.945];b=[.055,top+2.61,1.055]
 return ([a[i]+(lo[i]-a[i])*weight for i in range(3)],
         [b[i]+(hi[i]-b[i])*weight for i in range(3)])

def refine(path):
 data=json.loads(path.read_text());main=source_uv(data);accent=source_uv(data,True)
 key=path.stem.removeprefix('chao_');donor=json.loads((MODELS/'official'/f'chao_{DONORS.get(key,key)}.json').read_text())
 native_names={b['name'] for b in donor['bones']}
 # Aliased primary skeletons must inherit their real donor's crest roots.
 # Keeping baby-native indices here can hide a Swim skull core or a Fly eye.
 authored=[n for n in data['head_decorations'] if n not in native_names]
 data['head_decorations']=list(dict.fromkeys([*donor['head_decorations'],*authored]))
 data['evolution020_skin_uv']=list(main);data['evolution020_accent_uv']=list(accent)
 _,head_bone,head,lo,hi=core(data);top=lo[1]+head_bone['pivot'][1]
 # Keep original eye panels and their irises exactly aligned. The largest head
 # cuboid grows upward/backward; its lower edge/front plane and joints stay put.
 for b in data['bones']:
  if b['name'].startswith('skull__cube_'):
   for m in b['meshes']:
    if m is head:
     targets=[]
     for sf,rp in AXES:
      swim=max(0,-sf);fly=max(0,sf);run=max(0,-rp);power=max(0,rp)
      depth=1+.23*run+.27*swim+.06*fly+.09*power
      height=1+.24*fly+.10*power-.07*swim
      # Width remains modest at the lower edge to retain shoulder clearance;
      # the larger swim/power cap is a separate stepped upper-head volume.
      width=1+.035*swim+.035*power-.025*run
      targets.append([[round(v[0]*width,6),round(hi[1]-(hi[1]-v[1])*height,6),
                       round(lo[2]+(v[2]-lo[2])*depth,6)] for v in m['vertices']])
     m['morph_vertices']=targets
    elif any(x.get('material')=='lens' for x in b['meshes']):
     m['morph_vertices']=[[list(v) for v in m['vertices']] for _ in AXES]
 # Morph the original primary crest as part of the head, so an already large
 # Run/Power silhouette can develop just as visibly as the plain child's head.
 # Vertex-space conversion keeps every source bone pivot and animation intact.
 names={b['name']:b for b in data['bones']};cache={'skull':np.identity(4)}
 def relative(name):
  if name in cache:return cache[name]
  b=names[name];parent=b.get('parent')
  if not parent or name in ('emotion_dots','monster_crown') or name.startswith(('expression_','evolution_','evolution020_')):return None
  ancestor=relative(parent)
  if ancestor is None:return None
  x,y,z=[math.radians(v) for v in b.get('rotation',[0,0,0])]
  rx=np.array([[1,0,0,0],[0,math.cos(x),-math.sin(x),0],[0,math.sin(x),math.cos(x),0],[0,0,0,1.]])
  ry=np.array([[math.cos(y),0,math.sin(y),0],[0,1,0,0],[-math.sin(y),0,math.cos(y),0],[0,0,0,1.]])
  rz=np.array([[math.cos(z),-math.sin(z),0,0],[math.sin(z),math.cos(z),0,0],[0,0,1,0],[0,0,0,1.]])
  local=rz@ry@rx;local[:3,3]=b['pivot'];cache[name]=ancestor@local;return cache[name]
 for b in data['bones']:
  matrix=relative(b['name'])
  if matrix is None or any(m.get('material')=='lens' for m in b['meshes']):continue
  inverse=np.linalg.inv(matrix)
  for m in b['meshes']:
   if m is head:continue
   original=(matrix@np.array([v+[1] for v in m['vertices']]).T).T
   targets=[]
   for sf,rp in AXES:
    swim=max(0,-sf);fly=max(0,sf);run=max(0,-rp);power=max(0,rp)
    scale=(1+.38*swim+.1*fly-.05*run+.35*power,1-.15*swim+.4*fly+.05*run+.16*power,1+.25*swim+.12*fly+.42*run+.18*power)
    grown=original.copy();grown[:,0]*=scale[0];grown[:,1]*=scale[1];grown[:,2]=-3+(grown[:,2]+3)*scale[2]
    local=(inverse@grown.T).T
    targets.append([[round(float(n),6) for n in v[:3]] for v in local])
   m['morph_vertices']=targets
 data['bones']=[b for b in data['bones'] if not b['name'].startswith(('evolution_ridge_','evolution020_'))]
 data['head_decorations']=[n for n in data['head_decorations'] if not n.startswith(('evolution_ridge_','evolution020_'))]

 # Swim development makes a broad swept cap with paired connected cheek fins.
 for side in (-1,1):
  boxes=[]
  for segment in range(4):
   def box_at(sf,rp,s=segment,side=side):
    w=max(0,-sf);cx=side*(2.8+s*.4);cy=top+2.2+s*.1;z=1.0+s*1.25
    hx=1.25-s*.15;hy=1.05-s*.1
    return blend_box([cx-hx,cy-hy,z],[cx+hx,cy+hy,z+2.3-s*.12],w,top)
   boxes.append(box_at)
  add(data,'evolution020_swim_'+str(side).replace('-','n'),boxes,main,top)

 # Fly development grows two rising swept horns, rather than five generic
 # backwards stumps. Every step overlaps the previous one and the main head.
 for side in (-1,1):
  boxes=[]
  for segment in range(4):
   def box_at(sf,rp,s=segment,side=side):
    w=max(0,sf);cx=side*(2.55+s*.98);cy=top+.75-s*.98;z=.9+s*.25
    hx=.86-s*.09;hy=.65-s*.06
    return blend_box([cx-hx,cy-hy,z],[cx+hx,cy+hy,z+1.8-s*.18],w,top)
   boxes.append(box_at)
  add(data,'evolution020_fly_'+str(side).replace('-','n'),boxes,accent,top)

 # Five swept-back Sonic-like quills are made from overlapping rectangular
 # steps. Upper/lower side lanes remain visibly different from flight horns.
 for lane,(side,level) in enumerate(((0,0),(-1,0),(1,0),(-1,1),(1,1))):
  boxes=[]
  for segment in range(4):
   def box_at(sf,rp,s=segment,side=side,level=level):
    w=max(0,-rp);cx=side*(2.1+level*.55+s*.13);cy=top+1.1+level*1.45+s*.28;z=2.0+s*1.35
    hx=(.95 if side else .8)-s*.15;hy=.63-s*.075
    return blend_box([cx-hx,cy-hy,z],[cx+hx,cy+hy,z+2.15-s*.18],w,top)
   boxes.append(box_at)
  add(data,'evolution020_run_'+str(lane),boxes,accent if lane>=3 else main,top)

 # Power development is broad and blunt: three forehead/crown steps and two
 # compact shoulder-like head fins. It does not reuse the run quill profile.
 crown=[]
 for tier in range(3):
  def box_at(sf,rp,t=tier):
   w=max(0,rp);half=4.45-t*.55;cy=top+.45-t*.78
   return blend_box([-half,cy-.57,.4+t*.24],[half,cy+.57,4.3-t*.25],w,top)
  crown.append(box_at)
 add(data,'evolution020_power_crown',crown,main,top)
 for side in (-1,1):
  boxes=[]
  for segment in range(2):
   def box_at(sf,rp,s=segment,side=side):
    w=max(0,rp);cx=side*(3.25+s*.7);cy=top+1.65-s*.55
    return blend_box([cx-.85,cy-.6,1.0+s*.55],[cx+.85,cy+.6,3.0+s*.5],w,top)
   boxes.append(box_at)
  add(data,'evolution020_power_'+str(side).replace('-','n'),boxes,accent,top)

 # Monster's separate crown must scale upward from its attached lower edge.
 # Scaling negative local Y about the skull origin lifted the entire crown and
 # left a visible gap at stage two. Rebase without changing unscaled geometry.
 crown=next((b for b in data['bones'] if b['name']=='monster_crown'),None)
 if crown:
  old=crown['pivot'][1]
  attached=max(v[1]+old for m in crown['meshes'] for v in m['vertices'])
  for m in crown['meshes']:
   for collection in [m['vertices'],*m.get('morph_vertices',[])]:
    for v in collection:v[1]=round(v[1]+old-attached,6)
  crown['pivot'][1]=round(attached,6)
  crown['attachment_origin_020']='lower_edge'
 data['evolution_revision']='0.20.0'
 data['ordinary_axis_range']=[-1,1]
 path.write_text(json.dumps(data,separators=(',',':'))+'\n')

def main():
 for key in PRIMARY:refine(MODELS/'gameplay'/f'chao_{key}.json')
 print('Evolution020:15 ordinary rigs;135 stronger stepped endpoints;exact face planes/joints retained;Monster remains separate.')

if __name__=='__main__':main()
