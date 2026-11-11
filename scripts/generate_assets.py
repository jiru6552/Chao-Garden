#!/usr/bin/env python3
"""Rebuild original Chao Garden pixel art and all hand-authored data resources."""
from pathlib import Path
import json, math, random, sys
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'art'))
from item_sprite_art import egg, drive, save_sprite
RES=ROOT/'src/main/resources'
MOD='chao_garden'
# Empty carriers were removed; retain only their saved filled-item registry compatibility.
(RES/'data'/MOD/'recipe/chao_carrier.json').unlink(missing_ok=True)
COLORS={'normal':'6ed3e3','white':'f4f4e7','red':'df554f','yellow':'f5d65d','blue':'4b81d8','aqua':'5addbd','pink':'f3a3c5','orange':'ee9a4f','purple':'ad77d2','brown':'a17954','green':'5aa869','grey':'969eaf','lime':'b6df64','black':'424c66','sky_blue':'9bcdf0','cyan':'47d6e5'}
JEWELS={'silver':'b8d7e0','gold':'f2c559','ruby':'ee4269','sapphire':'4271ed','emerald':'3ad18a','amethyst':'b184ed','aquamarine':'69e5e2','garnet':'ad3957','onyx':'4b4c64','diamond':'c2effb','peridot':'b6df58','topaz':'f2a74e'}
FRUITS=['garden_fruit','strong_fruit','tasty_fruit','hero_fruit','dark_fruit','heart_fruit','chao_fruit','mushroom','smart_fruit','mint_candy','stamina_fruit','round_fruit','square_fruit','triangle_fruit']
DRIVES=['swim','fly','run','power']
ANIMALS=['penguin','seal','otter','rabbit','cheetah','warthog','bear','tiger','gorilla','peacock','parrot','condor','skunk','sheep','raccoon','half_fish','skeleton_dog','bat','dragon','unicorn','phoenix']
EGGS=['normal_egg']+[f'{c}_egg' for c in COLORS if c!='normal']+['shiny_egg']+[f'shiny_{c}_egg' for c in COLORS if c!='normal']+[f'{j}_jewel_egg' for j in JEWELS]
TOOLS=['chao_analyzer','chao_carrier','garden_guide','chao_spawn_egg']
ITEMS=EGGS+FRUITS+[f'{d}_drive' for d in DRIVES]+[f'{a}_animal' for a in ANIMALS]+TOOLS

def write(path,obj):
 p=RES/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,indent=2)+'\n')
def rgb(h):return tuple(bytes.fromhex(h))
def tint(c,f):return tuple(max(0,min(255,round(x*f))) for x in c)
def canvas():return Image.new('RGBA',(16,16))
def save(im,name):
 p=RES/f'assets/{MOD}/textures/item/{name}.png';save_sprite(im,p)
for name in EGGS:
 shiny=name.startswith('shiny');jewel='_jewel_' in name
 if jewel:c=JEWELS[name.split('_jewel')[0]]
 else:
  key=name.replace('shiny_','').replace('_egg','')
  c=COLORS.get(key,COLORS['normal'])
 save(egg(c,shiny,jewel),name)

fruit_colors=['df9a58','ecbd45','d66f8e','efeee3','81599d','e8719e','69c8df','b49562','739ee4','8cddb9','a6d665','e7b956','b18bd8','7ec598']
for name,h in zip(FRUITS,fruit_colors):
 im=canvas();d=ImageDraw.Draw(im);c=rgb(h)
 if name=='square_fruit':
  d.rectangle((7,1,8,3),fill='#6c5741');d.polygon([(8,3),(11,1),(13,2),(10,4)],fill='#538b58');d.rectangle((3,4,12,13),fill=(*tint(c,.48),255));d.rectangle((4,5,11,12),fill='#'+h);d.line((5,6,5,9),fill=(*tint(c,1.4),255));d.line((9,11,10,9),fill=(*tint(c,.78),255))
 elif name=='triangle_fruit':
  d.rectangle((7,1,8,3),fill='#6c5741');d.polygon([(8,3),(11,1),(13,2),(10,4)],fill='#538b58');d.polygon([(7,3),(8,3),(14,13),(1,13)],fill=(*tint(c,.48),255));d.polygon([(7,5),(8,5),(12,12),(3,12)],fill='#'+h);d.line((7,6,5,10),fill=(*tint(c,1.4),255));d.line((9,10,10,11),fill=(*tint(c,.78),255))
 elif name=='mushroom':
  d.rectangle((6,8,9,13),fill='#b99867');d.rectangle((7,8,8,12),fill='#f5e1b1');d.polygon([(2,8),(3,5),(5,3),(10,3),(12,5),(13,8)],fill='#654a54');d.polygon([(3,7),(4,5),(6,4),(10,4),(12,7)],fill='#b86f7b');d.rectangle((5,5,6,6),fill='#f5dfbe');d.point((10,6),fill='#f5dfbe')
 elif name=='mint_candy':
  d.polygon([(1,5),(5,7),(5,10),(1,12)],fill='#53a7a4');d.polygon([(14,5),(10,7),(10,10),(14,12)],fill='#53a7a4');d.rectangle((5,5,10,11),fill='#d1fff0');d.line((6,6,9,9),fill='#4cb895',width=2);d.point((6,6),fill='#ffffff')
 elif name=='heart_fruit':
  d.polygon([(3,4),(6,4),(8,6),(10,4),(12,4),(14,6),(14,9),(8,14),(2,9),(2,6)],fill='#6b3d59');d.polygon([(3,6),(4,5),(6,5),(8,7),(10,5),(12,5),(13,6),(13,9),(8,13),(3,9)],fill='#'+h);d.line((4,6,5,6),fill='#ffcfdd')
 else:
  d.rectangle((7,1,8,4),fill='#6c5741');d.polygon([(8,3),(11,1),(13,2),(10,4)],fill='#538b58');d.polygon([(4,5),(6,4),(10,4),(12,5),(13,8),(12,12),(10,14),(6,14),(3,11),(2,8)],fill=(*tint(c,.48),255));d.polygon([(4,6),(6,5),(10,5),(11,6),(12,8),(11,12),(9,13),(6,13),(4,11),(3,8)],fill='#'+h);d.rectangle((5,6,6,8),fill=(*tint(c,1.4),255));d.line((9,11,10,9),fill=(*tint(c,.78),255))
  if name=='hero_fruit':d.polygon([(8,7),(10,9),(8,11),(6,9)],fill='#d6b85a')
  if name=='dark_fruit':d.polygon([(8,6),(10,10),(6,10)],fill='#df87c0')
  if name=='chao_fruit':d.rectangle((6,8,6,9),fill='#273f5a');d.rectangle((9,8,9,9),fill='#273f5a')
 save(im,name)

for name,h in zip(DRIVES,['f1ce58','b889e3','7edb7e','e9606a']):
 save(drive(h),name+'_drive')

animal_colors=['5283af','7a9daa','866449','dad4b9','e3b451','a77c59','92715a','da9752','777183','439ca3','5bbd78','8c71a8','514e64','ded4ba','8e8a88','7fcad3','c6c4b7','856297','73b786','e2d7f0','ed9c48']
for name,h in zip(ANIMALS,animal_colors):
 im=canvas();d=ImageDraw.Draw(im);c=rgb(h);outline=(*tint(c,.46),255)
 # Small animal companions are captured in a soft, readable portrait silhouette.
 d.rectangle((4,7,11,12),fill=outline);d.rectangle((5,7,10,11),fill='#'+h);d.rectangle((3,12,6,13),fill=outline);d.rectangle((9,12,12,13),fill=outline);d.rectangle((5,12,6,12),fill=(*tint(c,1.2),255));d.rectangle((9,12,10,12),fill=(*tint(c,1.2),255));d.rectangle((4,4,11,8),fill=outline);d.rectangle((5,4,10,8),fill='#'+h)
 if name in ['rabbit','unicorn']:d.rectangle((4,1,6,4),fill=outline);d.rectangle((5,1,5,4),fill=(*tint(c,1.2),255));d.rectangle((9,1,11,4),fill=outline);d.rectangle((10,1,10,4),fill=(*tint(c,1.2),255))
 elif name in ['bat','dragon','phoenix','parrot','condor','peacock']:
  d.polygon([(4,8),(1,5),(1,11),(4,12)],fill=outline);d.polygon([(11,8),(14,5),(14,11),(11,12)],fill=outline);d.line((2,8,3,10),fill='#'+h);d.line((13,8,12,10),fill='#'+h)
 else:d.rectangle((4,3,6,4),fill=outline);d.rectangle((9,3,11,4),fill=outline)
 d.point((6,6),fill='#1f293b');d.point((9,6),fill='#1f293b');d.rectangle((7,8,8,8),fill=(*tint(c,1.6),255))
 if name in ['penguin','sheep','skunk','raccoon','skeleton_dog']:d.rectangle((6,9,9,11),fill='#e3e6d4')
 if name in ['tiger','cheetah']:d.point((5,5),fill='#765034');d.point((10,8),fill='#765034');d.line((6,10,7,10),fill='#765034')
 if name=='penguin':d.rectangle((7,7,8,8),fill='#eebc61')
 if name=='half_fish':d.polygon([(12,8),(15,5),(15,12)],fill='#558eac');d.rectangle((4,11,11,13),fill=(0,0,0,0))
 if name=='unicorn':d.line((8,1,8,4),fill='#eaca83');d.point((8,0),fill='#fff1c1')
 if name=='phoenix':d.line((7,1,5,4),fill='#ffe081');d.line((9,1,9,4),fill='#ffd277');d.point((10,2),fill='#f0b34c')
 if name=='dragon':d.point((4,2),fill='#f1d789');d.point((11,2),fill='#f1d789')
 if name=='peacock':d.point((1,4),fill='#8fd95b');d.point((14,4),fill='#8fd95b')
 save(im,name+'_animal')

im=canvas();d=ImageDraw.Draw(im);d.rectangle((2,2,12,11),fill='#273d58');d.rectangle((3,3,11,10),fill='#c59d63');d.rectangle((4,4,10,9),fill='#78d9c6');d.rectangle((5,7,6,8),fill='#294b61');d.line((7,8,9,5),fill='#e2fff1');d.rectangle((6,12,8,14),fill='#5d7c8f');save(im,'chao_analyzer')
im=canvas();d=ImageDraw.Draw(im);d.rectangle((4,1,11,4),fill='#405570');d.rectangle((5,2,10,5),fill='#a5bfca');d.rectangle((2,5,13,14),fill='#405570');d.rectangle((3,6,12,13),fill='#b79062');d.rectangle((4,7,11,11),fill='#69bec6');d.line((5,7,5,11),fill='#d8fff1');d.line((8,7,8,11),fill='#d8fff1');d.line((11,7,11,11),fill='#d8fff1');d.rectangle((7,12,8,13),fill='#f4cf75');save(im,'chao_carrier')
im=canvas();d=ImageDraw.Draw(im);d.polygon([(2,2),(12,1),(13,12),(3,14),(2,13)],fill='#364d52');d.polygon([(3,3),(11,2),(11,11),(4,13)],fill='#699878');d.line((4,3,4,12),fill='#b2c899');d.rectangle((6,5,9,8),fill='#8cdce1');d.point((6,5),fill='#c0ffff');d.point((7,6),fill='#2c5361');d.point((9,6),fill='#2c5361');d.line((6,10,9,9),fill='#d9cb96');save(im,'garden_guide')
save(egg(COLORS['normal']),'chao_spawn_egg')
for name in ITEMS:write(Path(f'assets/{MOD}/models/item/{name}.json'),{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/{name}'}})
lang={f'item.{MOD}.{name}':name.replace('_',' ').title().replace('Jewel Egg','Jewel Chao Egg').replace(' Animal','').replace('Grey','Gray') for name in ITEMS}
lang.update({'itemGroup.chao_garden':'Chao Garden','entity.chao_garden.chao':'Chao','entity.chao_garden.chao_egg':'Chao Egg','item.chao_garden.normal_egg':'Normal Chao Egg','item.chao_garden.shiny_egg':'Shiny Chao Egg','item.chao_garden.chao_spawn_egg':'Chao Spawn Egg','item.chao_garden.garden_guide':'Chao Care Guide','item.chao_garden.chao_analyzer':'Chao Analyzer','item.chao_garden.chao_carrier':'Chao','item.chao_garden.half_fish_animal':'Half Fish','item.chao_garden.skeleton_dog_animal':'Skeleton Dog'})
for name in EGGS:
 if 'Jewel' not in lang[f'item.{MOD}.{name}'] and name not in ['normal_egg','shiny_egg']:lang[f'item.{MOD}.{name}']=lang[f'item.{MOD}.{name}'].replace(' Egg',' Chao Egg')
lang.update({'accessories.slot.chao_companion':'Chao Companion','accessories.slot_group.chao_companion':'Chao Companions'})
lang_path=RES/f'assets/{MOD}/lang/en_us.json'
if lang_path.exists():
 previous=json.loads(lang_path.read_text());previous.update(lang);lang=previous
write(Path(f'assets/{MOD}/lang/en_us.json'),lang)
write(Path('pack.mcmeta'),{'pack':{'pack_format':34,'description':'Chao Garden — Chao companions, gardens and activities'}})
write(Path(f'data/{MOD}/tags/item/garden_food.json'),{'replace':False,'values':[f'{MOD}:{n}' for n in FRUITS]})
write(Path(f'data/{MOD}/tags/item/chao_eggs.json'),{'replace':False,'values':[f'{MOD}:{n}' for n in EGGS]})

def shapeless(name,ingredients,result,count=1):
 write(Path(f'data/{MOD}/recipe/{name}.json'),{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':i} for i in ingredients],'result':{'id':result,'count':count}})
def shaped(name,pattern,key,result):
 write(Path(f'data/{MOD}/recipe/{name}.json'),{'type':'minecraft:crafting_shaped','category':'misc','pattern':pattern,'key':{k:{'item':v} for k,v in key.items()},'result':{'id':result,'count':1}})
# Eggs are distributed through chest policy and the Black Market; no egg recipes.
shapeless('garden_guide',['minecraft:book','minecraft:wheat_seeds'],f'{MOD}:garden_guide')
shaped('chao_analyzer',[' G ','ICI',' R '],{'G':'minecraft:glass_pane','I':'minecraft:iron_ingot','C':'minecraft:copper_ingot','R':'minecraft:redstone'},f'{MOD}:chao_analyzer')

modifiers=[]
def item(name,weight=1,count=None):
 e={'type':'minecraft:item','name':f'{MOD}:{name}','weight':weight}
 if count:e['functions']=[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':count[0],'max':count[1]}}]
 return e
def pool(entries,chance=1):
 p={'rolls':1,'entries':entries}
 if chance<1:p['conditions']=[{'condition':'minecraft:random_chance','chance':chance}]
 return p
def add_loot(name,tables,pools):
 write(Path(f'data/{MOD}/loot_table/bonus/{name}.json'),{'type':'minecraft:generic','pools':pools})
 terms=[{'condition':'neoforge:loot_table_id','loot_table_id':f'minecraft:{t}'} for t in tables]
 condition=terms[0] if len(terms)==1 else {'condition':'minecraft:any_of','terms':terms}
 write(Path(f'data/{MOD}/loot_modifiers/{name}.json'),{'type':'neoforge:add_table','conditions':[condition],'table':f'{MOD}:bonus/{name}'})
 modifiers.append(f'{MOD}:{name}')

supplies=[item('garden_fruit',12),item('tasty_fruit',5),item('strong_fruit',4)]+[item(f'{n}_drive',8) for n in DRIVES]
hostiles=['zombie','husk','drowned','skeleton','stray','bogged','spider','cave_spider','creeper','slime','magma_cube','blaze','ghast','enderman','endermite','silverfish','witch','pillager','vindicator','evoker','ravager','phantom','piglin','piglin_brute','hoglin','zoglin','shulker','guardian','elder_guardian','warden','wither','ender_dragon','breeze']
add_loot('enemy_supplies',[f'entities/{n}' for n in hostiles],[pool(supplies,.18)])
# Every animal type has an explicit survival route, including mythical ones.
animal_sources={
 'penguin':['chicken','snow_golem'], 'seal':['dolphin'], 'otter':['cod','salmon'], 'rabbit':['rabbit'],
 'cheetah':['ocelot'], 'warthog':['pig','hoglin'], 'bear':['polar_bear'], 'tiger':['wolf'],
 'gorilla':['ravager'], 'peacock':['chicken'], 'parrot':['parrot'], 'condor':['phantom'],
 'skunk':['fox'], 'sheep':['sheep'], 'raccoon':['panda'], 'half_fish':['guardian','elder_guardian'],
 'skeleton_dog':['skeleton','wither_skeleton'], 'bat':['bat'], 'dragon':['ender_dragon','wither'],
 'unicorn':['horse','skeleton_horse'], 'phoenix':['blaze','ghast']}
for animal,mobs in animal_sources.items():
 chance=.5 if animal=='dragon' else (.08 if animal in ['unicorn','phoenix'] else .2)
 add_loot(f'animal_{animal}',[f'entities/{n}' for n in mobs],[pool([item(f'{animal}_animal')],chance)])
# Small companions may be rescued from hostile loot, so no animal killing is required.
add_loot('rescued_animals',[f'entities/{n}' for n in hostiles],[pool([item(f'{n}_animal',1 if n in ['dragon','unicorn','phoenix'] else 5) for n in ANIMALS],.06)])
chests=['simple_dungeon','abandoned_mineshaft','woodland_mansion','stronghold_corridor','stronghold_crossing','stronghold_library','village/village_temple','desert_pyramid','jungle_temple','buried_treasure','shipwreck_supply','shipwreck_treasure','ancient_city','end_city_treasure','bastion_treasure','bastion_other','nether_bridge','ruined_portal','trial_chambers/reward','trial_chambers/reward_rare','trial_chambers/reward_ominous','trial_chambers/reward_ominous_rare']
add_loot('chest_supplies',[f'chests/{n}' for n in chests],[pool([item(e['name'].split(':')[1],e['weight'],(1,3)) for e in supplies],.55),pool([item(f'{n}_animal') for n in ANIMALS],.18)])
write(Path(f'data/{MOD}/loot_table/entities/chao.json'),{'type':'minecraft:entity','pools':[]})
write(Path('data/neoforge/loot_modifiers/global_loot_modifiers.json'),{'replace':False,'entries':modifiers})
(ROOT/'scripts/resource_items.json').write_text(json.dumps(ITEMS,indent=2)+'\n')
print(f'Generated {len(ITEMS)} item icons/models, {len(modifiers)} additive loot modifiers, base recipes, and language/tags.')
# Keep the optional themes and their language entries after regenerating the
# base resources. This subprocess uses only the project's authored catalog.
import subprocess, sys
subprocess.run([sys.executable,str(ROOT/'art/generate_variant_resources.py')],check=True)
