#!/usr/bin/env python3
"""Original native art for a compact Battle-style Dark Garden foundation."""
from pathlib import Path
import json
import math
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
AS=RES/'assets/chao_garden'
TEX=AS/'textures/block/dark0255'
IDS=('dark_garden_soil','dark_garden_rock','dark_garden_deadwood','dark_garden_gravestone','dark_garden_fence','dark_garden_effigy')
SIZE=64


def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')


def stone(base,grain,vein=False):
    im=Image.new('RGBA',(SIZE,SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            # Integer periodic terms keep all four tile boundaries continuous.
            shift=round(grain*(.45*math.sin(2*math.pi*x/63)+.35*math.cos(4*math.pi*y/63)+.2*math.sin(2*math.pi*(x+y)/63)))
            if vein:shift-=round(max(0,math.cos(2*math.pi*(x+2*y)/63)-.85)*20)
            im.putpixel((x,y),tuple(max(0,min(255,c+shift)) for c in base)+(255,))
    return im


def deadwood():
    im=stone((35,32,42),5)
    d=ImageDraw.Draw(im)
    for x in (7,19,35,51):d.line((x,0,x+3,63),fill='#191820',width=2)
    return im


def grave():
    im=stone((154,154,169),6,True);d=ImageDraw.Draw(im)
    d.ellipse((13,10,50,47),outline='#777787',width=2)
    d.ellipse((19,19,44,40),outline='#777787',width=2)
    d.polygon(((25,22),(32,13),(39,22)),outline='#777787')
    d.rectangle((26,26,28,32),fill='#777787');d.rectangle((36,26,38,32),fill='#777787')
    d.line((18,52,46,52),fill='#777787',width=2)
    return im


def effigy():
    im=stone((48,48,66),7,True);d=ImageDraw.Draw(im)
    # Authored angular Chao mask: the three-by-three tile assembly is one
    # complete distant face, never nine repeated little face stickers.
    d.polygon(((6,16),(19,27),(29,25),(25,38),(16,39)),fill='#10101a')
    d.polygon(((58,16),(45,27),(35,25),(39,38),(48,39)),fill='#10101a')
    d.polygon(((11,51),(18,43),(24,49),(31,41),(38,49),(45,43),(54,51),(49,61),(42,54),(36,62),(30,54),(23,62),(17,54)),fill='#10101a')
    return im


def cube(a,b,texture='base'):
    x0,y0,z0=a;x1,y1,z1=b
    uv={'north':[16-x1,16-y1,16-x0,16-y0],'south':[x0,16-y1,x1,16-y0],
        'east':[16-z1,16-y1,16-z0,16-y0],'west':[z0,16-y1,z1,16-y0],
        'up':[x0,z0,x1,z1],'down':[x0,16-z1,x1,16-z0]}
    return {'from':a,'to':b,'faces':{face:{'texture':'#'+texture,'uv':values} for face,values in uv.items()}}


def merge_tag(name,value):
    path=RES/f'data/minecraft/tags/block/{name}.json'
    data=json.loads(path.read_text()) if path.exists() else {'replace':False,'values':[]}
    if value not in data['values']:data['values'].append(value)
    write(path,data)


def sky():
    im=Image.new('RGBA',(512,256))
    for y in range(256):
        latitude=y/255
        for x in range(512):
            angle=x/511*math.tau
            cloud=(math.sin(5*angle+latitude*12)+.7*math.cos(11*angle-latitude*29)
                   +.4*math.sin(17*angle+latitude*41))
            dark=max(0,min(1,(cloud+.5)/1.8))
            horizon=math.exp(-((latitude-.50)/.23)**2)
            base=(55+round(39*horizon),29+round(16*horizon),100+round(38*horizon))
            color=tuple(round(c*(1-.82*dark)) for c in base)
            # Polar caps stay uniform, avoiding texture pinwheels.
            cap=min(1,y/15,(255-y)/15)
            color=tuple(round(color[i]*cap+(40,23,77)[i]*(1-cap)) for i in range(3))
            im.putpixel((x,y),color+(255,))
    return im


def main():
    TEX.mkdir(parents=True,exist_ok=True)
    palettes={
        'dark_garden_soil':stone((200,199,210),5),
        'dark_garden_rock':stone((142,145,166),10,True),
        'dark_garden_deadwood':deadwood(),
        'dark_garden_gravestone':grave(),
        'dark_garden_fence':stone((42,37,50),4),
        'dark_garden_effigy':stone((48,48,66),7,True),
    }
    for ident,im in palettes.items():im.save(TEX/f'{ident}.png')
    # The Chao engraving belongs only on the broad front/back surfaces.
    stone((154,154,169),6,True).save(TEX/'dark_garden_gravestone_plain.png')
    effigy().save(TEX/'dark_garden_effigy_face.png')
    scene=AS/'textures/scene';scene.mkdir(parents=True,exist_ok=True);sky().save(scene/'dark_twilight_sky.png')
    for ident in IDS:
        ref=f'chao_garden:block/dark0255/{ident}'
        write(AS/f'models/item/{ident}.json',{'parent':f'chao_garden:block/{ident}'})
        write(RES/f'data/chao_garden/loot_table/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'chao_garden:{ident}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        if ident=='dark_garden_deadwood':
            write(AS/f'models/block/{ident}.json',{'textures':{'base':ref,'particle':ref},'elements':[cube([6,0,6],[10,16,10])]})
            write(AS/f'models/block/{ident}_joint.json',{'textures':{'base':ref,'particle':ref},'elements':[cube([6,0,6],[10,16,10]),cube([0,6,6],[16,10,10]),cube([6,6,0],[10,10,16])]})
            states={}
            for axis,rotation in (('y',{}),('x',{'x':90,'y':90}),('z',{'x':90})):
                states[f'axis={axis},branched=false']={'model':f'chao_garden:block/{ident}',**rotation}
                states[f'axis={axis},branched=true']={'model':f'chao_garden:block/{ident}_joint'}
            write(AS/f'blockstates/{ident}.json',{'variants':states})
        elif ident=='dark_garden_gravestone':
            rows=((1,15,0,3),(3,13,3,12),(4,12,12,14),(5,11,14,15),(6,10,15,16))
            elements=[cube([a,y0,5],[b,y1,11]) for a,b,y0,y1 in rows]
            for element in elements:
                for side in ('north','south'):element['faces'][side]['texture']='#engraving'
            write(AS/f'models/block/{ident}.json',{'textures':{'base':ref+'_plain','engraving':ref,'particle':ref+'_plain'},'elements':elements})
            write(AS/f'blockstates/{ident}.json',{'variants':{f'facing={face}':{'model':f'chao_garden:block/{ident}',**({'y':turn} if turn else {})} for face,turn in (('south',0),('west',90),('north',180),('east',270))}})
        elif ident=='dark_garden_fence':
            for name,parent in (('post','fence_post'),('side','fence_side')):write(AS/f'models/block/{ident}_{name}.json',{'parent':f'minecraft:block/{parent}','textures':{'texture':ref}})
            write(AS/f'models/block/{ident}.json',{'parent':'minecraft:block/fence_inventory','textures':{'texture':ref}})
            write(AS/f'blockstates/{ident}.json',{'multipart':[{'apply':{'model':f'chao_garden:block/{ident}_post'}}]+[{'when':{face:'true'},'apply':{'model':f'chao_garden:block/{ident}_side','y':turn,'uvlock':True}} for face,turn in (('north',0),('east',90),('south',180),('west',270))]})
        elif ident=='dark_garden_effigy':
            variants={}
            for tile in range(10):
                model={'parent':'minecraft:block/cube_all','textures':{'all':ref}}
                if tile:
                    col=(tile-1)%3;row=(tile-1)//3
                    model={'textures':{'base':ref,'face':'chao_garden:block/dark0255/dark_garden_effigy_face','particle':ref},'elements':[cube([0,0,0],[16,16,16])]}
                    model['elements'][0]['faces']['south']={'texture':'#face','uv':[col*16/3,row*16/3,(col+1)*16/3,(row+1)*16/3]}
                write(AS/f'models/block/{ident}_{tile}.json',model)
                for face,turn in (('south',0),('west',90),('north',180),('east',270)):variants[f'facing={face},tile={tile}']={'model':f'chao_garden:block/{ident}_{tile}',**({'y':turn} if turn else {})}
            write(AS/f'models/block/{ident}.json',{'parent':f'chao_garden:block/{ident}_0'})
            write(AS/f'blockstates/{ident}.json',{'variants':variants})
        else:
            write(AS/f'models/block/{ident}.json',{'parent':'minecraft:block/cube_all','textures':{'all':ref}})
            write(AS/f'blockstates/{ident}.json',{'variants':{'':{'model':f'chao_garden:block/{ident}'}}})
    merge_tag('dirt','chao_garden:dark_garden_soil')
    merge_tag('fences','chao_garden:dark_garden_fence')
    write(AS/'dark_garden_decor0255.json',{'version':'0.25.5','texturePrefix':'block/dark0255/','blocks':[{'id':i,'item':True} for i in IDS]})
    write(ROOT/'art/dark_garden_lang_fragment_0255.json',{f'block.chao_garden.{i}':name for i,name in zip(IDS,('Dark Garden Ashen Shore','Dark Garden Pale Crag','Dark Garden Bare Fruit Trunk','Dark Garden Gravestone','Dark Garden Iron Fence','Dark Garden Distant Effigy'))})
    write(RES/'data/chao_garden/worldgen/biome/dark_twilight.json',{'has_precipitation':False,'temperature':.7,'downfall':0,'effects':{'fog_color':0x403156,'sky_color':0x39215c,'water_color':0xb12b35,'water_fog_color':0x501520,'grass_color':0x948b9b,'foliage_color':0x8e783c},'carvers':{},'features':[],'spawners':{},'spawn_costs':{},'creature_spawn_probability':0.0})
    print('Dark Garden0255: six native blocks, pale ash/rock, red-water biome, original purple cloud dome and strict dirt/fence tag merges.')


if __name__=='__main__':main()
