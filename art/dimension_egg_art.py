"""Authored netherrack grain and borderless portal art for ordinary egg shapes.

The netherrack motif repeats at its native texel density on the world shell;
enlarging a single tile made the previous shell look like broad red bricks.
The End atlas is a static fallback.  Its placed egg uses the native portal
render pass, while this matching unframed starfield also supplies its item.
"""
from PIL import Image

NETHER = {'o': '452427', 'd': '5b3032', 'b': '743b3d', 'w': '944c4c', 'p': 'b06460'}
END_DARK, END_SOFT = '071115', '102226'
END_STARS = ('6dada6', 'c8ddd5', '447e7b')

# Broken, irregular one/two-pixel red stone clusters and dark porous seams.
# The 1.21.1 vanilla netherrack texture was the material reference; the pixels
# below are newly authored, rather than a copy of the vanilla texture.
ROCK = (
    'bdbbwwbbdbwwbbbd', 'dddbbwbbddbwbbbd', 'oddbbdbbbddbbpwb', 'dbbwbbwwbbddbbbb',
    'bbwwbwbbbwbbdodb', 'bwppwbbddbbwwdbb', 'bwwbbddoddbwwbbb', 'bbdbbdbddbwbdbbd',
    'dbddbwwbbwwbbddd', 'ddbbwwpwbwbbddob', 'bdbbwbbddbbbpwdd', 'wwbbdbbddbwbbbbb',
    'pwbddodbbwwbddbw', 'wwbdddbbwwbbddbw', 'bbdbbwwbdbddbbpw', 'bddbbwbddoddbbwb',
)

def rgb(value): return tuple(bytes.fromhex(value))

def _world_wrap(image, size):
    # World-shell sampling includes the longitude endpoints.  Match their
    # texels so the closing longitude does not introduce another seam.
    if size >= 32:
        for y in range(image.height): image.putpixel((image.width-1,y),image.getpixel((0,y)))
    return image

def surface(ident, size=16):
    """A full opaque texture tile, suitable for the existing 3D egg UVs."""
    if ident == 'nether':
        tile = Image.new('RGBA', (16,16))
        for y, row in enumerate(ROCK):
            for x, key in enumerate(row): tile.putpixel((x,y), (*rgb(NETHER[key]),255))
        image = Image.new('RGBA',(size,size))
        # Keep one texture pixel per native shell texel.  Four irregular tiles
        # across the 64px atlas give the placed egg grain instead of red bands.
        for y in range(size):
            for x in range(size): image.putpixel((x,y),tile.getpixel((x%16,y%16)))
        return _world_wrap(image,size)
    if ident != 'end': raise ValueError(ident)
    image = Image.new('RGBA', (size,size), (*rgb(END_DARK),255))
    for y in range(size):
        for x in range(size):
            # A full starfield reaches every part of the shell.  There are no
            # side bands, polar caps or decorative purple framing pixels.
            if (x*13+y*23+x*y*3) % 47 == 0:
                color = END_STARS[(x+y)%3]
            elif (x*5+y*11+x*y)%13 == 0:
                color = END_SOFT
            else: color = END_DARK
            image.putpixel((x,y), (*rgb(color),255))
    return _world_wrap(image,size)

def egg_sprite(ident, rows):
    """Use the shared rounded egg outline, with the entire shell retextured."""
    image = Image.new('RGBA',(16,16))
    tile = surface('nether',16) if ident == 'nether' else None
    star_positions = {(5,5):0,(9,4):1,(10,8):0,(4,9):1,(7,11):2,(11,6):2,(8,7):0}
    for y,(left,right) in rows.items():
        for x in range(left,right+1):
            edge = x in (left,right) or y in (1,14)
            if ident == 'nether':
                color = tile.getpixel((x,y))[:3]
                # Carry the irregular grain through the rim instead of drawing
                # a uniform brown outline; quiet shading keeps its egg volume.
                factor = (.80 if edge else 1.08) - max(0,x-5)*.020 - max(0,y-5)*.009
                color = tuple(round(c*factor) for c in color)
            elif ident == 'end':
                if (x,y) in star_positions: color=rgb(END_STARS[star_positions[(x,y)]])
                else: color=rgb(END_SOFT if (x//3+y//4)%3==0 else END_DARK)
            else: raise ValueError(ident)
            image.putpixel((x,y), (*color,255))
    return image
