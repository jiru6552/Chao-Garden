"""Finite production layout domains, separate from exact original references."""
from pathlib import Path

ORDINARY_FORMS = ('baby','swim','fly','run','power','hero','hero_swim','hero_fly','hero_run','hero_power','dark','dark_swim','dark_fly','dark_run','dark_power')
SPECIAL_FORMS = ('amy','omochao','tails','knuckles','chaos_light','chaos_angel','chaos_devil')
GAMEPLAY_FORMS = ORDINARY_FORMS + SPECIAL_FORMS
COAT_LAYOUTS = ('baby','swim','fly','run','power','hero','hero_fly','hero_power','dark','dark_run','dark_swim')
ATTACHMENT_LAYOUTS = COAT_LAYOUTS + ('tails','knuckles')
REFERENCE_ONLY_LAYOUTS = ('baghead','creeper','dad','dragon','emerald','ender','golden')
UNUSED_COAT_LAYOUTS = REFERENCE_ONLY_LAYOUTS + ('tails','knuckles')
REFERENCE_MODELS = COAT_LAYOUTS + REFERENCE_ONLY_LAYOUTS + ('tails','knuckles')
# Source-mesh GUI icons for six base toys became reachable again in 0.25.15.
UNUSED_ITEM_SPRITES = ('chao_carrier','chao_ball','chao_jack_in_box')

def prototype_path(root):
    root=Path(root)
    live=root/'src/main/resources/assets/chao_garden/models/entity/chao_mesh.json'
    return live if live.is_file() else root/'art/legacy_runtime/chao_mesh.json'

def pruning_paths(root):
    assets=Path(root)/'src/main/resources/assets/chao_garden'
    names=sorted(p.name for p in (assets/'textures/entity/chao/official/coats/swim').glob('*.png'))
    assert len(names)==172, 'Native coat catalog changed; review the whitelist'
    paths={f'textures/entity/chao/official/coats/{layout}/{name}' for layout in UNUSED_COAT_LAYOUTS for name in names}
    for key in REFERENCE_ONLY_LAYOUTS:
        paths.update(f'models/entity/{folder}/chao_{key}.json' for folder in ('gameplay','overlays','hats','cwe_parts','party_hats'))
        paths.update(f'textures/entity/chao/gameplay/masks/{key}{suffix}.png' for suffix in ('','_accent','_wings'))
    paths.update(f'textures/item/{name}.png' for name in UNUSED_ITEM_SPRITES)
    paths.update(('models/entity/chao_mesh.json','textures/entity/chao/official/coats/baby/cwe_parts.png'))
    assert len(paths)==1609
    return paths
