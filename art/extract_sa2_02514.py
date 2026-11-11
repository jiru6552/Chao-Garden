#!/usr/bin/env python3
"""Re-extract the selected supplied SA2 references; no game executable required.

See sa2_02514/provenance.json for original member hashes and format references.
This is an offline source-art tool, never part of the Minecraft runtime.
"""
from pathlib import Path
import struct,json,math,sys
from PIL import Image
from fruit_forms02512 import normal
ROOT=Path(__file__).resolve().parent/'sa2_02514'
def prs(data):
 p=0;bits=0;left=0;out=bytearray()
 def bit():
  nonlocal p,bits,left
  if not left:bits=data[p];p+=1;left=8
  v=bits&1;bits>>=1;left-=1;return v
 while True:
  if bit():out.append(data[p]);p+=1;continue
  if bit():
   word=data[p]|data[p+1]<<8;p+=2
   if not word:break
   offset=(word>>3)-8192;length=word&7
   if length:length+=2
   else:length=data[p]+1;p+=1
  else:
   length=((bit()<<1)|bit())+2;offset=data[p]-256;p+=1
  if not -len(out)<=offset<0:raise ValueError('Invalid PRS back-reference')
  for _ in range(length):out.append(out[offset])
  if len(out)>100*1024*1024:raise ValueError('PRS expansion limit')
 return bytes(out)
def rgb565(v):return (((v>>11)&31)*255//31,((v>>5)&63)*255//63,(v&31)*255//31,255)
def decode(tex):
 fmt=tex[11];w,h=struct.unpack_from('>HH',tex,12);data=tex[16:];im=Image.new('RGBA',(w,h));pix=im.load();p=0
 if fmt==5:
  for by in range(0,h,4):
   for bx in range(0,w,4):
    for y in range(4):
     for x in range(4):
      v=struct.unpack_from('>H',data,p)[0];p+=2
      if v&0x8000:c=(((v>>10)&31)*255//31,((v>>5)&31)*255//31,(v&31)*255//31,255)
      else:c=(((v>>8)&15)*17,((v>>4)&15)*17,(v&15)*17,((v>>12)&7)*255//7)
      if bx+x<w and by+y<h:pix[bx+x,by+y]=c
 elif fmt==14:
  for by in range(0,h,8):
   for bx in range(0,w,8):
    for sy,sx in [(0,0),(0,4),(4,0),(4,4)]:
     a,b=struct.unpack_from('>HH',data,p);cols=[rgb565(a),rgb565(b)]
     if a>b:cols += [tuple((2*cols[0][i]+cols[1][i])//3 for i in range(3))+(255,),tuple((cols[0][i]+2*cols[1][i])//3 for i in range(3))+(255,)]
     else:cols += [tuple((cols[0][i]+cols[1][i])//2 for i in range(3))+(255,),(0,0,0,0)]
     for y in range(4):
      bits=data[p+4+y]
      for x in range(4):
       if bx+sx+x<w and by+sy+y<h:pix[bx+sx+x,by+sy+y]=cols[(bits>>(6-2*x))&3]
     p+=8
 else:raise ValueError(fmt)
 assert p<=len(data)
 return im
archives={p.stem:prs(p.read_bytes()) for p in (ROOT/'original').glob('*.prs')}
b=bytearray(archives['ChaoMain'])
def u32(p):return struct.unpack_from('>I',b,p)[0]
def u16(p):return struct.unpack_from('>H',b,p)[0]
sections=[(u32(u32(16)+i*8)&~3,u32(u32(16)+i*8+4)) for i in range(u32(12))]
for i in range(u32(40),u32(40)+u32(44),8):
 module,offset=u32(i),u32(i+4);target=0
 while True:
  delta,kind,section,addend=struct.unpack_from('>HBBI',b,offset);offset+=8
  if kind==203:break
  if kind==202:target=sections[section][0];continue
  target+=delta
  if module==u32(0) and kind==1 and sections[section][0]:struct.pack_into('>I',b,target,sections[section][0]+addend)
def names(archive):
 d=archives[archive];flags,count=struct.unpack_from('>HH',d,8);stride=2+(28 if flags&8 else 0)+(2 if flags&4 else 0)+(2 if flags&2 else 0)+(4 if flags&1 else 0)
 return [d[14+i*stride:42+i*stride].split(b'\0')[0].decode() for i in range(count)]
for archive in sorted(p.name for p in (ROOT/'textures').iterdir() if p.is_dir()):
 d=archives[archive];p=8+struct.unpack_from('<I',d,4)[0]
 for name in names(archive):
  length=struct.unpack_from('<I',d,p+4)[0];tex=d[p:p+8+length];p+=8+length
  dest=ROOT/'textures'/archive/(name+'.png')
  if dest.exists():decode(tex).save(dest)
 assert p==len(d)
MODELS={'garden_fruit':0x3a7b18,'hero_garden_fruit':0x3a7f0c,'dark_garden_fruit':0x3a8290,'strong_fruit':0x3a9180,'tasty_fruit':0x3a9a64,'round_fruit':0x3ab4d4,'triangle_fruit':0x3abde0,'square_fruit':0x3ac7b4,'heart_fruit':0x3ad2a4,'chao_fruit':0x3ad8f8,'smart_fruit':0x3ade84,'orange_fruit':0x3ae43c,'gba_orange_fruit':0x3ae43c,'gba_blue_fruit':0x3aebb0,'gba_pink_fruit':0x3af2b8,'gba_yellow_fruit':0x3af9d0,'gba_purple_fruit':0x3b0028,'gba_green_fruit':0x3b050c,'gba_red_fruit':0x3b0bdc,'mushroom':0x3b12ac,'mushroom_alt':0x3b12ac,'mint_candy':0x3b1ad4,'chao_watering_can':0x6d11c,'chao_shovel':0x6e508}
MODELS.update(dict(zip([x+'_fruit_seed' for x in ['strong','tasty','hero_garden','dark_garden','round','triangle','square']],[0x3c968c,0x3c9c50,0x3ca1ac,0x3ca784,0x3cac0c,0x3cb0ac,0x3cb694])))
def extract(addr,arc):
 attach=u32(addr+4);p=u32(attach);verts={};norms={}
 while True:
  h=u32(p);t=h&255
  if t==255:break
  count=u32(p+4)>>16;offset=u32(p+4)&65535
  assert t==41,(hex(addr),t)
  for i in range(count):verts[offset+i]=struct.unpack_from('>fff',b,p+8+i*24);norms[offset+i]=struct.unpack_from('>fff',b,p+20+i*24)
  p+=(h>>16)*4+4
 p=u32(attach+4);faces=[];mat=0;flags=0;nms=names(arc)
 while True:
  h=u16(p);t=h&255
  if t==255:break
  if t in (8,9):mat=u16(p+2)&8191;flags=h>>8;p+=4;continue
  if t<8:p+=2;continue
  end=p+4+2*u16(p+2)
  if t in (64,65,66):
   header=u16(p+4);q=p+6
   for _ in range(header&16383):
    count=struct.unpack_from('>h',b,q)[0];q+=2;corners=[]
    for i in range(abs(count)):
     idx=u16(q);q+=2;uv=(.5,.5)
     if t!=64:uv=tuple(v/(256 if t==65 else 1024) for v in struct.unpack_from('>hh',b,q));q+=4
     if i>=2:q+=2*(header>>14)
     corners.append((idx,uv))
    for i in range(2,len(corners)):
     tri=[corners[j] for j in (i-2,i-1,i)];ps=[verts[j] for j,_ in tri];nn=normal(ps)
     if sum(v*v for v in nn)<.9:continue
     avg=[sum(norms[j][a] for j,_ in tri) for a in range(3)]
     if sum(a*c for a,c in zip(nn,avg))<0:tri.reverse()
     faces.append({'points':[verts[j] for j,_ in tri],'uv':[uv for _,uv in tri],'texture':nms[mat],'wrap_flags':flags,'double_sided':bool(h&0x1000)})
   assert q<=end
  elif not 16<=t<=31:raise ValueError(t)
  p=end
 return faces
items={}
for name,address in MODELS.items():
 archive='al_toy' if name in ('chao_watering_can','chao_shovel') else 'al_object'
 items[name]={'source_archive':archive,'object_offset':hex(address),'faces':extract(address,archive)}
(ROOT/'items.json').write_text(json.dumps(items,separators=(',',':'))+'\n')
print('Re-extracted',len(items),'source objects and all selected textures.')
