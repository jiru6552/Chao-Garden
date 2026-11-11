"""Package Chao Garden 0.10.0 after final tests and real Minecraft captures.

--previews-only validates the completed capture session and builds the full-frame
gallery/feature sheet without requiring or publishing a production JAR or ZIP.
No image in this release gallery is generated art or a simulated game view.
"""
from pathlib import Path
import argparse
import base64
import hashlib
import html
import io
import json
import math
import os
import re
import shutil
import textwrap
import zipfile

from PIL import Image, ImageDraw, ImageFont, ImageStat


SCRIPT = Path(__file__).resolve()
BASE = SCRIPT.parent
ROOT = BASE / 'chao-garden'
if not ROOT.is_dir() and SCRIPT.parent.name == 'scripts':
    ROOT = SCRIPT.parent.parent
    BASE = ROOT.parent
OUT = BASE / 'deliverables'
VERSION = '0.10.0'
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
INK, MUTED, PAPER = '#173f4b', '#47636e', '#edf3f5'
SHOTS = {
    'orchard': ('Fruit orchards', 'Twelve renewable fruit families',
                'Twelve four-block trees, each with four hanging fruit pods. Plant matching seeds on dirt or farmland and leave room for the canopy.'),
    'fruit-tree': ('Hanging fruit', 'Harvest, ripen, repeat',
                   'Ripe pods yield one fruit on right-click, then begin growing again. Bone meal advances ripeness; a supported canopy can replace missing pods.'),
    'animal-genes': ('Animal gene appearances', 'Six examples from 21 gene samples',
                     'Penguin, Rabbit, Peacock, Dragon, Unicorn and Phoenix samples demonstrate animated cuboid ears, crests, limbs and tails on the existing Chao forms.'),
    'supplies': ('Seeds, genes and drives', 'The actual inventory and gene tooltip',
                 'Colored DNA vials name their animal and exact benefits. The inventory shows all 21 samples, twelve seeds and four drive types.'),
    'trader': ('The wandering Black Market', 'An original Chao merchant model',
               'Dark glasses, a pale mouth cover, yellow hands and marker meet a traveling hood, robe and satchel. The model is newly authored.'),
    'black-market': ('Emerald trading', 'The real merchant interface',
                     'Rotating stock contains seeds, drives, genes, shiny and jewel eggs, and reusable toys. Seeds are sold in place of harvested fruit.'),
    'held-companion': ('Pick up your Chao', 'Animated first-person carrier',
                       'Sneak-right-click your Chao with an empty hand. Its filled carrier displays the saved Chao model while held; use it on a block to release.'),
    'held-companion-motion': ('Held Chao movement', 'A second actual rendered frame',
                              'The same carrier is captured again after animation time advances. Its saved coat, theme and acquired animal features use the normal model renderer.'),
    'race-course': ('Build a Chao race', 'Straight lines and connected flags',
                    'Build a starting row of 2–8 sections and an ending row. Race flags form a traversable course with links up to 32 blocks long.'),
    'race-link-preview': ('Link the route', 'An actual placement preview',
                          'Use a Race Marker on the start, then place flags in sequence. The preview line reports range; finish by using linked flags on the ending line.'),
    'race-lobby': ('Race entry', 'Choose Chao from your gardens',
                   'The starting-line lobby lists your registered Chao. Other nearby keepers can fill free slots; the host starts or cancels the contest.'),
    'karate-lobby': ('Chao karate', 'A 7×7 mat and shared tournament lobby',
                     'A connected square of 49 Karate Mats admits up to eight entrants. Bouts use virtual health, with training and Karate Experience awarded.'),
    'guide-second-index': ('An in-game mechanics guide', 'The second index in the actual guide',
                           'The care guide explains the new orchard, gene, market, garden, contest and companion systems alongside the existing lifecycle and genetics pages.'),
    'equipped-companion': ('Your adult companion', 'A real summoned Chao follows its keeper',
                           'With Accessories installed, an adult carrier in the Chao Companion slot summons its saved Chao. Power 10 unlocks combat help; Fly 10 unlocks gliding.'),
    'accessories-slot': ('The Chao Companion slot', 'The real Accessories equipment interface',
                         'One healthy adult can be equipped at a time. The optional integration requires Accessories and its oωo dependency on both client and server.'),
}
REQUIRED_BASE_SHOTS = tuple('chao-mechanics-' + key + '.png' for key in list(SHOTS)[:12])


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


def image_data(path):
    return 'data:image/png;base64,' + base64.b64encode(path.read_bytes()).decode('ascii')


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


def read_capture_session(log_path, root):
    """Use the last completed run, never a count guessed from directory contents."""
    log = log_path.read_text(errors='replace')
    completions = list(re.finditer(r'MECHANICS_VISUAL_STUDY_COMPLETE:\s*(\d+) actual Minecraft views', log))
    require(bool(completions), 'No completed mechanics visual session in ' + str(log_path))
    complete = completions[-1]
    segment = log[completions[-2].end() if len(completions) > 1 else 0:complete.start()]
    # A restarted client may share this log. Keep only the latest stage-zero run.
    starts = list(re.finditer(r'MECHANICS_VISUAL_CAPTURED chao-mechanics-[a-z0-9-]+\.png stage=0\b', segment))
    require(bool(starts), 'The completed session has no initial capture')
    segment = segment[starts[-1].start():]
    captured = re.findall(r'MECHANICS_VISUAL_CAPTURED (chao-mechanics-[a-z0-9-]+\.png) stage=(\d+)\b', segment)
    count = int(complete.group(1))
    require(len(captured) == count, f'Completion says {count} views but records {len(captured)} captures')
    require([int(stage) for _, stage in captured] == list(range(count)), 'Capture stages are missing, duplicated or out of order')
    names = [name for name, _ in captured]
    require(len(names) == len(set(names)), 'Duplicate capture filenames')
    require(set(REQUIRED_BASE_SHOTS) <= set(names), 'The completed session is missing a core mechanics view')
    harness = (root / 'src/main/java/com/chaogarden/client/MechanicsVisualSmoke.java').read_text()
    array = re.search(r'String\[\]\s+CAPTURES\s*=\s*\{(.*?)\};', harness, re.S)
    require(array is not None, 'Could not read the development harness capture list')
    expected = re.findall(r'"(chao-mechanics-[a-z0-9-]+\.png)"', array.group(1))
    require(names == expected[:count] and count <= len(expected), 'The completed session does not match the current harness')
    optional_limit = re.search(r'AccessoriesBridge\.available\(\)\s*\?\s*CAPTURES\.length\s*:\s*(\d+)', harness)
    permitted_counts = {len(expected)}
    if optional_limit:
        permitted_counts.add(int(optional_limit.group(1)))
    require(count in permitted_counts, 'Completed view count is from an older or incomplete harness: ' + str(count))
    # Screenshot writes are asynchronous. They can finish just after completion.
    for name in names:
        require(re.search(r'MECHANICS_VISUAL_SCREENSHOT ' + re.escape(name) + r':[^\n]*Saved screenshot', log) is not None,
                'Screenshot-save callback missing: ' + name)
        require(name.removeprefix('chao-mechanics-').removesuffix('.png') in SHOTS,
                'Add a title/caption for the new screenshot: ' + name)
    return names, count, segment.strip() + '\nMECHANICS_VISUAL_STUDY_COMPLETE: ' + str(count) + ' actual Minecraft views\n'


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


def start_here(count, optional_companion_captured):
    return textwrap.dedent(f'''\
    CHAO GARDEN {VERSION}
    Minecraft Java 1.21.1 | NeoForge 21.1.219+ (21.1 series) | Java 21

    INSTALL
    Replace the older Chao Garden jar with chao_garden-{VERSION}.jar in mods/.
    Install the same jar on the server and every client. The base mod works alone.
    Optional adult equipment companions require compatible NeoForge Accessories
    and its oωo dependency on client and server. Third-party mods are not bundled.
    The optional runtime tested versions and results are in source/TESTING.md.

    FIRST CHAO
    Craft a Normal Chao Egg: egg + emerald + wheat seeds + oak sapling (shapeless).
    Place it on a block; by default it hatches after five active minutes.
    Craft the Care Guide with a book + wheat seeds. Read it for recipes and pages
    about the new systems. Use an Analyzer to inspect care, genes and training.
    The player placing or claiming a Chao becomes its keeper.

    CONTROLS
    - Empty-hand right-click: pet or claim an unowned Chao.
    - Sneak + empty-hand right-click: pick up your Chao into a filled carrier.
      No empty carrier is needed first. The selected hand receives the item.
    - Use a filled carrier on a block: release into clear space.
    - Select a filled carrier in either hand: see its animated saved Chao.
    - Use an empty carrier on your Chao: the older capture method still works.
    - Right-click with fruit, a drive or a DNA sample: give the care item.
    - Right-click with an Analyzer: open the care journal.
    - Right-click with a toy: play; reusable toys have a 30-second Chao cooldown.
    - Garden Bell on a Garden Block, then your Chao: assign that Chao.
    - Sneak-use Garden Bell on an assigned Chao: unassign and resume following.
      On an unassigned Chao, sneak-use the bell toggles stay/follow.
    Ownership is enforced. Activity-controlled Chao cannot be picked up.

    DRIVES AND ANIMAL GENES
    Swim, Fly, Run and Power Drives give 24 progress toward the next level in
    their ability. Conventional chest loot tables (chests/ in any namespace)
    have an extra 40% chance of 1-2 drives and 12% chance of one animal gene.
    Earlier regional supply pools remain additive; opened chests are not refilled.
    There are 21 colored DNA-vial samples. Hover for the animal, exact training
    changes, appearance slots and special effects. Use a sample on your Chao.
    Parts can mix across slots and animate with the body; the main head remains
    broad and block-built. Skeleton Dog clears acquired parts. Half Fish changes
    the marker to a green flame; Bat hides feet, wings and tail. Chaos Chao keep
    training/checklist benefits without animal parts or ghost changes.
    Existing animal-item IDs remain working aliases. No new animal mobs appear.

    FRUIT ORCHARDS
    Twelve tree fruits: Garden, Strong, Tasty, Hero, Dark, Heart, Chao, Smart,
    Stamina, Round, Square and Triangle. Mushroom and Mint Candy are not trees.
    Craft one fruit into one matching seed, or buy seeds from farmers and the
    Black Market. Plant on dirt/farmland in a bright area. Leave four blocks of
    height and a clear 3x3 canopy area. Bone meal advances seedlings and pods.
    Each mature tree has three trunk blocks, a branch canopy and four hanging
    pods. Pods have four stages. Right-click a ripe pod for one fruit; it regrows.
    A supported canopy can replace missing pods. Obstructed trees do not grow
    through existing blocks. Fruit retains its normal Chao care effects.

    BLACK MARKET
    A Chao-like wandering trader takes emeralds. A stock includes four seeds,
    four drives, six genes, a shiny egg, a jewel egg, and ball/rattle/maracas.
    Seeds cost 4-8 emeralds; drives 3; ordinary genes 8, exotic genes 16;
    shiny eggs 32, jewel eggs 48; ball 8, rattle 6, maracas 10.
    Stock refreshes after 3-7 minutes while trading is closed. Natural visits
    are attempted at most every 20 minutes of world play with a 60% visit roll;
    trader- and mob-spawning rules apply. A visit lasts up to 20 minutes.
    Seeds are sold instead of fruit. Toys are reusable and add happiness/bond.

    GARDENS
    Place and right-click a Garden Block to enroll up to eight nearby unassigned
    Chao you own. Use a linked Garden Bell for particular Chao. Registered Chao
    roam within 16 blocks. The garden screen can move or unassign your loaded
    Chao. Race/karate lobbies list your registered Chao across gardens/dimensions.
    The records refer to existing entities; carried/missing Chao are unavailable.

    BUILD A RACE
    1. Build a straight row of 2-8 Starting Line sections facing the same way.
       Each supplies one entrant slot. Build a straight Ending Line row.
    2. Use Race Markers on the start, then place flags in sequence. Each new flag
       becomes the next source. Each connection must be at most 32 blocks.
       A green/red preview shows whether the placement is within range.
    3. Use a linked marker on the ending line to finish. An existing flag can
       be selected to continue or edit. Routes allow up to 64 links, no loops,
       and must total at least eight blocks. Build traversable terrain yourself.
    4. Ordinary/empty-hand right-click the start for the lobby. Select multiple
       Chao from your gardens; nearby keepers can enter theirs in free slots.
       Only the host starts/cancels. Races time out after five minutes.
    Run aids land speed, Swim water, Power climbing and Fly descent. Participants
    earn skill training and Race Experience; winners get a bonus.

    CHAO KARATE
    Build exactly one connected 7x7 square of Karate Mats (49 tiles), then
    right-click any tile. Up to eight Chao enter successive two-Chao bouts.
    Each bout has a 90-second limit, virtual health, ring-out and timeout rules.
    Power aids strikes; Swim defence; Fly dodging; Run attack frequency;
    Stamina bout health; Intelligence helps zeal. Competitors gain skill training
    and Karate Experience, with a champion bonus. Actual pets are not injured.
    Breaking the arena, cancellation or disconnect returns entrants safely.

    ADULT EQUIPMENT COMPANIONS (OPTIONAL ACCESSORIES)
    Put one healthy adult filled carrier into the Chao Companion equipment slot.
    A temporary world body follows its keeper. Unequipping, logout or changing
    dimensions recalls the body and saves its state in the same carrier.
    Power level 10: combat help against your recent target (excluding players
    and your tamed animals). Fly level 10: automatic slow glide while falling;
    hold sneak to descend normally. Both require energy above 5. Recover energy
    on the ground while not fighting. Holding/equipment pauses normal age/care.
    Defeat disables the equipped Chao for 1,200 server ticks (one minute at
    20 TPS), costs 10 happiness and one level in a randomly chosen trained skill.
    Levels/points never go below zero. If all skills are zero, happiness is still
    lost. Recovery is stored and cannot be bypassed by unequipping/releasing.
    The companion slot retains its item on player death.

    LIFECYCLE AND MAIN FORMS
    Default active times: egg 5 minutes, child 3 hours, adult 12 hours. Unloaded
    Chao and stored carriers do not age. Happiness 50+ at end of adulthood gives
    reincarnation. Primary type/alignment lock at first evolution; adult training
    can change the secondary form. Chaos evolution needs a third life or later,
    happiness ABOVE 50, all 21 genes this life, and balanced Normal influences.
    Neutral/Hero/Dark alignment selects Light/Angel/Devil Chaos respectively.
    Two adults given Heart Fruit can breed; cooldown is five minutes.

    MOD THEMES AND SAVES
    The existing 48 themes remain (46 mod tributes plus Nether and End).
    Mod-theme recipes/eggs require a matching loaded mod ID and a compatible
    NeoForge 1.21.1 build. Nether/End require no extra mod. /chao variants lists
    enabled themes; operators can use /chao variantegg <id>. The catalog includes
    accepted aliases, current projects, community ports and dormant classics.
    Saved heritage survives a removed dependency and resumes when it returns.
    Existing Chao, eggs and carriers retain data. source/MECHANICS.md and the
    feature documents explain exact behavior and Minecraft adaptations.

    PREVIEWS AND SOURCE
    Open previews/index.html: {count} actual full-frame Minecraft captures, with
    search/category controls and keyboard-friendly enlargement. All frames are
    embedded, so the mechanics gallery opens offline as one HTML file.
    {'The gallery includes real Accessories equipment and companion views.' if optional_companion_captured else 'This capture session ran without the optional Accessories equipment views.'}
    previews/historical-0.9-head-gallery.html retains the previous head study,
    clearly labelled as 0.9 evidence. It is not a new 0.10 capture session.
    Blockbench/ contains eight editable projects with embedded textures.
    source/ contains all code, resources, tests, generators and research notes;
    build caches, runtime worlds and third-party mod jars are excluded.

    BUILD AND VERIFICATION
    With a Java 21 JDK, open source/ and run:
      ./gradlew build coreTest
      ./gradlew runGameTestServer
    Windows: gradlew.bat. Initial setup downloads development dependencies.
    See source/TESTING.md for completed release checks and optional-mod tests.
    The installable JAR excludes identity fixtures, test classes and visual
    capture harnesses. SHA256SUMS.txt records the packaged JAR checksum.

    CREDITS
    Unofficial fan project; Chao/Sonic Adventure belong to their rights holders.
    Models, textures and mod-theme tributes are original reconstructions.
    Included Chao voices are Sega game recordings; see source/docs/AUDIO_CREDITS.md,
    source/LICENSE and source/TEMPLATE_LICENSE.txt. No other mod's jar or visual
    assets are bundled. Chao Island reference links accompany the feature docs.
    ''')


def build_feature_sheet(paths, output):
    columns, tile_w, image_h, caption_h, margin, header_h = 3, 640, 360, 112, 20, 120
    rows = math.ceil(len(paths) / columns)
    picture = Image.new('RGB', (columns * tile_w + margin * 2, header_h + rows * (image_h + caption_h) + margin), PAPER)
    draw = ImageDraw.Draw(picture)
    draw.text((margin, 18), 'Chao Garden 0.10.0 — Mechanics in Minecraft', fill=INK, font=font(36))
    draw.text((margin, 69), f'{len(paths)} actual game captures • full frames, resized to fit • Minecraft Java 1.21.1 / NeoForge', fill=MUTED, font=font(21))
    for index, path in enumerate(paths):
        key = path.stem.removeprefix('chao-mechanics-')
        title, subtitle, _ = SHOTS[key]
        x, y = margin + index % columns * tile_w, header_h + index // columns * (image_h + caption_h)
        draw.rectangle((x, y, x + tile_w - 8, y + image_h), fill='#142b35')
        with Image.open(path) as capture:
            shot = capture.convert('RGB')
            shot.thumbnail((tile_w - 8, image_h), Image.Resampling.LANCZOS)
            picture.paste(shot, (x + (tile_w - 8 - shot.width) // 2, y + (image_h - shot.height) // 2))
        draw.text((x + 8, y + image_h + 12), f'{index + 1:02d}  {title}', fill=INK, font=font(24))
        lines = wrap_pixels(draw, subtitle, tile_w - 24, font(19))
        for line_no, line in enumerate(lines[:2]):
            draw.text((x + 8, y + image_h + 49 + line_no * 25), line, fill=MUTED, font=font(19))
    save_png(picture, output)


def build_gallery(paths, output):
    cards = []
    categories = {
        'orchard': 'orchard', 'fruit-tree': 'orchard', 'animal-genes': 'genes', 'supplies': 'genes',
        'trader': 'market', 'black-market': 'market', 'held-companion': 'companion',
        'held-companion-motion': 'companion', 'race-course': 'activities',
        'race-link-preview': 'activities', 'race-lobby': 'activities', 'karate-lobby': 'activities',
        'guide-second-index': 'guide', 'equipped-companion': 'companion', 'accessories-slot': 'companion',
    }
    for index, path in enumerate(paths):
        key = path.stem.removeprefix('chao-mechanics-')
        title, subtitle, caption = SHOTS[key]
        search = html.escape(' '.join((title, subtitle, caption, categories[key])).lower(), quote=True)
        cards.append(f'''<article class="card" data-category="{categories[key]}" data-search="{search}">
<button class="capture" type="button" aria-label="Enlarge {html.escape(title, quote=True)}"><img src="{image_data(path)}" alt="Actual Minecraft capture: {html.escape(title, quote=True)}" loading="lazy"></button>
<div class="copy"><p class="number">{index + 1:02d} · ACTUAL MINECRAFT FRAME</p><h2>{html.escape(title)}</h2><p class="subtitle">{html.escape(subtitle)}</p><p>{html.escape(caption)}</p></div></article>''')
    controls = [
        ('Care and pickup', 'Empty-hand right-click pets or claims. Sneak-right-click your Chao with an empty hand picks it up. Use the filled carrier on a block to release. The Garden Bell handles stay/follow and garden assignment.'),
        ('Plant an orchard', 'Craft one fruit into one matching seed or buy seeds. Plant on dirt/farmland with four blocks of height and a clear 3×3 canopy area. Right-click ripe pods; bone meal speeds growth.'),
        ('Enter contests', 'A starting row has 2–8 sections. Link flags up to 32 blocks apart and finish at an ending row. For karate, build a connected 7×7 mat. Right-click the start or mat for the shared lobby.'),
        ('Equip an adult', 'With Accessories and oωo installed, equip a healthy adult carrier in the Chao Companion slot. Power 10 enables combat; Fly 10 enables automatic glide. Hold sneak to descend. Energy must exceed 5.'),
        ('Recover from defeat', 'An equipped Chao returns after 1,200 server ticks. Each defeat costs 10 happiness and one trained skill level. Stored recovery cannot be skipped by unequipping.'),
    ]
    guide_cards = ''.join('<div><h3>' + html.escape(title) + '</h3><p>' + html.escape(copy) + '</p></div>' for title, copy in controls)
    page = '''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Chao Garden 0.10.0 — Mechanics gallery</title><style>
:root{color-scheme:light}*{box-sizing:border-box}body{margin:0;background:#edf3f5;color:#173f4b;font:16px/1.6 system-ui,sans-serif}header,main,footer{max-width:1600px;margin:auto;padding:28px}header{padding-top:42px}h1{font-size:clamp(30px,4vw,49px);line-height:1.15;margin:0 0 18px}h2{font-size:25px;line-height:1.25;margin:0 0 9px}h3{font-size:17px;margin:0 0 8px}p{margin:0 0 14px}header>p{max-width:1050px;color:#47636e}.badge{font-size:13px;letter-spacing:.08em;font-weight:700;color:#287559}.controls{display:flex;gap:16px;flex-wrap:wrap;align-items:end;margin:28px 0 0}.search{flex:1;min-width:230px}label{display:block;font-size:14px;font-weight:650;margin-bottom:6px}input,select,button{font:inherit}input,select{padding:12px;border:1px solid #9fbac4;background:#fff;border-radius:9px;color:#173f4b}input{width:100%}button:focus-visible,a:focus-visible,input:focus-visible,select:focus-visible{outline:3px solid #e39935;outline-offset:4px}#count{font-size:14px;color:#47636e;margin:12px 0 0}.quick{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:18px;margin-top:28px}.quick>div{background:#deebe9;border-radius:10px;padding:18px}.quick p{font-size:14px;margin:0}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,540px),1fr));gap:24px;padding-top:0}.card{background:white;border:1px solid #d4e2e7;border-radius:14px;overflow:hidden}.capture{display:block;width:100%;border:0;padding:0;background:#132c35;cursor:zoom-in}.capture img{display:block;width:100%;height:auto}.copy{padding:24px}.copy p:last-child{margin-bottom:0}.number{font-size:11px;letter-spacing:.09em;color:#287559;font-weight:750}.subtitle{color:#47636e;font-weight:600}.copy p{font-size:15px}footer{padding-bottom:45px;color:#47636e}footer p{max-width:1000px}a{color:#146c85}dialog{width:min(96vw,1500px);max-height:96vh;padding:14px;border:0;border-radius:12px;background:#edf3f5;color:#173f4b}dialog::backdrop{background:#0c222ce8}dialog img{display:block;max-width:100%;max-height:76vh;width:auto;margin:auto}dialog p{text-align:center;font-size:14px;margin:12px 0 0}dialog .actions{display:flex;justify-content:center;gap:12px;margin-top:12px}dialog button{border:1px solid #9fbac4;border-radius:7px;padding:8px 18px;color:#173f4b;background:white;cursor:pointer}[hidden]{display:none!important}@media(prefers-reduced-motion:no-preference){button{transition:background .15s}}@media(max-width:600px){header,main,footer{padding-left:16px;padding-right:16px}.copy{padding:18px}}
</style></head><body><header><p class="badge">CHAO GARDEN 0.10.0 · MINECRAFT JAVA 1.21.1 / NEOFORGE</p>
<h1>A home, an orchard, and more to do together.</h1>
<p>Animal DNA samples, renewable hanging fruit, an emerald Black Market, player-built gardens and contests, and animated held Chao. Optional Accessories support lets an adult join you as a companion.</p>
<p>These are full frames from the real Minecraft client. The capture harness waits for synchronized scene state and completed rendered frames. The images are embedded in this file, so it works offline.</p>
<div class="quick">''' + guide_cards + '''</div>
<div class="controls"><div class="search"><label for="search">Find a mechanic</label><input type="search" id="search" placeholder="Try seeds, DNA, karate, gliding…"></div><div><label for="filter">Show</label><select id="filter"><option value="all">All mechanics</option><option value="orchard">Fruit orchards</option><option value="genes">Genes and supplies</option><option value="market">Black Market</option><option value="activities">Races and karate</option><option value="companion">Held and equipped Chao</option><option value="guide">In-game guide</option></select></div></div><p id="count" role="status" aria-live="polite"></p></header><main>''' + ''.join(cards) + '''</main>
<footer><p>The existing alignment/type evolutions, Light/Angel/Devil Chaos forms, inherited coats and 48 themes remain. Mod themes still require a matching compatible mod to be loaded. See START-HERE.txt and the source feature documents for precise rules and recipes.</p>
<p><a href="historical-0.9-head-gallery.html">Open the historical 0.9 head gallery</a> for the previous block-crown and forehead-goggle study. Its captures are labelled 0.9; this page documents the new 0.10 mechanics.</p><p>Unofficial fan project. Models and textures are original reconstructions; see the included source credits and audio provenance.</p></footer>
<dialog id="zoom" aria-label="Enlarged Minecraft capture"><img alt=""><p></p><div class="actions"><button id="previous" type="button">Previous</button><button id="close" type="button">Close</button><button id="next" type="button">Next</button></div></dialog>
<script>
const cards=[...document.querySelectorAll('.card')],search=document.querySelector('#search'),filter=document.querySelector('#filter'),count=document.querySelector('#count'),zoom=document.querySelector('#zoom');let current=0;
function visible(){return cards.filter(card=>!card.hidden)}
function update(){const query=search.value.trim().toLowerCase();for(const card of cards)card.hidden=!(filter.value==='all'||card.dataset.category===filter.value)||!card.dataset.search.includes(query);const n=visible().length;count.textContent=n+' capture'+(n===1?'':'s')+' · select an image to enlarge';}
function show(card){const image=card.querySelector('img');current=cards.indexOf(card);zoom.querySelector('img').src=image.src;zoom.querySelector('img').alt=image.alt;zoom.querySelector('p').textContent=card.querySelector('h2').textContent+' — '+card.querySelector('.subtitle').textContent;if(!zoom.open)zoom.showModal();}
function step(offset){const list=visible();const position=list.indexOf(cards[current]);show(list[(position+offset+list.length)%list.length]);}
search.addEventListener('input',update);filter.addEventListener('change',update);for(const card of cards)card.querySelector('.capture').addEventListener('click',()=>show(card));document.querySelector('#close').addEventListener('click',()=>zoom.close());document.querySelector('#previous').addEventListener('click',()=>step(-1));document.querySelector('#next').addEventListener('click',()=>step(1));zoom.addEventListener('click',event=>{if(event.target===zoom)zoom.close()});zoom.addEventListener('keydown',event=>{if(event.key==='ArrowLeft'){event.preventDefault();step(-1)}if(event.key==='ArrowRight'){event.preventDefault();step(1)}});update();
</script></body></html>'''
    require('http://' not in page and 'https://' not in page, 'Mechanics gallery acquired an external dependency')
    require(page.count('<article class="card"') == len(paths), 'Missing gallery cards')
    write_flushed(output, page.encode())


def validate_production_jar(source_jar, root):
    with zipfile.ZipFile(source_jar) as jar:
        names = jar.namelist()
        metadata = jar.read('META-INF/neoforge.mods.toml').decode()
        require(re.search(r'(?m)^version\s*=\s*"' + re.escape(VERSION) + r'"', metadata) is not None,
                'Production jar does not report version ' + VERSION)
        require(metadata.count('[[mods]]') == 1 and 'chao_variant_fixtures' not in metadata,
                'Fixture mod metadata leaked into production')
        for name in names:
            require(not any(token in name for token in ('variantfixtures', 'ChaoVisualSmoke', 'MechanicsVisualSmoke',
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
        require(jar.testzip() is None, 'Production jar CRC failure')


def source_files(root):
    excluded = {'.gradle', 'build', 'run', 'run-data', 'dev-mods', 'thirdpartyjars', 'third-party-jars',
                '__pycache__', '.git', '.idea', '.vscode', 'node_modules', '.cache'}
    for directory, dirs, files in os.walk(root):
        dirs[:] = sorted(name for name in dirs if name not in excluded)
        for name in sorted(files):
            path = Path(directory) / name
            relative = path.relative_to(root).as_posix()
            if name.endswith(('.pyc', '.log', '.writing', '.copying', '.tmp', '.zip')):
                continue
            # The Gradle wrapper is part of the build, not a third-party mod.
            if name.endswith('.jar') and relative != 'gradle/wrapper/gradle-wrapper.jar':
                continue
            require(not path.is_symlink(), 'Refusing to package a source symlink: ' + str(path))
            yield path, relative


def main(previews_only=False, capture_log=None, project_root=None, output_dir=None):
    root = Path(project_root).resolve() if project_root else ROOT
    out = Path(output_dir).resolve() if output_dir else OUT
    log_path = Path(capture_log).resolve() if capture_log else root.parent / 'chao010-mechanics-client.log'
    out.mkdir(parents=True, exist_ok=True)
    docshots = root / 'docs/screenshots'
    mechanics = docshots / 'mechanics-0.10'
    mechanics.mkdir(parents=True, exist_ok=True)
    names, count, capture_record = read_capture_session(log_path, root)
    frames, records = [], []
    for name in names:
        source = root / 'run/screenshots' / name
        size = validate_frame(source)
        target = mechanics / name
        copy_flushed(source, target)  # Keep the complete original capture bytes.
        frames.append(target)
        records.append({'file': name, 'width': size[0], 'height': size[1], 'sha256': sha256(target),
                        'title': SHOTS[source.stem.removeprefix('chao-mechanics-')][0]})
    # Remove a stale optional view from a previous, larger capture session.
    for obsolete in mechanics.glob('chao-mechanics-*.png'):
        if obsolete.name not in names:
            obsolete.unlink()
    manifest = {'release': VERSION, 'minecraft': '1.21.1', 'neoforge': '21.1.219',
                'capture_count': count, 'origin': 'Actual Minecraft client screenshots',
                'full_frames': True, 'capture_log_sha256': sha256(log_path), 'captures': records}
    manifest_path = root / 'docs/mechanics-capture-manifest-0.10.json'
    write_flushed(manifest_path, (json.dumps(manifest, indent=2, ensure_ascii=False) + '\n').encode())
    write_flushed(root / 'docs/mechanics-capture-record-0.10.txt', capture_record.encode())

    preview = out / f'chao-garden-{VERSION}-preview.png'
    gallery = root / 'docs/mechanics-gallery.html'
    build_feature_sheet(frames, preview)
    copy_flushed(preview, docshots / preview.name)
    build_gallery(frames, gallery)
    copy_flushed(gallery, out / f'chao-garden-{VERSION}-gallery.html')
    history_source = root / 'docs/head-refinement-gallery.html'
    historical = root / 'docs/historical-0.9-head-gallery.html'
    require('0.9' in history_source.read_text(), 'The preserved head gallery is no longer labelled 0.9')
    copy_flushed(history_source, historical)
    copy_flushed(historical, out / historical.name)
    start = start_here(count, 'chao-mechanics-accessories-slot.png' in names)
    start_path = root / 'docs/START-HERE-0.10.txt'
    write_flushed(start_path, start.encode())
    # Keep the release tool in the included source project as well.
    script_copy = root / 'scripts/package_chao010.py'
    if SCRIPT != script_copy.resolve():
        copy_flushed(SCRIPT, script_copy)
    if previews_only:
        print(f'Previews ready: {count} validated full Minecraft frames; feature sheet and standalone gallery.')
        print('Gallery:', out / f'chao-garden-{VERSION}-gallery.html')
        print('Preview:', preview)
        print('Full frames:', mechanics)
        print('Capture manifest:', manifest_path)
        return

    jar = out / f'chao_garden-{VERSION}.jar'
    source_jar = root / 'build/libs' / jar.name
    validate_production_jar(source_jar, root)
    copy_flushed(source_jar, jar)
    checksum = sha256(jar)
    projects = sorted((root / 'art/blockbench').glob('*.bbmodel'))
    require(len(projects) == 8, f'Expected eight Blockbench projects, found {len(projects)}')
    for project in projects:
        model = json.loads(project.read_text())
        require(model.get('elements') and model.get('textures'), 'Incomplete Blockbench project: ' + str(project))
        require(all(str(texture.get('source', '')).startswith('data:image/') for texture in model['textures']),
                'Blockbench texture is not embedded: ' + str(project))
    archive = out / f'chao-garden-{VERSION}-complete.zip'
    staging = archive.with_name(archive.name + '.building')
    prefix = f'Chao-Garden-{VERSION}/'
    with zipfile.ZipFile(staging, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as bundle:
        bundle.write(start_path, prefix + 'START-HERE.txt')
        bundle.write(jar, prefix + jar.name)
        for path, relative in source_files(root):
            bundle.write(path, prefix + 'source/' + relative)
        for project in projects:
            bundle.write(project, prefix + 'Blockbench/' + project.name)
        bundle.write(root / 'art/blockbench/README.md', prefix + 'Blockbench/EDITING.md')
        bundle.write(root / 'docs/BLOCKBENCH_VALIDATION.md', prefix + 'Blockbench/VALIDATION.md')
        bundle.write(gallery, prefix + 'previews/index.html')
        bundle.write(historical, prefix + 'previews/' + historical.name)
        bundle.write(preview, prefix + 'previews/' + preview.name)
        bundle.write(manifest_path, prefix + 'previews/CAPTURE-MANIFEST.json')
        bundle.write(root / 'docs/mechanics-capture-record-0.10.txt', prefix + 'previews/CAPTURE-RECORD.txt')
        for path in frames:
            bundle.write(path, prefix + 'previews/mechanics-0.10/' + path.name)
        bundle.write(root / 'docs/MOD_VARIANTS.md', prefix + 'previews/HISTORICAL-THEME-CATALOG.md')
        bundle.writestr(prefix + 'SHA256SUMS.txt', checksum + '  ' + jar.name + '\n')
    with zipfile.ZipFile(staging) as bundle:
        require(bundle.testzip() is None, 'Complete ZIP CRC failure')
        entries = bundle.namelist()
        require(len(entries) == len(set(entries)), 'Duplicate complete ZIP entries')
        require(sum(name.startswith(prefix + 'previews/mechanics-0.10/') for name in entries) == count,
                'ZIP screenshot count differs from completed session')
        require(sum(name.startswith(prefix + 'Blockbench/') and name.endswith('.bbmodel') for name in entries) == 8,
                'ZIP is missing a Blockbench project')
        require(prefix + 'source/gradle/wrapper/gradle-wrapper.jar' in entries, 'Missing build wrapper')
        for name in entries:
            if name.startswith(prefix + 'source/'):
                relative = name.removeprefix(prefix + 'source/')
                require(not ({'build', 'run', '.gradle', 'dev-mods'} & set(Path(relative).parts)),
                        'Runtime/build content in source archive: ' + relative)
                require(not relative.endswith('.jar') or relative == 'gradle/wrapper/gradle-wrapper.jar',
                        'Third-party jar in source archive: ' + relative)
    with staging.open('rb') as handle:
        os.fsync(handle.fileno())
    os.replace(staging, archive)
    fsync_directory(out)
    write_flushed(out / f'chao-garden-{VERSION}-SHA256SUMS.txt',
                  (sha256(archive) + '  ' + archive.name + '\n' + checksum + '  ' + jar.name + '\n'
                   + sha256(preview) + '  ' + preview.name + '\n'
                   + sha256(out / f'chao-garden-{VERSION}-gallery.html') + '  ' + f'chao-garden-{VERSION}-gallery.html\n').encode())
    print('Complete ZIP:', archive, archive.stat().st_size, 'bytes;', len(entries), 'entries')
    print('ZIP SHA256:', sha256(archive))
    print('Jar:', jar, 'SHA256:', checksum)
    print('Gallery:', out / f'chao-garden-{VERSION}-gallery.html')
    print('Preview:', preview)
    print('Full frames:', mechanics)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--previews-only', action='store_true', help='Validate captures and create galleries/images without packaging the production JAR')
    parser.add_argument('--capture-log', type=Path, help='Completed mechanics client log (default: project parent/chao010-mechanics-client.log)')
    parser.add_argument('--project-root', type=Path, help='NeoForge project directory')
    parser.add_argument('--output-dir', type=Path, help='Deliverable directory')
    options = parser.parse_args()
    main(options.previews_only, options.capture_log, options.project_root, options.output_dir)
