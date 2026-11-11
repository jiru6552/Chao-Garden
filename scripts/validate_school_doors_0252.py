#!/usr/bin/env python3
"""Validate the actual native slice files with Minecraft FaceInfo vertex ordering.

This checks material orientation in physical space independently of the generator's
face-renaming logic. Native GameTests and the client harness provide real clip/use
and packet/prediction/acknowledgement coverage; JSON cannot establish those facts.
"""
import json
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'src/main/resources/assets/chao_garden'
MODEL=AS/'models/block'
TEX=AS/'textures/block/school025'
# FaceInfo vertex0 and vertex2: the indices receiving uv[0:2] and uv[2:4].
# East starts at MAX_Z, west at MIN_Z. Renaming normals alone reverses the picture.
CORNERS={'north':((1,1,0),(0,0,0)), 'south':((0,1,1),(1,0,1)),
         'east':((1,1,1),(1,0,0)), 'west':((0,1,0),(0,0,1))}
ROLES=('principal','classroom','health','fortune','exit','market')

def require(ok,message):
    if not ok:raise AssertionError(message)

def close(a,b):return abs(a-b)<1e-6

def check_model(model,hinge,opened,section,part,label):
    if model.get('chao_door_profile')=='arch0254':
        from validate_school_doors_0254 import check_model as check_cut_model
        return check_cut_model(model,hinge,opened,section,part,label)
    elements=model['elements']
    active=section=='leaf' or section==('tip' if opened else 'center')
    require(len(elements)==(2 if section=='center' and active else 1 if active else 0),label+': wrong active native mesh count')
    for e in elements:
        a,b=e['from'],e['to']
        require(all(0<=x<=16 for p in (a,b) for x in p),label+': rendered interactive surface escapes owning voxel')
        require(all(a[i]<b[i] for i in range(3)),label+': degenerate native slice')
        leaf=('left' if b[0]<=8 else 'right') if section=='center' else hinge
        # Derive normals from the actual rigid swing: (dx,dz)->(-sign*dz,sign*dx).
        # Mapping names without this transform was another original texture defect.
        sign=1 if leaf=='left' else -1
        normal_names={(-1,0):'west',(1,0):'east',(0,-1):'north',(0,1):'south'}
        front=normal_names[(-sign,0)] if opened else 'south'
        back=normal_names[(sign,0)] if opened else 'north'
        for face,data in e['faces'].items():
            if face not in (front,back):
                require(data['texture']=='#edge',label+': broad door art stretched over a cross-section')
                continue
            require(data['texture']==('#base' if face==front else '#back'),label+': wrong independent front/back material')
            require('rotation' not in data,label+': unexpected UV turn')
            width=b[2]-a[2] if opened else b[0]-a[0]
            require(close(abs(data['uv'][2]-data['uv'][0])*368/16/width,16)
                    and close(abs(data['uv'][3]-data['uv'][1])*768/16/(b[1]-a[1]),16),label+': door art stretches across a native slice')
            for indices,uv in zip(CORNERS[face],(data['uv'][:2],data['uv'][2:])):
                p=[(a[i],b[i])[indices[i]] for i in range(3)]
                if opened:
                    distance=p[2]+(16 if section=='tip' else 0)-8
                else:
                    physical_x=p[0]+(0 if section=='center' else -16 if leaf=='left' else 16)
                    distance=physical_x+15 if leaf=='left' else 31-physical_x
                expected_u=distance*16/23
                if face==back:expected_u=16-expected_u
                expected_v=(48-(part*16+p[1]))/3
                require(close(uv[0],expected_u) and close(uv[1],expected_v),
                        f'{label}: {face} material reversed/offset at native vertex {p}: {uv} vs {[expected_u,expected_v]}')
    return len(elements)

def main():
    checks=models=0
    for role in ROLES:
        ident='school_door_'+role
        state=json.loads((AS/'blockstates'/(ident+'.json')).read_text())['variants']
        require(len(state)==144,ident+': finite four-facing/three-section native state set is incomplete')
        for hinge in ('left','right'):
            for opened in (False,True):
                for section in ('leaf','center','tip'):
                    for part in range(3):
                        name=f'{ident}_{hinge}_{"open" if opened else "closed"}_{section}_{part}'
                        model=json.loads((MODEL/(name+'.json')).read_text())
                        check_model(model,hinge,opened,section,part,name);models+=1
                        for face,rotation in (('south',0),('west',90),('north',180),('east',270)):
                            variant=state[f'facing={face},hinge={hinge},open={str(opened).lower()},section={section},segment={part}']
                            require(variant['model']=='chao_garden:block/'+name and variant.get('y',0)==rotation,name+': facing transform differs from native shape rotation')
                            checks+=1
                        # Prove this validator rejects the old open-door face-order mistake.
                        if opened and section=='tip':
                            broken=json.loads(json.dumps(model));f='west' if hinge=='left' else 'east';uv=broken['elements'][0]['faces'][f]['uv'];uv[0],uv[2]=uv[2],uv[0]
                            try:check_model(broken,hinge,opened,section,part,name+' deliberate reversed mutant')
                            except AssertionError:pass
                            else:raise AssertionError('Reversed open-face mutant escaped physical UV validation')
                            swapped=json.loads(json.dumps(model));faces=swapped['elements'][0]['faces'];faces['east'],faces['west']=faces['west'],faces['east']
                            try:check_model(swapped,hinge,opened,section,part,name+' deliberate swapped-normal mutant')
                            except AssertionError:pass
                            else:raise AssertionError('Swapped front/back normal mutant escaped rigid swing validation')
        front=Image.open(TEX/(ident+'.png')).convert('RGBA');back=Image.open(TEX/(ident+'_back.png')).convert('RGBA')
        require(front.size==back.size==(368,768),ident+': front/back authoring aspect changed')
        handle=(157,127,47) if role=='market' else (240,206,99)
        require(front.getpixel((308,408))[:3]==handle and back.getpixel((60,408))[:3]==handle,ident+': front/back handles must sit at matching physical free edges')
        if role=='market':
            require(len(front.crop((80,120,280,500)).getcolors(2) or [])==1,ident+': storage cupboard upper panel must stay plain and unlabelled')
            require(all(front.getpixel((184,y*2))[:3]==(18,61,71) for y in (292,308,324)),ident+': missing three low vent slits')
            require(all(pixel[:3]!=(220,199,144) for count,pixel in front.getcolors(front.width*front.height)),ident+': ornate gold door border returned to the plain cupboard')
        # Badge and louver artwork are authored upright for their own viewing side.
        require(front.crop((80,40,290,380)).tobytes()==back.crop((80,40,290,380)).tobytes(),ident+': reverse-side badge/louvers have been blanket-mirrored')
    # A moved anchor must exactly counter-translate the old arch geometry.
    for role in ROLES:
        arch=json.loads((MODEL/('school_arch_'+role+'.json')).read_text())
        zs=[p[2] for e in arch['elements'] for p in (e['from'],e['to'])]
        require(close(min(zs),-16) and close(max(zs),.35),'Arch moved-anchor mesh is not exactly counter-translated: '+role)
    print(f'School doors 0.25.2: {models} native models; {checks} facing variants; active/inactive owning-cell geometry and physical front/back UVs verified; reversed mutants rejected.')

if __name__=='__main__':main()
