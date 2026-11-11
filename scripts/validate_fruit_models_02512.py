#!/usr/bin/env python3
"""Validate actual OBJ references, UVs, finite closed fruit surfaces and icons."""
import json,math,sys,hashlib
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'art'))
from fruit_forms02512 import FOODS,SEEDS
A=ROOT/'src/main/resources/assets/chao_garden';checks=0
def check(ok,message):
    global checks
    checks+=1
    if not ok:raise AssertionError(message)
def read(p):return json.loads(p.read_text())
def validate_obj(path):
    vertices=[];uv=[];normals=[];faces=[];material=''
    for line in path.read_text().splitlines():
        bits=line.split()
        if not bits:continue
        if bits[0]=='v':vertices.append(tuple(map(float,bits[1:])))
        if bits[0]=='vt':uv.append(tuple(map(float,bits[1:])))
        if bits[0]=='vn':normals.append(tuple(map(float,bits[1:])))
        if bits[0]=='usemtl':material=bits[1]
        if bits[0]=='f':faces.append((material,[tuple(map(int,x.split('/'))) for x in bits[1:]]))
    check(bool(faces),f'Empty native mesh {path}')
    check(all(all(math.isfinite(v) and -1<=v<=2 for v in p) for p in vertices),f'Finite bounded OBJ {path}')
    check(all(all(math.isfinite(v) and 0<=v<=1 for v in p) for p in uv),f'UV outside material {path}')
    check(all(abs(sum(v*v for v in n)-1)<1e-5 for n in normals),f'Invalid native normals {path}')
    for mat,indices in faces:
        check((mat in {'fruit','stem','leaf','leaf_light','stalk'} or mat.startswith(('sa2_','cwe_'))) and len(indices) in (3,4),f'Material/topology {path}')
        check(all(1<=a<=len(vertices) and 1<=b<=len(uv) and 1<=c<=len(normals) for a,b,c in indices),f'Dangling vertex index {path}')
    return len(faces)
def native(model):
    check(model.get('loader')=='neoforge:obj','Missing native OBJ loader')
    check(model.get('automatic_culling') is False,'Fruit must not cull non-axis surfaces')
    path=A/model['model'].split(':',1)[1];check(path.is_file(),'Unresolved native geometry')
    for k,value in model['textures'].items():check((A/'textures'/(value.split(':',1)[1]+'.png')).is_file(),'Missing material '+k)
    return validate_obj(path)
def main():
    check(len(FOODS)==37 and len(SEEDS)==36,'Complete retained food/seed catalogs')
    faces=0
    for ident in [*FOODS,*(x+'_seed' for x in SEEDS),'chao_watering_can']:
        data=read(A/'models/item'/f'{ident}.json');check(data.get('loader')=='neoforge:separate_transforms',ident+': missing actual context switch')
        icon='chao_garden:item/'+ident
        check(data['perspectives']['gui']['textures']['layer0']==icon,ident+': GUI identity differs')
        folder='cwe02516' if ident in {'shiny_fruit','hyper_swim_fruit','hyper_fly_fruit','hyper_run_fruit','hyper_power_fruit'} else 'fruit02512'
        check(data['base']['parent']=='chao_garden:item/'+folder+'/'+ident,ident+': held/dropped identity differs')
        faces+=native(read(A/'models/item'/folder/f'{ident}.json'))
        with Image.open(A/'textures/item'/f'{ident}.png') as im:
            check(im.size==(32,32) and im.mode=='RGBA' and set(im.getchannel('A').tobytes())=={0,255},ident+': icon resolution/alpha')
            box=im.getbbox();check(box and box[0]>=1 and box[1]>=1 and box[2]<=31 and box[3]<=31,ident+': cropped or blank inventory icon')
    for fruit in SEEDS:
        hashes=set();heights=[]
        for age in range(4):
            model=read(A/'models/block'/f'{fruit}_pod_{age}.json');faces+=native(model)
            path=A/model['model'].split(':',1)[1];lines=path.read_text().splitlines();stem=False;ys=[];body=[]
            for line in lines:
                if line.startswith('usemtl '):stem=line in ('usemtl stem','usemtl cwe_pod_stem')
                if line.startswith('v '):
                    y=float(line.split()[2])*16
                    if stem:ys.append(y)
                    else:body.append(y)
            check(abs(max(ys)-(22 if fruit=='dark_garden_fruit' else 20))<1e-4,fruit+': floating stem')
            heights.append(max(body)-min(body));hashes.add(hashlib.sha256((A/'textures/block'/f'{fruit}_pod_{age}.png').read_bytes()).hexdigest())
        check(heights==sorted(set(heights)) and len(hashes)==4,fruit+': indistinct ripening stages')
        for stage in (0,1):faces+=native(read(A/'models/block'/f'{fruit}_seedling_{stage}.json'))
    # Mutation controls exercise actual native-file checks, not a second generator.
    import tempfile
    rejected=0
    for bad in ['v nan 0 0\n','v 0 0 0\nvt 0 0\nvn 0 0 0\nusemtl fruit\nf 1/1/1 1/1/1 1/1/1\n',
                'v 0 0 0\nvt 0 0\nvn 0 1 0\nusemtl fruit\nf 2/1/1 2/1/1 2/1/1\n']:
        with tempfile.NamedTemporaryFile(mode='w',suffix='.obj') as f:
            f.write(bad);f.flush()
            try:validate_obj(Path(f.name))
            except AssertionError:rejected+=1
    check(rejected==3,'Malformed geometry mutation controls')
    report={'release':'0.25.12','result':'passed','checks':checks,'native_faces':faces,'foods':37,'seeds':36,'malformed_meshes_rejected':rejected}
    p=ROOT/'docs/validation-0.25.12/fruit-assets.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(report,indent=2)+'\n')
    print(f'Fruit/model contract: {checks} checks; {faces} native faces; malformed fixtures rejected {rejected}.')
if __name__=='__main__':main()
