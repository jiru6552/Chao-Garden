"""Bind generated scenery catalogs to the actual finite registry declarations."""
from pathlib import Path
import json
import re


def scenery_items(root):
    root = Path(root)
    assets = root / 'src/main/resources/assets/chao_garden'
    manifest = json.loads((assets / 'kindergarten_decor025.json').read_text())
    source = (root / 'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text()
    declared = {}
    for kind, arguments in re.findall(r'add\(Kind\.([A-Z_]+),([^;]+)\);', source):
        for identifier in re.findall(r'"(school_[a-z0-9_]+)"', arguments):
            assert identifier not in declared, 'Duplicate scenery registry declaration: ' + identifier
            declared[identifier] = kind
    blocks = manifest['blocks']
    assert manifest['version'] == 25 and declared, 'Missing scenery registry domain'
    assert len(blocks) == len(declared), 'Scenery catalog length differs from registration'
    assert {row['id'] for row in blocks} == set(declared), 'Scenery IDs differ from registration'
    for row in blocks:
        assert re.fullmatch(r'school_[a-z0-9_]+', row['id']), 'Unsafe scenery ID'
        assert row['kind'] == declared[row['id']] and row['item'] is True, 'Scenery block/item kind mismatch'
    return set(declared)


def hallway_items(root):
    root = Path(root)
    source = (root / 'src/main/java/com/chaogarden/kindergarten/KindergartenHallThemes0252.java').read_text()
    declared = [identifier for arguments in re.findall(r'add\(\d+,([^;]+)\);', source)
                for identifier in re.findall(r'"(school_hall_[a-z_]+)"', arguments)]
    assert len(declared) == len(set(declared)) == 30, 'Hall palette registry domain differs'
    manifest = json.loads((root / 'src/main/resources/assets/chao_garden/kindergarten_hall_themes0252.json').read_text())
    assert manifest['version'] == '0.25.2' and manifest['texturePrefix'] == 'block/school0252/', 'Hall palette catalog version differs'
    assert len(manifest['blocks']) == len(declared), 'Hall palette catalog length differs from actual registration'
    assert {row['id'] for row in manifest['blocks']} == set(declared), 'Hall catalog IDs differ from actual registration'
    assert all(row['kind'] == ('WALL_SHELF' if row['id'] == 'school_hall_market_wall_shelf' else 'CUBE') and row['item'] is True for row in manifest['blocks']), 'Hall palette cube/item contract differs'
    return set(declared)


def hero_items(root):
    """The first Hero palette is four registered, obtainable native blocks."""
    root=Path(root)
    source=(root/'src/main/java/com/chaogarden/hub/HeroGardenDecor0253.java').read_text()
    domain={'hero_marble','hero_marble_trim','hero_fountain_emblem','hero_column'}
    arguments=re.findall(r'for\(String id:List\.of\(([^)]+)\)\)',source)
    assert len(arguments)==1, 'Hero palette lost its finite registry declaration'
    declared=re.findall(r'"(hero_[a-z_]+)"',arguments[0])
    assert len(declared)==len(set(declared))==4 and set(declared)==domain, 'Hero palette registry domain differs'
    manifest=json.loads((root/'src/main/resources/assets/chao_garden/hero_garden_decor0253.json').read_text())
    assert manifest['version']=='0.25.9' and manifest['texturePrefix']=='block/hero0253/', 'Hero palette catalog version/material prefix differs'
    assert len(manifest['blocks'])==4 and {row['id'] for row in manifest['blocks']}==domain, 'Hero palette catalog differs from registration'
    assert all(row=={'id':row['id'],'item':True} for row in manifest['blocks']), 'Hero palette introduced an unreviewed catalog/item kind'
    return domain


def lobby_blocks(root):
    root = Path(root)
    source = (root / 'src/main/java/com/chaogarden/hub/LobbyDecor025.java').read_text()
    declared = set(re.findall(r'BLOCKS\.register\("([a-z0-9_]+)"', source))
    manifest = json.loads((root / 'art/lobby_garden_manifest_025.json').read_text())
    assert declared and len(manifest['blocks']) == len(declared), 'Lobby catalog length differs'
    assert {row['id'] for row in manifest['blocks']} == declared, 'Lobby IDs differ from registration'
    assert all(row['item'] is False for row in manifest['blocks']), 'Internal scene blocks became obtainable items'
    return declared


# These are the only reviewed composite registries. They remain three real items,
# with finite blockstate tiles; tile_N model files are never extra item IDs.
ATLAS_SCENERY_025 = {
    'school_lobby_composite': ('RUG', 9, 13),
    'school_cloud_composite': ('CLOUD', 4, 4),
    'school_mountain_composite': ('MURAL', 8, 5),
}


def scenery_atlases(root):
    root = Path(root)
    manifest = json.loads((root / 'src/main/resources/assets/chao_garden/kindergarten_decor025.json').read_text())
    source = (root / 'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text()
    found = {row['id']: row for row in manifest['blocks'] if 'atlas' in row}
    assert set(found) == set(ATLAS_SCENERY_025), 'Composite scene registry domain changed'
    state = re.search(r'IntegerProperty\s+TILE\s*=\s*IntegerProperty\.create\("tile",\s*(\d+),\s*(\d+)\)', source)
    assert state and tuple(map(int, state.groups())) == (0, 127), 'Composite scene state domain changed'
    result = {}
    for identifier, (kind, columns, rows) in ATLAS_SCENERY_025.items():
        row = found[identifier]
        expected = {'columns': columns, 'rows': rows, 'usedTiles': columns * rows,
                    'stateMin': 0, 'stateMax': 127, 'fallback': 0,
                    'texture': 'chao_garden:block/school025/' + identifier}
        assert row['kind'] == kind and row['atlas'] == expected, 'Composite atlas metadata drift: ' + identifier
        assert 0 < columns * rows <= 128, 'Composite atlas exceeds registered states'
        result[identifier] = expected
    return result
