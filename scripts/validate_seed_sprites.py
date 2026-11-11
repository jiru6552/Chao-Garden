#!/usr/bin/env python3
"""Validate the native seed icon catalog's image and sprouting-bulb contracts."""
from pathlib import Path
import hashlib
import json
import sys
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'art'))
from sa2_fruit_art import PALETTES


def components(points):
    remaining=set(points);sizes=[]
    while remaining:
        pending=[remaining.pop()];size=0
        while pending:
            x,y=pending.pop();size+=1
            # Diagonal leaves are connected under the image's eight-neighbor grid.
            for point in ((x+dx,y+dy) for dx in (-1,0,1) for dy in (-1,0,1) if dx or dy):
                if point in remaining:
                    remaining.remove(point);pending.append(point)
        sizes.append(size)
    return sorted(sizes,reverse=True)


CWE_FRUITS=('orange_fruit','gba_orange_fruit','gba_blue_fruit','gba_pink_fruit','gba_green_fruit',
            'gba_purple_fruit','gba_yellow_fruit','gba_red_fruit','shiny_fruit',
            'hyper_swim_fruit','hyper_fly_fruit','hyper_run_fruit','hyper_power_fruit')
LEGACY_SEEDS={'stamina_fruit','mushroom','mushroom_alt'}
checks=0

def require(ok,message):
    global checks
    checks+=1
    if not ok:raise AssertionError(message)

def main():
    directory=ROOT/'src/main/resources/assets/chao_garden/textures/item'
    families=set(PALETTES)|set(CWE_FRUITS)
    require(len(PALETTES)==23 and len(CWE_FRUITS)==13 and len(families)==36,'Exact original/CWE seed families changed')
    expected={fruit+'_seed.png' for fruit in families}
    actual={path.name for path in directory.glob('*_seed.png')}
    require(actual==expected,('Exact seed catalog changed',actual^expected))
    active={row['fruit'] for row in json.loads((ROOT/'docs/fruit_catalog.json').read_text())['fruits']}
    require(len(active)==35 and active==families-{'stamina_fruit'},'Runtime/gallery active crop catalog differs')
    records=[];hashes=set()
    for fruit in sorted(families):
        path=directory/(fruit+'_seed.png')
        with Image.open(path) as image:
            require(image.mode=='RGBA' and image.size==(32,32),str(path)+': nativeRGBA dimensions')
            pixels=[image.getpixel((x,y)) for y in range(32) for x in range(32)]
            require({pixel[3] for pixel in pixels}=={0,255},str(path)+': binaryalpha')
            opaque={(x,y) for y in range(32) for x in range(32) if image.getpixel((x,y))[3]}
            sizes=components(opaque)
            require(sizes[0]>=100 and sum(sizes[1:])<=6,(path,'Bulb and sprout must remain connected',sizes))
            require(100<=len(opaque)<=550,(path,'Unreadable or oversized seed silhouette'))
        digest=hashlib.sha256(path.read_bytes()).hexdigest();hashes.add(digest)
        recipe=ROOT/'src/main/resources/data/chao_garden/recipe'/(fruit+'_seed.json')
        if fruit in LEGACY_SEEDS:
            require(not recipe.exists(),fruit+': compatibility-only seed gained an acquisition recipe')
        else:
            require(json.loads(recipe.read_text())=={'type':'minecraft:crafting_shapeless',
                    'ingredients':[{'item':'chao_garden:'+fruit}],
                    'result':{'id':'chao_garden:'+fruit+'_seed','count':1}},fruit+': matching fruit-first seed recipe')
        model=json.loads((directory.parents[1]/'models/item'/(fruit+'_seed.json')).read_text())
        require(model.get('loader')=='neoforge:separate_transforms' and model.get('textures',{}).get('layer0')=='chao_garden:item/'+fruit+'_seed',fruit+': seed model binding')
        records.append({'id':fruit+'_seed','size':[32,32],'active_acquisition':fruit not in LEGACY_SEEDS,
                        'opaque_pixels':len(opaque),'connected_regions':sizes,
                        'color_count':len(set(pixels))-1,'sha256':digest})
    require(len(hashes)==36,'All36 seed resources must have distinct native appearances')
    print(f'PASS:{checks} checks;36 distinct native seed resources,33 active fruit-first crops plus3 compatibility-only seed textures; binaryalpha, connected sprouting bulbs, matching native meshes and exact no-extra catalog.')
    return records

if __name__=='__main__':
    records=main()
    path=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'docs/validation-0.19/seed-resources.json'
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps({'release':'0.19.0','checks':checks,'result':'passed','sprites':records},indent=2)+'\n')
