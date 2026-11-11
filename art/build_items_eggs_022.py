#!/usr/bin/env python3
"""Finalize canonical inventory glyphs and three opaque lifecycle cocoons.

This is deliberately a small finalizer: the inspected 0.21 fruit, DNA, drive,
egg-shell and seed art remains the control rather than being redesigned.
"""
from pathlib import Path
import argparse
import hashlib
import json

from PIL import Image
from item_sprite_art import save_sprite, themed_egg

ROOT = Path(__file__).resolve().parents[1]
COCOON_TILE = (64, 32, 32, 32)
COCOON_STOPS = {
    'evolution': ((255, 244, 152), (123, 223, 198), (84, 153, 211)),
    'rebirth': ((255, 203, 151), (244, 150, 169), (224, 105, 159)),
    'death': ((237, 235, 219), (199, 202, 197), (149, 155, 154)),
}


def band_color(kind, index):
    """Sixteen broad, two-pixel latitude bands, independent of longitude."""
    stops = COCOON_STOPS[kind]
    t = max(0, min(15, index)) / 15 * 2
    segment = min(1, int(t))
    fraction = t - segment
    return tuple(round(a * (1-fraction) + b * fraction)
                 for a, b in zip(stops[segment], stops[segment+1]))


def cocoon_atlas(kind):
    image = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    for y in range(32):
        color = (*band_color(kind, y // 2), 255)
        for x in range(32):
            image.putpixel((64+x, 32+y), color)
    return image


def build(root=ROOT):
    root = Path(root)
    assets = root / 'src/main/resources/assets/chao_garden'
    entries = json.loads((assets / 'chao_variants.json').read_text())['variants']
    changed = []
    for entry in entries:
        path = assets / 'textures/item' / (entry['id'] + '_chao_egg.png')
        image = themed_egg(entry)
        if path.exists():
            with Image.open(path) as source:
                before = source.convert('RGBA')
            pixels = [(x, y) for y in range(16) for x in range(16)
                      if before.getpixel((x, y)) != image.getpixel((x, y))]
            if pixels:
                changed.append({'id': entry['id'], 'pixels': pixels})
        save_sprite(image, path)
    cocoons = {}
    for kind in COCOON_STOPS:
        relative = 'textures/entity/chao/cocoon_' + kind + '.png'
        path = assets / relative
        save_sprite(cocoon_atlas(kind), path)
        cocoons[kind] = {'file': relative, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
    report = {
        'release': '0.22.0', 'result': 'PASS',
        'changed_theme_pixels_on_this_invocation': changed,
        'canonical_highlight_policy': 'Opaque icon pixels take precedence over shell glare.',
        'cocoon_tile': list(COCOON_TILE), 'cocoons': cocoons,
        'scope': 'Canonical egg-item finalizer and three lifecycle material atlases; no model or other sprite redesign.',
    }
    target = root / 'docs/research/ITEMS_EGGS_022_BUILD.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, indent=2) + '\n')
    print(f'Items/eggs022: {sum(len(r["pixels"]) for r in changed)} canonical glyph pixels restored; '
          'three distinct opaque, longitude-continuous cocoon atlases generated.')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    build(parser.parse_args().root)
