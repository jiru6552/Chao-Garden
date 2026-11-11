#!/usr/bin/env python3
"""Merge independent scene writers after all legacy language finalizers."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
target = ROOT / 'src/main/resources/assets/chao_garden/lang/en_us.json'
locale = json.loads(target.read_text())
incoming = {'itemGroup.chao_garden.scenery': 'Chao Scenery'}
for filename in ('locale-kindergarten025.json', 'lobby_garden_lang_fragment_025.json', 'dark_garden_lang_fragment_0255.json', 'kindergarten_pupils_lang_0257.json'):
    fragment = json.loads((ROOT / 'art' / filename).read_text())
    assert not (set(incoming) & set(fragment)), 'Duplicate scene locale keys'
    incoming.update(fragment)
locale.update(incoming)
target.write_text(json.dumps(locale, indent=2, ensure_ascii=False, sort_keys=True) + '\n')
print(f'Scene locale025: {len(incoming)} independent labels merged.')
