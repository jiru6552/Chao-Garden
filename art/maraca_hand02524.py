"""Split the first instrument from the original SA2 pair; preserve every selected UV/material."""
from pathlib import Path
import json,math
root=Path(__file__).resolve().parents[1];assets=root/'src/main/resources/assets/chao_garden';p=assets/'models/sa2_02515/chao_maracas.obj'
lines=p.read_text().splitlines();verts=[list(map(float,l.split()[1:])) for l in lines if l.startswith('v ')];norms=[list(map(float,l.split()[1:])) for l in lines if l.startswith('vn ')];uvs=[l for l in lines if l.startswith('vt ')]
# The original lower and upper handle rings define its real centreline.
unique={tuple(v) for v in verts};bottom=[v for v in unique if v[2]<.1];top=[v for v in unique if .18<v[2]<.20]
mean=lambda a:[sum(v[i] for v in a)/len(a) for i in range(3)]
b,t=mean(bottom),mean(top);grip=[(a+c)/2 for a,c in zip(b,t)]
y=[t[i]-b[i] for i in range(3)];length=math.sqrt(sum(v*v for v in y));y=[v/length for v in y]
x=[1-y[0]*y[0],-y[0]*y[1],-y[0]*y[2]];length=math.sqrt(sum(v*v for v in x));x=[v/length for v in x]
z=[x[1]*y[2]-x[2]*y[1],x[2]*y[0]-x[0]*y[2],x[0]*y[1]-x[1]*y[0]]
axes=[x,y,z];dot=lambda a,b:sum(u*v for u,v in zip(a,b));out=['# One original SA2 maraca; actual stem grip normalized to (.5,.30,.5).','mtllib chao_garden:models/sa2_02515/materials.mtl'];count=0;material=''
for line in lines:
 if line.startswith('usemtl '):material=line
 if not line.startswith('f '):continue
 refs=[list(map(int,r.split('/'))) for r in line.split()[1:]]
 if not all(verts[r[0]-1][2]<.5 for r in refs):continue
 out.append(material)
 for vi,ti,ni in refs:
  local=[verts[vi-1][i]-grip[i] for i in range(3)];v=[dot(axis,local)+c for axis,c in zip(axes,[.5,.3,.5])];n=[dot(axis,norms[ni-1]) for axis in axes]
  out+=['v '+' '.join(f'{f:.8f}' for f in v),uvs[ti-1],'vn '+' '.join(f'{f:.8f}' for f in n)]
 out.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(count+1,count+len(refs)+1)));count+=len(refs)
assert count==132
(assets/'models/sa2_02515/chao_maraca_hand.obj').write_text('\n'.join(out)+'\n')
model=json.loads((assets/'models/item/sa2_02515/chao_maracas.json').read_text());model['model']='chao_garden:models/sa2_02515/chao_maraca_hand.obj';model.pop('display');(assets/'models/item/sa2_02515/chao_maraca_hand.json').write_text(json.dumps(model,indent=2)+'\n')
(root/'art/maraca_hand02524-landmarks.json').write_text(json.dumps({'source_grip':grip,'basis_rows':axes,'normalized_grip':[.5,.3,.5],'vertices':count},indent=2)+'\n')
print('Split one maraca:',count,'vertices; original pair unchanged; grip',grip)
