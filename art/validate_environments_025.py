#!/usr/bin/env python3
"""Finite scenery resource contracts, real texture alpha and production/dev dimension separation."""
from pathlib import Path
from copy import deepcopy
import argparse
import math
import itertools
import json
import re
import tempfile
import zipfile
from PIL import Image, ImageChops, ImageStat
from scene_resource_contract_025 import lobby_blocks, scenery_items, scenery_atlases

ROOT=Path(__file__).resolve().parents[1]
ASSET=Path('src/main/resources/assets/chao_garden')
DATA=Path('src/main/resources/data/chao_garden')
ROOMS=('hub','kindergarten','garden','hero_garden','dark_garden')
VANILLA_PARENTS={'minecraft:block/block':(), 'minecraft:block/cube_all':('all',),
                 'minecraft:block/cube_bottom_top':('bottom','top','side')}
VANILLA_TEXTURES={'minecraft:block/glass'}
ARCH_PALETTES={'school_arch_principal':'green','school_arch_classroom':'blue','school_arch_health':'cyan',
               'school_arch_fortune':'purple','school_arch_market':'wood','school_arch_exit':'green'}
checks=0
used_textures=set()

def require(ok,message):
 global checks
 checks+=1
 if not ok:raise AssertionError(message)

def read(root,path):return json.loads((Path(root)/path).read_text())
def identifier(value):
 require(isinstance(value,str) and re.fullmatch(r'[a-z0-9_.-]+:[a-z0-9_./-]+',value) is not None,'Invalid resource identifier: '+str(value))
 namespace,path=value.split(':',1)
 require('..' not in path.split('/') and not path.startswith('/'),'Resource path escapes assets: '+value)
 return namespace,path

def texture(root,value):
 namespace,path=identifier(value)
 if namespace=='minecraft':require(value in VANILLA_TEXTURES,'Unreviewed vanilla scene texture: '+value);return
 require(namespace=='chao_garden','Undeclared scenery texture namespace: '+value)
 p=Path(root)/ASSET/'textures'/(path+'.png');require(p.is_file(),'Missing scene texture: '+value)
 with Image.open(p) as image:require(image.format=='PNG' and image.width>0 and image.height>0,'Invalid scene PNG: '+value)
 used_textures.add(value)

def model(root,name,domain,overrides=None,trail=()):
 namespace,path=identifier(name)
 require(namespace=='chao_garden' and path in domain,'Model outside finite scenery domain: '+name)
 require(name not in trail,'Cyclic scenery model parent: '+name)
 file=Path(root)/ASSET/'models'/(path+'.json');require(file.is_file(),'Missing scene model: '+name)
 m=deepcopy((overrides or {}).get(name)) if name in (overrides or {}) else json.loads(file.read_text())
 inherited={};parent=m.get('parent')
 if parent:
  if parent in VANILLA_PARENTS:
   for required in VANILLA_PARENTS[parent]:require(required in m.get('textures',{}),'Missing inherited texture '+required+' in '+name)
  else:inherited=model(root,parent,domain,overrides,trail+(name,))
 bindings={**inherited,**m.get('textures',{})}
 def resolve(value):
  aliases=set()
  while value.startswith('#'):
   key=value[1:];require(key in bindings and key not in aliases,'Unbound/cyclic texture alias '+value+' in '+name)
   aliases.add(key);value=bindings[key]
  texture(root,value)
 for value in bindings.values():resolve(value)
 for element in m.get('elements',[]):
  for face in element.get('faces',{}).values():
   require('texture' in face,'Texture-less scene model face: '+name);resolve(face['texture'])
 return bindings

def alpha(image,kind,rgba=None):
 require(image.mode=='RGBA','Scene alpha material is not RGBA: '+kind)
 lo,hi=image.getchannel('A').getextrema()
 if kind=='glass':
  require(0<lo<=hi<128,'Lobby glass became opaque or invisible')
  if rgba is not None:require(all(lo==hi==value for (lo,hi),value in zip(image.getextrema(),rgba)),'Lobby glass pixels differ from declared material')
 else:require(lo==0 and hi==255,'Cutout scenery lacks both empty and opaque pixels: '+kind)

def shader_contract(doc,vertex,fragment):
 require(doc.get('vertex')=='chao_garden:scene_unlit' and doc.get('fragment')=='chao_garden:scene_unlit','Scene shader stage IDs changed')
 require(set(doc.get('attributes',[]))=={'Position','UV0','Color'},'Scene shader vertex attributes incomplete')
 require(doc.get('samplers')==[{'name':'Sampler0'}],'Scene shader texture sampler missing')
 uniforms={row['name']:row for row in doc.get('uniforms',[])}
 require(set(uniforms)=={'ModelViewMat','ProjMat','ColorModulator'},'Scene shader uniforms changed or global fog leaked into material')
 require(all(uniforms[n]['type']=='matrix4x4' and uniforms[n]['count']==16 and len(uniforms[n]['values'])==16 for n in ('ModelViewMat','ProjMat')),'Scene shader matrix uniform schema invalid')
 require(uniforms['ColorModulator']['type']=='float' and uniforms['ColorModulator']['count']==4 and uniforms['ColorModulator']['values']==[1,1,1,1],'Scene shader color uniform schema invalid')
 require('in vec2 UV0;' in vertex and 'in vec4 Color;' in vertex,'Scene vertex shader dropped material inputs')
 require('texture(Sampler0,texCoord0)*vertexColor*ColorModulator' in fragment and 'fragColor=color;' in fragment,'Scene fragment shader no longer preserves texture alpha')


def atlas_contract(native,manifest,atlas):
 require(set(manifest)==native and len(manifest)==len(native),'Native scene atlas catalog differs from block-model references')
 sources=atlas.get('sources',[])
 require(len(sources)==len(native) and all(set(row)=={'type','resource'} and row['type']=='minecraft:single' for row in sources),'Native scene atlas sources must be additive exact single sprites')
 require({row['resource'] for row in sources}=={'chao_garden:'+name for name in native},'Native scene texture absent from block atlas')


def composite_states(block,meta,blockstate):
 variants=blockstate.get('variants',{})
 expected={f'facing={face},tile={tile}' for tile in range(meta['stateMin'],meta['stateMax']+1) for face in ('north','south','east','west')}
 require(set(variants)==expected,'Composite blockstate domain mismatch: '+block)
 models=set()
 for tile in range(meta['stateMin'],meta['stateMax']+1):
  used=tile if tile<meta['usedTiles'] else meta['fallback']
  name='chao_garden:block/'+block+'_tile_'+str(used);models.add(name)
  for face,rotation in (('south',0),('west',90),('north',180),('east',270)):
   entry=variants[f'facing={face},tile={tile}']
   require(entry.get('model')==name and entry.get('y',0)==rotation,'Composite tile/facing fallback mismatch: '+block+' tile '+str(tile))
 return models

def composite_models(root,block,meta,domain):
 expected={block+'_tile_'+str(tile)+'.json' for tile in range(meta['usedTiles'])}
 actual={p.name for p in (Path(root)/ASSET/'models/block').glob(block+'_tile_*.json')}
 require(actual==expected,'Composite model files differ from finite used tiles: '+block)
 texture(root,meta['texture'])
 for tile in range(meta['usedTiles']):
  name='chao_garden:block/'+block+'_tile_'+str(tile);model(root,name,domain)
  doc=read(root,ASSET/'models/block'/(block+'_tile_'+str(tile)+'.json'))
  require(doc.get('textures',{}).get('base')==meta['texture'],'Composite tile binds another atlas: '+name)
  x=tile%meta['columns'];y=tile//meta['columns']
  bounds=[x*16/meta['columns'],y*16/meta['rows'],(x+1)*16/meta['columns'],(y+1)*16/meta['rows']]
  for element in doc.get('elements',[]):
   for side,face in element.get('faces',{}).items():
    uv=face.get('uv',[]);expected_uv=[bounds[0],bounds[3],bounds[2],bounds[1]] if side=='down' else bounds
    require(len(uv)==4 and all(abs(a-b)<1e-7 for a,b in zip(uv,expected_uv)),'Composite tile UV does not address its declared atlas cell: '+name)
 require(read(root,ASSET/'models/block'/(block+'.json'))==read(root,ASSET/'models/block'/(block+'_tile_0.json')),'Composite item/base fallback drift: '+block)


def arch_palette(block,doc):
 if doc.get('chao_door_profile')=='curve0255':
  import sys
  if str(ROOT/'scripts') not in sys.path:sys.path.insert(0,str(ROOT/'scripts'))
  from validate_school_doors_0255 import check_arch
  check_arch(block,doc);return
 if doc.get('chao_door_profile')=='arch0254':
  import sys
  if str(ROOT/'scripts') not in sys.path:sys.path.insert(0,str(ROOT/'scripts'))
  from validate_school_doors_0254 import check_arch
  check_arch(block,doc);return
 # The supplied SA2 room references distinguish coloured front frames from
 # cream wall-depth corner infill. Existence-only texture checks missed a real
 # all-cream fallback, so verify what each actual visible element resolves to.
 bindings=doc.get('textures',{});expected='chao_garden:block/school025/'+ARCH_PALETTES[block]
 require(bindings.get('base')==expected,'Arch main material silently fell back from its role palette: '+block)
 require(bindings.get('cream')=='chao_garden:block/school025/cream','Arch corner infill palette changed: '+block)
 groups={'frame':[],'infill':[],'trim':[]}
 for element in doc.get('elements',[]):
  # The native ARCH anchor moved one cell forward to free the door's center slice.
  # Add that exact16px world translation before checking the old visible planes.
  z0=element['from'][2]+16;z1=element['to'][2]+16
  if abs(z0)<1e-7 and abs(z1-16.2)<1e-7:groups['frame'].append(element)
  elif abs(z0)<1e-7 and abs(z1-16)<1e-7:groups['infill'].append(element)
  elif abs(z0-16.21)<1e-7 and abs(z1-16.35)<1e-7:groups['trim'].append(element)
 require(len(doc.get('elements',[]))==22 and len(groups['frame'])==9 and len(groups['infill'])==6 and len(groups['trim'])==7,'Arch jamb/header/infill/trim component contract changed: '+block)
 for group,binding in (('frame','#base'),('infill','#cream'),('trim','#gold')):
  for element in groups[group]:
   require(element.get('faces') and all(face.get('texture')==binding for face in element['faces'].values()),'Arch '+group+' uses wrong actual face material: '+block)


DOOR_ROLES=('principal','classroom','health','fortune','exit','market')
def door_contract(root,block,doc,domain,override=None):
 cabinet=block=='school_tool_cabinet'
 for suffix in ('','_back'):
  with Image.open(Path(root)/ASSET/'textures/block/school025'/(block+suffix+'.png')) as image:
   require(image.size==(368,768),'Door source art must retain physical23:48 leaf proportions and16px mipmap alignment')
 expected={f'facing={face},hinge={hinge},open={opened},section={section},segment={part}' for face in ('north','south','east','west') for hinge in ('left','right') for opened in ('true','false') for section in ('leaf','center','tip','far_tip') for part in range(3)}
 require(set(doc.get('variants',{}))==expected,'School door must expose exactly four facings, two hinges, two open states, four sections and three heights: '+block)
 # Native FaceInfo vertex0/vertex2 own the supplied UV endpoints. In particular,
 # east starts atMAX_Z while west starts atMIN_Z; changing only face names is wrong.
 vertices={'north':((1,1,0),(0,0,0)),'south':((0,1,1),(1,0,1)),'east':((1,1,1),(1,0,0)),'west':((0,1,0),(0,0,1))}
 for face,rotation in [('south',0),('west',90),('north',180),('east',270)]:
  for hinge in ('left','right'):
   for opened in ('true','false'):
    for section in ('leaf','center','tip','far_tip'):
     for part in range(3):
      name=block+'_'+hinge+('_open_' if opened=='true' else '_closed_')+section+'_'+str(part)
      variant=doc['variants'][f'facing={face},hinge={hinge},open={opened},section={section},segment={part}']
      if cabinet and section=='far_tip':
       inactive=block+'_'+hinge+'_closed_tip_'+str(part)
       require(variant.get('model')=='chao_garden:block/'+inactive and variant.get('y',0)==rotation,'Unused cabinet far state acquired active geometry')
       require(not read(root,ASSET/'models/block'/(inactive+'.json')).get('elements',[]),'Unused cabinet far state is not inert')
       continue
      require(variant.get('model')=='chao_garden:block/'+name and variant.get('y',0)==rotation,'School door state points at wrong owned/handed/rotated model')
      model(root,'chao_garden:block/'+name,domain,{'chao_garden:block/'+key:value for key,value in (override or {}).items()})
      m=(override or {}).get(name) or read(root,ASSET/'models/block'/(name+'.json'));elements=m.get('elements',[]);left=hinge=='left';opened_bool=opened=='true'
      if not cabinet and m.get('chao_door_profile') in ('arch0254','curve0255'):
       import sys
       if str(ROOT/'scripts') not in sys.path:sys.path.insert(0,str(ROOT/'scripts'))
       if m.get('chao_door_profile')=='curve0255':from validate_school_doors_0255 import check_model as check_cut_model
       else:from validate_school_doors_0254 import check_model as check_cut_model
       check_cut_model(m,hinge,opened_bool,section,part,name)
       for key,suffix in (('base',''),('back','_back'),('edge','_edge')):require(m.get('textures',{}).get(key)=='chao_garden:block/school025/'+block+suffix,'Door binds wrong role/front/back material')
       continue
      geometry=[]
      if section=='center' and not opened_bool:geometry=[([0,0,7],[8,16,9]),([8,0,7],[16,16,9])]
      elif section=='leaf':geometry=[(([0,0,8],[2,16,16]) if left else ([14,0,8],[16,16,16])) if opened_bool else (([1,0,7],[16,16,9]) if left else ([0,0,7],[15,16,9]))]
      elif section=='tip' and opened_bool:geometry=[([0,0,0],[2,16,15]) if left else ([14,0,0],[16,16,15])]
      case_geometry=[]
      if cabinet:
       y0=2 if part==0 else 0;y1=14 if part==2 else 16
       geometry=[([a[0],y0,a[2]],[b[0],y1,b[2]]) for a,b in geometry]
       if section!='tip':
        if part==0:case_geometry.append(([0,0,0],[16,2,10]))
        if part==2:case_geometry.append(([0,14,0],[16,16,10]))
        if section=='leaf':case_geometry.append(([0 if left else 15,y0,0],[1 if left else 16,y1,7]))
       require(m.get('textures',{}).get('case')=='chao_garden:block/school025/school_tool_cabinet_case','Utility cabinet lost its separate steel case material')
      require(len(elements)==len(geometry)+len(case_geometry),'Door slice has inactive/hidden/overlapping geometry: '+name)
      for e,(a,b) in zip(elements[len(geometry):],case_geometry):
       require(e['from']==a and e['to']==b and all(f['texture']=='#case' for f in e['faces'].values()),'Permanent cabinet case no longer matches its physical collision parts: '+name)
      for key,suffix in (('base',''),('back','_back'),('edge','_edge')):
       require(m.get('textures',{}).get(key)=='chao_garden:block/school025/'+block+suffix,'Door binds wrong role/front/back material')
      for e,(a,b) in zip(elements,geometry):
       require(e['from']==a and e['to']==b,'Door geometry differs from its actual in-cell collision ownership: '+name)
       leaf=('left' if b[0]<=8 else 'right') if section=='center' else hinge
       sign=1 if leaf=='left' else -1
       # Rotate the original SOUTH front normal along with the physical leaf.
       normals={(-1,0):'west',(1,0):'east'};front=normals[(-sign,0)] if opened_bool else 'south';back=normals[(sign,0)] if opened_bool else 'north'
       for side,data in e['faces'].items():
        if side not in (front,back):require(data['texture']=='#edge','Door artwork stretched onto a thin cut edge');continue
        require(data['texture']==('#base' if side==front else '#back'),'Door front/back material swapped during physical swing')
        uv=data['uv'];require(len(uv)==4 and not data.get('rotation',0),'Door broad face lost its upright UV schema')
        for corner,(u,v) in zip(vertices[side],((uv[0],uv[1]),(uv[2],uv[3]))):
         point=[(a[i],b[i])[corner[i]] for i in range(3)]
         if opened_bool:distance=point[2]+(16 if section=='tip' else 0)-8
         else:
          world_x=point[0]+(0 if section=='center' else -16 if leaf=='left' else 16)
          distance=world_x+15 if leaf=='left' else 31-world_x
         expected_u=distance*16/23
         if side==back:expected_u=16-expected_u
         expected_v=(48-part*16-point[1])/3
         require(abs(u-expected_u)<1e-7 and abs(v-expected_v)<1e-7,'Door picture reversed, repeated or stretched instead of staying on its physical leaf: '+name+' '+side)


def periodic_sky(image):
 w,h=image.size
 for first,last in [(image.crop((0,0,1,h)),image.crop((w-1,0,w,h))),(image.crop((0,0,w,1)),image.crop((0,h-1,w,h)))]:
  require(max(ImageStat.Stat(ImageChops.difference(first,last)).mean[:3])<2,'Ceiling sky is not continuous across its repeating atlas boundary')

def small_mesh_bounds(root,source=None):
 if source is None:source=(Path(root)/'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text()
 declared={identifier:[float(n) for n in values.split(',')] for identifier,values in re.findall(r'case "(school_[a-z_]+)"->new double\[\]\{([^}]+)\}',source)}
 ids={row['id'] for row in read(root,ASSET/'kindergarten_decor025.json')['blocks'] if row['kind']=='SMALL'}
 require(set(declared)==ids,'Native small-object collision bounds omit or invent a finite item')
 for identifier in ids:
  verts=[]
  for e in read(root,ASSET/'models/block'/(identifier+'.json'))['elements']:
   for vertex in itertools.product(*zip(e['from'],e['to'])):
    v=list(vertex)
    if 'rotation' in e:
     r=e['rotation'];i,j={'x':(1,2),'y':(2,0),'z':(0,1)}[r['axis']];a=math.radians(r['angle']);u,w=v[i]-r['origin'][i],v[j]-r['origin'][j]
     v[i]=r['origin'][i]+u*math.cos(a)-w*math.sin(a);v[j]=r['origin'][j]+u*math.sin(a)+w*math.cos(a)
    verts.append(v)
  expected=[min(v[i] for v in verts) for i in range(3)]+[max(v[i] for v in verts) for i in range(3)]
  require(len(declared[identifier])==6 and all(abs(a-b)<1e-5 for a,b in zip(expected,declared[identifier])),'Small-object collision/outline differs from the actual transformed native mesh: '+identifier)


def corner_painting_clearance(root,override=None):
 rooms=(Path(root)/'src/main/java/com/chaogarden/kindergarten/KindergartenRooms025.java').read_text()
 require('p.decor(6,82,7,"school_art_chao_small",Direction.WEST)' in rooms,'Reviewed chamfer painting moved or lost its finite model binding')
 require('return Math.abs(x)<=7&&z>=-8&&z<=10&&Math.abs(x)+Math.max(0,Math.max(-z-5,z-7))<=7;' in rooms and 'if(edge)for(int y=80;y<85;y++)p.decor' in rooms,'Actual finite foyer-wall generation changed; review physical painting clearance')
 state=read(root,ASSET/'blockstates/school_art_chao_small.json')['variants']['facing=west']
 require(state.get('y')==90,'Painting world-clearance guard requires the actual west-facing model transform')
 m=override or read(root,ASSET/'models/block/school_art_chao_small.json')
 def foyer(x,z):return abs(x)<=7 and -8<=z<=10 and abs(x)+max(0,-z-5,z-7)<=7
 walls=[(x,y,z,x+1,y+1,z+1) for x in range(-7,8) for z in range(-8,11) if foyer(x,z) and any(not foyer(x+dx,z+dz) for dx,dz in [(-1,0),(1,0),(0,-1),(0,1)]) for y in range(80,85)]
 require((5,82,9,6,83,10) in walls and (7,82,7,8,83,8) in walls,'Actual chamfer/support-wall cells disappeared from the finite school plan')
 require('p.decor(5,82,8,' not in rooms,'Artwork still replaces a live decorative column segment')
 for e in m.get('elements',[]):
  a,b=e['from'],e['to'];world=(6+(16-b[2])/16,82+a[1]/16,7+a[0]/16,6+(16-a[2])/16,82+b[1]/16,7+b[0]/16)
  require(all(not all(min(world[i+3],wall[i+3])-max(world[i],wall[i])>1e-8 for i in range(3)) for wall in walls),'Actual corner-painting frame/image intersects a real foyer boundary-wall cell')
 require(m.get('elements'),'Corner-painting guard must examine actual frame/image geometry')


SURFACE_KINDS={'COUNTER','DESK','SMALL','DRAPE','CURTAIN','WINDOW','PANEL','COLUMN','CABINET','BED'}
def clinic_bed_exterior(model):
 # A union model may omit internal contact faces, but its OUTER skin must still
 # be closed. Balance oriented geometric edges after splitting T-junctions;
 # this catches a missing cap/back without demanding six faces per patch.
 axes={'north':(2,0,0,1,1),'south':(2,1,0,1,1),'west':(0,0,2,1,-1),'east':(0,1,2,1,-1),'down':(1,0,0,2,-1),'up':(1,1,0,2,-1)}
 lines={};faces=[];volume=0;elements=model.get('elements',[])
 require(elements and all(len(e.get('faces',{}))==1 and 'rotation' not in e for e in elements),'Clinic bed must use the reviewed exposed union surface patches')
 for e in elements:
  a,b=e['from'],e['to'];require(all(a[i]<b[i] for i in range(3)),'Clinic bed has a degenerate solid patch')
  name,data=next(iter(e['faces'].items()));axis,end,u,v,cross=axes[name];normal=1 if end else -1;plane=(b if end else a)[axis]
  require('cullface' not in data,'Exposed clinic bed patch acquired world-block culling')
  area=(b[u]-a[u])*(b[v]-a[v]);volume+=normal*plane*area/3
  faces.append((axis,plane,a[u],b[u],a[v],b[v]))
  points=[]
  for cu,cv in ((a[u],a[v]),(b[u],a[v]),(b[u],b[v]),(a[u],b[v])):
   p=[0,0,0];p[axis]=plane;p[u]=cu;p[v]=cv;points.append(p)
  if cross!=normal:points.reverse()
  for p,q in zip(points,points[1:]+points[:1]):
   along=next(i for i in range(3) if p[i]!=q[i]);key=(along,)+tuple(p[i] for i in range(3) if i!=along);sign=1 if q[along]>p[along] else -1
   events=lines.setdefault(key,{});lo,hi=sorted((p[along],q[along]));events[lo]=events.get(lo,0)+sign;events[hi]=events.get(hi,0)-sign
 for events in lines.values():
  balance=0;keys=sorted(events)
  for i,p in enumerate(keys):
   balance+=events[p]
   if i<len(keys)-1:require(balance==0,'Clinic bed exterior has an open seam/back/cap')
  require(balance==0,'Clinic bed exterior has an unmatched edge')
 for i,a in enumerate(faces):
  for b in faces[i+1:]:
   if a[:2]==b[:2]:require(min(a[3],b[3])-max(a[2],b[2])<=1e-8 or min(a[5],b[5])-max(a[4],b[4])<=1e-8,'Clinic bed has overlapping coplanar outer skins')
 require(abs(volume-3144)<.0001,'Clinic bed closed skin lost or duplicated a complete frame/bedding component')
 require([[min(e['from'][i] for e in elements),max(e['to'][i] for e in elements)] for i in range(3)]==[[0,16],[0,15],[0,32]],'Clinic bed exterior no longer matches its native collision envelope')

def clinic_curtain_variant(root,variants,domain,base='school_green_room_curtain_tie'):
 for face,turn in {'south':0,'west':90,'north':180,'east':270}.items():
  identifier=base+('_east' if face=='east' else '')
  require(variants['facing='+face]=={'model':'chao_garden:block/'+identifier,**({'y':turn} if turn else {})},'Clinic curtain facing escaped the finite reviewed reflection')
 source=read(root,ASSET/'models/block'/(base+'.json'));mirror=read(root,ASSET/'models/block'/(base+'_east.json'))
 require(source['textures']==mirror['textures'] and len(source['elements'])==len(mirror['elements']),'Clinic curtain reflection lost native materials or geometry')
 for a,b in zip(source['elements'],mirror['elements']):
  require(a['from'][:2]==b['from'][:2] and a['to'][:2]==b['to'][:2] and b['from'][2]==16-a['to'][2] and b['to'][2]==16-a['from'][2],'Clinic curtain reflection moved its hanging tie to an unrelated physical surface')
  require(b['faces']['north']==a['faces']['south'] and b['faces']['south']==a['faces']['north'] and all(b['faces'][f]==a['faces'][f] for f in ('up','down','west','east')),'Clinic curtain reflection lost physical face/UV ownership')
 model(root,'chao_garden:block/'+base+'_east',domain)

def decor_surface_aspects(root,overrides=None):
 catalog=read(root,ASSET/'kindergarten_decor025.json');images={}
 for row in catalog['blocks']:
  if row['kind'] not in SURFACE_KINDS:continue
  identifier=row['id'];m=(overrides or {}).get(identifier) or read(root,ASSET/'models/block'/(identifier+'.json'));tex=m.get('textures',{})
  if identifier=='school_white_exam_bed':clinic_bed_exterior(m)
  def binding(value):
   while value.startswith('#'):value=tex[value[1:]]
   return value
  for element in m.get('elements',[]):
   faces=element.get('faces',{})
   # Only a wall picture/seal is intentionally one-sided. Its wood frame and every solid furnishing have backs.
   require(set(faces)=={'north','south','east','west','up','down'} or row['kind']=='PANEL' and set(faces)=={'south'} or identifier=='school_white_exam_bed' and len(faces)==1,'Solid school furniture lost a back/cap face: '+identifier)
   a,b=element['from'],element['to'];dx,dy,dz=[b[i]-a[i] for i in range(3)]
   for side,data in faces.items():
    texture=binding(data['texture'])
    if texture not in images:
     with Image.open(Path(root)/ASSET/'textures'/(texture.split(':',1)[1]+'.png')) as im:
      rgba=im.convert('RGBA');patterned=any(lo!=hi for lo,hi in rgba.getextrema()[:3]);images[texture]=(im.width,im.height,patterned)
    w,h,patterned=images[texture]
    if not patterned:continue # A uniform material has no visible aspect; geometry/closed-face guards still apply.
    u0,v0,u1,v1=data.get('uv',[0,0,16,16]);pw,ph=(dx,dy) if side in ('north','south') else (dz,dy) if side in ('east','west') else (dx,dz)
    require(pw>0 and ph>0 and abs(u1-u0)>0 and abs(v1-v0)>0,'Patterned school face has degenerate physical/UV area')
    physical=pw/ph;sampled=abs(u1-u0)*w/(abs(v1-v0)*h)
    require(abs(physical/sampled-1)<1e-4,'Patterned school surface stretches texels: '+identifier+' '+side+' physical='+str(physical)+' sampled='+str(sampled))
 # Material contracts stop a uniform fallback from silently bypassing the aspect checks.
 for identifier in ('school_red_counter','school_green_counter','school_principal_desk'):
  m=(overrides or {}).get(identifier) or read(root,ASSET/'models/block'/(identifier+'.json'));tex=m['textures'];top,body=m['elements'][:2]
  for key,suffix in [('front','body_front'),('side','body_side'),('rimfront','rim_front'),('rimside','rim_side')]:require(tex.get(key)=='chao_garden:block/school025/'+identifier+'_'+suffix,'Furniture surface lost its reviewed proportioned material: '+identifier+' '+key)
  for side,material in [('up','#base'),('down','#wood'),('north','#rimfront'),('south','#rimfront'),('west','#rimside'),('east','#rimside')]:require(top['faces'][side]['texture']==material,'Countertop/rim binding changed: '+identifier)
  for side in ('north','south'):require(body['faces'][side]['texture']=='#front','Counter body front/back uses another surface artwork')
  require(m['elements'][2]['faces']['west']['texture']=='#side' and m['elements'][3]['faces']['east']['texture']=='#side','Partitioned counter side uses stretched front artwork')
 for identifier in ('school_principal_desk','school_globe'):
  m=(overrides or {}).get(identifier) or read(root,ASSET/'models/block'/(identifier+'.json'))
  require(m['textures'].get('earth')=='chao_garden:block/school025/earth' and sum(f['texture']=='#earth' for e in m['elements'] for f in e['faces'].values())==30,'Globe continents were replaced or lost a stepped sphere face')
  (cx,cy,cz),r=(([16.5,14,8.5],2.2) if identifier=='school_principal_desk' else ([8,-2.5,8],2.1))
  for e in m['elements']:
   a,b=e['from'],e['to']
   for face,data in e['faces'].items():
    if data['texture']!='#earth':continue
    if face in ('north','south'):
     u0=(a[0]-cx+r)/(2*r)*16;u1=(b[0]-cx+r)/(2*r)*16;v0=(cy+r-b[1])/(2*r)*16;v1=(cy+r-a[1])/(2*r)*16
     expected=[u1,v0,u0,v1] if face=='north' else [u0,v0,u1,v1]
    elif face in ('east','west'):
     u0=(a[2]-cz+r)/(2*r)*16;u1=(b[2]-cz+r)/(2*r)*16;v0=(cy+r-b[1])/(2*r)*16;v1=(cy+r-a[1])/(2*r)*16
     expected=[u1,v0,u0,v1] if face=='east' else [u0,v0,u1,v1]
    else:
     u0=(a[0]-cx+r)/(2*r)*16;u1=(b[0]-cx+r)/(2*r)*16;v0=(a[2]-cz+r)/(2*r)*16;v1=(b[2]-cz+r)/(2*r)*16
     expected=[u0,v1,u1,v0] if face=='down' else [u0,v0,u1,v1]
    require(all(abs(x-y)<1e-5 for x,y in zip(data['uv'],expected)),'Globe band map does not continue at the actual sphere coordinates')

 for row in catalog['blocks']:
  if row['kind'] not in ('DRAPE','CURTAIN'):continue
  m=(overrides or {}).get(row['id']) or read(root,ASSET/'models/block'/(row['id']+'.json'));tex=m['textures']['base']
  with Image.open(Path(root)/ASSET/'textures'/(tex.split(':',1)[1]+'.png')) as image:
   rgba=image.convert('RGBA');require(rgba.tobytes()==rgba.crop((0,0,rgba.width,1)).tobytes()*rgba.height,'Curtain material still contains a baked gold band misaligned with its actual3D tie')
  tied=row['kind']=='CURTAIN' and row['id'].endswith('_tie') or row['kind']=='DRAPE' and not row['id'].endswith('_plain')
  if tied:
   gold=[e for e in m['elements'] if all(f['texture']=='#gold' for f in e['faces'].values())]
   require(len(gold)==2,'Curtain has a missing/duplicate tie or tassel');band,tassel=gold
   require(tassel['to'][2]>band['to'][2] and abs(tassel['to'][1]-band['from'][1])<1e-7,'Curtain tassel is behind the cloth or disconnected from its front band')
 principal=(overrides or {}).get('school_principal_desk') or read(root,ASSET/'models/block/school_principal_desk.json')
 require(all(e['from'][1]-10.3>=.04999 for e in principal['elements'] if any(f['texture']=='#black' for f in e['faces'].values())),'Principal paper ink has insufficient depth separation')
 altar=(overrides or {}).get('school_fortune_altar') or read(root,ASSET/'models/block/school_fortune_altar.json')
 card=[e for e in altar['elements'] if e['from'][0]==12 and e['from'][1]==12]
 require(len(card)==1 and card[0]['from'][2]-10.3>=.04999,'Fortune card mark has insufficient depth separation')


def school_boundary_contract(root,source=None):
 rooms=source if source is not None else (Path(root)/'src/main/java/com/chaogarden/kindergarten/KindergartenRooms025.java').read_text()
 for plane in ('for(int x:new int[]{-5,5})for(int z=18;z<=51;z++)for(int y=80;y<103;y++)p.barrierAir(x,y,z);',
               'for(int x=-5;x<=5;x++)for(int y=80;y<103;y++)p.barrierAir(x,y,51);',
               'for(int x=-5;x<=5;x++)for(int z=18;z<=51;z++)p.barrierAir(x,102,z);',
               'for(int x=-5;x<=5;x++)for(int y=86;y<103;y++)p.barrierAir(x,y,18);'):
  require(plane in rooms,'Public school approach volume lost a side/end/cap/high-front closure')
 require('if(blocks.getOrDefault(q,Blocks.AIR.defaultBlockState()).isAir())put(x,y,z,Blocks.BARRIER);' in rooms,'Boundary overwrites authored hedge/pillar/roof blocks instead of filling only AIR')


def coplanar_furnishing_faces(root,overrides=None):
 ids=('school_red_counter','school_green_counter','school_principal_desk','school_purple_room_curtain_tie','school_green_room_curtain_tie','school_purple_drape','school_green_drape')
 axes={'north':(2,0,(0,1)),'south':(2,1,(0,1)),'west':(0,0,(2,1)),'east':(0,1,(2,1)),'down':(1,0,(0,2)),'up':(1,1,(0,2))}
 for identifier in ids:
  m=(overrides or {}).get(identifier) or read(root,ASSET/'models/block'/(identifier+'.json'));elements=m['elements'];bindings=m['textures']
  def material(face):
   value=face['texture']
   while value.startswith('#'):value=bindings[value[1:]]
   return value
  for i,a in enumerate(elements):
   for b in elements[i+1:]:
    for side,(axis,end,flat) in axes.items():
     if side not in a['faces'] or side not in b['faces']:continue
     if material(a['faces'][side])==material(b['faces'][side]):continue
     plane_a=a['to' if end else 'from'][axis];plane_b=b['to' if end else 'from'][axis]
     if abs(plane_a-plane_b)>1e-7:continue
     overlap=all(min(a['to'][j],b['to'][j])-max(a['from'][j],b['from'][j])>1e-7 for j in flat)
     require(not overlap,'Different visible materials fight on one same-normal coplanar face: '+identifier+' '+side)

def door_badge_clearance(root,overrides=None):
 badge_colors={(214,206,156),(145,206,202),(233,211,114),(57,156,145),(49,90,96)}
 for role in ('principal','classroom','health','fortune','exit'):
  identifier='school_door_'+role
  if identifier in (overrides or {}):image=(overrides or {})[identifier]
  else:image=Image.open(Path(root)/ASSET/'textures/block/school025'/(identifier+'.png')).convert('RGBA')
  frame_doc=read(root,ASSET/'models/block'/('school_arch_'+role+'.json'));frame=frame_doc['elements']
  art_left,art_right=(-14.75,30.75) if frame_doc.get('chao_door_profile')=='curve0255' else (-14.25,30.25)
  pixels=[(x+.5,y+.5) for y in range(image.height) for x in range(image.width) if image.getpixel((x,y))[:3] in badge_colors]
  require(pixels,'Actual door badge artwork disappeared')
  for u,v in pixels:
   y=80+(1-v/image.height)*3
   for x in (art_left/16+u/image.width*23/16,art_right/16-u/image.width*23/16):
    require(not any(e['from'][0]/16<x<e['to'][0]/16 and 81+e['from'][1]/16<y<81+e['to'][1]/16 for e in frame),'Closed door badge lies behind the actual projected stepped arch: '+identifier)
  require(all(image.height/3<v<image.height*2/3 for u,v in pixels),'Door badge crosses a native segment/lighting seam')


def school_door_prediction_contract(root,source=None):
 code=source if source is not None else (Path(root)/'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text()
 begin=code.index('    @Override protected InteractionResult useWithoutItem',code.index('class SchoolDoorBlock'));end=code.index('    @Override public BlockState getStateForPlacement',begin);method=code[begin:end]
 compact=re.sub(r'\s+','',method)
 gate='BlockPos center=pairCenter(s,pos); if(!complete(level,pos,s)){ var migrated=migrateLegacyTips(level,pos,s); if(migrated!=null)center=migrated; else if(!expandLegacy(level,pos,s))return InteractionResult.PASS; }'
 require(re.sub(r'\s+','',gate) in compact,'Door predicts a partial/unloaded assembly without conservative migration/expansion or loses the migrated pair center')
 require('int flags=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_IMMEDIATE;' in method,'Door pair lost immediate native render invalidation')
 operation='pairStates(this,center,s.getValue(FACING),open).forEach((at,state)->level.setBlock(at,state,flags));'
 require(operation in method and method.index(operation)<method.index('if(!level.isClientSide)'),'Whole-pair client prediction was accidentally made server-only')
 expand=code[code.index('    private boolean expandLegacy',code.index('class SchoolDoorBlock')):code.index('    private Map<BlockPos,BlockState> legacyTipStates',code.index('class SchoolDoorBlock'))]
 require('public static final int PART_COUNT=21,CABINET_PART_COUNT=15;' in code and 'if(!there.equals(part.getValue()))return false;' in expand and 'else if(!there.isAir())return false;' in expand and expand.index('else if(!there.isAir())return false;')<expand.index('level.setBlock('),'Native21-part ornate/15-part cabinet ownership or obstruction-safe six-root legacy expansion was lost')
 migration=code[code.index('    private BlockPos migrateLegacyTips',code.index('class SchoolDoorBlock')):begin]
 old_check='if(!matches(level,old,null))continue;'
 free_check='for(var part:current.entrySet())if(!level.hasChunkAt(part.getKey())||!old.containsKey(part.getKey())&&!level.getBlockState(part.getKey()).isAir()){free=false;break;}'
 require(old_check in migration and free_check in migration and 'if(!free)continue;' in migration and max(migration.index(old_check),migration.index(free_check))<migration.index('level.setBlock('),'Saved-tip migration mutates an incomplete old pair or overwrites occupied/unloaded forward cells')
 require('if(toolCabinet||state.getValue(SECTION)==DoorSection.FAR_TIP)return null;' in migration and 'java.util.List.of(center,center.relative(state.getValue(FACING),2))' in code,'Saved-tip migration changes cabinet ownership or resolves an old rear tip to the wrong center')
 require('level.hasChunkAt(part.getKey())||!level.getBlockState(part.getKey()).equals(part.getValue())' in code,'Assembly matching accepts an unloaded or foreign saved member')
 require('REMOVING.set(true);' in migration and '}finally{REMOVING.set(false);}' in migration and migration.index('REMOVING.set(true);')<migration.index('level.setBlock('),'Saved-tip migration can recursively destroy the pair or leave removal suppression enabled')
 require('for(var at:old.keySet())if(!current.containsKey(at))level.setBlock(' in migration and 'for(var part:current.entrySet())if(!old.containsKey(part.getKey()))level.setBlock(part.getKey(),part.getValue(),flags);' in migration and 'return center;' in migration,'Saved-tip migration erases shared roots or fails to return the preserved pair center')
 require('if(!level.isClientSide)level.playSound(' in method,'Native door sound is no longer server-authoritative only')
 require('PacketDistributor' not in method and 'Minecraft.getInstance' not in method,'Door bypasses ordinary native interaction/prediction acknowledgement')


def school_mesh_contract(root,overrides=None):
 def doc(identifier):return (overrides or {}).get(identifier) or read(root,ASSET/'models/block'/(identifier+'.json'))
 # The casement and glazing must cover the whole actual2x2 cutout, including all four formerly open corners.
 window=doc('school_round_window');elements=window['elements']
 for x in [i/2+.25 for i in range(64)]:
  for y in [i/2+.25 for i in range(64)]:
   require(any(e['from'][0]<=x<=e['to'][0] and e['from'][1]<=y<=e['to'][1] and e['to'][2]>e['from'][2] for e in elements),'Window leaves a visible uncovered part of its actual two-by-two wall opening')
 # A closed upright book needs covers, pages and its visible back/spine. The old solid overlapping boxes hid the pages.
 principal=doc('school_principal_desk')['elements']
 for x in (-2,0):
  require(any(e['from']==[x+.15,10,9.8] and e['to']==[x+1.35,15,10] and all(f['texture'] in ('#blue','#red') for f in e['faces'].values()) for e in principal),'Principal upright book is missing its colored rear spine')
 stack=doc('school_book_stack')['elements']
 require(len(stack)==12,'Stacked books require three separate four-part cover/page/spine assemblies')
 # Center the complete three-part word on the same axis as the frozen Chao seal and entrance.
 spans=[]
 for x,part in [(-2,'left'),(0,'middle'),(2,'right')]:
  m=doc('school_facade_title_'+part);e=m['elements'][0];spans.append((x+e['from'][0]/16,x+e['to'][0]/16))
 for part in ('left','middle','right'):
  e=doc('school_facade_title_'+part)['elements'][1]
  with Image.open(Path(root)/ASSET/'textures/block/school025'/('school_facade_title_'+part+'.png')) as image:require(abs((e['to'][0]-e['from'][0])/(e['to'][1]-e['from'][1])-image.width/image.height)<.01,'Kindergarten sign text is stretched relative to its source image')
 require(all(abs(spans[i][1]-spans[i+1][0])<1e-9 for i in range(2)) and abs((spans[0][0]+spans[-1][1])/2-.5)<1e-9,'Kindergarten title halves are gapped or off the entrance/emblem centerline')
 guard=doc('school_altar_guard');require(guard.get('elements')==[],'Invisible altar collision screen acquired visible geometry')
 # Native element bounds and positive thickness are checked across the finite actual scenery catalog.
 catalog=read(root,ASSET/'kindergarten_decor025.json')
 for row in catalog['blocks']:
  for e in doc(row['id']).get('elements',[]):
   require(all(-16<=a<b<=32 for a,b in zip(e['from'],e['to'])),'Scenery element is flat, inverted or outside native model bounds: '+row['id'])


def dimensions(root,fixture_override=None):
 expected={}
 for room in ROOMS:
  d=read(root,DATA/'dimension'/(room+'.json'));t=read(root,DATA/'dimension_type'/(room+'.json'));expected['chao_garden:'+room]=d
  require(d.get('type')=='chao_garden:'+room,'Dimension type mismatch: '+room)
  generator=d.get('generator',{});settings=generator.get('settings',{})
  require(generator.get('type')=='minecraft:flat' and settings.get('layers')==[{'height':1,'block':'minecraft:air'}],'Scene generator changed from isolated void: '+room)
  require(settings.get('features') is False and settings.get('lakes') is False and settings.get('structure_overrides')==[],'Unrequested terrain/structure generation: '+room)
  require(settings.get('biome')==('chao_garden:dark_twilight' if room=='dark_garden' else ('chao_garden:hero_daylight' if room=='hero_garden' else ('minecraft:the_void' if room=='hub' else 'chao_garden:scenic_daylight'))),'Scene biome mismatch: '+room)
  require(t.get('min_y')==0 and t.get('height')==256 and t.get('coordinate_scale')==1,'Scene dimension coordinate envelope changed: '+room)
  require(t.get('natural') is False and t.get('has_raids') is False and t.get('bed_works') is False,'Scene dimension changes ordinary world progression: '+room)
  require(t.get('effects')==('chao_garden:dark_sky' if room=='dark_garden' else ('chao_garden:hero_sky' if room=='hero_garden' else ('minecraft:the_end' if room=='hub' else 'minecraft:overworld'))) and t.get('has_skylight')==(room!='hub') and t.get('has_ceiling') is False,'Wrong scene sky/ceiling: '+room)
  require(t.get('fixed_time')==(18000 if room=='hub' else 6000),'Scene reference lighting changed: '+room)
  require(t.get('ambient_light')==(.30 if room=='dark_garden' else .45 if room=='hub' else .12),'Authored realm ambient light changed: '+room)
 biome=read(root,DATA/'worldgen/biome/scenic_daylight.json')
 require(biome.get('spawners')=={} and biome.get('spawn_costs')=={} and biome.get('creature_spawn_probability')==0,'Scenic biome allows native wildlife')
 require(biome.get('carvers')=={} and biome.get('features')==[] and biome.get('has_precipitation') is False,'Scenic biome generates outside terrain/weather')
 hero_biome=read(root,DATA/'worldgen/biome/hero_daylight.json')
 require(hero_biome.get('spawners')=={} and hero_biome.get('spawn_costs')=={} and hero_biome.get('creature_spawn_probability')==0,'Hero biome allows native wildlife')
 require(hero_biome.get('carvers')=={} and hero_biome.get('features')==[] and hero_biome.get('has_precipitation') is False,'Hero biome generates outside terrain/weather')
 dark_biome=read(root,DATA/'worldgen/biome/dark_twilight.json')
 require(dark_biome.get('spawners')=={} and dark_biome.get('spawn_costs')=={} and dark_biome.get('creature_spawn_probability')==0,'Dark biome allows native wildlife')
 require(dark_biome.get('carvers')=={} and dark_biome.get('features')==[] and dark_biome.get('has_precipitation') is False,'Dark biome generates outside terrain/weather')
 legacy=Path(root)/'src/main/resources/data/minecraft/worldgen/world_preset/flat.json'
 require(not legacy.exists(),'Dedicated test flat preset leaked into normal production resources')
 require(not list((Path(root)/'src/main/resources/data/minecraft/worldgen/world_preset').glob('*.json')),'Scene data overrides an ordinary vanilla world preset')
 fixture=fixture_override if fixture_override is not None else read(root,Path('src/gameTestDimensions/resources/data/minecraft/worldgen/world_preset/flat.json'))
 vanilla=read(root,Path('art/fixtures/vanilla_flat_preset_1211.json'))
 require(set(fixture.get('dimensions',{}))==set(vanilla['dimensions'])|set(expected),'Dedicated flat fixture dimension domain changed')
 for key,value in vanilla['dimensions'].items():require(fixture['dimensions'][key]==value,'GameTest preset altered vanilla dimension: '+key)
 for key,value in expected.items():require(fixture['dimensions'][key]==value,'Dev dimension drifted from production data: '+key)
 return expected


def validate(root):
 used_textures.clear()
 school=scenery_items(root);internal=lobby_blocks(root);atlases=scenery_atlases(root)
 require(not school&internal,'School and internal scenery domains overlap')
 catalog=read(root,ASSET/'kindergarten_decor025.json');manifest=read(root,Path('art/lobby_garden_manifest_025.json'))
 require(manifest['version']=='0.25.0' and manifest['shader']=='scene_unlit','Lobby scene manifest version/shader mismatch')
 require(catalog['texturePrefix']=='block/school025/','Kindergarten material prefix changed')
 require({row['id'] for row in catalog['blocks'] if row['kind']=='ARCH'}==set(ARCH_PALETTES),'Reviewed room arch role domain changed')
 domain={'block/'+b for b in school|internal}|{'item/'+b for b in school}
 domain|={'block/'+block+'_tile_'+str(tile) for block,meta in atlases.items() for tile in range(meta['usedTiles'])}
 doors={'school_door_'+role for role in DOOR_ROLES}|{'school_tool_cabinet'}
 require({row['id'] for row in catalog['blocks'] if row['kind']=='DOOR'}==doors,'Reviewed school door registry domain changed')
 domain|={'block/'+block+'_'+hinge+'_'+opened+'_'+section+'_'+str(part) for block in doors for hinge in ('left','right') for opened in ('open','closed') for section in ('leaf','center','tip','far_tip') for part in range(3)}
 columns={'school_hall_column','school_twisted_column'}
 require({row['id'] for row in catalog['blocks'] if row['kind']=='COLUMN'}==columns,'Reviewed Ionic column BlockItem domain changed')
 domain|={'block/'+block+'_'+part for block in columns for part in ('base','shaft','capital')}
 domain|={'block/'+base+'_east' for base in ('school_green_room_curtain_tie','school_purple_room_curtain_tie')}
 for block in sorted(school|internal):
  blockstate=read(root,ASSET/'blockstates'/(block+'.json'));variants=blockstate.get('variants',{})
  if block in atlases:
   composite_states(block,atlases[block],blockstate);composite_models(root,block,atlases[block],domain)
  elif block in doors:door_contract(root,block,blockstate,domain)
  elif block in columns:
   require(set(variants)=={'facing='+face+',part='+part for face in ('north','south','east','west') for part in ('base','shaft','capital')},'Ionic member/facing state domain changed: '+block)
   for face,turn in {'south':0,'west':90,'north':180,'east':270}.items():
    for part in ('base','shaft','capital'):
     entry=variants['facing='+face+',part='+part]
     require(entry=={'model':'chao_garden:block/'+block+'_'+part,**({'y':turn} if turn else {})},'Ionic column state does not bind its finite actual member model')
   for part in ('base','shaft','capital'):model(root,'chao_garden:block/'+block+'_'+part,domain)
  elif block in ('school_green_room_curtain_tie','school_purple_room_curtain_tie'):
   require(set(variants)=={'facing='+face for face in ('north','south','east','west')},'Clinic curtain facing domain changed')
   clinic_curtain_variant(root,variants,domain,block)
  else:
   require(set(variants)==({'facing='+face for face in ('north','south','east','west')} if block in school else {''}),'Scene blockstate variants mismatch: '+block)
   for entry in variants.values():require(entry.get('model')=='chao_garden:block/'+block,'Scene blockstate model points outside its block: '+block)
  model(root,'chao_garden:block/'+block,domain)
  if block in ARCH_PALETTES:arch_palette(block,read(root,ASSET/'models/block'/(block+'.json')))
  item_path=Path(root)/ASSET/'models/item'/(block+'.json')
  if block in school:
   require(item_path.is_file(),'Reusable school decoration has no item model: '+block);model(root,'chao_garden:item/'+block,domain)
  else:require(not item_path.exists(),'Internal scenery became an obtainable item model: '+block)
 school_mesh_contract(root);small_mesh_bounds(root);corner_painting_clearance(root);decor_surface_aspects(root);school_boundary_contract(root);coplanar_furnishing_faces(root);door_badge_clearance(root);school_door_prediction_contract(root)
 with Image.open(Path(root)/ASSET/'textures/block/school025/school_cloud_composite.png') as image:periodic_sky(image)
 # Block/item registries and generated catalogs are bound by scene_resource_contract_025.
 # Also reject resource routes that could expose any internal id as a loot/recipe item.
 folders=[p for namespace in (Path(root)/'src/main/resources/data').iterdir() if namespace.is_dir() for p in (namespace/'recipe',namespace/'loot_table') if p.is_dir()]
 for folder in folders:
  for path in folder.rglob('*.json'):
   def item_refs(node):
    if isinstance(node,dict):
     for key,value in node.items():
      if key in ('id','name','item') and isinstance(value,str) and value.startswith('chao_garden:'):yield value.split(':',1)[1]
      yield from item_refs(value)
    elif isinstance(node,list):
     for value in node:yield from item_refs(value)
   require(not internal.intersection(item_refs(json.loads(path.read_text()))),'Internal scene block obtainable through '+str(path.relative_to(root)))
 textures=manifest.get('render_textures',[])
 require(len(textures)==len(set(textures)) and textures,'Duplicate/empty renderer texture manifest')
 # Later scenery registries (including lobby stair particles) also consume
 # these sprites. Inspect every native block binding before checking the atlas.
 for path in (Path(root)/ASSET/'models/block').glob('*.json'):
  for value in json.loads(path.read_text()).get('textures',{}).values():
   if value.startswith('chao_garden:scene/'):texture(root,value)
 native_scene={value.split(':',1)[1] for value in used_textures if value.startswith('chao_garden:scene/')}
 atlas_contract(native_scene,manifest.get('block_atlas_textures',[]),read(root,Path('src/main/resources/assets/minecraft/atlases/blocks.json')))
 # Bind the native models and the renderer's explicit scene texture constants to
 # the catalog, so an unreferenced alpha PNG cannot masquerade as the glass used.
 renderer=(Path(root)/'src/main/java/com/chaogarden/client/LobbySceneRenderer025.java').read_text()
 for name in re.findall(r'texture\("([a-z0-9_]+)"\)',renderer):texture(root,'chao_garden:scene/'+name)
 referenced={value.split(':',1)[1] for value in used_textures if value.startswith('chao_garden:scene/')}
 require(set(textures)==referenced,'Scene texture manifest differs from referenced model/renderer materials')
 require(re.search(r'GLASS_TEX\s*=\s*texture\("lobby_glass"\)',renderer) is not None,'Renderer glass texture binding missing')
 require(re.search(r'FLOOR\.draw\(poses,\s*buffers,\s*GLASS_TEX,\s*0\)',renderer) is not None,'Lobby floor does not use the translucent glass material')
 for value in textures:texture(root,'chao_garden:'+value)
 material=manifest.get('glass_material',{})
 require(material=={'texture':'scene/lobby_glass','rgba':[21,37,68,59],'render_mode':'translucent','depth_write':False},'Lobby transparent material contract changed')
 require(material['texture'] in textures,'Glass material absent from renderer texture manifest')
 with Image.open(Path(root)/ASSET/'textures'/(material['texture']+'.png')) as image:alpha(image,'glass',material['rgba'])
 for name in ('garden_palm_frond','kindergarten_portal_sign','lobby_exit_pad','lobby_exit_sign'):
  with Image.open(Path(root)/ASSET/'textures/scene'/(name+'.png')) as image:alpha(image,name)
 shader=Path(root)/ASSET/'shaders/core/scene_unlit'
 shader_contract(json.loads(shader.with_suffix('.json').read_text()),shader.with_suffix('.vsh').read_text(),shader.with_suffix('.fsh').read_text())
 expected=dimensions(root)
 return school,internal,domain,expected


def rejects(action,label):
 try:action()
 except (AssertionError,FileNotFoundError,KeyError):return
 raise AssertionError('Malformed fixture accepted: '+label)

def negative_fixtures(root,domain):
 # Minimal standalone catalogs prove source/catalog identity checking, without
 # ever rewriting the actual project while another agent compiles or renders it.
 with tempfile.TemporaryDirectory(prefix='chao025-invalid-scenes-') as directory:
  target=Path(directory)
  files=[Path('art/lobby_garden_manifest_025.json'),ASSET/'kindergarten_decor025.json',
   Path('src/main/java/com/chaogarden/hub/LobbyDecor025.java'),Path('src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java')]
  for file in files:(target/file).parent.mkdir(parents=True,exist_ok=True);(target/file).write_bytes((Path(root)/file).read_bytes())
  bad=read(root,files[0]);bad['blocks'][0]['item']=True;(target/files[0]).write_text(json.dumps(bad));rejects(lambda:lobby_blocks(target),'obtainable internal block')
  bad=read(root,files[1]);bad['blocks'][0]['kind']='BED';(target/files[1]).write_text(json.dumps(bad));rejects(lambda:scenery_items(target),'school registry kind drift')
 broken=read(root,ASSET/'models/block/garden_palm_trunk.json');broken['elements'][0]['faces']['north']['texture']='#missing'
 rejects(lambda:model(root,'chao_garden:block/garden_palm_trunk',domain,{'chao_garden:block/garden_palm_trunk':broken}),'unbound model material')
 broken=read(root,ASSET/'models/block/garden_turf.json');broken['textures']['all']='chao_garden:scene/does_not_exist_025'
 rejects(lambda:model(root,'chao_garden:block/garden_turf',domain,{'chao_garden:block/garden_turf':broken}),'missing referenced texture')
 rejects(lambda:alpha(Image.new('RGBA',(8,8),(21,37,68,255)),'glass'),'opaque glass atlas')
 broken=read(root,ASSET/'shaders/core/scene_unlit.json');broken['samplers']=[]
 rejects(lambda:shader_contract(broken,'',''),'missing shader sampler')
 broken=read(root,Path('src/gameTestDimensions/resources/data/minecraft/worldgen/world_preset/flat.json'));broken['dimensions']['minecraft:the_nether']=deepcopy(broken['dimensions']['chao_garden:garden'])
 rejects(lambda:dimensions(root,broken),'dev fixture overwrote vanilla Nether')
 atlas=read(root,Path('src/main/resources/assets/minecraft/atlases/blocks.json'));atlas['sources']=atlas['sources'][:-1]
 native=set(read(root,Path('art/lobby_garden_manifest_025.json'))['block_atlas_textures'])
 rejects(lambda:atlas_contract(native,list(native),atlas),'native scene material missing from block atlas')
 atlases=scenery_atlases(root);block='school_lobby_composite';meta=atlases[block]
 broken=read(root,ASSET/'blockstates'/(block+'.json'));broken['variants']['facing=south,tile=127']['model']='chao_garden:block/'+block+'_tile_127'
 rejects(lambda:composite_states(block,meta,broken),'composite unused tile bypasses fallback')
 with tempfile.TemporaryDirectory(prefix='chao025-invalid-atlas-') as directory:
  target=Path(directory)
  for file in (ASSET/'kindergarten_decor025.json',Path('src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java')):
   (target/file).parent.mkdir(parents=True,exist_ok=True);(target/file).write_bytes((Path(root)/file).read_bytes())
  broken=read(root,ASSET/'kindergarten_decor025.json');next(row for row in broken['blocks'] if row['id']==block)['atlas']['usedTiles']=129
  (target/ASSET/'kindergarten_decor025.json').write_text(json.dumps(broken));rejects(lambda:scenery_atlases(target),'composite metadata exceeds real registry states')
 broken=read(root,ASSET/'models/block/school_arch_classroom.json');broken['textures']['base']='chao_garden:block/school025/cream'
 rejects(lambda:arch_palette('school_arch_classroom',broken),'arch valid-but-wrong cream base fallback')
 broken=read(root,ASSET/'models/block/school_arch_exit.json')
 for element in broken['elements']:
  for edge in ('from','to'):element[edge][2]+=16
 rejects(lambda:arch_palette('school_arch_exit',broken),'arch mesh keeps old depth after its anchor moved forward')
 broken=read(root,ASSET/'models/block/school_arch_health.json');next(iter(broken['elements'][0]['faces'].values()))['texture']='#cream'
 rejects(lambda:arch_palette('school_arch_health',broken),'arch jamb face bypasses correct role base')
 broken=read(root,ASSET/'models/block/school_arch_fortune.json');infill=next(e for e in broken['elements'] if e['from'][2]==-16 and e['to'][2]==0)
 for face in infill['faces'].values():face['texture']='#base'
 rejects(lambda:arch_palette('school_arch_fortune',broken),'arch cream corner infill incorrectly recoloured')
 broken=read(root,ASSET/'blockstates/school_door_classroom.json');broken['variants'].pop('facing=south,hinge=right,open=false,section=leaf,segment=2')
 rejects(lambda:door_contract(root,'school_door_classroom',broken,domain),'door missing upper mirrored leaf state')
 block='school_door_exit';name=block+'_right_closed_leaf_1';broken=read(root,ASSET/'models/block'/(name+'.json'));broken['elements'][0]['faces']['south']['uv']=[0,16/3,16,32/3]
 rejects(lambda:door_contract(root,block,read(root,ASSET/'blockstates'/(block+'.json')),domain,{name:broken}),'door right leaf loses mirrored seam knob')
 name=block+'_left_open_tip_0';broken=read(root,ASSET/'models/block'/(name+'.json'));broken['elements'][0]['to'][2]-=.5
 rejects(lambda:door_contract(root,block,read(root,ASSET/'blockstates'/(block+'.json')),domain,{name:broken}),'open door art differs from actual swinging collision')
 broken=read(root,ASSET/'models/block/school_round_window.json');broken['elements']=[e for e in broken['elements'] if e['faces']['south']['texture']=='#glass']
 rejects(lambda:school_mesh_contract(root,{'school_round_window':broken}),'window glass leaves exposed rectangular wall-hole corners')
 broken=read(root,ASSET/'models/block/school_principal_desk.json');broken['elements']=[e for e in broken['elements'] if e['from']!=[-1.85,10,9.8]]
 rejects(lambda:school_mesh_contract(root,{'school_principal_desk':broken}),'upright book missing colored back/spine')
 broken=read(root,ASSET/'models/block/school_facade_title_middle.json');broken['elements'][0]['from'][0]+=4
 rejects(lambda:school_mesh_contract(root,{'school_facade_title_middle':broken}),'centered title has a physical gap')
 source=(Path(root)/'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text();broken=source.replace('6.5,-6,6.5,9.5,-2.5,9.5','6.5,0,6.5,9.5,10,9.5')
 rejects(lambda:small_mesh_bounds(root,broken),'small prop retains invisible high selection/collision volume')
 image=Image.new('RGBA',(64,64),'#2dace2')
 for x in range(64):image.putpixel((x,63),(20,120,210,255))
 rejects(lambda:periodic_sky(image),'repeating ceiling atlas has a hard background gradient seam')
 broken=read(root,ASSET/'models/block/school_art_chao.json')
 rejects(lambda:corner_painting_clearance(root,broken),'wide painting buries its frame/image in the actual chamfer wall')
 broken=read(root,ASSET/'models/block/school_red_counter.json');broken['elements'][1]['faces']['south']['texture']='#base'
 rejects(lambda:decor_surface_aspects(root,{'school_red_counter':broken}),'square counter painting stretched over fourteen-by-six front')
 broken=read(root,ASSET/'models/block/school_green_counter.json');broken['elements'][1]['faces'].pop('north')
 rejects(lambda:decor_surface_aspects(root,{'school_green_counter':broken}),'counter furniture loses its actual back face')
 broken=read(root,ASSET/'models/block/school_white_exam_bed.json');broken['elements'].pop(0)
 rejects(lambda:decor_surface_aspects(root,{'school_white_exam_bed':broken}),'clinic union furniture loses an actual exterior patch and opens its closed skin')
 broken=read(root,ASSET/'models/block/school_white_exam_bed.json');broken['elements'].append(deepcopy(broken['elements'][0]))
 rejects(lambda:decor_surface_aspects(root,{'school_white_exam_bed':broken}),'clinic union furniture duplicates an exposed skin and fights at the frame corner')
 broken=read(root,ASSET/'models/block/school_purple_room_curtain.json');broken['elements'][1]['faces']['south']['uv']=[0,0,16,16]
 rejects(lambda:decor_surface_aspects(root,{'school_purple_room_curtain':broken}),'thin curtain fold repeats and stretches whole cloth image')
 broken=read(root,ASSET/'models/block/school_globe.json');next(e for e in broken['elements'] if any(f['texture']=='#earth' for f in e['faces'].values()))['faces']['south']['uv']=[0,0,16,16]
 rejects(lambda:decor_surface_aspects(root,{'school_globe':broken}),'stepped globe repeats complete map on a thin band')
 broken=read(root,ASSET/'models/block/school_purple_room_curtain_tie.json');broken['elements'][-1]['from'][2]=4.5;broken['elements'][-1]['to'][2]=6
 rejects(lambda:decor_surface_aspects(root,{'school_purple_room_curtain_tie':broken}),'gold tassel is hidden behind curtain instead of joined in front')
 source=(Path(root)/'src/main/java/com/chaogarden/kindergarten/KindergartenRooms025.java').read_text()
 rejects(lambda:school_boundary_contract(root,source.replace('for(int z=18;z<=51;z++)for(int y=80;y<103;y++)','for(int z=20;z<=48;z++)for(int y=82;y<103;y++)')),'school boundary retains walk-under porch gap')
 rejects(lambda:school_boundary_contract(root,source.replace('if(blocks.getOrDefault(q,Blocks.AIR.defaultBlockState()).isAir())','')),'boundary replacement erases actual hedges/pillars')
 rejects(lambda:school_boundary_contract(root,source.replace('p.barrierAir(x,102,z);','')),'school approach has an open overflight cap')
 rejects(lambda:school_boundary_contract(root,source.replace('p.barrierAir(x,y,18);','')),'school approach can fly above the front gable')
 rejects(lambda:school_boundary_contract(root,source.replace('p.barrierAir(x,y,51);','')),'school approach has an open far return end')
 broken=read(root,ASSET/'models/block/school_green_counter.json');old=deepcopy(broken['elements'][1]);old['from']=[1,2,1];old['to']=[15,8,15];broken['elements'][1]=old
 rejects(lambda:coplanar_furnishing_faces(root,{'school_green_counter':broken}),'old counter apron intersects woodpost same-normal corner faces')
 broken=read(root,ASSET/'models/block/school_purple_room_curtain_tie.json');cloth=deepcopy(broken['elements'][0]);cloth['from'][1]=0;cloth['to'][1]=16;broken['elements'].append(cloth)
 rejects(lambda:coplanar_furnishing_faces(root,{'school_purple_room_curtain_tie':broken}),'uncut cloth endcap fights with gold tie endcap')
 with Image.open(Path(root)/ASSET/'textures/block/school025/school_door_exit.png') as original:
  image=original.convert('RGBA');bad=image.copy();background=image.getpixel((0,0));colors={(214,206,156),(145,206,202),(233,211,114),(57,156,145),(49,90,96)}
  points=[(x,y,image.getpixel((x,y))) for y in range(image.height) for x in range(image.width) if image.getpixel((x,y))[:3] in colors]
  for x,y,pixel in points:bad.putpixel((x,y),background)
  for x,y,pixel in points:bad.putpixel((x,max(0,y-round(image.height*67/384))),pixel)
 rejects(lambda:door_badge_clearance(root,{'school_door_exit':bad}),'upper badge is visibly clipped by closed arch shoulders')
 source=(Path(root)/'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text()
 operation='pairStates(this,center,s.getValue(FACING),open).forEach((at,state)->level.setBlock(at,state,flags));'
 bad=source.replace(operation,'if(!level.isClientSide){'+operation+'}')
 rejects(lambda:school_door_prediction_contract(root,bad),'door returns to individually visible server-only segment updates')
 rejects(lambda:school_door_prediction_contract(root,source.replace('|Block.UPDATE_IMMEDIATE;',';')),'door prediction drops immediate native redraw')
 rejects(lambda:school_door_prediction_contract(root,source.replace('else if(!expandLegacy(level,pos,s))return InteractionResult.PASS;','else expandLegacy(level,pos,s);')),'door predicts an incomplete pair after both conservative migration paths fail')
 rejects(lambda:school_door_prediction_contract(root,source.replace('if(migrated!=null)center=migrated;','if(migrated!=null)center=pairCenter(s,pos);')),'old forward tip click predicts around its stale center after migration')
 rejects(lambda:school_door_prediction_contract(root,source.replace('if(!matches(level,old,null))continue;','')),'saved-tip migration accepts an incomplete or foreign old assembly')
 rejects(lambda:school_door_prediction_contract(root,source.replace('for(var part:current.entrySet())if(!level.hasChunkAt(part.getKey())||!old.containsKey(part.getKey())&&!level.getBlockState(part.getKey()).isAir()){free=false;break;}','')),'saved-tip migration overwrites unrelated rear masonry')
 rejects(lambda:school_door_prediction_contract(root,source.replace('java.util.List.of(center,center.relative(state.getValue(FACING),2))','java.util.List.of(center,center.relative(state.getValue(FACING).getOpposite(),2))')),'old forward tip resolves migration in the wrong direction')
 rejects(lambda:school_door_prediction_contract(root,source.replace('}finally{REMOVING.set(false);}','}finally{}')),'saved-tip migration leaves recursive removal suppression enabled')
 return 46


def release_jar(path,expected,internal):
 with zipfile.ZipFile(path) as archive:
  names=set(archive.namelist())
  require(not any(n.startswith(('com/chaogarden/test/','data/chao_visual_bootstrap/')) or 'World025VisualSmoke' in n for n in names),'Development fixture/code leaked into release JAR')
  require(not any(n.startswith('data/minecraft/worldgen/world_preset/') for n in names),'Test/vanilla world preset override leaked into release JAR')
  for key,value in expected.items():
   name='data/chao_garden/dimension/'+key.split(':',1)[1]+'.json';require(name in names and json.loads(archive.read(name))==value,'Release dimension differs from validated production source: '+key)
  for room in ROOMS:
   name='data/chao_garden/dimension_type/'+room+'.json';require(name in names,'Release omits dimension type: '+room)
  for extension in ('json','vsh','fsh'):require('assets/chao_garden/shaders/core/scene_unlit.'+extension in names,'Release omits scene shader stage: '+extension)
  for block in internal:require('assets/chao_garden/models/item/'+block+'.json' not in names,'Internal item leaked into release JAR: '+block)
  require('assets/minecraft/atlases/blocks.json' in names,'Release omits native scene atlas registrations')
  require('assets/chao_garden/textures/scene/lobby_glass.png' in names and 'assets/chao_garden/shaders/core/scene_unlit.json' in names,'Release omits transparent lobby material')


def main():
 parser=argparse.ArgumentParser();parser.add_argument('--root',type=Path,default=ROOT);parser.add_argument('--jar',type=Path);args=parser.parse_args()
 school,internal,domain,expected=validate(args.root);negative=negative_fixtures(args.root,domain)
 if args.jar:release_jar(args.jar,expected,internal)
 print(f'Environment025 assertions passed: {checks:,}; {len(school)} reusable school decorations, {len(internal)} internal scene blocks, four isolated realms, RGBA glass/shader and dev separation; {negative} malformed fixtures rejected.')
if __name__=='__main__':main()
