#!/usr/bin/env python3
"""Independently inspect the final generated acquisition graph."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data'
checks=0
def require(value,message):
    global checks
    checks+=1
    if not value:raise ValueError(message)
def validate():
    for folder in ('recipe','recipes'):
        for path in (DATA/'chao_garden'/folder).rglob('*.json'):
            value=json.loads(path.read_text());result=value.get('result',{})
            if isinstance(result,dict):result=result.get('id',result.get('item',''))
            require(not(isinstance(result,str) and result.startswith('chao_garden:') and result.endswith('_egg')),
                    'Forbidden egg recipe '+str(path))
    legacy=('desert_eggs','jungle_eggs','ocean_eggs','ancient_eggs','end_eggs','bastion_eggs')
    ledger=json.loads((DATA/'neoforge/loot_modifiers/global_loot_modifiers.json').read_text())
    require(ledger['replace'] is False,'Other mods modifiers must be retained')
    require(ledger['entries'].count('chao_garden:chest_eggs')==1,'Egg policy must run once')
    require(ledger['entries'][-1]=='chao_garden:chest_eggs','Final egg policy ordering is not deterministic')
    for name in legacy:
        require('chao_garden:'+name not in ledger['entries'],'Legacy modifier active '+name)
        for rel in ('loot_modifiers/'+name+'.json','loot_table/bonus/'+name+'.json'):
            require(not(DATA/'chao_garden'/rel).exists(),'Legacy egg pool accessible '+name)
    def egg_route(node):
        if isinstance(node,dict):
            if any(key in ('name','id','item') and isinstance(val,str) and val.startswith('chao_garden:') and val.endswith('_egg') for key,val in node.items()):return True
            return any(egg_route(val) for val in node.values())
        return isinstance(node,list) and any(egg_route(val) for val in node)
    for path in (DATA/'chao_garden/loot_table').rglob('*.json'):
        require(not egg_route(json.loads(path.read_text())),'Direct old egg loot route '+str(path))
    require(json.loads((DATA/'chao_garden/loot_modifiers/chest_eggs.json').read_text())=={
        'type':'chao_garden:chest_eggs','conditions':[],'base_chance':.0025,'shiny_chance':.001,'gem_chance':.0002,'dimension_chance':.01},'Probabilities changed')
    require(not list((DATA/'chao_garden/advancement/recipes/chao_variants').glob('*.json')),'Disabled recipe unlock present')
    for v in json.loads((ROOT/'src/main/resources/assets/chao_garden/chao_variants.json').read_text())['variants']:
        require((ROOT/'src/main/resources/assets/chao_garden/models/item'/f'{v["id"]}_chao_egg.json').is_file(),'Paused artwork removed')
    language=ROOT/'src/main/resources/assets/chao_garden/lang/en_us.json'
    parsed=json.loads(language.read_text())
    require(language.read_text()==json.dumps(parsed,indent=2,ensure_ascii=False,sort_keys=True)+'\n','Locale serialization not canonical')
    print(f'Distribution023: {checks} resource acquisition checks passed')
if __name__=='__main__':validate()
