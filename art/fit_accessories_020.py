#!/usr/bin/env python3
"""Finish CWE upper-arm sleeves after the unchanged 0.19 content writer.

Runtime ChaoAccessoryFit fits garments against the current morphed skin, not
against a duplicated deformation formula. These sleeves use the source upper
arms' outward X axis; the old centered vertical boxes covered wrists/hands.
"""
from pathlib import Path
import json,copy
import build_cwe_content_019 as cwe

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/assets/chao_garden/models/entity/cwe_accessories'

def sleeve(side):
    p=OUT/f'hoodie_sleeve_{side}.json'
    data=json.loads(p.read_text())
    bounds=(-.12,-.15,-.65,2.16,1.15,.65) if side=='left' else (-2.16,-.15,-.65,.12,1.15,.65)
    x,y,z,X,Y,Z=bounds
    data['bones'][0]['meshes'][0]['vertices']=[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]]
    data['authored_metadata']={'fitting':'upper-arm outward axis; dynamically fitted to current gameplay arm core','release':'0.20.0'}
    p.write_text(json.dumps(data,separators=(',',':'))+'\n')

def spectacles(ident):
    # Start from the legacy author's pristine geometry on every run. The two
    # frames were too wide once eye spacing was correctly fitted, especially
    # Amy's narrow skull. Narrow each around its eye, preserving eye centres.
    p=OUT/f'{ident}.json';data=cwe.mesh(ident);main=data['bones'][0]
    for index,part in enumerate(main['meshes'][:10]):
        centre=-1.6 if index<5 else 1.5
        for vertex in part['vertices']:vertex[0]=centre+(vertex[0]-centre)*.8
    for vertex in main['meshes'][10]['vertices']:vertex[0]=-.65 if vertex[0]<0 else .55
    original=main['meshes'];main['meshes']=original[:11]
    for side,index,inner in [('left',11,-2.45),('right',12,2.35)]:
        data['bones'].append({'name':f'temple_{side}','parent':'accessory','pivot':[0,0,0],'rotation':[0,0,0],'meshes':[original[index]]})
        hinge=copy.deepcopy(original[index]);hinge['name']='spectacle_hinge'
        x,X=(-.35,0) if side=='left' else (0,.35)
        y,Y,z,Z=-.3,0,-3.25,-3.05
        hinge['vertices']=[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]]
        data['bones'].append({'name':f'hinge_{side}','parent':'accessory','pivot':[inner,0,0],'rotation':[0,0,0],'meshes':[hinge]})
    data['authored_metadata']={'fitting':'narrow eye-centred frames; independent hinges fit head width including Amy','release':'0.20.0'}
    p.write_text(json.dumps(data,separators=(',',':'))+'\n')

if __name__=='__main__':
    for side in ('left','right'):sleeve(side)
    for ident in ('round_glasses_accessory','aviators_accessory'):spectacles(ident)
    print('0.20.0 accessory finalizer: anatomical upper-arm sleeves and independently fitted spectacle hinges; runtime skin/eye/crown fitting enabled')
