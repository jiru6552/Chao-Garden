#!/usr/bin/env python3
"""Generate the silver reflection and remove obsolete painted egg sparkles.

The source shell pattern is retained. Only the exact white cross emitted by
the pre-0.17 generator is repaired; no model UVs, seams, spots or colors move.
"""
from collections import Counter
from pathlib import Path
import math
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
SPOTS = [
    [(4,6),(11,3),(16,8),(14,15),(7,17),(2,12)],
    [(33,1),(39,4),(42,10),(36,14),(29,10)],
    [(48,20),(57,17),(62,23),(59,31),(50,30),(46,26)],
    [(19,25),(27,23),(33,29),(30,37),(22,38),(17,32)],
    [(2,46),(9,41),(15,46),(15,54),(6,57),(0,52)],
    [(38,47),(45,43),(51,49),(49,57),(41,61),(35,54)],
]


def main():
    output = ASSETS / 'textures/misc/chao_reflection.png'
    output.parent.mkdir(parents=True, exist_ok=True)
    image = Image.new('RGBA', (64,64))
    # Neutral reflections replace the purple enchanted-item tint. Black parts
    # add no light under vanilla's foil blend; tapered ribbons provide shine.
    for y in range(64):
        for x in range(64):
            phase = ((x + y*.33) / 64) % 1
            distance = abs(phase-.46)
            ribbon = math.exp(-((distance/.085)**2))
            shoulder = math.exp(-(((phase-.60)/.17)**2))*.24
            # Foil uses SRC_COLOR,ONE, effectively squaring the brightness.
            # A bright tapered highlight remains visible after that blend.
            tone = min(255, round(12+(ribbon+shoulder)*212))
            image.putpixel((x,y), (round(tone*.90),round(tone*.96),tone,255))
    image.save(output, optimize=True)

    spot_mask = Image.new('1',(64,64))
    draw = ImageDraw.Draw(spot_mask)
    for polygon in SPOTS: draw.polygon(polygon,fill=1)
    count = pixels = 0
    for path in sorted((ASSETS/'textures/entity/eggs').glob('*.png')):
        if not (path.stem.endswith('_shiny') or path.stem.startswith('jewel_')):
            continue
        egg = Image.open(path).convert('RGBA')
        old_cross = [(x,y) for y in range(3,18) for x in range(5,20)
                     if egg.getpixel((x,y)) == (250,255,255,255)]
        if not old_cross: continue
        sibling = path.with_name(path.name.replace('_shiny.png','.png'))
        if path.stem.endswith('_shiny') and not path.stem.startswith('jewel_') and sibling.exists():
            source = Image.open(sibling).convert('RGBA')
            for point in old_cross: egg.putpixel(point,source.getpixel(point))
        else:
            # Jewel atlases consist of two solid colors plus this old cross.
            # Read their existing colors instead of duplicating palette values.
            colors = Counter(egg.getdata())
            colors.pop((250,255,255,255),None)
            shell,spot = [entry[0] for entry in colors.most_common(2)]
            for point in old_cross:
                egg.putpixel(point, spot if spot_mask.getpixel(point) else shell)
        egg.save(path,optimize=True)
        count += 1; pixels += len(old_cross)
    print(f'Silver reflection generated; repaired {pixels} painted sparkle pixels in {count} placed-egg atlases.')


if __name__ == '__main__': main()
