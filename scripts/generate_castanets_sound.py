"""Original dry wooden shell clicks; listening is a separate validation step."""
from pathlib import Path
import json,math,random,struct,subprocess,tempfile,wave
root=Path(__file__).resolve().parents[1];target=root/'src/main/resources/assets/chao_garden/sounds/toys';rate=44100;duration=.10
for variation in range(3):
    rng=random.Random(252800+variation);values=[];last=0
    modes=[(rng.uniform(a,b),amp,decay,rng.uniform(0,6.28)) for a,b,amp,decay in [(650,850,.48,90),(1400,1900,.35,115),(2600,3500,.22,145),(4200,5500,.12,170)]]
    for i in range(int(rate*duration)):
        t=i/rate;noise=rng.uniform(-1,1);high=noise-last;last=noise
        contact=.24*high*math.exp(-t*600)
        shell=sum(a*math.sin(2*math.pi*f*t+p)*math.exp(-t*d) for f,a,d,p in modes)
        values.append((contact+shell)*(1-math.exp(-t*6000))*min(1,(duration-t)/.015))
    peak=max(map(abs,values));samples=[round(v/peak*.58*32767) for v in values]
    with tempfile.TemporaryDirectory() as tmp:
        wav=Path(tmp)/'castanet.wav'
        with wave.open(str(wav),'wb') as s:s.setnchannels(1);s.setsampwidth(2);s.setframerate(rate);s.writeframes(struct.pack('<'+'h'*len(samples),*samples))
        subprocess.run(['ffmpeg','-nostdin','-xerror','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(target/f'castanets_{variation+1}.ogg')],check=True)
p=root/'src/main/resources/assets/chao_garden/sounds.json';d=json.loads(p.read_text());d['toy.castanets']={'subtitle':'subtitles.chao_garden.toy.castanets','sounds':[{'name':f'chao_garden:toys/castanets_{n}','stream':False} for n in range(1,4)]};p.write_text(json.dumps(d,indent=2)+'\n')
p=root/'src/main/resources/assets/chao_garden/lang/en_us.json';d=json.loads(p.read_text());d['subtitles.chao_garden.toy.castanets']='Castanets click';p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
print('Three original 0.10-second mono castanet clicks generated')
