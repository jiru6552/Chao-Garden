#!/usr/bin/env python3
"""Native block models for the walk-in Chao tube; uses existing Minecraft textures."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'

def write(path,value):
    target=RES/path;target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(value,indent=2)+'\n')

def cube(start,end,texture):
    return {'from':list(start),'to':list(end),'faces':{d:{'texture':'#'+texture} for d in ('north','south','east','west','up','down')}}

def model(elements):
    return {'parent':'minecraft:block/block','ambientocclusion':False,'render_type':'minecraft:translucent',
            'textures':{'particle':'minecraft:block/gold_block','gold':'minecraft:block/gold_block',
                        'glass':'minecraft:block/cyan_stained_glass','light':'minecraft:block/sea_lantern'},
            'elements':elements}

def main():
    lower=[cube((0,0,0),(16,1,16),'gold'),cube((2,1,2),(14,1.25,14),'light')]
    upper=[cube((0,15,0),(16,16,16),'gold'),cube((2,14.75,2),(14,15,14),'light')]
    for x in (0,14):
        for z in (0,14):
            lower.extend([cube((x,1,z),(x+2,3,z+2),'gold'),cube((x,3,z),(x+2,16,z+2),'glass')])
            upper.extend([cube((x,0,z),(x+2,13,z+2),'glass'),cube((x,13,z),(x+2,15,z+2),'gold')])
    for elements,low,high in ((lower,3,16),(upper,0,13)):
        elements.extend([cube((.25,low,2),(.5,high,14),'glass'),cube((15.5,low,2),(15.75,high,14),'glass')])
    write('assets/chao_garden/models/block/chao_teleporter_lower.json',model(lower))
    write('assets/chao_garden/models/block/chao_teleporter_upper.json',model(upper))
    write('assets/chao_garden/blockstates/chao_teleporter.json',{'variants':{
        'half=lower':{'model':'chao_garden:block/chao_teleporter_lower'},
        'half=upper':{'model':'chao_garden:block/chao_teleporter_upper'}}})
    complete=lower+[dict(element,**{'from':[element['from'][0],element['from'][1]+16,element['from'][2]],
                                  'to':[element['to'][0],element['to'][1]+16,element['to'][2]]}) for element in upper]
    item=model(complete)
    item['display']={'gui':{'rotation':[30,225,0],'translation':[0,-1,0],'scale':[.43,.43,.43]},
                     'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.22,.22,.22]},
                     'fixed':{'rotation':[0,0,0],'translation':[0,-1,0],'scale':[.43,.43,.43]},
                     'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,0,0],'scale':[.25,.25,.25]},
                     'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,-1,0],'scale':[.35,.35,.35]}}
    write('assets/chao_garden/models/item/chao_teleporter.json',item)
    fragment=ROOT/'art/hub_lang_fragment_024.json'
    fragment.write_text(json.dumps({'block.chao_garden.chao_teleporter':'Chao Teleporter',
        'item.chao_garden.chao_teleporter':'Chao Teleporter',
        'itemGroup.chao_garden':'Chao Garden'},indent=2)+'\n')
    language=RES/'assets/chao_garden/lang/en_us.json';data=json.loads(language.read_text());data.update(json.loads(fragment.read_text()))
    language.write_text(json.dumps(data,indent=2,ensure_ascii=False,sort_keys=True)+'\n')
    print('Hub024 native tube models: two block halves, open walking center, full-height item; no new textures.')

if __name__=='__main__':main()
