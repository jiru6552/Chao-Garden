#!/usr/bin/env python3
"""Remove duplicate alignment-fruit resources after every legacy asset writer."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
REPLACEMENTS={'hero_fruit':'hero_garden_fruit','dark_fruit':'dark_garden_fruit'}
def canonical(value):
    for old,new in REPLACEMENTS.items():
        value=re.sub(r'(?<![a-z_])'+old+r'(?=_|$)',new,value)
    return value
def remap(value):
    if isinstance(value,str):return canonical(value)
    if isinstance(value,list):
        values=[remap(x) for x in value]
        # Registry tags/catalogs are sets; keep structured arrays untouched.
        return list(dict.fromkeys(values)) if values!=value and all(isinstance(x,str) for x in values) else values
    if isinstance(value,dict):
        return {k:remap(v) for k,v in value.items() if not any('.'+old in k for old in REPLACEMENTS)}
    return value
def main():
    resources=ROOT/'src/main/resources';removed=[];updated=0
    for p in sorted(resources.rglob('*')):
        if not p.is_file():continue
        if any(p.stem==old or p.stem.startswith(old+'_') for old in REPLACEMENTS):
            removed.append(str(p.relative_to(resources)));p.unlink();continue
        if p.suffix=='.json':
            original=json.loads(p.read_text());changed=remap(original)
            if changed!=original:
                p.write_text(json.dumps(changed,indent=2,ensure_ascii=False)+'\n');updated+=1
    # Do not advertise stale items in the source-side generated catalogs either.
    for name in ('docs/fruit_items_015.json','docs/fruit_lang_015.json','scripts/resource_items.json'):
        p=ROOT/name;p.write_text(json.dumps(remap(json.loads(p.read_text())),indent=2,ensure_ascii=False)+'\n')
    print(f'Fruit02510: duplicate resources removed={len(removed)}, updated catalogs/tags={updated}; registry aliases retain old saves.')
if __name__=='__main__':main()
