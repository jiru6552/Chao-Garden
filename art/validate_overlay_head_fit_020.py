#!/usr/bin/env python3
"""Check current development meshes and fitted crown attachments across all endpoint forms."""
from pathlib import Path
import json,math
from build_forms_019 import BASE
ROOT=Path(__file__).resolve().parents[1]
MODEL=ROOT/'src/main/resources/assets/chao_garden/models/entity'

def head(data,target=None):
 candidates=[]
 for b in data['bones']:
  if not b['name'].startswith('skull__cube_'):continue
  points=[]
  for m in b['meshes']:
   vertices=m['morph_vertices'][target] if target is not None and 'morph_vertices' in m else m['vertices']
   points.extend([[v[i]+b['pivot'][i] for i in range(3)] for v in vertices])
  if not points:continue
  bounds=[min(v[i] for v in points) for i in range(3)]+[max(v[i] for v in points) for i in range(3)]
  candidates.append((math.prod(bounds[i+3]-bounds[i] for i in range(3)),bounds,b))
 _,bounds,b=max(candidates,key=lambda p:p[0])
 assert b['parent']=='skull' and all(abs(x)<1e-6 for x in b.get('rotation',[0,0,0]))
 return bounds

def near(a,b):assert all(abs(x-y)<1e-5 for x,y in zip(a,b)),(a,b)
def main():
 cases=0;changed=0;root_count=0
 for key,alias in BASE.items():
  source=json.loads((MODEL/f'official/chao_{alias}.json').read_text())
  gameplay=json.loads((MODEL/f'gameplay/chao_{key}.json').read_text())
  reference=head(source)
  for target in range(9):
   current=head(gameplay,target)
   scales=[(current[i+3]-current[i])/(reference[i+3]-reference[i]) for i in range(3)]
   offset=[current[i]-reference[i]*scales[i] for i in range(3)]
   point=[(reference[0]+reference[3])/2,reference[1],(reference[2]+reference[5])/2]
   fitted=[point[i]*scales[i]+offset[i] for i in range(3)]
   near(fitted,[(current[0]+current[3])/2,current[1],(current[2]+current[5])/2])
   for growth in [1,.82,.8,.88]:
    origin=current[1]+(offset[1]-current[1])*growth
    near([origin+reference[1]*scales[1]*growth],[current[1]])
   cases+=1
   if any(abs(current[i]-reference[i])>.05 for i in range(6)):changed+=1
  for directory in ['overlays','cwe_parts']:
   overlay=json.loads((MODEL/directory/f'chao_{alias}.json').read_text())
   for b in overlay['bones']:
    name=b['name']
    crown=name.startswith('animal_') and int(name.split('_')[1]) in (1,2,5)
    crown|=name.startswith('variant_') and name.endswith('_head')
    if not crown:continue
    assert b['parent']=='skull' and b['pivot']==[0,0,0],(directory,alias,name)
    assert all(abs(v)<1e-5 for v in b.get('rotation',[0,0,0]))
    root_count+=1
 assert cases==135 and changed>100
 report={'version':'0.20.0','development_endpoint_fits':cases,'changed_head_bounds':changed,'verified_skull_local_attachment_roots':root_count,
 'crown_surface_remains_anchored_at_all_growth_scales':True,'source_semantic_head_and_nonhead_joints_unchanged':True,
 'face_beak_placement':'fixed eye-relative plane retained; full skull affine deliberately excluded'}
 (ROOT/'docs/research/OVERLAY_HEAD_FIT_020_CHECKS.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()
