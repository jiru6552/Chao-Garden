#!/usr/bin/env python3
"""Make labeled frame-based review aids from the exact native 0.25.3 clips."""
from pathlib import Path
import hashlib
import json
import subprocess
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'docs/motion-0.25.3'
OUT=ROOT/'docs/validation-0.25.3/motion-review-aids'


def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()


def main():
    manifest=BASE/'motion-provenance.json';source=json.loads(manifest.read_text())
    assert source['release']=='0.25.3'
    OUT.mkdir(parents=True,exist_ok=True);reviews=[]
    for clip in source['clips']:
        video=BASE/clip['file'];assert sha(video)==clip['sha256']
        probe=json.loads(subprocess.check_output(['ffprobe','-v','error','-select_streams','v:0','-show_frames','-show_entries','frame=best_effort_timestamp_time','-of','json',str(video)]))
        pts=[float(frame['best_effort_timestamp_time']) for frame in probe['frames']]
        # Every fifth native frame plus the last. Sampling is explicit; this
        # does not claim uninterrupted playback or inspection of every frame.
        indices=sorted(set(range(0,len(pts),5))|{len(pts)-1})
        selection='+'.join('eq(n\\,'+str(index)+')' for index in indices)
        command=['ffmpeg','-v','error','-nostdin','-i',str(video),'-vf','select='+selection+',scale=320:180','-fps_mode','passthrough','-f','rawvideo','-pix_fmt','rgb24','-']
        process=subprocess.Popen(command,stdout=subprocess.PIPE)
        frames=[]
        for index in indices:
            payload=process.stdout.read(320*180*3)
            assert len(payload)==320*180*3,'Missing decoded review frame'
            frames.append((index,Image.frombytes('RGB',(320,180),payload),hashlib.sha256(payload).hexdigest()))
        assert process.stdout.read()==b'' and process.wait()==0
        sheets=[]
        for offset in range(0,len(frames),20):
            page=frames[offset:offset+20];rows=(len(page)+3)//4
            image=Image.new('RGB',(1280,40+rows*204),(22,24,32));draw=ImageDraw.Draw(image)
            draw.text((8,8),clip['scene']+' | actual native frames | every fifth + final | no audio',fill='white')
            for cell,(index,picture,_) in enumerate(page):
                x=(cell%4)*320;y=40+(cell//4)*204
                image.paste(picture,(x,y));draw.text((x+5,y+183),f'frame {index} / {len(pts)-1} · {pts[index]:.6f} s',fill='white')
            target=OUT/(clip['scene']+'-'+str(offset//20+1)+'.jpg');image.save(target,quality=94)
            sheets.append(dict(file=target.relative_to(ROOT).as_posix(),sha256=sha(target)))
        reviews.append(dict(scene=clip['scene'],clip_file=video.relative_to(ROOT).as_posix(),clip_sha256=sha(video),decoded_frames=len(pts),sampled_native_frames=len(indices),frames=[dict(index=index,pts_seconds=pts[index],decoded_scaled_rgb_sha256=digest) for index,_,digest in frames],sheets=sheets))
    result=dict(release='0.25.3',result='AIDS_GENERATED',source=dict(file=manifest.relative_to(ROOT).as_posix(),sha256=sha(manifest)),scope='Derived contacts, not additional native captures. Exact every-fifth native frame and final frame; no claim of complete playback. Whole clip identities bound by SHA.',clips=reviews)
    (OUT/'index.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(dict(clips=len(reviews),sampled_frames=sum(row['sampled_native_frames'] for row in reviews),sheets=sum(len(row['sheets']) for row in reviews))))


if __name__=='__main__':main()
