#!/usr/bin/env python3
"""Independent age/axis/topology/landmark checks on final authored resources."""
from pathlib import Path
import hashlib,json,math,subprocess,sys
import numpy as np
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden';M=A/'models/entity'
DONORS={'baby':'baby','swim':'swim','fly':'fly','run':'run','power':'power','hero':'hero','hero_swim':'swim','hero_fly':'hero_fly','hero_run':'run','hero_power':'hero_power','dark':'dark','dark_swim':'dark_swim','dark_fly':'fly','dark_run':'dark_run','dark_power':'power'}
COUNT=0

def check(ok,why):
 global COUNT;COUNT+=1
 if not ok:raise AssertionError(why)
def bounds(v):v=np.asarray(v);return np.r_[v.min(axis=0),v.max(axis=0)]
def intersects(a,b):return all(min(a[i+3],b[i+3])+1e-5>=max(a[i],b[i]) for i in range(3))
def evaluate(mesh,sf,rp,age):
 young=np.asarray(mesh['vertices'],float)
 if 'morph_vertices' not in mesh:return young
 aged=np.zeros_like(young)
 for y in range(3):
  for x in range(3):aged+=np.asarray(mesh['morph_vertices'][y*3+x])*max(0,1-abs(sf-(y-1)))*max(0,1-abs(rp-(x-1)))
 t=max(0,min(1,age/1.2));return young+(aged-young)*t

def signature(d,sf,rp,age):
 values=[]
 for b in d['bones']:
  for m in b['meshes']:
   # Ignore hidden reaction glyphs and the optional Monster-only crown.
   if b['name'].startswith(('expression_','monster_')):continue
   v=np.round(evaluate(m,sf,rp,age),6);v[np.abs(v)<1e-7]=0;values.extend(v.reshape(-1).tolist())
 return hashlib.sha256(json.dumps(values,separators=(',',':')).encode()).hexdigest()

all_aged=set();report=[]
for key,donor in DONORS.items():
 d=json.loads((M/'gameplay'/f'chao_{key}.json').read_text());raw=json.loads((M/'official'/f'chao_{donor}.json').read_text());names={b['name']:b for b in d['bones']}
 check(d['evolution_revision']=='0.21.0',key+' revision')
 check(d['gradual_evolution']['strength_range']==[0,1.2],key+' age coordinate')
 check(d['ordinary_axis_range']==[-1,1],key+' training coordinate')
 for b in raw['bones']:
  check(b['name'] in names,key+' missing native bone')
  check(all(names[b['name']].get(k)==b.get(k) for k in ('parent','pivot','rotation')),key+' changed native anchor '+b['name'])
 native_decor=set(raw['head_decorations']);check(native_decor<=set(d['head_decorations']),key+' donor crest hiding incomplete')
 check('gradual_head_021' in d['head_decorations'],key+' new crest hiding incomplete')
 check(not any(n.startswith(('evolution020_','evolution_ridge_')) for n in names),key+' obsolete generic ornaments')
 cores=[]
 for b in d['bones']:
  for m in b['meshes']:
   check(np.isfinite(m['vertices']).all(),key+' base finite')
   if b['name'].startswith('skull__cube_') and m['material']!='lens':
    v=bounds(m['vertices']);cores.append((np.prod(v[3:]-v[:3]),b,m))
   if m.get('morph_vertices'):
    check(len(m['morph_vertices'])==9,key+' endpoint count')
    for target in m['morph_vertices']:
     check(np.asarray(target).shape==np.asarray(m['vertices']).shape,key+' topology mismatch')
     check(np.isfinite(target).all(),key+' finite endpoint')
   if b['name'].startswith('gradual_'):
    for face in m['faces']:
     check(all(0<=uv[0]<d['texture_width'] and 0<=uv[1]<d['texture_height'] for uv in face['uv']),key+' UV outside atlas')
 _,corebone,coremesh=max(cores,key=lambda x:x[0]);corebase=bounds(coremesh['vertices']);size=corebase[3:]-corebase[:3]
 check(corebone['name'] not in d['head_decorations'],key+' cap hides core')
 crest=names['gradual_head_021']['meshes'];check(len(crest)==36,key+' matched crest topology')
 young=signature(d,0,0,0);centers=[]
 for ti,(sf,rp) in enumerate((s,r) for s in (-1,0,1) for r in (-1,0,1)):
  check(signature(d,sf,rp,0)==young,key+' newborn adult depends on future secondary')
  full=signature(d,sf,rp,1.2);check(full not in all_aged,key+' duplicate actual aged shape '+str(ti));all_aged.add(full)
  check(full!=young,key+' gradual aging has no geometric effect '+str(ti))
  midpoint=signature(d,sf,rp,.6);check(midpoint not in (young,full),key+' no actual midpoint '+str(ti))
  for b in d['bones']:
   for m in b['meshes']:
    v0=evaluate(m,sf,rp,0);vm=evaluate(m,sf,rp,.6);v1=evaluate(m,sf,rp,1.2)
    check(np.allclose(vm,(v0+v1)*.5,atol=1e-7),key+' age interpolation mismatch')
    check(np.array_equal(evaluate(m,sf,rp,2),v1),key+' visual age overshoots full morphology')
    if m['material']=='lens' or b['name'].startswith('skull__') and m is not coremesh:
     check(np.array_equal(v0,v1),key+' eye/face moved independently from fixed front plane')
  target=bounds(coremesh['morph_vertices'][ti]);growth=(target[3:]-target[:3])/size
  check(np.all(growth<=1.11) and np.all(growth>=.92),key+' exaggerated core growth '+str(growth))
  check(abs(target[2]-corebase[2])<1e-6 and abs(target[4]-corebase[4])<1e-6,key+' face/lower head plane drift')
  skullbox=target.copy();skullbox[:3]+=np.array(corebone['pivot']);skullbox[3:]+=np.array(corebone['pivot'])
  for slot in range(6):
   pieces=[bounds(crest[slot*6+i]['morph_vertices'][ti]) for i in range(6)]
   check(intersects(pieces[0],skullbox),key+' detached crest root '+str((ti,slot)))
   for i,piece in enumerate(pieces):
    check(np.all(piece[3:]>piece[:3]),key+' collapsed crest block')
    if i:check(intersects(piece,pieces[i-1]),key+' detached crest step '+str((ti,slot,i)))
  centers.append(full)
 report.append({'key':key,'young_signatures':1,'aged_shapes':len(set(centers)),'age_midpoints':9,'core_scale_minmax':[.92,1.11],'eye_front_plane_fixed':True})
# The two opposing endpoints can never have simultaneous positive tent weights.
for sf in np.linspace(-1,1,401):
 swim=max(0,1-abs(sf+1));fly=max(0,1-abs(sf-1));check(swim*fly==0,'opposed Swim/Fly weights coexist')
for rp in np.linspace(-1,1,401):
 run=max(0,1-abs(rp+1));power=max(0,1-abs(rp-1));check(run*power==0,'opposed Run/Power weights coexist')
# Cross-axis boundaries are continuous; a primary opposing a later secondary is
# still explicitly represented (e.g. Swim/Fly targetindex7 andFly/Swimindex1).
for key in DONORS:
 d=json.loads((M/'gameplay'/f'chao_{key}.json').read_text())
 for sf,rp in [(0,-.7),(0,.7),(-.7,0),(.7,0)]:
  for b in d['bones']:
   for m in b['meshes']:
    left=evaluate(m,sf-1e-6,rp,.83);right=evaluate(m,sf+1e-6,rp,.83)
    check(np.max(np.abs(left-right))<1e-4,key+' discontinuous axis boundary')
pal=json.loads((A/'gradual_evolution_palettes_021.json').read_text())['profiles'];check(set(pal)==set(DONORS),'palette coverage')
for key,colors in pal.items():
 check(len(colors)==9,key+' palette endpoint coverage')
 for entry in colors:
  for kind in ('body','accent','wings'):check(len(entry[kind])==3 and all(0<=x<=255 for x in entry[kind]),key+' bad RGB')
# These iconic color distinctions fail if primary-only textures are reused.
check(pal['run'][3]['body'][2]>pal['run'][3]['body'][0]*1.5,'Sonic secondary should mature blue')
check(pal['dark_run'][3]['accent'][0]>pal['dark_run'][3]['accent'][1]*2,'Shadow secondary red markings missing')
check(pal['fly'][7]['wings'][0]>pal['fly'][7]['wings'][2]*2,'NiGHTS fully aged yellow wings missing')
# Full authoring is deterministic even when legacy generated output already exists.
files=[M/'gameplay'/f'chao_{key}.json' for key in DONORS]+[A/'gradual_evolution_palettes_021.json',A/'gradual_evolution_models_021.json']
has_proportion_finalizer=any('proportion_adjustment_022' in json.loads(p.read_text()) for p in files[:15])
before={str(p):p.read_bytes() for p in files};subprocess.run([sys.executable,str(ROOT/'art/build_gradual_evolution_021.py')],check=True,stdout=subprocess.DEVNULL)
# Later releases finalize these exact age targets. Reapply the finalizer before
# comparing so this legacy determinism check does not erase newer corrections.
if has_proportion_finalizer:
 subprocess.run([sys.executable,str(ROOT/'art/build_proportions_022.py')],check=True,stdout=subprocess.DEVNULL)
check(all(p.read_bytes()==before[str(p)] for p in files),'non-idempotent gradual authoring')
# Explicit silhouette regressions for the swallowed jester/comb failures caught
# in the initial Minecraft views, not a requested minimum expansion of the core.
for key in ('baby','fly','run','dark_run'):
 d=json.loads((M/'gameplay'/f'chao_{key}.json').read_text());crest=next(b for b in d['bones'] if b['name']=='gradual_head_021')['meshes']
 young=np.array([v for m in crest for v in m['vertices']]);bb=bounds(young)
 if key=='baby':check(bb[1]<-7.5,'normal young crest swallowed by skull')
 if key=='fly':check(bb[0]<-5 and bb[3]>5 and bb[1]<-7,'young flight jesters swallowed by skull')
 if key in ('run','dark_run'):
  check(bb[5]>5,'running quills do not sweep behind head')
  first=[bounds(crest[slot*6]['vertices']) for slot in (0,1,2,3,4)]
  check(max(b[1] for b in first)-min(b[1] for b in first)>2,'running clusters still form a flat comb')
  check(all(min(b[3:]-b[:3])>.75 for b in first),'running cluster roots too thin')
# A connected stroke can still be completely swallowed by the square skull.
# Guard the actual exposed pieces/lengths that were missing in the client,
# measured against the authored core rather than another copy of its path data.
for key in ('hero_swim','swim','dark_fly','hero_fly'):
 d=json.loads((M/'gameplay'/f'chao_{key}.json').read_text());crest=next(b for b in d['bones'] if b['name']=='gradual_head_021')['meshes']
 cores=[(np.prod(bounds(m['vertices'])[3:]-bounds(m['vertices'])[:3]),b,m) for b in d['bones'] if b['name'].startswith('skull__cube_') for m in b['meshes'] if m['material']!='lens']
 _,bone,mesh=max(cores,key=lambda x:x[0]);skull=bounds(mesh['vertices']);skull[:3]+=bone['pivot'];skull[3:]+=bone['pivot']
 young=bounds([v for m in crest for v in m['vertices']]);check(young[3]>skull[3]+.9 and young[0]<skull[0]-.9,key+' young lateral feature still buried')
 if key!='hero_fly':
  agedskull=bounds(mesh['morph_vertices'][1]);agedskull[:3]+=bone['pivot'];agedskull[3:]+=bone['pivot']
  for slot in (1,2):
   pieces=[bounds(crest[slot*6+i]['morph_vertices'][1]) for i in range(6)]
   outside=[p for p in pieces if p[0]>agedskull[3]+.15 or p[3]<agedskull[0]-.15]
   check(len(outside)>=3,key+' Swim petals need an exposed outside shoulder and droop')
   check(max(p[4] for p in outside)>agedskull[1]+3.5,key+' Swim petals do not visibly descend beside head')
 if key=='hero_fly':
  for slot in (1,2,3,4):
   pieces=[bounds(crest[slot*6+i]['morph_vertices'][7]) for i in range(6)]
   check(any(p[0]>skull[3]+.1 or p[3]<skull[0]-.1 for p in pieces),'Hero Fly aged four-strand sweep still embedded')
  check(young[1]<skull[1]-1,'Hero Fly young arches still flat against crown')
hero=json.loads((M/'gameplay/chao_hero_swim.json').read_text());sw=hero['natural_hero_core'];pixel=sw['pixel'];uv=sw['uv']
check(sw['bone']=='skull__cube_1' and sw['layout']=='swim','Hero Swim override targets wrong anatomy')
from PIL import Image
im=Image.open(A/'textures/entity/chao/official/swim.png');check(im.getpixel(tuple(pixel))[3]==0,'Hero swatch must not overwrite existing pigment')
for bone in json.loads((M/'official/chao_swim.json').read_text())['bones']:
 for mesh in bone['meshes']:
  for face in mesh['faces']:
   us=[p[0] for p in face['uv']];vs=[p[1] for p in face['uv']]
   check(not(min(us)<=uv[0]<max(us) and min(vs)<=uv[1]<max(vs)),'Hero swatch overlaps an original UV face')
check(len(all_aged)==135,'complete135agedtopologies')
output={'version':'0.21.0','result':'PASS','checks':COUNT,'young_forms':15,'fully_aged_forms':135,'age_strengths':[0,.6,1.2],'ordinary_axes':[-1,1],'profiles':report}
p=ROOT/'docs/research/GRADUAL_MODELS_021_CHECKS.json';p.parent.mkdir(exist_ok=True,parents=True);p.write_text(json.dumps(output,indent=2)+'\n')
print('Gradual021 independent checks:',COUNT,'PASS;15young/135aged; age0/.6/1.2; connected restrained forms; fixed eyes/joints; natural palettes; deterministic.')
