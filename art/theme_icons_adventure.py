"""Original tiny adventure-mod emblems, authored on native 8px and 16px grids.

These are readable item/creature tributes rather than reproduced logo textures.
The 8px designs are deliberately authored, never reduced from the larger art.
"""
from PIL import Image, ImageDraw


# Each eight-character row is a directly authored inventory-size symbol.  A
# compact, mod-specific palette keeps important shapes distinct on Chao coats.
_ART = {
    'cataclysm': (
        ('g......g', 'go....og', '.ggoogg.', '.oddddo.',
         '.oggggo.', '.odrrdo.', '..orro..', '...oo...'),
        {'g':'e6ad3f','o':'252735','d':'484653','r':'f37335'}),
    'ice_fire': (
        ('r......b', 'rrr..bbb', '.rr..bb.', 'rwrrbbwb',
         'rrr..bbb', '.rr..bb.', '..r..b..', '...rb...'),
        {'r':'c54d42','b':'63c9e8','w':'fff4cd'}),
    'aether': (
        ('.gggggg.', 'gGggGgGg', 'ggbbbbgg', 'gGbbblGg',
         'ggbwlbgg', 'gGbbblGg', 'ggbbbbgg', '.gggggg.'),
        {'g':'c48d39','G':'ffe59c','b':'69b9df','l':'b9eaff','w':'f6ffff'}),
    'twilight_forest': (
        ('r..w..p.', 'g.gg.gg.', '.gbbbbg.', 'ggbllbgg',
         'ggbllbgg', '.gbbbbg.', 'g.gg.gg.', 'p..r..w.'),
        {'g':'477c38','b':'333e9a','l':'7951c5','r':'ed7966','w':'fff4cb','p':'c78bdd'}),
    'nether': (
        ('...oo...', '..obbo..', '.obdbbo.', '.olbbdo.',
         '.obddbo.', '.odbldo.', '..obbo..', '..oooo..'),
        {'o':'47282c','b':'854449','l':'b06662','d':'5c3238'}),
    'end': (
        ('...pp...', '..phhp..', '.pdddbp.', '.pbdcdp.',
         '.pdbbdp.', '.pddcdp.', '..pddp..', '..pppp..'),
        {'p':'713f9c','h':'b77be5','d':'101b29','b':'182c35','c':'70c4bc'}),
    'alexs_caves': (
        ('...g....', '..glgg..', '.glllb..', 'ggloolg.',
         '.gobolg.', '.gloolg.', '..ggg...', '...g....'),
        {'g':'4e8d33','l':'b7ec59','b':'729d42','o':'314a32'}),
    'alexs_mobs': (
        ('.oooooo.', 'okbbbboo', 'okbyybpo', 'okbpbpbo',
         'okbppbbo', 'okbbpbbo', 'okwwwwwo', '.oooooo.'),
        {'o':'413632','k':'d59c54','b':'805a37','y':'eadcae','p':'312c28','w':'d6c9a4'}),
    'cobblemon': (
        ('..oooo..', '.orrrro.', 'orhrrrro', 'oodwwdoo',
         'oodwwdoo', 'owwwwwwo', '.owwwwo.', '..oooo..'),
        {'o':'263743','r':'d44c48','h':'fa9270','d':'324554','w':'f2f2db'}),
    'undergarden': (
        ('..oooo..', '.ommmmo.', 'ombllbmo', 'omlwwlmo',
         '.obllbo.', '..ommo..', '..occo..', '...oo...'),
        {'o':'343d39','m':'8b928a','c':'b17442','b':'447f72','l':'69c7ab','w':'d5f5d7'}),
    'deeper_darker': (
        ('c......c', 'cc....cc', '.cooooc.', '.oddddo.',
         '.odccdo.', '.occcco.', '..occo..', '...oo...'),
        {'o':'183541','d':'1f4c53','c':'67cfce'}),
    'blue_skies': (
        ('.oooooo.', 'ogbbbboo', 'ogblwbbo', 'ogbwwwbo',
         'ogblwbbo', 'ogbbbboo', 'ogwwwwwo', '.oooooo.'),
        {'o':'243f69','g':'dab562','b':'459bd8','l':'9ee7ed','w':'f0ffef'}),
    'betweenlands': (
        ('..gggg..', '.ggllgg.', 'gglgglgg', 'glg..glg',
         'glg..glg', 'gglgglgg', '.ggllgg.', '..gggg..'),
        {'g':'82984b','l':'d6d9a0'}),
    'quark': (
        ('.oo..oo.', 'ocloolco', 'ollllllo', 'ollccllo',
         '.olcclo.', '..ollo..', '...oo...', '........'),
        {'o':'254b60','c':'3caebf','l':'9cecf0'}),
    'vampirism': (
        ('.oooooo.', 'orrrrrro', '.owrrwo.', '..w..w..',
         '...rr...', '..rrrr..', '...rr...', '........'),
        {'o':'422d43','r':'b53752','w':'ffefd7'}),
    'born_in_chaos': (
        ('...oo...', '..orro..', '.orhrro.', '.orrrso.',
         '.orrsso.', '..orsso.', '..osso..', '...oo...'),
        {'o':'310f25','r':'a91b35','h':'ed3347','s':'64152d'}),
}

ICON_IDS = set(_ART)


def _native_eight(ident):
    rows, palette = _ART[ident]
    if len(rows) != 8 or any(len(row) != 8 for row in rows):
        raise ValueError('Malformed native icon grid: ' + ident)
    image = Image.new('RGBA', (8, 8), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != '.':
                image.putpixel((x, y), (*bytes.fromhex(palette[key]), 255))
    return image


def render_theme(ident, size=16):
    """Return a binary-alpha original emblem at its authored 8px or 16px size."""
    if ident not in ICON_IDS:
        raise KeyError(ident)
    if size not in (8, 16):
        raise ValueError('Adventure emblems are authored at 8px and 16px')
    small = _native_eight(ident)
    if size == 8:
        return small
    image = small.resize((16, 16), Image.Resampling.NEAREST)
    d = ImageDraw.Draw(image)
    # Larger badges gain individual-texel highlights and object-specific marks;
    # native egg emblems keep the deliberately simplified silhouette above.
    if ident == 'cataclysm':
        # The 8px bridge joins the two old center dots. Restore their previous
        # dark separator only for the explicitly unchanged larger helmet.
        d.rectangle((6, 8, 9, 9), fill='#484653')
        d.line((4, 7, 11, 7), fill='#e6ad3f')
        d.line((6, 10, 9, 10), fill='#ffbc5d')
    elif ident == 'ice_fire':
        d.line((2, 4, 5, 5), fill='#f39a6a')
        d.line((10, 5, 13, 4), fill='#c1f5ff')
        d.point((2, 7), fill='#372633'); d.point((12, 7), fill='#263849')
    elif ident == 'aether':
        d.line((5, 7, 8, 7), fill='#f8ffff')
        d.line((8, 8, 10, 8), fill='#f8ffff')
        d.point((2, 3), fill='#fff6b7'); d.point((13, 11), fill='#fff6b7')
    elif ident == 'twilight_forest':
        d.line((6, 7, 9, 7), fill='#b894f4')
        d.line((5, 10, 8, 10), fill='#8b75d3')
        for x,y in ((0,0),(7,0),(12,1),(1,14),(6,15),(14,14)):
            if image.getpixel((x,y))[3]: d.point((x,y), fill='#fff6c2')
    elif ident == 'nether':
        # Pitted red stone on an egg, matching the complete Nether egg shell.
        for x,y in ((6,4),(9,6),(3,9),(8,11),(11,10)):
            d.point((x,y), fill='#ca8680')
        d.line((5,7,7,8), fill='#5c3238')
        d.line((8,10,10,11), fill='#5c3238')
    elif ident == 'end':
        # Purple shell rim enclosing the dark teal starfield of an End portal.
        for x,y in ((5,6),(10,7),(8,11),(4,10)):
            d.point((x,y), fill='#d0eee1')
        d.point((9,5), fill='#70c4bc')
        d.line((6,2,8,2), fill='#b77be5')
    elif ident == 'alexs_caves':
        d.line((6, 3, 6, 5), fill='#e6ff92')
        d.line((3, 7, 4, 7), fill='#dcff8d')
        d.point((10, 5), fill='#e6ff92')
        d.polygon(((7,5),(9,5),(8,7)), fill='#243b28')
        d.polygon(((5,8),(5,10),(7,10)), fill='#243b28')
        d.polygon(((10,8),(11,10),(9,10)), fill='#243b28')
        d.point((8,8), fill='#243b28')
    elif ident == 'alexs_mobs':
        d.line((3, 3, 3, 11), fill='#edbf7b')
        # Pawprint on the Animal Dictionary, a core item unique to this mod.
        for x,y in ((7,5),(10,5),(6,7),(11,7)):
            d.point((x,y), fill='#f0d8a6')
        d.rectangle((8,8,9,9), fill='#f0d8a6')
        d.line((5, 13, 12, 13), fill='#fff0cc')
    elif ident == 'cobblemon':
        d.line((5, 3, 8, 3), fill='#ffb19a')
        d.point((5, 4), fill='#ffe4bb')
        d.rectangle((7, 8, 8, 9), fill='#ffffff')
        d.line((5, 12, 10, 12), fill='#c1d6ce')
    elif ident == 'undergarden':
        d.line((5, 3, 10, 3), fill='#d2ddd2')
        d.point((6, 5), fill='#deffe0')
        d.line((7, 12, 8, 12), fill='#dfa56d')
    elif ident == 'deeper_darker':
        d.line((3, 1, 3, 3), fill='#b2e9da')
        d.line((12, 1, 12, 3), fill='#b2e9da')
        d.line((5, 10, 10, 10), fill='#c2ece1')
        d.rectangle((7, 7, 8, 8), fill='#15333e')
    elif ident == 'blue_skies':
        d.line((3, 3, 3, 11), fill='#f5d89c')
        d.point((8, 6), fill='#ffffff')
        d.line((6, 13, 12, 13), fill='#c2dcdf')
    elif ident == 'betweenlands':
        # Four assembled talisman pieces retain their cross-like joining seams.
        d.line((7, 2, 7, 5), fill='#536331')
        d.line((7, 10, 7, 13), fill='#536331')
        d.line((2, 7, 5, 7), fill='#536331')
        d.line((10, 7, 13, 7), fill='#536331')
        d.point((4, 3), fill='#eff2bb')
    elif ident == 'quark':
        d.line((3, 3, 5, 3), fill='#e4ffff')
        d.line((6, 5, 7, 8), fill='#d4ffff')
        d.line((10, 5, 9, 9), fill='#53c9d7')
    elif ident == 'vampirism':
        d.line((4, 5, 4, 8), fill='#ffffff')
        d.line((10, 5, 10, 8), fill='#ffffff')
        d.point((6, 10), fill='#f0838d')
    elif ident == 'born_in_chaos':
        # Seed of Chaos: a scarlet core enclosed by dark burgundy seed tissue.
        # The reference is item/chaossemt.png in the publisher's 1.21.1 JAR.
        d.line((6,4,8,4), fill='#f3535c')
        d.line((5,5,5,7), fill='#ed3347')
        d.line((8,8,10,7), fill='#d32540')
        d.line((6,11,8,12), fill='#64152d')
        d.point((9,10), fill='#842039')
    return image
