#!/usr/bin/env python3
"""Build the metadata-selected PDF from validated whole Minecraft captures.

Run package_chao018.py --previews-only first. The release owner must render
and inspect every resulting page before publishing. The PDF operation marker
is managed once per user request, before PDF authoring begins.
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
VERSION = '0.18.0'
INK, MUTED, PAPER, EDGE = map(HexColor, ('#173f4b', '#47636e', '#edf3f5', '#afc8cf'))


def clean(value):
    for old, new in (('×', 'x'), ('’', "'"), ('‘', "'"), ('—', '-'), ('–', '-'), ('‑', '-')):
        value = value.replace(old, new)
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
    pdf.rect(0, 0, width, height, fill=1, stroke=0)
    title, subtitle = clean(title), clean(subtitle)
    size = 22
    while pdfmetrics.stringWidth(title, 'GalleryBold', size) > width - 48:
        size -= .5
    if size < 16:
        raise ValueError('PDF title is too long: ' + title)
    pdf.setFillColor(INK)
    pdf.setFont('GalleryBold', size)
    pdf.drawString(24, height - 31, title)
    subtitle_size = 10
    while pdfmetrics.stringWidth(subtitle, 'Gallery', subtitle_size) > width - 48:
        subtitle_size -= .25
    if subtitle_size < 9:
        raise ValueError('PDF subtitle is too long: ' + subtitle)
    pdf.setFillColor(MUTED)
    pdf.setFont('Gallery', subtitle_size)
    pdf.drawString(24, height - 49, subtitle)
    pdf.setFont('Gallery', 8)
    pdf.drawString(24, 15, f'CHAO GARDEN {VERSION}  |  Minecraft Java 1.21.1 / NeoForge')
    pdf.drawRightString(width - 24, 15, f'{page:02d} / {total:02d}')


def main(output=None, plan_path=None):
    spec = importlib.util.spec_from_file_location('chao_package_018', ROOT / 'scripts/package_chao018.py')
    art = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(art)
    plan = art.load_plan(ROOT, plan_path)
    manifest = json.loads((ROOT / f'docs/release-capture-manifest-{VERSION}.json').read_text())
    if manifest['release'] != VERSION or manifest['scene_plan'] != plan:
        raise ValueError('The preview manifest does not match the current scene plan')
    expected = {item['key']: item for item in manifest['captures']}
    features = {item['key']: item for item in plan['features']}
    pages = [features[key] for key in plan['pdf']]
    directory = ROOT / f'docs/screenshots/release-{VERSION}'
    for name, path in (
        ('Gallery', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),
        ('GalleryBold', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'),
    ):
        pdfmetrics.registerFont(TTFont(name, path))
    buffer = io.BytesIO()
    pdf = canvas.Canvas(buffer, pagesize=(842, 595), pageCompression=1)
    pdf.setTitle('Chao Garden 0.18.0 - Official Models, Gifts and Garden Life')
    pdf.setAuthor('Chao Garden')
    pdf.setSubject('Actual Minecraft comparisons and behavior captures selected from the release scene plan')
    source_pages = []
    for number, feature in enumerate(pages, 1):
        key = feature['key']
        capture = expected[key]
        path = directory / capture['file']
        if art.sha256(path) != capture['sha256']:
            raise ValueError('Source capture changed: ' + key)
        header(pdf, f'{number:02d}  ' + feature.get('pdf_title', feature['title']),
               feature.get('pdf_subtitle', feature['subtitle']), number, len(pages))
        with Image.open(path) as picture:
            picture.verify()
            source_width, source_height = picture.size
        # Fit the complete frame; portraits and UI studies never crop to fill.
        box_x, box_y, box_width, box_height = 24, 77, 794, 446.625
        scale = min(box_width / source_width, box_height / source_height)
        image_width, image_height = source_width * scale, source_height * scale
        x = box_x + (box_width - image_width) / 2
        y = box_y + (box_height - image_height) / 2
        pdf.setStrokeColor(EDGE)
        pdf.setLineWidth(.6)
        pdf.rect(x - .3, y - .3, image_width + .6, image_height + .6, fill=0, stroke=1)
        pdf.drawImage(ImageReader(str(path)), x, y, width=image_width, height=image_height, mask='auto')
        caption = feature.get('pdf_caption', feature['caption'])
        size = 9.5
        lines = wrap(caption, 794, size)
        while len(lines) > 3 and size > 9:
            size -= .25
            lines = wrap(caption, 794, size)
        if len(lines) > 3:
            raise ValueError('PDF caption needs shortening: ' + key)
        pdf.setFillColor(MUTED)
        pdf.setFont('Gallery', size)
        for row, line in enumerate(lines):
            pdf.drawString(24, 57 - row * 13, line)
        pdf.bookmarkPage(key)
        pdf.addOutlineEntry(clean(feature.get('pdf_title', feature['title'])), key, level=0, closed=False)
        pdf.showPage()
        source_pages.append(dict(page=number, key=key, capture=capture['file'], sha256=capture['sha256']))
    pdf.save()
    payload = buffer.getvalue()
    reader = PdfReader(io.BytesIO(payload))
    if len(reader.pages) != len(pages) or len(reader.outline) != len(pages):
        raise ValueError('PDF page count or navigation differs from the scene plan')
    if not all(page.extract_text().strip() and len(page.images) >= 1 for page in reader.pages):
        raise ValueError('A PDF page lacks text or its actual capture')
    target = Path(output) if output else ROOT.parent / f'output/pdf/chao-garden-{VERSION}-preview-gallery.pdf'
    art.write_flushed(target, payload)
    art.write_flushed(ROOT / f'docs/chao-garden-{VERSION}-preview-gallery.pdf', payload)
    provenance = dict(release=VERSION, pdf=target.name, sha256=art.sha256(target), page_count=len(pages), pages=source_pages)
    art.write_flushed(ROOT / f'docs/pdf-source-manifest-{VERSION}.json', (json.dumps(provenance, indent=2) + '\n').encode())
    print(target, len(payload), 'bytes;', len(pages), 'pages; source copy and provenance written')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--plan', type=Path)
    args = parser.parse_args()
    main(args.output, args.plan)
