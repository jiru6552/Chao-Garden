#!/usr/bin/env python3
"""Exact code-authored tablet model and palette; final writer for analyzer resources."""
from pathlib import Path
import json
from PIL import Image,ImageDraw
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/chao_garden';D=R/'src/main/resources/data/chao_garden'
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2,sort_keys=True)+'\n')
# Front and rear panels, authored on the same 32px grid as the cuboid geometry.
blue='#2878be';deep='#154873';light='#57b8e7';yellow='#f6c849';cream='#ffe68a'
im=Image.new('RGBA',(32,32),blue);d=ImageDraw.Draw(im)
d.rectangle((1,1,30,30),outline=light,width=1);d.rectangle((3,4,28,26),fill=deep)
d.rectangle((4,5,27,25),fill='#a1dded');d.rectangle((5,6,26,8),fill='#4a9bbf')
d.rectangle((7,7,15,7),fill='#daeff6');d.rectangle((23,7,25,7),fill=cream)
# A tiny Chao silhouette and three clear status bars on the actual tablet screen.
d.rectangle((8,12,15,17),fill='#e2f8fa');d.rectangle((10,11,13,18),fill='#e2f8fa');d.point((12,10),fill='#e2f8fa')
d.rectangle((9,19,14,21),fill='#63bce5');d.rectangle((8,21,10,22),fill=yellow);d.rectangle((13,21,15,22),fill=yellow)
d.rectangle((9,14,10,15),fill=deep);d.rectangle((13,14,14,15),fill=deep)
for y,w in ((12,6),(16,4),(20,5)):d.rectangle((19,y,25,y+1),fill='#5c9cae');d.rectangle((19,y,19+w,y+1),fill=yellow)
d.rectangle((13,28,18,29),fill=yellow);d.point((16,2),fill=deep);im.save(A/'textures/item/chao_analyzer.png')
back=Image.new('RGBA',(32,32),blue);d=ImageDraw.Draw(back);d.rectangle((1,1,30,30),outline=light);d.rectangle((5,7,26,25),fill='#246db1')
d.ellipse((11,11,21,21),outline=yellow,width=2);d.rectangle((15,8,17,12),fill=yellow)
for y in (27,29):d.line((6,y,25,y),fill=deep)
back.save(A/'textures/item/chao_analyzer_back.png')
pal=Image.new('RGBA',(16,16),blue);d=ImageDraw.Draw(pal)
for i,c in enumerate((blue,deep,light,yellow,cream)):d.rectangle((i*3,0,i*3+2,15),fill=c)
pal.save(A/'textures/item/chao_analyzer_palette.png')
textures={'front':'chao_garden:item/chao_analyzer','back':'chao_garden:item/chao_analyzer_back','palette':'chao_garden:item/chao_analyzer_palette','particle':'chao_garden:item/chao_analyzer'}
def box(lo,hi,tone):return {'from':lo,'to':hi,'faces':{f:{'texture':'#palette','uv':[tone*3+1,7,tone*3+2,8]} for f in ('north','south','east','west','up','down')}}
e=[box([2,1,7.25],[14,15,8.75],0)]
e[0]['faces']['south']={'texture':'#front','uv':[0,0,16,16]};e[0]['faces']['north']={'texture':'#back','uv':[0,0,16,16]}
for x in (1.6,12.8):
 for y in (0.7,12.5):e.append(box([x,y,7],[x+1.6,y+2.8,9],3))
e.append(box([13.9,8,7.5],[14.4,10.3,8.4],4))
model={'credit':'Chao Garden 0.25.18 code-authored blue/yellow tablet','gui_light':'front','textures':textures,'elements':e,'display':{
 'gui':{'rotation':[0,-16,-5],'translation':[0,0,0],'scale':[.95,.95,.95]},
 'firstperson_righthand':{'rotation':[0,-28,-5],'translation':[0,3,0],'scale':[.5,.5,.5]},
 'firstperson_lefthand':{'rotation':[0,28,5],'translation':[0,3,0],'scale':[.5,.5,.5]},
 'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,3,1],'scale':[.65,.65,.65]},
 'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,3,1],'scale':[.65,.65,.65]},
 'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.5,.5,.5]},
 'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[.85,.85,.85]}}}
write(A/'models/item/chao_analyzer.json',model)
# Legacy block IDs remain loadable as internal classroom anchors; no player acquisition.
for p in (D/'recipe/lesson_desk.json',D/'advancement/recipes/misc/lesson_desk.json'):
 if p.exists():p.unlink()
write(D/'loot_table/blocks/lesson_desk.json',{'type':'minecraft:block','pools':[]})
from lobby_material_manifest0255 import render_textures
p=R/'art/lobby_garden_manifest_025.json';manifest=json.loads(p.read_text());manifest['render_textures']=render_textures(R,manifest['block_atlas_textures']);write(p,manifest)
print('0.25.18 blue/yellow tablet and retired desk acquisition generated')
