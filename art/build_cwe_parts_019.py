#!/usr/bin/env python3
"""Authored animal attachments, fitted to all 20 untouched supplied skeletons."""
import copy,json
from PIL import Image
from build_forms_019 import MODEL,ASSET,KEYS,source,head_bounds,box,add_bone,write
PARTS={21:(0,5,7),22:(2,5,7),23:(0,4,6),24:(0,1,4,5,6),25:(0,1,4,6),26:(0,1,4,6),27:(0,1,4,6),28:(0,1,2,4,6),29:(0,4,6),30:(4,6,7)}
# Bee has feet, rather than arms; keep this table identical to core's slot map.
PARTS[21]=(4,5,7)
NAMES=('bee','ladybug','scorpion','deer','elephant','kangaroo','koala','lion','mole','swallow')
COLORS=((237,187,45),(210,51,55),(115,69,129),(150,99,60),(147,155,172),(185,123,76),(144,157,168),(202,151,67),(116,87,105),(56,91,155))

def make(key):
 original=source(key);d=copy.deepcopy(json.loads((MODEL/'overlays'/f'chao_{key}.json').read_text()))
 d['bones']=[b for b in d['bones'] if not(b['name'].startswith(('animal_','variant_','chaos_')) or b['name']=='flame')]
 for b in d['bones']:b['meshes']=[]
 d['texture_width']=32;d['texture_height']=32;d['materials']={}
 _,top,bottom=head_bounds(original);sidewidth=3.5
 def mesh(n,lo,hi,animal,tone=0):return box(n,lo,hi,[animal-21+.5,tone+.5],'animal_'+str(animal))
 def group(slot,animal,parent,meshes,suffix=''):
  add_bone(d,f'animal_{slot}_{animal}{suffix}',parent,meshes)
 for animal,slots in PARTS.items():
  for slot in slots:
   if slot==0:
    for side,label in ((1,'left'),(-1,'right')):
     lo=[0 if side>0 else -2.4,-.1,-.7];hi=[2.4 if side>0 else 0,1.25,.7]
     ms=[mesh('arm',lo,hi,animal)]
     if animal in (23,29):
      for i in range(3):
       x=side*(1.5+i*.37);ms.append(mesh('claw',[min(x,x+side*.5),.1,-1.45],[max(x,x+side*.5),.9,-.45],animal,2))
     elif animal==24:ms.append(mesh('hoof',[1.55 if side>0 else -2.5,-.15,-.74],[2.5 if side>0 else -1.55,1.35,.74],animal,2))
     else:ms.append(mesh('paw',[1.55 if side>0 else -2.5,-.15,-.75],[2.5 if side>0 else -1.55,1.35,.75],animal,1))
     group(slot,animal,'arm_'+label,ms,'_'+label)
   elif slot==4:
    for side,label in ((1,'left'),(-1,'right')):
     anchor='foot_'+label
     source_names={b['name'] for b in original['bones']}
     if label=='right' and anchor not in source_names and 'feet_bone_right' in source_names:anchor='feet_bone_right'
     # Keep foot roots on the source's exact joint. Large kangaroo/swallow
     # toes extend forward (negative Z), never backward behind the heel.
     x0,x1=(0,2.1) if side>0 else (-2.1,0)
     length=5.1 if animal==26 else 4.35 if animal in (21,30) else 4
     ms=[mesh('foot',[x0,0,-length],[x1,2,0],animal)]
     if animal==21:
      for z in (-3.5,-1.8):ms.append(mesh('bee_band',[x0-.03,-.03,z],[x1+.03,2.03,z+.55],animal,2))
     elif animal in (24,):ms.append(mesh('hoof',[x0-.02,.3,-length-.03],[x1+.02,2.03,-length+1.4],animal,2))
     elif animal in (23,29,30):
      for i in range(3):
       x=x0+.2+i*.62;ms.append(mesh('toe',[x,.6,-length-.6],[x+.4,1.5,-length+.1],animal,2 if animal!=30 else 1))
     group(slot,animal,anchor,ms,'_'+label)
   elif slot==1:
    ms=[]
    for side in (-1,1):
     cx=side*3.2
     if animal==25:
      lo=[cx-1.6,top+.45,-.2];hi=[cx+1.6,bottom-1.1,.9]
     elif animal==26:
      lo=[cx-.7,top-3.8,.15];hi=[cx+.7,top+.5,1.3]
     elif animal==27:
      lo=[cx-1.25,top-1.05,-.3];hi=[cx+1.25,top+1.45,1.0]
     else:
      lo=[cx-.8,top-.9,-.2];hi=[cx+.8,top+.8,.9]
     ms.append(mesh('outer_ear',lo,hi,animal));ms.append(mesh('inner_ear',[lo[0]+.3,lo[1]+.3,lo[2]-.05],[hi[0]-.3,hi[1]-.3,lo[2]+.02],animal,1))
    group(slot,animal,'skull',ms)
   elif slot==5:
    ms=[]
    for side in (-1,1):
     if animal==24:
      for i in range(3):
       x=side*(1.7+i*.42);ms.append(mesh('antler_stem',[x-.22,top-1.1-i*.9,.3],[x+.22,top+.3-i*.9,.95],animal,2))
       if i:ms.append(mesh('antler_tine',[min(x,x+side*.85)-.15,top-.7-i*.9,.3],[max(x,x+side*.85)+.15,top-.35-i*.9,.85],animal,2))
     else:
      x=side*1.7;ms.extend([mesh('antenna',[x-.18,top-2,.3],[x+.18,top+.1,.7],animal,2),mesh('antenna_tip',[x-.4,top-2.5,.1],[x+.4,top-1.8,.9],animal,0)])
    group(slot,animal,'skull',ms)
   elif slot==2:
    ms=[]
    if animal==22:
     ms=[mesh('cap',[-3.6,top-.45,-2.5],[3.6,top+.25,2.6],animal)]
     for x,z in ((-2,-1),(1.5,-.8),(-.4,1.2)):ms.append(mesh('cap_spot',[x-.45,top-.5,z-.45],[x+.45,top-.44,z+.45],animal,2))
    else:
     for side in (-1,1):
      for i in range(3):
       x=side*(3.0+i*.35);ms.append(mesh('mane',[x-.55,top+i*1.15,-.6],[x+.55,top+1.8+i*1.15,1.4],animal,1))
     ms.append(mesh('forelock',[-1.1,top-.9,-.9],[1.1,top+.2,1.5],animal,1))
    group(slot,animal,'skull',ms)
   elif slot==6:
    ms=[]
    if animal==23:
     for i in range(5):
      z=.3+i*.72;y=-max(0,i-1)*.65;ms.append(mesh('stinger_segment',[-.48,y-.45,z],[.48,y+.45,z+1.0],animal))
     ms.append(mesh('stinger',[-.3,-2.65,3.55],[.3,-1.7,4.2],animal,2))
    elif animal==30:
     for side in (-1,1):
      for i in range(3):
       x=side*(.4+i*.55);ms.append(mesh('forked_tail',[x-.3,-.25,.2+i*.8],[x+.3,.3,1.5+i*.8],animal))
    else:
     length={24:1.4,25:2.2,26:4.5,27:1.0,28:3.4,29:1.7}[animal]
     ms=[mesh('tail',[-.45,-.4,0],[.45,.5,length],animal)]
     if animal==28:ms.append(mesh('tail_tuft',[-.75,-.65,length-.5],[.75,.8,length+.65],animal,1))
    group(slot,animal,'tail',ms)
   elif slot==7:
    for side,label in ((-1,'left'),(1,'right')):
     ms=[]
     for i in range(3 if animal==30 else 2):
      cx=side*(1.0+i*.8);cy=-.25+i*.35
      ms.append(mesh('wing_feather',[cx-.65,cy-.6,.05],[cx+.65,cy+.65,.35],animal,1 if animal==21 else 0))
     if animal==22:
      for i in range(3):
       cx=side*(.8+i*.6);ms.append(mesh('wing_spot',[cx-.22,-.1+i*.22,.37],[cx+.22,.35+i*.22,.42],animal,2))
     group(slot,animal,'wing_'+label,ms,'_'+label)
 write(MODEL/'cwe_parts'/f'chao_{key}.json',d)

def main():
 im=Image.new('RGBA',(32,32))
 for i,color in enumerate(COLORS):
  for tone,c in enumerate((color,tuple(min(255,int(x*.65+80)) for x in color),(43,36,47))):im.putpixel((i,tone),(*c,255))
 p=ASSET/'textures/entity/chao/cwe_parts.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p,optimize=True)
 for key in KEYS:
  make(key)
  data=source(key);_,top,_=head_bounds(data);meshes=[]
  for tier,(w,low,high) in enumerate(((2.3,-.35,.08),(1.7,-1.5,-.35),(1.1,-2.5,-1.5),(.5,-3.2,-2.5))):
   meshes.append(box('paper_hat_tier',[-w,top+low,-w+.3],[w,top+high,w+.3],[1.5,.5] if tier%2 else [.5,.5],'party'))
  hat={'texture_width':32,'texture_height':32,'bones':[{'name':'party_hat','parent':None,'pivot':[0,0,0],'meshes':meshes}]}
  write(MODEL/'party_hats'/f'chao_{key}.json',hat)
 write(ASSET/'cwe_animal_parts_019.json',{'animals':dict(zip(range(21,31),NAMES)),'slots':PARTS,'source_layouts':list(KEYS),'authorship':'New cuboid adaptations; source SADX slots preserved, bee/ladybug/scorpion parts authored.'})
 print('CWE parts019: 10 animals, 37 slot sets, 20 source skeleton fits.')
if __name__=='__main__':main()
