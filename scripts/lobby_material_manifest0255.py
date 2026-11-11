"""Finite lobby material names, independent of unrelated realm sky PNGs."""
from pathlib import Path
import re


def render_textures(root, block_atlas_materials):
    renderer=(Path(root)/'src/main/java/com/chaogarden/client/LobbySceneRenderer025.java').read_text()
    names={'scene/'+name for name in re.findall(r'texture\("([a-z0-9_]+)"\)',renderer)}
    assert names, 'Lobby renderer has no explicit texture bindings'
    assert all(re.fullmatch(r'scene/[a-z0-9_]+',name) for name in block_atlas_materials)
    return sorted(names | set(block_atlas_materials))
