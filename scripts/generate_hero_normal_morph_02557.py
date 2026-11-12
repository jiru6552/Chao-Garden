#!/usr/bin/env python3
"""Author Hero/Normal's paired blocky buds/stalks on the fixed original rig.

The nine mature corners were independently inspected in the live Chao Island
viewer. These are deliberately authored cuboids, not copied viewer geometry.
The older official Hero cuboids/halo supply construction vocabulary only.
"""
from pathlib import Path
import copy,hashlib,json,math
from generate_blocky_run_morph_02555 import pose,vertices,original_box,rotate

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
    # 0.25.58: each tip is still exactly TWO clean cuboids. White/inherited
    # lower necks change length/lean, while the colored upper cap changes its
    # aspect ratio and direction. The reference's rounded/tapered sections
    # are interpreted as angled rectangular blocks, never a new head mesh.
    # lowerLength,width,depth,angle; upperLength,width,depth,angle; baseX.
    profiles=[
      (.90,.65,.75,25, .80,.70,.80,25,.22),  # fresh: short paired buds
      (1.70,1.35,1.25,38,2.20,1.10,1.15,48,.25), # Run+Swim: broad long horns
      (1.65,1.45,1.35,38,2.35,1.35,1.20,52,.25), # Swim: wide white V roots
      (1.55,1.40,1.30,40,1.70,1.50,1.30,50,.25), # Power+Swim: thick horns
      (1.60,.90,.95,40,2.30,.65,.80,45,.28),     # Run: narrow long points
      (1.10,1.00,1.00,35,1.25,1.20,1.20,35,.30),# balanced: broad rounded buds
      (1.60,1.15,1.10,60,1.00,1.70,1.40,25,.35),# Power: bowed neck/broad cap
      (1.50,1.05,1.10,42,1.65,1.00,1.00,48,.28),# Run+Fly: shorter/broader point
      (1.55,.28,.40,35,.90,1.25,1.20,20,.45),      # Fly: thin stem/full small bulb
      (1.50,1.05,1.15,50,1.15,1.70,1.50,30,.32)]# Power+Fly: rising broad cap
    roots=[];caps=[]
    for length,width,depth,angle,upper,uw,ud,ua,bx in profiles:
        a=math.radians(angle);u=math.radians(ua)
        # Shared Z centers and Z-only rotations otherwise permit coincident
        # white-root/colored-cap front and back faces inside their overlap.
        # Keep the cap planes outside the root by at least0.04px on each side
        # for EVERY target, so convex age/axis blends cannot cross coplanarity.
        ud=max(ud,depth+.08)
        # The lower end remains slightly inside the unchanged skull. The cap
        # overlaps the upper end by0.16px; it never disconnects while blending.
        base=[bx,-5.80,.20]
        root=pose([base[0]+math.sin(a)*length/2,base[1]-math.cos(a)*length/2,base[2]],
                  [width,length,depth],[0,0,angle])
        neck=[base[0]+math.sin(a)*length,base[1]-math.cos(a)*length,base[2]]
        cap=pose([neck[0]+math.sin(u)*(upper/2-.16),neck[1]-math.cos(u)*(upper/2-.16),neck[2]],
                 [uw,upper,ud],[0,0,ua])
        roots.append(root);caps.append(cap)
    result=[]
    for sign,label in [(-1,'left'),(1,'right')]:
        def mirrored(values):
            poses=copy.deepcopy(values)
            for p in poses:p['center'][0]*=sign;p['rotation'][2]*=sign
            return poses
        result.append(feature(template,'hero_normal_root_'+label,mirrored(roots)))
        result.append(feature(template,'hero_normal_cap_'+label,mirrored(caps),True))
    return result


def wings(data):
    # Original wing planes have duplicated8vertices but zero thickness, and
    # their back island is transparent in every supplied coat. Close only
    # these Hero/Normal wings into thin blocks, preserve their original front
    # UVs and mirror the back onto that same visible artwork. No texture alpha
    # is changed. The root/outer semantic direction remains original.
    # span,height,pitch,yaw,slope; right wing, left mirrored. The mesh
    # angles stay at0 so every blended inner-edge midpoint stays EXACTLY
    # [0,.25,0]; original parent yaw and animation still rotate the wings.
    # Span/height/color make their variant silhouettes distinct. Targets follow
    # the existing fresh +3x3 mature lattice, not a switched set of models.
    profiles=[
      (3.00,2.00,0,0,0),
      (4.15,1.75,0,0,0),   # Run+Swim: long broad blade
      (3.90,2.05,0,0,0),   # Swim: broad, extended lemon-tipped wing
      (3.25,2.20,0,0,0),   # Power+Swim: compact broad blade
      (4.30,1.25,0,0,0),   # Run: long slender horizontal blade
      (3.20,1.90,0,0,0),   # balanced: compact white/gold blade
      (2.80,2.20,0,0,0),  # Power: short stocky gold blade
      (4.00,1.60,0,0,0),   # Run+Fly: intermediate slender blade
      (3.65,2.35,0,0,0),   # Fly: broad flat white/ochre blade
      (3.10,2.30,0,0,0)]  # Power+Fly: broad raised blade
    for bone in data['bones']:
        if bone['name'] not in ('wing_left__cube_0','wing_right__cube_0'):continue
        sign=-1 if 'left' in bone['name'] else 1
        for m in bone.get('meshes',[]):
            for key in ('morph_vertices','hero_run_vertices_052','hero_run_legacy_vertices_052'):m.pop(key,None)
            original=copy.deepcopy(m)
            for i,v in enumerate(m['vertices']):v[2]=-.08 if i<4 else .08
            visible=original['faces'][0]
            # Vertex-based mapping keeps white root and colored distal edge
            # correct on the mirrored back without copying a blank UV island.
            uv={i:visible['uv'][j] for j,i in enumerate(visible['v'])}
            m['faces'][1]['uv']=[copy.deepcopy(uv[i%4]) for i in m['faces'][1]['v']]
            front=visible['v']
            for a,b in zip(front,front[1:]+front[:1]):
                ids=[b,a,a+4,b+4]
                # Follow the adjacent front-edge alpha instead of painting an
                # opaque rim around its lower/distal pixel cutout. Inset keeps
                # nearest sampling inside the existing3x2UV island at corners.
                edge_uv=[[max(16.125,min(18.875,uv[i%4][0])),
                          max(17.125,min(18.875,uv[i%4][1]))] for i in ids]
                m['faces'].append({'v':ids,'uv':edge_uv})
            poses=[]
            for span,height,pitch,yaw,slope in profiles:
                rotation=[pitch,sign*yaw,sign*slope]
                # Rotate about the original inner wing attachment edge; the
                # parent's original shared pivot and all animations are exact.
                center=rotate([sign*span/2,0,0],rotation)
                center[1]+=.25
                poses.append(pose(center,[span,height,.16],rotation))
            m['vertices']=vertices(m,poses[0]);m['hero_normal_region_057']='wing'
            targets(m,poses)

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
    limbs(data);wings(data);halo(data,template)
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
      'approximation':'Fixed square head plus paired white/inherited roots and variant-specific colored caps (4 closed cuboids), square four-bar halo, compact limb deltas and two thin closed wing blocks. Smooth leaf/cup/point profiles are interpreted as clean angled boxes; no base-head deformation.',
      'refinement_02558':'Two-section root/cap aspects and directions differ substantially by corner. Existing wing pivots/front UVs remain; formerly blank back UVs intentionally mirror the same visible front island, edges follow the adjacent original front-edge alpha with inset UVs (avoiding an opaque frame around the original pixel cutout). Wing span/height develop continuously; original wing angles and exact local hinge remain with age/axes.',
      'root_cap_depth_policy_02558':{'minimum_cap_depth_excess':.08,'minimum_front_back_plane_clearance':.04,'scope':'Every fresh/mature target uses the same root/cap Z center and Z-only rotations; convex blends retain separated parallel front/back surfaces without changing the XY silhouette.'},
      'marker_anchor':'Originalemotion_dots/emotion_dot pivots/tilt remain; barcenters vary smoothly byatmost0.4modelpixels betweenfresh/maturecorners.',
      'provisional_rates':'960netcurrentlevelbarprogress peropposedaxis; independentmaturity; nooriginalgametimingclaim.'}
    actual={b['name']:b for b in data['bones']}
    for b in original['bones']:
        name=b['name'];assert actual[name]['pivot']==b['pivot'] and actual[name]['rotation']==b['rotation']
        if name.startswith(('skull','expression_')):assert actual[name]==b,name
        if name.startswith('emotion_') and name!='emotion_dot__cube_0':assert actual[name]==b,name
    OUT.write_text(json.dumps(data,separators=(',',':'))+'\n')
    refs=['chao_hero.geo.json','chao_hero_fly.geo.json','chao_hero_power.geo.json']
    manifest={'version':'0.25.58','model':str(OUT.relative_to(ASSETS)),'sha256':sha(OUT),'source_model_sha256':sha(source),'target_names':TARGETS,
      'canonical_head_face_and_joint_pivots_unchanged':True,'shared_animation_attachment_rig':True,'source_geometry_copied':False,
      'construction_reference_sha256':{n:sha(ROOT/'art/imported_chao/models'/n) for n in refs}}
    (ASSETS/'hero_normal057.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('HeroNormal058 refinement generated: fixed head,4paired featureboxes,4bar halo,2thin closed wings,10targets, original shared rig.')

if __name__=='__main__':author()
