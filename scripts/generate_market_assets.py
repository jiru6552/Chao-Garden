"""Original Black Market Chao skin and toy art; safe to run after base generators."""
from pathlib import Path
import json
import io
import os
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2)+'\n')


def save_png(picture, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    buffer=io.BytesIO()
    picture.save(buffer, format='PNG')
    payload=buffer.getvalue()
    with Image.open(io.BytesIO(payload)) as check:check.verify()
    temporary=path.with_name(path.name+'.writing')
    with temporary.open('wb') as handle:
        handle.write(payload);handle.flush();os.fsync(handle.fileno())
    os.replace(temporary,path)


def main():
    # Separate quiet atlas cells preserve model-unit UV density on all cuboids.
    colors = [(101,177,176), (60,80,68), (85,106,80), (204,177,91),
              (15,23,25), (159,178,173), (105,77,49), (151,190,182),
              (226,210,119)]
    atlas = Image.new('RGBA', (128,128), (0,0,0,255))
    d = ImageDraw.Draw(atlas)
    for tile, color in enumerate(colors):
        x,y=(tile%4)*32,(tile//4)*32
        d.rectangle((x,y,x+31,y+31),fill=(*color,255))
        lighter=tuple(min(255,round(c*1.035)) for c in color)
        darker=tuple(round(c*.96) for c in color)
        d.rectangle((x,y,x+31,y+1),fill=(*lighter,255))
        d.rectangle((x,y+14,x+31,y+15),fill=(*darker,255))
    save_png(atlas, ASSETS/'textures/entity/black_market_trader.png')

    # Ball: a small original block-built toy that remains three-dimensional
    # when the play session displays it as a temporary world prop.
    ball = Image.new('RGBA',(16,16),(89,180,209,255));b=ImageDraw.Draw(ball)
    b.rectangle((6,0,9,15),fill=(244,207,98,255));b.rectangle((0,6,15,9),fill=(244,207,98,255))
    for x,y in ((1,1),(11,1),(1,11),(11,11)):b.rectangle((x,y,x+3,y+3),fill=(230,109,104,255))
    save_png(ball, ASSETS/'textures/item/chao_ball.png')
    faces={side:{'texture':'#all','uv':[0,0,8,8]} for side in ('north','south','west','east','up','down')}
    write_json(ASSETS/'models/item/chao_ball.json',{
        'parent':'minecraft:block/block','textures':{'all':'chao_garden:item/chao_ball','particle':'chao_garden:item/chao_ball'},
        'elements':[{'from':[4,4,4],'to':[12,12,12],'faces':faces}],
        'display':{'gui':{'rotation':[30,225,0],'scale':[1.1,1.1,1.1]},
                   'ground':{'translation':[0,2,0],'scale':[.65,.65,.65]},
                   'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,1,0],'scale':[.6,.6,.6]},
                   'thirdperson_righthand':{'rotation':[10,45,0],'translation':[0,2,0],'scale':[.6,.6,.6]}}})
    rattle=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(rattle)
    d.rectangle((7,7,9,14),fill='#ad7247');d.rectangle((6,12,10,14),fill='#e9b969')
    d.rectangle((4,2,12,8),fill='#eda76d');d.rectangle((3,3,13,6),fill='#eda76d')
    d.rectangle((4,4,12,5),fill='#73c9c2');d.rectangle((5,2,7,3),fill='#ffe3a5')
    save_png(rattle,ASSETS/'textures/item/chao_rattle.png')
    maracas=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(maracas)
    for cx,cy,color in ((4,4,'#df826c'),(10,5,'#78bfc7')):
        d.line((cx+1,cy+3,cx+3,14),fill='#a4714f',width=2)
        d.rectangle((cx-2,cy-2,cx+2,cy+3),fill=color)
        d.rectangle((cx-1,cy-3,cx+1,cy+4),fill=color)
        d.line((cx-2,cy,cx+2,cy),fill='#f5d986',width=2)
    save_png(maracas,ASSETS/'textures/item/chao_maracas.png')
    for name in ('chao_rattle','chao_maracas'):
        write_json(ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':f'chao_garden:item/{name}'}})
    write_json(ASSETS/'models/item/black_market_trader_spawn_egg.json',{'parent':'minecraft:item/template_spawn_egg'})
    language_path=ASSETS/'lang/en_us.json'
    language=json.loads(language_path.read_text()) if language_path.exists() else {}
    language.update({'entity.chao_garden.black_market_trader':'Black Market Trader',
                     'item.chao_garden.black_market_trader_spawn_egg':'Black Market Trader Spawn Egg',
                     'item.chao_garden.chao_ball':'Chao Play Ball',
                     'item.chao_garden.chao_rattle':'Chao Rattle',
                     'item.chao_garden.chao_maracas':'Chao Maracas'})
    write_json(language_path,language)
    print('Generated original Black Market trader atlas, three toy models/icons and names.')


if __name__=='__main__':main()
