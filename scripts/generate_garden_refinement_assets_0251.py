#!/usr/bin/env python3
"""Deterministic native materials for the SA2 garden's grass, crag and quiet shore."""
from pathlib import Path
import math
import random
import json
from lobby_material_manifest0255 import render_textures
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'src/main/resources/assets/chao_garden/textures/scene'
TARGET.mkdir(parents=True, exist_ok=True)
SIZE = 64

def field(x, y, phase=0):
    # Whole integer periods make these materials continuous when a face repeats.
    u, v = x / SIZE * math.tau, y / SIZE * math.tau
    return (math.sin(u * 2 + v + phase) * 0.45
            + math.sin(u * 5 - v * 3 + phase * 2) * 0.26
            + math.cos(u * 11 + v * 7 + phase) * 0.17
            + math.sin(u * 23 - v * 19 + phase) * 0.12)

def image(base, amplitude, phase):
    out = Image.new('RGBA', (SIZE, SIZE))
    pixels = out.load()
    for y in range(SIZE):
        for x in range(SIZE):
            n = field(x, y, phase) * amplitude
            pixels[x, y] = tuple(max(0, min(255, round(c + n))) for c in base) + (255,)
    return out

# The first native study exposed a strong repeating vertical/rattan pattern.
# Toroidal jittered 2D cells provide subtle angular fractures in several
# directions; the wrapped distance field remains continuous across tile edges.
# This is authored art, not a game-texture extract.
rock_rng = random.Random(25102)
sites = []
for attempt in range(400):
    x, y = rock_rng.randrange(SIZE), rock_rng.randrange(SIZE)
    if all(min(abs(x-a), SIZE-abs(x-a))**2 + min(abs(y-b), SIZE-abs(y-b))**2 >= 17**2
           for a, b, tone in sites):
        sites.append((x, y, rock_rng.uniform(-5, 5)))
    if len(sites) == 7:
        break
rock = Image.new('RGBA', (SIZE, SIZE))
pixels = rock.load()
for y in range(SIZE):
    for x in range(SIZE):
        nearest = sorted((math.hypot(min(abs(x-a), SIZE-abs(x-a)),
                                    min(abs(y-b), SIZE-abs(y-b))), tone)
                         for a, b, tone in sites)
        gap = nearest[1][0] - nearest[0][0]
        fracture = max(0, 1-gap/1.6) * 12
        grain = rock_rng.uniform(-2, 2)
        broad = 2 * math.sin(x*math.tau/SIZE*3 + y*math.tau/SIZE*2)
        shade = nearest[0][1] + broad + grain - fracture
        pixels[x, y] = tuple(max(0, min(255, round(c+shade))) for c in (159,155,151)) + (255,)
rock.save(TARGET / 'garden_limestone.png', optimize=True)

# Low mixed green blades retain a soft lawn at a distance without the old neon
# lime shading and extremely regular horizontal bands.
grass = image((70, 146, 34), 12, 2.2)
draw = ImageDraw.Draw(grass)
rng = random.Random(25101)
for i in range(195):
    x, y = rng.randrange(SIZE), rng.randrange(SIZE)
    color = rng.choice([(78, 155, 39, 255), (61, 130, 29, 255), (91, 158, 43, 255), (71, 143, 35, 255)])
    draw.line((x, y, x + rng.choice([-1, 0, 1]), y - rng.choice([1, 2, 3])), fill=color)
grass.save(TARGET / 'garden_turf.png', optimize=True)

# A thin grass lip on side faces prevents every crag crest from becoming a
# one-metre neon-green stripe. The soil/rock continues down the same block.
edge = rock.copy()
for x in range(SIZE):
    depth = 3 + round(1.1 * math.sin(x * math.tau / SIZE * 5))
    for y in range(depth):
        edge.putpixel((x, y), grass.getpixel((x, y)))
edge.save(TARGET / 'garden_turf_edge.png', optimize=True)
assets = ROOT / 'src/main/resources/assets'
model = {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': 'chao_garden:scene/garden_turf',
    'bottom': 'chao_garden:scene/garden_limestone',
    'side': 'chao_garden:scene/garden_turf_edge'}}
(assets / 'chao_garden/models/block/garden_turf.json').write_text(json.dumps(model, indent=2) + '\n')
manifest_path = ROOT / 'art/lobby_garden_manifest_025.json'
manifest = json.loads(manifest_path.read_text())
manifest['block_atlas_textures'] = sorted(set(manifest['block_atlas_textures']) | {'scene/garden_turf_edge'})
manifest['render_textures'] = render_textures(ROOT, manifest['block_atlas_textures'])
manifest['refinement_generator'] = 'scripts/generate_garden_refinement_assets_0251.py'
manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
atlas = {'sources': [{'type': 'minecraft:single', 'resource': 'chao_garden:' + name}
                     for name in manifest['block_atlas_textures']]}
(assets / 'minecraft/atlases/blocks.json').write_text(json.dumps(atlas, indent=2) + '\n')

# The reference pond has a narrow pale stone bank; keep the existing native
# shore material ID but avoid a broad bright yellow beach around the whole pool.
shore = image((183, 178, 160), 6, 4.1)
shore.save(TARGET / 'garden_sand.png', optimize=True)
for name in ('garden_limestone', 'garden_turf', 'garden_turf_edge', 'garden_sand'):
    with Image.open(TARGET / (name + '.png')) as check:
        assert check.size == (64, 64) and check.mode == 'RGBA'
        assert check.getextrema()[3] == (255, 255)
print('Garden0251 materials: three revised materials plus thin turf edge; four deterministic 64px textures and exact native atlas bindings.')
