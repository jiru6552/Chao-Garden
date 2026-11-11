#!/usr/bin/env python3
"""Native kindergarten facilities and remedy art, applied after older generators."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden'
D=ROOT/'src/main/resources/data/chao_garden'

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n')

def save(im,path):
    path.parent.mkdir(parents=True,exist_ok=True);im.save(path)

def cube(lo,hi,texture):
    x,y,z=lo;X,Y,Z=hi
    faces={'north':[x,16-Y,X,16-y],'south':[16-X,16-Y,16-x,16-y],
           'west':[z,16-Y,Z,16-y],'east':[16-Z,16-Y,16-z,16-y],
           'up':[x,z,X,Z],'down':[x,16-Z,X,16-z]}
    return {'from':lo,'to':hi,'faces':{f:{'texture':'#'+texture,'uv':uv} for f,uv in faces.items()}}

def main():
    wood=Image.new('RGBA',(16,16),'#987248');d=ImageDraw.Draw(wood)
    for y in (0,7,15):d.line((0,y,15,y),fill='#795638')
    d.line((0,1,15,1),fill='#b18c5d');d.line((1,8,14,8),fill='#a47d51')
    save(wood,A/'textures/block/lesson_wood.png')
    paper=Image.new('RGBA',(16,16),'#e7e0bb');d=ImageDraw.Draw(paper)
    d.rectangle((0,0,15,1),fill='#497e99');d.rectangle((0,14,15,15),fill='#497e99')
    d.line((7,2,7,13),fill='#b8b197')
    for y in (4,7,10):d.line((1,y,5,y),fill='#738d9a');d.line((9,y,13,y),fill='#738d9a')
    save(paper,A/'textures/block/lesson_book.png')
    cabinet=Image.new('RGBA',(16,16),'#bbdfe5');d=ImageDraw.Draw(cabinet)
    d.rectangle((0,0,15,1),fill='#4d9bad');d.rectangle((0,14,15,15),fill='#45899b')
    d.line((1,8,14,8),fill='#80b6c2');d.rectangle((10,5,13,5),fill='#507d91');d.rectangle((10,11,13,11),fill='#507d91')
    save(cabinet,A/'textures/block/doctor_cabinet.png')
    screen=Image.new('RGBA',(16,16),'#2d596d');d=ImageDraw.Draw(screen)
    d.rectangle((1,1,14,14),fill='#75d4df');d.rectangle((4,5,11,10),fill='#b5edf0')
    d.rectangle((6,2,9,3),fill='#e7e88a');d.rectangle((5,6,5,8),fill='#365f76');d.rectangle((10,6,10,8),fill='#365f76')
    d.line((2,12,13,12),fill='#e4f6de');save(screen,A/'textures/block/doctor_screen.png')
    medicine=Image.new('RGBA',(16,16));d=ImageDraw.Draw(medicine)
    d.rectangle((5,1,10,3),fill='#5c96b0');d.rectangle((4,4,11,13),fill='#e0eff0')
    d.rectangle((3,5,12,12),fill='#a3d9e2');d.rectangle((5,4,6,12),fill='#eaf7f4')
    d.rectangle((4,13,11,14),fill='#5f9caf');d.rectangle((4,7,11,10),fill='#e8e3bd')
    d.point((6,8),fill='#e17891');d.point((9,8),fill='#e17891');d.line((6,9,9,9),fill='#e17891');d.line((7,10,8,10),fill='#e17891')
    save(medicine,A/'textures/item/chao_medicine.png')
    write(A/'models/item/chao_medicine.json',{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/chao_medicine'}})
    desk=[cube([0,9,1],[16,11,15],'wood')]
    for x in (1,13):
        for z in (2,12):desk.append(cube([x,0,z],[x+2,9,z+2],'wood'))
    desk.extend((cube([4,11,5],[12,12,12],'book'),cube([7.6,12,5],[8.4,12.5,12],'book')))
    clinic=[cube([2,0,2],[14,10,14],'cabinet'),cube([3,10,4],[13,16,12],'cabinet')]
    monitor=cube([4,11,3.8],[12,15,4],'cabinet')
    monitor['faces']['north']={'texture':'#screen','uv':[0,0,16,16]};clinic.append(monitor)
    for ident,parts,particle in (('lesson_desk',desk,'wood'),('doctor_station',clinic,'cabinet')):
        textures={'wood':'chao_garden:block/lesson_wood','book':'chao_garden:block/lesson_book',
                  'cabinet':'chao_garden:block/doctor_cabinet','screen':'chao_garden:block/doctor_screen'}
        textures['particle']=textures[particle]
        write(A/'models/block'/f'{ident}.json',{'textures':textures,'elements':parts})
        write(A/'models/item'/f'{ident}.json',{'parent':'chao_garden:block/'+ident})
        write(A/'blockstates'/f'{ident}.json',{'variants':{f'facing={direction}':{'model':'chao_garden:block/'+ident,'y':angle}
            for direction,angle in (('north',0),('east',90),('south',180),('west',270))}})
        write(D/'loot_table/blocks'/f'{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'chao_garden:'+ident}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(D/'recipe/chao_medicine.json',{'type':'minecraft:crafting_shapeless','ingredients':[
        {'item':'minecraft:sugar'},{'item':'minecraft:honey_bottle'},{'item':'minecraft:paper'}],
        'result':{'id':'chao_garden:chao_medicine','count':2}})
    # 0.24: these remain save-compatible world fixtures, acquired through Kindergarten only.
    for ident in ('lesson_desk','doctor_station'):
        (D/'recipe'/f'{ident}.json').unlink(missing_ok=True)
    langpath=A/'lang/en_us.json';lang=json.loads(langpath.read_text());lang.update({
        'item.chao_garden.chao_medicine':'Chao Medicine','block.chao_garden.lesson_desk':'Chao Lesson Desk',
        'block.chao_garden.doctor_station':'Chao Doctor Station'})
    write(langpath,lang)
    for tag in ('mineable/axe',):
        p=ROOT/'src/main/resources/data/minecraft/tags/block'/f'{tag}.json'
        data=json.loads(p.read_text()) if p.exists() else {'replace':False,'values':[]}
        data['values']=list(dict.fromkeys(data['values']+['chao_garden:lesson_desk','chao_garden:doctor_station']));write(p,data)
    ledger=ROOT/'scripts/resource_items.json';items=json.loads(ledger.read_text())
    write(ledger,list(dict.fromkeys(items+['chao_medicine','lesson_desk','doctor_station'])))
    print('Doctor medicine, four-facing Lesson Desk/Doctor Station, recipes and native art generated.')

if __name__=='__main__':main()
