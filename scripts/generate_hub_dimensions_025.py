#!/usr/bin/env python3
"""0.25 authored Chao World data; GameTest preset stays in the dev-only tree."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/chao_garden'
def write(path,value):
 path.parent.mkdir(parents=True,exist_ok=True)
 path.write_text(json.dumps(value,indent=2)+'\n')
for room in ('hub','kindergarten','garden','hero_garden','dark_garden'):
 outside=room!='hub'
 dimension_type={'ultrawarm':False,'natural':False,'coordinate_scale':1.0,
  'has_skylight':outside,'has_ceiling':False,'ambient_light':.30 if room=='dark_garden' else (.12 if outside else .45),
  'fixed_time':18000 if room=='hub' else 6000,'piglin_safe':True,'bed_works':False,
  'respawn_anchor_works':False,'has_raids':False,'logical_height':256,'min_y':0,
  'height':256,'infiniburn':'#minecraft:infiniburn_overworld',
  'effects':'chao_garden:dark_sky' if room=='dark_garden' else ('chao_garden:hero_sky' if room=='hero_garden' else ('minecraft:overworld' if outside else 'minecraft:the_end')),
  'monster_spawn_block_light_limit':0,'monster_spawn_light_level':0}
 write(DATA/'dimension_type'/(room+'.json'),dimension_type)
 write(DATA/'dimension'/(room+'.json'),{'type':'chao_garden:'+room,
  'generator':{'type':'minecraft:flat','settings':{'biome':'chao_garden:dark_twilight' if room=='dark_garden' else ('chao_garden:hero_daylight' if room=='hero_garden' else ('chao_garden:scenic_daylight' if outside else 'minecraft:the_void')),
   'features':False,'lakes':False,'layers':[{'height':1,'block':'minecraft:air'}],
   'structure_overrides':[]}}})
# Authored realms have no native wildlife population. A separate biome avoids
# changing Overworld plains or depending only on post-spawn entity cleanup.
write(DATA/'worldgen/biome/scenic_daylight.json',{
 'has_precipitation':False,'temperature':.8,'downfall':.4,
 'effects':{'fog_color':12638463,'sky_color':7907327,'water_color':0x80bcc5,'water_fog_color':0x366971},
 'carvers':{},'features':[],'spawners':{},'spawn_costs':{},'creature_spawn_probability':0.0})
# The dedicated server only uses the flat world preset. Preserve its three exact
# vanilla dimensions, then add data-defined realms without shipping this preset.
fixture=json.loads((ROOT/'art/fixtures/vanilla_flat_preset_1211.json').read_text())
for room in ('hub','kindergarten','garden','hero_garden','dark_garden'):
 fixture['dimensions']['chao_garden:'+room]=json.loads((DATA/'dimension'/(room+'.json')).read_text())
preset=Path('data/minecraft/worldgen/world_preset/flat.json')
(ROOT/'src/main/resources'/preset).unlink(missing_ok=True)
write(ROOT/'src/gameTestDimensions/resources'/preset,fixture)
print('Chao World0255: five isolated realms, dedicated Hero/Dark skies, eight-dimension dev preset generated.')
