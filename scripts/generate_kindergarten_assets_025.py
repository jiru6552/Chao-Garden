#!/usr/bin/env python3
"""Native, authored Kindergarten scenery: seamless fabrics, painted walls and sculpted furnishings."""
from pathlib import Path
import json,math,random,re
from copy import deepcopy
from PIL import Image,ImageDraw,ImageFont,ImageFilter
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';AS=RES/'assets/chao_garden';TEX=AS/'textures/block/school025';MODEL=AS/'models/block';TEX.mkdir(parents=True,exist_ok=True);MODEL.mkdir(parents=True,exist_ok=True)
source=(ROOT/'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text()
entries=[]
for kind,body in re.findall(r'add\(Kind\.([A-Z]+),(.*?)\);',source,re.S):
 for id in re.findall(r'"(school_[a-z_]+)"',body):entries.append((id,kind))
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def font(n):
 try:return ImageFont.truetype(FONT,n)
 except OSError:return ImageFont.load_default()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n')
def save(name,im):im.save(TEX/(name+'.png'),optimize=True)
def plain(name,color):save(name,Image.new('RGBA',(64,64),color))
for n,c in {'wood':'#6b3025','red':'#aa2134','blue':'#21699d','green':'#4fae76','purple':'#7b278f','gold':'#e6bf55','cream':'#f8e9ba','white':'#eef7e8','pink':'#ed5a9b','black':'#172337','yellow':'#f4d238','cyan':'#42ccdf','stone':'#8d989b'}.items():plain(n,c)
# Low-contrast painted textures, with exact edge repetition.
for n,color in [('school_cream_wall',(240,233,208)),('school_clapboard',(79,160,205)),('school_red_shingle',(177,49,53)),('school_wainscot',(133,78,48))]:
 im=Image.new('RGBA',(128,128),color);d=ImageDraw.Draw(im);rng=random.Random(n)
 if n=='school_clapboard':
  for y in range(0,128,16):d.line((0,y,127,y),fill=(48,105,151),width=2);d.line((0,y+2,127,y+2),fill=(117,192,222))
 elif n=='school_red_shingle':
  for y in range(0,128,16):
   d.line((0,y,127,y),fill=(100,35,47),width=2)
   for x in range(-16,144,32):d.line((x+(y//16%2)*16,y,x+(y//16%2)*16,y+15),fill=(123,37,45),width=2)
 elif n=='school_wainscot':
  for x in range(0,128,32):d.rectangle((x+2,2,x+29,125),outline=(84,47,31),width=3);d.rectangle((x+6,6,x+25,121),outline=(166,108,64),width=2)
  d.rectangle((0,0,127,7),fill=(91,52,32));d.line((0,8,127,8),fill=(195,136,78),width=2)
 else:
  for i in range(280):x=rng.randrange(128);y=rng.randrange(128);v=rng.choice([-3,-2,2]);d.point((x,y),fill=tuple(max(0,min(255,c+v)) for c in color))
 save(n,im)
# Embroidered teal and purple carpets, drawn as a repeating floral medallion tile.
for n,bg,vine,petal in [('school_lobby_rug','#276e82','#d6bd54','#e48d43'),('school_principal_rug','#3f235d','#42a0a7','#bd6da9')]:
 im=Image.new('RGBA',(128,128),bg);d=ImageDraw.Draw(im)
 for cx,cy in [(0,0),(128,0),(0,128),(128,128),(64,64)]:
  for r in range(18,47,7):d.arc((cx-r,cy-r,cx+r,cy+r),10,300,fill=vine,width=2)
  for a in range(0,360,45):
   x=cx+math.cos(math.radians(a))*13;y=cy+math.sin(math.radians(a))*13;d.ellipse((x-5,y-9,x+5,y+9),fill=petal,outline=vine,width=1)
  d.ellipse((cx-5,cy-5,cx+5,cy+5),fill='#f2d477')
 for a,b in [((12,64),(52,24)),((76,104),(116,64))]:d.arc((a[0],a[1]-30,b[0],b[1]+30),25,305,fill=vine,width=2)
 save(n,im)
im=Image.new('RGBA',(128,128),'#c7b392');d=ImageDraw.Draw(im)
for y in range(-32,160,32):
 for x in range(-32,160,32):
  ox=x+(16 if (y//32)%2 else 0);p=[(ox+6,y),(ox+26,y),(ox+32,y+6),(ox+32,y+26),(ox+26,y+32),(ox+6,y+32),(ox,y+26),(ox,y+6)];d.polygon(p,fill='#dbc9a9',outline='#ad9a7d');d.line((ox+6,y+1,ox+26,y+1),fill='#efe1c2')
save('school_stone_tile',im)
im=Image.new('RGBA',(128,128),'#e8d588');d=ImageDraw.Draw(im)
for y in range(0,128,16):d.line((0,y,127,y),fill='#d9b974');d.line((0,y+2,127,y+2),fill='#f8e9ae')
for x,y in [(20,20),(88,25),(56,82),(113,103)]:
 d.ellipse((x-7,y-5,x,y+3),fill='#df9c61');d.ellipse((x,y-5,x+7,y+3),fill='#df9c61');d.polygon([(x-7,y),(x+7,y),(x,y+11)],fill='#df9c61')
for x,y in [(28,94),(101,69)]:d.ellipse((x-9,y-7,x+9,y+7),fill='#f7eabb',outline='#bea56b');d.ellipse((x-4,y-2,x-2,y+3),fill='#bd9d65');d.ellipse((x+3,y-2,x+5,y+3),fill='#bd9d65');d.ellipse((x-2,y-14,x+2,y-10),fill='#d1ab61')
save('school_classroom_floor',im)
im=Image.new('RGBA',(128,128),'#87cbb5');d=ImageDraw.Draw(im)
for y in range(0,128,32):
 for x in range(0,128,32):d.rectangle((x,y,x+31,y+31),fill='#a3d7c4' if ((x+y)//32)%2 else '#579d88',outline='#d4e8d3')
save('school_clinic_tile',im)
im=Image.new('RGBA',(128,128),'#25a9e8');d=ImageDraw.Draw(im)
for x,y in [(5,24),(65,79),(113,19)]:
 for dx,dy,r in [(-15,2,12),(-3,-6,16),(12,0,13)]:d.ellipse((x+dx-r,y+dy-r,x+dx+r,y+dy+r),fill='#fffbe9')
 d.rectangle((x-27,y,x+25,y+10),fill='#fffbe9')
save('school_cloud_mural',im)
im=Image.new('RGBA',(128,128),'#efe2ba');d=ImageDraw.Draw(im);d.rectangle((0,20,127,107),fill='#b9914a')
for x in range(-8,144,24):d.ellipse((x,34,x+20,94),outline='#e2ce8a',width=4);d.ellipse((x+7,50,x+13,66),fill='#936c36')
d.line((0,15,127,15),fill='#fff7d7',width=5);d.line((0,112,127,112),fill='#fff7d7',width=5);save('school_frieze',im)
# Room-specific lower walls and fortune masonry from the supplied native frames.
im=Image.new('RGBA',(64,64),'#2d8976');d=ImageDraw.Draw(im)
for y in [3,18,35,51]:d.line((0,y,64,y),fill='#175a53',width=3);d.line((0,y+3,64,y+3),fill='#66af91',width=2)
save('school_clinic_wainscot',im)
im=Image.new('RGBA',(64,64),'#87718d');d=ImageDraw.Draw(im)
for y in [0,16,32,48]:
 d.line((0,y,64,y),fill='#665773',width=2)
 for x in range(-32 if (y//16)%2 else 0,65,32):d.line((x,y,x,y+16),fill='#665773',width=2)
save('school_fortune_stone',im)
# Upholstery, cabinet faces, painted doors, labels and child pictures.
for n,bg in [('school_purple_drape','#68217e'),('school_green_drape','#29725f')]:
 # Material only: the old baked tie was ten model pixels away from the real3D band.
 im=Image.new('RGBA',(208,256),bg);d=ImageDraw.Draw(im)
 for x in range(0,208,16):d.line((x,0,x,255),fill='#4c155c' if 'purple' in n else '#1d5547',width=6);d.line((x+6,0,x+6,255),fill='#9c4bb2' if 'purple' in n else '#4c9a7a',width=4)
 save(n,im)
for n in ['school_red_counter','school_green_counter','school_principal_desk']:
 im=Image.new('RGBA',(64,64),'#8f2530' if n!='school_green_counter' else '#30826c');d=ImageDraw.Draw(im)
 d.rectangle((2,3,61,60),outline='#e1bd65',width=2);d.rectangle((6,7,57,56),outline='#56272e',width=2)
 for a in range(0,360,45):x=32+math.cos(math.radians(a))*16;y=32+math.sin(math.radians(a))*16;d.ellipse((x-4,y-4,x+4,y+4),outline='#c99664',width=2)
 d.ellipse((25,24,39,39),outline='#e5bc66',width=2);save(n,im)
im=Image.new('RGBA',(128,128),'#204d77');d=ImageDraw.Draw(im)
for x in [6,67]:
 d.rounded_rectangle((x,5,x+55,122),radius=4,fill='#345d83',outline='#86b6c9',width=3)
 for y in [35,70,105]:
  d.line((x+3,y+10,x+52,y+10),fill='#80a7b5',width=3)
  for dx,c in [(8,'#d6caa2'),(25,'#87bac5'),(42,'#d3a6be')]:d.rectangle((x+dx-4,y-12,x+dx+4,y+7),fill=c);d.rectangle((x+dx-2,y-17,x+dx+2,y-12),fill='#b2bab7')
save('school_blue_cabinet',im)
im=Image.new('RGBA',(64,64),'#ed5a9b');d=ImageDraw.Draw(im)
for y in range(0,64,8):d.line((0,y,63,y),fill='#ce3d7f');
for x in range(0,64,8):d.line((x,0,x,63),fill='#f481b1')
save('pink_quilt',im)
im=Image.new('RGBA',(64,64),'#297cbb');d=ImageDraw.Draw(im)
for x,y,r in [(16,17,9),(36,33,13),(47,12,6),(12,48,8)]:d.ellipse((x-r,y-r,x+r,y+r),fill='#4eaa7c');d.arc((x-r,y-r,x+r,y+r),40,180,fill='#a7be60',width=2)
save('earth',im)
plain('glass',(142,215,219,140));plain('crystal',(111,229,233,185))
for n,role,col,label in [('principal','Principal','#77502d','PRINCIPAL'),('classroom','Classroom','#2855b4','CLASSROOM'),('health','Health Center','#2b9d91','HEALTH CENTER'),('fortune','Fortune','#a332a7','FORTUNE TELLER'),('exit','Exit','#347b57','EXIT')]:
 im=Image.new('RGBA',(128,128),col);d=ImageDraw.Draw(im);d.rounded_rectangle((6,2,121,125),radius=28,outline='#168849' if n=='principal' else '#b3eee3' if n=='health' else '#d9c384',width=5);d.rounded_rectangle((14,10,113,119),radius=22,outline='#effff8' if n=='health' else '#45b25d' if n=='principal' else '#ba9b5a',width=2);d.line((64,73,64,125),fill='#d4b26b',width=3);d.line((13,73,114,73),fill='#d4b26b',width=3);d.ellipse((99,76,108,85),fill='#efc746')
 if n=='health':d.ellipse((48,19,80,51),fill='#a7e6d4');d.rectangle((61,24,67,46),fill='#379f91');d.rectangle((53,32,75,38),fill='#379f91')
 else:
  d.ellipse((49,28,78,50),fill='#c2b587');d.ellipse((60,15,67,22),fill='#e3ca6b')
 d.text((64,59),role,fill='#efe5b5',font=font(11),anchor='mm');save('school_leaf_'+n,im)
for n,label,col in [('principal','PRINCIPAL','#765334'),('classroom','CLASSROOM','#3858b3'),('health','HEALTH CENTER','#339e91'),('fortune','FORTUNE TELLING','#a13a9f'),('market','BLACK MARKET','#5f4b49'),('exit','EXIT','#398063'),('kindergarten','CHAO KINDERGARTEN','#258dbd')]:
 im=Image.new('RGBA',(192,64),col);d=ImageDraw.Draw(im);d.rounded_rectangle((2,2,189,61),12,outline='#ead19c',width=3);d.text((96,31),label,fill='#fff3c6',font=font(17 if len(label)<13 else 15),anchor='mm');save('school_label_'+n,im)
im=Image.new('RGBA',(128,96),'#a8413d');d=ImageDraw.Draw(im);d.rectangle((3,3,124,92),outline='#f2d085',width=5)
for x,y in [(15,17),(69,23)]:
 d.rectangle((x,y,x+43,y+57),fill='#fff0cb',outline='#d7ba85');
 for yy in range(y+9,y+52,7):d.line((x+6,yy,x+35,yy),fill='#b29a71',width=1)
for x,y in [(12,13),(67,19),(116,80)]:d.ellipse((x,y,x+5,y+5),fill='#efcd4a')
save('school_noticeboard',im)
for idx,n in enumerate(['school_art_chao','school_art_friends','school_art_family','school_art_garden']):
 im=Image.new('RGBA',(96,80),'#f7e4bb');d=ImageDraw.Draw(im);d.rectangle((2,2,93,77),outline='#bc8150',width=5);d.rectangle((8,8,87,71),fill=['#45a6c7','#f7a3b7','#75a95b','#a3cbe9'][idx])
 def chao(x,y,c,scale=1):
  d.ellipse((x-11*scale,y-8*scale,x+11*scale,y+8*scale),fill=c,outline='#537392');d.polygon([(x+5*scale,y-5*scale),(x+2*scale,y-17*scale),(x-4*scale,y-4*scale)],fill=c);d.ellipse((x-3*scale,y-23*scale,x+3*scale,y-17*scale),fill='#e9d56b');d.ellipse((x-7*scale,y+8*scale,x+7*scale,y+20*scale),fill=c);d.rectangle((x-6*scale,y-2*scale,x-3*scale,y+3*scale),fill='#34506a');d.rectangle((x+3*scale,y-2*scale,x+6*scale,y+3*scale),fill='#34506a')
 if idx==0:chao(48,40,'#a5e2e0',1.4)
 elif idx==1:chao(28,40,'#9cdcd6',.8);chao(66,40,'#f3df9e',.8)
 elif idx==2:chao(31,35,'#d7c5e5',1);chao(67,45,'#9cdcd6',.7)
 else:
  d.rectangle((8,51,87,71),fill='#6bb45d');chao(49,42,'#b2e6e6',.7);d.ellipse((12,14,27,29),fill='#f1d969');d.line((76,56,76,28),fill='#8a6d3d',width=4);d.ellipse((61,19,87,36),fill='#489b5d')
 save(n,im)
# The source-video school seal is a complete gold disk and green laurel around
# one cyan head. The former 25..339-degree leaf loop made a literal C, while
# its gold orb merged into the top leaves and hid the head's upper silhouette.
im=Image.new('RGBA',(128,128),(0,0,0,0));d=ImageDraw.Draw(im)
d.ellipse((5,5,122,122),fill='#efd35c',outline='#98732d',width=3)
d.ellipse((10,10,117,117),outline='#fff0a0',width=2)
d.ellipse((18,18,109,109),outline='#3f8246',width=2)
for a in range(0,360,18):
 angle=math.radians(a);nx,ny=math.cos(angle),math.sin(angle);tx,ty=-ny,nx
 # Each leaf turns with the branch instead of repeating upright oval beads.
 cx,cy=63.5+nx*45.5,63.5+ny*45.5
 leaf=[(cx+nx*radial+tx*tangent,cy+ny*radial+ty*tangent)
       for radial,tangent in [(0,-7.5),(4,-2),(3,4),(0,7.5),(-3,2),(-4,-4)]]
 d.polygon(leaf,fill='#39854d',outline='#2d663e')
 d.line((cx+tx*-5,cy+ty*-5,cx+tx*4,cy+ty*4),fill='#7ba851',width=1)
# Keep the orb and the whole rounded head inside the laurel with visible air
# between their outlines. A single outline avoids the old ellipse/tuft seam.
head=[(63,39),(57,49),(52,55),(44,61),(39,68),(37,77),(40,87),
      (48,94),(58,98),(69,98),(80,94),(88,87),(91,77),(88,68),
      (82,61),(76,56),(71,49),(68,42)]
head_mask=Image.new('L',im.size,0);ImageDraw.Draw(head_mask).polygon(head,fill=255)
im.paste('#fff5bf',(0,0),head_mask.filter(ImageFilter.MaxFilter(5)))
im.paste('#79d5df',(0,0),head_mask)
d.arc((41,63,87,95),5,165,fill='#57b4cb',width=3)
d.ellipse((59,25,68,34),fill='#79d5df',outline='#4c98b5',width=2)
save('school_emblem',im)
# Scalloped red shingles and the yellow masonry approach are prominent exterior cues.
im=Image.new('RGBA',(128,128),'#a63445');d=ImageDraw.Draw(im)
for y in range(-16,144,16):
 for x in range(-32,160,32):
  xx=x+(16 if (y//16)%2 else 0);d.rounded_rectangle((xx,y,xx+31,y+24),radius=14,fill='#b94553' if (x//32+y//16)%2 else '#c14c57',outline='#682a38',width=2);d.arc((xx+3,y+2,xx+28,y+22),0,180,fill='#dc6870',width=2)
save('school_red_shingle',im)
im=Image.new('RGBA',(128,128),'#b6a164');d=ImageDraw.Draw(im)
for y in range(-16,144,16):
 for x in range(-32,160,32):
  xx=x+(16 if (y//16)%2 else 0);d.rectangle((xx+1,y+1,xx+30,y+14),fill='#dfc77e' if (x//32+y//16)%3 else '#d1b56c');d.line((xx+2,y+2,xx+29,y+2),fill='#f0db9b',width=2)
save('school_yellow_brick',im)
im=Image.new('RGBA',(576,108),'#2a69ad');d=ImageDraw.Draw(im);d.rounded_rectangle((2,2,573,105),14,outline='#f2d591',width=6);d.text((288,54),'KINDERGARTEN',fill='#fff4c5',stroke_width=1,stroke_fill='#274a83',font=font(52),anchor='mm')
for i,part in enumerate(['left','middle','right']):save('school_facade_title_'+part,im.crop((i*192,0,(i+1)*192,108)))
# One composed rug, rather than a flower motif repeated once per block.
w,h=576,832
im=Image.new('RGBA',(w,h),'#237b91');d=ImageDraw.Draw(im)
for inset,color,width in [(3,'#b3a653',12),(18,'#284f53',7),(30,'#ddc878',6),(47,'#173f59',3)]:d.rounded_rectangle((inset,inset,w-1-inset,h-1-inset),radius=18,outline=color,width=width)
for x in range(65,w-50,34):
 for y in [39,h-40]:d.ellipse((x-11,y-4,x+11,y+4),fill='#aa9e54');d.line((x-12,y+4,x+12,y-4),fill='#f1cc6b',width=2)
for y in range(70,h-50,34):
 for x in [39,w-40]:d.ellipse((x-4,y-11,x+4,y+11),fill='#aa9e54');d.line((x-4,y-12,x+4,y+12),fill='#f1cc6b',width=2)
cx,cy=w/2,h/2
for r,col,width in [(170,'#a2aa58',8),(150,'#e6ae49',5),(130,'#1a566c',9)]:d.ellipse((cx-r,cy-r*1.55,cx+r,cy+r*1.55),outline=col,width=width)
for a in range(0,360,30):
 ang=math.radians(a);x=cx+math.cos(ang)*89;y=cy+math.sin(ang)*148
 d.ellipse((x-36,y-56,x+36,y+56),fill='#db8e38',outline='#efc16b',width=5)
 d.ellipse((x-23,y-42,x+23,y+42),fill='#a95830',outline='#e7ac47',width=3)
for r,col in [(60,'#eac073'),(46,'#347f8e'),(30,'#e49637'),(13,'#edcf75')]:d.ellipse((cx-r,cy-r*1.25,cx+r,cy+r*1.25),fill=col)
for x,y in [(103,116),(w-103,116),(103,h-116),(w-103,h-116)]:
 for a in range(0,360,45):
  xx=x+math.cos(math.radians(a))*24;yy=y+math.sin(math.radians(a))*33;d.ellipse((xx-12,yy-20,xx+12,yy+20),fill='#daa64e',outline='#f4d67a',width=2)
 d.ellipse((x-9,y-9,x+9,y+9),fill='#a65e35')
save('school_lobby_composite',im.resize((1024,1024),Image.Resampling.LANCZOS))
# The ceiling is periodic in BOTH axes. Wrapped cloud lobes and a periodic sky
# avoid the former four-block gradient stripes and clipped cloud silhouettes.
im=Image.new('RGBA',(512,512));d=ImageDraw.Draw(im)
for y in range(512):
 for x in range(512):
  glow=(math.cos(x/512*math.tau)+math.cos(y/512*math.tau))*1.5
  im.putpixel((x,y),(round(45+glow),round(164+glow),round(226+glow),255))
for x,y,scale in [(54,98,1.1),(355,70,1.05),(239,273,1.2),(57,437,.85),(442,441,1.1)]:
 for ox in [-512,0,512]:
  for oy in [-512,0,512]:
   for dx,dy,r in [(-52,12,33),(-22,-12,41),(18,-19,48),(56,5,35),(1,18,42)]:
    xx=x+ox+dx*scale;yy=y+oy+dy*scale;rr=r*scale;d.ellipse((xx-rr,yy-rr,xx+rr,yy+rr),fill='#f7ffff')
   d.ellipse((x+ox-60*scale,y+oy+13*scale,x+ox+68*scale,y+oy+41*scale),fill='#d8f1f4')
save('school_cloud_composite',im)
# A continuous mountainscape rises into clouds on every classroom wall; the ceiling has only sky.
w,h=1024,640
im=Image.new('RGBA',(w,h));d=ImageDraw.Draw(im)
for y in range(h):
 t=y/(h-1);d.line((0,y,w-1,y),fill=(int(48+45*t),int(164+33*t),int(231-6*t),255))
for x,y,scale in [(140,87,1.0),(610,130,1.2),(925,68,.8)]:
 for wrap in [-w,0,w]:
  for dx,dy,r in [(-45,10,32),(-12,-9,43),(25,-15,39),(61,8,29)]:
   xx=x+wrap+dx*scale;yy=y+dy*scale;rr=r*scale;d.ellipse((xx-rr,yy-rr,xx+rr,yy+rr),fill='#f7ffff')
for base,amp,c,phase in [(380,125,'#a6cdd4',0),(464,118,'#71b9ac',1.4),(555,65,'#63ab71',.7)]:
 pts=[(x,base+math.sin(x/w*math.tau*3+phase)*amp*.55+math.cos(x/w*math.tau*5+phase)*amp*.35) for x in range(w+1)]
 d.polygon([(0,h)]+pts+[(w,h)],fill=c)
save('school_mountain_composite',im)
# Three-high leaves use an exact23:48 physical aspect, then are sliced by native cell.
# Eight logical art pixels per world pixel preserve circles and borders without stretching.
# Save at2x so368x768 remains divisible by16 and retains the native four-level mipmap budget.
# Both sides keep the handle at the free edge, while the badge reads from its own side.
for role,color in [('principal','#357b49'),('classroom','#315ca1'),('health','#409d91'),('fortune','#8e3699'),('exit','#397b58'),('market','#285f68')]:
 im=Image.new('RGBA',(184,384),color);d=ImageDraw.Draw(im)
 if role=='market':
  # The SA2 cupboard is plain teal joinery: chamfered recessed panels and three low vents.
  # The game's black-market posters are deliberately omitted per the user's hidden-shop request.
  d.polygon([(8,23),(22,8),(163,8),(176,23),(176,376),(8,376)],fill='#1f4f59')
  d.polygon([(18,31),(30,19),(153,19),(165,31),(165,359),(18,359)],fill='#235660',outline='#123e48',width=3)
  d.line([(21,34),(32,23),(152,23),(162,34)],fill='#3b737c',width=3);d.line((21,34,21,356),fill='#326b73',width=3)
  d.line((21,357,163,357),fill='#3a737b',width=2)
  for y in (290,306,322):
   d.rectangle((32,y,152,y+5),fill='#123d47');d.line((32,y-1,152,y-1),fill='#39727c',width=2);d.line((32,y+6,152,y+6),fill='#3d7a83',width=2)
 else:
  d.rounded_rectangle((7,7,176,376),radius=32,outline='#dcc790',width=5);d.rounded_rectangle((16,16,167,367),radius=27,outline='#a8cdbc',width=2)
  # Keep the complete badge beneath the actual stepped arch shoulders when closed.
  # Uniform reflow preserves the round badge/face and leaves the lower panel intact.
  def badge(rect):return tuple(92+(v-96)*20/33 if i%2==0 else 161+(v-77)*20/33 for i,v in enumerate(rect))
  d.ellipse(badge((63,44,129,110)),fill='#d6ce9c');d.ellipse(badge((91,26,102,37)),fill='#e9d372')
  if role=='health':d.rectangle(badge((91,57,101,99)),fill='#399c91');d.rectangle(badge((75,73,117,83)),fill='#399c91')
  else:
   d.ellipse(badge((78,65,113,91)),fill='#91ceca');d.ellipse(badge((85,73,88,81)),fill='#315a60');d.ellipse(badge((103,73,106,81)),fill='#315a60')
 back=im.copy();bd=ImageDraw.Draw(back)
 if role=='market':
  # Matching physical free-edge pulls remain correct on both sides when opened.
  for draw,x0,x1 in ((d,150,162),(bd,22,34)):
   draw.rectangle((x0-1,176,x1+1,225),fill='#514b2b');draw.rectangle((x0,178,x1,224),fill='#9d7f2f');draw.line((x0+1,179,x0+1,223),fill='#bd9b43',width=2)
 else:
  bd.ellipse((23,197,37,211),fill='#f0ce63',outline='#9c7639',width=2);d.ellipse((147,197,161,211),fill='#f0ce63',outline='#9c7639',width=2)
 save('school_door_'+role,im.resize((368,768),Image.Resampling.NEAREST));save('school_door_'+role+'_back',back.resize((368,768),Image.Resampling.NEAREST))
 save('school_door_'+role+'_edge',Image.new('RGBA',(16,16),color))
# A separate utility cabinet, not another recolored school door: cold rolled
# silver steel, recessed rectangular panels, stamped vents and steel pull plates.
im=Image.new('RGBA',(184,384),'#9ba6af');d=ImageDraw.Draw(im)
for y in range(384):
 c=(153+(y%7==0)*6,164+(y%7==0)*6,174+(y%7==0)*6,255);d.line((0,y,183,y),fill=c)
d.rectangle((5,5,178,378),outline='#64717c',width=5)
d.line((11,10,173,10),fill='#d9e1e6',width=3);d.line((11,10,11,374),fill='#c5cfd6',width=3)
for ya,yb in ((25,142),(244,356)):
 d.rectangle((22,ya,163,yb),fill='#8996a1',outline='#586671',width=3)
 d.line((26,ya+4,159,ya+4),fill='#b3bec6',width=2)
for y in (49,65,81,97,113,269,285,301,317,333):
 d.rectangle((37,y,147,y+4),fill='#424e59');d.line((37,y+5,147,y+5),fill='#c8d1d7')
for y in (30,352):
 d.rectangle((5,y,17,y+27),fill='#525f6a',outline='#cbd3d8',width=2)
 for sy in (y+6,y+20):d.ellipse((9,sy-1,12,sy+2),fill='#c8cfd4')
back=im.transpose(Image.Transpose.FLIP_LEFT_RIGHT);bd=ImageDraw.Draw(back)
for draw,x0,x1 in ((d,150,162),(bd,22,34)):
 draw.rectangle((x0-3,171,x1+3,231),fill='#53616d',outline='#dce3e7',width=2)
 draw.rectangle((x0,182,x1,220),fill='#d3dce2',outline='#677681',width=2)
 draw.line((x0+3,185,x0+3,216),fill='#f2f5f7',width=2)
save('school_tool_cabinet',im.resize((368,768),Image.Resampling.NEAREST))
save('school_tool_cabinet_back',back.resize((368,768),Image.Resampling.NEAREST))
case=Image.new('RGBA',(128,128),'#8d9ba6');cd=ImageDraw.Draw(case)
for y in range(0,128,4):cd.line((0,y,127,y),fill='#a9b5be' if y%8 else '#7b8995')
save('school_tool_cabinet_case',case);save('school_tool_cabinet_edge',Image.new('RGBA',(16,16),'#687782'))
for color in ['purple','green']:
 im=Image.new('RGBA',(192,64),'#68217e' if color=='purple' else '#29725f');d=ImageDraw.Draw(im)
 for x in range(0,192,16):
  d.rectangle((x,0,x+4,63),fill='#4c155c' if color=='purple' else '#1d5547');d.line((x+7,0,x+7,63),fill='#9c4bb2' if color=='purple' else '#4c9a7a',width=3)
 save('school_'+color+'_room_curtain',im)
def ornament_canvas(name,w,h,bg):
 im=Image.new('RGBA',(w,h),bg);d=ImageDraw.Draw(im)
 d.rectangle((2,2,w-3,h-3),outline='#e1bd65',width=2);d.rectangle((5,5,w-6,h-6),outline='#56272e',width=2)
 cx,cy=w/2,h/2;r=min(w,h)*.255
 for a in range(0,360,45):
  x=cx+math.cos(math.radians(a))*r;y=cy+math.sin(math.radians(a))*r;d.ellipse((x-3,y-3,x+3,y+3),outline='#c99664',width=2)
 d.ellipse((cx-6,cy-6,cx+6,cy+6),outline='#e5bc66',width=2);save(name,im)
for name,width,bg in [('school_red_counter',16,'#8f2530'),('school_green_counter',16,'#30826c'),('school_principal_desk',24,'#8f2530')]:
 ornament_canvas(name,width*4,64,bg)
 ornament_canvas(name+'_body_front',(width-8)*8,48,bg);ornament_canvas(name+'_body_side',64,48,bg)
 for suffix,w in [('rim_front',width*8),('rim_side',128)]:
  im=Image.new('RGBA',(w,16),bg);d=ImageDraw.Draw(im);d.rectangle((0,0,w-1,2),fill='#e1bd65');d.rectangle((0,13,w-1,15),fill='#b88848');save(name+'_'+suffix,im)
im=Image.new('RGBA',(192,256),'#204d77');d=ImageDraw.Draw(im)
for x in [6,100]:
 d.rounded_rectangle((x,5,x+85,250),radius=5,fill='#345d83',outline='#86b6c9',width=4)
 for y in [69,142,215]:
  d.line((x+4,y+18,x+81,y+18),fill='#80a7b5',width=3)
  for dx,c in [(15,'#d6caa2'),(43,'#87bac5'),(70,'#d3a6be')]:d.rectangle((x+dx-5,y-21,x+dx+5,y+14),fill=c);d.rectangle((x+dx-3,y-28,x+dx+3,y-21),fill='#b2bab7')
save('school_medicine_cabinet_front',im)
im=Image.new('RGBA',(224,400),'#ed5a9b');d=ImageDraw.Draw(im)
for y in range(0,400,16):d.line((0,y,223,y),fill='#ce3d7f',width=2)
for x in range(0,224,16):d.line((x,0,x,399),fill='#f481b1',width=2)
save('pink_quilt_top',im)
# Shared native element builders: all models fit legal-32 pixel element bounds.
def el(a,b,t='base',rotation=None,faces=None):
 e={'from':a,'to':b,'faces':{f:{'texture':'#'+t,'uv':[0,0,16,16]} for f in (faces or ['north','south','east','west','up','down'])}}
 if rotation:e['rotation']=rotation
 return e
def box(a,b,t='base'):return el(a,b,t)
def sphere(center,r,t='base'):
 x,y,z=center;out=[]
 for lo,hi in [(-1,-.65),(-.65,-.3),(-.3,.3),(.3,.65),(.65,1)]:
  mid=(lo+hi)/2;w=r*math.sqrt(max(.1,1-mid*mid));out.append(box([x-w,y+r*lo,z-w],[x+w,y+r*hi,z+w],t))
 return out
def model(id,kind):
 tex={'base':'chao_garden:block/school025/'+id,'wood':'chao_garden:block/school025/wood','gold':'chao_garden:block/school025/gold','cream':'chao_garden:block/school025/cream','white':'chao_garden:block/school025/white','red':'chao_garden:block/school025/red','blue':'chao_garden:block/school025/blue','green':'chao_garden:block/school025/green','purple':'chao_garden:block/school025/purple','pink':'chao_garden:block/school025/pink','black':'chao_garden:block/school025/black','yellow':'chao_garden:block/school025/yellow','cyan':'chao_garden:block/school025/cyan','glass':'chao_garden:block/school025/glass','stone':'chao_garden:block/school025/stone'}
 if kind in ['WALL','RUG','CLOUD','MURAL']:return {'parent':'minecraft:block/cube_all','textures':{'all':tex['base']}}
 out=[];pattern_volumes=[]
 if kind in ['COUNTER','DESK']:
  wide=kind=='DESK';lo,hi=(-4,20) if wide else (0,16)
  # The apron is partitioned around its wooden posts. Coplanar red/wood
  # faces at the old overlapping corners caused the observed native black grids.
  out=[box([lo,8,0],[hi,10,16]),box([lo+4,2,1],[hi-4,8,15]),box([lo+1,2,4],[lo+4,8,12]),box([hi-4,2,4],[hi-1,8,12])]
  if id!='school_fortune_altar':
   for key,suffix in [('front','body_front'),('side','body_side'),('rimfront','rim_front'),('rimside','rim_side')]:tex[key]='chao_garden:block/school025/'+id+'_'+suffix
   for face,data in out[0]['faces'].items():data['texture']='#base' if face=='up' else '#wood' if face=='down' else '#rimfront' if face in ('north','south') else '#rimside'
   for e in out[1:4]:
    for data in e['faces'].values():data['texture']='#wood'
   for face in ('north','south'):out[1]['faces'][face]['texture']='#front'
   out[2]['faces']['west']['texture']='#side';out[3]['faces']['east']['texture']='#side'
  for x in [lo+1,hi-4]:
   for z in [1,12]:out.append(box([x,0,z],[x+3,8,z+3],'wood'))
  if id=='school_principal_desk':
   for x,c in [(-2,'blue'),(0,'red')]:out.extend([box([x,10,6],[x+.15,15,10],c),box([x+1.35,10,6],[x+1.5,15,10],c),box([x+.15,10,9.8],[x+1.35,15,10],c),box([x+.15,10.2,6.1],[x+1.35,14.8,9.8],'cream')])
   out.extend([box([3,10.05,4],[10,10.3,12],'cream'),box([11,10,7],[13,12,9],'green'),el([12,12,7.8],[12.5,17,8.3],'green',{'origin':[12,12,8],'axis':'z','angle':-22.5,'rescale':False}),box([14,10,6],[19,10.8,11],'gold'),box([16,10.8,8],[17,12,9],'gold')])
   out+=sphere([16.5,14,8.5],2.2,'earth');tex['earth']='chao_garden:block/school025/earth';pattern_volumes.append(('earth',[16.5,14,8.5],2.2))
   for z in [5,6.5,8,9.5]:out.append(box([4,10.35,z],[9,10.4,z+.2],'black'))
   out.append(box([6,10,14],[12,11.6,15],'blue'))
  elif id=='school_fortune_altar':
   out=[box([-3,0,2],[19,8,14],'blue'),box([-4,8,0],[20,9,16],'gold'),box([-4,9,0],[20,10,16],'red'),box([5.5,10,5.5],[10.5,10.6,10.5],'gold')]+sphere([8,12,8],1.5,'crystal');tex['crystal']='chao_garden:block/school025/crystal'
   out.extend([box([-2,10,6],[1,10.8,10],'gold'),box([-.9,10.8,7.5],[-.2,12,8.2],'gold'),box([-3.15,12,7.25],[2.35,12.7,8.35],'gold')])
   for x in [-3,-.7,1.6]:out.extend([box([x,12.7,7.4],[x+.6,15,8.1],'cream'),box([x+.18,15,7.6],[x+.42,15.8,7.9],'yellow')])
   for x,z,c in [(15,7,'purple'),(18,6,'green'),(17,11,'cyan')]:out.extend([box([x-.8,10,z-.8],[x+.8,13,z+.8],c),box([x-.4,13,z-.4],[x+.4,14,z+.4],'gold')])
   out.extend([box([11,10,10],[14.5,16,10.3],'cream'),box([12,12,10.35],[13.5,14,10.4],'red')])
 elif kind=='STOOL':
  color='pink' if 'pink' in id else 'green' if 'green' in id else 'red'
  out=sphere([8,6,8],4,color)
  for element in out:
   for edge in ('from','to'):element[edge][1]=6+(element[edge][1]-6)*.4
  out.extend([box([7,1,7],[9,4.5,9],'white'),box([4,1,7],[12,2,9],'white'),box([7,1,4],[9,2,12],'white')])
  if 'chair' in id:
   # Seat the native adult belly above a compact cushion. The old sphere's
   # eleven-pixel top and full-height front engulfed the entire lower body.
   seat=8.75 if 'green' in id else 7.25;back=seat+8.5
   out=[box([4,seat-2,5],[12,seat,11.5],color),box([4.25,seat-.25,4.5],[11.75,back,6],color),
        box([3.75,seat-1,5.5],[5,seat+1,10.75],color),box([11,seat-1,5.5],[12.25,seat+1,10.75],color),
        box([7,1,7],[9,seat-2,9],'white'),box([4,1,7],[12,2,9],'white'),box([7,1,4],[9,2,12],'white')]
   if 'fortune' in id:out.extend([box([3.75,back-.5,4.2],[12.25,back+.5,6.1],'gold'),box([3.75,seat-1,4.2],[4.4,back,6.1],'gold'),box([11.6,seat-1,4.2],[12.25,back,6.1],'gold')])
 elif kind=='BED':
  out=[box([0,4,0],[16,8,32],'green'),box([1,8,5],[15,10,30],'quilt'),box([1,8,1],[15,11,7],'white')]
  for x in [0,14]:
   for z in [0,30]:out.append(box([x,0,z],[x+2,15,z+2],'white'))
  for z in [0,30]:out.append(box([0,13,z],[16,15,z+2],'white'))
  tex['quilt']='chao_garden:block/school025/pink_quilt_top'
  for face,data in out[1]['faces'].items():data['texture']='#quilt' if face=='up' else '#pink'
 elif kind=='CABINET':
  tex['base']='chao_garden:block/school025/school_medicine_cabinet_front'
  out=[box([-4,0,1],[20,32,14]),box([-5,30,0],[21,32,15],'blue')]
  for face,data in out[0]['faces'].items():data['texture']='#base' if face=='south' else '#blue'
  for x in [-3,17]:out.append(box([x,0,1],[x+2,3,15],'blue'))
 elif kind=='DRAPE':
  out=[box([-5,0,7],[21,32,9])]
  for x in [-5,1,7,13,19]:out.append(box([x,0,6],[x+2,32,10]))
  if not id.endswith('_plain'):out.append(box([-6,10,5],[22,12,11],'gold'));out.append(box([15,6,10.5],[17,10,12],'gold'))
  else:tex['base']='chao_garden:block/school025/'+id.removesuffix('_plain')
 elif kind=='CURTAIN':
  color='purple' if 'purple' in id else 'green';tex['base']='chao_garden:block/school025/school_'+color+'_room_curtain'
  out=[box([-16,0,6],[32,16,10])]
  for x in range(-16,32,4):out.append(box([x,0,5.5],[x+1.5,16,10.5]))
  if id.endswith('_tie'):out.extend([box([-16,7,5],[32,9,11],'gold'),box([28,4,10.5],[29.5,7,12],'gold')])
 elif kind=='GUARD':out=[];tex['base']=tex['glass']
 elif kind=='PANEL':
  if id=='school_emblem':out=[el([-16,-16,1],[32,32,1.1],faces=['south'])]
  elif id=='school_art_chao_small':
   tex['base']='chao_garden:block/school025/school_art_chao';out=[box([0,0,0],[16,13.333333,1],'wood'),el([.6,.5,1.1],[15.4,12.833333,1.2],faces=['south'])]
  elif id=='school_noticeboard':out=[box([0,0,0],[16,12,1],'wood'),el([.8,.6,1.1],[15.2,11.4,1.2],faces=['south'])]
  elif id.startswith('school_facade_title'):
   a,b=-8,24;out=[box([a,0,0],[b,18,1],'blue'),el([a,0,1.1],[b,18,1.2],faces=['south'])]
  elif id.startswith('school_label'):out=[box([-16,0,0],[32,16,1],'wood'),el([-15,.333333,1.1],[31,15.666667,1.2],faces=['south'])]
  else:out=[box([-4,0,0],[20,20,1],'wood'),el([-3,.833333,1.1],[19,19.166667,1.2],faces=['south'])]
 elif kind=='LEAF':
  upper=id.endswith('_upper');tex['base']='chao_garden:block/school025/'+id.removesuffix('_upper');e=box([1,0,7],[15,32 if upper else 16,8]);
  for face in e['faces'].values():face['uv']=[0,0,16,10.67] if upper else [0,10.67,16,16]
  out=[e]
 elif kind=='WINDOW':
  tex['base']=tex['green'];out=[box([1,1,7],[31,31,9],'glass')]
  # Full-depth casement corners close the exact2x2 wall aperture; glazing has no exposed hole.
  for y,inset in [(0,10),(2,6),(4,3),(6,1),(8,0),(10,0),(12,0),(14,0),(16,0),(18,0),(20,0),(22,0),(24,1),(26,3),(28,6),(30,10)]:
   if inset:out.extend([box([0,y,0],[inset,y+2,16],'green'),box([32-inset,y,0],[32,y+2,16],'green')])
  out.extend([box([0,8,0],[2,24,16],'green'),box([30,8,0],[32,24,16],'green'),box([0,0,0],[32,2,16],'green'),box([0,30,0],[32,32,16],'green'),box([15,2,6],[17,30,10],'green'),box([2,15,6],[30,17,10],'green')])
 elif kind=='ARCH':
  role=id.removeprefix('school_arch_');tex['base']=tex[{'principal':'green','classroom':'blue','health':'cyan','fortune':'purple','market':'wood','exit':'green'}[role]]
  # A complete three-block-high rounded frame, including coloured jambs.
  out=[box([-16,-16,0],[-11,8,16.2]),box([27,-16,0],[32,8,16.2])]
  for x0,x1,lo,hi in [(-16,-11,8,16),(-11,-6,16,23),(-6,0,23,28),(0,16,28,32),(16,22,23,28),(22,27,16,23),(27,32,8,16)]:
   out.append(box([x0,lo,0],[x1,hi,16.2]))
   if hi<32:out.append(box([x0,hi,0],[x1,32,16],'cream'))
   out.append(box([x0,hi-1,16.21],[x1,hi,16.35],'gold'))
 elif kind=='DOOR':out=[box([1,0,7],[16,16,9])]
 elif kind=='COLUMN':
  out=[box([3,0,3],[13,16,13],'blue'),el([2.5,6,5],[13.5,8,11],'cyan',{'origin':[8,7,8],'axis':'y','angle':45,'rescale':False})]
  if id=='school_hall_column':out=[box([3,0,3],[13,2,13],'cream'),box([5,2,5],[11,16,11],'cream')]
 elif kind=='TOY':
  from classroom_toy_geometry_0258 import toy_model
  return toy_model(id)
 else:
  if id=='school_crystal_ball':out=[box([2,0,2],[14,2,14],'gold')]+sphere([8,8,8],6,'crystal');tex['crystal']='chao_garden:block/school025/crystal'
  elif id=='school_globe':out=[box([3,0,3],[13,2,13],'wood'),box([7,2,7],[9,5,9],'gold')]+sphere([8,10,8],6,'earth');tex['earth']='chao_garden:block/school025/earth';pattern_volumes.append(('earth',[8,10,8],6))
  elif id=='school_book_stack':
   for y,c in [(0,'blue'),(3,'red'),(6,'green')]:out.extend([box([2,y,2],[14,y+1,12],c),box([3,y+1,2],[14,y+2,12],'cream'),box([2,y+1,2],[3,y+2,12],c),box([2,y+2,2],[14,y+3,12],c)])
  elif id=='school_open_book':out=[el([0,1,2],[8,2,13],'cream',{'origin':[8,1,8],'axis':'z','angle':-22.5,'rescale':False}),el([8,1,2],[16,2,13],'cream',{'origin':[8,1,8],'axis':'z','angle':22.5,'rescale':False})]
  elif id=='school_bust':out=[box([2,0,2],[14,2,14],'stone'),box([5,2,5],[11,7,11],'stone')]+sphere([8,11,8],5,'stone')+[box([6,15,6],[10,18,10],'stone')]
  elif id=='school_candelabra':
   out=[box([3,0,3],[13,2,13],'gold'),box([7,2,7],[9,13,9],'gold'),box([1,8,7],[15,10,9],'gold')]
   for x,h in [(1,12),(7,15),(13,12)]:out.extend([box([x,h-5,7],[x+2,h,9],'cream'),box([x+.5,h,7.5],[x+1.5,h+2,8.5],'yellow')])
  elif id=='school_quill':out=[box([4,0,4],[12,4,12],'green'),el([7,4,7],[8,15,9],'green',{'origin':[8,4,8],'axis':'z','angle':-22.5,'rescale':False})]
  else:
   for x,z,c in [(3,4,'purple'),(10,3,'green'),(5,11,'cyan'),(12,11,'red')]:out.extend([box([x-2,0,z-2],[x+2,7,z+2],c),box([x-1,7,z-1],[x+1,10,z+1],'gold')])
 # The door now owns the middle column; ARCH anchors move one cell forward.
 # Counter-translate only their decorative mesh to preserve the exact visible frame.
 if kind=='ARCH':
  for element in out:
   for edge in ('from','to'):element[edge][2]-=16
 # The opaque tie replaces the cloth at its height. This is a real union,
 # not two different same-normal endcaps fighting on the exact same plane.
 tied=kind=='CURTAIN' and id.endswith('_tie') or kind=='DRAPE' and not id.endswith('_plain')
 if tied:
  low,high=(7,9) if kind=='CURTAIN' else (10,12);partition=[]
  for e in out:
   if not all(f['texture']=='#base' for f in e['faces'].values()):partition.append(e);continue
   if e['from'][1]<low:
    piece=deepcopy(e);piece['to'][1]=min(low,e['to'][1]);partition.append(piece)
   if e['to'][1]>high:
    piece=deepcopy(e);piece['from'][1]=max(high,e['from'][1]);partition.append(piece)
  out=partition
 # Preserve texel aspect on every patterned cloth fold; edges are plain material cross-sections.
 if kind in ('DRAPE','CURTAIN'):
  lo,width,height=(-5,26,32) if kind=='DRAPE' else (-16,48,16);color='purple' if 'purple' in id else 'green'
  for e in out:
   for face,data in e['faces'].items():
    if data['texture']!='#base':continue
    if face in ('north','south'):
     u0=(e['from'][0]-lo)/width*16;u1=(e['to'][0]-lo)/width*16;v0=(height-e['to'][1])/height*16;v1=(height-e['from'][1])/height*16
     data['uv']=[u1,v0,u0,v1] if face=='north' else [u0,v0,u1,v1]
    else:data['texture']='#'+color
 # A globe's continents span its stepped sphere once, instead of repeating a full map on every thin band.
 for material,center,radius in pattern_volumes:
  cx,cy,cz=center
  for e in out:
   a,b=e['from'],e['to']
   for face,data in e['faces'].items():
    if data['texture']!='#'+material:continue
    if face in ('north','south'):
     u0=(a[0]-cx+radius)/(2*radius)*16;u1=(b[0]-cx+radius)/(2*radius)*16;v0=(cy+radius-b[1])/(2*radius)*16;v1=(cy+radius-a[1])/(2*radius)*16
     data['uv']=[u1,v0,u0,v1] if face=='north' else [u0,v0,u1,v1]
    elif face in ('east','west'):
     u0=(a[2]-cz+radius)/(2*radius)*16;u1=(b[2]-cz+radius)/(2*radius)*16;v0=(cy+radius-b[1])/(2*radius)*16;v1=(cy+radius-a[1])/(2*radius)*16
     data['uv']=[u1,v0,u0,v1] if face=='east' else [u0,v0,u1,v1]
    else:
     u0=(a[0]-cx+radius)/(2*radius)*16;u1=(b[0]-cx+radius)/(2*radius)*16;v0=(a[2]-cz+radius)/(2*radius)*16;v1=(b[2]-cz+radius)/(2*radius)*16
     data['uv']=[u0,v1,u1,v0] if face=='down' else [u0,v0,u1,v1]
 if kind=='SMALL':
  # Native adult heads are only5.6worldpixels wide at0.8scale. These are
  # tabletop objects rather than full-block sculptures, especially the ball.
  scale=.25 if id=='school_crystal_ball' else .35 if id in ('school_globe','school_bust','school_candelabra') else .4
  for element in out:
   for edge in ('from','to'):
    p=element[edge];p[0]=8+(p[0]-8)*scale;p[1]=p[1]*scale-6;p[2]=8+(p[2]-8)*scale
   if 'rotation' in element:
    p=element['rotation']['origin'];p[0]=8+(p[0]-8)*scale;p[1]=p[1]*scale-6;p[2]=8+(p[2]-8)*scale
 m={'textures':tex,'elements':out,'ambientocclusion':False}
 if id in ['school_crystal_ball','school_potion_display','school_round_window','school_fortune_altar']:m['render_type']='minecraft:translucent'
 if id=='school_emblem':m['render_type']='minecraft:cutout'
 return m
locale={}
for id,kind in entries:
 m=model(id,kind);write(MODEL/(id+'.json'),m)
 write(AS/'blockstates'/(id+'.json'),{'variants':{f'facing={face}':dict(model='chao_garden:block/'+id,**({'y':rot} if rot else {})) for face,rot in [('south',0),('west',90),('north',180),('east',270)]}})
 write(AS/'models/item'/(id+'.json'),{'parent':'chao_garden:block/'+id,'display':{'gui':{'rotation':[30,225,0],'scale':([1.8,1.8,1.8] if kind=='SMALL' else [.5,.5,.5]),'translation':([0,18,0] if kind=='SMALL' else [0,0,0])}}})
 write(RES/'data/chao_garden/loot_table/blocks'/(id+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'chao_garden:'+id}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 locale['block.chao_garden.'+id]=id.removeprefix('school_').replace('_',' ').title()
# Models that use only shared colors do not require a private base PNG, but always bind a real texture.
for id,kind in entries:
 p=MODEL/(id+'.json');m=json.loads(p.read_text());base=m.get('textures',{}).get('base')
 if base and not (AS/'textures'/(base.split(':',1)[1]+'.png')).exists():m['textures']['base']='chao_garden:block/school025/cream';write(p,m)
 if 'textures' in m and 'base' in m['textures']:m['textures']['particle']=m['textures']['base'];write(p,m)
atlas_meta={}
for id,columns,rows in [('school_lobby_composite',9,13),('school_cloud_composite',4,4),('school_mountain_composite',8,5)]:
 count=columns*rows;atlas_meta[id]={'columns':columns,'rows':rows,'usedTiles':count,'stateMin':0,'stateMax':127,'fallback':0,'texture':'chao_garden:block/school025/'+id}
 variants={}
 for tile in range(count):
  x=tile%columns;y=tile//columns;uv=[x*16/columns,y*16/rows,(x+1)*16/columns,(y+1)*16/rows]
  m={'textures':{'base':'chao_garden:block/school025/'+id,'particle':'chao_garden:block/school025/'+id},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':{face:{'texture':'#base','uv':([uv[0],uv[3],uv[2],uv[1]] if face=='down' else uv),'cullface':face} for face in ['north','south','east','west','up','down']}}],'ambientocclusion':False}
  write(MODEL/(id+'_tile_'+str(tile)+'.json'),m)
  if tile==0:write(MODEL/(id+'.json'),m)
 for tile in range(128):
  for face,rotation in [('south',0),('west',90),('north',180),('east',270)]:variants[f'facing={face},tile={tile}']=dict(model='chao_garden:block/'+id+'_tile_'+str(tile if tile<count else 0),**({'y':rotation} if rotation else {}))
 write(AS/'blockstates'/(id+'.json'),{'variants':variants})
# Every interactive element is wholly inside its owning voxel. Minecraft clips rays
# cell by cell, so oversized leaves owned only by the jamb cannot be targeted reliably.
# U is evaluated from the physical hinge-to-free-edge distance, not copied between
# face names: FaceInfo's east/west vertex order reverses when a leaf swings open.
def door_slice(a,b,hinge,opened,section,part):
 e=el(a,b,'edge');v0=(48-(part*16+b[1]))/3;v1=(48-(part*16+a[1]))/3
 if opened:
  distance=lambda z:z+(8 if section=='tip' else -8)
  u0=distance(a[2])*16/23;u1=distance(b[2])*16/23
  front='west' if hinge=='left' else 'east';back='east' if hinge=='left' else 'west'
  # A left leaf's original SOUTH front rotates WEST, and a right leaf's rotates EAST.
  # Native east vertex0 uses MAX_Z, west vertex0 uses MIN_Z.
  e['faces'][front]={'texture':'#base','uv':([u1,v0,u0,v1] if front=='east' else [u0,v0,u1,v1])}
  e['faces'][back]={'texture':'#back','uv':([16-u1,v0,16-u0,v1] if back=='east' else [16-u0,v0,16-u1,v1])}
 else:
  offset=(16 if hinge=='left' else -16) if section=='center' else 0
  distance=lambda x:(x+offset-1 if hinge=='left' else 15-x-offset)
  u0=distance(a[0])*16/23;u1=distance(b[0])*16/23
  # Native south vertex0 uses MIN_X, north vertex0 uses MAX_X.
  e['faces']['south']={'texture':'#base','uv':[u0,v0,u1,v1]}
  e['faces']['north']={'texture':'#back','uv':[16-u1,v0,16-u0,v1]}
 return e
def cabinet_case_slice(a,b):
 # Physical-coordinate UVs keep brushed steel at one scale on narrow case edges.
 e=el(a,b,'case');x0,y0,z0=a;x1,y1,z1=b
 for face,uv in {'north':[16-x1,16-y1,16-x0,16-y0],'south':[x0,16-y1,x1,16-y0],
                 'east':[16-z1,16-y1,16-z0,16-y0],'west':[z0,16-y1,z1,16-y0],
                 'up':[x0,z0,x1,z1],'down':[x0,16-z1,x1,16-z0]}.items():e['faces'][face]['uv']=uv
 return e
for id,kind in entries:
 if kind!='DOOR':continue
 # Retire the old oversized per-leaf model files; their native variants no longer exist.
 for old_hinge in ['left','right']:
  for old_open in ['open','closed']:
   for old_part in range(3):(MODEL/(f'{id}_{old_hinge}_{old_open}_{old_part}.json')).unlink(missing_ok=True)
 variants={}
 for hinge in ['left','right']:
  for opened in [False,True]:
   for section in ['leaf','center','tip']:
    for part in range(3):
     elements=[]
     if section=='center' and not opened:
      elements=[door_slice([0,0,7],[8,16,9],'left',False,section,part),door_slice([8,0,7],[16,16,9],'right',False,section,part)]
     elif section=='leaf':
      if opened:a,b=([0,0,8],[2,16,16]) if hinge=='left' else ([14,0,8],[16,16,16])
      else:a,b=([1,0,7],[16,16,9]) if hinge=='left' else ([0,0,7],[15,16,9])
      elements=[door_slice(a,b,hinge,opened,section,part)]
     elif section=='tip' and opened:
      a,b=([0,0,0],[2,16,15]) if hinge=='left' else ([14,0,0],[16,16,15])
      elements=[door_slice(a,b,hinge,opened,section,part)]
     if id=='school_tool_cabinet':
      y0=2 if part==0 else 0;y1=14 if part==2 else 16
      # Clip the moving steel leaves to the interior of the permanent case.
      for e in elements:
       e['from'][1]=y0;e['to'][1]=y1
       replacement=door_slice(e['from'],e['to'],('right' if section=='center' and e['from'][0]==8 else 'left') if section=='center' else hinge,opened,section,part)
       e['faces']=replacement['faces']
      if section!='tip':
       if part==0:elements.append(cabinet_case_slice([0,0,0],[16,2,10]))
       if part==2:elements.append(cabinet_case_slice([0,14,0],[16,16,10]))
       if section=='leaf':elements.append(cabinet_case_slice([0 if hinge=='left' else 15,y0,0],[1 if hinge=='left' else 16,y1,7]))
     suffix=f'{hinge}_{"open" if opened else "closed"}_{section}_{part}';name=id+'_'+suffix
     m={'textures':{'base':'chao_garden:block/school025/'+id,'back':'chao_garden:block/school025/'+id+'_back','edge':'chao_garden:block/school025/'+id+'_edge','particle':'chao_garden:block/school025/'+id},'elements':elements,'ambientocclusion':False}
     if id=='school_tool_cabinet':m['textures']['case']='chao_garden:block/school025/'+id+'_case'
     write(MODEL/(name+'.json'),m)
     if hinge=='left' and not opened and section=='leaf' and part==0:write(MODEL/(id+'.json'),m)
     for facing,rotation in [('south',0),('west',90),('north',180),('east',270)]:
      variants[f'facing={facing},hinge={hinge},open={str(opened).lower()},section={section},segment={part}']=dict(model='chao_garden:block/'+name,**({'y':rotation} if rotation else {}))
 write(AS/'blockstates'/(id+'.json'),{'variants':variants})
 if id=='school_tool_cabinet':
  # The creative item presents the complete cabinet, including its metal case.
  inventory=[]
  for hinge,section,dx in [('left','leaf',-16),('left','center',0),('right','leaf',16)]:
   for part in range(3):
    piece=json.loads((MODEL/f'{id}_{hinge}_closed_{section}_{part}.json').read_text())
    for e in piece['elements']:
     e['from']=[(e['from'][0]+dx+16)/3,(e['from'][1]+part*16)/3,e['from'][2]/3+6]
     e['to']=[(e['to'][0]+dx+16)/3,(e['to'][1]+part*16)/3,e['to'][2]/3+6];inventory.append(e)
  write(MODEL/(id+'.json'),{'textures':m['textures'],'elements':inventory,'ambientocclusion':False})
write(AS/'kindergarten_decor025.json',{'version':25,'blocks':[dict(id=id,kind=kind,item=True,**({'atlas':atlas_meta[id]} if id in atlas_meta else {})) for id,kind in entries],'texturePrefix':'block/school025/'})
write(ROOT/'art/locale-kindergarten025.json',locale)
print(f'Kindergarten025: {len(entries)} native decor blocks/items; composed rug, large cloud mural and arched frames generated.')
