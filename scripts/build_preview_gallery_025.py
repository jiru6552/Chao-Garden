#!/usr/bin/env python3
"""Bind current actual-client study captures to receipts and an offline reference gallery."""
from pathlib import Path
import argparse,base64,hashlib,html,io,json,re,shutil
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def copy_if_different(source,target):
    if source.resolve()!=target.resolve():shutil.copy2(source,target)
def data_image(path,reference=False):
    if not reference:return 'data:image/png;base64,'+base64.b64encode(path.read_bytes()).decode()
    # Original-resolution reference frames remain separately available in docs.
    with Image.open(path) as image:
        image.thumbnail((1280,720));b=io.BytesIO();image.convert('RGB').save(b,'JPEG',quality=90)
    return 'data:image/jpeg;base64,'+base64.b64encode(b.getvalue()).decode()
def read_receipts(path):
    log=path.read_text(errors='replace')
    start=log.rfind('CHAO_WORLD025_PREPARED hub-wide stage=0 ')
    if start<0:raise RuntimeError('No complete integrated world-study start in log')
    log=log[start:]
    if 'CHAO_WORLD025_COMPLETE 42 captures / 42 saves' not in log:raise RuntimeError('Current world study did not complete all 42 actual saved captures')
    captures=re.findall(r'(CHAO_WORLD025_CAPTURE (chao025-\S+\.png) stage=(\d+) [^\n]+)',log)
    saves=re.findall(r'CHAO_WORLD025_SAVED (chao025-\S+\.png) stage=(\d+) sha256=([a-f0-9]{64})',log)
    if len(captures)!=42 or len(saves)!=42 or {int(x[2]) for x in captures}!=set(range(42)):raise RuntimeError('Capture events are not one coherent 42-scene study')
    return {name:(int(stage),line) for line,name,stage in captures},{name:(int(stage),digest) for name,stage,digest in saves}
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,default=ROOT/'run/screenshots')
    parser.add_argument('--log',type=Path,default=ROOT/'run/logs/latest.log')
    parser.add_argument('--baby-log',type=Path)
    parser.add_argument('--baby-source',type=Path,help='Timed baby PNG directory, if different from --source')
    parser.add_argument('--motion-provenance',type=Path,default=ROOT/'docs/motion-0.25/video/motion-provenance.json')
    parser.add_argument('--allow-unreviewed',action='store_true',help='Build a review gallery without claiming visual PASS')
    args=parser.parse_args();plan=json.loads((ROOT/'docs/release-scenes-0.25.0.json').read_text())
    if not args.allow_unreviewed and not args.baby_log:raise RuntimeError('Final gallery requires the nine native continuous action clips and their visual review')
    events,saves=read_receipts(args.log);features=plan['features'];review_path=ROOT/'docs/validation-0.25/world-visual-review.json'
    reviews=json.loads(review_path.read_text()) if review_path.exists() else {}
    reviewed={x['key']:x for x in reviews.get('scenes',[])};records=[];cards=[]
    destination=ROOT/'docs/screenshots-0.25';destination.mkdir(parents=True,exist_ok=True)
    for index,scene in enumerate(features):
        name='chao025-'+scene['key']+'.png';source=args.source/name;digest=sha(source)
        if events.get(name,(None,))[0]!=index or saves.get(name)!=(index,digest):raise RuntimeError('Exact frame/event/save SHA mismatch: '+name)
        with Image.open(source) as image:
            image.load()
            if image.size!=(1280,720):raise RuntimeError('Framebuffer size changed: '+name)
        if not args.allow_unreviewed:
            checked=reviewed.get(scene['key'],{})
            if checked.get('sha256')!=digest or checked.get('result')!='PASS':raise RuntimeError('Current exact frame not visually reviewed PASS: '+name)
        target=ROOT/'docs'/scene['file'];copy_if_different(source,target)
        record=dict(key=scene['key'],file=scene['file'],sha256=digest,capture_event=events[name][1],visual_review=reviewed.get(scene['key'],{}))
        ref=scene.get('reference');reference=''
        if ref:
            ref_path=ROOT/'docs'/ref['file'];record['reference']=dict(**ref,sha256=sha(ref_path))
            reference=f'<div class="view reference"><div class="tag">USER VIDEO · {html.escape(ref["timecode"])}</div><img src="{data_image(ref_path,True)}" width="1280" height="720" alt="Native source frame at {html.escape(ref["timecode"])}"><p>{html.escape(ref["video"])} · {html.escape(ref["timecode"])}</p></div>'
        actual=f'<div class="view"><div class="tag">CURRENT MINECRAFT FRAME</div><img src="{data_image(target)}" width="1280" height="720" alt="{html.escape(scene["caption"])}"><p>1280×720 · SHA256 <code>{digest[:16]}…</code></p></div>'
        cards.append(f'<article id="{scene["key"]}"><h2>{index+1:02d}. {html.escape(scene["key"].replace("-"," "))}</h2><p>{html.escape(scene["caption"])}</p><div class="comparison">{actual}{reference}</div><a class="back" href="#top">Back to contents ↑</a></article>')
        records.append(record)
    motion_cards=[];motion_records=[]
    if args.baby_log:
        baby_source=args.baby_source or args.source
        baby_log=args.baby_log.read_text(errors='replace');keys=['newborn','quiet','crawl','pet','question','meal','water','sleep','held']
        if 'CHAO_BABY025_COMPLETE 108 timed frames' not in baby_log:raise RuntimeError('Baby study is incomplete')
        provenance=json.loads(args.motion_provenance.read_text())
        if provenance['source_log_sha256']!=sha(args.baby_log):raise RuntimeError('Continuous video provenance belongs to a different baby run')
        raw=args.motion_provenance.parent/'baby-actual-final.mp4'
        if not raw.is_file() or sha(raw)!=provenance['source_video_sha256']:raise RuntimeError('The complete-download raw recording is missing or mismatched')
        clips={x['scene']:x for x in provenance['clips']}
        if set(clips)!=set(keys):raise RuntimeError('Nine final continuous action clips are required')
        baby_review_path=ROOT/'docs/validation-0.25/baby-visual-review.json'
        baby_review=json.loads(baby_review_path.read_text()) if baby_review_path.exists() else {}
        reviewed_clips={x['key']:x for x in baby_review.get('sequences',[])}
        baby_saved=dict(re.findall(r'CHAO_BABY025_SAVED (chao025-baby-\S+\.png) sha256=([a-f0-9]{64})',baby_log))
        motion_dir=ROOT/'docs/motion-0.25';motion_dir.mkdir(parents=True,exist_ok=True)
        for key in keys:
            clip=clips[key];clip_path=args.motion_provenance.parent/clip['file']
            if sha(clip_path)!=clip['sha256'] or (clip['probe']['width'],clip['probe']['height'])!=(1280,720):raise RuntimeError('Actual native clip binding changed: '+key)
            if clip['probe']['avg_frame_rate']!='30/1':raise RuntimeError('Expected unchanged30fps native video: '+key)
            if not args.allow_unreviewed and (reviewed_clips.get(key,{}).get('sha256')!=clip['sha256'] or reviewed_clips.get(key,{}).get('result')!='PASS'):raise RuntimeError('Native action clip not individually reviewed PASS:'+key)
            frames=[];frame_records=[]
            for i in range(12):
                path=baby_source/f'chao025-baby-{key}-{i:02d}.png'
                if baby_saved.get(path.name)!=sha(path):raise RuntimeError('Baby save hash mismatch: '+path.name)
                event=next((line for line in baby_log.splitlines() if f'CHAO_BABY025_FRAME file={path.name} ' in line),None)
                if event is None:raise RuntimeError('Missing timed baby event: '+path.name)
                with Image.open(path) as f:
                    if f.size!=(1280,720):raise RuntimeError('Wrong baby frame size')
                    frames.append(f.convert('RGB').resize((640,360),Image.Resampling.LANCZOS))
                raw_destination=motion_dir/'frames'/path.name;raw_destination.parent.mkdir(parents=True,exist_ok=True);copy_if_different(path,raw_destination)
                frame_records.append(dict(file=raw_destination.relative_to(ROOT/'docs').as_posix(),sha256=sha(path),event=event))
            gif=motion_dir/f'baby-{key}.gif';frames[0].save(gif,save_all=True,append_images=frames[1:],duration=250,loop=0,optimize=True)
            strip=Image.new('RGB',(1280,3*202),'#17252f');draw=ImageDraw.Draw(strip)
            for i,f in enumerate(frames):
                tile=f.resize((320,180),Image.Resampling.LANCZOS);x=(i%4)*320;y=(i//4)*202;strip.paste(tile,(x,y+22));draw.text((x+8,y+4),f'{key} · frame{i:02d} · nominal{i*.25:.2f}s',fill='white')
            strip_path=motion_dir/f'baby-{key}-strip.jpg';strip.save(strip_path,quality=92)
            encoded='data:image/gif;base64,'+base64.b64encode(gif.read_bytes()).decode()
            native_video='data:video/mp4;base64,'+base64.b64encode(clip_path.read_bytes()).decode()
            poster=data_image(baby_source/f'chao025-baby-{key}-05.png')
            motion_cards.append(f'<article id="baby-{key}"><h2>Baby motion: {html.escape(key)}</h2><p>The video is a contiguous interval of actual 1280×720 game-window frames captured at 30 fps, without interpolation or retiming. It includes the production trigger and the full observed action interval. The GIF and contact strip below are sparse navigation aids from 12 separately saved frames, spaced 5 client ticks.</p><video controls loop playsinline preload="metadata" poster="{poster}" width="1280" height="720"><source src="{native_video}" type="video/mp4">Your viewer does not support embedded MP4 playback.</video><p>Source recording interval {clip["source_start_seconds"]:.3f}–{clip["source_end_seconds"]:.3f}s · SHA256 <code>{clip["sha256"][:16]}…</code></p><details><summary>Show sparse frame aid and labelled strip</summary><img class="motion" src="{encoded}" width="640" height="360" alt="Actual {key} sparse frame aid"><img src="{data_image(strip_path,True)}" width="1280" height="606" alt="Twelve individually labelled {key} frames"></details></article>')
            motion_records.append(dict(key=key,gif=gif.relative_to(ROOT/'docs').as_posix(),sha256=sha(gif),strip=strip_path.relative_to(ROOT/'docs').as_posix(),clip=dict(file=clip_path.relative_to(ROOT/'docs').as_posix(),sha256=clip['sha256'],source_start_seconds=clip['source_start_seconds'],source_end_seconds=clip['source_end_seconds'],probe=clip['probe']),frames=frame_records))
    navigation=''.join(f'<a href="#{x["key"]}">{i+1:02d} {html.escape(x["key"].replace("-"," "))}</a>' for i,x in enumerate(features))
    navigation+=''.join(f'<a href="#baby-{x["key"]}">Baby {x["key"]}</a>' for x in motion_records)
    status='CAPTURED — visual review pending' if args.allow_unreviewed else 'PASS — all 42 current still captures individually reviewed'
    page='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Chao Garden 0.25 — actual game and video reference</title><style>
    :root{color-scheme:dark}*{box-sizing:border-box}body{margin:0;background:#0b1720;color:#e9f5f7;font:16px/1.55 system-ui,sans-serif}header,main,footer{max-width:1600px;padding:28px;margin:auto}h1{font-size:clamp(28px,4vw,48px);line-height:1.12}h2{font-size:23px;margin:0 0 12px}p{color:#bfd5df;max-width:1100px}nav{display:flex;flex-wrap:wrap;gap:7px}nav a{background:#203d4c;border-radius:6px;padding:5px 9px;text-decoration:none;font-size:13px}a{color:#bdeef7}article{margin:0 0 32px;padding:22px;background:#142733;border:1px solid #315362;border-radius:12px;scroll-margin-top:12px}.comparison{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,500px),1fr));gap:16px}.view{background:#09141b;border:1px solid #32505e;border-radius:7px;overflow:hidden}.tag{font-size:11px;letter-spacing:1.4px;color:#9edcea;padding:9px 12px}.reference .tag{color:#f2cb89}img,video{display:block;width:100%;height:auto}.view p{padding:0 12px;font-size:12px}.back{display:inline-block;margin-top:14px;font-size:13px}.motion{max-width:960px;margin:0 auto 18px}details{margin-top:20px}summary{cursor:pointer;color:#aee4ef}code{font-size:11px}.badge{color:#d6f5cc}footer{color:#a7c6d4}</style><header id="top"><h1>Chao Garden 0.25</h1><p>Current Minecraft Java 1.21.1 framebuffer captures, compared with exact timestamped frames from the two supplied Sonic Adventure 2 videos. Current views use controlled cameras and clearly tagged disposable actors. Services, placement, release and toy choice use production paths. Native video frames are references; they are labelled separately.</p><p class="badge">'''+html.escape(status)+'''</p><p>The gallery is self-contained and works offline, including nine continuous 30 fps action videos. Native 1280×720 current stills are embedded without image alteration. Source-reference previews are reduced for this page; original 2560×1440 PNGs and video provenance remain in the source archive. An invented adult teacher and emerald sell prices are Minecraft adaptations.</p><nav>'''+navigation+'</nav></header><main>'+''.join(cards+motion_cards)+'</main><footer>No screenshots from earlier releases or synthetic mock game views are used. The still-capture manifest binds every image to its exact capture event and successful saved-file SHA256 acknowledgement. Optional full raw recording, available in the complete download only: <a href="motion-0.25/video/baby-actual-final.mp4">full actual client recording</a>. This optional archive-only link is not needed to play the embedded action clips.</footer></html>'
    gallery=ROOT/'docs/release-gallery-0.25.0.html';gallery.write_text(page)
    evidence_dir=ROOT/'docs/validation-0.25';evidence_dir.mkdir(parents=True,exist_ok=True)
    saved_log=evidence_dir/'world-capture.txt';copy_if_different(args.log,saved_log)
    logs=[dict(file=saved_log.relative_to(ROOT).as_posix(),sha256=sha(saved_log))]
    if args.baby_log:
        saved_baby_log=evidence_dir/'baby-capture.txt';copy_if_different(args.baby_log,saved_baby_log);logs.append(dict(file=saved_baby_log.relative_to(ROOT).as_posix(),sha256=sha(saved_baby_log)))
    manifest=dict(release='0.25.0',result='CAPTURED' if args.allow_unreviewed else 'PASS',capture_count=len(records),logs=logs,gallery=dict(file=gallery.relative_to(ROOT/'docs').as_posix(),sha256=sha(gallery),self_contained=True),captures=records,motion=motion_records)
    (ROOT/'docs/release-capture-manifest-0.25.0.json').write_text(json.dumps(manifest,indent=2)+'\n')
    ids=re.findall(r'id="([^"]+)"',page)
    if len(ids)!=len(set(ids)) or any(t not in ids for t in re.findall(r'href="#([^"]+)"',page)):raise RuntimeError('Unresolved or duplicate navigation anchors')
    if '<script' in page or re.search(r'(?:src|href)="https?://',page):raise RuntimeError('Gallery has external or scripted dependencies')
    current_payloads=re.findall(r'src="data:image/png;base64,([^"]+)"',page)
    if len(current_payloads)!=len(records):raise RuntimeError('Current frame embedding count changed')
    for record,payload in zip(records,current_payloads):
        if hashlib.sha256(base64.b64decode(payload,validate=True)).hexdigest()!=record['sha256']:raise RuntimeError('Embedded frame differs from saved game capture:'+record['key'])
    for mime,b64 in re.findall(r'(?:src|poster)="data:(image/[^;]+);base64,([^"]+)"',page):
        payload=base64.b64decode(b64,validate=True)
        with Image.open(io.BytesIO(payload)) as im:im.load()
    video_payloads=re.findall(r'src="data:video/mp4;base64,([^"]+)"',page)
    if len(video_payloads)!=len(motion_records):raise RuntimeError('Embedded native video count changed')
    for record,payload in zip(motion_records,video_payloads):
        if hashlib.sha256(base64.b64decode(payload,validate=True)).hexdigest()!=record['clip']['sha256']:raise RuntimeError('Embedded native video differs from reviewed clip:'+record['key'])
    report=ROOT/'docs/validation-0.25/gallery.json';report.parent.mkdir(parents=True,exist_ok=True)
    report.write_text(json.dumps(dict(result=manifest['result'],embedded_current_stills=len(records),motion_sequences=len(motion_records),embedded_native_30fps_videos=len(motion_records),network_dependencies=0,self_contained=True,all_embedded_images_decoded=True,all_stills_save_digest_bound=True,all_embedded_videos_digest_bound=True,individual_visual_review_required=True),indent=2)+'\n')
    print(f'{len(records)} actual current stills;{len(motion_records)} motion sequences;gallery{gallery.stat().st_size:,}bytes;{manifest["result"]}')
if __name__=='__main__':main()
