#!/usr/bin/env python3
"""Author the ten confirmed Chao Adventure 2 fruit profiles and their orchards.

Source images are visual references only. No downloaded reference pixels are
embedded. Run after older orchard/015/017 finalizers so Smart Fruit is renewed.
"""
from pathlib import Path
import importlib.util,json,sys
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'art'))
from sa2_fruit_art import PALETTES,ADVENTURE_SHAPES,sprite,skin,solids
from sa2_seed_art import sprite as seed_sprite

FRUITS=('energy_fruit','peace_fruit','smart_fruit','tall_fruit','vitality_fruit',
        'rare_fruit','wonder_fruit','creepy_fruit','rough_fruit','forbidden_fruit')

def main():
    spec=importlib.util.spec_from_file_location('orchard_helpers',ROOT/'scripts/generate_fruit_assets.py')
    g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
    a,d=g.ASSET,g.DATA/'chao_garden';lang={}
    saplings=[];crowns=[]
    for fruit in FRUITS:
        name=fruit.replace('_',' ').title();seed=fruit+'_seed'
        g.save(sprite(fruit),a/'textures/item'/f'{fruit}.png')
        g.save(seed_sprite(fruit),a/'textures/item'/f'{seed}.png')
        for item in (fruit,seed):
            g.write(a/'models/item'/f'{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'chao_garden:item/{item}'}})
        lang['item.chao_garden.'+fruit]=name;lang['item.chao_garden.'+seed]=name+' Seed'
        for suffix,title in (('seedling',name+' Seedling'),('pod','Hanging '+name),('tree_crown',name+' Tree Crown')):
            lang[f'block.chao_garden.{fruit}_{suffix}']=title
        seedling=Image.new('RGBA',(16,16));draw=ImageDraw.Draw(seedling)
        draw.rectangle((7,4,8,15),fill='#8f7048')
        for box,color in (((3,1,10,5),'#6e9a51'),((1,5,7,8),'#588748'),((9,4,14,8),'#5f8c49')):
            draw.rectangle(box,fill=color)
        draw.rectangle((6,3,8,5),fill=PALETTES[fruit][0]);g.save(seedling,a/'textures/block'/f'{fruit}_seedling.png')
        g.write(a/'models/block'/f'{fruit}_seedling.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':f'chao_garden:block/{fruit}_seedling'}})
        g.write(a/'blockstates'/f'{fruit}_seedling.json',{'variants':{f'stage={n}':{'model':f'chao_garden:block/{fruit}_seedling'} for n in (0,1)}})
        g.write(a/'blockstates'/f'{fruit}_tree_crown.json',{'variants':{
            'expanded=false':{'model':'chao_garden:block/chao_fruit_crown'},
            'expanded=true':{'model':'chao_garden:block/chao_fruit_crown_expanded'}}})
        for block in ('seedling','tree_crown'):g.write(d/'loot_table/blocks'/f'{fruit}_{block}.json',g.loot(seed))
        g.write(d/'recipe'/f'{seed}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'chao_garden:'+fruit}],
            'result':{'id':'chao_garden:'+seed,'count':1}})
        variants={}
        for age,scale in enumerate((.45,.65,.82,1)):
            g.save(skin(fruit,age),a/'textures/block'/f'{fruit}_pod_{age}.png')
            parts=solids(fruit,scale);top=max(b[1] for aa,b,t in parts)
            elements=[g.cube([7.5,top,7.5],[8.5,20,8.5],'bark')]
            elements.extend(g.cube(aa,b,t) for aa,b,t in parts)
            g.write(a/'models/block'/f'{fruit}_pod_{age}.json',{'ambientocclusion':False,
                'textures':{'particle':f'chao_garden:block/{fruit}_pod_{age}','fruit':f'chao_garden:block/{fruit}_pod_{age}',
                    'bark':'chao_garden:block/chao_fruit_bark'},'elements':elements})
            for facing,rotation in (('north',0),('east',90),('south',180),('west',270)):
                variants[f'age={age},facing={facing}']={'model':f'chao_garden:block/{fruit}_pod_{age}','y':rotation}
        g.write(a/'blockstates'/f'{fruit}_pod.json',{'variants':variants})
        g.write(d/'loot_table/blocks'/f'{fruit}_pod.json',g.loot(fruit,condition={
            'condition':'minecraft:block_state_property','block':f'chao_garden:{fruit}_pod','properties':{'age':'3'}}))
        saplings.append(f'chao_garden:{fruit}_seedling');crowns.append(f'chao_garden:{fruit}_tree_crown')
    g.merge_tag(g.DATA/'minecraft/tags/block/saplings.json',saplings)
    for tag in ('mineable/axe','logs','logs_that_burn'):g.merge_tag(g.DATA/f'minecraft/tags/block/{tag}.json',crowns)
    current=json.loads((a/'lang/en_us.json').read_text());current.update(lang);g.write(a/'lang/en_us.json',current)
    g.write(ROOT/'docs/fruit_lang_018.json',lang)
    # Keep preview/rebuild ledgers current even when this narrow finalizer runs alone.
    introduced=[item for fruit in FRUITS for item in (fruit,fruit+'_seed')]
    ledger=ROOT/'scripts/resource_items.json'
    items=json.loads(ledger.read_text()) if ledger.exists() else []
    g.write(ledger,list(dict.fromkeys(items+introduced)))
    g.write(ROOT/'docs/fruit_items_018.json',introduced)
    catalog_path=ROOT/'docs/fruit_catalog.json'
    catalog=json.loads(catalog_path.read_text())
    catalog['format_version']=3
    catalog['fruits']=[{'fruit':fruit,'seed':None if fruit.startswith('mushroom') else fruit+'_seed',
                       'color':g.COLORS[fruit],
                       'source':'overworld_tree' if fruit in g.NATURAL else
                           'rotating_black_market_adventure_fruit' if fruit in FRUITS else 'rotating_black_market_fruit'}
                      for fruit in g.ACTIVE]
    catalog['legacy_trees']=['stamina_fruit']
    g.write(catalog_path,catalog)
    # Older orchard generators retain compatibility blocks, not new mushroom seed paths.
    for fruit in ('mushroom','mushroom_alt'):
        # 1.21 uses singular recipe; remove older plural paths too so a legacy
        # generator cannot leave an acquisition route in either source layout.
        for directory in ('recipe','recipes'):
            (ROOT/'src/main/resources/data/chao_garden'/directory/f'{fruit}_seed.json').unlink(missing_ok=True)
        for block in ('seedling','tree_crown'):g.write(d/'loot_table/blocks'/f'{fruit}_{block}.json',g.loot(fruit))
    print('CA2 fruit assets: 10 profiles, 10 seeds/orchards, four ripening stages; no mushroom seed acquisition.')

if __name__=='__main__':main()
