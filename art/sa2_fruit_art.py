"""Native pixel fruit art and stepped hanging solids from SA2 visual references.

Authored pixels; Chao Island's original SA2 renders were inspected for silhouette,
colors and distinguishing surfaces. No downloaded image pixels are embedded.
"""
from PIL import Image, ImageDraw

PALETTES = {
 'garden_fruit': ('#e6c729','#e6cf33'),
 'hero_garden_fruit': ('#f2eb8c','#e7e780'),
 'dark_garden_fruit': ('#b84015','#bb411a'),
 'strong_fruit': ('#f1851d','#ed861c'), 'tasty_fruit': ('#e6bd4f','#e7c259'),
 'heart_fruit': ('#e327a5','#e220a3'), 'chao_fruit': ('#23c6dc','#2ac9dc'),
 'mushroom': ('#63b41f','#65b821'), 'mushroom_alt': ('#63b41f','#65b821'),
 'round_fruit': ('#147be1','#117ce7'), 'square_fruit': ('#18df41','#1bdd3f'),
 'triangle_fruit': ('#ffbf1b','#ffbd1f'),
 'smart_fruit': ('#7d2eb5','#7d2eb5'), 'stamina_fruit': ('#a6d665','#a6d665'),
 'energy_fruit': ('#ef6812','#ef6812'), 'peace_fruit': ('#b8d654','#b8d654'),
 'tall_fruit': ('#0a9dbf','#0a9dbf'), 'vitality_fruit': ('#b77234','#b77234'),
 'rare_fruit': ('#ae8b38','#ae8b38'), 'wonder_fruit': ('#388aa3','#388aa3'),
 'creepy_fruit': ('#12a8a2','#12a8a2'), 'rough_fruit': ('#682872','#682872'),
 'forbidden_fruit': ('#8c7824','#8c7824'),
}

ADVENTURE_SHAPES={
 'energy_fruit':[(5,4),(10,4),(13,6),(14,9),(12,13),(9,15),(5,14),(2,10),(3,6)],
 'peace_fruit':[(8,2),(11,4),(12,7),(14,10),(12,14),(5,15),(2,11),(4,7),(5,4)],
 'smart_fruit':[(6,3),(10,3),(13,5),(12,8),(10,9),(13,12),(11,14),(5,15),(2,12),(4,9),(3,6)],
 'tall_fruit':[(8,1),(11,5),(14,10),(12,14),(7,15),(2,13),(1,10),(5,5)],
 'vitality_fruit':[(8,1),(11,5),(13,8),(14,13),(11,15),(4,15),(2,12),(3,7),(5,5)],
 'rare_fruit':[(8,1),(10,5),(14,8),(13,13),(8,15),(3,13),(2,8),(6,5)],
 'wonder_fruit':[(8,1),(11,5),(14,11),(12,14),(8,15),(3,14),(1,11),(4,5)],
 'creepy_fruit':[(6,2),(11,3),(14,7),(13,11),(10,15),(5,14),(2,10),(2,5)],
 'rough_fruit':[(8,1),(12,4),(11,8),(13,11),(8,15),(3,12),(4,8),(3,4)],
 'forbidden_fruit':[(8,1),(11,5),(13,8),(14,12),(10,15),(4,14),(1,11),(3,8),(5,4)],
}

def adventure_color(fruit,x,y):
 """Authored native pixels based on the inspected CA2 Dreamcast fruit renders."""
 base=rgb(PALETTES[fruit][0])
 if fruit=='energy_fruit':base=(142,190,18) if y<5 else (250,173,10) if y<8 else (224,72,8)
 elif fruit=='peace_fruit':base=(191,219,86) if y<8 else (149,178,69)
 elif fruit=='tall_fruit':base=(16,190,195) if x%5<2 else (20,78,139)
 elif fruit=='vitality_fruit':base=(51,160,67) if y<7 else (189,107,42) if y<12 else (124,73,36)
 elif fruit=='rare_fruit':base=(168,149,67) if y<9 else (225,93,12)
 elif fruit=='wonder_fruit':
  a=max(0,min(1,(y-2)/11));base=tuple(round(v*(1-a)+w*a) for v,w in zip((226,244,244),(22,61,102)))
  if x%4==0:base=shade(base,.78)
 elif fruit=='creepy_fruit':base=(194,28,54) if x%5 in (0,1) else (8,179,163)
 elif fruit=='rough_fruit':base=(129,49,121) if y<8 else (37,25,85)
 elif fruit=='forbidden_fruit':
  base=(44,22,74) if y<5 else ((139,120,32) if (x*3+y*5)%7<4 else (105,86,22))
 factor=1.13-x*.022-y*.006+(.08 if x<7 else 0)
 return shade(base,factor)

def adventure_sprite(fruit):
 im=Image.new('RGBA',(16,16));mask=Image.new('1',(16,16));ImageDraw.Draw(mask).polygon(ADVENTURE_SHAPES[fruit],fill=1)
 for y in range(16):
  for x in range(16):
   if not mask.getpixel((x,y)):continue
   edge=any(not(0<=u<16 and 0<=v<16) or not mask.getpixel((u,v)) for u,v in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)))
   c=adventure_color(fruit,x,y);im.putpixel((x,y),(*shade(c,.8 if edge else 1),255))
 d=ImageDraw.Draw(im)
 if fruit in ('energy_fruit','peace_fruit','smart_fruit'):
  d.line((8,1,8,4),fill='#657a2c')
  d.polygon([(8,2),(10,0),(13,1),(10,3)],fill='#76ac21')
  if fruit=='energy_fruit':
   d.polygon([(7,3),(3,1),(3,0),(6,1)],fill='#4ca918');d.polygon([(9,3),(14,3),(15,5),(12,4)],fill='#72bb1f')
 if fruit=='peace_fruit':
  d.line([(3,9),(6,10),(10,9),(13,10)],fill='#e0e9a8')
  d.line([(4,12),(8,13),(12,12)],fill='#e4eab5')
 if fruit=='smart_fruit':d.line([(4,8),(8,9),(11,8)],fill='#592274')
 return im

def shade(c,f): return tuple(max(0,min(255,round(x*f))) for x in c)
def rgb(s): return tuple(bytes.fromhex(s.lstrip('#')))

def sprite(fruit):
 if fruit in ADVENTURE_SHAPES:return adventure_sprite(fruit)
 im=Image.new('RGBA',(16,16));mask=Image.new('L',(16,16));m=ImageDraw.Draw(mask)
 shapes={
 'garden_fruit':[(6,2),(10,2),(12,5),(13,9),(11,13),(8,15),(4,13),(2,9),(3,5)],
 'hero_garden_fruit':[(8,1),(11,4),(13,9),(11,13),(8,15),(4,13),(2,9),(4,4)],
 'dark_garden_fruit':[(8,1),(12,5),(14,10),(10,15),(5,14),(2,9),(4,4)],
 'strong_fruit':[(8,1),(10,5),(13,8),(12,13),(8,15),(3,13),(2,8),(5,5)],
 'tasty_fruit':[(10,2),(12,2),(11,5),(12,8),(10,12),(6,15),(3,13),(2,10),(5,6),(8,5)],
 'dark_fruit':[(12,1),(14,2),(12,4),(13,7),(10,11),(6,13),(1,14),(4,11),(7,9),(9,5),(11,4)],
 'heart_fruit':[(3,4),(6,3),(8,5),(11,3),(14,5),(13,10),(10,13),(8,15),(4,12),(2,8)],
 'chao_fruit':[(7,3),(10,4),(11,7),(13,10),(11,13),(8,14),(4,13),(2,10),(4,6)],
 'round_fruit':[(6,4),(10,4),(13,7),(14,10),(11,14),(6,15),(2,12),(1,8),(3,5)],
 'square_fruit':[(3,4),(12,4),(13,13),(4,15),(2,12),(2,6)],
 'triangle_fruit':[(7,3),(9,4),(14,14),(1,14)],
 }
 if fruit.startswith('mushroom'):
  m.rectangle((6,8,10,14),fill=255);m.polygon([(2,9),(2,6),(4,3),(8,1),(12,3),(14,7),(14,9)],fill=255)
 elif fruit=='hero_fruit':m.polygon([(5,3),(10,2),(13,5),(15,9),(12,13),(7,15),(3,13),(1,9),(2,6)],fill=255)
 else:m.polygon(shapes.get(fruit,shapes['garden_fruit']),fill=255)
 base=rgb(PALETTES[fruit][0])
 for y in range(16):
  for x in range(16):
   if mask.getpixel((x,y)):
    factor=1.13-(x*.023+y*.009)+(.10 if x<6 and y<8 else 0)
    if fruit=='strong_fruit':
     base2=(207,54,8) if y<7 else (248,174+min(50,(y-7)*6),36)
    elif fruit.startswith('mushroom') and y>=10:base2=(234,205,147)
    else:base2=base
    edge=any(not(0<=xx<16 and 0<=yy<16) or mask.getpixel((xx,yy))==0 for xx,yy in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)))
    im.putpixel((x,y),(*shade(base2,factor*(.76 if edge else 1)),255))
 d=ImageDraw.Draw(im)
 if fruit in ('round_fruit','square_fruit','triangle_fruit','chao_fruit','heart_fruit'):
  d.line((8,1,8,4),fill='#68852a');d.polygon([(9,2),(12,1),(14,3),(11,3)],fill='#6dc329')
 if fruit=='hero_garden_fruit':
  for x in (5,8,11):d.line((x,5,x-1,11),fill='#d8cf65')
 if fruit=='dark_garden_fruit':
  for x,y in ((5,4),(9,4),(3,7),(7,7),(11,7),(5,10),(9,10),(7,13)):
   d.line((x,y,x+2,y+1),fill='#efb43b');d.point((x,y+1),fill='#822109')
 if fruit.startswith('mushroom'):
  for box in ((4,4,6,5),(10,5,12,6),(7,2,9,3),(2,7,3,8)):d.ellipse(box,fill='#f4f2d9')
 if fruit=='dark_fruit':d.line((12,1,12,3),fill='#a62b11');d.line((10,5,9,7),fill='#ef6a22')
 return im


def skin(fruit,age=3):
 if fruit in ADVENTURE_SHAPES:
  im=Image.new('RGBA',(16,16))
  for y in range(16):
   for x in range(16):
    c=adventure_color(fruit,x,y) if age==3 else (82+age*16,122+age*12,64+age*6)
    if fruit=='peace_fruit' and age==3 and y in (7,11):c=(219,230,160)
    im.putpixel((x,y),(*c,255))
  return im
 color=rgb(PALETTES[fruit][1]);im=Image.new('RGBA',(16,16));p=im.load()
 for y in range(16):
  for x in range(16):
   c=color if age==3 else (92+age*12,139+age*8,69+age*10)
   if fruit=='strong_fruit' and age==3:c=(222,73+int(y*9),18+int(y*2))
   f=1.08-(y/16)*.23+(.07 if (x*3+y*5)%17<3 else 0)
   p[x,y]=(*shade(c,f),255)
 d=ImageDraw.Draw(im)
 if age==3:
  if fruit.startswith('mushroom'):
   for x,y in ((2,2),(10,3),(5,10),(13,12)):d.ellipse((x,y,x+2,y+1),fill='#f3edd0')
  elif fruit=='dark_garden_fruit':
   for y in range(1,16,4):
    for x in range((y//4)%2*3,16,6):d.line((x,y,x+3,y+2),fill='#eea13d')
  elif fruit=='hero_garden_fruit':
   for x in (2,6,10,14):d.line((x,1,x,15),fill='#d9ce6a')
 return im


def solids(fruit, scale=1):
 """Fruit solids centered at(8,8,8); caller adds hanging branch stalk."""
 def b(x1,y1,z1,x2,y2,z2,tex='fruit'):
  return ([8+(x1-8)*scale,8+(y1-8)*scale,8+(z1-8)*scale],
          [8+(x2-8)*scale,8+(y2-8)*scale,8+(z2-8)*scale],tex)
 if fruit=='smart_fruit':return [b(4,3,4,12,6,12),b(5.5,6,5.5,10.5,9,10.5),b(3.5,9,3.5,12.5,12,12.5),b(5,12,5,11,14,11)]
 if fruit=='rough_fruit':return [b(6,3,6,10,5,10),b(4,5,4,12,9,12),b(5,9,5,11,11,11),b(3.5,11,3.5,12.5,13,12.5),b(6,13,6,10,15,10)]
 if fruit in ('tall_fruit','vitality_fruit','rare_fruit','wonder_fruit','forbidden_fruit'):
  return [b(5,3,5,11,5,11),b(3,5,3,13,9,13),b(4,9,4,12,11,12),b(5,11,5,11,13,11),b(6.5,13,6.5,9.5,15,9.5)]
 if fruit in ('energy_fruit','peace_fruit','creepy_fruit'):
  return [b(5,3,5,11,5,11),b(3,5,3,13,11,13),b(4.5,11,4.5,11.5,13,11.5),b(6,13,6,10,15,10)]
 if fruit=='square_fruit':return [b(4,4,4,12,12,12)]
 if fruit=='triangle_fruit':return [b(3,4,4,13,6,12),b(4,6,5,12,8,11),b(5,8,6,11,10,10),b(6,10,6.5,10,12,9.5),b(7,12,7,9,14,9)]
 if fruit=='dark_fruit':return [b(3,4,6,6,6,9),b(5,5,5,9,8,10),b(8,7,5,11,11,11),b(10,10,6,13,13,10),b(11,13,7,12.5,15,9)]
 if fruit.startswith('mushroom'):return [b(6.5,3,6.5,9.5,8,9.5,'stem'),b(3,8,3,13,10,13),b(4,10,4,12,12,12),b(6,12,6,10,13,10)]
 if fruit=='heart_fruit':return [b(6.5,3,6,9.5,5,10),b(5,5,5,11,8,11),b(3,8,4,8,12,12),b(8,8,4,13,12,12),b(4,12,5,7,13,11),b(9,12,5,12,13,11)]
 if fruit in ('chao_fruit','tasty_fruit'):return [b(5,3,5,11,5,11),b(3.5,5,3.5,12.5,9,12.5),b(5,9,5,11,11,11),b(6,11,6,10,13,10),b(7,13,7,9,15,9)]
 if fruit=='strong_fruit':return [b(4,3,4,12,7,12),b(3,7,3,13,10,13),b(4.5,10,4.5,11.5,12,11.5),b(6,12,6,10,14,10),b(7,14,7,9,15,9)]
 if fruit=='hero_garden_fruit':return [b(5,3,5,11,5,11),b(3,5,3,13,10,13),b(4,10,4,12,12,12),b(5,12,5,11,14,11),b(7,14,7,9,15,9)]
 return [b(5,3,5,11,4.5,11),b(3.5,4.5,3.5,12.5,11.5,12.5),b(5,11.5,5,11,13,11)]
