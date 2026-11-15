"""Block-translated wearable meshes using retained CWE colors and texture regions."""
from pathlib import Path
import sys,json,copy
import numpy as np
from PIL import Image,ImageChops

def copy_faces(dest,other):dest.faces.extend(other.faces)
def solid_box(bounds,key,uv,g):
 m=g.Mesh();m.box(bounds[:3],bounds[3:],key)
 m.faces=[(ps,[uv]*len(ps),k) for ps,t,k in m.faces];return m

def ring(outline,cx,key,uv,g):
 outer=[(cx+x,y) for x,y in outline];inner=[(cx+x*.79,y*.79) for x,y in outline];m=g.Mesh();a,b=-3.24,-3.05
 for i in range(len(outer)):
  j=(i+1)%len(outer);o,O=outer[i],outer[j];q,Q=inner[i],inner[j]
  for pts in [[[*o,a],[*O,a],[*Q,a],[*q,a]],[[*q,b],[*Q,b],[*O,b],[*o,b]],[[*o,b],[*O,b],[*O,a],[*o,a]],[[*q,a],[*Q,a],[*Q,b],[*q,b]]]:m.face(pts,key,[uv]*4)
 return m,inner

def lens(contour,key,g):
 m=g.Mesh();xs=[p[0] for p in contour];ys=[p[1] for p in contour];cx=(min(xs)+max(xs))*.5;cy=(min(ys)+max(ys))*.5
 def uv(p):return (.02734375+(p[0]-min(xs))/(max(xs)-min(xs))*.890625,.046875+(p[1]-min(ys))/(max(ys)-min(ys))*.5390625)
 for i in range(len(contour)):
  j=(i+1)%len(contour);p,q=contour[i],contour[j]
  for z,reverse in [(-3.17,False),(-3.10,True)]:
   pts=[[cx,cy,z],[*p,z],[*q,z]]
   if reverse:pts.reverse()
   m.face(pts,key,[uv(p) for p in pts])
  pts=[[*p,-3.17],[*q,-3.17],[*q,-3.10],[*p,-3.10]];m.face(pts,key,[uv(p) for p in pts])
 return m

def stepped(name,tiles,g):
 round_=name.startswith('round');key='cwe_capmain_0d0d0dff_0' if round_ else 'cwe_aviators_000007ff_0';uv=(.33984375,.5) if round_ else (.88671875,.890625)
 # Orthogonal edges keep both silhouettes in Minecraft's stepped visual language.
 circle=[(-.45,-.95),(.45,-.95),(.45,-.8),(.8,-.8),(.8,-.45),(1.05,-.45),(1.05,.45),(.8,.45),(.8,.8),(.45,.8),(.45,.95),(-.45,.95),(-.45,.8),(-.8,.8),(-.8,.45),(-1.05,.45),(-1.05,-.45),(-.8,-.45),(-.8,-.8),(-.45,-.8)]
 tear=[(-1.05,-.6),(-1.05,-.75),(-.85,-.75),(-.85,-.85),(.75,-.85),(.75,-.7),(1.05,-.7),(1.05,-.15),(.9,-.15),(.9,.3),(.7,.3),(.7,.55),(.45,.55),(.45,.85),(.1,.85),(.1,.7),(-.3,.7),(-.3,.5),(-.55,.5),(-.55,.25),(-.8,.25),(-.8,-.1),(-1.05,-.1)]
 data={'texture_width':1024,'texture_height':1024,'bones':[{'name':'accessory','pivot':[0,0,0],'rotation':[0,0,0],'meshes':[]}],'accessory_fit':{'eyes':[0,0,-3.145,1.55],'front_back':-3.05,'reference_bounds':[-3.05,-.95,-3.24,3.05,.95,1.3]},'authored_metadata':{'source':'Supplied CWE 9.6.2.2 colors and design reference','release':'0.25.79','fit':'complete stepped front; source material UV bands; articulated cuboid temples with exact fixed seams'}}
 def entity(m,name):
  x=g.wearable_mesh(m,tiles,1024);x['name']=name;return x
 for side,sign in [('left',-1),('right',1)]:
  contour=circle if round_ else [(x*sign,y) for x,y in tear]
  if not round_ and sign<0:contour=list(reversed(contour))
  frame,hole=ring(contour,sign*1.55,key,uv,g);data['bones'][0]['meshes'].append(entity(frame,'frame_'+side))
  if not round_:data['bones'][0]['meshes'].append(entity(lens(hole,'cwe_aviators_9f2200ff_0',g),'lens_'+side))
  inner=[sign*2.60,-.05,-3.05];join=[sign*2.60,-.05,-2.60];contact=[sign*2.60,-.05,1.30]
  strip=solid_box([-.065,-.08,0,.065,.08,.45],key,uv,g)
  temple=solid_box([-.065,-.08,0,.065,.08,3.90],key,uv,g)
  data['bones'].extend([{'name':'hinge_'+side,'parent':'accessory','pivot':inner,'rotation':[0,0,0],'meshes':[]},{'name':'hinge_strip_'+side,'parent':'hinge_'+side,'pivot':[0,0,0],'rotation':[0,0,0],'meshes':[entity(strip,'hinge_strip_'+side)]},{'name':'temple_'+side,'parent':'accessory','pivot':join,'rotation':[0,0,0],'meshes':[entity(temple,'temple_'+side)]}])
  data['accessory_fit'][side]={'bone':'temple_'+side,'hinge':'hinge_'+side,'strip':'hinge_strip_'+side,'inner':inner,'join':join,'contact':contact}
 bridge=solid_box([-.67,-.30,-3.24,.67,-.12,-3.05],key,uv,g);data['bones'][0]['meshes'].append(entity(bridge,'bridge'))
 return data


HEAD=[-3.5,-6,-3,3.5,0,3]
BODY=[-2,-3.5,-2,2,.5,2]
WHITE='cwe_alo_mask_teethingring00_ffffffff_0'

def boxes(g,parts,key,uv=(.5,.5)):
 m=g.Mesh()
 for bounds in parts:m.faces.extend(solid_box(bounds,key,uv,g).faces)
 return m

def garment(name,g):
 """Canonical joint-local shells; white source lining stays separate from dye."""
 trim=g.Mesh()
 if name=='hoodie_hood':
  kind='head'
  cloth=boxes(g,[[-3.86,-6.38,-3.38,3.86,-6.12,3.38],[-3.86,-6.12,-3.38,-3.62,.15,3.38],[3.62,-6.12,-3.38,3.86,.15,3.38],[-3.62,-6.12,3.14,3.62,.15,3.38],[-3.62,-6.12,-3.5,3.62,-3.4,-3.26],[-3.62,-3.4,-3.5,-3.2,.15,-3.26],[3.2,-3.4,-3.5,3.62,.15,-3.26]],WHITE)
  # The face aperture is open down to the neck: no cloth crosses either iris
  # or the mouth. Its lining sits entirely outside the clear rectangle.
  trim=boxes(g,[[-3.36,-3.56,-3.53,3.36,-3.4,-3.5],[-3.36,-3.4,-3.53,-3.2,.15,-3.5],[3.2,-3.4,-3.53,3.36,.15,-3.5]],WHITE)
 elif name=='hoodie_hood_folded':
  kind='body'
  cloth=boxes(g,[[-1.95,-3.62,.9,1.95,-3.27,2.28],[-1.75,-3.27,1.45,1.75,-2.75,2.3]],WHITE)
  trim=boxes(g,[[-1.65,-3.66,1.2,1.65,-3.59,2.14]],WHITE)
 elif name.startswith('hoodie_sleeve_'):
  left=name.endswith('left');kind='arm_left' if left else 'arm_right'
  lo,hi=(-.12,2.12) if left else (-2.12,.12)
  cloth=boxes(g,[[lo,-.12,-.64,hi,.02,.64],[lo,.98,-.64,hi,1.12,.64],[lo,.02,-.64,hi,.98,-.50],[lo,.02,.50,hi,.98,.64]],WHITE)
  end=hi if left else lo;trim=boxes(g,[[end-.07,-.14,-.66,end+.07,1.14,-.5],[end-.07,-.14,.5,end+.07,1.14,.66],[end-.07,-.14,-.5,end+.07,.02,.5],[end-.07,.98,-.5,end+.07,1.14,.5]],WHITE)
 else:
  kind='body'
  cloth=boxes(g,[[-2.14,-3.6,-2.14,-2.02,.6,2.14],[2.02,-3.6,-2.14,2.14,.6,2.14],[-2.02,-3.6,2.02,2.02,.6,2.14],[-2.02,-2.5,-2.14,2.02,.6,-2.02],[-2.02,-3.6,-2.14,-.8,-2.5,-2.02],[.8,-3.6,-2.14,2.02,-2.5,-2.02]],WHITE)
  trim=boxes(g,[[-.9,-3.6,-2.2,-.72,-2.45,-2.14],[.72,-3.6,-2.2,.9,-2.45,-2.14],[-.9,-2.63,-2.2,.9,-2.45,-2.14],[-.065,-2.45,-2.19,.065,.45,-2.14],[-2.14,.44,-2.2,2.14,.6,-2.14]],WHITE)
 return cloth,trim,kind

def crowns(name,g):
 if name.startswith('baseball'):
  main='cwe_capmain_ffffffff_0';accent='cwe_capblue_00a4ffff_0'
  m=boxes(g,[[-3.67,-6.48,-3.22,3.67,-6.23,3.22],[-3.67,-6.23,-3.22,-3.43,-3.25,3.22],[3.43,-6.23,-3.22,3.67,-3.25,3.22],[-3.43,-6.23,2.98,3.43,-3.25,3.22],[-3.43,-6.23,-3.22,3.43,-3.25,-2.98]],main,(.5,.5))
  m.faces.extend(boxes(g,[[-3.65,-3.42,-5.35,3.65,-3.20,-3.1],[-.38,-6.67,-.38,.38,-6.48,.38],[-.75,-5.4,-3.29,.75,-4.35,-3.22]],accent,(.5,.5)).faces)
 else:
  main='cwe_press_hat_2d1704ff_0';paper='cwe_press_hat_fcfcfcff_0'
  # The crown surrounds the fixed skull instead of sitting inside its front
  # and sides. A smaller upper tier keeps the fedora's stepped silhouette;
  # that tier and its shoulder stay entirely above the skull's top plane.
  m=boxes(g,[[-3.58,-7.3,-3.12,3.58,-7.08,3.12],[-3.58,-7.08,-3.12,-3.38,-6.12,3.12],[3.38,-7.08,-3.12,3.58,-6.12,3.12],[-3.38,-7.08,2.92,3.38,-6.12,3.12],[-3.38,-7.08,-3.12,3.38,-6.12,-2.92],[-3.7,-6.30,-3.25,3.7,-6.12,3.25],[-3.7,-6.12,-3.25,-3.52,-3.45,3.25],[3.52,-6.12,-3.25,3.7,-3.45,3.25],[-3.52,-6.12,3.07,3.52,-3.45,3.25],[-3.52,-6.12,-3.25,3.52,-3.45,-3.07],[-3.8,-3.47,-3.65,3.8,-3.23,3.65]],main)
  m.faces.extend(boxes(g,[[.9,-6.45,-3.34,2.1,-4.5,-3.25]],paper).faces)
 return m

def bow(name,g):
 key='cwe_bow_ff0000ff_0';middle='cwe_bowmiddle_ff0000ff_0'
 m=g.Mesh()
 # Native bow artwork is mapped onto broad rectangular wings; the neck strap
 # remains this port's explicit LEGS-slot adaptation.
 for b in [[-1.7,2.5,-2.7,-1.12,3.8,-2.22],[-1.12,2.7,-2.7,-.65,3.6,-2.22],[-.65,2.87,-2.7,-.34,3.43,-2.22],[1.12,2.5,-2.7,1.7,3.8,-2.22],[.65,2.7,-2.7,1.12,3.6,-2.22],[.34,2.87,-2.7,.65,3.43,-2.22]]:m.box(b[:3],b[3:],key)
 m.box([-.4,2.7,-2.9],[.4,3.6,-2.15],middle)
 # Both original bow sprites carry transparent padding in their upper half.
 # Use the actual folded-cloth strip, rather than cutting the cuboid in half.
 m.faces=[(ps,[(u,.515625+v*.4765625) for u,v in uv],k) for ps,uv,k in m.faces]
 m.faces.extend(boxes(g,[[-3,2.85,-2.2,3,3.25,-2.05]],key,(.5,.7)).faces)
 return m

def entity(g,name,cloth,tiles,kind=None,trim=None,extra=None):
 data={'texture_width':1024,'texture_height':1024,'bones':[{'name':'accessory','pivot':[0,0,0],'rotation':[0,0,0],'meshes':[g.wearable_mesh(cloth,tiles,1024)]}],'authored_metadata':{'source':'Supplied CWE 9.6.2.2 design, colors and texture regions','release':'0.25.79','fit':'block translation in canonical joint space'}}
 if hasattr(cloth,'named_parts'):
  data['bones'][0]['meshes']=[]
  for part,mesh in cloth.named_parts.items():
   converted=g.wearable_mesh(mesh,tiles,1024);converted['name']=part;data['bones'][0]['meshes'].append(converted)
 if kind:data['accessory_fit']={'kind':kind}
 if trim and trim.faces:
  data['bones'].append({'name':'accessory_trim','parent':'accessory','pivot':[0,0,0],'rotation':[0,0,0],'meshes':[g.wearable_mesh(trim,tiles,1024)]});data['accessory_untinted_bones']=['accessory_trim']
 if name=='hoodie_hood':data['authored_metadata']['clear_face_viewport']=[-3.2,-3.4,3.2,.15]
 if extra:data.setdefault('accessory_fit',{}).update(extra)
 return data

def combined(g,data,tiles,include_temples=True):
 """Recover the same rest mesh for held items, preserving all source UV tiles."""
 m=g.Mesh();bones={b['name']:b for b in data['bones']}
 def pivot(b):
  p=np.array(b['pivot'],float)
  if b.get('parent'):p+=pivot(bones[b['parent']])
  return p
 for b in data['bones']:
  if not include_temples and b['name'].startswith(('temple_','hinge_strip_')):continue
  base=pivot(b)
  for me in b.get('meshes',[]):
   for f in me['faces']:
    uv=f['uv'];u=sum(x[0] for x in uv)/len(uv);v=sum(x[1] for x in uv)/len(uv)
    key=next(k for k,(x,y,w,h) in tiles.items() if x-1e-7<=u<=x+w+1e-7 and y-1e-7<=v<=y+h+1e-7);x,y,w,h=tiles[key]
    ps=[(np.array(me['vertices'][i])+base).tolist() for i in f['v']]
    m.face([[p[0],-p[1],-p[2]] for p in ps],key,[((u-x)/w,(v-y)/h) for u,v in uv])
 return m

def icon_front(g,mesh,mats):
 # Cancel only the software icon camera; held and dropped geometry stays whole.
 # A broad front view makes closed frames readable at the actual 16px GUI size.
 import math
 c,s=math.cos(.46),math.sin(.46);C,S=math.cos(.22),math.sin(.22)
 def inverse(p):
  x,y,z=p;Y=y*C+z*S;Z=-y*S+z*C
  return [x*c+Z*s,Y,-x*s+Z*c]
 posed=g.mapped(mesh,inverse)
 return g.render(posed,mats)

def make_clothing079(g):
 """Write only registered CWE clothing/item assets and their shared atlas."""
 from accessory079_non_spectacle import headphones,pacifier
 meshes={};materials={}
 for name,key in g.MAP.items():
  _,mat=g.clothing_source(name,g.ACCESS[key],True,node=1 if 'hoodie' in name else None,local='hoodie' in name);materials.update(mat)
 for node in [3,10,16]:materials.update(g.adapted(g.ACCESS['7_hoodie'],lambda p:(p[0],-p[1],-p[2]),neutral=True,node=node,local=True)[1])
 atlas=Image.new('RGBA',(1024,1024));atlas.paste(Image.open(g.D/'previous-palette.png'),(0,0));tiles={}
 for i,(key,im) in enumerate(sorted(materials.items())):
  x=i%8*128;y=(i//8+1)*128;atlas.paste(im.resize((128,128),Image.Resampling.NEAREST),(x,y));tiles[key]=(x,y,128,128)
 atlas.save(g.A/'textures/entity/chao/cwe_source02516.png')
 for name in g.MAP:
  if 'glasses' in name or 'aviators' in name:data=stepped(name,tiles,g)
  elif name in ['baseball_cap_accessory','press_hat_accessory']:data=entity(g,name,crowns(name,g),tiles,'head')
  elif 'bowtie' in name:data=entity(g,name,bow(name,g),tiles)
  elif name=='headphones_accessory':
   m,profile=headphones(g);data=entity(g,name,m,tiles,'head',extra=profile)
  elif name=='pacifier_accessory':
   m,profile=pacifier(g);data=entity(g,name,m,tiles,extra=profile)
  else:
   m,trim,kind=garment(name,g);data=entity(g,name,m,tiles,kind,trim)
  meshes[name]=data
 for name in ['hoodie_sleeve_left','hoodie_sleeve_right','hoodie_hood','hoodie_hood_folded']:
  m,trim,kind=garment(name,g);meshes[name]=entity(g,name,m,tiles,kind,trim)
 for name,data in meshes.items():g.write(g.A/'models/entity/cwe_accessories'/(name+'.json'),data)
 for name in g.MAP:
  data=meshes[name];m=combined(g,data,tiles);mat={key:materials[key] for _,_,key in m.faces}
  if 'hoodie' in name:
   tint=(70,120,202,255) if name.startswith('blue') else (238,152,183,255)
   # Item trim remains white too: a dedicated material key shares the original
   # source texels while the dye only affects the cloth's faces.
   cloth_key=WHITE+'_cloth';trim_key=WHITE+'_trim'
   mat={cloth_key:ImageChops.multiply(materials[WHITE],Image.new('RGBA',materials[WHITE].size,tint)),trim_key:materials[WHITE]}
   m=g.Mesh()
   # Display the complete outfit as one real held garment, including its hood
   # and sleeves. The same block panels are used by the articulated wearer.
   for part,offset in [(name,[0,0,0]),('hoodie_hood',[0,-3.6,0]),('hoodie_sleeve_left',[2,-3,0]),('hoodie_sleeve_right',[-2,-3,0])]:
    cloth,trim,_=garment(part,g)
    for source,key in [(cloth,cloth_key),(trim,trim_key)]:
     for ps,uv,_ in source.faces:
      m.face([[p[0]+offset[0],-p[1]-offset[1],-p[2]-offset[2]] for p in ps],key,uv)
  _,_,_=g.item(name,m,mat)
  if 'glasses' in name or 'aviators' in name:
   front=combined(g,data,tiles,False);icon_front(g,front,mat).save(g.A/'textures/item'/(name+'.png'))
 return list(g.MAP)

