"""Rigidly reorient one supplied SA2 cymbal, preserving its triangles and UVs.
Canonical X points out of the contact face toward the hand. Its rim is X=.5.
The same single-disc mesh is used for both hands, by proper rotations only.
"""
from pathlib import Path
import json,numpy as np
root=Path(__file__).resolve().parents[1];assets=root/'src/main/resources/assets/chao_garden'
v=[];uv=[];norm=[];faces=[];material=''
for line in (assets/'models/sa2_02515/chao_cymbals.obj').read_text().splitlines():
 if line.startswith('v '):v.append(list(map(float,line.split()[1:])))
 elif line.startswith('vt '):uv.append(list(map(float,line.split()[1:])))
 elif line.startswith('vn '):norm.append(list(map(float,line.split()[1:])))
 elif line.startswith('usemtl '):material=line.split()[1]
 elif line.startswith('f '):faces.append((material,[[int(i)-1 for i in a.split('/')] for a in line.split()[1:]]))
v=np.array(v);norm=np.array(norm)
chosen=[(m,f) for m,f in faces if np.mean(v[[a[0] for a in f],2])<.5];assert len(chosen)==60
plate=np.unique(v[[a[0] for m,f in chosen if m=='sa2_rappa_gold_52' for a in f]],axis=0);center=plate.mean(0)
_,axes=np.linalg.eigh((plate-center).T@(plate-center));out=axes[:,0];out*=1 if out[1]>0 else -1
vertical=np.array([1.,0,0]);vertical-=out*np.dot(vertical,out);vertical/=np.linalg.norm(vertical);depth=np.cross(out,vertical);matrix=np.stack([out,vertical,depth]);assert np.linalg.det(matrix)>.999
allpoints=np.unique(v[[a[0] for m,f in chosen for a in f]],axis=0);local=(allpoints-center)@matrix.T
radius=max(np.ptp(local[:,1]),np.ptp(local[:,2]))/2;scale=1/(2*radius);front=local[:,0].min()
def point(p):return (matrix@(p-center)-np.array([front,0,0]))*scale+np.array([.5,.5,.5])
handle=np.unique(v[[a[0] for m,f in chosen if m=='sa2_star01_52' for a in f]],axis=0);grip=point(handle.mean(0))
output=['# Single rigid cymbal from the supplied SA2 pair; all 60 triangles and UVs retained.','mtllib chao_garden:models/sa2_02515/materials.mtl'];count=0;last=''
for mat,face in chosen:
 if mat!=last:output.append('usemtl '+mat);last=mat
 for vi,ti,ni in face:
  output.append('v '+' '.join(f'{a:.8f}' for a in point(v[vi])));output.append('vt '+' '.join(f'{a:.8f}' for a in uv[ti]));output.append('vn '+' '.join(f'{a:.8f}' for a in matrix@norm[ni]))
 output.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(count+1,count+4)));count+=3
(assets/'models/sa2_02515/chao_cymbal_hand.obj').write_text('\n'.join(output)+'\n')
textures={m:f'chao_garden:block/sa2_02515/chao_cymbals/{m}' for m,_ in chosen};textures['particle']=textures['sa2_rappa_gold_52']
model={'loader':'neoforge:obj','model':'chao_garden:models/sa2_02515/chao_cymbal_hand.obj','automatic_culling':False,'shade_quads':True,'flip_v':True,'textures':textures,'render_type':'minecraft:cutout'}
(assets/'models/item/sa2_02515/chao_cymbal_hand.json').write_text(json.dumps(model,indent=2)+'\n')
report={'source_triangles':120,'single_disc_triangles':len(chosen),'grip':grip.tolist(),'rim_x':.5,'transform_determinant':float(np.linalg.det(matrix)),'source_model_unchanged':True}
(root/'art/cymbal-hand-landmarks02529.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
