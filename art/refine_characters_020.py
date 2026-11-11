#!/usr/bin/env python3
"""Refined Amy and an actual mechanical Omochao on the supplied semantic rig.

Only authored gameplay files are changed. Native source geometry/PNGs remain
exact. Omochao's mechanical hardware uses unused UV swatches; its large chassis,
hands and feet retain native UVs so the renderer can copy a builder's real coat.
"""
from pathlib import Path
import copy,hashlib,json,math
from PIL import Image
from build_forms_019 import source,source_geo,source_image,texture_character,box,points,add_bone,lenses,expressions
from import_official_chao import native_skin_masks

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden';M=A/'models/entity/gameplay';T=A/'textures/entity/chao/gameplay'

def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,separators=(',',':'))+'\n')
def uv_set(mesh,uv):
 for f in mesh['faces']:f['uv']=[list(uv) for _ in f['v']]
def replace_box(data,bone,lo,hi):
 b=next(b for b in data['bones'] if b['name']==bone)
 mesh=b['meshes'][0];mesh['vertices']=points(lo,hi)
 return mesh

def prepare(name,color):
 d=copy.deepcopy(source('baby'));uv=texture_character('baby',name,color)
 for b in d['bones']:
  if b['name'] in d['head_decorations']:b['meshes']=[]
 d['head_decorations']=[];d['authored_character']=name;d['source_uv_layout']='baby'
 d['character_revision']='0.20.0'
 return d,uv

def shapes(d,name,parent,items,uv,decor=False,pivot=(0,0,0)):
 add_bone(d,name,parent,[box(label,lo,hi,uv[color],material) for label,lo,hi,color,material in items],pivot=pivot,decor=decor)

def amy():
 d,uv=prepare('amy',(237,145,182))
 replace_box(d,'skull__cube_0',[-3.12,-5.9,-2.95],[3.12,-.18,2.85])
 # A connected, stepped cheek/top envelope gives a Chao head, not a helmet.
 shapes(d,'character_amy_head','skull',[
  ('left_cheek',[-3.57,-5.25,-2.5],[-3.05,-.75,2.4],'pink','character_skin'),
  ('right_cheek',[3.05,-5.25,-2.5],[3.57,-.75,2.4],'pink','character_skin'),
  ('soft_crown',[-2.72,-6.30,-2.38],[2.72,-5.80,2.45],'pink','character_skin'),
  ('chin',[-2.62,-.28,-2.48],[2.62,.18,2.35],'pink','character_skin')],uv)
 hair=[
  ('rear_hair',[-3.2,-5.75,2.38],[3.2,-1.20,3.35],'pink','character_skin'),
  ('rear_middle_tip',[-1.45,-1.40,2.7],[1.45,-.45,3.65],'pink','character_skin'),
  ('fringe_root',[-1.02,-6.18,-3.04],[.52,-5.60,-2.82],'pink','character_skin'),
  ('fringe_sweep',[-.80,-5.78,-3.15],[.72,-5.22,-2.90],'pink','character_skin'),
  ('fringe_curl',[-.10,-5.42,-3.21],[.80,-4.93,-2.96],'pink','character_skin'),
  ('fringe_tip',[.33,-5.04,-3.24],[.80,-4.68,-2.99],'pink','character_skin')]
 for side,label in ((-1,'right'),(1,'left')):
  def mirror(lo,hi):return ([lo[0]*side,lo[1],lo[2]],[hi[0]*side,hi[1],hi[2]]) if side>0 else ([-hi[0],lo[1],lo[2]],[-lo[0],hi[1],hi[2]])
  for i,(lo,hi) in enumerate((([2.85,-5.15,1.45],[3.62,-2.35,3.16]),([2.6,-2.58,2.38],[3.35,-1.20,3.64]),([2.25,-1.40,2.88],[2.94,-.48,3.85]))):
   lo,hi=mirror(lo,hi);hair.append((f'{label}_quill_{i}',lo,hi,'pink','character_skin'))
  for i,(lo,hi) in enumerate((([1.88,-7.02,-.80],[3.02,-5.85,.56]),([2.12,-7.64,-.56],[2.80,-6.82,.38]))):
   lo,hi=mirror(lo,hi);hair.append((f'{label}_ear_{i}',lo,hi,'pink','character_skin'))
  lo,hi=mirror([2.18,-6.9,-.845],[2.7,-6.12,-.81]);hair.append((f'{label}_inner_ear',lo,hi,'white','character_skin'))
 shapes(d,'character_hair','skull',hair,uv,decor=True)
 body=replace_box(d,'body__cube_0',[-1.9,-3.45,-1.70],[1.9,.45,1.70]);uv_set(body,uv['red'])
 shapes(d,'character_amy_torso','body',[
  ('rounded_belly',[-2.20,-2.10,-1.87],[2.20,-.40,1.87],'red','character_skin'),
  ('lower_torso',[-1.65,.28,-1.48],[1.65,.68,1.48],'red','character_skin'),
  ('collar_front',[-1.82,-3.58,-1.77],[1.82,-3.04,-1.69],'white','character_skin'),
  ('collar_back',[-1.82,-3.58,1.69],[1.82,-3.04,1.77],'white','character_skin'),
  ('collar_left',[1.74,-3.58,-1.70],[1.93,-3.04,1.70],'white','character_skin'),
  ('collar_right',[-1.93,-3.58,-1.70],[-1.74,-3.04,1.70],'white','character_skin')],uv)
 # Amy Chao has a red emotion dot; hands/feet stay pink, with no human boots.
 for b in d['bones']:
  if b['name'].startswith('emotion_dot__'):
   for m in b['meshes']:uv_set(m,uv['red'])
 lenses(d,uv['white']);expressions(d,'baby')
 d['attachment_landmarks']={'eyes':[0,-2,-3.10,2]}
 d['attachment_extra_bones']={'body':['character_amy_torso'],'skull':['character_amy_head']}
 return d


def robot_palette(uv):
 im=Image.open(T/'character_omochao.png').convert('RGBA');raw=source('baby')
 # Find unused original UV texels, including already reserved solid swatches.
 occupied=set()
 for b in raw['bones']:
  for m in b['meshes']:
   for f in m['faces']:
    coords=f.get('gameplay_uv',f['uv'])
    x0=math.floor(min(p[0] for p in coords)*im.width/raw['texture_width']);x1=math.ceil(max(p[0] for p in coords)*im.width/raw['texture_width'])
    y0=math.floor(min(p[1] for p in coords)*im.height/raw['texture_height']);y1=math.ceil(max(p[1] for p in coords)*im.height/raw['texture_height'])
    occupied.update((x,y) for x in range(x0,x1) for y in range(y0,y1))
 for point in uv.values():occupied.add((math.floor(point[0]*im.width/raw['texture_width']),math.floor(point[1]*im.height/raw['texture_height'])))
 free=[(x,y) for y in range(im.height-1,-1,-1) for x in range(im.width-1,-1,-1) if (x,y) not in occupied]
 variable={};values={'plate_dark':(34,99,157),'plate_light':(111,194,228),'plate_main':(83,174,216),'metal_dark':(54,68,81)}
 for (name,rgb),(x,y) in zip(values.items(),free):
  im.putpixel((x,y),(*rgb,255));uv[name]=[(x+.5)*raw['texture_width']/im.width,(y+.5)*raw['texture_height']/im.height]
  if name!='metal_dark':variable[name]=[x,y]
 im.save(T/'character_omochao.png',optimize=True)
 write(A/'omochao_palette_020.json',{'texture_width':im.width,'texture_height':im.height,'variable_pixels':variable,'fixed_pixels':{k:[math.floor(v[0]*im.width/raw['texture_width']),math.floor(v[1]*im.height/raw['texture_height'])] for k,v in uv.items() if k not in variable},'source_layout':'baby'})
 return uv


def omochao():
 d,uv=prepare('omochao',(83,174,216));uv=robot_palette(uv)
 # Keep the exact native UV islands for inherited coat/texture copying, but
 # replace organic cuboids with chamfered mechanical plating.
 replace_box(d,'skull__cube_0',[-3.45,-6.08,-2.98],[3.45,-.04,2.98])
 for b in d['bones']:
  n=b['name']
  if n in ('skull__cube_2','skull__cube_3') or n.startswith(('wing_','tail')):b['meshes']=[]
 head=[('top_bevel',[-3.05,-6.35,-2.58],[3.05,-6.00,2.58],'plate_light','robot_shell'),
       ('lower_bevel',[-3.05,-.12,-2.58],[3.05,.20,2.58],'plate_light','robot_shell'),
       ('front_plate',[-.68,-6.10,-3.035],[.68,.01,-2.985],'plate_dark','robot_shell'),
       ('back_plate',[-.68,-6.10,2.985],[.68,.01,3.035],'plate_dark','robot_shell'),
       ('crown_plate',[-.68,-6.375,-2.58],[.68,-6.335,2.58],'plate_dark','robot_shell')]
 for side in (-1,1):
  for y in (-5.52,-3.33,-.72):
   x=side*3.10;head.append(('front_rivet',[x-.13,y-.13,-3.055],[x+.13,y+.13,-3.01],'gray','robot_hardware'))
  x=side*3.47
  for z in (-1.9,1.9):head.append(('side_rivet',[x-.06,-3.35,z-.14],[x+.06,-3.05,z+.14],'gray','robot_hardware'))
 shapes(d,'character_robot_chassis','skull',head,uv)
 # Exactly one pair of mechanical spiral lenses. The inherited raised organic
 # eye cubes are absent, and no additional generic iris overlays are created.
 for side,label in ((-1,'right'),(1,'left')):
  x=side*1.9;y=-2.35;z=-3.23
  rings=[('eye_socket',[x-1.03,y-1.03,z+.09],[x+1.03,y+1.03,z+.17],'metal_dark','robot_hardware')]
  spiral=[(-.86,-.86,.86,-.58),(.58,-.58,.86,.86),(-.86,.58,.58,.86),(-.86,-.58,-.58,.58),(-.58,-.58,.31,-.31),(.04,-.31,.31,.31),(-.31,.04,.04,.31)]
  for i,(x0,y0,x1,y1) in enumerate(spiral):rings.append((f'spiral_{i}',[x+x0,y+y0,z-.045],[x+x1,y+y1,z+.04],'yellow','robot_hardware'))
  shapes(d,'character_robot_eye_'+label,'skull',rings,uv)
 shapes(d,'character_robot_mouth','skull',[
  ('mouth_middle',[-.55,-.72,-3.11],[.55,-.49,-3.045],'yellow','robot_hardware'),
  ('mouth_left',[-.78,-.95,-3.11],[-.50,-.64,-3.045],'yellow','robot_hardware'),
  ('mouth_right',[.50,-.95,-3.11],[.78,-.64,-3.045],'yellow','robot_hardware')],uv)
 body=replace_box(d,'body__cube_0',[-1.95,-3.50,-1.95],[1.95,.45,1.95])
 shapes(d,'character_robot_belly','body',[
  ('chest_stripe',[-.62,-3.51,-2.025],[.62,.46,-1.955],'plate_dark','robot_shell'),
  ('top_rim',[-1.99,-3.58,-1.99],[1.99,-3.43,1.99],'gray','robot_hardware'),
  ('lower_rim',[-1.99,.34,-1.99],[1.99,.51,1.99],'gray','robot_hardware'),
  ('neck',[-.58,-4.08,-.58],[.58,-3.40,.58],'gray','robot_hardware')],uv)
 for side,label in ((1,'left'),(-1,'right')):
  b=next(b for b in d['bones'] if b['name']==f'arm_{label}__cube_0');b['meshes']=[]
  b=next(b for b in d['bones'] if b['name']==f'arm_{label}__cube_1')
  lo=[2.12,.10,-1.19] if side>0 else [-3.85,.10,-1.19];hi=[3.85,1.85,.54] if side>0 else [-2.12,1.85,.54]
  b['meshes'][0]['vertices']=points(lo,hi)
  cable=[]
  for i in range(5):
   x0=i*.44;lo=[x0,.18,-.42];hi=[x0+.48,.82,.42]
   if side<0:lo[0],hi[0]=-hi[0],-lo[0]
   cable.append((f'cable_segment_{i}',lo,hi,'gray' if i%2==0 else 'metal_dark','robot_hardware'))
  # Sleeves and other fitted accessories measure this semantic upper-arm core.
  # Keep the real cable geometry here rather than an empty placeholder.
  upper_arm=next(b for b in d['bones'] if b['name']==f'arm_{label}__cube_0')
  upper_arm['meshes']=[box(name,lo,hi,uv[color],material) for name,lo,hi,color,material in cable]
  n='foot_'+label+'__cube_0';mesh=next(b for b in d['bones'] if b['name']==n)['meshes'][0]
  lo=[-.1,.08,-4.02] if side>0 else [-2.1,.08,-4.02];hi=[2.1,1.87,.04] if side>0 else [.1,1.87,.04];mesh['vertices']=points(lo,hi)
  lo=[-.14,1.78,-4.06] if side>0 else [-2.14,1.78,-4.06];hi=[2.14,2.10,.08] if side>0 else [.14,2.10,.08]
  shapes(d,'character_robot_sole_'+label,'foot_'+label,[('metal_sole',lo,hi,'orange','robot_hardware')],uv)
 shapes(d,'character_propeller','skull',[
  ('rotor_mast',[-.23,-.36,-.23],[.23,.35,.23],'gray','robot_hardware'),
  ('rotor_hub',[-.48,-.66,-.48],[.48,-.35,.48],'orange','robot_hardware'),
  ('blade_left',[-2.45,-.57,-.30],[-.45,-.38,.30],'orange','robot_hardware'),
  ('blade_right',[.45,-.57,-.30],[2.45,-.38,.30],'orange','robot_hardware')],uv,decor=True,pivot=(0,-6.4,0))
 shapes(d,'character_robot_key_shaft','body',[
  ('key_shaft',[-.23,-2.88,1.90],[.23,-2.42,3.28],'gray','robot_hardware')],uv)
 shapes(d,'character_windup_key','body',[
  ('left_top',[-1.72,-.82,-.16],[-.12,-.51,.16],'gray','robot_hardware'),
  ('right_top',[.12,-.82,-.16],[1.72,-.51,.16],'gray','robot_hardware'),
  ('left_bottom',[-1.72,.51,-.16],[-.12,.82,.16],'gray','robot_hardware'),
  ('right_bottom',[.12,.51,-.16],[1.72,.82,.16],'gray','robot_hardware'),
  ('left_edge',[-1.72,-.51,-.16],[-1.40,.51,.16],'gray','robot_hardware'),
  ('right_edge',[1.40,-.51,-.16],[1.72,.51,.16],'gray','robot_hardware'),
  ('middle',[-.19,-.82,-.16],[.19,.82,.16],'gray','robot_hardware')],uv,pivot=(0,-2.65,3.28))
 d['attachment_landmarks']={'eyes':[0,-2.35,-3.23,1.9]}
 d['robot_inherited_uv']='Native baby skin islands on skull/body/hands/feet; unused hardware swatches remain fixed.'
 return d

def amy_robot_coat():
 # Amy's red torso is an authored solid-UV mesh on her organic rig. Transfer
 # that semantic body colour into the robot's native torso UV island as well.
 im=Image.open(T/'character_amy.png').convert('RGBA');native=source('baby')
 torso=next(b for b in native['bones'] if b['name']=='body__cube_0')
 for mesh in torso['meshes']:
  for face in mesh['faces']:
   coords=face.get('gameplay_uv',face['uv']);sx=im.width/native['texture_width'];sy=im.height/native['texture_height']
   for y in range(math.floor(min(v[1] for v in coords)*sy),math.ceil(max(v[1] for v in coords)*sy)):
    for x in range(math.floor(min(v[0] for v in coords)*sx),math.ceil(max(v[0] for v in coords)*sx)):
     im.putpixel((x,y),(208,55,80,255))
 im.save(T/'robot_coat_amy.png',optimize=True)


def main():
 original=ROOT/'art/imported_chao';before={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in original.rglob('*') if p.is_file()}
 write(M/'chao_amy.json',amy());write(M/'chao_omochao.json',omochao());amy_robot_coat()
 manifest=json.loads((A/'gameplay_forms_019.json').read_text())
 manifest['models']=[x for x in manifest['models'] if x['key'] not in ('spartoi','egg')]
 for record in manifest['models']:
  if record['key'] in ('amy','omochao'):record['character_revision']='0.20.0'
 write(A/'gameplay_forms_019.json',manifest)
 assert before=={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in original.rglob('*') if p.is_file()}
 print('Characters020: refined connected Amy silhouette; mechanical Omochao with one spiral-eye pair, native inherited-coat UVs and fixed hardware.')
if __name__=='__main__':main()
