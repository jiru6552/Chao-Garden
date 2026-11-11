#!/usr/bin/env python3
"""Show native theme chest marks, tiny egg symbols and their real egg sprites."""
from pathlib import Path
import hashlib
import json
import sys
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'art'))
from variant_catalog import VARIANTS
from variant_icons import theme_icon
from item_sprite_art import themed_egg, save_sprite

HEADER, CELL_W, CELL_H, COLS = 112, 264, 176, 5

def main():
    sheet = Image.new('RGB', (COLS * CELL_W, HEADER + ((len(VARIANTS)+COLS-1)//COLS) * CELL_H + 48), '#edf3f5')
    draw = ImageDraw.Draw(sheet)
    font_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    def font(size): return ImageFont.truetype(font_path, size)
    draw.text((20, 15), 'Chao Garden 0.14.1 - Recognizable theme icons', font=font(30), fill='#173f4b')
    draw.text((20, 58), 'Original pixel art: chest badge (16px), native egg mark (8px), complete egg (16px).', font=font(19), fill='#47636e')
    draw.text((20, 84), 'Enlargements use nearest-neighbor; the small row in each tile shows actual native dimensions.', font=font(17), fill='#47636e')
    records = []
    for i, entry in enumerate(VARIANTS):
        x, y = i % COLS * CELL_W, HEADER + i // COLS * CELL_H
        draw.rounded_rectangle((x+5, y+3, x+CELL_W-5, y+CELL_H-5), 8, fill='#ffffff', outline='#c9dce0')
        words = entry['name'].split(); lines = []; line = ''
        for word in words:
            trial = (line+' '+word).strip()
            if line and draw.textlength(trial, font=font(16)) > CELL_W-24:
                lines.append(line); line=word
            else: line=trial
        if line: lines.append(line)
        assert len(lines) <= 2, entry['id']
        for j, text in enumerate(lines):
            draw.text((x+12, y+9+j*18), text, font=font(16), fill='#173f4b')
        images = [theme_icon(entry, 16), theme_icon(entry, 8), themed_egg(entry)]
        for im, ox, extent in zip(images, (18, 106, 180), (64, 48, 64)):
            enlarged = im.resize((extent, extent), Image.Resampling.NEAREST)
            sheet.paste(enlarged, (x+ox, y+52+(64-extent)//2), enlarged)
        for im, ox, label in zip(images, (40, 126, 204), ('16px', '8px', '16px')):
            sheet.paste(im, (x+ox, y+135+(16-im.height)//2), im)
            draw.text((x+ox+im.width/2, y+154), label, anchor='mt', font=font(11), fill='#64818a')
        records.append({'id':entry['id'], 'name':entry['name'],
            'native_8_sha256': hashlib.sha256(images[1].tobytes()).hexdigest(),
            'native_16_sha256': hashlib.sha256(images[0].tobytes()).hexdigest(),
            'egg_rgba_sha256': hashlib.sha256(images[2].tobytes()).hexdigest()})
    draw.text((20, sheet.height-31), 'Authored art reference - not a Minecraft screenshot. All 19 themes retain their existing availability requirements.', font=font(17), fill='#47636e')
    target = ROOT / 'docs/screenshots/theme-icons-0.14.1.png'
    save_sprite(sheet, target)
    (ROOT/'docs/theme-icon-manifest-0.14.1.json').write_text(json.dumps({'release':'0.14.1','theme_count':len(VARIANTS),'columns':COLS,'header_height':HEADER,'cell_height':CELL_H,'icons':records}, indent=2)+'\n')
    print(target, sheet.size)

if __name__ == '__main__': main()
