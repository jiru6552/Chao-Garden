#!/usr/bin/env python3
"""Independent rigid-box, connection and real tilted-halo clearance checks.

No Minecraft, network, generator import or private screenshot dependency.
Uses the retained independently authored separating-axis implementation.
"""
from pathlib import Path
import copy,hashlib,json,math
from validate_blocky_run_02555 import vertices,connected,volume,overlaps,rotate
ROOT=Path(__file__).resolve().parents[1];ASSETS=ROOT/'src/main/resources/assets/chao_garden'
MODEL=ASSETS/'models/entity/gameplay/chao_hero_normal_proto057.json';SOURCE=ASSETS/'models/entity/gameplay/chao_baby.json'

def weights(age,rp,sf):return [1-age]+[age*max(0,1-abs(rp-x))*max(0,1-abs(sf-y)) for y in (-1,0,1) for x in (-1,0,1)]
def main():
    data=json.loads(MODEL.read_text());source=json.loads(SOURCE.read_text());bones={b['name']:b for b in data['bones']};old={b['name']:b for b in source['bones']}
    for name,bone in old.items():
        assert bones[name]['pivot']==bone['pivot'] and bones[name]['rotation']==bone['rotation'],name+' rig changed'
        if name.startswith(('skull','expression_')) or name.startswith('emotion_') and name!='emotion_dot__cube_0':assert bones[name]==bone,name
    assert data['head_decorations']==source['head_decorations']
    head=bones['skull__cube_0']['meshes'][0]['vertices'];assert [[min(v[a] for v in head),max(v[a] for v in head)] for a in range(3)]==[[-3.5,3.5],[-6.0,0.0],[-3.0,3.0]]
    boxes=[];features=[];halo=[]
    for bone in data['bones']:
        for original in bone.get('meshes',[]):
            if 'hero_normal_box_057' not in original:continue
            m=copy.deepcopy(original);m['blocky_box_055']=m['hero_normal_box_057'];boxes.append(m)
            if bone['name']=='gradual_head_021':features.append(m)
            if bone['name']=='emotion_dot__cube_0' or bone['name'].startswith('hero_normal_halo_'):halo.append(m)
            assert len(m['vertices'])==8 and len(m['faces'])==6,m['name'];edges={}
            for face in m['faces']:
                ids=face['v'];assert len(ids)==4 and len(face['uv'])==4
                for a,b in zip(ids,ids[1:]+ids[:1]):edges[(a,b)]=edges.get((a,b),0)+1
            assert len(edges)==24 and all(n==1 and edges.get((b,a))==1 for (a,b),n in edges.items()),m['name']
            for field in ('centers','sizes','rotations'):
                value=m['hero_normal_box_057'][field];assert len(value)==10 and all(len(v)==3 for v in value);assert all(math.isfinite(x) for v in value for x in v)
            assert all(x>=.01 for v in m['hero_normal_box_057']['sizes'] for x in v)
            expected=vertices(m,[1]+[0]*9);assert max(abs(a-b) for va,vb in zip(expected,m['vertices']) for a,b in zip(va,vb))<1e-7,m['name']
    assert len(features)==4 and len(halo)==4 and len(boxes)==13
    dot=bones['emotion_dot'];group=bones['emotion_dots']
    def halo_in_skull(points):
        result=[]
        for p in points:
            p=rotate(p,dot['rotation']);p=[p[a]+dot['pivot'][a] for a in range(3)];p=rotate(p,group['rotation']);result.append([p[a]+group['pivot'][a] for a in range(3)])
        return result
    frames=0;minimum=999
    for age in (0,.25,.5,.75,1):
        for yi in range(21):
            for xi in range(21):
                rp=xi/10-1;sf=yi/10-1;w=weights(age,rp,sf);assert abs(sum(w)-1)<1e-9 and min(w)>=0
                f=[vertices(m,w) for m in features];assert connected(f,head),(age,rp,sf,'Disconnected paired root/cap')
                h=[halo_in_skull(vertices(m,w)) for m in halo];assert connected(h[1:],h[0]),(age,rp,sf,'Disconnected halo bars')
                for bar in h:
                    assert not overlaps(bar,head),(age,rp,sf,'Halo touches original head')
                    for cap in f:assert not overlaps(bar,cap),(age,rp,sf,'Tilted halo intersects paired feature')
                # This vertical measure is informative only; true separation is
                # independently tested above rather than forcing tips below a plane.
                minimum=min(minimum,min(p[1] for shape in f for p in shape)-max(p[1] for shape in h for p in shape))
                for mesh in boxes:
                    p=vertices(mesh,w);assert volume(p,mesh['faces'])>1e-8,(mesh['name'],age,rp,sf)
                frames+=1
    for name in ('foot_left__cube_0','foot_right__cube_0'):
        m=copy.deepcopy(bones[name]['meshes'][0]);m['blocky_box_055']=m['hero_normal_box_057']
        for target in range(10):
            w=[0]*10;w[target]=1;assert abs(max(p[1] for p in vertices(m,w))-2)<1e-7,'Sole support plane moved'
    manifest=json.loads((ASSETS/'hero_normal057.json').read_text());assert manifest['sha256']==hashlib.sha256(MODEL.read_bytes()).hexdigest();assert manifest['source_model_sha256']==hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    print(f'HeroNormal057: {len(boxes)} positive closed boxes, {frames} connected paired-feature/halo age-axis blends, fixed original head/face/rig, anchored soles; actual tilted-halo OBB separation passed (minimum vertical gap={minimum:.6f}px; negative alone would not imply intersection).')
if __name__=='__main__':main()
