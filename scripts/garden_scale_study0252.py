#!/usr/bin/env python3
"""Bind the compact footprint study to exact source; native receipts stay separately labelled."""
from pathlib import Path
import argparse, hashlib, json, math, re
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def field(path):
    source=path.read_text()
    m=re.search(r'boolean insideField\(int x,int z\)\{return Math.pow\(x/([0-9.]+),4\)\+Math.pow\(\(z([+-][0-9]+)\)/([0-9.]+),4\)<=1;\}',source)
    if not m:raise RuntimeError('Unrecognized authored footprint: '+str(path))
    rx,shift,rz=map(float,m.groups());cells={(x,z) for x in range(-60,61) for z in range(-60,61) if (x/rx)**4+((z+shift)/rz)**4<=1}
    xs=[x for x,z in cells];zs=[z for x,z in cells]
    return {'mask_columns':len(cells),'voxel_bounds':{'x':[min(xs),max(xs)],'z':[min(zs),max(zs)],'width':max(xs)-min(xs)+1,'depth':max(zs)-min(zs)+1},'center_span':[max(xs)-min(xs),max(zs)-min(zs)]},cells

def main():
    a=argparse.ArgumentParser();a.add_argument('--native-log',type=Path);args=a.parse_args()
    previous=ROOT/'docs/references-0.25.2/source-baselines/GardenRooms025-0.25.1.java';current=ROOT/'src/main/java/com/chaogarden/hub/GardenRooms025.java'
    old,oldmask=field(previous);new,newmask=field(current)
    assert old['mask_columns']==8279 and new['mask_columns']==1745
    refs=[ROOT/f'docs/references-0.25.1/native-keyframes/first-{n:03}.png' for n in (22,43,61,204,309,315,321,348,390,410)]
    native=None
    if args.native_log:
        log=args.native_log.read_text(errors='replace');m=re.search(r'CHAO_COMPACT0252_GEOMETRY oldMask=8279 newMask=(\d+) oldVoxelBox=101x91 newVoxelBox=49x41 lawnY79=(\d+) pondColumns=179',log)
        if not m or int(m[1])!=new['mask_columns']:raise RuntimeError('No matching current native compact geometry receipt')
        native={'file':args.native_log.resolve().relative_to(ROOT).as_posix(),'sha256':sha(args.native_log),'native_emitted_mask_columns':int(m[1]),'low_turf_y79_columns':int(m[2]),'scope':'Actual Minecraft server emitted-plan mask and player-height arrival clearance assertion. It does not recover the original SA2 mesh dimensions.'}
    output=ROOT/'docs/references-0.25.2/garden-footprint-comparison.png';im=Image.new('RGB',(1100,760),'#101826');d=ImageDraw.Draw(im)
    font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',18);small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',14);title=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',26)
    d.text((28,20),'First Neutral Garden — compact Minecraft reconstruction',font=title,fill='white')
    d.text((28,58),'Same scale: gray = previous field; green = new field. Original SA2 dimensions are inferred.',font=small,fill='#d3deeb')
    scale=5.8;cx,cz=360,405
    def point(x,z):return(cx+x*scale,cz+z*scale)
    for cells,color in ((oldmask,'#4d5665'),(newmask,'#4a985b')):
        for x,z in cells:
            a,b=point(x,z);d.rectangle((a,b,a+scale-.3,b+scale-.3),fill=color)
    for z in range(-6,10):
        for x in range(-20,-5):
            if ((x+13)/7)**2+((z+1)/10)**2<1:
                a,b=point(x,z);d.rectangle((a,b,a+scale-.3,b+scale-.3),fill='#44b9d5')
    for z in (-7,-10,-13,-16):
        a,b=point(-17,z-2);c,e=point(-13,z+1);d.rectangle((a,b,c,e),fill='#85dbe7',outline='#d7f5f7')
    for x,z,label,color in ((5,-18,'Cave','#f2c36b'),(17,9,'Transporter','#f9e39d'),(5,-12,'Arrival','#f6f8fd')):
        a,b=point(x+.5,z+.5);d.ellipse((a-4,b-4,a+4,b+4),fill=color);d.text((a+8,b-16),label,font=small,fill=color)
    d.text((690,140),'Previous: 8,279 field columns',font=font,fill='#b7c4d6');d.text((690,173),'New: 1,745 field columns',font=font,fill='#83d79a');d.text((690,213),'78.9% less ground-mask area',font=font,fill='white')
    lines=['Voxel bounds:101×91 →49×41','Center spans:100×90 →48×40','','Cave:3 blocks wide,4 high','Pond:179 fully filled columns','Waterfall:four3-high terraces','Palms:4–5 trunk blocks','Transporter:one-block hummock','','Gray/green areas include crags.','They are footprint masks, not','measurements of traversable lawn.','','Source frames:00:22,03:24,','05:09,05:48 and06:30.']
    for i,line in enumerate(lines):d.text((690,265+i*24),line,font=small,fill='#d3deeb')
    d.text((28,720),'Axes are authored Minecraft X/Z. Native player-height views and actual walks are the appearance evidence.',font=small,fill='#d3deeb');im.save(output,optimize=True)
    report={'release':'0.25.2','result':'SOURCE_SCALE_STUDY','previous':old,'current':new,'mask_area_reduction_percent':round(100*(1-new['mask_columns']/old['mask_columns']),6),'scale_claim':'Authored player-scaled Minecraft reconstruction from supplied SA2/Battle video frames; no measured source-level extraction or original metric dimensions claimed.','native_receipt':native,'sources':[{'file':p.relative_to(ROOT).as_posix(),'sha256':sha(p)} for p in (previous,current,*refs)],'comparison':{'file':output.relative_to(ROOT).as_posix(),'sha256':sha(output)}}
    out=ROOT/'docs/validation-0.25.2/garden-scale-source.json';out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'result':report['result'],'mask_area_reduction_percent':report['mask_area_reduction_percent'],'native_receipt':native},indent=2))
if __name__=='__main__':main()
