#!/usr/bin/env python3
"""Merge resource fragments after generators that rebuild shared catalogs."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'


def merge(target, fragments, extra=None):
    data = json.loads(target.read_text()) if target.exists() else {}
    for fragment in fragments:
        data.update(json.loads((ROOT / fragment).read_text()))
    data.update(extra or {})
    target.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')


merge(RES / 'assets/chao_garden/lang/en_us.json',
      ['docs/fruit_lang_015.json', 'art/music_lang_fragment.json'],
      {'block.chao_garden.garden_block': 'Chao Garden Transporter'})
merge(RES / 'assets/chao_garden/sounds.json', ['art/music_sounds_fragment.json'])
ledger = ROOT / 'scripts/resource_items.json'
items = json.loads(ledger.read_text())
items.extend(json.loads((ROOT / 'docs/fruit_items_015.json').read_text()))
items.append('chao_garden_music_disc')
items = list(dict.fromkeys(item for item in items if item != 'chao_carrier'))
ledger.write_text(json.dumps(items, indent=2) + '\n')
print('Merged SA2 fruit, music and transporter resource catalogs.')
