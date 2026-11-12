#!/usr/bin/env python3
"""Approved blocky baby -> Hero Run path on the unchanged original baby rig.

Added parts are rigid cuboids described by center/positive size/Euler targets.
No donor vertex indices, rounded surfaces, head deformation or visible swaps.
The renderer blends these parameters, not arbitrary deformed vertex clouds.
"""
from pathlib import Path
import copy
import hashlib
import json
import math

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
MODELS=ASSETS/'models/entity/gameplay'
OUT=MODELS/'chao_hero_run_proto055.json'
TARGETS=['neutral_baby','hero_baby','neutral_run_baby','hero_run_baby',
         'hero_run_young','hero_run_mature','hero_run_run_mature']
BODY_UV=[63.75,31.75]
TIP_UV=[63.25,31.75]
MARKER_UV=[58.5,1.5]

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def rounded(values):return [round(float(x),9) for x in values]
def pose(center,size,rotation=(0,0,0)):
    assert all(x>0 for x in size)
    return {'center':list(center),'size':list(size),'rotation':list(rotation)}
HIDDEN=pose([0,-3,0],[.1,.1,.1])

def rotate(point,rotation):
    x,y,z=point
    for axis,degrees in enumerate(rotation):
        c=math.cos(math.radians(degrees));s=math.sin(math.radians(degrees))
        if axis==0:y,z=c*y-s*z,s*y+c*z
        elif axis==1:x,z=c*x+s*z,-s*x+c*z
        else:x,y=c*x-s*y,s*x+c*y
    return [x,y,z]

def vertices(template,p):
    lo=[min(v[i] for v in template['vertices']) for i in range(3)]
    hi=[max(v[i] for v in template['vertices']) for i in range(3)]
    return [rounded([q+p['center'][i] for i,q in enumerate(rotate(
        [(.5 if v[a]==hi[a] else -.5)*p['size'][a] for a in range(3)],p['rotation']))])
        for v in template['vertices']]

def box_targets(mesh,poses):
    assert len(poses)==7
    mesh['blocky_box_055']={
        'centers':[rounded(p['center']) for p in poses],
        'sizes':[rounded(p['size']) for p in poses],
        'rotations':[rounded(p['rotation']) for p in poses]}
    return mesh

def box(template,name,poses,accent='none'):
    result=copy.deepcopy(template)
    for key in ('morph_vertices','hero_run_vertices_052','hero_run_legacy_vertices_052'):
        result.pop(key,None)
    result['name']=name
    result['vertices']=vertices(template,poses[0])
    for index,face in enumerate(result['faces']):
        uv=TIP_UV if accent=='all' or accent=='tip' and index in (1,4) else BODY_UV
        face['uv']=[uv[:] for _ in face['v']]
    result['blocky_region_055']='head_feature'
    return box_targets(result,poses)

def endpoint_part(template,name,target,p,accent='none',inactive=None):
    poses=[copy.deepcopy(inactive or HIDDEN) for _ in TARGETS]
    poses[target]=p
    return box(template,name,poses,accent)

def neutral_parts(template):
    specs=[
        ('neutral_run_crest_root',[0,-5.85,1.0],[2.2,1.5,3.6],[25,0,0],'none'),
        ('neutral_run_crest_tip',[0,-7.0,3.3],[1.35,.8,1.7],[10,0,0],'tip'),
        ('neutral_run_rear_left_root',[-2.2,-3.55,2.9],[2.3,2.8,2.0],[0,-20,0],'none'),
        ('neutral_run_rear_left_tip',[-3.1,-3.6,4.55],[1.45,1.8,1.8],[0,-7,0],'none'),
        ('neutral_run_rear_right_root',[2.2,-3.55,2.9],[2.3,2.8,2.0],[0,20,0],'none'),
        ('neutral_run_rear_right_tip',[3.1,-3.6,4.55],[1.45,1.8,1.8],[0,7,0],'none')]
    result=[]
    for n,c,s,r,a in specs:
        embedded=pose([0,-5.8,1],[.1,.1,.1]) if 'crest' in n else pose([
            -2.2 if 'left' in n else 2.2,-3.55,2.8],[.1,.1,.1])
        result.append(endpoint_part(template,n,2,pose(c,s,r),a,embedded))
    for sign,label in ((-1,'left'),(1,'right')):
        # Match the approved full nub exactly. Hidden targets keep positive
        # volume and remain inside the skull instead of toggling visibility.
        result.append(endpoint_part(template,'neutral_run_front_nub_'+label,2,
            pose([sign*3.45,-4.25,-2.1],[1.65,.6,1.0],[0,sign*15,sign*12]),'all',
            pose([sign*2.8,-4.25,-2.1],[.4,.3,.6])))
    return result

def hero_parts(template):
    result=[]
    for sign,label in ((-1,'left'),(1,'right')):
        result.append(endpoint_part(template,'hero_'+label+'_bud',1,
            pose([sign*.55,-6.2,1.1],[.6,.9,.65],[10,0,sign*12]),'tip',
            pose([sign*.55,-5.75,1.1],[.1,.1,.1])))
    root=[pose([0,-5.75,1],[.1,.1,.1]) for _ in TARGETS]
    tip=[pose([0,-5.75,1],[.1,.1,.1]) for _ in TARGETS]
    root[3]=pose([0,-6.15,1.0],[.8,1.05,.9],[20,0,0])
    tip[3]=pose([0,-6.8,1.4],[.55,.6,.65],[10,0,0])
    # Fresh adult is deliberately a short stalk, not the mature fan.
    root[4]=pose([0,-6.35,.75],[.7,1.25,.7],[10,0,0])
    tip[4]=pose([0,-7.1,1.05],[.45,.65,.45],[8,0,0])
    # Old Hero Run with Run centered: one arched rear-facing central horn.
    root[5]=pose([0,-6.6,1.05],[.75,1.9,.85],[15,0,0])
    tip[5]=pose([0,-7.6,2.05],[.65,.65,2.4],[-10,0,0])
    # Mature Hero/Run/Run rises above the skull then fans into three branches.
    root[6]=pose([0,-6.9,1.2],[.85,2.6,1.0],[18,0,0])
    tip[6]=pose([0,-8.1,2.6],[1.05,.8,2.6],[-10,0,0])
    result.extend([box(template,'hero_run_stem_root',root),box(template,'hero_run_stem_tip',tip,'tip')])
    for sign,label in ((-1,'left'),(1,'right')):
        for suffix,p,accent in [
            ('root',pose([sign*1.1,-8.15,2.0],[2.2,.85,1.0],[-5,-sign*20,sign*10]),'none'),
            ('tip',pose([sign*2.15,-7.75,2.75],[1.2,.65,.9],[-5,-sign*25,sign*30]),'tip')]:
            poses=[pose([0,-5.75,1],[.1,.1,.1]) for _ in TARGETS]
            # Adult dormant branches remain completely inside the existing
            # positive-volume stalk, then grow outward from its upper portion.
            poses[4]=pose([0,-6.35,.75],[.1,.1,.1])
            poses[5]=pose([0,-7.1,1.2],[.1,.1,.1])
            poses[6]=p
            result.append(box(template,'hero_run_fan_'+label+'_'+suffix,poses,accent))
    return result

def original_box(mesh):
    lo=[min(v[i] for v in mesh['vertices']) for i in range(3)]
    hi=[max(v[i] for v in mesh['vertices']) for i in range(3)]
    return pose([(a+b)/2 for a,b in zip(lo,hi)],[b-a for a,b in zip(lo,hi)])

def modest_limbs(data):
    for bone in data['bones']:
        name=bone['name']
        if name not in ('body__cube_0','arm_left__cube_1','arm_right__cube_1',
                        'foot_left__cube_0','foot_right__cube_0'):continue
        for mesh in bone.get('meshes',[]):
            base=original_box(mesh);poses=[copy.deepcopy(base) for _ in TARGETS]
            for index in (5,6):
                if name.startswith('body'):
                    scale=[1.03,1.03,1.02]
                elif name.startswith('arm'):
                    scale=[1.04]*3 if index==5 else [1.08]*3
                else:scale=[1.08,1.02,1.04] if index==5 else [1.2,1.05,1.12]
                poses[index]['size']=[a*b for a,b in zip(base['size'],scale)]
                if name.startswith('foot'):
                    # Keep the actual floor and rear ankle edge fixed.
                    poses[index]['center'][1]=max(v[1] for v in mesh['vertices'])-poses[index]['size'][1]/2
                    poses[index]['center'][2]=max(v[2] for v in mesh['vertices'])-poses[index]['size'][2]/2
            box_targets(mesh,poses)

def compatible_axes(data):
    """Transfer semantic box-bound DELTAS, never unrelated donor vertices.

    The unchanged original baby lattice has identical axis profiles. The old
    Hero Run adult supplies its already-authored wing/hand/foot influences;
    re-express each center/size delta on this canonical part's own corners.
    Runtime bounds the contribution and applies it only to adult non-heads.
    """
    source=MODELS/'chao_hero_run.json'
    adult={b['name']:b for b in json.loads(source.read_text())['bones']}
    for bone in data['bones']:
        name=bone['name']
        if not name.startswith(('body__','arm_','foot_','wing_','tail__')):continue
        donor=adult.get(name)
        if donor is None:continue
        for mesh in bone.get('meshes',[]):
            candidates=[m for m in donor.get('meshes',[]) if m.get('name')==mesh.get('name')
                        and m.get('material')==mesh.get('material')]
            if len(candidates)!=1:continue
            targets=candidates[0].get('morph_vertices')
            if targets is None or len(targets)!=9:continue
            own_lo=[min(v[a] for v in mesh['vertices']) for a in range(3)]
            own_hi=[max(v[a] for v in mesh['vertices']) for a in range(3)]
            base={'center':[(a+b)/2 for a,b in zip(own_lo,own_hi)],
                  'size':[b-a for a,b in zip(own_lo,own_hi)]}
            bounds=[]
            for vertices in targets:
                lo=[min(v[a] for v in vertices) for a in range(3)]
                hi=[max(v[a] for v in vertices) for a in range(3)]
                bounds.append(([(a+b)/2 for a,b in zip(lo,hi)],[b-a for a,b in zip(lo,hi)]))
            center,size=bounds[4];mapped=[]
            for target_center,target_size in bounds:
                values=[]
                for vertex in mesh['vertices']:
                    values.append(rounded([vertex[a]+target_center[a]-center[a]
                        +((vertex[a]-base['center'][a])/base['size'][a] if base['size'][a]>1e-9 else 0)
                         *(target_size[a]-size[a]) for a in range(3)]))
                mapped.append(values)
            # The center profile is exactly canonical, not an inherited adult
            # inflation/offset. All other profiles express bounded deltas only.
            mapped[4]=copy.deepcopy(mesh['vertices'])
            mesh['morph_vertices']=mapped
    return source

def halo(data,template):
    bones={b['name']:b for b in data['bones']}
    original=bones['emotion_dot__cube_0']['meshes'][0]
    base=original_box(original)
    poses=[copy.deepcopy(base) for _ in TARGETS]
    for index in (4,5,6):poses[index]=pose([0,0,-1.3],[2.95,.35,.35])
    box_targets(original,poses)
    for number,(center,size) in enumerate([([0,0,1.3],[2.95,.35,.35]),([-1.3,0,0],[.35,.35,2.95]),([1.3,0,0],[.35,.35,2.95])],1):
        targets=[pose([0,0,0],[.15,.15,.15]) for _ in TARGETS]
        for index in (4,5,6):targets[index]=pose(center,size)
        mesh=box(template,'halo_bar_'+str(number),targets)
        mesh.pop('blocky_region_055',None)
        for face in mesh['faces']:face['uv']=[MARKER_UV[:] for _ in face['v']]
        data['bones'].append({'name':'blocky_halo_055_'+str(number),'parent':'emotion_dot',
            'pivot':[0,0,0],'rotation':[0,0,0],'meshes':[mesh]})

def author():
    source=MODELS/'chao_baby.json';data=json.loads(source.read_text())
    original=copy.deepcopy(data);bones={b['name']:b for b in data['bones']}
    template=bones['skull__cube_0']['meshes'][0]
    features=bones['gradual_head_021']
    legacy=copy.deepcopy(features);legacy['name']='blocky_legacy_tuft_055';legacy['parent']='gradual_head_021'
    legacy['morphology_pivots_055']=[[0,0,0]]+[[0,2.5,0] for _ in range(6)]
    features['meshes']=neutral_parts(template)+hero_parts(template)
    data['bones'].append(legacy)
    modest_limbs(data)
    halo(data,template)
    axis_source=compatible_axes(data)
    data['authored_gameplay_form']='hero_run_proto055'
    data['blocky_run_path_055']={
        'targets':TARGETS,'canonical_uv_layout':'baby','original_head_box':[7,6,6],
        'fixed_head':'Original skull/eyes/facial panels bypass all legacy and prototype deformation.',
        'feature_parameter_order':'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv':BODY_UV,'feature_tip_uv':TIP_UV,'halo_uv':MARKER_UV,
        'reserved_pixels':{'feature_body':[127,63],'feature_tip':[126,63]},
        'baby_source':'Approved blocky addon-guided baby endpoint review; original head and face kept exact.',
        'adult_source':'Independently observed Chao Island viewer: short young stem, single old centered-Run horn, mature max-Run three-prong fan.',
        'source_geometry_copied':False,
        'provisional_rates':'Visual age/training inputs come from HeroRunMorphology052; no source-game timing is inferred from viewer sliders.',
        'compatible_axes':'Semantic adult Hero Run box-bound deltas mapped to canonical non-head corners; bounded at runtime, head/feature targets independent.',
        'compatible_axis_source_sha256':sha(axis_source)}
    # The whole original head and face records remain byte-equivalent JSON
    # values, including old targets. Runtime explicitly bypasses those targets.
    result={b['name']:b for b in data['bones']}
    for name in ('skull','skull__cube_0','skull__cube_1','skull__cube_2','skull__cube_3'):
        assert result[name]==next(b for b in original['bones'] if b['name']==name),name
    old_uv=[uv for b in original['bones'] for m in b.get('meshes',[]) for f in m['faces'] for uv in f['uv']]
    assert max(v for u,v in old_uv)<31.75
    assert min(v[1]+2.5 for m in legacy['meshes'] for v in m['vertices'])>-6
    assert max(v[1]+2.5 for m in legacy['meshes'] for v in m['vertices'])<0
    OUT.write_text(json.dumps(data,separators=(',',':'))+'\n')
    manifest={'version':'0.25.55','model':str(OUT.relative_to(ASSETS)),
        'sha256':sha(OUT),'source_model_sha256':sha(source),'target_names':TARGETS,
        'canonical_original_head_unchanged':True,'feature_meshes':len(features['meshes']),
        'feature_construction':'Closed positive-size rotated cuboids, parameter interpolation; original normal tuft retracts inside fixed head.',
        'reserved_pixels':data['blocky_run_path_055']['reserved_pixels'],
        'source_assets_unchanged':[str(source.relative_to(ROOT))],
        'compatible_axis_source_model_sha256':sha(axis_source),
        'shared_animation_and_attachment_rig':True}
    (ASSETS/'hero_run055.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('Blocky Run055 path generated: fixed original head,16 positive cuboid features, unchanged rig/face,4-bar marker.')

if __name__=='__main__':author()
