#!/usr/bin/env python3
"""Accessories datapack slot plus an animated carrier/Chao item model."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'


def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2) + '\n')


def main():
    write('data/chao_garden/accessories/slot/chao_companion.json', {
        'replace': False, 'amount': 1, 'operation': 'set', 'order': 1100,
        'icon': 'chao_garden:gui/slot/chao_companion',
        'validators': ['accessories:tag'], 'drop_rule': 'keep'})
    write('data/chao_garden/accessories/entity/chao_companion.json', {
        'replace': False, 'entities': ['minecraft:player'], 'slots': ['chao_companion']})
    write('data/chao_garden/accessories/group/chao_companion.json', {
        'replace': False, 'order': 130, 'slots': ['chao_companion'],
        'icon': 'chao_garden:gui/group/chao_companion'})
    write('data/accessories/tags/item/chao_companion.json', {
        'replace': False, 'values': ['chao_garden:chao_carrier']})
    write('assets/chao_garden/models/item/chao_carrier.json', {
        'parent': 'builtin/entity', 'gui_light': 'front',
        'display': {
            'gui': {'rotation': [0, 0, 0], 'translation': [0, -3, 0], 'scale': [.72, .72, .72]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [.5, .5, .5]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, -2, 0], 'scale': [.65, .65, .65]},
            'firstperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 1, -1], 'scale': [.8, .8, .8]},
            'firstperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 1, -1], 'scale': [.8, .8, .8]},
            'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 1, 0], 'scale': [.7, .7, .7]},
            'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 1, 0], 'scale': [.7, .7, .7]}}})
    image = Image.new('RGBA', (16,16), (0,0,0,0)); draw = ImageDraw.Draw(image)
    draw.rectangle((3,5,12,12), outline=(192,215,219,255), width=1)
    draw.rectangle((5,7,6,10), fill=(192,215,219,255)); draw.rectangle((9,7,10,10), fill=(192,215,219,255))
    draw.rectangle((6,3,9,4), fill=(192,215,219,255))
    target = RES/'assets/chao_garden/textures/gui/slot/chao_companion.png'; target.parent.mkdir(parents=True,exist_ok=True); image.save(target)
    # Accessories registers gui/slot in the block atlas and gui/group in the
    # GUI atlas. Group tabs require their own eight-pixel sprite directory.
    group = Image.new('RGBA', (8,8), (0,0,0,0)); draw = ImageDraw.Draw(group)
    ink = (78,86,95,255)
    draw.rectangle((3,0,4,1), fill=ink)
    draw.rectangle((1,3,6,6), outline=ink, width=1)
    draw.point((3,4), fill=ink); draw.point((5,4), fill=ink)
    target = RES/'assets/chao_garden/textures/gui/group/chao_companion.png'; target.parent.mkdir(parents=True,exist_ok=True); group.save(target)
    print('Generated companion slot data, icon and animated Chao item model.')


if __name__ == '__main__': main()
