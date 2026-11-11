#!/usr/bin/env python3
"""Original native CWE sprites, attachment meshes, orchard resources and physical toys."""
from pathlib import Path
import sys,json,copy,math
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden';D=ROOT/'src/main/resources/data/chao_garden'
sys.path[:0]=[str(ROOT/'scripts'),str(ROOT/'art')]
import generate_gene_assets as genes
import generate_fruit_assets as orchard
import build_toy_blocks_018 as toys
FRUITS=['orange_fruit','gba_orange_fruit','gba_blue_fruit','gba_pink_fruit','gba_green_fruit','gba_purple_fruit','gba_yellow_fruit','gba_red_fruit','shiny_fruit','hyper_swim_fruit','hyper_fly_fruit','hyper_run_fruit','hyper_power_fruit']
COLORS=['#ed9725','#f19527','#579bcf','#f396b9','#8dc554','#9b69bd','#f0ce50','#dc5c55','#d5b4ed','#65b2de','#a79ad7','#7ec365','#e67770']
EXTRA_GENES=['bee','ladybug','scorpion','deer','elephant','kangaroo','koala','lion','mole','swallow']
GENE_COLORS=['e9bf37','dd5550','9d4b9b','c69c63','a0a6b5','ca915c','aeb4c1','d7aa47','8d6655','5c92c6']
ACCESSORIES=['baseball_cap_accessory','bowtie_accessory','round_glasses_accessory','aviators_accessory','headphones_accessory','pacifier_accessory','press_hat_accessory','blue_hoodie_accessory','pink_hoodie_accessory','cheese_bowtie_accessory']
GIFTS=[x+'_lens' for x in ['clear','blue','green','red','yellow','purple','pink','orange','cyan','hero','dark','chaos']]+['cosmetic_wings','reverse_mirror','suspicious_potion']+[x+'_hero_brush' for x in ['normal','swim','fly','run','power']]+['omochao_parts_box']
NEW_TOYS=['chao_rocking_horse','chao_piano','chao_organ','chao_rubber_duck','chao_lifesaver','chao_triangle','chao_guitar','chao_accordion','chao_bubble_wand','chao_shovel','chao_bell','chao_castanets','chao_cymbals','chao_flute','chao_tambourine']
def write(path,data):orchard.write(path,data)
def save(im,path):orchard.save(im,path)
def replace(data,a,b):
 if isinstance(data,str):return data.replace(a,b)
 if isinstance(data,list):return [replace(x,a,b) for x in data]
 if isinstance(data,dict):return {replace(k,a,b):replace(v,a,b) for k,v in data.items()}
 return data
def color(hex):return tuple(bytes.fromhex(hex.strip('#')))
def shade(rgb,f):return tuple(max(0,min(255,round(v*f))) for v in rgb)
def fruit_sprite(ident,base):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);rgb=color(base)
 # Four broad step bands retain console fruit silhouettes rather than sphere noise.
 if ident=='orange_fruit':p=[(5,4),(10,4),(13,7),(13,11),(10,14),(5,14),(2,11),(2,7)]
 elif ident=='shiny_fruit':p=[(7,2),(9,5),(13,6),(12,10),(9,14),(5,14),(2,10),(3,6),(6,5)]
 elif ident.startswith('hyper_'):p=[(7,2),(9,4),(9,6),(12,8),(13,12),(10,15),(5,15),(2,12),(3,8),(6,6),(6,4)]
 else:p=[(6,3),(10,3),(13,6),(14,10),(12,13),(8,15),(4,13),(1,10),(2,6)]
 d.polygon(p,fill=shade(rgb,.58));mask=Image.new('1',(16,16));ImageDraw.Draw(mask).polygon(p,fill=1)
 for y in range(16):
  for x in range(16):
   if mask.getpixel((x,y)) and mask.getpixel((max(0,x-1),y)) and mask.getpixel((x,max(0,y-1))):
    f=1.22-x*.025-y*.009;im.putpixel((x,y),shade(rgb,f)+(255,))
 if ident.startswith('gba_'):d.line((4,7,6,5,9,5),fill=shade(rgb,1.35),width=1)
 if ident=='shiny_fruit':
  d.polygon([(7,6),(8,8),(10,9),(8,10),(7,12),(6,10),(4,9),(6,8)],fill='#fff3bb')
 if ident.startswith('hyper_'):
  d.line((5,9,10,9),fill='#fff1a0');d.line((5,12,10,12),fill='#fff1a0')
 d.line((7,3,7,1,8,0),fill='#7d5d35');d.polygon([(8,1),(11,1),(10,3),(8,3)],fill='#5b9745')
 return im
def sprite(ident):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 if ident.endswith('_lens'):
  hue={'clear':'#d9eff3','blue':'#4886df','green':'#5db953','red':'#dd5b55','yellow':'#e5c648','purple':'#9f6bca','pink':'#e293bc','orange':'#ec9d48','cyan':'#5ec5d6','hero':'#91d2e9','dark':'#c25852','chaos':'#e9bd72'}[ident[:-5]]
  d.rectangle((2,3,13,13),fill='#455665');d.rectangle((3,4,12,12),fill='#b9dce2');d.ellipse((4,5,7,11),fill='white');d.ellipse((8,5,11,11),fill='white');d.rectangle((5,6,6,10),fill=hue);d.rectangle((9,6,10,10),fill=hue);d.rectangle((2,2,13,4),fill='#7898a9')
 elif ident.endswith('brush'):
  d.rectangle((7,8,9,14),fill='#9c6743');d.rectangle((4,3,12,7),fill='#e6bd6b');d.rectangle((4,2,12,3),fill='#5a7898')
  for x in (5,7,9,11):d.line((x,0,x,2),fill='#f8edc4')
  hue={'normal':'#5b95d2','swim':'#e8ce55','fly':'#ad79c7','run':'#72b86c','power':'#d96972'}[ident.split('_',1)[0]];d.rectangle((4,4,12,5),fill=hue)
 elif ident=='cosmetic_wings':
  for p in [[(7,5),(3,2),(1,4),(2,9),(6,11)],[(8,5),(12,2),(14,4),(13,9),(9,11)]]:d.polygon(p,fill='#8d75ba');d.line(p+[p[0]],fill='#d2bde7')
 elif ident=='reverse_mirror':
  d.rectangle((7,10,9,15),fill='#8b6748');d.ellipse((2,1,13,12),fill='#dfbf70');d.ellipse((3,2,12,11),fill='#6a9fb9');d.line((5,7,9,3),fill='#e8f1ed')
 elif ident=='suspicious_potion':
  d.rectangle((6,0,9,2),fill='#8f6a44');d.rectangle((5,3,10,5),fill='#c9e4dc');d.polygon([(5,5),(3,8),(3,12),(6,15),(10,15),(13,12),(13,8),(10,5)],fill='#9aacc9');d.polygon([(4,9),(12,9),(12,12),(9,14),(6,14),(4,12)],fill='#7664ba');d.line((4,8,4,11),fill='#eef7e2')
 elif ident=='omochao_parts_box':
  d.rectangle((1,4,14,14),fill='#947345');d.polygon([(1,4),(5,1),(15,2),(14,5)],fill='#c29b5a');d.rectangle((6,4,9,14),fill='#ddbc78');d.rectangle((5,7,11,11),fill='#66a4bd');d.rectangle((7,8,8,10),fill='#e8d872')
 else:
  # Item icon is a readable miniaturization of the real accessory geometry.
  if 'hoodie' in ident:
   fill='#dba0bf' if ident.startswith('pink') else '#6797d4';d.polygon([(4,2),(11,2),(13,5),(15,13),(11,14),(10,9),(10,15),(5,15),(5,9),(4,14),(0,13),(2,5)],fill=fill);d.rectangle((5,3,10,6),fill='#354f6a');d.line((7,8,7,12),fill='#e7e6dc')
  elif 'glasses' in ident or 'aviators' in ident:
   d.line((0,6,15,6),fill='#4a5661',width=2)
   if ident=='round_glasses_accessory':
    for x in (1,9):d.ellipse((x,4,x+5,10),fill='#d8c997');d.ellipse((x+1,5,x+4,9),fill='#486f86')
   else:
    for x in (1,9):d.polygon([(x,5),(x+5,5),(x+5,8),(x+3,11),(x+1,9)],fill='#d8c997');d.polygon([(x+1,6),(x+4,6),(x+4,8),(x+3,9),(x+2,8)],fill='#486f86')
  elif 'bowtie' in ident:
   d.polygon([(1,4),(7,7),(14,4),(14,12),(7,9),(1,12)],fill='#cb596b');d.rectangle((6,6,9,10),fill='#ef928f')
  elif 'headphones' in ident:
   d.arc((2,1,13,14),180,360,fill='#556775',width=2);d.rectangle((1,7,4,13),fill='#9e8bc3');d.rectangle((11,7,14,13),fill='#9e8bc3')
  elif 'pacifier' in ident:d.ellipse((2,4,13,11),fill='#8fd2d3');d.ellipse((5,8,10,14),outline='#e5d28b',width=2)
  elif ident=='eggshell_hat':
   d.polygon([(2,7),(4,4),(6,6),(8,3),(10,6),(12,4),(14,7),(14,11),(12,14),(4,14),(2,11)],fill='#e1dcc3');d.line((4,12,11,12),fill='#fff2d8')
  else:d.polygon([(2,6),(4,2),(11,2),(13,6),(14,8),(14,10),(0,10),(0,8)],fill='#7392b2');d.rectangle((4,3,10,6),fill='#b7d6dd')
 return im
# Accessory meshes are local to the real sampled semantic bone pivots.
PALETTE={'white':(238,238,230),'black':(37,45,55),'glass':(111,163,180),'gold':(215,180,104),'red':(215,87,104),'pink':(229,155,182),'paper':(234,226,192)}
def box(bounds,mat='white',name='part'):
 x,y,z,X,Y,Z=bounds;verts=[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]]
 i=list(PALETTE).index(mat);u=(i%4)*4;v=(i//4)*4;uv=[[u,v],[u+4,v],[u+4,v+4],[u,v+4]]
 return {'name':name,'material':'body','shading':'flat','vertices':verts,'faces':[{'v':f,'uv':uv} for f in [[1,0,3,2],[4,5,6,7],[0,4,7,3],[5,1,2,6],[3,7,6,2],[0,1,5,4]]]}
def mesh(ident):
 parts=[]
 def b(bounds,mat='white'):parts.append(box(bounds,mat))
 if ident.endswith('hoodie_accessory'):
  b((-2,-.7,-1.6,2,3.2,1.6));b((-1.5,2.2,-1.78,1.5,3.1,-1.6),'paper');b((-1,-1,-1.8,1,.1,-1.5),'black')
 elif ident.startswith('hoodie_sleeve_'):b((-1,-.6,-1,1,1.7,1))
 elif ident.startswith('medal_'):
  # Two hanging ribbons and a stepped eight-sided medallion sit in front of hoodie cloth.
  b((-.7,-.8,-2.1,-.35,.55,-1.9),'red');b((.35,-.8,-2.1,.7,.55,-1.9),'red')
  b((-.65,.45,-2.35,.65,1.8,-2.1));b((-.9,.7,-2.35,-.65,1.55,-2.1));b((.65,.7,-2.35,.9,1.55,-2.1));b((-.4,.2,-2.35,.4,.45,-2.1));b((-.4,1.8,-2.35,.4,2.05,-2.1))
  if ident=='medal_race':b((-.3,.65,-2.43,.3,1.5,-2.35),'paper')
  else:b((-.4,.9,-2.43,.4,1.2,-2.35),'paper');b((-.15,.65,-2.43,.15,1.5,-2.35),'paper')
 elif ident=='eggshell_hat':
  # Open rim and alternating cracked teeth; no solid lid obscures a Chao's tip.
  for bounds in [(-3.1,-3.5,-2.8,3.1,-2.2,-2.4),(-3.1,-3.5,2.4,3.1,-2.2,2.8),(-3.1,-3.5,-2.4,-2.7,-2.2,2.4),(2.7,-3.5,-2.4,3.1,-2.2,2.4)]:b(bounds)
  for x in (-2.8,-.8,1.2):b((x,-4.3,-2.8,x+1.1,-3.5,-2.4));b((x+.5,-4.3,2.4,x+1.6,-3.5,2.8))
 elif 'glasses' in ident or 'aviators' in ident:
  for x in (-2.7,.4):
   b((x,-.6,-3.2,x+2.2,.85,-3.03),'glass');b((x-.15,-.8,-3.3,x+2.35,-.6,-3.0),'gold');b((x-.15,.85,-3.3,x+2.35,1.05,-3.0),'gold')
   for xx in (x-.15,x+2.2):b((xx,-.6,-3.3,xx+.15,.85,-3.0),'gold')
  b((-.4,-.3,-3.25,.4,-.1,-3.05),'black')
  for x in (-3.05,2.75):b((x,-.3,-3.2,x+.2,0,1.5),'black')
 elif 'bowtie' in ident:
  for x in (-1.7,.4):b((x,2.5,-2.7,x+1.3,3.8,-2.15),'red')
  b((-.45,2.8,-2.9,.45,3.5,-2.1),'pink');b((-3,2.85,-2.2,3,3.25,-2.05),'red')
 elif 'headphones' in ident:
  b((-3.4,-3.4,-.8,3.4,-3.1,.8),'black')
  for x in (-3.65,3.1):b((x,-3.1,-.8,x+.55,1.0,.8),'black');b((x-.15,-1.1,-1.2,x+.7,1.1,1.2))
 elif 'pacifier' in ident:
  b((-1.2,1.0,-3.3,1.2,2.05,-3.05));b((-.6,1.55,-3.7,.6,1.8,-3.3),'gold')
  b((-.6,1.8,-3.8,-.35,2.8,-3.55),'gold');b((.35,1.8,-3.8,.6,2.8,-3.55),'gold');b((-.6,2.55,-3.8,.6,2.8,-3.55),'gold')
 else:
  b((-3.25,-3.7,-2.9,3.25,-3.2,3.0));b((-2.8,-5,-2.6,2.8,-3.7,2.6));b((-2.9,-3.65,-4.0,2.9,-3.3,-2.7))
  if ident=='press_hat_accessory':b((-1.1,-4.9,-2.72,1.2,-3.8,-2.57),'paper')
 return {'texture_width':16,'texture_height':16,'bones':[{'name':'accessory','pivot':[0,0,0],'rotation':[0,0,0],'meshes':parts}]}
def toy_model(ident,active=False,frame=0):
 parts=[]
 def b(bounds,mat):parts.append(toys.cube(bounds,mat))
 bounce=(0,.5,1,.5)[frame] if active else 0
 if ident=='chao_rocking_horse':
  for z in (3,12):b((2,0,z,14,1,z+1),'wood');b((1,1,z,3,2,z+1),'wood');b((13,1,z,15,2,z+1),'wood')
  for x in (5,10):
   for z in (5,10):b((x,1,z,x+1,6+bounce,z+1),'wood')
  b((4,6+bounce,4,12,10+bounce,12),'wood');b((10,9+bounce,6,13,15+bounce,10),'wood');b((10,15+bounce,6,11,17+bounce,7),'wood');b((10,15+bounce,9,11,17+bounce,10),'wood');b((12,12+bounce,5.9,12.5,13+bounce,6.1),'black');b((5,10+bounce,4,10,10.7+bounce,12),'red')
 elif ident in ('chao_piano','chao_organ'):
  for x in (2,12):b((x,0,4,x+2,6,13),'wood')
  b((1,5,4,15,11,14),'wood');b((2,8,2,14,8.7,5),'white')
  for x in range(3,13,2):b((x,8.7-(.3 if active and (x+frame)%3==0 else 0),3,x+.6,9.2,5),'black')
  b((1,8,2,2,10,5),'wood');b((14,8,2,15,10,5),'wood')
  if ident=='chao_organ':
   for i,x in enumerate((2,4,6,8,10,12)):b((x,11,11,x+1.2,15+(3-abs(i-2)),12.2),'metal')
  else:b((1,11,4,15,12,14),'black')
 elif ident=='chao_rubber_duck':
  b((3,1+bounce,3,13,5+bounce,13),'yellow');b((9,5+bounce,5,14,10+bounce,11),'yellow');b((13,6+bounce,6,16,7.4+bounce,10),'red');b((12.5,8+bounce,4.9,13.2,8.8+bounce,5.1),'black');b((1,3+bounce,6,4,7+bounce,10),'yellow')
 elif ident=='chao_lifesaver':
  for q in ((2,0,4,5,3,12),(11,0,4,14,3,12),(5,0,2,11,3,5),(5,0,11,11,3,14)):b(q,'white')
  for q in ((2,-.05,7,5,3.05,9),(11,-.05,7,14,3.05,9),(7,-.05,2,9,3.05,5),(7,-.05,11,9,3.05,14)):b(q,'red')

 elif ident=='chao_triangle':
  b((4,2,6,12,2.7,7),'metal')
  for angle in (-22.5,22.5):parts.append(toys.cube((7.65,1.5,6,8.35,11,7),'metal',rotation={'origin':[8,11,6.5],'axis':'z','angle':angle}))
  b((7.7,11,6,8.1,14,6.5),'wood');b((4,1,10,12,1.6,10.6),'metal')
 elif ident=='chao_guitar':
  b((4,0,4,12,2,12),'wood');b((3,0,6,13,2,10),'wood');b((6.5,1.8,7,9.5,2.1,10),'black');b((7,1,0,9,2,8),'wood')
  for x in (7.3,7.7,8.1,8.5):b((x,2.1,0,x+.08,2.2,11),'metal')
 elif ident=='chao_accordion':
  b((2,0,3,5,8,12),'red');b((11,0,3,14,8,12),'black')
  for x in range(5,11):b((x,1 if x%2 else .5,4 if x%2 else 3.5,x+.7,7 if x%2 else 7.5,11 if x%2 else 11.5),'gray')
  for y in range(1,7):b((2,y,2.7,5,y+.5,3),'white')
 elif ident=='chao_bubble_wand':
  b((7.6,0,5,8.4,1,14),'pink')
  for q in ((5,0,2,11,1,3),(5,0,3,6,1,6),(10,0,3,11,1,6),(6,0,5,10,1,6)):b(q,'cyan')
  if active: b((3,5+bounce,5,6,8+bounce,8),'cyan')
 elif ident=='chao_bell':
  b((7.4,1,7.4,8.6,8,8.6),'wood');b((6,4,6,10,5,10),'brass');b((5,2,5,11,4,11),'brass');b((4,1,4,12,2,12),'brass');b((7,0,7,9,1,9),'black')
 elif ident=='chao_castanets':
  for x in (2,9):b((x,0,5,x+5,1.5,11),'wood');b((x,2+bounce,5,x+5,3.5+bounce,11),'wood');b((x+.5,1,10,x+4.5,3+bounce,11),'red')
 elif ident=='chao_cymbals':
  for x,z in ((1,2),(8,8)):
   b((x,0,z,x+6,.5,z+6),'brass');b((x+1,.5,z+1,x+5,1,z+5),'brass');b((x+2,1,z+2,x+4,2,z+4),'brass_dark');b((x+2.5,2,z+2.5,x+3.5,3,z+3.5),'rubber')
 elif ident=='chao_flute':
  b((2,0,7,14,1.2,8.2),'metal');b((1,.3,7.2,3,1.5,8),'metal')
  for x in (5,7,9,11):b((x,1.2,7.4,x+.6,1.35,8),'black')
 elif ident=='chao_tambourine':
  for q in ((3,0,4,5,2,12),(11,0,4,13,2,12),(5,0,2,11,2,4),(5,0,12,11,2,14)):b(q,'wood')
  b((5,1.7,4,11,2,12),'white')
  for q in ((2.8,.5,6,3.1,1.5,8),(12.9,.5,8,13.2,1.5,10),(6,.5,1.8,8,1.5,2.1),(8,.5,13.9,10,1.5,14.2)):b(q,'metal')
 elif ident=='chao_shovel':
  b((7.5,0,1,8.5,1,11),'wood');b((5.5,0,9,10.5,1,14),'metal');b((6.5,0,0,9.5,1,2),'wood')
  if active:
   b((1,0,5,6,3+frame,10),'skin')
   for x,z in ((1,5),(4,5),(1,8),(4,8)):b((x,3+frame,z,x+2,5+frame,z+2),'skin')
 return {'ambientocclusion':False,'textures':{'palette':'chao_garden:block/toy_palette','particle':'chao_garden:block/toy_palette'},'elements':parts,'display':{'gui':{'rotation':[30,225,0],'scale':[.65,.65,.65]},'ground':{'scale':[.5,.5,.5]},'fixed':{'scale':[.7,.7,.7]}}}
def contact_sheet():
 ids=FRUITS+[x+'_seed' for x in FRUITS]+[x+'_animal_gene' for x in EXTRA_GENES]+ACCESSORIES+GIFTS+['eggshell_hat']
 columns=8;cw=136;ch=88;im=Image.new('RGB',(columns*cw,44+((len(ids)+columns-1)//columns)*ch),'#e7eef0');d=ImageDraw.Draw(im)
 d.text((14,12),'Chao Garden 0.19 - native CWE inventory art',fill='#2d3e49')
 for i,key in enumerate(ids):
  x=(i%columns)*cw;y=44+(i//columns)*ch;icon=Image.open(A/'textures/item'/f'{key}.png').convert('RGBA').resize((48,48),Image.Resampling.NEAREST);im.paste(icon,(x+(cw-48)//2,y),icon)
  words=key.replace('_animal_gene',' DNA').replace('_accessory','').replace('_',' ');lines=[]
  while len(words)>21:
   end=words[:22].rfind(' ');end=21 if end<1 else end;lines.append(words[:end]);words=words[end:].strip()
  lines.append(words)
  for j,line in enumerate(lines):d.text((x+4,y+51+j*12),line,fill='#3f5663')
 save(im,ROOT/'docs/previews/cwe-content-019.png')
def main():
 lang=json.loads((A/'lang/en_us.json').read_text())
 # Clone only stable orchard plumbing, then replace each new crop's native identity art.
 for ident,base in zip(FRUITS,COLORS):
  for folder,tail in [('blockstates','_seedling'),('blockstates','_tree_crown'),('blockstates','_pod'),('models/block','_seedling')]:
   source=A/folder/('garden_fruit'+tail+'.json');write(A/folder/(ident+tail+'.json'),replace(json.loads(source.read_text()),'garden_fruit',ident))
  seed=ident+'_seed';save(fruit_sprite(ident,base),A/'textures/item'/f'{ident}.png')
  seedim=Image.new('RGBA',(16,16));sd=ImageDraw.Draw(seedim);rgb=color(base)
  for x,y in ((2,7),(7,2),(9,9)):
   sd.polygon([(x+2,y),(x+4,y+2),(x+4,y+4),(x+2,y+6),(x,y+4),(x,y+2)],fill=shade(rgb,.64));sd.line((x+1,y+2,x+2,y+4),fill='#f4e5af');sd.point((x+2,y+1),fill=shade(rgb,1.25))
  save(seedim,A/'textures/item'/f'{seed}.png')
  for name in (ident,seed):write(A/'models/item'/f'{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'chao_garden:item/{name}'}})
  source=Image.open(A/'textures/block/garden_fruit_seedling.png').convert('RGBA');source.putpixel((7,6),rgb+(255,));source.putpixel((8,7),rgb+(255,));save(source,A/'textures/block'/f'{ident}_seedling.png')
  for age in range(4):
   texture=Image.new('RGBA',(16,16));d=ImageDraw.Draw(texture);c=tuple(round(a*(age/3)+b*(1-age/3)) for a,b in zip(rgb,(103,142,67)))
   for y in range(16):d.line((0,y,15,y),fill=shade(c,.9+y*.006))
   if ident.startswith('hyper_'):
    for y in (4,11):d.rectangle((0,y,15,y+1),fill='#e6d68b')
   if ident=='shiny_fruit':d.polygon([(7,2),(9,6),(13,8),(9,10),(7,14),(5,10),(1,8),(5,6)],fill='#f0deb2')
   save(texture,A/'textures/block'/f'{ident}_pod_{age}.png')
   write(A/'models/block'/f'{ident}_pod_{age}.json',replace(json.loads((A/'models/block'/f'garden_fruit_pod_{age}.json').read_text()),'garden_fruit',ident))
  for suffix in ('_seedling','_tree_crown','_pod'):
   write(D/'loot_table/blocks'/f'{ident}{suffix}.json',replace(json.loads((D/'loot_table/blocks'/f'garden_fruit{suffix}.json').read_text()),'garden_fruit',ident))
  write(D/'recipe'/f'{seed}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'chao_garden:'+ident}],'result':{'id':'chao_garden:'+seed,'count':1}})
  for key,name in [(ident,ident.replace('gba_','Tiny Garden ').replace('_',' ').title()),(seed,ident.replace('gba_','Tiny Garden ').replace('_',' ').title()+' Seed')]:lang['item.chao_garden.'+key]=name
  for suffix,label in [('_seedling','Seedling'),('_tree_crown','Tree Crown'),('_pod','Hanging Fruit')]:lang['block.chao_garden.'+ident+suffix]=lang['item.chao_garden.'+ident]+' '+label
 # Real additional gene samples, generated with the same established DNA art vocabulary.
 genes.NAMES.extend(EXTRA_GENES);genes.COLORS.extend(GENE_COLORS)
 for index,name in enumerate(EXTRA_GENES,21):
  ident=name+'_animal_gene';save(genes.vial(index),A/'textures/item'/f'{ident}.png');write(A/'models/item'/f'{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'chao_garden:item/{ident}'}});lang['item.chao_garden.'+ident]=name.title()+' Animal Genes'
  # These ten samples never had legacy aliases; remove development-only leftovers.
  (A/'models/item'/f'{name}_animal.json').unlink(missing_ok=True);(A/'textures/item'/f'{name}_animal.png').unlink(missing_ok=True);lang.pop('item.chao_garden.'+name+'_animal',None)
 # All wearable and care items have their own readable inventory sprite.
 for ident in ACCESSORIES+GIFTS+['eggshell_hat']:
  save(sprite(ident),A/'textures/item'/f'{ident}.png');write(A/'models/item'/f'{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'chao_garden:item/{ident}'}});lang['item.chao_garden.'+ident]=ident.replace('_accessory','').replace('_',' ').title()
 palette=Image.new('RGBA',(16,16))
 for i,rgb in enumerate(PALETTE.values()):ImageDraw.Draw(palette).rectangle(((i%4)*4,(i//4)*4,(i%4)*4+3,(i//4)*4+3),fill=rgb+(255,))
 save(palette,A/'textures/entity/chao/cwe_accessories.png')
 for ident in ACCESSORIES+['hoodie_sleeve_left','hoodie_sleeve_right','eggshell_hat','medal_race','medal_karate']:write(A/'models/entity/cwe_accessories'/f'{ident}.json',mesh(ident))
 for ident in NEW_TOYS:
  write(A/'models/block'/f'{ident}_idle.json',toy_model(ident));variants={}
  for frame in range(4):write(A/'models/block'/f'{ident}_play_{frame}.json',toy_model(ident,True,frame))
  for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
   for playing in (False,True):
    for frame in range(4):variants[f'facing={facing},frame={frame},playing={str(playing).lower()}']={'model':f'chao_garden:block/{ident}_play_{frame}' if playing else f'chao_garden:block/{ident}_idle','y':angle}
  write(A/'blockstates'/f'{ident}.json',{'variants':variants});write(A/'models/item'/f'{ident}.json',{'parent':f'chao_garden:block/{ident}_idle'});write(D/'loot_table/blocks'/f'{ident}.json',orchard.loot(ident));lang['item.chao_garden.'+ident]=ident.replace('_',' ').title();lang['block.chao_garden.'+ident]=lang['item.chao_garden.'+ident]
 # Additive tags ensure new orchard wood supports actual vanilla leaf distance.
 for tag in ('logs','logs_that_burn','mineable/axe'):orchard.merge_tag(ROOT/'src/main/resources/data/minecraft/tags/block'/f'{tag}.json',['chao_garden:'+x+'_tree_crown' for x in FRUITS])
 orchard.merge_tag(ROOT/'src/main/resources/data/minecraft/tags/block/saplings.json',['chao_garden:'+x+'_seedling' for x in FRUITS])
 orchard.merge_tag(D/'tags/item/garden_food.json',['chao_garden:'+x for x in FRUITS])
 # Exact boundary:31 canonical samples and only the21 aliases that existed before0.19.
 original=genes.NAMES[:21]
 write(D/'tags/item/animal_genes.json',{'replace':False,'values':['chao_garden:'+x+suffix for x in original for suffix in ('_animal_gene','_animal')]+['chao_garden:'+x+'_animal_gene' for x in EXTRA_GENES]})
 write(A/'lang/en_us.json',lang)
 catalog_path=ROOT/'docs/fruit_catalog.json';catalog=json.loads(catalog_path.read_text())
 catalog['fruits']=[x for x in catalog['fruits'] if x['fruit'] not in FRUITS]+[{'fruit':ident,'seed':ident+'_seed','color':base,'source':'rotating_black_market_cwe_fruit'} for ident,base in zip(FRUITS,COLORS)]
 catalog['format_version']=4;write(catalog_path,catalog)
 item_path=ROOT/'docs/fruit_items_015.json';items=json.loads(item_path.read_text());write(item_path,list(dict.fromkeys(items+FRUITS+[x+'_seed' for x in FRUITS])))
 write(ROOT/'docs/cwe_content_019.json',{'fruits':FRUITS,'genes':EXTRA_GENES,'accessories':ACCESSORIES,'gifts':GIFTS,'toys':NEW_TOYS,'equipment_slots':{'head':'feet','face':'offhand','neck':'legs','body':'chest','hat':'head'},'animal_mobs_added':False,'dna_attraction':'one whole fruit consumed for one weighted DNA sample after600ticks; collectible unowned dropped fruit only'})
 contact_sheet()
 print('CWE019:13fruit orchards,10DNA samples,10real accessories,21specialgifts,16physicaltoys,eggshell hat')
if __name__=='__main__':main()
