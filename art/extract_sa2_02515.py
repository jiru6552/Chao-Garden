#!/usr/bin/env python3
"""Offline source audit/adaptation of supplied SA2B PRS files. No runtime game dependency."""
from pathlib import Path
import ast,struct,json,math,sys
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parent; SOURCE=ROOT/'sa2_02515'
sys.path.insert(0,str(ROOT))
from fruit_forms02512 import normal
# Reuse only decoder definitions, without running the historical extractor.
module=ast.parse((ROOT/'extract_sa2_02514.py').read_text())
exec(compile(ast.Module(body=[n for n in module.body if isinstance(n,ast.FunctionDef) and n.name in ('prs','rgb565','decode')],type_ignores=[]),'decoder02514','exec'))
def relocate(data):
 b=bytearray(data);u=lambda p:struct.unpack_from('>I',b,p)[0]
 sections=[(u(u(16)+i*8)&~3,u(u(16)+i*8+4)) for i in range(u(12))]
 for i in range(u(40),u(40)+u(44),8):
  mod,r=u(i),u(i+4);target=0
  while True:
   delta,kind,section,addend=struct.unpack_from('>HBBI',b,r);r+=8
   if kind==203:break
   if kind==202:target=sections[section][0];continue
   target+=delta
   if mod==u(0) and kind==1 and sections[section][0]:struct.pack_into('>I',b,target,sections[section][0]+addend)
 return b

def texture_names(data):
 flags,count=struct.unpack_from('>HH',data,8);stride=2+(28 if flags&8 else 0)+(2 if flags&4 else 0)+(2 if flags&2 else 0)+(4 if flags&1 else 0)
 return [data[14+i*stride:42+i*stride].split(b'\0')[0].decode() for i in range(count)]

def chunk(b,addr,names):
 u=lambda p:struct.unpack_from('>I',b,p)[0];h=lambda p:struct.unpack_from('>H',b,p)[0]
 at=u(addr+4)
 if not at:return []
 p=u(at);verts={};norms={}
 while True:
  head=u(p);typ=head&255
  if typ==255:break
  count=u(p+4)>>16;offset=u(p+4)&65535;stride={41:24,35:16,34:16,33:16,32:12}.get(typ)
  if stride is None:raise ValueError(('vertex type',typ,hex(addr)))
  for i in range(count):
   verts[offset+i]=struct.unpack_from('>fff',b,p+8+i*stride);norms[offset+i]=struct.unpack_from('>fff',b,p+20+i*stride) if typ==41 else (0,0,0)
  p+=(head>>16)*4+4
 p=u(at+4);faces=[];mat=0;flags=0
 while True:
  head=h(p);typ=head&255
  if typ==255:break
  if typ in (8,9):mat=h(p+2)&8191;flags=head>>8;p+=4;continue
  if typ<8:p+=2;continue
  end=p+4+2*h(p+2)
  if typ in (64,65,66):
   header=h(p+4);q=p+6
   for _ in range(header&16383):
    count=struct.unpack_from('>h',b,q)[0];q+=2;corners=[]
    for i in range(abs(count)):
     idx=h(q);q+=2;uv=(.5,.5)
     if typ!=64:uv=tuple(v/(256 if typ==65 else 1024) for v in struct.unpack_from('>hh',b,q));q+=4
     if i>=2:q+=2*(header>>14)
     corners.append((idx,uv))
    for i in range(2,len(corners)):
     tri=[corners[j] for j in (i-2,i-1,i)]
     if (i%2==1) != (count<0):tri.reverse()
     ps=[verts[j] for j,_ in tri];nn=normal(ps)
     if sum(v*v for v in nn)<.9:continue
     avg=[sum(norms[j][a] for j,_ in tri) for a in range(3)]
     if sum(a*c for a,c in zip(nn,avg))<0:tri.reverse()
     faces.append({'points':[verts[j] for j,_ in tri],'uv':[uv for _,uv in tri],'texture':names[mat],'wrap_flags':flags,'double_sided':bool(head&0x1000),'node':hex(addr)})
   assert q<=end
  elif not 16<=typ<=31:raise ValueError(('poly',typ,hex(addr)))
  p=end
 return faces

def transform(b,p):
 flags=struct.unpack_from('>I',b,p)[0];pos=struct.unpack_from('>fff',b,p+8);rot=struct.unpack_from('>iii',b,p+20);scale=struct.unpack_from('>fff',b,p+32)
 m=np.eye(4)
 if not flags&1:m[:3,3]=pos
 if not flags&2:
  order=(2,1,0) if flags&0x20 else (1,0,2)
  # Ninja default local Euler ZXY (column vectors Y @ X @ Z), alternative ZYX.
  for a in order:
   v=rot[a]*math.tau/65536;c,s=math.cos(v),math.sin(v);r=np.eye(4);i,j=[k for k in range(3) if k!=a]
   if a==1:s=-s
   r[i,i]=r[j,j]=c;r[i,j]=-s;r[j,i]=s;m=m@r
 if not flags&4:m=m@np.diag([*scale,1])
 return m

def hierarchy(b,addr,names,parent=None,siblings=False):
 if not addr:return []
 parent=np.eye(4) if parent is None else parent;m=parent@transform(b,addr);faces=chunk(b,addr,names)
 for f in faces:f['points']=[(m@np.array([*p,1]))[:3].tolist() for p in f['points']]
 child,sib=struct.unpack_from('>II',b,addr+44)
 faces+=hierarchy(b,child,names,m,True)
 if siblings:faces+=hierarchy(b,sib,names,parent,True)
 return faces

TOYS={'chao_maracas':0x627f4,'chao_bell':0x632cc,'chao_drum':0x63a48,'chao_flute':0x64a9c,'chao_castanets':0x65138,'chao_trumpet':0x65974,'chao_cymbals':0x65f98,'chao_tambourine':0x6709c,'chao_rattle':0x6a620,'chao_bubble_wand':0x6ec20,'chao_rocking_horse':0x307950,'chao_radio':0x3086f4,'chao_tv':0x309798,'chao_coffin':0x30ad7c,'chao_organ':0x30b994,'chao_piano':0x30c8a0,'chao_lifesaver':0x30d15c}
HATS=dict(zip(['apple_hat','bucket_hat','empty_can_hat','cardboard_box_hat','flower_pot_hat','paper_bag_hat','pan_hat','stump_hat','watermelon_hat','red_wool_beanie','blue_wool_beanie','black_white_wool_beanie'],[0x2a0b70,0x2a133c,0x2a19cc,0x2a1f88,0x2a2ac0,0x2a2d7c,0x2a3a60,0x2a4478,0x2a56b4,0x29dbb0,0x29eb58,0x29fb00]))
def main():
 b=relocate(prs((ROOT/'sa2_02514/original/ChaoMain.prs').read_bytes()));names=texture_names(prs((ROOT/'sa2_02514/original/al_toy.prs').read_bytes()));items={}
 for n,p in TOYS.items():items[n]={'source_archive':'al_toy','object_offset':hex(p),'faces':hierarchy(b,p,names)}
 names=texture_names(prs((SOURCE/'original/al_body.prs').read_bytes()))
 hats={n:{'source_archive':'al_body','object_offset':hex(p),'faces':hierarchy(b,p,names)} for n,p in HATS.items()}
 (SOURCE/'hats.json').write_text(json.dumps(hats,separators=(',',':'))+'\n')
 (SOURCE/'toys.json').write_text(json.dumps(items,separators=(',',':'))+'\n');print('Extracted',len(items),'toys',[(n,len(v['faces'])) for n,v in items.items()])
if __name__=='__main__':main()
