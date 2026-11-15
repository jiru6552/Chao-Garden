#!/usr/bin/env python3
"""Independent source geometry diagnostics for the fixed-head Hero Fly family.

No generator import, Minecraft visual claim or source geometry extraction.
Checks closed cuboids, dense rigid blends, source rig/head preservation, feature
connections, actual tilted halo clearance and lengthwise cap/wing UV coverage.
"""
from pathlib import Path
import copy
import hashlib
import json
import math
import random
if __package__:
 from .validate_blocky_run_02555 import vertices, connected, volume, overlaps, rotate, dot, sub
 from .validate_neutral_swim_morph_02559 import bounds, closed
 from .validate_neutral_power_morph_02563 import no_same_facing_coplanar
 from .validator_output import parse_output, write_result
else:
 from validate_blocky_run_02555 import vertices, connected, volume, overlaps, rotate, dot, sub
 from validate_neutral_swim_morph_02559 import bounds, closed
 from validate_neutral_power_morph_02563 import no_same_facing_coplanar
 from validator_output import parse_output, write_result

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
MODEL = MODELS / 'chao_hero_fly_proto065.json'
SOURCE = MODELS / 'chao_baby.json'
FIELD = 'hero_fly_box_065'
REGION = 'hero_fly_region_065'
TARGETS = ['hero_fly_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
# .72 deliberately added reciprocal Swim/Fly rows; only those two pins
# advance. Historical rig/head/retained-target preservation was audited.
PROTECTED = {
 'chao_baby.json': 'ae3d4da88b84905f8726b863bdb072e3a26eb404f38e565d0781b92b01e2cf9b',
 'chao_hero_run_proto055.json': '77b41d4501cba585b83be91b4052e8c56e03bff02e47db2b209f146d36390f08',
 'chao_neutral_proto056.json': 'a354458cae5605b873464614b57f9f9bd04d4a4e01d1ed793cf949194c4d14db',
 'chao_hero_normal_proto057.json': 'd0052db7f401ecc545e7a3db2992933b5671387520c82429d4a8aa336f28232d',
 'chao_neutral_swim_proto059.json': 'de2972ea6780a4252a98bbbdd84d674dd25a9ea4f3c1ea04be7124e3283593b1',
 'chao_dark_normal_proto060.json': '36ae27aed51674ea31310ab4af602e3e7127e1507c736ee9b6f75f3728acb514',
 'chao_neutral_fly_proto061.json': '05ce4c9211bee32d055575864e27e7e1a925ca68a838481810a57df7e507e203',
 'chao_neutral_run_proto062.json': 'cfed60309de43bc854272a4869d455d9ec76281a37a1787ff223f69b7ed85881',
 'chao_neutral_power_proto063.json': 'c3bfcbb2fb1730bbd23b8799e35d9a2edf33b3f4fab06c11a4638bacd1db07f0',
 'chao_hero_swim_proto064.json': '73a773cf29df2b8aa2e8e6d471428d8b3a7412a4ba1eab3809dace7f00a0b324'}


def weights(age, rp, sf):
 return [1-age]+[age*max(0,1-abs(rp-x))*max(0,1-abs(sf-y)) for y in (-1,0,1) for x in (-1,0,1)]


def endpoint(mesh, target):
 w=[0]*10;w[target]=1
 return vertices(mesh,w)


def centroid(points, indices):
 return [sum(points[i][a] for i in indices)/len(indices) for a in range(3)]


def semantics(features):
 named={m['name']:m for m in features}
 tip=named['hero_fly_crown_right_tip']
 ends={i:centroid(endpoint(tip,i),(2,3,6,7)) for i in range(10)}
 assert ends[4][1]<ends[5][1]-3, 'Run must grow a tall rising V instead of the low balanced arch'
 assert ends[4][2]<ends[5][2]-2, 'Run must sweep forward toward the face, not rearward'
 assert ends[2][2]>ends[5][2]+1, 'Opposite Swim must flatten and sweep rearward'
 assert ends[8][1]>ends[5][1]+.5, 'Fly main caps must grow long down-curled outer ears'
 assert ends[8][2]>3.5 and ends[9][2]>3.3, 'Fly and Power+Fly main caps must visibly extend behind the original skull back, matching actual side/above reference'
 assert ends[6][1]<ends[5][1]-3, 'Power main prongs must rise prominently'
 assert ends[4][1]<ends[7][1]-2, 'Run+Fly must return to a low arch instead of duplicating pure Run'
 assert ends[4][1]<ends[1][1]<ends[2][1], 'Run+Swim must moderate the vertical V and horizontal Swim directions'
 assert ends[6][1]<ends[3][1]<ends[2][1], 'Power+Swim must moderate high Power and flat Swim'
 spur=named['hero_fly_crown_right_spur']
 assert spur[FIELD]['sizes'][8][1]>3 and spur[FIELD]['sizes'][9][1]>3, 'Fly and Power+Fly need a separate emerging upper fork'
 assert spur[FIELD]['sizes'][6][1]>1.5, 'Power needs a separate outer spur'
 assert all(spur[FIELD]['sizes'][i][1]<.2 for i in (0,1,2,4,5,7)), 'Unused auxiliary volume must remain small and embedded'
 signatures=[tuple(round(x,7)for m in features for p in endpoint(m,i)for x in p)for i in range(1,10)]
 assert len(set(signatures))==9, 'Nine mature head corners must be distinct'
 for target in range(10):
  for section in ('root','tip','spur'):
   left=endpoint(named['hero_fly_crown_left_'+section],target)
   right=endpoint(named['hero_fly_crown_right_'+section],target)
   assert sorted(tuple(round(-p[a]if a==0 else p[a],7)for a in range(3))for p in left)==sorted(tuple(round(p[a],7)for a in range(3))for p in right)
 return {TARGETS[i]:ends[i]for i in range(10)}


def lengthwise_uv(mesh, kind):
 channel=mesh[FIELD];center=channel['centers'][0]
 if kind=='cap':
  axis=rotate([0,-1,0],channel['rotations'][0]);length=channel['sizes'][0][1]
  start,end,expected_start,expected_end=62.75,63.25,125,126
 else:
  sign=-1 if '_left_'in mesh['name']else 1
  axis=rotate([sign,0,0],channel['rotations'][0]);length=channel['sizes'][0][0]
  start,end,expected_start,expected_end=62.25,61.75,124,123
 count=prox=dist=long_faces=0
 for face in mesh['faces']:
  points=[mesh['vertices'][i]for i in face['v']]
  fractions=[.5+dot(sub(p,center),axis)/length for p in points]
  for f,uv in zip(fractions,face['uv']):
   assert abs(uv[0]-(start+(end-start)*f))<1e-7 and uv[1]==31.75,(mesh['name'],f,uv)
  if max(fractions)-min(fractions)<.99:
   pixel=expected_end if min(fractions)>.99 else expected_start
   assert all(math.floor(uv[0]*2)==pixel for uv in face['uv'])
   continue
  long_faces+=1
  for si in range(21):
   for ti in range(9):
    s,t=si/20,ti/8;w=[(1-s)*(1-t),s*(1-t),s*t,(1-s)*t]
    point=[sum(a*p[k]for a,p in zip(w,points))for k in range(3)]
    uv=[sum(a*p[k]for a,p in zip(w,face['uv']))for k in range(2)]
    fraction=.5+dot(sub(point,center),axis)/length;pixel=(math.floor(uv[0]*2),math.floor(uv[1]*2))
    assert pixel in ((expected_start,63),(expected_end,63)),(kind,mesh['name'],pixel)
    if fraction<.49:assert pixel==(expected_start,63);prox+=1
    if fraction>.51:assert pixel==(expected_end,63);dist+=1
    count+=1
 assert long_faces==4 and prox>=320 and dist>=320,(mesh['name'],long_faces,prox,dist)
 return count,prox,dist


def main():
 out=parse_output()
 data=json.loads(MODEL.read_text());source=json.loads(SOURCE.read_text())
 bones={b['name']:b for b in data['bones']};old={b['name']:b for b in source['bones']}
 assert len(bones)==62 and list(bones)[:59]==list(old)
 for name,bone in old.items():
  for field in ('pivot','rotation','parent'):assert bones[name].get(field)==bone.get(field),(name,field)
  if name.startswith(('skull','expression_'))or name.startswith('emotion_')and name!='emotion_dot__cube_0':assert bones[name]==bone,name
 assert data['head_decorations']==source['head_decorations'] and data['materials']==source['materials']
 for name in ('tail','tail__cube_0','tail__cube_1'):assert bones[name]==old[name],name
 head_mesh=old['skull__cube_0']['meshes'][0];head=head_mesh['vertices']
 assert bounds(head)==[[-3.5,3.5],[-6,0],[-3,3]]
 assert data['hero_fly_path_065']['targets']==TARGETS
 assert data['hero_fly_path_065']['reserved_pixels']=={'feature_body':[127,63],'cap_proximal':[125,63],'cap_distal':[126,63],'wing_root':[124,63],'wing_distal':[123,63]}
 old_uv=[uv for b in source['bones']for m in b.get('meshes',[])for f in m['faces']for uv in f['uv']]
 assert max(v for u,v in old_uv)<31.75,'New swatches overlap original UV islands'
 boxes=[];features=[];wings=[];halos=[]
 for bone in data['bones']:
  for original in bone.get('meshes',[]):
   if FIELD not in original:continue
   m=copy.deepcopy(original);m['blocky_box_055']=m[FIELD];boxes.append(m);closed(m)
   assert REGION in m
   for field in ('centers','sizes','rotations'):
    v=m[FIELD][field];assert len(v)==10 and all(len(x)==3 for x in v) and all(math.isfinite(x)for row in v for x in row)
   assert min(x for row in m[FIELD]['sizes']for x in row)>=.01
   assert max(abs(a-b)for p,q in zip(endpoint(m,0),m['vertices'])for a,b in zip(p,q))<1e-7
   if bone['name']=='gradual_head_021':features.append(m)
   elif bone['name'].startswith('wing_'):wings.append(m)
   elif bone['name']=='emotion_dot__cube_0'or bone['name'].startswith('hero_fly_halo_'):halos.append(m)
 assert (len(boxes),len(features),len(wings),len(halos))==(21,6,6,4)
 facts=semantics(features)
 cap_uv=[lengthwise_uv(m,'cap')for m in features if m['name'].endswith(('_tip','_spur'))]
 wing_uv=[lengthwise_uv(m,'wing')for m in wings]
 for m in features:
  if m['name'].endswith('_root'):assert all(uv==[63.75,31.75]for f in m['faces']for uv in f['uv'])
 dot_b=old['emotion_dot'];group=old['emotion_dots']
 def halo_in_skull(points):
  result=[]
  for p in points:
   q=rotate(p,dot_b['rotation']);q=[q[a]+dot_b['pivot'][a]for a in range(3)]
   q=rotate(q,group['rotation']);result.append([q[a]+group['pivot'][a]for a in range(3)])
  return result
 eyes=[m['vertices']for n in ('skull__cube_2','skull__cube_3')for m in old[n]['meshes']]
 rng=random.Random(25065)
 samples=[(a/20,x/10-1,y/10-1)for a in range(21)for x in range(21)for y in range(21)]
 samples += [(rng.random(),rng.uniform(-1,1),rng.uniform(-1,1))for _ in range(512)]
 face_pairs=coincident=0;minimum_volume=math.inf;max_hinge=0;minimum_vertical_gap=math.inf
 for age,rp,sf in samples:
  w=weights(age,rp,sf);assert abs(sum(w)-1)<1e-9 and min(w)>=0
  f=[vertices(m,w)for m in features];assert connected(f,head),(age,rp,sf,'Disconnected head flap')
  examined,same=no_same_facing_coplanar([(head_mesh,head)]+list(zip(features,f)));face_pairs+=examined;coincident+=same
  h=[halo_in_skull(vertices(m,w))for m in halos];assert connected(h[1:],h[0]),(age,rp,sf,'Disconnected halo')
  for points in f:
   assert not any(overlaps(points,e)for e in eyes),(age,rp,sf,'Eye collision')
   for bar in h:assert not overlaps(points,bar),(age,rp,sf,'Actual tilted halo collision')
  for bar in h:assert not overlaps(bar,head),(age,rp,sf,'Halo/skull collision')
  minimum_vertical_gap=min(minimum_vertical_gap,min(p[1]for shape in f for p in shape)-max(p[1]for shape in h for p in shape))
  for m in boxes:
   p=vertices(m,w);v=volume(p,m['faces']);assert v>1e-8;minimum_volume=min(minimum_volume,v)
  for m in wings:
   p=vertices(m,w);sign=-1 if '_left_'in m['name']else 1
   indices=(0,3,4,7)if sign<0 else(1,2,5,6)
   drift=math.dist(centroid(p,indices),[0,.25,0]);max_hinge=max(max_hinge,drift);assert drift<1e-7
 for name in ('foot_left__cube_0','foot_right__cube_0'):
  m=next(x for x in boxes if x is not None and x['name']==bones[name]['meshes'][0]['name'] and x[FIELD]==bones[name]['meshes'][0][FIELD])
  for i in range(10):assert abs(max(p[1]for p in endpoint(m,i))-2)<1e-7
 meta=json.loads((ASSETS/'hero_fly065.json').read_text());assert meta['sha256']==hashlib.sha256(MODEL.read_bytes()).hexdigest()
 for n,h in PROTECTED.items():assert hashlib.sha256((MODELS/n).read_bytes()).hexdigest()==h,n+' changed'
 result={'version':'0.25.65','scope':'Independent JSON geometry/SAT/UV checks, not Minecraft visual acceptance.','source_files':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest()for p in [MODEL,SOURCE,ASSETS/'hero_fly065.json']},'positive_closed_boxes':len(boxes),'dense_and_random_blends':len(samples),'head_feature_count':len(features),'halo_bar_count':len(halos),'wing_ray_count':len(wings),'original_joint_count':59,'total_bone_count':62,'original_head_size':[7,6,6],'arbitrary_rotation_face_pairs':face_pairs,'same_facing_coplanar_disjoint_pairs':coincident,'positive_area_coplanar_overlaps':0,'cap_uv_samples':sum(v[0]for v in cap_uv),'wing_uv_samples':sum(v[0]for v in wing_uv),'maximum_hinge_drift':max_hinge,'minimum_positive_volume':minimum_volume,'minimum_informative_vertical_halo_gap':minimum_vertical_gap,'halo_clearance_interpretation':'Negative global Y projection gap is permitted: actual full three-dimensional convex-box SAT checks prove crown and tilted halo remain disjoint. This number does not claim strict vertical separation.','all_ten_previous_models_exact':True,'semantic_head_tip_endpoints':facts}
 write_result(out,result)
 print('HeroFly065:',len(boxes),'positive closed boxes;',len(samples),'dense/random blends;',face_pairs,'arbitrary-rotation face pairs, zero positive-area coplanar overlaps; connected paired flaps/tilted halo, five-swatch cap/wing UVs, fixed original head/face/59 joints and ten prior models passed.')


if __name__=='__main__':main()
