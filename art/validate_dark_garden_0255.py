#!/usr/bin/env python3
"""Validate the actual six-block Dark palette, finite meshes, atmosphere and care bindings."""
from pathlib import Path
import argparse
import copy
import json
import math
import re
from PIL import Image
from dark_resource_contract_0255 import dark_items

ROOT=Path(__file__).resolve().parents[1]
CHECKS=0


def check(ok,message):
    global CHECKS
    CHECKS+=1
    assert ok,message


def read(path):return json.loads(path.read_text())


def model_contract(model):
    for element in model.get('elements',[]):
        a,b=element['from'],element['to'];check(all(0<=a[i]<b[i]<=16 for i in range(3)),'Dark model exceeds its physical native cell')
        check(set(element['faces'])=={'north','south','east','west','up','down'},'Dark solid has an uncapped native face')
        for side,face in element['faces'].items():
            u0,v0,u1,v1=face['uv'];check(all(0<=v<=16 for v in face['uv']),'Dark UV exceeds its atlas')
            if face['texture']=='#face':continue # One complete distant mask spans nine native tiles.
            width,height=(b[0]-a[0],b[1]-a[1]) if side in ('north','south') else ((b[2]-a[2],b[1]-a[1]) if side in ('east','west') else (b[0]-a[0],b[2]-a[2]))
            check(abs(abs(u1-u0)-width)<1e-7 and abs(abs(v1-v0)-height)<1e-7,'Dark material was stretched across a cropped member')


def contract(root,override=None):
    a=root/'src/main/resources/assets/chao_garden';override=override or {}
    ids=dark_items(root);check(len(ids)==6,'Dark registry domain differs')
    for ident in ids:
        check((a/f'models/item/{ident}.json').is_file(),'Dark BlockItem model missing')
        loot=read(root/f'src/main/resources/data/chao_garden/loot_table/blocks/{ident}.json')
        check(loot['pools'][0]['entries']==[{'type':'minecraft:item','name':'chao_garden:'+ident}],'Dark native loot duplicates or changes its item')
        with Image.open(a/f'textures/block/dark0255/{ident}.png') as im:
            check(im.size==(64,64) and im.mode=='RGBA','Dark material has inconsistent native texel density')
            check(im.getextrema()[3]==(255,255),'Dark solid material is translucent')
    biome=override.get('biome',read(root/'src/main/resources/data/chao_garden/worldgen/biome/dark_twilight.json'))
    check(biome['effects']['water_color']==0xb12b35,'Dark pool lost its cosmetic red native-water tint')
    check(biome['spawners']=={} and biome['features']==[] and biome['carvers']=={} and biome['creature_spawn_probability']==0,'Dark biome can add destructive terrain or wildlife')
    check(not biome['has_precipitation'],'Dark storm image became actual damaging/stochastic weather')
    with Image.open(a/'textures/block/dark0255/dark_garden_soil.png') as im:
        r,g,b,alpha=im.getpixel((32,32));check(min(r,g,b)>175 and max(r,g,b)-min(r,g,b)<25,'Battle Dark shore became dark grass rather than pale ash')
    sky=Image.open(a/'textures/scene/dark_twilight_sky.png')
    check(sky.size==(512,256) and sky.mode=='RGBA' and sky.getextrema()[3]==(255,255),'Dark cloud sphere has a translucent/wrong-sized texture')
    check(all(sky.getpixel((0,y))==sky.getpixel((511,y)) for y in range(256)),'Dark cloud sphere has a longitude seam')
    check(all(sky.getpixel((x,y))==sky.getpixel((0,y)) for x in range(512) for y in (0,255)),'Dark cloud dome has a polar pinwheel')
    textures={}
    for path in (a/'models/block').glob('dark_garden_*.json'):
        if not any(path.stem==ident or path.stem.startswith(ident+'_') for ident in ids):continue
        model=override.get(path.stem,read(path));model_contract(model);textures[path.stem]=model
    wood=read(a/'blockstates/dark_garden_deadwood.json')['variants']
    check(set(wood)=={f'axis={axis},branched={fork}' for axis in 'xyz' for fork in ('false','true')},'Bare wood finite axis/joint domain differs')
    check(textures['dark_garden_deadwood']['elements'][0]['from']==[6,0,6] and textures['dark_garden_deadwood']['elements'][0]['to']==[10,16,10],'Bare fruit trunk became a thick square tree')
    check(len(textures['dark_garden_deadwood_joint']['elements'])==3,'Bare branches lost their full-face-connected native joint')
    graves=read(a/'blockstates/dark_garden_gravestone.json')['variants']
    check(set(graves)=={f'facing={s}' for s in ('north','east','south','west')},'Gravestone facing domain differs')
    grave=textures['dark_garden_gravestone']
    check(len(grave['elements'])==5,'Gravestone lost its rounded native top')
    check(grave['textures']=={'base':'chao_garden:block/dark0255/dark_garden_gravestone_plain','engraving':'chao_garden:block/dark0255/dark_garden_gravestone','particle':'chao_garden:block/dark0255/dark_garden_gravestone_plain'},'Gravestone narrow faces have no separate unmarked stone material')
    for element in grave['elements']:
        for side,face in element['faces'].items():
            check(face['texture']==('#engraving' if side in ('north','south') else '#base'),'Gravestone engraving repeats on a side/top or is missing from front/back')
    with Image.open(a/'textures/block/dark0255/dark_garden_gravestone_plain.png') as plain,Image.open(a/'textures/block/dark0255/dark_garden_gravestone.png') as engraved:
        check(plain.size==(64,64) and plain.mode=='RGBA' and plain.getextrema()[3]==(255,255),'Plain gravestone edge is not an opaque matching-resolution material')
        check(plain.tobytes()!=engraved.tobytes(),'Plain gravestone edge still contains the face engraving')
    # All growth stages must actually meet the underside of the new native arm,
    # including immature stems after a player harvests and the scene reloads.
    for age in range(4):
        pod=read(a/f'models/block/dark_garden_fruit_pod_{age}.json')
        obj=(a/pod['model'].split(':',1)[1]).read_text().splitlines()
        verts=[list(map(float,line.split()[1:])) for line in obj if line.startswith('v ')]
        stemtop=[v for v in verts if abs(v[1]*16-22)<1e-5]
        check(bool(stemtop) and all(abs(v[0]*16-8)<=.50001 and abs(v[2]*16-8)<=.50001 for v in stemtop),'Dark OBJ stem does not reach branch underside')
        check(22-16==textures['dark_garden_deadwood']['elements'][0]['from'][0],'Dark stem and rotated limb leave a gap')
    effigy=override.get('effigy_states',read(a/'blockstates/dark_garden_effigy.json'))['variants']
    check(set(effigy)=={f'facing={face},tile={tile}' for face in ('north','east','south','west') for tile in range(10)},'Distant mask gained duplicate item IDs or lost a native tile/facing')
    for tile in range(1,10):
        face=textures[f'dark_garden_effigy_{tile}']['elements'][0]['faces']['south'];col=(tile-1)%3;row=(tile-1)//3
        check(face=={'texture':'#face','uv':[col*16/3,row*16/3,(col+1)*16/3,(row+1)*16/3]},'Distant Chao mask repeats/stretches a whole face in every tile')
    source=(root/'src/main/java/com/chaogarden/client/DarkGardenEffects0255.java').read_text()
    check('SkyType.NONE' in source and 'HubContent.DARK_GARDEN' in source,'Dark sky is not isolated or regained a vanilla sun/moon')
    check('private static final java.util.List<SkyVertex> DOME=dome();' in source,'Dark sphere geometry is rebuilt every frame')
    check('poses.mulPose(event.getModelViewMatrix())' in source and 'poses.translate(-camera.x,-camera.y,-camera.z)' in source,'Dark cloud world sphere is screen-fixed at oblique angles')
    rooms=(root/'src/main/java/com/chaogarden/hub/DarkGardenRooms0255.java').read_text()
    check('GardenTransporterBlock.putScene(p,TRANSPORTER,Direction.NORTH)' in rooms,'Dark care provider is backwards or one cell only')
    check('DarkGardenDecor0255.Effigy.TILE,1+row*3+col' in rooms,'Distant crag lost its complete single mask')
    check('GardenTransporterBlock.canMergeScene' in rooms and rooms.index('GardenTransporterBlock.canMergeScene')<rooms.index('p.apply(level,BOUNDS'),'Dark provider can expand after destructive scenery writes')
    check('else if(level.getBlockEntity(at)!=null' in rooms and 'p.apply(level,BOUNDS,(at,state)->preserve.contains(at))' in rooms,'Dark preparation silently erases a keeper block entity/provider')
    check('Blocks.LAVA' not in rooms and 'Blocks.MAGMA_BLOCK' not in rooms,'Reference red water became damaging lava')
    support=(root/'src/main/java/com/chaogarden/fruit/HangingFruitBlock.java').read_text()
    check('wood.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,d.getAxis())' in rooms and 'p.put(x,FRUIT_BRANCH_Y,z,joint)' in rooms and 'FRUIT_POD_Y=82,FRUIT_BRANCH_Y=83' in rooms,'Dark fruit has no visible arm connected to the central trunk')
    check('LobbyDecor025.PALM_SUPPORT' not in rooms,'Dark fruit still hangs from an invisible palm support')
    check('darkArm.getValue(BlockStateProperties.AXIS)==towardTree.getAxis()' in support and 'darkJoint.getValue(com.chaogarden.hub.DarkGardenDecor0255.Deadwood.BRANCHED)' in support and support.count('DarkGardenDecor0255.block("dark_garden_deadwood")')==3,'Dark bare fruit survival accepts unsupported floating fruit')
    dirt=read(root/'src/main/resources/data/minecraft/tags/block/dirt.json')
    check('chao_garden:dark_garden_soil' in dirt['values'] and not dirt.get('replace',False),'Dark soil excludes player saplings or replaces all existing dirt')
    return biome,textures


def main(root,self_test):
    biome,textures=contract(root);mutants=0
    if self_test:
        bad=copy.deepcopy(biome);bad['effects']['water_color']=0x6fbde8
        bad2=copy.deepcopy(biome);bad2['spawners']={'monster':[{'type':'minecraft:zombie','weight':1,'minCount':1,'maxCount':1}]}
        stretch=copy.deepcopy(textures['dark_garden_deadwood']);stretch['elements'][0]['faces']['south']['uv']=[0,0,16,16]
        uncapped=copy.deepcopy(textures['dark_garden_gravestone']);uncapped['elements'][0]['faces'].pop('up')
        states=read(root/'src/main/resources/assets/chao_garden/blockstates/dark_garden_effigy.json');states['variants'].pop('facing=south,tile=9')
        side_engraving=copy.deepcopy(textures['dark_garden_gravestone']);side_engraving['elements'][1]['faces']['east']['texture']='#engraving'
        top_engraving=copy.deepcopy(textures['dark_garden_gravestone']);top_engraving['elements'][-1]['faces']['up']['texture']='#engraving'
        for overrides in ({'biome':bad},{'biome':bad2},{'dark_garden_deadwood':stretch},{'dark_garden_gravestone':uncapped},{'effigy_states':states},{'dark_garden_gravestone':side_engraving},{'dark_garden_gravestone':top_engraving}):
            try:contract(root,overrides)
            except AssertionError:mutants+=1
            else:raise AssertionError('Dark malformed fixture was accepted')
    print(f'Dark Garden0255: {CHECKS:,} native palette/shape/atmosphere/care assertions passed; {mutants} malformed fixtures rejected.')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=ROOT);p.add_argument('--self-test',action='store_true');a=p.parse_args();main(a.root.resolve(),a.self_test)
