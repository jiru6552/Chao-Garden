#!/usr/bin/env python3
"""Unrelated sky files must not enter the lobby's renderer material contract."""
from pathlib import Path
import json
import shutil
import tempfile
from lobby_material_manifest0255 import render_textures

ROOT=Path(__file__).resolve().parents[1]
manifest=json.loads((ROOT/'art/lobby_garden_manifest_025.json').read_text())
expected=render_textures(ROOT,manifest['block_atlas_textures'])
assert expected==manifest['render_textures']
assert 'scene/lobby_glass' in expected and 'scene/garden_turf_edge' in expected
with tempfile.TemporaryDirectory(prefix='chao0255-material-scope-') as folder:
    copy=Path(folder)
    renderer=Path('src/main/java/com/chaogarden/client/LobbySceneRenderer025.java')
    (copy/renderer).parent.mkdir(parents=True)
    shutil.copy2(ROOT/renderer,copy/renderer)
    textures=copy/'src/main/resources/assets/chao_garden/textures/scene'
    textures.mkdir(parents=True)
    for name in ('dark_twilight_sky','unrelated_future_realm_sky'):
        (textures/(name+'.png')).write_bytes(b'unrelated realm texture discovery sentinel')
    assert render_textures(copy,manifest['block_atlas_textures'])==expected
    expanded=render_textures(copy,[*manifest['block_atlas_textures'],'scene/new_native_lobby_material'])
    assert set(expanded)==set(expected)|{'scene/new_native_lobby_material'}
assert 'scene/dark_twilight_sky' not in expected
print('Lobby material manifest0255: native declarations retained; unrelated realm/future sky PNGs excluded.')
