"""Generate retained themed artwork without re-enabling paused acquisition."""
from pathlib import Path
import json
from variant_catalog import VARIANTS, RETIRED_VARIANTS
from item_sprite_art import themed_egg, save_sprite
from prune_retired_resources import prune_retired_resources

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/chao_garden'
DATA=RES/'data/chao_garden'

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n')

def retire_resources(language):
    # Retired registry shells exist only to load old inventories as normal eggs.
    # Their active artwork, recipes, advancements and world atlases are removed.
    prune_retired_resources(RES)
    for ident in RETIRED_VARIANTS:
        language['item.chao_garden.'+ident+'_chao_egg']='Normal Chao Egg'
    write(ASSETS/'retired_chao_variants.json',list(RETIRED_VARIANTS))

def main():
    language=json.loads((ASSETS/'lang/en_us.json').read_text())
    retire_resources(language)
    identities={}
    for entry in VARIANTS:
        id=entry['id'];item=id+'_chao_egg';ingredients=['chao_garden:normal_egg']+entry['ingredients']
        identity=tuple(sorted(ingredients))
        if identity in identities:raise ValueError('Ambiguous recipes: '+id+' / '+identities[identity])
        identities[identity]=id
        # Current artwork remains registered, but no themed conversion is active.
        (DATA/'recipe'/f'{item}.json').unlink(missing_ok=True)
        (DATA/'advancement/recipes/chao_variants'/f'{item}.json').unlink(missing_ok=True)
        write(ASSETS/'models/item'/f'{item}.json',{'parent':'minecraft:item/generated',
            'textures':{'layer0':'chao_garden:item/'+item}})
        save_sprite(themed_egg(entry), ASSETS/'textures/item'/f'{item}.png')
        language['item.chao_garden.'+item]=entry['name']+' Chao Egg'
    write(ASSETS/'lang/en_us.json',language)
    ledger=ROOT/'scripts/resource_items.json'
    if ledger.exists():
        items=[item for item in json.loads(ledger.read_text())
               if item not in {ident+'_chao_egg' for ident in RETIRED_VARIANTS}]
        for entry in VARIANTS:
            item=entry['id']+'_chao_egg'
            if item not in items:items.append(item)
        ledger.write_text(json.dumps(items,indent=2)+'\n')
    print(f'Generated {len(VARIANTS)} retained theme models and original pixel icons; themed recipes remain disabled')

if __name__=='__main__':main()
