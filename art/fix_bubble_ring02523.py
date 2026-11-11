"""Deterministic OBJ geometry edit: preserve the striped stem, replace the cone with an open loop."""
from pathlib import Path
import math
root=Path(__file__).resolve().parents[1]
p=root/'src/main/resources/assets/chao_garden/models/sa2_02515/chao_bubble_wand.obj'
original=root/'art/bubble02523-original.obj'
if not original.exists():original.write_bytes(p.read_bytes())
v=[];uv=[];faces=[]
for line in original.read_text().splitlines():
 s=line.split()
 if not s:continue
 if s[0]=='v':v.append(tuple(map(float,s[1:])))
 if s[0]=='vt':uv.append(tuple(map(float,s[1:])))
 if s[0]=='f':faces.append([tuple(int(x)-1 for x in a.split('/')) for a in s[1:]])
out=['# 0.25.23: open bubble ring; center=(.5,.83,.5), inner radius=.079.',
     'mtllib chao_garden:models/sa2_02515/materials.mtl','usemtl sa2_syabon01_52'];n=0

def face(points,tex):
 global n
 a,b,c=points[:3];u=[b[i]-a[i] for i in range(3)];w=[c[i]-a[i] for i in range(3)]
 normal=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]]
 length=math.sqrt(sum(x*x for x in normal));normal=[x/length for x in normal]
 for point,t in zip(points,tex):
  out.append('v '+' '.join(f'{x:.7f}' for x in point));out.append('vt '+' '.join(f'{x:.7f}' for x in t));out.append('vn '+' '.join(f'{x:.7f}' for x in normal))
 out.append('f '+' '.join(f'{n+i}/{n+i}/{n+i}' for i in range(1,len(points)+1)));n+=len(points)
for f in faces:
 points=[v[x[0]] for x in f]
 if max(q[1] for q in points)>.763:continue
 points=[(.5+(x-.5)*.35,.0625+(y-.0625)*(.65/.7),.5+(z-.5)*.35) for x,y,z in points]
 face(points,[uv[x[1]] for x in f])
def ring(i,r,z):
 a=2*math.pi*i/20
 return(.5+r*math.cos(a),.83+r*math.sin(a),z)
for i in range(20):
 j=i+1
 for points in [
  [ring(i,.118,.483),ring(i,.079,.483),ring(j,.079,.483),ring(j,.118,.483)],
  [ring(i,.118,.517),ring(j,.118,.517),ring(j,.079,.517),ring(i,.079,.517)],
  [ring(i,.118,.483),ring(j,.118,.483),ring(j,.118,.517),ring(i,.118,.517)],
  [ring(i,.079,.483),ring(i,.079,.517),ring(j,.079,.517),ring(j,.079,.483)]]:
  face(points,[(.5,.5)]*4)
p.write_text('\n'.join(out)+'\n')
print('open ring:',n,'vertices')
# Carry the same open loop into every placed-toy pose with the original export's
# exact affine transforms; retain these transforms so regeneration is idempotent.
import json
import numpy as np
matrix_file=root/'art/bubble02523-ground-transforms.json'
if matrix_file.exists():matrices=json.loads(matrix_file.read_text())
else:
 old=np.array([list(map(float,s.split()[1:])) for s in original.read_text().splitlines() if s.startswith('v ')])
 x=np.column_stack((old,np.ones(len(old))));matrices={}
 for ground in sorted(p.parent.glob('chao_bubble_wand_*.obj')):
  y=np.array([list(map(float,s.split()[1:])) for s in ground.read_text().splitlines() if s.startswith('v ')])
  m=np.linalg.lstsq(x,y,rcond=None)[0];assert np.max(np.abs(x@m-y))<1e-6
  matrices[ground.name]=m.tolist()
 matrix_file.write_text(json.dumps(matrices,indent=2)+'\n')
for name,matrix in matrices.items():
 m=np.array(matrix);normal=np.linalg.inv(m[:3]).T;lines=[]
 for line in out:
  fields=line.split()
  if fields and fields[0]=='v':
   point=np.array([*map(float,fields[1:]),1])@m;line='v '+' '.join(f'{n:.7f}' for n in point)
  elif fields and fields[0]=='vn':
   v=np.array(list(map(float,fields[1:])))@normal;v/=np.linalg.norm(v);line='vn '+' '.join(f'{n:.7f}' for n in v)
  lines.append(line)
 (p.parent/name).write_text('\n'.join(lines)+'\n')
print('placed poses:',len(matrices))
