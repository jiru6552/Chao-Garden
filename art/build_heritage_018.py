#!/usr/bin/env python3
"""Finish Moon and genetics-aware Chaos coats on the supplied native UV layouts.

Run after import_official_chao.generate_coats and retarget_chao_overlays.
The original 20 supplied PNGs, geometry and animation inputs are never edited.
Gallery images inform the authored finish; none are copied into these atlases.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import math
import os
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw
from import_official_chao import ART, KEYS, native_skin_masks, recolor_native
from refine_egg_shell import wrap_longitude_edges

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
FORMS = ('light', 'angel', 'devil')
EYES = {'light': (65, 229, 106), 'angel': (77, 179, 237), 'devil': (252, 219, 64)}
COLORS = {
    'normal': (91, 208, 235), 'white': (239, 237, 223), 'red': (219, 73, 69),
    'yellow': (245, 212, 86), 'blue': (67, 114, 216), 'aqua': (89, 205, 215),
    'pink': (236, 156, 195), 'orange': (237, 143, 65), 'purple': (157, 109, 198),
    'brown': (155, 111, 79), 'green': (78, 165, 99), 'grey': (147, 162, 170),
    'lime': (168, 209, 83), 'black': (56, 62, 75), 'sky_blue': (140, 193, 234),
    'cyan': (52, 190, 178),
}
JEWELS = {
    'silver': (185, 210, 225), 'gold': (233, 179, 58), 'ruby': (195, 36, 83),
    'sapphire': (49, 96, 210), 'emerald': (28, 174, 126), 'amethyst': (144, 80, 206),
    'aquamarine': (64, 205, 220), 'garnet': (153, 46, 66), 'onyx': (45, 44, 73),
    'diamond': (180, 228, 240), 'peridot': (166, 197, 61), 'topaz': (230, 162, 52),
    'moon': (74, 69, 143),
}
SKIN_ROLES = ('body', 'head', 'tuft', 'accent', 'crest_tip', 'hand', 'toe',
              'heel', 'collar', 'belly', 'brow')


def rgba(path: Path) -> Image.Image:
    with Image.open(path) as image:
        return image.convert('RGBA')


def save(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    buffer = io.BytesIO()
    image.save(buffer, format='PNG', optimize=True)
    temporary = path.with_name(path.name + '.writing')
    with temporary.open('wb') as handle:
        handle.write(buffer.getvalue())
        handle.flush()
        os.fsync(handle.fileno())
    os.replace(temporary, path)


def hash01(x: int, y: int, channel: int = 0) -> float:
    n = (x * 374761393 + y * 668265263 + channel * 2246822519) & 0xffffffff
    n = ((n ^ (n >> 13)) * 1274126177) & 0xffffffff
    return ((n ^ (n >> 16)) & 0xffff) / 65535.0


def blend(a, b, amount):
    return tuple(round(a[i] * (1 - amount) + b[i] * amount) for i in range(3))


def moon_rgb(x: int, y: int, density: float = 1.0) -> tuple[int, int, int]:
    """Fine jittered facets with soft mottling, without a regular hole grid.

    The visible Moon reference has a deep blue/purple finish and mauve glints.
    Fine cells suggest that finish; the actual moving highlight is a shader.
    """
    cell = max(1.65, density * .92)
    u, v = (x + .5) / cell, (y + .5) / cell
    ix, iy = math.floor(u), math.floor(v)
    best = None
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            cx, cy = ix + dx, iy + dy
            px = cx + .5 + (hash01(cx, cy, 1) - .5) * .65
            py = cy + .5 + (hash01(cx, cy, 2) - .5) * .65
            distance = (u - px) ** 2 + (v - py) ** 2
            if best is None or distance < best[0]:
                best = (distance, cx, cy, u - px, v - py)
    distance, cx, cy, offx, offy = best
    broad = (.5 + .18 * math.sin(x / max(3, density * 2.3))
             + .15 * math.sin(y / max(3, density * 2.8))
             + .12 * math.sin((x + y) / max(5, density * 4.2)))
    base = blend((36, 57, 120), (115, 84, 153), max(0, min(1, broad)))
    facet = (hash01(cx, cy, 3) - .5) * .12
    facet += max(0, .42 - math.sqrt(distance)) * .09
    facet += (offx - offy) * .055
    return tuple(max(0, min(255, round(c * (1 + facet)))) for c in base)


def skin_mask(g: dict, image: Image.Image) -> Image.Image:
    skin, eyes, _ = native_skin_masks(g, image)
    return ImageChops.subtract(skin.convert('L'), eyes.convert('L'))


def chaos_eyes(g: dict, source: Image.Image, form: str) -> Image.Image:
    """Authored family eyes on custom Chaos coats; raw source PNGs stay exact."""
    _, eyes, _ = native_skin_masks(g, source)
    image = source.copy()
    pixels, original, selected = image.load(), source.load(), eyes.load()
    for y in range(source.height):
        for x in range(source.width):
            if selected[x, y] and original[x, y][3]:
                pixels[x, y] = (*EYES[form], original[x, y][3])
    unchanged_outside(source, image, eyes)
    return image


def atlas_skin_mask(image: Image.Image, materials: dict) -> Image.Image:
    mask = Image.new('L', image.size)
    draw = ImageDraw.Draw(mask)
    sx, sy = image.width / 256, image.height / 256
    for role in SKIN_ROLES:
        x, y, width, height = materials[role]
        draw.rectangle((round(x * sx), round(y * sy), round((x + width) * sx) - 1,
                        round((y + height) * sy) - 1), fill=255)
    return mask


def patterned_moon(source: Image.Image, mask: Image.Image, density: float) -> Image.Image:
    image = source.copy()
    pixels, original, active = image.load(), source.load(), mask.load()
    for y in range(image.height):
        for x in range(image.width):
            if active[x, y] and original[x, y][3]:
                pixels[x, y] = (*moon_rgb(x, y, density), original[x, y][3])
    return image


def recolor_atlas(source: Image.Image, mask: Image.Image, target, *,
                  two_tone: bool, shiny: bool, jewel: bool,
                  form: str, materials: dict) -> Image.Image:
    image = source.copy()
    pixels, original, active = image.load(), source.load(), mask.load()
    dark_regions = Image.new('L', source.size)
    if form == 'devil' and two_tone and not jewel:
        pen = ImageDraw.Draw(dark_regions)
        for role in ('body', 'head', 'tuft', 'belly', 'collar', 'brow'):
            x, y, w, h = materials[role]
            pen.rectangle((x, y, x + w - 1, y + h - 1), fill=255)
    for y in range(image.height):
        for x in range(image.width):
            old = original[x, y]
            if not old[3] or not active[x, y]:
                continue
            brightness = .73 + .32 * sum(old[:3]) / (3 * 255)
            shade = .15 if dark_regions.getpixel((x, y)) else 1
            rgb = tuple(max(0, min(255, round(c * brightness * shade))) for c in target)
            # Keep colour families coherent on every limb. Two-tone Devil
            # keeps its dark background, rather than tinting it bright red.
            pixels[x, y] = (*rgb, old[3])
    return image


def effective_coats():
    for name, target in [*COLORS.items(), *(("jewel_" + n, c) for n, c in JEWELS.items())]:
        for two in (False, True):
            for shiny in (False, True):
                key = name + ('_two' if two else '_mono') + ('_shiny' if shiny else '')
                yield key, name, target, two, shiny


def unchanged_outside(source: Image.Image, painted: Image.Image, mask: Image.Image):
    if source.size != painted.size or source.getchannel('A').tobytes() != painted.getchannel('A').tobytes():
        raise ValueError('Coat generator altered source size or alpha')
    background = ImageChops.invert(mask.convert('L'))
    if ImageChops.multiply(ImageChops.difference(source, painted).convert('RGB'),
                           Image.merge('RGB', (background,) * 3)).getbbox():
        raise ValueError('Coat generator changed protected UV pixels')


def build(output: Path = ASSETS) -> dict:
    output = Path(output)
    original_hashes = {key: hashlib.sha256((ART / 'textures' / (key + '.png')).read_bytes()).hexdigest()
                       for key in KEYS}
    meshes = {key: json.loads((ART / 'models' / ('chao_' + key + ('.json' if key == 'dad' else '.geo.json')))
                             .read_text())['minecraft:geometry'][0] for key in KEYS}
    materials = json.loads((ASSETS / 'models/entity/chao_mesh.json').read_text())['materials']
    records = []
    protected_checks = 0
    for key in KEYS:
        source = rgba(ART / 'textures' / (key + '.png'))
        mask = skin_mask(meshes[key], source)
        image = patterned_moon(source, mask, source.width / 64)
        unchanged_outside(source, image, mask)
        protected_checks += 1
        for two in (False, True):
            for shiny in (False, True):
                suffix = ('_two' if two else '_mono') + ('_shiny' if shiny else '')
                path = output / 'textures/entity/chao/official/coats' / key / ('jewel_moon' + suffix + '.png')
                save(image, path)
                records.append(path.relative_to(output).as_posix())

    for two in (False, True):
        for shiny in (False, True):
            suffix = ('_two' if two else '_mono') + ('_shiny' if shiny else '')
            source = rgba(ASSETS / 'textures/entity/chao' / ('jewel_amethyst' + suffix + '.png'))
            mask = atlas_skin_mask(source, materials)
            image = patterned_moon(source, mask, 1)
            unchanged_outside(source, image, mask)
            protected_checks += 1
            path = output / 'textures/entity/chao' / ('jewel_moon' + suffix + '.png')
            save(image, path); records.append(path.relative_to(output).as_posix())
            source = rgba(ASSETS / 'textures/entity/eggs' / ('jewel_amethyst' + suffix + '.png'))
            mask = Image.new('L', source.size, 255)
            image = wrap_longitude_edges(patterned_moon(source, mask, 3.2))
            path = output / 'textures/entity/eggs' / ('jewel_moon' + suffix + '.png')
            save(image, path); records.append(path.relative_to(output).as_posix())
            if any(image.getpixel((0, y)) != image.getpixel((image.width - 1, y)) for y in range(image.height)):
                raise ValueError('Moon egg wrap gutter differs')

    source = rgba(ASSETS / 'textures/item/amethyst_jewel_egg.png')
    image = patterned_moon(source, Image.new('L', source.size, 255), 1)
    path = output / 'textures/item/moon_jewel_egg.png'
    save(image, path); records.append(path.relative_to(output).as_posix())

    coat_specs = tuple(effective_coats())
    if len(coat_specs) != 116:
        raise ValueError('Expected116 effective genetic coats')
    for form in FORMS:
        canonical = 'chaos_' + form
        native_source = chaos_eyes(meshes['baby'], rgba(
            ASSETS / 'textures/entity/chao/official' / (canonical + '.png')), form)
        atlas_source = rgba(ASSETS / 'textures/entity/chao' / (canonical + '.png'))
        native_mask = skin_mask(meshes['baby'], native_source)
        atlas_mask = atlas_skin_mask(atlas_source, materials)
        # Canonical family eyes are corrected to the gallery reference. These
        # custom coats are separate from the byte-exact supplied source PNGs.
        # Aura, alpha and family anatomy stay independent of inherited colour.
        for directory, image in [('textures/entity/chao/official', native_source),
                                 ('textures/entity/chao', atlas_source)]:
            path = output / directory / (canonical + '.png')
            save(image, path); records.append(path.relative_to(output).as_posix())
        for coat, name, target, two, shiny in coat_specs:
            key = canonical + '_' + coat
            if name == 'normal' and two:
                native, atlas = native_source.copy(), atlas_source.copy()
            elif name == 'jewel_moon':
                native = patterned_moon(native_source, native_mask, native_source.width / 64)
                atlas = patterned_moon(atlas_source, atlas_mask, 1)
            else:
                jewel = name.startswith('jewel_')
                # The gallery's special regular mono-tone Chaos is white in
                # all three alignments. It is not the ordinary blue child.
                target = COLORS['white'] if name == 'normal' else target
                dark_two = form == 'devil' and two and not jewel
                native = recolor_native(meshes['baby'], native_source, target,
                    two_tone=dark_two, shiny=shiny,
                    invert_two_tone=dark_two,
                    accent_rgb=tuple(round(c * .15) for c in target) if dark_two else None)
                atlas = recolor_atlas(atlas_source, atlas_mask, target,
                    two_tone=two, shiny=shiny, jewel=jewel, form=form, materials=materials)
            unchanged_outside(native_source, native, native_mask)
            unchanged_outside(atlas_source, atlas, atlas_mask)
            protected_checks += 2
            for directory, image in [('textures/entity/chao/official', native),
                                     ('textures/entity/chao', atlas)]:
                path = output / directory / (key + '.png')
                save(image, path); records.append(path.relative_to(output).as_posix())
    if len(records) != 791 or len(set(records)) != len(records):
        raise ValueError('Heritage resource count mismatch: ' + str(len(records)))
    for key, digest in original_hashes.items():
        if hashlib.sha256((ART / 'textures' / (key + '.png')).read_bytes()).hexdigest() != digest:
            raise ValueError('Raw official source modified: ' + key)
    result = {'version': '0.18.0', 'moon_resources': 89, 'chaos_forms': list(FORMS),
              'chaos_effective_coats_per_form': 116, 'chaos_expanded_resources': 696,
              'canonical_family_resources': 6, 'resource_count': len(records),
              'canonical_eye_palette': EYES,
              'protected_uv_and_alpha_checks': protected_checks + len(FORMS),
              'coat_mask_checks': protected_checks, 'eye_only_paint_checks': len(FORMS),
              'raw_source_png_count_preserved': len(original_hashes),
              'resources': records,
              'notes': ['Authored blue-purple Moon finish; no copied gallery pixels.',
                        'Canonical Chaos anatomy/eyes/aura are preserved.',
                        'Shader provides moving sheen; no fixed white egg-base crosses.',
                        'Shiny-jewel version-specific rendering glitches are not reproduced.']}
    manifest = output / 'heritage_coats_018.json'
    manifest.write_text(json.dumps(result, indent=2) + '\n')
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ASSETS)
    args = parser.parse_args()
    result = build(args.output)
    print(f"Heritage018: {result['resource_count']} resources;89Moon;348Chaoscoats×2UVlayouts;"
          f" {result['protected_uv_and_alpha_checks']} protected-UV/alpha checks;20rawPNGs preserved.")


if __name__ == '__main__':
    main()
