"""Authored16px seed clusters with husks and fruit-related silhouettes.

Small separated kernels use the readable cluster vocabulary inspected in
Farmer's Delight and other crop mods. Their exact pixels and fruit-shaped
husks are original to this project; no external texture is embedded.
"""
from PIL import Image, ImageDraw
from sa2_fruit_art import PALETTES, ADVENTURE_SHAPES, rgb

# The largest kernel occupies less area than a whole fruit and has no stalk.
# Its coat echoes the parent crop; a pale split seam keeps it reading as seed.
SHAPES = {
    'garden_fruit': [(6,2),(8,2),(10,5),(10,8),(8,11),(5,11),(3,9),(3,6),(4,4)],
    'hero_garden_fruit': [(7,1),(9,4),(10,7),(9,10),(7,12),(4,10),(3,7),(4,4)],
    'dark_garden_fruit': [(7,1),(10,4),(11,7),(9,10),(6,12),(3,9),(2,6),(4,3)],
    'strong_fruit': [(7,1),(8,4),(10,6),(10,9),(8,11),(4,11),(2,8),(3,5),(5,4)],
    'tasty_fruit': [(8,2),(10,2),(10,4),(8,5),(9,8),(7,11),(4,12),(2,10),(2,7),(5,5),(7,5)],
    'hero_fruit': [(5,3),(8,2),(10,4),(11,7),(9,10),(6,12),(3,10),(2,7),(3,4)],
    'dark_fruit': [(10,1),(12,2),(10,4),(10,7),(8,10),(5,12),(1,12),(4,9),(6,6),(7,3)],
    'round_fruit': [(5,3),(8,3),(11,6),(11,8),(8,11),(5,11),(2,8),(2,6)],
    'square_fruit': [(3,3),(9,3),(11,5),(11,10),(9,12),(4,12),(2,10),(2,5)],
    'triangle_fruit': [(7,2),(9,5),(11,10),(2,10),(4,7)],
    'heart_fruit': [(3,3),(5,2),(7,4),(9,2),(11,4),(11,7),(9,10),(7,12),(4,10),(2,7),(2,4)],
    'chao_fruit': [(6,2),(8,2),(9,5),(10,6),(11,9),(9,11),(5,12),(2,10),(2,7),(4,5),(5,5)],
    'smart_fruit': [(6,2),(9,3),(10,5),(10,9),(8,11),(5,11),(2,8),(3,5)],
    'stamina_fruit': [(7,2),(10,4),(11,7),(9,10),(6,12),(3,10),(2,7),(4,4)],
}

SEAMS = {
    'garden_fruit': [(6,4),(5,6),(5,8),(6,9)],
    'hero_garden_fruit': [(7,3),(6,5),(6,8),(7,10)],
    'dark_garden_fruit': [(7,3),(6,5),(5,7),(6,10)],
    'strong_fruit': [(7,4),(6,6),(5,8),(6,9)],
    'tasty_fruit': [(8,4),(6,6),(5,8),(4,10)],
    'hero_fruit': [(6,4),(5,6),(5,8),(6,10)],
    'dark_fruit': [(9,3),(8,5),(7,7),(5,9),(4,10)],
    'round_fruit': [(6,5),(5,6),(5,8),(6,9)],
    'square_fruit': [(6,5),(5,6),(5,9),(7,10)],
    'triangle_fruit': [(7,5),(6,7),(5,9),(7,9)],
    'heart_fruit': [(7,5),(6,7),(6,9),(7,10)],
    'chao_fruit': [(6,4),(6,6),(5,8),(6,10)],
    'smart_fruit': [(6,4),(5,6),(5,8),(6,9)],
    'stamina_fruit': [(7,4),(6,6),(5,8),(6,10)],
}
for _fruit,_shape in ADVENTURE_SHAPES.items():
    SHAPES[_fruit]=[(round(1+x*.7),round(1+y*.7)) for x,y in _shape]
    SEAMS[_fruit]=[(6,4),(5,6),(5,8),(6,10)]


def mix(a, b, fraction):
    return tuple(round(x*(1-fraction)+y*fraction) for x,y in zip(a,b))


def _paint_kernel(image, polygon, fruit_color, *, seam=(), pale=False):
    mask = Image.new('1', image.size)
    ImageDraw.Draw(mask).polygon(polygon, fill=1)
    # A natural husk palette moderates the food colors, with a brighter split.
    base = mix(fruit_color, (190,151,83), .27)
    outline = mix(base, (73,51,35), .54)
    shadow = mix(base, (96,67,43), .31)
    highlight = mix(base, (249,224,153), .43)
    split = (247,222,154) if not pale else (255,245,196)
    for y in range(16):
        for x in range(16):
            if not mask.getpixel((x,y)): continue
            edge = any(not (0 <= u < 16 and 0 <= v < 16) or not mask.getpixel((u,v))
                       for u,v in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)))
            color = outline if edge else shadow if x > 7 or y > 9 else highlight if x < 5 or y < 5 else base
            image.putpixel((x,y), (*color,255))
    draw=ImageDraw.Draw(image)
    if seam:
        draw.line(seam, fill=(*split,255), width=1)
        end=seam[-1]
        # A tiny dark germ at the base of the pale split distinguishes a husk.
        draw.point(end, fill=(*outline,255))


def sprite(fruit):
    image=Image.new('RGBA',(16,16))
    color=rgb(PALETTES[fruit][0])
    if fruit.startswith('mushroom'):
        # The two mushroom crops are propagated by spore-bearing kernels in
        # this orchard adaptation. Three detached grains avoid a tiny food cap.
        pale = fruit == 'mushroom_alt'
        if pale: color=mix(color,(231,218,133),.38)
        for polygon,seam in (
            ([(4,2),(6,2),(7,4),(6,6),(4,6),(3,4)],[(5,3),(5,5)]),
            ([(11,4),(13,5),(14,7),(13,9),(11,9),(10,7),(10,5)],[(11,5),(11,7)]),
            ([(5,10),(7,10),(8,12),(7,14),(5,14),(4,12)],[(6,11),(6,13)]),
        ):
            _paint_kernel(image,polygon,color,seam=seam,pale=pale)
        return image
    _paint_kernel(image,SHAPES[fruit],color,seam=SEAMS[fruit],pale=fruit=='hero_garden_fruit')
    # A second detached kernel is the shared seed-item cue; it is a small
    # supporting shape, rather than the old large identical brown bean.
    _paint_kernel(image,[(12,12),(14,12),(14,14),(12,15),(11,14),(11,13)],
                  color,seam=[(13,13),(13,14)])
    return image
