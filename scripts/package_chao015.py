"""Package Chao Garden 0.15.0 from a completed actual-client release study.

Keep original full-frame PNGs once in source/docs/screenshots/release-0.15.
The gallery uses fifteen feature frames plus a review GIF built only from the
eight captured idle frames. Private inspection inputs are never packaged.
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
VERSION = '0.15.0'
SHOT_PREFIX = 'chao015-'
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
INK, MUTED, PAPER = '#173f4b', '#47636e', '#edf3f5'
SHOTS = {
    'main-forms': ('Official Chao forms', 'Imported models, textures and animation source',
                   'The main Chao forms use the supplied official addon geometry and textures. The Minecraft renderer preserves their cuboids, pivots and animation structure.'),
    'ability-forms': ('Ability and alignment forms', 'Swim, Fly, Run, Power, Hero and Dark forms',
                      'Ability and alignment forms use their corresponding imported rigs. The runtime selects a form from the saved Chao state.'),
    'tech-themes': ('Equipment-themed Chao', 'Retained mod tributes fitted to the imported bodies',
                    'The retained technology tributes keep their distinct crowns, accessories and badges, fitted to the imported Chao skeletons.'),
    'magic-themes': ('Magic-themed Chao', 'Retargeted crowns, goggles and theme badges',
                     'The retained magic themes use the imported bodies and motion. Their custom heads and matching badges follow the existing mod availability rules.'),
    'extra-themes': ('More retained themes', 'Adventure, dimension and creature tributes',
                     'The retained adventure and dimension appearances keep their own details on the new bodies. There are nineteen themes in the full collection.'),
    'genes-chaos': ('Animal parts and Chaos forms', 'Retargeted animal additions, Light, Angel and Devil Chaos',
                    'The twenty-one animal-gene sources retain their training and mixed-part behavior. Their additions and the three Chaos forms follow the imported skeletons.'),
    'transporter-trader': ('Garden transporter and trader', 'PC Chao transporter styling and the Black Market',
                           'The Garden Block now resembles the PC Chao transporter while retaining garden enrollment and activity access. The Black Market rotates special fruit and natural-seed stock.'),
    'eggs-front': ('Egg shell front view', 'Continuous shell texture across the rounded egg',
                   'The egg shell UVs have been corrected so the texture follows the curved shell without the earlier seam at the wrap boundary.'),
    'eggs-rear': ('Egg shell rear view', 'A second angle on the repaired texture wrap',
                  'This rear view checks the same placed eggs from the opposite side. The corrected shell mapping joins the texture at the longitude boundary.'),
    'natural-orchard': ('Five naturally introduced fruits', 'Garden, Hero Garden, Dark Garden, Strong and Tasty',
                        'Five SA2-inspired fruit families enter through natural trees. The orchard shows their Minecraft tree forms and hanging fruit.'),
    'market-orchard': ('Nine market-first fruit families', 'Buy special fruit, then craft its seed for a wider orchard',
                       'Nine additional fruit families first appear as rotating Black Market fruit offers. Craft the purchased fruit into seeds to grow the same renewable hanging-fruit system.'),
    'fruit-catalog': ('Fourteen SA2 fruit families', 'Current fruit and seed items',
                      'The active catalog contains five natural families and nine market-first families. Older removed fruit IDs remain compatible with existing saves.'),
    'black-market': ('Rotating Black Market stock', 'Emerald trades, special fruit and natural seeds',
                     'The Chao-like merchant uses emeralds. Three market fruits and two natural seeds rotate through each stock. Market fruit seeds come only from crafting the obtained fruit.'),
    'held-chao': ('Hold your own Chao', 'Shift-right-click pickup and the animated held companion',
                  'Shift-right-clicking your Chao creates its filled carrier directly. Holding it displays the animated saved Chao; an empty carrier is no longer an obtainable item.'),
    'music-disc': ('Chao Garden CD', 'Vanilla jukebox item - about 115.81 seconds',
                   'The CD combines the supplied Neutral Garden introduction with one full loop. It uses vanilla jukebox insertion, playback and ejection behavior.'),
}
DISPLAY_KEYS = tuple(SHOTS)
IDLE_KEYS = tuple('idle-' + str(i) for i in range(8))
ALL_KEYS = DISPLAY_KEYS + IDLE_KEYS
CATEGORIES = {key: ('forms' if key in ('main-forms','ability-forms','genes-chaos') else
                    'themes' if key.endswith('themes') else 'eggs' if key.startswith('eggs-') else
                    'orchard' if key in ('natural-orchard','market-orchard','fruit-catalog','black-market') else
                    'companions') for key in DISPLAY_KEYS}
HISTORIES = (
    ('icon-touchup-gallery.html','historical-0.14.1-icon-gallery.html','0.14.1'),
    ('collection-refinement-gallery.html','historical-0.14-collection-gallery.html','0.14.0'),
    ('icon-refinement-gallery.html','historical-0.13-icon-gallery.html','0.13'),
    ('detail-refinement-gallery.html','historical-0.12-refinement-gallery.html','0.12'),
    ('refinement-gallery.html','historical-0.11-refinement-gallery.html','0.11'),
    ('mechanics-gallery.html','historical-0.10-mechanics-gallery.html','0.10'),
    ('head-refinement-gallery.html','historical-0.9-head-gallery.html','0.9'),
)
REDUNDANT_COMPOSITES = {'chao-garden-' + version + '-preview.png' for version in
                        ('0.10.0','0.11.0','0.12.0','0.13.0','0.14.0','0.14.1',VERSION)}
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
    completion_pattern = r'CHAO_RELEASE_STUDY_COMPLETE:\s*(\d+)\s*actual Minecraft views'
    captured_pattern = r'CHAO_RELEASE_CAPTURED (chao015-[a-z0-9-]+\.png) stage=(\d+)\b'
    zero_pattern = r'CHAO_RELEASE_CAPTURED chao015-[a-z0-9-]+\.png stage=0\b'
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
    harness = (root / 'src/main/java/com/chaogarden/client/ReleaseVisualSmoke.java').read_text()
    array = re.search(r'String\[\]\s+CAPTURES\s*=\s*\{(.*?)\};', harness, re.S)
    require(array is not None, 'Could not read the current refinement capture list')
    expected = re.findall(r'"(chao015-[a-z0-9-]+\.png)"', array.group(1))
    require(names == expected and count == len(expected),
            'The completed study does not match the current complete harness')
    for name in names:
        require(re.search(r'CHAO_RELEASE_SCREENSHOT ' + re.escape(name)
                          + r':[^\n]*Saved screenshot', log[start:]) is not None,
                'Screenshot-save callback missing: ' + name)
        require(name.removeprefix(SHOT_PREFIX).removesuffix('.png') in ALL_KEYS,
                'Add a title/caption for the new screenshot: ' + name)
    require(names == [SHOT_PREFIX + key + '.png' for key in ALL_KEYS],
            'Release capture order differs from the expected fifteen feature and eight idle frames')
    record = segment.strip() + '\nCHAO_RELEASE_STUDY_COMPLETE: ' + str(count) + ' actual Minecraft views\n'
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
            require(not any(token in name for token in ('variantfixtures', 'ChaoVisualSmoke', 'MechanicsVisualSmoke', 'RefinementVisualSmoke', 'IconVisualSmoke', 'CollectionVisualSmoke', 'ReleaseVisualSmoke',
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
    require(len(frames) == 8, 'The idle review sequence needs eight captured frames')
    hashes = [sha256(path) for path in frames]
    require(len(set(hashes)) == 8, 'Captured idle frames are not all distinct')
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
        require(check.format == 'GIF' and check.n_frames == 8, 'Incomplete idle GIF')
        require(check.size == (640,360), 'Incorrect idle GIF frame size')
    write_flushed(target,payload)
    return {'file':target.name,'sha256':sha256(target),'frame_count':8,'review_fps':4,
            'origin':'Eight distinct actual Minecraft frames; full frames resized for a review sequence',
            'source_files':[path.name for path in frames],'source_sha256':hashes}


def start_here(count):
    return textwrap.dedent(f'''\
    CHAO GARDEN {VERSION}
    Minecraft Java 1.21.1 | NeoForge 21.1.219+ (21.1 series) | Java 21

    OFFICIAL FORMS AND GARDEN REFINEMENTS
    Main Chao forms now use the supplied official addon models, textures and
    animation source. Nineteen retained mod themes, twenty-one animal-gene
    sources and Light/Angel/Devil Chaos additions follow the imported skeletons.
    Egg shell texture wrapping is repaired. Fourteen SA2-inspired fruit families
    have five natural sources and nine market-first families with rotating fruit
    offers. The Garden Block has a PC Chao transporter appearance.
    Shift-right-click pickup directly creates the filled carrier; an empty
    carrier is no longer obtainable. The new Chao Garden CD plays the Neutral
    Garden introduction plus one full loop, about 115.81 seconds in a jukebox.

    INSTALL
    Replace the previous jar with chao_garden-{VERSION}.jar in mods/.
    Install the same version on the server and every client. Java 21 is needed.
    Optional adult equipment companions require compatible NeoForge Accessories
    and its dependency. Third-party mods are not bundled. See source/README.md
    and source/TESTING.md for exact dependencies and completed validation.

    START PLAYING
    Craft a Normal Chao Egg with egg + emerald + wheat seeds + oak sapling.
    Place it to incubate. The default hatch time is five active minutes.
    Craft the Care Guide with a book + wheat seeds for recipes and mechanics.
    Empty-hand right-click pets/claims an unowned Chao. Sneak + empty-hand
    right-click picks up your own Chao directly. Use its filled carrier on a
    block to release it into clear space. Holding a filled carrier displays
    the saved Chao with animation. Ownership and duplicate-UUID checks apply.
    Right-click with fruit, a drive or a DNA sample to give a care item.
    Use a Garden Bell to link a Chao to a garden. Right-click the transporter
    Garden Block for its roster and garden enrollment. Race and karate lobbies
    remain available through their starting line and complete 7x7 mat.

    FRUITS, MARKET AND MUSIC
    Five natural families: Garden, Hero Garden, Dark Garden, Strong and Tasty.
    Nine market-first families: Hero, Dark, Round, Triangle, Square, Heart,
    Chao, Mushroom and Nutritious Mushroom. Buy rotating fruit offers with
    emeralds, craft each obtained fruit into its seed, then grow renewable trees. Existing saved legacy crops stay
    compatible; see source/MECHANICS.md for the precise migration rules.
    Craft the Chao Garden CD with eight iron nuggets around an emerald.
    It uses normal vanilla jukebox playback and can be ejected and reused.

    PREVIEWS
    Extract the complete ZIP and open previews/index.html. The gallery has
    fifteen current full-frame feature captures and a motion review GIF made
    from eight distinct captured idle frames. All {count} original PNG frames
    are included once in source/docs/screenshots/release-0.15/.
    The GIF uses full frames resized to 640x360 at four review frames per second;
    it is a review sequence, not a claim about the original capture timing.
    PDF: source/docs/chao-garden-{VERSION}-preview-gallery.pdf
    Eight pages show selected current game views, including the held Chao and
    music disc. The HTML gallery opens offline with its included local images.
    Optional themes are shown with development identity fixtures; these views
    do not establish compatibility with complete third-party modpacks.
    Historical previews retain their original release labels. Redundant
    composite feature sheets are omitted; their individual frames remain.

    SOURCE AND EDITING
    source/ contains code, tests, resources, generators and asset-only model
    inputs. art/imported_chao contains only the selected geometry, textures and
    animation input. Imported source JSON and generated overlays are editable.
    Blockbench/ retains eight editable authoring projects for custom additions;
    those older authoring rigs are not substitutes for the imported JSON forms.
    Build caches, worlds, private inspection files and third-party jars are
    excluded. With Java 21, use ./gradlew build coreTest runGameTestServer.
    SHA256SUMS.txt records the installable jar checksum.

    CREDITS
    Unofficial fan project. Chao/Sonic Adventure and the supplied official addon
    assets belong to their rights holders. The import preserves supplied model,
    texture and animation inputs; custom themes and Minecraft features are
    project adaptations. Audio and source provenance appear in source/docs/.
    Read source/LICENSE, source/TEMPLATE_LICENSE.txt and source/docs/AUDIO_CREDITS.md.
    ''')


def build_feature_sheet(paths,output):
    columns,tile_w,image_h,caption_h,margin,header_h=3,640,360,104,20,120
    rows=math.ceil(len(paths)/columns)
    picture=Image.new('RGB',(columns*tile_w+margin*2,header_h+rows*(image_h+caption_h)+margin),PAPER)
    draw=ImageDraw.Draw(picture)
    draw.text((margin,18),'Chao Garden 0.15.0 - Official forms and garden refinements',fill=INK,font=font(34))
    draw.text((margin,69),'15 actual game views - full frames resized to fit - Minecraft Java 1.21.1 / NeoForge',fill=MUTED,font=font(21))
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
            '<p>This study records an earlier release and may differ from the current 0.15.0 models and mechanics.</p>'
            '<p><a href="'+html.escape(target,quote=True)+'">Open the complete historical gallery</a></p>'
            '<p>Extract the complete ZIP before following this local link.</p></html>')


def build_gallery(paths,motion,output,image_urls=None):
    def url(path):return image_urls[str(path)] if image_urls is not None else image_data(path)
    cards=[]
    for index,path in enumerate(paths):
        key=path.stem.removeprefix(SHOT_PREFIX);title,subtitle,caption=SHOTS[key]
        search=html.escape(' '.join((title,subtitle,caption)).lower(),quote=True)
        cards.append(f'''<article class="card" data-category="{CATEGORIES[key]}" data-search="{search}"><button class="capture" type="button" aria-label="Enlarge {html.escape(title,quote=True)}"><img src="{url(path)}" alt="{html.escape(title,quote=True)}" loading="lazy"></button><div class="copy"><p class="number">{index+1:02d} - ACTUAL MINECRAFT FRAME</p><h2>{html.escape(title)}</h2><p class="subtitle">{html.escape(subtitle)}</p><p>{html.escape(caption)}</p></div></article>''')
    cards.append(f'''<article class="card" data-category="motion" data-search="idle animation movement official animation motion captured poses"><button class="capture" type="button" aria-label="Enlarge idle motion review"><img src="{url(motion)}" alt="Idle animation - eight actual captured poses" loading="lazy"></button><div class="copy"><p class="number">16 - ACTUAL CAPTURED POSES</p><h2>Idle motion review</h2><p class="subtitle">Eight distinct actual client frames</p><p>Complete frames resized to 640x360 and played at four review frames per second. This sequence shows captured motion poses; its timing is chosen for review.</p></div></article>''')
    historical=' · '.join('<a href="'+filename+'">'+label+'</a>' for _,filename,label in HISTORIES)
    page='''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Chao Garden 0.15.0 - Official forms and garden refinements</title><style>
*{box-sizing:border-box}body{margin:0;background:#edf3f5;color:#173f4b;font:16px/1.6 system-ui,sans-serif}header,main,footer{max-width:1600px;margin:auto;padding:28px}header{padding-top:40px}h1{font-size:clamp(30px,4vw,48px);line-height:1.15;margin:0 0 20px}h2{font-size:25px;line-height:1.2;margin:0 0 12px}p{margin:0 0 14px}.number,.badge{font-size:12px;letter-spacing:.08em;color:#287559;font-weight:700}header>p{max-width:1050px}.controls{display:flex;gap:16px;flex-wrap:wrap;align-items:end;margin:24px 0 12px}.search{flex:1;min-width:230px}label{display:block;font-weight:650;font-size:14px;margin-bottom:6px}input,select,button{font:inherit}input,select{padding:12px;border:1px solid #9fbac4;border-radius:9px;background:white;color:#173f4b}input{width:100%}button:focus-visible,input:focus-visible,select:focus-visible,a:focus-visible{outline:3px solid #e39935;outline-offset:4px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,540px),1fr));gap:24px;padding-top:0}.card{background:white;border:1px solid #d4e2e7;border-radius:14px;overflow:hidden}.capture{display:block;width:100%;padding:0;border:0;background:#132c35;cursor:zoom-in}.capture img{display:block;width:100%;height:auto}.copy{padding:24px}.copy p:last-child{margin-bottom:0}.subtitle{font-weight:650;color:#47636e}.copy p{font-size:15px}#count,footer{color:#47636e}a{color:#146c85}dialog{width:min(96vw,1500px);max-height:96vh;padding:14px;border:0;border-radius:12px;background:#edf3f5;color:#173f4b}dialog::backdrop{background:#0c222ce8}dialog img{display:block;max-width:100%;max-height:76vh;width:auto;margin:auto}dialog p{text-align:center;margin:12px 0 0;font-size:14px}.actions{display:flex;justify-content:center;gap:12px;margin-top:12px}.actions button{border:1px solid #9fbac4;border-radius:7px;padding:8px 18px;color:#173f4b;background:white;cursor:pointer}[hidden]{display:none!important}@media(max-width:600px){header,main,footer{padding-left:16px;padding-right:16px}.copy{padding:18px}}
</style></head><body><header><p class="badge">CHAO GARDEN 0.15.0 - MINECRAFT JAVA 1.21.1 / NEOFORGE</p><h1>Official Chao forms. A fuller garden.</h1><p>Supplied official models and animations now support the main forms, nineteen retained themes, animal parts and the three Chaos forms. Egg wrapping, fruit sources, the transporter, held Chao and garden music are shown below.</p><p>Fifteen whole game frames plus a motion sequence made from eight captured frames. The archive works offline after extraction; the separate standalone HTML embeds its images.</p><div class="controls"><div class="search"><label for="search">Find a preview</label><input id="search" type="search" placeholder="Try Chaos, fruit, transporter, held Chao..."></div><div><label for="filter">Show</label><select id="filter"><option value="all">All previews</option><option value="forms">Forms and animal parts</option><option value="themes">Mod themes</option><option value="eggs">Egg shell</option><option value="orchard">Fruit and market</option><option value="companions">Transporter, held Chao and music</option><option value="motion">Motion</option></select></div></div><p id="count" role="status" aria-live="polite"></p></header><main>'''+''.join(cards)+'''</main><footer><p>Optional themes use development identity fixtures in these captures. These views do not establish full third-party modpack compatibility. See source/TESTING.md for completed validation and source/README.md for installation.</p><p>Historical studies: '''+historical+'''. Their release labels and original captures are retained.</p><p>Unofficial fan project. Supplied official models, textures, animation and audio belong to their rights holders. The source documents record provenance and the custom Minecraft adaptations.</p></footer><dialog id="zoom" aria-label="Enlarged preview"><img alt=""><p></p><div class="actions"><button id="previous" type="button">Previous</button><button id="close" type="button">Close</button><button id="next" type="button">Next</button></div></dialog><script>
const cards=[...document.querySelectorAll('.card')],search=document.querySelector('#search'),filter=document.querySelector('#filter'),count=document.querySelector('#count'),zoom=document.querySelector('#zoom');let current=0;
function visible(){return cards.filter(card=>!card.hidden)}function update(){const query=search.value.trim().toLowerCase();for(const card of cards)card.hidden=!(filter.value==='all'||card.dataset.category===filter.value)||!card.dataset.search.includes(query);count.textContent=visible().length+' previews - select an image to enlarge';}function show(card){const image=card.querySelector('img');current=cards.indexOf(card);zoom.querySelector('img').src=image.src;zoom.querySelector('img').alt=image.alt;zoom.querySelector('p').textContent=card.querySelector('h2').textContent+' - '+card.querySelector('.subtitle').textContent;if(!zoom.open)zoom.showModal();}function step(offset){const list=visible();if(!list.length)return;show(list[(list.indexOf(cards[current])+offset+list.length)%list.length]);}search.addEventListener('input',update);filter.addEventListener('change',update);for(const card of cards)card.querySelector('.capture').addEventListener('click',()=>show(card));document.querySelector('#close').addEventListener('click',()=>zoom.close());document.querySelector('#previous').addEventListener('click',()=>step(-1));document.querySelector('#next').addEventListener('click',()=>step(1));zoom.addEventListener('click',event=>{if(event.target===zoom)zoom.close()});zoom.addEventListener('keydown',event=>{if(event.key==='ArrowLeft'){event.preventDefault();step(-1)}if(event.key==='ArrowRight'){event.preventDefault();step(1)}});update();
</script></body></html>'''
    require('http://' not in page and 'https://' not in page,'Gallery acquired an external dependency')
    require(page.count('<article class="card"')==16,'Missing current gallery cards')
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
    log_path=Path(capture_log).resolve() if capture_log else root.parent/'chao015-client.log'
    out.mkdir(parents=True,exist_ok=True)
    screenshots=root/'docs/screenshots';release=screenshots/'release-0.15';release.mkdir(parents=True,exist_ok=True)
    names,count,capture_record=read_capture_session(log_path,root)
    require(count==23,'The release needs fifteen feature and eight idle captures')
    records=[];frames={}
    for name in names:
        source=root/'run/screenshots'/name;size=validate_frame(source);target=release/name
        copy_flushed(source,target);key=target.stem.removeprefix(SHOT_PREFIX);frames[key]=target
        records.append({'file':name,'width':size[0],'height':size[1],'sha256':sha256(target),
                        'title':SHOTS[key][0] if key in SHOTS else 'Captured idle pose '+key.split('-')[1]})
    for obsolete in release.glob('chao015-*.png'):
        if obsolete.name not in names:obsolete.unlink()
    motion=release/'chao015-idle-review.gif'
    motion_record=build_motion([frames[key] for key in IDLE_KEYS],motion)
    manifest={'release':VERSION,'minecraft':'1.21.1','neoforge':'21.1.219','capture_count':count,
              'origin':'Actual Minecraft client screenshots','full_frames':True,'capture_log_sha256':sha256(log_path),
              'capture_directory':'source/docs/screenshots/release-0.15/','captures':records,
              'gallery_feature_count':15,'motion_review':motion_record}
    manifest_path=root/'docs/release-capture-manifest-0.15.json';record_path=root/'docs/release-capture-record-0.15.txt'
    write_flushed(manifest_path,(json.dumps(manifest,indent=2)+'\n').encode());write_flushed(record_path,capture_record.encode())
    display=[frames[key] for key in DISPLAY_KEYS];preview=out/f'chao-garden-{VERSION}-preview.png'
    build_feature_sheet(display,preview);copy_flushed(preview,screenshots/preview.name)
    gallery=root/'docs/release-gallery-0.15.html'
    local_urls={str(p):os.path.relpath(p,gallery.parent).replace(os.sep,'/') for p in [*display,motion]}
    build_gallery(display,motion,gallery,local_urls)
    build_gallery(display,motion,out/f'chao-garden-{VERSION}-gallery.html')
    archive_gallery=out/f'chao-garden-{VERSION}-offline-gallery.html'
    archive_urls={str(p):'../source/docs/screenshots/release-0.15/'+p.name for p in [*display,motion]}
    build_gallery(display,motion,archive_gallery,archive_urls);copy_flushed(motion,out/motion.name)
    histories=[]
    for original,filename,label in HISTORIES:
        history_source=root/'docs'/original
        require(label in history_source.read_text(),'Historical gallery lost its release label: '+original)
        historical=root/'docs'/filename
        write_flushed(historical,historical_launcher(label,original).encode())
        copy_flushed(history_source,out/filename);histories.append((filename,label,original))
    start_path=root/'docs/START-HERE-0.15.txt';write_flushed(start_path,start_here(count).encode())
    script_copy=root/'scripts/package_chao015.py'
    if SCRIPT!=script_copy.resolve():copy_flushed(SCRIPT,script_copy)
    if previews_only:
        print('Previews ready:',count,'validated whole game frames; 15-view gallery, eight-pose GIF and feature sheet.')
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
        require(sum(n.startswith(prefix+'source/docs/screenshots/release-0.15/') and n.endswith('.png') for n in entries)==23,'Missing release PNG capture')
        require(sum(n.startswith(prefix+'Blockbench/') and n.endswith('.bbmodel') for n in entries)==8,'Missing Blockbench authoring project')
        require(sum(n.startswith(prefix+'source/art/imported_chao/') for n in entries)==41,'Missing or extra imported asset input')
        require(prefix+'source/gradle/wrapper/gradle-wrapper.jar' in nameset,'Missing Gradle wrapper')
        import posixpath
        for gallery_name in (prefix+'previews/index.html',prefix+'source/docs/release-gallery-0.15.html'):
            sources=re.findall(r'<img[^>]+src="([^"]+)"',bundle.read(gallery_name).decode())
            require(len(sources)==16,'Current gallery image count mismatch')
            for source in sources:
                resolved=posixpath.normpath(posixpath.join(posixpath.dirname(gallery_name),source))
                require(resolved in nameset,'Offline gallery references missing image: '+resolved)
        require(bundle.read(prefix+jar.name)==source_jar.read_bytes(),'ZIP jar differs from current build')
        require(bundle.read(prefix+f'source/docs/chao-garden-{VERSION}-preview-gallery.pdf')==current_pdf.read_bytes(),'ZIP PDF differs from current preview')
        for record in records:
            require(hashlib.sha256(bundle.read(prefix+'source/docs/screenshots/release-0.15/'+record['file'])).hexdigest()==record['sha256'],'ZIP capture hash mismatch')
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
    parser.add_argument('--capture-log',type=Path,help='Default: project parent/chao015-client.log')
    parser.add_argument('--project-root',type=Path)
    parser.add_argument('--output-dir',type=Path)
    args=parser.parse_args();main(args.previews_only,args.capture_log,args.project_root,args.output_dir)
