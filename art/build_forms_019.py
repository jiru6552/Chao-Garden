#!/usr/bin/env python3
"""Authored block-built primary/secondary Chao forms on untouched supplied anchors.

The15 primary rigs each contain nine topology-identical vertex targets. Runtime
bilinear interpolation follows both training axes, without threshold mesh swaps.
Reference pictures inform morphology; no reference picture pixels are copied.
"""
from pathlib import Path
from collections import Counter
import copy,hashlib,json,math
from PIL import Image,ImageDraw
from import_official_chao import KEYS,native_skin_masks,recolor_native,uv_regions

ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/chao_garden'
MODEL=ASSET/'models/entity'
ART=ROOT/'art/imported_chao'
AXES=[(sf,rp) for sf in (-1,0,1) for rp in (-1,0,1)]
BASE={'baby':'baby','swim':'swim','fly':'fly','run':'run','power':'power',
      'hero':'hero','hero_swim':'swim','hero_fly':'hero_fly','hero_run':'run','hero_power':'hero_power',
      'dark':'dark','dark_swim':'dark_swim','dark_fly':'fly','dark_run':'dark_run','dark_power':'power'}
MISSING={'hero_swim','hero_run','dark_fly','dark_power'}
SIDES=[[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[0,4,7,3],[1,2,6,5]]

def write(p,j):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(j,separators=(',',':'))+'\n')
def points(lo,hi):
 x,y,z=lo;X,Y,Z=hi
 return [[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]]
def box(name,lo,hi,uv,material='official'):
 return {'name':name,'material':material,'shading':'flat','vertices':points(lo,hi),
   'faces':[{'v':v,'uv':[list(uv) for i in v]} for v in SIDES]}
def add_bone(data,name,parent,meshes,pivot=(0,0,0),decor=False):
 data['bones'].append({'name':name,'parent':parent,'pivot':list(pivot),'rotation':[0,0,0],'meshes':meshes})
 if decor:data['head_decorations'].append(name)
def source(key):return json.loads((MODEL/'official'/f'chao_{key}.json').read_text())
def source_geo(key):return json.loads((ART/'models'/('chao_'+key+('.json' if key=='dad' else '.geo.json'))).read_text())['minecraft:geometry'][0]
def source_image(key):return Image.open(ART/'textures'/f'{key}.png').convert('RGBA')
def samples(key):
 g=source_geo(key);im=source_image(key);mask,eyes,base=native_skin_masks(g,im)
 main=[];accent=[];white=[];protected_white=[]
 for y in range(im.height):
  for x in range(im.width):
   p=im.getpixel((x,y))
   if not p[3]:continue
   uv=((x+.5)*g['description']['texture_width']/im.width,(y+.5)*g['description']['texture_height']/im.height)
   if mask.getpixel((x,y)) and not eyes.getpixel((x,y)):
    (main if sum((p[i]-base[i])**2 for i in range(3))<50**2 else accent).append((uv,p))
   if min(p[:3])>205 and max(p[:3])-min(p[:3])<22:
    white.append((uv,p))
    if eyes.getpixel((x,y)) and not mask.getpixel((x,y)):protected_white.append((uv,p))
 def pick(values,fallback):
  if not values:return fallback
  color=Counter(p for uv,p in values).most_common(1)[0][0]
  return next(uv for uv,p in values if p==color)
 m=pick(main,(.5,.5));a=pick(accent,m)
 # Genetic lenses multiply their source texel. Select neutral eye pigment,
 # never a common white skin swatch that palette recoloring can tint.
 candidates=protected_white or [v for v in white if eyes.getpixel((int(v[0][0]*im.width/g['description']['texture_width']),int(v[0][1]*im.height/g['description']['texture_height']))) ] or white
 w=max(candidates,key=lambda v:min(v[1][:3]))[0] if candidates else m
 return m,a,w

def descendants(data,parent):
 names={parent}
 for _ in data['bones']:
  names.update(b['name'] for b in data['bones'] if b.get('parent') in names)
 return names

def marker(data,alignment,uv):
 if alignment=='neutral':return
 original=source('hero' if alignment=='hero' else 'dark');branch=descendants(original,'emotion_dot')
 data['bones']=[b for b in data['bones'] if b['name']=='emotion_dot' or b['name'] not in descendants(data,'emotion_dot')]
 for b in original['bones']:
  if b['name'] in branch and b['name']!='emotion_dot':
   added=copy.deepcopy(b)
   for m in added['meshes']:
    for f in m['faces']:f['uv']=[list(uv) for _ in f['v']]
   data['bones'].append(added)

def head_bounds(data):
 choices=[]
 for b in data['bones']:
  if b['name'].startswith('skull__cube_'):
   vs=[v for m in b['meshes'] for v in m['vertices']]
   if vs:choices.append((math.prod(max(v[i] for v in vs)-min(v[i] for v in vs) for i in range(3)),b,vs))
 _,b,vs=max(choices,key=lambda a:a[0]);return b,min(v[1]+b['pivot'][1] for v in vs),max(v[1]+b['pivot'][1] for v in vs)

def primary(data,key,uv):
 if key not in MISSING:return
 # The four newly authored primaries replace the donor's head ornament, rather
 # than retaining an unrelated donor silhouette beneath new protrusions.
 hidden=set(data['head_decorations'])
 for b in data['bones']:
  if b['name'] in hidden:b['meshes']=[]
 data['head_decorations']=[]
 _,top,_=head_bounds(data)
 meshes=[]
 if key=='hero_swim':
  for side in (-1,1):
   for i in range(4):
    cx=side*(.95+i*.35);cy=top-.45+i*.30;z=.4+i*.78
    meshes.append(box('folded_swim_feeler', [cx-.50,cy-.30,z-.2],[cx+.50,max(cy+.30,top+.18) if i==0 else cy+.30,z+1.0],uv))
 elif key=='hero_run':
  for i in range(3):
   meshes.append(box('single_run_feeler',[-.46+i*.08,top-.65-i*.35,.15+i*.38],[.46-i*.08,top+.08-i*.35,1.10+i*.38],uv))
 elif key=='dark_fly':
  for side in (-1,1):
   for i in range(3):
    cx=side*(1.1+i*.95);cy=top-.5-i*.66;z=.3+i*.65
    meshes.append(box('forked_fly_horn',[cx-(.85-i*.17),cy-.48,z-.3],[cx+(.85-i*.17),max(cy+.48,top+.18) if i==0 else cy+.48,z+1.0],uv))
 elif key=='dark_power':
  for i in range(4):
   w=1.6-i*.36
   meshes.append(box('power_crown_tier',[-w,top-.5-i*.65,.1+i*.24],[w,top+.25-i*.65,2.0+i*.24],uv))
 add_bone(data,'evolution_primary_crown','skull',meshes,decor=True)


def decorate(data,key,main,accent):
 _,top,_=head_bounds(data)
 # Five connected stepped ridges participate in every topology. At zero
 # influence they sit inside the core head, so the original base stays quiet.
 is_run=key.endswith('run');is_swim=key.endswith('swim');is_fly=key.endswith('fly');is_power=key.endswith('power')
 for lane in range(5):
  side=0 if lane==0 else -1 if lane in (1,3) else 1
  level=0 if lane<3 else 1
  segments=[]
  for segment in range(3):
   targets=[]
   for sf,rp in AXES:
    swim=max(0,-sf);fly=max(0,sf);run=max(0,-rp);power=max(0,rp);active=max(abs(sf),abs(rp))
    length=(.8+run*(2.1+is_run*.9)+fly*.9+swim*(.7+is_swim*.4))
    spread=(1.9+level*.45+fly*.65+swim*.35)*side
    cx=spread*active+(side*.12*segment*(fly+.3*swim))
    cy=top+1.15+level*.9-active*(.65+power*1.25+fly*.45)+segment*(.24*run+.25*swim-.35*power)
    z=1.05+segment*length*.55
    half_x=.035+( .38+power*.20+fly*.14)*(active)*(1-.19*segment)
    half_y=.035+(.30+power*.25+swim*.10)*active*(1-.15*segment)
    depth=.07+(length*.74)*active*(1-.11*segment)
    if lane==0:
     half_x*=.85;cy-=active*(.2+.3*power);z+=.15
    lo=[cx-half_x,cy-half_y,z];hi=[cx+half_x,cy+half_y,z+depth]
    if segment==0:hi[1]=max(hi[1],top+.18)
    targets.append(points(lo,hi))
   m=box('stepped_development_ridge',[0,0,0],[1,1,1],accent if segment==2 else main)
   m['vertices']=targets[4];m['morph_vertices']=targets;segments.append(m)
  add_bone(data,f'evolution_ridge_{lane}','skull',segments,decor=True)


def monster_crown(data,uv):
 _,top,_=head_bounds(data)
 meshes=[]
 for side in (-1,0,1):
  for tier in range(3):
   cx=side*(1.8+tier*.2);w=.8-tier*.17
   meshes.append(box('monster_crown_step',[cx-w,top-.3-tier*.8,.4+tier*.6],[cx+w,top+.5-tier*.8,2.1+tier*.6],uv))
 add_bone(data,'monster_crown','skull',meshes,decor=True)


def morph_source(data):
 for b in data['bones']:
  n=b['name']
  if n.startswith('evolution_') or n.startswith('emotion'):continue
  for mesh in b['meshes']:
   if mesh.get('material')=='lens':continue
   vs=mesh['vertices'];lo=[min(v[i] for v in vs) for i in range(3)];hi=[max(v[i] for v in vs) for i in range(3)]
   targets=[]
   for sf,rp in AXES:
    swim=max(0,-sf);fly=max(0,sf);run=max(0,-rp);power=max(0,rp)
    modified=[]
    for v in vs:
     x,y,z=v
     if n.startswith('skull__'):
      x*=1+.07*swim+.07*power-.025*run
      z=lo[2]+(z-lo[2])*(1+.14*run+.09*fly-.025*swim)
      # Keep face panels stationary relative to the preserved front plane.
      if math.prod(hi[i]-lo[i] for i in range(3))>80:y=hi[1]+(y-hi[1])*(1+.07*power-.04*swim)
     elif n.startswith('body__'):
      x*=1+.13*power+.045*swim;z*=1+.07*swim
     elif n.startswith(('foot_','feet_')):
      x*=1+.12*swim+.06*power;z=hi[2]-(hi[2]-z)*(1+.15*swim+.10*run)
     elif n.startswith('arm_'):x*=1+.12*power
     elif n.startswith('wing_'):x*=1+.32*fly;y*=1+.15*fly
     elif n.startswith('tail'):z*=1+.18*swim+.14*run
     modified.append([round(x,6),round(y,6),round(z,6)])
    targets.append(modified)
   mesh['morph_vertices']=targets


def lenses(data,white):
 for b in data['bones']:
  if not b['name'].startswith('skull__'):continue
  for m in list(b['meshes']):
   vs=m['vertices'];lo=[min(v[i] for v in vs) for i in range(3)];hi=[max(v[i] for v in vs) for i in range(3)]
   if not(1.8<=hi[0]-lo[0]<=2.5 and 1.8<=hi[1]-lo[1]<=2.5 and .8<=hi[2]-lo[2]<=1.5):continue
   cx=(lo[0]+hi[0])/2;cy=(lo[1]+hi[1])/2
   b['meshes'].append(box('colored_iris',[cx-.42,cy-.72,lo[2]-.035],[cx+.42,cy+.72,lo[2]-.012],white,'lens'))


def expressions(data,donor):
 im=source_image(donor);g=source_geo(donor)
 black=min(((x,y) for y in range(im.height) for x in range(im.width) if im.getpixel((x,y))[3]),key=lambda xy:sum(im.getpixel(xy)[:3]))
 uv=[(black[0]+.5)*g['description']['texture_width']/im.width,(black[1]+.5)*g['description']['texture_height']/im.height]
 b,top,bottom=head_bounds(data);vs=[v for m in b['meshes'] for v in m['vertices']]
 front=min(v[2]+b['pivot'][2] for v in vs)-.045;center=bottom-1.1
 shapes={
 1:[([-.75,center-.15,front],[.75,center+.1,front+.025]),([-1,center-.6,front],[-.7,center,front+.025]),([.7,center-.6,front],[1,center,front+.025])],
 2:[([-.7,center-.6,front],[.7,center-.35,front+.025]),([-.95,center-.5,front],[-.65,center+.05,front+.025]),([.65,center-.5,front],[.95,center+.05,front+.025])],
 3:[([-.85,center-.2,front],[.85,center+.06,front+.025])],
 4:[([-.55,center-.8,front],[.55,center+.15,front+.025])],
 5:[([-.9,center-.25,front],[-.1,center,front+.025]),([-.1,center-.05,front],[.65,center+.2,front+.025])],
 6:[([-.55,center-.7,front],[.55,center+.10,front+.025])]}
 for kind,boxes in shapes.items():
  add_bone(data,'expression_mouth_'+str(kind),'skull',[box('mouth',lo,hi,uv,'expression_'+str(kind)) for lo,hi in boxes])


def texture_mask(key):
 g=source_geo(key);im=source_image(key);mask,eyes,_=native_skin_masks(g,im)
 out=Image.new('RGBA',im.size)
 for y in range(im.height):
  for x in range(im.width):
   if mask.getpixel((x,y)) and not eyes.getpixel((x,y)):out.putpixel((x,y),(255,255,255,255))
 p=ASSET/'textures/entity/chao/gameplay/masks'/f'{key}.png';p.parent.mkdir(parents=True,exist_ok=True);out.save(p,optimize=True)
 wing=Image.new('RGBA',im.size);draw=ImageDraw.Draw(wing)
 sx=im.width/g['description']['texture_width'];sy=im.height/g['description']['texture_height']
 for bone in g['bones']:
  if 'wing' in bone['name']:
   for cube in bone.get('cubes',[]):
    for x0,y0,x1,y1 in uv_regions(cube):
     if x1>x0 and y1>y0:draw.rectangle([int(x0*sx),int(y0*sy),math.ceil(x1*sx)-1,math.ceil(y1*sy)-1],fill=(255,255,255,255))
 wing.save(p.with_name(key+'_wings.png'),optimize=True)
 accent=Image.new('RGBA',im.size)
 _,eye_mask,base=native_skin_masks(g,im)
 for y in range(im.height):
  for x in range(im.width):
   rgb=im.getpixel((x,y))
   if rgb[3] and not eye_mask.getpixel((x,y)) and max(rgb[:3])-min(rgb[:3])>65 and sum((rgb[i]-base[i])**2 for i in range(3))>70**2:accent.putpixel((x,y),(255,255,255,255))
 accent.save(p.with_name(key+'_accent.png'),optimize=True)


def overlay_masks():
 data=json.loads((MODEL/'overlays/chao_baby.json').read_text())
 width,height=data['texture_width'],data['texture_height']
 out=Image.new('RGBA',(width,height));draw=ImageDraw.Draw(out)
 for bone in data['bones']:
  for mesh in bone['meshes']:
   if mesh['material'] not in ('head','tuft','accent','crest_tip','collar'):continue
   for face in mesh['faces']:
    uv=face['uv'];x0=max(0,math.floor(min(p[0] for p in uv)));x1=min(width-1,math.ceil(max(p[0] for p in uv)));y0=max(0,math.floor(min(p[1] for p in uv)));y1=min(height-1,math.ceil(max(p[1] for p in uv)))
    draw.rectangle((x0,y0,x1,y1),fill=(255,255,255,255))
 p=ASSET/'textures/entity/chao/gameplay/masks/overlay.png';out.save(p,optimize=True)
 Image.new('RGBA',(width,height)).save(p.with_name('overlay_wings.png'),optimize=True)
 accent=Image.new('RGBA',(width,height));ad=ImageDraw.Draw(accent)
 for bone in data['bones']:
  for mesh in bone['meshes']:
   if mesh['material'] not in ('tuft','accent','crest_tip'):continue
   for face in mesh['faces']:
    uv=face['uv'];ad.rectangle((math.floor(min(p[0] for p in uv)),math.floor(min(p[1] for p in uv)),math.ceil(max(p[0] for p in uv)),math.ceil(max(p[1] for p in uv))),fill=(255,255,255,255))
 accent.save(p.with_name('overlay_accent.png'),optimize=True)


def texture_character(base,name,color):
 g=source_geo(base);im=recolor_native(g,source_image(base),color,False)
 # Reserve only texels outside every original UV island. Original image bytes
 # stay under official/; new character palettes belong to a separate texture.
 occupied=set()
 for b in source(base)['bones']:
  for m in b['meshes']:
   for f in m['faces']:
    uv=f.get('gameplay_uv',f['uv']);sx=im.width/g['description']['texture_width'];sy=im.height/g['description']['texture_height'];x0=max(0,math.floor(min(p[0] for p in uv)*sx));x1=min(im.width,math.ceil(max(p[0] for p in uv)*sx));y0=max(0,math.floor(min(p[1] for p in uv)*sy));y1=min(im.height,math.ceil(max(p[1] for p in uv)*sy))
    occupied.update((x,y) for x in range(x0,x1) for y in range(y0,y1))
 free=[(x,y) for y in range(im.height-1,-1,-1) for x in range(im.width-1,-1,-1) if (x,y) not in occupied]
 palette={'pink':(234,126,168),'red':(208,55,80),'white':(239,238,216),'black':(34,43,54),'gray':(156,170,176),'blue':(74,162,211),'yellow':(246,216,79),'teal':(73,224,219),'purple':(93,28,132),'magenta':(155,29,160),'orange':(222,119,37)}
 assert len(free)>=len(palette)
 uv={}
 for (key,rgb),(x,y) in zip(palette.items(),free):im.putpixel((x,y),(*rgb,255));uv[key]=[(x+.5)*g['description']['texture_width']/im.width,(y+.5)*g['description']['texture_height']/im.height]
 p=ASSET/'textures/entity/chao/gameplay'/f'character_{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p,optimize=True)
 return uv


def character(name):
 base='dark' if name=='spartoi' else 'baby';data=copy.deepcopy(source(base));colors={'amy':(237,143,182),'spartoi':(143,42,158),'omochao':(83,174,216),'egg':(224,225,202)}
 uv=texture_character(base,name,colors[name])
 for b in data['bones']:
  if b['name'] in data['head_decorations']:b['meshes']=[]
 data['head_decorations']=[]
 def shape(bone,parent,boxes,decor=False):
  names={b['name'] for b in data['bones']}
  if parent=='foot_right' and parent not in names and 'feet_bone_right' in names:parent='feet_bone_right'
  assert parent in names,(name,bone,parent)
  add_bone(data,bone,parent,[box(n,lo,hi,uv[c]) for n,lo,hi,c in boxes],decor=decor)
 if name=='amy':
  shape('character_hair','skull',[
    ('crown',[-3.7,-6.8,-2.8],[3.7,-5.8,3.4],'pink'),
    ('headband',[-3.8,-6.2,-3.14],[3.8,-5.7,-2.98],'red'),
    ('back_hair',[-3.5,-5.8,2.3],[3.5,-2.1,4.2],'pink'),
    ('left_fringe',[-3.6,-5.8,-3.3],[-1.5,-4.6,-3.1],'pink'),
    ('right_fringe',[1.5,-5.8,-3.3],[3.6,-4.6,-3.1],'pink')],True)
  shape('character_dress','body',[('dress',[-2.6,-2.3,-2.0],[2.6,-.3,2.0],'red'),('hem',[-2.9,-.6,-2.2],[2.9,.2,2.2],'white')])
 elif name=='spartoi':
  shape('character_spartoi_brow','skull',[
   ('scalp',[-3.5,-7.1,-2.5],[3.5,-5.9,2.8],'purple'),
   ('brow_middle',[-1.1,-6.3,-3.2],[1.1,-4.9,-3.0],'purple'),
   ('brow_left',[-3.6,-6.0,-3.2],[-1.1,-5.2,-3.0],'purple'),
   ('brow_right',[1.1,-6.0,-3.2],[3.6,-5.2,-3.0],'purple')],True)
  shape('character_spartoi_horns','skull',[
   (f'horn_{side}_{i}',[side*2.7-(.65-i*.15),-7.1-i*.65,.5],[side*2.7+(.65-i*.15),-5.8-i*.65,1.6],'blue') for side in (-1,1) for i in range(3)],True)
  shape('character_spartoi_bib','body',[
   ('collar',[-1.9,-4.5,-1.63],[1.9,-3.6,-1.4],'white'),
   ('center',[-.6,-3.9,-1.65],[.6,-1.5,-1.4],'white'),
   ('left_fang',[-1.5,-3.9,-1.65],[-.8,-2.5,-1.4],'white'),
   ('right_fang',[.8,-3.9,-1.65],[1.5,-2.5,-1.4],'white')])
  for side,label in ((1,'left'),(-1,'right')):
   shape('character_spartoi_boot_'+label,'foot_'+label,[('blue_boot',[0 if side>0 else -2.2,-.05,-4.6],[2.2 if side>0 else 0,2.05,.1],'blue')])
   shape('character_spartoi_hand_'+label,'arm_'+label,[('pink_hand',[1.1 if side>0 else -2.1,-.06,-.56],[2.1 if side>0 else -1.1,1.06,.56],'pink')])
   cx=side*1.9
   shape('character_spartoi_eye_'+label,'skull',[
    ('black_rim',[cx-.70,-4.95,-3.20],[cx+.70,-3.45,-3.08],'black'),
    ('gold_iris',[cx-.47,-4.8,-3.25],[cx+.47,-3.60,-3.20],'yellow'),
    ('gold_tip',[cx-.2,-5.0,-3.25],[cx+.2,-3.4,-3.20],'yellow')])
 elif name=='omochao':
  robot=[]
  for side in (-1,1):
   x=side*1.9
   robot.extend([('eye_top',[x-.9,-5.0,-3.25],[x+.9,-4.6,-3.1],'yellow'),('eye_bottom',[x-.9,-3.45,-3.25],[x+.9,-3.05,-3.1],'yellow'),('eye_outer',[x-.9,-4.6,-3.25],[x-.5,-3.45,-3.1],'yellow'),('eye_inner',[x+.5,-4.6,-3.25],[x+.9,-3.45,-3.1],'yellow')])
  robot.append(('mouth',[-.65,-1.7,-3.17],[.65,-1.35,-3.03],'yellow'))
  shape('character_robot','skull',robot)
  shape('character_propeller','skull',[
   ('key_mast',[-.25,-7.3,-.25],[.25,-6,.25],'orange'),
   ('key_top',[-1.85,-8.6,-.35],[1.85,-8.2,.35],'orange'),('key_bottom',[-1.85,-7.5,-.35],[1.85,-7.1,.35],'orange'),
   ('key_l',[-1.85,-8.2,-.35],[-1.45,-7.5,.35],'orange'),('key_c',[-.2,-8.2,-.35],[.2,-7.5,.35],'orange'),('key_r',[1.45,-8.2,-.35],[1.85,-7.5,.35],'orange')],True)
  shape('character_robot_belly','body',[('panel',[-1.4,-3.8,-1.62],[1.4,-1.4,-1.4],'gray'),('button',[-.4,-2.9,-1.73],[.4,-2.1,-1.60],'red')])
 elif name=='egg':
  for b in data['bones']:
   if b['name']=='body' or b['name'].startswith('body__') or b['name'].startswith('skull__'):b['meshes']=[]
  shape('character_egg_shell','skull',[
   ('base',[-2.8,1.6,-2.5],[2.8,3.5,2.5],'white'),('waist',[-3.8,-2.6,-3.3],[3.8,1.6,3.3],'white'),('shoulder',[-3.2,-5.5,-2.9],[3.2,-2.6,2.9],'white'),('cap',[-2,-7,-1.9],[2,-5.5,1.9],'yellow'),
   ('eye_left',[-2.3,-2.4,-3.39],[-1.1,-.9,-3.34],'black'),('eye_right',[1.1,-2.4,-3.39],[2.3,-.9,-3.34],'black'),
   ('spot1',[-2,-2.1,-3.34],[-.8,-.9,-3.29],'blue'),('spot2',[1,-3.9,-2.94],[2.4,-2.7,-2.89],'blue')])
 lenses(data,samples(base)[2]);
 if name!='egg':expressions(data,base)
 data['authored_character']=name;data['source_uv_layout']=base
 return data


def main():
 raw_before={k:hashlib.sha256((ART/'textures'/f'{k}.png').read_bytes()).hexdigest() for k in KEYS};manifest=[]
 for key in list(KEYS)+[k for k in BASE if k not in KEYS]:
  donor=BASE.get(key,key);data=copy.deepcopy(source(donor));main_uv,accent,white=samples(donor)
  if key in BASE:
   primary(data,key,main_uv);marker(data,'hero' if key.startswith('hero') else 'dark' if key.startswith('dark') else 'neutral',accent)
   decorate(data,key,main_uv,accent);morph_source(data);monster_crown(data,accent)
  lenses(data,white);expressions(data,donor)
  data['source_uv_layout']=donor;data['authored_gameplay_form']=key;data['development_axes']=[list(v) for v in AXES] if key in BASE else []
  write(MODEL/'gameplay'/f'chao_{key}.json',data)
  manifest.append({'key':key,'source_uv_layout':donor,'morph_endpoints':9 if key in BASE else 0,'sha256':hashlib.sha256((MODEL/'gameplay'/f'chao_{key}.json').read_bytes()).hexdigest()})
 for key in KEYS:texture_mask(key)
 overlay_masks()
 for key in ('amy','omochao'):
  data=character(key);write(MODEL/'gameplay'/f'chao_{key}.json',data);manifest.append({'key':key,'source_uv_layout':data['source_uv_layout'],'authored_character':True,'morph_endpoints':0})
 assert raw_before=={k:hashlib.sha256((ART/'textures'/f'{k}.png').read_bytes()).hexdigest() for k in KEYS}
 write(ASSET/'gameplay_forms_019.json',{'primary_forms':15,'adult_endpoints':135,'axes':[list(a) for a in AXES],'models':manifest,
   'source':'Authored cuboid adaptations informed by Chao Island primary and advanced evolution galleries; raw source rigs untouched.'})
 print('Gameplay forms019:15primaries×9continuousendpoints;Amy/Omochao character bases;20nativeUVskinmasks;raw20inputsunchanged.')

if __name__=='__main__':main()
