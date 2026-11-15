#!/usr/bin/env python3
"""Independent source geometry diagnostics for the fixed-head Hero Power family.

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
from validate_blocky_run_02555 import vertices, connected, volume, overlaps, rotate, dot, sub
from validate_neutral_swim_morph_02559 import bounds, closed
from validate_neutral_power_morph_02563 import no_same_facing_coplanar

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
MODEL = MODELS / 'chao_hero_power_proto067.json'
SOURCE = MODELS / 'chao_baby.json'
FIELD = 'hero_power_box_067'
REGION = 'hero_power_region_067'
TARGETS = ['hero_power_young', 'run_swim', 'swim', 'power_swim',
           'run', 'balanced', 'power', 'run_fly', 'fly', 'power_fly']
PROTECTED = {
 'chao_baby.json': 'ae3d4da88b84905f8726b863bdb072e3a26eb404f38e565d0781b92b01e2cf9b',
 'chao_hero_run_proto055.json': '77b41d4501cba585b83be91b4052e8c56e03bff02e47db2b209f146d36390f08',
 'chao_neutral_proto056.json': 'a354458cae5605b873464614b57f9f9bd04d4a4e01d1ed793cf949194c4d14db',
 'chao_hero_normal_proto057.json': 'd0052db7f401ecc545e7a3db2992933b5671387520c82429d4a8aa336f28232d',
 'chao_neutral_swim_proto059.json': '06c6c2372669adb2fcf0fb4d529cbf9d0866b20e4b0d23b002e84c9fc8a9a1a8',
 'chao_dark_normal_proto060.json': '36ae27aed51674ea31310ab4af602e3e7127e1507c736ee9b6f75f3728acb514',
 'chao_neutral_fly_proto061.json': '2524c85c7c121531233b03eee36cdb3df6c8a39d568fbe9b0faea09ba887a885',
 'chao_neutral_run_proto062.json': 'cfed60309de43bc854272a4869d455d9ec76281a37a1787ff223f69b7ed85881',
 'chao_neutral_power_proto063.json': 'c3bfcbb2fb1730bbd23b8799e35d9a2edf33b3f4fab06c11a4638bacd1db07f0',
 'chao_hero_swim_proto064.json': '73a773cf29df2b8aa2e8e6d471428d8b3a7412a4ba1eab3809dace7f00a0b324',
 'chao_hero_fly_proto065.json': 'f5847fe9bc3429c03c5340f0cf65ff99649537759df9f5c993d25b53feb332cf',
 'chao_hero_run_proto066.json': '6ee8f47a220d769fa1e66a60cade8b7e8d211b16bafeca78dcd77074a8ae0e17'}


def weights(age, rp, sf):
 return [1-age]+[age*max(0,1-abs(rp-x))*max(0,1-abs(sf-y)) for y in (-1,0,1) for x in (-1,0,1)]


def endpoint(mesh, target):
 w=[0]*10;w[target]=1
 return vertices(mesh,w)


def centroid(points, indices):
 return [sum(points[i][a] for i in indices)/len(indices) for a in range(3)]


def semantics(features):
 named={m['name']:m for m in features}
 assert list(named)==['hero_power_crown_center_root','hero_power_crown_center_tip','hero_power_crown_left_root','hero_power_crown_left_tip','hero_power_crown_right_root','hero_power_crown_right_tip']
 root=named['hero_power_crown_center_root'];tip=named['hero_power_crown_center_tip'];side=named['hero_power_crown_right_tip']
 ends={i:centroid(endpoint(tip,i),(2,3,6,7)) for i in range(10)}
 side_ends={i:centroid(endpoint(side,i),(2,3,6,7)) for i in range(10)}
 for name in ['hero_power_crown_center_tip','hero_power_crown_left_tip','hero_power_crown_right_tip']:
  points=endpoint(named[name],0)
  assert min(p[1]for p in points)<-6.35 and volume(points,named[name]['faces'])>.5, 'Every fresh Power crown lobe must be visible, not hidden Run fans'
 assert tip[FIELD]['sizes'][6][0]>1.8*tip[FIELD]['sizes'][0][0], 'Mature Power needs a materially broader central bulb'
 assert ends[6][1]<ends[5][1]-1, 'Mature Power center must rise above compact balanced knobs'
 assert ends[4][2]>ends[5][2]+2 and side_ends[4][2]>side_ends[5][2]+2, 'Opposite Run must form three long rearward prongs'
 for i in (2,8):
  assert ends[i][2]>ends[0][2]+4, 'Fly/Swim central blade must extend far rearward in above/side view'
  assert root[FIELD]['sizes'][i][1]>4 and root[FIELD]['sizes'][i][0]<.8, 'Long thin white center blade must be real geometry, not a tiny front cap'
 assert side[FIELD]['sizes'][8][0]>side[FIELD]['sizes'][7][0]*2, 'Fly outer lobes must broaden beyond smaller RunFly prongs'
 assert side_ends[9][1]<side_ends[4][1]-.3, 'PowerFly must rise upward/out rather than repeat flat rearward Run prongs'
 signatures=[tuple(round(x,7)for m in features for p in endpoint(m,i)for x in p)for i in range(1,10)]
 assert len(set(signatures))==9
 for target in range(10):
  for section in ('root','tip'):
   left=endpoint(named['hero_power_crown_left_'+section],target);right=endpoint(named['hero_power_crown_right_'+section],target)
   assert sorted(tuple(round(-p[a]if a==0 else p[a],7)for a in range(3))for p in left)==sorted(tuple(round(p[a],7)for a in range(3))for p in right)
 return {TARGETS[i]:{'central_distal':ends[i],'right_lobe_distal':side_ends[i]}for i in range(10)}


def spot_uv(mesh):
 if mesh[REGION]=='crown_root':
  assert all(uv==[63.75,31.75]for f in mesh['faces']for uv in f['uv'])
  return 0
 channel=mesh[FIELD];center=channel['centers'][0];rotation=channel['rotations'][0];size=channel['sizes'][0]
 axis=rotate([0,-1,0],rotation);count=long_faces=0;coverage=set()
 for face in mesh['faces']:
  points=[mesh['vertices'][i]for i in face['v']]
  fractions=[.5+dot(sub(p,center),axis)/size[1]for p in points]
  if max(fractions)-min(fractions)<.99:
   expected=[63.25,31.75]if min(fractions)>.99 else[62.75,31.75]
   assert all(uv==expected for uv in face['uv'])
   continue
  long_faces+=1
  candidates=[]
  for cross in (0,2):
   direction=[0,0,0];direction[cross]=1;basis=rotate(direction,rotation)
   f=[.5+dot(sub(p,center),basis)/size[cross]for p in points]
   if max(f)-min(f)>.99:candidates.append((cross,basis))
  assert len(candidates)==1
  cross,basis=candidates[0]
  for point,fraction,uv in zip(points,fractions,face['uv']):
   across=.5+dot(sub(point,center),basis)/size[cross]
   assert abs(uv[0]-(58.1+1.8*across))<1e-7 and abs(uv[1]-(30.1+1.8*fraction))<1e-7
  local_pixels=set()
  for si in range(21):
   for ti in range(21):
    s,t=si/20,ti/20;w=[(1-s)*(1-t),s*(1-t),s*t,(1-s)*t]
    point=[sum(a*p[k]for a,p in zip(w,points))for k in range(3)]
    uv=[sum(a*p[k]for a,p in zip(w,face['uv']))for k in range(2)]
    fraction=.5+dot(sub(point,center),axis)/size[1];across=.5+dot(sub(point,center),basis)/size[cross]
    assert abs(uv[0]-(58.1+1.8*across))<1e-7 and abs(uv[1]-(30.1+1.8*fraction))<1e-7
    pixel=tuple(math.floor(v*2)for v in uv);assert pixel[0]in range(116,120)and pixel[1]in range(60,64)
    local_pixels.add(pixel);coverage.add(pixel);count+=1
  assert len(local_pixels)==16
 assert long_faces==4 and len(coverage)==16
 return count


def lengthwise_uv(mesh, kind):
 channel=mesh[FIELD];center=channel['centers'][0]
 sign=-1 if '_left_'in mesh['name']else 1
 axis=rotate([sign,0,0],channel['rotations'][0]);length=channel['sizes'][0][0]
 count=prox=dist=long_faces=0
 for face in mesh['faces']:
  points=[mesh['vertices'][i]for i in face['v']]
  fractions=[.5+dot(sub(p,center),axis)/length for p in points]
  for f,uv in zip(fractions,face['uv']):assert abs(uv[0]-(62.25-.5*f))<1e-7 and uv[1]==31.75
  if max(fractions)-min(fractions)<.99:
   pixel=123 if min(fractions)>.99 else 124
   assert all(math.floor(uv[0]*2)==pixel for uv in face['uv'])
   continue
  long_faces+=1
  for si in range(21):
   for ti in range(9):
    s,t=si/20,ti/8;w=[(1-s)*(1-t),s*(1-t),s*t,(1-s)*t]
    point=[sum(a*p[k]for a,p in zip(w,points))for k in range(3)]
    uv=[sum(a*p[k]for a,p in zip(w,face['uv']))for k in range(2)]
    fraction=.5+dot(sub(point,center),axis)/length;pixel=(math.floor(uv[0]*2),math.floor(uv[1]*2))
    assert pixel in((124,63),(123,63))
    if fraction<.49:assert pixel==(124,63);prox+=1
    if fraction>.51:assert pixel==(123,63);dist+=1
    count+=1
 assert long_faces==4 and prox>=320 and dist>=320
 return count


def main():
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
 assert data['hero_power_path_067']['targets']==TARGETS
 expected_pixels={'feature_body':[127,63],'cap_proximal':[125,63],'cap_distal':[126,63],'wing_root':[124,63],'wing_distal':[123,63],**{'patch_'+str(x)+'_'+str(y):[116+x,60+y]for y in range(4)for x in range(4)}}
 assert data['hero_power_path_067']['reserved_pixels']==expected_pixels
 old_uv=[uv for b in source['bones']for m in b.get('meshes',[])for f in m['faces']for uv in f['uv']]
 assert max(v for u,v in old_uv)<30,'New swatches overlap original UV islands'
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
   elif bone['name']=='emotion_dot__cube_0'or bone['name'].startswith('hero_power_halo_'):halos.append(m)
 assert (len(boxes),len(features),len(wings),len(halos))==(21,6,6,4)
 facts=semantics(features)
 cap_uv=[spot_uv(m)for m in features]
 wing_uv=[lengthwise_uv(m,'wing')for m in wings]
 dot_b=old['emotion_dot'];group=old['emotion_dots']
 def halo_in_skull(points):
  result=[]
  for p in points:
   q=rotate(p,dot_b['rotation']);q=[q[a]+dot_b['pivot'][a]for a in range(3)]
   q=rotate(q,group['rotation']);result.append([q[a]+group['pivot'][a]for a in range(3)])
  return result
 eyes=[m['vertices']for n in ('skull__cube_2','skull__cube_3')for m in old[n]['meshes']]
 rng=random.Random(25067)
 samples=[(a/20,x/10-1,y/10-1)for a in range(21)for x in range(21)for y in range(21)]
 samples += [(rng.random(),rng.uniform(-1,1),rng.uniform(-1,1))for _ in range(512)]
 face_pairs=coincident=0;minimum_volume=math.inf;max_hinge=0;minimum_vertical_gap=math.inf
 for age,rp,sf in samples:
  w=weights(age,rp,sf);assert abs(sum(w)-1)<1e-9 and min(w)>=0
  f=[vertices(m,w)for m in features];assert connected(f,head),(age,rp,sf,'Disconnected head flap')
  try:examined,same=no_same_facing_coplanar([(head_mesh,head)]+list(zip(features,f)))
  except AssertionError as error:raise AssertionError((age,rp,sf,str(error)))from error
  face_pairs+=examined;coincident+=same
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
 meta=json.loads((ASSETS/'hero_power067.json').read_text());assert meta['sha256']==hashlib.sha256(MODEL.read_bytes()).hexdigest()
 for n,h in PROTECTED.items():assert hashlib.sha256((MODELS/n).read_bytes()).hexdigest()==h,n+' changed'
 result={'version':'0.25.67','scope':'Independent JSON geometry/SAT/UV checks, not Minecraft visual acceptance.','source_files':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest()for p in [MODEL,SOURCE,ASSETS/'hero_power067.json']},'positive_closed_boxes':len(boxes),'dense_and_random_blends':len(samples),'head_feature_count':len(features),'halo_bar_count':len(halos),'wing_ray_count':len(wings),'original_joint_count':59,'total_bone_count':62,'original_head_size':[7,6,6],'arbitrary_rotation_face_pairs':face_pairs,'same_facing_coplanar_disjoint_pairs':coincident,'positive_area_coplanar_overlaps':0,'cap_uv_samples':sum(cap_uv),'wing_uv_samples':sum(wing_uv),'maximum_hinge_drift':max_hinge,'minimum_positive_volume':minimum_volume,'minimum_informative_vertical_halo_gap':minimum_vertical_gap,'halo_clearance_interpretation':'Negative global Y projection gap is permitted: actual full three-dimensional convex-box SAT checks prove crown and tilted halo remain disjoint. This number does not claim strict vertical separation.','all_twelve_previous_models_exact':True,'semantic_head_tip_endpoints':facts}
 out=Path('/workspace/.setup/hero067-model');out.mkdir(exist_ok=True);(out/'source-geometry-audit.json').write_text(json.dumps(result,indent=2)+'\n')
 print('HeroPower067:',len(boxes),'positive closed boxes;',len(samples),'dense/random blends;',face_pairs,'arbitrary-rotation face pairs, zero positive-area coplanar overlaps; connected central stem/fan and tilted halo, painted two-dimensional cap spots/wing UVs, fixed original head/face/59 joints and twelve prior models passed.')


if __name__=='__main__':main()
