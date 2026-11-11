"""Original plucked four-string chord, with deterministic damped-string synthesis."""
from pathlib import Path
import array,json,math,random,struct,subprocess,tempfile,wave
root=Path(__file__).resolve().parents[1];assets=root/'src/main/resources/assets/chao_garden';rate=44100;duration=.52
values=[0.0]*round(rate*duration)
# Open G voicing, staggered by 6ms for a compact strum; no sampled game audio.
for index,hz in enumerate([196.0,246.94165,293.66477,391.99544]):
 rng=random.Random(254500+index);length=round(rate/hz);ring=[rng.uniform(-1,1) for _ in range(length)];start=round(index*.006*rate)
 for i in range(len(values)-start):
  slot=i%length;v=ring[slot];ring[slot]=.496*(v+ring[(slot+1)%length]);t=i/rate
  envelope=min(1,t/.003)*min(1,(duration-(i+start)/rate)/.08)
  values[i+start]+=v*envelope*math.exp(-t*2.2)/(1+index*.15)
peak=max(map(abs,values));pcm=[round(v/peak*.70*32767) for v in values]
target=assets/'sounds/toys/guitar.ogg';target.parent.mkdir(parents=True,exist_ok=True)
with tempfile.TemporaryDirectory() as tmp:
 wav=Path(tmp)/'guitar.wav'
 with wave.open(str(wav),'wb') as out:out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate);out.writeframes(struct.pack('<'+'h'*len(pcm),*pcm))
 subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-fflags','+bitexact','-flags:a','+bitexact','-c:a','libvorbis','-q:a','5',str(target)],check=True)
p=assets/'sounds.json';data=json.loads(p.read_text());data['toy.guitar']={'subtitle':'subtitles.chao_garden.toy.guitar','sounds':[{'name':'chao_garden:toys/guitar','stream':False}]};p.write_text(json.dumps(data,indent=2)+'\n')
p=assets/'lang/en_us.json';data=json.loads(p.read_text());data['subtitles.chao_garden.toy.guitar']='Guitar strums';p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
print('Original 0.52-second guitar chord generated')
