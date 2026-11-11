#!/usr/bin/env python3
"""Build the 0.13 gallery from six real frames and the authored icon artwork.

Actual Minecraft frames are reproduced whole. The authored 48-theme reference
sheet is re-cut into three 4x4 pages so its labels and native icon artwork stay
readable. Requires Pillow, ReportLab and pypdf.
"""
from pathlib import Path
import argparse
import importlib.util
import io
import os

from PIL import Image
from pypdf import PdfReader
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

ROOT = Path(__file__).resolve().parents[1]
ORDER = (
    'drive-inventory', 'themed-eggs-a', 'themed-eggs-b',
    'theme-chests-tech', 'theme-chests-magic', 'theme-chests-adventure',
)
INK, MUTED, PAPER, EDGE = map(HexColor, ('#173f4b', '#47636e', '#edf3f5', '#afc8cf'))
THEME_COLS, THEME_ROWS = 6, 8
THEME_HEADER, TILE_W, TILE_H, THEME_FOOTER = 112, 264, 176, 48


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


def header(pdf, width, height, title, subtitle, page, total):
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
    pdf.drawString(24, 15, 'CHAO GARDEN 0.13.0  |  Minecraft Java 1.21.1 / NeoForge')


def caption(pdf, text, width=794, y=57, size=9.5, max_lines=3):
    pdf.setFillColor(MUTED)
    pdf.setFont('Gallery', size)
    lines = wrap(text, width, size)
    assert len(lines) <= max_lines, lines
    for row, line in enumerate(lines):
        pdf.drawString(24, y - row * 13, line)


def theme_panels(path):
    """Retain each original authored tile exactly once, without resampling."""
    with Image.open(path) as source:
        source.load()
        assert source.size == (THEME_COLS * TILE_W, THEME_HEADER + THEME_ROWS * TILE_H + THEME_FOOTER), source.size
        tiles = []
        for index in range(48):
            x = index % THEME_COLS * TILE_W
            y = THEME_HEADER + index // THEME_COLS * TILE_H
            tiles.append(source.crop((x, y, x + TILE_W, y + TILE_H)).convert('RGB'))
    assert len(tiles) == 48
    for start in (0, 16, 32):
        panel = Image.new('RGB', (TILE_W * 4, TILE_H * 4), '#edf3f5')
        for local, tile in enumerate(tiles[start:start + 16]):
            panel.paste(tile, ((local % 4) * TILE_W, (local // 4) * TILE_H))
        yield start, panel


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path,
                        default=ROOT.parent / 'output/pdf/chao-garden-0.13.0-preview-gallery.pdf')
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location('chao_package_013', ROOT / 'scripts/package_chao013.py')
    art = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(art)
    assert len(ORDER) == len(set(ORDER)) == 6 and set(ORDER) == set(art.SHOTS)
    for name, path in (('Gallery', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),
                       ('GalleryBold', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')):
        pdfmetrics.registerFont(TTFont(name, path))
    shots = ROOT / 'docs/screenshots/icons-0.13'
    total = 10
    buffer = io.BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=(842, 595), pageCompression=1)
    pdf.setTitle('Chao Garden 0.13.0 - Preview Gallery')
    pdf.setAuthor('Chao Garden')
    pdf.setSubject('Six actual Minecraft frames, the item sprites and all 48 authored theme icons')
    for index, key in enumerate(ORDER, 1):
        title, subtitle, text = art.SHOTS[key]
        if key.startswith(('themed-eggs-', 'theme-chests-')):
            text += ' Theme gates use identity-only test fixtures; these views do not test complete third-party mods.'
        path = shots / f'chao013-{key}.png'
        with Image.open(path) as image:
            assert image.size == (1280, 720)
            image.verify()
        pdf.setPageSize((842, 595))
        header(pdf, 842, 595, f'{index:02d}  {title}', subtitle, index, total)
        x, y, width, height = 24, 77, 794, 794 * 720 / 1280
        pdf.setStrokeColor(EDGE)
        pdf.setLineWidth(.6)
        pdf.rect(x - .3, y - .3, width + .6, height + .6, stroke=1, fill=0)
        pdf.drawImage(ImageReader(str(path)), x, y, width=width, height=height, mask='auto')
        caption(pdf, text)
        pdf.bookmarkPage(key)
        pdf.addOutlineEntry(clean(title), key, level=0, closed=False)
        pdf.showPage()

    pdf.setPageSize((595, 842))
    header(pdf, 595, 842, '07  Item sprite reference',
           'Authored pixel art - not a Minecraft screenshot', 7, total)
    path = ROOT / 'docs/screenshots/item-sprites-0.13.png'
    with Image.open(path) as image:
        image.load()
        source_width, source_height = image.size
    scale = min(547 / source_width, 718 / source_height)
    width, height = source_width * scale, source_height * scale
    pdf.drawImage(ImageReader(str(path)), (595 - width) / 2,
                  70 + (718 - height) / 2, width=width, height=height, mask='auto')
    caption(pdf, 'Upright drives now have dark center channels; themed eggs use native tiny symbols.',
            width=547, y=48, size=9, max_lines=2)
    pdf.bookmarkPage('sprites')
    pdf.addOutlineEntry('Item sprite reference', 'sprites', level=0, closed=False)
    pdf.showPage()

    source = ROOT / 'docs/screenshots/theme-icons-0.13.png'
    for start, image in theme_panels(source):
        page = 8 + start // 16
        title = f'{page:02d}  Theme icon reference - {start + 1:02d} to {start + 16:02d}'
        pdf.setPageSize((842, 700))
        header(pdf, 842, 700, title,
               'Authored pixel art - 16px chest badges, native 8px egg symbols and finished eggs', page, total)
        width = 794
        height = width * image.height / image.width
        assert height < 700 - 140
        pdf.drawImage(ImageReader(image), 24, 88, width=width, height=height, mask='auto')
        caption(pdf, 'Original reference tiles regrouped for clarity. Every one of the 48 themes appears once across these three pages.',
                width=794, y=63, size=9, max_lines=2)
        key = 'theme-icons-' + str(start + 1)
        pdf.bookmarkPage(key)
        pdf.addOutlineEntry(f'Theme icons {start + 1:02d}-{start + 16:02d}', key, level=0, closed=False)
        pdf.showPage()

    pdf.save()
    payload = buffer.getvalue()
    assert payload.startswith(b'%PDF-')
    result = PdfReader(io.BytesIO(payload))
    assert len(result.pages) == total
    assert all(page.extract_text().strip() for page in result.pages)
    assert len(result.outline) == total
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix('.pdf.writing')
    with temporary.open('wb') as handle:
        handle.write(payload)
        handle.flush()
        os.fsync(handle.fileno())
    os.replace(temporary, args.output)
    print(args.output, len(payload), 'bytes', total, 'pages')


if __name__ == '__main__':
    main()
