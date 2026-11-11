"""Offline original-stage geometry audit; format reference: X-Hax/sa_tools SAModel/GC.
Exports visual landtable triangles, excluding BASIC collision-only entries.
"""
import sys,json,struct,math
from pathlib import Path
import numpy as np
S=Path(__file__).resolve().parents[1];sys.path.insert(0,str(S/'art'))
from extract_sa2_02515 import prs,relocate,transform,texture_names
R=S/'art/sa2_02515'

def stage(name,address):
 b=relocate(prs((R/'original'/(name+'.prs')).read_bytes()));u=lambda p:struct.unpack_from('>I',b,p)[0];h=lambda p:struct.unpack_from('>H',b,p)[0]
 print(name,len(b),hex(address),b[address:address+32].hex())
 n,nvisual=h(address),h(address+2);p=u(address+16);objects=[]
 print('objects',n,nvisual,hex(p))
 def gc(obj,parent=None):
  if not obj:return []
  parent=np.eye(4) if parent is None else parent;m=parent@transform(b,obj);at=u(obj+4);faces=[]
  if at:
   vp=u(at);verts={}
   while b[vp]!=255:
    typ,stride,count=struct.unpack_from('>BBH',b,vp);pos=u(vp+8)
    if typ in (1,2):verts[typ]=[struct.unpack_from('>fff',b,pos+i*stride) for i in range(count)]
    vp+=16
   flags=0;tex=0
   for off,cnt in [(u(at+8),h(at+16)),(u(at+12),h(at+18))]:
    for i in range(cnt):
     row=off+16*i;params,num,pp,size=struct.unpack_from('>IIII',b,row)
     for j in range(num):
      k=params+j*8;t=b[k];v=u(k+4)
      if t==1:flags=v
      if t==8:tex=v&65535
     end=pp+size
     while pp<end and b[pp]:
      kind=b[pp];count=h(pp+1);pp+=3;indices=[]
      for j in range(count):
       k=h(pp) if flags&4 else b[pp];pp+=2 if flags&4 else 1;indices.append(k)
       for has,wide in [(32,16),(128,64),(2048,1024)]:
        if flags&has:pp+=2 if flags&wide else 1
      assert pp<=end
      if kind==0x90:tris=[indices[j:j+3] for j in range(0,count,3)]
      elif kind==0x98:tris=[[indices[j-2],indices[j-1],indices[j]] if j%2==0 else [indices[j-1],indices[j-2],indices[j]] for j in range(2,count)]
      elif kind==0xa0:tris=[[indices[0],indices[j-1],indices[j]] for j in range(2,count)]
      elif kind==0x80:tris=[t for j in range(0,count,4) for t in [[indices[j],indices[j+1],indices[j+2]],[indices[j],indices[j+2],indices[j+3]]]]
      else:raise ValueError(hex(kind))
      for tri in tris:faces.append({'points':[(m@np.array([*verts[1][j],1]))[:3].tolist() for j in tri],'texture_index':tex})
  child,sib=u(obj+44),u(obj+48)
  faces+=gc(child,m);faces+=gc(sib,parent);return faces
 for i in range(nvisual):
  row=p+i*32;obj=u(row+16);faces=gc(obj);objects.append({'object':hex(obj),'flags':hex(u(row+28)),'faces':faces})
 out={'stage':name,'landtable':hex(address),'objects':objects};(R/(name+'-mesh.json')).write_text(json.dumps(out,separators=(',',':')))
 print('triangles',sum(len(v['faces']) for v in objects))
for name,p in [('ChaoStgNeut',0x10740),('ChaoStgHero',0x18dd0),('ChaoStgDark',0x15ac0)]:stage(name,p)
