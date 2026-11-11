#!/usr/bin/env python3
"""Adapt the supplied SA2 item meshes and materials to native Minecraft assets.

The curated source keeps original vertices, UVs, wrap modes and texture names.
UV repeats/mirroring are baked inside each material (Minecraft uses an atlas),
and the same adapted geometry produces GUI icons, held items and planted pods.
"""
from pathlib import Path
import json,math,sys,hashlib
from collections import Counter
from PIL import Image,ImageOps
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'art'))
from fruit_forms02512 import Mesh,render,FOODS,SEEDS
from generate_fruit_models_02512 import obj,native,DISPLAY,write,save,A
SOURCE=ROOT/'art/sa2_02514'
CATALOG=json.loads((SOURCE/'items.json').read_text())

def image_for(archive,name):
    im=Image.open(SOURCE/'textures'/archive/(name+'.png')).convert('RGBA')
    im.putalpha(im.getchannel('A').point(lambda a:255 if a>=128 else 0))
    return im
def wrap_atlas(im,limits,flags):
    lo=[math.floor(limits[a][0]) for a in range(2)]
    counts=[max(1,math.ceil(limits[a][1])-lo[a]) for a in range(2)]
    counts=[2**math.ceil(math.log2(n)) for n in counts]
    out=Image.new('RGBA',(im.width*counts[0],im.height*counts[1]))
    for y in range(counts[1]):
        for x in range(counts[0]):
            tile=im.copy();u=x+lo[0];v=y+lo[1]
            if flags&32 and u!=0:tile=tile.crop((0 if u<0 else im.width-1,0,1 if u<0 else im.width,im.height)).resize(im.size)
            elif flags&128 and u%2:tile=ImageOps.mirror(tile)
            if flags&16 and v!=0:tile=tile.crop((0,0 if v<0 else im.height-1,im.width,1 if v<0 else im.height)).resize(im.size)
            elif flags&64 and v%2:tile=ImageOps.flip(tile)
            out.paste(tile,(x*im.width,y*im.height))
    return out,lo,counts

def adapted(name):
    info=CATALOG[name];faces=info['faces'];materials={};bounds={};uv_bounds={}
    for f in faces:
        key='sa2_'+f['texture']+'_'+str(f['wrap_flags'])
        uv_bounds.setdefault(key,[[],[]])
        for uv in f['uv']:
            for a in range(2):uv_bounds[key][a].append(uv[a])
    for key,coords in uv_bounds.items():
        tex,flags=key[4:].rsplit('_',1)
        im=image_for(info['source_archive'],tex)
        # Both Mushroom item IDs are the same green in SA2; the rare variant is a gameplay distinction.
        atlas,lo,count=wrap_atlas(im,[(min(v),max(v)) for v in coords],int(flags))
        materials[key]=atlas;bounds[key]=(lo,count)
    def orient(p):
        x,y,z=p
        if name=='chao_shovel':return (x,-y,-z)
        if name=='chao_watering_can':return (z,y,-x) # spout points toward the hand's pour direction
        return (x,y,z)
    pts=[orient(p) for f in faces for p in f['points']]
    low=[min(p[a] for p in pts) for a in range(3)];high=[max(p[a] for p in pts) for a in range(3)]
    size=10 if name.endswith('_seed') else 13
    factor=size/max(high[a]-low[a] for a in range(3));floor=1 if name.endswith('_seed') else 2
    mesh=Mesh()
    for f in faces:
        key='sa2_'+f['texture']+'_'+str(f['wrap_flags']);lo,count=bounds[key]
        ps=[orient(p) for p in f['points']]
        ps=[((p[0]-(low[0]+high[0])/2)*factor+8,(p[1]-low[1])*factor+floor,(p[2]-(low[2]+high[2])/2)*factor+8) for p in ps]
        uv=[((p[0]-lo[0])/count[0],(p[1]-lo[1])/count[1]) for p in f['uv']]
        mesh.face(ps,key,uv)
        if f.get('double_sided'):mesh.face(list(reversed(ps)),key,list(reversed(uv)))
    if name.endswith('_seed'):
        # Thicken only the hairline sprout connector to remain legible at 32px.
        # Leaf outline, coat, bulb geometry and original UVs are retained.
        nonleaf=[p for ps,_,mat in mesh.faces if 'go_seed_ha_' not in mat for p in ps]
        top=max(p[1] for p in nonleaf)
        materials['sa2_sprout_0']=Image.new('RGBA',(16,16),(66,143,48,255))
        tip=mesh.bounds()[4]
        mesh.box((7.78,top-.5,7.78),(8.22,tip-.15,8.22),'sa2_sprout_0')
        for sign in (-1,1):
            ps=[(8,tip-.9,8),(8,tip-.5,8),(8+sign*1.9,tip+.05,8),(8+sign*1.9,tip-.35,8)]
            mesh.face(ps,'sa2_sprout_0');mesh.face(list(reversed(ps)),'sa2_sprout_0')
    return mesh,materials

def binding(name,materials,age=None):
    result={'stem':'chao_garden:block/fruit02512/stem'}
    for key,im in materials.items():
        if age is not None and age<3:
            green=Image.new('RGBA',im.size,(86,134,59,255));green.putalpha(im.getchannel('A'))
            im=Image.blend(green,im,(.16,.45,.72)[age])
        suffix=f'_{age}' if age is not None else ''
        path=f'block/sa2_02514/{name}/{key}{suffix}'
        save(Path('textures')/(path+'.png'),im);result[key]='chao_garden:'+path
    counts=Counter('sa2_'+f['texture']+'_'+str(f['wrap_flags']) for f in CATALOG[name]['faces']
                   if f['texture'] not in {'ali_miha','ali_miha02','ali_mikuki','go_seed_ha','alm_sgo04','ali_grape01','ali_mic2'})
    result['particle']=result[counts.most_common(1)[0][0]]
    return result

def model(mesh,name,textures,display=None):
    info=obj(mesh,name)
    p=A/'models/fruit02512'/f'{name}.obj';p.write_text(p.read_text().replace('models/fruit02512/materials.mtl','models/fruit02512/sa2_02514.mtl').replace('# Authored Chao Garden 0.25.12 mesh; model pixels / 16.','# SA2 source geometry adapted for Chao Garden 0.25.14; model pixels / 16.'))
    d=native(name,textures['particle'],display);d['textures']=textures
    return d,info

def main():
    mats={'stem'};rows=[]
    for name in CATALOG:
        mesh,materials=adapted(name);mats.update(materials);textures=binding(name,materials)
        display=json.loads(json.dumps(DISPLAY))
        if name=='chao_watering_can':
            for hand in ('firstperson_righthand','firstperson_lefthand'):display[hand]['translation']=[-2.5,4,0];display[hand]['scale']=[.5,.5,.5]
        d,info=model(mesh,name,textures,display)
        write(Path('models/item/fruit02512')/(name+'.json'),d)
        write(Path('models/item')/(name+'.json'),{'loader':'neoforge:separate_transforms','gui_light':'front','textures':{'layer0':'chao_garden:item/'+name,'particle':textures['particle']},'base':{'parent':'chao_garden:item/fruit02512/'+name},'perspectives':{'gui':{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/'+name},'gui_light':'front'}}})
        save(Path('textures/item')/(name+'.png'),render(mesh,materials))
        rows.append({'id':name,'source_object':CATALOG[name]['object_offset'],'source_archive':CATALOG[name]['source_archive'],'faces':len(mesh.faces),'bounds':mesh.bounds()})
        if name in SEEDS:
            for age,scale in enumerate((.45,.65,.82,1)):
                pod=mesh.transformed(scale,center=(8,10,8),offset=(0,-1,0));top=pod.bounds()[4]
                pod.box((7.5,top-.5,7.5),(8.5,22 if name=='dark_garden_fruit' else 20,8.5))
                pn=name+'_pod_'+str(age);d,_=model(pod,pn,binding(name,materials,age));write(Path('models/block')/(pn+'.json'),d)
                # Retain the compatibility material path, now matching this fruit.
                save(Path('textures/block')/(pn+'.png'),Image.open(A/'textures'/(d['textures']['particle'].split(':',1)[1]+'.png')).resize((32,32),Image.Resampling.NEAREST))
        if name.endswith('_seed'):
            fruit=name[:-5]
            for stage,scale in enumerate((.65,.95)):
                planted=mesh.transformed(scale,center=(8,1,8),offset=(0,-1,0));sn=fruit+'_seedling_'+str(stage)
                d,_=model(planted,sn,textures);write(Path('models/block')/(sn+'.json'),d)
    write(Path('models/fruit02512/sa2_02514.mtl'),'\n'.join(f'newmtl {k}\nKd 1.0 1.0 1.0\nmap_Kd #{k}\n' for k in sorted(mats)))
    # Placeable shovel: same toy shape and colors as the inventory/held tool.
    shovel,materials=adapted('chao_shovel');textures=binding('chao_shovel',materials)
    for stage in ['idle',*(f'play_{i}' for i in range(4))]:
        angle=math.radians(78 if stage=='idle' else (30,15,-8,15)[int(stage[-1])])
        m=Mesh()
        for pts,uv,mat in shovel.faces:
            m.face([(p[0],(p[1]-8)*math.cos(angle)-(p[2]-8)*math.sin(angle),(p[1]-8)*math.sin(angle)+(p[2]-8)*math.cos(angle)+8) for p in pts],mat,uv)
        m=m.transformed(offset=(0,-m.bounds()[1]+.1,0))
        d,_=model(m,'chao_shovel_'+stage,textures);write(Path('models/block')/('chao_shovel_'+stage+'.json'),d)
    # Existing garden solids retain their layouts, collision and item identities.
    scenes=json.loads((SOURCE/'scene-map.json').read_text())
    for path,entry in scenes.items():
        im=image_for(entry['archive'],entry['texture'])
        if entry.get('opaque',True):
            bg=Image.new('RGBA',im.size,tuple(entry.get('background',[231,226,203,255])));bg.alpha_composite(im);im=bg
        im=im.resize(tuple(entry.get('size',[64,64])),Image.Resampling.NEAREST)
        save(Path('textures')/(path+'.png'),im)
    # Turn the original frieze into a continuous quarter-round corner soffit.
    straight=Image.open(A/'textures/block/hero0253/hero_marble_trim.png').convert('RGBA')
    corner=Image.new('RGBA',(64,64),straight.getpixel((0,0)))
    for y in range(64):
        for x in range(64):
            radius=math.hypot(63-x,63-y)
            v=max(0,min(63,round(63-radius)))
            u=max(0,min(63,round(math.atan2(63-y,63-x)/math.pi*128)))
            corner.putpixel((x,y),straight.getpixel((u,v)))
    for p in range(64):
        corner.putpixel((63,p),straight.getpixel((0,p)))
        corner.putpixel((p,63),straight.getpixel((0,p)))
    save(Path('textures/block/hero0253/hero_marble_trim_corner.png'),corner)
    for name in ('garden_palm_trunk' ,'garden_palm_fruit_support'):
        path=A/'models/block'/(name+'.json')
        if not path.exists():continue
        model_data=json.loads(path.read_text());model_data['textures']['end']='chao_garden:scene/garden_palm_end'
        for element in model_data.get('elements',[]):
            for face in ('up','down'):
                if face in element['faces']:element['faces'][face]['texture']='#end'
        write(Path('models/block')/(name+'.json'),model_data)
    atlas=A.parent/'minecraft/atlases/blocks.json'
    data=json.loads(atlas.read_text());known={row.get('resource') for row in data['sources']}
    for resource in ('chao_garden:scene/garden_palm_end','chao_garden:scene/lobby_glass'):
        if resource not in known:data['sources'].append({'type':'minecraft:single','resource':resource})
    atlas.write_text(json.dumps(data,indent=2)+'\n')
    from lobby_material_manifest0255 import render_textures
    manifest_path=ROOT/'art/lobby_garden_manifest_025.json'
    manifest=json.loads(manifest_path.read_text())
    manifest['block_atlas_textures']=sorted(row['resource'].split(':',1)[1] for row in data['sources'])
    manifest['render_textures']=render_textures(ROOT,manifest['block_atlas_textures'])
    manifest_path.write_text(json.dumps(manifest,indent=2)+'\n')
    report={'release':'0.25.14','source_models':len(rows),'items':rows,'scene_textures':scenes}
    dest=ROOT/'docs/validation-0.25.14/source-assets.json';dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(report,indent=2)+'\n')
    print(f'SA2 source adaptation: {len(rows)} item models, matching pods/seedlings, five placed-shovel poses, {len(scenes)} garden materials.')
if __name__=='__main__':main()
