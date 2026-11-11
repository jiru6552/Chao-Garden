#!/usr/bin/env python3
"""Apply the final acquisition boundary after historical asset writers."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data'
LEGACY=('desert_eggs','jungle_eggs','ocean_eggs','ancient_eggs','end_eggs','bastion_eggs')

def write(path,value,sort=False):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False,sort_keys=sort)+'\n')

def main():
    variants=json.loads((ROOT/'src/main/resources/assets/chao_garden/chao_variants.json').read_text())['variants']
    removed=0
    candidates=[DATA/'chao_garden/recipe/normal_egg.json']
    for v in variants:
        candidates.extend((DATA/'chao_garden/recipe'/f'{v["id"]}_chao_egg.json',
                           DATA/'chao_garden/advancement/recipes/chao_variants'/f'{v["id"]}_chao_egg.json'))
    for name in LEGACY:
        candidates.extend((DATA/'chao_garden/loot_modifiers'/f'{name}.json',DATA/'chao_garden/loot_table/bonus'/f'{name}.json'))
    for path in candidates:
        if path.exists():path.unlink();removed+=1
    ledger=DATA/'neoforge/loot_modifiers/global_loot_modifiers.json'
    value=json.loads(ledger.read_text())
    value['entries']=[entry for entry in value['entries'] if entry not in {'chao_garden:'+n for n in LEGACY}
                      and entry!='chao_garden:chest_eggs']
    # Stable ordering is part of deterministic loot generation; this final
    # writer always runs after the training-supply writer.
    value['entries'].append('chao_garden:chest_eggs')
    write(ledger,value)
    write(DATA/'chao_garden/loot_modifiers/chest_eggs.json',dict(type='chao_garden:chest_eggs',conditions=[],
        base_chance=.0025,shiny_chance=.001,gem_chance=.0002,dimension_chance=.01))
    language=ROOT/'src/main/resources/assets/chao_garden/lang/en_us.json'
    value=json.loads(language.read_text());value['key.chao_garden.companion_ender_chest']='Open End Chao Ender Chest'
    write(language,value,sort=True)
    print(f'Distribution023 finalized: {removed} obsolete acquisition files removed; all19 theme assets retained.')

if __name__=='__main__':main()
