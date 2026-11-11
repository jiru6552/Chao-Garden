"""Original short, soft metallic clashes. Listening validation remains separate."""
from pathlib import Path
import json,subprocess,tempfile,wave,numpy as np
root=Path(__file__).resolve().parents[1];target=root/'src/main/resources/assets/chao_garden/sounds/toys';rate=44100;duration=.85;t=np.arange(int(rate*duration))/rate
for variation in range(3):
 rng=np.random.default_rng(252900+variation);noise=rng.normal(0,1,len(t));high=noise-np.concatenate(([0],noise[:-1]));values=.16*high*np.exp(-t*19)
 for i in range(40):
  frequency=rng.uniform(700,11000);amplitude=.17/(1+i*.1);decay=rng.uniform(7,15)*(1+frequency/24000)
  values+=amplitude*np.sin(2*np.pi*frequency*t+rng.uniform(0,2*np.pi))*np.exp(-t*decay)
 values*=np.minimum(1,t/.0015)*np.minimum(1,(duration-t)/.08);values-=values.mean();samples=np.round(values/max(abs(values))*.62*32767).astype('<i2')
 with tempfile.TemporaryDirectory() as tmp:
  wav=Path(tmp)/'cymbal.wav'
  with wave.open(str(wav),'wb') as s:s.setnchannels(1);s.setsampwidth(2);s.setframerate(rate);s.writeframes(samples.tobytes())
  subprocess.run(['ffmpeg','-nostdin','-xerror','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(target/f'cymbals_{variation+1}.ogg')],check=True)
p=root/'src/main/resources/assets/chao_garden/sounds.json';d=json.loads(p.read_text());d['toy.cymbals']={'subtitle':'subtitles.chao_garden.toy.cymbals','sounds':[{'name':f'chao_garden:toys/cymbals_{n}','stream':False} for n in range(1,4)]};p.write_text(json.dumps(d,indent=2)+'\n')
p=root/'src/main/resources/assets/chao_garden/lang/en_us.json';d=json.loads(p.read_text());d['subtitles.chao_garden.toy.cymbals']='Cymbals clash';p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
print('Three original .85-second mono cymbal clashes generated')
