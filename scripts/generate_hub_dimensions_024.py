#!/usr/bin/env python3
"""Dimension/runtime data for the two small Chao public rooms."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/chao_garden'
def write(rel,value):
 p=DATA/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
for room,ambient in [('hub',.45),('kindergarten',.65)]:
 write('dimension_type/'+room+'.json',{'ultrawarm':False,'natural':False,'coordinate_scale':1.0,'has_skylight':False,'has_ceiling':True,'ambient_light':ambient,'fixed_time':18000,'piglin_safe':True,'bed_works':False,'respawn_anchor_works':False,'has_raids':False,'logical_height':256,'min_y':0,'height':256,'infiniburn':'#minecraft:infiniburn_overworld','effects':'minecraft:the_end','monster_spawn_block_light_limit':0,'monster_spawn_light_level':0})
 write('dimension/'+room+'.json',{'type':'chao_garden:'+room,'generator':{'type':'minecraft:flat','settings':{'biome':'minecraft:the_void','features':False,'lakes':False,'layers':[{'height':1,'block':'minecraft:air'}],'structure_overrides':[]}}})
write('recipe/chao_teleporter.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['GCG','GEG','CCC'],'key':{'G':{'item':'minecraft:glass'},'C':{'item':'minecraft:copper_ingot'},'E':{'item':'minecraft:emerald'}},'result':{'id':'chao_garden:chao_teleporter','count':1}})
write('loot_table/blocks/chao_teleporter.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'chao_garden:chao_teleporter','conditions':[{'condition':'minecraft:block_state_property','block':'chao_garden:chao_teleporter','properties':{'half':'lower'}}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
# GameTestServer deliberately ignores ordinary custom dimensions and uses only
# the flat world preset. This exact dev-only preset preserves all three vanilla
# entries and adds the same two data-defined room configurations. Its dedicated
# resource tree is wired only into GameTest runs, never normal clients or JARs.
reference=ROOT/'art/fixtures/vanilla_flat_preset_1211.json'
fixture=json.loads(reference.read_text())
for room in ('hub','kindergarten'):
 fixture['dimensions']['chao_garden:'+room]=json.loads((DATA/'dimension'/(room+'.json')).read_text())
preset=Path('data/minecraft/worldgen/world_preset/flat.json')
# Remove the old location so a complete regeneration also repairs working trees
# created before this fixture was separated from production resources.
legacy=ROOT/'src/main/resources'/preset
legacy.unlink(missing_ok=True)
p=ROOT/'src/gameTestDimensions/resources'/preset
p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(fixture,indent=2)+'\n')
print('Chao hub024: two void-backed dimensions, reversible walk-through tube and early-game recipe generated.')
