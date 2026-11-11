"""Offline adaptations of supplied SA2B car/Sonic doll/pogo meshes.

The Shadow plush is an explicit adaptation of the Sonic plush geometry using
its original Shadow face texture plus authored black/red/white cloth colors.
No exact Shadow doll geometry was recovered. Never executes game binaries.
"""
from pathlib import Path
import sys,json,struct,hashlib,math,tempfile,subprocess,wave
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'art'),str(ROOT/'scripts')]
import extract_sa2_02515 as decode
import generate_sa2_assets_02515 as old
from fruit_forms02512 import Mesh,normal,render
A=ROOT/'src/main/resources/assets/chao_garden';OUT=A/'models/active02549';OUT.mkdir(parents=True,exist_ok=True)
SOURCES={'chao_toy_car':0x6e0ac,'chao_sonic_doll':0x68870,'chao_shadow_doll':0x68870,'chao_pogo_stick':0x6bb48}
B=decode.relocate(decode.prs((ROOT/'art/sa2_02514/original/ChaoMain.prs').read_bytes()))
T=decode.prs((ROOT/'art/sa2_02514/original/al_toy.prs').read_bytes());N=decode.texture_names(T)
IMAGES={};p=8+struct.unpack_from('<I',T,4)[0]
for name in N:
 length=struct.unpack_from('<I',T,p+4)[0];IMAGES[name]=decode.decode(T[p:p+8+length]);p+=8+length
assert p==len(T)
old.source=lambda arc,n:IMAGES[n].copy()
def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(data if isinstance(data,str) else json.dumps(data,indent=2)+'\n')
def point(mesh,fn):
 out=Mesh()
 for ps,uv,mat in mesh.faces:out.face([fn(p) for p in ps],mat,uv)
 return out
def closest(mesh,goal):
 # Ericson closest point on a triangle, used only to choose real contact landmarks.
 import numpy as np
 p=np.array(goal);best=None
 for vertices,uv,mat in mesh.faces:
  a,b,c=map(np.array,vertices[:3]);ab=b-a;ac=c-a;ap=p-a;d1=ab@ap;d2=ac@ap
  if d1<=0 and d2<=0:q=a
  else:
   bp=p-b;d3=ab@bp;d4=ac@bp
   if d3>=0 and d4<=d3:q=b
   else:
    vc=d1*d4-d3*d2
    if vc<=0 and d1>=0 and d3<=0:q=a+ab*(d1/(d1-d3))
    else:
     cp=p-c;d5=ab@cp;d6=ac@cp
     if d6>=0 and d5<=d6:q=c
     else:
      vb=d5*d2-d1*d6
      if vb<=0 and d2>=0 and d6<=0:q=a+ac*(d2/(d2-d6))
      else:
       va=d3*d6-d5*d4
       if va<=0 and d4-d3>=0 and d5-d6>=0:q=b+(c-b)*((d4-d3)/((d4-d3)+(d5-d6)))
       else:q=a+ab*(vb/(va+vb+vc))+ac*(vc/(va+vb+vc))
  dist=float(((q-p)**2).sum())
  if best is None or dist<best[0]:best=(dist,q,normal(vertices))
 return {'point':[float(v) for v in best[1]],'normal':list(best[2])}
def native(mesh,name,textures):
 lines=['# Supplied SA2B source adaptation 0.25.49; block coordinates.','mtllib chao_garden:models/active02549/materials.mtl'];i=1
 for ps,uv,mat in mesh.faces:
  nn=normal(ps)
  if sum(x*x for x in nn)<.9:continue
  lines+=['usemtl '+mat]+['v '+' '.join(f'{v:.9f}' for v in p) for p in ps]+[f'vt {u:.9f} {1-v:.9f}' for u,v in uv]+['vn '+' '.join(f'{v:.9f}' for v in nn)]*len(ps)
  lines+=['f '+' '.join(f'{k}/{k}/{k}' for k in range(i,i+len(ps)))];i+=len(ps)
 write(OUT/(name+'.obj'),'\n'.join(lines)+'\n')
 return {'loader':'neoforge:obj','model':'chao_garden:models/active02549/'+name+'.obj','automatic_culling':False,'shade_quads':True,'flip_v':True,'textures':textures,'render_type':'minecraft:cutout'}
def shadow(mats,source_faces):
 for name,im in list(mats.items()):
  if 'soni03_' in name:
   # Sonic's face material148 uses U0..2 with mirrored repeat. Replacing
   # its already-wrapped64px atlas by stretching one32px Shadow tile made
   # the eyes/muzzle sample the wrong halves. Wrap the primary Shadow tile
   # with exactly the source material's original limits/flags instead.
   flags=int(name.rsplit('_',1)[1]);coords=[f['uv'] for f in source_faces if 'sa2_'+f['texture']+'_'+str(f['wrap_flags'])==name]
   uv=[v for face in coords for v in face]
   limits=[(min(v[i] for v in uv),max(v[i] for v in uv)) for i in (0,1)]
   mats[name],_,_=old.wrap_atlas(IMAGES['sh_doll01'],limits,flags);continue
  im=im.copy();pix=im.load()
  for y in range(im.height):
   for x in range(im.width):
    r,g,b,a=pix[x,y]
    if b>r*1.3 and b>g*1.15:
     # Red edge stripe identifies Shadow's quills without altering the mesh.
     pix[x,y]=(190,25,40,a) if x>im.width*.72 else (25,25,31,a)
    elif 'soni01_' in name and r>g*.9 and g>b*1.1:pix[x,y]=(235,236,232,a)
  mats[name]=im
ONLY=next((set(arg.split('=',1)[1].split(',')) for arg in sys.argv[1:] if arg.startswith('--only=')),None)
if ONLY and not ONLY<=SOURCES.keys():raise ValueError('Unknown active toy selection')
reports=json.loads((OUT/'manifest.json').read_text()) if ONLY else {}
materials={line.split(' ',1)[1] for line in (OUT/'materials.mtl').read_text().splitlines() if line.startswith('newmtl ')} if ONLY else set()
for ident,address in SOURCES.items():
 if ONLY and ident not in ONLY:continue
 info={'source_archive':'al_toy','faces':decode.hierarchy(B,address,N)}
 if ident=='chao_shadow_doll':
  # The Sonic donor's four nose triangles use constant(.5,.5), borrowing
  # black from Sonic's pupil. Shadow's pupil is red there, so point these
  # same triangles at a black pixel in the primary Shadow face tile.
  nose=0
  for face in info['faces']:
   if face['texture']=='soni03' and face['wrap_flags']==148 and all(tuple(uv)==(.5,.5) for uv in face['uv']):
    face['uv']=[(1/64,1/64)]*len(face['uv']);nose+=1
  assert nose==4,'Expected four original donor nose triangles'
 m,mats=old.adapt(info)
 if ident=='chao_shadow_doll':shadow(mats,info['faces'])
 if ident=='chao_toy_car':m=point(m,lambda p:(-p[2],p[1],p[0]))
 # A common upright source frame is used for hand models and all measurements.
 held=old.fit(m,(0,0,0,1,1,1));textures={}
 for mat,im in mats.items():
  path='block/active02549/'+ident+'/'+mat;dest=A/'textures'/(path+'.png');dest.parent.mkdir(parents=True,exist_ok=True);im.save(dest);textures[mat]='chao_garden:'+path;materials.add(mat)
 textures['particle']=next(iter(textures.values()))
 hand=native(held,ident+'_hand',textures);write(A/'models/item'/(ident+'_hand.json'),hand)
 if ident=='chao_toy_car':
  floor=point(old.fit(m,(3.4,0,2.5,12.6,4.8,13.5)),lambda p:(16-p[0],p[1],16-p[2]))
  # Smaller0.25.51 resting car: retain its orientation and floor plane,
  # shrinking about horizontal block centre. Held source stays unchanged.
  floor=point(floor,lambda p:(8+(p[0]-8)*.70,p[1]*.70,8+(p[2]-8)*.70))
 else:
  flat=point(m,lambda p:(p[0],p[2],-p[1]))
  floor=old.fit(flat,(3.5,0,2,12.5,3.0,14))
 block=native(point(floor,lambda p:tuple(v/16 for v in p)),ident+'_floor',textures);write(A/'models/block'/(ident+'_idle.json'),block)
 variants={}
 for face,angle in [('north',0),('east',90),('south',180),('west',270)]:
  for playing in [False,True]:
   for frame in range(4):variants[f'facing={face},frame={frame},playing={str(playing).lower()}']={'model':'chao_garden:block/'+ident+'_idle','y':angle}
 write(A/'blockstates'/(ident+'.json'),{'variants':variants})
 display={'thirdperson_righthand':{'translation':[0,2,0],'scale':[.55,.55,.55]},'thirdperson_lefthand':{'translation':[0,2,0],'scale':[.55,.55,.55]},'ground':{'translation':[0,2,0],'scale':[.5,.5,.5]},'fixed':{'rotation':[0,180,0],'scale':[.8,.8,.8]}}
 item={'loader':'neoforge:separate_transforms','gui_light':'front','textures':{'layer0':'chao_garden:item/'+ident,'particle':textures['particle']},'base':{'parent':'chao_garden:item/'+ident+'_hand','display':display},'perspectives':{'gui':{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/'+ident}}}};write(A/'models/item'/(ident+'.json'),item)
 icon=render(old.fit(m,(1,1,1,15,15,15)),mats,32);icon.save(A/'textures/item'/(ident+'.png'))
 write(ROOT/'src/main/resources/data/chao_garden/loot_table/blocks'/(ident+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'chao_garden:'+ident}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 # Explicit Minecraft acquisition adaptation, not an asserted SA2 price/reward.
 ingredient='minecraft:redstone' if ident=='chao_toy_car' else 'minecraft:slime_ball' if ident=='chao_pogo_stick' else 'minecraft:blue_wool' if ident=='chao_sonic_doll' else 'minecraft:black_wool'
 write(ROOT/'src/main/resources/data/chao_garden/recipe'/(ident+'.json'),{'type':'minecraft:crafting_shaped','category':'misc','pattern':[' W ','SIS',' S '],'key':{'W':{'item':ingredient},'S':{'item':'minecraft:stick'},'I':{'item':'minecraft:iron_nugget'}},'result':{'id':'chao_garden:'+ident,'count':1}})
 centered=point(held,lambda p:tuple(v-.5 for v in p));contacts={}
 if 'doll' in ident:
  for side,x in [('right',-.20),('left',.20)]:contacts[side]=closest(centered,(x,-.05,.05))
 if ident=='chao_toy_car':
  for side,x in [('right',-.13),('left',.13)]:contacts[side]=closest(centered,(-.25,-.18,-.19) if side=='right' else (x,.12,-.28))
 if ident=='chao_pogo_stick':
  for side,x in [('right',-.273),('left',.273)]:contacts[side]=closest(centered,(x,.44545,.038))
 reports[ident]={'offset':hex(address),'source_triangles':len(info['faces']),'emitted_triangles':len(held.faces),'held_bounds':held.bounds(),'floor_bounds_pixels':floor.bounds(),'contacts':contacts,'provenance':'Supplied SA2B ChaoMain.prs / al_toy.prs'+('; Sonic geometry/Shadow face + authored palette adaptation; face atlas uses original mirrored-repeat UV range, corrected0.25.51' if 'shadow' in ident else '')}
reports['shadow_face_02551']={'reference':'Primary supplied SA2 Battle al_toy.prs texture sh_doll01; exact decoded source tile',
 'source_texture':'sh_doll01','source_size':list(IMAGES['sh_doll01'].size),
 'source_rgba_sha256':hashlib.sha256(IMAGES['sh_doll01'].tobytes()).hexdigest(),
 'material148_wrap':'Two32px source tiles; right tile mirrors U, rather than stretching one tile across both repeats',
 'nose_uv':'Four donor nose triangles remapped from Sonic black-pupil UV(.5,.5), which is red on Shadow, to black source pixel centre(1/64,1/64)',
 'geometry':'Existing Sonic-doll adaptation; all vertices/contact geometry unchanged, no independently recovered Shadow mesh claimed'}
reports['car_scale_02551']={'factor':.70,'pivot_pixels':[8,0,8],'held_source_unchanged':True}
write(OUT/'materials.mtl' ,'\n'.join('newmtl '+m+'\nKd 1 1 1\nKa 1 1 1\nmap_Kd #'+m for m in sorted(materials))+'\n')
reports['inputs']={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [ROOT/'art/sa2_02514/original/ChaoMain.prs',ROOT/'art/sa2_02514/original/al_toy.prs']}
write(OUT/'manifest.json',reports)
# Original, short mechanical/cloth sounds; never copies character voice recordings.
for name,duration in ([] if ONLY else [('car',.12),('doll',.13),('pogo',.22)]):
 rate=44100;values=[]
 for i in range(round(rate*duration)):
  t=i/rate;u=t/duration;envelope=min(1,t/.004)*(1-u)**2
  if name=='pogo':v=.5*math.sin(2*math.pi*(350*t-520*t*t))+.16*math.sin(2*math.pi*630*t)
  elif name=='car':v=.35*math.sin(2*math.pi*170*t)+.18*math.sin(2*math.pi*1200*t)*math.exp(-55*t)
  else:v=.28*math.sin(2*math.pi*(730*t+190*t*t))+.12*math.sin(2*math.pi*1211*t)
  values.append(v*envelope)
 with tempfile.TemporaryDirectory() as tmp:
  p=Path(tmp)/'source.wav'
  with wave.open(str(p),'wb') as f:f.setnchannels(1);f.setsampwidth(2);f.setframerate(rate);f.writeframes(struct.pack('<'+'h'*len(values),*[round(v*32767) for v in values]))
  dest=A/'sounds/toys'/(name+'.ogg');dest.parent.mkdir(parents=True,exist_ok=True);subprocess.run(['ffmpeg','-v','error','-y','-i',str(p),'-fflags','+bitexact','-flags:a','+bitexact','-c:a','libvorbis','-q:a','5',str(dest)],check=True)
print(json.dumps({k:v for k,v in reports.items() if k!='inputs'},indent=2))
