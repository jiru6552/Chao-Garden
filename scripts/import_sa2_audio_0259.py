#!/usr/bin/env python3
"""Convert local SA2 PC ADX files into a Minecraft 1.21.1 resource pack."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile

AREA_FILES = {
    'lobby': 'chao_hall.adx',
    'neutral': 'chao_g_bgm_n.adx',
    'hero': 'chao_g_bgm_h.adx',
    'dark': 'chao_g_bgm_d.adx',
    'kindergarten': 'chao_k_m.adx',
}
CHORUS_FILES = tuple(f'chao_k_sing_{side}{index}.adx'
                    for side in ('hero', 'dark') for index in (1, 2, 3))
REQUIRED = ('lobby', 'hero', 'dark', 'kindergarten')
REFERENCE = 'https://github.com/X-Hax/SA2BModdingGuide/wiki/ADX-Music-List'

def locate(source):
    source = Path(source).resolve()
    if not source.is_dir():
        raise ValueError('Choose your Sonic Adventure 2 installation or its ADX folder.')
    candidates = [source, source/'resource/gd_PC/ADX', source/'resource/gd_PC/adx']
    for folder in candidates:
        if folder.is_dir() and any(p.suffix.lower() == '.adx' for p in folder.iterdir()):
            result = {}
            for path in folder.iterdir():
                if path.is_file() and path.suffix.lower() == '.adx':
                    key = path.name.casefold()
                    if key in result:
                        raise ValueError('Ambiguous case-duplicate input: ' + path.name)
                    result[key] = path
            return result
    raise ValueError('No ADX files found. Select resource/gd_PC/ADX in your SA2 installation.')

def probe(path, ffprobe):
    run = subprocess.run([ffprobe, '-v', 'error', '-show_entries',
        'stream=codec_name,channels:format=duration', '-of', 'json', str(path)],
        check=True, capture_output=True, text=True, timeout=60)
    data = json.loads(run.stdout)
    duration = float(data.get('format', {}).get('duration', 0))
    streams = data.get('streams', [])
    if len(streams) != 1 or streams[0].get('codec_name') != 'vorbis' or not 0 < duration <= 1800:
        raise ValueError('Invalid, empty or excessive converted Vorbis audio: ' + path.name)
    return duration, streams[0]['channels']

def build(source, target, allow_partial=False, ffmpeg='ffmpeg', ffprobe='ffprobe'):
    if not shutil.which(ffmpeg) or not shutil.which(ffprobe):
        raise ValueError('Install FFmpeg (including ffprobe), then run this command again.')
    inputs = locate(source)
    missing = [area for area in REQUIRED if AREA_FILES[area] not in inputs]
    if missing and not allow_partial:
        raise ValueError('Missing requested area tracks: ' + ', '.join(missing) +
                         '. Use --allow-partial only if you want a partial pack.')
    areas = {area: inputs[name] for area, name in AREA_FILES.items() if name in inputs}
    chorus = {Path(name).stem: inputs[name] for name in CHORUS_FILES if name in inputs}
    if not areas and not chorus:
        raise ValueError('No recognized Chao area or classroom singing files found.')
    target = Path(target).resolve()
    if target in (p.resolve() for p in inputs.values()):
        raise ValueError('Output must not replace a game audio input.')
    target.parent.mkdir(parents=True, exist_ok=True)
    sounds, records = {}, []
    with tempfile.TemporaryDirectory(prefix='chao-sa2-import-', dir=target.parent) as temporary:
        staging = Path(temporary)
        for kind, rows in (('area', areas), ('chorus', chorus)):
            for name, original in rows.items():
                rel = 'music/area/' + name if kind == 'area' else 'chao/chorus/' + name
                audio = staging/'assets/chao_garden/sounds'/Path(rel + '.ogg')
                audio.parent.mkdir(parents=True, exist_ok=True)
                subprocess.run([ffmpeg, '-nostdin', '-v', 'error', '-y', '-i', str(original),
                    '-map', '0:a:0', '-vn', '-map_metadata', '-1', '-ac', '2' if kind == 'area' else '1',
                    '-ar', '44100', '-c:a', 'libvorbis', '-q:a', '5', str(audio)],
                    check=True, capture_output=True, timeout=180)
                duration, channels = probe(audio, ffprobe)
                records.append(dict(kind=kind, id=name, source_file=original.name,
                    source_sha256=hashlib.sha256(original.read_bytes()).hexdigest(),
                    asset='assets/chao_garden/sounds/' + rel + '.ogg',
                    sha256=hashlib.sha256(audio.read_bytes()).hexdigest(),
                    duration_seconds=duration, channels=channels))
                entry = dict(name='chao_garden:' + rel, stream=True)
                if kind == 'area':
                    sounds['music.area.' + name] = dict(replace=True, sounds=[entry])
                else:
                    sounds.setdefault('chao.classroom_chorus', dict(replace=True, sounds=[]))['sounds'].append(entry)
        report = dict(format=1, source='local SA2 installation', reference=REFERENCE,
                      imported_areas=list(areas), missing_requested_areas=missing,
                      classroom_chorus_count=len(chorus),
                      learned_solo_songs_imported=False, black_market_imported=False, recordings=records)
        (staging/'pack.mcmeta').write_text(json.dumps({'pack': {'pack_format': 34,
             'description': 'Chao Garden: locally imported SA2 area themes and classroom chorus'}}) + '\n')
        (staging/'assets/chao_garden/sounds.json').write_text(json.dumps(sounds, indent=2) + '\n')
        (staging/'sa2-import-report.json').write_text(json.dumps(report, indent=2) + '\n')
        archive = staging/'pack.zip'
        with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
            for path in sorted(staging.rglob('*')):
                if path.is_file() and path != archive:
                    z.write(path, path.relative_to(staging).as_posix())
        with zipfile.ZipFile(archive) as z:
            if z.testzip() is not None:
                raise ValueError('Resource pack integrity check failed.')
        os.replace(archive, target)
    return dict(file=str(target), **{k:report[k] for k in
        ('imported_areas', 'missing_requested_areas', 'classroom_chorus_count',
         'learned_solo_songs_imported', 'black_market_imported')})

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('source', type=Path, help='SA2 installation folder or its resource/gd_PC/ADX folder')
    p.add_argument('output', type=Path, help='Output .zip resource pack')
    p.add_argument('--allow-partial', action='store_true')
    a = p.parse_args()
    try: print(json.dumps(build(a.source, a.output, a.allow_partial), indent=2))
    except (ValueError, subprocess.SubprocessError, OSError) as error: p.exit(1, str(error) + '\n')

if __name__ == '__main__': main()
