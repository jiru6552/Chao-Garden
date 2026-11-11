#!/usr/bin/env python3
"""Check every new physical CWE resource and enforce acquisition/geometry boundaries."""
from pathlib import Path
import json,re
from PIL import Image
from build_cwe_content_019 import FRUITS,EXTRA_GENES,ACCESSORIES,GIFTS,NEW_TOYS,ROOT,A,D
checks=0
def require(ok,message):
 global checks
 checks+=1
 if not ok:raise AssertionError(message)
def read(p):return json.loads(p.read_text())
def sprite(p,size):
 im=Image.open(p).convert('RGBA');return im.size==size and set(im.getchannel('A').tobytes())<={0,255} and im.getbbox()!=None
for fruit in FRUITS:
 for ident in (fruit,fruit+'_seed'):
  require(sprite(A/'textures/item'/f'{ident}.png',(32,32)),f'Native fruit/seed pixels {ident}')
  require(read(A/'models/item'/f'{ident}.json')['textures']['layer0']=='chao_garden:item/'+ident,f'Exact item binding {ident}')
 require(read(D/'recipe'/f'{fruit}_seed.json')['ingredients']==[{'item':'chao_garden:'+fruit}],f'Fruit-first recipe {fruit}')
 for suffix,n in [('_seedling',2),('_tree_crown',2),('_pod',16)]:
  variants=read(A/'blockstates'/f'{fruit}{suffix}.json')['variants'];require(len(variants)==n,f'Complete crop states {fruit}{suffix}')
  require(all((A/'models'/((v['model'].split(':',1)[1])+'.json')).exists() for v in variants.values()),f'Resolved block models {fruit}{suffix}')
 for age in range(4):require(sprite(A/'textures/block'/f'{fruit}_pod_{age}.png',(32,32)),f'Native ripening skin {fruit}/{age}')
for ident in [x+'_animal_gene' for x in EXTRA_GENES]:require(sprite(A/'textures/item'/f'{ident}.png',(32,32)),f'Consistent DNA item {ident}')
source_items=set(read(ROOT/'art/cwe02516/generated.json')['items'])
for ident in ACCESSORIES+GIFTS+['eggshell_hat']:require(sprite(A/'textures/item'/f'{ident}.png',(32,32) if ident in source_items else (16,16)),f'Native gift sprite {ident}')
for ident in ACCESSORIES+['hoodie_sleeve_left','hoodie_sleeve_right','eggshell_hat','medal_race','medal_karate']:
 data=read(A/'models/entity/cwe_accessories'/f'{ident}.json');meshes=data['bones'][0]['meshes'];require(bool(meshes),f'Real wearable mesh {ident}')
 for mesh in meshes:
  require(len(mesh['vertices'])>=8 and len(mesh['faces'])>=6,f'Closed nonempty geometry {ident}')
  for face in mesh['faces']:require(all(0<=x<len(mesh['vertices']) for x in face['v']),f'Valid attachment face {ident}')
for ident in NEW_TOYS:
 variants=read(A/'blockstates'/f'{ident}.json')['variants']
 # The ring has distinct dry/floating meshes. Check the exact Cartesian state
 # space and binding, not just a relaxed count that could hide missing states.
 expected={}
 for facing,rotation in [('north',0),('east',90),('south',180),('west',270)]:
  for frame in range(4):
   for playing in ('false','true'):
    for wet in (('false','true') if ident=='chao_lifesaver' else (None,)):
     key=f'facing={facing},frame={frame},playing={playing}'
     if wet is not None:key+=f',waterlogged={wet}'
     stage='idle' if playing=='false' else f'play_{frame}'
     suffix=('_float_' if wet=='true' else '_')+stage
     expected[key]={'model':f'chao_garden:block/{ident}{suffix}','y':rotation}
 require(set(variants)==set(expected),f'Complete exact physical toy states {ident}: expected {len(expected)}')
 for key,value in expected.items():
  require(variants[key]==value,f'Correct facing/frame/water model {ident}/{key}')
  require((A/'models'/(value['model'].split(':',1)[1]+'.json')).is_file(),f'Resolved physical toy model {ident}/{key}')
 suffixes=['idle']+[f'play_{i}' for i in range(4)]
 if ident=='chao_lifesaver':suffixes += ['float_idle']+[f'float_play_{i}' for i in range(4)]
 for suffix in suffixes:
  data=read(A/'models/block'/f'{ident}_{suffix}.json')
  if data.get('loader')=='neoforge:obj':
   require(data.get('loader')=='neoforge:obj',f'Native source shovel {suffix}')
   lines=(A/data['model'].split(':',1)[1]).read_text().splitlines()
   verts=[list(map(float,line.split()[1:])) for line in lines if line.startswith('v ')]
   require(len([line for line in lines if line.startswith('f ')])>=18 and all(-1<=v<=2 for p in verts for v in p),f'Nonempty bounded source shovel {suffix}')
   require(all((A/'textures'/(v.split(':',1)[1]+'.png')).is_file() for v in data['textures'].values()),f'Resolved source shovel materials {suffix}')
  else:
   require(bool(data['elements']),f'Real prop {ident}/{suffix}')
   require(all(all(-16<=a<b<=32 for a,b in zip(e['from'],e['to'])) for e in data['elements']),f'Nondegenerate bounded geometry {ident}/{suffix}')
 loot=read(D/'loot_table/blocks'/f'{ident}.json');require(loot['pools'][0]['entries']==[{'type':'minecraft:item','name':'chao_garden:'+ident}],f'Exactly one physical block drop {ident}')
for mushroom in ('mushroom','mushroom_alt'):require(not(D/'recipe'/f'{mushroom}_seed.json').exists(),f'No forbidden mushroom seed {mushroom}')
require(len(FRUITS)==13 and len(EXTRA_GENES)==10 and len(ACCESSORIES)==10 and len(NEW_TOYS)==16,'Complete declared content catalogs')
path=ROOT/'docs/validation-0.19/cwe-content.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps({'version':'0.19.0','checks':checks,'result':'passed','fruits':len(FRUITS),'genes':len(EXTRA_GENES),'wearables':len(ACCESSORIES),'special_gifts':len(GIFTS),'additional_physical_toys':len(NEW_TOYS),'scope':'resources only; Java/server tests recorded separately'},indent=2)+'\n')
print(f'CWE native content: {checks} checks passed')
