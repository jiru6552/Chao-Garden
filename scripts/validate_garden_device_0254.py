#!/usr/bin/env python3
"""Finite eight-cell transporter assets, matched native boxes and vanilla record silhouette.

No writes: verify generated resources against their authoring geometry, including
all four global rotations. Mutation controls exercise missing/shifted part guards.
"""
from pathlib import Path
import argparse
import copy
import importlib.util
import json
import re
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
checks=0

def require(condition,message):
    global checks
    checks+=1
    if not condition:raise AssertionError(message)

def load(path):return json.loads(path.read_text())

def generator():
    spec=importlib.util.spec_from_file_location('transporter',ROOT/'scripts/generate_transporter_assets.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

def validate_parts(module,data,variants,models):
    require(len(variants)==36,'Every facing needs eight actual parts plus one safe legacy state')
    for facing,turn in (('north',0),('east',1),('south',2),('west',3)):
        observed=set()
        for part in range(9):
            key=f'facing={facing},part={part}'
            require(key in variants,'Missing native variant '+key)
            variant=variants[key]
            require(variant.get('y',0)==turn*90,'Wrong native facing rotation '+key)
            if part==8:
                require(variant['model'].endswith('garden_block_legacy'),'Legacy decode expanded without space preflight')
                continue
            x,z=part&1,(part>>1)&1
            for _ in range(turn):x,z=z,1-x
            northpart=x+2*z+(part&4);observed.add(northpart)
            require(variant['model']==f'chao_garden:block/garden_block_part_{northpart}','Facing selected wrong world-cell mesh '+key)
            mesh=models[northpart]
            require(mesh==module.slice_model(data,northpart),'Part mesh/authoring geometry mismatch '+key)
            require(bool(mesh['elements']),'Native interaction cell has no visible geometry '+key)
            for element in mesh['elements']:
                require('rotation' not in element,'Rotated element crossed a native part boundary')
                require(all(0<=value<=16 for endpoint in (element['from'],element['to']) for value in endpoint),'Mesh outside its physical part cell')
                for face in element['faces'].values():
                    require(all(0<=value<=16 for value in face['uv']),'Texture sampled outside its own atlas sprite')
        require(observed==set(range(8)),'Facing duplicated or omitted a physical cell')

def validate(self_test=False):
    module=generator();data=module.enlarged(module.model())
    models={part:load(ASSETS/f'models/block/garden_block_part_{part}.json') for part in range(8)}
    variants=load(ASSETS/'blockstates/garden_block.json')['variants']
    validate_parts(module,data,variants,models)
    # The production collision class is generated from these exact cuboids;
    # GameTests additionally compare native selection/collision and harvesting.
    source=(ROOT/'src/main/java/com/chaogarden/activities/GardenDeviceGeometry0254.java').read_text()
    boxes=source.split('private static final double[][] BOXES = {',1)[1].split('  };',1)[0]
    actual=[[float(value.strip()) for value in row.split(',')] for row in re.findall(r'\{([^{}]+)\}',boxes)]
    require(actual==[e['from']+e['to'] for e in data['elements']],'Native collision geometry drifted from generated visible mesh')
    require([min(e['from'][i] for e in data['elements']) for i in range(3)]==[0,0,0],'Device minimum does not meet 2x2x2 footprint')
    require([max(e['to'][i] for e in data['elements']) for i in range(3)]==[32,32,32],'Device maximum does not meet 2x2x2 footprint')
    item=load(ASSETS/'models/block/garden_block.json')
    require(all(0<=v<=16 for e in item['elements'] for end in (e['from'],e['to']) for v in end),'Inventory representation overflows one block')
    loot=load(ROOT/'src/main/resources/data/chao_garden/loot_table/blocks/garden_block.json')
    terms=loot['pools'][0]['conditions'][1]['terms']
    require({term['properties']['part'] for term in terms}=={'0','8'},'Child cells duplicate native harvest drops')
    icon=Image.open(ASSETS/'textures/item/chao_garden_music_disc.png').convert('RGBA')
    require(icon.size==(16,16),'Record icon must use native sixteen pixel inventory size')
    require(set(icon.getchannel('A').tobytes())=={0,255},'Record sprite has blurry or fractional alpha')
    dark=blue=0
    for r,g,b,a in (icon.getpixel((x,y)) for y in range(16) for x in range(16)):
        if not a:continue
        if max(r,g,b)<110:dark+=1
        if b>140 and g>r*1.2:blue+=1
    require(dark>90 and blue>=8,'CD lacks vanilla dark vinyl and a clear Chao-blue center')
    require(all(icon.getpixel(p)[3]==0 for p in ((0,0),(15,0),(0,15),(15,15))),'Record silhouette missing')
    require(all(icon.getpixel(p)[3]==0 or max(icon.getpixel(p)[:3])<60 for p in ((7,7),(8,8))),'Record spindle hole must be empty or dark like vanilla records')
    if self_test:
        broken=copy.deepcopy(variants);del broken['facing=south,part=7']
        try:validate_parts(module,data,broken,models)
        except AssertionError:pass
        else:raise AssertionError('Missing part mutation passed')
        broken=copy.deepcopy(models);broken[0]['elements'][0]['to'][0]=17
        try:validate_parts(module,data,variants,broken)
        except AssertionError:pass
        else:raise AssertionError('Shifted model mutation passed')
    print(f'Garden device0254 passed {checks} mesh, rotation, native collision, loot and CD checks.')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--self-test',action='store_true');validate(parser.parse_args().self_test)
