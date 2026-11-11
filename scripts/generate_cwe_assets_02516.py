#!/usr/bin/env python3
"""Final CWE source asset pass: native items, toys, fruit pods and fitted clothing."""
from pathlib import Path
import json,sys,copy,math,re
import numpy as np
from PIL import Image,ImageChops
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'art'))
from fruit_forms02512 import Mesh,normal,render
from generate_sa2_assets_02514 import wrap_atlas
from generate_sa2_assets_02515 import fit,rotate,entity_mesh
from generate_fruit_models_02512 import DISPLAY
D=ROOT/'art/cwe02516';A=ROOT/'src/main/resources/assets/chao_garden'
ACCESS=json.loads((D/'accessories.json').read_text());OBJECTS=json.loads((D/'objects.json').read_text())
MAP=dict(zip(['baseball_cap_accessory','bowtie_accessory','round_glasses_accessory','aviators_accessory','headphones_accessory','pacifier_accessory','press_hat_accessory','blue_hoodie_accessory','cheese_bowtie_accessory'],ACCESS));MAP['pink_hoodie_accessory']='7_hoodie'
MATERIALS=set()
def write(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n' if not isinstance(data,str) else data)
def adapted(info,orient=None,neutral=False,node=None,local=False):
 faces=[f for f in info['faces'] if node is None or f['node']==node];m=Mesh();mats={};ranges={};colors={};keys={}
 for f in faces:
  color=[255]*4 if neutral else f['color'];k='cwe_'+re.sub('[^a-z0-9_]','_',str(f['texture']))+'_'+''.join(f'{x:02x}' for x in color)+'_'+str(f['wrap_flags']);keys[id(f)]=k
  if k not in ranges:ranges[k]=[[],[]];colors[k]=color
  for uv in f['uv']:
   for i,v in enumerate(uv):ranges[k][i].append(v)
 for k,coords in ranges.items():
  f=next(f for f in faces if keys[id(f)]==k);im=Image.open(D/'textures'/(f['texture']+'.png')).convert('RGBA') if f['texture'] else Image.new('RGBA',(16,16),'white')
  im=ImageChops.multiply(im,Image.new('RGBA',im.size,tuple(colors[k])));im.putalpha(im.getchannel('A').point(lambda a:255 if a>=128 else 0));im,lo,cnt=wrap_atlas(im,[(min(c),max(c)) for c in coords],f['wrap_flags']);mats[k]=im;ranges[k]=(lo,cnt)
 for f in faces:
  k=keys[id(f)];lo,cnt=ranges[k];pts=f['local_points'] if local and f['local_points'] else f['points'];pts=[orient(p) if orient else p for p in pts];uv=[((u-lo[0])/cnt[0],(v-lo[1])/cnt[1]) for u,v in f['uv']];m.face(pts,k,uv)
  if f['double_sided']:m.face(list(reversed(pts)),k,list(reversed(uv)))
 return m,mats

def bindings(n,mats,age=None):
 out={}
 for k,im in mats.items():
  if age is not None and age<3:
   green=Image.new('RGBA',im.size,(86,134,59,255));green.putalpha(im.getchannel('A'));im=Image.blend(green,im,(.16,.45,.72)[age])
  path='block/cwe02516/'+n+'/'+k+(f'_{age}' if age is not None else '');p=A/'textures'/(path+'.png');p.parent.mkdir(parents=True,exist_ok=True);im.save(p);out[k]='chao_garden:'+path;MATERIALS.add(k)
 out['particle']=next(iter(out.values()));return out

def native(mesh,n,tex,display=None):
 lines=['# CWE source adaptation 0.25.16; coordinates in blocks.','mtllib chao_garden:models/cwe02516/materials.mtl'];offset=1
 margins={}
 for mat,path in tex.items():
  if mat=='particle':continue
  with Image.open(A/'textures'/(path.split(':',1)[1]+'.png')) as im:margins[mat]=(.5/im.width,.5/im.height)
 for pts,uv,mat in mesh.faces:
  nn=normal(pts)
  if sum(v*v for v in nn)<.9:continue
  # Constant UVs on sprite boundaries otherwise sample unrelated atlas neighbors.
  # Keep each source material half a texel inside its own sprite, in every pose.
  du,dv=margins[mat];uv=[(min(1-du,max(du,u)),min(1-dv,max(dv,v))) for u,v in uv]
  lines.append('usemtl '+mat);lines+=['v '+' '.join(f'{v/16:.7f}' for v in p) for p in pts];lines += [f'vt {u:.7f} {1-v:.7f}' for u,v in uv];lines+=['vn '+' '.join(f'{v:.7f}' for v in nn)]*len(pts);lines.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(offset,offset+len(pts))));offset+=len(pts)
 write(A/'models/cwe02516'/(n+'.obj'),'\n'.join(lines)+'\n');d={'loader':'neoforge:obj','model':'chao_garden:models/cwe02516/'+n+'.obj','automatic_culling':False,'shade_quads':True,'flip_v':True,'textures':tex,'render_type':'minecraft:cutout'}
 if display:d['display']=display
 return d

def item(n,m,mats):
 m=fit(unique_surfaces(m),(1,1,1,15,15,15));tex=bindings(n,mats);write(A/'models/item/cwe02516'/(n+'.json'),native(m,n,tex,DISPLAY));write(A/'models/item'/(n+'.json'),{'loader':'neoforge:separate_transforms','gui_light':'front','textures':{'layer0':'chao_garden:item/'+n,'particle':tex['particle']},'base':{'parent':'chao_garden:item/cwe02516/'+n},'perspectives':{'gui':{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/'+n},'gui_light':'front'}}});render(m,mats).save(A/'textures/item'/(n+'.png'));return m,mats,tex

def bounds(data):
 ps=[v for b in data['bones'] for m in b.get('meshes',[]) for v in m['vertices']];return [min(p[a] for p in ps) for a in range(3)]+[max(p[a] for p in ps) for a in range(3)]
def old(n):return json.loads((D/'previous-models'/(n+'.json')).read_text())
def mapped(mesh,fn):
 out=Mesh()
 for pts,uv,k in mesh.faces:out.face([fn(p) for p in pts],k,uv)
 return out
def unique_surfaces(mesh):
 # EntityCutoutNoCull already draws both sides. Duplicated reverse triangles
 # otherwise z-fight with the visible surface and invert its lighting.
 out=Mesh();seen=set()
 for pts,uv,k in mesh.faces:
  key=(k,tuple(sorted(tuple(p)+tuple(t) for p,t in zip(pts,uv))))
  if key not in seen:out.face(pts,k,uv);seen.add(key)
 return out

def wearable_mesh(mesh,tiles,atlas_size):
 return entity_mesh(unique_surfaces(mesh),tiles,atlas_size)

def square_shell(mesh,target,axes=(0,2)):
 """Wrap cloth around boxy skin, retaining source UVs and the face opening.

 Split faces at box-corner planes before radial projection. Simply expanding
 vertices leaves the triangle interiors cutting through the skin at corners.
 """
 mesh=fit(unique_surfaces(mesh),target,True)
 center=np.array([(target[i]+target[i+3])/2 for i in range(3)])
 half=np.array([(target[i+3]-target[i])/2 for i in range(3)])
 out=Mesh()
 def clip(poly,plane):
  result=[]
  for i,(p,uv) in enumerate(poly):
   q,t=poly[i-1];a=float(np.dot(q,plane));b=float(np.dot(p,plane))
   if (a>=-1e-9)!=(b>=-1e-9):
    f=a/(a-b);result.append((q+(p-q)*f,t+(uv-t)*f))
   if b>=-1e-9:result.append((p,uv))
  return result
 for pts,uv,k in mesh.faces:
  original=[((np.asarray(p)-center)/half,np.asarray(t)) for p,t in zip(pts,uv)]
  for axis in axes:
   for sign in (-1,1):
    poly=original
    for other in axes:
     if axis==other:continue
     for side in (-1,1):
      plane=np.zeros(3);plane[axis]=sign;plane[other]=side;poly=clip(poly,plane)
      if len(poly)<3:break
     if len(poly)<3:break
    if len(poly)<3:continue
    projected=[]
    for p,t in poly:
     v=p.copy();radius=abs(v[axis])
     if radius<1e-8:
      for a in axes:v[a]=0
      v[axis]=sign
     else:
      factor=(1+.002*math.sqrt(sum(v[a]**2 for a in axes)))/radius
      for a in axes:v[a]*=factor
     projected.append((center+v*half).tolist())
    for j in range(1,len(projected)-1):
     ids=(0,j,j+1);triangle=[projected[i] for i in ids]
     if np.linalg.norm(np.cross(np.array(triangle[1])-triangle[0],np.array(triangle[2])-triangle[0]))>1e-9:
      out.face(triangle,k,[poly[i][1].tolist() for i in ids])
 return fit(out,target,True)

def fitted_headphones(info):
 # Preserve the padded source cups and their textures. The supplied rest-pose
 # band lies horizontally, so adapt its material onto a fitted connecting band.
 cups,mat=adapted(info,lambda p:(p[0],-p[1],-p[2]),node=0)
 _,band_materials=adapted(info,node=1);mat.update(band_materials);key=next(iter(band_materials))
 left=Mesh();right=Mesh()
 for pts,uv,k in cups.faces:(left if sum(p[0] for p in pts)<0 else right).face(pts,k,uv)
 band=Mesh();band.box((-3.65,-3.4,-.65),(3.65,-3.1,.65),key)
 band.box((-3.65,-3.1,-.65),(-3.25,.1,.65),key);band.box((3.25,-3.1,-.65),(3.65,.1,.65),key)
 band.faces.extend(fit(left,[-4.2,-1.25,-1.3,-3.45,1.1,1.3],True).faces)
 band.faces.extend(fit(right,[3.45,-1.25,-1.3,4.2,1.1,1.3],True).faces)
 return band,mat

def clothing_source(name,info,entity=False,node=None,local=False):
 if name=='headphones_accessory':
  m,mat=fitted_headphones(info)
  return (m if entity else mapped(m,lambda p:(p[0],-p[1],-p[2]))),mat
 m,mat=adapted(info,neutral='hoodie' in name,node=node,local=local)
 if name=='baseball_cap_accessory':m=rotate(m,0,90)
 if name=='bowtie_accessory':m=rotate(m,2,90)
 if entity:m=mapped(m,lambda p:(p[0],-p[1],-p[2]))
 return m,mat

def make_clothing():
 meshes={};materials={};items=[]
 for name,key in MAP.items():
  info=ACCESS[key];m,mat=clothing_source(name,info)
  tint=(70,120,202,255) if name.startswith('blue_hoodie') else (238,152,183,255)
  imat={k:ImageChops.multiply(v,Image.new('RGBA',v.size,tint)) for k,v in mat.items()} if 'hoodie' in name else mat
  item(name,m,imat);items.append(name)
  m,mat=clothing_source(name,info,True,node=1 if 'hoodie' in name else None,local='hoodie' in name)
  meshes[name]=(m,mat);materials.update(mat)
 for side,node in [('left',3),('right',10)]:
  name='hoodie_sleeve_'+side;m,mat=adapted(ACCESS['7_hoodie'],lambda p:(p[0],-p[1],-p[2]),neutral=True,node=node,local=True);meshes[name]=(m,mat);materials.update(mat)
 # Shared source hood is a separate articulated head attachment.
 m,mat=adapted(ACCESS['7_hoodie'],lambda p:(p[0],-p[1],-p[2]),neutral=True,node=16,local=True);meshes['hoodie_hood']=(m,mat);materials.update(mat)
 atlas=Image.new('RGBA',(1024,1024));atlas.paste(Image.open(D/'previous-palette.png'),(0,0));tiles={}
 for i,(k,im) in enumerate(sorted(materials.items())):
  x=i%8*128;y=(i//8+1)*128;assert y+128<=1024;atlas.paste(im.resize((128,128),Image.Resampling.NEAREST),(x,y));tiles[k]=(x,y,128,128)
 atlas.save(A/'textures/entity/chao/cwe_source02516.png')
 for name,(m,mat) in meshes.items():
  data=old(name) if name!='hoodie_hood' else {'bones':[{'name':'accessory','pivot':[0,0,0],'rotation':[0,0,0]}]};data.update(texture_width=1024,texture_height=1024)
  target=bounds(old(name)) if name!='hoodie_hood' else [-3.5,-6,-3,3.5,0,3]
  if name=='baseball_cap_accessory':target[1]=-6
  if name=='press_hat_accessory':target[1]=-6.5
  if 'bowtie' in name:
   # Keep the fitted neck strap; only the visible bow is the source shape.
   strap=copy.deepcopy(old(name)['bones'][0]['meshes'][3]);source=wearable_mesh(fit(m,[-1.7,2.5,-2.9,1.7,3.8,-2.15],True),tiles,1024);data['bones'][0]['meshes']=[source,strap]
  elif 'glasses' in name or 'aviators' in name:
   # Source eye rings retain their holes. The existing independent hinges keep
   # temple contact correct across narrow character and evolved skulls.
   front=Mesh();left=Mesh();right=Mesh();bridge=Mesh()
   for pts,uv,k in m.faces:
    if min(p[2] for p in pts)>-1.14:continue
    (left if max(p[0] for p in pts)<-.12 else right if min(p[0] for p in pts)>.12 else bridge).face(pts,k,uv)
   assert left.faces and right.faces
   data['bones'][0]['meshes']=[wearable_mesh(fit(left,[-2.6,-.8,-3.3,-.6,1.05,-3.03],True),tiles,1024),wearable_mesh(fit(right,[.5,-.8,-3.3,2.5,1.05,-3.03],True),tiles,1024)]
   if bridge.faces:data['bones'][0]['meshes'].append(wearable_mesh(fit(bridge,[-.65,-.3,-3.25,.55,-.1,-3.05],True),tiles,1024))
  elif name=='headphones_accessory':data['bones'][0]['meshes']=[wearable_mesh(m,tiles,1024)]
  elif 'hoodie' in name:
   if name=='hoodie_hood':shell=square_shell(m,target,(0,1,2))
   elif 'sleeve' in name:shell=square_shell(m,target,(1,2))
   else:shell=square_shell(m,[-2,-.7,-1.6,2,3.2,1.6])
   data['bones'][0]['meshes']=[wearable_mesh(shell,tiles,1024)]
  else:data['bones'][0]['meshes']=[wearable_mesh(fit(m,target,True),tiles,1024)]
  data['authored_metadata']={'source':'Supplied CWE 9.6.2.2','release':'0.25.16','fit':'source surfaces adapted to existing semantic anchors'};write(A/'models/entity/cwe_accessories'/(name+'.json'),data)
 return items

def make_objects():
 rows=[]
 for name,info in OBJECTS.items():
  m,mat=adapted(info,local=name=='chao_accordion')
  if name=='chao_guitar':m=rotate(rotate(m,2,90),1,180)
  base,mat,tex=item(name,m,mat);rows.append(name)
  if name in ('chao_guitar','chao_accordion'):
   # Guitar lies on the lawn; accordion bellows compress around their midpoint.
   placed=rotate(m,0,90) if name=='chao_guitar' else m
   placed=fit(placed,[3,0,3,13,8,13])
   for frame in [-1,0,1,2,3]:
    pose=placed
    if name=='chao_accordion' and frame>=0:
     pose=Mesh();factor=1-.05*(1,2,1,0)[frame]
     for pts,uv,k in placed.faces:pose.face([((p[0]-8)*factor+8,p[1],p[2]) for p in pts],k,uv)
    suffix='idle' if frame<0 else 'play_'+str(frame);write(A/'models/block'/(name+'_'+suffix+'.json'),native(pose,name+'_'+suffix,tex))
  elif name.endswith('_fruit'):
   for age,scale in enumerate((.45,.65,.82,1)):
    pod=base.transformed(scale,center=(8,10,8),offset=(0,-1,0));stem='cwe_pod_stem';pod.box((7.5,pod.bounds()[4]-.5,7.5),(8.5,20,8.5),stem);mats={**mat,stem:Image.new('RGBA',(16,16),(101,79,38,255))};t=bindings(name,mats,age);pn=name+'_pod_'+str(age);write(A/'models/block'/(pn+'.json'),native(pod,pn,t));Image.open(A/'textures'/(t['particle'].split(':')[1]+'.png')).resize((32,32),Image.Resampling.NEAREST).save(A/'textures/block'/(pn+'.png'))
 return rows

def main():
 items=make_clothing()+make_objects();write(A/'models/cwe02516/materials.mtl','\n'.join(f'newmtl {k}\nKd 1.0 1.0 1.0\nmap_Kd #{k}\n' for k in sorted(MATERIALS)))
 from lobby_material_manifest0255 import render_textures
 p=ROOT/'art/lobby_garden_manifest_025.json';data=json.loads(p.read_text());data['render_textures']=render_textures(ROOT,data['block_atlas_textures']);write(p,data)
 write(D/'generated.json',{'release':'0.25.16','items':items,'wearables':list(MAP),'source_objects':list(OBJECTS),'source_clothing_parts':13});print('CWE source assets:',len(items),'items,13 articulated wearable parts,2 toys and5fruit crops')
if __name__=='__main__':main()
