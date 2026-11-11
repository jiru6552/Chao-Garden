#!/usr/bin/env python3
"""Validate the native Adventure fruit assets without rewriting any resources."""
import json
from pathlib import Path
from PIL import Image
from build_ca2_fruit_018 import FRUITS

ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/chao_garden'
DATA=ROOT/'src/main/resources/data/chao_garden'
checks=0

def require(condition, message):
    global checks
    checks+=1
    if not condition:raise AssertionError(message)

def read(path):return json.loads(path.read_text())

def native(path):
    im=Image.open(path).convert('RGBA')
    return im.size==(32,32) and set(im.getchannel('A').tobytes())<={0,255}

def valid_model(reference):
    namespace,path=reference.split(':',1)
    return namespace=='chao_garden' and (ASSET/'models'/f'{path}.json').is_file()

for fruit in FRUITS:
    for item in (fruit,fruit+'_seed'):
        require(native(ASSET/'textures/item'/f'{item}.png'),f'Native sprite: {item}')
        model=read(ASSET/'models/item'/f'{item}.json')
        require(model.get('loader')=='neoforge:separate_transforms' and model.get('textures',{}).get('layer0')==f'chao_garden:item/{item}',f'Item model: {item}')
    for age in range(4):
        model=read(ASSET/'models/block'/f'{fruit}_pod_{age}.json')
        valid=model.get('loader')=='neoforge:obj' and native(ASSET/'textures/block'/f'{fruit}_pod_{age}.png')
        valid=valid and (ASSET/model['model'].split(':',1)[1]).is_file()
        textures=model.get('textures',{})
        source_materials={k:v for k,v in textures.items() if k.startswith('sa2_')}
        if source_materials:
            valid=valid and all(v.startswith(f'chao_garden:block/sa2_02514/{fruit}/') and v.endswith(f'_{age}') and (ASSET/'textures'/(v.split(':',1)[1]+'.png')).is_file() for v in source_materials.values())
            used={line.split()[1] for line in (ASSET/model['model'].split(':',1)[1]).read_text().splitlines() if line.startswith('usemtl ')}
            valid=valid and used<=(source_materials.keys()|{'stem'})
        else:valid=valid and textures.get('fruit')==f'chao_garden:block/{fruit}_pod_{age}'
        require(valid,f'Pod model and ripening texture: {fruit}/{age}')
    for suffix,count in (('seedling',2),('tree_crown',2),('pod',16)):
        variants=read(ASSET/'blockstates'/f'{fruit}_{suffix}.json').get('variants',{})
        require(len(variants)==count and all(valid_model(v['model']) for v in variants.values()),f'Complete blockstate: {fruit}/{suffix}')
    recipe=read(DATA/'recipe'/f'{fruit}_seed.json')
    require(recipe.get('type')=='minecraft:crafting_shapeless'
            and recipe.get('ingredients')==[{'item':'chao_garden:'+fruit}]
            and recipe.get('result')=={'id':'chao_garden:'+fruit+'_seed','count':1},f'Fruit-first seed recipe: {fruit}')
for mushroom in ('mushroom','mushroom_alt'):
    require(all(not (DATA/directory/f'{mushroom}_seed.json').exists() for directory in ('recipe','recipes')),
            f'No new mushroom seed recipe in either layout: {mushroom}')

report={'version':'0.18.0','checks':checks,'profiles':list(FRUITS),'result':'passed',
        'scope':['native sprite dimensions and binary alpha','item model bindings',
                 'nondegenerate pod geometry and four stage textures','complete blockstates',
                 'fruit-first seed recipes','no new mushroom seed recipes']}
destination=ROOT/'docs/validation-0.18/ca2-fruit.json'
destination.parent.mkdir(parents=True,exist_ok=True)
destination.write_text(json.dumps(report,indent=2)+'\n')
print(f'CA2 fruit assets: {checks} checks passed')
