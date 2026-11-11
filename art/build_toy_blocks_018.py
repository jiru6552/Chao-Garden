#!/usr/bin/env python3
"""Deterministic native Minecraft block models for eight placeable Chao toys."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
DATA=ROOT/'src/main/resources/data/chao_garden'
COLORS={'wood':'#986842','brass':'#e7b74f','brass_dark':'#a36c28','red':'#b84855',
        'blue':'#4b7fbb','cyan':'#72cbd4','yellow':'#e5d879','white':'#e3d5b6',
        'black':'#252e38','gray':'#75818b','metal':'#b4bdc1','green':'#5f976b',
        'pink':'#d68daa','purple':'#8a6f9e','skin':'#b7a47e','rubber':'#3d4650'}
IDS=('chao_trumpet','chao_drum','chao_jack_in_box','chao_radio','chao_tv',
     'chao_ball','chao_rattle','chao_maracas')

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n')

def tile(name):
    i=list(COLORS).index(name);x=(i%4)*4;y=(i//4)*4
    return [x,y,x+4,y+4]

def cube(bounds,material,rotation=None,screen=None):
    faces={side:{'texture':'#palette','uv':tile(material)}
           for side in ('north','south','east','west','up','down')}
    if screen:faces['north']={'texture':'#screen','uv':[0,0,16,16]}
    element={'from':list(bounds[:3]),'to':list(bounds[3:]),'faces':faces}
    if rotation:element['rotation']=rotation
    return element

def model(toy,active=False,frame=0):
    parts=[]
    def add(bounds,mat,**kw):parts.append(cube(bounds,mat,**kw))
    bounce=(0,.8,1.5,.8)[frame] if active else 0
    screen='chao_garden:block/toy_'+('tv' if toy=='chao_tv' else 'radio')+'_screen'+('' if active else '_off')
    if toy=='chao_trumpet':
        add((1,3,7,3,5,9),'brass');add((3,3.5,7.5,12,4.5,8.5),'brass')
        add((4,1.5,7.5,10.5,2.5,8.5),'brass');add((3.5,2,7.5,4.5,4,8.5),'brass')
        add((10,2,7.5,11,4,8.5),'brass')
        for x in (6,8,10):
            add((x-.4,4,7.6,x+.4,6.3-(.5 if active and (x//2+frame)%2 else 0),8.4),'brass_dark')
            add((x-.7,6.1-(.5 if active and (x//2+frame)%2 else 0),7.3,x+.7,6.6-(.5 if active and (x//2+frame)%2 else 0),8.7),'brass')
        add((11,3,6.5,12,5,9.5),'brass');add((12,2,5.5,13,6,10.5),'brass')
        add((13,1,4.5,14.3,2.3,11.5),'brass');add((13,5.7,4.5,14.3,7,11.5),'brass')
        add((13,2.3,4.5,14.3,5.7,5.8),'brass');add((13,2.3,10.2,14.3,5.7,11.5),'brass')
        add((12.9,2.3,5.8,13.1,5.7,10.2),'black')
    elif toy=='chao_drum':
        # Disjoint strips form a stepped cylinder without overlapping top faces.
        for x0,z0,x1,z1 in ((4,3,12,13),(3,4,4,12),(12,4,13,12)):
            add((x0,.8,z0,x1,7.3,z1),'red')
            add((x0,0,z0,x1,.8,z1),'brass')
            add((x0,7.3,z0,x1,8.3,z1),'brass')
            add((x0,8.3,z0,x1,8.7,z1),'white')
        for x,z in ((3.1,5),(3.1,10),(12.3,5),(12.3,10),(5,3.1),(10,3.1),(5,12.3),(10,12.3)):
            add((x,1,z,x+.6,7.2,z+.6),'metal')
        for i in (0,1):
            h=8.9+(2 if active and (frame+i)%2 else 0)
            add((5+i*4,h,4,5.7+i*4,h+.6,12),'wood',rotation={'origin':[8,h,8],'axis':'y','angle':45 if i else -45})
    elif toy=='chao_jack_in_box':
        add((4,.8,4,12,8,12),'red');add((3.8,0,3.8,12.2,.8,12.2),'brass')
        for x in (5,8,11):add((x,1,3.9,x+.6,7,4.1),'yellow')
        add((12,3,7,13,4,8),'metal');add((12.8,3,7,13.4,5.3,7.6),'metal');add((13,5,7,14,5.7,7.7),'wood')
        if active and frame in (1,2,3):
            # Author the raised lid directly so it cannot intersect the spring
            # character at an intermediate element rotation.
            add((4,8,11.5,12,16,12.5),'brass')
            top=15 if frame==2 else 12
            for y in range(8,top-3):add((6.5+(y%2),y,7,8.5+(y%2),y+.5,9),'metal')
            add((6,top-3,6,10,top,10),'cyan');add((7,top,7,9,top+.7,9),'yellow')
            add((6.8,top-2,5.9,7.4,top-1,6.1),'black');add((8.6,top-2,5.9,9.2,top-1,6.1),'black')
        else:add((3.8,8,3.8,12.2,9,12.2),'brass')
    elif toy=='chao_radio':
        add((2,0,4,14,8,12),'wood');add((3,1,3.8,8.2,7,4.1),'black')
        for y in (2,3.5,5,6.5):add((3.5,y,3.6,7.7,y+.3,3.9),'gray')
        add((9,4.8,3.75,12.5,6.7,4),'black',screen=True)
        for x in (9.5,12):add((x,1.7,3.5,x+1,2.7,4),'metal')
        add((5,8,7,6,10,9),'rubber');add((10,8,7,11,10,9),'rubber');add((5,10,7,11,11,9),'rubber')
        add((12.3,8,10,12.7,14,10.4),'metal',rotation={'origin':[12.5,8,10.2],'axis':'z','angle':-22.5})
    elif toy=='chao_tv':
        add((2,1,4,14,11,12),'gray');add((3,2,3.7,12.2,10,4.1),'black')
        add((3.7,2.8,3.5,11.5,9.3,3.8),'black',screen=True)
        add((12.6,3,3.5,13.5,4,4),'metal');add((12.6,7,3.5,13.5,8,4),'metal')
        for x in (3,11):add((x,0,6,x+2,1,10),'rubber')
        add((7,11,7,9,12,9),'rubber')
        for angle in (-22.5,22.5):add((7.8,11.5,7.8,8.2,15.5,8.2),'metal',rotation={'origin':[8,11.5,8],'axis':'z','angle':angle})
    elif toy=='chao_ball':
        shift=(0,.7,0,-.7)[frame] if active else 0
        add((5+shift,bounce,4,11+shift,8+bounce,12),'yellow')
        add((4+shift,1+bounce,5,12+shift,7+bounce,11),'blue')
        add((5+shift,1+bounce,3.9,11+shift,7+bounce,4.2),'red')
        add((5+shift,1+bounce,11.8,11+shift,7+bounce,12.1),'red')
    elif toy=='chao_rattle':
        add((7.3,.5,6,8.7,1.7,13),'wood');add((6,1+bounce,3,10,4+bounce,7),'yellow')
        add((6.8,1.8+bounce,2.8,9.2,3.2+bounce,3.1),'blue')
    else:
        for i,x in enumerate((4,10)):
            h=bounce if i==frame%2 else 0
            add((x+1,.4,7,x+2,1.5,13),'wood');add((x,1+h,3,x+3,4+h,7),'red' if i else 'green')
            add((x,2+h,2.9,x+3,2.7+h,3.1),'yellow')
    return {'parent':'minecraft:block/block','ambientocclusion':False,
            'textures':{'palette':'chao_garden:block/toy_palette','particle':'chao_garden:block/toy_palette','screen':screen},
            'elements':parts}

def textures():
    out=ASSETS/'textures/block';out.mkdir(parents=True,exist_ok=True)
    image=Image.new('RGB',(32,32));draw=ImageDraw.Draw(image)
    for i,(name,color) in enumerate(COLORS.items()):
        x=i%4*8;y=i//4*8;draw.rectangle((x,y,x+7,y+7),fill=color)
        base=tuple(int(color[j:j+2],16) for j in (1,3,5));light=tuple(min(255,c+10) for c in base);dark=tuple(max(0,c-10) for c in base)
        if name in ('wood','brass_dark'):draw.line((x,y+2,x+7,y+2),fill=dark);draw.line((x+1,y+6,x+6,y+6),fill=light)
        else:draw.line((x,y,x+7,y),fill=light);draw.line((x,y+7,x+7,y+7),fill=dark)
    image.save(out/'toy_palette.png')
    for kind in ('tv','radio'):
        image=Image.new('RGB',(16,64));draw=ImageDraw.Draw(image)
        for frame in range(4):
            y=frame*16;draw.rectangle((0,y,15,y+15),fill='#17232d')
            if kind=='tv':
                draw.rectangle((1,y+1,14,y+9),fill='#719eb8');draw.rectangle((1,y+10,14,y+14),fill='#60835c')
                draw.rectangle((11,y+2,12,y+3),fill='#e7d987');x=5+(frame%3)-1
                draw.rectangle((x,y+5,x+5,y+9),fill='#82d0da');draw.rectangle((x+1,y+10,x+4,y+12),fill='#82d0da')
                draw.point((x+1,y+7),fill='#263440');draw.point((x+4,y+7),fill='#263440')
                draw.rectangle((x+1,y+13,x+2,y+13),fill='#e5d879');draw.rectangle((x+4,y+12+(frame%2),x+5,y+12+(frame%2)),fill='#e5d879')
            else:
                for bar in range(5):
                    h=2+((bar*3+frame*2)%9);draw.rectangle((1+bar*3,y+14-h,2+bar*3,y+14),fill='#9ab675' if h<7 else '#d9c77d')
        path=out/f'toy_{kind}_screen.png';image.save(path)
        write(path.with_suffix('.png.mcmeta'),{'animation':{'frametime':6,'width':16,'height':16,'interpolate':False}})
        Image.new('RGB',(16,16),'#22323d').save(out/f'toy_{kind}_screen_off.png')

def main():
    textures()
    for ident in IDS:
        variants={}
        write(ASSETS/'models/block'/f'{ident}_idle.json',model(ident))
        for frame in range(4):write(ASSETS/'models/block'/f'{ident}_play_{frame}.json',model(ident,True,frame))
        for facing,angle in (('north',0),('east',90),('south',180),('west',270)):
            for active in (False,True):
                for frame in range(4):variants[f'facing={facing},frame={frame},playing={str(active).lower()}']={
                    'model':f'chao_garden:block/{ident}_play_{frame}' if active else f'chao_garden:block/{ident}_idle','y':angle}
        write(ASSETS/'blockstates'/f'{ident}.json',{'variants':variants})
        write(ASSETS/'models/item'/f'{ident}.json',{'parent':f'chao_garden:block/{ident}_idle'})
        write(DATA/'loot_table/blocks'/f'{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,
            'entries':[{'type':'minecraft:item','name':f'chao_garden:{ident}'}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    lang=ASSETS/'lang/en_us.json';entries=json.loads(lang.read_text())
    for ident in IDS:entries['block.chao_garden.'+ident]=entries.get('item.chao_garden.'+ident,ident.replace('_',' ').title())
    write(lang,entries)
    for ident in IDS:
        for active in (False,True):
            for frame in range(4):
                data=model(ident,active,frame)
                for element in data['elements']:
                    assert all(a<b for a,b in zip(element['from'],element['to'])),(ident,element)
                    if 'rotation' in element:assert element['rotation']['angle'] in (-45,-22.5,0,22.5,45)
    print('Eight physical toys; 40 block models; 256 oriented states; eight single-item loot tables; native palette and animated TV/radio screens.')

if __name__=='__main__':main()
