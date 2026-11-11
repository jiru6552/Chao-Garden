#!/usr/bin/env python3
"""Small native recovered-component/robo-heart sprites; neither is sold or crafted."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden'

def write(path,value):
 path.parent.mkdir(parents=True,exist_ok=True)
 path.write_text(json.dumps(value,indent=2)+'\n')

def sprite(heart):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 if heart:
  # Broad heart lobes, a clear point, and a tiny metal housing keep16px readable.
  outline=[(3,3),(6,3),(8,5),(10,3),(13,3),(15,5),(15,9),(8,15),(1,9),(1,5)]
  d.polygon(outline,fill='#3c5260')
  d.polygon([(3,4),(6,4),(8,6),(10,4),(13,4),(14,6),(14,9),(8,14),(2,9),(2,6)],fill='#afc5cc')
  d.polygon([(4,5),(6,5),(8,7),(10,5),(12,5),(13,7),(13,9),(8,13),(3,9),(3,7)],fill='#a93361')
  d.polygon([(4,6),(6,6),(8,8),(10,6),(12,6),(12,9),(8,12),(4,9)],fill='#e56a8b')
  d.line((4,8,6,8,7,7,8,10,9,8,12,8),fill='#ffe9ac')
  d.rectangle((6,0,9,2),fill='#5b7180');d.rectangle((7,1,8,3),fill='#e1bf69')
 else:
  d.polygon([(4,1),(11,1),(11,3),(14,3),(14,12),(11,12),(11,14),(4,14),(4,12),(1,12),(1,3),(4,3)],fill='#3f5765')
  d.rectangle((3,3,12,12),fill='#aec7cd');d.rectangle((4,4,11,11),fill='#638891')
  d.rectangle((6,5,10,9),fill='#d6e5df');d.rectangle((7,6,9,8),fill='#6db8c9')
  d.line((4,10,6,10,6,11),fill='#ddbd67');d.line((11,5,11,10,9,10),fill='#ddbd67')
  for x in (5,8,10):
   d.rectangle((x,0,x,2),fill='#d6b361');d.rectangle((x,13,x,15),fill='#d6b361')
 return im

def main():
 for ident,heart in [('robot_component',False),('robo_heart',True)]:
  path=A/'textures/item'/f'{ident}.png';path.parent.mkdir(parents=True,exist_ok=True)
  sprite(heart).save(path,optimize=True)
  write(A/'models/item'/f'{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/'+ident}})
 langpath=A/'lang/en_us.json';lang=json.loads(langpath.read_text())
 lang['item.chao_garden.robot_component']='Robot Component'
 lang['item.chao_garden.robo_heart']='Robo Heart'
 write(langpath,lang)
 print('Robot021: recoveredcomponent and calibratedheart native sprites/models generated; no shop/recipe acquisition.')

if __name__=='__main__':main()
