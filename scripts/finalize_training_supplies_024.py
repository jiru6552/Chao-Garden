#!/usr/bin/env python3
"""Canonical final training-supply loot after historical asset writers."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
D=ROOT/'src/main/resources/data'
ANIMALS=('penguin','seal','otter','peacock','parrot','condor','rabbit','cheetah','warthog',
 'bear','tiger','gorilla','skunk','sheep','raccoon','dragon','unicorn','phoenix','half_fish',
 'skeleton_dog','bat','bee','ladybug','scorpion','deer','elephant','kangaroo','koala','lion','mole','swallow')
MYTHICAL={'dragon','unicorn','phoenix'}

def write(p,v):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n')

def main():
 # Keep optional fruit rewards, but guaranteed drives have one actual-death hook.
 p=D/'chao_garden/loot_table/bonus/enemy_supplies.json';v=json.loads(p.read_text())
 for pool in v.get('pools',[]):
  pool['entries']=[x for x in pool['entries'] if not x.get('name','').endswith('_drive') and x.get('type')!='minecraft:empty']
  # Replace the old four drive entries'32tickets with an empty choice; the
  # remaining fruit probabilities are preserved rather than silently increased.
  pool['entries'].append({'type':'minecraft:empty','weight':32})
 write(p,v)
 # A single namespace-independent chest gene roll replaces the old shortlist roll.
 p=D/'chao_garden/loot_table/bonus/chest_supplies.json';v=json.loads(p.read_text())
 v['pools']=[pool for pool in v.get('pools',[]) if not any(x.get('name','').endswith(('_animal','_animal_gene')) for x in pool.get('entries',[]))]
 write(p,v)
 # Existing hostile rescued-animal drops follow the same modest rarity ratio.
 p=D/'chao_garden/loot_table/bonus/rescued_animals.json';v=json.loads(p.read_text())
 v['pools'][0]['entries']=[{'type':'minecraft:item','name':'chao_garden:'+n+'_animal_gene','weight':2 if n in MYTHICAL else 3} for n in ANIMALS]
 write(p,v)
 write(D/'chao_garden/tags/entity_type/chaos_drive_enemies.json',{'replace':False,'values':[]})
 print('Training supplies024: one enemy drive hook; one chest DNA pool;31 samples at ordinary3/mythical2 weights.')
if __name__=='__main__':main()
