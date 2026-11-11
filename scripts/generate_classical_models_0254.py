#!/usr/bin/env python3
"""Native Ionic members and oriented Hero cornice; run after both base asset writers.

Geometry study: Metropolitan Museum of Art, Greek Art (Ionic order: volutes,
abacus, fluted shaft and molded base). No photographic/game pixels are used.
https://resources.metmuseum.org/resources/metpublications/pdf/Greek_Art_From_Prehistoric_to_Classical.pdf
"""
from pathlib import Path
import json
import math
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
COLUMNS=('school_hall_column','school_twisted_column')
PARTS=('base','shaft','capital')
ROTATIONS={'north':180,'east':270,'south':0,'west':90}

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n')

def member(part):
    boxes=[]
    def add(a,b,texture='stone'):
        boxes.append((tuple(round(v,5) for v in a),tuple(round(v,5) for v in b),texture))
    def shaft(y0,y1):
        # Inset the complete fluted profile by half a model pixel on each side.
        # Its former 3.5..12.5 ribs overhung the base's 3.75..12.25 neck. Scaling
        # about the centre retains the same real flute recess depth/proportions.
        def flute(a,b,texture='stone'):
            def inset(v):return (8+(v[0]-8)*8/9,v[1],8+(v[2]-8)*8/9)
            add(inset(a),inset(b),texture)
        flute((4.5,y0,4),(11.5,y1,12),'flute');flute((4,y0,4.5),(12,y1,11.5),'flute')
        # Twelve projecting fillets leave genuine recessed vertical flute gaps.
        for q in (5.1,7.4,9.7):
            flute((q,y0,3.5),(q+1.2,y1,4.02));flute((q,y0,11.98),(q+1.2,y1,12.5))
            flute((3.5,y0,q),(4.02,y1,q+1.2));flute((11.98,y0,q),(12.5,y1,q+1.2))
    if part=='base':
        add((1.5,0,1.5),(14.5,1.5,14.5));add((2.5,1.5,2.5),(13.5,3,13.5))
        add((3.25,3,3.25),(12.75,4,12.75));add((3.75,4,3.75),(12.25,5.5,12.25));shaft(5.5,16)
    elif part=='shaft':shaft(0,16)
    else:
        shaft(0,4.5);add((3.25,4.5,3.25),(12.75,6,12.75))
        add((2.5,6,4),(13.5,9,12));add((5.5,9,4),(10.5,13.4,12));add((3,13.4,3),(13,14,13))
        # Paired disk volutes have actual stepped round silhouettes and recessed
        # contrasting spirals on BOTH viewing faces; no flat textured box cap.
        for cx in (3,13):
            cy=10.5;r=2.8
            for step in range(14):
                ya=cy-r+step*.4;yb=ya+.4;half=math.sqrt(max(0,r*r-((ya+yb)/2-cy)**2))
                add((cx-half,ya,3),(cx+half,yb,13))
            for i in range(46):
                t=i/45*math.pi*3.25;u=(i+1)/45*math.pi*3.25
                ra=2.35-2.05*i/46;rb=2.35-2.05*(i+1)/46
                xa=cx+math.cos(t)*ra;ya=cy+math.sin(t)*ra
                xb=cx+math.cos(u)*rb;yb=cy+math.sin(u)*rb
                for za,zb in ((2.88,3.001),(12.999,13.12)):
                    add((min(xa,xb)-.17,min(ya,yb)-.17,za),(max(xa,xb)+.17,max(ya,yb)+.17,zb),'groove')
        # The reduced abacus still touches the ceiling at Y16. The lower plate
        # starts above the 13.3px volute crown instead of masking its upper arc.
        add((1,14,1),(15,16,15))
    return boxes

def element(a,b,texture):
    x,y,z=(b[i]-a[i] for i in range(3))
    faces={}
    for face,u,v in (('north',x,y),('south',x,y),('east',z,y),('west',z,y),('up',x,z),('down',x,z)):
        faces[face]={'uv':[0,0,round(u,5),round(v,5)],'texture':'#'+texture}
    return {'from':a,'to':b,'faces':faces}

def columns():
    for identifier in COLUMNS:
        stone='cream' if identifier=='school_hall_column' else 'blue'
        groove='ionic_groove_cream' if stone=='cream' else 'ionic_groove_blue'
        for part in PARTS:
            write(ASSETS/f'models/block/{identifier}_{part}.json',{
                'ambientocclusion':True,'textures':{'particle':f'chao_garden:block/school025/{stone}',
                    'stone':f'chao_garden:block/school025/{stone}','groove':f'chao_garden:block/school025/{groove}',
                    'flute':f'chao_garden:block/school025/ionic_flute_{stone}'},
                'elements':[element(a,b,texture) for a,b,texture in member(part)]})
        write(ASSETS/f'models/block/{identifier}.json',{'parent':f'chao_garden:block/{identifier}_shaft'})
        variants={f'facing={face},part={part}':{'model':f'chao_garden:block/{identifier}_{part}',**({'y':rotation} if rotation else {})}
            for face,rotation in ROTATIONS.items() for part in PARTS}
        write(ASSETS/f'blockstates/{identifier}.json',{'variants':variants})
    for name,color in (('ionic_groove_cream',(159,151,126,255)),('ionic_groove_blue',(24,70,94,255)),
                       ('ionic_flute_cream',(230,216,173,255)),('ionic_flute_blue',(30,96,144,255))):
        image=Image.new('RGBA',(16,16),color)
        for x in range(16):image.putpixel((x,0),tuple(min(255,c+5) for c in color[:3])+(255,))
        image.save(ASSETS/f'textures/block/school025/{name}.png')
    # The same complete native element envelope supplies picking and collision,
    # so protruding scrolls remain selectable and no invisible square pier stays.
    arrays=[]
    for part in PARTS:
        rows=',\n'.join('      {'+','.join(str(v) for v in a+b)+'}' for a,b,_ in member(part))
        arrays.append('    new double[][] {\n'+rows+'\n    }')
    source='''package com.chaogarden.kindergarten;

import java.util.ArrayList;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated from the actual authored Ionic model elements by generate_classical_models_0254.py. */
final class IonicColumnMesh0254 {
  private static final double[][][] BOXES={
'''+',\n'.join(arrays)+'''
  };
  private static final VoxelShape[][] SHAPES=new VoxelShape[3][4];
  static {
    for(int part=0;part<3;part++)for(int turn=0;turn<4;turn++){
      var members=new ArrayList<VoxelShape>();
      for(double[] box:BOXES[part]){
        double x0=box[0],z0=box[2],x1=box[3],z1=box[5];
        for(int i=0;i<turn;i++){double a=16-z1,b=x0,c=16-z0,d=x1;x0=a;z0=b;x1=c;z1=d;}
        members.add(Shapes.box(x0/16,box[1]/16,z0/16,x1/16,box[4]/16,z1/16));
      }
      // Shapes.or optimizes after every append. A balanced native union avoids
      // repeatedly rebuilding the complete detailed capital coordinate grid.
      while(members.size()>1){
        var next=new ArrayList<VoxelShape>((members.size()+1)/2);
        for(int i=0;i<members.size();i+=2)next.add(i+1<members.size()
            ?Shapes.joinUnoptimized(members.get(i),members.get(i+1),BooleanOp.OR):members.get(i));
        members=next;
      }
      SHAPES[part][turn]=members.get(0).optimize();
    }
  }
  static VoxelShape shape(KindergartenIonicColumn0254.Part part,Direction facing){
    int turns=switch(facing){case SOUTH->0;case WEST->1;case NORTH->2;case EAST->3;default->throw new IllegalArgumentException("Horizontal column facing required");};
    return SHAPES[part.ordinal()][turns];
  }
  private IonicColumnMesh0254(){}
}
'''
    (ROOT/'src/main/java/com/chaogarden/kindergarten/IonicColumnMesh0254.java').write_text(source)

def cornice():
    image=Image.open(ASSETS/'textures/block/hero0253/hero_marble_trim.png').convert('RGBA')
    # A quarter-annular soffit turns the six molding stripes continuously from
    # its east neighbor into its south neighbor (canonical NW corner).
    corner=Image.new('RGBA',(64,64),'#bcb1d6');profile=[image.getpixel((0,y)) for y in range(64)]
    for y in range(64):
        for x in range(64):
            r=math.hypot(63-x,63-y);v=63-r
            if 0<=v<=63:corner.putpixel((x,y),profile[round(v)])
    d=ImageDraw.Draw(corner);cx=cy=63-31/math.sqrt(2)
    for i in range(8):
        angle=i*math.pi/4;px=cx+9*math.cos(angle);py=cy+9*math.sin(angle)
        d.ellipse((px-4,py-4,px+4,py+4),fill='#e3ddef',outline='#a496be')
    d.ellipse((cx-4,cy-4,cx+4,cy+4),fill='#f5edc7',outline='#b7a874')
    corner.save(ASSETS/'textures/block/hero0253/hero_marble_trim_corner.png')
    for is_corner in (False,True):
        suffix='_corner' if is_corner else ''
        faces={face:{'uv':[0,0,16,16],'texture':'#'+('soffit' if face=='down' else 'top' if face=='up' else 'side')}
            for face in ('up','down','north','south','east','west')}
        # Vanilla DOWN vertices map default V toward -Z. Explicitly reverse V
        # so the corner's image east/south joins meet world east/south neighbors.
        faces['down']['uv']=[0,16,16,0]
        write(ASSETS/f'models/block/hero_marble_trim{suffix}.json',{
            'parent':'minecraft:block/block','textures':{'particle':'chao_garden:block/hero0253/hero_marble_trim',
                'side':'chao_garden:block/hero0253/hero_marble_trim','top':'chao_garden:block/hero0253/hero_marble',
                'soffit':f'chao_garden:block/hero0253/hero_marble_trim{suffix}'},
            'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
    write(ASSETS/'blockstates/hero_marble_trim.json',{'variants':{
        f'corner={str(corner).lower()},facing={face}':{'model':'chao_garden:block/hero_marble_trim'+('_corner' if corner else ''),**({'y':rotation} if rotation else {})}
        for corner in (False,True) for face,rotation in {'north':0,'east':90,'south':180,'west':270}.items()}})

if __name__=='__main__':
    columns();cornice()
    print('Ionic columns and eight native oriented Hero cornice states generated; existing BlockItem IDs preserved.')
