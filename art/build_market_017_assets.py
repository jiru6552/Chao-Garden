#!/usr/bin/env python3
"""New repository-native 16px market props and the purple, dimpled Dreamcast Moon coat."""
from pathlib import Path
import json, math, colorsys
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/chao_garden'
TOYS=['chao_trumpet','chao_drum','chao_jack_in_box','chao_radio','chao_tv']
def sprite(name):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 dark='#333342';edge='#252735';gold='#e8b94e';hi='#ffe5a0'
 if name=='chao_trumpet':
  d.rectangle((2,7,10,9),fill=gold);d.rectangle((2,6,3,10),fill=hi)
  d.polygon([(10,7),(13,4),(14,4),(14,12),(13,12),(10,9)],fill=gold)
  d.line((14,4,14,12),fill=hi);d.rectangle((13,6,13,10),fill='#8c601e')
  d.rectangle((5,5,5,7),fill=hi);d.rectangle((8,5,8,7),fill=hi)
  d.line([(4,10),(4,12),(9,12),(9,10)],fill=gold,width=2)
 elif name=='chao_drum':
  d.rectangle((3,5,12,12),fill='#b64f51');d.ellipse((2,3,13,7),fill='#f4dbad',outline=dark)
  d.rectangle((2,11,13,13),fill=gold);d.line((4,7,6,11),fill=hi);d.line((10,7,8,11),fill=hi)
  d.line((1,1,7,5),fill='#9f7650');d.line((14,1,9,5),fill='#9f7650')
 elif name=='chao_jack_in_box':
  d.rectangle((3,9,12,14),fill='#bc4a5b',outline=edge);d.rectangle((4,9,11,10),fill='#e8bd63')
  d.polygon([(2,8),(5,6),(13,7),(12,9)],fill='#e0ad52',outline=edge)
  d.line([(7,9),(9,8),(6,7),(9,6),(7,5)],fill='#b7c7cf')
  d.ellipse((5,1,10,6),fill='#74ced7',outline=edge);d.point((6,3),fill=edge);d.point((9,3),fill=edge)
  d.rectangle((7,13,8,14),fill=hi)
 elif name=='chao_radio':
  d.rectangle((2,5,13,13),fill='#a56d45',outline=edge);d.rectangle((4,2,11,4),fill=dark);d.rectangle((5,3,10,4),fill='#d1a26b')
  d.ellipse((3,7,8,12),fill=dark);d.point((5,8),fill='#647983');d.point((7,10),fill='#647983')
  d.rectangle((10,7,12,8),fill='#8bd4ba');d.point((11,11),fill=gold);d.line((12,5,14,1),fill='#b6c3c8')
 elif name=='chao_tv':
  d.rectangle((1,4,14,13),fill='#756065',outline=edge);d.rectangle((2,5,10,11),fill='#90cfd1',outline=dark)
  d.rectangle((3,6,9,7),fill='#bdded6');d.rectangle((3,9,9,10),fill='#79b1c0')
  d.point((12,7),fill=gold);d.point((12,10),fill=gold);d.line([(5,1),(8,4),(11,1)],fill='#9aa8b1')
  d.rectangle((3,14,4,14),fill=dark);d.rectangle((11,14,12,14),fill=dark)
 return im

def moon_coat(source,target,kind):
 im=Image.open(source).convert('RGBA');px=im.load()
 for y in range(im.height):
  for x in range(im.width):
   r,g,b,a=px[x,y]
   # Isolate the amethyst coat's purple body; keep original eyes, wings and trim.
   if a and b>g*1.2 and r>g*1.15 and b>85:
    lum=(r+g+b)/3/255
    base=(164,138,201)
    f=.62+.65*lum
    xx=x%6;yy=y%6
    dist=(xx-2.5)**2+(yy-2.5)**2
    if dist<2.3:f*=.71
    elif dist<5 and yy<2.5:f*=1.14
    px[x,y]=tuple(min(255,round(c*f)) for c in base)+(a,)
 target.parent.mkdir(parents=True,exist_ok=True);im.save(target,optimize=True)

def main():
 from prune_retired_resources import prune_retired_resources
 prune_retired_resources(RES.parents[1])
 for toy in TOYS:
  p=RES/'textures/item'/f'{toy}.png';p.parent.mkdir(parents=True,exist_ok=True);sprite(toy).save(p)
  (RES/'models/item'/f'{toy}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'chao_garden:item/{toy}'}},indent=2)+'\n')
 # The suffixes cover inherited two-tone and shiny Moon offspring as well as purchased eggs.
 entity=RES/'textures/entity'
 for suffix in ['_mono','_two','_mono_shiny','_two_shiny']:
  for folder in [entity/'chao',entity/'eggs']:
   moon_coat(folder/f'jewel_amethyst{suffix}.png',folder/f'jewel_moon{suffix}.png','atlas')
  for geometry in (entity/'chao/official/coats').iterdir():
   src=geometry/f'jewel_amethyst{suffix}.png'
   if src.is_file():moon_coat(src,geometry/f'jewel_moon{suffix}.png','official')
 # Rounded egg silhouette matches the existing jewel egg icon; recolor body pixels only.
 src=RES/'textures/item/amethyst_jewel_egg.png';dst=RES/'textures/item/moon_jewel_egg.png'
 moon_coat(src,dst,'item')
 (RES/'models/item/moon_jewel_egg.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/moon_jewel_egg'}},indent=2)+'\n')
 lang=RES/'lang/en_us.json';data=json.loads(lang.read_text())
 for key,title in zip(TOYS,['Chao Trumpet','Chao Drum','Chao Jack-in-a-box','Chao Radio','Chao TV']):data['item.chao_garden.'+key]=title
 data['item.chao_garden.moon_jewel_egg']='Moon Jewel Chao Egg'
 lang.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
 ledger=ROOT/'scripts/resource_items.json'
 entries=json.loads(ledger.read_text())
 entries=[x for x in entries if x not in ['mushroom_seed','mushroom_alt_seed']]
 hats=['apple_hat','cardboard_box_hat','pan_hat','paper_bag_hat','empty_can_hat','stump_hat','flower_pot_hat','bucket_hat','watermelon_hat','red_wool_beanie','blue_wool_beanie','black_white_wool_beanie']
 entries=list(dict.fromkeys(entries+TOYS+hats+['moon_jewel_egg']))
 ledger.write_text(json.dumps(entries,indent=2)+'\n')
 for fruit in ['mushroom','mushroom_alt']:
  (ROOT/f'src/main/resources/data/chao_garden/recipe/{fruit}_seed.json').unlink(missing_ok=True)
  for suffix in ['seedling','tree_crown']:
   path=ROOT/f'src/main/resources/data/chao_garden/loot_table/blocks/{fruit}_{suffix}.json'
   path.write_text(path.read_text().replace(f'chao_garden:{fruit}_seed',f'chao_garden:{fruit}'))
 print('Built five 16px toy sprites/models and 89 Moon coat/egg resources; retired mushroom seed sources.')
if __name__=='__main__':main()
