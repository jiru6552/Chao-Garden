"""Original short struck-bell synthesis; no sampled game audio. Listening approval is separate."""
from pathlib import Path
import json,math,random,struct,subprocess,tempfile,wave
root=Path(__file__).resolve().parents[1];target=root/'src/main/resources/assets/chao_garden/sounds/toys';target.mkdir(parents=True,exist_ok=True);rate=44100
for variation in range(3):
 rng=random.Random(252500+variation);values=[];duration=.44
 for i in range(int(rate*duration)):
  t=i/rate;attack=1-math.exp(-t*3000);fade=min(1,(duration-t)/.06)
  value=sum(a*math.sin(2*math.pi*1120*ratio*t)*math.exp(-t*decay) for ratio,a,decay in [(1,1,9),(2.706,.46,14),(5.2,.19,24),(8.93,.08,38)])
  value+=rng.uniform(-1,1)*.10*math.exp(-t*250)
  values.append(value*attack*fade)
 peak=max(map(abs,values));samples=[round(v/peak*.68*32767) for v in values]
 with tempfile.TemporaryDirectory() as tmp:
  wav=Path(tmp)/'bell.wav'
  with wave.open(str(wav),'wb') as stream:
   stream.setnchannels(1);stream.setsampwidth(2);stream.setframerate(rate);stream.writeframes(struct.pack('<'+'h'*len(samples),*samples))
  subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(target/f'hand_bell_{variation+1}.ogg')],check=True)
p=root/'src/main/resources/assets/chao_garden/sounds.json';d=json.loads(p.read_text());d['toy.hand_bell']={'subtitle':'subtitles.chao_garden.toy.hand_bell','sounds':[{'name':f'chao_garden:toys/hand_bell_{n}','stream':False} for n in range(1,4)]};p.write_text(json.dumps(d,indent=2)+'\n')
p=root/'src/main/resources/assets/chao_garden/lang/en_us.json';d=json.loads(p.read_text());d['subtitles.chao_garden.toy.hand_bell']='Hand bell rings';p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
print('Three original 0.44-second mono bell clips generated')
