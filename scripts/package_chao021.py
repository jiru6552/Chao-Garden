#!/usr/bin/env python3
"""Package 0.21 from a metadata-described, completed actual Minecraft study.

No file-size ceiling is imposed. Every raw capture and its original timing is
retained; review GIFs contain only resized source frames, without interpolation.
The017packager remains an imported set of established byte/parity/privacy checks.
"""
from pathlib import Path
import argparse, base64, hashlib, html, importlib.util, io, json, math, os, posixpath, re, statistics, textwrap, zipfile
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT.parent
OUT=BASE/'deliverables'
VERSION='0.21.0'
spec=importlib.util.spec_from_file_location('chao_package_017_helpers',ROOT/'scripts/package_chao017.py')
a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)
a.VERSION=VERSION
a.REDUNDANT_COMPOSITES.add(f'chao-garden-{VERSION}-preview.png')
require,sha256,write_flushed,copy_flushed=a.require,a.sha256,a.write_flushed,a.copy_flushed
HISTORIES=(('release-gallery-0.20.0.html','historical-0.20.0-refinements-gallery.html','0.20.0'),('release-gallery-0.19.0.html','historical-0.19.0-kindergarten-gallery.html','0.19.0'),('release-gallery-0.18.0.html','historical-0.18.0-behavior-gallery.html','0.18.0'),('release-gallery-0.17.0.html','historical-0.17.0-surface-gallery.html','0.17.0'),*a.HISTORIES)
KEY=re.compile(r'[a-z0-9]+(?:[-_][a-z0-9]+)*\Z')


def historical_launcher(label,target):
    return a.historical_launcher(label,target).replace('current 0.17.0','current '+VERSION)


def pdf_page_metadata(plan):
    features={item['key']:item for item in plan['features']}
    return [dict(page=index,key=key,title=features[key].get('pdf_title',features[key]['title']),
        subtitle=features[key].get('pdf_subtitle',features[key]['subtitle']),
        caption=features[key].get('pdf_caption',features[key]['caption'])) for index,key in enumerate(plan['pdf'],1)]


def pdf_page_metadata_sha256(plan):
    return hashlib.sha256(json.dumps(pdf_page_metadata(plan),ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()).hexdigest()


def load_plan(root=ROOT,path=None):
    path=Path(path) if path else root/f'docs/release-scenes-{VERSION}.json'
    plan=json.loads(path.read_text())
    require(plan.get('release')==VERSION,'Capture plan release differs from '+VERSION)
    plan.setdefault('log_prefix','CHAO_EXPERIENCE');plan.setdefault('filename_prefix','chao021-')
    require(re.fullmatch(r'[A-Z][A-Z0-9_]*',plan['log_prefix']),'Invalid capture log prefix')
    require(KEY.fullmatch(plan['filename_prefix'].removesuffix('-')) is not None,'Invalid capture filename prefix')
    require(isinstance(plan.get('features'),list) and plan['features'],'At least one feature is required')
    plan.setdefault('motions',[])
    seen=set();features={}
    for feature in plan['features']:
        for field in ('key','title','subtitle','caption','category'):
            require(isinstance(feature.get(field),str) and feature[field].strip(),'Missing feature '+field)
        key=feature['key'];require(KEY.fullmatch(key) and key not in seen,'Duplicate or invalid feature key: '+key)
        seen.add(key);features[key]=feature
    motion_names=set();all_keys=list(features)
    for motion in plan['motions']:
        key=motion.get('key');frames=motion.get('frames')
        require(isinstance(key,str) and KEY.fullmatch(key) and key not in motion_names,'Duplicate or invalid motion key')
        motion_names.add(key)
        require(isinstance(motion.get('title'),str) and isinstance(motion.get('caption'),str),'Motion copy is missing')
        require(isinstance(frames,list) and len(frames)>=2,'A motion sequence needs at least two actual captures')
        for frame in frames:
            require(isinstance(frame,str) and KEY.fullmatch(frame) and frame not in seen,'Duplicate or invalid motion frame: '+str(frame))
            seen.add(frame);all_keys.append(frame)
        review=motion.get('review_frames',frames)
        require(isinstance(review,list) and len(review)>=2 and len(review)==len(set(review)),'Motion review needs at least two distinct captured frames')
        require(all(frame in frames for frame in review),'Motion review must use only its own captured frames')
        indices=[frames.index(frame) for frame in review]
        require(indices==sorted(indices),'Motion review frames must retain original capture order')
        if review!=frames:require(isinstance(motion.get('omission_reason'),str) and motion['omission_reason'].strip(),'A shortened review must explain every omitted capture')
    order=plan.get('capture_order',all_keys)
    require(isinstance(order,list) and len(order)==len(set(order)) and set(order)==seen,'Capture order must include every frame exactly once')
    plan['capture_order']=order
    pages=plan.get('pdf',list(features));require(isinstance(pages,list) and pages,'PDF selection is empty')
    require(len(pages)==len(set(pages)) and all(k in features for k in pages),'PDF pages must select distinct feature keys')
    plan['pdf']=pages
    return plan


def read_capture_session(log_path,plan):
    log=Path(log_path).read_text(errors='replace');prefix=re.escape(plan['log_prefix']);shot=re.escape(plan['filename_prefix'])
    end_rx=prefix+r'_STUDY_COMPLETE:\s*(\d+)\s*actual Minecraft views'
    captured_rx=prefix+r'_CAPTURED ('+shot+r'[a-z0-9_-]+\.png) stage=(\d+) gameTick=(\d+)\b'
    start_rx=prefix+r'_CAPTURED '+shot+r'[a-z0-9_-]+\.png stage=0\b'
    endings=list(re.finditer(end_rx,log));require(endings,'No completed actual-client study in '+str(log_path))
    end=endings[-1];lower=endings[-2].end() if len(endings)>1 else 0
    starts=list(re.finditer(start_rx,log[lower:end.start()]));require(starts,'Completed study has no initial capture')
    start=lower+starts[-1].start();require(re.search(start_rx,log[end.end():]) is None,'A newer incomplete study follows the completed run')
    segment=log[start:end.start()];captures=re.findall(captured_rx,segment);count=int(end[1])
    names=[name for name,stage,tick in captures]
    expected=[plan['filename_prefix']+key+'.png' for key in plan['capture_order']]
    require(count==len(captures)==len(expected),'Capture count differs from the current plan')
    require([int(stage) for name,stage,tick in captures]==list(range(count)),'Capture stages are missing, duplicated or unordered')
    require(names==expected,'Capture names/order differ from the current metadata plan')
    ticks={name:int(tick) for name,stage,tick in captures}
    require(all(b>=a for a,b in zip(ticks.values(),list(ticks.values())[1:])),'Game clock moves backwards')
    for name in names:
        require(re.search(prefix+r'_SCREENSHOT '+re.escape(name)+r':[^\n]*Saved screenshot',log[start:]),'Missing asynchronous screenshot save: '+name)
    record=segment.strip()+'\n'+plan['log_prefix']+'_STUDY_COMPLETE: '+str(count)+' actual Minecraft views\n'
    return names,ticks,record


def validate_production_jar(jar,root=ROOT,plan=None):
    a.validate_production_jar(jar,root)
    with zipfile.ZipFile(jar) as z:
        names=z.namelist()
        for name in names:
            require(not (name.startswith('com/chaogarden/client/') and any(x in name for x in ('VisualSmoke','VisualStudy','CaptureStudy','PoseProbe','VisualProbe'))),'Development study class in production JAR: '+name)
        for key in a.IMPORTED_KEYS:
            prefix='assets/chao_garden/textures/entity/chao/official/coats/'+key+'/'
            require(any(n.startswith(prefix) and n.endswith('.png') for n in names),'Missing inherited coat folder: '+key)
        for required in (plan or {}).get('required_jar_entries',[]):
            require(required in names,'Missing required release feature: '+required)


def build_motion(frames,target,label,ticks):
    require(len(frames)>=2 and len(frames)==len(ticks),'Missing motion frames or timing: '+label)
    intervals=[b-a for a,b in zip(ticks,ticks[1:])];require(all(x>0 for x in intervals),'Motion game ticks must strictly increase: '+label)
    # An actual sampled interval keeps the last hold on the50ms game clock;
    # averaging two intervals could produce a duration GIF cannot represent.
    durations=[int(x*50) for x in intervals]+[sorted(intervals)[len(intervals)//2]*50]
    hashes=[sha256(path) for path in frames];require(len(set(hashes))>=2,'Motion has no distinct captured moments: '+label)
    pictures=[]
    for path in frames:
        with Image.open(path) as src:
            frame=src.convert('RGB');frame.thumbnail((640,360),Image.Resampling.LANCZOS)
            whole=Image.new('RGB',(640,360),a.PAPER)
            whole.paste(frame,((640-frame.width)//2,(360-frame.height)//2))
            pictures.append(whole.quantize(colors=192,method=Image.Quantize.MEDIANCUT))
    buffer=io.BytesIO();pictures[0].save(buffer,format='GIF',save_all=True,append_images=pictures[1:],duration=durations,loop=0,optimize=True,disposal=2)
    payload=buffer.getvalue()
    with Image.open(io.BytesIO(payload)) as check:
        require(check.format=='GIF' and check.size==(640,360) and 2<=check.n_frames<=len(frames),'Incomplete motion review: '+label)
        encoded=check.n_frames
        encoded_duration=0
        for index in range(encoded):check.seek(index);encoded_duration+=check.info.get('duration',0)
        require(encoded_duration==sum(durations),'Motion timing changed during GIF encoding: '+label)
    write_flushed(target,payload)
    return dict(file=target.name,sha256=sha256(target),source_frame_count=len(frames),encoded_frame_count=encoded,
        source_files=[p.name for p in frames],source_sha256=hashes,source_game_ticks=ticks,
        source_interval_game_ticks=intervals,frame_durations_ms=durations,
        origin='Actual whole Minecraft frames fitted within640x360 without cropping or stretching; no synthesized or interpolated images',
        playback_timing_basis='Adjacent recorded game-tick differences at 50 ms per tick; last frame held for median interval. This is game-clock review, not wall-clock video.')


def build_feature_sheet(plan,frames,output):
    columns,tile_width,image_height,caption_height,margin,header_height=3,640,360,130,20,120
    rows=math.ceil(len(plan['features'])/columns)
    picture=Image.new('RGB',(columns*tile_width+margin*2,header_height+rows*(image_height+caption_height)+margin),a.PAPER)
    draw=ImageDraw.Draw(picture)
    draw.text((margin,18),f'Chao Garden {VERSION} - Growing forms, robot legacy and companions',fill=a.INK,font=a.font(34))
    draw.text((margin,69),str(len(plan['features']))+' actual game views - whole frames resized to fit - Minecraft Java 1.21.1 / NeoForge',fill=a.MUTED,font=a.font(21))
    for index,feature in enumerate(plan['features']):
        x=margin+index%columns*tile_width;y=header_height+index//columns*(image_height+caption_height)
        with Image.open(frames[feature['key']]) as source:
            shot=source.convert('RGB');shot.thumbnail((tile_width-8,image_height),Image.Resampling.LANCZOS)
            picture.paste(shot,(x+(tile_width-8-shot.width)//2,y+(image_height-shot.height)//2))
        title_lines=a.wrap_pixels(draw,f'{index+1:02d}  '+feature['title'],tile_width-24,a.font(22))
        require(len(title_lines)<=2,'Feature-sheet title needs shortening: '+feature['key'])
        for row,line in enumerate(title_lines):draw.text((x+8,y+image_height+10+row*26),line,fill=a.INK,font=a.font(22))
        subtitle_lines=a.wrap_pixels(draw,feature['subtitle'],tile_width-24,a.font(17))
        require(len(subtitle_lines)<=2,'Feature-sheet subtitle needs shortening: '+feature['key'])
        for row,line in enumerate(subtitle_lines):draw.text((x+8,y+image_height+68+row*23),line,fill=a.MUTED,font=a.font(17))
    a.save_png(picture,output)


def build_gallery(plan,frames,motions,output,urls=None):
    def uri(path):return urls[str(path)] if urls else a.image_data(path)
    cards=[];categories={}
    for f in plan['features']:
        categories.setdefault(f['category'],f.get('category_label',f['category'].replace('-',' ').title()))
        cards.append((f['title'],f['subtitle'],f['caption'],f['category'],frames[f['key']]))
    for m in plan['motions']:
        selected=m.get('review_frames',m['frames'])
        caption=m['caption']+' '+str(len(selected))+' actual source frames in this review; '+str(len(m['frames']))+' raw captures retained. Review timing follows recorded game ticks, without interpolation.'
        if selected!=m['frames']:caption+=' '+m['omission_reason']
        cards.append((m['title'],'Actual captured motion',caption,'motion',motions[m['key']]))
    if plan['motions']:categories['motion']='Motion'
    htmlcards=[]
    for number,(title,subtitle,caption,category,picture) in enumerate(cards,1):
        search=' '.join([title,subtitle,caption,category]).lower()
        htmlcards.append('<article class="card" data-category="'+html.escape(category,quote=True)+'" data-search="'+html.escape(search,quote=True)+'"><button class="capture" type="button" aria-label="Enlarge '+html.escape(title,quote=True)+'"><img src="'+html.escape(uri(picture),quote=True)+'" alt="'+html.escape(title,quote=True)+'" loading="lazy"></button><div class="copy"><p class="number">'+f'{number:02d}'+' - ACTUAL MINECRAFT CAPTURE</p><h2>'+html.escape(title)+'</h2><p class="subtitle">'+html.escape(subtitle)+'</p><p>'+html.escape(caption)+'</p></div></article>')
    options='<option value="all">All previews</option>'+''.join('<option value="'+html.escape(k,quote=True)+'">'+html.escape(v)+'</option>' for k,v in categories.items())
    history=' · '.join('<a href="'+file+'">'+label+'</a>' for original,file,label in HISTORIES)
    page='''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Chao Garden 0.21.0 - Gradual evolution, robot legacy and companions</title><style>
*{box-sizing:border-box}body{margin:0;background:#edf3f5;color:#173f4b;font:16px/1.6 system-ui,sans-serif}header,main,footer{max-width:1600px;margin:auto;padding:28px}h1{font-size:clamp(30px,4vw,48px);line-height:1.15;margin:12px 0 20px}h2{font-size:25px;line-height:1.2;margin:0 0 12px}p{margin:0 0 14px}.number,.badge{font-size:12px;letter-spacing:.07em;color:#287559;font-weight:700}header>p{max-width:1100px}.controls{display:flex;gap:16px;flex-wrap:wrap;align-items:end;margin:24px 0 12px}.search{flex:1;min-width:230px}label{display:block;font-weight:650;font-size:14px;margin-bottom:6px}input,select,button{font:inherit}input,select{padding:12px;border:1px solid #9fbac4;border-radius:9px;background:white;color:#173f4b}input{width:100%}button:focus-visible,input:focus-visible,select:focus-visible,a:focus-visible{outline:3px solid #e39935;outline-offset:4px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,540px),1fr));gap:24px;padding-top:0}.card{background:white;border:1px solid #d4e2e7;border-radius:14px;overflow:hidden}.capture{display:block;width:100%;padding:0;border:0;background:#132c35;cursor:zoom-in}.capture img{display:block;width:100%;height:auto}.copy{padding:24px}.copy p:last-child{margin-bottom:0}.subtitle{font-weight:650;color:#47636e}.copy p{font-size:15px}#count,footer{color:#47636e}a{color:#146c85}dialog{width:min(96vw,1500px);max-height:96vh;padding:14px;border:0;border-radius:12px;background:#edf3f5;color:#173f4b}dialog::backdrop{background:#0c222ce8}dialog img{display:block;max-width:100%;max-height:76vh;width:auto;margin:auto}dialog p{text-align:center;margin:12px 0 0;font-size:14px}.actions{display:flex;justify-content:center;gap:12px;margin-top:12px}.actions button{border:1px solid #9fbac4;border-radius:7px;padding:8px 18px;color:#173f4b;background:white;cursor:pointer}[hidden]{display:none!important}@media(max-width:600px){header,main,footer{padding-left:16px;padding-right:16px}.copy{padding:18px}}
</style></head><body><header><p class="badge">CHAO GARDEN 0.21.0 - MINECRAFT JAVA 1.21.1 / NEOFORGE</p><h1>A Chao grows. A robot leaves a legacy.</h1><p>Actual in-game views of restrained gradual evolution, locked first adult types, robot inheritance, skill-scaled companions and refined Chaos anatomy.</p><p>WHOLECOUNT whole game captures, with FEATURECOUNT feature views and MOTIONCOUNT motion reviews. Extract the archive and open this gallery offline.</p><div class="controls"><div class="search"><label for="search">Find a preview</label><input id="search" type="search" placeholder="Try gradual, heart, companion, Chaos..."></div><div><label for="filter">Show</label><select id="filter">OPTIONS</select></div></div><p id="count" role="status" aria-live="polite"></p></header><main>CARDS</main><footer><p>All current imagery comes from the Minecraft client. Motion reviews retain source captures and game-clock timing. Optional mod identity fixtures do not establish complete modpack compatibility. See source/TESTING.md for completed validation.</p><p>Historical studies: HISTORY.</p><p>Unofficial fan project. Supplied Sonic/Chao models, textures, animation and audio belong to their rights holders. Provenance and Minecraft adaptations are recorded with the source.</p></footer><dialog id="zoom" aria-label="Enlarged preview"><img alt=""><p></p><div class="actions"><button id="previous" type="button">Previous</button><button id="close" type="button">Close</button><button id="next" type="button">Next</button></div></dialog><script>
const cards=[...document.querySelectorAll('.card')],search=document.querySelector('#search'),filter=document.querySelector('#filter'),count=document.querySelector('#count'),zoom=document.querySelector('#zoom');let current=0;
function visible(){return cards.filter(card=>!card.hidden)}function update(){const query=search.value.trim().toLowerCase();for(const card of cards)card.hidden=!(filter.value==='all'||card.dataset.category===filter.value)||!card.dataset.search.includes(query);count.textContent=visible().length+' previews - select an image to enlarge';}function show(card){const image=card.querySelector('img');current=cards.indexOf(card);zoom.querySelector('img').src=image.src;zoom.querySelector('img').alt=image.alt;zoom.querySelector('p').textContent=card.querySelector('h2').textContent+' - '+card.querySelector('.subtitle').textContent;if(!zoom.open)zoom.showModal();}function step(offset){const list=visible();if(!list.length)return;show(list[(list.indexOf(cards[current])+offset+list.length)%list.length]);}search.addEventListener('input',update);filter.addEventListener('change',update);for(const card of cards)card.querySelector('.capture').addEventListener('click',()=>show(card));document.querySelector('#close').addEventListener('click',()=>zoom.close());document.querySelector('#previous').addEventListener('click',()=>step(-1));document.querySelector('#next').addEventListener('click',()=>step(1));zoom.addEventListener('click',event=>{if(event.target===zoom)zoom.close()});zoom.addEventListener('keydown',event=>{if(event.key==='ArrowLeft'){event.preventDefault();step(-1)}if(event.key==='ArrowRight'){event.preventDefault();step(1)}});update();
</script></body></html>'''
    for find,value in [('WHOLECOUNT',len(plan['capture_order'])),('FEATURECOUNT',len(plan['features'])),('MOTIONCOUNT',len(plan['motions'])),('OPTIONS',options),('CARDS',''.join(htmlcards)),('HISTORY',history)]:page=page.replace(find,str(value))
    require('http://' not in page and 'https://' not in page,'Current gallery gained an external dependency')
    require(page.count('<article class="card"')==len(cards),'Missing gallery cards')
    write_flushed(output,page.encode())


def start_here(plan):
    return textwrap.dedent(f"""CHAO GARDEN {VERSION}
Minecraft Java 1.21.1 | NeoForge 21.1.219+ (21.1 series) | Java 21

INSTALL
Replace the old mod jar with chao_garden-{VERSION}.jar in mods/.
Install the same version on the server and every client. Third-party mods are
not bundled. Adult Accessories companions require the optional compatible
NeoForge Accessories mod and its dependency on both sides.

THIS UPDATE
Age-driven gradual evolution with fixed first adult types, restrained
secondary forms, Omochao component/heart inheritance, skill-scaled companion
benefits, and refined Chaos anatomy.
See source/README.md for controls, source/TESTING.md for completed checks,
and source/docs/research for the inspected references and adaptations.

PREVIEWS
Extract the whole archive and open previews/index.html. It contains
{len(plan['features'])} feature views and {len(plan['motions'])} actual-frame
motion reviews. All {len(plan['capture_order'])} original full-frame PNGs and
recorded game ticks remain under source/docs/screenshots/release-{VERSION}.
The PDF gallery is source/docs/chao-garden-{VERSION}-preview-gallery.pdf.

SOURCE
The source includes exactly 41 asset-only supplied official inputs, converted
native resources, original additions, authoring tools, validation and retained
Blockbench projects. Private account/key files and the complete Marketplace
pack are not included. Assets retain their respective rights-holder credits.
""")


def main(previews_only=False,capture_log=None,project_root=None,output_dir=None,plan_path=None):
    root=Path(project_root).resolve() if project_root else ROOT
    out=Path(output_dir).resolve() if output_dir else OUT;out.mkdir(parents=True,exist_ok=True)
    plan=load_plan(root,plan_path);log=Path(capture_log) if capture_log else root.parent/'chao021-client.log'
    names,ticks,record=read_capture_session(log,plan)
    release=root/f'docs/screenshots/release-{VERSION}';release.mkdir(parents=True,exist_ok=True)
    frames={};captures=[]
    by_key={x['key']:x for x in plan['features']}
    for name in names:
        src=root/'run/screenshots'/name;size=a.validate_frame(src);dest=release/name;copy_flushed(src,dest)
        key=name.removeprefix(plan['filename_prefix']).removesuffix('.png');frames[key]=dest
        captures.append(dict(file=name,key=key,width=size[0],height=size[1],sha256=sha256(dest),game_tick=ticks[name],title=by_key[key]['title'] if key in by_key else key))
    for old in release.glob(plan['filename_prefix']+'*.png'):
        if old.name not in names:old.unlink()
    motions={};motion_records={}
    for m in plan['motions']:
        dest=release/(plan['filename_prefix']+m['key']+'-review.gif');motions[m['key']]=dest
        selected=m.get('review_frames',m['frames'])
        clock=[ticks[frames[key].name] for key in selected]
        motion_records[m['key']]=build_motion([frames[k] for k in selected],dest,m['key'],clock)
        motion_records[m['key']]['captured_frame_count']=len(m['frames'])
        omitted=[key for key in m['frames'] if key not in selected]
        motion_records[m['key']]['omitted_review_captures']=[dict(file=frames[key].name,sha256=sha256(frames[key]),game_tick=ticks[frames[key].name],reason=m['omission_reason']) for key in omitted]
        copy_flushed(dest,out/dest.name)
    manifest=dict(release=VERSION,minecraft='1.21.1',neoforge='21.1.219',capture_count=len(captures),
        origin='Actual Minecraft client screenshots',full_frames=True,capture_log_sha256=sha256(log),
        capture_directory=f'source/docs/screenshots/release-{VERSION}/',captures=captures,
        gallery_feature_count=len(plan['features']),motion_reviews=motion_records,
        scene_plan=plan)
    manifest_path=root/f'docs/release-capture-manifest-{VERSION}.json';record_path=root/f'docs/release-capture-record-{VERSION}.txt'
    write_flushed(manifest_path,(json.dumps(manifest,indent=2)+'\n').encode());write_flushed(record_path,record.encode())
    display=[frames[f['key']] for f in plan['features']]
    preview=out/f'chao-garden-{VERSION}-preview.png';build_feature_sheet(plan,frames,preview)
    copy_flushed(preview,root/'docs/screenshots'/preview.name)
    gallery=root/f'docs/release-gallery-{VERSION}.html';pictures=[*display,*motions.values()]
    build_gallery(plan,frames,motions,gallery,{str(p):os.path.relpath(p,gallery.parent).replace(os.sep,'/') for p in pictures})
    build_gallery(plan,frames,motions,out/f'chao-garden-{VERSION}-gallery.html')
    archive_gallery=out/f'chao-garden-{VERSION}-offline-gallery.html'
    build_gallery(plan,frames,motions,archive_gallery,{str(p):f'../source/docs/screenshots/release-{VERSION}/'+p.name for p in pictures})
    for original,file,label in HISTORIES:
        source=root/'docs'/original;require(source.is_file() and label in source.read_text(),'Historical gallery missing: '+original)
        write_flushed(root/'docs'/file,historical_launcher(label,original).encode())
        def embed_history(match):
            uri=match.group(2)
            if uri.startswith(('data:','http:','https:')):return match.group(0)
            picture=(source.parent/uri).resolve();require(picture.is_file(),'Missing historical image: '+str(picture))
            return match.group(1)+a.image_data(picture)+match.group(3)
        embedded=re.sub(r'(<img[^>]+src=")([^\"]+)(")',embed_history,source.read_text())
        write_flushed(out/file,embedded.encode())
    start=root/f'docs/START-HERE-{VERSION}.txt';write_flushed(start,start_here(plan).encode())
    if previews_only:
        print('Previews ready:',len(names),'whole frames;',len(plan['features']),'features;',len(motions),'motion reviews');return
    testing=(root/'TESTING.md').read_text().lower()
    require('validation in progress' not in testing and 'no client or artifact pass is asserted' not in testing and 'remain pending' not in testing,'Validation document is still a draft')
    pdf=root/f'docs/chao-garden-{VERSION}-preview-gallery.pdf';require(pdf.is_file() and pdf.read_bytes().startswith(b'%PDF-'),'Current preview PDF is missing')
    pdf_sources=json.loads((root/f'docs/pdf-source-manifest-{VERSION}.json').read_text())
    require(pdf_sources['sha256']==sha256(pdf) and [p['key'] for p in pdf_sources['pages']]==plan['pdf'],'PDF provenance differs from the current preview plan')
    require(pdf_sources.get('selected_page_metadata')==pdf_page_metadata(plan) and pdf_sources.get('selected_page_metadata_sha256')==pdf_page_metadata_sha256(plan),'PDF selected page metadata changed after rendering/review')
    source_hashes={p['key']:p['sha256'] for p in captures}
    require(all(p['sha256']==source_hashes[p['key']] for p in pdf_sources['pages']),'PDF uses a capture from an older study')
    source_jar=root/f'build/libs/chao_garden-{VERSION}.jar';validate_production_jar(source_jar,root,plan)
    jar=out/source_jar.name;copy_flushed(source_jar,jar);checksum=sha256(jar)
    projects=sorted((root/'art/blockbench').glob('*.bbmodel'));require(len(projects)>=8,'The eight retained editable projects must remain')
    for project in projects:
        data=json.loads(project.read_text());require(data.get('elements') and data.get('textures'),'Incomplete editable project: '+project.name)
        require(all(str(t.get('source','')).startswith('data:image/') for t in data['textures']),'Unembedded editable texture: '+project.name)
    entries=list(a.source_files(root));snapshot={rel:sha256(path) for path,rel in entries};png_paths={}
    for path,rel in sorted(entries,key=lambda x:(len(x[1]),x[1])):
        if rel.endswith('.png'):png_paths.setdefault(snapshot[rel],rel)
    archive=out/f'chao-garden-{VERSION}-complete.zip';staging=archive.with_name(archive.name+'.building');prefix=f'Chao-Garden-{VERSION}/'
    with zipfile.ZipFile(staging,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9,allowZip64=True) as z:
        z.write(start,prefix+'START-HERE.txt');z.write(jar,prefix+jar.name)
        for path,rel in entries:z.writestr(prefix+'source/'+rel,a.archive_source_bytes(path,rel,png_paths))
        for project in projects:z.write(project,prefix+'Blockbench/'+project.name)
        z.write(root/'art/blockbench/README.md',prefix+'Blockbench/EDITING.md');z.write(root/'docs/BLOCKBENCH_VALIDATION.md',prefix+'Blockbench/VALIDATION.md')
        z.write(archive_gallery,prefix+'previews/index.html')
        for original,file,label in HISTORIES:z.writestr(prefix+'previews/'+file,historical_launcher(label,'../source/docs/'+original))
        z.write(manifest_path,prefix+'previews/CAPTURE-MANIFEST.json');z.write(record_path,prefix+'previews/CAPTURE-RECORD.txt')
        z.write(root/'docs/MOD_VARIANTS.md',prefix+'previews/CURRENT-THEME-CATALOG.md')
        z.writestr(prefix+'SHA256SUMS.txt',checksum+'  '+jar.name+'\n')
    with zipfile.ZipFile(staging) as z:
        require(z.testzip() is None,'Complete ZIP CRC failure');nameset=set(z.namelist());require(len(nameset)==len(z.namelist()),'Duplicate archive entries')
        require(sum(n.startswith(prefix+f'source/docs/screenshots/release-{VERSION}/') and n.endswith('.png') for n in nameset)==len(captures),'Missing current raw capture')
        require(sum(n.startswith(prefix+'Blockbench/') and n.endswith('.bbmodel') for n in nameset)==len(projects),'Missing editable project')
        require(sum(n.startswith(prefix+'source/art/imported_chao/') for n in nameset)==41,'Imported source allowlist differs')
        for path,rel in entries:
            require(sha256(path)==snapshot[rel],'Source changed during packaging: '+rel)
            require(prefix+'source/'+rel in nameset,'Source file missing from ZIP: '+rel)
        for resource in (root/'src/main/resources').rglob('*'):
            if resource.is_file():require(z.read(prefix+'source/'+resource.relative_to(root).as_posix())==resource.read_bytes(),'ZIP resource differs from current source: '+str(resource))
        for key in a.IMPORTED_KEYS:
            coat=prefix+'source/src/main/resources/assets/chao_garden/textures/entity/chao/official/coats/'+key+'/'
            require(any(n.startswith(coat) and n.endswith('.png') for n in nameset),'Source inherited-coat folder missing: '+key)
        for g in (prefix+'previews/index.html',prefix+f'source/docs/release-gallery-{VERSION}.html'):
            images=re.findall(r'<img[^>]+src="([^"]+)"',z.read(g).decode())
            require(len(images)==len(plan['features'])+len(motions),'Gallery image count differs')
            for src in images:require(posixpath.normpath(posixpath.join(posixpath.dirname(g),src)) in nameset,'Missing offline gallery image: '+src)
        require(z.read(prefix+jar.name)==source_jar.read_bytes(),'ZIP JAR differs from current build')
        require(z.read(prefix+f'source/docs/chao-garden-{VERSION}-preview-gallery.pdf')==pdf.read_bytes(),'ZIP PDF differs from current preview')
        for capture in captures:require(hashlib.sha256(z.read(prefix+f'source/docs/screenshots/release-{VERSION}/'+capture['file'])).hexdigest()==capture['sha256'],'Capture hash differs in ZIP')
        for n in nameset:
            if n.startswith(prefix+'source/'):
                rel=n.removeprefix(prefix+'source/');require(Path(rel).parts[0] not in {'build','run','run-data','.gradle','dev-mods'},'Runtime/cache file in source ZIP')
                require(not rel.endswith('.jar') or rel=='gradle/wrapper/gradle-wrapper.jar','Third-party archive in source ZIP')
    with staging.open('rb') as f:os.fsync(f.fileno())
    os.replace(staging,archive);a.fsync_directory(out)
    sums=[(archive.name,sha256(archive)),(jar.name,checksum),(preview.name,sha256(preview)),*[(p.name,sha256(p)) for p in motions.values()]]
    write_flushed(out/f'chao-garden-{VERSION}-SHA256SUMS.txt',''.join(h+'  '+n+'\n' for n,h in sums).encode())
    print('Complete ZIP:',archive,archive.stat().st_size,'bytes;',len(nameset),'entries');print('ZIP SHA256:',sha256(archive));print('JAR SHA256:',checksum)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--previews-only',action='store_true');parser.add_argument('--capture-log',type=Path)
    parser.add_argument('--project-root',type=Path);parser.add_argument('--output-dir',type=Path);parser.add_argument('--plan',type=Path)
    args=parser.parse_args();main(args.previews_only,args.capture_log,args.project_root,args.output_dir,args.plan)
