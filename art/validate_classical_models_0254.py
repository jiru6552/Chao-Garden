#!/usr/bin/env python3
"""Finite native Ionic geometry and the real Hero soffit UV joins, including mutations."""
from pathlib import Path
from copy import deepcopy
import argparse
import json
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
COLUMNS={'school_hall_column','school_twisted_column'}
PARTS=('base','shaft','capital')

def require(ok,message):
    if not ok:raise AssertionError(message)

def read(path):return json.loads(path.read_text())

def contains(elements,x,y,z):
    return any(all(e['from'][i]-1e-6<=q<=e['to'][i]+1e-6 for i,q in enumerate((x,y,z))) for e in elements)

def column_member(model,part):
    elements=model.get('elements',[])
    require(elements and all('rotation' not in e for e in elements),'Ionic mesh missing or has an unmatched rotated collision member')
    for e in elements:
        a,b=e['from'],e['to'];require(all(0<=a[i]<b[i]<=16 for i in range(3)),'Ionic member escapes its cell')
        require(set(e['faces'])=={'up','down','north','south','east','west'},'Solid Ionic member has a missing face')
        for face,data in e['faces'].items():
            u0,v0,u1,v1=data['uv'];w,h=(b[0]-a[0],b[1]-a[1]) if face in ('north','south') else (b[2]-a[2],b[1]-a[1]) if face in ('east','west') else (b[0]-a[0],b[2]-a[2])
            require(abs(abs(u1-u0)-w)<.00002 and abs(abs(v1-v0)-h)<.00002,'Ionic molding stretches texels')
    require(all(contains(elements,8,(i+.5)/16,8) for i in range(256)),'Ionic member has a vertical structural gap')
    if part=='shaft':
        require(all(min(e['from'][axis] for e in elements)>=4 and max(e['to'][axis] for e in elements)<=12 for axis in (0,2)),'Ionic shaft ribs overhang the molded base neck')
        for q in (5.87,7.91,9.95):require(contains(elements,q,8,4.18),'Ionic shaft lost a raised flute fillet')
        for q in (6.84,8.89):require(not contains(elements,q,8,4.18) and contains(elements,q,8,4.7),'Ionic flutes became painted marks on a square pier')
    if part=='base':
        require(contains(elements,2,1,2) and not contains(elements,2,4,2),'Ionic base lost its projecting molded plinth')
    if part=='capital':
        require(any(e['from']==[1,14,1] and e['to']==[15,16,15] for e in elements),'Ionic abacus no longer has its reduced ceiling-contact plate')
        # The complete scroll crowns must remain below the supporting plate.
        # The old plate started at12.5 and physically hid their upper0.8px.
        plates=[e for e in elements if e['from'][1]>=13.4]
        require(len(plates)==2 and min(e['from'][1] for e in plates)>=13.4,'Ionic plate clips the upper volute arc')
        for z in (3.05,12.95):
            require(not any(e['from'][1]<13.4 and e['to'][1]>13.3 and e['from'][2]<=z<=e['to'][2] for e in elements),'Ionic upper plate intersects the scroll crown')
        grooves=[e for e in elements if e['faces']['south']['texture']=='#groove']
        require(len(grooves)>=120,'Paired Ionic volutes lost their modeled spiral relief')
        for cx in (3,13):
            for z in (2.94,13.06):
                require(sum(abs((e['from'][0]+e['to'][0])/2-cx)<2.6 and e['from'][2]<=z<=e['to'][2] for e in grooves)>=35,'One viewing face has no complete paired Ionic scrolls')

def cornice_contract(assets=ASSETS,states=None,models=None):
    states=states or read(assets/'blockstates/hero_marble_trim.json')
    variants=states.get('variants',{})
    require(set(variants)=={f'corner={corner},facing={face}' for corner in ('false','true') for face in ('north','east','south','west')},'Hero cornice lost its finite straight/corner facing domain')
    for corner in (False,True):
        identifier='hero_marble_trim'+('_corner' if corner else '')
        m=(models or {}).get(identifier) or read(assets/f'models/block/{identifier}.json')
        require(m.get('parent')=='minecraft:block/block' and len(m.get('elements',[]))==1,'Hero cornice must remain a whole native cube')
        cube=m['elements'][0]
        require(cube['from']==[0,0,0] and cube['to']==[16,16,16] and set(cube['faces'])=={'north','south','east','west','up','down'},'Hero cornice lost a solid native cube face')
        down=cube['faces']['down']
        require(down=={'uv':[0,16,16,0],'texture':'#soffit'},'Hero soffit UV orientation or aspect changed')
        require(m['textures']['soffit']=='chao_garden:block/hero0253/'+identifier,'Hero corner binds straight soffit artwork')
        for face,turn in {'north':0,'east':90,'south':180,'west':270}.items():
            require(variants[f'corner={str(corner).lower()},facing={face}']=={'model':'chao_garden:block/'+identifier,**({'y':turn} if turn else {})},'Hero cornice state reverses its real 90-degree corner transform')
    straight=Image.open(assets/'textures/block/hero0253/hero_marble_trim.png').convert('RGBA')
    corner=Image.open(assets/'textures/block/hero0253/hero_marble_trim_corner.png').convert('RGBA')
    require(corner.size==straight.size==(64,64) and corner.getchannel('A').getextrema()==(255,255),'Hero mitred soffit texture is partial or transparent')
    # Native DOWN default maps +U/+V to +X/-Z. The actual reversed V bounds
    # above correct this to +X/+Z, so NW artwork meets E/S straight runs.
    for p in range(64):
        require(corner.getpixel((63,p))==straight.getpixel((0,p)),'East end of corner molding does not continue into the straight north cornice')
        require(corner.getpixel((p,63))==straight.getpixel((0,p)),'South end of corner molding does not continue into the straight west cornice')
    require(corner.getpixel((0,0))!=corner.getpixel((63,0)),'Corner reverted to an unturned stripe')
    return variants

def validate():
    for identifier in COLUMNS:
        states=read(ASSETS/f'blockstates/{identifier}.json')['variants']
        require(set(states)=={f'facing={face},part={part}' for face in ('north','south','east','west') for part in PARTS},'Ionic states escape reviewed finite member domain')
        for part in PARTS:column_member(read(ASSETS/f'models/block/{identifier}_{part}.json'),part)
        require(read(ASSETS/f'models/block/{identifier}.json')=={'parent':f'chao_garden:block/{identifier}_shaft'},'Existing reusable column item detached from its shaft')
    cornice_contract()

def self_test():
    broken=deepcopy(read(ASSETS/'models/block/school_hall_column_shaft.json'));broken['elements']=broken['elements'][:2]
    try:column_member(broken,'shaft')
    except AssertionError:pass
    else:raise AssertionError('Mutation accepted missing physical flutes')
    broken=deepcopy(read(ASSETS/'models/block/school_hall_column_capital.json'));broken['elements']=[e for e in broken['elements'] if e['faces']['south']['texture']!='#groove']
    try:column_member(broken,'capital')
    except AssertionError:pass
    else:raise AssertionError('Mutation accepted missing Ionic scrolls')
    broken=deepcopy(read(ASSETS/'models/block/school_hall_column_shaft.json'))
    broken['elements'][2]['from'][2]=3.5
    try:column_member(broken,'shaft')
    except AssertionError:pass
    else:raise AssertionError('Mutation accepted flute rib overhanging its molded base neck')
    broken=deepcopy(read(ASSETS/'models/block/school_hall_column_capital.json'))
    plate=next(e for e in broken['elements'] if e['from']==[3,13.4,3])
    plate['from'][1]=12.5
    for face in ('north','south','east','west'):plate['faces'][face]['uv'][3]=1.5
    try:column_member(broken,'capital')
    except AssertionError:pass
    else:raise AssertionError('Mutation accepted plate hiding upper volute crown')
    broken=deepcopy(read(ASSETS/'models/block/school_hall_column_capital.json'))
    neck=next(e for e in broken['elements'] if e['from']==[5.5,9,4])
    neck['to'][1]=12.5
    for face in ('north','south','east','west'):neck['faces'][face]['uv'][3]=3.5
    try:column_member(broken,'capital')
    except AssertionError:pass
    else:raise AssertionError('Mutation accepted upper molding floating above a short central neck')
    broken=deepcopy(read(ASSETS/'blockstates/hero_marble_trim.json'));broken['variants']['corner=true,facing=east']['y']=270
    try:cornice_contract(states=broken)
    except AssertionError:pass
    else:raise AssertionError('Mutation accepted a backwards Hero corner')
    broken=deepcopy(read(ASSETS/'models/block/hero_marble_trim_corner.json'));broken['elements'][0]['faces']['down']['uv']=[0,0,8,16]
    try:cornice_contract(models={'hero_marble_trim_corner':broken})
    except AssertionError:pass
    else:raise AssertionError('Mutation accepted a stretched Hero soffit')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--self-test',action='store_true');args=parser.parse_args()
    validate()
    if args.self_test:self_test()
    print('Classical models 0.25.4: 2 existing column IDs / 6 Ionic member meshes, physical flutes and scrolls, 8 oriented Hero soffit states; edge UV joins passed. Self-test='+str(args.self_test))
