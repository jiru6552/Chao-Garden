#!/usr/bin/env python3
"""Native lavender column members with bounded texture UVs, plus optional chorus event."""
from pathlib import Path
import json
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
def main():
    image=Image.new('RGBA',(64,64))
    for y in range(64):
        for x in range(64):
            shade=round(5*((x%16)/15))
            image.putpixel((x,y),(206+shade,196+shade,221+shade,255))
    draw=ImageDraw.Draw(image)
    for x in (7,23,39,55):
        draw.line((x,0,x,63),fill='#b5a9c6');draw.line((x+1,0,x+1,63),fill='#e4deee')
    image.save(ASSETS/'textures/block/hero0253/hero_column.png')
    # Non-overlapping octagonal cross-section, sharing the native collision envelope.
    shaft=[([5,0,4],[11,16,5]),([4,0,5],[12,16,11]),([5,0,11],[11,16,12])]
    for part in ('shaft','base','capital'):
        boxes=shaft
        if part=='base':
            boxes=[([2,0,2],[14,3,14]),([3,3,3],[13,5,13])]+[( [a[0],5,a[2]],b) for a,b in shaft]
        if part=='capital':
            boxes=[(a,[b[0],11,b[2]]) for a,b in shaft]+[([3,11,3],[13,13,13]),([2,13,2],[14,16,14])]
        elements=[]
        for lo,hi in boxes:
            faces={}
            for side in ('north','south','east','west','up','down'):
                uv=([lo[0],16-hi[1],hi[0],16-lo[1]] if side in ('north','south')
                    else [lo[2],16-hi[1],hi[2],16-lo[1]] if side in ('east','west')
                    else [lo[0],lo[2],hi[0],hi[2]])
                faces[side]={'uv':uv,'texture':'#stone'}
            elements.append({'from':lo,'to':hi,'faces':faces})
        write(ASSETS/f'models/block/hero_column_{part}.json',
              {'parent':'minecraft:block/block','textures':{'stone':'chao_garden:block/hero0253/hero_column','particle':'chao_garden:block/hero0253/hero_column'},'elements':elements})
    write(ASSETS/'blockstates/hero_column.json',{'variants':{f'part={p}':{'model':f'chao_garden:block/hero_column_{p}'} for p in ('base','shaft','capital')}})
    write(ASSETS/'models/item/hero_column.json',{'parent':'chao_garden:block/hero_column_shaft'})
    write(ROOT/'src/main/resources/data/chao_garden/loot_table/blocks/hero_column.json',
          {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'chao_garden:hero_column'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(ASSETS/'hero_garden_decor0253.json',{'version':'0.25.9','texturePrefix':'block/hero0253/',
        'blocks':[{'id':name,'item':True} for name in ('hero_marble','hero_marble_trim','hero_fountain_emblem','hero_column')]})
    sounds=json.loads((ASSETS/'sounds.json').read_text())
    sounds['chao.classroom_chorus']={'subtitle':'subtitles.chao_garden.chao.classroom_chorus','sounds':[]}
    write(ASSETS/'sounds.json',sounds)
    locale=json.loads((ASSETS/'lang/en_us.json').read_text())
    locale.update({'block.chao_garden.hero_column':'Hero Garden Lavender Column','subtitles.chao_garden.chao.classroom_chorus':'Chao sing together'})
    write(ASSETS/'lang/en_us.json',dict(sorted(locale.items())))
    print('Hero0259: three slim column members; optional full-length classroom chorus event.')
if __name__=='__main__':main()
