#!/usr/bin/env python3
"""Independent fixed-head/closed-cuboid/connection checks for Neutral056.

Uses the prior independent separating-axis math, not the model generator.
No Minecraft, network, copied viewer geometry or private reference file needed.
"""
from pathlib import Path
import copy
import hashlib
import json
import math
from validate_blocky_run_02555 import vertices, connected, volume

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
MODEL=ASSETS/'models/entity/gameplay/chao_neutral_proto056.json'
SOURCE=ASSETS/'models/entity/gameplay/chao_baby.json'

def weights(age,rp,sf):
    return [1-age]+[age*max(0,1-abs(rp-x))*max(0,1-abs(sf-y)) for y in (-1,0,1) for x in (-1,0,1)]

def main():
    data=json.loads(MODEL.read_text());source=json.loads(SOURCE.read_text())
    bones={b['name']:b for b in data['bones']};old={b['name']:b for b in source['bones']}
    for name,bone in old.items():
        if name.startswith(('skull','expression_','emotion_')):assert bones[name]==bone,name
        assert bones[name]['pivot']==bone['pivot'] and bones[name]['rotation']==bone['rotation'],name+' rig changed'
    assert data['head_decorations']==source['head_decorations']
    head=bones['skull__cube_0']['meshes'][0]['vertices']
    assert [[min(v[a] for v in head),max(v[a] for v in head)] for a in range(3)]==[[-3.5,3.5],[-6.0,0.0],[-3.0,3.0]]
    boxes=[];features=[]
    for bone in data['bones']:
        for m in bone.get('meshes',[]):
            if 'neutral_box_056' not in m:continue
            m=copy.deepcopy(m);m['blocky_box_055']=m['neutral_box_056'];boxes.append(m)
            if bone['name']=='gradual_head_021':features.append(m)
            assert len(m['vertices'])==8 and len(m['faces'])==6,m['name']
            edges={}
            for face in m['faces']:
                ids=face['v'];assert len(ids)==4 and len(face['uv'])==4
                for a,b in zip(ids,ids[1:]+ids[:1]):edges[(a,b)]=edges.get((a,b),0)+1
            assert len(edges)==24 and all(n==1 and edges.get((b,a))==1 for (a,b),n in edges.items()),m['name']
            for field in ('centers','sizes','rotations'):
                values=m['neutral_box_056'][field]
                assert len(values)==10 and all(len(v)==3 for v in values)
                assert all(math.isfinite(x) for v in values for x in v)
            assert all(x>=.01 for v in m['neutral_box_056']['sizes'] for x in v)
            expected=vertices(m,[1]+[0]*9)
            assert max(abs(a-b) for va,vb in zip(expected,m['vertices']) for a,b in zip(va,vb))<1e-7
    assert len(features)==3 and len(boxes)==8
    frames=0
    for age in (0,.25,.5,.75,1):
        for yi in range(21):
            for xi in range(21):
                rp=xi/10-1;sf=yi/10-1;w=weights(age,rp,sf)
                assert abs(sum(w)-1)<1e-9 and min(w)>=0
                v=[vertices(m,w) for m in features]
                assert connected(v,head),(age,rp,sf,'Disconnected crown')
                for mesh in boxes:
                    p=vertices(mesh,w)
                    assert volume(p,mesh['faces'])>1e-8,(mesh['name'],age,rp,sf)
                    if mesh['name']=='neutral_fly_hook_tip':
                        assert max(x[1] for x in p)<-5.4,'Forehead hook intrudes on face'
                frames+=1
    for name in ('foot_left__cube_0','foot_right__cube_0'):
        mesh=next(m for b in data['bones'] if b['name']==name for m in b['meshes'])
        mesh=copy.deepcopy(mesh);mesh['blocky_box_055']=mesh['neutral_box_056']
        for target in range(10):
            w=[0]*10;w[target]=1
            assert abs(max(p[1] for p in vertices(mesh,w))-2)<1e-7,'Sole support plane moved'
    manifest=json.loads((ASSETS/'neutral056.json').read_text())
    assert manifest['sha256']==hashlib.sha256(MODEL.read_bytes()).hexdigest()
    assert manifest['source_model_sha256']==hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    print(f'Neutral056: {len(boxes)} closed positive boxes, {frames} connected age/axis blends, fixed original head/face/rig, anchored soles and forehead clearance passed.')

if __name__=='__main__':main()
