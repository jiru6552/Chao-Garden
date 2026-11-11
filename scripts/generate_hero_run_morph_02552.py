#!/usr/bin/env python3
"""Focused matched-topology Hero Run adaptation on the existing Chao rig.

Uses the current Baby UVs/joints and measured original SA2B Hero Run ratios.
This is a cuboid-style adaptation, not a claim of importing the Ninja mesh.
No other gameplay model is regenerated.
"""
from pathlib import Path
import copy,hashlib,json,math
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
MODELS=ASSETS/'models/entity/gameplay'
OUT=MODELS/'chao_hero_run_proto052.json'
NAMES=('hero_baby','run_baby','hero_run','mature','run_run')

def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def load(k):return json.loads((MODELS/f'chao_{k}.json').read_text())
def roundv(a):return [round(float(x),6) for x in a]
def mapped(source,canonical):
 out={b['name']:b for b in source['bones']}
 # Source eye panels use different cube indices; match their semantic side.
 oldeyes=[b for b in source['bones'] if any(m.get('material')=='lens' for m in b.get('meshes',[]))]
 neweyes=[b for b in canonical['bones'] if any(m.get('material')=='lens' for m in b.get('meshes',[]))]
 def center(b):
  m=next(m for m in b['meshes'] if m.get('material')=='lens');return sum(v[0] for v in m['vertices'])/len(m['vertices'])+b['pivot'][0]
 for a,b in zip(sorted(neweyes,key=center),sorted(oldeyes,key=center)):out[a['name']]=b
 return out

def author():
 data=load('baby');hero=load('hero');run=load('run');hr=load('hero_run')
 # Four strips exactly cover the neutral orb and continuously become the
 # existing four-bar Hero halo. No marker is swapped at an alignment threshold.
 marker=next(b for b in data['bones'] if b['name']=='emotion_dot__cube_0')
 original=copy.deepcopy(marker);lo=min(v[0] for v in original['meshes'][0]['vertices']);hi=max(v[0] for v in original['meshes'][0]['vertices'])
 for lane in range(4):
  part=marker if lane==0 else copy.deepcopy(original);part['name']=f'emotion_dot__cube_{lane}'
  for mesh in part['meshes']:
   for v in mesh['vertices']:v[0]=lo+(hi-lo)*(lane+(v[0]-lo)/(hi-lo))/4
   mesh['morph_vertices']=[copy.deepcopy(mesh['vertices']) for _ in range(9)]
  if lane:data['bones'].append(part)
 maps=[mapped(j,data) for j in (hero,run,hr,hr,hr)]
 # Measured original AL_RootObject type15 endpoint ratios (ChaoMain.prs).
 # Original HEAD includes its crest/rear point; keep facial cuboid and eye
 # planes stable, adding the characteristic rear point as a matched wedge.
 ratios={'body_normal':(1.5748/1.4773,1.6958/1.6212,1.6745/1.7078),
         'body_run':(1,1.5312/1.6212,1.6178/1.7078),
         'foot_run':(1.397,1.178),'hand_run':1.1329,
         'head_run_depth':3.8163/2.9309,'neck_normal':.20,'neck_run':.28764}
 for bone in data['bones']:
  n=bone['name'];piv=[];rot=[]
  for i,source in enumerate(maps):
   target=source.get(n,bone);p=list(target['pivot']);r=list(target.get('rotation',[0,0,0]))
   if n.startswith('wing_') and '__' not in n:
    # Maintain bilateral symmetry in the adaptation instead of inheriting
    # the Run donor's asymmetric authoring positions.
    if i>=2:p[1]=-2.5
   if n=='skull' and i>=3:p[1]-=ratios['neck_normal'] if i==3 else ratios['neck_run']
   piv.append(roundv(p));rot.append(roundv(r))
  bone['hero_run_pivots_052']=piv;bone['hero_run_rotations_052']=rot
  for mi,mesh in enumerate(bone.get('meshes',[])):
   targets=[]
   for ti,source in enumerate(maps):
    sb=source.get(n,bone);sm=next((m for m in sb.get('meshes',[]) if m.get('name')==mesh.get('name') and m.get('material')==mesh.get('material')),None)
    if sm is None and mi<len(sb.get('meshes',[])):sm=sb['meshes'][mi]
    vertices=copy.deepcopy(sm['vertices'] if sm and len(sm['vertices'])==len(mesh['vertices']) else mesh['vertices'])
    # Base mature silhouette follows the original Normal endpoint, while Run
    # specialization has its own reversible delta. Biological maturity is
    # never inferred from the reversible Run influence.
    if ti>=3:
     if n.startswith('body__'):
      sx,sy,sz=ratios['body_normal'] if ti==3 else ratios['body_run'];top=max(v[1] for v in vertices)
      vertices=[[v[0]*sx,top+(v[1]-top)*sy,v[2]*sz] for v in vertices]
     elif ti==4 and n.startswith(('foot_','feet_')) and '__' in n:
      lo=[min(v[a] for v in vertices) for a in range(3)];hi=[max(v[a] for v in vertices) for a in range(3)];cx=(lo[0]+hi[0])/2
      vertices=[[cx+(v[0]-cx)*ratios['foot_run'][0],v[1],hi[2]+(v[2]-hi[2])*ratios['foot_run'][1]] for v in vertices]
     elif ti==4 and n.startswith('arm_') and n.endswith('__cube_1'):
      center=[sum(v[a] for v in vertices)/len(vertices) for a in range(3)]
      vertices=[[center[a]+(v[a]-center[a])*(ratios['hand_run'] if a!=1 else 1.10) for a in range(3)] for v in vertices]
     elif n=='gradual_head_021' and sm:
      # Existing connected crest paths are the topology template. Replace their
      # endpoints below with source-informed crown paths.
      target=4 if ti==3 else 3;vertices=copy.deepcopy(sm.get('morph_vertices',[sm['vertices']]*9)[target])
    targets.append([roundv(v) for v in vertices])
   mesh['hero_run_vertices_052']=targets
   legacy=maps[2].get(n,bone)
   lm=next((m for m in legacy.get('meshes',[]) if m.get('name')==mesh.get('name') and m.get('material')==mesh.get('material')),None)
   if lm is None and mi<len(legacy.get('meshes',[])):lm=legacy['meshes'][mi]
   mesh['hero_run_legacy_vertices_052']=copy.deepcopy(lm.get('morph_vertices',[lm['vertices']]*9) if lm and len(lm['vertices'])==len(mesh['vertices']) else [mesh['vertices']]*9)
 # Source-informed connected, tapered crown paths, same 6 lanes x6 pieces.
 import sys
 sys.path.insert(0,str(ROOT/'art'))
 import build_gradual_evolution_021 as old
 skull=next(b for b in data['bones'] if b['name']=='skull__cube_0')['meshes'][0]
 head=next(b for b in data['bones'] if b['name']=='gradual_head_021')
 top=min(v[1] for v in skull['vertices'])
 young=[old.stem((0,-7.15,1),bend=(0,-6.9,.8),r=.33,tip=.10)]
 normal=[old.path([[0,-5.7,.8],[0,-7.5,1.1],[0,-8.1,2.6],[0,-7.8,3.8]],.38,.10)]
 # Original source has three broad curved/drooping prongs, not five quills.
 full=[old.path([[0,-5.7,.8],[0,-7.7,1.0],[0,-9.4,2.8],[0,-8.7,4.2]],.44,.25)]
 full+=old.pair(old.path([[.25,-7.4,1.8],[1.1,-8.4,2.2],[2.7,-8.5,3.2],[3.35,-6.7,4.0]],.55,.25))
 crown=[old.padded(old.styles('hero','normal')[0]),old.padded(old.styles('neutral','run')[0]),old.padded(young),old.padded(normal),old.padded(full)]
 # Dormant paired feelers sit on the upper stalk, matching the original's
 # tiny young nodes, so they grow out gradually instead of emerging through
 # the skull late in specialization.
 for endpoint in [2,3]:
  for lane,sign in [(1,1),(2,-1)]:
   crown[endpoint][lane]=old.path([[sign*.15,-7.3,1.8]]*4,.035,.035)
 for m in head['meshes']:
  _,_,slot,segment=m['name'].split('_');slot=int(slot);segment=int(segment)
  m['hero_run_vertices_052']=[[[round(float(x),6) for x in p] for p in old.stroke(style[slot],segment,top,1)] for style in crown]
  # Reserve two unused texels, preserving neutral stem/tip colors while Hero
  # alignment smoothly changes both to the family accent.
  uv=[63.75 if segment<4 else 63.25,31.75]
  for f in m['faces']:f['uv']=[uv[:] for _ in f['v']]
 # A closed shared-topology wedge recreates the original Run/Run rear head
 # point while keeping the eyes/front face exactly fixed.
 base=[[-3.48,-5.98,2.98],[3.48,-5.98,2.98],[3.48,-.02,2.98],[-3.48,-.02,2.98],[0,-3,2.97]]
 normalrear=copy.deepcopy(base);runrear=copy.deepcopy(base);runrear[4][2]=-3+6*ratios['head_run_depth']
 faces=[{'v':f,'uv':[[6.25,.25] for _ in f]} for f in [[0,3,2,1],[0,1,4],[1,2,4],[2,3,4],[3,0,4]]]
 data['bones'].append({'name':'hero_run_rear_052','parent':'skull','pivot':[0,0,0],'rotation':[0,0,0],
  'meshes':[{'name':'source_informed_rear_point','material':'official','shading':'flat','vertices':base,
    'hero_run_vertices_052':[copy.deepcopy(base),copy.deepcopy(base),copy.deepcopy(base),normalrear,runrear],'faces':faces}]})
 data.setdefault('attachment_extra_bones',{}).setdefault('skull',[]).append('hero_run_rear_052')
 # Dedicated prototype-only UV patches preserve shared original atlases.
 # Their dynamic colors retain inherited coats; only natural Two-tone Chao
 # acquire the reference's violet belly and blue/yellow limb bands.
 for bone in data['bones']:
  n=bone['name']
  for mesh in bone.get('meshes',[]):
   rectangle=(56,20,60,24) if n.startswith('arm_') and n.endswith('__cube_1') else (56,26,60,30) if n.startswith(('foot_','feet_')) and '__' in n else None
   for face in mesh['faces']:
    if n.startswith('body__') and all(abs(mesh['vertices'][v][2]-min(p[2] for p in mesh['vertices']))<1e-6 for v in face['v']):
     face['hero_run_uv_052']=[[52.25,28.25] for _ in face['v']]
    elif rectangle:
     u=[p[0] for p in face['uv']];v=[p[1] for p in face['uv']];du=max(u)-min(u);dv=max(v)-min(v);x0,y0,x1,y1=rectangle
     face['hero_run_uv_052']=[[x0+((p[0]-min(u))/du if du else .5)*(x1-x0),y0+((p[1]-min(v))/dv if dv else .5)*(y1-y0)] for p in face['uv']]
 data['hero_run_prototype_052']={'endpoints':list(NAMES),'canonical_uv_layout':'baby','shape_source':'SA2B ChaoMain.prs AL_RootObject type15, cuboid-style adaptation','ratios':ratios,'face_and_eyes':'canonical Baby topology; shared body/face/wardrobe anchors','crest_swatch_uv':[[63.75,31.75],[63.25,31.75]],'natural_two_tone_patches':{'belly_uv':[52.25,28.25],'hand_rect_uv':[56,20,60,24],'foot_rect_uv':[56,26,60,30]},'palette_reference':'Chao Island Hero Regular Two-tone Run photographs; authored adaptation, not extracted source pixels'}
 data['authored_gameplay_form']='hero_run_proto052'
 OUT.write_text(json.dumps(data,separators=(',',':'))+'\n')
 manifest={'version':'0.25.52','model':str(OUT.relative_to(ASSETS)),'sha256':digest(OUT),'protected_source_sha256':{f'models/entity/gameplay/chao_{k}.json':digest(MODELS/f'chao_{k}.json') for k in ['baby','hero','run','hero_run']},'endpoint_order':list(NAMES),'source_ratios':ratios,'source_mesh_imported':False}
 (ASSETS/'hero_run_morphology_052.json').write_text(json.dumps(manifest,indent=2)+'\n')
 print('Hero Run 052 canonical matched-topology asset generated; source models unchanged.')
if __name__=='__main__':author()
