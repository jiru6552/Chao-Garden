"""Original struck skin + short paired-jingle synthesis. Listening is a separate check."""
from pathlib import Path
import json, math, random, struct, subprocess, tempfile, wave
root=Path(__file__).resolve().parents[1]
target=root/'src/main/resources/assets/chao_garden/sounds/toys'
rate=44100;duration=.23
for variation in range(3):
    rng=random.Random(252600+variation);values=[];last=0
    partials=[(rng.uniform(2500,9500),rng.uniform(.05,.13),rng.uniform(22,45),rng.uniform(0,6.28)) for _ in range(13)]
    for i in range(int(rate*duration)):
        t=i/rate;noise=rng.uniform(-1,1);high=noise-last;last=noise
        skin=.6*math.sin(2*math.pi*180*t)*math.exp(-t*65)
        strike=.16*high*math.exp(-t*240)
        jingles=sum(a*math.sin(2*math.pi*f*t+p)*math.exp(-t*d) for f,a,d,p in partials)*(1-math.exp(-t*1800))
        values.append((skin+strike+jingles)*min(1,(duration-t)/.04))
    peak=max(map(abs,values));samples=[round(v/peak*.66*32767) for v in values]
    with tempfile.TemporaryDirectory() as tmp:
        wav=Path(tmp)/'tambourine.wav'
        with wave.open(str(wav),'wb') as s:
            s.setnchannels(1);s.setsampwidth(2);s.setframerate(rate);s.writeframes(struct.pack('<'+'h'*len(samples),*samples))
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(target/f'tambourine_{variation+1}.ogg')],check=True)
p=root/'src/main/resources/assets/chao_garden/sounds.json';d=json.loads(p.read_text());d['toy.tambourine']={'subtitle':'subtitles.chao_garden.toy.tambourine','sounds':[{'name':f'chao_garden:toys/tambourine_{n}','stream':False} for n in range(1,4)]};p.write_text(json.dumps(d,indent=2)+'\n')
p=root/'src/main/resources/assets/chao_garden/lang/en_us.json';d=json.loads(p.read_text());d['subtitles.chao_garden.toy.tambourine']='Tambourine jingles';p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
print('Three original 0.23-second mono tambourine clips generated')
