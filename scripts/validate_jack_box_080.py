#!/usr/bin/env python3
"""Independent shipped cuboid cavity/connectivity/envelope checks, not a visual pass."""
from pathlib import Path
import copy,importlib.util,json,math
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden'
checks=0
def check(value,message):
 global checks
 checks+=1
 if not value:raise AssertionError(message)
def touches(a,b):return all(min(a['to'][i],b['to'][i])>=max(a['from'][i],b['from'][i])-1e-7 for i in range(3))
def inside(point,e):return all(e['from'][i]+1e-6<point[i]<e['to'][i]-1e-6 for i in range(3))
def validate(data,active,frame):
 parts=data['elements'];check(bool(parts),'Nonempty physical box')
 for e in parts:
  check(all(math.isfinite(v) for p in [e['from'],e['to']] for v in p),'Finite cuboids')
  check(all(e['from'][i]<e['to'][i] for i in range(3)),'Nondegenerate closed cuboids')
  check(set(e['faces'])=={'north','south','east','west','up','down'},'Every exposed cuboid has all six faces')
  check(not e.get('rotation') and not e.get('light_emission',0),'No unsupported rotations or toy emission')
  check(all(3.8-1e-6<=p[0]<=14+1e-6 and -1e-6<=p[1]<=16+1e-6 and 3.8-1e-6<=p[2]<=12.5+1e-6 for p in [e['from'],e['to']]),'Preserved reviewed collider envelope')
 # The head/crank/lid all connect physically to the original base, including
 # parts inside the cavity. A pile of disconnected decorative bars must fail.
 reached={0};changed=True
 while changed:
  changed=False
  for i,e in enumerate(parts):
   if i not in reached and any(touches(e,parts[j]) for j in reached):reached.add(i);changed=True
 check(len(reached)==len(parts),'Every box/lid/spring/head component connects to the base')
 for point in [(8,4,8),(6,5,8.5),(10,6,8.5)]:check(not any(inside(point,e) for e in parts),'Box has a real cavity, not an opaque solid interior')
 if active and frame in (1,2,3):
  check(not any(inside((8,8.3,5),e) for e in parts),'Open lid leaves the opening clear')
  top=15 if frame==2 else 12
  check(any(inside((8,top-1,8),e) for e in parts),'Visible staged head exists at its expected reveal height')
 else:check(any(inside((8,8.3,5),e) for e in parts),'Idle lid fully closes the opening')
spec=importlib.util.spec_from_file_location('jack_source080',ROOT/'art/build_toy_blocks_018.py')
builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
models={}
for stage,active,frame in [('idle',False,0),*((f'play_{i}',True,i) for i in range(4))]:
 data=json.loads((A/'models/block'/f'chao_jack_in_box_{stage}.json').read_text());models[stage]=data
 validate(data,active,frame);check(data==builder.model('chao_jack_in_box',active,frame),'Targeted and complete pipeline generation agree')
# Explicit bad-model controls exercise physical construction, not just counts.
bad=copy.deepcopy(models['play_2']);bad['elements'].append({'from':[20,2,8],'to':[21,3,9],'faces':bad['elements'][0]['faces']})
try:validate(bad,True,2)
except AssertionError:pass
else:raise AssertionError('Outside/disconnected control was accepted')
bad=copy.deepcopy(models['play_2']);bad['elements'].append({'from':[4,.8,4],'to':[12,8,12],'faces':bad['elements'][0]['faces']})
try:validate(bad,True,2)
except AssertionError:pass
else:raise AssertionError('Solid-filled cavity control was accepted')
print(json.dumps({'version':'0.25.80','scope':'Jack only; no game/build/visual inference','result':'PASS','checks':checks,'stages':5,'negative_controls':2,'envelope':[3.8,0,3.8,14,16,12.5]}))
