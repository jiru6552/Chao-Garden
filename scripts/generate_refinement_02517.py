#!/usr/bin/env python3
"""Garden names, retired acquisition, native-style disc and source tatami materials."""
from pathlib import Path
import json,hashlib
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden';D=ROOT/'src/main/resources/data/chao_garden';R=ROOT/'art/refinement02517'
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2,ensure_ascii=False,sort_keys=True)+'\n')
# Exact original stage tatami region; its cloth edging and weave remain intact.
source=Image.open(R/'karate/karate01.png').convert('RGBA');source.crop((0,0,128,128)).save(A/'textures/block/karate_mat.png')
for name in ('en_us','en_gb'):
 p=A/'lang'/(name+'.json')
 if not p.exists():continue
 d=json.loads(p.read_text());d['block.chao_garden.garden_block']='Chao Garden';d['item.chao_garden.chao_garden_music_disc']='Chao Garden Music Disc';write(p,d)
for f in [D/'recipe/garden_bell.json',D/'advancement/recipes/misc/garden_bell.json']:
 if f.exists():f.unlink()
p=D/'recipe/karate_mat.json';d=json.loads(p.read_text());d['result']['count']=1;write(p,d)
# Only the center drops the one assembled item. Old individual carpet tiles remain recoverable.
write(D/'loot_table/blocks/karate_mat.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'chao_garden:karate_mat'}],'conditions':[{'condition':'minecraft:any_of','terms':[{'condition':'minecraft:block_state_property','block':'chao_garden:karate_mat','properties':{'tile':str(t)}} for t in (24,49)]},{'condition':'minecraft:survives_explosion'}]}]})
variants={}
for tile in range(50):
 x,z=tile%7,tile//7
 # Alternating woven grain and a single border frame around the assembled carpet.
 u0=0 if tile==49 or x==0 else 10/128*16;u1=16 if tile==49 or x==6 else 118/128*16
 model={'parent':'minecraft:block/block','textures':{'mat':'chao_garden:block/karate_mat','particle':'chao_garden:block/karate_mat'},'elements':[{'from':[0,0,0],'to':[16,1,16],'faces':{face:{'texture':'#mat','uv':[u0,0,u1,16]} for face in ('up','down','north','south','east','west')}}]}
 if tile!=49 and (x//2+z)%2:model['elements'][0]['faces']['up']['rotation']=90
 write(A/'models/block'/('karate_mat_'+str(tile)+'.json'),model)
 for axis in ('x','z'):
  for opened in ('true','false'):variants[f'axis={axis},open={opened},tile={tile}']={'model':'chao_garden:block/karate_mat_'+str(tile)}
write(A/'blockstates/karate_mat.json',{'variants':variants})
# Match the stepped silhouette, groove blocks and central label of native 16px records.
rows=['................','......####......','....########....','...##########...','..############..','..############..','.##############.','.##############.','.##############.','.##############.','..############..','..############..','...##########...','....########....','......####......','................']
im=Image.new('RGBA',(16,16));pal={'edge':(32,32,32,255),'body':(63,63,63,255),'light':(76,76,76,255),'label':(66,184,205,255),'hole':(30,34,34,255)}
for y,row in enumerate(rows):
 for x,c in enumerate(row):
  if c=='#':im.putpixel((x,y),pal['body'] if 2<x<13 and 2<y<13 else pal['edge'])
for x,y in [(5,4),(6,4),(4,5),(5,5),(4,6),(3,8),(4,8),(5,10),(6,11),(10,4),(11,5),(12,6),(11,10),(10,11),(9,12)]:im.putpixel((x,y),pal['light'])
for y in range(5,11):
 for x in range(5,11):
  if x in (5,10) and y in (5,10):continue
  im.putpixel((x,y),pal['label'])
for x in (7,8):
 for y in (7,8):im.putpixel((x,y),pal['hole'])
im.save(A/'textures/item/chao_garden_music_disc.png')
write(A/'models/item/chao_garden_music_disc.json',{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/chao_garden_music_disc'}})
# Refresh the scene atlas manifest after introducing the tatami tile models.
from lobby_material_manifest0255 import render_textures
p=ROOT/'art/lobby_garden_manifest_025.json';d=json.loads(p.read_text());d['render_textures']=render_textures(ROOT,d['block_atlas_textures']);write(p,d)
write(R/'provenance.json',{'tatami_source':'Supplied gd_PC/al_stg_karate_tex.prs: karate01, top-left 128×128 region','input_sha256':hashlib.sha256((R/'original/al_stg_karate_tex.prs').read_bytes()).hexdigest(),'disc':'Authored 16px vanilla-style record; cyan Chao label','accordion':'Single item surfaces plus half-texel UV inset stop boundary sampling and reverse-face flicker','coffin':'Half-texel edge clamp prevents neighboring atlas sprite bleeding'})
print('0.25.17 refinement assets generated')
