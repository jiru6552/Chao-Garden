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

def bounds(points):
    return [(min(p[a] for p in points),max(p[a] for p in points)) for a in range(3)]

def endpoint(mesh,target):
    w=[0]*10;w[target]=1
    return vertices(mesh,w)

def principal_ends(mesh,points):
    # These boxes retain the original skull template's upper/lower faces.
    # Inspect their evaluated face centers, not the authored angle constants.
    # Local -Z is forward, +Z rearward, and more-negative Y is higher.
    return [
        [sum(points[i][a] for i in mesh['faces'][face]['v'])/4 for a in range(3)]
        for face in (4,5)]

def corrected_silhouettes(features):
    by_name={m['name']:m for m in features}
    root=by_name['neutral_crown_root'];tip=by_name['neutral_crown_tip']
    apex=by_name['neutral_fly_hook_tip']
    su,sd=principal_ends(root,endpoint(root,2))
    tu,td=principal_ends(tip,endpoint(tip,2))
    assert 2.3<sd[2]<3.1 and su[2]>4,'Swim lower piece must rise beyond the rear roof edge'
    assert su[1]<sd[1]-1,'Swim rear stalk does not rise'
    assert tu[1]<td[1]-1.2 and tu[2]<td[2]-.25,'Swim short upper piece must turn up and slightly forward'
    assert abs(tu[1]-td[1])>abs(tu[2]-td[2])*2.5,'Swim upper piece stayed a horizontal bar'
    assert math.dist(su,td)<.4,'Swim two-piece centerline disconnected'
    power_root=endpoint(root,6);power_tip=endpoint(tip,6)
    rb=bounds(power_root);tb=bounds(power_tip)
    assert rb[0][1]-rb[0][0]>=3,'Power shoulder remained a thin stalk'
    pu,pd=principal_ends(root,power_root);cu,cd=principal_ends(tip,power_tip)
    assert pu[1]<pd[1]-1.2 and pu[2]<pd[2]-.9,'Power lower piece must rise forward'
    assert cu[1]<cd[1]-1.3 and cu[2]>cd[2]+.75,'Power short upper piece must curl back and up'
    assert abs(cu[1]-cd[1])>abs(cu[2]-cd[2])*1.4,'Power upper curl stayed a horizontal bar'
    assert math.dist(pu,cd)<.4,'Power two-piece centerline disconnected'
    cone=[endpoint(m,3) for m in (root,tip,apex)]
    cb=[bounds(v) for v in cone]
    widths=[v[0][1]-v[0][0] for v in cb]
    assert widths[0]>=5.5 and cb[0][2][1]-cb[0][2][0]>=4.5,'Power+Swim crown must cover most of the original roof'
    assert widths[0]>widths[1]*1.5 and widths[1]>widths[2]*2,'Power+Swim crown does not taper in three clean steps'
    assert widths[2]<=1.25,'Power+Swim apex remained a broad cap'
    centers=[sum(p[1] for p in v)/8 for v in cone]
    assert centers[0]>centers[1]>centers[2] and cb[2][1][0]<-9,'Power+Swim crown did not rise to an apex'
    # Fully dormant third targets remain buried, rather than floating nearby.
    for target in (0,1,2,4,5,6,7,9):
        for p in endpoint(apex,target):
            assert -3.5<p[0]<3.5 and -6<p[1]<0 and -3<p[2]<3,'Dormant feature exposed outside original head'

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
    corrected_silhouettes(features)
    frames=0;minimum_marker_gap=999
    # The actual default orb is a two-pixel cube around skull-localY=-11,
    # tilted -22.5deg. Preserve all marker joints and clear its lowest vertex.
    orb_bottom=-11+math.cos(math.radians(22.5))+math.sin(math.radians(22.5))
    for age_i in range(21):
        age=age_i/20
        for yi in range(21):
            for xi in range(21):
                rp=xi/10-1;sf=yi/10-1;w=weights(age,rp,sf)
                assert abs(sum(w)-1)<1e-9 and min(w)>=0
                v=[vertices(m,w) for m in features]
                assert connected(v,head),(age,rp,sf,'Disconnected crown')
                marker_gap=min(p[1] for box in v for p in box)-orb_bottom
                minimum_marker_gap=min(minimum_marker_gap,marker_gap)
                assert marker_gap>.3,(age,rp,sf,'Crown intersects the default tilted orb')
                for mesh in boxes:
                    p=vertices(mesh,w)
                    assert volume(p,mesh['faces'])>1e-8,(mesh['name'],age,rp,sf)
                    if mesh['name']=='neutral_fly_hook_tip':
                        # A dormant apex may stay deeper inside the opaque
                        # skull during mixed Swim/Power development. Keep its
                        # entire X/Z footprint strictly inside the skull: it
                        # can emerge only through the roof, never the face or
                        # side walls, even while its buried end is lower down.
                        assert all(-3.5<x[0]<3.5 and -3<x[2]<3 and x[1]<0 for x in p),'Apex crosses the original facial/side envelope'
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
    print(f'Neutral056 refinement058: {len(boxes)} closed positive boxes, {frames} connected age/axis blends, fixed original head/face/rig, rear-rising Swim stalk with short upright/forward tip, forward-then-back upright Power hook, three-step Power+Swim taper, anchored soles and forehead clearance passed; minimum default tilted-orb gap={minimum_marker_gap:.9f} model pixels.')

if __name__=='__main__':main()
