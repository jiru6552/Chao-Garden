#!/usr/bin/env python3
"""Freeze the complete 0.24 actual-client study into an offline image gallery."""
from pathlib import Path
import argparse, base64, hashlib, html, json, re, shutil
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT/'run/screenshots')
    parser.add_argument('--log', type=Path, default=ROOT/'run/logs/latest.log')
    args = parser.parse_args()
    args.source=args.source.resolve();args.log=args.log.resolve()
    plan = json.loads((ROOT/'docs/release-scenes-0.24.0.json').read_text())
    log = args.log.read_text(errors='replace')
    expected = len(plan['features'])
    if f'CHAO_HUB024_COMPLETE {expected} captures / {expected} saves' not in log:
        raise RuntimeError('The actual integrated study has not completed all captures and saves')
    events=re.findall(r'CHAO_HUB024_CAPTURE (chao024-[^\s]+\.png) stage=(\d+) ',log)
    if len(events)!=expected or {int(stage) for _,stage in events}!=set(range(expected)):
        raise RuntimeError('Capture events do not form one complete coherent study')
    saved=dict(re.findall(r'CHAO_HUB024_SAVED (chao024-[^\s]+\.png) sha256=([a-f0-9]{64})',log))
    if len(saved)!=expected:
        raise RuntimeError('Missing exact saved-file digest acknowledgements')
    destination = ROOT/'docs/screenshots-0.24'
    destination.mkdir(parents=True, exist_ok=True)
    cards, records = [], []
    for index, scene in enumerate(plan['features']):
        name = plan['filename_prefix']+scene['key']+'.png'
        source = args.source/name
        with Image.open(source) as image:
            image.load()
            if image.size != (1280,720):
                raise RuntimeError(f'Unexpected actual framebuffer size: {name}: {image.size}')
        target = ROOT/'docs'/scene['file']
        if sha(source)!=saved[name]:
            raise RuntimeError('Current frame does not match its actual save acknowledgement: '+name)
        shutil.copy2(source, target)
        line = next((line for line in log.splitlines() if f'CHAO_HUB024_CAPTURE {name} stage={index} ' in line),None)
        if line is None:
            raise RuntimeError('Missing matching capture event: '+name)
        records.append(dict(key=scene['key'],file=scene['file'],sha256=sha(target),capture_event=line))
        encoded = base64.b64encode(target.read_bytes()).decode('ascii')
        caption = html.escape(scene['caption'])
        cards.append(f'<figure id="{html.escape(scene["key"])}"><a href="#top" aria-label="Back to contents"><img loading="lazy" width="1280" height="720" src="data:image/png;base64,{encoded}" alt="{caption}"></a><figcaption><b>{index+1:02d}. {html.escape(scene["key"].replace("-"," "))}</b><br>{caption}</figcaption></figure>')
    navigation = ''.join(f'<a href="#{html.escape(scene["key"])}">{index+1:02d}</a>' for index,scene in enumerate(plan['features']))
    page = '''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Chao Garden 0.24.0 — actual game views</title><style>
    :root{color-scheme:dark}*{box-sizing:border-box}body{margin:0;background:#101820;color:#edf4f6;font:16px/1.55 system-ui,sans-serif}header,main{max-width:1280px;margin:auto;padding:28px}h1{font-size:clamp(25px,4vw,42px);line-height:1.15;margin:0 0 16px}p{max-width:960px;color:#c3d4da}nav{display:flex;gap:7px;flex-wrap:wrap}nav a{padding:5px 10px;background:#263a46;color:#bfe8f2;border-radius:6px;text-decoration:none}figure{margin:0 0 36px;scroll-margin-top:14px;background:#1c2a33;border:1px solid #34505d;border-radius:10px;overflow:hidden}img{display:block;width:100%;height:auto;aspect-ratio:16/9}figcaption{padding:16px 20px;color:#c3d4da}b{color:#fff}footer{padding:0 28px 40px;max-width:1280px;margin:auto;color:#a9c3cd}a{color:#bfe8f2}</style>
    <header id="top"><h1>Chao Garden 0.24.0</h1><p>Actual Minecraft Java 1.21.1 game views: the Chao lobby and Kindergarten, resident services, item choices, birth-shell hats and timed elytra flight. These are complete framebuffer captures from the opt-in development study, with controlled cameras and test actors. Travel, gift transactions, service actions and flight use the mod's normal code.</p><p>The pictures are embedded in this file, so the gallery also works offline. Select a number to jump to a view; select a picture to return here.</p><nav>'''+navigation+'</nav></header><main>'+''.join(cards)+'</main><footer>Hub scenery follows the neutral SA2 lobby. The other garden doors are decorative placeholders in this release. The hub and Kindergarten use vanilla decorative blocks.</footer></html>'
    gallery = ROOT/'docs/release-gallery-0.24.0.html'
    gallery.write_text(page)
    manifest = dict(release='0.24.0',result='CAPTURED',capture_count=expected,log=dict(file=str(args.log.relative_to(ROOT)),sha256=sha(args.log)),
                    gallery=dict(file=gallery.relative_to(ROOT).as_posix(),sha256=sha(gallery),self_contained=True),captures=records)
    manifest_path=ROOT/'docs/release-capture-manifest-0.24.0.json'
    manifest_path.write_text(json.dumps(manifest,indent=2)+'\n')
    embedded=re.findall(r'src="data:image/png;base64,([^"]+)"',page)
    if len(embedded)!=expected or '<script' in page or re.search(r'(?:src|href)="https?://',page):
        raise RuntimeError('Gallery lost its self-contained native HTML structure')
    for scene,encoded in zip(records,embedded):
        if hashlib.sha256(base64.b64decode(encoded,validate=True)).hexdigest()!=scene['sha256']:
            raise RuntimeError('Embedded image differs from its actual capture: '+scene['key'])
    ids=set(re.findall(r'id="([^"]+)"',page))
    if len(ids)!=expected+1 or any(target not in ids for target in re.findall(r'href="#([^"]+)"',page)):
        raise RuntimeError('Gallery navigation contains an unresolved or duplicate anchor')
    report=ROOT/'docs/validation-0.24/gallery.json'
    report.write_text(json.dumps(dict(release='0.24.0',result='PASS',embedded_actual_frames=expected,local_image_dependencies=0,
        network_dependencies=0,native_navigation_anchors=len(ids),method='Static HTML structure, native anchor resolution, full embedded PNG decoding and exact actual-save SHA256 comparison. Full framebuffer appearance is reviewed separately; this is not a graphical-browser layout test.',
        sources=[dict(file=p.relative_to(ROOT).as_posix(),sha256=sha(p)) for p in (gallery,manifest_path,ROOT/'docs/release-scenes-0.24.0.json')]),indent=2)+'\n')
    print(f'{expected} actual captures; self-contained gallery {gallery.stat().st_size:,} bytes')

if __name__ == '__main__':
    main()
