#!/usr/bin/env python3
"""Validate source provenance and real generated resources, not registry counts alone."""
from pathlib import Path
import hashlib,json,sys
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'art'))
from generate_sa2_assets_02514 import CATALOG,adapted,render
A=ROOT/'src/main/resources/assets/chao_garden';checks=0
def check(ok,msg):
 global checks
 checks+=1
 if not ok:raise AssertionError(msg)
def main():
 from validate_fruit_models_02512 import native
 source=ROOT/'art/sa2_02514';provenance=json.loads((source/'provenance.json').read_text())
 for path,digest in provenance['inputs'].items():check(hashlib.sha256((source/path).read_bytes()).hexdigest()==digest,'Source input changed: '+path)
 faces=0
 for name,info in CATALOG.items():
  mesh,mats=adapted(name);expected=render(mesh,mats)
  with Image.open(A/'textures/item'/(name+'.png')) as icon:check(icon.tobytes()==expected.tobytes(),'GUI/native geometry drift: '+name)
  data=json.loads((A/'models/item/fruit02512'/(name+'.json')).read_text());faces+=native(data)
  actual_mats={s.split()[1] for s in (A/data['model'].split(':')[1]).read_text().splitlines() if s.startswith('usemtl ')}
  check(actual_mats<=data['textures'].keys(),'Unbound native material '+name)
  check(all(min(im.size)>=16 and max(im.size)<=2048 for im in mats.values()),'Unbounded source atlas '+name)
  check(all(0<=c<=1 for _,uv,_ in mesh.faces for pair in uv for c in pair),'UV escapes Minecraft atlas '+name)
  check(info['source_archive'] in ('al_object','al_toy'),'Unidentified source '+name)
 for stage in ('idle',*(f'play_{i}' for i in range(4))):faces+=native(json.loads((A/'models/block'/('chao_shovel_'+stage+'.json')).read_text()))
 check(len(CATALOG)==31,'Expected 22 foods, seven Battle seeds and two tools')
 # Previously merged alignment-fruit IDs must not return as purchasable content.
 for legacy in ('hero_fruit','dark_fruit'):
  check(not (A/'models/item'/(legacy+'.json')).exists(),'Retired alignment fruit returned')
 for model in ('garden_palm_trunk','garden_palm_fruit_support'):
  p=A/'models/block'/(model+'.json')
  if p.exists():
   d=json.loads(p.read_text());check(d['textures']['end']=='chao_garden:scene/garden_palm_end','Palm end grain not bound')
   check(all(e['faces'][side]['texture']=='#end' for e in d['elements'] for side in ('up','down')),'Bark stretched over cut end')
 atlas=json.loads((A.parent/'minecraft/atlases/blocks.json').read_text())
 registered={row.get('resource') for row in atlas['sources']}
 check({'chao_garden:scene/garden_palm_end','chao_garden:scene/lobby_glass'}<=registered,'Custom scene materials must be registered in the block atlas')
 report={'release':'0.25.14','result':'PASS','checks':checks,'source_items':31,'native_faces_checked':faces,'source_sha256':hashlib.sha256((source/'items.json').read_bytes()).hexdigest()}
 (ROOT/'docs/validation-0.25.14/source-validation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
