#!/usr/bin/env python3
"""Check actual Java coat-name coverage, source alpha and raw texture parity."""
from pathlib import Path
import hashlib
import json
import re

from PIL import Image
from build_heritage_018 import ASSETS, ART, ROOT, KEYS, FORMS, COLORS, JEWELS, EYES
from import_official_chao import native_skin_masks


def validate():
    manifest = json.loads((ASSETS / 'heritage_coats_018.json').read_text())
    java = (ROOT / 'src/main/java/com/chaogarden/client/ChaoRenderer.java').read_text()
    arrays = {}
    for name in ('COLORS', 'JEWELS'):
        body = re.search(r'String\[\]\s+' + name + r'\s*=\s*\{(.*?)\}', java, re.S).group(1)
        arrays[name] = re.findall(r'"([a-z_]+)"', body)
    assert arrays['COLORS'] == list(COLORS), 'Java colour order differs from generated resources'
    assert arrays['JEWELS'] == ['none', *JEWELS], 'Java jewel order differs from generated resources'
    actual = set(manifest['resources'])
    checks = 0
    keys = set()
    for form in FORMS:
        family = 'chaos_' + form
        for color in range(len(arrays['COLORS'])):
            for jewel in range(len(arrays['JEWELS'])):
                for two in (False, True):
                    for shiny in (False, True):
                        coat = ('jewel_' + arrays['JEWELS'][jewel]) if jewel else arrays['COLORS'][color]
                        coat += ('_two' if two else '_mono') + ('_shiny' if shiny else '')
                        key = family if not color and not jewel and two and not shiny else family + '_' + coat
                        keys.add(key)
                        for folder in ('textures/entity/chao/official', 'textures/entity/chao'):
                            path = folder + '/' + key + '.png'
                            assert path in actual and (ASSETS / path).is_file(), path
                            checks += 1
    assert len(keys) == 348, len(keys)
    raw = {}
    for key in KEYS:
        original = ART / 'textures' / (key + '.png')
        runtime = ASSETS / 'textures/entity/chao/official' / (key + '.png')
        assert original.read_bytes() == runtime.read_bytes(), 'Raw comparison texture altered: ' + key
        raw[key] = hashlib.sha256(original.read_bytes()).hexdigest()
        checks += 1
    source = json.loads((ART / 'models/chao_baby.geo.json').read_text())['minecraft:geometry'][0]
    eye_pixels = {}
    for form in FORMS:
        image = Image.open(ASSETS / 'textures/entity/chao/official' / ('chaos_' + form + '.png')).convert('RGBA')
        _, mask, _ = native_skin_masks(source, image)
        samples = 0
        for y in range(image.height):
            for x in range(image.width):
                if mask.getpixel((x, y)) and image.getpixel((x, y))[3]:
                    assert image.getpixel((x, y))[:3] == EYES[form], (form, x, y)
                    samples += 1
        assert samples > 0
        eye_pixels[form] = samples
        checks += samples
    generated_hashes = {}
    for relative in actual:
        with Image.open(ASSETS / relative) as image:
            assert image.format == 'PNG'
            image.verify()
        generated_hashes[relative] = hashlib.sha256((ASSETS / relative).read_bytes()).hexdigest()
        checks += 1
    result = {'state_combinations': 2688, 'state_texture_path_checks': 5376,
              'effective_runtime_keys': len(keys), 'valid_generated_pngs': len(actual),
              'native_eye_pixels_by_family': eye_pixels, 'raw_comparison_png_sha256': raw,
              'manifest_sha256': hashlib.sha256((ASSETS / 'heritage_coats_018.json').read_bytes()).hexdigest(),
              'generated_png_sha256': dict(sorted(generated_hashes.items())),
              'checks': checks, 'result': 'passed'}
    path = ROOT / 'docs/validation-0.18/heritage-coats.json'
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(result, indent=2) + '\n')
    print('Heritage validation:', result['state_combinations'], 'states;',
          result['effective_runtime_keys'], 'keys;', result['valid_generated_pngs'],
          'PNGs;', checks, 'checks passed')
    return result


if __name__ == '__main__':
    validate()
