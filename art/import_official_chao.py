#!/usr/bin/env python3
"""Import the owner-supplied Sonic Chao models, textures and animation source.

Input is an already readable resource pack. This tool never handles entitlement
files, options/account data, content manifests or decryption keys. The checked-in
art/imported_chao directory is the reproducible, asset-only authoring input.
"""
from pathlib import Path
import argparse, json, shutil, hashlib, math
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'art/imported_chao'
RES=ROOT/'src/main/resources/assets/chao_garden'
KEYS=('baby','swim','fly','run','power','hero','hero_fly','hero_power','dark','dark_run','dark_swim','golden','baghead','dad','dragon','ender','tails','knuckles','emerald','creeper')

def coords(v):return [-v[0],-v[1],v[2]]
def diff(a,b):return [a[i]-b[i] for i in range(3)]
def vec(x):return [round(float(v),7) for v in x]
def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]

def box(cube,bone_pivot,bone_mirror=False):
    o=cube['origin'];w,h,d=cube['size'];f=cube.get('inflate',0)
    x0,y0,z0=[n-f for n in o];x1,y1,z1=[o[i]+cube['size'][i]+f for i in range(3)]
    verts=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
    mirrored=cube.get('mirror',bone_mirror)
    if mirrored:
        verts=[[x0+x1-v[0],v[1],v[2]] for v in verts]
    pivot=cube.get('pivot',bone_pivot)
    vertices=[vec(diff(coords(v),coords(pivot))) for v in verts]
    u,v=cube['uv']
    # Bedrock box-UV layout, in logical model texture units. Faces are outward
    # wound in the source coordinate system before the two-axis conversion.
    specs=[([0,3,2,1],(u+d,v+d,u+d+w,v+d+h),'north'),
           ([5,6,7,4],(u+d+w+d,v+d,u+d+w+d+w,v+d+h),'south'),
           ([1,2,6,5],(u+d+w,v+d,u+d+w+d,v+d+h),'east'),
           ([4,7,3,0],(u,v+d,u+d,v+d+h),'west'),
           ([3,7,6,2],(u+d,v,u+d+w,v+d),'up'),
           ([0,1,5,4],(u+d+w,v,u+d+w+w,v+d),'down')]
    faces=[]
    for ids,(u0,v0,u1,v1),side in specs:
        a=diff(vertices[ids[1]],vertices[ids[0]]);b=diff(vertices[ids[2]],vertices[ids[0]])
        if sum(n*n for n in cross(a,b))<1e-12:continue
        # Match Blockbench's Bedrock box-UV import and THREE.BoxGeometry face
        # corners. The source X axis is reversed, so the wall U direction is
        # opposite an ordinary Java cube. Both poles have their own winding.
        if side=='up':uv=[[u0,v1],[u0,v0],[u1,v0],[u1,v1]]
        elif side=='down':uv=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]]
        else:uv=[[u0,v1],[u0,v0],[u1,v0],[u1,v1]]
        if mirrored:ids=list(reversed(ids));uv=list(reversed(uv))
        faces.append({'v':ids,'uv':[vec(p) for p in uv]})
    return {'name':'source_cuboid','material':'official','vertices':vertices,'faces':faces,'shading':'flat'}

def convert(source):
    g=source['minecraft:geometry'][0];bones=[];lookup={b['name']:b for b in g['bones']}
    for b in g['bones']:
        name=b['name'];parent=b.get('parent');p=b['pivot']
        pivot=coords(diff(p,lookup[parent]['pivot'])) if parent else [*coords(p)]
        if not parent:pivot[1]+=24
        br=b.get('rotation',[0,0,0]);rot=[br[0],-br[1],-br[2]]
        bones.append({'name':name,'parent':parent,'pivot':vec(pivot),'rotation':vec(rot),'meshes':[]})
        for i,c in enumerate(b.get('cubes',[])):
            cp=c.get('pivot',p);r=c.get('rotation',[0,0,0])
            bones.append({'name':name+'__cube_'+str(i),'parent':name,'pivot':vec(coords(diff(cp,p))),
                          'rotation':vec([r[0],-r[1],-r[2]]),'meshes':[box(c,p,b.get('mirror',False))]})
    skull=lookup.get('skull',{});sc=skull.get('cubes',[])
    core=max(range(len(sc)),key=lambda i:math.prod(sc[i]['size'])) if sc else -1
    decorations=['skull__cube_'+str(i) for i,c in enumerate(sc) if i!=core and not(c.get('inflate',0)>0 and c['size'][0]<=2 and c['size'][1]<=2 and c['size'][2]<=1)]
    decorations += [b['name'] for b in g['bones'] if b.get('parent')=='skull' and not b['name'].startswith('emotion')]
    return {'head_decorations':decorations,'format_version':1,'texture_width':g['description']['texture_width'],'texture_height':g['description']['texture_height'],
            'materials':{'official':[0,0,64,32]},'bones':bones,
            'source_coordinate_conversion':'x=-x,y=24-y,z=z; Blockbench-equivalent rotations=x,-y,-z; native Bedrock box-UV'}

def write_json(path,value):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,separators=(',',':'))+'\n')
def main():
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--source',type=Path);ap.add_argument('--skip-coats',action='store_true');args=ap.parse_args()
    if args.source:
        for key in KEYS:
            source=args.source/'models/entity'/('chao_'+key+('.json' if key=='dad' else '.geo.json'))
            dst=ART/'models'/source.name;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dst)
            dst=ART/'textures'/f'{key}.png';dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(args.source/'textures/entity/chao'/f'{key}.png',dst)
        dst=ART/'animations/chao.animation.json';dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(args.source/'animations/characters/chao/chao.animation.json',dst)
    manifest=[]
    for key in KEYS:
        path=ART/'models'/('chao_'+key+('.json' if key=='dad' else '.geo.json'));source=json.loads(path.read_text());data=convert(source)
        write_json(RES/'models/entity/official'/f'chao_{key}.json',data)
        tex=ART/'textures'/f'{key}.png';dst=RES/'textures/entity/chao/official'/f'{key}.png';dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(tex,dst)
        g=source['minecraft:geometry'][0];n=sum(len(b.get('cubes',[])) for b in g['bones'])
        assert len(data['bones'])==len(g['bones'])+n
        assert sum(len(b['meshes']) for b in data['bones'])==n
        assert all(all(math.isfinite(v) for pt in m['vertices'] for v in pt) for b in data['bones'] for m in b['meshes'])
        manifest.append({'id':key,'source_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'source_bones':len(g['bones']),'source_cuboids':n,'texture_size':list(Image.open(tex).size),'logical_uv':[data['texture_width'],data['texture_height']]})
    dst=RES/'animations/official/chao.animation.json';dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ART/'animations/chao.animation.json',dst)
    write_json(RES/'models/entity/official/manifest.json',{'format_version':1,'models':manifest,'bone_aliases':{'head':'skull','left_arm':'arm_left','right_arm':'arm_right','left_foot':'foot_left','right_foot':'foot_right'}})
    print('Imported 20 official Chao rigs/textures; preserved source cuboids, parent pivots, cube rotations, mirror flags, inflation and zero-thickness planes; copied eight animation clips.')
    if not args.skip_coats:generate_coats()

def uv_regions(cube):
    w,h,d=cube['size'];u,v=cube['uv']
    return ((u,v+d,u+d+w+d+w,v+d+h),(u+d,v,u+d+w+w,v+d))

def native_skin_masks(g,original):
    """Skin and eye masks in actual PNG pixels, independent of logical UV scale."""
    sx=original.width/g['description']['texture_width'];sy=original.height/g['description']['texture_height']
    mask=Image.new('1',original.size);draw=ImageDraw.Draw(mask)
    eye_mask=Image.new('1',original.size);eye_draw=ImageDraw.Draw(eye_mask)
    skull=next((b for b in g['bones'] if b['name']=='skull'),None)
    core=max(skull.get('cubes',[]),key=lambda c:math.prod(c['size'])) if skull else None
    primary=[]
    for bone in g['bones']:
        name=bone['name']
        if name.startswith('emotion') or 'wing' in name or name=='held_block':continue
        for cube in bone.get('cubes',[]):
            eye=(name=='skull' and cube['size']==[2,2,1] and cube.get('inflate',0)>0)
            pen=eye_draw if eye else draw
            for x0,y0,x1,y1 in uv_regions(cube):
                if x1>x0 and y1>y0:pen.rectangle((int(x0*sx),int(y0*sy),math.ceil(x1*sx)-1,math.ceil(y1*sy)-1),fill=1)
            if cube is core:
                u,v=cube['uv'];w,h,d=cube['size']
                # The skull's broad upper face is skin, never an eye patch.
                for y in range(math.ceil(v*sy),math.ceil((v+d)*sy)):
                    for x in range(math.ceil((u+d)*sx),math.ceil((u+d+w)*sx)):
                        if 0<=x<original.width and 0<=y<original.height:
                            rgba=original.getpixel((x,y))
                            if rgba[3]:primary.append(rgba[:3])
    from collections import Counter
    base=Counter(primary).most_common(1)[0][0] if primary else (138,239,255)
    return mask,eye_mask,base

def is_base_hue(rgb,base):
    import colorsys
    h,s,v=colorsys.rgb_to_hsv(*(c/255 for c in rgb));bh,bs,bv=colorsys.rgb_to_hsv(*(c/255 for c in base))
    if bs<.18:return s<.30 and v>.35
    if bv<.35:return sum((rgb[i]-base[i])**2 for i in range(3))<95**2
    hue=min(abs(h-bh),1-abs(h-bh))
    return hue<.085 and abs(s-bs)<.40

def recolor_native(g,original,target_rgb,two_tone,shiny=False,preserve_original=False,accent_rgb=None,invert_two_tone=False):
    """Repaint all skin consistently; preserve eyes, wing alpha and source marks.

    Two-tone means replacing the same base-hue regions on the torso AND limbs,
    rather than leaving the entire original arms and feet unchanged. Dark forms
    retain their charcoal background and replace their contrasting accent hue.
    Shininess is a material pass, so it does not need a second pale painted skin.
    """
    image=original.convert('RGBA').copy()
    if preserve_original:return image
    mask,eyes,base=native_skin_masks(g,image)
    base_lum=max(1,sum(base)/3)
    for y in range(image.height):
        for x in range(image.width):
            p=image.getpixel((x,y))
            if not p[3] or not mask.getpixel((x,y)) or eyes.getpixel((x,y)):continue
            main=is_base_hue(p[:3],base)
            if two_tone:
                selected=not main if invert_two_tone else main
                if not selected:
                    if accent_rgb is None:continue
                    target=accent_rgb
                else:target=target_rgb
            else:target=target_rgb
            # Restrained source shading: it preserves readable cast shadows
            # without carrying the original hue into a colored limb.
            factor=max(.60,min(1.12,(sum(p[:3])/3)/base_lum))
            image.putpixel((x,y),tuple(max(0,min(255,round(c*factor))) for c in target)+(p[3],))
    return image

def generate_coats():
    from prune_retired_resources import prune_retired_resources
    from chao_palettes import palette_for
    prune_retired_resources(RES.parents[1])
    atlas=RES/'textures/entity/chao'
    coats=[p for p in atlas.glob('*.png') if not p.stem.startswith(('variant_','chaos_'))
           and __import__('re').fullmatch(r'.+_(?:mono|two)(?:_shiny)?',p.stem)]
    canonical={"neutral":["baby","swim","fly","run","power"],"hero":["hero",None,"hero_fly",None,"hero_power"],"dark":["dark","dark_swim",None,"dark_run",None]}
    count=0
    for key in KEYS:
        g=json.loads((ART/'models'/('chao_'+key+('.json' if key=='dad' else '.geo.json'))).read_text())['minecraft:geometry'][0]
        original=Image.open(ART/'textures'/f'{key}.png').convert('RGBA')
        for coat in coats:
            name=coat.stem;normal=name.startswith('normal_');two='_two' in name and not name.startswith('jewel_')
            parts=name.split('_');alignment=parts[1] if normal else None;typ=int(parts[2]) if normal else 0
            preserved=normal and canonical[alignment][typ]==key
            target=Image.open(coat).convert('RGBA').getpixel((16,16))[:3]
            accent=None
            if normal and not preserved:
                roles=palette_for(alignment,typ);target=roles['head'];accent=roles['crest_tip'] if two else None
            image=recolor_native(g,original,target,two,name.endswith('_shiny'),preserved,accent,
                invert_two_tone=two and not normal and key.startswith('dark'))
            dst=RES/'textures/entity/chao/official/coats'/key/f'{name}.png';dst.parent.mkdir(parents=True,exist_ok=True);image.save(dst,optimize=True);count+=1
    print(f'Generated {count} native-UV coats; skin hues now extend consistently across arms and feet.')
if __name__=='__main__':main()
