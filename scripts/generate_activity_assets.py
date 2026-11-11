#!/usr/bin/env python3
"""Original pixel textures, cuboid models and survival recipes for Chao activities."""
from pathlib import Path
import io
import json
import os
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
DATA = ROOT / 'src/main/resources/data/chao_garden'


def put(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + '.writing')
    with temporary.open('wb') as handle:
        handle.write(payload)
        handle.flush()
        os.fsync(handle.fileno())
    os.replace(temporary, path)


def write_json(path, value):
    put(path, (json.dumps(value, indent=2) + '\n').encode())


def png(name, painter, item=False):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0) if item else (232, 235, 222, 255))
    painter(ImageDraw.Draw(im))
    output = io.BytesIO()
    im.save(output, format='PNG')
    put(ASSETS / 'textures' / ('item' if item else 'block') / (name+'.png'), output.getvalue())


def wood(d):
    d.rectangle((0, 0, 15, 15), fill='#957954')
    for y in (3, 8, 13):
        d.line((0, y, 15, y), fill='#7d6548')
        d.line((0, y+1, 15, y+1), fill='#a88a60')
    d.rectangle((2, 1, 4, 2), fill='#a88a60')
    d.rectangle((11, 10, 13, 11), fill='#7d6548')


def rail(d, color):
    d.rectangle((0, 0, 15, 15), fill=color)
    for x in range(0, 16, 4):
        d.rectangle((x, 0, x+1, 1), fill='#eef4e8')
        d.rectangle((x+2, 2, min(15, x+3), 3), fill='#eef4e8')
    d.line((0, 8, 15, 8), fill='#eef4e8')


def emblem(d):
    d.rectangle((0, 0, 15, 15), fill='#ddbf7d')
    d.rectangle((1, 1, 14, 14), fill='#315d6b')
    d.rectangle((3, 5, 12, 12), fill='#78ced4')
    d.rectangle((6, 3, 9, 4), fill='#78ced4')
    d.rectangle((4, 7, 6, 10), fill='#f4fbf5')
    d.rectangle((9, 7, 11, 10), fill='#f4fbf5')
    d.line((5, 7, 5, 10), fill='#24414c')
    d.line((10, 7, 10, 10), fill='#24414c')


def flag(d):
    # The silhouette comes from the model's stepped cloth, not a rectangle
    # with a triangle painted on it. Quiet red folds keep it legible at 16px.
    d.rectangle((0, 0, 15, 15), fill='#ca3235')
    d.rectangle((0, 0, 1, 15), fill='#eb5a48')
    d.rectangle((2, 0, 4, 15), fill='#df413a')
    d.rectangle((7, 0, 9, 15), fill='#b82731')
    d.rectangle((10, 0, 15, 15), fill='#9e202c')
    d.line((0, 0, 15, 0), fill='#f6795c')
    d.line((0, 6, 15, 6), fill='#95212c')
    for y in (1, 3, 5):
        d.point((0, y), fill='#f7a573')


def pole(d):
    d.rectangle((0, 0, 15, 15), fill='#8b9da6')
    d.line((0, 0, 0, 15), fill='#dce5df')
    d.line((1, 0, 1, 15), fill='#768b98')
    d.line((0, 0, 15, 0), fill='#eaf0e6')
    d.line((0, 15, 15, 15), fill='#607582')


def mat(d):
    d.rectangle((0, 0, 15, 15), fill='#397ca7')
    d.rectangle((1, 1, 14, 14), fill='#d7e7e1')
    d.rectangle((3, 3, 12, 12), fill='#eef3e5')
    d.line((3, 11, 11, 11), fill='#d7e7e1')
    d.rectangle((6, 6, 9, 9), fill='#82b8c8')


def bell(d):
    d.rectangle((7, 1, 8, 3), fill='#e3ca8d')
    d.rectangle((6, 3, 9, 4), fill='#294c67')
    d.rectangle((5, 5, 10, 7), fill='#72c9df')
    d.rectangle((4, 8, 11, 10), fill='#4c9fbc')
    d.rectangle((3, 11, 12, 12), fill='#294c67')
    d.rectangle((4, 10, 11, 11), fill='#72c9df')
    d.rectangle((6, 5, 7, 7), fill='#bef1ed')
    d.rectangle((7, 13, 8, 14), fill='#d5b16a')


def element(a, b, texture):
    sx, sy, sz = [b[i]-a[i] for i in range(3)]
    dimensions = {'north':(sx,sy), 'south':(sx,sy), 'west':(sz,sy),
                  'east':(sz,sy), 'up':(sx,sz), 'down':(sx,sz)}
    return {'from':a, 'to':b, 'faces':{side:{'texture':'#'+texture,
            'uv':[0,0,*dimensions[side]]} for side in dimensions}}


def garden_sign():
    board = element([4,9,6],[12,16,10],'wood')
    for side in ('north', 'south'):
        board['faces'][side] = {'texture':'#emblem', 'uv':[0,0,16,16]}
    return board


def race_pennant():
    """A slim pole and a genuine, thin, pixel-stepped triangular pennant."""
    parts = [element([4, 0, 7], [6, 16, 9], 'pole')]
    # Seven horizontal one-unit rows form an isosceles point to the right.
    # A quarter-unit overlap attaches the cloth firmly to its two-unit pole.
    ends = (7, 10, 13, 16, 13, 10, 7)
    for row, end in enumerate(ends):
        cloth = element([5.75, 9+row, 7.625], [end, 10+row, 8.375], 'flag')
        for side in ('north', 'south'):
            cloth['faces'][side]['uv'] = [0, 6-row, end-5.75, 7-row]
        parts.append(cloth)
    return parts


def main():
    png('activity_wood', wood)
    png('activity_start', lambda d: rail(d, '#4f9b7a'))
    png('activity_finish', lambda d: rail(d, '#bb626a'))
    png('garden_emblem', emblem)
    png('race_flag', flag)
    png('race_pole', pole)
    png('karate_mat', mat)
    png('garden_bell', bell, item=True)
    textures = {'wood':'chao_garden:block/activity_wood', 'emblem':'chao_garden:block/garden_emblem',
                'start':'chao_garden:block/activity_start', 'finish':'chao_garden:block/activity_finish',
                'flag':'chao_garden:block/race_flag', 'pole':'chao_garden:block/race_pole',
                'mat':'chao_garden:block/karate_mat'}
    models = {
        'garden_block': [element([2,0,2],[14,2,14],'wood'),
                         element([3,2,3],[13,8,13],'wood'),
                         element([6,8,6],[10,10,10],'wood'),
                         garden_sign()],
        'race_start': [element([6,0,6],[10,16,10],'wood'),
                       element([0,5,7],[16,8,9],'start'),
                       element([0,12,7],[16,15,9],'start')],
        'race_finish': [element([6,0,6],[10,16,10],'wood'),
                        element([0,5,7],[16,8,9],'finish'),
                        element([0,12,7],[16,15,9],'finish')],
        'race_marker': race_pennant(),
        'karate_mat': [element([0,0,0],[16,1,16],'mat')],
    }
    for name, parts in models.items():
        model = {'parent':'minecraft:block/block', 'textures':{**textures, 'particle':textures['mat' if name=='karate_mat' else 'wood']},
                 'elements':parts, 'display':{'gui':{'rotation':[30,225,0], 'translation':[0,0,0], 'scale':[.65,.65,.65]},
                                            'fixed':{'rotation':[0,180,0], 'translation':[0,0,0], 'scale':[.5,.5,.5]}}}
        if name == 'race_marker':
            model['textures']['particle'] = textures['flag']
            # A front-facing inventory view preserves the point at native
            # inventory scale; the held and placed model remains fully 3D.
            model['display']['gui'] = {'rotation':[0,0,0], 'translation':[0,0,0], 'scale':[.8,.8,.8]}
            model['gui_light'] = 'front'
        write_json(ASSETS/'models/block'/f'{name}.json', model)
        # axis selects the actual row direction; the open lobby state leaves
        # the authored geometry intact. A partial variant also matches open.
        write_json(ASSETS/'blockstates'/f'{name}.json', {'variants':{
            'axis=x':{'model':'chao_garden:block/'+name},
            'axis=z':{'model':'chao_garden:block/'+name, 'y':90}}})
        write_json(ASSETS/'models/item'/f'{name}.json', {'parent':'chao_garden:block/'+name})
        write_json(DATA/'loot_table/blocks'/f'{name}.json', {
            'type':'minecraft:block', 'pools':[{'rolls':1,
             'conditions':[{'condition':'minecraft:survives_explosion'}],
             'entries':[{'type':'minecraft:item','name':'chao_garden:'+name}]}]})
    write_json(ASSETS/'models/item/garden_bell.json', {'parent':'minecraft:item/generated',
        'textures':{'layer0':'chao_garden:item/garden_bell'}})
    recipes = {
        'garden_block': (['P P','PCP',' E '], {'P':{'tag':'minecraft:planks'}, 'C':{'item':'minecraft:copper_ingot'},'E':{'item':'minecraft:emerald'}}, 1),
        'garden_bell': ([' I ','CCC',' C '], {'I':{'item':'minecraft:iron_nugget'},'C':{'item':'minecraft:copper_ingot'}}, 1),
        'race_start': (['WWW','SDS'], {'W':{'item':'minecraft:white_wool'},'S':{'item':'minecraft:stick'},'D':{'item':'minecraft:green_dye'}}, 3),
        'race_finish': (['WWW','SDS'], {'W':{'item':'minecraft:white_wool'},'S':{'item':'minecraft:stick'},'D':{'item':'minecraft:red_dye'}}, 3),
        'race_marker': ([' W',' S',' S'], {'W':{'item':'minecraft:red_wool'},'S':{'item':'minecraft:stick'}}, 4),
        'karate_mat': (['WWW',' L '], {'W':{'item':'minecraft:blue_wool'},'L':{'item':'minecraft:leather'}}, 7),
    }
    for name, (pattern,key,count) in recipes.items():
        write_json(DATA/'recipe'/f'{name}.json', {'type':'minecraft:crafting_shaped', 'category':'misc',
            'pattern':pattern, 'key':key, 'result':{'id':'chao_garden:'+name,'count':count}})
    lang = ASSETS/'lang/en_us.json'
    values = json.loads(lang.read_text()) if lang.exists() else {}
    names = {'garden_block':'Chao Garden Block', 'race_start':'Race Starting Line',
             'race_finish':'Race Finish Line', 'race_marker':'Race Marker Flag',
             'karate_mat':'Chao Karate Mat', 'garden_bell':'Garden Bell'}
    for name, label in names.items():
        values[('item' if name=='garden_bell' else 'block')+'.chao_garden.'+name] = label
    write_json(lang, values)
    # Keep the garden terminal's PC transporter model authoritative when the
    # complete legacy activity resource generator is rerun.
    from generate_transporter_assets import main as generate_transporter
    generate_transporter()
    print('Generated activity resources and PC-style transporter garden terminal.')


if __name__ == '__main__':
    main()
