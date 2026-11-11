#!/usr/bin/env python3
"""Verify cut silhouettes, native ownership, rigid UVs and wall-matching slender trim."""
import json
import re
from copy import deepcopy
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'src/main/resources/assets/chao_garden'
ROLES={'principal':'green','classroom':'blue','health':'cyan','fortune':'purple','exit':'green','market':'wood'}
PROFILE=[(-13.5,-11,29.75),(-11,-6,36.75),(-6,0,41.75),(0,16,45.75),(16,22,41.75),(22,27,36.75),(27,29.5,29.75)]
OUTER=[(-16,-11,32),(-11,-6,39),(-6,0,44),(0,16,48),(16,22,44),(22,27,39),(27,32,32)]
CORNERS={'north':((1,1,0),(0,0,0)),'south':((0,1,1),(1,0,1)),
         'east':((1,1,1),(1,0,0)),'west':((0,1,0),(0,0,1))}

def require(ok,message):
 if not ok:raise AssertionError(message)
def near(a,b):return abs(a-b)<1e-7

def expected_boxes(left,opened,section,segment):
 """Clip an independent physical leaf to one owning cell after the rigid swing."""
 result=[]
 if section==('center' if opened else 'tip'):return result
 for physical_left in (True,False):
  if section!='center' and physical_left!=left:continue
  for lo,hi,top in PROFILE:
   lo=max(lo,-13.5 if physical_left else 8);hi=min(hi,8 if physical_left else 29.5)
   if hi<=lo:continue
   if opened:
    a=[-14.5 if physical_left else 28.5,segment*16,8+(lo+13.5 if physical_left else 29.5-hi)]
    b=[-12.5 if physical_left else 30.5,min(top,segment*16+16),8+(hi+13.5 if physical_left else 29.5-lo)]
   else:a=[lo,segment*16,7];b=[hi,min(top,segment*16+16),9]
   offset=[0 if section=='center' else -16 if physical_left else 16,segment*16,16 if section=='tip' else 0]
   a=[max(0,a[i]-offset[i]) for i in range(3)];b=[min(16,b[i]-offset[i]) for i in range(3)]
   if all(a[i]<b[i] for i in range(3)):result.append((a,b,physical_left))
 return result

def check_model(model,hinge,opened,section,segment,label):
 require(model.get('chao_door_profile')=='arch0254',label+': missing cut-leaf profile')
 expected=expected_boxes(hinge=='left',opened,section,segment)
 require(len(model['elements'])==len(expected),label+': rectangular phantom or missing native strip')
 for e,(a,b,left) in zip(model['elements'],expected):
  require(e['from']==a and e['to']==b,label+': visible mesh differs from cut-top native shape')
  front=('west' if left else 'east') if opened else 'south'
  back=('east' if left else 'west') if opened else 'north'
  for side,face in e['faces'].items():
   if side not in (front,back):
    require(face['texture']=='#edge',label+': artwork on cut edge');continue
   require(face['texture']==('#base' if side==front else '#back'),label+': front/back switched during swing')
   require(not face.get('rotation',0),label+': badge is rotated')
   for corner,uv in zip(CORNERS[side],(face['uv'][:2],face['uv'][2:])):
    p=[(a[i],b[i])[corner[i]] for i in range(3)]
    if opened:distance=p[2]+(16 if section=='tip' else 0)-8
    else:
     x=p[0]+(0 if section=='center' else -16 if left else 16)
     distance=x+13.5 if left else 29.5-x
    u=(distance+.75)*16/23
    if side==back:u=16-u
    v=(48-segment*16-p[1])/3
    require(near(uv[0],u) and near(uv[1],v),label+': art stretched, backwards or detached from physical leaf')
 return len(expected)

def check_arch(block,doc):
 role=block.removeprefix('school_arch_');label=block
 require(doc.get('chao_door_profile')=='arch0254',label+': old bulky profile')
 require(doc['textures'].get('base')=='chao_garden:block/school025/'+ROLES[role],label+': wrong role material')
 require(doc['textures'].get('cream')=='chao_garden:block/school025/school_cream_wall',label+': corner infill differs from actual wall material')
 expected=[([-16,-16,-16],[-14,14,.2],'#base'),([30,-16,-16],[32,14,.2],'#base')]
 for x0,x1,top in OUTER:
  expected.append(([x0,top-18,-16],[x1,top-16,.2],'#base'))
  if top<48:expected.append(([x0,top-16,-16],[x1,32,0],'#cream'))
  expected.append(([x0,top-16.5,.21],[x1,top-16,.35],'#gold'))
 require(len(doc['elements'])==len(expected),label+': arch strip count changed')
 for e,(a,b,material) in zip(doc['elements'],expected):
  require(e['from']==a and e['to']==b,label+': trim exceeds2px or infill leaves an open corner')
  require(e.get('shade',True) and all(f['texture']==material for f in e['faces'].values()),label+': wall shade/material differs')

def main():
 java=(ROOT/'src/main/java/com/chaogarden/core/SchoolDoorGeometry0254.java').read_text()
 actual=[tuple(float(n) for n in s.split(',')) for s in re.findall(r'\{([-.\d]+,[-.\d]+,[-.\d]+)\}',java.split('private static final double[][] PROFILE=')[1].split(';')[0])]
 require(actual==PROFILE,'Native physical profile differs from the independently checked arch silhouette')
 native=(ROOT/'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text()
 require('if(!toolCabinet){' in native and 'SchoolDoorGeometry0254.boxes(left,open,section.ordinal(),segment)' in native,'Actual block bypasses cut outline/collision geometry')
 count=0
 for role in ROLES:
  check_arch('school_arch_'+role,json.loads((AS/'models/block'/('school_arch_'+role+'.json')).read_text()))
  ident='school_door_'+role
  for hinge in ('left','right'):
   for opened in (False,True):
    for section in ('leaf','center','tip'):
     for segment in range(3):
      name=f'{ident}_{hinge}_{"open" if opened else "closed"}_{section}_{segment}'
      check_model(json.loads((AS/'models/block'/(name+'.json')).read_text()),hinge,opened,section,segment,name);count+=1
 # These are the precise original user regressions, not existence-only fixtures.
 name='school_door_exit_left_closed_leaf_2';original=json.loads((AS/'models/block'/(name+'.json')).read_text())
 for defect in ('rectangle','reverse'):
  broken=deepcopy(original)
  if defect=='rectangle':broken['elements'][0]['to'][1]=16
  else:broken['elements'][0]['faces']['south']['uv'][0]+=1
  try:check_model(broken,'left',False,'leaf',2,name)
  except AssertionError:pass
  else:raise AssertionError('Cut-leaf '+defect+' regression escaped')
 print(f'School doors0.25.4: {count} native cut-leaf meshes, exact slender arch/wall fill, owning-cell bounds and rigid front/back UVs verified.')

if __name__=='__main__':main()
