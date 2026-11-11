#!/usr/bin/env python3
"""Native-UV theme coats for every ordinary animated donor skeleton."""
from pathlib import Path
import hashlib,json,math
from PIL import Image,ImageDraw
from variant_catalog import VARIANTS
from import_official_chao import native_skin_masks,is_base_hue,uv_regions

ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/chao_garden'
LAYOUTS=('baby','swim','fly','run','power','hero','hero_fly','hero_power','dark','dark_run','dark_swim')

def source(layout):
    geometry=json.loads((ROOT/'art/imported_chao/models'/('chao_'+layout+'.geo.json')).read_text())['minecraft:geometry'][0]
    image=Image.open(ROOT/'art/imported_chao/textures'/f'{layout}.png').convert('RGBA')
    return geometry,image

def wing_mask(geometry,image):
    result=Image.new('1',image.size);draw=ImageDraw.Draw(result)
    sx=image.width/geometry['description']['texture_width'];sy=image.height/geometry['description']['texture_height']
    for bone in geometry['bones']:
        if 'wing' not in bone['name']:continue
        for cube in bone.get('cubes',[]):
            for x0,y0,x1,y1 in uv_regions(cube):
                if x1>x0 and y1>y0:draw.rectangle((int(x0*sx),int(y0*sy),math.ceil(x1*sx)-1,math.ceil(y1*sy)-1),fill=1)
    return result

def coat(layout,palette):
    geometry,original=source(layout);image=original.copy()
    skin,eyes,base=native_skin_masks(geometry,original);wings=wing_mask(geometry,original)
    base_luminance=max(1,sum(base)/3)
    for y in range(image.height):
        for x in range(image.width):
            pixel=original.getpixel((x,y))
            if not pixel[3] or eyes.getpixel((x,y)):continue
            if skin.getpixel((x,y)):
                target=palette['head'] if is_base_hue(pixel[:3],base) else palette['accent']
                shade=max(.60,min(1.12,(sum(pixel[:3])/3)/base_luminance))
            elif wings.getpixel((x,y)):
                target=palette['wing'];shade=.65+.35*max(pixel[:3])/255
            else:continue
            image.putpixel((x,y),tuple(max(0,min(255,round(c*shade))) for c in target)+(pixel[3],))
    return image

def main():
    rows=[]
    for theme in VARIANTS:
        for layout in LAYOUTS:
            relative=f'textures/entity/chao/gameplay/variants/{theme["id"]}/{layout}.png'
            target=ASSET/relative;target.parent.mkdir(parents=True,exist_ok=True)
            if layout=='baby':
                # Preserve the approved baby image and Omochao inheritance source.
                target.write_bytes((ASSET/'textures/entity/chao/official'/f'variant_{theme["id"]}.png').read_bytes())
            else:coat(layout,theme['palette']).save(target)
            rows.append(dict(theme=theme['id'],layout=layout,file=relative,sha256=hashlib.sha256(target.read_bytes()).hexdigest()))
    manifest=dict(release='0.22.0',theme_count=len(VARIANTS),atlas_count=len(rows),native_uv_layouts=list(LAYOUTS),atlases=rows,
        rule='Original native skin/wing UV regions recolored to the fixed theme palette; eye texels and alpha remain unchanged. Existing baby coats are byte-identical.')
    (ASSET/'variant_native_atlases_022.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(f'Variants022: {len(VARIANTS)} themes × {len(LAYOUTS)} native UV layouts = {len(rows)} atlases; protected eyes and original baby coats.')

if __name__=='__main__':main()
