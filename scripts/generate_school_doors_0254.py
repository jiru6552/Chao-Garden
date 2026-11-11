#!/usr/bin/env python3
"""Final deterministic postpass: slender school arches and physically cut moving leaves.

The steel utility cabinet deliberately retains its independent rectangular carcass.
The retained 23:48 artwork is cropped by .75 model pixels at each leaf edge rather
than rescaled, preserving circular badges and the two independently authored sides.
"""
import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'src/main/resources/assets/chao_garden'
MODEL=AS/'models/block'
ROLES={'principal':'green','classroom':'blue','health':'cyan','fortune':'purple','exit':'green','market':'wood'}
SOURCE=ROOT/'src/main/java/com/chaogarden/core/SchoolDoorGeometry0254.java'
PROFILE=[[float(n) for n in row.split(',')] for row in re.findall(r'\{([-.\d]+,[-.\d]+,[-.\d]+)\}',SOURCE.read_text().split('private static final double[][] PROFILE=')[1].split(';')[0])]
LEFT=-13.5;RIGHT=29.5;MARGIN=.75
OUTER=[(-16,-11,32),(-11,-6,39),(-6,0,44),(0,16,48),(16,22,44),(22,27,39),(27,32,32)]

def write(path,data):path.write_text(json.dumps(data,indent=2)+'\n')

def box(a,b,material):
 return {'from':list(a),'to':list(b),'faces':{side:{'texture':'#'+material,'uv':[0,0,16,16]} for side in ('north','south','west','east','up','down')}}

def boxes(left,opened,section,segment):
 result=[]
 if (opened and section=='center') or (not opened and section=='tip'):return result
 for physical_left in (True,False):
  if section!='center' and physical_left!=left:continue
  for lo,hi,top in PROFILE:
   x0=max(lo,LEFT if physical_left else 8);x1=min(hi,8 if physical_left else RIGHT)
   y1=min(16,top-segment*16)
   if x0>=x1 or y1<=0:continue
   if opened:
    d0=x0-LEFT if physical_left else RIGHT-x1;d1=x1-LEFT if physical_left else RIGHT-x0
    z0=max(0,8+d0-(16 if section=='tip' else 0));z1=min(16,8+d1-(16 if section=='tip' else 0))
    if z0<z1:result.append(([1.5 if physical_left else 12.5,0,z0],[3.5 if physical_left else 14.5,y1,z1],physical_left))
   else:
    offset=0 if section=='center' else -16 if physical_left else 16
    a=max(0,x0-offset);b=min(16,x1-offset)
    if a<b:result.append(([a,0,7],[b,y1,9],physical_left))
 return result

def leaf(a,b,left,opened,section,part):
 e=box(a,b,'edge');v0=(48-part*16-b[1])/3;v1=(48-part*16-a[1])/3
 if opened:
  u0=(a[2]+(16 if section=='tip' else 0)-8+MARGIN)*16/23
  u1=(b[2]+(16 if section=='tip' else 0)-8+MARGIN)*16/23
  front='west' if left else 'east';back='east' if left else 'west'
  e['faces'][front]={'texture':'#base','uv':[u0,v0,u1,v1] if left else [u1,v0,u0,v1]}
  e['faces'][back]={'texture':'#back','uv':[16-u1,v0,16-u0,v1] if left else [16-u0,v0,16-u1,v1]}
 else:
  offset=0 if section=='center' else -16 if left else 16
  distance=lambda x:x+offset-LEFT if left else RIGHT-x-offset
  u0=(distance(a[0])+MARGIN)*16/23;u1=(distance(b[0])+MARGIN)*16/23
  e['faces']['south']={'texture':'#base','uv':[u0,v0,u1,v1]}
  e['faces']['north']={'texture':'#back','uv':[16-u1,v0,16-u0,v1]}
 return e

def arch(role):
 elements=[box([-16,-16,-16],[-14,14,.2],'base'),box([30,-16,-16],[32,14,.2],'base')]
 for x0,x1,top in OUTER:
  elements.append(box([x0,top-18,-16],[x1,top-16,.2],'base'))
  if top<48:elements.append(box([x0,top-16,-16],[x1,32,0],'cream'))
  elements.append(box([x0,top-16.5,.21],[x1,top-16,.35],'gold'))
 return {'chao_door_profile':'arch0254','textures':{'base':'chao_garden:block/school025/'+ROLES[role],
   'cream':'chao_garden:block/school025/school_cream_wall','gold':'chao_garden:block/school025/gold',
   'particle':'chao_garden:block/school025/'+ROLES[role]},'elements':elements}

def main():
 assert len(PROFILE)==7,'Shared native arch profile was not found'
 for role in ROLES:
  ident='school_door_'+role
  write(MODEL/('school_arch_'+role+'.json'),arch(role))
  for left in (True,False):
   hinge='left' if left else 'right'
   for opened in (False,True):
    for section in ('leaf','center','tip'):
     for segment in range(3):
      name=f'{ident}_{hinge}_{"open" if opened else "closed"}_{section}_{segment}'
      m={'chao_door_profile':'arch0254','textures':{k:'chao_garden:block/school025/'+ident+suffix for k,suffix in (('base',''),('back','_back'),('edge','_edge'),('particle',''))},
         'ambientocclusion':False,'elements':[leaf(a,b,l,opened,section,segment) for a,b,l in boxes(left,opened,section,segment)]}
      write(MODEL/(name+'.json'),m)
      if left and not opened and section=='leaf' and segment==0:write(MODEL/(ident+'.json'),m)
 print('School doors0.25.4: six slim arches and216 cut-top native leaf models; steel cabinet unchanged.')

if __name__=='__main__':main()
