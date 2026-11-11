"""Small original pixel symbols for the themed coats and egg inventory icons."""
import math
from PIL import Image, ImageDraw


def glyph(icon, color, second):
    im=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
    c=tuple(color)+(255,);s=tuple(second)+(255,)
    if icon in ('gear','coil','circle','planet','atom','lens'):
        d.ellipse((3,3,12,12),outline=c,width=2)
        if icon=='gear':
            for box in ((6,1,9,4),(6,11,9,14),(1,6,4,9),(11,6,14,9)):d.rectangle(box,fill=c)
        elif icon=='atom':
            d.line((2,3,13,12),fill=s,width=2);d.line((2,12,13,3),fill=s,width=2)
        elif icon=='planet':d.line((1,10,14,5),fill=s,width=2)
        elif icon=='coil':
            for y in (5,8,11):d.line((1,y,14,y),fill=s)
        d.rectangle((6,6,9,9),fill=s)
    elif icon in ('star','sun','flower','firefly'):
        points=[]
        for i in range(10):
            a=-math.pi/2+i*math.pi/5;r=6 if i%2==0 else 2.5
            points.append((round(8+math.cos(a)*r),round(8+math.sin(a)*r)))
        d.polygon(points,fill=c);d.rectangle((6,6,9,9),fill=s)
        if icon=='flower':d.line((8,9,8,15),fill=s,width=2)
    elif icon in ('drop','flame','soul','dragon'):
        d.polygon([(8,1),(13,8),(12,12),(8,15),(3,12),(2,8)],fill=c)
        if icon!='drop':d.polygon([(8,6),(11,10),(8,13),(5,10)],fill=s)
        if icon=='dragon':d.line((3,2,5,6,1,8),fill=s,width=2)
    elif icon in ('crystal','hex','rune'):
        d.polygon([(8,1),(13,5),(13,11),(8,15),(3,11),(3,5)],outline=c,width=2)
        d.line((8,3,8,13),fill=s,width=2)
        if icon=='rune':d.line((4,5,11,9,5,12),fill=s,width=2)
        elif icon=='hex':d.line((3,7,12,10,9,3),fill=s,width=2)
    elif icon in ('leaf','sprout','feather'):
        d.polygon([(8,2),(3,3),(2,7),(6,9),(8,8),(10,9),(14,6),(13,2)],fill=c)
        d.line((8,5,8,15),fill=s,width=2)
        if icon=='feather':d.line((3,12,12,3),fill=s,width=2)
    elif icon in ('book','scroll'):
        d.rectangle((2,3,13,13),fill=c);d.line((8,3,8,13),fill=s)
        for y in (6,9):d.line((4,y,6,y),fill=s);d.line((10,y,12,y),fill=s)
    elif icon in ('disk','terminal','circuit','pipe','rail','energy','gauge'):
        d.rectangle((2,2,13,13),outline=c,width=2)
        if icon=='energy':d.polygon([(9,3),(4,9),(8,9),(6,13),(12,6),(8,6)],fill=s)
        elif icon=='gauge':d.line((5,11,8,8,11,5),fill=s,width=2)
        elif icon=='rail':
            d.line((5,3,5,13),fill=s,width=2);d.line((10,3,10,13),fill=s,width=2)
            for y in (5,8,11):d.line((3,y,12,y),fill=s)
        elif icon=='pipe':d.line((4,12,4,5,12,5),fill=s,width=3)
        elif icon=='circuit':
            d.rectangle((6,6,9,9),fill=s)
            for x,y in ((1,7),(12,7),(7,1),(7,12)):d.rectangle((x,y,x+2,y+2),fill=s)
        else:d.rectangle((5,4,10,8),fill=s);d.line((5,11,10,11),fill=s)
    elif icon=='mushroom':
        d.rectangle((7,8,9,14),fill=s);d.polygon([(1,8),(4,3),(11,3),(15,8)],fill=c)
        for x,y in ((5,5),(10,6)):d.rectangle((x,y,x+1,y+1),fill=s)
    elif icon=='cloud':
        d.rectangle((2,7,13,12),fill=c);d.rectangle((5,3,10,10),fill=c)
        d.line((4,13,11,13),fill=s)
    elif icon in ('eye','ball'):
        d.ellipse((1,4,14,12),fill=c);d.rectangle((6,6,9,10),fill=s)
        if icon=='ball':d.line((2,8,13,8),fill=s,width=2)
    elif icon=='moon':
        d.ellipse((2,1,13,14),fill=c);d.ellipse((7,1,15,10),fill=(0,0,0,0))
    elif icon=='sculk':
        for y,w in ((3,3),(6,5),(9,3),(12,2)):d.rectangle((8-w,y,8+w,y+1),fill=c)
        d.line((4,2,2,0),fill=s,width=2);d.line((11,2,13,0),fill=s,width=2)
    elif icon=='hammer':
        d.line((4,13,10,4),fill=s,width=3);d.polygon([(5,2),(9,1),(14,5),(11,9)],fill=c)
    elif icon in ('skull','fang'):
        d.rectangle((3,2,12,10),fill=c)
        d.rectangle((5,5,6,6),fill=(0,0,0,0));d.rectangle((10,5,11,6),fill=(0,0,0,0))
        for x in (4,7,10):d.rectangle((x,9,x+1,14 if icon=='fang' else 12),fill=s)
    elif icon=='stone':
        d.polygon([(4,2),(11,2),(14,7),(11,13),(4,13),(1,7)],fill=c)
        d.rectangle((6,5,9,10),fill=s)
    else:raise ValueError('Unknown authored icon '+icon)
    return im


def theme_icon(theme, size=16):
    """Original mod-specific artwork, authored separately for 8px and 16px use.

    Egg symbols use the native 8px design. Chest atlas patches use the richer
    16px design; neither path shrinks a detailed glyph into an unreadable mark.
    """
    from theme_icons_tech import ICON_IDS as TECH, render_theme as tech
    from theme_icons_magic import ICON_IDS as MAGIC, render_theme as magic
    from theme_icons_adventure import ICON_IDS as ADVENTURE, render_theme as adventure
    ident = theme['id'] if isinstance(theme, dict) else theme
    if size not in (8, 16):
        raise ValueError('Theme icons are authored at 8px and 16px')
    for identities, render in ((TECH, tech), (MAGIC, magic), (ADVENTURE, adventure)):
        if ident in identities:
            return render(ident, size)
    raise ValueError('Missing authored theme icon: ' + str(ident))
