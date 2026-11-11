#!/usr/bin/env python3
"""Original DNA-vial items and additive, namespace-independent chest training loot."""
from pathlib import Path
import io
import json
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSET = RES / 'assets/chao_garden'
NAMES = ['penguin','seal','otter','peacock','parrot','condor','rabbit','cheetah',
         'warthog','bear','tiger','gorilla','skunk','sheep','raccoon','dragon',
         'unicorn','phoenix','half_fish','skeleton_dog','bat']
COLORS = ['4fa9e5','5bd2e0','cf8d52','28c4a0','ec5f80','9f8ee5','f0a8cf',
          'eac24e','b86d43','c68f5a','f08a37','8ca2ba','b77cdb','e8dd9d',
          'b7aa92','aa69f0','e9c65b','f36838','60dfbd','bbdca8','8650b8']


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + '.writing')
    with temporary.open('w') as handle:
        json.dump(value, handle, indent=2)
        handle.write('\n'); handle.flush(); os.fsync(handle.fileno())
    os.replace(temporary, path)


def save_png(picture, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = io.BytesIO(); picture.save(data, format='PNG')
    with Image.open(io.BytesIO(data.getvalue())) as check: check.verify()
    temporary = path.with_name(path.name + '.writing')
    with temporary.open('wb') as handle:
        handle.write(data.getvalue()); handle.flush(); os.fsync(handle.fileno())
    os.replace(temporary, path)


def vial(index):
    color = tuple(bytes.fromhex(COLORS[index]))
    def mix(first, second, amount):
        return tuple(round(a*(1-amount)+b*amount) for a,b in zip(first,second))+(255,)
    bright = mix(color, (244,255,246), .58)
    liquid = mix(color, (22,48,64), .58)
    picture = Image.new('RGBA', (32,32), (0,0,0,0)); d = ImageDraw.Draw(picture)
    # The broad glass rim and two clear DNA strands carry the design. Avoid the
    # previous tiny cap notches, nested highlights and several competing glints.
    d.polygon([(10,6),(21,6),(23,8),(23,25),(21,28),(19,30),
               (12,30),(9,27),(8,24),(8,8)], fill='#294451')
    d.polygon([(10,7),(20,7),(22,9),(22,24),(20,27),(18,28),
               (13,28),(10,25),(9,23),(9,9)], fill='#8fbac1')
    # One colored chamber, with a light meniscus rather than several inner rims.
    d.polygon([(11,11),(20,11),(21,12),(21,24),(19,27),
               (13,27),(11,24)], fill=liquid)
    d.line((12,11,19,11), fill=bright)
    # Two broad opposing curves and two links keep the DNA readable without
    # filling the entire vial with high-frequency crossing pixels.
    left = [(12,13),(13,15),(18,19),(19,21),(17,24)]
    right = [(19,13),(18,15),(13,19),(12,21),(14,24)]
    for y, a, b in ((13,12,19),(21,12,19)):
        d.line((a,y,b,y), fill=mix(color,(242,255,250),.18), width=1)
    d.line(right, fill=bright, width=2)
    d.line(left, fill=mix(color,(255,248,210),.76), width=2)
    # One short reflection and the lower glass rim are enough to suggest glass.
    d.line((9,10,9,18), fill='#effcfa', width=1)
    # A silver crimp cap with a sample-color seal and dark lower lip.
    d.rectangle((7,2,24,6), fill='#2b3e4a')
    d.rectangle((8,2,23,3), fill='#c9d4d3')
    d.rectangle((9,4,22,5), fill=color+(255,))
    return picture


def canonicalize_loot():
    """New drops use canonical samples; old inventory IDs remain registered."""
    aliases = {'chao_garden:'+name+'_animal':'chao_garden:'+name+'_animal_gene'
               for name in NAMES}
    def update(value):
        if isinstance(value, str): return aliases.get(value, value)
        if isinstance(value, list): return [update(child) for child in value]
        if isinstance(value, dict): return {key:update(child) for key,child in value.items()}
        return value
    for directory in ('loot_table','loot_modifiers'):
        for path in (RES/'data/chao_garden'/directory).rglob('*.json'):
            before = json.loads(path.read_text()); after = update(before)
            if before != after: write_json(path, after)


def write_preview(thumbnails):
    """Refresh the shareable gene index independently of loot/data generation."""
    sheet = Image.new('RGB', (840,426), '#edf3f5'); d = ImageDraw.Draw(sheet)
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 15)
    for index, image in enumerate(thumbnails):
        x=(index%7)*120; y=(index//7)*142
        sprite=image.resize((96,96),Image.Resampling.NEAREST)
        sheet.paste(sprite,(x+12,y+8),sprite)
        label='Boar' if NAMES[index]=='warthog' else NAMES[index].replace('_',' ').title()
        d.text((x+60,y+123),label,fill='#173f4b',font=font,anchor='mm')
    save_png(sheet, ROOT/'docs/screenshots/animal-gene-items.png')


def main():
    lang_path = ASSET / 'lang/en_us.json'
    language = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    thumbnails = []
    for index, name in enumerate(NAMES):
        image = vial(index); thumbnails.append(image)
        for ident in (name+'_animal_gene', name+'_animal'):
            save_png(image, ASSET / 'textures/item' / (ident+'.png'))
            write_json(ASSET / 'models/item' / (ident+'.json'), {
                'parent':'minecraft:item/generated',
                'textures':{'layer0':'chao_garden:item/'+ident}})
            title = 'Boar' if name == 'warthog' else name.replace('_',' ').title()
            language['item.chao_garden.'+ident] = title+' Animal Genes'
    write_json(lang_path, language)
    write_json(RES/'data/chao_garden/tags/item/animal_genes.json', {
        'replace':False, 'values':['chao_garden:'+name+suffix for name in NAMES
                                  for suffix in ('_animal_gene','_animal')]})
    write_json(RES/'data/chao_garden/loot_modifiers/generic_chest_training.json', {
        'type':'chao_garden:generic_chest_training', 'conditions':[],
        'drive_chance':.40, 'gene_chance':.12})
    global_path = RES/'data/neoforge/loot_modifiers/global_loot_modifiers.json'
    modifiers = json.loads(global_path.read_text()) if global_path.exists() else {'replace':False,'entries':[]}
    value = 'chao_garden:generic_chest_training'
    if value not in modifiers['entries']:modifiers['entries'].append(value)
    write_json(global_path, modifiers)
    canonicalize_loot()
    # Original, shareable inventory-scale preview; no retrieved images used.
    write_preview(thumbnails)
    print('Generated 21 colored DNA vials, 21 compatible legacy item appearances, benefit names, tags and generic chest loot.')


if __name__ == '__main__':
    main()
