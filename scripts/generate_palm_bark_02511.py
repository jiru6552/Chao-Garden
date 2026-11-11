#!/usr/bin/env python3
"""Crisp warm palm bark with horizontal leaf scars and native, uncropped UVs."""
from pathlib import Path
import json, math, random
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'

def main():
    rng=random.Random(2511)
    image=Image.new('RGBA',(64,64));pixels=image.load()
    for y in range(64):
        for x in range(64):
            # Four complete rings per metre. Edges wrap without the previous
            # cropped diamond pattern or smooth olive-coloured blur.
            scar=(y+round(1.2*math.sin(x*math.tau/64)))%16
            grain=2.0*math.sin(x*math.tau/8)+rng.choice((-2,-1,0,0,1,2))
            band={0:-22,1:-14,2:11,3:6,14:-3,15:-8}.get(scar,0)
            fibre=-6 if (x*13+y//4*3)%29==0 and 5<=scar<=12 else 0
            tone=grain+band+fibre
            pixels[x,y]=tuple(max(0,min(255,round(c+tone))) for c in (160,125,82))+(255,)
    image.save(ASSETS/'textures/scene/garden_palm_bark.png',optimize=True)
    path=ASSETS/'models/block/garden_palm_trunk.json'
    model=json.loads(path.read_text())
    # Existing native trunk silhouette and collision remain. Give each face the
    # complete tile rather than an implicit narrow crop of the bark pattern.
    for element in model['elements']:
        for direction,face in element['faces'].items():
            face['uv']=[0,0,16,16]
            if direction in ('up','down'):face['cullface']=direction
    path.write_text(json.dumps(model,indent=2)+'\n')
    print('Palm02511: warm ringed 64px bark, explicit full-face UVs and capped vertical joins.')

if __name__=='__main__':main()
