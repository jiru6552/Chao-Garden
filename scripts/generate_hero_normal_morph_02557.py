#!/usr/bin/env python3
"""Author Hero/Normal's paired blocky buds/stalks on the fixed original rig.

The nine mature corners were independently inspected in the live Chao Island
viewer. These are deliberately authored cuboids, not copied viewer geometry.
The older official Hero cuboids/halo supply construction vocabulary only.
"""
from pathlib import Path
import copy,hashlib,json
from generate_blocky_run_morph_02555 import pose,vertices,original_box

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
MODELS=ASSETS/'models/entity/gameplay'
OUT=MODELS/'chao_hero_normal_proto057.json'
TARGETS=['hero_normal_young','run_swim','swim','power_swim','run','normal','power','run_fly','fly','power_fly']
BODY_UV=[63.75,31.75];TIP_UV=[63.25,31.75];MARKER_UV=[58.5,1.5]

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def rounded(v):return [round(float(x),9) for x in v]
def targets(mesh,poses):
    assert len(poses)==10
    mesh['hero_normal_box_057']={k:[rounded(p[q]) for p in poses] for k,q in [('centers','center'),('sizes','size'),('rotations','rotation')]}
    return mesh

def feature(template,name,poses,tip=False,marker=False):
    m=copy.deepcopy(template)
    for key in ('morph_vertices','hero_run_vertices_052','hero_run_legacy_vertices_052'):m.pop(key,None)
    m['name']=name;m['vertices']=vertices(template,poses[0]);m['hero_normal_region_057']='marker' if marker else 'crown_tip' if tip else 'crown_root'
    uv=MARKER_UV if marker else TIP_UV if tip else BODY_UV
    for face in m['faces']:face['uv']=[uv[:] for _ in face['v']]
    return targets(m,poses)

def paired_features(template):
    # x,y,z,sizeX,sizeY,sizeZ,leanBackX,divergeZ for the RIGHT branch;
    # the LEFT is a geometric mirror. One white root + one colored cap each.
    # Normal centered buds and pure-Fly caps remain shorter than Run stalks.
    roots=[
      (.40,-6.05,.20,.65,.75,.75,-2,25),
      (.60,-6.45,.35,1.10,1.25,1.20,-8,35),
      (.55,-6.40,.20,1.25,1.10,1.20,-5,40),
      (.50,-6.40,.20,1.30,1.10,1.20,-4,35),
      (.65,-6.40,.25,1.0,1.15,1.0,-8,35),
      (.55,-6.30,.20,.95,1.0,1.0,-3,25),
      (.65,-6.30,.10,1.10,.95,1.1,0,40),
      (.60,-6.35,.20,1.05,1.0,1.1,-5,40),
      (.55,-6.20,.05,.50,.85,.60,0,35),
      (.60,-6.40,.10,1.10,1.15,1.15,-2,35)]
    tips=[
      (.75,-6.65,.25,.65,.75,.80,-3,25),
      (1.35,-7.65,.65,.85,1.85,1.05,-14,40),
      (1.25,-7.45,.40,1.10,1.55,1.20,-8,45),
      (1.20,-7.35,.25,1.40,1.40,1.35,-5,40),
      (1.35,-7.55,.50,.65,1.80,.95,-12,35),
      (1.05,-7.00,.30,1.0,1.10,1.15,-5,30),
      (1.20,-7.00,.10,1.35,1.0,1.30,0,30),
      (1.25,-7.15,.40,1.05,1.25,1.25,-8,45),
      (1.00,-6.90,.05,.85,.85,.95,0,35),
      (1.25,-7.20,.15,1.40,1.20,1.40,-3,35)]
    result=[]
    for sign,label in [(-1,'left'),(1,'right')]:
        def mirrored(values):return [pose([sign*x,y,z],[sx,sy,sz],[rx,0,sign*rz]) for x,y,z,sx,sy,sz,rx,rz in values]
        result.append(feature(template,'hero_normal_root_'+label,mirrored(roots)))
        result.append(feature(template,'hero_normal_cap_'+label,mirrored(tips),True))
    return result

def limbs(data):
    selected={'body__cube_0','arm_left__cube_1','arm_right__cube_1','foot_left__cube_0','foot_right__cube_0'}
    for bone in data['bones']:
        name=bone['name']
        if name not in selected:continue
        for mesh in bone.get('meshes',[]):
            base=original_box(mesh);poses=[copy.deepcopy(base)]
            for y in (-1,0,1):
                for x in (-1,0,1):
                    run=max(0,-x);power=max(0,x);swim=max(0,-y);fly=max(0,y);p=copy.deepcopy(base)
                    if name.startswith('body'):scale=[1.025+.035*power,1.025+.02*fly,1.015+.025*swim]
                    elif name.startswith('arm'):scale=[1.025+.075*power+.025*swim,1.025+.075*power,1.025+.045*power]
                    else:scale=[1.025+.055*swim+.02*power,1.0+.025*power,1.025+.09*run+.035*swim]
                    p['size']=[a*b for a,b in zip(base['size'],scale)]
                    if name.startswith('foot'):
                        p['center'][1]=max(v[1] for v in mesh['vertices'])-p['size'][1]/2
                        p['center'][2]=max(v[2] for v in mesh['vertices'])-p['size'][2]/2
                    poses.append(p)
            targets(mesh,poses)

def halo(data,template):
    bones={b['name']:b for b in data['bones']};original=bones['emotion_dot__cube_0']['meshes'][0]
    # Older official Hero ring is5px across versus a7px head. This4.6px ring
    # stays close to that construction, with modest smooth per-form height.
    heights=[.90,.55,.65,.70,.65,.85,.85,.70,.95,.80]
    specs=[([0,0,-2.1],[4.6,.4,.4]),([0,0,2.1],[4.6,.4,.4]),([-2.1,0,0],[.4,.4,4.6]),([2.1,0,0],[.4,.4,4.6])]
    for index,(center,size) in enumerate(specs):
        poses=[pose([center[0],h,center[2]],size) for h in heights]
        if index==0:
            original['vertices']=vertices(original,poses[0]);targets(original,poses)
        else:
            mesh=feature(template,'hero_normal_halo_bar_'+str(index),poses,marker=True)
            data['bones'].append({'name':'hero_normal_halo_057_'+str(index),'parent':'emotion_dot','pivot':[0,0,0],'rotation':[0,0,0],'meshes':[mesh]})

def author():
    source=MODELS/'chao_baby.json';data=json.loads(source.read_text());original=copy.deepcopy(data);bones={b['name']:b for b in data['bones']}
    template=bones['skull__cube_0']['meshes'][0];bones['gradual_head_021']['meshes']=paired_features(template)
    limbs(data);halo(data,template)
    data['authored_gameplay_form']='hero_normal_proto057'
    data['hero_normal_path_057']={
      'targets':TARGETS,'canonical_uv_layout':'baby','original_head_box':[7,6,6],
      'weights':'fresh=1-maturity; mature index1+y*3+x = maturity*triangle(runPower,x-1)*triangle(swimFly,y-1)',
      'feature_parameter_order':'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
      'feature_body_uv':BODY_UV,'feature_tip_uv':TIP_UV,'halo_uv':MARKER_UV,
      'reserved_pixels':{'feature_body':[127,63],'feature_tip':[126,63]},
      'scope':'Ordinary Hero-aligned adult with locked Normal first evolution only; HeroRun andNeutralNormal identities/paths unchanged.',
      'reference':'Independently operated Chao Island viewer: all9Hero/Normal mature corners personally viewedfront/side/back; young andmidsettings observed. Older official Hero cuboids/halo provide construction vocabulary.',
      'source_geometry_copied':False,
      'approximation':'Fixedsquarehead pluspairedwhite roots/coloredcaps (4closedcuboids), square4barhalo, modestcompactlimbdeltas; originalplanarwings retained. Smoothleaf/cup/pointprofiles simplified tocleanangledboxes.',
      'marker_anchor':'Originalemotion_dots/emotion_dot pivots/tilt remain; barcenters vary smoothly byatmost0.4modelpixels betweenfresh/maturecorners.',
      'provisional_rates':'960netcurrentlevelbarprogress peropposedaxis; independentmaturity; nooriginalgametimingclaim.'}
    actual={b['name']:b for b in data['bones']}
    for b in original['bones']:
        name=b['name'];assert actual[name]['pivot']==b['pivot'] and actual[name]['rotation']==b['rotation']
        if name.startswith(('skull','expression_')):assert actual[name]==b,name
        if name.startswith('emotion_') and name!='emotion_dot__cube_0':assert actual[name]==b,name
    OUT.write_text(json.dumps(data,separators=(',',':'))+'\n')
    refs=['chao_hero.geo.json','chao_hero_fly.geo.json','chao_hero_power.geo.json']
    manifest={'version':'0.25.57','model':str(OUT.relative_to(ASSETS)),'sha256':sha(OUT),'source_model_sha256':sha(source),'target_names':TARGETS,
      'canonical_head_face_and_joint_pivots_unchanged':True,'shared_animation_attachment_rig':True,'source_geometry_copied':False,
      'construction_reference_sha256':{n:sha(ROOT/'art/imported_chao/models'/n) for n in refs}}
    (ASSETS/'hero_normal057.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('HeroNormal057 generated: fixedhead,4pairedfeatureboxes,4barhalo,10targets, compact shared rig.')

if __name__=='__main__':author()
