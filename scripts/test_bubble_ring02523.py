"""Check the baked OBJ aperture and the placed-pose export reference points."""
from pathlib import Path
import json,re
import numpy as np
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/chao_garden/models/sa2_02515'
def read(path):
 v=[];f=[]
 for line in path.read_text().splitlines():
  s=line.split()
  if s and s[0]=='v':v.append(tuple(map(float,s[1:])))
  if s and s[0]=='f':f.append([int(x.split('/')[0])-1 for x in s[1:]])
 return np.array(v),f
v,faces=read(assets/'chao_bubble_wand.obj')
def hit(x,y,a,b,c):
 signs=[(q[0]-r[0])*(y-r[1])-(q[1]-r[1])*(x-r[0]) for q,r in [(a,b),(b,c),(c,a)]]
 return min(signs)>1e-9 or max(signs)<-1e-9
for x,y in [(.5,.83),(.53,.83),(.47,.83),(.5,.86),(.5,.80)]:
 assert not any(hit(x,y,v[f[0]],v[f[i]],v[f[i+1]]) for f in faces for i in range(1,len(f)-1))
assert any(hit(.598,.835,v[f[0]],v[f[i]],v[f[i+1]]) for f in faces for i in range(1,len(f)-1))
# OBJ vertices are duplicated per face. The final 320 are the uniformly sampled
# inner/outer ring, so their centroid measures the actual exported opening.
centres=[]
for path in sorted(assets.glob('chao_bubble_wand_*.obj')):
 p,f=read(path);assert len(p)==356 and f==faces
 centre=p[-320:].mean(axis=0);centres.append(centre)
 frame=1 if path.stem.endswith('_1') else 3 if path.stem.endswith('_3') else 0
 expected={0:(.5,.18845205,.72050828),1:(.50272276,.19030824,.72050828),3:(.49727724,.18302517,.72050828)}[frame]
 assert np.linalg.norm(centre-expected)<1e-6,(path,centre)
print('PASS: open aperture (5 rays), solid rim, all 5 placed poses share topology, actual ring centroids match placed-pose reference points within 1e-6 blocks.')
