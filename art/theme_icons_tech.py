"""Original tiny item motifs for the technology Chao themes.

Eight-pixel marks are deliberately authored separately: they are the symbols
used on 16px eggs, not reductions of the larger chest decals. Full-size marks
use their own 16px construction and opaque, palette-limited pixels.
"""
from PIL import Image, ImageDraw


ICON_IDS = {
    'create', 'ae2', 'refined_storage', 'mekanism',
    'immersive_engineering', 'thermal', 'pneumaticcraft', 'computercraft',
    'enderio', 'actually_additions', 'railcraft', 'industrialcraft',
    'buildcraft', 'projecte', 'psi', 'galacticraft',
}

MOTIFS = {
    'create': 'Wooden cogwheel with separated teeth and a dark central axle socket',
    'ae2': 'ME Controller face with a segmented rainbow core',
    'refined_storage': 'Advanced Processor: cyan crystal chip on a diagonal gray circuit substrate',
    'mekanism': 'Orange Atomic Disassembler with a green energy shaft',
    'immersive_engineering': 'Steel Engineer\'s Hammer on a wooden handle',
    'thermal': 'Redstone reception coil with metallic terminals',
    'pneumaticcraft': 'White pressure-gauge dial with a red needle',
    'computercraft': 'Stone computer bezel and green terminal prompt',
    'enderio': 'Ender pearl at the junction of compact conduits',
    'actually_additions': 'Blue powered drill with a silver bit',
    'railcraft': 'Steam locomotive, headlamp and red running board',
    'industrialcraft': 'Nuclear reactor radiation plaque',
    'buildcraft': 'Orange open-ended pipe wrench',
    'projecte': 'Round burgundy Philosopher\'s Stone with warm sparks around its rim',
    'psi': 'Psimetal CAD with cyan spell hardware',
    'galacticraft': 'Tier 1 rocket with a red nose and fins',
}

# Each plan is native 8px, including its negative space. Single-pixel features
# are intentional at this size; no 16px features are silently discarded.
_SMALL = {
    'create': ({'b':'725233','g':'bb884a','h':'edc177','d':'354b4d'}, (
        '...gg...', '.gggggg.', '.gbhhbg.', 'ggbddbgg',
        'ggbddbgg', '.gbhhbg.', '.gggggg.', '...gg...')),
    'ae2': ({'k':'303445','s':'aab6bd','w':'e7f0ea','r':'ec647b','g':'84d477','b':'6bcddd','p':'ae85e1'}, (
        '.ssssss.', 'swkkkkws', 'skrrggks', 'skpkkgks',
        'skpkkbks', 'skppbbks', 'swkkkkws', '.ssssss.')),
    'refined_storage': ({'s':'626e70','w':'d5dcd7','b':'929b99','c':'10baa4','L':'8af6df'}, (
        '...ww...', '..wccw..', '.wcLLcw.', 'wcLLLLcb',
        'wbcccbbs', '.wbbbbs.', '..wbbs..', '...ss...')),
    'mekanism': ({'k':'34494b','o':'d87724','y':'ffc05b','s':'c9d4cb','g':'55be69'}, (
        '...oyoo.', '..oyyyok', '..oyyok.', '...skk..',
        '..kgk...', '.kgk....', 'kgk.....', '.k......')),
    'immersive_engineering': ({'k':'2c3941','s':'8a9ca6','w':'dce3da','b':'875832','h':'c39159'}, (
        '...kk...', '..kswk..', '.kswssk.', '..kssk..',
        '..bhk...', '.bhk....', 'bhk.....', '.k......')),
    'thermal': ({'k':'69443a','g':'cbb571','w':'f1dfac','r':'b13533','o':'f25b37'}, (
        '.gggg...', '.gwwg...', '..rro...', '..orr...',
        '..rro...', '..orr...', '..gwwg..', '..gggg..')),
    'pneumaticcraft': ({'k':'37434b','s':'93a8b0','w':'eef0d8','r':'cc4a46'}, (
        '..ssss..', '.swwwws.', 'swkwwkws', 'swwwrwws',
        'swwrwwws', '.swwwws.', '..ssss..', '...kk...')),
    'computercraft': ({'k':'263b35','s':'798c81','w':'b9c5b4','g':'77e998'}, (
        '.ssssss.', 'swkkkkws', 'skgkkkks', 'skkgkkks',
        'skgkggks', 'skkkkkks', 'swwwwwws', '.ssssss.')),
    'enderio': ({'k':'333344','p':'80668f','s':'bbc7c6','g':'42a997','h':'93ddae'}, (
        '...ss...', '..spks..', '.pkggkp.', 'skghhgks',
        'skghggks', '.pkggkp.', '..skps..', '...ss...')),
    'actually_additions': ({'k':'283c54','b':'3d82ba','c':'82cbd7','s':'bfc9c4','w':'ecf1db'}, (
        '......s.', '....ssws', '..bbcsw.', '.bcbbs..',
        '.bbbk...', '..kbk...', '.kbk....', '..k.....')),
    'railcraft': ({'k':'303940','s':'6d8390','w':'f3dd93','r':'b4443e','b':'a17743'}, (
        '..kk....', '..ksk...', '.ksssk..', '.kswsk..',
        '.ksssk..', 'krrrrrrk', '.kbkkbk.', '..k..k..')),
    'industrialcraft': ({'k':'28333a','y':'e3bf43','h':'fff099','s':'8f9b98'}, (
        '.ssssss.', 'syyyyyys', 'skkyykks', 'skkyykks',
        'syykkyys', 'sykkkkys', 'syyyyyys', '.ssssss.')),
    'buildcraft': ({'k':'6c4531','o':'c6873f','h':'f4c878','s':'7a8b91'}, (
        '...o..o.', '..oh..ho', '..ohhhho', '...ohho.',
        '..ksko..', '.ksk....', 'ksk.....', '.k......')),
    'projecte': ({'b':'4d1932','m':'aa2b5d','h':'dfc6ce','r':'da1834','o':'f27655','y':'ffe45f'}, (
        '..r.r...', '.rrbrr..', 'rbhmmbr.', 'rmmmmmbr',
        'ommmmmbb', 'ommmmbbo', '.ybbbby.', '..oyyo..')),
    'psi': ({'k':'43566b','s':'acb9bf','w':'e6ede4','c':'4bd6e2','b':'6493b0'}, (
        '.wwwww..', 'wscccsw.', '.sssswk.', '...kwk..',
        '..kwk...', '..kwk...', '..kk....', '........')),
    'galacticraft': ({'k':'405668','s':'a7bec7','w':'ecf0df','r':'bf4a43','b':'4e89a2','o':'f6b457'}, (
        '...rr...', '..rwwr..', '..swws..', '..swbs..',
        '..swws..', '.rswwsr.', '.rrssrr.', '...oo...')),
}


def _small(ident):
    palette, rows = _SMALL[ident]
    im = Image.new('RGBA', (8, 8), (0, 0, 0, 0))
    if len(rows) != 8 or any(len(row) != 8 for row in rows):
        raise ValueError('Invalid native icon plan: ' + ident)
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != '.':
                color = bytes.fromhex(palette[key])
                im.putpixel((x,y), (*color,255))
    return im


def render_theme(ident, size=16):
    """Return fresh original RGBA artwork at its native 8px or 16px size."""
    if ident not in ICON_IDS:
        raise KeyError(ident)
    if size == 8:
        return _small(ident)
    if size != 16:
        raise ValueError('Technology theme icons are authored at 8px and 16px')
    im = Image.new('RGBA', (16,16), (0,0,0,0))
    d = ImageDraw.Draw(im)
    if ident == 'create':
        # Wooden teeth around a pale, square andesite axle socket.
        teeth = ((6,0,9,3),(12,2,14,5),(12,6,15,9),(10,12,13,14),
                 (6,12,9,15),(1,10,3,13),(0,6,3,9),(2,1,5,3))
        for box in teeth: d.rectangle(box,fill='#775032')
        d.polygon(((5,2),(10,2),(13,5),(13,10),(10,13),(5,13),(2,10),(2,5)),fill='#bc8a4f')
        d.line((4,4,10,3),fill='#e5bd79',width=2)
        d.rectangle((5,5,10,10),fill='#445858');d.rectangle((6,6,9,9),fill='#acbab0')
        d.rectangle((7,7,8,8),fill='#586b67')
    elif ident == 'ae2':
        d.rectangle((1,1,14,14),fill='#7d8c98');d.rectangle((2,2,13,13),fill='#dbe2dc')
        d.rectangle((3,3,12,12),fill='#242e40')
        for box,c in (((5,4,8,5),'#f36d8d'),((9,4,11,6),'#dbc864'),((10,7,11,9),'#79d78a'),
                      ((8,10,11,11),'#6bd8e5'),((4,9,6,11),'#ac8ce6'),((4,5,5,8),'#c480e3')):
            d.rectangle(box,fill=c)
        for x,y in ((1,1),(12,1),(1,12),(12,12)):d.rectangle((x,y,x+2,y+2),fill='#ebf1e9')
        d.rectangle((6,6,9,9),fill='#394255')
    elif ident == 'refined_storage':
        # The official Advanced Processor is a raised cyan crystal on an
        # oblique gray printed substrate, rather than a storage-disk housing.
        d.polygon(((7,1),(10,3),(14,8),(10,14),(7,15),(2,9),(3,6)),fill='#596366')
        d.polygon(((7,2),(10,4),(13,8),(9,13),(7,14),(3,9),(4,6)),fill='#a0a8a5')
        d.line((4,6,7,3,10,5),fill='#e0e5de',width=2)
        d.line((4,9,7,12,10,10),fill='#d0d7d0')
        d.line((8,12,9,13),fill='#737e7c')
        d.polygon(((7,4),(10,6),(11,9),(8,11),(5,9),(4,7)),fill='#086b65')
        d.polygon(((7,3),(10,5),(11,7),(8,10),(5,8),(5,5)),fill='#0fc7ab')
        d.polygon(((7,4),(9,5),(9,7),(7,8),(6,6)),fill='#80f3dc')
        d.line((6,5,7,4),fill='#c7fff0')
        d.line((9,8,10,7),fill='#3bdcc4')
        d.point((7,13),fill='#e3e7df');d.point((11,9),fill='#6a7473')
    elif ident == 'mekanism':
        d.line((3,13,10,6),fill='#263f43',width=4);d.line((3,12,8,7),fill='#62b96a',width=2)
        d.polygon(((8,2),(12,1),(15,3),(13,5),(13,8),(10,9),(8,7),(9,5),(6,4)),fill='#5f4031')
        d.polygon(((9,2),(12,2),(14,3),(12,5),(12,7),(10,8),(9,6),(10,4),(8,3)),fill='#f28b2e')
        d.line((10,3,12,3),fill='#ffd987');d.line((4,11,6,9),fill='#a6e2a0')
        d.rectangle((1,13,3,15),fill='#365959')
    elif ident == 'immersive_engineering':
        d.line((2,14,10,5),fill='#403a34',width=4);d.line((3,13,9,6),fill='#aa7644',width=2)
        d.polygon(((5,3),(8,0),(14,5),(14,7),(11,10),(8,8),(6,8),(4,6)),fill='#2d3c46')
        d.polygon(((6,3),(8,1),(13,5),(11,8),(8,6),(6,7),(5,5)),fill='#96a9ad')
        d.line((7,3,11,6),fill='#e4e8d9',width=2)
        d.line((3,12,4,11),fill='#ddac6b')
    elif ident == 'thermal':
        d.polygon(((5,0),(10,0),(11,3),(9,5),(10,11),(12,12),(11,15),(6,15),(5,12),(7,10),(6,5),(4,3)),fill='#634938')
        d.rectangle((5,1,10,3),fill='#e2c47c');d.line((6,1,10,1),fill='#fff2bb')
        d.rectangle((6,12,11,14),fill='#d4b16c');d.line((7,13,10,13),fill='#f3dda1')
        for y in (4,7,10):
            d.line((6,y,9,y+1),fill='#e64d38',width=2)
            d.point((6,y),fill='#ff9960')
        d.line((9,5,7,7),fill='#922d36');d.line((9,8,7,10),fill='#922d36')
    elif ident == 'pneumaticcraft':
        d.rectangle((7,12,9,15),fill='#46525b')
        d.ellipse((1,0,14,13),fill='#45555c');d.ellipse((2,1,13,12),fill='#adbdba')
        d.ellipse((3,2,12,11),fill='#f3efd5')
        for x,y in ((5,3),(9,3),(3,6),(11,6)):d.point((x,y),fill='#49525a')
        d.line((7,7,10,4),fill='#c84442',width=2);d.rectangle((6,6,8,8),fill='#4b535a')
        d.line((5,11,10,11),fill='#e0dfc6')
    elif ident == 'computercraft':
        d.rectangle((1,1,14,14),fill='#53675e');d.rectangle((2,2,13,13),fill='#9daa98')
        d.rectangle((3,3,12,11),fill='#172d25')
        d.line((5,5,7,7,5,9),fill='#95f3a5');d.line((8,9,10,9),fill='#76cb88')
        d.line((3,2,12,2),fill='#c8d4bf');d.rectangle((3,13,10,13),fill='#6e8375')
        d.point((12,13),fill='#8ce091')
    elif ident == 'enderio':
        for box in ((6,0,9,3),(6,12,9,15),(0,6,3,9),(12,6,15,9)):
            d.rectangle(box,fill='#6e6b83')
        d.polygon(((5,2),(10,2),(13,5),(13,10),(10,13),(5,13),(2,10),(2,5)),fill='#3b394f')
        d.polygon(((5,3),(10,3),(12,5),(12,10),(10,12),(5,12),(3,10),(3,5)),fill='#8d729d')
        d.ellipse((4,4,11,11),fill='#275a62');d.ellipse((5,5,10,10),fill='#4aaa93')
        d.rectangle((6,5,8,6),fill='#a8e8b3');d.point((9,9),fill='#226468')
        for x,y in ((7,0),(7,14),(0,7),(14,7)):d.point((x,y),fill='#ced8cf')
    elif ident == 'actually_additions':
        d.line((4,13,7,9),fill='#294563',width=4);d.line((4,12,6,10),fill='#4587b9',width=2)
        d.polygon(((2,7),(5,4),(9,4),(11,6),(9,10),(5,11),(2,9)),fill='#264568')
        d.polygon(((3,7),(5,5),(8,5),(10,7),(8,9),(5,10),(3,8)),fill='#428cc0')
        d.line((4,6,7,6),fill='#94d7e6');d.line((5,9,8,8),fill='#295f91')
        d.polygon(((9,4),(13,0),(15,2),(11,6)),fill='#7d939f')
        d.line((10,4,14,1),fill='#eef2de',width=2);d.point((12,3),fill='#8299a7')
    elif ident == 'railcraft':
        d.rectangle((6,0,9,3),fill='#29343c');d.rectangle((5,0,10,1),fill='#546674')
        d.rectangle((3,4,12,11),fill='#354653');d.line((4,4,11,4),fill='#99adad')
        d.rectangle((5,3,10,4),fill='#667f88');d.rectangle((5,6,10,10),fill='#1f303e')
        d.rectangle((6,6,9,9),fill='#e5c279');d.rectangle((7,6,8,7),fill='#fff0b5')
        d.rectangle((1,11,14,13),fill='#a7443d');d.line((2,11,13,11),fill='#e47c62')
        for x in (3,10):d.rectangle((x,14,x+2,15),fill='#27353c')
        d.line((1,15,14,15),fill='#86989c')
    elif ident == 'industrialcraft':
        d.rectangle((1,1,14,14),fill='#637577');d.rectangle((2,2,13,13),fill='#d9be47')
        d.line((3,2,12,2),fill='#f6e58d')
        d.polygon(((5,3),(2,6),(2,8),(5,8),(6,6)),fill='#28323a')
        d.polygon(((10,3),(13,6),(13,8),(10,8),(9,6)),fill='#28323a')
        d.polygon(((6,10),(9,10),(11,13),(4,13)),fill='#28323a')
        d.rectangle((7,7,8,8),fill='#28323a')
    elif ident == 'buildcraft':
        d.line((3,14,10,7),fill='#37484d',width=4);d.line((3,13,9,7),fill='#71888b',width=2)
        d.polygon(((8,1),(10,0),(10,4),(12,5),(14,3),(14,0),(15,2),(15,6),(12,9),(8,8),(6,5)),fill='#70442b')
        d.polygon(((8,2),(9,1),(9,5),(12,6),(15,3),(14,6),(12,8),(8,7),(7,5)),fill='#e0a052')
        d.line((8,3,8,5),fill='#ffd287');d.point((3,13),fill='#adc6ba')
    elif ident == 'projecte':
        # Round magenta/burgundy stone and a broken hot rim are the canonical
        # silhouette. The former square golden gem bezel has been removed.
        for box,color in (((5,0,6,2),'#d51c39'),((10,0,10,2),'#8a1638'),
                          ((12,2,13,4),'#f52f37'),((1,4,2,7),'#e21b34'),
                          ((0,8,1,10),'#ff955c'),((13,7,15,8),'#ef4940'),
                          ((13,11,14,12),'#ffca56'),((2,12,3,14),'#ffc653'),
                          ((6,14,8,15),'#f8e962'),((11,13,12,14),'#f5785c')):
            d.rectangle(box,fill=color)
        d.ellipse((2,2,13,14),fill='#4e1833')
        d.ellipse((3,3,12,13),fill='#84244f')
        d.ellipse((3,3,11,11),fill='#b33168')
        d.polygon(((4,4),(8,3),(10,5),(6,7),(4,8)),fill='#c33f77')
        d.polygon(((8,8),(12,6),(12,10),(9,13),(5,12)),fill='#9c2858')
        d.rectangle((5,5,6,6),fill='#d5c1c9')
        d.point((4,4),fill='#e2658f');d.point((10,11),fill='#be3b6a')
        d.line((5,13,9,13),fill='#6f1c43')
    elif ident == 'psi':
        d.polygon(((1,2),(11,2),(14,4),(14,7),(10,7),(8,14),(4,14),(6,8),(3,7)),fill='#435368')
        d.polygon(((2,3),(11,3),(13,4),(13,6),(9,6),(7,8),(4,6),(2,6)),fill='#dce5df')
        d.rectangle((4,4,10,5),fill='#55d0df');d.line((4,4,8,4),fill='#a8f3ed')
        d.polygon(((7,7),(9,7),(7,13),(5,13)),fill='#a0b3bc')
        d.point((6,11),fill='#e9f1df');d.line((11,4,12,4),fill='#698cac')
    elif ident == 'galacticraft':
        d.polygon(((7,0),(8,0),(11,4),(11,11),(13,12),(13,15),(10,14),(5,14),(2,15),(2,12),(4,11),(4,4)),fill='#435b6b')
        d.polygon(((7,0),(8,0),(10,3),(5,3)),fill='#bb4b44')
        d.rectangle((5,4,10,12),fill='#d9e5d9');d.line((6,4,6,11),fill='#f3f2df')
        d.rectangle((7,5,9,8),fill='#3e7894');d.point((7,5),fill='#b0e2df')
        d.polygon(((4,10),(4,13),(2,14),(3,11)),fill='#be4745')
        d.polygon(((11,10),(12,11),(13,14),(11,13)),fill='#be4745')
        d.rectangle((6,13,9,14),fill='#7b8d90');d.rectangle((7,15,8,15),fill='#f7bd62')
    return im
