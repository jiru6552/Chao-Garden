#!/usr/bin/env python3
"""Native authored waterfall limestone and broad-leaf terrace plants."""
from pathlib import Path
import json,math,random
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden'
def write(p,obj):
    p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,indent=2,ensure_ascii=False)+'\n')
def main():
    folder=A/'textures/block/neutral02510';folder.mkdir(parents=True,exist_ok=True)
    # Quiet vertical mineral streaks, separate from the enclosing cracked crag.
    rng=random.Random(2510);im=Image.new('RGBA',(64,64));px=im.load()
    for y in range(64):
        for x in range(64):
            u=x/64*math.tau;v=y/64*math.tau
            streak=3.5*math.sin(7*u+.12*math.sin(v))+2*math.sin(19*u+.2*math.cos(v))+1.2*math.sin(3*u+v)
            tone=streak+rng.uniform(-1.5,1.5)
            px[x,y]=tuple(round(c+tone) for c in (190,183,173))+(255,)
    im.save(folder/'cascade_rock.png')
    # Chunky leaf fans and sparse split lobes, on a pair of native cutout planes.
    leaf=Image.new('RGBA',(32,32));d=ImageDraw.Draw(leaf)
    for stem,end in [((15,31),(13,9)),((16,31),(23,16)),((15,31),(7,21))]:
        d.line([stem,end],fill='#416a24',width=2)
    leaves=[([(13,10),(10,4),(4,3),(1,7),(5,13),(10,14)],'#347322'),
            ([(13,9),(18,3),(27,3),(31,8),(25,12),(17,13)],'#438c29'),
            ([(22,17),(27,13),(31,16),(30,22),(24,25),(18,23)],'#367624'),
            ([(8,21),(3,16),(0,19),(1,25),(6,28),(12,25)],'#4b912c')]
    for poly,color in leaves:d.polygon(poly,fill=color)
    for line in [[(13,10),(4,7)],[(14,9),(27,7)],[(22,18),(28,19)],[(8,22),(3,21)]]:
        d.line(line,fill='#78a846',width=1)
    # Open slits rather than a large opaque leaf rectangle.
    for poly in [[(7,3),(9,7),(8,5)],[(22,3),(23,7),(24,3)],[(31,18),(27,20),(31,20)],[(1,23),(5,24),(1,25)]]:
        d.polygon(poly,fill=(0,0,0,0))
    leaf.save(folder/'broadleaf.png')
    write(A/'models/block/neutral_cascade_rock.json',{'parent':'minecraft:block/cube_all','textures':{'all':'chao_garden:block/neutral02510/cascade_rock'}})
    write(A/'models/block/neutral_broadleaf.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','ambientocclusion':False,'textures':{'cross':'chao_garden:block/neutral02510/broadleaf'}})
    for name in ['neutral_cascade_rock','neutral_broadleaf']:
        write(A/f'blockstates/{name}.json',{'variants':{'':{'model':'chao_garden:block/'+name}}})
    langpath=A/'lang/en_us.json';lang=json.loads(langpath.read_text())
    lang['block.chao_garden.neutral_cascade_rock']='Garden Cascade Limestone'
    lang['block.chao_garden.neutral_broadleaf']='Garden Broadleaf Plant'
    write(langpath,lang)
    print('Neutral02510: pale cascade stone and broadleaf cutout plant; native block models, no obtainable scenery items.')
if __name__=='__main__':main()
