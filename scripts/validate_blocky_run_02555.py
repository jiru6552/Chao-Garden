#!/usr/bin/env python3
"""Independent source-asset checks for the fixed-head blocky Run prototype.

Checks the actual generated JSON against its original model and approved baby
geometry, then uses separating axes to check connections through dense blends.
No Minecraft, generator import, network, external package or private file needed.
"""
from pathlib import Path
import hashlib
import json
import math

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
MODEL=ASSETS/'models/entity/gameplay/chao_hero_run_proto055.json'
SOURCE=ASSETS/'models/entity/gameplay/chao_baby.json'
TARGETS=['neutral_baby','hero_baby','neutral_run_baby','hero_run_baby',
         'hero_run_young','hero_run_mature','hero_run_run_mature']
CORNERS={}
APPROVED={
    1:'1da3879efe3800d235051d6c833b76c8d14b62510504ad484c8c752c9f019c9d',
    2:'32cabab38a5983d48318ccdbed7df9c1fd99a85a2fbd2d04264822562095751a',
    3:'feffcdc5c3b4e5259f8dc1f57d9a22bed3f25c5526ccb6350e5337ca5c5ad523'}

def dot(a,b):return sum(x*y for x,y in zip(a,b))
def sub(a,b):return [x-y for x,y in zip(a,b)]
def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def rotate(v,angles):
    x,y,z=v
    rx,ry,rz=[math.radians(x) for x in angles]
    y,z=math.cos(rx)*y-math.sin(rx)*z,math.sin(rx)*y+math.cos(rx)*z
    x,z=math.cos(ry)*x+math.sin(ry)*z,-math.sin(ry)*x+math.cos(ry)*z
    return [math.cos(rz)*x-math.sin(rz)*y,math.sin(rz)*x+math.cos(rz)*y,z]

def blend(mesh,weights):
    channel=mesh['blocky_box_055']
    return {name:[sum(w*channel[name][i][axis] for i,w in enumerate(weights)) for axis in range(3)]
            for name in ('centers','sizes','rotations')}

def vertices(mesh,weights):
    p=blend(mesh,weights)
    key=id(mesh)
    if key not in CORNERS:
        channel=mesh['blocky_box_055'];basis=[rotate([int(a==i) for a in range(3)],channel['rotations'][0]) for i in range(3)]
        result=[]
        for vertex in mesh['vertices']:
            delta=sub(vertex,channel['centers'][0]);corner=[dot(delta,basis[a])/channel['sizes'][0][a] for a in range(3)]
            assert all(abs(abs(x)-.5)<1e-6 for x in corner),'Reference corner mismatch'
            result.append([math.copysign(.5,x) for x in corner])
        CORNERS[key]=result
    return [[round(p['centers'][a]+q,9) for a,q in enumerate(rotate(
        [corner[a]*p['sizes'][a] for a in range(3)],p['rotations']))] for corner in CORNERS[key]]

def overlaps(a,b):
    # Independent OBB separating-axis test: three axes each plus nine crosses.
    aa=[sub(a[i],a[0]) for i in (1,3,4)];bb=[sub(b[i],b[0]) for i in (1,3,4)]
    for axis in aa+bb+[cross(x,y) for x in aa for y in bb]:
        length=math.sqrt(dot(axis,axis))
        if length<1e-10:continue
        axis=[x/length for x in axis];x=[dot(v,axis) for v in a];y=[dot(v,axis) for v in b]
        if min(max(x),max(y))-max(min(x),min(y))<1e-6:return False
    return True

def contained(points,box):
    axes=[sub(box[i],box[0]) for i in (1,3,4)]
    for axis in axes:
        length=math.sqrt(dot(axis,axis));axis=[x/length for x in axis]
        bounds=[dot(v,axis) for v in box]
        if any(dot(p,axis)<=min(bounds)+1e-7 or dot(p,axis)>=max(bounds)-1e-7 for p in points):return False
    return True

def connected(parts,base):
    reached={i for i,p in enumerate(parts) if overlaps(p,base)}
    while True:
        extra={i for i in range(len(parts)) if i not in reached and any(overlaps(parts[i],parts[j]) for j in reached)}
        if not extra:return len(reached)==len(parts)
        reached|=extra

def volume(points,faces):
    value=0
    for face in faces:
        ids=face['v'];a=points[ids[0]]
        for i in range(1,len(ids)-1):value+=dot(a,cross(points[ids[i]],points[ids[i+1]]))/6
    return value

def check_closed_box(mesh):
    assert len(mesh['vertices'])==8 and len(mesh['faces'])==6,mesh['name']
    edges={}
    for face in mesh['faces']:
        ids=face['v'];assert len(ids)==4 and len(face['uv'])==4
        for a,b in zip(ids,ids[1:]+ids[:1]):edges[(a,b)]=edges.get((a,b),0)+1
    assert len(edges)==24 and all(count==1 and edges.get((b,a))==1 for (a,b),count in edges.items()),mesh['name']
    assert volume(mesh['vertices'],mesh['faces'])>1e-7,mesh['name']
    for channel in ('centers','sizes','rotations'):
        values=mesh['blocky_box_055'][channel]
        assert len(values)==7 and all(len(v)==3 for v in values)
        assert all(math.isfinite(x) for v in values for x in v)
    assert all(x>=.01 for v in mesh['blocky_box_055']['sizes'] for x in v)
    normal=vertices(mesh,[1,0,0,0,0,0,0])
    assert max(abs(a-b) for va,vb in zip(normal,mesh['vertices']) for a,b in zip(va,vb))<1e-7,mesh['name']

def fingerprint(meshes,weights):
    text=';'.join(m['name']+':'+','.join(f'{0 if abs(q)<.0000005 else q:.6f}' for v in vertices(m,weights) for q in v)
                  for m in sorted(meshes,key=lambda m:m['name']))
    return hashlib.sha256(text.encode()).hexdigest()

def check_compatible_axes(data,source):
    """Verify semantic bound transfer and require actual nonzero axes.

    This catches the prior no-op lattice: merely retaining nine arrays is
    insufficient if every profile has the same positions.
    """
    adult_path=ASSETS/'models/entity/gameplay/chao_hero_run.json'
    adult={b['name']:b for b in json.loads(adult_path.read_text())['bones']}
    canonical={b['name']:b for b in source['bones']};count=0;profiles={i:[] for i in (1,4,5,7)}
    def bounds(points):
        lo=[min(v[a] for v in points) for a in range(3)];hi=[max(v[a] for v in points) for a in range(3)]
        return [(a+b)/2 for a,b in zip(lo,hi)],[b-a for a,b in zip(lo,hi)]
    for bone in data['bones']:
        name=bone['name']
        if not name.startswith(('body__','arm_','foot_','wing_','tail__')):continue
        if name not in adult or name not in canonical:continue
        for mesh in bone.get('meshes',[]):
            donor=[m for m in adult[name].get('meshes',[]) if m.get('name')==mesh.get('name') and m.get('material')==mesh.get('material')]
            own=[m for m in canonical[name].get('meshes',[]) if m.get('name')==mesh.get('name') and m.get('material')==mesh.get('material')]
            if len(donor)!=1 or len(own)!=1 or 'morph_vertices' not in donor[0]:continue
            vertices=own[0]['vertices'];own_center,own_size=bounds(vertices)
            old_targets=donor[0]['morph_vertices'];center,size=bounds(old_targets[4])
            assert mesh['morph_vertices'][4]==vertices,(name,'Canonical center profile')
            for target,old in enumerate(old_targets):
                target_center,target_size=bounds(old)
                for vi,vertex in enumerate(vertices):
                    for axis in range(3):
                        fraction=(vertex[axis]-own_center[axis])/own_size[axis] if own_size[axis]>1e-9 else 0
                        expected=vertex[axis]+target_center[axis]-center[axis]+fraction*(target_size[axis]-size[axis])
                        assert abs(mesh['morph_vertices'][target][vi][axis]-expected)<1e-8,(name,target,vi,axis)
            for target in profiles:profiles[target].extend(x for v in mesh['morph_vertices'][target] for x in v)
            count+=1
    assert count>0
    assert len({tuple(round(x,6) for x in values) for values in profiles.values()})==4,'Swim/Fly/Power geometry is a no-op'
    return hashlib.sha256(adult_path.read_bytes()).hexdigest()

def main():
    data=json.loads(MODEL.read_text());source=json.loads(SOURCE.read_text())
    bones={b['name']:b for b in data['bones']};old={b['name']:b for b in source['bones']}
    meta=data['blocky_run_path_055'];assert meta['targets']==TARGETS
    for name in ('skull','skull__cube_0','skull__cube_1','skull__cube_2','skull__cube_3'):
        assert bones[name]==old[name],f'Original head/face changed: {name}'
    assert data['head_decorations']==source['head_decorations']
    head=bones['skull__cube_0']['meshes'][0]['vertices']
    assert [[min(v[a] for v in head),max(v[a] for v in head)] for a in range(3)]==[[-3.5,3.5],[-6.0,0.0],[-3.0,3.0]]
    original_uv=[uv for b in source['bones'] for m in b.get('meshes',[]) for f in m['faces'] for uv in f['uv']]
    assert max(v for u,v in original_uv)<31.75,'Feature swatches overlap original UV use'
    assert meta['reserved_pixels']=={'feature_body':[127,63],'feature_tip':[126,63]}
    legacy=bones['blocky_legacy_tuft_055'];assert legacy['meshes']==old['gradual_head_021']['meshes']
    assert legacy['parent']=='gradual_head_021'
    for shift in legacy['morphology_pivots_055'][1:]:
        assert all(-6<v[1]+shift[1]<0 and -3.5<v[0]+shift[0]<3.5 and -3<v[2]+shift[2]<3
                   for m in legacy['meshes'] for v in m['vertices'])
    features=bones['gradual_head_021']['meshes'];assert len(features)==16
    boxes=[m for b in data['bones'] for m in b.get('meshes',[]) if 'blocky_box_055' in m]
    for mesh in boxes:check_closed_box(mesh)
    for target,expected in APPROVED.items():
        weights=[0]*7;weights[target]=1
        selected=[m for m in features if (target==1 and m['name'] in ('hero_left_bud','hero_right_bud'))
                  or (target==2 and m['name'].startswith('neutral_run_'))
                  or (target==3 and m['name'].startswith('hero_run_stem_'))]
        assert fingerprint(selected,weights)==expected,f'Approved baby endpoint changed: {TARGETS[target]}'
    frames=0
    for adult in (False,True):
        for ai in range(21):
            for bi in range(21):
                a,b=ai/20,bi/20;weights=[0]*7
                if adult:weights[4:]=[1-a,a*(1-b),a*b]
                else:weights[:4]=[(1-a)*(1-b),a*(1-b),(1-a)*b,a*b]
                points=[vertices(m,weights) for m in features]
                assert connected(points,head),(adult,a,b,'Disconnected feature')
                for mesh,v in zip(features,points):
                    if 'front_nub_' in mesh['name']:
                        assert max(p[1] for p in v)<-3.7 and min(p[2] for p in v)>-3,'Nub eye clearance'
                frames+=1
    # Adult dormant fan pieces are fully inside the positive-volume stalk.
    stem=next(m for m in features if m['name']=='hero_run_stem_root')
    for target in (4,5):
        weights=[0]*7;weights[target]=1;stalk=vertices(stem,weights)
        for mesh in features:
            if mesh['name'].startswith('hero_run_fan_'):assert contained(vertices(mesh,weights),stalk)
    # Compact original limb topology stays fixed in baby corners. Every foot
    # target preserves its original local floor plane, including growth.
    for name in ('body__cube_0','arm_left__cube_1','arm_right__cube_1','foot_left__cube_0','foot_right__cube_0'):
        mesh=bones[name]['meshes'][0];original=old[name]['meshes'][0]['vertices']
        for target in range(4):
            weights=[0]*7;weights[target]=1;assert vertices(mesh,weights)==original
        if name.startswith('foot'):
            for target in range(7):
                weights=[0]*7;weights[target]=1
                assert abs(max(v[1] for v in vertices(mesh,weights))-max(v[1] for v in original))<1e-7
    halo=[bones['emotion_dot__cube_0']['meshes'][0]]+[bones['blocky_halo_055_'+str(i)]['meshes'][0] for i in range(1,4)]
    for target in range(7):
        weights=[0]*7;weights[target]=1;parts=[vertices(m,weights) for m in halo]
        if target<4:assert all(contained(p,parts[0]) for p in parts[1:])
        else:assert connected(parts[1:],parts[0]),'Disconnected halo'
    manifest=json.loads((ASSETS/'hero_run055.json').read_text())
    assert manifest['compatible_axis_source_model_sha256']==check_compatible_axes(data,source)
    assert manifest['sha256']==hashlib.sha256(MODEL.read_bytes()).hexdigest()
    assert manifest['source_model_sha256']==hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    print(f'Blocky Run055: {len(boxes)} closed positive boxes, {frames} connected blends, exact approved baby endpoints, fixed head/eyes, floor and halo checks passed.')

if __name__=='__main__':main()
