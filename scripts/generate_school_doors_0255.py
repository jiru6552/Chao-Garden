#!/usr/bin/env python3
"""Smooth 96-section school arches with fitted leaves and rigid front/back art.

Coordinates retain the expanded 21-cell ornate pair ownership; the steel cabinet is
intentionally outside this writer's finite six-role domain.
"""
import json, math
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'src/main/resources/assets/chao_garden'; MODEL=AS/'models/block'; TEX=AS/'textures/block/school025'
ROLES={'principal':('green','#357b49'),'classroom':('blue','#315ca1'),'health':('cyan','#409d91'),'fortune':('purple','#8e3699'),'exit':('green','#397b58'),'market':('wood','#285f68')}
SECTIONS=96; OUTER_RADIUS=24.; INNER_RADIUS=22.5; SPRING=24.; CLEARANCE=.01; SEAM=0.
LEFT=-14.5; RIGHT=30.5; WIDTH=22.5; MARGIN=(23-WIDTH)/2
HINGE_Z=15.; SWING_Z=16.; TIP_OFFSET=16.
def depth(section):return 32. if section=='far_tip' else 16. if section=='tip' else 0.
r=lambda x:round(x,6)
def height(x,radius):return SPRING+math.sqrt(max(0,radius*radius-(x-8)**2))
def profile():
 cuts={LEFT,RIGHT,8-SEAM/2,8+SEAM/2,0.,16.}
 cuts.update(r(8-INNER_RADIUS*math.cos(math.pi*i/SECTIONS)) for i in range(SECTIONS+1))
 cuts=sorted(x for x in cuts if LEFT<=x<=RIGHT)
 return [(a,b,r(min(height(a,INNER_RADIUS),height(b,INNER_RADIUS))-CLEARANCE)) for a,b in zip(cuts,cuts[1:]) if not (a>=8-SEAM/2 and b<=8+SEAM/2)]
PROFILE=profile()
def box(a,b,mat):return {'from':list(a),'to':list(b),'faces':{f:{'texture':'#'+mat,'uv':[0,0,16,16]} for f in ('north','south','west','east','up','down')}}
def write(path,data):path.write_text(json.dumps(data,indent=2)+'\n')
def owned(left,opened,section,segment):
 out=[]
 if (opened and section=='center') or (not opened and section in ('tip','far_tip')):return out
 for physical_left in (True,False):
  if section!='center' and left!=physical_left:continue
  for lo,hi,top in PROFILE:
   x0=max(lo,LEFT if physical_left else 8+SEAM/2);x1=min(hi,8-SEAM/2 if physical_left else RIGHT)
   y1=min(16,top-segment*16)
   if x0>=x1 or y1<=0:continue
   if opened:
    d0=x0-LEFT if physical_left else RIGHT-x1;d1=x1-LEFT if physical_left else RIGHT-x0
    z0=max(0,SWING_Z+d0-depth(section));z1=min(16,SWING_Z+d1-depth(section))
    if z0>=z1:continue
    a=[1.5 if physical_left else 12.5,0,r(z0)];b=[3.5 if physical_left else 14.5,r(y1),r(z1)]
   else:
    offset=0 if section=='center' else -16 if physical_left else 16
    a=[r(max(0,x0-offset)),0,HINGE_Z-1];b=[r(min(16,x1-offset)),r(y1),HINGE_Z+1]
    if a[0]>=b[0]:continue
   # Entirely straight rectangular runs use one member, not dozens of internal seams.
   if out and out[-1][2]==physical_left:
    pa,pb,_=out[-1]
    axis=2 if opened else 0
    if all(pa[k]==a[k] and pb[k]==b[k] for k in range(3) if k!=axis) and (pb[axis]==a[axis] or pa[axis]==b[axis]):
     pa[axis]=min(pa[axis],a[axis]);pb[axis]=max(pb[axis],b[axis]);continue
   out.append((a,b,physical_left))
 return out

def leaf(a,b,left,opened,section,segment):
 e=box(a,b,'edge');v0=(48-segment*16-b[1])/3;v1=(48-segment*16-a[1])/3
 if opened:
  u0=(a[2]+depth(section)-SWING_Z+MARGIN)*16/23;u1=(b[2]+depth(section)-SWING_Z+MARGIN)*16/23
  front='west' if left else 'east';back='east' if left else 'west'
  e['faces'][front]={'texture':'#base','uv':[u0,v0,u1,v1] if left else [u1,v0,u0,v1]}
  e['faces'][back]={'texture':'#back','uv':[16-u1,v0,16-u0,v1] if left else [16-u0,v0,16-u1,v1]}
 else:
  offset=0 if section=='center' else -16 if left else 16
  distance=lambda x:x+offset-LEFT if left else RIGHT-x-offset
  u0=(distance(a[0])+MARGIN)*16/23;u1=(distance(b[0])+MARGIN)*16/23
  e['faces']['south']={'texture':'#base','uv':[u0,v0,u1,v1]}
  e['faces']['north']={'texture':'#back','uv':[16-u1,v0,16-u0,v1]}
 return e

def arch(role):
 elements=[box([-16,-16,-16],[-14.5,8,.2],'base'),box([30.5,-16,-16],[32,8,.2],'base')]
 cuts={-16.,32.,-14.5,30.5,8.}
 cuts.update(r(8-OUTER_RADIUS*math.cos(math.pi*i/SECTIONS)) for i in range(SECTIONS+1))
 cuts.update(r(8-INNER_RADIUS*math.cos(math.pi*i/SECTIONS)) for i in range(SECTIONS+1))
 cuts=sorted(cuts)
 for x0,x1 in zip(cuts,cuts[1:]):
  outer=r(min(height(x0,OUTER_RADIUS),height(x1,OUTER_RADIUS)))
  inner=r(max(height(max(-14.5,min(30.5,x0)),INNER_RADIUS),height(max(-14.5,min(30.5,x1)),INNER_RADIUS))) if x0>=-14.5 and x1<=30.5 else SPRING
  if outer>inner:elements.append(box([x0,inner-16,-16],[x1,outer-16,.2],'base'))
  if outer<48:elements.append(box([x0,outer-16,-16],[x1,32,0],'cream'))
  # The restrained gold inlay follows the same fine outer contour.
  if outer>inner:elements.append(box([x0,max(inner,outer-.25)-16,.21],[x1,outer-16,.35],'gold'))
 return {'chao_door_profile':'curve0255','curve_sections':SECTIONS,'textures':{'base':'chao_garden:block/school025/'+ROLES[role][0],'cream':'chao_garden:block/school025/school_cream_wall','gold':'chao_garden:block/school025/gold','particle':'chao_garden:block/school025/'+ROLES[role][0]},'elements':elements}

def art(role):
 ident='school_door_'+role
 original={side:Image.open(TEX/(ident+side+'.png')).convert('RGBA') for side in ('','_back')}
 for suffix in ('','_back'):
  im=Image.new('RGBA',(368,768),ROLES[role][1]);d=ImageDraw.Draw(im)
  back=bool(suffix)
  def at(distance,y):return ((23-(MARGIN+distance) if back else MARGIN+distance)*16,(48-y)*16)
  inset=.65
  curve=[at(inset+(WIDTH-2*inset)*i/96,height(LEFT+inset+(WIDTH-2*inset)*i/96,INNER_RADIUS)-.95) for i in range(97)]
  pts=[at(inset,.65),at(inset,height(LEFT+inset,INNER_RADIUS)-.95)]+curve+[at(WIDTH-inset,.65),at(inset,.65)]
  d.line(pts,fill='#dcc790',width=5,joint='curve')
  inner=.95;curve=[at(inner+(WIDTH-2*inner)*i/96,height(LEFT+inner+(WIDTH-2*inner)*i/96,INNER_RADIUS)-1.25) for i in range(97)]
  d.line([at(inner,.95)]+curve+[at(WIDTH-inner,.95),at(inner,.95)],fill='#a8cdbc',width=2,joint='curve')
  # Preserve the supplied complete round badge and each physical free-edge handle.
  patches=[(135,248,233,375),(36,382,89,437) if back else (280,382,337,437)]
  if role=='market':patches=[patches[1],(50,574,310,660)]
  for rect in patches:im.paste(original[suffix].crop(rect),rect[:2])
  im.save(TEX/(ident+suffix+'.png'))


def java():
 lines=',\n'.join('      {'+','.join(str(v) for v in row)+'}' for row in PROFILE)
 source='''package com.chaogarden.core;
import java.util.ArrayList;
import java.util.List;
/** Fine curved leaves: twenty-one owning voxels and rigid hinge motion as saved pairs. */
public final class SchoolDoorGeometry0255 {
  private SchoolDoorGeometry0255(){}
  public static final double LEFT_HINGE=-14.5,RIGHT_HINGE=30.5,WIDTH=22.5,ART_MARGIN=.25,SEAM=0,HINGE_DEPTH=15,SWING_DEPTH=16;
  private static final double[][] PROFILE={
%s
  };
  public record Box(double x0,double y0,double z0,double x1,double y1,double z1,boolean left){}
  public static List<Box> boxes(boolean left,boolean open,int section,int segment){
    List<Box> out=new ArrayList<>();
    if((open&&section==1)||(!open&&section>=2))return out;
    for(boolean physicalLeft:new boolean[]{true,false}){
      if(section!=1&&physicalLeft!=left)continue;
      for(double[] stripe:PROFILE){
        double x0=Math.max(stripe[0],physicalLeft?LEFT_HINGE:8+SEAM/2),x1=Math.min(stripe[1],physicalLeft?8-SEAM/2:RIGHT_HINGE);
        double y1=Math.min(16,stripe[2]-segment*16);if(x0>=x1||y1<=0)continue;
        Box next;
        if(open){
          double d0=physicalLeft?x0-LEFT_HINGE:RIGHT_HINGE-x1,d1=physicalLeft?x1-LEFT_HINGE:RIGHT_HINGE-x0;
          double offset=section==3?32:section==2?16:0;
          double z0=Math.max(0,SWING_DEPTH+d0-offset),z1=Math.min(16,SWING_DEPTH+d1-offset);if(z0>=z1)continue;
          next=new Box(physicalLeft?1.5:12.5,0,round(z0),physicalLeft?3.5:14.5,round(y1),round(z1),physicalLeft);
        }else{
          double offset=section==1?0:physicalLeft?-16:16,a=Math.max(0,x0-offset),b=Math.min(16,x1-offset);if(a>=b)continue;
          next=new Box(round(a),0,HINGE_DEPTH-1,round(b),round(y1),HINGE_DEPTH+1,physicalLeft);
        }
        if(!out.isEmpty()){
          var old=out.getLast();
          if(old.left==next.left&&old.y0==next.y0&&old.y1==next.y1&&(open?old.x0==next.x0&&old.x1==next.x1&&(old.z1==next.z0||old.z0==next.z1):old.z0==next.z0&&old.z1==next.z1&&(old.x1==next.x0||old.x0==next.x1))){
            out.set(out.size()-1,new Box(Math.min(old.x0,next.x0),old.y0,Math.min(old.z0,next.z0),Math.max(old.x1,next.x1),old.y1,Math.max(old.z1,next.z1),old.left));continue;
          }
        }
        out.add(next);
      }
    }
    return List.copyOf(out);
  }
  private static double round(double value){return Math.rint(value*1000000)/1000000;}
  public static double heightAtDistance(boolean left,double distance){
    double x=left?LEFT_HINGE+distance:RIGHT_HINGE-distance;
    for(double[] stripe:PROFILE)if(x>=stripe[0]&&x<=stripe[1])return stripe[2];return 0;
  }
}
'''%lines
 (ROOT/'src/main/java/com/chaogarden/core/SchoolDoorGeometry0255.java').write_text(source)

def main():
 java()
 for role in ROLES:
  ident='school_door_'+role;write(MODEL/('school_arch_'+role+'.json'),arch(role))
  for left in (True,False):
   for opened in (False,True):
    for section in ('leaf','center','tip','far_tip'):
     for segment in range(3):
      name=f'{ident}_{"left" if left else "right"}_{"open" if opened else "closed"}_{section}_{segment}'
      m={'chao_door_profile':'curve0255','curve_sections':SECTIONS,'textures':{k:'chao_garden:block/school025/'+ident+suffix for k,suffix in (('base',''),('back','_back'),('edge','_edge'),('particle',''))},'ambientocclusion':False,'elements':[leaf(a,b,l,opened,section,segment) for a,b,l in owned(left,opened,section,segment)]}
      write(MODEL/(name+'.json'),m)
      if left and not opened and section=='leaf' and segment==0:write(MODEL/(ident+'.json'),m)
  art(role)
  states=AS/'blockstates'/(ident+'.json');doc=json.loads(states.read_text())
  for key,value in list(doc['variants'].items()):
   if 'section=tip,' in key:doc['variants'][key.replace('section=tip,','section=far_tip,')]={**value,'model':value['model'].replace('_tip_','_far_tip_')}
  write(states,doc)
 # FAR_TIP is a registered enum value, but cabinets never own it or acquire geometry.
 states=AS/'blockstates/school_tool_cabinet.json';doc=json.loads(states.read_text())
 for key,value in list(doc['variants'].items()):
  if 'section=tip,' in key:doc['variants'][key.replace('section=tip,','section=far_tip,')]={**value,'model':value['model'].replace('_open_tip_','_closed_tip_')}
 write(states,doc)
 print(f'School doors0255: six curved96-section arches;288 edge-seated outward-swinging leaf meshes,1.5px frames,0.01px crown clearance; cabinet geometry unchanged.')
if __name__=='__main__':main()
