"""Split the existing SA2 castanet shells into rigid, hinged presentation parts.

Preserves source triangle topology and UVs. The largest flat face is the inner
contact plane; shell depth points outward. No source model or texture is replaced.
"""
from pathlib import Path
import json, numpy as np
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/chao_garden'
lines=(assets/'models/sa2_02515/chao_castanets.obj').read_text().splitlines()
v=[];uv=[];norm=[];faces=[];material=''
for line in lines:
    if line.startswith('v '):v.append(list(map(float,line.split()[1:])))
    elif line.startswith('vt '):uv.append(list(map(float,line.split()[1:])))
    elif line.startswith('vn '):norm.append(list(map(float,line.split()[1:])))
    elif line.startswith('usemtl '):material=line.split()[1]
    elif line.startswith('f '):faces.append((material,[[int(i)-1 for i in x.split('/')] for x in line.split()[1:]]))
v=np.array(v);norm=np.array(norm)
for label,mat,sign in [('base','sa2_kasuta01_20',1),('lid','sa2_star_yel_52',-1)]:
    chosen=[f for m,f in faces if m==mat];points=np.unique(v[[i[0] for f in chosen for i in f]],axis=0);center=points.mean(0)
    plane=max(chosen,key=lambda f:np.linalg.norm(np.cross(v[f[1][0]]-v[f[0][0]],v[f[2][0]]-v[f[0][0]])))
    inward=np.cross(v[plane[1][0]]-v[plane[0][0]],v[plane[2][0]]-v[plane[0][0]]);inward/=np.linalg.norm(inward)
    outside=-inward;x=np.array([1.,0,0]);x-=outside*np.dot(x,outside);x/=np.linalg.norm(x)
    y=outside*sign;z=np.cross(x,y);matrix=np.stack([x,y,z]);inside_depth=np.min((points-center)@outside)
    output=['# Rigid shell from the existing SA2 castanets; inner plane is y=.5.','mtllib chao_garden:models/sa2_02515/materials.mtl','usemtl '+mat];count=0
    for face in chosen:
        for vi,ti,ni in face:
            p=matrix@(v[vi]-center)+np.array([.5,.5-sign*inside_depth-(.005 if label=='lid' else 0),.5]);n=matrix@norm[ni]
            output.append('v '+' '.join(f'{a:.8f}' for a in p));output.append('vt '+' '.join(f'{a:.8f}' for a in uv[ti]));output.append('vn '+' '.join(f'{a:.8f}' for a in n))
        output.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(count+1,count+4)));count+=3
    (assets/f'models/sa2_02515/chao_castanets_hand_{label}.obj').write_text('\n'.join(output)+'\n')
    model={'loader':'neoforge:obj','model':f'chao_garden:models/sa2_02515/chao_castanets_hand_{label}.obj','automatic_culling':False,'shade_quads':True,'flip_v':True,'textures':{mat:f'chao_garden:block/sa2_02515/chao_castanets/{mat}','particle':f'chao_garden:block/sa2_02515/chao_castanets/{mat}'},'render_type':'minecraft:cutout'}
    (assets/f'models/item/sa2_02515/chao_castanets_hand_{label}.json').write_text(json.dumps(model,indent=2)+'\n')
    print(label,len(chosen),'source triangles retained')
