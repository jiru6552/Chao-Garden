#!/usr/bin/env python3
"""Original block-built Chaos anatomy, inherited native-UV coats and icy terminal surfaces.

Run after build_forms_019.py. The supplied raw comparison geometry, textures and
clips are read only. A surface union removes internal caps at material boundaries.
"""
from pathlib import Path
from collections import Counter
import copy, hashlib, json, math
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/chao_garden'
MODEL=ASSET/'models/entity'
TEX=ASSET/'textures/entity/chao'
SIDES=[[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[0,4,7,3],[1,2,6,5]]
MAIN=[36,26]; CHEST=[46,24]; TIP=[50,24]; WING=[54,24]; ORB=[58,24]
EYE=[2,2]
FAMILIES={'light':{'body':(232,246,242),'tip':(69,182,224),'chest':(164,220,232),'wing':(218,241,241),'orb':(62,244,229)},
 'angel':{'body':(249,247,237),'tip':(239,135,191),'chest':(226,58,133),'wing':(255,214,232),'orb':(255,247,171)},
 'devil':{'body':(29,37,43),'tip':(244,100,45),'chest':(255,215,49),'wing':(229,218,230),'orb':(190,90,255)}}

def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,separators=(',',':'))+'\n')
def points(lo,hi):
 x,y,z=lo;X,Y,Z=hi
 return [[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]]
def mesh(name,vertices,faces,material='official'):
 return {'name':name,'material':material,'shading':'flat','vertices':vertices,'faces':faces}
def box(name,lo,hi,uv=MAIN,material='official'):
 return mesh(name,points(lo,hi),[{'v':v,'uv':[list(uv) for _ in v]} for v in SIDES],material)
def bone(data,name,parent,meshes=(),pivot=(0,0,0),decor=False):
 value={'name':name,'parent':parent,'pivot':list(pivot),'rotation':[0,0,0],'meshes':list(meshes)}
 data['bones'].append(value)
 if decor:data['head_decorations'].append(name)
 return value

def textured_box(name,lo,hi):
 m=box(name,lo,hi)
 for f in m['faces']:f['uv']=[[32.25,22.25],[39.75,22.25],[39.75,29.75],[32.25,29.75]]
 return m

def surface_union(boxes,name,uv=MAIN,glass_axis=None,cut=0,direction=1,head=False):
 """Only exterior grid faces. Opaque/glass segments share edges, never internal caps."""
 axes=[sorted({p[i] for b in boxes for p in b}) for i in range(3)]
 if glass_axis is not None and axes[glass_axis][0]<cut<axes[glass_axis][-1]:
  axes[glass_axis]=sorted(set(axes[glass_axis]+[cut]))
 filled=set()
 for x in range(len(axes[0])-1):
  for y in range(len(axes[1])-1):
   for z in range(len(axes[2])-1):
    center=[(axes[i][a]+axes[i][a+1])/2 for i,a in enumerate((x,y,z))]
    if any(all(lo[i]<center[i]<hi[i] for i in range(3)) for lo,hi in boxes):filled.add((x,y,z))
 opaque_v=[];opaque_f=[];glass_v=[];glass_f=[]
 maximum=max(direction*v for v in axes[glass_axis]) if glass_axis is not None else 0
 extent=max(.01,maximum-direction*cut)
 face_neighbors=[(0,0,-1),(0,0,1),(0,-1,0),(0,1,0),(-1,0,0),(1,0,0)]
 # Greedy coplanar face merging avoids the fragment count of a voxelized union.
 panels={}
 for cell in sorted(filled):
  center=(axes[glass_axis][cell[glass_axis]]+axes[glass_axis][cell[glass_axis]+1])/2 if glass_axis is not None else 0
  translucent=glass_axis is not None and direction*(center-cut)>=-1e-7
  for face_id,delta in enumerate(face_neighbors):
   adjacent=tuple(cell[i]+delta[i] for i in range(3))
   if adjacent in filled:continue
   normal_axis=next(i for i,v in enumerate(delta) if v)
   plane=cell[normal_axis]+(1 if delta[normal_axis]>0 else 0)
   other=[i for i in range(3) if i!=normal_axis]
   panels.setdefault((face_id,plane,translucent),set()).add((cell[other[0]],cell[other[1]]))
 for (face_id,plane,translucent),cells in sorted(panels.items()):
  delta=face_neighbors[face_id];normal_axis=next(i for i,v in enumerate(delta) if v)
  other=[i for i in range(3) if i!=normal_axis]
  while cells:
   x,y=min(cells);X=x+1
   while (X,y) in cells:X+=1
   Y=y+1
   while all((i,Y) in cells for i in range(x,X)):Y+=1
   cells.difference_update((i,j) for i in range(x,X) for j in range(y,Y))
   lo=[0,0,0];hi=[0,0,0]
   lo[normal_axis]=hi[normal_axis]=axes[normal_axis][plane]
   lo[other[0]]=axes[other[0]][x];hi[other[0]]=axes[other[0]][X]
   lo[other[1]]=axes[other[1]][y];hi[other[1]]=axes[other[1]][Y]
   vs=points(lo,hi);indices=SIDES[face_id]
   vertices,faces=(glass_v,glass_f) if translucent else (opaque_v,opaque_f)
   start=len(vertices);vertices.extend(vs[j] for j in indices)
   if translucent:
    uvs=[]
    others=[i for i in range(3) if i!=glass_axis]
    span=next((i for i in others if hi[i]>lo[i]),others[0])
    for j in indices:
     t=max(0,min(1,(direction*vs[j][glass_axis]-direction*cut)/extent))
     across=(vs[j][span]-axes[span][0])/max(.01,axes[span][-1]-axes[span][0])
     uvs.append([.5+63*t, (16 if head else 0)+.5+15*across])
   else:
    uvs=[[32.25,22.25],[39.75,22.25],[39.75,29.75],[32.25,29.75]] if uv==MAIN else [list(uv)]*4
   faces.append({'v':[start+i for i in range(4)],'uv':uvs})
 return ([mesh(name,opaque_v,opaque_f)] if opaque_v else [],[mesh(name+'_ice',glass_v,glass_f)] if glass_v else [])


def feature(data,glass,name,parent,boxes,axis,cut,direction=1,head=False,uv=MAIN,decor=False):
 opaque,ice=surface_union(boxes,name,uv,axis,cut,direction,head)
 bone(data,name,parent,opaque,decor=decor);bone(glass,name,parent,ice,decor=decor)

def mirrored(boxes,side):
 return [([min(side*lo[0],side*hi[0]),lo[1],lo[2]],[max(side*lo[0],side*hi[0]),hi[1],hi[2]]) for lo,hi in boxes]

def build(form):
 source=json.loads((MODEL/'official/chao_baby.json').read_text())
 data=copy.deepcopy(source)
 # Keep exact animation joint names and pivots; author all visible anatomy.
 for b in data['bones']:
  if not b['name'].startswith('emotion_'):b['meshes']=[]
 for b in data['bones']:
  if b['name'].startswith(('emotion_question__','emotion_dizzy__')):
   for m in b['meshes']:
    for f in m['faces']:f['uv']=[[60.5,1.5] for _ in f['v']]
 data['head_decorations']=[]
 data['texture_width']=64;data['texture_height']=32
 data['attachment_landmarks']={'eyes':[0,-2.25,-3.045,1.75]}
 data['authored_metadata']={'version':'0.20.0','form':'chaos_'+form,'eye_centers':[[-1.75,-2.25,-3.045],[1.75,-2.25,-3.045]],'head_core_bounds':[[-3.5,-5.6,-3],[3.5,0,2.65]],'source_alias':'baby','glass_boundaries':'surface-union open joins; gradient-alpha terminal geometry'}
 glass=copy.deepcopy(data);glass['texture_width']=64;glass['texture_height']=32
 for b in glass['bones']:b['meshes']=[]
 byname={b['name']:b for b in data['bones']}
 # The broad core retains the addon's cuboid appearance. Narrow tiers add block slopes.
 core=[([-3.5,-5.6,-3],[3.5,-.55,2.65]),([-3,-.55,-2.65],[3,0,2.25]),([-2.8,-6.2,-2.45],[2.8,-5.6,2.3])]
 opaque,_=surface_union(core,'chaos_head_core')
 byname['skull__cube_0']['meshes']=opaque
 byname['body__cube_0']['meshes']=[textured_box('chaos_body',[-2,-3.5,-2],[2,.5,2])]
 # A single pair of colored, fully-open eyes; no donor panels, pupils or mouth.
 for side in (-1,1):
  cx=side*1.75
  eye_boxes=[([cx-.61,-3.06,-3.06],[cx+.61,-1.44,-3.025]),([cx-.81,-2.78,-3.06],[cx+.81,-1.72,-3.025])]
  eye,_=surface_union(eye_boxes,'chaos_eye',EYE)
  bone(data,'chaos_eye_'+str(side),'skull',eye)
 # Keep upper arms on supplied animation frames. Hands replace terminal volume.
 for side,arm in ((1,'arm_left'),(-1,'arm_right')):
  arm_boxes=mirrored([([0,0,-.5],[2,1,.5])],side)
  byname[arm+'__cube_0']['meshes']=surface_union(arm_boxes,'upper_arm')[0]
  hand_boxes=mirrored([([1.8,-.1,-1.25],[3.5,1.9,.65]),([3.5,.15,-.95],[4,.95,.35])],side)
  feature(data,glass,'chaos_hand_'+arm,arm,hand_boxes,0,side*1.8,side,uv=TIP)
  # Two small arm spikes define Devil's silhouette without floating fragments.
  if form=='devil':
   spike=mirrored([([2.15,-.5,.05],[2.85,.25,.8]),([2.65,-.9,.2],[3.05,-.25,.65])],side)
   bone(data,'chaos_arm_spikes_'+arm,arm,surface_union(spike,'devil_arm_spikes',TIP)[0])
 for side,foot in ((1,'foot_left'),(-1,'foot_right')):
  feet=mirrored([([0,0,-3.3],[2,2,0]),([.25,.25,-3.85],[1.75,1.75,-3.3])],side)
  feature(data,glass,'chaos_foot_'+foot,foot,feet,2,-1.2,-1,uv=TIP)
 # Family-specific abdomen markings are attached to body, not a face overlay.
 if form=='angel':
  heart=[([-.95,-2.6,-2.025],[.95,-1,-2.005]),([-1.2,-2.3,-2.025],[1.2,-1.35,-2.005]),([-.55,-1,-2.025],[.55,-.65,-2.005])]
  bone(data,'chaos_chest','body',surface_union(heart,'angel_rose_chest',CHEST)[0])
 elif form=='devil':
  crescent=[([-1.45,-2.35,-2.035],[-.8,-1.5,-2.005]),([.8,-2.35,-2.035],[1.45,-1.5,-2.005]),([-1,-1.65,-2.035],[1,-1.15,-2.005])]
  bone(data,'chaos_chest','body',surface_union(crescent,'devil_crescent',CHEST)[0])
 # Connected head ridges shape the skull itself, with distinct family outlines.
 if form=='light':
  crest=[([-1.9,-7,-1.9],[1.9,-5.7,2]),([-1.3,-7.9,-1],[1.3,-6.7,2.35]),([-.7,-8.7,.1],[.7,-7.5,2.5]),([-.35,-9.1,1.15],[.35,-8.3,2.7])]
  bone(data,'chaos_crown','skull',surface_union(crest,'light_single_teardrop')[0],decor=True)
  for side in (-1,1):
   lobe=mirrored([([2.7,-3.9,.7],[3.7,-.55,3.15]),([3.4,-3.1,1.3],[4.1,-.85,3.6]),([3.75,-2.4,2.3],[4.35,-1.2,3.9])],side)
   feature(data,glass,'chaos_crown_lobe_'+str(side),'skull',lobe,0,side*3.15,side,head=True,decor=True)
  rear=[([-1.8,-5.1,2.4],[1.8,-2,3.5]),([-1.2,-3.9,3.3],[1.2,-1.15,4.4]),([-.6,-2.6,4.1],[.6,-.6,4.9])]
  feature(data,glass,'chaos_crown_rear','skull',rear,2,3.3,head=True,decor=True)
 elif form=='angel':
  for side in (-1,1):
   horn=mirrored([([1.3,-6.35,-.5],[2.6,-4.8,1.8]),([1.8,-7.35,0],[2.9,-5.8,2]),([2.6,-7.75,.45],[3.5,-6.1,2.2]),([3.2,-6.8,.8],[4,-4.8,2.5]),([3.65,-5.55,1.1],[4.5,-3.5,2.9]),([4.1,-4.4,1.6],[4.8,-3,3.1])],side)
   feature(data,glass,'chaos_crown_horn_'+str(side),'skull',horn,0,side*3.2,side,head=True,decor=True)
  rear=[([-1.45,-5.1,2.4],[1.45,-2.5,3.6]),([-.9,-3.5,3.3],[.9,-1.2,4.5]),([-.45,-2,4.25],[.45,-.15,5.05])]
  feature(data,glass,'chaos_crown_rear','skull',rear,2,3.5,head=True,decor=True)
 else:
  for side in (-1,1):
   upper=mirrored([([2,-6.1,-.7],[3.3,-4.8,1.6]),([2.8,-6.8,-.35],[3.8,-5.6,1.7]),([3.4,-7.65,.05],[4,-6.35,1.75]),([3.65,-8.2,.5],[4.1,-7.2,1.8])],side)
   feature(data,glass,'chaos_crown_upper_'+str(side),'skull',upper,1,-6.5,-1,head=True,decor=True)
   lower=mirrored([([2.5,-1.9,.7],[3.6,-.3,2.8]),([3.2,-.9,1.5],[4.15,.3,3.3]),([3.8,-.15,2.3],[4.6,.9,3.7])],side)
   feature(data,glass,'chaos_crown_lower_'+str(side),'skull',lower,0,side*3.6,side,head=True,decor=True)
  tail=[([-.55,-.4,1.9],[.55,.5,4.5]),([-.42,-1.2,4],[.42,.15,6.2]),([-1.25,-2.1,5.4],[-.25,-.9,6.5]),([.25,-2.1,5.4],[1.25,-.9,6.5])]
  bone(data,'chaos_forked_tail','body',surface_union(tail,'devil_long_fork_tail',TIP)[0])
 # Chaos wings are almost hidden tiny light-colored triangles in the games.
 # Two little block tiers keep the supplied addon's silhouette without large wings.
 for side in (-1,1):
  wing=mirrored([([.55,-3.05,2.05],[1.5,-2.3,2.4]),([1.3,-2.85,2.1],[1.85,-2.4,2.45])],side)
  bone(data,'chaos_back_wing_'+str(side),'body',surface_union(wing,'chaos_tiny_back_wing',WING)[0])
 # Own luminous marker: the source emotion ball remains available to reaction code.
 if form=='angel':
  halo=[([-1.65,-10.3,-.7],[1.65,-9.95,-.35]),([-1.65,-10.3,.7],[1.65,-9.95,1.05]),([-1.65,-10.3,-.35],[-1.3,-9.95,.7]),([1.3,-10.3,-.35],[1.65,-9.95,.7])]
  marker=surface_union(halo,'angel_block_halo',ORB)[0]
  for m in marker:m['material']='halo'
 elif form=='devil':
  flame=[([-.8,-10.1,-.65],[.8,-8.8,.65]),([-.5,-11,-.4],[.45,-9.8,.4]),([.1,-11.55,-.25],[.55,-10.5,.25])]
  marker=surface_union(flame,'devil_marker_flame',ORB)[0]
  for m in marker:m['material']='flame'
 else:marker=[box('light_bright_orb',[-.8,-11,-.6],[.8,-9.4,1],ORB,'orb')]
 bone(data,'chaos_marker','skull',marker)
 # Rebuild glass hierarchy with identical anchors and empty opaque-only bones.
 lookup={b['name']:b for b in glass['bones']}
 for b in data['bones']:
  if b['name'] not in lookup:
   empty=copy.deepcopy(b);empty['meshes']=[];glass['bones'].append(empty)
 glass['head_decorations']=list(data['head_decorations'])
 data['authored_metadata']['glass_vertex_count']=sum(len(m['vertices']) for b in glass['bones'] for m in b['meshes'])
 write(MODEL/f'gameplay/chao_chaos_{form}.json',data)
 write(MODEL/f'gameplay/chao_chaos_{form}_glass.json',glass)
 return data,glass

def tint(rgb,target,base):
 shade=sum(rgb)/max(1,sum(base))
 return tuple(max(0,min(255,round(v*shade))) for v in target)
def make_textures():
 output=TEX/'gameplay/chaos';output.mkdir(parents=True,exist_ok=True)
 mask0=Image.open(TEX/'gameplay/masks/baby.png').convert('RGBA')
 masks={name:Image.new('RGBA',mask0.size) for name in ('chaos','chaos_wings','chaos_accent')}
 masks['chaos'].paste(mask0)
 scale=mask0.width/64
 def rect(image,xy,fill):
  ImageDraw.Draw(image).rectangle(tuple(round(v*scale) for v in xy),fill=fill)
 rect(masks['chaos'],(32,22,56-1/scale,30-1/scale),(255,255,255,255))
 rect(masks['chaos_wings'],(52,22,56-1/scale,26-1/scale),(255,255,255,255))
 rect(masks['chaos_accent'],(44,22,52-1/scale,26-1/scale),(255,255,255,255))
 for name,im in masks.items():im.save(TEX/f'gameplay/masks/{name}.png',optimize=True)
 count=0
 for form,palette in FAMILIES.items():
  for path in sorted((TEX/'official').glob(f'chaos_{form}*.png')):
   im=Image.open(path).convert('RGBA');original=im.copy();sx=im.width/64;sy=im.height/32
   canonical=path.stem in (f'chaos_{form}',f'chaos_{form}_normal_two',f'chaos_{form}_normal_two_shiny')
   base=original.getpixel((int(7.5*sx),int(8.5*sy)))[:3]
   if canonical:
    for y in range(im.height):
     for x in range(im.width):
      if mask0.getpixel((x*mask0.width//im.width,y*mask0.height//im.height))[3] and im.getpixel((x,y))[3]:
       rgb=original.getpixel((x,y))[:3];im.putpixel((x,y),(*tint(rgb,palette['body'],base),255))
   # An eight-texel coat patch preserves patterned/jewel inheritance on new cuboids.
   patch=im.crop((round(6*sx),round(6*sy),round(13*sx),round(12*sy))).resize((round(8*sx),round(8*sy)),Image.Resampling.NEAREST)
   im.paste(patch,(round(32*sx),round(22*sy)))
   inherited=im.getpixel((int(7.5*sx),int(8.5*sy)))[:3]
   tip=palette['tip'] if canonical else original.getpixel((int(15*sx),int(16*sy)))[:3]
   # Family anatomy remains recognizable in ordinary two-tone, while special coats inherit.
   colors={'chest':palette['chest'] if canonical else tuple(round(v*.75) for v in inherited),
    'tip':tip,'wing':palette['wing'] if canonical else inherited,'orb':palette['orb']}
   draw=ImageDraw.Draw(im)
   for role,uv in [('chest',CHEST),('tip',TIP),('wing',WING),('orb',ORB)]:
    u,v=uv;draw.rectangle((round((u-2)*sx),round((v-2)*sy),round((u+2)*sx)-1,round((v+2)*sy)-1),fill=(*colors[role],255))
   # Family eye texels are preserved byte-for-byte and never duplicated by donor eyes.
   assert im.getpixel((int(EYE[0]*sx),int(EYE[1]*sy)))==original.getpixel((int(EYE[0]*sx),int(EYE[1]*sy)))
   im.save(output/path.name,optimize=True);count+=1
 return count

def validate(data,glass):
 bones={b['name']:b for b in data['bones']};gb={b['name']:b for b in glass['bones']}
 assert bones.keys()==gb.keys()
 assert len([n for n in bones if n.startswith('chaos_eye_')])==2
 assert all(not b['meshes'] for n,b in bones.items() if n.startswith('skull__cube_') and n!='skull__cube_0')
 assert not any('mouth' in b['name'] for b in data['bones'])
 for name,b in bones.items():
  assert b['parent']==gb[name]['parent'] and b['pivot']==gb[name]['pivot']
  for kind,owner in [('opaque',b),('glass',gb[name])]:
   for m in owner['meshes']:
    assert all(math.isfinite(value) for v in m['vertices'] for value in v)
    for f in m['faces']:
     assert len(f['v'])==len(f['uv'])==4
     assert all(0<=v[0]<=64 and 0<=v[1]<=32 for v in f['uv'])
 # The lower opaque feet stop precisely at z=-1.2: no opaque backing under ice.
 for name in ('chaos_foot_foot_left','chaos_foot_foot_right'):
  assert all(v[2]>=-1.2 for m in bones[name]['meshes'] for v in m['vertices'])
  assert all(v[2]<=-1.2 for m in gb[name]['meshes'] for v in m['vertices'])
 return {'opaque_faces':sum(len(m['faces']) for b in bones.values() for m in b['meshes']),
 'glass_faces':sum(len(m['faces']) for b in gb.values() for m in b['meshes']),
 'head_bounds':data['authored_metadata']['head_core_bounds'],'eye_pairs':1,'mouths':0,
 'matching_semantic_hierarchy':True,'opaque_glass_caps_removed':True,'native_uv_bounds_checked':True}

def main():
 raw=ROOT/'art/imported_chao'
 protected={p.relative_to(raw).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in raw.rglob('*') if p.is_file()}
 report={'version':'0.20.0','families':{}}
 for form in FAMILIES:
  data,glass=build(form);report['families'][form]=validate(data,glass)
 report['inherited_textures']=make_textures()
 assert report['inherited_textures']==351,report['inherited_textures']
 assert protected=={p.relative_to(raw).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in raw.rglob('*') if p.is_file()}
 report['raw_source_files_preserved']=len(protected)
 write(ROOT/'docs/research/CHAOS_020_CHECKS.json',report)
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()
