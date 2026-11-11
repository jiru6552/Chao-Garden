#!/usr/bin/env python3
"""Build the 0.14.1 icon touch-up preview from current whole game frames.

Two actual Minecraft frames show the Chao badges. The final page enlarges the
native-art reference tiles for the revised Neo Vitae drop and Iron's scroll.
Requires Pillow, ReportLab and pypdf. Render and review all three pages before
publishing; this builder validates structure, not the final page appearance.
"""
from pathlib import Path
import argparse
import importlib.util
import io
import json

from PIL import Image
from pypdf import PdfReader
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

ROOT = Path(__file__).resolve().parents[1]
ORDER = ('magic-icons', 'magic-items')
INK, MUTED, PAPER, EDGE = map(HexColor, ('#173f4b', '#47636e', '#edf3f5', '#afc8cf'))
VERSION = '0.14.1'


def clean(value):
    for before, after in (('×', 'x'), ('’', "'"), ('‘', "'"), ('—', '-'), ('–', '-')):
        value = value.replace(before, after)
    return value


def wrap(value, width, size):
    lines, line = [], ''
    for word in clean(value).split():
        candidate = (line + ' ' + word).strip()
        if line and pdfmetrics.stringWidth(candidate, 'Gallery', size) > width:
            lines.append(line)
            line = word
        else:
            line = candidate
    if line:
        lines.append(line)
    return lines


def header(pdf, title, subtitle, page, total):
    width, height = 842, 595
    pdf.setFillColor(PAPER)
    pdf.rect(0, 0, width, height, stroke=0, fill=1)
    pdf.setFillColor(INK)
    size = 22
    while pdfmetrics.stringWidth(clean(title), 'GalleryBold', size) > width - 48:
        size -= .5
    assert size >= 16, title
    pdf.setFont('GalleryBold', size)
    pdf.drawString(24, height - 31, clean(title))
    pdf.setFillColor(MUTED)
    pdf.setFont('Gallery', 10)
    assert pdfmetrics.stringWidth(clean(subtitle), 'Gallery', 10) <= width - 48
    pdf.drawString(24, height - 49, clean(subtitle))
    pdf.setFont('Gallery', 8)
    pdf.drawRightString(width - 24, 15, f'{page:02d} / {total:02d}')
    pdf.drawString(24, 15, f'CHAO GARDEN {VERSION}  |  Minecraft Java 1.21.1 / NeoForge')


def caption(pdf, text, width=794, x=24, y=57, size=9.5, max_lines=3):
    pdf.setFillColor(MUTED)
    pdf.setFont('Gallery', size)
    lines = wrap(text, width, size)
    assert len(lines) <= max_lines, lines
    for row, line in enumerate(lines):
        pdf.drawString(x, y - row * 13, line)


def revised_tiles():
    manifest = json.loads((ROOT / 'docs/theme-icon-manifest-0.14.1.json').read_text())
    assert manifest['release'] == VERSION and manifest['theme_count'] == 19
    columns, header_h, tile_w, tile_h = manifest['columns'], manifest['header_height'], 264, manifest['cell_height']
    assert columns == 5 and tile_h == 176
    icons = manifest['icons']
    wanted = ('blood_magic', 'iron_spells')
    assert all(sum(item['id'] == ident for item in icons) == 1 for ident in wanted)
    with Image.open(ROOT / 'docs/screenshots/theme-icons-0.14.1.png') as source:
        source.load()
        assert source.size == (1320, 864)
        for ident in wanted:
            index = next(index for index, item in enumerate(icons) if item['id'] == ident)
            x, y = index % columns * tile_w, header_h + index // columns * tile_h
            yield ident, source.crop((x, y, x + tile_w, y + tile_h)).convert('RGB')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path,
                        default=ROOT.parent / 'output/pdf/chao-garden-0.14.1-preview-gallery.pdf')
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location('chao_package_0141', ROOT / 'scripts/package_chao0141.py')
    art = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(art)
    assert len(ORDER) == len(set(ORDER)) == 2 and set(ORDER) <= set(art.SHOTS)
    for name, path in (('Gallery', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),
                       ('GalleryBold', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')):
        pdfmetrics.registerFont(TTFont(name, path))
    shots = ROOT / 'docs/screenshots/collection-0.14.1'
    total = 3
    buffer = io.BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=(842, 595), pageCompression=1)
    pdf.setTitle('Chao Garden 0.14.1 - Blood Drop and Scroll')
    pdf.setAuthor('Chao Garden')
    pdf.setSubject('Two actual Minecraft frames and the revised native pixel-art symbols')
    for index, key in enumerate(ORDER, 1):
        title, subtitle, text = art.SHOTS[key]
        text += ' Development identity fixtures show the themes; these views do not test complete third-party mods.'
        path = shots / f'chao014-{key}.png'
        with Image.open(path) as image:
            assert image.size == (1280, 720)
            image.verify()
        header(pdf, f'{index:02d}  {title}', subtitle, index, total)
        x, y, width, height = 24, 77, 794, 794 * 720 / 1280
        pdf.setStrokeColor(EDGE)
        pdf.setLineWidth(.6)
        pdf.rect(x - .3, y - .3, width + .6, height + .6, stroke=1, fill=0)
        pdf.drawImage(ImageReader(str(path)), x, y, width=width, height=height, mask='auto')
        caption(pdf, text)
        pdf.bookmarkPage(key)
        pdf.addOutlineEntry(clean(title), key, level=0, closed=False)
        pdf.showPage()

    header(pdf, '03  Revised native symbols',
           'Authored art reference - 16px Chao badge, native 8px egg mark and complete egg', 3, total)
    tiles = list(revised_tiles())
    assert len(tiles) == 2
    for index, (ident, tile) in enumerate(tiles):
        # Retain the full tile, including all three asset views and actual-size
        # swatches. Enlarged pixels use a nearest-neighbor raster, not smoothing.
        enlarged = tile.resize((528, 352), Image.Resampling.NEAREST)
        x = 24 + index * 404
        pdf.drawImage(ImageReader(enlarged), x, 227, width=390, height=260, mask='auto')
    caption(pdf, 'Neo Vitae: a narrow pointed tip opens into a broad, rounded droplet base. The native eight-pixel mark uses the same outline idea as the sixteen-pixel chest badge.',
            width=376, x=32, y=203, size=10, max_lines=4)
    caption(pdf, "Iron's Spells: straight unfurled parchment, balanced curls and short ink lines replace the previous irregular diagonal shape. Both sizes stay centered on their surface.",
            width=376, x=436, y=203, size=10, max_lines=4)
    caption(pdf, 'The nineteen-theme collection and its mod availability rules are retained from 0.14.0. The offline HTML gallery includes all six current game frames and the complete theme reference sheet.',
            width=794, y=102, size=10, max_lines=3)
    pdf.bookmarkPage('native-symbols')
    pdf.addOutlineEntry('Revised native symbols', 'native-symbols', level=0, closed=False)
    pdf.showPage()
    pdf.save()
    payload = buffer.getvalue()
    assert payload.startswith(b'%PDF-')
    result = PdfReader(io.BytesIO(payload))
    assert len(result.pages) == total
    assert all(page.extract_text().strip() for page in result.pages)
    assert len(result.outline) == total
    art.write_flushed(args.output, payload)
    art.write_flushed(ROOT / f'docs/chao-garden-{VERSION}-preview-gallery.pdf', payload)
    print(args.output, len(payload), 'bytes', total, 'pages; source copy written')


if __name__ == '__main__':
    main()
