#!/usr/bin/env python3
"""Build the 0.12 preview PDF from validated actual Minecraft frames.

Requires Pillow, ReportLab and pypdf. The PNG capture directory and authored
sprite sheet are kept separate; every frame is included once without cropping.
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
    'rabbit-closeup', 'rabbit-profile', 'animal-detail',
    'mixed-child-adult', 'mixed-angle', 'gene-progression', 'held-mixed',
    'inventory', 'gene-tooltip', 'drive-tooltip',
    'animal-guide', 'journal-icons',
    'orchard', 'foliage-fruit', 'triangle-flags', 'flags-angle',
)
INK, MUTED, PAPER, EDGE = map(HexColor, ('#173f4b', '#47636e', '#edf3f5', '#afc8cf'))


def clean(value):
    for before, after in (('×', 'x'), ('’', "'"), ('‘', "'"), ('—', '-'), ('–', '-')):
        value = value.replace(before, after)
    return value


def wrap(value, width, size):
    lines, line = [], ''
    for word in clean(value).split():
        part = (line + ' ' + word).strip()
        if line and pdfmetrics.stringWidth(part, 'Gallery', size) > width:
            lines.append(line)
            line = word
        else:
            line = part
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
    if size < 16:
        raise ValueError('Title is too long: ' + title)
    pdf.setFont('GalleryBold', size)
    pdf.drawString(24, height - 31, clean(title))
    pdf.setFillColor(MUTED)
    pdf.setFont('Gallery', 10)
    assert pdfmetrics.stringWidth(clean(subtitle), 'Gallery', 10) <= width - 48
    pdf.drawString(24, height - 49, clean(subtitle))
    pdf.setFont('Gallery', 8)
    pdf.drawRightString(width - 24, 15, f'{page:02d} / {total:02d}')
    pdf.drawString(24, 15, 'CHAO GARDEN 0.12.0  |  Minecraft Java 1.21.1 / NeoForge')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path,
                        default=ROOT.parent / 'output/pdf/chao-garden-0.12.0-preview-gallery.pdf')
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location('chao_package_012', ROOT / 'scripts/package_chao012.py')
    art = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(art)
    assert len(ORDER) == len(set(ORDER)) == 16 and set(ORDER) == set(art.SHOTS)
    for name, path in (('Gallery', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),
                       ('GalleryBold', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')):
        pdfmetrics.registerFont(TTFont(name, path))
    shots = ROOT / 'docs/screenshots/refinement-0.12'
    total = len(ORDER) + 1
    buffer = io.BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=(842, 595), pageCompression=1)
    pdf.setTitle('Chao Garden 0.12.0 - Preview Gallery')
    pdf.setAuthor('Chao Garden')
    pdf.setSubject('Sixteen actual Minecraft views and the authored item sprite sheet')
    for index, key in enumerate(ORDER, 1):
        title, subtitle, caption = art.SHOTS[key]
        path = shots / f'chao012-{key}.png'
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
        pdf.setFillColor(MUTED)
        pdf.setFont('Gallery', 9.5)
        lines = wrap(caption, 794, 9.5)
        assert len(lines) <= 3, (key, lines)
        for row, line in enumerate(lines):
            pdf.drawString(24, 57 - row * 13, line)
        pdf.bookmarkPage(key)
        pdf.addOutlineEntry(clean(title), key, level=0, closed=False)
        pdf.showPage()
    pdf.setPageSize((595, 842))
    header(pdf, 595, 842, 'Item sprite reference',
           'Authored pixel art - not a Minecraft screenshot', total, total)
    path = ROOT / 'docs/screenshots/item-sprites-0.12.png'
    with Image.open(path) as image:
        image.load()
        source_width, source_height = image.size
    scale = min(547 / source_width, 718 / source_height)
    width, height = source_width * scale, source_height * scale
    pdf.drawImage(ImageReader(str(path)), (595 - width) / 2,
                  70 + (718 - height) / 2, width=width, height=height, mask='auto')
    pdf.setFillColor(MUTED)
    pdf.setFont('Gallery', 9)
    pdf.drawString(24, 48, 'Upright drives, rounded egg crowns and simpler DNA vials.')
    pdf.bookmarkPage('sprites')
    pdf.addOutlineEntry('Item sprite reference', 'sprites', level=0, closed=False)
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
