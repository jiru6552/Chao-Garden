#!/usr/bin/env python3
"""Validate the four adult staff meshes, source preservation and facial separation."""
from pathlib import Path
import hashlib,json,math
from PIL import Image
from validate_doctor_normals_0252 import validate_doctor_normals
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden';checks=0

def require(ok,message):
 global checks
 checks+=1
 if not ok:raise AssertionError(message)

def main():
 source=json.loads((A/'models/entity/official/chao_baby.json').read_text());original={b['name']:b for b in source['bones']}
 with Image.open(A/'textures/entity/chao/staff025.png') as atlas,Image.open(A/'textures/entity/chao/official/baby.png') as native:
  require(atlas.size==(256,128),'Native staff atlas size')
  require(atlas.crop((0,0,128,64)).tobytes()==native.convert('RGBA').tobytes(),'Staff atlas changed original neutral pixels')
 must={'teacher':['staff_teacher_cap','staff_teacher_waistcoat','staff_lesson_book'],
       'doctor':['staff_doctor_hair','staff_doctor_stethoscope_headset','staff_doctor_chestpiece','staff_doctor_moustache'],
       'principal':['staff_principal_spectacles','staff_moustache_left','staff_moustache_right','staff_principal_beard'],
       'fortune':['staff_fortune_circlet','staff_fortune_stole']}
 for role,names in must.items():
  d=json.loads((A/'models/entity/staff025'/f'{role}.json').read_text());bones={b['name']:b for b in d['bones']}
  require(len(bones)==len(d['bones']),f'{role}: duplicate bones')
  require(d['staff_025']['adult'] and d['staff_025']['role']==role,f'{role}: adult identity')
  require(d['texture_width']==128 and d['texture_height']==64,f'{role}: atlas UV scale')
  for name in names:require(name in bones,f'{role}: missing professional detail {name}')
  for side,x in [('left',-2),('right',2)]:
   eye=bones['staff_eye_'+side];require(eye['parent']=='skull' and eye['pivot']==[x,-2.05,-3.2],f'{role}: misplaced native face')
  require(len([b for b in bones if b.startswith('staff_eye_')])==2,f'{role}: duplicate eye pair')
  if role=='doctor':
   require('staff_doctor_smile' not in bones and 'staff_principal_beard' not in bones,'Doctor retained a grin or copied the Principal beard')
   names=[m.get('name','') for b in d['bones'] for m in b.get('meshes',[])]
   require(not any('tooth' in name or 'teeth' in name for name in names),'Doctor contains tooth geometry')
   require(sum(name.startswith('gray_moustache_lobe_') for name in names)==2,'Doctor lacks two swept moustache lobes')
   with Image.open(A/'textures/entity/chao/staff025.png') as atlas:
    report=validate_doctor_normals(d,atlas.convert('RGBA'))
   for label,ok in report['checks'].items():require(ok,'Doctor moustache: '+label)
  if role=='fortune':
   for side in ('left','right'):
    names={m.get('name','') for m in bones['staff_eye_'+side]['meshes']}
    require(names=={'violet_closed_lid','closed_lid_edge'},'Fortune eyelid unexpectedly contains an open white/pupil layer')
  for name,b in original.items():
   if name in ('skull__cube_2','skull__cube_3') or role=='teacher' and name=='skull__cube_1':
    require(bones[name]['meshes']==[],f'{role}: old facial/cap geometry remained')
   else:require(bones[name]==b,f'{role}: native skeleton altered: {name}')
  for b in d['bones']:
   require(b['parent'] is None or b['parent'] in bones,f'{role}: orphan bone')
   for value in b['pivot']+b['rotation']:require(math.isfinite(value),f'{role}: nonfinite joint')
   for m in b.get('meshes',[]):
    for v in m['vertices']:
     require(len(v)==3 and all(math.isfinite(x) and abs(x)<100 for x in v),f'{role}: invalid vertex')
    for f in m['faces']:
     require(len(f['v'])==len(f['uv']) and len(f['v'])>=3,f'{role}: invalid face topology')
     require(all(0<=i<len(m['vertices']) for i in f['v']),f'{role}: invalid face index')
     for u,v in f['uv']:require(0<=u<=128 and 0<=v<=64,f'{role}: invalid atlas UV')
  require(d['staff_025']['source_mesh_sha256']==hashlib.sha256((A/'models/entity/official/chao_baby.json').read_bytes()).hexdigest(),f'{role}: stale native input')
 print(f'Staff025 asset assertions passed: {checks:,}; four adult role meshes, one eye pair each and exact neutral source pixels preserved.')
if __name__=='__main__':main()
