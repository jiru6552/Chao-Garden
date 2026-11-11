#!/usr/bin/env python3
"""Original Chao orchard models, hanging fruit, seed icons and recipes."""
from pathlib import Path
import json
import os
import sys
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / 'src/main/resources/assets/chao_garden'
DATA = ROOT / 'src/main/resources/data'
sys.path.insert(0, str(ROOT/'art'))
from sa2_fruit_art import PALETTES, sprite as fruit_sprite, skin as fruit_skin, solids as fruit_solids
from sa2_seed_art import sprite as seed_sprite
COLORS = {name: values[0] for name, values in PALETTES.items()}
NATURAL = ['garden_fruit','hero_garden_fruit','dark_garden_fruit','strong_fruit','tasty_fruit']
MARKET = ['round_fruit','triangle_fruit','square_fruit','heart_fruit','chao_fruit','mushroom','mushroom_alt']
ADVENTURE = ['energy_fruit','peace_fruit','smart_fruit','tall_fruit','vitality_fruit','rare_fruit','wonder_fruit','creepy_fruit','rough_fruit','forbidden_fruit']
ACTIVE = NATURAL + MARKET + ADVENTURE
NAMES = {'garden_fruit':'Chao Garden Fruit','hero_garden_fruit':'Hero Garden Fruit','dark_garden_fruit':'Dark Garden Fruit','mushroom_alt':'Nutritious Mushroom'}
FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(path.name + '.writing')
    with temp.open('w') as handle:
        json.dump(data, handle, indent=2)
        handle.write('\n');handle.flush();os.fsync(handle.fileno())
    os.replace(temp, path)


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(path.name+'.writing')
    with temp.open('wb') as handle:
        image.save(handle, format='PNG');handle.flush();os.fsync(handle.fileno())
    os.replace(temp, path)


def merge_tag(path, values):
    existing = json.loads(path.read_text()).get('values', []) if path.exists() else []
    for value in values:
        if value not in existing:existing.append(value)
    write(path, {'replace':False, 'values':existing})


def shade(color, factor):
    return tuple(max(0, min(255, round(channel*factor))) for channel in color)


def cube(a, b, tex='bark', rotation=None):
    sx,sy,sz=[b[i]-a[i] for i in range(3)]
    sizes={'north':(sx,sy),'south':(sx,sy),'east':(sz,sy),'west':(sz,sy),'up':(sx,sz),'down':(sx,sz)}
    element = {'from': a, 'to': b, 'faces': {face: {'texture': '#'+tex, 'uv': [0, 0, *sizes[face]]} for face in FACES}}
    if rotation:element['rotation'] = rotation
    return element


def loot(item, *, condition=None):
    entry = {'type': 'minecraft:item', 'name': 'chao_garden:'+item}
    if condition:entry['conditions'] = [condition]
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [entry],
             'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}


def generate():
    lang = {}
    # Quiet vertical bark reads as a full Minecraft log rather than planks.
    bark = Image.new('RGBA', (16,16), '#8e7850');d=ImageDraw.Draw(bark)
    for x in (0,5,11):
        d.rectangle((x,0,x+1,15),fill='#65573b')
        d.line((x+2,0,x+2,15),fill='#a08a5c')
    for x,y in ((1,3),(6,11),(12,7),(15,2)):
        d.rectangle((x,y,x+1,y+3),fill='#756242')
    save(bark, ASSET/'textures/block/chao_fruit_bark.png')
    end=Image.new('RGBA',(16,16),'#bc995f');d=ImageDraw.Draw(end)
    for inset in (1,4,7):d.rectangle((inset,inset,15-inset,15-inset),outline='#907445')
    save(end,ASSET/'textures/block/chao_fruit_log_top.png')
    leaves=Image.new('RGBA',(16,16),'#65975b');d=ImageDraw.Draw(leaves)
    d.rectangle((0,0,15,2),fill='#82af69');d.rectangle((0,12,15,15),fill='#4e784d')
    for x in (1,5,9,13):d.line((x,2,x+1,13),fill='#739f5e')
    for x,y in ((0,0),(15,0),(0,15),(15,15),(0,7),(15,5)):d.point((x,y),fill=(0,0,0,0))
    save(leaves,ASSET/'textures/block/chao_fruit_leaves.png')
    # A separate grayscale cluster atlas receives the same biome foliage tint
    # as oak leaves. The small holes and pixel clusters are actual leaf faces,
    # not scaled-out frond geometry. The old colored patch stays for old palms.
    foliage=Image.new('RGBA',(16,16),(0,0,0,0));pixels=foliage.load()
    centers=((1,2,3),(6,1,3),(12,3,3),(3,8,3),(9,8,3),(15,10,3),(5,14,3),(11,14,3))
    for y in range(16):
        for x in range(16):
            nearest=min(((min(abs(x-cx),16-abs(x-cx)))**2+(min(abs(y-cy),16-abs(y-cy)))**2,cx,cy,r)
                        for cx,cy,r in centers)
            distance,cx,cy,r=nearest
            if distance>r*r or (x*11+y*7)%29==0:continue
            tone=(194 if distance<3 else 150 if distance<7 else 112)
            if (x+y*3)%7==0:tone+=18
            pixels[x,y]=(tone,tone,tone,255)
    save(foliage,ASSET/'textures/block/chao_orchard_leaves.png')
    write(ASSET/'models/block/chao_fruit_leaves.json',{
        'parent':'minecraft:block/leaves','render_type':'minecraft:cutout_mipped',
        'textures':{'all':'chao_garden:block/chao_orchard_leaves'}})
    write(ASSET/'models/item/chao_fruit_leaves.json',{'parent':'chao_garden:block/chao_fruit_leaves'})
    write(ASSET/'blockstates/chao_fruit_leaves.json',{'variants':{'':{'model':'chao_garden:block/chao_fruit_leaves'}}})
    tools={'condition':'minecraft:any_of','terms':[
        {'condition':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}},
        {'condition':'minecraft:match_tool','predicate':{'predicates':{'minecraft:enchantments':[
            {'enchantments':'minecraft:silk_touch','levels':{'min':1}}]}}}]}
    write(DATA/'chao_garden/loot_table/blocks/chao_fruit_leaves.json',{
        'type':'minecraft:block','pools':[
            {'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'},tools],
             'entries':[{'type':'minecraft:item','name':'chao_garden:chao_fruit_leaves'}]},
            {'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'},
                {'condition':'minecraft:inverted','term':tools},{'condition':'minecraft:random_chance','chance':.02}],
             'entries':[{'type':'minecraft:item','name':'minecraft:stick',
                 'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':2}}]}]}]})
    write(ASSET/'models/block/chao_fruit_log.json', {'parent':'minecraft:block/cube_column',
          'textures': {'side':'chao_garden:block/chao_fruit_bark','end':'chao_garden:block/chao_fruit_log_top'}})
    write(ASSET/'models/item/chao_fruit_log.json', {'parent':'chao_garden:block/chao_fruit_log'})
    write(ASSET/'blockstates/chao_fruit_log.json', {'variants': {
        'axis=y': {'model':'chao_garden:block/chao_fruit_log'},
        'axis=x': {'model':'chao_garden:block/chao_fruit_log','x':90,'y':90},
        'axis=z': {'model':'chao_garden:block/chao_fruit_log','x':90}}})
    write(DATA/'chao_garden/loot_table/blocks/chao_fruit_log.json',loot('chao_fruit_log'))
    lang['block.chao_garden.chao_fruit_log']='Chao Fruit Log'
    lang['block.chao_garden.chao_fruit_leaves']='Chao Fruit Leaves'
    # Four real branch arms, the central growing tip and twelve chunky palm
    # fronds. Stem ends from the pods meet the branch arms from below.
    elements=[cube([5,0,5],[11,10,11]),cube([-8,3,6],[8,7,10]),cube([8,3,6],[24,7,10]),
              cube([6,3,-8],[10,7,8]),cube([6,3,8],[10,7,24]),
              cube([3,9,3],[13,14,13],'leaves')]
    elements += [cube([5,9,-4],[11,13,8],'leaves'),cube([6,6,-12],[10,10,0],'leaves'),cube([7,3,-16],[9,7,-8],'leaves'),
                 cube([5,9,8],[11,13,20],'leaves'),cube([6,6,16],[10,10,28],'leaves'),cube([7,3,24],[9,7,32],'leaves'),
                 cube([-4,9,5],[8,13,11],'leaves'),cube([-12,6,6],[0,10,10],'leaves'),cube([-16,3,7],[-8,7,9],'leaves'),
                 cube([8,9,5],[20,13,11],'leaves'),cube([16,6,6],[28,10,10],'leaves'),cube([24,3,7],[32,7,9],'leaves')]
    write(ASSET/'models/block/chao_fruit_crown.json', {'render_type':'minecraft:cutout','ambientocclusion':False,
          'textures':{'particle':'chao_garden:block/chao_fruit_leaves','bark':'chao_garden:block/chao_fruit_bark','leaves':'chao_garden:block/chao_fruit_leaves'},
          'elements':elements})
    write(ASSET/'models/block/chao_fruit_crown_expanded.json',{
        'parent':'minecraft:block/cube_column',
        'textures':{'side':'chao_garden:block/chao_fruit_bark','end':'chao_garden:block/chao_fruit_log_top'}})
    mineable=['chao_garden:chao_fruit_log']
    saplings=[]
    for fruit, hexcolor in COLORS.items():
        color=tuple(bytes.fromhex(hexcolor[1:]));name=NAMES.get(fruit,fruit.replace('_',' ').title());seed=fruit+'_seed'
        if fruit in ACTIVE:
            save(fruit_sprite(fruit),ASSET/'textures/item'/f'{fruit}.png')
            write(ASSET/'models/item'/f'{fruit}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'chao_garden:item/{fruit}'}})
        lang['item.chao_garden.'+fruit]=name
        save(seed_sprite(fruit),ASSET/'textures/item'/f'{seed}.png')
        write(ASSET/'models/item'/f'{seed}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'chao_garden:item/{seed}'}})
        lang['item.chao_garden.'+seed]=name+' Seed'
        lang['block.chao_garden.'+fruit+'_seedling']=name+' Seedling'
        lang['block.chao_garden.'+fruit+'_pod']='Hanging '+name
        lang['block.chao_garden.'+fruit+'_tree_crown']=name+' Tree Crown'
        sapling=Image.new('RGBA',(16,16));d=ImageDraw.Draw(sapling)
        d.rectangle((7,3,8,15),fill='#8f7048');d.line((4,6,8,10),fill='#8f7048');d.line((8,8,11,5),fill='#8f7048')
        for a,b,fill in (((4,1),(10,5),'#6e9a51'),((1,4),(6,8),'#588748'),((9,3),(14,7),'#5f8c49'),((4,6),(11,10),'#466f3e')):
            d.rectangle((*a,*b),fill=fill)
        d.rectangle((5,1,8,2),fill='#8aaf64');d.rectangle((2,4,4,5),fill='#7da65b');d.rectangle((10,3,12,4),fill='#79a259')
        d.rectangle((7,6,8,7),fill=color)
        save(sapling,ASSET/'textures/block'/f'{fruit}_seedling.png')
        write(ASSET/'models/block'/f'{fruit}_seedling.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout',
              'textures':{'cross':f'chao_garden:block/{fruit}_seedling'}})
        write(ASSET/'blockstates'/f'{fruit}_seedling.json',{'variants':{f'stage={n}':{'model':f'chao_garden:block/{fruit}_seedling'} for n in (0,1)}})
        write(DATA/'chao_garden/loot_table/blocks'/f'{fruit}_seedling.json',loot(seed))
        saplings.append('chao_garden:'+fruit+'_seedling')
        write(ASSET/'blockstates'/f'{fruit}_tree_crown.json',{'variants':{
            'expanded=false':{'model':'chao_garden:block/chao_fruit_crown'},
            'expanded=true':{'model':'chao_garden:block/chao_fruit_crown_expanded'}}})
        write(DATA/'chao_garden/loot_table/blocks'/f'{fruit}_tree_crown.json',loot(seed))
        if fruit in ACTIVE and not fruit.startswith('mushroom'):
            write(DATA/'chao_garden/recipe'/f'{seed}.json',{'type':'minecraft:crafting_shapeless',
                  'ingredients':[{'item':'chao_garden:'+fruit}],'result':{'id':'chao_garden:'+seed,'count':1}})
        else:
            (DATA/'chao_garden/recipe'/f'{seed}.json').unlink(missing_ok=True)
        variants={}
        for age in range(4):
            save(fruit_skin(fruit,age),ASSET/'textures/block'/f'{fruit}_pod_{age}.png')
            # Reuse SA2 silhouette and surface identity at every ripening stage.
            scale=(.45,.65,.82,1)[age]
            parts=fruit_solids(fruit,scale)
            top=max(b[1] for a,b,tex in parts)
            # Dark Garden bare limbs begin six pixels into the cell above.
            # Their real pods need a stem that reaches that visible underside.
            stem_top=22 if fruit=='dark_garden_fruit' else 20
            pod=[cube([7.5,top,7.5],[8.5,stem_top,8.5],'bark')]
            pod += [cube(a,b,tex) for a,b,tex in parts]
            write(ASSET/'models/block'/f'{fruit}_pod_{age}.json',{'ambientocclusion':False,
                  'textures':{'particle':f'chao_garden:block/{fruit}_pod_{age}','fruit':f'chao_garden:block/{fruit}_pod_{age}','bark':'chao_garden:block/chao_fruit_bark','stem':'chao_garden:block/sa2_mushroom_stem'},'elements':pod})
            for facing,rotation in (('north',0),('east',90),('south',180),('west',270)):
                variants[f'age={age},facing={facing}']={'model':f'chao_garden:block/{fruit}_pod_{age}','y':rotation}
        write(ASSET/'blockstates'/f'{fruit}_pod.json',{'variants':variants})
        write(DATA/'chao_garden/loot_table/blocks'/f'{fruit}_pod.json',loot(fruit,condition={
              'condition':'minecraft:block_state_property','block':'chao_garden:'+fruit+'_pod','properties':{'age':'3'}}))
        mineable.extend(['chao_garden:'+fruit+'_tree_crown'])
    merge_tag(DATA/'minecraft/tags/block/mineable/axe.json', mineable)
    merge_tag(DATA/'minecraft/tags/block/saplings.json', saplings)
    merge_tag(DATA/'minecraft/tags/block/leaves.json',['chao_garden:chao_fruit_leaves'])
    merge_tag(DATA/'minecraft/tags/block/mineable/hoe.json',['chao_garden:chao_fruit_leaves'])
    logs=['chao_garden:chao_fruit_log']+['chao_garden:'+fruit+'_tree_crown' for fruit in COLORS]
    merge_tag(DATA/'minecraft/tags/block/logs_that_burn.json',logs)
    merge_tag(DATA/'minecraft/tags/block/logs.json',logs)
    write(ROOT/'docs/fruit_lang_015.json',lang)
    write(ROOT/'docs/fruit_catalog.json',{'format_version':3,'fruits':[{'fruit':fruit,'seed':None if fruit.startswith('mushroom') else fruit+'_seed','color':COLORS[fruit],'source':'overworld_tree' if fruit in NATURAL else 'rotating_black_market_adventure_fruit' if fruit in ADVENTURE else 'rotating_black_market_fruit'} for fruit in ACTIVE], 'legacy_trees':['stamina_fruit'],
          'tree_height':7,'canopy_width':5,'crown_height':4,'pod_radius':2,'leaf_blocks':42,
          'fruit_pods':4,'ripening_stages':4,'legacy_palms':'Retained; bone meal upgrades safely when space permits.'})
    # Prior chest/enemy tables must not bypass the market-first fruit path.
    for path in (DATA/'chao_garden/loot_table/bonus').glob('*.json'):
        table=json.loads(path.read_text())
        changed=False
        for pool in table.get('pools',[]):
            entries=pool.get('entries',[])
            kept=[e for e in entries if e.get('name','').removeprefix('chao_garden:') not in set(MARKET+ADVENTURE+['mint_candy','stamina_fruit'])]
            if kept!=entries:pool['entries']=kept;changed=True
        if changed:write(path,table)
    stem=Image.new('RGBA',(16,16),'#e0c18a');d=ImageDraw.Draw(stem)
    for x in (2,7,12):d.line((x,0,x,15),fill='#c6a472')
    save(stem,ASSET/'textures/block/sa2_mushroom_stem.png')
    write(ROOT/'docs/fruit_items_015.json',list(ACTIVE)+[fruit+'_seed' for fruit in COLORS])
    print(f'Generated {len(ACTIVE)} active SA2-inspired foods ({len(NATURAL)} wild / {len(MARKET)+len(ADVENTURE)} market-first), retaining old tree IDs.')


if __name__=='__main__':generate()
