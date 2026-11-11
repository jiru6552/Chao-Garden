#!/usr/bin/env python3
"""Audit actual item bindings, canonical icons, placed shells and cocoons.

No resource is rewritten. Deliberately corrupted in-memory controls prove that
the checks reject icon loss, dangling bindings, shell seams and painted shine.
"""
from pathlib import Path
from collections import Counter
import argparse
import copy
import hashlib
import json
import math

from PIL import Image
from variant_icons import theme_icon

ROOT = Path(__file__).resolve().parents[1]
checks = 0


def require(value, message):
    global checks
    checks += 1
    if not value:
        raise AssertionError(message)


def rgba(path):
    with Image.open(path) as image:
        image.load()
        return image.convert('RGBA')


def icon_pixels(entry):
    image = theme_icon(entry, 8)
    x, y = 4, 5
    if entry['id'] == 'ars_nouveau':
        left, top, right, bottom = image.getbbox()
        x = (16-(right-left))//2-left
        y = (16-(bottom-top))//2-top
    return {(x+u, y+v): image.getpixel((u, v))
            for v in range(8) for u in range(8) if image.getpixel((u, v))[3]}


def validate_icon(entry, image):
    pixels = icon_pixels(entry)
    for point, color in pixels.items():
        require(image.getpixel(point) == color, entry['id'] + ': canonical glyph pixel repainted at ' + str(point))
    return len(pixels)


def validate_sprite(image, label, size=None):
    require(image.size in ((16, 16), (32, 32)) if size is None else image.size == size,
            label + ': unexpected native sprite size')
    require(image.getbbox() is not None, label + ': blank sprite')
    require(set(image.getchannel('A').tobytes()) <= {0, 255}, label + ': fractional-alpha fringe')


def texture_path(assets, identifier):
    require(isinstance(identifier, str) and not identifier.startswith('#'), 'Unresolved texture alias ' + str(identifier))
    namespace, _, path = identifier.partition(':')
    if not path:
        namespace, path = 'minecraft', namespace
    require('..' not in path.split('/') and not path.startswith('/'), 'Unsafe texture path')
    if namespace != 'chao_garden':
        return None
    result = assets / 'textures' / (path + '.png')
    require(result.is_file(), 'Dangling texture binding ' + identifier)
    return result


def item_bindings(assets, identifier, visiting=()):
    require(identifier not in visiting, 'Cyclic model parent ' + identifier)
    namespace, _, relative = identifier.partition(':')
    if not relative:
        namespace, relative = 'minecraft', namespace
    if namespace != 'chao_garden':
        return {}
    path = assets / 'models' / (relative + '.json')
    require(path.is_file(), 'Dangling own model parent ' + identifier)
    data = json.loads(path.read_text())
    inherited = item_bindings(assets, data['parent'], visiting+(identifier,)) if data.get('parent') else {}
    return {**inherited, **data.get('textures', {})}


def resolve_alias(bindings, value, seen=()):
    require(isinstance(value, str), 'Non-string model texture')
    if not value.startswith('#'):
        return value
    name = value[1:]
    require(name not in seen and name in bindings, 'Dangling or cyclic texture alias ' + value)
    return resolve_alias(bindings, bindings[name], seen+(name,))


def validate_shell_mesh(data):
    require((data['texture_width'], data['texture_height']) == (64, 64), 'Unexpected placed egg atlas units')
    samples = panels = seams = 0
    for bone in data['bones']:
        for mesh in bone['meshes']:
            edges = Counter()
            vertices = mesh['vertices']
            for face in mesh['faces']:
                require(len(face['v']) == len(face['uv']), 'Egg UV topology mismatch')
                for index, (vertex, uv) in enumerate(zip(face['v'], face['uv'])):
                    require(0 <= vertex < len(vertices), 'Egg index outside shell')
                    require(all(math.isfinite(v) and .5-1e-4 <= v <= 63.5+1e-4 for v in uv), 'Egg UV outside centered atlas')
                    edges[tuple(sorted((vertex, face['v'][(index+1) % len(face['v'])])))] += 1
                    samples += 1
                points = [vertices[i] for i in face['v']]
                if max(v[1] for v in points)-min(v[1] for v in points) > 1e-6:
                    panels += 1
                    require(max(uv[0] for uv in face['uv'])-min(uv[0] for uv in face['uv']) <= 8, 'Egg stretches across longitude seam')
                    if any(abs(uv[0]-63.5) < 1e-4 for uv in face['uv']):
                        seams += 1
            require(all(v == 2 for v in edges.values()), 'Egg open/doubled shell edge')
    require(panels == 96 and seams == 6, 'Wrong closed egg longitude domain')
    return samples


def validate_shell_texture(image, label):
    require(image.size == (64, 64), label + ': wrong placed atlas size')
    require(set(image.getchannel('A').tobytes()) == {255}, label + ': shell alpha hole')
    for y in range(64):
        require(image.getpixel((0, y)) == image.getpixel((63, y)), label + ': longitude edge mismatch')


def validate_shiny(base, shiny, label):
    require(base.tobytes() == shiny.tobytes(), label + ': painted shiny albedo stripe; use the actual reflection layer')


COCOON_ENDS = {
    'evolution': ((255, 244, 152, 255), (84, 153, 211, 255)),
    'rebirth': ((255, 203, 151, 255), (224, 105, 159, 255)),
    'death': ((237, 235, 219, 255), (149, 155, 154, 255)),
}


def validate_cocoon(image, kind, mesh):
    require(image.size == (256, 256), kind + ': wrong native cocoon atlas units')
    require(image.getpixel((64, 32)) == COCOON_ENDS[kind][0]
            and image.getpixel((64, 63)) == COCOON_ENDS[kind][1], kind + ': wrong lifecycle palette')
    colors = []
    for y in range(256):
        if 32 <= y < 64:
            row = [image.getpixel((x, y)) for x in range(64, 96)]
            require(len(set(row)) == 1 and row[0][3] == 255, kind + ': cocoon longitude/alpha discontinuity')
            require(all(image.getpixel((x, y)) == (0, 0, 0, 0) for x in (*range(64), *range(96, 256))), kind + ': stray outside material tile')
            colors.append(row[0])
        else:
            require(all(image.getpixel((x, y)) == (0, 0, 0, 0) for x in range(256)), kind + ': stray outside material tile')
    require(len(set(colors)) == 16 and all(colors[y] == colors[y+1] for y in range(0, 32, 2)), kind + ': latitude bands are not sixteen complete pairs')
    samples = 0
    for face in mesh['faces']:
        uvs = face['uv']
        for u, v in [*uvs, [sum(p[0] for p in uvs)/len(uvs), sum(p[1] for p in uvs)/len(uvs)]]:
            require(64.5-1e-4 <= u <= 95.5+1e-4 and 32.5-1e-4 <= v <= 63.5+1e-4, kind + ': source cocoon UV left its original tile')
            require(image.getpixel((int(u), int(v)))[3] == 255, kind + ': visible cocoon alpha hole')
            samples += 1
    return samples


def main(root=ROOT, self_test=False):
    global checks
    checks = 0
    root = Path(root)
    assets = root / 'src/main/resources/assets/chao_garden'
    sprites = sorted((assets / 'textures/item').glob('*.png'))
    retired_alignment_items = {'hero_fruit', 'hero_fruit_seed', 'dark_fruit', 'dark_fruit_seed'}
    require(len(sprites) == 269, 'Expected 248 retained sprites, nineteen source-toy GUI icons and two tablet materials')
    require(not ({p.stem for p in sprites} & retired_alignment_items), 'Retired alignment-fruit sprite returned')
    from fruit_forms02512 import FOODS,SEEDS
    fruit_items=set(FOODS)|{f+'_seed' for f in SEEDS}|{'chao_watering_can','chao_shovel'}
    source_items = set(json.loads((root/'art/sa2_02515/toys.json').read_text())) | set(json.loads((root/'art/sa2_02515/hats.json').read_text()))
    require(len(source_items) == 29 and source_items <= {p.stem for p in sprites}, 'Missing source toy/hat GUI icon')
    source_items |= set(json.loads((root/'art/cwe02516/generated.json').read_text())['items'])
    require(len(source_items) == 55, 'Expected 29 original-game and 26 CWE source item icons')
    for path in sprites:
        validate_sprite(rgba(path), path.name, (32, 32) if '_animal' in path.stem or path.stem in fruit_items | source_items | {"chao_analyzer", "chao_analyzer_back"} else (16, 16))
    entries = json.loads((assets / 'chao_variants.json').read_text())['variants']
    require(len(entries) == 19, 'Expected nineteen retained themes')
    glyphs = 0
    for entry in entries:
        if entry['id'] not in ('nether', 'end'):
            glyphs += validate_icon(entry, rgba(assets/'textures/item'/f'{entry["id"]}_chao_egg.png'))
    require(glyphs == 683, 'Canonical seventeen-icon ink domain changed')
    item_models = sorted((assets / 'models/item').glob('*.json'))
    # Preserve the complete reviewed base and bind new scenery to its actual
    # finite registration, rather than accepting arbitrary extra model files.
    if (root/'art/controls025/base-item-models.json').is_file():
        from scene_resource_contract_025 import scenery_items, hallway_items, hero_items
        from dark_resource_contract_0255 import dark_items
        base = json.loads((root/'art/controls025/base-item-models.json').read_text())['item_models']
        require(len(base) == len(set(base)) == 315, 'Base item control changed')
        expected = (set(base) - retired_alignment_items) | scenery_items(root) | hallway_items(root) | hero_items(root) | dark_items(root) | {"chao_watering_can"}
        require({p.stem for p in item_models} == expected, 'Registered/compatibility scenery item model domain differs')
    else:
        require(len(item_models) == 315, 'Unexpected registered/compatibility item model domain')
    tube = json.loads((assets / 'models/item/chao_teleporter.json').read_text())
    require(tube.get('parent') == 'minecraft:block/block'
            and tube.get('render_type') == 'minecraft:translucent',
            'Teleporter item lost its native translucent block model')
    require(min(e['from'][1] for e in tube['elements']) == 0
            and max(e['to'][1] for e in tube['elements']) == 32,
            'Teleporter item must show both complete block halves')
    links, block_textures = 0, set()
    for path in item_models:
        bindings = item_bindings(assets, 'chao_garden:item/' + path.stem)
        for value in bindings.values():
            target = texture_path(assets, resolve_alias(bindings, value))
            if target:
                image = rgba(target)
                require(image.getbbox() is not None, 'Blank bound item/block texture ' + str(target))
                links += 1
                if '/textures/block/' in target.as_posix():
                    block_textures.add(str(target.relative_to(assets)))
    shell = json.loads((assets / 'models/entity/egg_mesh.json').read_text())
    shell_samples = validate_shell_mesh(shell)
    placed = sorted((assets / 'textures/entity/eggs').glob('*.png'))
    require(len(placed) == 191, 'Unexpected placed egg atlas domain')
    pairs = 0
    for path in placed:
        image = rgba(path)
        validate_shell_texture(image, path.name)
        if path.stem.endswith('_shiny'):
            base = path.with_name(path.stem.removesuffix('_shiny') + '.png')
            require(base.is_file(), 'Missing non-shiny placed control ' + base.name)
            validate_shiny(rgba(base), image, path.name)
            pairs += 1
    require(pairs == 86, 'Unexpected shiny/base albedo pair domain')
    prototype = assets / 'models/entity/chao_mesh.json'
    if not prototype.exists():
        prototype = root / 'art/legacy_runtime/chao_mesh.json'
    data = json.loads(prototype.read_text())
    cocoon_mesh = next(b for b in data['bones'] if b['name'] == 'cocoon')['meshes'][0]
    require(len(cocoon_mesh['vertices']) == 208 and len(cocoon_mesh['faces']) == 220, 'Source cocoon geometry changed')
    cocoon_samples, cocoon_hashes = 0, {}
    for kind in COCOON_ENDS:
        path = assets / 'textures/entity/chao' / ('cocoon_' + kind + '.png')
        cocoon_samples += validate_cocoon(rgba(path), kind, cocoon_mesh)
        cocoon_hashes[kind] = hashlib.sha256(path.read_bytes()).hexdigest()
    rejected = []
    if self_test:
        def rejects(name, call):
            try:
                call()
            except (AssertionError, KeyError):
                rejected.append(name)
            else:
                raise AssertionError('Corruption accepted: ' + name)
        entry = next(e for e in entries if e['id'] == 'aether')
        broken = rgba(assets/'textures/item/aether_chao_egg.png');broken.putpixel((5, 5), (1, 2, 3, 255))
        rejects('canonical icon glare overwrite', lambda: validate_icon(entry, broken))
        rejects('dangling item texture', lambda: texture_path(assets, 'chao_garden:item/__missing022'))
        broken = rgba(sprites[0]);broken.putpixel((7, 7), (1, 2, 3, 127))
        rejects('fractional sprite fringe', lambda: validate_sprite(broken, 'broken'))
        broken = copy.deepcopy(shell);broken['bones'][0]['meshes'][0]['faces'][0]['uv'][0][0] = 64
        rejects('closing shell UV stretch', lambda: validate_shell_mesh(broken))
        broken = rgba(placed[0]);first = broken.getpixel((0, 0));broken.putpixel((63, 0), ((first[0]+1)%256, *first[1:]))
        rejects('placed shell longitude seam', lambda: validate_shell_texture(broken, 'broken'))
        base = rgba(placed[0]);broken = base.copy();first = broken.getpixel((7, 7));broken.putpixel((7, 7), ((first[0]+1)%256, *first[1:]))
        rejects('painted shiny albedo stripe', lambda: validate_shiny(base, broken, 'broken'))
        good = rgba(assets/'textures/entity/chao/cocoon_death.png');broken = good.copy();broken.putpixel((70, 44), (1, 2, 3, 0))
        rejects('visible cocoon alpha hole', lambda: validate_cocoon(broken, 'death', cocoon_mesh))
        broken = good.copy();broken.putpixel((70, 44), (1, 2, 3, 255))
        rejects('cocoon longitude discontinuity', lambda: validate_cocoon(broken, 'death', cocoon_mesh))
        wrong = rgba(assets/'textures/entity/chao/cocoon_evolution.png')
        rejects('colored coat used for death cocoon', lambda: validate_cocoon(wrong, 'death', cocoon_mesh))
    report = {
        'release': '0.22.0', 'result': 'PASS', 'checks': checks,
        'item_sprites': len(sprites), 'egg_icons': sum(p.stem.endswith('_egg') for p in sprites),
        'dna_sprites': sum('_animal' in p.stem for p in sprites), 'drives': sum(p.stem.endswith('_drive') for p in sprites),
        'item_models': len(item_models), 'resolved_texture_links': links,
        'additional_bound_block_textures': len(block_textures), 'canonical_icon_pixels': glyphs,
        'placed_egg_atlases': len(placed), 'identical_shiny_albedo_pairs': pairs,
        'shell_uv_samples': shell_samples, 'cocoon_uv_and_center_samples': cocoon_samples,
        'cocoon_sha256': cocoon_hashes, 'rejected_corruptions': rejected,
        'scope': 'Actual resource/binding/palette/UV checks. Final material animation, shell appearance and inventory presentation require the separate actual-client review.',
    }
    target = root / 'docs/research/ITEMS_EGGS_022_CHECKS.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    main(args.root, args.self_test)
