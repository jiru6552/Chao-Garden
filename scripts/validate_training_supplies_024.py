#!/usr/bin/env python3
"""Independent final training-loot resources and catalog boundary checks."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1];D=ROOT/'src/main/resources/data';checks=0

def check(pass_,message):
 global checks;checks+=1
 if not pass_:raise AssertionError(message)
def read(rel):return json.loads((D/rel).read_text())
def main():
 enemy=read('chao_garden/loot_table/bonus/enemy_supplies.json')
 check(all(not x.get('name','').endswith('_drive') for p in enemy['pools'] for x in p['entries']),'Legacy enemy drive lottery survived')
 check(sum(x.get('weight',1) for p in enemy['pools'] for x in p['entries'] if x.get('type')=='minecraft:empty')==32,'Original optional fruit odds changed')
 chest=read('chao_garden/loot_table/bonus/chest_supplies.json')
 check(all(not x.get('name','').endswith(('_animal','_animal_gene')) for p in chest['pools'] for x in p['entries']),'Second chest DNA category survived')
 rescued=read('chao_garden/loot_table/bonus/rescued_animals.json')['pools'][0]['entries']
 check(len(rescued)==31 and len({x['name'] for x in rescued})==31,'Rescued catalog is not complete')
 for x in rescued:
  animal=x['name'].removeprefix('chao_garden:').removesuffix('_animal_gene')
  check(x['weight']==(2 if animal in {'dragon','unicorn','phoenix'} else 3),'Incorrect modest mythical weight')
 check(sum(x['weight'] for x in rescued)==90,'Weight tickets not conserved')
 check(read('chao_garden/tags/entity_type/chaos_drive_enemies.json')=={'replace':False,'values':[]},'Override hostile tag must remain extendable')
 ledger=read('neoforge/loot_modifiers/global_loot_modifiers.json')
 check(ledger['entries'].count('chao_garden:generic_chest_training')==1 and ledger['replace'] is False,'Chest policy ledger replaced or duplicated')
 print(f'Training supplies024: {checks} resource invariants passed')
if __name__=='__main__':main()
