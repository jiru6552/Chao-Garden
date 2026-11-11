#!/usr/bin/env python3
"""Validate included Hero native assets and isolated sky/biome contracts."""
from pathlib import Path
import argparse
from copy import deepcopy
import hashlib
import json
import re
from PIL import Image, ImageStat
from validate_classical_models_0254 import cornice_contract

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
DATA=ROOT/'src/main/resources/data/chao_garden'
IDS={'hero_marble','hero_marble_trim','hero_fountain_emblem'}


def require(ok,message):
    if not ok: raise AssertionError(message)


def read(path): return json.loads(path.read_text())


def emblem_contract(model):
    textures=model.get('textures',{})
    require(model.get('parent')=='minecraft:block/cube','Fountain must keep all six solid native cube faces')
    require(textures.get('east')=='chao_garden:block/hero0253/hero_fountain_emblem','Fountain Chao emblem faces away from the actual east-facing water/lawn')
    require(all(textures.get(face)=='chao_garden:block/hero0253/hero_marble' for face in ('north','south','west','up','down')),'Emblem material leaks around temple corner or its cube lost a face')


def validate():
    source=(ROOT/'src/main/java/com/chaogarden/hub/HeroGardenDecor0253.java').read_text()
    declared=set(re.findall(r'"(hero_[a-z_]+)"',source))
    require(declared==IDS|{'hero_column'},'Hero registry differs from three masonry blocks plus refined column')
    manifest=read(ASSETS/'hero_garden_decor0253.json')
    require(manifest['version']=='0.25.9' and manifest['texturePrefix']=='block/hero0253/','Hero catalog version/prefix changed')
    require({row['id'] for row in manifest['blocks']}==IDS|{'hero_column'} and len(manifest['blocks'])==4 and all(row['item'] is True for row in manifest['blocks']),'Hero native block/item catalog mismatch')
    colors=[]
    for identifier in IDS:
        block=read(ASSETS/f'blockstates/{identifier}.json')
        if identifier=='hero_marble_trim':cornice_contract()
        else:require(block=={'variants':{'':{'model':f'chao_garden:block/{identifier}'}}},'Hero blockstate no longer binds its actual native model')
        require(read(ASSETS/f'models/item/{identifier}.json')=={'parent':f'chao_garden:block/{identifier}'},'Hero BlockItem model is missing or detached from its block')
        model=read(ASSETS/f'models/block/{identifier}.json')
        if identifier=='hero_fountain_emblem':emblem_contract(model)
        elif identifier!='hero_marble_trim':require(model=={'parent':'minecraft:block/cube_all','textures':{'all':f'chao_garden:block/hero0253/{identifier}'}},'Hero pale masonry cube uses a stretched/partial face')
        with Image.open(ASSETS/f'textures/block/hero0253/{identifier}.png') as image:
            require(image.mode=='RGBA' and image.size==(64,64) and image.getchannel('A').getextrema()==(255,255),'Hero masonry texture is missing, transparent, or wrong resolution')
            require(len(image.getcolors(4096) or [])>4,'Hero authored material fell back to a flat placeholder')
            colors.append(image.tobytes())
        drops=read(DATA/f'loot_table/blocks/{identifier}.json')
        require(drops['pools'][0]['entries']==[{'type':'minecraft:item','name':f'chao_garden:{identifier}'}],'Hero masonry creative/survival identity differs')
    require(len(set(colors))==3,'Three Hero textures collapse to one fallback')
    biome=read(DATA/'worldgen/biome/hero_daylight.json')
    require(not biome['has_precipitation'] and not biome['spawners'] and not biome['features'] and biome['creature_spawn_probability']==0,'Hero biome permits weather, wildlife or overworld generation')
    require(biome['effects']['sky_color']==0xffedb6 and biome['effects']['fog_color']==0xffc4de,'Hero pale gold sky / pink cloud horizon fell back to Neutral blue')
    dim=read(DATA/'dimension/hero_garden.json');kind=read(DATA/'dimension_type/hero_garden.json')
    require(dim['generator']['settings']['biome']=='chao_garden:hero_daylight' and dim['generator']['settings']['layers']==[{'height':1,'block':'minecraft:air'}],'Hero dimension accidentally generates an overworld terrain preset')
    require(kind['effects']=='chao_garden:hero_sky' and kind['has_skylight'] and not kind['has_ceiling'],'Hero dimension lost its isolated open sky effect')
    effects=(ROOT/'src/main/java/com/chaogarden/client/HeroGardenEffects0253.java').read_text()
    require('mc.level.dimension().equals(HubContent.HERO_GARDEN)' in effects and 'Float.NaN' in effects,'Hero cloud pass leaked into other realms or restored overhead vanilla clouds')
    return 3


def native_overhead(path):
    """A genuinely downward native view must see cloud detail in every corner.

    A camera-unrotated AFTER_SKY pass leaves the upper corners as one flat pink
    fog color even though the garden terrain correctly rotates underneath it.
    This checks captured pixels, not the renderer's own matrix implementation.
    The release camera-pose gate independently verifies this scene's downward
    local-player view; this image check does not substitute for that gate.
    """
    image=Image.open(path).convert('RGB');w,h=image.size
    require((w,h)==(1280,720),'Hero overhead native framebuffer changed')
    boxes={'top_left':(0,0,220,180),'top_right':(w-220,0,w,180),
           'bottom_left':(0,h-180,220,h),'bottom_right':(w-220,h-180,w,h)}
    patches={}
    for name,box in boxes.items():
        crop=image.crop(box);colors=crop.getcolors(crop.width*crop.height);stats=ImageStat.Stat(crop)
        pink=sum(count for count,(r,g,b) in colors if r>g+12 and b>g+6)/(crop.width*crop.height)
        patches[name]={'unique_rgb_colors':len(colors),'rgb_standard_deviation':stats.stddev,'pink_fraction':pink}
        require(len(colors)>=4 and min(stats.stddev)>.5 and pink>.95,
                'Hero downward cloud view contains a screen-fixed fog horizon or missing sea detail in '+name+': '+str(patches[name]))
    return {'file':str(path),'sha256':hashlib.sha256(Path(path).read_bytes()).hexdigest(),
            'result':'PASS_WORLD_ROTATED_CLOUD_COVERAGE','patches':patches,
            'scope':'Four outer native-overhead pixel patches only; requires separate settled camera-pose provenance.'}


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--self-test',action='store_true');parser.add_argument('--native-overhead',type=Path);args=parser.parse_args()
    count=validate()
    if args.self_test:
        bad=deepcopy(read(ASSETS/'models/block/hero_fountain_emblem.json'));bad['textures']['east']=bad['textures']['west']
        try:emblem_contract(bad)
        except AssertionError:pass
        else:raise AssertionError('Mutation check accepted a backwards fountain emblem')
    if args.native_overhead:print(json.dumps(native_overhead(args.native_overhead),sort_keys=True))
    print(f'Hero Garden 0.25.3 assets passed: {count} native BlockItems, east-facing emblem and isolated wildlife-free sky. Self-test={args.self_test}.')
