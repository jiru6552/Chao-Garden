#!/usr/bin/env python3
"""Retire authored character-only assets; ordinary Chao egg resources stay intact."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden'

def main():
    removed=[]
    for character in ('spartoi','egg'):
        for path in (A/'models/entity/gameplay'/f'chao_{character}.json',
                     A/'textures/entity/chao/gameplay'/f'character_{character}.png'):
            if path.exists():path.unlink();removed.append(path.relative_to(ROOT).as_posix())
    path=A/'textures/item/spartoi_lens.png'
    if path.exists():path.unlink();removed.append(path.relative_to(ROOT).as_posix())
    # A retired inventory ID remains loadable with an ordinary clear-lens icon.
    (A/'models/item/spartoi_lens.json').write_text(json.dumps({'parent':'chao_garden:item/clear_lens'},indent=2)+'\n')
    path=A/'lang/en_us.json';lang=json.loads(path.read_text())
    lang['item.chao_garden.spartoi_lens']='Retired Lens'
    path.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')
    print(f'Retired authored Spartoi/Egg character assets: {len(removed)} files; ordinary eggs untouched.')
    return removed

if __name__=='__main__':main()
