#!/usr/bin/env python3
"""Build the Chao Garden CD's native sprite and 1.21.1 jukebox resources.

To rebuild its audio, pass --intro and --loop pointing to the owner's supplied
readable Sonic Marketplace garden recordings. A normal asset rebuild retains
the already included finite intro+one-loop Ogg track.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
TRACK = RES / 'assets/chao_garden/sounds/records/chao_garden.ogg'


def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2) + '\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--intro', type=Path)
    parser.add_argument('--loop', type=Path)
    args = parser.parse_args()
    assert bool(args.intro) == bool(args.loop), 'Pass both recordings together'
    sources = None
    if args.intro:
        assert args.intro.is_file() and args.loop.is_file()
        TRACK.parent.mkdir(parents=True, exist_ok=True)
        temporary = TRACK.with_name('chao_garden.writing.ogg')
        subprocess.run(['ffmpeg', '-nostdin', '-hide_banner', '-loglevel', 'error', '-y',
            '-i', str(args.intro), '-i', str(args.loop),
            '-filter_complex', '[0:a][1:a]concat=n=2:v=0:a=1[record]',
            '-map', '[record]', '-ar', '44100', '-ac', '2', '-c:a', 'libvorbis', '-q:a', '5',
            str(temporary)], check=True)
        os.replace(temporary, TRACK)
        sources = [{'file': p.name, 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()}
                   for p in (args.intro, args.loop)]
    assert TRACK.is_file(), 'Generate the record once with --intro and --loop'
    probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_entries',
        'format=duration:stream=codec_name,sample_rate,channels', '-of', 'json', str(TRACK)]))
    seconds = round(float(probe['format']['duration']), 6)
    assert 115 < seconds < 117
    assert probe['streams'][0]['codec_name'] == 'vorbis'
    write('data/chao_garden/jukebox_song/chao_garden.json', {
        'sound_event': 'chao_garden:music.chao_garden',
        'description': {'translate': 'jukebox_song.chao_garden.chao_garden'},
        'length_in_seconds': seconds, 'comparator_output': 7})
    write('assets/chao_garden/models/item/chao_garden_music_disc.json', {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': 'chao_garden:item/chao_garden_music_disc'}})
    write('data/chao_garden/recipe/chao_garden_music_disc.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['III', 'IEI', 'III'],
        'key': {'I': {'item': 'minecraft:iron_nugget'}, 'E': {'item': 'minecraft:emerald'}},
        'result': {'id': 'chao_garden:chao_garden_music_disc', 'count': 1}})
    write('data/chao_garden/advancement/recipes/misc/chao_garden_music_disc.json', {
        'parent': 'minecraft:recipes/root',
        'criteria': {'has_emerald': {'trigger': 'minecraft:inventory_changed',
            'conditions': {'items': [{'items': 'minecraft:emerald'}]}}},
        'requirements': [['has_emerald']],
        'rewards': {'recipes': ['chao_garden:chao_garden_music_disc']}})

    # Native sixteen-pixel vinyl-style Minecraft music disc. Broad dark grooves,
    # a small colored center label and a visible spindle hole make its purpose
    # readable in a hotbar without resembling a character medallion.
    image = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(image)
    d.polygon([(5,1),(10,1),(10,2),(12,2),(12,3),(13,3),(13,5),(14,5),
               (14,10),(13,10),(13,12),(12,12),(12,13),(10,13),(10,14),
               (5,14),(5,13),(3,13),(3,12),(2,12),(2,10),(1,10),(1,5),
               (2,5),(2,3),(3,3),(3,2),(5,2)], fill='#0d1118')
    d.ellipse((2,2,13,13), fill='#252b35')
    d.arc((2,2,13,13), 188, 278, fill='#4b5662', width=1)
    d.arc((3,3,12,12), 7, 96, fill='#10151e', width=1)
    d.arc((4,4,11,11), 155, 282, fill='#151c26', width=1)
    d.arc((3,3,12,12), 292, 338, fill='#39434f', width=1)
    d.ellipse((5,5,10,10), fill='#69c5d8')
    d.pieslice((5,5,10,10), 270, 90, fill='#f4d954')
    d.line((6,5,9,5), fill='#fff0a0', width=1)
    d.line((5,6,5,9), fill='#347d9d', width=1)
    d.line((6,10,9,10), fill='#347d9d', width=1)
    d.rectangle((7,7,8,8), fill=(0,0,0,0))
    target = RES / 'assets/chao_garden/textures/item/chao_garden_music_disc.png'
    target.parent.mkdir(parents=True, exist_ok=True); image.save(target)

    # The same fallback is used for an empty legacy held-Chao stack while an
    # old inventory is first being synchronized; it no longer draws a carrier.
    held = Image.new('RGBA', (16,16), (0,0,0,0)); h=ImageDraw.Draw(held)
    h.rectangle((3,5,12,11), fill='#76d6e6'); h.rectangle((5,11,10,13), fill='#76d6e6')
    h.rectangle((4,7,5,9), fill='#f4f8eb'); h.rectangle((10,7,11,9), fill='#f4f8eb')
    h.line((5,7,5,8), fill='#173943'); h.line((10,7,10,8), fill='#173943')
    h.rectangle((5,13,6,14), fill='#f2d15c'); h.rectangle((9,13,10,14), fill='#f2d15c')
    h.rectangle((7,1,8,2), fill='#f2d15c'); h.rectangle((7,4,9,4), fill='#76d6e6')
    held.save(RES/'assets/chao_garden/textures/item/held_chao.png')

    art = ROOT/'art'; art.mkdir(exist_ok=True)
    (art/'music_lang_fragment.json').write_text(json.dumps({
        'item.chao_garden.chao_carrier': 'Chao',
        'item.chao_garden.chao_garden_music_disc': 'Chao Garden CD',
        'jukebox_song.chao_garden.chao_garden': 'Chao Garden — Neutral Garden',
        'subtitles.chao_garden.music.chao_garden': 'Chao Garden music'}, indent=2)+'\n')
    (art/'music_sounds_fragment.json').write_text(json.dumps({'music.chao_garden': {
        'subtitle': 'subtitles.chao_garden.music.chao_garden',
        'sounds': [{'name': 'chao_garden:records/chao_garden', 'stream': True}]}}, indent=2)+'\n')
    manifest_path = art/'music_asset_manifest.json'
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else {}
    if sources: manifest['source_recordings'] = sources
    manifest.update({'arrangement': 'one intro followed by one complete loop',
        'duration_seconds': seconds, 'track_sha256': hashlib.sha256(TRACK.read_bytes()).hexdigest(),
        'codec': 'Vorbis', 'sample_rate': 44100, 'channels': 2, 'comparator_output': 7})
    manifest_path.write_text(json.dumps(manifest, indent=2)+'\n')
    print('Generated Chao Garden CD:', seconds, 'seconds; original 16px icon; vanilla jukebox component.')


if __name__ == '__main__': main()
