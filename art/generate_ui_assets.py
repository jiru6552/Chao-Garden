"""Original Adventure-era-inspired Chao panels. No extracted game assets.
Run with Python and Pillow. All geometry is drawn at 3x then downsampled.
"""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw
import io
import os
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/chao_garden/textures/gui'
ROOT.mkdir(parents=True,exist_ok=True)
S=3

def canvas():return Image.new('RGBA',(600*S,360*S))
def draw(im):return ImageDraw.Draw(im)
def rr(d,box,r,fill,outline=None,width=1):
 d.rounded_rectangle(tuple(int(v*S) for v in box),int(r*S),fill=fill,outline=outline,width=width*S)
def ell(d,box,fill,outline=None,width=1):d.ellipse(tuple(int(v*S) for v in box),fill=fill,outline=outline,width=width*S)
def line(d,pts,fill,width):d.line([(int(x*S),int(y*S)) for x,y in pts],fill=fill,width=int(width*S))
def over(im,painter,mask=None):
 """Paint translucency over the shell instead of replacing its alpha."""
 layer=Image.new('RGBA',im.size);painter(draw(layer))
 if mask is not None:layer.putalpha(ImageChops.multiply(layer.getchannel('A'),mask))
 im.alpha_composite(layer)
def save_gui(im,size,path):
 """Antialias once, discarding invisible ringing outside the artwork."""
 im=im.resize(size,Image.Resampling.LANCZOS)
 invisible=im.getchannel('A').point(lambda alpha:255 if alpha<=2 else 0)
 im.paste((0,0,0,0),(0,0),invisible)
 payload=io.BytesIO();im.save(payload,format='PNG',optimize=True)
 data=payload.getvalue()
 with Image.open(io.BytesIO(data)) as check:check.verify()
 temporary=path.with_suffix('.png.writing')
 with temporary.open('wb') as handle:
  handle.write(data);handle.flush();os.fsync(handle.fileno())
 os.replace(temporary,path)
for name,accent,soft in [('status',(18,154,225),(220,246,255)),('genetics',(238,164,30),(255,246,211)),('animals',(228,104,170),(255,229,242)),('guide',(35,169,201),(225,250,251))]:
 im=canvas();d=draw(im)
 rr(d,(2,5,598,359),24,(1,22,66,120))
 rr(d,(0,0,598,355),23,(238,255,255,255))
 rr(d,(3,3,595,352),21,(24,113,193,252))
 # Blue sky/deep-water gradient, masked into the rounded shell.
 grad=Image.new('RGBA',im.size);gd=draw(grad)
 for y in range(4*S,352*S):
  t=(y/S-4)/348;c=tuple(round(a+(b-a)*t) for a,b in zip((31,177,228),(25,81,163)))
  gd.line((3*S,y,595*S,y),fill=c+(255,))
 mask=Image.new('L',im.size);md=draw(mask);rr(md,(3,3,595,352),21,255);im.alpha_composite(Image.composite(grad,Image.new('RGBA',im.size),mask));d=draw(im)
 # Rings and gloss stay inside the rounded shell. Drawing them on a separate
 # layer also keeps their translucent pixels from punching holes in the panel.
 decoration=canvas();d=draw(decoration)
 for cx,cy,r in [(552,9,40),(490,339,76),(576,260,48)]:
  ell(d,(cx-r,cy-r,cx+r,cy+r),None,(127,229,255,85),3)
  ell(d,(cx-r+7,cy-r+7,cx+r-7,cy+r-7),None,(7,80,165,55),2)
 d.polygon([(7*S,14*S),(318*S,4*S),(229*S,56*S),(6*S,56*S)],fill=(164,239,255,190))
 line(d,[(16,57),(584,57)],(174,245,255,190),1)
 decoration.putalpha(ImageChops.multiply(decoration.getchannel('A'),mask));im.alpha_composite(decoration);d=draw(im)
 # Ring logo badge.
 ell(d,(17,14,42,39),(22,101,182,255),(255,224,69,255),4)
 ell(d,(22,19,37,34),(43,161,212,255),(255,247,178,255),1)
 # Profile and status cards are gently rounded rather than square inventory panels.
 over(im,lambda dd:rr(dd,(16,71,190,313),15,(8,58,125,80)));d=draw(im)
 rr(d,(16,69,190,311),15,(241,253,255,255),(162,239,255,255),1)
 over(im,lambda dd:rr(dd,(201,71,584,313),15,(8,58,125,80)));d=draw(im)
 rr(d,(201,69,584,311),15,(248,253,255,255),(166,236,253,255),1)
 rr(d,(25,99,181,229),11,(211,242,251,255))
 # Portrait softly suggests the blue Health Center chart.
 ell(d,(45,113,163,224),(230,251,255,255),(168,224,242,255),1)
 ell(d,(58,126,150,215),None,(196,236,248,255),2)
 over(im,lambda dd:ell(dd,(57,207,151,221),(160,212,230,120)))
 for x,y,r in [(38,110,2),(164,121,3),(37,200,2),(161,194,2)]:over(im,lambda dd:ell(dd,(x-r,y-r,x+r,y+r),(255,255,255,210)))
 d=draw(im)
 # Orange round-ended placard is an Adventure UI cue. Page accent appears as small tab.
 rr(d,(212,80,572,99),8,(240,174,44,255))
 over(im,lambda dd:rr(dd,(214,81,570,88),7,(255,210,98,200)));d=draw(im)
 rr(d,(26,79,180,94),6,accent+(255,))
 # Guide retains its deliberate note well. Inspector pages draw their own
 # content and no longer inherit an unrelated blue rectangle beneath every tab.
 if name=='guide':
  rr(d,(211,245,574,302),8,soft+(255,))
  over(im,lambda dd:line(dd,[(221,247),(564,247)],accent+(150,),1))
 # Header waveform stripe.
 for x in range(407,557,7):over(im,lambda dd:line(dd,[(x,47),(x+4,42)],(153,234,255,100),1),mask)
 save_gui(im,(600,360),ROOT/f'adventure_{name}.png')
# Original symbols: swim, fly, run, power, stamina, heart, food, energy, DNA, animal.
im=Image.new('RGBA',(320*S,32*S));d=draw(im)
colors=[(28,165,215),(165,107,220),(77,181,81),(232,98,78),(231,175,29),(239,105,151),(244,161,50),(60,172,220),(133,113,216),(97,167,111)]
for i,c in enumerate(colors):
 ox=i*32
 ell(d,(ox+2,2,ox+30,30),c+(255,),(255,255,255,220),1)
 def poly(p):d.polygon([((ox+x)*S,y*S) for x,y in p],fill=(255,255,255,255))
 if i==0:
  poly([(16,6),(9,16),(9,21),(12,25),(20,25),(23,21),(23,16)]);line(d,[(ox+12,21),(ox+14,23)],c+(255,),1)
 elif i==1:
  poly([(5,13),(15,18),(15,23),(11,23),(9,19),(5,17)]);poly([(27,13),(17,18),(17,23),(21,23),(23,19),(27,17)])
 elif i==2:poly([(19,5),(10,17),(16,17),(13,27),(24,13),(17,13)])
 elif i==3:
  poly([(7,23),(7,14),(12,14),(12,8),(17,8),(17,13),(22,13),(26,18),(25,24),(20,27),(12,27)])
 elif i==4:
  poly([(16,5),(19,12),(27,13),(21,18),(23,26),(16,22),(9,26),(11,18),(5,13),(13,12)])
 elif i==5:
  ell(d,(ox+7,8,ox+17,18),(255,255,255,255));ell(d,(ox+15,8,ox+25,18),(255,255,255,255));poly([(7,14),(25,14),(16,26)])
 elif i==6:
  ell(d,(ox+8,10,ox+25,26),(255,255,255,255));line(d,[(ox+16,11),(ox+17,6)],(255,255,255,255),2);poly([(17,7),(23,5),(24,9),(19,10)])
 elif i==7:poly([(18,4),(8,18),(15,18),(13,28),(25,12),(18,12)])
 elif i==8:
  line(d,[(ox+10,6),(ox+21,14),(ox+10,24)],(255,255,255,255),2);line(d,[(ox+21,6),(ox+10,14),(ox+21,24)],(255,255,255,255),2)
  for yy in [8,12,17,21]:line(d,[(ox+12,yy),(ox+19,yy)],(255,255,255,255),1)
 elif i==9:
  ell(d,(ox+10,16,ox+23,26),(255,255,255,255))
  for xx,yy in [(8,13),(13,9),(20,9),(25,13)]:ell(d,(ox+xx-3,yy-3,ox+xx+3,yy+3),(255,255,255,255))
save_gui(im,(320,32),ROOT/'adventure_symbols.png')
print('Original Adventure panels and symbols generated')
