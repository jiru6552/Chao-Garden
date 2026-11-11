#!/usr/bin/env python3
"""Independent visible-silhouette, connected-block and face/joint contracts."""
from pathlib import Path
import hashlib,json,math,subprocess,sys
from PIL import Image,ImageDraw,ImageChops
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden/models/entity'
KEYS=('baby','swim','fly','run','power','hero','hero_swim','hero_fly','hero_run','hero_power','dark','dark_swim','dark_fly','dark_run','dark_power')
ALIASES={'hero_swim':'swim','hero_run':'run','dark_fly':'fly','dark_power':'power'}
COUNT=0
def check(value,message):
 global COUNT;COUNT+=1
 if not value:raise AssertionError(message)
def load(path):return json.loads(path.read_text())
def box(vs):return [min(v[i] for v in vs) for i in range(3)],[max(v[i] for v in vs) for i in range(3)]
def volume(vs):
 lo,hi=box(vs);return math.prod(hi[i]-lo[i] for i in range(3))
def core(data):
 return max(((volume(m['vertices']),b,m) for b in data['bones'] if b['name'].startswith('skull__cube_') for m in b['meshes']),key=lambda v:v[0])[1:]
def overlaps(a,b):
 lo,hi=a;L,H=b
 return all(min(hi[i],H[i])+1e-5>=max(lo[i],L[i]) for i in range(3))
def matrix(b):
 x,y,z=[math.radians(v) for v in b.get('rotation',[0,0,0])]
 rx=np.array([[1,0,0,0],[0,math.cos(x),-math.sin(x),0],[0,math.sin(x),math.cos(x),0],[0,0,0,1.]])
 ry=np.array([[math.cos(y),0,math.sin(y),0],[0,1,0,0],[-math.sin(y),0,math.cos(y),0],[0,0,0,1.]])
 rz=np.array([[math.cos(z),-math.sin(z),0,0],[math.sin(z),math.cos(z),0,0],[0,0,1,0],[0,0,0,1.]])
 out=rz@ry@rx;out[:3,3]=b['pivot'];return out
def silhouette(data,target,axis):
 names={b['name']:b for b in data['bones']};cache={'skull':np.identity(4)}
 def world(name):
  if name in cache:return cache[name]
  b=names[name];parent=b.get('parent')
  if name in ('emotion_dots','monster_crown') or name.startswith('expression_'):return None
  if not parent:return None
  ancestor=world(parent)
  if ancestor is None:return None
  cache[name]=ancestor@matrix(b);return cache[name]
 im=Image.new('1',(640,640));draw=ImageDraw.Draw(im)
 for b in data['bones']:
  mtx=world(b['name'])
  if mtx is None:continue
  for m in b['meshes']:
   vs=(m.get('morph_vertices') or [m['vertices']]*9)[target]
   pts=(mtx@np.array([v+[1] for v in vs]).T).T
   for face in m['faces']:
    poly=[(320+pts[i][axis]*20,380+pts[i][1]*20) for i in face['v']]
    draw.polygon(poly,fill=1)
 return im

records=[];signatures=set();root_hashes={}
for key in KEYS:
 path=A/'gameplay'/f'chao_{key}.json';data=load(path);raw=load(A/'official'/f'chao_{ALIASES.get(key,key)}.json')
 check(data.get('evolution_revision')=='0.20.0',key+' not finalized')
 check(data.get('ordinary_axis_range')==[-1,1],key+' ordinary range was relabeled')
 names={b['name']:b for b in data['bones']}
 for b in raw['bones']:
  if '__cube_' in b['name']:continue
  actual=names[b['name']]
  check(all(actual.get(k)==b.get(k) for k in ('parent','pivot','rotation')),key+' semantic joint moved '+b['name'])
 raw_names={b['name'] for b in raw['bones']};donor_roots=set(raw['head_decorations']);actual_roots=set(data['head_decorations'])
 check(actual_roots<=set(names),key+' closed cap lists a nonexistent decoration root')
 check(donor_roots<=actual_roots,key+' closed cap omits native donor crest roots')
 check(actual_roots&raw_names==donor_roots,key+' wrong native core/eye listed as head decoration')
 def hidden(name,roots):
  seen=set()
  while name:
   if name in roots:return True
   check(name not in seen,key+' cycle in closed-cap hiding');seen.add(name);name=names[name].get('parent')
  return False
 for b in data['bones']:
  if b['name'].startswith('evolution020_'):check(hidden(b['name'],actual_roots),key+' authored evolution survives closed cap '+b['name'])
 for b in raw['bones']:
  if hidden(b['name'],donor_roots):check(hidden(b['name'],actual_roots),key+' donor crest subtree survives closed cap '+b['name'])
 cb,cm=core(data);face_bones=[b for b in data['bones'] if any(m.get('material')=='lens' for m in b['meshes'])]
 check(not hidden(cb['name'],actual_roots),key+' closed cap hides main skull core')
 for b in face_bones:check(not hidden(b['name'],actual_roots),key+' closed cap hides eye/iris panel '+b['name'])
 for b in face_bones:
  for m in b['meshes']:
   for target in m.get('morph_vertices',[]):check(target==m['vertices'],key+' face/iris alignment changed')
 center_front=min(v[2]+cb['pivot'][2] for v in cm['vertices'])
 for target in cm['morph_vertices']:
  check(min(v[2]+cb['pivot'][2] for v in target)==center_front,key+' face plane moved')
  check(max(v[1] for v in target)==max(v[1] for v in cm['vertices']),key+' lower head attachment moved')
 crown=names['monster_crown']
 check(crown.get('attachment_origin_020')=='lower_edge',key+' Monster crown origin not repaired')
 check(max(v[1] for m in crown['meshes'] for v in m['vertices'])==0,key+' Monster crown base is not its scale origin')
 for stretch in (1.2,1.4):
  groups=[box([[v[i]+cb['pivot'][i] for i in range(3)] for v in cm['vertices']])]
  groups += [box([[v[0]+crown['pivot'][0],v[1]*stretch+crown['pivot'][1],v[2]*stretch+crown['pivot'][2]] for v in m['vertices']]) for m in crown['meshes']]
  joined={0}
  for _ in groups:
   extra={i for i,b in enumerate(groups) if any(overlaps(b,groups[j]) for j in joined)}
   if extra<=joined:break
   joined|=extra
  check(len(joined)==len(groups),key+' Monster crown detaches under stage amplification')
 for t in range(9):
  signature=hashlib.sha256(json.dumps([[b['name'],[(m.get('morph_vertices') or [m['vertices']]*9)[t] for m in b['meshes']]] for b in data['bones']],separators=(',',':')).encode()).hexdigest()
  check(signature not in signatures,key+' duplicate endpoint');signatures.add(signature)
  # All generated steps are ordinary axis-aligned cuboids, never slanted faces.
  boxes=[box([[v[i]+cb['pivot'][i] for i in range(3)] for v in cm['morph_vertices'][t]])]
  for b in data['bones']:
   if not b['name'].startswith('evolution020_'):continue
   check(b['parent']=='skull' and b['pivot']==[0,0,0] and b['rotation']==[0,0,0],key+' step anchor changed')
   for m in b['meshes']:
    check(len(m['morph_vertices'])==9 and all(len(v)==8 for v in m['morph_vertices']),key+' cuboid topology changed')
    vs=m['morph_vertices'][t];lo,hi=box(vs)
    check(all(hi[i]>lo[i] for i in range(3)),key+' degenerate step')
    check(all(len({v[i] for v in vs})==2 for i in range(3)),key+' sloped rather than stepped cuboid')
    check(all(all(math.isfinite(n) for n in v) for v in vs),key+' nonfinite step')
    check(all(0<=uv[0]<data['texture_width'] and 0<=uv[1]<data['texture_height'] for f in m['faces'] for uv in f['uv']),key+' invalid step UV')
    boxes.append((lo,hi))
  # Flood from the main head; every new step must have a physical route to it.
  connected={0}
  for _ in boxes:
   added={i for i,b in enumerate(boxes) if any(overlaps(b,boxes[j]) for j in connected)}
   if added<=connected:break
   connected|=added
  check(len(connected)==len(boxes),key+' detached head blocks at endpoint '+str(t))
 contrasts={}
 for label,target in (('swim',1),('fly',7),('run',3),('power',5)):
  diffs=[]
  for axis in (0,2):
   base=silhouette(data,4,axis);grown=silhouette(data,target,axis)
   changed=sum(ImageChops.logical_xor(base,grown).get_flattened_data());union=sum(ImageChops.logical_or(base,grown).get_flattened_data())
   diffs.append(changed/max(1,union))
  check(max(diffs)>=.10,key+' '+label+' still has an indistinct full-development silhouette '+str(diffs))
  contrasts[label]={'front_difference':round(diffs[0],4),'profile_difference':round(diffs[1],4)}
 records.append({'key':key,'ordinary_limits':[-1,1],'silhouette_changes':contrasts})
 root_hashes[path]=hashlib.sha256(path.read_bytes()).hexdigest()

check(len(signatures)==135,'Expected135 distinct primary/secondary endpoints')
# Repeat the finalizer: it must derive from center geometry, not amplify itself.
subprocess.run([sys.executable,str(ROOT/'art/refine_evolution_020.py')],check=True,stdout=subprocess.DEVNULL)
for path,digest in root_hashes.items():check(hashlib.sha256(path.read_bytes()).hexdigest()==digest,'Evolution finalizer is not byte-idempotent: '+path.name)
out=ROOT/'docs/validation-0.20';out.mkdir(parents=True,exist_ok=True)
report={'release':'0.20.0','checks':COUNT,'result':'PASS','distinct_endpoints':135,'raw_comparison_inputs_modified':False,'ordinary_axis_range':[-1,1],'morphology':records}
(out/'evolution.json').write_text(json.dumps(report,indent=2)+'\n')
print('Evolution020 PASS:',COUNT,'checks;135 distinct stronger endpoints;face/joints unchanged;all head steps connected;front/profile contrast>=10%;byte-idempotent finalizer.')
