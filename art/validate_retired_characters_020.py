#!/usr/bin/env python3
"""Independent active-content and compatibility resource checks for removed characters."""
from pathlib import Path
import json,re

ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden'
checks=0
def require(value,message):
    global checks;checks+=1
    if not value:raise AssertionError(message)
for character in ('spartoi','egg'):
    require(not(A/'models/entity/gameplay'/f'chao_{character}.json').exists(),'Retired character mesh remains: '+character)
    require(not(A/'textures/entity/chao/gameplay'/f'character_{character}.png').exists(),'Retired character skin remains: '+character)
require(not(A/'textures/item/spartoi_lens.png').exists(),'Retired lens artwork remains')
require(json.loads((A/'models/item/spartoi_lens.json').read_text())=={'parent':'chao_garden:item/clear_lens'},'Compatibility lens must use its safe ordinary icon')
require(json.loads((A/'lang/en_us.json').read_text())['item.chao_garden.spartoi_lens']=='Retired Lens','Compatibility item advertised as an active variant')
for required in ('models/entity/egg_mesh.json','models/item/normal_egg.json','textures/item/normal_egg.png'):
    require((A/required).exists(),'Ordinary egg resources were removed: '+required)
for directory in ('official','gameplay'):
    require((A/f'models/entity/{directory}/chao_baby.json').exists(),'Ordinary Chao geometry was removed')
from build_cwe_content_019 import GIFTS
require('spartoi_lens' not in GIFTS,'Retired lens still generated in active gift catalog')
active_java=(ROOT/'src/main/java/com/chaogarden/cwe/CweCatalog.java').read_text()
require('"chaos","spartoi"' not in active_java,'Retired lens still in active Java catalog')
market_java=(ROOT/'src/main/java/com/chaogarden/market/MarketStock.java').read_text()
require('"spartoi_lens"' not in market_java,'Retired lens still offered by merchant')
result={'release':'0.20.0','checks':checks,'retired_characters':['spartoi','egg'],'ordinary_eggs_preserved':True,'compatibility_item':'spartoi_lens','result':'PASS'}
path=ROOT/'docs/validation-0.20/retired-characters.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(result,indent=2)+'\n')
print(f'Retired character resources: {checks} checks passed.')
