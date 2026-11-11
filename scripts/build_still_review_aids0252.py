#!/usr/bin/env python3
"""Build hash-bound current-still contact sheets, without a visual judgment."""
from pathlib import Path
import hashlib
import math
import tempfile
import textwrap
from PIL import Image, ImageDraw, ImageFont
from publish_release025 import publish
from capture_ledger0252 import capture_ledger_sha256
from review_ledger0252 import assert_frozen, publish_json, read_snapshot

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
PROOF = DOCS / 'validation-0.25.2'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    manifest_path = DOCS / 'release-capture-manifest-0.25.2.json'
    manifest, manifest_sha = read_snapshot(manifest_path)
    assert manifest['release'] == '0.25.2'
    rows = manifest['captures']
    assert len(rows) == len({row['file'] for row in rows})
    snapshots = {manifest_path: manifest_sha}
    native = []
    for row in rows:
        path = DOCS / row['file']
        assert path.resolve().is_relative_to(DOCS.resolve())
        assert sha(path) == row['sha256']
        with Image.open(path) as image:
            assert image.size == (1280, 720)
            image.load()
        snapshots[path] = row['sha256']
        native.append((row, path))
    output = PROOF / 'still-review'
    output.mkdir(parents=True, exist_ok=True)
    font_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    font = ImageFont.truetype(font_path, 13)
    title_font = ImageFont.truetype(font_path, 18)
    tile_w, tile_h, label_h, columns, per_page = 426, 240, 64, 3, 6
    sheets, indexed = [], []
    with tempfile.TemporaryDirectory(prefix='chao0252-still-review-') as temporary:
        for page in range(math.ceil(len(native) / per_page)):
            selected = native[page * per_page:(page + 1) * per_page]
            canvas = Image.new('RGB', (columns * tile_w, 46 + 2 * (tile_h + label_h)), '#101a23')
            draw = ImageDraw.Draw(canvas)
            draw.text((12, 10), f'0.25.2 current native stills — review aid {page + 1}', font=title_font, fill='white')
            for index, (row, path) in enumerate(selected):
                col, line = index % columns, index // columns
                x, y = col * tile_w, 46 + line * (tile_h + label_h)
                with Image.open(path) as image:
                    thumb = image.convert('RGB').resize((tile_w, tile_h), Image.Resampling.LANCZOS)
                canvas.paste(thumb, (x, y))
                label = f"{page * per_page + index + 1:03d} {row['key']} / {row['phase']}"
                draw.multiline_text((x + 5, y + tile_h + 4), '\n'.join(textwrap.wrap(label, 53)),
                                    font=font, fill='white', spacing=2)
                draw.text((x + 5, y + tile_h + 45), 'SHA256 ' + row['sha256'][:16] + '…', font=font, fill='#9edcea')
                indexed.append(dict(file=path.relative_to(ROOT).as_posix(), sha256=row['sha256'],
                    key=row['key'], phase=row['phase'], sheet=f'contact-{page:02d}.jpg',
                    sheet_tile=index, sheet_native_number=page * per_page + index + 1,
                    displayed_size=[tile_w, tile_h], native_size=[1280, 720]))
            name = f'contact-{page:02d}.jpg'
            stage = Path(temporary) / name
            canvas.save(stage, 'JPEG', quality=94, subsampling=0)
            with Image.open(stage) as saved:
                saved.load()
            assert_frozen(snapshots)
            target = output / name
            publish(stage, target)
            snapshots[target] = sha(target)
            sheets.append(dict(file=target.relative_to(ROOT).as_posix(), sha256=snapshots[target], native_tiles=len(selected)))
    index = dict(release='0.25.2', status='REVIEW_AIDS_ONLY_NO_VISUAL_JUDGMENT',
        method='Each tile is the entire exact native 1280×720 PNG scaled to 426×240 for systematic still inspection. JPEG sheets are labelled review aids, never current native screenshots. Full-resolution reopens are required for small texture, face, stem and seam details. This generator performs no image classification or visual PASS.',
        capture_ledger_sha256=capture_ledger_sha256(manifest), native_still_count=len(indexed), sheets=sheets, captures=indexed)
    publish_json(index, output / 'index.json', ROOT, snapshots)
    assert_frozen(snapshots)
    print(f'{len(indexed)} exact current PNGs indexed in {len(sheets)} review-only contact sheets')


if __name__ == '__main__':
    main()
