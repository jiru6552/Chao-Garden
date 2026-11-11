#!/usr/bin/env python3
"""Static metal-case ownership and independently evaluated moving-leaf UV regression.

Native GameTests/client checks establish interaction and physical passage separately.
"""
from copy import deepcopy
from pathlib import Path
import json
from PIL import Image
from validate_school_doors_0252 import check_model

ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'src/main/resources/assets/chao_garden'
ID='school_tool_cabinet'

def check(doc,hinge,opened,section,part):
    panels=[e for e in doc['elements'] if any(f['texture']=='#base' for f in e['faces'].values())]
    cases=[e for e in doc['elements'] if all(f['texture']=='#case' for f in e['faces'].values())]
    assert len(panels)+len(cases)==len(doc['elements']), 'Cabinet has an unreviewed physical material/surface'
    check_model(dict(doc,elements=panels),hinge,opened,section,part,ID)
    y0=2 if part==0 else 0;y1=14 if part==2 else 16
    assert all(e['from'][1]==y0 and e['to'][1]==y1 for e in panels), 'Moving leaves pierce the case lip'
    expected=[]
    if section!='tip':
        if part==0:expected.append(([0,0,0],[16,2,10]))
        if part==2:expected.append(([0,14,0],[16,16,10]))
        if section=='leaf':expected.append(([0 if hinge=='left' else 15,y0,0],[1 if hinge=='left' else 16,y1,7]))
    assert [(e['from'],e['to']) for e in cases]==expected, 'Opening erased or moved the permanent steel carcass'
    assert doc['textures']['case']=='chao_garden:block/school025/'+ID+'_case'
    for e in cases:
        a,b=e['from'],e['to']
        for face,data in e['faces'].items():
            axes=(0,1) if face in ('north','south') else (2,1) if face in ('east','west') else (0,2)
            assert all(abs(abs(data['uv'][i+2]-data['uv'][i])-(b[axis]-a[axis]))<1e-6 for i,axis in enumerate(axes)), 'Steel case texture is stretched on an exposed edge'

def main():
    manifest=json.loads((AS/'kindergarten_decor025.json').read_text())
    assert [r for r in manifest['blocks'] if r['id']==ID]==[{'id':ID,'kind':'DOOR','item':True}], 'The utility cabinet is not its own registered native block/item'
    states=json.loads((AS/'blockstates'/f'{ID}.json').read_text())['variants']
    assert len(states)==192
    checked=0;mutants=0
    for hinge in ('left','right'):
        for opened in (False,True):
            for section in ('leaf','center','tip'):
                for part in range(3):
                    name=f'{ID}_{hinge}_{"open" if opened else "closed"}_{section}_{part}'
                    doc=json.loads((AS/'models/block'/f'{name}.json').read_text());check(doc,hinge,opened,section,part);checked+=1
                    for face,turn in (('south',0),('west',90),('north',180),('east',270)):
                        state=states[f'facing={face},hinge={hinge},open={str(opened).lower()},section={section},segment={part}']
                        assert state['model']=='chao_garden:block/'+name and state.get('y',0)==turn
                    if section=='leaf':
                        broken=deepcopy(doc);broken['elements'].pop()
                        try:check(broken,hinge,opened,section,part)
                        except AssertionError:mutants+=1
                        else:raise AssertionError('Case-erasure mutant was accepted')
    # The shared native enum adds FAR_TIP for outward ornate doors. Cabinets
    # retain fifteen owned cells and route every unused far state to an existing
    # empty closed-tip model, never to an open moving panel or the steel case.
    for hinge in ('left','right'):
        for opened in (False,True):
            for part in range(3):
                inactive=f'{ID}_{hinge}_closed_tip_{part}'
                assert not json.loads((AS/'models/block'/f'{inactive}.json').read_text())['elements']
                for face,turn in (('south',0),('west',90),('north',180),('east',270)):
                    state=states[f'facing={face},hinge={hinge},open={str(opened).lower()},section=far_tip,segment={part}']
                    assert state['model']=='chao_garden:block/'+inactive and state.get('y',0)==turn
    item=json.loads((AS/'models/block'/f'{ID}.json').read_text())
    assert len(item['elements'])>12 and min(e['from'][0] for e in item['elements'])==0 and max(e['to'][0] for e in item['elements'])==16, 'Creative item does not show the complete cabinet case and both leaves'
    for suffix in ('','_back'):
        with Image.open(AS/'textures/block/school025'/f'{ID}{suffix}.png') as im:
            assert im.size==(368,768) and im.getchannel('A').getextrema()==(255,255), 'Metal leaf has invalid aspect or transparency'
            assert im.getpixel((184,100))[:3]==(66,78,89), 'Cabinet lost its stamped metal ventilation slits'
    print(f'Cabinet0253 static assets: {checked} models,144 preserved facing states plus48 inert enum states, fixed steel case/open back, physical UVs; {mutants} erased-case mutants rejected. PASS')

if __name__=='__main__':main()
