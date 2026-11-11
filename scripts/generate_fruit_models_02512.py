#!/usr/bin/env python3
"""Final fruit writer: native OBJ geometry, rendered icons and matching crops.

No reference-image pixels are imported. Sprite pixels are a deterministic
orthographic render of the very same geometry used in held and dropped items.
"""
from pathlib import Path
import hashlib,json,sys
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'art'))
from fruit_forms02512 import *
A=ROOT/'src/main/resources/assets/chao_garden'

def write(path,data):
    p=A/path;p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(data,indent=2)+'\n' if isinstance(data,dict) else data)
def save(path,im):
    p=A/path;p.parent.mkdir(parents=True,exist_ok=True);im.save(p)

def obj(mesh,name):
    lines=['# Authored Chao Garden 0.25.12 mesh; model pixels / 16.',
           'mtllib chao_garden:models/fruit02512/materials.mtl']
    offset=1
    for ps,uvs,mat in mesh.faces:
        n=normal(ps)
        if sum(x*x for x in n)<.9:raise ValueError('Degenerate face '+name)
        lines+=['usemtl '+mat]
        for p in ps:lines.append('v '+' '.join(f'{v/16:.7f}' for v in p))
        for u,v in uvs:lines.append(f'vt {u:.7f} {1-v:.7f}')
        for _ in ps:lines.append('vn '+' '.join(f'{v:.7f}' for v in n))
        lines.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(offset,offset+len(ps))))
        offset+=len(ps)
    write(Path('models/fruit02512')/(name+'.obj'),'\n'.join(lines)+'\n')
    return {'faces':len(mesh.faces),'bounds':mesh.bounds(),'sha256':hashlib.sha256(('\n'.join(lines)+'\n').encode()).hexdigest()}

def bindings(skin):
    return {'fruit':skin,'particle':skin,**{k:'chao_garden:block/fruit02512/'+k for k in ('stem','leaf','leaf_light','stalk')}}
def native(name,skin,display=None):
    out={'loader':'neoforge:obj','model':'chao_garden:models/fruit02512/'+name+'.obj',
         'automatic_culling':False,'shade_quads':True,'flip_v':True,
         'textures':bindings(skin),'render_type':'minecraft:cutout'}
    if display:out['display']=display
    return out
DISPLAY={
 'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.55,.55,.55]},
 'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.55,.55,.55]},
 'firstperson_righthand':{'rotation':[0,-35,0],'translation':[-1,4,0],'scale':[.55,.55,.55]},
 'firstperson_lefthand':{'rotation':[0,35,0],'translation':[-1,4,0],'scale':[.55,.55,.55]},
 'ground':{'translation':[0,2,0],'scale':[.45,.45,.45]},
 'fixed':{'rotation':[0,180,0],'scale':[.7,.7,.7]}}

def item(ident,mesh,materials,skin):
    obj(mesh,ident)
    save(Path('textures/item')/(ident+'.png'),render(mesh,materials))
    display=DISPLAY
    if ident=='chao_watering_can':
        display=json.loads(json.dumps(DISPLAY))
        for hand in ('firstperson_righthand','firstperson_lefthand'):
            display[hand]['translation']=[-2.5,4,0]
            display[hand]['scale']=[.5,.5,.5]
    write(Path('models/item/fruit02512')/(ident+'.json'),native(ident,skin,display))
    write(Path('models/item')/(ident+'.json'),{
      'loader':'neoforge:separate_transforms','gui_light':'front',
      'textures':{'layer0':'chao_garden:item/'+ident,'particle':skin},
      'base':{'parent':'chao_garden:item/fruit02512/'+ident},
      'perspectives':{'gui':{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/'+ident},'gui_light':'front'}}})

def main():
    write(Path('models/fruit02512/materials.mtl'),'\n'.join(f'newmtl {k}\nKd 1.0 1.0 1.0\nmap_Kd #{k}\n' for k in ('fruit','stem','leaf','leaf_light','stalk')))
    for mat,im in material_images('garden_fruit').items():
        if mat!='fruit':save(Path('textures/block/fruit02512')/(mat+'.png'),im)
    rows=[]
    for name in FOODS:
        skin='chao_garden:block/fruit02512/'+name
        materials=material_images(name);save(Path('textures/block/fruit02512')/(name+'.png'),materials['fruit'])
        mesh=fruit(name);item(name,mesh,materials,skin)
        row={'id':name,'item':obj(mesh,name)}
        if name in SEEDS:
            sm=seed(name);si=name+'_seed';sMat=material_images(name,True)
            save(Path('textures/block/fruit02512')/(si+'.png'),sMat['fruit'])
            item(si,sm,sMat,skin+'_seed');row['seed']=obj(sm,si)
            row['pods']=[]
            for age,scale in enumerate((.45,.65,.82,1)):
                pod=fruit(name,False).transformed(scale,center=(8,10,8),offset=(0,-1,0))
                top=pod.bounds()[4];stemtop=22 if name=='dark_garden_fruit' else 20
                pod.box((7.5,top-.5,7.5),(8.5,stemtop,8.5))
                pn=name+'_pod_'+str(age);row['pods'].append(obj(pod,pn))
                save(Path('textures/block')/(pn+'.png'),texture(name,age=age))
                write(Path('models/block')/(pn+'.json'),native(pn,'chao_garden:block/'+pn))
            # The planted bulb retains its fruit identity through both stages.
            for stage,scale in enumerate((.65,.95)):
                sn=name+'_seedling_'+str(stage)
                planted=sm.transformed(scale,center=(8,2,8),offset=(0,-2,0));obj(planted,sn)
                write(Path('models/block')/(sn+'.json'),native(sn,skin+'_seed'))
            write(Path('blockstates')/(name+'_seedling.json'),{'variants':{f'stage={stage}':{'model':'chao_garden:block/'+name+'_seedling_'+str(stage)} for stage in (0,1)}})
        rows.append(row)
    # A blue toy watering can with an open handle, tapering spout and rose.
    can=Mesh();can.lathe([(2,3,2.7,0),(3,3.5,3,0),(8,3.5,3,0),(9,3,2.7,0)],10)
    can.box((6.2,8.7,7.5),(9.8,9.1,8.5),'stalk')
    can.box((3.5,4,7.3),(4.3,10.5,8.7),'fruit');can.box((3.5,10,7.3),(7,10.8,8.7),'fruit')
    can.box((3.5,3.8,7.3),(5.8,4.6,8.7),'fruit')
    # Sloped spout is an explicit four-sided pipe, no disallowed vanilla rotations.
    low=[(10,4.5,7.2),(10,5.7,7.2),(10,5.7,8.8),(10,4.5,8.8)]
    high=[(14.5,10,7.1),(14.5,11.2,7.1),(14.5,11.2,8.9),(14.5,10,8.9)]
    for i in range(4):can.face([low[i],high[i],high[(i+1)%4],low[(i+1)%4]])
    can.box((13.8,10.6,6.3),(15.2,11.6,9.7),'stalk')
    mats=material_images('garden_fruit');mats['fruit']=Image.new('RGBA',(32,32),(66,151,203,255))
    save(Path('textures/block/fruit02512/chao_watering_can.png'),mats['fruit'])
    item('chao_watering_can',can,mats,'chao_garden:block/fruit02512/chao_watering_can')
    locale=A/'lang/en_us.json';lang=json.loads(locale.read_text());lang['item.chao_garden.chao_watering_can']='Watering Can';locale.write_text(json.dumps(lang,indent=2,ensure_ascii=False,sort_keys=True)+'\n')
    report=ROOT/'docs/validation-0.25.12/fruit-mesh-manifest.json';report.parent.mkdir(parents=True,exist_ok=True)
    report.write_text(json.dumps({'release':'0.25.12','foods':len(FOODS),'seeds':len(SEEDS),'models':rows},indent=2)+'\n')
    print(f'Fruit meshes: {len(FOODS)} foods, {len(SEEDS)} sprouting seeds, four pod stages and two seedling stages per crop; watering can.')

if __name__=='__main__':main()
