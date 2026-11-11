"""Package Chao Garden 0.15.1 from a completed actual-client asset study.

Keep original full-frame PNGs once in source/docs/screenshots/release-0.15.1.
The gallery uses five feature frames plus an accelerated review GIF built only
from eight captured End portal frames. Private inspection inputs are never packaged.
"""
from pathlib import Path
import argparse, base64, hashlib, html, io, json, math, os, re, shutil, textwrap, zipfile
from PIL import Image, ImageDraw, ImageFont, ImageStat

SCRIPT = Path(__file__).resolve()
BASE = SCRIPT.parent
ROOT = BASE / 'chao-garden'
if not ROOT.is_dir() and SCRIPT.parent.name == 'scripts':
    ROOT = SCRIPT.parent.parent
    BASE = ROOT.parent
OUT = BASE / 'deliverables'
VERSION = '0.15.1'
SHOT_PREFIX = 'chao0151-'
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
INK, MUTED, PAPER = '#173f4b', '#47636e', '#edf3f5'
SHOTS = {
    'dimension-eggs-front': ('Dimension eggs - front', 'Netherrack shell and the live End portal surface',
        'The Nether shell uses a finer netherrack pattern. The End shell uses Minecraft\'s actual animated End portal shader across the rounded egg, without a purple rim.'),
    'dimension-eggs-rear': ('Dimension eggs - rear', 'Continuous shells from the opposite side',
        'This view checks the opposite side of the same dimension shells. The repaired egg geometry and texture wrap are retained; the End portal surface does not depend on a painted UV seam.'),
    'seed-catalog': ('Fruit-shaped seeds', 'Fourteen active fruit and seed pairs',
        'The redesigned seed silhouettes and small color accents echo their corresponding fruits. Fruit, seed and orchard behavior remain the same; this update changes their presentation.'),
    'nether-item': ('Nether egg item', 'Native pixel-art netherrack shell',
        'The inventory egg repeats the Nether shell\'s recognizable rock clusters and dark cracks at native sixteen-pixel size.'),
    'end-item': ('End egg item', 'Borderless dark portal motif',
        'The inventory egg uses a static dark portal motif without the old purple outline. Placing it shows the actual animated Minecraft End portal shader.'),
}
DISPLAY_KEYS = tuple(SHOTS)
PORTAL_KEYS = tuple('portal-' + str(i) for i in range(8))
ALL_KEYS = DISPLAY_KEYS + PORTAL_KEYS
CATEGORIES = {key: ('seeds' if key == 'seed-catalog' else 'items' if key.endswith('-item') else 'eggs')
              for key in DISPLAY_KEYS}
HISTORIES = (
    ('release-gallery-0.15.html','historical-0.15-garden-gallery.html','0.15.0'),
    ('icon-touchup-gallery.html','historical-0.14.1-icon-gallery.html','0.14.1'),
    ('collection-refinement-gallery.html','historical-0.14-collection-gallery.html','0.14.0'),
    ('icon-refinement-gallery.html','historical-0.13-icon-gallery.html','0.13'),
    ('detail-refinement-gallery.html','historical-0.12-refinement-gallery.html','0.12'),
    ('refinement-gallery.html','historical-0.11-refinement-gallery.html','0.11'),
    ('mechanics-gallery.html','historical-0.10-mechanics-gallery.html','0.10'),
    ('head-refinement-gallery.html','historical-0.9-head-gallery.html','0.9'),
)
REDUNDANT_COMPOSITES = {'chao-garden-' + version + '-preview.png' for version in
                        ('0.10.0','0.11.0','0.12.0','0.13.0','0.14.0','0.14.1','0.15.0',VERSION)}
IMPORTED_KEYS = ('baby','swim','fly','run','power','hero','hero_fly','hero_power','dark',
                 'dark_run','dark_swim','golden','baghead','dad','dragon','ender','tails',
                 'knuckles','emerald','creeper')
IMPORTED_ALLOWLIST = {'animations/chao.animation.json'} | {
    'models/chao_' + key + ('.json' if key == 'dad' else '.geo.json') for key in IMPORTED_KEYS
} | {'textures/' + key + '.png' for key in IMPORTED_KEYS}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)

def fsync_directory(path):
    descriptor = os.open(path, os.O_DIRECTORY)
    try:
        os.fsync(descriptor)
    finally:
        os.close(descriptor)

def write_flushed(target, payload):
    """Publish verified complete bytes without exposing a partial output."""
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_name(target.name + '.writing')
    with temporary.open('wb') as handle:
        handle.write(payload)
        handle.flush()
        os.fsync(handle.fileno())
    os.replace(temporary, target)
    fsync_directory(target.parent)

def copy_flushed(source, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_name(target.name + '.copying')
    with source.open('rb') as src, temporary.open('wb') as dst:
        shutil.copyfileobj(src, dst)
        dst.flush()
        os.fsync(dst.fileno())
    os.replace(temporary, target)
    fsync_directory(target.parent)
    require(sha256(source) == sha256(target), 'Copy mismatch: ' + str(target))

def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()

def save_png(picture, target):
    buffer = io.BytesIO()
    picture.save(buffer, format='PNG', optimize=True)
    payload = buffer.getvalue()
    require(payload.startswith(b'\x89PNG\r\n\x1a\n'), 'PNG encoder failure')
    with Image.open(io.BytesIO(payload)) as check:
        check.verify()
    write_flushed(target, payload)

def font(size):
    try:
        return ImageFont.truetype(FONT, size)
    except OSError:
        return ImageFont.load_default(size=size)

def wrap_pixels(draw, value, width, text_font):
    lines, current = [], ''
    for word in value.split():
        candidate = (current + ' ' + word).strip()
        if current and draw.textlength(candidate, font=text_font) > width:
            lines.append(current)
            current = word
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines

def validate_frame(path):
    with Image.open(path) as check:
        require(check.format == 'PNG', 'Not a PNG capture: ' + str(path))
        check.verify()
    with Image.open(path) as image:
        image.load()
        require(image.width >= 960 and image.height >= 540, 'Capture is too small: ' + str(path))
        rgb = image.convert('RGB')
        require(sum(ImageStat.Stat(rgb.resize((64, 36))).stddev) > 12, 'Capture is blank or nearly uniform: ' + str(path))
        return rgb.size

def read_capture_session(log_path, root):
    """Match one completed, current-harness run and every asynchronous save."""
    log = log_path.read_text(errors='replace')
    completion_pattern = r'CHAO_ASSET_STUDY_COMPLETE:\s*(\d+)\s*actual Minecraft views'
    captured_pattern = r'CHAO_ASSET_CAPTURED (chao0151-[a-z0-9-]+\.png) stage=(\d+)\b'
    zero_pattern = r'CHAO_ASSET_CAPTURED chao0151-[a-z0-9-]+\.png stage=0\b'
    completions = list(re.finditer(completion_pattern, log))
    require(bool(completions), 'No completed refinement study in ' + str(log_path))
    complete = completions[-1]
    lower = completions[-2].end() if len(completions) > 1 else 0
    starts = list(re.finditer(zero_pattern, log[lower:complete.start()]))
    require(bool(starts), 'The completed study has no initial capture')
    start = lower + starts[-1].start()
    require(re.search(zero_pattern, log[complete.end():]) is None,
            'A newer, incomplete refinement study follows the last completed one')
    segment = log[start:complete.start()]
    captured = re.findall(captured_pattern, segment)
    count = int(complete.group(1))
    require(len(captured) == count, f'Completion says {count} views but records {len(captured)} captures')
    require([int(stage) for _, stage in captured] == list(range(count)),
            'Capture stages are missing, duplicated or out of order')
    names = [name for name, _ in captured]
    require(len(names) == len(set(names)), 'Duplicate capture filenames')
    harness = (root / 'src/main/java/com/chaogarden/client/DimensionSeedVisualSmoke.java').read_text()
    array = re.search(r'String\[\]\s+CAPTURES\s*=\s*\{(.*?)\};', harness, re.S)
    require(array is not None, 'Could not read the current refinement capture list')
    expected = re.findall(r'"(chao0151-[a-z0-9-]+\.png)"', array.group(1))
    require(names == expected and count == len(expected),
            'The completed study does not match the current complete harness')
    for name in names:
        require(re.search(r'CHAO_ASSET_SCREENSHOT ' + re.escape(name)
                          + r':[^\n]*Saved screenshot', log[start:]) is not None,
                'Screenshot-save callback missing: ' + name)
        require(name.removeprefix(SHOT_PREFIX).removesuffix('.png') in ALL_KEYS,
                'Add a title/caption for the new screenshot: ' + name)
    require(names == [SHOT_PREFIX + key + '.png' for key in ALL_KEYS],
            'Release capture order differs from the expected five feature and eight portal frames')
    record = segment.strip() + '\nCHAO_ASSET_STUDY_COMPLETE: ' + str(count) + ' actual Minecraft views\n'
    return names, count, record

def validate_production_jar(source_jar, root):
    with zipfile.ZipFile(source_jar) as jar:
        names = jar.namelist()
        metadata = jar.read('META-INF/neoforge.mods.toml').decode()
        require(re.search(r'(?m)^version\s*=\s*"' + re.escape(VERSION) + r'"', metadata) is not None,
                'Production jar does not report version ' + VERSION)
        require(metadata.count('[[mods]]') == 1 and 'chao_variant_fixtures' not in metadata,
                'Fixture mod metadata leaked into production')
        catalog = json.loads(jar.read('assets/chao_garden/chao_variants.json'))
        active = {entry['id'] for entry in catalog['variants']}
        require(len(active) == 19, 'Production catalog does not contain the nineteen retained themes')
        for name in names:
            match = re.fullmatch(r'assets/chao_garden/textures/entity/(?:chao|eggs)/variant_([a-z0-9_]+)\.png', name)
            if match:
                require(match.group(1) in active, 'Retired theme texture remains in production JAR: ' + name)
            match = re.fullmatch(r'assets/chao_garden/textures/item/([a-z0-9_]+)_chao_egg\.png', name)
            if match:
                require(match.group(1) in active, 'Retired themed egg art remains in production JAR: ' + name)
            match = re.fullmatch(r'data/chao_garden/(?:recipe|advancement/recipes/chao_variants)/([a-z0-9_]+)_chao_egg\.json', name)
            if match:
                require(match.group(1) in active, 'Retired theme recipe remains in production JAR: ' + name)
        for name in names:
            require(not any(token in name for token in ('variantfixtures', 'ChaoVisualSmoke', 'MechanicsVisualSmoke', 'RefinementVisualSmoke', 'IconVisualSmoke', 'CollectionVisualSmoke', 'ReleaseVisualSmoke', 'DimensionSeedVisualSmoke',
                                                        'com/chaogarden/test/', 'data/chao_garden/structure/empty.nbt',
                                                        'data/chao_garden/structure/activity_test.nbt')),
                    'Development content in production jar: ' + name)
            require(not name.endswith(('.jar', '.bbmodel')), 'Third-party archive or editor project embedded in jar: ' + name)
        for resource in (root / 'src/main/resources').rglob('*'):
            if not resource.is_file() or resource.suffix == '.bbmodel':
                continue
            relative = resource.relative_to(root / 'src/main/resources').as_posix()
            if relative in ('data/chao_garden/structure/empty.nbt', 'data/chao_garden/structure/activity_test.nbt'):
                continue
            require(relative in names, 'Source resource absent from production jar: ' + relative)
            require(jar.read(relative) == resource.read_bytes(), 'Production resource differs from source: ' + relative)
        for required_class in ('fruit/FruitContent', 'genes/GeneContent', 'market/BlackMarketTrader',
                               'activities/ActivityManager', 'companion/CompanionContent', 'client/HeldChaoRenderer'):
            require('com/chaogarden/' + required_class + '.class' in names, 'Missing production feature: ' + required_class)
        compiled_root = root / 'build/classes/java/main'
        for name in names:
            if name.startswith('com/chaogarden/') and name.endswith('.class'):
                compiled = compiled_root / name
                require(compiled.is_file(), 'Production class lacks current compiled output: ' + name)
                require(jar.read(name) == compiled.read_bytes(), 'Production class differs from compiled output: ' + name)
        require(jar.testzip() is None, 'Production jar CRC failure')

def archive_source_bytes(path, relative, png_paths):
    """Keep historical HTML image pixels once when identical PNGs are included.

    Only the archived document changes its URL form. The local original stays
    untouched, and an unmatched embedded image remains embedded in full.
    """
    payload = path.read_bytes()
    if not relative.endswith('.html') or b'data:image/png;base64,' not in payload:
        return payload
    document = payload.decode('utf-8')
    parent = Path(relative).parent
    def replace(match):
        image = base64.b64decode(match.group(1), validate=True)
        target = png_paths.get(hashlib.sha256(image).hexdigest())
        if target is None:
            return match.group(0)
        return html.escape(os.path.relpath(target, parent).replace(os.sep, '/'), quote=True)
    return re.sub(r'data:image/png;base64,([A-Za-z0-9+/=]+)', replace, document).encode('utf-8')

def image_data(path):
    mime = 'image/gif' if path.suffix.lower() == '.gif' else 'image/png'
    return 'data:' + mime + ';base64,' + base64.b64encode(path.read_bytes()).decode('ascii')


def build_motion(frames, target):
    require(len(frames) == 8, 'The portal review sequence needs eight captured frames')
    hashes = [sha256(path) for path in frames]
    require(len(set(hashes)) == 8, 'Captured portal frames are not all distinct')
    pictures = []
    for path in frames:
        with Image.open(path) as source:
            picture = source.convert('RGB').resize((640,360), Image.Resampling.LANCZOS)
            pictures.append(picture.quantize(colors=128, method=Image.Quantize.MEDIANCUT))
    buffer = io.BytesIO()
    pictures[0].save(buffer, format='GIF', save_all=True, append_images=pictures[1:],
                     duration=250, loop=0, optimize=True, disposal=2)
    payload = buffer.getvalue()
    with Image.open(io.BytesIO(payload)) as check:
        require(check.format == 'GIF' and check.n_frames == 8, 'Incomplete portal GIF')
        require(check.size == (640,360), 'Incorrect portal GIF frame size')
    write_flushed(target,payload)
    return {'file':target.name,'sha256':sha256(target),'frame_count':8,'review_fps':4,
            'origin':'Eight distinct actual Minecraft frames sampled roughly 2.5 seconds apart; full frames resized for accelerated review',
            'minimum_client_tick_delay':40,'source_files':[path.name for path in frames],'source_sha256':hashes}


def start_here(count):
    return textwrap.dedent(f'''\
    CHAO GARDEN {VERSION}
    Minecraft Java 1.21.1 | NeoForge 21.1.219+ (21.1 series) | Java 21

    DIMENSION EGGS AND FRUIT-SHAPED SEEDS
    The Nether egg now has finer netherrack rock clusters and dark cracks.
    Placed End eggs use Minecraft's actual animated End portal shader over
    their whole rounded shell, with no purple rim. The inventory icon is a
    static portal motif. Seed icons have distinct outlines and colors that
    echo their fruit; recipes, crops and item IDs remain unchanged.

    INSTALL
    Replace the previous jar with chao_garden-{VERSION}.jar in mods/.
    Install the same version on the server and every client. Java 21 is needed.
    Adult equipment companions optionally require compatible NeoForge Accessories
    and its dependency. Third-party mods are not bundled. See source/README.md
    and source/TESTING.md for exact dependencies and completed validation.

    CONTINUING YOUR GARDEN
    Existing Chao, eggs, orchards, held Chao and companions keep their data.
    Five natural fruit families: Garden, Hero Garden, Dark Garden, Strong, Tasty.
    Nine market-first families: Hero, Dark, Round, Triangle, Square, Heart,
    Chao, Mushroom and Nutritious Mushroom. Buy rotating market fruit, craft
    that fruit into its seed, then plant renewable trees. New wild trees appear
    in newly generated suitable Overworld chunks.
    Craft the Care Guide with a book and wheat seeds for recipes and controls.
    The imported Chao rigs, nineteen themes, animal parts and three Chaos forms,
    transporter, contests, companions and garden CD from 0.15 remain included.

    PREVIEWS
    Extract the complete ZIP and open previews/index.html. Five current feature
    captures show both dimension eggs, items and the fourteen active seed pairs.
    Eight further frames show the same placed End egg from a fixed camera,
    sampled roughly 2.5 seconds apart. The GIF plays those full frames at four
    review frames per second, so it is an accelerated review sequence.
    All {count} original PNGs are in source/docs/screenshots/release-0.15.1/.
    PDF: source/docs/chao-garden-{VERSION}-preview-gallery.pdf
    Three pages show the dimension shells, live portal surface and seed catalog.
    The gallery works offline after extraction. Earlier galleries and their
    individual original frames keep their historical release labels. Redundant
    composite sheets and older PDFs are omitted to reduce duplicate data.

    SOURCE AND EDITING
    source/ contains code, tests, resources, generators and asset-only model
    inputs. art/imported_chao retains the exact selected twenty geometries,
    twenty texture maps and eight-clip animation file. Blockbench/ contains
    eight editable authoring projects for custom additions and the egg shell.
    Build caches, worlds, private inspection files and third-party jars are
    excluded. With Java 21, use ./gradlew build coreTest runGameTestServer.
    SHA256SUMS.txt records the installable jar checksum.

    CREDITS
    Unofficial fan project. Supplied Sonic/Chao assets belong to their rights
    holders. The new seed and netherrack pixels are authored project work;
    the placed End egg uses Minecraft's built-in renderer and textures.
    Source provenance and art references are recorded in source/docs/.
    Read source/LICENSE, source/TEMPLATE_LICENSE.txt and source/docs/AUDIO_CREDITS.md.
    ''')


def build_feature_sheet(paths,output):
    columns,tile_w,image_h,caption_h,margin,header_h=3,640,360,104,20,120
    rows=math.ceil(len(paths)/columns)
    picture=Image.new('RGB',(columns*tile_w+margin*2,header_h+rows*(image_h+caption_h)+margin),PAPER)
    draw=ImageDraw.Draw(picture)
    draw.text((margin,18),'Chao Garden 0.15.1 - Dimension eggs and fruit-shaped seeds',fill=INK,font=font(34))
    draw.text((margin,69),'5 actual game views - full frames resized to fit - Minecraft Java 1.21.1 / NeoForge',fill=MUTED,font=font(21))
    for index,path in enumerate(paths):
        key=path.stem.removeprefix(SHOT_PREFIX);title,subtitle,_=SHOTS[key]
        x=margin+index%columns*tile_w;y=header_h+index//columns*(image_h+caption_h)
        with Image.open(path) as source:
            shot=source.convert('RGB');shot.thumbnail((tile_w-8,image_h),Image.Resampling.LANCZOS)
            picture.paste(shot,(x+(tile_w-8-shot.width)//2,y+(image_h-shot.height)//2))
        title_font=font(23)
        draw.text((x+8,y+image_h+10),f'{index+1:02d}  {title}',fill=INK,font=title_font)
        for row,line in enumerate(wrap_pixels(draw,subtitle,tile_w-24,font(18))[:2]):
            draw.text((x+8,y+image_h+45+row*24),line,fill=MUTED,font=font(18))
    save_png(picture,output)


def historical_launcher(label,target):
    return ('<!doctype html><html lang="en"><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width,initial-scale=1">'
            '<title>Historical Chao Garden '+html.escape(label)+' gallery</title>'
            '<style>body{max-width:800px;margin:60px auto;padding:24px;font:18px/1.6 system-ui;background:#edf3f5;color:#173f4b}a{color:#146c85}</style>'
            '<h1>Historical Chao Garden '+html.escape(label)+' gallery</h1>'
            '<p>This study records an earlier release and may differ from the current 0.15.1 models and mechanics.</p>'
            '<p><a href="'+html.escape(target,quote=True)+'">Open the complete historical gallery</a></p>'
            '<p>Extract the complete ZIP before following this local link.</p></html>')


def build_gallery(paths,motion,output,image_urls=None):
    def url(path):return image_urls[str(path)] if image_urls is not None else image_data(path)
    cards=[]
    for index,path in enumerate(paths):
        key=path.stem.removeprefix(SHOT_PREFIX);title,subtitle,caption=SHOTS[key]
        search=html.escape(' '.join((title,subtitle,caption)).lower(),quote=True)
        cards.append(f'''<article class="card" data-category="{CATEGORIES[key]}" data-search="{search}"><button class="capture" type="button" aria-label="Enlarge {html.escape(title,quote=True)}"><img src="{url(path)}" alt="{html.escape(title,quote=True)}" loading="lazy"></button><div class="copy"><p class="number">{index+1:02d} - ACTUAL MINECRAFT FRAME</p><h2>{html.escape(title)}</h2><p class="subtitle">{html.escape(subtitle)}</p><p>{html.escape(caption)}</p></div></article>''')
    cards.append(f'''<article class="card" data-category="motion" data-search="end portal animation motion shader starfield"><button class="capture" type="button" aria-label="Enlarge portal motion review"><img src="{url(motion)}" alt="End portal - eight actual captured frames" loading="lazy"></button><div class="copy"><p class="number">06 - ACTUAL CAPTURED FRAMES</p><h2>End portal motion review</h2><p class="subtitle">Eight distinct actual client frames</p><p>The same placed End egg is sampled roughly 2.5 seconds apart. Full frames are resized to 640x360 and shown at four review frames per second, creating an accelerated review of the vanilla portal surface.</p></div></article>''')
    historical=' · '.join('<a href="'+filename+'">'+label+'</a>' for _,filename,label in HISTORIES)
    page='''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Chao Garden 0.15.1 - Dimension eggs and fruit-shaped seeds</title><style>
*{box-sizing:border-box}body{margin:0;background:#edf3f5;color:#173f4b;font:16px/1.6 system-ui,sans-serif}header,main,footer{max-width:1600px;margin:auto;padding:28px}header{padding-top:40px}h1{font-size:clamp(30px,4vw,48px);line-height:1.15;margin:0 0 20px}h2{font-size:25px;line-height:1.2;margin:0 0 12px}p{margin:0 0 14px}.number,.badge{font-size:12px;letter-spacing:.08em;color:#287559;font-weight:700}header>p{max-width:1050px}.controls{display:flex;gap:16px;flex-wrap:wrap;align-items:end;margin:24px 0 12px}.search{flex:1;min-width:230px}label{display:block;font-weight:650;font-size:14px;margin-bottom:6px}input,select,button{font:inherit}input,select{padding:12px;border:1px solid #9fbac4;border-radius:9px;background:white;color:#173f4b}input{width:100%}button:focus-visible,input:focus-visible,select:focus-visible,a:focus-visible{outline:3px solid #e39935;outline-offset:4px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,540px),1fr));gap:24px;padding-top:0}.card{background:white;border:1px solid #d4e2e7;border-radius:14px;overflow:hidden}.capture{display:block;width:100%;padding:0;border:0;background:#132c35;cursor:zoom-in}.capture img{display:block;width:100%;height:auto}.copy{padding:24px}.copy p:last-child{margin-bottom:0}.subtitle{font-weight:650;color:#47636e}.copy p{font-size:15px}#count,footer{color:#47636e}a{color:#146c85}dialog{width:min(96vw,1500px);max-height:96vh;padding:14px;border:0;border-radius:12px;background:#edf3f5;color:#173f4b}dialog::backdrop{background:#0c222ce8}dialog img{display:block;max-width:100%;max-height:76vh;width:auto;margin:auto}dialog p{text-align:center;margin:12px 0 0;font-size:14px}.actions{display:flex;justify-content:center;gap:12px;margin-top:12px}.actions button{border:1px solid #9fbac4;border-radius:7px;padding:8px 18px;color:#173f4b;background:white;cursor:pointer}[hidden]{display:none!important}@media(max-width:600px){header,main,footer{padding-left:16px;padding-right:16px}.copy{padding:18px}}
</style></head><body><header><p class="badge">CHAO GARDEN 0.15.1 - MINECRAFT JAVA 1.21.1 / NEOFORGE</p><h1>Dimension eggs. Distinct seeds.</h1><p>Netherrack shell art, the actual animated End portal shader and fruit-shaped seed icons refine the existing garden. The imported Chao models, animations and gameplay remain included.</p><p>Five whole game frames plus a portal motion sequence made from eight captured frames. The archive works offline after extraction; the separate standalone HTML embeds its images.</p><div class="controls"><div class="search"><label for="search">Find a preview</label><input id="search" type="search" placeholder="Try Nether, End, portal, seed..."></div><div><label for="filter">Show</label><select id="filter"><option value="all">All previews</option><option value="eggs">Placed eggs</option><option value="seeds">Fruit and seeds</option><option value="items">Egg items</option><option value="motion">Motion</option></select></div></div><p id="count" role="status" aria-live="polite"></p></header><main>'''+''.join(cards)+'''</main><footer><p>Current views are actual Minecraft captures. The End egg uses the built-in animated portal shader when placed; its inventory icon is static. See source/TESTING.md for completed validation and source/README.md for installation.</p><p>Historical studies: '''+historical+'''. Their release labels and original captures are retained.</p><p>Unofficial fan project. Supplied official models, textures, animation and audio belong to their rights holders. The source documents record provenance and the custom Minecraft adaptations.</p></footer><dialog id="zoom" aria-label="Enlarged preview"><img alt=""><p></p><div class="actions"><button id="previous" type="button">Previous</button><button id="close" type="button">Close</button><button id="next" type="button">Next</button></div></dialog><script>
const cards=[...document.querySelectorAll('.card')],search=document.querySelector('#search'),filter=document.querySelector('#filter'),count=document.querySelector('#count'),zoom=document.querySelector('#zoom');let current=0;
function visible(){return cards.filter(card=>!card.hidden)}function update(){const query=search.value.trim().toLowerCase();for(const card of cards)card.hidden=!(filter.value==='all'||card.dataset.category===filter.value)||!card.dataset.search.includes(query);count.textContent=visible().length+' previews - select an image to enlarge';}function show(card){const image=card.querySelector('img');current=cards.indexOf(card);zoom.querySelector('img').src=image.src;zoom.querySelector('img').alt=image.alt;zoom.querySelector('p').textContent=card.querySelector('h2').textContent+' - '+card.querySelector('.subtitle').textContent;if(!zoom.open)zoom.showModal();}function step(offset){const list=visible();if(!list.length)return;show(list[(list.indexOf(cards[current])+offset+list.length)%list.length]);}search.addEventListener('input',update);filter.addEventListener('change',update);for(const card of cards)card.querySelector('.capture').addEventListener('click',()=>show(card));document.querySelector('#close').addEventListener('click',()=>zoom.close());document.querySelector('#previous').addEventListener('click',()=>step(-1));document.querySelector('#next').addEventListener('click',()=>step(1));zoom.addEventListener('click',event=>{if(event.target===zoom)zoom.close()});zoom.addEventListener('keydown',event=>{if(event.key==='ArrowLeft'){event.preventDefault();step(-1)}if(event.key==='ArrowRight'){event.preventDefault();step(1)}});update();
</script></body></html>'''
    require('http://' not in page and 'https://' not in page,'Gallery acquired an external dependency')
    require(page.count('<article class="card"')==6,'Missing current gallery cards')
    write_flushed(output,page.encode())


def source_files(root):
    excluded={'.gradle','build','run','run-data','dev-mods','thirdpartyjars','third-party-jars',
              '__pycache__','.git','.idea','.vscode','node_modules','.cache','tmp','.sonic-private-inspection'}
    found=set()
    for directory,dirs,files in os.walk(root):
        dirs[:]=sorted(name for name in dirs if name not in excluded and not name.startswith('.library-transfer'))
        for name in sorted(files):
            path=Path(directory)/name;relative=path.relative_to(root).as_posix()
            lowered=relative.lower()
            require(not any(part in lowered for part in ('entoptions','entitlement','keymaterial','.sonic-private-inspection')),
                    'Private inspection input found in source: '+relative)
            require(name.lower() not in {'options.txt','options.json','keys.json','key.txt','entoptions.txt'},
                    'Options/key input found in source: '+relative)
            require(not name.lower().endswith(('.key','.pem','.pfx','.p12','.mcworld','.mcpack','.mctemplate')),
                    'Private/archive input found in source: '+relative)
            if relative.startswith('art/imported_chao/'):
                asset=relative.removeprefix('art/imported_chao/')
                require(asset in IMPORTED_ALLOWLIST,'Non-whitelisted imported source asset: '+relative)
                found.add(asset)
            if name.endswith(('.pyc','.log','.writing','.copying','.tmp','.zip','.gif.building')):continue
            # Composite feature sheets repeat the individually preserved frames.
            # Keep the standalone deliverable, but omit these from the source ZIP.
            if relative.startswith('docs/screenshots/') and name in REDUNDANT_COMPOSITES:continue
            if relative.startswith('docs/chao-garden-') and relative.endswith('-preview-gallery.pdf') and VERSION not in name:continue
            if name.endswith('.jar') and relative!='gradle/wrapper/gradle-wrapper.jar':continue
            require(not path.is_symlink(),'Refusing to package a source symlink: '+relative)
            yield path,relative
    require(found==IMPORTED_ALLOWLIST,'Imported authoring inputs do not match the exact 41-file allowlist')


def main(previews_only=False,capture_log=None,project_root=None,output_dir=None):
    root=Path(project_root).resolve() if project_root else ROOT
    out=Path(output_dir).resolve() if output_dir else OUT
    log_path=Path(capture_log).resolve() if capture_log else root.parent/'chao0151-client.log'
    out.mkdir(parents=True,exist_ok=True)
    screenshots=root/'docs/screenshots';release=screenshots/'release-0.15.1';release.mkdir(parents=True,exist_ok=True)
    names,count,capture_record=read_capture_session(log_path,root)
    require(count==13,'The release needs five feature and eight portal captures')
    records=[];frames={}
    for name in names:
        source=root/'run/screenshots'/name;size=validate_frame(source);target=release/name
        copy_flushed(source,target);key=target.stem.removeprefix(SHOT_PREFIX);frames[key]=target
        records.append({'file':name,'width':size[0],'height':size[1],'sha256':sha256(target),
                        'title':SHOTS[key][0] if key in SHOTS else 'Captured portal frame '+key.split('-')[1]})
    for obsolete in release.glob('chao0151-*.png'):
        if obsolete.name not in names:obsolete.unlink()
    motion=release/'chao0151-portal-review.gif'
    motion_record=build_motion([frames[key] for key in PORTAL_KEYS],motion)
    timings = re.findall(r'CHAO_ASSET_CAPTURED (chao0151-portal-[0-7]\.png) stage=\d+ gameTick=(\d+) egg=([^\s]+)', capture_record)
    require([name for name,_,_ in timings] == [SHOT_PREFIX+key+'.png' for key in PORTAL_KEYS], 'Missing portal capture timing/identity')
    ticks = [int(tick) for _,tick,_ in timings]
    require(len({identity for _,_,identity in timings}) == 1, 'Portal review changed egg identity')
    require(all(b > a for a,b in zip(ticks,ticks[1:])), 'Portal capture clock did not advance')
    motion_record.update({'source_game_ticks':ticks, 'source_interval_game_ticks':[b-a for a,b in zip(ticks,ticks[1:])], 'same_entity':True})
    manifest={'release':VERSION,'minecraft':'1.21.1','neoforge':'21.1.219','capture_count':count,
              'origin':'Actual Minecraft client screenshots','full_frames':True,'capture_log_sha256':sha256(log_path),
              'capture_directory':'source/docs/screenshots/release-0.15.1/','captures':records,
              'gallery_feature_count':5,'motion_review':motion_record}
    manifest_path=root/'docs/release-capture-manifest-0.15.1.json';record_path=root/'docs/release-capture-record-0.15.1.txt'
    write_flushed(manifest_path,(json.dumps(manifest,indent=2)+'\n').encode());write_flushed(record_path,capture_record.encode())
    display=[frames[key] for key in DISPLAY_KEYS];preview=out/f'chao-garden-{VERSION}-preview.png'
    build_feature_sheet(display,preview);copy_flushed(preview,screenshots/preview.name)
    gallery=root/'docs/release-gallery-0.15.1.html'
    local_urls={str(p):os.path.relpath(p,gallery.parent).replace(os.sep,'/') for p in [*display,motion]}
    build_gallery(display,motion,gallery,local_urls)
    build_gallery(display,motion,out/f'chao-garden-{VERSION}-gallery.html')
    archive_gallery=out/f'chao-garden-{VERSION}-offline-gallery.html'
    archive_urls={str(p):'../source/docs/screenshots/release-0.15.1/'+p.name for p in [*display,motion]}
    build_gallery(display,motion,archive_gallery,archive_urls);copy_flushed(motion,out/motion.name)
    histories=[]
    for original,filename,label in HISTORIES:
        history_source=root/'docs'/original
        require(label in history_source.read_text(),'Historical gallery lost its release label: '+original)
        historical=root/'docs'/filename
        write_flushed(historical,historical_launcher(label,original).encode())
        history = history_source.read_text()
        def embed_history(match):
            source = match.group(2)
            if source.startswith(('data:','http:','https:')):return match.group(0)
            picture=(history_source.parent/source).resolve()
            require(picture.is_file(), 'Missing historical image: '+str(picture))
            return match.group(1)+image_data(picture)+match.group(3)
        history = re.sub(r'(<img[^>]+src=")([^"]+)(")',embed_history,history)
        write_flushed(out/filename,history.encode());histories.append((filename,label,original))
    start_path=root/'docs/START-HERE-0.15.1.txt';write_flushed(start_path,start_here(count).encode())
    script_copy=root/'scripts/package_chao0151.py'
    if SCRIPT!=script_copy.resolve():copy_flushed(SCRIPT,script_copy)
    if previews_only:
        print('Previews ready:',count,'validated whole game frames; five-view gallery, eight-frame GIF and feature sheet.')
        print('Gallery:',gallery);print('Capture manifest:',manifest_path);return
    current_pdf=root/f'docs/chao-garden-{VERSION}-preview-gallery.pdf'
    require(current_pdf.is_file() and current_pdf.read_bytes().startswith(b'%PDF-'),'Current PDF gallery is missing')
    jar=out/f'chao_garden-{VERSION}.jar';source_jar=root/'build/libs'/jar.name
    validate_production_jar(source_jar,root);copy_flushed(source_jar,jar);checksum=sha256(jar)
    projects=sorted((root/'art/blockbench').glob('*.bbmodel'))
    require(len(projects)==8,'Expected the eight retained editable authoring projects')
    for project in projects:
        data=json.loads(project.read_text());require(data.get('elements') and data.get('textures'),'Incomplete Blockbench project: '+project.name)
        require(all(str(t.get('source','')).startswith('data:image/') for t in data['textures']),'Blockbench texture is not embedded')
    archive=out/f'chao-garden-{VERSION}-complete.zip';staging=archive.with_name(archive.name+'.building');prefix=f'Chao-Garden-{VERSION}/'
    entries_to_add=list(source_files(root));png_paths={}
    for path,relative in sorted(entries_to_add,key=lambda pair:(len(pair[1]),pair[1])):
        if relative.endswith('.png'):png_paths.setdefault(sha256(path),relative)
    with zipfile.ZipFile(staging,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as bundle:
        bundle.write(start_path,prefix+'START-HERE.txt');bundle.write(jar,prefix+jar.name)
        for path,relative in entries_to_add:
            if relative=='docs/historical-0.9-head-gallery.html':
                payload=historical_launcher('0.9','head-refinement-gallery.html').encode()
            else:payload=archive_source_bytes(path,relative,png_paths)
            bundle.writestr(prefix+'source/'+relative,payload)
        for project in projects:bundle.write(project,prefix+'Blockbench/'+project.name)
        bundle.write(root/'art/blockbench/README.md',prefix+'Blockbench/EDITING.md')
        bundle.write(root/'docs/BLOCKBENCH_VALIDATION.md',prefix+'Blockbench/VALIDATION.md')
        bundle.write(archive_gallery,prefix+'previews/index.html')
        for filename,label,original in histories:
            bundle.writestr(prefix+'previews/'+filename,historical_launcher(label,'../source/docs/'+original))
        bundle.write(manifest_path,prefix+'previews/CAPTURE-MANIFEST.json');bundle.write(record_path,prefix+'previews/CAPTURE-RECORD.txt')
        bundle.write(root/'docs/MOD_VARIANTS.md',prefix+'previews/CURRENT-THEME-CATALOG.md')
        bundle.writestr(prefix+'SHA256SUMS.txt',checksum+'  '+jar.name+'\n')
    with zipfile.ZipFile(staging) as bundle:
        require(bundle.testzip() is None,'Complete ZIP CRC failure');entries=bundle.namelist();nameset=set(entries)
        require(len(entries)==len(nameset),'Duplicate complete ZIP entries')
        require(sum(n.startswith(prefix+'source/docs/screenshots/release-0.15.1/') and n.endswith('.png') for n in entries )==13,'Missing release PNG capture')
        require(sum(n.startswith(prefix+'Blockbench/') and n.endswith('.bbmodel') for n in entries)==8,'Missing Blockbench authoring project')
        require(sum(n.startswith(prefix+'source/art/imported_chao/') for n in entries)==41,'Missing or extra imported asset input')
        require(prefix+'source/gradle/wrapper/gradle-wrapper.jar' in nameset,'Missing Gradle wrapper')
        import posixpath
        for gallery_name in (prefix+'previews/index.html',prefix+'source/docs/release-gallery-0.15.1.html'):
            sources=re.findall(r'<img[^>]+src="([^"]+)"',bundle.read(gallery_name).decode())
            require(len(sources)==6,'Current gallery image count mismatch')
            for source in sources:
                resolved=posixpath.normpath(posixpath.join(posixpath.dirname(gallery_name),source))
                require(resolved in nameset,'Offline gallery references missing image: '+resolved)
        require(bundle.read(prefix+jar.name)==source_jar.read_bytes(),'ZIP jar differs from current build')
        require(bundle.read(prefix+f'source/docs/chao-garden-{VERSION}-preview-gallery.pdf')==current_pdf.read_bytes(),'ZIP PDF differs from current preview')
        for record in records:
            require(hashlib.sha256(bundle.read(prefix+'source/docs/screenshots/release-0.15.1/'+record['file'])).hexdigest()==record['sha256'],'ZIP capture hash mismatch')
        for name in entries:
            require(not any(term in name.lower() for term in ('entoptions','entitlement','keymaterial','.sonic-private-inspection')),'Private input in ZIP path')
            if name.startswith(prefix+'source/'):
                relative=name.removeprefix(prefix+'source/')
                require(not ({'build','run','.gradle','dev-mods'} & set(Path(relative).parts)),'Runtime/cache content in source ZIP')
                require(not relative.endswith('.jar') or relative=='gradle/wrapper/gradle-wrapper.jar','Third-party jar in source ZIP')
    size=staging.stat().st_size
    if size>50*1024*1024:print('Delivery size notice:',size,'bytes; larger than the preferred 50 MiB target')
    require(size<=75*1024*1024,'Complete ZIP exceeds 75 MiB; inspect archive contents before publishing')
    with staging.open('rb') as handle:os.fsync(handle.fileno())
    os.replace(staging,archive);fsync_directory(out)
    sums=[(archive.name,sha256(archive)),(jar.name,checksum),(preview.name,sha256(preview)),(motion.name,sha256(motion))]
    write_flushed(out/f'chao-garden-{VERSION}-SHA256SUMS.txt',''.join(digest+'  '+name+'\n' for name,digest in sums).encode())
    print('Complete ZIP:',archive,size,'bytes;',len(entries),'entries');print('ZIP SHA256:',sha256(archive))
    print('JAR SHA256:',checksum);print('Gallery:',out/f'chao-garden-{VERSION}-gallery.html');print('Motion:',motion)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--previews-only',action='store_true')
    parser.add_argument('--capture-log',type=Path,help='Default: project parent/chao0151-client.log')
    parser.add_argument('--project-root',type=Path)
    parser.add_argument('--output-dir',type=Path)
    args=parser.parse_args();main(args.previews_only,args.capture_log,args.project_root,args.output_dir)
