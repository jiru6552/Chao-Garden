#!/usr/bin/env python3
"""Blocky Neutral/Normal adult foundation: one fixed head and one crown.

The nine mature corners were visually inspected in the Chao Island viewer.
This deliberately authors simple cuboids, not copied viewer geometry. Original
addon models supply the construction vocabulary; their first-type crests are
not incorrectly substituted for the Normal-first-type specializations.
"""
from pathlib import Path
import copy
import hashlib
import json
from generate_blocky_run_morph_02555 import pose, vertices, original_box

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
MODELS=ASSETS/'models/entity/gameplay'
OUT=MODELS/'chao_neutral_proto056.json'
TARGETS=['neutral_normal_young','run_swim','swim','power_swim','run',
         'normal','power','run_fly','fly','power_fly']
BODY_UV=[63.75,31.75]
TIP_UV=[63.25,31.75]

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def rounded(v):return [round(float(x),9) for x in v]
def targets(mesh,poses):
    assert len(poses)==10
    mesh['neutral_box_056']={
        'centers':[rounded(p['center']) for p in poses],
        'sizes':[rounded(p['size']) for p in poses],
        'rotations':[rounded(p['rotation']) for p in poses]}
    return mesh

def crown(template,name,poses,tip=False):
    m=copy.deepcopy(template)
    for key in ('morph_vertices','hero_run_vertices_052','hero_run_legacy_vertices_052'):
        m.pop(key,None)
    m['name']=name;m['vertices']=vertices(template,poses[0]);m['neutral_region_056']='crown_tip' if tip else 'crown_root'
    for face in m['faces']:face['uv']=[(TIP_UV if tip else BODY_UV)[:] for _ in face['v']]
    return targets(m,poses)

def crests(template):
    # Fresh adulthood retains the original small upright tuft vocabulary.
    # Every endpoint is ONE connected crown. In particular, Normal/Run is
    # not the six-spiked Run-first-evolution form used by the baby prototype.
    root=[pose([0,-6.45,.75],[1.15,1.3,1.2],[-10,0,0]),
          pose([0,-6.65,1.1],[1.25,2.1,2.0],[-20,0,0]),
          pose([0,-6.6,3.35],[1.1,2.15,.9],[-55,0,0]),
          pose([0,-6.35,.45],[5.8,1.5,5.0]),
          pose([0,-6.65,1.3],[1.5,2.1,2.8],[-25,0,0]),
          pose([0,-6.5,.6],[1.4,1.55,1.6],[-10,0,0]),
          pose([0,-6.6,-.1],[3.2,1.85,1.2],[40,0,0]),
          pose([0,-6.15,1.0],[1.7,1.0,2.0],[-12,0,0]),
          pose([0,-6.5,.2],[1.45,1.55,1.45],[20,0,0]),
          pose([0,-6.25,.6],[1.55,1.0,1.6],[15,0,0])]
    tip=[pose([0,-7.35,1.0],[.8,1.1,.9],[-12,0,0]),
         pose([0,-7.6,2.05],[.7,1.1,2.8],[-24,0,0]),
         pose([0,-7.9,4.0],[.55,1.55,.65],[16,0,0]),
         pose([0,-7.55,.85],[3.25,1.5,3.0],[5,0,0]),
         pose([0,-7.5,2.7],[.9,1.0,2.2],[-35,0,0]),
         pose([0,-7.5,1.1],[.85,.85,1.15],[-18,0,0]),
         pose([0,-7.85,-.2],[1.75,1.7,.9],[-32,0,0]),
         pose([0,-6.6,2.0],[1.15,.65,1.75],[-8,0,0]),
         pose([0,-7.0,-1.1],[.95,.8,2.3],[22,0,0]),
         pose([0,-6.7,-.15],[1.0,.6,1.15],[25,0,0])]
    # Local -Z is the face and +Z the rear. Follow the user's subsequent red
    # direction/length edit: Swim grows beyond the rear roof edge, then its
    # short upper piece turns upright/slightly forward. Power's lower piece
    # rises forward, then its short upper piece curls back, an upright hook.
    # Power+Swim covers most of the unchanged skull roof, then narrows twice
    # into a stepped cone. Reuse the third piece rather than adding topology.
    # Its inactive targets stay fully buried centrally; the former front
    # dormant position would detach while rising into the Power+Swim apex.
    hook=[pose([0,-4.5,.6],[.12,.12,.12]) for _ in TARGETS]
    hook[0]=pose([0,-5.65,.6],[.12,.12,.12])
    hook[3]=pose([0,-8.65,1.15],[1.0,1.0,1.0],[10,0,0])
    hook[8]=pose([0,-6.35,-2.3],[.65,.65,1.1],[25,0,0])
    return [crown(template,'neutral_crown_root',root),crown(template,'neutral_crown_tip',tip,True),
            crown(template,'neutral_fly_hook_tip',hook,True)]

def limbs(data):
    # Viewer Normal-first-type limb variations are small. Keep the same palm
    # and ankle joints and floor-contact edge; never elongate an arm to fake
    # specialization. A complete corner lattice permits paired tuning.
    selected={'body__cube_0','arm_left__cube_1','arm_right__cube_1',
              'foot_left__cube_0','foot_right__cube_0'}
    for bone in data['bones']:
        name=bone['name']
        if name not in selected:continue
        for mesh in bone.get('meshes',[]):
            base=original_box(mesh);poses=[copy.deepcopy(base)]
            for y in (-1,0,1):
                for x in (-1,0,1):
                    run=max(0,-x);power=max(0,x);swim=max(0,-y);fly=max(0,y)
                    p=copy.deepcopy(base)
                    if name.startswith('body'):
                        scale=[1.025+.035*power,1.025+.02*fly,1.015+.025*swim]
                    elif name.startswith('arm'):
                        scale=[1.025+.075*power+.025*swim,1.025+.075*power,1.025+.045*power]
                    else:
                        scale=[1.025+.055*swim+.02*power,1.0+.025*power,1.025+.09*run+.035*swim]
                    p['size']=[a*b for a,b in zip(base['size'],scale)]
                    if name.startswith('foot'):
                        p['center'][1]=max(v[1] for v in mesh['vertices'])-p['size'][1]/2
                        p['center'][2]=max(v[2] for v in mesh['vertices'])-p['size'][2]/2
                    poses.append(p)
            targets(mesh,poses)

def author():
    source=MODELS/'chao_baby.json';data=json.loads(source.read_text());original=copy.deepcopy(data)
    bones={b['name']:b for b in data['bones']}
    bones['gradual_head_021']['meshes']=crests(bones['skull__cube_0']['meshes'][0])
    limbs(data)
    data['authored_gameplay_form']='neutral_proto056'
    data['neutral_path_056']={
        'targets':TARGETS,'canonical_uv_layout':'baby','original_head_box':[7,6,6],
        'weights':'fresh=1-maturity; mature index1+y*3+x = maturity*triangle(runPower,x-1)*triangle(swimFly,y-1)',
        'feature_parameter_order':'center_xyz,size_xyz_positive,rotation_xyz_degrees; compose Rz*Ry*Rx',
        'feature_body_uv':BODY_UV,'feature_tip_uv':TIP_UV,
        'reserved_pixels':{'feature_body':[127,63],'feature_tip':[126,63]},
        'scope':'Ordinary Neutral-aligned adult with locked Normal first evolution only; existing identities and other paths unchanged.',
        'reference':'Independently operated Chao Island viewer, refreshed in0.25.58 for Swim, Power+Swim and Power at young/half/mature stages and rotated views; subsequent user red-line direction/length edit takes priority. Original addon cuboids supply block construction only.',
        'source_geometry_copied':False,
        'approximation':'Fixed original head plus three reused crown cuboids: rear Swim stalk with short upright/slightly-forward tip, forward-then-back upright Power hook, and wide-to-narrow three-step Power+Swim cone; other visible mature endpoints and limbs retained. Dormant third piece moved inside the skull for connected growth. Original planar pink wings retained.',
        'provisional_rates':'960 net current level-bar progress per signed axis; age independent; no original-game timing claim.'}
    outbones={b['name']:b for b in data['bones']}
    for b in original['bones']:
        if b['name'].startswith(('skull','expression_','emotion_')):assert outbones[b['name']]==b,b['name']
    OUT.write_text(json.dumps(data,separators=(',',':'))+'\n')
    refs=['chao_run.geo.json','chao_power.geo.json','chao_fly.geo.json','chao_swim.geo.json']
    manifest={'version':'0.25.58','model':str(OUT.relative_to(ASSETS)),'sha256':sha(OUT),
        'source_model_sha256':sha(source),'target_names':TARGETS,'canonical_head_and_face_unchanged':True,
        'shared_animation_attachment_rig':True,'source_geometry_copied':False,
        'construction_reference_sha256':{n:sha(ROOT/'art/imported_chao/models'/n) for n in refs}}
    (ASSETS/'neutral056.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('Neutral056 generated: fixed original head, one3-box crown, 10 targets, compact shared rig.')

if __name__=='__main__':author()
