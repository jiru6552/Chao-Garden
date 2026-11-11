#!/usr/bin/env python3
"""Validate smooth silhouette, fitted trim, rigid UVs and unchanged steel cabinet."""
from pathlib import Path
from copy import deepcopy
import hashlib,json,math,re
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];AS=ROOT/'src/main/resources/assets/chao_garden'
ROLES={'principal':'green','classroom':'blue','health':'cyan','fortune':'purple','exit':'green','market':'wood'}
LEFT=-14.5;RIGHT=30.5;WIDTH=22.5;MARGIN=.25
CORNERS={'north':((1,1,0),(0,0,0)),'south':((0,1,1),(1,0,1)),'east':((1,1,1),(1,0,0)),'west':((0,1,0),(0,0,1))}
CHECKS=0
def require(ok,message):
 global CHECKS;CHECKS+=1
 if not ok:raise AssertionError(message)
def close(a,b):return abs(a-b)<.000002

def independent_profile():
 # A semicircle, not a reused seven-step staircase: angle samples bound both
 # axis changes to less than one model pixel throughout the whole arch.
 cuts={LEFT,RIGHT,8.,0.,16.}
 cuts.update(round(8-22.5*math.cos(i*math.pi/96),6) for i in range(97))
 cuts=sorted(x for x in cuts if LEFT<=x<=RIGHT)
 def y(x):return 23.99+math.sqrt(max(0,22.5**2-(x-8)**2))
 return [(a,b,round(min(y(a),y(b)),6)) for a,b in zip(cuts,cuts[1:])]
PROFILE=independent_profile()
def expected_boxes(left,opened,section,segment):
 result=[]
 if (opened and section=='center') or (not opened and section in ('tip','far_tip')):return result
 for handed in (True,False):
  if section!='center' and handed!=left:continue
  for lo,hi,top in PROFILE:
   lo=max(lo,LEFT if handed else 8);hi=min(hi,8 if handed else RIGHT)
   if hi<=lo:continue
   if opened:
    a=[LEFT if handed else RIGHT-2,segment*16,16+(lo-LEFT if handed else RIGHT-hi)]
    b=[LEFT+2 if handed else RIGHT,min(top,segment*16+16),16+(hi-LEFT if handed else RIGHT-lo)]
   else:a=[lo,segment*16,14];b=[hi,min(top,segment*16+16),16]
   offset=[0 if section=='center' else -16 if handed else 16,segment*16,32 if section=='far_tip' else 16 if section=='tip' else 0]
   a=[round(max(0,a[i]-offset[i]),6) for i in range(3)];b=[round(min(16,b[i]-offset[i]),6) for i in range(3)]
   if not all(a[i]<b[i] for i in range(3)):continue
   axis=2 if opened else 0
   if result:
    pa,pb,pl=result[-1]
    if pl==handed and all(pa[k]==a[k] and pb[k]==b[k] for k in range(3) if k!=axis) and (pb[axis]==a[axis] or pa[axis]==b[axis]):pa[axis]=min(pa[axis],a[axis]);pb[axis]=max(pb[axis],b[axis]);continue
   result.append((a,b,handed))
 return result

def check_model(model,hinge,opened,section,segment,label):
 require(model.get('chao_door_profile')=='curve0255' and model.get('curve_sections')==96,label+': old staircase silhouette')
 expected=expected_boxes(hinge=='left',opened,section,segment)
 require(len(model.get('elements',[]))==len(expected),label+': missing curved strip or rectangular phantom')
 for e,(a,b,left) in zip(model['elements'],expected):
  require(all(close(x,y) for x,y in zip(e['from']+e['to'],a+b)),label+': mesh differs from exact clipped curved leaf')
  require(all(0<=v<=16 for v in e['from']+e['to']),label+': visible leaf escapes its owning block')
  front=('west' if left else 'east') if opened else 'south';back=('east' if left else 'west') if opened else 'north'
  for side,face in e['faces'].items():
   if side not in (front,back):require(face['texture']=='#edge',label+': art painted on cut edge');continue
   require(face['texture']==('#base' if side==front else '#back') and not face.get('rotation',0),label+': flipped/rotated side material')
   for corner,uv in zip(CORNERS[side],(face['uv'][:2],face['uv'][2:])):
    p=[(a[i],b[i])[corner[i]] for i in range(3)]
    if opened:distance=p[2]+(32 if section=='far_tip' else 16 if section=='tip' else 0)-16
    else:
     x=p[0]+(0 if section=='center' else -16 if left else 16);distance=x-LEFT if left else RIGHT-x
    u=(distance+MARGIN)*16/23
    if side==back:u=16-u
    require(close(uv[0],u) and close(uv[1],(48-segment*16-p[1])/3),label+': stretched or backwards artwork after swing')
 return len(expected)

def check_arch(block,doc):
 role=block.removeprefix('school_arch_');label=block
 require(doc.get('chao_door_profile')=='curve0255' and doc.get('curve_sections')==96,label+': frame is still coarse steps')
 require(doc['textures']['base']=='chao_garden:block/school025/'+ROLES[role],label+': wrong role frame color')
 require(doc['textures']['cream']=='chao_garden:block/school025/school_cream_wall',label+': infill differs from wall material')
 require(len(doc['elements'])>400,label+': insufficient smooth arc sections')
 frame=[];infill=[];gold=[]
 for e in doc['elements']:
  require(e.get('shade',True),label+': wall shading disabled')
  textures={f['texture'] for f in e['faces'].values()};require(len(textures)==1,label+': mixed material members')
  material=next(iter(textures));a=e['from'];b=e['to'];require(a[0]<b[0] and a[1]<b[1] and a[2]<b[2],label+': nonphysical member')
  if material=='#base':
   frame.append(e);require(a[2]==-16 and b[2]==.2,label+': frame anchor depth changed')
   if a[1]==-16:require(close(b[0]-a[0],1.5) and b[1]==8,label+': jamb not slender1.5px')
   else:
    # Every visible arch member lies between the radius22.5 inner opening
    # and24 outer crown, within a conservative one-pixel tessellation error.
    for x,y in ((a[0],a[1]+16),(b[0],a[1]+16),(a[0],b[1]+16),(b[0],b[1]+16)):
     rad=math.hypot(x-8,y-24);require(22.49<=rad<=24.03,label+': trim not a thin circular ring')
    require(b[0]-a[0]<.8,label+': frame retained a wide stair tread')
  elif material=='#cream':
   infill.append(e);require(a[2]==-16 and b[2]==0 and b[1]==32,label+': cream corner lost complete wall depth')
  elif material=='#gold':gold.append(e);require(a[2]==.21 and b[2]==.35,label+': gold inlay is coplanar with frame')
  else:raise AssertionError(label+': unexpected material')
 require(len(frame)>150 and len(infill)>150 and len(gold)>150,label+': curved crown/infill/inlay domain incomplete')
 # Height profile across the actual leaf must clear the rendered frame and
 # retain no large rectangular cap. Side and crown values are independent landmarks.
 require(PROFILE[0][2]<28 and max(t for _,_,t in PROFILE)>46,label+': physical leaf is still rectangular/flat-topped')
 for lo,hi,top in PROFILE:
  require(hi-lo<.8,label+': leaf curve retained coarse horizontal steps')
  require(0<=23.99+math.sqrt(max(0,22.5**2-(((lo+hi)/2)-8)**2))-top<.8,label+': leaf does not follow the curved inner frame')
  # At the midpoint all frame base members must lie strictly above the leaf.
  x=(lo+hi)/2
  for e in frame:
   if e['from'][1]>-16 and e['from'][0]<=x<=e['to'][0]:require(top<e['from'][1]+16,label+': leaf intersects frame')
 return len(frame)

def main():
 native=(ROOT/'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text()
 require('SchoolDoorGeometry0255.boxes' in native and 'CURVED_SHAPES[left?0:1]' in native,'Actual native block bypasses curved picking/collision')
 require('swingDirection(b,face)' in native and 'swingDirection(state.getBlock(),face).getOpposite()' in native,'Native tip ownership did not follow outward ornate swing / retained cabinet swing')
 require('migrateLegacyTips' in native and 'legacyTipStates' in native and '!level.getBlockState(part.getKey()).isAir()' in native,'Saved rear/forward-tip assemblies lost their conservative migration')
 require('PART_COUNT=21,CABINET_PART_COUNT=15' in native and 'DoorSection.FAR_TIP' in native,'Exact three-depth ornate ownership or cabinet separation missing')
 require(LEFT==-14.5 and RIGHT==30.5,'Closed leaves leave a visible gap at the fixed inner jambs')
 require('Shapes.joinUnoptimized' in native and 'curveUnion(shapes,0,shapes.length).optimize()' in native,'Curved shapes lost efficient cached union construction')
 count=members=0
 for role in ROLES:
  check_arch('school_arch_'+role,json.loads((AS/'models/block'/('school_arch_'+role+'.json')).read_text()))
  ident='school_door_'+role
  for hinge in ('left','right'):
   for opened in (False,True):
    for section in ('leaf','center','tip','far_tip'):
     for segment in range(3):
      name=f'{ident}_{hinge}_{"open" if opened else "closed"}_{section}_{segment}'
      members+=check_model(json.loads((AS/'models/block'/(name+'.json')).read_text()),hinge,opened,section,segment,name);count+=1
 baseline=json.loads((ROOT/'art/controls0255/school_door_baseline.json').read_text())
 for file,digest in baseline['cabinet_files'].items():
  if file.endswith('blockstates/school_tool_cabinet.json'):
   old=ROOT/'art/controls0258/school_tool_cabinet_0257.json'
   require(hashlib.sha256(old.read_bytes()).hexdigest()==digest,'Cabinet pre0258 control changed')
   expected=json.loads(old.read_text())
   for key,value in list(expected['variants'].items()):
    if 'section=tip,' in key:expected['variants'][key.replace('section=tip,','section=far_tip,')]={**value,'model':value['model'].replace('_open_tip_','_closed_tip_')}
   require(json.loads((ROOT/file).read_text())==expected,'Cabinet changed outside the mandatory inert FAR_TIP state coverage')
  else:require(hashlib.sha256((ROOT/file).read_bytes()).hexdigest()==digest,'Hidden rectangular steel cabinet geometry/art changed: '+file)
 for file,digest in baseline['complete_round_badges'].items():
  with Image.open(ROOT/file) as im:require(im.size==(368,768) and hashlib.sha256(im.convert('RGBA').crop((135,248,233,375)).tobytes()).hexdigest()==digest,'Complete round badge was altered/clipped: '+file)
 # The actual source regressions must be rejected, including curve removal.
 original=json.loads((AS/'models/block/school_door_exit_left_closed_leaf_2.json').read_text())
 for defect in ('rectangle','reverse','depth','swapped-side'):
  bad=deepcopy(original)
  if defect=='rectangle':bad['elements'][0]['to'][1]=16
  elif defect=='reverse':bad['elements'][0]['faces']['south']['uv'][0]+=1
  elif defect=='depth':bad['elements'][0]['from'][2]=7;bad['elements'][0]['to'][2]=9
  else:bad['elements'][0]['faces']['north']['texture']='#base'
  try:check_model(bad,'left',False,'leaf',2,'malformed-'+defect)
  except AssertionError:pass
  else:raise AssertionError('Accepted malformed '+defect)
 # Outward-only geometry, both bounded forward cells and exact jamb closure
 # are new0258 invariants, independently corrupted one at a time.
 for defect in ('missing-far','reversed-open','open-back-art','jamb-gap'):
  name='school_door_exit_left_open_far_tip_0' if defect=='missing-far' else 'school_door_exit_left_open_tip_0' if defect in ('reversed-open','open-back-art') else 'school_door_exit_left_closed_leaf_0'
  bad=json.loads((AS/'models/block'/(name+'.json')).read_text())
  if defect=='missing-far':bad['elements']=[]
  elif defect=='reversed-open':bad['elements'][0]['from'][2]-=1
  elif defect=='open-back-art':bad['elements'][0]['faces']['west']['texture']='#back'
  else:bad['elements'][0]['from'][0]+=.25
  try:check_model(bad,'left',defect!='jamb-gap','far_tip' if defect=='missing-far' else 'leaf' if defect=='jamb-gap' else 'tip',0,'malformed-'+defect)
  except AssertionError:pass
  else:raise AssertionError('Accepted malformed '+defect)
 arch=json.loads((AS/'models/block/school_arch_exit.json').read_text())
 for defect in ('thick','coarse','wrong-cream'):
  bad=deepcopy(arch)
  if defect=='thick':bad['elements'][0]['to'][0]+=2
  elif defect=='coarse':bad['elements']=bad['elements'][:22]
  else:bad['textures']['cream']='chao_garden:block/school025/cream'
  try:check_arch('school_arch_exit',bad)
  except AssertionError:pass
  else:raise AssertionError('Accepted malformed '+defect)
 print(f'School doors0255: {CHECKS:,} checks;288 curved meshes/{members} owned patches,6 thin circular arches, complete badges,42 unchanged cabinet geometry/art assets plus one exact inert-state extension;11 malformed regressions rejected. PASS')
if __name__=='__main__':main()
