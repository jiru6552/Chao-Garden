#!/usr/bin/env python3
"""Build the garden terminal as the SA2 PC Chao-shaped transporter machine.

Reference: https://chao-island.com/info-center/misc/chao-transporter.html
The model and native pixel textures are authored here for Minecraft blocks.
The machine occupies eight native cells; its legacy interaction and registry ID stay.
"""
from pathlib import Path
import io
import copy
import math
import json
import os
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
SCALE = .6


def put(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_name(path.name + '.writing')
    with tmp.open('wb') as f:
        f.write(data); f.flush(); os.fsync(f.fileno())
    os.replace(tmp, path)


def write_json(path, value):
    put(path, (json.dumps(value, indent=2) + '\n').encode())


def picture(name, painter, size=16):
    im = Image.new('RGBA', (size, size), (255, 255, 255, 255))
    painter(ImageDraw.Draw(im), size)
    data = io.BytesIO(); im.save(data, format='PNG')
    put(ASSETS/'textures/block'/f'transporter_{name}.png', data.getvalue())


def shell(d, n):
    d.rectangle((0,0,n-1,n-1), fill='#83ccd6')
    d.line((0,0,n-1,0), fill='#c5ece7')
    d.line((0,1,0,n-1), fill='#9bdcde')
    d.line((0,n-1,n-1,n-1), fill='#65b4c5')
    d.line((n-1,0,n-1,n-1), fill='#72c2ce')
    d.rectangle((3,4,5,5), fill='#91d4da')
    d.rectangle((10,10,12,11), fill='#78c5d0')


def ivory(d, n):
    d.rectangle((0,0,n-1,n-1), fill='#d6e5df')
    d.line((0,0,n-1,0), fill='#f4f7e9')
    d.line((0,n-1,n-1,n-1), fill='#a5c2c5')
    d.line((n-1,0,n-1,n-1), fill='#bad4d3')


def red(d, n):
    d.rectangle((0,0,n-1,n-1), fill='#b51f23')
    d.line((0,0,n-1,0), fill='#ed4d38')
    d.line((0,1,n-1,1), fill='#d52c28')
    d.line((0,n-1,n-1,n-1), fill='#761e29')


def gold(d, n):
    d.rectangle((0,0,n-1,n-1), fill='#ebbd29')
    for y in range(0,n,3):
        d.line((0,y,n-1,y), fill='#c77f12')
        if y+1<n:d.line((0,y+1,n-1,y+1), fill='#ffdf4c')


def orb(d, n):
    d.rectangle((0,0,n-1,n-1), fill='#e7c93c')
    d.rectangle((1,1,n-3,n-3), fill='#f4d950')
    d.rectangle((2,2,5,4), fill='#fff08a')
    d.line((0,n-1,n-1,n-1), fill='#bea326')


def blue(d, n):
    d.rectangle((0,0,n-1,n-1), fill='#168fbf')
    d.rectangle((2,2,n-4,n-4), fill='#1aa6d6')
    d.rectangle((3,2,5,4), fill='#55cae6')
    d.line((0,n-1,n-1,n-1), fill='#086a9c')


def neck(d, n):
    d.rectangle((0,0,n-1,n-1), fill='#b6c8cc')
    for y,c in ((2,'#b59ec7'),(5,'#95cdd8'),(8,'#c6b3d4'),(11,'#95cdd8'),(14,'#d4e2dc')):
        d.line((0,y,n-1,y), fill=c)


GLYPHS = {
 'C':('111','100','100','100','111'), 'H':('101','101','111','101','101'),
 'A':('010','101','111','101','101'), 'O':('111','101','101','101','111'),
 'G':('111','100','101','101','111'), 'R':('110','101','110','101','101'),
 'D':('110','101','101','101','110'), 'E':('111','100','110','100','111'),
 'N':('101','111','111','111','101'),
}


def label(d, word, x, y, color, scale=1):
    for char in word:
        for py,row in enumerate(GLYPHS[char]):
            for px,mark in enumerate(row):
                if mark=='1':d.rectangle((x+px*scale,y+py*scale,x+(px+1)*scale-1,y+(py+1)*scale-1),fill=color)
        x += 4*scale


def screen(d, n):
    d.rectangle((0,0,n-1,n-1), fill='#172d32')
    d.rectangle((1,1,n-2,n-2), fill='#102329')
    d.line((2,2,n-3,2), fill='#31545b')
    label(d,'CHAO',1,6,'#53d8dc',2)
    label(d,'GARDEN',4,22,'#c6e3d8')
    d.line((4,29,27,29), fill='#537b7d')


def point(p):
    return [round(8+(p[0]-8)*SCALE,4), round(p[1]*SCALE,4), round(8+(p[2]-8)*SCALE,4)]


def box(a,b,material,name='',rotation=None):
    a,b=point(a),point(b)
    dims=[b[i]-a[i] for i in range(3)]
    sizes={'north':(dims[0],dims[1]),'south':(dims[0],dims[1]),
           'east':(dims[2],dims[1]),'west':(dims[2],dims[1]),
           'up':(dims[0],dims[2]),'down':(dims[0],dims[2])}
    obj={'from':a,'to':b,'faces':{s:{'texture':'#'+material,'uv':[0,0,round(w,4),round(h,4)]} for s,(w,h) in sizes.items()}}
    if name:obj['name']=name
    if rotation:obj['rotation']={'origin':point(rotation[0]),'axis':rotation[1],'angle':rotation[2],'rescale':False}
    return obj


def model():
    p=[]
    # Wide, low red-edged loading platform, with clipped block corners.
    p += [box([-1,0,-3],[17,.65,17],'red','red plinth edge'),
          box([-2,.65,-2],[18,1.3,16],'red','wide red plinth edge'),
          box([-1,1.3,-2],[17,2,16],'shell','loading platform'),
          box([0,1.3,-3],[16,2,-2],'shell','front clipped platform'),
          box([0,1.3,16],[16,2,17],'shell','rear clipped platform')]
    # Lower equipment housing and golden striped loading ramp.
    p += [box([1.75,2,8],[5.5,6.5,14],'shell','left lower housing'),
          box([10.5,2,8],[14.25,6.5,14],'shell','right lower housing'),
          box([2.25,6.5,8.5],[5.5,7.5,14],'ivory','left pale shoulder'),
          box([10.5,6.5,8.5],[13.75,7.5,14],'ivory','right pale shoulder'),
          box([5.5,5.6,10],[10.5,7.5,14],'ivory','back bridge'),
          box([5.5,2.1,3.8],[10.5,2.7,14],'gold','striped loading ramp',([8,2.1,3.8],'x',-22.5)),
          box([2.4,3.8,7.7],[4.6,5.9,8.2],'red','red side control')]
    # Pixel-stepped round red receiving pad at the ramp's foot.
    p += [box([5.2,2.02,-.5],[10.8,2.75,.5],'red','pad front'),
          box([4.4,2.02,.5],[11.6,2.75,3.7],'red','pad middle'),
          box([5.2,2.02,3.7],[10.8,2.75,4.7],'red','pad rear')]
    p += [box([7,7.5,10.5],[9,10.5,12.5],'neck','striped machine neck')]
    # Chao-shaped cyan monitor casing. Each contour step is a real solid block.
    p += [box([2,10,7.2],[14,12,15],'shell','rounded lower monitor'),
          box([1,12,6.5],[15,16.5,15.5],'shell','wide monitor cheeks'),
          box([1.8,16.5,7.1],[14.2,19.5,15],'shell','upper monitor'),
          box([3.1,19.5,7.8],[12.9,21,14.5],'shell','upper monitor taper'),
          box([4.5,21,8.5],[11.5,22,13.9],'shell','monitor crown'),
          box([6,22,9.1],[10,23.2,13],'shell','chao tuft')]
    # A red-framed black display and blue cheek controls are the PC design's key cues.
    p += [box([3.8,11.8,6.12],[12.2,19.1,6.42],'red','red screen surround'),
          box([4.3,12.35,6.03],[11.7,18.55,6.12],'screen','garden display'),
          box([1.6,13,6.0],[3.55,15.05,6.5],'blue','left cyan button'),
          box([12.45,13,6.0],[14.4,15.05,6.5],'blue','right cyan button')]
    display=p[-3]
    display['faces']['north']['uv']=[0,0,16,16]
    # Stepped golden marker floats on a short hidden mount above the head.
    p += [box([7.2,23.2,10.2],[8.8,23.9,11.8],'shell','orb mount'),
          box([7.1,23.9,10.1],[8.9,24.4,11.9],'orb','orb lower'),
          box([6.7,24.4,9.7],[9.3,25.8,12.3],'orb','orb middle'),
          box([7.1,25.8,10.1],[8.9,26.3,11.9],'orb','orb upper')]
    textures={name:'chao_garden:block/transporter_'+name for name in ('shell','ivory','red','gold','orb','blue','neck','screen')}
    return {'parent':'minecraft:block/block','ambientocclusion':True,'textures':{**textures,'particle':textures['shell']},
            'elements':p,'display':{'gui':{'rotation':[25,225,0],'translation':[0,.2,0],'scale':[.95,.95,.95]},
                                  'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.5,.5,.5]},
                                  'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[.8,.8,.8]}}}


def enlarged(legacy):
    data=copy.deepcopy(legacy)
    elements=[]
    def transform(p):
        # The old authored machine spans x2..14,z1.4..13.4,y0..15.78.
        return [round((p[0]-2)*32/12,6),round(p[1]*32/15.78,6),round((p[2]-1.4)*32/12,6)]
    for e in data['elements']:
        if 'rotation' in e:
            # Native stair treads split cleanly at cell planes; no rotated face
            # crosses a part boundary or disagrees with its physical selection.
            for i in range(8):
                z0=3.8+i*10.2/8;z1=3.8+(i+1)*10.2/8
                y=2.1+(z0-3.8)*math.sin(math.pi/8)
                step=box([5.5,y,z0],[10.5,y+.55,z1],'gold','loading ramp tread '+str(i))
                step['from']=transform(step['from']);step['to']=transform(step['to']);elements.append(step)
        else:
            e['from']=transform(e['from']);e['to']=transform(e['to']);elements.append(e)
    data['elements']=elements
    return data


def slice_model(data,part):
    origin=[16*(part&1),16*((part>>2)&1),16*((part>>1)&1)]
    elements=[]
    # These projections match Minecraft element face UV conventions. Clipped
    # screen UVs keep one readable label across all native part boundaries.
    axes={'north':(0,1,True,True),'south':(0,1,False,True),
          'west':(2,1,False,True),'east':(2,1,True,True),
          'up':(0,2,False,False),'down':(0,2,False,True)}
    side_axis={'west':(0,0),'east':(0,1),'down':(1,0),'up':(1,1),'north':(2,0),'south':(2,1)}
    for e in data['elements']:
        a=e['from'];b=e['to'];lo=[max(a[i],origin[i]) for i in range(3)];hi=[min(b[i],origin[i]+16) for i in range(3)]
        if any(hi[i]-lo[i]<.000001 for i in range(3)):continue
        faces={}
        for side,face in e['faces'].items():
            axis,end=side_axis[side]
            if abs((lo if end==0 else hi)[axis]-(a if end==0 else b)[axis])>.000001:continue
            u,v,flip_u,flip_v=axes[side]
            ua,ub=(b[u]-hi[u],b[u]-lo[u]) if flip_u else (lo[u]-a[u],hi[u]-a[u])
            va,vb=(b[v]-hi[v],b[v]-lo[v]) if flip_v else (lo[v]-a[v],hi[v]-a[v])
            # Every cuboid uses one complete texture. Fractional cropping is
            # continuous across cells and stays inside its atlas sprite.
            faces[side]={'texture':face['texture'],'uv':[round(ua/(b[u]-a[u])*16,6),round(va/(b[v]-a[v])*16,6),round(ub/(b[u]-a[u])*16,6),round(vb/(b[v]-a[v])*16,6)]}
        elements.append({'name':e.get('name','part'),'from':[round(lo[i]-origin[i],6) for i in range(3)],'to':[round(hi[i]-origin[i],6) for i in range(3)],'faces':faces})
    return {'parent':'minecraft:block/block','ambientocclusion':True,'textures':data['textures'],'elements':elements}


def geometry_java(data):
    rows=[]
    for e in data['elements']:
        rows.append('      {'+', '.join(str(v) for v in e['from']+e['to'])+'}')
    source='package com.chaogarden.activities;\n\nimport net.minecraft.core.Direction;\nimport net.minecraft.world.level.block.Block;\nimport net.minecraft.world.phys.shapes.Shapes;\nimport net.minecraft.world.phys.shapes.VoxelShape;\n\n/** Generated by generate_transporter_assets.py; the exact boxes used by all eight meshes. */\npublic final class GardenDeviceGeometry0254 {\n  private static final double[][] BOXES = {\n'+',\n'.join(rows)+'\n  };\n  private static final VoxelShape[][] SHAPES = new VoxelShape[4][8];\n  static {\n    for(int turn=0;turn<4;turn++) for(int part=0;part<8;part++) {\n      double ox=(part&1)*16,oy=((part>>2)&1)*16,oz=((part>>1)&1)*16;\n      VoxelShape shape=Shapes.empty();\n      for(double[] source:BOXES) {\n        double x0=source[0],y0=source[1],z0=source[2],x1=source[3],y1=source[4],z1=source[5];\n        for(int i=0;i<turn;i++){double old0=x0,old1=x1;x0=32-z1;x1=32-z0;z0=old0;z1=old1;}\n        x0=Math.max(x0,ox);y0=Math.max(y0,oy);z0=Math.max(z0,oz);\n        x1=Math.min(x1,ox+16);y1=Math.min(y1,oy+16);z1=Math.min(z1,oz+16);\n        if(x1>x0&&y1>y0&&z1>z0) shape=Shapes.or(shape,Block.box(x0-ox,y0-oy,z0-oz,x1-ox,y1-oy,z1-oz));\n      }\n      SHAPES[turn][part]=shape.optimize();\n    }\n  }\n  public static VoxelShape shape(Direction facing,int part) {\n    int turn=switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};\n    return SHAPES[turn][part];\n  }\n  private GardenDeviceGeometry0254() {}\n}\n'
    put(ROOT/'src/main/java/com/chaogarden/activities/GardenDeviceGeometry0254.java',source.encode())


def main():
    for name,fn in (('shell',shell),('ivory',ivory),('red',red),('gold',gold),('orb',orb),('blue',blue),('neck',neck)):
        picture(name,fn)
    picture('screen',screen,32)
    old=model();data=enlarged(old)
    write_json(ASSETS/'models/block/garden_block_legacy.json',old)
    item=copy.deepcopy(data)
    for e in item['elements']:
        e['from']=[v/2 for v in e['from']];e['to']=[v/2 for v in e['to']]
    write_json(ASSETS/'models/block/garden_block.json',item)
    write_json(ASSETS/'models/item/garden_block.json',{'parent':'chao_garden:block/garden_block'})
    for part in range(8):write_json(ASSETS/'models/block'/f'garden_block_part_{part}.json',slice_model(data,part))
    variants={}
    for turn,facing in enumerate(('north','east','south','west')):
        for part in range(9):
            model_part=part
            if part<8:
                x,z=part&1,(part>>1)&1
                for i in range(turn):x,z=z,1-x
                model_part=x+2*z+(part&4)
            variant={'model':'chao_garden:block/'+('garden_block_legacy' if part==8 else f'garden_block_part_{model_part}')}
            if turn:variant['y']=turn*90
            variants[f'facing={facing},part={part}']=variant
    write_json(ASSETS/'blockstates/garden_block.json',{'variants':variants})
    # One native item per complete provider. Child parts never duplicate drops.
    write_json(ROOT/'src/main/resources/data/chao_garden/loot_table/blocks/garden_block.json',{
        'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'},
        {'condition':'minecraft:any_of','terms':[{'condition':'minecraft:block_state_property','block':'chao_garden:garden_block','properties':{'part':str(p)}} for p in (0,8)]}],
        'entries':[{'type':'minecraft:item','name':'chao_garden:garden_block'}]}]})
    geometry_java(data)
    print('Generated eight-cell 2x2x2 transporter, legacy model, inventory model, native loot and matched collision boxes.')


if __name__=='__main__':main()
