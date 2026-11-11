#!/usr/bin/env python3
"""Decode supplied CWE PAK/DDS and SA2MDL/PE assets offline; never execute DLLs.
Formats follow X-Hax/sa_tools ModelFile, PolyChunk, VertexChunk and PAK sources.
"""
from pathlib import Path
import struct,json,math,io,re
import numpy as np
from PIL import Image
R=Path(__file__).resolve().parent/'cwe02516'
def pak(path,out):
 b=path.read_bytes();assert b[:4]==b'\x01pak';p=0x39
 def integer():
  nonlocal p
  v=struct.unpack_from('<I',b,p)[0];p+=4;return v
 def string():
  nonlocal p
  n=integer();v=b[p:p+n].decode();p+=n;return v
 n=integer();entries=[]
 for _ in range(n):
  long,name=string(),string();size,other=integer(),integer();assert size==other;entries.append((name.split('\\')[-1].lower(),size))
 data={}
 for name,size in entries:data[name]=b[p:p+size];p+=size
 assert p==len(b)
 info=next(v for k,v in data.items() if k.endswith('.inf'));assert len(info)%60==0
 names=[info[i:i+28].split(b'\0')[0].decode().lower() for i in range(0,len(info),60)]
 out.mkdir(parents=True,exist_ok=True)
 for name in names:Image.open(io.BytesIO(data[name+'.dds'])).convert('RGBA').save(out/(name+'.png'))
 return names
class Binary:
 def __init__(self,b,pe=False):
  self.b=b;self.sections=[]
  if pe:
   h=struct.unpack_from('<I',b,0x3c)[0];n=struct.unpack_from('<H',b,h+6)[0];opt=h+24;self.base=struct.unpack_from('<I',b,opt+28)[0];sec=opt+struct.unpack_from('<H',b,h+20)[0]
   for i in range(n):
    p=sec+i*40;size,va,rawsize,raw=struct.unpack_from('<4I',b,p+8);self.sections.append((self.base+va,size,raw,rawsize))
 def at(self,p):
  if not self.sections:return p
  for va,size,raw,rawsize in self.sections:
   if va<=p<va+size:assert p-va<rawsize;return p-va+raw
  raise ValueError(('Pointer outside PE sections',hex(p)))
 def v(self,fmt,p):return struct.unpack_from('<'+fmt,self.b,self.at(p))
 def u(self,p):return self.v('I',p)[0]
 def h(self,p):return self.v('H',p)[0]
 def matrix(self,p):
  flags=self.u(p);pos=self.v('3f',p+8);rot=self.v('3i',p+20);scale=self.v('3f',p+32);m=np.eye(4)
  if not flags&1:m[:3,3]=pos
  if not flags&2:
   for a in ((2,1,0) if flags&0x20 else (1,0,2)):
    v=rot[a]*math.tau/65536;c,s=math.cos(v),math.sin(v);r=np.eye(4);i,j=[k for k in range(3) if k!=a]
    if a==1:s=-s
    r[i,i]=r[j,j]=c;r[i,j]=-s;r[j,i]=s;m=m@r
  if not flags&4:m=m@np.diag([*scale,1])
  return m
 def extract(self,addr,names,config=None):
  faces=[];nodes=[];seen=set();cache={}
  slots={}
  if config:
   cs=config.get('color_slots',{});colors=cs.get('colors',[])
   for e in cs.get('entries',[]):slots[e['node_index'],e['material_index']]=[*bytes.fromhex(colors[e['slot_index']].lstrip('#')),255]
  def visit(p,parent,siblings=False):
   if not p:return
   assert p not in seen;seen.add(p);idx=len(nodes);m=parent@self.matrix(p);at=self.u(p+4);nodes.append({'index':idx,'address':hex(p),'matrix':m.tolist()});start=len(faces)
   if at:
    q=self.u(at);local={}
    while q:
     head=self.u(q);typ=head&255
     if typ==255:break
     count=self.u(q+4)>>16;offset=self.u(q+4)&65535;stride={44:28,41:24,35:16,34:12,33:32,32:16}.get(typ)
     assert stride is not None,('unsupported vertex',typ,hex(p))
     for i in range(count):
      xyz=self.v('3f',q+8+i*stride);world=(m@np.array([*xyz,1]))[:3].tolist()
      if typ==44:
       flags=self.u(q+8+i*stride+24);vi=offset+(flags&65535);weight=((flags>>16)&255)/255
       if ((head>>8)&3)==0:cache[vi]=[v*weight for v in world]
       else:cache[vi]=[a+v*weight for a,v in zip(cache[vi],world)]
      else:local[offset+i]=xyz;cache[offset+i]=world
     q+=(head>>16)*4+4
    q=self.u(at+4);texture=0;wrap=0;color=[255]*4;material=-1
    while q:
     head=self.h(q);typ=head&255
     if typ==255:break
     if typ in (8,9):texture=self.h(q+2)&8191;wrap=head>>8;q+=4;continue
     if typ<8:
      assert typ not in (4,5),('polygon cache not supported',typ);q+=2;continue
     end=q+4+2*self.h(q+2)
     if 16<=typ<=31:
      material+=1
      if typ&1:
       c=self.u(q+4);color=[(c>>16)&255,(c>>8)&255,c&255,c>>24]
      color=slots.get((idx,material),color)
     elif typ in (64,65,66):
      header=self.h(q+4);r=q+6
      for _ in range(header&16383):
       count=self.v('h',r)[0];r+=2;corners=[]
       for i in range(abs(count)):
        vi=self.h(r);r+=2;uv=[.5,.5]
        if typ!=64:uv=[v/(256 if typ==65 else 1024) for v in self.v('2h',r)];r+=4
        if i>=2:r+=2*(header>>14)
        corners.append((vi,uv))
       for i in range(2,len(corners)):
        tri=[corners[j] for j in (i-2,i-1,i)]
        if (i%2==1)!=(count<0):tri.reverse()
        points=[cache[j] for j,_ in tri]
        if np.linalg.norm(np.cross(np.array(points[1])-points[0],np.array(points[2])-points[0]))<1e-8:continue
        faces.append({'points':points,'local_points':[list(local[j]) for j,_ in tri] if all(j in local for j,_ in tri) else None,'uv':[uv for _,uv in tri],'texture':names[texture] if typ!=64 else None,'color':color,'wrap_flags':wrap,'double_sided':bool(head&0x1000),'node':idx})
      assert r<=end,(typ,r,end)
     else:raise ValueError(('unsupported poly',typ,hex(p)))
     q=end
   nodes[idx]['faces']=len(faces)-start
   child,sib=self.v('2I',p+44);visit(child,m,True)
   if siblings:visit(sib,parent,True)
  visit(addr,np.eye(4));return {'faces':faces,'nodes':nodes}
def main():
 names=pak(R/'original/CWE_OBJECT.pak',R/'textures');(R/'texture-order.json').write_text(json.dumps(names,indent=2)+'\n');items={}
 for p in sorted((R/'original/Accessories').glob('*.json')):
  cfg=json.loads(p.read_text());b=(p.parent/cfg['model']).read_bytes();assert b[:6]==b'SA2MDL'
  items[p.stem]=Binary(b).extract(struct.unpack_from('<I',b,8)[0],names,cfg);items[p.stem]['config']=cfg
  print(p.stem,len(items[p.stem]['faces']),'faces',[(n['index'],n['faces']) for n in items[p.stem]['nodes'] if n['faces']])
 (R/'accessories.json').write_text(json.dumps(items,separators=(',',':'))+'\n')
 toy_names=pak(R/'original/al_toy.pak',R/'textures/toy');toy_names=['toy/'+n for n in toy_names]
 binary=Binary((R/'original/CWE.dll').read_bytes(),True);objects={}
 for name,source in json.loads((R/'object-symbols.json').read_text()).items():
  objects[name]=binary.extract(int(source['virtual_address'],16),toy_names if name=='chao_accordion' else names)
  objects[name]['source']=source
 (R/'objects.json').write_text(json.dumps(objects,separators=(',',':'))+'\n')
 print('Extracted',len(objects),'named DLL objects without executing game code')
if __name__=='__main__':main()
