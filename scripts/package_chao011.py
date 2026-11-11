"""Package Chao Garden 0.11.0 after final tests and real Minecraft captures.

--previews-only validates the completed capture session and builds the full-frame
gallery/feature sheet without requiring or publishing a production JAR or ZIP.
Game captures and the separate authored sprite contact sheet are labelled explicitly.
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
VERSION = '0.11.0'
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
INK, MUTED, PAPER = '#173f4b', '#47636e', '#edf3f5'
SHOTS = {
    'orchard': ('Larger fruit orchards', 'Twelve trees with full leaf canopies',
                'New fruit trees reach seven blocks high and grow layered 5×5 leaf canopies above branching wood. Four fruit pods hang below each crown.'),
    'foliage-fruit': ('Leaves, branches and hanging fruit', 'The actual new tree at close range',
                      'Real biome-tinted leaves use vanilla-style support, decay, shears and Silk Touch behavior. Ripe pods still yield one fruit and regrow.'),
    'inventory': ('Refined inventory sprites', 'Eggs, capped crystal drives and DNA samples',
                   'The real inventory shows improved egg shading, the four faceted drives and all 21 animal-gene vials. The vial art is native 32px; drives and eggs are native 16px.'),
    'gene-tooltip': ('Animal Gene information', 'The real item tooltip',
                      'Each ordinary sample supplies one randomly selected remaining compatible body part. The tooltip lists training changes, possible slots and special effects.'),
    'drive-tooltip': ('Chaos Drives', 'Long crystals with metal endcaps',
                       'The four training drives retain their gameplay effects and have original pixel-art silhouettes based on the supplied Sonic Adventure references.'),
    'triangle-flags': ('Triangular race markers', 'Red pennants on slim silver poles',
                        'The flag is thin stepped geometry with a triangular silhouette. Existing marker IDs, saved routes and the 32-block connection limit remain.'),
    'flags-angle': ('Flags in three dimensions', 'Pennants viewed from a second angle',
                     'The marker model and selection outline follow their placed orientation. Markers remain non-colliding route controls.'),
    'gene-progression': ('One sample at a time', 'One, two, three and four Rabbit samples',
                         'Four child Chao demonstrate the same deterministic sample sequence. Each use adds a remaining Rabbit part until its four-part set is complete.'),
    'mixed-child-adult': ('Mix animal features', 'The same mixture on a child and an adult',
                          'Rabbit arms and ears coexist with Peacock crest, feet, tail and wings. Children have smaller additions; evolved Chao display more pronounced parts.'),
    'mixed-angle': ('Mixed parts from the side', 'The same Chao from another angle',
                     'Independent appearance slots keep unrelated features when another animal is introduced. Ears and horns from different species conflict; matching Sheep or Dragon pieces can coexist.'),
    'held-mixed': ('Carry your mixed Chao', 'The actual animated first-person model',
                    'The filled carrier renders the same saved creature, including mixed animal parts. Its name, owner, UUID and acquired features survive transport.'),
    'animal-guide': ('Updated animal-parts guidance', 'The actual in-game care guide',
                      'The guide explains gradual parts, mixing and the special samples alongside the existing care, garden, activity and companion systems.'),
}
SHOT_PREFIX = 'chao011-'


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
    """Match one completed, current-harness run and every asynchronous save."""
    log = log_path.read_text(errors='replace')
    completion_pattern = r'CHAO_REFINEMENT_STUDY_COMPLETE:\s*(\d+) actual Minecraft views'
    captured_pattern = r'CHAO_REFINEMENT_CAPTURED (chao011-[a-z0-9-]+\.png) stage=(\d+)\b'
    zero_pattern = r'CHAO_REFINEMENT_CAPTURED chao011-[a-z0-9-]+\.png stage=0\b'
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
    harness = (root / 'src/main/java/com/chaogarden/client/RefinementVisualSmoke.java').read_text()
    array = re.search(r'String\[\]\s+CAPTURES\s*=\s*\{(.*?)\};', harness, re.S)
    require(array is not None, 'Could not read the current refinement capture list')
    expected = re.findall(r'"(chao011-[a-z0-9-]+\.png)"', array.group(1))
    require(names == expected and count == len(expected),
            'The completed study does not match the current complete harness')
    for name in names:
        require(re.search(r'CHAO_REFINEMENT_SCREENSHOT ' + re.escape(name)
                          + r':[^\n]*Saved screenshot', log[start:]) is not None,
                'Screenshot-save callback missing: ' + name)
        require(name.removeprefix(SHOT_PREFIX).removesuffix('.png') in SHOTS,
                'Add a title/caption for the new screenshot: ' + name)
    record = segment.strip() + '\nCHAO_REFINEMENT_STUDY_COMPLETE: ' + str(count) + ' actual Minecraft views\n'
    return names, count, record


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


def start_here(count):
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
    Each ordinary sample adds or replaces ONE randomly selected compatible part
    not already supplied by that animal. Repeat a sample to finish its set, or
    mix different samples across independent body slots. This guaranteed one-part
    progression is a Minecraft adaptation; the source games use partial/random
    acquisition without a published probability on the cited Info Center page.
    Matching Sheep or Dragon ears and horns coexist; ears and horns from
    different animals conflict. Adult additions are more pronounced than child
    additions. Parts animate with the body; the main head remains block-built.
    Skeleton Dog clears acquired parts. Half Fish changes
    the marker to a green flame; Bat hides feet, wings and tail. Chaos Chao keep
    training/checklist benefits without animal parts or ghost changes.
    Existing animal-item IDs remain working aliases. No new animal mobs appear.

    FRUIT ORCHARDS
    Twelve tree fruits: Garden, Strong, Tasty, Hero, Dark, Heart, Chao, Smart,
    Stamina, Round, Square and Triangle. Mushroom and Mint Candy are not trees.
    Craft one fruit into one matching seed, or buy seeds from farmers and the
    Black Market. Plant on dirt/farmland in a bright area. Leave seven blocks of
    height and a clear 5x5 canopy area. Bone meal advances seedlings and pods.
    Each new tree has a taller trunk, horizontal wood branches, a layered real
    leaf canopy and four hanging pods. Pods have four stages. Right-click a ripe
    pod for one fruit; it regrows.
    A supported canopy can replace missing pods. Obstructed trees do not grow
    through existing blocks. Leaves use vanilla-style decay and can be collected
    with shears or Silk Touch. Old small palms remain valid; bone meal can safely
    expand them if there is room, preserving pod ripeness without duplicating
    harvested fruit. Fruit retains its normal Chao care effects.

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
    2. Use the red triangular Race Markers on the start, then place flags.
       Each new flag
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
    Extract the complete ZIP, then open previews/index.html: {count} actual
    full-frame Minecraft refinement
    captures, with search/category controls and keyboard-friendly enlargement.
    The separate sprite sheet is labelled authored pixel art, not a game frame.
    All images are embedded, so the new gallery works offline as one HTML file.
    previews/historical-0.10-mechanics-gallery.html preserves the 0.10 mechanics
    baseline; previews/historical-0.9-head-gallery.html preserves the 0.9 head
    study. Both are explicitly historical rather than new 0.11 captures.
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
    draw.text((margin, 18), 'Chao Garden 0.11.0 — Refinements in Minecraft', fill=INK, font=font(36))
    draw.text((margin, 69), f'{len(paths)} actual game captures • full frames, resized to fit • Minecraft Java 1.21.1 / NeoForge', fill=MUTED, font=font(21))
    for index, path in enumerate(paths):
        key = path.stem.removeprefix(SHOT_PREFIX)
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


def build_gallery(paths, output, sprite_sheet):
    categories = {
        'orchard': 'orchard', 'foliage-fruit': 'orchard', 'inventory': 'sprites',
        'gene-tooltip': 'genes', 'drive-tooltip': 'sprites', 'triangle-flags': 'flags',
        'flags-angle': 'flags', 'gene-progression': 'genes', 'mixed-child-adult': 'genes',
        'mixed-angle': 'genes', 'held-mixed': 'companion', 'animal-guide': 'guide',
    }
    cards = []
    for index, path in enumerate(paths):
        key = path.stem.removeprefix(SHOT_PREFIX)
        title, subtitle, caption = SHOTS[key]
        search = html.escape(' '.join((title, subtitle, caption, categories[key])).lower(), quote=True)
        cards.append(f'''<article class="card" data-origin="minecraft" data-category="{categories[key]}" data-search="{search}">
<button class="capture" type="button" aria-label="Enlarge {html.escape(title, quote=True)}"><img src="{image_data(path)}" alt="Actual Minecraft capture: {html.escape(title, quote=True)}" loading="lazy"></button>
<div class="copy"><p class="number">{index + 1:02d} · ACTUAL MINECRAFT FRAME</p><h2>{html.escape(title)}</h2><p class="subtitle">{html.escape(subtitle)}</p><p>{html.escape(caption)}</p></div></article>''')
    cards.append('''<article class="card" data-origin="authored" data-category="sprites" data-search="authored pixel art sprite sheet eggs genes dna chaos drives inventory">
<button class="capture" type="button" aria-label="Enlarge authored pixel art sprite sheet"><img src="''' + image_data(sprite_sheet) + '''" alt="Authored pixel art contact sheet: refined egg, drive and gene sprites, with native inventory-size samples" loading="lazy"></button>
<div class="copy"><p class="number">AUTHORED PIXEL ART · NOT A GAME CAPTURE</p><h2>Sprite details at a glance</h2><p class="subtitle">Native artwork with 16px inventory footprints</p><p>Original item sprites are shown enlarged without interpolation. All 92 egg icons share consistent shell shading; the four drives use crystal facets and metal caps; all 21 gene types have colored glass DNA vials. Small samples show their inventory-size footprint.</p></div></article>''')
    controls = [
        ('Grow a larger orchard', 'Craft or buy matching seeds. Plant on dirt/farmland with seven blocks of height and a clear 5×5 canopy area. Right-click ripe pods. Bone meal can expand an old palm safely when there is room.'),
        ('Build an animal mixture', 'An ordinary sample adds or replaces one randomly selected remaining compatible part. Repeat it to complete its set or introduce another animal. Different-species ears and horns conflict; matching Sheep or Dragon pieces coexist.'),
        ('Use the triangular flags', 'Select a starting line or existing flag with a Race Marker, then place the next flag within 32 blocks. Finish at an ending line. The marker model changes; saved routes and contest controls remain.'),
        ('Keep your existing Chao', 'Names, owners, genes, eggs and carriers retain their data. New child additions are less pronounced; adults use their authored full-size parts. Chaos Chao keep their special appearance.'),
    ]
    quick = ''.join('<div><h3>' + html.escape(title) + '</h3><p>' + html.escape(copy) + '</p></div>' for title, copy in controls)
    page = '''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Chao Garden 0.11.0 — Refinement gallery</title><style>
:root{color-scheme:light}*{box-sizing:border-box}body{margin:0;background:#edf3f5;color:#173f4b;font:16px/1.6 system-ui,sans-serif}header,main,footer{max-width:1600px;margin:auto;padding:28px}header{padding-top:42px}h1{font-size:clamp(30px,4vw,49px);line-height:1.15;margin:0 0 18px}h2{font-size:25px;line-height:1.25;margin:0 0 9px}h3{font-size:17px;margin:0 0 8px}p{margin:0 0 14px}header>p{max-width:1050px;color:#47636e}.badge{font-size:13px;letter-spacing:.08em;font-weight:700;color:#287559}.controls{display:flex;gap:16px;flex-wrap:wrap;align-items:end;margin:28px 0 0}.search{flex:1;min-width:230px}label{display:block;font-size:14px;font-weight:650;margin-bottom:6px}input,select,button{font:inherit}input,select{padding:12px;border:1px solid #9fbac4;background:#fff;border-radius:9px;color:#173f4b}input{width:100%}button:focus-visible,a:focus-visible,input:focus-visible,select:focus-visible{outline:3px solid #e39935;outline-offset:4px}#count{font-size:14px;color:#47636e;margin:12px 0 0}.quick{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:18px;margin-top:28px}.quick>div{background:#deebe9;border-radius:10px;padding:18px}.quick p{font-size:14px;margin:0}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,540px),1fr));gap:24px;padding-top:0}.card{background:white;border:1px solid #d4e2e7;border-radius:14px;overflow:hidden}.capture{display:block;width:100%;border:0;padding:0;background:#132c35;cursor:zoom-in}.capture img{display:block;width:100%;height:auto}.copy{padding:24px}.copy p:last-child{margin-bottom:0}.number{font-size:11px;letter-spacing:.09em;color:#287559;font-weight:750}.subtitle{color:#47636e;font-weight:600}.copy p{font-size:15px}footer{padding-bottom:45px;color:#47636e}footer p{max-width:1000px}a{color:#146c85}dialog{width:min(96vw,1500px);max-height:96vh;padding:14px;border:0;border-radius:12px;background:#edf3f5;color:#173f4b}dialog::backdrop{background:#0c222ce8}dialog img{display:block;max-width:100%;max-height:76vh;width:auto;margin:auto}dialog p{text-align:center;font-size:14px;margin:12px 0 0}dialog .actions{display:flex;justify-content:center;gap:12px;margin-top:12px}dialog button{border:1px solid #9fbac4;border-radius:7px;padding:8px 18px;color:#173f4b;background:white;cursor:pointer}[hidden]{display:none!important}@media(prefers-reduced-motion:no-preference){button{transition:background .15s}}@media(max-width:600px){header,main,footer{padding-left:16px;padding-right:16px}.copy{padding:18px}}
</style></head><body><header><p class="badge">CHAO GARDEN 0.11.0 · MINECRAFT JAVA 1.21.1 / NEOFORGE</p>
<h1>Fuller trees. Clearer sprites. Animal parts that grow together.</h1>
<p>The 0.11 refinement brings taller fruit trees with real leaf canopies, improved item sprites, red triangular race pennants, and gradual animal-part acquisition with mixed appearances.</p>
<p>''' + str(len(paths)) + ''' full frames below come from the real Minecraft client, after synchronized scene and rendered-frame barriers. The final card is a separately labelled sheet of authored pixel art. Images are embedded, so this gallery opens offline.</p>
<div class="quick">''' + quick + '''</div>
<div class="controls"><div class="search"><label for="search">Find a refinement</label><input type="search" id="search" placeholder="Try foliage, genes, triangle flags, eggs…"></div><div><label for="filter">Show</label><select id="filter"><option value="all">All refinements</option><option value="orchard">Larger orchards</option><option value="sprites">Item sprites</option><option value="genes">Animal parts</option><option value="flags">Triangular flags</option><option value="companion">Held Chao</option><option value="guide">In-game guide</option></select></div></div><p id="count" role="status" aria-live="polite"></p></header><main>''' + ''.join(cards) + '''</main>
<footer><p>The existing gardens, multiplayer races and karate, Black Market, inherited coats, 48 themes, ordinary evolutions and Light/Angel/Devil Chaos forms remain. Adult equipment companions still require Accessories and oωo. See START-HERE.txt and source/TESTING.md for precise controls and the completed release checks.</p>
<p>Historical studies: <a href="historical-0.10-mechanics-gallery.html">0.10 mechanics baseline</a> · <a href="historical-0.9-head-gallery.html">0.9 crowns and forehead goggles</a>. Those galleries retain their original release labels and are not new 0.11 captures.</p>
<p>Unofficial fan project. Models and textures are original reconstructions; included source documents describe Chao Island references, Minecraft adaptations and audio provenance.</p></footer>
<dialog id="zoom" aria-label="Enlarged preview"><img alt=""><p></p><div class="actions"><button id="previous" type="button">Previous</button><button id="close" type="button">Close</button><button id="next" type="button">Next</button></div></dialog>
<script>
const cards=[...document.querySelectorAll('.card')],search=document.querySelector('#search'),filter=document.querySelector('#filter'),count=document.querySelector('#count'),zoom=document.querySelector('#zoom');let current=0;
function visible(){return cards.filter(card=>!card.hidden)}
function update(){const query=search.value.trim().toLowerCase();for(const card of cards)card.hidden=!(filter.value==='all'||card.dataset.category===filter.value)||!card.dataset.search.includes(query);const list=visible(),n=list.filter(card=>card.dataset.origin==='minecraft').length,a=list.length-n;count.textContent=n+' game capture'+(n===1?'':'s')+(a?' + '+a+' authored art sheet':'')+' · select an image to enlarge';}
function show(card){const image=card.querySelector('img');current=cards.indexOf(card);zoom.querySelector('img').src=image.src;zoom.querySelector('img').alt=image.alt;zoom.querySelector('p').textContent=card.querySelector('h2').textContent+' — '+card.querySelector('.subtitle').textContent;if(!zoom.open)zoom.showModal();}
function step(offset){const list=visible();if(!list.length)return;const position=list.indexOf(cards[current]);show(list[(position+offset+list.length)%list.length]);}
search.addEventListener('input',update);filter.addEventListener('change',update);for(const card of cards)card.querySelector('.capture').addEventListener('click',()=>show(card));document.querySelector('#close').addEventListener('click',()=>zoom.close());document.querySelector('#previous').addEventListener('click',()=>step(-1));document.querySelector('#next').addEventListener('click',()=>step(1));zoom.addEventListener('click',event=>{if(event.target===zoom)zoom.close()});zoom.addEventListener('keydown',event=>{if(event.key==='ArrowLeft'){event.preventDefault();step(-1)}if(event.key==='ArrowRight'){event.preventDefault();step(1)}});update();
</script></body></html>'''
    require('http://' not in page and 'https://' not in page, 'Gallery acquired an external dependency')
    require(page.count('<article class="card"') == len(paths) + 1, 'Missing gallery cards')
    require(page.count('data-origin="minecraft"') == len(paths), 'Unlabelled game capture')
    require(page.count('data-origin="authored"') == 1, 'Authored art label is missing')
    write_flushed(output, page.encode())



def historical_launcher(label, target):
    """Point to the one complete historical gallery already kept in source/."""
    return ('<!doctype html><html lang="en"><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width,initial-scale=1">'
            '<title>Historical Chao Garden ' + html.escape(label) + ' gallery</title>'
            '<style>body{max-width:800px;margin:60px auto;padding:24px;font:18px/1.6 system-ui;'
            'background:#edf3f5;color:#173f4b}a{color:#146c85}h1{line-height:1.2}</style>'
            '<h1>Historical Chao Garden ' + html.escape(label) + ' gallery</h1>'
            '<p>This gallery records a previous release. Its trees, sprites or animal parts '
            'may differ from the current 0.11 refinement.</p><p><a href="'
            + html.escape(target, quote=True) + '">Open the complete historical gallery</a></p>'
            '<p>Extract the complete ZIP before following this local link. The gallery and '
            'all its images are included in the source documents.</p></html>')

def validate_production_jar(source_jar, root):
    with zipfile.ZipFile(source_jar) as jar:
        names = jar.namelist()
        metadata = jar.read('META-INF/neoforge.mods.toml').decode()
        require(re.search(r'(?m)^version\s*=\s*"' + re.escape(VERSION) + r'"', metadata) is not None,
                'Production jar does not report version ' + VERSION)
        require(metadata.count('[[mods]]') == 1 and 'chao_variant_fixtures' not in metadata,
                'Fixture mod metadata leaked into production')
        for name in names:
            require(not any(token in name for token in ('variantfixtures', 'ChaoVisualSmoke', 'MechanicsVisualSmoke', 'RefinementVisualSmoke',
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
    log_path = Path(capture_log).resolve() if capture_log else root.parent / 'chao011-refinement-client.log'
    out.mkdir(parents=True, exist_ok=True)
    docshots = root / 'docs/screenshots'
    refinement = docshots / 'refinement-0.11'
    refinement.mkdir(parents=True, exist_ok=True)
    names, count, capture_record = read_capture_session(log_path, root)
    frames, records = [], []
    for name in names:
        source = root / 'run/screenshots' / name
        size = validate_frame(source)
        target = refinement / name
        copy_flushed(source, target)
        frames.append(target)
        records.append({'file': name, 'width': size[0], 'height': size[1], 'sha256': sha256(target),
                        'title': SHOTS[source.stem.removeprefix(SHOT_PREFIX)][0]})
    # Only stale files in this release's own screenshot directory are removed.
    for obsolete in refinement.glob('chao011-*.png'):
        if obsolete.name not in names:
            obsolete.unlink()
    sprite_sheet = docshots / 'item-sprites-0.11.png'
    validate_frame(sprite_sheet)
    manifest = {'release': VERSION, 'minecraft': '1.21.1', 'neoforge': '21.1.219',
                'capture_count': count, 'origin': 'Actual Minecraft client screenshots',
                'full_frames': True, 'capture_log_sha256': sha256(log_path), 'captures': records,
                'authored_sprite_sheet': {'file': sprite_sheet.name, 'sha256': sha256(sprite_sheet),
                                         'origin': 'Authored pixel art contact sheet; not a game capture'}}
    manifest_path = root / 'docs/refinement-capture-manifest-0.11.json'
    record_path = root / 'docs/refinement-capture-record-0.11.txt'
    write_flushed(manifest_path, (json.dumps(manifest, indent=2, ensure_ascii=False) + '\n').encode())
    write_flushed(record_path, capture_record.encode())

    preview = out / f'chao-garden-{VERSION}-preview.png'
    gallery = root / 'docs/refinement-gallery.html'
    build_feature_sheet(frames, preview)
    copy_flushed(preview, docshots / preview.name)
    build_gallery(frames, gallery, sprite_sheet)
    copy_flushed(gallery, out / f'chao-garden-{VERSION}-gallery.html')
    copy_flushed(sprite_sheet, out / sprite_sheet.name)
    histories = []
    for original, filename, label in (
        ('mechanics-gallery.html', 'historical-0.10-mechanics-gallery.html', '0.10'),
        ('head-refinement-gallery.html', 'historical-0.9-head-gallery.html', '0.9'),
    ):
        history_source = root / 'docs' / original
        historical = root / 'docs' / filename
        require(label in history_source.read_text(), 'Historical gallery lost its release label: ' + original)
        # Keep the original document and its images once in source/. New
        # history links are small labelled launchers rather than another full
        # base64-image copy. Existing 0.9 documents are left byte-for-byte intact.
        if label == '0.10':
            write_flushed(historical, historical_launcher(label, original).encode())
        elif not historical.exists():
            copy_flushed(history_source, historical)
        copy_flushed(history_source, out / historical.name)
        histories.append((filename, label, original))
    start_path = root / 'docs/START-HERE-0.11.txt'
    write_flushed(start_path, start_here(count).encode())
    script_copy = root / 'scripts/package_chao011.py'
    if SCRIPT != script_copy.resolve():
        copy_flushed(SCRIPT, script_copy)
    if previews_only:
        print(f'Previews ready: {count} validated full Minecraft frames; feature sheet, authored sprite sheet and offline gallery.')
        print('Gallery:', out / f'chao-garden-{VERSION}-gallery.html')
        print('Preview:', preview)
        print('Full frames:', refinement)
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
    with zipfile.ZipFile(staging, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as bundle:
        bundle.write(start_path, prefix + 'START-HERE.txt')
        bundle.write(jar, prefix + jar.name)
        for path, relative in source_files(root):
            bundle.write(path, prefix + 'source/' + relative)
        for project in projects:
            bundle.write(project, prefix + 'Blockbench/' + project.name)
        bundle.write(root / 'art/blockbench/README.md', prefix + 'Blockbench/EDITING.md')
        bundle.write(root / 'docs/BLOCKBENCH_VALIDATION.md', prefix + 'Blockbench/VALIDATION.md')
        bundle.write(gallery, prefix + 'previews/index.html')
        for filename, label, original in histories:
            bundle.writestr(prefix + 'previews/' + filename,
                            historical_launcher(label, '../source/docs/' + original))
        bundle.write(preview, prefix + 'previews/' + preview.name)
        bundle.write(sprite_sheet, prefix + 'previews/' + sprite_sheet.name)
        bundle.write(manifest_path, prefix + 'previews/CAPTURE-MANIFEST.json')
        bundle.write(record_path, prefix + 'previews/CAPTURE-RECORD.txt')
        for path in frames:
            bundle.write(path, prefix + 'previews/refinement-0.11/' + path.name)
        bundle.write(root / 'docs/MOD_VARIANTS.md', prefix + 'previews/HISTORICAL-THEME-CATALOG.md')
        bundle.writestr(prefix + 'SHA256SUMS.txt', checksum + '  ' + jar.name + '\n')
    with zipfile.ZipFile(staging) as bundle:
        require(bundle.testzip() is None, 'Complete ZIP CRC failure')
        entries = bundle.namelist()
        require(len(entries) == len(set(entries)), 'Duplicate complete ZIP entries')
        require(sum(name.startswith(prefix + 'previews/refinement-0.11/') for name in entries) == count,
                'ZIP screenshot count differs from completed session')
        require(sum(name.startswith(prefix + 'Blockbench/') and name.endswith('.bbmodel') for name in entries) == 8,
                'ZIP is missing a Blockbench project')
        require(prefix + 'source/gradle/wrapper/gradle-wrapper.jar' in entries, 'Missing build wrapper')
        require(bundle.read(prefix + jar.name) == source_jar.read_bytes(), 'ZIP jar differs from built jar')
        for record in records:
            require(hashlib.sha256(bundle.read(prefix + 'previews/refinement-0.11/' + record['file'])).hexdigest()
                    == record['sha256'], 'ZIP screenshot hash differs: ' + record['file'])
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
                   + sha256(sprite_sheet) + '  ' + sprite_sheet.name + '\n'
                   + sha256(out / f'chao-garden-{VERSION}-gallery.html') + '  ' + f'chao-garden-{VERSION}-gallery.html\n').encode())
    print('Complete ZIP:', archive, archive.stat().st_size, 'bytes;', len(entries), 'entries')
    print('ZIP SHA256:', sha256(archive))
    print('Jar:', jar, 'SHA256:', checksum)
    print('Gallery:', out / f'chao-garden-{VERSION}-gallery.html')
    print('Preview:', preview)
    print('Authored sprite sheet:', out / sprite_sheet.name)
    print('Full frames:', refinement)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--previews-only', action='store_true', help='Validate captures and create galleries/images without packaging the production JAR')
    parser.add_argument('--capture-log', type=Path, help='Completed refinement client log (default: project parent/chao011-refinement-client.log)')
    parser.add_argument('--project-root', type=Path, help='NeoForge project directory')
    parser.add_argument('--output-dir', type=Path, help='Deliverable directory')
    options = parser.parse_args()
    main(options.previews_only, options.capture_log, options.project_root, options.output_dir)
