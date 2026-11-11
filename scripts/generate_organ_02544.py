"""Reproducible original pipe-organ tone and key-only frames for the existing organ."""
from pathlib import Path
import json, math, struct, subprocess, tempfile, wave
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
rate=44100; duration=.60
values=[]
for i in range(round(rate*duration)):
    t=i/rate
    attack=min(1,t/.025); release=min(1,(duration-t)/.10)
    envelope=attack*attack*(3-2*attack)*release*release*(3-2*release)
    # A steady flue-pipe fundamental with restrained octave and upper partials.
    value=sum(a*math.sin(2*math.pi*261.625565*h*t) for h,a in [(1,.72),(2,.24),(3,.09),(4,.045),(6,.015)])
    values.append(value*envelope)
peak=max(map(abs,values)); pcm=[round(v/peak*.65*32767) for v in values]
target=ASSETS/'sounds/toys/organ.ogg';target.parent.mkdir(parents=True,exist_ok=True)
with tempfile.TemporaryDirectory() as directory:
    wav=Path(directory)/'organ.wav'
    with wave.open(str(wav),'wb') as out:
        out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate)
        out.writeframes(struct.pack('<'+'h'*len(pcm),*pcm))
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(target)],check=True)
p=ASSETS/'sounds.json'; data=json.loads(p.read_text());data['toy.organ']={'subtitle':'subtitles.chao_garden.toy.organ','sounds':[{'name':'chao_garden:toys/organ','stream':False}]};p.write_text(json.dumps(data,indent=2)+'\n')
p=ASSETS/'lang/en_us.json';data=json.loads(p.read_text());data['subtitles.chao_garden.toy.organ']='Organ plays';p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
source=ASSETS/'models/sa2_02515/chao_organ_idle.obj'
for frame in range(4):
    lines=[]; material=''
    for line in source.read_text().splitlines():
        if line.startswith('usemtl '): material=line.split()[1]
        if frame==1 and material=='sa2_piano01_20' and line.startswith('v '):
            x,y,z=map(float,line.split()[1:]);line=f'v {x:.7f} {y-.003:.7f} {z:.7f}'
        lines.append(line)
    source.with_name(f'chao_organ_play_{frame}.obj').write_text('\n'.join(lines)+'\n')
print('Organ tone: 0.60 seconds, mono 44.1 kHz; four stationary cabinet frames, only key plane depresses')
