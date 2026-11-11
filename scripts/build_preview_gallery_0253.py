#!/usr/bin/env python3
"""Build the 0.25.3 offline gallery from the completed native capture cohort."""
from pathlib import Path
from fractions import Fraction
import argparse
import base64
import hashlib
import html
import json
import mimetypes
import re
import tempfile
import zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
RELEASE='0.25.3'
REALMS={'school':'chao_garden:kindergarten','garden':'chao_garden:garden',
        'hero':'chao_garden:hero_garden','hub':'chao_garden:hub'}
GROUPS=(('fortune','Complete Fortune circles',('fortune-',)),
        ('cabinet','Hidden metal utility cabinet',('cabinet-','market-')),
        ('neutral','Waterfall on the adjoining garden wall',('neutral-',)),
        ('hero','First playable Hero Garden',('hero-',)))

def require(value,message):
    if not value:raise RuntimeError(message)

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def esc(value):return html.escape(str(value),quote=True)
def phases(scene):
    if scene.get('walk_to'):return ('start','arrived')
    return ('start','end') if scene.get('motion') or scene.get('walk_distance') or scene.get('camera_path') else ('still',)
def moving(scene):return any(scene.get(key) for key in ('motion','walk_to','walk_distance','camera_path'))
def frame_name(scene,phase):return 'chao0253-'+scene['key']+('' if phase=='still' else '-'+phase)+'.png'

def resolve_under(parent,name):
    path=(parent/name).resolve()
    require(path.is_relative_to(parent.resolve()) and path.is_file(),'Missing or unsafe local resource: '+str(name))
    return path

def atomic_write(path,payload):
    path.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=path.parent,prefix=path.name+'.',delete=False) as handle:
        temporary=Path(handle.name);handle.write(payload)
    temporary.replace(path)

def build(args):
    docs=ROOT/'docs';plan_path=docs/'release-scenes-0.25.3.json'
    plan=json.loads(plan_path.read_text());plan_sha=sha(plan_path)
    require(plan.get('release')==RELEASE and plan.get('scene_count')==len(plan['scenes'])==23,'Expected exactly23 current release scenes')
    scenes=plan['scenes'];keys=[row['key'] for row in scenes]
    require(len(set(keys))==23 and all(re.fullmatch(r'[a-z0-9-]+',key) for key in keys),'Duplicate or unsafe scene keys')
    require(plan['expected_frames']==sum(len(phases(row)) for row in scenes)==32,'Expected exactly32 original native PNGs')
    clip_keys={row['key'] for row in scenes if moving(row)}
    require(plan['continuous_clip_count']==len(clip_keys)==9,'Expected exactly9 current native clips')
    receipt=json.loads(args.receipt.read_text());provenance=json.loads(args.provenance.read_text())
    require(receipt.get('release')==RELEASE and receipt.get('result')=='PASS','Native capture receipt is not current and complete')
    require(receipt.get('scene_plan_sha256')==provenance.get('scene_plan_sha256')==plan_sha,'Native capture/provenance belongs to another plan SHA')
    native=receipt['scenes'];native_by_file={Path(row['file']).name:row for row in native}
    require(len(native)==len(native_by_file)==32,'Native receipt has missing/duplicate frame entries')
    clips={row['scene']:row for row in provenance['clips']}
    require(len(clips)==len(provenance['clips'])==9 and set(clips)==clip_keys,'Clip cohort differs from the exact scene plan')
    frozen={plan_path:plan_sha,args.receipt:sha(args.receipt),args.provenance:sha(args.provenance)}
    assets={};frames=[];movies=[];scene_frames={}
    def asset(path,digest=None):
        digest=digest or sha(path);require(sha(path)==digest,'Media SHA mismatch: '+str(path))
        frozen[path]=digest;relative=path.relative_to(docs).as_posix();assets[relative]=path
        return relative,digest
    expected_names={frame_name(scene,phase) for scene in scenes for phase in phases(scene)}
    require(set(native_by_file)==expected_names,'Native receipt frame names differ from scene phases')
    require({p.name for p in args.screenshots.glob('chao0253-*.png')}==expected_names,'Screenshot folder has missing or extra current PNGs')
    for stage,scene in enumerate(scenes):
        scene_frames[scene['key']]=[]
        for phase in phases(scene):
            name=frame_name(scene,phase);path=resolve_under(args.screenshots,name);row=native_by_file[name]
            require((row['stage'],row['scene'],row['phase'])==(stage,scene['key'],phase),'Native frame stage/scene/phase mismatch: '+name)
            require(row['mode']=='ADVENTURE' and row['stable_render_frames']>=40,'Native frame is not a settled Adventure capture: '+name)
            expected_space=scene['walk_to'] if phase=='arrived' else scene['space']
            require(expected_space in REALMS and row['dimension']==REALMS[expected_space],'Native frame realm mismatch: '+name)
            with Image.open(path) as image:
                image.load();require(image.size==tuple(plan['framebuffer'])==(1280,720),'Native PNG framebuffer changed: '+name)
                require(image.convert('RGBA').getchannel('A').getextrema()==(255,255),'Native PNG acquired transparency: '+name)
            relative,digest=asset(path,row['sha256']);record=dict(scene=scene['key'],phase=phase,file=relative,sha256=digest)
            frames.append(record);scene_frames[scene['key']].append(record)
        if scene['key'] in clips:
            clip=clips[scene['key']];path=resolve_under(args.provenance.parent,clip['file']);relative,digest=asset(path,clip['sha256'])
            require(path.suffix=='.mp4' and path.stat().st_size==clip['size_bytes'],'Native MP4 type/size mismatch')
            hz=float(Fraction(clip['probe']['avg_frame_rate']));require(hz>0,'Native clip has invalid actual cadence')
            movies.append(dict(scene=scene['key'],file=relative,sha256=digest,actual_average_capture_hz=hz))
    raw=resolve_under(args.provenance.parent,provenance['source_video']);raw_relative,_=asset(raw,provenance['source_video_sha256'])
    metadata=raw.with_suffix('.capture.json')
    if provenance.get('capture_metadata_sha256'):asset(metadata,provenance['capture_metadata_sha256'])
    for path in (plan_path,args.receipt,args.provenance):asset(path)
    references=[];reference_manifest=docs/'references-0.25.3/hero/image-provenance.json'
    if reference_manifest.is_file():
        source=json.loads(reference_manifest.read_text());asset(reference_manifest)
        for row in source['images']:
            path=resolve_under(reference_manifest.parent,row['file']);relative,digest=asset(path,row['sha256'])
            references.append(dict(file=relative,sha256=digest,label='Hero Garden source reference · '+path.name))
    movie_by_scene={row['scene']:row for row in movies}
    embedded_files={row['file'] for row in frames+movies+references}
    def url(relative,embedded):
        if not embedded:return esc(relative)
        path=assets[relative];mime=mimetypes.guess_type(path.name)[0]
        require(mime in ('image/png','image/jpeg','video/mp4'),'Unexpected embedded media type')
        return 'data:'+mime+';base64,'+base64.b64encode(path.read_bytes()).decode('ascii')
    def page(embedded):
        sections=[];nav=[]
        for anchor,title,prefixes in GROUPS:
            selected=[scene for scene in scenes if scene['key'].startswith(prefixes)]
            require(selected,'Gallery request group is empty: '+anchor);nav.append('<a href="#'+anchor+'">'+esc(title)+'</a>')
            cards=[]
            for scene in selected:
                figures=[]
                for frame in scene_frames[scene['key']]:
                    figures.append('<figure><figcaption>ORIGINAL MINECRAFT PNG · '+esc(frame['phase'].upper())+'</figcaption><img data-original="'+esc(Path(frame['file']).name)+'" src="'+url(frame['file'],embedded)+'" width="1280" height="720" loading="lazy" alt="'+esc(scene['review_focus']+' · '+frame['phase'])+'"></figure>')
                movie='';clip=movie_by_scene.get(scene['key'])
                if clip:
                    movie='<div class="movie"><video data-scene="'+scene['key']+'" controls playsinline preload="none"><source src="'+url(clip['file'],embedded)+'" type="video/mp4"></video><p>Continuous native capture · actual average '+format(clip['actual_average_capture_hz'],'.2f')+' fps. Original frame order and timestamps.</p></div>'
                cards.append('<article id="'+scene['key']+'"><h3>'+esc(scene['key'].replace('-',' ').capitalize())+'</h3><p>'+esc(scene['review_focus'])+'</p><div class="views">'+''.join(figures)+'</div>'+movie+'</article>')
            sections.append('<section id="'+anchor+'"><h2>'+esc(title)+'</h2>'+''.join(cards)+'</section>')
        require(sum(len([s for s in scenes if s['key'].startswith(prefixes)]) for _,_,prefixes in GROUPS)==23,'Some scenes are omitted or shown twice')
        reference_html=''
        if references:
            reference_html='<section id="references"><h2>Reference images</h2><p>Source-game reference material. These images are separate from the current Minecraft captures above.</p><div class="views">'+''.join('<figure class="reference"><figcaption>'+esc(row['label'])+'</figcaption><img src="'+url(row['file'],embedded)+'" loading="lazy" alt="'+esc(row['label'])+'"></figure>' for row in references)+'</div></section>'
            nav.append('<a href="#references">Reference images</a>')
        recording=('The complete original recording is included in the companion gallery ZIP.' if embedded else '<a href="'+esc(raw_relative)+'">Complete original native recording</a>')
        return '''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Chao Garden 0.25.3 — native capture gallery</title><style>
*{box-sizing:border-box}html{color-scheme:dark;scroll-behavior:smooth}body{margin:0;background:#101821;color:#edf2f6;font:16px/1.55 system-ui,sans-serif}header,main,footer{max-width:1500px;margin:auto;padding:28px}h1{font-size:clamp(28px,4vw,44px);line-height:1.15}h2{font-size:29px;margin-top:45px}h3{font-size:21px;margin-top:0}p{color:#b8c9d6}a{color:#aad8f2}nav{display:flex;gap:8px;flex-wrap:wrap}nav a{background:#243747;padding:8px 12px;border-radius:6px;text-decoration:none}article{background:#182532;border:1px solid #354b5b;border-radius:10px;margin:18px 0;padding:20px;scroll-margin-top:15px}.views{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,500px),1fr));gap:14px}figure{margin:0;background:#0a121b;border:1px solid #344958;border-radius:6px;overflow:hidden}figcaption{font-size:12px;letter-spacing:.35px;padding:8px 12px;color:#a7dcf6}.reference figcaption{color:#edcb91}img,video{width:100%;height:auto;display:block}.movie{max-width:1080px;margin:20px auto 0}.movie p,footer{font-size:13px}code{overflow-wrap:anywhere}section{scroll-margin-top:20px}</style><header id="top"><h1>Chao Garden 0.25.3</h1><p>23 current Minecraft scenes · 32 original PNGs · 9 continuous native clips.</p><p>Four requested changes: complete Fortune wall circles, a hidden metal cabinet entrance, the garden waterfall on its adjoining wall, and the first playable Hero Garden. Hero views include its layout, fountain, pavilion, bell island and normal entry/return journey.</p><p>'''+('All required captures are embedded in this offline page.' if embedded else 'This offline gallery uses the original media files included beside it.')+''' Reference images are labelled separately. This gallery preserves capture media and does not substitute for the release’s visual review.</p><nav>'''+''.join(nav)+'</nav></header><main>'+''.join(sections)+reference_html+'</main><footer><p>'+recording+'</p><p>Scene plan SHA-256: <code>'+plan_sha+'</code></p><a href="#top">Back to contents ↑</a></footer></html>'
    relative_page=page(False);canonical=docs/'release-gallery-0.25.3.html'
    for local in re.findall(r'(?:src|href)="([^"]+)"',relative_page):
        if not local.startswith('#'):resolve_under(docs,html.unescape(local))
    estimated=sum(4*((assets[name].stat().st_size+2)//3) for name in embedded_files)+262144
    standalone=None;standalone_page=None
    if estimated<=args.max_standalone_mib*1024*1024:
        standalone_page=page(True).encode();require(len(standalone_page)<=args.max_standalone_mib*1024*1024,'Standalone exceeded its explicit size budget')
        require(len(re.findall(r'data-original="',standalone_page.decode()))==32 and len(re.findall(r'data-scene="',standalone_page.decode()))==9,'Wrong standalone embedded cohort')
        standalone=dict(file=args.standalone.name,sha256=hashlib.sha256(standalone_page).hexdigest(),size_bytes=len(standalone_page),self_contained=True)
    proof=dict(release=RELEASE,result='BUILT',scene_count=23,original_png_count=32,native_clip_count=9,scene_plan_sha256=plan_sha,receipt_sha256=sha(args.receipt),motion_provenance_sha256=sha(args.provenance),captures=frames,clips=movies,references=references,standalone=standalone,full_recording=dict(file=raw_relative,sha256=frozen[raw]))
    for path,digest in frozen.items():require(sha(path)==digest,'Source changed while building gallery: '+str(path))
    atomic_write(canonical,relative_page.encode())
    if standalone_page:atomic_write(args.standalone,standalone_page)
    else:args.standalone.unlink(missing_ok=True)
    manifest=docs/'release-capture-manifest-0.25.3.json';atomic_write(manifest,(json.dumps(proof,indent=2)+'\n').encode())
    args.zip.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=args.zip.parent,prefix=args.zip.name+'.',delete=False) as handle:temporary=Path(handle.name)
    with zipfile.ZipFile(temporary,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
        archive.write(canonical,'index.html');archive.write(manifest,manifest.name)
        for relative,path in sorted(assets.items()):archive.write(path,relative)
    with zipfile.ZipFile(temporary) as archive:
        require(archive.testzip() is None,'Gallery ZIP CRC check failed')
        require(all(relative in archive.namelist() for relative in assets),'Gallery ZIP omitted a relative media/full-recording link')
    temporary.replace(args.zip)
    print('BUILT:23 scenes,32 original PNGs,9 native clips; exact media hashes and plan SHA bound; '+('self-contained HTML + ' if standalone else 'standalone over size budget; ')+'portable ZIP with full recording.')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--screenshots',type=Path,default=ROOT/'docs/screenshots-0.25.3')
    parser.add_argument('--receipt',type=Path,default=ROOT/'docs/validation-0.25.3/runtime-client.json')
    parser.add_argument('--provenance',type=Path,default=ROOT/'docs/motion-0.25.3/motion-provenance.json')
    parser.add_argument('--standalone',type=Path,default=ROOT.parent/'deliverables/chao-garden-0.25.3-gallery.html')
    parser.add_argument('--zip',type=Path,default=ROOT.parent/'deliverables/chao-garden-0.25.3-gallery.zip')
    parser.add_argument('--max-standalone-mib',type=int,default=150)
    build(parser.parse_args())

if __name__=='__main__':main()
