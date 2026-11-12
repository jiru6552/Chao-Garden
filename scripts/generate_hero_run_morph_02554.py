#!/usr/bin/env python3
"""Unified, deliberately authored Hero Run path on the existing animation rig.

Targets share semantic rings, vertices and faces; no child/adult donor indices
are copied. Source ratios guide contours, while the facial inset stays fitted
to this mod's expressions. Old gameplay/source assets are never regenerated.
"""
from pathlib import Path
import copy
import hashlib
import json
import math

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT/'src/main/resources/assets/chao_garden'
MODELS = ASSETS/'models/entity/gameplay'
OUT = MODELS/'chao_hero_run_proto054.json'
NAMES = ['neutral_baby', 'hero_baby', 'neutral_run_baby', 'hero_run_baby',
         'hero_run_young', 'hero_run_mature', 'hero_run_run_mature']
REGIONS = {'belly':[80,32,96,48], 'hand':[96,32,112,48],
           'foot':[64,48,80,64], 'crown':[80,48,96,64], 'sole':[96,48,112,64],
           'body':[112,63], 'limb':[113,63], 'rear_head':[114,63],
           'crown_tip':[115,63], 'marker':[116,63], 'tail_tip':[117,63]}


def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def rounded(v): return [[round(float(x),6) for x in p] for p in v]
def swatch(key): return [(REGIONS[key][0]+.5)/2,(REGIONS[key][1]+.5)/2]
def atlas(key,u,v):
    x,y,x1,y1=REGIONS[key]
    return [(x+.5+max(0,min(1,u))*(x1-x-1))/2,
            (y+.5+max(0,min(1,v))*(y1-y-1))/2]


def surface(rings, region='body', uv=None, mesh_name='path_surface_054'):
    """Closed oriented solid, same ring/face graph for all seven targets."""
    count=len(rings[0][0]); ring_count=len(rings[0]); faces=[]
    faces.append(list(reversed(range(count))))
    for row in range(ring_count-1):
        for lane in range(count):
            nxt=(lane+1)%count
            faces.append([row*count+lane,row*count+nxt,(row+1)*count+nxt,(row+1)*count+lane])
    faces.append(list(range((ring_count-1)*count,ring_count*count)))
    targets=[rounded([p for ring in target for p in ring]) for target in rings]
    # Use signed volume once to orient the entire common surface outward.
    vol=0
    pts=targets[4]
    for candidate in [targets[4]]+targets:
        vol=0
        for f in faces:
            a=candidate[f[0]]
            for i in range(1,len(f)-1):
                b,c=candidate[f[i]],candidate[f[i+1]]
                vol+=a[0]*(b[1]*c[2]-b[2]*c[1])+a[1]*(b[2]*c[0]-b[0]*c[2])+a[2]*(b[0]*c[1]-b[1]*c[0])
        if abs(vol)>1e-8:break
    if vol<0: faces=[list(reversed(f)) for f in faces]
    encoded=[]
    for f in faces:
        tex=[uv(pts[at],at,f) if uv else swatch(region) for at in f]
        encoded.append({'v':f,'uv':tex})
    return {'name':mesh_name,'material':'official','shading':'flat',
            'vertices':targets[0], 'morphology_vertices_054':targets,'faces':encoded}


def head(target):
    # A flattened central facial inset retains all existing eyes/expressions.
    # Rear contours are vertices of this SAME shell, not an overlapping wedge.
    base=[(0,2.65,2.45,-2.20,2.15),(-.7,3.35,3.18,-3,2.7),
          (-3.4,3.5,3.18,-3,3),(-4.9,3.12,2.5,-2.35,2.6),
          (-5.9,1.8,1.2,-1.15,1.5),(-6.25,.3,.18,.15,.5)]
    if target in (0,2):
        # Preserve the normal baby's familiar broad face; modest bevels also
        # make zero/intermediate Run stages part of the same coherent surface.
        base=[(y, w, fw, fz, bz) for y,w,fw,fz,bz in base]
    rows=[]
    for row,(y,w,fw,fz,bz) in enumerate(base):
        if target==2 and row in (2,3): bz+=1.0
        if target==3 and row==4: y-=.15
        if target>=4 and row==5: y-=.05
        # Original Hero Run young->RunRun rear depth ratio 1.302047835:
        # fixed facial plane -3, rear point grows from +3 to +4.812287.
        rear=bz
        if target==6: rear+=1.812287*(1 if row==2 else .35 if row in (1,3) else 0)
        elif target==5: rear+=.20 if row==2 else 0
        corner=fz+(bz-fz)*.20;side=fz+(bz-fz)*.60
        rows.append([[fw,y,fz],[w,y,corner],[w,y,side],[w*.72,y,bz],
                     [0,y,rear],[-w*.72,y,bz],[-w,y,side],[-w,y,corner],[-fw,y,fz]])
    return rows


def body(target):
    normal=(target==5); run=(target==6)
    sx=1.065996 if normal else 1
    sy=1.046015 if normal else .9444905 if run else 1
    sz=.9805036 if normal else .9473006 if run else 1
    rows=[]
    for y,w,dep in [(0.5,1.0,1.15),(-.25,1.85,1.8),(-1.8,2.0,2.0),(-3.1,1.6,1.5),(-3.5,.75,.75)]:
        yy=.5+(y-.5)*sy
        rows.append([[x*sx,yy,z*sz] for x,z in [(w*.72,-dep),(w,-dep*.65),(w,dep*.65),(w*.72,dep),(-w*.72,dep),(-w,dep*.65),(-w,-dep*.65),(-w*.72,-dep)]])
    return rows


def hand(target,sign):
    size=1.1329 if target==6 else 1
    rows=[]
    for x,r in [(1.75,.55),(2.08,1),(3.17,1),(3.60,.58)]:
        rows.append([[sign*(2.65+(x-2.65)*size),.95+math.cos(i*math.pi/4)*1.05*size,
                      -.45+math.sin(i*math.pi/4)*1.05*size] for i in range(8)])
        # Rounded hand ends, preserving one compact closed solid.
        for p in rows[-1]:
            p[1]=.95+(p[1]-.95)*r;p[2]=-.45+(p[2]+.45)*r
    return rows


def foot(target,sign):
    w=2.3*(1.397 if target==6 else 1); depth=4*(1.178 if target==6 else 1)
    front=.2-depth;back=.2;cx=sign*.95;rows=[]
    outline=[(.24,0),(.5,.16),(.5,.86),(.3,1),(-.3,1),(-.5,.86),(-.5,.16),(-.24,0)]
    for y,scale,shorten in [(2,.93,0),(1.5,1,0),(.45,.9,.13),(0,.62,.27)]:
        rows.append([[cx+sign*x*w*scale,y,front+(back-front)*(z*(1-shorten)+shorten)] for x,z in outline])
    return rows


def tube(points,radii):
    rings=[]
    for i,p in enumerate(points):
        a=points[max(0,i-1)];b=points[min(len(points)-1,i+1)]
        tangent=[b[k]-a[k] for k in range(3)];length=math.sqrt(sum(x*x for x in tangent)) or 1
        tangent=[x/length for x in tangent]
        # Stable transported frame: these paths never point along pure X.
        right=[0,-tangent[2],tangent[1]];ln=math.sqrt(sum(x*x for x in right)) or 1
        right=[x/ln for x in right]
        up=[tangent[1]*right[2]-tangent[2]*right[1],tangent[2]*right[0]-tangent[0]*right[2],tangent[0]*right[1]-tangent[1]*right[0]]
        rings.append([[p[k]+radii[i]*(math.cos(j*math.pi/4)*right[k]+math.sin(j*math.pi/4)*up[k]) for k in range(3)] for j in range(8)])
    return rings


def crown(target,branch):
    sign=1 if branch==1 else -1
    if branch==0:
        if target in (0,2): return tube([(0,-5.7,.5),(0,-6.0,.7),(0,-6.4,.9),(0,-6.8,1.1)],[.45,.38,.22,.025])
        if target==1:return tube([(0,-5.8,.5)]*4,[0,0,0,0])
        if target in (1,3,4):
            top=-7.85
            return tube([(0,-5.7,.5),(0,-6.5,.7),(0,top+.6,.8),(0,top,.9)],[.47,.38,.25,.025])
        if target==5: return tube([(0,-5.7,.5),(0,-7.3,.8),(0,-8.5,2.2),(0,-8.2,3.8)],[.38,.5,.55,.025])
        return tube([(0,-5.7,.5),(0,-7.6,1.1),(0,-9.0,2.5),(0,-8.45,4.55)],[.38,.68,.75,.025])
    if target in (0,2): return tube([(0,-6,.6)]*4,[0,0,0,0])
    if target==1:
        return tube([(sign*.30,-5.95,.5),(sign*.65,-6.35,.6),(sign*1.05,-6.7,.65),(sign*1.28,-6.85,.7)],[.30,.43,.40,.16])
    if target in (1,3,4,5):
        extent=.26 if target in (1,4) else .45 if target==3 else .55
        return tube([(sign*.12,-7,.8),(sign*extent,-7.1,.9),(sign*extent,-7.25,1.0),(sign*extent,-7.3,1.1)],[.12,.14,.1,.015])
    return tube([(sign*.12,-7.5,1.2),(sign*1.3,-8.65,2.0),(sign*2.8,-8.5,3.3),(sign*3.5,-7.15,4.6)],[.35,.68,.72,.025])


def marker_mesh(halo):
    targets=[];n=12;m=6
    for target in range(7):
        pts=[]
        for row in range(m):
            for lane in range(n):
                phi=lane*math.tau/n
                if halo:
                    theta=row*math.tau/m;r=1.65+.26*math.cos(theta)
                    p=[r*math.cos(phi),.26*math.sin(theta),r*math.sin(phi)] if target>=4 else [0,0,0]
                else:
                    theta=row*math.pi/(m-1);r=math.sin(theta)
                    p=[r*math.cos(phi),-math.cos(theta),r*math.sin(phi)] if target<4 else [0,0,0]
                pts.append(p)
        targets.append(rounded(pts))
    faces=[]
    for row in range(m if halo else m-1):
        for lane in range(n):
            a=row*n+lane;b=row*n+(lane+1)%n;c=((row+1)%m)*n+(lane+1)%n;d=((row+1)%m)*n+lane
            faces.append({'v':[a,b,c,d],'uv':[swatch('marker')]*4})
    return {'name':'halo_054' if halo else 'orb_054','material':'halo' if halo else 'orb','shading':'flat','vertices':targets[0],'morphology_vertices_054':targets,'faces':faces}


def author():
    protected={str(p.relative_to(ASSETS)):sha(p) for p in [MODELS/'chao_baby.json',MODELS/'chao_baby_run_proto053.json',MODELS/'chao_hero_run_proto052.json',ASSETS/'baby_run_morphology_053.json',ASSETS/'hero_run_morphology_052.json']}
    data=json.loads((MODELS/'chao_baby.json').read_text())
    for bone in data['bones']:
        name=bone['name'];bone['morphology_pivots_054']=[copy.deepcopy(bone['pivot']) for _ in NAMES]
        bone['morphology_rotations_054']=[copy.deepcopy(bone.get('rotation',[0,0,0])) for _ in NAMES]
        for mesh in bone.get('meshes',[]):
            mesh.pop('morph_vertices',None)
            mesh['morphology_vertices_054']=[copy.deepcopy(mesh['vertices']) for _ in NAMES]
        if name=='skull__cube_0': bone['meshes']=[surface([head(i) for i in range(7)])]
        elif name=='body__cube_0':
            def body_uv(p,at,f):
                if all(v%8 in (0,7) for v in f): return atlas('belly',(p[0]+2.14)/4.28,(p[1]+3.7)/4.2)
                return swatch('body')
            bone['meshes']=[surface([body(i) for i in range(7)],uv=body_uv)]
        elif name in ('arm_left__cube_1','arm_right__cube_1'):
            sign=1 if 'left' in name else -1
            bone['meshes']=[surface([hand(i,sign) for i in range(7)],'limb',lambda p,at,f:atlas('hand',(p[2]+1.65)/2.4,(p[0]*sign-1.75)/1.85))]
        elif name in ('foot_left__cube_0','foot_right__cube_0'):
            sign=1 if 'left' in name else -1
            bone['meshes']=[surface([foot(i,sign) for i in range(7)],'limb',lambda p,at,f:atlas('sole' if all(v<8 for v in f) else 'foot',(p[0]*sign+.8)/3.5,(p[2]+4.6)/5.2))]
        elif name=='gradual_head_021':
            bone['meshes']=[surface([crown(i,j) for i in range(7)],'crown_tip',lambda p,at,f:atlas('crown',(at%8)/7,(at//8)/3),f'crown_branch_054_{j}') for j in range(3)]
        elif name=='emotion_dot__cube_0': bone['meshes']=[marker_mesh(False),marker_mesh(True)]
        elif name=='skull__cube_1': bone['meshes']=[]
        elif name=='tail__cube_0':
            for mesh in bone.get('meshes',[]):
                for face in mesh['faces']:face['uv']=[swatch('tail_tip') for _ in face['v']]
        elif name in ('arm_left__cube_0','arm_right__cube_0'):
            for mesh in bone.get('meshes',[]):
                for face in mesh['faces']:face['uv']=[swatch('limb') for _ in face['v']]
        if name=='skull':
            for t in (5,6):bone['morphology_pivots_054'][t][1]-=.20 if t==5 else .28764
        if name=='emotion_dot':
            # Fit the calm marker above the actual changing crown. The sibling
            # heart/question/dizzy/emphasis icons retain their old transforms.
            for t in range(7):bone['morphology_pivots_054'][t][1]=.2 if t<4 else -.4
        if name in ('arm_left','arm_right'):
            for t in range(1,7):bone['morphology_pivots_054'][t][0]*=.96
        if name in ('foot_left','foot_right'):
            for t in range(4,7):bone['morphology_pivots_054'][t][2]=1.1
    # Neutral Run's swept anatomy is a separate alignment corner, not imposed
    # on Hero children. Closed branches collapse at their real cranial roots.
    quills=[]
    specs=[((0,-5,1.5),(0,-6.0,4.2),(0,-6.8,6.5),.8)]
    for sign in (-1,1):specs.extend([((sign*2.1,-3.8,2.1),(sign*3.5,-3.8,4.6),(sign*4.8,-3.8,7.0),.85),((sign*2.2,-1.4,2),(sign*3.4,-.8,4.4),(sign*4.4,.3,6.3),.8)])
    for j,(a,b,c,r) in enumerate(specs):
        target=tube([a,[(a[k]+b[k])/2 for k in range(3)],b,c],[r,r*.9,r*.65,.02]);collapsed=[[list(a)]*8 for _ in range(4)]
        shapes=[collapsed,collapsed,target,collapsed,collapsed,collapsed,collapsed]
        name=f'neutral_run_quill_054_{j}';quills.append(name)
        data['bones'].append({'name':name,'parent':'skull','pivot':[0,0,0],'rotation':[0,0,0],
          'morphology_pivots_054':[[0,0,0] for _ in NAMES],'morphology_rotations_054':[[0,0,0] for _ in NAMES],
          'meshes':[surface(shapes,'rear_head',mesh_name='closed_neutral_quill_054')]})
    data['head_decorations']=['gradual_head_021','monster_crown']+quills
    data['attachment_extra_bones']={'skull':quills}
    data['hero_run_prototype_054']={'endpoints':NAMES,'semantic_regions_pixels':REGIONS,
      'canonical_uv_layout':'baby','topology':'authored common semantic rings, no donor vertex-index copy',
      'reference':'Chao Island actual 3D viewer + supplied SA2B ChaoMain.prs Hero Run point/part landmarks',
      'face':'existing eye/expression panels and fixed central facial inset preserved',
      'scope':'ordinary babies and locked Hero Run adults; unchanged policy, rates and saves'}
    data['authored_gameplay_form']='hero_run_proto054'
    data.pop('proportion_adjustment_022',None)
    OUT.write_text(json.dumps(data,separators=(',',':'))+'\n')
    for name,digest in protected.items():assert sha(ASSETS/name)==digest,name
    manifest={'version':'0.25.54','model':str(OUT.relative_to(ASSETS)),'sha256':sha(OUT),
      'endpoint_order':NAMES,'semantic_regions_pixels':REGIONS,'protected_source_sha256':protected,
      'source_mesh_imported':False,'biological_age_separate_from_training':True,
      'first_evolution':'shared topology; adult identity transition remains concealed by existing cocoon'}
    (ASSETS/'hero_run_morphology_054.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('Generated unified Hero Run 054 canonical path; old assets protected.')


if __name__=='__main__':author()
