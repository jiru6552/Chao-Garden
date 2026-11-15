#!/usr/bin/env python3
"""Audit shipped head-cover geometry and UV solidity without starting Minecraft.

The geometry test samples actual resource faces. Its ray checks distinguish the
required eye ports from the surrounding solid shell panels.
"""
from pathlib import Path
import copy, json
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden'
HEAD=np.array([-3.5,-6,-3,3.5,0,3.])
checks=0

def check(ok,why):
    global checks
    checks+=1
    if not ok: raise AssertionError(why)

def triangles(bone):
    for mesh in bone.get('meshes',[]):
        v=np.array(mesh['vertices'])
        for face in mesh['faces']:
            uv=np.array(face['uv'])
            points=v[face['v']]
            for i in range(1,len(points)-1):
                yield points[[0,i,i+1]],uv[[0,i,i+1]]

def barycentric(point,triangle,axes):
    p=np.array(point)[list(axes)]
    a,b,c=triangle[:,axes]
    q=np.array([b-a,c-a]).T
    if abs(np.linalg.det(q))<1e-8:return None
    u,v=np.linalg.solve(q,p-a)
    if min(u,v,1-u-v)<-1e-7:return None
    return np.array([1-u-v,u,v])

def crossings(tris,point,axis,side,limit):
    axes=tuple(i for i in range(3) if i!=axis)
    for ps,uv in tris:
        b=barycentric(point,ps,axes)
        if b is None:continue
        at=float(b@ps[:,axis])
        if side*(at-limit)>1e-6:yield at,b@uv

def opacity(atlas,uv):
    return atlas.getpixel(tuple(max(0,min(atlas.size[i]-1,int(uv[i]))) for i in (0,1)))[3]

def validate(data,atlas,name):
    tris=list(triangles(data));check(bool(tris),name+' missing mesh')
    for ps,uv in tris:
        n=np.cross(ps[1]-ps[0],ps[2]-ps[0]);check(np.linalg.norm(n)>1e-7,name+' degenerate surface')
        check(sum(abs(v)>1e-7 for v in n)==1,name+' non-block panel')
        for i in range(7):
            for j in range(7-i):
                b=np.array([i/6,j/6,1-(i+j)/6]);p=b@ps;t=b@uv
                check(not ((p>HEAD[:3]+1e-5)&(p<HEAD[3:]-1e-5)).all(),name+' surface buried in unchanged skull')
                check(opacity(atlas,t)==255,name+' transparent cloth/panel texel')
    # Check contacts across all four cardinal views, beyond just min/max bounds.
    for axis in (0,2):
        other=2 if axis==0 else 0
        for side in (-1,1):
            for y in (-5.6,-4.5,-2,-.35):
                for t in (-2.9,-1.5,0,1.5,2.9):
                    p=np.zeros(3);p[1]=y;p[other]=t
                    eye=axis==2 and side==-1 and -.75>y>-3.25 and .8<abs(t)<3.2
                    hits=list(crossings(tris,p,axis,side,HEAD[axis+(3 if side>0 else 0)]))
                    check(bool(hits)!=eye,name+f' unexpected cardinal shell coverage axis={axis} side={side} y={y} cross={t}')
    for side in (-1,1):
        for x in np.linspace(.9,3.1,9)*side:
            for y in np.linspace(-3.1,-.9,9):
                check(not list(crossings(tris,(x,y,0),2,-1,-3.135)),name+' real iris is hidden by its eye port')
    # A neck opening must not acquire a solid flat underside through the wearer.
    for ps,uv in tris:
        if min(ps[:,1])>.05:
            check(barycentric((0,0,0),ps,(0,2)) is None,name+' cap seals neck at head center')

def validate_items(hats):
    from generate_sa2_assets_02515 import hat_item_mesh,fit,icon,normal
    for name in hats:
        source,mats=hat_item_mesh(name);expected=fit(source,(1,1,1,15,15,15))
        native=json.loads((A/'models/item/sa2_02515'/f'{name}.json').read_text())
        obj=A/native['model'].split(':',1)[1]
        points=[tuple(map(float,line.split()[1:])) for line in obj.read_text().splitlines() if line.startswith('v ')]
        intended=[p for ps,uv,k in expected.faces if sum(n*n for n in normal(ps))>=.9 for p in ps]
        check(len(points)==len(intended),name+' held/inventory topology differs from worn shell')
        check(all(abs(p[a]-q[a]/16)<=5.01e-8 for p,q in zip(points,intended) for a in range(3)),name+' stale held/inventory shape')
        check(set(mats)<=native['textures'].keys(),name+' unbound custom item source material')
        for mat in mats:
            texture=A/'textures'/(native['textures'][mat].split(':',1)[1]+'.png')
            with Image.open(texture) as actual:check(actual.convert('RGBA').tobytes()==mats[mat].tobytes(),name+' custom item source texture differs')
        with Image.open(A/'textures/item'/f'{name}.png') as actual:
            check(actual.convert('RGBA').tobytes()==icon(expected,name,mats).tobytes(),name+' icon differs from ported custom item')

def main():
    hats=json.loads((ROOT/'art/sa2_02515/hats.json').read_text())
    atlas=Image.open(A/'textures/entity/chao/market_accessories.png').convert('RGBA')
    signatures={};sample_bones={};files=sorted((A/'models/entity/hats').glob('*.json'))
    check(bool(files),'no hat rigs')
    for path in files:
        data=json.loads(path.read_text());by={b['name']:b for b in data['bones']}
        check(data.get('authored_metadata',{}).get('headwear_contract')=='canonical_skull079',path.name+' missing canonical contract')
        for name in hats:
            b=by[name];check(b['parent']=='skull' and b['pivot']==[0,0,0],path.name+'/'+name+' anchor changed')
            signature=json.dumps(b['meshes'],sort_keys=True)
            if name in signatures:check(signature==signatures[name],path.name+'/'+name+' donor-dependent shell')
            else:signatures[name]=signature;sample_bones[name]=b;validate(b,atlas,name)
    check(len(signatures)==12,'source hat identity count changed')
    validate_items(hats)
    # Deliberately broken in-memory controls prove the checks reject regressions.
    def rejected(name,broken):
        try:validate(broken,atlas,name)
        except AssertionError:return True
        return False
    buried=copy.deepcopy(sample_bones['apple_hat'])
    for point in buried['meshes'][0]['vertices'][:4]:point[2]=-2.9
    check(rejected('apple_hat',buried),'control: buried shell escaped')
    missing=copy.deepcopy(sample_bones['bucket_hat'])
    missing['meshes'][0]['faces'].pop(0)
    check(rejected('bucket_hat',missing),'control: missing front escaped')
    filled=copy.deepcopy(sample_bones['red_wool_beanie']);m=filled['meshes'][0];at=len(m['vertices'])
    m['vertices'].extend([[-3.2,-3.25,-3.3],[-3.2,-.75,-3.3],[-.8,-.75,-3.3],[-.8,-3.25,-3.3]])
    m['faces'].append({'v':[at,at+1,at+2,at+3],'uv':[m['faces'][0]['uv'][0]]*4})
    check(rejected('red_wool_beanie',filled),'control: blocked iris escaped')
    transparent=copy.deepcopy(sample_bones['blue_wool_beanie'])
    transparent['meshes'][0]['faces'][0]['uv']=[[100.5,100.5]]*4
    check(rejected('blue_wool_beanie',transparent),'control: unintended alpha escaped')
    report={'release':'0.25.79','result':'PASS','checks':checks,'source_hats':12,'source_rigs':len(files),'negative_controls':4,'matching_custom_item_models_and_icons':12,
            'actual_iris_samples':1944,'cardinal_shell_samples':960,
            'source_design':'Supplied SA2 ChaoMain head-cover shapes / al_body palettes, translated into cuboid shell panels; all twelve full covers have real iris ports, as requested; raw provider models remain unchanged.',
            'method':'Independent final-JSON surfaces, triangle UV raster samples, unchanged-head clearance, cardinal rays and all iris port samples; no game or build was started.'}
    print(json.dumps(report,indent=2))
    return report

if __name__=='__main__':main()
