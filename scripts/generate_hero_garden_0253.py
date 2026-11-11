#!/usr/bin/env python3
"""Author the three native Hero masonry textures and its wildlife-free daylight biome.

The included reference screenshots are studies only. These pixels are drawn here,
not copied from game screenshots, and no existing room or dimension is rewritten.
"""
from pathlib import Path
import json
import math
import random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/chao_garden'
TEXTURES = ASSETS / 'textures/block/hero0253'
SIZE = 64
IDS = ('hero_marble', 'hero_marble_trim', 'hero_fountain_emblem')


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')


def marble():
    im = Image.new('RGBA', (SIZE, SIZE), '#e9e7f1')
    d = ImageDraw.Draw(im)
    # Soft veins continue across repeated tile boundaries. Mortar is deliberately
    # restrained so the pale architecture reads as one lavender stone material.
    for y in range(SIZE):
        for x in range(SIZE):
            wave = math.sin((x+y*1.7)*math.pi/32) + .45*math.sin((x*2-y)*math.pi/16)
            delta = round(wave*2)
            im.putpixel((x,y),(233+delta,231+delta,241+delta,255))
    d.line((0,0,63,0), fill='#d1cddd')
    d.line((0,0,0,63), fill='#d1cddd')
    d.line((1,1,63,1), fill='#faf7ff')
    d.line((1,1,1,63), fill='#faf7ff')
    return im


def trim():
    im = Image.new('RGBA', (SIZE, SIZE), '#bcb1d6')
    d = ImageDraw.Draw(im)
    for y, color in ((0,'#f4f0ff'),(3,'#ddd7ec'),(7,'#8e83ac'),(55,'#8e83ac'),(59,'#ddd7ec'),(63,'#f4f0ff')):
        d.line((0,y,63,y), fill=color, width=2)
    # Each complete rosette sits inside one block; its petals never depend on a
    # neighboring face, so turns and column caps do not cut off half a circle.
    for cx in (16,48):
        for i in range(8):
            angle=i*math.pi/4
            px=cx+9*math.cos(angle); py=32+9*math.sin(angle)
            d.ellipse((px-4,py-4,px+4,py+4),fill='#e3ddef',outline='#a496be')
        d.ellipse((cx-4,28,cx+4,36),fill='#f5edc7',outline='#b7a874')
    return im


def emblem():
    im = marble(); d = ImageDraw.Draw(im)
    d.rectangle((3,3,60,60),fill='#9c91bb',outline='#f9f2dc',width=2)
    d.rectangle((7,7,56,56),fill='#577f9e',outline='#ccc2df',width=2)
    # Original code-native Chao silhouette, cyan against the fountain's blue.
    d.ellipse((23,39,41,52),fill='#b9e5e1',outline='#d4f4e7')
    d.ellipse((18,23,46,42),fill='#a5ddd9',outline='#d1f1e7')
    d.polygon(((27,27),(35,13),(40,29)),fill='#a5ddd9')
    d.line((27,27,35,13,40,29),fill='#d1f1e7',width=1)
    for x in (26,37): d.ellipse((x,29,x+3,35),fill='#40667e')
    d.ellipse((31,8,37,14),fill='#f2df9e',outline='#bcb58d')
    return im


def build():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    write(ASSETS/'hero_garden_decor0253.json',{'version':'0.25.3','texturePrefix':'block/hero0253/',
        'blocks':[{'id':name,'item':True} for name in IDS]})
    textures = (marble(),trim(),emblem())
    for name, im in zip(IDS,textures):
        im.save(TEXTURES/f'{name}.png')
        ref=f'chao_garden:block/hero0253/{name}'
        if name=='hero_fountain_emblem':
            model={'parent':'minecraft:block/cube','textures':{'particle':ref,'down':'chao_garden:block/hero0253/hero_marble',
                'up':'chao_garden:block/hero0253/hero_marble','north':'chao_garden:block/hero0253/hero_marble',
                'south':'chao_garden:block/hero0253/hero_marble','west':'chao_garden:block/hero0253/hero_marble','east':ref}}
        else:
            model={'parent':'minecraft:block/cube_all','textures':{'all':ref}}
        write(ASSETS/f'models/block/{name}.json',model)
        write(ASSETS/f'models/item/{name}.json',{'parent':f'chao_garden:block/{name}'})
        write(ASSETS/f'blockstates/{name}.json',{'variants':{'':{'model':f'chao_garden:block/{name}'}}})
        write(RES/f'data/chao_garden/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[
            {'rolls':1,'entries':[{'type':'minecraft:item','name':f'chao_garden:{name}'}],
             'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(RES/'data/chao_garden/worldgen/biome/hero_daylight.json',{
        'has_precipitation':False,'temperature':.8,'downfall':0,
        'effects':{'fog_color':0xffc4de,'sky_color':0xffedb6,'water_color':0x6fbde8,'water_fog_color':0x547da5,
                   'grass_color':0x86c86d,'foliage_color':0x86c86d},
        'carvers':{},'features':[],'spawners':{},'spawn_costs':{},'creature_spawn_probability':0.0})
    locale=ASSETS/'lang/en_us.json'
    data=json.loads(locale.read_text()) if locale.exists() else {}
    data.update({'block.chao_garden.hero_marble':'Hero Garden Marble',
        'block.chao_garden.hero_marble_trim':'Hero Garden Rosette Cornice',
        'block.chao_garden.hero_fountain_emblem':'Hero Garden Fountain Emblem'})
    write(locale,dict(sorted(data.items())))
    print('Hero Garden 0.25.3: 3 native masonry blocks and wildlife-free Hero biome generated.')


if __name__=='__main__': build()
