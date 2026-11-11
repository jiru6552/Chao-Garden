"""Extend the existing native 16px item-art system with tier collars and multi-core drives."""
from pathlib import Path
import json
from PIL import Image, ImageDraw
from item_sprite_art import drive, save_sprite, shade

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
DATA=ROOT/'src/main/resources/data/chao_garden'
COLORS={'swim':'#efcb43','fly':'#9864d9','run':'#58b957','power':'#e85863'}
lang=json.loads((ASSETS/'lang/en_us.json').read_text())
icons=[]
def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n')
def item(id,name,tier,stat=None):
    im=drive(COLORS.get(stat,'#63d3db'));d=ImageDraw.Draw(im)
    # Wider single core, with tier-specific collars; the original four stay unchanged.
    if stat:
        color=COLORS[stat]
        d.rectangle((4,4,11,11),fill=color)
        d.line((4,4,4,11),fill='#e6edda');d.line((11,4,11,11),fill='#29424a')
        d.line((7,4,7,11),fill='#33434b');d.line((8,4,8,11),fill='#43525b')
    else:
        for x,color in zip((4,6,8,10),COLORS.values()):
            d.rectangle((x,4,x+1,11),fill=color);d.line((x,5,x,10),fill='#31414b')
    collar='#d8e9ed' if tier in ('solid','chaotic') else '#f5ce56'
    for y in (1,13):d.line((3,y,12,y),fill=collar)
    if tier.startswith('super'):
        for y in (2,12):d.line((3,y,12,y),fill='#bd852d')
        d.point((2,1),fill='#fff0b1');d.point((13,13),fill='#fff0b1')
    if tier.startswith('unstable'):
        for y in (1,13):
            d.line((3,y,12,y),fill='#f1ba34')
            for x in (4,7,10):d.point((x,y),fill='#2c243d')
        d.line(((5,4),(9,6),(6,8),(10,10)),fill='#fff5d5',width=1)
    if tier=='perfect':
        d.polygon(((8,2),(9,6),(13,7),(9,9),(8,13),(6,9),(2,7),(6,6)),fill='#fff0a0')
        d.polygon(((8,4),(9,7),(11,7),(8,9),(6,7)),fill='#fafaff')
    save_sprite(im,ASSETS/f'textures/item/{id}.png');icons.append((name,im))
    write(ASSETS/f'models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'chao_garden:item/{id}'}})
    lang['item.chao_garden.'+id]=name
def shapeless(id,ingredients):
    write(DATA/f'recipe/{id}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':'chao_garden:'+i} for i in ingredients],'result':{'id':'chao_garden:'+id,'count':1}})
for stat in COLORS:
    previous=stat+'_drive'
    for tier,count in (('solid',4),('super_solid',9),('unstable_solid',9)):
        id=f'{tier}_{stat}_drive';item(id,tier.replace('_',' ').title()+' '+stat.title()+' Drive',tier,stat)
        shapeless(id,[previous]*count);previous=id
for tier,previous,count in (('chaotic',None,4),('super_chaotic','chaotic_drive',9),('unstable_chaotic','super_chaotic_drive',9)):
    id=tier+'_drive';item(id,tier.replace('_',' ').title()+' Drive',tier)
    shapeless(id,[s+'_drive' for s in COLORS] if previous is None else [previous]*count)
item('perfect_drive','Perfect Drive','perfect')
write(DATA/'tags/item/unstable_solid_drives.json',{'replace':False,'values':[f'chao_garden:unstable_solid_{s}_drive' for s in COLORS]})
write(DATA/'recipe/perfect_drive.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['SCS','CNC','SCS'],'key':{'S':{'tag':'chao_garden:unstable_solid_drives'},'C':{'item':'chao_garden:unstable_chaotic_drive'},'N':{'item':'minecraft:nether_star'}},'result':{'id':'chao_garden:perfect_drive','count':1}})
write(ASSETS/'lang/en_us.json',lang)
if __name__=='__main__':
    print('Generated 16 combined-drive models, sprites, recipes and names; base assets unchanged.')
