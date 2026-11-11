#!/usr/bin/env python3
"""Preserve the language catalog and add the companion control labels."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
    path=ROOT/'src/main/resources/assets/chao_garden/lang/en_us.json'
    data=json.loads(path.read_text())
    data.update({'key.chao_garden.companion_boost':'Omochao Flight Boost',
                 'key.chao_garden.companion_ender_chest':'Open End Chao Ender Chest','key.categories.chao_garden':'Chao Garden'})
    path.write_text(json.dumps(data,indent=2,ensure_ascii=False,sort_keys=True)+'\n')
    print('Companion021: rebindable Omochao boost control labels generated.')
if __name__=='__main__':main()
