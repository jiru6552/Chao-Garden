#!/usr/bin/env python3
"""Render the authored inventory art without running any data/resource generator."""
from pathlib import Path
import argparse
import ast
import importlib.util
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'art'))
from item_sprite_art import drive, egg, save_sprite, themed_egg
from variant_catalog import VARIANTS


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write-resources', action='store_true',
                        help='Also refresh only egg, drive and gene item PNGs; leave data and models untouched')
    args = parser.parse_args()
    # Importing the base generator would rewrite data. Its literal palettes can
    # be read independently, keeping this visual index free of resource writes.
    syntax = ast.parse((ROOT/'scripts/generate_assets.py').read_text())
    palettes = {}
    for node in syntax.body:
        if isinstance(node, ast.Assign):
            for target in node.targets:
                if isinstance(target, ast.Name) and target.id in ('COLORS','JEWELS'):
                    palettes[target.id] = ast.literal_eval(node.value)
    spec = importlib.util.spec_from_file_location('gene_preview',ROOT/'scripts/generate_gene_assets.py')
    genes = importlib.util.module_from_spec(spec); spec.loader.exec_module(genes)
    themes = {entry['id']:entry for entry in VARIANTS}
    if args.write_resources:
        target = ROOT/'src/main/resources/assets/chao_garden/textures/item'
        count = 0
        for name, color in palettes['COLORS'].items():
            for shiny in (False, True):
                ident = ('shiny_egg' if name == 'normal' else 'shiny_'+name+'_egg') if shiny else name+'_egg'
                save_sprite(egg(color, shiny), target/(ident+'.png')); count += 1
        for name, color in palettes['JEWELS'].items():
            save_sprite(egg(color, jewel=True), target/(name+'_jewel_egg.png')); count += 1
        for entry in VARIANTS:
            save_sprite(themed_egg(entry), target/(entry['id']+'_chao_egg.png')); count += 1
        save_sprite(egg(palettes['COLORS']['normal']), target/'chao_spawn_egg.png'); count += 1
        for name, color in zip(('swim','fly','run','power'), ('f1ce58','b889e3','7edb7e','e9606a')):
            save_sprite(drive(color), target/(name+'_drive.png')); count += 1
        for index, name in enumerate(genes.NAMES):
            for suffix in ('_animal_gene','_animal'):
                save_sprite(genes.vial(index), target/(name+suffix+'.png')); count += 1
        genes.write_preview([genes.vial(index) for index in range(len(genes.NAMES))])
        print(f'Refreshed {count} item PNGs only; data, models and loot are untouched.')
    canvas = Image.new('RGB',(1120,1350),'#edf3f5'); draw = ImageDraw.Draw(canvas)
    font_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    def font(size): return ImageFont.truetype(font_path,size)
    def sprite(image,x,y,large=96):
        enlarged = image.resize((large,large),Image.Resampling.NEAREST)
        canvas.paste(enlarged,(x,y),enlarged)
        # Both actual-sized previews and enlargements use nearest-neighbor;
        # there are no antialiased edges or simulated game frames here.
        tiny = image.resize((16,16),Image.Resampling.NEAREST)
        canvas.paste(tiny,(x+large+8,y+large-16),tiny)
    draw.text((24,18),'Chao Garden 0.13.0 — inventory sprites',font=font(30),fill='#173f4b')
    draw.text((24,61),'Original pixel art · center-groove drives, mod-specific egg icons and cleaner DNA vials',font=font(18),fill='#47636e')
    draw.text((24,105),'CHAOS DRIVES',font=font(23),fill='#173f4b')
    for index,(name,color) in enumerate(zip(('Swim / yellow','Fly / purple','Run / green','Power / red'),
                                          ('f1ce58','b889e3','7edb7e','e9606a'))):
        x=index*280+76
        sprite(drive(color),x,142,112)
        draw.text((index*280+140,278),name,font=font(19),fill='#173f4b',anchor='mm')
    draw.text((24,328),'EGG FAMILIES',font=font(23),fill='#173f4b')
    samples = [('Normal',egg(palettes['COLORS']['normal'])),
               ('Blue',egg(palettes['COLORS']['blue'])),
               ('Red',egg(palettes['COLORS']['red'])),
               ('Black',egg(palettes['COLORS']['black'])),
               ('Shiny',egg(palettes['COLORS']['normal'],True)),
               ('Shiny blue',egg(palettes['COLORS']['blue'],True)),
               ('Diamond',egg(palettes['JEWELS']['diamond'],jewel=True)),
               ('Ruby',egg(palettes['JEWELS']['ruby'],jewel=True)),
               ('Gold',egg(palettes['JEWELS']['gold'],jewel=True))]
    samples.extend((themes[ident]['name'],themed_egg(themes[ident])) for ident in ('create','botania','ae2'))
    for index,(name,image) in enumerate(samples):
        x=index%6*186+30; y=370+index//6*170
        sprite(image,x,y)
        draw.text((index%6*186+92,y+125),name,font=font(16),fill='#173f4b',anchor='mm')
    draw.text((24,727),'21 ANIMAL GENE SAMPLES',font=font(23),fill='#173f4b')
    for index,name in enumerate(genes.NAMES):
        x=index%7*160+20; y=770+index//7*172
        sprite(genes.vial(index),x,y)
        label='Boar' if name=='warthog' else name.replace('_',' ').title()
        draw.text((index%7*160+80,y+126),label,font=font(16),fill='#173f4b',anchor='mm')
    draw.text((24,1312),'Native art: 16px drives/eggs; 32px DNA vials. Tiny examples show a 16px inventory footprint.',
              font=font(16),fill='#47636e')
    target=ROOT/'docs/screenshots/item-sprites-0.13.png'
    save_sprite(canvas,target)
    print(target)


if __name__=='__main__': main()
