"""Extract supplied SA2B broom/book/crayons and fit separate floor/hand views.

Ninja object geometry/textures are from supplied ChaoMain.prs/al_toy.prs. The
blank drawing paper and transient coloured strokes are new presentation quads.
This does not claim game-footage-accurate animation; see manifest provenance.
"""
from pathlib import Path
import sys,json,math,hashlib,struct,subprocess,tempfile,wave
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'art'),str(ROOT/'scripts')]
import extract_sa2_02515 as extract
from generate_sa2_assets_02514 import wrap_atlas
from fruit_forms02512 import normal,Mesh,render
from generate_fruit_models_02512 import DISPLAY
A=ROOT/'src/main/resources/assets/chao_garden';D=A/'models/quiet02549'
SOURCES={'broom':0x6c37c,'book_left':0x679fc,'book_right':0x676b8,'book_page':0x67370,'crayon_box':0x6d464,'crayon':0x6d724}
STROKES=[(-.55,-.25,.55,-.25),(-.55,.22,.55,.22),(-.35,.10,-.35,-.10),(-.35,-.10,.20,-.10),(.20,-.10,.20,.13),(.20,.13,-.35,.13),(-.05,-.10,.18,-.34),(.18,-.34,.32,-.18)]
STROKES=[(.90*x1,.55*z1,.90*x2,.55*z2) for x1,z1,x2,z2 in STROKES]
COLORS=['377ee8','69b44a','b36234','f6b534','f6b534','f6b534','54ad54','54ad54']
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(v if isinstance(v,str) else json.dumps(v,indent=2)+'\n')
_TEXTURES={}
def adapt(info):
 faces=info['faces'];ranges={};materials={};mesh=Mesh()
 for f in faces:
  key='sa2_'+f['texture']+'_'+str(f['wrap_flags']);ranges.setdefault(key,[[],[]])
  for uv in f['uv']:
   for axis in range(2):ranges[key][axis].append(uv[axis])
 for key,coords in list(ranges.items()):
  name,flags=key[4:].rsplit('_',1);image=_TEXTURES[name].copy();image.putalpha(image.getchannel('A').point(lambda a:255 if a>=128 else 0))
  image,low,count=wrap_atlas(image,[(min(v),max(v)) for v in coords],int(flags));materials[key]=image;ranges[key]=(low,count)
 for f in faces:
  key='sa2_'+f['texture']+'_'+str(f['wrap_flags']);low,count=ranges[key]
  mesh.face(f['points'],key,[((u-low[0])/count[0],(v-low[1])/count[1]) for u,v in f['uv']])
  if f.get('double_sided'):mesh.face(list(reversed(f['points'])),key,list(reversed([((u-low[0])/count[0],(v-low[1])/count[1]) for u,v in f['uv']])))
 return mesh,materials
def canon(p):return (p[0],-p[1],-p[2])
def mapped(mesh,fn):
 m=Mesh()
 for ps,uv,mat in mesh.faces:m.face([fn(p) for p in ps],mat,uv)
 return m

def native(name,mesh,textures,display=None):
 lines=['# Supplied original SA2B toy surfaces; separate 0.25.49 presentation.','mtllib chao_garden:models/quiet02549/materials.mtl'];index=1
 for ps,uv,mat in mesh.faces:
  n=normal(ps)
  if sum(v*v for v in n)<.9:continue
  lines.append('usemtl '+mat)
  lines+=['v '+' '.join(f'{v:.9f}' for v in p) for p in ps]
  im=Image.open(A/'textures'/(textures[mat].split(':',1)[1]+'.png'));du,dv=.5/im.width,.5/im.height
  lines+=['vt '+f'{min(1-du,max(du,u)):.9f} {1-min(1-dv,max(dv,v)):.9f}' for u,v in uv]
  lines+=['vn '+' '.join(f'{v:.9f}' for v in n)]*len(ps)
  lines.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(index,index+len(ps))));index+=len(ps)
 write(D/(name+'.obj'),'\n'.join(lines)+'\n')
 data={'loader':'neoforge:obj','model':f'chao_garden:models/quiet02549/{name}.obj','automatic_culling':False,'shade_quads':True,'flip_v':True,'textures':{**textures,'particle':next(iter(textures.values()))},'render_type':'minecraft:cutout'}
 if display:data['display']=display
 return data

def models():
 b=extract.relocate(extract.prs((ROOT/'art/sa2_02514/original/ChaoMain.prs').read_bytes()));raw=extract.prs((ROOT/'art/sa2_02514/original/al_toy.prs').read_bytes());names=extract.texture_names(raw)
 pos=8+struct.unpack_from('<I',raw,4)[0]
 for name in names:
  length=struct.unpack_from('<I',raw,pos+4)[0];_TEXTURES[name]=extract.decode(raw[pos:pos+8+length]).convert('RGBA');pos+=8+length
 parts={};bindings={};records={};mats_all={}
 for part,address in SOURCES.items():
  faces=extract.chunk(b,address,names);mesh,mats=adapt({'faces':faces,'source_archive':'al_toy'})
  tex={}
  for key,im in mats.items():
   path='block/quiet02549/'+key;dest=A/'textures'/(path+'.png');dest.parent.mkdir(parents=True,exist_ok=True);im.save(dest);tex[key]='chao_garden:'+path;mats_all[key]=im
  parts[part]=mesh;bindings[part]=tex
  records[part]={'address':hex(address),'source_triangles':len(faces),'source_textures':sorted({f['texture'] for f in faces}),'raw_bounds':mesh.bounds()}
  hand=mapped(mesh,lambda p:tuple(v+.5 for v in canon(p)))
  write(A/'models/item'/('chao_quiet_hand_'+part+'.json'),native('hand_'+part,hand,tex))
 # Existing source closed-book hinge rotations are +/-90deg; rotate the closed
 # assembly onto its cover. The two actual covers coincide in XZ with their
 # authored thickness retained, instead of leaving an upright book on the floor.
 book=Mesh();book.faces.extend(parts['book_right'].faces)
 book.faces.extend(mapped(parts['book_left'],lambda p:(-p[0],-p[1],p[2])).faces)
 book.faces.extend(parts['book_page'].faces)
 world={
  'chao_broom':mapped(parts['broom'],lambda p:(p[0],-p[2],p[1])),
  'chao_picture_book':book,
  'chao_crayons':parts['crayon_box']}
 for name,m in world.items():
  bounds=m.bounds();long=max(bounds[3]-bounds[0],bounds[5]-bounds[2]);scale={'chao_broom':.68,'chao_picture_book':.50,'chao_crayons':.48}[name]/long
  centre=[(bounds[i]+bounds[i+3])/2 for i in range(3)]
  m=mapped(m,lambda p:((p[0]-centre[0])*scale+.5,(p[1]-bounds[1])*scale,(p[2]-centre[2])*scale+.5))
  tex={**bindings['book_left'],**bindings['book_right'],**bindings['book_page']} if name=='chao_picture_book' else bindings['broom' if name=='chao_broom' else 'crayon_box']
  data=native(name+'_floor',m,tex,DISPLAY);write(A/'models/block'/(name+'.json'),data)
  # GUI only: present the supplied top/cover surface to the viewer. The
  # exact previous mesh/display model remains the non-GUI base, including
  # first/third-person, ground, fixed and held presentations.
  base={**data,'display':DISPLAY}
  write(A/'models/item'/(name+'.json'),{'loader':'neoforge:separate_transforms','gui_light':'front',
    'base':base,'perspectives':{'gui':{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/'+name}}}})
  write(A/'blockstates'/(name+'.json'),{'variants':{f'facing={f}':{'model':f'chao_garden:block/{name}','y':rot} for f,rot in [('north',0),('east',90),('south',180),('west',270)]}})
  # Rotate floor-up+Y onto camera-front+Z before the shared orthographic
  # render. A small in-plane turn keeps long broom/book silhouettes legible.
  turn=math.radians({'chao_broom':-27,'chao_picture_book':-12,'chao_crayons':-12}[name])
  def gui_point(p):
   x,y,z=(p[i]*16-8 for i in range(3));x,y,z=x,-z,y
   c,s=math.cos(turn),math.sin(turn)
   return (x*c-y*s+8,x*s+y*c+8,z+8)
  icon=mapped(m,gui_point);render(icon,mats_all).save(A/'textures/item'/(name+'.png'))
  records[name]={'world_bounds':m.bounds(),'collision_model_units':[v*16 for v in m.bounds()]}
 # Official box/crayon remain unchanged. Only the expendable drawing sheet and
 # ink are new geometry; using the supplied paper texture does not make the
 # drawing itself an original game asset.
 paperkey='quiet_paper';papertex='block/quiet02549/'+paperkey;im=_TEXTURES['kami01'].copy();im.save(A/'textures'/(papertex+'.png'));bindings_paper={paperkey:'chao_garden:'+papertex}
 for i,col in enumerate(COLORS):
  key='quiet_ink_'+str(i);Image.new('RGBA',(4,4),'#'+col).save(A/'textures/block/quiet02549'/(key+'.png'));bindings_paper[key]='chao_garden:block/quiet02549/'+key
 for stage in range(9):
  p=Mesh();p.face([(-.77,0,-.48),(.77,0,-.48),(.77,0,.48),(-.77,0,.48)],paperkey,[(0,0),(1,0),(1,1),(0,1)])
  p.face(list(reversed([(-.77,.012,-.48),(.77,.012,-.48),(.77,.012,.48),(-.77,.012,.48)])),paperkey,[(0,0),(1,0),(1,1),(0,1)])
  for i,(x1,z1,x2,z2) in enumerate(STROKES[:stage]):
   dx,dz=x2-x1,z2-z1;length=math.hypot(dx,dz);ox,oz=-dz/length*.014,dx/length*.014
   p.face([(x1-ox,-.004,z1-oz),(x2-ox,-.004,z2-oz),(x2+ox,-.004,z2+oz),(x1+ox,-.004,z1+oz)],'quiet_ink_'+str(i),[(0,0),(1,0),(1,1),(0,1)])
  p=mapped(p,lambda q:tuple(v+.5 for v in q));write(A/'models/item'/f'chao_quiet_hand_paper_{stage}.json',native('hand_paper_'+str(stage),p,bindings_paper))
 # Own MTL namespace; no modification to shared source material libraries.
 allkeys=set(k for d in [*bindings.values(),bindings_paper] for k in d)
 write(D/'materials.mtl','\n\n'.join('newmtl '+k+'\nKd 1 1 1\nmap_Kd #'+k for k in sorted(allkeys))+'\n')
 manifest={'provenance':'Original supplied Sonic Adventure 2 Battle ChaoMain.prs/al_toy.prs toy geometry and textures. New paper/ink presentation quads. Motions are fitted adaptations; no verified toy-action footage was supplied.','source_sha256':hashlib.sha256((ROOT/'art/sa2_02514/original/ChaoMain.prs').read_bytes()).hexdigest(),'parts':records,'strokes':STROKES,'colors':COLORS,'book_parts':'Original two covers plus separate two-triangle page; raw attachment rotations replaced by runtime hinges.','motion_metadata':'CWE ALM_HOUKI, ALM_BOOK_START/YOMU/MEKURI and ALM_RAKUGAKI drawing enum names, as supplemental action-semantic references.'}
 write(D/'manifest.json',manifest)
 # Obtainable toy recipes and normal recoverable block loot.
 recipe={'chao_broom':{'type':'minecraft:crafting_shaped','pattern':[' S ',' S ','WWW'],'key':{'S':{'item':'minecraft:stick'},'W':{'item':'minecraft:wheat'}},'result':{'id':'chao_garden:chao_broom','count':1}},'chao_picture_book':{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:book'},{'item':'minecraft:cyan_dye'},{'item':'minecraft:yellow_dye'}],'result':{'id':'chao_garden:chao_picture_book','count':1}},'chao_crayons':{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:paper'},{'item':'minecraft:honeycomb'},{'item':'minecraft:red_dye'},{'item':'minecraft:blue_dye'},{'item':'minecraft:green_dye'}],'result':{'id':'chao_garden:chao_crayons','count':1}}}
 for name,data in recipe.items():
  write(ROOT/'src/main/resources/data/chao_garden/recipe'/(name+'.json'),data)
  write(ROOT/'src/main/resources/data/chao_garden/loot_table/blocks'/(name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'chao_garden:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 print('Quiet official source parts:',{k:v['source_triangles'] for k,v in records.items() if 'source_triangles' in v})
 print('Floor collision model units:',{k:v['collision_model_units'] for k,v in records.items() if 'collision_model_units' in v})

def sound():
 rate=44100
 for name,duration in [('broom',.29),('book_page',.32),('crayon',.48)]:
  values=[];last=0;seed=149
  for i in range(round(rate*duration)):
   t=i/rate;seed=(seed*1664525+1013904223)&0xffffffff;noise=seed/0xffffffff*2-1
   last=.72*last+.28*noise;env=min(1,t/.018)*min(1,(duration-t)/.055)
   if name=='broom':v=(.85*last+.10*math.sin(math.tau*175*t))*env*(.75+.25*math.sin(math.pi*t/duration))
   elif name=='book_page':v=(noise-last)*env*(.35+.65*math.sin(math.pi*t/duration)**2)
   else:v=(.65*(noise-last)+.2*math.sin(math.tau*920*t))*.75*env*(.55+.45*math.sin(math.tau*29*t)**2)
   values.append(v)
  peak=max(map(abs,values));pcm=[round(v/peak*.48*32767) for v in values]
  with tempfile.TemporaryDirectory() as tmp:
   wav=Path(tmp)/(name+'.wav')
   with wave.open(str(wav),'wb') as f:f.setnchannels(1);f.setsampwidth(2);f.setframerate(rate);f.writeframes(struct.pack('<'+'h'*len(pcm),*pcm))
   dest=A/'sounds/toys'/(name+'.ogg');dest.parent.mkdir(parents=True,exist_ok=True)
   subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-fflags','+bitexact','-flags:a','+bitexact','-c:a','libvorbis','-q:a','5',str(dest)],check=True)
 print('Original procedural sweep/page/crayon samples generated')
if __name__=='__main__':
 models()
 if '--models-only' not in sys.argv:sound()
