#!/usr/bin/env python3
"""Check rollback identity, source-art provenance, UVs, collision envelopes and wearable fits."""
from pathlib import Path
import json,hashlib,sys,math
from PIL import Image,ImageOps
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden';D=ROOT/'art/sa2_02515';sys.path.insert(0,str(ROOT/'scripts'))
from generate_sa2_assets_02515 import TOYS,HATS,adapt,toy_mesh,toy_item_mesh,fit,render,icon,LIMITS,source,normal,RIGID_TOYS,rigid_toy_pose
checks=0
def check(value,msg):
 global checks
 checks+=1
 if not value:raise AssertionError(msg)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def obj(path):
 lines=path.read_text().splitlines();ps=[tuple(map(float,l.split()[1:])) for l in lines if l.startswith('v ')];uv=[tuple(map(float,l.split()[1:])) for l in lines if l.startswith('vt ')];mats={l.split()[1] for l in lines if l.startswith('usemtl ')}
 check(bool(ps) and len(ps)>=9,'Empty OBJ '+str(path));check(all(0<=v<=1 for t in uv for v in t),'UV outside atlas '+str(path));return ps,mats

def validate_rigid_toys():
 # Validate the shipped poses too: a correct generator alone cannot repair stale assets.
 for n in RIGID_TOYS:
  rest=obj(A/'models/sa2_02515'/(n+'_idle.obj'))[0]
  for frame in (-1,0,1,2,3):
   stage='idle' if frame<0 else 'play_'+str(frame)
   d=json.loads((A/'models/block'/(n+'_'+stage+'.json')).read_text())
   check(d['model']=='chao_garden:models/sa2_02515/'+n+'_'+stage+'.obj','Wrong rigid toy pose binding '+n+'/'+stage)
   ps,mats=obj(A/d['model'].split(':')[1])
   expected=[p for face,_,_ in rigid_toy_pose(n,frame).faces
             if sum(v*v for v in normal(face))>=.9 for p in face]
   check(len(ps)==len(rest)==len(expected),'Rigid toy topology changed '+n+'/'+stage)
   check(all(abs(p[a]-q[a]/16)<=5.01e-8 for p,q in zip(ps,expected) for a in range(3)),
         'Stale rigid toy pose '+n+'/'+stage)
   check(abs(min(p[1] for p in ps)-.005)<=5.01e-8,'Rigid toy loses floor contact '+n+'/'+stage)
   check(mats<=d['textures'].keys(),'Unbound rigid toy material '+n+'/'+stage)
   for v in d['textures'].values():check((A/'textures'/(v.split(':')[1]+'.png')).exists(),'Missing rigid toy material '+v)
   if frame in (0,2):check(ps==rest,'Rigid toy neutral pose changed '+n+'/'+stage)
   # Adjacent and distant pairs detect scale changes in the actual exported geometry.
   for i in range(len(ps)):
    for j in ((i+1)%len(ps),(i+len(ps)//2)%len(ps)):
     check(abs(math.dist(ps[i],ps[j])-math.dist(rest[i],rest[j]))<=2e-7,
           'Rigid toy changed size '+n+'/'+stage)

def toy_stages(name):
 stages=['idle',*(f'play_{i}' for i in range(4))]
 return stages+['float_'+stage for stage in stages] if name=='chao_lifesaver' else stages

def main():
 for rel,digest in json.loads((D/'provenance.json').read_text())['inputs'].items():check(sha(D/rel)==digest,'Changed reference '+rel)
 for path in json.loads((D/'rollback.json').read_text()):check((D/'rollback'/(path+'.png')).read_bytes()==(A/'textures'/(path+'.png')).read_bytes(),'Rollback not exact '+path)
 for path,e in json.loads((D/'artwork.json').read_text()).items():
  expected=source(e['archive'],e['texture'])
  if 'school0252/' in path:expected=ImageOps.pad(expected,(128,128),method=Image.Resampling.NEAREST,color=(223,211,183,255))
  with Image.open(A/'textures'/(path+'.png')) as actual:check(actual.tobytes()==expected.tobytes(),'Original artwork changed '+path)
 for n in [*TOYS,*HATS]:
  m,mats=toy_item_mesh(n) if n in TOYS else adapt(HATS[n]);expected=icon(fit(m,(1,1,1,15,15,15)),n,mats)
  with Image.open(A/'textures/item'/(n+'.png')) as actual:check(actual.tobytes()==expected.tobytes(),'Icon/model mismatch '+n)
  data=json.loads((A/'models/item/sa2_02515'/(n+'.json')).read_text());ps,used=obj(A/data['model'].split(':')[1]);check(used<=data['textures'].keys(),'Unbound item material '+n)
 for n in TOYS:
  for stage in toy_stages(n):
   d=json.loads((A/'models/block'/(n+'_'+stage+'.json')).read_text())
   check(d['model']=='chao_garden:models/sa2_02515/'+n+'_'+stage+'.obj','Wrong placed toy pose binding '+n+'/'+stage)
   ps,mats=obj(A/d['model'].split(':')[1]);lim=list(LIMITS.get(n,(3,0,3,13,8,13)))
   if stage!='idle' and n=='chao_maracas':lim[4]=6
   # Rigid mounted motion sweeps beyond the rest fit without changing scale.
   if n=='chao_rocking_horse':lim=[1,0,1,15,14,15.1]
   elif n=='chao_lifesaver':
    lim=[1,-.4,1,15,3.4,15]
    if stage.startswith('float_'):lim[1]+=12.8;lim[4]+=12.8
   # Fixed-size instruments sweep outside the old per-frame fitting envelope.
   elif n=='chao_rattle':lim=[6.25,0,4.74,9.61,3.96,11.26]
   elif n=='chao_drum':lim=[2.72,0,3,13.28,6.08,13]
   check(all(lim[a]-.002<=p[a]*16<=lim[a+3]+.002 for p in ps for a in range(3)),'Model escapes interaction envelope '+n+'/'+stage)
   check(mats<=d['textures'].keys(),'Unbound placed material '+n)
   for v in d['textures'].values():check((A/'textures'/(v.split(':')[1]+'.png')).exists(),'Missing material '+v)
 validate_rigid_toys()
 for p in (D/'previous-models/hats').glob('*.json'):
  old=json.loads(p.read_text());new=json.loads((A/'models/entity/hats'/p.name).read_text());check(new['texture_width']==1024 and new['texture_height']==1024,'Hat atlas scale')
  for before,after in zip(old['bones'],new['bones']):
   check(before['name']==after['name'] and before.get('parent')==after.get('parent') and before.get('pivot')==after.get('pivot'),'Hat anchor changed')
   if before['name'] in HATS:
    a=[v for m in before['meshes'] for v in m['vertices']];b=[v for m in after['meshes'] for v in m['vertices']]
    check(all(abs(fn(p[i] for p in a)-fn(p[i] for p in b))<.0001 for i in range(3) for fn in (min,max)),'Hat fitting envelope changed')
    check(all(0<=v<=1024 for m in after['meshes'] for f in m['faces'] for pair in f['uv'] for v in pair),'Hat UV outside atlas')
 for n in ('blue_hoodie_accessory','pink_hoodie_accessory','hoodie_sleeve_left','hoodie_sleeve_right'):
  old=json.loads((D/'previous-models/cwe_accessories'/(n+'.json')).read_text());new=json.loads((A/'models/entity/cwe_accessories'/(n+'.json')).read_text());a=old['bones'][0]['meshes'][0]['vertices'];b=new['bones'][0]['meshes'][0]['vertices'];check(len(b)==16,'Missing garment bevel')
  check(all(abs(fn(p[i] for p in a)-fn(p[i] for p in b))<.00001 for i in range(3) for fn in (min,max)),'Garment fitting envelope changed')
 report={'release':'0.25.15','result':'PASS','checks':checks,'source_toys':17,'source_hats':12,'placed_toy_poses':sum(len(toy_stages(n)) for n in TOYS),'original_artworks':10,'exact_texture_rollbacks':8,'source_geometry_triangles':{n:sum(len(o['faces']) for o in json.loads((D/(n+'-mesh.json')).read_text())['objects']) for n in ('ChaoStgNeut','ChaoStgHero','ChaoStgDark')}}
 out=ROOT/'docs/validation-0.25.15/source-validation.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':
 if sys.argv[1:]==['--rigid-toy-poses']:
  validate_rigid_toys();print(json.dumps({'result':'PASS','checks':checks,'rigid_toy_poses':10}))
 elif not sys.argv[1:]:main()
 else:raise SystemExit('Usage: validate_sa2_assets_02515.py [--rigid-toy-poses]')
