#!/usr/bin/env python3
"""Remove retired theme art/data while retaining ordinary-egg models for old saved IDs."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
RESOURCE_ROOT=ROOT/'src/main/resources'

def prune_retired_resources(resource_root=RESOURCE_ROOT):
    resource_root=Path(resource_root)
    assets=resource_root/'assets/chao_garden'
    catalog=json.loads((assets/'chao_variants.json').read_text())
    active={entry['id'] for entry in catalog['variants']}
    retired=json.loads((assets/'retired_chao_variants.json').read_text())
    if len(active)!=19 or len(retired)!=29 or active.intersection(retired):
        raise ValueError('Expected nineteen retained themes and twenty-nine separate legacy IDs')
    removed=[]
    fallback=json.dumps({'parent':'chao_garden:item/normal_egg'},indent=2)+'\n'
    for ident in retired:
        item=ident+'_chao_egg'
        for relative in (
            f'assets/chao_garden/textures/entity/chao/variant_{ident}.png',
            f'assets/chao_garden/textures/entity/eggs/variant_{ident}.png',
            f'assets/chao_garden/textures/item/{item}.png',
            f'data/chao_garden/recipe/{item}.json',
            f'data/chao_garden/advancement/recipes/chao_variants/{item}.json'):
            path=resource_root/relative
            if path.is_file():
                path.unlink();removed.append(relative)
        model=assets/'models/item'/f'{item}.json'
        if not model.is_file() or model.read_text()!=fallback:
            model.parent.mkdir(parents=True,exist_ok=True);model.write_text(fallback)
    return removed

if __name__=='__main__':
    removed=prune_retired_resources()
    print(f'Pruned {len(removed)} retired art/data files; preserved 19 active themes and 29 ordinary-egg compatibility models.')
