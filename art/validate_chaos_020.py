#!/usr/bin/env python3
"""Independent Chaos asset audit: headwear visibility, uncapped ice joins and coat coverage."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/chao_garden'
MODELS=ASSET/'models/entity/gameplay'

def descendants(bones,start):
 result={start}
 while True:
  expanded=result|{name for name,b in bones.items() if b.get('parent') in result}
  if expanded==result:return result
  result=expanded

def main():
 report={'version':'0.20.0','families':{},'tip_material_policy':{'two_tone_regular_colored_jewel':'translucent, alpha255 to97','mono_tone_regular_colored_jewel':'opaque, alpha255','shiny':'independent of opacity'}}
 for family in ['light','angel','devil']:
  opaque=json.loads((MODELS/f'chao_chaos_{family}.json').read_text())
  glass=json.loads((MODELS/f'chao_chaos_{family}_glass.json').read_text())
  base={b['name']:b for b in opaque['bones']};ice={b['name']:b for b in glass['bones']}
  assert base.keys()==ice.keys()
  for name in base:
   assert base[name]['parent']==ice[name]['parent'] and base[name]['pivot']==ice[name]['pivot']
  all_ice={name for name,b in ice.items() if b['meshes']}
  skull=descendants(base,'skull');head_ice=all_ice&skull
  limbs=all_ice-skull
  assert head_ice and limbs
  # This is the same visibility contract used by hats and birthday paper hats:
  # hide each head-decorations subtree, then copy all complete poses to ice.
  decorations=set(opaque['head_decorations']);hidden=set()
  for name in decorations:hidden|=descendants(base,name)
  assert head_ice<=hidden, 'Head ice protrudes through a hat: '+str(head_ice-hidden)
  assert not (limbs&hidden), 'Headwear incorrectly hides hands/feet'
  assert set(glass['head_decorations'])==decorations
  # Full skull hiding must also hide every transparent head branch.
  assert head_ice<=skull and not(limbs&skull)
  for foot in ['chaos_foot_foot_left','chaos_foot_foot_right']:
   for owner in [base[foot],ice[foot]]:
    for mesh in owner['meshes']:
     assert not any(all(abs(mesh['vertices'][v][2]+1.2)<1e-6 for v in face['v']) for face in mesh['faces']), 'Internal cap at opaque/ice join'
  for name in head_ice|limbs:
   uv=[p for m in ice[name]['meshes'] for f in m['faces'] for p in f['uv']]
   assert min(p[0] for p in uv)==.5 and max(p[0] for p in uv)==63.5
   assert all(0<=p[0]<64 and 0<=p[1]<32 for p in uv)
  old={p.name for p in (ASSET/'textures/entity/chao/official').glob(f'chaos_{family}*.png')}
  new={p.name for p in (ASSET/'textures/entity/chao/gameplay/chaos').glob(f'chaos_{family}*.png')}
  assert old==new and len(new)==117
  assert all(not base[name]['meshes'] for name in ['skull__cube_2','skull__cube_3'])
  assert len([name for name in base if name.startswith('chaos_eye_')])==2
  report['families'][family]={'hat_hidden_ice_branches':sorted(head_ice),'limb_ice_remains_visible':True,'full_skull_hide_covers_all_head_ice':True,'uncapped_foot_material_join':True,'full_opacity_gradient_uv_coverage':True,'inherited_coats':len(new)}
 (ROOT/'docs/research/CHAOS_020_VISIBILITY_CHECKS.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()
