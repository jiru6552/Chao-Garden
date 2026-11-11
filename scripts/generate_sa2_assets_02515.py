#!/usr/bin/env python3
"""Requested material rollbacks, source school art, toys and fitted wearables."""
from pathlib import Path
import json,math,sys,copy,shutil
from collections import Counter
from PIL import Image,ImageOps,ImageFilter
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'art'))
from fruit_forms02512 import Mesh,normal,render
from generate_fruit_models_02512 import DISPLAY
from generate_sa2_assets_02514 import wrap_atlas
D=ROOT/'art/sa2_02515';A=ROOT/'src/main/resources/assets/chao_garden'
TOYS=json.loads((D/'toys.json').read_text());HATS=json.loads((D/'hats.json').read_text())
MATERIALS=set()
def write(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n' if not isinstance(value,str) else value)
def source(arc,n):
 im=Image.open(D/'textures'/arc/(n+'.png')).convert('RGBA');im.putalpha(im.getchannel('A').point(lambda a:255 if a>=128 else 0));return im
def adapt(info,orient=None):
 mesh=Mesh();coords={};mats={};ranges={}
 for f in info['faces']:
  k='sa2_'+f['texture']+'_'+str(f['wrap_flags']);coords.setdefault(k,[[],[]])
  for uv in f['uv']:
   for a in (0,1):coords[k][a].append(uv[a])
 for k,uv in coords.items():
  n,flags=k[4:].rsplit('_',1);im=source(info['source_archive'],n);im,lo,count=wrap_atlas(im,[(min(u),max(u)) for u in uv],int(flags));mats[k]=im;ranges[k]=(lo,count)
 for f in info['faces']:
  k='sa2_'+f['texture']+'_'+str(f['wrap_flags']);lo,cnt=ranges[k];pts=[orient(p) if orient else p for p in f['points']];uv=[((u-lo[0])/cnt[0],(v-lo[1])/cnt[1]) for u,v in f['uv']]
  mesh.face(pts,k,uv)
  if f.get('double_sided'):mesh.face(list(reversed(pts)),k,list(reversed(uv)))
 return mesh,mats

def fit(mesh,box,stretch=False,reference_bounds=None):
 # Animated assemblies must retain their rest-pose transform, not fit each pose.
 b=mesh.bounds() if reference_bounds is None else reference_bounds;sizes=[max(.001,b[a+3]-b[a]) for a in range(3)];f=[(box[a+3]-box[a])/sizes[a] for a in range(3)]
 if not stretch:f=[min(f)]*3
 m=Mesh()
 for pts,uv,mat in mesh.faces:m.face([((p[0]-(b[0]+b[3])/2)*f[0]+(box[0]+box[3])/2,(p[1]-b[1])*f[1]+box[1],(p[2]-(b[2]+b[5])/2)*f[2]+(box[2]+box[5])/2) for p in pts],mat,uv)
 return m

def rotate(mesh,axis,degrees,center=None):
 c,s=math.cos(math.radians(degrees)),math.sin(math.radians(degrees));b=mesh.bounds();center=center or [(b[a]+b[a+3])/2 for a in range(3)];i,j=[a for a in range(3) if a!=axis];out=Mesh()
 for pts,uv,mat in mesh.faces:
  ps=[]
  for p in pts:
   v=list(p);a,b=p[i]-center[i],p[j]-center[j];v[i]=center[i]+a*c-b*s;v[j]=center[j]+a*s+b*c;ps.append(v)
  out.face(ps,mat,uv)
 return out

def bindings(n,mats):
 out={}
 for k,im in mats.items():
  p='block/sa2_02515/'+n+'/'+k;dest=A/'textures'/(p+'.png');dest.parent.mkdir(parents=True,exist_ok=True);im.save(dest);out[k]='chao_garden:'+p;MATERIALS.add(k)
 out['particle']=out[next(iter(mats))];return out

def native(mesh,n,tex,display=None):
 lines=['# SA2 source adaptation 0.25.15. Coordinates in blocks.','mtllib chao_garden:models/sa2_02515/materials.mtl'];offset=1
 for pts,uv,mat in mesh.faces:
  nn=normal(pts)
  if sum(v*v for v in nn)<.9:continue
  lines.append('usemtl '+mat)
  lines+=['v '+' '.join(f'{v/16:.7f}' for v in p) for p in pts]
  # Keep exact-edge UVs inside their own atlas sprite (coffin lid edge otherwise samples its neighbor).
  with Image.open(A/'textures'/(tex[mat].split(':')[1]+'.png')) as texture:
   eu,ev=.5/texture.width,.5/texture.height
  lines += [f'vt {max(eu,min(1-eu,u)):.7f} {1-max(ev,min(1-ev,v)):.7f}' for u,v in uv]
  lines += ['vn '+' '.join(f'{v:.7f}' for v in nn)]*len(pts)
  lines.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(offset,offset+len(pts))));offset+=len(pts)
 write(A/'models/sa2_02515'/(n+'.obj'),'\n'.join(lines)+'\n')
 d={'loader':'neoforge:obj','model':'chao_garden:models/sa2_02515/'+n+'.obj','automatic_culling':False,'shade_quads':True,'flip_v':True,'textures':tex,'render_type':'minecraft:cutout'}
 if display:d['display']=display
 return d

def icon(mesh,n,mats):
 return render(rotate(mesh,1,180) if n in ("chao_tv","chao_piano","chao_organ","chao_radio") else mesh,mats)

def item(mesh,n,mats,tex):
 write(A/'models/item/sa2_02515'/(n+'.json'),native(mesh,n,tex,DISPLAY))
 write(A/'models/item'/(n+'.json'),{'loader':'neoforge:separate_transforms','gui_light':'front','textures':{'layer0':'chao_garden:item/'+n,'particle':tex['particle']},'base':{'parent':'chao_garden:item/sa2_02515/'+n},'perspectives':{'gui':{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/'+n},'gui_light':'front'}}})
 icon(mesh,n,mats).save(A/'textures/item'/(n+'.png'))

LIMITS={'chao_trumpet':(1,0,4,15,7,12),'chao_drum':(3,0,3,13,9,13),'chao_radio':(2,0,4,14,14,12),'chao_tv':(2,0,3,14,16,13),'chao_rocking_horse':(1,0,2,15,14,14),'chao_piano':(1,0,1,15,15,15),'chao_organ':(1,0,1,15,15,15),'chao_coffin':(2,0,1,14,7,15),'chao_lifesaver':(1,0,1,15,3,15),'chao_rattle':(3,0,3,13,4,13),'chao_maracas':(3,0,3,13,4,13)}
def toy_mesh(n):
 m,mats=adapt(TOYS[n])
 if n in ('chao_drum','chao_cymbals'):m=rotate(m,2,90)
 if n=='chao_trumpet':m=rotate(m,1,75)
 if n in ('chao_rattle','chao_flute','chao_bell','chao_bubble_wand'):m=rotate(m,0,75)
 if n in ('chao_maracas','chao_cymbals'):
  # A usable pair, not one instrument masquerading as the pair in the catalog.
  one=fit(m,(3,.2,3,13,3.8,6.8));two=one.transformed(offset=(0,0,6));one.faces.extend(two.faces);m=one
 return m,mats

def toy_item_mesh(n):
 m,mats=toy_mesh(n)
 if n in ('chao_rattle','chao_flute','chao_bell','chao_bubble_wand'):m=rotate(m,0,-75)
 return m,mats

# ChaoMain.prs lid node 0x30a8a8: local translation plus root Z=0.05,
# and source Y rotation 18887/65536 turns. Its LOCAL Z is the long-edge hinge.
# Rotating about world X misses that slanted edge and pulls the lid off its hull.
COFFIN_LID_NODE='0x30a8a8'
COFFIN_OPEN_ANGLES=(0,25,50,75)
COFFIN_HINGE=(0.5462960004806519,1.4999990463256836,2.0303240306675434)
COFFIN_SOURCE_Y=18887*math.tau/65536
COFFIN_AXIS=(math.sin(COFFIN_SOURCE_Y),0,math.cos(COFFIN_SOURCE_Y))

def coffin_source_pose(frame):
 info=copy.deepcopy(TOYS['chao_coffin'])
 angle=math.radians(0 if frame<0 else COFFIN_OPEN_ANGLES[frame]);c,s=math.cos(angle),math.sin(angle)
 ax,ay,az=COFFIN_AXIS;cx,cy,cz=COFFIN_HINGE
 def hinged(p):
  x,y,z=p[0]-cx,p[1]-cy,p[2]-cz;dot=ax*x+ay*y+az*z
  return (cx+x*c+(ay*z-az*y)*s+ax*dot*(1-c),
          cy+y*c+(az*x-ax*z)*s+ay*dot*(1-c),
          cz+z*c+(ax*y-ay*x)*s+az*dot*(1-c))
 if angle:
  for face in info['faces']:
   if face['node']==COFFIN_LID_NODE:face['points']=[hinged(p) for p in face['points']]
 return info

def coffin_pose(frame):
 rest=adapt(TOYS['chao_coffin'])[0]
 limits=list(LIMITS['chao_coffin']);limits[1]=.08;limits[4]-=.08
 return fit(adapt(coffin_source_pose(frame))[0],limits,reference_bounds=rest.bounds())

def coffin_poses_only():
 # Reuse existing bindings: no unrelated toys, textures, icons or manifests change.
 tex=json.loads((A/'models/block/chao_coffin_idle.json').read_text())['textures']
 for frame in (-1,0,1,2,3):
  stage='idle' if frame<0 else 'play_'+str(frame)
  n='chao_coffin_'+stage
  write(A/'models/block'/(n+'.json'),native(coffin_pose(frame),n,tex))
 print('Regenerated only the five coffin poses (fixed source hinge and rest-pose fit).')

# Mounted toys share one fitted rest geometry with the runtime rider transform.
# Horse source forward is +Z; X rotation is fore/aft, never sideways Z rotation.
HORSE_ROCK_ANGLES=(0,6,0,-6)
HORSE_ROCK_PIVOT=(8,.08,8)  # model units: (.5,.005,.5) blocks
LIFESAVER_BOB_BLOCKS=(0,.025,0,-.025)
LIFESAVER_FLOAT_BLOCKS=.80
MOUNTED_TOYS=('chao_rocking_horse','chao_lifesaver')

def mounted_toy_rest(n):
 if n not in MOUNTED_TOYS:raise ValueError(n)
 lim=list(LIMITS[n]);lim[1]=.08;lim[4]-=.08
 return fit(toy_mesh(n)[0],lim)

def horse_common_lift(rest):
 # One lift for the entire animation, not a separate floor refit each frame.
 return max(0,-min(rotate(rest,0,a,HORSE_ROCK_PIVOT).bounds()[1]
                   for a in HORSE_ROCK_ANGLES))

def mounted_toy_pose(n,frame,waterlogged=False):
 rest=mounted_toy_rest(n)
 if n=='chao_rocking_horse':
  pose=rotate(rest,0,0 if frame<0 else HORSE_ROCK_ANGLES[frame],HORSE_ROCK_PIVOT)
  return pose.transformed(offset=(0,horse_common_lift(rest),0))
 return rest.transformed(offset=(0,16*((LIFESAVER_FLOAT_BLOCKS if waterlogged else 0)
                                      +(0 if frame<0 else LIFESAVER_BOB_BLOCKS[frame])),0))

def lifesaver_float_assets():
 n='chao_lifesaver'
 tex=json.loads((A/'models/block'/(n+'_idle.json')).read_text())['textures']
 for frame in (-1,0,1,2,3):
  stage='idle' if frame<0 else 'play_'+str(frame);name=n+'_float_'+stage
  write(A/'models/block'/(name+'.json'),native(mounted_toy_pose(n,frame,True),name,tex))
 variants={}
 for facing,angle in (('north',0),('east',90),('south',180),('west',270)):
  for playing in (False,True):
   for frame in range(4):
    for waterlogged in (False,True):
     stage='play_'+str(frame) if playing else 'idle'
     model=n+('_float_' if waterlogged else '_')+stage
     key=f'facing={facing},frame={frame},playing={str(playing).lower()},waterlogged={str(waterlogged).lower()}'
     variants[key]={'model':'chao_garden:block/'+model,'y':angle}
 write(A/'blockstates/chao_lifesaver.json',{'variants':variants})

def mounted_toy_poses_only():
 # Reuse bindings and leave items, icons, textures and unrelated assets untouched.
 for n in MOUNTED_TOYS:
  tex=json.loads((A/'models/block'/(n+'_idle.json')).read_text())['textures']
  for frame in (-1,0,1,2,3):
   stage='idle' if frame<0 else 'play_'+str(frame);name=n+'_'+stage
   write(A/'models/block'/(name+'.json'),native(mounted_toy_pose(n,frame),name,tex))
 lifesaver_float_assets()
 print('Regenerated only horse/lifesaver poses and waterlogged ring variants (rigid fixed-rest transforms).')

# Rattle and drum use one idle-size rest fit; per-pose fitting rescales a
# rotating instrument as its axis-aligned bounds change. Keep one center and
# use only vertical support translation after rotation to stay on the floor.
RIGID_TOYS=('chao_rattle','chao_drum')
RIGID_TOY_ANGLES=(0,8,0,-8)
RIGID_TOY_FLOOR=.08

def rigid_toy_rest(n):
 if n not in RIGID_TOYS:raise ValueError(n)
 lim=list(LIMITS[n]);lim[1]=RIGID_TOY_FLOOR;lim[4]-=RIGID_TOY_FLOOR
 return fit(toy_mesh(n)[0],lim)

def rigid_toy_pivot(rest):
 b=rest.bounds()
 return tuple((b[a]+b[a+3])/2 for a in range(3))

def rigid_toy_pose(n,frame):
 rest=rigid_toy_rest(n)
 angle=0 if frame<0 else RIGID_TOY_ANGLES[frame]
 pose=rotate(rest,2,angle,rigid_toy_pivot(rest)) if angle else rest
 # Ground support is translation, never another fit: shape and X/Z stay rigid.
 return pose.transformed(center=(0,0,0),offset=(0,RIGID_TOY_FLOOR-pose.bounds()[1],0))

def rigid_toy_poses_only():
 # Targeted repair: retain bindings, items, icons, textures and other toy fixes.
 for n in RIGID_TOYS:
  tex=json.loads((A/'models/block'/(n+'_idle.json')).read_text())['textures']
  for frame in (-1,0,1,2,3):
   stage='idle' if frame<0 else 'play_'+str(frame);name=n+'_'+stage
   write(A/'models/block'/(name+'.json'),native(rigid_toy_pose(n,frame),name,tex))
 print('Regenerated only rattle/drum block poses (rigid fixed-rest transforms).')

def toys():
 report=[]
 for n in TOYS:
  m,mats=toy_mesh(n);base=fit(toy_item_mesh(n)[0],(1,1,1,15,15,15));tex=bindings(n,mats);item(base,n,mats,tex);limits=LIMITS.get(n,(3,0,3,13,8,13));poses=[]
  for frame in [-1,0,1,2,3]:
   stage='idle' if frame<0 else 'play_'+str(frame);pose=m;binding=dict(tex)
   if n=='chao_tv':
    # Original screen child uses the original TV channel frames. Cabinet stays still.
    k=next(k for k in mats if k.startswith('sa2_channel_a_01_'))
    screen=source('al_toy','channel_a_0'+str(1 if frame<0 else frame+1));p='block/sa2_02515/chao_tv/screen_'+stage;screen.save(A/'textures'/(p+'.png'));binding[k]='chao_garden:'+p
   elif frame>=0 and n=='chao_coffin':
    pose=adapt(coffin_source_pose(frame))[0]
   elif frame>=0:
    angle=(0,8,0,-8)[frame] if n!='chao_coffin' else (0,3,0,-3)[frame]
    if n in (*MOUNTED_TOYS,*RIGID_TOYS):pass  # Fixed-rest transform applied below.
    elif n not in ('chao_radio','chao_piano','chao_organ'):pose=rotate(pose,2,angle)
    else:
     # Press source key faces / speaker membrane, while the cabinet remains planted.
     moving=Mesh()
     for ps,uv,k in pose.faces:
      dy=-(.04 if frame%2 else 0) if any(t in k for t in ('piano01','radi03')) else 0
      moving.face([(x,y+dy,z) for x,y,z in ps],k,uv)
     pose=moving
   lim=list(limits)
   if frame>=0 and n=='chao_maracas':lim[4]=6
   lim[1]=.08;lim[4]-=.08
   if n=='chao_coffin':pose=coffin_pose(frame)
   elif n in MOUNTED_TOYS:pose=mounted_toy_pose(n,frame)
   elif n in RIGID_TOYS:pose=rigid_toy_pose(n,frame)
   else:pose=fit(pose,lim)
   d=native(pose,n+'_'+stage,binding);write(A/'models/block'/(n+'_'+stage+'.json'),d);poses.append({'stage':stage,'bounds':pose.bounds()})
  report.append({'id':n,'source':TOYS[n]['object_offset'],'faces':len(m.faces),'poses':poses})
 lifesaver_float_assets()
 return report

def entity_mesh(mesh,tiles,atlas_size):
 out={'name':'sa2_source','material':'hat','shading':'flat','vertices':[],'faces':[]}
 for pts,uv,k in mesh.faces:
  start=len(out['vertices']);out['vertices'].extend(pts);x,y,w,h=tiles[k];out['faces'].append({'v':list(range(start,start+len(pts))),'uv':[[x+u*w,y+v*h] for u,v in uv]})
 return out

def hats():
 atlas=Image.new('RGBA',(1024,1024));atlas.paste(Image.open(D/'market_accessories.png'),(0,0));tiles={};allmats={};meshes={};report=[]
 for n in HATS:
  m,mat=adapt(HATS[n],lambda p:(p[0],-p[1],-p[2]));meshes[n]=m;allmats.update(mat)
 for i,(k,im) in enumerate(sorted(allmats.items())):
  x=i%8*128;y=(i//8+1)*128;assert y+128<=1024;atlas.paste(im.resize((128,128),Image.Resampling.NEAREST),(x,y));tiles[k]=(x,y,128,128)
 atlas.save(A/'textures/entity/chao/market_accessories.png')
 # The merchant uses the unchanged palette in the top-left of the enlarged atlas.
 merchant=json.loads((A/'models/entity/market_accessories.json').read_text());merchant.update(texture_width=1024,texture_height=1024);write(A/'models/entity/market_accessories.json',merchant)
 for p in (D/'previous-models/hats').glob('*.json'):
  data=json.loads(p.read_text());data.update(texture_width=1024,texture_height=1024)
  for bone in data['bones']:
   n=bone['name']
   if n not in meshes:continue
   old=[v for m in bone['meshes'] for v in m['vertices']];box=[min(v[a] for v in old) for a in range(3)]+[max(v[a] for v in old) for a in range(3)]
   adapted=fit(meshes[n],box,True);bone['meshes']=[entity_mesh(adapted,tiles,1024)]
  write(A/'models/entity/hats'/p.name,data)
 for n in HATS:
  m,mats=adapt(HATS[n]);m=fit(m,(1,1,1,15,15,15));tex=bindings(n,mats);item(m,n,mats,tex);report.append({'id':n,'source':HATS[n]['object_offset'],'faces':len(m.faces)})
 return report

def bevel_mesh(part):
 # Chamfer a cuboid without changing the fitting envelope or bone attachment.
 pts=part['vertices'];lo=[min(p[a] for p in pts) for a in range(3)];hi=[max(p[a] for p in pts) for a in range(3)];x,y,z=lo;X,Y,Z=hi;b=min(X-x,Z-z)*.07
 ring=[(x+b,z),(X-b,z),(X,z+b),(X,Z-b),(X-b,Z),(x+b,Z),(x,Z-b),(x,z+b)]
 verts=[[a,yy,c] for yy in (y,Y) for a,c in ring];faces=[];uv=part['faces'][0]['uv']
 for i in range(8):j=(i+1)%8;faces.append({'v':[i,j,j+8,i+8],'uv':uv})
 for ids in (list(reversed(range(8))),list(range(8,16))):
  for j in range(1,7):faces.append({'v':[ids[0],ids[j],ids[j+1]],'uv':uv[:3]})
 return {**part,'vertices':verts,'faces':faces}

def clothes():
 palette=Image.open(D/'cwe_accessories.png').convert('RGBA').resize((256,256),Image.Resampling.NEAREST)
 cloth=ImageOps.grayscale(source('al_body','alo_mask_wool02')).convert('RGBA').resize((64,64),Image.Resampling.BOX)
 # Neutral cloth retains dye colors; the weave is sampled from the original wool cap.
 gray=ImageOps.grayscale(cloth);gray=gray.point(lambda v:205+v*45//255);cloth=Image.merge('RGBA',(gray,gray,gray,Image.new('L',gray.size,255)));palette.paste(cloth,(0,0));palette.save(A/'textures/entity/chao/hoodie_fabric02515.png')
 for n in ('blue_hoodie_accessory','pink_hoodie_accessory','hoodie_sleeve_left','hoodie_sleeve_right'):
  data=json.loads((D/'previous-models/cwe_accessories'/(n+'.json')).read_text());data['bones'][0]['meshes'][0]=bevel_mesh(data['bones'][0]['meshes'][0]);write(A/'models/entity/cwe_accessories'/(n+'.json'),data)

def scenery():
 for p in json.loads((D/'rollback.json').read_text()):shutil.copy2(D/'rollback'/(p+'.png'),A/'textures'/(p+'.png'))
 # Area averaging, rather than nearest-neighbour aliasing, keeps source grass color with less sparkle.
 src=Image.open(ROOT/'art/sa2_02514/textures/al_stg_neut_tex/gneut03.png').convert('RGBA');src.resize((32,32),Image.Resampling.BOX).resize((64,64),Image.Resampling.NEAREST).save(A/'textures/scene/garden_turf.png')
 for path,entry in json.loads((D/'artwork.json').read_text()).items():
  im=source(entry['archive'],entry['texture'])
  if 'school0252/' in path:im=ImageOps.pad(im,(128,128),method=Image.Resampling.NEAREST,color=(223,211,183,255))
  im.save(A/'textures'/(path+'.png'))
 # Pictures use source aspect without stretching the original drawing into a landscape rectangle.
 for n in ('school_art_chao','school_art_friends','school_art_family','school_art_garden','school_art_chao_small'):
  p=A/'models/block'/(n+'.json');d=json.loads(p.read_text());base=d.get('parent')
  if 'elements' in d:
   for e in d['elements']:
    # Preserve mounting and overall frame size; fit the source art inside a square face.
    if any(f['texture']=='#base' for f in e['faces'].values()):
     width=e['to'][0]-e['from'][0];height=e['to'][1]-e['from'][1]
     if width>height:
      center=(e['to'][0]+e['from'][0])/2;e['from'][0]=center-height/2;e['to'][0]=center+height/2
   write(p,d)

def main():
 scenery();tr=toys();hr=hats();clothes()
 write(A/'models/sa2_02515/materials.mtl','\n'.join(f'newmtl {k}\nKd 1.0 1.0 1.0\nmap_Kd #{k}\n' for k in sorted(MATERIALS)))
 # Keep the existing material coverage manifest current for resource audit/replay.
 from lobby_material_manifest0255 import render_textures
 mp=ROOT/'art/lobby_garden_manifest_025.json';m=json.loads(mp.read_text());m['render_textures']=render_textures(ROOT,m['block_atlas_textures']);write(mp,m)
 write(ROOT/'docs/validation-0.25.15/source-assets.json',{'release':'0.25.15','toys':tr,'hats':hr,'restored_textures':json.loads((D/'rollback.json').read_text()),'artwork':json.loads((D/'artwork.json').read_text()),'hoodies':'original-wool material adaptation and chamfered body/sleeves; custom garment, not a vanilla SA2 model'})
 print('0.25.15: eight texture reversions, filtered grass, ten original school pictures, 17 toys/85 poses, twelve hats, two hoodie colors and sleeves.')
if __name__=='__main__':
 if sys.argv[1:]==['--coffin-poses']:coffin_poses_only()
 elif sys.argv[1:]==['--mounted-toy-poses']:mounted_toy_poses_only()
 elif sys.argv[1:]==['--rigid-toy-poses']:rigid_toy_poses_only()
 elif not sys.argv[1:]:main()
 else:raise SystemExit('Usage: generate_sa2_assets_02515.py [--coffin-poses | --mounted-toy-poses | --rigid-toy-poses]')
