"""Bind the six Dark Garden items to actual native registrations and their catalog."""
from pathlib import Path
import json
import re

DOMAIN={'dark_garden_soil','dark_garden_rock','dark_garden_deadwood',
        'dark_garden_gravestone','dark_garden_fence','dark_garden_effigy'}


def dark_items(root):
    root=Path(root)
    source=(root/'src/main/java/com/chaogarden/hub/DarkGardenDecor0255.java').read_text()
    declaration=re.search(r'public static final List<String> IDS=List\.of\((.*?)\);',source,re.S)
    assert declaration, 'Dark Garden lost its explicit finite registry domain'
    ids=re.findall(r'"(dark_garden_[a-z_]+)"',declaration[1])
    assert len(ids)==len(set(ids))==6 and set(ids)==DOMAIN, 'Dark Garden registry IDs changed'
    assert 'for(String id:IDS)' in source and 'BLOCKS.register(id,' in source, 'Dark native block registrations detached from finite domain'
    assert 'ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()))' in source, 'Dark native BlockItem registration differs'
    manifest=json.loads((root/'src/main/resources/assets/chao_garden/dark_garden_decor0255.json').read_text())
    assert manifest['version']=='0.25.5' and manifest['texturePrefix']=='block/dark0255/', 'Dark Garden catalog material prefix/version differs'
    assert len(manifest['blocks'])==6 and {row['id'] for row in manifest['blocks']}==DOMAIN, 'Dark Garden catalog differs from actual registry'
    assert all(row=={'id':row['id'],'item':True} for row in manifest['blocks']), 'Dark catalog introduced another item/internal kind'
    return set(ids)
