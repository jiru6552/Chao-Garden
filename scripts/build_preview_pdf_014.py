#!/usr/bin/env python3
"""Build the 0.14 gallery from six whole game frames and 19 authored theme tiles.

The native icon reference is regrouped into two readable pages (10 + 9 themes).
Requires Pillow, ReportLab and pypdf.
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
    'retained-eggs', 'neovitae-tooltip', 'tech-icons',
    'magic-icons', 'magic-items', 'dimensions',
)
INK, MUTED, PAPER, EDGE = map(HexColor, ('#173f4b', '#47636e', '#edf3f5', '#afc8cf'))
THEME_COLS, THEME_ROWS = 5, 4
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
    pdf.drawString(24, 15, 'CHAO GARDEN 0.14.0  |  Minecraft Java 1.21.1 / NeoForge')


def caption(pdf, text, width=794, y=57, size=9.5, max_lines=3):
    pdf.setFillColor(MUTED)
    pdf.setFont('Gallery', size)
    lines = wrap(text, width, size)
    assert len(lines) <= max_lines, lines
    for row, line in enumerate(lines):
        pdf.drawString(24, y - row * 13, line)


def theme_panels(path):
    """Retain all nineteen complete authored tiles, each exactly once."""
    with Image.open(path) as source:
        source.load()
        expected = (THEME_COLS * TILE_W, THEME_HEADER + THEME_ROWS * TILE_H + THEME_FOOTER)
        assert source.size == expected, (source.size, expected)
        tiles = []
        for index in range(19):
            x = index % THEME_COLS * TILE_W
            y = THEME_HEADER + index // THEME_COLS * TILE_H
            tiles.append(source.crop((x, y, x + TILE_W, y + TILE_H)).convert('RGB'))
    for start, count in ((0, 10), (10, 9)):
        panel = Image.new('RGB', (TILE_W * 3, TILE_H * 4), '#edf3f5')
        for local, tile in enumerate(tiles[start:start + count]):
            panel.paste(tile, ((local % 3) * TILE_W, (local // 3) * TILE_H))
        yield start, count, panel


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path,
                        default=ROOT.parent / 'output/pdf/chao-garden-0.14.0-preview-gallery.pdf')
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location('chao_package_014', ROOT / 'scripts/package_chao014.py')
    art = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(art)
    assert len(ORDER) == len(set(ORDER)) == 6 and set(ORDER) == set(art.SHOTS)
    for name, path in (('Gallery', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),
                       ('GalleryBold', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')):
        pdfmetrics.registerFont(TTFont(name, path))
    shots = ROOT / 'docs/screenshots/collection-0.14'
    total = 8
    buffer = io.BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=(842, 595), pageCompression=1)
    pdf.setTitle('Chao Garden 0.14.0 - Preview Gallery')
    pdf.setAuthor('Chao Garden')
    pdf.setSubject('Six actual Minecraft frames and all 19 retained authored theme icons')
    for index, key in enumerate(ORDER, 1):
        title, subtitle, text = art.SHOTS[key]
        text += ' Shown with development identity fixtures; these views do not test complete third-party mods.'
        path = shots / f'chao014-{key}.png'
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

    source = ROOT / 'docs/screenshots/theme-icons-0.14.png'
    for page, (start, count, image) in enumerate(theme_panels(source), 7):
        end = start + count
        title = f'{page:02d}  Retained theme reference - {start + 1:02d} to {end:02d}'
        pdf.setPageSize((595, 842))
        header(pdf, 595, 842, title,
               'Authored art - 16px badges, native 8px symbols and complete eggs', page, total)
        width = 547
        height = width * image.height / image.width
        assert height < 842 - 160
        pdf.drawImage(ImageReader(image), 24, 220, width=width, height=height, mask='auto')
        caption(pdf, 'Every retained theme appears once across these two pages. Nether and End use full egg surfaces; the chest badges match their appearance.',
                width=547, y=184, size=9, max_lines=3)
        caption(pdf, 'Seventeen mod-dependent themes plus Nether and End. Twenty-nine requested themes have been retired from the obtainable collection.',
                width=547, y=126, size=9, max_lines=3)
        key = 'theme-icons-' + str(start + 1)
        pdf.bookmarkPage(key)
        pdf.addOutlineEntry(f'Retained themes {start + 1:02d}-{end:02d}', key, level=0, closed=False)
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
