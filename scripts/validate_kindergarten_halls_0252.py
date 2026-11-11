#!/usr/bin/env python3
"""Finite hall palette bindings, opaque masonry and distinct destination surface identities."""
from pathlib import Path
from copy import deepcopy
import argparse
import hashlib
import json
import math
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'art'))
from scene_resource_contract_025 import hallway_items

def opaque(image):
    assert image.mode == 'RGBA' and image.size == (128, 128), 'Hall surface has the wrong native image format'
    assert image.getchannel('A').getextrema() == (255, 255), 'Solid hall masonry/fabric acquired a hole'

def complete_sigils(image):
    # Inspect all360 directions of every ring, including the former30..330 gap.
    # The equal X/Y radii and complete native cube UVs give circles in world space.
    ring=(129,81,126,255)
    for cx in (32,96):
        for cy in (32,96):
            for degrees in range(360):
                angle=math.radians(degrees);x=round(cx+19.5*math.cos(angle));y=round(cy+19.5*math.sin(angle))
                assert any(image.getpixel((x+dx,y+dy))==ring for dx in (-1,0,1) for dy in (-1,0,1)), 'Fortune wall ring is clipped, elliptical or open at '+str((cx,cy,degrees))
    assert all(image.getpixel((x,y))!=ring for x in (0,127) for y in range(128)), 'A circular motif is cut by the wall/corner face boundary'

def cube(model, identifier):
    texture = 'chao_garden:block/school0252/' + identifier
    if identifier == 'school_hall_market_wall_shelf':
        assert model['textures'] == {'board': 'minecraft:block/gray_concrete', 'bracket': 'minecraft:block/dark_oak_planks', 'particle': 'minecraft:block/gray_concrete'}, 'Wall shelf stopped reusing the reviewed native materials'
        elements=model['elements']
        assert len(elements) == 3 and elements[0]['from'] == [0,8,1] and elements[0]['to'] == [14,10,15], 'Wall shelf no longer meets the offset props at its real ten-pixel top'
        assert all(e['from'][0] == 0 for e in elements), 'Shelf/brackets are detached from their actual backing wall'
        assert all(all(0 <= a < b <= 16 for a,b in zip(e['from'],e['to'])) for e in elements), 'Wall shelf mesh escapes its cell'
    elif identifier == 'school_hall_market_shelf':
        assert model['textures'] == {'wood': texture, 'particle': texture} and len(model['elements']) == 9, 'Stock shelf lost its own native wood mesh'
        assert all(any(e['from'] == a and e['to'] == b for e in model['elements']) for a,b in (([2, 4, 7], [14, 8, 9]), ([2, 10, 7], [14, 15, 9]))), 'Stock shelf is open through its backing wall'
        for element in model['elements']:
            assert all(0 <= a < b <= 16 for a, b in zip(element['from'], element['to'])), 'Stock shelf escapes its actual wall cell'
    elif model.get('parent') == 'minecraft:block/cube_bottom_top':
        assert model['textures'] == {'side': texture, 'top': texture + '_cap', 'bottom': texture + '_cap'}, 'Wall trim acquired a rotated/fractured band on its exposed caps'
    else:
        assert model == {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture}}, 'Hall block is not an unscaled, complete cube with its own surface: ' + identifier

def validate(self_test=False):
    assets = ROOT / 'src/main/resources/assets/chao_garden'
    ids = hallway_items(ROOT)
    locale = json.loads((assets / 'lang/en_us.json').read_text())
    pixels = {}
    for identifier in sorted(ids):
        model_name = 'chao_garden:block/' + identifier
        states = json.loads((assets / 'blockstates' / (identifier + '.json')).read_text())
        assert states == {'variants': {'': {'model': model_name}}}, 'Hall surface has an invalid blockstate route'
        cube(json.loads((assets / 'models/block' / (identifier + '.json')).read_text()), identifier)
        assert json.loads((assets / 'models/item' / (identifier + '.json')).read_text()) == {'parent': model_name}, 'Hall palette item points to another block'
        if identifier != 'school_hall_market_wall_shelf':
            with Image.open(assets / 'textures/block/school0252' / (identifier + '.png')) as image:
                opaque(image)
                if identifier=='school_hall_fortune_wall':complete_sigils(image)
                pixels[identifier] = hashlib.sha256(image.tobytes()).hexdigest()
                if identifier.endswith('frieze') or identifier.endswith('letters'):
                    assert all(image.getpixel((0, y)) == image.getpixel((127, y)) for y in range(128)), 'Frieze edge phase is discontinuous at a cardinal corner'
        loot = json.loads((ROOT / 'src/main/resources/data/chao_garden/loot_table/blocks' / (identifier + '.json')).read_text())
        assert loot['pools'][0]['entries'] == [{'type': 'minecraft:item', 'name': 'chao_garden:' + identifier}], 'Hall block does not drop its one native palette item'
        assert 'block.chao_garden.' + identifier in locale, 'Hall palette localization is missing'
    assert len(set(pixels.values())) == len(pixels) == len(ids)-1, 'Destination finishes silently reused an unrelated surface'
    rejected = 0
    if self_test:
        transparent = Image.new('RGBA', (128, 128), '#7b4285'); transparent.putpixel((64, 64), (123, 66, 133, 0))
        wrong = {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'chao_garden:block/school025/school_cream_wall'}}
        with Image.open(assets / 'textures/block/school0252/school_hall_fortune_wall.png') as source:
            cut_circle=source.copy()
        for x in range(46,55):
            for y in range(19,46):cut_circle.putpixel((x,y),(72,35,79,255))
        for action in (lambda: opaque(transparent), lambda: cube(wrong, 'school_hall_fortune_wall'),lambda:complete_sigils(cut_circle)):
            try:
                action()
            except AssertionError:
                rejected += 1
            else:
                raise AssertionError('Malformed hall surface accepted')
    report = {'release': '0.25.2', 'result': 'PASS', 'nativePaletteBlocks': len(ids), 'fullyOpaqueSurfaces': len(pixels), 'reusedVanillaWallShelf': 1, 'distinctPixelBindings': len(set(pixels.values())), 'rejectedCorruptions': rejected}
    target = ROOT / 'docs/research/KINDERGARTEN_HALL_ASSETS_0252_CHECKS.json'
    target.parent.mkdir(parents=True, exist_ok=True); target.write_text(json.dumps(report, indent=2) + '\n')
    print(f'Kindergarten0252 halls: {len(ids)} finite full-cube surfaces; opaque, distinct and bound; PASS')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--self-test', action='store_true')
    validate(parser.parse_args().self_test)
