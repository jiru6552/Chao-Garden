#!/usr/bin/env python3
"""Independently check native theme pixels, fitted geometry and exposed symbols."""
from pathlib import Path
from collections import Counter
import argparse,copy,hashlib,json,math
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden';M=A/'models/entity'
LAYOUTS=('baby','swim','fly','run','power','hero','hero_fly','hero_power','dark','dark_run','dark_swim')
DONORS={'baby':'baby','swim':'swim','fly':'fly','run':'run','power':'power','hero':'hero','hero_swim':'swim','hero_fly':'hero_fly','hero_run':'run','hero_power':'hero_power','dark':'dark','dark_swim':'dark_swim','dark_fly':'fly','dark_run':'dark_run','dark_power':'power'}
def require(ok,message):
    if not ok:raise AssertionError(message)
def load(path):return json.loads(path.read_text())
def raw(layout):
    g=load(ROOT/'art/imported_chao/models'/f'chao_{layout}.geo.json')['minecraft:geometry'][0]
    im=Image.open(ROOT/'art/imported_chao/textures'/f'{layout}.png').convert('RGBA')
    return g,im
def masks(g,im):
    skin=Image.new('1',im.size);eyes=skin.copy();wings=skin.copy()
    sx=im.width/g['description']['texture_width'];sy=im.height/g['description']['texture_height']
    for bone in g['bones']:
        name=bone['name']
        if name.startswith('emotion') or name=='held_block':continue
        for cube in bone.get('cubes',[]):
            eye=name=='skull' and cube['size']==[2,2,1] and cube.get('inflate',0)>0
            target=eyes if eye else wings if 'wing' in name else skin
            draw=ImageDraw.Draw(target);u,v=cube['uv'];w,h,d=cube['size']
            for x0,y0,x1,y1 in ((u,v+d,u+d+w+d+w,v+d+h),(u+d,v,u+d+w+w,v+d)):
                if x1>x0 and y1>y0:draw.rectangle((int(x0*sx),int(y0*sy),math.ceil(x1*sx)-1,math.ceil(y1*sy)-1),fill=1)
    return skin,eyes,wings

def palette_ray(pixel,target,low,high):
    """Intersect all channel rounding intervals, including saturated channels."""
    for channel,value in zip(target,pixel):
        if channel==0:
            if value!=0:return False
            continue
        low=max(low,(value-.500001)/channel)
        if value<255:high=min(high,(value+.500001)/channel)
    return low<=high

def identity_pixel(g,original,eyes):
    head=next(b for b in g['bones'] if b['name']=='skull')
    cube=max(head['cubes'],key=lambda c:math.prod(c['size']));u,v=cube['uv'];w,h,d=cube['size']
    sx=original.width/g['description']['texture_width'];sy=original.height/g['description']['texture_height']
    values=[(x,y,original.getpixel((x,y))) for y in range(math.ceil(v*sy),math.ceil((v+d)*sy)) for x in range(math.ceil((u+d)*sx),math.ceil((u+d+w)*sx)) if original.getpixel((x,y))[3]]
    color=Counter(p[:3] for x,y,p in values).most_common(1)[0][0]
    return next((x,y) for x,y,p in values if p[:3]==color and not eyes.getpixel((x,y)))

def pixel_contract(image,original,skin,eyes,wings,label,palette=None):
    require(image.size==original.size,label+' wrong native size')
    painted=protected=0;palette_cache={}
    for y in range(image.height):
        for x in range(image.width):
            p=image.getpixel((x,y));q=original.getpixel((x,y))
            require(p[3]==q[3],label+' source alpha changed')
            if eyes.getpixel((x,y)):
                require(p==q,label+' protected expression texel recolored');protected+=1
            elif not skin.getpixel((x,y)) and not wings.getpixel((x,y)):
                require(p==q,label+' unrelated UV region recolored')
            elif q[3]:
                painted+=p!=q
                if palette is not None:
                    kind='skin' if skin.getpixel((x,y)) else 'wing';cache=(kind,p[:3])
                    if cache not in palette_cache:
                        palette_cache[cache]=(palette_ray(p[:3],palette['head'],.60,1.12) or palette_ray(p[:3],palette['accent'],.60,1.12)) if kind=='skin' else palette_ray(p[:3],palette['wing'],.65,1)
                    require(palette_cache[cache],label+' pigment outside declared theme palette')
    require(painted>100,label+' missing native recoloring')
    return image.width*image.height,protected

def matrix(bones,name,stop):
    chain=[]
    while name!=stop:
        require(name is not None and name in bones,'Missing ancestor for '+str(stop));b=bones[name];chain.append(b);name=b.get('parent')
    out=np.eye(4)
    for b in chain[::-1]:
        t=np.eye(4);t[:3,3]=b['pivot'];out=out@t
        rx,ry,rz=map(math.radians,b.get('rotation',[0,0,0]))
        for axis,angle in ((2,rz),(1,ry),(0,rx)):
            c,s=math.cos(angle),math.sin(angle);r=np.eye(4)
            if axis==0:r[1:3,1:3]=[[c,-s],[s,c]]
            elif axis==1:r[:3,:3]=[[c,0,s],[0,1,0],[-s,0,c]]
            else:r[:2,:2]=[[c,-s],[s,c]]
            out=out@r
    return out
def vertices(mesh,target,age):
    base=np.asarray(mesh['vertices'],dtype=float)
    return base+(np.asarray(mesh['morph_vertices'][target])-base)*age if mesh.get('morph_vertices') else base
def bound(bones,name,anchor,target,age):
    b=bones[name];m=matrix(bones,name,anchor)
    vs=[np.c_[vertices(mesh,target,age),np.ones(len(mesh['vertices']))]@m.T for mesh in b.get('meshes',[])]
    if not vs:return None
    v=np.concatenate(vs)[:,:3];return np.r_[v.min(0),v.max(0)]
def envelope(data,anchor,target,age):
    bones={b['name']:b for b in data['bones']}
    candidates=[bound(bones,n,anchor,target,age) for n in bones if n.startswith(anchor+'__cube_')]
    candidates=[c for c in candidates if c is not None]
    require(bool(candidates),'No '+anchor+' skin core')
    core=max(candidates,key=lambda c:np.prod(c[3:]-c[:3]));out=core.copy()
    for name in data.get('attachment_extra_bones',{}).get(anchor,[]):
        b=bound(bones,name,anchor,target,age)
        if b is not None:out[:3]=np.minimum(out[:3],b[:3]);out[3:]=np.maximum(out[3:],b[3:])
    return out
def affine(actual,reference):
    scale=(actual[3:]-actual[:3])/(reference[3:]-reference[:3]);return actual[:3]-reference[:3]*scale,scale
def fit_points(points,fit):return np.asarray(points)*fit[1]+fit[0]
def visible_symbol(meshes,body,fit):
    opaque=[];symbols=[]
    for mesh in meshes:
        v=fit_points(mesh['vertices'],fit);low,high=v.min(0),v.max(0)
        if mesh['material']=='variant_detail' and len(v)==4 and high[2]-low[2]<.001:symbols.append(v)
        elif np.all(high-low>.01):opaque.append((low,high))
    exposed=0
    for v in symbols:
        for x in (.2,.5,.8):
            for y in (.2,.5,.8):
                p=v[0]+x*(v[3]-v[0])+y*(v[1]-v[0])
                if p[2]>=body[2]-.001:continue
                if any(low[0]+.001<p[0]<high[0]-.001 and low[1]+.001<p[1]<high[1]-.001 and low[2]<p[2]-.001 for low,high in opaque):continue
                exposed+=1
    require(exposed>0,'All theme symbol surfaces are swallowed by torso/armor')
    return exposed

def main(self_test=False):
    manifest=load(A/'variant_native_atlases_022.json');themes=load(A/'chao_variants.json')['variants']
    require(manifest['release']=='0.22.0' and manifest['atlas_count']==209 and manifest['theme_count']==19,'Wrong native atlas domain')
    require(tuple(manifest['native_uv_layouts'])==LAYOUTS,'Native donor layouts changed')
    require({(r['theme'],r['layout']) for r in manifest['atlases']}=={(t['id'],l) for t in themes for l in LAYOUTS},'Incomplete native atlas product')
    pixels=eye_pixels=cases=goggles=surfaces=0;by_id={t['id']:t for t in themes}
    for row in manifest['atlases']:
        label=row['theme']+'/'+row['layout'];path=A/row['file'];require(hashlib.sha256(path.read_bytes()).hexdigest()==row['sha256'],label+' stale atlas hash')
        image=Image.open(path).convert('RGBA');g,original=raw(row['layout']);skin,eyes,wings=masks(g,original);palette=by_id[row['theme']]['palette']
        anchor=identity_pixel(g,original,eyes)
        require(image.getpixel(anchor)[:3]==tuple(palette['head']),label+' wrong theme identity on source skull')
        if row['layout']=='baby':
            require(path.read_bytes()==(A/'textures/entity/chao/official'/f'variant_{row["theme"]}.png').read_bytes(),label+' approved baby art changed')
            # The historic baby palette painter also recolors unbound UV swatches.
            require(image.getchannel('A').tobytes()==original.getchannel('A').tobytes(),label+' alpha')
            for y in range(image.height):
                for x in range(image.width):
                    if eyes.getpixel((x,y)):require(image.getpixel((x,y))==original.getpixel((x,y)),label+' eye');eye_pixels+=bool(eyes.getpixel((x,y)))
            pixels+=image.width*image.height
        else:
            p,e=pixel_contract(image,original,skin,eyes,wings,label,palette);pixels+=p;eye_pixels+=e
    for theme in themes:
        ident=theme['id']
        for key,layout in DONORS.items():
            data=load(M/'gameplay'/f'chao_{key}.json');reference=load(M/'official'/f'chao_{layout}.json')
            overlay=load(M/'overlays'/f'chao_{layout}.json');bones={b['name']:b for b in overlay['bones']}
            head0=envelope(reference,'skull',4,0);body0=envelope(reference,'body',4,0)
            for target in range(9):
                for age in (0,.5,1):
                    head=envelope(data,'skull',target,age);body=envelope(data,'body',target,age)
                    hf=affine(head,head0);bf=affine(body,body0)
                    require(np.isfinite(np.r_[head,body,*hf,*bf]).all(),'Non-finite theme fit')
                    require(np.allclose(fit_points([body0[:3],body0[3:]],bf),[body[:3],body[3:]],atol=1e-5),'Torso affine misses current skin')
                    marker=bones[f'variant_{ident}_marker'];translation=np.array([(head[0]+head[3]-head0[0]-head0[3])/2,head[1]-head0[1],(head[2]+head[5]-head0[2]-head0[5])/2])
                    require(abs(marker['pivot'][1]+translation[1]-(head[1]-5))<1e-5,'Marker crown gap/size changed')
                    surfaces+=visible_symbol(bones[f'variant_{ident}_body']['meshes'],body,bf)
                    if ident in ('create','thaumcraft'):
                        lenses=[m for m in bones[f'variant_{ident}_head']['meshes'] if 'goggles_' in m['name'] and m['name'].endswith('_lens')]
                        require(len(lenses)==2,'Actual goggles require two separate lenses')
                        for lens in lenses:
                            p=fit_points(lens['vertices'],hf)
                            require(p[:,1].min()>head[1]-.05 and p[:,1].max()<head[1]+(head[4]-head[1])*.40,'Goggles do not rest on upper forehead')
                            require(p[:,2].min()<head[2]-.05,'Goggles buried in forehead')
                        goggles+=1
                    cases+=1
    rejected=[]
    if self_test:
        row=next(r for r in manifest['atlases'] if r['layout']=='hero');image=Image.open(A/row['file']).convert('RGBA');g,original=raw('hero');skin,eyes,wings=masks(g,original)
        palette=by_id[row['theme']]['palette']
        for label,selector,change in [('eye',eyes,lambda p:(1,2,3,p[3])),('alpha',skin,lambda p:(*p[:3],0)),('theme hue',skin,lambda p:(1,2,3,p[3]))]:
            broken=image.copy();point=next((x,y) for y in range(image.height) for x in range(image.width) if selector.getpixel((x,y)) and original.getpixel((x,y))[3] and (label=='eye' or not eyes.getpixel((x,y))));broken.putpixel(point,change(broken.getpixel(point)))
            try:pixel_contract(broken,original,skin,eyes,wings,label,palette)
            except AssertionError:rejected.append(label)
            else:raise AssertionError('Corrupt '+label+' accepted')
        require(len(rejected)==3,'Corruption checks incomplete')
    report=dict(release='0.22.0',result='PASS',atlases=209,pixel_comparisons=pixels,protected_eye_pixels=eye_pixels,type_secondary_age_fits=cases,forehead_goggles_cases=goggles,exposed_symbol_samples=surfaces,rejected_corruptions=rejected,
        scope='Native generated PNG pixels and static 15-family/9-endpoint/3-age fits. Actual moving ancestors/material selection are separately checked by the client probe.')
    path=ROOT/'docs/research/VARIANTS_022_CHECKS.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--self-test',action='store_true');main(parser.parse_args().self_test)
