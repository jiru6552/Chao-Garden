#!/usr/bin/env python3
"""Guard the shipped Inspector backgrounds against the old baked bottom well.

Checks the actual final PNG output, independent of the generator implementation.
The Guide intentionally keeps a coloured note well; rounded transparent corners
and the opaque card are checked separately so this regression fix cannot flatten
the shared panel or restore the earlier dark corner artifacts.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
GUI = ROOT / 'src/main/resources/assets/chao_garden/textures/gui'


def main():
    for theme in ('status', 'genetics', 'animals', 'guide'):
        with Image.open(GUI / f'adventure_{theme}.png') as source:
            source.load()
            assert source.size == (600, 360), (theme, source.size)
            im = source.convert('RGBA')
        # Sample only the clear interior: outside the placard, border and the
        # antialiased rounded ends of the former bottom rectangle.
        card = im.getpixel((230, 230))
        assert card == (248, 253, 255, 255), (theme, 'card changed', card)
        bottom = {im.getpixel((x, y)) for x in range(225, 562, 7) for y in range(252, 297, 5)}
        if theme == 'guide':
            assert bottom == {(225, 250, 251, 255)}, ('Guide note well lost', bottom)
        else:
            assert bottom == {card}, (theme, 'stray coloured well under Inspector content', bottom)
        for point in ((0, 0), (599, 0), (0, 359), (599, 359)):
            assert im.getpixel(point) == (0, 0, 0, 0), (theme, 'corner artifact', point)
    print('Inspector UI: all three bottom cards clear; Guide note well and four transparent corners preserved.')


if __name__ == '__main__':
    main()
