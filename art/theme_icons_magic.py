"""Original, mod-specific magic emblems, authored on native 8/16 pixel grids.

The 8px marks are explicit tiny designs, not downsampled 16px artwork.  They
preserve the item silhouette and its identifying colors when inset into an egg.
No source-mod texture pixels are imported. Sources/motifs are recorded in
``docs/research/MAGIC_ICON_REFINEMENT_014.md``.
"""
from PIL import Image, ImageDraw

ICON_IDS = {
    'thaumcraft', 'astral_sorcery', 'botania', 'blood_magic', 'ars_nouveau',
    'occultism', 'hexerei', 'natures_aura', 'roots', 'iron_spells', 'malum',
    'evilcraft', 'mystical_agriculture', 'hexcasting', 'draconic_evolution',
    'tinkers',
}

# Each tiny design uses its own material palette rather than applying one coat
# tint to a generic symbol. '.' is transparent and every other pixel is opaque.
# The narrow 8px shapes were drawn directly for the egg's small badge area.
MINI = {
 'thaumcraft': (
  ('........','........','.gg..gg.','gppggppg','gphggphg','.gg..gg.','........','........'),
  {'g':'c89937','p':'4e1c86','h':'b99be0'}),
 'astral_sorcery': (
  ('...w....','...ww...','..wllw..','wwlbbllh','..lbblh.','...lwh..','...hh...','...h....'),
  {'w':'f0f5ed','l':'9bdde8','b':'498acc','h':'8d9fc6'}),
 'botania': (
  ('......gg','.....glg','....glg.','...glg..','..ygg...','.byy....','.bb.....','b.......'),
  {'g':'319c66','l':'a9f0a8','y':'dbbd58','b':'765448'}),
 'blood_magic': (
  ('...d....','...rd...','..drrd..','.dhrrsd.','.drrrsd.','.drhrrd.','..drrd..','...dd...'),
  {'d':'661f30','r':'c72f42','s':'8c1e32','h':'f48a8f'}),
 'ars_nouveau': (
  ('........','.gggggg.','.gppppg.','.gppppg.','.gppppg.','.gppppg.','.gwwwwg.','........'),
  {'g':'b39b58','p':'75499b','w':'e9dfc3'}),
 'occultism': (
  ('..rrrr..','.rpsppr.','rppssppr','rssssssr','rpsssspr','rppssppr','.rsppsr.','..rrrr..'),
  {'r':'ac9bb2','s':'fff0d0','p':'51425d'}),
 'hexerei': (
  ('......bb','.....bb.','....bb..','...bb...','..yy....','.yyyt...','yyyytt..','.tttt...'),
  {'b':'745245','y':'c5b77c','t':'8f784f'}),
 'natures_aura': (
  ('.....yy.','...yyyy.','..yyhyy.','.yyyhyy.','.yyhy...','.yhy....','..g.....','.g......'),
  {'y':'d9b849','h':'fff0a1','g':'687e3b'}),
 'roots': (
  ('...gg...','.glgbg..','..gbglg.','...bgg..','...b....','..bb....','..b.....','.b......'),
  {'g':'5e9456','l':'a4cc79','b':'8e6645'}),
 'iron_spells': (
  ('.bbbbbb.','dhwwwwbd','.bwhhwb.','.bwiiwb.','.bwhwwb.','.bwiiwb.','dbwwwwhd','.bbbbbb.'),
  {'d':'634531','b':'9d895f','w':'e9dfb9','h':'fff2ce','i':'625a40'}),
 'malum': (
  ('....p...','...pop..','..popp..','.pohop..','ppohopp.','.pooop..','..ppp...','........'),
  {'p':'74508f','o':'e49c68','h':'ffdd9c'}),
 'evilcraft': (
  ('...dd...','..drrd..','.drsrdd.','drsssrdd','.dssrrd.','..drrd..','...dd...','........'),
  {'d':'342632','r':'a73358','s':'662a49'}),
 'mystical_agriculture': (
  ('........','..gggg..','.gllglg.','glggyglg','glyygylg','.glylg..','..ggg...','........'),
  {'g':'468340','l':'a6d66b','y':'d9e495'}),
 'hexcasting': (
  ('.....pp.','....plpp','....ppp.','...bb...','..bb....','.bb.....','bb......','........'),
  {'p':'9c6dd1','l':'e6c5fb','b':'8e6549'}),
 'draconic_evolution': (
  ('.oo..oo.','oyoooyyo','oyyorhyo','oyyooyyo','.oyoyyo.','..oyyo..','...oo...','...o....'),
  {'o':'e45a0a','y':'ffd158','r':'ce2c0d','h':'fff09c'}),
 'tinkers': (
  ('........','..ccccc.','.cllcccc','..ccbcc.','....b...','...bb...','..bb....','..b.....'),
  {'c':'4579a0','l':'9cc7d5','b':'8e6244'}),
}


def _mini(ident):
    rows, colors = MINI[ident]
    if len(rows) != 8 or any(len(row) != 8 for row in rows):
        raise ValueError('Magic icon is not native 8px: ' + ident)
    image = Image.new('RGBA', (8, 8), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, mark in enumerate(row):
            if mark != '.':
                image.putpixel((x, y), (*bytes.fromhex(colors[mark]), 255))
    return image


def render_theme(ident, size=16):
    """Render one supported motif, retaining a separately authored 8px mark.

    16px designs keep the small silhouette, with individual native texels for
    lens edges, crystals, book bindings, leaf veins and mechanical core facets.
    Neither output includes a colored badge background or resampling fringe.
    """
    if ident not in ICON_IDS:
        raise KeyError(ident)
    if size not in (8, 16):
        raise ValueError('Magic emblem size must be 8 or 16')
    image = _mini(ident)
    if size == 8:
        return image
    image = image.resize((16, 16), Image.Resampling.NEAREST)
    d = ImageDraw.Draw(image)
    if ident == 'thaumcraft':
        # A real pair of individually framed square gold lenses, distinct from
        # the wider brass bridge and dark teal glazing of Create's goggles.
        d.line((2, 6, 5, 6), fill='#f4d36b')
        d.line((10, 6, 13, 6), fill='#f4d36b')
        d.point((3, 9), fill='#eee4fc'); d.point((11, 9), fill='#eee4fc')
        d.line((3, 10, 5, 10), fill='#35115f'); d.line((11, 10, 13, 10), fill='#35115f')
    elif ident == 'astral_sorcery':
        # Shifting Star: long white/cyan rays and a blue central jewel.
        image = Image.new('RGBA', (16,16), (0,0,0,0)); d=ImageDraw.Draw(image)
        d.polygon([(7,0),(8,0),(8,4),(10,6),(15,7),(15,8),(10,9),
                   (9,11),(8,15),(7,15),(6,10),(4,9),(0,8),(0,7),
                   (5,7),(6,5),(6,2)], fill='#91cdda')
        d.polygon([(7,0),(8,0),(8,5),(10,7),(7,7),(5,8),(0,8),(0,7),(5,7),(6,5),(6,2)],fill='#eef5ed')
        d.polygon([(9,6),(15,7),(15,8),(10,9),(8,12),(8,8)],fill='#b5e9ed')
        d.polygon([(8,9),(9,11),(8,15),(7,15),(7,11)],fill='#8499bd')
        d.rectangle((6,7,9,10),fill='#529bd8')
        d.rectangle((6,7,7,8),fill='#d8f9f2')
        d.line((8,8,8,10),fill='#296ab5')
        d.point((9,7),fill='#84cce8')
    elif ident == 'botania':
        d.line((12,2,7,7), fill='#d5ffd0')
        d.line((11,5,7,9), fill='#297a58')
        d.point((4,10), fill='#ffe995'); d.point((2,13), fill='#a67a51')
    elif ident == 'blood_magic':
        # Neo Vitae: a narrow, sharp tip above a full, rounded lower droplet.
        image=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(image)
        d.ellipse((1,6,14,15),fill='#661f30')
        d.polygon([(7,0),(7,2),(8,4),(11,7),(4,7),(6,4)],fill='#661f30')
        d.ellipse((2,7,13,14),fill='#c72f42')
        d.polygon([(7,3),(7,5),(10,8),(5,8),(6,6)],fill='#c72f42')
        d.polygon([(11,7),(13,9),(13,12),(11,14),(8,14),(10,12),(11,10)],fill='#8c1e32')
        d.line((4,9,4,11),fill='#f48a8f')
        d.point((5,8),fill='#ffbcc0')
    elif ident == 'ars_nouveau':
        # A centered plain spellbook: binding and pages, with no cross/glyph.
        image=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(image)
        d.rectangle((2,2,13,13),fill='#b39b58')
        d.rectangle((4,3,12,10),fill='#75499b')
        d.line((3,3,3,11),fill='#8a703e')
        d.line((5,4,11,4),fill='#9568b5')
        d.rectangle((4,11,12,12),fill='#e9dfc3')
        d.line((5,12,11,12),fill='#c4b58d')
    elif ident == 'occultism':
        # One unadorned chalk ring and a five-point star; no secondary runes.
        image=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(image)
        d.ellipse((0,0,15,15),fill='#51425d')
        d.ellipse((1,1,14,14),outline='#ac9bb2',width=1)
        d.line([(7,3),(10,12),(3,6),(12,6),(4,12),(7,3)],fill='#fff0d0',width=1)
    elif ident == 'hexerei':
        d.line((12,2,7,7), fill='#a8845b')
        d.line((3,10,1,12), fill='#e4d49a')
        d.line((6,10,3,14), fill='#dbc58e')
        d.line((8,12,6,15), fill='#6e5c3f')
        d.line((4,8,8,10), fill='#826548')
    elif ident == 'natures_aura':
        d.line((4,11,12,3), fill='#fff3aa')
        d.line((7,8,7,5), fill='#edcf64')
        d.line((8,7,11,7), fill='#ab882d')
        d.point((12,3), fill='#fff8c9')
    elif ident == 'roots':
        d.line((7,9,4,14), fill='#bd9163')
        d.line((4,4,6,5), fill='#c4df9a')
        d.point((10,4), fill='#8ac074')
        d.point((6,9), fill='#614c37')
    elif ident == 'iron_spells':
        # A centered unfurled scroll: straight parchment, two rolled ends,
        # small end curls and three short horizontal lines of ink.
        image=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(image)
        d.rectangle((3,2,12,13),fill='#9d895f')
        d.rectangle((4,3,11,12),fill='#e9dfb9')
        d.line((4,4,4,11),fill='#fff2ce')
        d.line((11,4,11,11),fill='#c6b489')
        d.line((6,5,9,5),fill='#625a40')
        d.line((6,7,10,7),fill='#625a40')
        d.line((6,9,8,9),fill='#625a40')
        for top in (0,12):
            d.rounded_rectangle((1,top,14,top+3),radius=1,fill='#634531')
            d.rectangle((2,top+1,13,top+2),fill='#e9dfb9')
            d.line((3,top+1,12,top+1),fill='#fff2ce')
            d.line((3,top+2,12,top+2),fill='#9d895f')
            d.point((2,top+1),fill='#9d895f')
            d.point((13,top+2),fill='#9d895f')
    elif ident == 'malum':
        d.line((8,4,6,8), fill='#fff1bf')
        d.point((10,8), fill='#493554')
        d.line((7,11,10,11), fill='#ad5f57')
        d.point((7,6), fill='#fbc68c')
    elif ident == 'evilcraft':
        d.line((6,3,4,5), fill='#de6081')
        d.line((9,5,11,8), fill='#4d243e')
        d.line((6,10,8,12), fill='#bd4265')
        d.point((6,4), fill='#fca0ac')
    elif ident == 'mystical_agriculture':
        d.line((4,4,6,3), fill='#d1ed90')
        d.line((3,6,3,8), fill='#9bc55b')
        d.point((9,6), fill='#f5f0b4')
        d.line((6,10,8,10), fill='#69a248')
        d.point((11,5), fill='#c4e581')
    elif ident == 'hexcasting':
        d.line((10,2,12,1), fill='#e5c8ff')
        d.line((11,3,11,5), fill='#d8b3ee')
        d.line((8,5,9,3), fill='#653e94')
        d.line((6,7,2,11), fill='#bf9771')
        d.point((0,12), fill='#5e4737')
    elif ident == 'draconic_evolution':
        # Dragon Heart: two fiery orange/yellow lobes and a long lower point.
        image=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(image)
        d.polygon([(3,1),(5,1),(7,3),(9,1),(12,1),(14,3),(15,6),
                   (14,9),(11,12),(8,15),(6,14),(4,11),(1,8),(0,5),(1,3)],fill='#e45a0a')
        d.polygon([(3,3),(5,2),(7,5),(9,5),(11,2),(13,3),(14,6),
                   (12,9),(10,11),(8,14),(6,11),(4,10),(2,7),(2,4)],fill='#ffd158')
        d.polygon([(7,3),(9,3),(10,6),(8,9),(8,14),(7,13),(6,8)],fill='#ce2c0d')
        d.line((4,4,4,7),fill='#fff09c',width=2)
        d.line((11,4,11,7),fill='#fff09c',width=2)
        d.line((10,9,8,12),fill='#ffb72f')
    elif ident == 'tinkers':
        d.line((4,2,10,2), fill='#b6dae7')
        d.line((10,3,13,4), fill='#7ba8c3')
        d.line((4,5,7,5), fill='#2e567a')
        d.line((7,9,4,13), fill='#bd936a')
        d.point((7,7), fill='#564533')
    return image
