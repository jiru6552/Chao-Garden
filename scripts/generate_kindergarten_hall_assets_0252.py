#!/usr/bin/env python3
"""Code-native, repeating school hall finishes; independent of the furniture/door writer."""
from pathlib import Path
import json
import math
import random
import re
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/chao_garden'
TEXTURES = ASSETS / 'textures/block/school0252'
TEXTURES.mkdir(parents=True, exist_ok=True)
SIZE = 128

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')

def surface(color):
    return Image.new('RGBA', (SIZE, SIZE), color)

def grain(im, seed, strength=2):
    rng = random.Random(seed)
    for _ in range(1100):
        x, y = rng.randrange(SIZE), rng.randrange(SIZE)
        r, g, b, a = im.getpixel((x, y)); delta = rng.randint(-strength, strength)
        im.putpixel((x, y), (max(0, min(255, r + delta)), max(0, min(255, g + delta)), max(0, min(255, b + delta)), a))
    return im

def star(draw, x, y, radius, color):
    draw.polygon([(x, y-radius), (x+radius*.22, y-radius*.22), (x+radius, y), (x+radius*.22, y+radius*.22),
                  (x, y+radius), (x-radius*.22, y+radius*.22), (x-radius, y), (x-radius*.22, y-radius*.22)], fill=color)

def stripes(draw, background, accent, metal):
    draw.rectangle((0, 0, 127, 127), fill=background)
    for y in (7, 119):
        draw.line((0, y, 127, y), fill=metal, width=2)
    draw.line((0, 12, 127, 12), fill=accent)
    draw.line((0, 114, 127, 114), fill=accent)

def font(size):
    return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', size)

def paint(name):
    im = surface('#ede6ce'); d = ImageDraw.Draw(im)
    if name == 'foyer_frieze':
        stripes(d, '#b9a46c', '#d4c393', '#efe2b8')
        for x in (32, 96):
            d.ellipse((x-13, 30, x+13, 97), outline='#eddfb0', width=3)
            d.ellipse((x-5, 52, x+5, 68), fill='#918254')
    elif name in ('principal_certificate', 'principal_award', 'classroom_student_work', 'classroom_class_photo', 'clinic_registration', 'clinic_wellness'):
        bg = '#d8d0ab' if name.startswith('principal') else '#e8f1e8' if name.startswith('clinic') else '#73b5cc'
        im = surface(bg); d = ImageDraw.Draw(im)
        d.rectangle((6, 10, 121, 117), fill='#725039', outline='#b3945b', width=3)
        d.rectangle((12, 16, 115, 111), fill='#eee3bf', outline='#cfb976', width=2)
        if name == 'principal_certificate':
            d.text((64, 30), 'CHAO ACADEMY', font=font(10), anchor='mm', fill='#695838')
            d.text((64, 47), 'CERTIFICATE', font=font(11), anchor='mm', fill='#705a38')
            for y, length in ((62, 70), (70, 77), (78, 58)):
                d.line((64-length/2, y, 64+length/2, y), fill='#b29d71', width=2)
            d.polygon([(93, 89), (87, 108), (96, 104), (104, 109), (101, 89)], fill='#7f4963')
            d.ellipse((86, 83, 104, 101), fill='#d4b45d', outline='#a48a42', width=2)
        elif name == 'principal_award':
            d.text((64, 30), 'GARDEN MERIT', font=font(10), anchor='mm', fill='#695838')
            d.polygon([(51, 69), (43, 104), (57, 97), (64, 107), (70, 74)], fill='#547ba2')
            d.polygon([(62, 73), (69, 107), (77, 96), (91, 103), (80, 69)], fill='#6b8eb3')
            d.ellipse((38, 40, 89, 91), fill='#b89a50', outline='#e5cd83', width=3)
            star(d, 64, 65, 16, '#f5df92')
        elif name == 'classroom_student_work':
            d.rectangle((16, 22, 111, 103), fill='#e5f1d4')
            d.ellipse((79, 29, 99, 49), fill='#efcd58')
            d.polygon([(16, 81), (39, 50), (64, 83), (84, 62), (111, 86), (111, 104), (16, 104)], fill='#80b37c')
            d.ellipse((44, 66, 73, 86), fill='#7acbd2', outline='#539daa', width=2)
            d.polygon([(56, 71), (62, 55), (68, 72)], fill='#7acbd2'); d.ellipse((61, 47, 67, 53), fill='#e6c45a')
            for x in (52, 64): d.ellipse((x, 73, x+3, 78), fill='#3a6774')
            d.text((64, 111), 'OUR GARDEN', font=font(8), anchor='mm', fill='#efe6c8')
        elif name == 'classroom_class_photo':
            d.rectangle((16, 22, 111, 103), fill='#92c8d5'); d.rectangle((16, 83, 111, 103), fill='#81a57a')
            for row, y in enumerate((53, 81)):
                for col, x in enumerate((29, 52, 76, 99)):
                    color = ('#a2dfd9', '#f0d18d', '#d7b8de', '#91c4df')[(col+row)%4]
                    d.ellipse((x-8, y-6, x+8, y+6), fill=color, outline='#5e8c9b')
                    d.polygon([(x, y-3), (x+3, y-15), (x+6, y-3)], fill=color)
                    d.ellipse((x, y-21, x+5, y-16), fill='#e9ce73')
                    d.ellipse((x-4, y-1, x-2, y+2), fill='#466978'); d.ellipse((x+3, y-1, x+5, y+2), fill='#466978')
            d.text((64, 111), 'OUR CLASS', font=font(8), anchor='mm', fill='#efe6c8')
        elif name == 'clinic_registration':
            d.text((64, 32), 'REGISTRATION', font=font(10), anchor='mm', fill='#4d8e7e')
            d.rectangle((38, 46, 91, 99), fill='#f7f3dc', outline='#7fa88f', width=2)
            d.rectangle((51, 43, 78, 50), fill='#aac5a8')
            for y in (61, 71, 81, 91):
                d.rectangle((44, y-2, 48, y+2), outline='#73a68e'); d.line((55, y, 84, y), fill='#9fb29b')
        else:
            d.text((64, 31), 'HAPPY & HEALTHY', font=font(9), anchor='mm', fill='#4d8e7e')
            d.ellipse((41, 44, 87, 90), fill='#b6d8bf', outline='#75a58e', width=2)
            d.rectangle((59, 50, 69, 84), fill='#5c9d88'); d.rectangle((47, 62, 81, 72), fill='#5c9d88')
            d.text((64, 103), 'CARE  REST  PLAY', font=font(8), anchor='mm', fill='#789b7d')
    elif name == 'market_shelf':
        im = surface('#685039'); d = ImageDraw.Draw(im)
        for y in range(0, 128, 16):
            d.line((0, y, 127, y), fill='#3c392e', width=2); d.line((0, y+3, 127, y+3), fill='#94754d')
        for x, y in ((23, 27), (76, 89), (96, 53)): d.ellipse((x-5, y-2, x+5, y+2), outline='#4b3d2b')
    elif name == 'fortune_wall':
        im = surface('#48234f'); d = ImageDraw.Draw(im)
        # Each circular sigil fits wholly inside one native wall face. Nothing
        # relies on a neighboring block or a corner to finish a clipped motif.
        for x in (32, 96):
            for y in (32, 96):
                d.polygon([(x, y-27), (x+27, y), (x, y+27), (x-27, y)], outline='#67406e', width=2)
                d.ellipse((x-20, y-20, x+20, y+20), outline='#81517e', width=2)
                d.ellipse((x-15, y-15, x+15, y+15), outline='#67406e', width=1)
                star(d, x, y, 4, '#b29374')
    elif name == 'fortune_dado':
        im = surface('#2d153b'); d = ImageDraw.Draw(im)
        for x in range(0, 128, 32):
            d.rectangle((x+3, 5, x+28, 122), outline='#63406b', width=2)
            d.line((x+7, 12, x+7, 114), fill='#44254e')
        d.rectangle((0, 0, 127, 7), fill='#5d3869'); d.line((0, 8, 127, 8), fill='#b59a65', width=2)
    elif name == 'fortune_frieze':
        stripes(d, '#321641', '#623164', '#c2a66a')
        for x in (16, 80):
            d.ellipse((x-9, 43, x+9, 63), fill='#d6c18d'); d.ellipse((x-4, 39, x+13, 58), fill='#321641')
            star(d, x+30, 74, 9, '#b8a0d1'); star(d, x+5, 90, 3, '#d6c18d')
        d.line((0, 103, 127, 103), fill='#75517e')
    elif name == 'fortune_ceiling':
        im = surface('#271334'); d = ImageDraw.Draw(im)
        d.line((18, 31, 59, 48, 92, 22, 112, 63), fill='#64476d', width=1)
        for x, y, r in ((18, 31, 4), (59, 48, 3), (92, 22, 4), (112, 63, 3), (28, 96, 3), (77, 107, 2)):
            star(d, x, y, r, '#c3b38e')
    elif name == 'fortune_carpet':
        im = surface('#37203f'); d = ImageDraw.Draw(im)
        for x, y in ((32, 32), (96, 96)):
            d.ellipse((x-18, y-18, x+18, y+18), outline='#633d67', width=2)
            d.ellipse((x-13, y-13, x+13, y+13), outline='#7d567a')
            star(d, x, y, 8, '#ad8b74')
        for x, y in ((16, 91), (91, 20)):
            star(d, x, y, 3, '#886d8c')
    elif name == 'clinic_wall':
        im = surface('#e8f1e8'); d = ImageDraw.Draw(im)
        for y in (31, 95): d.line((0, y, 127, y), fill='#dae7df')
    elif name == 'clinic_frieze':
        stripes(d, '#b7ddcf', '#f0f5e9', '#5b9f8e')
        for x in (32, 96):
            d.ellipse((x-19, 44, x+19, 82), outline='#e5f2e5', width=2)
            d.rectangle((x-4, 52, x+4, 74), fill='#579f8a'); d.rectangle((x-11, 59, x+11, 67), fill='#579f8a')
    elif name in ('clinic_ceiling', 'clinic_light'):
        im = surface('#e9f0e5'); d = ImageDraw.Draw(im)
        d.rectangle((1, 1, 126, 126), outline='#c2d9cf', width=3)
        if name.endswith('light'):
            d.rounded_rectangle((12, 16, 115, 111), radius=8, fill='#f4ffe8', outline='#5c9c8c', width=4)
            for y in range(28, 103, 12): d.line((24, y, 103, y), fill='#dce9d2', width=2)
    elif name == 'classroom_dado':
        im = surface('#d3a363'); d = ImageDraw.Draw(im)
        for x in range(0, 128, 32):
            d.rectangle((x+2, 8, x+29, 122), fill=('#efcf8f' if x % 64 == 0 else '#e4be7d'), outline='#bb8a50', width=2)
        d.rectangle((0, 0, 127, 8), fill='#ebcf8f'); d.line((0, 9, 127, 9), fill='#b97f4b', width=2)
    elif name == 'classroom_letters':
        stripes(d, '#f0dca3', '#fff0c4', '#c69e60')
        for x, label, color in ((12, 'A', '#668fb5'), (48, 'B', '#b86f79'), (84, 'C', '#76a17c')):
            d.rounded_rectangle((x-4, 38, x+30, 84), radius=4, fill='#f7e9c4', outline=color, width=2)
            d.text((x+13, 61), label, fill=color, font=font(28), anchor='mm')
    elif name == 'principal_parquet':
        im = surface('#805332'); d = ImageDraw.Draw(im)
        for y in range(-32, 160, 32):
            for x in range(-32, 160, 32):
                color = '#aa784a' if (x+y)//32 % 2 == 0 else '#98663e'
                d.rectangle((x+1, y+1, x+30, y+30), fill=color, outline='#70462b', width=2)
                for k in (7, 14, 22):
                    if (x+y)//32 % 2: d.line((x+k, y+4, x+k, y+27), fill='#ad7e50')
                    else: d.line((x+4, y+k, x+27, y+k), fill='#ba8856')
    elif name == 'principal_panel':
        im = surface('#6b432d'); d = ImageDraw.Draw(im)
        for x in (0, 64):
            d.rectangle((x+5, 7, x+58, 121), outline='#b48b53', width=3)
            d.rectangle((x+10, 13, x+53, 115), outline='#875c39', width=2)
        d.rectangle((0, 0, 127, 6), fill='#94683f'); d.line((0, 7, 127, 7), fill='#d0ac6b', width=2)
    elif name == 'principal_wall':
        im = surface('#d8d0ab'); d = ImageDraw.Draw(im)
        for x in range(0, 128, 32):
            d.line((x, 0, x, 127), fill='#c4bea0'); d.line((x+3, 0, x+3, 127), fill='#e7dfbd')
        for x, y in ((16, 28), (80, 92)):
            d.line((x, y-9, x, y+10), fill='#a4ab8c', width=2)
            d.ellipse((x-6, y-7, x, y), fill='#afb497'); d.ellipse((x, y, x+6, y+7), fill='#afb497')
    elif name == 'principal_frieze':
        stripes(d, '#58422f', '#896a45', '#c8aa68')
        for x in (32, 96):
            d.arc((x-18, 38, x+18, 90), 40, 320, fill='#b99d5d', width=2)
            for yy in (48, 60, 72):
                d.ellipse((x-19, yy, x-11, yy+5), fill='#b99d5d'); d.ellipse((x+11, yy, x+19, yy+5), fill='#b99d5d')
            d.rectangle((x-6, 53, x+6, 74), outline='#dfcb91', width=2)
            d.line((x, 54, x, 74), fill='#dfcb91')
    elif name in ('principal_ceiling', 'principal_light'):
        im = surface('#90704b'); d = ImageDraw.Draw(im)
        d.rectangle((4, 4, 123, 123), fill='#bfab7e', outline='#62472f', width=4)
        d.rectangle((10, 10, 117, 117), outline='#d4bf8e', width=2)
        if name.endswith('light'):
            d.rectangle((24, 24, 103, 103), fill='#f0dca8', outline='#a58245', width=3)
            for x in (47, 80): d.line((x, 26, x, 101), fill='#b39758', width=2)
            for y in (47, 80): d.line((26, y, 101, y), fill='#b39758', width=2)
    elif name == 'market_flagstone':
        im = surface('#62655f'); d = ImageDraw.Draw(im)
        for y in range(-32, 160, 32):
            for x in range(-32, 160, 64):
                xx = x + (32 if y//32 % 2 else 0)
                d.rectangle((xx+2, y+2, xx+61, y+29), fill='#77796e' if (x+y)//32 % 2 else '#6e726a', outline='#53594f', width=2)
                d.line((xx+5, y+4, xx+58, y+4), fill='#8a8c7d')
    elif name == 'market_wall':
        im = surface('#536e65'); d = ImageDraw.Draw(im)
        for y in range(0, 128, 32):
            d.line((0, y, 127, y), fill='#2e514b', width=3)
            d.line((0, y+4, 127, y+4), fill='#739183')
            for x in (13, 110): d.ellipse((x, y+12, x+3, y+15), fill='#ac9d73')
        for x, y in ((27, 19), (86, 52), (58, 91)):
            d.line((x, y, x+20, y), fill='#678276'); d.line((x+4, y+4, x+15, y+4), fill='#456358')
    elif name == 'market_frieze':
        stripes(d, '#3a514b', '#648071', '#ad9c6b')
        d.rectangle((0, 57, 127, 70), fill='#778577', outline='#b1b9a2', width=2)
        for x in (26, 90):
            d.rectangle((x-5, 49, x+5, 78), fill='#596c61', outline='#a99b72', width=2)
            d.ellipse((x-1, 53, x+1, 55), fill='#d4c9a0'); d.ellipse((x-1, 72, x+1, 74), fill='#d4c9a0')
    elif name == 'market_ceiling':
        im = surface('#655443'); d = ImageDraw.Draw(im)
        for x in range(0, 128, 32):
            d.line((x, 0, x, 127), fill='#443d33', width=3); d.line((x+5, 0, x+5, 127), fill='#816e54')
        d.rectangle((0, 3, 127, 15), fill='#4e493b'); d.line((0, 16, 127, 16), fill='#a69365', width=2)
    else:
        raise ValueError(name)
    grain(im, name)
    if name.endswith('frieze') or name.endswith('letters'):
        # Keep the side band's horizontal phase exact across cardinal walls/corners.
        for y in range(SIZE): im.putpixel((127, y), im.getpixel((0, y)))
    return im

def cube_model(identifier):
    name = identifier.removeprefix('school_hall_')
    texture = 'chao_garden:block/school0252/' + identifier
    if name == 'market_wall_shelf':
        def board(a, b, material):
            sizes = {'north': (b[0]-a[0], b[1]-a[1]), 'south': (b[0]-a[0], b[1]-a[1]),
                     'east': (b[2]-a[2], b[1]-a[1]), 'west': (b[2]-a[2], b[1]-a[1]),
                     'up': (b[0]-a[0], b[2]-a[2]), 'down': (b[0]-a[0], b[2]-a[2])}
            return {'from': a, 'to': b, 'faces': {face: {'texture': '#'+material, 'uv': [0, 0, *size]} for face, size in sizes.items()}}
        return {'textures': {'board': 'minecraft:block/gray_concrete', 'bracket': 'minecraft:block/dark_oak_planks', 'particle': 'minecraft:block/gray_concrete'},
                'elements': [board([0, 8, 1], [14, 10, 15], 'board'), board([0, 2, 3], [2, 8, 5], 'bracket'), board([0, 2, 11], [2, 8, 13], 'bracket')]}
    if name == 'market_shelf':
        def box(a, b):
            sizes = {'north': (b[0]-a[0], b[1]-a[1]), 'south': (b[0]-a[0], b[1]-a[1]),
                     'east': (b[2]-a[2], b[1]-a[1]), 'west': (b[2]-a[2], b[1]-a[1]),
                     'up': (b[0]-a[0], b[2]-a[2]), 'down': (b[0]-a[0], b[2]-a[2])}
            return {'from': a, 'to': b, 'faces': {face: {'texture': '#wood', 'uv': [0, 0, *size]} for face, size in sizes.items()}}
        return {'textures': {'wood': texture, 'particle': texture}, 'elements': [
            box([2, 4, 7], [14, 8, 9]), box([2, 10, 7], [14, 15, 9]), box([0, 0, 0], [16, 4, 16]),
            box([0, 8, 0], [16, 10, 16]), box([0, 15, 0], [16, 16, 16]),
            box([0, 4, 0], [2, 8, 16]), box([14, 4, 0], [16, 8, 16]),
            box([0, 10, 0], [2, 15, 16]), box([14, 10, 0], [16, 15, 16])], 'ambientocclusion': False}
    if name.endswith('frieze') or name.endswith('letters') or name in ('principal_certificate', 'principal_award', 'classroom_student_work', 'classroom_class_photo', 'clinic_registration', 'clinic_wellness'):
        family = name.split('_', 1)[0]
        cap = {'fortune': '#321641', 'clinic': '#e8f1e8', 'classroom': '#f0dca3', 'principal': '#d8d0ab', 'market': '#3a514b', 'foyer': '#ede6ce'}[family]
        cap_name = identifier + '_cap'
        surface(cap).save(TEXTURES / (cap_name + '.png'), optimize=True)
        return {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'side': texture, 'top': 'chao_garden:block/school0252/' + cap_name, 'bottom': 'chao_garden:block/school0252/' + cap_name}}
    return {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture}}

source = (ROOT / 'src/main/java/com/chaogarden/kindergarten/KindergartenHallThemes0252.java').read_text()
ids = [identifier for arguments in re.findall(r'add\(\d+,([^;]+)\);', source)
       for identifier in re.findall(r'"(school_hall_[a-z_]+)"', arguments)]
assert len(ids) == len(set(ids)) == 30
locale = {}
for block_id in ids:
    if block_id != 'school_hall_market_wall_shelf':
        paint(block_id.removeprefix('school_hall_')).save(TEXTURES / (block_id + '.png'), optimize=True)
    model = 'chao_garden:block/' + block_id
    write(ASSETS / 'models/block' / (block_id + '.json'), cube_model(block_id))
    write(ASSETS / 'blockstates' / (block_id + '.json'), {'variants': {'': {'model': model}}})
    write(ASSETS / 'models/item' / (block_id + '.json'), {'parent': model})
    write(RES / 'data/chao_garden/loot_table/blocks' / (block_id + '.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'chao_garden:' + block_id}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    label = block_id.removeprefix('school_hall_').replace('_', ' ').title()
    locale['block.chao_garden.' + block_id] = 'School ' + label
write(ROOT / 'art/locale-kindergarten-halls0252.json', locale)
write(ASSETS / 'kindergarten_hall_themes0252.json', {'version': '0.25.2', 'texturePrefix': 'block/school0252/', 'blocks': [{'id': identifier, 'item': True, 'kind': 'WALL_SHELF' if identifier == 'school_hall_market_wall_shelf' else 'CUBE'} for identifier in ids]})
# This independent last writer is safe to replay after the legacy language finalizer.
lang_path = ASSETS / 'lang/en_us.json'
lang = json.loads(lang_path.read_text()); lang.update(locale)
lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=False, sort_keys=True) + '\n')
print(f'Kindergarten0252 hall palette: {len(ids)} native repeating full-block finishes, generated independently.')
