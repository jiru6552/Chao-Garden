"""Original, palette-limited inventory art shared by every Chao egg family.

Sprites are authored directly on the native 16-pixel grid.  The shell, its
lighting and its markings use opaque pixels; only the background is transparent.
"""
from pathlib import Path
import io
import os

from PIL import Image, ImageDraw


EGG_ROWS = {
    # A broad four-pixel crown and faster-opening shoulders describe a curved
    # egg, rather than the old almost triangular two-pixel tip.
    1: (6, 9), 2: (4, 11), 3: (3, 12), 4: (3, 12),
    5: (2, 13), 6: (2, 13), 7: (2, 13), 8: (2, 13),
    9: (2, 13), 10: (2, 13), 11: (3, 12), 12: (3, 12),
    13: (4, 11), 14: (6, 9),
}


def rgb(value):
    if isinstance(value, str):
        return tuple(bytes.fromhex(value.removeprefix('#')))
    return tuple(value[:3])


def mix(first, second, amount):
    return tuple(round(a * (1 - amount) + b * amount) for a, b in zip(first, second))


def shade(color, value):
    return mix(color, (255, 252, 238), value - 1) if value > 1 else mix(color, (21, 32, 49), 1 - value)


def _shell(color, shiny=False):
    """Six stepped light bands make the egg read as a solid rounded shell."""
    image = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    mask = Image.new('L', (16, 16), 0)
    pixels, silhouette = image.load(), mask.load()
    for y, (left, right) in EGG_ROWS.items():
        for x in range(left, right + 1):
            silhouette[x, y] = 255
            edge = x in (left, right) or y in (1, 14)
            if edge:
                # A lit upper-left rim and dark lower-right rim avoid a flat
                # black border, while keeping pale shells visible on light GUIs.
                light = .72 if x == left and y < 11 else .44
                if y == 1:
                    light = .72
            else:
                light = 1.11 - max(0, x - 5) * .057 - max(0, y - 4) * .016
                light = max(.59, min(1.16, round(light * 8) / 8))
                if shiny:
                    light = light + .08 if x < 8 else light - .09
            pixels[x, y] = (*shade(color, light), 255)
    return image, mask


def _markings(image, mask, color):
    """Three irregular patches, shaded with the shell rather than flat decals."""
    marks = Image.new('L', (16, 16), 0)
    draw = ImageDraw.Draw(marks)
    draw.polygon(((8, 4), (10, 5), (10, 7), (8, 7), (7, 6)), fill=255)
    draw.polygon(((3, 8), (5, 8), (6, 10), (4, 11), (3, 10)), fill=255)
    draw.polygon(((9, 10), (11, 10), (12, 11), (10, 13), (8, 12)), fill=255)
    for y in range(16):
        for x in range(16):
            if marks.getpixel((x, y)) and mask.getpixel((x, y)):
                left, right = EGG_ROWS[y]
                if x in (left, right) or y in (1, 14):
                    continue
                light = max(.59, min(1.12, round((1.11 - max(0, x-5)*.057 - max(0, y-4)*.016)*8)/8))
                image.putpixel((x, y), (*shade(color, light), 255))


def _glare(image, shiny=False):
    draw = ImageDraw.Draw(image)
    draw.line((5, 5, 5, 7), fill='#fffdf0')
    draw.point((6, 4), fill='#fffdf0')
    if shiny:
        # The small four-point glint remains separate from the shell's spots.
        draw.line((12, 1, 12, 5), fill='#fff6bf')
        draw.line((10, 3, 14, 3), fill='#fff6bf')
        draw.point((12, 3), fill='#ffffff')
        draw.point((4, 12), fill='#fff7c8')


def egg(color, shiny=False, jewel=False):
    """Return a native RGBA egg.  Hex strings and RGB tuples are accepted.

    Normal Chao eggs use the familiar pale shell and turquoise patches.  Other
    inherited colors tint the whole shell; jewel shells have broad cut facets.
    The public call remains compatible with the original three-argument helper.
    """
    color = rgb(color)
    normal = color == (110, 211, 227) and not jewel
    shell_color = (241, 237, 212) if normal else color
    image, mask = _shell(shell_color, shiny)
    if jewel:
        facets = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        draw = ImageDraw.Draw(facets)
        for points, light in (
            (((7, 2), (9, 4), (6, 7), (4, 6)), 1.45),
            (((4, 6), (6, 7), (7, 11), (4, 12), (3, 9)), .92),
            (((6, 7), (9, 4), (11, 7), (8, 11)), 1.25),
            (((9, 4), (11, 5), (12, 9), (11, 7)), .72),
            (((11, 7), (12, 9), (10, 12), (8, 11)), .59),
            (((4, 12), (7, 11), (10, 12), (8, 13), (6, 13)), .77),
        ):
            draw.polygon(points, fill=(*shade(color, light), 255))
        # Every facet is clipped to the shared shell silhouette.
        for y in range(16):
            for x in range(16):
                if mask.getpixel((x, y)) and facets.getpixel((x, y))[3]:
                    image.putpixel((x, y), facets.getpixel((x, y)))
        draw = ImageDraw.Draw(image)
        draw.line((6, 7, 8, 11), fill=(*shade(color, .52), 255))
        draw.point((6, 6), fill=(*shade(color, 1.65), 255))
        draw.point((9, 4), fill='#fffef1')
    else:
        _markings(image, mask, color if normal else shade(color, .76))
        _glare(image, shiny)
    return image


def themed_egg(entry):
    """Use the same lit shell for the complete mod-gated theme catalog."""
    from variant_icons import theme_icon

    if entry['id'] in ('nether', 'end'):
        from dimension_egg_art import egg_sprite
        return egg_sprite(entry['id'], EGG_ROWS)
    palette = entry['palette']
    base = rgb(palette['body'])
    shell = mix(base, (246, 242, 220), .40)
    image, mask = _shell(shell)
    # Native tiny artwork preserves the distinctive item silhouette at 8px.
    # Canonical icon colors have their own outline rather than inheriting the
    # coat accent and losing the mod's recognizable visual cues.
    symbol = theme_icon(entry, 8)
    origin_x, origin_y = 4, 5
    occupied = set()
    if entry['id'] == 'ars_nouveau':
        bounds = symbol.getbbox()
        origin_x = (16-(bounds[2]-bounds[0]))//2-bounds[0]
        origin_y = (16-(bounds[3]-bounds[1]))//2-bounds[1]
    for y in range(8):
        for x in range(8):
            color = symbol.getpixel((x, y))
            if color[3] and mask.getpixel((x + origin_x, y + origin_y)):
                image.putpixel((x + origin_x, y + origin_y), color)
                occupied.add((x + origin_x, y + origin_y))
    # Shell lighting must never repaint a canonical mod glyph. Four icons
    # have ink at (5,5), where the former final glare pixel erased a detail.
    for point, color in (((6, 4), (255, 252, 235, 255)),
                         ((5, 5), (*shade(shell, 1.3), 255))):
        if point not in occupied:
            image.putpixel(point, color)
    return image


def drive(color):
    """A long faceted Chaos Drive crystal held between stacked metal caps.

    The supplied Sonic Adventure reference photographs establish the shape:
    an elongated colored prism and two striped gray ends.  This is an original
    16px drawing of that construction rather than a scaled photograph.
    """
    color = rgb(color)
    image = Image.new('RGBA', (16,16), (0,0,0,0)); draw = ImageDraw.Draw(image)
    # Both caps share the same centerline. Straight crystal sides and vertical
    # facets make a long prism read at inventory size without a card-like tilt.
    draw.rectangle((5,3,10,12), fill=(*shade(color,.38),255))
    draw.rectangle((6,4,8,11), fill=(*shade(color,1.09),255))
    draw.line((5,4,5,11), fill=(*shade(color,.77),255))
    draw.line((9,4,9,11), fill=(*shade(color,.84),255))
    draw.line((10,4,10,11), fill=(*shade(color,.52),255))
    draw.polygon(((6,4),(8,4),(6,9)), fill=(*shade(color,1.27),255))
    draw.line((6,5,6,7), fill=(*shade(color,1.48),255))
    # Recessed central channel, following the keeper's marked-up reference.
    # Two dark colored columns keep the long groove readable at inventory size.
    draw.rectangle((7,5,8,10), fill=(*mix(color,(24,30,36),.48),255))
    draw.line((7,4,7,11), fill=(*mix(color,(24,30,36),.38),255))
    # Wide, striped gray collars enclose each end of the colored prism.
    for top in (0,12):
        draw.rectangle((4,top+1,11,top+2), fill='#35434b')
        draw.line((5,top,10,top), fill='#7b8e8e')
        draw.line((4,top+1,10,top+1), fill='#c4cec1')
        draw.line((5,top+2,10,top+2), fill='#748b88')
        draw.line((5,top+3,10,top+3), fill='#bac8bc')
        draw.point((11,top+2), fill='#465c60')
    return image


def save_sprite(image, path):
    """Keep complete encoded bytes available even during rebuilds."""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    buffer = io.BytesIO()
    image.save(buffer, format='PNG', optimize=True)
    payload = buffer.getvalue()
    with Image.open(io.BytesIO(payload)) as check:
        check.verify()
    temporary = path.with_name(path.name + '.writing')
    with temporary.open('wb') as handle:
        handle.write(payload)
        handle.flush()
        os.fsync(handle.fileno())
    os.replace(temporary, path)
