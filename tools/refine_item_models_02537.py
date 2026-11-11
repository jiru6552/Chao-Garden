"""Native JSON item geometry; no raster textures or original music are rewritten."""
import copy,json,sys,zipfile
from pathlib import Path
from io import BytesIO
from PIL import Image
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/chao_garden'
def save(name,j):
 for hand in ['firstperson_righthand','firstperson_lefthand']:
  j['display'].setdefault(hand,copy.deepcopy(j['display']['firstperson_righthand']))
  if name in ['chao_key','garden_guide','chao_garden_music_disc']:
   j['display'][hand]['scale']=[.5,.5,.5];j['display'][hand]['translation']=[0,2,0]
  elif name=='chao_teleporter':
   j['display'][hand]['scale']=[.24,.24,.24];j['display'][hand]['translation']=[0,3,0]
 (root/'models/item'/f'{name}.json').write_text(json.dumps(j,indent=2)+'\n')
def box(lo,hi,tex,uv=None):
 return {'from':lo,'to':hi,'faces':{side:{'texture':'#'+tex,'uv':uv or [0,0,16,16]} for side in ['north','south','east','west','up','down']}}
def model(textures,display):return {'ambientocclusion':False,'gui_light':'front','textures':textures,'elements':[],'display':copy.deepcopy(display)}
with zipfile.ZipFile(sys.argv[1]) as z:
 display=json.loads(z.read('assets/minecraft/models/item/generated.json'))['display']
 im=Image.open(BytesIO(z.read('assets/minecraft/textures/item/music_disc_blocks.png'))).convert('RGBA')
# Teal spade-shaped key, recessed Chao emblem and floating yellow emotion orb.
j=model({'body':'minecraft:block/cyan_concrete','rim':'minecraft:block/blue_concrete','screen':'minecraft:block/black_concrete','blue':'minecraft:block/light_blue_concrete','gold':'minecraft:block/yellow_concrete','particle':'minecraft:block/cyan_concrete'},display)
e=j['elements']
for lo,hi in [([7,1,7],[9,8,9]),([4,1,7],[7,3,9]),([5,4,7],[7,5.5,9]),([5,8,6.7],[11,9,9.3]),([4,9,6.7],[12,12,9.3]),([5,12,6.7],[11,13,9.3]),([6,13,6.7],[10,14,9.3]),([7,14,6.7],[9,14.6,9.3])]:e.append(box(lo,hi,'body'))
for lo,hi in [([7.25,15.1,7.25],[8.75,16.6,8.75]),([6.9,15.45,7.6],[9.1,16.25,8.4]),([7.6,15.45,6.9],[8.4,16.25,9.1])]:e.append(box(lo,hi,'gold'))
for front in [True,False]:
 z=6.59 if front else 9.31;dz=-.025 if front else .025
 e.append(box([4.6,9,z],[11.4,12.5,z+.07],'rim'))
 z+=dz-.01 if front else .08
 e.append(box([5.1,9.35,z],[10.9,12.15,z+.025],'screen'))
 z+=dz-.01 if front else .04
 for lo,hi in [([5.8,9.65,z],[10.2,10.8,z+.02]),([6.5,10.8,z],[9.5,11.35,z+.02]),([7.25,11.35,z],[8.75,11.65,z+.02])]:e.append(box(lo,hi,'blue'))
 z+=dz-.01 if front else .04
 for x in [6.7,8.65]:e.append(box([x,9.95,z],[x+.55,10.8,z+.02],'screen'))
 e.append(box([7.7,11.8,z],[8.3,12.1,z+.02],'gold'))
j['display']['gui']={'rotation':[0,0,-28],'translation':[0,-.3,0],'scale':[.95,.95,.95]}
save('chao_key',j)
# Bound green journal, cream page edges, reinforced spine and central Chao crest.
j=model({'cover':'minecraft:block/green_concrete','spine':'minecraft:block/green_terracotta','paper':'minecraft:block/bone_block_side','gold':'minecraft:block/yellow_concrete','blue':'minecraft:block/light_blue_concrete','ink':'minecraft:block/black_concrete','particle':'minecraft:block/green_concrete'},display);e=j['elements']
e += [box([3.4,1.6,6.8],[12.5,14.4,9.2],'paper'),box([2.8,1,6.2],[13.1,15,6.8],'cover'),box([2.8,1,9.2],[13.1,15,9.8],'cover'),box([2.4,1,6.2],[3.7,15,9.8],'spine')]
for y in [3,12.6]:e.append(box([2.35,y,6.15],[3.75,y+.45,9.85],'gold'))
for front in [True,False]:
 z=6.15 if front else 9.81;off=-.04 if front else .04
 for x,y,w,h in [(4.1,2.1,7.9,.25),(4.1,13.65,7.9,.25),(4.1,2.1,.25,11.8),(11.75,2.1,.25,11.8)]:e.append(box([x,y,z],[x+w,y+h,z+.02],'gold'))
 for lo,hi in [([5.1,6.4,z],[11,8.6,z+.02]),([5.7,8.6,z],[10.4,9.5,z+.02]),([6.7,9.5,z],[9.4,10.3,z+.02]),([7.4,10.3,z],[8.7,10.7,z+.02])]:e.append(box(lo,hi,'blue'))
 z+=off
 for x in [6.1,9.1]:e.append(box([x,7,z],[x+.75,8.65,z+.02],'ink'))
 e.append(box([7.4,11.3,z],[8.7,12.6,z+.02],'gold'))
 e.append(box([6,4.6,z],[10.1,5.1,z+.02],'gold'))
j['display']['gui']={'rotation':[10,-20,-8],'translation':[0,0,0],'scale':[.95,.95,.95]}
save('garden_guide',j)
# The same vanilla disc pixel silhouette and surface UVs, with a two-colour label.
# Geometry is pixel-aligned and uses the existing vanilla sprite; no raster is edited.
j=model({'record':'minecraft:item/music_disc_blocks','blue':'minecraft:block/light_blue_concrete','yellow':'minecraft:block/yellow_concrete','particle':'minecraft:item/music_disc_blocks'},display)
for y in range(16):
 for x in range(16):
  r,g,b,a=im.getpixel((x,y))
  if not a:continue
  label=r>100 and r>g*1.5 and r>b*1.5
  tex=('blue' if y<8 else 'yellow') if label else 'record'
  uv=[8,8,8,8] if label else [x+.5,y+.5,x+.5,y+.5]
  el=box([x,15-y,7.5],[x+1,16-y,8.5],tex,uv);el['shade']=False
  # Keep only exposed side faces between pixels.
  for side,nx,ny in [('west',x-1,y),('east',x+1,y),('up',x,y-1),('down',x,y+1)]:
   if 0<=nx<16 and 0<=ny<16 and im.getpixel((nx,ny))[3]:el['faces'].pop(side)
  j['elements'].append(el)
save('chao_garden_music_disc',j)
# Combined two-block item geometry must have explicit UVs within the atlas sprite.
p=root/'models/item/chao_teleporter.json';j=json.loads(p.read_text())
for el in j['elements']:
 lo,hi=el['from'],el['to'];w,h,d=[hi[i]-lo[i] for i in range(3)]
 for side,f in el['faces'].items():
  u,v=(w,d) if side in ['up','down'] else (d,h) if side in ['east','west'] else (w,h)
  f['uv']=[0,0,min(16,u),min(16,v)]
for hand in ['firstperson','thirdperson']:
 j['display'][hand+'_lefthand']=copy.deepcopy(j['display'][hand+'_righthand'])
 j['display'][hand+'_lefthand']['rotation'][1]*=-1
save('chao_teleporter',j)
